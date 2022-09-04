package net.finmath.climate.models.dice.submodels;

import java.util.function.BiFunction;

/**
 * The function that maps (relative) abatement coefficient to (relative) cost.
 *
 * Function of (time, abatement)
 * Note: Sigma factor is missing here (moved to the outside)
 *
 * Note: The function depends on the time step size
 *
 * @author Christian Fries
 */
public class AbatementCostFunction implements BiFunction<Double, Double, Double> {

	private static double timeStep = 5.0;	// time step in the original model (should become a parameter)

	private final double backstopPriceInitial;		// Initial cost for backstop
	private final double backstopRate;				// Decay of backstop cost (parameter from original
	private final double abatementExponent;			// Exponent abatement in cost function (GAMS expcost2)

	public AbatementCostFunction(double backstopPriceInitial, double backstopRate, double abatementExponent) {
		super();
		this.backstopPriceInitial = backstopPriceInitial;
		this.backstopRate = backstopRate;
		this.abatementExponent = abatementExponent;
	}

	public AbatementCostFunction() {
		// Parameters from original paper. Note that the rate is a "per 5 year" rate.
		this(550.0/1000.0, 1-Math.pow(1-0.025,timeStep/5.0), 2.6);
	}

	@Override
	public Double apply(Double time, Double abatement) {
		final double abatementCost = backstopPriceInitial * Math.pow(1-backstopRate, time) * Math.pow(abatement , abatementExponent)/abatementExponent;
		// alternatively, express the backstopRate as exponential decay
		//		double abatementCost = backstopPriceInitial * Math.exp(-backstopRate * time) * Math.pow(abatement , theta2)/theta2;

		return abatementCost;
	}
}
