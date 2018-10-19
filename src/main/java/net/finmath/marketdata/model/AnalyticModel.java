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

import net.finmath.marketdata.calibration.ParameterObjectInterface;
import net.finmath.marketdata.model.curves.CurveInterface;
import net.finmath.marketdata.model.curves.DiscountCurveInterface;
import net.finmath.marketdata.model.curves.ForwardCurveInterface;
import net.finmath.marketdata.model.volatilities.VolatilitySurfaceInterface;
import net.finmath.modelling.DescribedModel;
import net.finmath.modelling.DescribedProduct;
import net.finmath.modelling.ProductDescriptor;
import net.finmath.modelling.descriptor.AnalyticModelDescriptor;
import net.finmath.modelling.productfactory.InterestRateAnalyticProductFactory;

/**
 * Implements a collection of market data objects (e.g., discount curves, forward curve)
 * which provide interpolation of market data or other derived quantities
 * ("calibrated curves"). This can be seen as a model to be used in analytic pricing
 * formulas - hence this class is termed <code>AnalyticModel</code>.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class AnalyticModel implements AnalyticModelInterface, Serializable, Cloneable, DescribedModel<AnalyticModelDescriptor> {

	private static final long serialVersionUID = 6906386712907555046L;

	private final LocalDate referenceDate;

	private final Map<String, CurveInterface>				curvesMap				= new HashMap<>();
	private final Map<String, VolatilitySurfaceInterface>	volatilitySurfaceMap	= new HashMap<>();


	/**
	 * Create an empty analytic model.
	 */
	public AnalyticModel() {
		referenceDate = null;
	}

	/**
	 * Create an empty analytic model for a specified date.
	 *
	 * @param referenceDate The reference date that should be used for all curves and surfaces of this model.
	 */
	public AnalyticModel(LocalDate referenceDate) {
		this.referenceDate = referenceDate;
	}

	/**
	 * Create an analytic model with the given curves.
	 *
	 * @param curves The vector of curves.
	 */
	public AnalyticModel(CurveInterface[] curves) {
		for (CurveInterface curve : curves) {
			curvesMap.put(curve.getName(), curve);
		}
		this.referenceDate = null;
	}

	/**
	 * Create an analytic model with the given curves.
	 *
	 * @param curves A collection of curves.
	 */
	public AnalyticModel(Collection<CurveInterface> curves) {
		for (CurveInterface curve : curves) {
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
	public AnalyticModel(LocalDate referenceDate, CurveInterface[] curves) {
		for (CurveInterface curve : curves) {
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
	public AnalyticModel(LocalDate referenceDate, Collection<CurveInterface> curves) {
		for (CurveInterface curve : curves) {
			curvesMap.put(curve.getName(), curve);
			LocalDate curveDate = curve.getReferenceDate();
			if(referenceDate != null && curveDate != null && ! referenceDate.equals(curveDate)) {
				throw new IllegalArgumentException("Reference date of curve "+curve.getName()+" does not match the reference date of the model.");
			}
		}
		this.referenceDate = referenceDate;
	}

	@Override
	public CurveInterface getCurve(String name)
	{
		return curvesMap.get(name);
	}

	@Override
	public  Map<String, CurveInterface> getCurves()
	{
		return Collections.unmodifiableMap(curvesMap);
	}

	@Override
	public AnalyticModelInterface addCurve(String name, CurveInterface curve) {
		LocalDate curveDate = curve.getReferenceDate();

		if(referenceDate != null && curveDate != null && ! referenceDate.equals(curveDate)) {
			throw new IllegalArgumentException("Reference date of curve does not match reference date of model.");
		}

		AnalyticModel newModel = clone();
		newModel.curvesMap.put(name, curve);

		return newModel;
	}

	public AnalyticModelInterface addCurve(CurveInterface curve) {
		LocalDate curveDate = curve.getReferenceDate();

		if(referenceDate != null && curveDate != null && ! referenceDate.equals(curveDate)) {
			throw new IllegalArgumentException("Reference date of curve does not match reference date of model.");
		}

		AnalyticModel newModel = clone();
		newModel.curvesMap.put(curve.getName(), curve);

		return newModel;
	}

	@Override
	public AnalyticModelInterface addCurves(CurveInterface... curves) {
		Map<String, CurveInterface>	curvesMap	= new HashMap<>();

		for (CurveInterface curve : curves) {
			curvesMap.put(curve.getName(), curve);

			LocalDate curveDate = curve.getReferenceDate();
			if(referenceDate != null && curveDate != null && ! referenceDate.equals(curveDate) ) {
				throw new IllegalArgumentException("Reference date of curve "+curve.getName()+" does not match the reference date of the model.");
			}
		}

		AnalyticModel newModel = clone();
		newModel.curvesMap.putAll(curvesMap);

		return newModel;
	}

	@Override
	public AnalyticModelInterface addCurves(Set<CurveInterface> curves) {
		Map<String, CurveInterface>	curvesMap	= new HashMap<>();

		for (CurveInterface curve : curves) {
			curvesMap.put(curve.getName(), curve);

			LocalDate curveDate = curve.getReferenceDate();
			if(referenceDate != null && curveDate != null && ! referenceDate.equals(curveDate) ) {
				throw new IllegalArgumentException("Reference date of curve "+curve.getName()+" does not match the reference date of the model.");
			}
		}

		AnalyticModel newModel = clone();
		newModel.curvesMap.putAll(curvesMap);

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
		for(CurveInterface curve : curves) {
			setCurve(curve);
		}
	}

	@Override
	public DiscountCurveInterface getDiscountCurve(String discountCurveName) {
		DiscountCurveInterface discountCurve = null;
		CurveInterface curve = getCurve(discountCurveName);
		if(DiscountCurveInterface.class.isInstance(curve)) {
			discountCurve = (DiscountCurveInterface)curve;
		}

		return discountCurve;
	}

	@Override
	public ForwardCurveInterface getForwardCurve(String forwardCurveName) {
		ForwardCurveInterface forwardCurve = null;
		CurveInterface curve = getCurve(forwardCurveName);
		if(ForwardCurveInterface.class.isInstance(curve)) {
			forwardCurve = (ForwardCurveInterface)curve;
		}

		return forwardCurve;
	}

	@Override
	public VolatilitySurfaceInterface getVolatilitySurface(String name) {
		return volatilitySurfaceMap.get(name);
	}

	@Override
	public Map<String, VolatilitySurfaceInterface> getVolatilitySurfaces() {
		return Collections.unmodifiableMap(volatilitySurfaceMap);
	}

	public AnalyticModelInterface addVolatilitySurface(VolatilitySurfaceInterface volatilitySurface)
	{
		LocalDate surfaceDate = volatilitySurface.getReferenceDate();

		if(referenceDate != null && surfaceDate != null && ! referenceDate.equals(surfaceDate)) {
			throw new IllegalArgumentException("Reference date of surface does not match reference date of model.");
		}

		AnalyticModel newModel = clone();
		newModel.volatilitySurfaceMap.put(volatilitySurface.getName(), volatilitySurface);

		return newModel;
	}

	@Override
	public AnalyticModelInterface addVolatilitySurfaces(VolatilitySurfaceInterface... volatilitySurfaces)
	{
		AnalyticModel newModel = clone();
		for(VolatilitySurfaceInterface volatilitySurface : volatilitySurfaces) {
			newModel.volatilitySurfaceMap.put(volatilitySurface.getName(), volatilitySurface);

			LocalDate surfaceDate = volatilitySurface.getReferenceDate();
			if(referenceDate != null && surfaceDate != null && ! referenceDate.equals(surfaceDate) ) {
				throw new IllegalArgumentException("Reference date of surface "+volatilitySurface.getName()+" does not match the reference date of the model.");
			}
		}
		return newModel;
	}

	@Override
	public AnalyticModelInterface addVolatilitySurfaces(Set<VolatilitySurfaceInterface> volatilitySurfaces) {
		AnalyticModel newModel = clone();
		for(VolatilitySurfaceInterface volatilitySurface : volatilitySurfaces) {
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
	public void setVolatilitySurface(VolatilitySurfaceInterface volatilitySurface)
	{
		volatilitySurfaceMap.put(volatilitySurface.getName(), volatilitySurface);
	}

	private void set(Object marketDataObject) {
		if(marketDataObject instanceof CurveInterface) {
			setCurve((CurveInterface)marketDataObject);
		} else if(marketDataObject instanceof VolatilitySurfaceInterface) {
			setVolatilitySurface((VolatilitySurfaceInterface)marketDataObject);
		} else {
			throw new IllegalArgumentException("Provided object is not of supported type.");
		}
	}

	@Override
	public AnalyticModel clone()
	{
		AnalyticModel newModel = new AnalyticModel(referenceDate);
		newModel.curvesMap.putAll(curvesMap);
		newModel.volatilitySurfaceMap.putAll(volatilitySurfaceMap);
		return newModel;
	}

	@Override
	public AnalyticModelInterface getCloneForParameter(Map<ParameterObjectInterface, double[]> curveParameterPairs) throws CloneNotSupportedException {

		// Build the modified clone of this model
		AnalyticModel modelClone = clone();

		// Add modified clones of curves to model clone
		if(curveParameterPairs != null) {
			for(Entry<ParameterObjectInterface,double[]> curveParameterPair : curveParameterPairs.entrySet()) {
				ParameterObjectInterface newCurve = curveParameterPair.getKey().getCloneForParameter(curveParameterPair.getValue());
				modelClone.set(newCurve);
			}
		}

		return modelClone;
	}

	@Override
	public String toString() {
		return "AnalyticModel: referenceDate=" + referenceDate.toString() + ", curves=" + curvesMap.keySet() + ", volatilitySurfaces=" + volatilitySurfaceMap.keySet();
	}

	/**
	 * Returns the reference date of the curves of this model.
	 *
	 * @return The reference date of the model.
	 */
	public LocalDate getReferenceDate() {
		return referenceDate;
	}

	@Override
	public AnalyticModelDescriptor getDescriptor() {
		return new AnalyticModelDescriptor(getCurves(), Collections.unmodifiableMap(volatilitySurfaceMap));
	}

	@Override
	public DescribedProduct<? extends ProductDescriptor> getProductFromDescriptor(ProductDescriptor productDescriptor) {

		return new  InterestRateAnalyticProductFactory(referenceDate).getProductFromDescriptor(productDescriptor);

	}

}
