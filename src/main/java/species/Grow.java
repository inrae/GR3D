package species;


import java.util.List;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import environment.Basin;
import environment.Time;
import fr.cemagref.simaqualife.kernel.AquaNismsGroup;
import fr.cemagref.simaqualife.kernel.processes.AquaNismsGroupProcess;
import fr.cemagref.simaqualife.kernel.util.TransientParameters.InitTransientParameters;
import fr.cemagref.simaqualife.pilot.Pilot;
import miscellaneous.Miscellaneous;

import org.openide.util.lookup.ServiceProvider;

import species.DiadromousFish.Stage;
import umontreal.iro.lecuyer.probdist.NormalDist;
import umontreal.iro.lecuyer.randvar.NormalGen;

@ServiceProvider(service = AquaNismsGroupProcess.class)
public class Grow extends AquaNismsGroupProcess<DiadromousFish, DiadromousFishGroup> {

	private double tempMinGrow = 3.;
	private double tempMaxGrow = 26.; 
	private double tempOptGrow = 17.;
	private double kOpt = 0.3;
	private double sigmaDeltaLVonBert = 0.2; // random value... has to be fixed with literature 

	private transient NormalGen genNormal;

	public static void main(String[] args) { System.out.println((new
			XStream(new DomDriver())) .toXML(new Grow())); }

	@Override
	@InitTransientParameters
	public void initTransientParameters(Pilot pilot) {
            super.initTransientParameters(pilot);
		genNormal = new NormalGen( pilot.getRandomStream(),
				new NormalDist(0., 1.));		
	}

	@Override
	public void doProcess(DiadromousFishGroup group) {
                for(Basin basin : group.getEnvironment().getBasins()){
                    if (basin.getFishs(group)!=null) for(DiadromousFish fish : basin.getFishs(group)){
                        double muDeltaLVonBert = 0.;
			double kVonBert = 0.;
			double growthIncrement = 0.;
			//Grow
			// 1) We calculate the kVonBert
			
			if (group.getKOpt()==Double.NaN){
				kVonBert = kOpt * 
				Miscellaneous.temperatureEffect(fish.getPosition().getCurrentTemperature(group.getPilot()), tempMinGrow, tempOptGrow, tempMaxGrow);
			} else {
				kVonBert = group.getKOpt() * 
						Miscellaneous.temperatureEffect(fish.getPosition().getCurrentTemperature(group.getPilot()), tempMinGrow, tempOptGrow, tempMaxGrow);
			}
				
			// 2) We update the size of the fish
			if (fish.getLength() < group.getLinfVonBert()){
				muDeltaLVonBert = Math.log((group.getLinfVonBert() - fish.getLength()) * (1 - Math.exp(-kVonBert * Time.getSeasonDuration()))) - (Math.pow(sigmaDeltaLVonBert,2))/2;
				growthIncrement = Math.exp(genNormal.nextDouble()*sigmaDeltaLVonBert + muDeltaLVonBert);
				fish.setLength(Math.min(group.getLinfVonBert(), fish.getLength() + growthIncrement));											
			}else{
				fish.setLength(group.getLinfVonBert());
			}

			if (fish.getStage() == Stage.IMMATURE){
				if (fish.getLength() > group.getlFirstMaturity()){
					fish.setStage(Stage.MATURE);
				}
			}
			//System.out.println("la température du lieu de vie du poisson est :" + fish.getPosition().getCurrentTemperature() + ", la saison est :" + Time.getSeason() + " et sa nouvelle taille est :" + fish.getLength());
                    }
                }
	}
}
