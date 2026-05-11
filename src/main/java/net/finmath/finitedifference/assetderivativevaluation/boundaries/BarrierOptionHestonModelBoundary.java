package net.finmath.finitedifference.assetderivativevaluation.boundaries;

import net.finmath.finitedifference.assetderivativevaluation.models.FDMHestonModel;
import net.finmath.finitedifference.assetderivativevaluation.products.BarrierOption;
import net.finmath.finitedifference.assetderivativevaluation.products.FiniteDifferenceEquityProduct;
import net.finmath.finitedifference.boundaries.BoundaryCondition;
import net.finmath.finitedifference.boundaries.StandardBoundaryCondition;
import net.finmath.modelling.products.BarrierType;
import net.finmath.modelling.products.CallOrPut;

/**
 * Boundary conditions for {@link BarrierOption} under the {@link
 * FDMHestonModel}.
 *
 * <p>
 * State variables are assumed to be (S, v), where S is the asset level and v
 * the variance.
 * For knock-out options, Dirichlet conditions are imposed on the barrier side
 * in the asset direction.
 * Variance-direction boundaries are left untouched via {@link
 * StandardBoundaryCondition#none()}.
 * </p>
 *
 * <p>
 * The barrier is assumed to coincide with the lower or upper boundary of the
 * asset grid.
 * In-options may still be handled elsewhere by the product logic.
 * </p>
 *
 * <p>
 * This version uses grid-aware continuation-side spot boundary values, with two
 * targeted
 * exceptions motivated by the remaining Heston outlier cases:
 * </p>
 * <ul>
 * <li>for UP_OUT CALL at the lower spot boundary, use {@code none()} instead of
 * a hard zero,</li>
 * <li>for DOWN_OUT PUT at the upper spot boundary, use {@code none()} instead
 * of a hard zero.</li>
 * </ul>
 *
 * @author Alessandro Gnoatto
 */
public class BarrierOptionHestonModelBoundary implements FiniteDifferenceBoundary {

	/**
	 * The epsilon.
	 */
	private static final double EPSILON = 1E-6;

	/**
	 * The model.
	 */
	private final FDMHestonModel model;

	/**
	 * Performs the operation.
	 *
	 * @param model The value.
	 */
	public BarrierOptionHestonModelBoundary(final FDMHestonModel model) {
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

		time = Math.max(time, EPSILON);

		final double stock = stateVariables.length > 0 ? stateVariables[0] : 0.0;

		final BoundaryCondition[] result = new BoundaryCondition[2];

		/*
		 * S -> lower boundary
		 *
		 * If the lower boundary is the knock-out barrier, use rebate.
		 *
		 * Targeted relaxation:
		 * For UP_OUT CALL, the lower continuation-side spot boundary was
		 * previously
		 * clamped to zero. We now leave that PDE row free via none().
		 */
		if (barrierType == BarrierType.DOWN_OUT) {
			result[0] = StandardBoundaryCondition.dirichlet(option.getRebate());
		} else if (barrierType == BarrierType.UP_OUT && sign == CallOrPut.CALL) {
			result[0] = StandardBoundaryCondition.none();
		} else {
			result[0] = StandardBoundaryCondition.dirichlet(
					getDiscountedIntrinsicValue(option, time, stock)
			);
		}

		// v -> lower boundary
		result[1] = StandardBoundaryCondition.none();

		return result;
	}

	@Override
	public BoundaryCondition[] getBoundaryConditionsAtUpperBoundary(
			final FiniteDifferenceEquityProduct product,
			double time,
			final double... stateVariables) {

		final BarrierOption option = (BarrierOption) product;
		final BarrierType barrierType = option.getBarrierType();
		final CallOrPut sign = option.getCallOrPut();

		time = Math.max(time, EPSILON);

		final double stock = stateVariables.length > 0 ? stateVariables[0] : 0.0;

		final BoundaryCondition[] result = new BoundaryCondition[2];

		/*
		 * S -> upper boundary
		 *
		 * If the upper boundary is the knock-out barrier, use rebate.
		 *
		 * Targeted relaxation:
		 * For DOWN_OUT PUT, the upper continuation-side spot boundary was
		 * previously
		 * clamped to zero. We now leave that PDE row free via none().
		 */
		if (barrierType == BarrierType.UP_OUT) {
			result[0] = StandardBoundaryCondition.dirichlet(option.getRebate());
		} else if (barrierType == BarrierType.DOWN_OUT && sign == CallOrPut.PUT) {
			result[0] = StandardBoundaryCondition.none();
		} else {
			result[0] = StandardBoundaryCondition.dirichlet(
					getDiscountedIntrinsicValue(option, time, stock)
			);
		}

		// v -> upper boundary
		result[1] = StandardBoundaryCondition.none();

		return result;
	}

	private double getDiscountedIntrinsicValue(
			final BarrierOption option,
			final double evaluationTime,
			final double stock) {

		final double maturity = option.getMaturity();
		final double tau = Math.max(maturity - evaluationTime, 0.0);

		final double discountFactorRiskFree = model.getRiskFreeCurve().getDiscountFactor(tau);
		final double discountFactorDividend = model.getDividendYieldCurve().getDiscountFactor(tau);

		final double discountedForwardLikeSpot = stock * discountFactorDividend;
		final double discountedStrike = option.getStrike() * discountFactorRiskFree;

		if (option.getCallOrPut() == CallOrPut.CALL) {
			return Math.max(discountedForwardLikeSpot - discountedStrike, 0.0);
		} else {
			return Math.max(discountedStrike - discountedForwardLikeSpot, 0.0);
		}
	}
}
