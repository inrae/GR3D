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
package miscellaneous;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class QueueMemoryMap <E extends Number>  {

	Map<String, QueueMemory<E>> queueMemoryMap;

	public QueueMemoryMap(List<String> basinNames, int memorySize) {
		super();
		queueMemoryMap = new HashMap<String, QueueMemory<E>>();
		for (String basinName : basinNames){
			queueMemoryMap.put(basinName, new QueueMemory<E>(memorySize));
		}
	}

	public void push(Map<String, E> items) {
		List<String>   mapKeys= new ArrayList<String>(queueMemoryMap.keySet());
		
		//System.out.println("mapKeys : " +mapKeys);
		//System.out.println("itemKeys : " + items.keySet());

		for (String itemKey : items.keySet()){
			//System.out.println(itemKey + ":"+items.get(itemKey)+"-->" + queueMemoryMap.containsKey(itemKey));
			queueMemoryMap.get(itemKey).push(items.get(itemKey));
			mapKeys.remove(itemKey);
		}
		//TODO throw a warnings
		if (! mapKeys.isEmpty()){
			String sep="";
			for (String mapKey : mapKeys){
				System.out.print(sep + mapKey);
				sep=" ;";
			}
			System.out.println( " not updated with others");
		}
	}

	public Map<String, Double> getGeometricMeans() {
		HashMap<String, Double> resultat= new HashMap<String, Double>();
		for (String basinName : queueMemoryMap.keySet()){
			resultat.put(basinName, queueMemoryMap.get(basinName).getGeometricMean());
		}
		return resultat;
	}
	
	public Map<String, Double> getMeans() {
		HashMap<String, Double> resultat= new HashMap<String, Double>();
		for (String basinName : queueMemoryMap.keySet()){
			resultat.put(basinName, queueMemoryMap.get(basinName).getMean());
		}
		return resultat;
	}

	public Set<String> keySet(){
		return (queueMemoryMap.keySet());
	}
	
	public QueueMemory<E> get(String key){
		return (queueMemoryMap.get(key));
	}
}
