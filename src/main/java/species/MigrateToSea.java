package species;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import environment.Basin;
import environment.RiverBasin;
import environment.Time;
import environment.Time.Season;
import fr.cemagref.simaqualife.kernel.processes.AquaNismsGroupProcess;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import miscellaneous.Duo;
import species.DiadromousFish.Stage;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = AquaNismsGroupProcess.class)
public class MigrateToSea extends AquaNismsGroupProcess<DiadromousFish, DiadromousFishGroup> {

	private Season seaMigrationSeason = Season.SUMMER;	

	private boolean displayFluxesOnConsole = true;

	public static void main(String[] args) {
		System.out.println((new XStream(new DomDriver()))
				.toXML(new MigrateToSea()));
	}	

	@Override
	public void doProcess(DiadromousFishGroup group) {
		Time time = group.getEnvironment().getTime();
		if (time.getSeason(group.getPilot()) == seaMigrationSeason ){
			Basin destination;

			//On cr�er la Map pour stocker les flux d'export
			Map<String, Double> totalOutputFluxes = new Hashtable<String, Double>(); 
		

			List<Duo<DiadromousFish,Basin>> fishesToMove = new ArrayList<Duo<DiadromousFish,Basin>>();
			for (RiverBasin basin : group.getEnvironment().getRiverBasins()) {
		
				//Fish move to sea and compute the related export of nutrients 
				List<DiadromousFish> fishes = basin.getFishs(group);

				// ON r�-initialise notre map pour chauqe bassin 
				for (String nutrient : group.getNutrientRoutine().getNutrientsOfInterest()) {
					totalOutputFluxes.put(nutrient, 0.); 
				}
				totalOutputFluxes.put("biomass", 0.); //cr�ation de la biomasse 
				totalOutputFluxes.put("abundanceExp", 0.);

				if (fishes!=null) {
					for (DiadromousFish fish : fishes) {
						destination = group.getEnvironment().getAssociatedSeaBasin(fish.getPosition());
						fishesToMove.add(new Duo<DiadromousFish, Basin>(fish, destination)); //Mentionne la sortie d'un poisson de la boucle 

						double abundanceExp = fish.getAmount();
						double biomass = group.getNutrientRoutine().getWeight(fish) * fish.getAmount(); 

						if (fish.getStage()==Stage.IMMATURE) {
							Map <String, Double> aFluxExportedByJuveniles= group.getNutrientRoutine().computeNutrientsExportForJuveniles(fish); 
							for (String nutrient: aFluxExportedByJuveniles.keySet()) {
								totalOutputFluxes.put(nutrient,totalOutputFluxes.get(nutrient) + aFluxExportedByJuveniles.get(nutrient) * fish.getAmount()); 	
								
								group.getNutrientRoutine().getNutrientExportFluxesCollection().put(time.getYear(group.getPilot()), nutrient, basin.getName(),  aFluxExportedByJuveniles.get(nutrient) * fish.getAmount());
							}

							totalOutputFluxes.put("biomass", totalOutputFluxes.get("biomass") + biomass); 
							totalOutputFluxes.put("abundanceExp", totalOutputFluxes.get("abundanceExp")+ abundanceExp); 
						}
					}     
				}

				for (Duo<DiadromousFish,Basin> duo : fishesToMove) {
					duo.getFirst().moveTo(group.getPilot(), duo.getSecond(), group); //on d�place les poissons dans le fichier MoveTo et on d�note la destination du poisson.
				}

				if (displayFluxesOnConsole) {
					System.out.println(group.getPilot().getCurrentTime() + "; " + time.getYear(group.getPilot()) + ";" + time.getSeason(group.getPilot()) 
					+ ";EXPORT;"
					+ basin.getName() + "; " + totalOutputFluxes);}

				BufferedWriter bW = group.getbWForFluxes();
				if ( bW != null) {
					try {

						bW.write(group.getPilot().getCurrentTime() + "; " + time.getYear(group.getPilot()) + ";" + time.getSeason(group.getPilot()) 
						+";"+ basin.getName() + ";" + basin.getJuvenileNumber() + ";EXPORT; NONE");
						bW.write(";" + totalOutputFluxes.get("abundanceExp") + ";" + totalOutputFluxes.get("biomass"));
						for (String nutrient : group.getNutrientRoutine().getNutrientsOfInterest()) {
							bW.write(";" + totalOutputFluxes.get(nutrient));
						}
						bW.write("\n");

					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
}