/*
 * Created on 09.02.2004
 */
package net.finmath.montecarlo.interestrate.simple;

import java.util.Arrays;
import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.BrownianMotionLazyInit;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.interestrate.models.LIBORMarketModelFromCovarianceModel;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCorrelationModel;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCorrelationModelExponentialDecay;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCovarianceModel;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCovarianceModelFromVolatilityAndCorrelation;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORVolatilityModel;
import net.finmath.montecarlo.process.MonteCarloProcess;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;

/**
 * Implements a basic LIBOR market model with a some drift approximation methods.
 *
 * @author Christian Fries
 * @version 0.5
 * @since finmath-lib 4.1.0
 */
public class SimpleLIBORMarketModel extends AbstractLIBORMarketModel {

	public enum Driftapproximation { EULER, LINE_INTEGRAL, PREDICTOR_CORRECTOR }

	public enum Measure { SPOT, TERMINAL }

	private final double[]						liborInitialValues;
	private LIBORCovarianceModel	covarianceModel;

	private Driftapproximation  driftAproximationMethod = Driftapproximation.EULER;
	private Measure             measure = Measure.SPOT;

	/**
	 * Creates a LIBOR Market Model for given covariance.
	 *
	 * @param timeDiscretizationFromArray The time discretization of the process (simulation time).
	 * @param liborPeriodDiscretization The discretization of the interest rate curve into forward rates (tenor structure).
	 * @param numberOfPaths The number of paths.
	 * @param liborInitialValues The initial values for the forward rates.
	 * @param covarianceModel  The covariance model to use.
	 */
	public SimpleLIBORMarketModel(
			final TimeDiscretization	timeDiscretizationFromArray,
			final TimeDiscretization	liborPeriodDiscretization,
			final int                 numberOfPaths,
			final double[]            liborInitialValues,
			final LIBORCovarianceModel covarianceModel
			) {
		super(  liborPeriodDiscretization,
				new BrownianMotionLazyInit(timeDiscretizationFromArray, covarianceModel.getNumberOfFactors(), numberOfPaths, 3141 /* seed */)
				);
		this.liborInitialValues = liborInitialValues;
		this.covarianceModel    = covarianceModel;
	}

	/**
	 * Creates a LIBOR Market Model for given covariance.
	 *
	 * @param liborPeriodDiscretization The discretization of the interest rate curve into forward rates (tenor structure).
	 * @param liborInitialValues The initial values for the forward rates.
	 * @param covarianceModel  The covariance model to use.
	 * @param brownianMotion The brownian driver for the Monte-Carlo simulation.
	 */
	public SimpleLIBORMarketModel(
			final TimeDiscretization		liborPeriodDiscretization,
			final double[]                liborInitialValues,
			final LIBORCovarianceModel covarianceModel,
			final BrownianMotionLazyInit	brownianMotion
			) {
		super(liborPeriodDiscretization, brownianMotion);

		if(covarianceModel.getNumberOfFactors() > brownianMotion.getNumberOfFactors()) {
			throw new RuntimeException("Number of factors in covariance model is larger than number of Brownian drivers.");
		}

		this.liborInitialValues = liborInitialValues;
		this.covarianceModel    = covarianceModel;
	}

	/**
	 * Creates a LIBOR Market Model for given volatility and correlation model.
	 *
	 * @param timeDiscretizationFromArray The time discretization of the process (simulation time).
	 * @param liborPeriodDiscretization The discretization of the interest rate curve into forward rates (tenor structure).
	 * @param numberOfPaths The number of paths.
	 * @param liborInitialValues The initial values for the forward rates.
	 * @param volatilityModel  The volatility model to use.
	 * @param correlationModel The correlation model to use.
	 */
	public SimpleLIBORMarketModel(
			final TimeDiscretization		timeDiscretizationFromArray,
			final TimeDiscretization		liborPeriodDiscretization,
			final int						numberOfPaths,
			final double[]				liborInitialValues,
			final LIBORVolatilityModel	volatilityModel,
			final LIBORCorrelationModel	correlationModel
			) {
		super(  liborPeriodDiscretization,
				new BrownianMotionLazyInit(timeDiscretizationFromArray, correlationModel.getNumberOfFactors(), numberOfPaths, 3141 /* seed */)
				);
		this.liborInitialValues = liborInitialValues;
		covarianceModel    = new LIBORCovarianceModelFromVolatilityAndCorrelation(timeDiscretizationFromArray, liborPeriodDiscretization, volatilityModel, correlationModel);
	}

