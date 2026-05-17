package net.finmath.finitedifference.solvers;

import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.OpenMapRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;

import net.finmath.finitedifference.FiniteDifferenceExerciseUtil;
import net.finmath.finitedifference.assetderivativevaluation.models.FiniteDifferenceEquityModel;
import net.finmath.finitedifference.assetderivativevaluation.products.FiniteDifferenceEquityProduct;
import net.finmath.finitedifference.assetderivativevaluation.products.FiniteDifferenceInternalStateConstraint;
import net.finmath.finitedifference.boundaries.BoundaryCondition;
import net.finmath.finitedifference.grids.Grid;
import net.finmath.finitedifference.grids.SpaceTimeDiscretization;
import net.finmath.modelling.Exercise;

/**
 * Theta-method solver for two-dimensional PDEs in <em>state-variable form</em>.
 *
 * <p>
 * This solver assumes the two state variables {@code (X0, X1)} follow a generic
 * SDE
 * </p>
 *
 * <p>
 * {@code dX_i(t) = mu_i(t, X0, X1) dt + sum_k b_{i,k}(t, X0, X1) dW_k(t)}, i =
 * 0,1.
 * </p>
 *
 * <p>
 * It builds the backward PDE operator using
 * </p>
 *
 * <ul>
 *   <li>Drift: {@code sum_i mu_i * d/dx_i}</li>
 * <li>Diffusion: {@code 0.5 * sum_{i,j} a_{i,j} * d^2/(dx_i dx_j)} with {@code
 * a = b b^T}</li>
 *   <li>Discounting: {@code -r(t) * u}</li>
 * </ul>
 *
 * <p>
 * Boundary conditions are enforced via explicit {@link BoundaryCondition}
 * objects.
 * Dirichlet rows are overwritten only if the corresponding boundary condition
 * is of Dirichlet type.
 * If the boundary condition type is NONE, the PDE row is left intact.
 * </p>
 *
 * <p>
 * In addition, products may define internal state constraints through
 * {@link FiniteDifferenceInternalStateConstraint}. Constrained nodes are
 * imposed
 * as internal Dirichlet rows.
 * </p>
 *
 * <p>
 * For American exercise, the solver formulates each backward step as a linear
 * complementarity
 * problem and solves it with projected SOR. For non-American exercise, a direct
 * linear solve is used.
 * </p>
 *
 * <p>
 * The solver returns the full time history as a flattened matrix of dimension
 * {@code (n0*n1) x (nT+1)}.
 * Flattening convention: {@code k = i0 + i1*n0} where {@code i0} is the fastest
 * index.
 * </p>
 *
 * @author Enrico De Vecchi
 * @author Alessandro Gnoatto
 */
public class FDMThetaMethod2D implements FDMSolver {

	/**
	 * The psor omega.
	 */
	private static final double PSOR_OMEGA = 1.2;
	/**
	 * The psor max iterations.
	 */
	private static final int PSOR_MAX_ITERATIONS = 500;
	/**
	 * The psor tolerance.
	 */
	private static final double PSOR_TOLERANCE = 1E-10;

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
	 * Creates a two-dimensional theta-method finite-difference solver.
	 *
	 * @param model The finite-difference equity model providing drift, factor
	 *     loadings,
	 *        discounting, and boundary conditions.
	 * @param product The product to be valued. It may optionally implement
	 *        {@link FiniteDifferenceInternalStateConstraint}.
	 * @param spaceTimeDiscretization The joint space-time discretization,
	 *     including
	 *        both spatial grids and the theta parameter.
	 * @param exercise The exercise specification controlling whether and when
	 *     exercise is allowed.
	 */
	public FDMThetaMethod2D(
			final FiniteDifferenceEquityModel model,
			final FiniteDifferenceEquityProduct product,
			final SpaceTimeDiscretization spaceTimeDiscretization,
			final Exercise exercise) {
		this.model = model;
		this.product = product;
		this.spaceTimeDiscretization = spaceTimeDiscretization;
		this.exercise = exercise;
	}

	/**
	 * Solves the PDE using a payoff that depends only on the first state
	 * variable.
	 *
	 * <p>
	 * This overload promotes the one-dimensional terminal payoff into a two-
	 * dimensional one by
	 * ignoring the second state variable.
	 * </p>
	 *
	 * @param time The maturity time of the claim.
	 * @param valueAtMaturity The terminal payoff as a function of the first
	 *     state variable.
	 * @return The full flattened space-time solution surface.
	 */
	@Override
	public double[][] getValues(final double time, final DoubleUnaryOperator valueAtMaturity) {
		return getValues(time, (x0, x1) -> valueAtMaturity.applyAsDouble(x0));
	}

