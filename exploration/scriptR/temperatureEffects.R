
rm(list = ls())

Ratkowsky = function(temperature, par){
  # Ratkowsky, D. A., McMEEKIN, T. A. & Chandler, R. E. (1983) Model for Bacterial Culture Growth Rate Throughout the Entire 
  #    Biokinetic Temperature Range. J. BACTERIOL. 154, 5.
  
  #  see  Shi, P.-J., Reddy, G. V. P., Chen, L. & Ge, F. (2016) Comparison of Thermal Performance Equations in Describing Temperature-Dependent
  #    Developmental Rates of Insects: (I) Empirical Models. Annals of the Entomological Society of America 109, 211–215.

  #
  # Li, W. K. & Dickie, P. M. (1987) Temperature characteristics of photosynthetic and heterotrophic activities: seasonal 
  #    variations in temperate microbial plankton. Applied and Environmental Microbiology 53, 2282–2295.
  # Low‐Décarie, E., Boatman, T. G., Bennett, N., Passfield, W., Gavalás‐Olea, A., Siegel, P. & Geider, R. J. (2017) Predictions 
  #    of response to temperature are contingent on model choice and data quality. Ecology and Evolution 7, 10467–10481.
  # Grimaud, G. M., Mairet, F., Sciandra, A. & Bernard, O. (2017) Modeling the temperature effect on the specific growth rate of 
  #    phytoplankton: a review. Reviews in Environmental Science and Bio/Technology 16, 625–645.


  Tmin = par[1]
  Tmax = par[2]
  a = par[3] # give the extent of change in rate for unit change in temperature below Topt 
  b = par[4] # give the extent of change in rate for unit change in temperature above Topt 
  
  rate = ((a * (temperature - Tmin))^2) * ((1 - exp(b * (temperature - Tmax)))^2)
  
  rate[!between(temperature, Tmin, Tmax)] = 0
  return(rate)
}


temperature = seq(0, 35, .1)
Tmin = 10
Tmax = 25

temperature = seq(Tmin, Tmax, .1)
plot(temperature, Ratkowsky(temperature, par = c(Tmin, Tmax, .2, .2)), type = 'l', ylab = 'rate', mmain = 'Ratkowsky')
lines(temperature, Ratkowsky(temperature, par = c(Tmin, Tmax, .3, .1)), col = 2)
lines(temperature, Ratkowsky(temperature, par = c(Tmin, Tmax, .1, .3)), col = 3)

modifiedLehman = function(temperature, par){
  # Svirezhev, Yu. M., Krysanova, V. P. & Voinov, A. A. (1984) Mathematical modelling of a fish pond ecosystem. 
  #   Ecological Modelling 21, 315–337.

  Tmin = par[1]
  Topt = par[2]
  Tmax = par[3]
  
  # rate(Tmin or Tmax) = exp(-4.6) = 1 %

  rate = rep(0, length(temperature))
  rate[temperature < Topt] = exp(-4.6 * ((Topt - temperature[temperature < Topt]) / (Topt - Tmin))^4)
  rate[temperature >= Topt] = exp(-4.6 * ((temperature[temperature >= Topt] - Topt) / (Tmax - Topt))^4)
  
  return(rate)
}

Tmin = 10
Topt = 20
Tmax = 30

plot(temperature, modifiedLehman(temperature,  par = c(Tmin, Topt, Tmax)), ylab = 'rate', type = 'l', main = ' modified Lehman')
temperature[which.max(modifiedLehman(temperature,  par = c(Tmin, Topt, Tmax)))]

Rosso1 = function(temperature, par){
  # Rosso, L., Lobry, J. R. & Flandrois, J. P. (1993) An Unexpected Correlation between Cardinal Temperatures of Microbial Growth Highlighted 
  #    by a New Model. Journal of Theoretical Biology 162, 447–463.
  # Rosso, L., Lobry, J. R., Bajard, S. & Flandrois, J. P. (1995) Convenient model to describe the combined effects of temperature 
  #   and pH on microbial growth. Applied and Environmental Microbiology 61, 610–616.
  
  Tmin = par[1]
  Topt = par[2]
  Tmax = par[3]
  
  rate = ((temperature - Tmax) * (temperature - Tmin)^2) /
    ((Topt - Tmin) * ((Topt - Tmin) * (temperature - Topt) - (Topt - Tmax) * (Topt + Tmin - 2 * temperature)))
  
  rate[temperature <= Tmin | temperature >= Tmax] = 0
  
  return(rate)
}

