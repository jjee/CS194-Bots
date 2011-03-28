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
	private HashMap<ROUnit, EnemyUnitDatum> mobile;
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
					airThreat[x][y] = 1;
				}else{
					ratings[x][y] = -10000;
					airThreat[x][y] = 1;
				}
			}
		}
		
		//buildings = new HashMap<ROUnit,TilePosition>();
		mobile = new HashMap<ROUnit,EnemyUnitDatum>();
		self = Game.getInstance().self();
		this.width = width;
		this.height = height;
	}
	
	public void addUnit(ROUnit u){
		if(!self.isEnemy(u.getPlayer())) return;
		
		if(mobile.containsKey(u)) { removeUnit(u);}
		Position center = Position.centerOfTile(u.getTilePosition());
		double dropRate = 0;
		double threat = 0;
		int threatRange = 0;
		int ratingRange = atkRange;
		if(u.getType().isBuilding()){
			int amt = 10;
			if(u.getType().isResourceDepot()) amt+=10;
			if(u.getType().isRefinery()) amt+=5;
			Game.getInstance().drawCircleMap(center, atkRange, Color.GREEN, false);
			incrementOverRange(center,atkRange,amt,ratings);
			dropRate+=amt;
			if(u.getType().equals(UnitType.TERRAN_BUNKER)){
				incrementOverRange(center,32*11,32*100,airThreat);
				threat += (32*100);
				threatRange = 32*11;
			}
		}else if(u.getType().isWorker()){
			int amt = 10;
			incrementOverRange(center,atkRange,amt,ratings);
			dropRate +=amt;
		}
		/*
		if(u.getType().isDetector()){
			int range = u.getType().sightRange()+32;
			Game.getInstance().drawCircleMap(center, range, Color.YELLOW, false);
			incrementOverRange(center,range,-50,ratings);
			dropRate-=50;
			ratingRange = range;
		}*/
		
		if(u.getType().airWeapon().targetsAir()){
			int range = u.getType().airWeapon().maxRange()+32*3;
			int damage = u.getAirWeaponDamage()*100;
			Game.getInstance().drawCircleMap(center, range, Color.RED, false);
			incrementOverRange(center,range,damage,airThreat);
			
			threat+=damage;
			threatRange=range;
		}
		EnemyUnitDatum eud = new EnemyUnitDatum();
		eud.airRange = threatRange;
		eud.airThreat = threat;
		eud.dropRange = ratingRange;
		eud.dropRating = dropRate;
		eud.tp = u.getTilePosition();
		mobile.put(u, eud);
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
					if(array[x][y] < 1 && array.equals(airThreat)) array[x][y] = 1;
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
			center = Position.centerOfTile(mobile.get(u).tp);
		if(center==null) return;
		EnemyUnitDatum eud = mobile.get(u);
		incrementOverRange(center,eud.airRange,-(int)eud.airThreat,airThreat);
		incrementOverRange(center,eud.dropRange,-(int)eud.dropRating,ratings);
		
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
	public HashMap<ROUnit,EnemyUnitDatum> mobile(){return mobile;}
}
