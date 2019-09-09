package species;


import java.util.List;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import environment.Basin;
import environment.Time;
import fr.cemagref.simaqualife.kernel.AquaNismsGroup;
import fr.cemagref.simaqualife.kernel.processes.AquaNismsGroupProcess;
import fr.cemagref.simaqualife.kernel.util.TransientParameters.InitTransientParameters;
import fr.cemagref.simaqualife.pilot.Pilot;
import miscellaneous.Miscellaneous;

import org.openide.util.lookup.ServiceProvider;

import species.DiadromousFish.Gender;
import species.DiadromousFish.Stage;
import umontreal.iro.lecuyer.probdist.NormalDist;
import umontreal.iro.lecuyer.randvar.NormalGen;

@ServiceProvider(service = AquaNismsGroupProcess.class)
public class Grow extends AquaNismsGroupProcess<DiadromousFish, DiadromousFishGroup> {

	/**
	 * temperature minimum for growth
	 * @unit °C
	 */
	private double tempMinGrow = 3.;

	/**
	 * temperature maximum for growth
	 * @unit °C
	 */
	private double tempMaxGrow = 26.; 

	/**
	 * temperature optimal for growth
	 * @unit °C
	 */
	private double tempOptGrow = 17.;

	/**
	 * K, Brody growth rate at optimal temperature
	 * L = Linf *(1-exp(-K*(t-t0))
	 * @unit year -1
	 */
	private double kOptForFemale= 0.3;

	
	/**
	 * @return the kOptForFemale
	 */
	public double getkOptForFemale() {
		return kOptForFemale;
	}

	/**
	 * @param kOptForFemale the kOptForFemale to set
	 */
	public void setkOptForFemale(double kOptForFemale) {
		this.kOptForFemale = kOptForFemale;
	}

	/**
	 * @return the kOptForMale
	 */
	public double getkOptForMale() {
		return kOptForMale;
	}

	/**
	 * @param kOptForMale the kOptForMale to set
	 */
	public void setkOptForMale(double kOptForMale) {
		this.kOptForMale = kOptForMale;
	}

	/**
	 * K, Brody growth rate at optimal temperature
	 * L = Linf *(1-exp(-K*(t-t0))
	 * @unit year -1
	 */
	private double kOptForMale= 0.3;
	
	/**
	 * standart deviation for the lognormal random draw of growth increment
	 * @unit cm
	 */
	private double sigmaDeltaLVonBert = 0.2;

	private transient NormalGen genNormal;

	public static void main(String[] args) { System.out.println((new
			XStream(new DomDriver())) .toXML(new Grow())); }

	@Override
	@InitTransientParameters
	public void initTransientParameters(Pilot pilot) {
		super.initTransientParameters(pilot);
		genNormal = new NormalGen( pilot.getRandomStream(),
				new NormalDist(0., 1.));		
	}

	
	
	@Override
	public void doProcess(DiadromousFishGroup group) {
		for(Basin basin : group.getEnvironment().getBasins()){
			if (basin.getFishs(group)!=null) 
				for(DiadromousFish fish : basin.getFishs(group)){
					double muDeltaLVonBert = 0.;
					double kVonBert = 0.;
					double growthIncrement = 0.;
					
					// 1) calculate the kVonBert 
					//System.out.println(this.getKOpt(fish, group) );
						kVonBert = this.getKOpt(fish, group) *
								Miscellaneous.temperatureEffect(fish.getPosition().getCurrentTemperature(group.getPilot()), tempMinGrow, tempOptGrow, tempMaxGrow);

					// 2) Update the fish length with a lognormal normal draw  of increment
					// limit the fish length to Linf
					if (fish.getLength() < group.getLinfVonBert(fish)){
						muDeltaLVonBert = Math.log((group.getLinfVonBert(fish) - fish.getLength()) * (1 - Math.exp(-kVonBert * Time.getSeasonDuration()))) - (sigmaDeltaLVonBert*sigmaDeltaLVonBert)/2;
						growthIncrement = Math.exp(genNormal.nextDouble()*sigmaDeltaLVonBert + muDeltaLVonBert);
					
						
						fish.setLength(Math.min(group.getLinfVonBert(fish), fish.getLength() + growthIncrement));											
					}
					else {
						fish.setLength(group.getLinfVonBert(fish));
					}
					//System.out.println(fish.getAge() + " -> "+ fish.getLength() + " ("+fish.getStage()+"): "+ growthIncrement);
					// test if fish become mature
					if (fish.getStage() == Stage.IMMATURE && fish.getLength() > group.getlFirstMaturity(fish)){
							fish.setStage(Stage.MATURE); 
					}
					//System.out.println("la temp�rature du lieu de vie du poisson est :" + fish.getPosition().getCurrentTemperature() + ", la saison est :" + Time.getSeason() + " et sa nouvelle taille est :" + fish.getLength());
				}
		}
	}
	
	/**
	 * @param fish
	 * @param group
	 * @return the Brody coeff   from Diadromousgroup if exists or from this grow process
	 * depends of the fish gender .In case of undifferentiaced fish, the mean for male and female is considered
	 */
	public  double getKOpt(DiadromousFish fish, DiadromousFishGroup group) {
		 double kOpt = group.getKOpt(fish);
		 if (Double.isNaN(kOpt)){ // no definition for the group
				if (fish.getGender() == Gender.FEMALE)
					kOpt = kOptForFemale;
				else if (fish.getGender() == Gender.MALE)
					kOpt = kOptForMale;
				else
					kOpt=  (kOptForFemale + kOptForMale) / 2.;
		 }	 
		 
		 return kOpt;
		
		
	}
}
