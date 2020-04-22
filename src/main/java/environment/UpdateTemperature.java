package environment;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import species.DiadromousFish;
import species.DiadromousFishGroup;
import fr.cemagref.simaqualife.kernel.processes.AquaNismsGroupProcess;

public class UpdateTemperature extends AquaNismsGroupProcess<DiadromousFish, DiadromousFishGroup> {

	private long nbYearBeforeWarming = 50; // in year
	private long nbYearForStopWarming = 150;
	private double tempValueOfCenturyWarmingWinter = 3.;
	private double tempValueOfCenturyWarmingSpring = 3.;
	private double tempValueOfCenturyWarmingSummer = 3.;
	private double tempValueOfCenturyWarmingAutomn = 3.;
	private double incrementDuration = 100.; // in year
	private double incrementPower = 1.; // 1=linear, 


	public static void main(String[] args) { System.out.println((new
			XStream(new DomDriver())) .toXML(new UpdateTemperature())); }

	@Override
	public void doProcess(DiadromousFishGroup group) {
		// TODO Include a power in the equation
		Time time = group.getEnvironment().getTime();
		if (time.getYear(group.getPilot())>nbYearBeforeWarming & 
				time.getYear(group.getPilot())<nbYearForStopWarming){

			for (Basin basin : group.getEnvironment().getBasins()){			
				basin.setFallTemperature(basin.getFallTemperature() + tempValueOfCenturyWarmingAutomn / (incrementDuration/time.getSeasonDuration()));
				basin.setWinterTemperature(basin.getWinterTemperature() + tempValueOfCenturyWarmingWinter / (incrementDuration/time.getSeasonDuration()));
				basin.setSummerTemperature(basin.getSummerTemperature() + tempValueOfCenturyWarmingSummer / (incrementDuration/time.getSeasonDuration()));
				basin.setSpringTemperature(basin.getSpringTemperature() + tempValueOfCenturyWarmingSpring / (incrementDuration/time.getSeasonDuration()));
				//System.out.println("la temp�rature dans le bassin " + basin.getId() + " dont le nom est " + basin.getName() + " vaut " + basin.getCurrentTemperature() + " en "+  time.getSeason() + " de l'ann�e " + time.getYear());
			}		
		}
	}
}