	/**
	 * Solves the backward PDE on the full two-dimensional space-time grid.
	 *
	 * <p>
	 * The returned matrix is indexed as {@code
	 * values[flattenedSpaceIndex][timeIndex]}.
	 * The first column corresponds to maturity and subsequent columns
	 * correspond to earlier
	 * times in backward time-stepping order.
	 * </p>
	 *
	 * <p>
	 * At each time step, the method
	 * </p>
	 * <ul>
	 *   <li>builds model coefficients at the two theta evaluation times,</li>
	 *   <li>assembles the left- and right-hand-side theta operators,</li>
	 *   <li>enforces outer boundary conditions and internal constraints,</li>
	 * <li>solves either a linear system or a linear complementarity
	 * problem,</li>
	 * <li>reimposes constraints and Dirichlet boundaries for numerical
	 * safety.</li>
	 * </ul>
	 *
	 * @param time The maturity time of the claim.
	 * @param valueAtMaturity The terminal payoff as a function of both state
	 *     variables.
	 * @return The full flattened space-time solution surface.
	 * @throws IllegalArgumentException If the discretization does not provide
	 *     two spatial grids.
	 */
	public double[][] getValues(final double time, final DoubleBinaryOperator valueAtMaturity) {

		final Grid x0GridObject = spaceTimeDiscretization.getSpaceGrid(0);
		final Grid x1GridObject = spaceTimeDiscretization.getSpaceGrid(1);

		if (x0GridObject == null || x1GridObject == null) {
			throw new IllegalArgumentException(
					"SpaceTimeDiscretization must provide two space grids (dimension 0 and dimension 1).");
		}

		final double[] x0Grid = x0GridObject.getGrid();
		final double[] x1Grid = x1GridObject.getGrid();

		final int n0 = x0Grid.length;
		final int n1 = x1Grid.length;

		final int numberOfTimeSteps = spaceTimeDiscretization.getTimeDiscretization().getNumberOfTimeSteps();
		final int timeLength = numberOfTimeSteps + 1;
		final double theta = spaceTimeDiscretization.getTheta();

		final DifferentialOperators2D operators = buildDifferentialOperators(x0Grid, x1Grid);
		final boolean[] isBoundary = buildBoundaryMask(n0, n1);

		RealMatrix u = buildTerminalValues(x0Grid, x1Grid, valueAtMaturity);
		final RealMatrix z = MatrixUtils.createRealMatrix(n0 * n1, timeLength);
		z.setColumnMatrix(0, u);

		for (int m = 0; m < numberOfTimeSteps; m++) {

			final double deltaTau = spaceTimeDiscretization.getTimeDiscretization().getTimeStep(m);

			final double tm = spaceTimeDiscretization.getTimeDiscretization().getTime(numberOfTimeSteps - m);
			final double tmp1 = spaceTimeDiscretization.getTimeDiscretization().getTime(numberOfTimeSteps - (m + 1));

			final ModelCoefficients2D coefficientsM = buildModelCoefficients(x0Grid, x1Grid, tm);
			final ModelCoefficients2D coefficientsMp1 = buildModelCoefficients(x0Grid, x1Grid, tmp1);

			final RealMatrix lhs = buildThetaLeftHandSide(operators, coefficientsMp1, deltaTau, theta);
			final RealMatrix rhsOperator = buildThetaRightHandSide(operators, coefficientsM, deltaTau, theta);
			final RealMatrix rhs = rhsOperator.multiply(u);

			final double tauMp1 = spaceTimeDiscretization.getTimeDiscretization().getTime(m + 1);
			final double boundaryTime = spaceTimeDiscretization.getTimeDiscretization().getLastTime() - tauMp1;

			applyOuterBoundaryConditions(lhs, rhs, x0Grid, x1Grid, isBoundary, boundaryTime);
			applyInternalConstraints(lhs, rhs, x0Grid, x1Grid, boundaryTime);

			final boolean isExerciseDate =
					FiniteDifferenceExerciseUtil.isExerciseAllowedAtTimeToMaturity(tauMp1, exercise);

			final RealMatrix nextU;
			if (exercise.isAmerican() && isExerciseDate) {
				final RealMatrix obstacle = buildObstacleVector(x0Grid, x1Grid, boundaryTime, valueAtMaturity);
				nextU = solveProjectedSOR(lhs, rhs, obstacle, u, PSOR_OMEGA, PSOR_MAX_ITERATIONS, PSOR_TOLERANCE);

				reimposeInternalConstraints(nextU, x0Grid, x1Grid, boundaryTime);
				reimposeBoundaryValues(nextU, x0Grid, x1Grid, isBoundary, boundaryTime);
			} else {
				final DecompositionSolver solver = new LUDecomposition(lhs).getSolver();
				nextU = solver.solve(rhs);

				if (isExerciseDate) {
					applyExerciseProjection(nextU, x0Grid, x1Grid, boundaryTime, valueAtMaturity);
				} else {
					reimposeInternalConstraints(nextU, x0Grid, x1Grid, boundaryTime);
					reimposeBoundaryValues(nextU, x0Grid, x1Grid, isBoundary, boundaryTime);
				}
			}

			u = nextU;
			z.setColumnMatrix(m + 1, u);
		}

		return z.getData();
	}

	/**
	 * Returns the flattened value vector at a given evaluation time for a
	 * payoff that depends only
	 * on the first state variable.
	 *
	 * @param evaluationTime The time at which the value is requested.
	 * @param time The maturity time of the claim.
	 * @param valueAtMaturity The terminal payoff as a function of the first
	 *     state variable.
	 * @return The flattened value vector at the requested evaluation time.
	 */
	@Override
	public double[] getValue(final double evaluationTime, final double time, final DoubleUnaryOperator valueAtMaturity) {
		final RealMatrix values = new Array2DRowRealMatrix(getValues(time, valueAtMaturity));
		final double tau = time - evaluationTime;
		final int timeIndex = this.spaceTimeDiscretization.getTimeDiscretization().getTimeIndexNearestLessOrEqual(tau);
		return values.getColumn(timeIndex);
	}

