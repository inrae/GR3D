/**
 * Patrick.Lambert
 * @author Patrick Lambert
 * @copyright Copyright (c) 2014, Irstea
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
import java.util.List;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import environment.BasinNetwork;
import environment.BasinNetworkReal;
import environment.RiverBasin;
import environment.Time;
import environment.Time.Season;
import fr.cemagref.observation.kernel.Observable;
import fr.cemagref.simaqualife.kernel.processes.AquaNismsGroupProcess;
import fr.cemagref.simaqualife.pilot.Pilot;
/**
 *
 */
public class IdentifyPopulation extends AquaNismsGroupProcess<DiadromousFish, DiadromousFishGroup> {

	private boolean consoleDisplay = false;
	private List<Long> years; 
	private Season fluxesSeason = Season.SPRING;
	private String fileNameOutput = "data/output/fluxes";

	private transient BufferedWriter bW;
	private String sep;
	//@Observable(description = "fluxes")
	//private transient long[] fluxes;

	public static void main(String[] args) {
		System.out.println((new XStream(new DomDriver()))
				.toXML(new IdentifyPopulation()));
	}

	/* (non-Javadoc)
	 * @see fr.cemagref.simaqualife.kernel.processes.Process#doProcess(java.lang.Object)
	 */
	@Override
	public void doProcess(DiadromousFishGroup group) {
		if (bW == null){
			if (fileNameOutput != null) {
				sep=";";
				try {
					new File(group.getOutputPath()+fileNameOutput).getParentFile().mkdirs();
					bW = new BufferedWriter(new FileWriter(new File(group.getOutputPath()+
							fileNameOutput +group.getSimulationId()+ ".csv")));

					// BasinNetworkReal nbr= (BasinNetworkReal) pilot.getAquaticWorld().getEnvironment();
					BasinNetwork nbr=  group.getEnvironment();
					bW.write("year"+sep+"migrationBasin");
					for (String basinName : nbr.getRiverBasinNames()){
						bW.write(sep+basinName);
					}
					bW.write("\n");
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		Time time = group.getEnvironment().getTime();
		if (years.contains(time.getYear(group.getPilot())) && time.getSeason(group.getPilot()) == fluxesSeason){

			String[] basinNames = group.getEnvironment().getRiverBasinNames();

			if (fileNameOutput != null) {
				try {
					for (RiverBasin basin : group.getEnvironment().getRiverBasins()){
						bW.write(time.getYear(group.getPilot())+sep+basin.getName());

						for (String basinName : basinNames){
							bW.write(sep+Math.round(basin.getSpawnerOrigins().getMeans().get(basinName)));
						}
						bW.write("\n");
					}
				}
				catch (IOException e) {
					e.printStackTrace();
				}

				if (consoleDisplay) {
					System.out.print("MIGRATION" +"\t");
					for (String basinName : basinNames){
						System.out.print(basinName+"\t");
					}
					System.out.println();	
					for (RiverBasin basin : group.getEnvironment().getRiverBasins()){
						System.out.print(basin.getName()+'\t');

						for (String basinName : basinNames){
							System.out.print(+Math.round(basin.getSpawnerOrigins().getMeans().get(basinName)) +"\t");
						}
						System.out.println();	
					}
				}
			}
		}

		if (group.getPilot().getCurrentTime()== group.getPilot().getSimBegin()+group.getPilot().getSimDuration()-1){

			try {
				bW.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
