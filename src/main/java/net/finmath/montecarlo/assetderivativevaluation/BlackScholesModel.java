/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 20.01.2004
 */
package net.finmath.montecarlo.assetderivativevaluation;

import java.util.Map;

import net.finmath.montecarlo.RandomVariable;
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
public class BlackScholesModel extends AbstractModel {

	private final RandomVariableInterface initialValue;
	private final RandomVariableInterface riskFreeRate;
	private final RandomVariableInterface volatility;
		
	/*
	 * The interface definition requires that we provide the initial value, the drift and the volatility in terms of random variables.
	 * We construct the corresponding random variables here and will return (immutable) references to them.
	 */
	private RandomVariableInterface[]	initialState;
	private RandomVariableInterface		drift;
	private RandomVariableInterface[]	factorLoadings;
	
	/**
	 * Create a Monte-Carlo simulation using given time discretization.
	 * 
	 * @param initialValue Spot value.
	 * @param riskFreeRate The risk free rate.
	 * @param volatility The log volatility.
	 */
	public BlackScholesModel(
			RandomVariableInterface initialValue,
			RandomVariableInterface riskFreeRate,
			RandomVariableInterface volatility) {
		super();

		this.initialValue	= initialValue;
		this.riskFreeRate	= riskFreeRate;
		this.volatility		= volatility;
	}

	/**
	 * Create a Monte-Carlo simulation using given time discretization.
	 * 
	 * @param initialValue Spot value.
	 * @param riskFreeRate The risk free rate.
	 * @param volatility The log volatility.
	 * @deprecated
	 */
	public BlackScholesModel(
			double initialValue,
			double riskFreeRate,
			double volatility) {
		super();

		this.initialValue	= new RandomVariable(initialValue);
		this.riskFreeRate	= new RandomVariable(riskFreeRate);
		this.volatility		= new RandomVariable(volatility);
	}

	@Override
	public RandomVariableInterface[] getInitialState() {
		// Since the underlying process is configured to simulate log(S), the initial value and the drift are transformed accordingly.
		if(initialState == null) 	initialState = new RandomVariableInterface[] { initialValue.log() };
		
		return initialState;
	}

	@Override
	public RandomVariableInterface[] getDrift(int timeIndex, RandomVariableInterface[] realizationAtTimeIndex, RandomVariableInterface[] realizationPredictor) {
		// Since the underlying process is configured to simulate log(S), the initial value and the drift are transformed accordingly.
		if(drift == null) drift = riskFreeRate.sub(volatility.squared().div(2));
		return new RandomVariableInterface[] { drift };
	}

	@Override
	public RandomVariableInterface[] getFactorLoading(int timeIndex, int component, RandomVariableInterface[] realizationAtTimeIndex) {
		if(factorLoadings == null) factorLoadings = new RandomVariableInterface[] { volatility };
		return factorLoadings;
	}

	@Override
	public RandomVariableInterface applyStateSpaceTransform(int componentIndex, RandomVariableInterface randomVariable) {
		return randomVariable.exp();
	}

	@Override
	public RandomVariableInterface getNumeraire(double time) {
		return riskFreeRate.mult(time).exp();
	}

	@Override
	public int getNumberOfComponents() {
		return 1;
	}

	public RandomVariableInterface getRandomVariableForConstant(double value) {
		return getProcess().getStochasticDriver().getRandomVariableForConstant(value);
	}

	@Override
	public BlackScholesModel getCloneWithModifiedData(Map<String, Object> dataModified) {
		/*
		 * Determine the new model parameters from the provided parameter map.
		 */
		double	newInitialValue	= dataModified.get("initialValue") != null	? ((Number)dataModified.get("initialValue")).doubleValue() : initialValue.getAverage();
		double	newRiskFreeRate	= dataModified.get("riskFreeRate") != null	? ((Number)dataModified.get("riskFreeRate")).doubleValue() : this.getRiskFreeRate();
		double	newVolatility	= dataModified.get("volatility") != null	? ((Number)dataModified.get("volatility")).doubleValue()	: this.getVolatility();

		return new BlackScholesModel(newInitialValue, newRiskFreeRate, newVolatility);
	}

	@Override
	public String toString() {
		return super.toString() + "\n" +
				"BlackScholesModel:\n" +
				"  initial value...:" + initialValue + "\n" +
				"  risk free rate..:" + riskFreeRate + "\n" +
				"  volatiliy.......:" + volatility;
	}

	/**
	 * Returns the risk free rate parameter of this model.
	 *
	 * @return Returns the riskFreeRate.
	 */
	public double getRiskFreeRate() {
		return riskFreeRate.getAverage();
	}

	/**
	 * Returns the volatility parameter of this model.
	 * 
	 * @return Returns the volatility.
	 */
	public double getVolatility() {
		return volatility.getAverage();
	}
}
