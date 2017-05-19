// cd C:\Program Files\OpenMOLE
// /usr/local/openmole/openmole -c -configuration /home/win/BORDEAUX/thibaud.rougier/GR3D/ -p /home/win/BORDEAUX/thibaud.rougier/GR3D/target/GR3D-1.0-SNAPSHOT.jar -s /home/win/BORDEAUX/thibaud.rougier/GR3D/4thExlplo_PubliGR3D/GR3D_openmole_PubliGR3D.scala
// openmole.bat -c -p D:\workspace\GR3D\target\GR3D-1.0-SNAPSHOT.jar -s D:\workspace\GR3D\exploration\openmole\GR3D_test.scala 2>D:\workspace\GR3D\exploration\openmole\erreurTest.txt


import org.openmole.plugin.task.groovy._
import org.openmole.plugin.domain.range._
import org.openmole.plugin.hook.display._
import org.openmole.plugin.environment.pbs._
import org.openmole.core.model.execution.Environment
import org.openmole.plugin.domain.collection._
import org.openmole.plugin.sampling.combine._
import org.openmole.plugin.domain.distribution._
import org.openmole.plugin.hook.display._
import org.openmole.plugin.hook.file._

//val wd = "/home/dumoulin/Documents/SimAquaLife/Applications/GR3D-ORIGINAL/"
//val wd = "/home/win/BORDEAUX/thibaud.rougier/GR3D"
val wd = "D:/workspace/GR3D/"

//logger.level("FINE")
// val env = new PBSEnvironment("rougier", "calcul64.clermont.cemagref.fr")

// Reproduce Parameter
val eta = Prototype[Double]("eta")
val ratioS95S50 = Prototype[Double]("ratioS95S50")
val lambda = Prototype[Double]("lambda")
val survOptRep = Prototype[Double]("survOptRep")
val sigmaRep = Prototype[Double]("sigmaRep")

// Growth parameter
val kOptGrow = Prototype[Double]("kOptGrow")
val sigmaGrow = Prototype[Double]("sigmaGrow")

// Survive Parameter
val mortalityRateInSea = Prototype[Double]("mortalityRateInSea")

// Maturation
val lFirstMaturity = Prototype[Double]("lFirstMaturity")

// Anadromous migration
val pHoming = Prototype[Double]("pHoming")
val weightOfDeathBasin = Prototype[Double]("weightOfDeathBasin")

// Environment
val interdistance = Prototype[Double]("interdistance")
val surfaceOfBV2 = Prototype[Double]("surfaceOfBV2")

// Replicat
val replicat = Prototype[Int]("replicat")

val groupsFile = Prototype[File]("groupsFile")
val envFile = Prototype[File]("envFile")

// Output
val bn1 = Prototype[String]("bn1")
val bn2 = Prototype[String]("bn2")
val fs1 = Prototype[Int]("fs1")
val fs2 = Prototype[Int]("fs2")
val mlr1 = Prototype[Double]("mlr1")
val mlr2 = Prototype[Double]("mlr2")
val mlre1 = Prototype[Double]("mlre1")
val mlre2 = Prototype[Double]("mlre2")
val gmropc1 = Prototype[Double]("gmropc1")
val gmropc2 = Prototype[Double]("gmropc2")

val explo = ExplorationTask("explo",
    Factor(eta, List(2.4) toDomain) x
	Factor(ratioS95S50, List(2.0) toDomain) x
	Factor(lambda, List(0.00041) toDomain) x
	Factor(survOptRep, List(0.0017) toDomain) x
	Factor(sigmaRep, List(0.0001) toDomain) x
	Factor(kOptGrow, List(0.3) toDomain) x
	Factor(sigmaGrow, List(0.0001) toDomain) x
	Factor(mortalityRateInSea, List(0.4) toDomain) x
	Factor(lFirstMaturity, List(40.0) toDomain) x
	Factor(pHoming, List(0.8) toDomain) x
	Factor(weightOfDeathBasin, List(0.2) toDomain) x
	Factor(surfaceOfBV2, List(20000.0) toDomain) x
	Factor(interdistance, List(100.0) toDomain) x  
	Factor(replicat, 1 to 1 by 1 toDomain)
    )


val model = GroovyTask("model", scala.io.Source.fromFile(wd+"exploration/openmole/GR3D_test.groovy").mkString)
model addImport "fr.cemagref.simaqualife.extensions.pilot.BatchRunner"
model addImport "fr.cemagref.simaqualife.pilot.Pilot"
model addImport "miscellaneous.ReflectUtils"
model addInput eta
model addInput ratioS95S50
model addInput lambda
model addInput survOptRep
model addInput sigmaRep
model addInput kOptGrow
model addInput sigmaGrow
model addInput mortalityRateInSea
model addInput lFirstMaturity
model addInput pHoming
model addInput weightOfDeathBasin
model addInput surfaceOfBV2
model addInput interdistance
model addInput replicat
model addParameter (groupsFile -> new File(wd+"exploration/openmole/fishTry2BV.xml"))
model addParameter (envFile -> new File(wd+"exploration/openmole/BNtry2Basins.xml"))
model addOutput eta
model addOutput ratioS95S50
model addOutput lambda
model addOutput survOptRep
model addOutput sigmaRep
model addOutput kOptGrow
model addOutput sigmaGrow
model addOutput mortalityRateInSea
model addOutput lFirstMaturity
model addOutput pHoming
model addOutput weightOfDeathBasin
model addOutput surfaceOfBV2
model addOutput interdistance
model addOutput replicat
model addOutput bn1
model addOutput bn2
model addOutput fs1
model addOutput fs2
model addOutput mlr1
model addOutput mlr2
model addOutput mlre1
model addOutput mlre2
model addOutput gmropc1
model addOutput gmropc2

//val h = new ToStringHook
val h = AppendToCSVFileHook("SAL/outputTest.txt")

val ex = explo -< (model hook h) toExecution
ex.start
ex.waitUntilEnded