package species;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import environment.Time;
import environment.Time.Season;
import fr.cemagref.simaqualife.kernel.processes.AquaNismsGroupProcess;

public class WriteCurrentNutrientImportFluxes extends AquaNismsGroupProcess<DiadromousFish, DiadromousFishGroup> {

	private String fileName= "currentNutrientImporttFluxes";
	private Season writeSeason = Season.SPRING;

	private transient BufferedWriter bW;

	public static void main(String[] args) {
		System.out.println((new XStream(new DomDriver()))
				.toXML(new WriteCurrentNutrientImportFluxes()));

	}

	@Override
	public void doProcess(DiadromousFishGroup group) {
		String sep = ";"; 

		try {
			// initialise the bW the first time
			if ( bW == null) {

				String outputPath = "data/output/" ;
				new File(outputPath + fileName).getParentFile().mkdirs();

				bW = new BufferedWriter(new FileWriter(new File(outputPath+
						fileName + group.getSimulationId() + ".csv")));

				bW.write("year"+sep+"nutrient" + sep + "originBasin");
				for (String birthBasinName : group.getEnvironment().getRiverBasinNames()) {
					bW.write(sep + birthBasinName); // write each basin name in the file 
				}
				bW.write("\n");
			}


			Map<Long, Map <String, Map<String, Map<String, Double>>>> fluxesCollection = group.getNutrientRoutine().
					getNutrientImportFluxesCollection().getImportFluxesCollection();

			long year = Time.getYear(group.getPilot());

			if (year >= group.getMinYearToWrite() & Time.getSeason(group.getPilot()) == writeSeason) {

				Map <String, Map<String, Map<String, Double>>> yearsMap = fluxesCollection.get(year); 

				for (String nutrient : group.getNutrientRoutine().getNutrientsOfInterest()) {

					Map<String, Map<String, Double>> originsMap = yearsMap.get(nutrient); 
					for (String originBasinName : group.getEnvironment().getRiverBasinNames()) {
						bW.write(year+ sep+ nutrient + sep + originBasinName );
						Map<String, Double> destinationsMap = originsMap.get(originBasinName); 

						for (String destinationBasinName : group.getEnvironment().getRiverBasinNames()) {
							Double aFlux = destinationsMap.get(destinationBasinName); 
							bW.write( sep + aFlux);
						}
						bW.write("\n");
					}
				}
			}

			// close the bW
			if (group.getPilot().getCurrentTime()== group.getPilot().getSimBegin()+group.getPilot().getSimDuration()-1){
				bW.flush();
				bW.close();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
