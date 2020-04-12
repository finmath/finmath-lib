/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christianfries.com.
 *
 * Created on 02.07.2017
 */

package net.finmath.montecarlo.automaticdifferentiation;

import java.util.Map;

import net.finmath.stochastic.RandomVariable;

/**
 * Interface implemented by model which can provide their independent model parameters.
 *
 * This is useful for the model independent calculation of derivatives using AAD.
 *
 * @author Christian Fries
 */
public interface IndependentModelParameterProvider {

	/**
	 * Returns a map of independent model parameters of this model.
	 *
	 * @return Map of independent model parameters of this model.
	 */
	default Map<String, RandomVariable> getModelParameters() {
		// TODO Remove default implementation in 4.0.0 version to enforce models complying with this interface. NOTE: The default here is to allow backward compatibility.
		throw new UnsupportedOperationException();
	}
}
