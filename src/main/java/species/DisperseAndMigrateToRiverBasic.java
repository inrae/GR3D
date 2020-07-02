package species;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.openide.util.lookup.ServiceProvider;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import environment.Basin;
import environment.BasinNetwork;
import environment.RiverBasin;
import environment.SeaBasin;
import fr.cemagref.simaqualife.kernel.processes.AquaNismsGroupProcess;
import fr.cemagref.simaqualife.kernel.util.TransientParameters.InitTransientParameters;
import fr.cemagref.simaqualife.pilot.Pilot;

@ServiceProvider(service = AquaNismsGroupProcess.class)
public class DisperseAndMigrateToRiverBasic extends AquaNismsGroupProcess<DiadromousFish, DiadromousFishGroup> {

	/**
	 * the coefficient independent of environmental factors in the logistic function used to calculate the probability
	 * to disperse
	 * 
	 * @unit -
	 */
	private double alpha0Rep = -2.2;

	/**
	 * the coefficient associated with the distance between catchment in the logistic function used to calculate the
	 * probability to disperse i.e. the relative influence of accessibility
	 * 
	 * @unit -
	 */
	// TODO transform to a negative value (the larger the distance , the smaller the accessibility is) and correct in
	// the computation of the weight
	private double alpha1Rep = 17.3;

	/**
	 * the mean distance between catchments used to standardize the inter-catchment distance in the logistic function
	 * that calculates the probability to disperse
	 * 
	 * @unit km
	 */
	private double meanInterDistance = 300.; // (from the 53 cathments among the 173 of Lassalles 2008)

	/**
	 * the standard deviation of distances between catchments used to standardize the inter-catchment distance in the
	 * logistic function that calculates the probability to disperse
	 * 
	 * @unit km
	 */
	private double standardDeviationInterDistance = 978.; // (from the 53 cathments among the 173 of Lassalles 2008)

	/**
	 * the coefficient associated with the attractive surface of the catchment in the logistic function used to
	 * calculate the probability to disperse i.e. the relative influence of attractiveness should be positive : the
	 * larger the surface , the higher the attractiveness is
	 * 
	 * @unit -
	 */
	// TODO check the sign in the formula
	private double alpha3Rep = 0.;

	/**
	 * the mean surface used to standardize the catchment surface in the logistic function that calculates the
	 * probability to disperse
	 * 
	 * @unit ? ha ?
	 */
	private double meanBvSurface = 23071.; // (from the 53 cathments among the 173 of Lassalles 2008)

	/**
	 * the standard deviation used to standardize the catchment surface in the logistic function that calculates the
	 * probability to disperse
	 * 
	 * @unit ? ha ?
	 */
	private double standardDeviationBvSurface = 39833.; // (from the 53 cathments among the 173 of Lassalles 2008)

	/**
	 * a map associtaing a sea bassin with the weight (accessibility and atrrtactivity) for each river bassin <key>
	 * SeaBasin <value> <key> RiverBasin <value> weight to calculate probaility to disperse
	 */
	protected transient Map<SeaBasin, Map<RiverBasin, Double>> basinWeightsPerBasin;


	/**
	 * a map associtaing a sea bassin with the distance for each river bassin <key> SeaBasin <value> <key> RiverBasin
	 * <value> distance between the river Basin and the river basin associated with the sea basin
	 */
	// @Deprecated
	// protected transient Map<SeaBasin, Map<RiverBasin, Double>> basinDistancesPerBasin;

	@Override
	@InitTransientParameters
	public void initTransientParameters(Pilot pilot) {
		super.initTransientParameters(pilot);

		BasinNetwork bn = (BasinNetwork) pilot.getAquaticWorld().getEnvironment();

		// calcul les poids des bassins voisins qui ne d�pendent pas des poissons pour chaque SeaBassin
		basinWeightsPerBasin = new TreeMap<SeaBasin, Map<RiverBasin, Double>>();

		// Compute the weight of each river basin
		for (SeaBasin seaBas : bn.getSeaBasins()) {

			Map<RiverBasin, Double> mapWeight = new TreeMap<RiverBasin, Double>();
			RiverBasin associatedRiverBasin;

			for (Entry<Basin, Double> entry : seaBas.getNeighboursDistances().entrySet()) {
				associatedRiverBasin = (environment.RiverBasin) entry.getKey();
				// RiverBasin associatedRiverBasin = (RiverBasin) bn.getAssociatedRiverBasin(entry.getKey());

				double weight = alpha0Rep - alpha1Rep * ((entry.getValue() - meanInterDistance) / standardDeviationInterDistance)
						+ alpha3Rep
								* ((associatedRiverBasin.getAttractiveSurface() - meanBvSurface) / standardDeviationBvSurface);

				mapWeight.put(associatedRiverBasin, weight);

			}
			basinWeightsPerBasin.put(seaBas, mapWeight);
		}
	}


	public static void main(String[] args) {
		System.out.println((new XStream(new DomDriver())).toXML(new DisperseAndMigrateToRiverBasic()));
	}


	@Override
	public void doProcess(DiadromousFishGroup group) {

	}
}
