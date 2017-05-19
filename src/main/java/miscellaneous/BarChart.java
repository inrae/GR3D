package miscellaneous;

import javax.swing.JComboBox;
import javax.swing.JComponent;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import fr.cemagref.observation.gui.Configurable;
import fr.cemagref.observation.gui.Drawable;
import fr.cemagref.observation.kernel.ObservablesHandler;
import fr.cemagref.observation.kernel.ObserverListener;
import fr.cemagref.ohoui.filters.NoTransientField;
import fr.cemagref.ohoui.swing.OUIPanel;
import fr.cemagref.ohoui.swing.OhOUI;
import fr.cemagref.ohoui.swing.OhOUIDialog;

public class BarChart extends ObserverListener implements Configurable, Drawable {

    private transient ChartPanel chartPanel;
    private transient JFreeChart jfchart; 
    protected transient DefaultCategoryDataset dataset;

    protected String variableName;
    /**
     * <code>variable</code> is the variable to represent
     */
    protected transient ObservablesHandler.ObservableFetcher variable;
    protected transient ObservablesHandler classObservable;
    private String titlePrefix="Observation of ", title = "", xAxisLabel = "Time", yAxisLabel = "";

    public BarChart() {
        graphUpdated();
    }

    public BarChart(ObservablesHandler.ObservableFetcher variable) {
        this();
        this.variable = variable;
        variableUpdated();
    }

    protected void variableUpdated() {
        if (variable != null) {
            variableName = variable.getDeclaredName();
            yAxisLabel = variable.getDescription();
            title = titlePrefix + yAxisLabel;
        }
    }

    /**
     * Modify the prefix used for updating the title. By default, it is "Observation of ".
     * @param prefix
     */
    protected void setTitlePrefix(String prefix) {
        this.titlePrefix = prefix;
    }

    protected void graphUpdated() {
        PlotOrientation orientation = PlotOrientation.HORIZONTAL;
        boolean legend = true;
        boolean tooltips = true;
        boolean urls = false;

        jfchart = ChartFactory.createBarChart(title, xAxisLabel, yAxisLabel, dataset, orientation, legend, tooltips, urls);
        

        if (chartPanel == null) {
            chartPanel = new ChartPanel(jfchart);
        } else {
            chartPanel.setChart(jfchart);
        }
    }

    public JFreeChart getJfchart() {
        return jfchart;
    }

    @Override
    public void configure() {
        ObservablesHandler.ObservableFetcher variableBak = variable;
        OhOUIDialog dialog = OhOUI.getDialog(null, this, new NoTransientField());
        JComboBox comboBox = new JComboBox(classObservable.getDescriptions());
        if (variable != null) {
            comboBox.setSelectedItem(variable.getDescription());
        }
        OUIPanel ouiPanel = OUIPanel.makeLabelComponentOUIPanel(variable, comboBox, "Variable", "");
        dialog.getContentPane().add(ouiPanel.getPanel(), 0);
        dialog.pack();
        dialog.setVisible(true);
        variable = classObservable.getObservableFetcher((String) comboBox.getSelectedItem());
        if (variable != null) {
            if (!variable.equals(variableBak)) {
                variableUpdated();
            }
        }
        graphUpdated();
    }

    /**
     * @see fr.cemagref.observation.kernel.ObserverListener#addObservable(fr.cemagref.observation.kernel.ObservablesHandler)
     */
    @Override
    public void addObservable(ObservablesHandler classObservable) {
        this.classObservable = classObservable;
    }

    /**
     * @see fr.cemagref.observation.gui.Drawable#getDisplay()
     */
    @Override
    public JComponent getDisplay() {
        return chartPanel;
    }

    /**
     * @see fr.cemagref.observation.gui.Drawable#getTitle()
     */
    @Override
    public String getTitle() {
        return title;
    }

    /**
     * @see fr.cemagref.observation.kernel.ObserverListener#init()
     */
    @Override
    public void init() {
        if (variable == null) {
            if (classObservable != null) {
                if (variableName!=null) {
                    variable = classObservable.getObservableFetcherByName(variableName);
                } else if (classObservable.numberOfAttributes() > 0) {
                    variable = classObservable.getObservableFetcher(0);
                }
            }
            // TODO classObservable.getAttributes().length *must* be > 0
        }
    }

	@Override
	public void close() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void valueChanged(ObservablesHandler arg0, Object arg1, long arg2) {
		// TODO Auto-generated method stub
		
	}
}
