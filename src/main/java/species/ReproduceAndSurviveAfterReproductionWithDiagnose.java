package species;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import miscellaneous.Duo;
import miscellaneous.Miscellaneous;
import miscellaneous.Trio;
import species.DiadromousFish.Gender;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.univariate.BrentOptimizer;
import org.apache.commons.math3.optim.univariate.SearchInterval;
import org.apache.commons.math3.optim.univariate.UnivariateObjectiveFunction;
import org.apache.commons.math3.optim.univariate.UnivariatePointValuePair;
import org.openide.util.lookup.ServiceProvider;

import umontreal.iro.lecuyer.probdist.NormalDist;
import umontreal.iro.lecuyer.randvar.NormalGen;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import environment.RiverBasin;
import environment.Time;
import environment.Time.Season;
import fr.cemagref.simaqualife.kernel.processes.AquaNismsGroupProcess;
import fr.cemagref.simaqualife.kernel.util.TransientParameters.InitTransientParameters;
import fr.cemagref.simaqualife.pilot.Pilot;

@ServiceProvider(service = AquaNismsGroupProcess.class)

public class ReproduceAndSurviveAfterReproductionWithDiagnose extends AquaNismsGroupProcess<DiadromousFish, DiadromousFishGroup>{

	// for the calibration of the model we use S_etoileGir = 190000; surfGir = 80351;
	//	alphaGirRougierEtAl = 6400000; betaGirRougierEtAl = 172000; dGirRougierEtAl = 19.2; tempGirMeanSpringSum = 20.24

	private Season reproductionSeason = Season.SPRING;
	private double tempMinRep = 14.;
	private double tempMaxRep = 26.; 
	private double tempOptRep = 20.;
	private double eta = 2.4; // parameter linking surface of a basin and S_etoile
	private double ratioS95_S50 = 2.;	
	private double a=135000.; // Parameter of fecndity (number of eggs per individual)
	private double delta_t=0.33; // duration of the mortality considered in the reproduction process (ex.: from eggs to juvenile in estuary for alosa alosa = 0.33)
	private double survOptRep = 0.0017;
	private double lambda = 0.00041;
	private double initialLength = 2.;
	private double sigmaRecruitment = 0.3;
	private double survivalRateAfterReproduction = 0.1;
	private double maxNumberOfSuperIndividualPerReproduction = 50.;
	private boolean withDiagnose = true;


	private transient NormalGen genNormal;
	private transient MortalityFunction mortalityFunction;
	/**
	 *  relationship between
	 *  	recruitment in number of juvenile on spawning grounds
	 *     stock in number of FEMALES
	 * @unit
	 */
	private transient StockRecruitmentRelationship stockRecruitmentRelationship;
	// private transient UniformGen genUniform;

	public static void main(String[] args) { System.out.println((new
			XStream(new DomDriver())) .toXML(new ReproduceAndSurviveAfterReproductionWithDiagnose())); }

	@Override
	@InitTransientParameters
	public void initTransientParameters(Pilot pilot) {
		super.initTransientParameters(pilot);
		genNormal = new NormalGen( pilot.getRandomStream(),
				new NormalDist(0., 1.));
		mortalityFunction=new MortalityFunction();
		stockRecruitmentRelationship=  new StockRecruitmentRelationship();
	}