Rosso2 = function(temperature, par){
  # with an inflexion point below Topt
  #
  # Rosso, L., Lobry, J. R., Bajard, S. & Flandrois, J. P. (1995) Convenient model to describe the combined effects of temperature 
  #   and pH on microbial growth. Applied and Environmental Microbiology 61, 610–616.
  # Grimaud, G. M., Mairet, F., Sciandra, A. & Bernard, O. (2017) Modeling the temperature effect on the specific growth rate of 
  #   phytoplankton: a review. Reviews in Environmental Science and Bio/Technology 16, 625–645.
  
  # Rougier, T., Drouineau, H., Dumoulin, N., Faure, T., Deffuant, G., Rochard, E. & Lambert, P. (2014) The GR3D model, a tool to explore 
  #   the Global Repositioning Dynamics of Diadromous fish Distribution. Ecological Modelling 283, 31–44.
  # Kielbassa, J., Delignette-Muller, M. L., Pont, D. & Charles, S. (2010) Application of a temperature-dependent von Bertalanffy growth 
  #   model to bullhead (Cottus gobio). Ecological Modelling 221, 2475–2481.



  Tmin = par[1]
  Topt = par[2]
  Tmax = par[3]
  
  rate = (temperature - Tmin) * (temperature - Tmax) / ((temperature - Tmin) * (temperature - Tmax) - (temperature - Topt)^2)
  
  rate[temperature <= Tmin | temperature >= Tmax] = 0
  
  return(rate)
}

Tmin = 10
Topt = 22
Tmax = 25
plot(temperature, Rosso2(temperature,  par = c(Tmin, Topt, Tmax)), ylab = 'rate', type = 'l')
lines(temperature, Rosso1(temperature,  par = c(Tmin, Topt, Tmax)), col = 2)
legend("topleft", legend = c("Rosso1 (GR3D)", "Rosso2"), lty = 1, col = c(1,2))


Blanchard = function(temperature, par){
  # Grimaud, G. M., Mairet, F., Sciandra, A. & Bernard, O. (2017) Modeling the temperature effect on the specific growth rate of 
  #   phytoplankton: a review. Reviews in Environmental Science and Bio/Technology 16, 625–645.
  
  Topt = par[1]
  Tmax = par[2]
  beta = par[3]
  
  rate = (((Tmax - temperature) / (Tmax - Topt))^beta) * exp(-beta * (Topt - temperature) / (Tmax - Topt))
  rate[is.na(rate)] = 0
  rate[temperature > Tmax] = 0
  return(rate)
  }

Topt = 22
Tmax = 25
betas = c(1.1, 2, 4)
plot(temperature, Blanchard(temperature,  par = c(Topt, Tmax, betas[1])), ylab = 'rate', type = 'l', main = "Blanchard")
for (i in (2:length(betas))){
  lines(temperature, Blanchard(temperature,  par = c(Topt, Tmax, betas[i])), col =i)
}
legend("topleft", legend = paste0("betas = ", betas), lty = 1, col = 1 : length(betas))

EppleyNorberg = function(temperature, par){
  # Eppley, R. W. (1972) Temperature and phytoplankton growth in the sea. Fish. bull 70, 1063–1085.
  # Norberg, J. (2004) Biodiversity and ecosystem functioning: A complex adaptive systems approach.
  #   Limnology and Oceanography 49, 1269–1277.
  #
  # Grimaud, G. M., Mairet, F., Sciandra, A. & Bernard, O. (2017) Modeling the temperature effect on the specific growth rate of 
  #   phytoplankton: a review. Reviews in Environmental Science and Bio/Technology 16, 625–645.

  w = par[1] # the thermal niche width
  z = par[2] # the temperature at which the growth rate is equal to the Eppley function and is a proxy of Topt 
  a = par[3] # 
  b = par[4] # 
  
  rate = (1 - ((temperature - z) / w)^2) * a * exp(b * temperature)
  rate[rate < 0] = 0
  return(rate)
}

plot(temperature, EppleyNorberg(temperature, par = c(10, 20 , .6,.06)), type = 'l', ylab = 'rate', main = "EppleyNorberg")


