package environment;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import fr.cemagref.simaqualife.kernel.util.TransientParameters.InitTransientParameters;
import fr.cemagref.simaqualife.pilot.Pilot;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Pattern;

public class BasinNetworkWithnBasins extends BasinNetwork {

	private String basinNetworkFileName ="data/input/BasinNetwork.csv";
	private String distanceGridFileName  ="data/input/distanceGrid.csv";

	public static void main(String[] args) { System.out.println((new
			XStream(new DomDriver())) .toXML(new BasinNetworkWithnBasins())); }

	@InitTransientParameters
	public void initTransientParameters(Pilot pilot) {
		grid = new Basin[nbBasin * 3];

		String name;
		double longitude=0., latitude=0., surface =0., pDam = 0., firstDamHeight =0.;
		double winterTemperature, springTemperature, summerTemperature, fallTemperature;
		//	double x, y;
		FileReader reader;
		Scanner scanner;
		try {
			// open the file
			reader = new FileReader(basinNetworkFileName);

			// Parsing the file
			scanner = new Scanner(reader);
			scanner.useLocale(Locale.ENGLISH); // to have a comma as decimal separator !!!
			scanner.useDelimiter(Pattern.compile("[;\n]"));

			// skip the first line
			scanner.nextLine();
			int index=0;
			while (scanner.hasNext()) {
				name = scanner.next();
				//System.out.print(name +" ");		
				longitude = scanner.nextDouble();
				//System.out.print(longitude +"\t");			
				latitude = scanner.nextDouble();
				//System.out.print(latitude +"\t");		
				surface = scanner.nextDouble();
				//System.out.print(surface +"\t");		
				pDam = scanner.nextDouble();
				//System.out.print(pDam +"\t");		
				firstDamHeight = scanner.nextDouble();
				//System.out.print(firstDamHeight +"\t");			
				winterTemperature = scanner.nextDouble();
				//System.out.print(winterTemperature +"\t");			
				springTemperature = scanner.nextDouble();
				//System.out.print(springTemperature +"\t");			
				summerTemperature = scanner.nextDouble();
				//System.out.print(summerTemperature +"\t");			
				fallTemperature = scanner.nextDouble();
				//System.out.print(fallTemperature +"\t");			

				// create the corresponding basins
				Basin riverBasin = new RiverBasin(pilot, index, name, 
						winterTemperature, springTemperature, summerTemperature, fallTemperature, 
						latitude, longitude, surface, firstDamHeight, pDam,
						this.memorySize, this.memorySizeLongQueue);	
				Basin offshoreBasin = new OffshoreBasin(index + 2 * nbBasin, name+"-o", 
						12., 12., 12., 12.);
				Basin seaBasin = new SeaBasin(index+ nbBasin, name+"-s", 
						(offshoreBasin.getWinterTemperature() + winterTemperature)/2., 
						(offshoreBasin.getSpringTemperature() + springTemperature)/2., 
						(offshoreBasin.getSummerTemperature() + summerTemperature)/2., 
						(offshoreBasin.getFallTemperature() + fallTemperature)/2.);

				// read the shape for each basin 
				riverBasin.getShape().moveTo(scanner.nextDouble(), scanner.nextDouble());
				for (int i =0; i<3; i++){
					riverBasin.getShape().lineTo(scanner.nextDouble(),scanner.nextDouble());
				}
				riverBasin.getShape().closePath();

				seaBasin.getShape().moveTo(scanner.nextDouble(), scanner.nextDouble());
				for (int i =0; i<3; i++){
					seaBasin.getShape().lineTo(scanner.nextDouble(),scanner.nextDouble());
				}
				seaBasin.getShape().closePath();

				offshoreBasin.getShape().moveTo(scanner.nextDouble(), scanner.nextDouble());
				for (int i =0; i<3; i++){
					offshoreBasin.getShape().lineTo(new Double(scanner.nextDouble()),new Double(scanner.next()));
				}
				offshoreBasin.getShape().closePath();

				// add the basins to the basin grid
				grid[index] = riverBasin;
				grid[index + nbBasin] = seaBasin;
				grid[index + 2* nbBasin] = offshoreBasin;

				// increment index
				index ++;
			}

			/*			for (Basin basin : grid){
				System.out.println(basin.getShape().getBounds2D().toString());
			}*/
		} 
		catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		/*		for (int i=0; i < nbBasin*3;i++){
			System.out.println(grid[i].getShape().getBounds2D().toString());
		}*/

		// fill the distanceGrid
		distanceGrid = new double[nbBasin][nbBasin];
		try {
			// open the file
			reader = new FileReader(distanceGridFileName);
			// Parsing the file
			scanner = new Scanner(reader);
			scanner.useLocale(Locale.ENGLISH); // to have a comma as decimal separator !!!
			scanner.useDelimiter(Pattern.compile("[;\n]"));

			int i, j;
			int index = 0;

			//System.out.println(scanner.nextLine());
			while (scanner.hasNext()) {
				j= index % nbBasin;
				i= (index -j) / nbBasin;
				distanceGrid[i][j] = new Double(scanner.next());
				index++;
			}

			reader.close();
		} 
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// TODO fill the disatnce in each basin
		for (Basin basin : grid){
			basin.setNeighboursDistances(getNeighboursWithDistance(basin));
		}
	}

}