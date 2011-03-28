package fixedBots;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

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

import edu.berkeley.nlp.starcraft.AbstractCerebrate;
import edu.berkeley.nlp.starcraft.Cerebrate;
import edu.berkeley.nlp.starcraft.Strategy;
import edu.berkeley.nlp.starcraft.scripting.Command;
import edu.berkeley.nlp.starcraft.scripting.JythonInterpreter;
import edu.berkeley.nlp.starcraft.scripting.Thunk;
import edu.berkeley.nlp.starcraft.util.UnitUtils;

public class VultureDropNoCheese extends AbstractCerebrate implements Strategy {
	
	JythonInterpreter jython = new JythonInterpreter();
	private Player me;
	private TilePosition myHome;
	private Unit myBase;
	
	private Unit sparky;
	private int sparkySteps = 0;
	private int sparkyLives = 3;
	private int sparkyMode = 0;
	private boolean cheeserDefenseMode = false;
	private boolean cheeserAttackMode = false;
	
	boolean toScout = false;
	private TilePosition scoutTarget;
	private HashSet<TilePosition> scouted = new HashSet<TilePosition>();
	private ArrayList<Unit> enemyBuildings = new ArrayList<Unit>();
	private ArrayList<Position> enemyBases = new ArrayList<Position>();
	
	private ArrayList<Position> rallyPoints = new ArrayList<Position>();
	private ArrayList<Position> minePoints = new ArrayList<Position>();
	private ArrayList<Position> intermediaries = new ArrayList<Position>();
	private ArrayList<UnitType> rebuildList = new ArrayList<UnitType>();

	private HashMap<Unit, Integer> workers = new HashMap<Unit, Integer>();
	private HashMap<Unit, Integer> vultures = new HashMap<Unit, Integer>();
	private HashMap<Unit, Integer> dropships = new HashMap<Unit, Integer>();
	
	private int gameMode = 0;
	private int buildDistance = 12;
	private int numIterations = 500;
	private int numGasHarvesters = 2;

	private Random rand = new Random();
	
	@Override
	public List<Cerebrate> getTopLevelCerebrates() {
		initializeJython();
		  return Arrays.<Cerebrate>asList(jython,this);
	}
		
	public boolean placeAndBuild(UnitType unit, boolean isCommandCenter) {		
		Unit builder = selectWorker();
		if (builder == null)
			return false;
		
		for (int i = 0; i < numIterations; i++) {
			int x = rand.nextInt(2*buildDistance)-buildDistance;
			int y = rand.nextInt(2*buildDistance)-buildDistance;
			TilePosition pos = new TilePosition(myHome.x()+x, myHome.y()+y);
			TilePosition add_pos = new TilePosition(myHome.x()+x+2, myHome.y()+y+1);
			if (isCommandCenter && builder.canBuildHere(myHome, UnitType.TERRAN_COMMAND_CENTER)) {
				builder.build(myHome, UnitType.TERRAN_COMMAND_CENTER);
				return true;
			}
			else if (builder.canBuildHere(pos, unit)) {
				if (unit.equals(UnitType.TERRAN_FACTORY) && UnitUtils.getAllMy(UnitType.TERRAN_MACHINE_SHOP).size() == 0 || unit.equals(UnitType.TERRAN_STARPORT)) {
					if (builder.canBuildHere(add_pos, unit)) {
						  builder.build(pos, unit);
						  return true;
					  }
				}
				else {
					builder.build(pos, unit);
					return true;					  
				}
			}
		}
		buildDistance++;
		return false;
	}

	public Unit selectWorker() {
		Unit worker = null;
		int numWorkers = 2;
		for (Unit w : workers.keySet()) {
			if (workers.get(w) == 0 && !w.isConstructing()) {
				worker = w;
				break;
			}
			if (workers.get(w) != 1)
				numWorkers--;
			if (numWorkers <= 0)
				break;
		}
		return worker;
	}
	
	public boolean minesClear(Position loc) {
		for (ROUnit mine : UnitUtils.getAllMy(UnitType.TERRAN_VULTURE_SPIDER_MINE)) {
			if (mine.getPosition().getDistance(loc) < 100)
				return false;
		}
		return true;
	}
	
	public Position getClosestEnemyBuilding (Position loc) {
		Position closest = enemyBuildings.get(0).getLastKnownPosition();
		double currDistance = loc.getDistance(closest);
		for (Unit building : enemyBuildings) {
			if (loc.getDistance(building.getLastKnownPosition()) < currDistance) {
				closest = building.getLastKnownPosition();
				currDistance = loc.getDistance(building.getLastKnownPosition());
			}
		}
		return closest;
	}
	
