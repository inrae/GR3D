package species;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import environment.Basin;
import environment.RiverBasin;
import environment.SeaBasin;
import environment.Time;
import environment.Time.Season;
import fr.cemagref.simaqualife.kernel.processes.AquaNismsGroupProcess;
import species.DiadromousFish.Gender;

import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = AquaNismsGroupProcess.class)
public class PopulateBasinNetworkSeveralTimesAccordingToBasinSize extends AquaNismsGroupProcess<DiadromousFish, DiadromousFishGroup> {

	
	private int nbFishPerSI=2500;
	private double initialLength =2.;
	private double etaPopulate = 40; // parameter linking surface of a basin and S_etoile; Should be superior at eta (eta = 2.4 in ReproduceAndSurviveAfterReproduction). 
	private int timesOfPopulate = 5;
	private Season populateSeason = Season.SPRING;

	public static void main(String[] args) { System.out.println((new
			XStream(new DomDriver())) .toXML(new PopulateBasinNetworkSeveralTimesAccordingToBasinSize())); }

	@Override
	public void doProcess(DiadromousFishGroup group) {

		if (Time.getNbYearFromBegin(group.getPilot()) <= timesOfPopulate && Time.getSeason(group.getPilot()) == populateSeason){

			for (RiverBasin riverBasin : group.getEnvironment().getRiverBasins()){
			
				int numberOfFemaleToPopulate = (int) Math.round(etaPopulate* riverBasin.getAccessibleSurface()) ;
				int nbFemaleSI= numberOfFemaleToPopulate/nbFishPerSI ;
				int nbMaleSI = nbFemaleSI;
				
				for (int i=0; i < nbFemaleSI; i++){
					group.addAquaNism(new DiadromousFish(group.getPilot(), riverBasin, initialLength, nbFishPerSI, Gender.FEMALE));
				}
				for (int i=0; i < nbMaleSI; i++){
					group.addAquaNism(new DiadromousFish(group.getPilot(), riverBasin, initialLength, nbFishPerSI, Gender.MALE));
				}
			}
		}
		
		System.out.println("fem: "+ group. getFemaleSpawnerEffective()+ " \tmal"+group.getMaleSpawnerEffective());
	}
}



