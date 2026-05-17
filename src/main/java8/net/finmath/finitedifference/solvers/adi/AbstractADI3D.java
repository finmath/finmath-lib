package net.finmath.finitedifference.solvers.adi;

import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;

import net.finmath.finitedifference.FiniteDifferenceExerciseUtil;
import net.finmath.finitedifference.assetderivativevaluation.models.FiniteDifferenceEquityModel;
import net.finmath.finitedifference.assetderivativevaluation.products.FiniteDifferenceEquityProduct;
import net.finmath.finitedifference.assetderivativevaluation.products.FiniteDifferenceInternalStateConstraint;
import net.finmath.finitedifference.boundaries.BoundaryCondition;
import net.finmath.finitedifference.grids.Grid;
import net.finmath.finitedifference.grids.SpaceTimeDiscretization;
import net.finmath.finitedifference.solvers.FDMSolver;
import net.finmath.finitedifference.solvers.TridiagonalMatrix;
import net.finmath.modelling.Exercise;

/**
 * Generic three-dimensional alternating direction implicit finite difference
 * solver.
 *
 * <p>
 * The solver works on three state variables and applies a stabilized Douglas-
 * type
 * ADI splitting. The operator is split into:
 * </p>
 * <ul>
 * <li>{@code A0}: mixed derivative terms plus discount term, treated
 * explicitly,</li>
 * <li>{@code A1}: first-direction drift and diffusion terms, treated
 * implicitly,</li>
 * <li>{@code A2}: second-direction drift and diffusion terms, treated
 * implicitly,</li>
 * <li>{@code A3}: third-direction drift and diffusion terms, treated
 * implicitly.</li>
 * </ul>
 *
 * <p>
 * The flattening convention is
 * </p>
 * <pre>
 * k = i0 + n0 * (i1 + n1 * i2)
 * </pre>
 * <p>
 * where {@code i0} is the index in the first spatial direction.
 * </p>
 *
 * <p>
 * This class provides the generic explicit operator application, boundary
 * handling,
 * obstacle projection and time stepping. The directional implicit line solves
 * are
 * left abstract, so subclasses may use a dedicated 3D stencil builder or a
 * custom
 * transport discretization.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public abstract class AbstractADI3D implements FDMSolver {

	/**
	 * This interface is needed for payoff depending explicitly on the time
	 * dimension
	 * and three state variables.
	 */
	@FunctionalInterface
	public interface DoubleQuaternaryOperator {
		/**
		 * Performs the operation.
		 *
		 * @param x0 The value.
		 * @param x1 The value.
		 * @param x2 The value.
		 * @param x3 The value.
		 * @return The value.
		 */
		double applyAsDouble(double x0, double x1, double x2, double x3);
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
	 * The x2 grid.
	 */
	private final double[] x2Grid;

	/**
	 * The n0.
	 */
	private final int n0;
	/**
	 * The n1.
	 */
	private final int n1;
	/**
	 * The n2.
	 */
	private final int n2;
	/**
	 * The n.
	 */
	private final int n;

	protected AbstractADI3D(
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
		final Grid x2GridObj = spaceTimeDiscretization.getSpaceGrid(2);

		if (x0GridObj == null || x1GridObj == null || x2GridObj == null) {
			throw new IllegalArgumentException("AbstractADI3D requires a 3D discretization.");
		}

		this.x0Grid = x0GridObj.getGrid();
		this.x1Grid = x1GridObj.getGrid();
		this.x2Grid = x2GridObj.getGrid();

		this.n0 = getX0Grid().length;
		this.n1 = getX1Grid().length;
		this.n2 = getX2Grid().length;
		this.n = getN0() * getN1() * getN2();

		this.theta = Math.max(0.5, spaceTimeDiscretization.getTheta());
	}

	@Override
	public double[][] getValues(final double time, final DoubleUnaryOperator valueAtMaturity) {
		return getValues(
				time,
				(x0, x1, x2) -> valueAtMaturity.applyAsDouble(x0),
				(runningTime, x0, x1, x2) -> valueAtMaturity.applyAsDouble(x0));
	}

	/**
	 * Returns the value.
	 *
	 * @param time The value.
	 * @param valueAtMaturity The value.
	 * @return The value.
	 */
	public double[][] getValues(final double time, final DoubleBinaryOperator valueAtMaturity) {
		return getValues(
				time,
				(x0, x1, x2) -> valueAtMaturity.applyAsDouble(x0, x1),
				(runningTime, x0, x1, x2) -> valueAtMaturity.applyAsDouble(x0, x1));
	}

	/**
	 * Returns the value.
	 *
	 * @param time The value.
	 * @param valueAtMaturity The value.
	 * @return The value.
	 */
	public double[][] getValues(
			final double time,
			final DoubleTernaryOperator valueAtMaturity) {
		return getValues(
				time,
				valueAtMaturity,
				(runningTime, x0, x1, x2) -> valueAtMaturity.applyAsDouble(x0, x1, x2));
	}

	/**
	 * Returns the value.
	 *
	 * @param time The value.
	 * @param valueAtMaturity The value.
	 * @param exerciseValue The value.
	 * @return The value.
	 */
	public double[][] getValues(
			final double time,
			final DoubleTernaryOperator valueAtMaturity,
			final DoubleQuaternaryOperator exerciseValue) {

		final int timeLength = getSpaceTimeDiscretization().getTimeDiscretization().getNumberOfTimeSteps() + 1;
		final int numberOfTimeSteps = getSpaceTimeDiscretization().getTimeDiscretization().getNumberOfTimeSteps();

		double[] u = new double[getN()];
		for (int k2 = 0; k2 < getN2(); k2++) {
			for (int k1 = 0; k1 < getN1(); k1++) {
				for (int k0 = 0; k0 < getN0(); k0++) {
					u[flatten(k0, k1, k2)] =
							valueAtMaturity.applyAsDouble(getX0Grid()[k0], getX1Grid()[k1], getX2Grid()[k2]);
				}
			}
		}

		applyOuterBoundaries(time, u);
		applyInternalConstraints(time, u);
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

			applyExerciseObstacleIfNeeded(runningTimeNext, tauNext, u, exerciseValue);

			applyInternalConstraints(runningTimeNext, u);
			applyOuterBoundaries(runningTimeNext, u);

			u = sanitize(u);

			solutionSurface.setColumn(m + 1, u.clone());
		}

		return solutionSurface.getData();
	}

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
	 * Returns the value.
	 *
	 * @param evaluationTime The value.
	 * @param time The value.
	 * @param valueAtMaturity The value.
	 * @return The value.
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
	 * Returns the value.
	 *
	 * @param evaluationTime The value.
	 * @param time The value.
	 * @param valueAtMaturity The value.
	 * @return The value.
	 */
	public double[] getValue(
			final double evaluationTime,
			final double time,
			final DoubleTernaryOperator valueAtMaturity) {

		final RealMatrix values = new Array2DRowRealMatrix(getValues(time, valueAtMaturity));
		final double tau = time - evaluationTime;
		final int timeIndex = getSpaceTimeDiscretization().getTimeDiscretization().getTimeIndexNearestLessOrEqual(tau);
		return values.getColumn(timeIndex);
	}

	/**
	 * Returns the value.
	 *
	 * @param evaluationTime The value.
	 * @param time The value.
	 * @param valueAtMaturity The value.
	 * @param exerciseValue The value.
	 * @return The value.
	 */
	public double[] getValue(
			final double evaluationTime,
			final double time,
			final DoubleTernaryOperator valueAtMaturity,
			final DoubleQuaternaryOperator exerciseValue) {

		final RealMatrix values = new Array2DRowRealMatrix(getValues(time, valueAtMaturity, exerciseValue));
		final double tau = time - evaluationTime;
		final int timeIndex = getSpaceTimeDiscretization().getTimeDiscretization().getTimeIndexNearestLessOrEqual(tau);
		return values.getColumn(timeIndex);
	}

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

		applyOuterBoundaries(currentTime, y2);

		final double[] a3u = applyA3Explicit(u, currentTime);
		final double[] rhs3 = subtract(y2, scale(a3u, getTheta() * dt));
		double[] y3 = solveThirdDirectionLines(rhs3, currentTime, dt);
		y3 = sanitize(y3);

		applyInternalConstraints(currentTime, y3);
		applyOuterBoundaries(currentTime, y3);

		return y3;
	}

	protected double[] applyFullExplicitOperator(final double[] u, final double time) {
		return add(
				add(applyA0Explicit(u, time), applyA1Explicit(u, time)),
				add(applyA2Explicit(u, time), applyA3Explicit(u, time)));
	}

	/**
	 * Explicit application of A0 containing all mixed derivative terms and the
	 * discount term.
	 * @param u The value.
	 * @param time The value.
	 * @return The value.
	 */
	protected double[] applyA0Explicit(final double[] u, final double time) {

		final double[] out = new double[getN()];

		final double tSafe = Math.max(time, 1E-10);
		final double discountFactor = getModel().getRiskFreeCurve().getDiscountFactor(tSafe);
		final double r = -Math.log(discountFactor) / tSafe;

		for (int k2 = 1; k2 < getN2() - 1; k2++) {
			for (int k1 = 1; k1 < getN1() - 1; k1++) {
				for (int k0 = 1; k0 < getN0() - 1; k0++) {

					final int k = flatten(k0, k1, k2);

					final double x0 = getX0Grid()[k0];
					final double x1 = getX1Grid()[k1];
					final double x2 = getX2Grid()[k2];

					final double[][] b = getModel().getFactorLoading(time, x0, x1, x2);

					double a01 = 0.0;
					double a02 = 0.0;
					double a12 = 0.0;

					for (int f = 0; f < b[0].length; f++) {
						a01 += b[0][f] * b[1][f];
						a02 += b[0][f] * b[2][f];
						a12 += b[1][f] * b[2][f];
					}

					final double dx0Down = getX0Grid()[k0] - getX0Grid()[k0 - 1];
					final double dx0Up = getX0Grid()[k0 + 1] - getX0Grid()[k0];
					final double dx1Down = getX1Grid()[k1] - getX1Grid()[k1 - 1];
					final double dx1Up = getX1Grid()[k1 + 1] - getX1Grid()[k1];
					final double dx2Down = getX2Grid()[k2] - getX2Grid()[k2 - 1];
					final double dx2Up = getX2Grid()[k2 + 1] - getX2Grid()[k2];

					final double d0d1 =
							(
									u[flatten(k0 + 1, k1 + 1, k2)]
									- u[flatten(k0 + 1, k1 - 1, k2)]
									- u[flatten(k0 - 1, k1 + 1, k2)]
									+ u[flatten(k0 - 1, k1 - 1, k2)]
							)
							/ ((dx0Down + dx0Up) * (dx1Down + dx1Up));

					final double d0d2 =
							(
									u[flatten(k0 + 1, k1, k2 + 1)]
									- u[flatten(k0 + 1, k1, k2 - 1)]
									- u[flatten(k0 - 1, k1, k2 + 1)]
									+ u[flatten(k0 - 1, k1, k2 - 1)]
							)
							/ ((dx0Down + dx0Up) * (dx2Down + dx2Up));

					final double d1d2 =
							(
									u[flatten(k0, k1 + 1, k2 + 1)]
									- u[flatten(k0, k1 + 1, k2 - 1)]
									- u[flatten(k0, k1 - 1, k2 + 1)]
									+ u[flatten(k0, k1 - 1, k2 - 1)]
							)
							/ ((dx1Down + dx1Up) * (dx2Down + dx2Up));

					out[k] = a01 * d0d1 + a02 * d0d2 + a12 * d1d2 - r * u[k];
				}
			}
		}

		return out;
	}

	protected double[] applyA1Explicit(final double[] u, final double time) {

		final double[] out = new double[getN()];

		for (int k2 = 0; k2 < getN2(); k2++) {
			for (int k1 = 0; k1 < getN1(); k1++) {
				for (int k0 = 1; k0 < getN0() - 1; k0++) {

					final int k = flatten(k0, k1, k2);

					final double x0 = getX0Grid()[k0];
					final double x1 = getX1Grid()[k1];
					final double x2 = getX2Grid()[k2];

					final double[] drift = getModel().getDrift(time, x0, x1, x2);
					final double[][] b = getModel().getFactorLoading(time, x0, x1, x2);

					final double mu0 = drift[0];

					double a00 = 0.0;
					for (int f = 0; f < b[0].length; f++) {
						a00 += b[0][f] * b[0][f];
					}

					final double dxDown = getX0Grid()[k0] - getX0Grid()[k0 - 1];
					final double dxUp = getX0Grid()[k0 + 1] - getX0Grid()[k0];

					final double d1 =
							(u[flatten(k0 + 1, k1, k2)] - u[flatten(k0 - 1, k1, k2)])
							/ (dxDown + dxUp);

					final double d2 =
							2.0 * (
									(u[flatten(k0 + 1, k1, k2)] - u[k]) / dxUp
									- (u[k] - u[flatten(k0 - 1, k1, k2)]) / dxDown
							)
							/ (dxDown + dxUp);

					out[k] = mu0 * d1 + 0.5 * a00 * d2;
				}
			}
		}

		return out;
	}

	protected double[] applyA2Explicit(final double[] u, final double time) {

		final double[] out = new double[getN()];

		for (int k2 = 0; k2 < getN2(); k2++) {
			for (int k1 = 1; k1 < getN1() - 1; k1++) {
				for (int k0 = 0; k0 < getN0(); k0++) {

					final int k = flatten(k0, k1, k2);

					final double x0 = getX0Grid()[k0];
					final double x1 = getX1Grid()[k1];
					final double x2 = getX2Grid()[k2];

					final double[] drift = getModel().getDrift(time, x0, x1, x2);
					final double[][] b = getModel().getFactorLoading(time, x0, x1, x2);

					final double mu1 = drift[1];

					double a11 = 0.0;
					for (int f = 0; f < b[1].length; f++) {
						a11 += b[1][f] * b[1][f];
					}

					final double dxDown = getX1Grid()[k1] - getX1Grid()[k1 - 1];
					final double dxUp = getX1Grid()[k1 + 1] - getX1Grid()[k1];

					final double d1 =
							(u[flatten(k0, k1 + 1, k2)] - u[flatten(k0, k1 - 1, k2)])
							/ (dxDown + dxUp);

					final double d2 =
							2.0 * (
									(u[flatten(k0, k1 + 1, k2)] - u[k]) / dxUp
									- (u[k] - u[flatten(k0, k1 - 1, k2)]) / dxDown
							)
							/ (dxDown + dxUp);

					out[k] = mu1 * d1 + 0.5 * a11 * d2;
				}
			}
		}

		return out;
	}

	protected double[] applyA3Explicit(final double[] u, final double time) {

		final double[] out = new double[getN()];

		for (int k2 = 1; k2 < getN2() - 1; k2++) {
			for (int k1 = 0; k1 < getN1(); k1++) {
				for (int k0 = 0; k0 < getN0(); k0++) {

					final int k = flatten(k0, k1, k2);

					final double x0 = getX0Grid()[k0];
					final double x1 = getX1Grid()[k1];
					final double x2 = getX2Grid()[k2];

					final double[] drift = getModel().getDrift(time, x0, x1, x2);
					final double[][] b = getModel().getFactorLoading(time, x0, x1, x2);

					final double mu2 = drift[2];

					double a22 = 0.0;
					for (int f = 0; f < b[2].length; f++) {
						a22 += b[2][f] * b[2][f];
					}

					final double dxDown = getX2Grid()[k2] - getX2Grid()[k2 - 1];
					final double dxUp = getX2Grid()[k2 + 1] - getX2Grid()[k2];

					final double d1 =
							(u[flatten(k0, k1, k2 + 1)] - u[flatten(k0, k1, k2 - 1)])
							/ (dxDown + dxUp);

					final double d2 =
							2.0 * (
									(u[flatten(k0, k1, k2 + 1)] - u[k]) / dxUp
									- (u[k] - u[flatten(k0, k1, k2 - 1)]) / dxDown
							)
							/ (dxDown + dxUp);

					out[k] = mu2 * d1 + 0.5 * a22 * d2;
				}
			}
		}

		return out;
	}

	protected abstract double[] solveFirstDirectionLines(
			double[] rhs,
			double time,
			double dt);

	protected abstract double[] solveSecondDirectionLines(
			double[] rhs,
			double time,
			double dt);

	protected abstract double[] solveThirdDirectionLines(
			double[] rhs,
			double time,
			double dt);

	protected void applyOuterBoundaries(final double time, final double[] u) {

		for (int k2 = 0; k2 < getN2(); k2++) {
			for (int k1 = 0; k1 < getN1(); k1++) {
				u[flatten(0, k1, k2)] =
						getLowerBoundaryValueForFirstDirection(time, k1, k2, u[flatten(0, k1, k2)]);
				u[flatten(getN0() - 1, k1, k2)] =
						getUpperBoundaryValueForFirstDirection(time, k1, k2, u[flatten(getN0() - 1, k1, k2)]);
			}
		}

		for (int k2 = 0; k2 < getN2(); k2++) {
			for (int k0 = 0; k0 < getN0(); k0++) {
				u[flatten(k0, 0, k2)] =
						getLowerBoundaryValueForSecondDirection(time, k0, k2, u[flatten(k0, 0, k2)]);
				u[flatten(k0, getN1() - 1, k2)] =
						getUpperBoundaryValueForSecondDirection(time, k0, k2, u[flatten(k0, getN1() - 1, k2)]);
			}
		}

		for (int k1 = 0; k1 < getN1(); k1++) {
			for (int k0 = 0; k0 < getN0(); k0++) {
				u[flatten(k0, k1, 0)] =
						getLowerBoundaryValueForThirdDirection(time, k0, k1, u[flatten(k0, k1, 0)]);
				u[flatten(k0, k1, getN2() - 1)] =
						getUpperBoundaryValueForThirdDirection(time, k0, k1, u[flatten(k0, k1, getN2() - 1)]);
			}
		}
	}

	protected void applyInternalConstraints(final double time, final double[] u) {
		if (!(getProduct() instanceof FiniteDifferenceInternalStateConstraint)) {
			return;
		}

		final FiniteDifferenceInternalStateConstraint constraint =
				(FiniteDifferenceInternalStateConstraint) getProduct();

		for (int k2 = 0; k2 < getN2(); k2++) {
			for (int k1 = 0; k1 < getN1(); k1++) {
				for (int k0 = 0; k0 < getN0(); k0++) {
					final int k = flatten(k0, k1, k2);
					if (constraint.isConstraintActive(time, getX0Grid()[k0], getX1Grid()[k1], getX2Grid()[k2])) {
						u[k] = constraint.getConstrainedValue(time, getX0Grid()[k0], getX1Grid()[k1], getX2Grid()[k2]);
					}
				}
			}
		}
	}

	protected void applyExerciseObstacleIfNeeded(
			final double runningTime,
			final double tau,
			final double[] u,
			final DoubleQuaternaryOperator exerciseValue) {

		final boolean isExerciseAllowed =
				FiniteDifferenceExerciseUtil.isExerciseAllowedAtTimeToMaturity(tau, getExercise());

		if (!isExerciseAllowed) {
			return;
		}

		for (int k2 = 0; k2 < getN2(); k2++) {
			for (int k1 = 0; k1 < getN1(); k1++) {
				for (int k0 = 0; k0 < getN0(); k0++) {
					if (isInternalConstraintActive(runningTime, getX0Grid()[k0], getX1Grid()[k1], getX2Grid()[k2])) {
						continue;
					}

					final int k = flatten(k0, k1, k2);
					final double payoff = exerciseValue.applyAsDouble(
							runningTime,
							getX0Grid()[k0],
							getX1Grid()[k1],
							getX2Grid()[k2]);

					u[k] = Math.max(u[k], payoff);
				}
			}
		}
	}

	protected boolean isInternalConstraintActive(
			final double time,
			final double x0,
			final double x1,
			final double x2) {

		if (getProduct() instanceof FiniteDifferenceInternalStateConstraint) {
			return ((FiniteDifferenceInternalStateConstraint) getProduct())
					.isConstraintActive(time, x0, x1, x2);
		}
		return false;
	}

	protected double getLowerBoundaryValueForFirstDirection(
			final double time,
			final int secondIndex,
			final int thirdIndex,
			final double fallback) {

		final BoundaryCondition[] conditions =
				getModel().getBoundaryConditionsAtLowerBoundary(
						getProduct(),
						time,
						getX0Grid()[0],
						getX1Grid()[secondIndex],
						getX2Grid()[thirdIndex]);

		return extractBoundaryValue(conditions, 0, fallback);
	}

	protected double getUpperBoundaryValueForFirstDirection(
			final double time,
			final int secondIndex,
			final int thirdIndex,
			final double fallback) {

		final BoundaryCondition[] conditions =
				getModel().getBoundaryConditionsAtUpperBoundary(
						getProduct(),
						time,
						getX0Grid()[getN0() - 1],
						getX1Grid()[secondIndex],
						getX2Grid()[thirdIndex]);

		return extractBoundaryValue(conditions, 0, fallback);
	}

	protected double getLowerBoundaryValueForSecondDirection(
			final double time,
			final int firstIndex,
			final int thirdIndex,
			final double fallback) {

		final BoundaryCondition[] conditions =
				getModel().getBoundaryConditionsAtLowerBoundary(
						getProduct(),
						time,
						getX0Grid()[firstIndex],
						getX1Grid()[0],
						getX2Grid()[thirdIndex]);

		return extractBoundaryValue(conditions, 1, fallback);
	}

	protected double getUpperBoundaryValueForSecondDirection(
			final double time,
			final int firstIndex,
			final int thirdIndex,
			final double fallback) {

		final BoundaryCondition[] conditions =
				getModel().getBoundaryConditionsAtUpperBoundary(
						getProduct(),
						time,
						getX0Grid()[firstIndex],
						getX1Grid()[getN1() - 1],
						getX2Grid()[thirdIndex]);

		return extractBoundaryValue(conditions, 1, fallback);
	}

	protected double getLowerBoundaryValueForThirdDirection(
			final double time,
			final int firstIndex,
			final int secondIndex,
			final double fallback) {

		final BoundaryCondition[] conditions =
				getModel().getBoundaryConditionsAtLowerBoundary(
						getProduct(),
						time,
						getX0Grid()[firstIndex],
						getX1Grid()[secondIndex],
						getX2Grid()[0]);

		return extractBoundaryValue(conditions, 2, fallback);
	}

	protected double getUpperBoundaryValueForThirdDirection(
			final double time,
			final int firstIndex,
			final int secondIndex,
			final double fallback) {

		final BoundaryCondition[] conditions =
				getModel().getBoundaryConditionsAtUpperBoundary(
						getProduct(),
						time,
						getX0Grid()[firstIndex],
						getX1Grid()[secondIndex],
						getX2Grid()[getN2() - 1]);

		return extractBoundaryValue(conditions, 2, fallback);
	}

	protected double extractBoundaryValue(
			final BoundaryCondition[] conditions,
			final int index,
			final double fallback) {

		if (conditions != null
				&& conditions.length > index
				&& conditions[index] != null
				&& conditions[index].isDirichlet()) {
			return conditions[index].getValue();
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

	protected int flatten(final int i0, final int i1, final int i2) {
		return FDM3DGridUtil.flatten(i0, i1, i2, getN0(), getN1());
	}

	protected double[] add(final double[] a, final double[] b) {
		final double[] out = new double[a.length];
		for (int i = 0; i < a.length; i++) {
			out[i] = a[i] + b[i];
		}
		return out;
	}

	protected double[] subtract(final double[] a, final double[] b) {
		final double[] out = new double[a.length];
		for (int i = 0; i < a.length; i++) {
			out[i] = a[i] - b[i];
		}
		return out;
	}

	protected double[] scale(final double[] a, final double c) {
		final double[] out = new double[a.length];
		for (int i = 0; i < a.length; i++) {
			out[i] = c * a[i];
		}
		return out;
	}

	protected double[] sanitize(final double[] u) {
		final double[] out = new double[u.length];
		for (int i = 0; i < u.length; i++) {
			final double value = u[i];
			if (!Double.isFinite(value)) {
				out[i] = 0.0;
			} else if (value > 1E12) {
				out[i] = 1E12;
			} else if (value < -1E12) {
				out[i] = -1E12;
			} else {
				out[i] = value;
			}
		}
		return out;
	}

	protected int getN() {
		return n;
	}

	protected int getN2() {
		return n2;
	}

	protected int getN1() {
		return n1;
	}

	protected int getN0() {
		return n0;
	}

	protected double[] getX2Grid() {
		return x2Grid;
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

	/**
	 * Ternary payoff operator used for terminal conditions depending on three
	 * state variables.
	 */
	@FunctionalInterface
	public interface DoubleTernaryOperator {
		/**
		 * Performs the operation.
		 *
		 * @param x0 The value.
		 * @param x1 The value.
		 * @param x2 The value.
		 * @return The value.
		 */
		double applyAsDouble(double x0, double x1, double x2);
	}
}