	public void scout() {
		if (scouted.containsAll(Game.getInstance().getStartLocations()) || !enemyBases.isEmpty()) {
			toScout = false;
			sparkySteps = 0;
			sparky.rightClick(myHome);
		}
		
		if (toScout){
			for (TilePosition tp: Game.getInstance().getStartLocations()){
				if (scouted.contains(tp)) continue;
					scoutTarget = tp;
			}
			sparky.rightClick(scoutTarget);
			sparkySteps++;
			if (sparkySteps == 100 || sparkySteps == 170 || sparkySteps == 220)
				rallyPoints.add(sparky.getPosition());
			if (sparkySteps == 250 || sparkySteps == 280)
				minePoints.add(sparky.getPosition());

		}
		
		if (scoutTarget!= null){
			if (sparky.getTilePosition().getDistance(scoutTarget) < 5) {
				scouted.add(scoutTarget);
				scoutTarget = null;
			}
		}
		
		Game.getInstance().drawLineMap(sparky.getPosition(), Position.centerOfTile(myHome), Color.YELLOW);
	}

	public void cheese() {
		//Check how the cheeser SCV is doing and respond appropriately
		cheeserDefenseMode = false;
		for (ROUnit u: Game.getInstance().getAllUnits()) {
			if (me.isEnemy(u.getPlayer()) && (sparky.equals(u.getTarget()) || sparky.equals(u.getOrderTarget()))) {
				sparky.rightClick(myHome);
				cheeserDefenseMode = true;
				cheeserAttackMode = false;
				sparkySteps++;
				boolean toAdd = true;
				for (Position p : minePoints) {
					if (sparky.getDistance(p) < 250) {
						toAdd = false;
						break;
					}
				}
				if (sparky.getDistance(new Position(myHome)) < 800 || sparky.getDistance(getClosestEnemyBuilding(sparky.getPosition())) < 800)
					toAdd = false;
				if (toAdd)
					minePoints.add(sparky.getPosition());
			}
			else if (enemyBuildings.contains(u)) {
				if (cheeserDefenseMode == false) {
					sparky.attackUnit(u);
					cheeserAttackMode = true;
				}
			}
		}
		if (cheeserDefenseMode == false && cheeserAttackMode == false) {
			sparky.rightClick(enemyBases.get(0));
		}
		
		Game.getInstance().drawLineMap(sparky.getPosition(), Position.centerOfTile(myHome), Color.YELLOW);
	}
	
	public void earlyBuildOrder() {	
		//SCVs
		if (workers.size() < 10 && myBase.getTrainingQueue().isEmpty()) {
			myBase.train(UnitType.TERRAN_SCV);
		}	
		
		//Refinery
		if (UnitUtils.getAllMy(UnitType.TERRAN_REFINERY).size() == 0 && UnitUtils.getAllMy(UnitType.TERRAN_BARRACKS).size() > 0) {
			Unit builder = selectWorker();
			if (builder != null) {
				ROUnit closestPatch = UnitUtils.getClosest(myBase, Game.getInstance().getGeysers());
				if (closestPatch != null) {
					builder.build(closestPatch.getTilePosition(), UnitType.TERRAN_REFINERY);
				}
			}
		}
		  
		//Starport if ready
		if (UnitUtils.getAllMy(UnitType.TERRAN_FACTORY).size() > 0 && UnitUtils.getAllMy(UnitType.TERRAN_STARPORT).size() < 1)
			placeAndBuild(UnitType.TERRAN_STARPORT, false);
		for (ROUnit s : UnitUtils.getAllMy(UnitType.TERRAN_STARPORT))
			UnitUtils.assumeControl(s).buildAddon(UnitType.TERRAN_CONTROL_TOWER);
	  
		//Vultures
		if (UnitUtils.getAllMy(UnitType.TERRAN_MACHINE_SHOP).size() > 0)
			for (ROUnit f : UnitUtils.getAllMy(UnitType.TERRAN_FACTORY)) {
				if (f.getTrainingQueue().isEmpty())
				UnitUtils.assumeControl(f).train(UnitType.TERRAN_VULTURE);
			}
	  
		//Factory
		if (UnitUtils.getAllMy(UnitType.TERRAN_FACTORY).size() < 1) {
			  placeAndBuild(UnitType.TERRAN_FACTORY, false);
		}
		for (ROUnit f : UnitUtils.getAllMy(UnitType.TERRAN_FACTORY)) {
			UnitUtils.assumeControl(f).buildAddon(UnitType.TERRAN_MACHINE_SHOP);
		}
		for (ROUnit ms : UnitUtils.getAllMy(UnitType.TERRAN_MACHINE_SHOP)) {
			UnitUtils.assumeControl(ms).research(TechType.SPIDER_MINES);
			UnitUtils.assumeControl(ms).upgrade(UpgradeType.ION_THRUSTERS);
		}
	    
		//Marines
		if (UnitUtils.getAllMy(UnitType.TERRAN_MARINE).size() < 6) {
			for(ROUnit u: UnitUtils.getAllMy(UnitType.TERRAN_BARRACKS)) {
				if (u.getTrainingQueue().isEmpty())
					UnitUtils.assumeControl(u).train(UnitType.TERRAN_MARINE);
			}
		}
		
		//Barracks
		if (UnitUtils.getAllMy(UnitType.TERRAN_BARRACKS).size() == 0) {
			placeAndBuild(UnitType.TERRAN_BARRACKS, false);
		}	
	}
	
