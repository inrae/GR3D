package species;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import fr.cemagref.simaqualife.kernel.processes.AquaNismsGroupProcess;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class TypeTrajectoryCV extends AquaNismsGroupProcess<DiadromousFish, DiadromousFishGroup> {

    private String fileNameOutput = "summary";
    private transient BufferedWriter bW;

    public static void main(String[] args) {
        System.out.println((new XStream(new DomDriver())).toXML(new TypeTrajectoryCV()));

        //TypeTrajectoryCV trajectoryCV = new TypeTrajectoryCV();
//        trajectoryCV.computeKapa("data/input/reality/Obs1900.csv");
    }

    @Override
    public void doProcess(DiadromousFishGroup group) {

       // int[] finalStates = group.getEnvironment().getFinalStates();
       // int[] finalStatesWithStoch = group.getEnvironment().getFinalStatesWithStochasticity();
        int[] finalStatesForKappa = group.getEnvironment().getFinalStatesForKappa();
        double[] geoMeanRecOverProdCap = group.getEnvironment().getGeoMeansLastRecsOverProdCaps();
        double[] meanLastRec = group.getEnvironment().getMeanLastRecruitments();
        double[] meanPercOfAut = group.getEnvironment().getMeanLastPercOfAut();
        double[] probOfNonNulRecruitmentDuringLastYears = group.getEnvironment().getProbOfNonNulRecruitmentDuringLastYears();
        long[] yearsOfFirstNonNulRep = group.getEnvironment().getYearsOfFirstNonNulRep();
        long[] yearsOfLastNulRep = group.getEnvironment().getYearsOfLastNulRep();
        String[] finalStatesNames = group.getEnvironment().getRiverBasinNames();
        double[] finalProbabilityOfPresence = group.getEnvironment().getFinalProbabilityOfPresence();

        // System.out.println(group.getEnvironment().getMeanLastRecruitmentsBV2());
        //System.out.println(" nom des bv " + Arrays.deepToString(finalStatesNames) + "final states : " + Arrays.toString(finalStates));				
        //System.out.println(" nom des bv " + Arrays.deepToString(finalStatesNames) + "final states wirth stoch : " + Arrays.toString(finalStatesWithStoch));
        //System.out.println(" nom des bv " + Arrays.deepToString(finalStatesNames) + "geo mean of last 10 years R/alpha : " + Arrays.toString(geoMeanRecOverProdCap));
        //System.out.println(" nom des bv " + Arrays.deepToString(finalStatesNames) + "mean last recruitment : " + Arrays.toString(meanLastRec));
        //System.out.println(" nom des bv " + Arrays.deepToString(finalStatesNames));
        //System.out.println(" nom des bv " + Arrays.deepToString(finalStatesNames) + "prob non nul rec : " + Arrays.toString(probOfNonNulRecruitmentDuringLastYears));
        //System.out.println("mean last percentage of autochtone : " + Arrays.toString(meanPercOfAut));
        //System.out.println(" nom des bv " + Arrays.deepToString(finalStatesNames) + "year of first non nul reproduction : " + Arrays.toString(yearsOfFirstNonNulRep));
        //System.out.println(" nom des bv " + Arrays.deepToString(finalStatesNames) + "year of last nul reproduction : " + Arrays.toString(yearsOfLastNulRep));
        //this.fireChangesToObservers();
        if (fileNameOutput != null) {
        	// create the subdirectorrys if necessary ?
            new File(group.getOutputPath()+fileNameOutput).getParentFile().mkdirs();
            
            try {
                bW = new BufferedWriter(new FileWriter(new File(group.getOutputPath()+
                        fileNameOutput+group.getSimulationId()+ ".csv")));
                int nbBV = finalStatesNames.length;

                bW.write("nom des bv");
                for (int i = 0; i < nbBV; i++) {
                    bW.write(";" + finalStatesNames[i]);
                }
                bW.write("\n");

                bW.write("Final states");
                for (int i = 0; i < nbBV; i++) {
                    bW.write(";" + finalStatesForKappa[i]);
                }
                bW.write("\n");

                bW.write("year of first non nul reproduction");
                for (int i = 0; i < nbBV; i++) {
                    bW.write(";" + yearsOfFirstNonNulRep[i]);
                }
                bW.write("\n");

                bW.write("year of last nul reproduction");
                for (int i = 0; i < nbBV; i++) {
                    bW.write(";" + yearsOfLastNulRep[i]);
                }
                bW.write("\n");

                bW.write("mean last recruitment");
                for (int i = 0; i < nbBV; i++) {
                    bW.write(";" + meanLastRec[i]);
                }
                bW.write("\n");

                bW.write("geo mean of last 10 years R/alpha");
                for (int i = 0; i < nbBV; i++) {
                    bW.write(";" + geoMeanRecOverProdCap[i]);
                }
                bW.write("\n");

                bW.write("mean last percentage of autochtone");
                for (int i = 0; i < nbBV; i++) {
                    bW.write(";" + meanPercOfAut[i]);
                }
                bW.write("\n");

                bW.write("prob of non nul recruitment during last years");
                for (int i = 0; i < nbBV; i++) {
                    bW.write(";" + probOfNonNulRecruitmentDuringLastYears[i]);
                }
                bW.write("\n");
                
                computeKapa(bW, "data/input/reality/Obs1900.csv", finalStatesForKappa, finalStatesNames);
                bW.write("\n");
                
                bW.write("likelihood;" + ((Double) group.computeLikelihood()).toString() + "\n");
                
                bW.write("spawnersMatureAgeSumStat;" + group.computeSpawnerForFirstTimeSummaryStatistic() + "\n");
                
                bW.write("higherPopulatedLatitude;" + group.getHigherPopulatedLatitude() + "\n");
                
                bW.write("graine;" + group.getPilot().getParameters().getRngStatusIndex() + "\n");
                
                bW.write("finalProbOfPres");
                for (int i = 0; i < nbBV; i++) {
                    bW.write(";" + finalProbabilityOfPresence[i]);
                }
                bW.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void computeKapa(BufferedWriter writer, String fileNameInputForInitialObservation, int[] finalStatesForKappa, String[] finalStatesNames) throws IOException {

        // 1 : read input file of observation
        FileReader reader;
        Scanner scanner;
        Map<String, Integer> obs1900 = new HashMap<String, Integer>();
        try {
            reader = new FileReader(fileNameInputForInitialObservation);
            // Parsing the file
            scanner = new Scanner(reader);
            scanner.useLocale(Locale.ENGLISH); // to have a comma as decimal separator !!!
            scanner.useDelimiter(Pattern.compile("[;\r]"));

            scanner.nextLine(); // to skip the file first line of entete

            while (scanner.hasNext()) {
                obs1900.put(scanner.next().replaceAll("\n", ""), scanner.nextInt());
            }
            reader.close();

        } catch (IOException ex) {
            Logger.getLogger(TypeTrajectoryCV.class.getName()).log(Level.SEVERE, null, ex);
        }

        // 2 : compute the confusion matrix between observed and computed data
        //             obs      0       1
        //      pred    0       VN      FN
        //              1       FP      VP
        double[][] confusionMatrix = new double[2][2];
        int computedVal;
        int obsVal;
        int nbVal = 0;
        for (int i = 0; i < finalStatesNames.length; i++) {
            if (obs1900.containsKey(finalStatesNames[i])) {
                computedVal = (finalStatesForKappa[i] > 0) ? 1 : 0;
                obsVal = obs1900.get(finalStatesNames[i]);
                if (computedVal == 0) {
                    if (obsVal == 0) {
                        confusionMatrix[0][0]++;
                    } else {
                        confusionMatrix[0][1]++;
                    }
                } else {
                    if (obsVal == 0) {
                        confusionMatrix[1][0]++;
                    } else {
                        confusionMatrix[1][1]++;
                    }
                }
                nbVal++;
            }
        }
        // normalisation
        confusionMatrix[0][0] = confusionMatrix[0][0] / nbVal;
        confusionMatrix[0][1] = confusionMatrix[0][1] / nbVal;
        confusionMatrix[1][0] = confusionMatrix[1][0] / nbVal;
        confusionMatrix[1][1] = confusionMatrix[1][1] / nbVal;

        // 3 compute kapa
        // kapa= (pra-pre)/(1-pre)
        // with pra=VN+VP and pre=(VN+FN)*(VN+FP)+(FN+VP)*(FP+VP)
        double VN = confusionMatrix[0][0];
        double FN = confusionMatrix[0][1];
        double FP = confusionMatrix[1][0];
        double VP = confusionMatrix[1][1];
        double pra = VN + VP;
        double pre = (VN + FN) * (VN + FP) + (FN + VP) * (FP + VP);
        double kapa = (pra - pre) / (1 - pre);

        writer.write("confusion matrix(VN,FN,FP,VP);");
        writer.write(((Double) confusionMatrix[0][0]).toString());
        writer.write(";");
        writer.write(((Double) confusionMatrix[0][1]).toString());
        writer.write(";");
        writer.write(((Double) confusionMatrix[1][0]).toString());
        writer.write(";");
        writer.write(((Double) confusionMatrix[1][1]).toString());
        writer.write("\nkappa;");
        writer.write(((Double) kapa).toString());

        //System.out.println("kappa value: " + kapa);
    }

}
