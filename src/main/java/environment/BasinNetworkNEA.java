package environment;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;


import fr.cemagref.simaqualife.kernel.util.TransientParameters.InitTransientParameters;
import fr.cemagref.simaqualife.pilot.Pilot;

import java.awt.geom.Path2D;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.regex.Pattern;

import miscellaneous.QueueMemoryMap;
import species.ExportPopulationStatus;

import org.geotools.data.FeatureSource;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.FeatureIterator;
import org.locationtech.jts.geom.MultiPolygon;
import org.opengis.feature.simple.SimpleFeature;

public class BasinNetworkNEA extends BasinNetwork {

	private String basinFile = "data/input/northeastamerica/nea_basins.csv";
	private String seaBasinShpFile = "data/input/northeastamerica/shape/seabasins.shp";
	private String riverBasinShpFile = "data/input/northeastamerica/shape/riverbasins.shp";

	private String distanceGridFileName = "data/input/northeastamerica/distanceGridNEA.csv";
	private String temperatureCatchmentFile = "data/input/northeastamerica/basins_temperatures.csv";

	private boolean useRealPDam = false;

	private transient Map<Long, Map<String, Double[]>> temperatureSeries;

	class Record implements Comparable<Record> {

		private int order;
		private int basin_id;
		private String name;
		private double longitude;
		private double latitude;
		private double surface;
		private double pDam;


		Record(int order, int basin_id, String name, double longitude, double latitude, double surface, double pDam) {
			this.order = order;
			this. basin_id = basin_id;
			this.name = name;
			this.longitude = longitude;
			this.latitude = latitude;
			this.surface = surface;
			this.pDam = pDam;
		}

		public int compareTo(Record rec) {
			return this.order - rec.order;
		}

		public int getOrder() {
			return order;
		}
		
		/**
		 * @return the basin_id used in database
		 */
		public int getBasin_id() {
			return basin_id;
		}

		public String getName() {
			return name;
		}

		public double getLongitude() {
			return longitude;
		}

		public double getLatitude() {
			return latitude;
		}

		public double getSurface() {
			return surface;
		}

		public double getPDam(){
			return pDam;
		}
	}

	public static void main(String[] args) {
		System.out.println((new XStream(new DomDriver())).toXML(new BasinNetworkNEA()));
	}

	public Map<String, Double[]> getTemperaturesBasin(long year) {
		return temperatureSeries.get(year);
	}