	public void intermediateBuildOrder() {
		//SCVs
		if (workers.size() < 16) {
			if (myBase.getTrainingQueue().isEmpty())
				myBase.train(UnitType.TERRAN_SCV);
		}

		//Dropships
		if (UnitUtils.getAllMy(UnitType.TERRAN_CONTROL_TOWER).size() > 0) {
			if (UnitUtils.getAllMy(UnitType.TERRAN_STARPORT).get(0).getTrainingQueue().isEmpty())
				UnitUtils.assumeControl(UnitUtils.getAllMy(UnitType.TERRAN_STARPORT).get(0)).train(UnitType.TERRAN_DROPSHIP);
		}
		
		//Starport
		if (UnitUtils.getAllMy(UnitType.TERRAN_STARPORT).size() == 0 && UnitUtils.getAllMy(UnitType.TERRAN_FACTORY).size() > 0)
			placeAndBuild(UnitType.TERRAN_STARPORT, false);
		for (ROUnit s : UnitUtils.getAllMy(UnitType.TERRAN_STARPORT))
			UnitUtils.assumeControl(s).buildAddon(UnitType.TERRAN_CONTROL_TOWER);
		
		//More factories
		if (UnitUtils.getAllMy(UnitType.TERRAN_FACTORY).size() < 2)
			placeAndBuild(UnitType.TERRAN_FACTORY, false);
		for (ROUnit f : UnitUtils.getAllMy(UnitType.TERRAN_FACTORY)) {
			UnitUtils.assumeControl(f).buildAddon(UnitType.TERRAN_MACHINE_SHOP);
		}
		
		//Research if needed
		for (ROUnit ms : UnitUtils.getAllMy(UnitType.TERRAN_MACHINE_SHOP)) {
			UnitUtils.assumeControl(ms).research(TechType.SPIDER_MINES);
			UnitUtils.assumeControl(ms).upgrade(UpgradeType.ION_THRUSTERS);
		}
		
		//Train vultures
		if (UnitUtils.getAllMy(UnitType.TERRAN_MACHINE_SHOP).size() > 0)
			for (ROUnit f : UnitUtils.getAllMy(UnitType.TERRAN_FACTORY)) {
				if (f.getTrainingQueue().isEmpty())
					UnitUtils.assumeControl(f).train(UnitType.TERRAN_VULTURE);
			}
	}
	
