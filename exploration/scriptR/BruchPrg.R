library(openxlsx)
# ============================================================

dataBruch = read.xlsx("BDalosesBruch.xlsx")
dataBruch$`M.gonades.(g)` = as.numeric(dataBruch$`M.gonades.(g)`)

head(dataBruch)


tapply(dataBruch$`Lf.(cm)`, dataBruch[,c('Ann√©e', 'Sexe')],min, na.rm = TRUE)

tapply(dataBruch$`Lf.(cm)`, dataBruch[,c('Ann√©e', 'Sexe')],min, na.rm = TRUE)
tapply(dataBruch$`Lf.(cm)`, dataBruch[,c('Ann√©e', 'Sexe')],quantile, na.rm = TRUE, probs=.05)

tapply(dataBruch$`Lf.(cm)`, dataBruch[,c('Ann√©e', 'Sexe')],max, na.rm = TRUE)
tapply(dataBruch$`Lf.(cm)`, dataBruch[,c('Ann√©e', 'Sexe')],quantile, na.rm = TRUE, probs=.95)

tapply(dataBruch$`Lf.(cm)`, dataBruch[,c('Ann√©e', 'Sexe')],quantile, na.rm = TRUE, probs=.5)

sel = dataBruch$Ann√©e==2013 & dataBruch$Sexe =='F'

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

# ------------------------------------------------
# temperature effect on growth
# ------------------------------------------------
temperatureEffect= function(temp, Tmin, Topt, Tmax){
  #  if (temp<=Tmin | temp >= Tmax)
  #    return(0)
  #  else
  response=(temp-Tmin)*(temp-Tmax)/((temp-Tmin)*(temp-Tmax)-(temp-Topt)^2)
  
  response[temp<=Tmin | temp >= Tmax] = 0
  return(response)
  
}
temperature=seq(8,30,.1)
# temperature effect on spawner survival (Survival Process in GR3D)
plot(temperature, temperatureEffect(temperature, 10, 20, 23), type='l')
# temperature effect on recruit survival (Reproduction process in GR3D, that is computed from the survival curve of juveniles (Jatteau et al, 2017) )
lines(temperature, temperatureEffect(temperature, 9.75, 20, 26), col='red')


lines(temperature, temperatureEffect(temperature, 9.75, 20, 26) * temperatureEffect(temperature, 10, 20, 23), type='l', col='green')
lines(temperature, temperatureEffect(temperature, 9.75, 20, 26) * temperatureEffect(temperature, 10, 20, 23) * exp(-.4*5), type='l', col='blue')
tempData=read.csv("/home/patrick.lambert/Documents/workspace/GR3D/data/input/reality/SeasonTempBVFacAtlant1801_2100_newCRU_RCP85.csv", sep=";")
sel = tempData$NOM=="Garonne" & tempData$Year>=2008 & tempData$Year<=2018
plot(tempData$Year[sel], tempData$Winter[sel], type='l')
Tref=colMeans(tempData[sel, c("Winter", "Spring", "summer", "Autumn")])
points(Tref,  temperatureEffect(Tref, 9.75, 20, 26), col="red")
text(Tref, temperatureEffect(Tref, 9.75, 20, 26),  c("Winter", "Spring", "Summer", "Autumn"), pos=1)

mean( temperatureEffect(Tref, 9.75, 20, 26))
# ----------------------------------------------
# growth simulation
# ----------------------------------------------
vonBertalaffyGrowth = function(age, L0, Linf, K){
  t0=log(1-L0/Linf)/K
  return(Linf*(1-exp(-K*(age-t0))))
}

Pauly= function(age, t0, Linf, K, D){
  return(Linf/10*((1-exp(-K*D*(age-t0)))^(1/D)))
}

