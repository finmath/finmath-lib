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
 * This class implements an inhomogeneous displaced log-normal model, that is, it provides the drift and volatility specification
 * and performs the calculation of the numeraire (consistent with the dynamics, i.e. the drift).
 *
 * The model is
 * \[
 * 	\mathrm{d}S = r S dt + \sigma (S + d) \mathrm{d}W, \quad S(0) = S_{0},
 * \]
 * \[
 * 	\mathrm{d}N = r N \mathrm{d}t, \quad N(0) = N_{0},
 * \]
 *
 * Note that
 * \[
 *	\mathrm{d}(S/N) = \sigma (S/N + d/N) \mathrm{d}W
 * \]
 * i.e.
 * \[
 *	\mathrm{d}(S/N + d/N) = -r d/N dt + \sigma (S/N + d/N) \mathrm{d}W
 * \]
 *
 * The class provides the model of S to an <code>{@link net.finmath.montecarlo.process.MonteCarloProcess}</code>
 * via the specification of (via X = S/N+d/N)
 * \( S = f(X) = N X - d \), \( \mu = d \frac{exp(- r t_2) - exp(- r t_1)}{t_2-t_1} \), \( \lambda_{1,1} = \sigma X \),
 * of the SDE
 * \[
 * 	dX = \mu dt + \lambda_{1,1} dW, \quad X(0) = S_{0} + e,
 * \]
 * with \( N(0) = 1 \). See {@link net.finmath.montecarlo.process.MonteCarloProcess} for the notation.
 *
 * @author Christian Fries
 * @see net.finmath.montecarlo.process.MonteCarloProcess The interface for numerical schemes.
 * @see net.finmath.montecarlo.model.ProcessModel The interface for models provinding parameters to numerical schemes.
 * @version 1.0
 */
public class InhomogeneousDisplacedLognomalModel extends AbstractProcessModel {

	private final RandomVariableFactory randomVariableFactory;

	private final RandomVariable initialValue;
	private final RandomVariable riskFreeRate;		// Actually the same as the drift (which is not stochastic)
	private final RandomVariable displacement;
	private final RandomVariable volatility;

	private final boolean isUseMilsteinCorrection;

	/**
	 * Create a blended normal/lognormal model.
	 *
	 * @param randomVariableFactory The RandomVariableFactory used to generate random variables from constants.
	 * @param initialValue Spot value.
	 * @param riskFreeRate The risk free rate.
	 * @param displacement The displacement parameter d.
	 * @param volatility The volatility.
	 * @param isUseMilsteinCorrection If true, a Milstein scheme correction is applied in the drift.
	 */
	public InhomogeneousDisplacedLognomalModel(
			final RandomVariableFactory randomVariableFactory,
			final RandomVariable initialValue,
			final RandomVariable riskFreeRate,
			final RandomVariable displacement,
			final RandomVariable volatility,
			final boolean isUseMilsteinCorrection) {
		super();
		this.randomVariableFactory = randomVariableFactory;
		this.initialValue	= initialValue;
		this.riskFreeRate	= riskFreeRate;
		this.displacement	= displacement;
		this.volatility		= volatility;
		this.isUseMilsteinCorrection = isUseMilsteinCorrection;
	}

