/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 25.05.2013
 */

package net.finmath.montecarlo;

import java.util.HashMap;
import java.util.Map;

import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;

/**
 * Provides a Brownian motion from given (independent) increments and performs a control of the expectation and the standard deviation.
 *
 * This class references a class providing Brownian increments \( \Delta U(t_{i}) \) and transforms them via
 * \[
 * 	\Delta W(t_{i}) = a \Delta U(t_{i}) + b
 * \]
 * such that \Delta W(t_{i}) has exact mean 0 and exact standard deviation \( \sqrt{\Delta t_{i}} \).
 *
 * @author Christian Fries
 * @version 1.0
 */
public class BrownianMotionWithControlVariate implements BrownianMotion {

	private final BrownianMotion	brownianMotion;

	private transient Map<Integer, Double> averages = new HashMap<>();
	private transient Map<Integer, Double> scalings = new HashMap<>();

	/**
	 * Create a controlled Brownian motion.
	 *
	 * @param brownianMotion The Brownian motion providing the (un-controlled) factors <i>dU<sub>j</sub></i>.
	 */
	public BrownianMotionWithControlVariate(final BrownianMotion brownianMotion) {
		super();
		this.brownianMotion	= brownianMotion;
	}

	@Override
	public RandomVariable getBrownianIncrement(final int timeIndex, final int factorIndex) {
		final RandomVariable brownianIncrement = brownianMotion.getBrownianIncrement(timeIndex, factorIndex);

		final int mapIndex = timeIndex * brownianMotion.getNumberOfFactors() + factorIndex;
		final double average = averages.computeIfAbsent(mapIndex, index -> { return brownianIncrement.getAverage();});
		final double scaling = scalings.computeIfAbsent(mapIndex, index -> { return Math.sqrt(brownianMotion.getTimeDiscretization().getTimeStep(timeIndex)) / brownianIncrement.getStandardDeviation();});

		RandomVariable brownianIncrementControlled = brownianIncrement;

		if(average != 0.0) {
			brownianIncrementControlled = brownianIncrementControlled.sub(average);
		}

		if(Double.isFinite(scaling) && scaling != 1.0) {
			brownianIncrementControlled = brownianIncrementControlled.mult(scaling);
		}

		return brownianIncrementControlled;
	}

	@Override
	public TimeDiscretization getTimeDiscretization() {
		return brownianMotion.getTimeDiscretization();
	}

	@Override
	public int getNumberOfFactors() {
		return brownianMotion.getNumberOfFactors();
	}

	@Override
	public int getNumberOfPaths() {
		return brownianMotion.getNumberOfPaths();
	}

	@Override
	public RandomVariable getRandomVariableForConstant(final double value) {
		return brownianMotion.getRandomVariableForConstant(value);
	}

	@Override
	public BrownianMotion getCloneWithModifiedSeed(final int seed) {
		return new BrownianMotionWithControlVariate(brownianMotion.getCloneWithModifiedSeed(seed));
	}

	@Override
	public BrownianMotion getCloneWithModifiedTimeDiscretization(final TimeDiscretization newTimeDiscretization) {
		return new BrownianMotionWithControlVariate(brownianMotion.getCloneWithModifiedTimeDiscretization(newTimeDiscretization));
	}

	@Override
	public RandomVariable getIncrement(final int timeIndex, final int factor) {
		return getBrownianIncrement(timeIndex, factor);
	}
}
