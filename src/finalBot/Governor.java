package finalBot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.bwapi.proxy.model.Color;
import org.bwapi.proxy.model.Game;
import org.bwapi.proxy.model.Player;
import org.bwapi.proxy.model.Position;
import org.bwapi.proxy.model.ROUnit;
import org.bwapi.proxy.model.TechType;
import org.bwapi.proxy.model.TilePosition;
import org.bwapi.proxy.model.Unit;
import org.bwapi.proxy.model.UnitType;
import org.bwapi.proxy.model.UpgradeType;
import org.bwapi.proxy.util.Pair;

import edu.berkeley.nlp.starcraft.util.ConvexHull;
import edu.berkeley.nlp.starcraft.util.Counter;
import edu.berkeley.nlp.starcraft.util.UnitUtils;

public class Governor extends Overseer {
	private HashMap<ROUnit, UnitType> builders;
	private HashSet<ROUnit> allWorkers;
	private HashSet<ROUnit> gasWorkers;
	private LinkedList<UnitType> toBuild;
	private Pair<UnitType, TilePosition> nextBuild;
	private GameStage gamestage;
	private int updateTime;
	private final int REBUILD_TIME = 30;
	private ROUnit naturalBase;
	private ConvexHull miningArea;
	private boolean rushDetect = false, cloakDetect = false, airDetect = false;
	private boolean rushDealt = false, cloakDealt = false, airDealt = false;
	private boolean scan = false;
	public Governor() {
		builders = new HashMap<ROUnit, UnitType>();
		allWorkers = new HashSet<ROUnit>();
		toBuild = new LinkedList<UnitType>();
		scout = null;
		attacker = null;
		gamestage = GameStage.EARLY;
		gasWorkers = new HashSet<ROUnit>();
		naturalBase = UnitUtils.getAllMy(UnitType.TERRAN_COMMAND_CENTER).get(0);
		landManagement();
	}
	
	public Governor(LinkedList<UnitType> toBuild) {
		this();
		this.toBuild = toBuild;
	}
	
	public void addToBuild(UnitType type, int priority) {
		toBuild.remove(type);
		toBuild.add(priority, type);
	}
	
	public void landManagement(){
		Set<Position> vertices = new HashSet<Position>();
		vertices.add(naturalBase.getPosition());
		Set<? extends ROUnit> patches = Game.getInstance().getMinerals();
		for(ROUnit p : patches){
			if(p.isVisible())
				vertices.add(p.getPosition());
		}
		Set<ROUnit> geysers = (Set<ROUnit>) Game.getInstance().getStaticGeysers();
		vertices.add(Tools.findClosest(geysers,naturalBase.getPosition()).getPosition());	
		miningArea = new ConvexHull(vertices);
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
		Counter<UnitType> units = new Counter<UnitType>();
		for(ROUnit u : Game.getInstance().self().getUnits()) {
			if(units.containsKey(u.getType()))
				units.setCount(u.getType(),units.getCount(u.getType())+1);
			else
				units.setCount(u.getType(),1);
		}
		
		//buildings
		boolean hasAcademy = !UnitUtils.getAllMy(UnitType.TERRAN_ACADEMY).isEmpty();
		boolean willHaveAcademy = false;
	
		//upgrades
		boolean hasStim = me.hasResearched(TechType.STIM_PACKS);
		boolean hasRange = me.getUpgradeLevel(UpgradeType.U_238_SHELLS) == 1;
		detect();
		Counter<UnitType> futureAssets = new Counter<UnitType>();
		for(ROUnit u: builders.keySet()){
			UnitType willHave = builders.get(u);
			if (u.getBuildUnit()==null){ //unit going to construct but
				availMinerals -= willHave.mineralPrice();		//haven't paid price yet
				availGas -= willHave.gasPrice();
			}
			if(futureAssets.containsKey(willHave))
				futureAssets.setCount(willHave,futureAssets.getCount(willHave)+1);
			else
				futureAssets.setCount(willHave,1);		
			if (willHave == UnitType.TERRAN_SUPPLY_DEPOT){
				supplyExpecting += 16;
			} else if (willHave == UnitType.TERRAN_ACADEMY) {
				willHaveAcademy = true;
			}
		}
		
		//plan given above state
		List<ROUnit> centers = UnitUtils.getAllMy(UnitType.TERRAN_COMMAND_CENTER);
		if(centers.isEmpty()) return null;
		Unit center = UnitUtils.assumeControl(centers.get(0));
		if(gamestage == GameStage.EARLY){	
			earlyBuild(plan, availMinerals, availGas, supply, supplyExpecting, units, futureAssets, center);
		} else if (gamestage == GameStage.MID){
			midBuild(plan, availMinerals, availGas, supply, supplyExpecting, units, futureAssets, center);
		} else {

		}
		
		//if(!plan.isEmpty())
			//Game.getInstance().printf("Building: " + plan.get(0).getFirst());
		
		//if(builders!=null)
			//Game.getInstance().printf("Builder count: " + builders.size());
		return plan;
	}
	
