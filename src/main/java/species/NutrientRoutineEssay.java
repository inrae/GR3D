
package species;

import environment.SeaBasin;
import fr.cemagref.simaqualife.pilot.Pilot;
import species.DiadromousFish.Gender;
import species.DiadromousFish.Stage;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;


/**
 * @author camille.poulet
 *
 */
public class NutrientRoutineEssay {


	private static enum SpawningPosition  {PRE,POST}; // on cr�er un static pour r�server une m�me classe m�moire pour toutes les instances 

	//private static enum Gender {UNDIFFERENCIED, FEMALE, MALE}; 

	private ArrayList<String> nutrientsOfInterest;

	private double residenceTime;

	private Map<String, Double> excretionRate; 

	/**
	 * Main feature for weight (g) computation before spawning i.e. gametes expelling according to gender, for a given length (cm) 
	 * //Voir pour un retour � la ligne lors du commentaire 
	 * key gender
	 * value
	 * 		key feature
	 * 		value value
	 */
	private Map <DiadromousFish.Gender,Map<String, Double>> fishFeaturesPreSpawning;

	private Map <DiadromousFish.Gender, Map<String, Double>> fishFeaturesPostSpawning;

	private Map<String, Double> juvenileFeatures;


	/**
	 *  Weight  of gametes spawned for both males and females 
	 * key gender
	 * value g
	 * usually computed as the difference between unspawned gonad (inbound) and spawned gonad (outbound; "spent gonad") 
	 */
	//private Map <DiadromousFish.Gender, Double> spawnedGametesWeight; 


	/**
	 * chemical composition of carcass before gametes expelling (before spawning) i.e. soma + gonads + gametes
	 * <key> gender
	 * <value> 
	 * 		<key> chemical element
	 * 		<value> value ratio element / (total wet weight) g/g
	 */
	private Map<DiadromousFish.Gender,Map<String,Double>> compoCarcassPreSpawning;

	//package permettant la cr�ation d'une table de hachage ie fonctionnant en cl� -valeur. Cl� unique, mais valeur peut �tre associ�e � plusieurs cl�s; 
	//La class d'objet Map a pour point faible la taille des donn�es � stocker. Plus on a de valeur dans la table, plus c'est lourd et lent! Donc, trouver un compromis entre temps de calcul et espace.
	//N'accepte pas la valeur nulle et thread safe i.e. utilisable simultan�ment par plusieurs �l�ments du programme. 


	/**
	 * chemical composition of gametes estimated from the MIGADO dataset (BDalosesBruch)
	 * <key> gender
	 * <value> 
	 * 		<key> chemical element
	 * 		<value> value
	 */
	private Map<DiadromousFish.Gender, Map<String,Double>> compoGametes;

	// For juveniles - Based on Taverny (1991)

	/**
	 * chemical composition of juveniles
	 * 		<key> chemical element
	 * 		<value> value
	 */

	private Map<String,Double> compoJuvenile;



	/**
	 * Constructor based on the 5 Map of fish composition 
	 * @param fishFeaturesPreSpawning
	 * @param compoCarcassPreSpawning
	 * @param compoCarcassPostSpawning
	 * @param compoGametes
	 * @param compoJuvenile
	 */
	public NutrientRoutineEssay(ArrayList<String> nutrientsOfInterest, 

			double residenceTime,
			Map <String, Double> excretionRate,
			Map<DiadromousFish.Gender, Map<String, Double>> fishFeaturesPreSpawning,
			Map<DiadromousFish.Gender, Map<String, Double>> fishFeaturesPostSpawning,
			Map<DiadromousFish.Gender, Map<String, Double>> compoCarcassPreSpawning,
			Map<DiadromousFish.Gender, Map<String, Double>> compoGametes,
			Map<String, Double> juvenileFeatures,
			Map<String, Double> compoJuvenile)
	{
		super();
		this.nutrientsOfInterest = nutrientsOfInterest;
		this.excretionRate = excretionRate; 
		this.residenceTime = residenceTime; 
		this.fishFeaturesPreSpawning = fishFeaturesPreSpawning;
		this.fishFeaturesPostSpawning = fishFeaturesPostSpawning;
		this.compoCarcassPreSpawning = compoCarcassPreSpawning;
		this.compoGametes = compoGametes;
		this.juvenileFeatures = juvenileFeatures;
		this.compoJuvenile = compoJuvenile; 

	}

