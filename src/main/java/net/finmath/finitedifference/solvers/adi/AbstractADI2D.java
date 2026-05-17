package net.finmath.finitedifference.solvers.adi;

import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;

import net.finmath.finitedifference.FiniteDifferenceExerciseUtil;
import net.finmath.finitedifference.assetderivativevaluation.models.FiniteDifferenceEquityModel;
import net.finmath.finitedifference.assetderivativevaluation.products.FiniteDifferenceEquityEventProduct;
import net.finmath.finitedifference.assetderivativevaluation.products.FiniteDifferenceEquityProduct;
import net.finmath.finitedifference.assetderivativevaluation.products.FiniteDifferenceInternalStateConstraint;
import net.finmath.finitedifference.boundaries.BoundaryCondition;
import net.finmath.finitedifference.grids.Grid;
import net.finmath.finitedifference.grids.SpaceTimeDiscretization;
import net.finmath.finitedifference.solvers.FDMSolver;
import net.finmath.finitedifference.solvers.ThomasSolver;
import net.finmath.finitedifference.solvers.TridiagonalMatrix;
import net.finmath.modelling.Exercise;

/**
 * Generic two-dimensional alternating direction implicit finite-difference
 * solver.
 *
 * <p>
 * The solver acts on a two-dimensional state vector
 * <i>(x_0,x_1)</i> and computes an approximation of the value function
 * <i>V(t,x_0,x_1)</i> associated with a parabolic pricing equation of the form
 * </p>
 *
 * <p>
 * <i>
 * \frac{\partial V}{\partial t}
 * + \mathcal{A}_0 V
 * + \mathcal{A}_1 V
 * + \mathcal{A}_2 V
 * = 0,
 * </i>
 * </p>
 *
 * <p>
 * where the operator is split into:
 * </p>
 * <ul>
 * <li><i>\mathcal{A}_0</i>: mixed derivative term together with discounting,
 * treated explicitly,</li>
 * <li><i>\mathcal{A}_1</i>: first-direction drift and diffusion terms, treated
 * implicitly,</li>
 * <li><i>\mathcal{A}_2</i>: second-direction drift and diffusion terms, treated
 * implicitly.</li>
 * </ul>
 *
 * <p>
 * The time stepping uses a stabilized Douglas-type ADI splitting. For a time
 * step
 * <i>\Delta t</i>, the algorithm first forms an explicit predictor and then
 * performs
 * successive implicit solves along the two spatial directions. To improve
 * numerical
 * stability, the implementation applies two half Douglas steps. This yields a
 * scheme
 * which is efficient for two-dimensional problems since each implicit stage
 * reduces to
 * a collection of tridiagonal linear systems along grid lines.
 * </p>
 *
 * <p>
 * The flattening convention for a grid function is
 * </p>
 * <pre>
 * k = i0 + i1 * n0
 * </pre>
 * <p>
 * where <i>i0</i> is the index in the first spatial direction and <i>i1</i> is
 * the
 * index in the second spatial direction. Hence the first state direction is
 * stored as
 * the fastest varying index.
 * </p>
 *
 * <p>
 * The solver supports:
 * </p>
 * <ul>
 *   <li>terminal conditions depending on one or two state variables,</li>
 * <li>discrete exercise obstacles for Bermudan or American-style problems,</li>
 *   <li>continuous obstacles applied after each backward step,</li>
 *   <li>internal state constraints supplied by the product,</li>
 *   <li>vector-level equity event conditions at prescribed event times,</li>
 *   <li>Dirichlet boundary extraction from the model boundary conditions.</li>
 * </ul>
 *
 * @author Alessandro Gnoatto
 */
public abstract class AbstractADI2D implements FDMSolver {

	/**
	 * The event time tolerance.
	 */
	private static final double EVENT_TIME_TOLERANCE = 1E-12;

	/**
	 * Functional interface for payoffs or obstacles depending on time and two
	 * state variables.
	 *
	 * <p>
	 * The three arguments are interpreted as running time and the two spatial
	 * coordinates.
	 * This is useful for early-exercise values depending explicitly on time.
	 * </p>
	 */
	@FunctionalInterface
	public interface DoubleTernaryOperator {

