/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
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
 * @version 1.0
 */
public class RombergRealIntegration extends AbstractRealIntegral {

	private final int numberOfEvaluationPoints;

	/**
	 * Create a Romberg integrator.
	 *
	 * @param lowerBound Lower bound of the integral.
	 * @param upperBound Upper bound of the integral.
	 * @param numberOfEvaluationPoints Maximum number of evaluation points to be used.
	 */
	public RombergRealIntegration(final double lowerBound, final double upperBound, final int numberOfEvaluationPoints) {
		super(lowerBound, upperBound);
		this.numberOfEvaluationPoints = numberOfEvaluationPoints;
	}

	@Override
	public double integrate(final DoubleUnaryOperator integrand) {

		// We use commons-math RombergIntegrator
		return new RombergIntegrator().integrate(numberOfEvaluationPoints, new UnivariateFunction() {
			@Override
			public double value(final double argument) {
				return integrand.applyAsDouble(argument);
			}
		}, getLowerBound(), getUpperBound());
	}

}
