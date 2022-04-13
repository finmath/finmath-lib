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

	private static double timeStep = 5.0;	// time step in the original model (should become a parameter)

	private final EmissionIntensityFunction emissionIntensityFunction;
	private final double externalEmissionsInitial;
	private final double externalEmissionsDecay;	// per 5Y

	public EmissionFunction(EmissionIntensityFunction emissionIntensityFunction, double externalEmissionsInitial, double externalEmissionsDecay) {
		super();
		this.emissionIntensityFunction = emissionIntensityFunction;
		this.externalEmissionsInitial = externalEmissionsInitial;
		this.externalEmissionsDecay = externalEmissionsDecay;
	}

	public EmissionFunction(EmissionIntensityFunction emissionIntensityFunction) {
		// Parameters from original model
		this(emissionIntensityFunction, 2.6, 0.115);
	}

	@Override
	public Double apply(Double time, Double economicOutput) {
		double emissionPerEconomicOutput = emissionIntensityFunction.apply(time);
		// The parameter externalEmissionsDecay is formulated for a 5 year period
		double externalEmissions = externalEmissionsInitial * Math.pow(1-externalEmissionsDecay, time*timeStep/5.0);

		return timeStep * (emissionPerEconomicOutput * economicOutput + externalEmissions);
	}
}
