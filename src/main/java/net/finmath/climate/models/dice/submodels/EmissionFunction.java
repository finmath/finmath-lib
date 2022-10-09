package net.finmath.climate.models.dice.submodels;

import java.util.function.BiFunction;

/**
 * The function that maps economicOutput to emission at a given time in GtCO2 / year
 * \(
 * 	(t, Y) \mapsto E(t,Y)
 * \)
 * where Y is the GDP.
 *
 * Note: The emissions is in GtCO2 / year.
 *
 * Note: The function depends on the time step size
 * TODO Change parameter to per year.
 *
 * @author Christian Fries
 */
public class EmissionFunction implements BiFunction<Double, Double, Double> {

	private final double timeStep;
	private final EmissionIntensityFunction emissionIntensityFunction;
	private final double externalEmissionsInitial;
	private final double externalEmissionsDecay;	// per 5Y

	private static double annualizedExternalEmissionsDecay = 1-Math.pow(1-0.115, 1/5); //0.115 for 5 years, thus 1-5th_root(1-0.115)

	public EmissionFunction(double timeStep, EmissionIntensityFunction emissionIntensityFunction, double externalEmissionsInitial, double externalEmissionsDecay) {
		super();
		this.timeStep = timeStep;
		this.emissionIntensityFunction = emissionIntensityFunction;
		this.externalEmissionsInitial = externalEmissionsInitial;
		this.externalEmissionsDecay = externalEmissionsDecay;
	}

	public EmissionFunction(double timeStep, EmissionIntensityFunction emissionIntensityFunction) {
		// Parameters from original model - the externalEmissionsInitial is per 5Y in the original model.
		this(timeStep, emissionIntensityFunction, 2.6/5, annualizedExternalEmissionsDecay);
	}

	@Override
	public Double apply(Double time, Double economicOutput) {
		final double emissionPerEconomicOutput = emissionIntensityFunction.apply(time);
		final double externalEmissions = externalEmissionsInitial * Math.pow(1-externalEmissionsDecay, time);

		return (emissionPerEconomicOutput * economicOutput + externalEmissions);
	}
}