	public void lateBuildOrder() {
		//Wraiths
		int j = 0;
		for (ROUnit s : UnitUtils.getAllMy(UnitType.TERRAN_STARPORT)) {
			if (j%2 == 1 && s.getTrainingQueue().isEmpty())
				UnitUtils.assumeControl(s).train(UnitType.TERRAN_WRAITH);
			j++;
		}
		int numWraiths = UnitUtils.getAllMy(UnitType.TERRAN_WRAITH).size();
		for (ROUnit w: UnitUtils.getAllMy(UnitType.TERRAN_WRAITH)) {
			if (!enemyBuildings.isEmpty() && w.isIdle() && (numWraiths > 2 || w.getDistance(rallyPoints.get(2)) > 1000)) {
				for (ROUnit u: Game.getInstance().getAllUnits()) {
					if (me.isEnemy(u.getPlayer()) && (w.equals(u.getTarget()) || w.equals(u.getOrderTarget()))) {
						UnitUtils.assumeControl(w).useTech(TechType.PERSONNEL_CLOAKING);
						break;
					}
				}
				UnitUtils.assumeControl(w).attackMove(getClosestEnemyBuilding(w.getPosition()));
			}
			else if (w.isIdle() && w.getDistance(rallyPoints.get(2)) > 300)
				UnitUtils.assumeControl(w).attackMove(rallyPoints.get(2));
		}
		
		//Armory
		if (UnitUtils.getAllMy(UnitType.TERRAN_ARMORY).size() < 1) {
			placeAndBuild(UnitType.TERRAN_ARMORY, false);
		}
		for (ROUnit a : UnitUtils.getAllMy(UnitType.TERRAN_ARMORY)) {
			UnitUtils.assumeControl(a).upgrade(UpgradeType.TERRAN_VEHICLE_WEAPONS);
			UnitUtils.assumeControl(a).upgrade(UpgradeType.TERRAN_VEHICLE_PLATING);
			UnitUtils.assumeControl(a).upgrade(UpgradeType.TERRAN_SHIP_PLATING);
			UnitUtils.assumeControl(a).upgrade(UpgradeType.TERRAN_SHIP_WEAPONS);
		}
		
		//Goliaths
		int i = 0;
		for (ROUnit f : UnitUtils.getAllMy(UnitType.TERRAN_FACTORY)) {
			if (i%2 == 0 && f.getTrainingQueue().isEmpty())
				UnitUtils.assumeControl(f).train(UnitType.TERRAN_GOLIATH);
			i++;
		}
		for (ROUnit g: UnitUtils.getAllMy(UnitType.TERRAN_GOLIATH)) {
			if (g.isIdle() && !enemyBuildings.isEmpty() && g.getDistance(rallyPoints.get(2)) > 1000) {
				UnitUtils.assumeControl(g).attackMove(getClosestEnemyBuilding(g.getPosition()));
			}
			else if (g.isIdle() && g.getDistance(rallyPoints.get(2)) > 300)
				UnitUtils.assumeControl(g).attackMove(rallyPoints.get(2));
		}
		
		//More factories
		if (UnitUtils.getAllMy(UnitType.TERRAN_FACTORY).size() < 3)
			placeAndBuild(UnitType.TERRAN_FACTORY, false);
		for (ROUnit f : UnitUtils.getAllMy(UnitType.TERRAN_FACTORY)) {
			UnitUtils.assumeControl(f).buildAddon(UnitType.TERRAN_MACHINE_SHOP);
		}
		for (ROUnit ms : UnitUtils.getAllMy(UnitType.TERRAN_MACHINE_SHOP)) {
			UnitUtils.assumeControl(ms).research(TechType.TANK_SIEGE_MODE);
		}
		
		//Siege tanks
		int k = 0;
		for (ROUnit f : UnitUtils.getAllMy(UnitType.TERRAN_FACTORY)) {
			if (k%3 == 0 && f.getTrainingQueue().isEmpty())
				UnitUtils.assumeControl(f).train(UnitType.TERRAN_SIEGE_TANK_TANK_MODE);
			k++;
		}
		for (ROUnit s: UnitUtils.getAllMy(UnitType.TERRAN_SIEGE_TANK_TANK_MODE)) {
			if (!enemyBuildings.isEmpty() && s.isIdle()) {
				UnitUtils.assumeControl(s).useTech(TechType.NONE);
				UnitUtils.assumeControl(s).attackMove(getClosestEnemyBuilding(s.getPosition()));
			}
			if (!enemyBuildings.isEmpty() && s.getDistance(getClosestEnemyBuilding(s.getPosition())) < 300)
				UnitUtils.assumeControl(s).useTech(TechType.TANK_SIEGE_MODE);
		}
		for (ROUnit s: UnitUtils.getAllMy(UnitType.TERRAN_SIEGE_TANK_SIEGE_MODE)) {
			if (!enemyBuildings.isEmpty() && s.isIdle())
				UnitUtils.assumeControl(s).attackMove(getClosestEnemyBuilding(s.getPosition()));
		}
	}
	
