package fixedBots;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bwapi.proxy.model.Color;
import org.bwapi.proxy.model.Game;
import org.bwapi.proxy.model.Order;
import org.bwapi.proxy.model.Position;
import org.bwapi.proxy.model.ROUnit;
import org.bwapi.proxy.model.TechType;
import org.bwapi.proxy.model.TilePosition;
import org.bwapi.proxy.model.Unit;
import org.bwapi.proxy.model.UnitType;
import org.bwapi.proxy.model.UpgradeType;

import edu.berkeley.nlp.starcraft.util.UnitUtils;


public class LurkerDrop extends EmptyFixedBot{
	String hatchery = "Zerg Hatchery";
	String overlord = "Zerg Overlord";
	String zergling = "Zerg Zergling";
	String spawningPool = "Zerg Spawning Pool";
	String den = "Zerg Hydralisk Den";
	String hydralisk = "Zerg Hydralisk";
	String drone = "Zerg Drone";
	String lurker = "Zerg Lurker";
	String lurkerEgg = "Zerg Lurker Egg";
	String larva = "Zerg Larva";
	String extractor = "Zerg Extractor";
	String lair = "Zerg Lair";
	String lingSpeed = "Zerg Zergling Speed Upgrade";
	String drop = "Zerg Overlord Drop Upgrade";
	String lurker_up = "Zerg Lurker Upgrade";
	String ovieSpeed = "Zerg Overlord Speed Upgrade";
	
	boolean buildLock = false;
	boolean buildComplete = false;
	List<BuildCommand> buildOrder = new ArrayList<BuildCommand>();
	Unit lastBuilder = null;
	BuildCommand lastOrder = null;
	List<Unit> bases = new ArrayList<Unit>();
	List<Unit> larvae = new ArrayList<Unit>();
	List<Unit> drones = new ArrayList<Unit>();
	List<Unit> ovies = new ArrayList<Unit>();
	List<Unit> lings = new ArrayList<Unit>();
	List<Unit> hydras = new ArrayList<Unit>();
	List<Unit> lurkers = new ArrayList<Unit>();
	List<Unit> defenders = new ArrayList<Unit>();
	List<ROUnit> enemyUnits = new ArrayList<ROUnit>();
	Unit spawnPool;
	Unit hydraDen;
	Unit extractDrone;
	Unit myExtractor;
	
	private Set<TilePosition> scouted = new HashSet<TilePosition>();
	private boolean toScout = true;
	private Unit scout;
	private TilePosition scoutTarget;
	private boolean buildOvie = false;
	private HashMap<Unit, Boolean> boundaries = new HashMap<Unit, Boolean>();
	private int buildCount;
	
	boolean dropTech;
	boolean lurkTech;
	
