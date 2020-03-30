package species;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import environment.Basin;
import environment.Time;
import environment.Time.Season;
import fr.cemagref.simaqualife.kernel.processes.AquaNismsGroupProcess;
import species.DiadromousFish.Gender;

import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = AquaNismsGroupProcess.class)
public class PopulateBasinNetworkSeveralTimes extends AquaNismsGroupProcess<DiadromousFish, DiadromousFishGroup> {

	private int nbSIPerBasin=200;
	private int nbFishPerSI=2500;
	private double initialLength =20.;

	private int timesOfPopulate = 10;
	private Season populateSeason = Season.SPRING;

	public static void main(String[] args) { System.out.println((new
			XStream(new DomDriver())) .toXML(new PopulateBasinNetworkSeveralTimes())); }

	@Override
	public void doProcess(DiadromousFishGroup group) {

		if (group.getEnvironment().getTime().getNbYearFromBegin(group.getPilot()) <= timesOfPopulate &&
				group.getEnvironment().getTime().getSeason(group.getPilot()) == populateSeason){

			int nbFemaleSIPerBasin = nbSIPerBasin / 2;
			int nbMaleSIPerBasin = nbSIPerBasin - nbFemaleSIPerBasin;

			for (Basin basin : group.getEnvironment().getRiverBasins()){
				for (int i=0; i < nbFemaleSIPerBasin; i++){
					group.addAquaNism(new DiadromousFish(group.getPilot(), basin, initialLength, nbFishPerSI, Gender.FEMALE));
				}
				for (int i=0; i < nbMaleSIPerBasin; i++){
					group.addAquaNism(new DiadromousFish(group.getPilot(), basin, initialLength, nbFishPerSI, Gender.MALE));
				}
			}
		}
		
		//System.out.println("fem: "+ group. getFemaleSpawnerEffective()+ " \tmal"+group.getMaleSpawnerEffective());
	}
}
