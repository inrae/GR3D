package species;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.openide.util.lookup.ServiceProvider;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import environment.Basin;
import environment.BasinNetwork;
import environment.RiverBasin;
import fr.cemagref.observation.kernel.Observable;
import fr.cemagref.simaqualife.kernel.AquaNismsGroup;
import fr.cemagref.simaqualife.kernel.Processes;
import fr.cemagref.simaqualife.pilot.Pilot;
import miscellaneous.Duo;
import miscellaneous.TreeMapForCentile;
import species.DiadromousFish.Gender;
import species.DiadromousFish.Stage;

/**
 *
 */
@ServiceProvider(service = AquaNismsGroup.class)
public class DiadromousFishGroup extends AquaNismsGroup<DiadromousFish, BasinNetwork>
		implements Comparable<DiadromousFishGroup> {

	public String name = "species A";
	public Color color = Color.RED;

	/**
	 * distance maximum of dispersion
	 * 
	 * @unit km
	 */
	public double dMaxDisp = 300.;

	/**
	 * Routine to compute nutrient fluxes operated by a single individual (TODO by a single super individual).
	 * 
	 */
	private NutrientRoutine nutrientRoutine;

	public String fileNameInputForInitialObservation = "data/input/reality/Obs1900.csv";

	/**
	 * centile to calcucale the range of species distribution
	 * 
	 * @unit
	 */
	public double centileForRange = 0.95;

	/**
	 * file with the calibated parameters (from baysian approach)
	 * 
	 * @unit
	 */
	private String parameterSetfileName = "data/input/reality/parameterSet.csv";

	/**
	 * line to use in the calibrated parameters file
	 * 
	 * @unit
	 */
	private int parameterSetLine = 0;

	/**
	 * year when the update of the basin should occur
	 * 
	 * @unit
	 */
	private long yearOfTheUpdate;

	/**
	 * list of the basins to be updated
	 * column 1: name of the basin
	 * column 2: Pattractive: how the bassin become attractive (0 not attractive, 1 ??? normal weight associated to catchment size)
	 * column 3: Paccessible: how the bassin become acesssible (0 not accessible, 1 ???normal weight to inter catchment distance )
	 * @unit
	 */
	private String basinsToUpdateFile = "data/input/reality/basinsToUpdate.csv";

	private String outputPath = "data/output/";

	private long minYearToWrite = 1900;

	// TODO:remove fileNameFluxes
	private String fileNameFluxes = "fluxes";

	private transient BufferedWriter bWForFluxes;
	private transient String sep;

	/**
	 *  map
	 *  <key> basin name
	 *  <value> Duo
	 *  	<first>       pAttractive
	 *  	<second>  pAccessible
	 * @unit
	 */
	private transient Map<String, Duo<Double, Double>> basinsToUpdate;

	/**
	 * length when fish hatchs (when the diadromousFish is created after reproduction) no diffrence between gender
	 * 
	 * @unit cm
	 */
	private double lengthAtHatching = 2.;

	/**
	 * L infinity of the van Bertalanffy growth curve for  female
	 * L = Linf *(1-exp(-K*(t-t0))
	 * @unit cm
	 */
	public double linfVonBertForFemale = 60.;

	/**
	 * L infinity of the van Bertalanffy growth curve for  male
	 * L = Linf *(1-exp(-K*(t-t0))
         *
	 * @unit cm
	 */
	public double linfVonBertForMale = 60.;


	/**
	 * Brody growth coefficient of the von Bertalanffy growth curve for female (calculated from the parameterset file) *
	 * L = Linf *(1-exp(-K*(t-t0))
	 * 
	 * @unit year-1
	 */
	private transient double kOptForFemale;

	/**
	 * Brody growth coefficient of the von Bertalanffy growth curve for male (calculated from the parameterset file) * L
	 * = Linf *(1-exp(-K*(t-t0))
	 * 
	 * @unit year-1
	 */
	private transient double kOptForMale;

	/**
	 * length at first maturity. At that length the female become Stage.MATURE
	 * 
	 * @unit cm
	 */
	public double lFirstMaturityForFemale = 55.;

	/**
	 * length at first maturity. At that length the female become Stage.MATURE
	 * 
	 * @unit cm
	 */
	public double lFirstMaturityForMale = 40.;

	/**
	 * minimum temperature for the reproduction (from the parameterset file)
	 * 
	 * @unit °C
	 */
	private transient double tempMinRep; // parametre de reproduction

	/**
	 * list of the parameters provided by the calibration
	 * 
	 * @unit
	 */
	private transient List<Duo<Double, Double>> parameterSets;

	// =================================================
	// calibration
	// =================================================

	private Double targetedAgeForFemaleSpawnerForFirstTime = 5.5;
	private Double targetedAgeForMaleSpawnerForFirstTime = 4.5;

	/**
	 * map of observedoccurence in 1900
	 * 
	 * @unit
	 */
	private transient Map<String, Integer> obs1900;


	public static void main(String[] args) {
		DiadromousFishGroup diadromousFishGroup = new DiadromousFishGroup(new Pilot(), null, null);

		double aResidenceTime = 30;

		Map <String, Double> anExcretionRate = new Hashtable <String, Double>(); 
		anExcretionRate.put("N", 24.71E-6 * 24); //values from Barber et al, Alosa sapidissima in ug/g wet mass/hour : convertit en g
		anExcretionRate.put("P", 2.17E-6 * 24); //values from Barber et al, Alosa sapidissima in ug/g wet mass/hour: convertit en g

		/*
		 * A feature pre spawning
		 */
		Map<DiadromousFish.Gender, Map<String, Double>> aFeaturePreSpawning = new Hashtable<DiadromousFish.Gender, Map<String, Double>>();

		/*
		 * For females
		 */
		Map<String, Double> aFeature = new Hashtable<String, Double>();

		aFeature.put("aLW", Math.exp(-4.9078)); // weight size relationship computed from BDalosesBruch
		aFeature.put("bLW", 3.147);
		// aFeature.put("bLW",3.3429);// parametre "b" de la relation taille/poids - Coefficient d'allometrie
		// aFeature.put("aLW",1.2102E-6 * Math.pow(10., aFeature.get("bLW"))); // parametre "a" de la relation
		// taille/poids en kg/cm- Traduit la condition
		// aFeature.put("GSI",0.15);
		aFeaturePreSpawning.put(Gender.FEMALE, aFeature);

		/*
		 * For males
		 */
		aFeature = new Hashtable<String, Double>();
		aFeature.put("aLW", Math.exp(-1.304));
		aFeature.put("bLW", 2.1774);
		// aFeature.put("aLW",2.4386E-6 * Math.pow(10, aFeature.get("bLW"))); // Conversion des g/mm en g.cm (from
		// Taverny, 1991)
		// aFeature.put("GSI",.08);
		aFeaturePreSpawning.put(Gender.MALE, aFeature);

		/*
		 * a Feature post Spawning
		 */
		Map<DiadromousFish.Gender, Map<String, Double>> aFeaturePostSpawning = new Hashtable<DiadromousFish.Gender, Map<String, Double>>();

		/*
		 * For females
		 */
		aFeature = new Hashtable<String, Double>();
		aFeature.put("aLW", Math.exp(-4.3276)); // weight size relationship computed from BDalosesBruch
		aFeature.put("bLW", 2.9418);
		// aFeature.put("GSI",0.10); //From BDalosesBruch
		// aFeature.put("aLW",aFeaturePreSpawning.get(Gender.FEMALE).get("aLW")/(1+aFeature.get("GSI"))); // parametre
		// "a" de la relation taille/poids avec Lt en cm - Traduit la condition
		// aFeature.put("bLW",aFeaturePreSpawning.get(Gender.FEMALE).get("bLW"));// parametre "b" de la relation
		// taille/poids - Coefficient d'allometrie
		aFeaturePostSpawning.put(Gender.FEMALE, aFeature);

		/*
		 * For males
		 */
		aFeature = new Hashtable<String, Double>();

		aFeature.put("aLW", Math.exp(-4.5675));// parametre "a" de la relation taille/poids - Coefficient d'allometrie
		aFeature.put("bLW", 2.9973);
		// aFeature.put("GSI",.05); From BDalosesBruch
		// aFeature.put("aLW",aFeaturePreSpawning.get(Gender.MALE).get("aLW")/(1+aFeature.get("GSI")));
		// aFeature.put("bLW",aFeaturePreSpawning.get(Gender.MALE).get("bLW"));
		aFeaturePostSpawning.put(Gender.MALE, aFeature);

		Map<DiadromousFish.Gender, Double> aGameteSpawned = new Hashtable<DiadromousFish.Gender, Double>();
		aGameteSpawned.put(Gender.FEMALE, 131.); // Compute from the difference between spawned and unspawned ovaries ie
		// correspond to a mean weight of eggs spawned
		aGameteSpawned.put(Gender.MALE, 44.8); // Compute from the difference between spawned and unspawned testes ie
		// correspond to a mean weight of sperm spawned

		// carcass composition for fish before spawning
		Map<DiadromousFish.Gender, Map<String, Double>> aCompoCarcassPreSpawning = new Hashtable<DiadromousFish.Gender, Map<String, Double>>();
		Map<String, Double> aCompo = new Hashtable<String, Double>();
		aCompo.put("N", 2.958 / 100.); // On remplit une collection avec un put.
		aCompo.put("P", 0.673 / 100.);
		aCompoCarcassPreSpawning.put(Gender.FEMALE, aCompo);

		aCompo = new Hashtable<String, Double>();
		aCompo.put("N", 2.941 / 100.);
		aCompo.put("P", 0.666 / 100.);
		aCompoCarcassPreSpawning.put(Gender.MALE, aCompo);

		// carcass composition for fish after spawning
		Map<DiadromousFish.Gender, Map<String, Double>> aCompoCarcassPostSpawning = new Hashtable<DiadromousFish.Gender, Map<String, Double>>();
		aCompo = new Hashtable<String, Double>();
		aCompo.put("N", 3.216 / 100.); // On remplit une collection avec un put.
		aCompo.put("P", 0.997 / 100.);
		aCompoCarcassPostSpawning.put(Gender.FEMALE, aCompo);

		aCompo = new Hashtable<String, Double>();
		aCompo.put("N", 2.790 / 100.); // From Haskel et al, 2017
		aCompo.put("P", 0.961 / 100.);
		aCompoCarcassPostSpawning.put(Gender.MALE, aCompo);

		// Gametes composition approximated by the difference between gonads weight before and after spawning.
		Map<DiadromousFish.Gender, Map<String, Double>> aCompoGametes = new Hashtable<DiadromousFish.Gender, Map<String, Double>>();
		aCompo = new Hashtable<String, Double>();
		aCompo.put("N", 3.242 / 100.); // On remplit une collection avec un put. From Haskel et al, 2018.
		aCompo.put("P", 0.320 / 100.); // Haskel = %P, N, ici ratio donc divise par 100
		aCompoGametes.put(Gender.FEMALE, aCompo);

		aCompo = new Hashtable<String, Double>();
		aCompo.put("N", 3.250 / 100.);
		aCompo.put("P", 0.724 / 100.);
		aCompoGametes.put(Gender.MALE, aCompo);

		// features for juveniles

		Map<String, Double> aJuvenileFeatures = new Hashtable<String, Double>();
		aJuvenileFeatures.put("bLW", 3.0306);
		aJuvenileFeatures.put("aLW", Math.exp(-11.942) * Math.pow(10., aJuvenileFeatures.get("bLW")));

		// carcass composition for juveniles fish
		Map<String, Double> aCompoJuveniles = new Hashtable<String, Double>();
		aCompoJuveniles.put("N", 2.803 / 100.); // On remplit une collection avec un put. %N in wet weight (Haskell et
		// al, 2017) on Alosa sapidissima
		aCompoJuveniles.put("P", 0.887 / 100.); // %P in wet weight (from Haskell et al, 2017) on Alosa sapidissima

		ArrayList<String> nutrientsOfInterest = new ArrayList<String>();
		nutrientsOfInterest.add("N");
		nutrientsOfInterest.add("P");

		diadromousFishGroup.nutrientRoutine = new NutrientRoutine(nutrientsOfInterest, aResidenceTime, anExcretionRate,
				aFeaturePreSpawning, aFeaturePostSpawning, aCompoCarcassPreSpawning, aCompoGametes, aJuvenileFeatures,
				aCompoJuveniles);

		System.out.println((new XStream(new DomDriver())).toXML(diadromousFishGroup));
	}


	public DiadromousFishGroup(Pilot pilot, BasinNetwork environment, Processes processes) {
		super(pilot, environment, processes);
	}


	public DiadromousFishGroup() {
		super();
	}


	public double getPattractive(String basinName) {
		// TODO pass in argument a Basin
		// remove "-s" of the sea basin name
		String shortBasinName = basinName.substring(0, basinName.length() - 2);
		if (basinsToUpdate.containsKey(shortBasinName))
			return basinsToUpdate.get(shortBasinName).getFirst();
		else
			return Double.NaN;
	}


	public double getPaccessible(String basinName) {
		// TODO pass in argument a Basin
		// WHY not a short name
		if (basinsToUpdate.containsKey(basinName))
			return basinsToUpdate.get(basinName).getSecond();
		else
			return Double.NaN;
	}


	/**
	 * @return the yearOfTheUpdate
	 */
	public long getYearOfTheUpdate() {
		return yearOfTheUpdate;
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see fr.cemagref.simaqualife.kernel.AquaNismsGroup#initTransientParameters(fr.cemagref.simaqualife.pilot.Pilot)
	 */
	@Override
	public void initTransientParameters(Pilot pilot)
			throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		super.initTransientParameters(pilot);

		// basin to be updated
		if (basinsToUpdate != null) {
			String subDir = basinsToUpdateFile;
			if (basinsToUpdateFile.lastIndexOf("/") != -1)
				subDir = basinsToUpdateFile.substring(basinsToUpdateFile.lastIndexOf("/") + 1,
						basinsToUpdateFile.length());
			if (subDir.lastIndexOf(".") != -1)
				subDir = subDir.substring(0, subDir.lastIndexOf("."));
			outputPath = outputPath.concat(subDir).concat("/");
			System.out.println(outputPath);

			basinsToUpdate = new HashMap<String, Duo<Double, Double>>();
			FileReader reader;
			Scanner scanner;
			String basins;
			double pAttractive;
			double pAccessible;
			try {
				// open the file
				reader = new FileReader(basinsToUpdateFile);
				// Parsing the file
				scanner = new Scanner(reader);
				scanner.useLocale(Locale.ENGLISH); // to have a comma as decimal separator !!!
				scanner.useDelimiter(Pattern.compile("[;\r]"));

				scanner.nextLine();
				while (scanner.hasNext()) {
					basins = scanner.next();
					if (basins != null) {
						pAttractive = scanner.nextDouble();
						pAccessible = scanner.nextDouble();
						scanner.nextLine();
						Duo<Double, Double> duo = new Duo<Double, Double>(pAttractive, pAccessible);
						basinsToUpdate.put(basins, duo);
					}
				}
				reader.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// charge kopt et temMinRep depuis le fichier de parametre. Sinon (parameterSetLine<=0), ce sont les
		// valeurs dans le processus de reproduction qui sont utilis�
		kOptForFemale = Double.NaN;
		kOptForMale = Double.NaN;
		tempMinRep = Double.NaN;
		if (parameterSetLine > 0) {
			parameterSets = new ArrayList<Duo<Double, Double>>(10);

			// open the file
			FileReader reader1;
			Scanner scanner1;

			try {
				reader1 = new FileReader(parameterSetfileName);
				// Parsing the file
				scanner1 = new Scanner(reader1);
				scanner1.useLocale(Locale.ENGLISH); // to have a comma as decimal separator !!!
				scanner1.useDelimiter(Pattern.compile("[;\r]"));

				scanner1.nextLine(); // skip the first line
				while (scanner1.hasNext()) {
					String rien = scanner1.next(); // skip id
					// System.out.println(rien.compareTo("\n"));
					if (rien.compareTo("\n") != 0) {
						Duo<Double, Double> duo = new Duo<Double, Double>(scanner1.nextDouble(), scanner1.nextDouble());
						// System.out.println(duo.toString());
						parameterSets.add(duo);
					}
				}

				scanner1.close();
				reader1.close();
			} catch (Exception e) {
				e.printStackTrace();
			}

			double kOpt = parameterSets.get(parameterSetLine - 1).getFirst();
			// 40 correspond to the lFirstMaturity used by Rougier to calibrate the model
			kOptForMale = kOpt * lFirstMaturityForMale / 40.;
			kOptForFemale = kOpt * lFirstMaturityForFemale / 40.;
			tempMinRep = parameterSets.get(parameterSetLine - 1).getSecond();
		}

		// open an buffered writer to export fluxes
		if (!fileNameFluxes.isEmpty()) {
			sep = ";";
			new File(this.outputPath + fileNameFluxes).getParentFile().mkdirs();
			try {
				bWForFluxes = new BufferedWriter(
						new FileWriter(new File(this.outputPath + fileNameFluxes + this.getSimulationId() + ".csv")));

				bWForFluxes.write("timestep" + sep + "year" + sep + "season" + sep + "basin" + sep + "abundance" + sep
						+ "fluxType" + sep + "origin" + sep + "abundanceExp" + sep + "biomass");
				for (String nutrient : nutrientRoutine.getNutrientsOfInterest()) {
					bWForFluxes.write(sep + nutrient);
				}
				bWForFluxes.write("\n");

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		// ------------------------------------------------------------------
		// read occurence data file
		// -----------------------------------------------------------
		FileReader reader;
		Scanner scanner;

		obs1900 = new HashMap<String, Integer>();
		try {
			reader = new FileReader(fileNameInputForInitialObservation);
			// Parsing the file
			scanner = new Scanner(reader);
			scanner.useLocale(Locale.ENGLISH); // to have a comma as decimal separator !!!
			scanner.useDelimiter(Pattern.compile("[;\r]"));

			scanner.nextLine(); // to skip the file first line of headers

			while (scanner.hasNextLine()) {
				String[] fields = scanner.nextLine().split(",");
				// System.out.println(Arrays.toString(fields));
				if (fields.length > 2) {
					if (!fields[2].isEmpty())
						obs1900.put(fields[1], Integer.valueOf(fields[2]));
				}
			}

			reader.close();
			scanner.close();

		} catch (IOException ex) {
			Logger.getLogger(DiadromousFishGroup.class.getName()).log(Level.SEVERE, null, ex);
		}

		// defaut values
		if (targetedAgeForFemaleSpawnerForFirstTime == null)
			targetedAgeForFemaleSpawnerForFirstTime = 5.5;
		if (targetedAgeForMaleSpawnerForFirstTime == null)
			targetedAgeForMaleSpawnerForFirstTime = 4.5;

		// this.nutrientRoutine.getNutrientFluxesCollection().setBasinNames(this.getEnvironment().getRiverBasinNames());
		this.nutrientRoutine.createNutrienFluxesCollections(this.getEnvironment().getRiverBasinNames());
	}


	/**
	 * @param fish
	 * @param group
	 * @return the Brody coeff
	 */
	public double getKOpt(DiadromousFish fish) {
		double kOpt = 0.;

		if (fish.getGender() == Gender.FEMALE)
			kOpt = kOptForFemale;
		else if (fish.getGender() == Gender.MALE)
			kOpt = kOptForMale;
		else
			kOpt = (kOptForFemale + kOptForMale) / 2.;

		return kOpt;
	}


	public BufferedWriter getbWForFluxes() {
		return bWForFluxes;
	}


	public double getTempMinRep() {
		return tempMinRep;
	}


	@Observable(description = "Nb of SI")
	public int getNbSI() {
		int nbSI = 0;
		for (Basin basin : this.getEnvironment().getBasins()) {
			if (basin.getFishs(this) != null)
				nbSI += basin.getFishs(this).size();
		}
		return nbSI;
	}


	@Observable(description = "Sizes mean of SI")
	public double getSizesMeanOfSI() {
		double totalEffective = 0;
		double nbSI = 0;
		for (Basin basin : this.getEnvironment().getBasins()) {
			if (basin.getFishs(this) != null) {
				nbSI += basin.getFishs(this).size();
				for (DiadromousFish superFish : basin.getFishs(this)) {
					totalEffective += superFish.getAmount();
				}
			}
		}
		if (nbSI >= 0)
			return totalEffective / nbSI;
		else
			return Double.NaN;
	}


	@Observable(description = "# of SI with ind < 10")
	public double getNbLittleSI() {
		double nb = 0;
		for (Basin basin : this.getEnvironment().getBasins()) {
			if (basin.getFishs(this) != null) {
				for (DiadromousFish superFish : basin.getFishs(this)) {
					if (superFish.getAmount() < 10L)
						nb++;
				}
			}
		}
		return nb;
	}


	public double getMeanLengthOfMatureFish() {
		double sumOfLength = 0.;
		double numberOfMatureFish = 0.;
		for (Basin basin : this.getEnvironment().getBasins()) {
			if (basin.getFishs(this) != null) {
				for (DiadromousFish fish : basin.getFishs(this)) {
					if (fish.isMature()) {
						sumOfLength += fish.getAmount() * fish.getLength();
						numberOfMatureFish += fish.getAmount();
					}
				}
			}
		}
		return sumOfLength / numberOfMatureFish;
	}


	public double getStandardDeviationOfMatureFishLength() {
		double standardDeviationOfMatureFishLength = 0.;
		double sumOfSquareLength = 0.;
		double numberOfMatureFish = 0.;
		double meanOfSquareLengthOfMatureFish = 0.;
		for (Basin basin : this.getEnvironment().getBasins()) {
			if (basin.getFishs(this) != null) {
				for (DiadromousFish fish : basin.getFishs(this)) {
					if (fish.isMature()) {
						sumOfSquareLength += fish.getAmount() * fish.getLength() * fish.getLength();
						numberOfMatureFish += fish.getAmount();
					}
				}
			}
			meanOfSquareLengthOfMatureFish = sumOfSquareLength / numberOfMatureFish;
			double meanLength = getMeanLengthOfMatureFish();
			standardDeviationOfMatureFishLength = Math.sqrt(meanOfSquareLengthOfMatureFish - meanLength * meanLength);
		}
		return standardDeviationOfMatureFishLength;
	}


	public String getName() {
		return name;
	}


	public Color getColor() {
		return color;
	}


	public double getLinfVonBert(DiadromousFish fish) {
		if (fish.getGender() == Gender.FEMALE)
			return linfVonBertForFemale;
		else if (fish.getGender() == Gender.MALE)
			return linfVonBertForMale;
		else
			return (linfVonBertForFemale + linfVonBertForMale) / 2.;
	}


	public double getlFirstMaturity(DiadromousFish fish) {
		if (fish.getGender() == Gender.FEMALE)
			return lFirstMaturityForFemale;
		else if (fish.getGender() == Gender.MALE)
			return lFirstMaturityForMale;
		else
			return (lFirstMaturityForFemale + lFirstMaturityForFemale) / 2.;
	}


	/**
	 * @return the lengthAtHatching
	 */
	public double getLengthAtHatching() {
		return lengthAtHatching;
	}


	public double getdMaxDisp() {
		return dMaxDisp;
	}


	public NutrientRoutine getNutrientRoutine() {
		return nutrientRoutine;
	}


	/**
	 * @return the minYearToWrite
	 */
	public long getMinYearToWrite() {
		return minYearToWrite;
	}


	// ================================================================
	// statictis for calibration
	// ================================================================
	public double computeFemaleSpawnerForFirstTimeSummaryStatisticWithTarget(double TARGET) {
		double sum = 0;
		for (RiverBasin riverBasin : getEnvironment().getRiverBasins()) {
			if (riverBasin.getSpawnersForFirstTimeMeanAges(Gender.FEMALE).getMeanWithoutZero() > 0.) {
				double val = riverBasin.getSpawnersForFirstTimeMeanAges(Gender.FEMALE).getMeanWithoutZero() - TARGET;
				sum += val * val;
			}
		}
		// System.out.println("sum female: " + sum);
		return sum;
	}


	@Observable(description = "Female spawners For First Time Summary Statistic")
	public double computeFemaleSpawnerForFirstTimeSummaryStatistic() {
		return computeFemaleSpawnerForFirstTimeSummaryStatisticWithTarget(targetedAgeForFemaleSpawnerForFirstTime);
	}


	@Observable(description = "mean age at first reproduction for female")
	public double getMeanAgeOfFirstReprodutionForFemale() {
		double sum = 0;
		double nb = 0;
		for (RiverBasin riverBasin : getEnvironment().getRiverBasins()) {
			if (riverBasin.getSpawnersForFirstTimeMeanAges(Gender.FEMALE).getMeanWithoutZero() > 0.) {
				nb++;
				sum += riverBasin.getSpawnersForFirstTimeMeanAges(Gender.FEMALE).getMeanWithoutZero();
			}
		}
		return sum / nb;
	}


	@Observable(description = "mean length of first reprodution for female")
	public double getMeanLengthOfFirstReprodutionForFemale() {
		double sum = 0;
		double nb = 0;
		for (RiverBasin riverBasin : getEnvironment().getRiverBasins()) {
			if (riverBasin.getSpawnersForFirstTimeMeanLengths(Gender.FEMALE).getMeanWithoutZero() > 0.) {
				nb++;
				sum += riverBasin.getSpawnersForFirstTimeMeanLengths(Gender.FEMALE).getMeanWithoutZero();
			}
		}
		return sum / nb;
	}


	public double computeMaleSpawnerForFirstTimeSummaryStatisticWithTarget(double TARGET) {
		double sum = 0;
		for (RiverBasin riverBasin : getEnvironment().getRiverBasins()) {
			if (riverBasin.getSpawnersForFirstTimeMeanAges(Gender.MALE).getMeanWithoutZero() > 0.) {
				double val = riverBasin.getSpawnersForFirstTimeMeanAges(Gender.MALE).getMeanWithoutZero() - TARGET;
				sum += val * val;
			}
		}
		// System.out.println("sum male: " + sum);
		return sum;
	}


	@Observable(description = "Male spawners For First Time Summary Statistic")
	public double computeMaleSpawnerForFirstTimeSummaryStatistic() {
		return computeMaleSpawnerForFirstTimeSummaryStatisticWithTarget(targetedAgeForFemaleSpawnerForFirstTime);
	}


	@Observable(description = "mean age of first reprodution for male")
	public double getMeanAgeOfFirstReprodutionForMale() {
		double sum = 0;
		double nb = 0;
		for (RiverBasin riverBasin : getEnvironment().getRiverBasins()) {
			if (riverBasin.getSpawnersForFirstTimeMeanAges(Gender.MALE).getMeanWithoutZero() > 0.) {
				nb++;
				sum += riverBasin.getSpawnersForFirstTimeMeanAges(Gender.MALE).getMeanWithoutZero();
			}
		}
		return sum / nb;
	}


	@Observable(description = "mean length of first reprodution for male")
	public double getMeanLengthOfFirstReprodutionForMale() {
		double sum = 0;
		double nb = 0;
		for (RiverBasin riverBasin : getEnvironment().getRiverBasins()) {
			if (riverBasin.getSpawnersForFirstTimeMeanLengths(Gender.MALE).getMeanWithoutZero() > 0.) {
				nb++;
				sum += riverBasin.getSpawnersForFirstTimeMeanLengths(Gender.MALE).getMeanWithoutZero();
			}
		}
		return sum / nb;
	}


	@Observable(description = "Likelihood Summary stat")
	public double computeLikelihood() {
		int obsVal;
		double sumLogWherePres = 0.;
		double sumLogWhereAbs = 0.;
		final double[] probOfNonNulRecruitmentDuringLastYears = getEnvironment()
				.getProbOfNonNulRecruitmentDuringLastYears();
		final String[] finalStatesNames = getEnvironment().getRiverBasinNames();
		for (int i = 0; i < finalStatesNames.length; i++) {
			if (obs1900.containsKey(finalStatesNames[i])) {
				obsVal = obs1900.get(finalStatesNames[i]);
				if (obsVal == 0) {
					sumLogWhereAbs += Math.log(1 - probOfNonNulRecruitmentDuringLastYears[i]);
				} else {
					sumLogWherePres += Math.log(probOfNonNulRecruitmentDuringLastYears[i]);
				}
			}
		}

		return sumLogWhereAbs + sumLogWherePres;
	}


	// ========================================================
	// obeserver to explore the distribution
	// ========================================================
	@Observable(description = "Higher Populated Latitude")
	public double getHigherPopulatedLatitude() {
		double latitude = 0.0;
		RiverBasin[] basins = getEnvironment().getRiverBasins();
		int[] finalStates = getEnvironment().getFinalStates();
		for (int i = 0; i < finalStates.length; i++) {
			if ((finalStates[i] == 1) && (basins[i].getLatitude() > latitude)) {
				latitude = basins[i].getLatitude();
			}

		}
		return latitude;
	}


	@Observable(description = "Number of colonized basins")
	public double getNbColonizedBasins() {
		int nb = 0;
		for (Basin seaBasin : getEnvironment().getSeaBasins()) {
			if (seaBasin.getFishs(this) != null) {
				if (!seaBasin.getFishs(this).isEmpty()) {
					nb++;
				}
			}
		}
		return nb;
	}


	@Observable(description = "Northern colonized basins")
	public double getNorthernBasins() {
		int northernBasin = Integer.MAX_VALUE;
		for (Basin seaBasin : getEnvironment().getSeaBasins()) {
			if (seaBasin.getFishs(this) != null) {
				if (!seaBasin.getFishs(this).isEmpty()) {
					northernBasin = Math.min(northernBasin, getEnvironment().getAssociatedRiverBasin(seaBasin).getId());
				}
			}
		}
		return northernBasin;
	}


	@Observable(description = "Southern colonized basins")
	public double getSouthernBasins() {
		int southernBasin = Integer.MIN_VALUE;
		for (Basin seaBasin : getEnvironment().getSeaBasins()) {
			if (seaBasin.getFishs(this) != null) {
				if (!seaBasin.getFishs(this).isEmpty()) {
					southernBasin = Math.max(southernBasin, getEnvironment().getAssociatedRiverBasin(seaBasin).getId());
				}
			}
		}
		return southernBasin;
	}


	@Observable(description = "Range distribution with latitude")
	public Double[] getRangeDistributionWithLat() {
		// TODO keep the extreme latitudes from the catchment
		double southernBasin = 35.;
		double northernBasin = 60.;
		RiverBasin riverBasin;
		TreeMapForCentile latitudeEffective = new TreeMapForCentile();
		for (Basin seaBasin : getEnvironment().getSeaBasins()) {
			if (seaBasin.getFishs(this) != null) {
				if (!seaBasin.getFishs(this).isEmpty()) {
					riverBasin = (RiverBasin) getEnvironment().getAssociatedRiverBasin(seaBasin);
					long effective = 0;
					for (DiadromousFish fish : seaBasin.getFishs(this)) {
						effective += fish.getAmount();
					}
					southernBasin = Math.max(southernBasin, riverBasin.getLatitude());
					latitudeEffective.putWithAdding(riverBasin.getLatitude(), effective);
					northernBasin = Math.min(northernBasin, riverBasin.getLatitude());
				}
			}
		}

		Double[] rangeDistribution = new Double[4];

		rangeDistribution[0] = (latitudeEffective.isEmpty() ? (southernBasin + northernBasin) / 2.
				: latitudeEffective.calculateMedian());
		rangeDistribution[1] = Math.min(southernBasin, northernBasin);
		rangeDistribution[2] = Math.max(southernBasin, northernBasin);
		rangeDistribution[3] = latitudeEffective.calculateCentile(centileForRange);
		return rangeDistribution;
	}


	@Observable(description = "Range distribution")
	public Double[] getRangeDistribution() {
		double southernBasin = 0;
		double nbBasin = getEnvironment().getNbBasin();
		double northernBasin = nbBasin;
		Basin riverBasin;
		TreeMapForCentile latitudeEffective = new TreeMapForCentile();
		for (Basin seaBasin : getEnvironment().getSeaBasins()) {
			if (seaBasin.getFishs(this) != null) {
				if (!seaBasin.getFishs(this).isEmpty()) {
					riverBasin = getEnvironment().getAssociatedRiverBasin(seaBasin);
					long effective = 0;
					for (DiadromousFish fish : seaBasin.getFishs(this)) {
						effective += fish.getAmount();
					}
					latitudeEffective.putWithAdding(riverBasin.getId(), effective);
					southernBasin = Math.max(southernBasin, riverBasin.getId());
					northernBasin = Math.min(northernBasin, riverBasin.getId());
				}
			}
		}
		southernBasin = nbBasin - southernBasin;
		northernBasin = nbBasin - northernBasin;
		Double[] rangeDistribution = new Double[3];
		rangeDistribution[0] = (latitudeEffective.isEmpty() ? (southernBasin + northernBasin) / 2.
				: nbBasin - latitudeEffective.calculateMedian());
		rangeDistribution[1] = Math.min(southernBasin, northernBasin);
		rangeDistribution[2] = Math.max(southernBasin, northernBasin);

		return rangeDistribution;
	}


	/**
	 * @return sum of effectives in all the river basins
	 */
	@Observable(description = "Number of fishes in river basin")
	public double getFishEffective() {
		long eff = 0;
		for (RiverBasin basin : this.getEnvironment().getRiverBasins()) {
			if (basin.getFishs(this) != null) {
				for (DiadromousFish fish : basin.getFishs(this)) {
					eff += fish.getAmount();
				}
			}
		}
		return eff;
	}


	/**
	 * @return sum of spawner effectives in all the river basins
	 */
	@Observable(description = "Number of spawners in river basins")
	public double getSpawnerEffective() {
		long eff = 0;
		for (RiverBasin basin : this.getEnvironment().getRiverBasins()) {
			if (basin.getFishs(this) != null) {
				for (DiadromousFish fish : basin.getFishs(this)) {
					if (fish.getStage() == Stage.MATURE)
						eff += fish.getAmount();
				}
			}
		}
		return eff;
	}


	/**
	 * @return sum of male spawner effectives in all the river basins
	 */
	@Observable(description = "Number of male spawners in all basins")
	public double getMaleSpawnerEffective() {
		long eff = 0;
		for (Basin basin : this.getEnvironment().getBasins()) {
			if (basin.getFishs(this) != null) {
				for (DiadromousFish fish : basin.getFishs(this)) {
					if (fish.getStage() == Stage.MATURE && fish.getGender() == Gender.MALE)
						eff += fish.getAmount();
				}
			}
		}
		return eff;
	}


	/**
	 * @return sum of female spawner effectives in all the river basins
	 */
	@Observable(description = "Number of female spawners in all basins")
	public double getFemaleSpawnerEffective() {
		long eff = 0;
		for (Basin basin : this.getEnvironment().getBasins()) {
			if (basin.getFishs(this) != null) {
				for (DiadromousFish fish : basin.getFishs(this)) {
					if (fish.getStage() == Stage.MATURE && fish.getGender() == Gender.FEMALE)
						eff += fish.getAmount();
				}
			}
		}
		return eff;
	}


	@Override
	public void addAquaNism(DiadromousFish fish) {
		// avoid utilisation of global fishes list
		// super.addAquaNism(fish);
		fish.getPosition().addFish(fish, this);
	}


	@Override
	public void removeAquaNism(DiadromousFish fish) {
		// avoid utilisation of global fishes list
		// super.removeAquaNism(fish);
		fish.getPosition().removeFish(fish, this);
	}


	@Override
	public int compareTo(DiadromousFishGroup t) {
		return name.compareTo(t.name);
	}


	/**
	 * 
	 * concat at RngSatusIndex, temperatureCatchmentFile
	 * 
	 * @return simulation name
	 */
	public String getSimulationId() {
		String id = "_";
		id = id.concat(Integer.toString(getPilot().getParameters().getRngStatusIndex()));
		String temperatureFile = ((BasinNetwork) getPilot().getAquaticWorld().getEnvironment())
				.getTemperatureCatchmentFile();
		id = id.concat("-")
				.concat(temperatureFile.substring(temperatureFile.length() - 9, temperatureFile.length() - 4));
		if (parameterSetLine > 0) {
			id = id.concat("-").concat(Integer.toString(parameterSetLine));
		}
		return id;
	}


	public boolean isThereBasinToUpdate() {
		return basinsToUpdate != null;
	}


	/**
	 * @return the outputPath
	 */
	public String getOutputPath() {
		return outputPath;
	}

}
