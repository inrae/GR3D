library(openxlsx)
library(dplyr)
library(tidyr)
library(lubridate)

library(insol)

rm(list = ls())

# ===== probability to spawn from BRT Paumier et al
library(gbm)
load("BTRarticle.RData")


# ===== juvenile survival from Jatteau et al 2017 ===============================================================================
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

computeJuvenileSurvival = function(data, parDurationIncubation, modelSurvivalEmbryoCoeff, modelSurvivalLarvaeCoeff, horizon) {
  
  data = data %>% select(one_of(c("date", "temperature"))) %>%
    mutate(temperatureEmbryo = temperature ^ -parDurationIncubation[2]) %>%
    mutate(dailyEmbryoSurvival = dailySurvivalEmbryoCoeff(temperature, modelSurvivalEmbryoCoeff, parDurationIncubation) ) %>%
    mutate(dailyLarvalSurvival = dailySurvivalLarvaeCoeff(temperature, modelSurvivalLarvaeCoeff)) %>%
    mutate(durationIncubation = NA, juvenileSurvival = NA)
  
  for (t in (1:(nrow(data) - (horizon + 20)))) {
    # compute duration of incubation
    if (!is.na(data$temperatureEmbryo[t])) {
      i = 0
      while (sum(data$temperatureEmbryo[t:(t + i)]) < parDurationIncubation[1]) {
        i = i + 1
        if (is.na(sum(data$temperatureEmbryo[t:(t + i)]))) {
          i = NA
          break()
        }
      }
    } 
    durationIncubation = i + 1
    data$durationIncubation[t] = durationIncubation
    if (!is.na(durationIncubation)) {
      # cumulative survival of embryo
      embryoSurvival = prod(data$dailyEmbryoSurvival[t:(t + durationIncubation - 1)])
      
      # cumulative survival of larval until horizon days of hatch
      larvalSurvival = prod(data$dailyLarvalSurvival[(t + durationIncubation):(t + durationIncubation + horizon - 1)])
      
      # cumulative juvenile survival
      data$juvenileSurvival[t] = embryoSurvival * larvalSurvival
    }
  }
  return(data)
}

parDurationIncubation = c(1124.531453,  -1.834438)
modelSurvivalEmbryoCoeff = c(-1.245015E+01,  1.120331E+00 , -7.830614E-03, -6.112982E-04)
modelSurvivalLarvaeCoeff = c(-1.851093E+00,  5.132855E-01, -1.020347E-02, -7.005887E-05 )
horizon = 14 


# ==== Air temperature ===================================================================================================
# données Safran (mises à disposition par Eric Sauquet)
safranDordogne = read.csv("GARDONNE_T.txt", sep = " ",  skip = 1, header = FALSE, col.names = c("date", "temperature")) %>% 
  mutate(date = as.Date(date), site = "Dordogne") %>%
  mutate(season = cut(as.numeric(format(date, '%m')), breaks = c(0, 3,6,9,12), labels = c("winter", "spring", "summer" , "fall"))) %>% 
  mutate(lien = paste(site, format(date, "%Y"), season, sep = "_"))
safranGaronne = read.csv("LAMAGIST_T.txt", sep = " ",  skip = 1, header = FALSE, col.names = c("date", "temperature")) %>% 
  mutate( date = as.Date(date), site = "Garonne") %>%
  mutate(season = cut(as.numeric(format(date, '%m')), breaks = c(0, 3,6,9,12), labels = c("winter", "spring", "summer" , "fall"))) %>% 
  mutate(lien = paste(site, format(date, "%Y"), season, sep = "_"))

# ==== water temperature + probability to spawn from Paumier et al =======================================================
# données EDF (mis à disposition par Elorri)
dataDordogne = read.xlsx( xlsxFile = 'TQ_Dordogne_Garonne.xlsx', sheet = "Dordogne_1997_2018", detectDates = TRUE) %>%
  select(one_of(c("date", "temperature", "debit" )))  %>%
  mutate(season = cut(as.numeric(format(date, '%m')), breaks = c(0, 3,6,9,12), labels = c("winter", "spring", "summer" , "fall"))) %>%
  mutate(site = "Dordogne", lien = paste(site, format(date, "%Y"), season, sep = "_")) %>%
  mutate(dayOfYear = yday(date), DL = daylength(lat = 44.8452, long = 0.4065, jd = dayOfYear, tmz = 1)[,3]) %>%
  mutate(Temp1 = temperature, Q1 = log(debit)) %>%
  mutate(deltaT1 = c(NA,diff(Temp1)) , deltaQ1 = c(NA,diff(Q1)), deltaDL1 = c(NA, diff(DL)))
