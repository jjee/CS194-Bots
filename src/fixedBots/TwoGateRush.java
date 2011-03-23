package fixedBots;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bwapi.proxy.model.Game;
import org.bwapi.proxy.model.Order;
import org.bwapi.proxy.model.Player;
import org.bwapi.proxy.model.ROUnit;
import org.bwapi.proxy.model.TilePosition;
import org.bwapi.proxy.model.Unit;
import org.bwapi.proxy.model.UnitType;

import edu.berkeley.nlp.starcraft.Cerebrate;
import edu.berkeley.nlp.starcraft.scripting.JythonInterpreter;
import edu.berkeley.nlp.starcraft.util.UnitUtils;

public class TwoGateRush extends EmptyFixedBot {
	JythonInterpreter jython = new JythonInterpreter();
	
	private List<Unit> myZealots;
	private List<Unit> myGateways;
	private List<Unit> myPylons;
	private Set<TilePosition> scouted;
	private List<String> buildOrder;
	private TilePosition myHome;
	private TilePosition scoutTarget;
	private Set<ROUnit> searched = new HashSet<ROUnit>();
	
	private Unit scout;
	
	private boolean holdOrders = false;
	private String lastOrder;
	String probe = "Protoss Probe";
	String pylon = "Protoss Pylon";
	String zealot = "Protoss Zealot";
	String gateway = "Protoss Gateway";
	
	private boolean buildComplete = false;
	
	
	@Override
  public List<Cerebrate> getTopLevelCerebrates() {
		initializeJython();
	  return Arrays.<Cerebrate>asList(jython,this);
  }


	@Override
  public void onFrame() {
		List<ROUnit> roWorkers = UnitUtils.getAllMy(UnitType.getUnitType(probe));
		workers.clear();
		for(ROUnit r: roWorkers){
			workers.add(UnitUtils.assumeControl(r));
		}
	  for(Unit u: workers) {
	  	if(u.isIdle()) {
	  		ROUnit closestPatch = UnitUtils.getClosest(u, Game.getInstance().getMinerals());
	  		u.rightClick(closestPatch);
	  	}
	  }  
	  scout();
	  buildNext();
	  attack();
	  
  }
	public void scout(){
		if(scout == null && workers.size() > 9)
			scout = workers.get(1);
		if(scout!=null){
			if(scoutTarget == null){
				for(TilePosition tp: myMap.getStartSpots()){
					if(!scouted.contains(tp)){
						scoutTarget = tp;
						break;
					}
				}
			}
			if(scoutTarget!=null){
				scout.rightClick(scoutTarget);
		
				if(close(scout.getTilePosition(),scoutTarget)){
					scouted.add(scoutTarget);
					scoutTarget = null;
				}
			}
		}
	}
	
	public boolean close(TilePosition t1, TilePosition t2){
		int x = Math.abs(t1.x() - t2.x());
		int y = Math.abs(t1.y() - t2.y());
		return x+y < 5;
	}
	
	public void attack(){
		List<ROUnit> roZealots = UnitUtils.getAllMy(UnitType.getUnitType(zealot));
		myZealots.clear();
		for(ROUnit r: roZealots){
			myZealots.add(UnitUtils.assumeControl(r));
		}
		
		int idleCount = 0;
		for(Unit z: myZealots){
			if(z.isIdle()&&!z.isBeingConstructed())
				idleCount++;
		}
		
		if(idleCount >=10){
			ROUnit target = null;
			Player me = Game.getInstance().self();
			for(ROUnit b: myMap.getBuildings()){
				if(me.isEnemy(b.getPlayer())){
					target = b;
					break;
				}
			}
			if(target==null){
				for(ROUnit t: myMap.getGasSpots()){
					if(!searched.contains(target)&&
							!me.canSeeUnitAtPosition(UnitType.getUnitType(probe),t.getLastKnownPosition())){
						target = t;
					}
				}
				if(target!=null)
					searched.add(target);
			}
			if(target!=null){
				for(Unit z: myZealots){
					if(z.isIdle())
						z.attackMove(target.getLastKnownPosition());
				}
			}else{
				searched.clear();
			}
		}
	}
	
	public void buildNext(){
		if(!buildComplete&&!buildOrder.isEmpty()&&!holdOrders){
			if(createUnit(buildOrder.get(0))){
				lastOrder = buildOrder.remove(0);
			}
		}else if(buildComplete){
			buildContinue();
		}
		
		if(buildOrder.isEmpty()){
			buildComplete = true;
		}
		if(holdOrders&&workers.get(0).getOrder().equals(Order.MINING_MINERALS)){
			holdOrders = false;
		}
	}
	
	public void buildContinue(){
		if(!holdOrders){
			List<ROUnit> pylons = UnitUtils.getAllMy(UnitType.getUnitType(pylon)); 
			int pylonCount = 0;
			for(ROUnit p: pylons){
				if(p.isBeingConstructed())
					pylonCount++;
			}
			boolean needSupply = getSupply()+pylonCount*16 < 4*myGateways.size();
			boolean needProbes = workers.size() < 12;
			List<ROUnit> bases = UnitUtils.getAllMy(UnitType.getUnitType("Protoss Nexus"));
			for(ROUnit b: bases){
				if(!b.getTrainingQueue().isEmpty())
					needProbes = false;
			}
			boolean newGateway = getMinerals() > myGateways.size()*100+150;
			boolean everyGatewayInUse = false;
			for(Unit g: myGateways){
				if(!g.isBeingConstructed())
					everyGatewayInUse = !g.getTrainingQueue().isEmpty();
			}
			
			newGateway = everyGatewayInUse && newGateway;
			
			if(needSupply){
				createUnit(pylon);
			}else if(needProbes){
				createUnit(probe);
			}else if(newGateway){
				createUnit(gateway);
			}else{
				createUnit(zealot);
			}
		}else{
			if(workers.get(0).getOrder().equals(Order.MINING_MINERALS)){
				holdOrders = false;
			}
		}
	}
	
