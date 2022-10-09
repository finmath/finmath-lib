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

	private final double timeStep;
	private final double productivityGrowthRate;        	// ga: Initial TFP rate
	private final double productivityGrowthRateDecayRate;	// deltaA: TFP increase rate

	public EvolutionOfProductivity(double timeStep, double productivityGrowthRate, double productivityGrowthRateDecayRate) {
		super();
		this.timeStep = timeStep;
		this.productivityGrowthRate = productivityGrowthRate;
		this.productivityGrowthRateDecayRate = productivityGrowthRateDecayRate;
	}

	public EvolutionOfProductivity(double timeStep) {
		// Parameters from original model: initial productivity growth 0.076, decaying with 0.005 per 5 years, thus 0.001 per 1 year
		// TODO reparametrization to timeStep only approximately correct
		//		this(0.076, 0.001);  // TODO eearlier version missed the /5 - check impact
		this(timeStep, 1-Math.pow(1-0.076,1/5), 0.001);
	}

	@Override
	public Function<Double, Double> apply(Double time) {
		return (Double productivity) -> {
			return productivity / Math.pow(1 - productivityGrowthRate * Math.exp(-productivityGrowthRateDecayRate * time), timeStep);
		};
	}
}
