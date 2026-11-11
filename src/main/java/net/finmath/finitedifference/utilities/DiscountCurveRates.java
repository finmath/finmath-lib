package net.finmath.finitedifference.utilities;

import net.finmath.marketdata.model.curves.DiscountCurve;

/** Local continuously compounded rates for finite-difference pricing operators. */
public final class DiscountCurveRates {

	private static final double DIFFERENTIATION_STEP = 1E-4;

	private DiscountCurveRates() {
	}

	/**
	 * Approximates {@code -d log(D(t))/dt}, not the zero rate from zero to t.
	 * Uses a centered second-order difference, or a second-order forward
	 * difference near zero, without querying the curve at negative times.
	 * The step is in year fractions and scales with time. At a curve kink the
	 * centered difference averages the adjacent forward rates; time grids should
	 * resolve such kinks. Discount factors must be finite and strictly positive.
	 *
	 * @param curve Discount curve.
	 * @param time Non-negative calendar time, in year fractions.
	 * @return Instantaneous continuously compounded forward rate.
	 */
	public static double getInstantaneousForwardRate(final DiscountCurve curve, final double time) {
		if(!Double.isFinite(time) || time < 0.0) {
			throw new IllegalArgumentException("Time must be finite and non-negative.");
		}
		final double h = DIFFERENTIATION_STEP * Math.max(1.0, time);
		if(time >= h) {
			return -logDiscountFactorRatio(curve, time - h, time + h) / (2.0 * h);
		}
		// -L'(t) = (-4*(L(t+h)-L(t)) + (L(t+2h)-L(t))) / (2h).
		return (-4.0 * logDiscountFactorRatio(curve, time, time + h)
				+ logDiscountFactorRatio(curve, time, time + 2.0 * h)) / (2.0 * h);
	}

	private static double logDiscountFactorRatio(final DiscountCurve curve, final double start, final double end) {
		final double startDiscountFactor = curve.getDiscountFactor(start);
		final double endDiscountFactor = curve.getDiscountFactor(end);
		if(!(startDiscountFactor > 0.0) || !Double.isFinite(startDiscountFactor)
				|| !(endDiscountFactor > 0.0) || !Double.isFinite(endDiscountFactor)) {
			throw new IllegalArgumentException("Discount factors must be finite and strictly positive.");
		}
		// Avoid rounding D(end)/D(start) to one for nearby discount factors.
		final double relativeChange = (endDiscountFactor - startDiscountFactor) / startDiscountFactor;
		return Math.abs(relativeChange) < 0.5 ? Math.log1p(relativeChange)
				: Math.log(endDiscountFactor) - Math.log(startDiscountFactor);
	}
}