	private void detect(){
		if(specialist.getAlert()==Alert.EARLY_RUSH){
			rushDetect = true;
		} else if (specialist.getAlert() == Alert.CLOAKED_UNITS) {
			cloakDetect = true;
		} else if (specialist.getAlert() == Alert.AIR_STRUCTURES ||
				specialist.getAlert() == Alert.AIR_UNITS){
			airDetect = true;
		}
	}
	
	private void midBuild(List<Pair<UnitType, TilePosition>> plan,
			int availMinerals, int availGas, int supply, int supplyExpecting, Counter<UnitType> units,
			Counter<UnitType> futureAssets, Unit center) {
		int turretNec = 3;
		if(airDetect&&cloakDetect){
			turretNec*=2;
		}
		if(airDetect && !airDealt){
			if(availMinerals >= 75 && turretNec < units.getCount(UnitType.TERRAN_MISSILE_TURRET) + units.getCount(UnitType.TERRAN_MISSILE_TURRET)){
				plan.add(new Pair<UnitType, TilePosition>(UnitType.TERRAN_MISSILE_TURRET,center.getTilePosition()));
			 } else if (availMinerals < 75) {
					airDealt = true;
			}
			availMinerals-=75;
		}
		if(cloakDetect && !cloakDealt){
			if(availMinerals >= 75 && turretNec < units.getCount(UnitType.TERRAN_MISSILE_TURRET) + units.getCount(UnitType.TERRAN_MISSILE_TURRET)){
				Position p = attacker.closestChoke(naturalBase);
				TilePosition tp = new TilePosition(p.x()*Tools.TILE_SIZE,p.y()*Tools.TILE_SIZE);
				plan.add(new Pair<UnitType, TilePosition>(UnitType.TERRAN_MISSILE_TURRET,tp));
			} else if (availMinerals < 75) {
				cloakDealt = true;
			}
			availMinerals-=75;
		}
		if(availMinerals >=100&& supply + supplyExpecting < 2 + 2*(1+units.getCount(UnitType.TERRAN_BARRACKS))){
			plan.add(new Pair<UnitType, TilePosition>(UnitType.TERRAN_SUPPLY_DEPOT,center.getTilePosition()));
			availMinerals-=100;
		}
		
		boolean hasAcad = !UnitUtils.getAllMy(UnitType.TERRAN_ACADEMY).isEmpty();
		List<ROUnit> barracks = UnitUtils.getAllMy(UnitType.TERRAN_BARRACKS);
		List<ROUnit> marines = UnitUtils.getAllMy(UnitType.TERRAN_MARINE);
		int marineCount = marines.size();
		List<ROUnit> medics = UnitUtils.getAllMy(UnitType.TERRAN_MEDIC);
		int medicCount = medics.size();
		for(ROUnit b: barracks){
			if(b.isCompleted()&&b.getTrainingQueue().isEmpty()){
				if(marineCount > medicCount*4 && hasAcad) {
					if(availMinerals >= 50 && supply > 1 && availGas >= 25){
						plan.add(new Pair<UnitType, TilePosition>(UnitType.TERRAN_MEDIC,null));
						medicCount++;
					}
					availMinerals-=50;
					availGas-=25;
					supply-=2;
				} else if(availMinerals >= 50 && supply > 1){
					plan.add(new Pair<UnitType, TilePosition>(UnitType.TERRAN_MARINE,null));
					availMinerals-=50;
					supply-=2;
				}
			}
		}
		
		if(units.getCount(UnitType.TERRAN_BARRACKS) + futureAssets.getCount(UnitType.TERRAN_BARRACKS) < 6){
			if(availMinerals >= 150)
				plan.add(new Pair<UnitType, TilePosition>(UnitType.TERRAN_BARRACKS,center.getTilePosition()));
			availMinerals -=150;
		} 
	}
	
