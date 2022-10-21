package net.finmath.climate.models.dice.submodels;

import java.util.function.Function;

import net.finmath.time.TimeDiscretization;

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
public class EvolutionOfPopulation implements Function<Integer, Function<Double, Double>> {

	private final TimeDiscretization timeDiscretization;
	private final double populationAsymptotic;	 // Asymptotic population (La)
	private final double populationGrowth;		 // Population growth (lg) (in the original model this a per-5Y)

	public EvolutionOfPopulation(TimeDiscretization timeDiscretization, double populationAsymptotic, double populationGrowth) {
		super();
		this.timeDiscretization = timeDiscretization;
		this.populationAsymptotic = populationAsymptotic;
		this.populationGrowth = populationGrowth;
	}

	public EvolutionOfPopulation(TimeDiscretization timeDiscretization) {
		this(timeDiscretization, 11500, 0.134/5); //original parameter is per 5 year time step, we rescale to 1Y
	}

	@Override
	public Function<Double, Double> apply(Integer timeIndex) {
		double timeStep = timeDiscretization.getTimeStep(timeIndex);
		return (Double population) -> {
			return population * Math.pow(populationAsymptotic/population,populationGrowth*timeStep);
		};
	}
}
