package miscellaneous;

import fr.cemagref.simaqualife.pilot.Pilot;
import umontreal.iro.lecuyer.randvar.NormalGen;
import umontreal.iro.lecuyer.randvar.UniformGen;

public class Miscellaneous {

	static public double temperatureEffect(double T, double Tmin, double Topt, double Tmax) {
		if (T <= Tmin || T >= Tmax) {
			return 0;
		} else {
			return (T - Tmin) * (T - Tmax) / ((T - Tmin) * (T - Tmax) - ((T - Topt) * (T-Topt)));
		}
	}

	static public double rectangularTemperatureEffect(double T, double Tmin, double Tmax) {
		if (T <= Tmin || T >= Tmax) {
			return 0;
		} else {
			return 1;
		}
	}
}


