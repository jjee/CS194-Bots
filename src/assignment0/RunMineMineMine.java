package assignment0;

import org.bwapi.unit.model.BroodwarGameType;
import org.bwapi.unit.model.BroodwarRace;
import org.junit.Test;

import edu.berkeley.nlp.starcraft.GeneralTest;

public class RunMineMineMine extends GeneralTest {
	@Test
	public void runMining() {
		GeneralTest.test(new MineMineMine(), tourneyMap("SpaceUMS","t3"), new BroodwarRace[]{BroodwarRace.Protoss,BroodwarRace.Protoss}, BroodwarGameType.MELEE);
	}
}
