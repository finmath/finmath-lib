/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 28.11.2012
 */
package net.finmath.marketdata.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.finmath.marketdata.calibration.ParameterObject;
import net.finmath.marketdata.model.curves.Curve;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.model.volatilities.VolatilitySurface;

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

	private static final long serialVersionUID = 6906386712907555046L;

	private final LocalDate referenceDate;

	private final Map<String, Curve>				curvesMap				= new HashMap<>();
	private final Map<String, VolatilitySurface>	volatilitySurfaceMap	= new HashMap<>();


	/**
	 * Create an empty analytic model.
	 */
	public AnalyticModelFromCurvesAndVols() {
		referenceDate = null;
	}

	/**
	 * Create an empty analytic model for a specified date.
	 *
	 * @param referenceDate The reference date that should be used for all curves and surfaces of this model.
	 */
	public AnalyticModelFromCurvesAndVols(LocalDate referenceDate) {
		this.referenceDate = referenceDate;
	}

	/**
	 * Create an analytic model with the given curves.
	 *
	 * @param curves The vector of curves.
	 */
	public AnalyticModelFromCurvesAndVols(Curve[] curves) {
		for (Curve curve : curves) {
			curvesMap.put(curve.getName(), curve);
		}
		this.referenceDate = null;
	}

	/**
	 * Create an analytic model with the given curves.
	 *
	 * @param curves A collection of curves.
	 */
	public AnalyticModelFromCurvesAndVols(Collection<Curve> curves) {
		for (Curve curve : curves) {
			curvesMap.put(curve.getName(), curve);
		}
		this.referenceDate = null;
	}

	/**
	 * Create an analytic model with the given curves for the specified reference date.
	 *
	 * @param referenceDate The reference date that should be used for all curves and surfaces of this model.
	 * @param curves The vector of curves.
	 */
	public AnalyticModelFromCurvesAndVols(LocalDate referenceDate, Curve[] curves) {
		for (Curve curve : curves) {
			curvesMap.put(curve.getName(), curve);
			LocalDate curveDate = curve.getReferenceDate();
			if(referenceDate != null && curveDate != null && ! referenceDate.equals(curveDate)) {
				throw new IllegalArgumentException("Reference date of curve "+curve.getName()+" does not match the reference date of the model.");
			}
		}
		this.referenceDate = referenceDate;
	}

	/**
	 * Create an analytic model with the given curves for the specified reference date.
	 *
	 * @param referenceDate The reference date that should be used for all curves and surfaces of this model.
	 * @param curves A collection of curves.
	 */
	public AnalyticModelFromCurvesAndVols(LocalDate referenceDate, Collection<Curve> curves) {
		for (Curve curve : curves) {
			curvesMap.put(curve.getName(), curve);
			LocalDate curveDate = curve.getReferenceDate();
			if(referenceDate != null && curveDate != null && ! referenceDate.equals(curveDate)) {
				throw new IllegalArgumentException("Reference date of curve "+curve.getName()+" does not match the reference date of the model.");
			}
		}
		this.referenceDate = referenceDate;
	}

	/**
	 * Create an analytic model for the specified reference date, together with curves and volatility surfaces, each with their specific name.
	 *
	 * @param referenceDate The reference date that should be used for all curves and surfaces of this model.
	 * @param curvesMap A map containing all curves, together with their names they should have in the model.
	 * @param volatilitySurfaceMap A map containing all volatility surfaces, together with their names they should have in the model.
	 */
	public AnalyticModelFromCurvesAndVols(LocalDate referenceDate, Map<String, Curve> curvesMap, Map<String, VolatilitySurface> volatilitySurfaceMap) {
		this(referenceDate);
		this.curvesMap.putAll(curvesMap);
		this.volatilitySurfaceMap.putAll(volatilitySurfaceMap);
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
		LocalDate curveDate = curve.getReferenceDate();

		if(referenceDate != null && curveDate != null && ! referenceDate.equals(curveDate)) {
			throw new IllegalArgumentException("Reference date of curve does not match reference date of model.");
		}

		AnalyticModelFromCurvesAndVols newModel = clone();
		newModel.curvesMap.put(name, curve);

		return newModel;
	}

	public AnalyticModel addCurve(Curve curve) {
		LocalDate curveDate = curve.getReferenceDate();

		if(referenceDate != null && curveDate != null && ! referenceDate.equals(curveDate)) {
			throw new IllegalArgumentException("Reference date of curve does not match reference date of model.");
		}

		AnalyticModelFromCurvesAndVols newModel = clone();
		newModel.curvesMap.put(curve.getName(), curve);

		return newModel;
	}

	@Override
	public AnalyticModel addCurves(Curve... curves) {
		Map<String, Curve>	curvesMap	= new HashMap<>();

		for (Curve curve : curves) {
			curvesMap.put(curve.getName(), curve);

			LocalDate curveDate = curve.getReferenceDate();
			if(referenceDate != null && curveDate != null && ! referenceDate.equals(curveDate) ) {
				throw new IllegalArgumentException("Reference date of curve "+curve.getName()+" does not match the reference date of the model.");
			}
		}

		AnalyticModelFromCurvesAndVols newModel = clone();
		newModel.curvesMap.putAll(curvesMap);

		return newModel;
	}

	@Override
	public AnalyticModel addCurves(Set<Curve> curves) {
		Map<String, Curve>	curvesMap	= new HashMap<>();

		for (Curve curve : curves) {
			curvesMap.put(curve.getName(), curve);

			LocalDate curveDate = curve.getReferenceDate();
			if(referenceDate != null && curveDate != null && ! referenceDate.equals(curveDate) ) {
				throw new IllegalArgumentException("Reference date of curve "+curve.getName()+" does not match the reference date of the model.");
			}
		}

		AnalyticModelFromCurvesAndVols newModel = clone();
		newModel.curvesMap.putAll(curvesMap);

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
	public DiscountCurve getDiscountCurve(String discountCurveName) {
		DiscountCurve discountCurve = null;
		Curve curve = getCurve(discountCurveName);
		if(DiscountCurve.class.isInstance(curve)) {
			discountCurve = (DiscountCurve)curve;
		}

		return discountCurve;
	}

	@Override
	public ForwardCurve getForwardCurve(String forwardCurveName) {
		ForwardCurve forwardCurve = null;
		Curve curve = getCurve(forwardCurveName);
		if(ForwardCurve.class.isInstance(curve)) {
			forwardCurve = (ForwardCurve)curve;
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
		LocalDate surfaceDate = volatilitySurface.getReferenceDate();

		if(referenceDate != null && surfaceDate != null && ! referenceDate.equals(surfaceDate)) {
			throw new IllegalArgumentException("Reference date of surface does not match reference date of model.");
		}

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

			LocalDate surfaceDate = volatilitySurface.getReferenceDate();
			if(referenceDate != null && surfaceDate != null && ! referenceDate.equals(surfaceDate) ) {
				throw new IllegalArgumentException("Reference date of surface "+volatilitySurface.getName()+" does not match the reference date of the model.");
			}
		}
		return newModel;
	}

	@Override
	public AnalyticModel addVolatilitySurfaces(Set<VolatilitySurface> volatilitySurfaces) {
		AnalyticModelFromCurvesAndVols newModel = clone();
		for(VolatilitySurface volatilitySurface : volatilitySurfaces) {
			newModel.volatilitySurfaceMap.put(volatilitySurface.getName(), volatilitySurface);

			LocalDate surfaceDate = volatilitySurface.getReferenceDate();
			if(referenceDate != null && surfaceDate != null && ! referenceDate.equals(surfaceDate) ) {
				throw new IllegalArgumentException("Reference date of surface "+volatilitySurface.getName()+" does not match the reference date of the model.");
			}
		}
		return newModel;
	}

	/**
	 * @deprecated This class will become immutable. Use addVolatilitySurface instead.
	 */
	@Override
	@Deprecated
	public void setVolatilitySurface(VolatilitySurface volatilitySurface)
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
		AnalyticModelFromCurvesAndVols newModel = new AnalyticModelFromCurvesAndVols(referenceDate);
		newModel.curvesMap.putAll(curvesMap);
		newModel.volatilitySurfaceMap.putAll(volatilitySurfaceMap);
		return newModel;
	}

	@Override
	public AnalyticModel getCloneForParameter(Map<ParameterObject, double[]> curveParameterPairs) throws CloneNotSupportedException {

		// Build the modified clone of this model
		AnalyticModelFromCurvesAndVols modelClone = clone();

		// Add modified clones of curves to model clone
		if(curveParameterPairs != null) {
			for(Entry<ParameterObject,double[]> curveParameterPair : curveParameterPairs.entrySet()) {
				ParameterObject newCurve = curveParameterPair.getKey().getCloneForParameter(curveParameterPair.getValue());
				modelClone.set(newCurve);
			}
		}

		return modelClone;
	}

	@Override
	public String toString() {
		return "AnalyticModelFromCuvesAndVols: referenceDate=" + referenceDate + ", curves=" + curvesMap.keySet() + ", volatilitySurfaces=" + volatilitySurfaceMap.keySet();
	}

	/**
	 * Returns the reference date of the curves of this model.
	 *
	 * @return The reference date of the model.
	 */
	public LocalDate getReferenceDate() {
		return referenceDate;
	}

}
