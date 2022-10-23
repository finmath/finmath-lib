package net.finmath.climate.models.dice.submodels;

import java.util.function.BiFunction;
import java.util.function.Function;

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
public class EmissionExternalFunction implements Function<Double, Double> {

	private final TimeDiscretization timeDiscretization;
	private final double externalEmissionsInitial;
	private final double externalEmissionsDecay;	// per 5Y

	private static double annualizedExternalEmissionsDecay = -Math.log(1-0.115)/5.0; //0.115 for 5 years, thus 1-5th_root(1-0.115)

	public EmissionExternalFunction(TimeDiscretization timeDiscretization, double externalEmissionsInitial, double externalEmissionsDecay) {
		super();
		this.timeDiscretization = timeDiscretization;
		this.externalEmissionsInitial = externalEmissionsInitial;
		this.externalEmissionsDecay = externalEmissionsDecay;
	}

	public EmissionExternalFunction(TimeDiscretization timeDiscretization) {
		this(timeDiscretization, 2.6, annualizedExternalEmissionsDecay);
	}

	@Override
	public Double apply(Double time) {
		final double externalEmissions = externalEmissionsInitial * Math.exp(-externalEmissionsDecay * time);

		return externalEmissions;
	}

	public TimeDiscretization getTimeDiscretization() {
		return timeDiscretization;
	}
}
