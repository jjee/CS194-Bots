package finalBot;

import java.util.ArrayList;
import java.util.List;

import org.bwapi.proxy.model.Game;
import org.bwapi.proxy.model.ROUnit;
import org.bwapi.proxy.model.UnitType;

public class TerranSpecialist extends Specialist {
	private List<ROUnit> factories;
	private State myState;
	private enum State {
		NONE,
		VULTURE_LIKELY,
		VULTURE_CONFIRMED
	}
	
	public TerranSpecialist() {
		factories = new ArrayList<ROUnit>();
		myState = State.NONE;
	}
	
	public void update(ROUnit unit) {
		if (unit.getType() == UnitType.TERRAN_FACTORY) {
			factories.add(unit);
			if (builder.getGameStage() == GameStage.EARLY && factories.size() == 2) {
				myState = State.VULTURE_LIKELY;
				Game.getInstance().printf("ALERT: 2 factories detected, vulture rush likely.");
			}
		} else if (myState == State.VULTURE_LIKELY && unit.getType() == UnitType.TERRAN_VULTURE) {
			myState = State.VULTURE_CONFIRMED;
			Game.getInstance().printf("WARNING: Vultures approaching, build defenses.");
			//Rush alert
		} else if (unit.getType() == UnitType.TERRAN_STARPORT) {
			Game.getInstance().printf("ALERT: Starport detected; build anti-airs.");
			//Air structures alert
		} else if (unit.getType().isFlyer()) {
			Game.getInstance().printf("WARNING: Air units approaching!");
			//Air units alert
		}
	}
	
	public void act() {}
}
