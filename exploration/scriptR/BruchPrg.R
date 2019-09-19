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
# temperature effect on spawner survival 
plot(temperature, temperatureEffect(temperature, 10, 20, 23), type='l')
# temperature effect on recruit survival 
lines(temperature, temperatureEffect(temperature, 9.75, 20, 26), col='red')


lines(temperature, temperatureEffect(temperature, 9.75, 20, 26) * temperatureEffect(temperature, 10, 20, 23), type='l', col='green')
lines(temperature, temperatureEffect(temperature, 9.75, 20, 26) * temperatureEffect(temperature, 10, 20, 23) * exp(-.4*5), type='l', col='blue')

tempData=read.csv("/home/patrick/Documents/workspace/GR3D/data/input/reality/SeasonTempBVFacAtlant1801_2100_newCRU_RCP85.csv", sep=";")
sel = tempData$NOM=="Garonne" & tempData$Year>=2008 & tempData$Year<=2018

Tref=colMeans(tempData[sel, c("Winter", "Spring", "summer", "Autumn")])
points(Tref,  temperatureEffect(Tref, 9.75, 20, 26), col="red")
text(Tref, temperatureEffect(Tref, 9.75, 20, 26),  c("Winter", "Spring", "Summer", "Autumn"), pos=1)

plot(tempData$Year[sel], tempData$Winter[sel], type='l')

mean( temperatureEffect(Tref, 9.75, 20, 26))
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

# pas cohérent avec la temperature effet sur le coeff de croissance
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

tempEffect = mean(temperatureEffect( c(7.753891, 14.979708, 19.782974, 11.108207) , 3, 17, 26))

age=seq(0,8,.25)


# ###########################################################################"
# comparaison male femelle
# ###########################################################################
Lfemale = 55
(koptFemale = KvonBertalaffy(5.5, L = Lfemale, L0=L0, Linf=Linf)/tempEffect)

Lmale = 40
(koptMale = KvonBertalaffy(4.5, L = Lmale, L0, Linf)/tempEffect)


plot(x=NaN, xlim=range(age), ylim=c(0, Linf), xlabel ='age (year)', ylabel ='length (cm)')
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
lines(age, apply(res, 1, quantile, probs =0.5 ),  lty=2, lwd = 2, col = 'red')


for (i in 1:100) {
  lines(age, vonBertalaffyIncrement(max(age)/timestep, L0, Linf, koptFemale, timestep, sigma, withTempEffect = TRUE), col='pink')
}
lines(age, vonBertalaffyGrowth(age, L0, Linf, koptFemale * tempEffect), lty=2, lwd = 2)
abline(h = Lmale)
abline(h = Lfemale)

ageMale=vonBertalaffyInverse(L=Lmale, L0, Linf, koptMale*tempEffect)
points(ageMale, vonBertalaffyGrowth(ageMale, L0, Linf, koptMale * tempEffect), col = 'red')
segments(x0 = ageMale, y0=0, y1=vonBertalaffyGrowth(ageMale, L0, Linf, koptMale * tempEffect))

ageFemale=vonBertalaffyInverse(L=55, L0, Linf, koptFemale*tempEffect)
points(ageFemale, vonBertalaffyGrowth(ageFemale, L0, Linf, koptFemale * tempEffect), col = 'red')
segments(x0 = ageFemale, y0=0, y1=vonBertalaffyGrowth(ageFemale, L0, Linf, koptFemale * tempEffect))
cat(koptFemale, koptMale, sep =" ")
cat(ageFemale, ageMale, sep =" ")


######################################################################################
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
