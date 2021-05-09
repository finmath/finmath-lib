package net.finmath.randomnumbers;

import java.io.Serializable;
import java.util.function.DoubleSupplier;

/**
 * Interface for a 1-dimensional random number generator
 * generating a sequence of vectors sampling the space [0,1]
 *
 * @author Christian Fries
 * @version 1.0
 */
public interface RandomNumberGenerator1D extends RandomNumberGenerator, DoubleSupplier {

	/**
	 * Thread safe implementation returning the next double value of
	 * this random number generator.
	 *
	 * @return The next double value of this random number generator
	 */
	double nextDouble();

	/**
	 * Possibly faster, non-thread safe implementation returning the next double value of
	 * this random number generator.
	 *
	 * The user of this method has to ensure synchronization if
	 * this generator is shared by different threads.
	 *
	 * @return The next double value of this random number generator
	 */
	default double nextDoubleFast() {
		return nextDouble();
	}

	@Override
	default double[] getNext() {
		return new double[] { nextDouble() };
	}

	@Override
	default int getDimension() {
		return 1;
	}

	// Alias to function as <code>DoubleSupplier</code>
	@Override
	default double getAsDouble() {
		return nextDouble();
	}
}
