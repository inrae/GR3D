package miscellaneous;

import java.awt.Color;
import java.lang.reflect.InvocationTargetException;

import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.AbstractRenderer;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYSeries;

import fr.cemagref.observation.kernel.ObservablesHandler;
import fr.cemagref.observation.observers.jfreechart.TemporalSerieChart;
import environment.Basin;

public class TemporalSerieChartForBasin extends TemporalSerieChart {

    public TemporalSerieChartForBasin() {
        super();
        init();
    }
    
    public TemporalSerieChartForBasin(ObservablesHandler.ObservableFetcher variable) {
        super(variable);
        init();
    }

    /**
     * @see fr.cemagref.observation.kernel.ObserverListener#valueChanged(fr.cemagref.observation.kernel.ObservablesHandler, java.lang.Object, int)
     */
    @Override
    public void valueChanged(ObservablesHandler clObservable, Object instance, long t) {
    	// we fetch the value of the observable
        double value;
        try {
        	Object objectValue = variable.fetchValue(instance);
        	if (objectValue instanceof Integer)
        		value = (Integer)objectValue;
        	else
        		value = (Double)objectValue;
        } catch (IllegalArgumentException e1) {
            e1.printStackTrace();
            return;
        } catch (IllegalAccessException e1) {
            e1.printStackTrace();
            return;
        } catch (InvocationTargetException e) {
			e.printStackTrace();
            return;
        }
  
        int index = dataset.indexOf(((Basin) instance).getId());
        XYSeries serie;
        if (index >= 0) {
            serie = dataset.getSeries(index);
        } else {
            // This is the first value of the observable 
            serie = new XYSeries(((Basin) instance).getId());
            dataset.addSeries(serie);
        }
        serie.add(t, value);
    }

	@Override
	protected void graphTypeUpdated() {
		super.graphTypeUpdated();
			
        
        XYPlot plot= (XYPlot) this.getJfchart().getPlot();
        plot.setBackgroundPaint(Color.white);
        AbstractRenderer r0 = (AbstractRenderer) plot.getRenderer(0);
        
        r0.setSeriesPaint(0, Color.black);
        r0.setSeriesPaint(1, Color.gray);
	}



}