dataDordogne$pReproduce = predict.gbm(finalBrtPres, dataDordogne, n.trees = finalBrtPres$gbm.call$best.trees, type="response")
dataDordogne$pReproduce[1] = NA
dataDordogne$juvenileSurvival = computeJuvenileSurvival(dataDordogne, parDurationIncubation, modelSurvivalEmbryoCoeff, modelSurvivalLarvaeCoeff, horizon)$juvenileSurvival
head(dataDordogne,5)

dataGaronne = read.xlsx( xlsxFile = 'TQ_Dordogne_Garonne.xlsx', sheet = "Garonne_1976_2018", detectDates = TRUE) %>%
  rename(date = Date) %>% select(one_of(c("date", "temperature", "debit")))  %>%
  mutate(season = cut(as.numeric(format(date, '%m')), breaks = c(0, 3,6,9,12), labels = c("winter", "spring", "summer" , "fall"))) %>% 
  mutate(site = "Garonne", lien = paste(site, format(date, "%Y"), season, sep = "_")) %>%
  mutate(dayOfYear = yday(date), DL = daylength(lat = 44.1167, long = 0.8333, jd = dayOfYear, tmz = 1)[,3]) %>%
  mutate(Temp1 = temperature, Q1 = log(debit)) %>%
  mutate(deltaT1 = c(NA,diff(Temp1)) , deltaQ1 = c(NA,diff(Q1)), deltaDL1 = c(NA, diff(DL)))
dataGaronne$pReproduce = predict.gbm(finalBrtPres, dataGaronne, n.trees = finalBrtPres$gbm.call$best.trees, type="response")
dataGaronne$pReproduce[1] = NA
dataGaronne$juvenileSurvival = computeJuvenileSurvival(dataGaronne, parDurationIncubation, modelSurvivalEmbryoCoeff, modelSurvivalLarvaeCoeff, horizon)$juvenileSurvival

head(dataGaronne)

rm(finalBrtPres)


# combine data =========================================================================================================

selectedSeason = c("spring", "winter","fall", "summer")
# selectedSeason = c("spring", "winter")
# selectedSeason = c("spring")
dailyWaterTemperature = dataDordogne %>% bind_rows(dataGaronne) %>%  filter(season %in% selectedSeason) %>% rename(waterTemperature = temperature)
waterData = dailyWaterTemperature %>% group_by(lien, season) %>% summarize(meanWaterTemp = mean(waterTemperature)) 

head(waterData)
dailyAirTemperature = safranDordogne  %>%  bind_rows(safranGaronne)  %>%  filter(season %in% selectedSeason) %>% rename(airTemperature = temperature)
airData =  dailyAirTemperature %>% group_by(lien, season) %>% summarize(meanAirTemp = mean(airTemperature))

myData = waterData %>% left_join(airData, by = c("lien", "season"))
myDailyData = dailyWaterTemperature  %>% left_join(dailyAirTemperature %>% select(one_of(c("date", "airTemperature", "site"))), by = c("date", "site"))

head(myDailyData)

weightedJuvSurvival = myDailyData %>% mutate(pReproduce = replace_na(pReproduce, 0))  %>% group_by(lien, season) %>% summarise(meanWaterTemp = mean(waterTemperature), meanAirTemp = mean(airTemperature), 
                                                                    meanSurvival = weighted.mean(x = juvenileSurvival, w = pReproduce, na.rm = TRUE))
head(weightedJuvSurvival)

temperature = seq(5,30,.1)
plot(weightedJuvSurvival$meanAirTemp, weightedJuvSurvival$meanSurvival, pch=20, xlim=range(temperature), ylim=c(0,1),
     col = c("red", "blue", "yellow", "green")[as.factor(weightedJuvSurvival$season)])
legend("topleft", legend = c("winter",  "spring", "summer", "fall"), col = c("red", "blue", "yellow", "green"), lty = -1, pch = 20)


