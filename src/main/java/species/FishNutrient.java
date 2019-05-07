/**
 * 
 * @author Camille Poulet, Patrick Lambert
 * @copyright Copyright (c) 2019, Irstea
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

import environment.SeaBasin;
import environment.Time.Season;
import fr.cemagref.simaqualife.pilot.Pilot;
import species.DiadromousFish.Gender;
import species.DiadromousFish.Stage;
import species.ReproduceAndSurviveAfterReproduction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;


/**
 * @author camille.poulet
 *
 */
public class FishNutrient {

	private ArrayList<String> nutrientsOfInterest;
	/**
	 * Main feature for weight computation before spawning i.e. gametes expelling //Voir pour un retour à la ligne lors du commentaire 
	 * key gender
	 * value
	 * 		key feature
	 * 		value value
	 */
	private Map <Gender,Map<String, Double>> fishFeaturesPreSpawning;

	private Map <Gender, Map<String, Double>> fishFeaturesPostSpawning;

	private Map <String, Double> juvenileFeatures;


	/**
	 * chemical composition of carcass before gametes expelling (before spawning) i.e. carcass + gonads + gametes
	 * <key> gender
	 * <value> 
	 * 		<key> chemical element
	 * 		<value> value
	 */
	private Map<DiadromousFish.Gender,Map<String,Double>> compoCarcassPreSpawning;

	//package permettant la création d'une table de hachage ie fonctionnant en clé -valeur. Clé unique, mais valeur peut être associée à plusieurs clés; 
	//La class d'objet Map a pour point faible la taille des données à stocker. Plus on a de valeur dans la table, plus c'est lourd et lent! Donc, trouver un compromis entre temps de calcul et espace.
	//N'accepte pas la valeur nulle et thread safe i.e. utilisable simultanément par plusieurs éléments du programme. 

	/**
	 * chemical composition of carcass after spawning i.e. gonads without gametes
	 * <key> gender
	 * <value> 
	 * 		<key> chemical element
	 * 		<value> value
	 */
	private Map<DiadromousFish.Gender, Map<String, Double>> compoCarcassPostSpawning;

	/**
	 * chemical composition of gametes
	 * <key> gender
	 * <value> 
	 * 		<key> chemical element
	 * 		<value> value
	 */
	private Map<DiadromousFish.Gender, Map<String,Double>> compoGametes;

	// For juveniles - Based on Taverny (1991)

	/**
	 * chemical composition of juveniles
	 * <key> stage
	 * <value> 
	 * 		<key> chemical element
	 * 		<value> value
	 */

	private Map<String,Double> compoJuvenile; 


	/**
	 * 
	 */
	public FishNutrient() {
		// TODO Auto-generated constructor stub
	}


	/**
	 * Constructor based on the 5 Map of fish composition 
	 * @param fishFeaturesPreSpawning
	 * @param compoCarcassPreSpawning
	 * @param compoCarcassPostSpawning
	 * @param compoGametes
	 * @param compoJuvenile
	 */
	public FishNutrient(ArrayList<String> nutrientsOfInterest, Map<Gender, Map<String, Double>> fishFeaturesPreSpawning,
			Map<Gender, Map<String, Double>> fishFeaturesPostSpawning,
			Map<Gender, Map<String, Double>> compoCarcassPreSpawning,
			Map<Gender, Map<String, Double>> compoCarcassPostSpawning, Map<Gender, Map<String, Double>> compoGametes,
			Map<String, Double> juvenileFeatures,
			Map<String, Double> compoJuvenile) {
		super();
		this.nutrientsOfInterest = nutrientsOfInterest;
		this.fishFeaturesPreSpawning = fishFeaturesPreSpawning;
		this.fishFeaturesPostSpawning = fishFeaturesPostSpawning;
		this.compoCarcassPreSpawning = compoCarcassPreSpawning;
		this.compoCarcassPostSpawning = compoCarcassPostSpawning;
		this.compoGametes = compoGametes;
		this.juvenileFeatures = juvenileFeatures;
		this.compoJuvenile = compoJuvenile; 

	}

