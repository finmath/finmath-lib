/**
 * Contains covariance models and their calibration as plug-ins for the LIBOR market model and volatility and correlation models which may be used to build a covariance model.
 * Covariance models provide they free parameters via an interface. The class AbstractLIBORCovarianceModelParametric provides a method that implements the generic calibration of the models.
 *
 * NOTE: Classes in this package are almost identical to the package net.finmath.montecarlo.interestrate.modelplugins except that the parameter may be a RandomVariable.
 * The two packages will be merged in a future version.
 *
 * @author Christian Fries
 */
package net.finmath.montecarlo.interestrate.covariancemodels;
