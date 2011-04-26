package finalBot;

import org.bwapi.proxy.model.ROUnit;

public abstract class Specialist extends Overseer {
	protected Alert myAlert;
	protected int lastFrameObserved;
	protected boolean rushDetected;
	protected boolean cloakingDetected;
	protected boolean airDetected;
	protected boolean noDefense;
	
	//TODO: Figure out how long LATENCY should be
	protected final static int LATENCY = 10000;
	
	abstract public void update();
	abstract public void update(ROUnit unit);
	
	public Alert getAlert() {
		return myAlert;
	}

}
