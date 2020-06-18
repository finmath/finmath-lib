package net.finmath.randomnumbers;

/**
 * Implements a multi-dimensional Sobol sequence.
 *
 * The class is just a wrapper to Apache commons-math implementation
 * in order to implement the interfaces <code>RandomNumberGenerator1D</code>
 * and {@link java.util.function.DoubleSupplier}.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class SobolSequence1D implements RandomNumberGenerator1D {

	private static final long serialVersionUID = 1368710922067034251L;

	private final SobolSequence sobolSequence;

	/**
	 * Create a Sobol sequence.
	 */
	public SobolSequence1D() {
		super();
		sobolSequence = new SobolSequence(1);
	}

	@Override
	public double nextDouble() {
		return sobolSequence.getNext()[0];
	}
}
