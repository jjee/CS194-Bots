package finalBot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bwapi.proxy.model.Bwta;
import org.bwapi.proxy.model.Chokepoint;
import org.bwapi.proxy.model.Order;
import org.bwapi.proxy.model.Position;
import org.bwapi.proxy.model.ROUnit;
import org.bwapi.proxy.model.TilePosition;
import org.bwapi.proxy.model.Unit;
import org.bwapi.proxy.model.UnitType;

import edu.berkeley.nlp.starcraft.util.UnitUtils;

public class Commander extends Overseer {
	private Set<Unit> armyUnits; //all units under attacker control
	private List<ArmyGroup> marineMedicGroups;
	private int attackCount; //number of times we tried to attack
	
	public Commander(){
		armyUnits = new HashSet<Unit>();
		marineMedicGroups = new ArrayList<ArmyGroup>();
		attackCount = 0;
	}
	
	public void addAttacker(Unit u) { //add a unit for attacker class to use
		armyUnits.add(u);
		
		//add to some group
		UnitType type = u.getType();
		if(type==UnitType.TERRAN_MARINE || type==UnitType.TERRAN_MEDIC){
			int threshold = 0;
			ArmyGroup choice = null;
			if(type==UnitType.TERRAN_MARINE){
				threshold = 8;
			} else if (type==UnitType.TERRAN_MEDIC){
				threshold = 2;
			}
			
			for(ArmyGroup g: marineMedicGroups){
				if(!g.isAttacking()) {
					if(g.getUnits(type).size() < threshold){
						choice = g;
					}
				}
			}
			
			if(choice==null){
				choice = new ArmyGroup();
				Position chokeCenter = closestChoke(u);
				choice.setRally(Tools.randomNearby(chokeCenter,7));
			}
			choice.add(u);
			marineMedicGroups.add(choice);
		}
	}
	
	private Position closestChoke(ROUnit u){
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
		if(u.getType()==UnitType.TERRAN_MARINE||u.getType()==UnitType.TERRAN_MEDIC){
			for(ArmyGroup g: marineMedicGroups){
				if(g.getUnits().contains(u)){
					g.remove(u);
					return;
				}
			}
		}
	}

	private void attack(ArmyGroup g, TilePosition tp) {//have group attack
		Position pos = new Position(tp.x()*Tools.TILE_SIZE,tp.y()*Tools.TILE_SIZE);
		if(g.isAttacking()){
			g.setAttack(true);
			attackCount++;
		}
		Set<Unit> marines = g.getUnits(UnitType.TERRAN_MARINE);
		Set<Unit> medics = g.getUnits(UnitType.TERRAN_MEDIC);
		
		ROUnit target = g.selectTarget();
		
		if(!Tools.close(g.getLocation(),tp,40) && !g.underAttack()){
			for(Unit u: g.getUnits()){
				if(UnitUtils.groupRadius(g.getUnits()) > 400){
					if(!Tools.close(u, g.getLocation(), 4))
						u.rightClick(g.getLocation());
				}
				if(u.isIdle() && u.getType()==UnitType.TERRAN_MARINE){
					u.attackMove(pos);
				} else if (u.getType() == UnitType.TERRAN_MEDIC) {
					u.rightClick(Tools.calcAvgLoc(marines));
				}
				
			}
		} else {
			for(Unit m: marines){
				if(m.getOrder() == Order.ATTACK_MOVE || 
						m.getOrder() ==Order.ATTACK_UNIT) continue;
				if(target==null)
					m.attackMove(pos);
				else
					m.attackUnit(target);
			}
			for(Unit c: medics){
				if(c.isIdle()&&!Tools.close(c, Tools.calcAvgLoc(marines), 8)){
					c.rightClick(Tools.calcAvgLoc(marines));
				}
			}
		}
	}
	
	private void retreat(ArmyGroup g) {//retreat group
		for(Unit u: g.getUnits()){
			List<ROUnit> centers = UnitUtils.getAllMy(UnitType.TERRAN_COMMAND_CENTER);
			if(!centers.isEmpty() && u.isIdle())
				u.rightClick(centers.get(0));
		}
	}

	public void act(){
		int totalEnemyForces = scout.groundForces() + scout.airForces();
		for(ArmyGroup g: marineMedicGroups){
			if(!g.isAttacking()){
				g.rally();
			}else{
				if(scout.enemyBuildings()>0){
					System.out.println(scout.enemyBases().get(0).getLastKnownTilePosition());
					attack(g,scout.enemyBases().get(0).getLastKnownTilePosition());
				}
			}
		}
		
		if(armyUnits.size() > totalEnemyForces){
			for(ArmyGroup g: marineMedicGroups){
				if(g.getUnits().size() >= 10 && scout.enemyBuildings()>0){
					g.setAttack(true);
				}
			}
		}
	}
}
