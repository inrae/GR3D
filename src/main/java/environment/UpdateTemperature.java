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

		if (Time.getYear(group.getPilot())>nbYearBeforeWarming & Time.getYear(group.getPilot())<nbYearForStopWarming){

			for (Basin basin : group.getEnvironment().getBasins()){			
				basin.setFallTemperature(basin.getFallTemperature() + tempValueOfCenturyWarmingAutomn / (incrementDuration/Time.getSeasonDuration()));
				basin.setWinterTemperature(basin.getWinterTemperature() + tempValueOfCenturyWarmingWinter / (incrementDuration/Time.getSeasonDuration()));
				basin.setSummerTemperature(basin.getSummerTemperature() + tempValueOfCenturyWarmingSummer / (incrementDuration/Time.getSeasonDuration()));
				basin.setSpringTemperature(basin.getSpringTemperature() + tempValueOfCenturyWarmingSpring / (incrementDuration/Time.getSeasonDuration()));
				//System.out.println("la température dans le bassin " + basin.getId() + " dont le nom est " + basin.getName() + " vaut " + basin.getCurrentTemperature() + " en "+  Time.getSeason() + " de l'année " + Time.getYear());
			}		
		}
	}
}
