package net.finmath.finitedifference.solvers;

import java.util.function.DoubleUnaryOperator;

import net.finmath.finitedifference.FiniteDifferenceExerciseUtil;
import net.finmath.finitedifference.assetderivativevaluation.models.FiniteDifferenceEquityModel;
import net.finmath.finitedifference.assetderivativevaluation.products.FiniteDifferenceOneDimensionalKnockInProduct;
import net.finmath.finitedifference.grids.SpaceTimeDiscretization;
import net.finmath.modelling.Exercise;
import net.finmath.modelling.products.BarrierType;

/**
 * Direct two-state theta-method solver for 1D knock-in barrier-style products.
 *
 * <p>
 * Regime 0 = not yet activated (barrier not yet hit).
 * Regime 1 = already activated (barrier has been hit).
 * </p>
 *
 * <p>
 * This implementation is matrix-free:
 * </p>
 *
 * <ul>
 * <li>The active regime is solved on the full grid using a tridiagonal theta
 * step.</li>
 * <li>The inactive regime is solved only on the continuation-side subgrid.</li>
 * <li>On the already-hit region and at the barrier interface, the coupling
 * between inactive
 * and active regimes is governed by a {@link TwoStateActivationPolicy}.</li>
 * </ul>
 *
 * <p>
 * Supports European, Bermudan, and American exercise in the active regime.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class FDMThetaMethod1DTwoState implements FDMSolver {

	/**
	 * The epsilon.
	 */
	private static final double EPSILON = 1E-10;

	/**
	 * The model.
	 */
	private final FiniteDifferenceEquityModel model;
	/**
	 * The product.
	 */
	private final FiniteDifferenceOneDimensionalKnockInProduct product;
	/**
	 * The space time discretization.
	 */
	private final SpaceTimeDiscretization spaceTimeDiscretization;
	/**
	 * The exercise.
	 */
	private final Exercise exercise;
	/**
	 * The active boundary provider.
	 */
	private final TwoStateActiveBoundaryProvider activeBoundaryProvider;
	/**
	 * The activation policy.
	 */
	private final TwoStateActivationPolicy activationPolicy;

	/**
	 * Creates a direct two-state theta-method solver for one-dimensional knock-
	 * in products.
	 *
	 * <p>
	 * The solver evolves two coupled value functions:
	 * </p>
	 * <ul>
	 * <li>the <em>inactive</em> regime, representing the contract value before
	 * the barrier has been hit,</li>
	 * <li>the <em>active</em> regime, representing the value after activation,
	 * which behaves like the corresponding activated claim.</li>
	 * </ul>
	 *
	 * <p>
	 * The active regime is solved on the full spatial grid, while the inactive
	 * regime is solved only on the
	 * portion of the grid where the barrier has not yet been triggered. The
	 * already-hit region and the
	 * continuation-side interface are coupled through the default continuation-
	 * style activation policy.
	 * </p>
	 *
	 * @param model The finite-difference equity model providing local PDE
	 *     coefficients and discounting.
	 * @param product The knock-in product to be valued.
	 * @param spaceTimeDiscretization The spatial and temporal discretization,
	 *     including the theta parameter.
	 * @param exercise The exercise specification. Bermudan and American
	 *     exercise are applied only to the active regime.
	 * @param activeBoundaryProvider Provider for the boundary values of the
	 *     already-activated regime.
	 */
	public FDMThetaMethod1DTwoState(
			final FiniteDifferenceEquityModel model,
			final FiniteDifferenceOneDimensionalKnockInProduct product,
			final SpaceTimeDiscretization spaceTimeDiscretization,
			final Exercise exercise,
			final TwoStateActiveBoundaryProvider activeBoundaryProvider) {
		this(
				model,
				product,
				spaceTimeDiscretization,
				exercise,
				activeBoundaryProvider,
				new ContinuationActivationPolicy()
		);
	}

	/**
	 * Creates a direct two-state theta-method solver with an explicit
	 * activation policy.
	 *
	 * <p>
	 * The activation policy controls:
	 * </p>
	 * <ul>
	 *   <li>the inactive value on the already-hit region at maturity,</li>
	 * <li>the inactive value on the already-hit region during backward
	 * stepping,</li>
	 * <li>the interface value seen by the continuation-side inactive PDE at the
	 * barrier.</li>
	 * </ul>
	 *
	 * @param model The finite-difference equity model providing local PDE
	 *     coefficients and discounting.
	 * @param product The knock-in product to be valued.
	 * @param spaceTimeDiscretization The spatial and temporal discretization,
	 *     including the theta parameter.
	 * @param exercise The exercise specification. Bermudan and American
	 *     exercise are applied only to the active regime.
	 * @param activeBoundaryProvider Provider for the boundary values of the
	 *     already-activated regime.
	 * @param activationPolicy Policy governing activation coupling between
	 *     inactive and active regimes.
	 */
	public FDMThetaMethod1DTwoState(
			final FiniteDifferenceEquityModel model,
			final FiniteDifferenceOneDimensionalKnockInProduct product,
			final SpaceTimeDiscretization spaceTimeDiscretization,
			final Exercise exercise,
			final TwoStateActiveBoundaryProvider activeBoundaryProvider,
			final TwoStateActivationPolicy activationPolicy) {

		if (model == null) {
			throw new IllegalArgumentException("Model must not be null.");
		}
		if (product == null) {
			throw new IllegalArgumentException("Product must not be null.");
		}
		if (spaceTimeDiscretization == null) {
			throw new IllegalArgumentException("Space-time discretization must not be null.");
		}
		if (exercise == null) {
			throw new IllegalArgumentException("Exercise must not be null.");
		}
		if (activeBoundaryProvider == null) {
			throw new IllegalArgumentException("Active boundary provider must not be null.");
		}
		if (activationPolicy == null) {
			throw new IllegalArgumentException("Activation policy must not be null.");
		}

		this.model = model;
		this.product = product;
		this.spaceTimeDiscretization = spaceTimeDiscretization;
		this.exercise = exercise;
		this.activeBoundaryProvider = activeBoundaryProvider;
		this.activationPolicy = activationPolicy;
	}

	/**
	 * Solves the two-state backward PDE on the full space-time grid and returns
	 * the inactive-regime value surface.
	 *
	 * <p>
	 * At maturity, the active regime equals the supplied activated payoff,
	 * while the inactive regime equals either
	 * the activation-policy value on the already-hit region or the product's
	 * inactive terminal value on the
	 * not-yet-hit region. The method then steps backward in time:
	 * </p>
	 * <ul>
	 *   <li>solving the activated regime on the full grid,</li>
	 * <li>solving the non-activated regime on the continuation-side
	 * subgrid,</li>
	 * <li>applying the activation policy on the already-hit region and at the
	 * barrier interface.</li>
	 * </ul>
	 *
	 * <p>
	 * The returned surface stores the inactive-regime values only, since these
	 * represent the contract
	 * value prior to barrier activation.
	 * </p>
	 *
	 * @param time The maturity time of the product.
	 * @param valueAtMaturity The terminal payoff of the activated regime as a
	 *     function of the state variable.
	 * @return The inactive-regime solution surface indexed as {@code
	 *     values[spaceIndex][timeIndex]}.
	 */
	@Override
	public double[][] getValues(final double time, final DoubleUnaryOperator valueAtMaturity) {

		if (!exercise.isEuropean() && !exercise.isBermudan() && !exercise.isAmerican()) {
			throw new IllegalArgumentException(
					"FDMThetaMethod1DTwoState currently supports only European, Bermudan, and American exercise.");
		}

		final BarrierType barrierType = product.getBarrierType();
		if (barrierType != BarrierType.DOWN_IN && barrierType != BarrierType.UP_IN) {
			throw new IllegalArgumentException("FDMThetaMethod1DTwoState is only for knock-in barrier options.");
		}

		final double[] xGrid = spaceTimeDiscretization.getSpaceGrid(0).getGrid();
		final int nX = xGrid.length;
		if (nX < 2) {
			throw new IllegalArgumentException("Need at least two grid points.");
		}

		final int barrierIndex = findBarrierIndex(xGrid, product.getBarrierValue());
		final int timeLength = spaceTimeDiscretization.getTimeDiscretization().getNumberOfTimeSteps() + 1;
		final int numberOfTimeSteps = spaceTimeDiscretization.getTimeDiscretization().getNumberOfTimeSteps();

		double[] inactive = new double[nX];
		double[] active = new double[nX];

		for (int i = 0; i < nX; i++) {
			final double x = xGrid[i];
			final double payoff = valueAtMaturity.applyAsDouble(x);

			active[i] = payoff;
			inactive[i] = isAlreadyHitRegion(x, barrierType, product.getBarrierValue())
					? activationPolicy.getAlreadyHitValueAtMaturity(
							x,
							payoff,
							product.getInactiveValueAtMaturity())
					: product.getInactiveValueAtMaturity();
		}

		final double[][] solutionSurface = new double[nX][timeLength];
		for (int i = 0; i < nX; i++) {
			solutionSurface[i][0] = inactive[i];
		}

		for (int m = 0; m < numberOfTimeSteps; m++) {

			final double deltaTau = spaceTimeDiscretization.getTimeDiscretization().getTimeStep(m);

			final double tm = spaceTimeDiscretization.getTimeDiscretization().getTime(numberOfTimeSteps - m);
			final double tmp1 = spaceTimeDiscretization.getTimeDiscretization().getTime(numberOfTimeSteps - (m + 1));

			final double tauMp1 = spaceTimeDiscretization.getTimeDiscretization().getTime(m + 1);
			final double currentTime = spaceTimeDiscretization.getTimeDiscretization().getLastTime() - tauMp1;

			final double lowerActiveBoundary = activeBoundaryProvider.getLowerBoundaryValue(currentTime, xGrid[0]);
			final double upperActiveBoundary = activeBoundaryProvider.getUpperBoundaryValue(currentTime, xGrid[nX - 1]);

			final boolean isExerciseDate =
					FiniteDifferenceExerciseUtil.isExerciseAllowedAtTimeToMaturity(tauMp1, exercise);

			final double[] nextActive;
			if (exercise.isAmerican() && isExerciseDate) {
				nextActive = solveVanillaStepAmerican(
						xGrid,
						active,
						tm,
						tmp1,
						deltaTau,
						lowerActiveBoundary,
						upperActiveBoundary,
						valueAtMaturity
				);
			} else {
				final double[] nextActiveContinuation = solveVanillaStep(
						xGrid,
						active,
						tm,
						tmp1,
						deltaTau,
						lowerActiveBoundary,
						upperActiveBoundary
				);

				if (exercise.isBermudan() && isExerciseDate) {
					nextActive = applyBermudanExerciseProjection(
							nextActiveContinuation,
							xGrid,
							valueAtMaturity,
							lowerActiveBoundary,
							upperActiveBoundary
					);
				} else {
					nextActive = nextActiveContinuation;
				}
			}

			final double[] nextInactive = new double[nX];

			switch (barrierType) {
			case DOWN_IN:
				fillDownInInactiveStep(
						xGrid,
						barrierIndex,
						inactive,
						active,
						nextActive,
						nextInactive,
						tm,
						tmp1,
						deltaTau,
						currentTime);
				break;

			case UP_IN:
				fillUpInInactiveStep(
						xGrid,
						barrierIndex,
						inactive,
						active,
						nextActive,
						nextInactive,
						tm,
						tmp1,
						deltaTau,
						currentTime);
				break;

			default:
				throw new IllegalArgumentException("Unsupported barrier type: " + barrierType);
			}

			active = nextActive;
			inactive = nextInactive;

			for (int i = 0; i < nX; i++) {
				solutionSurface[i][m + 1] = inactive[i];
			}
		}

		return solutionSurface;
	}

	@Override
	public double[][] getValues(final double time, final double[] valueAtMaturity) {

		final double[] xGrid = spaceTimeDiscretization.getSpaceGrid(0).getGrid();

		if (valueAtMaturity.length != xGrid.length) {
			throw new IllegalArgumentException("Terminal vector size does not match grid size.");
		}

		return getValues(time, x -> valueAtMaturity[findGridIndex(xGrid, x)]);
	}

	/**
	 * Returns the inactive-regime value vector at the requested evaluation
	 * time.
	 *
	 * @param evaluationTime The time at which the value vector is requested.
	 * @param time The maturity time of the claim.
	 * @param valueAtMaturity The terminal payoff as a function of the state
	 *     variable.
	 * @return The inactive-regime value vector across the spatial grid at the
	 *     specified evaluation time.
	 */
	@Override
	public double[] getValue(
			final double evaluationTime,
			final double time,
			final DoubleUnaryOperator valueAtMaturity) {

		final double[][] values = getValues(time, valueAtMaturity);
		final double tau = time - evaluationTime;
		final int timeIndex = spaceTimeDiscretization.getTimeDiscretization().getTimeIndexNearestLessOrEqual(tau);

		final double[] column = new double[values.length];
		for (int i = 0; i < values.length; i++) {
			column[i] = values[i][timeIndex];
		}
		return column;
	}

	private void fillDownInInactiveStep(
			final double[] xGrid,
			final int barrierIndex,
			final double[] inactivePrevious,
			final double[] activePrevious,
			final double[] activeNext,
			final double[] inactiveNext,
			final double tm,
			final double tmp1,
			final double deltaTau,
			final double currentTime) {

		for (int i = 0; i <= barrierIndex; i++) {
			inactiveNext[i] = activationPolicy.getAlreadyHitValue(
					currentTime,
					xGrid[i],
					activeNext[i]
			);
		}

		if (barrierIndex == xGrid.length - 1) {
			return;
		}

		final double[] subGrid = sliceGrid(xGrid, barrierIndex, xGrid.length - 1);
		final double[] previousSub = new double[subGrid.length];

		for (int j = 0; j < subGrid.length; j++) {
			previousSub[j] = inactivePrevious[barrierIndex + j];
		}

		previousSub[0] = activationPolicy.getAlreadyHitValue(
				tm,
				xGrid[barrierIndex],
				activePrevious[barrierIndex]
		);

		final double discountedNoHitValue = getDiscountedNoHitValue(currentTime);

		final double interfaceValue = activationPolicy.getInterfaceValue(
				currentTime,
				xGrid[barrierIndex],
				activeNext[barrierIndex]
		);

		final double[] nextSub = solveVanillaStep(
				subGrid,
				previousSub,
				tm,
				tmp1,
				deltaTau,
				interfaceValue,
				discountedNoHitValue);

		for (int j = 0; j < nextSub.length; j++) {
			inactiveNext[barrierIndex + j] = nextSub[j];
		}

		for (int i = 0; i <= barrierIndex; i++) {
			inactiveNext[i] = activationPolicy.getAlreadyHitValue(
					currentTime,
					xGrid[i],
					activeNext[i]
			);
		}
	}

	private void fillUpInInactiveStep(
			final double[] xGrid,
			final int barrierIndex,
			final double[] inactivePrevious,
			final double[] activePrevious,
			final double[] activeNext,
			final double[] inactiveNext,
			final double tm,
			final double tmp1,
			final double deltaTau,
			final double currentTime) {

		for (int i = barrierIndex; i < xGrid.length; i++) {
			inactiveNext[i] = activationPolicy.getAlreadyHitValue(
					currentTime,
					xGrid[i],
					activeNext[i]
			);
		}

		if (barrierIndex == 0) {
			return;
		}

		final double[] subGrid = sliceGrid(xGrid, 0, barrierIndex);
		final double[] previousSub = new double[subGrid.length];

		for (int j = 0; j < subGrid.length; j++) {
			previousSub[j] = inactivePrevious[j];
		}

		previousSub[subGrid.length - 1] = activationPolicy.getAlreadyHitValue(
				tm,
				xGrid[barrierIndex],
				activePrevious[barrierIndex]
		);

		final double discountedNoHitValue = getDiscountedNoHitValue(currentTime);

		final double interfaceValue = activationPolicy.getInterfaceValue(
				currentTime,
				xGrid[barrierIndex],
				activeNext[barrierIndex]
		);

		final double[] nextSub = solveVanillaStep(
				subGrid,
				previousSub,
				tm,
				tmp1,
				deltaTau,
				discountedNoHitValue,
				interfaceValue);

		for (int j = 0; j < nextSub.length; j++) {
			inactiveNext[j] = nextSub[j];
		}

		for (int i = barrierIndex; i < xGrid.length; i++) {
			inactiveNext[i] = activationPolicy.getAlreadyHitValue(
					currentTime,
					xGrid[i],
					activeNext[i]
			);
		}
	}

	private double[] solveVanillaStep(
			final double[] xGrid,
			final double[] previousValues,
			final double tm,
			final double tmp1,
			final double deltaTau,
			final double lowerBoundaryValue,
			final double upperBoundaryValue) {

		final int n = xGrid.length;

		if (n != previousValues.length) {
			throw new IllegalArgumentException("Grid and solution vector size mismatch.");
		}

		if (n == 1) {
			return new double[] {lowerBoundaryValue };
		}

		if (n == 2) {
			return new double[] {lowerBoundaryValue, upperBoundaryValue };
		}

		final double theta = spaceTimeDiscretization.getTheta();

		final ThetaMethod1DAssembly.ModelCoefficients coefficients_m =
				ThetaMethod1DAssembly.buildModelCoefficients(model, xGrid, tm);
		final ThetaMethod1DAssembly.ModelCoefficients coefficients_mp1 =
				ThetaMethod1DAssembly.buildModelCoefficients(model, xGrid, tmp1);

		final TridiagonalMatrix lhs = new TridiagonalMatrix(n);
		final TridiagonalMatrix rhsOperator = new TridiagonalMatrix(n);

		ThetaMethod1DAssembly.buildThetaLeftHandSide(
				lhs,
				xGrid,
				coefficients_mp1.getDrift(),
				coefficients_mp1.getVariance(),
				coefficients_mp1.getShortRate(),
				deltaTau,
				theta);

		ThetaMethod1DAssembly.buildThetaRightHandSide(
				rhsOperator,
				xGrid,
				coefficients_m.getDrift(),
				coefficients_m.getVariance(),
				coefficients_m.getShortRate(),
				deltaTau,
				theta);

		final double[] rhs = ThetaMethod1DAssembly.apply(rhsOperator, previousValues);

		ThetaMethod1DAssembly.overwriteAsDirichlet(lhs, rhs, 0, lowerBoundaryValue);
		ThetaMethod1DAssembly.overwriteAsDirichlet(lhs, rhs, n - 1, upperBoundaryValue);

		final double[] next = ThomasSolver.solve(lhs.getLowerDiagonal(), lhs.getMainDiagonal(), lhs.getUpperDiagonal(), rhs);
		next[0] = lowerBoundaryValue;
		next[n - 1] = upperBoundaryValue;

		return next;
	}

	/**
	 * Returns the discounted value of the inactive terminal amount used at the
	 * outer boundary
	 * of the inactive regime.
	 *
	 * <p>
	 * For classic knock-in barrier options, this is the rebate paid at maturity
	 * if never activated.
	 * For digital knock-ins, this is typically zero.
	 * </p>
	 *
	 * @param currentTime The current model time at which the boundary value is
	 *     required.
	 * @return The discounted inactive terminal value.
	 */
	private double getDiscountedNoHitValue(final double currentTime) {

		if (product.getInactiveValueAtMaturity() == 0.0) {
			return 0.0;
		}

		final double t = Math.max(currentTime, EPSILON);
		final double maturity = product.getMaturity();

		if (t >= maturity) {
			return product.getInactiveValueAtMaturity();
		}

		final double discountFactorAtCurrentTime = model.getRiskFreeCurve().getDiscountFactor(t);
		final double discountFactorAtMaturity = model.getRiskFreeCurve().getDiscountFactor(maturity);

		return product.getInactiveValueAtMaturity() * discountFactorAtMaturity / discountFactorAtCurrentTime;
	}

	private int findBarrierIndex(final double[] grid, final double barrier) {
		for (int i = 0; i < grid.length; i++) {
			if (Math.abs(grid[i] - barrier) < 1E-12) {
				return i;
			}
		}

		throw new IllegalArgumentException(
				"Barrier must coincide with a 1D grid node for direct two-state knock-in pricing.");
	}

	private double[] sliceGrid(final double[] grid, final int startInclusive, final int endInclusive) {
		final double[] result = new double[endInclusive - startInclusive + 1];
		for (int i = 0; i < result.length; i++) {
			result[i] = grid[startInclusive + i];
		}
		return result;
	}

	private boolean isAlreadyHitRegion(
			final double x,
			final BarrierType barrierType,
			final double barrier) {

		switch (barrierType) {
		case DOWN_IN:
			return x <= barrier;
		case UP_IN:
			return x >= barrier;
		default:
			throw new IllegalArgumentException("Unsupported barrier type: " + barrierType);
		}
	}

	private double[] solveVanillaStepAmerican(
			final double[] xGrid,
			final double[] previousValues,
			final double tm,
			final double tmp1,
			final double deltaTau,
			final double lowerBoundaryValue,
			final double upperBoundaryValue,
			final DoubleUnaryOperator exerciseValue) {

		final int n = xGrid.length;

		if (n != previousValues.length) {
			throw new IllegalArgumentException("Grid and solution vector size mismatch.");
		}

		if (n == 1) {
			return new double[] {lowerBoundaryValue };
		}

		if (n == 2) {
			return new double[] {lowerBoundaryValue, upperBoundaryValue };
		}

		final double theta = spaceTimeDiscretization.getTheta();

		final ThetaMethod1DAssembly.ModelCoefficients coefficients_m =
				ThetaMethod1DAssembly.buildModelCoefficients(model, xGrid, tm);
		final ThetaMethod1DAssembly.ModelCoefficients coefficients_mp1 =
				ThetaMethod1DAssembly.buildModelCoefficients(model, xGrid, tmp1);

		final TridiagonalMatrix lhs = new TridiagonalMatrix(n);
		final TridiagonalMatrix rhsOperator = new TridiagonalMatrix(n);

		ThetaMethod1DAssembly.buildThetaLeftHandSide(
				lhs,
				xGrid,
				coefficients_mp1.getDrift(),
				coefficients_mp1.getVariance(),
				coefficients_mp1.getShortRate(),
				deltaTau,
				theta);

		ThetaMethod1DAssembly.buildThetaRightHandSide(
				rhsOperator,
				xGrid,
				coefficients_m.getDrift(),
				coefficients_m.getVariance(),
				coefficients_m.getShortRate(),
				deltaTau,
				theta);

		final double[] rhs = ThetaMethod1DAssembly.apply(rhsOperator, previousValues);

		ThetaMethod1DAssembly.overwriteAsDirichlet(lhs, rhs, 0, lowerBoundaryValue);
		ThetaMethod1DAssembly.overwriteAsDirichlet(lhs, rhs, n - 1, upperBoundaryValue);

		final double[] obstacle = buildActiveObstacleVector(
				xGrid,
				exerciseValue,
				lowerBoundaryValue,
				upperBoundaryValue
		);

		final double[] next = ProjectedTridiagonalSOR.solve(
				lhs,
				rhs,
				obstacle,
				previousValues,
				1.2,
				500,
				1E-10
		);

		next[0] = lowerBoundaryValue;
		next[n - 1] = upperBoundaryValue;

		return next;
	}

	private double[] applyBermudanExerciseProjection(
			final double[] continuationValues,
			final double[] xGrid,
			final DoubleUnaryOperator exerciseValue,
			final double lowerBoundaryValue,
			final double upperBoundaryValue) {

		final double[] exercisedValues = continuationValues.clone();

		for (int i = 0; i < exercisedValues.length; i++) {
			if (i == 0) {
				exercisedValues[i] = lowerBoundaryValue;
			} else if (i == exercisedValues.length - 1) {
				exercisedValues[i] = upperBoundaryValue;
			} else {
				exercisedValues[i] = Math.max(exercisedValues[i], exerciseValue.applyAsDouble(xGrid[i]));
			}
		}

		return exercisedValues;
	}

	private double[] buildActiveObstacleVector(
			final double[] xGrid,
			final DoubleUnaryOperator exerciseValue,
			final double lowerBoundaryValue,
			final double upperBoundaryValue) {

		final double[] obstacle = new double[xGrid.length];

		for (int i = 0; i < xGrid.length; i++) {
			if (i == 0) {
				obstacle[i] = lowerBoundaryValue;
			} else if (i == xGrid.length - 1) {
				obstacle[i] = upperBoundaryValue;
			} else {
				obstacle[i] = exerciseValue.applyAsDouble(xGrid[i]);
			}
		}

		return obstacle;
	}

	private int findGridIndex(final double[] grid, final double value) {
		for (int i = 0; i < grid.length; i++) {
			if (Math.abs(grid[i] - value) < 1E-12) {
				return i;
			}
		}
		throw new IllegalArgumentException("Point does not coincide with a grid node.");
	}
}