		/**
		 * Evaluates the function.
		 *
		 * @param x0 First argument.
		 * @param x1 Second argument.
		 * @param x2 Third argument.
		 * @return Function value.
		 */
		double applyAsDouble(double x0, double x1, double x2);
	}

	/**
	 * The model.
	 */
	private final FiniteDifferenceEquityModel model;
	/**
	 * The product.
	 */
	private final FiniteDifferenceEquityProduct product;
	/**
	 * The space time discretization.
	 */
	private final SpaceTimeDiscretization spaceTimeDiscretization;
	/**
	 * The exercise.
	 */
	private final Exercise exercise;

	/**
	 * The theta.
	 */
	private final double theta;

	/**
	 * The x0 grid.
	 */
	private final double[] x0Grid;
	/**
	 * The x1 grid.
	 */
	private final double[] x1Grid;

	/**
	 * The n0.
	 */
	private final int n0;
	/**
	 * The n1.
	 */
	private final int n1;
	/**
	 * The n.
	 */
	private final int n;

	/**
	 * The stencil builder.
	 */
	private final ADI2DStencilBuilder stencilBuilder;

	/**
	 * Creates the ADI solver.
	 *
	 * @param model The finite-difference model.
	 * @param product The product to be valued.
	 * @param spaceTimeDiscretization The space-time discretization.
	 * @param exercise The exercise specification.
	 */
	protected AbstractADI2D(
			final FiniteDifferenceEquityModel model,
			final FiniteDifferenceEquityProduct product,
			final SpaceTimeDiscretization spaceTimeDiscretization,
			final Exercise exercise) {

		this.model = model;
		this.product = product;
		this.spaceTimeDiscretization = spaceTimeDiscretization;
		this.exercise = exercise;

		final Grid x0GridObj = spaceTimeDiscretization.getSpaceGrid(0);
		final Grid x1GridObj = spaceTimeDiscretization.getSpaceGrid(1);
		if (x0GridObj == null || x1GridObj == null) {
			throw new IllegalArgumentException("AbstractADI2D requires a 2D discretization.");
		}

		this.x0Grid = x0GridObj.getGrid();
		this.x1Grid = x1GridObj.getGrid();

		this.n0 = getX0Grid().length;
		this.n1 = getX1Grid().length;
		this.n = getN0() * getN1();

		this.theta = Math.max(0.5, spaceTimeDiscretization.getTheta());
		this.stencilBuilder = new ADI2DStencilBuilder(model, getX0Grid(), getX1Grid());

		validateProductEventTimesInGrid();
	}

	/**
	 * Returns the full value surface for a terminal payoff depending on the
	 * first state variable.
	 *
	 * @param time Maturity time.
	 * @param valueAtMaturity Terminal payoff.
	 * @return Value surface indexed by flattened space index and time index.
	 */
	@Override
	public double[][] getValues(final double time, final DoubleUnaryOperator valueAtMaturity) {
		return getValues(
				time,
				(x0, x1) -> valueAtMaturity.applyAsDouble(x0),
				(runningTime, x0, x1) -> valueAtMaturity.applyAsDouble(x0));
	}

	/**
	 * Returns the full value surface for a two-dimensional terminal payoff.
	 *
	 * @param time Maturity time.
	 * @param valueAtMaturity Terminal payoff.
	 * @return Value surface indexed by flattened space index and time index.
	 */
	public double[][] getValues(final double time, final DoubleBinaryOperator valueAtMaturity) {
		return getValues(
				time,
				valueAtMaturity,
				(runningTime, x0, x1) -> valueAtMaturity.applyAsDouble(x0, x1));
	}

	/**
	 * Returns the full value surface under a continuous obstacle.
	 *
	 * @param time Maturity time.
	 * @param valueAtMaturity Terminal payoff.
	 * @param continuousObstacleValue Continuous obstacle value.
	 * @return Value surface indexed by flattened space index and time index.
	 */
	public double[][] getValuesWithContinuousObstacle(
			final double time,
			final DoubleBinaryOperator valueAtMaturity,
			final DoubleTernaryOperator continuousObstacleValue) {
		return getValuesInternal(time, valueAtMaturity, null, continuousObstacleValue);
	}

