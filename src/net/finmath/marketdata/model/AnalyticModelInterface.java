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

	public abstract CurveInterface getCurve(String name);

	public abstract void setCurve(CurveInterface curve);

	public abstract DiscountCurveInterface getDiscountCurve(String discountCurveName);

	public abstract ForwardCurveInterface getForwardCurve(String forwardCurveName);

	public abstract AnalyticModelInterface getCloneForParameter(Map<CurveInterface, double[]> curvesParameterPairs) throws CloneNotSupportedException;
}