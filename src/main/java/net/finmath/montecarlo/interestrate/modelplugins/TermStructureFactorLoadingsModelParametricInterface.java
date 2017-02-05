/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 24.12.2016
 */
package net.finmath.montecarlo.interestrate.modelplugins;

import net.finmath.montecarlo.interestrate.TermStructureModelInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * A base class and interface description for the instantaneous covariance of
 * an forward rate interest rate model.
 * 
 * @author Christian Fries
 */
public interface TermStructureFactorLoadingsModelParametricInterface extends TermStructureFactorLoadingsModelInterface {
	/**
	 * Get the parameters of determining this parametric
	 * covariance model. The parameters are usually free parameters
	 * which may be used in calibration.
	 * 
	 * @return Parameter vector.
	 */
	public abstract double[]	getParameter();

	/**
	 * Return an instance of this model using a new set of parameters.
	 * Note: To improve performance it is admissible to return the same instance of the object given that the parameters have not changed. Models should be immutable.
	 * 
	 * @param parameters The new set of parameters.
	 * @return An instance of AbstractLIBORCovarianceModelParametric with modified parameters.
	 */
	public abstract TermStructureCovarianceModelParametric getCloneWithModifiedParameters(double[] parameters);
}
