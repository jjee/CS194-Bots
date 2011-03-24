package fixedBots;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bwapi.proxy.model.TilePosition;

public class AStarThreat extends AStarSearch {
	/** AStar */
	protected TilePosition myStart;
	//private TilePosition[] successors;
	//private ThreatGrid grid;
	private boolean reverse;
	
	protected boolean searchDirection(double[][] grid, TilePosition start, TilePosition end){
		return grid[start.x()][start.y()] < grid[end.x()][end.y()];
	}
	
	@Override
	public List<TilePosition> getPath(double[][] grid, TilePosition start, TilePosition end) {
		
		if(searchDirection(grid,start,end)){
			reverse = true;
			myStart = end;
			myEnd = start;
		}
		else{
			reverse = false;
			myStart = start;
			myEnd = end;
		}
		
		xEnd = myEnd.x();
		yEnd = myEnd.y();
		successors = new TilePosition[8];
		this.grid = grid;
		myGrid = new Node[grid.length][grid[0].length];
		Node node  = search(myStart);
		
		if (node == null) return null;
		
		List<TilePosition> path = new ArrayList<TilePosition>();
		while (node != null) {
			path.add(node.state);
			node = node.parent;
		}
		
		if(!reverse)
			Collections.reverse(path);
		
		return path;
	}
	
	@Override
	protected double getCost(TilePosition from, TilePosition to){
		double multiplier = 1;
		if(from.x()-to.x()!=0 && from.y()-to.y() != 0)
				multiplier = 1.5;
		
		return multiplier*(2+(grid[from.x()][from.y()] + grid[to.x()][to.y()]))/2;
	}
	
}

