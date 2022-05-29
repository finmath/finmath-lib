package net.finmath.climate.models.dice.submodels;

import java.util.function.BiFunction;

/**
 * The function that maps CarbonConcentration (in GtC) and external forcing (in W/m^2) to forcing (in W/m^2).
 * 
 * @author Christian Fries
 */
public class ForcingFunction implements BiFunction<CarbonConcentration, Double, Double> {

	private double carbonConcentrationBase = 580;
	private double forcingPerCarbonDoubling = 3.6813;

	@Override
	public Double apply(CarbonConcentration carbonConcentration, Double forcingExternal) {
		return forcingPerCarbonDoubling * Math.log(carbonConcentration.getCarbonConcentrationInAtmosphere() / carbonConcentrationBase ) / Math.log(2) + forcingExternal;
	}
}
