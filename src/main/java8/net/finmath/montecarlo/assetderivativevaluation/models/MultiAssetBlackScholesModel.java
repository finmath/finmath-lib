/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.06.2014
 */
package net.finmath.montecarlo.assetderivativevaluation.models;

import java.util.Arrays;
import java.util.Map;

import net.finmath.functions.LinearAlgebra;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFromArrayFactory;
import net.finmath.montecarlo.model.AbstractProcessModel;
import net.finmath.montecarlo.process.MonteCarloProcess;
import net.finmath.stochastic.RandomVariable;

/**
 * This class implements a multi-asset Black Scholes model providing an <code>AbstractProcessModel</code>.
 * The class can be used with an EulerSchemeFromProcessModel to create a Monte-Carlo simulation.
 *
 * The model can be specified by general factor loadings, that is, in the form
 * \[
 * 	dS_{i} = r S_{i} dt + S_{i} \sum_{j=0}^{m-1} f{i,j} dW_{j}, \quad S_{i}(0) = S_{i,0},
 * \]
 * \[
 * 	dN = r N dt, \quad N(0) = N_{0}.
 * \]
 *
 * Alternatively, the model can be specifies by providing volatilities and correlations
 * from which the factor loadings \( f_{i,j} \) are derived such that
 * \[
 * 	\sum_{k=0}^{m-1} f{i,k} f{j,k} = \sigma_{i} \sigma_{j} \rho_{i,j}
 * \]
 * such that the effective model is
 * \[
 * 	dS_{i} = r S_{i} dt + \sigma_{i} S_{i} dW_{i}, \quad S_{i}(0) = S_{i,0},
 * \]
 * \[
 * 	dN = r N dt, \quad N(0) = N_{0},
 * \]
 * \[
 * 	dW_{i} dW_{j} = \rho_{i,j} dt,
 * \]
 * Note that in case the model is used with an EulerSchemeFromProcessModel, the BrownianMotion used can
 * have a correlation, which alters the simulation (which is admissible).
 * The specification above hold, provided that the BrownianMotion used has independent components.
 *
 * The class provides the model of \( S_{i} \) to an <code>{@link net.finmath.montecarlo.process.MonteCarloProcess}</code> via the specification of
 * \( f = exp \), \( \mu_{i} = r - \frac{1}{2} \sigma_{i}^2 \), \( \lambda_{i,j} = \sigma_{i} g_{i,j} \), i.e.,
 * of the SDE
 * \[
 * 	dX_{i} = \mu_{i} dt + \sum_{j=0}^{m-1} \lambda_{i,j} dW_{j}, \quad X_{i}(0) = \log(S_{i,0}),
 * \]
 * with \( S = f(X) \). See {@link net.finmath.montecarlo.process.MonteCarloProcess} for the notation.
 *
 * @author Christian Fries
 * @see net.finmath.montecarlo.process.MonteCarloProcess The interface for numerical schemes.
 * @see net.finmath.montecarlo.model.ProcessModel The interface for models provinding parameters to numerical schemes.
 * @version 1.1
 */
public class MultiAssetBlackScholesModel extends AbstractProcessModel {

	private final RandomVariableFactory randomVariableFactory;

	private final double[]		initialValues;
	private final double		riskFreeRate;		// Actually the same as the drift (which is not stochastic)
	private final double[][]	factorLoadings;

	private final RandomVariable[]		initialStates;
	private final RandomVariable[]		drift;
	private final RandomVariable[][]	factorLoadingOnPaths;

	/**
	 * Create a multi-asset Black-Scholes model.
	 *
	 * @param randomVariableFactory The RandomVariableFactory used to construct model parameters as random variables.
	 * @param initialValues Spot values.
	 * @param riskFreeRate The risk free rate.
	 * @param factorLoadings The matrix of factor loadings, where factorLoadings[underlyingIndex][factorIndex] is the coefficient of the Brownian driver factorIndex used for the underlying underlyingIndex.
	 */
	public MultiAssetBlackScholesModel(
			final RandomVariableFactory randomVariableFactory,
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
	}

