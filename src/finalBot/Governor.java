package finalBot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.bwapi.proxy.model.Game;
import org.bwapi.proxy.model.Player;
import org.bwapi.proxy.model.ROUnit;
import org.bwapi.proxy.model.TechType;
import org.bwapi.proxy.model.TilePosition;
import org.bwapi.proxy.model.Unit;
import org.bwapi.proxy.model.UnitType;
import org.bwapi.proxy.model.UpgradeType;
import org.bwapi.proxy.util.Pair;

import edu.berkeley.nlp.starcraft.util.UnitUtils;

public class Governor extends Overseer {
	private HashMap<ROUnit, UnitType> builders;
	private HashSet<ROUnit> allWorkers;
	private LinkedList<UnitType> toBuild;
	private Pair<UnitType, TilePosition> nextBuild;
	private GameStage gamestage;
	
	public Governor() {
		builders = new HashMap<ROUnit, UnitType>();
		allWorkers = new HashSet<ROUnit>();
		toBuild = new LinkedList<UnitType>();
		scout = null;
		attacker = null;
		gamestage = GameStage.EARLY;
	}
	
	public Governor(LinkedList<UnitType> toBuild) {
		this();
		this.toBuild = toBuild;
	}
	
	public void addToBuild(UnitType type, int priority) {
		toBuild.remove(type);
		toBuild.add(priority, type);
	}
	
	//TODO
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public List<Pair<UnitType, TilePosition>> plan() {
		//to start off, get state stuff
		Player me = Game.getInstance().self();
		List<Pair<UnitType, TilePosition>> plan = new ArrayList<Pair<UnitType,TilePosition>>();
		//resources
		int availGas = me.gas();
		int availMinerals = me.minerals();
		int supply = me.supplyTotal() - me.supplyUsed();
		int supplyExpecting = 0;
		
		
		// general
		HashMap<UnitType,Integer> units = new HashMap<UnitType,Integer>();
		for(ROUnit u : Game.getInstance().self().getUnits()) {
			if(units.containsKey(u.getType()))
				units.put(u.getType(),units.get(u.getType())+1);
			else
				units.put(u.getType(),1);
		}
		
		HashMap<UnitType,Integer> futureUnits = new HashMap<UnitType,Integer>(units);
		
		//buildings
		boolean hasAcademy = !UnitUtils.getAllMy(UnitType.TERRAN_ACADEMY).isEmpty();
		boolean willHaveAcademy = false;
	
		//upgrades
		boolean hasStim = me.hasResearched(TechType.STIM_PACKS);
		boolean hasRange = me.getUpgradeLevel(UpgradeType.U_238_SHELLS) == 1;
		
		HashMap<UnitType,Integer> futureAssets = new HashMap<UnitType,Integer>();
		for(ROUnit u: builders.keySet()){
			UnitType willHave = builders.get(u);
			if (u.isConstructing() && u.getBuildUnit()==null){ //unit going to construct but
				availMinerals -= willHave.mineralPrice();		//haven't paid price yet
				availGas -= willHave.gasPrice();
			}
			if(futureAssets.containsKey(willHave))
				futureAssets.put(willHave,futureAssets.get(willHave)+1);
			else
				futureAssets.put(willHave,1);		
			if (willHave == UnitType.TERRAN_SUPPLY_DEPOT){
				supplyExpecting += 8;
			} else if (willHave == UnitType.TERRAN_ACADEMY) {
				willHaveAcademy = true;
			}
		}
		
		//plan given above state
		List<ROUnit> centers = UnitUtils.getAllMy(UnitType.TERRAN_COMMAND_CENTER);
		if(centers.isEmpty()) return null;
		Unit center = UnitUtils.assumeControl(centers.get(0));
		if(gamestage == GameStage.EARLY){	
			earlyBuild(plan, availMinerals, supply, units, futureAssets, center);
		} else if (gamestage == GameStage.MID){
			if(availMinerals >=100&& supply + supplyExpecting < 2 + units.get(UnitType.TERRAN_BARRACKS)){
				plan.add(new Pair(UnitType.TERRAN_SUPPLY_DEPOT,center.getTilePosition()));
				availMinerals-=100;
			}
		} else {

		}
		
		
		return null;
	}

	private void earlyBuild(List<Pair<UnitType, TilePosition>> plan,
			int availMinerals, int supply, HashMap<UnitType, Integer> units,
			HashMap<UnitType, Integer> futureAssets, Unit center) {
		if(8 > units.get(UnitType.TERRAN_SCV) + futureAssets.get(UnitType.TERRAN_SCV)){
			if(supply > 1 && availMinerals >= 50)
				plan.add(new Pair<UnitType, TilePosition>(UnitType.TERRAN_SCV,null));
		} else if (units.get(UnitType.TERRAN_SUPPLY_DEPOT) + futureAssets.get(UnitType.TERRAN_SUPPLY_DEPOT) == 0) {
			if(availMinerals >= 100)
				plan.add(new Pair<UnitType, TilePosition>(UnitType.TERRAN_SUPPLY_DEPOT,center.getTilePosition()));
		} else if(11 > units.get(UnitType.TERRAN_SCV) + futureAssets.get(UnitType.TERRAN_SCV)){
			if(supply > 1 && availMinerals >= 50)
				plan.add(new Pair<UnitType, TilePosition>(UnitType.TERRAN_SCV,null));
		} else if(units.get(UnitType.TERRAN_BARRACKS) + futureAssets.get(UnitType.TERRAN_BARRACKS) < 1){
			if(availMinerals >= 150)
				plan.add(new Pair<UnitType, TilePosition>(UnitType.TERRAN_BARRACKS,center.getTilePosition()));
		} else if(units.get(UnitType.TERRAN_REFINERY) + futureAssets.get(UnitType.TERRAN_REFINERY) < 1){
			if(availMinerals >= 50)
				plan.add(new Pair<UnitType, TilePosition>(UnitType.TERRAN_REFINERY,center.getTilePosition()));
		}
	}
	
