
setwd("/home/thierry/work/Simaqualife/GR3D/analysis")
% lecture des données
data=read.table(file="output2.txt",header=TRUE,sep=',')

dataname="output_1280"
data=read.table(file=paste(dataname,".txt",sep=""),header=TRUE,sep=',')
attach(data) %% pour 

% réorganisation des données
% solution 1 : on fait l'analyse pour les valeurs d'un seul réplicat
data1=subset(data,replicat==1);
Fit1 <- summary(aov(fs1 ~ pHoming*interdistance*nbInd*surfaceOfBV1*surfaceOfBV2*ratioS95S50*weightOfDeathBasin, data = data1))
Fit2 <- summary(aov(fs2 ~ pHoming*interdistance*nbInd*surfaceOfBV1*surfaceOfBV2*ratioS95S50*weightOfDeathBasin, data = data1))

# Calcul des indices
SumSq1 <- Fit[[1]][,2]
Total1 <- length(data1$interdistance)*var(fs1)
Indices1 <- 100*SumSq1/Total1
print(Indices1)
TabIndices1 <- cbind(Fit1[[1]],Indices1)
#suprint(TabIndices)
TabIndices1 <- TabIndices1[order(Indices1, decreasing=T),]
print(TabIndices1)

SumSq2 <- Fit2[[1]][,2]
Total2 <- length(data1$interdistance)*var(fs1)
Indices2 <- 100*SumSq2/Total2
print(Indices2)
TabIndices2 <- cbind(Fit2[[1]],Indices2)
#suprint(TabIndices)
TabIndices2 <- TabIndices2[order(Indices2, decreasing=T),]
print(TabIndices2)



# plot des 10 plus grands effets
pdf(paste(dataname,"_indiceglob.pdf",sep=""), bg = "white")
plot(TabIndices1$Indices1[1:10],xlab="factor",ylab="factor effect in %",axes=F, ann=T,main=paste("global indice for",dataname))
axis(1, lab=F,cex.axis=0.7)
# Plot x axis labels at default tick marks with labels at 45 degree angle
text(axTicks(1), par("usr")[3] - 2, srt=60, adj=1,labels=TabNames1[1:10], xpd=T, cex=0.3)
axis(2, las=1, cex.axis=0.8)
box()
dev.off()


# Calcul des indices globaux (todo)
TabNames=sub(" +$", "", row.names(TabIndices))
TabNames== "pHoming"

names=c("interdistance","weightOfDeathBasin","ratioS95S50" ,"nbInd","pHoming","surfaceOfBV2","surfaceOfBV1")
IndiceTot=sum(TabIndices$Indices[which(grepl(name,TabNames))])
name="pHoming"
IndiceTot=1
i=1;
for (n in names) { 
  IndiceTot[i]=sum(TabIndices$Indices[which(grepl(n,TabNames))])
  i=i+1
}
indiceTot=data.frame(IndiceTot,row.names=names)
print(indiceTot)
ordre=order(indiceTot$IndiceTot,decreasing=T)

// plot the data 
#pdf(paste(dataname,"_indiceglob.pdf",sep=""), bg = "white")
#jpeg('E:/my_graphs/test1.jpg', quality = 100, bg = "white", res = 200, width = 7, height = 7, units = "in") 

plot(indiceTot[ordre,],xlab="factor",ylab="total effect in %",axes=F, ann=T,main=paste("global indice for",dataname))
axis(1, lab=F,cex.axis=0.7)
# Plot x axis labels at default tick marks with labels at 45 degree angle
text(axTicks(1), par("usr")[3] - 2, srt=60, adj=1,labels=names[ordre], xpd=T, cex=0.6)
axis(2, las=1, cex.axis=0.8)
box()
dev.off()


// solution 2 : on prend le plus problable des réplicats
// pour récupérer les modalités les plus importantes
a <- c(letters[1:10], 'b','f','f')
which.max(table(a))

