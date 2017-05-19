package species;

import java.util.TreeMap;
import java.util.Map;

import environment.Basin;
import environment.BasinNetwork;
import environment.RiverBasin;

import fr.cemagref.simaqualife.kernel.processes.AquaNismsGroupProcess;
import fr.cemagref.simaqualife.kernel.util.TransientParameters.InitTransientParameters;
import fr.cemagref.simaqualife.pilot.Pilot;

import org.openide.util.lookup.ServiceProvider;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;



@ServiceProvider(service = AquaNismsGroupProcess.class)
public class DisperseAndMigrateToRiverBasic extends AquaNismsGroupProcess<DiadromousFish, DiadromousFishGroup> {

	private double alpha0Rep = -2.2;
	private double alpha1Rep = 17.3;
	private double alpha3Rep = 0.;
	
	private double meanBvSurface = 23071., standardDeviationBvSurface = 39833.; // for standard core values... Value for the selected 54 BV of the Atlantic Coast from the 196 BV of LAssale, 2008
	private double meanInterDistance = 300., standardDeviationInterDistance = 978.; // for standard core values...Value for the selected 54 BV of the Atlantic Coast from the 196 BV of LAssale, 2008
	
	protected transient Map<Basin,Map<Basin,Double>> accessibleBasinsPerBasin;
	protected transient Map<Basin,Map<Basin,Double>> distanceBasinsPerBasin;
	//private transient ObservablesHandler cObservable;

	@Override
	@InitTransientParameters
	public void initTransientParameters(Pilot pilot) {
            super.initTransientParameters(pilot);
		// calcul les poids des bassins voisins qui ne dépendent pas des poissons pour chaque SeaBassin
		BasinNetwork bn = (BasinNetwork) pilot.getAquaticWorld().getEnvironment();
		accessibleBasinsPerBasin = new TreeMap<Basin, Map<Basin,Double>>();
		distanceBasinsPerBasin = new TreeMap<Basin, Map<Basin,Double>>();

		for (Basin seaBas : bn.getSeaBasins()){
			Map<Basin,Double> mapDist = bn.getNeighboursWithDistance(seaBas);	
			distanceBasinsPerBasin.put(seaBas, mapDist);
						
			// Compute the weight of each basin
			Map<Basin,Double> accessibleBasins = bn.getNeighboursWithDistance(seaBas);
			for (Basin bas : accessibleBasins.keySet()){
				double weight = alpha0Rep 
						- alpha1Rep * ((accessibleBasins.get(bas)-meanInterDistance)/standardDeviationInterDistance)
						+ alpha3Rep*((((RiverBasin) bn.getAssociatedRiverBasin(bas)).getAttractiveSurface()-meanBvSurface)/standardDeviationBvSurface);
				accessibleBasins.put(bas, weight);
			}						
			accessibleBasinsPerBasin.put(seaBas, accessibleBasins);
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
