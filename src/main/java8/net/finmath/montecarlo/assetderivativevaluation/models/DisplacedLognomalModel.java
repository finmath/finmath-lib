/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.01.2004
 */
package net.finmath.montecarlo.assetderivativevaluation.models;

import java.util.Map;

import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFromArrayFactory;
import net.finmath.montecarlo.model.AbstractProcessModel;
import net.finmath.montecarlo.process.MonteCarloProcess;
import net.finmath.stochastic.RandomVariable;

/**
 * This class implements a displaced lognormal model, that is, it provides the drift and volatility specification
 * and performs the calculation of the numeraire (consistent with the dynamics, i.e. the drift).
 *
 * The model is
 * \[
 * 	\mathrm{d}S = r S \mathrm{d}t + \sigma (d \cdot N + S) \mathrm{d}W, \quad S(0) = S_{0},
 * \]
 * \[
 * 	\mathrm{d}N = r N \mathrm{d}t, \quad N(0) = N_{0},
 * \]
 *
 * Note that
 * \[
 *	\mathrm{d}(S/N) = \sigma (d+S/N) \mathrm{d}W
 * \]
 * that is
 * \[
 * 	\mathrm{d}X = - 1/2 \sigma^2 \mathrm{d}t + \sigma \mathrm{d}W
 * \]
 * with exp(X) = d + S/N, i.e. S = N ( exp(X)-d ).
 *
 * The class provides the model of S to an <code>{@link net.finmath.montecarlo.process.MonteCarloProcess}</code>
 * via the specification of
 * \( S = f(X) = N (exp(X)-d) \), \( \mu = -\frac{1}{2} \sigma^{2} \), \( \lambda_{1,1} = \sigma \), i.e.,
 * of the SDE
 * \[
 * 	dX = \mu dt + \lambda_{1,1} dW, \quad X(0) = \log(d+S_{0}),
 * \]
 * with \( N(0) = 1 \). See {@link net.finmath.montecarlo.process.MonteCarloProcess} for the notation.
 *
 * The model can be interpreted as a linear interpolation of the Black-Scholes model
 * {@link net.finmath.montecarlo.assetderivativevaluation.models.BlackScholesModel}
 * and the homogeneous Bachelier model
 * {@link net.finmath.montecarlo.assetderivativevaluation.models.BachelierModel}.
 *
 * @author Christian Fries
 * @see net.finmath.montecarlo.process.MonteCarloProcess The interface for numerical schemes.
 * @see net.finmath.montecarlo.model.ProcessModel The interface for models provinding parameters to numerical schemes.
 * @version 1.1
 */
public class DisplacedLognomalModel extends AbstractProcessModel {

	private final RandomVariableFactory randomVariableFactory;

	private final RandomVariable initialValue;
	private final RandomVariable riskFreeRate;		// Actually the same as the drift (which is not stochastic)
	private final RandomVariable displacement;
	private final RandomVariable volatility;

	/**
	 * Create a Monte-Carlo simulation using given time discretization.
	 *
	 * @param randomVariableFactory The RandomVariableFactory used to generate random variables from constants.
	 * @param initialValue Spot value.
	 * @param riskFreeRate The risk free rate.
	 * @param displacement The displacement parameter d.
	 * @param volatility The volatility.
	 */
	public DisplacedLognomalModel(
			final RandomVariableFactory randomVariableFactory,
			final RandomVariable initialValue,
			final RandomVariable riskFreeRate,
			final RandomVariable displacement,
			final RandomVariable volatility) {
		super();
		this.randomVariableFactory = randomVariableFactory;
		this.initialValue	= initialValue;
		this.riskFreeRate	= riskFreeRate;
		this.displacement	= displacement;
		this.volatility		= volatility;
	}

	/**
	 * Create a Monte-Carlo simulation using given time discretization.
	 *
	 * @param randomVariableFactory The RandomVariableFactory used to generate random variables from constants.
	 * @param initialValue Spot value.
	 * @param riskFreeRate The risk free rate.
	 * @param displacement The displacement parameter d.
	 * @param volatility The volatility.
	 */
	public DisplacedLognomalModel(
			final RandomVariableFactory randomVariableFactory,
			final double initialValue,
			final double riskFreeRate,
			final double displacement,
			final double volatility) {
		this(
				randomVariableFactory,
				randomVariableFactory.createRandomVariable(initialValue),
				randomVariableFactory.createRandomVariable(riskFreeRate),
				randomVariableFactory.createRandomVariable(displacement),
				randomVariableFactory.createRandomVariable(volatility)
				);
	}