	/**
	 * Creates a one factor LIBOR Market Model (correlation = 1).
	 *
	 * @param timeDiscretizationFromArray The time discretization of the process (simulation time).
	 * @param liborPeriodDiscretization The discretization of the interest rate curve into forward rates (tenor structure).
	 * @param numberOfPaths The number of paths.
	 * @param liborInitialValues The initial values for the forward rates.
	 * @param volatilityModel  The volatility model to use.
	 */
	public SimpleLIBORMarketModel(
			final TimeDiscretization		timeDiscretizationFromArray,
			final TimeDiscretization		liborPeriodDiscretization,
			final int						numberOfPaths,
			final double[]				liborInitialValues,
			final LIBORVolatilityModel	volatilityModel
			) {
		super(  liborPeriodDiscretization,
				new BrownianMotionLazyInit(timeDiscretizationFromArray, 1 /* numberOfFactors */, numberOfPaths, 3141 /* seed */)
				);
		this.liborInitialValues = liborInitialValues;
		covarianceModel    = new LIBORCovarianceModelFromVolatilityAndCorrelation(timeDiscretizationFromArray, liborPeriodDiscretization, volatilityModel, new LIBORCorrelationModelExponentialDecay(timeDiscretizationFromArray, liborPeriodDiscretization, 1, 0.0, false));
	}

	/* (non-Javadoc)
	 * @see com.spacelike.fdml.monteCarlo.stockOptionPricing.LogNormalProcess#getInitialValue()
	 */
	@Override
	public RandomVariable[] getInitialValue() {
		final RandomVariable[] initialValueRandomVariable = new RandomVariable[getNumberOfComponents()];
		for(int componentIndex=0; componentIndex<getNumberOfComponents(); componentIndex++) {
			initialValueRandomVariable[componentIndex] = new RandomVariableFromDoubleArray(0.0, liborInitialValues[componentIndex]);
		}
		return initialValueRandomVariable;
	}