	@Override
	public void doProcess(DiadromousFishGroup group) {

		if (Time.getSeason(group.getPilot()) == reproductionSeason){
			List<DiadromousFish> deadFish = new ArrayList<DiadromousFish>();

			for(RiverBasin riverBasin : group.getEnvironment().getRiverBasins()){
				double b, c, alpha, beta, amountPerSuperIndividual ,  S95, S50 ;
				double numberOfFemaleGenitors = 0.;
				double numberOfAutochtones = 0.;
				double numberOfFemaleSpawnerForFirstTime = 0.;
				double femaleSpawnersForFirstTimeAgesSum = 0.;
				long survivalAmount;
				double muRecruitment = 0.;
				//double weightOfGenitors = 0.;

				// origins of spawner during this reproduction			
				Map<String, Long> spawnerOriginsDuringReproduction = new HashMap<String, Long>(group.getEnvironment().getRiverBasinNames().length); 
				for (String basinName : group.getEnvironment().getRiverBasinNames()){
					spawnerOriginsDuringReproduction.put(basinName,  0L);
				}

				List<DiadromousFish> fishInBasin = riverBasin.getFishs(group);
				if (fishInBasin != null){

					// --------------------------------------------------------------------------------------------------
					// definition of the stock recruiment relationship
					// --------------------------------------------------------------------------------------------------
					
					// effective temperature for reproduction (priority to the ANG value) 
					double tempEffectRep;
					if (Double.isNaN(group.getTempMinRep())){
						tempEffectRep = Miscellaneous.temperatureEffect(riverBasin.getCurrentTemperature(group.getPilot()), tempMinRep, tempOptRep, tempMaxRep);
					}
					else {
						tempEffectRep = Miscellaneous.temperatureEffect(riverBasin.getCurrentTemperature(group.getPilot()), group.getTempMinRep(), tempOptRep, tempMaxRep);
					}

					// Compute  the prelimenary parameters b and c for the stock-recruitment relationship 
					b = (tempEffectRep == 0.) ? 0. : - Math.log(survOptRep * tempEffectRep) / delta_t;
					c = lambda/riverBasin.getAccessibleSurface();

					// Compute  alpha and beta parameters of the  the stock-recruitment relationship 
					alpha = (b * Math.exp(- b * delta_t))/(c * (1 - Math.exp(- b * delta_t)));
					beta = b / (a * c * (1 - Math.exp(- b * delta_t)));
					//System.out.println(a+ ", " +b + ", " + c + ", " + delta_t + "= "+ alpha);

					// keep the last value of alpha (productive capacities)
					riverBasin.getLastProdCapacities().push(alpha);

					// Compute the amount per superIndividual
					amountPerSuperIndividual = alpha / maxNumberOfSuperIndividualPerReproduction;

					// Compute the Allee effect parameters  S95 and S50
					S95 = eta * riverBasin.getAccessibleSurface(); // corresponds to S* in the rougier publication
					S50 = S95 / ratioS95_S50;

					// initilisation of the stock recruitment relationship
					stockRecruitmentRelationship.init(alpha, beta, S50, S95);
						
					// --------------------------------------------------------------------------------------------------
					// calulation of the spawner number
					// --------------------------------------------------------------------------------------------------

					// age of autochnonous spawnser
					Map<Integer, Long> ageOfNativeSpawners = new TreeMap<Integer, Long>(); 

					// compute the number of spawners and keep the origines of the spawners
					for (DiadromousFish fish : fishInBasin){
						
						if (fish.getGender() == Gender.FEMALE  &  fish.isMature()){
							
							//System.out.println(fish.getAge() + " -> "+ fish.getLength() + " ("+fish.getStage()+")");
							if (fish.getNumberOfReproduction() < 1) {
								numberOfFemaleSpawnerForFirstTime++;
								femaleSpawnersForFirstTimeAgesSum += fish.getAge();
							}
							numberOfFemaleGenitors += fish.getAmount() ;

							// spawner per origine
							String basinName = fish.getBirthBasin().getName();
							spawnerOriginsDuringReproduction.put(basinName, spawnerOriginsDuringReproduction.get(basinName) + fish.getAmount() );

							// number of autochtonous fish per age
							if (riverBasin == fish.getBirthBasin()){ 
								numberOfAutochtones += fish.getAmount();
								Integer age = (int) Math.floor(fish.getAge()); //ASK floor() or ceil()
								if (ageOfNativeSpawners.containsKey(age))
									ageOfNativeSpawners.put(age, ageOfNativeSpawners.get(age)+fish.getAmount());
								else
									ageOfNativeSpawners.put(age, fish.getAmount());
							}

							// increment number of reproduction (for possible iteroparty)
							fish.incNumberOfReproduction();	

							// survival after reproduction (semelparity or iteroparity) of SI (change the amount of the SI)
							survivalAmount = Miscellaneous.binomialForSuperIndividual(group.getPilot(), fish.getAmount(), survivalRateAfterReproduction);
							if (survivalAmount > 0) 
								fish.setAmount(survivalAmount);
							else
								deadFish.add(fish);
						}
					}

					// keep the spawner number
					riverBasin.setLastSpawnerNumber(numberOfFemaleGenitors);

					// --------------------------------------------------------------------------------------------------
					// Diagnose  of the population dynamics in the basin
					// --------------------------------------------------------------------------------------------------
					if (withDiagnose==true) {
						// calculate and keep the features of the stock recruitment relationships
						riverBasin.setMortalityCrash(stockRecruitmentRelationship.getSigmaZcrash());

						// initialise the mortality function for the autochnous spawners
						// use to approximate the mortality of all the spawners to give a proxy of the Allee trap 
						if (numberOfFemaleGenitors > 0.) {
							List<Trio<Integer, Long, Long>> mortalityData= new ArrayList<Trio<Integer, Long, Long>>();
							// first age
							// second effective of native spwaner
							// third  effective of corresponding recruitment
							for (Integer age : ageOfNativeSpawners.keySet()){
								if (riverBasin.getLastRecruitments().getItemFromLast(age) != null){
									mortalityData.add(new Trio<Integer, Long, Long>(age, 
											ageOfNativeSpawners.get(age), 
											riverBasin.getLastRecruitments().getItemFromLast(age)));
								} 
								else{
									mortalityData.add(new Trio<Integer, Long, Long>(age, 0L, 0L));
								}
							}
							mortalityFunction.init(mortalityData);
							riverBasin.setNativeSpawnerMortality(mortalityFunction.getSigmaZ());
						}
						else {
							riverBasin.setNativeSpawnerMortality(Double.NaN);
						}

						riverBasin.setStockTrap(stockRecruitmentRelationship.getStockTrap(riverBasin.getNativeSpawnerMortality())); 


						System.out.println(riverBasin.getName().toUpperCase());
						//System.out.println("alpha="+alpha+ "\tbeta="+beta+"\tS50="+S50+ "\tS95="+S95);
						System.out.println("\tScrash="+stockRecruitmentRelationship.getStockAtZcrash()+
								"\tZcrash="+ stockRecruitmentRelationship.getSigmaZcrash() + 
								"\tZ="+ riverBasin.getNativeSpawnerMortality());
						System.out.println("\tStrap="+stockRecruitmentRelationship.getStockTrap(riverBasin.getNativeSpawnerMortality())+
								"\tStotal="+numberOfFemaleGenitors+"\tSautochthonous="+
								spawnerOriginsDuringReproduction.get(riverBasin.getName()));


						String message;
						if (Double.isNaN(riverBasin.getNativeSpawnerMortality()))
							message="noSense";
						else {
							double stockTrap=stockRecruitmentRelationship.getStockTrap(riverBasin.getNativeSpawnerMortality());
							if (riverBasin.getNativeSpawnerMortality()>stockRecruitmentRelationship.getSigmaZcrash())
								message="overZcrash";
							else {
								if (numberOfFemaleGenitors < stockTrap)
									message = "inTrapWithStrayers";
								else {
									if (spawnerOriginsDuringReproduction.get(riverBasin.getName()) < stockTrap)
										message = "inTrapWithOnlyNatives";
									else
										message = "sustain";
								}
							}
						}
						System.out.println("\t"+message);
					}

					// --------------------------------------------------------------------------------------------------
					// Reproduction process (number of recruits)
					// --------------------------------------------------------------------------------------------------
					
					if (numberOfFemaleGenitors > 0.) {
					
						//BH Stock-Recruitment relationship with logistic depensation
						double meanNumberOfRecruit = stockRecruitmentRelationship.getRecruitment(numberOfFemaleGenitors);

						//  lognormal random draw
						muRecruitment = Math.log(meanNumberOfRecruit) - (Math.pow(sigmaRecruitment,2))/2;
						long numberOfRecruit = Math.round(Math.exp(genNormal.nextDouble()*sigmaRecruitment + muRecruitment));

						//System.out.println(group.getPilot().getCurrentTime()+"  "+Time.getSeason(group.getPilot())+"  "+ riverBasin.getName()+": " + numberOfGenitors + "  spwaners \tgive "+ numberOfRecruit + " recruits");
						
						// keep last % of  autochtone
						riverBasin.getLastPercentagesOfAutochtones().push(numberOfAutochtones * 100 / numberOfFemaleGenitors);

						// keep the number of spawners for the firt time in the basin
						if (numberOfFemaleSpawnerForFirstTime>0){
							riverBasin.getSpawnersForFirstTimeMeanAges().push(femaleSpawnersForFirstTimeAgesSum/numberOfFemaleSpawnerForFirstTime);
						}else{
							riverBasin.getSpawnersForFirstTimeMeanAges().push(0.);
						}

						//System.out.println("nb spawners in basin " + riverBasin.getName() + " : " + numberOfGenitors);
						//System.out.println("nb recruit in basin " + riverBasin.getName() + " : " + numberOfRecruit);

						// Creation of new superFish
						if (numberOfRecruit > 0){

							// features of the super individuals
							int numberOfsuperIndividual = Math.max(1, 
									(int) Math.round(numberOfRecruit / amountPerSuperIndividual));
							long effectiveAmount =  numberOfRecruit / numberOfsuperIndividual;

							for (int i = 0; i < numberOfsuperIndividual; i++){
								group.addAquaNism(new DiadromousFish(group.getPilot(), riverBasin, initialLength, effectiveAmount, Gender.FEMALE));
							}

							// stock the first year when recruitment is non nul
							if (riverBasin.getYearOfFirstNonNulRep() == 0){
								riverBasin.setYearOfFirstNonNulRep(Time.getYear(group.getPilot()));
							}

							// keep the last recruitments in the stack
							riverBasin.getLastRecruitmentExpectations().push(Math.round(meanNumberOfRecruit));
							riverBasin.getLastRecruitments().push(numberOfsuperIndividual * effectiveAmount); 
							riverBasin.getLastRecsOverProdCaps().push(((double) riverBasin.getLastRecruitments().getLastItem())/riverBasin.getLastProdCapacities().getLastItem());

							// keep the no null recruitment
							if (numberOfAutochtones > 0){
								riverBasin.getNumberOfNonNulRecruitmentForFinalProbOfPres().push(1.);
								riverBasin.getNumberOfNonNulRecruitmentDuringLastYears().push(1.);
							}else{
								riverBasin.getNumberOfNonNulRecruitmentForFinalProbOfPres().push(0.);
								riverBasin.getNumberOfNonNulRecruitmentDuringLastYears().push(0.);
							}

						}
						else {
							// stock the last year of null recruitment
							riverBasin.setYearOfLastNulRep(Time.getYear(group.getPilot()));							
							riverBasin.getLastRecruitmentExpectations().push((long) 0);
							riverBasin.getLastRecruitments().push((long) 0);
							riverBasin.getLastRecsOverProdCaps().push(0.);
							riverBasin.getNumberOfNonNulRecruitmentDuringLastYears().push(0.);
							riverBasin.getNumberOfNonNulRecruitmentForFinalProbOfPres().push(0.);
						}
					}
					else {
						// stock information when no spawners reproduce
						riverBasin.setYearOfLastNulRep(Time.getYear(group.getPilot()));
						riverBasin.getLastRecruitmentExpectations().push((long) 0);
						riverBasin.getLastRecruitments().push((long) 0);
						riverBasin.getLastRecsOverProdCaps().push(0.);
						riverBasin.getLastPercentagesOfAutochtones().push(0.);
						riverBasin.getNumberOfNonNulRecruitmentDuringLastYears().push(0.);
						riverBasin.getNumberOfNonNulRecruitmentForFinalProbOfPres().push(0.);
					}
					// --------------------------------------------------------------------------------------------------
					// Remove deadfish
					// --------------------------------------------------------------------------------------------------
					for (DiadromousFish fish : deadFish){
						group.removeAquaNism(fish);
					}
					deadFish.clear();
				}
				else {
					riverBasin.setYearOfLastNulRep(Time.getYear(group.getPilot()));
				}

				// System.out.println("("+numberOfGenitors+")");
				//System.out.println("  BEFORE " +riverBasin.getSpawnerOrigins().keySet());
				riverBasin.getSpawnerOrigins().push(spawnerOriginsDuringReproduction);
				//System.out.println("  AFTER " +riverBasin.getSpawnerOrigins().keySet());
			}
			// --------------------------------------------------------------------------------------------------
			// update the observers
			// --------------------------------------------------------------------------------------------------
			for (RiverBasin riverBasin : group.getEnvironment().getRiverBasins()){
				riverBasin.getCobservable().fireChanges(riverBasin, pilot.getCurrentTime());
			}                                                
		}
	}

