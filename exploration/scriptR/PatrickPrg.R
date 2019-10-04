# see BruchPrg for upload of data

basins = read.csv("/home/patrick/Documents/workspace/GR3D/data/input/reality/basins.csv", sep = ";")
tempData = read.csv("/home/patrick/Documents/workspace/GR3D/data/input/reality/SeasonTempBVFacAtlant1801_2100_newCRU_RCP85.csv", sep = ";")

riverName = "Garonne"
riverName = "Rhine"
riverName = "Meuse"

surfaceBasin = basins[basins$nomBV == riverName, 'surface']

sel = tempData$NOM == riverName & tempData$Year >= 1901 & tempData$Year <= 1910
(TrefInRiver = as.data.frame(t(colMeans(tempData[sel, c("Winter", "Spring", "summer", "Autumn")]))))


# =============================== temp en fonction de la latitude ====
library(dplyr)

extractTemp <- as.data.frame(tempData %>%
                               filter(Year > 1991 & Year <= 2009) %>%
                               group_by(NOM,Lat) %>%
                               summarize(meanSpring = mean(Spring)) 
                             %>% arrange(Lat)
)

plot(extractTemp$Lat, extractTemp$meanSpring, pch = 20)
abline(v = 45.25) # Garonne
abline(v = 49.25) # vire
abline(v = 52.25) # Rhine

extractTemp %>% filter(NOM == riverName)

abline(h = 11)
abline(h = 12.5)

levels(extractTemp$NOM)


# ==== effect temperature sur la reproduction ====================================


temperatureEffect = function(tempWater, TminRep, ToptRep, TmaxRep){
  #  if (tempWater<=TminRep | tempWater >= TmaxRep)
  #    return(0)
  #  else
  response = (tempWater - TminRep) * (tempWater - TmaxRep) / ((tempWater - TminRep) * (tempWater - TmaxRep) - (tempWater - ToptRep)^2)
  
  response[tempWater <= TminRep | tempWater >= TmaxRep] = 0
  return(response)
  
}

thermalBevertonHolt = function(stock, temperature, juvenileTolerance, surface = 84810) {
  survOptRep = 0.0017
  delta_t = 0.33
  lambda = 4.1E-4
  a = 135000 # ~fecondité
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
  beta = b / (a * c * (1 - exp(-b * delta_t)))
  
  recruit = alpha * stock * AlleeEffect / (beta + stock * AlleeEffect)
  return(recruit)
}

stockAtCrash = function(temperature, juvenileTolerance, surface = 84810) {
  survOptRep = 0.0017
  delta_t = 0.33
  lambda = 4.1E-4
  a = 135000 # ~fecondité
  tempEffect = temperatureEffect(temperature, juvenileTolerance[1], juvenileTolerance[2], juvenileTolerance[3])
  b =  -log(survOptRep * tempEffect) / delta_t
  b[b == Inf] = 0
  c = lambda / surface
  
  eta = 2.4
  theta = 1.9
  #theta = 1.15
  S95 = eta * surface
  S50 = S95 / theta
  
  beta = b / (a * c * (1 - exp(-b * delta_t)))
  
  res = S50 + (S95 - S50) * log(beta * log(19) / (S95 - S50)) / log(19)
  res[is.nan(res)] = 0
  return(res)
}


thermalBevertonHoltWithoutAllee = function(stock, temperature, juvenileTolerance, surface = 84810)  {
  survOptRep = 0.0017
  delta_t = 0.33
  lambda = 4.1E-4
  a = 135000 # ~fecondité
  tempEffect = temperatureEffect(temperature, juvenileTolerance[1], juvenileTolerance[2], juvenileTolerance[3] )
  b =  -log(survOptRep * tempEffect)/delta_t
  c = lambda/surface
  
  alpha = (b * exp(-b * delta_t)) / (c * (1 - exp(-b * delta_t)))
  alpha[tempEffect == 0] = 0
  beta = b / (a * c * (1 - exp(-b * delta_t)))
  
  recruit = alpha * stock  / (beta + stock )
  return(recruit)
}


modifiedBevertonHolt = function(stock){
  alpha = 6.4e6 # for the Garonne River !!!
  beta = .172e6
  d = 19.2
  
  return(alpha * (stock^d) / ((beta^d) + (stock^d)))
}

survivingRecruit = function(stock, temperature, juvenileTolerance, spawnerTolerance, sigmaZ, surface){
  survStock = stock * temperatureEffect(temperature, spawnerTolerance[1], spawnerTolerance[2], spawnerTolerance[3])
  recruit = thermalBevertonHolt(survStock, temperature, juvenileTolerance, surface)
  survRecruit =  recruit * exp(-sigmaZ)
  return(survRecruit)
}



spawnerTolerance = c(10, 20, 23)
juvenileTolerance = c(9.8, 20, 26)


spawnerTolerance = c(10.7, 17, 25.7) #  spawning temperature range Paumier et al 2019
juvenileTolerance = c(10.8, 20.8, 29.8) #tolerance temperature range Jatteau 2017