	public void buildNext(){
		if(!buildOrder.isEmpty()&&!buildLock){
			if(buildOrder.get(0).order.equals(lingSpeed)){
				if(spawnPool == null || !spawnPool.isCompleted()){
					System.out.println("No spawn pool");
					return;
				}else if(getMinerals() >= 100 && Game.getInstance().self().gas() >= 100){
					System.out.println("Upgrading");
					spawnPool.upgrade(UpgradeType.METABOLIC_BOOST);
					buildOrder.remove(0);
				}
			}else if(buildOrder.get(0).order.equals(drop)){
				List<ROUnit> lairs = UnitUtils.getAllMy(UnitType.getUnitType(lair));
				if(lairs.isEmpty()){
					System.out.println("No lairs");
				}else if(getMinerals() >= 200 && Game.getInstance().self().gas() >= 200){
					if(lairs.get(0).isBeingConstructed()||lairs.get(0).isUpgrading())
						return;
					UnitUtils.assumeControl(lairs.get(0)).upgrade(UpgradeType.VENTRAL_SACS);
					buildOrder.remove(0);
					dropTech = true;
				}
			}else if(buildOrder.get(0).order.equals(lurker_up)){
				List<ROUnit> dens = UnitUtils.getAllMy(UnitType.getUnitType(den));
				if(dens.isEmpty()){
					System.out.println("No dens");
					return;
				} else if(getMinerals() >= 200 && Game.getInstance().self().gas() >= 200){
					if(hydraDen == null || hydraDen.isBeingConstructed())
						return;
					hydraDen.research(TechType.LURKER_ASPECT);
					lurkTech = true;
					buildOrder.remove(0);
				}
			}else if(buildOrder.get(0).order.equals(ovieSpeed)) {
				List<ROUnit> lairs = UnitUtils.getAllMy(UnitType.getUnitType(lair));
				if(lairs.isEmpty()){
					System.out.println("No lairs");
				}else if(getMinerals() >= 150 && Game.getInstance().self().gas() >= 150){
					if(lairs.get(0).isBeingConstructed()||lairs.get(0).isUpgrading())
						return;
					UnitUtils.assumeControl(lairs.get(0)).upgrade(UpgradeType.PNEUMATIZED_CARAPACE);
					buildOrder.remove(0);
				}
			}else if(createUnit(UnitType.getUnitType(buildOrder.get(0).order),buildOrder.get(0).loc)){
				lastOrder = buildOrder.remove(0);
			}
		}else if(buildLock){
			//check to see if builder still actually going to build 
			if(lastBuilder.getOrder().equals(Order.MINING_MINERALS)||
					lastBuilder.getOrder().equals(Order.MOVE_TO_GAS)||
					lastBuilder.getOrder().equals(Order.MOVE_TO_MINERALS)||
					lastBuilder.getOrder().equals(Order.GUARD)||
					lastBuilder.isGatheringGas()||
					lastBuilder.isGatheringMinerals()||lastBuilder.isIdle()){
				buildLock = false;
				drones.add(lastBuilder);
				buildOrder.add(0,lastOrder);
			}
		}
		if(buildOrder.size() == 0)
			buildComplete = true;
		//System.out.println(buildOrder.get(0));
	}
	
	public void buildContinue(){
		if(!buildLock){
			//is there a spawning pool?
			boolean buildSpawnPool = (spawnPool == null);
			if(buildSpawnPool==true) System.out.println("need spawnpool");
			//is there a hydraden?
			boolean buildHydraDen = (hydraDen == null);
			//enough supply?
			int supplyNeeded = 4*bases.size() - getSupply();
			int oviesWillHave = 0;
			for(ROUnit u: ovies){
				if(u.isMorphing())
					oviesWillHave++;
				else if(u.getType().equals(UnitType.getUnitType(larva)))
					oviesWillHave++;	
			}
			boolean needSupply = false;
			if(oviesWillHave*16 < supplyNeeded)
				needSupply = true;
			//enough drones?
			boolean needDrones = (drones.size() < 15);
			for(Unit l: larvae){
				if(l.isMorphing())
					needDrones = false;
			}
			if(getMinerals()>800&&drones.size()>7) needDrones = false;
			//enough hydralisks?
			boolean needHydras = (hydras.size() < 2);
			//enough lurkers?
			boolean needLurkers = (lurkers.size() < 6);
			//enough zerglings?
			boolean needLings = canBuild(zergling);
			boolean needHatch = getMinerals() > 400+200*bases.size();
			
			for(Unit h: bases){
				if(h.isMorphing())
					needHatch = false;
			}
			if(needDrones){
				createUnit(UnitType.getUnitType(drone),null);
			}else if(buildSpawnPool){
				createUnit(UnitType.getUnitType(spawningPool),null);
			}else if(buildHydraDen){
				createUnit(UnitType.getUnitType(den),null);
			}else if(needSupply){
				createUnit(UnitType.getUnitType(overlord),null);
			}else if(needHatch){
				boolean b = createUnit(UnitType.getUnitType(hatchery),null);
				
			}else{
				if(needHydras && canBuild(hydralisk)){
					createUnit(UnitType.getUnitType(hydralisk),null);
				}else if(!needHydras && needLurkers && canBuild(lurker)){
					createUnit(UnitType.getUnitType(lurker),null);
				}else if(canBuild(hydralisk)&&!needLurkers){
					createUnit(UnitType.getUnitType(hydralisk),null);
				}else if(needLings&&getMinerals()>150){
					createUnit(UnitType.getUnitType(zergling),null);
				}
			}
		}else{
			if(buildCount <50){
				buildCount++;
				return;
			}
			//check to see if builder still actually going to build 
			if(lastBuilder!=null && lastBuilder.exists()&&lastBuilder.getOrder().equals(Order.MINING_MINERALS)||
					lastBuilder.getOrder().equals(Order.MOVE_TO_GAS)||
					lastBuilder.getOrder().equals(Order.MOVE_TO_MINERALS)||
					lastBuilder.getOrder().equals(Order.GUARD)||
					lastBuilder.isGatheringGas()||
					lastBuilder.isGatheringMinerals()||lastBuilder.isIdle()){
				buildLock = false;
				drones.add(lastBuilder);
			}
			if(lastBuilder!=null&&!lastBuilder.exists())
				buildLock = false;
		}
	}
	
