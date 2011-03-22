package assignment0;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.bwapi.proxy.ProxyBot;
import org.bwapi.proxy.ProxyBotFactory;
import org.bwapi.proxy.ProxyServer;
import org.bwapi.proxy.model.Color;
import org.bwapi.proxy.model.Game;
import org.bwapi.proxy.model.Player;
import org.bwapi.proxy.model.Position;
import org.bwapi.proxy.model.ROUnit;
import org.bwapi.proxy.model.TilePosition;
import org.bwapi.proxy.model.Unit;
import org.bwapi.proxy.model.UnitType;

import edu.berkeley.nlp.starcraft.AbstractCerebrate;
import edu.berkeley.nlp.starcraft.Cerebrate;
import edu.berkeley.nlp.starcraft.Strategy;
import edu.berkeley.nlp.starcraft.overmind.Overmind;
import edu.berkeley.nlp.starcraft.scripting.Command;
import edu.berkeley.nlp.starcraft.scripting.JythonInterpreter;
import edu.berkeley.nlp.starcraft.scripting.Thunk;
import edu.berkeley.nlp.starcraft.util.UnitUtils;

public class MineMineMine extends AbstractCerebrate implements Strategy {
	JythonInterpreter jython = new JythonInterpreter();
	private TilePosition myHome;
	private Unit myBase;
	private final List<Unit> workers = new ArrayList<Unit>();
	private int workersBuilt;
	private List<Unit> attackers = new ArrayList<Unit>();
	private Unit myBuilder;
	private TilePosition buildPos;
	private List<Unit> gateways = new ArrayList<Unit>();
	private Player mySelf;
	private int pylons;
	private int off;
	private Unit runner;
	private TilePosition enemy;
	private int cannons;
  

	@Override
  public List<Cerebrate> getTopLevelCerebrates() {
		initializeJython();
	  return Arrays.<Cerebrate>asList(jython,this);
  }