Tmin = 8
spawnerTolerance = c(Tmin, 20, 23) 
juvenileTolerance = c(Tmin, 16, 23) 

sigmaZ = 0.4 * 5 # Z * lifespan of female
propFemale = 0.5

# ================================ stock-recruitment relationship at 1900-1910 SPRING temperature ====

ymax = 1e7
S = seq(0, 1e6, 100)
# with spawner mortality according to temperature
survS = S * temperatureEffect(TrefInRiver$Spring, spawnerTolerance[1], spawnerTolerance[2], spawnerTolerance[3])
plot(S, thermalBevertonHolt(survS, TrefInRiver$Spring, juvenileTolerance, surface = surfaceBasin), type = 'l',
     xlab = 'Stock (female)', ylab = 'R (female and male)', ylim = c(0, ymax) )
lines(S, thermalBevertonHolt(survS, TrefInRiver$Spring + 2, juvenileTolerance, surface = surfaceBasin), col = 'red')
lines(S, thermalBevertonHolt(survS, TrefInRiver$Spring - 2, juvenileTolerance, surface = surfaceBasin), col = 'blue')
# replacement line with prop of female in recruit
lines(c(0, ymax * exp(-sigmaZ) * propFemale), c(0, ymax), col = 'green', lty = 2)


# =============================== comparison with Rougier et al stock-recruitment relationship for the Garonne River ======

tempe = TrefInRiver$Spring
tempe = as.numeric(tempData %>%
  filter(Year > 1991 & Year <= 2009 & NOM == riverName) %>%
  summarize(meanSpring = mean(Spring), meanSummer = mean(summer)) )
tempe = juvenileTolerance[2]
tempe = 20.2 # Rougier 2014

Stot = seq(0, 2e6, 1e4)
ymax = 1e7

survS = Stot * temperatureEffect(tempe, spawnerTolerance[1], spawnerTolerance[2], spawnerTolerance[3])
plot(Stot, thermalBevertonHolt(stock = survS * propFemale, temperature = tempe, juvenileTolerance = juvenileTolerance, 
                               surface = surfaceBasin),
     type = 'l', xlab = 'Total stock (2 x female)', ylab = 'R (female and male)', ylim = c(0, ymax) )
# lines(Stot, thermalBevertonHolt(stock = survS, temperature = tempe, juvenileTolerance = juvenileTolerance, surface = surfaceBasin), lty = 3)
lines(Stot, thermalBevertonHoltWithoutAllee(Stot * propFemale, tempe, juvenileTolerance,  surface = surfaceBasin), col = 'blue')
## lines(Stot, thermalBevertonHolt(Stot, tempe, juvenileTolerance), lty=2 )
lines(Stot, modifiedBevertonHolt(Stot), col = 'green') # for the Garonne River
# replacement line
lines(c(0, ymax * exp(-sigmaZ)), c(0, ymax), col = 'green', lty = 2)

# ===================================== stock at equilibrium =============================================================
stocksAtEquilibrium = function(temperature, juvenileTolerance, spawnerTolerance, sigmaZ, surface, propFemale = 0.5) {

   # !!!!! stock and recruit consider only females !!!!!!
  objFct = function(stock, temperature, juvenileTolerance, spawnerTolerance, sigmaZ, surface){
    survRecruit = survivingRecruit(stock, temperature, juvenileTolerance, spawnerTolerance, sigmaZ, surface) * propFemale 
    return((survRecruit - stock)^2)
  }
  
  stockatequilibrium = 0
  stockattrap = 0
  
  # stock at crash before the spawner mortality
  tempEffSpawner =  temperatureEffect(temperature, spawnerTolerance[1], spawnerTolerance[2], spawnerTolerance[3])
  stockatcrash = ifelse(tempEffSpawner > 0, stockAtCrash(temperature, juvenileTolerance, surface) / tempEffSpawner, 0)
  
  
  if (stockatcrash > 0) {
    recruitatcrash = thermalBevertonHolt(stockatcrash * tempEffSpawner,
                                         temperature, juvenileTolerance, surface) * propFemale 
    sigmaZatcrash = -log(stockatcrash / recruitatcrash)
    
    if (sigmaZ < sigmaZatcrash) {
      stockattrap =  optimise(objFct, c(0,  stockatcrash), temperature = temperature, juvenileTolerance = juvenileTolerance, spawnerTolerance = spawnerTolerance, 
                              sigmaZ = sigmaZ, surface = surface)$minimum
      
      stockatequilibrium = optimise(objFct, (c(1, 100) * stockatcrash), temperature = temperature, juvenileTolerance = juvenileTolerance, spawnerTolerance = spawnerTolerance, 
                                      sigmaZ = sigmaZ, surface = surface)$minimum
    }
  }
  
  return(c(stockatequilibrium, stockattrap, stockatcrash))
}

temperature = seq(5,30,.1)

stocks = as.data.frame(t(sapply(temperature, function(temp) (stocksAtEquilibrium(temp, juvenileTolerance, spawnerTolerance, sigmaZ, surface = surfaceBasin)))))
names(stocks) = c('atEquilibrium', 'atTrap', 'atCrash')
stocks$temperature = temperature