	/**
	 * compute the nutrient fluxes for a single fish (in the super individual)
	 *  that dies before spawning  
	 * @param fish
	 */
	public Map<String,Double> computeNutrientsInputForDeadFish(DiadromousFish fish, ArrayList<String> nutrientsOfInterest) { 

		Map<String,Double> nutrientsInput = new Hashtable<String, Double>(); // On cr�er ici une Map, classe m�re des hashtable (Homme = classe mere ie Map//Jules = hashtable)
		for (String nutrient : nutrientsOfInterest) {

			if (fish.getStage()== Stage.MATURE) {
				double totalWeightPre = this.getWeight(fish,SpawningPosition.PRE);
				double carcass = totalWeightPre 
						* compoCarcassPreSpawning.get(fish.getGender()).get(nutrient);
				double excretion = totalWeightPre 
						* residenceTime 
						* excretionRate.get(nutrient) ; 
				double nutrientImport = carcass + excretion;

				nutrientsInput.put(nutrient, nutrientImport); 
			}
			else { 
				nutrientsInput.put(nutrient, 0.);
			}
		}

		//TODO Multiply by fish amount 
		return nutrientsInput;
	}

	public Map<String,Double> computeNutrientsInputForDeadFish(DiadromousFish fish) { 

		return computeNutrientsInputForDeadFish(fish,this.nutrientsOfInterest); 
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
				double totalWeightPre = this.getWeight(fish,SpawningPosition.PRE);
				double gametes = (this.getGonadWeight(fish, SpawningPosition.PRE) - this.getGonadWeight(fish, SpawningPosition.POST))
						* compoGametes.get(fish.getGender()).get(nutrient);
				double excretion = totalWeightPre
						* residenceTime 
						* excretionRate.get(nutrient);
				double nutrientImport = gametes + excretion;

				nutrientsInput.put(nutrient, nutrientImport); 	
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

				double JuvenileMass = this.getWeight(juvenileFish);
				nutrientsExport.put(nutrient, JuvenileMass * compoJuvenile.get(nutrient));
			}
		}

		return nutrientsExport;
	}

	public Map<String,Double> computeNutrientsExportForJuveniles(DiadromousFish juvenileFish){
		return computeNutrientsExportForJuveniles(juvenileFish, this.nutrientsOfInterest);

	}

	/**
	 * Compute the weight for a fish with length (cm) 
	 * @param fish
	 * @return weight (g)
	 */
	public double getWeight (DiadromousFish fish, SpawningPosition spawningPosition) {

		double weight = 0.; 
			if (fish.getStage()==Stage.IMMATURE) 
			weight = juvenileFeatures.get("aLW") * Math.pow(fish.getLength(),juvenileFeatures.get("bLW"));
		else  //Stage.MATURE
			weight = fishFeaturesPreSpawning.get(fish.getGender()).get("aLW") * Math.pow(fish.getLength(), fishFeaturesPreSpawning.get(fish.getGender()).get("bLW") ); 
		
		return weight;
	}

	/**
	 * Compute the weight for a fish with length (cm) 
	 * @param fish
	 * @return weight (g)
	 */
	public double getWeight (DiadromousFish fish) {

		return getWeight (fish, SpawningPosition.PRE);
	}


	/**
	 * Compute the gonad weight for a fish with length (cm) to compute the gamete emission (g). 
	 * @param fish
	 * @return weight (g)
	 */
	public double getGonadWeight (DiadromousFish fish, SpawningPosition spawningPosition) {

		double gonadWeight = 0.; 
			if (fish.getStage()==Stage.MATURE);
				if (spawningPosition == SpawningPosition.PRE)
			gonadWeight = Math.exp(fishFeaturesPreSpawning.get(fish.getGender()).get("aLW_Gonad")
					+ fishFeaturesPreSpawning.get(fish.getGender()).get("bLW_Gonad") * Math.log(fish.getLength())); 
		else 
			gonadWeight = Math.exp(fishFeaturesPostSpawning.get(fish.getGender()).get("aLW_Gonad")
					+ fishFeaturesPostSpawning.get(fish.getGender()).get("bLW_Gonad") * Math.log(fish.getLength())); 

		return gonadWeight;
	}
	
	/**
	 * Compute the gonad weight for a fish with length (cm) to compute the gamete emission (g). 
	 * @param fish
	 * @return weight (g)
	 */
	public double getGonadWeight (DiadromousFish fish) {

		return getGonadWeight (fish, SpawningPosition.PRE);
	}	




	public ArrayList<String> getNutrientsOfInterest() {
		return nutrientsOfInterest;
	}

	/**
	 * @param args
	 */
	/**
	 * @param args
	 */
	/**
	 * @param args
	 */
	public static void main(String[] args)	{


		double aResidenceTime =30; 

		System.out.println("aResidenceTime: " + aResidenceTime); //


		Map <String, Double> anExcretionRate = new Hashtable <String, Double>(); 
		anExcretionRate.put("N", 24.71E-6); //values from Barber et al, Alewifes  in ug/g wet mass/hour : convertit en g
		anExcretionRate.put("P", 2.17E-6); //values from Barber et al, Alewifes in ug/g wet mass/hour: convertit en g

		System.out.println("anExcretionRate: " + anExcretionRate.toString()); //

		/*
		 * A feature pre spawning 
		 */
		Map<Gender, Map<String, Double>> aFeaturePreSpawning = new Hashtable<DiadromousFish.Gender, Map<String,Double>>();

		/*
		 * For females
		 */
		Map<String,Double> aFeature = new Hashtable<String,Double>();

		aFeature.put("bLW", 3.3429); //From Taverny 
		aFeature.put("aLW", 1.2102E-6 * Math.pow(10., aFeature.get("bLW"))); //weight size relationship -- Conversion des g/mm en g.cm (from Taverny, 1991) 
		aFeature.put("bLW_Gonad", 2.6729); // issu de la relation taille - poids des gonades Bruch
		aFeature.put("aLW_Gonad", -5.2425); // issu de la relation taille - poids des gonades Bruch
		
		aFeaturePreSpawning.put(Gender.FEMALE, aFeature);

		/*
		 * For males 
		 */
		aFeature = new Hashtable<String,Double>();
		aFeature.put("bLW", 3.2252);
		aFeature.put("aLW", 2.4386E-6 * Math.pow(10., aFeature.get("bLW"))); //weight size relationship from Taverny -- Conversion des g/mm en g.cm (from Taverny, 1991) 
		aFeature.put("bLW_Gonad", 3.3838); 
		aFeature.put("aLW_Gonad", -8.8744); 
		aFeaturePreSpawning.put(Gender.MALE,aFeature);

		System.out.println("aFeaturePreSpawning: " + aFeaturePreSpawning.toString()); //

		/*
		 * a Feature post Spawning 
		 */
		Map<Gender, Map<String, Double>> aFeaturePostSpawning = new Hashtable<DiadromousFish.Gender, Map<String,Double>>();

		/*
		 * For females 
		 */
		aFeature = new Hashtable<String,Double>();
		
		aFeature.put("aLW_Gonad", -6.6234); // issu de la relation taille - poids des gonades Bruch
		aFeature.put("bLW_Gonad", 2.8545); // issu de la relation taille - poids des gonades Bruch
	
		aFeaturePostSpawning.put(Gender.FEMALE, aFeature);

		/*
		 * For males 
		 */
		aFeature = new Hashtable<String,Double>();

		aFeature.put("aLW_Gonad", -11.285); // issu de la relation taille - poids des gonades Bruch
		aFeature.put("bLW_Gonad", 3.8331); // issu de la relation taille - poids des gonades Bruch
	
		aFeaturePostSpawning.put(Gender.MALE,aFeature);

		System.out.println("aFeaturePostSpawning: " + aFeaturePostSpawning.toString());

		// carcass composition for fish before spawning
		Map<Gender, Map<String, Double>> aCompoCarcassPreSpawning = new Hashtable<DiadromousFish.Gender,Map<String,Double>>();
		Map<String,Double> aCompo = new Hashtable<String,Double>();
		aCompo.put("N", 2.958 / 100.); //On remplit une collection avec un put. Values from Haskell (2018) Alosa sapidissima (%)
		aCompo.put("P", 0.673 / 100.); //Values from Haskell (2018) Alosa sapidissima (%)
		aCompoCarcassPreSpawning.put(Gender.FEMALE,aCompo);

		aCompo = new Hashtable<String,Double>();
		aCompo.put("N", 2.941 / 100.); //Values from Haskell (2018) Alosa sapidissima (%)
		aCompo.put("P", 0.666 / 100.);// Values from Haskell (2018) Alosa sapidissima (%)
		aCompoCarcassPreSpawning.put(Gender.MALE,aCompo);

		System.out.println("aCompoCarcassPreSpawning: " + aCompoCarcassPreSpawning.toString()); //


		// Gametes composition approximated by the difference between gonads weight before and after spawning. 
		Map<Gender, Map<String, Double>> aCompoGametes = new Hashtable<DiadromousFish.Gender,Map<String,Double>>();
		aCompo = new Hashtable<String,Double>();
		aCompo.put("N", 3.242 / 100.); //On remplit une collection avec un put. From Haskel et al, 2018. 
		aCompo.put("P", 0.320 / 100.); // Haskel = %P, N, ici ratio donc divise par 100 
		aCompoGametes.put(Gender.FEMALE,aCompo);

		aCompo = new Hashtable<String,Double>();
		aCompo.put("N", 3.250 / 100.); // Approxim�e par la compo des gonades 
		aCompo.put("P", 0.724 / 100.);
		aCompoGametes.put(Gender.MALE,aCompo);

		System.out.println("aCompoGametes:" + aCompoGametes.toString()); //

		// features for juveniles 

		Map<String,Double> aJuvenileFeatures = new Hashtable<String, Double>();
		aJuvenileFeatures.put("bLW",3.0306);
		aJuvenileFeatures.put("aLW",Math.exp(-11.942) * Math.pow(10., aJuvenileFeatures.get("bLW")));

		System.out.println("aJuvenileFeatures: " + aJuvenileFeatures.toString()); 

		// carcass composition for juveniles fish 
		Map<String, Double> aCompoJuveniles = new Hashtable<String,Double>();
		aCompoJuveniles.put("N", 2.803 / 100.); //On remplit une collection avec un put. %N in wet weight (Haskell et al, 2017) on Alosa sapidissima 
		aCompoJuveniles.put("P", 0.887 / 100.); //%P in wet weight (from Haskell et al, 2017) on Alosa sapidissima 

		System.out.println("aCompoJuveniles: " + aCompoJuveniles.toString()); 

		ArrayList <String> nutrientsOfInterest= new ArrayList <String>();
		nutrientsOfInterest.add("N");
		nutrientsOfInterest.add("P");

		System.out.println("nutrientsOfInterest: " + nutrientsOfInterest);


		NutrientRoutineEssay fn = new NutrientRoutineEssay(nutrientsOfInterest,aResidenceTime, anExcretionRate, aFeaturePreSpawning, aFeaturePostSpawning, 
				aCompoCarcassPreSpawning, aCompoGametes, aJuvenileFeatures, aCompoJuveniles);

		SeaBasin basin = new SeaBasin(0,"Bidon",10.,12., 14.,12.); //il faut aller dans "SeaBasin" dans "environement et regarder comment est construit le constructeur. Il lui faut ici un rang, un nom de bassin versant, et des temp�rature pour chaque saison 
		Pilot pilot = new Pilot ();
		DiadromousFish fishFemale = new DiadromousFish (pilot, basin, 52., 1L, Gender.FEMALE); //Idem ici, on regarde comment est construit DiadromousFih et on lui donne les valeur de ce qu'il nous demande. 
		fishFemale.setStage(Stage.MATURE);
		DiadromousFish fishMale = new DiadromousFish (pilot, basin, 47., 1L, Gender.MALE); //Idem ici, on regarde comment est construit DiadromousFih et on lui donne les valeur de ce qu'il nous demande. 
		fishMale.setStage(Stage.MATURE);
		DiadromousFish juvenileFish = new DiadromousFish(pilot,basin,7.0,1L,Gender.UNDIFFERENCIED);
		juvenileFish.setStage(Stage.IMMATURE);

		System.out.println(); // affiche une ligne blanche 
		System.out.println(fishFemale.getGender() + ": " + fishFemale.getLength() + " cm " + fn.getWeight(fishFemale, SpawningPosition.PRE)+ " g " + fn.getWeight(fishFemale, SpawningPosition.POST));
		System.out.println("\tNutrients Fluxes for dead fish " + fn.computeNutrientsInputForDeadFish(fishFemale).toString());
		System.out.println("\tNutrients Fluxes for survival  " + fn.computeNutrientsInputForSurvivalAfterSpawning(fishFemale).toString());

		System.out.println(fishMale.getGender() + ": " + fishMale.getLength() + " cm " + fn.getWeight(fishMale, SpawningPosition.PRE)+ " g " + fn.getWeight(fishMale, SpawningPosition.POST));
		System.out.println("\tNutrients Fluxes for dead fish " + fn.computeNutrientsInputForDeadFish(fishMale).toString());
		System.out.println("\tNutrients Fluxes for survival  " + fn.computeNutrientsInputForSurvivalAfterSpawning(fishMale).toString());

		System.out.println(juvenileFish.getStage() + ": " + juvenileFish.getLength() + " cm " + fn.getWeight(juvenileFish)+ " g ");
		System.out.println("\tNutrients Fluxes for juveniles " + fn.computeNutrientsExportForJuveniles(juvenileFish).toString());


		/* Create XML file 
		 * 
		 */
		System.out.println((new	XStream(new DomDriver())).toXML(fn));

	} 
}


