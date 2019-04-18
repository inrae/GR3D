age=3:8

fecundity = function(age){
  871.72*age +50916
}
cbind(age, fecundity(age), fecundity(age) -mean(fecundity(age)))
# 


#===================================================
# masse des gonades, RGS
# ===================================================
# Taverny 1991
Wt=seq(1000, 3000, 200) # masse totale (g)
Gn = - 85.5634 + (Wt )*0.166 # masse de la gonade (g)
plot(Wt,Gn)
plot(Wt, Gn/Wt)

# ==================================================
# relation taille poinds
# =================================================
LtJuv=30:150 # mm
LtGen=400:800 #mm
Minho = function(Lt){
  # formule avec Lt en cm
  return(0.0221 * (Lt/10)^2.8147)
}


GirondeJuvenile = function(Lt){
  # pour année 1985
  exp(-11.571) * Lt^2.9467
}

GirondeMale = function(Lt){
  # Lt en mm
  # pour l'année  1988 (livre p44)
  2.4386e-6*Lt^3.2252
}

GirondeFemale = function(Lt){
  # Lt en mm
  # pour l'année  1988 (livre p44)
  1.2102e-6*Lt^3.3429
}
#  =========================================    PB
LoireMale = function(Lf){
  # longueur fourche
  5.5070e-3*Lf^3.114
}
LoireFemale = function(Lf){
  # longueur fourche
    7.2980e-3* Lf^3.019
}

SebouMale = function(Lt){
  2.0705e-6*Lt^3.2587
}
SebouFemale = function(Lt){
  8.2056e-7*Lt^3.4105
}

          
plot(LtJuv, GirondeJuvenile(LtJuv), type='l')


plot(LtGen, Minho(LtGen), type='l', col='black', ylim=c(0,5000), xlab ='total length (mm)', ylab = 'weight (g)')
lines(LtGen, GirondeMale(LtGen), col='green')
lines(LtGen, GirondeFemale(LtGen), col='green', lty=2)
lines(LtGen, SebouMale(LtGen), col='red')
lines(LtGen, SebouFemale(LtGen), col='red', lty=2)
lines(LtGen, LoireMale(LtGen), col='blue')
lines(LtGen, LoireFemale(LtGen), col='blue', lty=2)
legend('topleft', legend=c('Minho', 'Gironde Male', 'Gironde Female', 'Sebou Male', 'Sebou Female', 'Loire Male', 'Loire Female'),
       lty=c(1,1,2, 1,2,1,2, 1,2), 
       col=c('black', 'green', 'green', 'red', 'red', 'blue', 'blue'))

plot(LtGen, LoireMale(LtGen), col='blue', type='l',xlab ='total length (mm)', ylab = 'weight (g)')

lines(LtGen, LoireFemale(LtGen), col='blue', lty=2)
points(506, LoireFemale(506), col='blue')
