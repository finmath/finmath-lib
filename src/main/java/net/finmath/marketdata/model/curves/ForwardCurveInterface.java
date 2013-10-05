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
    double getForward(AnalyticModelInterface model, double fixingTime);

	/**
	 * Returns the forward for the corresponding fixing time.
	 * @param model An analytic model providing a context. Some curves do not need this (can be null).
	 * @param fixingTime The fixing time of the index associated with this forward curve.
	 * 
	 * @return The forward.
	 */
    double getForward(AnalyticModelInterface model, double fixingTime, double paymentOffset);

    /**
	 * Returns the name of the discount curve associated with this forward curve.
	 * 
	 * @return The name of the discount curve associated with this forward curve.
	 */
	String getDiscountCurveName();
	
	/**
	 * Returns the function mapping a fixing to its payment offset, associated with this curve.
	 * 
	 * @return The function mapping a fixing to its payment offset, associated with this curve.
	 */
	CurveInterface getPaymentOffsets();
	
	/**
	 * Returns the payment offset associated with this forward curve and a corresponding fixingTime.
	 * 
	 * @param fixingTime The fixing time of the index associated with this forward curve.
	 * @return The payment offset associated with this forward curve.
	 */
    double getPaymentOffset(double fixingTime);
}