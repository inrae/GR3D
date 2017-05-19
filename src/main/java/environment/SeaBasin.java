package environment;

public class SeaBasin extends Basin {

    public SeaBasin(int id, String basinName, double winterTemperature,
            double springTemperature, double summerTemperature,
            double fallTemperature) {
        super(id, basinName, winterTemperature, springTemperature, summerTemperature,
                fallTemperature);
        this.type = Basin.TypeBassin.SEA;
    }

}
