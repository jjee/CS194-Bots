package finalBot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bwapi.proxy.model.Position;
import org.bwapi.proxy.model.ROUnit;
import org.bwapi.proxy.model.TilePosition;
import org.bwapi.proxy.model.Unit;
import org.bwapi.proxy.model.UnitType;

import edu.berkeley.nlp.starcraft.util.UnitUtils;

public class Commander {
	private Spy scout;
	private Governor builder;
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
		if(type==UnitType.TERRAN_MARINE && type==UnitType.TERRAN_MEDIC){
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
			}
			choice.add(u);
		}
	}
	
	public void removeAttacker(Unit u) { //remove unit from attacker class
		armyUnits.remove(u);
	}

	public void attack(ArmyGroup g, TilePosition tp) {//have group attack
		Position pos = new Position(tp.x(),tp.y());
		if(g.isAttacking()){
			g.setAttack(true);
			attackCount++;
		}
		if(!Tools.close(g.getLocation(),tp,20)){
			for(Unit u: g.getUnits()){
				if(u.isIdle()){
					u.rightClick(pos);
				}
			}
		} else {
			Set<Unit> marines = g.getUnits(UnitType.TERRAN_MARINE);
			Set<Unit> medics = g.getUnits(UnitType.TERRAN_MEDIC);
			for(Unit m: marines){
				m.attackMove(pos);
			}
			for(Unit c: medics){
				if(!Tools.close(c, Tools.calcAvgLoc(marines), 8)){
					c.rightClick(Tools.calcAvgLoc(marines));
				}
			}
		}
	}
	
	public void move(ArmyGroup g) {//move group
	
	}
	public void retreat(ArmyGroup g) {//retreat group
		for(Unit u: g.getUnits()){
			List<ROUnit> centers = UnitUtils.getAllMy(UnitType.TERRAN_COMMAND_CENTER);
			if(!centers.isEmpty() && u.isIdle())
				u.rightClick(centers.get(0));
		}
	}

	public void moveGroups() { //order each group to do something
		
	}
	
	public void setSpy(Spy s){ scout = s; }
	public void setGovernor(Governor g){ builder = g;} 
}