	/**
	 * Returns the flattened value vector at a given evaluation time for a
	 * genuinely two-dimensional payoff.
	 *
	 * @param evaluationTime The time at which the value is requested.
	 * @param time The maturity time of the claim.
	 * @param valueAtMaturity The terminal payoff as a function of both state
	 *     variables.
	 * @return The flattened value vector at the requested evaluation time.
	 */
	public double[] getValue(final double evaluationTime, final double time, final DoubleBinaryOperator valueAtMaturity) {
		final RealMatrix values = new Array2DRowRealMatrix(getValues(time, valueAtMaturity));
		final double tau = time - evaluationTime;
		final int timeIndex = this.spaceTimeDiscretization.getTimeDiscretization().getTimeIndexNearestLessOrEqual(tau);
		return values.getColumn(timeIndex);
	}

	/**
	 * Builds the terminal value vector at maturity.
	 *
	 * @param x0Grid The spatial grid in the first dimension.
	 * @param x1Grid The spatial grid in the second dimension.
	 * @param valueAtMaturity The payoff function evaluated at maturity.
	 * @return A column matrix containing the flattened terminal values.
	 */
	private RealMatrix buildTerminalValues(
			final double[] x0Grid,
			final double[] x1Grid,
			final DoubleBinaryOperator valueAtMaturity) {

		final int n0 = x0Grid.length;
		final int n1 = x1Grid.length;
		final int n = n0 * n1;

		final RealMatrix u = MatrixUtils.createRealMatrix(n, 1);

		for (int j = 0; j < n1; j++) {
			for (int i = 0; i < n0; i++) {
				final int k = flatten(i, j, n0);
				u.setEntry(k, 0, valueAtMaturity.applyAsDouble(x0Grid[i], x1Grid[j]));
			}
		}

		return u;
	}

	/**
	 * Builds a mask identifying whether each flattened grid node lies on the
	 * outer spatial boundary.
	 *
	 * @param n0 The number of grid points in the first dimension.
	 * @param n1 The number of grid points in the second dimension.
	 * @return A boolean array whose entries are {@code true} exactly on the
	 *     outer boundary.
	 */
	private boolean[] buildBoundaryMask(final int n0, final int n1) {
		final boolean[] isBoundary = new boolean[n0 * n1];

		for (int j = 0; j < n1; j++) {
			for (int i = 0; i < n0; i++) {
				final int k = flatten(i, j, n0);
				isBoundary[k] = (i == 0 || i == n0 - 1 || j == 0 || j == n1 - 1);
			}
		}

		return isBoundary;
	}

	/**
	 * Builds the discrete first- and second-order differential operators on the
	 * flattened two-dimensional grid.
	 *
	 * <p>
	 * The flattening convention is {@code k = i0 + i1*n0}, where the first
	 * dimension is the fast index.
	 * </p>
	 *
	 * @param x0Grid The spatial grid in the first dimension.
	 * @param x1Grid The spatial grid in the second dimension.
	 * @return The assembled differential operators.
	 */
	private DifferentialOperators2D buildDifferentialOperators(final double[] x0Grid, final double[] x1Grid) {

		final FiniteDifferenceMatrixBuilder builder0 = new FiniteDifferenceMatrixBuilder(x0Grid);
		final RealMatrix t10 = builder0.getFirstDerivativeMatrix();
		final RealMatrix t20 = builder0.getSecondDerivativeMatrix();

		final FiniteDifferenceMatrixBuilder builder1 = new FiniteDifferenceMatrixBuilder(x1Grid);
		final RealMatrix t11 = builder1.getFirstDerivativeMatrix();
		final RealMatrix t21 = builder1.getSecondDerivativeMatrix();

		final RealMatrix d0 = buildBlockDiagonal(t10, x1Grid.length);
		final RealMatrix d00 = buildBlockDiagonal(t20, x1Grid.length);
		final RealMatrix d1 = buildKronWithIdentityLeft(t11, x0Grid.length);
		final RealMatrix d11 = buildKronWithIdentityLeft(t21, x0Grid.length);
		final RealMatrix d01 = buildKron(t11, t10);

		return new DifferentialOperators2D(d0, d1, d00, d11, d01);
	}

	/**
	 * Builds the model coefficient matrices at a given time.
	 *
	 * <p>
	 * The returned object contains diagonal matrices for drift and covariance
	 * entries, together with
	 * the instantaneous short rate implied by the risk-free discount curve.
	 * </p>
	 *
	 * @param x0Grid The spatial grid in the first dimension.
	 * @param x1Grid The spatial grid in the second dimension.
	 * @param time The model time at which coefficients are evaluated.
	 * @return The model coefficients evaluated on the full grid.
	 */
	private ModelCoefficients2D buildModelCoefficients(
			final double[] x0Grid,
			final double[] x1Grid,
			final double time) {

		final int n0 = x0Grid.length;
		final int n1 = x1Grid.length;
		final int n = n0 * n1;

		final double[] mu0 = new double[n];
		final double[] mu1 = new double[n];
		final double[] a00 = new double[n];
		final double[] a11 = new double[n];
		final double[] a01 = new double[n];

		for (int j = 0; j < n1; j++) {
			for (int i = 0; i < n0; i++) {
				final int k = flatten(i, j, n0);

				final double x0 = x0Grid[i];
				final double x1 = x1Grid[j];

				final double[] drift = model.getDrift(time, x0, x1);
				mu0[k] = drift.length > 0 ? drift[0] : 0.0;
				mu1[k] = drift.length > 1 ? drift[1] : 0.0;

				final double[][] factorLoadings = model.getFactorLoading(time, x0, x1);

				double variance00 = 0.0;
				double variance11 = 0.0;
				double covariance01 = 0.0;

				if (factorLoadings.length > 0) {
					final int numberOfFactors = factorLoadings[0].length;
					for (int factor = 0; factor < numberOfFactors; factor++) {
						final double b0 = factorLoadings[0][factor];
						final double b1 = factorLoadings.length > 1 ? factorLoadings[1][factor] : 0.0;

						variance00 += b0 * b0;
						variance11 += b1 * b1;
						covariance01 += b0 * b1;
					}
				}

				a00[k] = variance00;
				a11[k] = variance11;
				a01[k] = covariance01;
			}
		}

		return new ModelCoefficients2D(
				MatrixUtils.createRealDiagonalMatrix(mu0),
				MatrixUtils.createRealDiagonalMatrix(mu1),
				MatrixUtils.createRealDiagonalMatrix(a00),
				MatrixUtils.createRealDiagonalMatrix(a11),
				MatrixUtils.createRealDiagonalMatrix(a01),
				getShortRate(time));
	}

