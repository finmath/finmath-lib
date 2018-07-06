/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christianfries.com.
 *
 * Created on 21 May 2018
 */
package net.finmath.randomnumbers;

public class HaltonSequence implements RandomNumberGenerator {

	private final int base[];

	private int currentIndex = 0;

	/**
	 * Constructs a Halton sequence with the given base.
	 * 
	 * @param base
	 */
	public HaltonSequence(int[] base) {
		for(int i=0; i<base.length; i++) {
			if(base[i] <= 1) throw new IllegalArgumentException("base needs to be larger than 1");
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
	 * Return a Halton number, sequence starting at index = 0, base >= 2.
	 * 
	 * @param index The index of the sequence.
	 * @param base The base of the sequence.
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