	/**
	 * Berverton and Holt stock-recruitment relationship with an Allee effect simulated with a logistic function
	 */
	class StockRecruitmentRelationship implements UnivariateFunction{

		/**
		 *  alpha of the stock-recruitment relationship
		 *   R = beta Seff / (beta + Seff) with Seff the stock that effectivly participates to the reproduction
		 *  
		 * @unit # of individuals
		 */
		double alpha;

		/**
		 *	 *  beta of the stock-recruitment relationship
		 *   R = alpha * Seff / (beta + Seff) with Seff the stock that effectivly participates to the reproduction
		 * @unit
		 */
		double beta;

		/**
		 * the value of the stock for which 50% partipate to the reproduction
		 * @unit  # of individuals 
		 */
		double S50;

		/**
		 * the value of the stock for which 95% partipate to the reproduction
		 * @unit  # of individuals 
		 */
		double S95;

		/**
		 *  to simplify the calculation
		 * @unit
		 */
		transient double log19;

		/**
		 * the value of the stock for which 50% partipate to the reproduction
		 * @unit  # of individuals 
		 */
		double sigmaZ; // 

		public void init(double alpha, double beta, double S50, double S95) {
			this.alpha = alpha;
			this.beta = beta;
			this.S50 = S50;
			this.S95 = S95;

			log19 = Math.log(19) ;
		}


