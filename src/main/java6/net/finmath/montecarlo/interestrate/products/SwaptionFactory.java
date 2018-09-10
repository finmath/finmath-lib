/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 07.06.2014
 */
package net.finmath.montecarlo.interestrate.products;

import net.finmath.time.TimeDiscretizationInterface;

/**
 * A factory (helper class) to create swaptions extending {@link AbstractLIBORMonteCarloProduct}
 * according to some (simplified) specifications.
 *
 * The class is useful if you like to create, e.g., calibration products depending
 * on some parameters.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class SwaptionFactory {

	private SwaptionFactory() {
	}

	public static AbstractLIBORMonteCarloProduct createSwaption(String className, double swaprate, TimeDiscretizationInterface swapTenor, String valueUnitAsString) {

		if(className.equals("SwaptionAnalyticApproximation")) {
			SwaptionAnalyticApproximation.ValueUnit valueUnit = SwaptionAnalyticApproximation.ValueUnit.valueOf(valueUnitAsString);
			return new SwaptionAnalyticApproximation(swaprate, swapTenor.getAsDoubleArray(), valueUnit);
		}
		else if(className.equals("SwaptionSimple")) {
			SwaptionSimple.ValueUnit valueUnit = SwaptionSimple.ValueUnit.valueOf(valueUnitAsString);
			return new SwaptionSimple(swaprate, swapTenor.getAsDoubleArray(), valueUnit);
		}
		else if(className.equals("SwaptionAnalyticApproximationRebonato")) {
			SwaptionAnalyticApproximationRebonato.ValueUnit valueUnit = SwaptionAnalyticApproximationRebonato.ValueUnit.valueOf(valueUnitAsString);
			return new SwaptionAnalyticApproximationRebonato(swaprate, swapTenor.getAsDoubleArray(), valueUnit);
		} else {
			throw new RuntimeException("Unknown class: " + className);
		}
	}
}
