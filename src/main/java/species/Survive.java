package species;


import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import environment.Basin;

import environment.Basin.TypeBassin;
import environment.Time;
import fr.cemagref.simaqualife.kernel.processes.AquaNismsGroupProcess;
import fr.cemagref.simaqualife.pilot.Pilot;
import miscellaneous.BinomialForSuperIndividualGen;
import miscellaneous.Miscellaneous;
import species.DiadromousFish.SpawnerOrigin;

import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = AquaNismsGroupProcess.class)
public class Survive extends AquaNismsGroupProcess<DiadromousFish, DiadromousFishGroup> {

	public double tempMinSurvivalSpawnerInRiv = 10. ;
	public double tempMaxSurvivalSpawnerInRiv = 23. ; // DEFINIR TROIS AUTRES POUR MORTALITE
	public double tempOptSurvivalSpawnerInRiv = 20.;
	public double survivalProbOptSpawnerInRiv = 1.;

	//public double mortalityRateInRiver = 0.4;
	public double mortalityRateInSea = 0.4;
	public double mortalityRateInOffshore = 0.4;

	/**
	 * the random numbers generator for binomial draws
	 * @unit --
	 */
	private transient BinomialForSuperIndividualGen aleaGen;
	
	public static void main(String[] args) { System.out.println((new
			XStream(new DomDriver())) .toXML(new Survive())); }


	@Override
	public void initTransientParameters(Pilot pilot) {
		super.initTransientParameters(pilot);
		aleaGen = new BinomialForSuperIndividualGen(pilot.getRandomStream());
	}

	
	@Override
	public void doProcess(DiadromousFishGroup group) {

		Time time = group.getEnvironment().getTime();

		double survivalProbability=1.;
		long survivalAmount;
		long deathAmount; 

		Map<SpawnerOrigin, Map<String, Double>> totalInputFluxes = new Hashtable<SpawnerOrigin, Map <String, Double>>(); //On cr�er la Map pour stocker les flux 
		totalInputFluxes.put(SpawnerOrigin.AUTOCHTONOUS, new Hashtable < String, Double>()); 
		totalInputFluxes.put(SpawnerOrigin.ALLOCHTONOUS, new Hashtable < String, Double>()); 

		for (SpawnerOrigin origin: totalInputFluxes.keySet()) {
			for (String nutrient : group.getNutrientRoutine().getNutrientsOfInterest()) {
				totalInputFluxes.get(origin).put(nutrient, 0.); // ON MET A JOUR NOTRE map 
			}
			totalInputFluxes.get(origin).put("biomass",0.); 
		}

		//double sumNGuadalquivir = 0.;

		for(Basin basin : group.getEnvironment().getBasins()){
			//System.out.print(basin.getName());

			if (basin.getFishs(group)!=null) {
				//System.out.println(" y a  des poissons");
				ListIterator<DiadromousFish> fishIterator = basin.getFishs(group).listIterator();
				while (fishIterator.hasNext()) {
					DiadromousFish  fish = fishIterator.next();
					survivalProbability = 1.;
					//Compute the survival probability according to the fish position
					if(fish.getPosition().getType() == TypeBassin.RIVER && fish.isMature()){ //Survive in river before spawning 


						//double tempEffectSurv = Miscellaneous.temperatureEffect(fish.getPosition().getCurrentTemperature(group.getPilot()), tempMinSurvivalSpawnerInRiv, tempOptSurvivalSpawnerInRiv, tempMaxSurvivalSpawnerInRiv);
						double tempEffectSurv = Miscellaneous.rectangularTemperatureEffect(fish.getPosition().getCurrentTemperature(group.getPilot()), tempMinSurvivalSpawnerInRiv, tempMaxSurvivalSpawnerInRiv);
						if (tempEffectSurv == 0.){
							survivalProbability = 0.;
							//System.out.println("le poisson situ� dans le bassin " + fish.getPosition().getName() + " en " + Time.getSeason() +" a un coeff de mortalit� de " + fish.getMortalityRateInRiver() + " mais � cause de la temp�rature une prob de survie de " + survivalProbability);
						}else{
							survivalProbability = survivalProbOptSpawnerInRiv * tempEffectSurv;
							//System.out.println("le poisson situ� dans le bassin " + fish.getPosition().getName() + " en " + Time.getSeason() + " a un coeff de mortalit� de " + fish.getMortalityRateInRiver() + " et donc une prob de survie de " + survivalProbability);
						}	
						//TODO:add nutrientFluxesRoutine

					}else if (fish.getPosition().getType() == TypeBassin.SEA){ //Survive at sea 
						survivalProbability = Math.exp(-mortalityRateInSea * time.getSeasonDuration());
						//System.out.println("le poisson situ� dans le bassin " + fish.getPosition().getName() + " en " + Time.getSeason() + " a un coeff de mortalit� de " + fish.getMortalityRateInSea() + " et donc une prob de survie de " + survivalProbability);

					}else if (fish.getPosition().getType() == TypeBassin.OFFSHORE){ //Survive offshore
						survivalProbability = Math.exp(-mortalityRateInOffshore * time.getSeasonDuration());
						//System.out.println("le poisson situ� dans le bassin " + fish.getPosition().getName() + " en " + Time.getSeason() + " a un coeff de mortalit� de " + fish.getMortalityRateInOffshore() + " et donc une prob de survie de " + survivalProbability);

					}else{
						survivalProbability = 1.;
					}
					//Compute survival amount in the SI for one fish whatever its position 
					deathAmount = 0L; 
					if (survivalProbability<1.){
						survivalAmount = aleaGen.getSuccessNumber2(fish.getAmount(), survivalProbability);
						deathAmount = fish.getAmount() - survivalAmount;

						if (survivalAmount > 0) 
							fish.setAmount(survivalAmount);
						else 
							fishIterator.remove();

						if (deathAmount > 0L && fish.getPosition().getType() == TypeBassin.RIVER) { //Compute the fluxes for dead fish in river in the SI. 

							SpawnerOrigin spawnerOrigin; 
							if (fish.getBirthBasin() == basin) 
								spawnerOrigin = SpawnerOrigin.AUTOCHTONOUS; 
							else
								spawnerOrigin = SpawnerOrigin.ALLOCHTONOUS;

							Map<String, Double> aFluxForDeadFishBeforeSpawning = group.getNutrientRoutine().computeNutrientsInputForDeathBeforeSpawning(fish); // 

							for (String nutrient: aFluxForDeadFishBeforeSpawning.keySet()) {
								group.getNutrientRoutine().getNutrientImportFluxesCollection().
								put(time.getYear(group.getPilot()), nutrient, fish.getBirthBasin().getName(), basin.getName(), aFluxForDeadFishBeforeSpawning.get(nutrient) * deathAmount);
							}

							/*
							 * if
							 * (basin.getName().equalsIgnoreCase("Guadalquivir")
							 * ) { sumNGuadalquivir +=
							 * aFluxForDeadFishBeforeSpawning.get("N") *
							 * deathAmount;
							 * System.out.println(Time.getYear(group.getPilot())
							 * + " : " + sumNGuadalquivir); }
							 */
						}
					}
				}//end on loop of fish 
			}
		} //end loop on basin 
	}//end of doprocess 
}//end of class 