	/**
	 * Builds the theta-method left-hand-side matrix for one backward step.
	 *
	 * @param operators The discrete differential operators.
	 * @param coefficients The model coefficients evaluated at the implicit
	 *     theta time level.
	 * @param deltaTau The backward time step size in time-to-maturity
	 *     coordinates.
	 * @param theta The theta-method parameter.
	 * @return The left-hand-side matrix of the theta step.
	 */
	private RealMatrix buildThetaLeftHandSide(
			final DifferentialOperators2D operators,
			final ModelCoefficients2D coefficients,
			final double deltaTau,
			final double theta) {

		final int n = operators.getD0().getRowDimension();
		final RealMatrix identity = MatrixUtils.createRealIdentityMatrix(n);

		final RealMatrix driftTerm =
				coefficients.getMu0().scalarMultiply(deltaTau).multiply(operators.getD0())
				.add(coefficients.getMu1().scalarMultiply(deltaTau).multiply(operators.getD1()));

		final RealMatrix diffusionTerm =
				coefficients.getA00().multiply(operators.getD00().scalarMultiply(0.5 * deltaTau))
				.add(coefficients.getA11().multiply(operators.getD11().scalarMultiply(0.5 * deltaTau)))
				.add(coefficients.getA01().multiply(operators.getD01().scalarMultiply(deltaTau)));

		final RealMatrix generatorStep =
				identity.scalarMultiply(1.0 + coefficients.getShortRate() * deltaTau)
				.subtract(driftTerm)
				.subtract(diffusionTerm);

		return generatorStep.scalarMultiply(theta).add(identity.scalarMultiply(1.0 - theta));
	}

	/**
	 * Builds the theta-method right-hand-side operator for one backward step.
	 *
	 * @param operators The discrete differential operators.
	 * @param coefficients The model coefficients evaluated at the explicit
	 *     theta time level.
	 * @param deltaTau The backward time step size in time-to-maturity
	 *     coordinates.
	 * @param theta The theta-method parameter.
	 * @return The right-hand-side operator of the theta step.
	 */
	private RealMatrix buildThetaRightHandSide(
			final DifferentialOperators2D operators,
			final ModelCoefficients2D coefficients,
			final double deltaTau,
			final double theta) {

		final int n = operators.getD0().getRowDimension();
		final RealMatrix identity = MatrixUtils.createRealIdentityMatrix(n);

		final RealMatrix driftTerm =
				coefficients.getMu0().scalarMultiply(deltaTau).multiply(operators.getD0())
				.add(coefficients.getMu1().scalarMultiply(deltaTau).multiply(operators.getD1()));

		final RealMatrix diffusionTerm =
				coefficients.getA00().multiply(operators.getD00().scalarMultiply(0.5 * deltaTau))
				.add(coefficients.getA11().multiply(operators.getD11().scalarMultiply(0.5 * deltaTau)))
				.add(coefficients.getA01().multiply(operators.getD01().scalarMultiply(deltaTau)));

		final RealMatrix generatorStep =
				identity.scalarMultiply(1.0 - coefficients.getShortRate() * deltaTau)
				.add(driftTerm)
				.add(diffusionTerm);

		return generatorStep.scalarMultiply(1.0 - theta).add(identity.scalarMultiply(theta));
	}

	/**
	 * Applies outer Dirichlet boundary conditions by overwriting the
	 * corresponding matrix rows.
	 *
	 * @param lhs The left-hand-side matrix to be modified.
	 * @param rhs The right-hand-side vector to be modified.
	 * @param x0Grid The spatial grid in the first dimension.
	 * @param x1Grid The spatial grid in the second dimension.
	 * @param isBoundary The mask indicating which nodes lie on the outer
	 *     boundary.
	 * @param boundaryTime The current model time at which boundary values are
	 *     evaluated.
	 */
	private void applyOuterBoundaryConditions(
			final RealMatrix lhs,
			final RealMatrix rhs,
			final double[] x0Grid,
			final double[] x1Grid,
			final boolean[] isBoundary,
			final double boundaryTime) {

		final int n0 = x0Grid.length;
		final int n1 = x1Grid.length;
		final int n = n0 * n1;

		for (int j = 0; j < n1; j++) {
			for (int i = 0; i < n0; i++) {
				final int k = flatten(i, j, n0);
				if (!isBoundary[k]) {
					continue;
				}

				final BoundaryCondition boundaryCondition = chooseBoundaryCondition(i, j, x0Grid, x1Grid, boundaryTime);
				if (boundaryCondition != null && boundaryCondition.isDirichlet()) {
					overwriteAsDirichlet(lhs, rhs, k, boundaryCondition.getValue(), n);
				}
			}
		}
	}

