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
 */
public class SABRModel {

	/**
	 * 
	 */
	private SABRModel() {
		// TODO Auto-generated constructor stub
	}

	public static double[] sabrCalibrateParameterForImpliedNormalVols(final double underlying, final double maturity, final double[] givenStrikes, final double[] givenVolatilities) throws SolverException {
		double[] parameterLowerBound = { 0.0, 0.0, 0.0, 0.0, Double.NEGATIVE_INFINITY};
		double[] parameterUpperBound = {Double.POSITIVE_INFINITY, 1.0, 1.0, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};

		return sabrCalibrateParameterForImpliedNormalVols(underlying, maturity, givenStrikes, givenVolatilities, parameterLowerBound, parameterUpperBound);
	}

	public static double[] sabrCalibrateParameterForImpliedNormalVols(final double underlying, final double maturity, final double[] givenStrikes, final double[] givenVolatilities, final double[] parameterLowerBound, final double[] parameterUpperBound) throws SolverException {
		double alpha = 0.006;
		double beta = 0.05;
		double rho = 0.0;
		double nu = 0.075;
		double displacement = 0.02;

		double[] parameterInitialValues = { alpha, beta, rho, nu, displacement };

		double[] parameterSteps = { 0.5/100.0/100.0, 1.0/100.0, 0.5/100.0, 0.5/100.0, 0.1/100.0 };
		return sabrCalibrateParameterForImpliedNormalVols(underlying, maturity, givenStrikes, givenVolatilities, parameterInitialValues, parameterSteps, parameterLowerBound, parameterUpperBound);
	}

	public static double[] sabrCalibrateParameterForImpliedNormalVols(final double underlying, final double maturity, final double[] givenStrikes, final double[] givenVolatilities, final double[] parameterInitialValues, double[] parameterSteps, final double[] parameterLowerBound, final double[] parameterUpperBound) throws SolverException {
		/*
		 * Using Levenberg Marquardt to calibrate SABR
		 */

		double[] targetValues = givenVolatilities;
		int maxIteration = 1000;
		int numberOfThreads = 8;

		LevenbergMarquardt lm = new LevenbergMarquardt(parameterInitialValues, targetValues, maxIteration, numberOfThreads) {			
			private static final long serialVersionUID = -4481118838855868864L;

			@Override
			public void setValues(double[] parameters, double[] values) throws SolverException {
				for(int parameterIndex = 0; parameterIndex<parameters.length; parameterIndex++) {
					parameters[parameterIndex] = Math.min(Math.max(parameters[parameterIndex],parameterLowerBound[parameterIndex]),parameterUpperBound[parameterIndex]);
				}
				for(int strikeIndex = 0; strikeIndex < givenStrikes.length; strikeIndex++) {
					double strike = givenStrikes[strikeIndex];
					values[strikeIndex] = AnalyticFormulas.sabrBerestyckiNormalVolatilityApproximation(parameters[0] /* alpha */, parameters[1] /* beta */, parameters[2] /* rho */, parameters[3] /* nu */, parameters[4] /* displacement */, underlying, strike, maturity);
				}
			}
		};
		lm.setErrorTolerance(1E-16);

		lm.run();

		double[] bestParameters = lm.getBestFitParameters();

		return bestParameters;
	}
}