	private void dealRush(List<Pair<UnitType, TilePosition>> plan,
			int availMinerals, int availGas, int supply, int supplyExpecting, Counter<UnitType> units,
			Counter<UnitType> futureAssets, Unit center){
		List<ROUnit> barracks = UnitUtils.getAllMy(UnitType.TERRAN_BARRACKS);
		boolean freeBarrack = false;
		for(ROUnit b: barracks){
			if(b.getTrainingQueue().isEmpty())
				freeBarrack = true;
		}
		boolean notDealt = false;
		if(freeBarrack&&units.getCount(UnitType.TERRAN_MARINE)+futureAssets.getCount(UnitType.TERRAN_MARINE)<8){
			plan.add(new Pair<UnitType, TilePosition>(UnitType.TERRAN_MARINE,null));
			if(availMinerals>=50 && supply>1){
				availMinerals-=50;
				supply-=2;
			}
			notDealt = true;
		}
		if(units.getCount(UnitType.TERRAN_BUNKER)+futureAssets.getCount(UnitType.TERRAN_BUNKER)<2){
			if(availMinerals>=100){
				Position choke = attacker.closestChoke(naturalBase);
				TilePosition chokeTile = new TilePosition(choke.x()*Tools.TILE_SIZE,choke.y()*Tools.TILE_SIZE);
				plan.add(new Pair<UnitType, TilePosition>(UnitType.TERRAN_BUNKER,chokeTile));
			}	
			availMinerals-=100;
			notDealt = true;
		}
		if(!notDealt) rushDealt = true;
	}

