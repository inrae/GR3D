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
package species;


import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;

import fr.cemagref.simaqualife.extensions.pilot.BatchRunner;
import fr.cemagref.simaqualife.pilot.Pilot;
import fr.inria.optimization.cmaes.CMAEvolutionStrategy;
import fr.inria.optimization.cmaes.fitness.IObjectiveFunction;
import miscellaneous.Duo;
import miscellaneous.ReflectUtils;

/**
 *
 */
public class Calibrate  {



	public Calibrate() {

	}



	public static void main(String[] args) {
		GR3DObjeciveFunction fitfun = new GR3DObjeciveFunction(10.,10.);
		
		// new a CMA-ES and set some initial values
		CMAEvolutionStrategy cma = new CMAEvolutionStrategy();

		cma.setDimension(fitfun.getParameterRanges().size());

		//cma. parameters.setPopulationSize(30);
		cma.setInitialX(5.);
		cma.setInitialStandardDeviation(2.5);
		
		cma.options.stopTolFun=1e-6;    // function value range within iteration and of past values

		// from CMAEvolutionStrategy.properties, to avoid to load the file
		cma.options.stopTolFunHist = 1e-13 ; // function value range of 10+30*N/lambda past values
		cma.options.stopTolX = 0.0 ;                 // absolute x-changes
		cma.options.stopTolXFactor = 1e-11;           // relative to initial stddev
		cma.options.stopTolUpXFactor = 1000;          // relative to initial stddev	
		
		
		// initialize cma and get fitness array to fill in later
		double[] fitness = cma.init();  // new double[cma.parameters.getPopulationSize()];
		
		double[][] pop= cma.samplePopulation();
		// iteration loop
		while(cma.stopConditions.getNumber() == 0) {

			// --- core iteration step ---
			pop = cma.samplePopulation(); // get a new population of solutions

			for(int i = 0; i < 	pop.length; ++i) {    // for each candidate solution i
				// a simple way to handle constraints that define a convex feasible domain  
				// (like box constraints, i.e. variable boundaries) via "blind re-sampling" 
				// assumes that the feasible domain is convex, the optimum is  
				while (!fitfun.isFeasible(pop[i]))     //   not located on (or very close to) the domain boundary,  
					pop[i] = cma.resampleSingle(i);    //   initialX is feasible and initialStandardDeviations are  
				//   sufficiently small to prevent quasi-infinite looping here
				// compute fitness/objective value	
				fitness[i] = fitfun.valueOf(pop[i]); // fitfun.valueOf() is to be minimized
			}
			cma.updateDistribution(fitness);         // pass fitness array to update search distribution
			// --- end core iteration step ---

			// output to files and console 
			cma.writeToDefaultFiles(0);
			int outmod = 100;
			if (cma.getCountIter() % (100*outmod) == 1)
				cma.printlnAnnotation(); // might write file as well
			if (cma.getCountIter() % outmod == 1)
				cma.println(); 
			//
			// evaluate mean value as it is the best estimator for the optimum
			cma.setFitnessOfMeanX(fitfun.valueOf(cma.getMeanX())); // updates the best ever solution 
		}

		// final output
		//cma.writeToDefaultFiles(1);
		cma.println();
		cma.println("Terminated due to");
		for (String s : cma.stopConditions.getMessages())
			cma.println("  " + s);

		cma.println("Best function value " + cma.getBestFunctionValue() + " at evaluation " + cma.getBestEvaluationNumber());

		cma.println("best par: "+ Arrays.toString(fitfun.x2par(cma.getBestX())));

		System.out.println("\n"+fitfun.valueOf(cma.getBestX()));
		System.out.println();
		for (int i=0; i < pop.length; i++) {
			System.out.println(Arrays.toString(fitfun.x2par((pop[i]))));
		}
	}
}


/**
 * objective function based on GR3D 
 */
class GR3DObjeciveFunction  implements   IObjectiveFunction {

	private double a_femaleLengthPenalty=100.;
	private double a_maleLengthPenalty=100.;
	private  Map<String, Duo<Double, Double>> parameterRanges;

	private transient Pilot pilot;

	public GR3DObjeciveFunction(double a_femaleLengthPenalty, double a_maleLengthPenalty) {
		loadSimulation();
		this.a_femaleLengthPenalty = a_femaleLengthPenalty;
		this.a_maleLengthPenalty = a_maleLengthPenalty;

		parameterRanges = new Hashtable<String, Duo<Double,Double>>();
		parameterRanges.put("tempMinRep", new Duo<Double, Double>(9., 12.));
		parameterRanges.put("KOptFemale", new Duo<Double, Double>(0.2, .5));
		parameterRanges.put("KOptMale", new Duo<Double, Double>(0.2, .5));
	}
	
	

	/**
	 * @return the parameterRanges
	 */
	public Map<String, Duo<Double, Double>> getParameterRanges() {
		return parameterRanges;
	}


