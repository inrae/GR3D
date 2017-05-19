package species;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import environment.Basin;
import environment.RiverBasin;
import environment.Time;
import environment.Time.Season;
import fr.cemagref.simaqualife.kernel.processes.AquaNismsGroupProcess;
import java.util.ArrayList;
import java.util.List;
import miscellaneous.Duo;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = AquaNismsGroupProcess.class)
public class MigrateToSea extends AquaNismsGroupProcess<DiadromousFish, DiadromousFishGroup> {

	private Season seaMigrationSeason = Season.SUMMER;	

	public static void main(String[] args) {
		System.out.println((new XStream(new DomDriver()))
				.toXML(new MigrateToSea()));
	}	

	@Override
	public void doProcess(DiadromousFishGroup group) {

		if (Time.getSeason(group.getPilot()) == seaMigrationSeason ){
			Basin destination;
                        List<Duo<DiadromousFish,Basin>> fishesToMove = new ArrayList<Duo<DiadromousFish,Basin>>();
                        for (int i = 0; i < group.getEnvironment().getRiverBasins().length; i++) {
                                RiverBasin basin = group.getEnvironment().getRiverBasins()[i];
                            
                                List<DiadromousFish> fishes = basin.getFishs(group);
                                if (fishes!=null) for (DiadromousFish fish : fishes) {
                                        destination = group.getEnvironment().getAssociatedSeaBasin(fish.getPosition());
                                        fishesToMove.add(new Duo<DiadromousFish, Basin>(fish, destination));
                                }       
                        }
                        for (Duo<DiadromousFish,Basin> duo : fishesToMove) {
                                duo.getFirst().moveTo(group.getPilot(), duo.getSecond(), group);
                        }
                }
	}
}
