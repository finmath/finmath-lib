package net.finmath.finitedifference.assetderivativevaluation.boundaries;

import net.finmath.finitedifference.assetderivativevaluation.models.FDMSabrModel;
import net.finmath.finitedifference.assetderivativevaluation.products.AmericanOption;
import net.finmath.finitedifference.assetderivativevaluation.products.FiniteDifferenceEquityProduct;
import net.finmath.finitedifference.boundaries.BoundaryCondition;
import net.finmath.finitedifference.boundaries.StandardBoundaryCondition;
import net.finmath.modelling.products.CallOrPut;

/**
 * Boundary conditions for {@link AmericanOption} under the {@link
 * FDMSabrModel}.
 *
 * <p>
 * State variables are assumed to be {@code (S, alpha)}, where {@code S} is the
 * asset level and {@code alpha} the stochastic volatility factor.
 * Dirichlet conditions are imposed in the asset direction, while the
 * volatility-direction boundaries are left untouched via
 * {@link StandardBoundaryCondition#none()}.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class AmericanOptionSabrModelBoundary
implements FiniteDifferenceBoundary {

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
	public AmericanOptionSabrModelBoundary(final FDMSabrModel model) {
		this.model = model;
	}

	@Override
	public BoundaryCondition[] getBoundaryConditionsAtLowerBoundary(
			final FiniteDifferenceEquityProduct product,
			double time,
			final double... stateVariables) {

		final AmericanOption option = (AmericanOption) product;
		final CallOrPut sign = option.getCallOrPut();

		time = Math.max(time, EPSILON);

		final double discountFactorRiskFree = model.getRiskFreeCurve().getDiscountFactor(time);
		final double riskFreeRate = -Math.log(discountFactorRiskFree) / time;

		final double strike = option.getStrike();
		final double maturity = option.getMaturity();

		final BoundaryCondition[] result = new BoundaryCondition[2];

		// S -> 0
		if (sign == CallOrPut.CALL) {
			result[0] = StandardBoundaryCondition.dirichlet(0.0);
		} else {
			result[0] = StandardBoundaryCondition.dirichlet(
					strike * Math.exp(-riskFreeRate * (maturity - time))
					);
		}

		// alpha -> lower boundary: leave PDE row intact
		result[1] = StandardBoundaryCondition.none();

		return result;
	}

	@Override
	public BoundaryCondition[] getBoundaryConditionsAtUpperBoundary(
			final FiniteDifferenceEquityProduct product,
			double time,
			final double... stateVariables) {

		final AmericanOption option = (AmericanOption) product;
		final CallOrPut sign = option.getCallOrPut();

		time = Math.max(time, EPSILON);

		final double discountFactorRiskFree = model.getRiskFreeCurve().getDiscountFactor(time);
		final double riskFreeRate = -Math.log(discountFactorRiskFree) / time;

		final double discountFactorDividend = model.getDividendYieldCurve().getDiscountFactor(time);
		final double dividendYieldRate = -Math.log(discountFactorDividend) / time;

		final double strike = option.getStrike();
		final double maturity = option.getMaturity();

		final double stock = stateVariables.length > 0 ? stateVariables[0] : 0.0;

		final BoundaryCondition[] result = new BoundaryCondition[2];

		// S -> infinity
		if (sign == CallOrPut.CALL) {
			final double dividendAdjustedStockPrice =
					stock * Math.exp(-dividendYieldRate * (maturity - time));
			result[0] = StandardBoundaryCondition.dirichlet(
					dividendAdjustedStockPrice
					- strike * Math.exp(-riskFreeRate * (maturity - time))
					);
		} else {
			result[0] = StandardBoundaryCondition.dirichlet(0.0);
		}

		// alpha -> upper boundary: leave PDE row intact
		result[1] = StandardBoundaryCondition.none();

		return result;
	}
}