embryoSurvival =  cumulativeSurvivalEmbryoCoeff(temperature, modelSurvivalEmbryoCoeff)
larvalSurvival =  dailySurvivalLarvaeCoeff(temperature, modelSurvivalLarvaeCoeff)^horizon
points(weightedJuvSurvivalSplash$meanWaterTemp, weightedJuvSurvivalSplash$meanSurvival, pch = 20, col = 'darkblue')

lines(temperature, embryoSurvival * larvalSurvival, col = 'red')
lines(temperature, temperatureEffect(temperature, juvenileTolerance[1], juvenileTolerance[2], juvenileTolerance[3]) * max(embryoSurvival * larvalSurvival), col = "blue")
#legend("topleft", legend = c("uniform bull", "observed splash", "juvenile tolerance", "temperature effect"),
#       pch = c(20,20,-1,-1), lty = c(0,0, 1,1), col = c('black', 'green', 'red', "blue"))

  
# with fake day  of the year ====================================
myDailyDataFake = myDailyData %>% mutate(fakeDayOfYear = case_when(season == "winter" ~ dayOfYear + 90,
                                                 season == "spring" ~ dayOfYear,
                                                 season == "summer" ~ dayOfYear - 91,
                                                 season == " winter" ~ dayOfYear - 183)) %>%
  mutate(DL = case_when(site == "Garonne" ~ daylength(lat = 44.1167, long = 0.8333, jd = fakeDayOfYear, tmz = 1)[,3],
                         site == "Dordogne" ~ daylength(lat = 44.8452, long = 0.4065, jd = fakeDayOfYear, tmz = 1)[,3])) %>%
  mutate(deltaDL1 = c(NA, diff(DL)))
myDailyDataFake$pReproduceFake = predict.gbm(finalBrtPres, myDailyDataFake, n.trees = finalBrtPres$gbm.call$best.trees, type="response")
myDailyDataFake$pReproduceFake[1] = NA
head(myDailyDataFake)



weightedJuvSurvivalFake = myDailyDataFake %>% mutate(pReproduceFake = replace_na(pReproduceFake, 0))  %>% group_by(lien, season) %>% 
  summarise(meanWaterTemp = mean(waterTemperature), meanAirTemp = mean(airTemperature), 
            meanSurvival = weighted.mean(x = juvenileSurvival, w = pReproduceFake))


plot(weightedJuvSurvivalFake$meanAirTemp, weightedJuvSurvivalFake$meanSurvival, pch = 20, xlim = range(temperature), ylim = c(0,1),
     col = c("red", "blue", "yellow", "green")[as.factor(weightedJuvSurvivalFake$season)])
legend("topleft", legend = c("winter",  "spring", "summer", "fall"), col = c("red", "blue", "yellow", "green"), lty = -1, pch = 20)


weightedJuvSurvivalFake %>% filter(season == "summer" & meanSurvival <.45)

myDailyDataFake %>% filter(between(date, as.Date("2003-06-25"), as.Date("2003-07-10")), site == 'Dordogne' )

# calibration ============================================================================================
objFun = function(par, observationData) {
  SE = (observationData$meanSurvival  - par[4] * Rosso2(observationData$meanAirTemp, par))^2
  return(sum(SE, na.rm = TRUE))
}

objFunLehman = function(par, observationData) {
  SE = (observationData$meanSurvival  - par[4] * modifiedLehman(observationData$meanAirTemp, par))^2
  return(sum(SE, na.rm = TRUE))
}

data = weightedJuvSurvivalFake %>% drop_na()
resRosso2 = nlm(objFun, p = c(10,17,25,.6), observationData = data)
resLehman = nlm(objFunLehman, p = c(10,17,25,.6), observationData = data)

print(resRosso2)
plot(data$meanAirTemp, data$meanSurvival, pch=20, xlim = range(temperature), ylim=c(0,1),
     col = c("red", "blue", "yellow", "green")[as.factor(data$season)])
legend("topleft", legend = c("winter",  "spring", "summer", "fall"), col = c("red", "blue", "yellow", "green"), lty = -1, pch = 20)
# lines(temperature, Rosso2(temperature, juvenileTolerance) * max(embryoSurvival * larvalSurvival), col = "blue", lty =2)
lines(temperature, Rosso2(temperature, resRosso2$estimate[1:3]) * resRosso2$estimate[4], col = "blue") 
lines(temperature, modifiedLehman(temperature, resLehman$estimate[1:3]) * resLehman$estimate[4], col ='cyan') 

