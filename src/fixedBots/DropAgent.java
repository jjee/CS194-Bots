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
	private TilePosition myTarget;
	private boolean pickup = false;
	private boolean waiting = false;
	private int steps = 0;
	private boolean move = false;
	
	public DropAgent(Unit dropUnit, double[][] threatGrid){
		me = dropUnit;
		this.threatGrid = threatGrid;
		path = new ArrayList<TilePosition>();
	}
	
	/**
	 * Returns unit dropper WILL pick up; else null
	 */
	public Unit passenger(){ return passenger;}
	public boolean getDropStatus(){ return drop;}
	public boolean getPickupStatus(){ return pickup;}
	public boolean needPath(){ return waiting;}
	public boolean needMove(){ return move;}
	
	public void load(Unit u){
		if(u==null) return;
		passenger = u;
		if(!u.getTilePosition().equals(myTarget)){
			pickup = false;
		}
		if(me.getLoadedUnits().contains(u)||u.isLoaded()){
			pickup = false;
			move = false;
			waiting = false;
			path.clear();
			return;
		}
			
		if(close(me.getTilePosition(),passenger.getTilePosition())){
			if(me.isIdle()){
				me.load(u);
				pickup = false;
				move = false;
				waiting = false;
				path.clear();
				return;
			}
		}else{
			if(!pickup)
				move(u.getTilePosition());
			else
				move();
		}
		pickup = true;
	}
	
	public void load(){ load(passenger);}
	
	/**
	 * Returns false if carrying no units yet
	 * @param loc, passenger
	 */
	public boolean drop(TilePosition loc){
		if(me.getLoadedUnits().isEmpty()||loc==null)
			return false;
		if(!loc.equals(myTarget))
			drop = false;
		if(me.getLoadedUnits().isEmpty()){
			drop = false;
			return true;
		}
		if(!drop){
			System.out.println(me + "dropping at " + loc);
		}
		if(!close(me.getTilePosition(),loc)){
			if(!drop)
				move(loc);
			else
				move();
		}else{
			//boolean unloading = me.getOrder().equals(Order.UNLOAD) || me.getOrder().equals(Order.MOVE_UNLOAD);
			move = false;
			if(me.isIdle()){
				me.unloadAll(toDropPosition(me.getTilePosition()));
			}
			path.clear();
		}
		drop = true;
		return true;
	}
	
	public void drop(){ drop(myTarget);}
	/**
	 * Moves dropper along path.
	 */
	public void move(){
		if(myTarget==null) return;
		if(!path.isEmpty()){
			if(close(me.getTilePosition(),next())){
				path.remove(0);
				steps++;
				//System.out.println(steps);
				if(path.isEmpty()&&!waiting){
					move = false;
					path.clear();
					System.out.println("completed path");
				}
			}
			else
				me.rightClick(next());
		}else{
			me.rightClick(myTarget);
		}

	}
	public void move(TilePosition tp){
		myTarget = tp;
		steps = 0;
		move = true;
		waiting = true;
		move();
	}

	private TilePosition next() {
		return path.get(0);
	}
	private Position toDropPosition(TilePosition a){
		return new Position(a.x()*32+(int)(Math.random()*300 - 150),
				a.y()*32 + (int)(Math.random()*300 - 150));
	}
	private boolean close(TilePosition a, TilePosition b){
		return a.getDistance(b) < 3;
	}
	
	/**
	 * Paths from current location to target location. Stores path in path.
	 */
	public void path(){
		if(myTarget==null || !me.exists())
			return;
		System.out.println(me.getTilePosition() + " " + myTarget);
		steps = 0;
		waiting = false;
		path = (new AStarThreat()).getPath(threatGrid,me.getTilePosition(),myTarget);
	}
	
	public int steps(){
		return steps;
	}
}
