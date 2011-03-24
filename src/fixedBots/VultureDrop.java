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

public class VultureDrop extends AbstractCerebrate implements Strategy {
	
	JythonInterpreter jython = new JythonInterpreter();
	private Player me;
	private TilePosition myHome;
	private ArrayList<Position> rallyPoints = new ArrayList<Position>();;
	
	private Unit myBase;
	private Unit sparky;
	private int sparkySteps = 0;

	private HashMap<Unit, Integer> workers = new HashMap<Unit, Integer>();
	private HashMap<Unit, Integer> vultures = new HashMap<Unit, Integer>();
	private HashMap<Unit, Integer> dropships = new HashMap<Unit, Integer>();
	
	boolean toScout = true;
	private TilePosition scoutTarget;
	private HashSet<TilePosition> scouted = new HashSet<TilePosition>();
	private ArrayList<Unit> enemyBuildings = new ArrayList<Unit>();
	private ArrayList<Position> enemyBases = new ArrayList<Position>();
	private Position intermediary = null;
	
	private boolean cheeserDefenseMode = false;
	private boolean cheeserAttackMode = false;
	
	private int gameMode = 0;
	private int buildDistance = 12;
	private int numIterations = 500;
	private Random rand = new Random();
	
	@Override
	public List<Cerebrate> getTopLevelCerebrates() {
		initializeJython();
		  return Arrays.<Cerebrate>asList(jython,this);
	}
	
	public boolean placeAndBuild(String unit) {
		Unit builder = null;
		int numBuilders = 2;
		for (Unit w : workers.keySet()) {
			if (workers.get(w) == 0 && !w.isConstructing()) {
				builder = w;
				break;
			}
			if (workers.get(w) != 1)
				numBuilders--;
			if (numBuilders <= 0)
				break;
		}
		if (builder == null) {
			return false;
		}
		
		for (int i = 0; i < numIterations; i++) {
			int x = rand.nextInt(2*buildDistance)-buildDistance;
			int y = rand.nextInt(2*buildDistance)-buildDistance;
			TilePosition pos = new TilePosition(myHome.x()+x, myHome.y()+y);
			TilePosition add_pos = new TilePosition(myHome.x()+x+2, myHome.y()+y+1);
			if (builder.canBuildHere(pos, UnitType.getUnitType(unit))) {
				if (unit.equals("Terran Factory") && UnitUtils.getAllMy(UnitType.TERRAN_MACHINE_SHOP).size() == 0 || unit.equals("Terran Starport")) {
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
		}
		buildDistance++;
		return false;
	}
	
	public void scout() {
		if (scouted.containsAll(Game.getInstance().getStartLocations())) {
			toScout = false;
			sparky.rightClick(myHome);
		}
		
		if (toScout){
			for (TilePosition tp: Game.getInstance().getStartLocations()){
				if (scouted.contains(tp)) continue;
					scoutTarget = tp;
			}
			sparky.rightClick(scoutTarget);
			sparkySteps++;
			if (sparkySteps == 100)
				rallyPoints.add(sparky.getPosition());
			if (sparkySteps == 130)
				rallyPoints.add(sparky.getPosition());
			if (sparkySteps == 170)
				rallyPoints.add(sparky.getPosition());
			if (sparkySteps == 200)
				rallyPoints.add(sparky.getPosition());
		}
		
		if (scoutTarget!= null){
			if (sparky.getTilePosition().getDistance(scoutTarget) < 5) {
				scouted.add(scoutTarget);
				scoutTarget = null;
			}
		}
	}

	public void cheese() {
		//Check how the cheeser SCV is doing and respond appropriately
		cheeserDefenseMode = false;
		for (ROUnit u: Game.getInstance().getAllUnits()) {
			if (me.isEnemy(u.getPlayer()) && (sparky.equals(u.getTarget()) || sparky.equals(u.getOrderTarget()))) {
				sparky.rightClick(myHome);
				cheeserDefenseMode = true;
				cheeserAttackMode = false;
			}
			else if (enemyBuildings.contains(u)) {
				if (cheeserDefenseMode == false) {
					sparky.attackUnit(u);
					cheeserAttackMode = true;
				}
			}
		}
		if(cheeserDefenseMode == false && cheeserAttackMode == false) {
			sparky.rightClick(enemyBases.get(0));
		}
	}
	
	public void earlyBuildOrder() {	
		//SCVs
		if (workers.size() < 10) {
			myBase.train(UnitType.TERRAN_SCV);
		}	
		
		//Refinery
		if (UnitUtils.getAllMy(UnitType.TERRAN_REFINERY).size() == 0 && UnitUtils.getAllMy(UnitType.TERRAN_BARRACKS).size() > 0) {
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
				}
			}
		}
		  
		//Starport if ready
		if (UnitUtils.getAllMy(UnitType.TERRAN_FACTORY).size() > 0)
			placeAndBuild("Terran Starport");
		for (ROUnit s : UnitUtils.getAllMy(UnitType.TERRAN_STARPORT))
			UnitUtils.assumeControl(s).buildAddon(UnitType.TERRAN_CONTROL_TOWER);
	  
		//Vultures
		if (UnitUtils.getAllMy(UnitType.TERRAN_MACHINE_SHOP).size() > 0)
			for (ROUnit f : UnitUtils.getAllMy(UnitType.TERRAN_FACTORY)) {
				UnitUtils.assumeControl(f).train(UnitType.TERRAN_VULTURE);
			}
	  
		//Factory
		if (UnitUtils.getAllMy(UnitType.TERRAN_FACTORY).size() < 1) {
			  placeAndBuild("Terran Factory");
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
				UnitUtils.assumeControl(u).train(UnitType.TERRAN_MARINE);
			}
		}
	  
