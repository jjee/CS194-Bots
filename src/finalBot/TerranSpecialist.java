package finalBot;

import org.bwapi.proxy.model.Game;
import org.bwapi.proxy.model.ROUnit;
import org.bwapi.proxy.model.UnitType;


public class TerranSpecialist extends Specialist {
	private State myState;
	private boolean expanded;
	
	private enum State {
		NONE,
		VULTURE_LIKELY,
		VULTURE_CONFIRMED
	}
	
	public TerranSpecialist() {
		myState = State.NONE;
		myAlert = Alert.NONE;
		rushDetected = false;
		airDetected = false;
		cloakingDetected = false;
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
			totalAttackPotential += u.getGroundWeaponDamage() / (u.getGroundWeaponCooldown()+1) / 2;
		}
		if (totalAttackPotential < 10 && scout.getStaticDef() < 2) {
			myAlert = Alert.NO_DEFENSE;
			Game.getInstance().printf("Opponent lacks defense, build up attack!");
		}
	}
	
	//Called on unit show, presumes that unit is enemy's
	public void update(ROUnit unit) {
		lastFrameObserved = Game.getInstance().getFrameCount();
		if (unit.getType() == UnitType.TERRAN_MARINE) {
			if (builder.getGameStage() == GameStage.EARLY && !rushDetected && scout.getNumberOf(UnitType.TERRAN_MARINE) > 20) {
				myAlert = Alert.EARLY_RUSH;
				rushDetected = true;
				Game.getInstance().printf("WARNING: Marine rush imminent.");
			}
		} else if (unit.getType() == UnitType.TERRAN_FACTORY) {
			if (builder.getGameStage() == GameStage.EARLY && !rushDetected && scout.getNumberOf(UnitType.TERRAN_FACTORY) == 2) {
				myState = State.VULTURE_LIKELY;
				Game.getInstance().printf("ALERT: 2 factories detected, vulture rush likely.");
			}
		} else if (unit.getType() == UnitType.TERRAN_VULTURE && myState == State.VULTURE_LIKELY) {
			myState = State.VULTURE_CONFIRMED;
			myAlert = Alert.EARLY_RUSH;
			rushDetected = true;
			Game.getInstance().printf("WARNING: Vultures approaching, build defenses.");
		} else if (unit.getType() == UnitType.TERRAN_STARPORT && !airDetected) {
			myAlert = Alert.AIR_STRUCTURES;
			Game.getInstance().printf("ALERT: Starport detected; build anti-airs.");
		} else if (unit.getType().isFlyer() && !airDetected) {
			myAlert = Alert.AIR_UNITS;
			airDetected = true;
			Game.getInstance().printf("WARNING: Air units approaching!");
		} else if (unit.getType() == UnitType.TERRAN_COMMAND_CENTER && !expanded) {
			myAlert = Alert.EXPANSION;
			expanded = true;
			Game.getInstance().printf("ALERT: Enemy is expanding!");
		} else if (unit.canCloak() && !cloakingDetected) {
			myAlert = Alert.CLOAKED_UNITS;
			cloakingDetected = true;
			Game.getInstance().printf("ALERT: Units with cloaking capability detected.");
		}
	}
}
