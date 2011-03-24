package fixedBots;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import org.bwapi.proxy.model.TilePosition;

public class AStarSearch {
	/** AStar */
	
	protected TilePosition myEnd;
	protected Node[][] myGrid;
	protected TilePosition[] successors;
	protected double[][] grid;
	
	protected int xEnd;
	protected int yEnd;
	
	class Node implements Comparable<Node> {
		Node parent;
		TilePosition state;
		double cost, fcost;
		
		public Node(Node parent, TilePosition state, double cost, double h) {
			super();
			this.parent = parent;
			this.state = state;
			this.cost = cost;
			this.fcost = cost + h;
		}

		@Override
		public int compareTo(Node compareTo) {
			if(compareTo.fcost == fcost)
				return 0;
			else if(compareTo.fcost > fcost)
				return -1;
			else
				return 1;
		}
	}
	
	public List<TilePosition> getPath(double[][] grid, TilePosition start, TilePosition end) {
		myEnd = start;
		xEnd = myEnd.x();
		yEnd = myEnd.y();
		successors = new TilePosition[8];
		this.grid = grid;
		if(myGrid == null)
			myGrid = new Node[grid.length][grid[0].length];
		Node node  = search(end);
		
		if (node == null) return null;
		
		List<TilePosition> path = new ArrayList<TilePosition>();
		while (node != null) {
			path.add(node.state);
			node = node.parent;
		}
		
		return path;
	}
	
	protected Node search(TilePosition start) {
		//initialize	
		PriorityQueue<Node> fringe = new PriorityQueue<Node>();
		Node startNode = new Node(null, start, 0,0);
		Node node = startNode;
		fringe.add(startNode);

		while (node != null) { //AStar loop
			TilePosition state = node.state;
			if (state.equals(myEnd)) {
				return node;
			}
			
			setSuccessors(state);
			for(TilePosition childTP : successors){
				if(childTP==null) continue; //skip if invalid next state
				
				double transCost = getCost(state,childTP);
				if(transCost == Double.POSITIVE_INFINITY) continue; //skip if not reachable
				double cost = node.cost + transCost;
				double h = heuristic(childTP);	
				
				//update fringe
				Node oldNode = myGrid[childTP.x()][childTP.y()];
				if(oldNode==null){
					Node childNode = new Node(node, childTP, cost, h);
					myGrid[childTP.x()][childTP.y()] = childNode;
					fringe.add(childNode);
				}else{
					if(cost < oldNode.cost){
						oldNode.parent = node;
						oldNode.cost = cost;
						oldNode.fcost = cost + h;
						fringe.remove(oldNode);
						fringe.add(oldNode);
					}
				}
				
			}
			node = fringe.poll();
		}
		return null; //no path
	}
	
	protected double getCost(TilePosition from, TilePosition to){
		return grid[from.x()][from.y()] + grid[from.x()][from.y()];
	}

	protected double heuristic(TilePosition tp) {
		int x = Math.abs(tp.x() - xEnd);
		int y = Math.abs(tp.y() - yEnd);
		double distance = 0;
		
		if(x > y){
			distance += y*1.5;
			distance += (x-y);
		}else{
			distance += x*1.5;
			distance += (y-x);
		}
		return distance;
	}
	
	protected void setSuccessors(TilePosition tp){
		int x = tp.x();
		int y = tp.y();
		int i = 0;
		
		for(int dx = -1; dx <= 1; dx++)
			for(int dy = -1; dy <= 1; dy++)
				if(dx!=0 || dy!=0)
					setSuccessor(i++,dx+x,dy+y);		
		
	}
	
	protected void setSuccessor(int i, int x, int y){
		if(checkXY(x,y)){
			if(myGrid[x][y]!=null)
				successors[i] = myGrid[x][y].state;
			else
				successors[i] = new TilePosition(x,y);
		}else{
			successors[i] = null;
		}
	}
	
	protected boolean checkXY(int x, int y){
		return (x >= 0 && x < grid.length && y >= 0 && y < grid[0].length);
	}

}

