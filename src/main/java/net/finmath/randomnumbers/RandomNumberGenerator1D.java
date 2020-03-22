/**
 *
 */
package net.finmath.randomnumbers;

import java.io.Serializable;

/**
 * Interface for a 1-dimensional random number generator
 * generating a sequence of vectors sampling the space [0,1]
 *
 * @author Christian Fries
 * @version 1.0
 */
public interface RandomNumberGenerator1D extends RandomNumberGenerator, Serializable {

	double nextDouble();
	
	default double[] getNext() {
		return new double[] { nextDouble() };
	}

	default public int getDimension() {
		return 1;
	}
}
