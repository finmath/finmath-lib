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
 * The class provides the model of S to an <code>{@link net.finmath.montecarlo.process.AbstractProcessInterface}</code> via the specification of
 * \( f = \text{identity} \), \( \mu = \frac{exp(r \Delta t_{i}) - 1}{\Delta t_{i}} S(t_{i}) \), \( \lambda_{1,1} = \sigma \frac{exp(-2 r t_{i}) - exp(-2 r t_{i+1})}{2 r \Delta t_{i}} \), i.e.,
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
public class InhomogeneousDisplacedLognomalModel extends AbstractModel {

	private final double initialValue;
	private final double riskFreeRate;		// Actually the same as the drift (which is not stochastic)
	private final double displacement;
	private final double volatility;
	
	private final boolean isUseMilsteinCorrection;
	
	/*
	 * The interface definition requires that we provide the initial value, the drift and the volatility in terms of random variables.
	 * We construct the corresponding random variables here and will return (immutable) references to them.
	 */
	private RandomVariableInterface[]	initialValueVector	= new RandomVariableInterface[1];

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
	public RandomVariableInterface[] getInitialState() {
		if(initialValueVector[0] == null) 	initialValueVector[0] = getRandomVariableForConstant(initialValue);
		
		return initialValueVector;
	}

	@Override
	public RandomVariableInterface[] getDrift(int timeIndex, RandomVariableInterface[] realizationAtTimeIndex, RandomVariableInterface[] realizationPredictor) {
		double dt = getProcess().getTimeDiscretization().getTimeStep(timeIndex);
		RandomVariableInterface[] drift = new RandomVariableInterface[realizationAtTimeIndex.length];
		for(int componentIndex = 0; componentIndex<realizationAtTimeIndex.length; componentIndex++) {
			drift[componentIndex] = realizationAtTimeIndex[componentIndex].mult((Math.exp(riskFreeRate * dt)-1)/dt);
			if(isUseMilsteinCorrection) {
				drift[componentIndex] = drift[componentIndex].add(realizationAtTimeIndex[componentIndex].add(displacement).mult(0.5*volatility*volatility * Math.exp(riskFreeRate * dt)).mult(getProcess().getStochasticDriver().getIncrement(timeIndex, 0).squared().sub(dt)));
			}
		}
		return drift;
	}

	@Override
	public RandomVariableInterface[] getFactorLoading(int timeIndex, int component, RandomVariableInterface[] realizationAtTimeIndex) {
		double dt = getProcess().getTimeDiscretization().getTimeStep(timeIndex);
		RandomVariableInterface[] volatilityOnPaths = new RandomVariableInterface[realizationAtTimeIndex.length];
		for(int componentIndex = 0; componentIndex<realizationAtTimeIndex.length; componentIndex++) {
			volatilityOnPaths[componentIndex] = realizationAtTimeIndex[componentIndex].add(displacement).mult(volatility * Math.exp(riskFreeRate * dt));
		}
		return volatilityOnPaths;
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
