/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 28.11.2012
 */
package net.finmath.marketdata.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.finmath.marketdata.model.curves.CurveInterface;
import net.finmath.marketdata.model.curves.DiscountCurveFromForwardCurve;
import net.finmath.marketdata.model.curves.DiscountCurveInterface;
import net.finmath.marketdata.model.curves.ForwardCurveInterface;

/**
 * Implements a collection of market data objects (e.g., discount curves, forward curve)
 * which provide interpolation of market data or other derived quantities
 * ("calibrated curves"). This can be seen as a model to be used in analytic pricing
 * formulas - hence this class is termed <code>AnalyticModel</code>.
 * 
 * @author Christian Fries
 */
public class AnalyticModel implements AnalyticModelInterface {

	private final Map<String, CurveInterface> curvesMap = new HashMap<String, CurveInterface>();

	/**
	 * Create an empty analytic model.
	 */
	public AnalyticModel() {
	}

	/**
	 * Create an analytic model with the given curves.
	 * 
	 * @param curves The vector of curves.
	 */
	public AnalyticModel(CurveInterface[] curves) {
        for (CurveInterface curve : curves) curvesMap.put(curve.getName(), curve);
	}
	
	/**
	 * Create an analytic model with the given curves.
	 * 
	 * @param curves A collection of curves.
	 */
	public AnalyticModel(Collection<CurveInterface> curves) {
		for(CurveInterface curve : curves) curvesMap.put(curve.getName(), curve);
	}

	/* (non-Javadoc)
	 * @see net.finmath.marketdata.model.AnalyticModelInterface#getCurve(java.lang.String)
	 */
	@Override
	public CurveInterface getCurve(String name)
	{
		return curvesMap.get(name);
	}

	@Override
    public void setCurve(CurveInterface curve)
	{
		curvesMap.put(curve.getName(), curve);
	}

	public void setCurves(CurveInterface[] curves) {
		for(CurveInterface curve : curves) setCurve(curve);
	}
	
	/* (non-Javadoc)
	 * @see net.finmath.marketdata.model.AnalyticModelInterface#getDiscountCurve(java.lang.String)
	 */
	@Override
	public DiscountCurveInterface getDiscountCurve(String discountCurveName) {
		DiscountCurveInterface discountCurve = null;
		CurveInterface curveForDiscountingCurve			= getCurve(discountCurveName);
		if(DiscountCurveInterface.class.isInstance(curveForDiscountingCurve)) {
			discountCurve	= (DiscountCurveInterface)curveForDiscountingCurve;
		}
		else if(ForwardCurveInterface.class.isInstance(curveForDiscountingCurve)) {
			// Check if the discount curve is a forward curve
			ForwardCurveInterface	forwardCurveForDiscounting	= (ForwardCurveInterface) curveForDiscountingCurve;
			discountCurve = new DiscountCurveFromForwardCurve(forwardCurveForDiscounting.getName());
		}
		return discountCurve;
	}

	/* (non-Javadoc)
	 * @see net.finmath.marketdata.model.AnalyticModelInterface#getForwardCurve(java.lang.String)
	 */
	@Override
	public ForwardCurveInterface getForwardCurve(String forwardCurveName) {
		ForwardCurveInterface forwardCurve = null;
		CurveInterface curveForForwards			= getCurve(forwardCurveName);
		if(ForwardCurveInterface.class.isInstance(curveForForwards)) {
			forwardCurve	= (ForwardCurveInterface)curveForForwards;
		}
		return forwardCurve;
	}

	@Override
    public AnalyticModelInterface getCloneForParameter(Map<CurveInterface, double[]> curveParameterPairs) throws CloneNotSupportedException {

		// Build the modified clone of this model
		AnalyticModel modelClone = new AnalyticModel();

		// Add all other curves
		modelClone.curvesMap.putAll(this.curvesMap);

		// Add modified clones of curves to model clone
		if(curveParameterPairs != null) {
			for(Entry<CurveInterface,double[]> curveParameterPair : curveParameterPairs.entrySet()) {
				CurveInterface newCurve = curveParameterPair.getKey().getCloneForParameter(curveParameterPair.getValue());
				modelClone.setCurve(newCurve);
			}
		}
		
		return modelClone;
	}
}
