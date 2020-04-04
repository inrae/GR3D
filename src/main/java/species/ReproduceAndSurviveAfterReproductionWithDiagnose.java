package species;


import java.io.BufferedWriter;
import java.io.IOException;
import java.io.ObjectInputStream.GetField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import miscellaneous.BinomialForSuperIndividualGen;
import miscellaneous.Duo;
import miscellaneous.Miscellaneous;
import miscellaneous.Trio;
import species.DiadromousFish.Gender;
import species.DiadromousFish.SpawnerOrigin;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.univariate.BrentOptimizer;
import org.apache.commons.math3.optim.univariate.SearchInterval;
import org.apache.commons.math3.optim.univariate.UnivariateObjectiveFunction;
import org.apache.commons.math3.optim.univariate.UnivariatePointValuePair;
import org.openide.util.lookup.ServiceProvider;

import umontreal.iro.lecuyer.probdist.NormalDist;
import umontreal.iro.lecuyer.randvar.NormalACRGen;
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
	private double tempMinRep = 14. ;
	private double tempMaxRep = 26. ; 
	private double tempOptRep = 20. ;
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
	private double proportionOfFemaleAtBirth =0.5;
	private boolean displayFluxesOnConsole = true;

	private transient NormalGen genNormal;

	/**
	 * the random numbers generator for binomial draws
	 * @unit --
	 */
	private transient BinomialForSuperIndividualGen aleaGen;

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

		aleaGen = new BinomialForSuperIndividualGen(pilot.getRandomStream());

		mortalityFunction=new MortalityFunction();
		stockRecruitmentRelationship=  new StockRecruitmentRelationship();
	}


	/**
	 * @return the tempMinRep
	 */
	public double getTempMinRep() {
		return tempMinRep;
	}

	@Override
	public void doProcess(DiadromousFishGroup group) {
		Time time = group.getEnvironment().getTime();
		if (time.getSeason(group.getPilot()) == reproductionSeason){
			//List<DiadromousFish> deadFish = new ArrayList<DiadromousFish>();
			// a River
			List<String> basinList = new ArrayList<String>() ;
			basinList.add("Garonne");
			basinList.add("Vire");
			basinList.add("Somme");
			basinList.add("Meuse");
			basinList. add("Rhine");

			if (displayFluxesOnConsole == true)
				System.out.print(group.getPilot().getCurrentTime() + " - ");

			for(RiverBasin riverBasin : group.getEnvironment().getRiverBasins()){

				// before the party !!!!
				double fluxBefore =riverBasin.getSpawnerNumber();

				double b, c, alpha, beta, amountPerSuperIndividual ,  S95, S50 ;
				double numberOfFemaleSpawners = 0.;
				double numberOfMaleSpawners = 0.;
				double numberOfAutochtones = 0.;

				double numberOfFemaleSpawnerForFirstTime = 0.;
				double femaleSpawnersForFirstTimeAgesSum = 0.;
				double femaleSpawnersForFirstTimeLengthsSum = 0.;
				double numberOfMaleSpawnerForFirstTime = 0.;
				double maleSpawnersForFirstTimeAgesSum = 0.;
				double maleSpawnersForFirstTimeLengthsSum = 0.;

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

					//Initiate the total fluxes for this basin 
					Map<SpawnerOrigin, Map<String, Double>> totalInputFluxes = new Hashtable<SpawnerOrigin, Map <String, Double>>(); //On cr�er la Map pour stocker les flux 
					totalInputFluxes.put(SpawnerOrigin.AUTOCHTONOUS, new Hashtable < String, Double>()); 
					totalInputFluxes.put(SpawnerOrigin.ALLOCHTONOUS, new Hashtable < String, Double>()); 
					for (SpawnerOrigin origin: totalInputFluxes.keySet()) {
						for (String nutrient : group.getNutrientRoutine().getNutrientsOfInterest()) {
							totalInputFluxes.get(origin).put(nutrient, 0.); // ON MET A JOUR NOTRE map 
						}
						totalInputFluxes.get(origin).put("biomass",0.); 
					}


					// --------------------------------------------------------------------------------------------------
					// definition of the stock recruiment relationship
					// --------------------------------------------------------------------------------------------------

					// effective temperature for reproduction (priority to the ANG value) 
					double tempEffectRep;
					//CHECK if currentTemp can be removed
					double currentTemp = riverBasin.getCurrentTemperature(group.getPilot());
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

					// compute the number of female spawners and keep the origine of the female spawners
					ListIterator<DiadromousFish> fishIterator = fishInBasin.listIterator();
					while (fishIterator.hasNext()) {
						DiadromousFish fish = fishIterator.next();

						//for (DiadromousFish fish : fishInBasin){

						if (fish.isMature()) {

							// number of spawners per gender
							if (fish.getGender() == Gender.FEMALE) {
								//System.out.println(fish.getAge() + " -> "+ fish.getLength() + " ("+fish.getStage()+")");
								numberOfFemaleSpawners += fish.getAmount() ; // on ajoute a chaque fois le fish.getAmount (CcumSum)		
								if (fish.getNumberOfReproduction() < 1) {
									numberOfFemaleSpawnerForFirstTime += fish.getAmount();
									femaleSpawnersForFirstTimeAgesSum += fish.getAge() * fish.getAmount();
									femaleSpawnersForFirstTimeLengthsSum += fish.getLength() * fish.getAmount();
								}
							}
							else if (fish.getGender() == Gender.MALE) {
								numberOfMaleSpawners += fish.getAmount() ; // on ajoute a chaque fois le fish.getAmount (CcumSum)
								if (fish.getNumberOfReproduction() < 1) {
									numberOfMaleSpawnerForFirstTime += fish.getAmount();
									maleSpawnersForFirstTimeAgesSum += fish.getAge() * fish.getAmount();
									maleSpawnersForFirstTimeLengthsSum += fish.getLength() * fish.getAmount();

								}
							}

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

							// origin of the spwaner
							SpawnerOrigin spawnerOrigin; 
							if (fish.getBirthBasin() == riverBasin) 
								spawnerOrigin = SpawnerOrigin.AUTOCHTONOUS; 
							else
								spawnerOrigin = SpawnerOrigin.ALLOCHTONOUS;

							// survival after reproduction (semelparity or iteroparity) of SI (change the amount of the SI)
							double biomass = 0.; 
							//survivalAmount = aleaGen.getSuccessNumber2(fish.getAmount(), survivalRateAfterReproduction); 
							survivalAmount = Miscellaneous.binomialForSuperIndividual(group.getPilot(), fish.getAmount(), survivalRateAfterReproduction); 
							// update the amount of fish or kill the fish if survival amount = 0		
							if (survivalAmount > 0) {// SUperindividu est encore vivant mais il perd des effectifs 

								// --------------------------------------------------------------------------------------- nutrient routine -----------------------------
								//Export for one fish survived after spawning (survivalAmount) : excretion + gametes
								Map <String, Double> aFluxAfterSurvival = group.getNutrientRoutine().computeNutrientsInputForSurvivalAfterSpawning(fish); 

								//Export for one fish that dies after spawning (fish.getAmount - survivalAmount): excretion + gametes + carcasse 
								Map<String, Double> aFluxForDeadFish = group.getNutrientRoutine().computeNutrientsInputForDeathAfterSpawning(fish); 

								for (String nutrient: aFluxAfterSurvival.keySet()) {
									//For all survival fish
									group.getNutrientRoutine().getNutrientImportFluxesCollection().
									put(time.getYear(group.getPilot()), nutrient, fish.getBirthBasin().getName(), riverBasin.getName(), aFluxAfterSurvival.get(nutrient) * survivalAmount);

									//For all dead fish
									group.getNutrientRoutine().getNutrientImportFluxesCollection().
									put(time.getYear(group.getPilot()), nutrient, fish.getBirthBasin().getName(), riverBasin.getName(), aFluxForDeadFish.get(nutrient) * (fish.getAmount() - survivalAmount));

								}
								for (String nutrient: aFluxAfterSurvival.keySet()) {
									//For all survival fish
									totalInputFluxes.get(spawnerOrigin).put(nutrient,totalInputFluxes.get(spawnerOrigin).get(nutrient) + aFluxAfterSurvival.get(nutrient) * survivalAmount); 

									//For all dead fish
									totalInputFluxes.get(spawnerOrigin).put(nutrient,totalInputFluxes.get(spawnerOrigin).get(nutrient) + aFluxForDeadFish.get(nutrient) * (fish.getAmount() - survivalAmount)); 
								}

								//compute biomass for dead fish 
								biomass = group.getNutrientRoutine().getWeight(fish) * (fish.getAmount() - survivalAmount); 
								totalInputFluxes.get(spawnerOrigin).put("biomass", totalInputFluxes.get(spawnerOrigin).get("biomass") + biomass);
								// --------------------------------------------------------------------------------------- nutrient routine -----------------------------

								//update the amount of individual in the super-individual 
								fish.setAmount(survivalAmount); 
							}
							else {
								//Le superindividu est mort !!! 

								// --------------------------------------------------------------------------------------- nutrient routine -----------------------------
								//Export for fished died after spawning (fish.getAmount): carcasses + excretion + gametes 
								Map<String, Double> aFluxForDeadFish = group.getNutrientRoutine().computeNutrientsInputForDeathAfterSpawning(fish); // 

								for (String nutrient: aFluxForDeadFish.keySet()) {
									group.getNutrientRoutine().getNutrientImportFluxesCollection().
									put(time.getYear(group.getPilot()), nutrient, fish.getBirthBasin().getName(), riverBasin.getName(), aFluxForDeadFish.get(nutrient) * (fish.getAmount() - survivalAmount));
								}
								biomass = group.getNutrientRoutine().getWeight(fish) * (fish.getAmount());
								totalInputFluxes.get(spawnerOrigin).put("biomass", totalInputFluxes.get(spawnerOrigin).get("biomass") + biomass);
								// --------------------------------------------------------------------------------------- nutrient routine -----------------------------

								// remove fish because fish is dead
								fishIterator.remove();
								//deadFish.add(fish);
							}
						}
					}

					// keep the  number of female spawner
					riverBasin.setLastFemaleSpawnerNumber(numberOfFemaleSpawners);

					// --------------------------------------------------------------------------------------------------
					// Diagnose  of the population dynamics in the basin
					// --------------------------------------------------------------------------------------------------
					if (withDiagnose == true) {
						// calculate and keep the features of the stock recruitment relationships
						riverBasin.setMortalityCrash(stockRecruitmentRelationship.getSigmaZcrash());

						// initialise the mortality function for the autochnous spawners
						// use to approximate the mortality of all the spawners to give a proxy of the Allee trap 
						if (numberOfFemaleSpawners > 0.) {
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
								"\tStotal="+numberOfFemaleSpawners+"\tStotalMale="+numberOfMaleSpawners+ "\tSautochthonous="+
								spawnerOriginsDuringReproduction.get(riverBasin.getName()));

						/*	// display effective from each catchment
						System.out.print(riverBasin.getName());
						for (String natalBasinName : group.getEnvironment().getRiverBasinNames()){
							System.out.print("\t"+natalBasinName);
						}
						System.out.println();
						System.out.print(riverBasin.getName());
						for (String natalBasinName : group.getEnvironment().getRiverBasinNames()){
							System.out.print("\t"+spawnerOriginsDuringReproduction.get(natalBasinName));
						}
						System.out.println();*/

						// System.out.println("\t"+ riverBasin.getPopulationStatus());

						/*String message;
						if (Double.isNaN(riverBasin.getNativeSpawnerMortality()))
							message="noSense";
						else {
							double stockTrap=stockRecruitmentRelationship.getStockTrap(riverBasin.getNativeSpawnerMortality());
							if (riverBasin.getNativeSpawnerMortality()>stockRecruitmentRelationship.getSigmaZcrash())
								message="overZcrash";
							else {
								if (numberOfFemaleSpawners < stockTrap)
									message = "inTrapWithStrayers";
								else {
									if (spawnerOriginsDuringReproduction.get(riverBasin.getName()) < stockTrap)
										message = "inTrapWithOnlyNatives";
									else
										message = "sustain";
								}
							}
						}
						System.out.println("\t"+message);*/
					}

					// --------------------------------------------------------------------------------------------------
					// Reproduction process (compute the number of recruits)
					//  need to have at least one female and one male
					//  use the proportion of female at birth to compute the number of recruits of each gender
					// --------------------------------------------------------------------------------------------------

					if (numberOfFemaleSpawners > 0. && numberOfMaleSpawners >0.) {

						//BH Stock-Recruitment relationship with logistic depensation
						double meanNumberOfRecruit = stockRecruitmentRelationship.getRecruitment(numberOfFemaleSpawners);

						//  lognormal random draw
						muRecruitment = Math.log(meanNumberOfRecruit) - (Math.pow(sigmaRecruitment,2))/2;
						long numberOfRecruit = Math.round(Math.exp(genNormal.nextDouble()*sigmaRecruitment + muRecruitment));
						long numberOfFemaleRecruit =  Math.round(numberOfRecruit *  proportionOfFemaleAtBirth);
						long numberOfMaleRecruit  = numberOfRecruit - numberOfFemaleRecruit;

						//System.out.println(group.getPilot().getCurrentTime()+"  "+Time.getSeason(group.getPilot())+"  "+ riverBasin.getName()+": " + numberOfGenitors + "  spwaners \tgive "+ numberOfRecruit + " recruits");

						// ----------------------------------------------
						// keep information when reproduction
						// keep last % of  autochtone
						riverBasin.getLastPercentagesOfAutochtones().push(numberOfAutochtones * 100 / numberOfFemaleSpawners);

						// keep the number of spawners for the first time in the basin
						if (numberOfFemaleSpawnerForFirstTime>0) {
							riverBasin.getSpawnersForFirstTimeMeanAges(Gender.FEMALE).push(femaleSpawnersForFirstTimeAgesSum / numberOfFemaleSpawnerForFirstTime);
							riverBasin.getSpawnersForFirstTimeMeanLengths(Gender.FEMALE).push(femaleSpawnersForFirstTimeLengthsSum / numberOfFemaleSpawnerForFirstTime);
						}
						else {
							riverBasin.getSpawnersForFirstTimeMeanAges(Gender.FEMALE).push(0.);
							riverBasin.getSpawnersForFirstTimeMeanLengths(Gender.FEMALE).push(0.);
						}
						if (numberOfMaleSpawnerForFirstTime>0) {
							riverBasin.getSpawnersForFirstTimeMeanAges(Gender.MALE).push(maleSpawnersForFirstTimeAgesSum/numberOfMaleSpawnerForFirstTime);
							riverBasin.getSpawnersForFirstTimeMeanLengths(Gender.MALE).push(maleSpawnersForFirstTimeLengthsSum / numberOfMaleSpawnerForFirstTime);
						}
						else {
							riverBasin.getSpawnersForFirstTimeMeanAges(Gender.MALE).push(0.);
							riverBasin.getSpawnersForFirstTimeMeanLengths(Gender.MALE).push(0.);
						}

						// display info from a catchment list
						if (displayFluxesOnConsole == true) {
							if (basinList.contains(riverBasin.getName()) ) {
								System.out.print(riverBasin.getName() + ": " + numberOfFemaleSpawners + " + "+ numberOfMaleSpawners +
										" -> "+ numberOfRecruit + "\t")  ;
							}
						}

						// Creation of new superFish
						if (numberOfRecruit > 0){
							long numberOfsuperIndividual, effectiveAmount, remainingFish;

							// features of the super individuals
							// for female
							numberOfsuperIndividual = Math.max(1L, 
									Math.round(numberOfFemaleRecruit / amountPerSuperIndividual));
							effectiveAmount =  (long) Math.floor(numberOfFemaleRecruit / numberOfsuperIndividual);
							for (long i = 0; i < (numberOfsuperIndividual-1); i++){
								group.addAquaNism(new DiadromousFish(group.getPilot(), riverBasin, initialLength, effectiveAmount, Gender.FEMALE));
							}
							// the last Super indivial could be larger to include remainging fish
							remainingFish = numberOfFemaleRecruit - numberOfsuperIndividual * effectiveAmount;
							group.addAquaNism(new DiadromousFish(group.getPilot(), riverBasin, initialLength, effectiveAmount + remainingFish, Gender.FEMALE));


							// for male
							numberOfsuperIndividual = Math.max(1L, 
									Math.round(numberOfMaleRecruit / amountPerSuperIndividual));
							effectiveAmount =  (long) Math.floor(numberOfMaleRecruit / numberOfsuperIndividual);
							for (long i = 0; i < (numberOfsuperIndividual-1); i++){
								group.addAquaNism(new DiadromousFish(group.getPilot(), riverBasin, initialLength, effectiveAmount, Gender.MALE));
							}
							// the last Super indivial could be larger to include remainging fish
							remainingFish = numberOfMaleRecruit - numberOfsuperIndividual * effectiveAmount;
							group.addAquaNism(new DiadromousFish(group.getPilot(), riverBasin, initialLength, effectiveAmount + remainingFish, Gender.MALE));

							// ----------------------------------------------
							// keep information when reproduction with success
							// stock the first year when recruitment is non nul
							if (riverBasin.getYearOfFirstNonNulRep() == 0){
								riverBasin.setYearOfFirstNonNulRep(time.getYear(group.getPilot()));
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
							riverBasin.setYearOfLastNulRep(time.getYear(group.getPilot()));							
							riverBasin.getLastRecruitmentExpectations().push((long) 0);
							riverBasin.getLastRecruitments().push((long) 0);
							riverBasin.getLastRecsOverProdCaps().push(0.);
							riverBasin.getNumberOfNonNulRecruitmentDuringLastYears().push(0.);
							riverBasin.getNumberOfNonNulRecruitmentForFinalProbOfPres().push(0.);
						}
					}
					else {
						// stock information when no spawners reproduce
						//System.out.println(riverBasin.getName()+ "\tF:"+numberOfFemaleSpawners+ " M:"+numberOfMaleSpawners);
						riverBasin.setYearOfLastNulRep(time.getYear(group.getPilot()));
						riverBasin.getLastRecruitmentExpectations().push((long) 0);
						riverBasin.getLastRecruitments().push((long) 0);
						riverBasin.getLastRecsOverProdCaps().push(0.);
						riverBasin.getLastPercentagesOfAutochtones().push(0.);
						riverBasin.getNumberOfNonNulRecruitmentDuringLastYears().push(0.);
						riverBasin.getNumberOfNonNulRecruitmentForFinalProbOfPres().push(0.);
					}
					// --------------------------------------------------------------------------------------------------
					// Remove deadfish and compute the related nutrient fluxes 
					// --------------------------------------------------------------------------------------------------
					/*	for (DiadromousFish fish : deadFish){
						group.removeAquaNism(fish);
					}
					deadFish.clear();*/

					// -------------------------------------------------------
					// display information
					// -----------------------------------------------------
					/*	if 	(displayFluxesOnConsole)

						System.out.println(group.getPilot().getCurrentTime() + "; " + Time.getYear(group.getPilot()) + ";" + Time.getSeason(group.getPilot()) + ";IMPORT;"
								+ riverBasin.getName() + ";" +  fluxBefore + ";" + riverBasin.getSpawnerNumberPerGroup(group)+  "; " + totalInputFluxes); */
					BufferedWriter bW = group.getbWForFluxes();
					if ( bW != null) {
						try {
							for (SpawnerOrigin origin : totalInputFluxes.keySet()) {
								bW.write(group.getPilot().getCurrentTime() + "; " + time.getYear(group.getPilot()) + ";" + time.getSeason(group.getPilot()) 
								+";"+ riverBasin.getName() +  ";" + fluxBefore + ";" + "IMPORT"+ ";" + origin);
								bW.write(";" + totalInputFluxes.get(origin).get("biomass"));

								for (String nutrient : group.getNutrientRoutine().getNutrientsOfInterest()) {
									bW.write(";" + totalInputFluxes.get(origin).get(nutrient));
								}
								bW.write("\n");
							}
						} catch (IOException e) {

							e.printStackTrace();
						}
					}
				}
				else {
					riverBasin.setYearOfLastNulRep(time.getYear(group.getPilot()));
				}


				//System.out.println("("+numberOfGenitors+")");
				//System.out.println("  BEFORE " +riverBasin.getSpawnerOrigins().keySet());
				riverBasin.getSpawnerOrigins().push(spawnerOriginsDuringReproduction);
				//System.out.println("  AFTER " +riverBasin.getSpawnerOrigins().keySet());
			}
			if (displayFluxesOnConsole == true)
				System.out.println();

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



