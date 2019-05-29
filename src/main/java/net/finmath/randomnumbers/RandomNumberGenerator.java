/**
 *
 */
package net.finmath.randomnumbers;

import java.io.Serializable;

/**
 * Interface for an n-dimensional random number generator
 * generating a sequence of vectors sampling the space [0,1]^{n}
 *
 * @author Christian Fries
 * @version 1.0
 */
public interface RandomNumberGenerator extends Serializable {

	double[] getNext();

	int getDimension();
}
