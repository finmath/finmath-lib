/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 23.02.2004
 */

package net.finmath.functions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.CholeskyDecomposition;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.QRDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.factory.LinearSolverFactory_DDRM;
import org.ejml.interfaces.linsol.LinearSolverDense;

/**
 * This class implements some methods from linear algebra (e.g. solution of a linear equation, PCA).
 *
 * It is basically a functional wrapper using either Apache Commons Math, EJML or JBlas
 *
 * @author Christian Fries
 * @version 1.6
 */
public class LinearAlgebra {

	private static boolean isEigenvalueDecompositionViaSVD = Boolean.parseBoolean(System.getProperty("net.finmath.functions.LinearAlgebra.isEigenvalueDecompositionViaSVD","false"));

	public enum SolverBackend {
		COMMONS_MATH,
		EJML,
		JBLAS
	}

	private static final String PROPERTY_SOLVER_BACKEND = "net.finmath.functions.LinearAlgebra.solverBackend";
	private static final String PROPERTY_LEGACY_USE_COMMONS_MATH = "net.finmath.functions.LinearAlgebra.isUseApacheCommonsMath";

	/*
	 * The default backend used by the static methods is immutable after class
	 * initialization. This avoids a mutable global switch while preserving the
	 * existing static method signatures.
	 */
	private static final SolverBackend solverBackend;
	private static final boolean isSolverUseApacheCommonsMath;

	static {
		SolverBackend configuredSolverBackend = getConfiguredSolverBackend();

		/*
		 * Check if jblas is available only when it is requested, since loading
		 * jblas may fail on platforms without a compatible native library.
		 * If legacy configuration requested jblas but it is not available, fall
		 * back to the pure-Java EJML backend.
		 */
		if(configuredSolverBackend == SolverBackend.JBLAS) {
			if(!JBlasAvailabilityHolder.IS_AVAILABLE) {
				configuredSolverBackend = SolverBackend.EJML;
			}
		}

		solverBackend = configuredSolverBackend;

		/*
		 * Backward compatibility for methods that still use the old boolean.
		 * If the backend is EJML, those methods use Commons Math and must not
		 * accidentally fall through to jblas.
		 */
		isSolverUseApacheCommonsMath = solverBackend != SolverBackend.JBLAS;
	}

	public static boolean isEigenvalueDecompositionViaSVD() {
		return isEigenvalueDecompositionViaSVD;
	}

	/**
	 * Returns the legacy solver flag. This method is kept for backward compatibility
	 * and returns false only if the default backend is jblas. In particular, it
	 * returns true for EJML, since methods not converted to the backend enum
	 * continue to use Commons Math.
	 *
	 * @return True if the default solver backend is not jblas.
	 */
	public static boolean isSolverUseApacheCommonsMath() {
		return isSolverUseApacheCommonsMath;
	}

	public static boolean isJBlasAvailable() {
		return JBlasAvailabilityHolder.IS_AVAILABLE;
	}

	/**
	 * Returns the immutable default backend used by the static solve methods.
	 * The value is determined once during class initialization from system
	 * properties.
	 *
	 * @return The default solver backend.
	 */
	public static SolverBackend getSolverBackend() {
		return solverBackend;
	}

	/**
	 * Returns the immutable default backend used by the static solve methods.
	 *
	 * @return The default solver backend.
	 */
	public static SolverBackend getDefaultSolverBackend() {
		return solverBackend;
	}

	/**
	 * Creates a backend-specific, immutable solver instance. Use this if different
	 * parts of an application should use different solver backends without changing
	 * global static state.
	 *
	 * @param solverBackend The solver backend.
	 * @return A backend-specific solver instance.
	 */
	public static Solver getSolver(final SolverBackend solverBackend) {
		return new Solver(solverBackend);
	}

	/**
	 * Creates a backend-specific, immutable solver instance.
	 *
	 * @param solverBackend The solver backend.
	 * @return A backend-specific solver instance.
	 */
	public static Solver of(final SolverBackend solverBackend) {
		return getSolver(solverBackend);
	}

	/**
	 * Creates a solver instance using the default backend.
	 *
	 * @return A solver instance using the default backend.
	 */
	public static Solver getDefaultSolver() {
		return new Solver(solverBackend);
	}

	/**
	 * Immutable backend-specific solver facade. The instance methods delegate to
	 * static methods with an explicit backend parameter. This avoids mutable global
	 * state and allows different threads to use different solver backends safely.
	 */
	public static final class Solver {

		private final SolverBackend solverBackend;

		private Solver(final SolverBackend solverBackend) {
			this.solverBackend = requireSolverBackendAvailable(solverBackend);
		}

		public SolverBackend getSolverBackend() {
			return solverBackend;
		}

		public double[] solveLinearEquationTikonov(final double[][] matrixA, final double[] b, final double lambda) {
			return LinearAlgebra.solveLinearEquationTikonov(solverBackend, matrixA, b, lambda);
		}

		public double[] solveLinearEquationTikonov(
				final double[][] matrixA,
				final double[] b,
				final double lambda0,
				final double lambda1,
				final double lambda2) {
			return LinearAlgebra.solveLinearEquationTikonov(solverBackend, matrixA, b, lambda0, lambda1, lambda2);
		}

		public double[] solveLinearEquation(final double[][] matrixA, final double[] b) {
			return LinearAlgebra.solveLinearEquation(solverBackend, matrixA, b);
		}

		public double[] solveLinearEquationSVD(final double[][] matrixA, final double[] b) {
			return LinearAlgebra.solveLinearEquationSVD(solverBackend, matrixA, b);
		}

		public double[][] invert(final double[][] matrix) {
			return LinearAlgebra.invert(solverBackend, matrix);
		}

		public double[] solveLinearEquationSymmetric(final double[][] matrix, final double[] vector) {
			return LinearAlgebra.solveLinearEquationSymmetric(solverBackend, matrix, vector);
		}

		public double[] solveLinearEquationLeastSquare(final double[][] matrix, final double[] vector) {
			return LinearAlgebra.solveLinearEquationLeastSquare(solverBackend, matrix, vector);
		}

		public double[][] solveLinearEquationLeastSquare(final double[][] matrix, final double[][] rhs) {
			return LinearAlgebra.solveLinearEquationLeastSquare(solverBackend, matrix, rhs);
		}

		public double[][] pseudoInverse(final double[][] matrix) {
			return LinearAlgebra.pseudoInverse(solverBackend, matrix);
		}

		public double[] solveTikhonovViaNormalEquations(
				final double[][] matrix,
				final double[] rhs,
				final double regularizationLambda) {
			return LinearAlgebra.solveTikhonovViaNormalEquations(solverBackend, matrix, rhs, regularizationLambda);
		}

