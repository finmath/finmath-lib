package net.finmath.finitedifference.assetderivativevaluation.boundaries;

import net.finmath.finitedifference.assetderivativevaluation.models.FDMSabrModel;
import net.finmath.finitedifference.assetderivativevaluation.products.EuropeanOption;
import net.finmath.finitedifference.assetderivativevaluation.products.FiniteDifferenceEquityProduct;
import net.finmath.finitedifference.boundaries.BoundaryCondition;
import net.finmath.finitedifference.boundaries.StandardBoundaryCondition;
import net.finmath.modelling.products.CallOrPut;

/**
 * Boundary conditions for {@link EuropeanOption} under the {@link
 * FDMSabrModel}.
 *
 * <p>
 * State variables are assumed to be {@code (S, alpha)}, where {@code S} is the
 * asset level and {@code alpha} is the stochastic volatility factor.
 * Dirichlet conditions are imposed in the asset direction, while the
 * volatility-direction boundaries are left untouched via
 * {@link StandardBoundaryCondition#none()}.
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
public class EuropeanOptionSabrModelBoundary implements FiniteDifferenceBoundary {

	/**
	 * The epsilon.
	 */
	private static final double EPSILON = 1E-6;

	/**
	 * The model.
	 */
	private final FDMSabrModel model;

	/**
	 * Performs the operation.
	 *
	 * @param model The value.
	 */
	public EuropeanOptionSabrModelBoundary(final FDMSabrModel model) {
		this.model = model;
	}

	@Override
	public BoundaryCondition[] getBoundaryConditionsAtLowerBoundary(
			final FiniteDifferenceEquityProduct product,
			double time,
			final double... stateVariables) {

		final EuropeanOption option = (EuropeanOption) product;
		time = Math.max(time, EPSILON);

		final double stock = stateVariables.length > 0 ? stateVariables[0] : 0.0;

		final BoundaryCondition[] result = new BoundaryCondition[2];

		result[0] = StandardBoundaryCondition.dirichlet(
				getDiscountedIntrinsicValue(option, time, stock)
		);

		// alpha -> lower boundary: leave PDE row intact
		result[1] = StandardBoundaryCondition.none();

		return result;
	}

	@Override
	public BoundaryCondition[] getBoundaryConditionsAtUpperBoundary(
			final FiniteDifferenceEquityProduct product,
			double time,
			final double... stateVariables) {

		final EuropeanOption option = (EuropeanOption) product;
		time = Math.max(time, EPSILON);

		final double stock = stateVariables.length > 0 ? stateVariables[0] : 0.0;

		final BoundaryCondition[] result = new BoundaryCondition[2];

		result[0] = StandardBoundaryCondition.dirichlet(
				getDiscountedIntrinsicValue(option, time, stock)
		);

		// alpha -> upper boundary: leave PDE row intact
		result[1] = StandardBoundaryCondition.none();

		return result;
	}

	private double getDiscountedIntrinsicValue(
			final EuropeanOption option,
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
