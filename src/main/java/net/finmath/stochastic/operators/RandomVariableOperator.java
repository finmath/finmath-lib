package net.finmath.stochastic.operators;

import net.finmath.stochastic.RandomOperator;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;

/**
 * A factory for useful {@link RandomOperator}s.
 * 
 * @author Christian Fries
 */
public class RandomVariableOperator {

	/**
	 * The expected short fall operator for a given percentageLevel.
	 * 
	 * @param percentageLevel The percentage level.
	 * @return The operator that maps a randomVariable to its expected short fall (wrapped in a random variable).
	 */
	static RandomOperator expectedShortFall(final Double percentageLevel) {

		if(percentageLevel < 0 || percentageLevel > 1) throw new IllegalArgumentException("");
		
		return (RandomVariable x) -> {
			// Special case: constant will result in that constant
			if(x.isDeterministic() || x.getVariance() == 0) return x;

			double quantileValue = x.getQuantile(percentageLevel);
			RandomVariable indicator = x.sub(quantileValue).choose(Scalar.of(0.0), Scalar.of(1.0));
			RandomVariable averageVar = x.mult(indicator).average().div(percentageLevel);

			return averageVar;
		};
	}
}
