package net.finmath.finitedifference.solvers;

import java.util.Arrays;

import net.finmath.finitedifference.assetderivativevaluation.models.FiniteDifferenceEquityModel;

/**
 * Shared matrix-free assembly utilities for one-dimensional theta-method
 * finite-difference solvers.
 *
 * <p>
 * This class is primarily numerical: it assembles the one-dimensional spatial
 * operator directly into tridiagonal coefficients.
 * </p>
 *
 * <p>
 * The generic assembly methods work with arrays for drift, variance, and local
 * discount / reaction coefficients, which allows state-dependent reaction terms
 * as required for interest-rate PDEs.
 * </p>
 *
 * <p>
 * For backward compatibility with equity-only solvers using deterministic
 * discounting, the class also provides convenience methods based on a scalar
 * short rate.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public final class ThetaMethod1DAssembly {

	/**
	 * The safe time epsilon.
	 */
	private static final double SAFE_TIME_EPSILON = 1E-6;

	private ThetaMethod1DAssembly() {
	}

	/**
	 * Container for model coefficients on a one-dimensional grid in the
	 * deterministic-rate equity case.
	 *
	 * <p>
	 * This compatibility container is still useful for jump and two-state
	 * equity
	 * solvers, where the discounting term is spatially constant.
	 * </p>
	 */
	public static final class ModelCoefficients {

		/**
		 * The drift.
		 */
		private final double[] drift;
		/**
		 * The variance.
		 */
		private final double[] variance;
		/**
		 * The short rate.
		 */
		private final double shortRate;

		/**
		 * Performs the operation.
		 *
		 * @param drift The value.
		 * @param variance The value.
		 * @param shortRate The value.
		 */
		public ModelCoefficients(
				final double[] drift,
				final double[] variance,
				final double shortRate) {
			this.drift = drift;
			this.variance = variance;
			this.shortRate = shortRate;
		}

		/**
		 * Returns the value.
		 *
		 * @return The value.
		 */
		public double[] getDrift() {
			return drift;
		}

		/**
		 * Returns the value.
		 *
		 * @return The value.
		 */
		public double[] getVariance() {
			return variance;
		}

		/**
		 * Returns the value.
		 *
		 * @return The value.
		 */
		public double getShortRate() {
			return shortRate;
		}
	}

	private static final class RowCoefficients {

		/**
		 * The lower.
		 */
		private final double lower;
		/**
		 * The diag.
		 */
		private final double diag;
		/**
		 * The upper.
		 */
		private final double upper;

		private RowCoefficients(final double lower, final double diag, final double upper) {
			this.lower = lower;
			this.diag = diag;
			this.upper = upper;
		}
	}

	/**
	 * Compatibility method for equity-only solvers with deterministic
	 * discounting.
	 *
	 * <p>
	 * Evaluates the one-dimensional drift, variance, and scalar short rate on
	 * the
	 * supplied grid at one time.
	 * </p>
	 *
	 * @param model The finite-difference equity model.
	 * @param xGrid The one-dimensional state grid.
	 * @param time The running time.
	 * @return The model coefficients.
	 */
	public static ModelCoefficients buildModelCoefficients(
			final FiniteDifferenceEquityModel model,
			final double[] xGrid,
			final double time) {

		final int numberOfGridPoints = xGrid.length;

		final double[] drift = new double[numberOfGridPoints];
		final double[] variance = new double[numberOfGridPoints];

		for (int i = 0; i < numberOfGridPoints; i++) {
			final double x = xGrid[i];

			drift[i] = model.getDrift(time, x)[0];

			final double[][] factorLoading = model.getFactorLoading(time, x);

			double localVariance = 0.0;
			for (int factor = 0; factor < factorLoading[0].length; factor++) {
				final double b = factorLoading[0][factor];
				localVariance += b * b;
			}
			variance[i] = localVariance;
		}

		return new ModelCoefficients(drift, variance, getShortRate(model, time));
	}

	/**
	 * Compatibility method for equity-only solvers with deterministic
	 * discounting.
	 *
	 * <p>
	 * Computes the continuously compounded short rate implied by the model
	 * discount curve.
	 * </p>
	 *
	 * @param model The finite-difference equity model.
	 * @param time The running time.
	 * @return The short rate.
	 */
	public static double getShortRate(final FiniteDifferenceEquityModel model, final double time) {
		final double safeTime = time == 0.0 ? SAFE_TIME_EPSILON : Math.max(time, SAFE_TIME_EPSILON);
		return -Math.log(model.getRiskFreeCurve().getDiscountFactor(safeTime)) / safeTime;
	}

	/**
	 * Compatibility overload for deterministic-rate solvers.
	 *
	 * <p>
	 * Builds the left-hand side matrix for the theta step using a scalar short
	 * rate.
	 * </p>
	 *
	 * @param lhs The tridiagonal matrix to overwrite.
	 * @param xGrid The one-dimensional state grid.
	 * @param drift The drift values on the grid.
	 * @param variance The variance values on the grid.
	 * @param shortRate The scalar short rate.
	 * @param deltaTau The time step size.
	 * @param theta The theta parameter.
	 */
	public static void buildThetaLeftHandSide(
			final TridiagonalMatrix lhs,
			final double[] xGrid,
			final double[] drift,
			final double[] variance,
			final double shortRate,
			final double deltaTau,
			final double theta) {

		final double[] localDiscountRate = new double[xGrid.length];
		Arrays.fill(localDiscountRate, shortRate);

		buildThetaLeftHandSide(
				lhs,
				xGrid,
				drift,
				variance,
				localDiscountRate,
				deltaTau,
				theta
		);
	}

	/**
	 * Builds the left-hand side matrix for the theta step:
	 *
	 * <p>
	 * <i>
	 * I - theta * dt * L(t_{m+1}).
	 * </i>
	 * </p>
	 *
	 * @param lhs The tridiagonal matrix to overwrite.
	 * @param xGrid The one-dimensional state grid.
	 * @param drift The drift values on the grid.
	 * @param variance The variance values on the grid.
	 * @param localDiscountRate The local reaction / discount coefficients on
	 *     the grid.
	 * @param deltaTau The time step size.
	 * @param theta The theta parameter.
	 */
	public static void buildThetaLeftHandSide(
			final TridiagonalMatrix lhs,
			final double[] xGrid,
			final double[] drift,
			final double[] variance,
			final double[] localDiscountRate,
			final double deltaTau,
			final double theta) {

		final double alpha = theta * deltaTau;

		for (int i = 0; i < xGrid.length; i++) {
			final RowCoefficients spatial = spatialOperatorRow(
					i,
					xGrid,
					drift[i],
					variance[i],
					localDiscountRate[i]
			);

			lhs.lower[i] = -alpha * spatial.lower;
			lhs.diag[i] = 1.0 - alpha * spatial.diag;
			lhs.upper[i] = -alpha * spatial.upper;
		}
	}

	/**
	 * Compatibility overload for deterministic-rate solvers.
	 *
	 * <p>
	 * Builds the right-hand side operator for the theta step using a scalar
	 * short
	 * rate.
	 * </p>
	 *
	 * @param rhsOperator The tridiagonal matrix to overwrite.
	 * @param xGrid The one-dimensional state grid.
	 * @param drift The drift values on the grid.
	 * @param variance The variance values on the grid.
	 * @param shortRate The scalar short rate.
	 * @param deltaTau The time step size.
	 * @param theta The theta parameter.
	 */
	public static void buildThetaRightHandSide(
			final TridiagonalMatrix rhsOperator,
			final double[] xGrid,
			final double[] drift,
			final double[] variance,
			final double shortRate,
			final double deltaTau,
			final double theta) {

		final double[] localDiscountRate = new double[xGrid.length];
		Arrays.fill(localDiscountRate, shortRate);

		buildThetaRightHandSide(
				rhsOperator,
				xGrid,
				drift,
				variance,
				localDiscountRate,
				deltaTau,
				theta
		);
	}

	/**
	 * Builds the right-hand side operator for the theta step:
	 *
	 * <p>
	 * <i>
	 * I + (1-theta) * dt * L(t_m).
	 * </i>
	 * </p>
	 *
	 * @param rhsOperator The tridiagonal matrix to overwrite.
	 * @param xGrid The one-dimensional state grid.
	 * @param drift The drift values on the grid.
	 * @param variance The variance values on the grid.
	 * @param localDiscountRate The local reaction / discount coefficients on
	 *     the grid.
	 * @param deltaTau The time step size.
	 * @param theta The theta parameter.
	 */
	public static void buildThetaRightHandSide(
			final TridiagonalMatrix rhsOperator,
			final double[] xGrid,
			final double[] drift,
			final double[] variance,
			final double[] localDiscountRate,
			final double deltaTau,
			final double theta) {

		final double alpha = (1.0 - theta) * deltaTau;

		for (int i = 0; i < xGrid.length; i++) {
			final RowCoefficients spatial = spatialOperatorRow(
					i,
					xGrid,
					drift[i],
					variance[i],
					localDiscountRate[i]
			);

			rhsOperator.lower[i] = alpha * spatial.lower;
			rhsOperator.diag[i] = 1.0 + alpha * spatial.diag;
			rhsOperator.upper[i] = alpha * spatial.upper;
		}
	}

	/**
	 * Applies a tridiagonal operator to a vector.
	 *
	 * @param matrix The tridiagonal matrix.
	 * @param vector The vector.
	 * @return The product {@code matrix * vector}.
	 */
	public static double[] apply(final TridiagonalMatrix matrix, final double[] vector) {
		final int n = vector.length;
		final double[] result = new double[n];

		for (int i = 0; i < n; i++) {
			double value = matrix.diag[i] * vector[i];
			if (i > 0) {
				value += matrix.lower[i] * vector[i - 1];
			}
			if (i < n - 1) {
				value += matrix.upper[i] * vector[i + 1];
			}
			result[i] = value;
		}

		return result;
	}

	/**
	 * Overwrites one matrix row with a Dirichlet condition.
	 *
	 * @param matrix The matrix.
	 * @param rhs The right-hand side vector.
	 * @param row The row to overwrite.
	 * @param value The prescribed value.
	 */
	public static void overwriteAsDirichlet(
			final TridiagonalMatrix matrix,
			final double[] rhs,
			final int row,
			final double value) {

		matrix.lower[row] = 0.0;
		matrix.diag[row] = 1.0;
		matrix.upper[row] = 0.0;
		rhs[row] = value;
	}

	private static RowCoefficients spatialOperatorRow(
			final int i,
			final double[] xGrid,
			final double drift,
			final double variance,
			final double localDiscountRate) {

		final int n = xGrid.length;
		final double halfVariance = 0.5 * variance;

		double firstDerivativeLower = 0.0;
		double firstDerivativeDiagonal = 0.0;
		double firstDerivativeUpper = 0.0;

		double secondDerivativeLower = 0.0;
		double secondDerivativeDiagonal = 0.0;
		double secondDerivativeUpper = 0.0;

		if (i == 0) {
			final double h1 = xGrid[1] - xGrid[0];
			final double h2 = xGrid[2] - xGrid[1];

			firstDerivativeDiagonal = -1.0 / h1;
			firstDerivativeUpper = 1.0 / h1;

			secondDerivativeDiagonal = -2.0 / (h1 * h2);
			secondDerivativeUpper = 2.0 / (h1 * (h1 + h2));
		} else if (i == n - 1) {
			final double h0 = xGrid[i] - xGrid[i - 1];
			final double h3 = xGrid[i - 1] - xGrid[i - 2];

			firstDerivativeLower = -1.0 / h0;
			firstDerivativeDiagonal = 1.0 / h0;

			secondDerivativeLower = 2.0 / (h0 * (h0 + h3));
			secondDerivativeDiagonal = -2.0 / (h3 * h0);
		} else {
			final double h0 = xGrid[i] - xGrid[i - 1];
			final double h1 = xGrid[i + 1] - xGrid[i];

			firstDerivativeLower = -h1 / (h0 * (h1 + h0));
			firstDerivativeDiagonal = (h1 - h0) / (h1 * h0);
			firstDerivativeUpper = h0 / (h1 * (h0 + h1));

			secondDerivativeLower = 2.0 / (h0 * (h0 + h1));
			secondDerivativeDiagonal = -2.0 / (h0 * h1);
			secondDerivativeUpper = 2.0 / (h1 * (h0 + h1));
		}

		return new RowCoefficients(
				drift * firstDerivativeLower + halfVariance * secondDerivativeLower,
				drift * firstDerivativeDiagonal + halfVariance * secondDerivativeDiagonal - localDiscountRate,
				drift * firstDerivativeUpper + halfVariance * secondDerivativeUpper
		);
	}
}
