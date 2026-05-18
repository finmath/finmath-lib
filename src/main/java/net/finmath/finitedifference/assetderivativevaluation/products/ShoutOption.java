package net.finmath.finitedifference.assetderivativevaluation.products;

import java.util.Arrays;
import java.util.function.DoubleBinaryOperator;

import net.finmath.finitedifference.assetderivativevaluation.models.FiniteDifferenceEquityModel;
import net.finmath.finitedifference.grids.SpaceTimeDiscretization;
import net.finmath.finitedifference.solvers.FDMSolver;
import net.finmath.finitedifference.solvers.FDMSolverFactory;
import net.finmath.finitedifference.solvers.FDMThetaMethod1D;
import net.finmath.finitedifference.solvers.adi.AbstractADI2D;
import net.finmath.modelling.EuropeanExercise;
import net.finmath.modelling.products.CallOrPut;

/**
 * Finite-difference valuation of a shout option.
 *
 * <p>
 * A shout option is a path-dependent extension of a vanilla option allowing the
 * holder to
 * lock in intrinsic value a finite number of times before maturity. In this
 * implementation,
 * the locked level is represented by a reset strike <i>K</i><sup>*</sup>, and
 * the standard
 * shout rule
 * </p>
 *
 * <p>
 * <i>K</i><sup>*</sup> = <i>S</i>
 * </p>
 *
 * <p>
 * is used at each shout time. The contract considered here has fixed maturity
 * <i>T</i>, a finite maximum number of shouts, continuous shout right, optional
 * constant
 * shout cash adjustment, and no maturity extension or yearly counter reset.
 * </p>
 *
 * <p>
 * For a fixed locked strike <i>K</i>, the terminal payoff is that of a vanilla
 * option,
 * namely
 * </p>
 *
 * <p>
 * <i>&Phi;(S(T);K) = max(&omega;(S(T)-K),0)</i>,
 * </p>
 *
 * <p>
 * where <i>&omega; = 1</i> for a call and <i>&omega; = -1</i> for a put.
 * </p>
 *
 * <p>
 * Let <i>V</i><sup>(u)</sup>(<i>t,S;K</i>) denote the value when <i>u</i>
 * shouts have already
 * been used and the current locked strike is <i>K</i>. The shout feature
 * induces the obstacle
 * recursion
 * </p>
 *
 * <p>
 * <i>V</i><sup>(u)</sup>(<i>t,S;K</i>) =
 * max(
 * continuation,
 * <i>V</i><sup>(u+1)</sup>(<i>t,S;S</i>) + c
 * ),
 * </p>
 *
 * <p>
 * where <i>c</i> is the constant shout cash adjustment. Hence the valuation
 * proceeds
 * backward over planes indexed by the number of used shouts and, inside each
 * plane,
 * over slices corresponding to fixed strike values taken from a strike grid.
 * </p>
 *
 * <p>
 * The implementation adapts to the present framework the ideas of H. Windcliff,
 * K.R. Vetzal, P.A. Forsyth, A. Verma, T.F. Coleman,
 * <i>An object-oriented framework for valuing shout options on high-performance
 * computer architectures</i>,
 * JEDC.
 * </p>
 *
 * <p>
 * The recursion is performed over planes of used shout count and slices of
 * fixed strike.
 * Each slice is a standard PDE solve with a continuous obstacle interpolated
 * from the next
 * shout plane.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class ShoutOption implements FiniteDifferenceEquityProduct {

	/**
	 * The underlying name.
	 */
	private final String underlyingName;
	/**
	 * The maturity.
	 */
	private final double maturity;
	/**
	 * The initial strike.
	 */
	private final double initialStrike;
	/**
	 * The strike grid.
	 */
	private final double[] strikeGrid;
	/**
	 * The maximum number of shouts.
	 */
	private final int maximumNumberOfShouts;
	/**
	 * The call or put.
	 */
	private final CallOrPut callOrPut;
	/**
	 * The shout cash adjustment.
	 */
	private final double shoutCashAdjustment;

	/**
	 * Creates a shout option.
	 *
	 * @param underlyingName Name of the underlying. May be {@code null}.
	 * @param maturity Option maturity.
	 * @param initialStrike Initial strike.
	 * @param strikeGrid Grid of strikes used for the locked-strike recursion.
	 * @param maximumNumberOfShouts Maximum number of shouts.
	 * @param callOrPut Call/put flag.
	 * @param shoutCashAdjustment Constant cash adjustment added upon shout.
	 */
	public ShoutOption(
			final String underlyingName,
			final double maturity,
			final double initialStrike,
			final double[] strikeGrid,
			final int maximumNumberOfShouts,
			final CallOrPut callOrPut,
			final double shoutCashAdjustment) {

		if (callOrPut == null) {
			throw new IllegalArgumentException("callOrPut must not be null.");
		}
		if (maturity < 0.0) {
			throw new IllegalArgumentException("maturity must be non-negative.");
		}
		if (initialStrike <= 0.0) {
			throw new IllegalArgumentException("initialStrike must be positive.");
		}
		if (strikeGrid == null || strikeGrid.length < 2) {
			throw new IllegalArgumentException("strikeGrid must contain at least two points.");
		}
		if (maximumNumberOfShouts < 0) {
			throw new IllegalArgumentException("maximumNumberOfShouts must be non-negative.");
		}

		this.underlyingName = underlyingName;
		this.maturity = maturity;
		this.initialStrike = initialStrike;
		this.strikeGrid = strikeGrid.clone();
		this.maximumNumberOfShouts = maximumNumberOfShouts;
		this.callOrPut = callOrPut;
		this.shoutCashAdjustment = shoutCashAdjustment;

		validateStrikeGrid();
	}

	/**
	 * Creates a shout option with zero shout cash adjustment.
	 *
	 * @param maturity Option maturity.
	 * @param initialStrike Initial strike.
	 * @param strikeGrid Grid of strikes used for the locked-strike recursion.
	 * @param maximumNumberOfShouts Maximum number of shouts.
	 * @param callOrPut Call/put flag.
	 */
	public ShoutOption(
			final double maturity,
			final double initialStrike,
			final double[] strikeGrid,
			final int maximumNumberOfShouts,
			final CallOrPut callOrPut) {
		this(
				null,
				maturity,
				initialStrike,
				strikeGrid,
				maximumNumberOfShouts,
				callOrPut,
				0.0
				);
	}

	/**
	 * Returns the value at the specified evaluation time.
	 *
	 * @param evaluationTime Evaluation time.
	 * @param model The finite-difference model.
	 * @return The value vector on the model space grid.
	 */
	@Override
	public double[] getValue(final double evaluationTime, final FiniteDifferenceEquityModel model) {
		final double[][] values = getValues(model);

		final SpaceTimeDiscretization valuationDiscretization = model.getSpaceTimeDiscretization();
		final double tau = maturity - evaluationTime;
		final int timeIndex = valuationDiscretization.getTimeDiscretization()
				.getTimeIndexNearestLessOrEqual(tau);

		final double[] column = new double[values.length];
		for (int i = 0; i < values.length; i++) {
			column[i] = values[i][timeIndex];
		}
		return column;
	}

	/**
	 * Returns the full value surface.
	 *
	 * @param model The finite-difference model.
	 * @return The value surface indexed by space point and time index.
	 */
	@Override
	public double[][] getValues(final FiniteDifferenceEquityModel model) {
		validateModel(model);

		final int dimensions = model.getSpaceTimeDiscretization().getNumberOfSpaceGrids();

		if (maximumNumberOfShouts == 0) {
			return createVanillaSliceProduct(initialStrike).getValues(model);
		}

		if (dimensions == 1) {
			return getValues1D(model);
		} else if (dimensions == 2) {
			return getValues2D(model);
		} else {
			throw new IllegalArgumentException("ShoutOption currently supports only 1D and 2D models.");
		}
	}

	private double[][] getValues1D(final FiniteDifferenceEquityModel model) {
		final SpaceTimeDiscretization discretization = model.getSpaceTimeDiscretization();
		final double[] xGrid = discretization.getSpaceGrid(0).getGrid();

		validateInitialStrikeInsideGrid();
		validateStrikeGridCoversResetRange(xGrid);

		double[][][] nextPlane = solveLastPlane1D(model);

		for (int usedShouts = maximumNumberOfShouts - 1; usedShouts >= 0; usedShouts--) {
			final double[][][] currentPlane = new double[strikeGrid.length][][];

			for (int strikeIndex = 0; strikeIndex < strikeGrid.length; strikeIndex++) {
				final double strike = strikeGrid[strikeIndex];

				final FiniteDifferenceEquityProduct sliceProduct = createVanillaSliceProduct(strike);
				final double[] terminalValues = buildPointwiseTerminalValues(xGrid, strike);

				final FDMThetaMethod1D solver = new FDMThetaMethod1D(
						model,
						sliceProduct,
						discretization,
						new EuropeanExercise(maturity)
						);

				final double[][][] nextPlaneForObstacle = nextPlane;

				final DoubleBinaryOperator continuousObstacle =
						(runningTime, currentSpot) ->
				interpolateNextPlaneAtResetStrike1D(
						nextPlaneForObstacle,
						discretization,
						xGrid,
						currentSpot,
						runningTime
						) + shoutCashAdjustment;

				currentPlane[strikeIndex] = solver.getValues(
						maturity,
						terminalValues,
						continuousObstacle
						);
			}

			nextPlane = currentPlane;
		}

		return interpolatePlaneAtInitialStrike1D(nextPlane, discretization, xGrid, initialStrike);
	}

	private double[][] getValues2D(final FiniteDifferenceEquityModel model) {
		final SpaceTimeDiscretization discretization = model.getSpaceTimeDiscretization();
		final double[] x0Grid = discretization.getSpaceGrid(0).getGrid();
		final double[] x1Grid = discretization.getSpaceGrid(1).getGrid();

		validateInitialStrikeInsideGrid();
		validateStrikeGridCoversResetRange(x0Grid);

		double[][][] nextPlane = solveLastPlane2D(model);

		for (int usedShouts = maximumNumberOfShouts - 1; usedShouts >= 0; usedShouts--) {
			final double[][][] currentPlane = new double[strikeGrid.length][][];

			for (int strikeIndex = 0; strikeIndex < strikeGrid.length; strikeIndex++) {
				final double strike = strikeGrid[strikeIndex];

				final FiniteDifferenceEquityProduct sliceProduct = createVanillaSliceProduct(strike);
				final FDMSolver solver = FDMSolverFactory.createSolver(
						model,
						sliceProduct,
						new EuropeanExercise(maturity)
						);

				if (!(solver instanceof AbstractADI2D)) {
					throw new IllegalArgumentException("2D shout recursion requires an ADI-style solver.");
				}

				final double[][][] nextPlaneForObstacle = nextPlane;

				final AbstractADI2D.DoubleTernaryOperator continuousObstacle =
						(runningTime, currentSpot, secondState) ->
				interpolateNextPlaneAtResetStrike2D(
						nextPlaneForObstacle,
						discretization,
						x0Grid,
						x1Grid,
						currentSpot,
						secondState,
						runningTime
						) + shoutCashAdjustment;

				currentPlane[strikeIndex] = ((AbstractADI2D) solver).getValuesWithContinuousObstacle(
						maturity,
						(assetValue, secondState) -> terminalPayoff(assetValue, strike),
						continuousObstacle
						);
			}

			nextPlane = currentPlane;
		}

		return interpolatePlaneAtInitialStrike2D(nextPlane, discretization, x0Grid, x1Grid, initialStrike);
	}

	private double[][][] solveLastPlane1D(final FiniteDifferenceEquityModel model) {
		final double[][][] plane = new double[strikeGrid.length][][];

		for (int strikeIndex = 0; strikeIndex < strikeGrid.length; strikeIndex++) {
			plane[strikeIndex] = createVanillaSliceProduct(strikeGrid[strikeIndex]).getValues(model);
		}

		return plane;
	}

	private double[][][] solveLastPlane2D(final FiniteDifferenceEquityModel model) {
		final double[][][] plane = new double[strikeGrid.length][][];

		for (int strikeIndex = 0; strikeIndex < strikeGrid.length; strikeIndex++) {
			plane[strikeIndex] = createVanillaSliceProduct(strikeGrid[strikeIndex]).getValues(model);
		}

		return plane;
	}

	private FiniteDifferenceEquityProduct createVanillaSliceProduct(final double strike) {
		return new EuropeanOption(underlyingName, maturity, strike, callOrPut);
	}

	private double[] buildPointwiseTerminalValues(final double[] xGrid, final double strike) {
		final double[] values = new double[xGrid.length];

		for (int i = 0; i < xGrid.length; i++) {
			values[i] = terminalPayoff(xGrid[i], strike);
		}

		return values;
	}

	private double terminalPayoff(final double assetValue, final double strike) {
		if (callOrPut == CallOrPut.CALL) {
			return Math.max(assetValue - strike, 0.0);
		} else {
			return Math.max(strike - assetValue, 0.0);
		}
	}

	private double interpolateNextPlaneAtResetStrike1D(
			final double[][][] nextPlane,
			final SpaceTimeDiscretization discretization,
			final double[] xGrid,
			final double currentSpot,
			final double runningTime) {

		final int timeIndex = getTimeIndexForRunningTime(discretization, runningTime);

		final double[] valuesAcrossStrikes = new double[strikeGrid.length];
		for (int strikeIndex = 0; strikeIndex < strikeGrid.length; strikeIndex++) {
			valuesAcrossStrikes[strikeIndex] = interpolate1DAtTime(
					nextPlane[strikeIndex],
					xGrid,
					timeIndex,
					currentSpot
					);
		}

		return interpolateLinearWithConstantExtrapolation(
				strikeGrid,
				valuesAcrossStrikes,
				resetStrike(currentSpot)
				);
	}

	private double interpolateNextPlaneAtResetStrike2D(
			final double[][][] nextPlane,
			final SpaceTimeDiscretization discretization,
			final double[] x0Grid,
			final double[] x1Grid,
			final double currentSpot,
			final double secondState,
			final double runningTime) {

		final int timeIndex = getTimeIndexForRunningTime(discretization, runningTime);

		final double[] valuesAcrossStrikes = new double[strikeGrid.length];
		for (int strikeIndex = 0; strikeIndex < strikeGrid.length; strikeIndex++) {
			valuesAcrossStrikes[strikeIndex] = interpolate2DAtTime(
					nextPlane[strikeIndex],
					x0Grid,
					x1Grid,
					timeIndex,
					currentSpot,
					secondState
					);
		}

		return interpolateLinearWithConstantExtrapolation(
				strikeGrid,
				valuesAcrossStrikes,
				resetStrike(currentSpot)
				);
	}

	private double[][] interpolatePlaneAtInitialStrike1D(
			final double[][][] plane,
			final SpaceTimeDiscretization discretization,
			final double[] xGrid,
			final double strike) {

		final int numberOfTimePoints = discretization.getTimeDiscretization().getNumberOfTimeSteps() + 1;
		final double[][] result = new double[xGrid.length][numberOfTimePoints];

		for (int timeIndex = 0; timeIndex < numberOfTimePoints; timeIndex++) {
			for (int spotIndex = 0; spotIndex < xGrid.length; spotIndex++) {
				final double[] valuesAcrossStrikes = new double[strikeGrid.length];

				for (int strikeIndex = 0; strikeIndex < strikeGrid.length; strikeIndex++) {
					valuesAcrossStrikes[strikeIndex] = plane[strikeIndex][spotIndex][timeIndex];
				}

				result[spotIndex][timeIndex] = interpolateLinearWithConstantExtrapolation(
						strikeGrid,
						valuesAcrossStrikes,
						strike
						);
			}
		}

		return result;
	}

	private double[][] interpolatePlaneAtInitialStrike2D(
			final double[][][] plane,
			final SpaceTimeDiscretization discretization,
			final double[] x0Grid,
			final double[] x1Grid,
			final double strike) {

		final int numberOfTimePoints = discretization.getTimeDiscretization().getNumberOfTimeSteps() + 1;
		final int numberOfStates = x0Grid.length * x1Grid.length;
		final double[][] result = new double[numberOfStates][numberOfTimePoints];

		for (int timeIndex = 0; timeIndex < numberOfTimePoints; timeIndex++) {
			for (int flatIndex = 0; flatIndex < numberOfStates; flatIndex++) {
				final double[] valuesAcrossStrikes = new double[strikeGrid.length];

				for (int strikeIndex = 0; strikeIndex < strikeGrid.length; strikeIndex++) {
					valuesAcrossStrikes[strikeIndex] = plane[strikeIndex][flatIndex][timeIndex];
				}

				result[flatIndex][timeIndex] = interpolateLinearWithConstantExtrapolation(
						strikeGrid,
						valuesAcrossStrikes,
						strike
						);
			}
		}

		return result;
	}

	private int getTimeIndexForRunningTime(
			final SpaceTimeDiscretization discretization,
			final double runningTime) {

		final double tau = maturity - runningTime;
		int timeIndex = discretization.getTimeDiscretization().getTimeIndexNearestLessOrEqual(tau);

		if (timeIndex < 0) {
			timeIndex = 0;
		}
		if (timeIndex >= discretization.getTimeDiscretization().getNumberOfTimeSteps() + 1) {
			timeIndex = discretization.getTimeDiscretization().getNumberOfTimeSteps();
		}

		return timeIndex;
	}

	private double interpolate1DAtTime(
			final double[][] surface,
			final double[] xGrid,
			final int timeIndex,
			final double xQuery) {

		final int i0 = getLowerBracketIndexWithConstantExtrapolation(xGrid, xQuery);
		final int i1 = Math.min(i0 + 1, xGrid.length - 1);

		final double x0 = xGrid[i0];
		final double x1 = xGrid[i1];

		final double v0 = surface[i0][timeIndex];
		final double v1 = surface[i1][timeIndex];

		if (i0 == i1 || Math.abs(x1 - x0) < 1E-14) {
			return v0;
		}

		final double w = (xQuery - x0) / (x1 - x0);
		return (1.0 - w) * v0 + w * v1;
	}

	private double interpolate2DAtTime(
			final double[][] surface,
			final double[] x0Grid,
			final double[] x1Grid,
			final int timeIndex,
			final double x0Query,
			final double x1Query) {

		final int i0 = getLowerBracketIndexWithConstantExtrapolation(x0Grid, x0Query);
		final int i1 = Math.min(i0 + 1, x0Grid.length - 1);

		final int j0 = getLowerBracketIndexWithConstantExtrapolation(x1Grid, x1Query);
		final int j1 = Math.min(j0 + 1, x1Grid.length - 1);

		final double x0L = x0Grid[i0];
		final double x0U = x0Grid[i1];
		final double x1L = x1Grid[j0];
		final double x1U = x1Grid[j1];

		final double f00 = surface[flatten(i0, j0, x0Grid.length)][timeIndex];
		final double f10 = surface[flatten(i1, j0, x0Grid.length)][timeIndex];
		final double f01 = surface[flatten(i0, j1, x0Grid.length)][timeIndex];
		final double f11 = surface[flatten(i1, j1, x0Grid.length)][timeIndex];

		final double wx;
		if (i0 == i1 || Math.abs(x0U - x0L) < 1E-14) {
			wx = 0.0;
		} else {
			wx = (x0Query - x0L) / (x0U - x0L);
		}

		final double wy;
		if (j0 == j1 || Math.abs(x1U - x1L) < 1E-14) {
			wy = 0.0;
		} else {
			wy = (x1Query - x1L) / (x1U - x1L);
		}

		return (1.0 - wx) * (1.0 - wy) * f00
				+ wx * (1.0 - wy) * f10
				+ (1.0 - wx) * wy * f01
				+ wx * wy * f11;
	}

	private double resetStrike(final double currentSpot) {
		return currentSpot;
	}

	private int flatten(final int i0, final int i1, final int n0) {
		return i0 + i1 * n0;
	}

	private double interpolateLinearWithConstantExtrapolation(
			final double[] x,
			final double[] y,
			final double xQuery) {

		final int i0 = getLowerBracketIndexWithConstantExtrapolation(x, xQuery);
		final int i1 = Math.min(i0 + 1, x.length - 1);

		final double x0 = x[i0];
		final double x1 = x[i1];

		final double y0 = y[i0];
		final double y1 = y[i1];

		if (i0 == i1 || Math.abs(x1 - x0) < 1E-14) {
			return y0;
		}

		final double w = (xQuery - x0) / (x1 - x0);
		return (1.0 - w) * y0 + w * y1;
	}

	private int getLowerBracketIndexWithConstantExtrapolation(final double[] grid, final double x) {
		if (x <= grid[0]) {
			return 0;
		}
		if (x >= grid[grid.length - 1]) {
			return grid.length - 2;
		}

		int upperIndex = 1;
		while (upperIndex < grid.length && grid[upperIndex] < x) {
			upperIndex++;
		}
		return upperIndex - 1;
	}

	private void validateModel(final FiniteDifferenceEquityModel model) {
		if (model == null) {
			throw new IllegalArgumentException("model must not be null.");
		}
	}

	private void validateStrikeGrid() {
		for (int i = 0; i < strikeGrid.length; i++) {
			if (strikeGrid[i] <= 0.0) {
				throw new IllegalArgumentException("All strike-grid values must be positive.");
			}
			if (i > 0 && strikeGrid[i] <= strikeGrid[i - 1]) {
				throw new IllegalArgumentException("strikeGrid must be strictly increasing.");
			}
		}
	}

	private void validateInitialStrikeInsideGrid() {
		if (initialStrike < strikeGrid[0] || initialStrike > strikeGrid[strikeGrid.length - 1]) {
			throw new IllegalArgumentException("initialStrike must lie inside strikeGrid.");
		}
	}

	private void validateStrikeGridCoversResetRange(final double[] xGrid) {
		final double effectiveLower = xGrid.length > 2 ? xGrid[1] : xGrid[0];
		final double effectiveUpper = xGrid.length > 2 ? xGrid[xGrid.length - 2] : xGrid[xGrid.length - 1];

		if (strikeGrid[0] > effectiveLower || strikeGrid[strikeGrid.length - 1] < effectiveUpper) {
			throw new IllegalArgumentException(
					"For v1 with K* = S, strikeGrid should cover the interior range of the first-state grid."
					);
		}
	}

	/**
	 * Returns the underlying name.
	 *
	 * @return The underlying name, possibly {@code null}.
	 */
	public String getUnderlyingName() {
		return underlyingName;
	}

	/**
	 * Returns the maturity.
	 *
	 * @return The maturity.
	 */
	public double getMaturity() {
		return maturity;
	}

	/**
	 * Returns the initial strike.
	 *
	 * @return The initial strike.
	 */
	public double getInitialStrike() {
		return initialStrike;
	}

	/**
	 * Returns the strike grid.
	 *
	 * @return A defensive copy of the strike grid.
	 */
	public double[] getStrikeGrid() {
		return strikeGrid.clone();
	}

	/**
	 * Returns the maximum number of shouts.
	 *
	 * @return The maximum number of shouts.
	 */
	public int getMaximumNumberOfShouts() {
		return maximumNumberOfShouts;
	}

	/**
	 * Returns the call/put flag.
	 *
	 * @return The call/put flag.
	 */
	public CallOrPut getCallOrPut() {
		return callOrPut;
	}

	/**
	 * Returns the constant shout cash adjustment.
	 *
	 * @return The shout cash adjustment.
	 */
	public double getShoutCashAdjustment() {
		return shoutCashAdjustment;
	}

	/**
	 * Returns a string representation.
	 *
	 * @return A string representation of the product parameters.
	 */
	@Override
	public String toString() {
		return "ShoutOption [maturity=" + maturity
				+ ", initialStrike=" + initialStrike
				+ ", maximumNumberOfShouts=" + maximumNumberOfShouts
				+ ", callOrPut=" + callOrPut
				+ ", strikeGrid=" + Arrays.toString(strikeGrid)
				+ "]";
	}
}
