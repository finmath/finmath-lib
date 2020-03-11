/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 23.02.2004
 */

package net.finmath.functions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.QRDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;

/**
 * This class implements some methods from linear algebra (e.g. solution of a linear equation, PCA).
 *
 * It is basically a functional wrapper using either the Apache commons math or JBlas
 *
 * @author Christian Fries
 * @version 1.6
 */
public class LinearAlgebra {

	private static boolean isEigenvalueDecompositionViaSVD = Boolean.parseBoolean(System.getProperty("net.finmath.functions.LinearAlgebra.isEigenvalueDecompositionViaSVD","false"));
	private static boolean isSolverUseApacheCommonsMath;
	private static boolean isJBlasAvailable;

	static {
		// Default value is true, in which case we will NOT use jblas
		boolean isSolverUseApacheCommonsMath = Boolean.parseBoolean(System.getProperty("net.finmath.functions.LinearAlgebra.isUseApacheCommonsMath","true"));

		/*
		 * Check if jblas is available
		 */
		if(!isSolverUseApacheCommonsMath) {
			try {
				final double[] x = org.jblas.Solve.solve(new org.jblas.DoubleMatrix(2, 2, 1.0, 1.0, 0.0, 1.0), new org.jblas.DoubleMatrix(2, 1, 1.0, 1.0)).data;
				// The following should not happen.
				if(x[0] != 1.0 || x[1] != 0.0) {
					isJBlasAvailable = false;
				}
				else {
					isJBlasAvailable = true;
				}
			}
			catch(final java.lang.UnsatisfiedLinkError e) {
				isJBlasAvailable = false;
			}
		}

		if(!isJBlasAvailable) {
			isSolverUseApacheCommonsMath = true;
		}
		LinearAlgebra.isSolverUseApacheCommonsMath = isSolverUseApacheCommonsMath;
	}

