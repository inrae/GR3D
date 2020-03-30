package species;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import environment.Basin;
import environment.BasinNetwork;
import environment.Time;
import environment.Time.Season;
import fr.cemagref.simaqualife.kernel.processes.AquaNismsGroupProcess;
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
	public void doProcess(DiadromousFishGroup group) {
		Time time = group.getEnvironment().getTime();

		if (time.getSeason(group.getPilot()) == riverMigrationSeason) {
			BasinNetwork bn = group.getEnvironment();

			long amountWithHoming, strayedAmount;

			// probability of homing
			double pHoming;
			if (time.getYear(group.getPilot()) < NbYearForInstallPop) {
				pHoming = pHomingForReachEquil;
			} else {
				pHoming = pHomingAfterEquil;
			}

			List<DiadromousFish> deadFish = new ArrayList<DiadromousFish>();
			List<DiadromousFish> newFish = new ArrayList<DiadromousFish>();

			// creation of the death basin (for the lost strayers)
			//TODO move as a transient field
			Basin deathBasin = new Basin(-1, "deathBasin", 0, 0, 0, 0);
			
			List<Duo<DiadromousFish, Basin>> fishesToMove = new ArrayList<Duo<DiadromousFish, Basin>>();
			for (Basin basin : group.getEnvironment().getSeaBasins()) {

				List<DiadromousFish> fishes = basin.getFishs(group);
				if (fishes != null) {
					for (DiadromousFish fish : fishes) {
	
						// verify that fish is in a sea basin
						assert fish.getPosition().getType() == Basin.TypeBassin.SEA;
						
						if (fish.isMature()) {
							// fish with homing
							amountWithHoming = Miscellaneous.binomialForSuperIndividual(group.getPilot(), fish.getAmount(), pHoming); // seuil par d�faut fix� � 50								

							// strayed fish 
							if (killStrayers == true && time.getYear(group.getPilot()) >= yearOfTheKillings) {
								strayedAmount = 0;
							}
							else { 
								strayedAmount = fish.getAmount() - amountWithHoming;
							}

							// influence of the fish length on the probability to disperse
							if (strayedAmount != 0) {
								// calcula the weight associated with the fish length in the probabaility to disperse
								double weightFishLength = -(alpha2Rep * ((fish.getLength() - meanSpawnersLengthAtRepro) / standardDeviationOfSpawnersLengthAtRepro));

								// upload the weights associated with features of the catchment (accessibility and attractivity)
								List<Duo<Basin, Double>> accBasOfFish = new ArrayList<Duo<Basin, Double>>();
								for (Map.Entry<Basin, Double> entry : accessibleBasinsPerBasin.get(fish.getPosition()).entrySet()) {
									Duo<Basin, Double> duo = new Duo<Basin, Double>(entry.getKey(), entry.getValue());
									accBasOfFish.add(duo);
								}

								// We fill the weight table
								double totalWeight = 0.;
								double probToGo = 0.;
								long amountToGo = 0;
								// TODO manage the case when AccBasOfFish is empty
								for (Duo<Basin, Double> accBasin : accBasOfFish) {
									// total weight for the basin
									Basin b = accBasin.getFirst();
									Double weight = accBasin.getSecond();
									double accBasinWeight = 1 / (1 + Math.exp(-(weight + weightFishLength)));
									
									// put weight to 0 for unused basins
									if (group.isThereBasinToUpdate()){
										if (time.getYear(group.getPilot()) >= group.getYearOfTheUpdate()
												&& group.getPattractive(b.getName()) == 0){
											//TODO use correctely getPaccessible
											accBasinWeight = 0 ;
										}
									}
									accBasin.setSecond(accBasinWeight);
									totalWeight += accBasinWeight;
								}

								// add the deathBasin in the list
								accBasOfFish.add(new Duo<Basin, Double>(deathBasin, weightOfDeathBasin));
								totalWeight = totalWeight + weightOfDeathBasin;

								// compute sequentially the prob to go into a  basin
								for (Duo<Basin, Double> accBasin : accBasOfFish) {
									Basin b = accBasin.getFirst();
									Double weight = accBasin.getSecond();
									probToGo = weight / totalWeight;
									amountToGo = Miscellaneous.binomialForSuperIndividual(group.getPilot(), strayedAmount, probToGo);

									if (amountToGo > 0) {
										if (b.getId() != -1) {
											newFish.add(fish.duplicateWithNewPositionAndAmount(group.getPilot(), bn.getAssociatedRiverBasin(b), amountToGo));
										}
										//else{
											//TODO add to the cemetery 
											//deadFish.add(fish.duplicateWithNewPositionAndAmount(accBasin, amountToGo));
											//}
									}
									totalWeight -= weight;
									strayedAmount -= amountToGo;

									if (strayedAmount <= 0) {
										break;
									}
								}
							}

							// update fish with homing
							if (amountWithHoming > 0) {
								fish.setAmount(amountWithHoming);
								// retour soit dans le bassin de naissance pour les semelpares 
								// soit dans le dernier bassin de reproduction pour les it�ropares
								fishesToMove.add(new Duo<DiadromousFish, Basin>(fish, bn.getAssociatedRiverBasin(fish.getPosition())));
							} else {
								deadFish.add(fish);
							}
						}
					}
				}
			}
			for (Duo<DiadromousFish, Basin> duo : fishesToMove) {
				duo.getFirst().moveTo(group.getPilot(), duo.getSecond(), group);
			}
			for (DiadromousFish fish : deadFish) {
				group.removeAquaNism(fish);
			}
			for (DiadromousFish fish : newFish) {
				group.addAquaNism(fish);
			}
		}
	}
}
