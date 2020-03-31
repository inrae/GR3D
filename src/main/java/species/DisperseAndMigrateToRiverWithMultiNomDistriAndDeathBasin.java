package species;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import environment.Basin;
import environment.BasinNetwork;
import environment.RiverBasin;
import environment.SeaBasin;
import environment.Time;
import environment.Time.Season;
import fr.cemagref.simaqualife.kernel.processes.AquaNismsGroupProcess;
import fr.cemagref.simaqualife.pilot.Pilot;
import miscellaneous.Miscellaneous;

import org.openide.util.lookup.ServiceProvider;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import miscellaneous.Duo;

/**
 *
 */
@ServiceProvider(service = AquaNismsGroupProcess.class)
public class DisperseAndMigrateToRiverWithMultiNomDistriAndDeathBasin extends DisperseAndMigrateToRiverBasic {


	/**
	 * the season when fish migrate to the river to reproduce
	 * @unit
	 */
	private Season riverMigrationSeason = Season.SPRING;

	/**
	 *  the homing  probalilty during the installation of new populations ( to reach kind of equilibrium)
	 * @unit
	 */
	private double pHomingForReachEquil = 1.0;

	/**
	 *  the homing  probalilty after the installation of new populations ( after reaching an equilibrium)
	 * @unit
	 */
	private double pHomingAfterEquil = 0.8;

	/**
	 * Number of year for newly created populations to be installed ( to reach an equilibrium)
	 * @unit
	 */
	private long NbYearForInstallPop = 50;


	/** the coefficient associated with  the fish size in the logistic function used to calculate the probability to disperse
	 * @unit -
	 */
	private double alpha2Rep = 0.;

	/** 
	 * the mean length used to standardize the fish length in the logistic function that calculates the probability to disperse
	 * @unit -
	 */
	private double meanSpawnersLengthAtRepro = 45.;

	/** 
	 * the length standard deviation  used to standardize the fish length in the logistic function that calculates the probability to disperse
	 * @unit -
	 */
	private double standardDeviationOfSpawnersLengthAtRepro = 2.; // for standard core values...

	/**
	 * the weigth of the death bassin ( for strayers that do not find a catcment) used to calculate the probability to disperse
	 * @unit
	 */
	private double weightOfDeathBasin = 0.2;

	/**
	 *  a bollean to kill of the strayers (used to determine if a catchment is a souce or a sink) the year given by yearOfTheKilling
	 * @unit
	 */
	private boolean killStrayers;

	/**
	 * the year when the strayers are killed (used to determine if a catchment is a souce or a sink) if killStrayers is true
	 * @unit
	 */
	private long yearOfTheKillings;

	public static void main(String[] args) {
		System.out.println((new XStream(new DomDriver()))
				.toXML(new DisperseAndMigrateToRiverWithMultiNomDistriAndDeathBasin()));
	}


	@Override
	public void initTransientParameters(Pilot pilot) {
		// TODO Auto-generated method stub
		super.initTransientParameters(pilot);
	}


