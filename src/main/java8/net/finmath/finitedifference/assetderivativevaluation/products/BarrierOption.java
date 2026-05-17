package net.finmath.finitedifference.assetderivativevaluation.products;

import java.util.HashMap;
import java.util.Map;

import net.finmath.finitedifference.FiniteDifferenceExerciseUtil;
import net.finmath.finitedifference.assetderivativevaluation.boundaries.ActiveBoundaryProviderFactory;
import net.finmath.finitedifference.assetderivativevaluation.models.FDMHestonModel;
import net.finmath.finitedifference.assetderivativevaluation.models.FDMSabrModel;
import net.finmath.finitedifference.assetderivativevaluation.models.FiniteDifferenceEquityModel;
import net.finmath.finitedifference.assetderivativevaluation.products.internal.ActivatedVectorEventState;
import net.finmath.finitedifference.assetderivativevaluation.products.internal.DiscreteKnockInActivationSupport;
import net.finmath.finitedifference.assetderivativevaluation.products.internal.DiscreteMonitoringSupport;
import net.finmath.finitedifference.assetderivativevaluation.products.internal.ProductEventStateStack;
import net.finmath.finitedifference.grids.Grid;
import net.finmath.finitedifference.grids.SpaceTimeDiscretization;
import net.finmath.finitedifference.grids.UniformGrid;
import net.finmath.finitedifference.solvers.FDMSolver;
import net.finmath.finitedifference.solvers.FDMSolverFactory;
import net.finmath.finitedifference.solvers.FDMThetaMethod1DTwoState;
import net.finmath.finitedifference.solvers.adi.AbstractADI2D;
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
import net.finmath.modelling.products.MonitoringType;
import net.finmath.time.TimeDiscretization;

/**
 * Finite-difference valuation of a standard single-barrier option on one asset.
 *
 * <p>
 * The barrier acts on the first state variable of the model, which is assumed
 * to represent the underlying level.
 * </p>
 *
 * <p>
 * Current implementation policy:
 * </p>
 * <ul>
 * <li>continuously monitored knock-out options are priced directly by the
 * finite-difference solver,</li>
 * <li>discretely monitored knock-out options are implemented for 1D models and
 * for 2D Heston/SABR models via internal constraints active only on monitoring
 * dates,</li>
 * <li>continuously monitored 1D knock-in options are priced directly through a
 * coupled two-state PDE on an auxiliary spatial grid where the barrier is
 * placed on an interior node,</li>
 * <li>discretely monitored 1D knock-in options are implemented via event-time
 * replacement by the activated vanilla continuation surface,</li>
 * <li>continuously monitored 2D Heston / SABR knock-in options are priced
 * directly through an activated-vanilla + pre-hit PDE / interface
 * formulation,</li>
 * <li>discretely monitored 2D Heston / SABR knock-in options are implemented on
 * the full 2D grid via event-time replacement by the activated vanilla
 * continuation surface,</li>
 * <li>for discrete monitored knock-ins with Bermudan/American exercise, only
 * the activated state carries exercise rights; the pre-hit state is always
 * solved as a European continuation problem,</li>
 *   <li>other 2D knock-in options fall back to in-out parity.</li>
 * </ul>
 *
 * @author Alessandro Gnoatto
 */
