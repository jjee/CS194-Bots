package fixedBots;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bwapi.proxy.model.Color;
import org.bwapi.proxy.model.Game;
import org.bwapi.proxy.model.Player;
import org.bwapi.proxy.model.Position;
import org.bwapi.proxy.model.ROUnit;
import org.bwapi.proxy.model.TilePosition;
import org.bwapi.proxy.model.UnitType;

public class DropData {
	private double[][] ratings; //how good is a tile for dropping
	private double[][] airThreat; //how dangerous is a tile for flying over
	HashMap<ROUnit,TilePosition> mobile;
	private Player self;
	private int width, height;
	int atkRange = 8*32;

	public DropData(int width, int height){
		ratings = new double[width][height];
		airThreat = new double[width][height];
		for(int x = 0; x < width; x++){
			for(int y = 0; y < height; y++){
				boolean canWalk = Game.getInstance().isWalkable(x/32*4, y/32*4);
				if(canWalk){
					ratings[x][y] = 9;
					airThreat[x][y] = 5;
				}else{
					ratings[x][y] = 1;
					airThreat[x][y] = 1;
				}
			}
		}
		
		//buildings = new HashMap<ROUnit,TilePosition>();
		mobile = new HashMap<ROUnit,TilePosition>();
		self = Game.getInstance().self();
		this.width = width;
		this.height = height;
	}
	
	public void addUnit(ROUnit u){
		if(!self.isEnemy(u.getPlayer())) return;
		
		if(mobile.containsKey(u)) { removeUnit(u);}
		Position center = Position.centerOfTile(u.getTilePosition());
		mobile.put(u, u.getTilePosition());
		if(u.getType().isBuilding()){
			int amt = 10;
			if(u.getType().isResourceDepot()) amt+=10;
			if(u.getType().isRefinery()) amt+=5;
			Game.getInstance().drawCircleMap(center, atkRange, Color.GREEN, false);
			incrementOverRange(center,atkRange,amt,ratings);
			if(u.getType().equals(UnitType.TERRAN_BUNKER)){
				incrementOverRange(center,32*9,32,airThreat);
			}
		}else {
			int amt = 2;
			incrementOverRange(center,atkRange,amt,ratings);
		}
		
		if(u.getType().isDetector()){
			int range = u.getType().sightRange()+32;
			Game.getInstance().drawCircleMap(center, range, Color.YELLOW, false);
			incrementOverRange(center,range,-50,ratings);
		}
		
		if(u.getType().airWeapon().targetsAir()){
			int range = u.getType().airWeapon().maxRange()+64;
			Game.getInstance().drawCircleMap(center, range, Color.RED, false);
			incrementOverRange(center,range,u.getAirWeaponDamage(),airThreat);
		}
	}
	
	private void incrementOverRange(Position center, int range, int amt, double[][] array){
		int startx = Math.max((center.x()-range)/32,0);
		int starty = Math.max((center.y() - range)/32,0);
		int endx = Math.min((center.x() + range)/32,width);
		int endy = Math.min((center.y() +range)/32,height);
		/*
		for(int x = startx; x < endx; x++){
			for(int y = starty; y < endy; y++){
				int dX = Math.abs(x*32 - center.x());
				int dY = Math.abs(y*32 - center.y());
				int squaredSum = dX*dX + dY*dY;
				if(Math.sqrt(squaredSum) < range){
					array[x][y]+=amt;
					if(array[x][y] < 1 && array.equals(airThreat)) array[x][y] = 1;
				}
			}
		}*/
		
		for(int x = 0; x < width; x++){
			for(int y = 0; y < height; y++){
				int dX = Math.abs(x*32 - center.x());
				int dY = Math.abs(y*32 - center.y());
				int squaredSum = dX*dX + dY*dY;
				if(Math.sqrt(squaredSum) < range){
					array[x][y]+=amt;
					if(array[x][y] < 1) array[x][y] = 1;
				}
			}
		}
	}
	/*
	private int getX(ROUnit u){ return u.getLastKnownTilePosition().x();}
	private int getX(TilePosition tp){ return tp.x();}
	private int getY(ROUnit u){ return u.getLastKnownTilePosition().y();}
	private int getY(TilePosition tp){ return tp.y();}
	*/
	public void removeUnit(ROUnit u){

		if(!self.isEnemy(u.getPlayer())) return;
		
		Position center = null;
		if(mobile.containsKey(u))
			center = Position.centerOfTile(mobile.get(u));
		if(center==null) return;

		if(u.getType().isBuilding()){
			int amt = -10;
			if(u.getType().isResourceDepot()) amt-=10;
			if(u.getType().isRefinery()) amt-=5;
			Game.getInstance().drawCircleMap(center, atkRange, Color.GREEN, false);
			incrementOverRange(center,atkRange,amt,ratings);
			if(u.getType().equals(UnitType.TERRAN_BUNKER)){
				incrementOverRange(center,32*9,-32,airThreat);
			}
		}else {
			int amt = -2;
			incrementOverRange(center,atkRange,amt,ratings);
		}
		
		if(u.getType().isDetector()){
			int range = u.getType().sightRange()+32;
			Game.getInstance().drawCircleMap(center, range, Color.YELLOW, false);
			incrementOverRange(center,range,50,ratings);
		}
		
		if(u.getType().airWeapon().targetsAir()){
			int range = u.getType().airWeapon().maxRange()+64;
			Game.getInstance().drawCircleMap(center, range, Color.RED, false);
			incrementOverRange(center,range,-u.getAirWeaponDamage(),airThreat);
		}
		if(mobile.containsKey(u)) mobile.remove(u);
	}
	
	public void refresh(){
		List<ROUnit> dNE = new ArrayList<ROUnit>();
		for(ROUnit unit: mobile.keySet()){
			if(self.canSeeUnitAtPosition(unit.getType(),unit.getLastKnownPosition()) && !unit.isVisible())
				dNE.add(unit);
		}
		for(ROUnit d: dNE){
			removeUnit(d);
		}
	}
	
	public double[][] getRatings(){ return ratings;}
	public double[][] getThreats(){ return airThreat;}
}
