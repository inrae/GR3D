library(openxlsx)
# ============================================================

dataBruch = read.xlsx("/home/patrick/Documents/AA/CC et migrateur/thèse Poulet/BDalosesBruch.xlsx")
dataBruch$`M.gonades.(g)` = as.numeric(dataBruch$`M.gonades.(g)`)

head(dataBruch)


tapply(dataBruch$`Lf.(cm)`, dataBruch[,c('Année', 'Sexe')],min, na.rm = TRUE)

tapply(dataBruch$`Lf.(cm)`, dataBruch[,c('Année', 'Sexe')],min, na.rm = TRUE)
tapply(dataBruch$`Lf.(cm)`, dataBruch[,c('Année', 'Sexe')],quantile, na.rm = TRUE, probs=.05)

tapply(dataBruch$`Lf.(cm)`, dataBruch[,c('Année', 'Sexe')],max, na.rm = TRUE)
tapply(dataBruch$`Lf.(cm)`, dataBruch[,c('Année', 'Sexe')],quantile, na.rm = TRUE, probs=.95)

tapply(dataBruch$`Lf.(cm)`, dataBruch[,c('Année', 'Sexe')],quantile, na.rm = TRUE, probs=.5)

sel = dataBruch$Année==2013 & dataBruch$Sexe =='F'

hist(dataBruch$`Lt.(cm)`[sel])

abline(v=quantile(dataBruch$`Lt.(cm)`[sel], probs = 0.05))

sel = dataBruch$Sexe=='M'
lm (dataBruch$`Lt.(cm)`[sel]~dataBruch$`Lf.(cm)`[sel])

summary(lm (dataBruch$`Lt.(cm)`~dataBruch$`Lf.(cm)`))
summary(lm (dataBruch$`Lt.(cm)`~dataBruch$`Lf.(cm)` * dataBruch$Sexe))

# ====================================================
# fecundity
# Taverny 1991
# ====================================================
(41*172895+33*202902+74*186424)/(41+33+74)
(41*98390+33*110386+74*104325)/(41+33+74)

# ============================================================
# maximal production of recruit in GR3D for the Garonne basin
# ============================================================
bj=-log(1.7e-3) /.33
cj= 4.1e-4 / (84810*.5356)
(alphaj = bj*exp(-bj*.33)/(cj*(1-exp(-bj*.33))))

# ================================================
# growth in GR3D
# ================================================
temperatureEffect= function(temp, Tmin, Topt, Tmax){
  #  if (temp<=Tmin | temp >= Tmax)
  #    return(0)
  #  else
  response=(temp-Tmin)*(temp-Tmax)/((temp-Tmin)*(temp-Tmax)-(temp-Topt)^2)
  
  response[temp<=Tmin | temp >= Tmax] = 0
  return(response)
}

# ------------------------------------------------
# temperature effect on growth
# ------------------------------------------------
tempData=read.csv("/home/patrick/Documents/workspace/GR3D/data/input/reality/SeasonTempBVFacAtlant1801_2100_newCRU_RCP85.csv", sep=";")
sel = tempData$NOM=="Garonne" & tempData$Year>=2008 & tempData$Year<=2018
Tref=colMeans(tempData[sel, c("Winter", "Spring", "summer", "Autumn")])
TrefAtSea = (12 + Tref) / 2

temperature=seq(0,30,.1)
# temperature effect on growth
tempEffects = temperatureEffect(temperature,  3, 17, 26)
plot(temperature,tempEffects, type='l')

points(TrefAtSea,  temperatureEffect(TrefAtSea, 3, 17, 26), col="red", pch =20)
text(TrefAtSea, temperatureEffect(TrefAtSea, 3, 17, 26),  c("Winter", "Spring", "Summer", "Autumn"), pos=4)

points(Tref,  temperatureEffect(Tref, 3, 17, 26), col="red")
text(Tref, temperatureEffect(Tref, 3, 17, 26),  c("Winter", "Spring", "Summer", "Autumn"), pos=2, cex=.5)

plot(tempData$Year[sel], tempData$Winter[sel], type='l')

# ----------------------------------------------
# growth simulation
# ----------------------------------------------
vonBertalaffyGrowth = function(age, L0, Linf, K){
  t0=log(1-L0/Linf)/K
  return(Linf*(1-exp(-K*(age-t0))))
}

