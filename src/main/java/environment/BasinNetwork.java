package environment;

import fr.cemagref.simaqualife.kernel.AquaNismsGroup;
import fr.cemagref.simaqualife.kernel.spatial.Environment;
import fr.cemagref.simaqualife.kernel.util.TransientParameters.InitTransientParameters;
import fr.cemagref.simaqualife.pilot.Pilot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import species.DiadromousFish;

public abstract class BasinNetwork extends Environment<Basin, DiadromousFish> {

    protected int nbBasin;
    protected int memorySize; // size of all the queues
    protected int memorySizeLongQueue; // size of all longer queues
   
    protected double cVthresholdForFinalStates = 5.;
    protected double RecruitmentThresholdForFsAndKappa = 50.;
    
    protected long yearOfTheBegin = 1800;
    
    protected transient Basin[] grid;
    protected transient double[][] distanceGrid;
    
    protected transient Time time;
    
    @InitTransientParameters
    public void initTransientParameters(Pilot pilot) {
    	time = new Time();
    }
    

    public int getRow(int id) {
        return (id % nbBasin);
    }

    public int getColumn(int id) {
        return ((id - (id % nbBasin)) / nbBasin);
    }

    public int getId(int i, int j) {
        return (j * nbBasin + i);
    }

    public Basin getAssociatedRiverBasin(Basin basin) {
        int i = getRow(basin.getId());
        return (grid[getId(i, 0)]);
    }

    public Basin getAssociatedSeaBasin(Basin basin) {
        int i = getRow(basin.getId());
        return (grid[getId(i, 1)]);
    }

    public Basin getAssociatedOffshoreBasin(Basin basin) {
        int i = getRow(basin.getId());
        return (grid[getId(i, 2)]);
    }

    @Override
    public List<Basin> getNeighbours(Basin basin) {
        int j = getColumn(basin.getId());
        if (j == 0) //that means for a river basin
        {
            return null;
        } else {
            List<Basin> neighbours = new ArrayList<Basin>(nbBasin - 1);
            int i = getRow(basin.getId());

            for (int k = 0; k < nbBasin; k++) {
                if (k != i) {
                    neighbours.add(grid[getId(k, j)]);
                }
            }
            return neighbours;
        }
    }

    public Map<Basin, Double> getNeighboursWithDistance(Basin basin) {
        int j = getColumn(basin.getId());
        if (j == 0) //that means for a river basin
        {
            return null;
        } else {
            Map<Basin, Double> neighbours = new TreeMap<Basin, Double>();
            int i = getRow(basin.getId());

            for (int k = 0; k < nbBasin; k++) {
                if (k != i) {
                    neighbours.put(grid[getId(k, j)],
                            distanceGrid[i][k]);
                }
            }
            return neighbours;
        }
    }

    public int getNbBasin() {
        return nbBasin;
    }

    public Basin[] getBasins() {
        return grid;
    }

    public RiverBasin[] getRiverBasins() {
        RiverBasin[] riverGrid = new RiverBasin[nbBasin];

        for (int i = 0; i < nbBasin; i++) {
            riverGrid[i] = (RiverBasin) grid[i];
        }

        return riverGrid;
    }

    public SeaBasin[] getSeaBasins() {
        SeaBasin[] seaGrid = new SeaBasin[nbBasin];

        for (int i = 0; i < nbBasin; i++) {
            seaGrid[i] = (SeaBasin) grid[nbBasin + i];
        }

        return seaGrid;
    }

    public String[] getRiverBasinNames() {
        RiverBasin[] riverBasins = getRiverBasins();
        String[] names = new String[riverBasins.length];
        for (int i = 0; i < riverBasins.length; i++) {
            names[i] = riverBasins[i].getName();
        }
        return names;
    }

    public int[] getFinalStates() {
        // TODO ask to Nicolas to be able to use ReflectUtils with arguments
       
        RiverBasin[] riverBasins = getRiverBasins();
        int[] finalStates = new int[riverBasins.length];
        for (int i = 0; i < riverBasins.length; i++) {
            RiverBasin riverBasin = riverBasins[i];
            double roundedCV = Math.round(riverBasin.getLastRecruitments().getCoefficientVariation() * 10000.) / 100.;
            if (roundedCV == 0.) {
                finalStates[i] = 0;
            } else if (roundedCV < this.cVthresholdForFinalStates) {
                finalStates[i] = 2;
            } else {
                finalStates[i] = 1;
            }
        }
        return finalStates;
    }
    
    public int[] getFinalStatesWithStochasticity() {
        // TODO ask to Nicolas to be able to use ReflectUtils with arguments
       
        RiverBasin[] riverBasins = getRiverBasins();
        int[] finalStates = new int[riverBasins.length];
        for (int i = 0; i < riverBasins.length; i++) {
            RiverBasin riverBasin = riverBasins[i];
            double roundedCV = Math.round(riverBasin.getLastRecruitmentExpectations().getCoefficientVariation() * 10000.) / 100.;
            if (roundedCV == 0.) {
                finalStates[i] = 0;
            } else if (roundedCV < this.cVthresholdForFinalStates) {
                finalStates[i] = 2;
            } else {
                finalStates[i] = 1;
            }
        }
        return finalStates;
    }
    
