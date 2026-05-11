package net.finmath.finitedifference.interestrate.products;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import net.finmath.finitedifference.grids.SpaceTimeDiscretization;
import net.finmath.finitedifference.interestrate.models.FiniteDifferenceInterestRateModel;
import net.finmath.finitedifference.solvers.FDMThetaMethod1D;
import net.finmath.modelling.EuropeanExercise;
import net.finmath.time.Period;
import net.finmath.time.Schedule;

/**
 * Finite-difference valuation of a deterministic-cashflow bond.
 *
 * <p>
 * This class is the reduced-scope PDE analogue of the analytic bond products in
 * finmath. It is intentionally restricted to deterministic cashflows:
 * </p>
 * <ul>
 *   <li>zero-coupon bonds,</li>
 *   <li>fixed-coupon bonds,</li>
 *   <li>deterministic redemption at maturity.</li>
 * </ul>
 *
 * <p>
 * The class does <b>not</b> yet support floating coupons, credit-risk survival
 * curves, basis-factor curves, or recovery payments.
 * </p>
 *
 * <p>
 * Let {@code T_i} denote the payment dates of the schedule, {@code \delta_i}
 * the corresponding accrual factors, {@code c} the fixed coupon rate,
 * {@code N} the notional, and {@code R} the redemption paid on the final
 * payment date. The deterministic cashflow paid at {@code T_i} is
 * </p>
 *
 * <p>
 * <i>
 * C_i = N c \delta_i
 * </i>
 * </p>
 *
 * <p>
 * for intermediate periods, and
 * </p>
 *
 * <p>
 * <i>
 * C_n = N c \delta_n + R
 * </i>
 * </p>
 *
 * <p>
 * on the final payment date.
 * </p>
 *
 * <p>
 * In the event-based finite-difference framework this is represented through
 * jumps of the value function at payment dates:
 * </p>
 *
 * <p>
 * <i>
 * V(T_i^{-},x) = V(T_i^{+},x) + C_i.
 * </i>
 * </p>
 *
 * <p>
 * The current implementation uses the one-dimensional theta-method solver and
 * therefore requires a one-dimensional finite-difference interest-rate model.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class Bond implements FiniteDifferenceInterestRateProduct {

	/**
	 * The event time tolerance.
	 */
	private static final double EVENT_TIME_TOLERANCE = 1E-12;

	/**
	 * The schedule.
	 */
	private final Schedule schedule;
	/**
	 * The fixed coupon.
	 */
	private final double fixedCoupon;
	/**
	 * The notional.
	 */
	private final double notional;
	/**
	 * The redemption.
	 */
	private final double redemption;
	/**
	 * The maturity.
	 */
	private final double maturity;

	/**
	 * Creates a fixed-coupon bond with redemption equal to the notional.
	 *
	 * @param schedule The payment schedule.
	 * @param fixedCoupon The fixed coupon rate.
	 * @param notional The notional.
	 */
	public Bond(
			final Schedule schedule,
			final double fixedCoupon,
			final double notional) {
		this(schedule, fixedCoupon, notional, notional);
	}

	/**
	 * Creates a deterministic-cashflow bond.
	 *
	 * @param schedule The payment schedule.
	 * @param fixedCoupon The fixed coupon rate.
	 * @param notional The notional.
	 * @param redemption The redemption paid on the final payment date.
	 */
	public Bond(
			final Schedule schedule,
			final double fixedCoupon,
			final double notional,
			final double redemption) {

		if (schedule == null) {
			throw new IllegalArgumentException("schedule must not be null.");
		}
		if (schedule.getNumberOfPeriods() <= 0) {
			throw new IllegalArgumentException("schedule must contain at least one period.");
		}
		if (fixedCoupon < 0.0) {
			throw new IllegalArgumentException("fixedCoupon must be non-negative.");
		}
		if (notional < 0.0) {
			throw new IllegalArgumentException("notional must be non-negative.");
		}
		if (redemption < 0.0) {
			throw new IllegalArgumentException("redemption must be non-negative.");
		}

		this.schedule = schedule;
		this.fixedCoupon = fixedCoupon;
		this.notional = notional;
		this.redemption = redemption;
		this.maturity = schedule.getPayment(schedule.getNumberOfPeriods() - 1);

		validateSchedule();
	}

	/**
	 * Creates a zero-coupon bond with unit notional.
	 *
	 * @param maturity The maturity.
	 * @return A zero-coupon bond.
	 */
	public static Bond ofZeroCouponBond(final double maturity) {
		return ofZeroCouponBond(maturity, 1.0);
	}

	/**
	 * Creates a zero-coupon bond.
	 *
	 * @param maturity The maturity.
	 * @param notional The notional paid at maturity.
	 * @return A zero-coupon bond.
	 */
	public static Bond ofZeroCouponBond(final double maturity, final double notional) {
		final Schedule schedule = new SinglePaymentSchedule(maturity);
		return new Bond(schedule, 0.0, notional, notional);
	}

	@Override
	public double[] getValue(
			final double evaluationTime,
			final FiniteDifferenceInterestRateModel model) {

		validateModel(model);

		final FDMThetaMethod1D solver = new FDMThetaMethod1D(
				model,
				this,
				model.getSpaceTimeDiscretization(),
				new EuropeanExercise(maturity)
		);

		return solver.getValue(
				evaluationTime,
				maturity,
				buildZeroTerminalValues(model)
		);
	}

	@Override
	public double[][] getValues(final FiniteDifferenceInterestRateModel model) {

		validateModel(model);

		final FDMThetaMethod1D solver = new FDMThetaMethod1D(
				model,
				this,
				model.getSpaceTimeDiscretization(),
				new EuropeanExercise(maturity)
		);

		return solver.getValues(
				maturity,
				buildZeroTerminalValues(model)
		);
	}

	@Override
	public double[] getEventTimes() {
		final double[] eventTimes = new double[schedule.getNumberOfPeriods()];

		for (int periodIndex = 0; periodIndex < schedule.getNumberOfPeriods(); periodIndex++) {
			eventTimes[periodIndex] = schedule.getPayment(periodIndex);
		}

		return eventTimes;
	}

	@Override
	public double[] applyEventCondition(
			final double time,
			final double[] valuesAfterEvent,
			final FiniteDifferenceInterestRateModel model) {

		if (valuesAfterEvent == null) {
			throw new IllegalArgumentException("valuesAfterEvent must not be null.");
		}

		final double payment = getCashflowAt(time);
		if (payment == 0.0) {
			return valuesAfterEvent;
		}

		final double[] valuesBeforeEvent = valuesAfterEvent.clone();
		for (int i = 0; i < valuesBeforeEvent.length; i++) {
			valuesBeforeEvent[i] += payment;
		}

		return valuesBeforeEvent;
	}

	/**
	 * Returns the payment schedule.
	 *
	 * @return The payment schedule.
	 */
	public Schedule getSchedule() {
		return schedule;
	}

	/**
	 * Returns the fixed coupon rate.
	 *
	 * @return The fixed coupon rate.
	 */
	public double getFixedCoupon() {
		return fixedCoupon;
	}

	/**
	 * Returns the notional.
	 *
	 * @return The notional.
	 */
	public double getNotional() {
		return notional;
	}

	/**
	 * Returns the redemption amount paid on the final payment date.
	 *
	 * @return The redemption amount.
	 */
	public double getRedemption() {
		return redemption;
	}

	/**
	 * Returns the maturity, equal to the last payment date of the schedule.
	 *
	 * @return The maturity.
	 */
	public double getMaturity() {
		return maturity;
	}

	/**
	 * Returns the deterministic cashflow paid at the given period index.
	 *
	 * @param periodIndex The period index.
	 * @return The cashflow paid at the corresponding payment date.
	 */
	public double getCashflow(final int periodIndex) {
		validatePeriodIndex(periodIndex);

		double cashflow = notional * fixedCoupon * schedule.getPeriodLength(periodIndex);

		if (periodIndex == schedule.getNumberOfPeriods() - 1) {
			cashflow += redemption;
		}

		return cashflow;
	}

	@Override
	public String toString() {
		return "Bond [schedule=" + schedule
				+ ", fixedCoupon=" + fixedCoupon
				+ ", notional=" + notional
				+ ", redemption=" + redemption
				+ ", eventTimes=" + Arrays.toString(getEventTimes())
				+ "]";
	}

	private void validateSchedule() {
		double previousPayment = -1.0;

		for (int periodIndex = 0; periodIndex < schedule.getNumberOfPeriods(); periodIndex++) {
			final double payment = schedule.getPayment(periodIndex);
			final double periodLength = schedule.getPeriodLength(periodIndex);

			if (payment < 0.0) {
				throw new IllegalArgumentException("Schedule payment times must be non-negative.");
			}
			if (periodLength < 0.0) {
				throw new IllegalArgumentException("Schedule period lengths must be non-negative.");
			}
			if (periodIndex > 0 && payment <= previousPayment) {
				throw new IllegalArgumentException("Schedule payment times must be strictly increasing.");
			}

			previousPayment = payment;
		}
	}

	private void validateModel(final FiniteDifferenceInterestRateModel model) {
		if (model == null) {
			throw new IllegalArgumentException("model must not be null.");
		}
		if (model.getSpaceTimeDiscretization().getNumberOfSpaceGrids() != 1) {
			throw new IllegalArgumentException(
					"Bond currently supports only one-dimensional finite-difference interest-rate models."
			);
		}
	}

	private void validatePeriodIndex(final int periodIndex) {
		if (periodIndex < 0 || periodIndex >= schedule.getNumberOfPeriods()) {
			throw new IllegalArgumentException("periodIndex out of bounds.");
		}
	}

	private double getCashflowAt(final double time) {
		double totalCashflow = 0.0;

		for (int periodIndex = 0; periodIndex < schedule.getNumberOfPeriods(); periodIndex++) {
			if (Math.abs(time - schedule.getPayment(periodIndex)) <= EVENT_TIME_TOLERANCE) {
				totalCashflow += getCashflow(periodIndex);
			}
		}

		return totalCashflow;
	}

	private double[] buildZeroTerminalValues(final FiniteDifferenceInterestRateModel model) {
		final SpaceTimeDiscretization discretization = model.getSpaceTimeDiscretization();
		final double[] xGrid = discretization.getSpaceGrid(0).getGrid();
		return new double[xGrid.length];
	}

	/**
	 * Minimal schedule used for the zero-coupon-bond factory.
	 *
	 * <p>
	 * It represents one single period ending at the payment date.
	 * </p>
	 *
	 * @author Alessandro Gnoatto
	 */
	private static final class SinglePaymentSchedule implements Schedule {

		/**
		 * The maturity.
		 */
		private final double maturity;

		private SinglePaymentSchedule(final double maturity) {
			if (maturity < 0.0) {
				throw new IllegalArgumentException("maturity must be non-negative.");
			}
			this.maturity = maturity;
		}

		@Override
		public int getNumberOfPeriods() {
			return 1;
		}

		@Override
		public double getFixing(final int periodIndex) {
			validatePeriodIndex(periodIndex);
			return maturity;
		}

		@Override
		public double getPayment(final int periodIndex) {
			validatePeriodIndex(periodIndex);
			return maturity;
		}

		@Override
		public double getPeriodStart(final int periodIndex) {
			validatePeriodIndex(periodIndex);
			return 0.0;
		}

		@Override
		public double getPeriodEnd(final int periodIndex) {
			validatePeriodIndex(periodIndex);
			return maturity;
		}

		@Override
		public double getPeriodLength(final int periodIndex) {
			validatePeriodIndex(periodIndex);
			return 0.0;
		}

		@Override
		public int getPeriodIndex(final double time) {
			return Math.abs(time - maturity) <= EVENT_TIME_TOLERANCE ? 0 : -1;
		}

		@Override
		public int getPeriodIndex(final java.time.LocalDate date) {
			throw new UnsupportedOperationException("SinglePaymentSchedule does not support LocalDate lookup.");
		}

		@Override
		public net.finmath.time.Period getPeriod(final int periodIndex) {
			throw new UnsupportedOperationException("SinglePaymentSchedule does not provide Period objects.");
		}

		@Override
		public java.time.LocalDate getReferenceDate() {
			return null;
		}

		@Override
		public net.finmath.time.daycount.DayCountConvention getDaycountconvention() {
			return null;
		}

		@Override
		public List<Period> getPeriods() {
			throw new UnsupportedOperationException("SinglePaymentSchedule does not provide Period objects.");
		}

		@Override
		public Iterator<Period> iterator() {
			throw new UnsupportedOperationException("SinglePaymentSchedule does not provide Period iteration.");
		}

		private void validatePeriodIndex(final int periodIndex) {
			if (periodIndex != 0) {
				throw new IllegalArgumentException("SinglePaymentSchedule supports only period index 0.");
			}
		}
	}
}
