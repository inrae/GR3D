package species;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import miscellaneous.Duo;
import miscellaneous.Miscellaneous;
import species.DiadromousFish.Gender;

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

@Deprecated
@ServiceProvider(service = AquaNismsGroupProcess.class)
public class ReproduceAndSurviveAfterReproduction extends AquaNismsGroupProcess<DiadromousFish, DiadromousFishGroup>{

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


	public static void main(String[] args) { System.out.println((new
			XStream(new DomDriver())) .toXML(new ReproduceAndSurviveAfterReproduction())); }

	@Override
	@InitTransientParameters
	public void initTransientParameters(Pilot pilot) {
		super.initTransientParameters(pilot);
		genNormal = new NormalGen( pilot.getRandomStream(),
				new NormalDist(0., 1.));
	}
	
	private double functionMortality(double z, Duo<Integer, Double>[] coeff){
		double res=0.;
		for(Duo<Integer, Double> duo : coeff){
			res += duo.getSecond() * Math.exp(z * (double) duo.getFirst());
		}
		return res-1.;
	}

	@Override
	public void doProcess(DiadromousFishGroup group) {

		if (Time.getSeason(group.getPilot()) == reproductionSeason){
			List<DiadromousFish> deadFish = new ArrayList<DiadromousFish>();
			double b, c, alpha, beta, amountPerSuperIndividual , Setoile, S95, S50 ;

			for(RiverBasin riverBasin : group.getEnvironment().getRiverBasins()){

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
					spawnerOriginsDuringReproduction.put(basinName,  0L);
				}

				// System.out.println("REPRODUCTION  in "+riverBasin.getName()+" : FISH FROM ");

				List<DiadromousFish> fishInBasin =  riverBasin.getFishs(group);
				if (fishInBasin != null){
					
					// effective temperature for reproduction (priority to the ANG value) 
					double tempEffectRep;
					if (group.getTempMinRep() == Double.NaN){
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
					//stockRecruitmentRelationship.init(alpha, beta, S50, S95);

					// age of autochnonous spawnser
					//Map<Integer, Long> ageOfNativeSpawners = new TreeMap<Integer, Long>(); 
					
					// compute the number of spawners and keep the origine of the spawners
					for( DiadromousFish fish : fishInBasin){
						if( fish.isMature()){
							if (fish.getNumberOfReproduction() < 1) {
								numberOfSpawnerForFirstTime++; //ASK individual or super-individual ?
								spawnersForFirstTimeAgesSum += fish.getAge();
							}
							numberOfGenitors += fish.getAmount() ;

							// spawner per origine
							String basinName = fish.getBirthBasin().getName();
							spawnerOriginsDuringReproduction.put(basinName, spawnerOriginsDuringReproduction.get(basinName) + fish.getAmount() );

							// number of autochtonous fish per age
							/*if (riverBasin == fish.getBirthBasin()){ 
								numberOfAutochtones += fish.getAmount();
								Integer age = (int) Math.floor(fish.getAge());
								if (ageOfNativeSpawners.containsKey(age))
									ageOfNativeSpawners.put(age, ageOfNativeSpawners.get(age)+fish.getAmount());
								else
									ageOfNativeSpawners.put(age, fish.getAmount());
							} */

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

					System.out.println("  numberOfGenitors: "+  numberOfGenitors);
					/*
					// calcul de la mortalit� associ�e aux g�niteurs autochtones
					if (numberOfGenitors > 0.) {
						System.out.println(riverBasin.getName().toUpperCase());
						System.out.println("Spawners");
						for (Entry<Integer, Long> entry : ageOfNativeSpawners.entrySet()){
							System.out.println("\t"+ entry.toString());
						}
						Duo<Integer, Double>[] coeffs= new Duo[ageOfNativeSpawners.size()];
						int j=0;
						for (Integer age : ageOfNativeSpawners.keySet()){
							if (riverBasin.getLastRecruitments().getItemFromLast(age) != null){
								//System.out.println(ageOfNativeSpawners.get(age)+"/"+ riverBasin.getLastRecruitments().getItemFromLast(age));
								coeffs[j] = new Duo<Integer, Double>(age, 
										(double) ageOfNativeSpawners.get(age) 
										/ (double) riverBasin.getLastRecruitments().getItemFromLast(age));
							} else{
								coeffs[j] =new Duo<Integer, Double>(age, 0.);
							}
							j++;
						}

						System.out.println("Recruits");
						for (Object recruit : riverBasin.getLastRecruitments().toArray()){
							System.out.println("\t"+ (Long) recruit);
						}
						System.out.println("\t last "+riverBasin.getLastRecruitments().getLastItem());

						System.out.println("coeffs");
						for (Duo duo : coeffs){
							System.out.println(" "+duo.getFirst()+"<->"+duo.getSecond());
						}
					}*/

					// Reproduction process (number of recruits)
					if (numberOfGenitors > 0.) {
						//BH Stock-Recruitment relationship with logistic depensation
						double meanNumberOfRecruit = 
								Math.round((alpha * numberOfGenitors * (1 / (1 + Math.exp(- Math.log(19)*((numberOfGenitors - S50) / (S95 - S50)))))) /
										(beta + numberOfGenitors * (1 / (1 + Math.exp(- Math.log(19)*((numberOfGenitors - S50) / (S95 - S50)))))));

						//  lognormal random draw
						muRecruitment = Math.log(meanNumberOfRecruit) - (Math.pow(sigmaRecruitment,2))/2;
						long numberOfRecruit = Math.round(Math.exp(genNormal.nextDouble()*sigmaRecruitment + muRecruitment));

						// keep last % of  autochtone
						riverBasin.getLastPercentagesOfAutochtones().push(numberOfAutochtones * 100 / numberOfGenitors);

						// keep the number of spawners for the firt time in the basin
						if (numberOfSpawnerForFirstTime>0){
							riverBasin.getSpawnersForFirstTimeMeanAges().push(spawnersForFirstTimeAgesSum/numberOfSpawnerForFirstTime);
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
						
							for (int i=0; i<numberOfsuperIndividual; i++){
								group.addAquaNism(new DiadromousFish(group.getPilot(), riverBasin, initialLength, effectiveAmount, Gender.UNDIFFERENCIED));
							}
							
							// stock the first year when recruitment is non nul
							if (riverBasin.getYearOfFirstNonNulRep() == 0){
								riverBasin.setYearOfFirstNonNulRep(Time.getYear(group.getPilot()));
							}	
							riverBasin.getLastRecruitmentExpectations().push(Math.round(meanNumberOfRecruit));
							riverBasin.getLastRecruitments().push(numberOfsuperIndividual * effectiveAmount); // on remplit la pile qui permet de stocker un nombre fix� de derniers recrutement
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
			// on met � jour les observeurs
			for (RiverBasin riverBasin : group.getEnvironment().getRiverBasins()){
				riverBasin.getCobservable().fireChanges(riverBasin, pilot.getCurrentTime());
			}                                                
		}
	}
}

