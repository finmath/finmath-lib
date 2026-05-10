package net.finmath.finitedifference.solvers;

import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;

import net.finmath.finitedifference.FiniteDifferenceExerciseUtil;
import net.finmath.finitedifference.FiniteDifferenceModel;
import net.finmath.finitedifference.FiniteDifferenceProduct;
import net.finmath.finitedifference.assetderivativevaluation.models.FiniteDifferenceEquityModel;
import net.finmath.finitedifference.assetderivativevaluation.products.FiniteDifferenceEquityEventProduct;
import net.finmath.finitedifference.assetderivativevaluation.products.FiniteDifferenceEquityProduct;
import net.finmath.finitedifference.assetderivativevaluation.products.FiniteDifferenceInternalStateConstraint;
import net.finmath.finitedifference.boundaries.BoundaryCondition;
import net.finmath.finitedifference.grids.SpaceTimeDiscretization;
import net.finmath.finitedifference.interestrate.models.FiniteDifferenceInterestRateModel;
import net.finmath.finitedifference.interestrate.products.FiniteDifferenceInterestRateProduct;
import net.finmath.modelling.Exercise;

/**
 * Theta-method solver for a one-dimensional PDE in state-variable form.
 *
 * <p>
 * The solver supports:
 * </p>
 * <ul>
 *   <li>pointwise terminal payoff initialization,</li>
 *   <li>direct terminal-vector initialization,</li>
 * <li>direct terminal-vector initialization with separate pointwise exercise
 * payoff,</li>
 * <li>direct terminal-vector initialization with a continuous time-dependent
 * obstacle,</li>
 * <li>vector-level event conditions applied on the value vector at prescribed
 * event times.</li>
 * </ul>
 *
 * <p>
 * The third case is useful for Bermudan and American digitals, where the
 * maturity
 * layer should be cell-averaged, while early-exercise projection should remain
 * pointwise.
 * </p>
 *
 * <p>
 * The fourth case is useful for shout-style problems, where the solution is
 * constrained
 * by a time- and state-dependent continuation floor {@code V >= V*(t,x)} at
 * every time step.
 * </p>
 *
 * <p>
 * If the product is a {@link FiniteDifferenceInterestRateProduct} or a
 * {@link FiniteDifferenceEquityEventProduct}, then at every event time {@code
 * t}
 * the solver applies the vector-level jump condition
 * </p>
 *
 * <p>
 * <i>
 * V(t^{-},x) = J_t(V(t^{+},x),x).
 * </i>
 * </p>
 *
 * <p>
 * Event times are required to be part of the solver time discretization.
 * </p>
 *
 * @author Alessandro Gnoatto
 * @author Ralph Rudd
 * @author Christian Fries
 * @author Jorg Kienitz
 */
public class FDMThetaMethod1D implements FDMSolver {

	/**
	 * The safe time epsilon.
	 */
	private static final double SAFE_TIME_EPSILON = 1E-6;
	/**
	 * The event time tolerance.
	 */
	private static final double EVENT_TIME_TOLERANCE = 1E-12;
	/**
	 * The local discount bond epsilon.
	 */
	private static final double LOCAL_DISCOUNT_BOND_EPSILON = 1E-6;

	/**
	 * The model.
	 */
	private final FiniteDifferenceModel model;
	/**
	 * The product.
	 */
	private final FiniteDifferenceProduct<? extends FiniteDifferenceModel> product;
	/**
	 * The space time discretization.
	 */
	private final SpaceTimeDiscretization spaceTimeDiscretization;
	/**
	 * The exercise.
	 */
	private final Exercise exercise;

	private static final class ModelCoefficients {

		/**
		 * The drift.
		 */
		private final double[] drift;
		/**
		 * The variance.
		 */
		private final double[] variance;
		/**
		 * The local discount rate.
		 */
		private final double[] localDiscountRate;

		private ModelCoefficients(
				final double[] drift,
				final double[] variance,
				final double[] localDiscountRate) {
			this.drift = drift;
			this.variance = variance;
			this.localDiscountRate = localDiscountRate;
		}

		private double[] getDrift() {
			return drift;
		}

