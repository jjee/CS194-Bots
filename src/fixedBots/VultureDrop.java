package fixedBots;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import org.bwapi.proxy.ProxyBot;
import org.bwapi.proxy.ProxyBotFactory;
import org.bwapi.proxy.ProxyServer;
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
import edu.berkeley.nlp.starcraft.overmind.Overmind;
import edu.berkeley.nlp.starcraft.scripting.Command;
import edu.berkeley.nlp.starcraft.scripting.JythonInterpreter;
import edu.berkeley.nlp.starcraft.scripting.Thunk;
import edu.berkeley.nlp.starcraft.util.UnitUtils;

public class VultureDrop extends AbstractCerebrate implements Strategy {
	
	JythonInterpreter jython = new JythonInterpreter();
	private Player me;
	private TilePosition myHome;
//	private Position enemyHome;
	private ArrayList<Position> rallyPoints;
	
	private Unit myBase;
	private Unit cheeser;

	private ArrayList<Unit> workers = new ArrayList<Unit>();
	private ArrayList<Unit> harvesters = new ArrayList<Unit>();	
	private ArrayList<Unit> marines = new ArrayList<Unit>();
	private ArrayList<Unit> supplyDepots = new ArrayList<Unit>();
	private ArrayList<Unit> barracks = new ArrayList<Unit>();
	private ArrayList<Unit> refineries = new ArrayList<Unit>();
	private ArrayList<Unit> factories = new ArrayList<Unit>();
	private ArrayList<Unit> machineShops = new ArrayList<Unit>();
	private HashMap<Unit, Integer> vultures = new HashMap<Unit, Integer>();
	private ArrayList<Unit> starports = new ArrayList<Unit>();
	private ArrayList<Unit> controlTowers = new ArrayList<Unit>();
	private HashMap<Unit, Integer> dropships = new HashMap<Unit, Integer>();
	private ArrayList<Unit> armories = new ArrayList<Unit>();
	
	private HashSet<TilePosition> scouted = new HashSet<TilePosition>();
	boolean toScout = true;
	private TilePosition scoutTarget;
	private ArrayList<Unit> enemyBuildings = new ArrayList<Unit>();
	private Position enemyLocation;
	
	private boolean cheeserDefenseMode = false;
	private boolean cheeserAttackMode = false;
	private boolean refineryFlag = false;
	private boolean supplyFlag = false;
	
	private int gameMode = 0;
	private int buildDistance = 12;
	private int numIterations = 500;
	private Random rand = new Random();
	
	@Override
	public List<Cerebrate> getTopLevelCerebrates() {
		initializeJython();
		  return Arrays.<Cerebrate>asList(jython,this);
	}
	
	public boolean placeAndBuild(Unit builder, String unit) {
		  int i = 0;
		  while(true) {
			  if(i > numIterations) {
				  buildDistance++;
				  break;
			  }
			  int x = rand.nextInt(2*buildDistance)-buildDistance;
			  int y = rand.nextInt(2*buildDistance)-buildDistance;
			  int add_x = x+2;
			  int add_y = y+1;
			  TilePosition pos = new TilePosition(myHome.x()+x, myHome.y()+y);
			  TilePosition add_pos = new TilePosition(myHome.x()+add_x, myHome.y()+add_y);
			  if (builder.canBuildHere(pos, UnitType.getUnitType(unit))) {
				  if (unit.equals("Terran Factory") && machineShops.size() == 0 || unit.equals("Terran Starport")) {
					  if (builder.canBuildHere(add_pos, UnitType.getUnitType(unit))) {
						  builder.build(pos, UnitType.getUnitType(unit));
						  return true;
					  }
				  }
				  else {
					  builder.build(pos, UnitType.getUnitType(unit));
					  return true;					  
				  }
			  }
			  i++;
		  }
		  return false;
	}
	
