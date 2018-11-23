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
		if(basisFunctions.length == 0) {
			return new double[] { };
		}
		else if(basisFunctions.length == 1) {
			/*
			 * Regression with one basis function is just a projection on that vector. <b,x>/<b,b>
			 */
			return new double[] { value.mult(basisFunctions[0]).getAverage() / basisFunctions[0].squared().getAverage() };
		}
		else if(basisFunctions.length == 2) {
			/*
			 * Regression with two basis functions can be solved explicitly if determinant != 0 (otherwise we will fallback to SVD)
			 */
			double a = basisFunctions[0].squared().getAverage();
			double b = basisFunctions[0].mult(basisFunctions[1]).average().squared().doubleValue();
			double c = b;
			double d = basisFunctions[1].squared().getAverage();

			double determinant =  (a * d - b * c);
			if(determinant != 0) {
				double x = value.mult(basisFunctions[0]).getAverage();
				double y = value.mult(basisFunctions[1]).getAverage();

				double alpha0 = (d * x - b * y) / determinant;
				double alpha1 = (a * y - c * x) / determinant;

				return new double[] { alpha0, alpha1 };
			}
		}

		/*
		 * General case
		 */

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