	@Override
	public double valueOf(double[] x) {
		// x[0] tempMinRep
		// x[1] kOptFemale
		// x[2] kOptMale

		double[] par = x2par(x); // in natural unit
		try {
			pilot.load();

			ReflectUtils.setFieldValueFromPath(pilot, "aquaticWorld.aquaNismsGroupsList.0.processes.processesEachStep.6.tempMinRep", par[0]);
			//System.out.println("tempMinRep: " + (double)  ReflectUtils.getValueFromPath(pilot, "aquaticWorld.aquaNismsGroupsList.0.processes.processesEachStep.6.getTempMinRep"));

			ReflectUtils.setFieldValueFromPath(pilot, "aquaticWorld.aquaNismsGroupsList.0.processes.processesEachStep.3.kOptForFemale", par[1]);
			//System.out.println("KOptFemale: " + (double)  ReflectUtils.getValueFromPath(pilot, "aquaticWorld.aquaNismsGroupsList.0.processes.processesEachStep.3.getkOptForFemale"));

			ReflectUtils.setFieldValueFromPath(pilot, "aquaticWorld.aquaNismsGroupsList.0.processes.processesEachStep.3.kOptForMale",par[2]);
			//System.out.println("KOptMale: " + (double)  ReflectUtils.getValueFromPath(pilot, "aquaticWorld.aquaNismsGroupsList.0.processes.processesEachStep.3.getkOptForMale"));

		} catch (Exception   e1) {
			e1.printStackTrace();
		}

		pilot.run();

		double likelihood =0;
		double femaleLengthPenalty = 0.;
		double maleLengthPenalty =0.;
		try {

			likelihood = (double) 	ReflectUtils.getValueFromPath(pilot, "aquaticWorld.aquaNismsGroupsList.0.computeLikelihood");
			//System.out.println("likelihood: "+ likelihood);
			femaleLengthPenalty = (double) 	ReflectUtils.getValueFromPath(pilot, "aquaticWorld.aquaNismsGroupsList.0.computeFemaleSpawnerForFirstTimeSummaryStatistic");
			//System.out.println("femaleLengthPenalty: "+femaleLengthPenalty);
			maleLengthPenalty = (double) 	ReflectUtils.getValueFromPath(pilot, "aquaticWorld.aquaNismsGroupsList.0.computeMaleSpawnerForFirstTimeSummaryStatistic");
			//System.out.println("maleLengthPenalty: "+maleLengthPenalty);

			System.out.println("likelihood: "+ likelihood+ " femaleLengthPenalty: "+femaleLengthPenalty+ " maleLengthPenalty: "+maleLengthPenalty);
			
		} catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		double result = likelihood +a_femaleLengthPenalty * femaleLengthPenalty + a_maleLengthPenalty * maleLengthPenalty ;

		System.out.println(Arrays.toString(x2par(x)) + "->"+result);
		return  result;
	}

	@Override
	public boolean isFeasible(double[] x) {
		double[] par = x2par(x);
		
		boolean test = true;
		double a, b;
		a = parameterRanges.get("tempMinRep").getFirst();
		b = parameterRanges.get("tempMinRep").getSecond();
		if  (par[0] < a | par[0]>b)
				test = false;
		
		a = parameterRanges.get("KOptFemale").getFirst();
		b = parameterRanges.get("KOptFemale").getSecond();
		if  (par[1] < a | par[1]>b)
				test = false;
		
		a = parameterRanges.get("KOptMale").getFirst();
		b = parameterRanges.get("KOptMale").getSecond();
		if  (par[2] < a | par[2]>b)
				test = false;

		return test;
	}

	public void loadSimulation() {
		String[] args= {"-simDuration", "100", "-simBegin", "1", "-RNGStatusIndex", "1", 
				"-groups", "data/input/fishTryRealBV_calibration.xml", 
				"-env", "data/input/BNtryRealBasins.xml",
				"q", "true"
		};
		pilot = new Pilot();

		try {
			// iniatialize the simulation 
			pilot.init();
			// no display on the console ????
			pilot.setQuiet();

			new BatchRunner(pilot).parseArgs(args, true, true, false);
		} catch (IOException | IllegalArgumentException e) {

			e.printStackTrace();
		}
	}

	/**
	 * @param x paremters in the range [0,10]
	 * @return parameter in natural unit (based on parameters range)
	 */
	public double[] x2par(double[] x) {
		double[] naturalPar = new double[x.length]; // par in natural unit
		double a, b; 

		a = parameterRanges.get("tempMinRep").getFirst();
		b = parameterRanges.get("tempMinRep").getSecond();
		naturalPar[0] = a + (b-a) * x[0] /10.;

		a = parameterRanges.get("KOptFemale").getFirst();
		b = parameterRanges.get("KOptFemale").getSecond();
		naturalPar[1] = a + (b-a) * x[1] /10.;
		
		a = parameterRanges.get("KOptMale").getFirst();
		b = parameterRanges.get("KOptMale").getSecond();
		naturalPar[2] = a + (b-a) * x[2] /10.;
		
		return naturalPar;
	}


	public double[] par2x(double[] naturalPar) {
		double[] x = new double[naturalPar.length];
		double a, b;
	
		a = parameterRanges.get("tempMinRep").getFirst();
		b = parameterRanges.get("tempMinRep").getSecond();
		x[0] = 10.* (naturalPar[0] - a) / (b-a);
		
		a = parameterRanges.get("KOptFemale").getFirst();
		b = parameterRanges.get("KOptFemale").getSecond();
		x[1] = 10.* (naturalPar[1] - a) / (b-a);
		
		a = parameterRanges.get("KOptFemale").getFirst();
		b = parameterRanges.get("KOptFemale").getSecond();
		x[2] = 10.* (naturalPar[2] - a) / (b-a);
		
		return x;
	}
}



