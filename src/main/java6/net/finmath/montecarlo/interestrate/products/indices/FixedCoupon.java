/*
 * Created on 06.12.2009
 *
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 */
package net.finmath.montecarlo.interestrate.products.indices;

import java.util.Set;

import net.finmath.montecarlo.RandomVariable;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * A fixed coupon index paying constant coupon..
 * 
 * @author Christian Fries
 */
public class FixedCoupon extends AbstractIndex {

	private static final long serialVersionUID = 5375406324063846793L;
	private final RandomVariableInterface coupon;
	
	/**
	 * Creates a fixed coupon index paying constant coupon.
	 * 
	 * @param coupon The coupon.
	 */
	public FixedCoupon(double coupon) {
		super();
		this.coupon = new RandomVariable(coupon);
	}

	@Override
	public RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) {
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