vonBertalaffyInverse = function(L, L0, Linf, K){
  t0=log(1-L0/Linf)/K
  return (t0 - log(1-L/Linf)/K)
}

KvonBertalaffy = function(age, L, L0, Linf){
  t0=log(1-L0/Linf)/K
  return (-log(1-L/Linf)/(age-t0))
}

#vonBertalaffyInverse(L=40, L0, Linf, koptMale)

Zfct=function(age, Ltarget,  L0, Linf, K, Tref ) {
  return (sapply(age,function(a) (vonBertalaffyGrowth(a, L0, Linf, K) * mean(temperatureEffect(Tref, 3, 17, 26)) - Ltarget)^2))
}

Pauly= function(age, t0, Linf, K, D){
  return(Linf/10*((1-exp(-K*D*(age-t0)))^(1/D)))
}

# pas cohérent avec la temperature effet sur le coeff de croissance mais ca marche
vonBertalaffyIncrement = function(nStep, L0, Linf, K, deltaT, sigma, withTempEffect=FALSE, TrefAtSea = c(9.876946, 13.489854, 15.891487, 11.554104) ){
  tempEffect = temperatureEffect( TrefAtSea , 3, 17, 26)
  L=matrix(nrow=nStep+1)
  L[1]=L0
  for (i in 1:nStep) {
    mu = log((Linf-L[i])*(1-exp(-K*deltaT))) - sigma*sigma/2
    increment = exp(rnorm(1, mu, sigma))
    if (withTempEffect){
      increment = increment * tempEffect[((i-1) %% 4)+1]
    }
    L[i+1]=L[i]+increment
  }
  return(L)
}

# ---------------------------------------------------
# parametres
# ----------------------------------------------------
L0 = 2
Linf = 80
#kopt = 0.3900707
#koptMale = 0.2
#koptFemale = 0.3
timestep = 0.25
sigma = 0.2

tempEffect = mean(temperatureEffect( TrefAtSea, 3, 17, 26))

#c(mean(temperatureEffect( Tref, 3, 17, 26)), mean(temperatureEffect( TrefAtSea, 3, 17, 26)))
age=seq(0,8,.25)


# ###########################################################################"
# comparaison male femelle
# ###########################################################################
Lfemale = 55
(koptFemale = KvonBertalaffy(5.5, L = Lfemale, L0=L0, Linf=Linf)/tempEffect)

Lmale = 40
(koptMale = KvonBertalaffy(4.5, L = Lmale, L0, Linf)/tempEffect)


plot(x=NaN, xlim=range(age), ylim=c(0, Linf), xlab ='age (year)', ylab ='length (cm)')
for (i in 1:100) {
  lines(age, vonBertalaffyIncrement(max(age)/timestep, L0, Linf, koptMale, timestep, sigma, withTempEffect = TRUE), col='blue')
}
lines(age, vonBertalaffyGrowth(age, L0, Linf, koptMale * tempEffect), lty=2, lwd = 2, col = 'black')

res=matrix(nrow = max(age)/timestep + 1, ncol= 100 )
# verification par rapport à la médiane et la moyenne
for (i in 1:100) {
  res[,i]=vonBertalaffyIncrement(max(age)/timestep, L0, Linf, koptMale, timestep, sigma, withTempEffect = TRUE)
}
lines(age, apply(res, 1, quantile, probs =0.5 ),  lty=2, lwd = 2, col = 'red')
lines( age, rowMeans(res))


for (i in 1:100) {
  lines(age, vonBertalaffyIncrement(max(age)/timestep, L0, Linf, koptFemale, timestep, sigma, withTempEffect = TRUE), col='pink')
}
lines(age, vonBertalaffyGrowth(age, L0, Linf, koptFemale * tempEffect), lty=2, lwd = 2)
abline(h = Lmale)
abline(h = Lfemale)

ageMale=vonBertalaffyInverse(L=Lmale, L0, Linf, koptMale*tempEffect)
points(ageMale, vonBertalaffyGrowth(ageMale, L0, Linf, koptMale * tempEffect), col = 'red')
segments(x0 = ageMale, y0=0, y1=vonBertalaffyGrowth(ageMale, L0, Linf, koptMale * tempEffect))

