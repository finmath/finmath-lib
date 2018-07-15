/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 24.03.2014
 */

package net.finmath.fouriermethod.models;

import net.finmath.fouriermethod.CharacteristicFunctionInterface;
import net.finmath.modelling.ModelInterface;

/**
 * Interface which has to be implemented by models providing the
 * characteristic functions of stochastic processes.
 *
 * @author Christian Fries
 */
@FunctionalInterface
public interface ProcessCharacteristicFunctionInterface extends ModelInterface {

	/**
	 * Returns the characteristic function of X(t), where X is <code>this</code> stochastic process.
	 *
	 * @param time The time at which the stochastic process is observed.
	 * @return The characteristic function of X(t).
	 */
	CharacteristicFunctionInterface apply(double time);
}
