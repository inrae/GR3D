package environment;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import species.DiadromousFish;
import species.DiadromousFishGroup;
import fr.cemagref.simaqualife.kernel.processes.AquaNismsGroupProcess;

public class UpdateAccesibleSurface extends AquaNismsGroupProcess<DiadromousFish, DiadromousFishGroup> {

	public static void main(String[] args) { System.out.println((new
			XStream(new DomDriver())) .toXML(new UpdateAccesibleSurface())); }

	@Override
	public void doProcess(DiadromousFishGroup group) {
		if (group.isThereBasinToUpdate()){

			if (group.getEnvironment().getTime().getYear(group.getPilot()) >= group.getYearOfTheUpdate()){
				for (RiverBasin riverBasin : group.getEnvironment().getRiverBasins()){
					if (! Double.isNaN(group.getPaccessible(riverBasin.getName()))){
						riverBasin.setPAccessible(group.getPaccessible(riverBasin.getName()));
					}
				}
			}
		}
	}
}






