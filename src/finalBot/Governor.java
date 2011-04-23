package finalBot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.bwapi.proxy.model.Game;
import org.bwapi.proxy.model.ROUnit;
import org.bwapi.proxy.model.TilePosition;
import org.bwapi.proxy.model.Unit;
import org.bwapi.proxy.model.UnitType;
import org.bwapi.proxy.util.Pair;

import edu.berkeley.nlp.starcraft.util.UnitUtils;

public class Governor {
	private HashMap<ROUnit, UnitType> builders;
	private HashSet<ROUnit> allWorkers;
	private LinkedList<UnitType> toBuild;
	private Pair<UnitType, TilePosition> nextBuild;
	private Spy scout;
	private Commander attacker;
	
	public Governor() {
		builders = new HashMap<ROUnit, UnitType>();
		allWorkers = new HashSet<ROUnit>();
		toBuild = new LinkedList<UnitType>();
		scout = null;
		attacker = null;
	}
	
	public Governor(LinkedList<UnitType> toBuild) {
		this();
		this.toBuild = toBuild;
	}
	
	public void setSpy(Spy spy) {
		scout = spy;
	}
	
	public void setCommander(Commander commander) {
		attacker = commander;
	}
	
	public void addToBuild(UnitType type, int priority) {
		toBuild.remove(type);
		toBuild.add(priority, type);
	}
	
	public List<Pair<UnitType, TilePosition>> plan() {
		return null;
	}
	
	public void executePlan() {
		
	}
	
	public void advance() {
		if (nextBuild != null && build(nextBuild.getFirst(), nextBuild.getSecond()))
			nextBuild = plan().remove(0);
	}
	
	public void addBuilder(Unit worker) {
		allWorkers.add(worker);
		
		boolean assigned = false;
		int iteration = 0;
		while (!assigned && !toBuild.isEmpty()) {
			for (UnitType t : toBuild) {
				int numFound = 0;
				for (ROUnit u : builders.keySet()) {
					if (builders.get(u) == t)
						numFound++;
				}
				if (numFound <= iteration) {
					builders.put(worker, t);
					assigned = true;
					break;
				}
			}
			iteration++;
		}
	}
	
	public void removeBuilder(Unit worker) {
		allWorkers.remove(worker);
		builders.remove(worker);
	}
	
	public Unit acquireBuilder(UnitType toBuild, TilePosition tp) {
		List<ROUnit> units = new LinkedList<ROUnit>();
		for (ROUnit u : builders.keySet()) {
			if (builders.get(u) == toBuild && !u.isCarryingGas() && !u.isCarryingMinerals() && !u.isConstructing())
				units.add(u);
		}
		if (units.isEmpty())
			return null;
		return UnitUtils.assumeControl(Tools.findClosest(units, tp));
	}
	
	public boolean build(UnitType type, TilePosition tp) {
		Unit builder = acquireBuilder(type, tp);
		if (builder == null)
			return false;
		if (builder.canBuildHere(tp, type)) {
			builder.build(tp, type);
			return true;					  
		}
		return false;
	}
	
	public boolean produce(UnitType type) {
		UnitType producer = type.whatBuilds().getKey();
		List<ROUnit> allProducers = UnitUtils.getAllMy(producer);
		for (ROUnit u : allProducers) {
			if (u.getTrainingQueue().isEmpty()) {
				UnitUtils.assumeControl(u).train(type);
				return true;
			}
		}
		return false;
	}
	
	public void gatherMinerals(Unit worker) {
		ROUnit closestPatch = UnitUtils.getClosest(worker, Game.getInstance().getMinerals());
		if (closestPatch != null)
			worker.rightClick(closestPatch);
	}
	
	public void gatherMinerals(Unit worker, TilePosition tp) {
		ROUnit closestPatch = Tools.findClosest((Set<ROUnit>) Game.getInstance().getMinerals(), tp);
		if (closestPatch != null)
			worker.rightClick(closestPatch);
	}
	
	public void gatherGas(Unit worker) {
		ROUnit closestRefinery = Tools.findClosest(UnitUtils.getAllMy(UnitType.TERRAN_REFINERY), worker.getPosition());
		if (closestRefinery != null)
			worker.rightClick(closestRefinery);
	}
	
	public void gatherGas(Unit worker, TilePosition tp) {
		ROUnit closestRefinery = Tools.findClosest(UnitUtils.getAllMy(UnitType.TERRAN_REFINERY), tp);
		if (closestRefinery != null)
			worker.rightClick(closestRefinery);		
	}
}
