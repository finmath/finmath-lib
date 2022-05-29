package net.finmath.climate.models.dice.submodels;

import java.util.function.Function;

/**
 * the evolution of the productivity (economy)
 * \(
 * 	A(t_{i+1}) = A(t_{i}) / (1-ga * \exp(- deltaA * t))
 * \)
 * 
 * Note: The function depends on the time step size
 * TODO Fix time stepping
 * 
 * @author Christian Fries
 */
public class EvolutionOfProductivity implements Function<Double, Function<Double, Double>> {

	private final double timeStep;	// time step in the original model (should become a parameter)

	private final double productivityGrowthRate;        	// ga: Initial TFP rate
	private final double productivityGrowthRateDecayRate;	// deltaA: TFP increase rate

	public EvolutionOfProductivity(double timeStep, double productivityGrowthRate, double productivityGrowthRateDecayRate) {
		super();
		this.timeStep = timeStep;
		this.productivityGrowthRate = productivityGrowthRate;
		this.productivityGrowthRateDecayRate = productivityGrowthRateDecayRate;
	}

	public EvolutionOfProductivity() {
		// Parameters from original model: population growth per 5 year
		this(5.0, 0.076, 0.005);
	}

	@Override
	public Function<Double, Double> apply(Double time) {
		return (Double productivity) -> {
			return productivity / (1 - productivityGrowthRate * Math.exp(-productivityGrowthRateDecayRate * timeStep * time));
		};		
	}
}
