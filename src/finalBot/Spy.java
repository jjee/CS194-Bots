package finalBot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bwapi.proxy.model.Game;
import org.bwapi.proxy.model.Order;
import org.bwapi.proxy.model.Position;
import org.bwapi.proxy.model.ROUnit;
import org.bwapi.proxy.model.TilePosition;
import org.bwapi.proxy.model.Unit;
import org.bwapi.proxy.model.UnitType;
import org.bwapi.proxy.model.WeaponType;

import edu.berkeley.nlp.starcraft.util.UnitUtils;

public class Spy extends Overseer {
	private Map<TilePosition,Integer> scouted; //maps areas scouted to time scouted?
	private Set<ROUnit> enemyUnits;
	private Unit myScout;
	private TilePosition myHome; 
	private static final int FRAMES_PER_MIN = 1440;
	private static final int PIXEL_SCOUT_RANGE = 1000;
	
	public Spy() {
		scouted = new HashMap<TilePosition,Integer>();
		enemyUnits = new HashSet<ROUnit>();
		myHome = Game.getInstance().self().getStartLocation();
	}
	
	// grabs SCV from builder for scouting
	private void assignScout(TilePosition tp) {
		myScout = builder.pullWorker(tp);
		myScout.stop();
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
			scouted.put(tp, Game.getInstance().getFrameCount());
			return;
		}
		myScout.move(tp);
	}
	
	// uses scout to find enemy
	public void findEnemy() {
		if(!myScout.isStopped())
			return;
		for(TilePosition tp : Game.getInstance().getStartLocations()) {
			if(!scouted.containsKey(tp) || scouted.get(tp) < Game.getInstance().getFrameCount()-FRAMES_PER_MIN) {
				scan(tp);
				return;
			}
		}
	}
	
	// scouts enemy's nearby potential expansions locations 
	public void scoutEnemy() {
		if(!myScout.isStopped())
			return;
		List<TilePosition> potentialExpansions = new LinkedList<TilePosition>();
		for(ROUnit u : Game.getInstance().getGeysers()) {
			if(Tools.close((Unit) u, enemyGroundUnits(), PIXEL_SCOUT_RANGE))
				potentialExpansions.add(u.getLastKnownTilePosition());
		}
		for(TilePosition tp : potentialExpansions) {
			if(!scouted.containsKey(tp) || scouted.get(tp) < Game.getInstance().getFrameCount()-FRAMES_PER_MIN) {
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
	
	public int getNumberOf(UnitType type){
		int count = 0;
		for(ROUnit u : enemyUnits){
			if(u.getType()==type)
				count++;
		}
		return count;
	}
	
	public int getStaticDef(){
		int count = 0;
		for(ROUnit u : enemyUnits){
			if(u.getType().isBuilding() && 
					(u.getType().canAttack() || u.getType() == UnitType.TERRAN_BUNKER))
				count++;
		}
		return count;
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
		if(myScout==null || !myScout.isVisible()) {
			if(UnitUtils.getAllMy(UnitType.TERRAN_SCV).size() >= 11){
				assignScout(myHome);
				return;
			}
		}
		if(myScout==null){
			System.out.println("no scout");
			return;
		}
		if(myScout != null && !enemyGroundUnits().isEmpty() && !myScout.getOrder().equals(Order.MOVE)) {
			//int maxAtkRange = maxGroundRange();
			int maxAtkRange = 15;
			if(Tools.close(myScout, enemyGroundUnits(), maxAtkRange)) {
				if(!myScout.getOrder().equals(Order.PATROL))
					myScout.patrol(new Position(myHome));
			}
			else if(myScout.getOrder().equals(Order.PATROL))
				myScout.stop();
		}
		
		if(enemyBuildings() <= 0){
			findEnemy();
			System.out.println("looking for enemy");
		}else{
			scoutEnemy();
			System.out.println("scouting");
		}
		if(myScout.isStopped()) {
			ROUnit target = Tools.findClosest(enemyBases(),myScout.getTilePosition());
			if(target!=null)
				myScout.attack(target);
			else{
				
			}
		}
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
	
	// max atk range of ground units
	public int maxGroundRange() {
		int maxAtkRange = 15;
		for(ROUnit u : enemyGroundUnits()) {
			WeaponType ground_weapon = u.getType().groundWeapon();
			if(ground_weapon == null)
				continue;
			maxAtkRange = Math.max(maxAtkRange, ground_weapon.maxRange());
		}
		return maxAtkRange;
	}
	
	public List<ROUnit> enemyBases() {
		List<ROUnit> bases = new LinkedList<ROUnit>();
		for(ROUnit u : enemyUnits) {
			if(u.getType().isResourceDepot())
				bases.add(u);
		}
		return bases;
	}
}