	public boolean canBuild(String name){
		UnitType t = UnitType.getUnitType(name);
		if(!t.isBuilding()&&!t.equals(UnitType.getUnitType(overlord))){
			if(getSupply() < t.supplyRequired())
				return false;
		}
		if(t.gasPrice() > Game.getInstance().self().gas())
			return false;
		if(t.mineralPrice() > getMinerals())
			return false;
		
		return true;
	}
	
	private void resetLarvae(){
		larvae.clear();
		List<ROUnit> rolarvae = UnitUtils.getAllMy(UnitType.ZERG_LARVA);
		for(ROUnit l: rolarvae){
			if(!l.isMorphing()){
				larvae.add(UnitUtils.assumeControl(l));
			}
		}
	}
	public boolean createUnit(UnitType t, TilePosition area){
		resetLarvae();
		if(area==null){
			area = bases.get(bases.size()-1).getTilePosition();
		}
		if(!t.equals(UnitType.getUnitType(overlord))&&!t.isBuilding()){
			if(getSupply() < 1){
				for(Unit u: ovies){
					if(u.isMorphing())
						return false;
				}
				return createUnit(UnitType.getUnitType(overlord),null);
			}
		}
		 if(t.equals(UnitType.getUnitType(spawningPool)) || t.equals(UnitType.getUnitType(den)) 
				|| t.equals(UnitType.getUnitType(hatchery))){
			if(getMinerals() >= t.mineralPrice() && Game.getInstance().self().gas() >= t.gasPrice()&& !drones.isEmpty()){
				Unit morpher = (Unit)findClosest(drones,area);
				
				TilePosition tp;
				for(int i = 6; i < 30; i++){
					tp = findBuildRadius(area,i,morpher,t);
					if(tp!=null){
						morpher.build(tp,t);
						lastBuilder = morpher;
						buildCount = 0;
						buildLock = true;
						Game.getInstance().drawLineMap(morpher.getPosition(), Position.centerOfTile(tp), Color.GREEN);
						drones.remove(morpher);
						System.out.println("Building " + t.getName() + morpher + morpher.exists()+morpher.getOrder());
						return true;
					}
				}
			}
		}else if(t.equals(UnitType.getUnitType(zergling)) || t.equals(UnitType.getUnitType(hydralisk))
				|| t.equals(UnitType.getUnitType(drone)) || t.equals(UnitType.getUnitType(overlord))){
			if(getMinerals() >= t.mineralPrice() && (getSupply() >= t.supplyRequired() || t.equals(UnitType.getUnitType(overlord)))
					&& Game.getInstance().self().gas() >= t.gasPrice() && !larvae.isEmpty()){
				Unit morpher = (Unit)findClosest(larvae,area);
				if(t.equals(UnitType.getUnitType(zergling)) && (spawnPool==null ||!spawnPool.isCompleted())){
					if(spawnPool == null && !buildOrder.contains(new BuildCommand(spawningPool)))
						buildOrder.add(0,new BuildCommand(spawningPool));
					return false;
				}
				if(t.equals(UnitType.getUnitType(hydralisk)) && (hydraDen==null ||!hydraDen.isCompleted()))
					return false;
				if(t.equals(UnitType.getUnitType(overlord))){
					ovies.add(morpher);
				}
					
				morpher.morph(t);
				//larvae.remove(morpher);
				return true;
			}
			if(getSupply()<1 && !buildOvie) {
				buildOrder.add(0,new BuildCommand(overlord));
				buildOvie = true;
			}
		}else if(t.equals(UnitType.getUnitType(lurker))){
			if(lurkTech&&!hydraDen.isResearching()&&getMinerals() >= t.mineralPrice() && getSupply() > 1
					&& Game.getInstance().self().gas() >= t.gasPrice() && !hydras.isEmpty()){
				hydras.get(0).morph(t);
				hydras.remove(0);
				return true;
			}else if(hydras.isEmpty()){
				buildOrder.add(0,new BuildCommand(hydralisk));
			}
			if(getSupply() < 2 && !buildOvie) {
				buildOrder.add(0, new BuildCommand(overlord));
				buildOvie = true;
				System.out.println("adding new ovy");
			}
		}else if(t.equals(UnitType.getUnitType(lair))){
			if(getMinerals() >= t.mineralPrice() && Game.getInstance().self().gas() >= t.gasPrice()){
				Unit morpher = (Unit) findClosest(bases,area);
				morpher.morph(t);
				System.out.println("Building Lair");
				return true;
			}
		}
		return false;
	}
	
