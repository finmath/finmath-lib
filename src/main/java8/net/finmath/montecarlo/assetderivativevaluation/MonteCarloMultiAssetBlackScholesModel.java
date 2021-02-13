/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.06.2014
 */
package net.finmath.montecarlo.assetderivativevaluation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.functions.LinearAlgebra;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.BrownianMotionFromMersenneRandomNumbers;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFromArrayFactory;
import net.finmath.montecarlo.model.AbstractProcessModel;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel.Scheme;
import net.finmath.montecarlo.process.MonteCarloProcess;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;

/**
 * This class implements a multi-asset Black Schole Model as Monte-Carlo simulation implementing <code>AssetModelMonteCarloSimulationModel</code>.
 *
 * The model is
 * \[
 * 	dS_{i} = r S_{i} dt + \sigma_{i} S_{i} dW_{i}, \quad S_{i}(0) = S_{i,0},
 * \]
 * \[
 * 	dN = r N dt, \quad N(0) = N_{0},
 * \]
 * \[
 * 	dW_{i} dW_{j} = \rho_{i,j} dt,
 * \]
 *
 * The class provides the model of \( S_{i} \) to an <code>{@link net.finmath.montecarlo.process.MonteCarloProcess}</code> via the specification of
 * \( f = exp \), \( \mu_{i} = r - \frac{1}{2} \sigma_{i}^2 \), \( \lambda_{i,j} = \sigma_{i} g_{i,j} \), i.e.,
 * of the SDE
 * \[
 * 	dX_{i} = \mu_{i} dt + \lambda_{i,j} dW, \quad X_{i}(0) = \log(S_{i,0}),
 * \]
 * with \( S = f(X) \). See {@link net.finmath.montecarlo.process.MonteCarloProcess} for the notation.
 *
 * @author Christian Fries
 * @author Roland Bachl
 * @see net.finmath.montecarlo.process.MonteCarloProcess The interface for numerical schemes.
 * @see net.finmath.montecarlo.model.ProcessModel The interface for models provinding parameters to numerical schemes.
 * @version 1.1
 */
public class MonteCarloMultiAssetBlackScholesModel extends AbstractProcessModel implements AssetModelMonteCarloSimulationModel {

	private final MonteCarloProcess process;

	private final RandomVariableFactory randomVariableFactory;

	private final double[]		initialValues;
	private final double		riskFreeRate;		// Actually the same as the drift (which is not stochastic)
	private final double[][]	factorLoadings;

	private static final int defaultSeed = 3141;

	private final RandomVariable[]		initialStates;
	private final RandomVariable[]		drift;
	private final RandomVariable[][]	factorLoadingOnPaths;

	/**
	 * Create a Monte-Carlo simulation using given time discretization.
	 *
	 * @param randomVariableFactory The RandomVariableFactory used to construct model parameters as random variables.
	 * @param brownianMotion The Brownian motion to be used for the numerical scheme.
	 * @param initialValues Spot values.
	 * @param riskFreeRate The risk free rate.
	 * @param factorLoadings The matrix of factor loadings, where factorLoadings[underlyingIndex][factorIndex] is the coefficient of the Brownian driver factorIndex used for the underlying underlyingIndex.
	 */
	public MonteCarloMultiAssetBlackScholesModel(
			final RandomVariableFactory randomVariableFactory,
			final BrownianMotion brownianMotion,
			final double[]		initialValues,
			final double		riskFreeRate,
			final double[][]	factorLoadings
			) {
		this.randomVariableFactory = randomVariableFactory;
		this.initialValues	= initialValues;
		this.riskFreeRate	= riskFreeRate;
		this.factorLoadings	= factorLoadings;

		/*
		 * The interface definition requires that we provide the initial value, the drift and the volatility in terms of random variables.
		 * We construct the corresponding random variables here and will return (immutable) references to them.
		 *
		 * Since the underlying process is configured to simulate log(S),
		 * the initial value and the drift are transformed accordingly.
		 *
		 */
		initialStates = new RandomVariable[getNumberOfComponents()];
		drift = new RandomVariable[getNumberOfComponents()];
		factorLoadingOnPaths = new RandomVariable[getNumberOfComponents()][];
		for(int underlyingIndex = 0; underlyingIndex<initialValues.length; underlyingIndex++) {
			double volatilitySquaredForUnderlying = 0.0;
			factorLoadingOnPaths[underlyingIndex]		= new RandomVariable[factorLoadings[underlyingIndex].length];
			for(int factorIndex = 0; factorIndex<factorLoadings[underlyingIndex].length; factorIndex++) {
				volatilitySquaredForUnderlying += factorLoadings[underlyingIndex][factorIndex] * factorLoadings[underlyingIndex][factorIndex];
				factorLoadingOnPaths[underlyingIndex][factorIndex]	= getRandomVariableForConstant(factorLoadings[underlyingIndex][factorIndex]);
			}

			initialStates[underlyingIndex]				= getRandomVariableForConstant(Math.log(initialValues[underlyingIndex]));
			drift[underlyingIndex]						= getRandomVariableForConstant(riskFreeRate - volatilitySquaredForUnderlying / 2.0);
		}

		// TODO Creation of this should be completed before calling process constructor.

		// Create a corresponding MC process
		process = new EulerSchemeFromProcessModel(this, brownianMotion, Scheme.EULER_FUNCTIONAL);
	}

