obsFile = newFile();
obsFile << "<hashtable></hashtable>";

arguments = ['-simDuration','802','-simBegin','2','-timeStepDuration','1',
  '-groups', groupsFile.getAbsolutePath(),'-env',envFile.getAbsolutePath(),'-observers',obsFile.getAbsolutePath(),
  '-RNGStatusIndex', replicat
  ] as String[];

Pilot.init();
BatchRunner.parseArgs(arguments,false,true,false);

ReflectUtils.setFieldValueFromPath(Pilot.getInstance(), "aquaticWorld.environment.interDistance", interdistance);
ReflectUtils.setFieldValueFromPath(Pilot.getInstance(), "aquaticWorld.environment.surfaceOfBv2", surfaceOfBV2);
ReflectUtils.setFieldValueFromPath(Pilot.getInstance(), "aquaticWorld.aquaNismsGroupsList.0.lFirstMaturity", lFirstMaturity);
path = "aquaticWorld.aquaNismsGroupsList.0.processes.";
ReflectUtils.setFieldValueFromPath(Pilot.getInstance(), path+"processesEachStep.5.eta", eta);
ReflectUtils.setFieldValueFromPath(Pilot.getInstance(), path+"processesEachStep.5.ratioS95_S50", ratioS95S50);
ReflectUtils.setFieldValueFromPath(Pilot.getInstance(), path+"processesEachStep.5.lambda", lambda);
ReflectUtils.setFieldValueFromPath(Pilot.getInstance(), path+"processesEachStep.5.survOptRep", survOptRep);
ReflectUtils.setFieldValueFromPath(Pilot.getInstance(), path+"processesEachStep.5.sigmaRecruitment", sigmaRep);
ReflectUtils.setFieldValueFromPath(Pilot.getInstance(), path+"processesEachStep.2.kOpt", kOptGrow);
ReflectUtils.setFieldValueFromPath(Pilot.getInstance(), path+"processesEachStep.2.sigmaDeltaLVonBert", sigmaGrow);
ReflectUtils.setFieldValueFromPath(Pilot.getInstance(), path+"processesEachStep.4.mortalityRateInSea", mortalityRateInSea);
ReflectUtils.setFieldValueFromPath(Pilot.getInstance(), path+"processesEachStep.3.pHoming", pHoming);
ReflectUtils.setFieldValueFromPath(Pilot.getInstance(), path+"processesEachStep.3.weightOfDeathBasin", weightOfDeathBasin);

BatchRunner.load();
Pilot.run()

basinsNames = ReflectUtils.getValueFromPath(Pilot.getInstance(),"aquaticWorld.environment.getRiverBasinNames")
bn1=basinsNames[0]
bn2=basinsNames[1]
finalStates = ReflectUtils.getValueFromPath(Pilot.getInstance(),"aquaticWorld.environment.getFinalStatesWithStochasticity")
fs1=finalStates[0]
fs2=finalStates[1]
mlrs = ReflectUtils.getValueFromPath(Pilot.getInstance(),"aquaticWorld.environment.getMeanLastRecruitments")
mlr1=mlrs[0]
mlr2=ReflectUtils.getValueFromPath(Pilot.getInstance(),"aquaticWorld.environment.getMeanLastRecruitmentsBV2")
mlres = ReflectUtils.getValueFromPath(Pilot.getInstance(),"aquaticWorld.environment.getMeanLastRecruitmentExpectations")
mlre1=mlres[0]
mlre2=mlres[1]
gmropcs = ReflectUtils.getValueFromPath(Pilot.getInstance(),"aquaticWorld.environment.getGeoMeansLastRecsOverProdCaps")
gmropc1 = gmropcs[0]
gmropc2 = gmropcs[1]
