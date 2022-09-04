package net.finmath.climate.models.dice.submodels;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * the evolution of the capital (economy)
 * \(
 * 	K(t_{i+1}) = (1-delta) K(t_{i}) + investment
 * \)
 *
 * Note: The function depends on the time step size
 * TODO Fix time stepping
 *
 * @author Christian Fries
 */
public class EvolutionOfCapital implements Function<Double, BiFunction<Double, Double, Double>> {

	private static double timeStep = 5.0;	// time step in the original model (should become a parameter)

	private final double capitalDeprecation;

	public EvolutionOfCapital(double capitalDeprecation) {
		super();
		this.capitalDeprecation = capitalDeprecation;
	}

	public EvolutionOfCapital() {
		// Parameters from original model: capital deprecation per 5 year: 10%
		this(0.1);
	}

	@Override
	public BiFunction<Double, Double, Double> apply(Double time) {
		return (Double capital, Double investment) -> {
			return (1.0-capitalDeprecation) * capital + investment * timeStep;
		};
	}

}