	@Override
  public void onFrame() {
	  runner = workers.get(1);
	  for(Unit u: workers) {
	  	if(u.isIdle() && u.getID() != runner.getID()) {
	  		ROUnit closestPatch = UnitUtils.getClosest(u, Game.getInstance().getMinerals());
	  		u.rightClick(closestPatch);
	  	}
	  	Game.getInstance().drawLineMap(u.getPosition(), Position.centerOfTile(myHome), Color.GREEN);
	  }
	  if(off > 0 && !runner.isConstructing() && cannons < 1)
		  runner.rightClick(enemy);
	  else if(!runner.isConstructing() && cannons < 1)
		  runner.rightClick(enemy);
	  if(mySelf.supplyUsed() >= 20 && Game.getInstance().canBuildHere(myBuilder, buildPos.add(off,0), UnitType.PROTOSS_FORGE)) {
		  if(Game.getInstance().canMake(myBuilder, UnitType.PROTOSS_FORGE))
			  myBuilder.build(buildPos.add(off,0), UnitType.PROTOSS_FORGE);
	  }
	  /*
	  else if(mySelf.supplyUsed() >= 24 && Game.getInstance().canBuildHere(myBuilder, buildPos.add(off,4), UnitType.PROTOSS_GATEWAY)) {
		  if(Game.getInstance().canMake(myBuilder, UnitType.PROTOSS_GATEWAY))
			  myBuilder.build(buildPos.add(off,4), UnitType.PROTOSS_GATEWAY);
	  }
	  */
	  else if(pylons >= 1 && Game.getInstance().canBuildHere(runner, enemy, UnitType.PROTOSS_PYLON)) {
		  if(Game.getInstance().canMake(runner, UnitType.PROTOSS_PYLON))
			  runner.build(enemy, UnitType.PROTOSS_PYLON);
	  }
	  else if(mySelf.supplyUsed() >= 20 && Game.getInstance().canBuildHere(runner,enemy.add(-2,off),UnitType.PROTOSS_PHOTON_CANNON)) {
		  if(Game.getInstance().canMake(runner, UnitType.PROTOSS_PHOTON_CANNON)) {
			  runner.build(enemy.add(-2,off), UnitType.PROTOSS_PHOTON_CANNON);
		  }
	  }
	  else if(mySelf.supplyUsed() >= 20 && Game.getInstance().canBuildHere(runner,enemy.add(0,off),UnitType.PROTOSS_PHOTON_CANNON)) {
		  if(Game.getInstance().canMake(runner, UnitType.PROTOSS_PHOTON_CANNON)) {
			  runner.build(enemy.add(0,off), UnitType.PROTOSS_PHOTON_CANNON);
		  }
	  }
	  else if(mySelf.supplyUsed() >= 20 && Game.getInstance().canBuildHere(runner,enemy.add(2,off),UnitType.PROTOSS_PHOTON_CANNON)) {
		  if(Game.getInstance().canMake(runner, UnitType.PROTOSS_PHOTON_CANNON)) {
			  runner.build(enemy.add(2,off), UnitType.PROTOSS_PHOTON_CANNON);
		  }
	  }
	  else if(mySelf.supplyUsed() >= 20 && Game.getInstance().canBuildHere(runner,enemy.add(4,off),UnitType.PROTOSS_PHOTON_CANNON)) {
		  if(Game.getInstance().canMake(runner, UnitType.PROTOSS_PHOTON_CANNON)) {
			  runner.build(enemy.add(4,off), UnitType.PROTOSS_PHOTON_CANNON);
		  }
	  }
	  else if(mySelf.supplyUsed() >= 20 && Game.getInstance().canBuildHere(runner,enemy.add(-4,off),UnitType.PROTOSS_PHOTON_CANNON)) {
		  if(Game.getInstance().canMake(runner, UnitType.PROTOSS_PHOTON_CANNON)) {
			  runner.build(enemy.add(-4,off), UnitType.PROTOSS_PHOTON_CANNON);
		  }
	  }
	  else if(mySelf.supplyUsed() >= 16 && mySelf.supplyUsed() >= (2*pylons*8+18)-2 && Game.getInstance().canBuildHere(myBuilder, buildPos.add(0,pylons*2+2), UnitType.PROTOSS_PYLON)) {
		  if(Game.getInstance().canMake(myBuilder, UnitType.PROTOSS_PYLON)) {
			  myBuilder.build(buildPos.add(0,pylons*2+2), UnitType.PROTOSS_PYLON);
		  }
	  }
	  else if(Game.getInstance().canMake(myBase, UnitType.PROTOSS_PROBE) && workers.size() < 16) {
		  myBase.train(UnitType.PROTOSS_PROBE);
		  workersBuilt++;
		  ROUnit closestPatch = UnitUtils.getClosest(myBuilder, Game.getInstance().getMinerals());
	  	  myBuilder.rightClick(closestPatch);
	  }
	  /*
	  if(myHome.x()-10 < 0) {
		  if(mySelf.supplyUsed() >= 16 && Game.getInstance().canBuildHere(myBuilder, buildPos.add(0,2), UnitType.PROTOSS_PYLON)) {
			  if(Game.getInstance().canMake(myBuilder, UnitType.PROTOSS_PYLON))
				  myBuilder.build(buildPos.add(0,2), UnitType.PROTOSS_PYLON);
		  }
		  else if(mySelf.supplyUsed() >= 20 && Game.getInstance().canBuildHere(myBuilder, buildPos.add(2,0), UnitType.PROTOSS_GATEWAY)) {
			  if(Game.getInstance().canMake(myBuilder, UnitType.PROTOSS_GATEWAY))
				  myBuilder.build(buildPos.add(2,0), UnitType.PROTOSS_GATEWAY);
		  }
		  else if(mySelf.supplyUsed() >= 24 && Game.getInstance().canBuildHere(myBuilder, buildPos.add(2,4), UnitType.PROTOSS_GATEWAY)) {
			  if(Game.getInstance().canMake(myBuilder, UnitType.PROTOSS_GATEWAY))
				  myBuilder.build(buildPos.add(2,4), UnitType.PROTOSS_GATEWAY);
		  }
		  else if(mySelf.supplyUsed() >= 32 && Game.getInstance().canBuildHere(myBuilder, buildPos.add(0,4), UnitType.PROTOSS_PYLON)) {
			  if(Game.getInstance().canMake(myBuilder, UnitType.PROTOSS_PYLON))
				  myBuilder.build(buildPos.add(0,4), UnitType.PROTOSS_GATEWAY);
		  }
		  else if(mySelf.supplyUsed() >= 32 && mySelf.supplyUsed() >= mySelf.supplyTotal()-8 && Game.getInstance().canBuildHere(myBuilder, buildPos.add(0,pylons*2+2), UnitType.PROTOSS_PYLON)) {
			  if(Game.getInstance().canMake(myBuilder, UnitType.PROTOSS_PYLON))
				  myBuilder.build(buildPos.add(0,pylons*2+2), UnitType.PROTOSS_PYLON);	  
		  }
		  else if(!gateways.isEmpty()) {
			  for(Unit g: gateways) {
				  if(g.getTrainingQueue().size() < 5 && Game.getInstance().canMake(g, UnitType.PROTOSS_ZEALOT))
					  g.train(UnitType.PROTOSS_ZEALOT);
			  }
		  }
		  else if(Game.getInstance().canMake(myBase, UnitType.PROTOSS_PROBE) && workers.size() < 16) {
			  myBase.train(UnitType.PROTOSS_PROBE);
			  workersBuilt++;
			  ROUnit closestPatch = UnitUtils.getClosest(myBuilder, Game.getInstance().getMinerals());
		  	  myBuilder.rightClick(closestPatch);
		  }
	  }*/
	  /*
	  if(mySelf.minerals() >= 100 && workers.size() >= 7 && Game.getInstance().isBuildable(buildPos.x()+pylons*2,buildPos.y()+pylons*2))
		  myBuilder.build(buildPos.add(0,pylons*2), UnitType.PROTOSS_PYLON);
	  else if(mySelf.minerals() >= 150 && workers.size() >= 9 && Game.getInstance().isBuildable(buildPos.x()+3,buildPos.y())) {
		  myBuilder.build(buildPos.add(3,0), UnitType.PROTOSS_GATEWAY);
	  }
	  else {
		  ROUnit closestPatch = UnitUtils.getClosest(myBuilder, Game.getInstance().getMinerals());
		  myBuilder.rightClick(closestPatch);
	  }
	  if(mySelf.minerals() >= 50 && workers.size() < 12 && mySelf.supplyUsed() < mySelf.supplyTotal()) {
		  myBase.train(UnitType.PROTOSS_PROBE);
		  workersBuilt++;
	  }
	  /*
	  else if(mySelf.minerals() >= 100 && myGateway.getRemainingBuildTime() == 0)
		  myGateway.train(UnitType.PROTOSS_ZEALOT);
	  */
  }

