package net.finmath.finitedifference.solvers.adi;

/**
 * Identifies the PDE role of a barrier-oriented 2D ADI solver.
 *
 * <p>
 * OUT_STANDARD is the usual direct knock-out formulation.
 * IN_PRE_HIT is the pre-hit continuation PDE used in the direct
 * knock-in interface formulation.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public enum BarrierPDEMode {

	/**
	 * Standard direct pricing of a knock-out barrier PDE.
	 */
	OUT_STANDARD,

	/**
	 * Pre-hit continuation PDE for a direct knock-in formulation.
	 * The barrier is treated as an interface carrying the activated trace.
	 */
	IN_PRE_HIT
}
