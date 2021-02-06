/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.01.2004
 */
package net.finmath.montecarlo.assetderivativevaluation.models;

import java.util.Map;

import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.montecarlo.RandomVariableFactory;
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
 * 	dN = r N dt, \quad N(0) = N_{0},
 * \]
 *
 * The class provides the model of S to an <code>{@link net.finmath.montecarlo.process.MonteCarloProcess}</code> via the specification of
 * \( f = exp \), \( \mu = r - \frac{1}{2} \sigma^2 \), \( \lambda_{1,1} = \sigma \), i.e.,
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
public class BlackScholesModelWithCurves extends AbstractProcessModel {

	private final RandomVariable initialValue;
	private final RandomVariable volatility;

	private final DiscountCurve discountCurveForForwardRate;
	private final DiscountCurve discountCurveForDiscountRate;

	private final RandomVariableFactory randomVariableFactory;

	// Cache for arrays provided though AbstractProcessModel
	private final RandomVariable[]	initialState;
	private final RandomVariable	driftAdjustment;
	private final RandomVariable[]	factorLoadings;

	/**
	 * Create a Black-Scholes specification implementing AbstractProcessModel.
	 *
	 * @param initialValue Spot value.
	 * @param discountCurveForForwardRate The curve used for calcuation of the forward.
	 * @param volatility The log volatility.
	 * @param discountCurveForDiscountRate The curve used for calcualtion of the disocunt factor / numeraire.
	 * @param randomVariableFactory The random variable factory used to create random variables from constants.
	 */
	public BlackScholesModelWithCurves(
			final RandomVariable initialValue,
			final DiscountCurve discountCurveForForwardRate,
			final RandomVariable volatility,
			final DiscountCurve discountCurveForDiscountRate,
			final RandomVariableFactory randomVariableFactory) {
		this.initialValue = initialValue;
		this.volatility = volatility;
		this.discountCurveForForwardRate = discountCurveForForwardRate;
		this.discountCurveForDiscountRate = discountCurveForDiscountRate;
		this.randomVariableFactory = randomVariableFactory;

		initialState = new RandomVariable[] { initialValue.log() };
		driftAdjustment = volatility.squared().div(-2.0);
		factorLoadings = new RandomVariable[] { volatility };
	}

	/**
	 * Create a Black-Scholes specification implementing AbstractProcessModel.
	 *
	 * @param initialValue Spot value.
	 * @param discountCurveForForwardRate The curve used for calcuation of the forward.
	 * @param volatility The log volatility.
	 * @param discountCurveForDiscountRate The curve used for calcualtion of the disocunt factor / numeraire.
	 * @param randomVariableFactory The random variable factory used to create random variables from constants.
	 */
	public BlackScholesModelWithCurves(
			final Double initialValue,
			final DiscountCurve discountCurveForForwardRate,
			final Double volatility,
			final DiscountCurve discountCurveForDiscountRate,
			final RandomVariableFactory randomVariableFactory) {
		this(randomVariableFactory.createRandomVariable(initialValue), discountCurveForForwardRate, randomVariableFactory.createRandomVariable(volatility), discountCurveForDiscountRate, randomVariableFactory);
	}

	@Override
	public RandomVariable[] getInitialState(MonteCarloProcess process) {
		return initialState;
	}

	@Override
	public RandomVariable[] getDrift(final MonteCarloProcess process, final int timeIndex, final RandomVariable[] realizationAtTimeIndex, final RandomVariable[] realizationPredictor) {
		final double time = process.getTime(timeIndex);
		final double timeNext = process.getTime(timeIndex+1);

		final double rate = Math.log(discountCurveForForwardRate.getDiscountFactor(time) / discountCurveForForwardRate.getDiscountFactor(timeNext)) / (timeNext-time);

		return new RandomVariable[] { driftAdjustment.add(rate) };
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
		final double discounFactorForDiscounting = discountCurveForDiscountRate.getDiscountFactor(time);

		return randomVariableFactory.createRandomVariable(1.0/discounFactorForDiscounting);
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
	public BlackScholesModelWithCurves getCloneWithModifiedData(final Map<String, Object> dataModified) {
		/*
		 * Determine the new model parameters from the provided parameter map.
		 */
		final RandomVariable	newInitialValue	= dataModified.get("initialValue") != null	? (RandomVariable)dataModified.get("initialValue") : initialValue;
		final RandomVariable	newVolatility	= dataModified.get("volatility") != null	? (RandomVariable)dataModified.get("volatility") 	: volatility;

		return new BlackScholesModelWithCurves(newInitialValue, discountCurveForForwardRate, newVolatility, discountCurveForDiscountRate, randomVariableFactory);
	}

	@Override
	public String toString() {
		return super.toString() + "\n" +
				"BlackScholesModel:\n" +
				"  initial value...:" + initialValue + "\n" +
				"  forward curve...:" + discountCurveForForwardRate + "\n" +
				"  discount curve..:" + discountCurveForDiscountRate + "\n" +
				"  volatiliy.......:" + getVolatility();
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
	 * Returns the volatility parameter of this model.
	 *
	 * @return Returns the volatility.
	 */
	public RandomVariable getVolatility() {
		return factorLoadings[0];
	}
}
