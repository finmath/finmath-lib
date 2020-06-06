/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 17.06.2016
 */

package net.finmath.randomnumbers;

import java.io.Serializable;

/**
 * Mersenne Twister random number generator. This class is just a wrapper of
 * <code>org.apache.commons.math3.random.MersenneTwister</code>.
 *
 * @author Christian Fries
 *
 * @version 1.0
 */
public class MersenneTwister implements RandomNumberGenerator1D, Serializable {

	private static final long serialVersionUID = -1827470318370174186L;

	private final org.apache.commons.math3.random.MersenneTwister mersenneTwister;

	public MersenneTwister(final long seed) {
		mersenneTwister	= new org.apache.commons.math3.random.MersenneTwister(seed);
	}

	public MersenneTwister() {
		mersenneTwister	= new org.apache.commons.math3.random.MersenneTwister();
	}

	/**
	 * Returns the next random number in the sequence.
	 *
	 * @return The next random number in the sequence.
	 */
	@Override
	public double nextDouble() {
		synchronized (mersenneTwister) {
			return mersenneTwister.nextDouble();
		}
	}

	@Override
	public double nextDoubleFast() {
		return mersenneTwister.nextDouble();
	}
}
