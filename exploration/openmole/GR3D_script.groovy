obsFile = newFile();
obsFile << "<hashtable></hashtable>";

arguments = ['-simDuration','802','-simBegin','2','-timeStepDuration','1',
  '-groups', groupsFile.getAbsolutePath(),'-env',envFile.getAbsolutePath(),'-observers',obsFile.getAbsolutePath(),
  '-RNGStatusIndex', replicat
  ] as String[];

Pilot.init();
BatchRunner.parseArgs(arguments,false,true,false);

ReflectUtils.setFieldValueFromPath(Pilot.getInstance(), "aquaticWorld.environment.surfaceOfBv1", surfaceOfBV1);
ReflectUtils.setFieldValueFromPath(Pilot.getInstance(), "aquaticWorld.environment.surfaceOfBv2", surfaceOfBV2);
ReflectUtils.setFieldValueFromPath(Pilot.getInstance(), "aquaticWorld.environment.interDistance", interdistance);
path = "aquaticWorld.aquaNismsGroupsList.0.processes.";
ReflectUtils.setFieldValueFromPath(Pilot.getInstance(), path+"processesAtBegin.0.nbSIPerBasin", nbInd);
ReflectUtils.setFieldValueFromPath(Pilot.getInstance(), path+"processesEachStep.4.pHoming", pHoming);
ReflectUtils.setFieldValueFromPath(Pilot.getInstance(), path+"processesEachStep.5.ratioS95_S50", ratioS95S50);
ReflectUtils.setFieldValueFromPath(Pilot.getInstance(), path+"processesEachStep.4.weightOfDeathBasin", weightOfDeathBasin);

BatchRunner.load();
Pilot.run()

basinsNames = ReflectUtils.getValueFromPath(Pilot.getInstance(),"aquaticWorld.environment.getBasinsNames")
bn1=basinsNames[0]
bn2=basinsNames[1]
finalStates = ReflectUtils.getValueFromPath(Pilot.getInstance(),"aquaticWorld.environment.getFinalStates")
fs1=finalStates[0]
fs2=finalStates[1]
mlrs = ReflectUtils.getValueFromPath(Pilot.getInstance(),"aquaticWorld.environment.getMeanlastRecruitments")
mlr1=mlrs[0]
mlr2=mlrs[1]
