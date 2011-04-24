package finalBot;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bwapi.proxy.model.Game;
import org.bwapi.proxy.model.Position;
import org.bwapi.proxy.model.ROUnit;
import org.bwapi.proxy.model.TilePosition;
import org.bwapi.proxy.model.Unit;

import edu.berkeley.nlp.starcraft.util.UnitUtils;

public class Tools {
	static final int TILE_SIZE = 32;

	public static int getMapDistance(){
		return 0;
	}
	
	public static boolean close(ROUnit u, TilePosition t, int dist){
		TilePosition uLoc = u.getTilePosition();
		return Math.abs(uLoc.x()-t.x()) + Math.abs(uLoc.y()-t.y()) < dist;
	}
	
	public static boolean close(TilePosition uLoc, TilePosition t, int dist){
		return Math.abs(uLoc.x()-t.x()) + Math.abs(uLoc.y()-t.y()) < dist;
	}
	
	public static boolean close(ROUnit u, Set<ROUnit> targets, int dist){
		for(ROUnit target: targets){
			if(close(u,target.getTilePosition(),dist))
				return true;
		}
		return false;
	}
	
	public static boolean close(ROUnit u, List<ROUnit> targets, int dist){
		for(ROUnit target: targets){
			if(close(u,target.getTilePosition(),dist))
				return true;
		}
		return false;
	}
	
	public static Position randomNearby(ROUnit u, int dist){
		return randomNearby(u.getPosition(),dist);
	}
	
	public static Position randomNearby(Position p, int dist){
		int dx = (int)(Math.random()*dist-dist/2);
		int dy = (int)(Math.random()*dist-dist/2);
		int x = p.x();
		int y = p.y();
		int newx = x+dx;
		int newy = y+dy;
		for(int i = 0; i <100; i++){
			if(Game.getInstance().mapHeight()*TILE_SIZE>newy 
					&& Game.getInstance().mapWidth()*TILE_SIZE > newx
					&& newx > 0 && newy > 0
					|| Game.getInstance().isWalkable(newx/4, newy/4))
				return new Position(newx,newy);
			dx = (int)(Math.random()*dist-dist/2);
			dy = (int)(Math.random()*dist-dist/2);
		}
		return null;
	}
	
	public static List<ROUnit> enemyUnits(){
		List<ROUnit> units = new ArrayList<ROUnit>();
		for(ROUnit u: Game.getInstance().getAllUnits()){
			if(Game.getInstance().self().isEnemy(u.getPlayer()))
				units.add(u);
		}
		return units;
	}
	
	public static ROUnit findClosest(Set<ROUnit> units, TilePosition p) {
		int x = p.x()*TILE_SIZE;
		int y = p.y()*TILE_SIZE;
		return findClosest(units,new Position(x,y));
	}
	
	public static ROUnit findClosest(Set<ROUnit> units, Position p) {
		return findClosest(new ArrayList<ROUnit>(units), p);
	}
	
	public static ROUnit findClosest(List<ROUnit> units, TilePosition p){
		int x = p.x()*TILE_SIZE;
		int y = p.y()*TILE_SIZE;
		return findClosest(units,new Position(x,y));
	}
	
	public static ROUnit findClosest(List<ROUnit> units, Position p){
		double best = 10000;
		ROUnit bestu = null;
		for(ROUnit u: units){
			double d = u.getDistance(p);
			if(d < best){
				best = d;
				bestu = u;
			}
		}
		return bestu;
	}
	
	public static List<Unit> convertRO(List<ROUnit> units){
		List<Unit> retList = new ArrayList<Unit>();
		for(ROUnit u: units){
			retList.add(UnitUtils.assumeControl(u));
		}
		return retList;
	}
	
	public static TilePosition calcAvgLoc(Set<Unit> units){
		int sumX = 0, sumY = 0;
		int unitCount = units.size();
		if (unitCount==0) return null;
		
		for(Unit u: units){
			sumX = sumX + u.getTilePosition().x();
			sumY = sumY + u.getTilePosition().y();
		}
		
		return new TilePosition(sumX/unitCount,sumY/unitCount);
	}
}
