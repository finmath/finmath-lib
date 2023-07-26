package net.finmath.climate.models.dice.submodels;

import java.util.function.BiFunction;

import net.finmath.time.TimeDiscretization;

/**
 * The function that maps \(i, \sigma(t_{i})) \) to \sigma(t_{i+1})), where \( \sigma(t) \) is the emission intensity (in kgCO2 / USD = GtCO2 / (10^12 USD)).
 *
 * The emission intensity is the factor that is applied to the GDP to get the corresponding emissions.
 *
 * The function is modelled as an exponential decay, where the decay rate decays exponentially (double exponential).
 *
 * Note: This is the function \( \sigma(t) \) from the original model, except that the division by \( (1-\mu(0)) \) is missing here.
 *
 * @author Christian Fries
 */
public class EvolutionOfEmissionIndustrialIntensity implements BiFunction<Integer, Double, Double> {

	private static double e0 = 35.85;					// Initial emissions
	private static double q0 = 105.5;					// Initial global output
	private static double sigma0 = e0/q0;				// Calculated initial emissions intensity, the 1/(1-mu0) is outside

	//	private static double mu0 = 0.03;					// Initial mitigation rate
	//	private static double sigma0 = e0/(q0*(1-mu0));		// Calculated initial emissions intensity

	private final TimeDiscretization timeDiscretization;
	private final double emissionIntensityInitial;		// sigma0;
	private final double emissionIntensityRateInitial;	// = 0.0152;		// -g	// per year
	private final double emissionIntensityRateDecay;	// exp decay rate corresponding to annual -0.001;			// -d	// per year

	/**
	 * The evolution of the emission intensity
	 * @param timeStep The size of one timeStep.
	 * @param emissionIntensityRateInitial
	 * @param emissionIntensityRateDecay
	 */
	public EvolutionOfEmissionIndustrialIntensity(TimeDiscretization timeDiscretization, double emissionIntensityInitial,
	                                           double emissionIntensityRateInitial, double emissionIntensityRateDecay) {
		super();
		this.timeDiscretization = timeDiscretization;
		this.emissionIntensityInitial = emissionIntensityInitial;
		this.emissionIntensityRateInitial = emissionIntensityRateInitial;
		this.emissionIntensityRateDecay = emissionIntensityRateDecay;
	}

	public EvolutionOfEmissionIndustrialIntensity(TimeDiscretization timeDiscretization) {
		// Parameters from original model
		this(timeDiscretization, sigma0, 0.0152, -Math.log(1-0.001));
	}

	@Override
	public Double apply(Integer timeIndex, Double emissionIntensity) {
		double time = timeDiscretization.getTime(timeIndex);
		double timeStep = timeDiscretization.getTimeStep(timeIndex);
		final double emissionIntensityRate = emissionIntensityRateInitial * Math.exp(-emissionIntensityRateDecay * time);

		return emissionIntensity*Math.exp(-emissionIntensityRate*timeStep);
	}

	public TimeDiscretization getTimeDiscretization() {
		return timeDiscretization;
	}

	public double getEmissionIntensityInitial() {
		return emissionIntensityInitial;
	}

	public double getEmissionIntensityRateInitial() {
		return emissionIntensityRateInitial;
	}

	public double getEmissionIntensityRateDecay() {
		return emissionIntensityRateDecay;
	}
}
