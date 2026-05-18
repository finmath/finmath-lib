package net.finmath.finitedifference.assetderivativevaluation.products;

import net.finmath.finitedifference.assetderivativevaluation.models.FDMHestonModel;
import net.finmath.finitedifference.assetderivativevaluation.models.FDMSabrModel;
import net.finmath.finitedifference.assetderivativevaluation.models.FiniteDifferenceEquityModel;
import net.finmath.finitedifference.assetderivativevaluation.products.internal.DiscreteMonitoringSupport;
import net.finmath.finitedifference.grids.Grid;
import net.finmath.finitedifference.grids.SpaceTimeDiscretization;
import net.finmath.finitedifference.grids.UniformGrid;
import net.finmath.finitedifference.solvers.ContinuationActivationPolicy;
import net.finmath.finitedifference.solvers.FDMSolver;
import net.finmath.finitedifference.solvers.FDMSolverFactory;
import net.finmath.finitedifference.solvers.FDMThetaMethod1DTwoState;
import net.finmath.finitedifference.solvers.ImmediateCashActivationPolicy;
import net.finmath.finitedifference.solvers.TwoStateActivationPolicy;
import net.finmath.finitedifference.solvers.TwoStateActiveBoundaryProvider;
import net.finmath.finitedifference.solvers.adi.ActivatedBarrierTrace2D;
import net.finmath.finitedifference.solvers.adi.BarrierPDEMode;
import net.finmath.finitedifference.solvers.adi.BarrierPreHitSpecification;
import net.finmath.interpolation.RationalFunctionInterpolation;
import net.finmath.interpolation.RationalFunctionInterpolation.ExtrapolationMethod;
import net.finmath.interpolation.RationalFunctionInterpolation.InterpolationMethod;
import net.finmath.modelling.EuropeanExercise;
import net.finmath.modelling.Exercise;
import net.finmath.modelling.products.BarrierType;
import net.finmath.modelling.products.MonitoringType;
import net.finmath.modelling.products.TouchSettlementTiming;
import net.finmath.time.TimeDiscretization;

/**
 * Finite-difference valuation of a single-barrier cash touch option.
 *
 * <p>
 * The product pays a fixed cash amount <i>N</i> depending on whether a barrier
 * event occurs during
 * the life of the contract. Let <i>T</i> denote the maturity, <i>B</i> the
 * barrier and
 * <i>S</i>(<i>t</i>) the underlying level.
 * </p>
 *
 * <pre>
 * tau_B = inf { t in [0,T] : S(t) &lt;= B }    for a down barrier,
 * tau_B = inf { t in [0,T] : S(t) &gt;= B }    for an up barrier.
 *
 * One-touch at expiry:  V(T) = N 1_{tau_B &lt;= T}
 * No-touch at expiry:   V(T) = N 1_{tau_B &gt; T}
 * One-touch at hit:     V(tau_B) = N
 * </pre>
 *
 * <p>
 * For expiry settlement, the post-hit state carries the discounted cash amount
 * maturing at
 * <i>T</i>. In other words, once the barrier has been activated, the
 * continuation value is the
 * deterministic cash value implied by the model discount curve.
 * </p>
 *
 * <p>
 * Barrier-type semantics:
 * </p>
 * <ul>
 * <li>{@link BarrierType#DOWN_IN} and {@link BarrierType#UP_IN} represent one-
 * touch products,</li>
 * <li>{@link BarrierType#DOWN_OUT} and {@link BarrierType#UP_OUT} represent no-
 * touch products.</li>
 * </ul>
 *
 * <p>
 * Settlement semantics:
 * </p>
 * <ul>
 * <li>{@link TouchSettlementTiming#AT_EXPIRY}: the hit / no-hit event is
 * monitored over the full
 *       option life and the payoff is settled at maturity,</li>
 * <li>{@link TouchSettlementTiming#AT_HIT}: the cash amount is paid immediately
 * when the barrier
 * is hit for the first time; this is currently supported only for one-touch
 * products.</li>
 * </ul>
 *
 * <p>
 * Monitoring semantics:
 * </p>
 * <ul>
 * <li>{@link MonitoringType#CONTINUOUS}: the current continuous-monitoring
 * implementation is used,</li>
 * <li>{@link MonitoringType#DISCRETE}: monitoring is applied only on the
 * prescribed monitoring
 * dates via vector event conditions. First milestone scope is 1D European cash
 * touch / no-touch.</li>
 * </ul>
 *
 * @author Alessandro Gnoatto
 */
