package environment;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import fr.cemagref.simaqualife.kernel.util.TransientParameters.InitTransientParameters;
import fr.cemagref.simaqualife.pilot.Pilot;

public class BasinNetworkWith2Basins extends BasinNetwork {


	private String nameOfBv[] = {"BV1", "BV2"};
	private double surfaceOfBv1 = 10000.;
	private double surfaceOfBv2 = 10000.;
	private double interDistance =100.;
	private double winterTemperatureOfBv1 =8.7;
	private double springTemperatureOfBv1 = 17.4;
	private double summerTemperatureOfBv1 = 23.1;
	private double fallTemperatureOfBv1 = 13.;  // Gironde estuary values : We used daily estimates of estuarine water temperature (°C) close to the Blayais nuclear power plant provided by EDF (Electricité De France) between 1991 and 2009
	private double winterTemperatureOfBv2 =8.7;
	private double springTemperatureOfBv2 = 17.4;
	private double summerTemperatureOfBv2 = 23.1;
	private double fallTemperatureOfBv2 = 13.; 
	
	public static void main(String[] args) { System.out.println((new
			XStream(new DomDriver())) .toXML(new BasinNetworkWith2Basins())); }

	@InitTransientParameters
	public void initTransientParameters(Pilot pilot) {
		nbBasin = 2;


		double surfaceOfBv[] = {surfaceOfBv1, surfaceOfBv2};
		double winterTemperature[] = {winterTemperatureOfBv1, winterTemperatureOfBv2};
		double springTemperature[] = {springTemperatureOfBv1, springTemperatureOfBv2};
		double summerTemperature[] = {summerTemperatureOfBv1, summerTemperatureOfBv2};
		double fallTemperature[] = {fallTemperatureOfBv1, fallTemperatureOfBv2};
		Double longitude=0., pDam = 0., firstDamHeight =0.;
		grid = new Basin[nbBasin * 3];
		int index=0;
		while (index < nbBasin) {
			double latitude = index * interDistance;
			// create the corresponding basins
			Basin riverBasin = new RiverBasin(pilot, index, nameOfBv[index], 
					winterTemperature[index], springTemperature[index], summerTemperature[index], fallTemperature[index], 
					latitude, longitude, surfaceOfBv[index], firstDamHeight, pDam,
					this.memorySize, this.memorySizeLongQueue);	
			Basin offshoreBasin = new OffshoreBasin(index + 2 * nbBasin, nameOfBv[index]+"-o", 
					12., 12., 12., 12.);
			Basin seaBasin = new SeaBasin(index+ nbBasin, nameOfBv[index]+"-s", 
					(offshoreBasin.getWinterTemperature() + winterTemperature[index])/2., 
					(offshoreBasin.getSpringTemperature() + springTemperature[index])/2., 
					(offshoreBasin.getSummerTemperature() + summerTemperature[index])/2., 
					(offshoreBasin.getFallTemperature() + fallTemperature[index])/2.);

			// add the basins to the basin grid
			grid[index] = riverBasin;
			grid[index + nbBasin] = seaBasin;
			grid[index + 2* nbBasin] = offshoreBasin;

			// increment index
			index ++;
		}

		// fill the distanceGrid
		distanceGrid = new double[nbBasin][nbBasin];
		distanceGrid[0][0] = 0.;
		distanceGrid[0][1] = interDistance;
		distanceGrid[1][0] = interDistance;
		distanceGrid[1][1] = 0.;
	;

		// TODO fill the distance in each basin
		for (Basin basin : grid){
			basin.setNeighboursDistances(getNeighboursWithDistance(basin));
		}
	}
}
