package fixedBots;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bwapi.proxy.model.Game;
import org.bwapi.proxy.model.Player;
import org.bwapi.proxy.model.ROUnit;
import org.bwapi.proxy.model.TilePosition;


public class SCMap {
	private int height, width;
	private Set<TilePosition> startSpots;
	private Set<ROUnit> gasSpots; //use to figure out possible expansion locations
	private Set<ROUnit> buildings; //buildings seen
	private Player self;
	
	@SuppressWarnings("unchecked")
	public SCMap(){
		height = Game.getInstance().getMapHeight();
		width = Game.getInstance().getMapWidth();
		startSpots = Game.getInstance().getStartLocations();
		gasSpots = (Set<ROUnit>) Game.getInstance().getGeysers();
		buildings = new HashSet<ROUnit>();
		self = Game.getInstance().self();
	}
	
	public int getHeight(){ return height;}
	public int getWidth(){ return width;}
	public Set<TilePosition> getStartSpots(){ return startSpots;}
	public Set<ROUnit> getGasSpots(){ return gasSpots;}
	
	public Set<ROUnit> getBuildings(){
		return buildings;
	}
	
	public void refresh(){
		/*
		List<ROUnit> dNE = new ArrayList<ROUnit>();
		for(ROUnit unit: buildings){
			if(self.canSeeUnitAtPosition(unit.getType(),unit.getLastKnownPosition()) && !unit.isVisible())
				dNE.add(unit);
		}
		for(ROUnit d: dNE){
			buildings.remove(d);
		}*/
	}
	public void addBuilding(ROUnit u){buildings.add(u);}
	public void removeBuilding(ROUnit u){buildings.remove(u);}
}
