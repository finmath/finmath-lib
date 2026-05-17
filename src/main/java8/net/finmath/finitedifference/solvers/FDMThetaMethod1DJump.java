package net.finmath.finitedifference.solvers;

import java.util.Optional;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;

import net.finmath.finitedifference.FiniteDifferenceExerciseUtil;
import net.finmath.finitedifference.assetderivativevaluation.models.FiniteDifferenceEquityModel;
import net.finmath.finitedifference.assetderivativevaluation.models.JumpComponent;
import net.finmath.finitedifference.assetderivativevaluation.products.FiniteDifferenceEquityProduct;
import net.finmath.finitedifference.assetderivativevaluation.products.FiniteDifferenceInternalStateConstraint;
import net.finmath.finitedifference.boundaries.BoundaryCondition;
import net.finmath.finitedifference.grids.SpaceTimeDiscretization;
import net.finmath.modelling.Exercise;

/**
 * Theta-method solver for a one-dimensional PIDE with an explicit jump term.
 *
 * <p>
 * The local diffusive part is handled through the usual theta-method
 * discretization, leading to a tridiagonal linear system at each time step.
 * The non-local jump contribution is treated explicitly and is therefore added
 * to the right-hand side in IMEX form.
 * </p>
 *
 * <p>
 * For a one-dimensional state variable {@code x}, the jump term is interpreted
 * as
 * </p>
 *
 * <pre>
 * integral [ u(x exp(y)) - u(x) - x (exp(y) - 1) u_x(x) ] nu(dy),
 * </pre>
 *
 * <p>
 * where {@code y} denotes the log-relative jump size.
 * </p>
 *
 * <p>
 * The numerical quadrature splits the integration interval at zero in order to
 * avoid sampling the Levy density exactly at the singular point. This makes the
 * implementation suitable not only for finite-activity jump models such as
 * Merton, but also for infinite-activity models such as Variance Gamma,
 * provided the jump component supplies a finite numerical integration window.
 * </p>
 *
 * <p>
 * Current scope:
 * </p>
 * <ul>
 *   <li>one-dimensional space grids only,</li>
 *   <li>jumps acting on state variable index {@code 0},</li>
 *   <li>explicit treatment of the jump operator,</li>
 * <li>linear interpolation with constant extrapolation for shifted values.</li>
 * </ul>
 *
 * @author Alessandro Gnoatto
 */
public class FDMThetaMethod1DJump implements FDMSolver {

	/**
	 * The default quadrature points per side.
	 */
	private static final int DEFAULT_QUADRATURE_POINTS_PER_SIDE = 200;

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
	 * The quadrature points per side.
	 */
	private final int quadraturePointsPerSide;

	/**
	 * Creates a one-dimensional theta-method PIDE solver with explicit jump
	 * term.
	 *
	 * <p>
	 * This constructor uses a default number of quadrature cells on each side
	 * of
	 * zero for the evaluation of the jump integral.
	 * </p>
	 *
	 * @param model The finite-difference model.
	 * @param product The product to be valued.
	 * @param spaceTimeDiscretization The space-time discretization.
	 * @param exercise The exercise specification.
	 */
	public FDMThetaMethod1DJump(
			final FiniteDifferenceEquityModel model,
			final FiniteDifferenceEquityProduct product,
			final SpaceTimeDiscretization spaceTimeDiscretization,
			final Exercise exercise) {
		this(
				model,
				product,
				spaceTimeDiscretization,
				exercise,
				DEFAULT_QUADRATURE_POINTS_PER_SIDE
		);
	}

