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
import net.finmath.modelling.products.SwingStrikeFixingConvention;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * Floating-strike fixed-maturity swing option.
 *
 * <p>
 * This product combines the volume-control features of a swing option with a
 * floating strike
 * determined by a weighted fixing average of the underlying. Let
 * <i>t_0, \ldots, t_{n-1}</i> denote the decision dates and
 * <i>u_0, \ldots, u_{m-1}</i> the fixing dates with associated non-negative
 * weights
 * <i>w_0, \ldots, w_{m-1}</i>. At each decision date the holder chooses an
 * exercised quantity
 * <i>q_i</i> subject to local bounds
 * </p>
 *
 * <p>
 * <i>q_i \in [q_i^{min}, q_i^{max}]</i>,
 * </p>
 *
 * <p>
 * and global cumulative bounds
 * </p>
 *
 * <p>
 * <i>Q^{min} \le \sum_{i=0}^{n-1} q_i \le Q^{max}</i>.
 * </p>
 *
 * <p>
 * The strike is built from an accumulator process
 * </p>
 *
 * <p>
 * <i>A_{new} = A_{old} + w S</i>,
 * </p>
 *
 * <p>
 * updated on fixing dates, and the effective strike used at a decision date is
 * </p>
 *
 * <p>
 * <i>K(A) = strikeShift + strikeScale \cdot A / W</i>,
 * </p>
 *
 * <p>
 * where <i>W</i> is the cumulative fixing weight available at that decision
 * date. The immediate
 * exercise payoff at a decision time is therefore
 * </p>
 *
 * <p>
 * <i>q \max(\omega (S - K(A)), 0)</i>,
 * </p>
 *
 * <p>
 * where <i>\omega = 1</i> for a call and <i>\omega = -1</i> for a put.
 * </p>
 *
 * <p>
 * The valuation is carried out by dynamic programming on two additional
 * discrete contract states:
 * cumulative consumed quantity and strike accumulator. Between event dates,
 * continuation values are
 * propagated backward with the existing one- or two-dimensional finite-
 * difference solver of the
 * supplied model. On fixing dates, the accumulator state is shifted according
 * to the fixing rule.
 * On decision dates, the Bellman optimization is applied over the admissible
 * exercised quantities.
 * </p>
 *
 * <p>
 * If a fixing date and a decision date coincide, the convention
 * {@link SwingStrikeFixingConvention#FIX_THEN_EXERCISE} is applied, that is,
 * the strike is first
 * updated using the fixing at that date and only then used in the exercise
 * payoff.
 * </p>
 *
 * <p>
 * As in the fixed-strike swing implementation, a full state-independent surface
 * {@link #getValues(FiniteDifferenceEquityModel)} is not well defined because
 * the contract value
 * depends on the additional cumulative-quantity and accumulator states. Hence
 * this implementation
 * exposes {@link #getValue(double, FiniteDifferenceEquityModel)} at evaluation
 * time 0 only.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class FloatingStrikeSwingOption implements FiniteDifferenceEquityProduct {

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
	 * The fixing times.
	 */
	private final double[] fixingTimes;
	/**
	 * The fixing weights.
	 */
	private final double[] fixingWeights;

	/**
	 * The maturity.
	 */
	private final double maturity;

	/**
	 * The strike shift.
	 */
	private final double strikeShift;
	/**
	 * The strike scale.
	 */
	private final double strikeScale;
	/**
	 * The accumulator grid.
	 */
	private final double[] accumulatorGrid;

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
	 * The fixing convention.
	 */
	private final SwingStrikeFixingConvention fixingConvention;

	/**
	 * Creates a floating-strike swing option.
	 *
	 * @param underlyingName Name of the underlying. May be {@code null}.
	 * @param decisionTimes Exercise decision times.
	 * @param fixingTimes Strike-fixing times.
	 * @param fixingWeights Strike-fixing weights.
	 * @param strikeShift Additive strike shift.
	 * @param strikeScale Multiplicative strike scale.
	 * @param accumulatorGrid Grid for the strike accumulator state.
	 * @param localMinQuantity Local minimum quantities.
	 * @param localMaxQuantity Local maximum quantities.
	 * @param globalMinQuantity Global minimum total quantity.
	 * @param globalMaxQuantity Global maximum total quantity.
	 * @param callOrPut Call/put flag.
	 * @param quantityMode Quantity control mode.
	 * @param quantityGridStep Quantity discretization step for
	 * {@link SwingQuantityMode#DISCRETE_QUANTITY_GRID}.
	 * @param fixingConvention Strike-fixing convention.
	 */
	public FloatingStrikeSwingOption(
			final String underlyingName,
			final double[] decisionTimes,
			final double[] fixingTimes,
			final double[] fixingWeights,
			final double strikeShift,
			final double strikeScale,
			final double[] accumulatorGrid,
			final double[] localMinQuantity,
			final double[] localMaxQuantity,
			final double globalMinQuantity,
			final double globalMaxQuantity,
			final CallOrPut callOrPut,
			final SwingQuantityMode quantityMode,
			final double quantityGridStep,
			final SwingStrikeFixingConvention fixingConvention) {

		if (decisionTimes == null || decisionTimes.length == 0) {
			throw new IllegalArgumentException("decisionTimes must contain at least one time.");
		}
		if (fixingTimes == null || fixingWeights == null || fixingTimes.length == 0) {
			throw new IllegalArgumentException("fixingTimes and fixingWeights must be non-empty.");
		}
		if (fixingTimes.length != fixingWeights.length) {
			throw new IllegalArgumentException("fixingTimes and fixingWeights must have the same length.");
		}
		if (accumulatorGrid == null || accumulatorGrid.length < 2) {
			throw new IllegalArgumentException("accumulatorGrid must contain at least two points.");
		}
		if (localMinQuantity == null || localMaxQuantity == null) {
			throw new IllegalArgumentException("Local quantity arrays must not be null.");
		}
		if (localMinQuantity.length != decisionTimes.length || localMaxQuantity.length != decisionTimes.length) {
			throw new IllegalArgumentException("Local quantity arrays must have the same length as decisionTimes.");
		}
		if (callOrPut == null) {
			throw new IllegalArgumentException("callOrPut must not be null.");
		}
		if (quantityMode == null) {
			throw new IllegalArgumentException("quantityMode must not be null.");
		}
		if (fixingConvention == null) {
			throw new IllegalArgumentException("fixingConvention must not be null.");
		}
		if (quantityMode == SwingQuantityMode.DISCRETE_QUANTITY_GRID && quantityGridStep <= 0.0) {
			throw new IllegalArgumentException("quantityGridStep must be positive for DISCRETE_QUANTITY_GRID mode.");
		}
		if (fixingConvention != SwingStrikeFixingConvention.FIX_THEN_EXERCISE) {
			throw new IllegalArgumentException("Only FIX_THEN_EXERCISE is currently supported.");
		}

		this.underlyingName = underlyingName;
		this.decisionTimes = decisionTimes.clone();
		this.fixingTimes = fixingTimes.clone();
		this.fixingWeights = fixingWeights.clone();
		this.maturity = Math.max(
				decisionTimes[decisionTimes.length - 1],
				fixingTimes[fixingTimes.length - 1]
				);
		this.strikeShift = strikeShift;
		this.strikeScale = strikeScale;
		this.accumulatorGrid = accumulatorGrid.clone();
		this.localMinQuantity = localMinQuantity.clone();
		this.localMaxQuantity = localMaxQuantity.clone();
		this.globalMinQuantity = globalMinQuantity;
		this.globalMaxQuantity = globalMaxQuantity;
		this.callOrPut = callOrPut;
		this.quantityMode = quantityMode;
		this.quantityGridStep = quantityGridStep;
		this.fixingConvention = fixingConvention;

		validateInputs();
	}

	/**
	 * Creates a floating-strike swing option with anonymous underlying.
	 *
	 * @param decisionTimes Exercise decision times.
	 * @param fixingTimes Strike-fixing times.
	 * @param fixingWeights Strike-fixing weights.
	 * @param strikeShift Additive strike shift.
	 * @param strikeScale Multiplicative strike scale.
	 * @param accumulatorGrid Grid for the strike accumulator state.
	 * @param localMinQuantity Local minimum quantities.
	 * @param localMaxQuantity Local maximum quantities.
	 * @param globalMinQuantity Global minimum total quantity.
	 * @param globalMaxQuantity Global maximum total quantity.
	 * @param callOrPut Call/put flag.
	 * @param quantityMode Quantity control mode.
	 * @param quantityGridStep Quantity discretization step for
	 * {@link SwingQuantityMode#DISCRETE_QUANTITY_GRID}.
	 * @param fixingConvention Strike-fixing convention.
	 */
	public FloatingStrikeSwingOption(
			final double[] decisionTimes,
			final double[] fixingTimes,
			final double[] fixingWeights,
			final double strikeShift,
			final double strikeScale,
			final double[] accumulatorGrid,
			final double[] localMinQuantity,
			final double[] localMaxQuantity,
			final double globalMinQuantity,
			final double globalMaxQuantity,
			final CallOrPut callOrPut,
			final SwingQuantityMode quantityMode,
			final double quantityGridStep,
			final SwingStrikeFixingConvention fixingConvention) {
		this(
				null,
				decisionTimes,
				fixingTimes,
				fixingWeights,
				strikeShift,
				strikeScale,
				accumulatorGrid,
				localMinQuantity,
				localMaxQuantity,
				globalMinQuantity,
				globalMaxQuantity,
				callOrPut,
				quantityMode,
				quantityGridStep,
				fixingConvention
				);
	}

	/**
	 * Creates a floating-strike swing option with time-homogeneous local
	 * quantity bounds.
	 *
	 * @param underlyingName Name of the underlying. May be {@code null}.
	 * @param decisionTimes Exercise decision times.
	 * @param fixingTimes Strike-fixing times.
	 * @param fixingWeights Strike-fixing weights.
	 * @param strikeShift Additive strike shift.
	 * @param strikeScale Multiplicative strike scale.
	 * @param accumulatorGrid Grid for the strike accumulator state.
	 * @param localMinQuantity Constant local minimum quantity.
	 * @param localMaxQuantity Constant local maximum quantity.
	 * @param globalMinQuantity Global minimum total quantity.
	 * @param globalMaxQuantity Global maximum total quantity.
	 * @param callOrPut Call/put flag.
	 * @param quantityMode Quantity control mode.
	 * @param quantityGridStep Quantity discretization step for
	 * {@link SwingQuantityMode#DISCRETE_QUANTITY_GRID}.
	 * @param fixingConvention Strike-fixing convention.
	 */
	public FloatingStrikeSwingOption(
			final String underlyingName,
			final double[] decisionTimes,
			final double[] fixingTimes,
			final double[] fixingWeights,
			final double strikeShift,
			final double strikeScale,
			final double[] accumulatorGrid,
			final double localMinQuantity,
			final double localMaxQuantity,
			final double globalMinQuantity,
			final double globalMaxQuantity,
			final CallOrPut callOrPut,
			final SwingQuantityMode quantityMode,
			final double quantityGridStep,
			final SwingStrikeFixingConvention fixingConvention) {
		this(
				underlyingName,
				decisionTimes,
				fixingTimes,
				fixingWeights,
				strikeShift,
				strikeScale,
				accumulatorGrid,
				fill(decisionTimes.length, localMinQuantity),
				fill(decisionTimes.length, localMaxQuantity),
				globalMinQuantity,
				globalMaxQuantity,
				callOrPut,
				quantityMode,
				quantityGridStep,
				fixingConvention
				);
	}

	/**
	 * Creates a floating-strike swing option with anonymous underlying and
	 * time-homogeneous
	 * local quantity bounds.
	 *
	 * @param decisionTimes Exercise decision times.
	 * @param fixingTimes Strike-fixing times.
	 * @param fixingWeights Strike-fixing weights.
	 * @param strikeShift Additive strike shift.
	 * @param strikeScale Multiplicative strike scale.
	 * @param accumulatorGrid Grid for the strike accumulator state.
	 * @param localMinQuantity Constant local minimum quantity.
	 * @param localMaxQuantity Constant local maximum quantity.
	 * @param globalMinQuantity Global minimum total quantity.
	 * @param globalMaxQuantity Global maximum total quantity.
	 * @param callOrPut Call/put flag.
	 * @param quantityMode Quantity control mode.
	 * @param quantityGridStep Quantity discretization step for
	 * {@link SwingQuantityMode#DISCRETE_QUANTITY_GRID}.
	 * @param fixingConvention Strike-fixing convention.
	 */
	public FloatingStrikeSwingOption(
			final double[] decisionTimes,
			final double[] fixingTimes,
			final double[] fixingWeights,
			final double strikeShift,
			final double strikeScale,
			final double[] accumulatorGrid,
			final double localMinQuantity,
			final double localMaxQuantity,
			final double globalMinQuantity,
			final double globalMaxQuantity,
			final CallOrPut callOrPut,
			final SwingQuantityMode quantityMode,
			final double quantityGridStep,
			final SwingStrikeFixingConvention fixingConvention) {
		this(
				null,
				decisionTimes,
				fixingTimes,
				fixingWeights,
				strikeShift,
				strikeScale,
				accumulatorGrid,
				localMinQuantity,
				localMaxQuantity,
				globalMinQuantity,
				globalMaxQuantity,
				callOrPut,
				quantityMode,
				quantityGridStep,
				fixingConvention
				);
	}

	/**
	 * Returns the value at the given evaluation time.
	 *
	 * <p>
	 * In this implementation only evaluation time 0 is supported, since the
	 * additional contract
	 * states must otherwise be supplied explicitly.
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
					"FloatingStrikeSwingOption v2 supports getValue only at evaluationTime = 0."
					);
		}
		if (model == null) {
			throw new IllegalArgumentException("model must not be null.");
		}

		final SpaceTimeDiscretization baseDiscretization = model.getSpaceTimeDiscretization();
		final int dimensions = baseDiscretization.getNumberOfSpaceGrids();

		if (dimensions != 1 && dimensions != 2) {
			throw new IllegalArgumentException("FloatingStrikeSwingOption currently supports only 1D and 2D models.");
		}

		if (globalMaxQuantity <= EPS || getTotalLocalMaxQuantity() <= EPS) {
			return new double[getNumberOfStates(baseDiscretization)];
		}

		final List<double[]> cumulativeQuantityGrids = buildCumulativeQuantityGrids();
		final List<Event> events = buildEvents();

		List<List<double[]>> nextEventValuePlanes = buildZeroValuePlanes(
				cumulativeQuantityGrids.get(cumulativeQuantityGrids.size() - 1).length,
				accumulatorGrid.length,
				getNumberOfStates(baseDiscretization)
				);

		double nextEventTime = maturity;

		for (int eventIndex = events.size() - 1; eventIndex >= 0; eventIndex--) {
			final Event event = events.get(eventIndex);

			final double segmentLength = nextEventTime - event.time;
			List<List<double[]>> valueAfterCurrentEvent = propagateBackwardOverSegment(
					model,
					segmentLength,
					nextEventValuePlanes
					);

			if (event.hasDecision()) {
				valueAfterCurrentEvent = applyDecisionAtEvent(
						event,
						baseDiscretization,
						cumulativeQuantityGrids,
						valueAfterCurrentEvent
						);
			}

			if (event.hasFixing()) {
				valueAfterCurrentEvent = applyFixingAtEvent(
						baseDiscretization,
						event.fixingWeightAtDate,
						valueAfterCurrentEvent
						);
			}

			nextEventValuePlanes = valueAfterCurrentEvent;
			nextEventTime = event.time;
		}

		if (nextEventTime > EPS) {
			nextEventValuePlanes = propagateBackwardOverSegment(
					model,
					nextEventTime,
					nextEventValuePlanes
					);
		}

		if (nextEventValuePlanes.size() != 1) {
			throw new IllegalStateException("Internal error: initial quantity plane should be unique.");
		}

		return interpolateAccumulatorPlaneSlice(nextEventValuePlanes.get(0), 0.0);
	}

	/**
	 * Returns the full value surface.
	 *
	 * <p>
	 * This operation is intentionally unsupported, since the contract value
	 * depends on additional
	 * discrete states.
	 * </p>
	 *
	 * @param model The finite-difference model.
	 * @return Never returns normally.
	 */
	@Override
	public double[][] getValues(final FiniteDifferenceEquityModel model) {
		throw new UnsupportedOperationException(
				"FloatingStrikeSwingOption carries additional discrete states "
						+ "(cumulative quantity and strike accumulator). "
						+ "Use getValue(0.0, model) in this v2 implementation."
				);
	}

	private List<List<double[]>> applyDecisionAtEvent(
			final Event event,
			final SpaceTimeDiscretization discretization,
			final List<double[]> cumulativeQuantityGrids,
			final List<List<double[]>> valueAfterDecision) {

		final double[] previousQuantityGrid = cumulativeQuantityGrids.get(event.decisionIndex);
		final double[] nextQuantityGrid = cumulativeQuantityGrids.get(event.decisionIndex + 1);
		final int numberOfStates = getNumberOfStates(discretization);

		final List<List<double[]>> valueBeforeDecision = new ArrayList<>(previousQuantityGrid.length);

		for (int previousIndex = 0; previousIndex < previousQuantityGrid.length; previousIndex++) {

			final double previousQuantity = previousQuantityGrid[previousIndex];
			final int[] admissibleDestinationIndices = getAdmissibleDestinationIndices(
					event.decisionIndex,
					previousQuantity,
					nextQuantityGrid
					);

			if (admissibleDestinationIndices.length == 0) {
				throw new IllegalStateException(
						"No admissible next quantities found from previous cumulative quantity " + previousQuantity
						);
			}

			final List<double[]> accumulatorSlices = new ArrayList<>(accumulatorGrid.length);

			for (int accumulatorIndex = 0; accumulatorIndex < accumulatorGrid.length; accumulatorIndex++) {

				final double effectiveStrike = getEffectiveStrike(
						accumulatorGrid[accumulatorIndex],
						event.cumulativeWeightAfterFixing
						);

				final double[] unitIntrinsic = buildUnitIntrinsicVector(discretization, effectiveStrike);
				final double[] valueSlice = new double[numberOfStates];
				Arrays.fill(valueSlice, Double.NEGATIVE_INFINITY);

				for (final int nextIndex : admissibleDestinationIndices) {
					final double exercisedQuantity = nextQuantityGrid[nextIndex] - previousQuantity;
					final double[] continuation = valueAfterDecision.get(nextIndex).get(accumulatorIndex);

					for (int stateIndex = 0; stateIndex < numberOfStates; stateIndex++) {
						final double candidate =
								exercisedQuantity * unitIntrinsic[stateIndex] + continuation[stateIndex];

						if (candidate > valueSlice[stateIndex]) {
							valueSlice[stateIndex] = candidate;
						}
					}
				}

				accumulatorSlices.add(valueSlice);
			}

			valueBeforeDecision.add(accumulatorSlices);
		}

		return valueBeforeDecision;
	}

	private List<List<double[]>> applyFixingAtEvent(
			final SpaceTimeDiscretization discretization,
			final double fixingWeightAtDate,
			final List<List<double[]>> valueAfterFixing) {

		final int dimensions = discretization.getNumberOfSpaceGrids();
		final double[] x0Grid = discretization.getSpaceGrid(0).getGrid();

		final List<List<double[]>> valueBeforeFixing = new ArrayList<>(valueAfterFixing.size());

		if (dimensions == 1) {
			for (int quantityIndex = 0; quantityIndex < valueAfterFixing.size(); quantityIndex++) {
				final List<double[]> oldAccumulatorSlices = new ArrayList<>(accumulatorGrid.length);

				for (int oldAccumulatorIndex = 0; oldAccumulatorIndex < accumulatorGrid.length; oldAccumulatorIndex++) {
					final double oldAccumulator = accumulatorGrid[oldAccumulatorIndex];
					final double[] out = new double[x0Grid.length];

					for (int i = 0; i < x0Grid.length; i++) {
						final double queryAccumulator = oldAccumulator + fixingWeightAtDate * x0Grid[i];
						out[i] = interpolateAccumulatorAtState(
								valueAfterFixing.get(quantityIndex),
								queryAccumulator,
								i
								);
					}

					oldAccumulatorSlices.add(out);
				}

				valueBeforeFixing.add(oldAccumulatorSlices);
			}
		} else if (dimensions == 2) {
			final double[] x1Grid = discretization.getSpaceGrid(1).getGrid();
			final int numberOfStates = x0Grid.length * x1Grid.length;

			for (int quantityIndex = 0; quantityIndex < valueAfterFixing.size(); quantityIndex++) {
				final List<double[]> oldAccumulatorSlices = new ArrayList<>(accumulatorGrid.length);

				for (int oldAccumulatorIndex = 0; oldAccumulatorIndex < accumulatorGrid.length; oldAccumulatorIndex++) {
					final double oldAccumulator = accumulatorGrid[oldAccumulatorIndex];
					final double[] out = new double[numberOfStates];

					for (int j = 0; j < x1Grid.length; j++) {
						for (int i = 0; i < x0Grid.length; i++) {
							final int flat = flatten(i, j, x0Grid.length);
							final double queryAccumulator = oldAccumulator + fixingWeightAtDate * x0Grid[i];

							out[flat] = interpolateAccumulatorAtState(
									valueAfterFixing.get(quantityIndex),
									queryAccumulator,
									flat
									);
						}
					}

					oldAccumulatorSlices.add(out);
				}

				valueBeforeFixing.add(oldAccumulatorSlices);
			}
		} else {
			throw new IllegalArgumentException("Only 1D and 2D models are supported.");
		}

		return valueBeforeFixing;
	}

	private List<List<double[]>> propagateBackwardOverSegment(
			final FiniteDifferenceEquityModel model,
			final double segmentLength,
			final List<List<double[]>> terminalPlanesAtSegmentEnd) {

		if (segmentLength <= EPS) {
			return deepCopyValuePlanes(terminalPlanesAtSegmentEnd);
		}

		final SpaceTimeDiscretization segmentDiscretization = createSegmentDiscretization(
				model.getSpaceTimeDiscretization(),
				segmentLength
				);

		final FiniteDifferenceEquityModel segmentModel =
				model.getCloneWithModifiedSpaceTimeDiscretization(segmentDiscretization);

		final FiniteDifferenceEquityProduct proxyProduct =
				new EuropeanOption(underlyingName, segmentLength, strikeShift, callOrPut);

		final int dimensions = segmentDiscretization.getNumberOfSpaceGrids();
		final List<List<double[]>> propagated = new ArrayList<>(terminalPlanesAtSegmentEnd.size());

		if (dimensions == 1) {
			final FDMThetaMethod1D solver = new FDMThetaMethod1D(
					segmentModel,
					proxyProduct,
					segmentDiscretization,
					new EuropeanExercise(segmentLength)
					);

			for (final List<double[]> accumulatorPlanes : terminalPlanesAtSegmentEnd) {
				final List<double[]> propagatedAccumulatorPlanes = new ArrayList<>(accumulatorPlanes.size());

				for (final double[] terminalSlice : accumulatorPlanes) {
					propagatedAccumulatorPlanes.add(solver.getValue(0.0, segmentLength, terminalSlice));
				}

				propagated.add(propagatedAccumulatorPlanes);
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

			for (final List<double[]> accumulatorPlanes : terminalPlanesAtSegmentEnd) {
				final List<double[]> propagatedAccumulatorPlanes = new ArrayList<>(accumulatorPlanes.size());

				for (final double[] terminalSlice : accumulatorPlanes) {
					final DoubleBinaryOperator terminalFunction =
							(x0, x1) -> interpolate2DWithConstantExtrapolation(
									terminalSlice,
									x0Grid,
									x1Grid,
									x0,
									x1
									);

							propagatedAccumulatorPlanes.add(solver.getValue(0.0, segmentLength, terminalFunction));
				}

				propagated.add(propagatedAccumulatorPlanes);
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

	private List<Event> buildEvents() {

		final List<Event> events = new ArrayList<>();

		int fixingPointer = 0;
		int decisionPointer = 0;
		double cumulativeWeight = 0.0;

		while (fixingPointer < fixingTimes.length || decisionPointer < decisionTimes.length) {

			final double nextFixingTime =
					fixingPointer < fixingTimes.length ? fixingTimes[fixingPointer] : Double.POSITIVE_INFINITY;

			final double nextDecisionTime =
					decisionPointer < decisionTimes.length ? decisionTimes[decisionPointer] : Double.POSITIVE_INFINITY;

			final double eventTime = Math.min(nextFixingTime, nextDecisionTime);

			double fixingWeightAtDate = 0.0;
			while (fixingPointer < fixingTimes.length && Math.abs(fixingTimes[fixingPointer] - eventTime) < EPS) {
				fixingWeightAtDate += fixingWeights[fixingPointer];
				fixingPointer++;
			}

			final boolean hasDecision =
					decisionPointer < decisionTimes.length
					&& Math.abs(decisionTimes[decisionPointer] - eventTime) < EPS;

			final int decisionIndex = hasDecision ? decisionPointer : -1;
			if (hasDecision) {
				decisionPointer++;
			}

			cumulativeWeight += fixingWeightAtDate;

			events.add(new Event(
					eventTime,
					fixingWeightAtDate,
					decisionIndex,
					cumulativeWeight
					));
		}

		return events;
	}

	private List<List<double[]>> buildZeroValuePlanes(
			final int numberOfQuantityStates,
			final int numberOfAccumulatorStates,
			final int numberOfModelStates) {

		final List<List<double[]>> surfaces = new ArrayList<>(numberOfQuantityStates);

		for (int quantityIndex = 0; quantityIndex < numberOfQuantityStates; quantityIndex++) {
			final List<double[]> accumulatorPlanes = new ArrayList<>(numberOfAccumulatorStates);

			for (int accumulatorIndex = 0; accumulatorIndex < numberOfAccumulatorStates; accumulatorIndex++) {
				accumulatorPlanes.add(new double[numberOfModelStates]);
			}

			surfaces.add(accumulatorPlanes);
		}

		return surfaces;
	}

	private double[] interpolateAccumulatorPlaneSlice(
			final List<double[]> accumulatorPlanes,
			final double accumulatorQuery) {

		final int numberOfStates = accumulatorPlanes.get(0).length;
		final double[] out = new double[numberOfStates];

		for (int stateIndex = 0; stateIndex < numberOfStates; stateIndex++) {
			out[stateIndex] = interpolateAccumulatorAtState(
					accumulatorPlanes,
					accumulatorQuery,
					stateIndex
					);
		}

		return out;
	}

	private double interpolateAccumulatorAtState(
			final List<double[]> accumulatorPlanes,
			final double accumulatorQuery,
			final int stateIndex) {

		final int lowerIndex = getLowerBracketIndexWithConstantExtrapolation(accumulatorGrid, accumulatorQuery);
		final int upperIndex = Math.min(lowerIndex + 1, accumulatorGrid.length - 1);

		final double aL = accumulatorGrid[lowerIndex];
		final double aU = accumulatorGrid[upperIndex];

		final double vL = accumulatorPlanes.get(lowerIndex)[stateIndex];
		final double vU = accumulatorPlanes.get(upperIndex)[stateIndex];

		if (lowerIndex == upperIndex || Math.abs(aU - aL) < EPS) {
			return vL;
		}

		final double w = (accumulatorQuery - aL) / (aU - aL);
		return (1.0 - w) * vL + w * vU;
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

	private double getEffectiveStrike(final double accumulatorValue, final double cumulativeWeightAtDecision) {
		if (Math.abs(strikeScale) < EPS) {
			return strikeShift;
		}
		if (cumulativeWeightAtDecision <= EPS) {
			throw new IllegalArgumentException(
					"Positive cumulative fixing weight is required at each decision date for floating strike."
					);
		}
		return strikeShift + strikeScale * accumulatorValue / cumulativeWeightAtDecision;
	}

	private double[] buildUnitIntrinsicVector(
			final SpaceTimeDiscretization discretization,
			final double effectiveStrike) {

		final double[] x0Grid = discretization.getSpaceGrid(0).getGrid();
		final int dimensions = discretization.getNumberOfSpaceGrids();

		if (dimensions == 1) {
			final double[] payoff = new double[x0Grid.length];
			for (int i = 0; i < x0Grid.length; i++) {
				payoff[i] = unitIntrinsic(x0Grid[i], effectiveStrike);
			}
			return payoff;
		} else if (dimensions == 2) {
			final double[] x1Grid = discretization.getSpaceGrid(1).getGrid();
			final double[] payoff = new double[x0Grid.length * x1Grid.length];

			for (int j = 0; j < x1Grid.length; j++) {
				for (int i = 0; i < x0Grid.length; i++) {
					payoff[flatten(i, j, x0Grid.length)] = unitIntrinsic(x0Grid[i], effectiveStrike);
				}
			}
			return payoff;
		} else {
			throw new IllegalArgumentException("Only 1D and 2D models are supported.");
		}
	}

	private double unitIntrinsic(final double assetValue, final double effectiveStrike) {
		final double signedIntrinsic = callOrPut.toInteger() * (assetValue - effectiveStrike);
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

	private List<List<double[]>> deepCopyValuePlanes(final List<List<double[]>> planes) {
		final List<List<double[]>> copy = new ArrayList<>(planes.size());

		for (final List<double[]> accumulatorPlanes : planes) {
			final List<double[]> accumulatorCopy = new ArrayList<>(accumulatorPlanes.size());

			for (final double[] slice : accumulatorPlanes) {
				accumulatorCopy.add(slice.clone());
			}

			copy.add(accumulatorCopy);
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

		for (int i = 0; i < fixingTimes.length; i++) {
			if (fixingTimes[i] < 0.0) {
				throw new IllegalArgumentException("fixingTimes must be non-negative.");
			}
			if (i > 0 && fixingTimes[i] < fixingTimes[i - 1] - EPS) {
				throw new IllegalArgumentException("fixingTimes must be non-decreasing.");
			}
			if (fixingWeights[i] < 0.0) {
				throw new IllegalArgumentException("fixingWeights must be non-negative.");
			}
		}

		for (int i = 0; i < accumulatorGrid.length; i++) {
			if (i > 0 && accumulatorGrid[i] <= accumulatorGrid[i - 1]) {
				throw new IllegalArgumentException("accumulatorGrid must be strictly increasing.");
			}
		}
		if (accumulatorGrid[0] > EPS) {
			throw new IllegalArgumentException("accumulatorGrid should contain or lie below the initial accumulator 0.");
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

		double cumulativeFixingWeight = 0.0;
		int fixingPointer = 0;

		for (final double decisionTime : decisionTimes) {
			while (fixingPointer < fixingTimes.length && fixingTimes[fixingPointer] <= decisionTime + EPS) {
				cumulativeFixingWeight += fixingWeights[fixingPointer];
				fixingPointer++;
			}

			if (Math.abs(strikeScale) > EPS && cumulativeFixingWeight <= EPS) {
				throw new IllegalArgumentException(
						"Each decision time must have positive cumulative fixing weight when strikeScale != 0."
						);
			}
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
	 * Returns the fixing times.
	 *
	 * @return A defensive copy of the fixing times.
	 */
	public double[] getFixingTimes() {
		return fixingTimes.clone();
	}

	/**
	 * Returns the fixing weights.
	 *
	 * @return A defensive copy of the fixing weights.
	 */
	public double[] getFixingWeights() {
		return fixingWeights.clone();
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
	 * Returns the additive strike shift.
	 *
	 * @return The additive strike shift.
	 */
	public double getStrikeShift() {
		return strikeShift;
	}

	/**
	 * Returns the multiplicative strike scale.
	 *
	 * @return The multiplicative strike scale.
	 */
	public double getStrikeScale() {
		return strikeScale;
	}

	/**
	 * Returns the accumulator grid.
	 *
	 * @return A defensive copy of the accumulator grid.
	 */
	public double[] getAccumulatorGrid() {
		return accumulatorGrid.clone();
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
	 * Returns the call/put flag.
	 *
	 * @return The call/put flag.
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

	/**
	 * Returns the strike-fixing convention.
	 *
	 * @return The strike-fixing convention.
	 */
	public SwingStrikeFixingConvention getFixingConvention() {
		return fixingConvention;
	}

	private static final class Event {

		/**
		 * The time.
		 */
		private final double time;
		/**
		 * The fixing weight at date.
		 */
		private final double fixingWeightAtDate;
		/**
		 * The decision index.
		 */
		private final int decisionIndex;
		/**
		 * The cumulative weight after fixing.
		 */
		private final double cumulativeWeightAfterFixing;

		private Event(
				final double time,
				final double fixingWeightAtDate,
				final int decisionIndex,
				final double cumulativeWeightAfterFixing) {
			this.time = time;
			this.fixingWeightAtDate = fixingWeightAtDate;
			this.decisionIndex = decisionIndex;
			this.cumulativeWeightAfterFixing = cumulativeWeightAfterFixing;
		}

		private boolean hasFixing() {
			return fixingWeightAtDate > EPS;
		}

		private boolean hasDecision() {
			return decisionIndex >= 0;
		}
	}

	private static double[] fill(final int n, final double value) {
		final double[] x = new double[n];
		Arrays.fill(x, value);
		return x;
	}
}
