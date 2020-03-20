/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 15.07.2012
 */


package net.finmath.timeseries.models.parametric;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.optim.SimplePointChecker;
import org.apache.commons.math3.random.MersenneTwister;

import net.finmath.optimizer.LevenbergMarquardt;
import net.finmath.optimizer.Optimizer;
import net.finmath.optimizer.SolverException;
import net.finmath.timeseries.HistoricalSimulationModel;
import net.finmath.timeseries.TimeSeries;
import net.finmath.timeseries.TimeSeriesModelParametric;
import net.finmath.timeseries.TimeSeriesView;

/**
 * Displaced log-normal process with GJR-GARCH(1,1) volatility.
 *
 * This class estimate the process
 * \[
 *   \mathrm{d} \log(X + a) = \frac{\sigma}{b + a} \mathrm{d}W(t)
 * \]
 * where \( a &gt; -min(X(t_{i}) \) and thus \( X+a &gt; 0 \) and \( b = 1 - -min(X(t_{i}) \) \) and
 * \( \sigma \) is given by a GJR-GARCH(1,1) process.
 *
 * The choice of b ensures that b+a &ge; 1.
 * For a=0 we have a log-normal process with volatility &sigma;/(b + a).
 * For a=infinity we have a normal process with volatility &sigma;.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class DisplacedLognormalGJRGARCH implements TimeSeriesModelParametric, HistoricalSimulationModel {

	private final TimeSeries timeSeries;

	private final double lowerBoundDisplacement;
	private double upperBoundDisplacement = 10000000;

	private final int maxIterations = 10000000;

	/*
	 * Model properties
	 */
	private final String[] parameterNames	= new String[] { "omega", "alpha", "beta", "mu", "gamma", "displacement" };
	private final double[] parameterGuess	= new double[] { 0.10, 0.2, 0.2, 0.0, 0.0, 10.0 };
	private final double[] parameterStep	= new double[] { 0.01, 0.1, 0.1, 0.1, 0.1,  1.0 };
	private final double[] lowerBound;
	private final double[] upperBound;

	public DisplacedLognormalGJRGARCH(final TimeSeries timeSeries) {
		this(timeSeries, -Double.MAX_VALUE);
	}

	public DisplacedLognormalGJRGARCH(final TimeSeries timeSeries, final double lowerBoundDisplacement) {
		this.timeSeries = timeSeries;

		double valuesMin = Double.MAX_VALUE;
		for(final double value : timeSeries.getValues()) {
			valuesMin = Math.min(value, valuesMin);
		}
		this.lowerBoundDisplacement = Math.max(-valuesMin+1,lowerBoundDisplacement);

		lowerBound = new double[] { 0, 							0, 0, 							0, 							0, this.lowerBoundDisplacement };
		upperBound = new double[] { Double.POSITIVE_INFINITY,	1, 1,	 Double.POSITIVE_INFINITY, 	 Double.POSITIVE_INFINITY, upperBoundDisplacement };
	}

	public DisplacedLognormalGJRGARCH(final TimeSeries timeSeries, final double lowerBoundDisplacement, final double upperBoundDisplacement) {
		this.timeSeries = timeSeries;

		double valuesMin = Double.MAX_VALUE;
		for(final double value : timeSeries.getValues()) {
			valuesMin = Math.min(value, valuesMin);
		}
		this.lowerBoundDisplacement = Math.max(-valuesMin+1,lowerBoundDisplacement);
		this.upperBoundDisplacement = Math.max(this.lowerBoundDisplacement,upperBoundDisplacement);

		lowerBound = new double[] { 0, 							0, 0, 							0, 							0, this.lowerBoundDisplacement };
		upperBound = new double[] { Double.POSITIVE_INFINITY,	1, 1,	 Double.POSITIVE_INFINITY, 	 Double.POSITIVE_INFINITY, this.upperBoundDisplacement };
	}

	public double getLogLikelihoodForParameters(final double[] parameters)
	{
		final double omega		= parameters[0];
		final double alpha		= parameters[1];
		final double beta			= parameters[2];
		final double mu			= parameters[3];
		final double gamma		= parameters[4];
		final double displacement	= parameters[5];

		double logLikelihood = 0.0;

		final double volScaling	= (1+Math.abs(displacement));
		double evalPrev		= 0.0;
		double eval			= volScaling * (Math.log((timeSeries.getValue(1)+displacement)/(timeSeries.getValue(0)+displacement)));
		double h			= omega / (1.0 - alpha - beta);
		double m			= 0.0; // xxx how to init?

		final int length = timeSeries.getNumberOfTimePoints();

		for (int i = 1; i < length-1; i++) {
			m = eval;
			h = (omega + (alpha + gamma * (m < mu ? 1.0 : 0.0)) * m * m) + beta * h;

			final double value1 = timeSeries.getValue(i);
			final double value2 = timeSeries.getValue(i+1);

			final double evalNext	= volScaling * (Math.log((value2+displacement)/(value1+displacement)));
			final double mNext =  evalNext;
			logLikelihood += - Math.log(h) - 2 * Math.log((value2+displacement)/volScaling) - mNext* mNext / h;

			evalPrev = eval;
			eval = evalNext;
		}
		logLikelihood += - Math.log(2 * Math.PI) * (length);
		logLikelihood *= 0.5;

		return logLikelihood;
	}

	public double getLastResidualForParameters(final double[] parameters) {
		final double omega		= parameters[0];
		final double alpha		= parameters[1];
		final double beta			= parameters[2];
		final double mu			= parameters[3];
		final double gamma		= parameters[4];
		final double displacement	= parameters[5];

		double evalPrev		= 0.0;
		final double volScaling	= (1+Math.abs(displacement));
		double h			= omega / (1.0 - alpha - beta);
		double m			= 0.0; // xxx how to init?

		final int length = timeSeries.getNumberOfTimePoints();
		for (int i = 1; i < length-1; i++) {
			final double eval	= volScaling * (Math.log((timeSeries.getValue(i)+displacement)/(timeSeries.getValue(i-1)+displacement)));

			m = eval;
			h = (omega + (alpha + gamma * (m < mu ? 1.0 : 0.0)) * m * m) + beta * h;

			evalPrev = eval;
		}

		return h;
	}

	public double[] getSzenarios(final double[] parameters) {
		final double omega		= parameters[0];
		final double alpha		= parameters[1];
		final double beta			= parameters[2];
		final double mu			= parameters[3];
		final double gamma		= parameters[4];
		final double displacement	= parameters[5];

		final double[] szenarios = new double[timeSeries.getNumberOfTimePoints()-1];

		final double volScaling	= (1+Math.abs(displacement));
		double evalPrev		= 0.0;
		double h			= omega / (1.0 - alpha - beta);
		double m			= 0.0;
		double vol = Math.sqrt(h) / volScaling;
		for (int i = 1; i <= timeSeries.getNumberOfTimePoints()-1; i++) {
			final double y = Math.log((timeSeries.getValue(i)+displacement)/(timeSeries.getValue(i-1)+displacement));

			final double eval	= volScaling * y;
			m = eval;

			szenarios[i-1]	= m / vol / volScaling;

			h = (omega + (alpha + gamma * (m < mu ? 1.0 : 0.0)) * m * m) + beta * h;
			vol = Math.sqrt(h) / volScaling;
			evalPrev = eval;
		}
		java.util.Arrays.sort(szenarios);

		return szenarios;
	}

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
			public double value(final double[] parameters) {

				final double omega		= parameters[0];
				final double alpha		= parameters[1];
				final double beta			= parameters[2];
				final double mu			= parameters[3];
				final double gamma		= parameters[4];
				final double displacement	= parameters[5];

				double logLikelihood = getLogLikelihoodForParameters(parameters);

				// Penalty to prevent solver from hitting the bounds
				logLikelihood -= Math.max(1E-30-omega,0)/1E-30;
				logLikelihood -= Math.max(1E-30-alpha,0)/1E-30;
				logLikelihood -= Math.max((alpha-1)+1E-30,0)/1E-30;
				logLikelihood -= Math.max(1E-30-beta,0)/1E-30;
				logLikelihood -= Math.max((beta-1)+1E-30,0)/1E-30;

				return logLikelihood;
			}

		}
		final GARCHMaxLikelihoodFunction objectiveFunction = new GARCHMaxLikelihoodFunction();

		// Create a guess for the solver
		final double[] guessParameters = new double[6];
		System.arraycopy(parameterGuess, 0, guessParameters, 0, parameterGuess.length);

		if(guess != null) {
			// A guess was provided, use that one
			guessParameters[0]	= (Double)guess.get("Omega");
			guessParameters[1]	= (Double)guess.get("Alpha");
			guessParameters[2]	= (Double)guess.get("Beta");
			guessParameters[3]	= (Double)guess.get("Mu");
			guessParameters[4]	= (Double)guess.get("Gamme");
			guessParameters[5]	= (Double)guess.get("Displacement");
		}


		// Seek optimal parameter configuration
		final Optimizer lm = new LevenbergMarquardt(guessParameters, new double[] { 1000.0 }, maxIterations*100, 2) {
			private static final long serialVersionUID = -3791313169935939272L;

			@Override
			public void setValues(final double[] arg0, final double[] arg1) {
				arg1[0] = objectiveFunction.value(arg0);
			}
		};

		double[] bestParameters = null;

		final boolean isUseLM = false;

		if(isUseLM) {
			try {
				lm.run();
			} catch (final SolverException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			bestParameters = lm.getBestFitParameters();
		}
		else {
			final org.apache.commons.math3.optim.nonlinear.scalar.noderiv.CMAESOptimizer optimizer2 = new org.apache.commons.math3.optim.nonlinear.scalar.noderiv.CMAESOptimizer(maxIterations, Double.POSITIVE_INFINITY, true, 0, 0, new MersenneTwister(), false, new SimplePointChecker<org.apache.commons.math3.optim.PointValuePair>(0, 0))
			{
				@Override
				public double computeObjectiveValue(final double[] params) {
					return objectiveFunction.value(params);
				}

				/* (non-Javadoc)
				 * @see org.apache.commons.math3.optim.nonlinear.scalar.MultivariateOptimizer#getGoalType()
				 */
				@Override
				public org.apache.commons.math3.optim.nonlinear.scalar.GoalType getGoalType() {
					// TODO Auto-generated method stub
					return org.apache.commons.math3.optim.nonlinear.scalar.GoalType.MAXIMIZE;
				}

				/* (non-Javadoc)
				 * @see org.apache.commons.math3.optim.BaseMultivariateOptimizer#getStartPoint()
				 */
				@Override
				public double[] getStartPoint() {
					return guessParameters;
				}

				/* (non-Javadoc)
				 * @see org.apache.commons.math3.optim.BaseMultivariateOptimizer#getLowerBound()
				 */
				@Override
				public double[] getLowerBound() {
					return lowerBound;
				}

				/* (non-Javadoc)
				 * @see org.apache.commons.math3.optim.BaseMultivariateOptimizer#getUpperBound()
				 */
				@Override
				public double[] getUpperBound() {
					return upperBound;
				}
			};

			try {
				final org.apache.commons.math3.optim.PointValuePair result = optimizer2.optimize(
						new org.apache.commons.math3.optim.nonlinear.scalar.noderiv.CMAESOptimizer.PopulationSize((int) (4 + 3 * Math.log(guessParameters.length))),
						new org.apache.commons.math3.optim.nonlinear.scalar.noderiv.CMAESOptimizer.Sigma(parameterStep)
						);
				bestParameters = result.getPoint();
			} catch(final org.apache.commons.math3.exception.MathIllegalStateException e) {
				System.out.println("Solver failed");
				bestParameters = guessParameters;
			}
		}

		// Transform parameters to GARCH parameters
		final double omega		= bestParameters[0];
		final double alpha		= bestParameters[1];
		final double beta			= bestParameters[2];
		final double mu			= bestParameters[3];
		final double gamma		= bestParameters[4];
		final double displacement	= bestParameters[5];

		final Map<String, Object> results = new HashMap<>();
		results.put("parameters", bestParameters);
		results.put("Omega", omega);
		results.put("Alpha", alpha);
		results.put("Beta", beta);
		results.put("Mu", mu);
		results.put("gamma", gamma);
		results.put("Displacement", displacement);
		results.put("Szenarios", this.getSzenarios(bestParameters));
		results.put("Likelihood", this.getLogLikelihoodForParameters(bestParameters));
		results.put("Vol", Math.sqrt(this.getLastResidualForParameters(bestParameters)));
		System.out.println(results.get("Likelihood") + "\t" + Arrays.toString(bestParameters));
		return results;
	}

	private static double restrictToOpenSet(double value, final double lowerBond, final double upperBound) {
		value = Math.max(value, lowerBond  * (1.0+Math.signum(lowerBond)*1E-15) + 1E-15);
		value = Math.min(value, upperBound * (1.0-Math.signum(upperBound)*1E-15) - 1E-15);
		return value;
	}

	@Override
	public TimeSeriesModelParametric getCloneCalibrated(final TimeSeries timeSeries) {
		return new DisplacedLognormalGJRGARCH(timeSeries);
	}

	@Override
	public HistoricalSimulationModel getCloneWithWindow(final int windowIndexStart, final int windowIndexEnd) {
		return new DisplacedLognormalGJRGARCH(new TimeSeriesView(timeSeries, windowIndexStart, windowIndexEnd));
	}

	@Override
	public double[] getParameters() {
		return (double[])getBestParameters().get("parameters");
	}

	@Override
	public String[] getParameterNames() {
		return parameterNames;
	}
}