		private double[] getVariance() {
			return variance;
		}

		private double[] getLocalDiscountRate() {
			return localDiscountRate;
		}
	}

	/**
	 * Creates a theta-method finite-difference solver for a one-dimensional
	 * backward PDE.
	 *
	 * @param model The finite-difference equity model providing PDE
	 *     coefficients and boundary conditions.
	 * @param product The equity product to be valued.
	 * @param spaceTimeDiscretization The joint space-time discretization.
	 * @param exercise The exercise specification.
	 */
	public FDMThetaMethod1D(
			final FiniteDifferenceEquityModel model,
			final FiniteDifferenceEquityProduct product,
			final SpaceTimeDiscretization spaceTimeDiscretization,
			final Exercise exercise) {
		this((FiniteDifferenceModel) model, product, spaceTimeDiscretization, exercise);
	}

	/**
	 * Creates a theta-method finite-difference solver for a one-dimensional
	 * backward PDE.
	 *
	 * @param model The finite-difference interest-rate model providing PDE
	 *     coefficients and boundary conditions.
	 * @param product The interest-rate product to be valued.
	 * @param spaceTimeDiscretization The joint space-time discretization.
	 * @param exercise The exercise specification.
	 */
	public FDMThetaMethod1D(
			final FiniteDifferenceInterestRateModel model,
			final FiniteDifferenceInterestRateProduct product,
			final SpaceTimeDiscretization spaceTimeDiscretization,
			final Exercise exercise) {
		this((FiniteDifferenceModel) model, product, spaceTimeDiscretization, exercise);
	}

	/**
	 * Creates a theta-method finite-difference solver for a one-dimensional
	 * backward PDE.
	 *
	 * @param model The finite-difference model providing PDE coefficients and
	 *     boundary conditions.
	 * @param product The product to be valued.
	 * @param spaceTimeDiscretization The joint space-time discretization.
	 * @param exercise The exercise specification.
	 */
	public FDMThetaMethod1D(
			final FiniteDifferenceModel model,
			final FiniteDifferenceProduct<? extends FiniteDifferenceModel> product,
			final SpaceTimeDiscretization spaceTimeDiscretization,
			final Exercise exercise) {

		if (model == null) {
			throw new IllegalArgumentException("model must not be null.");
		}
		if (product == null) {
			throw new IllegalArgumentException("product must not be null.");
		}
		if (spaceTimeDiscretization == null) {
			throw new IllegalArgumentException("spaceTimeDiscretization must not be null.");
		}
		if (exercise == null) {
			throw new IllegalArgumentException("exercise must not be null.");
		}
		if (spaceTimeDiscretization.getNumberOfSpaceGrids() != 1) {
			throw new IllegalArgumentException("FDMThetaMethod1D requires a one-dimensional space discretization.");
		}

		this.model = model;
		this.product = product;
		this.spaceTimeDiscretization = spaceTimeDiscretization;
		this.exercise = exercise;

		validateModelProductCompatibility();
		validateProductEventTimesInGrid();
	}

	@Override
	public double[][] getValues(final double time, final DoubleUnaryOperator valueAtMaturity) {

		final double[] xGrid = spaceTimeDiscretization.getSpaceGrid(0).getGrid();
		final double[] terminalValues = new double[xGrid.length];

		for (int i = 0; i < xGrid.length; i++) {
			terminalValues[i] = valueAtMaturity.applyAsDouble(xGrid[i]);
		}

		return getValuesInternal(time, terminalValues, valueAtMaturity, null);
	}

	@Override
	public double[] getValue(
			final double evaluationTime,
			final double time,
			final DoubleUnaryOperator valueAtMaturity) {

		final double[][] values = getValues(time, valueAtMaturity);
		return extractTimeSlice(values, time, evaluationTime);
	}

	@Override
	public double[][] getValues(final double time, final double[] terminalValues) {
		return getValuesInternal(time, terminalValues, null, null);
	}

	@Override
	public double[] getValue(
			final double evaluationTime,
			final double time,
			final double[] terminalValues) {

		final double[][] values = getValues(time, terminalValues);
		return extractTimeSlice(values, time, evaluationTime);
	}

