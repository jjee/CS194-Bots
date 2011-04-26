package finalBot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bwapi.proxy.model.Bwta;
import org.bwapi.proxy.model.Chokepoint;
import org.bwapi.proxy.model.Game;
import org.bwapi.proxy.model.Order;
import org.bwapi.proxy.model.Player;
import org.bwapi.proxy.model.Position;
import org.bwapi.proxy.model.ROUnit;
import org.bwapi.proxy.model.TechType;
import org.bwapi.proxy.model.TilePosition;
import org.bwapi.proxy.model.Unit;
import org.bwapi.proxy.model.UnitType;

import edu.berkeley.nlp.starcraft.util.UnitUtils;

public class Commander extends Overseer {
	private Set<Unit> armyUnits; //all units under attacker control
	private List<ArmyGroup> marineGroups;
	private Map<Unit,Unit> medics;
	private Set<Unit> peasants;
	private int attackCount; //number of times we tried to attack
	private Player me;
	
	public Commander(){
		armyUnits = new HashSet<Unit>();
		marineGroups = new ArrayList<ArmyGroup>();
		peasants = new HashSet<Unit>();
		medics = new HashMap<Unit,Unit>();
		attackCount = 0;
		me = Game.getInstance().self();
	}
	
	public boolean useBunkers(Unit u){
		List<ROUnit> bunkers = UnitUtils.getAllMy(UnitType.TERRAN_BUNKER);
		boolean loaded = false;
		for (ROUnit b: bunkers) {
			if (u.isCompleted()&&b.isCompleted() &&b.getLoadedUnits().size() < 4) {
				UnitUtils.assumeControl(b).load(u);
				loaded = true;
			}
		}
		return loaded;
	}
	
	public void addAttacker(Unit u) { //add a unit for attacker class to use
		armyUnits.add(u);
		
		//add to some group
		UnitType type = u.getType();
		if(type==UnitType.TERRAN_MARINE || type==UnitType.TERRAN_FIREBAT){
			ArmyGroup choice = null;
			
			for(ArmyGroup g: marineGroups){
				if(!g.isAttacking()) {
					choice = g;
				}
			}
			
			if(choice==null){
				choice = new ArmyGroup();
				Position chokeCenter = closestChoke(builder.getHome());
				choice.setRally(Tools.randomNearby(chokeCenter,100));
			}
			choice.add(u);
			marineGroups.add(choice);
		} else if(type==UnitType.TERRAN_MEDIC){
			medics.put(u,null);
		}
	}
	
	public Position closestChoke(ROUnit u){
		Set<Chokepoint> chokes = Bwta.getInstance().getChokepoints();
		Position best = null;
		double bestDist = 9001;
		for(Chokepoint c : chokes){
			double dist = c.getCenter().getDistance(u.getPosition());
			if(dist < bestDist){
				bestDist = dist;
				best = c.getCenter();
			}
		}
		return best;
	}
	
	public void removeAttacker(Unit u) { //remove unit from attacker class
		armyUnits.remove(u);
		if(u.getType()==UnitType.TERRAN_MARINE || u.getType()==UnitType.TERRAN_FIREBAT){
			for(ArmyGroup g: marineGroups){
				if(g.getUnits().contains(u)){
					g.remove(u);
					return;
				}
			}
		} else if(u.getType()==UnitType.TERRAN_MEDIC){
			medics.remove(u);
		}
	}

	private void attack(ArmyGroup g, TilePosition tp) {//have group attack
		Position pos = new Position(tp.x()*Tools.TILE_SIZE,tp.y()*Tools.TILE_SIZE);
		if(!g.isAttacking()){
			g.setAttack(true);
			attackCount++;
		}
		
		Set<Unit> marines = g.getUnits(UnitType.TERRAN_MARINE);
		Set<Unit> firebats = g.getUnits(UnitType.TERRAN_FIREBAT);
		
		for(Unit m: marines){
			if(m.isIdle())
				m.attackMove(pos);
		}
		for(Unit f: firebats){
			if(f.isIdle())
				f.attackMove(pos);
		}
	}
	
	private void heal(){
		for(Unit c : medics.keySet()){
			if(medics.get(c)==null || !medics.get(c).exists()){
				medics.put(c, healTarget());
			}
			if(medics.get(c)==null || !medics.get(c).exists()){
				if(c.isIdle()){
					c.rightClick(builder.getHome());
				}
				return;
			}
			if(5 < c.getTilePosition().getDistance(medics.get(c).getTilePosition())){
				c.rightClick(medics.get(c).getTilePosition());
				//System.out.println(medics.get(c).getTilePosition());
			}
		}
	}
	
