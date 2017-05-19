rm(list=ls())

# size of the plots
WIDTH=1024
HEIGHT=768

set.seed(1)

workingDir = commandArgs(TRUE)
if (length(workingDir)==0) {
  stop("This script need a argument for the working directory")
}

## Read the input file and load data
inputFile = paste(workingDir,"/ABC_seq_2param_3stats_NewAfterSA.yaml",sep="")
if (!file.exists(inputFile)) {
  stop(paste("File not found: ",inputFile,sep=""))
}
library(yaml)
inputData = yaml.load_file(inputFile)
nbParam = length(inputData$parameters)
nbStats = 3
statsNames = c("likelihood","agestat","maxlat")

for (file in Sys.glob(paste(workingDir,"output_step*",sep="/"))) {
  stepIndex=gsub('.*step(.*)','\\1',file)
  stepIndex=sprintf("%03d", as.integer(stepIndex))
  data <- read.table(file, quote="\"")
  # plot density
  png(paste(workingDir,"/density_step",stepIndex,".png",sep=""), width=WIDTH, height=HEIGHT)
  par(mfrow=c(1,nbParam),oma = c( 0, 0, 3, 0 ) )
  for (i in (1:nbParam)) {
    paramName=substring(inputData$parameters[[i]]$javapath,29)
    plot(density(data[,i+1],weights=data[,1]),main=paste("density of ",paramName,sep=""),xlab=paramName)
  }
  title(paste("Posteriors at step ",stepIndex,sep=""), outer = TRUE )
  dev.off()
  # plot simulations
  png(paste(workingDir,"/simulations_step",stepIndex,".png",sep=""), width=WIDTH, height=HEIGHT)
  par(mfrow=c(nbStats,nbParam),oma = c( 0, 0, 3, 0 ) )
  for (stat in (1:nbStats)) for (i in (1:nbParam)) {
    paramName=substring(inputData$parameters[[i]]$javapath,29)
    plot(data[,i+1],data[,stat+1+nbParam],main=paste(statsNames[stat]," / ",paramName),xlab=paramName,ylab=statsNames[stat])
  }
  title(paste("Simulations stats at step ",stepIndex,sep=""), outer = TRUE )
  dev.off()
}

# mencoder mf://density_step*.png -mf w=1024:h=768:fps=4:type=png -ovc lavc -o density.avi
# mencoder mf://simulations_step*.png -mf w=1024:h=768:fps=4:type=png -ovc lavc -o simulations.avi