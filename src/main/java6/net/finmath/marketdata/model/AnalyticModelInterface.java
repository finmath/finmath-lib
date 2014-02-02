/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 28.11.2012
 */
package net.finmath.marketdata.model;

import java.util.Map;

import net.finmath.marketdata.model.curves.CurveInterface;
import net.finmath.marketdata.model.curves.DiscountCurveInterface;
import net.finmath.marketdata.model.curves.ForwardCurveInterface;

/**
 * @author Christian Fries
 */
public interface AnalyticModelInterface {

	CurveInterface getCurve(String name);

	void setCurve(CurveInterface curve);

	DiscountCurveInterface getDiscountCurve(String discountCurveName);

	ForwardCurveInterface getForwardCurve(String forwardCurveName);

	AnalyticModelInterface getCloneForParameter(Map<CurveInterface, double[]> curvesParameterPairs) throws CloneNotSupportedException;
}