	/**
	 * Returns the values at a given evaluation time under a continuous
	 * obstacle.
	 *
	 * @param evaluationTime Evaluation time.
	 * @param time Maturity time.
	 * @param valueAtMaturity Terminal payoff.
	 * @param continuousObstacleValue Continuous obstacle value.
	 * @return Value vector on the flattened space grid.
	 */
	public double[] getValueWithContinuousObstacle(
			final double evaluationTime,
			final double time,
			final DoubleBinaryOperator valueAtMaturity,
			final DoubleTernaryOperator continuousObstacleValue) {

		final RealMatrix values = new Array2DRowRealMatrix(
				getValuesWithContinuousObstacle(time, valueAtMaturity, continuousObstacleValue)
		);
		final double tau = time - evaluationTime;
		final int timeIndex = getSpaceTimeDiscretization().getTimeDiscretization().getTimeIndexNearestLessOrEqual(tau);
		return values.getColumn(timeIndex);
	}

	/**
	 * Returns the full value surface under a discrete exercise obstacle.
	 *
	 * @param time Maturity time.
	 * @param valueAtMaturity Terminal payoff.
	 * @param exerciseValue Exercise payoff.
	 * @return Value surface indexed by flattened space index and time index.
	 */
	public double[][] getValues(
			final double time,
			final DoubleBinaryOperator valueAtMaturity,
			final DoubleTernaryOperator exerciseValue) {
		return getValuesInternal(time, valueAtMaturity, exerciseValue, null);
	}

	private double[][] getValuesInternal(
			final double time,
			final DoubleBinaryOperator valueAtMaturity,
			final DoubleTernaryOperator exerciseValue,
			final DoubleTernaryOperator continuousObstacleValue) {

		if (exerciseValue != null && continuousObstacleValue != null) {
			throw new IllegalArgumentException(
					"Provide either a discrete exercise obstacle or a continuous obstacle, not both.");
		}

		final int timeLength = getSpaceTimeDiscretization().getTimeDiscretization().getNumberOfTimeSteps() + 1;
		final int numberOfTimeSteps = getSpaceTimeDiscretization().getTimeDiscretization().getNumberOfTimeSteps();

		double[] u = new double[getN()];
		for (int j = 0; j < getN1(); j++) {
			for (int i = 0; i < getN0(); i++) {
				u[flatten(i, j)] = valueAtMaturity.applyAsDouble(getX0Grid()[i], getX1Grid()[j]);
			}
		}

		applyOuterBoundaries(time, u);
		applyInternalConstraints(time, u);
		u = applyProductEventConditionIfNeeded(time, u);
		applyInternalConstraints(time, u);
		applyOuterBoundaries(time, u);
		u = sanitize(u);

		final RealMatrix solutionSurface = new Array2DRowRealMatrix(getN(), timeLength);
		solutionSurface.setColumn(0, u.clone());

		for (int m = 0; m < numberOfTimeSteps; m++) {
			final double dt = getSpaceTimeDiscretization().getTimeDiscretization().getTimeStep(m);

			final double tauNext = getSpaceTimeDiscretization().getTimeDiscretization().getTime(m + 1);
			final double runningTimeNext =
					getSpaceTimeDiscretization().getTimeDiscretization().getLastTime() - tauNext;

			u = performStableDouglasStep(u, runningTimeNext, dt);

			applyInternalConstraints(runningTimeNext, u);
			applyOuterBoundaries(runningTimeNext, u);

			if (continuousObstacleValue != null) {
				applyContinuousObstacleIfNeeded(runningTimeNext, u, continuousObstacleValue);
			} else {
				applyExerciseObstacleIfNeeded(runningTimeNext, tauNext, u, exerciseValue);
			}

			u = applyProductEventConditionIfNeeded(runningTimeNext, u);

			applyInternalConstraints(runningTimeNext, u);
			applyOuterBoundaries(runningTimeNext, u);

			u = sanitize(u);

			solutionSurface.setColumn(m + 1, u.clone());
		}

		return solutionSurface.getData();
	}