	public void scout() {
		if (scouted.containsAll(Game.getInstance().getStartLocations())) {
			toScout = false;
			System.out.println("Stop Scouting");
			cheeser.rightClick(Game.getInstance().self().getStartLocation());
		}
		
		if (toScout){
			for (TilePosition tp: Game.getInstance().getStartLocations()){
				if (scouted.contains(tp)) continue;
					scoutTarget = tp;
			}
			cheeser.rightClick(scoutTarget);
		}
		
		if (scoutTarget!=null){
			if (cheeser.getTilePosition().getDistance(scoutTarget) < 5) {
				scouted.add(scoutTarget);
				scoutTarget = null;
			}
		}
	}

	public void cheese() {
		  //Check how the cheeser SCV is doing and respond appropriately
		  cheeserDefenseMode = false;
		  for (ROUnit u: Game.getInstance().getAllUnits()) {
			  if (me.isEnemy(u.getPlayer()) && (cheeser.equals(u.getTarget()) || cheeser.equals(u.getOrderTarget()))) {
				  cheeser.rightClick(myHome);
				  cheeserDefenseMode = true;
				  cheeserAttackMode = false;
			  }
			  else if (enemyBuildings.contains(u)) {
				  if (cheeserDefenseMode == false) {
					  cheeser.attackUnit(u);
					  cheeserAttackMode = true;
				  }
			  }
		  }
		  if(cheeserDefenseMode == false && cheeserAttackMode == false) {
			  cheeser.rightClick(enemyLocation);
		  }
	}
	
	public void earlyBuildOrder() {	
		  //SCVs
		  if (me.minerals() > 250 && workers.size() < 16) {
			  myBase.train(UnitType.getUnitType("Terran SCV"));
		  }
		
		 //Refinery
		  if (refineries.size() == 0 && barracks.size() > 0 && me.minerals() > 100) {
			  ROUnit closestPatch = UnitUtils.getClosest(workers.get(1), Game.getInstance().getGeysers());
			  if (closestPatch != null) {
				  workers.get(1).build(closestPatch.getTilePosition(), UnitType.getUnitType("Terran Refinery"));
				  refineryFlag = true;
			  }
		  }
		  
		  if (refineryFlag) {
			  for (ROUnit unit : me.getUnits()) {
				  if (unit.getType() == UnitType.getUnitType("Terran Refinery")) {
					  refineries.add((Unit) unit);
					  refineryFlag = false;
					  break;
				  }
			  }
		  }
		  
		  //Starport if ready
		  if (starports.size() == 0 && factories.size() >= 2 && me.gas() > 100)
			  placeAndBuild(workers.get(1), "Terran Starport");
		  
		  //Vultures
		  if (factories.size() > 0)
			  factories.get(0).train(UnitType.getUnitType("Terran Vulture"));
		  
		  //Factory
		  if (factories.size() < 2 && me.gas() > 100 && me.minerals() > 200 && marines.size() > 5) {
			  placeAndBuild(workers.get(1), "Terran Factory");
		  }
		  for (Unit f : factories)
			  f.buildAddon(UnitType.getUnitType("Terran Machine Shop"));
		  for (Unit ms : machineShops) {
			  ms.research(TechType.SPIDER_MINES);
			  ms.upgrade(UpgradeType.ION_THRUSTERS);
		  }
		    
		  //Marines
		  if (barracks.size() > 0 && marines.size() < 6) {
			  for(Unit u: barracks) {
				  u.train(UnitType.getUnitType("Terran Marine"));
			  }
		  }
		  for (Unit u: marines) {
			  int i = 0;
			  if(u.getDistance(rallyPoints.get(i)) > 500 || (u.isIdle() && u.getDistance(rallyPoints.get(i)) > 100))
				  u.attackMove(rallyPoints.get(i));
		  }
		  
		  //Barracks
		  if (barracks.size() == 0 && me.minerals() > 150 && workers.size() >= 8) {
			  placeAndBuild(workers.get(1), "Terran Barracks");
		  }	
	}
	
