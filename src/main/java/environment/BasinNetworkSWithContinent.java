package environment;

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

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.store.ContentFeatureSource;
import org.locationtech.jts.geom.MultiPolygon;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import fr.cemagref.simaqualife.kernel.util.TransientParameters.InitTransientParameters;
import fr.cemagref.simaqualife.pilot.Pilot;
import miscellaneous.QueueMemoryMap;

public class BasinNetworkSWithContinent extends BasinNetwork {

	private String basinFile = "data/input/northeastamerica/nea_basins.csv";

	private String continentShpFile = "data/input/northeastamerica/shape/nea_continent.shp";
	private String seaBasinShpFile = "data/input/northeastamerica/shape/seabasins.shp";
	private String riverBasinShpFile = "data/input/northeastamerica/shape/riverbasins.shp";
	private String riverBasinNameLabel = "NAME";
	private String seaBasinNameLabel = "name";
	private String continentPathName = "PATH";

	private String distanceGridFileName = "data/input/northeastamerica/distanceGridNEA.csv";
	private String temperatureCatchmentFile = "data/input/northeastamerica/basins_temperatures.csv";

	private boolean useRealPDam = false;

	private transient Map<Long, Map<String, Double[]>> temperatureSeries;
	private transient Map<String, Path2D.Double> mapContinent;

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
			this.basin_id = basin_id;
			this.name = name;
			this.longitude = longitude;
			this.latitude = latitude;
			this.surface = surface;
			this.pDam = pDam;
		}


		@Override
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


		public double getPDam() {
			return pDam;
		}


		@Override
		public String toString() {
			return "Record [order=" + this.order + ", basin_id=" + this.basin_id + ", name=" + this.name
					+ ", longitude=" + this.longitude + ", latitude=" + this.latitude + ", surface=" + this.surface
					+ ", pDam=" + this.pDam + "]";
		}
	}


	public static void main(String[] args) {
		System.out.println((new XStream(new DomDriver())).toXML(new BasinNetworkSWithContinent()));
	}


	@Override
	public Map<String, Double[]> getTemperaturesBasin(long year) {
		return temperatureSeries.get(year);
	}


	private Map<String, Path2D.Double> loadBasins(String basinShpFile, String name) throws Exception {
		Map<String, Path2D.Double> mapBasin = new HashMap<String, Path2D.Double>();
		ShapefileDataStore basinStore = null;
		SimpleFeatureIterator iterator = null;

		try {
			File aFile = new File(basinShpFile);
			basinStore = new ShapefileDataStore(aFile.toURI().toURL());

			ContentFeatureSource featureBasin = basinStore.getFeatureSource();

			iterator = featureBasin.getFeatures().features();
			// test if the header exist for the first feature
			SimpleFeature feature = iterator.next();
			boolean test = false;

			for (Property property : feature.getProperties()) {
				if (property.getName().toString().matches(name)) {
					test = true;
					break;
				}
			}
			if (test == false) {
				StringBuilder message = new StringBuilder();
				message.append("The name header ").append(name).append(" does not exist in ").append(basinShpFile)
						.append(". Choose between ");
				for (Property property : feature.getProperties()) {
					message.append(property.getName().toString()).append(" ");
				}
				message.append("and change in the xml");
				throw new Exception(message.toString());
			}

			// do the job for the first feature
			MultiPolygon multiPolygon = (MultiPolygon) feature.getDefaultGeometry();
			// build the shape for each basin
			Path2D.Double shape = new Path2D.Double();
			shape.moveTo((multiPolygon.getCoordinates())[0].x, (multiPolygon.getCoordinates())[0].y);
			for (int i = 1; i < multiPolygon.getCoordinates().length; i++) {
				shape.lineTo((multiPolygon.getCoordinates())[i].x, (multiPolygon.getCoordinates())[i].y);
			}
			shape.closePath();
			mapBasin.put(String.valueOf(feature.getAttribute(name)), shape);
			// do the same job for the following features
			while (iterator.hasNext()) {
				feature = iterator.next();

				multiPolygon = (MultiPolygon) feature.getDefaultGeometry();
				// -- build the shape for each basin
				shape = new Path2D.Double();
				shape.moveTo((multiPolygon.getCoordinates())[0].x, (multiPolygon.getCoordinates())[0].y);
				for (int i = 1; i < multiPolygon.getCoordinates().length; i++) {
					shape.lineTo((multiPolygon.getCoordinates())[i].x, (multiPolygon.getCoordinates())[i].y);
				}
				shape.closePath();
				mapBasin.put(String.valueOf(feature.getAttribute(name)), shape);

			}
		} catch (Exception e1) {
			e1.printStackTrace();
		} finally {
			iterator.close();
			basinStore.dispose();
		}

		return mapBasin;
	}


	@Override
	@InitTransientParameters
	public void initTransientParameters(Pilot pilot) {

		super.initTransientParameters(pilot);

		FileReader reader;
		Scanner scanner;

		// =============================================
		// upload shapes
		// shape files could be omitted (for batch mode)

		// load the shape of the seaBasin
		Map<String, Path2D.Double> mapSeaBasin = null;

		if (seaBasinShpFile != null && seaBasinShpFile.length() > 0) {
			try {
				mapSeaBasin = loadBasins(seaBasinShpFile, seaBasinNameLabel);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// ----- load the shape of the riverBasin
		Map<String, Path2D.Double> mapRiverBasin = null;
		if (riverBasinShpFile != null && riverBasinShpFile.length() > 0) {
			try {
				mapRiverBasin = loadBasins(riverBasinShpFile, riverBasinNameLabel);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// ----- load the continent
		if (continentShpFile != null && continentShpFile.length() > 0) {
			try {
				mapContinent = loadBasins(continentShpFile, continentPathName);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// ===========================================
		// load features of riverBasins
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
			scanner.useLocale(Locale.ENGLISH); // to have a point as decimal
												// separator !!!
			scanner.useDelimiter(Pattern.compile("[;,\r\n]"));

			// System.out.println(scanner.nextLine());
			scanner.nextLine(); // skip the first line with headers
			while (scanner.hasNext()) {
				basin_id = scanner.nextInt(); // gid
				name = scanner.next();
				longitude = scanner.nextDouble();
				latitude = scanner.nextDouble();
				surface = scanner.nextDouble();
				order = scanner.nextInt();
				if (useRealPDam == true) {
					pDam = scanner.nextDouble();
				} else {
					scanner.next();
					pDam = 1.;
				} // skip pDam reel
				scanner.nextLine();
				// System.out.println(order);
				records.add(new Record(order, basin_id, name, longitude, latitude, surface, pDam));
			}
			reader.close();
			scanner.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// sort the record according to the order field
		Collections.sort(records);

		// number of basin is equal to the size of the list
		nbBasin = records.size();
		grid = new Basin[nbBasin * 2];

		// create the basin
		for (int index = 0; index < nbBasin; index++) {
			Record record = records.get(index);

			Basin riverBasin = new RiverBasin(pilot, index, record.getName(), record.getBasin_id(), winterTemperature,
					springTemperature, summerTemperature, fallTemperature, record.getLatitude(), record.getLongitude(),
					record.getSurface(), firstDamHeight, record.getPDam(), pAttractive, this.memorySize,
					this.memorySizeLongQueue);
			// Basin offshoreBasin = new OffshoreBasin(index + 2 * nbBasin,
			// name+"-o",
			// 12., 12., 12., 12.);
			Basin seaBasin = new SeaBasin(index + nbBasin, record.getName() + "-s", (12. + winterTemperature) / 2.,
					(12. + springTemperature) / 2., (12. + summerTemperature) / 2., (12. + fallTemperature) / 2.);

			// append the shape for each basin
			if (mapRiverBasin != null) {
				if (mapRiverBasin.containsKey(record.getName()))
					riverBasin.getShape().append(mapRiverBasin.get(record.getName()), true);
				else {
					System.out.println(record.getName() + "does not exist in river basin shape");
					System.out.println(record.toString());
					System.exit(1);
					;
				}

			}
			if (mapSeaBasin != null) {
				if (mapSeaBasin.containsKey(record.getName()))
					seaBasin.getShape().append(mapSeaBasin.get(record.getName()), true);
				else {
					System.out.println(record.getName() + "does not exist in sea basin shape");
					System.out.println(record.toString());
					System.exit(1);
					;
				}
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
			scanner.useLocale(Locale.ENGLISH); // to have a point as decimal
												// separator !!!
			scanner.useDelimiter(Pattern.compile("[;,\r\n]"));

			// skip the first line with headers
			scanner.nextLine();
			/*
			 * int i, j; int index = 0; while (scanner.hasNext() & index < Math.pow(nbBasin, 2.)) { j = index % nbBasin;
			 * i = (index - j) / nbBasin; if (j == 0) scanner.next(); // to skip the first column //
			 * System.out.println("i"+i+"j"+j+"index"+index); distanceGrid[i][j] = Double.valueOf(scanner.next());
			 * index++; }
			 */
			int i = 0;
			while (scanner.hasNextLine()) {
				String[] fields = scanner.nextLine().split(",");
				for (int j = 0; j < nbBasin; j++) {
					distanceGrid[i][j] = Double.valueOf(fields[j + 1]);
				}
			}

			reader.close();
			scanner.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// TODO fill the distance in each basin
		for (Basin basin : grid) {
			basin.setNeighboursDistances(getNeighboursWithDistance(basin));
		}

		// create a QueueMemoryMap to keep the spawnerOrigine in each basin
		List<String> basinNames = new ArrayList<String>();
		for (RiverBasin basin : this.getRiverBasins()) {
			basinNames.add(basin.getName());
		}
		for (RiverBasin basin : this.getRiverBasins()) {
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
			scanner.useLocale(Locale.ENGLISH); // to have a point as decimal
												// separator !!!
			scanner.useDelimiter(Pattern.compile("[,;\r\n]"));
			char[] charac = { '"' };
			String doublequote = new String(charac);
			scanner.nextLine(); // skip the first line with headers
			while (scanner.hasNext()) {
				scanner.next(); // skip gid
				name = scanner.next();
				name = name.replaceAll(new String(doublequote), "");
				year = (long) scanner.nextInt();
				Double[] seasonalTemperature = new Double[4];
				seasonalTemperature[0] = scanner.nextDouble();
				seasonalTemperature[1] = scanner.nextDouble();
				seasonalTemperature[2] = scanner.nextDouble();
				seasonalTemperature[3] = scanner.nextDouble();
				// scanner.nextLine();

				// store seasonal temperatures for each basin for the
				// corresponding year
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
			reader.close();
			scanner.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	/**
	 * @return the temperatureCatchmentFile
	 */
	@Override
	public String getTemperatureCatchmentFile() {
		return temperatureCatchmentFile;
	}


	/**
	 * @return the continent
	 */
	public Map<String, Path2D.Double> getMapContinent() {
		return mapContinent;
	}

}