	/**
	 * Create a Monte-Carlo simulation using given time discretization.
	 *
	 * @param brownianMotion The Brownian motion to be used for the numerical scheme.
	 * @param initialValues Spot values.
	 * @param riskFreeRate The risk free rate.
	 * @param volatilities The log volatilities.
	 * @param correlations A correlation matrix.
	 */
	public MonteCarloMultiAssetBlackScholesModel(
			final BrownianMotion brownianMotion,
			final double[]	initialValues,
			final double		riskFreeRate,
			final double[]	volatilities,
			final double[][]	correlations
			) {
		this(new RandomVariableFromArrayFactory(), brownianMotion, initialValues, riskFreeRate, getFactorLoadingsFromVolatilityAnCorrelation(volatilities, correlations));
	}

	private static double[][] getFactorLoadingsFromVolatilityAnCorrelation(double[] volatilities, double[][] correlations) {
		final double[][] factorLoadings = LinearAlgebra.getFactorMatrix(correlations, correlations.length);
		for(int underlyingIndex = 0; underlyingIndex<factorLoadings.length; underlyingIndex++) {
			final double volatility = volatilities[underlyingIndex];
			for(int factorIndex = 0; factorIndex<factorLoadings[underlyingIndex].length; factorIndex++) {
				factorLoadings[underlyingIndex][factorIndex] = factorLoadings[underlyingIndex][factorIndex] * volatility;
			}
		}
		return factorLoadings;
	}

	/**
	 * Create a Monte-Carlo simulation using given time discretization.
	 *
	 * @param timeDiscretization The time discretization.
	 * @param numberOfPaths The number of Monte-Carlo path to be used.
	 * @param initialValues Spot values.
	 * @param riskFreeRate The risk free rate.
	 * @param volatilities The log volatilities.
	 * @param correlations A correlation matrix.
	 */
	public MonteCarloMultiAssetBlackScholesModel(
			final TimeDiscretization timeDiscretization,
			final int numberOfPaths,
			final double[]	initialValues,
			final double		riskFreeRate,
			final double[]	volatilities,
			final double[][]	correlations
			) {
		this(timeDiscretization,
				numberOfPaths,
				defaultSeed,
				initialValues,
				riskFreeRate,
				volatilities,
				correlations
				);
	}

	/**
	 * Create a Monte-Carlo simulation using given time discretization.
	 *
	 * @param timeDiscretization The time discretization.
	 * @param numberOfPaths The number of Monte-Carlo path to be used.
	 * @param seed The seed to be used.
	 * @param initialValues Spot values.
	 * @param riskFreeRate The risk free rate.
	 * @param volatilities The log volatilities.
	 * @param correlations A correlation matrix.
	 */
	public MonteCarloMultiAssetBlackScholesModel(
			final TimeDiscretization timeDiscretization,
			final int numberOfPaths,
			final int seed,
			final double[]		initialValues,
			final double		riskFreeRate,
			final double[]		volatilities,
			final double[][]	correlations
			) {
		this(new BrownianMotionFromMersenneRandomNumbers(timeDiscretization, initialValues.length /* numberOfFactors */, numberOfPaths, seed), initialValues, riskFreeRate, volatilities, correlations);
	}

