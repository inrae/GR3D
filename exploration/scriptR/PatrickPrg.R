# see BruchPrg for upload of data


# ========================================================================
# effect tempertaure sur la reproduction
# ========================================================================

thermalBevertonHolt = function (stock, temperature, juvenileTolerance) {
  survOptRep = 0.0017
  surface =84810
  delta_t = 0.33
  lambda = 4.1E-4
  a=135000 # ~fecondité
  tempEffect = temperatureEffect(temperature, juvenileTolerance[1], juvenileTolerance[2], juvenileTolerance[3] )
  b =  -log(survOptRep * tempEffect)/delta_t
  c = lambda/surface
  
  eta = 2.4
  theta =1.9
  #theta = 1.15
  S95 = eta * surface
  S50 = S95 / theta
  
  AlleeEffect = 1 / (1+ exp(-log(19) * (S - S50) / (S95 - S50))) 
  
  alpha = (b * exp(-b * delta_t)) / (c * (1 - exp(-b * delta_t)))
  alpha[tempEffect == 0] = 0
  beta = b /(a * c * (1-exp(-b * delta_t)))
  
  recruit = alpha * stock * AlleeEffect / (beta + stock * AlleeEffect)
  return (recruit)
}

thermalBevertonHoltWithoutAllee = function (stock, temperature, juvenileTolerance) {
  survOptRep = 0.0017
  surface =84810
  delta_t = 0.33
  lambda = 4.1E-4
  a=135000 # ~fecondité
  tempEffect = temperatureEffect(temperature, juvenileTolerance[1], juvenileTolerance[2], juvenileTolerance[3] )
  b =  -log(survOptRep * tempEffect)/delta_t
  c = lambda/surface
  
  
  alpha = (b * exp(-b * delta_t)) / (c * (1 - exp(-b * delta_t)))
  alpha[tempEffect == 0] = 0
  beta = b /(a * c * (1-exp(-b * delta_t)))
  
  recruit = alpha * stock  / (beta + stock )
  return (recruit)
}


modifiedBevertonHolt = function(stock){
  alpha = 6.4e6
  beta = .172e6
  d=19.2
  
  return  (alpha * (S^d) / ((beta^d) + (S^d)))
}

temperature =18

juvenileTolerance = c(9.8, 20, 26)
spawnerTolerance = c(10, 20, 23)
Z=0.4

S=seq(0, 1e6, 100)
plot(S, thermalBevertonHolt(S, Tref[2], juvenileTolerance), type ='l', ylab ='R', ylim = c(0, 8e6) )
lines(S, thermalBevertonHolt(S, Tref[2]+2, juvenileTolerance), col='red')
lines(S, thermalBevertonHolt(S, Tref[2]-2, juvenileTolerance), col='blue')


Stot=seq(0, 0.3e6, 100)*2
plot(Stot, thermalBevertonHolt(Stot/2, juvenileTolerance[2], juvenileTolerance), type ='l', ylab ='R', ylim = c(0, 8e6) )
lines(Stot, thermalBevertonHoltWithoutAllee(Stot/2, juvenileTolerance[2], juvenileTolerance), col ='blue')
## lines(Stot, thermalBevertonHolt(Stot, juvenileTolerance[2], juvenileTolerance), lty=2 )
lines(Stot, modifiedBevertonHolt(Stot), col = 'green')



thermBH_without = sapply(temperature, function(temp) (thermalBevertonHolt(stock = 4e5, temp)))
plot(temperature, thermBH_without, type='line,', ylab ='recruit')

thermBH_with = sapply(temperature, function(temp) (thermalBevertonHolt(stock = 4e5 *  
                                                                         temperatureEffect(temp, spawnerTolerance[1], spawnerTolerance[2], spawnerTolerance[3]), temp, juvenileTolerance)))
lines(temperature, thermBH_with, col='red')


recruitProduction = function(temperature, juvenileTolerance, spawnerTolerance, Z, stock) {
  survivingStock = stock * temperatureEffect(temperature, spawnerTolerance[1], spawnerTolerance[2], spawnerTolerance[3])
  recruit = sapply(1:length(temperature), function(i) (thermalBevertonHolt(survivingStock[i], temperature[i], juvenileTolerance)))
  survivingRecruit =  recruit * exp(-Z*5)
  return(survivingRecruit)
}


fct = function(temperature, juvenileTolerance, spawnerTolerance, Z, stock){
  return( (recruitProduction(temperature, juvenileTolerance, spawnerTolerance, Z, stock) -stock)^2)
}


stock =4e5
temperature =seq(0,30,.1)
plot(temperature, recruitProduction(temperature, juvenileTolerance, spawnerTolerance, Z, stock), type='l')
plot(temperature, fct(temperature, juvenileTolerance, spawnerTolerance, Z, stock), type='l')


optimise(fct, interval = c(5, 20),  stock=stock, Z=Z, juvenileTolerance = c(9, 18, 26), spawnerTolerance = c(9, 18, 26) )$minimum

plot(temperature, thermBH_with * exp(-Z*5), ylab = 'surviving spawner' , type = 'line')


abline(h=4e5)
#lines(temperature, thermBH_with * exp(-(Z/2)*5), col='green')
cbind(temperature, thermBH_with *exp(-Z*5) -4e5)


# temperature effect on spawner survival 
plot(temperature, temperatureEffect(temperature, 10, 20, 23), type='l', type = 'line')


# temperature effect on recruit survival 
lines(temperature, temperatureEffect(temperature, 9.75, 20, 26), col='red')


lines(temperature, temperatureEffect(temperature, 9.75, 20, 26) * temperatureEffect(temperature, 10, 20, 23), type='l', col='green')
lines(temperature, temperatureEffect(temperature, 9.75, 20, 26) * temperatureEffect(temperature, 10, 20, 23) * exp(-.4*5), type='l', col='blue')