/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 23.03.2014
 */

package net.finmath.fouriermethod;

import net.finmath.compatibility.java.util.function.Function;

import org.apache.commons.math3.complex.Complex;

/**
 * Interface which has to be implemented by characteristic functions of
 * random variables, e.g., Fourier transforms of values (payoffs).
 * 
 * This is a functional interface.
 * 
 * @author Christian Fries
 */
public interface CharacteristicFunctionInterface extends Function<Complex, Complex> {

}