		public double[] solveLinearEquationCholesky(final double[][] matrix, final double[] rhs) {
			return LinearAlgebra.solveLinearEquationCholesky(solverBackend, matrix, rhs);
		}
	}

	private static SolverBackend requireSolverBackend(final SolverBackend solverBackend) {
		if(solverBackend == null) {
			throw new NullPointerException("solverBackend");
		}
		return solverBackend;
	}

	private static SolverBackend requireSolverBackendAvailable(final SolverBackend solverBackend) {
		requireSolverBackend(solverBackend);

		if(solverBackend == SolverBackend.JBLAS && !JBlasAvailabilityHolder.IS_AVAILABLE) {
			throw new IllegalArgumentException("JBLAS backend requested, but jblas is not available.");
		}

		return solverBackend;
	}

	private static SolverBackend getConfiguredSolverBackend() {
		final String configuredBackend = System.getProperty(PROPERTY_SOLVER_BACKEND);

		if(configuredBackend != null && !configuredBackend.trim().isEmpty()) {
			return parseSolverBackend(configuredBackend);
		}

		/*
		 * Backward compatibility with the old boolean property:
		 * true  -> COMMONS_MATH
		 * false -> JBLAS
		 */
		final boolean useCommonsMath = Boolean.parseBoolean(System.getProperty(PROPERTY_LEGACY_USE_COMMONS_MATH,"true"));
		return useCommonsMath ? SolverBackend.COMMONS_MATH : SolverBackend.JBLAS;
	}

	private static SolverBackend parseSolverBackend(final String configuredBackend) {
		final String normalizedName = configuredBackend
				.trim()
				.replace('-', '_')
				.replace(' ', '_')
				.toUpperCase(Locale.ENGLISH);

		if("COMMONS".equals(normalizedName)
				|| "APACHE".equals(normalizedName)
				|| "APACHE_COMMONS".equals(normalizedName)
				|| "APACHE_COMMONS_MATH".equals(normalizedName)) {
			return SolverBackend.COMMONS_MATH;
		}

		return SolverBackend.valueOf(normalizedName);
	}

	private static boolean checkJBlasAvailability() {
		try {
			final double[] x = org.jblas.Solve.solve(new org.jblas.DoubleMatrix(2, 2, 1.0, 1.0, 0.0, 1.0), new org.jblas.DoubleMatrix(2, 1, 1.0, 1.0)).data;
			return Math.abs(x[0] - 1.0) < 1E-12 && Math.abs(x[1]) < 1E-12;
		}
		catch(final RuntimeException | LinkageError exception) {
			return false;
		}
	}

	/*
	 * Initialization-on-demand holder. Commons Math and EJML use does not load the
	 * optional native jblas classes. The probe runs at most once when availability
	 * is queried, JBLAS is explicitly requested, or JBLAS is the configured default.
	 */
	private static final class JBlasAvailabilityHolder {
		private static final boolean IS_AVAILABLE = checkJBlasAvailability();

		private JBlasAvailabilityHolder() {
		}
	}

	/**
	 * Create a Cholesky decomposition of a symmetric matrix.
	 *
	 * @param symmetricMatrix The input matrix.
	 * @return A lower triangle matrix representing the CholeskyDecomposition.
	 */
	public static double[][] getCholeskyDecomposition(double[][] symmetricMatrix) {
		final CholeskyDecomposition decomposition = new CholeskyDecomposition(new Array2DRowRealMatrix(symmetricMatrix));

		final double[][] choleskyDecomposition = decomposition.getL().getData();

		return choleskyDecomposition;
	}

	/**
	 * Find a solution of the linear equation A x = b where
	 * <ul>
	 * <li>A is an m x n - matrix given as double[m][n]</li>
	 * <li>b is an m - vector given as double[m],</li>
	 * <li>x is an n - vector given as double[n],</li>
	 * </ul>
	 * using a standard Tikhonov regularization, i.e., we solve in the least square sense
	 *   A* x = b*
	 * where A* = (A^T, lambda I)^T and b* = (b^T , 0)^T.
	 *
	 * @param matrixA The matrix A (left hand side of the linear equation).
	 * @param b The vector (right hand of the linear equation).
	 * @param lambda The parameter lambda of the Tikhonov regularization. Lambda effectively measures which small numbers are considered zero.
	 * @return A solution x to A x = b.
	 */
	public static double[] solveLinearEquationTikonov(final double[][] matrixA, final double[] b, final double lambda) {
		return solveLinearEquationTikonov(solverBackend, matrixA, b, lambda);
	}

	public static double[] solveLinearEquationTikonov(
			final SolverBackend solverBackend,
			final double[][] matrixA,
			final double[] b,
			final double lambda) {
		requireSolverBackendAvailable(solverBackend);

		if(lambda == 0) {
			return solveLinearEquationLeastSquare(solverBackend, matrixA, b);
		}

		/*
		 * The copy of the array is inefficient, but the use cases for this method are currently limited.
		 * And SVD is an alternative to this method.
		 */
		final int rows = matrixA.length;
		final int cols = matrixA[0].length;
		final double[][] matrixRegularized = new double[rows+cols][cols];
		final double[] bRegularized = new double[rows+cols];					// Note the JVM initializes arrays to zero.
		for(int i=0; i<rows; i++) {
			System.arraycopy(matrixA[i], 0, matrixRegularized[i], 0, cols);
		}
		System.arraycopy(b, 0, bRegularized, 0, rows);

		for(int j=0; j<cols; j++) {
			final double[] matrixRow = matrixRegularized[rows+j];

			matrixRow[j] = lambda;
		}

		return solveLinearEquation(solverBackend, matrixRegularized, bRegularized);
	}

	/**
	 * Find a solution of the linear equation A x = b where
	 * <ul>
	 * <li>A is an m x n - matrix given as double[m][n]</li>
	 * <li>b is an m - vector given as double[m],</li>
	 * <li>x is an n - vector given as double[n],</li>
	 * </ul>
	 * using a Tikhonov regularization, i.e., we solve in the least square sense
	 *   A* x = b*
	 * where A* = (A^T, lambda0 I, lambda1 S, lambda2 C)^T and b* = (b^T , 0 , 0 , 0)^T.
	 *
	 * The matrix I is the identity matrix, effectively reducing the level of the solution vector.
	 * The matrix S is the first order central finite difference matrix with -lambda1 on the element [i][i-1] and +lambda1 on the element [i][i+1]
	 * The matrix C is the second order central finite difference matrix with -0.5 lambda2 on the element [i][i-1] and [i][i+1] and lambda2 on the element [i][i].
	 *
	 * @param matrixA The matrix A (left hand side of the linear equation).
	 * @param b The vector (right hand of the linear equation).
	 * @param lambda0 The parameter lambda0 of the Tikhonov regularization. Reduces the norm of the solution vector.
	 * @param lambda1 The parameter lambda1 of the Tikhonov regularization. Reduces the slope of the solution vector.
	 * @param lambda2 The parameter lambda1 of the Tikhonov regularization. Reduces the curvature of the solution vector.
	 * @return The solution x of the equation A* x = b*
	 */
	public static double[] solveLinearEquationTikonov(final double[][] matrixA, final double[] b, final double lambda0, final double lambda1, final double lambda2) {
		return solveLinearEquationTikonov(solverBackend, matrixA, b, lambda0, lambda1, lambda2);
	}