	/**
	 * Applies internal state constraints by overwriting the corresponding
	 * matrix rows as internal Dirichlet rows.
	 *
	 * @param lhs The left-hand-side matrix to be modified.
	 * @param rhs The right-hand-side vector to be modified.
	 * @param x0Grid The spatial grid in the first dimension.
	 * @param x1Grid The spatial grid in the second dimension.
	 * @param boundaryTime The current model time at which constraint values are
	 *     evaluated.
	 */
	private void applyInternalConstraints(
			final RealMatrix lhs,
			final RealMatrix rhs,
			final double[] x0Grid,
			final double[] x1Grid,
			final double boundaryTime) {

		final int n0 = x0Grid.length;
		final int n1 = x1Grid.length;
		final int n = n0 * n1;

		for (int j = 0; j < n1; j++) {
			for (int i = 0; i < n0; i++) {
				final int k = flatten(i, j, n0);
				final double x0 = x0Grid[i];
				final double x1 = x1Grid[j];

				if (isInternalConstraintActive(boundaryTime, x0, x1)) {
					overwriteAsDirichlet(lhs, rhs, k, getInternalConstrainedValue(boundaryTime, x0, x1), n);
				}
			}
		}
	}

	/**
	 * Builds the obstacle vector used in the projected SOR solve for American
	 * exercise.
	 *
	 * <p>
	 * At each node the obstacle is chosen in the following order:
	 * </p>
	 * <ul>
	 *   <li>Dirichlet boundary value, if active,</li>
	 *   <li>internal constrained value, if active,</li>
	 * <li>otherwise the intrinsic value given by {@code valueAtMaturity}.</li>
	 * </ul>
	 *
	 * @param x0Grid The spatial grid in the first dimension.
	 * @param x1Grid The spatial grid in the second dimension.
	 * @param boundaryTime The current model time.
	 * @param valueAtMaturity The intrinsic exercise value.
	 * @return The obstacle vector as a column matrix.
	 */
	private RealMatrix buildObstacleVector(
			final double[] x0Grid,
			final double[] x1Grid,
			final double boundaryTime,
			final DoubleBinaryOperator valueAtMaturity) {

		final int n0 = x0Grid.length;
		final int n1 = x1Grid.length;
		final RealMatrix obstacle = MatrixUtils.createRealMatrix(n0 * n1, 1);

		for (int j = 0; j < n1; j++) {
			for (int i = 0; i < n0; i++) {
				final int k = flatten(i, j, n0);
				final double x0 = x0Grid[i];
				final double x1 = x1Grid[j];

				final BoundaryCondition boundaryCondition = chooseBoundaryCondition(i, j, x0Grid, x1Grid, boundaryTime);

				if (boundaryCondition != null && boundaryCondition.isDirichlet()) {
					obstacle.setEntry(k, 0, boundaryCondition.getValue());
				} else if (isInternalConstraintActive(boundaryTime, x0, x1)) {
					obstacle.setEntry(k, 0, getInternalConstrainedValue(boundaryTime, x0, x1));
				} else {
					obstacle.setEntry(k, 0, valueAtMaturity.applyAsDouble(x0, x1));
				}
			}
		}

		return obstacle;
	}

	/**
	 * Solves the linear complementarity problem
	 * {@code lhs * u >= rhs}, {@code u >= obstacle},
	 * together with the complementarity condition, using projected SOR.
	 *
	 * <p>
	 * The iteration performs a Gauss-Seidel SOR update at each row and then
	 * projects the result
	 * onto the obstacle set. The initial guess is typically the previous time-
	 * step solution.
	 * </p>
	 *
	 * @param lhs The system matrix.
	 * @param rhs The right-hand-side column vector.
	 * @param obstacle The obstacle column vector.
	 * @param initialGuess The initial guess for the iteration.
	 * @param omega The relaxation parameter.
	 * @param maxIterations The maximum number of SOR iterations.
	 * @param tolerance The convergence tolerance in supremum norm.
	 * @return The projected SOR solution as a column matrix.
	 * @throws IllegalArgumentException If a diagonal entry is numerically zero.
	 */
	private RealMatrix solveProjectedSOR(
			final RealMatrix lhs,
			final RealMatrix rhs,
			final RealMatrix obstacle,
			final RealMatrix initialGuess,
			final double omega,
			final int maxIterations,
			final double tolerance) {

		final int n = lhs.getRowDimension();
		final double[] u = new double[n];
		for (int i = 0; i < n; i++) {
			u[i] = Math.max(initialGuess.getEntry(i, 0), obstacle.getEntry(i, 0));
		}

		for (int iteration = 0; iteration < maxIterations; iteration++) {
			double maxChange = 0.0;

			for (int i = 0; i < n; i++) {
				final double diagonal = lhs.getEntry(i, i);
				if (Math.abs(diagonal) < 1E-14) {
					throw new IllegalArgumentException("Projected SOR failed due to near-zero diagonal entry at row " + i + ".");
				}

				double sum = 0.0;
				for (int j = 0; j < n; j++) {
					if (j != i) {
						sum += lhs.getEntry(i, j) * u[j];
					}
				}

				final double gaussSeidelValue = (rhs.getEntry(i, 0) - sum) / diagonal;
				final double relaxedValue = (1.0 - omega) * u[i] + omega * gaussSeidelValue;
				final double projectedValue = Math.max(obstacle.getEntry(i, 0), relaxedValue);

				maxChange = Math.max(maxChange, Math.abs(projectedValue - u[i]));
				u[i] = projectedValue;
			}

			if (maxChange < tolerance) {
				break;
			}
		}

		final RealMatrix solution = MatrixUtils.createRealMatrix(n, 1);
		for (int i = 0; i < n; i++) {
			solution.setEntry(i, 0, u[i]);
		}

		return solution;
	}

