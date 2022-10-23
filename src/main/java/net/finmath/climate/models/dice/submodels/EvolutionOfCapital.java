package net.finmath.climate.models.dice.submodels;

import java.util.function.BiFunction;
import java.util.function.Function;

import net.finmath.time.TimeDiscretization;

/**
 * the evolution of the capital (economy)
 * \(
 * 	K(t_{i+1}) = (1-delta) K(t_{i}) + investment
 * \)
 *
 * @author Christian Fries
 */
public class EvolutionOfCapital implements Function<Integer, BiFunction<Double, Double, Double>> {

	private final TimeDiscretization timeDiscretization;
	private final double capitalDeprecation;

	public EvolutionOfCapital(TimeDiscretization timeDiscretization, double capitalDeprecation) {
		super();
		this.timeDiscretization = timeDiscretization;
		this.capitalDeprecation = capitalDeprecation;
	}

	public EvolutionOfCapital(TimeDiscretization timeDiscretization) {
		// capital deprecation per 1 year: 5th root of (1-0.1) per 5 years
		this(timeDiscretization, -Math.log(1-0.1)/5.0);
	}

	@Override
	public BiFunction<Double, Double, Double> apply(Integer timeIndex) {
		double timeStep = timeDiscretization.getTimeStep(timeIndex);
		return (Double capital, Double investment) -> {
			return capital * Math.exp(-capitalDeprecation * timeStep) + investment * timeStep;
		};
	}
}
