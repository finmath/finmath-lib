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
 * It is basically a functional wrapper using either the Colt library or Apache commons math.
 * 
 * I am currently preferring to use Colt, due to better performance in some situations, however it allows
 * to easily switch some parts to Apache commons math (this is the motivation for this class).
 * 
 * @author Christian Fries
 * @version 1.6
 */
public class LinearAlgebra {

	private static boolean isEigenvalueDecompositionViaSVD = Boolean.parseBoolean(System.getProperty("net.finmath.functions.LinearAlgebra.isEigenvalueDecompositionViaSVD","false"));
	private static boolean isSolverUseApacheCommonsMath;
	static {
		// Default value is true, in which case we will NOT use jblas
		boolean isSolverUseApacheCommonsMath = Boolean.parseBoolean(System.getProperty("net.finmath.functions.LinearAlgebra.isUseApacheCommonsMath","true"));

		/*
		 * Check if jblas is available
		 */
		if(!isSolverUseApacheCommonsMath) {
			try {
				double[] x = org.jblas.Solve.solve(new org.jblas.DoubleMatrix(2, 2, 1.0, 1.0, 0.0, 1.0), new org.jblas.DoubleMatrix(2, 1, 1.0, 1.0)).data;
				// The following should not happen.
				if(x[0] != 1.0 || x[1] != 0.0) isSolverUseApacheCommonsMath = true;
			}
			catch(java.lang.UnsatisfiedLinkError e) {
				isSolverUseApacheCommonsMath = true;
			}
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
	 * 
	 * @param matrixA The matrix A (left hand side of the linear equation).
	 * @param b The vector (right hand of the linear equation).
	 * @return A solution x to A x = b.
	 */
	public static double[] solveLinearEquation(double[][] matrixA, double[] b) {

		if(isSolverUseApacheCommonsMath) {
			Array2DRowRealMatrix matrix = new Array2DRowRealMatrix(matrixA);

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
	 * Returns the inverse of a given matrix.
	 * 
	 * @param matrix A matrix given as double[n][n].
	 * @return The inverse of the given matrix.
	 */
	public static double[][] invert(double[][] matrix) {

		if(isSolverUseApacheCommonsMath) {
			// Use LU from common math
			LUDecomposition lu = new LUDecomposition(new Array2DRowRealMatrix(matrix));
			double[][] matrixInverse = lu.getSolver().getInverse().getData();

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
	public static double[] solveLinearEquationSymmetric(double[][] matrix, double[] vector) {
		if(isSolverUseApacheCommonsMath) {
			DecompositionSolver solver = new LUDecomposition(new Array2DRowRealMatrix(matrix)).getSolver();			
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
	public static double[] solveLinearEquationLeastSquare(double[][] matrix, double[] vector) {
		// We use the linear algebra package apache commons math
		DecompositionSolver solver = new SingularValueDecomposition(new Array2DRowRealMatrix(matrix, false)).getSolver();
		return solver.solve(new ArrayRealVector(vector)).toArray();
	}

	/**
	 * Returns the matrix of the n Eigenvectors corresponding to the first n largest Eigenvalues of a correlation matrix.
	 * These Eigenvectors can also be interpreted as "principal components" (i.e., the method implements the PCA).
	 * 
	 * @param correlationMatrix The given correlation matrix.
	 * @param numberOfFactors The requested number of factors (eigenvectors).
	 * @return Matrix of n Eigenvectors (columns) (matrix is given as double[n][numberOfFactors], where n is the number of rows of the correlationMatrix.
	 */
	public static double[][] getFactorMatrix(double[][] correlationMatrix, int numberOfFactors) {
		return getFactorMatrixUsingCommonsMath(correlationMatrix, numberOfFactors);
	}

	/**
	 * Returns a correlation matrix which has rank &lt; n and for which the first n factors agree with the factors of correlationMatrix.
	 * 
	 * @param correlationMatrix The given correlation matrix.
	 * @param numberOfFactors The requested number of factors (Eigenvectors).
	 * @return Factor reduced correlation matrix.
	 */
	public static double[][] factorReduction(double[][] correlationMatrix, int numberOfFactors) {
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
	private static double[][] getFactorMatrixUsingCommonsMath(double[][] correlationMatrix, int numberOfFactors) {
		/*
		 * Factor reduction
		 */
		// Create an eigen vector decomposition of the correlation matrix
		double[]	eigenValues;
		double[][]	eigenVectorMatrix;

		if(isEigenvalueDecompositionViaSVD) {
			SingularValueDecomposition svd = new SingularValueDecomposition(new Array2DRowRealMatrix(correlationMatrix));
			eigenValues = svd.getSingularValues();
			eigenVectorMatrix = svd.getV().getData();
		}
		else {
			EigenDecomposition eigenDecomp = new EigenDecomposition(new Array2DRowRealMatrix(correlationMatrix, false));
			eigenValues			= eigenDecomp.getRealEigenvalues();
			eigenVectorMatrix	= eigenDecomp.getV().getData();
		}

		class EigenValueIndex implements Comparable<EigenValueIndex> {
			private int index;
			Double value;

			public EigenValueIndex(int index, double value) {
				this.index = index; this.value = value;
			}

			@Override
			public int compareTo(EigenValueIndex o) { return o.value.compareTo(value); }
		};
		List<EigenValueIndex> eigenValueIndices = new ArrayList<EigenValueIndex>();
		for(int i=0; i<eigenValues.length; i++) eigenValueIndices.add(i,new EigenValueIndex(i,eigenValues[i]));
		Collections.sort(eigenValueIndices);

		// Extract factors corresponding to the largest eigenvalues
		double[][] factorMatrix = new double[eigenValues.length][numberOfFactors];
		for (int factor = 0; factor < numberOfFactors; factor++) {
			int		eigenVectorIndex	= (int) eigenValueIndices.get(factor).index;
			double	eigenValue			= eigenValues[eigenVectorIndex];
			double	signChange			= eigenVectorMatrix[0][eigenVectorIndex] > 0.0 ? 1.0 : -1.0;		// Convention: Have first entry of eigenvector positive. This is to make results more consistent.
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
	public static double[][] factorReductionUsingCommonsMath(double[][] correlationMatrix, int numberOfFactors) {

		// Extract factors corresponding to the largest eigenvalues
		double[][] factorMatrix = getFactorMatrix(correlationMatrix, numberOfFactors);

		// Renormalize rows
		for (int row = 0; row < correlationMatrix.length; row++) {
			double sumSquared = 0;
			for (int factor = 0; factor < numberOfFactors; factor++)
				sumSquared += factorMatrix[row][factor] * factorMatrix[row][factor];
			if(sumSquared != 0) {
				for (int factor = 0; factor < numberOfFactors; factor++)
					factorMatrix[row][factor] = factorMatrix[row][factor] / Math.sqrt(sumSquared);
			}
			else {
				// This is a rare case: The factor reduction of a completely decorrelated system to 1 factor
				for (int factor = 0; factor < numberOfFactors; factor++)
					factorMatrix[row][factor] = 1.0;			    
			}
		}

		// Orthogonalized again
		double[][] reducedCorrelationMatrix = (new Array2DRowRealMatrix(factorMatrix).multiply(new Array2DRowRealMatrix(factorMatrix).transpose())).getData();

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
	public double[][] exp(double[][] matrix) {
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
	public RealMatrix exp(RealMatrix matrix) {
		return new Array2DRowRealMatrix(exp(matrix.getData()));
	}
}
