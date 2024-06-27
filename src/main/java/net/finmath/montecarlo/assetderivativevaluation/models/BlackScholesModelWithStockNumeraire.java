/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.01.2004
 */
package net.finmath.montecarlo.assetderivativevaluation.models;

import java.util.Arrays;
import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFromArrayFactory;
import net.finmath.montecarlo.model.AbstractProcessModel;
import net.finmath.montecarlo.process.MonteCarloProcess;
import net.finmath.stochastic.RandomVariable;

/**
 * This class implements a Black Scholes Model, that is, it provides the drift and volatility specification
 * and performs the calculation of the numeraire (consistent with the dynamics, i.e. the drift).
 *
 * The model is
 * \[
 * 	dS = r S dt + \sigma S dW, \quad S(0) = S_{0},
 * \]
 * \[
 * 	dB = r B dt, \quad B(0) = B_{0},
 * \]
 * however, the model uses \( N = S \) as the numeraire.
 * 
 * A straight forward calculation shows
 * that this alters the drift of \( S \) from \( r - \frac{1}{2} \sigma^2 \) to \( r + \frac{1}{2} \sigma^2 \)
 * (note that from Itô's Lemma we have d(B/S) = (B/S) (dB/B - dS/S - (dB/B)(dS/S) + (dS/S)(dS/S))).
 *
 * The class provides the model of S to an <code>{@link net.finmath.montecarlo.process.MonteCarloProcess}</code> via the specification of
 * \( f = exp \), \( \mu = r + \frac{1}{2} \sigma^2 \), \( \lambda_{1,1} = \sigma \), i.e.,
 * of the SDE
 * \[
 * 	dX = \mu dt + \lambda_{1,1} dW, \quad X(0) = \log(S_{0}),
 * \]
 * with \( S = f(X) \). See {@link net.finmath.montecarlo.process.MonteCarloProcess} for the notation.
 *
 * @author Christian Fries
 * @see net.finmath.montecarlo.process.MonteCarloProcess The interface for numerical schemes.
 * @see net.finmath.montecarlo.model.ProcessModel The interface for models provinding parameters to numerical schemes.
 * @version 1.0
 */
public class BlackScholesModelWithStockNumeraire extends AbstractProcessModel {

	private final RandomVariableFactory randomVariableFactory;

	private final RandomVariable initialValue;
	private final RandomVariable riskFreeRate;
	private final RandomVariable volatility;

	// Cache for arrays provided though AbstractProcessModel
	private final RandomVariable[]	initialState;
	private final RandomVariable[]	drift;
	private final RandomVariable[]	factorLoadings;

	/**
	 * Create a Black-Scholes specification implementing AbstractProcessModel.
	 *
	 * @param initialValue Spot value.
	 * @param riskFreeRate The risk free rate.
	 * @param volatility The log volatility.
	 * @param randomVariableFactory The random variable factory used to create random variables from constants.
	 */
	public BlackScholesModelWithStockNumeraire(
			final RandomVariable initialValue,
			final RandomVariable riskFreeRate,
			final RandomVariable volatility,
			final RandomVariableFactory randomVariableFactory) {
		super();

		this.initialValue = initialValue;
		this.volatility = volatility;
		this.riskFreeRate	= riskFreeRate;
		this.randomVariableFactory = randomVariableFactory;

		// Cache
		initialState = new RandomVariable[] { initialValue.log() };
		drift = new RandomVariable[] { riskFreeRate.add(volatility.squared().div(2)) };
		factorLoadings = new RandomVariable[] { volatility };
	}

	/**
	 * Create a Monte-Carlo simulation using given time discretization.
	 *
	 * @param initialValue Spot value.
	 * @param riskFreeRate The risk free rate.
	 * @param volatility The log volatility.
	 * @param randomVariableFactory The random variable factory used to create random variables from constants.
	 */
	public BlackScholesModelWithStockNumeraire(
			final double initialValue,
			final double riskFreeRate,
			final double volatility,
			final RandomVariableFactory randomVariableFactory) {
		this(randomVariableFactory.createRandomVariable(initialValue), randomVariableFactory.createRandomVariable(riskFreeRate), randomVariableFactory.createRandomVariable(volatility), randomVariableFactory);
	}

