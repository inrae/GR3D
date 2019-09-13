
package species;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import environment.RiverBasin;
import environment.Time;
import environment.Time.Season;
import fr.cemagref.simaqualife.kernel.processes.AquaNismsGroupProcess;
import fr.cemagref.simaqualife.pilot.Pilot;
import species.DiadromousFish.Stage;

/**
 *
 */
public class ExportFluxes extends AquaNismsGroupProcess<DiadromousFish, DiadromousFishGroup> {

	private Season exportSeason = Season.SPRING;

	private String fileNameOutput = "EffectiveFluxes";

	private transient BufferedWriter bW;
	private transient String sep=";";


	public static void main(String[] args) {
		System.out.println((new XStream(new DomDriver()))
				.toXML(new ExportFluxes()));
	}

	/* (non-Javadoc)
	 * @see fr.cemagref.simaqualife.kernel.processes.AquaNismsGroupProcess#initTransientParameters(fr.cemagref.simaqualife.pilot.Pilot)
	 */
	@Override
	public void initTransientParameters(Pilot pilot) {
		super.initTransientParameters(pilot);
		sep=";";

	}

	@Override
	public void doProcess(DiadromousFishGroup group) {

		if (bW==null){
			if (fileNameOutput != null){
				new File(group.getOutputPath()+fileNameOutput).getParentFile().mkdirs();
				try {
					bW = new BufferedWriter(new FileWriter(new File(group.getOutputPath()+
							fileNameOutput +group.getSimulationId()+ ".csv")));

					bW.write("year"+sep+"migrationBasin" ); //create the field of the column
					for (String birthBasinName : group.getEnvironment().getRiverBasinNames()) {
						bW.write(sep + birthBasinName); // write each basin name in the file 
					}
					bW.write("\n");

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		try {
			if (Time.getSeason(pilot) == exportSeason & Time.getYear(pilot)>1900) {


				for (RiverBasin migrationBasin : group.getEnvironment().getRiverBasins()) {
					//Create the map to get the abundance in each birth basin
					Map<String, Long> spawnerOriginsBeforeReproduction = new HashMap<String, Long>(group.getEnvironment().getRiverBasinNames().length); 
					for (String basinName : group.getEnvironment().getRiverBasinNames()){
						spawnerOriginsBeforeReproduction.put(basinName,  0L);			
					}

					//compute the cumulative effective per birth basin 
					if (migrationBasin.getFishs(group) != null) {
						for (DiadromousFish fish : migrationBasin.getFishs(group)) {
							if (fish.getStage() == Stage.MATURE) {
								String birthBasinName = fish.getBirthBasin().getName();
								spawnerOriginsBeforeReproduction.put(birthBasinName, spawnerOriginsBeforeReproduction.get(birthBasinName) + fish.getAmount());


							}

						}
					}

					//write the first two fields of the line 
					bW.write(Time.getYear(pilot)+sep+migrationBasin.getName());

					//write the cumulative effective from birth basin 
					for (String birthBasinName : group.getEnvironment().getRiverBasinNames()) {
						bW.write(sep+spawnerOriginsBeforeReproduction.get(birthBasinName));
					}
					//write an end-of(line
					bW.write("\n");
				}
			}
			if (group.getPilot().getCurrentTime()== group.getPilot().getSimBegin()+group.getPilot().getSimDuration()-1)
				bW.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}