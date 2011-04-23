package finalBot;

import java.util.HashSet;
import java.util.Set;

import org.bwapi.proxy.model.TilePosition;
import org.bwapi.proxy.model.Unit;
import org.bwapi.proxy.model.UnitType;

public class ArmyGroup {
	private Set<Unit> units;
	private boolean attacking;
	
	public ArmyGroup(){
		units = new HashSet<Unit>();
		attacking = false;
	}
	
	public Set<Unit> getUnits(){ return units; }
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
	
}