	public static double[] solveLinearEquationTikonov(
			final SolverBackend solverBackend,
			final double[][] matrixA,
			final double[] b,
			final double lambda0,
			final double lambda1,
			final double lambda2) {
		requireSolverBackendAvailable(solverBackend);

		if(lambda0 == 0 && lambda1 ==0 && lambda2 == 0) {
			return solveLinearEquationLeastSquare(solverBackend, matrixA, b);
		}

		/*
		 * The copy of the array is inefficient, but the use cases for this method are currently limited.
		 * And SVD is an alternative to this method.
		 */
		final int rows = matrixA.length;
		final int cols = matrixA[0].length;
		final double[][] matrixRegularized = new double[rows+3*cols][cols];
		final double[] bRegularized = new double[rows+3*cols];					// Note the JVM initializes arrays to zero.
		for(int i=0; i<rows; i++) {
			System.arraycopy(matrixA[i], 0, matrixRegularized[i], 0, cols);
		}
		System.arraycopy(b, 0, bRegularized, 0, rows);

		for(int j=0; j<cols; j++) {
			final double[] matrixRow = matrixRegularized[rows+0*cols+j];

			matrixRow[j] = lambda0;
		}

		for(int j=0; j<cols; j++) {
			final double[] matrixRow = matrixRegularized[rows+1*cols+j];

			matrixRow[j] = 0;
			if(j>0) {
				matrixRow[j-1] = lambda1;
			}
			if(j<cols-1) {
				matrixRow[j+1] = -lambda1;
			}
		}

		for(int j=0; j<cols; j++) {
			final double[] matrixRow = matrixRegularized[rows+2*cols+j];

			matrixRow[j] = lambda2;
			if(j>0) {
				matrixRow[j-1] = -0.5 * lambda2;
			}
			if(j<cols-1) {
				matrixRow[j+1] = -0.5 * lambda2;
			}
		}

		return solveLinearEquation(solverBackend, matrixRegularized, bRegularized);
	}

	/**
	 * Find a solution of the linear equation A x = b where
	 * <ul>
	 * <li>A is an m x n - matrix given as double[m][n]</li>
	 * <li>b is an m - vector given as double[m],</li>
	 * <li>x is an n - vector given as double[n],</li>
	 * </ul>
	 *
	 * @param matrixA The matrix A (left hand side of the linear equation).
	 * @param b The vector (right hand of the linear equation).
	 * @return A solution x to A x = b.
	 */
	public static double[] solveLinearEquation(final double[][] matrixA, final double[] b) {
		return solveLinearEquation(solverBackend, matrixA, b);
	}

	public static double[] solveLinearEquation(
			final SolverBackend solverBackend,
			final double[][] matrixA,
			final double[] b) {
		requireSolverBackendAvailable(solverBackend);

		switch(solverBackend) {
		case EJML: {
			final int numberOfColumns = checkMatrixAndVectorDimensions(matrixA, b);

			final LinearSolverDense<DMatrixRMaj> solverEJML;
			if(matrixA.length == numberOfColumns) {
				solverEJML = LinearSolverFactory_DDRM.linear(numberOfColumns);
			}
			else if(matrixA.length > numberOfColumns) {
				solverEJML = LinearSolverFactory_DDRM.leastSquares(matrixA.length, numberOfColumns);
			}
			else {
				solverEJML = LinearSolverFactory_DDRM.pseudoInverse(true);
			}

			return solveLinearEquationUsingEJML(
					matrixA,
					b,
					solverEJML,
					"Linear solve failed. Matrix is probably singular or ill-conditioned.");
		}

		case JBLAS: {
			final int numberOfColumns = checkMatrixAndVectorDimensions(matrixA, b);
			if(matrixA.length == numberOfColumns) {
				return org.jblas.Solve.solve(new org.jblas.DoubleMatrix(matrixA), new org.jblas.DoubleMatrix(b)).data;
			}
			else {
				return org.jblas.Solve.solveLeastSquares(new org.jblas.DoubleMatrix(matrixA), new org.jblas.DoubleMatrix(b)).data;
			}
		}

		case COMMONS_MATH:
		default: {
			final Array2DRowRealMatrix matrix = new Array2DRowRealMatrix(matrixA);

			final DecompositionSolver solver;
			if(matrix.getColumnDimension() == matrix.getRowDimension()) {
				solver = new LUDecomposition(matrix).getSolver();
			}
			else {
				solver = new QRDecomposition(new Array2DRowRealMatrix(matrixA)).getSolver();
			}

			// Using SVD - very slow
			//			solver = new SingularValueDecomposition(new Array2DRowRealMatrix(A)).getSolver();

			return solver.solve(new Array2DRowRealMatrix(b)).getColumn(0);
		}
		}
	}

	/**
	 * Find a solution of the linear equation A x = b where
	 * <ul>
	 * <li>A is an m x n - matrix given as double[m][n]</li>
	 * <li>b is an m - vector given as double[m],</li>
	 * <li>x is an n - vector given as double[n],</li>
	 * </ul>
	 *
	 * @param matrixA The matrix A (left hand side of the linear equation).
	 * @param b The vector (right hand of the linear equation).
	 * @return A solution x to A x = b.
	 */
	public static double[] solveLinearEquationSVD(final double[][] matrixA, final double[] b) {
		return solveLinearEquationSVD(solverBackend, matrixA, b);
	}

