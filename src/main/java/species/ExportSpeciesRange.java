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

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import environment.Time;
import fr.cemagref.simaqualife.kernel.processes.AquaNismsGroupProcess;
import fr.cemagref.simaqualife.pilot.Pilot;

/**
 *
 */
public class ExportSpeciesRange extends AquaNismsGroupProcess<DiadromousFish, DiadromousFishGroup> {


	private String fileNameOutput = "range";

	private transient BufferedWriter bW;
	private transient String sep=";";

	public static void main(String[] args) {
		System.out.println((new XStream(new DomDriver()))
				.toXML(new ExportSpeciesRange()));
	}

	/* (non-Javadoc)
	 * @see fr.cemagref.simaqualife.kernel.processes.AquaNismsGroupProcess#initTransientParameters(fr.cemagref.simaqualife.pilot.Pilot)
	 */
	@Override
	public void initTransientParameters(Pilot pilot) {
		super.initTransientParameters(pilot);
		sep=";";
	}

	/* (non-Javadoc)
	 * @see fr.cemagref.simaqualife.kernel.processes.Process#doProcess(java.lang.Object)
	 */
	@Override
	public void doProcess(DiadromousFishGroup group) {
		
		if (bW==null){
			if (fileNameOutput != null){
			    new File(group.getOutputPath()+fileNameOutput).getParentFile().mkdirs();
				try {
					bW = new BufferedWriter(new FileWriter(new File(group.getOutputPath()+
							fileNameOutput +group.getSimulationId()+ ".csv")));

					bW.write("timestep"+sep+"year"+sep+"season"+sep+"medianRange"
							+sep+"southRange"+sep+"northRange"+sep+"centileRange"+"\n");

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		try {
			Time time = group.getEnvironment().getTime();
			bW.write(group.getPilot().getCurrentTime()+sep+time.getYear(group.getPilot()));
			bW.write(sep+time.getSeason(group.getPilot()));
			
			for (double value : group.getRangeDistributionWithLat()){
				bW.write(sep+value);
			}
			bW.write("\n");
			
			if (group.getPilot().getCurrentTime()== group.getPilot().getSimBegin()+group.getPilot().getSimDuration()-1){
				bW.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