	public TilePosition findBuildRadius(TilePosition c, int radius, Unit u, UnitType t){
		TilePosition tp;
		for(int y = -radius; y <= radius; y+=1){
			for(int x = -radius; x <= radius; x+=1){
				tp = new TilePosition(c.x()+x,c.y()+y);
				if(u.canBuildHere(tp, t)&&Math.random()>0.8)
					return tp;
			}
		}
		return null;
	}
	
	@Override
	public void setUpBuildOrder() {
		for(int i = 0; i < 5; i++) {
			buildOrder.add(new BuildCommand(drone));
		}
		buildOrder.add(new BuildCommand(overlord));
		buildOrder.add(new BuildCommand(spawningPool));
		//buildOrder.add(new BuildCommand(extractor));
		buildOrder.add(new BuildCommand(drone));
		buildOrder.add(new BuildCommand(drone));
		//3 drones on gas
		buildOrder.add(new BuildCommand(zergling));
		buildOrder.add(new BuildCommand(zergling));
		buildOrder.add(new BuildCommand(zergling));
		buildOrder.add(new BuildCommand(drone));
		buildOrder.add(new BuildCommand(lair));
		buildOrder.add(new BuildCommand(overlord));
		buildOrder.add(new BuildCommand(overlord));
		buildOrder.add(new BuildCommand(drone));
		buildOrder.add(new BuildCommand(drone));
		buildOrder.add(new BuildCommand(drone));
		buildOrder.add(new BuildCommand(drop));
		buildOrder.add(new BuildCommand(den));
		buildOrder.add(new BuildCommand(lurker_up));
		buildOrder.add(new BuildCommand(hydralisk));
		buildOrder.add(new BuildCommand(hydralisk));
		buildOrder.add(new BuildCommand(drone));
		buildOrder.add(new BuildCommand(drone));
		for(int i = 0; i<2; i++){
			buildOrder.add(new BuildCommand(lurker));
		}
		buildOrder.add(new BuildCommand(ovieSpeed));
		//buildOrder.add(new BuildCommand(lingSpeed));
	}
	
	@Override
	public void onFrame(){
		for(Unit u: drones) {
			  	if(u.isIdle()) {
			  		ROUnit closestPatch = UnitUtils.getClosest(u, Game.getInstance().getMinerals());
			  		if(closestPatch!=null)
			  			u.rightClick(closestPatch);
			  	}
		} 
		gasFrame();
		if(!buildComplete)
			buildNext();
		else
			buildContinue();
		attack();
		if(toScout)
			scout();
	}
	
