package finalBot;

public abstract class Overseer {
	protected Governor builder;
	protected Commander attacker;
	protected Spy scout;
	protected TerranSpecialist terranSpecialist;
	protected ProtossSpecialist protossSpecialist;
	protected ZergSpecialist zergSpecialist;
	
	public void setGovernor(Governor builder) {
		this.builder = builder;
	}
	
	public void setCommander(Commander attacker) {
		this.attacker = attacker;
	}
	
	public void setSpy(Spy scout) {
		this.scout = scout;
	}
	
	public void setTerranSpecialist(TerranSpecialist terranSpecialist) {
		this.terranSpecialist = terranSpecialist;
	}
	
	public void setTerranSpecialist(ProtossSpecialist protossSpecialist) {
		this.protossSpecialist = protossSpecialist;
	}
	
	public void setTerranSpecialist(ZergSpecialist zergSpecialist) {
		this.zergSpecialist = zergSpecialist;
	}
	
	public abstract void act();
}
