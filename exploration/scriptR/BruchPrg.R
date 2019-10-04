library(openxlsx)
# ============================================================

dataBruch = read.xlsx("BDalosesBruch.xlsx")
#dataBruch = read.xlsx("/home/patrick/Documents/AA/CC et migrateur/thèse Poulet/BDalosesBruch.xlsx")
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
tempData = read.csv("/home/patrick/Documents/workspace/GR3D/data/input/reality/SeasonTempBVFacAtlant1801_2100_newCRU_RCP85.csv", sep =";")
sel = tempData$NOM == "Garonne" & tempData$Yea >= 2008 & tempData$Year <= 2018
Tref = colMeans(tempData[sel, c("Winter", "Spring", "summer", "Autumn")])
TrefAtSea = (12 + Tref) / 2

temperature = seq(0,30,.1)
# temperature effect on growth
tempEffects = temperatureEffect(temperature,  3, 17, 26)
plot(temperature,tempEffects, type = 'l')

points(TrefAtSea,  temperatureEffect(TrefAtSea, 3, 17, 26), col = "red", pch = 20)
text(TrefAtSea, temperatureEffect(TrefAtSea, 3, 17, 26),  c("Winter", "Spring", "Summer", "Autumn"), pos = 4)

points(Tref,  temperatureEffect(Tref, 3, 17, 26), col = "red")
text(Tref, temperatureEffect(Tref, 3, 17, 26),  c("Winter", "Spring", "Summer", "Autumn"), pos = 2, cex = .5)

plot(tempData$Year[sel], tempData$Winter[sel], type = 'l')

# ----------------------------------------------
# growth simulation
# ----------------------------------------------
vonBertalaffyGrowth = function(age, L0, Linf, K){
  t0 = log(1 - L0 / Linf) / K
  return(Linf * (1 - exp(-K * (age - t0))))
}

vonBertalaffyInverse = function(L, L0, Linf, K){
  t0 = log(1 - L0/Linf)/K
  return(t0 - log(1 - L/Linf)/K)
}

KvonBertalaffy = function(age, L, L0, Linf) {
  return(-log((Linf - L ) / (Linf - L0)) / age)
}

#vonBertalaffyInverse(L=40, L0, Linf, koptMale)

Zfct = function(age, Ltarget,  L0, Linf, K, Tref ) {
  return(sapply(age,function(a) (vonBertalaffyGrowth(a, L0, Linf, K) * mean(temperatureEffect(Tref, 3, 17, 26)) - Ltarget)^2))
}

Pauly = function(age, t0, Linf, K, D){
  return(Linf / 10 * ((1 - exp(-K * D * (age - t0)))^(1 / D)))
}

# pas cohérent avec la temperature effet sur le coeff de croissance mais ca marche
vonBertalaffyIncrement = function(nStep, L0, Linf, K, deltaT, sigma, withTempEffect=FALSE, TrefAtSea = c(9.876946, 13.489854, 15.891487, 11.554104) ){
  tempEffect = temperatureEffect( TrefAtSea , 3, 17, 26)
  L = matrix(nrow = nStep + 1)
  L[1] = L0
  for (i in 1:nStep) {
    mu = log((Linf - L[i]) * (1 - exp(-K * deltaT))) - sigma * sigma / 2
    increment = exp(rnorm(1, mu, sigma))
    if (withTempEffect) {
      increment = increment * tempEffect[((i - 1) %% 4) + 1]
    }
    L[i + 1] = L[i] + increment
  }
  return(L)
}

# ---------------------------------------------------
# parametres
# ----------------------------------------------------
L0 = 2
Linf = 70
#kopt = 0.3900707
#koptMale = 0.2
#koptFemale = 0.3
timestep = 0.25 # time step of the simumation
sigma = 0.2

tempEffect = mean(temperatureEffect( TrefAtSea, 3, 17, 26))

#c(mean(temperatureEffect( Tref, 3, 17, 26)), mean(temperatureEffect( TrefAtSea, 3, 17, 26)))
age = seq(0,8,.25)


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
abline(v=45.25) # Garonne
abline(v=49.25) # vire
abline(v=52.25) # Rhine


abline(h=11)
abline(h=12.5)
# ======================================================================
# exploration of growth for male and female
# ======================================================================
correction=mean(temperatureEffect(TrefAtSea, 3, 17, 26))

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

# ===================================================================
# Exploration of Stock recruitement-relationship for GR3D calibration
# =====================================================================

#Use to improve the likelihood between observations and GR3D outputs in terms of abudances and North limit colonization. 

#a = fécondité de l'espèce, a = 135000
#S = quantité de géniteurs: ici on veut la quantité R0 produite par 1000 géniteurs en fonction de la T° 
#Ratio = 0.2 
#n= paramètre simulant l'effet Allee 


#-----------On cherche a reproduire la relation SR telle que modélisée dans GR3D-------------- 

temperatureEffect= function(tempWater, TminRep, ToptRep, TmaxRep){
  #  if (tempWater<=TminRep | tempWater >= TmaxRep)
  #    return(0)
  #  else
  response=(tempWater-TminRep)*(tempWater-TmaxRep)/((tempWater-TminRep)*(tempWater-TmaxRep)-(tempWater-ToptRep)^2)
  
  response[tempWater<=TminRep | tempWater >= TmaxRep] = 0
  return(response)
  
}

#Relation SR telle qu'elle est modélisée dans GR3D