	public static double[] solveLinearEquationSVD(
			final SolverBackend solverBackend,
			final double[][] matrixA,
			final double[] b) {
		requireSolverBackendAvailable(solverBackend);

		switch(solverBackend) {
		case EJML: {
			checkMatrixAndVectorDimensions(matrixA, b);

			return solveLinearEquationUsingEJML(
					matrixA,
					b,
					LinearSolverFactory_DDRM.pseudoInverse(true),
					"SVD solve failed.");
		}

		case JBLAS: {
			return org.jblas.Solve.pinv(new org.jblas.DoubleMatrix(matrixA))
					.mmul(new org.jblas.DoubleMatrix(b)).data;
		}

		case COMMONS_MATH:
		default: {
			final Array2DRowRealMatrix matrix = new Array2DRowRealMatrix(matrixA);

			// Using SVD - very slow
			final DecompositionSolver solver = new SingularValueDecomposition(matrix).getSolver();

			return solver.solve(new Array2DRowRealMatrix(b)).getColumn(0);
		}
		}
	}
	/**
	 * Returns the inverse of a given matrix.
	 *
	 * @param matrix A matrix given as double[n][n].
	 * @return The inverse of the given matrix.
	 */
	public static double[][] invert(final double[][] matrix) {
		return invert(solverBackend, matrix);
	}

	public static double[][] invert(final SolverBackend solverBackend, final double[][] matrix) {
		requireSolverBackendAvailable(solverBackend);

		switch(solverBackend) {
		case EJML: {
			final int numberOfColumns = checkMatrixDimensions(matrix, "matrix");
			checkSquareMatrix(matrix, numberOfColumns);

			return invertUsingEJML(
					matrix,
					LinearSolverFactory_DDRM.linear(numberOfColumns),
					"Matrix inverse failed. Matrix is probably singular or ill-conditioned.");
		}

		case JBLAS: {
			return org.jblas.Solve.pinv(new org.jblas.DoubleMatrix(matrix)).toArray2();
		}

		case COMMONS_MATH:
		default: {
			// Use LU from common math
			final LUDecomposition lu = new LUDecomposition(new Array2DRowRealMatrix(matrix));
			final double[][] matrixInverse = lu.getSolver().getInverse().getData();

			return matrixInverse;
		}
		}
	}

	/**
	 * Find a solution of the linear equation A x = b where
	 * <ul>
	 * <li>A is an symmetric n x n - matrix given as double[n][n]</li>
	 * <li>b is an n - vector given as double[n],</li>
	 * <li>x is an n - vector given as double[n],</li>
	 * </ul>
	 *
	 * @param matrix The matrix A (left hand side of the linear equation).
	 * @param vector The vector b (right hand of the linear equation).
	 * @return A solution x to A x = b.
	 */
	public static double[] solveLinearEquationSymmetric(final double[][] matrix, final double[] vector) {
		return solveLinearEquationSymmetric(solverBackend, matrix, vector);
	}

	public static double[] solveLinearEquationSymmetric(
			final SolverBackend solverBackend,
			final double[][] matrix,
			final double[] vector) {
		requireSolverBackendAvailable(solverBackend);

		switch(solverBackend) {
		case EJML: {
			final int numberOfColumns = checkMatrixAndVectorDimensions(matrix, vector);
			checkSquareMatrix(matrix, numberOfColumns);

			return solveLinearEquationUsingEJML(
					matrix,
					vector,
					LinearSolverFactory_DDRM.linear(numberOfColumns),
					"Symmetric linear solve failed. Matrix is probably singular or ill-conditioned.");
		}

		case JBLAS: {
			return org.jblas.Solve.solveSymmetric(new org.jblas.DoubleMatrix(matrix), new org.jblas.DoubleMatrix(vector)).data;
		}

		case COMMONS_MATH:
		default: {
			final DecompositionSolver solver = new LUDecomposition(new Array2DRowRealMatrix(matrix)).getSolver();
			return solver.solve(new Array2DRowRealMatrix(vector)).getColumn(0);
		}
		}
	}

	/**
	 * Find a solution of the linear equation A x = b in the least square sense where
	 * <ul>
	 * <li>A is an m x n - matrix given as double[m][n]</li>
	 * <li>b is an m - vector given as double[m],</li>
	 * <li>x is an n - vector given as double[n],</li>
	 * </ul>
	 * If the least-squares solution is not unique, the solution with minimum
	 * Euclidean norm is returned.
	 *
	 * @param matrix The matrix A (left hand side of the linear equation).
	 * @param vector The vector b (right hand of the linear equation).
	 * @return A solution x to A x = b.
	 */
	public static double[] solveLinearEquationLeastSquare(final double[][] matrix, final double[] vector) {
		return solveLinearEquationLeastSquare(solverBackend, matrix, vector);
	}

	public static double[] solveLinearEquationLeastSquare(
			final SolverBackend solverBackend,
			final double[][] matrix,
			final double[] vector) {
		requireSolverBackendAvailable(solverBackend);

		switch(solverBackend) {
		case EJML: {
			checkMatrixAndVectorDimensions(matrix, vector);

			/*
			 * The previous Commons Math implementation uses SVD.
			 * Use EJML's SVD-based pseudo-inverse to preserve robust behavior.
			 */
			return solveLinearEquationUsingEJML(
					matrix,
					vector,
					LinearSolverFactory_DDRM.pseudoInverse(true),
					"Least-square solve failed.");
		}

		case JBLAS: {
			return org.jblas.Solve.pinv(new org.jblas.DoubleMatrix(matrix))
					.mmul(new org.jblas.DoubleMatrix(vector)).data;
		}

		case COMMONS_MATH:
		default: {
			final DecompositionSolver solver = new SingularValueDecomposition(new Array2DRowRealMatrix(matrix, false)).getSolver();
			return solver.solve(new ArrayRealVector(vector)).toArray();
		}
		}
	}

	/**
	 * Find a solution of the linear equation A X = B in the least square sense where
	 * <ul>
	 * <li>A is an m x n - matrix given as double[m][n]</li>
	 * <li>B is an m x k - matrix given as double[m][k],</li>
	 * <li>X is an n x k - matrix given as double[n][k],</li>
	 * </ul>
	 * If a least-squares solution is not unique, each right-hand side is solved
	 * using the minimum-Euclidean-norm solution.
	 *
	 * @param matrix The matrix A (left hand side of the linear equation).
	 * @param rhs The matrix B (right hand of the linear equation).
	 * @return A solution X to A X = B.
	 */
	public static double[][] solveLinearEquationLeastSquare(final double[][] matrix, final double[][] rhs) {
		return solveLinearEquationLeastSquare(solverBackend, matrix, rhs);
	}

	public static double[][] solveLinearEquationLeastSquare(
			final SolverBackend solverBackend,
			final double[][] matrix,
			final double[][] rhs) {
		requireSolverBackendAvailable(solverBackend);

		switch(solverBackend) {
		case EJML: {
			checkMatrixAndMatrixDimensions(matrix, rhs);

			/*
			 * The previous Commons Math implementation uses SVD.
			 * Use EJML's SVD-based pseudo-inverse to preserve robust behavior.
			 */
			return solveLinearEquationUsingEJML(
					matrix,
					rhs,
					LinearSolverFactory_DDRM.pseudoInverse(true),
					"Least-square solve failed.");
		}

		case JBLAS: {
			return org.jblas.Solve.pinv(new org.jblas.DoubleMatrix(matrix))
					.mmul(new org.jblas.DoubleMatrix(rhs)).toArray2();
		}

		case COMMONS_MATH:
		default: {
			final DecompositionSolver solver = new SingularValueDecomposition(new Array2DRowRealMatrix(matrix, false)).getSolver();
			return solver.solve(new Array2DRowRealMatrix(rhs)).getData();
		}
		}
	}

