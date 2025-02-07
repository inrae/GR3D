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
import java.util.HashMap;
import java.util.Map;

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
import species.DiadromousFishGroup;

@SuppressWarnings("serial")
public class BasinNetworkObserverWithContinent extends ObserverListener
		implements Configurable, Drawable, MouseMotionListener {

	private String title;
	private double threshold = 13000000.;

	@Description(name = "Color scale", tooltip = "")
	public ColorScaleEnum colorScaleEnum = ColorScaleEnum.RedsScale;

	// a basin network
	private transient BasinNetworkSWithContinent bn;
	// the time
	private transient Time time;

	// list of reachRect
	private transient Map<Shape, Basin> shapeBasinMap;

	// continent
	private transient Map<String, Path2D.Double> mapContinent;

	protected transient Pilot pilot;

	// use to draw objects ( basins and continent)
	private transient double minX, minY, maxX, maxY, rangeX, rangeY;

	// layer to be displayed
	private transient JComponent display;
	private transient JLabel label;

	// basin under the mouse
	private transient Basin basinUnderMouse;


	@Override
	public void addObservable(ObservablesHandler arg0) {
		// nothing to do
	}


	@Override
	public void close() {
		// nothing to do
	}


	@Override
	public void init() {
		// nothing to do
	}


	@TransientParameters.InitTransientParameters
	public void init(Pilot pilot) {
		this.pilot = pilot;
		if (this.colorScaleEnum == null) {
			this.colorScaleEnum = ColorScaleEnum.RedsScale;
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

		// time in bn is not yet created
		time = new Time();
	}


	@Override
	public void valueChanged(ObservablesHandler arg0, Object arg1, long arg2) {

		// update the label

		String txt = Long.valueOf((time.getYear(pilot))).toString() + (" ") + time.getSeason(pilot).toString() + " - ";
		label.setText(txt);
		if (basinUnderMouse != null) {
			txt += (basinUnderMouse.getName() + " ");

			for (DiadromousFishGroup group : basinUnderMouse.getGroups()) {
				txt += group.getName() + "=" + basinUnderMouse.getEffective(group) + " in "
						+ basinUnderMouse.getSuperFishNumber(group) + "  SI ";
			}
		}
		label.setText(txt);
		display.repaint();
	}


	@Override
	public void mouseDragged(MouseEvent arg0) {
		// nothing to do
	}


	@Override
	public void mouseMoved(MouseEvent e) {
		int x = (e.getX());
		int y = (e.getY());
		String txt = (Long.valueOf(time.getYear(pilot))).toString() + (" ") + time.getSeason(pilot).toString() + " - ";

		// identify the basin under the mouse position and enrich the label

		basinUnderMouse = null;
		for (Shape shape : shapeBasinMap.keySet()) {
			if (shape.contains(x, y)) {
				basinUnderMouse = shapeBasinMap.get(shape);

				txt += (basinUnderMouse.getName() + " ");

				for (DiadromousFishGroup group : basinUnderMouse.getGroups()) {
					txt += group.getName() + "=" + basinUnderMouse.getEffective(group) + " in "
							+ basinUnderMouse.getSuperFishNumber(group) + "  SI ";
				}
				break;
			}
		}
		label.setText(txt);
	}


	@Override
	public JComponent getDisplay() {
		// ASK why here and not in inittransient parameter
		// compute min and max of x and y

		/*
		 * double maxX, maxY;
		 * 
		 * maxX = maxY = Double.NEGATIVE_INFINITY; minX = minY = Double.POSITIVE_INFINITY;
		 * 
		 * bn = (BasinNetwork) pilot.getAquaticWorld().getEnvironment(); for (Basin basin : bn.getBasins()) { // take
		 * the opposite of y coordinates because positive values in // a panel are to the south minX = Math.min(minX,
		 * basin.getShape().getBounds2D().getMinX()); maxX = Math.max(maxX, basin.getShape().getBounds2D().getMaxX());
		 * minY = Math.min(minY, basin.getShape().getBounds2D().getMinY()); maxY = Math.max(maxY,
		 * basin.getShape().getBounds2D().getMaxY());
		 * 
		 * // compute the range with a small margin rangeX = maxX - minX; // need to calculate the margin minX -= 0.02 *
		 * rangeX; maxX += 0.02 * rangeX; rangeX = maxX - minX;
		 * 
		 * rangeY = maxY - minY; minY -= 0.02 * rangeY; maxY += 0.02 * rangeY; rangeY = maxY - minY; }
		 */
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
		// System.out.println("ranges of network for display: "+rangeX+"
		// "+rangeY);
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


	public static void main(String[] args) {
		System.out.println((new XStream(new DomDriver())).toXML(new BasinNetworkObserverWithContinent()));
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

			// draw the legend
			colorScaleEnum.getScale().drawLegend(g2d, threshold);

			// draw the basins and fill the map for mouse detection
			shapeBasinMap.clear();// to be used by the mouseMoved()

			double abundance;
			for (Basin basin : bn.getBasins()) {
				// draw each basin
				Path2D.Double displayShape = (Path2D.Double) basin.getShape().createTransformedShape(af);

				// to be used by the mouseMoved()
				shapeBasinMap.put(displayShape, basin);

				// draw the shape with a color according to its type
				if (basin instanceof RiverBasin) {
					g.setColor(colorScaleEnum.getScale().getRiverBasinColor());
				} else if (basin instanceof SeaBasin) {
					g.setColor(colorScaleEnum.getScale().getSeaBasinColor());
				} else {
					g.setColor(new Color(0, 0, 0));
				}
				g2d.draw(displayShape);

				// fill the shape with a color according to its fish abundance
				// (FROM ALL THE FISH GROUP)
				abundance = 0;
				for (DiadromousFishGroup group : basin.getGroups()) {
					abundance += basin.getEffective(group);
				}
				// System.out.println(basin.getName()+ "-->"+abundance);
				if (abundance == 0.) {
					g.setColor(Color.WHITE);
				} else {
					g.setColor(colorScaleEnum.getScale().getColor(Math.min(1., abundance / threshold)));
					// g.setColor(new Color(0.8f * (1f - (float) Math.min(1.,
					// abundance / threshold)), 0.8f * (1 - (float) Math.min(1.,
					// abundance / threshold)), 0.8f * (1 - (float) Math.min(1.,
					// abundance / threshold))));
				}
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