	/**
	 * Applies pointwise exercise projection to a solution vector at an exercise
	 * date.
	 *
	 * <p>
	 * Dirichlet boundary values and internal state constraints take precedence
	 * over the intrinsic exercise value.
	 * At all remaining nodes the solution is replaced by the maximum of
	 * continuation value and intrinsic value.
	 * </p>
	 *
	 * @param u The solution vector to be modified in place.
	 * @param x0Grid The spatial grid in the first dimension.
	 * @param x1Grid The spatial grid in the second dimension.
	 * @param boundaryTime The current model time.
	 * @param valueAtMaturity The intrinsic exercise value.
	 */
	private void applyExerciseProjection(
			final RealMatrix u,
			final double[] x0Grid,
			final double[] x1Grid,
			final double boundaryTime,
			final DoubleBinaryOperator valueAtMaturity) {

		final int n0 = x0Grid.length;
		final int n1 = x1Grid.length;

		for (int j = 0; j < n1; j++) {
			for (int i = 0; i < n0; i++) {
				final int k = flatten(i, j, n0);
				final double x0 = x0Grid[i];
				final double x1 = x1Grid[j];

				final BoundaryCondition boundaryCondition = chooseBoundaryCondition(i, j, x0Grid, x1Grid, boundaryTime);

				if (boundaryCondition != null && boundaryCondition.isDirichlet()) {
					u.setEntry(k, 0, boundaryCondition.getValue());
				} else if (isInternalConstraintActive(boundaryTime, x0, x1)) {
					u.setEntry(k, 0, getInternalConstrainedValue(boundaryTime, x0, x1));
				} else {
					u.setEntry(k, 0, Math.max(u.getEntry(k, 0), valueAtMaturity.applyAsDouble(x0, x1)));
				}
			}
		}
	}

	/**
	 * Reapplies internal state constraints to the current solution vector.
	 *
	 * @param u The solution vector to be modified in place.
	 * @param x0Grid The spatial grid in the first dimension.
	 * @param x1Grid The spatial grid in the second dimension.
	 * @param boundaryTime The current model time.
	 */
	private void reimposeInternalConstraints(
			final RealMatrix u,
			final double[] x0Grid,
			final double[] x1Grid,
			final double boundaryTime) {

		final int n0 = x0Grid.length;
		final int n1 = x1Grid.length;

		for (int j = 0; j < n1; j++) {
			for (int i = 0; i < n0; i++) {
				final double x0 = x0Grid[i];
				final double x1 = x1Grid[j];

				if (isInternalConstraintActive(boundaryTime, x0, x1)) {
					u.setEntry(flatten(i, j, n0), 0, getInternalConstrainedValue(boundaryTime, x0, x1));
				}
			}
		}
	}

	/**
	 * Reapplies outer Dirichlet boundary values to the current solution vector.
	 *
	 * @param u The solution vector to be modified in place.
	 * @param x0Grid The spatial grid in the first dimension.
	 * @param x1Grid The spatial grid in the second dimension.
	 * @param isBoundary The mask identifying outer boundary nodes.
	 * @param boundaryTime The current model time.
	 */
	private void reimposeBoundaryValues(
			final RealMatrix u,
			final double[] x0Grid,
			final double[] x1Grid,
			final boolean[] isBoundary,
			final double boundaryTime) {

		final int n0 = x0Grid.length;
		final int n1 = x1Grid.length;

		for (int j = 0; j < n1; j++) {
			for (int i = 0; i < n0; i++) {
				final int k = flatten(i, j, n0);
				if (!isBoundary[k]) {
					continue;
				}

				final BoundaryCondition boundaryCondition = chooseBoundaryCondition(i, j, x0Grid, x1Grid, boundaryTime);
				if (boundaryCondition != null && boundaryCondition.isDirichlet()) {
					u.setEntry(k, 0, boundaryCondition.getValue());
				}
			}
		}
	}

	/**
	 * Selects the relevant boundary condition for a boundary node.
	 *
	 * <p>
	 * If a node lies on multiple boundaries, precedence follows the order
	 * x0-lower, x0-upper, x1-lower, x1-upper, matching the original
	 * implementation style.
	 * </p>
	 *
	 * @param i The first-dimension grid index.
	 * @param j The second-dimension grid index.
	 * @param x0Grid The spatial grid in the first dimension.
	 * @param x1Grid The spatial grid in the second dimension.
	 * @param boundaryTime The current model time.
	 * @return The boundary condition applicable to the node, or {@code null}
	 *     for interior points.
	 */
	private BoundaryCondition chooseBoundaryCondition(
			final int i,
			final int j,
			final double[] x0Grid,
			final double[] x1Grid,
			final double boundaryTime) {

		final double x0 = x0Grid[i];
		final double x1 = x1Grid[j];

		final BoundaryCondition[] lowerConditions =
				model.getBoundaryConditionsAtLowerBoundary(product, boundaryTime, x0, x1);
		final BoundaryCondition[] upperConditions =
				model.getBoundaryConditionsAtUpperBoundary(product, boundaryTime, x0, x1);

		if (i == 0) {
			return lowerConditions[0];
		} else if (i == x0Grid.length - 1) {
			return upperConditions[0];
		} else if (j == 0) {
			return lowerConditions[1];
		} else if (j == x1Grid.length - 1) {
			return upperConditions[1];
		}

		return null;
	}

