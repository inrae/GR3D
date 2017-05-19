####################################
## This script use EasyABC for calibrating GR3D Model.
## Usage:
##   Create a new directory under data/output
##   Copy the sample file ABC.yaml into this created directory
##   Edit your ABC.yaml input file for your needs
##   Run the script with the name of your directory as argument, like:
##  Rscript ABC.R calibrationDirectory

rm(list=ls())

set.seed(1)

workingDir = commandArgs(TRUE)
if (length(workingDir)==0) {
  stop("This script need a argument for the working directory")
}
outputDir = "simus/"
if (file.exists(paste(workingDir,"/",outputDir,sep=""))) {
  cat("The simulations output dir already exists. Are you sure to continue? [Y/n]: ")
  i=readLines(con="stdin", 1)
  if (nchar(i)!=0 && i!="Y" && i!="y") {
    stop("exiting")
  }
}

## Read the input file and load data
inputFile = paste(workingDir,"/ABC_seq_2param_3stats_NewAfterSA.yaml",sep="")
if (!file.exists(inputFile)) {
  stop(paste("File not found: ",inputFile,sep=""))
}
library(yaml)
inputData = yaml.load_file(inputFile)
# load prior
prior=list()
for (i in 1:length(inputData$parameters)) {
  p = inputData$parameters[[i]]
  prior[[i]] = unlist(p$prior)
}

salrun <- function(jarfile,simDuration,simBegin,timeStepDuration=1) {
  assign("inputData",inputData)
  assign("outputDir",outputDir)
  function(parameters) {
    # loading the user's data
    parametersNames=list()
    for (i in 1:length(inputData$parameters)) {
      parametersNames = c(parametersNames, inputData$parameters[[i]]$javapath)
    }
    # extracting the seed and the model parameters
    seed=parameters[1]
    thetas=parameters[2:length(parameters)]
    outputFile = paste(outputDir,"output",seed,sep="")
    if (!file.exists(paste(outputFile,".csv",sep=""))) {
      # run
      arguments=c('-q','-simDuration',simDuration, '-simBegin',simBegin,
        '-timeStepDuration',timeStepDuration, '-groups',"data/input/fishTryRealBV_CC.xml",
        '-env',"data/input/BNtryRealBasins.xml",'-observers',"data/input/obsTryRealABC.xml", '-RNGStatusIndex', format(seed,scientific=FALSE))
      library("rJava")
      .jinit(classpath=jarfile)#, force.init=TRUE)
      .jcall("miscellaneous.EasyABC","V","runSimulation",arguments, outputFile,.jarray(unlist(parametersNames)),.jarray(thetas))
    }
    # return the kappa
    likelihood=as.double(as.matrix(read.csv(paste(outputFile,".csv",sep=""), skip=10, h=F, sep=";"))[1,2])
    agestat=as.double(as.matrix(read.csv(paste(outputFile,".csv",sep=""), skip=11, h=F, sep=";"))[1,2])
    maxlat=as.double(as.matrix(read.csv(paste(outputFile,".csv",sep=""), skip=12, h=F, sep=";"))[1,2])
    if (maxlat>0)
      c(likelihood,agestat,maxlat)
    else
      c(NA,NA,NA)
  }
}

setwd('ABC_seq_2param_3stats_NewAfterSA/')

model = salrun(
  jarfile = "GR3D-1.0-SNAPSHOT.jar",
  simDuration = 400,
  simBegin = 2)
# model(c(1,0.679266600369595))

library(lhs)
library(mnormt)
library(parallel)
source("/home/dumoulin/easyabc/pkg/R/EasyABC-internal.R")
source("/home/dumoulin/easyabc/pkg/R/ABC_sequential.R")
#library("EasyABC")

result=ABC_sequential(method="Lenormand", model=model, prior=prior, nb_simul=inputData$nbSimul, summary_stat_target=c(0.0,0.0,53.55), p_acc_min=inputData$p_acc_min,  alpha=inputData$alpha, use_seed=TRUE, progress_bar=TRUE, verbose=TRUE, n_cluster=inputData$nbCluster)
save(result,file=paste("result.data",sep=""))

png(file="density.png")
par(mfrow=c(1,2))
for (i in 1:length(inputData$parameters)) {
  # cutting the variable name (too long with "processes.processesEachStep")
  title=substring(inputData$parameters[[i]]$javapath,29)
  plot(main=title,density(result$param[,i],weights=result$weights))
}
dev.off()