	public void vultureDrop() {
		//Build second starport
		if (UnitUtils.getAllMy(UnitType.TERRAN_STARPORT).size() < 2) {
			placeAndBuild(UnitType.TERRAN_STARPORT, false);
		}
		for (ROUnit s : UnitUtils.getAllMy(UnitType.TERRAN_STARPORT))
			UnitUtils.assumeControl(s).buildAddon(UnitType.TERRAN_CONTROL_TOWER);
		for (ROUnit s : UnitUtils.getAllMy(UnitType.TERRAN_CONTROL_TOWER))
			UnitUtils.assumeControl(s).research(TechType.CLOAKING_FIELD);
		
		//Build more dropships 
		if (dropships.size() < vultures.size()/4) {
			int i = 0;
			for (ROUnit s : UnitUtils.getAllMy(UnitType.TERRAN_STARPORT)) {
				if (i%2 == 0 && s.getTrainingQueue().isEmpty())
					UnitUtils.assumeControl(s).train(UnitType.TERRAN_DROPSHIP);
				i++;
			}
		}

		//Load vultures while idle, move to enemy base and unload if full
		for (Unit d : dropships.keySet()) {
			//Find a target
			Position target = myBase.getPosition();
			if (!enemyBases.isEmpty()) {
				target = enemyBases.get(0);
				double currDistance = d.getDistance(target);
				for (Position eb : enemyBases) {
					if (d.getDistance(eb) < currDistance) {
						target = eb;
						currDistance = d.getDistance(eb);
					}
				}	
			}
			else if (!enemyBuildings.isEmpty())
				target = enemyBuildings.get(0).getLastKnownPosition();
			
			//Load vultures, other units
			if (d.getLoadedUnits().size() < 4 && dropships.get(d) == 0 && d.isIdle()) {
				for (Unit v : vultures.keySet()) {
					if (!v.isLoaded() && vultures.get(v) == 0) {
						d.load(v);
					}
				}
				if (UnitUtils.getAllMy(UnitType.TERRAN_GOLIATH).size() > 2) {
					for (ROUnit g : UnitUtils.getAllMy(UnitType.TERRAN_GOLIATH)) {
						if (!enemyBuildings.isEmpty() && g.getDistance(getClosestEnemyBuilding(g.getPosition())) > 300)
							d.load(g);
					}
				}
				for (ROUnit t : UnitUtils.getAllMy(UnitType.TERRAN_SIEGE_TANK_TANK_MODE)) {
					if (!enemyBuildings.isEmpty() && t.getDistance(getClosestEnemyBuilding(t.getPosition())) > 300)
						d.load(t);
				}
			}
			
			//Go to an intermediary location
			if (d.getLoadedUnits().size() >= 3 && dropships.get(d) == 0)
				dropships.put(d, 1);
			else if (dropships.get(d) == 1 && d.isIdle()) {
				int i = rand.nextInt(2);
				d.move(intermediaries.get(i));
				dropships.put(d, i+2);
			}
			if (dropships.get(d) == 2 && (d.getDistance(intermediaries.get(0)) < 100 || d.getDistance(target) < 100)) {
				dropships.put(d, 4);
				d.stop();
			}
			else if (dropships.get(d) == 3 && (d.getDistance(intermediaries.get(1)) < 100 || d.getDistance(target) < 100)) {
				dropships.put(d, 4);
				d.stop();
			}
			
			//Go to enemy buildings and unload
			if (dropships.get(d) == 4 && d.isIdle()) {
				d.move(target);
			}
			if (dropships.get(d) == 4 && d.getDistance(target) < 100) {
				dropships.put(d, 5);
			}
			if (dropships.get(d) == 5) {
				ROUnit closestPatch = UnitUtils.getClosest(d.getPosition(), Game.getInstance().getMinerals());
				if (closestPatch != null && d.getDistance(closestPatch) < 500)
					d.unloadAll(closestPatch.getPosition().add(rand.nextInt(200)-100, rand.nextInt(200)-100));
				else 
					d.unloadAll(target.add(rand.nextInt(200)-100, rand.nextInt(200)-100));
				for (ROUnit u : d.getLoadedUnits()) {
					if (u.getType().equals(UnitType.TERRAN_VULTURE))
						vultures.put((Unit) u, 1);
				}
				dropships.put(d, 0);
			}
			
			Game.getInstance().drawLineMap(d.getPosition(), Position.centerOfTile(myHome), Color.BLUE);
		}
	}

