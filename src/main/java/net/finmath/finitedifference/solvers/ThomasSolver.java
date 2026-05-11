package net.finmath.finitedifference.solvers;

/**
 * Utility class providing an implementation of the Thomas algorithm
 * for solving a tridiagonal linear system.
 * <p>
 * The method solves a system of the form
 * <pre>
 * [ d0  u0   0   ...                ] [x0]   [r0]
 * [ l1  d1  u1   0   ...            ] [x1] = [r1]
 * [  0  l2  d2  u2   0   ...        ] [x2]   [r2]
 * [              ...                ] [...]  [...]
 * [          ... l(n-1) d(n-1)      ] [xn]   [rn]
 * </pre>
 * where:
 * <ul>
 * <li>{@code lower[i]} contains the sub-diagonal entry for row {@code i},</li>
 * <li>{@code diag[i]} contains the main diagonal entry for row {@code i},</li>
 * <li>{@code upper[i]} contains the super-diagonal entry for row {@code
 * i},</li>
 * <li>{@code rhs[i]} contains the right-hand side entry for row {@code i}.</li>
 * </ul>
 * <p>
 * By convention, {@code lower[0]} is unused and may be set to any value, and
 * {@code upper[n-1]} is unused by the backward substitution step.
 * <p>
 * This solver runs in linear time and is intended for non-singular tridiagonal
 * systems. No validation of input sizes or pivot values is performed.
 *
 * @author Alessandro Gnoatto
 */
public final class ThomasSolver {

	/**
	 * Creates no instances of this utility class.
	 */
	private ThomasSolver() {
	}

	/**
	 * Solves a tridiagonal linear system using the Thomas algorithm.
	 * <p>
	 * The input arrays represent the three diagonals of the tridiagonal matrix
	 * and the right-hand side vector. All arrays are expected to have identical
	 * length {@code n}, where {@code n >= 1}.
	 * <p>
	 * The algorithm consists of a forward elimination phase followed by a
	 * backward substitution phase.
	 * <p>
	 * Note: The caller must provide arrays of consistent length and a
	 * non-singular tridiagonal system.
	 *
	 * @param lower The lower diagonal of the system matrix. Entry {@code
	 *     lower[i]}
	 * represents the coefficient below the main diagonal in row {@code i}.
	 * 		The value {@code lower[0]} is not used.
	 * @param diag The main diagonal of the system matrix. Entry {@code diag[i]}
	 * 		represents the diagonal coefficient in row {@code i}.
	 * @param upper The upper diagonal of the system matrix. Entry {@code
	 *     upper[i]}
	 * represents the coefficient above the main diagonal in row {@code i}.
	 * 		The value {@code upper[n-1]} is not used in the final row.
	 * @param rhs The right-hand side vector of the linear system.
	 * @return The solution vector {@code x} satisfying the tridiagonal system.
	 */
	public static double[] solve(
			final double[] lower,
			final double[] diag,
			final double[] upper,
			final double[] rhs) {

		final int n = diag.length;

		final double[] cPrime = new double[n];
		final double[] dPrime = new double[n];
		final double[] x = new double[n];

		cPrime[0] = upper[0] / diag[0];
		dPrime[0] = rhs[0] / diag[0];

		for (int i = 1; i < n; i++) {
			final double denom = diag[i] - lower[i] * cPrime[i - 1];
			cPrime[i] = i < n - 1 ? upper[i] / denom : 0.0;
			dPrime[i] = (rhs[i] - lower[i] * dPrime[i - 1]) / denom;
		}

		x[n - 1] = dPrime[n - 1];
		for (int i = n - 2; i >= 0; i--) {
			x[i] = dPrime[i] - cPrime[i] * x[i + 1];
		}

		return x;
	}
}