	/**
	 * Returns the matrix of the n Eigenvectors corresponding to the first n largest Eigenvalues of a correlation matrix.
	 * These Eigenvectors can also be interpreted as "principal components" (i.e., the method implements the PCA).
	 *
	 * @param correlationMatrix The given correlation matrix.
	 * @param numberOfFactors The requested number of factors (eigenvectors).
	 * @return Matrix of n Eigenvectors (columns) (matrix is given as double[n][numberOfFactors], where n is the number of rows of the correlationMatrix.
	 */
	public static double[][] getFactorMatrix(final double[][] correlationMatrix, final int numberOfFactors) {
		return getFactorMatrixUsingCommonsMath(correlationMatrix, numberOfFactors);
	}

	/**
	 * Returns a correlation matrix which has rank &lt; n and for which the first n factors agree with the factors of correlationMatrix.
	 *
	 * @param correlationMatrix The given correlation matrix.
	 * @param numberOfFactors The requested number of factors (Eigenvectors).
	 * @return Factor reduced correlation matrix.
	 */
	public static double[][] factorReduction(final double[][] correlationMatrix, final int numberOfFactors) {
		return factorReductionUsingCommonsMath(correlationMatrix, numberOfFactors);
	}

	/**
	 * Returns the matrix of the n Eigenvectors corresponding to the first n largest Eigenvalues of a correlation matrix.
	 * These eigenvectors can also be interpreted as "principal components" (i.e., the method implements the PCA).
	 *
	 * @param correlationMatrix The given correlation matrix.
	 * @param numberOfFactors The requested number of factors (Eigenvectors).
	 * @return Matrix of n Eigenvectors (columns) (matrix is given as double[n][numberOfFactors], where n is the number of rows of the correlationMatrix.
	 */
	private static double[][] getFactorMatrixUsingCommonsMath(final double[][] correlationMatrix, final int numberOfFactors) {
		/*
		 * Factor reduction
		 */
		// Create an eigen vector decomposition of the correlation matrix
		double[]	eigenValues;
		double[][]	eigenVectorMatrix;

		if(isEigenvalueDecompositionViaSVD) {
			final SingularValueDecomposition svd = new SingularValueDecomposition(new Array2DRowRealMatrix(correlationMatrix));
			eigenValues = svd.getSingularValues();
			eigenVectorMatrix = svd.getV().getData();
		}
		else {
			final EigenDecomposition eigenDecomp = new EigenDecomposition(new Array2DRowRealMatrix(correlationMatrix, false));
			eigenValues			= eigenDecomp.getRealEigenvalues();
			eigenVectorMatrix	= eigenDecomp.getV().getData();
		}

		class EigenValueIndex implements Comparable<EigenValueIndex> {
			private final int	index;
			private final Double value;

			EigenValueIndex(final int index, final double value) {
				this.index = index; this.value = value;
			}

			@Override
			public int compareTo(final EigenValueIndex o) { return o.value.compareTo(value); }
		}
		final List<EigenValueIndex> eigenValueIndices = new ArrayList<>();
		for(int i=0; i<eigenValues.length; i++) {
			eigenValueIndices.add(i,new EigenValueIndex(i,eigenValues[i]));
		}
		Collections.sort(eigenValueIndices);

		// Extract factors corresponding to the largest eigenvalues
		final double[][] factorMatrix = new double[eigenValues.length][numberOfFactors];
		for (int factor = 0; factor < numberOfFactors; factor++) {
			final int		eigenVectorIndex	= eigenValueIndices.get(factor).index;
			double	eigenValue			= eigenValues[eigenVectorIndex];
			final double	signChange			= eigenVectorMatrix[0][eigenVectorIndex] > 0.0 ? 1.0 : -1.0;		// Convention: Have first entry of eigenvector positive. This is to make results more consistent.
			double  eigenVectorNormSquared     = 0.0;
			for (int row = 0; row < eigenValues.length; row++) {
				eigenVectorNormSquared += eigenVectorMatrix[row][eigenVectorIndex] * eigenVectorMatrix[row][eigenVectorIndex];
			}
			eigenValue = Math.max(eigenValue,0.0);
			for (int row = 0; row < eigenValues.length; row++) {
				factorMatrix[row][factor] = signChange * Math.sqrt(eigenValue/eigenVectorNormSquared) * eigenVectorMatrix[row][eigenVectorIndex];
			}
		}

		return factorMatrix;
	}

	/**
	 * Returns a correlation matrix which has rank &lt; n and for which the first n factors agree with the factors of correlationMatrix.
	 *
	 * @param correlationMatrix The given correlation matrix.
	 * @param numberOfFactors The requested number of factors (Eigenvectors).
	 * @return Factor reduced correlation matrix.
	 */
	public static double[][] factorReductionUsingCommonsMath(final double[][] correlationMatrix, final int numberOfFactors) {

		// Extract factors corresponding to the largest eigenvalues
		final double[][] factorMatrix = getFactorMatrix(correlationMatrix, numberOfFactors);

		// Renormalize rows
		for (int row = 0; row < correlationMatrix.length; row++) {
			double sumSquared = 0;
			for (int factor = 0; factor < numberOfFactors; factor++) {
				sumSquared += factorMatrix[row][factor] * factorMatrix[row][factor];
			}
			if(sumSquared != 0) {
				for (int factor = 0; factor < numberOfFactors; factor++) {
					factorMatrix[row][factor] = factorMatrix[row][factor] / Math.sqrt(sumSquared);
				}
			}
			else {
				// This is a rare case: The factor reduction of a completely decorrelated system to 1 factor
				for (int factor = 0; factor < numberOfFactors; factor++) {
					factorMatrix[row][factor] = 1.0;
				}
			}
		}

		// Orthogonalized again
		final double[][] reducedCorrelationMatrix = (new Array2DRowRealMatrix(factorMatrix).multiply(new Array2DRowRealMatrix(factorMatrix).transpose())).getData();

		return getFactorMatrix(reducedCorrelationMatrix, numberOfFactors);
	}