	@Override
	public double[][] getValues(
			final double time,
			final double[] terminalValues,
			final DoubleUnaryOperator exerciseValue) {
		return getValuesInternal(time, terminalValues, exerciseValue, null);
	}

	@Override
	public double[] getValue(
			final double evaluationTime,
			final double time,
			final double[] terminalValues,
			final DoubleUnaryOperator exerciseValue) {

		final double[][] values = getValues(time, terminalValues, exerciseValue);
		return extractTimeSlice(values, time, evaluationTime);
	}

	/**
	 * Returns the value.
	 *
	 * @param time The value.
	 * @param terminalValues The value.
	 * @param continuousObstacleValue The value.
	 * @return The value.
	 */
	public double[][] getValues(
			final double time,
			final double[] terminalValues,
			final DoubleBinaryOperator continuousObstacleValue) {
		return getValuesInternal(time, terminalValues, null, continuousObstacleValue);
	}

	/**
	 * Returns the value.
	 *
	 * @param evaluationTime The value.
	 * @param time The value.
	 * @param terminalValues The value.
	 * @param continuousObstacleValue The value.
	 * @return The value.
	 */
	public double[] getValue(
			final double evaluationTime,
			final double time,
			final double[] terminalValues,
			final DoubleBinaryOperator continuousObstacleValue) {

		final double[][] values = getValues(time, terminalValues, continuousObstacleValue);
		return extractTimeSlice(values, time, evaluationTime);
	}

	private double[] extractTimeSlice(
			final double[][] values,
			final double time,
			final double evaluationTime) {

		final double tau = time - evaluationTime;
		final int timeIndex = spaceTimeDiscretization.getTimeDiscretization().getTimeIndexNearestLessOrEqual(tau);

		final double[] column = new double[values.length];
		for (int i = 0; i < values.length; i++) {
			column[i] = values[i][timeIndex];
		}
		return column;
	}

