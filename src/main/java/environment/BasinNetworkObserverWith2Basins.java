package environment;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
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

import environment.Basin.TypeBassin;
import fr.cemagref.observation.gui.Configurable;
import fr.cemagref.observation.gui.Drawable;

@SuppressWarnings("serial")
public class BasinNetworkObserverWith2Basins extends BasinNetworkObserver implements Configurable,
Drawable, MouseMotionListener {

	private double maxDistance =1000.;
	private double minDistance = 50.;
	private double maxSurface=100000.;


	// list of reachRect
	private transient Map<Shape, Basin> shapeBasinMap;
	private transient Map<Basin, Shape> basinShapeMap;

	// list of color
	//private transient Color[] colorList;


	@Override
	public void init() {

		// create a uniform generator to  calculate 100 xAlea and yAlea
		// to randomly locate the fish in a basin
		nbAlea = 1000;
		aleaX=new double[nbAlea];
		aleaY=new double[nbAlea];
		for (int i=0; i < nbAlea; i++){
			aleaX[i]= Math.random();
			aleaY[i]= Math.random();	
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

/*		// list of color
		colors= new ArrayList<Color>(25);
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
		colors.add(new Color(255, 14, 0));*/

		// list of color
		colors= new ArrayList<Color>(2);
		colors.add(Color.BLACK);
		colors.add(Color.GRAY);


		// Initialize the map linking shape with basin
		shapeBasinMap = new HashMap<Shape, Basin>();

	}

	@Override
	public JComponent getDisplay() {
		// compute the range with a small margin
		double marginRatio = .02;
		double surf= maxSurface/2, interRect=maxSurface*.05;
		minX =  -2*(surf + interRect);
		rangeX = maxSurface  - minX;
		minX -= rangeX *marginRatio;
		rangeX += 2*rangeX *marginRatio;
		
		bn = (BasinNetwork) pilot.getAquaticWorld().getEnvironment();
		minY = Double.POSITIVE_INFINITY;
		for (RiverBasin basin : bn.getRiverBasins()) {
			minY = Math.min(minY ,basin.getLatitude());
		}
		minY = (minY -minDistance/2.) ;
		rangeY = maxDistance - minY;
		minY -= rangeY * marginRatio;
		rangeY += 2*rangeY*marginRatio;
		// define the shape for each basin
		basinShapeMap = new HashMap<Basin, Shape>();
		for (Basin basin : bn.getBasins()) {
			Path2D.Double shape = new Path2D.Double();
			if (basin.getType() == TypeBassin.RIVER){
				shape.moveTo( 0., ((RiverBasin) basin).getLatitude()- minDistance/2);
				shape.lineTo(((RiverBasin) basin).getSurface(), ((RiverBasin) basin).getLatitude()- minDistance/2);
				shape.lineTo(((RiverBasin) basin).getSurface(), ((RiverBasin) basin).getLatitude()+ minDistance/2);
				shape.lineTo( 0., ((RiverBasin) basin).getLatitude()+ minDistance/2);
				shape.closePath();
			} else {
				RiverBasin rivBas = (RiverBasin) bn.getAssociatedRiverBasin(basin);
				double lag= surf +interRect;
				if (basin.getType() == TypeBassin.OFFSHORE)
					lag *=2 ;
		
				shape.moveTo(-lag,  rivBas.getLatitude()- minDistance/2);
				shape.lineTo(-lag+surf, rivBas.getLatitude()- minDistance/2);
				shape.lineTo( -lag+surf, rivBas.getLatitude()+ minDistance/2);
				shape.lineTo( -lag ,rivBas.getLatitude()+ minDistance/2);
				shape.closePath();
			}
			basinShapeMap.put(basin, shape);
		}


		return display;
	}
	public static void main(String[] args) { System.out.println((new
			XStream(new DomDriver())) .toXML(new BasinNetworkObserverWith2Basins())); }

	private class DisplayComponent extends JComponent {

		@Override
		protected synchronized void paintComponent(Graphics g) {

			double W =this.getWidth();
			double H =this.getHeight();
			AffineTransform af = new AffineTransform(W/rangeX, 0., 0., 
					-H/rangeY, -W*minX/rangeX, H*(1. + minY/rangeY));
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
			int fishDiameter = (int) Math.round(Math.min(W,H) / 100.);

			shapeBasinMap.clear();

			int idxColor;
			//System.out.println("minX="+minX+ " rangeX=" + rangeX+ " minY="+minY+ "rangeY="+rangeY);
			for (Basin basin : basinShapeMap.keySet()) {

				// draw each basin
				//System.out.println( basin.getName() +" "+ basinShapeMap.get(basin).getBounds2D().toString());
				Path2D.Double displayShape = (Path2D.Double)
						((Path2D.Double) basinShapeMap.get(basin)).createTransformedShape(af);
				//System.out.println( basin.getName() +" "+ displayShape.getBounds2D().toString());
				shapeBasinMap.put(displayShape, basin); // to be used by the mouseMoved()

				// fill the 
				if (basin instanceof  RiverBasin)
					g.setColor(new Color(0, 204, 255));
				else if (basin instanceof SeaBasin)
					g.setColor(new Color(0, 102, 255));
				else
					g.setColor(new Color(0, 0, 255));

				g2d.fill(displayShape);

				// draw the contour of the basin
				if (basin instanceof  RiverBasin) {
					idxColor = bn.getRow(basin.getId()) % colors.size();
					g.setColor(colors.get(idxColor));
					g2d.draw(displayShape);
				}

				// draw fish
				int cpt=0;
				Map<DiadromousFishGroup, List<DiadromousFish>> fishPerGroup= basin.getFishPerGroup();
				for (DiadromousFishGroup group : fishPerGroup.keySet()){
					//g.setColor(group.getColor());

					for (DiadromousFish fish : fishPerGroup.get(group)){
						do {
							fishX = displayShape.getBounds2D().getMinX() + displayShape.getBounds2D().getWidth()
									* (0.2 + .6 * aleaX[cpt % nbAlea]); //- fishDiameter/2;
							fishY =  displayShape.getBounds2D().getMinY()+ displayShape.getBounds2D().getHeight()
									* (0.2 + .6 * aleaY[cpt % nbAlea]);// - fishDiameter/2;
							//System.out.println(! displayShape.contains(fishX, fishY));
							cpt++;
						} while (! displayShape.contains(fishX, fishY));
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






