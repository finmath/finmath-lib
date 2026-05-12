package net.finmath.finitedifference.assetderivativevaluation.boundaries;

import net.finmath.finitedifference.assetderivativevaluation.models.FDMMultiAssetBlackScholesModel;
import net.finmath.finitedifference.assetderivativevaluation.products.BestOfOption;
import net.finmath.finitedifference.assetderivativevaluation.products.FiniteDifferenceEquityProduct;
import net.finmath.finitedifference.boundaries.BoundaryCondition;
import net.finmath.finitedifference.boundaries.StandardBoundaryCondition;
import net.finmath.functions.AnalyticFormulas;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.modelling.products.CallOrPut;

/**
 * Boundary conditions for {@link BestOfOption} under
 * {@link FDMMultiAssetBlackScholesModel}.
 *
 * <p>
 * The payoff is
 * </p>
 *
 * <p>
 * <i>
 * \left( \omega \left( \max(S_1(T), S_2(T)) - K \right) \right)^{+},
 * </i>
 * </p>
 *
 * <p>
 * where {@code \omega \in \{+1,-1\}} is the call/put sign.
 * </p>
 *
 * <p>
 * The multidimensional boundary convention of the framework is used:
 * </p>
 * <ul>
 * <li>index {@code 0} of the returned array corresponds to the first state
 * variable,</li>
 *   <li>index {@code 1} corresponds to the second state variable.</li>
 * </ul>
 *
 * <p>
 * On the lower faces {@code S_1 = 0} and {@code S_2 = 0}, the best-of payoff
 * reduces exactly to a one-dimensional vanilla option on the remaining asset.
 * Accordingly, this boundary class requires both asset grids to start at zero.
 * </p>
 *
 * <p>
 * On the upper faces, asymptotically correct Dirichlet values are used:
 * </p>
 * <ul>
 *   <li>on {@code S_1 = S_{1,\max}}, the call is approximated by
 * {@code S_1 e^{-q_1 \tau} - K e^{-r \tau}} and the put by {@code 0},</li>
 *   <li>on {@code S_2 = S_{2,\max}}, the call is approximated by
 * {@code S_2 e^{-q_2 \tau} - K e^{-r \tau}} and the put by {@code 0}.</li>
 * </ul>
 *
 * @author Alessandro Gnoatto
 */
public class BestOfOptionMultiAssetBlackScholesModelBoundary implements FiniteDifferenceBoundary {

	/**
	 * The time floor.
	 */
	private static final double TIME_FLOOR = 1E-10;
	/**
	 * The grid tolerance.
	 */
	private static final double GRID_TOLERANCE = 1E-12;

	/**
	 * The model.
	 */
	private final FDMMultiAssetBlackScholesModel model;

	/**
	 * Performs the operation.
	 *
	 * @param model The value.
	 */
	public BestOfOptionMultiAssetBlackScholesModelBoundary(final FDMMultiAssetBlackScholesModel model) {
		if (model == null) {
			throw new IllegalArgumentException("model must not be null.");
		}
		if (model.getInitialValue() == null || model.getInitialValue().length != 2) {
			throw new IllegalArgumentException(
					"BestOfOptionMultiAssetBlackScholesModelBoundary requires a two-dimensional model.");
		}

		final double[] firstGrid = model.getSpaceTimeDiscretization().getSpaceGrid(0).getGrid();
		final double[] secondGrid = model.getSpaceTimeDiscretization().getSpaceGrid(1).getGrid();

		if (Math.abs(firstGrid[0]) > GRID_TOLERANCE || Math.abs(secondGrid[0]) > GRID_TOLERANCE) {
			throw new IllegalArgumentException(
					"BestOfOptionMultiAssetBlackScholesModelBoundary requires both asset grids to start at 0. "
							+ "The lower-face one-dimensional reduction is exact only at S1 = 0 and S2 = 0.");
		}

		this.model = model;
	}

	@Override
	public BoundaryCondition[] getBoundaryConditionsAtLowerBoundary(
			final FiniteDifferenceEquityProduct product,
			final double time,
			final double... stateVariables) {

		final BestOfOption bestOfOption = validateAndCastProduct(product);
		validateStateVariables(stateVariables);

		final double strike = bestOfOption.getStrike();
		final double tau = Math.max(bestOfOption.getMaturity() - time, 0.0);

		final double s1 = stateVariables[0];
		final double s2 = stateVariables[1];

		final BoundaryCondition[] conditions = new BoundaryCondition[] {
				StandardBoundaryCondition.none(),
				StandardBoundaryCondition.none()
		};

		/*
		 * On S1 = 0, max(S1, S2) = S2 since S2 >= 0.
		 */
		if (isAtLowerBoundary(0, s1)) {
			conditions[0] = StandardBoundaryCondition.dirichlet(
					getReducedOneDimensionalValue(
							bestOfOption.getCallOrPut(),
							s2,
							strike,
							1,
							tau
					)
			);
		}

		/*
		 * On S2 = 0, max(S1, S2) = S1 since S1 >= 0.
		 */
		if (isAtLowerBoundary(1, s2)) {
			conditions[1] = StandardBoundaryCondition.dirichlet(
					getReducedOneDimensionalValue(
							bestOfOption.getCallOrPut(),
							s1,
							strike,
							0,
							tau
					)
			);
		}

		return conditions;
	}

