package finalBot;

import java.util.Arrays;
import java.util.List;
import org.bwapi.proxy.model.Game;
import org.bwapi.proxy.model.Player;
import org.bwapi.proxy.model.ROUnit;
import org.bwapi.proxy.model.Race;
import org.bwapi.proxy.model.UnitType;

import edu.berkeley.nlp.starcraft.AbstractCerebrate;
import edu.berkeley.nlp.starcraft.Cerebrate;
import edu.berkeley.nlp.starcraft.Strategy;
import edu.berkeley.nlp.starcraft.scripting.Command;
import edu.berkeley.nlp.starcraft.scripting.JythonInterpreter;
import edu.berkeley.nlp.starcraft.scripting.Thunk;
import edu.berkeley.nlp.starcraft.util.UnitUtils;

public class FinalBot extends AbstractCerebrate implements Strategy {
	private JythonInterpreter jython = new JythonInterpreter();
	private Governor governor;
	private Commander commander;
	private Spy spy;
	private Specialist specialist;
	private Player me;
	private Player enemy;

	@Override
	public List<Cerebrate> getTopLevelCerebrates() {
		initializeJython();
		return Arrays.<Cerebrate>asList(jython,this);
	}


	@Override
	public void onFrame() {
		spy.act();
		governor.act();
		commander.act();
		specialist.update();
	}

	@Override
	public void onStart() {
		me = Game.getInstance().self();
		for (Player p : Game.getInstance().getPlayers())
			if (me.isEnemy(p))
				enemy = p;
		
		governor = new Governor();
		commander = new Commander();
		spy = new Spy();
		
		if (enemy.getRace() == Race.TERRAN)
			specialist = new TerranSpecialist();
		else if (enemy.getRace() == Race.PROTOSS)
			specialist = new ProtossSpecialist();
		else
			specialist = new ZergSpecialist();
		
		spy.setCommander(commander);
		spy.setGovernor(governor);
		spy.setSpecialist(specialist);
		commander.setGovernor(governor);
		commander.setSpy(spy);
		commander.setSpecialist(specialist);
		governor.setCommander(commander);
		governor.setSpy(spy);
		governor.setSpecialist(specialist);
		specialist.setGovernor(governor);
		specialist.setCommander(commander);
		specialist.setSpy(spy);
		
		List<ROUnit> myWorkers = UnitUtils.getAllMy(UnitType.TERRAN_SCV);
		for(ROUnit w: myWorkers){
			governor.addWorker(UnitUtils.assumeControl(w));
		}
	}

	@Override
	public void onUnitCreate(ROUnit unit) {
		if(me.isEnemy(unit.getPlayer()))
			return;
		
		if(unit.getType().isWorker()){
			governor.addWorker(UnitUtils.assumeControl(unit));
		} else if(unit.getType()==UnitType.TERRAN_MARINE || unit.getType()==UnitType.TERRAN_MEDIC){
			commander.addAttacker(UnitUtils.assumeControl(unit));
		}
	}

	@Override
	public void onUnitDestroy(ROUnit unit) {
		if(me.isEnemy(unit.getPlayer())){
			spy.removeEnemyUnit(unit);
			return;
		}
		
		if(unit.getType().isWorker()){
			governor.removeWorker(UnitUtils.assumeControl(unit));
		} else if(unit.getType()==UnitType.TERRAN_MARINE || unit.getType()==UnitType.TERRAN_MEDIC){
			commander.removeAttacker(UnitUtils.assumeControl(unit));
		}
	}

	@Override
	public void onUnitHide(ROUnit unit) {

	}

	@Override
	public void onUnitMorph(ROUnit unit) {

	}

	@Override
	public void onUnitShow(ROUnit unit) {
		if(me.isEnemy(unit.getPlayer())){
			spy.addEnemyUnit(unit);
			specialist.update(unit);
			return;
		}
	}


	@Override
	public void onEnd(boolean isWinnerFlag) {

	}

	// Feel free to add command and things here.
	// bindFields will bind all member variables of the object
	// commands should be self explanatory...
	protected void initializeJython() {
		jython.bindFields(this);
		jython.bind("game", Game.getInstance());
		jython.bindIntCommand("speed",new Command<Integer>() {
			@Override
			public void call(Integer arg) {
				Game.getInstance().printf("Setting speed to %d",arg);
				Game.getInstance().setLocalSpeed(arg);	      
			}
		});
		jython.bindThunk("reset",new Thunk() {

			@Override
			public void call() {
				initializeJython();

			}

		});

	}


}