	/**
	 * Returns the values at the specified evaluation time for a one-dimensional
	 * terminal payoff.
	 *
	 * @param evaluationTime Evaluation time.
	 * @param time Maturity time.
	 * @param valueAtMaturity Terminal payoff.
	 * @return Value vector on the flattened space grid.
	 */
	@Override
	public double[] getValue(
			final double evaluationTime,
			final double time,
			final DoubleUnaryOperator valueAtMaturity) {

		final RealMatrix values = new Array2DRowRealMatrix(getValues(time, valueAtMaturity));
		final double tau = time - evaluationTime;
		final int timeIndex = getSpaceTimeDiscretization().getTimeDiscretization().getTimeIndexNearestLessOrEqual(tau);
		return values.getColumn(timeIndex);
	}

	/**
	 * Returns the values at the specified evaluation time for a two-dimensional
	 * terminal payoff.
	 *
	 * @param evaluationTime Evaluation time.
	 * @param time Maturity time.
	 * @param valueAtMaturity Terminal payoff.
	 * @return Value vector on the flattened space grid.
	 */
	public double[] getValue(
			final double evaluationTime,
			final double time,
			final DoubleBinaryOperator valueAtMaturity) {

		final RealMatrix values = new Array2DRowRealMatrix(getValues(time, valueAtMaturity));
		final double tau = time - evaluationTime;
		final int timeIndex = getSpaceTimeDiscretization().getTimeDiscretization().getTimeIndexNearestLessOrEqual(tau);
		return values.getColumn(timeIndex);
	}

	/**
	 * Returns the values at the specified evaluation time under a discrete
	 * exercise obstacle.
	 *
	 * @param evaluationTime Evaluation time.
	 * @param time Maturity time.
	 * @param valueAtMaturity Terminal payoff.
	 * @param exerciseValue Exercise payoff.
	 * @return Value vector on the flattened space grid.
	 */
	public double[] getValue(
			final double evaluationTime,
			final double time,
			final DoubleBinaryOperator valueAtMaturity,
			final DoubleTernaryOperator exerciseValue) {

		final RealMatrix values = new Array2DRowRealMatrix(getValues(time, valueAtMaturity, exerciseValue));
		final double tau = time - evaluationTime;
		final int timeIndex = getSpaceTimeDiscretization().getTimeDiscretization().getTimeIndexNearestLessOrEqual(tau);
		return values.getColumn(timeIndex);
	}

	/**
	 * Performs one stabilized Douglas time step by splitting it into two half
	 * steps.
	 *
	 * @param u Current solution vector.
	 * @param currentTime Current running time.
	 * @param dt Time-step size.
	 * @return Updated solution vector.
	 */
	protected double[] performStableDouglasStep(
			final double[] u,
			final double currentTime,
			final double dt) {

		final double halfDt = 0.5 * dt;

		double[] uMid = performDouglasHalfStep(u, currentTime + halfDt, halfDt);
		uMid = sanitize(uMid);

		double[] uNext = performDouglasHalfStep(uMid, currentTime, halfDt);
		uNext = sanitize(uNext);

		return uNext;
	}

	/**
	 * Performs one half Douglas step.
	 *
	 * @param u Current solution vector.
	 * @param currentTime Current running time.
	 * @param dt Time-step size.
	 * @return Updated solution vector.
	 */
	protected double[] performDouglasHalfStep(
			final double[] u,
			final double currentTime,
			final double dt) {

		final double[] explicit = applyFullExplicitOperator(u, currentTime);
		final double[] y0 = add(u, scale(explicit, dt));

		applyOuterBoundaries(currentTime, y0);

		final double[] a1u = applyA1Explicit(u, currentTime);
		final double[] rhs1 = subtract(y0, scale(a1u, getTheta() * dt));
		double[] y1 = solveFirstDirectionLines(rhs1, currentTime, dt);
		y1 = sanitize(y1);

		applyOuterBoundaries(currentTime, y1);

		final double[] a2u = applyA2Explicit(u, currentTime);
		final double[] rhs2 = subtract(y1, scale(a2u, getTheta() * dt));
		double[] y2 = solveSecondDirectionLines(rhs2, currentTime, dt);
		y2 = sanitize(y2);

		applyInternalConstraints(currentTime, y2);
		applyOuterBoundaries(currentTime, y2);

		return y2;
	}

