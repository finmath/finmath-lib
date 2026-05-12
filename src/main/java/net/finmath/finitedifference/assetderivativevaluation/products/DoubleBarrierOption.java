package net.finmath.finitedifference.assetderivativevaluation.products;

import java.util.HashMap;
import java.util.Map;

import net.finmath.finitedifference.FiniteDifferenceExerciseUtil;
import net.finmath.finitedifference.assetderivativevaluation.boundaries.FiniteDifferenceBoundary;
import net.finmath.finitedifference.assetderivativevaluation.models.FDMHestonModel;
import net.finmath.finitedifference.assetderivativevaluation.models.FDMSabrModel;
import net.finmath.finitedifference.assetderivativevaluation.models.FiniteDifferenceEquityModel;
import net.finmath.finitedifference.assetderivativevaluation.products.internal.ActivatedVectorEventState;
import net.finmath.finitedifference.assetderivativevaluation.products.internal.DiscreteMonitoringSupport;
import net.finmath.finitedifference.assetderivativevaluation.products.internal.ProductEventStateStack;
import net.finmath.finitedifference.boundaries.BoundaryCondition;
import net.finmath.finitedifference.boundaries.StandardBoundaryCondition;
import net.finmath.finitedifference.grids.Grid;
import net.finmath.finitedifference.grids.SpaceTimeDiscretization;
import net.finmath.finitedifference.grids.UniformGrid;
import net.finmath.finitedifference.solvers.FDMSolver;
import net.finmath.finitedifference.solvers.FDMSolverFactory;
import net.finmath.finitedifference.solvers.FDMThetaMethod1D;
import net.finmath.interpolation.RationalFunctionInterpolation;
import net.finmath.interpolation.RationalFunctionInterpolation.ExtrapolationMethod;
import net.finmath.interpolation.RationalFunctionInterpolation.InterpolationMethod;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.modelling.EuropeanExercise;
import net.finmath.modelling.Exercise;
import net.finmath.modelling.products.CallOrPut;
import net.finmath.modelling.products.DoubleBarrierType;
import net.finmath.modelling.products.MonitoringType;
import net.finmath.time.TimeDiscretization;

