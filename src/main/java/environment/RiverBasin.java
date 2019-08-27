package environment;

import org.jfree.data.time.MovingAverage;

import miscellaneous.QueueMemory;
import miscellaneous.QueueMemoryMap;
import fr.cemagref.observation.kernel.Observable;
import fr.cemagref.observation.kernel.ObservablesHandler;
import fr.cemagref.simaqualife.pilot.Pilot;
import species.DiadromousFish;
import species.DiadromousFishGroup;

/**
 * @author Patrick.Lambert
 *
 */
public class RiverBasin extends Basin {

	private final double latitude;
	private final double longitude;
	private final double surface; // in km2
	private double firstDamHeight;
	private double pAccessible; // in %
	private double pAttractive;
	private long yearOfFirstNonNulRep;
	private long yearOfLastNulRep;

	private QueueMemoryMap<Long> spawnerOrigins;

	//private int memorySize; // size of the queue	
	// TODO define as many QueueMemory as populations
	private QueueMemory<Long> lastRecruitments;
	private QueueMemory<Long> lastRecruitmentExpectations;
	private QueueMemory<Double> lastProdCapacities; // keep the production capacities of the basin (parameter alpha of the reproduction function)
	private QueueMemory<Double> lastRecsOverProdCaps;
	private QueueMemory<Double> lastPercentagesOfAutochtones;
	private QueueMemory<Double> numberOfNonNulRecruitmentDuringLastYears; // Prob of non nul recruitment during the last "memorySize" years... if 10 non nul recruitment during the last 10 year, p=0.999... if 8 non nul recruitment during the last 10 years, p = 0.8... if 0 recruitment, p = 0.001
	private QueueMemory<Double> spawnersForFirstTimeMeanAges;
	private QueueMemory<Double> numberOfNonNulRecruitmentForFinalProbOfPres;
	
	private double nativeSpawnerMortality; // mortality coefficient between recruitement and spawning for fish born in this basin
	private double mortalityCrash;
	private double stockTrap; 
	private double lastFemaleSpawnerNumber;
	
	protected static transient ObservablesHandler cobservable;

	public ObservablesHandler getCobservable() {
		return cobservable;
	}

	@Observable(description = "nb of juvenile")
	public double getJuvenileNumber() {
		long nbJuv = 0;
		for (DiadromousFishGroup group : this.getGroups()) {
			for (DiadromousFish fish : this.getFishs(group)) {
				if (fish.getAge() == 0) {
					nbJuv += fish.getAmount();
				}
			}
		}
		return nbJuv;
	}

	@Observable(description = "nb of spawners")
	public double getSpawnerNumber() {
		long nbSpawn = 0;
		for (DiadromousFishGroup group : this.getGroups()) {
			for (DiadromousFish fish : this.getFishs(group)) {
				if (fish.isMature()) {
					nbSpawn += fish.getAmount();
				}
			}
		}
		return nbSpawn;
	}

	public RiverBasin(Pilot pilot, int id, String basinName, double winterTemperature,
			double springTemperature, double summerTemperature,
			double fallTemperature, double latitude, double longitude,
			double surface, double firstDamHeight, double pAccessible, double pAttractive,
			int memorySize, int memorySizeLongQueue) {
		super(id, basinName, winterTemperature, springTemperature,
				summerTemperature, fallTemperature);
		this.latitude = latitude;
		this.longitude = longitude;
		this.surface = surface;
		this.firstDamHeight = firstDamHeight;
		this.pAccessible = pAccessible;
		this.pAttractive = pAttractive;
		this.yearOfFirstNonNulRep = 0;
		this.yearOfLastNulRep = 0;
		this.type = Basin.TypeBassin.RIVER;
		//this.memorySize = memoryAllQueues;		
		this.lastRecruitments = new QueueMemory<Long>(memorySize);
		this.lastRecruitmentExpectations = new QueueMemory<Long>(memorySize);
		this.lastProdCapacities = new QueueMemory<Double>(memorySize);
		this.lastRecsOverProdCaps = new QueueMemory<Double>(memorySize);
		this.lastPercentagesOfAutochtones = new QueueMemory<Double>(memorySize);
		this.numberOfNonNulRecruitmentDuringLastYears = new QueueMemory<Double>(memorySize);
		this.spawnersForFirstTimeMeanAges = new QueueMemory<Double>(memorySize);
		this.numberOfNonNulRecruitmentForFinalProbOfPres = new QueueMemory<Double>(memorySizeLongQueue);

		if (cobservable == null) {
			cobservable = pilot.addObservable(this.getClass());
		}
	}
	
	public RiverBasin(Pilot pilot, int id, String basinName, double winterTemperature,
			double springTemperature, double summerTemperature,
			double fallTemperature, double latitude, double longitude,
			double surface, double firstDamHeight, double pAccessible,
			int memorySize, int memorySizeLongQueue) {
				this(pilot, id, basinName, winterTemperature, springTemperature, summerTemperature, 
				fallTemperature, latitude, longitude, surface, firstDamHeight, pAccessible, 1,
				memorySize, memorySizeLongQueue);
				}
				