	/**
	 * Overwrites a matrix row as a Dirichlet condition.
	 *
	 * @param lhs The left-hand-side matrix to modify.
	 * @param rhs The right-hand-side vector to modify.
	 * @param row The row index to overwrite.
	 * @param value The Dirichlet value to impose.
	 * @param dimension The matrix dimension.
	 */
	private void overwriteAsDirichlet(
			final RealMatrix lhs,
			final RealMatrix rhs,
			final int row,
			final double value,
			final int dimension) {

		for (int col = 0; col < dimension; col++) {
			lhs.setEntry(row, col, 0.0);
		}

		lhs.setEntry(row, row, 1.0);
		rhs.setEntry(row, 0, value);
	}

	/**
	 * Computes the instantaneous short rate implied by the risk-free discount
	 * curve.
	 *
	 * @param time The time at which the short rate is required.
	 * @return The continuously compounded short rate inferred from the discount
	 *     factor.
	 */
	private double getShortRate(final double time) {
		final double safeTime = (time == 0.0 ? 1E-6 : time);
		return -Math.log(model.getRiskFreeCurve().getDiscountFactor(safeTime)) / safeTime;
	}

	/**
	 * Checks whether an internal state constraint is active at the specified
	 * grid point.
	 *
	 * @param time The model time.
	 * @param x0 The first state variable.
	 * @param x1 The second state variable.
	 * @return {@code true} if an internal constraint is active, {@code false}
	 *     otherwise.
	 */
	private boolean isInternalConstraintActive(final double time, final double x0, final double x1) {
		if (product instanceof FiniteDifferenceInternalStateConstraint) {
			return ((FiniteDifferenceInternalStateConstraint) product).isConstraintActive(time, x0, x1);
		}
		return false;
	}

	/**
	 * Returns the constrained value prescribed by the product at the specified
	 * point.
	 *
	 * @param time The model time.
	 * @param x0 The first state variable.
	 * @param x1 The second state variable.
	 * @return The constrained value.
	 */
	private double getInternalConstrainedValue(final double time, final double x0, final double x1) {
		return ((FiniteDifferenceInternalStateConstraint) product).getConstrainedValue(time, x0, x1);
	}

	/**
	 * Flattens a two-dimensional grid index into a one-dimensional index.
	 *
	 * @param i0 The index in the first dimension.
	 * @param i1 The index in the second dimension.
	 * @param n0 The size of the first dimension.
	 * @return The flattened index {@code i0 + i1*n0}.
	 */
	private static int flatten(final int i0, final int i1, final int n0) {
		return i0 + i1 * n0;
	}

	/**
	 * Builds a block-diagonal sparse matrix with identical blocks on the
	 * diagonal.
	 *
	 * @param block The block to replicate.
	 * @param numBlocks The number of blocks.
	 * @return The block-diagonal sparse matrix.
	 */
	private static RealMatrix buildBlockDiagonal(final RealMatrix block, final int numBlocks) {
		final int rowDimension = block.getRowDimension();
		final int blockDimension = rowDimension * numBlocks;
		final OpenMapRealMatrix out = new OpenMapRealMatrix(blockDimension, blockDimension);

		for (int b = 0; b < numBlocks; b++) {
			final int row0 = b * rowDimension;
			final int col0 = b * rowDimension;

			for (int i = 0; i < rowDimension; i++) {
				for (int j = Math.max(0, i - 2); j <= Math.min(rowDimension - 1, i + 2); j++) {
					final double value = block.getEntry(i, j);
					if (value != 0.0) {
						out.setEntry(row0 + i, col0 + j, value);
					}
				}
			}
		}

		return out;
	}

	/**
	 * Builds the sparse Kronecker product of two matrices.
	 *
	 * @param a The left matrix.
	 * @param b The right matrix.
	 * @return The sparse Kronecker product {@code a ⊗ b}.
	 */
	private static RealMatrix buildKron(final RealMatrix a, final RealMatrix b) {
		final int aRows = a.getRowDimension();
		final int aCols = a.getColumnDimension();
		final int bRows = b.getRowDimension();
		final int bCols = b.getColumnDimension();

		final OpenMapRealMatrix out = new OpenMapRealMatrix(aRows * bRows, aCols * bCols);

		for (int i = 0; i < aRows; i++) {
			for (int j = Math.max(0, i - 2); j <= Math.min(aCols - 1, i + 2); j++) {
				final double aValue = a.getEntry(i, j);
				if (aValue == 0.0) {
					continue;
				}

				final int rowBase = i * bRows;
				final int colBase = j * bCols;

				for (int p = 0; p < bRows; p++) {
					for (int q = Math.max(0, p - 2); q <= Math.min(bCols - 1, p + 2); q++) {
						final double bValue = b.getEntry(p, q);
						if (bValue == 0.0) {
							continue;
						}

						out.setEntry(rowBase + p, colBase + q, aValue * bValue);
					}
				}
			}
		}

		return out;
	}

