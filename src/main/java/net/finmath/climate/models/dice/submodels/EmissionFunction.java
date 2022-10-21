package net.finmath.climate.models.dice.submodels;

import java.util.function.BiFunction;

import net.finmath.time.TimeDiscretization;

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
public class EmissionFunction implements BiFunction<Integer, Double, Double> {

	private final TimeDiscretization timeDiscretization;
	private final EmissionIntensityFunction emissionIntensityFunction;
	private final double externalEmissionsInitial;
	private final double externalEmissionsDecay;	// per 5Y

	private static double annualizedExternalEmissionsDecay = 1-Math.pow(1-0.115, 1.0/5.0); //0.115 for 5 years, thus 1-5th_root(1-0.115)

	public EmissionFunction(TimeDiscretization timeDiscretization, EmissionIntensityFunction emissionIntensityFunction, double externalEmissionsInitial, double externalEmissionsDecay) {
		super();
		this.timeDiscretization = timeDiscretization;
		this.emissionIntensityFunction = emissionIntensityFunction;
		this.externalEmissionsInitial = externalEmissionsInitial;
		this.externalEmissionsDecay = externalEmissionsDecay;
	}

	public EmissionFunction(TimeDiscretization timeDiscretization, EmissionIntensityFunction emissionIntensityFunction) {
		this(timeDiscretization, emissionIntensityFunction, 2.6, annualizedExternalEmissionsDecay);
	}

	@Override
	public Double apply(Integer timeIndex, Double economicOutput) {
		double time = timeDiscretization.getTime(timeIndex);
		final double emissionPerEconomicOutput = emissionIntensityFunction.apply(time);
		final double externalEmissions = externalEmissionsInitial * Math.pow(1-externalEmissionsDecay, time);

		return (emissionPerEconomicOutput * economicOutput + externalEmissions);
	}

	public TimeDiscretization getTimeDiscretization() {
		return timeDiscretization;
	}
}