	public void executePlan() {
		List<Pair<UnitType, TilePosition>> plan = plan();
		for(Pair<UnitType,TilePosition> p: plan){
			UnitType t = p.getFirst();
			TilePosition approx = p.getSecond();
			if (t.isAddon()){
				//TODO
			} else if (t.isBuilding()){
				build(t,approx);
			} else {
				produce(t);
			}
		}
	}
	
	public void advance() {
		if (nextBuild != null && build(nextBuild.getFirst(), nextBuild.getSecond()))
			nextBuild = plan().remove(0);
	}
	
	public void addWorker(Unit worker) {
		allWorkers.add(worker);
	}
	
	public void removeWorker(Unit worker) {
		allWorkers.remove(worker);
		builders.remove(worker);
	}
	
	/**
	 * Must be called at very beginning before doing anything else
	 */
	public void updateBuilders() {
		for (ROUnit u : builders.keySet()) {
			if (u.isIdle())
				builders.remove(u);
		}
	}
	
	private Unit acquireBuilder(TilePosition tp) {
		List<ROUnit> units = new LinkedList<ROUnit>();
		for (ROUnit u : allWorkers) {
			if (!u.isCarryingGas() && !u.isCarryingMinerals() 
					&& !u.isConstructing() && !builders.containsKey(u))
				units.add(u);
		}
		if (units.isEmpty())
			return null;
		return UnitUtils.assumeControl(Tools.findClosest(units, tp));
	}
	
	public Unit pullWorker(TilePosition tp) {
		Unit worker = acquireBuilder(tp);
		allWorkers.remove(worker);
		return worker;
	}
	
	public boolean build(UnitType type, TilePosition tp) {
		Unit builder = acquireBuilder(tp);
		if (builder == null)
			return false;
		if (!builder.isConstructing()&&!Game.getInstance().isVisible(tp)){
			builder.rightClick(tp);
			return false;
		}
		TilePosition actualTP = selectBuildLoc(type,tp, builder);
		if (actualTP!=null) {
			builder.build(tp, type);
			builders.put(builder, type);
			return true;					  
		}
		return false;
	}
	
	private TilePosition selectBuildLoc(UnitType unit, TilePosition approx, Unit builder){
		int numIterations = 20;
		Random rand = new Random();
		for (int buildDistance = 1; buildDistance < 20; buildDistance++){
			for (int i = 0; i < numIterations; i++) {
				int x = rand.nextInt(2*buildDistance)-buildDistance;
				int y = rand.nextInt(2*buildDistance)-buildDistance;
				TilePosition pos = new TilePosition(approx.x()+x, approx.y()+y);
				//TilePosition add_pos = new TilePosition(approx.x()+x+2, approx.y()+y+1);
				if(builder.canBuildHere(pos, unit)){
					return pos;
				}
			}
		}
		return null;
	}
	
	public boolean produce(UnitType type) {
		UnitType producer = type.whatBuilds().getKey();
		List<ROUnit> allProducers = UnitUtils.getAllMy(producer);
		for (ROUnit u : allProducers) {
			if (u.getTrainingQueue().isEmpty()) {
				UnitUtils.assumeControl(u).train(type);
				return true;
			}
		}
		return false;
	}
	
	public void gatherMinerals(Unit worker) {
		ROUnit closestPatch = UnitUtils.getClosest(worker, Game.getInstance().getMinerals());
		if (closestPatch != null)
			worker.rightClick(closestPatch);
	}
	
	public void gatherMinerals(Unit worker, TilePosition tp) {
		ROUnit closestPatch = Tools.findClosest((Set<ROUnit>) Game.getInstance().getMinerals(), tp);
		if (closestPatch != null)
			worker.rightClick(closestPatch);
	}
	
	public void gatherGas(Unit worker) {
		ROUnit closestRefinery = Tools.findClosest(UnitUtils.getAllMy(UnitType.TERRAN_REFINERY), worker.getPosition());
		if (closestRefinery != null)
			worker.rightClick(closestRefinery);
	}
	
	public void gatherGas(Unit worker, TilePosition tp) {
		ROUnit closestRefinery = Tools.findClosest(UnitUtils.getAllMy(UnitType.TERRAN_REFINERY), tp);
		if (closestRefinery != null)
			worker.rightClick(closestRefinery);		
	}
	
	public void mine(){
		for(ROUnit w: allWorkers){
			if(builders.containsKey(w)) continue;
			gatherMinerals(UnitUtils.assumeControl(w));
		}
	}
	
	public void gas(){
		List<ROUnit> refineries = UnitUtils.getAllMy(UnitType.TERRAN_REFINERY);
		if(!refineries.isEmpty()) return;
		if(Game.getInstance().self().gas() > 200) return;
		int gasCount = 0;
		for(ROUnit w: allWorkers){
			if(w.isGatheringGas())
				gasCount++;
		}
		for(ROUnit w: allWorkers){
			if (gasCount>=3) return;
			if(!w.isGatheringGas() && builders.containsKey(w)){
				gatherGas(UnitUtils.assumeControl(w));
				gasCount++;
			}
		}
	}
	
	public void act(){
		executePlan();
		mine();
		gas();
	}
	
	public GameStage getGameStage() {
		return gamestage;
	}
}