	/**
	 * Create a Black-Scholes model from given parameters.
	 *
	 * @param initialValue Spot value.
	 * @param riskFreeRate The risk free rate.
	 * @param volatility The log volatility.
	 */
	public BlackScholesModelWithStockNumeraire(
			final double initialValue,
			final double riskFreeRate,
			final double volatility) {
		this(initialValue, riskFreeRate, volatility, new RandomVariableFromArrayFactory());
	}

	@Override
	public RandomVariable[] getInitialState(MonteCarloProcess process) {
		return initialState;
	}

	@Override
	public RandomVariable[] getDrift(final MonteCarloProcess process, final int timeIndex, final RandomVariable[] realizationAtTimeIndex, final RandomVariable[] realizationPredictor) {
		return drift;
	}

	@Override
	public RandomVariable[] getFactorLoading(final MonteCarloProcess process, final int timeIndex, final int component, final RandomVariable[] realizationAtTimeIndex) {
		return factorLoadings;
	}

	@Override
	public RandomVariable applyStateSpaceTransform(MonteCarloProcess process, int timeIndex, final int componentIndex, final RandomVariable randomVariable) {
		return randomVariable.exp();
	}

	@Override
	public RandomVariable applyStateSpaceTransformInverse(final MonteCarloProcess process, final int timeIndex, final int componentIndex, final RandomVariable randomVariable) {
		return randomVariable.log();
	}

	@Override
	public RandomVariable getNumeraire(MonteCarloProcess process, final double time) {
		try {
			return process.getProcessValue(process.getTimeIndex(time), 0);
		} catch (CalculationException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public int getNumberOfComponents() {
		return 1;
	}

	@Override
	public int getNumberOfFactors() {
		return 1;
	}

	@Override
	public RandomVariable getRandomVariableForConstant(final double value) {
		return randomVariableFactory.createRandomVariable(value);
	}

	@Override
	public BlackScholesModelWithStockNumeraire getCloneWithModifiedData(final Map<String, Object> dataModified) {
		/*
		 * Determine the new model parameters from the provided parameter map.
		 */
		final double	newInitialValue	= dataModified.get("initialValue") != null	? ((Number)dataModified.get("initialValue")).doubleValue() 	: initialValue.getAverage();
		final double	newRiskFreeRate	= dataModified.get("riskFreeRate") != null	? ((Number)dataModified.get("riskFreeRate")).doubleValue()	: getRiskFreeRate().getAverage();
		final double	newVolatility	= dataModified.get("volatility") != null	? ((Number)dataModified.get("volatility")).doubleValue()	: getVolatility().getAverage();

		return new BlackScholesModelWithStockNumeraire(newInitialValue, newRiskFreeRate, newVolatility, randomVariableFactory);
	}

	/**
	 * Return the initial value of this model.
	 *
	 * @return the initial value of this model.
	 */
	@Override
	public RandomVariable[] getInitialValue(final MonteCarloProcess process) {
		return new RandomVariable[] { initialValue };
	}

	/**
	 * Returns the risk free rate parameter of this model.
	 *
	 * @return Returns the riskFreeRate.
	 */
	public RandomVariable getRiskFreeRate() {
		return riskFreeRate;
	}

	/**
	 * Returns the volatility parameter of this model.
	 *
	 * @return Returns the volatility.
	 */
	public RandomVariable getVolatility() {
		return factorLoadings[0];
	}

	@Override
	public String toString() {
		return "BlackScholesModel [initialValue=" + initialValue + ", riskFreeRate=" + riskFreeRate + ", volatility="
				+ volatility + ", randomVariableFactory=" + randomVariableFactory + ", initialState="
				+ Arrays.toString(initialState) + ", drift=" + Arrays.toString(drift) + ", factorLoadings="
				+ Arrays.toString(factorLoadings) + "]";
	}
}