	private double[][] getValuesInternal(
			final double time,
			final double[] terminalValues,
			final DoubleUnaryOperator exerciseValue,
			final DoubleBinaryOperator continuousObstacleValue) {

		final double[] xGrid = spaceTimeDiscretization.getSpaceGrid(0).getGrid();
		final int numberOfGridPoints = xGrid.length;

		if (terminalValues == null) {
			throw new IllegalArgumentException("terminalValues must not be null.");
		}
		if (terminalValues.length != numberOfGridPoints) {
			throw new IllegalArgumentException("terminalValues length does not match spatial grid length.");
		}
		if (exerciseValue != null && continuousObstacleValue != null) {
			throw new IllegalArgumentException(
					"Provide either a discrete exercise payoff or a continuous obstacle, not both.");
		}
		if ((exercise.isBermudan() || exercise.isAmerican())
				&& exerciseValue == null
				&& continuousObstacleValue == null) {
			throw new IllegalArgumentException(
					"Non-European exercise requires a pointwise exercise payoff function.");
		}

		final double theta = spaceTimeDiscretization.getTheta();
		final int timeLength = spaceTimeDiscretization.getTimeDiscretization().getNumberOfTimeSteps() + 1;
		final int numberOfTimeSteps = spaceTimeDiscretization.getTimeDiscretization().getNumberOfTimeSteps();
		final double horizon = spaceTimeDiscretization.getTimeDiscretization().getLastTime();

		double[] u = terminalValues.clone();
		final double[][] z = new double[numberOfGridPoints][timeLength];

		final BoundaryCondition lowerTerminalCondition = getLowerBoundaryCondition(horizon, xGrid[0]);
		final BoundaryCondition upperTerminalCondition = getUpperBoundaryCondition(horizon, xGrid[numberOfGridPoints - 1]);

		reimposeInternalConstraints(u, xGrid, horizon);
		reimposeBoundaryValues(u, lowerTerminalCondition, upperTerminalCondition);
		u = applyProductEventConditionIfNeeded(horizon, u);
		reimposeInternalConstraints(u, xGrid, horizon);
		reimposeBoundaryValues(u, lowerTerminalCondition, upperTerminalCondition);

		for (int i = 0; i < numberOfGridPoints; i++) {
			z[i][0] = u[i];
		}

		for (int m = 0; m < numberOfTimeSteps; m++) {

			final double deltaTau = spaceTimeDiscretization.getTimeDiscretization().getTimeStep(m);

			final double tm = spaceTimeDiscretization.getTimeDiscretization().getTime(numberOfTimeSteps - m);
			final double tmp1 = spaceTimeDiscretization.getTimeDiscretization().getTime(numberOfTimeSteps - (m + 1));

			final ModelCoefficients coefficientsAtCurrentTime = buildModelCoefficients(xGrid, tm);
			final ModelCoefficients coefficientsAtNextTime = buildModelCoefficients(xGrid, tmp1);

			final TridiagonalMatrix lhs = new TridiagonalMatrix(numberOfGridPoints);
			final TridiagonalMatrix rhsOperator = new TridiagonalMatrix(numberOfGridPoints);

			ThetaMethod1DAssembly.buildThetaLeftHandSide(
					lhs,
					xGrid,
					coefficientsAtNextTime.getDrift(),
					coefficientsAtNextTime.getVariance(),
					coefficientsAtNextTime.getLocalDiscountRate(),
					deltaTau,
					theta
			);

			ThetaMethod1DAssembly.buildThetaRightHandSide(
					rhsOperator,
					xGrid,
					coefficientsAtCurrentTime.getDrift(),
					coefficientsAtCurrentTime.getVariance(),
					coefficientsAtCurrentTime.getLocalDiscountRate(),
					deltaTau,
					theta
			);

			final double[] rhs = ThetaMethod1DAssembly.apply(rhsOperator, u);

			final double tauMp1 = spaceTimeDiscretization.getTimeDiscretization().getTime(m + 1);
			final double boundaryTime = horizon - tauMp1;

			final BoundaryCondition lowerCondition = getLowerBoundaryCondition(boundaryTime, xGrid[0]);
			final BoundaryCondition upperCondition = getUpperBoundaryCondition(boundaryTime, xGrid[numberOfGridPoints - 1]);

			if (lowerCondition.isDirichlet()) {
				ThetaMethod1DAssembly.overwriteAsDirichlet(lhs, rhs, 0, lowerCondition.getValue());
			}

			if (upperCondition.isDirichlet()) {
				ThetaMethod1DAssembly.overwriteAsDirichlet(lhs, rhs, numberOfGridPoints - 1, upperCondition.getValue());
			}

			for (int i = 1; i < numberOfGridPoints - 1; i++) {
				final double x = xGrid[i];
				if (isInternalConstraintActive(boundaryTime, x)) {
					ThetaMethod1DAssembly.overwriteAsDirichlet(
							lhs,
							rhs,
							i,
							getInternalConstrainedValue(boundaryTime, x)
					);
				}
			}

			final boolean isExerciseDate =
					FiniteDifferenceExerciseUtil.isExerciseAllowedAtTimeToMaturity(tauMp1, exercise);

			final double[] nextU;

			if (continuousObstacleValue != null) {
				final double[] obstacle = buildContinuousObstacleVector(
						xGrid,
						boundaryTime,
						continuousObstacleValue,
						lowerCondition,
						upperCondition
				);

				nextU = ProjectedTridiagonalSOR.solve(
						lhs,
						rhs,
						obstacle,
						u,
						1.2,
						500,
						1E-10
				);

				reimposeInternalConstraints(nextU, xGrid, boundaryTime);
				reimposeBoundaryValues(nextU, lowerCondition, upperCondition);
			} else if (exercise.isAmerican() && isExerciseDate) {

				final double[] obstacle = buildObstacleVector(
						xGrid,
						boundaryTime,
						exerciseValue,
						lowerCondition,
						upperCondition
				);

				nextU = ProjectedTridiagonalSOR.solve(
						lhs,
						rhs,
						obstacle,
						u,
						1.2,
						500,
						1E-10
				);

				reimposeInternalConstraints(nextU, xGrid, boundaryTime);
				reimposeBoundaryValues(nextU, lowerCondition, upperCondition);
			} else {
				nextU = ThomasSolver.solve(lhs.lower, lhs.diag, lhs.upper, rhs);

				if (isExerciseDate && (exercise.isBermudan() || exercise.isAmerican())) {
					applyExerciseProjection(
							nextU,
							xGrid,
							boundaryTime,
							exerciseValue,
							lowerCondition,
							upperCondition
					);
				} else {
					reimposeInternalConstraints(nextU, xGrid, boundaryTime);
					reimposeBoundaryValues(nextU, lowerCondition, upperCondition);
				}
			}

			u = applyProductEventConditionIfNeeded(boundaryTime, nextU);
			reimposeInternalConstraints(u, xGrid, boundaryTime);
			reimposeBoundaryValues(u, lowerCondition, upperCondition);

			for (int i = 0; i < numberOfGridPoints; i++) {
				z[i][m + 1] = u[i];
			}
		}

		return z;
	}

