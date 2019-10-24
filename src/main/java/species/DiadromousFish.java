package species;

import environment.Basin;
import environment.BasinNetwork;
import fr.cemagref.simaqualife.kernel.AquaNism;
import fr.cemagref.simaqualife.kernel.AquaNismsGroup;
import fr.cemagref.simaqualife.pilot.Pilot;

public class DiadromousFish extends AquaNism<Basin, BasinNetwork> {

	public static enum Stage {IMMATURE, MATURE};
	//TODO replace UNDIFFERENCIED by UNDETERMINED
	public static enum Gender {UNDIFFERENCIED, FEMALE, MALE}; 

	public enum SpawnerOrigin {AUTOCHTONOUS, ALLOCHTONOUS};
	
	private long amount;
	/**
	 *  age of the fish
	 * @unit (decimal) year
	 */
	private double age;
	private double length;
	private Basin birthBasin;
	private Stage stage;
	private Gender gender;    
    private int numberOfReproduction;

	
	public DiadromousFish(Pilot pilot, Basin position, double initialLength, long fishAmount, Gender gender) {
		super(pilot, position);
		this.age = 0.;
		this.length = initialLength;
		this.birthBasin = position;
		this.amount = fishAmount;
		this.stage = Stage.IMMATURE;
		this.gender = gender;		
		this.numberOfReproduction = 0;
	}
	
	
	public DiadromousFish duplicateWithNewPositionAndAmount(Pilot pilot, Basin newPosition, long newAmount){
		
		DiadromousFish newFish=new DiadromousFish(pilot, newPosition, this.getLength(), newAmount, this.gender);
		newFish.age = this.age;
		newFish.birthBasin =  this.birthBasin;
		newFish.stage = this.stage;
		newFish.numberOfReproduction = this.numberOfReproduction;
		
		return newFish;
		
	}
	
	public Stage getStage() {
		return stage;
	}

	public boolean isMature(){
		return this.stage== Stage.MATURE;
	}

	public void setStage(Stage stage) {
		this.stage = stage;
	}


	public int getNumberOfReproduction() {
		return numberOfReproduction;
	}

	public void incNumberOfReproduction() {
		this.numberOfReproduction ++;
	}


	public long getAmount() {
		return amount;
	}

	public void setAmount(long fishAmount) {
		this.amount = fishAmount;
	}

	public double getAge() {
		return age;
	}

	public void setAge(double age) {
		this.age = age;
	}

	public double getLength() {
		return length;
	}

	public void setLength(double length) {
		this.length = length;
	}

	public Basin getBirthBasin() {
		return birthBasin;
	}
		
	/**
	 * @return the gender
	 */
	public Gender getGender() {
		return gender;
	}


	@Override
	public <ANG extends AquaNismsGroup<?, BasinNetwork>> void moveTo(
			Pilot pilot, Basin destination, ANG group) {

		if (this.position != destination) {
			this.position.removeFish(this, (DiadromousFishGroup) group);
			destination.addFish(this, (DiadromousFishGroup) group);
		}

		super.moveTo(pilot, destination, group);
	}
}
