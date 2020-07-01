/**
 * patrick
 * @author Patrick Lambert
 * @copyright Copyright (c) 2018, Irstea
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.

 */
package miscellaneous;

import fr.cemagref.simaqualife.pilot.Pilot;
import umontreal.iro.lecuyer.randvar.NormalGen;
import umontreal.iro.lecuyer.randvar.UniformGen;
import umontreal.iro.lecuyer.rng.RandomStream;

/**
 *
 */
public class BinomialForSuperIndividualGen {

	private long threshold;

	private NormalGen normalGen;

	private UniformGen uniformGen;


	public BinomialForSuperIndividualGen(RandomStream randomStream, long threshold) {
		normalGen = new NormalGen(randomStream, 0., 1.);
		// (NormalACRGen to speed up execution give weird result
		uniformGen = new UniformGen(randomStream, 0., 1.);

		this.threshold = threshold;
	}


	public BinomialForSuperIndividualGen(RandomStream randomStream) {
		this(randomStream, 50L);
	}


	public long getSuccessNumber(long draws, double succesProbability) {

		if (draws >= threshold) {
			double mean = succesProbability * draws;
			double standardDeviation = Math.sqrt(succesProbability * (1 - succesProbability) * draws);
			// int nbTRy =0;
			if (3. * standardDeviation < 1.)
				return Math.round(mean);
			else {
				long successes = -1L;
				// approximate the binomial by a normal distribution of mean n p and variance n p (1-p)
				while (successes < 0 | successes > draws) {
					successes = Math.round(normalGen.nextDouble() * standardDeviation + mean);
					// nbTRy ++;
				}
				// if (nbTRy >1)
				// System.out.println("n =" + draws + "\t p= "+ succesProbability + "\tect = " + standardDeviation+ "
				// \tnp = " + Math.round(mean) +" --> " +nbTRy + " for " + successes);
				return successes;
			}

		} else {
			long successes = 0;
			for (long i = 0; i < draws; i++) {
				if (uniformGen.nextDouble() < succesProbability) {
					successes++;
				}
			}
			return successes;
		}
	}


	private long constantDraw(double mean) {
		return Math.round(mean);
	}


	private long normalDraw(long draws, double mean, double standardDeviation) {
		double successes = -1.;
		while (successes < 0 | successes > draws) {
			successes = Math.round(normalGen.nextDouble() * standardDeviation + mean);
		}
		return Math.round(successes);
	}


	private long binomialDraw(long draws, double succesProbability) {
		long successes = 0;
		for (successes = 0; successes < draws; successes++) {
			if (uniformGen.nextDouble() < succesProbability) {
				successes++;
			}
		}
		return successes;
	}


	public long getSuccessNumber2(long draws, double succesProbability) {

		if (draws >= threshold) {
			double mean = succesProbability * draws;
			double standardDeviation = Math.sqrt(succesProbability * (1 - succesProbability) * draws);

			if (3. * standardDeviation < 1.)
				return constantDraw(mean);
			else
				return normalDraw(draws, mean, standardDeviation);

		} else {
			return binomialDraw(draws, succesProbability);
		}
	}


	public static long getSuccessNumber(Pilot pilot, long amount, double succesProba, long threshold) {

		if (amount > threshold) { // use a normal approximation for huge amount
			/*
			 * double p=-1.; while (p<0 | p>1){ p = genAleaNormal.nextDouble() * Math.sqrt(succesProba * (1 -
			 * succesProba) /amount) + succesProba; } amountWithSuccess = (long) Math.round(p* amount);
			 */
			double amountWithSuccess = -1;
			while (amountWithSuccess < 0 | amountWithSuccess > amount) {
				amountWithSuccess = Math.round(NormalGen.nextDouble(pilot.getRandomStream(), 0., 1.)
						* Math.sqrt(succesProba * (1 - succesProba) * amount) + succesProba * amount);
			}
			return (long) amountWithSuccess;

		} else {
			UniformGen aleaGen = new UniformGen(pilot.getRandomStream(), 0., 1.);
			long amountWithSuccess = 0L;
			for (long i = 0; i < amount; i++) {
				if (aleaGen.nextDouble() < succesProba) {
					amountWithSuccess++;
				}
			}
			return amountWithSuccess;
		}
	}


	public static long getSuccessNumber(Pilot pilot, long amount, double succesProba) {
		return getSuccessNumber(pilot, amount, succesProba, 50);
	}

}