	public void vultureMicro() {
		//Train vultures
		if (vultures.size() < 16) {
			for (ROUnit f : UnitUtils.getAllMy(UnitType.TERRAN_FACTORY)) {
				if (f.getTrainingQueue().isEmpty())
					UnitUtils.assumeControl(f).train(UnitType.TERRAN_VULTURE);
			}
		}
		
		//Rally vultures, attack
		int numAttacking = 0;
		for (Unit v: vultures.keySet()) {
		  if (vultures.get(v) == 0) {
			  if (v.getSpiderMineCount() > 2 && v.isIdle()) {
				  int x = rand.nextInt(150)-75;
				  int y = rand.nextInt(150)-75;
				  Position loc = minePoints.get(rand.nextInt(minePoints.size())).add(x, y);
				  if (minesClear(loc))
					  v.useTech(TechType.SPIDER_MINES, loc);
			  }
			  if (numAttacking >= 6 && !enemyBuildings.isEmpty() && v.isIdle())
				 vultures.put(v, 1);
			  else if (v.isIdle() && v.getDistance(rallyPoints.get(0)) > 300) {
				  v.attackMove(rallyPoints.get(0));
			  }
		  }
		  else if (vultures.get(v) == 1) {
				if (rand.nextDouble() < 0.005) {
					v.useTech(TechType.SPIDER_MINES, v.getPosition());
				}
				else if (v.isIdle()) {
					if (!enemyBuildings.isEmpty())	
						v.attackMove(getClosestEnemyBuilding(v.getPosition()));
				}
			}
		}
	}
	
	//If necessary, rebuild any destroyed units
	public void rebuild() {
		if (rebuildList.size() > 0) {
			UnitType toBuild = rebuildList.get(0);
			boolean tryRebuild = true;
			if (toBuild.equals(UnitType.TERRAN_COMMAND_CENTER)) {
				tryRebuild = placeAndBuild(UnitType.TERRAN_COMMAND_CENTER, true);
			}
			else if (toBuild.equals(UnitType.TERRAN_REFINERY)) {
				Unit builder = null;
				for (Unit w : workers.keySet()) {
					if (workers.get(w) == 0) {
						builder = w;
						break;
					}
				}
				if (builder != null) {
					ROUnit closestPatch = UnitUtils.getClosest(builder, Game.getInstance().getGeysers());
					if (closestPatch != null) {
						builder.build(closestPatch.getTilePosition(), UnitType.TERRAN_REFINERY);
						tryRebuild = true;
					} 
					else
						tryRebuild = false;
				}
				else
					tryRebuild = false;
			}
			else if (UnitUtils.getAllMy(toBuild).size() < 1) {
				tryRebuild = placeAndBuild(toBuild, false);
			}
			if (tryRebuild)
				rebuildList.remove(0);
		}
	}

	public void supplyWorkersAndBunkers() {
		//Make sure have enough supply depots
		if (me.supplyUsed() >= me.supplyTotal()-5)
			placeAndBuild(UnitType.TERRAN_SUPPLY_DEPOT, false);
		  
		//Make sure have enough workers
		if (me.minerals() > 50 && workers.size() < 12 && UnitUtils.getAllMy(UnitType.TERRAN_SUPPLY_DEPOT).size() > 0) {
			if (myBase.getTrainingQueue().isEmpty())
				myBase.train(UnitType.TERRAN_SCV);
		}
		
		//Bunkers
		if (UnitUtils.getAllMy(UnitType.TERRAN_BARRACKS).size() > 0 && UnitUtils.getAllMy(UnitType.TERRAN_BUNKER).size() < 2) {
			Unit builder = selectWorker();
			if (builder != null) {
				int x = rand.nextInt(6)-3;
				int y = rand.nextInt(6)-3;
				builder.build((new TilePosition(rallyPoints.get(1))).add(x, y), UnitType.TERRAN_BUNKER);
			}
		}
	}

	public void barracksAndMarines() {
		//If have enough minerals, make more barracks
		if (me.minerals() > 800)
			placeAndBuild(UnitType.TERRAN_BARRACKS, false);
		  
		//If have enough minerals, make more marines
		if (me.minerals() > 200) {
			for (ROUnit b: UnitUtils.getAllMy(UnitType.TERRAN_BARRACKS)) {
				if (b.getTrainingQueue().isEmpty())
					UnitUtils.assumeControl(b).train(UnitType.TERRAN_MARINE);
			}
		}
		
		//Get rid of enemy workers
		ROUnit enemyWorker = null;
		for (ROUnit u: Game.getInstance().getAllUnits()) {
			if (me.isEnemy(u.getPlayer()) && u.getType().isWorker() && u.getDistance(new Position(myHome)) < 100) {
				enemyWorker = u;
				break;
			}
		}
		  
		//Rally marines
		int numMarines = UnitUtils.getAllMy(UnitType.TERRAN_MARINE).size();
		for (ROUnit m: UnitUtils.getAllMy(UnitType.TERRAN_MARINE)) {
			if (numMarines > 12 && !enemyBuildings.isEmpty() && m.isIdle()) {
				UnitUtils.assumeControl(m).attackMove(getClosestEnemyBuilding(m.getPosition()));
			}
			else if (m.isIdle() && enemyWorker != null) {
				UnitUtils.assumeControl(m).attack(enemyWorker);
			}
			else if (m.isIdle() && m.getDistance(rallyPoints.get(2)) > 300)
				UnitUtils.assumeControl(m).attackMove(rallyPoints.get(2));
		}
		for (ROUnit b: UnitUtils.getAllMy(UnitType.TERRAN_BUNKER)) {
			if (b.getLoadedUnits().size() < 4) {
				for (ROUnit m: UnitUtils.getAllMy(UnitType.TERRAN_MARINE))
					UnitUtils.assumeControl(b).load(m);
			}
		}
	}
	
