package species;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.List;
import java.util.Map;

import environment.Basin;
import environment.BasinNetwork;
import environment.RiverBasin;
import environment.Time;
import environment.Time.Season;
import fr.cemagref.simaqualife.kernel.processes.AquaNismsGroupProcess;
import fr.cemagref.simaqualife.kernel.util.TransientParameters.InitTransientParameters;
import fr.cemagref.simaqualife.pilot.Pilot;

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

	private transient Map<Basin,Map<Basin,Double>> accessibleBasinsPerBasin;
	private transient Map<Basin,Map<Basin,Double>> distanceBasinsPerBasin;
	//private transient ObservablesHandler cObservable;

	@Override
	@InitTransientParameters
	public void initTransientParameters(Pilot pilot) {
            super.initTransientParameters(pilot);

		// calcul les poids des bassins voisins qui ne d�pendent pas des poissons pour chaque SeaBassin
		BasinNetwork bn = (BasinNetwork) pilot.getAquaticWorld().getEnvironment();
		accessibleBasinsPerBasin = new TreeMap<Basin, Map<Basin,Double>>();
		distanceBasinsPerBasin = new TreeMap<Basin, Map<Basin,Double>>();

		for (Basin seaBas : bn.getSeaBasins()){
			Map<Basin,Double> mapDist = bn.getNeighboursWithDistance(seaBas);	
			distanceBasinsPerBasin.put(seaBas, mapDist);
			
			// Compute the mean and standard deviation of the distance between seaBas and the other basin
			double sumOfDistance = 0.;
			double sumOfSquareDistance = 0.;
			double sumOfSurface = 0.;
			double sumOfSquareSurface = 0.;

			for (Basin bas : mapDist.keySet()){
				sumOfDistance += mapDist.get(bas);
				sumOfSquareDistance += Math.pow(mapDist.get(bas),2);
				sumOfSurface += ((RiverBasin) bn.getAssociatedRiverBasin(bas)).getSurface();
				sumOfSquareSurface += Math.pow(((RiverBasin) bn.getAssociatedRiverBasin(bas)).getSurface(), 2);
			}
			double meanOfInterBasinDistance = sumOfDistance / mapDist.size();
			double meanOfSquareInterBasinDistance = sumOfSquareDistance / mapDist.size();
			double standardDeviationOfInterBasinDistance = Math.pow((meanOfSquareInterBasinDistance - Math.pow(meanOfInterBasinDistance, 2)) , 0.5);			
			
			double meanOfBasinsSurface = sumOfSurface / mapDist.size();
			double meanOfSquareBasinsSurface = sumOfSquareSurface / mapDist.size();
			double standardDeviationOfBasinsSurface = Math.pow((meanOfSquareBasinsSurface - Math.pow(meanOfBasinsSurface, 2)) , 0.5);

			// Compute the weight of each basin
			Map<Basin,Double> accessibleBasins = bn.getNeighboursWithDistance(seaBas);
			for (Basin bas : accessibleBasins.keySet()){
				double weight = alpha0Rep 
						- alpha1Rep * ((accessibleBasins.get(bas) - meanOfInterBasinDistance)/standardDeviationOfInterBasinDistance)
						+ alpha3Rep*((((RiverBasin) bn.getAssociatedRiverBasin(bas)).getSurface() - meanOfBasinsSurface) / standardDeviationOfBasinsSurface);
				accessibleBasins.put(bas, weight);
			}						
			accessibleBasinsPerBasin.put(seaBas, accessibleBasins);
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

			long amountWithHoming, strayedAmount;
			double meanLengthOfMatureFishes = group.getMeanLengthOfMatureFish();
			double standardDeviationOfMatureFishesLength = group.getStandardDeviationOfMatureFishLength();

			Map<Basin,Double> distBasOfFish;

			List<DiadromousFish> deadFish = new ArrayList<DiadromousFish>();
			List<DiadromousFish> newFish = new ArrayList<DiadromousFish>();

			for (DiadromousFish fish : group.getAquaNismsList() ) {
		
				if (fish.isMature())   {
					// fish with homing
					amountWithHoming = Miscellaneous.binomialForSuperIndividual(group.getPilot(), fish.getAmount(), pHoming); // seuil par d�faut fix� � 50								

					// strayed fish 
					strayedAmount = fish.getAmount() - amountWithHoming;					
					
					if (strayedAmount != 0) {
						// On r�cup�re les info du poids des bassin par rapport � la position du poisson
						Map<Basin,Double> accBasOfFish= new TreeMap<Basin, Double>(accessibleBasinsPerBasin.get(fish.getPosition()));
						//accBasOfFish = accessibleBasinsPerBasin.get(fish.getPosition());						

						// On retire certains bassins si on consid�re une distance max de dispersion
						distBasOfFish = distanceBasinsPerBasin.get(fish.getPosition());
						if (group.getdMaxDisp() != 0){
							// TODO pourquoi distbasoffish peut �tre nul ?
							if (distBasOfFish != null){
								dMaxDispFish = (group.getdMaxDisp()/group.getLinfVonBert(fish))*fish.getLength();
								// load accessible basins
								for (Basin surroundingBasin : distBasOfFish.keySet()){
									Double distance = distBasOfFish.get(surroundingBasin);
									//System.out.println("pour le poisson " + fish.hashCode() + " situ� dans le bassin " + basin.getName() + " et n� dans le bassin " + fish.getBirthBasin().getName());
									//System.out.println("la distance vaut " + distance + " pour le bassin " + surroundingBasin.getName());
									if (distance >= dMaxDispFish) {
										accBasOfFish.remove(surroundingBasin);
									}
								}
							}							
						}

						// We fill the weight table
						double totalWeight = 0.;
						double probToGo = 0.;
						long amountToGo = 0;
						// TODO Qu'est ce qui se passe si AccBasOfFish est vide... �a beug pas mais c'est pas tr�s clair... donc � v�rifier
						for (Basin accBasin : accBasOfFish.keySet()){
							double accBasinWeightLogit = accBasOfFish.get(accBasin) + alpha2Rep*((fish.getLength() - meanLengthOfMatureFishes) / standardDeviationOfMatureFishesLength);
							double accBasinWeight = 1 / (1 + Math.exp(- accBasinWeightLogit));
							accBasOfFish.put(accBasin, accBasinWeight);
							totalWeight += accBasinWeight;
						}

						// compute sequentially the prob to go into a  basin
						for (Basin accBasin : accBasOfFish.keySet()){
							probToGo = accBasOfFish.get(accBasin) / totalWeight;
							amountToGo = Miscellaneous.binomialForSuperIndividual(group.getPilot(), strayedAmount, probToGo);

							if (amountToGo > 0){
								newFish.add(fish.duplicateWithNewPositionAndAmount(group.getPilot(), bn.getAssociatedRiverBasin(accBasin), amountToGo));			
							}

							totalWeight -= accBasOfFish.get(accBasin);
							strayedAmount -= amountToGo;	
						}
					}

					// update fish with homing
					if (amountWithHoming > 0){
						fish.setAmount(amountWithHoming);
						// retour soit dans le bassin de naissance pour les semelpares 
						// soit dans le dernier bassin de reproduction pour les it�ropares
						fish.moveTo(group.getPilot(), bn.getAssociatedRiverBasin(fish.getPosition()), group);
					} else {
						deadFish.add(fish);
					}
				}
			}
			
			for (DiadromousFish fish : deadFish){
				group.removeAquaNism(fish);
			}
			for (DiadromousFish fish : newFish){
				group.addAquaNism(fish);
			}
		}
	}
}
