/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 25.12.2016
 */

package net.finmath.montecarlo.interestrate;

import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.marketdata.model.curves.DiscountCurveInterface;
import net.finmath.marketdata.model.curves.ForwardCurveInterface;
import net.finmath.montecarlo.model.AbstractModelInterface;

/**
 * @author Christian Fries
 *
 */
public interface SingleCurrencyInterestRateModelInterface extends AbstractModelInterface {

	/**
	 * Return the associated analytic model, a collection of market date object like discount curve, forward curve
	 * and volatility surfaces.
	 * 
	 * @return The associated analytic model.
	 */
	AnalyticModelInterface getAnalyticModel();

	/**
	 * Return the discount curve associated the forwards.
	 * 
	 * @return the discount curve associated the forwards.
	 */
	DiscountCurveInterface getDiscountCurve();

	/**
	 * Return the initial forward rate curve.
	 * 
	 * @return the forward rate curve
	 */
	ForwardCurveInterface getForwardRateCurve();

	/**
	 * Create a new object implementing SingleCurrencyInterestRateModelInterface, using the new data.
	 * 
	 * @param dataModified A map with values to be used in constructions (keys are identical to parameter names of the constructors).
	 * @return A new object implementing SingleCurrencyInterestRateModelInterface, using the new data.
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	SingleCurrencyInterestRateModelInterface getCloneWithModifiedData(Map<String, Object> dataModified) throws CalculationException;

}