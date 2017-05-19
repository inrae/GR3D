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


	private transient NormalGen genNormal;
	private transient MortalityFunction mortalityFunction;
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
				double b, c, alpha, beta, amountPerSuperIndividual , Setoile, S95, S50 ;
				double numberOfGenitors = 0.;
				double numberOfAutochtones = 0.;
				double numberOfSpawnerForFirstTime = 0.;
				double spawnersForFirstTimeAgesSum = 0.;
				long survivalAmount;
				double muRecruitment = 0.;
				//double weightOfGenitors = 0.;

				// origins of spawner during this reproduction			
				Map<String, Long> spawnerOriginsDuringReproduction = new HashMap<String, Long>(group.getEnvironment().getRiverBasinNames().length); 
				for (String basinName : group.getEnvironment().getRiverBasinNames()){
					spawnerOriginsDuringReproduction.put(basinName,  (long) 0);
				}

				List<DiadromousFish> fishInBasin = riverBasin.getFishs(group);
				if (fishInBasin != null){
					//Calcul of b and c in stock-recruitment relationships 

					double tempEffectRep;
					if (group.getTempMinRep() == Double.NaN){
						tempEffectRep = Miscellaneous.temperatureEffect(riverBasin.getCurrentTemperature(group.getPilot()), tempMinRep, tempOptRep, tempMaxRep);
					}
					else {
						tempEffectRep = Miscellaneous.temperatureEffect(riverBasin.getCurrentTemperature(group.getPilot()), group.getTempMinRep(), tempOptRep, tempMaxRep);
					}

					// Calcul of alpha and beta of the basin
					c = lambda/riverBasin.getAccessibleSurface();
					if (tempEffectRep == 0.){
						b=0;
						alpha=0.;
						beta=0.;
					}
					else {
						b = - Math.log(survOptRep * tempEffectRep) / delta_t;
						alpha = (b * Math.exp(- b * delta_t))/(c * (1 - Math.exp(- b * delta_t)));
						beta = b / (a * c * (1 - Math.exp(- b * delta_t)));
					}

					//System.out.println(a+ ", " +b + ", " + c + ", " + delta_t + "= "+ alpha);
					riverBasin.getLastProdCapacities().push(alpha);

					// Calcul of the amount per superIndividual
					amountPerSuperIndividual = alpha / maxNumberOfSuperIndividualPerReproduction;

					// Calcul of Setoile, S95 and S50
					Setoile = eta * riverBasin.getAccessibleSurface();
					S95 = Setoile;
					S50 = S95 / ratioS95_S50;

					// initilisation de la relation stock recruitment
					stockRecruitmentRelationship.init(alpha, beta, S50, S95);

					// calcul de Zcrash
					//Double Zcrash = Math.log(alpha*(d-1)/(d*beta*Math.pow(d-1, 1/d)));
					// age of autochnonous spawnser
					Map<Integer, Long> ageOfNativeSpawners = new TreeMap<Integer, Long>(); 

					// compute the number of spawners and keep the origines of the spawners
					for( DiadromousFish fish : fishInBasin){
						if( fish.isMature()){
							if (fish.getNumberOfReproduction() < 1) {
								numberOfSpawnerForFirstTime++;
								spawnersForFirstTimeAgesSum += fish.getAge();
							}
							numberOfGenitors += fish.getAmount() ;

							// spawner per origine
							String basinName = fish.getBirthBasin().getName();
							spawnerOriginsDuringReproduction.put(basinName, spawnerOriginsDuringReproduction.get(basinName) + fish.getAmount() );

							// number of autochtone and age of autochnone
							if (riverBasin == fish.getBirthBasin()){ 
								numberOfAutochtones += fish.getAmount();
								Integer age = (int) Math.floor(fish.getAge());
								if (ageOfNativeSpawners.containsKey(age))
									ageOfNativeSpawners.put(age, ageOfNativeSpawners.get(age)+fish.getAmount());
								else
									ageOfNativeSpawners.put(age, fish.getAmount());
							}

							//System.out.println("l'âge du poisson est :" + fish.getAge() + " et la saison est :" + Time.getSeason());
							// Survive After Reproduction
							fish.incNumberOfReproduction();	

							// survival after reproduction (semelparity or iteroparity) of SI (change the amount of the SI)
							survivalAmount = Miscellaneous.binomialForSuperIndividual(group.getPilot(), fish.getAmount(), survivalRateAfterReproduction);
							if (survivalAmount > 0) 
								fish.setAmount(survivalAmount);
							else
								deadFish.add(fish);
						}
					}


					// prepare les données pour le calcul de la mortalité associée aux géniteurs autochtones
					if (numberOfGenitors > 0.) {
						List<Trio<Integer, Long, Long>> mortalityData= new ArrayList<Trio<Integer, Long, Long>>();
						// first age
						// second effective of native spwaner
						// third  effective of corresponding recruitment
						for (Integer age : ageOfNativeSpawners.keySet()){
							if (riverBasin.getLastRecruitments().getItemFromLast(age) != null){
								mortalityData.add(new Trio<Integer, Long, Long>(age, 
										ageOfNativeSpawners.get(age), 
										riverBasin.getLastRecruitments().getItemFromLast(age)));
							} else{
								mortalityData.add(new Trio<Integer, Long, Long>(age, 0L, 0L));
							}
						}
						mortalityFunction.init(mortalityData);
						riverBasin.setNativeSpawnerMortality(mortalityFunction.getSigmaZ());
					}
					else{
						riverBasin.setNativeSpawnerMortality(Double.NaN);
					}

					
					riverBasin.setMortalityCrash(stockRecruitmentRelationship.getSigmaZcrash());
					riverBasin.setStockTrap(stockRecruitmentRelationship.getStockTrap(riverBasin.getNativeSpawnerMortality()));
					riverBasin.setLastSpawnerNumber(numberOfGenitors);
					
					// AFFICHAGE DES RESULTATS
					/*System.out.println(riverBasin.getName().toUpperCase());
					//System.out.println("alpha="+alpha+ "\tbeta="+beta+"\tS50="+S50+ "\tS95="+S95);
					System.out.println("\tScrash="+stockRecruitmentRelationship.getStockAtZcrash()+
							"\tZcrash="+ stockRecruitmentRelationship.getSigmaZcrash() + 
							"\tZ="+ riverBasin.getNativeSpawnerMortality());
					System.out.println("\tStrap="+stockRecruitmentRelationship.getStockTrap(riverBasin.getNativeSpawnerMortality())+
							"\tStotal="+numberOfGenitors+"\tSautochthonous="+
							spawnerOriginsDuringReproduction.get(riverBasin.getName()));
				
					
					String diagnose;
					if (Double.isNaN(riverBasin.getNativeSpawnerMortality()))
						diagnose="noSense";
					else {
						double stockTrap=stockRecruitmentRelationship.getStockTrap(riverBasin.getNativeSpawnerMortality());
						if (riverBasin.getNativeSpawnerMortality()>stockRecruitmentRelationship.getSigmaZcrash())
							diagnose="overZcrash";
						else {
							if (numberOfGenitors < stockTrap)
								diagnose = "inTrapWithStrayers";
							else {
								if (spawnerOriginsDuringReproduction.get(riverBasin.getName()) < stockTrap)
									diagnose = "inTrapWithOnlyNatives";
								else
									diagnose = "sustain";
							}
						}
					}
					System.out.println("\t"+diagnose);*/


					// Reproduction process (number of recruits)
					if (numberOfGenitors > 0.) {
						//BH Stock-Recruitment relationship with logistic depensation
						double meanNumberOfRecruit = stockRecruitmentRelationship.getRecruitment(numberOfGenitors);
						muRecruitment = Math.log(meanNumberOfRecruit) - (Math.pow(sigmaRecruitment,2))/2;

						long numberOfRecruit = Math.round(Math.exp(genNormal.nextDouble()*sigmaRecruitment + muRecruitment));

						riverBasin.getLastPercentagesOfAutochtones().push(numberOfAutochtones * 100 / numberOfGenitors);

						if (numberOfSpawnerForFirstTime>0){
							riverBasin.getSpawnersForFirstTimeMeanAges().push(spawnersForFirstTimeAgesSum/numberOfSpawnerForFirstTime);
						}else{
							riverBasin.getSpawnersForFirstTimeMeanAges().push(0.);
						}

						//System.out.println("nb spawners in basin " + riverBasin.getName() + " : " + numberOfGenitors);
						//System.out.println("nb recruit in basin " + riverBasin.getName() + " : " + numberOfRecruit);

						// Creation of new superFish
						if(numberOfRecruit > 0){
							// stock the first year when recruitment is non nul
							if(riverBasin.getYearOfFirstNonNulRep() == 0){
								riverBasin.setYearOfFirstNonNulRep(Time.getYear(group.getPilot()));
							}

							int numberOfsuperIndividual = Math.max(1, 
									(int) Math.round(numberOfRecruit / amountPerSuperIndividual));
							long effectiveAmount =  numberOfRecruit / numberOfsuperIndividual;

							// System.out.println(numberOfRecruit + " / " + amountPerSuperIndividual +" = " +numberOfsuperIndividual);
							//System.out.println(numberOfRecruit + " / " + numberOfsuperIndividual +" = " +effectiveAmount);
							for (int i=0; i<numberOfsuperIndividual; i++){
								group.addAquaNism(new DiadromousFish(group.getPilot(), riverBasin, initialLength, effectiveAmount));
							}
							riverBasin.getLastRecruitmentExpectations().push(Math.round(meanNumberOfRecruit));
							riverBasin.getLastRecruitments().push(numberOfsuperIndividual * effectiveAmount); // on remplit la pile qui permet de stocker un nombre fixé de derniers recrutement
							riverBasin.getLastRecsOverProdCaps().push(((double) riverBasin.getLastRecruitments().getLastItem())/riverBasin.getLastProdCapacities().getLastItem());

							if (numberOfAutochtones > 0){
								riverBasin.getNumberOfNonNulRecruitmentForFinalProbOfPres().push(1.0);
								riverBasin.getNumberOfNonNulRecruitmentDuringLastYears().push(1.0);
							}else{
								riverBasin.getNumberOfNonNulRecruitmentForFinalProbOfPres().push(0.0);
								riverBasin.getNumberOfNonNulRecruitmentDuringLastYears().push(0.0);
							}

						}
						else {
							// stock the last year of null recruitment
							riverBasin.setYearOfLastNulRep(Time.getYear(group.getPilot()));							
							riverBasin.getLastRecruitmentExpectations().push((long) 0);
							riverBasin.getLastRecruitments().push((long) 0);
							riverBasin.getLastRecsOverProdCaps().push(0.);
							riverBasin.getNumberOfNonNulRecruitmentDuringLastYears().push(0.);
							riverBasin.getNumberOfNonNulRecruitmentForFinalProbOfPres().push(0.0);
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
						riverBasin.getNumberOfNonNulRecruitmentForFinalProbOfPres().push(0.0);
					}

					// Remove deadfish
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
			// on met à jour les observeurs
			for (RiverBasin riverBasin : group.getEnvironment().getRiverBasins()){
				riverBasin.getCobservable().fireChanges(riverBasin, pilot.getCurrentTime());
			}                                                
		}
	}

	class StockRecruitmentRelationship implements UnivariateFunction{

		double alpha;
		double beta;
		double S50;
		double S95;
		double sigmaZ; // mortality


		public void init(double alpha, double beta, double S50, double S95) {
			this.alpha = alpha;
			this.beta = beta;
			this.S50 = S50;
			this.S95 = S95;
		}

		public double getRecruitment(double stock){
			//BH Stock-Recruitment relationship with logistic depensation
			double meanNumberOfRecruit = 0.;
			if (stock >0)
				meanNumberOfRecruit= Math.round((alpha * stock * (1 / (1 + Math.exp(- Math.log(19)*((stock - S50) / (S95 - S50)))))) /
						(beta + stock * (1 / (1 + Math.exp(- Math.log(19)*((stock - S50) / (S95 - S50)))))));
			return meanNumberOfRecruit;
		}

		public double getStockAtZcrash(){
			if (beta !=0)
				return(S50 + (S95 - S50) * Math.log(beta * Math.log(19) / (S95-S50)) / Math.log(19));
			else
				return Double.NaN;
		}

		public double getSigmaZcrash(){
			double stockAtZcrash= getStockAtZcrash();
			if (!Double.isNaN(stockAtZcrash))
				return -Math.log(stockAtZcrash / getRecruitment(stockAtZcrash));
			else
				return Double.NaN;
		}


		@Override
		public double value(double S) {
			double res=getRecruitment(S)-S*Math.exp(sigmaZ);
			return res*res;
		}

		private double getPoint(double sigmaZ){
			if (!Double.isNaN(sigmaZ)){
				this.sigmaZ=sigmaZ;
				BrentOptimizer optimizer = new BrentOptimizer(1e-6, 1e-12);
				UnivariatePointValuePair solution =
						optimizer.optimize(new UnivariateObjectiveFunction(this),
								new MaxEval(100),
								GoalType.MINIMIZE,
								new SearchInterval(0, getStockAtZcrash()));
				this.sigmaZ = Double.NaN;
				return solution.getPoint();
			}
			else
				return Double.NaN;
		}

		public double getStockTrap(double sigmaZ) {
			return getPoint(sigmaZ);
		}

	}

	class MortalityFunction implements UnivariateFunction {

		// first age
		// second effective of native spwaner
		// third  effective of corresponding recruitment
		List<Trio<Integer, Long, Long>> data;

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
			return Math.pow(res-1., 2.);
		}

		private double getPoint(){
			if (data.isEmpty()){
				return Double.NaN;
			}
			else {
				BrentOptimizer optimizer = new BrentOptimizer(1e-6, 1e-12);
				UnivariatePointValuePair solution =
						optimizer.optimize(new UnivariateObjectiveFunction(this),
								new MaxEval(100),
								GoalType.MINIMIZE,
								new SearchInterval(0, 10));
				return solution.getPoint();
			}
		}

		// return sigmaZ = Z*meanAge
		public double getSigmaZ2(){
			/*double Z = getPoint();
			double alpha; // % of maturation for a given age
			double sum=0;
			double sum1=0;
			for(Trio<Integer, Long, Long> trio : data){
				alpha = ((double)trio.getSecond()) / (((double)trio.getThird())*Math.exp(-((double) trio.getFirst())*Z));
				sum += alpha * Math.exp(-((double) trio.getFirst())*Z);
				sum1 += ((double)trio.getSecond()) / ((double)trio.getThird());
			}
			
			System.out.println(getPoint()*getMeanAge()+" <> "+ Math.log(sum)+ " <> " + Math.log(sum1));*/
			return getPoint()*getMeanAge();
		}
		
		public double getSigmaZ(){
			double sum=0;
			for(Trio<Integer, Long, Long> trio : data){
				 sum += ((double)trio.getSecond()) / ((double)trio.getThird());
			}
			return (- Math.log(sum));
		}
	}
}