	protected double[] applyFullExplicitOperator(final double[] u, final double time) {
		return add(add(applyA0Explicit(u, time), applyA1Explicit(u, time)), applyA2Explicit(u, time));
	}

	protected double[] applyA0Explicit(final double[] u, final double time) {

		final double[] out = new double[getN()];

		final double tSafe = Math.max(time, 1E-10);
		final double discountFactor = getModel().getRiskFreeCurve().getDiscountFactor(tSafe);
		final double r = -Math.log(discountFactor) / tSafe;

		for (int j = 1; j < getN1() - 1; j++) {
			for (int i = 1; i < getN0() - 1; i++) {
				final int k = flatten(i, j);

				final double x0 = getX0Grid()[i];
				final double x1 = getX1Grid()[j];

				final double[][] b = getModel().getFactorLoading(time, x0, x1);

				double a01 = 0.0;
				for (int f = 0; f < b[0].length; f++) {
					a01 += b[0][f] * b[1][f];
				}

				final double dx0Down = getX0Grid()[i] - getX0Grid()[i - 1];
				final double dx0Up = getX0Grid()[i + 1] - getX0Grid()[i];
				final double dx1Down = getX1Grid()[j] - getX1Grid()[j - 1];
				final double dx1Up = getX1Grid()[j + 1] - getX1Grid()[j];

				final double d0d1 =
						(
								u[flatten(i + 1, j + 1)]
								- u[flatten(i + 1, j - 1)]
								- u[flatten(i - 1, j + 1)]
								+ u[flatten(i - 1, j - 1)]
						)
						/ ((dx0Down + dx0Up) * (dx1Down + dx1Up));

				out[k] = a01 * d0d1 - r * u[k];
			}
		}

		return out;
	}

	protected double[] applyA1Explicit(final double[] u, final double time) {

		final double[] out = new double[getN()];

		for (int j = 0; j < getN1(); j++) {
			for (int i = 1; i < getN0() - 1; i++) {
				final int k = flatten(i, j);

				final double x0 = getX0Grid()[i];
				final double x1 = getX1Grid()[j];

				final double[] drift = getModel().getDrift(time, x0, x1);
				final double[][] b = getModel().getFactorLoading(time, x0, x1);

				final double mu0 = drift[0];

				double a00 = 0.0;
				for (int f = 0; f < b[0].length; f++) {
					a00 += b[0][f] * b[0][f];
				}

				final double dxDown = getX0Grid()[i] - getX0Grid()[i - 1];
				final double dxUp = getX0Grid()[i + 1] - getX0Grid()[i];

				final double d1 =
						(u[flatten(i + 1, j)] - u[flatten(i - 1, j)])
						/ (dxDown + dxUp);

				final double d2 =
						2.0 * (
								(u[flatten(i + 1, j)] - u[k]) / dxUp
								- (u[k] - u[flatten(i - 1, j)]) / dxDown
						)
						/ (dxDown + dxUp);

				out[k] = mu0 * d1 + 0.5 * a00 * d2;
			}
		}

		return out;
	}

	protected double[] applyA2Explicit(final double[] u, final double time) {

		final double[] out = new double[getN()];

		for (int j = 1; j < getN1() - 1; j++) {
			for (int i = 0; i < getN0(); i++) {
				final int k = flatten(i, j);

				final double x0 = getX0Grid()[i];
				final double x1 = getX1Grid()[j];

				final double[] drift = getModel().getDrift(time, x0, x1);
				final double[][] b = getModel().getFactorLoading(time, x0, x1);

				final double mu1 = drift[1];

				double a11 = 0.0;
				for (int f = 0; f < b[1].length; f++) {
					a11 += b[1][f] * b[1][f];
				}

				final double dxDown = getX1Grid()[j] - getX1Grid()[j - 1];
				final double dxUp = getX1Grid()[j + 1] - getX1Grid()[j];

				final double d1 =
						(u[flatten(i, j + 1)] - u[flatten(i, j - 1)])
						/ (dxDown + dxUp);

				final double d2 =
						2.0 * (
								(u[flatten(i, j + 1)] - u[k]) / dxUp
								- (u[k] - u[flatten(i, j - 1)]) / dxDown
						)
						/ (dxDown + dxUp);

				out[k] = mu1 * d1 + 0.5 * a11 * d2;
			}
		}

		return out;
	}