	/**
	 * Creates a one-dimensional theta-method PIDE solver with explicit jump
	 * term.
	 *
	 * <p>
	 * The local operator is handled implicitly through the theta method, while
	 * the
	 * jump contribution is evaluated explicitly by numerical quadrature.
	 * </p>
	 *
	 * @param model The finite-difference model.
	 * @param product The product to be valued.
	 * @param spaceTimeDiscretization The space-time discretization.
	 * @param exercise The exercise specification.
	 * @param quadraturePointsPerSide Number of quadrature cells used on each
	 *     side
	 *        of zero for the numerical evaluation of the jump integral.
	 */
	public FDMThetaMethod1DJump(
			final FiniteDifferenceEquityModel model,
			final FiniteDifferenceEquityProduct product,
			final SpaceTimeDiscretization spaceTimeDiscretization,
			final Exercise exercise,
			final int quadraturePointsPerSide) {

		if (model == null) {
			throw new IllegalArgumentException("Model must not be null.");
		}
		if (product == null) {
			throw new IllegalArgumentException("Product must not be null.");
		}
		if (spaceTimeDiscretization == null) {
			throw new IllegalArgumentException("Space-time discretization must not be null.");
		}
		if (exercise == null) {
			throw new IllegalArgumentException("Exercise must not be null.");
		}
		if (spaceTimeDiscretization.getNumberOfSpaceGrids() != 1) {
			throw new IllegalArgumentException("FDMThetaMethod1DJump requires a one-dimensional discretization.");
		}
		if (quadraturePointsPerSide <= 0) {
			throw new IllegalArgumentException("quadraturePointsPerSide must be positive.");
		}

		final Optional<JumpComponent> optionalJumpComponent = model.getJumpComponent();
		if (optionalJumpComponent.isPresent() && optionalJumpComponent.get().getStateVariableIndex() != 0) {
			throw new IllegalArgumentException(
					"FDMThetaMethod1DJump currently supports jumps only on state variable index 0."
			);
		}

		this.model = model;
		this.product = product;
		this.spaceTimeDiscretization = spaceTimeDiscretization;
		this.exercise = exercise;
		this.quadraturePointsPerSide = quadraturePointsPerSide;
	}

	/**
	 * Returns the full space-time solution for a payoff specified as a terminal
	 * value function.
	 *
	 * <p>
	 * The payoff is sampled on the spatial grid at maturity and then propagated
	 * backwards in time.
	 * </p>
	 *
	 * @param time The maturity time.
	 * @param valueAtMaturity The terminal payoff function.
	 * @return The full space-time solution matrix.
	 */
	@Override
	public double[][] getValues(final double time, final DoubleUnaryOperator valueAtMaturity) {

		final double[] xGrid = spaceTimeDiscretization.getSpaceGrid(0).getGrid();
		final double[] terminalValues = new double[xGrid.length];

		for (int i = 0; i < xGrid.length; i++) {
			terminalValues[i] = valueAtMaturity.applyAsDouble(xGrid[i]);
		}

		return getValuesInternal(time, terminalValues, valueAtMaturity, null);
	}

	/**
	 * Returns the solution at a given evaluation time for a payoff specified as
	 * a
	 * terminal value function.
	 *
	 * @param evaluationTime The time at which the solution is requested.
	 * @param time The maturity time.
	 * @param valueAtMaturity The terminal payoff function.
	 * @return The solution vector on the spatial grid at the requested time.
	 */
	@Override
	public double[] getValue(
			final double evaluationTime,
			final double time,
			final DoubleUnaryOperator valueAtMaturity) {

		final double[][] values = getValues(time, valueAtMaturity);
		return extractTimeSlice(values, time, evaluationTime);
	}

	/**
	 * Returns the full space-time solution for terminal values already sampled
	 * on
	 * the spatial grid.
	 *
	 * @param time The maturity time.
	 * @param terminalValues The terminal values on the spatial grid.
	 * @return The full space-time solution matrix.
	 */
	@Override
	public double[][] getValues(final double time, final double[] terminalValues) {
		return getValuesInternal(time, terminalValues, null, null);
	}

	/**
	 * Returns the solution at a given evaluation time for terminal values
	 * already
	 * sampled on the spatial grid.
	 *
	 * @param evaluationTime The time at which the solution is requested.
	 * @param time The maturity time.
	 * @param terminalValues The terminal values on the spatial grid.
	 * @return The solution vector on the spatial grid at the requested time.
	 */
	@Override
	public double[] getValue(
			final double evaluationTime,
			final double time,
			final double[] terminalValues) {

		final double[][] values = getValues(time, terminalValues);
		return extractTimeSlice(values, time, evaluationTime);
	}

	/**
	 * Returns the full space-time solution for a product with discrete early
	 * exercise opportunities.
	 *
	 * @param time The maturity time.
	 * @param terminalValues The terminal values on the spatial grid.
	 * @param exerciseValue The pointwise exercise value.
	 * @return The full space-time solution matrix.
	 */
	@Override
	public double[][] getValues(
			final double time,
			final double[] terminalValues,
			final DoubleUnaryOperator exerciseValue) {
		return getValuesInternal(time, terminalValues, exerciseValue, null);
	}