	@Override
	public void doProcess(DiadromousFishGroup group) {
		Time time = group.getEnvironment().getTime();

		if (time.getSeason(group.getPilot()) == riverMigrationSeason) {
			BasinNetwork bn = group.getEnvironment();

			long homingAmount, strayedAmount;

			// probability of homing
			double pHoming;
			if (time.getYear(group.getPilot()) < NbYearForInstallPop) {
				pHoming = pHomingForReachEquil;
			} else {
				pHoming = pHomingAfterEquil;
			}

			//List<DiadromousFish> deadFish = new ArrayList<DiadromousFish>();
			//List<DiadromousFish> newFish = new ArrayList<DiadromousFish>();
			//List<Duo<DiadromousFish, Basin>> fishesToMove = new ArrayList<Duo<DiadromousFish, Basin>>();

			for (SeaBasin departure : group.getEnvironment().getSeaBasins()) {

				RiverBasin homingDestination  = (RiverBasin) bn.getAssociatedRiverBasin(departure);

				List<DiadromousFish> fishes = departure.getFishs(group);
				if (fishes != null) {
					ListIterator<DiadromousFish> fishIterator = fishes .listIterator();
					while (fishIterator.hasNext()) {		
						DiadromousFish fish = fishIterator.next();

						//for (DiadromousFish fish : fishes) {
						if (fish.isMature()) {
							// fish with homing
							homingAmount = Miscellaneous.binomialForSuperIndividual(group.getPilot(), fish.getAmount(), pHoming); // seuil par d�faut fix� � 50								
							// strayed fish 
							if (killStrayers == true && time.getYear(group.getPilot()) >= yearOfTheKillings) {
								strayedAmount = 0;
							}
							else { 
								strayedAmount = fish.getAmount() - homingAmount;
							}

							// manage strayed fish
							if (strayedAmount != 0) {
								// calculate the weight associated with the fish length in the probabaility to disperse
								double weightFishLength = -(alpha2Rep * ((fish.getLength() - meanSpawnersLengthAtRepro) / standardDeviationOfSpawnersLengthAtRepro));


								Map<RiverBasin, Double> basinWeightswithDeathWeight = new TreeMap<RiverBasin,Double>();

								// We fill the weight table
								double totalWeight = 0.;
								double probToGo = 0.;
								long amountToGo = 0;
								// TODO manage the case when AccBasOfFish is empty
								//for (Duo<Basin, Double> accBasin : accBasOfFish) {

								for (Entry<RiverBasin, Double> entry : basinWeightsPerBasin.get(departure).entrySet()) {

									// total weight for the basins
									RiverBasin destination = entry.getKey();
									double weight = entry.getValue();
									double accBasinWeight = 1 / (1 + Math.exp(-(weight + weightFishLength)));

									// put weight to 0 for unused basins
									if (group.isThereBasinToUpdate()){
										if (time.getYear(group.getPilot()) >= group.getYearOfTheUpdate()
												&& group.getPattractive(destination.getName()) == 0){
											//TODO use correctely getPaccessible
											accBasinWeight = 0 ;
										}
									}
									totalWeight += accBasinWeight;

									basinWeightswithDeathWeight.put(destination, accBasinWeight);
								}

								// add the deathBasin in the map
								//accBasOfFish.add(new Duo<Basin, Double>(deathBasin, weightOfDeathBasin));
								//basinWeightswithDeathWeight.put(deathBasin, weightOfDeathBasin);
								totalWeight = totalWeight + weightOfDeathBasin;

								// compute sequentially the prob to go into a  basin
								for (Entry<RiverBasin, Double> entry : basinWeightswithDeathWeight.entrySet()) {
									RiverBasin destination = entry.getKey();
									double weight = entry.getValue();
									probToGo = weight / totalWeight;
									amountToGo = Miscellaneous.binomialForSuperIndividual(group.getPilot(), strayedAmount, probToGo);

									if (amountToGo > 0) {
										// add a "duplicated" fish in the destination basin
										DiadromousFish duplicatedFish = fish.duplicateWithNewPositionAndAmount(group.getPilot(), destination, amountToGo);
										destination.addFish(duplicatedFish, group);
										//newFish.add(fish.duplicateWithNewPositionAndAmount(group.getPilot(), bn.getAssociatedRiverBasin(b), amountToGo));
									}
									//else{
									//TODO add to the cemetery 
									//deadFish.add(fish.duplicateWithNewPositionAndAmount(accBasin, amountToGo));
									//}

									totalWeight -= weight;
									strayedAmount -= amountToGo;

									if (strayedAmount <= 0) {
										//CHECK if it not alwalys the northorn catcments that do not receive fish
										break;
									}
								}
							}

							// update fish with homing
							if (homingAmount > 0) {
								fish.setAmount(homingAmount);
								// retour soit dans le bassin de naissance pour les semelpares 
								// soit dans le dernier bassin de reproduction pour les it�ropares
								//fish.moveTo(group.getPilot(), bn.getAssociatedRiverBasin(fish.getPosition()), group);
								fish.setPosition(homingDestination);
							} 
							/*else {
								deadFish.add(fish);
							}*/
							
							// remove from the list of departure basin
							fishIterator.remove();
						} // end if mature

					} // end of listiterator
				}
			}
			/*	for (Duo<DiadromousFish, Basin> duo : fishesToMove) {
				duo.getFirst().moveTo(group.getPilot(), duo.getSecond(), group);
			}
			for (DiadromousFish fish : deadFish) {
				group.removeAquaNism(fish);
			}
			for (DiadromousFish fish : newFish) {
				group.addAquaNism(fish);
			}*/
		}
	}

}