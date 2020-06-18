package net.finmath.randomnumbers;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.Validate;

/**
 * A van-der-Corput sequence \( \{ x_{i} \vert i = 0, 1, \ldots \} \) implementing
 * {@link net.finmath.randomnumbers.RandomNumberGenerator1D}.
 *
 * @author Christian Fries
 */
public class VanDerCorputSequence implements RandomNumberGenerator1D {

	private static final long serialVersionUID = 1368710922067034251L;

	private final AtomicInteger index;
	private final int base;

	public static void main(String[] args) {
		for(int i=0; i<30; i++) {
			final double x = getVanDerCorputNumber(i, 2);
			System.out.println(i + "\t" + x);
		}
	}

	/**
	 * Create a van-der-Corput sequence for a given start index and base.
	 *
	 * @param startIndex The start index. Must be &ge; 0.
	 * @param base The base of the sequence. Must be &gt; 1.
	 */
	public VanDerCorputSequence(int startIndex, int base) {
		super();
		Validate.isTrue(startIndex >= 0, "Parameter base startIndex be >= 0.");
		Validate.isTrue(base > 1, "Parameter base must be > 1.");
		this.index = new AtomicInteger(startIndex);
		this.base = base;
	}

	public VanDerCorputSequence(int base) {
		this(0, base);
	}

	@Override
	public double nextDouble() {
		return getVanDerCorputNumber(index.getAndIncrement(), base);
	}

	/**
	 * Return the van-der-Corput number.
	 *
	 * @param index The index of the sequence starting with 0
	 * @param base The base.
	 * @return The van der Corput number
	 */
	public static double getVanDerCorputNumber(long index, int base) {

		index = index + 1;

		double x = 0.0;
		double refinementFactor = 1.0 / base;

		while(index > 0) {
			x += (index % base) * refinementFactor;
			index = index / base;
			refinementFactor = refinementFactor / base;
		}

		return x;
	}
}
