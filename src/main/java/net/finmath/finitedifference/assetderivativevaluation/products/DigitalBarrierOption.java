package net.finmath.finitedifference.assetderivativevaluation.products;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

import net.finmath.finitedifference.FiniteDifferenceExerciseUtil;
import net.finmath.finitedifference.assetderivativevaluation.boundaries.ActiveBoundaryProviderFactory;
import net.finmath.finitedifference.assetderivativevaluation.models.FDMHestonModel;
import net.finmath.finitedifference.assetderivativevaluation.models.FDMSabrModel;
import net.finmath.finitedifference.assetderivativevaluation.models.FiniteDifferenceEquityModel;
import net.finmath.finitedifference.assetderivativevaluation.products.internal.DiscreteKnockInActivationSupport;
import net.finmath.finitedifference.assetderivativevaluation.products.internal.DiscreteMonitoringSupport;
import net.finmath.finitedifference.grids.Grid;
import net.finmath.finitedifference.grids.SpaceTimeDiscretization;
import net.finmath.finitedifference.grids.UniformGrid;
import net.finmath.finitedifference.solvers.FDMSolver;
import net.finmath.finitedifference.solvers.FDMSolverFactory;
import net.finmath.finitedifference.solvers.FDMThetaMethod1DTwoState;
import net.finmath.finitedifference.solvers.adi.ActivatedBarrierTrace2D;
import net.finmath.finitedifference.solvers.adi.BarrierPDEMode;
import net.finmath.finitedifference.solvers.adi.BarrierPreHitSpecification;
import net.finmath.interpolation.RationalFunctionInterpolation;
import net.finmath.interpolation.RationalFunctionInterpolation.ExtrapolationMethod;
import net.finmath.interpolation.RationalFunctionInterpolation.InterpolationMethod;
import net.finmath.modelling.EuropeanExercise;
import net.finmath.modelling.Exercise;
import net.finmath.modelling.products.BarrierType;
import net.finmath.modelling.products.CallOrPut;
import net.finmath.modelling.products.DigitalPayoffType;
import net.finmath.modelling.products.MonitoringType;
import net.finmath.time.TimeDiscretization;

/**
 * Finite-difference valuation of a single-barrier digital option on one asset.
 *
 * <p>
 * Supported payoff families:
 * </p>
 * <ul>
 *   <li>cash-or-nothing call / put,</li>
 *   <li>asset-or-nothing call / put,</li>
 *   <li>down-in / up-in / down-out / up-out.</li>
 * </ul>
 *
 * <p>
 * Current implementation policy:
 * </p>
 * <ul>
 * <li>continuously monitored knock-out options are priced directly by the
 * finite-difference solver,</li>
 * <li>continuously monitored 1D knock-in options are priced directly through a
 * coupled two-state PDE on an auxiliary spatial grid where the barrier is
 * placed on an interior node,</li>
 * <li>continuously monitored 2D knock-in options use the activated-branch /
 * pre-hit formulation,</li>
 * <li>for one-dimensional models, terminal payoff initialization is cell-
 * averaged in order to reduce strike-discontinuity grid bias,</li>
 * <li>discretely monitored 1D and 2D knock-out options are handled through
 * event-time zeroing on breached nodes,</li>
 * <li>discretely monitored 1D and 2D knock-in options are handled through
 * event-time replacement by the activated vanilla digital continuation
 * surface,</li>
 * <li>for discretely monitored knock-ins with Bermudan/American exercise, only
 * the activated state carries exercise rights; the pre-hit state is solved as a
 * European continuation problem,</li>
 *   <li>2D discrete monitoring is supported for Heston / SABR models.</li>
 * </ul>
 *
 * @author Alessandro Gnoatto
 */
