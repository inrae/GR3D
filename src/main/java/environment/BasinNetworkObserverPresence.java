package environment;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import fr.cemagref.observation.gui.Configurable;
import fr.cemagref.observation.gui.Drawable;
import fr.cemagref.observation.kernel.ObservablesHandler;
import fr.cemagref.observation.kernel.ObserverListener;
import fr.cemagref.ohoui.annotations.Description;
import fr.cemagref.ohoui.filters.NoTransientField;
import fr.cemagref.ohoui.swing.OhOUI;
import fr.cemagref.ohoui.swing.OhOUIDialog;
import fr.cemagref.simaqualife.kernel.util.TransientParameters;
import fr.cemagref.simaqualife.pilot.Pilot;

@SuppressWarnings("serial")
public class BasinNetworkObserverPresence extends ObserverListener
		implements Configurable, Drawable, MouseMotionListener {

	private String title;

	private String presenceFileName = "data/input/northeastamerica/nea_presence.csv";

	private String period = "obs_1751_1850";

	@Description(name = "Color scale", tooltip = "")
	public ColorScaleEnum colorScaleEnum = ColorScaleEnum.BluesScale;

	protected transient Pilot pilot;

	// a basin network
	private transient BasinNetworkSWithContinent bn;

	// list of reachRect
	private transient Map<Shape, Basin> shapeBasinMap;

	// continent
	private transient Map<String, Path2D.Double> mapContinent;

	// use to draw objects ( basins and continent)
	private transient double minX, minY, maxX, maxY, rangeX, rangeY;

	// map with presence
	private transient Map<String, Map<String, Integer>> presences;

	// layer to be displayed
	private transient JComponent display;
	private transient JLabel label;

	// basin under the mouse
	private transient Basin basinUnderMouse;


	@TransientParameters.InitTransientParameters
	public void init(Pilot pilot) {
		this.pilot = pilot;

		if (this.colorScaleEnum == null) {
			this.colorScaleEnum = ColorScaleEnum.RedsScale;

			try {
				// open the file
				FileReader reader = new FileReader(presenceFileName);
				// Parsing the file
				Scanner scanner = new Scanner(reader);
				scanner.useLocale(Locale.ENGLISH); // to have a point as decimal
				// separator !!!
				// scanner.useDelimiter(Pattern.compile("[;,\r\n]"));

				// read the headers
				String[] headers = scanner.nextLine().split(",");
				presences = new TreeMap<String, Map<String, Integer>>();
				for (int i = 2; i < headers.length; i++) {
					presences.put(headers[i], new TreeMap<String, Integer>());
				}

				// read the lines
				while (scanner.hasNextLine()) {
					String[] fields = scanner.nextLine().split(",");
					// System.out.println(Arrays.toString(fields));

					for (int j = 2; j < headers.length; j++) {
						if (j >= fields.length)
							presences.get(headers[j]).put(fields[1], -1);
						else {
							if (fields[j].compareTo("") == 0)
								presences.get(headers[j]).put(fields[1], -1);
							else
								presences.get(headers[j]).put(fields[1], Integer.valueOf(fields[j]));
						}
					}
				}
				// reader.close();
				// scanner.close();
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
		// the Jpanal that holds all the components to be displayed
		display = new JPanel(new BorderLayout());

		// the ad hoc component (specific internal class
		DisplayComponent displayComponent = new DisplayComponent();
		displayComponent.addMouseMotionListener(this);
		displayComponent.setVisible(true);
		displayComponent.setDoubleBuffered(true);
		display.add(displayComponent, BorderLayout.CENTER);

		// label where information will be displayed
		label = new JLabel("");
		display.add(label, BorderLayout.PAGE_START);

		// Initialize the map linking shape with basin
		shapeBasinMap = new HashMap<Shape, Basin>();

		// load basin to a have access to the shape
		bn = (BasinNetworkSWithContinent) pilot.getAquaticWorld().getEnvironment();

	}


	/*
	 * @Override public void valueChanged(ObservablesHandler arg0, Object arg1, long arg2) {
	 * 
	 * // update the label
	 * 
	 * String txt = ""; label.setText(txt); if (basinUnderMouse != null) { txt += (basinUnderMouse.getName() + " "); }
	 * label.setText(txt); display.repaint(); }
	 */

	@Override
	public void mouseDragged(MouseEvent arg0) {
		// nothing to do
	}


	@Override
	public void mouseMoved(MouseEvent e) {
		int x = (e.getX());
		int y = (e.getY());
		String txt = " ";

		// identify the basin under the mouse position
		basinUnderMouse = null;
		for (Shape shape : shapeBasinMap.keySet()) {
			if (shape.contains(x, y)) {
				basinUnderMouse = shapeBasinMap.get(shape);
				txt = (basinUnderMouse.getName());
				break;
			}
		}
		label.setText(txt);
	}


	@Override
	public JComponent getDisplay() {
		// ASK why here and not in inittransient parameter
		// compute min and max of x and y from the continent
		if (mapContinent == null) {
			maxX = maxY = Double.NEGATIVE_INFINITY;
			minX = minY = Double.POSITIVE_INFINITY;
			mapContinent = ((BasinNetworkSWithContinent) pilot.getAquaticWorld().getEnvironment()).getMapContinent();
			for (Path2D.Double path : mapContinent.values()) {
				minX = Math.min(minX, path.getBounds2D().getMinX());
				maxX = Math.max(maxX, path.getBounds2D().getMaxX());
				minY = Math.min(minY, path.getBounds2D().getMinY());
				maxY = Math.max(maxY, path.getBounds2D().getMaxY());
			}
			rangeX = maxX - minX;
			rangeY = maxY - minY;
		}
		return display;
	}


	@Override
	public String getTitle() {
		return title;
	}


	@Override
	public void configure() {
		OhOUIDialog dialog = OhOUI.getDialog(null, this, new NoTransientField());
		dialog.setSize(new Dimension(500, 500));
		dialog.setVisible(true);
		display.repaint();
	}


	public void disable() {
		display.setVisible(false);
	}


	@Override
	public void addObservable(ObservablesHandler arg0) {
		// TODO Auto-generated method stub

	}


	@Override
	public void close() {
		// TODO Auto-generated method stub

	}


	@Override
	public void init() {
		// TODO Auto-generated method stub

	}


	@Override
	public void valueChanged(ObservablesHandler arg0, Object arg1, long arg2) {
		String txt = " ";
		if (basinUnderMouse != null)
			txt = basinUnderMouse.getName();
		label.setText(txt);
		display.repaint();
	}


	public static void main(String[] args) {
		System.out.println((new XStream(new DomDriver())).toXML(new BasinNetworkObserverPresence()));
	}

	private class DisplayComponent extends JComponent {

		@Override
		protected synchronized void paintComponent(Graphics g) {

			// affine transformation to resize the drawing according to the
			// window size
			double W = this.getWidth();
			double H = this.getHeight();
			double scaling = Math.min(W / rangeX, H / rangeY);
			// AffineTransform af = new AffineTransform(scaling, 0., 0., -scaling, -minX * scaling, H + minY * scaling);
			AffineTransform af = new AffineTransform(scaling, 0., 0., -scaling, -minX * scaling, maxY * scaling);

			// System.out.println(af.toString());

			Graphics2D g2d = (Graphics2D) g;
			g2d.setStroke(new BasicStroke(2)); // define the line

			// Draw Background
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, (int) W, (int) H);
			this.paintComponents(g);

			g.setColor(Color.BLUE);
			int side = (int) ((W < H ? W : H) * 1.00);
			g.fillRect(0, 0, side, side);
			this.paintComponents(g);

			// draw the continent
			g.setColor(Color.LIGHT_GRAY);
			for (Path2D.Double path : bn.getMapContinent().values()) {
				Path2D.Double displayContinent = (Path2D.Double) path.createTransformedShape(af);
				// g2d.draw(displayContinent);
				g2d.fill(displayContinent);
			}

			// draw the basins and fill the map for mouse detection
			shapeBasinMap.clear();// to be used by the mouseMoved()

			if (presences.get(period) == null)
				// TODO throw an error
				System.out.println("the period does not exit tin the presence file");

			for (Basin basin : bn.getBasins()) {
				// draw each basin
				Path2D.Double displayShape = (Path2D.Double) basin.getShape().createTransformedShape(af);

				// to be used by the mouseMoved()
				shapeBasinMap.put(displayShape, basin);

				// draw the shape with a color according to its type
				String basinName = null;
				if (basin instanceof RiverBasin) {
					g.setColor(colorScaleEnum.getScale().getRiverBasinColor());
					basinName = basin.getName();
				} else if (basin instanceof SeaBasin) {
					g.setColor(colorScaleEnum.getScale().getSeaBasinColor());
					basinName = bn.getAssociatedRiverBasin(basin).getName();
				} else {
					g.setColor(new Color(0, 0, 0));
				}
				g2d.draw(displayShape);

				int presence = -1;
				if (presences.get(period).get(basinName) != null)
					presence = presences.get(period).get(basinName);
				else
					System.out.println(basinName + " is not present in the presence file");

				/*
				 * if (presence == 1) { g.setColor(Color.RED); } else if (presence == 0) { g.setColor(Color.WHITE); }
				 * else g.setColor(Color.LIGHT_GRAY);
				 */

				if (presence > 0)
					g.setColor(colorScaleEnum.getScale().getColor(presence));
				else if (presence == 0)
					g.setColor(Color.WHITE);
				else
					g.setColor(Color.GRAY);

				g2d.fill(displayShape);
			}
		}
	}

	public enum ColorScaleEnum {

		RedsScale(new RedsScale()), BluesScale(new BluesScale()), BicolorScale(new BicolorScale()), GraysScale(
				new GraysScale());
		private ColorScale scale;


		ColorScaleEnum(ColorScale colorScale) {
			scale = colorScale;
		}


		public ColorScale getScale() {
			return scale;
		}
	}

	public interface ColorScale {

		public Color getColor(double value);


		public Color getSeaBasinColor();


		public Color getRiverBasinColor();


		public void drawLegend(Graphics2D g2d, double threshold);
	}

	public static class BluesScale implements ColorScale {

		@Override
		public Color getColor(double value) {
			return new Color(0.4f + 0.45f * (1f - (float) value), 0.4f + 0.45f * (1f - (float) value), 1f);
		}


		@Override
		public void drawLegend(Graphics2D g2d, double threshold) {
			int nbLegend = 11;
			for (int i = 0; i < nbLegend; i++) {
				float col = (float) i / (float) (nbLegend - 1);
				g2d.setColor(getColor(col));
				g2d.fillRect(10, 30 + 10 * (nbLegend - 1 - i), 20, 10);
				double limit = Math.round(threshold * i / (nbLegend - 1) * 10.) / 10.;
				g2d.setColor(Color.BLACK);
				g2d.drawString(String.valueOf(limit), 32, 20 + 10 * (nbLegend + 1 - i));
			}
		}


		@Override
		public Color getSeaBasinColor() {
			return new Color(0, 102, 255);
		}


		@Override
		public Color getRiverBasinColor() {
			return new Color(0, 204, 255);
		}

	}

	public static class RedsScale implements ColorScale {

		@Override
		public Color getColor(double value) {
			return new Color(1f, 0.4f + 0.45f * (1f - (float) value), 0.4f + 0.45f * (1f - (float) value));
		}


		@Override
		public void drawLegend(Graphics2D g2d, double threshold) {
			int nbLegend = 11;
			for (int i = 0; i < nbLegend; i++) {
				float col = (float) i / (float) (nbLegend - 1);
				g2d.setColor(getColor(col));
				g2d.fillRect(10, 30 + 10 * (nbLegend - 1 - i), 20, 10);
				double limit = Math.round(threshold * i / (nbLegend - 1) * 10.) / 10.;
				g2d.setColor(Color.BLACK);
				g2d.drawString(String.valueOf(limit), 32, 20 + 10 * (nbLegend + 1 - i));
			}
		}


		@Override
		public Color getSeaBasinColor() {
			return new Color(0, 102, 255);
		}


		@Override
		public Color getRiverBasinColor() {
			return new Color(0, 204, 255);
		}

	}

	public static class GraysScale implements ColorScale {

		@Override
		public Color getColor(double value) {
			return new Color(0.4f + 0.45f * (1f - (float) value), 0.4f + 0.45f * (1f - (float) value),
					0.4f + 0.45f * (1f - (float) value));
		}


		@Override
		public void drawLegend(Graphics2D g2d, double threshold) {
			int nbLegend = 11;
			for (int i = 0; i < nbLegend; i++) {
				float col = (float) i / (float) (nbLegend - 1);
				g2d.setColor(getColor(col));
				g2d.fillRect(10, 30 + 10 * (nbLegend - 1 - i), 20, 10);
				double limit = Math.round(threshold * i / (nbLegend - 1) * 10.) / 10.;
				g2d.setColor(Color.BLACK);
				g2d.drawString(String.valueOf(limit), 32, 20 + 10 * (nbLegend + 1 - i));
			}
		}


		@Override
		public Color getSeaBasinColor() {
			return Color.BLACK;
		}


		@Override
		public Color getRiverBasinColor() {
			return Color.BLACK;
		}

	}

	public static class BicolorScale implements ColorScale {

		@Override
		public Color getColor(double value) {
			return value > 0 ? new Color(0.6f, 0.6f, 0.6f) : Color.WHITE;
		}


		@Override
		public void drawLegend(Graphics2D g2d, double threshold) {
		}


		@Override
		public Color getSeaBasinColor() {
			return Color.BLACK;
		}


		@Override
		public Color getRiverBasinColor() {
			return Color.BLACK;
		}

	}

}
