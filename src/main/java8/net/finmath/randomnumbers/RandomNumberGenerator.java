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

	/**
	 * Get the next sample vector of dimension n, where n is <code>getDimension</code>.
	 *
	 * An implementation has to be thread safe.
	 *
	 * @return The next sample vector of dimension n, where n is <code>getDimension</code>.
	 */
	double[] getNext();

	/**
	 * Get the sample vector dimension.
	 *
	 * @return The sample vector dimension n.
	 */
	int getDimension();
}
