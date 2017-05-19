package miscellaneous;

import java.util.concurrent.ArrayBlockingQueue;
import java.lang.Number; 

public class QueueMemory<E extends Number> extends ArrayBlockingQueue<E> {

	private int memorySize; 

	public QueueMemory(int memorySize) {
		super(memorySize);
		this.memorySize = memorySize;
	}

	public void push(E item){
		try {
			if (this.size() == this.memorySize){
				this.poll();
			}
			this.put(item);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public E getLastItem(){
		Object[] items = this.toArray();
		if (items.length > 0)
			return (E) items[items.length-1];
		else
			return null;
	}
	
	public E getItemFromLast(int i){
		Object[] items = this.toArray();
		
		if (i>=0 & i<items.length)
			return (E) items[items.length-i];
		else
			return null;
	}

	public double getSum(){
		double sum= 0.;
		for (E item : this){
			sum += this.doubleValue(item);
		}
		return (sum);
	}

	public double getMean(){
		double sum= 0.;
		for (E item : this){
			sum +=  this.doubleValue(item);
		}
		sum = sum / memorySize ;
		return (sum);
	}

	public double getMeanWithoutZero(){
		double sum= 0.;
		double total= 0.;
		for (E item : this){
			if (this.doubleValue(item) > 0.){
				sum +=  this.doubleValue(item);
				total++;
			}			
		}
		if(total > 0){
		sum = sum / total;
		}
		return (sum);
	}

	public double getGeometricMean(){
		double sum= 0.;
		for (E item : this){
			sum += Math.log(this.doubleValue(item));
		}
		return (Math.exp(sum / memorySize));
	}

	public double getStandartDeviation(){ 
		double mean= this.getMean();
		double sse= 0;
		for (E item : this){
			sse +=  Math.pow(this.doubleValue(item) - mean, 2.);
		}
		return (Math.sqrt(sse /(memorySize -1.)));		
	}

	public double getCoefficientVariation(){
		return (this.getStandartDeviation() / this.getMean());
	}

	private double doubleValue(E item){
		if (item instanceof Double)
			return ((Double) item);
		else if (item instanceof Float)
			return ((Float) item).doubleValue();
		else if (item instanceof Long)
			return ((Long) item).doubleValue();
		else if (item instanceof Integer)
			return ((Integer) item).doubleValue();
		else
			return Double.NaN;
	}
}

