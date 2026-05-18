package net.finmath.finitedifference.assetderivativevaluation.products;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.DoubleBinaryOperator;

import net.finmath.finitedifference.FiniteDifferenceExerciseUtil;
import net.finmath.finitedifference.assetderivativevaluation.models.FiniteDifferenceEquityModel;
import net.finmath.finitedifference.assetderivativevaluation.products.internal.ActivatedVectorEventState;
import net.finmath.finitedifference.assetderivativevaluation.products.internal.DiscreteMonitoringSupport;
import net.finmath.finitedifference.assetderivativevaluation.products.internal.ProductEventStateStack;
import net.finmath.finitedifference.grids.Grid;
import net.finmath.finitedifference.grids.SpaceTimeDiscretization;
import net.finmath.finitedifference.solvers.FDMSolver;
import net.finmath.finitedifference.solvers.FDMSolverFactory;
import net.finmath.finitedifference.solvers.FDMThetaMethod1D;
import net.finmath.finitedifference.solvers.adi.AbstractADI2D;
import net.finmath.modelling.EuropeanExercise;
import net.finmath.modelling.Exercise;
import net.finmath.modelling.products.DoubleBarrierType;
import net.finmath.modelling.products.MonitoringType;
import net.finmath.time.TimeDiscretization;

/**
 * Finite-difference valuation of a double-barrier cash binary option.
 *
 * <p>
 * The contract is defined by a maturity <i>T</i>, a cash amount <i>C</i>, and
 * two barriers
 * <i>L &lt; U</i>. Let <i>S(t)</i> denote the underlying and let
 * </p>
 *
 * <p>
 * <i>&tau;<sub>L</sub> = inf { t in [0,T] : S(t) &le; L }</i>,
 * </p>
 *
 * <p>
 * <i>&tau;<sub>U</sub> = inf { t in [0,T] : S(t) &ge; U }</i>,
 * </p>
 *
 * <p>
 * with the alive band given by
 * </p>
 *
 * <p>
 * <i>L &lt; S(t) &lt; U</i>.
 * </p>
 *
 * <p>
 * Supported barrier styles:
 * </p>
 * <ul>
 * <li>{@link DoubleBarrierType#KNOCK_OUT}: pays <i>C</i> if neither barrier is
 * hit before maturity,</li>
 * <li>{@link DoubleBarrierType#KNOCK_IN}: pays <i>C</i> if either barrier is
 * hit before or at maturity,</li>
 * <li>{@link DoubleBarrierType#KIKO}: pays <i>C</i> if the lower barrier is hit
 * first while the upper barrier acts as knock-out,</li>
 * <li>{@link DoubleBarrierType#KOKI}: pays <i>C</i> if the upper barrier is hit
 * first while the lower barrier acts as knock-out.</li>
 * </ul>
 *
 * <p>
 * For continuous monitoring, the implementation remains a direct one-state
 * constrained PDE.
 * For discrete monitoring, barrier activation / knock-out is applied only at
 * the supplied
 * monitoring dates via vector-level event conditions.
 * </p>
 *
 * <p>
 * For discretely monitored knock-in / KIKO / KOKI cases, the activated branch
 * is represented
 * by cached activated cash vectors. The cache is stored in a thread-local stack
 * so that nested
 * valuations and concurrent valuations of the same product instance do not
 * overwrite each other.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class DoubleBarrierBinaryOption implements
FiniteDifferenceEquityEventProduct,
FiniteDifferenceInternalStateConstraint {

	/**
	 * The underlying name.
	 */
	private final String underlyingName;
	/**
	 * The maturity.
	 */
	private final double maturity;
	/**
	 * The cash payoff.
	 */
	private final double cashPayoff;
	/**
	 * The lower barrier.
	 */
	private final double lowerBarrier;
	/**
	 * The upper barrier.
	 */
	private final double upperBarrier;
	/**
	 * The double barrier type.
	 */
	private final DoubleBarrierType doubleBarrierType;
	/**
	 * The exercise.
	 */
	private final Exercise exercise;
	/**
	 * The monitoring type.
	 */
	private final MonitoringType monitoringType;
	/**
	 * The monitoring times.
	 */
	private final double[] monitoringTimes;

	/**
	 * The activated vector event state stack.
	 */
	private transient ProductEventStateStack<ActivatedVectorEventState> activatedVectorEventStateStack;

	/**
	 * Creates a double-barrier cash binary option.
	 *
	 * @param underlyingName Name of the underlying. May be {@code null}.
	 * @param maturity Option maturity.
	 * @param cashPayoff Cash payoff amount.
	 * @param lowerBarrier Lower barrier.
	 * @param upperBarrier Upper barrier.
	 * @param doubleBarrierType Double-barrier type.
	 * @param exercise Exercise specification.
	 */
	public DoubleBarrierBinaryOption(
			final String underlyingName,
			final double maturity,
			final double cashPayoff,
			final double lowerBarrier,
			final double upperBarrier,
			final DoubleBarrierType doubleBarrierType,
			final Exercise exercise) {
		this(
				underlyingName,
				maturity,
				cashPayoff,
				lowerBarrier,
				upperBarrier,
				doubleBarrierType,
				exercise,
				MonitoringType.CONTINUOUS,
				null
				);
	}

	/**
	 * Creates a double-barrier cash binary option.
	 *
	 * @param underlyingName Name of the underlying. May be {@code null}.
	 * @param maturity Option maturity.
	 * @param cashPayoff Cash payoff amount.
	 * @param lowerBarrier Lower barrier.
	 * @param upperBarrier Upper barrier.
	 * @param doubleBarrierType Double-barrier type.
	 * @param exercise Exercise specification.
	 * @param monitoringType Monitoring type.
	 * @param monitoringTimes Monitoring times for discrete monitoring.
	 */
	public DoubleBarrierBinaryOption(
			final String underlyingName,
			final double maturity,
			final double cashPayoff,
			final double lowerBarrier,
			final double upperBarrier,
			final DoubleBarrierType doubleBarrierType,
			final Exercise exercise,
			final MonitoringType monitoringType,
			final double[] monitoringTimes) {

		if (doubleBarrierType == null) {
			throw new IllegalArgumentException("Double barrier type must not be null.");
		}
		if (exercise == null) {
			throw new IllegalArgumentException("Exercise must not be null.");
		}
		if (monitoringType == null) {
			throw new IllegalArgumentException("Monitoring type must not be null.");
		}
		if (!exercise.isEuropean() && !exercise.isBermudan() && !exercise.isAmerican()) {
			throw new IllegalArgumentException(
					"DoubleBarrierBinaryOption currently supports only European, Bermudan, and American exercise.");
		}
		if (maturity < 0.0) {
			throw new IllegalArgumentException("Maturity must be non-negative.");
		}
		if (cashPayoff < 0.0) {
			throw new IllegalArgumentException("Cash payoff must be non-negative.");
		}
		if (lowerBarrier <= 0.0 || upperBarrier <= 0.0) {
			throw new IllegalArgumentException("Barriers must be positive.");
		}
		if (lowerBarrier >= upperBarrier) {
			throw new IllegalArgumentException("lowerBarrier must be < upperBarrier.");
		}

		this.underlyingName = underlyingName;
		this.maturity = maturity;
		this.cashPayoff = cashPayoff;
		this.lowerBarrier = lowerBarrier;
		this.upperBarrier = upperBarrier;
		this.doubleBarrierType = doubleBarrierType;
		this.exercise = exercise;
		this.monitoringType = monitoringType;
		this.monitoringTimes = monitoringTimes == null ? null : monitoringTimes.clone();

		validateMonitoringSpecification();
	}

	/**
	 * Creates a European double-barrier cash binary option.
	 *
	 * @param underlyingName Name of the underlying. May be {@code null}.
	 * @param maturity Option maturity.
	 * @param cashPayoff Cash payoff amount.
	 * @param lowerBarrier Lower barrier.
	 * @param upperBarrier Upper barrier.
	 * @param doubleBarrierType Double-barrier type.
	 */
	public DoubleBarrierBinaryOption(
			final String underlyingName,
			final double maturity,
			final double cashPayoff,
			final double lowerBarrier,
			final double upperBarrier,
			final DoubleBarrierType doubleBarrierType) {
		this(
				underlyingName,
				maturity,
				cashPayoff,
				lowerBarrier,
				upperBarrier,
				doubleBarrierType,
				new EuropeanExercise(maturity)
				);
	}

	/**
	 * Creates a European double-barrier cash binary option.
	 *
	 * @param underlyingName Name of the underlying. May be {@code null}.
	 * @param maturity Option maturity.
	 * @param cashPayoff Cash payoff amount.
	 * @param lowerBarrier Lower barrier.
	 * @param upperBarrier Upper barrier.
	 * @param doubleBarrierType Double-barrier type.
	 * @param monitoringType Monitoring type.
	 * @param monitoringTimes Monitoring times for discrete monitoring.
	 */
	public DoubleBarrierBinaryOption(
			final String underlyingName,
			final double maturity,
			final double cashPayoff,
			final double lowerBarrier,
			final double upperBarrier,
			final DoubleBarrierType doubleBarrierType,
			final MonitoringType monitoringType,
			final double[] monitoringTimes) {
		this(
				underlyingName,
				maturity,
				cashPayoff,
				lowerBarrier,
				upperBarrier,
				doubleBarrierType,
				new EuropeanExercise(maturity),
				monitoringType,
				monitoringTimes
				);
	}

	/**
	 * Creates a European double-barrier cash binary option with anonymous
	 * underlying.
	 *
	 * @param maturity Option maturity.
	 * @param cashPayoff Cash payoff amount.
	 * @param lowerBarrier Lower barrier.
	 * @param upperBarrier Upper barrier.
	 * @param doubleBarrierType Double-barrier type.
	 */
	public DoubleBarrierBinaryOption(
			final double maturity,
			final double cashPayoff,
			final double lowerBarrier,
			final double upperBarrier,
			final DoubleBarrierType doubleBarrierType) {
		this(
				null,
				maturity,
				cashPayoff,
				lowerBarrier,
				upperBarrier,
				doubleBarrierType,
				new EuropeanExercise(maturity)
				);
	}

	/**
	 * Creates a European double-barrier cash binary option with anonymous
	 * underlying.
	 *
	 * @param maturity Option maturity.
	 * @param cashPayoff Cash payoff amount.
	 * @param lowerBarrier Lower barrier.
	 * @param upperBarrier Upper barrier.
	 * @param doubleBarrierType Double-barrier type.
	 * @param monitoringType Monitoring type.
	 * @param monitoringTimes Monitoring times for discrete monitoring.
	 */
	public DoubleBarrierBinaryOption(
			final double maturity,
			final double cashPayoff,
			final double lowerBarrier,
			final double upperBarrier,
			final DoubleBarrierType doubleBarrierType,
			final MonitoringType monitoringType,
			final double[] monitoringTimes) {
		this(
				null,
				maturity,
				cashPayoff,
				lowerBarrier,
				upperBarrier,
				doubleBarrierType,
				new EuropeanExercise(maturity),
				monitoringType,
				monitoringTimes
				);
	}

	/**
	 * Creates a double-barrier cash binary option with anonymous underlying.
	 *
	 * @param maturity Option maturity.
	 * @param cashPayoff Cash payoff amount.
	 * @param lowerBarrier Lower barrier.
	 * @param upperBarrier Upper barrier.
	 * @param doubleBarrierType Double-barrier type.
	 * @param exercise Exercise specification.
	 */
	public DoubleBarrierBinaryOption(
			final double maturity,
			final double cashPayoff,
			final double lowerBarrier,
			final double upperBarrier,
			final DoubleBarrierType doubleBarrierType,
			final Exercise exercise) {
		this(
				null,
				maturity,
				cashPayoff,
				lowerBarrier,
				upperBarrier,
				doubleBarrierType,
				exercise
				);
	}

	/**
	 * Creates a double-barrier cash binary option with anonymous underlying.
	 *
	 * @param maturity Option maturity.
	 * @param cashPayoff Cash payoff amount.
	 * @param lowerBarrier Lower barrier.
	 * @param upperBarrier Upper barrier.
	 * @param doubleBarrierType Double-barrier type.
	 * @param exercise Exercise specification.
	 * @param monitoringType Monitoring type.
	 * @param monitoringTimes Monitoring times for discrete monitoring.
	 */
	public DoubleBarrierBinaryOption(
			final double maturity,
			final double cashPayoff,
			final double lowerBarrier,
			final double upperBarrier,
			final DoubleBarrierType doubleBarrierType,
			final Exercise exercise,
			final MonitoringType monitoringType,
			final double[] monitoringTimes) {
		this(
				null,
				maturity,
				cashPayoff,
				lowerBarrier,
				upperBarrier,
				doubleBarrierType,
				exercise,
				monitoringType,
				monitoringTimes
				);
	}

	private ProductEventStateStack<ActivatedVectorEventState> getActivatedVectorEventStateStack() {
		if (activatedVectorEventStateStack == null) {
			activatedVectorEventStateStack = new ProductEventStateStack<>();
		}

		return activatedVectorEventStateStack;
	}

	private ActivatedVectorEventState createActivatedVectorEventState(
			final FiniteDifferenceEquityModel model) {

		return new ActivatedVectorEventState(
				buildActivatedVectorsAtEventTimes(model),
				DiscreteMonitoringSupport.DEFAULT_MONITORING_TIME_TOLERANCE
				);
	}

	private ActivatedVectorEventState getCurrentActivatedVectorEventState() {
		final ActivatedVectorEventState state =
				getActivatedVectorEventStateStack().currentOrNull();

		if (state == null) {
			throw new IllegalStateException(
					"Discrete knock-in event condition requires cached activated vectors."
					);
		}

		return state;
	}

	/**
	 * Returns the values at the specified evaluation time on the model space
	 * grid.
	 *
	 * @param evaluationTime Evaluation time.
	 * @param model The finite-difference model.
	 * @return The value vector at the requested evaluation time.
	 */
	@Override
	public double[] getValue(final double evaluationTime, final FiniteDifferenceEquityModel model) {
		final double[][] values = getValues(model);

		final SpaceTimeDiscretization valuationDiscretization = getValuationSpaceTimeDiscretization(model);
		final double tau = maturity - evaluationTime;
		final int timeIndex = valuationDiscretization.getTimeDiscretization()
				.getTimeIndexNearestLessOrEqual(tau);

		final double[] column = new double[values.length];
		for (int i = 0; i < values.length; i++) {
			column[i] = values[i][timeIndex];
		}

		if (usesDiscreteMonitoring() && isMonitoringTime(evaluationTime)) {
			return applyEvaluationTimeDiscreteCondition(evaluationTime, column, model);
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
		validateProductConfiguration(model);

		final FiniteDifferenceEquityModel effectiveModel = getEffectiveModelForValuation(model);
		final SpaceTimeDiscretization valuationDiscretization = effectiveModel.getSpaceTimeDiscretization();

		if (cashPayoff == 0.0) {
			return buildZeroValueSurface(valuationDiscretization);
		}

		final int dims = valuationDiscretization.getNumberOfSpaceGrids();

		if (dims == 1) {
			return getValues1D(effectiveModel, valuationDiscretization);
		} else if (dims == 2) {
			return getValues2D(effectiveModel, valuationDiscretization);
		} else {
			throw new IllegalArgumentException("DoubleBarrierBinaryOption currently supports only 1D and 2D models.");
		}
	}

	private double[][] getValues1D(
			final FiniteDifferenceEquityModel model,
			final SpaceTimeDiscretization valuationDiscretization) {

		if (usesDiscreteMonitoring() && requiresActivatedEventState()) {
			try(ProductEventStateStack.Scope ignored =
					getActivatedVectorEventStateStack().push(createActivatedVectorEventState(model))) {
				return getValues1DInternal(model, valuationDiscretization);
			}
		}

		return getValues1DInternal(model, valuationDiscretization);
	}

	private double[][] getValues1DInternal(
			final FiniteDifferenceEquityModel model,
			final SpaceTimeDiscretization valuationDiscretization) {

		final Exercise solverExercise = getSolverExerciseForValuation();

		final FDMSolver solver = new FDMThetaMethod1D(
				model,
				this,
				valuationDiscretization,
				solverExercise
				);

		final double[] terminalValues = usesDiscreteMonitoring()
				? buildDiscreteTerminalValues1D(valuationDiscretization)
						: buildCellAveragedTerminalValues(valuationDiscretization);

		if (solverExercise.isEuropean()) {
			return solver.getValues(maturity, terminalValues);
		}

		return solver.getValues(
				maturity,
				terminalValues,
				this::pointwiseExercisePayoff
				);
	}

	private double[][] getValues2D(
			final FiniteDifferenceEquityModel model,
			final SpaceTimeDiscretization valuationDiscretization) {

		if (usesDiscreteMonitoring() && requiresActivatedEventState()) {
			try(ProductEventStateStack.Scope ignored =
					getActivatedVectorEventStateStack().push(createActivatedVectorEventState(model))) {
				return getValues2DInternal(model, valuationDiscretization);
			}
		}

		return getValues2DInternal(model, valuationDiscretization);
	}

	private double[][] getValues2DInternal(
			final FiniteDifferenceEquityModel model,
			final SpaceTimeDiscretization valuationDiscretization) {

		final Exercise solverExercise = getSolverExerciseForValuation();

		final FDMSolver solver = FDMSolverFactory.createSolver(
				model,
				this,
				valuationDiscretization,
				solverExercise
				);

		final DoubleBinaryOperator terminalPayoff2D = usesDiscreteMonitoring()
				? getDiscreteTerminalPayoff2D()
						: (assetValue, secondState) -> pointwiseTerminalPayoff(assetValue);

						if (solverExercise.isEuropean()) {
							return solver.getValues(maturity, terminalPayoff2D);
						}

						if (!(solver instanceof AbstractADI2D)) {
							throw new IllegalArgumentException(
									"Two-dimensional Bermudan/American double-barrier binary pricing requires an ADI solver.");
						}

						return ((AbstractADI2D) solver).getValues(
								maturity,
								terminalPayoff2D,
								(runningTime, assetValue, secondState) -> pointwiseExercisePayoff(assetValue)
								);
	}

	@Override
	public double[] getEventTimes() {
		if (!usesDiscreteMonitoring()) {
			return new double[0];
		}

		return monitoringTimes == null ? new double[0] : monitoringTimes.clone();
	}

	@Override
	public double[] applyEventCondition(
			final double time,
			final double[] valuesAfterEvent,
			final FiniteDifferenceEquityModel model) {

		if (!usesDiscreteMonitoring()) {
			return valuesAfterEvent;
		}

		final int dims = model.getSpaceTimeDiscretization().getNumberOfSpaceGrids();

		if (dims == 1) {
			final double[] xGrid = model.getSpaceTimeDiscretization().getSpaceGrid(0).getGrid();

			if (valuesAfterEvent.length != xGrid.length) {
				throw new IllegalArgumentException(
						"Value vector length does not match the one-dimensional spatial grid.");
			}

			return applyDiscreteEvent1D(time, valuesAfterEvent, xGrid);
		}

		if (dims == 2) {
			final double[] x0 = model.getSpaceTimeDiscretization().getSpaceGrid(0).getGrid();
			final double[] x1 = model.getSpaceTimeDiscretization().getSpaceGrid(1).getGrid();

			if (valuesAfterEvent.length != x0.length * x1.length) {
				throw new IllegalArgumentException(
						"Value vector length does not match the two-dimensional spatial grid.");
			}

			return applyDiscreteEvent2D(time, valuesAfterEvent, x0, x1);
		}

		throw new IllegalArgumentException("Only 1D and 2D grids are supported.");
	}

	private double[] applyDiscreteEvent1D(
			final double time,
			final double[] valuesAfterEvent,
			final double[] xGrid) {

		final double[] valuesBeforeEvent = valuesAfterEvent.clone();
		final double[] activatedVector = requiresActivatedEventState()
				? getCurrentActivatedVectorEventState().getActivatedVector(time)
						: null;

		for (int i = 0; i < xGrid.length; i++) {
			final double assetValue = xGrid[i];

			switch (doubleBarrierType) {
			case KNOCK_OUT:
				if (!isInsideBarrierBand(assetValue)) {
					valuesBeforeEvent[i] = 0.0;
				}
				break;

			case KNOCK_IN:
				if (!isInsideBarrierBand(assetValue)) {
					valuesBeforeEvent[i] = activatedVector[i];
				}
				break;

			case KIKO:
				if (isBelowLowerBarrier(assetValue)) {
					valuesBeforeEvent[i] = activatedVector[i];
				} else if (isAboveUpperBarrier(assetValue)) {
					valuesBeforeEvent[i] = 0.0;
				}
				break;

			case KOKI:
				if (isAboveUpperBarrier(assetValue)) {
					valuesBeforeEvent[i] = activatedVector[i];
				} else if (isBelowLowerBarrier(assetValue)) {
					valuesBeforeEvent[i] = 0.0;
				}
				break;

			default:
				throw new IllegalArgumentException("Unsupported double barrier type.");
			}
		}

		return valuesBeforeEvent;
	}

	private double[] applyDiscreteEvent2D(
			final double time,
			final double[] valuesAfterEvent,
			final double[] x0,
			final double[] x1) {

		final double[] valuesBeforeEvent = valuesAfterEvent.clone();
		final double[] activatedVector = requiresActivatedEventState()
				? getCurrentActivatedVectorEventState().getActivatedVector(time)
						: null;

		final int n0 = x0.length;

		for (int j = 0; j < x1.length; j++) {
			for (int i = 0; i < x0.length; i++) {
				final int index = flatten(i, j, n0);
				final double assetValue = x0[i];

				switch (doubleBarrierType) {
				case KNOCK_OUT:
					if (!isInsideBarrierBand(assetValue)) {
						valuesBeforeEvent[index] = 0.0;
					}
					break;

				case KNOCK_IN:
					if (!isInsideBarrierBand(assetValue)) {
						valuesBeforeEvent[index] = activatedVector[index];
					}
					break;

				case KIKO:
					if (isBelowLowerBarrier(assetValue)) {
						valuesBeforeEvent[index] = activatedVector[index];
					} else if (isAboveUpperBarrier(assetValue)) {
						valuesBeforeEvent[index] = 0.0;
					}
					break;

				case KOKI:
					if (isAboveUpperBarrier(assetValue)) {
						valuesBeforeEvent[index] = activatedVector[index];
					} else if (isBelowLowerBarrier(assetValue)) {
						valuesBeforeEvent[index] = 0.0;
					}
					break;

				default:
					throw new IllegalArgumentException("Unsupported double barrier type.");
				}
			}
		}

		return valuesBeforeEvent;
	}

	private double[] applyEvaluationTimeDiscreteCondition(
			final double evaluationTime,
			final double[] valuesAtEvaluationTime,
			final FiniteDifferenceEquityModel model) {

		if (!requiresActivatedEventState()) {
			return applyEventCondition(evaluationTime, valuesAtEvaluationTime, model);
		}

		final Map<Double, double[]> activatedVectorsAtEventTimes = new HashMap<>();
		activatedVectorsAtEventTimes.put(
				evaluationTime,
				buildActivatedCashVectorAtEventTime(
						evaluationTime,
						model,
						valuesAtEvaluationTime.length
						)
				);

		final ActivatedVectorEventState state = new ActivatedVectorEventState(
				activatedVectorsAtEventTimes,
				DiscreteMonitoringSupport.DEFAULT_MONITORING_TIME_TOLERANCE
				);

		try(ProductEventStateStack.Scope ignored =
				getActivatedVectorEventStateStack().push(state)) {
			return applyEventCondition(evaluationTime, valuesAtEvaluationTime, model);
		}
	}

	private Map<Double, double[]> buildActivatedVectorsAtEventTimes(
			final FiniteDifferenceEquityModel model) {

		final Map<Double, double[]> activatedVectorsAtEventTimes = new HashMap<>();
		final int numberOfSpacePoints =
				getTotalNumberOfSpacePoints(model.getSpaceTimeDiscretization());

		for (final double eventTime : monitoringTimes) {
			activatedVectorsAtEventTimes.put(
					eventTime,
					buildActivatedCashVectorAtEventTime(
							eventTime,
							model,
							numberOfSpacePoints
							)
					);
		}

		return activatedVectorsAtEventTimes;
	}

	private double[] buildActivatedCashVectorAtEventTime(
			final double eventTime,
			final FiniteDifferenceEquityModel model,
			final int numberOfSpacePoints) {

		final double activatedValue = getActivatedCashValueAt(eventTime, model);
		final double[] activatedVector = new double[numberOfSpacePoints];

		Arrays.fill(activatedVector, activatedValue);

		return activatedVector;
	}

	private double getActivatedCashValueAt(
			final double eventTime,
			final FiniteDifferenceEquityModel model) {

		final double tolerance = DiscreteMonitoringSupport.DEFAULT_MONITORING_TIME_TOLERANCE;

		double cashReceiptTime = maturity;

		if (exercise.isExerciseAllowed(eventTime)) {
			cashReceiptTime = eventTime;
		} else {
			for (final double exerciseTime : exercise.getExerciseTimes()) {
				if (exerciseTime >= eventTime - tolerance) {
					cashReceiptTime = exerciseTime;
					break;
				}
			}
		}

		final double discountAtEventTime =
				model.getRiskFreeCurve().getDiscountFactor(eventTime);
		final double discountAtCashReceiptTime =
				model.getRiskFreeCurve().getDiscountFactor(cashReceiptTime);

		return cashPayoff * discountAtCashReceiptTime / discountAtEventTime;
	}

	private Exercise getSolverExerciseForValuation() {
		if (usesDiscreteMonitoring() && requiresActivatedEventState()) {
			return new EuropeanExercise(maturity);
		}

		return exercise;
	}

	private boolean requiresActivatedEventState() {
		return doubleBarrierType == DoubleBarrierType.KNOCK_IN
				|| doubleBarrierType == DoubleBarrierType.KIKO
				|| doubleBarrierType == DoubleBarrierType.KOKI;
	}

	private double[] buildDiscreteTerminalValues1D(final SpaceTimeDiscretization discretization) {

		final double[] terminalValues = new double[discretization.getSpaceGrid(0).getGrid().length];

		if (doubleBarrierType == DoubleBarrierType.KNOCK_OUT) {
			Arrays.fill(terminalValues, cashPayoff);
		}

		return terminalValues;
	}

	private DoubleBinaryOperator getDiscreteTerminalPayoff2D() {

		if (doubleBarrierType == DoubleBarrierType.KNOCK_OUT) {
			return (assetValue, secondState) -> cashPayoff;
		}

		return (assetValue, secondState) -> 0.0;
	}

	private void validateProductConfiguration(final FiniteDifferenceEquityModel model) {
		if (model == null) {
			throw new IllegalArgumentException("Model must not be null.");
		}

		final SpaceTimeDiscretization valuationDiscretization = getValuationSpaceTimeDiscretization(model);
		final int dims = valuationDiscretization.getNumberOfSpaceGrids();

		if (dims != 1 && dims != 2) {
			throw new IllegalArgumentException("DoubleBarrierBinaryOption currently supports only 1D and 2D models.");
		}

		final double[] spotGrid = valuationDiscretization.getSpaceGrid(0).getGrid();
		final double gridMin = spotGrid[0];
		final double gridMax = spotGrid[spotGrid.length - 1];

		if (lowerBarrier < gridMin || upperBarrier > gridMax) {
			throw new IllegalArgumentException(
					"Both double barriers must lie inside the first state-variable grid domain of the supplied model.");
		}
	}

	private double[][] buildZeroValueSurface(final SpaceTimeDiscretization discretization) {
		final int numberOfSpacePoints = getTotalNumberOfSpacePoints(discretization);
		final int numberOfTimePoints = discretization.getTimeDiscretization().getNumberOfTimeSteps() + 1;

		final double[][] zeroValues = new double[numberOfSpacePoints][numberOfTimePoints];

		for (int i = 0; i < numberOfSpacePoints; i++) {
			for (int j = 0; j < numberOfTimePoints; j++) {
				zeroValues[i][j] = 0.0;
			}
		}

		return zeroValues;
	}

	private double[] buildCellAveragedTerminalValues(final SpaceTimeDiscretization discretization) {
		final double[] sGrid = discretization.getSpaceGrid(0).getGrid();
		final double[] terminalValues = new double[sGrid.length];

		for (int i = 0; i < sGrid.length; i++) {
			final double leftEdge = getLeftDualCellEdge(sGrid, i);
			final double rightEdge = getRightDualCellEdge(sGrid, i);

			terminalValues[i] = cellAveragedTerminalPayoff(leftEdge, rightEdge);
		}

		return terminalValues;
	}

	private double cellAveragedTerminalPayoff(final double leftEdge, final double rightEdge) {
		if (!(leftEdge < rightEdge)) {
			throw new IllegalArgumentException("Require leftEdge < rightEdge.");
		}

		final double cellLength = rightEdge - leftEdge;

		final double belowLowerLength =
				Math.max(0.0, Math.min(rightEdge, lowerBarrier) - leftEdge);
		final double aboveUpperLength =
				Math.max(0.0, rightEdge - Math.max(leftEdge, upperBarrier));
		final double insideBandLength =
				Math.max(0.0, Math.min(rightEdge, upperBarrier) - Math.max(leftEdge, lowerBarrier));

		switch (doubleBarrierType) {
		case KNOCK_OUT:
			return cashPayoff * insideBandLength / cellLength;

		case KNOCK_IN:
			return cashPayoff * (belowLowerLength + aboveUpperLength) / cellLength;

		case KIKO:
			return cashPayoff * belowLowerLength / cellLength;

		case KOKI:
			return cashPayoff * aboveUpperLength / cellLength;

		default:
			throw new IllegalArgumentException("Unsupported double barrier type.");
		}
	}

	private double pointwiseTerminalPayoff(final double assetValue) {
		switch (doubleBarrierType) {
		case KNOCK_OUT:
			return isInsideBarrierBand(assetValue) ? cashPayoff : 0.0;

		case KNOCK_IN:
			return isInsideBarrierBand(assetValue) ? 0.0 : cashPayoff;

		case KIKO:
			return assetValue <= lowerBarrier ? cashPayoff : 0.0;

		case KOKI:
			return assetValue >= upperBarrier ? cashPayoff : 0.0;

		default:
			throw new IllegalArgumentException("Unsupported double barrier type.");
		}
	}

	/**
	 * Returns the immediate exercise payoff under the current binary state.
	 *
	 * <p>
	 * For continuous monitoring, this matches the terminal state
	 * classification.
	 * For discrete knock-in-like products, the pre-hit branch is solved as a
	 * European
	 * continuation problem; exercise rights are carried by the activated cash
	 * branch.
	 * </p>
	 *
	 * @param assetValue Current underlying level.
	 * @return The immediate exercise payoff.
	 */
	private double pointwiseExercisePayoff(final double assetValue) {

		if (usesDiscreteMonitoring()) {
			if (doubleBarrierType == DoubleBarrierType.KNOCK_OUT) {
				return cashPayoff;
			}

			return 0.0;
		}

		return pointwiseTerminalPayoff(assetValue);
	}

	private double getLeftDualCellEdge(final double[] grid, final int i) {
		if (i == 0) {
			return grid[0];
		}

		return 0.5 * (grid[i - 1] + grid[i]);
	}

	private double getRightDualCellEdge(final double[] grid, final int i) {
		if (i == grid.length - 1) {
			return grid[grid.length - 1];
		}

		return 0.5 * (grid[i] + grid[i + 1]);
	}

	private boolean isInsideBarrierBand(final double assetValue) {
		return assetValue > lowerBarrier && assetValue < upperBarrier;
	}

	private boolean isBelowLowerBarrier(final double assetValue) {
		return assetValue <= lowerBarrier;
	}

	private boolean isAboveUpperBarrier(final double assetValue) {
		return assetValue >= upperBarrier;
	}

	/**
	 * Returns whether the internal constrained regime is active.
	 *
	 * @param time Evaluation time.
	 * @param stateVariables State variables.
	 * @return {@code true} if the underlying is outside the alive band.
	 */
	@Override
	public boolean isConstraintActive(final double time, final double... stateVariables) {

		if (usesDiscreteMonitoring()) {
			return false;
		}

		final double underlyingLevel = stateVariables[0];

		return isBelowLowerBarrier(underlyingLevel) || isAboveUpperBarrier(underlyingLevel);
	}

	/**
	 * Returns the constrained value in the barrier region.
	 *
	 * @param time Evaluation time.
	 * @param stateVariables State variables.
	 * @return The constrained value.
	 */
	@Override
	public double getConstrainedValue(final double time, final double... stateVariables) {
		final double underlyingLevel = stateVariables[0];

		switch (doubleBarrierType) {
		case KNOCK_OUT:
			return 0.0;

		case KNOCK_IN:
			return cashPayoff;

		case KIKO:
			return isBelowLowerBarrier(underlyingLevel) ? cashPayoff : 0.0;

		case KOKI:
			return isAboveUpperBarrier(underlyingLevel) ? cashPayoff : 0.0;

		default:
			throw new IllegalArgumentException("Unsupported double barrier type.");
		}
	}

	private boolean usesDiscreteMonitoring() {
		return DiscreteMonitoringSupport.usesDiscreteMonitoring(monitoringType);
	}

	private boolean isMonitoringTime(final double time) {
		return DiscreteMonitoringSupport.isMonitoringTime(
				time,
				monitoringTimes,
				DiscreteMonitoringSupport.DEFAULT_MONITORING_TIME_TOLERANCE
				);
	}

	private void validateMonitoringSpecification() {
		DiscreteMonitoringSupport.validateMonitoringSpecification(
				monitoringType,
				monitoringTimes,
				maturity,
				DiscreteMonitoringSupport.DEFAULT_MONITORING_TIME_TOLERANCE
				);
	}

	private SpaceTimeDiscretization getValuationSpaceTimeDiscretization(final FiniteDifferenceEquityModel model) {
		final SpaceTimeDiscretization base = model.getSpaceTimeDiscretization();

		TimeDiscretization refinedTimeDiscretization = base.getTimeDiscretization();
		boolean requiresModifiedDiscretization = false;

		if (exercise.isBermudan()) {
			refinedTimeDiscretization =
					FiniteDifferenceExerciseUtil.refineTimeDiscretization(
							refinedTimeDiscretization,
							exercise
							);
			requiresModifiedDiscretization = true;
		}

		if (usesDiscreteMonitoring()) {
			refinedTimeDiscretization =
					DiscreteMonitoringSupport.refineTimeDiscretizationWithMonitoring(
							refinedTimeDiscretization,
							maturity,
							monitoringTimes
							);
			requiresModifiedDiscretization = true;
		}

		if (!requiresModifiedDiscretization) {
			return base;
		}

		if (base.getNumberOfSpaceGrids() == 1) {
			return new SpaceTimeDiscretization(
					base.getSpaceGrid(0),
					refinedTimeDiscretization,
					base.getTheta(),
					new double[] {base.getCenter(0) }
					);
		}

		final int numberOfSpaceGrids = base.getNumberOfSpaceGrids();
		final Grid[] spaceGrids = new Grid[numberOfSpaceGrids];
		final double[] center = new double[numberOfSpaceGrids];

		for (int i = 0; i < numberOfSpaceGrids; i++) {
			spaceGrids[i] = base.getSpaceGrid(i);
			center[i] = base.getCenter(i);
		}

		return new SpaceTimeDiscretization(
				spaceGrids,
				refinedTimeDiscretization,
				base.getTheta(),
				center
				);
	}

	private FiniteDifferenceEquityModel getEffectiveModelForValuation(final FiniteDifferenceEquityModel model) {
		final SpaceTimeDiscretization effectiveDiscretization = getValuationSpaceTimeDiscretization(model);

		if (effectiveDiscretization == model.getSpaceTimeDiscretization()) {
			return model;
		}

		return model.getCloneWithModifiedSpaceTimeDiscretization(effectiveDiscretization);
	}

	private int getTotalNumberOfSpacePoints(final SpaceTimeDiscretization discretization) {
		final int dims = discretization.getNumberOfSpaceGrids();

		if (dims == 1) {
			return discretization.getSpaceGrid(0).getGrid().length;
		} else if (dims == 2) {
			return discretization.getSpaceGrid(0).getGrid().length
					* discretization.getSpaceGrid(1).getGrid().length;
		} else {
			throw new IllegalArgumentException("Only 1D and 2D grids are supported.");
		}
	}

	private int flatten(final int i, final int j, final int n0) {
		return i + n0 * j;
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
	 * Returns the cash payoff.
	 *
	 * @return The cash payoff.
	 */
	public double getCashPayoff() {
		return cashPayoff;
	}

	/**
	 * Returns the lower barrier.
	 *
	 * @return The lower barrier.
	 */
	public double getLowerBarrier() {
		return lowerBarrier;
	}

	/**
	 * Returns the upper barrier.
	 *
	 * @return The upper barrier.
	 */
	public double getUpperBarrier() {
		return upperBarrier;
	}

	/**
	 * Returns the double-barrier type.
	 *
	 * @return The double-barrier type.
	 */
	public DoubleBarrierType getDoubleBarrierType() {
		return doubleBarrierType;
	}

	/**
	 * Returns the exercise specification.
	 *
	 * @return The exercise specification.
	 */
	public Exercise getExercise() {
		return exercise;
	}

	/**
	 * Returns the monitoring type.
	 *
	 * @return The monitoring type.
	 */
	public MonitoringType getMonitoringType() {
		return monitoringType;
	}

	/**
	 * Returns the monitoring times.
	 *
	 * @return The monitoring times, or {@code null} for continuous monitoring.
	 */
	public double[] getMonitoringTimes() {
		return monitoringTimes == null ? null : monitoringTimes.clone();
	}
}
