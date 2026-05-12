package net.finmath.finitedifference.solvers;

import org.apache.commons.math3.linear.OpenMapRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;

/**
 * Builds finite difference matrices for approximating the first and second
 * derivatives of a function sampled on a (possibly) non-uniform grid.
 *
 * <p>
 * The matrices returned by this class are linear operators which, when applied
 * to a vector of function values {@code f(x_i)}, approximate {@code f'(x_i)}
 * and
 * {@code f''(x_i)} at the grid points {@code x_i}.
 * </p>
 *
 * <p>
 * The implementation uses
 * </p>
 * <ul>
 *   <li>forward differences at the first node,</li>
 *   <li>backward differences at the last node,</li>
 *   <li>central differences at internal nodes.</li>
 * </ul>
 *
 * @author Alessandro Gnoatto
 */
public class FiniteDifferenceMatrixBuilder {

	/**
	 * The x.
	 */
	private final double[] x; // Grid points
	/**
	 * The n.
	 */
	private final int n;

	/**
	 * The t1.
	 */
	private final RealMatrix matrixT1; // Matrix for the first derivative
	/**
	 * The t2.
	 */
	private final RealMatrix matrixT2; // Matrix for the second derivative

	/**
	 * Constructs a finite difference matrix builder for the given grid.
	 *
	 * @param x Array of grid points.
	 */
	public FiniteDifferenceMatrixBuilder(final double[] x) {

		this.x = x;
		this.n = x.length;

		matrixT1 = new OpenMapRealMatrix(n, n);
		matrixT2 = new OpenMapRealMatrix(n, n);

		buildMatrices();
	}

	/**
	 * Builds the finite difference matrices for first and second derivatives.
	 *
	 * <p>
	 * Uses:
	 * </p>
	 * <ul>
	 *   <li>Forward difference at the first node.</li>
	 *   <li>Backward difference at the last node.</li>
	 *   <li>Central difference for internal nodes.</li>
	 * </ul>
	 */
	private void buildMatrices() {

		for (int i = 0; i < n; i++) {

			if (i == 0) {
				// First node (forward difference, 2 points)
				// f'(x0) = [f(x1)-f(x0)]/[x1-x0]
				final double h1 = x[i + 1] - x[i];
				matrixT1.setEntry(i, i, -1.0 / h1);
				matrixT1.setEntry(i, i + 1, 1.0 / h1);

				// f''(x0) = A0*f(x0)+A1*f(x1)
				// A0 = -2/[(x1-x0)*(x2-x1)]
				// A1 = 2/[(x1-x0)*((x1-x0)+(x2-x1))]
				final double h2 = x[i + 2] - x[i + 1];

				matrixT2.setEntry(i, i + 1, 2.0 / (h1 * (h1 + h2)));
				matrixT2.setEntry(i, i, -2.0 / (h1 * h2));
			} else if (i == n - 1) {
				// Last node (backward difference, 2 points)
				// f'(xN) = [f(xN)-f(xN-1)]/[xN-xN-1]
				final double h0 = x[i] - x[i - 1];
				matrixT1.setEntry(i, i, 1.0 / h0);
				matrixT1.setEntry(i, i - 1, -1.0 / h0);

				// f''(xN) = B1*f(xN-1)+B2*f(xN)
				// B1 = 2/[(xN-xN-1)*((xN-1-xN-2)+(xN-xN-1))]
				// B2 = -2/[(xN-1-xN-2)(xN-xN-1)]
				final double h3 = x[i - 1] - x[i - 2];

				matrixT2.setEntry(i, i - 1, 2.0 / (h0 * (h0 + h3)));
				matrixT2.setEntry(i, i, -2.0 / (h3 * h0));
			} else {
				// Internal nodes (central difference, 3 points)
				final double h0 = x[i] - x[i - 1];
				final double h1 = x[i + 1] - x[i];

				// First derivative coefficients
				// f'(xi) = a*f(xi-1)+b*f(xi)+c*f(xi+1)
				// a = - h1 / (h0*(h1+h0))
				// b = (h1-h0) / (h1*h0)
				// c = h0 / (h1*(h0+h1)
				matrixT1.setEntry(i, i - 1, -h1 / (h0 * (h1 + h0)));
				matrixT1.setEntry(i, i, (h1 - h0) / (h1 * h0));
				matrixT1.setEntry(i, i + 1, h0 / (h1 * (h0 + h1)));

				// Second derivative coefficients
				// f''(xi) = d*f(xi-1)+e*f(xi)+f*f(xi+1)
				// d = 2 / [h0*(h0+h1)]
				// e = -2 / (h0*h1)
				// f = 2 / (h1*(h0+h1))
				matrixT2.setEntry(i, i - 1, 2.0 / (h0 * (h0 + h1)));
				matrixT2.setEntry(i, i, -2.0 / (h0 * h1));
				matrixT2.setEntry(i, i + 1, 2.0 / (h1 * (h0 + h1)));
			}
		}
	}

	/**
	 * Returns the finite difference matrix for the first derivative.
	 *
	 * @return The matrix representing the first derivative operator.
	 */
	public RealMatrix getFirstDerivativeMatrix() {
		return matrixT1;
	}

	/**
	 * Returns the finite difference matrix for the second derivative.
	 *
	 * @return The matrix representing the second derivative operator.
	 */
	public RealMatrix getSecondDerivativeMatrix() {
		return matrixT2;
	}
}