	private void validateModelProductCompatibility() {
		if (model instanceof FiniteDifferenceEquityModel && !(product instanceof FiniteDifferenceEquityProduct)) {
			throw new IllegalArgumentException(
					"An equity finite-difference model requires a FiniteDifferenceEquityProduct.");
		}
		if (model instanceof FiniteDifferenceInterestRateModel && !(product instanceof FiniteDifferenceInterestRateProduct)) {
			throw new IllegalArgumentException(
					"An interest-rate finite-difference model requires a FiniteDifferenceInterestRateProduct.");
		}
		if (!(model instanceof FiniteDifferenceEquityModel) && !(model instanceof FiniteDifferenceInterestRateModel)) {
			throw new IllegalArgumentException(
					"FDMThetaMethod1D currently supports only FiniteDifferenceEquityModel and FiniteDifferenceInterestRateModel.");
		}
	}

	private void validateProductEventTimesInGrid() {

		if (product instanceof FiniteDifferenceInterestRateProduct) {
			validateEventTimesInGrid(((FiniteDifferenceInterestRateProduct) product).getEventTimes());
			return;
		}

		if (product instanceof FiniteDifferenceEquityEventProduct) {
			validateEventTimesInGrid(((FiniteDifferenceEquityEventProduct) product).getEventTimes());
		}
	}

	private void validateEventTimesInGrid(final double[] eventTimes) {

		final double horizon = spaceTimeDiscretization.getTimeDiscretization().getLastTime();

		for (final double eventTime : eventTimes) {

			if (eventTime < -EVENT_TIME_TOLERANCE || eventTime > horizon + EVENT_TIME_TOLERANCE) {
				throw new IllegalArgumentException(
						"Event time " + eventTime
						+ " lies outside the solver time horizon [0," + horizon + "].");
			}

			final double tau = horizon - eventTime;
			final int timeIndex = spaceTimeDiscretization.getTimeDiscretization().getTimeIndex(tau);

			if (timeIndex < 0) {
				throw new IllegalArgumentException(
						"Event time " + eventTime
						+ " is not contained in the solver time discretization. "
						+ "Please refine the time grid so that all event times are grid points.");
			}
		}
	}

	private boolean isProductEventTime(final double time) {

		final double[] eventTimes;

		if (product instanceof FiniteDifferenceInterestRateProduct) {
			eventTimes = ((FiniteDifferenceInterestRateProduct) product).getEventTimes();
		} else if (product instanceof FiniteDifferenceEquityEventProduct) {
			eventTimes = ((FiniteDifferenceEquityEventProduct) product).getEventTimes();
		} else {
			return false;
		}

		for (final double eventTime : eventTimes) {
			if (Math.abs(eventTime - time) <= EVENT_TIME_TOLERANCE) {
				return true;
			}
		}

		return false;
	}

	private double[] applyProductEventConditionIfNeeded(
			final double time,
			final double[] valuesAfterEvent) {

		if (!isProductEventTime(time)) {
			return valuesAfterEvent;
		}

		if (product instanceof FiniteDifferenceInterestRateProduct) {
			return ((FiniteDifferenceInterestRateProduct) product).applyEventCondition(
					time,
					valuesAfterEvent,
					(FiniteDifferenceInterestRateModel) model
			);
		}

		if (product instanceof FiniteDifferenceEquityEventProduct) {
			return ((FiniteDifferenceEquityEventProduct) product).applyEventCondition(
					time,
					valuesAfterEvent,
					(FiniteDifferenceEquityModel) model
			);
		}

		return valuesAfterEvent;
	}

