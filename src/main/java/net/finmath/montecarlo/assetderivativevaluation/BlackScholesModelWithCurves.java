/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.01.2004
 */
package net.finmath.montecarlo.assetderivativevaluation;

import java.util.Map;

import net.finmath.marketdata.model.curves.DiscountCurveInterface;
import net.finmath.montecarlo.AbstractRandomVariableFactory;
import net.finmath.montecarlo.model.AbstractModel;
import net.finmath.stochastic.RandomVariableInterface;

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
 * The class provides the model of S to an <code>{@link net.finmath.montecarlo.process.AbstractProcessInterface}</code> via the specification of
 * \( f = exp \), \( \mu = r - \frac{1}{2} \sigma^2 \), \( \lambda_{1,1} = \sigma \), i.e.,
 * of the SDE
 * \[
 * 	dX = \mu dt + \lambda_{1,1} dW, \quad X(0) = \log(S_{0}),
 * \]
 * with \( S = f(X) \). See {@link net.finmath.montecarlo.process.AbstractProcessInterface} for the notation.
 *
 * @author Christian Fries
 * @see net.finmath.montecarlo.process.AbstractProcessInterface The interface for numerical schemes.
 * @see net.finmath.montecarlo.model.AbstractModelInterface The interface for models provinding parameters to numerical schemes.
 */
public class BlackScholesModelWithCurves extends AbstractModel {

	private final RandomVariableInterface initialValue;
	private final RandomVariableInterface volatility;

	private final DiscountCurveInterface discountCurveForForwardRate;
	private final DiscountCurveInterface discountCurveForDiscountRate;

	private final AbstractRandomVariableFactory randomVariableFactory;

	// Cache for arrays provided though AbstractModel
	private final RandomVariableInterface[]	initialState;
	private final RandomVariableInterface	driftAdjustment;
	private final RandomVariableInterface[]	factorLoadings;

	/**
	 * Create a Black-Scholes specification implementing AbstractModel.
	 *
	 * @param initialValue Spot value.
	 * @param discountCurveForForwardRate The curve used for calcuation of the forward.
	 * @param volatility The log volatility.
	 * @param discountCurveForDiscountRate The curve used for calcualtion of the disocunt factor / numeraire.
	 * @param randomVariableFactory The random variable factory used to create random variables from constants.
	 */
	public BlackScholesModelWithCurves(
			RandomVariableInterface initialValue,
			DiscountCurveInterface discountCurveForForwardRate,
			RandomVariableInterface volatility,
			DiscountCurveInterface discountCurveForDiscountRate,
			AbstractRandomVariableFactory randomVariableFactory) {
		this.initialValue = initialValue;
		this.volatility = volatility;
		this.discountCurveForForwardRate = discountCurveForForwardRate;
		this.discountCurveForDiscountRate = discountCurveForDiscountRate;
		this.randomVariableFactory = randomVariableFactory;

		this.initialState = new RandomVariableInterface[] { initialValue.log() };
		this.driftAdjustment = volatility.squared().div(-2.0);
		this.factorLoadings = new RandomVariableInterface[] { volatility };
	}

	/**
	 * Create a Black-Scholes specification implementing AbstractModel.
	 *
	 * @param initialValue Spot value.
	 * @param discountCurveForForwardRate The curve used for calcuation of the forward.
	 * @param volatility The log volatility.
	 * @param discountCurveForDiscountRate The curve used for calcualtion of the disocunt factor / numeraire.
	 * @param randomVariableFactory The random variable factory used to create random variables from constants.
	 */
	public BlackScholesModelWithCurves(
			Double initialValue,
			DiscountCurveInterface discountCurveForForwardRate,
			Double volatility,
			DiscountCurveInterface discountCurveForDiscountRate,
			AbstractRandomVariableFactory randomVariableFactory) {
		this(randomVariableFactory.createRandomVariable(initialValue), discountCurveForForwardRate, randomVariableFactory.createRandomVariable(volatility), discountCurveForDiscountRate, randomVariableFactory);
	}

	@Override
	public RandomVariableInterface[] getInitialState() {
		return initialState;
	}

	@Override
	public RandomVariableInterface[] getDrift(int timeIndex, RandomVariableInterface[] realizationAtTimeIndex, RandomVariableInterface[] realizationPredictor) {
		double time = getTime(timeIndex);
		double timeNext = getTime(timeIndex+1);

		double rate = Math.log(discountCurveForForwardRate.getDiscountFactor(time) / discountCurveForForwardRate.getDiscountFactor(timeNext)) / (timeNext-time);

		return new RandomVariableInterface[] { driftAdjustment.add(rate) };
	}

	@Override
	public RandomVariableInterface[] getFactorLoading(int timeIndex, int component, RandomVariableInterface[] realizationAtTimeIndex) {
		return factorLoadings;
	}

	@Override
	public RandomVariableInterface applyStateSpaceTransform(int componentIndex, RandomVariableInterface randomVariable) {
		return randomVariable.exp();
	}

	@Override
	public RandomVariableInterface applyStateSpaceTransformInverse(int componentIndex, RandomVariableInterface randomVariable) {
		return randomVariable.log();
	}

	@Override
	public RandomVariableInterface getNumeraire(double time) {
		double discounFactorForDiscounting = discountCurveForDiscountRate.getDiscountFactor(time);

		return randomVariableFactory.createRandomVariable(1.0/discounFactorForDiscounting);
	}

	@Override
	public int getNumberOfComponents() {
		return 1;
	}

	public RandomVariableInterface getRandomVariableForConstant(double value) {
		return randomVariableFactory.createRandomVariable(value);
	}

	@Override
	public BlackScholesModelWithCurves getCloneWithModifiedData(Map<String, Object> dataModified) {
		/*
		 * Determine the new model parameters from the provided parameter map.
		 */
		RandomVariableInterface	newInitialValue	= dataModified.get("initialValue") != null	? (RandomVariableInterface)dataModified.get("initialValue") : initialValue;
		RandomVariableInterface	newVolatility	= dataModified.get("volatility") != null	? (RandomVariableInterface)dataModified.get("volatility") 	: volatility;

		return new BlackScholesModelWithCurves(newInitialValue, discountCurveForForwardRate, newVolatility, discountCurveForDiscountRate, randomVariableFactory);
	}

	@Override
	public String toString() {
		return super.toString() + "\n" +
				"BlackScholesModel:\n" +
				"  initial value...:" + getInitialValue() + "\n" +
				"  forward curve...:" + discountCurveForForwardRate + "\n" +
				"  discount curve..:" + discountCurveForDiscountRate + "\n" +
				"  volatiliy.......:" + getVolatility();
	}

	/**
	 * Return the initial value of this model.
	 *
	 * @return the initial value of this model.
	 */
	public RandomVariableInterface[] getInitialValue() {
		return new RandomVariableInterface[] { initialValue };
	}

	/**
	 * Returns the volatility parameter of this model.
	 *
	 * @return Returns the volatility.
	 */
	public RandomVariableInterface getVolatility() {
		return factorLoadings[0];
	}
}