	private Unit healTarget(){
		List<ArmyGroup> attackingGroups = new ArrayList<ArmyGroup>();
		for(ArmyGroup g: marineGroups){
			if(g.isAttacking() && !g.getUnits().isEmpty()){
				attackingGroups.add(g);
			}
		}
		if(attackingGroups.isEmpty()) attackingGroups = marineGroups;
		if(attackingGroups.isEmpty()) return null;
		int randomGroup = (int) (Math.random()*attackingGroups.size());
		int randomMarine = (int) (Math.random()*attackingGroups.get(randomGroup).getUnits().size());
		ArmyGroup g = attackingGroups.get(randomGroup);
		int i = 0;
		Unit choice = null;
		for(Unit m: g.getUnits()){
			if(i==randomMarine) choice = m;
			i++;
		}
		
		return choice;
	}
	
	private void retreat(ArmyGroup g) {//retreat group
		for(Unit u: g.getUnits()){
			List<ROUnit> centers = UnitUtils.getAllMy(UnitType.TERRAN_COMMAND_CENTER);
			if(!centers.isEmpty() && u.isIdle())
				u.rightClick(centers.get(0));
		}
	}
	
	private ROUnit getTarget(ArmyGroup g){
		Set<ROUnit> enemies = scout.getEnemyUnits();
		return Tools.findClosest(enemies, g.getLocation());
	}
	
	public void updateGroups(){
		for(ArmyGroup g: marineGroups){
			Set<Unit> toRemove = new HashSet<Unit>();
			Set<Unit> gUnits = g.getUnits();
			for(Unit u: gUnits){
				if(!u.isVisible()){
					toRemove.add(u);
				}
			}
			for(Unit u: toRemove){
				gUnits.remove(u);
				armyUnits.remove(u);
			}
		}
		
	}
	public void peasantRush(){
		int rushingEnemies = 0;
		List<ROUnit> enemies =Tools.enemyUnits();
		if(getArmySize()<1){
			if(enemies!=null){
				for(ROUnit u:enemies){
					if(u.getOrderTarget()!=null && u.getOrderTarget().getPlayer()!=null && 
							u.getOrderTarget().getPlayer().equals(me) &&
							u.getOrderTarget().isGatheringMinerals()){
						rushingEnemies++;
						Unit toAdd = builder.pullWorker(u.getTilePosition());
						if(toAdd!=null)
							peasants.add(toAdd);
					}
				}
			}
		}
		if(rushingEnemies == 0){
			for(Unit p : peasants){
				builder.addWorker(p);
			}
			peasants.clear();
		}
		if(rushingEnemies>0){
			for(Unit p: peasants){
				ROUnit nearest = UnitUtils.getClosest(p, enemies);
				p.attackUnit(nearest);
			}
		}
		
	}
	
	public int getArmySize(){
		int count = 0;
		for(Unit u: armyUnits){
			if(u.isCompleted())
				count++;
		}
		return count;
	}

	public void act(){
		updateGroups();
		int totalEnemyForces = scout.groundForces() + scout.airForces();
		Set <ArmyGroup> toRemove = new HashSet<ArmyGroup>();
		peasantRush();
		
		for(ArmyGroup g: marineGroups){
			Set<Unit> bunkerUnits = new HashSet<Unit>();
			for(Unit u: g.getUnits()){
				if(useBunkers(u)){
					bunkerUnits.add(u);
				}
			}
			for(Unit u: bunkerUnits){
				g.remove(u);
			}
			if(g.getUnits().isEmpty()){
				toRemove.add(g);
				continue;
			}
			
			if(!g.isAttacking()){
				g.rally();
			}else{
				ROUnit target = getTarget(g);
				if(target!=null)
					attack(g,target.getLastKnownTilePosition());
			}
		}
		
		for(ArmyGroup g: toRemove){
			marineGroups.remove(g);
		}
		
		if(getArmySize()> totalEnemyForces){
			for(ArmyGroup g: marineGroups){
				if(!g.isAttacking() && g.getUnits().size() >20){
					g.setAttack(true);
				}
			}
		}
		heal();
		
	}
}