	private Map<String, Path2D.Double> loadBasins(String basinShpFile) {
		Map<String, Path2D.Double> mapBasin = new HashMap<String, Path2D.Double>();
		ShapefileDataStore basinStore = null;
		SimpleFeatureIterator  iterator = null;
		try {
			File aFile = new File(basinShpFile);
			basinStore = new ShapefileDataStore(aFile.toURI().toURL());

			ContentFeatureSource  featureBasin = basinStore.getFeatureSource();
			iterator = featureBasin.getFeatures().features();
			while (iterator.hasNext()) {
				SimpleFeature feature = iterator.next();
				MultiPolygon multiPolygon = (MultiPolygon) feature.getDefaultGeometry();
				// build the shape for each basin
				Path2D.Double shape = new Path2D.Double();
				shape.moveTo((multiPolygon.getCoordinates())[0].x, (multiPolygon.getCoordinates())[0].y);
				for (int i = 1; i < multiPolygon.getCoordinates().length; i++) {
					shape.lineTo((multiPolygon.getCoordinates())[i].x, (multiPolygon.getCoordinates())[i].y);
				}
				shape.closePath();
				mapBasin.put((String) feature.getAttribute("NOM"), shape);
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		} finally {
			iterator.close();
			basinStore.dispose();
		}

		return mapBasin;
	}

	@InitTransientParameters
	public void initTransientParameters(Pilot pilot) {

		FileReader reader;
		Scanner scanner;

		// shape files can be omitted (for batch mode)
		// load the shape of the seaBasin
		Map<String, Path2D.Double> mapSeaBasin = null;
		//String cwd = System.getProperty("user.dir").concat("/");
		//System.out.println(cwd);
		if (seaBasinShpFile != null && seaBasinShpFile.length() > 0) {
			mapSeaBasin = loadBasins(seaBasinShpFile);
		}
		// load the shape of the riverBasin
		Map<String, Path2D.Double> mapRiverBasin = null;
		if (riverBasinShpFile != null && riverBasinShpFile.length() > 0) {
			mapRiverBasin = loadBasins(riverBasinShpFile);
		}
		// load the feature of the riverBasin
		String name;
		double pDam = 1, pAttractive = 1;
		double longitude, latitude, surface = 0., firstDamHeight = 0.;
		int order, basin_id;
		double winterTemperature = 5., springTemperature = 10., summerTemperature = 20., fallTemperature = 15.;
		List<Record> records = new ArrayList<Record>();
		try {
			// open the file
			reader = new FileReader(basinFile);
			// Parsing the file
			scanner = new Scanner(reader);
			scanner.useLocale(Locale.ENGLISH); // to have a point as decimal separator !!!
			scanner.useDelimiter(Pattern.compile("[;,\r]"));

			//System.out.println(scanner.nextLine());
			scanner.nextLine(); // skip the first line with headers
			while (scanner.hasNext()) {
				basin_id = scanner.nextInt(); // gid
				name = scanner.next();
				longitude = scanner.nextDouble();
				latitude = scanner.nextDouble();
				surface = scanner.nextDouble();
				order = scanner.nextInt();
				if (useRealPDam == true){
					pDam=scanner.nextDouble();
				}               	
				else {
					scanner.next();
					pDam=1.;} // skip pDam reel
				scanner.nextLine();
				//System.out.println(order);
				records.add(new Record(order, basin_id, name, longitude, latitude, surface, pDam));
			}
			reader.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// sort the record according to the order field (north to south)
		Collections.sort(records);

		// number of basin is equal to the size of the list
		nbBasin = records.size();
		grid = new Basin[nbBasin * 2];

		// create the basin
		for (int index = 0; index < nbBasin; index++) {
			Record record = records.get(index);

			Basin riverBasin = new RiverBasin(pilot, index, record.getName(), record.getBasin_id(),
					winterTemperature, springTemperature, summerTemperature, fallTemperature,
					record.getLatitude(), record.getLongitude(), record.getSurface(), firstDamHeight, record.getPDam(), pAttractive,
					this.memorySize, this.memorySizeLongQueue);
			//Basin offshoreBasin = new OffshoreBasin(index + 2 * nbBasin, name+"-o", 
			//		12., 12., 12., 12.);
			Basin seaBasin = new SeaBasin(index + nbBasin, record.getName() + "-s",
					(12. + winterTemperature) / 2.,
					(12. + springTemperature) / 2.,
					(12. + summerTemperature) / 2.,
					(12. + fallTemperature) / 2.);
			// append the shape for each basin
			//System.out.println(record.getName());
			if (mapRiverBasin != null) {
				riverBasin.getShape().append(mapRiverBasin.get(record.getName()), true);
			}
			if (mapSeaBasin != null) {
				seaBasin.getShape().append(mapSeaBasin.get(record.getName()), true);
			}
			// add the basins to the basin grid
			grid[index] = riverBasin;
			grid[index + nbBasin] = seaBasin;
		}

		// fill the distanceGrid
		distanceGrid = new double[nbBasin][nbBasin];
		try {
			// open the file
			reader = new FileReader(distanceGridFileName);
			// Parsing the file
			scanner = new Scanner(reader);
			scanner.useLocale(Locale.ENGLISH); // to have a point as decimal separator !!!
			scanner.useDelimiter(Pattern.compile("[;,\r]"));

			// skip the first line with headers
			scanner.nextLine();
			int i, j;
			int index = 0;
			while (scanner.hasNext() & index < Math.pow(nbBasin, 2.)) {
				j = index % nbBasin;
				i = (index - j) / nbBasin;
				if (i ==0 ) 
					scanner.next(); // to skip the first column
				//System.out.println("i"+i+"j"+j+"index"+index);
				distanceGrid[i][j] = Double.valueOf(scanner.next());
				index++;
			}
			
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		// TODO fill the distance in each basin
		for (Basin basin : grid) {
			basin.setNeighboursDistances(getNeighboursWithDistance(basin));
		}

		// create a QueueMemoryMap to keep the spawnerOrigine in each basin
		List<String> basinNames = new ArrayList<String>();
		for (RiverBasin basin : this.getRiverBasins()){
			basinNames.add(basin.getName());
		}
		for (RiverBasin basin : this.getRiverBasins()){
			basin.setSpawnerOrigins(new QueueMemoryMap<Long>(basinNames, memorySize));
		}

		// fill the temperatures series
		temperatureSeries = new TreeMap<Long, Map<String, Double[]>>();
		Long year;
		try {
			// open the file
			reader = new FileReader(temperatureCatchmentFile);
			// Parsing the file
			scanner = new Scanner(reader);
			scanner.useLocale(Locale.ENGLISH); // to have a point as decimal separator !!!
			scanner.useDelimiter(Pattern.compile("[,;\r]"));
			char[] charac = {'"'};
			String doublequote = new String(charac);
			scanner.nextLine(); // skip the first line with headers
			while (scanner.hasNext()) {
				scanner.next(); // skip gid
				name = scanner.next();
				name = name.replaceAll(new String(doublequote), "");
				scanner.next(); //skip id
				//System.out.println(scanner.next());
				year = (long) scanner.nextInt();
				Double[] seasonalTemperature = new Double[4];
				seasonalTemperature[0] = scanner.nextDouble();
				seasonalTemperature[1] = scanner.nextDouble();
				seasonalTemperature[2] = scanner.nextDouble();
				seasonalTemperature[3] = scanner.nextDouble();
				//scanner.nextLine();

				Map<String, Double[]> temperatureYear = temperatureSeries.get(year);
				if (temperatureYear == null) {
					temperatureYear = new TreeMap<String, Double[]>();
					temperatureYear.put(name, seasonalTemperature);
					temperatureSeries.put(year, temperatureYear);
				} else {
					temperatureYear.put(name, seasonalTemperature);
					temperatureSeries.put(year, temperatureYear);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * @return the temperatureCatchmentFile
	 */
	public String getTemperatureCatchmentFile() {
		return temperatureCatchmentFile;
	}
	
}