	/**
	 * compute the nutrient fluxes for a single fish (in the super individual)
	 *  that dies before spawning  
	 * @param fish
	 */
	public Map<String,Double> computeNutrientsInputForDeathBeforeSpawning(DiadromousFish fish, ArrayList<String> nutrientsOfInterest) { 

		Map<String,Double> nutrientsInput = new Hashtable<String, Double>(); // On créer ici une Map, classe mère des hashtable (Homme = classe mere ie Map//Jules = hashtable)
		for (String nutrient : nutrientsOfInterest) {

			if (fish.getStage()== Stage.MATURE) {
				double totalWeightPre = fishFeaturesPreSpawning.get(fish.getGender()).get("aLW") * Math.pow(fish.getLength(), fishFeaturesPreSpawning.get(fish.getGender()).get("bLW"));
				//totalWeightPost = totalWeightPre * (1-GSIfemalePost)+ totalWeightPost * GSIfemalePost * CoeffLossWeight
				nutrientsInput.put(nutrient, totalWeightPre * compoCarcassPreSpawning.get(fish.getGender()).get(nutrient));
			}
			else { 
				nutrientsInput.put(nutrient, 0.);
			}
		}
		return nutrientsInput;
	}

	public Map<String,Double> computeNutrientsInputForDeathBeforeSpawning(DiadromousFish fish) { 

		return computeNutrientsInputForDeathBeforeSpawning(fish,this.nutrientsOfInterest); 
	}

	/**
	 * compute the nutrient fluxes for a single fish (in the super individual)
	 * that dies after spawning (gametes expelling)
	 * @param fish
	 * @return nutrientsInput
	 */
	public Map<String, Double> computeNutrientsInputForDeathAfterSpawning(DiadromousFish fish, ArrayList<String> nutrientsOfInterest) {

		Map<String,Double> nutrientsInput = new Hashtable<String,Double>();
		for (String nutrient : nutrientsOfInterest) {

			if (fish.getStage()== Stage.MATURE) {

				double totalWeightPre = fishFeaturesPreSpawning.get(fish.getGender()).get("aLW") * 
						Math.pow(fish.getLength(), fishFeaturesPreSpawning.get(fish.getGender()).get("bLW"));
				double totalWeightPost = fishFeaturesPostSpawning.get(fish.getGender()).get("aLW") * 
						Math.pow(fish.getLength(), fishFeaturesPostSpawning.get(fish.getGender()).get("bLW"));
				nutrientsInput.put(nutrient,(totalWeightPre - totalWeightPost) * 
						compoCarcassPostSpawning.get(fish.getGender()).get(nutrient));
			}
			else {
				nutrientsInput.put(nutrient,0.);
			}	
		}
		return nutrientsInput; 
	}

	public Map<String, Double> computeNutrientsInputForDeathAfterSpawning(DiadromousFish fish){

		return computeNutrientsInputForDeathAfterSpawning(fish, this.nutrientsOfInterest); 
	}
	/**
	 * compute the nutrient fluxes for a single fish (in the super individual)
	 * that survives after spawning 
	 * Map: model output = element of interest ie string + double ie the quantification of this fluxes. 
	 * @return nutrientsInput
	 */
	public Map<String,Double>computeNutrientsInputForSurvivalAfterSpawning(DiadromousFish fish, ArrayList<String> nutrientsOfInterest) {

		Map<String,Double> nutrientsInput = new Hashtable<String,Double>();
		for (String nutrient: nutrientsOfInterest) {
			if (fish.getStage()==Stage.MATURE) {

				double totalWeightPre = fishFeaturesPreSpawning.get(fish.getGender()).get("aLW") 
						* Math.pow(fish.getLength(), fishFeaturesPreSpawning.get(fish.getGender()).get("bLW"));
				//TODO Fix with new data 
				double totalWeightPost = fishFeaturesPostSpawning.get(fish.getGender()).get("aLW")
						* Math.pow(fish.getLength(), fishFeaturesPostSpawning.get(fish.getGender()).get("bLW"));
				nutrientsInput.put(nutrient, (totalWeightPre - totalWeightPost) * 
						compoGametes.get(fish.getGender()).get(nutrient)); 	
			}
			else {
				nutrientsInput.put(nutrient,0.);
			}
		}
		return nutrientsInput;
	}	

	public Map<String,Double>computeNutrientsInputForSurvivalAfterSpawning(DiadromousFish fish) {

		return computeNutrientsInputForSurvivalAfterSpawning(fish, this.nutrientsOfInterest);
	}


