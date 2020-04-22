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

public class ExportMeanRecruitment extends AquaNismsGroupProcess<DiadromousFish, DiadromousFishGroup> {

	private String fileNameOutput = "meanrecruit";
	private Season reproductionSeason = Season.SPRING;

	private transient BufferedWriter bW;
	private transient String sep=";";

	public static void main(String[] args) {
		System.out.println((new XStream(new DomDriver())).toXML(new ExportMeanRecruitment()));
	}

	@Override
	public void initTransientParameters(Pilot pilot) {
		super.initTransientParameters(pilot);
		sep=";";
	}

	@Override
	public void doProcess(DiadromousFishGroup group) {

		//double[] meanLastRec = group.getEnvironment().getMeanLastRecruitments();

		if (bW==null){
			if (fileNameOutput != null){
				try {		
					new File(group.getOutputPath()+fileNameOutput).getParentFile().mkdirs();
					bW = new BufferedWriter(new FileWriter(new File(group.getOutputPath()+
							fileNameOutput +group.getSimulationId()+ ".csv")));

					bW.write("timestep"+sep+"year"+sep+"season");

					for (String name :group.getEnvironment().getRiverBasinNames()){
						bW.write(sep+name);
					}
					bW.write("\n");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		Time time = group.getEnvironment().getTime();
		if (time.getSeason(group.getPilot()) == reproductionSeason){
			try {
				bW.write(group.getPilot().getCurrentTime()+sep+time.getYear(group.getPilot()));
				bW.write(sep+time.getSeason(group.getPilot()));

				for (RiverBasin basin :group.getEnvironment().getRiverBasins()){
					bW.write(sep+basin.getLastRecruitments().getMean());
				}
				bW.write("\n");

				if (group.getPilot().getCurrentTime()== group.getPilot().getSimBegin()+group.getPilot().getSimDuration()-1){
					bW.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}


		// close the output file 
		try {
			if (group.getPilot().getCurrentTime()== group.getPilot().getSimBegin()+group.getPilot().getSimDuration()-1){
				bW.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