	/**
	 * Builds the sparse Kronecker product {@code A ⊗ I}, where {@code I} is an
	 * identity matrix.
	 *
	 * @param a The left matrix.
	 * @param nIdentity The dimension of the identity matrix.
	 * @return The sparse matrix {@code a ⊗ I}.
	 */
	private static RealMatrix buildKronWithIdentityLeft(final RealMatrix a, final int nIdentity) {
		final int aRows = a.getRowDimension();
		final int aCols = a.getColumnDimension();
		final int N = aRows * nIdentity;

		final OpenMapRealMatrix out = new OpenMapRealMatrix(N, aCols * nIdentity);

		for (int i = 0; i < aRows; i++) {
			for (int j = Math.max(0, i - 2); j <= Math.min(aCols - 1, i + 2); j++) {
				final double aValue = a.getEntry(i, j);
				if (aValue == 0.0) {
					continue;
				}

				for (int k = 0; k < nIdentity; k++) {
					out.setEntry(i * nIdentity + k, j * nIdentity + k, aValue);
				}
			}
		}

		return out;
	}

	/**
	 * Container for the discrete two-dimensional differential operators.
	 */
	private static final class DifferentialOperators2D {

		/**
		 * The d0.
		 */
		private final RealMatrix d0;
		/**
		 * The d1.
		 */
		private final RealMatrix d1;
		/**
		 * The d00.
		 */
		private final RealMatrix d00;
		/**
		 * The d11.
		 */
		private final RealMatrix d11;
		/**
		 * The d01.
		 */
		private final RealMatrix d01;

		/**
		 * Creates a container for the discrete differential operators.
		 *
		 * @param d0 The first derivative operator with respect to the first
		 *     state variable.
		 * @param d1 The first derivative operator with respect to the second
		 *     state variable.
		 * @param d00 The second derivative operator with respect to the first
		 *     state variable.
		 * @param d11 The second derivative operator with respect to the second
		 *     state variable.
		 * @param d01 The mixed derivative operator.
		 */
		private DifferentialOperators2D(
				final RealMatrix d0,
				final RealMatrix d1,
				final RealMatrix d00,
				final RealMatrix d11,
				final RealMatrix d01) {
			this.d0 = d0;
			this.d1 = d1;
			this.d00 = d00;
			this.d11 = d11;
			this.d01 = d01;
		}

		/**
		 * Returns the first derivative operator with respect to the first state
		 * variable.
		 *
		 * @return The operator {@code d/dx0}.
		 */
		private RealMatrix getD0() {
			return d0;
		}

		/**
		 * Returns the first derivative operator with respect to the second
		 * state variable.
		 *
		 * @return The operator {@code d/dx1}.
		 */
		private RealMatrix getD1() {
			return d1;
		}

		/**
		 * Returns the second derivative operator with respect to the first
		 * state variable.
		 *
		 * @return The operator {@code d^2/dx0^2}.
		 */
		private RealMatrix getD00() {
			return d00;
		}

		/**
		 * Returns the second derivative operator with respect to the second
		 * state variable.
		 *
		 * @return The operator {@code d^2/dx1^2}.
		 */
		private RealMatrix getD11() {
			return d11;
		}

		/**
		 * Returns the mixed derivative operator.
		 *
		 * @return The operator {@code d^2/(dx0 dx1)}.
		 */
		private RealMatrix getD01() {
			return d01;
		}
	}

	/**
	 * Container for the model coefficients evaluated on the full flattened
	 * grid.
	 */
	private static final class ModelCoefficients2D {

		/**
		 * The mu0.
		 */
		private final RealMatrix mu0;
		/**
		 * The mu1.
		 */
		private final RealMatrix mu1;
		/**
		 * The a00.
		 */
		private final RealMatrix a00;
		/**
		 * The a11.
		 */
		private final RealMatrix a11;
		/**
		 * The a01.
		 */
		private final RealMatrix a01;
		/**
		 * The short rate.
		 */
		private final double shortRate;

		/**
		 * Creates a container for the model coefficient matrices.
		 *
		 * @param mu0 The diagonal matrix of first-component drift values.
		 * @param mu1 The diagonal matrix of second-component drift values.
		 * @param a00 The diagonal matrix of first-component variances.
		 * @param a11 The diagonal matrix of second-component variances.
		 * @param a01 The diagonal matrix of covariances.
		 * @param shortRate The instantaneous short rate.
		 */
		private ModelCoefficients2D(
				final RealMatrix mu0,
				final RealMatrix mu1,
				final RealMatrix a00,
				final RealMatrix a11,
				final RealMatrix a01,
				final double shortRate) {
			this.mu0 = mu0;
			this.mu1 = mu1;
			this.a00 = a00;
			this.a11 = a11;
			this.a01 = a01;
			this.shortRate = shortRate;
		}

		/**
		 * Returns the diagonal drift matrix for the first state variable.
		 *
		 * @return The matrix of first-component drifts.
		 */
		private RealMatrix getMu0() {
			return mu0;
		}

		/**
		 * Returns the diagonal drift matrix for the second state variable.
		 *
		 * @return The matrix of second-component drifts.
		 */
		private RealMatrix getMu1() {
			return mu1;
		}

		/**
		 * Returns the diagonal variance matrix for the first state variable.
		 *
		 * @return The matrix of first-component variances.
		 */
		private RealMatrix getA00() {
			return a00;
		}

		/**
		 * Returns the diagonal variance matrix for the second state variable.
		 *
		 * @return The matrix of second-component variances.
		 */
		private RealMatrix getA11() {
			return a11;
		}

		/**
		 * Returns the diagonal covariance matrix.
		 *
		 * @return The matrix of covariances.
		 */
		private RealMatrix getA01() {
			return a01;
		}

		/**
		 * Returns the instantaneous short rate.
		 *
		 * @return The short rate.
		 */
		private double getShortRate() {
			return shortRate;
		}
	}
}