	public Map<String,Double> computeNutrientsExportForJuveniles(DiadromousFish juvenileFish, ArrayList<String>nutrientsOfInterest) {
		Map<String,Double> nutrientsExport = new Hashtable<String,Double>();
		for(String nutrient: nutrientsOfInterest) {
			if(juvenileFish.getStage()==Stage.IMMATURE) {

				double JuvenileMass = juvenileFeatures.get("aLW") * 
						Math.pow(juvenileFish.getLength(), juvenileFeatures.get("bLW"));
				nutrientsExport.put(nutrient, JuvenileMass * compoJuvenile.get(nutrient));
			}
		}

		return nutrientsExport;
	}

	public Map<String,Double> computeNutrientsExportForJuveniles(DiadromousFish juvenileFish){
		return computeNutrientsExportForJuveniles(juvenileFish, this.nutrientsOfInterest);

	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args)	{

		Map<Gender, Map<String, Double>> aFeaturePreSpawning = new Hashtable<DiadromousFish.Gender, Map<String,Double>>();
		Map<String,Double> aFeature = new Hashtable<String,Double>();
		aFeature.put("bLW",3.3429);// parametre "b" de la relation taille/poids - Coefficient d'allometrie
		aFeature.put("aLW",1.2102E-6 * Math.pow(10., aFeature.get("bLW"))); // parametre "a" de la relation taille/poids en kg/cm- Traduit la condition
		aFeature.put("GSI",0.15); 
		aFeaturePreSpawning.put(Gender.FEMALE, aFeature);

		aFeature = new Hashtable<String,Double>();
		aFeature.put("bLW",3.2252);
		aFeature.put("aLW",2.4386E-6 * Math.pow(10, aFeature.get("bLW"))); // Conversion des g/mm en g.cm (from Taverny, 1991) 
		aFeature.put("GSI",.07);
		aFeaturePreSpawning.put(Gender.MALE,aFeature);

		System.out.println("aFeaturePreSpawning: " + aFeaturePreSpawning.toString()); //

		Map<Gender, Map<String, Double>> aFeaturePostSpawning = new Hashtable<DiadromousFish.Gender, Map<String,Double>>();
		aFeature = new Hashtable<String,Double>();
		aFeature.put("GSI",0.15);
		aFeature.put("aLW",aFeaturePreSpawning.get(Gender.FEMALE).get("aLW")/(1+aFeature.get("GSI"))); // parametre "a" de la relation taille/poids avec Lt en cm - Traduit la condition
		aFeature.put("bLW",aFeaturePreSpawning.get(Gender.FEMALE).get("bLW"));// parametre "b" de la relation taille/poids - Coefficient d'allometrie
		aFeaturePostSpawning.put(Gender.FEMALE, aFeature);

		aFeature = new Hashtable<String,Double>();
		aFeature.put("GSI",.07);
		aFeature.put("aLW",aFeaturePreSpawning.get(Gender.MALE).get("aLW")/(1+aFeature.get("GSI")));
		aFeature.put("bLW",aFeaturePreSpawning.get(Gender.MALE).get("bLW"));
		aFeaturePostSpawning.put(Gender.MALE,aFeature);

		System.out.println("aFeaturePostSpawning: " + aFeaturePostSpawning.toString());

		// carcass composition for fish before spawning
		Map<Gender, Map<String, Double>> aCompoCarcassPreSpawning = new Hashtable<DiadromousFish.Gender,Map<String,Double>>();
		Map<String,Double> aCompo = new Hashtable<String,Double>();
		aCompo.put("N", 2.917); //On remplit une collection avec un put. 
		aCompo.put("P", 0.725);
		aCompoCarcassPreSpawning.put(Gender.FEMALE,aCompo);

		aCompo = new Hashtable<String,Double>();
		aCompo.put("N", 2.921);
		aCompo.put("P",0.662);
		aCompoCarcassPreSpawning.put(Gender.MALE,aCompo);

		System.out.println("aCompoCarcassPreSpawning: " + aCompoCarcassPreSpawning.toString()); //

		// carcass composition for fish after spawning
		Map<Gender, Map<String, Double>> aCompoCarcassPostSpawning = new Hashtable<DiadromousFish.Gender,Map<String,Double>>();
		aCompo = new Hashtable<String,Double>();
		aCompo.put("N", 3.216); //On remplit une collection avec un put. 
		aCompo.put("P", 0.997);
		aCompoCarcassPostSpawning.put(Gender.FEMALE,aCompo);

		aCompo = new Hashtable<String,Double>();
		aCompo.put("N", 2.790); // From Haskel et al, 2017 
		aCompo.put("P",0.961);
		aCompoCarcassPostSpawning.put(Gender.MALE,aCompo);

		System.out.println("aCompoCarcassPostSpawning: " + aCompoCarcassPostSpawning.toString()); //

		// Gametes composition approximated by the difference between gonads weight before and after spawning. 
		Map<Gender, Map<String, Double>> aCompoGametes = new Hashtable<DiadromousFish.Gender,Map<String,Double>>();
		aCompo = new Hashtable<String,Double>();
		aCompo.put("N", 3.242); //On remplit une collection avec un put. From Haskel et al, 2018. 
		aCompo.put("P", 0.320);
		aCompoGametes.put(Gender.FEMALE,aCompo);

		aCompo = new Hashtable<String,Double>();
		aCompo.put("N", 3.250);
		aCompo.put("P", 0.724);
		aCompoGametes.put(Gender.MALE,aCompo);

		System.out.println("aCompoGametes:" + aCompoGametes.toString()); //

		// features for juveniles 

		Map<String,Double> aJuvenileFeatures = new Hashtable<String, Double>();
		aJuvenileFeatures.put("aLW",Math.exp(-11.942));
		aJuvenileFeatures.put("bLW",3.0306);

		System.out.println(aJuvenileFeatures.toString()); 

		// carcass composition for juveniles fish 
		Map<String, Double> aCompoJuveniles = new Hashtable<String,Double>();
		aCompoJuveniles.put("N", 2.803); //On remplit une collection avec un put. %N in wet weight (Haskell et al, 2017) on Alosa sapidissima 
		aCompoJuveniles.put("P", 0.887); //%P in wet weight (from Haskell et al, 2017) on Alosa sapidissima 

		System.out.println("aCompoJuveniles: " + aCompoJuveniles.toString()); 

		ArrayList <String> nutrientsOfInterest= new ArrayList <String>();
		nutrientsOfInterest.add("N");
		nutrientsOfInterest.add("P");

		System.out.println("nutrientsOfInterest: " + nutrientsOfInterest);
		
		FishNutrient fn = new FishNutrient(nutrientsOfInterest, aFeaturePreSpawning, aFeaturePostSpawning, 
				aCompoCarcassPreSpawning, aCompoCarcassPostSpawning, aCompoGametes,
				aJuvenileFeatures, aCompoJuveniles);

		SeaBasin basin = new SeaBasin(0,"Bidon",10.,12., 14.,12.); //il faut aller dans "SeaBasin" dans "environement et regarder comment est construit le constructeur. Il lui faut ici un rang, un nom de bassin versant, et des température pour chaque saison 
		Pilot pilot = new Pilot ();
		DiadromousFish fish = new DiadromousFish (pilot, basin, 40., 1L, Gender.FEMALE); //Idem ici, on regarde comment est construit DiadromousFih et on lui donne les valeur de ce qu'il nous demande. 
		fish.setStage(Stage.MATURE);
		DiadromousFish juvenileFish = new DiadromousFish(pilot,basin,2.0,1L,Gender.UNDIFFERENCIED);
		juvenileFish.setStage(Stage.IMMATURE);
		
		System.out.println(fish.toString());
		System.out.println("Nutrients Fluxes for death before spawning " + fn.computeNutrientsInputForDeathBeforeSpawning(fish).toString());
		System.out.println("Nutrients Fluxes for death after spawning " + fn.computeNutrientsInputForDeathAfterSpawning(fish).toString());
		System.out.println("Nutrients Fluxes for survival  " + fn.computeNutrientsInputForSurvivalAfterSpawning(fish).toString());
		System.out.println("Nutrients Fluxes for juveniles " + fn.computeNutrientsExportForJuveniles(juvenileFish).toString());


		/* Create XML file 
		 * 
		 */
		//System.out.println((new	XStream(new DomDriver())).toXML(fn));

	} 
}


