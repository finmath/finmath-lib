package net.finmath.finitedifference.assetderivativevaluation.boundaries;

import net.finmath.finitedifference.assetderivativevaluation.models.FDMMultiAssetBlackScholesModel;
import net.finmath.finitedifference.assetderivativevaluation.products.FiniteDifferenceEquityProduct;
import net.finmath.finitedifference.assetderivativevaluation.products.WorstOfOption;
import net.finmath.finitedifference.boundaries.BoundaryCondition;
import net.finmath.finitedifference.boundaries.StandardBoundaryCondition;
import net.finmath.functions.AnalyticFormulas;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.modelling.products.CallOrPut;

/**
 * Boundary conditions for {@link WorstOfOption} under
 * {@link FDMMultiAssetBlackScholesModel}.
 *
 * <p>
 * The product payoff is
 * </p>
 *
 * <p>
 * <i>
 * \left( \omega \left( \min(S_1(T), S_2(T)) - K \right) \right)^{+},
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
 * On the lower faces {@code S_1 = 0} and {@code S_2 = 0}, the worst-of
 * underlying is locked at zero for all future times, since zero is absorbing
 * in the Black-Scholes model. Hence the option value on the lower faces is the
 * discounted deterministic payoff
 * </p>
 *
 * <p>
 * <i>
 * e^{-r \tau} \left( \omega (0 - K) \right)^{+}.
 * </i>
 * </p>
 *
 * <p>
 * Accordingly, this boundary class requires both asset grids to start at zero.
 * </p>
 *
 * <p>
 * On the upper faces, asymptotically one asset is so large that the worst-of
 * underlying behaves like the remaining asset. Therefore:
 * </p>
 * <ul>
 *   <li>on {@code S_1 = S_{1,max}}, the boundary is approximated by the
 *       corresponding one-dimensional vanilla option on {@code S_2},</li>
 *   <li>on {@code S_2 = S_{2,max}}, the boundary is approximated by the
 *       corresponding one-dimensional vanilla option on {@code S_1}.</li>
 * </ul>
 *
 * @author Alessandro Gnoatto
 */
public class WorstOfOptionMultiAssetBlackScholesModelBoundary implements FiniteDifferenceBoundary {

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
	 * Creates the boundary class.
	 *
	 * @param model The underlying finite-difference model.
	 */
	public WorstOfOptionMultiAssetBlackScholesModelBoundary(final FDMMultiAssetBlackScholesModel model) {
		if (model == null) {
			throw new IllegalArgumentException("model must not be null.");
		}
		if (model.getInitialValue() == null || model.getInitialValue().length != 2) {
			throw new IllegalArgumentException(
					"WorstOfOptionMultiAssetBlackScholesModelBoundary requires a two-dimensional model.");
		}

		final double[] firstGrid = model.getSpaceTimeDiscretization().getSpaceGrid(0).getGrid();
		final double[] secondGrid = model.getSpaceTimeDiscretization().getSpaceGrid(1).getGrid();

		if (Math.abs(firstGrid[0]) > GRID_TOLERANCE || Math.abs(secondGrid[0]) > GRID_TOLERANCE) {
			throw new IllegalArgumentException(
					"WorstOfOptionMultiAssetBlackScholesModelBoundary requires both asset grids to start at 0. "
							+ "The lower-face deterministic reduction is exact only at S1 = 0 and S2 = 0.");
		}

		this.model = model;
	}

	@Override
	public BoundaryCondition[] getBoundaryConditionsAtLowerBoundary(
			final FiniteDifferenceEquityProduct product,
			final double time,
			final double... stateVariables) {

		final WorstOfOption worstOfOption = validateAndCastProduct(product);
		validateStateVariables(stateVariables);

		final double tau = Math.max(worstOfOption.getMaturity() - time, 0.0);
		final double s1 = stateVariables[0];
		final double s2 = stateVariables[1];

		final BoundaryCondition[] conditions = new BoundaryCondition[] {
				StandardBoundaryCondition.none(),
				StandardBoundaryCondition.none()
		};

		final double deterministicLowerFaceValue = getDeterministicLowerFaceValue(
				worstOfOption.getCallOrPut(),
				worstOfOption.getStrike(),
				tau
		);

		if (isAtLowerBoundary(0, s1)) {
			conditions[0] = StandardBoundaryCondition.dirichlet(deterministicLowerFaceValue);
		}

		if (isAtLowerBoundary(1, s2)) {
			conditions[1] = StandardBoundaryCondition.dirichlet(deterministicLowerFaceValue);
		}

		return conditions;
	}

	@Override
	public BoundaryCondition[] getBoundaryConditionsAtUpperBoundary(
			final FiniteDifferenceEquityProduct product,
			final double time,
			final double... stateVariables) {

		final WorstOfOption worstOfOption = validateAndCastProduct(product);
		validateStateVariables(stateVariables);

		final double tau = Math.max(worstOfOption.getMaturity() - time, 0.0);
		final double strike = worstOfOption.getStrike();

		final double s1 = stateVariables[0];
		final double s2 = stateVariables[1];

		final BoundaryCondition[] conditions = new BoundaryCondition[] {
				StandardBoundaryCondition.none(),
				StandardBoundaryCondition.none()
		};

		/*
		 * On S1 = S1_max, approximate min(S1,S2) by S2.
		 */
		if (isAtUpperBoundary(0, s1)) {
			conditions[0] = StandardBoundaryCondition.dirichlet(
					getReducedOneDimensionalValue(
							worstOfOption.getCallOrPut(),
							s2,
							strike,
							1,
							tau
					)
			);
		}

		/*
		 * On S2 = S2_max, approximate min(S1,S2) by S1.
		 */
		if (isAtUpperBoundary(1, s2)) {
			conditions[1] = StandardBoundaryCondition.dirichlet(
					getReducedOneDimensionalValue(
							worstOfOption.getCallOrPut(),
							s1,
							strike,
							0,
							tau
					)
			);
		}

		return conditions;
	}