sel = TRUE
plot(stocks$temperature[sel], stocks$atEquilibrium[sel], type = 'l', xlab = 'T (°C)', ylab = 'Stock (# female)' , main = riverName)
lines(stocks$temperature[sel], stocks$atTrap[sel], col = 'red')
legend('topleft', legend = c('at equilibrium', 'at trap'), col = c('black', 'red'), lty = 1)
max(stocks$temperature[stocks$atEquilibrium == 0 & stocks$temperature < 20])

# verif
tempe = 14.4
stocks[stocks$temperature == tempe, ]



S = seq(0, 5e5, 1e3) # only females

recruit = thermalBevertonHolt(S * temperatureEffect(tempe, spawnerTolerance[1], spawnerTolerance[2], spawnerTolerance[3]),
                              temperature = tempe, juvenileTolerance = juvenileTolerance, surface = surfaceBasin) * propFemale

plot(S, recruit, type = 'l', xlab = 'Stock (# female)', ylab = 'Recruit (# of female)', main = paste(riverName, surfaceBasin))
lines(c(0, max(recruit) * exp(-sigmaZ)), c(0, max(recruit)), col = 'green', lty = 2)
sAtEqui = stocks$atEquilibrium[stocks$temperature == tempe]
sAtCrash = stocks$atCrash[stocks$temperature == tempe]
sAtTrap = stocks$atTrap[stocks$temperature == tempe]
points(sAtEqui, thermalBevertonHolt(sAtEqui * temperatureEffect(tempe, spawnerTolerance[1], spawnerTolerance[2], spawnerTolerance[3]),
                                    temperature = tempe, juvenileTolerance = juvenileTolerance, surface = surfaceBasin) * propFemale,
       pch = 20) 
points(sAtTrap, thermalBevertonHolt(sAtTrap * temperatureEffect(tempe, spawnerTolerance[1], spawnerTolerance[2], spawnerTolerance[3]),
                                    temperature = tempe, juvenileTolerance = juvenileTolerance, surface = surfaceBasin) * propFemale, 
       col = 'red', pch = 20 )
points(sAtCrash, thermalBevertonHolt(sAtCrash * temperatureEffect(tempe, spawnerTolerance[1], spawnerTolerance[2], spawnerTolerance[3]),
                                    temperature = tempe, juvenileTolerance = juvenileTolerance, surface = surfaceBasin) * propFemale)


# ==== recruitment according to temperature ===========================
# but need to fix a reference stock !
refStock = 1.5e6 / 2 # female !!!
temperature = seq(5,30,.1)

thermBH_with = sapply(temperature, function(temp) (thermalBevertonHolt(stock = refStock *  
                                                                         temperatureEffect(temp, spawnerTolerance[1], spawnerTolerance[2], spawnerTolerance[3]), temp, juvenileTolerance)))
plot(temperature, thermBH_with, type = "l", ylab = 'recruit')

# verify impact of Allee effect 
thermBH_without = sapply(temperature, function(temp) (thermalBevertonHolt(stock = refStock, temp, juvenileTolerance)))
lines(temperature, thermBH_without, col = 'red')


# ===== with a fixed reference stock
recruitProduction = function(temperature, juvenileTolerance, spawnerTolerance, sigmaZ, refStock) {
  survivingStock = refStock * temperatureEffect(temperature, spawnerTolerance[1], spawnerTolerance[2], spawnerTolerance[3])
  recruit = sapply(1:length(temperature), function(i) (thermalBevertonHolt(survivingStock[i], temperature[i], juvenileTolerance)))
  survivingRecruit =  recruit * exp(-sigmaZ) # surviving spawners
  return(survivingRecruit)
}

refStock = 2e5 
temperature = seq(5,30,.1)
plot(temperature, recruitProduction(temperature, juvenileTolerance, spawnerTolerance, sigmaZ, refStock = refStock), type = 'l',
     ylab = "survival spawner")
abline(h = refStock)

fct = function(temperature, juvenileTolerance, spawnerTolerance, sigmaZ, refStock){
   return( (recruitProduction(temperature, juvenileTolerance, spawnerTolerance, sigmaZ, refStock) - refStock)^2)
 }
 optimise(fct, interval = c(5, 20),  refStock=refStock, sigmaZ=sigmaZ, juvenileTolerance , spawnerTolerance )$minimum


# ===================
# OLD
# ==================
# temperature effect on spawner survival 
plot(temperature, temperatureEffect(temperature, 10, 20, 23), type='l', type = 'line')


# temperature effect on recruit survival 
lines(temperature, temperatureEffect(temperature, 9.75, 20, 26), col='red')


lines(temperature, temperatureEffect(temperature, 9.75, 20, 26) * temperatureEffect(temperature, 10, 20, 23), type='l', col='green')
lines(temperature, temperatureEffect(temperature, 9.75, 20, 26) * temperatureEffect(temperature, 10, 20, 23) * exp(-.4*5), type='l', col='blue')