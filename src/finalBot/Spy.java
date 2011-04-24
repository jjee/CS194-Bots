package finalBot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bwapi.proxy.model.Game;
import org.bwapi.proxy.model.ROUnit;
import org.bwapi.proxy.model.TilePosition;
import org.bwapi.proxy.model.Unit;
import org.bwapi.proxy.model.WeaponType;

public class Spy extends Overseer {
	private Map<TilePosition,Long> scouted; //maps areas scouted to time scouted?
	private Set<ROUnit> enemyUnits;
	private Unit myScout;
	private long MINUTE_IN_MS = 60000;
	private int PIXEL_SCOUT_RANGE = 1000;
	
	public Spy() {
		scouted = new HashMap<TilePosition,Long>();
		enemyUnits = new HashSet<ROUnit>();
	}
	
	// grabs SCV from builder for scouting
	private void assignScout(TilePosition tp) {
		myScout = builder.pullWorker(tp);
	}
	
	// returns SCV to builder or removes if scout destroyed
	public void unassignScout() {
		if(myScout.exists())
			builder.addWorker(myScout);
		myScout = null;
	}
	
	// scans nearby area of tp for enemies
	public void scan(TilePosition tp) {
		if(myScout == null)
			assignScout(tp);
		if(Tools.close((ROUnit) myScout, tp, myScout.getType().sightRange()/32)) {
			scouted.put(tp, System.currentTimeMillis());
			return;
		}
		myScout.move(tp);
	}
	
	// uses scout to find enemy
	public void findEnemy() {
		if(!myScout.isIdle())
			return;
		for(TilePosition tp : Game.getInstance().getStartLocations()) {
			if(!scouted.containsKey(tp) || scouted.get(tp) < System.currentTimeMillis()-MINUTE_IN_MS) {
				scan(tp);
				return;
			}
		}
	}
	
	// scouts enemy's nearby potential expansions locations 
	public void scoutEnemy() {
		if(!myScout.isIdle())
			return;
		List<TilePosition> potentialExpansions = new LinkedList<TilePosition>();
		for(ROUnit u : Game.getInstance().getGeysers()) {
			if(Tools.close((Unit) u, enemyGroundUnits(), PIXEL_SCOUT_RANGE))
				potentialExpansions.add(u.getLastKnownTilePosition());
		}
		for(TilePosition tp : potentialExpansions) {
			if(!scouted.containsKey(tp) || scouted.get(tp) < System.currentTimeMillis()-MINUTE_IN_MS) {
				scan(tp);
				return;
			}
		}
	}

	// remove buildings if not there anymore
	public void updateEnemyUnits() {
		Set<ROUnit> toRemove = new HashSet<ROUnit>();
		for(ROUnit u : enemyUnits) {
			if(Game.getInstance().self().canSeeUnitAtPosition(u.getType(),u.getLastKnownPosition()) && !u.isVisible())
				toRemove.add(u);
		}
		enemyUnits.removeAll(toRemove);
	}
	
	// adds enemy unit to set
	public void addEnemyUnit(ROUnit u) {
		enemyUnits.add(u);
	}
	
	// removes enemy from set
	public void removeEnemyUnit(ROUnit unit) {
		enemyUnits.remove(unit);
	}
	
	// enemy armed air unit count
	public int airForces() {
		int airUnits = 0;
		for(ROUnit u : enemyUnits) {
			if(u.getType().isFlyer() && u.getType().canAttack())
				airUnits++;
		}
		return airUnits;
 	}

	// enemy armed ground unit count
	public int groundForces() {
		int groundUnits = 0;
		for(ROUnit u : enemyUnits) {
			if(!u.getType().isFlyer() && u.getType().canAttack())
				groundUnits++;
		}
		return groundUnits;
	}
	
	// enemy cloaked unit count
	public int cloakedForces() {
		int cloakedUnits = 0;
		for(ROUnit u : enemyUnits) {
			if(u.getType().isCloakable() || u.isCloaked() || u.getType().isBurrowable())
				cloakedUnits++;
		}
		return cloakedUnits;
	}
	
	// enemy melee attackers count
	public int meleeForces() {
		int meleeUnits = 0;
		for(ROUnit u : enemyUnits) {
			WeaponType weapon = u.getType().groundWeapon();
			if(!weapon.equals(WeaponType.NONE) && weapon.maxRange() <= 15)
				meleeUnits++;
		}
		return meleeUnits;
	}
	
	// enemy ground ranged attackers count
	public int rangedForces() {
		int rangedUnits = 0;
		for(ROUnit u : enemyUnits) {
			WeaponType weapon = u.getType().groundWeapon();
			if(!weapon.equals(WeaponType.NONE) && weapon.maxRange() > 15)
				rangedUnits++;
		}
		return rangedUnits;
	}
	
	// existence of a small force
	public int smallForces() {
		return 0;
	}
	
	public int largeForces() {
		return 0;
	}
	
	public void act(){
		if(enemyBuildings() <= 0)
			findEnemy();
		else
			scoutEnemy();
	}
	
	// enemy building count
	public int enemyBuildings() {
		int buildings = 0;
		for(ROUnit u : enemyUnits) {
			if(u.getType().isBuilding());
				buildings++;
		}
		return buildings;
	}
	
	// list of units on ground, includes both buildings and forces
	public List<ROUnit> enemyGroundUnits() {
		List<ROUnit> groundUnits = new LinkedList<ROUnit>();
		for(ROUnit u : enemyUnits) {
			if(!u.getType().isFlyer())
				groundUnits.add(u);
		}
		return groundUnits;
	}
}
