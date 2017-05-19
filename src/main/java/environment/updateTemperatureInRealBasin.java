package environment;

import java.util.Map;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import species.DiadromousFish;
import species.DiadromousFishGroup;
import environment.Time.Season;
import fr.cemagref.simaqualife.kernel.processes.AquaNismsGroupProcess;

public class updateTemperatureInRealBasin extends AquaNismsGroupProcess<DiadromousFish, DiadromousFishGroup> {
	
	double offshoreTemperature = 12.;
	
	public static void main(String[] args) { System.out.println((new
			XStream(new DomDriver())) .toXML(new updateTemperatureInRealBasin())); }

	@Override
	public void doProcess(DiadromousFishGroup group) {
		// TODO Include a power in the equation

		if (Time.getSeason(group.getPilot()) == Season.WINTER){
			Map<String, Double[]> temperaturesbasin = ((BasinNetworkReal) group.getEnvironment()).getTemperaturesBasin(Time.getYear(group.getPilot()));

			for (Basin basin : group.getEnvironment().getBasins()){		
				if (basin instanceof RiverBasin){
					basin.setWinterTemperature(temperaturesbasin.get(basin.getName())[0]);
					basin.setSpringTemperature(temperaturesbasin.get(basin.getName())[1]);
					basin.setSummerTemperature(temperaturesbasin.get(basin.getName())[2]);
					basin.setFallTemperature(temperaturesbasin.get(basin.getName())[3]);					
				} else if (basin instanceof SeaBasin) {
					basin.setWinterTemperature((offshoreTemperature + temperaturesbasin.get(group.getEnvironment().getAssociatedRiverBasin(basin).getName())[0])/2.);
					basin.setSpringTemperature((offshoreTemperature + temperaturesbasin.get(group.getEnvironment().getAssociatedRiverBasin(basin).getName())[1])/2.);
					basin.setSummerTemperature((offshoreTemperature + temperaturesbasin.get(group.getEnvironment().getAssociatedRiverBasin(basin).getName())[2])/2.);
					basin.setFallTemperature((offshoreTemperature + temperaturesbasin.get(group.getEnvironment().getAssociatedRiverBasin(basin).getName())[3])/2.);
				}		
			}
		}
	}
}