	public void scout(){
		if(ovies.isEmpty())
			return;
		else if(scout == null)
			scout = ovies.get(0);
		if(scouted.containsAll(myMap.getStartSpots())) {
			toScout = false;
			System.out.println("Stop Scouting");
			scout.rightClick(Game.getInstance().self().getStartLocation());
		}
		
		if(toScout){
			for(TilePosition tp: myMap.getStartSpots()){
				if(scouted.contains(tp)) continue;
				scoutTarget = tp;
			}

			scout.rightClick(scoutTarget);
		}
		
		if(scoutTarget!=null){
			if(close(scout.getTilePosition(), scoutTarget)){
				scouted.add(scoutTarget);
				scoutTarget = null;
			}
		}
	}
	
	private void dropNow(Unit m){
		Order order = m.getOrder();
		Position p = m.getPosition();
		if(!order.equals(Order.MOVE_UNLOAD)||!order.equals(Order.UNLOAD)){
			Position unloadPosition = new Position(p.x()+(int)(Math.random()*100)-50,
					p.y() + (int)(Math.random()*100) - 50);
			m.unloadAll(unloadPosition);
			System.out.println("trying tio unload all");
		}
	}
	
	/*@Override
	public boolean close(TilePosition t1, TilePosition t2){
		double dist = t1.getDistance(t2);
		return dist < 9;
	}*/
	
	public void attack(){
		int armyCount = 0;
		for(Unit u: lings){
			if(u.isIdle())
				armyCount++;
		}
		for(Unit u: hydras){
			if(u.isIdle())
				armyCount++;
		}
		if(armyCount >= 15){
			for(Unit u: lings){
				if(u.isIdle()){
					TilePosition tp = getTarget(u);
					if(tp!=null)
						u.attackMove(new Position(tp.x()*32,tp.y()*32));
				}
			}
			for(Unit u: hydras){
				if(u.isIdle()){
					TilePosition tp = getTarget(u);
					if(tp!=null)
						u.attackMove(new Position(tp.x()*32,tp.y()*32));
				}
			}
		}
		for(Unit u: defenders){
			if(!u.isBurrowed())
				u.burrow();
		}
		int unborrowedLurkers = 0;
		for(Unit u: lurkers) {
			if(!u.isBurrowed() && close(enemyUnits,u.getTilePosition()))
				u.burrow();
			else if(!defenders.contains(u) && u.isBurrowed() && !close(enemyUnits,u.getTilePosition()))
				u.unburrow();
			if(!u.isBurrowed())
				unborrowedLurkers++;
		}
		
		//load
		Unit mover = null;
		if(ovies.isEmpty()) return;
		for(Unit u : ovies) {
			if(u.isIdle()) {
				mover = u;
				break;
			}
		}
		if(mover != null){
			for(Unit u: lurkers) {
				if(u.isMorphing()) continue;
				if(!defenders.contains(u) && mover.getLoadedUnits().size() < 2&&!u.isBurrowed()) {
					mover.load(u);
				}
			}
		}
		//drop
		for(Unit m: ovies){
			if(m.getLoadedUnits().size() > 0 && close(enemyUnits,m.getTilePosition())){
				//dropNow(m);
				System.out.println("unloading");
				boundaries.put(m, true);
				//continue;
			}
			if(m.getLoadedUnits().size() == 0 && m.isIdle()){
				boundaries.put(m, false);
				if(unborrowedLurkers == 0&&!bases.isEmpty()){
					TilePosition home = bases.get(0).getTilePosition();
					if(!close(m.getTilePosition(),home))
						m.move(home);
					System.out.println("returning home");
				}
			}
			TilePosition t = getTarget(m);
			if(m.isIdle() && m.getLoadedUnits().size()>0 && !boundaries.get(m) && t != null) {
				int tx=t.x(), ty = t.y();
				int borderx = 0;
				int bordery = 0;
				int verticalBoundsDist = Math.min(t.x(), Game.getInstance().getMapWidth()-t.x());
				int horizBoundsDist = Math.min(t.y(),Game.getInstance().getMapHeight()-t.y());
				boolean sneak = true;
				if(verticalBoundsDist < horizBoundsDist){
					if(Math.abs(ty-m.getTilePosition().y()) < 10)
						sneak = false;
					if(tx>myMap.getWidth()/2)
						borderx = myMap.getWidth()-3;
					else
						borderx = 3;
					bordery = m.getTilePosition().y();
				}else{
					if(Math.abs(tx-m.getTilePosition().x()) < 10)
						sneak = false;
					if(ty>myMap.getHeight()/2)
						bordery = myMap.getHeight()-3;
					else
						bordery = 3;
					borderx = m.getTilePosition().x();
				}
				TilePosition b = null;
				if(sneak){
					b = new TilePosition(borderx,bordery);
					if(close(m.getTilePosition(),b))
						boundaries.put(m, true);
					else
						m.move(b);
				}else{
					boundaries.put(m, true);
				}
			}
			else if(m.isIdle()&&m.getLoadedUnits().size()>0){
				if(t==null)
					t = new TilePosition((int)(Math.random()*myMap.getWidth()),(int)(Math.random()*myMap.getHeight()));
				Position p = new Position(t.x()*32+(int)(Math.random()*100)-50,
						t.y()*32 + (int)(Math.random()*100) - 50);
				m.unloadAll(p);
			}
		}
	}
	
