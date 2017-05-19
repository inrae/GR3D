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
package environment;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import species.DiadromousFish;
import species.DiadromousFishGroup;
import fr.cemagref.simaqualife.kernel.processes.AquaNismsGroupProcess;

/**
 *
 */
public class InformTime extends AquaNismsGroupProcess<DiadromousFish, DiadromousFishGroup> {

	private boolean consoleDisplay = true;
	
	public static void main(String[] args) { System.out.println((new
			XStream(new DomDriver())) .toXML(new InformTime())); }

	/* (non-Javadoc)
	 * @see fr.cemagref.simaqualife.kernel.processes.Process#doProcess(java.lang.Object)
	 */
	@Override
	public void doProcess(DiadromousFishGroup group) {
		if (consoleDisplay)
		System.out.println("--- "+ group.getPilot().getCurrentTime() + " :\t "+
				Time.getSeason(group.getPilot())+" \t"+Time.getYear(group.getPilot())+ " ---");
	}

}
