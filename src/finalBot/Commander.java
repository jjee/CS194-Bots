package finalBot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bwapi.proxy.model.Bwta;
import org.bwapi.proxy.model.Chokepoint;
import org.bwapi.proxy.model.Game;
import org.bwapi.proxy.model.Order;
import org.bwapi.proxy.model.Position;
import org.bwapi.proxy.model.ROUnit;
import org.bwapi.proxy.model.TechType;
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
	
	public boolean useBunkers(Unit u){
		List<ROUnit> bunkers = UnitUtils.getAllMy(UnitType.TERRAN_BUNKER);
		boolean loaded = false;
		for (ROUnit b: bunkers) {
			if (b.isCompleted() &&b.getLoadedUnits().size() < 4) {
				UnitUtils.assumeControl(b).load(u);
				loaded = true;
			}
		}
		return loaded;
	}
	
	public void addAttacker(Unit u) { //add a unit for attacker class to use
		armyUnits.add(u);
		
		if(useBunkers(u))
			return;
		
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
				Position chokeCenter = closestChoke(builder.getHome());
				choice.setRally(Tools.randomNearby(chokeCenter,100));
			}
			choice.add(u);
			marineMedicGroups.add(choice);
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
		if(!g.isAttacking()){
			g.setAttack(true);
			attackCount++;
		}
		
		Set<Unit> marines = g.getUnits(UnitType.TERRAN_MARINE);
		Set<Unit> medics = g.getUnits(UnitType.TERRAN_MEDIC);
		ROUnit target = g.selectTarget();
		for(Unit m: marines){
			if(m.isIdle())
				m.attackMove(pos);
			
			if(target!=null && target.isVisible() &&
					Tools.close(m.getTilePosition(),target.getLastKnownTilePosition(),6) && m.getHitPoints() > 30 &&
				Game.getInstance().self().hasResearched(TechType.STIM_PACKS) &&
				m.getStimTimer()==0){
				m.useTech(TechType.STIM_PACKS);
				
			}
		}
		Set<Unit> changeGroups = new HashSet<Unit>();
		for(Unit c: medics){
			if(marines.size()==0){
				changeGroups.add(c);
			}else if(c.isIdle()&&!g.underAttack()){
				if(UnitUtils.avePos(marines)!=null)
					c.rightClick(UnitUtils.avePos(marines));
			}
		}
		
		for(Unit c: changeGroups){
			ArmyGroup choice = null;
			for(ArmyGroup changeG: marineMedicGroups){
				choice = changeG;
			}
			if(changeGroups==null){
				c.rightClick(builder.getHome());
				choice = new ArmyGroup();
			}
			g.remove(c);
			choice.add(c);
		}
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
		for(ArmyGroup g: marineMedicGroups){
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

	public void act(){
		updateGroups();
		int totalEnemyForces = scout.groundForces() + scout.airForces();
		Set <ArmyGroup> toRemove = new HashSet<ArmyGroup>();
		
		for(ArmyGroup g: marineMedicGroups){
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
			marineMedicGroups.remove(g);
		}
		
		if(armyUnits.size()> totalEnemyForces){
			for(ArmyGroup g: marineMedicGroups){
				if(!g.isAttacking() && armyUnits.size() >20){
					g.setAttack(true);
				}
			}
		}
		
	}
}