	private void earlyBuild(List<Pair<UnitType, TilePosition>> plan,
			int availMinerals, int availGas, int supply, int supplyExpecting, Counter<UnitType> units,
			Counter<UnitType> futureAssets, Unit center) {
		Player me = Game.getInstance().self();
		//buildings
		List<ROUnit> academies = UnitUtils.getAllMy(UnitType.TERRAN_ACADEMY);
		ROUnit academy = null;
		if(!academies.isEmpty()){
			academy = academies.get(0);
		}
	
		//upgrades
		boolean hasStim = me.hasResearched(TechType.STIM_PACKS);
		boolean hasRange = me.getUpgradeLevel(UpgradeType.U_238_SHELLS) == 1;
		
		if(8 > units.getCount(UnitType.TERRAN_SCV) + futureAssets.getCount(UnitType.TERRAN_SCV)){
			if(supply > 1 && availMinerals >= 50)
				plan.add(new Pair<UnitType, TilePosition>(UnitType.TERRAN_SCV,null));
			availMinerals-=50;
			supply-=2;
		} else if (units.getCount(UnitType.TERRAN_SUPPLY_DEPOT) + futureAssets.getCount(UnitType.TERRAN_SUPPLY_DEPOT) == 0) {
			if(availMinerals >= 100) 
				plan.add(new Pair<UnitType, TilePosition>(UnitType.TERRAN_SUPPLY_DEPOT,center.getTilePosition()));
			availMinerals -=100;
		} else if(11 > units.getCount(UnitType.TERRAN_SCV) + futureAssets.getCount(UnitType.TERRAN_SCV)){
			if(supply > 1 && availMinerals >= 50)
				plan.add(new Pair<UnitType, TilePosition>(UnitType.TERRAN_SCV,null));
			supply-=2;
			availMinerals-=50;
		} else if(units.getCount(UnitType.TERRAN_BARRACKS) + futureAssets.getCount(UnitType.TERRAN_BARRACKS) < 2){
			if(availMinerals >= 150)
				plan.add(new Pair<UnitType, TilePosition>(UnitType.TERRAN_BARRACKS,center.getTilePosition()));
			availMinerals -=150;
		} else if(rushDetect && !rushDealt){
			dealRush(plan, availMinerals, availGas, supply, supplyExpecting, units, futureAssets, center);
		} else if(units.getCount(UnitType.TERRAN_REFINERY) + futureAssets.getCount(UnitType.TERRAN_REFINERY) < 1){
			if(availMinerals >= 100) 
				plan.add(new Pair<UnitType, TilePosition>(UnitType.TERRAN_REFINERY,center.getTilePosition()));
			availMinerals -=50;
		} else if(units.getCount(UnitType.TERRAN_ACADEMY) +futureAssets.getCount(UnitType.TERRAN_ACADEMY) < 1){
			if(availMinerals >= 150)
				plan.add(new Pair<UnitType, TilePosition>(UnitType.TERRAN_ACADEMY,center.getTilePosition()));
			availMinerals-=150;
		} else if(academy!=null && !hasStim){
			if(availMinerals >= 100 && availGas >=100 && !academy.isResearching())
				UnitUtils.assumeControl(academy).research(TechType.STIM_PACKS);
			if(!academy.isResearching()){
				availMinerals-=100;
				availGas-=100;
			}
		} else if(academy!=null && !hasRange){
			if(availMinerals >= 150 && availGas >=150 && !academy.isUpgrading())
				UnitUtils.assumeControl(academy).upgrade(UpgradeType.U_238_SHELLS);
			if(!academy.isUpgrading()){
				availMinerals-=150;
				availGas-=150;
			}
		} else if(academy!=null&&units.getCount(UnitType.TERRAN_COMSAT_STATION)+futureAssets.getCount(UnitType.TERRAN_COMSAT_STATION)<1){
			if(availMinerals>=50&&availGas>=50)
				UnitUtils.assumeControl(center).buildAddon(UnitType.TERRAN_COMSAT_STATION);
		}
		
		
		
		if(hasRange&&hasStim&&units.getCount(UnitType.TERRAN_COMSAT_STATION)>0){
			gamestage = GameStage.MID;
		}
		
		if(availMinerals >=100&& supply + supplyExpecting < 4 + 2*(1+units.getCount(UnitType.TERRAN_BARRACKS))){
			plan.add(new Pair<UnitType, TilePosition>(UnitType.TERRAN_SUPPLY_DEPOT,center.getTilePosition()));
			availMinerals-=100;
		}
		
		List<ROUnit> centers = UnitUtils.getAllMy(UnitType.TERRAN_COMMAND_CENTER);
		if(!centers.isEmpty()&&centers.get(0).getTrainingQueue().isEmpty()&&
				units.getCount(UnitType.TERRAN_SCV) < 18) {
			if(availMinerals >= 50 && supply > 1){
				plan.add(new Pair<UnitType, TilePosition>(UnitType.TERRAN_SCV,null));
				availMinerals-=50;
				supply-=2;
			}
		}
		
		List<ROUnit> barracks = UnitUtils.getAllMy(UnitType.TERRAN_BARRACKS);
		List<ROUnit> marines = UnitUtils.getAllMy(UnitType.TERRAN_MARINE);
		int marineCount = marines.size();
		List<ROUnit> medics = UnitUtils.getAllMy(UnitType.TERRAN_MEDIC);
		int medicCount = medics.size();
		for(ROUnit b: barracks){
			if(b.isCompleted()&&b.getTrainingQueue().isEmpty()){
				if(marineCount > medicCount*4 && academy!=null && academy.isCompleted()) {
					if(availMinerals >= 50 && supply > 1 && availGas >= 25){
						plan.add(new Pair<UnitType, TilePosition>(UnitType.TERRAN_MEDIC,null));
						medicCount++;
					}
					availMinerals-=50;
					availGas-=25;
					supply-=2;
				} else if(availMinerals >= 50 && supply > 1){
					plan.add(new Pair<UnitType, TilePosition>(UnitType.TERRAN_MARINE,null));
					availMinerals-=50;
					supply-=2;
				}
			}
		}
		
		
	}
	
	public void executePlan() {
		List<Pair<UnitType, TilePosition>> plan = plan();
		if(plan==null) return;
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
		gasWorkers.remove(worker);
	}
	
