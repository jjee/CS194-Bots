package finalBot;

public abstract class Overseer {
	protected Governor builder;
	protected Commander attacker;
	protected Spy scout;
	protected Specialist specialist;
	
	public void setGovernor(Governor builder) {
		this.builder = builder;
	}
	
	public void setCommander(Commander attacker) {
		this.attacker = attacker;
	}
	
	public void setSpy(Spy scout) {
		this.scout = scout;
	}
	
	public void setSpecialist(Specialist specialist) {
		this.specialist = specialist;
	}
}
