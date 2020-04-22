package species;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import environment.Basin;
import environment.BasinNetwork;
import environment.RiverBasin;
import environment.SeaBasin;
import environment.Time;
import environment.Time.Season;
import fr.cemagref.simaqualife.kernel.processes.AquaNismsGroupProcess;
import fr.cemagref.simaqualife.kernel.util.TransientParameters.InitTransientParameters;
import fr.cemagref.simaqualife.pilot.Pilot;
import miscellaneous.BinomialForSuperIndividualGen;
import miscellaneous.Miscellaneous;

import org.openide.util.lookup.ServiceProvider;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

@ServiceProvider(service = AquaNismsGroupProcess.class)
public class DisperseAndMigrateToRiverStandardization extends AquaNismsGroupProcess<DiadromousFish, DiadromousFishGroup> {

	private double pHoming=0.5;
	//private transient UniformGen genUniform;
	private Season riverMigrationSeason = Season.SPRING;
	private double alpha0Rep = 0.;
	private double alpha1Rep = 0.015;
	private double alpha2Rep = 0.;
	private double alpha3Rep = 0.;

	private transient Map<SeaBasin,Map<RiverBasin,Double>> basinWeightsPerBasin;
	private transient Map<SeaBasin,Map<RiverBasin,Double>> basinDistancesPerBasin;
	//private transient ObservablesHandler cObservable;

	@Override
	@InitTransientParameters
	public void initTransientParameters(Pilot pilot) {
		super.initTransientParameters(pilot);

		
		BasinNetwork bn = (BasinNetwork) pilot.getAquaticWorld().getEnvironment();
		
		// calcul les poids des bassins voisins qui ne d�pendent pas des poissons pour chaque SeaBassin

		basinDistancesPerBasin = new TreeMap<SeaBasin, Map<RiverBasin,Double>>();

		basinWeightsPerBasin = new TreeMap<SeaBasin, Map<RiverBasin,Double>>();
		
		for (SeaBasin seaBasin : bn.getSeaBasins()){
			// prepare the distance matrix with riverBasin as key
			Map<RiverBasin,Double> mapDist = new TreeMap<RiverBasin, Double>();	
			for (Entry<Basin,Double> entry : bn.getNeighboursWithDistance(seaBasin).entrySet() ) {
				mapDist.put((RiverBasin) bn.getAssociatedRiverBasin(entry.getKey()), entry.getValue());
			}
			
			// fill basin Distances Per Basin
			basinDistancesPerBasin.put(seaBasin, mapDist);

			// fill  basin Weights Per Basin
			//       compute the mean and standard deviation of the distance between seaBas and the other basin
			double sumOfDistance = 0.;
			double sumOfSquareDistance = 0.;
			double sumOfSurface = 0.;
			double sumOfSquareSurface = 0.;

			for (Entry<RiverBasin, Double> entry : mapDist.entrySet()) {
				sumOfDistance += entry.getValue() ;
				sumOfSquareDistance += entry.getValue() * entry.getValue();
				double basinSurface = entry.getKey().getSurface();
				sumOfSurface += basinSurface;
				sumOfSquareSurface += basinSurface * basinSurface;
			}
			double meanOfInterBasinDistance = sumOfDistance / mapDist.size();
			double meanOfSquareInterBasinDistance = sumOfSquareDistance / mapDist.size();
			double standardDeviationOfInterBasinDistance = Math.pow(meanOfSquareInterBasinDistance - (meanOfInterBasinDistance * meanOfInterBasinDistance) , 0.5);			

			double meanOfBasinsSurface = sumOfSurface / mapDist.size();
			double meanOfSquareBasinsSurface = sumOfSquareSurface / mapDist.size();
			double standardDeviationOfBasinsSurface = Math.pow(meanOfSquareBasinsSurface - (meanOfBasinsSurface * meanOfBasinsSurface) , 0.5);

			//        compute the weight of each basin
			//Map<Basin,Double> accessibleBasins = bn.getNeighboursWithDistance(seaBasin);
			
			Map<RiverBasin,Double> mapWeights = new TreeMap<RiverBasin, Double>();	
			for (Entry<Basin,Double> entry : bn.getNeighboursWithDistance(seaBasin).entrySet() ) {
				mapWeights.put((RiverBasin) bn.getAssociatedRiverBasin(entry.getKey()), entry.getValue());
			}
			 //replace the value by the weight
			for (Entry<RiverBasin, Double> entry : mapWeights.entrySet()) {
				double weight = alpha0Rep 
						- alpha1Rep * ((entry.getValue() - meanOfInterBasinDistance) / standardDeviationOfInterBasinDistance)
						+ alpha3Rep* ((entry.getKey().getSurface() - meanOfBasinsSurface) / standardDeviationOfBasinsSurface);
				mapWeights.put(entry.getKey(), weight);
			}						
			basinWeightsPerBasin.put(seaBasin, mapWeights);
		}
	}

	public static void main(String[] args) {
		System.out.println((new XStream(new DomDriver()))
				.toXML(new DisperseAndMigrateToRiverStandardization()));
	}