modifiedMasterReaction = function(temperature, par){
  # Corkrey, R., McMeekin, T. A., Bowman, J. P., Ratkowsky, D. A., Olley, J. & Ross, T. (2014) Protein Thermodynamics Can Be Predicted 
  # Directly from Biological Growth Rates. PLoS ONE 9, e96100.

  
  # Grimaud, G. M., Mairet, F., Sciandra, A. & Bernard, O. (2017) Modeling the temperature effect on the specific growth rate of 
  #   phytoplankton: a review. Reviews in Environmental Science and Bio/Technology 16, 625–645.
  
  TinK = temperature + 273.15

  deltaH_star = 4874 # enthalpy change (J/mol amino acid residue)
  T_H_star = 375.5   # conergence temperaure for enthalpy (K) 
  deltaS_star = 17.0 # entropy change (J / K)
  T_S_star = 390.9   #  convergence temperature fot entropy (K) 
  R = 8.314          # gaz constant (J/ K mol)
  
  deltaH_A = par[1]  # enthalpy activation (J / mol)
  deltaCp = par[2]   # the heat capacity change (J/mol amino acid residue)
  n = par[3]         # the number of amino acid residues  
  c = par[4]         # scaling constant 
  
  rate = TinK *  exp(c - deltaH_A / (R * TinK)) / 
    (1 + exp(-n * (deltaH_star - TinK * deltaS_star + deltaCp * (TinK - T_H_star - TinK * log(TinK / T_S_star))) / (R * TinK)))
  
  return(rate)
}


temperature = seq(0,100,.1)
plot(temperature, modifiedMasterReaction(temperature, par = c(48.6, 49.7, 388, 1)), type = 'l', ylab = 'rate', main = "modifiedMasterReaction", 
     ylim = c(0,1000))
lines(temperature, modifiedMasterReaction(temperature, par = c(73.3, 59.9, 422, 1)), col = 2)
lines(temperature, modifiedMasterReaction(temperature, par = c(71.3, 71.4, 180, 1)), col = 3)
lines(temperature, modifiedMasterReaction(temperature, par = c(96.0, 96.9, 101, 1)), col = 4)
legend("bottomright", legend = c('psychrophile', "mesophile", "thermophile", "hyperthermophile"), lty = 1 , col = 1:4)

YanHunt = function(temperature, par){
  # Yan, W. & Hunt, L. A. (1999) An Equation for Modelling the Temperature Response of Plants using only 
  #    the Cardinal Temperatures. Annals of Botany 84, 607–614.

  Topt = par[1]
  Tmax = par[2]

  rate = ((Tmax - temperature) / (Tmax - Topt)) * ((temperature / Topt) ^ (Topt / (Tmax - Topt)))
  rate[rate < 0] = 0
  return(rate)
  } 

temperature = seq(3, 35,.1)
Topt = 22
Tmax = 30
plot(temperature, YanHunt(temperature, par = c(Topt, Tmax)),   type = 'l', ylab = 'rate', main = "YanHunt")

Arrhenius = function(temperature, par){
  # Gillooly, J. F., Brown, J. H., West, G. B., Savage, V. M. & Charnov, E. L. (2001) Effects of Size and Temperature on 
  #   Metabolic Rate. Science 293, 2248–2251.
  # Charnov, E. L. & Gillooly, J. F. (2003) Thermal time: Body size, food quality and the 10°C rule. Evolutionary Ecology Research 5, 43–51.

  Ei = par[1] # average activation energy for the rate-limiting enzyme-catalyzed biochemical reactions of metabolism. ~= 0.6 eV
  tetha0 = par[2]
  T0 = par[3] + 273.15
  kB = 8.62E-5 # Boltzmann's constant in eV K-1 
  
  TinK = temperature + 273.15
  
  rate = tetha0 * exp(Ei * (TinK - T0) / (kB * TinK * T0))
  return(rate)
}

modifiedThorntonLessem = function(temperature, par){
  # Thornton, K. W. & Lessem, A. S. (1978) A temperature algorithm for modifying biological rates. Transactions of the American 
  #    Fisheries Society 107, 284–287.
  
  Tlow01 = par[1]  # =~ Tmin
  Tlow80 = par[2]  # lower bound of tolerance range 
  Thigh80 = par[3] # higher bound of tolerance range 
  Thigh99 = par[4] # =~ Tmax
  
  b = log((1 - .8) / .8)
  a = log((1 - 0.01)/0.01) - b
  
  rateLow  = 1 / (1 + exp(b - a * (temperature - Tlow80)  / (Tlow80 - Tlow01)))
  rateHigh = 1 / (1 + exp(b + a * (temperature - Thigh80) / (Thigh99 - Thigh80)))
  return(rateLow + rateHigh - 1)
}

plot(temperature, modifiedThorntonLessem(temperature, par = c(Tmin, 14, 27, Tmax)), type = 'l', ylab = 'rate', main = "modifiedThorntonLessem")
temperature[which.max(modifiedThorntonLessem(temperature, par = c(Tmin, 14, 27, Tmax)))]

lines(temperature, modifiedLehman(temperature,  par = c(Tmin, 21.3, Tmax)), col = 'red')
