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
	private final RandomVariableFactory			randomVariableFactory;
	private final Map<String, Curve>			curvesMap					= new HashMap<>();
	private final Map<String, VolatilitySurface>	volatilitySurfaceMap	= new HashMap<>();

	/**
	 * Create an empty analytic model.
	 */
	public AnalyticModelFromCurvesAndVols() {
		randomVariableFactory = new RandomVariableFromArrayFactory();
	}

	/**
	 * Create an empty analytic model using a given AbstractRandomVariableFactory for construction of result types.
	 *
	 * @param randomVariableFactory given AbstractRandomVariableFactory for construction of result types.
	 */
	public AnalyticModelFromCurvesAndVols(final RandomVariableFactory randomVariableFactory) {
		this.randomVariableFactory = randomVariableFactory;
	}

	/**
	 * Create an analytic model with the given curves.
	 *
	 * @param curves The vector of curves.
	 */
	public AnalyticModelFromCurvesAndVols(final Curve[] curves) {
		this();
		for (final Curve curve : curves) {
			curvesMap.put(curve.getName(), curve);
		}
	}

	/**
	 * Create an analytic model with the given curves  using a given AbstractRandomVariableFactory for construction of result types.
	 *
	 * @param randomVariableFactory given AbstractRandomVariableFactory for construction of result types.
	 * @param curves The vector of curves.
	 */
	public AnalyticModelFromCurvesAndVols(final RandomVariableFactory randomVariableFactory, final Curve[] curves) {
		this(randomVariableFactory);
		for (final Curve curve : curves) {
			curvesMap.put(curve.getName(), curve);
		}
	}

	/**
	 * Create an analytic model with the given curves.
	 *
	 * @param curves A collection of curves.
	 */
	public AnalyticModelFromCurvesAndVols(final Collection<Curve> curves) {
		this();
		for(final Curve curve : curves) {
			curvesMap.put(curve.getName(), curve);
		}
	}

	@Override
	public RandomVariable getRandomVariableForConstant(final double value) {
		return randomVariableFactory.createRandomVariable(value);
	}

	@Override
	public Curve getCurve(final String name)
	{
		return curvesMap.get(name);
	}

	@Override
	public  Map<String, Curve> getCurves()
	{
		return Collections.unmodifiableMap(curvesMap);
	}

	@Override
	public AnalyticModel addCurve(final String name, final Curve curve) {
		final AnalyticModelFromCurvesAndVols newModel = clone();
		newModel.curvesMap.put(name, curve);
		return newModel;
	}

	public AnalyticModel addCurve(final Curve curve) {
		final AnalyticModelFromCurvesAndVols newModel = clone();
		newModel.curvesMap.put(curve.getName(), curve);
		return newModel;
	}

	@Override
	public AnalyticModel addCurves(final Curve... curves) {
		final AnalyticModelFromCurvesAndVols newModel = clone();
		for(final Curve curve : curves) {
			newModel.curvesMap.put(curve.getName(), curve);
		}
		return newModel;
	}

	@Override
	public AnalyticModel addCurves(final Set<Curve> curves) {
		final AnalyticModelFromCurvesAndVols newModel = clone();
		for(final Curve curve : curves) {
			newModel.curvesMap.put(curve.getName(), curve);
		}
		return newModel;
	}

	/**
	 * @deprecated This class will become immutable. Use addCurve instead.
	 */
	@Override
	@Deprecated
	public void setCurve(final Curve curve)
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
	public void setCurves(final Curve[] curves) {
		for(final Curve curve : curves) {
			setCurve(curve);
		}
	}

	@Override
	public DiscountCurveInterface getDiscountCurve(final String discountCurveName) {
		DiscountCurveInterface discountCurve = null;
		final Curve curve = getCurve(discountCurveName);
		if(DiscountCurveInterface.class.isInstance(curve)) {
			discountCurve = (DiscountCurveInterface)curve;
		}

		return discountCurve;
	}

	@Override
	public ForwardCurveInterface getForwardCurve(final String forwardCurveName) {
		ForwardCurveInterface forwardCurve = null;
		final Curve curve = getCurve(forwardCurveName);
		if(ForwardCurveInterface.class.isInstance(curve)) {
			forwardCurve = (ForwardCurveInterface)curve;
		}

		return forwardCurve;
	}

	@Override
	public VolatilitySurface getVolatilitySurface(final String name) {
		return volatilitySurfaceMap.get(name);
	}

	@Override
	public Map<String, VolatilitySurface> getVolatilitySurfaces() {
		return Collections.unmodifiableMap(volatilitySurfaceMap);
	}

	public AnalyticModel addVolatilitySurface(final VolatilitySurface volatilitySurface)
	{
		final AnalyticModelFromCurvesAndVols newModel = clone();
		newModel.volatilitySurfaceMap.put(volatilitySurface.getName(), volatilitySurface);
		return newModel;
	}

	@Override
	public AnalyticModel addVolatilitySurfaces(final VolatilitySurface... volatilitySurfaces)
	{
		final AnalyticModelFromCurvesAndVols newModel = clone();
		for(final VolatilitySurface volatilitySurface : volatilitySurfaces) {
			newModel.volatilitySurfaceMap.put(volatilitySurface.getName(), volatilitySurface);
		}
		return newModel;
	}

	@Override
	public AnalyticModel addVolatilitySurfaces(final Set<VolatilitySurface> volatilitySurfaces) {
		final AnalyticModelFromCurvesAndVols newModel = clone();
		for(final VolatilitySurface volatilitySurface : volatilitySurfaces) {
			newModel.volatilitySurfaceMap.put(volatilitySurface.getName(), volatilitySurface);
		}
		return newModel;
	}

	private void setVolatilitySurface(final VolatilitySurface volatilitySurface)
	{
		volatilitySurfaceMap.put(volatilitySurface.getName(), volatilitySurface);
	}

	private void set(final Object marketDataObject) {
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
		final AnalyticModelFromCurvesAndVols newModel = new AnalyticModelFromCurvesAndVols();
		newModel.curvesMap.putAll(curvesMap);
		newModel.volatilitySurfaceMap.putAll(volatilitySurfaceMap);
		return newModel;
	}

	@Override
	public AnalyticModel getCloneForParameter(final Map<ParameterObject, RandomVariable[]> curveParameterPairs) throws CloneNotSupportedException {

		// Build the modified clone of this model
		final AnalyticModelFromCurvesAndVols modelClone = clone();

		// Add modified clones of curves to model clone
		if(curveParameterPairs != null) {
			for(final Entry<ParameterObject, RandomVariable[]> curveParameterPair : curveParameterPairs.entrySet()) {
				final ParameterObject newCurve = curveParameterPair.getKey().getCloneForParameter(curveParameterPair.getValue());
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