	public boolean createUnit(String name){
		Unit u = null;
		if(UnitType.getUnitType(name).isBuilding())
			u = workers.get(0);
		else if(name.equals("Protoss Zealot")){
			if(myGateways.isEmpty()){
				//buildOrder.add(0, gateway);
				return false;
			}
				
			for(Unit a: myGateways){
				if(a.getTrainingQueue().isEmpty()&&!a.isBeingConstructed())
					u = a;
			}
		}
		
		if(name.equals("Protoss Probe")){
			if(Game.getInstance().self().minerals() >= 50 && getSupply() >= 1){
				  myBases.get(0).train(UnitType.getUnitType(name));
				  return true;
			  }
		}else if(name.equals("Protoss Gateway") && getMinerals() >= 150){
			if(getMinerals() >= 150){
				TilePosition tp;
				UnitType type = UnitType.getUnitType(name);
				for(int r = 8; r < 20; r++){
					tp = findBuildRadius(myHome,r,u,type);
					if(tp!=null){
						u.build(tp, type);
						holdOrders = true;
						return true;
					}
				}
				return false;
			}
		}else if(name.equals("Protoss Zealot")){
			if(getSupply()<=4*myGateways.size()){
				boolean needPylon = true;
				for(Unit p: myPylons){
					if(p.isBeingConstructed())
						needPylon = false;
				}
				if(needPylon){
					buildOrder.add(0,pylon);
					return false;
				}
			}
			if(getMinerals() >= 100 && getSupply() >= 2){
				if(u != null){
					System.out.println(UnitType.getUnitType(name));
					u.train(UnitType.getUnitType(name));
					System.out.println(u.canMake(UnitType.getUnitType(name)));
					return true;
				}
			}
		}else if(name.equals("Protoss Pylon")){
			if(getMinerals() >= 100){
				TilePosition tp;
				UnitType type = UnitType.getUnitType(name);
				for(int r = 8; r < 20; r++){
					tp = findBuildRadius(myHome,r,u,type);
					if(tp!=null){
						u.build(tp, type);
						holdOrders = true;
						return true;
					}
				}
				  return false;
			}
		}
		
		return false;
	}

	@Override
	public void onStart() {
		super.onStart();
		
		myGateways = new ArrayList<Unit>();
		myZealots = new ArrayList<Unit>();
		myPylons = new ArrayList<Unit>();
		buildOrder = new ArrayList<String>();
		scouted = new HashSet<TilePosition>();
		scouted.add(myHome);
		setUpBuildOrder();
		myHome = Game.getInstance().self().getStartLocation();;
	}
	

	@Override
	public void onUnitCreate(ROUnit unit) {
		if(unit.getType().getName().equals("Protoss Probe"))
			workers.add(UnitUtils.assumeControl(unit));
		
		if(unit.getType().getName().equals(gateway) && !myGateways.contains(unit)){
			myGateways.add(UnitUtils.assumeControl(unit));
			holdOrders = false;
		}
		
		if(unit.getType().getName().equals(pylon)){
			myPylons.add(UnitUtils.assumeControl(unit));
			holdOrders = false;
		}
		if(unit.getType().getName().equals(zealot)){
			myZealots.add(UnitUtils.assumeControl(unit));
		}
  }
	
	@Override
	public void onUnitShow(ROUnit unit){
		
		if(unit.getType().isBuilding()){
			myMap.addBuilding(unit);
		}
		if(unit.getTilePosition().equals(scoutTarget))
			scoutTarget = null;
	}

	@Override
	public void setUpBuildOrder() {
		// TODO Auto-generated method stub
		buildOrder.add(probe);
		buildOrder.add(probe);
		buildOrder.add(probe);
		buildOrder.add(probe);
		buildOrder.add(pylon);
		buildOrder.add(gateway);
		buildOrder.add(gateway);
		buildOrder.add(probe);
		buildOrder.add(probe);
		buildOrder.add(zealot);
		buildOrder.add(probe);
		buildOrder.add(pylon);
		for(int i = 0; i< 6; i++){
			buildOrder.add(zealot);
		}
		buildOrder.add(pylon);
		for(int i = 0; i< 4; i++){
			buildOrder.add(zealot);
		}
	}
	
	public TilePosition findBuildRadius(TilePosition c, int radius, Unit u, UnitType t){
		TilePosition tp;
		TilePosition next;
		TilePosition prev;
		TilePosition top;
		TilePosition bottom;
		for(int y = -radius; y <= radius; y+=1){
			for(int x = -radius; x <= radius; x+=1){
				tp = new TilePosition(c.x()+x,c.y()+y);
				next = new TilePosition(c.x()+x+2,c.y()+y);
				prev = new TilePosition(c.x()+x-2,c.y()+y);
				top = new TilePosition(c.x()+x,c.y()+y+2);
				bottom = new TilePosition(c.x()+x,c.y()+y-2);
				if(t.getName().equals(pylon)){
					if(u.canBuildHere(tp, t)&&u.canBuildHere(next, t)&&
							u.canBuildHere(prev, t)&& u.canBuildHere(bottom, t) && u.canBuildHere(top,t))
						return tp;
					
				}else if(u.canBuildHere(tp, t))
					return tp;
			}
		}
		return null;
	}
}


