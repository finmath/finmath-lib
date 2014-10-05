/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 28.11.2012
 */
package net.finmath.marketdata.model;

import java.util.Map;

import net.finmath.marketdata.calibration.ParameterObjectInterface;
import net.finmath.marketdata.model.curves.CurveInterface;
import net.finmath.marketdata.model.curves.DiscountCurveInterface;
import net.finmath.marketdata.model.curves.ForwardCurveInterface;
import net.finmath.marketdata.model.volatilities.VolatilitySurfaceInterface;
import net.finmath.modelling.ModelInterface;

/**
 * @author Christian Fries
 */
public interface AnalyticModelInterface extends ModelInterface, Cloneable {

	/**
	 * Get a curve by a given curve name.
	 * 
	 * @param name The name of the curve.
	 * @return The curve with the corresponding name, given that it is part of this model, otherwise null is return.
	 */
	CurveInterface getCurve(String name);

	/**
	 * Add a reference to a given curve under a given name to this model. It is not necessary that the name given agrees with
	 * <code>curve.getName()</code>. This method comes in handy, if you like to create curve mappings.
	 * 
	 * @param name Name under which the curve is known in the model.
	 * @param curve The curve.
	 * @return A clone of this model, containing the curves of this model which are not known under the given name and the new curve under the given name.
	 */
	AnalyticModelInterface addCurve(String name, CurveInterface curve);

	AnalyticModelInterface addCurves(CurveInterface... curves);

	@Deprecated
	void setCurve(CurveInterface curve);

	DiscountCurveInterface getDiscountCurve(String discountCurveName);

	ForwardCurveInterface getForwardCurve(String forwardCurveName);

	VolatilitySurfaceInterface getVolatilitySurface(String name);

	AnalyticModelInterface addVolatilitySurfaces(VolatilitySurfaceInterface... volatilitySurfaces);

	@Deprecated
	void setVolatilitySurface(VolatilitySurfaceInterface volatilitySurface);

	AnalyticModelInterface clone();

	AnalyticModelInterface getCloneForParameter(Map<ParameterObjectInterface, double[]> curvesParameterPairs) throws CloneNotSupportedException;
}