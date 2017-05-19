package species;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import fr.cemagref.simaqualife.kernel.processes.AquaNismsGroupProcess;

public class PlopProcess extends AquaNismsGroupProcess<DiadromousFish, DiadromousFishGroup> {

	private int temporisation = 3000; // in ms
	
	public static void main(String[] args) {
		System.out.println((new XStream(new DomDriver()))
				.toXML(new PlopProcess()));
	}	
	
	@Override
	public void doProcess(DiadromousFishGroup arg0) {
    	try {
			Thread.sleep(temporisation);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	

}