	/**
	 * Create a multi-asset Black-Scholes model.
	 *
	 * @param randomVariableFactory The RandomVariableFactory used to construct model parameters as random variables.
	 * @param initialValues Spot values.
	 * @param riskFreeRate The risk free rate.
	 * @param volatilities The log volatilities.
	 * @param correlations A correlation matrix.
	 */
	public MultiAssetBlackScholesModel(
			final RandomVariableFactory randomVariableFactory,
			final double[]		initialValues,
			final double		riskFreeRate,
			final double[]		volatilities,
			final double[][]	correlations
			) {
		this(	randomVariableFactory,
				initialValues,
				riskFreeRate,
				getFactorLoadingsFromVolatilityAnCorrelation(volatilities, correlations)
				);
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
	 * Create a multi-asset Black-Scholes model.
	 *
	 * @param initialValues Spot values.
	 * @param riskFreeRate The risk free rate.
	 * @param factorLoadings The matrix of factor loadings, where factorLoadings[underlyingIndex][factorIndex] is the coefficient of the Brownian driver factorIndex used for the underlying underlyingIndex.
	 */
	public MultiAssetBlackScholesModel(double[] initialValues, double riskFreeRate, double[][] factorLoadings) {
		this(new RandomVariableFromArrayFactory(), initialValues, riskFreeRate, factorLoadings);
	}

	/**
	 * Create a multi-asset Black-Scholes model.
	 *
	 * @param initialValues Spot values.
	 * @param riskFreeRate The risk free rate.
	 * @param volatilities The log volatilities.
	 * @param correlations A correlation matrix.
	 */
	public MultiAssetBlackScholesModel(double[] initialValues, double riskFreeRate, double[] volatilities, double[][] correlations) {
		this(new RandomVariableFromArrayFactory(), initialValues, riskFreeRate, volatilities, correlations);
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
	public RandomVariable getNumeraire(final MonteCarloProcess process, double time) {
		final double numeraireValue = Math.exp(riskFreeRate * time);

		return getRandomVariableForConstant(numeraireValue);
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
	public int getNumberOfFactors() {
		return factorLoadings[0].length;
	}


	@Override
	public MultiAssetBlackScholesModel getCloneWithModifiedData(final Map<String, Object> dataModified) {

		final RandomVariableFactory newRandomVariableFactory = (RandomVariableFactory) dataModified.getOrDefault("randomVariableFactory", randomVariableFactory);

		final double[]		newInitialValues		= (double[]) dataModified.getOrDefault("initialValues", initialValues);
		final double			newRiskFreeRate			= ((Double) dataModified.getOrDefault("riskFreeRate", riskFreeRate)).doubleValue();

		double[][]		newFactorLoadings		= (double[][]) dataModified.getOrDefault("factorLoadings", factorLoadings);
		if(dataModified.containsKey("volatilities") || dataModified.containsKey("correlations")) {
			if(dataModified.containsKey("factorLoadings")) {
				throw new IllegalArgumentException("Inconsistend parameters. Cannot specify volatility or corellation and factorLoadings at the same time.");
			}

			final double[] newVolatilities = (double[]) dataModified.getOrDefault("volatilities", getVolatilityVector());
			final double[][] newCorrelations = (double[][]) dataModified.getOrDefault("correlations", getCorrelationMatrix());
			newFactorLoadings = getFactorLoadingsFromVolatilityAnCorrelation(newVolatilities, newCorrelations);
		}

		return new MultiAssetBlackScholesModel(
				newRandomVariableFactory,
				newInitialValues,
				newRiskFreeRate,
				newFactorLoadings
				);
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
	public double[][] getFactorLoadingMatrix() {
		return factorLoadings;
	}

	/**
	 * Returns the volatility parameters of this model.
	 *
	 * @return Returns the volatilities.
	 */
	public double[] getVolatilityVector() {
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
	public double[][] getCorrelationMatrix() {
		final double[] volatilities = getVolatilityVector();

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
}
