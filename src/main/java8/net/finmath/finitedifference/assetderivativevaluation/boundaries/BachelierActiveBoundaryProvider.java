package net.finmath.finitedifference.assetderivativevaluation.boundaries;

import net.finmath.finitedifference.assetderivativevaluation.models.FiniteDifferenceEquityModel;
import net.finmath.finitedifference.solvers.TwoStateActiveBoundaryProvider;
import net.finmath.modelling.products.CallOrPut;

/**
 * Active-state outer boundary provider for the direct two-state knock-in solver
 * under a Bachelier-type equity model.
 *
 * <p>
 * The active state represents the post-activation value, i.e. the corresponding
 * vanilla European option after the barrier has been hit.
 * </p>
 *
 * <p>
 * Boundary conventions:
 * </p>
 * <ul>
 *   <li>Lower boundary:
 *     <ul>
 *       <li>call: 0</li>
 *       <li>put: K exp(-r(T-t))</li>
 *     </ul>
 *   </li>
 *   <li>Upper boundary:
 *     <ul>
 *       <li>call: S - K exp(-r(T-t))</li>
 *       <li>put: 0</li>
 *     </ul>
 *   </li>
 * </ul>
 */
public class BachelierActiveBoundaryProvider implements TwoStateActiveBoundaryProvider {

	/**
	 * The epsilon.
	 */
	private static final double EPSILON = 1E-10;

	/**
	 * The model.
	 */
	private final FiniteDifferenceEquityModel model;
	/**
	 * The strike.
	 */
	private final double strike;
	/**
	 * The maturity.
	 */
	private final double maturity;
	/**
	 * The call or put.
	 */
	private final CallOrPut callOrPut;

	/**
	 * Performs the operation.
	 *
	 * @param model The value.
	 * @param strike The value.
	 * @param maturity The value.
	 * @param callOrPut The value.
	 */
	public BachelierActiveBoundaryProvider(
			final FiniteDifferenceEquityModel model,
			final double strike,
			final double maturity,
			final CallOrPut callOrPut) {
		this.model = model;
		this.strike = strike;
		this.maturity = maturity;
		this.callOrPut = callOrPut;
	}

	@Override
	public double getLowerBoundaryValue(final double time, final double stateVariable) {
		final double t = Math.max(time, EPSILON);
		final double discountFactorRiskFree = model.getRiskFreeCurve().getDiscountFactor(t);
		final double riskFreeRate = -Math.log(discountFactorRiskFree) / t;

		if (callOrPut == CallOrPut.CALL) {
			return 0.0;
		} else {
			return strike * Math.exp(-riskFreeRate * (maturity - time));
		}
	}

	@Override
	public double getUpperBoundaryValue(final double time, final double stateVariable) {
		final double t = Math.max(time, EPSILON);
		final double discountFactorRiskFree = model.getRiskFreeCurve().getDiscountFactor(t);
		final double riskFreeRate = -Math.log(discountFactorRiskFree) / t;

		if (callOrPut == CallOrPut.CALL) {
			return stateVariable - strike * Math.exp(-riskFreeRate * (maturity - time));
		} else {
			return 0.0;
		}
	}
}
