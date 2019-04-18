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

	private double aLWfemalePre = 0.0221; // param�tre "a" de la relation taille/poids avec Lt en cm
	private double bLWfemalePre  = 2.8147; // param�tre "b" de la relation taille/poids
	private double GSIfemalePre =.15;	
	private double aLWmalePre = 0.0221; // param�tre "a" de la relation taille/poids avec Lt en cm
	private double bLWmalePre  = 2.8147; // param�tre "b" de la relation taille/poids
	private double GSImalePre =.07;

	// Si on ne possède pas wT post reproduction 
	
		private double aLWfemalePost = 0.; // param�tre "a" de la relation taille/poids avec Lt en cm
		private double bLWfemalePost  = 0.; // param�tre "b" de la relation taille/poids
		private double GSIfemalePost=0.;	
		private double aLWmalePost = 0.; // param�tre "a" de la relation taille/poids avec Lt en cm
		private double bLWmalePost = 0.; // param�tre "b" de la relation taille/poids
		private double GSImalePost =.07;
		
	
	
	//Valeurs de Haskell pour A. sapidissima -- A rechercher pour Alosa alosa
	
		private double compoNpreMale = 2.921; 
		private double compoPpreMale = 0.662;
		private double compoNpreFemale = 2.917;
		private double compoPpreFemale = 0.725;
		
		private double compoNpostMale = 2.790 ;
		private double compoPpostMale = 0.961;
		private double compoNpostFemale = 3.216 ;
		private double compoPpostFemale = 0.997;


	/**
	 * 
	 */
	public FishNutrient() {
		// TODO Auto-generated constructor stub
	}

	public void computeNP(DiadromousFish fish) {
		double totalWeightPre, totalWeightPost;
		
		if (fish.getStage() == Stage.MATURE) {
			if (fish.getGender() == Gender.FEMALE ) {
				totalWeightPre = aLWfemalePre * Math.pow(fish.getLength(), bLWfemalePre);
				totalWeightPost = aLWfemalePost * Math.pow(fish.getLength(), bLWfemalePost);
				// totalWeightPost = totalWeightPre * GSIfemalePre;
			}
			else if (fish.getGender() == Gender.MALE) {
				totalWeightPre = aLWmalePre * Math.pow(fish.getLength(), bLWmalePre);
				totalWeightPost = aLWmalePost * Math.pow(fish.getLength(), bLWmalePost);
				// totalWeightPost =  totalWeightPre * GSImalePre;
			}
			else {
				totalWeightPre = Double.NaN;
				totalWeightPost = 0.;
			}
			
		}
	}
}