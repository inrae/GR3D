// openmole.bat -c -p C:\WorkspaceJava\GR3D\target\GR3D-1.0-SNAPSHOT.jar -s C:\WorkspaceJava\GR3D\exploration\openmole\GR3D_openmole.scala

import org.openmole.plugin.task.groovy._
import org.openmole.plugin.domain.range._
import org.openmole.plugin.hook.display._
import org.openmole.plugin.environment.pbs._
import org.openmole.plugin.domain.collection._
import org.openmole.plugin.sampling.combine._
import org.openmole.plugin.domain.distribution._
import org.openmole.plugin.hook.display._
import org.openmole.plugin.hook.file._
import org.openmole.plugin.environment.glite._
import org.openmole.plugin.grouping.batch._

//val wd = "/home/dumoulin/Documents/SimAquaLife/Applications/GR3D-ORIGINAL/"
val wd = "C:/WorkspaceJava/GR3D/"

logger.level("FINE")
val cluster = new PBSEnvironment("dumoulin", "calcul64.clermont.cemagref.fr")

val ratioS95S50 = Prototype[Double]("ratioS95S50")
val interdistance = Prototype[Double]("interdistance")
val nbInd = Prototype[Int]("nbInd")
val groupsFile = Prototype[File]("groupsFile")
val envFile = Prototype[File]("envFile")
val surfaceOfBV1 = Prototype[Double]("surfaceOfBV1")
val surfaceOfBV2 = Prototype[Double]("surfaceOfBV2")
val weightOfDeathBasin = Prototype[Double]("weightOfDeathBasin")
val pHoming = Prototype[Double]("pHoming")
val replicat = Prototype[Int]("replicat")

val bn1 = Prototype[String]("bn1")
val bn2 = Prototype[String]("bn2")
val fs1 = Prototype[Int]("fs1")
val fs2 = Prototype[Int]("fs2")
val mlr1 = Prototype[Double]("mlr1")
val mlr2 = Prototype[Double]("mlr2")

val explo = ExplorationTask("explo",
    Factor(ratioS95S50, List(1.8, 2) toDomain) x
    Factor(interdistance, List(100.0, 300.0) toDomain) x
    Factor(nbInd, List(500, 5000) toDomain) x
    Factor(surfaceOfBV1, List(10000.0, 50000.0, 100000.0) toDomain) x
    Factor(surfaceOfBV2, List(10000.0, 50000.0, 100000.0) toDomain) x
    Factor(weightOfDeathBasin, List(0.2, 0.4, 0.6) toDomain) x
    Factor(pHoming, List(0.2, 0.5, 0.9) toDomain) x
    Factor(replicat, 0 to 9 by 1 toDomain)
    )

val model = GroovyTask("model", scala.io.Source.fromFile(wd+"exploration/openmole/GR3D_script.groovy").mkString)
model addImport "fr.cemagref.simaqualife.extensions.pilot.BatchRunner"
model addImport "fr.cemagref.simaqualife.pilot.Pilot"
model addImport "miscellaneous.ReflectUtils"
model addParameter (ratioS95S50 -> 1.8)
model addParameter (interdistance -> 100.0)
model addParameter (nbInd -> 5000)
model addParameter (groupsFile -> new File(wd+"data/input/fishTry2BV.xml"))
model addParameter (envFile -> new File(wd+"data/input/BNtry2Basins.xml"))
model addParameter (surfaceOfBV1 -> 10000.0)
model addParameter (surfaceOfBV2 -> 10000.0)
model addParameter (weightOfDeathBasin -> 0.4)
model addParameter (pHoming -> 0.9)
model addParameter (replicat -> 0)
model addOutput ratioS95S50
model addOutput interdistance
model addOutput nbInd
model addOutput surfaceOfBV1
model addOutput surfaceOfBV2
model addOutput weightOfDeathBasin
model addOutput pHoming
model addOutput replicat
model addOutput bn1
model addOutput bn2
model addOutput fs1
model addOutput fs2
model addOutput mlr1
model addOutput mlr2

//val h = new ToStringHook
val h = new AppendToCSVFileHook(wd+"data/output/output.txt")

// on grid
val ex = explo -< (model on complexsystems by 2000 hook h) toExecution
// on cluster
//val ex = explo -< (model on cluster by 600 hook h) toExecution

ex.start
ex.waitUntilEnded