package miscellaneous;

import java.lang.reflect.InvocationTargetException;

import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;

import fr.cemagref.observation.kernel.ObservablesHandler;

public class TemporalRangeSerieChart extends TemporalRangeChart {

    public TemporalRangeSerieChart() {
        super();
        init();
    }
    
    public TemporalRangeSerieChart(ObservablesHandler.ObservableFetcher variable) {
        super(variable);
        init();
    }

    /**
     * @see fr.cemagref.observation.kernel.ObserverListener#valueChanged(fr.cemagref.observation.kernel.ObservablesHandler, java.lang.Object, int)
     */
    @Override
    public void valueChanged(ObservablesHandler clObservable, Object instance, long t) {
    	// we fetch the value of the observable
        Double[] value;
        try {
        	Object objectValue = variable.fetchValue(instance);
        	value =  (Double[]) objectValue;
        	
/*        	if (objectValue instanceof Integer)
        		value = (Integer)objectValue;
        	else
        		value = (Double)objectValue;*/
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

        int index = dataset.indexOf(instance.hashCode());
        YIntervalSeries serie;
        if (index >= 0) {
            serie = dataset.getSeries(index);
        } else {
            // This is the first value of the observable 
            serie = new YIntervalSeries(instance.hashCode());
            dataset.addSeries(serie);
        }
        serie.add(t, value[0], value[1], value[2]);
    }

    /**
     * @see fr.cemagref.observation.kernel.ObserverListener#init()
     */
    @Override
    public void init() {
        super.init();
        dataset = new YIntervalSeriesCollection();
        graphTypeUpdated();
    }

    /**
     * @see fr.cemagref.observation.kernel.ObserverListener#close()
     */
    @Override
    public void close() {
    }

	@Override
	public void configure() {
		// TODO Auto-generated method stub
		
	}

}
