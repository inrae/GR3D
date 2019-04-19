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

	private double aLWfemalePre = 0.0221; // paramï¿½tre "a" de la relation taille/poids avec Lt en cm
	private double bLWfemalePre  = 2.8147; // paramï¿½tre "b" de la relation taille/poids
	private double GSIfemalePre =.15;	
	private double aLWmalePre = 0.0221; // paramï¿½tre "a" de la relation taille/poids avec Lt en cm
	private double bLWmalePre  = 2.8147; // paramï¿½tre "b" de la relation taille/poids
	private double GSImalePre =.07;

	// Si on ne possÃ¨de pas wT post reproduction 

	/**
	 *  paramï¿½tre "a" de la relation taille/poids avec Lt en cm
	 */
	private double aLWfemalePost = 0.; // paramï¿½tre "a" de la relation taille/poids avec Lt en cm
	private double bLWfemalePost  = 0.; // paramï¿½tre "b" de la relation taille/poids
	private double GSIfemalePost=0.10;	
	private double aLWmalePost = 0.; // paramï¿½tre "a" de la relation taille/poids avec Lt en cm
	private double bLWmalePost = 0.; // paramï¿½tre "b" de la relation taille/poids
	private double GSImalePost =.07;

	//Si on possede WT post reproduction 

	private double CoeffLossWeight = 0.; 

	//Valeurs de Haskell pour A. sapidissima -- A rechercher pour Alosa alosa

	private double compoNpreMale = 2.921; 
	private double compoPpreMale = 0.662;
	private double compoNpreFemale = 2.917;
	private double compoPpreFemale = 0.725;

	private double compoNpostMale = 2.790 ;
	private double compoPpostMale = 0.961;
	private double compoNpostFemale = 3.216 ;
	private double compoPpostFemale = 0.997;

	private double compoNoocyte =0.0;
	private double compoNsperm =0.0;
	private double compoPoocyte =0.0;
	private double compoPsperm =0.0;


	/**
	 * 
	 */
	public FishNutrient() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * compute the N and P fluxes for a single fish (in the super individual)
	 *  that dies after spawning (gametes expelling) 
	 * @param fish
	 */

	public double[] computeInportNPforDeathBeforeSpawning(DiadromousFish fish) {
		double totalWeightPre; 

		double [] inportNP = new double[2]; 
		inportNP[0] = 0; 
		inportNP[1] = 0; 

		if (fish.getStage()== Stage.MATURE) {
			if(fish.getGender()== Gender.FEMALE) {
				totalWeightPre = aLWfemalePre * Math.pow(fish.getLength(), bLWfemalePre);
				//totalWeightPost = totalWeightPre * (1-GSIfemalePost)+ totalWeightPost * GSIfemalePost * CoeffLossWeight
				inportNP[0] = totalWeightPre * compoNpreFemale;
				inportNP[1] = totalWeightPre * compoPpreFemale;
			}
			else if (fish.getGender()== Gender.MALE) {
				totalWeightPre = aLWmalePre * Math.pow(fish.getLength(), bLWmalePre);
				//totalWeightPost = totalWeightPre * (1-GSImalePost)+ totalWeightPost * GSImalePost * CoeffLossWeight
				inportNP[0] = totalWeightPre * compoNpreMale; 
				inportNP[1] = totalWeightPre * compoPpreMale; 	
			}
			else {
				totalWeightPre= Double.NaN; 
				inportNP[0] = 0.; 
				inportNP[1] = 0; 
			}
		}
		return inportNP;

	}

	public double[] computeInportNPforDeathAfterSpawning(DiadromousFish fish) {
		double totalWeightPre, totalWeightPost;

		double[] inportNP = new double[2]; //Pour faire un tableau à deux colonnes, N et P 
		inportNP[0] = 0.;
		inportNP[1] = 0.;

		if (fish.getStage() == Stage.MATURE) {
			if (fish.getGender() == Gender.FEMALE ) {
				totalWeightPre = aLWfemalePre * Math.pow(fish.getLength(), bLWfemalePre);
				totalWeightPost = aLWfemalePost * Math.pow(fish.getLength(), bLWfemalePost);
				//totalWeightPost = totalWeightPre * (1-GSIfemalePost)+ totalWeightPost * GSIfemalePost * CoeffLossWeight
				inportNP[0] = totalWeightPost * compoNpostFemale + (totalWeightPre - totalWeightPost) * compoNoocyte;
				inportNP[1] = totalWeightPost * compoPpostFemale + (totalWeightPre - totalWeightPost) * compoPoocyte;
			}
			else if (fish.getGender() == Gender.MALE) {
				totalWeightPre = aLWmalePre * Math.pow(fish.getLength(), bLWmalePre);
				totalWeightPost = aLWmalePost * Math.pow(fish.getLength(), bLWmalePost);
				//totalWeightPost = totalWeightPre * (1-GSImalePost)+ totalWeightPost * GSImalePost * CoeffLossWeight
				inportNP[0] = totalWeightPost * compoNpostMale + (totalWeightPre - totalWeightPost) * compoNsperm;
				inportNP[1] = totalWeightPost * compoPpostMale + (totalWeightPre - totalWeightPost) * compoPsperm;
			}
			else {
				totalWeightPre = Double.NaN;
				totalWeightPost = 0.;
				inportNP[0]=0.;
				inportNP[1]=0.;
			}
		}

		return inportNP;	
	}

	public double [] computeInportNPforSurvivalAfterSpawning(DiadromousFish fish) {
		double totalWeightPre, totalWeightPost;

		double[]inportNP = new double [2]; 
		inportNP[0]= 0;
		inportNP[1] = 0; 

		if (fish.getStage()== Stage.MATURE) {
			if (fish.getGender()==Gender.FEMALE){
				totalWeightPre = aLWfemalePre * Math.pow(fish.getLength(), bLWfemalePre);
				totalWeightPost = aLWfemalePost * Math.pow(fish.getLength(), bLWfemalePost);
				//totalWeightPost = totalWeightPre * (1-GSImalePost)+ totalWeightPost * GSImalePost * CoeffLossWeight
				inportNP[0] = (totalWeightPre - totalWeightPost)* compoNoocyte; 
				inportNP[1] = (totalWeightPre - totalWeightPost)* compoPoocyte; 	
			}
			else if (fish.getGender()== Gender.MALE) {

				totalWeightPre = aLWfemalePre * Math.pow(fish.getLength(), bLWmalePre);
				totalWeightPost = aLWfemalePost * Math.pow(fish.getLength(), bLWmalePost);
				//totalWeightPost = totalWeightPre * (1-GSImalePost)+ totalWeightPost * GSImalePost * CoeffLossWeight
				inportNP[0] = (totalWeightPre - totalWeightPost)* compoNsperm; 
				inportNP[1] = (totalWeightPre - totalWeightPost)* compoPsperm; 

			}
			else {
				totalWeightPre= Double.NaN; 
				totalWeightPost = 0.; 
				inportNP[0] = 0.; 
				inportNP[1] = 0;

			}
		}

		return inportNP; 
	}

} 





