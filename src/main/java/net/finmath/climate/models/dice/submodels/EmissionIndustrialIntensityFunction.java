package net.finmath.climate.models.dice.submodels;

import java.util.function.BiFunction;

/**
 * The function that maps time to emission intensity \( \sigma(t) \) (in kgCO2 / USD = GtCO2 / (10^12 USD)).
 *
 * The emission intensity is the factor that is applied to the GDP to get the corresponding emissions.
 *
 * The function is modelled as an exponential decay, where the decay rate decays exponentially (double exponential).
 *
 * Note: This is the function \( \sigma(t) \) from the original model, except that the division by \( (1-\mu(0)) \) is missing here.
 *
 * @author Christian Fries
 */
public class EmissionIndustrialIntensityFunction implements BiFunction<Double, Double, Double> {

	private static double e0 = 35.85;					// Initial emissions
	private static double q0 = 105.5;					// Initial global output
	private static double sigma0 = e0/q0;				// Calculated initial emissions intensity, the 1/(1-mu0) is outside

	//	private static double mu0 = 0.03;					// Initial mitigation rate
	//	private static double sigma0 = e0/(q0*(1-mu0));		// Calculated initial emissions intensity

	private final double timeStep;
	private final double emissionIntensityInitial;		// sigma0;
	private final double emissionIntensityRateInitial;	// = 0.0152;		// -g	// per year
	private final double emissionIntensityRateDecay;	// exp decay rate corresponding to annual -0.001;			// -d	// per year

	/**
	 * The evolution of the emission intensity
	 * @param timeStep The size of one timeStep.
	 * @param emissionIntensityRateInitial
	 * @param emissionIntensityRateDecay
	 */
	public EmissionIndustrialIntensityFunction(double timeStep, double emissionIntensityInitial,
	                                           double emissionIntensityRateInitial, double emissionIntensityRateDecay) {
		super();
		this.timeStep = timeStep;
		this.emissionIntensityInitial = emissionIntensityInitial;
		this.emissionIntensityRateInitial = emissionIntensityRateInitial;
		this.emissionIntensityRateDecay = emissionIntensityRateDecay;
	}

	/**
	 * @deprecated
	 */
	public EmissionIndustrialIntensityFunction(double emissionIntensityInitial, double emissionIntensityRateInitial,
	                                           double emissionIntensityRateDecay) {
		this(5, emissionIntensityInitial, emissionIntensityRateInitial, emissionIntensityRateDecay);
	}

	/**
	 * @deprecated
	 */
	public EmissionIndustrialIntensityFunction() {
		// These parameters give a good approximation of the original sigma(t), when using the apply(time) function
		this(5, sigma0, 0.0152321293038455, 0.000491139147827816);
	}

	public EmissionIndustrialIntensityFunction(double timeStep) {
		// Parameters from original model
		this(timeStep, sigma0, 0.0152, -Math.log(1-0.001));
	}

	/**
	 * @deprecated
	 */
	public Double apply(Double time) {
		final double emissionIntensityRate = emissionIntensityRateInitial * Math.exp(-emissionIntensityRateDecay * time);
		final double emissionIntensity = emissionIntensityInitial * Math.exp(-emissionIntensityRate * time);

		return emissionIntensity;
	}

	@Override
	public Double apply(Double time, Double emissionIntensity) {
		final double emissionIntensityRate = emissionIntensityRateInitial * Math.exp(-emissionIntensityRateDecay * time);

		return emissionIntensity*Math.exp(-emissionIntensityRate*timeStep);
	}

}
