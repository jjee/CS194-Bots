package fixedBots;

import java.util.ArrayList;
import java.util.List;

import org.bwapi.proxy.model.Order;
import org.bwapi.proxy.model.Position;
import org.bwapi.proxy.model.TilePosition;
import org.bwapi.proxy.model.Unit;

public class DropAgent {
	private Unit me, passenger;
	private List<TilePosition> path;
	private double[][] threatGrid; //shared with rest of units
	private boolean drop = false;
	private TilePosition myHome, myTarget;
	private int steps;
	
	public DropAgent(Unit dropUnit, double[][] threatGrid){
		me = dropUnit;
		myHome = dropUnit.getTilePosition();
		this.threatGrid = threatGrid;
		path = new ArrayList<TilePosition>();
	}
	
	/**
	 * Returns unit dropper WILL pick up; else null
	 */
	public Unit passenger(){ return passenger;}
	
	public boolean getStatus(){ return drop;}
	public int getSteps(){return steps;}
	
	public boolean load(Unit target){
		if(me.getLoadedUnits().contains(target)){
			passenger = null;
			return false;
		}
		passenger = target;
		me.load(passenger);
		return true;
	}
	
	/**
	 * Returns false if carrying no units yet
	 * @param loc, passenger
	 */
	public boolean setDrop(TilePosition loc){
		if(me.getLoadedUnits().isEmpty())
			return false;
		myTarget = loc;
		drop = true;
		return true;
	}
	
	/**
	 * Moves dropper along path or to home. Drop if ready.
	 */
	public void move(){
		if(drop){
			boolean unloading = me.getOrder().equals(Order.UNLOAD) || me.getOrder().equals(Order.MOVE_UNLOAD);
			if(me.getLoadedUnits().isEmpty()){
				drop = false;
				return;
			}
			
			if(path.isEmpty()&&!unloading){
				me.unloadAll(toDropPosition(myTarget));
				return;
			}
			if(close(me.getTilePosition(),next())){ 
				path.remove(0);
				steps++;
			}
			if(path.isEmpty()&&!unloading){ 
				me.unloadAll(toDropPosition(myTarget));
			}else if(!path.isEmpty()){
				me.rightClick(next());
			}
			
		}else{
			myTarget = myHome;
			if(!path.isEmpty()){
				me.rightClick(next());
			}else
				me.rightClick(myHome);
		}
	}

	private TilePosition next() {
		return path.get(0);
	}
	private Position toDropPosition(TilePosition a){
		return new Position(a.x()*32+(int)(Math.random()*100 - 100),
				a.y()*32 + (int)(Math.random()*100 - 100));
	}
	private boolean close(TilePosition a, TilePosition b){
		return a.getDistance(b) < 5;
	}
	
	/**
	 * Paths from current location to target location. Stores path in path.
	 */
	public void path(){
		path = (new AStarThreat()).getPath(threatGrid,me.getTilePosition(),myTarget);
		steps = 0;
	}
}
