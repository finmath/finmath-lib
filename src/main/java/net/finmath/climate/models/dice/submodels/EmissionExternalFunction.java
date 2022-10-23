package net.finmath.climate.models.dice.submodels;

import java.util.function.Function;

/**
 * The function that models external emissions as GtCO2 / year
 * \(
 * 	(t) \mapsto E_{\mathrm{ex}}(t) .
 * \)
 *
 * @author Christian Fries
 */
public class EmissionExternalFunction implements Function<Double, Double> {

	private final double externalEmissionsInitial;
	private final double externalEmissionsDecay;	// per 5Y

	private static double annualizedExternalEmissionsDecay = -Math.log(1-0.115)/5.0; //0.115 for 5 years, thus 1-5th_root(1-0.115)

	/**
	 * 
	 * @param externalEmissionsInitial Initial value for the emissions per year. Unit: GtCO2 / year.
	 * @param externalEmissionsDecay Exponential decay rate. Unit: 1/year.
	 */
	public EmissionExternalFunction(double externalEmissionsInitial, double externalEmissionsDecay) {
		super();
		this.externalEmissionsInitial = externalEmissionsInitial;
		this.externalEmissionsDecay = externalEmissionsDecay;
	}

	public EmissionExternalFunction() {
		this(2.6, annualizedExternalEmissionsDecay);
	}

	@Override
	public Double apply(Double time) {
		final double externalEmissions = externalEmissionsInitial * Math.exp(-externalEmissionsDecay * time);

		return externalEmissions;
	}
}