	@Override
	public RandomVariable[] getInitialState(MonteCarloProcess process) {
		return initialStates;
	}

	@Override
	public RandomVariable[] getDrift(final MonteCarloProcess process, final int timeIndex, final RandomVariable[] realizationAtTimeIndex, final RandomVariable[] realizationPredictor) {
		return drift;
	}

	@Override
	public RandomVariable[] getFactorLoading(final MonteCarloProcess process, final int timeIndex, final int component, final RandomVariable[] realizationAtTimeIndex) {
		return factorLoadingOnPaths[component];
	}

	@Override
	public RandomVariable applyStateSpaceTransform(final MonteCarloProcess process, final int timeIndex, final int componentIndex, final RandomVariable randomVariable) {
		return randomVariable.exp();
	}

	@Override
	public RandomVariable applyStateSpaceTransformInverse(final MonteCarloProcess process, final int timeIndex, final int componentIndex, final RandomVariable randomVariable) {
		return randomVariable.log();
	}

	@Override
	public RandomVariable getAssetValue(final double time, final int assetIndex) throws CalculationException {
		int timeIndex = getTimeIndex(time);
		if(timeIndex < 0) {
			timeIndex = -timeIndex-1;
		}
		return getAssetValue(timeIndex, assetIndex);
	}

	@Override
	public RandomVariable getAssetValue(final int timeIndex, final int assetIndex) throws CalculationException {
		return process.getProcessValue(timeIndex, assetIndex);
	}

	@Override
	public RandomVariable getMonteCarloWeights(final double time) throws CalculationException {
		return process.getMonteCarloWeights(getTimeIndex(time));
	}

	@Override
	public RandomVariable getNumeraire(final MonteCarloProcess process, double time) {
		final double numeraireValue = Math.exp(riskFreeRate * time);

		return getRandomVariableForConstant(numeraireValue);
	}

	@Override
	public RandomVariable getNumeraire(final int timeIndex) throws CalculationException {
		final double time = process.getTime(timeIndex);

		return getNumeraire(process, time);
	}

	@Override
	public RandomVariable getNumeraire(final double time) throws CalculationException {
		return getNumeraire(process, time);
	}

	@Override
	public RandomVariable getRandomVariableForConstant(final double value) {
		return randomVariableFactory.createRandomVariable(value);
	}

	@Override
	public int getNumberOfComponents() {
		return initialValues.length;
	}

	@Override
	public int getNumberOfAssets() {
		return getNumberOfComponents();
	}

	@Override
	public String toString() {
		return "MonteCarloMultiAssetBlackScholesModel [initialValues="
				+ Arrays.toString(initialValues) + ", riskFreeRate="
				+ riskFreeRate + ", factorLoadings="
				+ Arrays.toString(factorLoadings) + "]";
	}

	/**
	 * Returns the risk free rate parameter of this model.
	 *
	 * @return Returns the riskFreeRate.
	 */
	public double getRiskFreeRate() {
		return riskFreeRate;
	}

	/**
	 * Returns the factorLoadings parameters of this model.
	 *
	 * @return Returns the factorLoadings.
	 */
	public double[][] getFactorLoadings() {
		return factorLoadings;
	}

	/**
	 * Returns the volatility parameters of this model.
	 *
	 * @return Returns the volatilities.
	 */
	public double[] getVolatilities() {
		final double[] volatilities = new double[factorLoadings.length];
		for(int underlyingIndex = 0; underlyingIndex<factorLoadings.length; underlyingIndex++) {
			double volatilitySquaredOfUnderlying = 0.0;
			for(int factorIndex = 0; factorIndex<factorLoadings[underlyingIndex].length; factorIndex++) {
				final double factorLoading = factorLoadings[underlyingIndex][factorIndex];
				volatilitySquaredOfUnderlying += factorLoading*factorLoading;
			}
			volatilities[underlyingIndex] = Math.sqrt(volatilitySquaredOfUnderlying);
		}
		return volatilities;
	}