	public void intermediateBuildOrder() {
		//Armory
		if (dropships.size() > 0 && me.gas() > 50 && me.minerals() > 100) {
			placeAndBuild(workers.get(1), "Terran Armory");
		}
		
		//Dropship 
		if (controlTowers.size() > 0 && me.gas() > 100 && me.minerals() > 100) {
			starports.get(0).train(UnitType.getUnitType("Terran Dropship"));
		}
		
		//Starport
		if (starports.size() == 0 && factories.size() >= 2 && me.gas() > 100) {
			placeAndBuild(workers.get(1), "Terran Starport");
		}
		for (Unit s : starports)
			s.buildAddon(UnitType.getUnitType("Terran Control Tower"));
		
		//Third factory
		if (factories.size() < 3 && me.gas() > 100 && me.minerals() > 200)
			placeAndBuild(workers.get(1), "Terran Factory");
		
		//Research if needed
		for (Unit ms : machineShops) {
			ms.research(TechType.SPIDER_MINES);
			ms.upgrade(UpgradeType.ION_THRUSTERS);
		}
		
		//Train vultures
		for (Unit f : factories)
			f.train(UnitType.getUnitType("Terran Vulture"));
		
//		//Rally vultures
//		for (Unit v: vultures.keySet()) {
//			int i = rand.nextInt(4);
//			if (v.getDistance(rallyPoints.get(i)) > 500 || (v.isIdle() && v.getDistance(rallyPoints.get(i)) > 100))
//				v.attackMove(rallyPoints.get(i));
//		}
	}
	
	public void vultureDrop() {
		//Build second starport
		if (starports.size() == 1 && me.gas() > 100) {
			placeAndBuild(workers.get(1), "Terran Starport");
		}
		for (Unit s : starports)
			s.buildAddon(UnitType.getUnitType("Terran Control Tower"));
		
		//Build more dropships 
		if (dropships.size() < vultures.size()/4 && me.gas() > 100 && me.minerals() > 100) {
			for (Unit s : starports)
				s.train(UnitType.getUnitType("Terran Dropship"));
		}
		
		//Train vultures
		for (Unit f : factories)
			f.train(UnitType.getUnitType("Terran Vulture"));
		
		//Rally vultures
		for (Unit v: vultures.keySet()) {
//			int i = rand.nextInt();
//			if (vultures.get(v) == 0 && v.getDistance(rallyPoints.get(i)) > 500 || (v.isIdle() && v.getDistance(rallyPoints.get(i)) > 100))
//				v.attackMove(rallyPoints.get(i));
			if (vultures.get(v) == 1) {
				if (v.getSpiderMineCount() > 0 && v.isIdle()) {
					v.useTech(TechType.SPIDER_MINES, v.getPosition());
				}
				else if (v.isIdle())
					v.attackMove(enemyLocation);
			}
		}
		
		//Load vultures while idle, move to enemy base and unload if full
		for (Unit d : dropships.keySet()) {
			if (d.getLoadedUnits().size() < 4 && d.isIdle() && dropships.get(d) == 0) {
				for (Unit v : vultures.keySet()) {
					if (!v.isLoaded() && vultures.get(v) == 0) {
						d.load(v);
					}
				}
			}
			if (d.getLoadedUnits().size() >= 4 && dropships.get(d) == 0)
				dropships.put(d, 1);
			if (dropships.get(d) == 1 && d.isIdle()) {
				d.unloadAll(enemyLocation.add(rand.nextInt(100)-50, rand.nextInt(100)-50));
				for (ROUnit vulture : d.getLoadedUnits()) {
					vultures.put((Unit) vulture, 1);
				}
				dropships.put(d, 0);
			}
		}		
	}
	
