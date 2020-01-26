/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 28.11.2012
 */
package net.finmath.marketdata2.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.finmath.marketdata2.calibration.ParameterObject;
import net.finmath.marketdata2.model.curves.Curve;
import net.finmath.marketdata2.model.curves.DiscountCurveInterface;
import net.finmath.marketdata2.model.curves.ForwardCurveInterface;
import net.finmath.marketdata2.model.volatilities.VolatilitySurface;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFromArrayFactory;
import net.finmath.stochastic.RandomVariable;

/**
 * Implements a collection of market data objects (e.g., discount curves, forward curve)
 * which provide interpolation of market data or other derived quantities
 * ("calibrated curves"). This can be seen as a model to be used in analytic pricing
 * formulas - hence this class is termed <code>AnalyticModelFromCuvesAndVols</code>.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class AnalyticModelFromCurvesAndVols implements AnalyticModel, Serializable, Cloneable {

	/**
	 *
	 */
	private static final long serialVersionUID = -1551367852009541732L;
	private final RandomVariableFactory			abstractRandomVariableFactory;
	private final Map<String, Curve>			curvesMap					= new HashMap<>();
	private final Map<String, VolatilitySurface>	volatilitySurfaceMap	= new HashMap<>();

	/**
	 * Create an empty analytic model.
	 */
	public AnalyticModelFromCurvesAndVols() {
		abstractRandomVariableFactory = new RandomVariableFromArrayFactory();
	}

	/**
	 * Create an empty analytic model using a given AbstractRandomVariableFactory for construction of result types.
	 *
	 * @param abstractRandomVariableFactory given AbstractRandomVariableFactory for construction of result types.
	 */
	public AnalyticModelFromCurvesAndVols(RandomVariableFactory abstractRandomVariableFactory) {
		this.abstractRandomVariableFactory = abstractRandomVariableFactory;
	}

	/**
	 * Create an analytic model with the given curves.
	 *
	 * @param curves The vector of curves.
	 */
	public AnalyticModelFromCurvesAndVols(Curve[] curves) {
		this();
		for (Curve curve : curves) {
			curvesMap.put(curve.getName(), curve);
		}
	}

	/**
	 * Create an analytic model with the given curves  using a given AbstractRandomVariableFactory for construction of result types.
	 *
	 * @param abstractRandomVariableFactory given AbstractRandomVariableFactory for construction of result types.
	 * @param curves The vector of curves.
	 */
	public AnalyticModelFromCurvesAndVols(RandomVariableFactory abstractRandomVariableFactory, Curve[] curves) {
		this(abstractRandomVariableFactory);
		for (Curve curve : curves) {
			curvesMap.put(curve.getName(), curve);
		}
	}

	/**
	 * Create an analytic model with the given curves.
	 *
	 * @param curves A collection of curves.
	 */
	public AnalyticModelFromCurvesAndVols(Collection<Curve> curves) {
		this();
		for(Curve curve : curves) {
			curvesMap.put(curve.getName(), curve);
		}
	}

	@Override
	public RandomVariable getRandomVariableForConstant(double value) {
		return abstractRandomVariableFactory.createRandomVariable(value);
	}

	@Override
	public Curve getCurve(String name)
	{
		return curvesMap.get(name);
	}

	@Override
	public  Map<String, Curve> getCurves()
	{
		return Collections.unmodifiableMap(curvesMap);
	}

	@Override
	public AnalyticModel addCurve(String name, Curve curve) {
		AnalyticModelFromCurvesAndVols newModel = clone();
		newModel.curvesMap.put(name, curve);
		return newModel;
	}

	public AnalyticModel addCurve(Curve curve) {
		AnalyticModelFromCurvesAndVols newModel = clone();
		newModel.curvesMap.put(curve.getName(), curve);
		return newModel;
	}

	@Override
	public AnalyticModel addCurves(Curve... curves) {
		AnalyticModelFromCurvesAndVols newModel = clone();
		for(Curve curve : curves) {
			newModel.curvesMap.put(curve.getName(), curve);
		}
		return newModel;
	}

	@Override
	public AnalyticModel addCurves(Set<Curve> curves) {
		AnalyticModelFromCurvesAndVols newModel = clone();
		for(Curve curve : curves) {
			newModel.curvesMap.put(curve.getName(), curve);
		}
		return newModel;
	}

	/**
	 * @deprecated This class will become immutable. Use addCurve instead.
	 */
	@Override
	@Deprecated
	public void setCurve(Curve curve)
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
	public void setCurves(Curve[] curves) {
		for(Curve curve : curves) {
			setCurve(curve);
		}
	}

	@Override
	public DiscountCurveInterface getDiscountCurve(String discountCurveName) {
		DiscountCurveInterface discountCurve = null;
		Curve curve = getCurve(discountCurveName);
		if(DiscountCurveInterface.class.isInstance(curve)) {
			discountCurve = (DiscountCurveInterface)curve;
		}

		return discountCurve;
	}

	@Override
	public ForwardCurveInterface getForwardCurve(String forwardCurveName) {
		ForwardCurveInterface forwardCurve = null;
		Curve curve = getCurve(forwardCurveName);
		if(ForwardCurveInterface.class.isInstance(curve)) {
			forwardCurve = (ForwardCurveInterface)curve;
		}

		return forwardCurve;
	}

	@Override
	public VolatilitySurface getVolatilitySurface(String name) {
		return volatilitySurfaceMap.get(name);
	}

	@Override
	public Map<String, VolatilitySurface> getVolatilitySurfaces() {
		return Collections.unmodifiableMap(volatilitySurfaceMap);
	}

	public AnalyticModel addVolatilitySurface(VolatilitySurface volatilitySurface)
	{
		AnalyticModelFromCurvesAndVols newModel = clone();
		newModel.volatilitySurfaceMap.put(volatilitySurface.getName(), volatilitySurface);
		return newModel;
	}

	@Override
	public AnalyticModel addVolatilitySurfaces(VolatilitySurface... volatilitySurfaces)
	{
		AnalyticModelFromCurvesAndVols newModel = clone();
		for(VolatilitySurface volatilitySurface : volatilitySurfaces) {
			newModel.volatilitySurfaceMap.put(volatilitySurface.getName(), volatilitySurface);
		}
		return newModel;
	}

	@Override
	public AnalyticModel addVolatilitySurfaces(Set<VolatilitySurface> volatilitySurfaces) {
		AnalyticModelFromCurvesAndVols newModel = clone();
		for(VolatilitySurface volatilitySurface : volatilitySurfaces) {
			newModel.volatilitySurfaceMap.put(volatilitySurface.getName(), volatilitySurface);
		}
		return newModel;
	}

	private void setVolatilitySurface(VolatilitySurface volatilitySurface)
	{
		volatilitySurfaceMap.put(volatilitySurface.getName(), volatilitySurface);
	}

	private void set(Object marketDataObject) {
		if(marketDataObject instanceof Curve) {
			setCurve((Curve)marketDataObject);
		} else if(marketDataObject instanceof VolatilitySurface) {
			setVolatilitySurface((VolatilitySurface)marketDataObject);
		} else {
			throw new IllegalArgumentException("Provided object is not of supported type.");
		}
	}

	@Override
	public AnalyticModelFromCurvesAndVols clone()
	{
		AnalyticModelFromCurvesAndVols newModel = new AnalyticModelFromCurvesAndVols();
		newModel.curvesMap.putAll(curvesMap);
		newModel.volatilitySurfaceMap.putAll(volatilitySurfaceMap);
		return newModel;
	}

	@Override
	public AnalyticModel getCloneForParameter(Map<ParameterObject, RandomVariable[]> curveParameterPairs) throws CloneNotSupportedException {

		// Build the modified clone of this model
		AnalyticModelFromCurvesAndVols modelClone = clone();

		// Add modified clones of curves to model clone
		if(curveParameterPairs != null) {
			for(Entry<ParameterObject, RandomVariable[]> curveParameterPair : curveParameterPairs.entrySet()) {
				ParameterObject newCurve = curveParameterPair.getKey().getCloneForParameter(curveParameterPair.getValue());
				modelClone.set(newCurve);
			}
		}

		return modelClone;
	}

	@Override
	public String toString() {
		return "AnalyticModelFromCuvesAndVols: curves=" + curvesMap.keySet();
	}
}