public class DigitalBarrierOption implements
FiniteDifferenceEquityEventProduct,
FiniteDifferenceInternalStateConstraint,
FiniteDifferenceOneDimensionalKnockInProduct {

	private enum PricingMode {
		/**
		 * The direct out.
		 */
		DIRECT_OUT,
		/**
		 * The direct in 1d two state.
		 */
		DIRECT_IN_1D_TWO_STATE,
		/**
		 * The direct in 1d discrete event.
		 */
		DIRECT_IN_1D_DISCRETE_EVENT,
		/**
		 * The direct in 2d discrete event.
		 */
		DIRECT_IN_2D_DISCRETE_EVENT,
		/**
		 * The direct in 2d pre hit.
		 */
		DIRECT_IN_2D_PRE_HIT,
		/**
		 * The parity in fallback.
		 */
		PARITY_IN_FALLBACK
	}

	/**
	 * The default interior barrier extra steps 1 d.
	 */
	private static final int DEFAULT_INTERIOR_BARRIER_EXTRA_STEPS_1D = 40;
	/**
	 * The down in put extra steps 1 d.
	 */
	private static final int DOWN_IN_PUT_EXTRA_STEPS_1D = 160;
	/**
	 * The up in call extra steps 1 d.
	 */
	private static final int UP_IN_CALL_EXTRA_STEPS_1D = 160;
	/**
	 * The grid tolerance.
	 */
	private static final double GRID_TOLERANCE = 1E-8;

	/**
	 * The underlying name.
	 */
	private final String underlyingName;
	/**
	 * The maturity.
	 */
	private final double maturity;
	/**
	 * The strike.
	 */
	private final double strike;
	/**
	 * The barrier value.
	 */
	private final double barrierValue;
	/**
	 * The call or put sign.
	 */
	private final CallOrPut callOrPutSign;
	/**
	 * The barrier type.
	 */
	private final BarrierType barrierType;
	/**
	 * The digital payoff type.
	 */
	private final DigitalPayoffType digitalPayoffType;
	/**
	 * The cash payoff.
	 */
	private final double cashPayoff;
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

	private static final class DiscreteKnockInEventState {

		/**
		 * The activated vectors at event times.
		 */
		private final Map<Double, double[]> activatedVectorsAtEventTimes;

		private DiscreteKnockInEventState(final Map<Double, double[]> activatedVectorsAtEventTimes) {
			this.activatedVectorsAtEventTimes = activatedVectorsAtEventTimes;
		}
	}

	/**
	 * The discrete knock in event state stack.
	 */
	private transient ThreadLocal<ArrayDeque<DiscreteKnockInEventState>> discreteKnockInEventStateStack;

	/**
	 * Performs the operation.
	 *
	 * @param underlyingName The value.
	 * @param maturity The value.
	 * @param strike The value.
	 * @param barrierValue The value.
	 * @param callOrPutSign The value.
	 * @param barrierType The value.
	 * @param digitalPayoffType The value.
	 * @param cashPayoff The value.
	 * @param exercise The value.
	 */
	public DigitalBarrierOption(
			final String underlyingName,
			final double maturity,
			final double strike,
			final double barrierValue,
			final CallOrPut callOrPutSign,
			final BarrierType barrierType,
			final DigitalPayoffType digitalPayoffType,
			final double cashPayoff,
			final Exercise exercise) {
		this(
				underlyingName,
				maturity,
				strike,
				barrierValue,
				callOrPutSign,
				barrierType,
				digitalPayoffType,
				cashPayoff,
				exercise,
				MonitoringType.CONTINUOUS,
				null
				);
	}

	/**
	 * Performs the operation.
	 *
	 * @param underlyingName The value.
	 * @param maturity The value.
	 * @param strike The value.
	 * @param barrierValue The value.
	 * @param callOrPutSign The value.
	 * @param barrierType The value.
	 * @param digitalPayoffType The value.
	 * @param cashPayoff The value.
	 * @param exercise The value.
	 * @param monitoringType The value.
	 * @param monitoringTimes The value.
	 */
	public DigitalBarrierOption(
			final String underlyingName,
			final double maturity,
			final double strike,
			final double barrierValue,
			final CallOrPut callOrPutSign,
			final BarrierType barrierType,
			final DigitalPayoffType digitalPayoffType,
			final double cashPayoff,
			final Exercise exercise,
			final MonitoringType monitoringType,
			final double[] monitoringTimes) {

		if (callOrPutSign == null) {
			throw new IllegalArgumentException("Option type must not be null.");
		}
		if (barrierType == null) {
			throw new IllegalArgumentException("Barrier type must not be null.");
		}
		if (digitalPayoffType == null) {
			throw new IllegalArgumentException("Digital payoff type must not be null.");
		}
		if (exercise == null) {
			throw new IllegalArgumentException("Exercise must not be null.");
		}
		if (monitoringType == null) {
			throw new IllegalArgumentException("Monitoring type must not be null.");
		}
		if (!exercise.isEuropean() && !exercise.isBermudan() && !exercise.isAmerican()) {
			throw new IllegalArgumentException(
					"DigitalBarrierOption currently supports only European, Bermudan, and American exercise.");
		}
		if (maturity < 0.0) {
			throw new IllegalArgumentException("Maturity must be non-negative.");
		}
		if (digitalPayoffType == DigitalPayoffType.CASH_OR_NOTHING && cashPayoff < 0.0) {
			throw new IllegalArgumentException("Cash payoff must be non-negative.");
		}

		this.underlyingName = underlyingName;
		this.maturity = maturity;
		this.strike = strike;
		this.barrierValue = barrierValue;
		this.callOrPutSign = callOrPutSign;
		this.barrierType = barrierType;
		this.digitalPayoffType = digitalPayoffType;
		this.cashPayoff = digitalPayoffType == DigitalPayoffType.CASH_OR_NOTHING ? cashPayoff : Double.NaN;
		this.exercise = exercise;
		this.monitoringType = monitoringType;
		this.monitoringTimes = monitoringTimes == null ? null : monitoringTimes.clone();

		validateMonitoringSpecification();
	}

	/**
	 * Performs the operation.
	 *
	 * @param underlyingName The value.
	 * @param maturity The value.
	 * @param strike The value.
	 * @param barrierValue The value.
	 * @param callOrPutSign The value.
	 * @param barrierType The value.
	 * @param digitalPayoffType The value.
	 * @param cashPayoff The value.
	 */
	public DigitalBarrierOption(
			final String underlyingName,
			final double maturity,
			final double strike,
			final double barrierValue,
			final CallOrPut callOrPutSign,
			final BarrierType barrierType,
			final DigitalPayoffType digitalPayoffType,
			final double cashPayoff) {
		this(
				underlyingName,
				maturity,
				strike,
				barrierValue,
				callOrPutSign,
				barrierType,
				digitalPayoffType,
				cashPayoff,
				new EuropeanExercise(maturity)
				);
	}

	/**
	 * Performs the operation.
	 *
	 * @param underlyingName The value.
	 * @param maturity The value.
	 * @param strike The value.
	 * @param barrierValue The value.
	 * @param callOrPutSign The value.
	 * @param barrierType The value.
	 * @param digitalPayoffType The value.
	 * @param cashPayoff The value.
	 * @param monitoringType The value.
	 * @param monitoringTimes The value.
	 */
	public DigitalBarrierOption(
			final String underlyingName,
			final double maturity,
			final double strike,
			final double barrierValue,
			final CallOrPut callOrPutSign,
			final BarrierType barrierType,
			final DigitalPayoffType digitalPayoffType,
			final double cashPayoff,
			final MonitoringType monitoringType,
			final double[] monitoringTimes) {
		this(
				underlyingName,
				maturity,
				strike,
				barrierValue,
				callOrPutSign,
				barrierType,
				digitalPayoffType,
				cashPayoff,
				new EuropeanExercise(maturity),
				monitoringType,
				monitoringTimes
				);
	}

	/**
	 * Performs the operation.
	 *
	 * @param underlyingName The value.
	 * @param maturity The value.
	 * @param strike The value.
	 * @param barrierValue The value.
	 * @param callOrPutSign The value.
	 * @param barrierType The value.
	 * @param digitalPayoffType The value.
	 * @param cashPayoff The value.
	 */
	public DigitalBarrierOption(
			final String underlyingName,
			final double maturity,
			final double strike,
			final double barrierValue,
			final double callOrPutSign,
			final BarrierType barrierType,
			final DigitalPayoffType digitalPayoffType,
			final double cashPayoff) {
		this(
				underlyingName,
				maturity,
				strike,
				barrierValue,
				mapCallOrPut(callOrPutSign),
				barrierType,
				digitalPayoffType,
				cashPayoff,
				new EuropeanExercise(maturity)
				);
	}

	/**
	 * Performs the operation.
	 *
	 * @param maturity The value.
	 * @param strike The value.
	 * @param barrierValue The value.
	 * @param callOrPutSign The value.
	 * @param barrierType The value.
	 * @param digitalPayoffType The value.
	 * @param cashPayoff The value.
	 */
	public DigitalBarrierOption(
			final double maturity,
			final double strike,
			final double barrierValue,
			final CallOrPut callOrPutSign,
			final BarrierType barrierType,
			final DigitalPayoffType digitalPayoffType,
			final double cashPayoff) {
		this(
				null,
				maturity,
				strike,
				barrierValue,
				callOrPutSign,
				barrierType,
				digitalPayoffType,
				cashPayoff,
				new EuropeanExercise(maturity)
				);
	}

	/**
	 * Performs the operation.
	 *
	 * @param maturity The value.
	 * @param strike The value.
	 * @param barrierValue The value.
	 * @param callOrPutSign The value.
	 * @param barrierType The value.
	 * @param digitalPayoffType The value.
	 * @param cashPayoff The value.
	 * @param exercise The value.
	 */
	public DigitalBarrierOption(
			final double maturity,
			final double strike,
			final double barrierValue,
			final CallOrPut callOrPutSign,
			final BarrierType barrierType,
			final DigitalPayoffType digitalPayoffType,
			final double cashPayoff,
			final Exercise exercise) {
		this(
				null,
				maturity,
				strike,
				barrierValue,
				callOrPutSign,
				barrierType,
				digitalPayoffType,
				cashPayoff,
				exercise
				);
	}

	/**
	 * Performs the operation.
	 *
	 * @param maturity The value.
	 * @param strike The value.
	 * @param barrierValue The value.
	 * @param callOrPutSign The value.
	 * @param barrierType The value.
	 * @param digitalPayoffType The value.
	 * @param cashPayoff The value.
	 * @param exercise The value.
	 * @param monitoringType The value.
	 * @param monitoringTimes The value.
	 */
	public DigitalBarrierOption(
			final double maturity,
			final double strike,
			final double barrierValue,
			final CallOrPut callOrPutSign,
			final BarrierType barrierType,
			final DigitalPayoffType digitalPayoffType,
			final double cashPayoff,
			final Exercise exercise,
			final MonitoringType monitoringType,
			final double[] monitoringTimes) {
		this(
				null,
				maturity,
				strike,
				barrierValue,
				callOrPutSign,
				barrierType,
				digitalPayoffType,
				cashPayoff,
				exercise,
				monitoringType,
				monitoringTimes
				);
	}

	/**
	 * Performs the operation.
	 *
	 * @param maturity The value.
	 * @param strike The value.
	 * @param barrierValue The value.
	 * @param callOrPutSign The value.
	 * @param barrierType The value.
	 * @param digitalPayoffType The value.
	 * @param cashPayoff The value.
	 */
	public DigitalBarrierOption(
			final double maturity,
			final double strike,
			final double barrierValue,
			final double callOrPutSign,
			final BarrierType barrierType,
			final DigitalPayoffType digitalPayoffType,
			final double cashPayoff) {
		this(
				null,
				maturity,
				strike,
				barrierValue,
				mapCallOrPut(callOrPutSign),
				barrierType,
				digitalPayoffType,
				cashPayoff,
				new EuropeanExercise(maturity)
				);
	}

	/**
	 * Performs the operation.
	 *
	 * @param maturity The value.
	 * @param strike The value.
	 * @param barrierValue The value.
	 * @param callOrPutSign The value.
	 * @param barrierType The value.
	 * @param digitalPayoffType The value.
	 * @param cashPayoff The value.
	 * @param exercise The value.
	 */
	public DigitalBarrierOption(
			final double maturity,
			final double strike,
			final double barrierValue,
			final double callOrPutSign,
			final BarrierType barrierType,
			final DigitalPayoffType digitalPayoffType,
			final double cashPayoff,
			final Exercise exercise) {
		this(
				null,
				maturity,
				strike,
				barrierValue,
				mapCallOrPut(callOrPutSign),
				barrierType,
				digitalPayoffType,
				cashPayoff,
				exercise
				);
	}

	/**
	 * Performs the operation.
	 *
	 * @param maturity The value.
	 * @param strike The value.
	 * @param barrierValue The value.
	 * @param callOrPutSign The value.
	 * @param barrierType The value.
	 * @param digitalPayoffType The value.
	 * @param cashPayoff The value.
	 * @param exercise The value.
	 * @param monitoringType The value.
	 * @param monitoringTimes The value.
	 */
	public DigitalBarrierOption(
			final double maturity,
			final double strike,
			final double barrierValue,
			final double callOrPutSign,
			final BarrierType barrierType,
			final DigitalPayoffType digitalPayoffType,
			final double cashPayoff,
			final Exercise exercise,
			final MonitoringType monitoringType,
			final double[] monitoringTimes) {
		this(
				null,
				maturity,
				strike,
				barrierValue,
				mapCallOrPut(callOrPutSign),
				barrierType,
				digitalPayoffType,
				cashPayoff,
				exercise,
				monitoringType,
				monitoringTimes
				);
	}

	private ThreadLocal<ArrayDeque<DiscreteKnockInEventState>> getDiscreteKnockInEventStateStack() {
		if (discreteKnockInEventStateStack == null) {
			discreteKnockInEventStateStack = ThreadLocal.withInitial(ArrayDeque::new);
		}
		return discreteKnockInEventStateStack;
	}

	private void pushDiscreteKnockInEventState(final DiscreteKnockInEventState state) {
		getDiscreteKnockInEventStateStack().get().push(state);
	}

	private void popDiscreteKnockInEventState() {
		final ArrayDeque<DiscreteKnockInEventState> stack = getDiscreteKnockInEventStateStack().get();

		if (stack.isEmpty()) {
			throw new IllegalStateException("No discrete knock-in event state to pop.");
		}

		stack.pop();

		if (stack.isEmpty()) {
			getDiscreteKnockInEventStateStack().remove();
		}
	}

	private DiscreteKnockInEventState getCurrentDiscreteKnockInEventState() {
		final ArrayDeque<DiscreteKnockInEventState> stack = getDiscreteKnockInEventStateStack().get();
		return stack.isEmpty() ? null : stack.peek();
	}

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

	@Override
	public double[][] getValues(final FiniteDifferenceEquityModel model) {

		validateProductConfiguration(model);

		if (!usesDiscreteMonitoring()) {
			if (isDegenerateZeroCase()) {
				return buildZeroValueSurface(model);
			}

			if (isDegenerateVanillaCase()) {
				return createVanillaDigitalOption().getValues(model);
			}
		}

		final FiniteDifferenceEquityModel effectiveModel = getEffectiveModelForValuation(model);

		switch (getPricingMode(effectiveModel)) {
		case DIRECT_OUT:
			return priceOutOptionDirectly(effectiveModel);

		case DIRECT_IN_1D_TWO_STATE:
			return priceInOptionDirectly1D(effectiveModel);

		case DIRECT_IN_1D_DISCRETE_EVENT:
			return priceInOptionDiscrete1D(effectiveModel);

		case DIRECT_IN_2D_DISCRETE_EVENT:
			return priceInOptionDiscrete2D(effectiveModel);

		case DIRECT_IN_2D_PRE_HIT:
			return priceInOptionDirectly2D(effectiveModel);

		case PARITY_IN_FALLBACK:
			return priceInOptionByParity(effectiveModel);

		default:
			throw new IllegalStateException("Unsupported pricing mode.");
		}
	}

	private double[] applyEvaluationTimeDiscreteCondition(
			final double evaluationTime,
			final double[] valuesAtEvaluationTime,
			final FiniteDifferenceEquityModel model) {

		final int dims = model.getSpaceTimeDiscretization().getNumberOfSpaceGrids();
		final double[] adjustedValues = valuesAtEvaluationTime.clone();

		if (dims == 1) {
			final double[] xGrid = model.getSpaceTimeDiscretization().getSpaceGrid(0).getGrid();

			if (isOutOption()) {
				for (int i = 0; i < xGrid.length; i++) {
					if (isBarrierBreached(xGrid[i])) {
						adjustedValues[i] = 0.0;
					}
				}
				return adjustedValues;
			}

			final double[] activatedValuesAtEvaluationTime =
					createActivatedVanillaDigitalOption().getValue(evaluationTime, model);

			for (int i = 0; i < xGrid.length; i++) {
				if (isBarrierBreached(xGrid[i])) {
					adjustedValues[i] = activatedValuesAtEvaluationTime[i];
				}
			}
			return adjustedValues;
		}

		if (dims == 2) {
			final double[] x0 = model.getSpaceTimeDiscretization().getSpaceGrid(0).getGrid();
			final double[] x1 = model.getSpaceTimeDiscretization().getSpaceGrid(1).getGrid();
			final int n0 = x0.length;

			if (isOutOption()) {
				for (int j = 0; j < x1.length; j++) {
					for (int i = 0; i < x0.length; i++) {
						if (isBarrierBreached(x0[i])) {
							adjustedValues[flatten(i, j, n0)] = 0.0;
						}
					}
				}
				return adjustedValues;
			}

			final double[] activatedValuesAtEvaluationTime =
					createActivatedVanillaDigitalOption().getValue(evaluationTime, model);

			for (int j = 0; j < x1.length; j++) {
				for (int i = 0; i < x0.length; i++) {
					if (isBarrierBreached(x0[i])) {
						adjustedValues[flatten(i, j, n0)] =
								activatedValuesAtEvaluationTime[flatten(i, j, n0)];
					}
				}
			}
			return adjustedValues;
		}

		return adjustedValues;
	}

	private boolean isMonitoringTime(final double time) {
		if (!usesDiscreteMonitoring() || monitoringTimes == null) {
			return false;
		}

		final double tolerance = DiscreteMonitoringSupport.DEFAULT_MONITORING_TIME_TOLERANCE;
		for (final double monitoringTime : monitoringTimes) {
			if (Math.abs(monitoringTime - time) <= tolerance) {
				return true;
			}
		}
		return false;
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

			if (isOutOption()) {
				return applyDiscreteOutEvent1D(valuesAfterEvent, xGrid);
			}

			final DiscreteKnockInEventState state = getCurrentDiscreteKnockInEventState();
			if (state == null) {
				return valuesAfterEvent;
			}

			return applyDiscreteInEvent1D(time, valuesAfterEvent, xGrid, state);
		}

		if (dims == 2) {
			final double[] x0 = model.getSpaceTimeDiscretization().getSpaceGrid(0).getGrid();
			final double[] x1 = model.getSpaceTimeDiscretization().getSpaceGrid(1).getGrid();

			if (valuesAfterEvent.length != x0.length * x1.length) {
				throw new IllegalArgumentException(
						"Value vector length does not match the two-dimensional spatial grid.");
			}

			if (isOutOption()) {
				return applyDiscreteOutEvent2D(valuesAfterEvent, x0, x1);
			}

			final DiscreteKnockInEventState state = getCurrentDiscreteKnockInEventState();
			if (state == null) {
				return valuesAfterEvent;
			}

			return applyDiscreteInEvent2D(time, valuesAfterEvent, x0, x1, state);
		}

		throw new IllegalArgumentException("Only 1D and 2D grids are supported.");
	}

	private double[] applyDiscreteOutEvent1D(
			final double[] valuesAfterEvent,
			final double[] xGrid) {

		final double[] valuesBeforeEvent = valuesAfterEvent.clone();

		for (int i = 0; i < xGrid.length; i++) {
			if (isBarrierBreached(xGrid[i])) {
				valuesBeforeEvent[i] = 0.0;
			}
		}

		return valuesBeforeEvent;
	}

	private double[] applyDiscreteInEvent1D(
			final double time,
			final double[] valuesAfterEvent,
			final double[] xGrid,
			final DiscreteKnockInEventState state) {

		final double[] activatedVector = getActivatedVectorForEventTime(time, state);
		final double[] valuesBeforeEvent = valuesAfterEvent.clone();
		final boolean isExerciseTime = exercise.isExerciseAllowed(time);

		for (int i = 0; i < xGrid.length; i++) {
			if (!isBarrierBreached(xGrid[i])) {
				continue;
			}

			double value = activatedVector[i];

			if (isExerciseTime) {
				value = Math.max(value, pointwiseImmediateExercisePayoff(xGrid[i]));
			}

			valuesBeforeEvent[i] = value;
		}

		return valuesBeforeEvent;
	}

	private double[] applyDiscreteOutEvent2D(
			final double[] valuesAfterEvent,
			final double[] x0,
			final double[] x1) {

		final double[] valuesBeforeEvent = valuesAfterEvent.clone();
		final int n0 = x0.length;

		for (int j = 0; j < x1.length; j++) {
			for (int i = 0; i < x0.length; i++) {
				if (isBarrierBreached(x0[i])) {
					valuesBeforeEvent[flatten(i, j, n0)] = 0.0;
				}
			}
		}

		return valuesBeforeEvent;
	}

	private double[] applyDiscreteInEvent2D(
			final double time,
			final double[] valuesAfterEvent,
			final double[] x0,
			final double[] x1,
			final DiscreteKnockInEventState state) {

		final double[] activatedVector = getActivatedVectorForEventTime(time, state);
		final double[] valuesBeforeEvent = valuesAfterEvent.clone();
		final boolean isExerciseTime = exercise.isExerciseAllowed(time);
		final int n0 = x0.length;

		for (int j = 0; j < x1.length; j++) {
			for (int i = 0; i < x0.length; i++) {
				if (!isBarrierBreached(x0[i])) {
					continue;
				}

				double value = activatedVector[flatten(i, j, n0)];

				if (isExerciseTime) {
					value = Math.max(value, pointwiseImmediateExercisePayoff(x0[i]));
				}

				valuesBeforeEvent[flatten(i, j, n0)] = value;
			}
		}

		return valuesBeforeEvent;
	}

	private double[] getActivatedVectorForEventTime(
			final double time,
			final DiscreteKnockInEventState state) {

		if (state == null || state.activatedVectorsAtEventTimes == null) {
			throw new IllegalStateException(
					"Discrete knock-in event condition requires cached activated vectors."
					);
		}

		final double tolerance = DiscreteMonitoringSupport.DEFAULT_MONITORING_TIME_TOLERANCE;

		for (final Map.Entry<Double, double[]> entry : state.activatedVectorsAtEventTimes.entrySet()) {
			if (Math.abs(entry.getKey() - time) <= tolerance) {
				return entry.getValue();
			}
		}

		throw new IllegalArgumentException(
				"No cached activated vector found for event time " + time + "."
				);
	}

	private PricingMode getPricingMode(final FiniteDifferenceEquityModel model) {

		if (isOutOption()) {
			return PricingMode.DIRECT_OUT;
		}

		final int dims = model.getSpaceTimeDiscretization().getNumberOfSpaceGrids();

		if (usesDiscreteMonitoring()) {
			if (dims == 1) {
				return PricingMode.DIRECT_IN_1D_DISCRETE_EVENT;
			}
			if (dims == 2 && supportsDirect2DDiscreteMonitoring(model)) {
				return PricingMode.DIRECT_IN_2D_DISCRETE_EVENT;
			}
			return PricingMode.PARITY_IN_FALLBACK;
		}

		if (dims == 1) {
			return PricingMode.DIRECT_IN_1D_TWO_STATE;
		}

		if (dims == 2 && supportsDirect2DKnockIn(model)) {
			return PricingMode.DIRECT_IN_2D_PRE_HIT;
		}

		return PricingMode.PARITY_IN_FALLBACK;
	}

	private boolean supportsDirect2DKnockIn(final FiniteDifferenceEquityModel model) {
		return model instanceof FDMHestonModel || model instanceof FDMSabrModel;
	}

	private boolean supportsDirect2DDiscreteMonitoring(final FiniteDifferenceEquityModel model) {
		return model instanceof FDMHestonModel || model instanceof FDMSabrModel;
	}

	private PricingMode getPricingModeForCellAveraging() {
		if (isOutOption()) {
			return PricingMode.DIRECT_OUT;
		}
		if (usesDiscreteMonitoring()) {
			return PricingMode.DIRECT_IN_1D_DISCRETE_EVENT;
		}
		return PricingMode.DIRECT_IN_1D_TWO_STATE;
	}

	private void validateProductConfiguration(final FiniteDifferenceEquityModel model) {
		validateBarrierInsideGrid(model);

		if (!exercise.isEuropean() && !exercise.isBermudan() && !exercise.isAmerican()) {
			throw new IllegalArgumentException(
					"DigitalBarrierOption currently supports only European, Bermudan, and American exercise.");
		}

		if (usesDiscreteMonitoring()) {
			validateDiscreteMonitoringScope(model);
		}
	}

	private void validateDiscreteMonitoringScope(final FiniteDifferenceEquityModel model) {
		final int dims = model.getSpaceTimeDiscretization().getNumberOfSpaceGrids();

		if (dims == 1) {
			if (!exercise.isEuropean() && !exercise.isBermudan() && !exercise.isAmerican()) {
				throw new IllegalArgumentException(
						"Discrete monitoring for DigitalBarrierOption currently supports only European, Bermudan, and American exercise.");
			}
			return;
		}

		if (dims == 2 && supportsDirect2DDiscreteMonitoring(model)) {
			if (!exercise.isEuropean() && !exercise.isBermudan() && !exercise.isAmerican()) {
				throw new IllegalArgumentException(
						"Discrete monitoring for 2D DigitalBarrierOption currently supports only European, Bermudan, and American exercise.");
			}
			return;
		}

		throw new IllegalArgumentException(
				"Discrete monitoring is currently supported only for 1D models and for 2D Heston/SABR models.");
	}

	private double[][] buildZeroValueSurface(final FiniteDifferenceEquityModel model) {
		final int numberOfSpacePoints = getTotalNumberOfSpacePoints(model.getSpaceTimeDiscretization());
		final int numberOfTimePoints =
				model.getSpaceTimeDiscretization().getTimeDiscretization().getNumberOfTimeSteps() + 1;

		final double[][] zeroValues = new double[numberOfSpacePoints][numberOfTimePoints];
		for (int i = 0; i < numberOfSpacePoints; i++) {
			for (int j = 0; j < numberOfTimePoints; j++) {
				zeroValues[i][j] = 0.0;
			}
		}
		return zeroValues;
	}

	private double[][] priceOutOptionDirectly(final FiniteDifferenceEquityModel model) {
		final FDMSolver solver = FDMSolverFactory.createSolver(
				model,
				this,
				model.getSpaceTimeDiscretization(),
				exercise
				);

		final boolean isOneDimensional =
				model.getSpaceTimeDiscretization().getNumberOfSpaceGrids() == 1;

		if (isOneDimensional) {
			final double[] terminalValues =
					buildCellAveragedTerminalValues(model.getSpaceTimeDiscretization());

			if (exercise.isEuropean()) {
				return solver.getValues(maturity, terminalValues);
			}

			return solver.getValues(
					maturity,
					terminalValues,
					this::pointwisePayoffForDirectOutPricing
					);
		}

		return solver.getValues(maturity, this::pointwisePayoffForDirectOutPricing);
	}

	private double[][] priceInOptionDirectly1D(final FiniteDifferenceEquityModel model) {

		final int numberOfSpaceDimensions = model.getSpaceTimeDiscretization().getNumberOfSpaceGrids();

		if (barrierType != BarrierType.DOWN_IN && barrierType != BarrierType.UP_IN) {
			throw new IllegalStateException(
					"priceInOptionDirectly1D was called for a non knock-in barrier type.");
		}

		if (numberOfSpaceDimensions != 1) {
			throw new IllegalArgumentException("priceInOptionDirectly1D requires a 1D model.");
		}

		final FiniteDifferenceEquityModel knockInModel = createAuxiliaryKnockInModel1D(model);

		final FDMSolver solver = new FDMThetaMethod1DTwoState(
				knockInModel,
				this,
				knockInModel.getSpaceTimeDiscretization(),
				exercise,
				ActiveBoundaryProviderFactory.createProvider(
						knockInModel,
						strike,
						maturity,
						callOrPutSign
						)
				);

		final double[][] knockInValuesOnAuxiliaryGrid = solver.getValues(
				maturity,
				buildCellAveragedTerminalValues(knockInModel.getSpaceTimeDiscretization())
				);

		return interpolateSurfaceToOriginalGrid1D(
				knockInValuesOnAuxiliaryGrid,
				knockInModel.getSpaceTimeDiscretization().getSpaceGrid(0).getGrid(),
				model.getSpaceTimeDiscretization().getSpaceGrid(0).getGrid()
				);
	}

	private double[][] priceInOptionDiscrete1D(final FiniteDifferenceEquityModel model) {

		if (barrierType != BarrierType.DOWN_IN && barrierType != BarrierType.UP_IN) {
			throw new IllegalStateException(
					"priceInOptionDiscrete1D was called for a non knock-in barrier type.");
		}

		if (!usesDiscreteMonitoring()) {
			throw new IllegalStateException(
					"priceInOptionDiscrete1D requires discrete monitoring.");
		}

		final DigitalOption activatedDigital = createActivatedVanillaDigitalOption();

		final Map<Double, double[]> activatedVectorsAtEventTimes = new HashMap<>();
		for (final double eventTime : monitoringTimes) {
			activatedVectorsAtEventTimes.put(
					eventTime,
					activatedDigital.getValue(eventTime, model).clone()
					);
		}

		pushDiscreteKnockInEventState(new DiscreteKnockInEventState(activatedVectorsAtEventTimes));

		try {
			final FDMSolver solver = FDMSolverFactory.createSolver(
					model,
					this,
					model.getSpaceTimeDiscretization(),
					new EuropeanExercise(maturity)
					);

			return solver.getValues(
					maturity,
					DiscreteKnockInActivationSupport.buildZeroTerminalValues(
							model.getSpaceTimeDiscretization()
							)
					);
		} finally {
			popDiscreteKnockInEventState();
		}
	}

	private double[][] priceInOptionDiscrete2D(final FiniteDifferenceEquityModel model) {

		if (barrierType != BarrierType.DOWN_IN && barrierType != BarrierType.UP_IN) {
			throw new IllegalStateException(
					"priceInOptionDiscrete2D was called for a non knock-in barrier type.");
		}

		if (model.getSpaceTimeDiscretization().getNumberOfSpaceGrids() != 2) {
			throw new IllegalArgumentException("priceInOptionDiscrete2D requires a 2D model.");
		}

		if (!usesDiscreteMonitoring()) {
			throw new IllegalStateException(
					"priceInOptionDiscrete2D requires discrete monitoring.");
		}

		if (!supportsDirect2DDiscreteMonitoring(model)) {
			return priceInOptionByParity(model);
		}

		final DigitalOption activatedDigital = createActivatedVanillaDigitalOption();

		final Map<Double, double[]> activatedVectorsAtEventTimes = new HashMap<>();
		for (final double eventTime : monitoringTimes) {
			activatedVectorsAtEventTimes.put(
					eventTime,
					activatedDigital.getValue(eventTime, model).clone()
					);
		}

		pushDiscreteKnockInEventState(new DiscreteKnockInEventState(activatedVectorsAtEventTimes));

		try {
			final FDMSolver solver = FDMSolverFactory.createSolver(
					model,
					this,
					model.getSpaceTimeDiscretization(),
					new EuropeanExercise(maturity)
					);

			return solver.getValues(maturity, assetValue -> getInactiveValueAtMaturity());
		} finally {
			popDiscreteKnockInEventState();
		}
	}

	private double[][] priceInOptionDirectly2D(final FiniteDifferenceEquityModel model) {

		if (barrierType != BarrierType.DOWN_IN && barrierType != BarrierType.UP_IN) {
			throw new IllegalStateException(
					"priceInOptionDirectly2D was called for a non knock-in barrier type.");
		}

		if (model.getSpaceTimeDiscretization().getNumberOfSpaceGrids() != 2) {
			throw new IllegalArgumentException("priceInOptionDirectly2D requires a 2D model.");
		}

		if (!supportsDirect2DKnockIn(model)) {
			return priceInOptionByParity(model);
		}

		final FiniteDifferenceEquityModel effectiveBarrierModel =
				getEffectiveModelForTwoDimensionalKnockIn(model);

		final FiniteDifferenceEquityModel activatedModel =
				createAuxiliaryActivatedModel2D(effectiveBarrierModel);

		final DigitalOption activatedProduct = createActivatedVanillaDigitalOption();

		final FDMSolver activatedSolver = FDMSolverFactory.createSolver(
				activatedModel,
				activatedProduct,
				activatedModel.getSpaceTimeDiscretization(),
				exercise
				);

		final double[][] activatedValues =
				activatedSolver.getValues(maturity, this::pointwiseImmediateExercisePayoff);

		final ActivatedBarrierTrace2D trace =
				extractActivatedBarrierTrace2D(activatedModel, activatedValues);

		final BarrierPreHitSpecification preHitSpecification =
				createPreHitSpecification(effectiveBarrierModel, trace);

		final FiniteDifferenceEquityModel preHitModel =
				createAuxiliaryPreHitModel2D(effectiveBarrierModel, preHitSpecification);

		final FDMSolver preHitSolver =
				FDMSolverFactory.createSolver(
						preHitModel,
						this,
						preHitModel.getSpaceTimeDiscretization(),
						new EuropeanExercise(maturity),
						BarrierPDEMode.IN_PRE_HIT,
						preHitSpecification
						);

		final double[][] preHitValues =
				preHitSolver.getValues(maturity, assetValue -> getInactiveValueAtMaturity());

		return assembleDirectKnockInSurface2D(
				model,
				activatedModel,
				activatedValues,
				preHitModel,
				preHitValues
				);
	}

	private SpaceTimeDiscretization getValuationSpaceTimeDiscretization(
			final FiniteDifferenceEquityModel model) {

		final SpaceTimeDiscretization base = model.getSpaceTimeDiscretization();

		if (!exercise.isBermudan() && !usesDiscreteMonitoring()) {
			return base;
		}

		TimeDiscretization refinedTimeDiscretization = base.getTimeDiscretization();

		if (exercise.isBermudan()) {
			refinedTimeDiscretization =
					FiniteDifferenceExerciseUtil.refineTimeDiscretization(
							refinedTimeDiscretization,
							exercise
							);
		}

		if (usesDiscreteMonitoring()) {
			refinedTimeDiscretization =
					DiscreteMonitoringSupport.refineTimeDiscretizationWithMonitoring(
							refinedTimeDiscretization,
							maturity,
							monitoringTimes
							);
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

	private FiniteDifferenceEquityModel getEffectiveModelForValuation(
			final FiniteDifferenceEquityModel model) {

		final SpaceTimeDiscretization effectiveDiscretization =
				getValuationSpaceTimeDiscretization(model);

		if (effectiveDiscretization == model.getSpaceTimeDiscretization()) {
			return model;
		}

		return model.getCloneWithModifiedSpaceTimeDiscretization(effectiveDiscretization);
	}

	private FiniteDifferenceEquityModel getEffectiveModelForTwoDimensionalKnockIn(
			final FiniteDifferenceEquityModel model) {

		final SpaceTimeDiscretization effectiveDiscretization =
				getValuationSpaceTimeDiscretization(model);

		if (effectiveDiscretization == model.getSpaceTimeDiscretization()) {
			return model;
		}

		return model.getCloneWithModifiedSpaceTimeDiscretization(effectiveDiscretization);
	}

	private FiniteDifferenceEquityModel createAuxiliaryActivatedModel2D(
			final FiniteDifferenceEquityModel barrierModel) {

		final SpaceTimeDiscretization base = barrierModel.getSpaceTimeDiscretization();
		final double[] baseSpotGrid = base.getSpaceGrid(0).getGrid();

		if (baseSpotGrid.length < 2) {
			throw new IllegalArgumentException("Barrier grid must contain at least two points.");
		}

		final boolean forceWidenedActivatedGrid =
				(barrierModel instanceof FDMHestonModel || barrierModel instanceof FDMSabrModel)
				&& !exercise.isEuropean()
				&& barrierType == BarrierType.DOWN_IN
				&& callOrPutSign == CallOrPut.PUT;

		boolean barrierAlreadyOnSpotGrid = false;
		for (final double s : baseSpotGrid) {
			if (Math.abs(s - barrierValue) <= GRID_TOLERANCE) {
				barrierAlreadyOnSpotGrid = true;
				break;
			}
		}

		if (barrierAlreadyOnSpotGrid && !forceWidenedActivatedGrid) {
			return barrierModel;
		}

		final double[] secondGrid = base.getSpaceGrid(1).getGrid();

		final Grid activatedSpotGrid = buildActivatedSpotGridAlignedAtBarrier(
				baseSpotGrid,
				barrierModel.getInitialValue()[0]
				);

		final Grid preservedSecondGrid = new UniformGrid(
				secondGrid.length - 1,
				secondGrid[0],
				secondGrid[secondGrid.length - 1]
				);

		final SpaceTimeDiscretization activatedDiscretization =
				new SpaceTimeDiscretization(
						new Grid[] {activatedSpotGrid, preservedSecondGrid },
						base.getTimeDiscretization(),
						base.getTheta(),
						barrierModel.getInitialValue()
						);

		return barrierModel.getCloneWithModifiedSpaceTimeDiscretization(activatedDiscretization);
	}

	private Grid buildActivatedSpotGridAlignedAtBarrier(
			final double[] baseSpotGrid,
			final double initialSpot) {

		final double deltaS = baseSpotGrid[1] - baseSpotGrid[0];
		final double currentMin = baseSpotGrid[0];
		final double currentMax = baseSpotGrid[baseSpotGrid.length - 1];
		final double currentHalfWidth = Math.max(initialSpot - currentMin, currentMax - initialSpot);

		final double targetMin = Math.max(1E-8, initialSpot - 2.0 * currentHalfWidth);
		final double targetMax = initialSpot + 2.0 * currentHalfWidth;

		final int leftSteps = Math.max(1, (int)Math.ceil((barrierValue - targetMin) / deltaS));
		final int rightSteps = Math.max(1, (int)Math.ceil((targetMax - barrierValue) / deltaS));

		final double sMin = barrierValue - leftSteps * deltaS;
		final double sMax = barrierValue + rightSteps * deltaS;

		return new UniformGrid(leftSteps + rightSteps, sMin, sMax);
	}

	private BarrierPreHitSpecification createPreHitSpecification(
			final FiniteDifferenceEquityModel barrierModel,
			final ActivatedBarrierTrace2D trace) {

		final double[] originalSpotGrid =
				barrierModel.getSpaceTimeDiscretization().getSpaceGrid(0).getGrid();
		final double deltaS = originalSpotGrid[1] - originalSpotGrid[0];

		final double spotMin;
		final double spotMax;
		final int numberOfSpotSteps;
		final int barrierSpotIndex;

		if (barrierType == BarrierType.DOWN_IN) {
			spotMin = barrierValue;
			numberOfSpotSteps = (int)Math.ceil((originalSpotGrid[originalSpotGrid.length - 1] - barrierValue) / deltaS);
			spotMax = barrierValue + numberOfSpotSteps * deltaS;
			barrierSpotIndex = 0;
		} else if (barrierType == BarrierType.UP_IN) {
			spotMax = barrierValue;
			numberOfSpotSteps = (int)Math.ceil((barrierValue - originalSpotGrid[0]) / deltaS);
			spotMin = barrierValue - numberOfSpotSteps * deltaS;
			barrierSpotIndex = numberOfSpotSteps;
		} else {
			throw new IllegalArgumentException("Pre-hit specification requested for non knock-in type.");
		}

		return new BarrierPreHitSpecification(
				barrierType,
				barrierValue,
				barrierSpotIndex,
				spotMin,
				spotMax,
				numberOfSpotSteps,
				trace
				);
	}

	private FiniteDifferenceEquityModel createAuxiliaryPreHitModel2D(
			final FiniteDifferenceEquityModel barrierModel,
			final BarrierPreHitSpecification preHitSpecification) {

		final SpaceTimeDiscretization base = barrierModel.getSpaceTimeDiscretization();
		final double[] secondGrid = base.getSpaceGrid(1).getGrid();

		final Grid preHitSpotGrid = new UniformGrid(
				preHitSpecification.getNumberOfSpotSteps(),
				preHitSpecification.getSpotMin(),
				preHitSpecification.getSpotMax()
				);

		final Grid preservedSecondGrid = new UniformGrid(
				secondGrid.length - 1,
				secondGrid[0],
				secondGrid[secondGrid.length - 1]
				);

		final SpaceTimeDiscretization preHitDiscretization =
				new SpaceTimeDiscretization(
						new Grid[] {preHitSpotGrid, preservedSecondGrid },
						base.getTimeDiscretization(),
						base.getTheta(),
						barrierModel.getInitialValue()
						);

		return barrierModel.getCloneWithModifiedSpaceTimeDiscretization(preHitDiscretization);
	}

	private ActivatedBarrierTrace2D extractActivatedBarrierTrace2D(
			final FiniteDifferenceEquityModel activatedModel,
			final double[][] activatedValues) {

		final SpaceTimeDiscretization disc = activatedModel.getSpaceTimeDiscretization();
		final double[] x0 = disc.getSpaceGrid(0).getGrid();
		final double[] x1 = disc.getSpaceGrid(1).getGrid();

		final int barrierIndex = findExactGridIndex(x0, barrierValue);

		final int timeCount = activatedValues[0].length;
		final double[][] traceValues = new double[x1.length][timeCount];

		for (int j = 0; j < x1.length; j++) {
			final int k = flatten(barrierIndex, j, x0.length);
			for (int timeIndex = 0; timeIndex < timeCount; timeIndex++) {
				traceValues[j][timeIndex] = activatedValues[k][timeIndex];
			}
		}

		return new ActivatedBarrierTrace2D(
				barrierValue,
				x1,
				disc.getTimeDiscretization(),
				traceValues
				);
	}

	private int findExactGridIndex(final double[] grid, final double x) {
		for (int i = 0; i < grid.length; i++) {
			if (Math.abs(grid[i] - x) <= GRID_TOLERANCE) {
				return i;
			}
		}
		throw new IllegalArgumentException("Expected exact barrier node not found on auxiliary grid.");
	}

	private double[][] assembleDirectKnockInSurface2D(
			final FiniteDifferenceEquityModel originalModel,
			final FiniteDifferenceEquityModel activatedModel,
			final double[][] activatedValues,
			final FiniteDifferenceEquityModel preHitModel,
			final double[][] preHitValues) {

		final SpaceTimeDiscretization originalDiscretization = originalModel.getSpaceTimeDiscretization();

		final double[][] activatedOnOriginalGrid =
				interpolateSurfaceToOriginalGrid2DAlongFirstState(
						activatedValues,
						activatedModel.getSpaceTimeDiscretization(),
						originalDiscretization
						);

		final double[][] preHitOnOriginalGrid =
				interpolateSurfaceToOriginalGrid2DAlongFirstState(
						preHitValues,
						preHitModel.getSpaceTimeDiscretization(),
						originalDiscretization
						);

		final double[] x0 = originalDiscretization.getSpaceGrid(0).getGrid();
		final double[] x1 = originalDiscretization.getSpaceGrid(1).getGrid();

		final int numberOfColumns = activatedOnOriginalGrid[0].length;
		final double[][] result = new double[x0.length * x1.length][numberOfColumns];

		for (int j = 0; j < x1.length; j++) {
			for (int i = 0; i < x0.length; i++) {
				final boolean alreadyHit =
						barrierType == BarrierType.DOWN_IN
						? x0[i] <= barrierValue
						: x0[i] >= barrierValue;

						final int k = flatten(i, j, x0.length);
						final double[][] source = alreadyHit ? activatedOnOriginalGrid : preHitOnOriginalGrid;

						for (int timeIndex = 0; timeIndex < numberOfColumns; timeIndex++) {
							result[k][timeIndex] = source[k][timeIndex];
						}
			}
		}

		return result;
	}

	private double[][] priceInOptionByParity(final FiniteDifferenceEquityModel barrierModel) {

		final DigitalOption vanillaOption = createVanillaDigitalOption();
		final DigitalBarrierOption correspondingOutOption = createCorrespondingOutOption();

		final double[][] outValues = correspondingOutOption.getValues(barrierModel);

		final FiniteDifferenceEquityModel vanillaModel = createAuxiliaryVanillaModel(barrierModel);
		final double[][] vanillaValues = vanillaOption.getValues(vanillaModel);

		final SpaceTimeDiscretization barrierDiscretization = barrierModel.getSpaceTimeDiscretization();
		final SpaceTimeDiscretization vanillaDiscretization = vanillaModel.getSpaceTimeDiscretization();

		final int dims = barrierDiscretization.getNumberOfSpaceGrids();

		if (dims == 1) {
			final double[] barrierGrid = barrierDiscretization.getSpaceGrid(0).getGrid();
			final double[] vanillaGrid = vanillaDiscretization.getSpaceGrid(0).getGrid();

			final int numberOfColumns = outValues[0].length;
			final double[][] inValues = new double[outValues.length][numberOfColumns];

			for (int timeIndex = 0; timeIndex < numberOfColumns; timeIndex++) {
				final RationalFunctionInterpolation interpolator = new RationalFunctionInterpolation(
						vanillaGrid,
						getColumn(vanillaValues, timeIndex),
						InterpolationMethod.LINEAR,
						ExtrapolationMethod.CONSTANT
						);

				for (int i = 0; i < barrierGrid.length; i++) {
					final double stock = barrierGrid[i];
					final double vanillaValue = interpolator.getValue(stock);
					inValues[i][timeIndex] = vanillaValue - outValues[i][timeIndex];
				}
			}

			return inValues;
		} else if (dims == 2) {
			final double[][] vanillaOnBarrierGrid = interpolateSurfaceToOriginalGrid2DAlongFirstState(
					vanillaValues,
					vanillaDiscretization,
					barrierDiscretization
					);

			final int numberOfRows = outValues.length;
			final int numberOfColumns = outValues[0].length;
			final double[][] inValues = new double[numberOfRows][numberOfColumns];

			for (int i = 0; i < numberOfRows; i++) {
				for (int j = 0; j < numberOfColumns; j++) {
					inValues[i][j] = vanillaOnBarrierGrid[i][j] - outValues[i][j];
				}
			}

			return inValues;
		} else {
			throw new IllegalArgumentException("Only 1D and 2D grids are supported.");
		}
	}

	private double[] buildCellAveragedTerminalValues(final SpaceTimeDiscretization discretization) {
		final double[] sGrid = discretization.getSpaceGrid(0).getGrid();
		final double[] terminalValues = new double[sGrid.length];

		for (int i = 0; i < sGrid.length; i++) {
			final double leftEdge = getLeftDualCellEdge(sGrid, i);
			final double rightEdge = getRightDualCellEdge(sGrid, i);
			terminalValues[i] = cellAveragedPayoffForProductMode(leftEdge, rightEdge);
		}

		return terminalValues;
	}

	private double cellAveragedPayoffForProductMode(final double leftEdge, final double rightEdge) {
		switch (getPricingModeForCellAveraging()) {
		case DIRECT_OUT:
			return cellAveragedPayoffForDirectOutPricing(leftEdge, rightEdge);

		case DIRECT_IN_1D_TWO_STATE:
		case DIRECT_IN_1D_DISCRETE_EVENT:
		case DIRECT_IN_2D_DISCRETE_EVENT:
		case DIRECT_IN_2D_PRE_HIT:
		case PARITY_IN_FALLBACK:
			return cellAveragedPureDigital(leftEdge, rightEdge);

		default:
			throw new IllegalStateException("Unsupported pricing mode.");
		}
	}

	private double pointwisePayoffForDirectOutPricing(final double assetValue) {
		if (!isAliveAtExerciseOrMaturityForOutOption(assetValue)) {
			return 0.0;
		}
		return pointwiseImmediateExercisePayoff(assetValue);
	}

	private double pointwiseImmediateExercisePayoff(final double assetValue) {
		final boolean inTheMoney =
				callOrPutSign == CallOrPut.CALL
				? assetValue > strike
						: assetValue < strike;

				if (!inTheMoney) {
					return 0.0;
				}

				switch (digitalPayoffType) {
				case CASH_OR_NOTHING:
					return cashPayoff;
				case ASSET_OR_NOTHING:
					return assetValue;
				default:
					throw new IllegalStateException("Unsupported digital payoff type.");
				}
	}

	private double cellAveragedPureDigital(final double leftEdge, final double rightEdge) {
		if (!(leftEdge < rightEdge)) {
			throw new IllegalArgumentException("Require leftEdge < rightEdge.");
		}

		switch (digitalPayoffType) {
		case CASH_OR_NOTHING:
			return cellAveragedCashDigital(leftEdge, rightEdge);

		case ASSET_OR_NOTHING:
			return cellAveragedAssetDigital(leftEdge, rightEdge);

		default:
			throw new IllegalStateException("Unsupported digital payoff type.");
		}
	}

	private double cellAveragedCashDigital(final double leftEdge, final double rightEdge) {
		final double width = rightEdge - leftEdge;

		if (callOrPutSign == CallOrPut.CALL) {
			if (rightEdge <= strike) {
				return 0.0;
			}
			if (leftEdge >= strike) {
				return cashPayoff;
			}
			return cashPayoff * (rightEdge - strike) / width;
		} else {
			if (leftEdge >= strike) {
				return 0.0;
			}
			if (rightEdge <= strike) {
				return cashPayoff;
			}
			return cashPayoff * (strike - leftEdge) / width;
		}
	}

	private double cellAveragedAssetDigital(final double leftEdge, final double rightEdge) {
		final double width = rightEdge - leftEdge;

		if (callOrPutSign == CallOrPut.CALL) {
			if (rightEdge <= strike) {
				return 0.0;
			}
			if (leftEdge >= strike) {
				return 0.5 * (leftEdge + rightEdge);
			}

			final double lower = strike;
			return 0.5 * (rightEdge * rightEdge - lower * lower) / width;
		} else {
			if (leftEdge >= strike) {
				return 0.0;
			}
			if (rightEdge <= strike) {
				return 0.5 * (leftEdge + rightEdge);
			}

			final double upper = strike;
			return 0.5 * (upper * upper - leftEdge * leftEdge) / width;
		}
	}

	private double cellAveragedPayoffForDirectOutPricing(final double leftEdge, final double rightEdge) {
		if (!(leftEdge < rightEdge)) {
			throw new IllegalArgumentException("Require leftEdge < rightEdge.");
		}

		final double width = rightEdge - leftEdge;

		switch (barrierType) {
		case DOWN_OUT:
			return averageDigitalPayoffOverInterval(
					Math.max(leftEdge, barrierValue),
					rightEdge,
					width
					);

		case UP_OUT:
			return averageDigitalPayoffOverInterval(
					leftEdge,
					Math.min(rightEdge, barrierValue),
					width
					);

		default:
			throw new IllegalArgumentException("Direct out payoff requested for non out-option type.");
		}
	}

	private double averageDigitalPayoffOverInterval(
			final double aliveLeft,
			final double aliveRight,
			final double totalWidth) {

		if (aliveRight <= aliveLeft) {
			return 0.0;
		}

		switch (digitalPayoffType) {
		case CASH_OR_NOTHING:
			return averageCashDigitalOverAliveInterval(aliveLeft, aliveRight, totalWidth);

		case ASSET_OR_NOTHING:
			return averageAssetDigitalOverAliveInterval(aliveLeft, aliveRight, totalWidth);

		default:
			throw new IllegalStateException("Unsupported digital payoff type.");
		}
	}

	private double averageCashDigitalOverAliveInterval(
			final double aliveLeft,
			final double aliveRight,
			final double totalWidth) {

		final double lower;
		final double upper;

		if (callOrPutSign == CallOrPut.CALL) {
			lower = Math.max(aliveLeft, strike);
			upper = aliveRight;
		} else {
			lower = aliveLeft;
			upper = Math.min(aliveRight, strike);
		}

		if (upper <= lower) {
			return 0.0;
		}

		return cashPayoff * (upper - lower) / totalWidth;
	}

	private double averageAssetDigitalOverAliveInterval(
			final double aliveLeft,
			final double aliveRight,
			final double totalWidth) {

		final double lower;
		final double upper;

		if (callOrPutSign == CallOrPut.CALL) {
			lower = Math.max(aliveLeft, strike);
			upper = aliveRight;
		} else {
			lower = aliveLeft;
			upper = Math.min(aliveRight, strike);
		}

		if (upper <= lower) {
			return 0.0;
		}

		return 0.5 * (upper * upper - lower * lower) / totalWidth;
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

	private double[][] interpolateSurfaceToOriginalGrid1D(
			final double[][] valuesOnAuxiliaryGrid,
			final double[] auxiliaryGrid,
			final double[] originalGrid) {

		final int numberOfColumns = valuesOnAuxiliaryGrid[0].length;
		final double[][] interpolatedValues = new double[originalGrid.length][numberOfColumns];

		for (int timeIndex = 0; timeIndex < numberOfColumns; timeIndex++) {
			final RationalFunctionInterpolation interpolator = new RationalFunctionInterpolation(
					auxiliaryGrid,
					getColumn(valuesOnAuxiliaryGrid, timeIndex),
					InterpolationMethod.LINEAR,
					ExtrapolationMethod.CONSTANT
					);

			for (int i = 0; i < originalGrid.length; i++) {
				interpolatedValues[i][timeIndex] = interpolator.getValue(originalGrid[i]);
			}
		}

		return interpolatedValues;
	}

	private double[][] interpolateSurfaceToOriginalGrid2DAlongFirstState(
			final double[][] valuesOnAuxiliaryGrid,
			final SpaceTimeDiscretization auxiliaryDiscretization,
			final SpaceTimeDiscretization originalDiscretization) {

		final double[] auxiliaryX0 = auxiliaryDiscretization.getSpaceGrid(0).getGrid();
		final double[] auxiliaryX1 = auxiliaryDiscretization.getSpaceGrid(1).getGrid();

		final double[] originalX0 = originalDiscretization.getSpaceGrid(0).getGrid();
		final double[] originalX1 = originalDiscretization.getSpaceGrid(1).getGrid();

		if (auxiliaryX1.length != originalX1.length) {
			throw new IllegalArgumentException(
					"2D digital knock-in interpolation currently requires the second state-variable grid to remain unchanged.");
		}

		for (int j = 0; j < originalX1.length; j++) {
			if (Math.abs(auxiliaryX1[j] - originalX1[j]) > 1E-12) {
				throw new IllegalArgumentException(
						"2D digital knock-in interpolation currently requires the second state-variable grid to remain unchanged.");
			}
		}

		final int auxiliaryN0 = auxiliaryX0.length;
		final int originalN0 = originalX0.length;
		final int originalN1 = originalX1.length;

		final int numberOfColumns = valuesOnAuxiliaryGrid[0].length;
		final double[][] interpolatedValues = new double[originalN0 * originalN1][numberOfColumns];

		for (int timeIndex = 0; timeIndex < numberOfColumns; timeIndex++) {
			for (int j = 0; j < originalN1; j++) {

				final double[] auxiliarySlice = new double[auxiliaryN0];
				for (int i = 0; i < auxiliaryN0; i++) {
					final int k = flatten(i, j, auxiliaryN0);
					auxiliarySlice[i] = valuesOnAuxiliaryGrid[k][timeIndex];
				}

				final RationalFunctionInterpolation interpolator = new RationalFunctionInterpolation(
						auxiliaryX0,
						auxiliarySlice,
						InterpolationMethod.LINEAR,
						ExtrapolationMethod.CONSTANT
						);

				for (int i = 0; i < originalN0; i++) {
					final int k = flatten(i, j, originalN0);
					interpolatedValues[k][timeIndex] = interpolator.getValue(originalX0[i]);
				}
			}
		}

		return interpolatedValues;
	}

	private FiniteDifferenceEquityModel createAuxiliaryKnockInModel1D(final FiniteDifferenceEquityModel originalModel) {

		final SpaceTimeDiscretization originalDiscretization = originalModel.getSpaceTimeDiscretization();
		final TimeDiscretization timeDiscretization = originalDiscretization.getTimeDiscretization();
		final double thetaValue = originalDiscretization.getTheta();

		final double[] originalGrid = originalDiscretization.getSpaceGrid(0).getGrid();
		if (originalGrid.length < 2) {
			throw new IllegalArgumentException("Barrier grid must contain at least two points.");
		}

		final double deltaS = originalGrid[1] - originalGrid[0];
		final int numberOfSteps = originalGrid.length - 1;
		final int extraStepsBeyondBarrier = getKnockInExtraStepsBeyondBarrier1D();

		final double sMin;
		final double sMax;

		if (barrierType == BarrierType.DOWN_IN) {
			sMin = barrierValue - extraStepsBeyondBarrier * deltaS;
			sMax = sMin + numberOfSteps * deltaS;
		} else if (barrierType == BarrierType.UP_IN) {
			sMax = barrierValue + extraStepsBeyondBarrier * deltaS;
			sMin = sMax - numberOfSteps * deltaS;
		} else {
			throw new IllegalArgumentException("Auxiliary knock-in model requested for non knock-in barrier type.");
		}

		validateBarrierIsInteriorGridNode(sMin, sMax, deltaS, numberOfSteps);

		final Grid knockInGrid = new UniformGrid(numberOfSteps, sMin, sMax);

		final SpaceTimeDiscretization knockInDiscretization = new SpaceTimeDiscretization(
				knockInGrid,
				timeDiscretization,
				thetaValue,
				new double[] {originalModel.getInitialValue()[0] }
				);

		return originalModel.getCloneWithModifiedSpaceTimeDiscretization(knockInDiscretization);
	}

	private int getKnockInExtraStepsBeyondBarrier1D() {
		if (barrierType == BarrierType.DOWN_IN && callOrPutSign == CallOrPut.PUT) {
			return DOWN_IN_PUT_EXTRA_STEPS_1D;
		}
		if (barrierType == BarrierType.UP_IN && callOrPutSign == CallOrPut.CALL) {
			return UP_IN_CALL_EXTRA_STEPS_1D;
		}
		return DEFAULT_INTERIOR_BARRIER_EXTRA_STEPS_1D;
	}

	private void validateBarrierIsInteriorGridNode(
			final double sMin,
			final double sMax,
			final double deltaS,
			final int numberOfSteps) {

		final double barrierIndexReal = (barrierValue - sMin) / deltaS;
		final long barrierIndexRounded = Math.round(barrierIndexReal);

		if (Math.abs(barrierIndexReal - barrierIndexRounded) > GRID_TOLERANCE) {
			throw new IllegalArgumentException("Auxiliary knock-in grid does not place the barrier on a grid node.");
		}

		if (barrierIndexRounded <= 0 || barrierIndexRounded >= numberOfSteps) {
			throw new IllegalArgumentException("Auxiliary knock-in grid must place the barrier on an interior node.");
		}

		if (barrierValue <= sMin || barrierValue >= sMax) {
			throw new IllegalArgumentException(
					"Auxiliary knock-in grid must contain the barrier strictly inside the domain.");
		}
	}

	private static double[] getColumn(final double[][] matrix, final int columnIndex) {
		final double[] column = new double[matrix.length];
		for (int i = 0; i < matrix.length; i++) {
			column[i] = matrix[i][columnIndex];
		}
		return column;
	}

	private static int flatten(final int i0, final int i1, final int n0) {
		return i0 + i1 * n0;
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

	private boolean isOutOption() {
		return barrierType == BarrierType.DOWN_OUT || barrierType == BarrierType.UP_OUT;
	}

	private boolean usesDiscreteMonitoring() {
		return DiscreteMonitoringSupport.usesDiscreteMonitoring(monitoringType);
	}

	private void validateMonitoringSpecification() {
		DiscreteMonitoringSupport.validateMonitoringSpecification(
				monitoringType,
				monitoringTimes,
				maturity,
				DiscreteMonitoringSupport.DEFAULT_MONITORING_TIME_TOLERANCE
				);
	}

	private boolean isBarrierBreached(final double assetValue) {
		switch (barrierType) {
		case DOWN_IN:
		case DOWN_OUT:
			return assetValue <= barrierValue;
		case UP_IN:
		case UP_OUT:
			return assetValue >= barrierValue;
		default:
			throw new IllegalArgumentException("Unsupported barrier type.");
		}
	}

	private boolean isAliveAtExerciseOrMaturityForOutOption(final double assetValue) {
		switch (barrierType) {
		case DOWN_OUT:
			return assetValue > barrierValue;
		case UP_OUT:
			return assetValue < barrierValue;
		default:
			return false;
		}
	}

	private boolean isDegenerateZeroCase() {
		return (barrierType == BarrierType.UP_OUT && callOrPutSign == CallOrPut.CALL && barrierValue <= strike)
				|| (barrierType == BarrierType.DOWN_OUT && callOrPutSign == CallOrPut.PUT && barrierValue >= strike)
				|| (barrierType == BarrierType.DOWN_IN && callOrPutSign == CallOrPut.CALL && barrierValue >= strike)
				|| (barrierType == BarrierType.UP_IN && callOrPutSign == CallOrPut.PUT && barrierValue <= strike);
	}

	private boolean isDegenerateVanillaCase() {
		return (barrierType == BarrierType.UP_IN && callOrPutSign == CallOrPut.CALL && barrierValue <= strike)
				|| (barrierType == BarrierType.DOWN_IN && callOrPutSign == CallOrPut.PUT && barrierValue >= strike);
	}

	private void validateBarrierInsideGrid(final FiniteDifferenceEquityModel model) {
		final double[] grid = model.getSpaceTimeDiscretization().getSpaceGrid(0).getGrid();
		final double lowerBoundary = grid[0];
		final double upperBoundary = grid[grid.length - 1];

		if (barrierValue < lowerBoundary || barrierValue > upperBoundary) {
			throw new IllegalArgumentException(
					"The barrier must lie inside the first state-variable grid domain of the supplied model.");
		}
	}

	@Override
	public boolean isConstraintActive(final double time, final double... stateVariables) {
		if (usesDiscreteMonitoring() || !isOutOption()) {
			return false;
		}

		final double underlyingLevel = stateVariables[0];

		switch (barrierType) {
		case DOWN_OUT:
			return underlyingLevel <= barrierValue;
		case UP_OUT:
			return underlyingLevel >= barrierValue;
		default:
			return false;
		}
	}

	@Override
	public double getConstrainedValue(final double time, final double... stateVariables) {
		if (!isOutOption()) {
			throw new IllegalStateException("Internal constrained value requested for a non out-option.");
		}

		return 0.0;
	}

	private DigitalOption createVanillaDigitalOption() {
		return new DigitalOption(
				underlyingName,
				maturity,
				strike,
				callOrPutSign,
				digitalPayoffType,
				cashPayoff,
				exercise
				);
	}

	private DigitalOption createActivatedVanillaDigitalOption() {
		return new DigitalOption(
				underlyingName,
				maturity,
				strike,
				callOrPutSign,
				digitalPayoffType,
				cashPayoff,
				exercise
				);
	}

	private DigitalBarrierOption createCorrespondingOutOption() {
		return new DigitalBarrierOption(
				underlyingName,
				maturity,
				strike,
				barrierValue,
				callOrPutSign,
				getCorrespondingOutBarrierType(),
				digitalPayoffType,
				cashPayoff,
				exercise,
				monitoringType,
				monitoringTimes
				);
	}

	private BarrierType getCorrespondingOutBarrierType() {
		if (barrierType == BarrierType.DOWN_IN) {
			return BarrierType.DOWN_OUT;
		}
		if (barrierType == BarrierType.UP_IN) {
			return BarrierType.UP_OUT;
		}
		throw new IllegalArgumentException("No corresponding out barrier type for " + barrierType);
	}

	private FiniteDifferenceEquityModel createAuxiliaryVanillaModel(final FiniteDifferenceEquityModel barrierModel) {

		final SpaceTimeDiscretization barrierDiscretization = barrierModel.getSpaceTimeDiscretization();
		final TimeDiscretization timeDiscretization = barrierDiscretization.getTimeDiscretization();
		final double thetaValue = barrierDiscretization.getTheta();

		final double[] barrierGrid = barrierDiscretization.getSpaceGrid(0).getGrid();

		if (barrierGrid.length < 2) {
			throw new IllegalArgumentException("Barrier grid must contain at least two points.");
		}

		final double deltaS = barrierGrid[1] - barrierGrid[0];

		final double initialValue = barrierModel.getInitialValue()[0];
		final double currentMin = barrierGrid[0];
		final double currentMax = barrierGrid[barrierGrid.length - 1];
		final double currentHalfWidth = Math.max(initialValue - currentMin, currentMax - initialValue);

		final double targetMin = Math.max(1E-8, initialValue - 2.0 * currentHalfWidth);
		final double targetMax = initialValue + 2.0 * currentHalfWidth;

		final double sMin = Math.floor(targetMin / deltaS) * deltaS;
		final double sMax = Math.ceil(targetMax / deltaS) * deltaS;
		final int numberOfSteps = (int)Math.round((sMax - sMin) / deltaS);

		final Grid vanillaSpotGrid = new UniformGrid(numberOfSteps, sMin, sMax);

		if (barrierDiscretization.getNumberOfSpaceGrids() == 1) {
			final SpaceTimeDiscretization vanillaDiscretization = new SpaceTimeDiscretization(
					vanillaSpotGrid,
					timeDiscretization,
					thetaValue,
					new double[] {initialValue }
					);
			return barrierModel.getCloneWithModifiedSpaceTimeDiscretization(vanillaDiscretization);
		} else if (barrierDiscretization.getNumberOfSpaceGrids() == 2) {
			final double[] secondGrid = barrierDiscretization.getSpaceGrid(1).getGrid();
			final Grid preservedSecondGrid = new UniformGrid(
					secondGrid.length - 1,
					secondGrid[0],
					secondGrid[secondGrid.length - 1]
					);

			final SpaceTimeDiscretization vanillaDiscretization = new SpaceTimeDiscretization(
					new Grid[] {vanillaSpotGrid, preservedSecondGrid },
					timeDiscretization,
					thetaValue,
					barrierModel.getInitialValue()
					);
			return barrierModel.getCloneWithModifiedSpaceTimeDiscretization(vanillaDiscretization);
		} else {
			throw new IllegalArgumentException("Only 1D and 2D grids are supported.");
		}
	}

	private static CallOrPut mapCallOrPut(final double callOrPutSign) {
		if (callOrPutSign == 1.0) {
			return CallOrPut.CALL;
		}
		if (callOrPutSign == -1.0) {
			return CallOrPut.PUT;
		}
		throw new IllegalArgumentException("Unknown option type.");
	}

	/**
	 * Returns the value.
	 *
	 * @return The value.
	 */
	public String getUnderlyingName() {
		return underlyingName;
	}

	@Override
	public double getMaturity() {
		return maturity;
	}

	/**
	 * Returns the value.
	 *
	 * @return The value.
	 */
	public double getStrike() {
		return strike;
	}

	@Override
	public double getBarrierValue() {
		return barrierValue;
	}

	/**
	 * Returns the value.
	 *
	 * @return The value.
	 */
	public CallOrPut getCallOrPut() {
		return callOrPutSign;
	}

	@Override
	public BarrierType getBarrierType() {
		return barrierType;
	}

	/**
	 * Returns the value.
	 *
	 * @return The value.
	 */
	public DigitalPayoffType getDigitalPayoffType() {
		return digitalPayoffType;
	}

	/**
	 * Returns the value.
	 *
	 * @return The value.
	 */
	public double getCashPayoff() {
		return cashPayoff;
	}

	/**
	 * Returns the value.
	 *
	 * @return The value.
	 */
	public Exercise getExercise() {
		return exercise;
	}

	/**
	 * Returns the value.
	 *
	 * @return The value.
	 */
	public MonitoringType getMonitoringType() {
		return monitoringType;
	}

	/**
	 * Returns the value.
	 *
	 * @return The value.
	 */
	public double[] getMonitoringTimes() {
		return monitoringTimes == null ? null : monitoringTimes.clone();
	}

	@Override
	public double getInactiveValueAtMaturity() {
		return 0.0;
	}
}
