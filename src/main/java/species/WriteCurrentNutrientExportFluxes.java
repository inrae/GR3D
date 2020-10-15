package species;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import environment.Time;
import environment.Time.Season;
import fr.cemagref.simaqualife.kernel.processes.AquaNismsGroupProcess;

public class WriteCurrentNutrientExportFluxes extends AquaNismsGroupProcess<DiadromousFish, DiadromousFishGroup> {

	private String fileName = "currentNutrientExportFluxes";
	private Season writeSeason = Season.SUMMER;

	private transient BufferedWriter bW;


	public static void main(String[] args) {
		System.out.println((new XStream(new DomDriver())).toXML(new WriteCurrentNutrientExportFluxes()));
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

				bW.write("year" + sep + "nutrient" + sep + "originBasin" + sep + "value" + "\n");
			}

			// write information for the current year
			Map<Long, Map<String, Map<String, Double>>> fluxesCollection = group.getNutrientRoutine()
					.getNutrientExportFluxesCollection().getExportFluxesCollection();

			long year = time.getYear(group.getPilot());

			if (year >= group.getMinYearToWrite() & time.getSeason(group.getPilot()) == writeSeason) {

				for (String nutrient : group.getNutrientRoutine().getNutrientsOfInterest()) {
					for (String originBasinName : group.getEnvironment().getRiverBasinNames()) {
						bW.write(year + sep + nutrient + sep + originBasinName);
						bW.write(sep + fluxesCollection.get(year).get(nutrient).get(originBasinName));
					}
					bW.write("\n");
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
