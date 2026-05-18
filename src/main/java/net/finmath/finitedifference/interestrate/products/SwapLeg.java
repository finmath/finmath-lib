package net.finmath.finitedifference.interestrate.products;

import java.util.Arrays;

import net.finmath.finitedifference.interestrate.models.FiniteDifferenceInterestRateModel;
import net.finmath.time.Schedule;

/**
 * Finite-difference valuation of a reduced-scope swap leg.
 *
 * <p>
 * This class mirrors the role of the analytic and Monte Carlo swap-leg classes,
 * but is adapted to the current Markovian finite-difference rates framework.
 * </p>
 *
 * <p>
 * A leg is specified by
 * </p>
 * <ul>
 *   <li>a schedule,</li>
 *   <li>an optional forwarding-curve name,</li>
 *   <li>a notional for each period,</li>
 *   <li>a spread for each period,</li>
 *   <li>an optional flag for notional exchange.</li>
 * </ul>
 *
 * <p>
 * If {@code forwardCurveName} is {@code null} or empty, the leg is interpreted
 * as a fixed leg and the spread is the fixed coupon rate.
 * Otherwise the coupon in period {@code i} is
 * </p>
 *
 * <p>
 * <i>
 * \bigl(F(t;T_i^{\mathrm{fix}},T_i^{\mathrm{pay}}) + s_i\bigr)\delta_i.
 * </i>
 * </p>
 *
 * <p>
 * Reduced-scope Markovian assumption:
 * </p>
 * <ul>
 *   <li>fixed-leg periods with {@code periodStart < evaluationTime} are ignored,</li>
 *   <li>floating-leg periods with {@code fixingDate < evaluationTime} are ignored.</li>
 * </ul>
 *
 * <p>
 * This means the product is interpreted as the remaining forward-looking leg
 * from the current evaluation time onward. The class is therefore intended for
 * forward/running swap valuation in the PDE setting and for swaption
 * underlyings. It does not attempt to encode already-fixed-but-not-yet-paid
 * floating coupons as additional state variables.
 * </p>
 *
 * <p>
 * Since the product is linear, valuation is performed directly from discount
 * bonds and forward rates. No PDE solver is required.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class SwapLeg implements FiniteDifferenceInterestRateProduct {

	/**
	 * The time tolerance.
	 */
	private static final double TIME_TOLERANCE = 1E-12;

	/**
	 * The leg schedule.
	 */
	private final Schedule legSchedule;
	/**
	 * The forward curve name.
	 */
	private final String forwardCurveName;
	/**
	 * The notionals.
	 */
	private final double[] notionals;
	/**
	 * The spreads.
	 */
	private final double[] spreads;
	/**
	 * The is notional exchanged.
	 */
	private final boolean isNotionalExchanged;

	/**
	 * Creates a swap leg.
	 *
	 * @param legSchedule The leg schedule.
	 * @param forwardCurveName The forwarding-curve name. If {@code null} or empty,
	 *        the leg is interpreted as fixed.
	 * @param notionals The notionals for all periods.
	 * @param spreads The spreads or fixed coupon rates for all periods.
	 * @param isNotionalExchanged If true, notional exchange is included.
	 */
	public SwapLeg(
			final Schedule legSchedule,
			final String forwardCurveName,
			final double[] notionals,
			final double[] spreads,
			final boolean isNotionalExchanged) {

		if (legSchedule == null) {
			throw new IllegalArgumentException("legSchedule must not be null.");
		}
		if (legSchedule.getNumberOfPeriods() <= 0) {
			throw new IllegalArgumentException("legSchedule must contain at least one period.");
		}
		if (notionals == null || notionals.length != legSchedule.getNumberOfPeriods()) {
			throw new IllegalArgumentException(
					"notionals must have the same length as the number of schedule periods."
					);
		}
		if (spreads == null || spreads.length != legSchedule.getNumberOfPeriods()) {
			throw new IllegalArgumentException(
					"spreads must have the same length as the number of schedule periods."
					);
		}

		this.legSchedule = legSchedule;
		this.forwardCurveName = forwardCurveName;
		this.notionals = notionals.clone();
		this.spreads = spreads.clone();
		this.isNotionalExchanged = isNotionalExchanged;

		validateInputs();
	}

	/**
	 * Creates a swap leg with constant notional equal to one and constant spread.
	 *
	 * @param legSchedule The leg schedule.
	 * @param forwardCurveName The forwarding-curve name. If {@code null} or empty,
	 *        the leg is interpreted as fixed.
	 * @param spread The constant spread or fixed coupon rate.
	 * @param isNotionalExchanged If true, notional exchange is included.
	 */
	public SwapLeg(
			final Schedule legSchedule,
			final String forwardCurveName,
			final double spread,
			final boolean isNotionalExchanged) {
		this(
				legSchedule,
				forwardCurveName,
				createConstantArray(legSchedule, 1.0),
				createConstantArray(legSchedule, spread),
				isNotionalExchanged
				);
	}

	/**
	 * Creates a swap leg with constant notional equal to one, constant spread,
	 * and no notional exchange.
	 *
	 * @param legSchedule The leg schedule.
	 * @param forwardCurveName The forwarding-curve name. If {@code null} or empty,
	 *        the leg is interpreted as fixed.
	 * @param spread The constant spread or fixed coupon rate.
	 */
	public SwapLeg(
			final Schedule legSchedule,
			final String forwardCurveName,
			final double spread) {
		this(legSchedule, forwardCurveName, spread, false);
	}

	@Override
	public double[] getValue(
			final double evaluationTime,
			final FiniteDifferenceInterestRateModel model) {

		validateModel(model);

		final double[] xGrid = model.getSpaceTimeDiscretization().getSpaceGrid(0).getGrid();
		final double[] values = new double[xGrid.length];

		for (int i = 0; i < xGrid.length; i++) {
			values[i] = getValueAt(evaluationTime, xGrid[i], model);
		}

		return values;
	}

	@Override
	public double[][] getValues(final FiniteDifferenceInterestRateModel model) {

		validateModel(model);

		final double[] xGrid = model.getSpaceTimeDiscretization().getSpaceGrid(0).getGrid();
		final int numberOfSpacePoints = xGrid.length;
		final int numberOfTimes = model.getSpaceTimeDiscretization().getTimeDiscretization().getNumberOfTimes();

		final double[][] values = new double[numberOfSpacePoints][numberOfTimes];

		for (int timeIndex = 0; timeIndex < numberOfTimes; timeIndex++) {
			final double evaluationTime = model.getSpaceTimeDiscretization().getTimeDiscretization().getTime(timeIndex);

			for (int spaceIndex = 0; spaceIndex < numberOfSpacePoints; spaceIndex++) {
				values[spaceIndex][timeIndex] = getValueAt(evaluationTime, xGrid[spaceIndex], model);
			}
		}

		return values;
	}

	/**
	 * Returns the leg value at the given evaluation time and state.
	 *
	 * @param evaluationTime The evaluation time.
	 * @param stateVariable The state variable.
	 * @param model The finite-difference interest-rate model.
	 * @return The leg value.
	 */
	public double getValueAt(
			final double evaluationTime,
			final double stateVariable,
			final FiniteDifferenceInterestRateModel model) {

		double value = 0.0;

		for (int periodIndex = 0; periodIndex < legSchedule.getNumberOfPeriods(); periodIndex++) {

			final double periodStart = legSchedule.getPeriodStart(periodIndex);
			final double fixingDate = legSchedule.getFixing(periodIndex);
			final double paymentDate = legSchedule.getPayment(periodIndex);
			final double periodLength = legSchedule.getPeriodLength(periodIndex);
			final double notional = notionals[periodIndex];

			if (isFixedLeg()) {
				if (periodStart < evaluationTime - TIME_TOLERANCE) {
					continue;
				}
			} else {
				if (fixingDate < evaluationTime - TIME_TOLERANCE) {
					continue;
				}
			}

			if (paymentDate <= evaluationTime + TIME_TOLERANCE) {
				continue;
			}

			double couponRate = spreads[periodIndex];
			if (!isFixedLeg()) {
				couponRate += model.getForwardRate(
						forwardCurveName,
						evaluationTime,
						fixingDate,
						paymentDate,
						stateVariable
						);
			}

			value += notional
					* couponRate
					* periodLength
					* model.getDiscountBond(evaluationTime, paymentDate, stateVariable);

			if (isNotionalExchanged) {
				if (periodStart > evaluationTime + TIME_TOLERANCE) {
					value -= notional * model.getDiscountBond(evaluationTime, periodStart, stateVariable);
				}
				if (legSchedule.getPeriodEnd(periodIndex) > evaluationTime + TIME_TOLERANCE) {
					value += notional * model.getDiscountBond(
							evaluationTime,
							legSchedule.getPeriodEnd(periodIndex),
							stateVariable
							);
				}
			}
		}

		return value;
	}

	/**
	 * Returns the schedule.
	 *
	 * @return The schedule.
	 */
	public Schedule getSchedule() {
		return legSchedule;
	}

	/**
	 * Returns the forwarding-curve name.
	 *
	 * @return The forwarding-curve name, possibly {@code null}.
	 */
	public String getForwardCurveName() {
		return forwardCurveName;
	}

	/**
	 * Returns the notionals.
	 *
	 * @return The notionals.
	 */
	public double[] getNotionals() {
		return notionals.clone();
	}

	/**
	 * Returns the spreads or fixed coupon rates.
	 *
	 * @return The spreads.
	 */
	public double[] getSpreads() {
		return spreads.clone();
	}

	/**
	 * Returns whether notional exchange is included.
	 *
	 * @return True if notional exchange is included.
	 */
	public boolean isNotionalExchanged() {
		return isNotionalExchanged;
	}

	/**
	 * Returns whether the leg is fixed.
	 *
	 * @return True if the leg is fixed.
	 */
	public boolean isFixedLeg() {
		return forwardCurveName == null || forwardCurveName.isEmpty();
	}

	@Override
	public String toString() {
		return "SwapLeg [legSchedule=" + legSchedule
				+ ", forwardCurveName=" + forwardCurveName
				+ ", notionals=" + Arrays.toString(notionals)
				+ ", spreads=" + Arrays.toString(spreads)
				+ ", isNotionalExchanged=" + isNotionalExchanged
				+ "]";
	}

	private void validateInputs() {
		for (int periodIndex = 0; periodIndex < legSchedule.getNumberOfPeriods(); periodIndex++) {
			if (notionals[periodIndex] < 0.0) {
				throw new IllegalArgumentException("All notionals must be non-negative.");
			}
			if (legSchedule.getFixing(periodIndex) < 0.0) {
				throw new IllegalArgumentException("Schedule fixing times must be non-negative.");
			}
			if (legSchedule.getPayment(periodIndex) <= legSchedule.getFixing(periodIndex)) {
				throw new IllegalArgumentException("Each payment time must be strictly later than fixing.");
			}
			if (legSchedule.getPeriodLength(periodIndex) < 0.0) {
				throw new IllegalArgumentException("Schedule period lengths must be non-negative.");
			}
		}
	}

	private void validateModel(final FiniteDifferenceInterestRateModel model) {
		if (model == null) {
			throw new IllegalArgumentException("model must not be null.");
		}
		if (model.getSpaceTimeDiscretization().getNumberOfSpaceGrids() != 1) {
			throw new IllegalArgumentException(
					"SwapLeg currently supports only one-dimensional finite-difference interest-rate models."
					);
		}
	}

	private static double[] createConstantArray(final Schedule schedule, final double value) {
		if (schedule == null) {
			throw new IllegalArgumentException("schedule must not be null.");
		}

		final double[] array = new double[schedule.getNumberOfPeriods()];
		Arrays.fill(array, value);
		return array;
	}
}