	private ModelCoefficients buildModelCoefficients(
			final double[] xGrid,
			final double time) {

		final int numberOfGridPoints = xGrid.length;

		final double[] drift = new double[numberOfGridPoints];
		final double[] variance = new double[numberOfGridPoints];
		final double[] localDiscountRate = new double[numberOfGridPoints];

		for (int i = 0; i < numberOfGridPoints; i++) {
			final double x = xGrid[i];

			drift[i] = getDriftAt(time, x);

			final double[][] factorLoading = getFactorLoadingAt(time, x);

			double localVariance = 0.0;
			for (int factor = 0; factor < factorLoading[0].length; factor++) {
				final double b = factorLoading[0][factor];
				localVariance += b * b;
			}
			variance[i] = localVariance;

			localDiscountRate[i] = getLocalShortRateAt(time, x);
		}

		return new ModelCoefficients(drift, variance, localDiscountRate);
	}

	private double getDriftAt(final double time, final double x) {
		if (model instanceof FiniteDifferenceEquityModel) {
			return ((FiniteDifferenceEquityModel) model).getDrift(time, x)[0];
		} else if (model instanceof FiniteDifferenceInterestRateModel) {
			return ((FiniteDifferenceInterestRateModel) model).getDrift(time, x)[0];
		} else {
			throw new IllegalStateException("Unsupported model type.");
		}
	}

	private double[][] getFactorLoadingAt(final double time, final double x) {
		if (model instanceof FiniteDifferenceEquityModel) {
			return ((FiniteDifferenceEquityModel) model).getFactorLoading(time, x);
		} else if (model instanceof FiniteDifferenceInterestRateModel) {
			return ((FiniteDifferenceInterestRateModel) model).getFactorLoading(time, x);
		} else {
			throw new IllegalStateException("Unsupported model type.");
		}
	}

	private double getLocalShortRateAt(final double time, final double x) {
		if (model instanceof FiniteDifferenceEquityModel) {
			final double safeTime = time == 0.0 ? SAFE_TIME_EPSILON : Math.max(time, SAFE_TIME_EPSILON);
			return -Math.log(((FiniteDifferenceEquityModel) model).getRiskFreeCurve().getDiscountFactor(safeTime)) / safeTime;
		} else if (model instanceof FiniteDifferenceInterestRateModel) {
			final FiniteDifferenceInterestRateModel interestRateModel =
					(FiniteDifferenceInterestRateModel) model;

			final double maturity = time + LOCAL_DISCOUNT_BOND_EPSILON;
			final double discountBond = interestRateModel.getDiscountBond(time, maturity, x);

			return -Math.log(discountBond) / LOCAL_DISCOUNT_BOND_EPSILON;
		} else {
			throw new IllegalStateException("Unsupported model type.");
		}
	}

	private BoundaryCondition getLowerBoundaryCondition(final double time, final double x) {
		if (model instanceof FiniteDifferenceEquityModel) {
			return ((FiniteDifferenceEquityModel) model)
					.getBoundaryConditionsAtLowerBoundary((FiniteDifferenceEquityProduct) product, time, x)[0];
		} else if (model instanceof FiniteDifferenceInterestRateModel) {
			return ((FiniteDifferenceInterestRateModel) model)
					.getBoundaryConditionsAtLowerBoundary((FiniteDifferenceInterestRateProduct) product, time, x)[0];
		} else {
			throw new IllegalStateException("Unsupported model type.");
		}
	}

	private BoundaryCondition getUpperBoundaryCondition(final double time, final double x) {
		if (model instanceof FiniteDifferenceEquityModel) {
			return ((FiniteDifferenceEquityModel) model)
					.getBoundaryConditionsAtUpperBoundary((FiniteDifferenceEquityProduct) product, time, x)[0];
		} else if (model instanceof FiniteDifferenceInterestRateModel) {
			return ((FiniteDifferenceInterestRateModel) model)
					.getBoundaryConditionsAtUpperBoundary((FiniteDifferenceInterestRateProduct) product, time, x)[0];
		} else {
			throw new IllegalStateException("Unsupported model type.");
		}
	}

