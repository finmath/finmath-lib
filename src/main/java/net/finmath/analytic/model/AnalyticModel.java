/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 28.11.2012
 */
package net.finmath.analytic.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.finmath.analytic.calibration.ParameterObjectInterface;
import net.finmath.analytic.model.curves.CurveInterface;
import net.finmath.analytic.model.curves.DiscountCurveInterface;
import net.finmath.analytic.model.curves.ForwardCurveInterface;
import net.finmath.analytic.model.volatilities.AbstractVolatilitySurface;
import net.finmath.analytic.model.volatilities.VolatilitySurfaceInterface;
import net.finmath.montecarlo.AbstractRandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * Implements a collection of market data objects (e.g., discount curves, forward curve)
 * which provide interpolation of market data or other derived quantities
 * ("calibrated curves"). This can be seen as a model to be used in analytic pricing
 * formulas - hence this class is termed <code>AnalyticModel</code>.
 * 
 * @author Christian Fries
 */
public class AnalyticModel implements AnalyticModelInterface, Cloneable {

	private final AbstractRandomVariableFactory			randomVariableFactory;
	private final Map<String, CurveInterface>			curvesMap					= new HashMap<String, CurveInterface>();
	private final Map<String, VolatilitySurfaceInterface>	volatilitySurfaceMap	= new HashMap<String, VolatilitySurfaceInterface>();

	/**
	 * Create an empty analytic model.
	 */
	public AnalyticModel() {
		randomVariableFactory = new RandomVariableFactory();
	}

	/**
	 * Create an empty analytic model using a given AbstractRandomVariableFactory for construction of result types.
	 * 
	 * @param randomVariableFactory given AbstractRandomVariableFactory for construction of result types.
	 */
	public AnalyticModel(AbstractRandomVariableFactory randomVariableFactory) {
		this.randomVariableFactory = randomVariableFactory;
	}
  
	/**
	 * Create an analytic model with the given curves.
	 * 
	 * @param curves The vector of curves.
	 */
	public AnalyticModel(CurveInterface[] curves) {
		this();
        for (CurveInterface curve : curves) curvesMap.put(curve.getName(), curve);
	}
	
	/**
	 * Create an analytic model with the given curves  using a given AbstractRandomVariableFactory for construction of result types.
	 * 
	 * @param randomVariableFactory given AbstractRandomVariableFactory for construction of result types.
	 * @param curves The vector of curves.
	 */
	public AnalyticModel(AbstractRandomVariableFactory randomVariableFactory, CurveInterface[] curves) {
		this(randomVariableFactory);
        for (CurveInterface curve : curves) curvesMap.put(curve.getName(), curve);
	}

	/**
	 * Create an analytic model with the given curves.
	 * 
	 * @param curves A collection of curves.
	 */
	public AnalyticModel(Collection<CurveInterface> curves) {
		this();
		for(CurveInterface curve : curves) curvesMap.put(curve.getName(), curve);
	}

	public RandomVariableInterface getRandomVariableForConstant(double value) {
		return randomVariableFactory.createRandomVariable(value);
	}
	
	@Override
	public CurveInterface getCurve(String name)
	{
		return curvesMap.get(name);
	}

	public AnalyticModelInterface addCurve(String name, CurveInterface curve) {
		AnalyticModel newModel = clone();
		newModel.curvesMap.put(name, curve);
		return newModel;
	}

	public AnalyticModelInterface addCurve(CurveInterface curve) {
		AnalyticModel newModel = clone();
		newModel.curvesMap.put(curve.getName(), curve);
		return newModel;
	}

	@Override
	public AnalyticModelInterface addCurves(CurveInterface... curves) {
		AnalyticModel newModel = clone();
		for(CurveInterface curve : curves) newModel.curvesMap.put(curve.getName(), curve);
		return newModel;
	}

	@Override
	public AnalyticModelInterface addCurves(Set<CurveInterface> curves) {
		AnalyticModel newModel = clone();
		for(CurveInterface curve : curves) newModel.curvesMap.put(curve.getName(), curve);
		return newModel;
	}

