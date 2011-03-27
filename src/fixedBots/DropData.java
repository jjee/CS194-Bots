package fixedBots;

import java.util.HashMap;

import org.bwapi.proxy.model.Color;
import org.bwapi.proxy.model.Game;
import org.bwapi.proxy.model.Player;
import org.bwapi.proxy.model.Position;
import org.bwapi.proxy.model.ROUnit;
import org.bwapi.proxy.model.TilePosition;

public class DropData {
	private double[][] ratings; //how good is a tile for dropping
	private double[][] airThreat; //how dangerous is a tile for flying over
	HashMap<ROUnit,TilePosition> buildings, mobile;
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
					ratings[x][y] = 5;
					airThreat[x][y] = 5;
				}else{
					ratings[x][y] = -200;
					airThreat[x][y] = 1;
				}
			}
		}
		
		buildings = new HashMap<ROUnit,TilePosition>();
		mobile = new HashMap<ROUnit,TilePosition>();
		self = Game.getInstance().self();
		this.width = width;
		this.height = height;
	}
	
	public void addUnit(ROUnit u){
		if(!self.isEnemy(u.getPlayer())) return;
		if(buildings.containsKey(u)) return;
		if(mobile.containsKey(u)) { removeUnit(u);}
		Position center = Position.centerOfTile(u.getTilePosition());
		
		if(u.getType().isBuilding()){
			buildings.put(u,u.getTilePosition());
			int amt = 10;
			if(u.getType().isResourceDepot()) amt+=10;
			if(u.getType().isRefinery()) amt+=5;
			
			Game.getInstance().drawCircleMap(center, atkRange, Color.GREEN, false);
			incrementOverRange(center,atkRange,amt,ratings);
		}else if(!u.isFlying()){
			mobile.put(u, u.getTilePosition());
			int amt = 2;
			incrementOverRange(center,atkRange,amt,ratings);
		}
		
		if(u.getType().isDetector()){
			int range = u.getType().sightRange()+32;
			Game.getInstance().drawCircleMap(center, range, Color.YELLOW, false);
			incrementOverRange(center,range,-100,ratings);
		}
		
		if(u.getType().airWeapon().targetsAir()){
			int range = u.getType().airWeapon().maxRange()+32;
			Game.getInstance().drawCircleMap(center, range, Color.RED, false);
			incrementOverRange(center,range,u.getAirWeaponDamage(),airThreat);
		}
	}
	
	private void incrementOverRange(Position center, int range, int amt, double[][] array){
		int startx = Math.max((center.x()-range)/32,0);
		int starty = Math.max((center.y() - range)/32,0);
		int endx = Math.min((center.x() + range)/32,width);
		int endy = Math.min((center.y() +range)/32,height);
		
		for(int x = startx; x < endx; x++){
			for(int y = starty; y < endy; y++){
				int dX = Math.abs(x*32 - center.x());
				int dY = Math.abs(y*32 - center.y());
				int squaredSum = dX*dX + dY*dY;
				if(Math.sqrt(squaredSum) < range){
					array[x][y]+=amt;
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
		if(buildings.containsKey(u))
			center = Position.centerOfTile(buildings.get(u));
		else if(mobile.containsKey(u))
			center = Position.centerOfTile(mobile.get(u));
		if(center==null) return;

		if(u.getType().isBuilding()){
			int amt = -10;
			if(u.getType().isResourceDepot()) amt-=10;
			if(u.getType().isRefinery()) amt-=5;
			Game.getInstance().drawCircleMap(center, atkRange, Color.GREEN, false);
			incrementOverRange(center,atkRange,amt,ratings);
		}else if(!u.isFlying()){
			int amt = -2;
			incrementOverRange(center,atkRange,amt,ratings);
		}
		
		if(u.getType().isDetector()){
			int range = u.getType().sightRange()+32;
			Game.getInstance().drawCircleMap(center, range, Color.YELLOW, false);
			incrementOverRange(center,range,+100,ratings);
		}
		
		if(u.getType().airWeapon().targetsAir()){
			int range = u.getType().airWeapon().maxRange()+32;
			Game.getInstance().drawCircleMap(center, range, Color.RED, false);
			incrementOverRange(center,range,-u.getAirWeaponDamage(),airThreat);
		}
		if(buildings.containsKey(u)) buildings.remove(u);
		if(mobile.containsKey(u)) mobile.remove(u);
	}
	
	public double[][] getRatings(){ return ratings;}
	public double[][] getThreats(){ return airThreat;}
}
