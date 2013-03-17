/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 30.11.2012
 */
package net.finmath.marketdata.model.curves;

import net.finmath.marketdata.model.AnalyticModelInterface;

/**
 * The interface which is implemented by forward curves.
 * 
 * @author Christian Fries
 */
public interface ForwardCurveInterface extends CurveInterface {

	/**
	 * Returns the forward for the corresponding fixing time.
	 * @param model An analytic model providing a context. Some curves do not need this (can be null).
	 * @param fixingTime The fixing time of the index associated with this forward curve.
	 * 
	 * @return The forward.
	 */
	public abstract double getForward(AnalyticModelInterface model, double fixingTime);

	/**
	 * Returns the name of the discount curve associated with this forward curve.
	 * 
	 * @return The name of the discount curve associated with this forward curve.
	 */
	String getDiscountCurveName();

	/**
	 * Returns the payment offset associated with this forward curve.
	 * 
	 * @return The payment offset associated with this forward curve.
	 */
	public double getPaymentOffset();
}