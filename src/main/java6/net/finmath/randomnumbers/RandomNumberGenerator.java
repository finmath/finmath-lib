/**
 *
 */
package net.finmath.randomnumbers;

/**
 * Interface for an n-dimensional random number generator
 * generating a sequence of vectors sampling the space [0,1]^{n}
 *
 * @author Christian Fries
 * @version 1.0
 */
public interface RandomNumberGenerator {

	double[] getNext();

	int getDimension();
}
