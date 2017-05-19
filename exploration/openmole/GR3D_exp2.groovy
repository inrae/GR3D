obsFile = newFile();
obsFile << "<hashtable></hashtable>";

arguments = ['-simDuration','1202','-simBegin','2','-timeStepDuration','1',
  '-groups', groupFile.getAbsolutePath(),'-env',envFile.getAbsolutePath(),'-observers',obsFile.getAbsolutePath(),
  '-RNGStatusIndex', replicat
  ] as String[];

Pilot pilot = new Pilot();
BatchRunner runner = new BatchRunner(pilot);
        
pilot.init();
runner.parseArgs(arguments,false,true,false);

// ReflectUtils.setFieldValueFromPath(pilot, "aquaticWorld.environment.temperatureCatchmentFile", temperatureCatchmentFile);
ReflectUtils.setFieldValueFromPath(pilot, "aquaticWorld.aquaNismsGroupsList.0.parameterSetLine", parameterSetLine);
ReflectUtils.setFieldValueFromPath(pilot, "aquaticWorld.aquaNismsGroupsList.0.basinsToUpdateFile", basinsToUpdateFile);

pilot.load();
pilot.run()