	public int computeDist(TilePosition a, TilePosition b){
		int dx = Math.abs(a.x() - b.x());
		int dy = Math.abs(a.y() - b.y());
		return dx+dy;
	}
	
	public TilePosition getTarget(Unit u){
		ROUnit target = null;
		int best = 10000;
		for(ROUnit b: myMap.getBuildings()){
			if(Game.getInstance().self().isEnemy(b.getPlayer())){
				int dist = computeDist(u.getTilePosition(),b.getLastKnownTilePosition());
				if(dist < best){
					best = dist;
					target = b;
					//System.out.println(target.getType().getName());
				}
			}
		}
		if(target == null)
			return null;
		return target.getLastKnownTilePosition();
		
	}
	
	public void gasFrame(){
		if(myExtractor == null && spawnPool!=null && (spawnPool.isBeingConstructed()||spawnPool.isCompleted())){
			if(extractDrone==null)
				extractDrone = drones.get(0);
			ROUnit closestPatch = UnitUtils.getClosest(extractDrone, Game.getInstance().getGeysers());
			if (closestPatch != null) {
				extractDrone.build(closestPatch.getTilePosition(), UnitType.getUnitType("Zerg Extractor"));
				drones.remove(extractDrone);
			}
		}
		if(myExtractor!=null&&myExtractor.isCompleted()){
			int gas = 0;
			for(Unit d: drones){
				if(d.isGatheringGas())
					gas++;
			}
			for(Unit d: drones){
				if(gas>=3)
					break;
				if(!d.isGatheringGas()){
					d.rightClick(myExtractor);
					gas++;
				}
			}
			System.out.println(gas);
		}
	}
	
	@Override
	public void onStart(){
		super.onStart();
		setUpBuildOrder();
		scouted.add(Game.getInstance().self().getStartLocation());
	}
	
	@Override
	public void onUnitShow(ROUnit unit){
		
	
		if(unit.getType().isBuilding()){
			myMap.addBuilding(unit);
		}
		if(Game.getInstance().self().isEnemy(unit.getPlayer())){
			enemyUnits.add(unit);
			return;
		}
		if(unit.getType().equals(UnitType.getUnitType(overlord))) {
			buildOvie = false;
		}
		//if(unit.getTilePosition().equals(scoutTarget))
			//scoutTarget = null;
	}
	
