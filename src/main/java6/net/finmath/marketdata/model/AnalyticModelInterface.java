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

	CurveInterface getCurve(String name);

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