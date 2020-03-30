package species;

import environment.Time;
import fr.cemagref.simaqualife.kernel.processes.AquaNismsGroupProcess;

import org.openide.util.lookup.ServiceProvider;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import environment.Basin;

@ServiceProvider(service = AquaNismsGroupProcess.class)
public class Age extends AquaNismsGroupProcess<DiadromousFish, DiadromousFishGroup> {

	public static void main(String[] args) { System.out.println((new
			XStream(new DomDriver())) .toXML(new Age())); }
	
	@Override
	public void doProcess(DiadromousFishGroup group) {
                for(Basin basin : group.getEnvironment().getBasins()){
                    if (basin.getFishs(group)!=null) for(DiadromousFish fish : basin.getFishs(group)){
			//Age
			fish.setAge(fish.getAge() + group.getEnvironment().getTime().getSeasonDuration());
                    }
                }
	}
}
