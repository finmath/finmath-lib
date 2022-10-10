package net.finmath.climate.models.dice.submodels;

import java.util.function.BiFunction;

/**
 * The function that maps CarbonConcentration (in GtC) and external forcing (in W/m^2) to forcing (in W/m^2).
 *
 * @author Christian Fries
 */
public class ForcingFunction implements BiFunction<CarbonConcentration3DScalar, Double, Double> {

	private final double carbonConcentrationBase = 580;
	private final double forcingPerCarbonDoubling = 3.6813/5;			// Parameter in original model was per 5 year

	@Override
	public Double apply(CarbonConcentration3DScalar carbonConcentration, Double forcingExternal) {
		return forcingPerCarbonDoubling * Math.log(carbonConcentration.getExpectedCarbonConcentrationInAtmosphere() / carbonConcentrationBase ) / Math.log(2) + forcingExternal;
	}
}
