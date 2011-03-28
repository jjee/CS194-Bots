package fixedBots;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bwapi.proxy.model.Color;
import org.bwapi.proxy.model.Game;
import org.bwapi.proxy.model.Order;
import org.bwapi.proxy.model.Player;
import org.bwapi.proxy.model.Position;
import org.bwapi.proxy.model.ROUnit;
import org.bwapi.proxy.model.TilePosition;
import org.bwapi.proxy.model.Unit;
import org.bwapi.proxy.model.UnitType;

import edu.berkeley.nlp.starcraft.util.UnitUtils;

public class LurkerDropWC extends LurkerDropWithData{
	private Unit sparky;
	private boolean cheeserDefenseMode;
	private boolean cheeserAttackMode;
	private TilePosition toCheck;
	private int stepCount = 0;
	private TilePosition stopPoint;
	
	@Override
	public void onFrame(){
		cheese();
		super.onFrame();
	}
	
	public void cheese() {
		cheeserDefenseMode = false;
		if(myHome==null) return;
		if(stepCount<200)
			stepCount++;
		else if(stepCount==200)
			stopPoint = sparky.getTilePosition();
		if(sparky == null){
			if(!drones.isEmpty())
				sparky = drones.remove(0);
		}
		Player me = Game.getInstance().self();
		cheeserDefenseMode = false;
		Set<ROUnit> enemyBuildings = new HashSet<ROUnit>();
		List<ROUnit> enemyBases = new ArrayList<ROUnit>();
		for(ROUnit b: myMap.getBuildings()){
			if(me.isEnemy(b.getPlayer())){
				enemyBuildings.add(b);
				if(b.getType().isResourceDepot())
					enemyBases.add(b);
			}
		}
		if(enemyBuildings.isEmpty()){
			for(TilePosition tp: myMap.getStartSpots()){
				if(!scouted.contains(tp))
					toCheck = tp;
				if(close(sparky.getTilePosition(),tp)){
					scouted.add(tp);
				}
			}
			if(toCheck!=null)
				sparky.rightClick(toCheck);
			return;
		}
		if(enemyBases.isEmpty()) return;
		
		for (ROUnit u: Game.getInstance().getAllUnits()) {
			if (me.isEnemy(u.getPlayer()) && (sparky.equals(u.getTarget()) || sparky.equals(u.getOrderTarget()))) {
				sparky.rightClick(myHome);
				cheeserDefenseMode = true;
				cheeserAttackMode = false;
				
				break;
			}
			else if (enemyBuildings.contains(u)) {
				if (cheeserDefenseMode == false) {
					sparky.attackUnit(u);
					cheeserAttackMode = true;
				}
			}
		}
		
		if (cheeserDefenseMode == false && cheeserAttackMode == false) {
			sparky.rightClick(enemyBases.get(0).getLastKnownTilePosition());
		}
		
		Game.getInstance().drawLineMap(sparky.getPosition(), Position.centerOfTile(myHome), Color.YELLOW);
	}
	
}
