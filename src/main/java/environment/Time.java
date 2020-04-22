package environment;

import fr.cemagref.simaqualife.pilot.Pilot;

public class Time {


	public static enum Season {WINTER, SPRING, SUMMER, AUTOMN};

	public  Season getSeason(Pilot pilot){
		return getSeason(pilot.getCurrentTime());
	}

	// use dans MyCSvObserver
	public static  Season getSeason(long time){
		return Season.values()[(int)time% Season.values().length];
	}

	public  long getYear(Pilot pilot){
		return getNbYearFromBegin(pilot.getCurrentTime())+
				((BasinNetwork) pilot.getAquaticWorld().getEnvironment()).getYearOfTheBegin();		
	}
	
	public  long getNbYearFromBegin(Pilot pilot){
		return getNbYearFromBegin(pilot.getCurrentTime());

	}
	
	// use dans MyCSvObserver
	public static long getNbYearFromBegin(long time){
		return (long) Math.floor(time / Season.values().length);		
	}
	

	/**
	 * @return the duration of season (time step) 
	 * @unit year
	 */
	public  double getSeasonDuration(){
		return 1./ Season.values().length;
	}
}
