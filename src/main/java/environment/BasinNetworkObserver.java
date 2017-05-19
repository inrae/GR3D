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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import species.DiadromousFish;
import species.DiadromousFishGroup;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import fr.cemagref.observation.gui.Configurable;
import fr.cemagref.observation.gui.Drawable;
import fr.cemagref.observation.kernel.ObservablesHandler;
import fr.cemagref.observation.kernel.ObserverListener;
import fr.cemagref.ohoui.filters.NoTransientField;
import fr.cemagref.ohoui.swing.OhOUI;
import fr.cemagref.ohoui.swing.OhOUIDialog;
import fr.cemagref.simaqualife.kernel.util.TransientParameters;
import fr.cemagref.simaqualife.pilot.Pilot;

@SuppressWarnings("serial")
public class BasinNetworkObserver extends ObserverListener implements Configurable,
        Drawable, MouseMotionListener {

    protected String title;

    protected transient BasinNetwork bn;
    protected transient double minX, minY, rangeX, rangeY;

    // list of reachRect
    protected transient Map<Shape, Basin> shapeBasinMap;

    // to improve eel point display
    protected transient int nbAlea;
    protected transient double[] aleaX, aleaY;

    protected transient JComponent display;
    protected transient JLabel label;
    protected transient List<Color> colors;
    // list of color
    //protected transient Color[] colorList;

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

    
    protected Pilot pilot;

    @TransientParameters.InitTransientParameters
    public void init(Pilot pilot) {
        this.pilot = pilot;
        // create a uniform generator to  calculate 100 xAlea and yAlea
        // to randomly locate the fish in a basin
        nbAlea = 1000;
        aleaX = new double[nbAlea];
        aleaY = new double[nbAlea];
        for (int i = 0; i < nbAlea; i++) {
            aleaX[i] = Math.random();
            aleaY[i] = Math.random();
        }

        display = new JPanel(new BorderLayout());
        DisplayComponent displayComponent = new DisplayComponent();
        displayComponent.addMouseMotionListener(this);
        displayComponent.setVisible(true);
        displayComponent.setDoubleBuffered(true);
        label = new JLabel("");
        display.add(displayComponent, BorderLayout.CENTER);
        display.add(label, BorderLayout.PAGE_START);

        /*		// color list
         colorList = new Color[6];
         colorList[0]= Color.GREEN;
         colorList[1]= Color.CYAN;
         colorList[2]= Color.MAGENTA;
         colorList[3]= Color.PINK;
         colorList[4]= Color.ORANGE;
         colorList[5]= Color.YELLOW;*/
        // Initialize the map linking shape with basin
        shapeBasinMap = new HashMap<Shape, Basin>();

        // list of color
        colors = new ArrayList<Color>(25);
        colors.add(new Color(0, 157, 0));
        colors.add(new Color(0, 163, 0));
        colors.add(new Color(0, 168, 0));
        colors.add(new Color(0, 172, 0));
        colors.add(new Color(17, 175, 0));
        colors.add(new Color(57, 178, 0));
        colors.add(new Color(82, 179, 0));
        colors.add(new Color(103, 180, 0));
        colors.add(new Color(122, 179, 0));
        colors.add(new Color(138, 178, 39));
        colors.add(new Color(153, 175, 117));
        colors.add(new Color(165, 173, 153));
        colors.add(new Color(171, 171, 171));
        colors.add(new Color(183, 167, 157));
        colors.add(new Color(201, 159, 127));
        colors.add(new Color(219, 149, 78));
        colors.add(new Color(235, 139, 0));
        colors.add(new Color(248, 127, 0));
        colors.add(new Color(255, 114, 0));
        colors.add(new Color(255, 101, 0));
        colors.add(new Color(255, 87, 0));
        colors.add(new Color(255, 72, 0));
        colors.add(new Color(255, 57, 0));
        colors.add(new Color(255, 39, 0));
        colors.add(new Color(255, 14, 0));

    }

    @Override
    public void valueChanged(ObservablesHandler arg0, Object env, long time) {
        display.repaint();
        String txt = (new Long(Time.getYear(pilot))).toString() + (" ")
                + Time.getSeason(pilot).toString();
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
        Basin basin;

        String txt = (new Long(Time.getYear(pilot))).toString() + (" ")
                + Time.getSeason(pilot).toString() + " - ";

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
        bn = (BasinNetwork) pilot.getAquaticWorld().getEnvironment();

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
        //display.repaint();
    }

    public void disable() {
        display.setVisible(false);
    }

    public static void main(String[] args) {
        System.out.println((new XStream(new DomDriver())).toXML(new BasinNetworkObserver()));
    }

    private class DisplayComponent extends JComponent {

        @Override
        protected synchronized void paintComponent(Graphics g) {

            double W = this.getWidth();
            double H = this.getHeight();
            AffineTransform af = new AffineTransform(W / rangeX, 0., 0.,
                    -H / rangeY, -W * minX / rangeX, H * (1. + minY / rangeY));
			//System.out.println(af.toString());

            // prepare the graphics
            this.paintComponents(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setStroke(new BasicStroke(3)); // define the line

            // Draw Background
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, (int) W, (int) H);

            // prepare the diameter for the fish circle
            double fishX, fishY;
            int fishDiameter = (int) Math.round(Math.min(W, H) / 100.);

            shapeBasinMap.clear();

            int idxColor;
            for (Basin basin : bn.getBasins()) {
                // draw each basin
                Path2D.Double displayShape
                        = (Path2D.Double) basin.getShape().createTransformedShape(af);
                //System.out.println( basin.getName() +" "+ displayShape.getBounds2D().toString());
                shapeBasinMap.put(displayShape, basin); // to be used by the mouseMoved()

                // fill the 
                if (basin instanceof RiverBasin) {
                    g.setColor(new Color(0, 204, 255));
                } else if (basin instanceof SeaBasin) {
                    g.setColor(new Color(0, 102, 255));
                } else {
                    g.setColor(new Color(0, 0, 255));
                }

                g2d.fill(displayShape);

                // draw the contour of the basin
                if (basin instanceof RiverBasin) {
                    idxColor = bn.getRow(basin.getId()) % colors.size();
                    g.setColor(colors.get(idxColor));
                    g2d.draw(displayShape);
                }

                // draw fish
                int cpt = 0;
                Map<DiadromousFishGroup, List<DiadromousFish>> fishPerGroup = basin.getFishPerGroup();
                for (DiadromousFishGroup group : fishPerGroup.keySet()) {
                    //g.setColor(group.getColor());

                    for (DiadromousFish fish : fishPerGroup.get(group)) {
                        do {
                            fishX = displayShape.getBounds2D().getMinX() + displayShape.getBounds2D().getWidth()
                                    * (0.2 + .6 * aleaX[cpt % nbAlea]); //- fishDiameter/2;
                            fishY = displayShape.getBounds2D().getMinY() + displayShape.getBounds2D().getHeight()
                                    * (0.2 + .6 * aleaY[cpt % nbAlea]);// - fishDiameter/2;
                            //System.out.println(! displayShape.contains(fishX, fishY));
                            cpt++;
                        } while (!displayShape.contains(fishX, fishY));
                        idxColor = bn.getRow(fish.getBirthBasin().getId()) % colors.size();
                        g.setColor(colors.get(idxColor));
                        g2d.fillOval((int) fishX, (int) fishY, fishDiameter,
                                fishDiameter);
                    }
                }
            }
        }
    }
}