	/**
	 * Create a Monte-Carlo simulation using given time discretization.
	 *
	 * @param initialValue Spot value.
	 * @param riskFreeRate The risk free rate.
	 * @param displacement The displacement parameter d.
	 * @param volatility The volatility.
	 */
	public DisplacedLognomalModel(
			final double initialValue,
			final double riskFreeRate,
			final double displacement,
			final double volatility) {
		this(new RandomVariableFromArrayFactory(), initialValue, riskFreeRate, displacement, volatility);
	}

	@Override
	public RandomVariable[] getInitialState(MonteCarloProcess process) {
		return new RandomVariable[] { initialValue.add(displacement).log() };
	}

	@Override
	public RandomVariable[] getDrift(final MonteCarloProcess process, final int timeIndex, final RandomVariable[] realizationAtTimeIndex, final RandomVariable[] realizationPredictor) {
		final RandomVariable[] drift = new RandomVariable[realizationAtTimeIndex.length];
		for(int componentIndex = 0; componentIndex<realizationAtTimeIndex.length; componentIndex++) {
			drift[componentIndex] = volatility.squared().mult(-0.5);
		}
		return drift;
	}

	@Override
	public RandomVariable[] getFactorLoading(final MonteCarloProcess process, final int timeIndex, final int component, final RandomVariable[] realizationAtTimeIndex) {
		return new RandomVariable[] { volatility };
	}

	@Override
	public RandomVariable applyStateSpaceTransform(final MonteCarloProcess process, final int timeIndex, final int componentIndex, final RandomVariable randomVariable) {
		final double time = process.getTime(timeIndex);
		return randomVariable.exp().sub(displacement).mult(riskFreeRate.mult(time).exp());
	}

	@Override
	public RandomVariable applyStateSpaceTransformInverse(final MonteCarloProcess process, final int timeIndex, final int componentIndex, final RandomVariable randomVariable) {
		final double time = process.getTime(timeIndex);
		return randomVariable.div(riskFreeRate.mult(time).exp()).add(displacement).log();
	}

	@Override
	public RandomVariable getNumeraire(MonteCarloProcess process, final double time) {
		return riskFreeRate.mult(time).exp();
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
	public DisplacedLognomalModel getCloneWithModifiedData(final Map<String, Object> dataModified) {
		/*
		 * Determine the new model parameters from the provided parameter map.
		 */
		final RandomVariableFactory newRandomVariableFactory = (RandomVariableFactory)dataModified.getOrDefault("randomVariableFactory", randomVariableFactory);

		final RandomVariable newInitialValue	= RandomVariableFactory.getRandomVariableOrDefault(newRandomVariableFactory, dataModified.get("initialValue"), initialValue);
		final RandomVariable newRiskFreeRate	= RandomVariableFactory.getRandomVariableOrDefault(newRandomVariableFactory, dataModified.get("riskFreeRate"), riskFreeRate);
		final RandomVariable newDisplacement	= RandomVariableFactory.getRandomVariableOrDefault(newRandomVariableFactory, dataModified.get("displacement"), displacement);
		final RandomVariable newVolatility		= RandomVariableFactory.getRandomVariableOrDefault(newRandomVariableFactory, dataModified.get("volatility"), volatility);

		return new DisplacedLognomalModel(newRandomVariableFactory, newInitialValue, newRiskFreeRate, newDisplacement, newVolatility);
	}


	@Override
	public String toString() {
		return "DisplacedLognomalModelExperimental [randomVariableFactory=" + randomVariableFactory + ", initialValue="
				+ initialValue + ", riskFreeRate=" + riskFreeRate + ", displacement=" + displacement + ", volatility="
				+ volatility + "]";
	}

	public RandomVariableFactory getRandomVariableFactory() {
		return randomVariableFactory;
	}

	public RandomVariable getInitialValue() {
		return initialValue;
	}

	/**
	 * Returns the risk free rate parameter of this model.
	 *
	 * @return Returns the riskFreeRate.
	 */
	public RandomVariable getRiskFreeRate() {
		return riskFreeRate;
	}

	public RandomVariable getDisplacement() {
		return displacement;
	}

	/**
	 * Returns the volatility parameter of this model.
	 *
	 * @return Returns the volatility.
	 */
	public RandomVariable getVolatility() {
		return volatility;
	}
}
