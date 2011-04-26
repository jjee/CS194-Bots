package finalBot;

import org.bwapi.proxy.model.Game;
import org.bwapi.proxy.model.ROUnit;
import org.bwapi.proxy.model.UnitType;

public class ZergSpecialist extends Specialist {
	private boolean greedy;
	private boolean noDefense = false;
	
	public ZergSpecialist() {
		myAlert = Alert.NONE;
		rushDetected = false;
		airDetected = false;
		cloakingDetected = false;
		greedy = false;
		noDefense = false;
	}
	
	public void update() {
		if (Game.getInstance().getFrameCount() - lastFrameObserved > LATENCY) {
			myAlert = Alert.NONE;
			noDefense = false;
		} 
		if (myAlert == Alert.NONE)
			return;
		
		//TODO: Formula for calculating defense depth
		int totalAttackPotential = 0;
		for (ROUnit u : scout.enemyGroundUnits()) {
			totalAttackPotential += u.getGroundWeaponDamage() / (u.getGroundWeaponCooldown()+1);
		}
		if (totalAttackPotential < 6 && scout.getStaticDef() < 2 && !noDefense) {
			noDefense = true;
			myAlert = Alert.NO_DEFENSE;
			noDefense = true;
			Game.getInstance().printf("Opponent lacks defense, build up attack!");
		}
	}

	public void update(ROUnit unit) {
		lastFrameObserved = Game.getInstance().getFrameCount();
		if (unit.getType() == UnitType.ZERG_SPAWNING_POOL && builder.getGameStage() == GameStage.EARLY && !rushDetected) {
			if (scout.getNumberOf(UnitType.ZERG_DRONE) <= 5) {
				rushDetected = true;
				myAlert = Alert.EARLY_RUSH;
				Game.getInstance().printf("WARNING: Opponent going for 4/5-pool.");
			}
		} else if (unit.getType() == UnitType.ZERG_HATCHERY && builder.getGameStage() == GameStage.EARLY) {
			if (scout.getNumberOf(UnitType.ZERG_SPAWNING_POOL) == 0 && !greedy) {
				greedy = true;
				myAlert = Alert.NO_DEFENSE;
				Game.getInstance().printf("ALERT: Zerg opponent going greedy with second hatchery.");
			}
		} else if (unit.getType() == UnitType.ZERG_SPIRE && !airDetected) {
			myAlert = Alert.AIR_STRUCTURES;
			Game.getInstance().printf("ALERT: Spire detected, mutalisks likely.");
		} else if (unit.getType() == UnitType.ZERG_MUTALISK && !airDetected) {
			myAlert = Alert.AIR_UNITS;
			airDetected = true;
			Game.getInstance().printf("WARNING: Mutalisks detected, prepare air defense!");
		} else if (unit.getType() == UnitType.ZERG_HYDRALISK_DEN && !cloakingDetected) {
			Game.getInstance().printf("ALERT: Hydralisk den detected, possibility of lurkers.");
		} else if (unit.getType() == UnitType.ZERG_LURKER && !cloakingDetected) {
			cloakingDetected = true;
			myAlert = Alert.CLOAKED_UNITS;
			Game.getInstance().printf("WARNING: Lurkers produced, prepare detectors!");
		}
	}
}
