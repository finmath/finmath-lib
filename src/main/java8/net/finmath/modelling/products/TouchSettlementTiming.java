package net.finmath.modelling.products;

/**
 * Settlement timing for touch-style barrier products.
 *
 * <ul>
 * <li>{@link #AT_EXPIRY}: payoff is determined by hit/no-hit over the option
 * life
 *       and paid at maturity,</li>
 * <li>{@link #AT_HIT}: payoff is made immediately when the barrier is first
 * hit.</li>
 * </ul>
 */
public enum TouchSettlementTiming {
	/**
	 * The at expiry.
	 */
	AT_EXPIRY,
	/**
	 * The at hit.
	 */
	AT_HIT
}
