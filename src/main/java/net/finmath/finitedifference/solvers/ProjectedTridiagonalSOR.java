package net.finmath.finitedifference.solvers;

/**
 * Utility class providing a projected successive over-relaxation (PSOR)
 * algorithm for tridiagonal linear complementarity problems.
 * <p>
 * The class is intended for problems of the form
 * </p>
 * <pre>
 *     A x >= b
 *     x >= obstacle
 *     (A x - b)_i (x_i - obstacle_i) = 0
 * </pre>
 * <p>
 * where {@code A} is a tridiagonal matrix, {@code b} is the right-hand side,
 * and {@code obstacle} is the lower obstacle or payoff constraint.
 * </p>
 *
 * <p>
 * The method combines a Gauss-Seidel iteration with over-relaxation and a
 * pointwise projection onto the admissible set defined by the obstacle. It is
 * particularly useful for finite difference discretizations of variational
 * inequalities, for example in the pricing of American-style options.
 * </p>
 *
 * <p>
 * Two overloads of the solver are provided: one accepting a
 * {@link TridiagonalMatrix} container and one accepting the three diagonals
 * directly. Additional utility methods compute the infinity norm of the
 * complementarity residual for a candidate solution.
 * </p>
 *
 * <p>
 * The caller must provide arrays of consistent length, a positive system
 * dimension, and a tridiagonal system with non-zero diagonal entries.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public final class ProjectedTridiagonalSOR {

	/**
	 * Creates no instances of this utility class.
	 */
	private ProjectedTridiagonalSOR() {
	}

	/**
	 * Solves a tridiagonal linear complementarity problem using projected SOR.
	 * <p>
	 * This is a convenience overload accepting the system matrix in
	 * {@link TridiagonalMatrix} form.
	 * </p>
	 *
	 * @param matrix The tridiagonal system matrix.
	 * @param rhs The right-hand side vector {@code b}.
	 * @param obstacle The obstacle vector defining the lower bound constraint
	 *         {@code x >= obstacle}.
	 * @param initialGuess The initial iterate used to start the PSOR iteration.
	 * @param omega The relaxation parameter. Values in the interval
	 * 		{@code (0, 2)} are typically used in practice.
	 * @param maxIterations The maximum number of PSOR iterations.
	 * @param tolerance The stopping tolerance for the maximum absolute change
	 * 		between two consecutive iterates. If {@code tolerance <= 0}, the
	 * 		iteration runs for the full number of iterations.
	 * @return An approximate solution of the tridiagonal linear complementarity
	 *     problem.
	 */
	public static double[] solve(
			final TridiagonalMatrix matrix,
			final double[] rhs,
			final double[] obstacle,
			final double[] initialGuess,
			final double omega,
			final int maxIterations,
			final double tolerance) {

		return solve(
				matrix.getLowerDiagonal(),
				matrix.getMainDiagonal(),
				matrix.getUpperDiagonal(),
				rhs,
				obstacle,
				initialGuess,
				omega,
				maxIterations,
				tolerance);
	}

	/**
	 * Solves a tridiagonal linear complementarity problem using projected SOR.
	 * <p>
	 * Starting from the provided initial guess, the method performs
	 * Gauss-Seidel-style updates, applies over-relaxation, and then projects
	 * each
	 * updated component onto the obstacle constraint. Before the iteration
	 * starts,
	 * the initial guess is projected onto the admissible region
	 * {@code x >= obstacle}.
	 * </p>
	 *
	 * <p>
	 * At each iteration, the method monitors the maximum absolute update size.
	 * If
	 * this quantity falls below the specified tolerance, the iteration is
	 * stopped
	 * early.
	 * </p>
	 *
	 * @param lower The lower diagonal of the tridiagonal system matrix.
	 * @param diag The main diagonal of the tridiagonal system matrix.
	 * @param upper The upper diagonal of the tridiagonal system matrix.
	 * @param rhs The right-hand side vector {@code b}.
	 * @param obstacle The obstacle vector defining the lower bound constraint
	 * 		{@code x >= obstacle}.
	 * @param initialGuess The initial iterate used to start the PSOR iteration.
	 * @param omega The relaxation parameter. Values in the interval
	 * 		{@code (0, 2)} are typically used in practice.
	 * @param maxIterations The maximum number of PSOR iterations.
	 * @param tolerance The stopping tolerance for the maximum absolute change
	 * 		between two consecutive iterates. If {@code tolerance <= 0}, the
	 * 		iteration runs for the full number of iterations.
	 * @return An approximate solution of the tridiagonal linear complementarity
	 *     problem.
	 * @throws ArithmeticException If a zero diagonal entry is encountered
	 *     during
	 * 		the iteration.
	 */
	public static double[] solve(
			final double[] lower,
			final double[] diag,
			final double[] upper,
			final double[] rhs,
			final double[] obstacle,
			final double[] initialGuess,
			final double omega,
			final int maxIterations,
			final double tolerance) {

		validateInputs(lower, diag, upper, rhs, obstacle, initialGuess);

		final int n = diag.length;
		final double[] x = initialGuess.clone();

		for (int i = 0; i < n; i++) {
			x[i] = Math.max(x[i], obstacle[i]);
		}

		for (int iter = 0; iter < maxIterations; iter++) {
			double maxChange = 0.0;

			for (int i = 0; i < n; i++) {
				final double aii = diag[i];
				if (aii == 0.0) {
					throw new ArithmeticException("Zero diagonal entry encountered at i=" + i);
				}

				final double left = i > 0 ? lower[i] * x[i - 1] : 0.0;
				final double right = i < n - 1 ? upper[i] * x[i + 1] : 0.0;

				final double gaussSeidelValue = (rhs[i] - left - right) / aii;
				final double relaxedValue = (1.0 - omega) * x[i] + omega * gaussSeidelValue;
				final double projectedValue = Math.max(obstacle[i], relaxedValue);

				maxChange = Math.max(maxChange, Math.abs(projectedValue - x[i]));
				x[i] = projectedValue;
			}

			if (tolerance > 0.0 && maxChange <= tolerance) {
				break;
			}
		}

		return x;
	}

	/**
	 * Computes the infinity norm of the complementarity residual for a
	 * candidate
	 * solution.
	 * <p>
	 * This is a convenience overload accepting the system matrix in
	 * {@link TridiagonalMatrix} form.
	 * </p>
	 *
	 * @param matrix The tridiagonal system matrix.
	 * @param rhs The right-hand side vector {@code b}.
	 * @param obstacle The obstacle vector.
	 * @param x The candidate solution vector.
	 * @return The infinity norm of the complementarity residual.
	 */
	public static double complementarityResidualInfNorm(
			final TridiagonalMatrix matrix,
			final double[] rhs,
			final double[] obstacle,
			final double[] x) {

		return complementarityResidualInfNorm(
				matrix.getLowerDiagonal(),
				matrix.getMainDiagonal(),
				matrix.getUpperDiagonal(),
				rhs,
				obstacle,
				x);
	}

	/**
	 * Computes the infinity norm of the complementarity residual for a
	 * candidate
	 * solution.
	 * <p>
	 * For each component, the residual combines:
	 * </p>
	 * <ul>
	 * <li>the primal feasibility violation {@code max(0, obstacle[i] -
	 * x[i])},</li>
	 * <li>the dual feasibility violation {@code max(0, rhs[i] - (A
	 * x)[i])},</li>
	 *   <li>the complementarity defect
	 *       {@code |(x[i] - obstacle[i]) * ((A x)[i] - rhs[i])|}.</li>
	 * </ul>
	 * <p>
	 * The method returns the maximum of these quantities over all components.
	 * A value close to zero indicates that the complementarity conditions are
	 * nearly satisfied.
	 * </p>
	 *
	 * @param lower The lower diagonal of the tridiagonal system matrix.
	 * @param diag The main diagonal of the tridiagonal system matrix.
	 * @param upper The upper diagonal of the tridiagonal system matrix.
	 * @param rhs The right-hand side vector {@code b}.
	 * @param obstacle The obstacle vector.
	 * @param x The candidate solution vector.
	 * @return The infinity norm of the complementarity residual.
	 */
	public static double complementarityResidualInfNorm(
			final double[] lower,
			final double[] diag,
			final double[] upper,
			final double[] rhs,
			final double[] obstacle,
			final double[] x) {

		validateInputs(lower, diag, upper, rhs, obstacle, x);

		double maxResidual = 0.0;
		for (int i = 0; i < diag.length; i++) {
			final double ax =
					(i > 0 ? lower[i] * x[i - 1] : 0.0)
					+ diag[i] * x[i]
							+ (i < diag.length - 1 ? upper[i] * x[i + 1] : 0.0);

			final double primalViolation = Math.max(0.0, obstacle[i] - x[i]);
			final double dualViolation = Math.max(0.0, rhs[i] - ax);
			final double slack = x[i] - obstacle[i];
			final double residual = Math.max(
					Math.max(primalViolation, dualViolation),
					Math.abs(slack * (ax - rhs[i])));

			maxResidual = Math.max(maxResidual, residual);
		}

		return maxResidual;
	}

	/**
	 * Validates that all input arrays are non-null, have positive length, and
	 * share the same dimension.
	 *
	 * @param lower The lower diagonal array.
	 * @param diag The main diagonal array.
	 * @param upper The upper diagonal array.
	 * @param rhs The right-hand side vector.
	 * @param obstacle The obstacle vector.
	 * @param initialGuess The initial guess or candidate solution vector.
	 * @throws IllegalArgumentException If an input array is {@code null}, if
	 *     the
	 * system dimension is zero, or if the array lengths are inconsistent.
	 */
	private static void validateInputs(
			final double[] lower,
			final double[] diag,
			final double[] upper,
			final double[] rhs,
			final double[] obstacle,
			final double[] initialGuess) {

		if (lower == null || diag == null || upper == null || rhs == null || obstacle == null || initialGuess == null) {
			throw new IllegalArgumentException("Input arrays must not be null.");
		}
		if (diag.length == 0) {
			throw new IllegalArgumentException("System dimension must be positive.");
		}
		if (lower.length != diag.length
				|| upper.length != diag.length
				|| rhs.length != diag.length
				|| obstacle.length != diag.length
				|| initialGuess.length != diag.length) {
			throw new IllegalArgumentException("All arrays must have the same length.");
		}
	}
}
