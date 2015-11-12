/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 30.11.2012
 */
package net.finmath.marketdata.model.curves;

import java.time.LocalDate;

import net.finmath.marketdata.calibration.ParameterObjectInterface;
import net.finmath.marketdata.model.AnalyticModelInterface;

/**
 * The interface which is implemented by a general curve.
 * 
 * @author Christian Fries
 */
public interface CurveInterface extends ParameterObjectInterface, Cloneable {

	/**
	 * Get the name of the curve.
	 * 
	 * @return The name of this curve
	 */
	String getName();

	/**
	 * Return the reference date of this curve, i.e. the date
	 * associated with t=0.
	 * 
	 * @return The date identified as t=0.
	 */
	LocalDate getReferenceDate();

	/**
	 * Returns the value for the time using the interpolation method associated with this curve.
	 * 
	 * @param time Time for which the value should be returned.
	 * @return The value at the give time.
	 */
	double getValue(double time);

	/**
	 * Returns the value for the time using the interpolation method associated with this curve
	 * within a given context, i.e., a model. The model (context) is needed only if the curve
	 * relies on another curve. Examples are a forward curve which relies on a discount curve or
	 * a discount curve which is defined via a spread over another curve.
	 *  
	 * @param model An analytic model providing a context.
	 * @param time Time for which the value should be returned.
	 * 
	 * @return The value at the give time.
	 */
	double getValue(AnalyticModelInterface model, double time);

	/**
	 * Create a deep copied clone.
	 * 
	 * @return A clone (deep copied).
	 * @throws CloneNotSupportedException Thrown, when the curve could not be cloned.
	 */
	Object clone() throws CloneNotSupportedException;

	/**
	 * Returns a curve builder bases on a clone of this curve. Using that curve
	 * builder you may create a new curve from this curve by adding points or
	 * changing properties.
	 * Note: The clone has the same name than this one.
	 * 
	 * @return An object implementing the CurveBuilderInterface where the underlying curve is a clone of this curve.
	 * @throws CloneNotSupportedException Thrown, when this curve could not be cloned.
	 */
	CurveBuilderInterface getCloneBuilder() throws CloneNotSupportedException;

	@Override
    CurveInterface getCloneForParameter(double[] value) throws CloneNotSupportedException;
}