	public void mineMineralsAndGas() {
		//Minerals
		int numOnGas = 0;
		for (Unit u: workers.keySet()) {
			if (u.isIdle()) {
				ROUnit closestPatch = UnitUtils.getClosest(u, Game.getInstance().getMinerals());
				if (closestPatch != null) {
					u.rightClick(closestPatch);
					workers.put(u, 0);
				}
			}
			else if (u.isGatheringGas() && !u.isConstructing())
				numOnGas++;
			
			Game.getInstance().drawLineMap(u.getPosition(), Position.centerOfTile(myHome), Color.GREEN);
		}

		//Gas
		if (me.gas() > 4*me.minerals())
			numGasHarvesters = 0;
		else if (me.gas() > 3*me.minerals())
			numGasHarvesters = 1;
		else if (me.gas() > 2*me.minerals())
			numGasHarvesters = 2;
		else
			numGasHarvesters = 3;
		
		if (numOnGas < numGasHarvesters) {
			for (ROUnit r : UnitUtils.getAllMy(UnitType.TERRAN_REFINERY)) {
				if (r.isCompleted()) {
					for (Unit w : workers.keySet()) {
						if (workers.get(w) == 0 || w.isIdle()) {
							w.rightClick(r);
							workers.put(w, 1);
							numOnGas++;
						}
						if (numOnGas >= numGasHarvesters)
							break;
					}
				}
			}
		}
		else {
			int diff = numOnGas - numGasHarvesters;
			for (Unit u: workers.keySet()) {
				if (diff <= 0)
					break;
				if (u.isGatheringGas()) {
					u.stop();
					diff--;
				}
			}
		}
	}

	public void onFrame() {
		if (toScout)
			scout();
		else if (sparky != null) {
			if (sparkyMode == 0) {
				sparkyMode = 1;
			}
			if (sparkyMode == 1) {
				if (sparky.getDistance(new Position(myHome)) < 800)
					sparkyMode = 2;
				else {
					boolean toAdd = true;
					for (Position p : minePoints) {
						if (sparky.getDistance(p) < 250) {
							toAdd = false;
							break;
						}
					}
					if (sparky.getDistance(new Position(myHome)) < 800 || sparky.getDistance(getClosestEnemyBuilding(sparky.getPosition())) < 800)
						toAdd = false;
					if (toAdd)
						minePoints.add(sparky.getPosition());
				}					
			}
			workers.put(sparky, 0);
		}
		  
		rebuild();
		supplyWorkersAndBunkers();
		  
		//Game mode
		if (gameMode == 0) {
			if (UnitUtils.getAllMy(UnitType.TERRAN_STARPORT).size() > 0 || UnitUtils.getAllMy(UnitType.TERRAN_FACTORY).size() > 0  && UnitUtils.getAllMy(UnitType.TERRAN_BUNKER).size() > 1) {
				gameMode = 1;
				System.out.println("early game");
			}
			else
				earlyBuildOrder();
		}
		else if (gameMode == 1) {
			if (dropships.size() > 0) {
				gameMode = 2;
				System.out.println("intermediate game");
			}
			else
				intermediateBuildOrder();
		}
		else if (gameMode == 2) {
			if (dropships.size() > 1) {
				gameMode = 3;
				System.out.println("late game");
			}
			vultureDrop();
		}
		else if (gameMode == 3) {
			vultureDrop();
			lateBuildOrder();
		}
		
		vultureMicro();		
		barracksAndMarines();		  
		mineMineralsAndGas();
	}

