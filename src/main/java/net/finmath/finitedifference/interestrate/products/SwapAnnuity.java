package net.finmath.finitedifference.interestrate.products;

import net.finmath.finitedifference.interestrate.models.FiniteDifferenceInterestRateModel;
import net.finmath.time.Schedule;

/**
 * Utility methods for swap annuities in the finite-difference interest-rate
 * framework.
 *
 * <p>
 * The reduced-scope swap annuity at time {@code t} is defined as
 * </p>
 *
 * <p>
 * <i>
 * A(t,x) = \sum_{i} \delta_i P(t,T_i;x),
 * </i>
 * </p>
 *
 * <p>
 * where the sum runs over the fixed-leg periods which have not yet started,
 * i.e. periods with {@code periodStart >= t}. This is consistent with the
 * remaining forward-looking interpretation used by {@link SwapLeg} and
 * {@link Swap}.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public final class SwapAnnuity {

	/**
	 * The time tolerance.
	 */
	private static final double TIME_TOLERANCE = 1E-12;

	private SwapAnnuity() {
	}

	/**
	 * Returns the reduced-scope swap annuity at the given evaluation time and
	 * state.
	 *
	 * @param evaluationTime The evaluation time.
	 * @param schedule The fixed-leg schedule.
	 * @param model The finite-difference interest-rate model.
	 * @param stateVariables The current state variables.
	 * @return The swap annuity.
	 */
	public static double getSwapAnnuity(
			final double evaluationTime,
			final Schedule schedule,
			final FiniteDifferenceInterestRateModel model,
			final double... stateVariables) {

		if (schedule == null) {
			throw new IllegalArgumentException("schedule must not be null.");
		}
		if (model == null) {
			throw new IllegalArgumentException("model must not be null.");
		}
		if (stateVariables == null || stateVariables.length != 1) {
			throw new IllegalArgumentException("Exactly one state variable is required.");
		}

		double value = 0.0;

		for (int periodIndex = 0; periodIndex < schedule.getNumberOfPeriods(); periodIndex++) {
			final double periodStart = schedule.getPeriodStart(periodIndex);
			final double paymentDate = schedule.getPayment(periodIndex);

			if (periodStart < evaluationTime - TIME_TOLERANCE) {
				continue;
			}
			if (paymentDate <= evaluationTime + TIME_TOLERANCE) {
				continue;
			}

			value += schedule.getPeriodLength(periodIndex)
				 * model.getDiscountBond(
							evaluationTime,
							paymentDate,
							stateVariables[0]
					);
		}

		return value;
	}
}