	private double[] buildObstacleVector(
			final double[] xGrid,
			final double boundaryTime,
			final DoubleUnaryOperator exerciseValue,
			final BoundaryCondition lowerCondition,
			final BoundaryCondition upperCondition) {

		final double[] obstacle = new double[xGrid.length];
		for (int i = 0; i < xGrid.length; i++) {
			if (i == 0 && lowerCondition.isDirichlet()) {
				obstacle[i] = lowerCondition.getValue();
			} else if (i == xGrid.length - 1 && upperCondition.isDirichlet()) {
				obstacle[i] = upperCondition.getValue();
			} else if (isInternalConstraintActive(boundaryTime, xGrid[i])) {
				obstacle[i] = getInternalConstrainedValue(boundaryTime, xGrid[i]);
			} else {
				obstacle[i] = exerciseValue.applyAsDouble(xGrid[i]);
			}
		}
		return obstacle;
	}

	private double[] buildContinuousObstacleVector(
			final double[] xGrid,
			final double boundaryTime,
			final DoubleBinaryOperator continuousObstacleValue,
			final BoundaryCondition lowerCondition,
			final BoundaryCondition upperCondition) {

		final double[] obstacle = new double[xGrid.length];

		for (int i = 0; i < xGrid.length; i++) {
			if (i == 0 && lowerCondition.isDirichlet()) {
				obstacle[i] = lowerCondition.getValue();
			} else if (i == xGrid.length - 1 && upperCondition.isDirichlet()) {
				obstacle[i] = upperCondition.getValue();
			} else if (isInternalConstraintActive(boundaryTime, xGrid[i])) {
				obstacle[i] = getInternalConstrainedValue(boundaryTime, xGrid[i]);
			} else {
				obstacle[i] = continuousObstacleValue.applyAsDouble(boundaryTime, xGrid[i]);
			}
		}

		return obstacle;
	}

	private void applyExerciseProjection(
			final double[] u,
			final double[] xGrid,
			final double boundaryTime,
			final DoubleUnaryOperator exerciseValue,
			final BoundaryCondition lowerCondition,
			final BoundaryCondition upperCondition) {

		for (int i = 0; i < xGrid.length; i++) {
			if (i == 0 && lowerCondition.isDirichlet()) {
				u[i] = lowerCondition.getValue();
			} else if (i == xGrid.length - 1 && upperCondition.isDirichlet()) {
				u[i] = upperCondition.getValue();
			} else if (isInternalConstraintActive(boundaryTime, xGrid[i])) {
				u[i] = getInternalConstrainedValue(boundaryTime, xGrid[i]);
			} else {
				u[i] = Math.max(u[i], exerciseValue.applyAsDouble(xGrid[i]));
			}
		}
	}

	private void reimposeInternalConstraints(
			final double[] u,
			final double[] xGrid,
			final double boundaryTime) {

		for (int i = 1; i < xGrid.length - 1; i++) {
			if (isInternalConstraintActive(boundaryTime, xGrid[i])) {
				u[i] = getInternalConstrainedValue(boundaryTime, xGrid[i]);
			}
		}
	}

	private void reimposeBoundaryValues(
			final double[] u,
			final BoundaryCondition lowerCondition,
			final BoundaryCondition upperCondition) {

		if (lowerCondition.isDirichlet()) {
			u[0] = lowerCondition.getValue();
		}
		if (upperCondition.isDirichlet()) {
			u[u.length - 1] = upperCondition.getValue();
		}
	}

	private boolean isInternalConstraintActive(final double time, final double x) {
		if (product instanceof FiniteDifferenceInternalStateConstraint) {
			return ((FiniteDifferenceInternalStateConstraint) product).isConstraintActive(time, x);
		}
		return false;
	}

	private double getInternalConstrainedValue(final double time, final double x) {
		return ((FiniteDifferenceInternalStateConstraint) product).getConstrainedValue(time, x);
	}
}
