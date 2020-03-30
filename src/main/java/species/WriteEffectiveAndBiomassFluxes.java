/**
 * patrick.lambert
 * @author Patrick Lambert
 * @copyright Copyright (c) 2018, Irstea
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.

 */
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
public class WriteEffectiveAndBiomassFluxes extends AquaNismsGroupProcess<DiadromousFish, DiadromousFishGroup> {

	private Season exportSeason = Season.SPRING;

	private String fileNameOutput = "effectiveBiomassFluxes";

	private transient BufferedWriter bW;
	private transient String sep=";";

	public static void main(String[] args) {
		System.out.println((new XStream(new DomDriver()))
				.toXML(new WriteEffectiveAndBiomassFluxes()));
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

					bW.write("year"+sep+"type"+ sep+"originBasin" ); //create the field of the column
					for (String birthBasinName : group.getEnvironment().getRiverBasinNames()) {
						bW.write(sep + birthBasinName); // write each basin name in the file 
					}
					bW.write("\n");

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		Time time = group.getEnvironment().getTime();
		
		if (time.getSeason(pilot) == exportSeason & time.getYear(pilot) >= group.getMinYearToWrite()) {

			//Create the map to get the biomass in each migration basin and birth basin
			Map<String, Map<String, Long>> spawnerEffectivePerDestination = new HashMap<String,  Map<String, Long>>(group.getEnvironment().getRiverBasinNames().length); 
			//Create the map to get the abundance in each migration and birth birth basin
			Map<String,  Map<String, Double>> spawnerBiomassPerDestination = new HashMap<String,  Map<String, Double>>(group.getEnvironment().getRiverBasinNames().length); 

			// initialise maps with 0
			for (String destinationName: group.getEnvironment().getRiverBasinNames()) {
				Map<String, Long> spawnerEffectivePerOrigin =  new HashMap<String, Long>(group.getEnvironment().getRiverBasinNames().length); 
				Map<String, Double> spawnerBiomassPerOrigin =  new HashMap<String, Double>(group.getEnvironment().getRiverBasinNames().length); 
				for (String originName : group.getEnvironment().getRiverBasinNames()){
					spawnerEffectivePerOrigin.put(originName,  0L);		
					spawnerBiomassPerOrigin.put(originName, 0.);
				}

				spawnerEffectivePerDestination.put(destinationName,spawnerEffectivePerOrigin );
				spawnerBiomassPerDestination.put(destinationName,spawnerBiomassPerOrigin );
			}

			for (RiverBasin destinationBasin: group.getEnvironment().getRiverBasins()) {
				//compute the cumulative effective and biomass per birth basin 
				if (destinationBasin.getFishs(group) != null) {
					for (DiadromousFish fish : destinationBasin.getFishs(group)) {
						if (fish.getStage() == Stage.MATURE) {
							String originBasinName = fish.getBirthBasin().getName();

							spawnerEffectivePerDestination.get(destinationBasin.getName()).
							put(originBasinName, spawnerEffectivePerDestination.get(destinationBasin.getName()).get(originBasinName) + fish.getAmount() );

							double biomass = group.getNutrientRoutine().getWeight(fish) * fish.getAmount();
							spawnerBiomassPerDestination.get(destinationBasin.getName()).
							put(originBasinName, spawnerBiomassPerDestination.get(destinationBasin.getName()).get(originBasinName) + biomass );
						}
					}
				}
			}

			try {
				// write effective
				for (String originBasinName: group.getEnvironment().getRiverBasinNames()) {
					bW.write(time.getYear(pilot)+ sep + "effective" + sep + originBasinName );

					for (String destinationBasinName : group.getEnvironment().getRiverBasinNames()) {
						bW.write(sep+spawnerEffectivePerDestination.get(destinationBasinName).get(originBasinName));
					}
					// write an end-of-line
					bW.write("\n");
				}

				// write biomass
				for (String originBasinName: group.getEnvironment().getRiverBasinNames()) {
					bW.write(time.getYear(pilot)+ sep + "biomass"+ sep + originBasinName );

					for (String destinationBasinName : group.getEnvironment().getRiverBasinNames()) {
						bW.write(sep+spawnerBiomassPerDestination.get(destinationBasinName).get(originBasinName));
					}
					// write an end-of-line
					bW.write("\n");
				}


				if (group.getPilot().getCurrentTime()== group.getPilot().getSimBegin()+group.getPilot().getSimDuration()-1) {
					bW.flush();
					bW.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}