	/**
	 * Calculate the "matrix exponential" (expm).
	 *
	 * Note: The function currently requires jblas. If jblas is not availabe on your system, an exception will be thrown.
	 * A future version of this function may implement a fall back.
	 *
	 * @param matrix The given matrix.
	 * @return The exp(matrix).
	 */
	public static double[][] exp(final double[][] matrix) {
		return org.jblas.MatrixFunctions.expm(new org.jblas.DoubleMatrix(matrix)).toArray2();
	}

	/**
	 * Calculate the power of a matrix
	 *
	 * Note: The function currently requires jblas. If jblas is not availabe on your system, an exception will be thrown.
	 * A future version of this function may implement a fall back.
	 *
	 * @param matrix The given matrix.
	 * @param exponent The exponent
	 * @return The pow(matrix, exponent).
	 */
	public static double[][] pow(final double[][] matrix, double exponent) {
		return org.jblas.MatrixFunctions.expm(org.jblas.MatrixFunctions.log(new org.jblas.DoubleMatrix(matrix).mul(exponent))).toArray2();
	}

	/**
	 * Calculate the "matrix exponential" (expm).
	 *
	 * Note: The function currently requires jblas. If jblas is not availabe on your system, an exception will be thrown.
	 * A future version of this function may implement a fall back.
	 *
	 * @param matrix The given matrix.
	 * @return The exp(matrix).
	 */
	public static RealMatrix exp(final RealMatrix matrix) {
		return new Array2DRowRealMatrix(exp(matrix.getData()));
	}

	/**
	 * Transpose a matrix
	 *
	 * @param matrix The given matrix.
	 * @return The transposed matrix.
	 */
	public static double[][] transpose(final double[][] matrix){

		//Get number of rows and columns of matrix
		final int numberOfRows = matrix.length;
		final int numberOfCols = matrix[0].length;

		//Instantiate a unitMatrix of dimension dim
		final double[][] transpose = new double[numberOfCols][numberOfRows];

		//Create unit matrix
		for(int rowIndex = 0; rowIndex < numberOfRows; rowIndex++) {
			for(int colIndex = 0; colIndex < numberOfCols; colIndex++) {
				transpose[colIndex][rowIndex] = matrix[rowIndex][colIndex];
			}
		}
		return transpose;
	}

	/**
	 * Pseudo-Inverse of a matrix calculated in the least square sense.
	 *
	 * @param matrix The given matrix A.
	 * @return pseudoInverse The pseudo-inverse matrix P, such that A*P*A = A and P*A*P = P
	 */
	public static double[][] pseudoInverse(final double[][] matrix){
		return pseudoInverse(solverBackend, matrix);
	}

	public static double[][] pseudoInverse(final SolverBackend solverBackend, final double[][] matrix){
		requireSolverBackendAvailable(solverBackend);

		switch(solverBackend) {
		case EJML: {
			return invertUsingEJML(
					matrix,
					LinearSolverFactory_DDRM.pseudoInverse(true),
					"Pseudo-inverse failed.");
		}

		case JBLAS: {
			return org.jblas.Solve.pinv(new org.jblas.DoubleMatrix(matrix)).toArray2();
		}

		case COMMONS_MATH:
		default: {
			// Use SVD from common math
			final SingularValueDecomposition svd = new SingularValueDecomposition(new Array2DRowRealMatrix(matrix));
			final double[][] matrixInverse = svd.getSolver().getInverse().getData();

			return matrixInverse;
		}
		}
	}

	/**
	 * Generates a diagonal matrix with the input vector on its diagonal
	 *
	 * @param vector The given matrix A.
	 * @return diagonalMatrix The matrix with the vectors entries on its diagonal
	 */
	public static double[][] diag(final double[] vector){

		// Note: According to the Java Language spec, an array is initialized with the default value, here 0.
		final double[][] diagonalMatrix = new double[vector.length][vector.length];

		for(int index = 0; index < vector.length; index++) {
			diagonalMatrix[index][index] = vector[index];
		}

		return diagonalMatrix;
	}

	/**
	 * Multiplication of two matrices.
	 *
	 * @param left The matrix A.
	 * @param right The matrix B
	 * @return product The matrix product of A*B (if suitable)
	 */
	public static double[][] multMatrices(final double[][] left, final double[][] right){
		return new Array2DRowRealMatrix(left).multiply(new Array2DRowRealMatrix(right)).getData();
	}

	/**
	 * Multiplication of matrix and vector. The vector array is interpreted as column vector.
	 *
	 * @param matrix The matrix A.
	 * @param vector The vector v
	 * @return product The matrix product of A*v (if suitable)
	 */
	public static double[] multMatrixVector(final double[][] matrix, final double[] vector){
		return new Array2DRowRealMatrix(matrix).multiply(new Array2DRowRealMatrix(vector)).getColumn(0);
	}

	/**
	 * Matrix power. Tries to calculate a matrix A such that M^{exponent} = A.
	 *
	 * @param matrix The matrix M of which we like to have the power.
	 * @param exponent The exponent.
	 * @return The exponent-th power of M
	 */
	public static double[][] matrixPow(double[][] matrix, double exponent) {
		return matrixExp(matrixLog(new Array2DRowRealMatrix(matrix)).scalarMultiply(exponent)).getData();
	}

	/**
	 * Matrix exponential. Tries to calculate the matrix A such that exp(M) = A.
	 *
	 * @param matrix The matrix M
	 * @return exp(M)
	 */
	public static double[][] matrixExp(double[][] matrix) {
		return matrixExp(new Array2DRowRealMatrix(matrix)).getData();
	}

	/**
	 * Matrix logarithm. Tries to calculate the matrix A such that log(M) = A.
	 *
	 * @param matrix The matrix M
	 * @return log(M)
	 */
	public static double[][] matrixLog(double[][] matrix) {
		return matrixLog(new Array2DRowRealMatrix(matrix)).getData();
	}

	/**
	 * Matrix Frobenius norm squared ||M||^2.
	 *
	 * @param matrix The matrix M
	 * @return The norm squared ||M||^2.
	 */
	public static double matrixNormFrobeniusSquared(final double[][] matrix) {
		/*
		 * Kahan summation on entry * entry
		 */
		double sum = 0.0;
		double error = 0.0;														// Running error compensation
		for(final double[] row : matrix) {
			for(final double entry : row) {
				final double value = entry * entry - error;		// Error corrected value
				final double newSum = sum + value;				// New sum
				error = (newSum - sum) - value;					// New numerical error
				sum	= newSum;
			}
		}
		return sum;
	}

