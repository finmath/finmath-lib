package net.finmath.finitedifference.assetderivativevaluation.boundaries;

import net.finmath.finitedifference.assetderivativevaluation.models.FDMVarianceGammaModel;
import net.finmath.finitedifference.assetderivativevaluation.products.EuropeanOption;
import net.finmath.finitedifference.assetderivativevaluation.products.FiniteDifferenceEquityProduct;
import net.finmath.finitedifference.boundaries.BoundaryCondition;
import net.finmath.finitedifference.boundaries.StandardBoundaryCondition;
import net.finmath.modelling.products.CallOrPut;

/**
 * Boundary conditions for {@link EuropeanOption} under the
 * {@link FDMVarianceGammaModel}.
 *
 * <p>
 * The class follows the standard factory naming convention
 * {@code <ProductSimpleName><ModelCoreName>Boundary}, so that it can be
 * instantiated automatically by {@link FDBoundaryFactory}.
 * </p>
 *
 * <p>
 * Since the finite-difference Variance Gamma model is formulated in the stock
 * variable, the standard one-dimensional vanilla outer-domain asymptotics are
 * used.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class EuropeanOptionVarianceGammaModelBoundary implements FiniteDifferenceBoundary {

	/**
	 * The model.
	 */
	private final FDMVarianceGammaModel model;
	/**
	 * The epsilon.
	 */
	private static final double EPSILON = 1E-6;

	/**
	 * Creates the boundary condition associated with a given
	 * {@link FDMVarianceGammaModel}.
	 *
	 * @param model The Variance Gamma model used to determine risk-free and
	 *              dividend discount factors.
	 */
	public EuropeanOptionVarianceGammaModelBoundary(final FDMVarianceGammaModel model) {
		this.model = model;
	}

	@Override
	public BoundaryCondition[] getBoundaryConditionsAtLowerBoundary(
			final FiniteDifferenceEquityProduct product,
			double time,
			final double... stateVariables) {

		final EuropeanOption option = (EuropeanOption) product;
		final CallOrPut sign = option.getCallOrPut();

		if (sign == CallOrPut.CALL) {
			return new BoundaryCondition[] {
					StandardBoundaryCondition.dirichlet(0.0)
			};
		}

		time = Math.max(time, EPSILON);

		final double discountFactorRiskFree = model.getRiskFreeCurve().getDiscountFactor(time);
		final double riskFreeRate = -Math.log(discountFactorRiskFree) / time;

		final double strike = option.getStrike();
		final double maturity = option.getMaturity();

		return new BoundaryCondition[] {
				StandardBoundaryCondition.dirichlet(
						strike * Math.exp(-riskFreeRate * (maturity - time))
				)
		};
	}

	@Override
	public BoundaryCondition[] getBoundaryConditionsAtUpperBoundary(
			final FiniteDifferenceEquityProduct product,
			double time,
			final double... stateVariables) {

		final EuropeanOption option = (EuropeanOption) product;
		final CallOrPut sign = option.getCallOrPut();

		if (sign == CallOrPut.PUT) {
			return new BoundaryCondition[] {
					StandardBoundaryCondition.dirichlet(0.0)
			};
		}

		time = Math.max(time, EPSILON);

		final double discountFactorRiskFree = model.getRiskFreeCurve().getDiscountFactor(time);
		final double riskFreeRate = -Math.log(discountFactorRiskFree) / time;

		final double discountFactorDividend = model.getDividendYieldCurve().getDiscountFactor(time);
		final double dividendYieldRate = -Math.log(discountFactorDividend) / time;

		final double strike = option.getStrike();
		final double maturity = option.getMaturity();
		final double stock = stateVariables[0];

		final double dividendAdjustedStockPrice =
				stock * Math.exp(-dividendYieldRate * (maturity - time));

		return new BoundaryCondition[] {
				StandardBoundaryCondition.dirichlet(
						dividendAdjustedStockPrice
						- strike * Math.exp(-riskFreeRate * (maturity - time))
				)
		};
	}
}