	@Override
  public void onStart() {
		workersBuilt = 0;
		mySelf = Game.getInstance().self();
		myHome = Game.getInstance().self().getStartLocation();
		for(ROUnit u: Game.getInstance().self().getUnits()) {
			if(u.getType().isWorker()) {
				workers.add(UnitUtils.assumeControl(u));
			} else if(u.getType().isResourceDepot()) {
				myBase = UnitUtils.assumeControl(u);
			}
		}
		if(myHome.x()-10 < 0) {
			buildPos = myHome.add(4,2);
			off = 3;
			enemy = new TilePosition(50,50);
		}
		else {
			buildPos = myHome.add(-3,-5);
			off = -3;
			enemy = new TilePosition(9,15);
		}
		myBuilder = workers.get(0);
  }

	@Override
  public void onUnitCreate(ROUnit unit) {
	  if(unit.getType().isWorker()) {
		  workers.add(UnitUtils.assumeControl(unit));
	  }
	  else if(!unit.getType().isBuilding())
		  attackers.add(UnitUtils.assumeControl(unit));
	  else if(unit.getType().supplyProvided() > 0 && !unit.getType().isResourceDepot())
		  pylons++;
	  else if(unit.getHitPoints() == 100)
		  cannons++;
  }

	@Override
  public void onUnitDestroy(ROUnit unit) {
	  
  }

	@Override
  public void onUnitHide(ROUnit unit) {
	  
  }

	@Override
  public void onUnitMorph(ROUnit unit) {
	  
  }
	
	@Override
  public void onUnitShow(ROUnit unit) {
	  
  }
	

	@Override
  public void onEnd(boolean isWinnerFlag) {
	  
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
