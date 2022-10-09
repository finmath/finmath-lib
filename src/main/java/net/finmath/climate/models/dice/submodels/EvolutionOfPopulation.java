package net.finmath.climate.models.dice.submodels;

import java.util.function.Function;

/**
 * the evolution of the population (economy)
 * \(
 * 	L(t_{i+1}) = L(t_{i}) * (L(\infty)/L(t_{i})^{g}
 * \)
 *
 * Note: The function depends on the time step size
 * TODO Fix time stepping
 *
 * @author Christian Fries
 */
public class EvolutionOfPopulation implements Function<Double, Function<Double, Double>> {

	private final double timeStep;	// time step in the original model (should become a parameter)
	private final double populationAsymptotic;	 // Asymptotic population (La)
	private final double populationGrowth;		 // Population growth (lg) (in the original model this a per-5Y)

	public EvolutionOfPopulation(double timeStep, double populationAsymptotic, double populationGrowth) {
		super();
		this.timeStep = timeStep;
		this.populationAsymptotic = populationAsymptotic;
		this.populationGrowth = populationGrowth;
	}

	public EvolutionOfPopulation(double timeStep) {
		this(timeStep, 11500, 0.134/5); //original parameter is per 5 year time step, we rescale to 1Y
	}

	@Override
	public Function<Double, Double> apply(Double time) {
		return (Double population) -> {
			return population * Math.pow(populationAsymptotic/population,populationGrowth*timeStep);
		};
	}
}
