/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 24.03.2014
 */

package net.finmath.fouriermethod.models;

import net.finmath.fouriermethod.CharacteristicFunction;
import net.finmath.modelling.Model;

/**
 * Interface which has to be implemented by models providing the
 * characteristic functions of stochastic processes.
 *
 * @author Christian Fries
 * @version 1.0
 */
@FunctionalInterface
public interface CharacteristicFunctionModel extends Model {

	/**
	 * Returns the characteristic function of X(t), where X is <code>this</code> stochastic process.
	 *
	 * @param time The time at which the stochastic process is observed.
	 * @return The characteristic function of X(t).
	 */
	CharacteristicFunction apply(double time);
}
