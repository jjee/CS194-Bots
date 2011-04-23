package finalBot;

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

public class Governor {
	private HashMap<ROUnit, UnitType> builders;
	private HashSet<ROUnit> allWorkers;
	private LinkedList<UnitType> toBuild;
	private Pair<UnitType, TilePosition> nextBuild;
	private Spy scout;
	private Commander attacker;
	
	public Governor() {
		builders = new HashMap<ROUnit, UnitType>();
		allWorkers = new HashSet<ROUnit>();
		toBuild = new LinkedList<UnitType>();
		scout = null;
		attacker = null;
	}
	
	public Governor(LinkedList<UnitType> toBuild) {
		this();
		this.toBuild = toBuild;
	}
	
	public void setSpy(Spy spy) {
		scout = spy;
	}
	
	public void setCommander(Commander commander) {
		attacker = commander;
	}
	
	public void addToBuild(UnitType type, int priority) {
		toBuild.remove(type);
		toBuild.add(priority, type);
	}
	
	//TODO
	public List<Pair<UnitType, TilePosition>> plan() {
		//to start off, get state stuff
		int workers = allWorkers.size();
		Player me = Game.getInstance().self();
		int availGas = me.gas();
		int availMinerals = me.minerals();
		int supply = me.supplyTotal() - me.supplyUsed();
		int barracks = UnitUtils.getAllMy(UnitType.TERRAN_BARRACKS).size();
		int supplyExpecting = 0;
		boolean hasAcademy = !UnitUtils.getAllMy(UnitType.TERRAN_ACADEMY).isEmpty();
		boolean hasStim = me.hasResearched(TechType.STIM_PACKS);
		boolean hasRange = me.getUpgradeLevel(UpgradeType.U_238_SHELLS) == 1;
		int comsats = UnitUtils.getAllMy(UnitType.TERRAN_ACADEMY).size();
		int turrets = UnitUtils.getAllMy(UnitType.TERRAN_MISSILE_TURRET).size();
		
		for(ROUnit u: builders.keySet()){
			UnitType willHave = builders.get(u);
			if (u.isConstructing() && u.getBuildUnit()==null){ //unit going to construct but
				availMinerals -= willHave.mineralPrice();
				availGas -= willHave.gasPrice();
			}
			if (willHave == UnitType.TERRAN_BARRACKS){
				barracks++;
			} else if (willHave == UnitType.TERRAN_SUPPLY_DEPOT){
				supplyExpecting += 8;
			} else if (willHave == UnitType.TERRAN_ACADEMY) {
				hasAcademy = true;
			}
		}
		//not done getting stuff yet ^
		
		
		return null;
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
}
