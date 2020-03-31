package species;

import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.Map;

import environment.Basin;
import environment.BasinNetwork;
import environment.RiverBasin;
import environment.SeaBasin;
import fr.cemagref.simaqualife.kernel.processes.AquaNismsGroupProcess;
import fr.cemagref.simaqualife.kernel.util.TransientParameters.InitTransientParameters;
import fr.cemagref.simaqualife.pilot.Pilot;

import org.openide.util.lookup.ServiceProvider;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;



@ServiceProvider(service = AquaNismsGroupProcess.class)
public class DisperseAndMigrateToRiverBasic extends AquaNismsGroupProcess<DiadromousFish, DiadromousFishGroup> {

	/** the coefficient independent of environmental factors in the logistic function used to calculate the probability to disperse
	 * @unit -
	 */
	private double alpha0Rep = -2.2;
	
	/** 
	 * the coefficient associated with  the distance between catchment in the logistic function used to calculate the probability to disperse
	 * i.e. the relative influence of accessibility
	 * @unit -
	 */
	//TODO transform to a negative value (the larger the distance , the smaller the accessibility is) and correct in the computation of the weight
	private double alpha1Rep = 17.3; 
	
	/**
	 * the mean distance between catchments used to standardize the inter-catchment distance in the logistic function that calculates the probability to disperse
	 * @unit  km
	 */
	private double meanInterDistance = 300.; // (from the 53 cathments among the 173 of Lassalles 2008)
	
	/**
	 * the standard deviation of distances between catchments used to standardize the inter-catchment distance in the logistic function that calculates the probability to disperse
	 * @unit  km
	 */
	private double standardDeviationInterDistance = 978.; // (from the 53 cathments among the 173 of Lassalles 2008)
	
	/** 
	 * the coefficient associated with  the attractive surface of the catchment  in the logistic function used to calculate the probability to disperse
	 * i.e. the relative influence of attractiveness
	 *  should be positive : the larger the surface , the higher the attractiveness is
	 * @unit -
	 */
	//TODO check the sign in the formula 
	private double alpha3Rep = 0.;
	
	/**
	 * the mean surface used to standardize the catchment surface in the logistic function that calculates the probability to disperse
	 * @unit  ? ha ?
	 */
	private double meanBvSurface = 23071.; // (from the 53 cathments among the 173 of Lassalles 2008)

	/**
	 * the standard deviation used to standardize the catchment surface in the logistic function that calculates the probability to disperse
	 * @unit ? ha ?
	 */
	private double standardDeviationBvSurface = 39833.; // (from the 53 cathments among the 173 of Lassalles 2008)
	
	/**
	 *  a map associtaing a sea bassin with the weight (accessibility and atrrtactivity) for each river bassin
	 *  <key> SeaBasin
	 *  <value>
	 *  	<key> RiverBasin
	 *  	<value>  part of weight independant of fish size used to calculate probability to disperse
	 */
	protected transient Map<SeaBasin,Map<RiverBasin,Double>> basinWeightsPerBasin;
	
	/**
	 *  a map associtaing a sea bassin with the distance for each river bassin
	 *  <key> SeaBasin
	 *  <value>
	 *  	<key> RiverBasin
	 *  	<value> distance between the river Basin and the river basin associated with the sea basin
	 */
	protected transient Map<SeaBasin,Map<RiverBasin,Double>> basinDistancesPerBasin;
	

	@Override
	@InitTransientParameters
	public void initTransientParameters(Pilot pilot) {
            super.initTransientParameters(pilot);
		// calcul les poids des bassins voisins qui ne d�pendent pas des poissons pour chaque SeaBassin
		BasinNetwork bn = (BasinNetwork) pilot.getAquaticWorld().getEnvironment();
		basinWeightsPerBasin = new TreeMap<SeaBasin, Map<RiverBasin,Double>>();
		basinDistancesPerBasin = new TreeMap<SeaBasin, Map<RiverBasin,Double>>();

		for (SeaBasin seaBasin : bn.getSeaBasins()){
			// prepare the distance matrix with riverBasin as key
			Map<RiverBasin,Double> mapDist = new TreeMap<RiverBasin, Double>();	
			for (Entry<Basin,Double> entry : bn.getNeighboursWithDistance(seaBasin).entrySet() ) {
				mapDist.put((RiverBasin) bn.getAssociatedRiverBasin(entry.getKey()), entry.getValue());
			}
	
			
			// fill basin Distances Per Basin
			basinDistancesPerBasin.put(seaBasin, mapDist);
						
			// Compute the weight of each river basin
			//Map<Basin,Double> accessibleBasins = bn.getNeighboursWithDistance(seaBas);
			Map<RiverBasin,Double> mapWeights = new TreeMap<RiverBasin, Double>();	
			for (Entry<Basin,Double> entry : bn.getNeighboursWithDistance(seaBasin).entrySet() ) {
				mapWeights.put((RiverBasin) bn.getAssociatedRiverBasin(entry.getKey()), entry.getValue());
			}
			 //replace the value by the weight
				for (Entry<RiverBasin, Double> entry : mapWeights.entrySet()) {
					double weight = alpha0Rep 
							- alpha1Rep * ((entry.getValue() - meanInterDistance) / standardDeviationInterDistance)
							+ alpha3Rep* ((entry.getKey().getSurface() - meanBvSurface) / standardDeviationBvSurface);
					mapWeights.put(entry.getKey(), weight);
				}						
				basinWeightsPerBasin.put(seaBasin, mapWeights);
		}
	}

	public static void main(String[] args) {
		System.out.println((new XStream(new DomDriver()))
				.toXML(new DisperseAndMigrateToRiverBasic()));
	}

	@Override
	public void doProcess(DiadromousFishGroup group) {
		
	}
}
