/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 02.10.2016
 */

package net.finmath.functions;

import net.finmath.optimizer.LevenbergMarquardt;
import net.finmath.optimizer.SolverException;

/**
 * @author Christian Fries
 *
 * @version 1.0
 */
public class SABRModel {

	/**
	 *
	 */
	private SABRModel() {
		// TODO Auto-generated constructor stub
	}

	public static double[] sabrCalibrateParameterForImpliedNormalVols(final double underlying, final double maturity, final double[] givenStrikes, final double[] givenVolatilities) throws SolverException {
		final double[] parameterLowerBound = { 0.0, 0.0, 0.0, 0.0, Double.NEGATIVE_INFINITY};
		final double[] parameterUpperBound = {Double.POSITIVE_INFINITY, 1.0, 1.0, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};

		return sabrCalibrateParameterForImpliedNormalVols(underlying, maturity, givenStrikes, givenVolatilities, parameterLowerBound, parameterUpperBound);
	}

	public static double[] sabrCalibrateParameterForImpliedNormalVols(final double underlying, final double maturity, final double[] givenStrikes, final double[] givenVolatilities, final double[] parameterLowerBound, final double[] parameterUpperBound) throws SolverException {
		final double alpha = 0.006;
		final double beta = 0.05;
		final double rho = 0.0;
		final double nu = 0.075;
		final double displacement = 0.02;

		final double[] parameterInitialValues = { alpha, beta, rho, nu, displacement };

		final double[] parameterSteps = { 0.5/100.0/100.0, 1.0/100.0, 0.5/100.0, 0.5/100.0, 0.1/100.0 };
		return sabrCalibrateParameterForImpliedNormalVols(underlying, maturity, givenStrikes, givenVolatilities, parameterInitialValues, parameterSteps, parameterLowerBound, parameterUpperBound);
	}

	public static double[] sabrCalibrateParameterForImpliedNormalVols(final double underlying, final double maturity, final double[] givenStrikes, final double[] givenVolatilities, final double[] parameterInitialValues, final double[] parameterSteps, final double[] parameterLowerBound, final double[] parameterUpperBound) throws SolverException {
		/*
		 * Using Levenberg Marquardt to calibrate SABR
		 */

		final double[] targetValues = givenVolatilities;
		final int maxIteration = 1000;
		final int numberOfThreads = 8;

		final LevenbergMarquardt lm = new LevenbergMarquardt(parameterInitialValues, targetValues, maxIteration, numberOfThreads) {
			private static final long serialVersionUID = -4481118838855868864L;

			@Override
			public void setValues(final double[] parameters, final double[] values) {
				for(int parameterIndex = 0; parameterIndex<parameters.length; parameterIndex++) {
					parameters[parameterIndex] = Math.min(Math.max(parameters[parameterIndex],parameterLowerBound[parameterIndex]),parameterUpperBound[parameterIndex]);
				}
				for(int strikeIndex = 0; strikeIndex < givenStrikes.length; strikeIndex++) {
					final double strike = givenStrikes[strikeIndex];
					values[strikeIndex] = AnalyticFormulas.sabrBerestyckiNormalVolatilityApproximation(parameters[0] /* alpha */, parameters[1] /* beta */, parameters[2] /* rho */, parameters[3] /* nu */, parameters[4] /* displacement */, underlying, strike, maturity);
				}
			}
		};
		lm.setErrorTolerance(1E-16);

		lm.run();

		final double[] bestParameters = lm.getBestFitParameters();

		return bestParameters;
	}
}
