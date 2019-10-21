library(dplyr)
library(tidyr)
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

# directly with  coeff
dailySurvivalEmbryoCoeff = function(temperature, modelSurvivalEmbryoCoeff, parDurationIncubation){
  sel = is.na(temperature)
  temperature[sel] = 0
  duration = (parDurationIncubation[1] * temperature ^ parDurationIncubation[2]) # remove the round() to smooth the curve
  predict_matrix = model.matrix(~ Temp + I(Temp ^ 2) + I(Temp ^ 3), data = data.frame(Temp = temperature))
  surv = (1 / (1 + exp(-predict_matrix %*% modelSurvivalEmbryoCoeff))) ^ (1 / duration)
  surv[sel] = NA
  return(surv)
}

cumulativeSurvivalEmbryoCoeff = function(temperature, modelSurvivalEmbryoCoeff){
  sel = is.na(temperature)
  temperature[sel] = 0
  predict_matrix = model.matrix(~ Temp + I(Temp ^ 2) + I(Temp ^ 3), data = data.frame(Temp = temperature))
  surv = (1 / (1 + exp(-predict_matrix %*% modelSurvivalEmbryoCoeff))) 
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

embryoSurvival =  cumulativeSurvivalEmbryoCoeff(temperature, modelSurvivalEmbryoCoeff)
larvalSurvival =  dailySurvivalLarvaeCoeff(temperature, modelSurvivalLarvaeCoeff)^horizon

plot(temperature, embryoSurvival, type = 'l', col = 'blue')
lines(temperature, larvalSurvival, col = 'green')
lines(temperature, embryoSurvival * larvalSurvival, col = 'red' )
legend("topright", legend = c("embryo", "larval", "cumulative"), col = c('blue', 'green', 'red'), lty = 1)

# duration of hatching
duration = (parDurationIncubation[1] * temperature ^ parDurationIncubation[2]) 
plot(temperature, duration, type = 'l')

# ===== comparison between  Jatteau and Rosso equation =================================
temperatureEffect = function(tempWater, TminRep, ToptRep, TmaxRep){
  response = (tempWater - TminRep) * (tempWater - TmaxRep) / ((tempWater - TminRep) * (tempWater - TmaxRep) - (tempWater - ToptRep)^2)
  response[tempWater <= TminRep | tempWater >= TmaxRep] = 0
  return(response)
}

juvenileTolerance = c(10.8, 20.8, 29.8) #tolerance temperature range Jatteau 2017
plot(temperature, embryoSurvival * larvalSurvival, type = 'l', col = 'red', ylim = c(0,1))
lines(temperature, temperatureEffect(temperature, juvenileTolerance[1], juvenileTolerance[2], juvenileTolerance[3] ))

cbind(temperature, embryoSurvival * larvalSurvival)

# new juvenile tolerance range at 1% =============================
c(9.7, 20.8, 30.7)
temperature[which.max(embryoSurvival * larvalSurvival)]
max(temperature[(embryoSurvival * larvalSurvival) <= (0.01 * max(embryoSurvival * larvalSurvival)) & temperature < temperature[which.max(embryoSurvival * larvalSurvival)] ])
min(temperature[(embryoSurvival * larvalSurvival) <= (0.01 * max(embryoSurvival * larvalSurvival)) & temperature > temperature[which.max(embryoSurvival * larvalSurvival)] ])

# ==== stock recruitment relationship accoring to temperature
thermalBevertonHolt = function(stock, temperature, juvenileTolerance, surface = 84810, aFecundity = 135000) {
  survOptRep = 0.0017
  delta_t = 0.33
  lambda = 4.1E-4
  #  a ~ fecondité
  tempEffect = temperatureEffect(temperature, juvenileTolerance[1], juvenileTolerance[2], juvenileTolerance[3] )
  b =  -log(survOptRep * tempEffect)/delta_t
  c = lambda/surface
  
  eta = 2.4
  theta = 1.9
  #theta = 1.15
  S95 = eta * surface
  S50 = S95 / theta
  
  AlleeEffect = 1 / (1 + exp(-log(19) * (stock - S50) / (S95 - S50))) 
  
  alpha = (b * exp(-b * delta_t)) / (c * (1 - exp(-b * delta_t)))
  alpha[tempEffect == 0] = 0
  beta = b / (aFecundity * c * (1 - exp(-b * delta_t)))
  
  recruit = alpha * stock * AlleeEffect / (beta + stock * AlleeEffect)
  return(recruit)
}



plot(temperature, (embryoSurvival * larvalSurvival) / (embryoSurvival * larvalSurvival)[temperature == juvenileTolerance[2]] , type = 'l', col = 'red', ylim = c(0,1), ylab = 'suvival / survival(Topt)' )
lines(temperature, temperatureEffect(temperature, juvenileTolerance[1], juvenileTolerance[2], juvenileTolerance[3] ), lty = 2)
lines(temperature, sapply(temperature,  function(temp) (thermalBevertonHolt(stock = 84810 * 3, temperature = temp, juvenileTolerance = juvenileTolerance, aFecundity = 135000 * 2))) /
        thermalBevertonHolt(stock = 84810 * 3, temperature = juvenileTolerance[2] , juvenileTolerance = juvenileTolerance, aFecundity = 135000 * 2))
legend('topleft', legend = c("Jatteau et al", "GR3D temp effect", "GR3D S-R rel."), lty = c(1,2,1), col = c("red", "black", "black"))

# ==== observed data ========================================================================
envGGD = read.csv("envCondition.csv", sep = ";")
repro = read.csv("reproMonitoring.csv", sep = ";")

envGGD = envGGD %>% mutate(date = as.Date(date)) %>% mutate(month = format(date, '%m')) %>%
  mutate(season = cut(as.numeric(format(date, '%m')), breaks = c(0, 3, 6, 9, 12), labels = c("winter", "spring", "summer" , "fall"))) %>%
  mutate(lien = paste(lien, season, sep = '_'))


repro = repro %>% mutate(date = as.Date(date)) %>% mutate(month = format(date, '%m')) %>%
  mutate(season = cut(as.numeric(format(date, '%m')), breaks = c(0, 3, 6, 9, 12), labels = c("winter", "spring", "summer" , "fall"))) %>%
  mutate(lien = paste(lien, season, sep = '_'))


selectedSeason = c("spring")
elabData =   envGGD %>% select(one_of(c("lien", "date", "season", "Temp1", "juvenileSurvival"))) %>%  left_join(select(repro, one_of(c("lien", "date", "Bulls", "BullsRelative"))), by = c("lien", "date")) %>% 
  filter(season %in% selectedSeason)

juvSurvival = elabData %>% group_by(lien) %>% summarise(meanWaterTemp = mean(Temp1), meanSurvival = mean(juvenileSurvival, na.rm = TRUE))
weightedJuvSurvivalSplash = elabData %>% mutate(Bulls = replace_na(Bulls, 0)) %>% group_by(lien) %>% summarise(meanWaterTemp = mean(Temp1), 
                                                                meanSurvival = weighted.mean(x = juvenileSurvival, w = Bulls, na.rm = TRUE))

plot(juvSurvival$meanWaterTemp, juvSurvival$meanSurvival, pch = 20, xlim = c(10,30), ylim = c(0,1), 
     xlab = ' water temperature', ylab = 'juvenile survival')
points(weightedJuvSurvivalSplash$meanWaterTemp, weightedJuvSurvivalSplash$meanSurvival, pch = 20, col = 'green')

lines(temperature, embryoSurvival * larvalSurvival, col = 'red')
lines(temperature, temperatureEffect(temperature, juvenileTolerance[1], juvenileTolerance[2], juvenileTolerance[3]), col = "blue")
legend("topleft", legend = c("uniform bull", "observed splash", "juvenile tolerance", "temperature effect"),
       pch = c(20,20,-1,-1), lty = c(0,0, 1,1), col = c('black', 'green', 'red', "blue"))
 
# run code to upload SAFRAN data
weightedJuvSurvival = weightedJuvSurvival %>% left_join(airData, by = 'lien')


# ==== water temperature versus air temperature
library(openxlsx)

# données EDF (mis à disposition par Elorri)
dataDordogne = read.xlsx( xlsxFile = 'TQ_Dordogne_Garonne.xlsx', sheet = "Dordogne_1997_2018", detectDates = TRUE) %>%
  select(one_of(c("date", "temperature")))  %>%
  mutate(season = cut(as.numeric(format(date, '%m')), breaks = c(0, 3,6,9,12), labels = c("winter", "spring", "summer" , "fall"))) %>%
  mutate(site = "Dordogne", lien = paste(site, format(date, "%Y"), season, sep = "_"))
dataGaronne = read.xlsx( xlsxFile = 'TQ_Dordogne_Garonne.xlsx', sheet = "Garonne_1976_2018", detectDates = TRUE) %>%
  rename(date = Date) %>% select(one_of(c("date", "temperature")))  %>%
  mutate(season = cut(as.numeric(format(date, '%m')), breaks = c(0, 3,6,9,12), labels = c("winter", "spring", "summer" , "fall"))) %>% 
  mutate(site = "Garonne", lien = paste(site, format(date, "%Y"), season, sep = "_"))

# données Safran (mises à disposition par Eric Sauquet)
safranDordogne = read.csv("GARDONNE_T.txt", sep = " ",  skip = 1, header = FALSE, col.names = c("date", "temperature")) %>% 
  mutate(date = as.Date(date), site = "Dordogne") %>%
  mutate(season = cut(as.numeric(format(date, '%m')), breaks = c(0, 3,6,9,12), labels = c("winter", "spring", "summer" , "fall"))) %>% 
  mutate(lien = paste(site, format(date, "%Y"), season, sep = "_"))
safranGaronne = read.csv("LAMAGIST_T.txt", sep = " ",  skip = 1, header = FALSE, col.names = c("date", "temperature")) %>% 
  mutate( date = as.Date(date), site = "Garonne") %>%
  mutate(season = cut(as.numeric(format(date, '%m')), breaks = c(0, 3,6,9,12), labels = c("winter", "spring", "summer" , "fall"))) %>% 
  mutate(lien = paste(site, format(date, "%Y"), season, sep = "_"))

# myData
# selectedSeason = c("spring", "winter","fall", "summer")
# selectedSeason = c("spring", "winter")
selectedSeason = c("spring")
dailyWaterTemperature = dataDordogne %>% bind_rows(dataGaronne) %>%  filter(season %in% selectedSeason)
waterData = dailyWaterTemperature %>% group_by(lien, season) %>% summarize(meanWaterTemp = mean(temperature))

dailyAirTemperature = safranDordogne  %>%  bind_rows(safranGaronne)  %>%  filter(season %in% selectedSeason)
airData =  dailyAirTemperature %>% group_by(lien, season) %>% summarize(meanAirTemp = mean(temperature))

myData = waterData %>% left_join(airData, by = c("lien", "season"))
myDailyData = dailyWaterTemperature %>% rename(waterTemperature = temperature) %>% left_join(dailyAirTemperature %>% rename(airTemperature = temperature), by = c("date"))

# seasonal average
plot(myData$meanWaterTemp, myData$meanAirTemp, xlab = "seasonal average WATER temperature", ylab = "seasonal average AIR temperature", pch = 20, 
     col = c("red", "blue", "yellow", "green")[as.factor(myData$season)])
legend("topleft", legend = c("winter",  "spring", "summer", "fall"), col = c("red", "blue", "yellow", "green"), lty = 1)
#text(myData$meanWaterTemp, myData$meanAirTemp, myData$lien, cex=.5, pos=2)
airWaterLm = lm(meanAirTemp ~ meanWaterTemp , data = myData) 
summary(airWaterLm)
abline(reg = airWaterLm, col = 'blue')
abline(coef = c(0,1), lty = 2)

rbind(spawnerTolerance, predict(airWaterLm, newdata = data.frame(meanWaterTemp = spawnerTolerance, season = "spring") ))
rbind(juvenileTolerance, predict(airWaterLm, newdata = data.frame(meanWaterTemp = juvenileTolerance, season = "spring") ))

# ==== daily values ==================================================================
# see Santiago, J. M., García de Jalón, D., Alonso, C., Solana, J., Ribalaygua, J., Pórtoles, J. & Monjo, R. (2016) Brown trout thermal niche 
#   and climate change: expected changes in the distribution of cold-water fish in central Spain. Ecohydrology 9, 514–528.
# Mohseni, O., Stefan, H. G. & Erickson, T. R. (1998) A nonlinear regression model for weekly stream temperatures. 
#    Water Resources Research 34, 2685–2692.


plot(myDailyData$waterTemperature, myDailyData$airTemperature, pch = 20, col = 'grey',
     xlab = "WATER temperature", ylab = "AIR temperature")
points(myData$meanWaterTemp, myData$meanAirTemp, pch = 20, col = 'blue')

legend("topleft", legend = c( "daily", "seasonal average"), col = c("grey", "blue"), pch = 20)
abline(coef = c(0,1), lty = 2)
daylyLm = lm(airTemperature ~ waterTemperature , data = myDailyData)
summary(daylyLm)

rbind(spawnerTolerance, predict(daylyLm, newdata = data.frame(waterTemperature = spawnerTolerance, season = "spring") ))
rbind(juvenileTolerance, predict(daylyLm, newdata = data.frame(waterTemperature = juvenileTolerance, season = "spring") ))
abline(reg = daylyLm, col = 'black')

modifiedMohseni = function(airTemperature, par){
  mu = par[1]     # the estimated minimum stream temperature
  alpha = par[2]  # the estimated maximum stream temperature
  beta = par[3]   # the air temperature at which the rate of change of the stream temperature with respect to the air temperature is at a maximum
  gamma = par[4]  # the value of the rate of change at β.
  lambda = par[5] # a coefﬁcient representing the resistance of DMST to change with respect to DMAT variation in one day (ΔTa).
  
  deltaAirTemperature = c(NA, diff(airTemperature))
  waterTemperature = mu + lambda * deltaAirTemperature + (alpha - mu) / (1 + exp(gamma * (beta - airTemperature)))
  
  return(waterTemperature)
}

SSE = function(par, obsAirTemperature, obsWaterTemperature){
    return(sum((modifiedMohseni(obsAirTemperature, par) - obsWaterTemperature)^2, na.rm = TRUE))
}

daylyNLM = nlm(f = SSE, p = c(5, 30, 15, .2, .1), obsAirTemperature = myDailyData$airTemperature, obsWaterTemperature = myDailyData$waterTemperature )

daylyLM = lm(waterTemperature ~ airTemperature, data = myDailyData)

hist(modifiedMohseni( myDailyData$airTemperature, par = daylyNLM$estimate ) - myDailyData$waterTemperature, breaks =  50)
hist(predict.lm(daylyLM, newdata = myDailyData) - myDailyData$waterTemperature, breaks =  50)

summary(lm(modifiedMohseni( myDailyData$airTemperature, par = daylyNLM$estimate ) ~ myDailyData$waterTemperature))
summary(daylyLM)

plot(myDailyData$airTemperature, myDailyData$waterTemperature, pch = 20, col = 'grey',
     xlab = "AIR temperature", ylab = "WATER temperature")

# ===== survival according to air or water temperature ==============================
plot(weightedJuvSurvival$meanWaterTemp, weightedJuvSurvival$meanSurvival, pch = 20, xlim = c(10,30), ylim = c(0,1), 
     xlab = 'temperature', ylab = 'juvenile survival', col = 'green')
points(weightedJuvSurvival$meanAirTemp, weightedJuvSurvival$meanSurvival, pch = 20, col = 'darkgreen')
lines(temperature, embryoSurvival * larvalSurvival, col = 'red')
lines(temperature, temperatureEffect(temperature, juvenileTolerance[1], juvenileTolerance[2], juvenileTolerance[3]), col = "blue")
legend("topright", legend = c("water temperature", "air temperature", "juvenile tolerance", "temperature effect"), 
       col = c("green", "darkgreen", 'red', 'blue'), pch = c(20, 20, -1,-1),  lty = c(0,0,1,1),, cex = .8)

plot(juvSurvival$meanWaterTemp, juvSurvival$meanSurvival, pch = 20, xlim = c(10,30), ylim = c(0,1), 
     xlab = ' water temperature', ylab = 'juvenile survival')
points(weightedJuvSurvival$meanWaterTemp, weightedJuvSurvival$meanSurvival, pch = 20, col = 'green')

lines(temperature, embryoSurvival * larvalSurvival, col = 'red')
lines(temperature, temperatureEffect(temperature, juvenileTolerance[1], juvenileTolerance[2], juvenileTolerance[3]), col = "blue")
legend("topleft", legend = c("uniform bull", "observed splash", "juvenile tolerance", "temperature effect"),
       pch = c(20,20,-1,-1), lty = c(0,0, 1,1), col = c('black', 'green', 'red', "blue"))