	  public void onFrame() {
		  if (toScout)
			  scout();
		  else
			  cheese();
		  
		  //Make sure have enough supply depots
		  if (me.supplyUsed() >= me.supplyTotal()-5)
			  supplyFlag = false;
		  if (!supplyFlag && me.minerals() > 100) {
			  supplyFlag = placeAndBuild(workers.get(0), "Terran Supply Depot");
		  }
		  
		  //Make sure have enough workers
		  if (me.minerals() > 50 && workers.size() < 9 && supplyDepots.size() > 0) {
			  myBase.train(UnitType.getUnitType("Terran SCV"));
		  }
		  
		  //Game mode
		  if (gameMode == 0) {
			  if (machineShops.size() > 0 && !machineShops.get(0).isResearching())
				  gameMode = 1;
			  else
				  earlyBuildOrder();
		  }
		  else if (gameMode == 1) {
			  if (dropships.size() > 0)
				  gameMode = 2;
			  else
				  intermediateBuildOrder();
		  }
		  else if (gameMode == 2) {
			  vultureDrop();
		  }
		  
		  //If have enough minerals, make barracks
		  if (me.minerals() > 1000)
			  placeAndBuild(workers.get(1), "Terran Barracks");
		  
		  //If have enough minerals, make marines
		  if (barracks.size() > 0 && me.minerals() > 300) {
			  for(Unit u: barracks) {
				  u.train(UnitType.getUnitType("Terran Marine"));
			  }
		  }
		  for (Unit u: marines) {
			  int i = 0;
			  if(u.getDistance(rallyPoints.get(i)) > 500 || (u.isIdle() && u.getDistance(rallyPoints.get(i)) > 100))
				  u.attackMove(rallyPoints.get(i));
		  }
		  
		  //Mine mine mine
		  for (Unit u: workers) {
			  if(u.isIdle()) {
				  ROUnit closestPatch = UnitUtils.getClosest(u, Game.getInstance().getMinerals());
				  u.rightClick(closestPatch);
			  }
		  	
			  Game.getInstance().drawLineMap(u.getPosition(), Position.centerOfTile(myHome), Color.GREEN);
		  }
		  
		  //Gas
		  if (refineries.size() > 0 && me.gas() < 200) {
			  if (!harvesters.get(0).isGatheringGas())
				  harvesters.get(0).rightClick(refineries.get(0));
			  if (!harvesters.get(1).isGatheringGas())
				  harvesters.get(1).rightClick(refineries.get(0));
			  if (!harvesters.get(2).isGatheringGas())
				  harvesters.get(2).rightClick(refineries.get(0));
		  }
	  }

		@Override
	  public void onStart() {
			me = Game.getInstance().self();
			myHome = me.getStartLocation();
	
			rallyPoints = new ArrayList<Position>();
			rallyPoints.add(new Position(myHome));
//			rallyPoints.add(new Position(myHome.x()/32+50, myHome.y()/32));
//			rallyPoints.add(new Position(myHome.x()/32, myHome.y()/32+50));
//			rallyPoints.add(new Position(myHome.x()/32, myHome.y()/32-50));
			
			for(ROUnit u: me.getUnits()) {
				if(u.getType().isWorker()) {
					workers.add(UnitUtils.assumeControl(u));
				} else if(u.getType().isResourceDepot()) {
					myBase = UnitUtils.assumeControl(u);
				}
			}

			cheeser = workers.remove(0);
//			cheeser.rightClick(enemyBuildings.get(0));
			harvesters.add(workers.get(2));
	  }

		@Override
	  public void onUnitCreate(ROUnit unit) {
		  if(unit.getType().isWorker() && !unit.equals(cheeser)) {
			  workers.add(UnitUtils.assumeControl(unit));
			  if (workers.size() == 4)
				  harvesters.add(workers.get(3));
			  else if (workers.size() == 5)
				  harvesters.add(workers.get(4));
		  }
		  else if(unit.getType().equals(UnitType.getUnitType("Terran Marine"))) {
			  marines.add(UnitUtils.assumeControl(unit));
		  }
		  else if(unit.getType().equals(UnitType.getUnitType("Terran Supply Depot"))) {
			  supplyDepots.add(UnitUtils.assumeControl(unit));
		  }
		  else if(unit.getType().equals(UnitType.getUnitType("Terran Barracks"))) {
			  barracks.add(UnitUtils.assumeControl(unit));
		  }	  
		  else if(unit.getType().equals(UnitType.getUnitType("Terran Factory"))) {
			  factories.add(UnitUtils.assumeControl(unit));
		  }	 
		  else if(unit.getType().equals(UnitType.getUnitType("Terran Vulture"))) {
			  vultures.put(UnitUtils.assumeControl(unit), 0);
		  }	 
		  else if(unit.getType().equals(UnitType.getUnitType("Terran Starport"))) {
			  starports.add(UnitUtils.assumeControl(unit));
		  }	 
		  else if(unit.getType().equals(UnitType.getUnitType("Terran Dropship"))) {
			  dropships.put(UnitUtils.assumeControl(unit), 0);
		  }
		  else if(unit.getType().equals(UnitType.getUnitType("Terran Refinery"))) {
			  refineries.add(UnitUtils.assumeControl(unit));
		  }
		  else if(unit.getType().equals(UnitType.getUnitType("Terran Machine Shop"))) {
			  machineShops.add(UnitUtils.assumeControl(unit));
		  }
		  else if(unit.getType().equals(UnitType.getUnitType("Terran Control Tower"))) {
			  controlTowers.add(UnitUtils.assumeControl(unit));
		  }
		  else if(unit.getType().equals(UnitType.getUnitType("Terran Armory"))) {
			  armories.add(UnitUtils.assumeControl(unit));
		  }
	  }

