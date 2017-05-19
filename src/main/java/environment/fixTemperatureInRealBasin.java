package environment;

import java.util.Map;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import species.DiadromousFish;
import species.DiadromousFishGroup;
import fr.cemagref.simaqualife.kernel.processes.AquaNismsGroupProcess;

public class fixTemperatureInRealBasin extends AquaNismsGroupProcess<DiadromousFish, DiadromousFishGroup> {

	private Long selectedYear = (long) 2000;

	public static void main(String[] args) { System.out.println((new
			XStream(new DomDriver())) .toXML(new fixTemperatureInRealBasin())); }

	@Override
	public void doProcess(DiadromousFishGroup group) {

		Map<String, Double[]> temperaturesbasin = ((BasinNetworkReal) group.getEnvironment()).getTemperaturesBasin(selectedYear);

		for (Basin basin : group.getEnvironment().getBasins()){		

			if (basin instanceof RiverBasin){
				//System.out.println(basin.getName());
				//System.out.println(temperaturesbasin.get(basin.getName()));
				basin.setWinterTemperature(temperaturesbasin.get(basin.getName())[0]);
				basin.setSpringTemperature(temperaturesbasin.get(basin.getName())[1]);
				basin.setSummerTemperature(temperaturesbasin.get(basin.getName())[2]);
				basin.setFallTemperature(temperaturesbasin.get(basin.getName())[3]);
			} else if (basin instanceof SeaBasin) {
				//System.out.println(basin.getName()+"-->"+group.getEnvironment().getAssociatedSeaBasin(basin));
				//System.out.println(temperaturesbasin.get(basin.getName()));
				basin.setWinterTemperature(temperaturesbasin.get(group.getEnvironment().getAssociatedRiverBasin(basin).getName())[0]);
				basin.setSpringTemperature(temperaturesbasin.get(group.getEnvironment().getAssociatedRiverBasin(basin).getName())[1]);
				basin.setSummerTemperature(temperaturesbasin.get(group.getEnvironment().getAssociatedRiverBasin(basin).getName())[2]);
				basin.setFallTemperature(temperaturesbasin.get(group.getEnvironment().getAssociatedRiverBasin(basin).getName())[3]);
			}		
		}
	}
}

