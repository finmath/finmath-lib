package net.finmath.climate.models.dice.submodels;

import java.util.function.Function;

import net.finmath.time.TimeDiscretization;

/**
 * The evolution of the productivity (economy)
 * \(
 * 	A(t_{i+1}) = A(t_{i}) / (1 - ga * \exp(- deltaA * t))
 * \)
 *
 * @author Christian Fries
 */
public class EvolutionOfProductivity implements Function<Integer, Function<Double, Double>> {

	private final TimeDiscretization timeDiscretization;
	private final double productivityGrowthRate;        	// ga: Initial TFP rate
	private final double productivityGrowthRateDecayRate;	// deltaA: TFP increase rate

	/**
	 * The evolution of the productivity (economy)
	 * \(
	 * 	A(t_{i+1}) = A(t_{i}) / (1 - ga * \exp(- deltaA * t))
	 * \)
	 * 
	 * @param timeDiscretization The time discretization.
	 * @param productivityGrowthRate The productivity growth rate ga per 1Y.
	 * @param productivityGrowthRateDecayRate The productivity growth decay rate per 1Y.
	 */
	public EvolutionOfProductivity(TimeDiscretization timeDiscretization, double productivityGrowthRate, double productivityGrowthRateDecayRate) {
		super();
		this.timeDiscretization = timeDiscretization;
		this.productivityGrowthRate = productivityGrowthRate;
		this.productivityGrowthRateDecayRate = productivityGrowthRateDecayRate;
	}

	public EvolutionOfProductivity(TimeDiscretization timeDiscretization) {
		// Parameters from original model: initial productivity growth 0.076 per 5 years, decaying with 0.005 per 1 year.
		this(timeDiscretization, 1-Math.pow(1-0.076,1.0/5.0), 0.005);
	}

	@Override
	public Function<Double, Double> apply(Integer timeIndex) {
		double time = timeDiscretization.getTime(timeIndex);
		double timeStep = timeDiscretization.getTimeStep(timeIndex);
		return (Double productivity) -> {
			return productivity / Math.pow(1 - productivityGrowthRate * Math.exp(-productivityGrowthRateDecayRate * time), timeStep);
		};
	}
}
