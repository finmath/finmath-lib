package net.finmath.climate.models.dice.submodels;

import java.util.function.BiFunction;

/**
 * The function that maps (relative) abatement coefficient to (relative) cost.
 *
 * Function of (time, abatement)
 * Note: Sigma factor is missing here (moved to the outside)
 *
 * @author Christian Fries
 */
public class AbatementCostFunction implements BiFunction<Double, Double, Double> {

	private final double backstopPriceInitial;		// Initial cost for backstop
	private final double backstopRate;				// Decay of backstop cost (parameter from original
	private final double abatementExponent;			// Exponent abatement in cost function (GAMS expcost2)

	/**
	 *
	 * @param backstopPriceInitial USD per ton CO2 to abate 100%
	 * @param backstopRate Annual rate by which the price declines.
	 * @param abatementExponent Exponent for mu.
	 */
	public AbatementCostFunction(double backstopPriceInitial, double backstopRate, double abatementExponent) {
		super();
		this.backstopPriceInitial = backstopPriceInitial;	// USD per ton CO2 to abate 100%
		this.backstopRate = backstopRate;
		this.abatementExponent = abatementExponent;
	}

	public AbatementCostFunction() {
		// Parameters from original paper. Note that the rate is a "per 5 year" rate.
		this(550.0/1000.0, -Math.log(1-0.025)/5.0, 2.6);
	}

	@Override
	public Double apply(Double time, Double abatement) {
		final double backstopPrice = backstopPriceInitial * Math.exp(-backstopRate * time);
		final double abatementCost = backstopPrice * Math.pow(abatement , abatementExponent)/abatementExponent;

		return abatementCost;
	}
}