	public double getFirstDamHeight() {
		return firstDamHeight;
	}

	public double getLongitude() {
		return longitude;
	}

	public double getLatitude() {
		return latitude;
	}

	public double getSurface() {
		return surface;
	}
	
	public void setPAccessible(double pAccessible) {
		this.pAccessible = pAccessible;
	}
	
	public void setPAttractive(double pAttractive) {
		this.pAttractive = pAttractive;
	}
	
	public double getAccessibleSurface(){
		return (surface * pAccessible);
	}

	public double getAttractiveSurface(){
		return (surface * pAttractive);
	}

	public long getYearOfFirstNonNulRep() {
		return yearOfFirstNonNulRep;
	}

	public void setYearOfFirstNonNulRep(long yearOfFirstNonNulRep) {
		this.yearOfFirstNonNulRep = yearOfFirstNonNulRep;
	}

	public long getYearOfLastNulRep() {
		return yearOfLastNulRep;
	}

	public void setYearOfLastNulRep(long yearOfLastNulRep) {
		this.yearOfLastNulRep = yearOfLastNulRep;
	}

	public QueueMemory<Double> getLastProdCapacities() {
		return lastProdCapacities;
	}

	public QueueMemory<Long> getLastRecruitments() {
		return lastRecruitments;
	}

	public QueueMemory<Long> getLastRecruitmentExpectations() {
		return lastRecruitmentExpectations;
	}

	public QueueMemory<Double> getLastRecsOverProdCaps() {
		return lastRecsOverProdCaps;
	}

	public QueueMemory<Double> getLastPercentagesOfAutochtones() {
		return lastPercentagesOfAutochtones;
	}

	public QueueMemory<Double> getNumberOfNonNulRecruitmentDuringLastYears() {
		return numberOfNonNulRecruitmentDuringLastYears;
	}

	public QueueMemory<Double> getSpawnersForFirstTimeMeanAges() {
		return spawnersForFirstTimeMeanAges;
	}

	public QueueMemory<Double> getNumberOfNonNulRecruitmentForFinalProbOfPres(){
		return numberOfNonNulRecruitmentForFinalProbOfPres;
	}

	@Observable(description = "R/alpha")
	public double getLastRecruitmentOverProdCapacity() {
		if (lastRecsOverProdCaps.getLastItem() != null) {
			return lastRecsOverProdCaps.getLastItem();
		} else {
			return 0.;
		}
	}

	@Observable(description = "% Autochtone")
	public double getLastPercentageOfAutochtone() {
		if (lastPercentagesOfAutochtones.getLastItem() != null) {
			return lastPercentagesOfAutochtones.getLastItem();
		} else {
			return 0.;
		}
	}

	public void setSpawnerOrigins(QueueMemoryMap<Long> spawnerOrigins) {
		this.spawnerOrigins = spawnerOrigins;
	}

	public QueueMemoryMap<Long> getSpawnerOrigins() {
		return spawnerOrigins;
	}

	/**
	 * @return the nativeSpawnerMortality
	 */
	public double getNativeSpawnerMortality() {
		return nativeSpawnerMortality;
	}

	/**
	 * @param nativeSpawnerMortality the nativeSpawnerMortality to set
	 */
	public void setNativeSpawnerMortality(double nativeSpawnerMortality) {
		this.nativeSpawnerMortality = nativeSpawnerMortality;
	}

	/**
	 * @return the mortalityCrash
	 */
	public double getMortalityCrash() {
		return mortalityCrash;
	}

	/**
	 * @param mortalityCrash the mortalityCrash to set
	 */
	public void setMortalityCrash(double mortalityCrash) {
		this.mortalityCrash = mortalityCrash;
	}

	/**
	 * @return the stockTrap
	 */
	public double getStockTrap() {
		return stockTrap;
	}

	/**
	 * @param stockTrap the stockTrap to set
	 */
	public void setStockTrap(double stockTrap) {
		this.stockTrap = stockTrap;
	}
	
	/**
	 * @return the last number of female spawners
	 */
	public double getLastFemaleSpawnerNumber() {
		return lastFemaleSpawnerNumber;
	}

	/**
	 * @param lastSpawner the lastSpawnerNumber to set
	 */
	public void setLastFemaleSpawnerNumber(double lastSpawner) {
		this.lastFemaleSpawnerNumber = lastSpawner;
	}

	public String getPopulationStatus() {
		String populationStatus;
		if (Double.isNaN(nativeSpawnerMortality))
			populationStatus="noSense";
		else {

			if (nativeSpawnerMortality> mortalityCrash)
				populationStatus="overZcrash";
			else {
				if (lastFemaleSpawnerNumber < stockTrap)
					populationStatus = "inTrapWithStrayers";
				else {
					if (lastFemaleSpawnerNumber * lastPercentagesOfAutochtones.getLastItem() < stockTrap)
						populationStatus = "inTrapWithOnlyNatives";
					else
						populationStatus = "sustain";
				}
			}
		}
		//System.out.println("\t"+populationStatus);
		return populationStatus;
	}
}
