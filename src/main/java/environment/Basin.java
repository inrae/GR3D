package environment;

import environment.Time.Season;
import fr.cemagref.simaqualife.kernel.spatial.Position;
import fr.cemagref.simaqualife.pilot.Pilot;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import species.DiadromousFish;
import species.DiadromousFishGroup;

public class Basin implements Position, Comparable<Basin> {

	@Override
	public int compareTo(Basin t) {
		return id - t.id;
	}

	public static enum TypeBassin {

		RIVER, SEA, OFFSHORE
	};

	/**
	 *  Identifier of basin in GR3D
	 *  	if n is the number of basin, id from 0 to n-1 correspond to riverBasin, from n to 2n-1 to seaBasin 
	 *     and the next to offshore basins.
	 *     with modulo operation it is possible to find to which riverBasin a seaBassin corresponds to
	 * @unit -
	 */
	private final int id;

	/**
	 *  the name of the catchement
	 * @unit -
	 */
	private final String name;

	/**
	 * the type of the basin 
	 * @unit -
	 */
	protected TypeBassin type;

	/**
	 * the mean temperature during the current winter
	 * @unit °C
	 */
	private double winterTemperature;

	/**
	 * the mean temperature during the current spring
	 * @unit °C
	 */
	private double springTemperature;

	/**
	 * the mean temperature during the current summer
	 * @unit °C
	 */
	private double summerTemperature;

	/**
	 * the mean temperature during the current winter
	 * @unit °C
	 */
	private double fallTemperature;
	//private double latitude;
	// private double longitude;

	//private double alphaBH; // Correspond to the carrying capacity
	//private double betaBH; // Correspond to level of genitors to produce alpha/2 recruits
	//private double depensatoryBH = 1.; // If d=1, no depensatory dynamics; if d>1 Allee effect

	/**
	 * the basin shape to be drawn
	 * @unit
	 */
	private final Path2D.Double shape;


	/**
	 *  distance to other catchments in the universe (from the same type ?)
	 * @unit km
	 */
	private Map<Basin, Double> neighboursDistances;


	/**
	 * list of diadromous fish present in the basin for each group 
	 * @unit
	 */
	private Map<DiadromousFishGroup, List<DiadromousFish>> fishPerGroup;


	/**
	 *  effective of fish present in the basin for each group
	 * @unit
	 */
	private Map<DiadromousFishGroup, Long> effectivePerGroup;

	public Basin(int id, String name, double winterTemperature,
			double springTemperature, double summerTemperature,
			double fallTemperature) {
		this.id = id;
		this.name = name;
		this.winterTemperature = winterTemperature;
		this.springTemperature = springTemperature;
		this.summerTemperature = summerTemperature;
		this.fallTemperature = fallTemperature;

		this.shape = new Path2D.Double();

		fishPerGroup = new TreeMap<DiadromousFishGroup, List<DiadromousFish>>();
		effectivePerGroup = new TreeMap<DiadromousFishGroup, Long>();
		neighboursDistances = new TreeMap<Basin, Double>();
	}

	public TypeBassin getType() {
		return type;
	}

	public Map<Basin, Double> getNeighboursDistances() {
		return neighboursDistances;
	}

	public void setNeighboursDistances(Map<Basin, Double> neighboursDistances) {
		this.neighboursDistances = neighboursDistances;
	}

	public Set<DiadromousFishGroup> getGroups() {
		return fishPerGroup.keySet();
	}

	public List<DiadromousFish> getFishs(DiadromousFishGroup group) {
		return fishPerGroup.get(group);
	}

	public void updateEffective() {
		for (DiadromousFishGroup group : fishPerGroup.keySet()) {
			long eff = 0;
			for (DiadromousFish fish : fishPerGroup.get(group)) {
				eff += fish.getAmount();
			}
			effectivePerGroup.put(group, eff);
		}
	}

	public Map<DiadromousFishGroup, List<DiadromousFish>> getFishPerGroup() {
		return fishPerGroup;
	}

	public long getEffective(DiadromousFishGroup group) {
		long eff = 0;
		if (fishPerGroup.containsKey(group)) {
			for (DiadromousFish fish : fishPerGroup.get(group)) {
				eff += fish.getAmount();
			}
		}
		return eff;
	}

	public int getSuperFishNumber(DiadromousFishGroup group) {
		int nb = 0;
		if (fishPerGroup.containsKey(group)) {
			nb = fishPerGroup.get(group).size();
		}
		return nb;
	}

	public boolean addFish(DiadromousFish fish, DiadromousFishGroup group) {
		if (!fishPerGroup.containsKey(group)) {
			fishPerGroup.put(group, new ArrayList<DiadromousFish>());
		}

		return (fishPerGroup.get(group)).add(fish);
	}

	public boolean removeFish(DiadromousFish fish, DiadromousFishGroup group) {
		return (fishPerGroup.get(group)).remove(fish);
	}

	public double getAnnualTemperature() {
		return ((winterTemperature + springTemperature
				+ summerTemperature + fallTemperature) / 4.);
	}

	public double getCurrentTemperature(Pilot pilot) {
		Time time =( (BasinNetwork) pilot.getAquaticWorld().getEnvironment()).getTime();
		if (time.getSeason(pilot) == Season.WINTER) {
			return (winterTemperature);
		} else if (time.getSeason(pilot) == Season.SPRING) {
			return (springTemperature);
		} else if (time.getSeason(pilot) == Season.SUMMER) {
			return (summerTemperature);
		} else {
			return (fallTemperature);
		}
	}

	public Path2D.Double getShape() {
		return shape;
	}

	/*	public void setShape(Path2D.Double shape) {
     this.shape = shape;
     }*/
	public String getName() {
		return name;
	}

	public int getId() {
		return id;
	}

	public double getWinterTemperature() {
		return winterTemperature;
	}

	public double getSpringTemperature() {
		return springTemperature;
	}

	public double getSummerTemperature() {
		return summerTemperature;
	}

	public double getFallTemperature() {
		return fallTemperature;
	}

	public void setWinterTemperature(double winterTemperature) {
		this.winterTemperature = winterTemperature;
	}

	public void setSpringTemperature(double springTemperature) {
		this.springTemperature = springTemperature;
	}

	public void setSummerTemperature(double summerTemperature) {
		this.summerTemperature = summerTemperature;
	}

	public void setFallTemperature(double fallTemperature) {
		this.fallTemperature = fallTemperature;
	}

}
