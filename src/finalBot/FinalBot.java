package finalBot;

import java.util.Arrays;
import java.util.List;
import org.bwapi.proxy.model.Game;
import org.bwapi.proxy.model.ROUnit;
import edu.berkeley.nlp.starcraft.AbstractCerebrate;
import edu.berkeley.nlp.starcraft.Cerebrate;
import edu.berkeley.nlp.starcraft.Strategy;
import edu.berkeley.nlp.starcraft.scripting.Command;
import edu.berkeley.nlp.starcraft.scripting.JythonInterpreter;
import edu.berkeley.nlp.starcraft.scripting.Thunk;

public class FinalBot extends AbstractCerebrate implements Strategy {
	JythonInterpreter jython = new JythonInterpreter();
	

	@Override
  public List<Cerebrate> getTopLevelCerebrates() {
		initializeJython();
	  return Arrays.<Cerebrate>asList(jython,this);
  }


	@Override
  public void onFrame() {
	
  }

	@Override
  public void onStart() {
	
  }

	@Override
  public void onUnitCreate(ROUnit unit) {
	
  }

	@Override
  public void onUnitDestroy(ROUnit unit) {
	  
  }

	@Override
  public void onUnitHide(ROUnit unit) {
	  
  }

	@Override
  public void onUnitMorph(ROUnit unit) {
	  
  }
	
	@Override
  public void onUnitShow(ROUnit unit) {
	  
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

