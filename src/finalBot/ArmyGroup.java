package finalBot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bwapi.proxy.model.Color;
import org.bwapi.proxy.model.Game;
import org.bwapi.proxy.model.Order;
import org.bwapi.proxy.model.Position;
import org.bwapi.proxy.model.ROUnit;
import org.bwapi.proxy.model.TilePosition;
import org.bwapi.proxy.model.Unit;
import org.bwapi.proxy.model.UnitType;

import edu.berkeley.nlp.starcraft.util.UnitUtils;

public class ArmyGroup {
	private Set<Unit> units;
	private boolean attacking;
	private Position rallyPoint;
	private boolean gathering = false;
	private Set<Unit> positioned;
	private int rallyDist =150;
	
	public ArmyGroup(){
		units = new HashSet<Unit>();
		attacking = false;
		positioned = new HashSet<Unit>();
	}
	
	public void setRally(Position p){ rallyPoint = p; }
	public Position getRally(){ return rallyPoint; }
	
	public Set<Unit> getUnits(){ 
		Set<Unit> retUnits = new HashSet<Unit>();
		for(Unit u: units){
			if(u.isCompleted())
				retUnits.add(u);
		}
		return units; 
	}
	/**
	 * 
	 * @param type
	 * @return units of specified type
	 */
	public Set<Unit> getUnits(UnitType type){
		Set<Unit> retUnits = new HashSet<Unit>();
		for(Unit u: units){
			if(u.getType().equals(type))
				retUnits.add(u);
		}
		return retUnits;
	}
	
	/**
	 * Calculates avg tileposition for group
	 * @return avg tileposition
	 */
	public TilePosition getLocation(){
		return Tools.calcAvgLoc(units);
	}
	
	public void add(Unit u) { units.add(u); }
	
	public void remove(Unit u) { units.remove(u);}
	
	public void setAttack(boolean b) { attacking = b; }
	public boolean isAttacking() { return attacking; }
	
	public boolean underAttack() {
		List<ROUnit> enemies = Tools.enemyUnits();
		for(ROUnit e : enemies){
			if(e.getTarget()!=null && units.contains(e.getTarget())){
				return true;
			}
		}
		for(ROUnit u : units){
			if(u.getTarget()!=null){
				return true;
			}
		}
		return false;
	}
	
	public void rally(){
		if(rallyPoint==null) return;
		for(Unit u: units){
			if(u.isIdle() && u.getPosition().getDistance(rallyPoint) > 100)
				u.attackMove(rallyPoint);
			else if (!positioned.contains(u)&&u.getPosition().getDistance(rallyPoint) <rallyDist){
				u.rightClick(u.getPosition());
				positioned.add(u);
				rallyDist+=2;
			}
		}
	}
	
	public ROUnit selectTarget(){
		List<ROUnit> enemies = Tools.enemyUnits();
		Set<ROUnit> cantTarget = new HashSet<ROUnit>();
		if(enemies==null||enemies.isEmpty()) return null;
		
		//remove units that can't be targeted
		for(ROUnit e: enemies){
			if(e.isCloaked() && !e.isDetected())
				cantTarget.add(e);
		}
		for(ROUnit e: cantTarget){
			enemies.remove(e);
		}
		
		List<ROUnit> attackingUnits = new ArrayList<ROUnit>();
		for(ROUnit e : enemies){
			if(e.getTarget()!=null && units.contains(e.getTarget())){
				attackingUnits.add(e);
			}
		}
		if(attackingUnits.isEmpty()) {
			return Tools.findClosest(enemies, getLocation());
		} else {
			return Tools.findClosest(attackingUnits, getLocation());
		}
	}
	
	public boolean gather(){
		boolean retVal = true;
		if(UnitUtils.groupRadius(units) > 150){
			if(units.isEmpty()) return false;
			for(Unit u: units){
				if(u.getTilePosition().getDistance(getLocation()) > 3){
					u.rightClick(getLocation());
					retVal = false;
				}
			}
		}
		gathering = !retVal;
		return retVal;
	}
	
	public boolean isGathering(){ return gathering; }
}
