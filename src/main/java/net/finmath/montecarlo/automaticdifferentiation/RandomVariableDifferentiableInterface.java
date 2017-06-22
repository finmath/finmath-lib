/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 20.06.2017
 */

package net.finmath.montecarlo.automaticdifferentiation;

import java.util.Map;

import net.finmath.stochastic.RandomVariableInterface;

/**
 * @author Christian Fries
 *
 */
public interface RandomVariableDifferentiableInterface extends RandomVariableInterface {
	
	/**
	 * A unique id for this random variable. Will be used in <code>getGradient</code>.
	 * 
	 * @return The id for this random variable.
	 */
	Long getID();
	
	/**
	 * Returns the gradient of this random variable with respect to all its leaf nodes.
	 * The method calculated the map \( v \mapsto \frac{d u}{d v} \) where \( u \) denotes <code>this</code>.
	 * 
	 * @return The gradient map.
	 */
	Map<Long, RandomVariableInterface> getGradient();
}
