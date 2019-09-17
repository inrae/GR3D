package species;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import environment.Basin;
import fr.cemagref.simaqualife.kernel.processes.AquaNismsGroupProcess;
import species.DiadromousFish.Gender;

import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = AquaNismsGroupProcess.class)
public class PopulateWithASinglePopulation extends AquaNismsGroupProcess<DiadromousFish, DiadromousFishGroup> {

	private int nbSIPerBasin=100;
	private long nbFishPerSI = 10; 
	private int bassinInd =12;
	private double initialLength =2.;

	public static void main(String[] args) { System.out.println((new
			XStream(new DomDriver())) .toXML(new PopulateWithASinglePopulation())); }

	@Override
	public void doProcess(DiadromousFishGroup group) {
		int nbFemaleSIPerBasin = nbSIPerBasin / 2;
		int nbMaleSIPerBasin = nbSIPerBasin - nbFemaleSIPerBasin;
		Basin basin = group.getEnvironment().getRiverBasins()[bassinInd];
		for (int i=0; i < nbFemaleSIPerBasin; i++){
			group.addAquaNism(new DiadromousFish(group.getPilot(), basin, initialLength, nbFishPerSI, Gender.FEMALE));
		}
		for (int i=0; i < nbMaleSIPerBasin; i++){
			group.addAquaNism(new DiadromousFish(group.getPilot(), basin, initialLength, nbFishPerSI, Gender.MALE));
		}
	}
}

