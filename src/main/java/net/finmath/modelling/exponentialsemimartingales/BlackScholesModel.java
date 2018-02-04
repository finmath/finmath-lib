/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.01.2004
 */
package net.finmath.modelling.exponentialsemimartingales;

import java.util.Map;

import org.apache.commons.math3.complex.Complex;

import net.finmath.fouriermethod.CharacteristicFunctionInterface;
import net.finmath.fouriermethod.models.ProcessCharacteristicFunctionInterface;
import net.finmath.montecarlo.AbstractRandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFactory;
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
 * @author Alessandro Gnoatto
 * @see net.finmath.montecarlo.process.AbstractProcessInterface The interface for numerical schemes.
 * @see net.finmath.montecarlo.model.AbstractModelInterface The interface for models provinding parameters to numerical schemes.
 */
public class BlackScholesModel extends AbstractModel implements ProcessCharacteristicFunctionInterface {

	private final RandomVariableInterface initialValue;
	private final RandomVariableInterface riskFreeRate;
	private final RandomVariableInterface volatility;

	private final AbstractRandomVariableFactory randomVariableFactory;

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
	 * @param randomVariableFactory The random variable factory used to create random variables from constants.
	 */
	public BlackScholesModel(
			RandomVariableInterface initialValue,
			RandomVariableInterface riskFreeRate,
			RandomVariableInterface volatility,
			AbstractRandomVariableFactory randomVariableFactory) {
		super();

		this.initialValue	= initialValue;
		this.riskFreeRate	= riskFreeRate;
		this.volatility		= volatility;
		this.randomVariableFactory = randomVariableFactory;
	}

	/**
	 * Create a Monte-Carlo simulation using given time discretization.
	 * 
	 * @param initialValue Spot value.
	 * @param riskFreeRate The risk free rate.
	 * @param volatility The log volatility.
	 * @param randomVariableFactory The random variable factory used to create random variables from constants.
	 */
	public BlackScholesModel(
			double initialValue,
			double riskFreeRate,
			double volatility,
			AbstractRandomVariableFactory randomVariableFactory) {
		super();

		this.initialValue	= randomVariableFactory.createRandomVariable(initialValue);
		this.riskFreeRate	= randomVariableFactory.createRandomVariable(riskFreeRate);
		this.volatility		= randomVariableFactory.createRandomVariable(volatility);
		this.randomVariableFactory = randomVariableFactory;
	}

	/**
	 * Create a Monte-Carlo simulation using given time discretization.
	 * 
	 * @param initialValue Spot value.
	 * @param riskFreeRate The risk free rate.
	 * @param volatility The log volatility.
	 */
	public BlackScholesModel(
			double initialValue,
			double riskFreeRate,
			double volatility) {
		this(initialValue, riskFreeRate, volatility, new RandomVariableFactory());
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
	public RandomVariableInterface applyStateSpaceTransformInverse(int componentIndex, RandomVariableInterface randomVariable) {
		return randomVariable.log();
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
		return randomVariableFactory.createRandomVariable(value);
	}

	@Override
	public BlackScholesModel getCloneWithModifiedData(Map<String, Object> dataModified) {
		/*
		 * Determine the new model parameters from the provided parameter map.
		 */
		double	newInitialValue	= dataModified.get("initialValue") != null	? ((Number)dataModified.get("initialValue")).doubleValue() 	: initialValue.getAverage();
		double	newRiskFreeRate	= dataModified.get("riskFreeRate") != null	? ((Number)dataModified.get("riskFreeRate")).doubleValue()	: getRiskFreeRate().getAverage();
		double	newVolatility	= dataModified.get("volatility") != null	? ((Number)dataModified.get("volatility")).doubleValue()	: getVolatility().getAverage();

		return new BlackScholesModel(newInitialValue, newRiskFreeRate, newVolatility, randomVariableFactory);
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
	 * Return the initial value of this model.
	 * 
	 * @return the initial value of this model.
	 */
	public RandomVariableInterface[] getInitialValue() {
		return new RandomVariableInterface[] { initialValue };
	}

	/**
	 * Returns the risk free rate parameter of this model.
	 *
	 * @return Returns the riskFreeRate.
	 */
	public RandomVariableInterface getRiskFreeRate() {
		return riskFreeRate;
	}

	/**
	 * Returns the volatility parameter of this model.
	 * 
	 * @return Returns the volatility.
	 */
	public RandomVariableInterface getVolatility() {
		return volatility;
	}

	@Override
	public CharacteristicFunctionInterface apply(double time) {
		return argument -> {
			Complex iargument = argument.multiply(Complex.I);
			return	iargument
					.multiply(
							iargument
							.multiply(0.5*volatility.getAverage()*volatility.getAverage()*time)
							.add(Math.log(initialValue.getAverage())-0.5*volatility.getAverage()*volatility.getAverage()*time+riskFreeRate.getAverage()*time))
					.exp();
		};
	}
}
