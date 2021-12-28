/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 07.06.2014
 */
package net.finmath.montecarlo.interestrate.products;

import net.finmath.time.TimeDiscretization;

/**
 * A factory (helper class) to create swaptions extending {@link AbstractTermStructureMonteCarloProduct}
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

	public static TermStructureMonteCarloProduct createSwaption(final String className, final double swaprate, final TimeDiscretization swapTenor, final String valueUnitAsString) {

		if(className.equals("SwaptionAnalyticApproximation")) {
			final SwaptionAnalyticApproximation.ValueUnit valueUnit = SwaptionAnalyticApproximation.ValueUnit.valueOf(valueUnitAsString);
			return new SwaptionAnalyticApproximation(swaprate, swapTenor.getAsDoubleArray(), valueUnit);
		}
		else if(className.equals("SwaptionSimple")) {
			final SwaptionSimple.ValueUnit valueUnit = SwaptionSimple.ValueUnit.valueOf(valueUnitAsString);
			return new SwaptionSimple(swaprate, swapTenor.getAsDoubleArray(), valueUnit);
		}
		else if(className.equals("SwaptionAnalyticApproximationRebonato")) {
			final SwaptionAnalyticApproximationRebonato.ValueUnit valueUnit = SwaptionAnalyticApproximationRebonato.ValueUnit.valueOf(valueUnitAsString);
			return new SwaptionAnalyticApproximationRebonato(swaprate, swapTenor.getAsDoubleArray(), valueUnit);
		} else {
			throw new RuntimeException("Unknown class: " + className);
		}
	}
}
