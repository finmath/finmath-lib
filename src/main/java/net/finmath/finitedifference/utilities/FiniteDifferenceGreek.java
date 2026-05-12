package net.finmath.finitedifference.utilities;

/**
 * Enumeration of standard finite-difference Greek surface types.
 *
 * @author Alessandro Gnoatto
 */
public enum FiniteDifferenceGreek {

	/**
	 * First derivative with respect to the first spatial dimension.
	 */
	DELTA,

	/**
	 * Second derivative with respect to the first spatial dimension.
	 */
	GAMMA,

	/**
	 * Derivative with respect to calendar evaluation time.
	 */
	THETA
}
