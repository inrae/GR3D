
// /usr/local/openmole/openmole -c -configuration /home/win/BORDEAUX/thibaud.rougier/GR3D/ -p /home/win/BORDEAUX/thibaud.rougier/GR3D/target/GR3D-1.0-SNAPSHOT.jar -s /home/win/BORDEAUX/thibaud.rougier/GR3D/4thExlplo_PubliGR3D/GR3D_openmole_PubliGR3D.scala

// cd D:\workspace\GR3D\
// c:\"Program Files"\OpenMOLE\openmole.bat -c -p target\GR3D-2.0-SNAPSHOT.jar -s exploration\openmole\GR3D_exp2.scala 2>exploration\openmole\erreurExp1.txt 

//1>&2


import org.openmole.plugin.task.groovy._
import org.openmole.plugin.domain.range._
import org.openmole.plugin.hook.display._
import org.openmole.plugin.environment.pbs._
//import org.openmole.core.model.execution.Environment
import org.openmole.plugin.domain.collection._
import org.openmole.plugin.sampling.combine._
import org.openmole.plugin.domain.distribution._
import org.openmole.plugin.hook.display._
import org.openmole.plugin.hook.file._

//val wd = "/home/dumoulin/Documents/SimAquaLife/Applications/GR3D/"
//val wd = "/home/win/BORDEAUX/thibaud.rougier/GR3D"
val wd = "D:/workspace/GR3D/"

//logger.level("FINE")
// val env = new PBSEnvironment("rougier", "calcul64.clermont.cemagref.fr")

//val temperatureCatchmentFile = Val[String]
val basinsToUpdateFile = Val[String]
val parameterSetLine= Val[Int]
val replicat = Val[Int]

val groupFile = Val[File]
val envFile = Val[File]

val explo = ExplorationTask(
    //(temperatureCatchmentFile in List("D:/workspace/GR3D/input/reality/SeasonTempBVFacAtlant1801_2100_newCRU_RCP85.csv") toDomain) x
	(basinsToUpdateFile in List(
		"data/input/reality/sansGaronne.csv",
		"data/input/reality/sansSeudre.csv",
		"data/input/reality/sansCharente.csv",
		"data/input/reality/sansSevre_Niortaise.csv",
		"data/input/reality/sansLay.csv",
		"data/input/reality/sansAuzance.csv",
		"data/input/reality/sansLoire.csv", 
		"data/input/reality/sansVilaine.csv",
		"data/input/reality/sansBlavet.csv",
		"data/input/reality/sansScorff.csv",
		"data/input/reality/sansOdet.csv",
		"data/input/reality/sansAulne.csv",
		"data/input/reality/sansElorn.csv",
		"data/input/reality/sansLeguer.csv",
		"data/input/reality/sansLeff.csv",
		"data/input/reality/sansTrieux.csv",
		"data/input/reality/sansRance.csv",
		"data/input/reality/sansCouesnon.csv",
		"data/input/reality/sansSelune.csv",
		"data/input/reality/sansSienne.csv",
		"data/input/reality/sansDouve.csv",
		"data/input/reality/sansVire.csv",
		"data/input/reality/sansOrne.csv",
		"data/input/reality/sansDives.csv",
		"data/input/reality/sansTouques.csv",
		"data/input/reality/sansSeine.csv")) x
	(parameterSetLine in  (1 to 10 by 1)) x
	(replicat in (1 to 30 by 1))
    )

val model = GroovyTask(scala.io.Source.fromFile(wd+"exploration/openmole/GR3D_exp2.groovy").mkString) set (
    imports += "fr.cemagref.simaqualife.extensions.pilot.BatchRunner",
    imports += ("fr.cemagref.simaqualife.pilot.Pilot","miscellaneous.ReflectUtils"),
    inputs += (parameterSetLine, replicat, groupFile, envFile),
	inputs += (basinsToUpdateFile),
    outputs += replicat,
    // default values
    groupFile := File(wd+"data/input/fishTryRealBV_CC.xml"),
    envFile := File(wd+"data/input/BNtryRealBasins.xml"),
	basinsToUpdateFile := "",
    parameterSetLine := 0,
    replicat := 0
)

//val h = new ToStringHook
val h = AppendToCSVFileHook(wd + "data/output/outputExp1.txt")

//val ex = model toExecution
val ex = explo -< (model hook h) toExecution
ex.start
ex.waitUntilEnded