	protected double[] solveFirstDirectionLines(
			final double[] rhs,
			final double time,
			final double dt) {

		final double[] out = rhs.clone();

		for (int j = 0; j < getN1(); j++) {
			final TridiagonalMatrix m = getStencilBuilder().buildFirstDirectionLineMatrix(time, dt, getTheta(), j);

			final double[] lineRhs = new double[getN0()];
			for (int i = 0; i < getN0(); i++) {
				lineRhs[i] = rhs[flatten(i, j)];
			}

			final double lowerBoundaryValue = getLowerBoundaryValueForFirstDirection(time, j, lineRhs[0]);
			final double upperBoundaryValue = getUpperBoundaryValueForFirstDirection(time, j, lineRhs[getN0() - 1]);

			overwriteBoundaryRow(m, lineRhs, 0, lowerBoundaryValue);
			overwriteBoundaryRow(m, lineRhs, getN0() - 1, upperBoundaryValue);

			final double[] solved = ThomasSolver.solve(m.getLowerDiagonal(), m.getMainDiagonal(), m.getUpperDiagonal(), lineRhs);

			for (int i = 0; i < getN0(); i++) {
				out[flatten(i, j)] = solved[i];
			}
		}

		return out;
	}

	protected double[] solveSecondDirectionLines(
			final double[] rhs,
			final double time,
			final double dt) {

		final double[] out = rhs.clone();

		for (int i = 0; i < getN0(); i++) {
			final TridiagonalMatrix m = getStencilBuilder().buildSecondDirectionLineMatrix(time, dt, getTheta(), i);

			final double[] lineRhs = new double[getN1()];
			for (int j = 0; j < getN1(); j++) {
				lineRhs[j] = rhs[flatten(i, j)];
			}

			final double lowerBoundaryValue = getLowerBoundaryValueForSecondDirection(time, i, lineRhs[0]);
			final double upperBoundaryValue = getUpperBoundaryValueForSecondDirection(time, i, lineRhs[getN1() - 1]);

			overwriteBoundaryRow(m, lineRhs, 0, lowerBoundaryValue);
			overwriteBoundaryRow(m, lineRhs, getN1() - 1, upperBoundaryValue);

			final double[] solved = ThomasSolver.solve(m.getLowerDiagonal(), m.getMainDiagonal(), m.getUpperDiagonal(), lineRhs);

			for (int j = 0; j < getN1(); j++) {
				out[flatten(i, j)] = solved[j];
			}
		}

		return out;
	}

	protected void applyOuterBoundaries(final double time, final double[] u) {

		for (int j = 0; j < getN1(); j++) {
			u[flatten(0, j)] = getLowerBoundaryValueForFirstDirection(time, j, u[flatten(0, j)]);
			u[flatten(getN0() - 1, j)] = getUpperBoundaryValueForFirstDirection(time, j, u[flatten(getN0() - 1, j)]);
		}

		for (int i = 0; i < getN0(); i++) {
			u[flatten(i, 0)] = getLowerBoundaryValueForSecondDirection(time, i, u[flatten(i, 0)]);
			u[flatten(i, getN1() - 1)] = getUpperBoundaryValueForSecondDirection(time, i, u[flatten(i, getN1() - 1)]);
		}
	}

