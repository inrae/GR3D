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

@ServiceProvider(service = AquaNismsGroupProcess.class)
public class DisperseAndMigrateToRiverWithMultiNomDistriAndDeathBasin extends DisperseAndMigrateToRiverBasic {

	private double pHomingForReachEquil = 1.0;
	private double pHomingAfterEquil = 0.8;
	private long NbYearForInstallPop = 50;
	private Season riverMigrationSeason = Season.SPRING;
	private double alpha2Rep = 0.;
	private double meanSpawnersLengthAtRepro = 45., standardDeviationOfSpawnersLengthAtRepro = 2.; // for standard core values...
	private double weightOfDeathBasin = 0.2;
	private boolean killStrayers;
	private long yearOfTheKillings;

	public static void main(String[] args) {
		System.out.println((new XStream(new DomDriver()))
				.toXML(new DisperseAndMigrateToRiverWithMultiNomDistriAndDeathBasin()));
	}

	@Override
	public void doProcess(DiadromousFishGroup group) {

		if (Time.getSeason(group.getPilot()) == riverMigrationSeason) {
			BasinNetwork bn = group.getEnvironment();

			long amountWithHoming, strayedAmount;
			double pHoming;

			if (Time.getYear(group.getPilot()) < NbYearForInstallPop) {
				pHoming = pHomingForReachEquil;
			} else {
				pHoming = pHomingAfterEquil;
			}

			List<DiadromousFish> deadFish = new ArrayList<DiadromousFish>();
			List<DiadromousFish> newFish = new ArrayList<DiadromousFish>();

			Basin deathBasin = new Basin(-1, "deathBasin", 0, 0, 0, 0);
			List<Duo<DiadromousFish, Basin>> fishesToMove = new ArrayList<Duo<DiadromousFish, Basin>>();
			for (Basin basin : group.getEnvironment().getSeaBasins()) {

				List<DiadromousFish> fishes = basin.getFishs(group);
				if (fishes != null) {
					for (int j = 0; j < fishes.size(); j++) {
						DiadromousFish fish = fishes.get(j);
						assert fish.getPosition().getType() == Basin.TypeBassin.SEA;
						if (fish.isMature()) {
							// fish with homing
							amountWithHoming = Miscellaneous.binomialForSuperIndividual(group.getPilot(), fish.getAmount(), pHoming); // seuil par défaut fixé à 50								

							// strayed fish 
							if (killStrayers == true && Time.getYear(group.getPilot()) >= yearOfTheKillings) {
								strayedAmount = 0;
							}
							else { 
								strayedAmount = fish.getAmount() - amountWithHoming;
							}

							if (strayedAmount != 0) {
								double weightFishLength = Math.exp(-(alpha2Rep * ((fish.getLength() - meanSpawnersLengthAtRepro) / standardDeviationOfSpawnersLengthAtRepro)));

								// On récupère les info du poids des bassin par rapport à la position du poisson
								List<Duo<Basin, Double>> accBasOfFish = new ArrayList<Duo<Basin, Double>>();
								for (Map.Entry<Basin, Double> entry : accessibleBasinsPerBasin.get(fish.getPosition()).entrySet()) {
									Duo<Basin, Double> duo = new Duo<Basin, Double>(entry.getKey(), entry.getValue());
									accBasOfFish.add(duo);
								}

								// We fill the weight table
								double totalWeight = 0.;
								double probToGo = 0.;
								long amountToGo = 0;
								// TODO Qu'est ce qui se passe si AccBasOfFish est vide... ça beug pas mais c'est pas très clair... donc à vérifier
								for (Duo<Basin, Double> accBasin : accBasOfFish) {
									Basin b = accBasin.getFirst();
									Double weight = accBasin.getSecond();
									double accBasinWeight = 1 / (1 + Math.exp(-weight) * weightFishLength);
									// put weight to 0 for unused basins
									if (group.isThereBasinToUpdate()){
										if (Time.getYear(group.getPilot()) >= group.getYearOfTheUpdate()
												&& group.getPattractive(b.getName()) == 0){
											//TODO use correctely getPaccessible
											accBasinWeight = 0 ;
										}
									}
									accBasin.setSecond(accBasinWeight);
									totalWeight += accBasinWeight;
									//}
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
								// soit dans le dernier bassin de reproduction pour les itéropares
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
