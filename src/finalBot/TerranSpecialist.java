package finalBot;

import org.bwapi.proxy.model.Game;
import org.bwapi.proxy.model.ROUnit;
import org.bwapi.proxy.model.UnitType;


public class TerranSpecialist extends Specialist {
	private State myState;
	private Alert myAlert;
	private boolean seenAir;
	private boolean expanded;
	private int lastFrameObserved;
	//TODO: Figure out how long LATENCY should be
	private final static int LATENCY = 10000;
	
	private enum State {
		NONE,
		VULTURE_LIKELY,
		VULTURE_CONFIRMED
	}
	
	public TerranSpecialist() {
		myState = State.NONE;
		myAlert = Alert.NONE;
		seenAir = false;
		expanded = false;
	}
	
	//Called on frame
	public void update() {
		if (Game.getInstance().getFrameCount() - lastFrameObserved > LATENCY) {
			myState = State.NONE;
			myAlert = Alert.NONE;
		}
		
		//TODO: Formula for calculating defense depth
		int totalAttackPotential = 0;
		for (ROUnit u : scout.enemyGroundUnits()) {
			totalAttackPotential += u.getGroundWeaponDamage() / u.getGroundWeaponCooldown() / 2;
		}
		if (totalAttackPotential < 10 && scout.getNumberOf(UnitType.TERRAN_BUNKER) < 2)
			myAlert = Alert.NO_DEFENSE;
	}
	
	//Called on unit show, presumes that unit is enemy's
	public void update(ROUnit unit) {
		lastFrameObserved = Game.getInstance().getFrameCount();
		if (unit.getType() == UnitType.TERRAN_MARINE) {
			if (builder.getGameStage() == GameStage.EARLY && scout.getNumberOf(UnitType.TERRAN_MARINE) > 20) {
				myAlert = Alert.EARLY_RUSH;
				Game.getInstance().printf("WARNING: Marine rush imminent.");
			}
		} else if (unit.getType() == UnitType.TERRAN_FACTORY) {
			if (builder.getGameStage() == GameStage.EARLY && scout.getNumberOf(UnitType.TERRAN_FACTORY) == 2) {
				myState = State.VULTURE_LIKELY;
				Game.getInstance().printf("ALERT: 2 factories detected, vulture rush likely.");
			}
		} else if (unit.getType() == UnitType.TERRAN_VULTURE && myState == State.VULTURE_LIKELY) {
			myState = State.VULTURE_CONFIRMED;
			myAlert = Alert.EARLY_RUSH;
			Game.getInstance().printf("WARNING: Vultures approaching, build defenses.");
		} else if (unit.getType() == UnitType.TERRAN_STARPORT && !seenAir) {
			myAlert = Alert.AIR_STRUCTURES;
			Game.getInstance().printf("ALERT: Starport detected; build anti-airs.");
		} else if (unit.getType().isFlyer() && !seenAir) {
			myAlert = Alert.AIR_UNITS;
			seenAir = true;
			Game.getInstance().printf("WARNING: Air units approaching!");
		} else if (unit.getType() == UnitType.TERRAN_COMMAND_CENTER && !expanded) {
			myAlert = Alert.EXPANSION;
			expanded = true;
			Game.getInstance().printf("ALERT: Enemy is expanding!");
		} else if (unit.canCloak()) {
			myAlert = Alert.CLOAKED_UNITS;
			Game.getInstance().printf("ALERT: Units with cloaking capability detected.");
		}
	}
	
	public Alert getAlert() {
		return myAlert;
	}
}