	/**
	 * Returns the solution at a given evaluation time for a product with
	 * discrete
	 * early exercise opportunities.
	 *
	 * @param evaluationTime The time at which the solution is requested.
	 * @param time The maturity time.
	 * @param terminalValues The terminal values on the spatial grid.
	 * @param exerciseValue The pointwise exercise value.
	 * @return The solution vector on the spatial grid at the requested time.
	 */
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
	 * Returns the full space-time solution for a problem with a continuous
	 * obstacle.
	 *
	 * @param time The maturity time.
	 * @param terminalValues The terminal values on the spatial grid.
	 * @param continuousObstacleValue The obstacle as a function of time and
	 *     state.
	 * @return The full space-time solution matrix.
	 */
	public double[][] getValues(
			final double time,
			final double[] terminalValues,
			final DoubleBinaryOperator continuousObstacleValue) {
		return getValuesInternal(time, terminalValues, null, continuousObstacleValue);
	}

	/**
	 * Returns the solution at a given evaluation time for a problem with a
	 * continuous obstacle.
	 *
	 * @param evaluationTime The time at which the solution is requested.
	 * @param time The maturity time.
	 * @param terminalValues The terminal values on the spatial grid.
	 * @param continuousObstacleValue The obstacle as a function of time and
	 *     state.
	 * @return The solution vector on the spatial grid at the requested time.
	 */
	public double[] getValue(
			final double evaluationTime,
			final double time,
			final double[] terminalValues,
			final DoubleBinaryOperator continuousObstacleValue) {

		final double[][] values = getValues(time, terminalValues, continuousObstacleValue);
		return extractTimeSlice(values, time, evaluationTime);
	}

	/**
	 * Extracts the solution vector corresponding to a requested evaluation time
	 * from the full space-time solution matrix.
	 *
	 * @param values The full space-time solution matrix.
	 * @param time The maturity time.
	 * @param evaluationTime The requested evaluation time.
	 * @return The solution vector on the spatial grid.
	 */
	private double[] extractTimeSlice(
			final double[][] values,
			final double time,
			final double evaluationTime) {

		final double tau = time - evaluationTime;
		final int timeIndex =
				spaceTimeDiscretization.getTimeDiscretization().getTimeIndexNearestLessOrEqual(tau);

		final double[] column = new double[values.length];
		for (int i = 0; i < values.length; i++) {
			column[i] = values[i][timeIndex];
		}
		return column;
	}

