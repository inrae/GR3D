/*
 * Copyright (C) 2014 dumoulin
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
package miscellaneous;

import fr.cemagref.simaqualife.extensions.pilot.BatchRunner;
import fr.cemagref.simaqualife.pilot.Pilot;

public class EasyABC {

    public static void runSimulation(String[] batchArgs, String outputfilename, String[] paramNames, double[] paramValues) throws Exception {
        try {
            Pilot pilot = new Pilot();
            BatchRunner runner = new BatchRunner(pilot);
            pilot.init();
            runner.parseArgs(batchArgs, false, true, false);
            pilot.load();
            ReflectUtils.setFieldValueFromPath(pilot.getAquaticWorld().getAquaNismsGroupsList().get(0), "processes.processesAtEnd.0.fileNameOutput", outputfilename);
            for (int i = 0; i < paramNames.length; i++) {
                ReflectUtils.setFieldValueFromPath(pilot.getAquaticWorld().getAquaNismsGroupsList().get(0), paramNames[i], paramValues[i]);
            }
            pilot.run();
            // For forcing resources releasing (like shp files)
            System.gc();
        } catch (Throwable e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        runSimulation("-simDuration 400 -simBegin 2 -timeStepDuration 1 -groups data/input/fishTryRealBV_CC.xml -env data/input/BNtryRealBasins.xml -observers data/input/obsTryReal.xml".split("\\ "),
                "data/output/tsointsoin", new String[]{"processes.processesEachStep.5.tempMinRep", "processes.processesEachStep.4.tempMinMortGenInRiv",
                    "processes.processesEachStep.5.ratioS95_S50", "processes.processesEachStep.3.pHomingAfterEquil"},
                new double[]{10, 8, 2, 0.7});
    }
}
