library(dplyr)
library(survival)
library(lme4)
#library(pracma)
#library(data.table)

rm( list = ls())

# ===== TSR Jatteau et al 2017 corrected (see script_TSR v3.R) ================================================

# with lme output
dailySurvivalEmbryo = function(temperature, modelSurvivalEmbryo, parDurationIncubation){
  sel = is.na(temperature)
  temperature[sel] = 0
  duration = round(parDurationIncubation[1] * temperature^parDurationIncubation[2])
  predict_matrix = model.matrix(~Temp+I(Temp^2)+I(Temp^3),data = data.frame(Temp = temperature))
  surv = (1 / (1 + exp(-predict_matrix %*% fixef(modelSurvivalEmbryo))))^(1 / duration)
  surv[sel] = NA
  return(surv)
}

dailySurvivalLarvae = function(temperature, modelSurvivalLarvae) {
  sel = is.na(temperature)
  temperature[sel] = 0
  pred_matrix = model.matrix(~Temp + I(Temp^2)+ I(Temp^3), data = data.frame(Temp = temperature))
  surv = exp(-1/exp(pred_matrix %*% (modelSurvivalLarvae$coefficients[1:4])))
  surv[sel] = NA
  return(surv)
}

# with lme coeff
dailySurvivalEmbryoCoeff = function(temperature, modelSurvivalEmbryoCoeff, parDurationIncubation){
  sel = is.na(temperature)
  temperature[sel] = 0
  duration = (parDurationIncubation[1] * temperature ^ parDurationIncubation[2]) # remove the round() to smooth the curve
  predict_matrix = model.matrix(~ Temp + I(Temp ^ 2) + I(Temp ^ 3), data = data.frame(Temp = temperature))
  surv = (1 / (1 + exp(-predict_matrix %*% modelSurvivalEmbryoCoeff))) ^ (1 / duration)
  surv[sel] = NA
  return(surv)
}

dailySurvivalLarvaeCoeff = function(temperature, modelSurvivalLarvaeCoeff) {
  sel = is.na(temperature)
  temperature[sel] = 0
  pred_matrix = model.matrix(~Temp + I(Temp^2) + I(Temp^3), data = data.frame(Temp = temperature)) 
  surv = exp(-1/exp(pred_matrix %*% (modelSurvivalLarvaeCoeff)))
  surv[sel] = NA
  return(surv)
}

parDurationIncubation = c(1124.531453,  -1.834438)
modelSurvivalEmbryoCoeff = c(-1.245015E+01,  1.120331E+00 , -7.830614E-03, -6.112982E-04)
modelSurvivalLarvaeCoeff = c(-1.851093E+00,  5.132855E-01, -1.020347E-02, -7.005887E-05 )
horizon = 14 
temperature = seq(5,35,.1)

embryoSurvival =  dailySurvivalEmbryoCoeff(temperature, modelSurvivalEmbryoCoeff, parDurationIncubation)
larvalSurvival =  dailySurvivalLarvaeCoeff(temperature, modelSurvivalLarvaeCoeff)^horizon

plot(temperature, embryoSurvival, type = 'l')
lines(temperature, larvalSurvival)

plot(temperature, embryoSurvival * larvalSurvival, type = 'l')

temperature = seq(5, 30,.1)
duration = (parDurationIncubation[1] * temperature ^ parDurationIncubation[2]) 
plot(temperature, duration, type ='l')

# ==== observed data ========================================================================
envGGD = read.csv("envCondition.csv", sep = ";")
repro = read.csv("reproMonitoring.csv", sep = ";")

envGGD = envGGD %>% mutate(dateC = as.character(date), date = as.Date(date)) %>% mutate(month = format(date, '%m'))
repro = repro %>% mutate(dateC = as.character(date), date = as.Date(date)) %>% mutate(month = format(date, '%m'))


elabData =   envGGD %>% left_join(select(repro, one_of(c("lien", "dateC", "Bulls", "BullsRelative"))), by = c("lien", "dateC")) %>% 
  filter(month %in% c('04', '05', '06'))

myData = elabData %>% group_by(lien) %>% summarise(meanWaterTemp = mean(Temp1), meanSurvival = mean(juvenileSurvival, na.rm = TRUE))


plot(elabData$Temp1, elabData$juvenileSurvival, pch = 20, col = "grey", xlim = c(10,30), ylim = c(0,1),
     xlab = 'water temperature', ylab = 'juvenile survival')
points(myData$meanWaterTemp, myData$meanSurvival)
lines(temperature, embryoSurvival * larvalSurvival)
       