/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.01.2004
 */
package net.finmath.montecarlo.assetderivativevaluation;

import java.util.Map;

import net.finmath.montecarlo.model.AbstractModel;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * This class implements a (variant of the) Bachelier model, that is,
 * it provides the drift and volatility specification
 * and performs the calculation of the numeraire (consistent with the dynamics, i.e. the drift).
 *
 * The model is
 * \[
 * 	d(S/N) = \sigma dW, \quad S(0) = S_{0},
 * \]
 * \[
 * 	dN = r N dt, \quad N(0) = N_{0},
 * \]
 * 
 * Note: This implies the dynamic
 * \[
 * 	dS = r S dt + \sigma exp(r t) dW, \quad S(0) = S_{0},
 * \]
 * for \( S \). For The model 
 * \[
 * 	dS = r S dt + \sigma dW, \quad S(0) = S_{0},
 * \]
 * see {@link net.finmath.montecarlo.assetderivativevaluation.InhomogenousBachelierModel}.
 * 
 * The model's implied Bachelier volatility for a given maturity T is
 * <code>volatility * Math.exp(riskFreeRate * optionMaturity)</code>
 * 
 * The class provides the model of S to an <code>{@link net.finmath.montecarlo.process.AbstractProcessInterface}</code> via the specification of
 * \( f = \text{identity} \), \( \mu = \frac{exp(r \Delta t_{i}) - 1}{\Delta t_{i}} S(t_{i}) \), \( \lambda_{1,1} = \sigma \), i.e.,
 * of the SDE
 * \[
 * 	dX = \mu dt + \lambda_{1,1} dW, \quad X(0) = \log(S_{0}),
 * \]
 * with \( S = X \). See {@link net.finmath.montecarlo.process.AbstractProcessInterface} for the notation.
 * 
 * @author Christian Fries
 * @see net.finmath.montecarlo.process.AbstractProcessInterface The interface for numerical schemes.
 * @see net.finmath.montecarlo.model.AbstractModelInterface The interface for models provinding parameters to numerical schemes.
 */
public class BachelierModel extends AbstractModel {

	private final double initialValue;
	private final double riskFreeRate;		// Actually the same as the drift (which is not stochastic)
	private final double volatility;

	/*
	 * The interface definition requires that we provide the initial value, the drift and the volatility in terms of random variables.
	 * We construct the corresponding random variables here and will return (immutable) references to them.
	 */
	private RandomVariableInterface[]	initialValueVector	= new RandomVariableInterface[1];

	/**
	 * Create a Monte-Carlo simulation using given time discretization.
	 * 
	 * @param initialValue Spot value.
	 * @param riskFreeRate The risk free rate.
	 * @param volatility The volatility.
	 */
	public BachelierModel(
			double initialValue,
			double riskFreeRate,
			double volatility) {
		super();

		this.initialValue	= initialValue;
		this.riskFreeRate	= riskFreeRate;
		this.volatility		= volatility;
	}

	@Override
	public RandomVariableInterface[] getInitialState() {
		if(initialValueVector[0] == null) {
			initialValueVector[0] = getRandomVariableForConstant(initialValue);
		}

		return initialValueVector;
	}

	@Override
	public RandomVariableInterface[] getDrift(int timeIndex, RandomVariableInterface[] realizationAtTimeIndex, RandomVariableInterface[] realizationPredictor) {
		double dt = getProcess().getTimeDiscretization().getTimeStep(timeIndex);
		RandomVariableInterface[] drift = new RandomVariableInterface[realizationAtTimeIndex.length];
		for(int componentIndex = 0; componentIndex<realizationAtTimeIndex.length; componentIndex++) {
			drift[componentIndex] = realizationAtTimeIndex[componentIndex].mult((Math.exp(riskFreeRate*dt)-1.0)/dt);
		}
		return drift;
	}

	@Override
	public RandomVariableInterface[] getFactorLoading(int timeIndex, int component, RandomVariableInterface[] realizationAtTimeIndex) {
		double t	= getProcess().getTime(timeIndex);
		double dt	= getProcess().getTimeDiscretization().getTimeStep(timeIndex);
		RandomVariableInterface volatilityOnPaths = getRandomVariableForConstant(volatility * Math.exp(riskFreeRate*(t+dt)));// * Math.sqrt((1-Math.exp(-2 * riskFreeRate*dt))/(2*riskFreeRate*dt)));
		return new RandomVariableInterface[] { volatilityOnPaths };
	}

	@Override
	public RandomVariableInterface applyStateSpaceTransform(int componentIndex, RandomVariableInterface randomVariable) {
		return randomVariable;
	}

	@Override
	public RandomVariableInterface applyStateSpaceTransformInverse(int componentIndex, RandomVariableInterface randomVariable) {
		return randomVariable;
	}

	@Override
	public RandomVariableInterface getNumeraire(double time) {
		double numeraireValue = Math.exp(riskFreeRate * time);

		return getRandomVariableForConstant(numeraireValue);
	}

	@Override
	public int getNumberOfComponents() {
		return 1;
	}

	public RandomVariableInterface getRandomVariableForConstant(double value) {
		return getProcess().getStochasticDriver().getRandomVariableForConstant(value);
	}

	@Override
	public BachelierModel getCloneWithModifiedData(Map<String, Object> dataModified) {
		/*
		 * Determine the new model parameters from the provided parameter map.
		 */
		double	newInitialValue	= dataModified.get("initialValue") != null	? ((Number)dataModified.get("initialValue")).doubleValue() : initialValue;
		double	newRiskFreeRate	= dataModified.get("riskFreeRate") != null	? ((Number)dataModified.get("riskFreeRate")).doubleValue() : this.getRiskFreeRate();
		double	newVolatility	= dataModified.get("volatility") != null	? ((Number)dataModified.get("volatility")).doubleValue()	: this.getVolatility();

		return new BachelierModel(newInitialValue, newRiskFreeRate, newVolatility);
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

	public double getImpliedBachelierVolatility(double maturity) {
		return volatility * Math.exp(riskFreeRate * maturity);
	}
}
