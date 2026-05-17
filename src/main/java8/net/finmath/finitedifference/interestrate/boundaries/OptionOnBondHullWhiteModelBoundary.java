package net.finmath.finitedifference.interestrate.boundaries;

import org.apache.commons.math3.distribution.NormalDistribution;

import net.finmath.finitedifference.boundaries.BoundaryCondition;
import net.finmath.finitedifference.boundaries.StandardBoundaryCondition;
import net.finmath.finitedifference.interestrate.models.FDMHullWhiteModel;
import net.finmath.finitedifference.interestrate.products.Bond;
import net.finmath.finitedifference.interestrate.products.FiniteDifferenceInterestRateProduct;
import net.finmath.finitedifference.interestrate.products.OptionOnBond;
import net.finmath.modelling.products.CallOrPut;

/**
 * Exact boundary conditions for {@link OptionOnBond} under {@link
 * FDMHullWhiteModel}.
 *
 * <p>
 * For a one-factor Hull-White model, a European option on a deterministic-
 * cashflow
 * bond admits an exact valuation by Jamshidian decomposition. Let {@code T} be
 * the
 * exercise date and let the remaining cashflows of the underlying bond at and
 * after
 * {@code T} be
 * </p>
 *
 * <p>
 * <i>
 * \sum_{i} C_i P(T,T_i;x).
 * </i>
 * </p>
 *
 * <p>
 * Since the bond value is strictly decreasing in the one-factor state variable
 * {@code x}, there exists a unique state {@code x*} solving
 * </p>
 *
 * <p>
 * <i>
 * \sum_i C_i P(T,T_i;x^*) = K,
 * </i>
 * </p>
 *
 * <p>
 * where {@code K} is the strike of the bond option.
 * The bond option value is then given by the sum of zero-coupon bond options
 * with strikes
 * </p>
 *
 * <p>
 * <i>
 * K_i = P(T,T_i;x^*).
 * </i>
 * </p>
 *
 * <p>
 * Hence both lower and upper finite-difference boundaries can be imposed by
 * exact
 * Dirichlet conditions.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class OptionOnBondHullWhiteModelBoundary implements FiniteDifferenceInterestRateBoundary {

	/**
	 * The time tolerance.
	 */
	private static final double TIME_TOLERANCE = 1E-12;
	/**
	 * The root tolerance.
	 */
	private static final double ROOT_TOLERANCE = 1E-12;
	/**
	 * The max bracketing steps.
	 */
	private static final int MAX_BRACKETING_STEPS = 100;
	/**
	 * The max bisection steps.
	 */
	private static final int MAX_BISECTION_STEPS = 200;

	/**
	 * The normal distribution.
	 */
	private static final NormalDistribution NORMAL_DISTRIBUTION = new NormalDistribution();

	/**
	 * The model.
	 */
	private final FDMHullWhiteModel model;

	/**
	 * Creates the exact Hull-White boundary for {@link OptionOnBond}.
	 *
	 * @param model The Hull-White model.
	 */
	public OptionOnBondHullWhiteModelBoundary(final FDMHullWhiteModel model) {
		if (model == null) {
			throw new IllegalArgumentException("model must not be null.");
		}
		if (model.getInitialValue() == null || model.getInitialValue().length != 1) {
			throw new IllegalArgumentException(
					"OptionOnBondHullWhiteModelBoundary requires a one-dimensional Hull-White model.");
		}

		this.model = model;
	}

	@Override
	public BoundaryCondition[] getBoundaryConditionsAtLowerBoundary(
			final FiniteDifferenceInterestRateProduct product,
			final double time,
			final double... stateVariables) {

		final OptionOnBond optionOnBond = validateAndCastProduct(product);
		validateStateVariables(stateVariables);

		return new BoundaryCondition[] {
				StandardBoundaryCondition.dirichlet(
						getExactOptionValue(optionOnBond, time, stateVariables[0])
				)
		};
	}

	@Override
	public BoundaryCondition[] getBoundaryConditionsAtUpperBoundary(
			final FiniteDifferenceInterestRateProduct product,
			final double time,
			final double... stateVariables) {

		final OptionOnBond optionOnBond = validateAndCastProduct(product);
		validateStateVariables(stateVariables);

		return new BoundaryCondition[] {
				StandardBoundaryCondition.dirichlet(
						getExactOptionValue(optionOnBond, time, stateVariables[0])
				)
		};
	}

	private OptionOnBond validateAndCastProduct(final FiniteDifferenceInterestRateProduct product) {
		if (!(product instanceof OptionOnBond)) {
			throw new IllegalArgumentException(
					"OptionOnBondHullWhiteModelBoundary requires an OptionOnBond product.");
		}

		return (OptionOnBond) product;
	}

	private void validateStateVariables(final double[] stateVariables) {
		if (stateVariables == null || stateVariables.length != 1) {
			throw new IllegalArgumentException("Exactly one state variable is required.");
		}
	}

	private double getExactOptionValue(
			final OptionOnBond optionOnBond,
			final double time,
			final double currentStateVariable) {

		final Bond underlyingBond = optionOnBond.getUnderlyingBond();
		final double exerciseDate = optionOnBond.getExerciseDate();
		final double strike = optionOnBond.getStrike();
		final CallOrPut callOrPut = optionOnBond.getCallOrPut();
		final int sign = callOrPut.toInteger();

		/*
		 * At or after exercise the value is the immediate payoff.
		 */
		if (time >= exerciseDate - TIME_TOLERANCE) {
			final double bondValue = getUnderlyingBondValueAtTime(
					underlyingBond,
					exerciseDate,
					currentStateVariable
			);
			return Math.max(sign * (bondValue - strike), 0.0);
		}

		/*
		 * Strike zero is a special case: the call equals the underlying bond,
		 * the put is worthless.
		 */
		if (Math.abs(strike) <= TIME_TOLERANCE) {
			if (callOrPut == CallOrPut.CALL) {
				return getUnderlyingBondOptionlessValue(
						underlyingBond,
						time,
						currentStateVariable
				);
			}
			return 0.0;
		}

		final double exerciseBoundaryState = findExerciseBoundaryState(underlyingBond, exerciseDate, strike);

		double value = 0.0;

		for (int periodIndex = 0; periodIndex < underlyingBond.getSchedule().getNumberOfPeriods(); periodIndex++) {
			final double paymentTime = underlyingBond.getSchedule().getPayment(periodIndex);

			if (paymentTime < exerciseDate - TIME_TOLERANCE) {
				continue;
			}

			final double cashflow = underlyingBond.getCashflow(periodIndex);
			final double zeroCouponStrike = model.getDiscountBond(
					exerciseDate,
					paymentTime,
					exerciseBoundaryState
			);

			value += cashflow * getZeroCouponBondOptionValue(
					time,
					currentStateVariable,
					exerciseDate,
					paymentTime,
					zeroCouponStrike,
					callOrPut
			);
		}

		return value;
	}

	private double getUnderlyingBondOptionlessValue(
			final Bond bond,
			final double time,
			final double stateVariable) {

		double value = 0.0;

		for (int periodIndex = 0; periodIndex < bond.getSchedule().getNumberOfPeriods(); periodIndex++) {
			final double paymentTime = bond.getSchedule().getPayment(periodIndex);

			if (paymentTime < time - TIME_TOLERANCE) {
				continue;
			}

			value += bond.getCashflow(periodIndex) * model.getDiscountBond(
					time,
					paymentTime,
					stateVariable
			);
		}

		return value;
	}

	private double getUnderlyingBondValueAtTime(
			final Bond bond,
			final double time,
			final double stateVariable) {

		double value = 0.0;

		for (int periodIndex = 0; periodIndex < bond.getSchedule().getNumberOfPeriods(); periodIndex++) {
			final double paymentTime = bond.getSchedule().getPayment(periodIndex);

			if (paymentTime < time - TIME_TOLERANCE) {
				continue;
			}

			value += bond.getCashflow(periodIndex) * model.getDiscountBond(
					time,
					paymentTime,
					stateVariable
			);
		}

		return value;
	}

	private double findExerciseBoundaryState(
			final Bond bond,
			final double exerciseDate,
			final double strike) {

		double lower = -1.0;
		double upper = 1.0;

		double functionValueAtLower = getBondValueMinusStrikeAtExercise(bond, exerciseDate, lower, strike);
		double functionValueAtUpper = getBondValueMinusStrikeAtExercise(bond, exerciseDate, upper, strike);

		int bracketingStep = 0;
		while (functionValueAtLower < 0.0 && bracketingStep < MAX_BRACKETING_STEPS) {
			lower *= 2.0;
			functionValueAtLower = getBondValueMinusStrikeAtExercise(bond, exerciseDate, lower, strike);
			bracketingStep++;
		}

		bracketingStep = 0;
		while (functionValueAtUpper > 0.0 && bracketingStep < MAX_BRACKETING_STEPS) {
			upper *= 2.0;
			functionValueAtUpper = getBondValueMinusStrikeAtExercise(bond, exerciseDate, upper, strike);
			bracketingStep++;
		}

		if (functionValueAtLower < 0.0 || functionValueAtUpper > 0.0) {
			throw new IllegalArgumentException(
					"Could not bracket the Jamshidian root for the bond option.");
		}

		double left = lower;
		double right = upper;

		for (int iteration = 0; iteration < MAX_BISECTION_STEPS; iteration++) {
			final double midpoint = 0.5 * (left + right);
			final double functionValueAtMidpoint =
					getBondValueMinusStrikeAtExercise(bond, exerciseDate, midpoint, strike);

			if (Math.abs(functionValueAtMidpoint) < ROOT_TOLERANCE
					|| Math.abs(right - left) < ROOT_TOLERANCE) {
				return midpoint;
			}

			if (functionValueAtMidpoint > 0.0) {
				left = midpoint;
			} else {
				right = midpoint;
			}
		}

		return 0.5 * (left + right);
	}

	private double getBondValueMinusStrikeAtExercise(
			final Bond bond,
			final double exerciseDate,
			final double stateVariable,
			final double strike) {
		return getUnderlyingBondValueAtTime(bond, exerciseDate, stateVariable) - strike;
	}

	private double getZeroCouponBondOptionValue(
			final double currentTime,
			final double currentStateVariable,
			final double exerciseDate,
			final double bondMaturity,
			final double strike,
			final CallOrPut callOrPut) {

		final int sign = callOrPut.toInteger();

		final double discountBondToExercise = model.getDiscountBond(
				currentTime,
				exerciseDate,
				currentStateVariable
		);

		/*
		 * If bond maturity equals exercise, the underlying zero-coupon bond
		 * pays 1
		 * at exercise and the payoff is deterministic.
		 */
		if (Math.abs(bondMaturity - exerciseDate) <= TIME_TOLERANCE) {
			return discountBondToExercise * Math.max(sign * (1.0 - strike), 0.0);
		}

		final double discountBondToMaturity = model.getDiscountBond(
				currentTime,
				bondMaturity,
				currentStateVariable
		);

		final double bondVolatilitySquared =
				model.getShortRateConditionalVariance(currentTime, exerciseDate)
			 * model.getB(exerciseDate, bondMaturity)
			 * model.getB(exerciseDate, bondMaturity);

		if (bondVolatilitySquared <= TIME_TOLERANCE) {
			final double forwardBondPrice = discountBondToMaturity / discountBondToExercise;
			return discountBondToExercise * Math.max(sign * (forwardBondPrice - strike), 0.0);
		}

		final double bondVolatility = Math.sqrt(bondVolatilitySquared);

		final double dPlus =
				(Math.log(discountBondToMaturity / (strike * discountBondToExercise))
						+ 0.5 * bondVolatilitySquared)
				/ bondVolatility;

		final double dMinus = dPlus - bondVolatility;

		if (callOrPut == CallOrPut.CALL) {
			return discountBondToMaturity * NORMAL_DISTRIBUTION.cumulativeProbability(dPlus)
					- strike * discountBondToExercise * NORMAL_DISTRIBUTION.cumulativeProbability(dMinus);
		} else {
			return strike * discountBondToExercise * NORMAL_DISTRIBUTION.cumulativeProbability(-dMinus)
					- discountBondToMaturity * NORMAL_DISTRIBUTION.cumulativeProbability(-dPlus);
		}
	}
}
