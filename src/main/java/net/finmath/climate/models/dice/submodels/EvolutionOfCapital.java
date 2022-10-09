package net.finmath.climate.models.dice.submodels;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * the evolution of the capital (economy)
 * \(
 * 	K(t_{i+1}) = (1-delta) K(t_{i}) + investment
 * \)
 *
 * @author Christian Fries
 */
public class EvolutionOfCapital implements Function<Double, BiFunction<Double, Double, Double>> {

	private final double timeStep;
	private final double capitalDeprecation;

	public EvolutionOfCapital(double timeStep, double capitalDeprecation) {
		super();
		this.timeStep = timeStep;
		this.capitalDeprecation = capitalDeprecation;
	}

	public EvolutionOfCapital(double timeStep) {
		// capital deprecation per 1 year: 5th root of (1-0.1) per 5 years
		this(timeStep, 1-Math.pow(1-0.1, 1/5));
	}

	@Override
	public BiFunction<Double, Double, Double> apply(Double time) {
		return (Double capital, Double investment) -> {
			return Math.pow(1.0-capitalDeprecation, timeStep) * capital + investment * timeStep;
		};
	}
}
