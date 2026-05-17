package net.finmath.climate.models.dice.submodels;

import java.util.function.Function;

/**
 * The function models the external forcing as a linear function capped at 1.0
 *
 * @author Maximilian Singhof
 */
public class ForcingExternalFunction  implements Function<Double, Double> {

	private final double intercept;
	private final double linear;

	public ForcingExternalFunction(double intercept, double linear) {
		this.intercept = intercept;
		this.linear = linear;
	}

	public ForcingExternalFunction() {
		// Parameters from original model
		this(0.5, 0.5/(5*17));
	}

	@Override
	public Double apply(Double time) {
		return Math.min(intercept + linear*time,1.0);
	}
}

