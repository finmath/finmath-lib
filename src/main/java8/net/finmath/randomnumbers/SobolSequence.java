/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christianfries.com.
 *
 * Created on 21 May 2018
 */
package net.finmath.randomnumbers;

import org.apache.commons.math3.random.SobolSequenceGenerator;

/**
 * Implements a multi-dimensional Sobol sequence.
 *
 * The class is just a wrapper to Apache commons-math implementation
 * in order to implement the interface <code>RandomNumberGenerator</code>
 * with a minimal change. The value 0, the first element of the sequence, is omitted.
 *
 *
 * @author Christian Fries
 * @version 1.0
 */
public class SobolSequence implements RandomNumberGenerator {

	private static final long serialVersionUID = -1904010803493075019L;

	private final int dimension;

	private final SobolSequenceGenerator generator;

	/**
	 * Constructs a Sobol sequence with given dimension.
	 *
	 * @param dimension The dimension of the sequence.
	 */
	public SobolSequence(final int dimension) {
		this.dimension = dimension;
		generator = new SobolSequenceGenerator(dimension);
	}

	@Override
	public double[] getNext() {
		synchronized (generator) {
			return generator.nextVector();
		}
	}

	@Override
	public int getDimension() {
		return dimension;
	}
}
