package species;


import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import environment.Basin;

import environment.Basin.TypeBassin;
import environment.Time;
import fr.cemagref.simaqualife.kernel.processes.AquaNismsGroupProcess;

import miscellaneous.Miscellaneous;

import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = AquaNismsGroupProcess.class)
public class Survive extends AquaNismsGroupProcess<DiadromousFish, DiadromousFishGroup> {

	public double tempMinMortGenInRiv = 10.;
	public double tempMaxMortGenInRiv = 23.; // DEFINIR TROIS AUTRES POUR MORTALITE
	public double tempOptMortGenInRiv = 20.;
	public double survivalProbOptGenInRiv = 1.;

	public double mortalityRateInRiver = 0.4;
	public double mortalityRateInSea = 0.4;
	public double mortalityRateInOffshore = 0.4;

	public static void main(String[] args) { System.out.println((new
			XStream(new DomDriver())) .toXML(new Survive())); }

	@Override
	public void doProcess(DiadromousFishGroup group) {
		// TODO Auto-generated method stub
		double survivalProbability;
		List<DiadromousFish> deadFish = new ArrayList<DiadromousFish>();
		long survivalAmount; 

		for(Basin basin : group.getEnvironment().getBasins()){
                    if (basin.getFishs(group)!=null) for(DiadromousFish fish : basin.getFishs(group)){
			survivalProbability = 1.;
			//Survive
			if(fish.getPosition().getType() == TypeBassin.RIVER && fish.isMature()){				
				double tempEffectSurv = Miscellaneous.temperatureEffect(fish.getPosition().getCurrentTemperature(group.getPilot()), tempMinMortGenInRiv, tempOptMortGenInRiv, tempMaxMortGenInRiv);
				if (tempEffectSurv == 0.){
					survivalProbability = 0.;
					//System.out.println("le poisson situé dans le bassin " + fish.getPosition().getName() + " en " + Time.getSeason() +" a un coeff de mortalité de " + fish.getMortalityRateInRiver() + " mais à cause de la température une prob de survie de " + survivalProbability);
				}else{
					survivalProbability = survivalProbOptGenInRiv * tempEffectSurv;
					//System.out.println("le poisson situé dans le bassin " + fish.getPosition().getName() + " en " + Time.getSeason() + " a un coeff de mortalité de " + fish.getMortalityRateInRiver() + " et donc une prob de survie de " + survivalProbability);
				}				
			}else if (fish.getPosition().getType() == TypeBassin.SEA){ 
				survivalProbability = Math.exp(-mortalityRateInSea * Time.getSeasonDuration());
				//System.out.println("le poisson situé dans le bassin " + fish.getPosition().getName() + " en " + Time.getSeason() + " a un coeff de mortalité de " + fish.getMortalityRateInSea() + " et donc une prob de survie de " + survivalProbability);
			}else if (fish.getPosition().getType() == TypeBassin.OFFSHORE){
				survivalProbability = Math.exp(-mortalityRateInOffshore * Time.getSeasonDuration());
				//System.out.println("le poisson situé dans le bassin " + fish.getPosition().getName() + " en " + Time.getSeason() + " a un coeff de mortalité de " + fish.getMortalityRateInOffshore() + " et donc une prob de survie de " + survivalProbability);
			}else{
				survivalProbability = 1.;
			}

			if (survivalProbability<1.){
				survivalAmount = Miscellaneous.binomialForSuperIndividual(group.getPilot(), fish.getAmount(), survivalProbability);

				if (survivalAmount > 0) 
					fish.setAmount(survivalAmount);
				else
					deadFish.add(fish);
			}
                    }
                }

		for (DiadromousFish fish : deadFish){
			group.removeAquaNism(fish);
		}
	}
}
