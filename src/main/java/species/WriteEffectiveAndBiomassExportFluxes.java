package species;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import environment.RiverBasin;
import environment.Time;
import environment.Time.Season;
import fr.cemagref.simaqualife.kernel.processes.AquaNismsGroupProcess;
import species.DiadromousFish.Stage;

public class WriteEffectiveAndBiomassExportFluxes extends AquaNismsGroupProcess<DiadromousFish, DiadromousFishGroup> {

	private String fileName = "currentEffectiveAndBiomassExportFluxes";
	private Season writeSeason = Season.SUMMER;

	private transient BufferedWriter bW;

	public static void main(String[] args) {
		System.out.println((new XStream(new DomDriver())).toXML(new WriteEffectiveAndBiomassExportFluxes()));
	}


	@Override
	public void doProcess(DiadromousFishGroup group) {
		String sep = ";";

		Time time = group.getEnvironment().getTime();

		// initialise the bW the first time
		try {
			if (bW == null) {
				String outputPath = group.getOutputPath();
				new File(outputPath + fileName).getParentFile().mkdirs();

				bW = new BufferedWriter(
						new FileWriter(new File(outputPath + fileName + group.getSimulationId() + ".csv")));

				bW.write("year" + sep + "originBasin" + sep + "abundance" + sep + "biomass" + "\n");
			}

			// write information for the current year
			Map<String, Double> totalOutputFluxes = new Hashtable<String, Double>(); 

			long year = time.getYear(group.getPilot());

			if (year >= group.getMinYearToWrite() & time.getSeason(group.getPilot()) == writeSeason) {			
				for (RiverBasin basin : group.getEnvironment().getRiverBasins()){

					totalOutputFluxes.put("biomass", 0.); //cr�ation de la biomasse 
					totalOutputFluxes.put("abundance", 0.);

					List<DiadromousFish> fishes = basin.getFishs(group);
					if (fishes!=null) {
						ListIterator<DiadromousFish> fishIterator = fishes.listIterator();
						// for (DiadromousFish fish : fishes) {
						while (fishIterator.hasNext()) {
							DiadromousFish fish = fishIterator.next();
							if (fish.getStage()==Stage.IMMATURE) {
								double abundanceExp = fish.getAmount();
								double biomass = group.getNutrientRoutine().getWeight(fish) * fish.getAmount(); 

								totalOutputFluxes.put("biomass", totalOutputFluxes.get("biomass") + biomass); 
								totalOutputFluxes.put("abundance", totalOutputFluxes.get("abundance") + abundanceExp); 
							}
						}
					}
					try {

						bW.write(time.getYear(group.getPilot()) + ";"+ basin.getName() + ";");
						bW.write(+ totalOutputFluxes.get("abundance") + ";" + totalOutputFluxes.get("biomass"));
						bW.write("\n");

					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

			// close the bW
			if (group.getPilot().getCurrentTime() == group.getPilot().getSimBegin() + group.getPilot().getSimDuration()
					- 1) {
				bW.flush();
				bW.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