		public double getEffectiveStock (double stock) {
			return stock  / (1 + Math.exp(- log19 * (stock - S50) / (S95 - S50)));
		}


		public double getRecruitment(double stock){
			//BH Stock-Recruitment relationship with logistic depensation
			double meanNumberOfRecruit = 0.;
			double effectiveStock  = getEffectiveStock(stock);
			if (stock >0)
				meanNumberOfRecruit = Math.round(alpha * effectiveStock) /(beta + effectiveStock );
			return meanNumberOfRecruit;
		}


		/**
		 * the stock that corresponds to the intersection between SR relationship and tahe tangent that pass through the origin 
		 * @return the stock at 
		 */
		public double getStockAtZcrash(){
			if (beta !=0)
				return(S50 + (S95 - S50) * Math.log(beta * log19 / (S95-S50)) / log19);
			else
				return Double.NaN;
		}


		/**
		 *  the crash mortality
		 *  (corresponds the slope of the tangent that pass through the origin)
		 * @return the crash mortality  ( year-1)
		 */
		public double getSigmaZcrash(){
			double stockAtZcrash= getStockAtZcrash();
			if (!Double.isNaN(stockAtZcrash))
				return -Math.log(stockAtZcrash / getRecruitment(stockAtZcrash));
			else
				return Double.NaN;
		}


