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
 * 	dS = r S dt + \sigma (d+S) dW, \quad S(0) = S_{0},
 * \]
 * \[
 * 	dN = r N dt, \quad N(0) = N_{0},
 * \]
 *
 * The class provides the model of S to an <code>{@link net.finmath.montecarlo.process.MonteCarloProcess}</code> via the specification of
 * \( f = \text{identity} \), \( \mu = \frac{exp(r \Delta t_{i}) - 1}{\Delta t_{i}} S(t_{i}) \), \( \lambda_{1,1} = \sigma \frac{exp(-2 r t_{i}) - exp(-2 r t_{i+1})}{2 r \Delta t_{i}} \), i.e.,
 * of the SDE
 * \[
 * 	dX = \mu dt + \lambda_{1,1} dW, \quad X(0) = \log(S_{0}),
 * \]
 * with \( S = X \). See {@link net.finmath.montecarlo.process.MonteCarloProcess} for the notation.
 *
 * @author Christian Fries
 * @see net.finmath.montecarlo.process.MonteCarloProcess The interface for numerical schemes.
 * @see net.finmath.montecarlo.model.ProcessModel The interface for models provinding parameters to numerical schemes.
 * @version 1.0
 */
public class DisplacedLognomalModelExperimental extends AbstractProcessModel {

	private final double initialValue;
	private final double riskFreeRate;		// Actually the same as the drift (which is not stochastic)
	private final double displacement;
	private final double volatility;

	private final RandomVariableFactory randomVariableFactory =  new RandomVariableFromArrayFactory();

	/*
	 * The interface definition requires that we provide the initial value, the drift and the volatility in terms of random variables.
	 * We construct the corresponding random variables here and will return (immutable) references to them.
	 */
	private final RandomVariable[]	initialValueVector	= new RandomVariable[1];

	/**
	 * Create a Monte-Carlo simulation using given time discretization.
	 *
	 * @param initialValue Spot value.
	 * @param riskFreeRate The risk free rate.
	 * @param displacement The displacement parameter d.
	 * @param volatility The volatility.
	 */
	public DisplacedLognomalModelExperimental(
			final double initialValue,
			final double riskFreeRate,
			final double displacement,
			final double volatility) {
		super();

		this.initialValue	= initialValue;
		this.riskFreeRate	= riskFreeRate;
		this.displacement	= displacement;
		this.volatility		= volatility;
	}

	@Override
	public RandomVariable[] getInitialState(MonteCarloProcess process) {
		if(initialValueVector[0] == null) {
			initialValueVector[0] = getRandomVariableForConstant(Math.log(initialValue+displacement));
		}

		return initialValueVector;
	}

	@Override
	public RandomVariable[] getDrift(final MonteCarloProcess process, final int timeIndex, final RandomVariable[] realizationAtTimeIndex, final RandomVariable[] realizationPredictor) {
		final RandomVariable[] drift = new RandomVariable[realizationAtTimeIndex.length];
		for(int componentIndex = 0; componentIndex<realizationAtTimeIndex.length; componentIndex++) {
			drift[componentIndex] = getRandomVariableForConstant(riskFreeRate - 0.5 * volatility * volatility - riskFreeRate * displacement);
		}
		return drift;
	}

	@Override
	public RandomVariable[] getFactorLoading(final MonteCarloProcess process, final int timeIndex, final int component, final RandomVariable[] realizationAtTimeIndex) {
		final RandomVariable volatilityOnPaths = getRandomVariableForConstant(volatility);
		return new RandomVariable[] { volatilityOnPaths };
	}

	@Override
	public RandomVariable applyStateSpaceTransform(final int componentIndex, final RandomVariable randomVariable) {
		return randomVariable.exp().sub(displacement);
	}

	@Override
	public RandomVariable applyStateSpaceTransformInverse(final int componentIndex, final RandomVariable randomVariable) {
		return randomVariable.add(displacement).log();
	}

	@Override
	public RandomVariable getNumeraire(MonteCarloProcess process, final double time) {
			final double numeraireValue = Math.exp(riskFreeRate * time);

			return getRandomVariableForConstant(numeraireValue);
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
	public DisplacedLognomalModelExperimental getCloneWithModifiedData(final Map<String, Object> dataModified) {
		/*
		 * Determine the new model parameters from the provided parameter map.
		 */
		final double	newInitialValue	= dataModified.get("initialValue") != null	? ((Number)dataModified.get("initialValue")).doubleValue() : initialValue;
		final double	newRiskFreeRate	= dataModified.get("riskFreeRate") != null	? ((Number)dataModified.get("riskFreeRate")).doubleValue() : this.getRiskFreeRate();
		final double	newDisplacement	= dataModified.get("displacement") != null	? ((Number)dataModified.get("displacement")).doubleValue()	: this.getVolatility();
		final double	newVolatility	= dataModified.get("volatility") != null	? ((Number)dataModified.get("volatility")).doubleValue()	: this.getVolatility();

		return new DisplacedLognomalModelExperimental(newInitialValue, newRiskFreeRate, newDisplacement, newVolatility);
	}

	@Override
	public String toString() {
		return super.toString() + "\n" +
				"BachelierModel:\n" +
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
		return riskFreeRate;
	}

	/**
	 * Returns the volatility parameter of this model.
	 *
	 * @return Returns the volatility.
	 */
	public double getVolatility() {
		return volatility;
	}
}