	/**
	 * Returns the volatility parameters of this model.
	 *
	 * @return Returns the volatilities.
	 */
	public double[][] getCorrelations() {
		final double[] volatilities = getVolatilities();

		final double[][] correlations = new double[factorLoadings.length][factorLoadings.length];
		for(int underlyingIndex1 = 0; underlyingIndex1<factorLoadings.length; underlyingIndex1++) {
			for(int underlyingIndex2 = 0; underlyingIndex2<factorLoadings.length; underlyingIndex2++) {
				double covariance = 0.0;
				for(int factorIndex = 0; factorIndex<factorLoadings[underlyingIndex1].length; factorIndex++) {
					covariance += factorLoadings[underlyingIndex1][factorIndex]*factorLoadings[underlyingIndex2][factorIndex];
				}

				double correlation;
				if(volatilities[underlyingIndex1] != 0 && volatilities[underlyingIndex2] != 0) {
					correlation = covariance / volatilities[underlyingIndex1] / volatilities[underlyingIndex2];
				}
				else {
					correlation = underlyingIndex1 == underlyingIndex2 ? 1.0 : 0.0;
				}
				correlations[underlyingIndex1][underlyingIndex2] = correlation;
			}
		}
		return correlations;
	}

	/**
	 * Returns the number of paths.
	 *
	 * @return The number of paths.
	 * @see net.finmath.montecarlo.process.MonteCarloProcessFromProcessModel#getNumberOfPaths()
	 */
	@Override
	public int getNumberOfPaths() {
		return process.getNumberOfPaths();
	}

	@Override
	public MonteCarloMultiAssetBlackScholesModel getCloneWithModifiedData(final Map<String, Object> dataModified) {

		BrownianMotion 	newBrownianMotion		= (BrownianMotion) process.getStochasticDriver();

		final RandomVariableFactory newRandomVariableFactory = (RandomVariableFactory) dataModified.getOrDefault("randomVariableFactory", randomVariableFactory);

		final double[]		newInitialValues		= (double[]) dataModified.getOrDefault("initialValues", initialValues);
		final double			newRiskFreeRate			= ((Double) dataModified.getOrDefault("riskFreeRate", riskFreeRate)).doubleValue();

		double[][]		newFactorLoadings		= (double[][]) dataModified.getOrDefault("factorLoadings", factorLoadings);
		if(dataModified.containsKey("volatilities") || dataModified.containsKey("correlations")) {
			if(dataModified.containsKey("factorLoadings")) {
				throw new IllegalArgumentException("Inconsistend parameters. Cannot specify volatility or corellation and factorLoadings at the same time.");
			}

			final double[] newVolatilities = (double[]) dataModified.getOrDefault("volatilities", getVolatilities());
			final double[][] newCorrelations = (double[][]) dataModified.getOrDefault("correlations", getCorrelations());
			newFactorLoadings = getFactorLoadingsFromVolatilityAnCorrelation(newVolatilities, newCorrelations);
		}

		if(dataModified.containsKey("seed")) {
			newBrownianMotion = newBrownianMotion.getCloneWithModifiedSeed((Integer) dataModified.get("seed"));
		}
		if(dataModified.containsKey("timeDiscretization")) {
			newBrownianMotion = newBrownianMotion.getCloneWithModifiedTimeDiscretization( (TimeDiscretization) dataModified.get("timeDiscretization"));
		}

		return new MonteCarloMultiAssetBlackScholesModel(
				newRandomVariableFactory,
				newBrownianMotion,
				newInitialValues,
				newRiskFreeRate,
				newFactorLoadings
				);
	}

	@Override
	public AssetModelMonteCarloSimulationModel getCloneWithModifiedSeed(final int seed) {
		final Map<String, Object> dataModified = new HashMap<>();
		dataModified.put("seed", Integer.valueOf(seed));
		return getCloneWithModifiedData(dataModified);
	}

	@Override
	public TimeDiscretization getTimeDiscretization() {
		return process.getTimeDiscretization();
	}

	@Override
	public double getTime(int timeIndex) {
		return process.getTime(timeIndex);
	}

	@Override
	public int getTimeIndex(double time) {
		return process.getTimeIndex(time);
	}

	@Override
	public RandomVariable getMonteCarloWeights(int timeIndex) throws CalculationException {
		return process.getMonteCarloWeights(timeIndex);
	}

	@Override
	public int getNumberOfFactors() {
		return process.getNumberOfFactors();
	}
}