	/**
	 * Find a solution of the linear equation A x = b where
	 * <ul>
	 * <li>A is an n x m - matrix given as double[n][m]</li>
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
		if(lambda == 0) {
			return solveLinearEquationLeastSquare(matrixA, b);
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


		//		return solveLinearEquationLeastSquare(matrixRegularized, bRegularized);
		final DecompositionSolver solver = new QRDecomposition(new Array2DRowRealMatrix(matrixRegularized, false)).getSolver();
		return solver.solve(new ArrayRealVector(bRegularized, false)).toArray();
	}

	/**
	 * Find a solution of the linear equation A x = b where
	 * <ul>
	 * <li>A is an n x m - matrix given as double[n][m]</li>
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
		if(lambda0 == 0 && lambda1 ==0 && lambda2 == 0) {
			return solveLinearEquationLeastSquare(matrixA, b);
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

		//		return solveLinearEquationLeastSquare(matrixRegularized, bRegularized);
		final DecompositionSolver solver = new QRDecomposition(new Array2DRowRealMatrix(matrixRegularized, false)).getSolver();
		return solver.solve(new ArrayRealVector(bRegularized, false)).toArray();
	}

	/**
	 * Find a solution of the linear equation A x = b where
	 * <ul>
	 * <li>A is an n x m - matrix given as double[n][m]</li>
	 * <li>b is an m - vector given as double[m],</li>
	 * <li>x is an n - vector given as double[n],</li>
	 * </ul>
	 *
	 * @param matrixA The matrix A (left hand side of the linear equation).
	 * @param b The vector (right hand of the linear equation).
	 * @return A solution x to A x = b.
	 */
	public static double[] solveLinearEquation(final double[][] matrixA, final double[] b) {

		if(isSolverUseApacheCommonsMath) {
			final Array2DRowRealMatrix matrix = new Array2DRowRealMatrix(matrixA);

			DecompositionSolver solver;
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
		else {
			return org.jblas.Solve.solve(new org.jblas.DoubleMatrix(matrixA), new org.jblas.DoubleMatrix(b)).data;

			// For use of colt:
			// cern.colt.matrix.linalg.Algebra linearAlgebra = new cern.colt.matrix.linalg.Algebra();
			// return linearAlgebra.solve(new DenseDoubleMatrix2D(A), linearAlgebra.transpose(new DenseDoubleMatrix2D(new double[][] { b }))).viewColumn(0).toArray();

			// For use of parallel colt:
			// return new cern.colt.matrix.tdouble.algo.decomposition.DenseDoubleLUDecomposition(new cern.colt.matrix.tdouble.impl.DenseDoubleMatrix2D(A)).solve(new cern.colt.matrix.tdouble.impl.DenseDoubleMatrix1D(b)).toArray();
		}
	}

	/**
	 * Find a solution of the linear equation A x = b where
	 * <ul>
	 * <li>A is an n x m - matrix given as double[n][m]</li>
	 * <li>b is an m - vector given as double[m],</li>
	 * <li>x is an n - vector given as double[n],</li>
	 * </ul>
	 *
	 * @param matrixA The matrix A (left hand side of the linear equation).
	 * @param b The vector (right hand of the linear equation).
	 * @return A solution x to A x = b.
	 */
	public static double[] solveLinearEquationSVD(final double[][] matrixA, final double[] b) {

		if(isSolverUseApacheCommonsMath) {
			final Array2DRowRealMatrix matrix = new Array2DRowRealMatrix(matrixA);

			// Using SVD - very slow
			final DecompositionSolver solver = new SingularValueDecomposition(matrix).getSolver();

			return solver.solve(new Array2DRowRealMatrix(b)).getColumn(0);
		}
		else {
			return org.jblas.Solve.solve(new org.jblas.DoubleMatrix(matrixA), new org.jblas.DoubleMatrix(b)).data;

			// For use of colt:
			// cern.colt.matrix.linalg.Algebra linearAlgebra = new cern.colt.matrix.linalg.Algebra();
			// return linearAlgebra.solve(new DenseDoubleMatrix2D(A), linearAlgebra.transpose(new DenseDoubleMatrix2D(new double[][] { b }))).viewColumn(0).toArray();

			// For use of parallel colt:
			// return new cern.colt.matrix.tdouble.algo.decomposition.DenseDoubleLUDecomposition(new cern.colt.matrix.tdouble.impl.DenseDoubleMatrix2D(A)).solve(new cern.colt.matrix.tdouble.impl.DenseDoubleMatrix1D(b)).toArray();
		}
	}
	/**
	 * Returns the inverse of a given matrix.
	 *
	 * @param matrix A matrix given as double[n][n].
	 * @return The inverse of the given matrix.
	 */
	public static double[][] invert(final double[][] matrix) {

		if(isSolverUseApacheCommonsMath) {
			// Use LU from common math
			final LUDecomposition lu = new LUDecomposition(new Array2DRowRealMatrix(matrix));
			final double[][] matrixInverse = lu.getSolver().getInverse().getData();

			return matrixInverse;
		}
		else {
			return org.jblas.Solve.pinv(new org.jblas.DoubleMatrix(matrix)).toArray2();
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
		if(isSolverUseApacheCommonsMath) {
			final DecompositionSolver solver = new LUDecomposition(new Array2DRowRealMatrix(matrix)).getSolver();
			return solver.solve(new Array2DRowRealMatrix(vector)).getColumn(0);
		}
		else {
			return org.jblas.Solve.solveSymmetric(new org.jblas.DoubleMatrix(matrix), new org.jblas.DoubleMatrix(vector)).data;
			/* To use the linear algebra package colt from cern.
			cern.colt.matrix.linalg.Algebra linearAlgebra = new cern.colt.matrix.linalg.Algebra();
			double[] x = linearAlgebra.solve(new DenseDoubleMatrix2D(A), linearAlgebra.transpose(new DenseDoubleMatrix2D(new double[][] { b }))).viewColumn(0).toArray();

			return x;
			 */
		}
	}

	/**
	 * Find a solution of the linear equation A x = b in the least square sense where
	 * <ul>
	 * <li>A is an n x m - matrix given as double[n][m]</li>
	 * <li>b is an m - vector given as double[m],</li>
	 * <li>x is an n - vector given as double[n],</li>
	 * </ul>
	 *
	 * @param matrix The matrix A (left hand side of the linear equation).
	 * @param vector The vector b (right hand of the linear equation).
	 * @return A solution x to A x = b.
	 */
	public static double[] solveLinearEquationLeastSquare(final double[][] matrix, final double[] vector) {
		// We use the linear algebra package apache commons math
		final DecompositionSolver solver = new SingularValueDecomposition(new Array2DRowRealMatrix(matrix, false)).getSolver();
		return solver.solve(new ArrayRealVector(vector)).toArray();
	}

	/**
	 * Find a solution of the linear equation A X = B in the least square sense where
	 * <ul>
	 * <li>A is an n x m - matrix given as double[n][m]</li>
	 * <li>B is an m x k - matrix given as double[m][k],</li>
	 * <li>X is an n x k - matrix given as double[n][k],</li>
	 * </ul>
	 *
	 * @param matrix The matrix A (left hand side of the linear equation).
	 * @param rhs The matrix B (right hand of the linear equation).
	 * @return A solution X to A X = B.
	 */
	public static double[][] solveLinearEquationLeastSquare(final double[][] matrix, final double[][] rhs) {
		// We use the linear algebra package apache commons math
		final DecompositionSolver solver = new SingularValueDecomposition(new Array2DRowRealMatrix(matrix, false)).getSolver();
		return solver.solve(new Array2DRowRealMatrix(rhs)).getData();
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
	public double[][] exp(final double[][] matrix) {
		return org.jblas.MatrixFunctions.expm(new org.jblas.DoubleMatrix(matrix)).toArray2();
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
	public RealMatrix exp(final RealMatrix matrix) {
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
		if(isSolverUseApacheCommonsMath) {
			// Use LU from common math
			final SingularValueDecomposition svd = new SingularValueDecomposition(new Array2DRowRealMatrix(matrix));
			final double[][] matrixInverse = svd.getSolver().getInverse().getData();

			return matrixInverse;
		}
		else {
			return org.jblas.Solve.pinv(new org.jblas.DoubleMatrix(matrix)).toArray2();
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
}
