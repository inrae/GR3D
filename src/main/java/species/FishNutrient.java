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

import species.DiadromousFish.Gender;
import species.DiadromousFish.Stage;

/**
 *
 */
public class FishNutrient {

	private double aLWfemale = 0.0221; // param�tre "a" de la relation taille/poids avec Lt en cm
	private double bLWfemale  = 2.8147; // param�tre "b" de la relation taille/poids
	private double GSIfemale =.15;
	
	private double aLWmale = 0.0221; // param�tre "a" de la relation taille/poids avec Lt en cm
	private double bLWmale  = 2.8147; // param�tre "b" de la relation taille/poids
	private double GSImale =.07;

	/**
	 * 
	 */
	public FishNutrient() {
		// TODO Auto-generated constructor stub
	}

	public void computeNP(DiadromousFish fish) {
		double totalWeightPre , totalWeightPost;
		
		if (fish.getStage() == Stage.MATURE) {
			if (fish.getGender() == Gender.FEMALE ) {
				totalWeightPre = aLWfemale * Math.pow(fish.getLength(), bLWfemale);
				totalWeightPost = totalWeightPre * GSIfemale;
			}
			else if (fish.getGender() == Gender.MALE) {
				totalWeightPre = aLWmale * Math.pow(fish.getLength(), bLWmale);
				totalWeightPost =  totalWeightPre * GSImale;
			}
			else {
				totalWeightPre = Double.NaN;
				totalWeightPost = 0.;
			}
			
		}
	}
}