	public void onStart() {
		me = Game.getInstance().self();
		myHome = me.getStartLocation();
		scouted.add(myHome);
		
		for(ROUnit u: me.getUnits()) {
			if(u.getType().isWorker()) {
				workers.put(UnitUtils.assumeControl(u), 0);
			} else if(u.getType().isResourceDepot()) {
				myBase = UnitUtils.assumeControl(u);
			}
		}
	}

	public void onUnitCreate(ROUnit unit) {
		if (unit.getType().isWorker()) {
			workers.put(UnitUtils.assumeControl(unit), 0);
			if (workers.size() == 7 && toScout == false) {
				for (Unit w : workers.keySet()) {
					if (!w.isConstructing()) {
						workers.remove(w);
						sparky = w;
						toScout = true;
						break;
					}
				}
				System.out.println("yay");
			}
		}
		else if (unit.getType().equals(UnitType.TERRAN_VULTURE))
			vultures.put(UnitUtils.assumeControl(unit), 0); 
		else if (unit.getType().equals(UnitType.TERRAN_DROPSHIP))
			dropships.put(UnitUtils.assumeControl(unit), 0);
	}

	public void onUnitDestroy(ROUnit unit) { 
		if(unit.equals(sparky)) {
			if (sparkyLives > 0) {
				for (Unit w : workers.keySet()) {
					if (workers.get(w) == 0) {
						sparky = w;
						workers.remove(w);
						break;
					}
				}
				sparky.rightClick(enemyBases.get(0));
				sparkyLives--;
			}
		}
		else if(workers.containsKey(unit))
			workers.remove(UnitUtils.assumeControl(unit));
		if (enemyBuildings.contains((Unit)unit))
			enemyBuildings.remove(unit);
		if (enemyBases.contains((Unit)unit))
			enemyBases.remove(unit);
		else if (vultures.containsKey(unit))
			vultures.remove(UnitUtils.assumeControl(unit)); 
		else if (dropships.containsKey(unit))
			dropships.remove(UnitUtils.assumeControl(unit));
		
		if (unit.getPlayer().equals(me) && unit.getType().isBuilding())
			rebuildList.add(unit.getType());
	  }

	public void onUnitHide(ROUnit unit) {
		  
	}

	public void onUnitMorph(ROUnit unit) {
		if (unit.getType().isBuilding() && unit.getPlayer().isEnemy(me) && !enemyBuildings.contains((Unit)unit))
			enemyBuildings.add((Unit)unit);
 	}
		
	public void onUnitShow(ROUnit unit) {
		if (unit.getType().isBuilding() && unit.getPlayer().isEnemy(me)) {
			if (!enemyBuildings.contains((Unit)unit))
				enemyBuildings.add((Unit)unit);
			if (unit.getType().isResourceDepot() && !enemyBases.contains(unit.getPosition())) {
				enemyBases.add(unit.getPosition());
				if (intermediaries.size() == 0) {
					ROUnit closestPatch = UnitUtils.getClosest(unit, Game.getInstance().getMinerals());
					if (closestPatch != null && unit.getDistance(closestPatch) < 500) {
						intermediaries.add(new Position(closestPatch.getPosition().x(), myBase.getPosition().y()));
						intermediaries.add(new Position(myBase.getPosition().x(), closestPatch.getPosition().y()));
					}
					else {
						if (unit.getPosition().x() < myBase.getPosition().x())
							intermediaries.add(new Position(unit.getPosition().x()+150, myBase.getPosition().y()));
						else
							intermediaries.add(new Position(unit.getPosition().x()-150, myBase.getPosition().y()));
						if (unit.getPosition().y() > myBase.getPosition().y())
							intermediaries.add(new Position(myBase.getPosition().x(), unit.getPosition().y()+150));
						else
							intermediaries.add(new Position(myBase.getPosition().x(), unit.getPosition().y()-150));
					}
				}
			}
		}
	}
		

	public void onEnd(boolean isWinnerFlag) {
		if(isWinnerFlag == true)
			Game.getInstance().printf("GG");
	}
		
	// Feel free to add command and things here.
	// bindFields will bind all member variables of the object
	// commands should be self explanatory...
	protected void initializeJython() {
		jython.bindFields(this);
		jython.bind("game", Game.getInstance());
		jython.bindIntCommand("speed",new Command<Integer>() {
			@Override
			public void call(Integer arg) {
				Game.getInstance().printf("Setting speed to %d",arg);
				Game.getInstance().setLocalSpeed(arg);	      
			}
		});
			
		jython.bindThunk("reset",new Thunk() {
			@Override
			public void call() {
				initializeJython(); 
			}
		});	
	}
}