		@Override
	  public void onUnitDestroy(ROUnit unit) { 
			if(unit.equals(cheeser)) {
				cheeser = workers.remove(workers.size()-1);
				cheeser.rightClick(enemyLocation);
			}
			else if(unit.getType().isWorker()) {
				workers.remove(UnitUtils.assumeControl(unit));
				if (unit == harvesters.get(0)) {
					harvesters.remove(0);
					harvesters.add(0, workers.get(2));
				  }
				else if (unit == harvesters.get(1)) {
					harvesters.remove(1);
					harvesters.add(1, workers.get(3));
				}
				else if (unit == harvesters.get(2)) {
					harvesters.remove(1);
					harvesters.add(2, workers.get(4));
				}
			}
			if (enemyBuildings.contains((Unit)unit))
				enemyBuildings.remove(unit);
			else if(unit.getType().equals(UnitType.getUnitType("Terran Marine"))) {
				marines.remove(UnitUtils.assumeControl(unit));	
			}
			else if(unit.getType().equals(UnitType.getUnitType("Terran Supply Depot"))) {
				supplyDepots.remove(UnitUtils.assumeControl(unit));
			}
			else if(unit.getType().equals(UnitType.getUnitType("Terran Barracks"))) {
				barracks.remove(UnitUtils.assumeControl(unit));
	  		}	  
			else if(unit.getType().equals(UnitType.getUnitType("Terran Factory"))) {
				factories.remove(UnitUtils.assumeControl(unit));
			}	 
			else if(unit.getType().equals(UnitType.getUnitType("Terran Vulture"))) {
				vultures.remove(UnitUtils.assumeControl(unit));
			}	 
			else if(unit.getType().equals(UnitType.getUnitType("Terran Starport"))) {
				starports.remove(UnitUtils.assumeControl(unit));
			}	 
			else if(unit.getType().equals(UnitType.getUnitType("Terran Dropship"))) {
				dropships.remove(UnitUtils.assumeControl(unit));
			}
			else if(unit.getType().equals(UnitType.getUnitType("Terran Refinery"))) {
				refineries.remove(UnitUtils.assumeControl(unit));
			}
			else if(unit.getType().equals(UnitType.getUnitType("Terran Machine Shop"))) {
				machineShops.remove(UnitUtils.assumeControl(unit));
			}
			else if(unit.getType().equals(UnitType.getUnitType("Terran Control Tower"))) {
				controlTowers.remove(UnitUtils.assumeControl(unit));
			}
			else if(unit.getType().equals(UnitType.getUnitType("Terran Armory"))) {
				armories.remove(UnitUtils.assumeControl(unit));
			}
	  }

		@Override
	  public void onUnitHide(ROUnit unit) {
		  
	  }

		@Override
	  public void onUnitMorph(ROUnit unit) {
		  
	  }
		
		@Override
	  public void onUnitShow(ROUnit unit) {
			if (unit.getType().isBuilding() && unit.getPlayer().isEnemy(me)) {
				if (!enemyBuildings.contains((Unit)unit))
					enemyBuildings.add((Unit)unit);
				if (unit.getType().isResourceDepot())
					enemyLocation = unit.getPosition();
			}
	  }
		

		@Override
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
