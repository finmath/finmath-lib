/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 23.03.2014
 */

package net.finmath.fouriermethod;

import java.util.function.Function;

import org.apache.commons.math3.complex.Complex;

/**
 * Interface which has to be implemented by characteristic functions of
 * random variables, e.g., Fourier transforms of values (payoffs).
 *
 * This is a functional interface.
 *
 * @author Christian Fries
 * @version 1.0
 */
@FunctionalInterface
public interface CharacteristicFunction extends Function<Complex, Complex> {

}
