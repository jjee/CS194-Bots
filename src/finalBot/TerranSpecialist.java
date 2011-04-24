package finalBot;

import java.util.ArrayList;
import java.util.List;

import org.bwapi.proxy.model.Game;
import org.bwapi.proxy.model.ROUnit;
import org.bwapi.proxy.model.Unit;
import org.bwapi.proxy.model.UnitType;

public class TerranSpecialist extends Specialist {
	private List<ROUnit> factories;
	
	public TerranSpecialist() {
		factories = new ArrayList<ROUnit>();
	}
	
	public void update(ROUnit unit) {
		if (unit.getType() == UnitType.TERRAN_FACTORY) {
			factories.add(unit);
			if (builder.getGameStage() == GameStage.EARLY && factories.size() == 2)
				Game.getInstance().printf("2 factories detected!");
		}
	}
	
	public void act() {}
}
