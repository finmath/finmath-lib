/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 15.07.2012
 */


package net.finmath.timeseries.models.parametric;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;

import net.finmath.timeseries.HistoricalSimulationModel;

/**
 * Displaced log-normal process with constanst volatility.
 *
 * This class estimate the process
 * \[
 *   \mathrm{d} \log(X + a) = \frac{\sigma}{b + a} \mathrm{d}W(t)
 * \]
 * where \( a &gt; -min(X(t_{i}) \) and thus \( X+a &gt; 0 \) and \( b = 1 - -min(X(t_{i}) \) \) and
 * \( \sigma \) is a constant.
 *
 * The choice of b ensures that b+a &ge; 1.
 * For a=0 we have a log-normal process with volatility &sigma;/(b + a).
 * For a=infinity we have a normal process with volatility &sigma;.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class DisplacedLognormal implements HistoricalSimulationModel {

	private final double[] values;
	private final double lowerBoundDisplacement;
	private final double upperBoundDisplacement = 10000000;
	private final int windowIndexStart;
	private final int windowIndexEnd;
	private final int maxIterations = 1000000;

	public DisplacedLognormal(final double[] values) {
		this.values = values;
		windowIndexStart	= 0;
		windowIndexEnd		= values.length-1;

		double valuesMin = Double.MAX_VALUE;
		for (int i = windowIndexStart; i <= windowIndexEnd; i++) {
			valuesMin = Math.min(values[i], valuesMin);
		}
		lowerBoundDisplacement = -valuesMin+1;

	}

	public DisplacedLognormal(final double[] values, final double lowerBoundDisplacement) {
		this.values = values;
		windowIndexStart	= 0;
		windowIndexEnd		= values.length-1;

		double valuesMin = Double.MAX_VALUE;
		for (int i = windowIndexStart; i <= windowIndexEnd; i++) {
			valuesMin = Math.min(values[i], valuesMin);
		}
		this.lowerBoundDisplacement = Math.max(-valuesMin+1,lowerBoundDisplacement);

	}

	public DisplacedLognormal(final double[] values, final int windowIndexStart, final int windowIndexEnd) {
		this.values = values;
		this.windowIndexStart	= windowIndexStart;
		this.windowIndexEnd		= windowIndexEnd;

		double valuesMin = Double.MAX_VALUE;
		for (int i = windowIndexStart; i <= windowIndexEnd; i++) {
			valuesMin = Math.min(values[i], valuesMin);
		}
		lowerBoundDisplacement = -valuesMin+1;
	}

	public DisplacedLognormal(final double[] values, final double lowerBoundDisplacement, final int windowIndexStart, final int windowIndexEnd) {
		this.values = values;
		this.windowIndexStart	= windowIndexStart;
		this.windowIndexEnd		= windowIndexEnd;

		double valuesMin = Double.MAX_VALUE;
		for (int i = windowIndexStart; i <= windowIndexEnd; i++) {
			valuesMin = Math.min(values[i], valuesMin);
		}
		this.lowerBoundDisplacement = Math.max(-valuesMin+1,lowerBoundDisplacement);
	}

	@Override
	public HistoricalSimulationModel getCloneWithWindow(final int windowIndexStart, final int windowIndexEnd) {
		return new DisplacedLognormal(values, windowIndexStart, windowIndexEnd);
	}

	public HistoricalSimulationModel getCloneWithWindow(final double lowerBoundDisplacement, final int windowIndexStart, final int windowIndexEnd) {
		return new DisplacedLognormal(values, lowerBoundDisplacement, windowIndexStart, windowIndexEnd);
	}

	public double getLogLikelihoodForParameters(final double omega, final double alpha, final double beta, final double displacement)
	{
		double logLikelihood = 0.0;

		final double volScaling	= (1+Math.abs(displacement));

		double volSquaredEstimate = 0.0;
		for (int i = windowIndexStart+1; i <= windowIndexEnd-1; i++) {
			final double eval	= volScaling * (Math.log((values[i]+displacement)/(values[i-1]+displacement)));
			volSquaredEstimate += eval*eval;
		}
		volSquaredEstimate /= windowIndexEnd-windowIndexStart;

		double eval			= volScaling * (Math.log((values[windowIndexStart+1]+displacement)/(values[windowIndexStart+1-1]+displacement)));
		for (int i = windowIndexStart+1; i <= windowIndexEnd-1; i++) {
			final double evalNext	= volScaling * (Math.log((values[i+1]+displacement)/(values[i]+displacement)));

			final double volSquared = volSquaredEstimate / volScaling * volScaling;		// h = (sigma*)^2, volSquared = (sigma^a)^2

			logLikelihood += - Math.log(volSquaredEstimate) - 2 * Math.log((values[i+1]+displacement)/volScaling) - evalNext*evalNext / volSquaredEstimate;
			eval = evalNext;
		}
		logLikelihood += - Math.log(2 * Math.PI) * (windowIndexEnd-windowIndexStart);
		logLikelihood *= 0.5;

		return logLikelihood;
	}

	public double getLastResidualForParameters(final double omega, final double alpha, final double beta, final double displacement) {
		final double volScaling = (1+Math.abs(displacement));
		double h = omega / (1.0 - alpha - beta);
		for (int i = windowIndexStart+1; i <= windowIndexEnd; i++) {
			final double eval	= volScaling * (Math.log((values[i]+displacement)/(values[i-1]+displacement)));
			//			double eval	= volScaling * (values[i]-values[i-1])/(values[i-1]+displacement);
			h = omega + alpha * eval * eval + beta * h;
		}

		return h;
	}

	public double[] getQuantilPredictionsForParameters(final double omega, final double alpha, final double beta, final double displacement, final double[] quantiles) {
		final double[] szenarios = new double[windowIndexEnd-windowIndexStart+1-1];

		final double volScaling = (1+Math.abs(displacement));

		double volSquaredEstimate = 0.0;
		for (int i = windowIndexStart+1; i <= windowIndexEnd-1; i++) {
			final double eval	= volScaling * (Math.log((values[i]+displacement)/(values[i-1]+displacement)));
			volSquaredEstimate += eval*eval;
		}
		volSquaredEstimate /= windowIndexEnd-windowIndexStart;

		double vol = Math.sqrt(volSquaredEstimate) / volScaling;
		for (int i = windowIndexStart+1; i <= windowIndexEnd; i++) {
			final double y = Math.log((values[i]+displacement)/(values[i-1]+displacement));
			//			double y = (values[i]-values[i-1])/(values[i-1]+displacement);
			szenarios[i-windowIndexStart-1]	= y / vol;

			final double eval		= volScaling * y;
			vol = Math.sqrt(volSquaredEstimate) / volScaling;
		}
		java.util.Arrays.sort(szenarios);

		final double[] quantileValues = new double[quantiles.length];
		for(int i=0; i<quantiles.length; i++) {
			final double quantile = quantiles[i];
			final double quantileIndex = szenarios.length * quantile  - 1;
			final int quantileIndexLo = (int)quantileIndex;
			final int quantileIndexHi = quantileIndexLo+1;

			final double szenarioRelativeChange =
					(quantileIndexHi-quantileIndex) * Math.exp(szenarios[Math.max(quantileIndexLo,0               )] * vol)
					+ (quantileIndex-quantileIndexLo) * Math.exp(szenarios[Math.min(quantileIndexHi,szenarios.length)] * vol);
			/*
			double szenarioRelativeChange =
					(quantileIndexHi-quantileIndex) * (1 + szenarios[Math.max(quantileIndexLo,0               )] * vol)
					+ (quantileIndex-quantileIndexLo) * (1 + szenarios[Math.min(quantileIndexHi,szenarios.length)] * vol);
			 */

			final double quantileValue = (values[windowIndexEnd]+displacement) * szenarioRelativeChange - displacement;
			quantileValues[i] = quantileValue;
		}

		return quantileValues;
	}

	/* (non-Javadoc)
	 * @see net.finmath.timeseries.HistoricalSimulationModel#getBestParameters()
	 */
	@Override
	public Map<String, Object> getBestParameters() {
		return getBestParameters(null);
	}

	/* (non-Javadoc)
	 * @see net.finmath.timeseries.HistoricalSimulationModel#getBestParameters(java.util.Map)
	 */
	@Override
	public Map<String, Object> getBestParameters(final Map<String, Object> guess) {

		// Create the objective function for the solver
		class GARCHMaxLikelihoodFunction implements MultivariateFunction, Serializable {

			private static final long serialVersionUID = 7072187082052755854L;

			@Override
			public double value(final double[] variables) {
				/*
				 * Transform variables: The solver variables are in (-\infty, \infty).
				 * We transform the variable to the admissible domain for GARCH, that is
				 * omega > 0, 0 < alpha < 1, 0 < beta < (1-alpha), displacement > lowerBoundDisplacement  ??????
				 * ???? usually for GARCH the restrictions are written like omega > 0, alpha > 0, beta > 0, and alpha + beta < 1
				 */
				final double omega	= Math.exp(variables[0]);
				final double mucorr	= Math.exp(-Math.exp(-variables[1]));
				final double muema	= Math.exp(-Math.exp(-variables[2]));
				final double beta		= mucorr * muema;
				final double alpha	= mucorr - beta;
				//				double alpha = 1.0/(1.0+Math.exp(-variables[1]));
				//				double beta = (1.0-alpha)*1.0/(1.0+Math.exp(-variables[2]));
				final double displacementNormed = 1.0/(1.0+Math.exp(-variables[3]));
				final double displacement = (upperBoundDisplacement-lowerBoundDisplacement)*displacementNormed+lowerBoundDisplacement;

				double logLikelihood = getLogLikelihoodForParameters(omega,alpha,beta,displacement);

				// Penalty to prevent solver from hitting the bounds
				logLikelihood -= Math.max(1E-30-omega,0)/1E-30;
				logLikelihood -= Math.max(1E-30-alpha,0)/1E-30;
				logLikelihood -= Math.max((alpha-1)+1E-30,0)/1E-30;
				logLikelihood -= Math.max(1E-30-beta,0)/1E-30;
				logLikelihood -= Math.max((beta-1)+1E-30,0)/1E-30;
				logLikelihood -= Math.max(1E-30-displacementNormed,0)/1E-30;
				logLikelihood -= Math.max((displacementNormed-1)+1E-30,0)/1E-30;

				return logLikelihood;
			}

		}
		final GARCHMaxLikelihoodFunction objectiveFunction = new GARCHMaxLikelihoodFunction();

		// Create a guess for the solver
		double guessOmega = 1.0;
		double guessAlpha = 0.2;
		double guessBeta = 0.2;
		double guessDisplacement = (lowerBoundDisplacement + upperBoundDisplacement) / 2.0;
		if(guess != null) {
			// A guess was provided, use that one
			guessOmega			= (Double)guess.get("Omega");
			guessAlpha			= (Double)guess.get("Alpha");
			guessBeta			= (Double)guess.get("Beta");
			guessDisplacement	= (Double)guess.get("Displacement");
		}

		// Constrain guess to admissible range
		guessOmega			= restrictToOpenSet(guessOmega, 0.0, Double.MAX_VALUE);
		guessAlpha			= restrictToOpenSet(guessAlpha, 0.0, 1.0);
		guessBeta			= restrictToOpenSet(guessBeta, 0.0, 1.0-guessAlpha);
		guessDisplacement	= restrictToOpenSet(guessDisplacement, lowerBoundDisplacement, upperBoundDisplacement);

		final double guessMucorr	= guessAlpha + guessBeta;
		final double guessMuema	= guessBeta / (guessAlpha+guessBeta);

		// Transform guess to solver coordinates
		final double[] guessParameters = new double[4];
		guessParameters[0] = Math.log(guessOmega);
		guessParameters[1] = -Math.log(-Math.log(guessMucorr));
		guessParameters[2] = -Math.log(-Math.log(guessMuema));
		guessParameters[3] = -Math.log(1.0/((guessDisplacement-lowerBoundDisplacement)/(upperBoundDisplacement-lowerBoundDisplacement))-1.0);

		// Seek optimal parameter configuration
		//		org.apache.commons.math3.optimization.direct.BOBYQAOptimizer optimizer2 = new org.apache.commons.math3.optimization.direct.BOBYQAOptimizer(6);
		final org.apache.commons.math3.optimization.direct.CMAESOptimizer optimizer2 = new org.apache.commons.math3.optimization.direct.CMAESOptimizer();

		double[] bestParameters = null;
		try {
			final PointValuePair result = optimizer2.optimize(
					maxIterations,
					objectiveFunction,
					GoalType.MAXIMIZE,
					guessParameters /* start point */
					);
			bestParameters = result.getPoint();
		} catch(final org.apache.commons.math3.exception.MathIllegalStateException e) {
			// Retry with new guess. This guess corresponds to omaga=1, alpha=0.5; beta=0.25; displacement=1+lowerBoundDisplacement;
			final double[] guessParameters2 = {0.0, 0.0, 0.0, 10.0};
			/*			PointValuePair result = optimizer2.optimize(
			maxIterations,
			objectiveFunction,
			GoalType.MAXIMIZE,
			guessParameters2
			);*/
			System.out.println("Solver failed");
			bestParameters = guessParameters2;//result.getPoint();
		}

		// Transform parameters to GARCH parameters
		final double omega	= Math.exp(bestParameters[0]);
		final double mucorr	= Math.exp(-Math.exp(-bestParameters[1]));
		final double muema	= Math.exp(-Math.exp(-bestParameters[2]));
		final double beta		= mucorr * muema;
		final double alpha	= mucorr - beta;
		final double displacementNormed = 1.0/(1.0+Math.exp(-bestParameters[3]));
		final double displacement = (upperBoundDisplacement-lowerBoundDisplacement)*displacementNormed+lowerBoundDisplacement;

		final double[] quantiles = {0.01, 0.05, 0.5};
		final double[] quantileValues = this.getQuantilPredictionsForParameters(omega, alpha, beta, displacement, quantiles);

		final Map<String, Object> results = new HashMap<>();
		results.put("Omega", omega);
		results.put("Alpha", alpha);
		results.put("Beta", beta);
		results.put("Displacement", displacement);
		results.put("Likelihood", this.getLogLikelihoodForParameters(omega, alpha, beta, displacement));
		results.put("Vol", Math.sqrt(this.getLastResidualForParameters(omega, alpha, beta, displacement)));
		results.put("Quantile=1%", quantileValues[0]);
		results.put("Quantile=5%", quantileValues[1]);
		results.put("Quantile=50%", quantileValues[2]);
		return results;
	}

	private static double restrictToOpenSet(double value, final double lowerBond, final double upperBound) {
		value = Math.max(value, lowerBond  * (1.0+Math.signum(lowerBond)*1E-15) + 1E-15);
		value = Math.min(value, upperBound * (1.0-Math.signum(upperBound)*1E-15) - 1E-15);
		return value;
	}
}