	/**
	 * Matrix trace tr(M).
	 *
	 * @param matrix The matrix M
	 * @return The trace tr(M)
	 */
	public static double matrixTrace(final double[][] matrix) {
		/*
		 * Kahan summation on matrix[i][i]
		 */
		double sum = 0.0;
		double error = 0.0;														// Running error compensation
		for(int i = 0; i < Math.min(matrix.length, matrix[0].length); i++) {
			final double value = matrix[i][i] - error;		// Error corrected value
			final double newSum = sum + value;				// New sum
			error = (newSum - sum) - value;					// New numerical error
			sum	= newSum;
		}
		return sum;
	}

	/*
	 * There are better ways doing this - but this here is sufficient for some less crital purposes.
	 */

	private static RealMatrix matrixExp(RealMatrix matrix) {
		if(MatrixUtils.isSymmetric(matrix, 1E-10)) {
			// Symmetric matrix: try to use eigenvalue decomposition.
			final EigenDecomposition eigenDecomposition = new EigenDecomposition(matrix);
			final RealMatrix diag = eigenDecomposition.getD();
			for(int i=0; i<diag.getRowDimension(); i++) {
				diag.setEntry(i, i, Math.exp(diag.getEntry(i, i)));
			}
			return eigenDecomposition.getV().multiply(eigenDecomposition.getD()).multiply(eigenDecomposition.getVT());
		}
		else {
			RealMatrix exp = MatrixUtils.createRealIdentityMatrix(matrix.getRowDimension());
			double factor = 1.0;
			for(int k=1; k<15; k++) {
				factor = factor * k;
				exp = exp.add(matrix.power(k).scalarMultiply(1.0/factor));
			}
			return exp;
		}
	}

	private static RealMatrix matrixLog(RealMatrix matrix) {
		if(MatrixUtils.isSymmetric(matrix, 1E-10)) {
			// Symmetric matrix: try to use eigenvalue decomposition.
			final EigenDecomposition eigenDecomposition = new EigenDecomposition(matrix);
			final RealMatrix diag = eigenDecomposition.getD();
			for(int i=0; i<diag.getRowDimension(); i++) {
				diag.setEntry(i, i, Math.log(diag.getEntry(i, i)));
			}
			return eigenDecomposition.getV().multiply(eigenDecomposition.getD()).multiply(eigenDecomposition.getVT());
		}
		else {
			final RealMatrix m = matrix.subtract(MatrixUtils.createRealIdentityMatrix(matrix.getRowDimension()));
			RealMatrix log = m.copy();
			for(int k=2; k<15; k++) {
				log = log.add(m.power(k).scalarMultiply((k%2 == 0 ? -1.0 : 1.0)/k));
			}
			return log;
		}
	}


	private static double[] solveLinearEquationUsingEJML(
			final double[][] matrix,
			final double[] vector,
			final LinearSolverDense<DMatrixRMaj> solver,
			final String failureMessage) {

		final DMatrixRMaj matrixEJML = new DMatrixRMaj(matrix);
		final DMatrixRMaj vectorEJML = new DMatrixRMaj(vector.length, 1);
		for(int row = 0; row < vector.length; row++) {
			vectorEJML.set(row, 0, vector[row]);
		}
		final DMatrixRMaj solutionEJML = new DMatrixRMaj(matrixEJML.getNumCols(), 1);

		if(!solver.setA(matrixEJML)) {
			throw new ArithmeticException(failureMessage);
		}

		solver.solve(vectorEJML, solutionEJML);

		final double[] solution = new double[solutionEJML.getNumRows()];
		for(int row = 0; row < solution.length; row++) {
			solution[row] = solutionEJML.get(row, 0);
		}

		return solution;
	}

	private static double[][] solveLinearEquationUsingEJML(
			final double[][] matrix,
			final double[][] rhs,
			final LinearSolverDense<DMatrixRMaj> solver,
			final String failureMessage) {

		final DMatrixRMaj matrixEJML = new DMatrixRMaj(matrix);
		final DMatrixRMaj rhsEJML = new DMatrixRMaj(rhs);
		final DMatrixRMaj solutionEJML = new DMatrixRMaj(matrixEJML.getNumCols(), rhsEJML.getNumCols());

		if(!solver.setA(matrixEJML)) {
			throw new ArithmeticException(failureMessage);
		}

		solver.solve(rhsEJML, solutionEJML);

		final double[][] solution = new double[solutionEJML.getNumRows()][solutionEJML.getNumCols()];
		for(int row = 0; row < solution.length; row++) {
			for(int column = 0; column < solution[row].length; column++) {
				solution[row][column] = solutionEJML.get(row, column);
			}
		}

		return solution;
	}

	private static double[][] invertUsingEJML(
			final double[][] matrix,
			final LinearSolverDense<DMatrixRMaj> solver,
			final String failureMessage) {

		final DMatrixRMaj matrixEJML = new DMatrixRMaj(matrix);
		final DMatrixRMaj inverseEJML = new DMatrixRMaj(matrixEJML.getNumCols(), matrixEJML.getNumRows());

		if(!solver.setA(matrixEJML)) {
			throw new ArithmeticException(failureMessage);
		}

		solver.invert(inverseEJML);

		final double[][] inverse = new double[inverseEJML.getNumRows()][inverseEJML.getNumCols()];
		for(int row = 0; row < inverse.length; row++) {
			for(int column = 0; column < inverse[row].length; column++) {
				inverse[row][column] = inverseEJML.get(row, column);
			}
		}

		return inverse;
	}

	private static int checkMatrixAndVectorDimensions(final double[][] matrix, final double[] vector) {
		final int numberOfColumns = checkMatrixDimensions(matrix, "matrix");

		if(vector == null) {
			throw new NullPointerException("vector");
		}
		if(matrix.length != vector.length) {
			throw new IllegalArgumentException(
					"Incompatible dimensions: matrix has " + matrix.length
							+ " rows, vector has length " + vector.length + ".");
		}

		return numberOfColumns;
	}

	private static int checkMatrixAndMatrixDimensions(final double[][] matrix, final double[][] rhs) {
		final int numberOfColumns = checkMatrixDimensions(matrix, "matrix");
		checkMatrixDimensions(rhs, "rhs");

		if(matrix.length != rhs.length) {
			throw new IllegalArgumentException(
					"Incompatible dimensions: matrix has " + matrix.length
							+ " rows, rhs has " + rhs.length + " rows.");
		}

		return numberOfColumns;
	}

	private static int checkMatrixDimensions(final double[][] matrix, final String name) {
		if(matrix == null) {
			throw new NullPointerException(name);
		}
		if(matrix.length == 0) {
			throw new IllegalArgumentException("Matrix " + name + " has zero rows.");
		}
		if(matrix[0] == null) {
			throw new NullPointerException(name + "[0]");
		}

		final int numberOfColumns = matrix[0].length;

		if(numberOfColumns == 0) {
			throw new IllegalArgumentException("Matrix " + name + " has zero columns.");
		}

		for(int row = 1; row < matrix.length; row++) {
			if(matrix[row] == null) {
				throw new NullPointerException(name + "[" + row + "]");
			}
			if(matrix[row].length != numberOfColumns) {
				throw new IllegalArgumentException(
						"Matrix " + name + " is not rectangular: row 0 has length "
								+ numberOfColumns + ", row " + row + " has length "
								+ matrix[row].length + ".");
			}
		}

		return numberOfColumns;
	}

