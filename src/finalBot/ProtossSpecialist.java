package finalBot;

import org.bwapi.proxy.model.Game;
import org.bwapi.proxy.model.ROUnit;
import org.bwapi.proxy.model.UnitType;

public class ProtossSpecialist extends Specialist {
	private State myState;
	private boolean expanded;
	
	private enum State {
		NONE,
		ZEALOT_LIKELY,
		ZEALOT_CONFIRMED
	}
	
	public ProtossSpecialist() {
		myState = State.NONE;
		myAlert = Alert.NONE;
		rushDetected = false;
		airDetected = false;
		cloakingDetected = false;
		expanded = false;
	}
	
	@Override
	public void update() {
		if (Game.getInstance().getFrameCount() - lastFrameObserved > LATENCY) {
			myState = State.NONE;
			myAlert = Alert.NONE;
		}
		if (myAlert == Alert.NONE)
			return;
		
		//TODO: Formula for calculating defense depth
		int totalAttackPotential = 0;
		for (ROUnit u : scout.enemyGroundUnits()) {
			totalAttackPotential += u.getGroundWeaponDamage() / (u.getGroundWeaponCooldown()+1) / 3;
		}
		if (totalAttackPotential < 10 && scout.getStaticDef() < 2) {
			myAlert = Alert.NO_DEFENSE;
			Game.getInstance().printf("Opponent lacks defense, build up attack!");
		}
	}

	public void update(ROUnit unit) {
		lastFrameObserved = Game.getInstance().getFrameCount();
		if (unit.getType() == UnitType.PROTOSS_GATEWAY) {
			if (builder.getGameStage() == GameStage.EARLY && !rushDetected && scout.getNumberOf(UnitType.PROTOSS_GATEWAY) == 2) {
				myState = State.ZEALOT_LIKELY;
				Game.getInstance().printf("ALERT: 2 gateways detected, zealot rush likely.");
			}
		} else if (unit.getType() == UnitType.PROTOSS_ZEALOT && myState == State.ZEALOT_LIKELY) {
			myState = State.ZEALOT_CONFIRMED;
			myAlert = Alert.EARLY_RUSH;
			rushDetected = true;
			Game.getInstance().printf("WARNING: Zealots coming, build defenses.");
		} else if (builder.getGameStage() == GameStage.EARLY && unit.getType() == UnitType.PROTOSS_TEMPLAR_ARCHIVES) {
			myAlert = Alert.CLOAKED_UNITS;
			cloakingDetected = true;
			Game.getInstance().printf("WARNING: DT rush likely!");
		} else if (unit.getType() == UnitType.PROTOSS_STARGATE && !airDetected) {
			myAlert = Alert.AIR_STRUCTURES;
			Game.getInstance().printf("ALERT: Stargate detected; build anti-airs.");
		} else if (unit.getType().isFlyer() && !airDetected) {
			myAlert = Alert.AIR_UNITS;
			airDetected = true;
			Game.getInstance().printf("WARNING: Air units approaching!");
		} else if (unit.getType() == UnitType.PROTOSS_NEXUS && scout.getNumberOf(UnitType.PROTOSS_NEXUS) == 2 && !expanded) {
			myAlert = Alert.EXPANSION;
			expanded = true;
			Game.getInstance().printf("ALERT: Enemy is expanding!");
		} 
	}

}