ageFemale=vonBertalaffyInverse(L=Lfemale, L0, Linf, koptFemale*tempEffect)
points(ageFemale, vonBertalaffyGrowth(ageFemale, L0, Linf, koptFemale * tempEffect), col = 'red')
segments(x0 = ageFemale, y0=0, y1=vonBertalaffyGrowth(ageFemale, L0, Linf, koptFemale * tempEffect))
cat(koptFemale, koptMale, sep =" ")
cat(ageFemale, ageMale, sep =" ")

# --------------------------------------------------------------
# croissance sur le Rhin
# --------------------------------------------------------------
sel = tempData$NOM=="Rhine" & tempData$Year>=2008 & tempData$Year<=2018
TrefRhine = (12 + colMeans(tempData[sel, c("Winter", "Spring", "summer", "Autumn")])) / 2
rbind(TrefAtSea, TrefRhine)
tempEffectRhine = mean(temperatureEffect( TrefRhine, 3, 17, 26))
c(vonBertalaffyInverse(L=Lfemale, L0, Linf, koptFemale*tempEffectRhine), 
  vonBertalaffyInverse(L=Lmale, L0, Linf, koptMale*tempEffectRhine))

# ======================================================================
#  temp en fonction de la latitude
# ====================================================================
library(dplyr)

extractTemp<-as.data.frame(tempData %>%
                             filter(Year>1901 & Year <= 1910) %>%
                             group_by(NOM,Lat)%>%
                             summarize(meanSpring=mean(Spring)) 
                           %>% arrange (Lat)
)

plot(extractTemp$Lat, extractTemp$meanSpring, pch=20)
abline(v=49.25) # vire
abline(v=53.75) # elbe


abline(h=11)
abline(h=12.5)
# ======================================================================
# exploration of growth for male and female
# ======================================================================
correction=mean(temperatureEffect(Tref, 3, 17, 26))

age=seq(0,10,.25)
present = vonBertalaffyGrowth(age, 2, 60, 0.3900707 * correction)
plot(age, present, type='l', lwd=3, ylim =c(0,80))
present[age == 5]
abline(v=5)

male =  vonBertalaffyGrowth(age, 2, 65, 0.3900707 * correction)
lines(age, male, type='l', lwd=3, ylim =c(0,80), col='blue')
abline(h=40, col='blue', lwd=2, lty=2)
male[age == 5]

female =  vonBertalaffyGrowth(age, 2, 75, 0.3900707*55/40 * correction)
lines(age, female, lwd=3, col='red')
abline(h=55, col='red', lwd=2, lty=2)
female[age == 5]

## a partir d'individus en mer donc à croissance  de plus lente à mesure qu'ils sont agées
(taverny = Pauly(age,t0=-0.7294, Linf=701.59, K=0.4491, D=.5912))
lines (age, taverny, lwd=2, col ='green')
taverny[age == 5]

# ===================================================================
# GR3D outputs
# =====================================================================
simData=read.csv("/home/patrick/Documents/workspace/GR3D/data/output/lengthAgeDistribution_1-RCP85.csv", sep=";", row.names = NULL)

simGaronne= simData[simData$basin =="Garonne",]
sel=simGaronne$nbSpawn == 0
tapply(simGaronne$length[sel], simGaronne[sel,c('year')],quantile, na.rm = TRUE, probs=.5)


# masse des gonades avant
sel = (dataBruch$LOT =='Tuilières' | dataBruch$LOT =='Golfech') & !is.na(dataBruch$`M.gonades.(g)`) & dataBruch$Sexe =='F'
mean(dataBruch$`M.gonades.(g)`[sel]/dataBruch$`M.tot.(g)`[sel])



sel = (dataBruch$LOT =='Tuilières' | dataBruch$LOT =='Golfech') & dataBruch$Sexe =='F'
sum(sel)
Wpre = mean(dataBruch$`M.tot.(g)`[sel])
Wgonad =mean(dataBruch$`M.gonades.(g)`[sel], na.rm = TRUE)

sel = (! (dataBruch$LOT =='Tuilières' | dataBruch$LOT =='Golfech')) & dataBruch$Sexe =='F'
Wpost= mean(dataBruch$`M.tot.(g)`[sel])
WgonadSpent =mean(dataBruch$`M.gonades.(g)`[sel], na.rm = TRUE)
(Wloss=(Wpre - Wpost)/Wpre)



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