		//Bunker
		if (UnitUtils.getAllMy(UnitType.TERRAN_BARRACKS).size() > 0 && UnitUtils.getAllMy(UnitType.TERRAN_BUNKER).size() < 2) {
			Unit builder = null;
			int numBuilders = 2;
			for (Unit w : workers.keySet()) {
				if (workers.get(w) == 0 && !w.isConstructing()) {
					builder = w;
					break;
				}
				if (--numBuilders <= 0)
					break;
			}
			if (builder != null) {
				int x = rand.nextInt(6)-3;
				int y = rand.nextInt(6)-3;
				builder.build((new TilePosition(rallyPoints.get(1))).add(x, y), UnitType.TERRAN_BUNKER);
			}
		}
		
		//Barracks
		if (UnitUtils.getAllMy(UnitType.TERRAN_BARRACKS).size() == 0) {
			placeAndBuild("Terran Barracks");
		}	
	}
	
	public void intermediateBuildOrder() {
		//SCVs
		if (workers.size() < 16) {
			myBase.train(UnitType.TERRAN_SCV);
		}

		//Dropships
		if (UnitUtils.getAllMy(UnitType.TERRAN_CONTROL_TOWER).size() > 0) {
			UnitUtils.assumeControl(UnitUtils.getAllMy(UnitType.TERRAN_STARPORT).get(0)).train(UnitType.TERRAN_DROPSHIP);
		}
		
		//Starport
		if (UnitUtils.getAllMy(UnitType.TERRAN_STARPORT).size() == 0 && UnitUtils.getAllMy(UnitType.TERRAN_FACTORY).size() > 0) {
			placeAndBuild("Terran Starport");
		}
		for (ROUnit s : UnitUtils.getAllMy(UnitType.TERRAN_STARPORT))
			UnitUtils.assumeControl(s).buildAddon(UnitType.TERRAN_CONTROL_TOWER);
		
		//More factories
		if (UnitUtils.getAllMy(UnitType.TERRAN_FACTORY).size() < 3)
			placeAndBuild("Terran Factory");
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
				UnitUtils.assumeControl(f).train(UnitType.TERRAN_VULTURE);
			}
	}
	
	public void vultureDrop() {
		//Build second starport
		if (UnitUtils.getAllMy(UnitType.TERRAN_STARPORT).size() < 2) {
			placeAndBuild("Terran Starport");
		}
		for (ROUnit s : UnitUtils.getAllMy(UnitType.TERRAN_STARPORT))
			UnitUtils.assumeControl(s).buildAddon(UnitType.TERRAN_CONTROL_TOWER);
		
		//Build more dropships 
		if (dropships.size() < vultures.size()/4) {
			for (ROUnit s : UnitUtils.getAllMy(UnitType.TERRAN_STARPORT)) {
				UnitUtils.assumeControl(s).train(UnitType.TERRAN_DROPSHIP);
			}
		}
		
		//Train vultures
		for (ROUnit f : UnitUtils.getAllMy(UnitType.TERRAN_FACTORY)) {
			UnitUtils.assumeControl(f).train(UnitType.TERRAN_VULTURE);
		}
		
		//Vultures attack
		for (Unit v: vultures.keySet()) {
			if (vultures.get(v) == 1) {
				if (v.getSpiderMineCount() > 0 && v.isIdle()) {
					v.useTech(TechType.SPIDER_MINES, v.getPosition());
				}
				else if (v.isIdle()) {
					Position closestBase = enemyBases.get(0);
					double currDistance = v.getDistance(closestBase);
					for (Position eb : enemyBases) {
						if (v.getDistance(eb) < currDistance) {
							closestBase = eb;
							currDistance = v.getDistance(eb);
						}
					}				
					v.attackMove(closestBase);
				}
			}
		}
		
		//Load vultures while idle, move to enemy base and unload if full
		for (Unit d : dropships.keySet()) {
			Position target;
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
			else break;
			
			if (d.getLoadedUnits().size() < 4 && d.isIdle() && dropships.get(d) == 0) {
				for (Unit v : vultures.keySet()) {
					if (!v.isLoaded() && vultures.get(v) == 0) {
						d.load(v);
					}
				}
			}
			
			if (d.getLoadedUnits().size() >= 4 && dropships.get(d) == 0)
				dropships.put(d, 1);
			else if (dropships.get(d) == 1 && d.isIdle()) {
				d.move(intermediary);
			}
			if (dropships.get(d) == 1 && (d.getDistance(intermediary) < 400) || d.getDistance(target) < 300) {
				dropships.put(d, 2);
				d.stop();
			}
			
			if (dropships.get(d) == 2 && d.isIdle()) {
				d.move(target);
			}
			if (dropships.get(d) == 2 && d.getDistance(target) < 300) {
				ROUnit closestPatch = UnitUtils.getClosest(target, Game.getInstance().getMinerals());
				d.unloadAll(closestPatch.getPosition().add(rand.nextInt(100)-50, rand.nextInt(100)-50));
				for (ROUnit vulture : d.getLoadedUnits()) {
					vultures.put((Unit) vulture, 1);
				}
				dropships.put(d, 3);
			}			
			if (dropships.get(d) == 3 && d.getLoadedUnits().size() == 0)
				dropships.put(d, 0);
		}		
	}
	
	  public void onFrame() {
		  if (toScout)
			  scout();
		  else
			  workers.put(sparky, 0);
		  
		  //Make sure have enough supply depots
		  if (me.supplyUsed() >= me.supplyTotal()-5)
			  placeAndBuild("Terran Supply Depot");
		  
		  //Make sure have enough workers
		  if (me.minerals() > 50 && workers.size() < 9 && UnitUtils.getAllMy(UnitType.TERRAN_SUPPLY_DEPOT).size() > 0) {
			  myBase.train(UnitType.TERRAN_SCV);
		  }
		  
		  //Game mode
		  if (gameMode == 0) {
			  if (UnitUtils.getAllMy(UnitType.TERRAN_FACTORY).size() > 0 || UnitUtils.getAllMy(UnitType.TERRAN_STARPORT).size() > 0) {
				  gameMode = 1;
				  System.out.println("entering intermediate mode");
			  }
			  else
				  earlyBuildOrder();
		  }
		  else if (gameMode == 1) {
			  if (dropships.size() > 0) {
				  gameMode = 2;
				  System.out.println("entering late game mode");
			  }
			  else
				  intermediateBuildOrder();
		  }
		  else if (gameMode == 2) {
			  vultureDrop();
		  }
		  
		  //If have enough minerals, make more barracks
		  if (me.minerals() > 1000)
			  placeAndBuild("Terran Barracks");
		  
		  //If have enough minerals, make more marines
		  if (me.minerals() > 300) {
			  for (ROUnit u: UnitUtils.getAllMy(UnitType.TERRAN_BARRACKS)) {
				  UnitUtils.assumeControl(u).train(UnitType.TERRAN_MARINE);
			  }
		  }
		  for (ROUnit u: UnitUtils.getAllMy(UnitType.TERRAN_MARINE)) {
			  if (u.getDistance(rallyPoints.get(2)) > 500 || (u.isIdle() && u.getDistance(rallyPoints.get(2)) > 100))
				  UnitUtils.assumeControl(u).attackMove(rallyPoints.get(2));
		  }
		  for (ROUnit b: UnitUtils.getAllMy(UnitType.TERRAN_BUNKER)) {
			  if (b.getLoadedUnits().size() < 4) {
				  for (ROUnit m: UnitUtils.getAllMy(UnitType.TERRAN_MARINE))
					  UnitUtils.assumeControl(b).load(m);
			  }
		  }
		  
		  for (ROUnit v: UnitUtils.getAllMy(UnitType.TERRAN_VULTURE)) {
			  if (vultures.get(v) == 0) {
				  if (v.getSpiderMineCount() > 2 && v.isIdle()) {
					  int x = rand.nextInt(60)-30;
					  int y = rand.nextInt(60)-30;
					  UnitUtils.assumeControl(v).useTech(TechType.SPIDER_MINES, rallyPoints.get(3).add(x, y));
				  }
				  else if (v.getDistance(rallyPoints.get(0)) > 500 || (v.isIdle() && v.getDistance(rallyPoints.get(0)) > 100))
					  UnitUtils.assumeControl(v).attackMove(rallyPoints.get(0));
			  }
		  }
		  
		  //Mine mine mine
		  int numOnGas = 0;
		  for (Unit u: workers.keySet()) {
			  if (u.isIdle()) {
				  ROUnit closestPatch = UnitUtils.getClosest(u, Game.getInstance().getMinerals());
				  if (closestPatch != null)
					  u.rightClick(closestPatch);
				  workers.put(u, 0);
			  }
			  else if (u.isGatheringGas())
				  numOnGas++;
			  
			  Game.getInstance().drawLineMap(u.getPosition(), Position.centerOfTile(myHome), Color.GREEN);
		  }
		  
		  //Gas
		  if (numOnGas < 2) {
			  for (ROUnit r : UnitUtils.getAllMy(UnitType.TERRAN_REFINERY)) {
				  if (r.isCompleted()) {
					  for (Unit w : workers.keySet()) {
						  if (workers.get(w) == 0 || w.isIdle()) {
							  w.rightClick(r);
							  workers.put(w, 1);
							  numOnGas++;
						  }
						  if (numOnGas >= 2)
							  break;
					  }
				  }
			  }
		  }
	  }

		@Override
	  public void onStart() {
			me = Game.getInstance().self();
			myHome = me.getStartLocation();
			
			for(ROUnit u: me.getUnits()) {
				if(u.getType().isWorker()) {
					workers.put(UnitUtils.assumeControl(u), 0);
				} else if(u.getType().isResourceDepot()) {
					myBase = UnitUtils.assumeControl(u);
				}
			}

			for (Unit w : workers.keySet()) {
				sparky = w;
				workers.remove(w);
				break;
			}
	  }

	  public void onUnitCreate(ROUnit unit) {
		  if(unit.getType().isWorker() && !unit.equals(sparky)) {
			  workers.put(UnitUtils.assumeControl(unit), 0);
		  }
		  else if(unit.getType().equals(UnitType.getUnitType("Terran Vulture"))) {
			  vultures.put(UnitUtils.assumeControl(unit), 0);
		  }	  
		  else if(unit.getType().equals(UnitType.getUnitType("Terran Dropship"))) {
			  dropships.put(UnitUtils.assumeControl(unit), 0);
		  }
	  }

	  public void onUnitDestroy(ROUnit unit) { 
		  if(unit.equals(sparky)) {
			  for (Unit w : workers.keySet()) {
				  if (workers.get(w) == 0) {
					  sparky = w;
					  workers.remove(w);
					  break;
				  }
			  }
			  sparky.rightClick(enemyBases.get(0));
		  }
		  else if(unit.getType().isWorker()) {
			  workers.remove(UnitUtils.assumeControl(unit));
		  }
		  if (enemyBuildings.contains((Unit)unit))
			  enemyBuildings.remove(unit);
		  if (enemyBases.contains((Unit)unit))
			  enemyBases.remove(unit);
		  else if(unit.getType().equals(UnitType.getUnitType("Terran Vulture"))) {
			  vultures.remove(UnitUtils.assumeControl(unit));
		  }	  
		  else if(unit.getType().equals(UnitType.getUnitType("Terran Dropship"))) {
			  dropships.remove(UnitUtils.assumeControl(unit));
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
				if (unit.getType().isResourceDepot()) {
					enemyBases.add(unit.getPosition());
					if (intermediary == null) {
						if (unit.getPosition().x() > myBase.getPosition().x())
							intermediary = new Position(Game.getInstance().getMapWidth()*32, myBase.getPosition().y());
						else
							intermediary = new Position(0, myBase.getPosition().y());
					}
				}
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