	protected void applyInternalConstraints(final double time, final double[] u) {
		if (!(getProduct() instanceof FiniteDifferenceInternalStateConstraint)) {
			return;
		}

		final FiniteDifferenceInternalStateConstraint constraint =
				(FiniteDifferenceInternalStateConstraint) getProduct();

		for (int j = 0; j < getN1(); j++) {
			for (int i = 0; i < getN0(); i++) {
				final int k = flatten(i, j);
				if (constraint.isConstraintActive(time, getX0Grid()[i], getX1Grid()[j])) {
					u[k] = constraint.getConstrainedValue(time, getX0Grid()[i], getX1Grid()[j]);
				}
			}
		}
	}

	protected void applyExerciseObstacleIfNeeded(
			final double runningTime,
			final double tau,
			final double[] u,
			final DoubleTernaryOperator exerciseValue) {

		final boolean isExerciseAllowed =
				FiniteDifferenceExerciseUtil.isExerciseAllowedAtTimeToMaturity(tau, getExercise());

		if (!isExerciseAllowed || exerciseValue == null) {
			return;
		}

		for (int j = 0; j < getN1(); j++) {
			for (int i = 0; i < getN0(); i++) {
				if (isInternalConstraintActive(runningTime, getX0Grid()[i], getX1Grid()[j])) {
					continue;
				}

				final int k = flatten(i, j);
				final double payoff = exerciseValue.applyAsDouble(runningTime, getX0Grid()[i], getX1Grid()[j]);
				u[k] = Math.max(u[k], payoff);
			}
		}
	}

	protected void applyContinuousObstacleIfNeeded(
			final double runningTime,
			final double[] u,
			final DoubleTernaryOperator continuousObstacleValue) {

		if (continuousObstacleValue == null) {
			return;
		}

		for (int j = 1; j < getN1() - 1; j++) {
			for (int i = 1; i < getN0() - 1; i++) {
				if (isInternalConstraintActive(runningTime, getX0Grid()[i], getX1Grid()[j])) {
					continue;
				}

				final int k = flatten(i, j);
				final double obstacle = continuousObstacleValue.applyAsDouble(
						runningTime,
						getX0Grid()[i],
						getX1Grid()[j]
				);

				u[k] = Math.max(u[k], obstacle);
			}
		}
	}

	protected boolean isInternalConstraintActive(final double time, final double x0, final double x1) {
		if (getProduct() instanceof FiniteDifferenceInternalStateConstraint) {
			return ((FiniteDifferenceInternalStateConstraint) getProduct()).isConstraintActive(time, x0, x1);
		}
		return false;
	}

	protected double getLowerBoundaryValueForFirstDirection(final double time, final int secondIndex, final double fallback) {
		final BoundaryCondition[] conditions =
				getModel().getBoundaryConditionsAtLowerBoundary(getProduct(), time, getX0Grid()[0], getX1Grid()[secondIndex]);
		return extractBoundaryValue(conditions[0], fallback);
	}

	protected double getUpperBoundaryValueForFirstDirection(final double time, final int secondIndex, final double fallback) {
		final BoundaryCondition[] conditions =
				getModel().getBoundaryConditionsAtUpperBoundary(getProduct(), time, getX0Grid()[getN0() - 1], getX1Grid()[secondIndex]);
		return extractBoundaryValue(conditions[0], fallback);
	}

	protected double getLowerBoundaryValueForSecondDirection(final double time, final int firstIndex, final double fallback) {
		final BoundaryCondition[] conditions =
				getModel().getBoundaryConditionsAtLowerBoundary(getProduct(), time, getX0Grid()[firstIndex], getX1Grid()[0]);
		return extractBoundaryValue(conditions[1], fallback);
	}

	protected double getUpperBoundaryValueForSecondDirection(final double time, final int firstIndex, final double fallback) {
		final BoundaryCondition[] conditions =
				getModel().getBoundaryConditionsAtUpperBoundary(getProduct(), time, getX0Grid()[firstIndex], getX1Grid()[getN1() - 1]);
		return extractBoundaryValue(conditions[1], fallback);
	}

	protected double extractBoundaryValue(final BoundaryCondition condition, final double fallback) {
		if (condition != null && condition.isDirichlet()) {
			return condition.getValue();
		}
		return fallback;
	}

