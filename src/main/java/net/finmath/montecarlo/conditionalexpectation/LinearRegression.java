/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 26.08.2018
 */
package net.finmath.montecarlo.conditionalexpectation;

import net.finmath.functions.LinearAlgebra;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * Performs a linear regression on random variables implementing RandomVariableInterface.
 *
 * @author Christian Fries
 */
public class LinearRegression {

	private final RandomVariableInterface[] basisFunctions;

	/**
	 * Create the linear regression with a set of basis functions.
	 *
	 * @param basisFunctions A vector of (independent) random variables to be used as basis functions.
	 */
	public LinearRegression(RandomVariableInterface[] basisFunctions) {
		super();
		this.basisFunctions = basisFunctions;
	}

	/**
	 * Get the vector of regression coefficients.
	 *
	 * @param value The random variable to regress.
	 * @return The vector of regression coefficients.
	 */
	public double[] getRegressionCoefficients(RandomVariableInterface value) {
		// Build regression matrix
		double[][] BTB = new double[basisFunctions.length][basisFunctions.length];
		for(int i=0; i<basisFunctions.length; i++) {
			for(int j=0; j<=i; j++) {
				double covariance = basisFunctions[i].mult(basisFunctions[j]).getAverage();
				BTB[i][j] = covariance;
				BTB[j][i] = covariance;
			}
		}

		double[] BTX = new double[basisFunctions.length];
		for(int i=0; i<basisFunctions.length; i++) {
			double covariance = basisFunctions[i].mult(value).getAverage();
			BTX[i] = covariance;
		}

		return LinearAlgebra.solveLinearEquationLeastSquare(BTB, BTX);
	}
}
