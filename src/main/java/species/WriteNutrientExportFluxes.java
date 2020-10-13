package species;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import fr.cemagref.simaqualife.kernel.processes.AquaNismsGroupProcess;

@Deprecated
public class WriteNutrientExportFluxes extends AquaNismsGroupProcess<DiadromousFish, DiadromousFishGroup> {

	private String fileName = "nutrientExportFluxes";


	public static void main(String[] args) {
		System.out.println((new XStream(new DomDriver())).toXML(new WriteNutrientExportFluxes()));

	}


	@Override
	public void doProcess(DiadromousFishGroup group) {

		BufferedWriter bW;

		String outputPath = group.getOutputPath();
		String sep = ";";
		new File(outputPath + fileName).getParentFile().mkdirs();
		try {
			bW = new BufferedWriter(new FileWriter(new File(outputPath + fileName + group.getSimulationId() + ".csv")));

			bW.write("year" + sep + "nutrient" + sep + "originBasin" + sep + "value" + "\n");

			Map<Long, Map<String, Map<String, Double>>> fluxesCollection = group.getNutrientRoutine()
					.getNutrientExportFluxesCollection().getExportFluxesCollection();

			// to iterate on sorted years
			List<Long> years = new ArrayList<Long>(fluxesCollection.keySet());
			Collections.sort(years);

			for (long year : years) {
				if (year >= group.getMinYearToWrite()) {

					for (String nutrient : group.getNutrientRoutine().getNutrientsOfInterest()) {

						for (String originBasinName : group.getEnvironment().getRiverBasinNames()) {
							bW.write(year + sep + nutrient + sep + originBasinName);
							bW.write(sep + fluxesCollection.get(year).get(nutrient).get(originBasinName) + '\n');
						}

					}
				}
			}

			bW.flush();
			bW.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
