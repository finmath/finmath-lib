/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christianfries.com.
 *
 * Created on 17.06.2017
 */
package net.finmath.montecarlo.automaticdifferentiation;

import java.util.Map;
import java.util.Set;

import net.finmath.stochastic.RandomVariable;

/**
 * Interface providing additional methods for
 * random variable implementing <code>RandomVariable</code>
 * allowing automatic differentiation.
 *
 * The interface will introduce three additional methods:
 * <code>Long getID()</code> and
 * <code>Map&lt;Long, RandomVariable&gt; getGradient()</code>
 * and
 * <code>Map&lt;Long, RandomVariable&gt; getTangents()</code>.
 *
 * The method <code>getGradient</code> will return a map providing the first order
 * differentiation of the given random variable (this) with respect to
 * <i>all</i> its input <code>RandomVariableDifferentiable</code>s.
 *
 * The method <code>getTangents</code> will return a map providing the first order
 * differentiation of <i>all</i> dependent random variables with respect to the
 * given random variable (this).
 *
 * To get the differentiation dY/dX of Y with respect to a specific object X using backward mode (getGradient) use
 * <code>
 * 		Map gradient = Y.getGradient();
 * 		RandomVariable derivative = Y.get(X.getID());
 * </code>
 *
 * To get the differentiation dY/dX of Y with respect to a specific object X using forward mode (getTanget) use
 * <code>
 * 		Map tangent = X.getTangent();
 * 		RandomVariable derivative = X.get(Y.getID());
 * </code>
 *
 * Note: Some implementations may allow limit the result of the gradient to leave nodes or the result of the tangent to terminal nodes.
 *
 * @author Christian Fries
 * @version 1.0
 */
public interface RandomVariableDifferentiable extends RandomVariable {

	/**
	 * A unique id for this random variable. Will be used in <code>getGradient</code>.
	 *
	 * @return The id for this random variable.
	 */
	Long getID();

	/**
	 * Returns the gradient of this random variable with respect to all its leaf nodes.
	 * The method calculates the map \( v \mapsto \frac{d u}{d v} \) where \( u \) denotes <code>this</code>.
	 *
	 * @return The gradient map.
	 */
	default Map<Long, RandomVariable> getGradient() {
		return getGradient(null);
	}

	/**
	 * Returns the gradient of this random variable with respect to the given IDs.
	 * The method calculates the map \( v \mapsto \frac{d u}{d v} \) where \( u \) denotes <code>this</code>.
	 *
	 * @param independentIDs {@link Set} of IDs of random variables \( v \) with respect to which the gradients \( \frac{d u}{d v} \) will be calculated. If null, derivatives w.r.t. all known independents are returned.
	 * @return The gradient map.
	 */
	Map<Long, RandomVariable> getGradient(Set<Long> independentIDs);

	/**
	 * Returns the tangents of this random variable with respect to all its dependent nodes.
	 * The method calculated the map \( u \mapsto \frac{d u}{d v} \) where \( v \) denotes <code>this</code>.
	 *
	 * @return The map of all tangents .
	 */
	default Map<Long, RandomVariable> getTangents() {
		return getTangents(null);
	}

	/**
	 * Returns the tangents of this random variable with respect to the given dependent node IDs (if dependent).
	 * The method calculated the map \( u \mapsto \frac{d u}{d v} \) where \( v \) denotes <code>this</code>.
	 *
	 * @param dependentIDs {@link Set} of IDs of random variables \( u \) with respect to which the differentials \( \frac{d u}{d v} \) will be calculated.
	 * If null, derivatives w.r.t. all known dependents are returned.
	 * @return The map of differentials.
	 */
	Map<Long, RandomVariable> getTangents(Set<Long> dependentIDs);

	/**
	 * Returns a clone of this differentiable random variable with a new ID. This implies that the
	 * random variable is a leaf node and independent from all previous calculations.
	 *
	 * @return A clone of this differentiable random variable with a new ID.
	 */
	default RandomVariableDifferentiable getCloneIndependent() {
		throw new UnsupportedOperationException("Cloning not supported. Please add implementation of getCloneIndependent.");
	}
}
