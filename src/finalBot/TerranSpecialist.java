package finalBot;

import java.util.ArrayList;
import java.util.List;

import org.bwapi.proxy.model.Game;
import org.bwapi.proxy.model.Unit;
import org.bwapi.proxy.model.UnitType;

public class TerranSpecialist {
	private Governor builder;
	private Commander attacker;
	private Spy scout;
	private List<Unit> factories;
	
	public TerranSpecialist() {
		factories = new ArrayList<Unit>();
	}
	
	public void update(Unit unit) {
		if (unit.getType() == UnitType.TERRAN_FACTORY) {
			factories.add(unit);
			if (builder.getGameStage() == GameStage.EARLY && factories.size() == 2)
				Game.getInstance().printf("2 factories detected!");
		}
	}
}