	/**
	 * @deprecated This class will become immutable. Use addCurve instead.
	 */
	@Override
	@Deprecated
    public void setCurve(CurveInterface curve)
	{
		curvesMap.put(curve.getName(), curve);
	}

	/**
	 * Set some curves.
	 * 
	 * @param curves Array of curves to set.
	 * @deprecated This class will become immutable. Use addCurve instead.
	 */
	@Deprecated
	public void setCurves(CurveInterface[] curves) {
		for(CurveInterface curve : curves) setCurve(curve);
	}
	
	@Override
	public DiscountCurveInterface getDiscountCurve(String discountCurveName) {
		DiscountCurveInterface discountCurve = null;
		CurveInterface curve = getCurve(discountCurveName);
		if(DiscountCurveInterface.class.isInstance(curve))
			discountCurve = (DiscountCurveInterface)curve;

		return discountCurve;
	}

	@Override
	public ForwardCurveInterface getForwardCurve(String forwardCurveName) {
		ForwardCurveInterface forwardCurve = null;
		CurveInterface curve = getCurve(forwardCurveName);
		if(ForwardCurveInterface.class.isInstance(curve))
			forwardCurve = (ForwardCurveInterface)curve;

		return forwardCurve;
	}

	@Override
	public VolatilitySurfaceInterface getVolatilitySurface(String name) {
		return volatilitySurfaceMap.get(name);
	}
	
	public AnalyticModelInterface addVolatilitySurface(VolatilitySurfaceInterface volatilitySurface)
	{
		AnalyticModel newModel = clone();
		newModel.volatilitySurfaceMap.put(volatilitySurface.getName(), volatilitySurface);
		return newModel;
	}

	@Override
	public AnalyticModelInterface addVolatilitySurfaces(VolatilitySurfaceInterface... volatilitySurfaces)
	{
		AnalyticModel newModel = clone();
		for(VolatilitySurfaceInterface volatilitySurface : volatilitySurfaces) newModel.volatilitySurfaceMap.put(volatilitySurface.getName(), volatilitySurface);
		return newModel;
	}

	@Override
	public AnalyticModelInterface addVolatilitySurfaces(Set<AbstractVolatilitySurface> volatilitySurfaces) {
		AnalyticModel newModel = clone();
		for(VolatilitySurfaceInterface volatilitySurface : volatilitySurfaces) newModel.volatilitySurfaceMap.put(volatilitySurface.getName(), volatilitySurface);
		return newModel;
	}

	private void setVolatilitySurface(VolatilitySurfaceInterface volatilitySurface)
	{
		volatilitySurfaceMap.put(volatilitySurface.getName(), volatilitySurface);
	}
	
	private void set(Object marketDataObject) {
		if(marketDataObject instanceof CurveInterface)					setCurve((CurveInterface)marketDataObject);
		else if(marketDataObject instanceof VolatilitySurfaceInterface)	setVolatilitySurface((VolatilitySurfaceInterface)marketDataObject);
		else throw new IllegalArgumentException("Provided object is not of supported type.");
	}

	@Override
	public AnalyticModel clone()
	{
		AnalyticModel newModel = new AnalyticModel();
		newModel.curvesMap.putAll(curvesMap);
		newModel.volatilitySurfaceMap.putAll(volatilitySurfaceMap);
		return newModel;
	}

	@Override
    public AnalyticModelInterface getCloneForParameter(Map<ParameterObjectInterface, RandomVariableInterface[]> curveParameterPairs) throws CloneNotSupportedException {

		// Build the modified clone of this model
		AnalyticModel modelClone = clone();

		// Add modified clones of curves to model clone
		if(curveParameterPairs != null) {
			for(Entry<ParameterObjectInterface, RandomVariableInterface[]> curveParameterPair : curveParameterPairs.entrySet()) {
				ParameterObjectInterface newCurve = curveParameterPair.getKey().getCloneForParameter(curveParameterPair.getValue());
				modelClone.set(newCurve);
			}
		}
		
		return modelClone;
	}

	@Override
	public String toString() {
		return "AnalyticModel: curves=" + curvesMap.keySet();
	}
}
