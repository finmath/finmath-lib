/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 30.11.2012
 */
package net.finmath.marketdata2.model.curves;

import net.finmath.marketdata2.model.AnalyticModel;
import net.finmath.stochastic.RandomVariable;

/**
 * The interface which is implemented by forward curves.
 *
 * @author Christian Fries
 * @version 1.0
 */
public interface ForwardCurveInterface extends Curve {

	/**
	 * Returns the forward for the corresponding fixing time.
	 * @param model An analytic model providing a context. Some curves do not need this (can be null).
	 * @param fixingTime The fixing time of the index associated with this forward curve.
	 *
	 * @return The forward.
	 */
	RandomVariable getForward(AnalyticModel model, double fixingTime);

	/**
	 * Returns the forward for the corresponding fixing time and paymentOffset.
	 * @param model An analytic model providing a context. Some curves do not need this (can be null).
	 * @param fixingTime The fixing time of the index associated with this forward curve.
	 * @param paymentOffset The payment offset (as internal day count fraction) specifying the payment of this index. Used only as a fallback and/or consistency check.
	 *
	 * @return The forward.
	 */
	RandomVariable getForward(AnalyticModel model, double fixingTime, double paymentOffset);

	/**
	 * @return The name of the discount curve associated with this forward curve (e.g. OIS for collateralized forwards)
	 */
	String getDiscountCurveName();

	/**
	 * Returns the payment offset associated with this forward curve and a corresponding fixingTime.
	 *
	 * @param fixingTime The fixing time of the index associated with this forward curve.
	 * @return The payment offset associated with this forward curve.
	 */
	double getPaymentOffset(double fixingTime);
}
