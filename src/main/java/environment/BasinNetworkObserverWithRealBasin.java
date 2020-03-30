package environment;

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
import species.DiadromousFishGroup;

@SuppressWarnings("serial")
public class BasinNetworkObserverWithRealBasin extends ObserverListener implements Configurable,
        Drawable, MouseMotionListener {

    private String title;
    private double threshold = 13000000.;

    @Description(name = "Color scale", tooltip = "")
    public ColorScaleEnum colorScaleEnum = ColorScaleEnum.BluesScale;

    private transient BasinNetwork bn;
    private transient Time time;
    private transient double minX, minY, rangeX, rangeY;

    // list of reachRect
    private transient Map<Shape, Basin> shapeBasinMap;

    private transient JComponent display;
    private transient JLabel label;
    
    protected transient Pilot pilot;

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
            this.colorScaleEnum = ColorScaleEnum.BluesScale;
        }
        // the Jpanal that holds all the components to be displayed
        display = new JPanel(new BorderLayout());
        
        // the ad hoc compenment (specific internal class
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
    }

    @Override
    public void valueChanged(ObservablesHandler arg0, Object arg1, long arg2) {
        display.repaint();
        
        // update the label
        String txt = Long.valueOf((time.getYear(pilot))).toString() + (" ")
                + time.getSeason(pilot).toString();
        label.setText(txt);
    }

    @Override
    public void mouseDragged(MouseEvent arg0) {
        // nothing to do
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        int y = (e.getY());
        int x = (e.getX());

        // to update the label
        String txt = (Long.valueOf(time.getYear(pilot))).toString() + (" ")
                + time.getSeason(pilot).toString() + " - ";

        // identify the basin uunder the mouse position and enrich the label
        Basin basin;
        for (Shape shape : shapeBasinMap.keySet()) {
            if (shape.contains(x, y)) {
                basin = shapeBasinMap.get(shape);
                txt += (basin.getName() + " ");

                for (DiadromousFishGroup group : basin.getGroups()) {
                    txt += group.getName() + "=" + basin.getEffective(group) + " in "
                            + basin.getSuperFishNumber(group) + "  SI ";
                }
                break;
            }
        }
        label.setText(txt);
    }

    @Override
    public JComponent getDisplay() {
    	if (bn == null) { //CHECK
        bn = (BasinNetwork) pilot.getAquaticWorld().getEnvironment();
        time = bn.getTime();
    	}
        // compute min and max of x and y 
        double maxX, maxY;
        maxX = maxY = Double.NEGATIVE_INFINITY;
        minX = minY = Double.POSITIVE_INFINITY;

        rangeX = rangeY = 0.;
        // System.out.println(esu.toString());
        for (Basin basin : bn.getBasins()) {
            // take the opposite of y coordinates because positive values in
            // a panel are to the south
            minX = Math.min(minX, basin.getShape().getBounds2D().getMinX());
            maxX = Math.max(maxX, basin.getShape().getBounds2D().getMaxX());
            minY = Math.min(minY, basin.getShape().getBounds2D().getMinY());
            maxY = Math.max(maxY, basin.getShape().getBounds2D().getMaxY());
        }

        // compute the range with a small margin
        rangeX = maxX - minX;
        minX -= 0.02 * rangeX;
        maxX += 0.02 * rangeX;
        rangeX = maxX - minX;

        rangeY = maxY - minY;
        minY -= 0.02 * rangeY;
        maxY += 0.02 * rangeY;
        rangeY = maxY - minY;

        //System.out.println("ranges of network for display: "+rangeX+" "+rangeY);
        return display;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void configure() {
        OhOUIDialog dialog = OhOUI
                .getDialog(null, this, new NoTransientField());
        dialog.setSize(new Dimension(500, 500));
        dialog.setVisible(true);
        display.repaint();
    }

    public void disable() {
        display.setVisible(false);
    }

    public static void main(String[] args) {
        System.out.println((new XStream(new DomDriver())).toXML(new BasinNetworkObserverWithRealBasin()));
    }

    private class DisplayComponent extends JComponent {

        @Override
        protected synchronized void paintComponent(Graphics g) {

        	// affine transmortaion to resize the drawing according to window size
        	//TODO fixed the same scalinf for x and y to avoid weird map deformation
            double W = this.getWidth();
            double H = this.getHeight();
            AffineTransform af = new AffineTransform(W / rangeX, 0., 0.,
                    -H / rangeY, -W * minX / rangeX, H * (1. + minY / rangeY));
			//System.out.println(af.toString());

            // Draw Background
            g.setColor(Color.BLUE);
            g.fillRect(0, 0, (int) W, (int) H);

            // prepare the graphics
            this.paintComponents(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setStroke(new BasicStroke(2)); // define the line

            // draw the legend
            colorScaleEnum.getScale().drawLegend(g2d, threshold);

            shapeBasinMap.clear();// to be used by the mouseMoved()
            double abundance;
            for (Basin basin : bn.getBasins()) {
                // draw each basin
                Path2D.Double displayShape
                        = (Path2D.Double) basin.getShape().createTransformedShape(af);
                //System.out.println( basin.getName() +" "+ displayShape.getBounds2D().toString());
                shapeBasinMap.put(displayShape, basin); // to be used by the mouseMoved()

                // draw the shape with a color according to its type
                if (basin instanceof RiverBasin) {
                    g.setColor(colorScaleEnum.getScale().getRiverBasinColor());
                } else if (basin instanceof SeaBasin) {
                    g.setColor(colorScaleEnum.getScale().getSeaBasinColor());
                } else {
                    g.setColor(new Color(255, 0, 0));
                }
                g2d.draw(displayShape);

                // fill the shape with a color according to its fish abundance (FROM ALL THE FISH GROUP)
                abundance = 0;
                for (DiadromousFishGroup group : basin.getGroups()) {
                    abundance += basin.getEffective(group);
                }
                //System.out.println(basin.getName()+ "-->"+abundance);
                if (abundance == 0.) {
                    g.setColor(Color.WHITE);
                } else {
                    g.setColor(colorScaleEnum.getScale().getColor(Math.min(1., abundance / threshold)));
                    //g.setColor(new Color(0.8f * (1f - (float) Math.min(1., abundance / threshold)), 0.8f * (1 - (float) Math.min(1., abundance / threshold)), 0.8f * (1 - (float) Math.min(1., abundance / threshold))));
                }
                g2d.fill(displayShape);
            }
        }
    }

    public enum ColorScaleEnum {

        BluesScale(new BluesScale()),
        BicolorScale(new BicolorScale()),
        GraysScale(new GraysScale());
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
                double limit = Math.round((double) threshold * (double) i / (double) (nbLegend - 1) * 10.) / 10.;
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
            return new Color(0.4f + 0.45f * (1f - (float) value), 0.4f + 0.45f * (1f - (float) value), 0.4f + 0.45f * (1f - (float) value));
        }

        @Override
        public void drawLegend(Graphics2D g2d, double threshold) {
            int nbLegend = 11;
            for (int i = 0; i < nbLegend; i++) {
                float col = (float) i / (float) (nbLegend - 1);
                g2d.setColor(getColor(col));
                g2d.fillRect(10, 30 + 10 * (nbLegend - 1 - i), 20, 10);
                double limit = Math.round((double) threshold * (double) i / (double) (nbLegend - 1) * 10.) / 10.;
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
