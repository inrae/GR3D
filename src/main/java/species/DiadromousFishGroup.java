package species;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import environment.Basin;
import environment.BasinNetwork;
import environment.BasinNetworkReal;
import environment.RiverBasin;
import fr.cemagref.observation.kernel.Observable;
import fr.cemagref.simaqualife.kernel.AquaNismsGroup;
import fr.cemagref.simaqualife.kernel.Processes;
import fr.cemagref.simaqualife.pilot.Pilot;

import java.awt.Color;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import miscellaneous.Duo;
import miscellaneous.TreeMapForCentile;

import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = AquaNismsGroup.class)
public class DiadromousFishGroup extends AquaNismsGroup< DiadromousFish, BasinNetwork> implements Comparable<DiadromousFishGroup> {

	public String name = "species A";
	public Color color = Color.RED;

	/**
	 * L infinity of the van Bertalanffy growth curve
	 * L = Linf *(1-exp(-K*(t-t0))
	 * @unit cm
	 */
	public double linfVonBert = 60.;

	/**
	 *  ????
	 * @unit
	 */
	public double dMaxDisp = 300.;
	
	/**
	 *  length at first maturity. At that length the fish become Stage.MATURE
	 * @unit cm
	 */
	public double lFirstMaturity = 40.;
	
	public String fileNameInputForInitialObservation = "data/input/reality/Obs1900.csv";

	/**
	 *  centile to calucale the range of species distribution
	 * @unit
	 */
	public double centileForRange = 0.95;

	/**
	 * file with the calibated parameters (from baysian approach)
	 * @unit
	 */
	private String parameterSetfileName= "data/input/reality/parameterSet.csv";

	/**
	 *  line to use in the calibrated parameters file
	 * @unit
	 */
	private int parameterSetLine =0;

	/**
	 *  year when the update of the basin should occur
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
	 * Brody growth coefficient of the von Bertalanffy growth curve (from the parameterset file)
	 *  	 * L = Linf *(1-exp(-K*(t-t0))
	 * @unit year-1
	 */
	private transient double kOpt; 
	/**
	 * minimum temperature for the reproduction (from the parameterset file)
	 * @unit °C
	 */
	private transient double tempMinRep; //parametre de reproduction


	/**
	 * list of the parameters provided by the calibration
	 * @unit
	 */
	private transient List<Duo<Double, Double>> parameterSets;

	public static void main(String[] args) {
		System.out.println((new XStream(new DomDriver())).toXML(new DiadromousFishGroup(new Pilot(), null, null)));
	}

	public DiadromousFishGroup(Pilot pilot, BasinNetwork environment, Processes processes) {
		super(pilot, environment, processes);
	}


	public double getPattractive(String basinName){
		// TODO pass in argument a Basin
		// remove "-s" of the sea basin name
		String shortBasinName = basinName.substring(0, basinName.length()-2);
		if (basinsToUpdate.containsKey(shortBasinName))
			return basinsToUpdate.get(shortBasinName).getFirst();
		else
			return Double.NaN;
	}

