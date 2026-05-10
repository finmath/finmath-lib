package net.finmath.finitedifference.assetderivativevaluation.boundaries;

import net.finmath.finitedifference.assetderivativevaluation.models.FDMHestonModel;
import net.finmath.finitedifference.assetderivativevaluation.products.DoubleBarrierOption;
import net.finmath.finitedifference.assetderivativevaluation.products.FiniteDifferenceEquityProduct;
import net.finmath.finitedifference.boundaries.BoundaryCondition;
import net.finmath.finitedifference.boundaries.StandardBoundaryCondition;

/**
 * Boundary conditions for {@link DoubleBarrierOption} under the
 * {@link FDMHestonModel}.
 *
 * <p>
 * State variables are assumed to be (S, v), where S is the asset level and
 * v the variance.
 * Dirichlet conditions are imposed in the asset direction, while the
 * variance-direction boundaries are left untouched via
 * {@link StandardBoundaryCondition#none()}.
 * </p>
 *
 * <p>
 * The double barriers themselves are enforced internally by the product through
 * state constraints. Therefore, the outer-domain boundaries use the same
 * vanilla asymptotics as {@code EuropeanOption}.
 * </p>
 *
 * <p>
 * This implementation uses grid-aware asset-direction boundary values: instead
 * of applying asymptotic formulas blindly, it evaluates discounted intrinsic
 * value at the actual boundary stock level.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class DoubleBarrierOptionHestonModelBoundary implements FiniteDifferenceBoundary {

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
	public DoubleBarrierOptionHestonModelBoundary(final FDMHestonModel model) {
		this.model = model;
	}

	@Override
	public BoundaryCondition[] getBoundaryConditionsAtLowerBoundary(
			final FiniteDifferenceEquityProduct product,
			double time,
			final double... stateVariables) {

		final DoubleBarrierOption option = (DoubleBarrierOption) product;
		time = Math.max(time, EPSILON);

		final double s = stateVariables.length > 0 ? stateVariables[0] : 0.0;

		final BoundaryCondition[] result = new BoundaryCondition[2];

		result[0] = StandardBoundaryCondition.dirichlet(
				getDiscountedIntrinsicValue(option, time, s)
		);

		// v -> lower boundary: leave PDE row intact
		result[1] = StandardBoundaryCondition.none();

		return result;
	}

	@Override
	public BoundaryCondition[] getBoundaryConditionsAtUpperBoundary(
			final FiniteDifferenceEquityProduct product,
			double time,
			final double... stateVariables) {

		final DoubleBarrierOption option = (DoubleBarrierOption) product;
		time = Math.max(time, EPSILON);

		final double s = stateVariables.length > 0 ? stateVariables[0] : 0.0;

		final BoundaryCondition[] result = new BoundaryCondition[2];

		result[0] = StandardBoundaryCondition.dirichlet(
				getDiscountedIntrinsicValue(option, time, s)
		);

		// v -> upper boundary: leave PDE row intact
		result[1] = StandardBoundaryCondition.none();

		return result;
	}

	private double getDiscountedIntrinsicValue(
			final DoubleBarrierOption option,
			final double evaluationTime,
			final double stock) {

		final double maturity = option.getMaturity();
		final double tau = Math.max(maturity - evaluationTime, 0.0);

		final double discountFactorRiskFree =
				model.getRiskFreeCurve().getDiscountFactor(tau);
		final double discountFactorDividend =
				model.getDividendYieldCurve().getDiscountFactor(tau);

		final double discountedForwardLikeSpot = stock * discountFactorDividend;
		final double discountedStrike = option.getStrike() * discountFactorRiskFree;

		switch (option.getCallOrPut()) {
		case CALL:
			return Math.max(discountedForwardLikeSpot - discountedStrike, 0.0);
		case PUT:
			return Math.max(discountedStrike - discountedForwardLikeSpot, 0.0);
		default:
			throw new IllegalArgumentException("Unsupported option type.");
		}
	}
}