	@Override
	public BoundaryCondition[] getBoundaryConditionsAtUpperBoundary(
			final FiniteDifferenceEquityProduct product,
			final double time,
			final double... stateVariables) {

		final BestOfOption bestOfOption = validateAndCastProduct(product);
		validateStateVariables(stateVariables);

		final double strike = bestOfOption.getStrike();
		final double tau = Math.max(bestOfOption.getMaturity() - time, 0.0);

		final double s1 = stateVariables[0];
		final double s2 = stateVariables[1];

		final BoundaryCondition[] conditions = new BoundaryCondition[] {
				StandardBoundaryCondition.none(),
				StandardBoundaryCondition.none()
		};

		if (isAtUpperBoundary(0, s1)) {
			conditions[0] = StandardBoundaryCondition.dirichlet(
					getUpperFaceValue(
							bestOfOption.getCallOrPut(),
							s1,
							strike,
							0,
							tau
					)
			);
		}

		if (isAtUpperBoundary(1, s2)) {
			conditions[1] = StandardBoundaryCondition.dirichlet(
					getUpperFaceValue(
							bestOfOption.getCallOrPut(),
							s2,
							strike,
							1,
							tau
					)
			);
		}

		return conditions;
	}

	private BestOfOption validateAndCastProduct(final FiniteDifferenceEquityProduct product) {
		if (!(product instanceof BestOfOption)) {
			throw new IllegalArgumentException(
					"BestOfOptionMultiAssetBlackScholesModelBoundary requires a BestOfOption.");
		}

		return (BestOfOption) product;
	}

	private void validateStateVariables(final double[] stateVariables) {
		if (stateVariables == null || stateVariables.length != 2) {
			throw new IllegalArgumentException("Two state variables are required.");
		}
	}

	private boolean isAtLowerBoundary(final int dimension, final double value) {
		final double[] grid = model.getSpaceTimeDiscretization().getSpaceGrid(dimension).getGrid();
		return Math.abs(value - grid[0]) <= GRID_TOLERANCE;
	}

	private boolean isAtUpperBoundary(final int dimension, final double value) {
		final double[] grid = model.getSpaceTimeDiscretization().getSpaceGrid(dimension).getGrid();
		return Math.abs(value - grid[grid.length - 1]) <= GRID_TOLERANCE;
	}

	private double getReducedOneDimensionalValue(
			final CallOrPut callOrPut,
			final double spot,
			final double strike,
			final int assetIndex,
			final double tau) {

		if (callOrPut == CallOrPut.CALL) {
			return getBlackScholesCallValue(spot, strike, assetIndex, tau);
		}

		return getBlackScholesPutValue(spot, strike, assetIndex, tau);
	}

	private double getUpperFaceValue(
			final CallOrPut callOrPut,
			final double boundarySpot,
			final double strike,
			final int assetIndex,
			final double tau) {

		if (callOrPut == CallOrPut.PUT) {
			return 0.0;
		}

		return Math.max(
				boundarySpot * getDividendDiscountFactor(assetIndex, tau)
				- strike * getRiskFreeDiscountFactor(tau),
				0.0
		);
	}

	private double getBlackScholesCallValue(
			final double spot,
			final double strike,
			final int assetIndex,
			final double tau) {

		if (tau <= 0.0) {
			return Math.max(spot - strike, 0.0);
		}
		if (spot <= 0.0) {
			return 0.0;
		}
		if (strike <= 0.0) {
			return spot * getDividendDiscountFactor(assetIndex, tau)
					- strike * getRiskFreeDiscountFactor(tau);
		}

		final double riskFreeRate = getRiskFreeRate(tau);
		final double dividendYield = getDividendYieldRate(assetIndex, tau);
		final double forward = spot * Math.exp((riskFreeRate - dividendYield) * tau);
		final double payoffUnit = Math.exp(-riskFreeRate * tau);

		return AnalyticFormulas.blackScholesGeneralizedOptionValue(
				forward,
				model.getVolatilities()[assetIndex],
				tau,
				strike,
				payoffUnit
		);
	}

	private double getBlackScholesPutValue(
			final double spot,
			final double strike,
			final int assetIndex,
			final double tau) {

		if (tau <= 0.0) {
			return Math.max(strike - spot, 0.0);
		}
		if (strike <= 0.0) {
			return 0.0;
		}
		if (spot <= 0.0) {
			return strike * getRiskFreeDiscountFactor(tau);
		}

		final double callValue = getBlackScholesCallValue(spot, strike, assetIndex, tau);

		return callValue
				- spot * getDividendDiscountFactor(assetIndex, tau)
				+ strike * getRiskFreeDiscountFactor(tau);
	}

	private double getRiskFreeRate(final double tau) {
		final double safeTau = Math.max(tau, TIME_FLOOR);
		final DiscountCurve curve = model.getRiskFreeCurve();
		return -Math.log(curve.getDiscountFactor(safeTau)) / safeTau;
	}

	private double getDividendYieldRate(final int assetIndex, final double tau) {
		final double safeTau = Math.max(tau, TIME_FLOOR);
		final DiscountCurve curve = model.getDividendYieldCurves()[assetIndex];
		return -Math.log(curve.getDiscountFactor(safeTau)) / safeTau;
	}

	private double getRiskFreeDiscountFactor(final double tau) {
		if (tau <= 0.0) {
			return 1.0;
		}
		return model.getRiskFreeCurve().getDiscountFactor(tau);
	}

	private double getDividendDiscountFactor(final int assetIndex, final double tau) {
		if (tau <= 0.0) {
			return 1.0;
		}
		return model.getDividendYieldCurves()[assetIndex].getDiscountFactor(tau);
	}
}
