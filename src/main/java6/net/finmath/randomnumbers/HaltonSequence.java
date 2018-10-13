/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christianfries.com.
 *
 * Created on 21 May 2018
 */
package net.finmath.randomnumbers;

/**
 * Implements a multi-dimensional Halton sequence (quasi random numbers) with the given bases.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class HaltonSequence implements RandomNumberGenerator {

	private final int base[];

	private int currentIndex = 0;

	/**
	 * Constructs a Halton sequence with the given bases.
	 *
	 * The bases should be integers without common divisor greater than 1, for example, prime numbers.
	 *
	 * @param base The array of base integers. The length of the array defines the dimension of the sequence.
	 */
	public HaltonSequence(int[] base) {
		for(int i=0; i<base.length; i++) {
			if(base[i] <= 1) {
				throw new IllegalArgumentException("base needs to be larger than 1");
			}
		}

		this.base = base;
	}

	@Override
	public double[] getNext() {
		return getHaltonNumber(currentIndex++);
	}

	@Override
	public int getDimension() {
		return base.length;
	}

	public double[] getHaltonNumber(long index) {
		double[] x = new double[base.length];
		for(int dimension = 0; dimension<base.length; dimension++) {
			x[dimension] = getHaltonNumberForGivenBase(index, base[dimension]);
		}
		return x;
	}

	public double getHaltonNumber(long index, int dimension) {
		return getHaltonNumberForGivenBase(index, base[dimension]);
	}

	/**
	 * Return a Halton number, sequence starting at index = 0, base &gt; 1.
	 *
	 * @param index The index of the sequence.
	 * @param base The base of the sequence. Has to be greater than one (this is not checked).
	 * @return The Halton number.
	 */
	public static double getHaltonNumberForGivenBase(long index, int base) {
		index += 1;

		double x = 0.0;
		double factor = 1.0 / base;
		while(index > 0) {
			x += (index % base) * factor;
			factor /= base;
			index /= base;
		}

		return x;
	}
}