	/**
	 * Create a blended normal/lognormal model.
	 *
	 * @param randomVariableFactory The RandomVariableFactory used to generate random variables from constants.
	 * @param initialValue Spot value.
	 * @param riskFreeRate The risk free rate.
	 * @param displacement The displacement parameter d.
	 * @param volatility The volatility.
	 * @param isUseMilsteinCorrection If true, the drift will include the Milstein correction (making an Euler scheme a Milstein scheme).
	 */
	public InhomogeneousDisplacedLognomalModel(
			final RandomVariableFactory randomVariableFactory,
			final double initialValue,
			final double riskFreeRate,
			final double displacement,
			final double volatility,
			final boolean isUseMilsteinCorrection) {
		this(
				randomVariableFactory,
				randomVariableFactory.createRandomVariable(initialValue),
				randomVariableFactory.createRandomVariable(riskFreeRate),
				randomVariableFactory.createRandomVariable(displacement),
				randomVariableFactory.createRandomVariable(volatility),
				isUseMilsteinCorrection);
	}

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
			final double initialValue,
			final double riskFreeRate,
			final double displacement,
			final double volatility,
			final boolean isUseMilsteinCorrection) {
		this(new RandomVariableFromArrayFactory(), initialValue, riskFreeRate, displacement, volatility, isUseMilsteinCorrection);
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
			final double initialValue,
			final double riskFreeRate,
			final double displacement,
			final double volatility) {
		this(initialValue, riskFreeRate, displacement, volatility, false);
	}

	@Override
	public RandomVariable[] getInitialState(MonteCarloProcess process) {
		return new RandomVariable[] { initialValue.add(displacement) };
	}

	@Override
	public RandomVariable[] getDrift(final MonteCarloProcess process, final int timeIndex, final RandomVariable[] realizationAtTimeIndex, final RandomVariable[] realizationPredictor) {
		final double time = process.getTimeDiscretization().getTime(timeIndex);
		final double timeNext = process.getTimeDiscretization().getTime(timeIndex+1);
		final double dt = timeNext - time;
		final RandomVariable[] drift = new RandomVariable[realizationAtTimeIndex.length];
		for(int componentIndex = 0; componentIndex<realizationAtTimeIndex.length; componentIndex++) {
			drift[componentIndex] = displacement.mult(riskFreeRate.mult(-timeNext).exp().sub(riskFreeRate.mult(-time).exp()).div(timeNext-time));
			if(isUseMilsteinCorrection) {
				/*
				 * Note: The Milstein corrections assume that the model has a single factor.
				 * While this is true for this model, in general you have to loop over all factors.
				 */
				drift[componentIndex] = drift[componentIndex].add(
						getFactorLoading(process, timeIndex, componentIndex, realizationAtTimeIndex)[0]
								.mult(volatility).div(2)
								.mult(process.getStochasticDriver().getIncrement(timeIndex, 0).squared().sub(dt)));
			}
		}
		return drift;
	}

	@Override
	public RandomVariable[] getFactorLoading(final MonteCarloProcess process, final int timeIndex, final int component, final RandomVariable[] realizationAtTimeIndex) {
		final RandomVariable[] volatilityOnPaths = new RandomVariable[realizationAtTimeIndex.length];
		for(int componentIndex = 0; componentIndex<realizationAtTimeIndex.length; componentIndex++) {
			volatilityOnPaths[componentIndex] = applyStateSpaceTransformInverse(process, timeIndex, componentIndex, realizationAtTimeIndex[componentIndex]).mult(volatility);
		}
		return volatilityOnPaths;
	}

	@Override
	public RandomVariable applyStateSpaceTransform(final MonteCarloProcess process, final int timeIndex, final int componentIndex, final RandomVariable randomVariable) {
		return randomVariable.mult(riskFreeRate.mult(process.getTime(timeIndex)).exp()).sub(displacement);
	}

	@Override
	public RandomVariable applyStateSpaceTransformInverse(final MonteCarloProcess process, final int timeIndex, final int componentIndex, final RandomVariable randomVariable) {
		return randomVariable.add(displacement).div(riskFreeRate.mult(process.getTime(timeIndex)).exp());
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
	public InhomogeneousDisplacedLognomalModel getCloneWithModifiedData(final Map<String, Object> dataModified) {
		/*
		 * Determine the new model parameters from the provided parameter map.
		 */
		final RandomVariableFactory newRandomVariableFactory = (RandomVariableFactory)dataModified.getOrDefault("randomVariableFactory", randomVariableFactory);

		final RandomVariable newInitialValue	= RandomVariableFactory.getRandomVariableOrDefault(newRandomVariableFactory, dataModified.get("initialValue"), initialValue);
		final RandomVariable newRiskFreeRate	= RandomVariableFactory.getRandomVariableOrDefault(newRandomVariableFactory, dataModified.get("riskFreeRate"), riskFreeRate);
		final RandomVariable newDisplacement	= RandomVariableFactory.getRandomVariableOrDefault(newRandomVariableFactory, dataModified.get("displacement"), displacement);
		final RandomVariable newVolatility		= RandomVariableFactory.getRandomVariableOrDefault(newRandomVariableFactory, dataModified.get("volatility"), volatility);

		return new InhomogeneousDisplacedLognomalModel(newRandomVariableFactory, newInitialValue, newRiskFreeRate, newDisplacement, newVolatility, isUseMilsteinCorrection);
	}

	@Override
	public String toString() {
		return "InhomogeneousDisplacedLognomalModel [randomVariableFactory=" + randomVariableFactory + ", initialValue="
				+ initialValue + ", riskFreeRate=" + riskFreeRate + ", displacement=" + displacement + ", volatility="
				+ volatility + ", isUseMilsteinCorrection=" + isUseMilsteinCorrection + "]";
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
