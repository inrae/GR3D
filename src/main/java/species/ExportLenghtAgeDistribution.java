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
public class ExportLenghtAgeDistribution extends AquaNismsGroupProcess<DiadromousFish, DiadromousFishGroup> {

	private Season exportSeason = Season.SPRING;

	private String fileNameOutput = "lengthAgeDistribution";

	private transient BufferedWriter bW;
	private transient String sep=";";

	public static void main(String[] args) {
		System.out.println((new XStream(new DomDriver()))
				.toXML(new ExportLenghtAgeDistribution()));
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

					bW.write("timestep"+sep+"year"+sep+"season"+sep+"basin"+ sep+"gender" 
							+sep+ "effective" +sep+"length"+sep+"age"+sep+"nbSpawn"+"\n");

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		try {
			if (Time.getSeason(pilot) == exportSeason & Time.getYear(pilot)>1900) {
				for (RiverBasin basin : group.getEnvironment().getRiverBasins()) {
					if (basin.getFishs(group) != null) {
						for (DiadromousFish fish : basin.getFishs(group)) {
							if (fish.getStage() == Stage.MATURE) {
								
								//System.out.println(fish.getAge() + " -> "+ fish.getLength() +" - "+fish.getNumberOfReproduction());
								
								bW.write(pilot.getCurrentTime() + sep);
								bW.write(Time.getYear(pilot) + sep);
								bW.write(Time.getSeason(pilot) + sep);
								bW.write(basin.getName() + sep);
								bW.write(fish.getGender() + sep);
								bW.write(fish.getAmount() + sep);
								bW.write(fish.getLength() + sep);
								bW.write(fish.getAge()+ sep);
								bW.write(fish.getNumberOfReproduction()+	"\n");
							}
						}
					}
				}
			}
			if (group.getPilot().getCurrentTime()== group.getPilot().getSimBegin()+group.getPilot().getSimDuration()-1)
				bW.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

