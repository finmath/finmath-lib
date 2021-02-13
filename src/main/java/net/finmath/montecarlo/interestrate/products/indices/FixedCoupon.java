/*
 * Created on 06.12.2009
 *
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 */
package net.finmath.montecarlo.interestrate.products.indices;

import java.util.Set;

import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;

/**
 * A fixed coupon index paying constant coupon..
 *
 * @author Christian Fries
 * @version 1.0
 */
public class FixedCoupon extends AbstractIndex {

	private static final long serialVersionUID = 5375406324063846793L;

	private final RandomVariable coupon;

	/**
	 * Creates a fixed coupon index paying constant coupon.
	 *
	 * @param coupon The coupon.
	 */
	public FixedCoupon(final double coupon) {
		super();
		this.coupon = new RandomVariableFromDoubleArray(coupon);
	}

	@Override
	public RandomVariable getValue(final double evaluationTime, final TermStructureMonteCarloSimulationModel model) {
		return coupon;
	}

	/**
	 * Returns the coupon.
	 *
	 * @return the coupon
	 */
	public RandomVariable getCoupon() {
		return coupon;
	}

	@Override
	public Set<String> queryUnderlyings() {
		return null;
	}

	@Override
	public String toString() {
		return "FixedCoupon [coupon=" + coupon + ", toString()="
				+ super.toString() + "]";
	}
}
