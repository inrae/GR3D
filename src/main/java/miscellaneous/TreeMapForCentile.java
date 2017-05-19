package miscellaneous;
import java.util.TreeMap;
import java.util.Map.Entry;


public class TreeMapForCentile extends TreeMap<Double,Long> {

	public Long putWithAdding(double key, long effective){
		Long out;
		if (this.containsKey(key)) {
			out= this.get(key);	
			this.put(key, out + effective);
		}
		else	
			out=this.put(key, effective);
		return out;
	}

	public double calculateMedian(){

		if (this.isEmpty()){
			return Double.NaN;
		} else if (this.size()==1){
			return this.lastKey();
		} else {
			double median = 0;
			long totalEffective = 0;
			TreeMap<Long,Double> cumulativeEffectiveValue = new TreeMap<Long, Double>();;
			long cumulativeEffective = 0;
			for (Double key : this.keySet()){
				cumulativeEffective += this.get(key);
				cumulativeEffectiveValue.put(cumulativeEffective, key);
			}
			totalEffective = cumulativeEffectiveValue.lastEntry().getKey();

			long midEffective = totalEffective / 2 ;
			Entry<Long, Double> floorEntry = cumulativeEffectiveValue.floorEntry(midEffective);

			if (floorEntry == null){
				median= Double.NaN;
			} else {
				Entry<Long, Double> ceilEntry;				
				if (floorEntry.getKey() == midEffective)
					median = floorEntry.getValue();
				else {
					ceilEntry = cumulativeEffectiveValue.ceilingEntry(midEffective);
					median = ((ceilEntry.getValue() - floorEntry.getValue()) * (midEffective - floorEntry.getKey()) / 
							(ceilEntry.getKey() - floorEntry.getKey())) + floorEntry.getValue();
				}
			}
			return median;

		}
	}

	public double calculateCentile(double centile){

		if (this.isEmpty()){
			return Double.NaN;
		} else if (this.size()==1){
			return this.lastKey();
		} else {
			double centil = 0;
			TreeMap<Long,Double> cumulativeEffectiveValue = new TreeMap<Long, Double>();;
			long cumulativeEffective = 0;
			for (Double key : this.keySet()){
				cumulativeEffective += this.get(key);
				cumulativeEffectiveValue.put(cumulativeEffective, key);
			}
			long totalEffective = cumulativeEffectiveValue.lastEntry().getKey();

			long centileEffective = (long) (totalEffective * centile) ;
			Entry<Long, Double> floorEntry = cumulativeEffectiveValue.floorEntry(centileEffective);

			if (floorEntry == null){
				centil= Double.NaN;
			} else {

				if (floorEntry.getKey() == centileEffective)
					centile = floorEntry.getValue();
				else {
					Entry<Long, Double> ceilEntry = cumulativeEffectiveValue.ceilingEntry(centileEffective);
					centil = ((ceilEntry.getValue() - floorEntry.getValue()) * 
							(centileEffective - floorEntry.getKey()) / 
							(ceilEntry.getKey() - floorEntry.getKey())) + floorEntry.getValue();
				}
			}
			return centil;

		}
	}


	public static void main(String[] args) {
		TreeMapForCentile tm= new TreeMapForCentile();

		tm.putWithAdding(40., 100);
		tm.putWithAdding(42., 53);
		tm.putWithAdding(45., 10);
		tm.putWithAdding(42., 17);
		tm.putWithAdding(46., 100);

		System.out.println(tm.toString());
		System.out.println(tm.calculateMedian());
		System.out.println(tm.calculateCentile(.5));
	}
}