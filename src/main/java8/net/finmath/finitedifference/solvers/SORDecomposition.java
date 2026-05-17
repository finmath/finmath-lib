package net.finmath.finitedifference.solvers;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

/**
 * Performs a Successive Over-Relaxation (SOR) decomposition for solving linear
 * systems
 * of the form {@code A x = b}.
 *
 * <p>
 * The input matrix {@code A} is decomposed into its diagonal part {@code D},
 * strictly
 * lower triangular part {@code L}, and strictly upper triangular part {@code
 * U}. The
 * solver then applies an iterative SOR (Gauss-Seidel with relaxation) sweep to
 * approximate the solution.
 * </p>
 *
 * @author Enrico De Vecchi
 * @author Alessandro Gnoatto
 */
public class SORDecomposition {

	// Diagonal part of the matrix A
	/**
	 * The d.
	 */
	private final RealMatrix matrixD;

	// Strictly lower triangular part of the matrix A
	/**
	 * The l.
	 */
	private final RealMatrix matrixL;

	// Strictly upper triangular part of the matrix A
	/**
	 * The u.
	 */
	private final RealMatrix matrixU;

	// Relaxation factor omega
	/**
	 * The omega.
	 */
	private double omega;

	// Keep a reference copy of A for efficient SOR sweeps
	/**
	 * The a.
	 */
	private final double[][] a;

	/**
	 * Constructs the decomposition of the given matrix {@code A} into {@code
	 * D}, {@code L}, and {@code U}.
	 *
	 * @param matrixA The input square matrix to decompose.
	 */
	public SORDecomposition(final RealMatrix matrixA) {
		this.a = matrixA.getData();
		this.matrixD = MatrixUtils.createRealDiagonalMatrix(getDiagonal(matrixA));
		this.matrixL = lowerMatrix(matrixA);
		this.matrixU = upperMatrix(matrixA);
		this.omega = 1.0;
	}

	/**
	 * Returns the diagonal matrix {@code D} of the decomposition.
	 *
	 * @return The diagonal part of {@code A}.
	 */
	public RealMatrix getD() {
		return matrixD;
	}

	/**
	 * Returns the strictly lower triangular matrix {@code L} of the
	 * decomposition.
	 *
	 * @return The strictly lower triangular part of {@code A}.
	 */
	public RealMatrix getL() {
		return matrixL;
	}

	/**
	 * Returns the strictly upper triangular matrix {@code U} of the
	 * decomposition.
	 *
	 * @return The strictly upper triangular part of {@code A}.
	 */
	public RealMatrix getU() {
		return matrixU;
	}

	private double[] getDiagonal(final RealMatrix matrixM) {
		final double[] diag = new double[matrixM.getColumnDimension()];
		for (int i = 0; i < diag.length; i++) {
			diag[i] = matrixM.getEntry(i, i);
		}
		return diag;
	}

	private RealMatrix upperMatrix(final RealMatrix matrixM) {
		final int n = matrixM.getColumnDimension();
		final double[][] U = new double[n][n];
		for (int i = 0; i < n; i++) {
			for (int j = i + 1; j < n; j++) {
				U[i][j] = matrixM.getEntry(i, j);
			}
		}
		return MatrixUtils.createRealMatrix(U);
	}

	private RealMatrix lowerMatrix(final RealMatrix matrixM) {
		final int n = matrixM.getColumnDimension();
		final double[][] L = new double[n][n];
		for (int i = 1; i < n; i++) {
			for (int j = 0; j < i; j++) {
				L[i][j] = matrixM.getEntry(i, j);
			}
		}
		return MatrixUtils.createRealMatrix(L);
	}

	/**
	 * Performs a fixed number of SOR iterations to approximate the solution of
	 * {@code A x = b}.
	 *
	 * <p>
	 * This method is a backwards-compatible entry point which delegates to
	 * {@link #getSol(RealMatrix, RealMatrix, double, int, double)} with {@code
	 * tol = 0.0}.
	 * </p>
	 *
	 * @param x0   The initial guess for {@code x} (n x 1).
	 * @param b     The right-hand side vector {@code b} (n x 1).
	 * @param w The relaxation factor {@code omega} (typically {@code 0 < w <
	 *     2}).
	 * @param steps The number of iterations to perform.
	 * @return The approximate solution after the specified number of
	 *     iterations.
	 */
	public RealMatrix getSol(final RealMatrix x0, final RealMatrix b, final double w, final int steps) {
		// Backwards compatible entry point: perform a fixed number of
		// iterations.
		return getSol(x0, b, w, steps, 0.0);
	}

	/**
	 * Performs SOR (Gauss-Seidel with relaxation) to solve {@code A x = b}.
	 *
	 * <p>
	 * If {@code tol > 0.0}, the iteration stops early when the infinity norm of
	 * the residual
	 * satisfies {@code ||A x - b||_inf <= tol}. If {@code tol == 0.0}, the
	 * solver always runs
	 * {@code maxIters} iterations.
	 * </p>
	 *
	 * @param x0       Initial guess (n x 1).
	 * @param b        Right-hand side (n x 1).
	 * @param w Relaxation factor {@code omega} (typically {@code 0 < w < 2}).
	 * @param maxIters Maximum number of sweeps.
	 * @param tol Residual tolerance in infinity norm (set to 0 to disable early
	 *     stopping).
	 * @return Approximate solution.
	 */
	public RealMatrix getSol(
			final RealMatrix x0,
			final RealMatrix b,
			final double w,
			final int maxIters,
			final double tol) {

		final int n = b.getRowDimension();
		if (b.getColumnDimension() != 1 || x0.getColumnDimension() != 1) {
			throw new IllegalArgumentException("SOR expects column vectors (n x 1).");
		}
		if (x0.getRowDimension() != n) {
			throw new IllegalArgumentException("x_0 and b must have the same dimension.");
		}

		this.omega = w;

		final double[] x = x0.getColumn(0).clone();
		final double[] bb = b.getColumn(0);

		for (int iter = 0; iter < maxIters; iter++) {
			for (int i = 0; i < n; i++) {
				final double aii = a[i][i];
				if (aii == 0.0) {
					throw new ArithmeticException("Zero diagonal entry encountered at i=" + i);
				}

				double sigma = 0.0;

				// j < i uses updated x (Gauss-Seidel)
				for (int j = 0; j < i; j++) {
					sigma += a[i][j] * x[j];
				}
				// j > i uses previous x values (still in x[])
				for (int j = i + 1; j < n; j++) {
					sigma += a[i][j] * x[j];
				}

				final double xGs = (bb[i] - sigma) / aii;
				x[i] = (1.0 - w) * x[i] + w * xGs;
			}

			if (tol > 0.0) {
				final double resInf = residualInfNorm(x, bb);
				if (resInf <= tol) {
					break;
				}
			}
		}

		return MatrixUtils.createColumnRealMatrix(x);
	}

	private double residualInfNorm(final double[] x, final double[] b) {
		double max = 0.0;
		for (int i = 0; i < a.length; i++) {
			double ax = 0.0;
			for (int j = 0; j < a.length; j++) {
				ax += a[i][j] * x[j];
			}
			max = Math.max(max, Math.abs(ax - b[i]));
		}
		return max;
	}
}