	/**
	 * Core backward solver routine.
	 *
	 * <p>
	 * Starting from terminal values, this method performs backward time-
	 * stepping
	 * with a theta discretization of the local operator and an explicit
	 * quadrature-based treatment of the jump term.
	 * </p>
	 *
	 * @param time The maturity time.
	 * @param terminalValues The terminal values on the spatial grid.
	 * @param exerciseValue The discrete exercise value, if applicable.
	 * @param continuousObstacleValue The continuous obstacle, if applicable.
	 * @return The full space-time solution matrix.
	 */
	private double[][] getValuesInternal(
			final double time,
			final double[] terminalValues,
			final DoubleUnaryOperator exerciseValue,
			final DoubleBinaryOperator continuousObstacleValue) {

		final double[] xGrid = spaceTimeDiscretization.getSpaceGrid(0).getGrid();
		final int nX = xGrid.length;

		if (terminalValues == null) {
			throw new IllegalArgumentException("terminalValues must not be null.");
		}
		if (terminalValues.length != nX) {
			throw new IllegalArgumentException("terminalValues length does not match spatial grid length.");
		}
		if (exerciseValue != null && continuousObstacleValue != null) {
			throw new IllegalArgumentException(
					"Provide either a discrete exercise payoff or a continuous obstacle, not both."
			);
		}
		if ((exercise.isBermudan() || exercise.isAmerican())
				&& exerciseValue == null
				&& continuousObstacleValue == null) {
			throw new IllegalArgumentException(
					"Non-European exercise requires a pointwise exercise payoff function."
			);
		}

		final Optional<JumpComponent> optionalJumpComponent = model.getJumpComponent();

		final double theta = spaceTimeDiscretization.getTheta();
		final int timeLength = spaceTimeDiscretization.getTimeDiscretization().getNumberOfTimeSteps() + 1;
		final int numberOfTimeSteps = spaceTimeDiscretization.getTimeDiscretization().getNumberOfTimeSteps();

		double[] u = terminalValues.clone();
		final double[][] z = new double[nX][timeLength];

		for (int i = 0; i < nX; i++) {
			z[i][0] = u[i];
		}

		for (int m = 0; m < numberOfTimeSteps; m++) {

			final double deltaTau = spaceTimeDiscretization.getTimeDiscretization().getTimeStep(m);

			final double tm = spaceTimeDiscretization.getTimeDiscretization().getTime(numberOfTimeSteps - m);
			final double tmp1 = spaceTimeDiscretization.getTimeDiscretization().getTime(numberOfTimeSteps - (m + 1));

			final ThetaMethod1DAssembly.ModelCoefficients coefficientsM =
					ThetaMethod1DAssembly.buildModelCoefficients(model, xGrid, tm);
			final ThetaMethod1DAssembly.ModelCoefficients coefficientsMp1 =
					ThetaMethod1DAssembly.buildModelCoefficients(model, xGrid, tmp1);

			final TridiagonalMatrix lhs = new TridiagonalMatrix(nX);
			final TridiagonalMatrix rhsOperator = new TridiagonalMatrix(nX);

			ThetaMethod1DAssembly.buildThetaLeftHandSide(
					lhs,
					xGrid,
					coefficientsMp1.getDrift(),
					coefficientsMp1.getVariance(),
					coefficientsMp1.getShortRate(),
					deltaTau,
					theta
			);

			ThetaMethod1DAssembly.buildThetaRightHandSide(
					rhsOperator,
					xGrid,
					coefficientsM.getDrift(),
					coefficientsM.getVariance(),
					coefficientsM.getShortRate(),
					deltaTau,
					theta
			);

			final double[] rhs = ThetaMethod1DAssembly.apply(rhsOperator, u);

			if (optionalJumpComponent.isPresent()) {
				final double[] jumpContribution = computeExplicitJumpContribution(
						optionalJumpComponent.get(),
						xGrid,
						u,
						tm
				);

				for (int i = 0; i < nX; i++) {
					rhs[i] += deltaTau * jumpContribution[i];
				}
			}

			final double tauMp1 = spaceTimeDiscretization.getTimeDiscretization().getTime(m + 1);
			final double boundaryTime = spaceTimeDiscretization.getTimeDiscretization().getLastTime() - tauMp1;

			final BoundaryCondition lowerCondition =
					model.getBoundaryConditionsAtLowerBoundary(product, boundaryTime, xGrid[0])[0];

			if (lowerCondition.isDirichlet()) {
				ThetaMethod1DAssembly.overwriteAsDirichlet(lhs, rhs, 0, lowerCondition.getValue());
			}

			final BoundaryCondition upperCondition =
					model.getBoundaryConditionsAtUpperBoundary(product, boundaryTime, xGrid[nX - 1])[0];

			if (upperCondition.isDirichlet()) {
				ThetaMethod1DAssembly.overwriteAsDirichlet(lhs, rhs, nX - 1, upperCondition.getValue());
			}

			for (int i = 1; i < nX - 1; i++) {
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
				nextU = ThomasSolver.solve(lhs.getLowerDiagonal(), lhs.getMainDiagonal(), lhs.getUpperDiagonal(), rhs);

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

			u = nextU;
			for (int i = 0; i < nX; i++) {
				z[i][m + 1] = u[i];
			}
		}

		return z;
	}

	/**
	 * Computes the explicit jump contribution on the whole spatial grid for one
	 * time layer.
	 *
	 * @param jumpComponent The jump component.
	 * @param xGrid The spatial grid.
	 * @param u The current solution values.
	 * @param time The current time.
	 * @return The jump contribution at all spatial grid points.
	 */
	private double[] computeExplicitJumpContribution(
			final JumpComponent jumpComponent,
			final double[] xGrid,
			final double[] u,
			final double time) {

		final int n = xGrid.length;
		final double[] firstDerivative = computeFirstDerivative(xGrid, u);
		final double[] result = new double[n];

		for (int i = 0; i < n; i++) {
			final double x = xGrid[i];
			final double lowerBound = jumpComponent.getLowerIntegrationBound(time, x);
			final double upperBound = jumpComponent.getUpperIntegrationBound(time, x);

			if (!(lowerBound < upperBound)) {
				result[i] = 0.0;
				continue;
			}

			result[i] = integrateJumpContributionAtNode(
					jumpComponent,
					xGrid,
					u,
					x,
					u[i],
					firstDerivative[i],
					time,
					lowerBound,
					upperBound
			);
		}

		return result;
	}

	/**
	 * Computes the jump integral at a single spatial grid point.
	 *
	 * @param jumpComponent The jump component.
	 * @param xGrid The spatial grid.
	 * @param u The current solution values.
	 * @param x The spatial point at which the integral is evaluated.
	 * @param uAtX The solution value at {@code x}.
	 * @param derivativeAtX The first derivative at {@code x}.
	 * @param time The current time.
	 * @param lowerBound The lower integration bound.
	 * @param upperBound The upper integration bound.
	 * @return The jump contribution at the given point.
	 */
	private double integrateJumpContributionAtNode(
			final JumpComponent jumpComponent,
			final double[] xGrid,
			final double[] u,
			final double x,
			final double uAtX,
			final double derivativeAtX,
			final double time,
			final double lowerBound,
			final double upperBound) {

		double integral = 0.0;

		if (lowerBound < 0.0) {
			final double negativeUpper = Math.min(upperBound, 0.0);
			if (lowerBound < negativeUpper) {
				integral += midpointIntegrate(
						jumpComponent,
						xGrid,
						u,
						x,
						uAtX,
						derivativeAtX,
						time,
						lowerBound,
						negativeUpper,
						quadraturePointsPerSide
				);
			}
		}

		if (upperBound > 0.0) {
			final double positiveLower = Math.max(lowerBound, 0.0);
			if (positiveLower < upperBound) {
				integral += midpointIntegrate(
						jumpComponent,
						xGrid,
						u,
						x,
						uAtX,
						derivativeAtX,
						time,
						positiveLower,
						upperBound,
						quadraturePointsPerSide
				);
			}
		}

		return integral;
	}

	/**
	 * Evaluates one subinterval of the jump integral by midpoint quadrature.
	 *
	 * <p>
	 * Splitting the integral into negative and positive parts avoids evaluating
	 * the Levy density exactly at zero.
	 * </p>
	 *
	 * @param jumpComponent The jump component.
	 * @param xGrid The spatial grid.
	 * @param u The current solution values.
	 * @param x The spatial point at which the integral is evaluated.
	 * @param uAtX The solution value at {@code x}.
	 * @param derivativeAtX The first derivative at {@code x}.
	 * @param time The current time.
	 * @param a The lower endpoint of the quadrature interval.
	 * @param b The upper endpoint of the quadrature interval.
	 * @param numberOfCells The number of midpoint cells.
	 * @return The quadrature approximation on the given subinterval.
	 */
	private double midpointIntegrate(
			final JumpComponent jumpComponent,
			final double[] xGrid,
			final double[] u,
			final double x,
			final double uAtX,
			final double derivativeAtX,
			final double time,
			final double a,
			final double b,
			final int numberOfCells) {

		if (!(a < b)) {
			return 0.0;
		}

		final double h = (b - a) / numberOfCells;
		double integral = 0.0;

		for (int k = 0; k < numberOfCells; k++) {
			final double y = a + (k + 0.5) * h;
			final double shiftedX = x * Math.exp(y);
			final double shiftedValue = interpolateLinearWithConstantExtrapolation(xGrid, u, shiftedX);
			final double levyDensity = jumpComponent.getLevyDensity(time, y, x);

			final double integrand =
					shiftedValue
					- uAtX
					- x * (Math.exp(y) - 1.0) * derivativeAtX;

			integral += integrand * levyDensity;
		}

		return h * integral;
	}

	/**
	 * Computes the first derivative of the solution on the spatial grid.
	 *
	 * <p>
	 * One-sided differences are used at the boundaries and a central
	 * non-uniform-grid formula is used in the interior.
	 * </p>
	 *
	 * @param xGrid The spatial grid.
	 * @param values The solution values.
	 * @return The approximate first derivative on the grid.
	 */
	private double[] computeFirstDerivative(final double[] xGrid, final double[] values) {

		final int n = xGrid.length;
		final double[] derivative = new double[n];

		if (n < 2) {
			throw new IllegalArgumentException("Need at least two spatial grid points.");
		}

		if (n == 2) {
			final double slope = (values[1] - values[0]) / (xGrid[1] - xGrid[0]);
			derivative[0] = slope;
			derivative[1] = slope;
			return derivative;
		}

		for (int i = 0; i < n; i++) {
			if (i == 0) {
				final double h = xGrid[1] - xGrid[0];
				derivative[i] = (values[1] - values[0]) / h;
			} else if (i == n - 1) {
				final double h = xGrid[n - 1] - xGrid[n - 2];
				derivative[i] = (values[n - 1] - values[n - 2]) / h;
			} else {
				final double h0 = xGrid[i] - xGrid[i - 1];
				final double h1 = xGrid[i + 1] - xGrid[i];

				final double lowerWeight = -h1 / (h0 * (h0 + h1));
				final double diagWeight = (h1 - h0) / (h0 * h1);
				final double upperWeight = h0 / (h1 * (h0 + h1));

				derivative[i] =
						lowerWeight * values[i - 1]
						+ diagWeight * values[i]
						+ upperWeight * values[i + 1];
			}
		}

		return derivative;
	}

	/**
	 * Evaluates the solution at a shifted point by linear interpolation on the
	 * grid with constant extrapolation outside the grid range.
	 *
	 * @param xGrid The spatial grid.
	 * @param values The solution values.
	 * @param x The evaluation point.
	 * @return The interpolated value.
	 */
	private double interpolateLinearWithConstantExtrapolation(
			final double[] xGrid,
			final double[] values,
			final double x) {

		if (x <= xGrid[0]) {
			return values[0];
		}
		if (x >= xGrid[xGrid.length - 1]) {
			return values[values.length - 1];
		}

		int left = 0;
		int right = xGrid.length - 1;

		while (right - left > 1) {
			final int mid = (left + right) >>> 1;
			if (xGrid[mid] <= x) {
				left = mid;
			} else {
				right = mid;
			}
		}

		final double x0 = xGrid[left];
		final double x1 = xGrid[left + 1];
		final double u0 = values[left];
		final double u1 = values[left + 1];

		final double weight = (x - x0) / (x1 - x0);
		return (1.0 - weight) * u0 + weight * u1;
	}

	/**
	 * Builds the obstacle vector for discrete early exercise.
	 *
	 * @param xGrid The spatial grid.
	 * @param boundaryTime The current calendar time.
	 * @param exerciseValue The pointwise exercise value.
	 * @param lowerCondition The lower boundary condition.
	 * @param upperCondition The upper boundary condition.
	 * @return The obstacle vector.
	 */
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

	/**
	 * Builds the obstacle vector for a continuous obstacle problem.
	 *
	 * @param xGrid The spatial grid.
	 * @param boundaryTime The current calendar time.
	 * @param continuousObstacleValue The obstacle as a function of time and
	 *     state.
	 * @param lowerCondition The lower boundary condition.
	 * @param upperCondition The upper boundary condition.
	 * @return The obstacle vector.
	 */
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

	/**
	 * Applies a discrete exercise projection after one backward step.
	 *
	 * @param u The current solution vector.
	 * @param xGrid The spatial grid.
	 * @param boundaryTime The current calendar time.
	 * @param exerciseValue The pointwise exercise value.
	 * @param lowerCondition The lower boundary condition.
	 * @param upperCondition The upper boundary condition.
	 */
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

	/**
	 * Reimposes internal state constraints after one backward step.
	 *
	 * @param u The solution vector.
	 * @param xGrid The spatial grid.
	 * @param boundaryTime The current calendar time.
	 */
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

	/**
	 * Reimposes Dirichlet boundary values after one backward step.
	 *
	 * @param u The solution vector.
	 * @param lowerCondition The lower boundary condition.
	 * @param upperCondition The upper boundary condition.
	 */
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

	/**
	 * Checks whether an internal state constraint is active at a given time and
	 * state.
	 *
	 * @param time The current calendar time.
	 * @param x The state value.
	 * @return {@code true} if an internal constraint is active.
	 */
	private boolean isInternalConstraintActive(final double time, final double x) {
		if (product instanceof FiniteDifferenceInternalStateConstraint) {
			return ((FiniteDifferenceInternalStateConstraint) product).isConstraintActive(time, x);
		}
		return false;
	}

	/**
	 * Returns the constrained value associated with an active internal
	 * constraint.
	 *
	 * @param time The current calendar time.
	 * @param x The state value.
	 * @return The constrained value.
	 */
	private double getInternalConstrainedValue(final double time, final double x) {
		return ((FiniteDifferenceInternalStateConstraint) product).getConstrainedValue(time, x);
	}
}