	/* (non-Javadoc)
	 * @see com.spacelike.fdml.monteCarlo.stockOptionPricing.LogNormalProcess#getInitialValue(int)
	 */
	public RandomVariableFromDoubleArray getInitialValue(final int componentIndex) {
		return new RandomVariableFromDoubleArray(0.0, liborInitialValues[componentIndex]);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.templatemethoddesign.LogNormalProcess#getFactorLoading(int, int, int, net.finmath.stochastic.RandomVariable[])
	 */
	@Override
	public	RandomVariable	getFactorLoading(final int timeIndex, final int factor, final int component, final RandomVariable[] realizationAtTimeIndex)
	{
		return covarianceModel.getFactorLoading(timeIndex, component, null)[factor];
	}

	@Override
	public RandomVariable[] getDrift(final int timeIndex, final RandomVariable[] realizationAtTimeIndex, final RandomVariable[] realizationPredictor) {

		final RandomVariable[] drift = new RandomVariable[getNumberOfComponents()];

		for(int componentIndex=0; componentIndex<getNumberOfComponents(); componentIndex++) {

			// Check if this LIBOR is already fixed
			if(getTime(timeIndex) >= this.getLiborPeriod(componentIndex)) {
				drift[componentIndex] = null;
				continue;
			}

			drift[componentIndex] = this.getDrift(timeIndex, componentIndex, realizationAtTimeIndex, realizationPredictor);
		}

		return drift;
	}

	/* (non-Javadoc)
	 * @see com.spacelike.fdml.monteCarlo.stockOptionPricing.LogNormalProcess#getDrift(int, int)
	 */
	@Override
	public RandomVariable getDrift(final int timeIndex, final int componentIndex, final RandomVariable[] realizationAtTimeIndex, final RandomVariable[] realizationPredictor) {
		// The following is the drift of the LIBOR component

		final int		numberOfPaths		= getNumberOfPaths();

		final double	time					= getTime(timeIndex);

		/*
		 * We implemented several different methods to calculate the drift
		 */
		final RandomVariable[] liborVectorStart	= realizationAtTimeIndex;
		final RandomVariable[] liborVectorEnd	= realizationPredictor;
		if(driftAproximationMethod == Driftapproximation.PREDICTOR_CORRECTOR && liborVectorEnd != null) {
			final double[] drift = getLMMTerminasureDriftEuler(timeIndex, componentIndex, liborVectorStart);
			final double[] driftEulerWithPredictor = getLMMTerminasureDriftEuler(timeIndex, componentIndex, liborVectorEnd);
			for(int pathIndex=0; pathIndex<numberOfPaths; pathIndex++) {
				drift[pathIndex] = (drift[pathIndex] + driftEulerWithPredictor[pathIndex]) / 2.0;
			}
			return new RandomVariableFromDoubleArray(time, drift);
		}
		else if(driftAproximationMethod == Driftapproximation.LINE_INTEGRAL && liborVectorEnd != null) {
			final double[] drift = getLMMTerminasureDriftLineIntegral(timeIndex, componentIndex, liborVectorStart, liborVectorEnd);
			return new RandomVariableFromDoubleArray(time, drift);
		}
		else {
			final double[] drift = getLMMTerminasureDriftEuler(timeIndex, componentIndex, liborVectorStart);
			return new RandomVariableFromDoubleArray(time, drift);
		}
	}

	/**
	 * @return Returns the driftAproximationMethod.
	 */
	public Driftapproximation getDriftAproximationMethod() {
		return driftAproximationMethod;
	}

	protected double[] getLMMTerminasureDriftEuler(final int timeIndex, final int componentIndex, final RandomVariable[] liborVectorStart) {
		// The following is the drift of the LIBOR component

		final int		numberOfPaths		= getNumberOfPaths();

		final double	time					= getTime(timeIndex);

		// Allocate memory
		final double[] drift = new double[numberOfPaths];

		// Initialize to 0.0
		Arrays.fill(drift,0.0);

		// Get the start and end of the summation (start is the LIBOR after the current LIBOR component, end is the last LIBOR)
		int firstLiborIndex	= componentIndex+1;
		int lastLiborIndex 	= getLiborPeriodDiscretization().getNumberOfTimeSteps()-1;

		if(measure == Measure.SPOT) {
			// Spot measure
			firstLiborIndex	= this.getLiborPeriodIndex(time)+1;
			if(firstLiborIndex<0) {
				firstLiborIndex = -firstLiborIndex-1 + 1;
			}
			lastLiborIndex	= componentIndex;
		}

		// The sum
		for(int liborIndex = firstLiborIndex; liborIndex<=lastLiborIndex; liborIndex++) {

			final double		periodLength	= getLiborPeriodDiscretization().getTimeStep(liborIndex);
			final RandomVariable	libor			= liborVectorStart[liborIndex];
			final RandomVariable	covariance		= covarianceModel.getCovariance(timeIndex, componentIndex, liborIndex, null);


			if(measure == Measure.SPOT) {
				for(int path=0; path<drift.length; path++) {
					// The drift summation
					drift[path] += libor.get(path) * periodLength * covariance.get(path) / (1 + libor.get(path) * periodLength);
				}
			}
			else {
				for(int path=0; path<drift.length; path++) {
					// The drift summation
					drift[path] -= libor.get(path) * periodLength * covariance.get(path) / (1 + libor.get(path) * periodLength);
				}
			}
		}

		return drift;
	}

	private double[] getLMMTerminasureDriftLineIntegral(final int timeIndex, final int componentIndex, final RandomVariable[] liborVectorStart, final RandomVariable[] liborVectorEnd) {
		// The following is the dirft of the LIBOR component

		final int		numberOfPaths		= getNumberOfPaths();

		final double	time				= getTime(timeIndex);

		// Allocate memory
		final double[] drift = new double[numberOfPaths];

		// Initialize to 0.0
		Arrays.fill(drift,0.0);

		// Get the start and end of the summation (start is the LIBOR after the current LIBOR component, end is the last LIBOR)
		int firstLiborIndex	= componentIndex+1;
		int lastLiborIndex 	= getLiborPeriodDiscretization().getNumberOfTimeSteps()-1;

		if(measure == Measure.SPOT) {
			// Spot measure
			firstLiborIndex	= this.getLiborPeriodIndex(time)+1;
			if(firstLiborIndex<0) {
				firstLiborIndex = -firstLiborIndex-1 + 1;
			}
			lastLiborIndex	= componentIndex;
		}

		// The sum
		for(int liborIndex = firstLiborIndex; liborIndex<=lastLiborIndex; liborIndex++) {

			final double periodLength = getLiborPeriodDiscretization().getTimeStep(liborIndex);

			final RandomVariable liborAtStartOfPeriod		= liborVectorStart[liborIndex];
			final RandomVariable liborAtEndOfPeriod			= liborVectorEnd[liborIndex];

			final RandomVariable	covariance	= covarianceModel.getCovariance(timeIndex, componentIndex, liborIndex, null);

			for(int path=0; path<drift.length; path++) {
				drift[path] -= covariance.get(path) * Math.log(
						(1 + periodLength * liborAtEndOfPeriod.get(path))
						/ (1 + periodLength * liborAtStartOfPeriod.get(path))
						)
						/ Math.log(liborAtEndOfPeriod.get(path) / liborAtStartOfPeriod.get(path));
			}
		}
		return drift;
	}

	/**
	 * @return Returns the measure.
	 */
	public Measure getMeasure() {
		return measure;
	}

	/**
	 * @TODO This method does not interpolate the Numeraire if it is requested outside the liborPeriodDiscretization
	 *
	 * @param timeIndex The time indes at which the numeriare is requrested.
	 * @return The numeraire at the specified time as <code>RandomVariable</code>
	 */
	@Override
	public RandomVariable getNumeraire(final int timeIndex)
	{
		final double time = getTime(timeIndex);

		// Get the start of the product
		int firstLiborIndex		= this.getLiborPeriodIndex(time);
		//		while(liborPeriodDiscretization[firstLiborIndex] < time) firstLiborIndex++;

		// Get the end of the product
		int lastLiborIndex 	= getLiborPeriodDiscretization().getNumberOfTimeSteps()-1;

		if(measure == Measure.SPOT) {
			// Spot measure
			firstLiborIndex	= 0;
			lastLiborIndex	= this.getLiborPeriodIndex(time)-1;
			if(lastLiborIndex < -1) {
				System.out.println("Interpolation on Numeraire not supported.");
			}
		}

		/**
		 * Calculation of the numeraire (terminal measure)
		 */
		final double[] numeraire = new double[getNumberOfPaths()];

		// Initialize to 1.0
		Arrays.fill(numeraire,1.0);

		// The product
		for(int liborIndex = firstLiborIndex; liborIndex<=lastLiborIndex; liborIndex++) {
			final RandomVariable libor = getProcessValue(Math.min(timeIndex,liborIndex), liborIndex);

			final double periodLength = getLiborPeriodDiscretization().getTimeStep(liborIndex);

			if(measure == Measure.SPOT) {
				for(int path=0; path<numeraire.length; path++) {
					numeraire[path] *= 1 + libor.get(path) * periodLength;
				}
			}
			else {
				for(int path=0; path<numeraire.length; path++) {
					numeraire[path] /= 1 + libor.get(path) * periodLength;
				}
			}
		}
		return new RandomVariableFromDoubleArray(time,numeraire);
	}

	/**
	 * @param driftAproximationMethod The driftAproximationMethod to set.
	 */
	public void setDriftAproximationMethod(final Driftapproximation driftAproximationMethod) {
		this.driftAproximationMethod = driftAproximationMethod;
	}

	/**
	 * @param measure The measure to set.
	 */
	public void setMeasure(final Measure measure) {
		this.measure = measure;
	}

	/**
	 * @return the covarianceModel
	 */
	public LIBORCovarianceModel getCovarianceModel() {
		return covarianceModel;
	}

	/**
	 * @param covarianceModel the covarianceModel to set
	 */
	public void setCovarianceModel(final LIBORCovarianceModel covarianceModel) {
		this.covarianceModel = covarianceModel;
	}



	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.MonteCarloSimulationModel#getCloneWithModifiedData(java.util.Map)
	 */
	@Override
	public SimpleLIBORMarketModel getCloneWithModifiedData(final Map<String, Object> dataModified) {
		throw new RuntimeException("Method not implemented");
	}

	@Override
	public Object getCloneWithModifiedSeed(final int seed) {
		return new SimpleLIBORMarketModel(
				getLiborPeriodDiscretization(),
				liborInitialValues,
				getCovarianceModel(),
				new BrownianMotionLazyInit(getTimeDiscretization(), covarianceModel.getNumberOfFactors(), getNumberOfPaths(), seed)
				);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel#getModel()
	 */
	@Override
	public LIBORMarketModelFromCovarianceModel getModel() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel#getProcess()
	 */
	@Override
	public MonteCarloProcess getProcess() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.MonteCarloSimulationModel#getRandomVariableForConstant(double)
	 */
	@Override
	public RandomVariable getRandomVariableForConstant(final double value) {
		return getBrownianMotion().getRandomVariableForConstant(value);
	}

	@Override
	public Map<String, RandomVariable> getModelParameters() {
		// TODO Add implementation
		throw new UnsupportedOperationException();
	}
}
