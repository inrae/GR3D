# # Code article V1.2 avec ajout DDL
# February 2019

library(dplyr)
library(tidyr)
library(stats)
library(lubridate)
library(purrr)
library(suncalc)
library(furrr)
library(tictoc)
library(caret)
library(readr)
library(patchwork)
library(dismo)
library(gbm)
library(vioplot)
library(fields)
library(gam)
library(voxel)
library(aplpack)
library(scales)
library(tidyverse)

rm(list=ls())

USER="Alexis"

if(USER=="Alexis"){
  repertoire_travail<-getwd()
  repertoire_data<-paste0(repertoire_travail,"/data/input/")
  repertoire_dataGW<-paste0(repertoire_travail,"/data/GlobalWarming/")
  repertoire_sorties<-paste0(repertoire_travail,"/data/output/")  
}else{
  repertoire_travail <- getwd()
  repertoire_data <- "../data/input/"
  repertoire_sorties <- "../data/output/"
} 

#----------------------------------------------------------------------------
#La fonction de contour remasterisée
load(paste0(repertoire_sorties, "contourGBM.RData"))
#La fonction de calcul de dayl-length
load(paste0(repertoire_sorties, "dayLight.RData"))
#----------------------------------------------------------------------------


# Day length -------------------------------------------------------------
st <- as.Date("2019-01-01")
en <- as.Date("2019-12-31")
calendar <- seq.Date(st, en, by = "day")

dayL<-data.frame(date=calendar,
                 site=c(rep("Garonne", 365),rep("Dordogne",365)),
                 DL=c(dayLight(dates=calendar), 
                      dayLight(dates=calendar, latitude = 44.8333 , longitude=0.35)))

dayL %>% 
  group_by(site) %>% 
  mutate(deltaDL1=ifelse(row_number() == 1, NaN, DL - lag(DL, 1)),
         dataLink=format(date, "%d-%m"),
         jourDeAnnee=lubridate::yday(date)) %>% 
  ungroup() %>% 
  as.data.frame()->dayL

# Day length -------------------------------------------------------------



# Data env + reproduction -------------------------------------------------------------

envCondition<-read.table(paste0(repertoire_data, "envCondition.csv"), 
                         header=TRUE, sep=";") %>%  
  mutate(jourDeAnnee=lubridate::yday(date)) %>% 
  filter(jourDeAnnee %in% c(91:213))->data
data$date<-as.Date(data$date)
data$dataLink=base::format(data$date, "%d-%m")


repro<-read.table(paste0(repertoire_data,"reproMonitoring.csv"), sep=";", header = T)
repro$date<-as.Date(repro$date)
repro$Presence<-as.numeric(repro$Bulls>0)

data<-right_join(x=data, y=repro, by=c("lien", "date"))

# Data env + reproduction -------------------------------------------------------------

data<-left_join(x=data, y=dayL, by=c("dataLink","site")) 
data<-data[,names(data) %in% c("lien", "site", 'date.x', "Temp1",
                               "deltaDL1.y", "Q1", "Presence")]

names(data)<-c("lien", "site", 'date', "Temp1", "Q1", "Presence", "deltaDL1")

# END du data--------------------------------------------------------------

indexes = sample(1:nrow(data),size=0.3*nrow(data))
trained_data = data[-indexes,]
test_data = data[indexes,]


# ------------------------------------------------------------------------------
# 1/
# Le modele BRT presence-absence 
# ------------------------------------------------------------------------------

Variable<-c("Temp1", "Q1", "deltaDL1")


brtTempPres<-gbm.step(data=trained_data, 
                      gbm.x = Variable, gbm.y = "Presence",
                      family = "bernoulli", tree.complexity = 5,
                      learning.rate = 0.001, bag.fraction = 0.5)

Relative_influence<-as.data.frame(summary(brtTempPres))

# ----------------------------------------------------------------------------

data$Preds<-predict.gbm(brtTempPres, data, n.trees=brtTempPres$gbm.call$best.trees, type="response")
d <- cbind(data$Presence, data$Preds)

pres <- d[d[,1]==1 , 2]
abs <- d[d[,1]==0 , 2]
e.dm<-evaluate(p=pres, a=abs)
e.dm
# Check the evaluation results
str(e.dm)
# Boxplot of presence and absence suitable values
# blue are absences, red are presences
boxplot(e.dm,col=c('blue','red'))
# Density plot of presence and absence suitable values
# blue are absences, red are presences
raster::density(e.dm)#
tr.b<-threshold(e.dm,'spec_sens')
# Get the value of the threshold=0.570914
abline(v=tr.b)

#----------------------------------------------------------------------
# Parital plot

par(mfrow=c(2,2))

a<-plot.gbm(brtTempPres, 
            i.var=1, 
            return.grid = TRUE, type="link")


b<-plot.gbm(brtTempPres, 
            i.var=2, 
            return.grid = TRUE, type="link")

c<-plot.gbm(brtTempPres, 
            i.var=3, 
            return.grid = TRUE, type="link")

a %>% 
  ggplot()+
  geom_line(aes(x=Temp1, y=y))+
  xlab("Water temperature (°C)")+
  ylab("Marginal effect on logit(p)")+
  theme_bw()->A

b %>% 
  ggplot()+
  geom_line(aes(x=Q1, y=y))+
  xlab("Water discharge (log scale; m3/s)")+
  ylab("Marginal effect on logit(p)")+
  theme_bw()->B

c %>% 
  ggplot()+
  geom_line(aes(x=deltaDL1, y=y))+
  xlab("Day-length difference (hours)")+
  ylab("Marginal effect on logit(p)")+
  theme_bw()->C

C+A+B+plot_layout(2,2)

#----------------------------------------------------------------------------
# 3-d dependanc plots


find.int <- gbm.interactions(brtTempPres)
find.int$interactions
find.int$rank.list

par(mfrow=c(2,2), mar=c(4,4,1,2))

# Temperature et delta day length
contourGBM(brtTempPres, 3, 1,perspective = FALSE, 
           smooth = "average", y.label='Water temperature (°C)',
           x.label="Day-length difference (hours)")

X<-data.frame(x=data$deltaDL1,
              y=data$Temp1)

aplpack::plothulls(X,fraction=1,add=TRUE, pch="", main="")

points(data$Temp1[data$Presence>0]~data$deltaDL1[data$Presence>0],
       pch="+",cex=.7)

box()

#-------------------------------------------------------

# Temeprature et debit
contourGBM(brtTempPres, 1, 2,perspective = FALSE, 
           smooth = "average", x.label='Water temperature (°C)',
           y.label="Water discharge (log scale; m3/s)")

X<-data.frame(x=data$Temp1,
              y=data$Q1)

aplpack::plothulls(X,fraction=1,add=TRUE, pch="", main="")

points(data$Q1[data$Presence>0] ~ data$Temp1[data$Presence>0],
       pch="+",cex=.7)

box()
#---------------------------------------------------------------

contourGBM(brtTempPres, 3, 2,perspective = FALSE, 
           smooth = "average", x.label="Day-length difference (hours)",
           y.label="Water discharge (log scale; m3/s)")

X<-data.frame(x=data$deltaDL1,
              y=data$Q1)

aplpack::plothulls(X,fraction=1,add=TRUE, pch="", main="")

points(data$deltaDL1[data$Presence>0],
       data$Q1[data$Presence>0],
       pch="+",cex=.7)
box()





