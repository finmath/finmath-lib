package net.finmath.finitedifference.assetderivativevaluation.boundaries;

import net.finmath.finitedifference.assetderivativevaluation.models.FDMMultiAssetBlackScholesModel;
import net.finmath.finitedifference.assetderivativevaluation.products.BasketOption;
import net.finmath.finitedifference.assetderivativevaluation.products.FiniteDifferenceEquityProduct;
import net.finmath.finitedifference.boundaries.BoundaryCondition;
import net.finmath.finitedifference.boundaries.StandardBoundaryCondition;
import net.finmath.functions.AnalyticFormulas;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.modelling.products.CallOrPut;

/**
 * Boundary conditions for {@link BasketOption} under
 * {@link FDMMultiAssetBlackScholesModel}.
 *
 * <p>
 * The product payoff is
 * </p>
 *
 * <p>
 * <i>
 * \left( \omega \left( q_1 S_1(T) + q_2 S_2(T) - K \right) \right)^{+},
 * </i>
 * </p>
 *
 * <p>
 * where {@code q1} and {@code q2} are signed asset quantities and
 * {@code \omega \in \{+1,-1\}} is the call/put sign.
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
 * On the lower faces {@code S1 = 0} and {@code S2 = 0}, the option reduces to
 * a one-dimensional signed-quantity option on the remaining asset. Accordingly,
 * this boundary class requires both asset grids to start at zero. On upper
 * faces, the boundary value is chosen according to the sign of the quantity
 * multiplying the boundary asset:
 * </p>
 * <ul>
 * <li>if {@code callOrPut.toInteger() * quantityBoundaryAsset > 0}, the option
 * is
 *       asymptotically linear in the boundary asset and the discounted linear
 *       intrinsic approximation is used,</li>
 * <li>if {@code callOrPut.toInteger() * quantityBoundaryAsset < 0}, the option
 * tends to zero as the boundary asset grows and a zero Dirichlet value is
 * used,</li>
 * <li>if the boundary-asset quantity is zero, the boundary reduces exactly to a
 *       one-dimensional option on the remaining asset.</li>
 * </ul>
 *
 * <p>
 * This single boundary class therefore covers ordinary basket options, spread
 * options, and exchange options as special cases.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class BasketOptionMultiAssetBlackScholesModelBoundary implements FiniteDifferenceBoundary {

	/**
	 * The time floor.
	 */
	private static final double TIME_FLOOR = 1E-10;
	/**
	 * The quantity tolerance.
	 */
	private static final double QUANTITY_TOLERANCE = 1E-14;
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
	public BasketOptionMultiAssetBlackScholesModelBoundary(final FDMMultiAssetBlackScholesModel model) {
		if (model == null) {
			throw new IllegalArgumentException("model must not be null.");
		}
		if (model.getInitialValue() == null || model.getInitialValue().length != 2) {
			throw new IllegalArgumentException(
					"BasketOptionMultiAssetBlackScholesModelBoundary requires a two-dimensional model.");
		}

		final double[] firstGrid = model.getSpaceTimeDiscretization().getSpaceGrid(0).getGrid();
		final double[] secondGrid = model.getSpaceTimeDiscretization().getSpaceGrid(1).getGrid();

		if (Math.abs(firstGrid[0]) > GRID_TOLERANCE || Math.abs(secondGrid[0]) > GRID_TOLERANCE) {
			throw new IllegalArgumentException(
					"BasketOptionMultiAssetBlackScholesModelBoundary requires both asset grids to start at 0. "
							+ "The lower-face one-dimensional reduction is exact only at S1 = 0 and S2 = 0.");
		}

		this.model = model;
	}

	@Override
	public BoundaryCondition[] getBoundaryConditionsAtLowerBoundary(
			final FiniteDifferenceEquityProduct product,
			final double time,
			final double... stateVariables) {

		final BasketOption basketOption = validateAndCastProduct(product);
		validateStateVariables(stateVariables);

		final double[] quantities = basketOption.getQuantities();
		final double strike = basketOption.getStrike();
		final double tau = Math.max(basketOption.getMaturity() - time, 0.0);

		final double s1 = stateVariables[0];
		final double s2 = stateVariables[1];

		final BoundaryCondition[] conditions = new BoundaryCondition[] {
				StandardBoundaryCondition.none(),
				StandardBoundaryCondition.none()
		};

		if (isAtLowerBoundary(0, s1)) {
			conditions[0] = StandardBoundaryCondition.dirichlet(
					getReducedOneDimensionalValue(
							basketOption.getCallOrPut(),
							quantities[1],
							s2,
							strike - quantities[0] * s1,
							1,
							tau
							)
					);
		}

		if (isAtLowerBoundary(1, s2)) {
			conditions[1] = StandardBoundaryCondition.dirichlet(
					getReducedOneDimensionalValue(
							basketOption.getCallOrPut(),
							quantities[0],
							s1,
							strike - quantities[1] * s2,
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

		final BasketOption basketOption = validateAndCastProduct(product);
		validateStateVariables(stateVariables);

		final double[] quantities = basketOption.getQuantities();
		final double strike = basketOption.getStrike();
		final double tau = Math.max(basketOption.getMaturity() - time, 0.0);

		final double s1 = stateVariables[0];
		final double s2 = stateVariables[1];

		final BoundaryCondition[] conditions = new BoundaryCondition[] {
				StandardBoundaryCondition.none(),
				StandardBoundaryCondition.none()
		};

		if (isAtUpperBoundary(0, s1)) {
			conditions[0] = StandardBoundaryCondition.dirichlet(
					getUpperFaceValue(
							basketOption.getCallOrPut(),
							quantities,
							strike,
							s1,
							s2,
							0,
							tau
							)
					);
		}

		if (isAtUpperBoundary(1, s2)) {
			conditions[1] = StandardBoundaryCondition.dirichlet(
					getUpperFaceValue(
							basketOption.getCallOrPut(),
							quantities,
							strike,
							s1,
							s2,
							1,
							tau
							)
					);
		}

		return conditions;
	}

	private BasketOption validateAndCastProduct(final FiniteDifferenceEquityProduct product) {
		if (!(product instanceof BasketOption)) {
			throw new IllegalArgumentException(
					"BasketOptionMultiAssetBlackScholesModelBoundary requires a BasketOption.");
		}

		final BasketOption basketOption = (BasketOption) product;

		if (basketOption.getQuantities().length != 2) {
			throw new IllegalArgumentException(
					"BasketOptionMultiAssetBlackScholesModelBoundary currently supports only two assets.");
		}
		if (!basketOption.getExercise().isEuropean()) {
			throw new IllegalArgumentException(
					"BasketOptionMultiAssetBlackScholesModelBoundary currently supports only European exercise.");
		}

		return basketOption;
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

	private double getUpperFaceValue(
			final CallOrPut callOrPut,
			final double[] quantities,
			final double strike,
			final double s1,
			final double s2,
			final int boundaryAssetIndex,
			final double tau) {

		final double quantityBoundaryAsset = quantities[boundaryAssetIndex];
		final int sign = callOrPut.toInteger();

		if (Math.abs(quantityBoundaryAsset) <= QUANTITY_TOLERANCE) {
			if (boundaryAssetIndex == 0) {
				return getReducedOneDimensionalValue(
						callOrPut,
						quantities[1],
						s2,
						strike - quantities[0] * s1,
						1,
						tau
						);
			}
			return getReducedOneDimensionalValue(
					callOrPut,
					quantities[0],
					s1,
					strike - quantities[1] * s2,
					0,
					tau
					);
		}

		if (sign * quantityBoundaryAsset < 0.0) {
			return 0.0;
		}

		final double discountedLinearCombination =
				quantities[0] * s1 * getDividendDiscountFactor(0, tau)
				+ quantities[1] * s2 * getDividendDiscountFactor(1, tau)
				- strike * getRiskFreeDiscountFactor(tau);

		return Math.max(sign * discountedLinearCombination, 0.0);
	}

	private double getReducedOneDimensionalValue(
			final CallOrPut callOrPut,
			final double quantity,
			final double spot,
			final double adjustedStrike,
			final int assetIndex,
			final double tau) {

		final int sign = callOrPut.toInteger();

		if (Math.abs(quantity) <= QUANTITY_TOLERANCE) {
			return Math.max(-sign * adjustedStrike, 0.0) * getRiskFreeDiscountFactor(tau);
		}

		if (quantity > 0.0) {
			final double effectiveStrike = adjustedStrike / quantity;

			if (sign > 0) {
				if (effectiveStrike <= 0.0) {
					return quantity * spot * getDividendDiscountFactor(assetIndex, tau)
							- adjustedStrike * getRiskFreeDiscountFactor(tau);
				}
				return quantity * getBlackScholesCallValue(
						spot,
						effectiveStrike,
						assetIndex,
						tau
						);
			}

			if (effectiveStrike <= 0.0) {
				return 0.0;
			}
			return quantity * getBlackScholesPutValue(
					spot,
					effectiveStrike,
					assetIndex,
					tau
					);
		}

		final double absoluteQuantity = -quantity;
		final double transformedStrike = -adjustedStrike / absoluteQuantity;

		if (sign > 0) {
			if (transformedStrike <= 0.0) {
				return 0.0;
			}
			return absoluteQuantity * getBlackScholesPutValue(
					spot,
					transformedStrike,
					assetIndex,
					tau
					);
		}

		if (transformedStrike <= 0.0) {
			return absoluteQuantity * spot * getDividendDiscountFactor(assetIndex, tau)
					+ adjustedStrike * getRiskFreeDiscountFactor(tau);
		}
		return absoluteQuantity * getBlackScholesCallValue(
				spot,
				transformedStrike,
				assetIndex,
				tau
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
