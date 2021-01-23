/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 24.12.2016
 */
package net.finmath.montecarlo.interestrate.models.covariance;

/**
 * A base class and interface description for the instantaneous covariance of
 * an forward rate interest rate model.
 *
 * @author Christian Fries
 * @version 1.0
 */
public interface TermStructureCovarianceModel extends TermStructureTenorTimeScaling, TermStructureFactorLoadingsModel {
}