/**
 * Finite-difference valuation of a continuously monitored vanilla double-
 * barrier option.
 *
 * <p>
 * The product is defined by a strike <i>K</i>, lower and upper barriers
 * <i>L &lt; U</i>, maturity <i>T</i>, and a call/put sign. Let <i>S(t)</i>
 * denote the
 * underlying. The alive region is the open band
 * </p>
 *
 * <p>
 * <i>L &lt; S(t) &lt; U</i>.
 * </p>
 *
 * <p>
 * For a knock-out contract, the option survives only as long as the path
 * remains inside
 * the band. For a knock-in contract, the option becomes active once the path
 * hits either
 * barrier. Writing
 * </p>
 *
 * <p>
 * <i>&tau; = inf { t in [0,T] : S(t) &le; L or S(t) &ge; U }</i>,
 * </p>
 *
 * <p>
 * the corresponding payoffs are
 * </p>
 *
 * <p>
 * <i>V(T) = 1_{&tau; &gt; T} max(&omega;(S(T)-K),0)</i> for knock-out,
 * </p>
 *
 * <p>
 * <i>V(T) = 1_{&tau; &le; T} max(&omega;(S(T)-K),0)</i> for knock-in,
 * </p>
 *
 * <p>
 * where <i>&omega; = 1</i> for a call and <i>&omega; = -1</i> for a put.
 * </p>
 *
 * <p>
 * This implementation supports:
 * </p>
 * <ul>
 *   <li>vanilla call / put payoffs,</li>
 * <li>{@link DoubleBarrierType#KNOCK_OUT} and {@link
 * DoubleBarrierType#KNOCK_IN},</li>
 *   <li>European, Bermudan, and American exercise,</li>
 *   <li>one-dimensional models,</li>
 *   <li>two-dimensional Heston and SABR models.</li>
 * </ul>
 *
 * <p>
 * The knock-out case is priced directly by imposing an internal-state
 * constraint outside
 * the alive band. The knock-in case is priced directly without parity
 * decomposition:
 * </p>
 * <ul>
 *   <li>outside the alive band, the option is already activated and equals the
 *       corresponding vanilla value,</li>
 *   <li>inside the alive band, a pre-hit PDE is solved on the band
 * <i>(L,U)</i> with time-dependent Dirichlet boundary data taken from the
 * activated
 *       vanilla branch at the two barriers,</li>
 * <li>for Bermudan and American exercise, the obstacle is applied only in the
 * activated
 * vanilla branch, while the pre-hit branch remains a pure continuation problem
 * up to
 *       barrier activation.</li>
 * </ul>
 *
 * <p>
 * In the one-dimensional case this leads to a PDE on the truncated spatial
 * interval
 * <i>(L,U)</i>. In the two-dimensional case the same idea is applied while
 * preserving the
 * second state-variable grid and imposing barrier traces that depend on time
 * and on the
 * second state variable.
 * </p>
 *
 * <p>
 * For discrete monitoring, barrier activation / knock-out is applied only at
 * the supplied
 * monitoring dates via event conditions. In the discretely monitored knock-in
 * case, the
 * activated branch is the true vanilla branch with the product exercise style,
 * while the
 * pre-hit branch is solved as a European continuation problem between
 * monitoring dates.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class DoubleBarrierOption implements
		FiniteDifferenceEquityEventProduct,
		FiniteDifferenceInternalStateConstraint {

	private enum PricingMode {
		/**
		 * The direct out.
		 */
		DIRECT_OUT,
		/**
		 * The direct in 1d pre hit.
		 */
		DIRECT_IN_1D_PRE_HIT,
		/**
		 * The direct in 1d discrete event.
		 */
		DIRECT_IN_1D_DISCRETE_EVENT,
		/**
		 * The direct in 2d pre hit.
		 */
		DIRECT_IN_2D_PRE_HIT,
		/**
		 * The direct in 2d discrete event.
		 */
		DIRECT_IN_2D_DISCRETE_EVENT
	}

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
	 * The lower barrier.
	 */
	private final double lowerBarrier;
	/**
	 * The upper barrier.
	 */
	private final double upperBarrier;
	/**
	 * The call or put sign.
	 */
	private final CallOrPut callOrPutSign;
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
	 * Creates a double-barrier option.
	 *
	 * @param underlyingName Name of the underlying. May be {@code null}.
	 * @param maturity Option maturity.
	 * @param strike Option strike.
	 * @param lowerBarrier Lower barrier.
	 * @param upperBarrier Upper barrier.
	 * @param callOrPutSign Call/put sign.
	 * @param doubleBarrierType Double-barrier type.
	 * @param exercise Exercise specification.
	 */
	public DoubleBarrierOption(
			final String underlyingName,
			final double maturity,
			final double strike,
			final double lowerBarrier,
			final double upperBarrier,
			final CallOrPut callOrPutSign,
			final DoubleBarrierType doubleBarrierType,
			final Exercise exercise) {
		this(
				underlyingName,
				maturity,
				strike,
				lowerBarrier,
				upperBarrier,
				callOrPutSign,
				doubleBarrierType,
				exercise,
				MonitoringType.CONTINUOUS,
				null
		);
	}

	/**
	 * Creates a double-barrier option.
	 *
	 * @param underlyingName Name of the underlying. May be {@code null}.
	 * @param maturity Option maturity.
	 * @param strike Option strike.
	 * @param lowerBarrier Lower barrier.
	 * @param upperBarrier Upper barrier.
	 * @param callOrPutSign Call/put sign.
	 * @param doubleBarrierType Double-barrier type.
	 * @param exercise Exercise specification.
	 * @param monitoringType Monitoring type.
	 * @param monitoringTimes Monitoring times for discrete monitoring.
	 */
	public DoubleBarrierOption(
			final String underlyingName,
			final double maturity,
			final double strike,
			final double lowerBarrier,
			final double upperBarrier,
			final CallOrPut callOrPutSign,
			final DoubleBarrierType doubleBarrierType,
			final Exercise exercise,
			final MonitoringType monitoringType,
			final double[] monitoringTimes) {

		if (callOrPutSign == null) {
			throw new IllegalArgumentException("Option type must not be null.");
		}
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
					"DoubleBarrierOption currently supports only European, Bermudan, and American exercise.");
		}
		if (maturity < 0.0) {
			throw new IllegalArgumentException("Maturity must be non-negative.");
		}
		if (strike <= 0.0) {
			throw new IllegalArgumentException("Strike must be positive.");
		}
		if (lowerBarrier <= 0.0 || upperBarrier <= 0.0) {
			throw new IllegalArgumentException("Barriers must be positive.");
		}
		if (lowerBarrier >= upperBarrier) {
			throw new IllegalArgumentException("lowerBarrier must be < upperBarrier.");
		}
		if (doubleBarrierType == DoubleBarrierType.KIKO || doubleBarrierType == DoubleBarrierType.KOKI) {
			throw new IllegalArgumentException("KIKO/KOKI are not supported for vanilla double-barrier options.");
		}

		this.underlyingName = underlyingName;
		this.maturity = maturity;
		this.strike = strike;
		this.lowerBarrier = lowerBarrier;
		this.upperBarrier = upperBarrier;
		this.callOrPutSign = callOrPutSign;
		this.doubleBarrierType = doubleBarrierType;
		this.exercise = exercise;
		this.monitoringType = monitoringType;
		this.monitoringTimes = monitoringTimes == null ? null : monitoringTimes.clone();

		validateMonitoringSpecification();
	}

	/**
	 * Creates a European double-barrier option.
	 *
	 * @param underlyingName Name of the underlying. May be {@code null}.
	 * @param maturity Option maturity.
	 * @param strike Option strike.
	 * @param lowerBarrier Lower barrier.
	 * @param upperBarrier Upper barrier.
	 * @param callOrPutSign Call/put sign.
	 * @param doubleBarrierType Double-barrier type.
	 */
	public DoubleBarrierOption(
			final String underlyingName,
			final double maturity,
			final double strike,
			final double lowerBarrier,
			final double upperBarrier,
			final CallOrPut callOrPutSign,
			final DoubleBarrierType doubleBarrierType) {
		this(
				underlyingName,
				maturity,
				strike,
				lowerBarrier,
				upperBarrier,
				callOrPutSign,
				doubleBarrierType,
				new EuropeanExercise(maturity)
		);
	}

	/**
	 * Creates a European double-barrier option.
	 *
	 * @param underlyingName Name of the underlying. May be {@code null}.
	 * @param maturity Option maturity.
	 * @param strike Option strike.
	 * @param lowerBarrier Lower barrier.
	 * @param upperBarrier Upper barrier.
	 * @param callOrPutSign Call/put sign.
	 * @param doubleBarrierType Double-barrier type.
	 * @param monitoringType Monitoring type.
	 * @param monitoringTimes Monitoring times for discrete monitoring.
	 */
	public DoubleBarrierOption(
			final String underlyingName,
			final double maturity,
			final double strike,
			final double lowerBarrier,
			final double upperBarrier,
			final CallOrPut callOrPutSign,
			final DoubleBarrierType doubleBarrierType,
			final MonitoringType monitoringType,
			final double[] monitoringTimes) {
		this(
				underlyingName,
				maturity,
				strike,
				lowerBarrier,
				upperBarrier,
				callOrPutSign,
				doubleBarrierType,
				new EuropeanExercise(maturity),
				monitoringType,
				monitoringTimes
		);
	}

	/**
	 * Creates a European double-barrier option with anonymous underlying.
	 *
	 * @param maturity Option maturity.
	 * @param strike Option strike.
	 * @param lowerBarrier Lower barrier.
	 * @param upperBarrier Upper barrier.
	 * @param callOrPutSign Call/put sign.
	 * @param doubleBarrierType Double-barrier type.
	 */
	public DoubleBarrierOption(
			final double maturity,
			final double strike,
			final double lowerBarrier,
			final double upperBarrier,
			final CallOrPut callOrPutSign,
			final DoubleBarrierType doubleBarrierType) {
		this(
				null,
				maturity,
				strike,
				lowerBarrier,
				upperBarrier,
				callOrPutSign,
				doubleBarrierType,
				new EuropeanExercise(maturity)
		);
	}

	/**
	 * Creates a European double-barrier option with anonymous underlying.
	 *
	 * @param maturity Option maturity.
	 * @param strike Option strike.
	 * @param lowerBarrier Lower barrier.
	 * @param upperBarrier Upper barrier.
	 * @param callOrPutSign Call/put sign.
	 * @param doubleBarrierType Double-barrier type.
	 * @param monitoringType Monitoring type.
	 * @param monitoringTimes Monitoring times for discrete monitoring.
	 */
	public DoubleBarrierOption(
			final double maturity,
			final double strike,
			final double lowerBarrier,
			final double upperBarrier,
			final CallOrPut callOrPutSign,
			final DoubleBarrierType doubleBarrierType,
			final MonitoringType monitoringType,
			final double[] monitoringTimes) {
		this(
				null,
				maturity,
				strike,
				lowerBarrier,
				upperBarrier,
				callOrPutSign,
				doubleBarrierType,
				new EuropeanExercise(maturity),
				monitoringType,
				monitoringTimes
		);
	}

	/**
	 * Creates a European double-barrier option using a numeric call/put sign.
	 *
	 * @param maturity Option maturity.
	 * @param strike Option strike.
	 * @param lowerBarrier Lower barrier.
	 * @param upperBarrier Upper barrier.
	 * @param callOrPutSign Numeric sign, {@code 1.0} for call and {@code -1.0}
	 *     for put.
	 * @param doubleBarrierType Double-barrier type.
	 */
	public DoubleBarrierOption(
			final double maturity,
			final double strike,
			final double lowerBarrier,
			final double upperBarrier,
			final double callOrPutSign,
			final DoubleBarrierType doubleBarrierType) {
		this(
				null,
				maturity,
				strike,
				lowerBarrier,
				upperBarrier,
				mapCallOrPut(callOrPutSign),
				doubleBarrierType,
				new EuropeanExercise(maturity)
		);
	}

	/**
	 * Creates a European double-barrier option using a numeric call/put sign.
	 *
	 * @param maturity Option maturity.
	 * @param strike Option strike.
	 * @param lowerBarrier Lower barrier.
	 * @param upperBarrier Upper barrier.
	 * @param callOrPutSign Numeric sign, {@code 1.0} for call and {@code -1.0}
	 *     for put.
	 * @param doubleBarrierType Double-barrier type.
	 * @param monitoringType Monitoring type.
	 * @param monitoringTimes Monitoring times for discrete monitoring.
	 */
	public DoubleBarrierOption(
			final double maturity,
			final double strike,
			final double lowerBarrier,
			final double upperBarrier,
			final double callOrPutSign,
			final DoubleBarrierType doubleBarrierType,
			final MonitoringType monitoringType,
			final double[] monitoringTimes) {
		this(
				null,
				maturity,
				strike,
				lowerBarrier,
				upperBarrier,
				mapCallOrPut(callOrPutSign),
				doubleBarrierType,
				new EuropeanExercise(maturity),
				monitoringType,
				monitoringTimes
		);
	}

	/**
	 * Creates a double-barrier option using a numeric call/put sign.
	 *
	 * @param underlyingName Name of the underlying. May be {@code null}.
	 * @param maturity Option maturity.
	 * @param strike Option strike.
	 * @param lowerBarrier Lower barrier.
	 * @param upperBarrier Upper barrier.
	 * @param callOrPutSign Numeric sign, {@code 1.0} for call and {@code -1.0}
	 *     for put.
	 * @param doubleBarrierType Double-barrier type.
	 * @param exercise Exercise specification.
	 */
	public DoubleBarrierOption(
			final String underlyingName,
			final double maturity,
			final double strike,
			final double lowerBarrier,
			final double upperBarrier,
			final double callOrPutSign,
			final DoubleBarrierType doubleBarrierType,
			final Exercise exercise) {
		this(
				underlyingName,
				maturity,
				strike,
				lowerBarrier,
				upperBarrier,
				mapCallOrPut(callOrPutSign),
				doubleBarrierType,
				exercise
		);
	}

	/**
	 * Creates a double-barrier option using a numeric call/put sign.
	 *
	 * @param underlyingName Name of the underlying. May be {@code null}.
	 * @param maturity Option maturity.
	 * @param strike Option strike.
	 * @param lowerBarrier Lower barrier.
	 * @param upperBarrier Upper barrier.
	 * @param callOrPutSign Numeric sign, {@code 1.0} for call and {@code -1.0}
	 *     for put.
	 * @param doubleBarrierType Double-barrier type.
	 * @param exercise Exercise specification.
	 * @param monitoringType Monitoring type.
	 * @param monitoringTimes Monitoring times for discrete monitoring.
	 */
	public DoubleBarrierOption(
			final String underlyingName,
			final double maturity,
			final double strike,
			final double lowerBarrier,
			final double upperBarrier,
			final double callOrPutSign,
			final DoubleBarrierType doubleBarrierType,
			final Exercise exercise,
			final MonitoringType monitoringType,
			final double[] monitoringTimes) {
		this(
				underlyingName,
				maturity,
				strike,
				lowerBarrier,
				upperBarrier,
				mapCallOrPut(callOrPutSign),
				doubleBarrierType,
				exercise,
				monitoringType,
				monitoringTimes
		);
	}

	/**
	 * Creates a double-barrier option with anonymous underlying.
	 *
	 * @param maturity Option maturity.
	 * @param strike Option strike.
	 * @param lowerBarrier Lower barrier.
	 * @param upperBarrier Upper barrier.
	 * @param callOrPutSign Call/put sign.
	 * @param doubleBarrierType Double-barrier type.
	 * @param exercise Exercise specification.
	 */
	public DoubleBarrierOption(
			final double maturity,
			final double strike,
			final double lowerBarrier,
			final double upperBarrier,
			final CallOrPut callOrPutSign,
			final DoubleBarrierType doubleBarrierType,
			final Exercise exercise) {
		this(
				null,
				maturity,
				strike,
				lowerBarrier,
				upperBarrier,
				callOrPutSign,
				doubleBarrierType,
				exercise
		);
	}

	/**
	 * Creates a double-barrier option with anonymous underlying.
	 *
	 * @param maturity Option maturity.
	 * @param strike Option strike.
	 * @param lowerBarrier Lower barrier.
	 * @param upperBarrier Upper barrier.
	 * @param callOrPutSign Call/put sign.
	 * @param doubleBarrierType Double-barrier type.
	 * @param exercise Exercise specification.
	 * @param monitoringType Monitoring type.
	 * @param monitoringTimes Monitoring times for discrete monitoring.
	 */
	public DoubleBarrierOption(
			final double maturity,
			final double strike,
			final double lowerBarrier,
			final double upperBarrier,
			final CallOrPut callOrPutSign,
			final DoubleBarrierType doubleBarrierType,
			final Exercise exercise,
			final MonitoringType monitoringType,
			final double[] monitoringTimes) {
		this(
				null,
				maturity,
				strike,
				lowerBarrier,
				upperBarrier,
				callOrPutSign,
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
			final FiniteDifferenceEquityModel effectiveModel,
			final FiniteDifferenceEquityProduct activatedProduct) {

		return new ActivatedVectorEventState(
				buildActivatedVectorsAtEventTimes(effectiveModel, activatedProduct),
				DiscreteMonitoringSupport.DEFAULT_MONITORING_TIME_TOLERANCE
		);
	}

	private ActivatedVectorEventState getCurrentActivatedVectorEventState() {
		final ActivatedVectorEventState state =
				getActivatedVectorEventStateStack().currentOrNull();

		if (state == null) {
			throw new IllegalStateException(
					"Discrete knock-in event condition requires cached activated continuation data."
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

		if (!usesDiscreteMonitoring()) {
			if (isDegenerateZeroCase()) {
				return buildZeroValueSurface(model);
			}

			if (isDegenerateVanillaCase()) {
				return createVanillaOption().getValues(getEffectiveModelForValuation(model));
			}
		}

		switch (getPricingMode(model)) {
		case DIRECT_OUT:
			return priceOutOptionDirectly(model);

		case DIRECT_IN_1D_PRE_HIT:
			return priceInOptionDirectly1D(model);

		case DIRECT_IN_1D_DISCRETE_EVENT:
			return priceInOptionDiscrete1D(model);

		case DIRECT_IN_2D_PRE_HIT:
			return priceInOptionDirectly2D(model);

		case DIRECT_IN_2D_DISCRETE_EVENT:
			return priceInOptionDiscrete2D(model);

		default:
			throw new IllegalStateException("Unsupported pricing mode.");
		}
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

			if (dims == 2 && supportsDirect2D(model)) {
				return PricingMode.DIRECT_IN_2D_DISCRETE_EVENT;
			}

			throw new IllegalArgumentException(
					"Discrete double-barrier knock-in currently supports only 1D models and 2D Heston/SABR models.");
		}

		if (dims == 1) {
			return PricingMode.DIRECT_IN_1D_PRE_HIT;
		}

		if (dims == 2 && supportsDirect2D(model)) {
			return PricingMode.DIRECT_IN_2D_PRE_HIT;
		}

		throw new IllegalArgumentException(
				"Direct double-barrier knock-in currently supports only 1D models and 2D Heston/SABR models.");
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
			throw new IllegalArgumentException("DoubleBarrierOption currently supports only 1D and 2D models.");
		}

		if (dims == 2 && !supportsDirect2D(model)) {
			throw new IllegalArgumentException(
					"Two-dimensional DoubleBarrierOption currently supports only Heston and SABR models.");
		}

		validateBarriersInsideGrid(model);

		if (usesDiscreteMonitoring()) {
			validateDiscreteMonitoringScope(model);
		}
	}

	private void validateDiscreteMonitoringScope(final FiniteDifferenceEquityModel model) {
		final int dims = model.getSpaceTimeDiscretization().getNumberOfSpaceGrids();

		if (dims == 1) {
			return;
		}

		if (dims == 2 && supportsDirect2D(model)) {
			return;
		}

		throw new IllegalArgumentException(
				"Discrete monitoring for DoubleBarrierOption currently supports only 1D models and 2D Heston/SABR models.");
	}

	private double[][] buildZeroValueSurface(final FiniteDifferenceEquityModel model) {
		final SpaceTimeDiscretization valuationDiscretization = getValuationSpaceTimeDiscretization(model);
		final int numberOfSpacePoints = getTotalNumberOfSpacePoints(valuationDiscretization);
		final int numberOfTimePoints = valuationDiscretization.getTimeDiscretization().getNumberOfTimeSteps() + 1;

		final double[][] zeroValues = new double[numberOfSpacePoints][numberOfTimePoints];
		for (int i = 0; i < numberOfSpacePoints; i++) {
			for (int j = 0; j < numberOfTimePoints; j++) {
				zeroValues[i][j] = 0.0;
			}
		}
		return zeroValues;
	}

	/*
	 * ============================================
	 * DIRECT KNOCK-OUT
	 * ============================================
	 */

	private double[][] priceOutOptionDirectly(final FiniteDifferenceEquityModel model) {
		final FiniteDifferenceEquityModel effectiveModel = getEffectiveModelForValuation(model);
		final SpaceTimeDiscretization valuationDiscretization = effectiveModel.getSpaceTimeDiscretization();

		final FDMSolver solver = FDMSolverFactory.createSolver(
				effectiveModel,
				this,
				valuationDiscretization,
				exercise
		);

		final boolean isOneDimensional = valuationDiscretization.getNumberOfSpaceGrids() == 1;

		if (isOneDimensional) {
			final double[] terminalValues = buildCellAveragedTerminalValues(valuationDiscretization);

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

	/*
	 * ============================================
	 * DIRECT KNOCK-IN 1D
	 * ============================================
	 */

	private double[][] priceInOptionDirectly1D(final FiniteDifferenceEquityModel model) {
		final FiniteDifferenceEquityModel effectiveModel = getEffectiveModelForValuation(model);

		/*
		 * Step 1: activated vanilla branch on a widened vanilla grid.
		 */
		final FiniteDifferenceEquityModel activatedModel = createAuxiliaryActivatedModel(effectiveModel);
		final FiniteDifferenceEquityProduct activatedProduct = createActivatedVanillaProduct();

		final double[][] activatedValues = activatedProduct.getValues(activatedModel);

		/*
		 * Step 2: build lower / upper barrier traces from activated vanilla.
		 */
		final DoubleBarrierTrace1D lowerTrace = extractActivatedBoundaryTrace1D(
				activatedModel,
				activatedValues,
				lowerBarrier
		);
		final DoubleBarrierTrace1D upperTrace = extractActivatedBoundaryTrace1D(
				activatedModel,
				activatedValues,
				upperBarrier
		);

		/*
		 * Step 3: solve pre-hit PDE on the alive band only.
		 */
		final FiniteDifferenceEquityModel preHitModel = createAuxiliaryPreHitModel1D(
				effectiveModel,
				lowerTrace,
				upperTrace
		);

		final FDMSolver preHitSolver = new FDMThetaMethod1D(
				preHitModel,
				this,
				preHitModel.getSpaceTimeDiscretization(),
				new EuropeanExercise(maturity)
		);

		final double[] zeroTerminal = buildZeroTerminalValues(preHitModel.getSpaceTimeDiscretization());
		final double[][] preHitValues = preHitSolver.getValues(maturity, zeroTerminal);

		/*
		 * Step 4: interpolate both branches back to the original grid and
		 * stitch.
		 */
		return assembleDirectKnockInSurface1D(
				model,
				activatedModel,
				activatedValues,
				preHitModel,
				preHitValues
		);
	}

	private double[][] priceInOptionDiscrete1D(final FiniteDifferenceEquityModel model) {
		final FiniteDifferenceEquityModel effectiveModel = getEffectiveModelForValuation(model);
		final FiniteDifferenceEquityProduct activatedProduct = createActivatedVanillaProduct();

		try(ProductEventStateStack.Scope ignored =
				getActivatedVectorEventStateStack().push(
						createActivatedVectorEventState(effectiveModel, activatedProduct)
				)) {

			final FDMSolver solver = FDMSolverFactory.createSolver(
					effectiveModel,
					this,
					effectiveModel.getSpaceTimeDiscretization(),
					new EuropeanExercise(maturity)
			);

			final double[] zeroTerminal = buildZeroTerminalValues(effectiveModel.getSpaceTimeDiscretization());
			return solver.getValues(maturity, zeroTerminal);
		}
	}

	/*
	 * ============================================
	 * DIRECT KNOCK-IN 2D HESTON / SABR
	 * ============================================
	 */

	private double[][] priceInOptionDirectly2D(final FiniteDifferenceEquityModel model) {
		final FiniteDifferenceEquityModel effectiveModel = getEffectiveModelForValuation(model);

		/*
		 * Step 1: activated vanilla branch on a widened vanilla grid.
		 */
		final FiniteDifferenceEquityModel activatedModel = createAuxiliaryActivatedModel(effectiveModel);
		final FiniteDifferenceEquityProduct activatedProduct = createActivatedVanillaProduct();

		final double[][] activatedValues = activatedProduct.getValues(activatedModel);

		/*
		 * Step 2: lower / upper barrier traces depending on second state
		 * variable.
		 */
		final DoubleBarrierTrace2D lowerTrace = extractActivatedBoundaryTrace2D(
				activatedModel,
				activatedValues,
				lowerBarrier
		);
		final DoubleBarrierTrace2D upperTrace = extractActivatedBoundaryTrace2D(
				activatedModel,
				activatedValues,
				upperBarrier
		);

		/*
		 * Step 3: solve pre-hit PDE on the alive band using ordinary 2D ADI,
		 *         but with time-dependent Dirichlet spot boundaries.
		 */
		final FiniteDifferenceEquityModel preHitModel = createAuxiliaryPreHitModel2D(
				effectiveModel,
				lowerTrace,
				upperTrace
		);

		final FDMSolver preHitSolver = FDMSolverFactory.createSolver(
				preHitModel,
				this,
				preHitModel.getSpaceTimeDiscretization(),
				new EuropeanExercise(maturity)
		);

		final double[][] preHitValues = preHitSolver.getValues(maturity, assetValue -> 0.0);

		/*
		 * Step 4: interpolate both branches back to the original grid and
		 * stitch.
		 */
		return assembleDirectKnockInSurface2D(
				model,
				activatedModel,
				activatedValues,
				preHitModel,
				preHitValues
		);
	}

	private double[][] priceInOptionDiscrete2D(final FiniteDifferenceEquityModel model) {
		final FiniteDifferenceEquityModel effectiveModel = getEffectiveModelForValuation(model);
		final FiniteDifferenceEquityProduct activatedProduct = createActivatedVanillaProduct();

		try(ProductEventStateStack.Scope ignored =
				getActivatedVectorEventStateStack().push(
						createActivatedVectorEventState(effectiveModel, activatedProduct)
				)) {

			final FDMSolver solver = FDMSolverFactory.createSolver(
					effectiveModel,
					this,
					effectiveModel.getSpaceTimeDiscretization(),
					new EuropeanExercise(maturity)
			);

			return solver.getValues(maturity, assetValue -> 0.0);
		}
	}

	private Map<Double, double[]> buildActivatedVectorsAtEventTimes(
			final FiniteDifferenceEquityModel effectiveModel,
			final FiniteDifferenceEquityProduct activatedProduct) {

		final Map<Double, double[]> activatedVectorsAtEventTimes = new HashMap<>();

		if (monitoringTimes == null) {
			return activatedVectorsAtEventTimes;
		}

		for (final double eventTime : monitoringTimes) {
			activatedVectorsAtEventTimes.put(
					eventTime,
					activatedProduct.getValue(eventTime, effectiveModel).clone()
			);
		}

		return activatedVectorsAtEventTimes;
	}

	/*
	 * ============================================
	 * ACTIVATED VANILLA BRANCH
	 * ============================================
	 */

	private FiniteDifferenceEquityProduct createActivatedVanillaProduct() {
		if (exercise.isEuropean()) {
			return new EuropeanOption(underlyingName, maturity, strike, callOrPutSign);
		}
		if (exercise.isBermudan()) {
			return new BermudanOption(underlyingName, exercise.getExerciseTimes(), strike, callOrPutSign);
		}
		if (exercise.isAmerican()) {
			return new AmericanOption(underlyingName, maturity, strike, callOrPutSign);
		}
		throw new IllegalArgumentException("Unsupported exercise specification.");
	}

	private EuropeanOption createVanillaOption() {
		return new EuropeanOption(underlyingName, maturity, strike, callOrPutSign);
	}

	private FiniteDifferenceEquityModel createAuxiliaryActivatedModel(final FiniteDifferenceEquityModel baseModel) {
		final SpaceTimeDiscretization base = baseModel.getSpaceTimeDiscretization();
		final TimeDiscretization timeDiscretization = base.getTimeDiscretization();
		final double thetaValue = base.getTheta();

		final double[] baseSpotGrid = base.getSpaceGrid(0).getGrid();
		if (baseSpotGrid.length < 2) {
			throw new IllegalArgumentException("Spot grid must contain at least two points.");
		}

		final double deltaS = baseSpotGrid[1] - baseSpotGrid[0];
		final double initialSpot = baseModel.getInitialValue()[0];

		final double currentMin = baseSpotGrid[0];
		final double currentMax = baseSpotGrid[baseSpotGrid.length - 1];
		final double currentHalfWidth = Math.max(initialSpot - currentMin, currentMax - initialSpot);

		final double targetMin = Math.max(1E-8, initialSpot - 2.0 * currentHalfWidth);
		final double targetMax = initialSpot + 2.0 * currentHalfWidth;

		final double sMin = Math.floor(targetMin / deltaS) * deltaS;
		final double sMax = Math.ceil(targetMax / deltaS) * deltaS;
		final int numberOfSteps = Math.max(2, (int)Math.round((sMax - sMin) / deltaS));

		final Grid activatedSpotGrid = new UniformGrid(numberOfSteps, sMin, sMax);

		if (base.getNumberOfSpaceGrids() == 1) {
			final SpaceTimeDiscretization activatedDiscretization = new SpaceTimeDiscretization(
					activatedSpotGrid,
					timeDiscretization,
					thetaValue,
					new double[] {initialSpot }
			);
			return baseModel.getCloneWithModifiedSpaceTimeDiscretization(activatedDiscretization);
		} else if (base.getNumberOfSpaceGrids() == 2) {
			final double[] secondGrid = base.getSpaceGrid(1).getGrid();

			final Grid preservedSecondGrid = new UniformGrid(
					secondGrid.length - 1,
					secondGrid[0],
					secondGrid[secondGrid.length - 1]
			);

			final SpaceTimeDiscretization activatedDiscretization = new SpaceTimeDiscretization(
					new Grid[] {activatedSpotGrid, preservedSecondGrid },
					timeDiscretization,
					thetaValue,
					baseModel.getInitialValue()
			);
			return baseModel.getCloneWithModifiedSpaceTimeDiscretization(activatedDiscretization);
		} else {
			throw new IllegalArgumentException("Only 1D and 2D grids are supported.");
		}
	}

	/*
	 * ============================================
	 * PRE-HIT BAND MODELS
	 * ============================================
	 */

	private FiniteDifferenceEquityModel createAuxiliaryPreHitModel1D(
			final FiniteDifferenceEquityModel baseModel,
			final DoubleBarrierTrace1D lowerTrace,
			final DoubleBarrierTrace1D upperTrace) {

		final SpaceTimeDiscretization base = baseModel.getSpaceTimeDiscretization();
		final double[] originalSpotGrid = base.getSpaceGrid(0).getGrid();
		final double deltaS = originalSpotGrid[1] - originalSpotGrid[0];

		final int numberOfSteps = Math.max(2, (int)Math.ceil((upperBarrier - lowerBarrier) / deltaS));
		final Grid preHitSpotGrid = new UniformGrid(numberOfSteps, lowerBarrier, upperBarrier);

		final SpaceTimeDiscretization preHitDiscretization = new SpaceTimeDiscretization(
				preHitSpotGrid,
				base.getTimeDiscretization(),
				base.getTheta(),
				new double[] {baseModel.getInitialValue()[0] }
		);

		final FiniteDifferenceEquityModel wrappedBase =
				baseModel.getCloneWithModifiedSpaceTimeDiscretization(preHitDiscretization);

		return new DoubleBarrierPreHitModel1D(
				wrappedBase,
				preHitDiscretization,
				lowerTrace,
				upperTrace
		);
	}

	private FiniteDifferenceEquityModel createAuxiliaryPreHitModel2D(
			final FiniteDifferenceEquityModel baseModel,
			final DoubleBarrierTrace2D lowerTrace,
			final DoubleBarrierTrace2D upperTrace) {

		final SpaceTimeDiscretization base = baseModel.getSpaceTimeDiscretization();
		final double[] originalSpotGrid = base.getSpaceGrid(0).getGrid();
		final double deltaS = originalSpotGrid[1] - originalSpotGrid[0];

		final int numberOfSpotSteps = Math.max(2, (int)Math.ceil((upperBarrier - lowerBarrier) / deltaS));
		final Grid preHitSpotGrid = new UniformGrid(numberOfSpotSteps, lowerBarrier, upperBarrier);

		final double[] secondGrid = base.getSpaceGrid(1).getGrid();
		final Grid preservedSecondGrid = new UniformGrid(
				secondGrid.length - 1,
				secondGrid[0],
				secondGrid[secondGrid.length - 1]
		);

		final SpaceTimeDiscretization preHitDiscretization = new SpaceTimeDiscretization(
				new Grid[] {preHitSpotGrid, preservedSecondGrid },
				base.getTimeDiscretization(),
				base.getTheta(),
				baseModel.getInitialValue()
		);

		if (baseModel instanceof FDMHestonModel) {
			final FDMHestonModel hestonBase = (FDMHestonModel)baseModel;
			return new DoubleBarrierPreHitHestonModel(
					hestonBase,
					preHitDiscretization,
					lowerTrace,
					upperTrace
			);
		} else if (baseModel instanceof FDMSabrModel) {
			final FDMSabrModel sabrBase = (FDMSabrModel)baseModel;
			return new DoubleBarrierPreHitSabrModel(
					sabrBase,
					preHitDiscretization,
					lowerTrace,
					upperTrace
			);
		} else {
			throw new IllegalArgumentException(
					"Two-dimensional pre-hit double-barrier model currently supports only Heston and SABR.");
		}
	}

	/*
	 * ============================================
	 * TRACE EXTRACTION
	 * ============================================
	 */

	private DoubleBarrierTrace1D extractActivatedBoundaryTrace1D(
			final FiniteDifferenceEquityModel activatedModel,
			final double[][] activatedValues,
			final double barrier) {

		final SpaceTimeDiscretization disc = activatedModel.getSpaceTimeDiscretization();
		final double[] spotGrid = disc.getSpaceGrid(0).getGrid();
		final int numberOfColumns = activatedValues[0].length;

		final double[] traceValues = new double[numberOfColumns];

		for (int timeIndex = 0; timeIndex < numberOfColumns; timeIndex++) {
			final RationalFunctionInterpolation interpolator = new RationalFunctionInterpolation(
					spotGrid,
					getColumn(activatedValues, timeIndex),
					InterpolationMethod.LINEAR,
					ExtrapolationMethod.CONSTANT
			);
			traceValues[timeIndex] = interpolator.getValue(barrier);
		}

		return new DoubleBarrierTrace1D(
				maturity,
				disc.getTimeDiscretization(),
				traceValues
		);
	}

	private DoubleBarrierTrace2D extractActivatedBoundaryTrace2D(
			final FiniteDifferenceEquityModel activatedModel,
			final double[][] activatedValues,
			final double barrier) {

		final SpaceTimeDiscretization disc = activatedModel.getSpaceTimeDiscretization();

		final double[] spotGrid = disc.getSpaceGrid(0).getGrid();
		final double[] secondGrid = disc.getSpaceGrid(1).getGrid();

		final int n0 = spotGrid.length;
		final int n1 = secondGrid.length;
		final int numberOfColumns = activatedValues[0].length;

		final double[][] traceValues = new double[n1][numberOfColumns];

		for (int timeIndex = 0; timeIndex < numberOfColumns; timeIndex++) {
			for (int j = 0; j < n1; j++) {
				final double[] slice = new double[n0];
				for (int i = 0; i < n0; i++) {
					slice[i] = activatedValues[flatten(i, j, n0)][timeIndex];
				}

				final RationalFunctionInterpolation interpolator = new RationalFunctionInterpolation(
						spotGrid,
						slice,
						InterpolationMethod.LINEAR,
						ExtrapolationMethod.CONSTANT
				);

				traceValues[j][timeIndex] = interpolator.getValue(barrier);
			}
		}

		return new DoubleBarrierTrace2D(
				maturity,
				secondGrid,
				disc.getTimeDiscretization(),
				traceValues
		);
	}

	/*
	 * ============================================
	 * STITCHING
	 * ============================================
	 */

	private double[][] assembleDirectKnockInSurface1D(
			final FiniteDifferenceEquityModel originalModel,
			final FiniteDifferenceEquityModel activatedModel,
			final double[][] activatedValues,
			final FiniteDifferenceEquityModel preHitModel,
			final double[][] preHitValues) {

		final double[] originalGrid = getValuationSpaceTimeDiscretization(originalModel).getSpaceGrid(0).getGrid();

		final double[][] activatedOnOriginalGrid = interpolateSurfaceToOriginalGrid1D(
				activatedValues,
				activatedModel.getSpaceTimeDiscretization().getSpaceGrid(0).getGrid(),
				originalGrid
		);

		final double[][] preHitOnOriginalGrid = interpolateSurfaceToOriginalGrid1D(
				preHitValues,
				preHitModel.getSpaceTimeDiscretization().getSpaceGrid(0).getGrid(),
				originalGrid
		);

		final int numberOfColumns = activatedOnOriginalGrid[0].length;
		final double[][] result = new double[originalGrid.length][numberOfColumns];

		for (int i = 0; i < originalGrid.length; i++) {
			final boolean alreadyActivated = isKnockedIn(originalGrid[i]);
			final double[][] source = alreadyActivated ? activatedOnOriginalGrid : preHitOnOriginalGrid;

			for (int timeIndex = 0; timeIndex < numberOfColumns; timeIndex++) {
				result[i][timeIndex] = source[i][timeIndex];
			}
		}

		return result;
	}

	private double[][] assembleDirectKnockInSurface2D(
			final FiniteDifferenceEquityModel originalModel,
			final FiniteDifferenceEquityModel activatedModel,
			final double[][] activatedValues,
			final FiniteDifferenceEquityModel preHitModel,
			final double[][] preHitValues) {

		final SpaceTimeDiscretization originalDiscretization = getValuationSpaceTimeDiscretization(originalModel);

		final double[][] activatedOnOriginalGrid = interpolateSurfaceToOriginalGrid2DAlongFirstState(
				activatedValues,
				activatedModel.getSpaceTimeDiscretization(),
				originalDiscretization
		);

		final double[][] preHitOnOriginalGrid = interpolateSurfaceToOriginalGrid2DAlongFirstState(
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
				final boolean alreadyActivated = isKnockedIn(x0[i]);
				final double[][] source = alreadyActivated ? activatedOnOriginalGrid : preHitOnOriginalGrid;
				final int k = flatten(i, j, x0.length);

				for (int timeIndex = 0; timeIndex < numberOfColumns; timeIndex++) {
					result[k][timeIndex] = source[k][timeIndex];
				}
			}
		}

		return result;
	}

	/*
	 * ============================================
	 * INTERPOLATION
	 * ============================================
	 */

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
					"2D interpolation currently requires the second state-variable grid to remain unchanged.");
		}

		for (int j = 0; j < originalX1.length; j++) {
			if (Math.abs(auxiliaryX1[j] - originalX1[j]) > 1E-12) {
				throw new IllegalArgumentException(
						"2D interpolation currently requires the second state-variable grid to remain unchanged.");
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
					auxiliarySlice[i] = valuesOnAuxiliaryGrid[flatten(i, j, auxiliaryN0)][timeIndex];
				}

				final RationalFunctionInterpolation interpolator = new RationalFunctionInterpolation(
						auxiliaryX0,
						auxiliarySlice,
						InterpolationMethod.LINEAR,
						ExtrapolationMethod.CONSTANT
				);

				for (int i = 0; i < originalN0; i++) {
					interpolatedValues[flatten(i, j, originalN0)][timeIndex] =
							interpolator.getValue(originalX0[i]);
				}
			}
		}

		return interpolatedValues;
	}

	/*
	 * ============================================
	 * PAYOFFS / TERMINALS / STATE LOGIC
	 * ============================================
	 */

	private double[] buildZeroTerminalValues(final SpaceTimeDiscretization discretization) {
		final double[] spotGrid = discretization.getSpaceGrid(0).getGrid();
		final double[] zero = new double[spotGrid.length];
		for (int i = 0; i < zero.length; i++) {
			zero[i] = 0.0;
		}
		return zero;
	}

	private double[] buildCellAveragedTerminalValues(final SpaceTimeDiscretization discretization) {
		final double[] sGrid = discretization.getSpaceGrid(0).getGrid();
		final double[] terminalValues = new double[sGrid.length];

		for (int i = 0; i < sGrid.length; i++) {
			final double leftEdge = getLeftDualCellEdge(sGrid, i);
			final double rightEdge = getRightDualCellEdge(sGrid, i);
			terminalValues[i] = cellAveragedPayoffForDirectOutPricing(leftEdge, rightEdge);
		}

		return terminalValues;
	}

	private double cellAveragedPayoffForDirectOutPricing(final double leftEdge, final double rightEdge) {
		if (!(leftEdge < rightEdge)) {
			throw new IllegalArgumentException("Require leftEdge < rightEdge.");
		}

		final double aliveLeft = Math.max(leftEdge, lowerBarrier);
		final double aliveRight = Math.min(rightEdge, upperBarrier);

		if (aliveRight <= aliveLeft) {
			return 0.0;
		}

		final double aliveWeight = (aliveRight - aliveLeft) / (rightEdge - leftEdge);
		return aliveWeight * cellAveragedVanillaPayoff(aliveLeft, aliveRight);
	}

	private double cellAveragedVanillaPayoff(final double leftEdge, final double rightEdge) {
		final double cellLength = rightEdge - leftEdge;

		if (callOrPutSign == CallOrPut.CALL) {
			if (rightEdge <= strike) {
				return 0.0;
			}
			if (leftEdge >= strike) {
				return 0.5 * (leftEdge + rightEdge) - strike;
			}
			return 0.5 * Math.pow(rightEdge - strike, 2.0) / cellLength;
		} else {
			if (leftEdge >= strike) {
				return 0.0;
			}
			if (rightEdge <= strike) {
				return strike - 0.5 * (leftEdge + rightEdge);
			}
			return 0.5 * Math.pow(strike - leftEdge, 2.0) / cellLength;
		}
	}

	private double pointwisePayoffForDirectOutPricing(final double assetValue) {
		if (!isAliveInsideBand(assetValue)) {
			return 0.0;
		}
		return pointwiseVanillaPayoff(assetValue);
	}

	private double pointwiseVanillaPayoff(final double assetValue) {
		if (callOrPutSign == CallOrPut.CALL) {
			return Math.max(assetValue - strike, 0.0);
		}
		return Math.max(strike - assetValue, 0.0);
	}

	private boolean isOutOption() {
		return doubleBarrierType == DoubleBarrierType.KNOCK_OUT;
	}

	private boolean isAliveInsideBand(final double assetValue) {
		return assetValue > lowerBarrier && assetValue < upperBarrier;
	}

	private boolean isKnockedIn(final double assetValue) {
		return assetValue <= lowerBarrier || assetValue >= upperBarrier;
	}

	private boolean isDegenerateZeroCase() {
		return (doubleBarrierType == DoubleBarrierType.KNOCK_OUT && callOrPutSign == CallOrPut.CALL && upperBarrier <= strike)
				|| (doubleBarrierType == DoubleBarrierType.KNOCK_OUT && callOrPutSign == CallOrPut.PUT && lowerBarrier >= strike)
				|| (doubleBarrierType == DoubleBarrierType.KNOCK_IN && callOrPutSign == CallOrPut.CALL && lowerBarrier >= strike)
				|| (doubleBarrierType == DoubleBarrierType.KNOCK_IN && callOrPutSign == CallOrPut.PUT && upperBarrier <= strike);
	}

	private boolean isDegenerateVanillaCase() {
		return (doubleBarrierType == DoubleBarrierType.KNOCK_IN && callOrPutSign == CallOrPut.CALL && upperBarrier <= strike)
				|| (doubleBarrierType == DoubleBarrierType.KNOCK_IN && callOrPutSign == CallOrPut.PUT && lowerBarrier >= strike);
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

	/*
	 * ============================================
	 * DISCRETE MONITORING HOOKS
	 * ============================================
	 */

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

		if (isOutOption()) {
			if (dims == 1) {
				return applyDiscreteOutEvent1D(valuesAfterEvent, model.getSpaceTimeDiscretization().getSpaceGrid(0).getGrid());
			} else if (dims == 2) {
				return applyDiscreteOutEvent2D(
						valuesAfterEvent,
						model.getSpaceTimeDiscretization().getSpaceGrid(0).getGrid(),
						model.getSpaceTimeDiscretization().getSpaceGrid(1).getGrid()
				);
			}
		} else {
			final ActivatedVectorEventState eventState = getCurrentActivatedVectorEventState();

			if (dims == 1) {
				return applyDiscreteInEvent1D(
						time,
						valuesAfterEvent,
						model.getSpaceTimeDiscretization().getSpaceGrid(0).getGrid(),
						eventState
				);
			} else if (dims == 2) {
				return applyDiscreteInEvent2D(
						time,
						valuesAfterEvent,
						model.getSpaceTimeDiscretization().getSpaceGrid(0).getGrid(),
						model.getSpaceTimeDiscretization().getSpaceGrid(1).getGrid(),
						eventState
				);
			}
		}

		throw new IllegalArgumentException("Only 1D and 2D grids are supported.");
	}

	private double[] applyEvaluationTimeDiscreteCondition(
			final double evaluationTime,
			final double[] valuesAtEvaluationTime,
			final FiniteDifferenceEquityModel model) {

		if (!usesDiscreteMonitoring()) {
			return valuesAtEvaluationTime;
		}

		final int dims = model.getSpaceTimeDiscretization().getNumberOfSpaceGrids();

		if (isOutOption()) {
			if (dims == 1) {
				return applyDiscreteOutEvent1D(valuesAtEvaluationTime, model.getSpaceTimeDiscretization().getSpaceGrid(0).getGrid());
			} else if (dims == 2) {
				return applyDiscreteOutEvent2D(
						valuesAtEvaluationTime,
						model.getSpaceTimeDiscretization().getSpaceGrid(0).getGrid(),
						model.getSpaceTimeDiscretization().getSpaceGrid(1).getGrid()
				);
			}
			return valuesAtEvaluationTime;
		}

		final FiniteDifferenceEquityModel effectiveModel = getEffectiveModelForValuation(model);
		final FiniteDifferenceEquityProduct activatedProduct = createActivatedVanillaProduct();

		final Map<Double, double[]> activatedVectorsAtEventTimes = new HashMap<>();
		activatedVectorsAtEventTimes.put(
				evaluationTime,
				activatedProduct.getValue(evaluationTime, effectiveModel).clone()
		);

		final ActivatedVectorEventState eventState = new ActivatedVectorEventState(
				activatedVectorsAtEventTimes,
				DiscreteMonitoringSupport.DEFAULT_MONITORING_TIME_TOLERANCE
		);

		if (dims == 1) {
			return applyDiscreteInEvent1D(
					evaluationTime,
					valuesAtEvaluationTime,
					model.getSpaceTimeDiscretization().getSpaceGrid(0).getGrid(),
					eventState
			);
		} else if (dims == 2) {
			return applyDiscreteInEvent2D(
					evaluationTime,
					valuesAtEvaluationTime,
					model.getSpaceTimeDiscretization().getSpaceGrid(0).getGrid(),
					model.getSpaceTimeDiscretization().getSpaceGrid(1).getGrid(),
					eventState
			);
		}

		return valuesAtEvaluationTime;
	}

	private double[] applyDiscreteOutEvent1D(
			final double[] valuesAfterEvent,
			final double[] spotGrid) {

		final double[] valuesBeforeEvent = valuesAfterEvent.clone();

		for (int i = 0; i < spotGrid.length; i++) {
			if (!isAliveInsideBand(spotGrid[i])) {
				valuesBeforeEvent[i] = 0.0;
			}
		}

		return valuesBeforeEvent;
	}

	private double[] applyDiscreteOutEvent2D(
			final double[] valuesAfterEvent,
			final double[] x0,
			final double[] x1) {

		final double[] valuesBeforeEvent = valuesAfterEvent.clone();

		for (int j = 0; j < x1.length; j++) {
			for (int i = 0; i < x0.length; i++) {
				if (!isAliveInsideBand(x0[i])) {
					valuesBeforeEvent[flatten(i, j, x0.length)] = 0.0;
				}
			}
		}

		return valuesBeforeEvent;
	}

	private double[] applyDiscreteInEvent1D(
			final double time,
			final double[] valuesAfterEvent,
			final double[] spotGrid,
			final ActivatedVectorEventState eventState) {

		final double[] activatedVector = eventState.getActivatedVector(time);
		final double[] valuesBeforeEvent = valuesAfterEvent.clone();

		for (int i = 0; i < spotGrid.length; i++) {
			if (isKnockedIn(spotGrid[i])) {
				valuesBeforeEvent[i] = activatedVector[i];
			}
		}

		return valuesBeforeEvent;
	}

	private double[] applyDiscreteInEvent2D(
			final double time,
			final double[] valuesAfterEvent,
			final double[] x0,
			final double[] x1,
			final ActivatedVectorEventState eventState) {

		final double[] activatedVector = eventState.getActivatedVector(time);
		final double[] valuesBeforeEvent = valuesAfterEvent.clone();

		for (int j = 0; j < x1.length; j++) {
			for (int i = 0; i < x0.length; i++) {
				if (isKnockedIn(x0[i])) {
					valuesBeforeEvent[flatten(i, j, x0.length)] =
							activatedVector[flatten(i, j, x0.length)];
				}
			}
		}

		return valuesBeforeEvent;
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

	private boolean isMonitoringTime(final double time) {
		return DiscreteMonitoringSupport.isMonitoringTime(
				time,
				monitoringTimes,
				DiscreteMonitoringSupport.DEFAULT_MONITORING_TIME_TOLERANCE
		);
	}

	/*
	 * ============================================
	 * INTERNAL CONSTRAINTS
	 * ============================================
	 */

	/**
	 * Returns whether the internal knock-out constraint is active.
	 *
	 * @param time Evaluation time.
	 * @param stateVariables State variables.
	 * @return {@code true} if the state is outside the alive band.
	 */
	@Override
	public boolean isConstraintActive(final double time, final double... stateVariables) {
		if (!isOutOption()) {
			return false;
		}

		if (usesDiscreteMonitoring()) {
			return false;
		}

		final double underlyingLevel = stateVariables[0];
		return underlyingLevel <= lowerBarrier || underlyingLevel >= upperBarrier;
	}

	/**
	 * Returns the constrained value in the knocked-out region.
	 *
	 * @param time Evaluation time.
	 * @param stateVariables State variables.
	 * @return The constrained value.
	 */
	@Override
	public double getConstrainedValue(final double time, final double... stateVariables) {
		if (!isOutOption()) {
			throw new IllegalStateException("Internal constrained value requested for a non knock-out product.");
		}
		return 0.0;
	}

	/*
	 * ============================================
	 * MODEL / GRID HELPERS
	 * ============================================
	 */

	private void validateBarriersInsideGrid(final FiniteDifferenceEquityModel model) {
		final double[] spotGrid = model.getSpaceTimeDiscretization().getSpaceGrid(0).getGrid();
		final double gridMin = spotGrid[0];
		final double gridMax = spotGrid[spotGrid.length - 1];

		if (lowerBarrier < gridMin || upperBarrier > gridMax) {
			throw new IllegalArgumentException(
					"Both double barriers must lie inside the first state-variable grid domain of the supplied model.");
		}
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

	private static CallOrPut mapCallOrPut(final double sign) {
		if (sign == 1.0) {
			return CallOrPut.CALL;
		}
		if (sign == -1.0) {
			return CallOrPut.PUT;
		}
		throw new IllegalArgumentException("Unknown option type.");
	}

	/*
	 * ============================================
	 * GETTERS
	 * ============================================
	 */

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
	 * Returns the strike.
	 *
	 * @return The strike.
	 */
	public double getStrike() {
		return strike;
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
	 * Returns the call/put sign.
	 *
	 * @return The call/put sign.
	 */
	public CallOrPut getCallOrPut() {
		return callOrPutSign;
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

	/*
	 * ============================================
	 * TRACE HOLDERS
	 * ============================================
	 */

	private static final class DoubleBarrierTrace1D {

		/**
		 * The maturity.
		 */
		private final double maturity;
		/**
		 * The time discretization.
		 */
		private final TimeDiscretization timeDiscretization;
		/**
		 * The values.
		 */
		private final double[] values;

		private DoubleBarrierTrace1D(
				final double maturity,
				final TimeDiscretization timeDiscretization,
				final double[] values) {
			this.maturity = maturity;
			this.timeDiscretization = timeDiscretization;
			this.values = values;
		}

		private double getBoundaryValue(final double runningTime) {
			final double tau = Math.max(0.0, maturity - runningTime);
			int timeIndex = timeDiscretization.getTimeIndexNearestLessOrEqual(tau);

			if (timeIndex < 0) {
				timeIndex = 0;
			}
			if (timeIndex >= values.length) {
				timeIndex = values.length - 1;
			}

			return values[timeIndex];
		}
	}

	private static final class DoubleBarrierTrace2D {

		/**
		 * The maturity.
		 */
		private final double maturity;
		/**
		 * The second grid.
		 */
		private final double[] secondGrid;
		/**
		 * The time discretization.
		 */
		private final TimeDiscretization timeDiscretization;
		/**
		 * The values.
		 */
		private final double[][] values; // [secondIndex][timeIndex]

		private DoubleBarrierTrace2D(
				final double maturity,
				final double[] secondGrid,
				final TimeDiscretization timeDiscretization,
				final double[][] values) {
			this.maturity = maturity;
			this.secondGrid = secondGrid;
			this.timeDiscretization = timeDiscretization;
			this.values = values;
		}

		private double getBoundaryValue(final double runningTime, final double secondState) {
			final double tau = Math.max(0.0, maturity - runningTime);
			int timeIndex = timeDiscretization.getTimeIndexNearestLessOrEqual(tau);

			if (timeIndex < 0) {
				timeIndex = 0;
			}
			if (timeIndex >= values[0].length) {
				timeIndex = values[0].length - 1;
			}

			if (secondGrid.length == 1) {
				return values[0][timeIndex];
			}

			final int exactIndex = findExactGridIndex(secondGrid, secondState);
			if (exactIndex >= 0) {
				return values[exactIndex][timeIndex];
			}

			if (secondState <= secondGrid[0]) {
				return values[0][timeIndex];
			}
			if (secondState >= secondGrid[secondGrid.length - 1]) {
				return values[secondGrid.length - 1][timeIndex];
			}

			int upper = 1;
			while (upper < secondGrid.length && secondGrid[upper] < secondState) {
				upper++;
			}
			final int lower = upper - 1;

			final double xL = secondGrid[lower];
			final double xU = secondGrid[upper];

			final double vL = values[lower][timeIndex];
			final double vU = values[upper][timeIndex];

			final double weight = (secondState - xL) / (xU - xL);
			return (1.0 - weight) * vL + weight * vU;
		}

		private int findExactGridIndex(final double[] grid, final double x) {
			for (int i = 0; i < grid.length; i++) {
				if (Math.abs(grid[i] - x) <= GRID_TOLERANCE) {
					return i;
				}
			}
			return -1;
		}
	}

	/*
	 * ============================================
	 * 1D PRE-HIT MODEL WRAPPER
	 * ============================================
	 */

	private static final class DoubleBarrierPreHitModel1D
			implements FiniteDifferenceEquityModel, FiniteDifferenceBoundary {

		/**
		 * The delegate.
		 */
		private final FiniteDifferenceEquityModel delegate;
		/**
		 * The discretization.
		 */
		private final SpaceTimeDiscretization discretization;
		/**
		 * The lower trace.
		 */
		private final DoubleBarrierTrace1D lowerTrace;
		/**
		 * The upper trace.
		 */
		private final DoubleBarrierTrace1D upperTrace;

		private DoubleBarrierPreHitModel1D(
				final FiniteDifferenceEquityModel delegate,
				final SpaceTimeDiscretization discretization,
				final DoubleBarrierTrace1D lowerTrace,
				final DoubleBarrierTrace1D upperTrace) {
			this.delegate = delegate;
			this.discretization = discretization;
			this.lowerTrace = lowerTrace;
			this.upperTrace = upperTrace;
		}

		@Override
		public DiscountCurve getRiskFreeCurve() {
			return delegate.getRiskFreeCurve();
		}

		@Override
		public DiscountCurve getDividendYieldCurve() {
			return delegate.getDividendYieldCurve();
		}

		@Override
		public SpaceTimeDiscretization getSpaceTimeDiscretization() {
			return discretization;
		}

		@Override
		public double[] getDrift(final double time, final double... stateVariables) {
			return delegate.getDrift(time, stateVariables);
		}

		@Override
		public double[][] getFactorLoading(final double time, final double... stateVariables) {
			return delegate.getFactorLoading(time, stateVariables);
		}

		@Override
		public double[] getInitialValue() {
			return delegate.getInitialValue();
		}

		@Override
		public BoundaryCondition[] getBoundaryConditionsAtLowerBoundary(
				final FiniteDifferenceEquityProduct product,
				final double time,
				final double... stateVariables) {

			final BoundaryCondition[] base =
					delegate.getBoundaryConditionsAtLowerBoundary(product, time, stateVariables);

			final BoundaryCondition[] result =
					base != null ? base.clone() : new BoundaryCondition[] {StandardBoundaryCondition.none() };

			result[0] = StandardBoundaryCondition.dirichlet(lowerTrace.getBoundaryValue(time));
			return result;
		}

		@Override
		public BoundaryCondition[] getBoundaryConditionsAtUpperBoundary(
				final FiniteDifferenceEquityProduct product,
				final double time,
				final double... stateVariables) {

			final BoundaryCondition[] base =
					delegate.getBoundaryConditionsAtUpperBoundary(product, time, stateVariables);

			final BoundaryCondition[] result =
					base != null ? base.clone() : new BoundaryCondition[] {StandardBoundaryCondition.none() };

			result[0] = StandardBoundaryCondition.dirichlet(upperTrace.getBoundaryValue(time));
			return result;
		}

		@Override
		public FiniteDifferenceEquityModel getCloneWithModifiedSpaceTimeDiscretization(
				final SpaceTimeDiscretization newSpaceTimeDiscretization) {

			final FiniteDifferenceEquityModel clonedDelegate =
					delegate.getCloneWithModifiedSpaceTimeDiscretization(newSpaceTimeDiscretization);

			return new DoubleBarrierPreHitModel1D(
					clonedDelegate,
					newSpaceTimeDiscretization,
					lowerTrace,
					upperTrace
			);
		}
	}

	/*
	 * ============================================
	 * 2D PRE-HIT MODEL WRAPPER
	 * ============================================
	 */

	private static final class DoubleBarrierPreHitHestonModel
			extends FDMHestonModel
			implements FiniteDifferenceBoundary {

		/**
		 * The base model.
		 */
		private final FDMHestonModel baseModel;
		/**
		 * The discretization.
		 */
		private final SpaceTimeDiscretization discretization;
		/**
		 * The lower trace.
		 */
		private final DoubleBarrierTrace2D lowerTrace;
		/**
		 * The upper trace.
		 */
		private final DoubleBarrierTrace2D upperTrace;

		private DoubleBarrierPreHitHestonModel(
				final FDMHestonModel baseModel,
				final SpaceTimeDiscretization discretization,
				final DoubleBarrierTrace2D lowerTrace,
				final DoubleBarrierTrace2D upperTrace) {

			super(
					baseModel.getInitialValue()[0],
					baseModel.getInitialValue()[1],
					baseModel.getRiskFreeCurve(),
					baseModel.getDividendYieldCurve(),
					baseModel.getKappa(),
					baseModel.getThetaV(),
					baseModel.getSigma(),
					baseModel.getRho(),
					discretization
			);

			this.baseModel = baseModel;
			this.discretization = discretization;
			this.lowerTrace = lowerTrace;
			this.upperTrace = upperTrace;
		}

		@Override
		public SpaceTimeDiscretization getSpaceTimeDiscretization() {
			return discretization;
		}

		@Override
		public BoundaryCondition[] getBoundaryConditionsAtLowerBoundary(
				final FiniteDifferenceEquityProduct product,
				final double time,
				final double... stateVariables) {

			final BoundaryCondition[] base =
					baseModel.getBoundaryConditionsAtLowerBoundary(product, time, stateVariables);

			final BoundaryCondition[] result = base.clone();

			final double secondState = stateVariables.length > 1 ? stateVariables[1] : getInitialValue()[1];

			/*
			 * First state variable = spot boundary at lower barrier.
			 * Second state variable boundary stays whatever the original Heston
			 * model/product boundary provides.
			 */
			result[0] = StandardBoundaryCondition.dirichlet(
					lowerTrace.getBoundaryValue(time, secondState)
			);

			return result;
		}

		@Override
		public BoundaryCondition[] getBoundaryConditionsAtUpperBoundary(
				final FiniteDifferenceEquityProduct product,
				final double time,
				final double... stateVariables) {

			final BoundaryCondition[] base =
					baseModel.getBoundaryConditionsAtUpperBoundary(product, time, stateVariables);

			final BoundaryCondition[] result = base.clone();

			final double secondState = stateVariables.length > 1 ? stateVariables[1] : getInitialValue()[1];

			/*
			 * First state variable = spot boundary at upper barrier.
			 * Second state variable boundary stays whatever the original Heston
			 * model/product boundary provides.
			 */
			result[0] = StandardBoundaryCondition.dirichlet(
					upperTrace.getBoundaryValue(time, secondState)
			);

			return result;
		}

		@Override
		public FiniteDifferenceEquityModel getCloneWithModifiedSpaceTimeDiscretization(
				final SpaceTimeDiscretization newSpaceTimeDiscretization) {

			final FDMHestonModel clonedBaseModel = new FDMHestonModel(
					baseModel.getInitialValue()[0],
					baseModel.getInitialValue()[1],
					baseModel.getRiskFreeCurve(),
					baseModel.getDividendYieldCurve(),
					baseModel.getKappa(),
					baseModel.getThetaV(),
					baseModel.getSigma(),
					baseModel.getRho(),
					newSpaceTimeDiscretization
			);

			return new DoubleBarrierPreHitHestonModel(
					clonedBaseModel,
					newSpaceTimeDiscretization,
					lowerTrace,
					upperTrace
			);
		}
	}

	private static final class DoubleBarrierPreHitSabrModel
			extends FDMSabrModel
			implements FiniteDifferenceBoundary {

		/**
		 * The base model.
		 */
		private final FDMSabrModel baseModel;
		/**
		 * The discretization.
		 */
		private final SpaceTimeDiscretization discretization;
		/**
		 * The lower trace.
		 */
		private final DoubleBarrierTrace2D lowerTrace;
		/**
		 * The upper trace.
		 */
		private final DoubleBarrierTrace2D upperTrace;

		private DoubleBarrierPreHitSabrModel(
				final FDMSabrModel baseModel,
				final SpaceTimeDiscretization discretization,
				final DoubleBarrierTrace2D lowerTrace,
				final DoubleBarrierTrace2D upperTrace) {

			super(
					baseModel.getInitialSpot(),
					baseModel.getInitialAlpha(),
					baseModel.getRiskFreeCurve(),
					baseModel.getDividendYieldCurve(),
					baseModel.getBeta(),
					baseModel.getNu(),
					baseModel.getRho(),
					discretization
			);

			this.baseModel = baseModel;
			this.discretization = discretization;
			this.lowerTrace = lowerTrace;
			this.upperTrace = upperTrace;
		}

		@Override
		public SpaceTimeDiscretization getSpaceTimeDiscretization() {
			return discretization;
		}

		@Override
		public BoundaryCondition[] getBoundaryConditionsAtLowerBoundary(
				final FiniteDifferenceEquityProduct product,
				final double time,
				final double... stateVariables) {

			final BoundaryCondition[] base =
					baseModel.getBoundaryConditionsAtLowerBoundary(product, time, stateVariables);

			final BoundaryCondition[] result = base.clone();

			final double secondState = stateVariables.length > 1 ? stateVariables[1] : getInitialValue()[1];

			/*
			 * First state variable = spot boundary at lower barrier.
			 * Second state variable boundary stays whatever the original SABR
			 * model/product boundary provides.
			 */
			result[0] = StandardBoundaryCondition.dirichlet(
					lowerTrace.getBoundaryValue(time, secondState)
			);

			return result;
		}

		@Override
		public BoundaryCondition[] getBoundaryConditionsAtUpperBoundary(
				final FiniteDifferenceEquityProduct product,
				final double time,
				final double... stateVariables) {

			final BoundaryCondition[] base =
					baseModel.getBoundaryConditionsAtUpperBoundary(product, time, stateVariables);

			final BoundaryCondition[] result = base.clone();

			final double secondState = stateVariables.length > 1 ? stateVariables[1] : getInitialValue()[1];

			/*
			 * First state variable = spot boundary at upper barrier.
			 * Second state variable boundary stays whatever the original SABR
			 * model/product boundary provides.
			 */
			result[0] = StandardBoundaryCondition.dirichlet(
					upperTrace.getBoundaryValue(time, secondState)
			);

			return result;
		}

		@Override
		public FiniteDifferenceEquityModel getCloneWithModifiedSpaceTimeDiscretization(
				final SpaceTimeDiscretization newSpaceTimeDiscretization) {

			final FDMSabrModel clonedBaseModel = new FDMSabrModel(
					baseModel.getInitialSpot(),
					baseModel.getInitialAlpha(),
					baseModel.getRiskFreeCurve(),
					baseModel.getDividendYieldCurve(),
					baseModel.getBeta(),
					baseModel.getNu(),
					baseModel.getRho(),
					newSpaceTimeDiscretization
			);

			return new DoubleBarrierPreHitSabrModel(
					clonedBaseModel,
					newSpaceTimeDiscretization,
					lowerTrace,
					upperTrace
			);
		}
	}
}
