/*
 * Copyright (C) 2013 dumoulin
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

import environment.Time;
import fr.cemagref.observation.kernel.ObservablesHandler;
import fr.cemagref.observation.observers.CSVObserver;

/**
 *
 * @author dumoulin
 */
public class MyCSVObserver extends CSVObserver {

    @Override
    public void valueChanged(ObservablesHandler clObservable, Object instance, long t) {
        if (isInDates(t)) {
            StringBuffer buf = new StringBuffer();
            StringBuffer sbSeparator = new StringBuffer(" " + this.separator + " ");
            // print current Time
            buf.append(Time.getNbYearFromBegin(t));
            buf.append(" ");
            buf.append(Time.getSeason(t));
            // print value of each field
            for (ObservablesHandler.ObservableFetcher fetcher : fetchers) {
                buf.append(sbSeparator);
                Object value = getValue(fetcher, instance);
                buf.append(value == null ? "N/A" : value);
            }
            outputStream.println(buf);
        }
    }
}
