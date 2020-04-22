package species;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import environment.Basin;
import environment.Time;
import environment.Time.Season;
import fr.cemagref.simaqualife.kernel.processes.AquaNismsGroupProcess;
import org.openide.util.lookup.ServiceProvider;
@Deprecated
@ServiceProvider(service = AquaNismsGroupProcess.class)
public class MigrateToRiver extends AquaNismsGroupProcess<DiadromousFish, DiadromousFishGroup> {


	private Season riverMigrationSeason = Season.SPRING;

	public static void main(String[] args) {
		System.out.println((new XStream(new DomDriver()))
				.toXML(new MigrateToRiver()));
	}	

	@Override
	public void doProcess(DiadromousFishGroup group) {

		if (group.getEnvironment().getTime().getSeason(group.getPilot()) == riverMigrationSeason ){
			Basin destination = null;
			for (DiadromousFish fish : group.getAquaNismsList()){
				destination = group.getEnvironment().getAssociatedRiverBasin(fish.getPosition());
				fish.moveTo(group.getPilot(), destination, group);
			}
		}
	}
}

