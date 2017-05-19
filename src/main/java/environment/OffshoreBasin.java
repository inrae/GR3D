package environment;

public class OffshoreBasin extends Basin {

	public OffshoreBasin(int id, String basinName, double winterTemperature,
			double springTemperature, double summerTemperature,
			double fallTemperature) {
		super(id, basinName, winterTemperature, springTemperature, summerTemperature,
				fallTemperature);
		this.type = Basin.TypeBassin.OFFSHORE;
	}

}
