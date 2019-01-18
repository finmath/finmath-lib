/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.01.2004
 */
package net.finmath.montecarlo.assetderivativevaluation;

import java.util.Map;

import net.finmath.montecarlo.model.AbstractProcessModel;
import net.finmath.stochastic.RandomVariable;

/**
 * This class implements an inhomogeneous displaced log-normal model, that is, it provides the drift and volatility specification
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
public class InhomogeneousDisplacedLognomalModel extends AbstractProcessModel {

	private final double initialValue;
	private final double riskFreeRate;		// Actually the same as the drift (which is not stochastic)
	private final double displacement;
	private final double volatility;

	private final boolean isUseMilsteinCorrection;

	/*
	 * The interface definition requires that we provide the initial value, the drift and the volatility in terms of random variables.
	 * We construct the corresponding random variables here and will return (immutable) references to them.
	 */
	private RandomVariable[]	initialValueVector	= new RandomVariable[1];

	/**
	 * Create a blended normal/lognormal model.
	 *
	 * @param initialValue Spot value.
	 * @param riskFreeRate The risk free rate.
	 * @param displacement The displacement parameter d.
	 * @param volatility The volatility.
	 * @param isUseMilsteinCorrection If true, the drift will include the Milstein correction (making an Euler scheme a Milstein scheme).
	 */
	public InhomogeneousDisplacedLognomalModel(
			double initialValue,
			double riskFreeRate,
			double displacement,
			double volatility,
			boolean isUseMilsteinCorrection) {
		super();

		this.initialValue	= initialValue;
		this.riskFreeRate	= riskFreeRate;
		this.displacement	= displacement;
		this.volatility		= volatility;

		this.isUseMilsteinCorrection = isUseMilsteinCorrection;
	}

	/**
	 * Create a blended normal/lognormal model.
	 *
	 * @param initialValue Spot value.
	 * @param riskFreeRate The risk free rate.
	 * @param displacement The displacement parameter d.
	 * @param volatility The volatility.
	 */
	public InhomogeneousDisplacedLognomalModel(
			double initialValue,
			double riskFreeRate,
			double displacement,
			double volatility) {
		this(initialValue, riskFreeRate, displacement, volatility, false);
	}

	@Override
	public RandomVariable[] getInitialState() {
		if(initialValueVector[0] == null) {
			initialValueVector[0] = getRandomVariableForConstant(initialValue);
		}

		return initialValueVector;
	}

	@Override
	public RandomVariable[] getDrift(int timeIndex, RandomVariable[] realizationAtTimeIndex, RandomVariable[] realizationPredictor) {
		double dt = getProcess().getTimeDiscretization().getTimeStep(timeIndex);
		RandomVariable[] drift = new RandomVariable[realizationAtTimeIndex.length];
		for(int componentIndex = 0; componentIndex<realizationAtTimeIndex.length; componentIndex++) {
			drift[componentIndex] = realizationAtTimeIndex[componentIndex].mult((Math.exp(riskFreeRate * dt)-1)/dt);
			if(isUseMilsteinCorrection) {
				drift[componentIndex] = drift[componentIndex].add(realizationAtTimeIndex[componentIndex].add(displacement).mult(0.5*volatility*volatility * Math.exp(riskFreeRate * dt)).mult(getProcess().getStochasticDriver().getIncrement(timeIndex, 0).squared().sub(dt)));
			}
		}
		return drift;
	}

	@Override
	public RandomVariable[] getFactorLoading(int timeIndex, int component, RandomVariable[] realizationAtTimeIndex) {
		double dt = getProcess().getTimeDiscretization().getTimeStep(timeIndex);
		RandomVariable[] volatilityOnPaths = new RandomVariable[realizationAtTimeIndex.length];
		for(int componentIndex = 0; componentIndex<realizationAtTimeIndex.length; componentIndex++) {
			volatilityOnPaths[componentIndex] = realizationAtTimeIndex[componentIndex].add(displacement).mult(volatility * Math.exp(riskFreeRate * dt));
		}
		return volatilityOnPaths;
	}

	@Override
	public RandomVariable applyStateSpaceTransform(int componentIndex, RandomVariable randomVariable) {
		return randomVariable;
	}

	@Override
	public RandomVariable applyStateSpaceTransformInverse(int componentIndex, RandomVariable randomVariable) {
		return randomVariable;
	}

	@Override
	public RandomVariable getNumeraire(double time) {
		double numeraireValue = Math.exp(riskFreeRate * time);

		return getRandomVariableForConstant(numeraireValue);
	}

	@Override
	public int getNumberOfComponents() {
		return 1;
	}

	@Override
	public RandomVariable getRandomVariableForConstant(double value) {
		return getProcess().getStochasticDriver().getRandomVariableForConstant(value);
	}

	@Override
	public InhomogeneousDisplacedLognomalModel getCloneWithModifiedData(Map<String, Object> dataModified) {
		/*
		 * Determine the new model parameters from the provided parameter map.
		 */
		double	newInitialValue	= dataModified.get("initialValue") != null	? ((Number)dataModified.get("initialValue")).doubleValue() : initialValue;
		double	newRiskFreeRate	= dataModified.get("riskFreeRate") != null	? ((Number)dataModified.get("riskFreeRate")).doubleValue() : this.getRiskFreeRate();
		double	newDisplacement	= dataModified.get("displacement") != null	? ((Number)dataModified.get("displacement")).doubleValue()	: this.getVolatility();
		double	newVolatility	= dataModified.get("volatility") != null	? ((Number)dataModified.get("volatility")).doubleValue()	: this.getVolatility();

		return new InhomogeneousDisplacedLognomalModel(newInitialValue, newRiskFreeRate, newDisplacement, newVolatility, isUseMilsteinCorrection);
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