	/**
	 * Validates and casts the product.
	 *
	 * @param product The product.
	 * @return The worst-of option.
	 */
	private WorstOfOption validateAndCastProduct(final FiniteDifferenceEquityProduct product) {
		if (!(product instanceof WorstOfOption)) {
			throw new IllegalArgumentException(
					"WorstOfOptionMultiAssetBlackScholesModelBoundary requires a WorstOfOption.");
		}

		return (WorstOfOption) product;
	}

	/**
	 * Validates the state variables.
	 *
	 * @param stateVariables The state variables.
	 */
	private void validateStateVariables(final double[] stateVariables) {
		if (stateVariables == null || stateVariables.length != 2) {
			throw new IllegalArgumentException("Two state variables are required.");
		}
	}

	/**
	 * Checks if the point is on the lower boundary of a given dimension.
	 *
	 * @param dimension Dimension index.
	 * @param value State-variable value.
	 * @return True if the point is on the lower boundary.
	 */
	private boolean isAtLowerBoundary(final int dimension, final double value) {
		final double[] grid = model.getSpaceTimeDiscretization().getSpaceGrid(dimension).getGrid();
		return Math.abs(value - grid[0]) <= GRID_TOLERANCE;
	}

	/**
	 * Checks if the point is on the upper boundary of a given dimension.
	 *
	 * @param dimension Dimension index.
	 * @param value State-variable value.
	 * @return True if the point is on the upper boundary.
	 */
	private boolean isAtUpperBoundary(final int dimension, final double value) {
		final double[] grid = model.getSpaceTimeDiscretization().getSpaceGrid(dimension).getGrid();
		return Math.abs(value - grid[grid.length - 1]) <= GRID_TOLERANCE;
	}

	/**
	 * Deterministic lower-face value.
	 *
	 * @param callOrPut Option type.
	 * @param strike Strike.
	 * @param tau Time to maturity.
	 * @return Discounted deterministic payoff on the lower faces.
	 */
	private double getDeterministicLowerFaceValue(
			final CallOrPut callOrPut,
			final double strike,
			final double tau) {
		return Math.max(callOrPut.toInteger() * (0.0 - strike), 0.0) * getRiskFreeDiscountFactor(tau);
	}

	/**
	 * One-dimensional reduced value on the remaining asset.
	 *
	 * @param callOrPut Option type.
	 * @param spot Remaining asset spot.
	 * @param strike Strike.
	 * @param assetIndex Asset index of the remaining asset.
	 * @param tau Time to maturity.
	 * @return Reduced one-dimensional value.
	 */
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

	/**
	 * Black-Scholes call value.
	 *
	 * @param spot Spot.
	 * @param strike Strike.
	 * @param assetIndex Asset index.
	 * @param tau Time to maturity.
	 * @return Call value.
	 */
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

	/**
	 * Black-Scholes put value.
	 *
	 * @param spot Spot.
	 * @param strike Strike.
	 * @param assetIndex Asset index.
	 * @param tau Time to maturity.
	 * @return Put value.
	 */
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

	/**
	 * Risk-free rate extracted from the curve.
	 *
	 * @param tau Time to maturity.
	 * @return Continuously compounded risk-free rate.
	 */
	private double getRiskFreeRate(final double tau) {
		final double safeTau = Math.max(tau, TIME_FLOOR);
		final DiscountCurve curve = model.getRiskFreeCurve();
		return -Math.log(curve.getDiscountFactor(safeTau)) / safeTau;
	}

	/**
	 * Dividend-yield rate extracted from the curve.
	 *
	 * @param assetIndex Asset index.
	 * @param tau Time to maturity.
	 * @return Continuously compounded dividend yield.
	 */
	private double getDividendYieldRate(final int assetIndex, final double tau) {
		final double safeTau = Math.max(tau, TIME_FLOOR);
		final DiscountCurve curve = model.getDividendYieldCurves()[assetIndex];
		return -Math.log(curve.getDiscountFactor(safeTau)) / safeTau;
	}

	/**
	 * Risk-free discount factor.
	 *
	 * @param tau Time to maturity.
	 * @return Risk-free discount factor.
	 */
	private double getRiskFreeDiscountFactor(final double tau) {
		if (tau <= 0.0) {
			return 1.0;
		}
		return model.getRiskFreeCurve().getDiscountFactor(tau);
	}

	/**
	 * Dividend discount factor.
	 *
	 * @param assetIndex Asset index.
	 * @param tau Time to maturity.
	 * @return Dividend discount factor.
	 */
	private double getDividendDiscountFactor(final int assetIndex, final double tau) {
		if (tau <= 0.0) {
			return 1.0;
		}
		return model.getDividendYieldCurves()[assetIndex].getDiscountFactor(tau);
	}
}
