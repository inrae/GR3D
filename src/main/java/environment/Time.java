package environment;

import fr.cemagref.simaqualife.pilot.Pilot;

public final class Time {


	public static enum Season {WINTER, SPRING, SUMMER, AUTOMN};

	public static Season getSeason(Pilot pilot){
		return getSeason(pilot.getCurrentTime());
	}

	public static Season getSeason(long time){
		return Season.values()[(int)time% Season.values().length];
	}

	public static long getYear(Pilot pilot){
		return getNbYearFromBegin(pilot.getCurrentTime())+
				((BasinNetwork) pilot.getAquaticWorld().getEnvironment()).getYearOfTheBegin();		
	}
	
	public static long getNbYearFromBegin(long time){
		return (long) Math.floor(time / Season.values().length);		
	}

	public static double getSeasonDuration(){
		return 1./ Season.values().length;
	}
}
