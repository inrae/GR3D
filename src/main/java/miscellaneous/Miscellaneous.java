package miscellaneous;

import fr.cemagref.simaqualife.pilot.Pilot;
import umontreal.iro.lecuyer.randvar.NormalGen;
import umontreal.iro.lecuyer.randvar.UniformGen;

public class Miscellaneous {

	public static long binomialForSuperIndividual(Pilot pilot, long amount, double succesProba, long threshold) {
			long amountWithSuccess;
			if (amount > threshold) { // use a normal approximation for huge amount 
				/*			double p=-1.;
	             while (p<0 | p>1){
	             p = genAleaNormal.nextDouble() * 
	             Math.sqrt(succesProba * (1 - succesProba) /amount) + succesProba;
	             }
	             amountWithSuccess = (long) Math.round(p* amount);*/
				amountWithSuccess = -1;
				while (amountWithSuccess < 0 | amountWithSuccess > amount) {
					amountWithSuccess = Math.round(NormalGen.nextDouble(pilot.getRandomStream(), 0., 1.) * Math.sqrt(succesProba * (1 - succesProba) * amount)
							+ succesProba * amount);
				}

			} else {
				UniformGen aleaGen = new UniformGen(pilot.getRandomStream(), 0., 1.);
				amountWithSuccess = 0;
				for (long i = 0; i < amount; i++) {
					if (aleaGen.nextDouble() < succesProba) {
						amountWithSuccess++;
					}
				}
			}
			return amountWithSuccess;
		}
	public static long binomialForSuperIndividual(Pilot pilot, long amount, double succesProba) {
		return binomialForSuperIndividual(pilot, amount, succesProba, 50);
	}

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


