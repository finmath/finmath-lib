package net.finmath.finitedifference.assetderivativevaluation.boundaries;

import net.finmath.finitedifference.assetderivativevaluation.models.FDMBlackScholesModel;
import net.finmath.finitedifference.assetderivativevaluation.products.BarrierOption;
import net.finmath.finitedifference.assetderivativevaluation.products.FiniteDifferenceEquityProduct;
import net.finmath.finitedifference.boundaries.BoundaryCondition;
import net.finmath.finitedifference.boundaries.StandardBoundaryCondition;
import net.finmath.modelling.products.BarrierType;
import net.finmath.modelling.products.CallOrPut;

/**
 * Boundary conditions for {@link BarrierOption} under the {@link
 * FDMBlackScholesModel}.
 *
 * <p>
 * This class supports the explicit boundary-condition API returning
 * {@link BoundaryCondition} objects.
 * </p>
 *
 * <p>
 * Conventions:
 * </p>
 * <ul>
 *   <li>For knock-out options, the rebate is paid at hit, so the barrier-side
 *       boundary is pinned to the rebate.</li>
 *   <li>For knock-in options, the outer boundaries use the corresponding
 *       vanilla Black-Scholes asymptotics.</li>
 * <li>The barrier is assumed to coincide with one spatial boundary of the grid
 *       for direct knock-out pricing:
 * down barriers at the lower boundary, up barriers at the upper boundary.</li>
 * </ul>
 *
 * <p>
 * In the direct two-state knock-in solver, the activated regime should use
 * these
 * vanilla asymptotic boundaries, while the inactive regime is coupled to the
 * activated regime on the hit set.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class BarrierOptionBlackScholesModelBoundary implements FiniteDifferenceBoundary {

	/**
	 * The epsilon.
	 */
	private static final double EPSILON = 1E-6;

	/**
	 * The model.
	 */
	private final FDMBlackScholesModel model;

	/**
	 * Performs the operation.
	 *
	 * @param model The value.
	 */
	public BarrierOptionBlackScholesModelBoundary(final FDMBlackScholesModel model) {
		this.model = model;
	}

	@Override
	public BoundaryCondition[] getBoundaryConditionsAtLowerBoundary(
			final FiniteDifferenceEquityProduct product,
			double time,
			final double... stateVariables) {

		final BarrierOption option = (BarrierOption) product;
		final BarrierType barrierType = option.getBarrierType();
		final CallOrPut sign = option.getCallOrPut();

		/*
		 * For a down-and-out option, the lower boundary is the barrier-side
		 * boundary,
		 * so the value is pinned to the rebate (paid at hit).
		 *
		 * For all other cases, including down-in, use vanilla asymptotics.
		 */
		if (barrierType == BarrierType.DOWN_OUT) {
			return new BoundaryCondition[] {
					StandardBoundaryCondition.dirichlet(option.getRebate())
			};
		}

		if (sign == CallOrPut.CALL) {
			/*
			 * Vanilla call asymptotic at S -> 0.
			 */
			return new BoundaryCondition[] {
					StandardBoundaryCondition.dirichlet(0.0)
			};
		} else {
			/*
			 * Vanilla put asymptotic at S -> 0:
			 * V(t,S) ~ K exp(-r(T-t)).
			 */
			time = Math.max(time, EPSILON);

			final double discountFactorRiskFree =
					model.getRiskFreeCurve().getDiscountFactor(time);
			final double riskFreeRate =
					-Math.log(discountFactorRiskFree) / time;

			final double strike = option.getStrike();
			final double maturity = option.getMaturity();

			final double value =
					strike * Math.exp(-riskFreeRate * (maturity - time));

			return new BoundaryCondition[] {
					StandardBoundaryCondition.dirichlet(value)
			};
		}
	}

	@Override
	public BoundaryCondition[] getBoundaryConditionsAtUpperBoundary(
			final FiniteDifferenceEquityProduct product,
			double time,
			final double... stateVariables) {

		final BarrierOption option = (BarrierOption) product;
		final BarrierType barrierType = option.getBarrierType();
		final CallOrPut sign = option.getCallOrPut();

		/*
		 * For an up-and-out option, the upper boundary is the barrier-side
		 * boundary,
		 * so the value is pinned to the rebate (paid at hit).
		 *
		 * For all other cases, including up-in, use vanilla asymptotics.
		 */
		if (barrierType == BarrierType.UP_OUT) {
			return new BoundaryCondition[] {
					StandardBoundaryCondition.dirichlet(option.getRebate())
			};
		}

		if (sign == CallOrPut.CALL) {
			/*
			 * Vanilla call asymptotic at large S:
			 * V(t,S) ~ S exp(-q(T-t)) - K exp(-r(T-t)).
			 */
			time = Math.max(time, EPSILON);

			final double discountFactorRiskFree =
					model.getRiskFreeCurve().getDiscountFactor(time);
			final double riskFreeRate =
					-Math.log(discountFactorRiskFree) / time;

			final double discountFactorDividend =
					model.getDividendYieldCurve().getDiscountFactor(time);
			final double dividendYieldRate =
					-Math.log(discountFactorDividend) / time;

			final double strike = option.getStrike();
			final double maturity = option.getMaturity();
			final double stateVariable = stateVariables[0];

			final double dividendAdjustedStockPrice =
					stateVariable * Math.exp(-dividendYieldRate * (maturity - time));

			final double value =
					dividendAdjustedStockPrice
					- strike * Math.exp(-riskFreeRate * (maturity - time));

			return new BoundaryCondition[] {
					StandardBoundaryCondition.dirichlet(value)
			};
		} else {
			/*
			 * Vanilla put asymptotic at large S.
			 */
			return new BoundaryCondition[] {
					StandardBoundaryCondition.dirichlet(0.0)
			};
		}
	}
}
