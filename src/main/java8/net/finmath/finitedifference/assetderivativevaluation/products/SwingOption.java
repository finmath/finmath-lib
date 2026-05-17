package net.finmath.finitedifference.assetderivativevaluation.products;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.DoubleBinaryOperator;

import net.finmath.finitedifference.assetderivativevaluation.models.FiniteDifferenceEquityModel;
import net.finmath.finitedifference.grids.Grid;
import net.finmath.finitedifference.grids.SpaceTimeDiscretization;
import net.finmath.finitedifference.solvers.FDMSolver;
import net.finmath.finitedifference.solvers.FDMSolverFactory;
import net.finmath.finitedifference.solvers.FDMThetaMethod1D;
import net.finmath.modelling.EuropeanExercise;
import net.finmath.modelling.products.CallOrPut;
import net.finmath.modelling.products.SwingQuantityMode;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * Fixed-strike generalized swing option.
 *
 * <p>
 * A swing option is a multiple-exercise contract with volume constraints. At
 * each
 * decision time <i>t<sub>i</sub></i>, the holder chooses an exercised quantity
 * <i>q<sub>i</sub></i> subject to local bounds
 * </p>
 *
 * <p>
 * <i>q<sub>i</sub> &isin; [q<sub>i</sub><sup>min</sup>,
 * q<sub>i</sub><sup>max</sup>]</i>,
 * </p>
 *
 * <p>
 * and global cumulative constraints
 * </p>
 *
 * <p>
 * <i>Q<sup>min</sup> &le; &sum;<sub>i=0</sub><sup>n-1</sup> q<sub>i</sub> &le;
 * Q<sup>max</sup></i>.
 * </p>
 *
 * <p>
 * For a fixed strike <i>K</i>, the immediate payoff at decision time
 * <i>t<sub>i</sub></i>
 * is
 * </p>
 *
 * <p>
 * <i>q<sub>i</sub> max(&omega;(S(t<sub>i</sub>) - K), 0)</i>,
 * </p>
 *
 * <p>
 * where <i>&omega; = 1</i> for a call and <i>&omega; = -1</i> for a put.
 * </p>
 *
 * <p>
 * The valuation is performed by dynamic programming over an auxiliary state
 * variable
 * representing cumulative consumed quantity
 * <i>Q<sub>i</sub> = &sum;<sub>k=0</sub><sup>i-1</sup> q<sub>k</sub></i>.
 * Between
 * decision dates, continuation values are propagated backward by the finite-
 * difference
 * solver of the underlying model. At each decision date the value is obtained
 * from the
 * Bellman step
 * </p>
 *
 * <p>
 * <i>V<sub>i</sub>(S,Q) = sup<sub>q &isin; A(i,Q)</sub>
 * { q max(&omega;(S-K),0) + C<sub>i</sub>(S,Q+q) }</i>,
 * </p>
 *
 * <p>
 * where <i>A(i,Q)</i> denotes the admissible set induced by local and global
 * quantity
 * constraints.
 * </p>
 *
 * <p>
 * The quantity control may be either bang-bang, where only local extrema are
 * admissible,
 * or based on a discretized quantity grid. The implementation supports one- and
 * two-dimensional finite-difference equity models. Since cumulative consumed
 * quantity is
 * an additional path state not contained in the model space grid, a full value
 * surface
 * independent of this state is not well defined. Therefore
 * {@link #getValues(FiniteDifferenceEquityModel)} is intentionally unsupported
 * in this
 * implementation, while {@link #getValue(double, FiniteDifferenceEquityModel)}
 * is
 * provided at evaluation time 0.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class SwingOption implements FiniteDifferenceEquityProduct {

	/**
	 * The eps.
	 */
	private static final double EPS = 1.0E-12;

	/**
	 * The underlying name.
	 */
	private final String underlyingName;
	/**
	 * The decision times.
	 */
	private final double[] decisionTimes;
	/**
	 * The maturity.
	 */
	private final double maturity;
	/**
	 * The strike.
	 */
	private final double strike;
	/**
	 * The local min quantity.
	 */
	private final double[] localMinQuantity;
	/**
	 * The local max quantity.
	 */
	private final double[] localMaxQuantity;
	/**
	 * The global min quantity.
	 */
	private final double globalMinQuantity;
	/**
	 * The global max quantity.
	 */
	private final double globalMaxQuantity;
	/**
	 * The call or put.
	 */
	private final CallOrPut callOrPut;
	/**
	 * The quantity mode.
	 */
	private final SwingQuantityMode quantityMode;
	/**
	 * The quantity grid step.
	 */
	private final double quantityGridStep;

	/**
	 * Creates a generalized swing option.
	 *
	 * @param underlyingName Name of the underlying. May be {@code null}.
	 * @param decisionTimes Exercise decision times.
	 * @param strike Fixed strike.
	 * @param localMinQuantity Local minimum quantities.
	 * @param localMaxQuantity Local maximum quantities.
	 * @param globalMinQuantity Global minimum total quantity.
	 * @param globalMaxQuantity Global maximum total quantity.
	 * @param callOrPut Call/put indicator.
	 * @param quantityMode Quantity control mode.
	 * @param quantityGridStep Quantity discretization step for
	 * {@link SwingQuantityMode#DISCRETE_QUANTITY_GRID}.
	 */
	public SwingOption(
			final String underlyingName,
			final double[] decisionTimes,
			final double strike,
			final double[] localMinQuantity,
			final double[] localMaxQuantity,
			final double globalMinQuantity,
			final double globalMaxQuantity,
			final CallOrPut callOrPut,
			final SwingQuantityMode quantityMode,
			final double quantityGridStep) {

		if (decisionTimes == null || decisionTimes.length == 0) {
			throw new IllegalArgumentException("decisionTimes must contain at least one time.");
		}
		if (localMinQuantity == null || localMaxQuantity == null) {
			throw new IllegalArgumentException("Local quantity arrays must not be null.");
		}
		if (localMinQuantity.length != decisionTimes.length || localMaxQuantity.length != decisionTimes.length) {
			throw new IllegalArgumentException("Local quantity arrays must have the same length as decisionTimes.");
		}
		if (strike < 0.0) {
			throw new IllegalArgumentException("strike must be non-negative.");
		}
		if (callOrPut == null) {
			throw new IllegalArgumentException("callOrPut must not be null.");
		}
		if (quantityMode == null) {
			throw new IllegalArgumentException("quantityMode must not be null.");
		}
		if (quantityMode == SwingQuantityMode.DISCRETE_QUANTITY_GRID && quantityGridStep <= 0.0) {
			throw new IllegalArgumentException("quantityGridStep must be positive for DISCRETE_QUANTITY_GRID mode.");
		}

		this.underlyingName = underlyingName;
		this.decisionTimes = decisionTimes.clone();
		this.maturity = decisionTimes[decisionTimes.length - 1];
		this.strike = strike;
		this.localMinQuantity = localMinQuantity.clone();
		this.localMaxQuantity = localMaxQuantity.clone();
		this.globalMinQuantity = globalMinQuantity;
		this.globalMaxQuantity = globalMaxQuantity;
		this.callOrPut = callOrPut;
		this.quantityMode = quantityMode;
		this.quantityGridStep = quantityGridStep;

		validateInputs();
	}

	/**
	 * Creates a generalized swing option with anonymous underlying.
	 *
	 * @param decisionTimes Exercise decision times.
	 * @param strike Fixed strike.
	 * @param localMinQuantity Local minimum quantities.
	 * @param localMaxQuantity Local maximum quantities.
	 * @param globalMinQuantity Global minimum total quantity.
	 * @param globalMaxQuantity Global maximum total quantity.
	 * @param callOrPut Call/put indicator.
	 * @param quantityMode Quantity control mode.
	 * @param quantityGridStep Quantity discretization step for
	 * {@link SwingQuantityMode#DISCRETE_QUANTITY_GRID}.
	 */
	public SwingOption(
			final double[] decisionTimes,
			final double strike,
			final double[] localMinQuantity,
			final double[] localMaxQuantity,
			final double globalMinQuantity,
			final double globalMaxQuantity,
			final CallOrPut callOrPut,
			final SwingQuantityMode quantityMode,
			final double quantityGridStep) {
		this(
				null,
				decisionTimes,
				strike,
				localMinQuantity,
				localMaxQuantity,
				globalMinQuantity,
				globalMaxQuantity,
				callOrPut,
				quantityMode,
				quantityGridStep
		);
	}

	/**
	 * Creates a generalized swing option with time-homogeneous local bounds.
	 *
	 * @param underlyingName Name of the underlying. May be {@code null}.
	 * @param decisionTimes Exercise decision times.
	 * @param strike Fixed strike.
	 * @param localMinQuantity Constant local minimum quantity.
	 * @param localMaxQuantity Constant local maximum quantity.
	 * @param globalMinQuantity Global minimum total quantity.
	 * @param globalMaxQuantity Global maximum total quantity.
	 * @param callOrPut Call/put indicator.
	 * @param quantityMode Quantity control mode.
	 * @param quantityGridStep Quantity discretization step for
	 * {@link SwingQuantityMode#DISCRETE_QUANTITY_GRID}.
	 */
	public SwingOption(
			final String underlyingName,
			final double[] decisionTimes,
			final double strike,
			final double localMinQuantity,
			final double localMaxQuantity,
			final double globalMinQuantity,
			final double globalMaxQuantity,
			final CallOrPut callOrPut,
			final SwingQuantityMode quantityMode,
			final double quantityGridStep) {
		this(
				underlyingName,
				decisionTimes,
				strike,
				fill(decisionTimes.length, localMinQuantity),
				fill(decisionTimes.length, localMaxQuantity),
				globalMinQuantity,
				globalMaxQuantity,
				callOrPut,
				quantityMode,
				quantityGridStep
		);
	}

	/**
	 * Creates a generalized swing option with anonymous underlying and time-
	 * homogeneous
	 * local bounds.
	 *
	 * @param decisionTimes Exercise decision times.
	 * @param strike Fixed strike.
	 * @param localMinQuantity Constant local minimum quantity.
	 * @param localMaxQuantity Constant local maximum quantity.
	 * @param globalMinQuantity Global minimum total quantity.
	 * @param globalMaxQuantity Global maximum total quantity.
	 * @param callOrPut Call/put indicator.
	 * @param quantityMode Quantity control mode.
	 * @param quantityGridStep Quantity discretization step for
	 * {@link SwingQuantityMode#DISCRETE_QUANTITY_GRID}.
	 */
	public SwingOption(
			final double[] decisionTimes,
			final double strike,
			final double localMinQuantity,
			final double localMaxQuantity,
			final double globalMinQuantity,
			final double globalMaxQuantity,
			final CallOrPut callOrPut,
			final SwingQuantityMode quantityMode,
			final double quantityGridStep) {
		this(
				null,
				decisionTimes,
				strike,
				localMinQuantity,
				localMaxQuantity,
				globalMinQuantity,
				globalMaxQuantity,
				callOrPut,
				quantityMode,
				quantityGridStep
		);
	}

	/**
	 * Returns the value at the given evaluation time.
	 *
	 * <p>
	 * In this implementation only evaluation time 0 is supported, since for
	 * later
	 * times the cumulative consumed quantity would have to be supplied
	 * explicitly.
	 * </p>
	 *
	 * @param evaluationTime Evaluation time.
	 * @param model The finite-difference model.
	 * @return The value on the model space grid.
	 */
	@Override
	public double[] getValue(final double evaluationTime, final FiniteDifferenceEquityModel model) {

		if (Math.abs(evaluationTime) > EPS) {
			throw new UnsupportedOperationException(
					"SwingOption v1 supports getValue only at evaluationTime = 0. "
							+ "For later times, cumulative consumed quantity would need to be supplied explicitly."
			);
		}
		if (model == null) {
			throw new IllegalArgumentException("model must not be null.");
		}

		final SpaceTimeDiscretization baseDiscretization = model.getSpaceTimeDiscretization();
		final int dimensions = baseDiscretization.getNumberOfSpaceGrids();

		if (dimensions != 1 && dimensions != 2) {
			throw new IllegalArgumentException("SwingOption currently supports only 1D and 2D models.");
		}

		if (globalMaxQuantity <= EPS || getTotalLocalMaxQuantity() <= EPS) {
			return new double[getNumberOfStates(baseDiscretization)];
		}

		final List<double[]> cumulativeQuantityGrids = buildCumulativeQuantityGrids();

		List<double[]> nextDecisionValueSlices = null;

		for (int decisionIndex = decisionTimes.length - 1; decisionIndex >= 0; decisionIndex--) {

			final double[] previousQuantityGrid = cumulativeQuantityGrids.get(decisionIndex);
			final double[] nextQuantityGrid = cumulativeQuantityGrids.get(decisionIndex + 1);

			final List<double[]> continuationSlicesAtCurrentDecision;

			if (nextDecisionValueSlices == null) {
				continuationSlicesAtCurrentDecision = null;
			} else {
				final double segmentLength = decisionTimes[decisionIndex + 1] - decisionTimes[decisionIndex];
				continuationSlicesAtCurrentDecision = propagateBackwardOverSegment(
						model,
						segmentLength,
						nextDecisionValueSlices
				);
			}

			nextDecisionValueSlices = applySwingStepConditionAtDecision(
					decisionIndex,
					baseDiscretization,
					previousQuantityGrid,
					nextQuantityGrid,
					continuationSlicesAtCurrentDecision
			);
		}

		if (nextDecisionValueSlices == null || nextDecisionValueSlices.size() != 1) {
			throw new IllegalStateException("Internal error in swing recursion.");
		}

		final double[] valueAtFirstDecision = nextDecisionValueSlices.get(0);

		if (decisionTimes[0] <= EPS) {
			return valueAtFirstDecision;
		}

		final List<double[]> valueAtTimeZero = propagateBackwardOverSegment(
				model,
				decisionTimes[0],
				Arrays.asList(valueAtFirstDecision)
		);

		return valueAtTimeZero.get(0);
	}

	/**
	 * Returns the full value surface.
	 *
	 * <p>
	 * This operation is intentionally unsupported, since the contract value
	 * depends on
	 * the additional cumulative-quantity state.
	 * </p>
	 *
	 * @param model The finite-difference model.
	 * @return Never returns normally.
	 */
	@Override
	public double[][] getValues(final FiniteDifferenceEquityModel model) {
		throw new UnsupportedOperationException(
				"SwingOption carries an additional cumulative-quantity state. "
						+ "A path-state-aware value surface container is required. "
						+ "Use getValue(0.0, model) in this v1 implementation."
		);
	}

	private List<double[]> applySwingStepConditionAtDecision(
			final int decisionIndex,
			final SpaceTimeDiscretization discretization,
			final double[] previousQuantityGrid,
			final double[] nextQuantityGrid,
			final List<double[]> continuationSlicesAtCurrentDecision) {

		final int numberOfStates = getNumberOfStates(discretization);
		final double[] unitIntrinsic = buildUnitIntrinsicVector(discretization);

		final List<double[]> valueSlicesBeforeDecision = new ArrayList<>(previousQuantityGrid.length);

		for (int previousIndex = 0; previousIndex < previousQuantityGrid.length; previousIndex++) {

			final double previousQuantity = previousQuantityGrid[previousIndex];
			final int[] admissibleDestinationIndices = getAdmissibleDestinationIndices(
					decisionIndex,
					previousQuantity,
					nextQuantityGrid
			);

			if (admissibleDestinationIndices.length == 0) {
				throw new IllegalStateException(
						"No admissible next quantities found from previous cumulative quantity " + previousQuantity
				);
			}

			final double[] valueSlice = new double[numberOfStates];
			Arrays.fill(valueSlice, Double.NEGATIVE_INFINITY);

			for (final int nextIndex : admissibleDestinationIndices) {

				final double nextQuantity = nextQuantityGrid[nextIndex];
				final double exercisedQuantity = nextQuantity - previousQuantity;
				final double[] continuation = continuationSlicesAtCurrentDecision == null
						? null
						: continuationSlicesAtCurrentDecision.get(nextIndex);

				for (int stateIndex = 0; stateIndex < numberOfStates; stateIndex++) {
					final double candidate =
							exercisedQuantity * unitIntrinsic[stateIndex]
									+ (continuation == null ? 0.0 : continuation[stateIndex]);

					if (candidate > valueSlice[stateIndex]) {
						valueSlice[stateIndex] = candidate;
					}
				}
			}

			valueSlicesBeforeDecision.add(valueSlice);
		}

		return valueSlicesBeforeDecision;
	}

	private List<double[]> propagateBackwardOverSegment(
			final FiniteDifferenceEquityModel model,
			final double segmentLength,
			final List<double[]> terminalSlicesAtSegmentEnd) {

		if (segmentLength <= EPS) {
			return deepCopySlices(terminalSlicesAtSegmentEnd);
		}

		final SpaceTimeDiscretization segmentDiscretization = createSegmentDiscretization(
				model.getSpaceTimeDiscretization(),
				segmentLength
		);

		final FiniteDifferenceEquityModel segmentModel =
				model.getCloneWithModifiedSpaceTimeDiscretization(segmentDiscretization);

		final FiniteDifferenceEquityProduct proxyProduct =
				new EuropeanOption(underlyingName, segmentLength, strike, callOrPut);

		final int dimensions = segmentDiscretization.getNumberOfSpaceGrids();
		final List<double[]> propagated = new ArrayList<>(terminalSlicesAtSegmentEnd.size());

		if (dimensions == 1) {
			final FDMThetaMethod1D solver = new FDMThetaMethod1D(
					segmentModel,
					proxyProduct,
					segmentDiscretization,
					new EuropeanExercise(segmentLength)
			);

			for (final double[] terminalSlice : terminalSlicesAtSegmentEnd) {
				propagated.add(solver.getValue(0.0, segmentLength, terminalSlice));
			}
		} else if (dimensions == 2) {
			final FDMSolver solver = FDMSolverFactory.createSolver(
					segmentModel,
					proxyProduct,
					segmentDiscretization,
					new EuropeanExercise(segmentLength)
			);

			final double[] x0Grid = segmentDiscretization.getSpaceGrid(0).getGrid();
			final double[] x1Grid = segmentDiscretization.getSpaceGrid(1).getGrid();

			for (final double[] terminalSlice : terminalSlicesAtSegmentEnd) {
				final DoubleBinaryOperator terminalFunction =
						(x0, x1) -> interpolate2DWithConstantExtrapolation(
								terminalSlice,
								x0Grid,
								x1Grid,
								x0,
								x1
						);

				propagated.add(solver.getValue(0.0, segmentLength, terminalFunction));
			}
		} else {
			throw new IllegalArgumentException("Only 1D and 2D models are supported.");
		}

		return propagated;
	}

	private List<double[]> buildCumulativeQuantityGrids() {

		final int numberOfDecisionTimes = decisionTimes.length;

		final double[] suffixMin = new double[numberOfDecisionTimes + 1];
		final double[] suffixMax = new double[numberOfDecisionTimes + 1];

		for (int i = numberOfDecisionTimes - 1; i >= 0; i--) {
			suffixMin[i] = suffixMin[i + 1] + localMinQuantity[i];
			suffixMax[i] = suffixMax[i + 1] + localMaxQuantity[i];
		}

		final List<double[]> grids = new ArrayList<>(numberOfDecisionTimes + 1);
		grids.add(new double[] {0.0 });

		for (int decisionIndex = 0; decisionIndex < numberOfDecisionTimes; decisionIndex++) {

			final double[] previousGrid = grids.get(decisionIndex);
			final double[] localQuantityCandidates = buildLocalQuantityCandidates(decisionIndex);

			final List<Double> nextGridCandidates = new ArrayList<>();

			for (final double previousQuantity : previousGrid) {
				for (final double exercisedQuantity : localQuantityCandidates) {

					final double nextQuantity = previousQuantity + exercisedQuantity;

					final boolean canStillReachGlobalMinimum =
							nextQuantity + suffixMax[decisionIndex + 1] >= globalMinQuantity - EPS;

					final boolean canStillRespectGlobalMaximum =
							nextQuantity + suffixMin[decisionIndex + 1] <= globalMaxQuantity + EPS;

					if (canStillReachGlobalMinimum && canStillRespectGlobalMaximum) {
						nextGridCandidates.add(nextQuantity);
					}
				}
			}

			final double[] nextGrid = uniqueSorted(nextGridCandidates);
			if (nextGrid.length == 0) {
				throw new IllegalArgumentException(
						"No feasible cumulative quantity states found at decision index " + decisionIndex
				);
			}

			grids.add(nextGrid);
		}

		return grids;
	}

	private double[] buildLocalQuantityCandidates(final int decisionIndex) {

		final double localMin = localMinQuantity[decisionIndex];
		final double localMax = localMaxQuantity[decisionIndex];

		if (quantityMode == SwingQuantityMode.BANG_BANG) {
			if (Math.abs(localMax - localMin) < EPS) {
				return new double[] {localMin };
			}
			return new double[] {localMin, localMax };
		}

		final List<Double> candidates = new ArrayList<>();
		double current = localMin;

		while (current < localMax - EPS) {
			candidates.add(current);
			current += quantityGridStep;
		}
		candidates.add(localMax);

		return uniqueSorted(candidates);
	}

	private int[] getAdmissibleDestinationIndices(
			final int decisionIndex,
			final double previousQuantity,
			final double[] nextQuantityGrid) {

		final double localMin = localMinQuantity[decisionIndex];
		final double localMax = localMaxQuantity[decisionIndex];

		final List<Integer> indices = new ArrayList<>();

		for (int nextIndex = 0; nextIndex < nextQuantityGrid.length; nextIndex++) {
			final double exercisedQuantity = nextQuantityGrid[nextIndex] - previousQuantity;

			if (exercisedQuantity >= localMin - EPS
					&& exercisedQuantity <= localMax + EPS) {
				indices.add(nextIndex);
			}
		}

		final int[] result = new int[indices.size()];
		for (int i = 0; i < indices.size(); i++) {
			result[i] = indices.get(i);
		}
		return result;
	}

	private double[] buildUnitIntrinsicVector(final SpaceTimeDiscretization discretization) {

		final double[] x0Grid = discretization.getSpaceGrid(0).getGrid();
		final int dimensions = discretization.getNumberOfSpaceGrids();

		if (dimensions == 1) {
			final double[] payoff = new double[x0Grid.length];
			for (int i = 0; i < x0Grid.length; i++) {
				payoff[i] = unitIntrinsic(x0Grid[i]);
			}
			return payoff;
		} else if (dimensions == 2) {
			final double[] x1Grid = discretization.getSpaceGrid(1).getGrid();
			final double[] payoff = new double[x0Grid.length * x1Grid.length];

			for (int j = 0; j < x1Grid.length; j++) {
				for (int i = 0; i < x0Grid.length; i++) {
					payoff[flatten(i, j, x0Grid.length)] = unitIntrinsic(x0Grid[i]);
				}
			}
			return payoff;
		} else {
			throw new IllegalArgumentException("Only 1D and 2D models are supported.");
		}
	}

	private double unitIntrinsic(final double assetValue) {
		final double signedIntrinsic = callOrPut.toInteger() * (assetValue - strike);
		return Math.max(signedIntrinsic, 0.0);
	}

	private SpaceTimeDiscretization createSegmentDiscretization(
			final SpaceTimeDiscretization baseDiscretization,
			final double segmentLength) {

		final TimeDiscretization baseTimeDiscretization = baseDiscretization.getTimeDiscretization();
		final int baseNumberOfTimeSteps = baseTimeDiscretization.getNumberOfTimeSteps();
		final double baseLastTime = Math.max(baseTimeDiscretization.getLastTime(), EPS);

		final int segmentNumberOfTimeSteps = Math.max(
				1,
				(int) Math.round(baseNumberOfTimeSteps * segmentLength / baseLastTime)
		);

		final TimeDiscretization segmentTimeDiscretization = new TimeDiscretizationFromArray(
				0.0,
				segmentNumberOfTimeSteps,
				segmentLength / segmentNumberOfTimeSteps
		);

		if (baseDiscretization.getNumberOfSpaceGrids() == 1) {
			return new SpaceTimeDiscretization(
					baseDiscretization.getSpaceGrid(0),
					segmentTimeDiscretization,
					baseDiscretization.getTheta(),
					new double[] {baseDiscretization.getCenter(0) }
			);
		}

		final int numberOfSpaceGrids = baseDiscretization.getNumberOfSpaceGrids();
		final Grid[] spaceGrids = new Grid[numberOfSpaceGrids];
		final double[] center = new double[numberOfSpaceGrids];

		for (int i = 0; i < numberOfSpaceGrids; i++) {
			spaceGrids[i] = baseDiscretization.getSpaceGrid(i);
			center[i] = baseDiscretization.getCenter(i);
		}

		return new SpaceTimeDiscretization(
				spaceGrids,
				segmentTimeDiscretization,
				baseDiscretization.getTheta(),
				center
		);
	}

	private int getNumberOfStates(final SpaceTimeDiscretization discretization) {
		if (discretization.getNumberOfSpaceGrids() == 1) {
			return discretization.getSpaceGrid(0).getGrid().length;
		}
		return discretization.getSpaceGrid(0).getGrid().length
			 * discretization.getSpaceGrid(1).getGrid().length;
	}

	private double interpolate2DWithConstantExtrapolation(
			final double[] flattenedValues,
			final double[] x0Grid,
			final double[] x1Grid,
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

		final double f00 = flattenedValues[flatten(i0, j0, x0Grid.length)];
		final double f10 = flattenedValues[flatten(i1, j0, x0Grid.length)];
		final double f01 = flattenedValues[flatten(i0, j1, x0Grid.length)];
		final double f11 = flattenedValues[flatten(i1, j1, x0Grid.length)];

		final double wx = (i0 == i1 || Math.abs(x0U - x0L) < EPS) ? 0.0 : (x0Query - x0L) / (x0U - x0L);
		final double wy = (j0 == j1 || Math.abs(x1U - x1L) < EPS) ? 0.0 : (x1Query - x1L) / (x1U - x1L);

		return (1.0 - wx) * (1.0 - wy) * f00
				+ wx * (1.0 - wy) * f10
				+ (1.0 - wx) * wy * f01
				+ wx * wy * f11;
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

	private int flatten(final int i0, final int i1, final int n0) {
		return i0 + i1 * n0;
	}

	private List<double[]> deepCopySlices(final List<double[]> slices) {
		final List<double[]> copy = new ArrayList<>(slices.size());
		for (final double[] slice : slices) {
			copy.add(slice.clone());
		}
		return copy;
	}

	private double[] uniqueSorted(final List<Double> values) {
		if (values.isEmpty()) {
			return new double[0];
		}

		final double[] raw = new double[values.size()];
		for (int i = 0; i < values.size(); i++) {
			raw[i] = values.get(i);
		}
		Arrays.sort(raw);

		final List<Double> unique = new ArrayList<>();
		unique.add(raw[0]);

		for (int i = 1; i < raw.length; i++) {
			if (Math.abs(raw[i] - unique.get(unique.size() - 1)) > 1.0E-10) {
				unique.add(raw[i]);
			}
		}

		final double[] result = new double[unique.size()];
		for (int i = 0; i < unique.size(); i++) {
			result[i] = unique.get(i);
		}
		return result;
	}

	private double getTotalLocalMaxQuantity() {
		double total = 0.0;
		for (final double q : localMaxQuantity) {
			total += q;
		}
		return total;
	}

	private static double[] fill(final int n, final double value) {
		final double[] x = new double[n];
		Arrays.fill(x, value);
		return x;
	}

	private void validateInputs() {

		for (int i = 0; i < decisionTimes.length; i++) {
			if (decisionTimes[i] < 0.0) {
				throw new IllegalArgumentException("decisionTimes must be non-negative.");
			}
			if (i > 0 && decisionTimes[i] <= decisionTimes[i - 1]) {
				throw new IllegalArgumentException("decisionTimes must be strictly increasing.");
			}
			if (localMinQuantity[i] < 0.0 || localMaxQuantity[i] < 0.0) {
				throw new IllegalArgumentException("Local quantities must be non-negative.");
			}
			if (localMaxQuantity[i] + EPS < localMinQuantity[i]) {
				throw new IllegalArgumentException("Each localMaxQuantity must be >= localMinQuantity.");
			}
		}

		if (globalMinQuantity < 0.0 || globalMaxQuantity < 0.0) {
			throw new IllegalArgumentException("Global quantities must be non-negative.");
		}
		if (globalMaxQuantity + EPS < globalMinQuantity) {
			throw new IllegalArgumentException("globalMaxQuantity must be >= globalMinQuantity.");
		}

		double totalLocalMin = 0.0;
		double totalLocalMax = 0.0;
		for (int i = 0; i < localMinQuantity.length; i++) {
			totalLocalMin += localMinQuantity[i];
			totalLocalMax += localMaxQuantity[i];
		}

		if (totalLocalMin > globalMaxQuantity + EPS) {
			throw new IllegalArgumentException("Global maximum is below the mandatory total local minimum.");
		}
		if (totalLocalMax + EPS < globalMinQuantity) {
			throw new IllegalArgumentException("Global minimum is above the achievable total local maximum.");
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
	 * Returns the decision times.
	 *
	 * @return A defensive copy of the decision times.
	 */
	public double[] getDecisionTimes() {
		return decisionTimes.clone();
	}

	/**
	 * Returns the maturity.
	 *
	 * @return The maturity, equal to the last decision time.
	 */
	public double getMaturity() {
		return maturity;
	}

	/**
	 * Returns the strike.
	 *
	 * @return The fixed strike.
	 */
	public double getStrike() {
		return strike;
	}

	/**
	 * Returns the local minimum quantities.
	 *
	 * @return A defensive copy of the local minimum quantities.
	 */
	public double[] getLocalMinQuantity() {
		return localMinQuantity.clone();
	}

	/**
	 * Returns the local maximum quantities.
	 *
	 * @return A defensive copy of the local maximum quantities.
	 */
	public double[] getLocalMaxQuantity() {
		return localMaxQuantity.clone();
	}

	/**
	 * Returns the global minimum total quantity.
	 *
	 * @return The global minimum total quantity.
	 */
	public double getGlobalMinQuantity() {
		return globalMinQuantity;
	}

	/**
	 * Returns the global maximum total quantity.
	 *
	 * @return The global maximum total quantity.
	 */
	public double getGlobalMaxQuantity() {
		return globalMaxQuantity;
	}

	/**
	 * Returns the call/put indicator.
	 *
	 * @return The call/put indicator.
	 */
	public CallOrPut getCallOrPut() {
		return callOrPut;
	}

	/**
	 * Returns the quantity control mode.
	 *
	 * @return The quantity control mode.
	 */
	public SwingQuantityMode getQuantityMode() {
		return quantityMode;
	}

	/**
	 * Returns the quantity grid step.
	 *
	 * @return The quantity grid step.
	 */
	public double getQuantityGridStep() {
		return quantityGridStep;
	}
}
