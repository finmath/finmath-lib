package net.finmath.finitedifference.interestrate.products;

import net.finmath.finitedifference.FiniteDifferenceProduct;
import net.finmath.finitedifference.interestrate.models.FiniteDifferenceInterestRateModel;

/**
 * Interface for products valued by a finite-difference interest-rate model.
 *
 * <p>
 * This interface specializes the generic
 * {@link net.finmath.finitedifference.FiniteDifferenceProduct} to the case of
 * interest-rate finite-difference models.
 * </p>
 *
 * <p>
 * Interest-rate products typically involve event times such as fixing dates,
 * coupon dates, payment dates, exercise dates, call dates, or redemption dates.
 * These are exposed via {@link #getEventTimes()}.
 * </p>
 *
 * <p>
 * At an event time {@code t}, the backward induction first produces the
 * continuation value
 * </p>
 *
 * <p>
 * <i>
 * V(t^{+},x),
 * </i>
 * </p>
 *
 * <p>
 * and the product then applies its event rule to obtain
 * </p>
 *
 * <p>
 * <i>
 * V(t^{-},x) = \mathcal{J}_{t}(V(t^{+},x),x).
 * </i>
 * </p>
 *
 * <p>
 * Typical examples are:
 * </p>
 * <ul>
 *   <li>adding a coupon or redemption amount,</li>
 *   <li>applying early exercise,</li>
 *   <li>applying callability,</li>
 *   <li>processing a fixing-dependent cashflow.</li>
 * </ul>
 *
 * <p>
 * Products without intermediate events may return an empty array from
 * {@link #getEventTimes()} and use the default implementation of
 * {@link #applyEventCondition(double, double[],
 * FiniteDifferenceInterestRateModel)}.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public interface FiniteDifferenceInterestRateProduct
extends FiniteDifferenceProduct<FiniteDifferenceInterestRateModel> {

	@Override
	default Class<FiniteDifferenceInterestRateModel> getModelClass() {
		return FiniteDifferenceInterestRateModel.class;
	}

	/**
	 * Returns the event times of the product.
	 *
	 * <p>
	 * Event times are the dates where the backward induction may have to apply
	 * a
	 * jump or another event condition, for example because of coupon accrual,
	 * coupon payment, fixing, exercise, callability, or redemption.
	 * </p>
	 *
	 * <p>
	 * Products without intermediate events may return an empty array.
	 * </p>
	 *
	 * @return The event times of the product.
	 */
	default double[] getEventTimes() {
		return new double[0];
	}

	/**
	 * Applies the event condition at a given event time.
	 *
	 * <p>
	 * The input array {@code valuesAfterEvent} represents the continuation
	 * values
	 * immediately after the event time, that is
	 * </p>
	 *
	 * <p>
	 * <i>
	 * V(t^{+},x).
	 * </i>
	 * </p>
	 *
	 * <p>
	 * The returned array represents the values immediately before the event
	 * time,
	 * that is
	 * </p>
	 *
	 * <p>
	 * <i>
	 * V(t^{-},x).
	 * </i>
	 * </p>
	 *
	 * <p>
	 * The ordering of the entries must match the state-space ordering of the
	 * underlying finite-difference model.
	 * </p>
	 *
	 * <p>
	 * The default implementation leaves the continuation values unchanged.
	 * </p>
	 *
	 * @param time The event time.
	 * @param valuesAfterEvent The continuation values immediately after the
	 *     event.
	 * @param model The finite-difference interest-rate model.
	 * @return The values immediately before the event.
	 */
	default double[] applyEventCondition(
			final double time,
			final double[] valuesAfterEvent,
			final FiniteDifferenceInterestRateModel model) {
		return valuesAfterEvent;
	}
}