    public int[] getFinalStatesForKappa() {
        // TODO ask to Nicolas to be able to use ReflectUtils with arguments
       
        RiverBasin[] riverBasins = getRiverBasins();
        int[] finalStatesForKappa = new int[riverBasins.length];
        for (int i = 0; i < riverBasins.length; i++) {
            RiverBasin riverBasin = riverBasins[i];
            if (riverBasin.getLastRecruitments().getMean() <= RecruitmentThresholdForFsAndKappa) {
                finalStatesForKappa[i] = 0;
            } else {
                finalStatesForKappa[i] = 1;
            }
        }
        return finalStatesForKappa;
    }

    public double[] getMeanLastRecruitments() {
        RiverBasin[] riverBasins = getRiverBasins();
        double[] data = new double[riverBasins.length];
        for (int i = 0; i < riverBasins.length; i++) {
            data[i] = riverBasins[i].getLastRecruitments().getMean();
        }
        return data;
    }
    
    public double[] getProbOfNonNulRecruitmentDuringLastYears(){
    	RiverBasin[] riverBasins = getRiverBasins();
    	double[] data = new double[riverBasins.length];
    	for (int i = 0; i < riverBasins.length; i++) {
    		if (riverBasins[i].getNumberOfNonNulRecruitmentDuringLastYears().getSum() < 1.0){
    			data[i] = 0.001;
    		}else if(riverBasins[i].getNumberOfNonNulRecruitmentDuringLastYears().getSum() > (riverBasins[i].getNumberOfNonNulRecruitmentDuringLastYears().size()-1)){
    			data[i] = 0.999;
    		}else{
    			data[i] = riverBasins[i].getNumberOfNonNulRecruitmentDuringLastYears().getSum() / (riverBasins[i].getNumberOfNonNulRecruitmentDuringLastYears().size());
    		}
    	}
    	return data;
    }
    
    public double[] getFinalProbabilityOfPresence(){ // function computed for obtain probability of presence in 2100 for the allis shad case study
    	RiverBasin[] riverBasins = getRiverBasins();
    	double[] data = new double[riverBasins.length];
    	for (int i = 0; i < riverBasins.length; i++) {
    			data[i] = riverBasins[i].getNumberOfNonNulRecruitmentForFinalProbOfPres().getSum() / riverBasins[i].getNumberOfNonNulRecruitmentForFinalProbOfPres().size();
    	}
    	return data;
    }
    
    public double getMeanLastRecruitmentsBV2() {
        RiverBasin[] riverBasins = getRiverBasins();
        double data = riverBasins[1].getLastRecruitments().getMean();
        return data;
    }
    
    public double[] getMeanLastRecruitmentExpectations() {
        RiverBasin[] riverBasins = getRiverBasins();
        double[] data = new double[riverBasins.length];
        for (int i = 0; i < riverBasins.length; i++) {
            data[i] = riverBasins[i].getLastRecruitmentExpectations().getMean();
        }
        return data;
    }
    
    public double[] getGeoMeansLastRecsOverProdCaps() {
        // TODO ask to Nicolas to be able to use ReflectUtils with arguments
       
        RiverBasin[] riverBasins = getRiverBasins();
        double [] geoMeans = new double [riverBasins.length];
        for (int i = 0; i < riverBasins.length; i++) {
        	geoMeans[i] = riverBasins[i].getLastRecsOverProdCaps().getGeometricMean();
        }
        return geoMeans;
    }
    
    public double[] getMeanLastPercOfAut() { // give the mean of the last % of autochtone spawners
        RiverBasin[] riverBasins = getRiverBasins();
        double[] data = new double[riverBasins.length];
        for (int i = 0; i < riverBasins.length; i++) {
            data[i] = riverBasins[i].getLastPercentagesOfAutochtones().getMean();
        }
        return data;
    }
    
    public long[] getYearsOfFirstNonNulRep() {
        RiverBasin[] riverBasins = getRiverBasins();
        long[] data = new long[riverBasins.length];
        for (int i = 0; i < riverBasins.length; i++) {
            data[i] = riverBasins[i].getYearOfFirstNonNulRep();
        }
        return data;
    }
    
    public long[] getYearsOfLastNulRep() {
        RiverBasin[] riverBasins = getRiverBasins();
        long[] data = new long[riverBasins.length];
        for (int i = 0; i < riverBasins.length; i++) {
            data[i] = riverBasins[i].getYearOfLastNulRep();
        }
        return data;
    }
    
    
    /**
	 * @return the yearOfTheBegin
	 */
	public long getYearOfTheBegin() {
		return yearOfTheBegin;
	}

	@Override
    public void addAquaNism(DiadromousFish fish, AquaNismsGroup group) {
        // TODO Auto-generated method stub
    }

    @Override
    public void moveAquaNism(DiadromousFish fish, AquaNismsGroup group,
            Basin destination) {
        // TODO Auto-generated method stub
    }

    @Override
    public void removeAquaNism(DiadromousFish fish, AquaNismsGroup group) {
        // TODO Auto-generated method stub
    }
    
	public abstract Map<String, Double[]> getTemperaturesBasin(long year);
	
	public abstract String getTemperatureCatchmentFile();

	/**
	 * @return the time
	 */
	public Time getTime() {
		return time;
		
		
	}


	
	
}
