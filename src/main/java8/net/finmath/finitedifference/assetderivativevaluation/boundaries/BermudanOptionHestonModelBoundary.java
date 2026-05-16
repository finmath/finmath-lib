package net.finmath.finitedifference.assetderivativevaluation.boundaries;

import net.finmath.finitedifference.assetderivativevaluation.models.FDMHestonModel;
import net.finmath.finitedifference.assetderivativevaluation.products.BermudanOption;
import net.finmath.finitedifference.assetderivativevaluation.products.FiniteDifferenceEquityProduct;
import net.finmath.finitedifference.boundaries.BoundaryCondition;
import net.finmath.finitedifference.boundaries.StandardBoundaryCondition;
import net.finmath.modelling.products.CallOrPut;

/**
 * Boundary conditions for {@link BermudanOption} under the {@link
 * FDMHestonModel}.
 *
 * <p>
 * State variables are assumed to be (S, v), where S is the asset level and v
 * the variance.
 * Dirichlet conditions are imposed in the asset direction, while the variance-
 * direction
 * boundaries are left untouched via {@link StandardBoundaryCondition#none()}.
 * </p>
 */
public class BermudanOptionHestonModelBoundary
		implements FiniteDifferenceBoundary {

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
	public BermudanOptionHestonModelBoundary(final FDMHestonModel model) {
		this.model = model;
	}

	@Override
	public BoundaryCondition[] getBoundaryConditionsAtLowerBoundary(
			final FiniteDifferenceEquityProduct product,
			double time,
			final double... stateVariables) {

		final BermudanOption option = (BermudanOption) product;
		final CallOrPut sign = option.getCallOrPut();

		time = Math.max(time, EPSILON);

		final double discountFactorRiskFree = model.getRiskFreeCurve().getDiscountFactor(time);
		final double riskFreeRate = -Math.log(discountFactorRiskFree) / time;

		final double strike = option.getStrike();
		final double maturity = option.getMaturity();

		final BoundaryCondition[] result = new BoundaryCondition[2];

		if (sign == CallOrPut.CALL) {
			result[0] = StandardBoundaryCondition.dirichlet(0.0);
		} else {
			result[0] = StandardBoundaryCondition.dirichlet(
					strike * Math.exp(-riskFreeRate * (maturity - time))
			);
		}

		result[1] = StandardBoundaryCondition.none();

		return result;
	}

	@Override
	public BoundaryCondition[] getBoundaryConditionsAtUpperBoundary(
			final FiniteDifferenceEquityProduct product,
			double time,
			final double... stateVariables) {

		final BermudanOption option = (BermudanOption) product;
		final CallOrPut sign = option.getCallOrPut();

		time = Math.max(time, EPSILON);

		final double discountFactorRiskFree = model.getRiskFreeCurve().getDiscountFactor(time);
		final double riskFreeRate = -Math.log(discountFactorRiskFree) / time;

		final double discountFactorDividend = model.getDividendYieldCurve().getDiscountFactor(time);
		final double dividendYieldRate = -Math.log(discountFactorDividend) / time;

		final double strike = option.getStrike();
		final double maturity = option.getMaturity();

		final double s = stateVariables.length > 0 ? stateVariables[0] : 0.0;

		final BoundaryCondition[] result = new BoundaryCondition[2];

		if (sign == CallOrPut.CALL) {
			final double dividendAdjustedStockPrice =
					s * Math.exp(-dividendYieldRate * (maturity - time));
			result[0] = StandardBoundaryCondition.dirichlet(
					dividendAdjustedStockPrice
					- strike * Math.exp(-riskFreeRate * (maturity - time))
			);
		} else {
			result[0] = StandardBoundaryCondition.dirichlet(0.0);
		}

		result[1] = StandardBoundaryCondition.none();

		return result;
	}
}