		/**
		 * the objective function uses to calculate the depensation trap (Allee trap)
		 */
		@Override
		public double value(double stock) {
			double res=getRecruitment(stock) - stock * Math.exp(sigmaZ);
			return res*res;
		}


		/**
		 * calculate the  stock correspondinf to the depensation trap
		 * corresponds to intersection between mortality rate and the stock-recruitment relationship  
		 * @param sigmaZ the total mortality coefficient 
		 * @return 
		 */
		private double getStockTrap(double sigmaZ){
			if (!Double.isNaN(sigmaZ)){
				this.sigmaZ=sigmaZ;
				BrentOptimizer optimizer = new BrentOptimizer(1e-6, 1e-12);
				UnivariatePointValuePair solution =
						optimizer.optimize(new UnivariateObjectiveFunction(this),
								new MaxEval(100),
								GoalType.MINIMIZE,
								new SearchInterval(0, getStockAtZcrash()));
				this.sigmaZ = Double.NaN; //WHY
				return solution.getPoint();
			}
			else
				return Double.NaN;
		}
	}

	/**
	 * mortatiity function for stock with dirrenet ages
	 */
	class MortalityFunction implements UnivariateFunction {

		// first:       age
		// second: effective of native spwaner
		// third:     effective of corresponding recruitment
		List<Trio<Integer, Long, Long>> data; //WHY age as integer

		public void init(List<Trio<Integer, Long, Long>> data){
			this.data = data;
		}

		private double getMeanAge(){
			double res=0.;
			double effTotal=0.;
			for(Trio<Integer, Long, Long> trio : data){
				res += ((double) trio.getFirst())* ((double)trio.getSecond());
				effTotal += trio.getSecond();
			}
			return res/effTotal;
		}


		@Override
		public double value(double Z) {
			double res=0.;
			for(Trio<Integer, Long, Long> trio : data){
				res += (((double) trio.getSecond())/((double) trio.getThird())) * Math.exp(Z * (double) trio.getFirst());
			}
			return (res-1) * (res-1); //WHY -1
		}


		/**
		 * calculate by optimsation of the total mortality coefficient over the livespan
		 * @return
		 */
		private double getSigmaZ2(){
			double Z = Double.NaN;

			if (!data.isEmpty()){
				BrentOptimizer optimizer = new BrentOptimizer(1e-6, 1e-12);
				UnivariatePointValuePair solution =
						optimizer.optimize(new UnivariateObjectiveFunction(this),
								new MaxEval(100),
								GoalType.MINIMIZE,
								new SearchInterval(0, 10));
				Z= solution.getPoint();
			}
			return Z * getMeanAge();
		}


		/**
		 * simple approximation of total mortality coefficient over the lifespan
		 * @return
		 */
		public double getSigmaZ(){
			double sum=0;
			for(Trio<Integer, Long, Long> trio : data){
				sum += ((double)trio.getSecond()) / ((double)trio.getThird());
			}
			return (- Math.log(sum));
		}
	}
}



