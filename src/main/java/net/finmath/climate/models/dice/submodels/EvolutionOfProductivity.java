package net.finmath.climate.models.dice.submodels;

import java.util.function.Function;

/**
 * The evolution of the productivity (economy)
 * \(
 * 	A(t_{i+1}) = A(t_{i}) / (1 - ga * \exp(- deltaA * t))
 * \)
 *
 * @author Christian Fries
 */
public class EvolutionOfProductivity implements Function<Double, Function<Double, Double>> {

	private final double timeStep;
	private final double productivityGrowthRate;        	// ga: Initial TFP rate
	private final double productivityGrowthRateDecayRate;	// deltaA: TFP increase rate

	/**
	 * The evolution of the productivity (economy)
	 * \(
	 * 	A(t_{i+1}) = A(t_{i}) / (1 - ga * \exp(- deltaA * t))
	 * \)
	 * 
	 * @param timeStep The size of one timeStep.
	 * @param productivityGrowthRate The productivity growth rate ga per 1Y.
	 * @param productivityGrowthRateDecayRate The productivity growth decay rate per 1Y.
	 */
	public EvolutionOfProductivity(double timeStep, double productivityGrowthRate, double productivityGrowthRateDecayRate) {
		super();
		this.timeStep = timeStep;
		this.productivityGrowthRate = productivityGrowthRate;
		this.productivityGrowthRateDecayRate = productivityGrowthRateDecayRate;
	}

	public EvolutionOfProductivity(double timeStep) {
		// Parameters from original model: initial productivity growth 0.076 per 5 years, decaying with 0.005 per 1 year.
		this(timeStep, 1-Math.pow(1-0.076,1.0/5.0), 0.005);
	}

	@Override
	public Function<Double, Double> apply(Double time) {
		return (Double productivity) -> {
			return productivity / Math.pow(1 - productivityGrowthRate * Math.exp(-productivityGrowthRateDecayRate * time), timeStep);
		};
	}
}