	public double getPaccessible(String basinName){
		// TODO pass in argument a Basin
		//WHY not a short name
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

	/* (non-Javadoc)
	 * @see fr.cemagref.simaqualife.kernel.AquaNismsGroup#initTransientParameters(fr.cemagref.simaqualife.pilot.Pilot)
	 */
	@Override
	public void initTransientParameters(Pilot pilot)
			throws IllegalArgumentException, IllegalAccessException,
			InvocationTargetException {
		super.initTransientParameters(pilot);

		// basin to be updated
		if ( basinsToUpdate != null){
			String subDir=basinsToUpdateFile;
			if (basinsToUpdateFile.lastIndexOf("/")!=-1)
				subDir=basinsToUpdateFile.substring(basinsToUpdateFile.lastIndexOf("/")+1,
						basinsToUpdateFile.length());
			if (subDir.lastIndexOf(".")!=-1)
				subDir=subDir.substring(0, subDir.lastIndexOf("."));
			outputPath= outputPath.concat(subDir).concat("/");
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
					if (basins!= null) {
						pAttractive = scanner.nextDouble();
						pAccessible = scanner.nextDouble();
						scanner.nextLine();
						Duo<Double, Double> duo=new Duo<Double, Double>(pAttractive, pAccessible);
						basinsToUpdate.put(basins, duo);
					}
				}
				reader.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// charge kopt et temMinRep depuis le fichier de parametre. Sinon (parameterSetLine<=0), ce sont les 
		// valeur dasn le procoessus de reroduction qui sont utilis�
		kOpt=Double.NaN;
		tempMinRep =Double.NaN;
		if (parameterSetLine>0){
			parameterSets = new ArrayList<Duo<Double,Double>>(10);

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
					String rien= scanner1.next(); // skip id
					//System.out.println(rien.compareTo("\n"));
					if(rien.compareTo("\n")!=0){
						Duo<Double, Double> duo=new Duo<Double, Double>(scanner1.nextDouble(), scanner1.nextDouble());
						//System.out.println(duo.toString());
						parameterSets.add(duo);
					}
				}

				scanner1.close();
				reader1.close();
			} catch (Exception e) {
				e.printStackTrace();
			}

			kOpt = parameterSets.get(parameterSetLine-1).getFirst();
			tempMinRep = parameterSets.get(parameterSetLine-1).getSecond();
		}
	}

	public double getKOpt(){
		return kOpt;
	}

	public double getTempMinRep(){
		return tempMinRep;
	}

	@Observable(description = "Nb of SI")
	public int getNbSI() {
		int nbSI = 0;
		for (Basin basin : this.getEnvironment().getBasins() ) {
			if (basin.getFishs(this) != null) 
				nbSI += basin.getFishs(this).size();
		}
		return nbSI;
	}

	@Observable(description = "Sizes mean of SI")
	public double getSizesMeanOfSI() {
		double totalEffective = 0;
		double nbSI =0;
		for (Basin basin : this.getEnvironment().getBasins() ) {
			if (basin.getFishs(this) != null) {
				nbSI += basin.getFishs(this).size();
				for (DiadromousFish superFish : basin.getFishs(this)) {
					totalEffective += superFish.getAmount();
				}
			}
		}
		if (nbSI >=0)
			return totalEffective /nbSI;
		else
			return Double.NaN;
	}

	@Observable(description = "# of SI with ind < 10")
	public double getNbLittleSI() {
		double nb = 0;
		for (Basin basin : this.getEnvironment().getBasins() ) {
			if (basin.getFishs(this) != null) {
				for (DiadromousFish superFish : basin.getFishs(this)) {
					if ( superFish.getAmount()<10L)	
						nb++;
				}
			}
		}
		return nb;
	}

	public double getMeanLengthOfMatureFish(){
		double sumOfLength = 0.;
		double numberOfMatureFish = 0.;
		for (Basin basin : this.getEnvironment().getBasins() ) {
			if (basin.getFishs(this) != null) {
				for (DiadromousFish fish : basin.getFishs(this)) {
					if (fish.isMature()){
						sumOfLength += fish.getAmount() * fish.getLength();
						numberOfMatureFish += fish.getAmount();
					}
				}
			}
		}
		return sumOfLength / numberOfMatureFish;
	}

	public double getStandardDeviationOfMatureFishLength(){
		double standardDeviationOfMatureFishLength = 0.;
		double sumOfSquareLength = 0.;
		double numberOfMatureFish = 0.;
		double meanOfSquareLengthOfMatureFish = 0.;
		for (Basin basin : this.getEnvironment().getBasins() ) {
			if (basin.getFishs(this) != null) {
				for (DiadromousFish fish : basin.getFishs(this)) {
					if (fish.isMature()){
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

	public double getLinfVonBert() {
		return linfVonBert;
	}

	public void setLinfVonBert(double linfVonBert) {
		this.linfVonBert = linfVonBert;
	}

	public double getdMaxDisp() {
		return dMaxDisp;
	}

	public double getlFirstMaturity() {
		return lFirstMaturity;
	}

	public void setlFirstMaturity(double lFirstMaturity) {
		this.lFirstMaturity = lFirstMaturity;
	}
	
	// ================================================================
	// statictis for calibration
	// ================================================================
	@Observable(description="Spawners For First Time Summary Statistic")
	public double computeSpawnerForFirstTimeSummaryStatistic() {
		double sum = 0;
		//TODO move TARGET to the right place
		double TARGET = 5.0;
		for (RiverBasin riverBasin : getEnvironment().getRiverBasins()) {
			if (riverBasin.getSpawnersForFirstTimeMeanAges().getMeanWithoutZero() > 0.) {
				double val = riverBasin.getSpawnersForFirstTimeMeanAges().getMeanWithoutZero()  - TARGET;
				sum += val * val;
			}
		}
		return sum;
	}


	@Observable(description = "Likelihood Summary stat")
	public double computeLikelihood() throws IOException {
		// 1 : read input file of observation
		FileReader reader;
		Scanner scanner;
		//TODO move the obs1900 and the scanner
		Map<String, Integer> obs1900 = new HashMap<String, Integer>();
		try {
			reader = new FileReader(fileNameInputForInitialObservation);
			// Parsing the file
			scanner = new Scanner(reader);
			scanner.useLocale(Locale.ENGLISH); // to have a comma as decimal separator !!!
			scanner.useDelimiter(Pattern.compile("[;\r]"));

			scanner.nextLine(); // to skip the file first line of entete

			while (scanner.hasNext()) {
				obs1900.put(scanner.next().replaceAll("\n", ""), scanner.nextInt());
			}
			reader.close();
			scanner.close();

		} catch (IOException ex) {
			Logger.getLogger(DiadromousFishGroup.class.getName()).log(Level.SEVERE, null, ex);
		}

		int obsVal;
		double sumLogWherePres = 0.;
		double sumLogWhereAbs = 0.;
		final double[] probOfNonNulRecruitmentDuringLastYears = getEnvironment().getProbOfNonNulRecruitmentDuringLastYears();
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
	@Observable(description="Higher Populated Latitude")
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
	
	
	@Observable(description="Number of colonized basins")
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
	

	@Observable(description="Northern colonized basins")
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

	
	@Observable(description="Southern colonized basins")
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
		//TODO keep the extreme latitudes from the catchment
		double southernBasin = 35.;
		double northernBasin  = 60.;
		RiverBasin riverBasin;
		TreeMapForCentile latitudeEffective = new TreeMapForCentile();
		for (Basin seaBasin : getEnvironment().getSeaBasins()) {
			if (seaBasin.getFishs(this) != null) {
				if (!seaBasin.getFishs(this).isEmpty()) {
					riverBasin = (RiverBasin) getEnvironment().getAssociatedRiverBasin(seaBasin);
					long effective = 0;
					for (DiadromousFish fish : seaBasin.getFishs(this)){
						effective += fish.getAmount();
					}
					southernBasin = Math.max(southernBasin, riverBasin.getLatitude());
					latitudeEffective.putWithAdding(riverBasin.getLatitude(), effective);
					northernBasin = Math.min(northernBasin, riverBasin.getLatitude());
				}
			}
		}

		Double[] rangeDistribution = new Double[4];

		rangeDistribution[0]= (latitudeEffective.isEmpty() ? (southernBasin +northernBasin)/2. :
			latitudeEffective.calculateMedian());
		rangeDistribution[1]= Math.min(southernBasin,northernBasin);
		rangeDistribution[2]= Math.max(southernBasin,northernBasin);
		rangeDistribution[3]= latitudeEffective.calculateCentile(centileForRange);	
		return rangeDistribution;
	}

	
	@Observable(description = "Range distribution")
	public Double[] getRangeDistribution() {
		double southernBasin = 0;
		double nbBasin = getEnvironment().getNbBasin();
		double northernBasin  = nbBasin;
		Basin riverBasin;
		TreeMapForCentile latitudeEffective = new TreeMapForCentile();
		for (Basin seaBasin : getEnvironment().getSeaBasins()) {
			if (seaBasin.getFishs(this) != null) {
				if (!seaBasin.getFishs(this).isEmpty()) {
					riverBasin = getEnvironment().getAssociatedRiverBasin(seaBasin);
					long effective = 0;
					for (DiadromousFish fish : seaBasin.getFishs(this)){
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
		rangeDistribution[0]= (latitudeEffective.isEmpty() ? (southernBasin +northernBasin)/2. :
			nbBasin - latitudeEffective.calculateMedian());	
		rangeDistribution[1]=  Math.min(southernBasin,northernBasin);
		rangeDistribution[2]=  Math.max(southernBasin,northernBasin);		

		return rangeDistribution;
	}

	/**
	 * @return sum of effectives in all the river basins
	 */
	@Observable(description = "Number of fishes in river basin")
	public double getFishEffective() {
		long eff = 0;
		for (RiverBasin basin : this.getEnvironment().getRiverBasins()){
			if (basin.getFishs(this) != null) {
				for (DiadromousFish fish : basin.getFishs(this)) {
					eff += fish.getAmount();
				}
			}
		}
		return eff;
	}

	
	@Override
	public void addAquaNism(DiadromousFish fish) {
		// avoid utilisation of global fishes list
		//super.addAquaNism(fish);
		fish.getPosition().addFish(fish, this);
	}

	
	@Override
	public void removeAquaNism(DiadromousFish fish) {
		// avoid utilisation of global fishes list
		//super.removeAquaNism(fish);
		fish.getPosition().removeFish(fish, this);
	}

	
	@Override
	public int compareTo(DiadromousFishGroup t) {
		return name.compareTo(t.name);
	}

	
	/**
	 * 
	 * concat at RngSatusIndex, temperatureCatchmentFile
	 * @return simulation name
	 */
	public String getSimulationId(){
		String id="_";
		id=id.concat(Integer.toString(getPilot().getParameters().getRngStatusIndex()));
		String temperatureFile = ((BasinNetworkReal) getPilot().getAquaticWorld().getEnvironment()).getTemperatureCatchmentFile();
		id=id.concat("-").concat(temperatureFile.substring(temperatureFile.length()-9, temperatureFile.length()-4));
		if (parameterSetLine>0){
			id=id.concat("-").concat(Integer.toString(parameterSetLine));
		}
		return id ;
	}

	public boolean isThereBasinToUpdate(){
		return basinsToUpdate != null;
	}

	/**
	 * @return the outputPath
	 */
	public String getOutputPath() {
		return outputPath;
	}



}
