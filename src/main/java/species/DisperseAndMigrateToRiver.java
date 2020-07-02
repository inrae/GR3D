package species;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.openide.util.lookup.ServiceProvider;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import environment.Basin;
import environment.BasinNetwork;
import environment.Time.Season;
import fr.cemagref.simaqualife.kernel.processes.AquaNismsGroupProcess;
import miscellaneous.BinomialForSuperIndividualGen;

@ServiceProvider(service = AquaNismsGroupProcess.class)
public class DisperseAndMigrateToRiver extends DisperseAndMigrateToRiverBasic {

	private double pHoming = 0.5;
	private Season riverMigrationSeason = Season.SPRING;
	private double alpha2Rep = 0.;


	public static void main(String[] args) {
		System.out.println((new XStream(new DomDriver())).toXML(new DisperseAndMigrateToRiver()));
	}


	// TODO need to be corrected to remove fish remove
	@Override
	public void doProcess(DiadromousFishGroup group) {

		if (group.getEnvironment().getTime().getSeason(group.getPilot()) == riverMigrationSeason) {
			BasinNetwork bn = group.getEnvironment();
			double dMaxDispFish = 0.;

			long amountWithHoming, strayedAmount;

			Map<Basin, Double> distBasOfFish;

			List<DiadromousFish> deadFish = new ArrayList<DiadromousFish>();
			List<DiadromousFish> newFish = new ArrayList<DiadromousFish>();

			for (DiadromousFish fish : group.getAquaNismsList()) {

				if (fish.isMature()) {
					// fish with homing
					amountWithHoming = BinomialForSuperIndividualGen.getSuccessNumber(group.getPilot(), fish.getAmount(),
							pHoming); // seuil par d�faut fix� � 50

					// strayed fish
					strayedAmount = fish.getAmount() - amountWithHoming;

					if (strayedAmount != 0) {
						// On r�cup�re les info du poids des bassin par rapport � la position du poisson
						Map<Basin, Double> accBasOfFish = new TreeMap<Basin, Double>(
								basinWeightsPerBasin.get(fish.getPosition()));
						// accBasOfFish = accessibleBasinsPerBasin.get(fish.getPosition());

						// On retire certains bassins si on consid�re une distance max de dispersion
						distBasOfFish = fish.getPosition().getNeighboursDistances();
						if (group.getdMaxDisp() != 0) {
							// TODO pourquoi distbasoffish peut �tre nul ?
							if (distBasOfFish != null) {
								dMaxDispFish = (group.getdMaxDisp() / group.getLinfVonBert(fish)) * fish.getLength();
								// load accessible basins
								for (Basin surroundingBasin : distBasOfFish.keySet()) {
									Double distance = distBasOfFish.get(surroundingBasin);
									// System.out.println("pour le poisson " + fish.hashCode() + " situ� dans le bassin
									// " + basin.getName() + " et n� dans le bassin " + fish.getBirthBasin().getName());
									// System.out.println("la distance vaut " + distance + " pour le bassin " +
									// surroundingBasin.getName());
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
						// TODO Qu'est ce qui se passe si AccBasOfFish est vide... �a beug pas mais c'est pas tr�s
						// clair... donc � v�rifier
						for (Basin accBasin : accBasOfFish.keySet()) {
							double accBasinWeightLogit = accBasOfFish.get(accBasin) + alpha2Rep * fish.getLength();
							double accBasinWeight = 1 / (1 + Math.exp(-accBasinWeightLogit));
							accBasOfFish.put(accBasin, accBasinWeight);
							totalWeight += accBasinWeight;
						}

						// compute sequentially the prob to go into a basin
						for (Basin accBasin : accBasOfFish.keySet()) {
							probToGo = accBasOfFish.get(accBasin) / totalWeight;
							amountToGo = BinomialForSuperIndividualGen.getSuccessNumber(group.getPilot(), strayedAmount,
									probToGo);

							if (amountToGo > 0) {
								newFish.add(fish.duplicateWithNewPositionAndAmount(group.getPilot(),
										bn.getAssociatedRiverBasin(accBasin), amountToGo));
							}

							totalWeight -= accBasOfFish.get(accBasin);
							strayedAmount -= amountToGo;
						}
					}

					// update fish with homing
					if (amountWithHoming > 0) {
						fish.setAmount(amountWithHoming);
						// retour soit dans le bassin de naissance pour les semelpares
						// soit dans le dernier bassin de reproduction pour les it�ropares
						fish.moveTo(group.getPilot(), bn.getAssociatedRiverBasin(fish.getPosition()), group);
					} else {
						deadFish.add(fish);
					}
				}
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
