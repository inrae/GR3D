package species;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import fr.cemagref.simaqualife.kernel.processes.AquaNismsGroupProcess;

public class WriteImportNutrientFluxes extends AquaNismsGroupProcess<DiadromousFish, DiadromousFishGroup> {

	private String fileName= "fluxes";



	public static void main(String[] args) {
		System.out.println((new XStream(new DomDriver()))
				.toXML(new WriteImportNutrientFluxes()));

	}

	@Override
	public void doProcess(DiadromousFishGroup group) {

		BufferedWriter bW;
	
		String outputPath = "data/output/" ;
		String sep = ";"; 
		new File(outputPath + fileName).getParentFile().mkdirs();
		try {
			bW = new BufferedWriter(new FileWriter(new File(outputPath+
					fileName + group.getSimulationId() + ".csv")));

			bW.write("year"+sep+"nutrient" + sep + "originBasin");

			for (String birthBasinName : group.getEnvironment().getRiverBasinNames()) {
				bW.write(sep + birthBasinName); // write each basin name in the file 
			}

			bW.write("\n");

			Map<Long, Map <String, Map<String, Map<String, Double>>>> fluxesCollection = group.getNutrientRoutine().getNutrientFluxesCollection().getFluxesCollection();

			for (long year : fluxesCollection.keySet()) {

				Map <String, Map<String, Map<String, Double>>> yearsMap = fluxesCollection.get(year); 

				for (String nutrient : yearsMap.keySet()) {

					Map<String, Map<String, Double>> originsMap = yearsMap.get(nutrient); 

					for (String originBasinName : originsMap.keySet()) {

						Map<String, Double> destinationsMap = originsMap.get(originBasinName); 

						for (String destinationBasinName : destinationsMap.keySet()) {

							Double aFlux = destinationsMap.get(destinationBasinName); 

							bW.write(year + sep+ nutrient + sep +  originBasinName + sep + aFlux + "\n");
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