vonBertalaffyIncrement = function(nStep, L0, Linf, K, deltaT, sigma, withTempEffect=FALSE){
  tempEffect = temperatureEffect( c(7.753891, 14.979708, 19.782974, 11.108207) , 3, 17, 26)
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

vonBertalaffyIncrement(6/.25, 0, 60, 0.3900707, .25, .2)

age=seq(0,6,.25)
plot(age,vonBertalaffyGrowth(age, 2, 60, 0.3900707), type="l")
for (i in 1:100) {
  lines(age, vonBertalaffyIncrement(6/.25, 2, 60, 0.3900707, .25, .2), col='red')
}
lines(age, vonBertalaffyGrowth(age, 2, 60, 0.3900707), lwd=3, col='black')
abline(h=40)
for (i in 1:100) {
  lines(age, vonBertalaffyIncrement(6/.25, 2, 60, 0.3900707, .25, .2, withTempEffect = TRUE), col='green')
}
lines(age, vonBertalaffyGrowth(age, 2, 60, 0.3900707*mean(temperatureEffect(Tref, 3, 17, 26))), lty=2, lwd = 2)

abline(h=40)



nbRep=1000
res=matrix(nrow=nbRep)
for (i in 1:nbRep) {
  prov = vonBertalaffyIncrement(24, 2, 60, 0.3900707, .25, .2)
  res[i] = prov[max(which(prov < 40))+4]
}
mean(res)
hist(res,20)
abline(v=mean(res), col='red')

res2=matrix(nrow=nbRep)
for (i in 1:nbRep) {
  res2[i] = vonBertalaffyIncrement(4, 40, 60, 0.3900707, .25, .2)[5]
}
mean(res2)
hist(res2,20)
abline(v=mean(res2), col='red')


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

## a partir d'individus en mer donc √† croissance  de plus lente √† mesure qu'ils sont ag√©es
(taverny = Pauly(age,t0=-0.7294, Linf=701.59, K=0.4491, D=.5912))
lines (age, taverny, lwd=2, col ='green')
taverny[age == 5]

# ===================================================================
# GR3D outputs
# =====================================================================
simData=read.csv("/home/patrick.lambert/Documents/workspace/GR3D/data/output/lengthAgeDistribution_1-RCP85.csv", sep=";", row.names = NULL)

simGaronne= simData[simData$basin =="Garonne",]
sel=simGaronne$nbSpawn == 0
tapply(simGaronne$length[sel], simGaronne[sel,c('year')],quantile, na.rm = TRUE, probs=.5)


# masse des gonades avant
sel = (dataBruch$LOT =='Tuili√®res' | dataBruch$LOT =='Golfech') & !is.na(dataBruch$`M.gonades.(g)`) & dataBruch$Sexe =='F'
mean(dataBruch$`M.gonades.(g)`[sel]/dataBruch$`M.tot.(g)`[sel])

sel = (dataBruch$LOT =='Tuili√®res' | dataBruch$LOT =='Golfech') & dataBruch$Sexe =='F'
sum(sel)
Wpre = mean(dataBruch$`M.tot.(g)`[sel])
Wgonad =mean(dataBruch$`M.gonades.(g)`[sel], na.rm = TRUE)

sel = (! (dataBruch$LOT =='Tuili√®res' | dataBruch$LOT =='Golfech')) & dataBruch$Sexe =='F'
Wpost= mean(dataBruch$`M.tot.(g)`[sel])
WgonadSpent =mean(dataBruch$`M.gonades.(g)`[sel], na.rm = TRUE)
(Wloss=(Wpre - Wpost)/Wpre)

# ===================================================================
# Exploration of Stock recruitement-relationship for GR3D calibration
# =====================================================================

#Use to improve the likelihood between observations and GR3D outputs in terms of abudances and North limit colonization. 

#a = fÈconditÈ de l'espËce, a = 135000
#S = quantitÈ de gÈniteurs: ici on veut la quantitÈ R0 produite par 1000 gÈniteurs en fonction de la T∞ 
#Ratio = 0.2 
#n= paramËtre simulant l'effet Allee 


#-----------On cherche a reproduire la relation SR telle que modÈlisÈe dans GR3D-------------- 

temperatureEffect= function(tempWater, TminRep, ToptRep, TmaxRep){
  #  if (tempWater<=TminRep | tempWater >= TmaxRep)
  #    return(0)
  #  else
  response=(tempWater-TminRep)*(tempWater-TmaxRep)/((tempWater-TminRep)*(tempWater-TmaxRep)-(tempWater-ToptRep)^2)
  
  response[tempWater<=TminRep | tempWater >= TmaxRep] = 0
  return(response)
  
}

#Relation SR telle qu'elle est modÈlisÈe dans GR3D

numberOfSpawner<- seq(0:400000)

StockRecruitementRelationship <-function (temp, surfaceWatershed, S) {
  
  lambda = 4.1E-4
  deltaTrecruitement = 0.33
  survOptRep =  0.0017
  n= 2.4
  ratioTeta = 1.9
  a = 135000
  
  #parametre c de la RS de BH intÈgrant un effet du BV considÈrÈ 
  cj = lambda/surfaceWatershed
  
  #parametre b reprÈsentant la mortalitÈ densitÈ dÈpendante de la RS de BH intÈgrant un effet de la temperature
  # bj = (-(1/deltaTrecruitement))*
  #   log(survOptRep * temperatureEffect(temp, 9.8, 20.0, 26.0))
   
  bj = - log(survOptRep * temperatureEffect(temp, 9.8, 20.0, 26.0)) / deltaTrecruitement
  
  #parametre a (fÈconditÈ de l'espËce) de la RS de BH intÈgrant un effet de la temperature
  alphaj = (bj * exp(-bj * deltaTrecruitement)) / (cj * (1-exp(-bj * deltaTrecruitement)))
  
  #Bj = paramËtre de la relation SR intÈgrant l'effet de la tempÈrature 
  betaj = bj/(a*cj*(1-exp(-bj*deltaTrecruitement)))
  
  #p = proportion de gÈniteurs participant ‡ la reproduction en focntion de la quantitÈ de gÈniteur total
  #p = 1/(1+exp(-log(19)*(S-n)/(Ratio*surfaceWatershed)))
  
  S95 = n * surfaceWatershed
  S50 = S95/ratioTeta
  
  p= 1/(1+exp(-log(19)*(S-S50)/(S95-S50)))
  
  #relation Stock Recrutement ie calcul le nombre de recrues en fonction du nombre de gÈniteurs et de la T en intÈgrant l'effet Allee 
  
  #R0 = aj * S * p 
  
  AlleeEffect = 1/ (1+exp(-log(19)*(S -n/ratioTeta*surfaceWatershed)/(n*surfaceWatershed -n/ratioTeta*surfaceWatershed)))
  
  Rj = (alphaj * S * AlleeEffect)/(betaj +S * AlleeEffect)
  
  #Rj = ((aj * S) * p)/(Bj +S * p)
  
  StockRecruitement = as.data.frame(Rj)
  
  return (Rj) 
  
}

  StockRecruitement<-StockRecruitementRelationship (18, 84810, numberOfSpawner) 

  plot(numberOfSpawner, StockRecruitement,type = 'l', xlab= "Number of spawners", ylab = "Number of recruits")
  
#-----------On cherche ‡ dÈterminer le numbre de juvÈniles gÈnÈrÈs par S = 100000 gÈniteurs en fonction de la T∞ -------------- 

temperature <- seq (8,30,.1)
numberOfSpawner=100000
  
  StockRecruitementRelationship <-function (temp, surfaceWatershed, S) {
    
    lambda = 4.1E-4
    deltaTrecruitement = 0.33
    survOptRep =  0.0017
    n= 2.4
    ratioTeta = 1.9
    a = 135000
    
    #parametre c de la RS de BH intÈgrant un effet du BV considÈrÈ 
    cj = lambda/surfaceWatershed
    
    #parametre b reprÈsentant la mortalitÈ densitÈ dÈpendante de la RS de BH intÈgrant un effet de la temperature
    # bj = (-(1/deltaTrecruitement))*
    #   log(survOptRep * temperatureEffect(temp, 9.8, 20.0, 26.0))
    
    bj = - log(survOptRep * temperatureEffect(temp, 9.8, 20.0, 26.0)) / deltaTrecruitement
    
    #parametre a (fÈconditÈ de l'espËce) de la RS de BH intÈgrant un effet de la temperature
    alphaj = (bj * exp(-bj * deltaTrecruitement)) / (cj * (1-exp(-bj * deltaTrecruitement)))
    
    #Bj = paramËtre de la relation SR intÈgrant l'effet de la tempÈrature 
    betaj = bj/(a*cj*(1-exp(-bj*deltaTrecruitement)))
    
    #p = proportion de gÈniteurs participant ‡ la reproduction en focntion de la quantitÈ de gÈniteur total
    #p = 1/(1+exp(-log(19)*(S-n)/(Ratio*surfaceWatershed)))
    
    S95 = n * surfaceWatershed
    S50 = S95/ratioTeta
    
    p= 1/(1+exp(-log(19)*(S-S50)/(S95-S50)))
    
    #relation Stock Recrutement ie calcul le nombre de recrues en fonction du nombre de gÈniteurs et de la T en intÈgrant l'effet Allee 
    
    #R0 = aj * S * p 
    
    AlleeEffect = 1/ (1+exp(-log(19)*(S -n/ratioTeta*surfaceWatershed)/(n*surfaceWatershed -n/ratioTeta*surfaceWatershed)))
    
    Rj = (alphaj * S * AlleeEffect)/(betaj +S * AlleeEffect)
    
    #Rj = ((aj * S) * p)/(Bj +S * p)
    
    StockRecruitement = as.data.frame(Rj)
    
    return (Rj) 
    
  }
  
  plot(temperature, StockRecruitementRelationship (temperature, 84810, numberOfSpawner),
       type="l", 
       xlab =" Temperature (∞C)",
       ylab = "Number Of Recruits")
  

#-----------On cherche ‡ dÈterminer le numbre de gÈniteurs survivants en fonction de la T∞ -------------- 

  #Prend en compte Zsea 
  
  plot(temperature, StockRecruitementRelationship (temperature, 84810, numberOfSpawner),
       type="l", 
       xlab =" Temperature (∞C)",
       ylab = "Number Of Recruits")