public class BarrierOption implements
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
	 * The rebate.
	 */
	private final double rebate;
	/**
	 * The call or put sign.
	 */
	private final CallOrPut callOrPutSign;
	/**
	 * The barrier type.
	 */
	private final BarrierType barrierType;
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
	 * Performs the operation.
	 *
	 * @param underlyingName The value.
	 * @param maturity The value.
	 * @param strike The value.
	 * @param barrierValue The value.
	 * @param rebate The value.
	 * @param callOrPutSign The value.
	 * @param barrierType The value.
	 */
	public BarrierOption(
			final String underlyingName,
			final double maturity,
			final double strike,
			final double barrierValue,
			final double rebate,
			final double callOrPutSign,
			final BarrierType barrierType) {
		this(
				underlyingName,
				maturity,
				strike,
				barrierValue,
				rebate,
				mapCallOrPut(callOrPutSign),
				barrierType,
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
	 * @param rebate The value.
	 * @param callOrPutSign The value.
	 * @param barrierType The value.
	 */
	public BarrierOption(
			final String underlyingName,
			final double maturity,
			final double strike,
			final double barrierValue,
			final double rebate,
			final CallOrPut callOrPutSign,
			final BarrierType barrierType) {
		this(
				underlyingName,
				maturity,
				strike,
				barrierValue,
				rebate,
				callOrPutSign,
				barrierType,
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
	 * @param rebate The value.
	 * @param callOrPutSign The value.
	 * @param barrierType The value.
	 * @param exercise The value.
	 */
	public BarrierOption(
			final String underlyingName,
			final double maturity,
			final double strike,
			final double barrierValue,
			final double rebate,
			final double callOrPutSign,
			final BarrierType barrierType,
			final Exercise exercise) {
		this(
				underlyingName,
				maturity,
				strike,
				barrierValue,
				rebate,
				mapCallOrPut(callOrPutSign),
				barrierType,
				exercise
		);
	}

	/**
	 * Performs the operation.
	 *
	 * @param underlyingName The value.
	 * @param maturity The value.
	 * @param strike The value.
	 * @param barrierValue The value.
	 * @param rebate The value.
	 * @param callOrPutSign The value.
	 * @param barrierType The value.
	 * @param exercise The value.
	 */
	public BarrierOption(
			final String underlyingName,
			final double maturity,
			final double strike,
			final double barrierValue,
			final double rebate,
			final CallOrPut callOrPutSign,
			final BarrierType barrierType,
			final Exercise exercise) {
		this(
				underlyingName,
				maturity,
				strike,
				barrierValue,
				rebate,
				callOrPutSign,
				barrierType,
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
	 * @param rebate The value.
	 * @param callOrPutSign The value.
	 * @param barrierType The value.
	 * @param exercise The value.
	 * @param monitoringType The value.
	 * @param monitoringTimes The value.
	 */
	public BarrierOption(
			final String underlyingName,
			final double maturity,
			final double strike,
			final double barrierValue,
			final double rebate,
			final double callOrPutSign,
			final BarrierType barrierType,
			final Exercise exercise,
			final MonitoringType monitoringType,
			final double[] monitoringTimes) {
		this(
				underlyingName,
				maturity,
				strike,
				barrierValue,
				rebate,
				mapCallOrPut(callOrPutSign),
				barrierType,
				exercise,
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
	 * @param rebate The value.
	 * @param callOrPutSign The value.
	 * @param barrierType The value.
	 * @param exercise The value.
	 * @param monitoringType The value.
	 * @param monitoringTimes The value.
	 */
	public BarrierOption(
			final String underlyingName,
			final double maturity,
			final double strike,
			final double barrierValue,
			final double rebate,
			final CallOrPut callOrPutSign,
			final BarrierType barrierType,
			final Exercise exercise,
			final MonitoringType monitoringType,
			final double[] monitoringTimes) {
		super();
		this.underlyingName = underlyingName;
		this.maturity = maturity;
		this.strike = strike;
		this.barrierValue = barrierValue;
		this.rebate = rebate;
		this.callOrPutSign = callOrPutSign;
		this.barrierType = barrierType;
		this.exercise = exercise;
		this.monitoringType = monitoringType;
		this.monitoringTimes = monitoringTimes == null ? null : monitoringTimes.clone();
	}

	/**
	 * Performs the operation.
	 *
	 * @param maturity The value.
	 * @param strike The value.
	 * @param barrierValue The value.
	 * @param rebate The value.
	 * @param callOrPutSign The value.
	 * @param barrierType The value.
	 */
	public BarrierOption(
			final double maturity,
			final double strike,
			final double barrierValue,
			final double rebate,
			final double callOrPutSign,
			final BarrierType barrierType) {
		this(null, maturity, strike, barrierValue, rebate, callOrPutSign, barrierType);
	}

	/**
	 * Performs the operation.
	 *
	 * @param maturity The value.
	 * @param strike The value.
	 * @param barrierValue The value.
	 * @param rebate The value.
	 * @param callOrPutSign The value.
	 * @param barrierType The value.
	 */
	public BarrierOption(
			final double maturity,
			final double strike,
			final double barrierValue,
			final double rebate,
			final CallOrPut callOrPutSign,
			final BarrierType barrierType) {
		this(null, maturity, strike, barrierValue, rebate, callOrPutSign, barrierType);
	}

	/**
	 * Performs the operation.
	 *
	 * @param maturity The value.
	 * @param strike The value.
	 * @param barrierValue The value.
	 * @param rebate The value.
	 * @param callOrPutSign The value.
	 * @param barrierType The value.
	 * @param exercise The value.
	 */
	public BarrierOption(
			final double maturity,
			final double strike,
			final double barrierValue,
			final double rebate,
			final double callOrPutSign,
			final BarrierType barrierType,
			final Exercise exercise) {
		this(null, maturity, strike, barrierValue, rebate, callOrPutSign, barrierType, exercise);
	}

	/**
	 * Performs the operation.
	 *
	 * @param maturity The value.
	 * @param strike The value.
	 * @param barrierValue The value.
	 * @param rebate The value.
	 * @param callOrPutSign The value.
	 * @param barrierType The value.
	 * @param exercise The value.
	 */
	public BarrierOption(
			final double maturity,
			final double strike,
			final double barrierValue,
			final double rebate,
			final CallOrPut callOrPutSign,
			final BarrierType barrierType,
			final Exercise exercise) {
		this(null, maturity, strike, barrierValue, rebate, callOrPutSign, barrierType, exercise);
	}

	/**
	 * Performs the operation.
	 *
	 * @param maturity The value.
	 * @param strike The value.
	 * @param barrierValue The value.
	 * @param rebate The value.
	 * @param callOrPutSign The value.
	 * @param barrierType The value.
	 * @param exercise The value.
	 * @param monitoringType The value.
	 * @param monitoringTimes The value.
	 */
	public BarrierOption(
			final double maturity,
			final double strike,
			final double barrierValue,
			final double rebate,
			final CallOrPut callOrPutSign,
			final BarrierType barrierType,
			final Exercise exercise,
			final MonitoringType monitoringType,
			final double[] monitoringTimes) {
		this(
				null,
				maturity,
				strike,
				barrierValue,
				rebate,
				callOrPutSign,
				barrierType,
				exercise,
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
	 */
	public BarrierOption(
			final String underlyingName,
			final double maturity,
			final double strike,
			final double barrierValue,
			final CallOrPut callOrPutSign,
			final BarrierType barrierType) {
		this(underlyingName, maturity, strike, barrierValue, 0.0, callOrPutSign, barrierType);
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
	 * @param exercise The value.
	 */
	public BarrierOption(
			final String underlyingName,
			final double maturity,
			final double strike,
			final double barrierValue,
			final CallOrPut callOrPutSign,
			final BarrierType barrierType,
			final Exercise exercise) {
		this(underlyingName, maturity, strike, barrierValue, 0.0, callOrPutSign, barrierType, exercise);
	}

	/**
	 * Performs the operation.
	 *
	 * @param maturity The value.
	 * @param strike The value.
	 * @param barrierValue The value.
	 * @param callOrPutSign The value.
	 * @param barrierType The value.
	 */
	public BarrierOption(
			final double maturity,
			final double strike,
			final double barrierValue,
			final double callOrPutSign,
			final BarrierType barrierType) {
		this(null, maturity, strike, barrierValue, 0.0, callOrPutSign, barrierType);
	}

	/**
	 * Performs the operation.
	 *
	 * @param maturity The value.
	 * @param strike The value.
	 * @param barrierValue The value.
	 * @param callOrPutSign The value.
	 * @param barrierType The value.
	 * @param exercise The value.
	 */
	public BarrierOption(
			final double maturity,
			final double strike,
			final double barrierValue,
			final double callOrPutSign,
			final BarrierType barrierType,
			final Exercise exercise) {
		this(null, maturity, strike, barrierValue, 0.0, callOrPutSign, barrierType, exercise);
	}

	private ProductEventStateStack<ActivatedVectorEventState> getActivatedVectorEventStateStack() {
		if (activatedVectorEventStateStack == null) {
			activatedVectorEventStateStack = new ProductEventStateStack<>();
		}

		return activatedVectorEventStateStack;
	}

	private ActivatedVectorEventState createActivatedVectorEventState(
			final FiniteDifferenceEquityModel effectiveModel,
			final double[][] activatedSurface) {

		return new ActivatedVectorEventState(
				buildActivatedVectorsAtEventTimes(effectiveModel, activatedSurface),
				DiscreteMonitoringSupport.DEFAULT_MONITORING_TIME_TOLERANCE
		);
	}

	private Map<Double, double[]> buildActivatedVectorsAtEventTimes(
			final FiniteDifferenceEquityModel effectiveModel,
			final double[][] activatedSurface) {

		final Map<Double, double[]> activatedVectorsAtEventTimes = new HashMap<>();

		if (monitoringTimes == null) {
			return activatedVectorsAtEventTimes;
		}

		final SpaceTimeDiscretization activatedDiscretization =
				effectiveModel.getSpaceTimeDiscretization();

		for (final double eventTime : monitoringTimes) {
			activatedVectorsAtEventTimes.put(
					eventTime,
					DiscreteKnockInActivationSupport.getActivatedVectorAt(
							eventTime,
							maturity,
							activatedSurface,
							activatedDiscretization,
							effectiveModel,
							GRID_TOLERANCE
					).clone()
			);
		}

		return activatedVectorsAtEventTimes;
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
				return createVanillaOption().getValues(model);
			}
		}

		switch (getPricingMode(model)) {
		case DIRECT_OUT:
			return priceOutOptionDirectly(model);
		case DIRECT_IN_1D_TWO_STATE:
			return priceInOptionDirectly1D(model);
		case DIRECT_IN_1D_DISCRETE_EVENT:
			return priceInOptionDiscrete1D(model);
		case DIRECT_IN_2D_DISCRETE_EVENT:
			return priceInOptionDiscrete2D(model);
		case DIRECT_IN_2D_PRE_HIT:
			return priceInOptionDirectly2D(model);
		case PARITY_IN_FALLBACK:
			return priceInOptionByParity(model);
		default:
			throw new IllegalStateException("Unsupported pricing mode.");
		}
	}

	@Override
	public double[] getEventTimes() {
		if (usesDiscreteMonitoring() && !isOutOption()) {
			return monitoringTimes == null ? new double[0] : monitoringTimes.clone();
		}
		return new double[0];
	}

	@Override
	public double[] applyEventCondition(
			final double time,
			final double[] valuesAfterEvent,
			final FiniteDifferenceEquityModel model) {

		if (!usesDiscreteMonitoring() || isOutOption()) {
			return valuesAfterEvent;
		}

		final ActivatedVectorEventState state =
				getActivatedVectorEventStateStack().currentOrNull();

		if (state == null) {
			return valuesAfterEvent;
		}

		final int dims = model.getSpaceTimeDiscretization().getNumberOfSpaceGrids();

		if (dims == 1) {
			return applyDiscreteKnockInEvent1D(time, valuesAfterEvent, model, state);
		}
		if (dims == 2) {
			return applyDiscreteKnockInEvent2D(time, valuesAfterEvent, model, state);
		}

		throw new IllegalArgumentException("Only 1D and 2D grids are supported.");
	}

	private double[] applyDiscreteKnockInEvent1D(
			final double time,
			final double[] valuesAfterEvent,
			final FiniteDifferenceEquityModel model,
			final ActivatedVectorEventState state) {

		final double[] xGrid = model.getSpaceTimeDiscretization().getSpaceGrid(0).getGrid();
		if (valuesAfterEvent.length != xGrid.length) {
			throw new IllegalArgumentException(
					"Value vector length does not match the one-dimensional spatial grid."
			);
		}

		final double[] activatedVector = state.getActivatedVector(time);
		final double[] valuesBeforeEvent = valuesAfterEvent.clone();

		for (int i = 0; i < xGrid.length; i++) {
			if (isBarrierBreachedForKnockIn(xGrid[i])) {
				valuesBeforeEvent[i] = activatedVector[i];
			}
		}

		return valuesBeforeEvent;
	}

	private double[] applyDiscreteKnockInEvent2D(
			final double time,
			final double[] valuesAfterEvent,
			final FiniteDifferenceEquityModel model,
			final ActivatedVectorEventState state) {

		final double[] x0Grid = model.getSpaceTimeDiscretization().getSpaceGrid(0).getGrid();
		final double[] x1Grid = model.getSpaceTimeDiscretization().getSpaceGrid(1).getGrid();

		if (valuesAfterEvent.length != x0Grid.length * x1Grid.length) {
			throw new IllegalArgumentException(
					"Value vector length does not match the two-dimensional spatial grid."
			);
		}

		final double[] activatedVector = state.getActivatedVector(time);
		final double[] valuesBeforeEvent = valuesAfterEvent.clone();

		for (int j = 0; j < x1Grid.length; j++) {
			for (int i = 0; i < x0Grid.length; i++) {
				final int k = DiscreteKnockInActivationSupport.flatten(i, j, x0Grid.length);
				if (isBarrierBreachedForKnockIn(x0Grid[i])) {
					valuesBeforeEvent[k] = activatedVector[k];
				}
			}
		}

		return valuesBeforeEvent;
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
			if (supportsDiscreteIn(model)) {
				return PricingMode.DIRECT_IN_2D_DISCRETE_EVENT;
			}
			return PricingMode.PARITY_IN_FALLBACK;
		}

		if (dims == 1) {
			return PricingMode.DIRECT_IN_1D_TWO_STATE;
		}

		if (supportsContinuousDirectIn(model)) {
			return PricingMode.DIRECT_IN_2D_PRE_HIT;
		}

		return PricingMode.PARITY_IN_FALLBACK;
	}

	private void validateProductConfiguration(final FiniteDifferenceEquityModel model) {
		validateBarrierInsideGrid(model);
		validateMonitoringSpecification();

		if (usesDiscreteMonitoring()) {
			if (isOutOption()) {
				if (supportsDiscreteOut(model)) {
					return;
				}
				throw new IllegalArgumentException(
						"Discrete knock-out monitoring is currently implemented only for one-dimensional models "
						+ "or two-dimensional Heston/SABR models."
				);
			}

			if (supportsDiscreteIn(model)) {
				return;
			}

			throw new IllegalArgumentException(
					"Discrete knock-in monitoring is currently implemented only for one-dimensional models "
					+ "or two-dimensional Heston/SABR models."
			);
		}

		final int dims = model.getSpaceTimeDiscretization().getNumberOfSpaceGrids();

		if (exercise.isEuropean()) {
			return;
		}

		if (dims == 1 && isOutOption()) {
			return;
		}

		if (dims == 1 && !isOutOption() && (exercise.isBermudan() || exercise.isAmerican())) {
			return;
		}

		if (dims == 2 && supportsContinuousDirectIn(model)
				&& (exercise.isBermudan() || exercise.isAmerican())) {
			return;
		}

		throw new IllegalArgumentException(
				"Non-European exercise unsupported for this model / barrier configuration."
		);
	}

	private boolean supportsDiscreteOut(final FiniteDifferenceEquityModel model) {
		final int dims = model.getSpaceTimeDiscretization().getNumberOfSpaceGrids();
		return dims == 1 || supportsDirect2DStochasticVolBarrier(model);
	}

	private boolean supportsDiscreteIn(final FiniteDifferenceEquityModel model) {
		final int dims = model.getSpaceTimeDiscretization().getNumberOfSpaceGrids();
		return dims == 1 || supportsDirect2DStochasticVolBarrier(model);
	}

	private boolean supportsContinuousDirectIn(final FiniteDifferenceEquityModel model) {
		return supportsDirect2DStochasticVolBarrier(model);
	}

	private boolean supportsDirect2DStochasticVolBarrier(final FiniteDifferenceEquityModel model) {
		return model instanceof FDMHestonModel || model instanceof FDMSabrModel;
	}

	private double[][] buildZeroValueSurface(final FiniteDifferenceEquityModel model) {
		final int numberOfSpacePoints = getTotalNumberOfSpacePoints(model.getSpaceTimeDiscretization());
		final int numberOfTimePoints =
				getValuationSpaceTimeDiscretization(model).getTimeDiscretization().getNumberOfTimeSteps() + 1;

		final double[][] zeroValues = new double[numberOfSpacePoints][numberOfTimePoints];
		for (int i = 0; i < numberOfSpacePoints; i++) {
			for (int j = 0; j < numberOfTimePoints; j++) {
				zeroValues[i][j] = 0.0;
			}
		}
		return zeroValues;
	}

	private double[][] priceOutOptionDirectly(final FiniteDifferenceEquityModel model) {
		final SpaceTimeDiscretization valuationDiscretization = getValuationSpaceTimeDiscretization(model);
		return createSolver(model, this, valuationDiscretization, exercise)
				.getValues(maturity, this::getTerminalPayoffForDirectOutPricing);
	}

	private double[][] priceInOptionDirectly1D(final FiniteDifferenceEquityModel model) {

		if (barrierType != BarrierType.DOWN_IN && barrierType != BarrierType.UP_IN) {
			throw new IllegalStateException(
					"priceInOptionDirectly1D was called for a non knock-in barrier type."
			);
		}

		final FiniteDifferenceEquityModel effectiveBaseModel = getEffectiveModelForOneDimensionalKnockIn(model);
		final FiniteDifferenceEquityModel knockInModel = createAuxiliaryKnockInModel1D(effectiveBaseModel);

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
				this::pointwiseImmediateExercisePayoff
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
					"priceInOptionDiscrete1D was called for a non knock-in barrier type."
			);
		}

		if (!usesDiscreteMonitoring()) {
			throw new IllegalStateException(
					"priceInOptionDiscrete1D requires discrete monitoring."
			);
		}

		final FiniteDifferenceEquityModel effectiveModel = getEffectiveModelForOneDimensionalKnockIn(model);

		final double[][] activatedVanillaValues =
				createActivatedVanillaProduct(getActivatedExercise()).getValues(effectiveModel);

		try(ProductEventStateStack.Scope ignored =
				getActivatedVectorEventStateStack().push(
						createActivatedVectorEventState(
								effectiveModel,
								activatedVanillaValues
						)
				)) {

			final FDMSolver solver = createSolver(
					effectiveModel,
					this,
					effectiveModel.getSpaceTimeDiscretization(),
					getPreHitExercise()
			);

			return solver.getValues(
					maturity,
					DiscreteKnockInActivationSupport.buildZeroTerminalValues(
							effectiveModel.getSpaceTimeDiscretization()
					)
			);
		}
	}

	private double[][] priceInOptionDiscrete2D(final FiniteDifferenceEquityModel model) {

		if (barrierType != BarrierType.DOWN_IN && barrierType != BarrierType.UP_IN) {
			throw new IllegalStateException(
					"priceInOptionDiscrete2D was called for a non knock-in barrier type."
			);
		}

		if (!usesDiscreteMonitoring()) {
			throw new IllegalStateException(
					"priceInOptionDiscrete2D requires discrete monitoring."
			);
		}

		if (model.getSpaceTimeDiscretization().getNumberOfSpaceGrids() != 2) {
			throw new IllegalStateException(
					"priceInOptionDiscrete2D requires a two-dimensional model."
			);
		}

		final FiniteDifferenceEquityModel effectiveModel =
				getEffectiveModelForTwoDimensionalKnockIn(model);

		final double[][] activatedVanillaValues =
				createActivatedVanillaProduct(getActivatedExercise()).getValues(effectiveModel);

		try(ProductEventStateStack.Scope ignored =
				getActivatedVectorEventStateStack().push(
						createActivatedVectorEventState(
								effectiveModel,
								activatedVanillaValues
						)
				)) {

			final FDMSolver solver = createSolver(
					effectiveModel,
					this,
					effectiveModel.getSpaceTimeDiscretization(),
					getPreHitExercise()
			);

			if (!(solver instanceof AbstractADI2D)) {
				throw new IllegalStateException(
						"Discrete 2D knock-in requires an ADI 2D solver, but got "
						+ solver.getClass().getName()
				);
			}

			return ((AbstractADI2D)solver).getValues(
					maturity,
					(x0, x1) -> 0.0
			);
		}
	}

	private double[][] priceInOptionDirectly2D(final FiniteDifferenceEquityModel model) {

		if (barrierType != BarrierType.DOWN_IN && barrierType != BarrierType.UP_IN) {
			throw new IllegalStateException(
					"priceInOptionDirectly2D was called for a non knock-in barrier type."
			);
		}

		if (!supportsContinuousDirectIn(model)) {
			return priceInOptionByParity(model);
		}

		final FiniteDifferenceEquityModel effectiveBarrierModel =
				getEffectiveModelForTwoDimensionalKnockIn(model);

		final FiniteDifferenceEquityModel activatedModel =
				createAuxiliaryActivatedModel2D(effectiveBarrierModel);

		final FiniteDifferenceEquityProduct activatedProduct =
				createActivatedVanillaProduct(getActivatedExercise());

		final FDMSolver activatedSolver =
				createSolver(
						activatedModel,
						activatedProduct,
						activatedModel.getSpaceTimeDiscretization(),
						getActivatedExercise()
				);

		final double[][] activatedValues = getValuesForActivatedVanilla(activatedSolver);

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
						getPreHitExercise(),
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

	private double[][] getValuesForActivatedVanilla(final FDMSolver solver) {
		return solver.getValues(maturity, this::pointwiseImmediateExercisePayoff);
	}

	private FiniteDifferenceEquityProduct createActivatedVanillaProduct(final Exercise activatedExercise) {
		if (activatedExercise.isEuropean()) {
			return new EuropeanOption(underlyingName, maturity, strike, callOrPutSign);
		}
		if (activatedExercise.isBermudan()) {
			return new BermudanOption(underlyingName, activatedExercise.getExerciseTimes(), strike, callOrPutSign);
		}
		if (activatedExercise.isAmerican()) {
			return new AmericanOption(underlyingName, maturity, strike, callOrPutSign);
		}
		throw new IllegalArgumentException("Unsupported exercise specification.");
	}

	private Exercise getActivatedExercise() {
		return exercise;
	}

	private Exercise getPreHitExercise() {
		return new EuropeanExercise(maturity);
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
			final int k = DiscreteKnockInActivationSupport.flatten(barrierIndex, j, x0.length);
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

				final int k = DiscreteKnockInActivationSupport.flatten(i, j, x0.length);
				final double[][] source = alreadyHit ? activatedOnOriginalGrid : preHitOnOriginalGrid;

				for (int timeIndex = 0; timeIndex < numberOfColumns; timeIndex++) {
					result[k][timeIndex] = source[k][timeIndex];
				}
			}
		}

		return result;
	}

	private double[][] priceInOptionByParity(final FiniteDifferenceEquityModel barrierModel) {

		final EuropeanOption vanillaOption = createVanillaOption();
		final BarrierOption correspondingOutOption = createCorrespondingOutOption();

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
						DiscreteKnockInActivationSupport.getColumn(vanillaValues, timeIndex),
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

	private double[][] interpolateSurfaceToOriginalGrid1D(
			final double[][] valuesOnAuxiliaryGrid,
			final double[] auxiliaryGrid,
			final double[] originalGrid) {

		final int numberOfColumns = valuesOnAuxiliaryGrid[0].length;
		final double[][] interpolatedValues = new double[originalGrid.length][numberOfColumns];

		for (int timeIndex = 0; timeIndex < numberOfColumns; timeIndex++) {
			final RationalFunctionInterpolation interpolator = new RationalFunctionInterpolation(
					auxiliaryGrid,
					DiscreteKnockInActivationSupport.getColumn(valuesOnAuxiliaryGrid, timeIndex),
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
					"2D knock-in interpolation currently requires the second state-variable grid to remain unchanged."
			);
		}

		for (int j = 0; j < originalX1.length; j++) {
			if (Math.abs(auxiliaryX1[j] - originalX1[j]) > 1E-12) {
				throw new IllegalArgumentException(
						"2D knock-in interpolation currently requires the second state-variable grid to remain unchanged."
				);
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
					final int k = DiscreteKnockInActivationSupport.flatten(i, j, auxiliaryN0);
					auxiliarySlice[i] = valuesOnAuxiliaryGrid[k][timeIndex];
				}

				final RationalFunctionInterpolation interpolator = new RationalFunctionInterpolation(
						auxiliaryX0,
						auxiliarySlice,
						InterpolationMethod.LINEAR,
						ExtrapolationMethod.CONSTANT
				);

				for (int i = 0; i < originalN0; i++) {
					final int k = DiscreteKnockInActivationSupport.flatten(i, j, originalN0);
					interpolatedValues[k][timeIndex] = interpolator.getValue(originalX0[i]);
				}
			}
		}

		return interpolatedValues;
	}

	private FDMSolver createSolver(
			final FiniteDifferenceEquityModel model,
			final FiniteDifferenceEquityProduct product,
			final SpaceTimeDiscretization discretization,
			final Exercise exerciseSpecification) {
		return FDMSolverFactory.createSolver(model, product, discretization, exerciseSpecification);
	}

	private EuropeanOption createVanillaOption() {
		return new EuropeanOption(underlyingName, maturity, strike, callOrPutSign);
	}

	private BarrierOption createCorrespondingOutOption() {
		return new BarrierOption(
				underlyingName,
				maturity,
				strike,
				barrierValue,
				rebate,
				callOrPutSign,
				getCorrespondingOutBarrierType(),
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
		final double initialValue = originalModel.getInitialValue()[0];
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
				new double[] {initialValue }
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
					"Auxiliary knock-in grid must contain the barrier strictly inside the domain."
			);
		}
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

	private double getTerminalPayoffForDirectOutPricing(final double assetValue) {

		if (callOrPutSign == CallOrPut.CALL) {
			if (barrierType == BarrierType.DOWN_OUT) {
				if (barrierValue <= strike) {
					return Math.max(assetValue - strike, 0.0);
				}
				return assetValue > barrierValue ? Math.max(assetValue - strike, 0.0) : rebate;
			} else if (barrierType == BarrierType.UP_OUT) {
				if (barrierValue <= strike) {
					return 0.0;
				}
				return assetValue < barrierValue ? Math.max(assetValue - strike, 0.0) : rebate;
			}
		} else {
			if (barrierType == BarrierType.DOWN_OUT) {
				if (barrierValue >= strike) {
					return 0.0;
				}
				return assetValue > barrierValue ? Math.max(strike - assetValue, 0.0) : rebate;
			} else if (barrierType == BarrierType.UP_OUT) {
				if (barrierValue >= strike) {
					return Math.max(strike - assetValue, 0.0);
				}
				return assetValue < barrierValue ? Math.max(strike - assetValue, 0.0) : rebate;
			}
		}

		throw new IllegalArgumentException("Direct terminal payoff requested for non out-option type.");
	}

	private double pointwiseImmediateExercisePayoff(final double assetValue) {
		if (callOrPutSign == CallOrPut.CALL) {
			return Math.max(assetValue - strike, 0.0);
		}
		return Math.max(strike - assetValue, 0.0);
	}

	private void validateBarrierInsideGrid(final FiniteDifferenceEquityModel model) {
		final double[] grid = model.getSpaceTimeDiscretization().getSpaceGrid(0).getGrid();
		final double lowerBoundary = grid[0];
		final double upperBoundary = grid[grid.length - 1];

		if (barrierValue < lowerBoundary || barrierValue > upperBoundary) {
			throw new IllegalArgumentException(
					"The barrier must lie inside the first state-variable grid domain of the supplied model."
			);
		}
	}

	@Override
	public boolean isConstraintActive(final double time, final double... stateVariables) {
		if (!isOutOption()) {
			return false;
		}

		if (usesDiscreteMonitoring() && !isMonitoringTime(time)) {
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

		return rebate;
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

	private boolean isBarrierBreachedForKnockIn(final double underlyingLevel) {
		switch (barrierType) {
		case DOWN_IN:
			return underlyingLevel <= barrierValue;
		case UP_IN:
			return underlyingLevel >= barrierValue;
		default:
			return false;
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

	private SpaceTimeDiscretization getValuationSpaceTimeDiscretization(final FiniteDifferenceEquityModel model) {

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

	private FiniteDifferenceEquityModel getEffectiveModelForOneDimensionalKnockIn(
			final FiniteDifferenceEquityModel model) {

		final SpaceTimeDiscretization effectiveDiscretization = getValuationSpaceTimeDiscretization(model);

		if (effectiveDiscretization == model.getSpaceTimeDiscretization()) {
			return model;
		}

		return model.getCloneWithModifiedSpaceTimeDiscretization(effectiveDiscretization);
	}

	private FiniteDifferenceEquityModel getEffectiveModelForTwoDimensionalKnockIn(
			final FiniteDifferenceEquityModel model) {

		final SpaceTimeDiscretization effectiveDiscretization = getValuationSpaceTimeDiscretization(model);

		if (effectiveDiscretization == model.getSpaceTimeDiscretization()) {
			return model;
		}

		return model.getCloneWithModifiedSpaceTimeDiscretization(effectiveDiscretization);
	}

	/**
	 * Returns the value.
	 *
	 * @return The value.
	 */
	public String getUnderlyingName() {
		return underlyingName;
	}

	/**
	 * Returns the value.
	 *
	 * @return The value.
	 */
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

	/**
	 * Returns the value.
	 *
	 * @return The value.
	 */
	public double getBarrierValue() {
		return barrierValue;
	}

	/**
	 * Returns the value.
	 *
	 * @return The value.
	 */
	public double getRebate() {
		return rebate;
	}

	/**
	 * Returns the value.
	 *
	 * @return The value.
	 */
	public CallOrPut getCallOrPut() {
		return callOrPutSign;
	}

	/**
	 * Returns the value.
	 *
	 * @return The value.
	 */
	public BarrierType getBarrierType() {
		return barrierType;
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
		return rebate;
	}
}
