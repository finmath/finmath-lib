package net.finmath.finitedifference.solvers.adi;

/**
 * Utility methods for indexing and boundary handling on a three-dimensional
 * grid.
 *
 * <p>
 * The flattening convention is
 * </p>
 * <pre>
 * k = i0 + n0 * (i1 + n1 * i2)
 * </pre>
 *
 * <p>
 * This class is intentionally stateless and generic.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public final class FDM3DGridUtil {

	private FDM3DGridUtil() {
	}

	/**
	 * Performs the operation.
	 *
	 * @param i0 The value.
	 * @param i1 The value.
	 * @param i2 The value.
	 * @param n0 The value.
	 * @param n1 The value.
	 * @return The value.
	 */
	public static int flatten(
			final int i0,
			final int i1,
			final int i2,
			final int n0,
			final int n1) {
		return i0 + n0 * (i1 + n1 * i2);
	}

	/**
	 * Performs the operation.
	 *
	 * @param flatIndex The value.
	 * @param n0 The value.
	 * @param n1 The value.
	 * @param n2 The value.
	 * @return The value.
	 */
	public static int[] unflatten(
			final int flatIndex,
			final int n0,
			final int n1,
			final int n2) {

		final int n = n0 * n1 * n2;
		if (flatIndex < 0 || flatIndex >= n) {
			throw new IllegalArgumentException("flatIndex out of range.");
		}

		final int i2 = flatIndex / (n0 * n1);
		final int remainder = flatIndex - i2 * n0 * n1;
		final int i1 = remainder / n0;
		final int i0 = remainder - i1 * n0;

		return new int[] {i0, i1, i2 };
	}

	/**
	 * Returns whether the condition holds.
	 *
	 * @param i0 The value.
	 * @return The value.
	 */
	public static boolean isOnLowerBoundaryFirstDirection(final int i0) {
		return i0 == 0;
	}

	/**
	 * Returns whether the condition holds.
	 *
	 * @param i0 The value.
	 * @param n0 The value.
	 * @return The value.
	 */
	public static boolean isOnUpperBoundaryFirstDirection(final int i0, final int n0) {
		return i0 == n0 - 1;
	}

	/**
	 * Returns whether the condition holds.
	 *
	 * @param i1 The value.
	 * @return The value.
	 */
	public static boolean isOnLowerBoundarySecondDirection(final int i1) {
		return i1 == 0;
	}

	/**
	 * Returns whether the condition holds.
	 *
	 * @param i1 The value.
	 * @param n1 The value.
	 * @return The value.
	 */
	public static boolean isOnUpperBoundarySecondDirection(final int i1, final int n1) {
		return i1 == n1 - 1;
	}

	/**
	 * Returns whether the condition holds.
	 *
	 * @param i2 The value.
	 * @return The value.
	 */
	public static boolean isOnLowerBoundaryThirdDirection(final int i2) {
		return i2 == 0;
	}

	/**
	 * Returns whether the condition holds.
	 *
	 * @param i2 The value.
	 * @param n2 The value.
	 * @return The value.
	 */
	public static boolean isOnUpperBoundaryThirdDirection(final int i2, final int n2) {
		return i2 == n2 - 1;
	}

	/**
	 * Returns whether the condition holds.
	 *
	 * @param i0 The value.
	 * @param i1 The value.
	 * @param i2 The value.
	 * @param n0 The value.
	 * @param n1 The value.
	 * @param n2 The value.
	 * @return The value.
	 */
	public static boolean isOnAnyBoundary(
			final int i0,
			final int i1,
			final int i2,
			final int n0,
			final int n1,
			final int n2) {
		return i0 == 0 || i0 == n0 - 1
				|| i1 == 0 || i1 == n1 - 1
				|| i2 == 0 || i2 == n2 - 1;
	}

	/**
	 * Returns whether the condition holds.
	 *
	 * @param i0 The value.
	 * @param i1 The value.
	 * @param i2 The value.
	 * @param n0 The value.
	 * @param n1 The value.
	 * @param n2 The value.
	 * @return The value.
	 */
	public static boolean isInteriorPoint(
			final int i0,
			final int i1,
			final int i2,
			final int n0,
			final int n1,
			final int n2) {
		return i0 > 0 && i0 < n0 - 1
				&& i1 > 0 && i1 < n1 - 1
				&& i2 > 0 && i2 < n2 - 1;
	}

	/**
	 * Performs the operation.
	 *
	 * @param n0 The value.
	 * @param n1 The value.
	 * @param n2 The value.
	 */
	public static void validateGridShape(final int n0, final int n1, final int n2) {
		if (n0 < 2 || n1 < 2 || n2 < 2) {
			throw new IllegalArgumentException("All grid directions must have at least two nodes.");
		}
	}

	/**
	 * Performs the operation.
	 *
	 * @param i0 The value.
	 * @param i1 The value.
	 * @param i2 The value.
	 * @param n0 The value.
	 * @param n1 The value.
	 * @param n2 The value.
	 */
	public static void validateIndex(
			final int i0,
			final int i1,
			final int i2,
			final int n0,
			final int n1,
			final int n2) {

		if (i0 < 0 || i0 >= n0) {
			throw new IllegalArgumentException("Index i0 out of range.");
		}
		if (i1 < 0 || i1 >= n1) {
			throw new IllegalArgumentException("Index i1 out of range.");
		}
		if (i2 < 0 || i2 >= n2) {
			throw new IllegalArgumentException("Index i2 out of range.");
		}
	}
}
