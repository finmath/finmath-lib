/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 28.11.2012
 */
package net.finmath.marketdata2.model;

import java.util.Map;
import java.util.Set;

import net.finmath.marketdata2.calibration.ParameterObject;
import net.finmath.marketdata2.model.curves.Curve;
import net.finmath.marketdata2.model.curves.DiscountCurveInterface;
import net.finmath.marketdata2.model.curves.ForwardCurveInterface;
import net.finmath.marketdata2.model.volatilities.VolatilitySurface;
import net.finmath.modelling.Model;
import net.finmath.stochastic.RandomVariable;

/**
 * A collection of objects representing analytic valuations, i.e., curves and volatility surfaces.
 *
 * @author Christian Fries
 * @version 1.0
 */
public interface AnalyticModel extends Model, Cloneable {

	RandomVariable getRandomVariableForConstant(double value);

	/**
	 * Get a curve by a given curve name.
	 *
	 * @param name The name of the curve.
	 * @return The curve with the corresponding name, given that it is part of this model, otherwise null is return.
	 */
	Curve getCurve(String name);

	/**
	 * Returns an unmodifiable map of all curves.
	 *
	 * @return Map of all curves.
	 */
	Map<String, Curve> getCurves();

	/**
	 * Add a reference to a given curve under a given name to this model. It is not necessary that the name given agrees with
	 * <code>curve.getName()</code>. This method comes in handy, if you like to create curve mappings.
	 *
	 * @param name Name under which the curve is known in the model.
	 * @param curve The curve.
	 * @return A clone of this model, containing the curves of this model which are not known under the given name and the new curve under the given name.
	 */
	AnalyticModel addCurve(String name, Curve curve);

	/**
	 * Create a new analytic model consisting of a clone of this one together with the given curves added.
	 *
	 * @param curves The set of curves to add.
	 * @return A new analytic model.
	 */
	AnalyticModel addCurves(Curve... curves);

	/**
	 * Create a new analytic model consisting of a clone of this one together with the given curves added.
	 *
	 * @param curves The list of curves to add.
	 * @return A new analytic model.
	 */
	AnalyticModel addCurves(Set<Curve> curves);

	@Deprecated
	void setCurve(Curve curve);

	/**
	 * Returns a discount curve for a given name.
	 *
	 * @param discountCurveName The name of the requested curve.
	 * @return discount curve corresponding to discountCurveName or null if no discountCurve with this name exists in the model
	 */
	DiscountCurveInterface getDiscountCurve(String discountCurveName);

	/**
	 * Returns a forward curve for a given name.
	 *
	 * @param forwardCurveName The name of the requested curve.
	 * @return forward curve corresponding to forwardCurveName or null if no forwardCurve with this name exists in the model
	 */
	ForwardCurveInterface getForwardCurve(String forwardCurveName);

	/**
	 * Returns a volatility surface for a given name.
	 *
	 * @param name THe name of the requested surface.
	 * @return The volatility surface corresponding to the name.
	 */
	VolatilitySurface getVolatilitySurface(String name);

	/**
	 * Returns an unmodifiable map of all volatility surfaces.
	 *
	 * @return Map of all volatility surfaces.
	 */
	Map<String, VolatilitySurface> getVolatilitySurfaces();

	AnalyticModel addVolatilitySurfaces(VolatilitySurface... volatilitySurfaces);

	/**
	 * Create a new analytic model consisting of a clone of this one together with the given volatility surfaces added.
	 *
	 * @param volatilitySurfaces The list of volatility surfaces to add.
	 * @return A new analytic model.
	 */
	AnalyticModel addVolatilitySurfaces(Set<VolatilitySurface> volatilitySurfaces);


	AnalyticModel clone();

	AnalyticModel getCloneForParameter(Map<ParameterObject, RandomVariable[]> curvesParameterPairs) throws CloneNotSupportedException;
}
