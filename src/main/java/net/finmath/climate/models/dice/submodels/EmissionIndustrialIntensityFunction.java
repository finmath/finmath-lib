package net.finmath.climate.models.dice.submodels;

import java.util.function.Function;

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
public class EmissionIndustrialIntensityFunction implements Function<Double, Double> {

	private static double e0 = 35.85;					// Initial emissions
	private static double q0 = 105.5;					// Initial global output
	private static double sigma0 = e0/q0;				// Calculated initial emissions intensity, the 1/(1-mu0) is outside

	//	private static double mu0 = 0.03;					// Initial mitigation rate
	//	private static double sigma0 = e0/(q0*(1-mu0));		// Calculated initial emissions intensity

	private final double emissionIntensityInitial;		// sigma0;
	private final double emissionIntensityRateInitial;	// = 0.0152;		// -g	// per year
	private final double emissionIntensityRateDecay;	// = 0.001;			// -d	// per year

	/**
	 * 
	 * @param emissionIntensityInitial The initial emission intensity. Unit: GtCO2 / (10^12 USD)
	 * @param emissionIntensityRateInitial
	 * @param emissionIntensityRateDecay
	 */
	public EmissionIndustrialIntensityFunction(double emissionIntensityInitial, double emissionIntensityRateInitial, double emissionIntensityRateDecay) {
		super();
		this.emissionIntensityInitial = emissionIntensityInitial;
		this.emissionIntensityRateInitial = emissionIntensityRateInitial;
		this.emissionIntensityRateDecay = emissionIntensityRateDecay;
	}

	public EmissionIndustrialIntensityFunction() {
		// Parameters from original model
		this(sigma0, 0.0152, -Math.log(1-0.001));
	}

	@Override
	public Double apply(Double time) {
		final double emissionIntensityRate = emissionIntensityRateInitial * Math.exp(-emissionIntensityRateDecay * time);
		final double emissionIntensity = emissionIntensityInitial * Math.exp(-emissionIntensityRate * time);

		return emissionIntensity;
	}
}