	private static void checkSquareMatrix(final double[][] matrix, final int numberOfColumns) {
		if(matrix.length != numberOfColumns) {
			throw new IllegalArgumentException(
					"Matrix is not square: " + matrix.length + " x " + numberOfColumns + ".");
		}
	}

	public static boolean matrixIsRowZero(double[][] matrix, int i) {
		for(double value : matrix[i]) {
			if(value != 0.0) return false;
		}
		return true;
	}

	public static boolean matrixIsColZero(double[][] matrix, int i) {
		for(int row = 0; row < matrix.length; row++) {
			if(matrix[row][i] != 0.0) return false;
		}
		return true;
	}

	/**
	 * Solves the Tikhonov-regularized normal equations
	 *
	 * <pre>
	 *     (B^T B + lambda I) x = B^T beta.
	 * </pre>
	 *
	 * The normal matrix is assembled explicitly and solved by Cholesky decomposition.
	 * Forming the unregularized normal matrix squares the spectral condition number
	 * of B. If Cholesky fails, this method therefore solves the original least-squares
	 * system for lambda = 0, or its augmented form for lambda &gt; 0, instead of applying
	 * an SVD to B<sup>T</sup> B.
	 *
	 * @param matrix The matrix B.
	 * @param rhs The right-hand side beta.
	 * @param regularizationLambda The finite, non-negative regularization parameter lambda.
	 * @return The Tikhonov-regularized solution x.
	 */
	public static double[] solveTikhonovViaNormalEquations(
			final double[][] matrix,
			final double[] rhs,
			final double regularizationLambda) {
		return solveTikhonovViaNormalEquations(solverBackend, matrix, rhs, regularizationLambda);
	}

	/**
	 * Solves the Tikhonov-regularized normal equations using an explicit backend.
	 * See {@link #solveTikhonovViaNormalEquations(double[][], double[], double)}
	 * for the equation and conditioning characteristics.
	 *
	 * @param solverBackend The solver backend.
	 * @param matrix The matrix B.
	 * @param rhs The right-hand side beta.
	 * @param regularizationLambda The finite, non-negative regularization parameter lambda.
	 * @return The Tikhonov-regularized solution x.
	 */
	public static double[] solveTikhonovViaNormalEquations(
			final SolverBackend solverBackend,
			final double[][] matrix,
			final double[] rhs,
			final double regularizationLambda) {
		requireSolverBackendAvailable(solverBackend);
		if(!Double.isFinite(regularizationLambda) || regularizationLambda < 0.0) {
			throw new IllegalArgumentException("regularizationLambda must be finite and non-negative.");
		}

		final int cols = checkMatrixAndVectorDimensions(matrix, rhs);
		final int rows = matrix.length;

		final double[][] normalMatrix = new double[cols][cols];
		final double[] normalRhs = new double[cols];

		/*
		 * normalMatrix = B^T B
		 * normalRhs    = B^T beta
		 */
		for(int row = 0; row < rows; row++) {
			final double[] matrixRow = matrix[row];
			final double rhsValue = rhs[row];

			for(int col1 = 0; col1 < cols; col1++) {
				final double value1 = matrixRow[col1];
				if(value1 == 0.0) {
					continue;
				}

				normalRhs[col1] += value1 * rhsValue;

				for(int col2 = 0; col2 <= col1; col2++) {
					normalMatrix[col1][col2] += value1 * matrixRow[col2];
				}
			}
		}

		/*
		 * Add lambda I and mirror the lower triangle.
		 */
		for(int col1 = 0; col1 < cols; col1++) {
			normalMatrix[col1][col1] += regularizationLambda;

			for(int col2 = 0; col2 < col1; col2++) {
				normalMatrix[col2][col1] = normalMatrix[col1][col2];
			}
		}

		try {
			return solveLinearEquationCholesky(solverBackend, normalMatrix, normalRhs);
		}
		catch(final RuntimeException choleskyFailed) {
			/*
			 * Avoid applying an SVD to the normal matrix, since that would retain
			 * the squared conditioning. Solve the original system, augmented with
			 * sqrt(lambda) I for regularized problems.
			 */
			if(regularizationLambda == 0.0) {
				return solveLinearEquationLeastSquare(solverBackend, matrix, rhs);
			}

			final double[][] augmentedMatrix = new double[rows + cols][cols];
			final double[] augmentedRhs = new double[rows + cols];
			for(int row = 0; row < rows; row++) {
				System.arraycopy(matrix[row], 0, augmentedMatrix[row], 0, cols);
				augmentedRhs[row] = rhs[row];
			}

			final double sqrtRegularizationLambda = Math.sqrt(regularizationLambda);
			for(int col = 0; col < cols; col++) {
				augmentedMatrix[rows + col][col] = sqrtRegularizationLambda;
			}

			return solveLinearEquationLeastSquare(solverBackend, augmentedMatrix, augmentedRhs);
		}
	}

	public static double[] solveLinearEquationCholesky(
			final double[][] matrix,
			final double[] rhs) {
		return solveLinearEquationCholesky(solverBackend, matrix, rhs);
	}

	public static double[] solveLinearEquationCholesky(
			final SolverBackend solverBackend,
			final double[][] matrix,
			final double[] rhs) {
		requireSolverBackendAvailable(solverBackend);

		switch(solverBackend) {
		case EJML: {
			final int numberOfColumns = checkMatrixAndVectorDimensions(matrix, rhs);
			checkSquareMatrix(matrix, numberOfColumns);

			return solveLinearEquationUsingEJML(
					matrix,
					rhs,
					LinearSolverFactory_DDRM.chol(matrix.length),
					"Cholesky decomposition failed. Matrix is probably not symmetric positive definite.");
		}

		case JBLAS: {
			return org.jblas.Solve.solvePositive(new org.jblas.DoubleMatrix(matrix), new org.jblas.DoubleMatrix(rhs)).data;
		}

		case COMMONS_MATH:
		default: {
			final DecompositionSolver solver =
					new CholeskyDecomposition(
							new Array2DRowRealMatrix(matrix, false),
							1.0E-12,
							1.0E-15)
					.getSolver();

			return solver.solve(new ArrayRealVector(rhs, false)).toArray();
		}
		}
	}

}
