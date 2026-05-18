package net.finmath.finitedifference.assetderivativevaluation.boundaries;

import net.finmath.finitedifference.assetderivativevaluation.models.FDMMultiAssetBlackScholesModel;
import net.finmath.finitedifference.assetderivativevaluation.products.DigitalBasketOption;
import net.finmath.finitedifference.assetderivativevaluation.products.FiniteDifferenceEquityProduct;
import net.finmath.finitedifference.boundaries.BoundaryCondition;
import net.finmath.finitedifference.boundaries.StandardBoundaryCondition;
import net.finmath.functions.AnalyticFormulas;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.modelling.products.CallOrPut;

/**
 * Boundary conditions for {@link DigitalBasketOption} under
 * {@link FDMMultiAssetBlackScholesModel}.
 *
 * <p>
 * The class covers the three digital payoff families supported by
 * {@link DigitalBasketOption}:
 * </p>
 * <ul>
 *   <li>linear-combination digital,</li>
 *   <li>best-of digital,</li>
 *   <li>worst-of digital.</li>
 * </ul>
 *
 * <p>
 * The lower-face reductions are exact and therefore this class requires both
 * asset grids to start at zero.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class DigitalBasketOptionMultiAssetBlackScholesModelBoundary implements FiniteDifferenceBoundary {

	/**
	 * The time floor.
	 */
	private static final double TIME_FLOOR = 1E-10;
	/**
	 * The grid tolerance.
	 */
	private static final double GRID_TOLERANCE = 1E-12;
	/**
	 * The quantity tolerance.
	 */
	private static final double QUANTITY_TOLERANCE = 1E-14;
	/**
	 * The strike tolerance.
	 */
	private static final double STRIKE_TOLERANCE = 1E-14;

	/**
	 * The model.
	 */
	private final FDMMultiAssetBlackScholesModel model;

	/**
	 * Creates the boundary class.
	 *
	 * @param model The underlying finite-difference model.
	 */
	public DigitalBasketOptionMultiAssetBlackScholesModelBoundary(final FDMMultiAssetBlackScholesModel model) {
		if (model == null) {
			throw new IllegalArgumentException("model must not be null.");
		}
		if (model.getInitialValue() == null || model.getInitialValue().length != 2) {
			throw new IllegalArgumentException(
					"DigitalBasketOptionMultiAssetBlackScholesModelBoundary requires a two-dimensional model.");
		}

		final double[] firstGrid = model.getSpaceTimeDiscretization().getSpaceGrid(0).getGrid();
		final double[] secondGrid = model.getSpaceTimeDiscretization().getSpaceGrid(1).getGrid();

		if (Math.abs(firstGrid[0]) > GRID_TOLERANCE || Math.abs(secondGrid[0]) > GRID_TOLERANCE) {
			throw new IllegalArgumentException(
					"DigitalBasketOptionMultiAssetBlackScholesModelBoundary requires both asset grids to start at 0. "
							+ "The lower-face reductions are exact only at S1 = 0 and S2 = 0.");
		}

		this.model = model;
	}

	@Override
	public BoundaryCondition[] getBoundaryConditionsAtLowerBoundary(
			final FiniteDifferenceEquityProduct product,
			final double time,
			final double... stateVariables) {

		final DigitalBasketOption digitalBasketOption = validateAndCastProduct(product);
		validateStateVariables(stateVariables);

		final double tau = Math.max(digitalBasketOption.getMaturity() - time, 0.0);
		final double s1 = stateVariables[0];
		final double s2 = stateVariables[1];

		final BoundaryCondition[] conditions = new BoundaryCondition[] {
				StandardBoundaryCondition.none(),
				StandardBoundaryCondition.none()
		};

		switch (digitalBasketOption.getBasketDigitalType()) {
		case LINEAR_COMBINATION:
			final double[] quantities = digitalBasketOption.getQuantities();

			if (isAtLowerBoundary(0, s1)) {
				conditions[0] = StandardBoundaryCondition.dirichlet(
						getReducedLinearCombinationDigitalValue(
								digitalBasketOption.getCallOrPut(),
								quantities[1],
								s2,
								digitalBasketOption.getStrike() - quantities[0] * s1,
								1,
								tau,
								digitalBasketOption.getCashPayoff()
								)
						);
			}

			if (isAtLowerBoundary(1, s2)) {
				conditions[1] = StandardBoundaryCondition.dirichlet(
						getReducedLinearCombinationDigitalValue(
								digitalBasketOption.getCallOrPut(),
								quantities[0],
								s1,
								digitalBasketOption.getStrike() - quantities[1] * s2,
								0,
								tau,
								digitalBasketOption.getCashPayoff()
								)
						);
			}
			break;

		case BEST_OF:
			if (isAtLowerBoundary(0, s1)) {
				conditions[0] = StandardBoundaryCondition.dirichlet(
						getReducedVanillaDigitalValue(
								digitalBasketOption.getCallOrPut(),
								s2,
								digitalBasketOption.getStrike(),
								1,
								tau,
								digitalBasketOption.getCashPayoff()
								)
						);
			}

			if (isAtLowerBoundary(1, s2)) {
				conditions[1] = StandardBoundaryCondition.dirichlet(
						getReducedVanillaDigitalValue(
								digitalBasketOption.getCallOrPut(),
								s1,
								digitalBasketOption.getStrike(),
								0,
								tau,
								digitalBasketOption.getCashPayoff()
								)
						);
			}
			break;

		case WORST_OF:
			final double deterministicLowerFaceValue = getDeterministicDigitalValue(
					digitalBasketOption.getCallOrPut(),
					0.0,
					digitalBasketOption.getStrike(),
					tau,
					digitalBasketOption.getCashPayoff()
					);

			if (isAtLowerBoundary(0, s1)) {
				conditions[0] = StandardBoundaryCondition.dirichlet(deterministicLowerFaceValue);
			}

			if (isAtLowerBoundary(1, s2)) {
				conditions[1] = StandardBoundaryCondition.dirichlet(deterministicLowerFaceValue);
			}
			break;

		default:
			throw new IllegalStateException("Unsupported basket digital type.");
		}

		return conditions;
	}

	@Override
	public BoundaryCondition[] getBoundaryConditionsAtUpperBoundary(
			final FiniteDifferenceEquityProduct product,
			final double time,
			final double... stateVariables) {

		final DigitalBasketOption digitalBasketOption = validateAndCastProduct(product);
		validateStateVariables(stateVariables);

		final double tau = Math.max(digitalBasketOption.getMaturity() - time, 0.0);
		final double s1 = stateVariables[0];
		final double s2 = stateVariables[1];

		final BoundaryCondition[] conditions = new BoundaryCondition[] {
				StandardBoundaryCondition.none(),
				StandardBoundaryCondition.none()
		};

		switch (digitalBasketOption.getBasketDigitalType()) {
		case LINEAR_COMBINATION:
			final double[] quantities = digitalBasketOption.getQuantities();

			if (isAtUpperBoundary(0, s1)) {
				conditions[0] = StandardBoundaryCondition.dirichlet(
						getReducedLinearCombinationDigitalValue(
								digitalBasketOption.getCallOrPut(),
								quantities[1],
								s2,
								digitalBasketOption.getStrike() - quantities[0] * s1,
								1,
								tau,
								digitalBasketOption.getCashPayoff()
								)
						);
			}

			if (isAtUpperBoundary(1, s2)) {
				conditions[1] = StandardBoundaryCondition.dirichlet(
						getReducedLinearCombinationDigitalValue(
								digitalBasketOption.getCallOrPut(),
								quantities[0],
								s1,
								digitalBasketOption.getStrike() - quantities[1] * s2,
								0,
								tau,
								digitalBasketOption.getCashPayoff()
								)
						);
			}
			break;

		case BEST_OF:
			if (isAtUpperBoundary(0, s1)) {
				conditions[0] = StandardBoundaryCondition.dirichlet(
						getBestOfUpperFaceValue(
								digitalBasketOption.getCallOrPut(),
								s1,
								s2,
								digitalBasketOption.getStrike(),
								1,
								tau,
								digitalBasketOption.getCashPayoff()
								)
						);
			}

			if (isAtUpperBoundary(1, s2)) {
				conditions[1] = StandardBoundaryCondition.dirichlet(
						getBestOfUpperFaceValue(
								digitalBasketOption.getCallOrPut(),
								s2,
								s1,
								digitalBasketOption.getStrike(),
								0,
								tau,
								digitalBasketOption.getCashPayoff()
								)
						);
			}
			break;

		case WORST_OF:
			if (isAtUpperBoundary(0, s1)) {
				conditions[0] = StandardBoundaryCondition.dirichlet(
						getWorstOfUpperFaceValue(
								digitalBasketOption.getCallOrPut(),
								s1,
								s2,
								digitalBasketOption.getStrike(),
								1,
								tau,
								digitalBasketOption.getCashPayoff()
								)
						);
			}

			if (isAtUpperBoundary(1, s2)) {
				conditions[1] = StandardBoundaryCondition.dirichlet(
						getWorstOfUpperFaceValue(
								digitalBasketOption.getCallOrPut(),
								s2,
								s1,
								digitalBasketOption.getStrike(),
								0,
								tau,
								digitalBasketOption.getCashPayoff()
								)
						);
			}
			break;

		default:
			throw new IllegalStateException("Unsupported basket digital type.");
		}

		return conditions;
	}

	private DigitalBasketOption validateAndCastProduct(final FiniteDifferenceEquityProduct product) {
		if (!(product instanceof DigitalBasketOption)) {
			throw new IllegalArgumentException(
					"DigitalBasketOptionMultiAssetBlackScholesModelBoundary requires a DigitalBasketOption.");
		}

		final DigitalBasketOption digitalBasketOption = (DigitalBasketOption) product;

		if (!digitalBasketOption.getExercise().isEuropean()) {
			throw new IllegalArgumentException(
					"DigitalBasketOptionMultiAssetBlackScholesModelBoundary currently supports only European exercise.");
		}

		return digitalBasketOption;
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

	private double getReducedLinearCombinationDigitalValue(
			final CallOrPut callOrPut,
			final double quantity,
			final double spot,
			final double adjustedStrike,
			final int assetIndex,
			final double tau,
			final double cashPayoff) {

		final int sign = callOrPut.toInteger();

		if (Math.abs(quantity) <= QUANTITY_TOLERANCE) {
			return getDeterministicDigitalValue(callOrPut, 0.0, adjustedStrike, tau, cashPayoff);
		}

		if (quantity > 0.0) {
			final double effectiveStrike = adjustedStrike / quantity;

			if (sign > 0) {
				return getCashOrNothingCallValue(spot, effectiveStrike, assetIndex, tau, cashPayoff);
			}

			return getCashOrNothingPutValue(spot, effectiveStrike, assetIndex, tau, cashPayoff);
		}

		final double absoluteQuantity = -quantity;
		final double transformedStrike = -adjustedStrike / absoluteQuantity;

		if (sign > 0) {
			return getCashOrNothingPutValue(spot, transformedStrike, assetIndex, tau, cashPayoff);
		}

		return getCashOrNothingCallValue(spot, transformedStrike, assetIndex, tau, cashPayoff);
	}

	private double getReducedVanillaDigitalValue(
			final CallOrPut callOrPut,
			final double spot,
			final double strike,
			final int assetIndex,
			final double tau,
			final double cashPayoff) {

		if (callOrPut == CallOrPut.CALL) {
			return getCashOrNothingCallValue(spot, strike, assetIndex, tau, cashPayoff);
		}

		return getCashOrNothingPutValue(spot, strike, assetIndex, tau, cashPayoff);
	}

	private double getBestOfUpperFaceValue(
			final CallOrPut callOrPut,
			final double boundarySpot,
			final double remainingSpot,
			final double strike,
			final int remainingAssetIndex,
			final double tau,
			final double cashPayoff) {

		if (callOrPut == CallOrPut.CALL) {
			if (boundarySpot > strike) {
				return cashPayoff * getRiskFreeDiscountFactor(tau);
			}

			return getCashOrNothingCallValue(
					remainingSpot,
					strike,
					remainingAssetIndex,
					tau,
					cashPayoff
					);
		}

		if (boundarySpot >= strike) {
			return 0.0;
		}

		return getCashOrNothingPutValue(
				remainingSpot,
				strike,
				remainingAssetIndex,
				tau,
				cashPayoff
				);
	}

	private double getWorstOfUpperFaceValue(
			final CallOrPut callOrPut,
			final double boundarySpot,
			final double remainingSpot,
			final double strike,
			final int remainingAssetIndex,
			final double tau,
			final double cashPayoff) {

		if (callOrPut == CallOrPut.CALL) {
			if (boundarySpot <= strike) {
				return 0.0;
			}

			return getCashOrNothingCallValue(
					remainingSpot,
					strike,
					remainingAssetIndex,
					tau,
					cashPayoff
					);
		}

		if (boundarySpot < strike) {
			return cashPayoff * getRiskFreeDiscountFactor(tau);
		}

		return getCashOrNothingPutValue(
				remainingSpot,
				strike,
				remainingAssetIndex,
				tau,
				cashPayoff
				);
	}

	private double getDeterministicDigitalValue(
			final CallOrPut callOrPut,
			final double underlyingValue,
			final double strike,
			final double tau,
			final double cashPayoff) {

		return callOrPut.toInteger() * (underlyingValue - strike) > 0.0
				? cashPayoff * getRiskFreeDiscountFactor(tau)
						: 0.0;
	}

	private double getCashOrNothingCallValue(
			final double spot,
			final double strike,
			final int assetIndex,
			final double tau,
			final double cashPayoff) {

		if (tau <= 0.0) {
			return spot > strike ? cashPayoff : 0.0;
		}

		if (strike < -STRIKE_TOLERANCE) {
			return cashPayoff * getRiskFreeDiscountFactor(tau);
		}

		if (Math.abs(strike) <= STRIKE_TOLERANCE) {
			return spot > 0.0 ? cashPayoff * getRiskFreeDiscountFactor(tau) : 0.0;
		}

		if (spot <= 0.0) {
			return 0.0;
		}

		final double riskFreeRate = getRiskFreeRate(tau);
		final double dividendYield = getDividendYieldRate(assetIndex, tau);
		final double forward = spot * Math.exp((riskFreeRate - dividendYield) * tau);
		final double payoffUnit = Math.exp(-riskFreeRate * tau);

		return cashPayoff
				* payoffUnit
				* AnalyticFormulas.blackScholesDigitalOptionValue(
						forward,
						0.0,
						model.getVolatilities()[assetIndex],
						tau,
						strike
						);
	}

	private double getCashOrNothingPutValue(
			final double spot,
			final double strike,
			final int assetIndex,
			final double tau,
			final double cashPayoff) {

		if (tau <= 0.0) {
			return spot < strike ? cashPayoff : 0.0;
		}

		if (strike <= STRIKE_TOLERANCE) {
			return 0.0;
		}

		if (spot <= 0.0) {
			return cashPayoff * getRiskFreeDiscountFactor(tau);
		}

		return cashPayoff * getRiskFreeDiscountFactor(tau)
				- getCashOrNothingCallValue(spot, strike, assetIndex, tau, cashPayoff);
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
}
