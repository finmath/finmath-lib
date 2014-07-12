/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 23.03.2014
 */

package net.finmath.integration;

import java.util.function.DoubleUnaryOperator;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.integration.RombergIntegrator;

/**
 * Implements a Romberg integrator.
 * 
 * The class is actually a wrapper to the Romberg integrator in commons-math.
 * 
 * @author Christian Fries
 */
public class RombergRealIntegration extends AbstractRealIntegral {

	private int numberOfEvaluationPoints;
	
	/**
	 * Create a Romberg integrator.
	 * 
	 * @param lowerBound Lower bound of the integral.
	 * @param upperBound Upper bound of the integral.
	 * @param numberOfEvaluationPoints Maximum number of evaluation points to be used.
	 */
	public RombergRealIntegration(double lowerBound, double upperBound, int numberOfEvaluationPoints) {
		super(lowerBound, upperBound);
		this.numberOfEvaluationPoints = numberOfEvaluationPoints;
	}

	/* (non-Javadoc)
	 * @see net.finmath.integration.AbstractRealIntegral#integrate(java.util.function.DoubleUnaryOperator)
	 */
	@Override
	public double integrate(DoubleUnaryOperator integrand) {
		
		// We use commons-math RombergIntegrator
		return new RombergIntegrator().integrate(numberOfEvaluationPoints, new UnivariateFunction() {
			public double value(double argument) {
				return integrand.applyAsDouble(argument);
			}
		}, getLowerBound(), getUpperBound());
	}

}