	@Override
	public void onUnitCreate(ROUnit unit){
		if(!unit.getPlayer().equals(Game.getInstance().self()))
				return;
		if(((Unit)unit).getType().isBuilding()){
			buildLock = false;
		}
		
		Unit u = UnitUtils.assumeControl(unit);
		if(u.getType().equals(UnitType.getUnitType(hatchery)))
			bases.add(u);
		if(u.getType().equals(UnitType.getUnitType(larva)))
			larvae.add(u);
		if(u.getType().equals(UnitType.getUnitType(drone)))
			drones.add(u);
		//if(u.getType().equals(UnitType.getUnitType(overlord)))
			//ovies.add(u);
		if(u.getType().equals(UnitType.getUnitType(zergling)))
			lings.add(u);
		if(u.getType().equals(UnitType.getUnitType(hydralisk)))
			hydras.add(u);
		if(u.getType().equals(UnitType.getUnitType(lurker)))
			lurkers.add(u);
		
	}
	@Override
	public void onUnitMorph(ROUnit unit){
		if(!unit.getPlayer().equals(Game.getInstance().self()))
			return;
		if(((Unit)unit).getType().isBuilding()){
			buildLock = false;
		}

		Unit u = UnitUtils.assumeControl(unit);
		if(u.getType().isBuilding()){
			drones.remove(u);
		}
		if(u.getType().getName().equals(hatchery)){
			bases.add(u);
		}
		//if(u.getType().equals(UnitType.getUnitType(larva)))
			//larvae.add(u);
		if(u.getType().equals(UnitType.getUnitType(drone))){
			drones.add(u);
			larvae.remove(u);
		}
		if(u.getType().equals(UnitType.getUnitType(overlord))) {
			//ovies.add(u);
			larvae.remove(u);
			buildOvie = false;
		}
		if(u.getType().equals(UnitType.getUnitType(zergling))){
			lings.add(u);
			larvae.remove(u);
		}
		if(u.getType().equals(UnitType.getUnitType(hydralisk))){
			larvae.remove(u);
			hydras.add(u);
		}
		if(u.getType().equals(UnitType.getUnitType(lurker))) {
			if(lurkers.size() < 3)
				defenders.add(u);
			lurkers.add(u);
			hydras.remove(u);
		}
		if(u.getType().equals(UnitType.getUnitType(spawningPool)))
			spawnPool = u;
		if(u.getType().equals(UnitType.getUnitType(den)))
			hydraDen = u;
		if(u.getType().equals(UnitType.getUnitType(extractor)))
			myExtractor = u;
	}
	
	@Override
	public void onUnitDestroy(ROUnit unit) {
		super.onUnitDestroy(unit);
		if(Game.getInstance().self().isEnemy(unit.getPlayer())){
			enemyUnits.remove(unit);
			return;
		}
		Unit u = UnitUtils.assumeControl(unit);
		if(u.getType().equals(UnitType.getUnitType(hatchery)))
			bases.remove(u);
		if(u.getType().equals(UnitType.getUnitType(larva)))
			larvae.remove(u);
		if(u.getType().equals(UnitType.getUnitType(drone)))
			drones.remove(u);
		if(u.getType().equals(UnitType.getUnitType(overlord))){
			ovies.remove(u);
			if(!buildOrder.isEmpty()&&
					(!buildOrder.get(0).order.equals(overlord ) || getSupply() < 2))
				buildOrder.add(0,new BuildCommand(overlord));
		}
		if(u.getType().equals(UnitType.getUnitType(zergling)))
			lings.remove(u);
		if(u.getType().equals(UnitType.getUnitType(hydralisk)))
			hydras.remove(u);
		if(u.getType().equals(UnitType.getUnitType(lurker))){
			lurkers.remove(u);
			if(defenders.contains(u)){
				defenders.remove(u);
			}
		}
		if(u.getType().equals(UnitType.getUnitType(spawningPool))){
			spawnPool = null;
			buildOrder.add(new BuildCommand(spawningPool));
		}
		if(u.getType().equals(UnitType.getUnitType(den))){
			hydraDen = null;
			buildOrder.add(new BuildCommand(den));
		}
		if(u.getType().equals(UnitType.getUnitType(extractor))){
			myExtractor = null;
			extractDrone = null;
		}
			
	}
}