public class TouchOption implements
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
		 * The direct in 2d pre hit.
		 */
		DIRECT_IN_2D_PRE_HIT
	}

	/**
	 * The default interior barrier extra steps 1 d.
	 */
	private static final int DEFAULT_INTERIOR_BARRIER_EXTRA_STEPS_1D = 40;
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
	 * The barrier value.
	 */
	private final double barrierValue;
	/**
	 * The barrier type.
	 */
	private final BarrierType barrierType;
	/**
	 * The payoff amount.
	 */
	private final double payoffAmount;
	/**
	 * The settlement timing.
	 */
	private final TouchSettlementTiming settlementTiming;
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
	 * Performs the operation.
	 *
	 * @param underlyingName The value.
	 * @param maturity The value.
	 * @param barrierValue The value.
	 * @param barrierType The value.
	 * @param payoffAmount The value.
	 * @param settlementTiming The value.
	 * @param exercise The value.
	 */
	public TouchOption(
			final String underlyingName,
			final double maturity,
			final double barrierValue,
			final BarrierType barrierType,
			final double payoffAmount,
			final TouchSettlementTiming settlementTiming,
			final Exercise exercise) {
		this(
				underlyingName,
				maturity,
				barrierValue,
				barrierType,
				payoffAmount,
				settlementTiming,
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
	 * @param barrierValue The value.
	 * @param barrierType The value.
	 * @param payoffAmount The value.
	 * @param settlementTiming The value.
	 * @param exercise The value.
	 * @param monitoringType The value.
	 * @param monitoringTimes The value.
	 */
	public TouchOption(
			final String underlyingName,
			final double maturity,
			final double barrierValue,
			final BarrierType barrierType,
			final double payoffAmount,
			final TouchSettlementTiming settlementTiming,
			final Exercise exercise,
			final MonitoringType monitoringType,
			final double[] monitoringTimes) {

		if (barrierType == null) {
			throw new IllegalArgumentException("Barrier type must not be null.");
		}
		if (settlementTiming == null) {
			throw new IllegalArgumentException("Settlement timing must not be null.");
		}
		if (exercise == null) {
			throw new IllegalArgumentException("Exercise must not be null.");
		}
		if (monitoringType == null) {
			throw new IllegalArgumentException("Monitoring type must not be null.");
		}
		if (!exercise.isEuropean()) {
			throw new IllegalArgumentException("TouchOption currently supports only European exercise.");
		}
		if (maturity < 0.0) {
			throw new IllegalArgumentException("Maturity must be non-negative.");
		}
		if (payoffAmount < 0.0) {
			throw new IllegalArgumentException("Payoff amount must be non-negative.");
		}
		if (settlementTiming == TouchSettlementTiming.AT_HIT
				&& barrierType != BarrierType.DOWN_IN
				&& barrierType != BarrierType.UP_IN) {
			throw new IllegalArgumentException(
					"AT_HIT settlement is currently supported only for one-touch products (DOWN_IN / UP_IN).");
		}

		this.underlyingName = underlyingName;
		this.maturity = maturity;
		this.barrierValue = barrierValue;
		this.barrierType = barrierType;
		this.payoffAmount = payoffAmount;
		this.settlementTiming = settlementTiming;
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
	 * @param barrierValue The value.
	 * @param barrierType The value.
	 * @param payoffAmount The value.
	 * @param settlementTiming The value.
	 */
	public TouchOption(
			final String underlyingName,
			final double maturity,
			final double barrierValue,
			final BarrierType barrierType,
			final double payoffAmount,
			final TouchSettlementTiming settlementTiming) {
		this(
				underlyingName,
				maturity,
				barrierValue,
				barrierType,
				payoffAmount,
				settlementTiming,
				new EuropeanExercise(maturity)
				);
	}

	/**
	 * Performs the operation.
	 *
	 * @param underlyingName The value.
	 * @param maturity The value.
	 * @param barrierValue The value.
	 * @param barrierType The value.
	 * @param payoffAmount The value.
	 * @param exercise The value.
	 */
	public TouchOption(
			final String underlyingName,
			final double maturity,
			final double barrierValue,
			final BarrierType barrierType,
			final double payoffAmount,
			final Exercise exercise) {
		this(
				underlyingName,
				maturity,
				barrierValue,
				barrierType,
				payoffAmount,
				TouchSettlementTiming.AT_EXPIRY,
				exercise
				);
	}

	/**
	 * Performs the operation.
	 *
	 * @param underlyingName The value.
	 * @param maturity The value.
	 * @param barrierValue The value.
	 * @param barrierType The value.
	 * @param payoffAmount The value.
	 */
	public TouchOption(
			final String underlyingName,
			final double maturity,
			final double barrierValue,
			final BarrierType barrierType,
			final double payoffAmount) {
		this(
				underlyingName,
				maturity,
				barrierValue,
				barrierType,
				payoffAmount,
				TouchSettlementTiming.AT_EXPIRY,
				new EuropeanExercise(maturity)
				);
	}

	/**
	 * Performs the operation.
	 *
	 * @param maturity The value.
	 * @param barrierValue The value.
	 * @param barrierType The value.
	 * @param payoffAmount The value.
	 * @param settlementTiming The value.
	 */
	public TouchOption(
			final double maturity,
			final double barrierValue,
			final BarrierType barrierType,
			final double payoffAmount,
			final TouchSettlementTiming settlementTiming) {
		this(
				null,
				maturity,
				barrierValue,
				barrierType,
				payoffAmount,
				settlementTiming,
				new EuropeanExercise(maturity)
				);
	}

	/**
	 * Performs the operation.
	 *
	 * @param maturity The value.
	 * @param barrierValue The value.
	 * @param barrierType The value.
	 * @param payoffAmount The value.
	 */
	public TouchOption(
			final double maturity,
			final double barrierValue,
			final BarrierType barrierType,
			final double payoffAmount) {
		this(
				null,
				maturity,
				barrierValue,
				barrierType,
				payoffAmount,
				TouchSettlementTiming.AT_EXPIRY,
				new EuropeanExercise(maturity)
				);
	}

	/**
	 * Performs the operation.
	 *
	 * @param maturity The value.
	 * @param barrierValue The value.
	 * @param barrierType The value.
	 * @param payoffAmount The value.
	 * @param settlementTiming The value.
	 * @param exercise The value.
	 * @param monitoringType The value.
	 * @param monitoringTimes The value.
	 */
	public TouchOption(
			final double maturity,
			final double barrierValue,
			final BarrierType barrierType,
			final double payoffAmount,
			final TouchSettlementTiming settlementTiming,
			final Exercise exercise,
			final MonitoringType monitoringType,
			final double[] monitoringTimes) {
		this(
				null,
				maturity,
				barrierValue,
				barrierType,
				payoffAmount,
				settlementTiming,
				exercise,
				monitoringType,
				monitoringTimes
				);
	}

	/**
	 * Performs the operation.
	 *
	 * @param maturity The value.
	 * @param barrierValue The value.
	 * @param barrierType The value.
	 * @param payoffAmount The value.
	 * @param settlementTiming The value.
	 * @param exercise The value.
	 */
	public TouchOption(
			final double maturity,
			final double barrierValue,
			final BarrierType barrierType,
			final double payoffAmount,
			final TouchSettlementTiming settlementTiming,
			final Exercise exercise) {
		this(
				null,
				maturity,
				barrierValue,
				barrierType,
				payoffAmount,
				settlementTiming,
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
	 * @param barrierValue The value.
	 * @param barrierType The value.
	 * @param payoffAmount The value.
	 * @return The value.
	 */
	public static TouchOption oneTouchAtExpiry(
			final String underlyingName,
			final double maturity,
			final double barrierValue,
			final BarrierType barrierType,
			final double payoffAmount) {

		if (barrierType != BarrierType.DOWN_IN && barrierType != BarrierType.UP_IN) {
			throw new IllegalArgumentException("One-touch requires DOWN_IN or UP_IN.");
		}

		return new TouchOption(
				underlyingName,
				maturity,
				barrierValue,
				barrierType,
				payoffAmount,
				TouchSettlementTiming.AT_EXPIRY,
				new EuropeanExercise(maturity)
				);
	}

	/**
	 * Performs the operation.
	 *
	 * @param maturity The value.
	 * @param barrierValue The value.
	 * @param barrierType The value.
	 * @param payoffAmount The value.
	 * @return The value.
	 */
	public static TouchOption oneTouchAtExpiry(
			final double maturity,
			final double barrierValue,
			final BarrierType barrierType,
			final double payoffAmount) {
		return oneTouchAtExpiry(null, maturity, barrierValue, barrierType, payoffAmount);
	}

	/**
	 * Performs the operation.
	 *
	 * @param underlyingName The value.
	 * @param maturity The value.
	 * @param barrierValue The value.
	 * @param barrierType The value.
	 * @param payoffAmount The value.
	 * @return The value.
	 */
	public static TouchOption oneTouchAtHit(
			final String underlyingName,
			final double maturity,
			final double barrierValue,
			final BarrierType barrierType,
			final double payoffAmount) {

		if (barrierType != BarrierType.DOWN_IN && barrierType != BarrierType.UP_IN) {
			throw new IllegalArgumentException("One-touch at hit requires DOWN_IN or UP_IN.");
		}

		return new TouchOption(
				underlyingName,
				maturity,
				barrierValue,
				barrierType,
				payoffAmount,
				TouchSettlementTiming.AT_HIT,
				new EuropeanExercise(maturity)
				);
	}

	/**
	 * Performs the operation.
	 *
	 * @param maturity The value.
	 * @param barrierValue The value.
	 * @param barrierType The value.
	 * @param payoffAmount The value.
	 * @return The value.
	 */
	public static TouchOption oneTouchAtHit(
			final double maturity,
			final double barrierValue,
			final BarrierType barrierType,
			final double payoffAmount) {
		return oneTouchAtHit(null, maturity, barrierValue, barrierType, payoffAmount);
	}

	/**
	 * Performs the operation.
	 *
	 * @param underlyingName The value.
	 * @param maturity The value.
	 * @param barrierValue The value.
	 * @param barrierType The value.
	 * @param payoffAmount The value.
	 * @return The value.
	 */
	public static TouchOption noTouchAtExpiry(
			final String underlyingName,
			final double maturity,
			final double barrierValue,
			final BarrierType barrierType,
			final double payoffAmount) {

		if (barrierType != BarrierType.DOWN_OUT && barrierType != BarrierType.UP_OUT) {
			throw new IllegalArgumentException("No-touch requires DOWN_OUT or UP_OUT.");
		}

		return new TouchOption(
				underlyingName,
				maturity,
				barrierValue,
				barrierType,
				payoffAmount,
				TouchSettlementTiming.AT_EXPIRY,
				new EuropeanExercise(maturity)
				);
	}

	/**
	 * Performs the operation.
	 *
	 * @param maturity The value.
	 * @param barrierValue The value.
	 * @param barrierType The value.
	 * @param payoffAmount The value.
	 * @return The value.
	 */
	public static TouchOption noTouchAtExpiry(
			final double maturity,
			final double barrierValue,
			final BarrierType barrierType,
			final double payoffAmount) {
		return noTouchAtExpiry(null, maturity, barrierValue, barrierType, payoffAmount);
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

		final FiniteDifferenceEquityModel effectiveModel = getEffectiveModelForValuation(model);

		if (payoffAmount == 0.0) {
			return buildZeroValueSurface(effectiveModel);
		}

		if (usesDiscreteMonitoring()) {
			return priceDiscreteMonitored1D(effectiveModel);
		}

		switch (getPricingMode(effectiveModel)) {
		case DIRECT_OUT:
			return priceOutOptionDirectly(effectiveModel);
		case DIRECT_IN_1D_TWO_STATE:
			return priceInOptionDirectly1D(effectiveModel);
		case DIRECT_IN_2D_PRE_HIT:
			return priceInOptionDirectly2D(effectiveModel);
		default:
			throw new IllegalStateException("Unsupported pricing mode.");
		}
	}

	@Override
	public double[] getEventTimes() {
		if (!usesDiscreteMonitoring()) {
			return new double[0];
		}
		return monitoringTimes.clone();
	}

	@Override
	public double[] applyEventCondition(
			final double time,
			final double[] valuesAfterEvent,
			final FiniteDifferenceEquityModel model) {

		if (!usesDiscreteMonitoring()) {
			return valuesAfterEvent;
		}

		final double[] xGrid = model.getSpaceTimeDiscretization().getSpaceGrid(0).getGrid();

		if (valuesAfterEvent.length != xGrid.length) {
			throw new IllegalArgumentException(
					"Value vector length does not match the one-dimensional spatial grid.");
		}

		final double[] valuesBeforeEvent = valuesAfterEvent.clone();

		for (int i = 0; i < xGrid.length; i++) {
			if (!isBarrierBreached(xGrid[i])) {
				continue;
			}

			if (isOutOption()) {
				valuesBeforeEvent[i] = 0.0;
			} else {
				valuesBeforeEvent[i] = getTouchedValueAtEventTime(time, model);
			}
		}

		return valuesBeforeEvent;
	}

	private PricingMode getPricingMode(final FiniteDifferenceEquityModel model) {

		if (isOutOption()) {
			return PricingMode.DIRECT_OUT;
		}

		final int dims = model.getSpaceTimeDiscretization().getNumberOfSpaceGrids();

		if (dims == 1) {
			return PricingMode.DIRECT_IN_1D_TWO_STATE;
		}

		return PricingMode.DIRECT_IN_2D_PRE_HIT;
	}

	private boolean supportsDirect2D(final FiniteDifferenceEquityModel model) {
		return model instanceof FDMHestonModel || model instanceof FDMSabrModel;
	}

	private void validateProductConfiguration(final FiniteDifferenceEquityModel model) {
		if (model == null) {
			throw new IllegalArgumentException("Model must not be null.");
		}

		final int dims = model.getSpaceTimeDiscretization().getNumberOfSpaceGrids();

		if (dims != 1 && dims != 2) {
			throw new IllegalArgumentException(
					"TouchOption currently supports only one-dimensional or two-dimensional models.");
		}

		validateBarrierInsideGrid(model);

		if (usesDiscreteMonitoring()) {
			validateDiscreteMonitoringScope(dims);
			return;
		}

		validateContinuousMonitoringScope(model, dims);
	}

	private void validateDiscreteMonitoringScope(final int dims) {
		if (dims != 1) {
			throw new IllegalArgumentException(
					"Discrete monitoring is currently implemented only for one-dimensional models.");
		}
		if (!exercise.isEuropean()) {
			throw new IllegalArgumentException(
					"Discrete monitoring is currently implemented only for European exercise.");
		}
	}

	private void validateContinuousMonitoringScope(
			final FiniteDifferenceEquityModel model,
			final int dims) {

		if (!exercise.isEuropean()) {
			throw new IllegalArgumentException("TouchOption currently supports only European exercise.");
		}

		if (settlementTiming == TouchSettlementTiming.AT_HIT && isOutOption()) {
			throw new IllegalArgumentException(
					"AT_HIT settlement is currently supported only for one-touch products.");
		}

		if (dims == 2) {
			if (!supportsDirect2D(model)) {
				throw new IllegalArgumentException(
						"Two-dimensional TouchOption currently supports only Heston and SABR models.");
			}
			if (settlementTiming == TouchSettlementTiming.AT_HIT && isOutOption()) {
				throw new IllegalArgumentException(
						"Two-dimensional AT_HIT currently supports only one-touch products.");
			}
		}
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

	private double[][] priceDiscreteMonitored1D(final FiniteDifferenceEquityModel model) {

		final FDMSolver solver = FDMSolverFactory.createSolver(
				model,
				this,
				model.getSpaceTimeDiscretization(),
				exercise
				);

		return solver.getValues(
				maturity,
				buildDiscreteMonitoringTerminalValues(model.getSpaceTimeDiscretization())
				);
	}

	private double[] buildDiscreteMonitoringTerminalValues(final SpaceTimeDiscretization discretization) {
		final double[] sGrid = discretization.getSpaceGrid(0).getGrid();
		final double[] terminalValues = new double[sGrid.length];

		final double terminalValue = isOutOption() ? payoffAmount : 0.0;

		for (int i = 0; i < sGrid.length; i++) {
			terminalValues[i] = terminalValue;
		}

		return terminalValues;
	}

	private double[][] priceOutOptionDirectly(final FiniteDifferenceEquityModel model) {
		final FDMSolver solver = FDMSolverFactory.createSolver(
				model,
				this,
				model.getSpaceTimeDiscretization(),
				exercise
				);

		if (model.getSpaceTimeDiscretization().getNumberOfSpaceGrids() == 1) {
			final double[] terminalValues =
					buildCellAveragedTerminalValues(model.getSpaceTimeDiscretization());

			return solver.getValues(maturity, terminalValues);
		}

		return solver.getValues(maturity, this::pointwiseNoTouchTerminalPayoff);
	}

	private double[][] priceInOptionDirectly1D(final FiniteDifferenceEquityModel model) {

		if (barrierType != BarrierType.DOWN_IN && barrierType != BarrierType.UP_IN) {
			throw new IllegalStateException(
					"priceInOptionDirectly1D was called for a non knock-in barrier type.");
		}

		final FiniteDifferenceEquityModel knockInModel = createAuxiliaryKnockInModel1D(model);

		final FDMSolver solver = new FDMThetaMethod1DTwoState(
				knockInModel,
				this,
				knockInModel.getSpaceTimeDiscretization(),
				exercise,
				createActiveBoundaryProvider(knockInModel),
				createActivationPolicy()
				);

		final double[][] knockInValuesOnAuxiliaryGrid = solver.getValues(
				maturity,
				buildActiveTerminalValues(knockInModel.getSpaceTimeDiscretization())
				);

		return interpolateSurfaceToOriginalGrid1D(
				knockInValuesOnAuxiliaryGrid,
				knockInModel.getSpaceTimeDiscretization().getSpaceGrid(0).getGrid(),
				model.getSpaceTimeDiscretization().getSpaceGrid(0).getGrid()
				);
	}

	private double[][] priceInOptionDirectly2D(final FiniteDifferenceEquityModel model) {

		if (barrierType != BarrierType.DOWN_IN && barrierType != BarrierType.UP_IN) {
			throw new IllegalStateException(
					"priceInOptionDirectly2D was called for a non knock-in barrier type.");
		}

		if (model.getSpaceTimeDiscretization().getNumberOfSpaceGrids() != 2) {
			throw new IllegalArgumentException("priceInOptionDirectly2D requires a 2D model.");
		}

		final ActivatedBarrierTrace2D trace = createAnalyticalActivatedBarrierTrace2D(model);

		final BarrierPreHitSpecification preHitSpecification =
				createPreHitSpecification2D(model, trace);

		final FiniteDifferenceEquityModel preHitModel =
				createAuxiliaryPreHitModel2D(model, preHitSpecification);

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

		return assembleDirectOneTouchSurface2D(
				model,
				preHitModel,
				preHitValues
				);
	}

	private TwoStateActiveBoundaryProvider createActiveBoundaryProvider(final FiniteDifferenceEquityModel model) {
		switch (settlementTiming) {
		case AT_EXPIRY:
			return new DiscountedCashAtExpiryActiveBoundaryProvider(model, maturity, payoffAmount);
		case AT_HIT:
			return new ImmediateCashActiveBoundaryProvider(payoffAmount);
		default:
			throw new IllegalStateException("Unsupported settlement timing: " + settlementTiming);
		}
	}

	private TwoStateActivationPolicy createActivationPolicy() {
		switch (settlementTiming) {
		case AT_EXPIRY:
			return new ContinuationActivationPolicy();
		case AT_HIT:
			return new ImmediateCashActivationPolicy(payoffAmount);
		default:
			throw new IllegalStateException("Unsupported settlement timing: " + settlementTiming);
		}
	}

	private double[] buildActiveTerminalValues(final SpaceTimeDiscretization discretization) {
		return buildConstantTerminalValues(discretization, payoffAmount);
	}

	private double[] buildCellAveragedTerminalValues(final SpaceTimeDiscretization discretization) {
		final double[] sGrid = discretization.getSpaceGrid(0).getGrid();
		final double[] terminalValues = new double[sGrid.length];

		for (int i = 0; i < sGrid.length; i++) {
			final double leftEdge = getLeftDualCellEdge(sGrid, i);
			final double rightEdge = getRightDualCellEdge(sGrid, i);
			terminalValues[i] = cellAveragedNoTouchPayoff(leftEdge, rightEdge);
		}

		return terminalValues;
	}

	private double[] buildConstantTerminalValues(
			final SpaceTimeDiscretization discretization,
			final double value) {

		final double[] sGrid = discretization.getSpaceGrid(0).getGrid();
		final double[] terminalValues = new double[sGrid.length];

		for (int i = 0; i < sGrid.length; i++) {
			terminalValues[i] = value;
		}

		return terminalValues;
	}

	private double pointwiseNoTouchTerminalPayoff(final double assetValue) {
		return isAliveAtExerciseOrMaturityForOutOption(assetValue) ? payoffAmount : 0.0;
	}

	private double cellAveragedNoTouchPayoff(final double leftEdge, final double rightEdge) {
		if (!(leftEdge < rightEdge)) {
			throw new IllegalArgumentException("Require leftEdge < rightEdge.");
		}

		switch (barrierType) {
		case DOWN_OUT:
			if (rightEdge <= barrierValue) {
				return 0.0;
			}
			if (leftEdge >= barrierValue) {
				return payoffAmount;
			}
			return payoffAmount * (rightEdge - Math.max(leftEdge, barrierValue)) / (rightEdge - leftEdge);

		case UP_OUT:
			if (leftEdge >= barrierValue) {
				return 0.0;
			}
			if (rightEdge <= barrierValue) {
				return payoffAmount;
			}
			return payoffAmount * (Math.min(rightEdge, barrierValue) - leftEdge) / (rightEdge - leftEdge);

		default:
			throw new IllegalArgumentException("No-touch terminal payoff requested for non out-option.");
		}
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
					"2D touch interpolation currently requires the second state-variable grid to remain unchanged.");
		}

		for (int j = 0; j < originalX1.length; j++) {
			if (Math.abs(auxiliaryX1[j] - originalX1[j]) > 1E-12) {
				throw new IllegalArgumentException(
						"2D touch interpolation currently requires the second state-variable grid to remain unchanged.");
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
		final double initialValue = originalModel.getInitialValue()[0];
		final int extraStepsBeyondBarrier = DEFAULT_INTERIOR_BARRIER_EXTRA_STEPS_1D;

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

	private BarrierPreHitSpecification createPreHitSpecification2D(
			final FiniteDifferenceEquityModel model,
			final ActivatedBarrierTrace2D trace) {

		final double[] originalSpotGrid =
				model.getSpaceTimeDiscretization().getSpaceGrid(0).getGrid();
		final double deltaS = originalSpotGrid[1] - originalSpotGrid[0];

		final double spotMin;
		final double spotMax;
		final int numberOfSpotSteps;
		final int barrierSpotIndex;

		if (barrierType == BarrierType.DOWN_IN) {
			spotMin = barrierValue;
			numberOfSpotSteps =
					(int) Math.ceil((originalSpotGrid[originalSpotGrid.length - 1] - barrierValue) / deltaS);
			spotMax = barrierValue + numberOfSpotSteps * deltaS;
			barrierSpotIndex = 0;
		} else if (barrierType == BarrierType.UP_IN) {
			spotMax = barrierValue;
			numberOfSpotSteps =
					(int) Math.ceil((barrierValue - originalSpotGrid[0]) / deltaS);
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
			final FiniteDifferenceEquityModel model,
			final BarrierPreHitSpecification preHitSpecification) {

		final SpaceTimeDiscretization base = model.getSpaceTimeDiscretization();
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
						model.getInitialValue()
						);

		return model.getCloneWithModifiedSpaceTimeDiscretization(preHitDiscretization);
	}

	private ActivatedBarrierTrace2D createAnalyticalActivatedBarrierTrace2D(
			final FiniteDifferenceEquityModel model) {

		final SpaceTimeDiscretization disc = model.getSpaceTimeDiscretization();
		final double[] secondGrid = disc.getSpaceGrid(1).getGrid();
		final TimeDiscretization timeDiscretization = disc.getTimeDiscretization();

		final int timeCount = timeDiscretization.getNumberOfTimeSteps() + 1;
		final double[][] traceValues = new double[secondGrid.length][timeCount];

		for (int timeIndex = 0; timeIndex < timeCount; timeIndex++) {
			final double tau = timeDiscretization.getTime(timeIndex);
			final double value = getActivatedValueForTau(model, tau);

			for (int j = 0; j < secondGrid.length; j++) {
				traceValues[j][timeIndex] = value;
			}
		}

		return new ActivatedBarrierTrace2D(
				barrierValue,
				secondGrid,
				timeDiscretization,
				traceValues
				);
	}

	private double getActivatedValueForTau(
			final FiniteDifferenceEquityModel model,
			final double tau) {

		switch (settlementTiming) {
		case AT_EXPIRY:
			return getDiscountedCashValueForTau(model, tau);
		case AT_HIT:
			return payoffAmount;
		default:
			throw new IllegalStateException("Unsupported settlement timing: " + settlementTiming);
		}
	}

	private double getDiscountedCashValueForTau(
			final FiniteDifferenceEquityModel model,
			final double tau) {

		if (payoffAmount == 0.0) {
			return 0.0;
		}

		if (tau <= 0.0) {
			return payoffAmount;
		}

		final double runningTime = Math.max(maturity - tau, 0.0);

		final double discountFactorAtTime =
				model.getRiskFreeCurve().getDiscountFactor(runningTime);
		final double discountFactorAtMaturity =
				model.getRiskFreeCurve().getDiscountFactor(maturity);

		return payoffAmount * discountFactorAtMaturity / discountFactorAtTime;
	}

	private double[][] buildActivatedSurfaceOnOriginalGrid2D(
			final FiniteDifferenceEquityModel originalModel) {

		final SpaceTimeDiscretization disc = originalModel.getSpaceTimeDiscretization();
		final int n0 = disc.getSpaceGrid(0).getGrid().length;
		final int n1 = disc.getSpaceGrid(1).getGrid().length;
		final int timeCount = disc.getTimeDiscretization().getNumberOfTimeSteps() + 1;

		final double[][] activated = new double[n0 * n1][timeCount];

		for (int timeIndex = 0; timeIndex < timeCount; timeIndex++) {
			final double tau = disc.getTimeDiscretization().getTime(timeIndex);
			final double value = getActivatedValueForTau(originalModel, tau);

			for (int j = 0; j < n1; j++) {
				for (int i = 0; i < n0; i++) {
					activated[flatten(i, j, n0)][timeIndex] = value;
				}
			}
		}

		return activated;
	}

	private double[][] assembleDirectOneTouchSurface2D(
			final FiniteDifferenceEquityModel originalModel,
			final FiniteDifferenceEquityModel preHitModel,
			final double[][] preHitValues) {

		final SpaceTimeDiscretization originalDiscretization = originalModel.getSpaceTimeDiscretization();

		final double[][] activatedOnOriginalGrid =
				buildActivatedSurfaceOnOriginalGrid2D(originalModel);

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

	private void validateBarrierInsideGrid(final FiniteDifferenceEquityModel model) {
		final double[] grid = model.getSpaceTimeDiscretization().getSpaceGrid(0).getGrid();
		final double lowerBoundary = grid[0];
		final double upperBoundary = grid[grid.length - 1];

		if (barrierValue < lowerBoundary || barrierValue > upperBoundary) {
			throw new IllegalArgumentException(
					"The barrier must lie inside the first state-variable grid domain of the supplied model.");
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

	private double getTouchedValueAtEventTime(
			final double time,
			final FiniteDifferenceEquityModel model) {

		if ((settlementTiming == TouchSettlementTiming.AT_HIT) || (time >= maturity - DiscreteMonitoringSupport.DEFAULT_MONITORING_TIME_TOLERANCE)) {
			return payoffAmount;
		}

		final double safeTime = Math.max(time, 1E-12);
		final double dfTime = model.getRiskFreeCurve().getDiscountFactor(safeTime);
		final double dfMat = model.getRiskFreeCurve().getDiscountFactor(maturity);

		return payoffAmount * dfMat / dfTime;
	}

	private SpaceTimeDiscretization getValuationSpaceTimeDiscretization(final FiniteDifferenceEquityModel model) {

		final SpaceTimeDiscretization base = model.getSpaceTimeDiscretization();

		if (!usesDiscreteMonitoring()) {
			return base;
		}

		final TimeDiscretization refinedTimeDiscretization =
				DiscreteMonitoringSupport.refineTimeDiscretizationWithMonitoring(
						base.getTimeDiscretization(),
						maturity,
						monitoringTimes
						);

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

	@Override
	public double getBarrierValue() {
		return barrierValue;
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
	public double getPayoffAmount() {
		return payoffAmount;
	}

	/**
	 * Returns the value.
	 *
	 * @return The value.
	 */
	public TouchSettlementTiming getSettlementTiming() {
		return settlementTiming;
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

	private static final class DiscountedCashAtExpiryActiveBoundaryProvider
	implements TwoStateActiveBoundaryProvider {

		/**
		 * The epsilon.
		 */
		private static final double EPSILON = 1E-10;

		/**
		 * The model.
		 */
		private final FiniteDifferenceEquityModel model;
		/**
		 * The maturity.
		 */
		private final double maturity;
		/**
		 * The payoff amount.
		 */
		private final double payoffAmount;

		private DiscountedCashAtExpiryActiveBoundaryProvider(
				final FiniteDifferenceEquityModel model,
				final double maturity,
				final double payoffAmount) {
			this.model = model;
			this.maturity = maturity;
			this.payoffAmount = payoffAmount;
		}

		@Override
		public double getLowerBoundaryValue(final double time, final double stateVariable) {
			return getDiscountedCashValue(time);
		}

		@Override
		public double getUpperBoundaryValue(final double time, final double stateVariable) {
			return getDiscountedCashValue(time);
		}

		private double getDiscountedCashValue(final double time) {
			if (payoffAmount == 0.0) {
				return 0.0;
			}

			final double effectiveTime = Math.max(time, EPSILON);

			if (effectiveTime >= maturity) {
				return payoffAmount;
			}

			final double discountFactorAtTime =
					model.getRiskFreeCurve().getDiscountFactor(effectiveTime);
			final double discountFactorAtMaturity =
					model.getRiskFreeCurve().getDiscountFactor(maturity);

			return payoffAmount * discountFactorAtMaturity / discountFactorAtTime;
		}
	}

	private static final class ImmediateCashActiveBoundaryProvider
	implements TwoStateActiveBoundaryProvider {

		/**
		 * The payoff amount.
		 */
		private final double payoffAmount;

		private ImmediateCashActiveBoundaryProvider(final double payoffAmount) {
			this.payoffAmount = payoffAmount;
		}

		@Override
		public double getLowerBoundaryValue(final double time, final double stateVariable) {
			return payoffAmount;
		}

		@Override
		public double getUpperBoundaryValue(final double time, final double stateVariable) {
			return payoffAmount;
		}
	}
}
