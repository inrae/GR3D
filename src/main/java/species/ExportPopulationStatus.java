package species;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import environment.BasinNetworkReal;
import environment.RiverBasin;
import environment.Time;
import environment.Time.Season;
import fr.cemagref.simaqualife.kernel.processes.AquaNismsGroupProcess;
import fr.cemagref.simaqualife.pilot.Pilot;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class ExportPopulationStatus extends AquaNismsGroupProcess<DiadromousFish, DiadromousFishGroup> {

	private String fileNameMortality = "mortalty";
	private String fileNameMortalityCrash = "mortalityCrash";
	private String fileNameStockTrap = "stockTrap";
	private String fileNamePopulationStatus = "populationStatus";
	private Season reproductionSeason = Season.SPRING;

	private transient BufferedWriter bWMortality;
	private transient BufferedWriter bWMortalityCrash;
	private transient BufferedWriter bWStockTrap;
	private transient BufferedWriter bwPopulationStatus;

	private transient String sep;

	public static void main(String[] args) {
		System.out.println((new XStream(new DomDriver())).toXML(new ExportPopulationStatus()));
	}

	@Override
	public void initTransientParameters(Pilot pilot) {
		super.initTransientParameters(pilot);
		sep=";";
	}

	@Override
	public void doProcess(DiadromousFishGroup group) {
		try {	
			if (fileNameMortality != null){
				if  (bWMortality==null) {

					new File(group.getOutputPath()+fileNameMortality).getParentFile().mkdirs();
					bWMortality = new BufferedWriter(new FileWriter(new File(group.getOutputPath()+
							fileNameMortality +group.getSimulationId()+ ".csv")));

					bWMortality.write("timestep"+sep+"year"+sep+"season");

					for (String name :group.getEnvironment().getRiverBasinNames()){
						bWMortality.write(sep+name);
					}
					bWMortality.write("\n");
				}
			}

			if (fileNameMortalityCrash != null){
				if  (bWMortalityCrash==null) {

					new File(group.getOutputPath()+fileNameMortalityCrash).getParentFile().mkdirs();
					bWMortalityCrash = new BufferedWriter(new FileWriter(new File(group.getOutputPath()+
							fileNameMortalityCrash +group.getSimulationId()+ ".csv")));

					bWMortalityCrash.write("timestep"+sep+"year"+sep+"season");

					for (String name :group.getEnvironment().getRiverBasinNames()){
						bWMortalityCrash.write(sep+name);
					}
					bWMortalityCrash.write("\n");
				}
			}

			if (fileNameStockTrap != null){
				if  (bWStockTrap==null) {

					new File(group.getOutputPath()+fileNameStockTrap).getParentFile().mkdirs();
					bWStockTrap = new BufferedWriter(new FileWriter(new File(group.getOutputPath()+
							fileNameStockTrap +group.getSimulationId()+ ".csv")));

					bWStockTrap.write("timestep"+sep+"year"+sep+"season");

					for (String name :group.getEnvironment().getRiverBasinNames()){
						bWStockTrap.write(sep+name);
					}
					bWStockTrap.write("\n");
				}
			}

			if (fileNamePopulationStatus != null){
				if  (bwPopulationStatus==null) {

					new File(group.getOutputPath()+fileNamePopulationStatus).getParentFile().mkdirs();
					bwPopulationStatus = new BufferedWriter(new FileWriter(new File(group.getOutputPath()+
							fileNamePopulationStatus +group.getSimulationId()+ ".csv")));

					bwPopulationStatus.write("timestep"+sep+"year"+sep+"season");

					for (String name :group.getEnvironment().getRiverBasinNames()){
						bwPopulationStatus.write(sep+name);
					}
					bwPopulationStatus.write("\n");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}



		Time time = group.getEnvironment().getTime();
		if (time.getSeason(group.getPilot()) == reproductionSeason){
			try {
				if(bWMortality != null){
					bWMortality.write(group.getPilot().getCurrentTime()+sep+time.getYear(group.getPilot()));
					bWMortality.write(sep+time.getSeason(group.getPilot()));
					for (RiverBasin basin :group.getEnvironment().getRiverBasins()){
						bWMortality.write(sep+basin.getNativeSpawnerMortality());
					}
					bWMortality.write("\n");
				}

				if(bWMortalityCrash != null){
					bWMortalityCrash.write(group.getPilot().getCurrentTime()+sep+time.getYear(group.getPilot()));
					bWMortalityCrash.write(sep+time.getSeason(group.getPilot()));
					for (RiverBasin basin :group.getEnvironment().getRiverBasins()){
						bWMortalityCrash.write(sep+basin.getMortalityCrash());
					}
					bWMortalityCrash.write("\n");
				}

				if(bWStockTrap != null){
					bWStockTrap.write(group.getPilot().getCurrentTime()+sep+time.getYear(group.getPilot()));
					bWStockTrap.write(sep+time.getSeason(group.getPilot()));
					for (RiverBasin basin :group.getEnvironment().getRiverBasins()){
						bWStockTrap.write(sep+basin.getStockTrap());
					}
					bWStockTrap.write("\n");
				}

				if(bwPopulationStatus != null){
					bwPopulationStatus.write(group.getPilot().getCurrentTime()+sep+time.getYear(group.getPilot()));
					bwPopulationStatus.write(sep+time.getSeason(group.getPilot()));
					for (RiverBasin basin :group.getEnvironment().getRiverBasins()){
						bwPopulationStatus.write(sep+basin.getPopulationStatus());
					}
					bwPopulationStatus.write("\n");
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// close the output file 
		try {

			if (group.getPilot().getCurrentTime()== group.getPilot().getSimBegin()+group.getPilot().getSimDuration()-1){
				bWMortality.close();
			}

			if (group.getPilot().getCurrentTime()== group.getPilot().getSimBegin()+group.getPilot().getSimDuration()-1){
				bWMortalityCrash.close();
			}

			if (group.getPilot().getCurrentTime()== group.getPilot().getSimBegin()+group.getPilot().getSimDuration()-1){
				bWStockTrap.close();
			}

			if (group.getPilot().getCurrentTime()== group.getPilot().getSimBegin()+group.getPilot().getSimDuration()-1){
				bwPopulationStatus.close();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