	@Override
	public void doProcess(DiadromousFishGroup group) {

		if (group.getEnvironment().getTime().getSeason(group.getPilot()) == riverMigrationSeason ){
			BasinNetwork bn = group.getEnvironment();
			double dMaxDispFish = 0.;

			long homingAmount, strayedAmount;
			double meanLengthOfMatureFishes = group.getMeanLengthOfMatureFish();
			double standardDeviationOfMatureFishesLength = group.getStandardDeviationOfMatureFishLength();



			List<DiadromousFish> deadFish = new ArrayList<DiadromousFish>();
			List<DiadromousFish> newFish = new ArrayList<DiadromousFish>();


			for (SeaBasin departure : bn.getSeaBasins()) {

				// distance from departure basin to destination basin
				Map<RiverBasin,Double> distanceBasinFromDeparture = basinDistancesPerBasin.get(departure);
				
				RiverBasin homingDestination  = (RiverBasin) bn.getAssociatedRiverBasin(departure);

				ListIterator<DiadromousFish> fishIterator = departure.getFishs(group) .listIterator();
				while (fishIterator.hasNext()) {		
					DiadromousFish fish = fishIterator.next();
					//for (DiadromousFish fish : group.getAquaNismsList() ) {

					if (fish.isMature())   {

						// fish with homing
						homingAmount = BinomialForSuperIndividualGen.getSuccessNumber(group.getPilot(), fish.getAmount(), pHoming); // seuil par d�faut fix� � 50								
						// strayed fish 
						strayedAmount = fish.getAmount() - homingAmount;					

						if (strayedAmount != 0) {
							// accessible basin from the departure basin. depend of the size of the fish
							Map<RiverBasin,Double>  wForAccessibleBasins = new TreeMap<RiverBasin, Double>(basinWeightsPerBasin.get(departure));
							
							// remove basins too far 
							if (group.getdMaxDisp() != 0){
								dMaxDispFish = (group.getdMaxDisp()/group.getLinfVonBert(fish))*fish.getLength();
								// load accessible basins
								for (Basin surroundingBasin : distanceBasinFromDeparture.keySet()){
									double distance = distanceBasinFromDeparture.get(surroundingBasin);
									//System.out.println("pour le poisson " + fish.hashCode() + " situ� dans le bassin " + basin.getName() + " et n� dans le bassin " + fish.getBirthBasin().getName());
									//System.out.println("la distance vaut " + distance + " pour le bassin " + surroundingBasin.getName());
									if (distance >= dMaxDispFish) {
										wForAccessibleBasins.remove(surroundingBasin);
									}
								}							
							}

							// We fill the weight table
							double totalWeight = 0.;
							double probToGo = 0.;
							long amountToGo = 0;
							// TODO Qu'est ce qui se passe si AccBasOfFish est vide... �a beug pas mais c'est pas tr�s clair... donc � v�rifier
							for (RiverBasin destination : wForAccessibleBasins.keySet()){
								double accBasinWeightLogit = wForAccessibleBasins.get(destination) + alpha2Rep*((fish.getLength() - meanLengthOfMatureFishes) / standardDeviationOfMatureFishesLength);
								double accBasinWeight = 1 / (1 + Math.exp(- accBasinWeightLogit));
								wForAccessibleBasins.put(destination, accBasinWeight);
								totalWeight += accBasinWeight;
							}

							// compute sequentially the prob to go into a  basin
							for (RiverBasin destination : wForAccessibleBasins.keySet()){
								probToGo = wForAccessibleBasins.get(destination) / totalWeight;
								amountToGo = BinomialForSuperIndividualGen.getSuccessNumber(group.getPilot(), strayedAmount, probToGo);

								if (amountToGo > 0){
									// add a "duplicated" fish in the destination basin
									DiadromousFish duplicatedFish = fish.duplicateWithNewPositionAndAmount(group.getPilot(), destination, amountToGo);
									destination.addFish(duplicatedFish, group);
									//newFish.add(fish.duplicateWithNewPositionAndAmount(group.getPilot(), bn.getAssociatedRiverBasin(accBasin), amountToGo));			
								}

								totalWeight -= wForAccessibleBasins.get(destination);
								strayedAmount -= amountToGo;	
							}
						}

						// move still alive fish with homing
						if (homingAmount > 0){
							fish.setAmount(homingAmount);
							// retour soit dans le bassin de naissance pour les semelpares 
							// soit dans le dernier bassin de reproduction pour les it�ropares
							//fish.moveTo(group.getPilot(), bn.getAssociatedRiverBasin(fish.getPosition()), group);
							fish.setPosition(homingDestination);
						}/* else {
							deadFish.add(fish);
						}*/
						
						// remove from the list of departure basin
						fishIterator.remove();
					}
				}
			}
/*
			for (DiadromousFish fish : deadFish){
				group.removeAquaNism(fish);
			}
			for (DiadromousFish fish : newFish){
				group.addAquaNism(fish);
			}
			*/
		}
	}
}