	/**
	 * Must be called at very beginning before doing anything else
	 */
	public void updateBuilders() {
		Set<ROUnit> toRemove = new HashSet<ROUnit>();
		for (ROUnit u : builders.keySet()) {
			Game.getInstance().drawCircleMap(u.getPosition(), 20, Color.GREEN, false);
			if (u.isIdle() || (u.isGatheringGas() && builders.get(u) == UnitType.TERRAN_REFINERY))
				toRemove.add(u);
			else if (u.isGatheringMinerals()||u.isGatheringGas()){
				Unit myUnit = UnitUtils.assumeControl(u);
				TilePosition tp = selectBuildLoc(builders.get(u),myUnit.getTilePosition(),myUnit);
				if(tp!=null)
					UnitUtils.assumeControl(u).build(tp, builders.get(u));
			}
		}
		for(ROUnit u: toRemove){
			builders.remove(u);
		}
	}
	
	private Unit acquireBuilder(TilePosition tp) {
		List<ROUnit> units = new LinkedList<ROUnit>();
		for (ROUnit u : allWorkers) {
			if (!u.isIdle()&&u.isCompleted()&&!u.isConstructing() && !builders.containsKey(u))
				units.add(u);
		}
		if (units.isEmpty())
			return null;
		ROUnit u = Tools.findClosest(units, tp);
		System.out.println(u);
		if(u==null){
			System.out.println("no builder found");
			return null;
		}
		return UnitUtils.assumeControl(u);
	}
	
	public Unit pullWorker(TilePosition tp) {
		Unit worker = acquireBuilder(tp);
		if(worker==null) return null;
		allWorkers.remove(worker);
		return worker;
	}
	
	@SuppressWarnings("unchecked")
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
		if(unit == UnitType.TERRAN_REFINERY){
			Set<ROUnit> geysers = (Set<ROUnit>) Game.getInstance().getStaticGeysers();
			return UnitUtils.getClosest(builder.getPosition(), geysers).getTilePosition();
		}
		int numIterations = 30;
		Random rand = new Random();
		for (int tryDist = 0; tryDist < 30; tryDist++){
			int buildDistance = (int) (Math.random()*20+5);
			for (int i = 0; i < numIterations; i++) {
				int x = rand.nextInt(2*buildDistance)-buildDistance;
				int y = rand.nextInt(2*buildDistance)-buildDistance;
				TilePosition pos = new TilePosition(approx.x()+x, approx.y()+y);
				//TilePosition add_pos = new TilePosition(approx.x()+x+2, approx.y()+y+1);
				if(!miningArea.withinHull(new Position(pos.x()*Tools.TILE_SIZE,pos.y()*Tools.TILE_SIZE))&&
						builder.canBuildHere(pos, unit)){
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
			if(builders.containsKey(w)||(!w.isIdle()&&!w.isGatheringGas())||gasWorkers.contains(w)) {
				//if(builders.containsKey(w))
					//System.out.println("contains builder");
				continue;
			}
			gatherMinerals(UnitUtils.assumeControl(w));
		}
	}
	
	public void gas(){
		List<ROUnit> refineries = UnitUtils.getAllMy(UnitType.TERRAN_REFINERY);
		if(refineries.isEmpty()||!refineries.get(0).isCompleted()) return;
		//System.out.println(refineries);
		
		if(Game.getInstance().self().gas() > 200) {
			gasWorkers.clear();
			return;
		}
		int gasCount = 0;
		for(ROUnit w: allWorkers){
			if(w.isGatheringGas()){
				gasCount++;
				gasWorkers.add(w);
			}
		}
		for(ROUnit w: allWorkers){
			if (gasCount>=3) return;
			if(!w.isGatheringGas() && !builders.containsKey(w)){
				gatherGas(UnitUtils.assumeControl(w));
				gasCount++;
				gasWorkers.add(w);
			}
		}
	}
	
	private void assignScan(){
		List<ROUnit> comsats = UnitUtils.getAllMy(UnitType.TERRAN_COMSAT_STATION);
		if(!comsats.isEmpty()&&!scan){
			scout.assignComSat(UnitUtils.assumeControl(comsats.get(0)));
			scan= true;
		}
	}
	
	public void act(){
		updateTime++;
		assignScan();
		if(updateTime>=REBUILD_TIME){
			updateBuilders();
			updateTime = 0;
		}
		executePlan();
		gas();
		mine();
		miningArea.draw();
	}
	
	public GameStage getGameStage() {
		return gamestage;
	}
	
	public ROUnit getHome(){
		return naturalBase;
	}
}
