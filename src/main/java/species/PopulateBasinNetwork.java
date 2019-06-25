package species;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import environment.Basin;
import fr.cemagref.simaqualife.kernel.processes.AquaNismsGroupProcess;
import species.DiadromousFish.Gender;

import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = AquaNismsGroupProcess.class)
public class PopulateBasinNetwork extends AquaNismsGroupProcess<DiadromousFish, DiadromousFishGroup> {

	private int nbSIPerBasin=200;
	private int nbFishPerSI=2500;
	private double initialLength =2.;
	 //TODO rename the attributes
	public static void main(String[] args) { System.out.println((new
			XStream(new DomDriver())) .toXML(new PopulateBasinNetwork())); }
	
	@Override
	public void doProcess(DiadromousFishGroup group) {
		for (Basin basin : group.getEnvironment().getRiverBasins()){
			for (int i=0; i < nbSIPerBasin; i++){
				group.addAquaNism(new DiadromousFish(group.getPilot(), basin, initialLength, nbFishPerSI, Gender.UNDIFFERENCIED));
			}
		}
	}
}