numberOfSpawner<- seq(0:400000)

StockRecruitementRelationship <-function (temp, surfaceWatershed, S) {
  
  lambda = 4.1E-4
  deltaTrecruitement = 0.33
  survOptRep =  0.0017
  n= 2.4
  ratioTeta = 1.9
  a = 135000
  
  #parametre c de la RS de BH intégrant un effet du BV considéré 
  cj = lambda/surfaceWatershed
  
  #parametre b représentant la mortalité densité dépendante de la RS de BH intégrant un effet de la temperature
  # bj = (-(1/deltaTrecruitement))*
  #   log(survOptRep * temperatureEffect(temp, 9.8, 20.0, 26.0))
   
  bj = - log(survOptRep * temperatureEffect(temp, 9.8, 20.0, 26.0)) / deltaTrecruitement
  
  #parametre a (fécondité de l'espèce) de la RS de BH intégrant un effet de la temperature
  alphaj = (bj * exp(-bj * deltaTrecruitement)) / (cj * (1-exp(-bj * deltaTrecruitement)))
  
  #Bj = paramètre de la relation SR intégrant l'effet de la température 
  betaj = bj/(a*cj*(1-exp(-bj*deltaTrecruitement)))
  
  #p = proportion de géniteurs participant à la reproduction en focntion de la quantité de géniteur total
  #p = 1/(1+exp(-log(19)*(S-n)/(Ratio*surfaceWatershed)))
  
  S95 = n * surfaceWatershed
  S50 = S95/ratioTeta
  
  p= 1/(1+exp(-log(19)*(S-S50)/(S95-S50)))
  
  #relation Stock Recrutement ie calcul le nombre de recrues en fonction du nombre de géniteurs et de la T en intégrant l'effet Allee 
  
  #R0 = aj * S * p 
  
  AlleeEffect = 1/ (1+exp(-log(19)*(S -n/ratioTeta*surfaceWatershed)/(n*surfaceWatershed -n/ratioTeta*surfaceWatershed)))
  
  Rj = (alphaj * S * AlleeEffect)/(betaj +S * AlleeEffect)
  
  #Rj = ((aj * S) * p)/(Bj +S * p)
  
  StockRecruitement = as.data.frame(Rj)
  
  return (Rj) 
  
}

  StockRecruitement<-StockRecruitementRelationship (18, 84810, numberOfSpawner) 

  plot(numberOfSpawner, StockRecruitement,type = 'l', xlab= "Number of spawners", ylab = "Number of recruits")
  
#-----------On cherche à déterminer le numbre de juvéniles générés par S = 100000 géniteurs en fonction de la T° -------------- 

temperature <- seq (8,30,.1)
numberOfSpawner=100000
  
  StockRecruitementRelationship <-function (temp, surfaceWatershed, S) {
    
    lambda = 4.1E-4
    deltaTrecruitement = 0.33
    survOptRep =  0.0017
    n= 2.4
    ratioTeta = 1.9
    a = 135000
    
    #parametre c de la RS de BH intégrant un effet du BV considéré 
    cj = lambda/surfaceWatershed
    
    #parametre b représentant la mortalité densité dépendante de la RS de BH intégrant un effet de la temperature
    # bj = (-(1/deltaTrecruitement))*
    #   log(survOptRep * temperatureEffect(temp, 9.8, 20.0, 26.0))
    
    bj = - log(survOptRep * temperatureEffect(temp, 9.8, 20.0, 26.0)) / deltaTrecruitement
    
    #parametre a (fécondité de l'espèce) de la RS de BH intégrant un effet de la temperature
    alphaj = (bj * exp(-bj * deltaTrecruitement)) / (cj * (1-exp(-bj * deltaTrecruitement)))
    
    #Bj = paramètre de la relation SR intégrant l'effet de la température 
    betaj = bj/(a*cj*(1-exp(-bj*deltaTrecruitement)))
    
    #p = proportion de géniteurs participant à la reproduction en focntion de la quantité de géniteur total
    #p = 1/(1+exp(-log(19)*(S-n)/(Ratio*surfaceWatershed)))
    
    S95 = n * surfaceWatershed
    S50 = S95/ratioTeta
    
    p= 1/(1+exp(-log(19)*(S-S50)/(S95-S50)))
    
    #relation Stock Recrutement ie calcul le nombre de recrues en fonction du nombre de géniteurs et de la T en intégrant l'effet Allee 
    
    #R0 = aj * S * p 
    
    AlleeEffect = 1/ (1+exp(-log(19)*(S -n/ratioTeta*surfaceWatershed)/(n*surfaceWatershed -n/ratioTeta*surfaceWatershed)))
    
    Rj = (alphaj * S * AlleeEffect)/(betaj +S * AlleeEffect)
    
    #Rj = ((aj * S) * p)/(Bj +S * p)
    
    StockRecruitement = as.data.frame(Rj)
    
    return (Rj) 
    
  }
  
  plot(temperature, StockRecruitementRelationship (temperature, 84810, numberOfSpawner),
       type="l", 
       xlab =" Temperature (°C)",
       ylab = "Number Of Recruits")
  

#-----------On cherche à déterminer le numbre de géniteurs survivants en fonction de la T° -------------- 

  #Prend en compte Zsea 
  
  plot(temperature, StockRecruitementRelationship (temperature, 84810, numberOfSpawner),
       type="l", 
       xlab =" Temperature (°C)",
       ylab = "Number Of Recruits")