	protected void overwriteBoundaryRow(
			final TridiagonalMatrix m,
			final double[] rhs,
			final int row,
			final double value) {

		m.getLowerDiagonal()[row] = 0.0;
		m.getMainDiagonal()[row] = 1.0;
		m.getUpperDiagonal()[row] = 0.0;
		rhs[row] = value;
	}

	protected int flatten(final int i0, final int i1) {
		return i0 + i1 * getN0();
	}

	protected double[] sanitize(final double[] values) {
		final double[] out = values.clone();
		for (int i = 0; i < out.length; i++) {
			if (!Double.isFinite(out[i])) {
				throw new ArithmeticException("ADI solver produced a non-finite value.");
			}
		}
		return out;
	}

	protected double[] add(final double[] a, final double[] b) {
		if (a.length != b.length) {
			throw new IllegalArgumentException("Vector length mismatch.");
		}

		final double[] out = new double[a.length];
		for (int i = 0; i < a.length; i++) {
			out[i] = a[i] + b[i];
		}
		return out;
	}

	protected double[] subtract(final double[] a, final double[] b) {
		if (a.length != b.length) {
			throw new IllegalArgumentException("Vector length mismatch.");
		}

		final double[] out = new double[a.length];
		for (int i = 0; i < a.length; i++) {
			out[i] = a[i] - b[i];
		}
		return out;
	}

	protected double[] scale(final double[] a, final double scalar) {
		final double[] out = new double[a.length];
		for (int i = 0; i < a.length; i++) {
			out[i] = scalar * a[i];
		}
		return out;
	}

	private void validateProductEventTimesInGrid() {

		if (!(getProduct() instanceof FiniteDifferenceEquityEventProduct)) {
			return;
		}

		final double[] eventTimes = ((FiniteDifferenceEquityEventProduct) getProduct()).getEventTimes();
		final double horizon = getSpaceTimeDiscretization().getTimeDiscretization().getLastTime();

		for (final double eventTime : eventTimes) {
			if (eventTime < -EVENT_TIME_TOLERANCE || eventTime > horizon + EVENT_TIME_TOLERANCE) {
				throw new IllegalArgumentException(
						"Event time " + eventTime
						+ " lies outside the solver time horizon [0," + horizon + "].");
			}

			final double tau = horizon - eventTime;
			final int timeIndex = getSpaceTimeDiscretization().getTimeDiscretization().getTimeIndex(tau);

			if (timeIndex < 0) {
				throw new IllegalArgumentException(
						"Event time " + eventTime
						+ " is not contained in the solver time discretization. "
						+ "Please refine the time grid so that all event times are grid points.");
			}
		}
	}

	private boolean isProductEventTime(final double time) {

		if (!(getProduct() instanceof FiniteDifferenceEquityEventProduct)) {
			return false;
		}

		final double[] eventTimes = ((FiniteDifferenceEquityEventProduct) getProduct()).getEventTimes();

		for (final double eventTime : eventTimes) {
			if (Math.abs(eventTime - time) <= EVENT_TIME_TOLERANCE) {
				return true;
			}
		}

		return false;
	}

	protected double[] applyProductEventConditionIfNeeded(
			final double time,
			final double[] valuesAfterEvent) {

		if (!isProductEventTime(time)) {
			return valuesAfterEvent;
		}

		return ((FiniteDifferenceEquityEventProduct) getProduct()).applyEventCondition(
				time,
				valuesAfterEvent,
				getModel()
		);
	}

	protected ADI2DStencilBuilder getStencilBuilder() {
		return stencilBuilder;
	}

	protected int getN() {
		return n;
	}

	protected int getN1() {
		return n1;
	}

	protected int getN0() {
		return n0;
	}

	protected double[] getX1Grid() {
		return x1Grid;
	}

	protected double[] getX0Grid() {
		return x0Grid;
	}

	protected double getTheta() {
		return theta;
	}

	protected Exercise getExercise() {
		return exercise;
	}

	protected SpaceTimeDiscretization getSpaceTimeDiscretization() {
		return spaceTimeDiscretization;
	}

	protected FiniteDifferenceEquityProduct getProduct() {
		return product;
	}

	protected FiniteDifferenceEquityModel getModel() {
		return model;
	}
}
