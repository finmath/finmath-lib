package net.finmath.finitedifference.solvers;

/**
 * Data container representing a tridiagonal matrix by its three diagonals.
 * <p>
 * A tridiagonal matrix has non-zero entries only on the main diagonal,
 * the first lower diagonal, and the first upper diagonal.
 * Instead of storing the full matrix, this class stores those three
 * diagonals in separate arrays.
 * <p>
 * For a matrix of dimension {@code n}:
 * <ul>
 * <li>{@code lower[i]} stores the entry below the main diagonal in row {@code
 * i},</li>
 *   <li>{@code diag[i]} stores the main diagonal entry in row {@code i},</li>
 * <li>{@code upper[i]} stores the entry above the main diagonal in row {@code
 * i}.</li>
 * </ul>
 * <p>
 * By convention, {@code lower[0]} and {@code upper[n-1]} are typically unused.
 *
 * @author Alessandro Gnoatto
 */
public class TridiagonalMatrix {

	/**
	 * The lower diagonal of the matrix.
	 * <p>
	 * Entry {@code lower[i]} represents the coefficient below the main
	 * diagonal in row {@code i}. The value {@code lower[0]} is typically
	 * unused.
	 */
	public final double[] lower;

	/**
	 * The main diagonal of the matrix.
	 * <p>
	 * Entry {@code diag[i]} represents the diagonal coefficient in row {@code
	 * i}.
	 */
	public final double[] diag;

	/**
	 * The upper diagonal of the matrix.
	 * <p>
	 * Entry {@code upper[i]} represents the coefficient above the main
	 * diagonal in row {@code i}. The value {@code upper[n-1]} is typically
	 * unused.
	 */
	public final double[] upper;

	/**
	 * Creates a tridiagonal matrix representation of dimension {@code n}.
	 * <p>
	 * All three diagonal arrays are initialized with length {@code n} and
	 * filled with zeros.
	 *
	 * @param n The dimension of the tridiagonal matrix.
	 */
	public TridiagonalMatrix(final int n) {
		this.lower = new double[n];
		this.diag = new double[n];
		this.upper = new double[n];
	}
}
