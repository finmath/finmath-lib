/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 30.11.2012
 */
package net.finmath.marketdata2.model.curves;

import net.finmath.marketdata2.model.AnalyticModel;
import net.finmath.stochastic.RandomVariable;

/**
 * The interface which is implemented by discount curves. A discount curve is a mapping of T to df(T) where df(T)
 * represents the present value of a cash flow or 1 in time T, with respect to a specific currency unit and collateralization.
 *
 * @author Christian Fries
 * @version 1.0
 */
public interface DiscountCurveInterface extends Curve {

	/**
	 * Returns the discount factor for the corresponding maturity. This getter is not optimized for performance.
	 *
	 * @param maturity The maturity for which the discount factor is requested.
	 * @return The discount factor (i.e., price of the zero coupon bond with given maturity and notional 1.
	 */
	RandomVariable getDiscountFactor(double maturity);

	/**
	 * Returns the discount factor for the corresponding maturity. This getter is not optimized for performance.
	 *
	 * @param model An analytic model providing a context. Some curves do not need this (can be null).
	 * @param maturity The maturity for which the discount factor is requested.
	 *
	 * @return The discount factor (i.e., price of the zero coupon bond with given maturity and notional 1.
	 */
	RandomVariable getDiscountFactor(AnalyticModel model, double maturity);

}
