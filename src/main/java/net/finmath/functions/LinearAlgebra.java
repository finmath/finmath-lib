/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 * 
 * Created on 23.02.2004
 */

package net.finmath.functions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.CholeskyDecomposition;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.SingularValueDecomposition;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.EigenvalueDecomposition;

/**
 * This class implements some methods from linear algebra (e.g. solution of a linear equation, PCA).
 * 
 * It is basically a functional wrapper using either the Colt library or Apache commons math.
 * 
 * I am currently preferring to use Colt, due to better performance in some situations, however it allows
 * to easily switch some parts to Apache commons math (this is the motivation for this class).
 * 
 * @author Christian Fries
 * @version 1.5
 */
public class LinearAlgebra {
	
	/**
	 * Find a solution of the linear equation A x = b where
	 * <ul>
	 * <li>A is an n x m - matrix given as double[n][m]</li>
	 * <li>b is an m - vector given as double[m],</li>
	 * <li>x is an n - vector given as double[n],</li>
	 * </ul>
	 * 
	 * @param A The matrix (left hand side of the linear equation).
	 * @param b The vector (right hand of the linear equation).
	 * @return A solution x to A x = b.
	 */
	public static double[] solveLinearEquation(double[][] A, double[] b) {

		// We use the linear algebra package from cern.
        cern.colt.matrix.linalg.Algebra linearAlgebra = new cern.colt.matrix.linalg.Algebra();
        double[] x = linearAlgebra.solve(new DenseDoubleMatrix2D(A), linearAlgebra.transpose(new DenseDoubleMatrix2D(new double[][] { b }))).viewColumn(0).toArray();

        return x;
	}
	
	/**
	 * Returns the inverse of a given matrix.
	 * 
	 * @param matrix A matrix given as double[n][n].
	 * @return The inverse of the given matrix.
	 */
	public static double[][] invert(double[][] matrix) {
		// We use the linear algebra package from cern.
		cern.colt.matrix.linalg.Algebra linearAlgebra = new cern.colt.matrix.linalg.Algebra();		
		double [][] matrixInverse = linearAlgebra.inverse(new DenseDoubleMatrix2D(matrix)).toArray();

		return matrixInverse;
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
		boolean  isUseApacheCommonsMath = true;
		if(isUseApacheCommonsMath) {
			// We use the linear algebra package apache commons math
			DecompositionSolver solver = new CholeskyDecomposition(new Array2DRowRealMatrix(matrix, false)).getSolver();
			return solver.solve(new ArrayRealVector(vector)).toArray();
		}
		else {
			return solveLinearEquation(matrix, vector);
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
		boolean  isUseApacheCommonsMath = true;
		if(isUseApacheCommonsMath) {
			/*
			 * Note: Commons math has convergence problems, where Colt does not.
			 */
			return getFactorMatrixUsingCommonsMath(correlationMatrix, numberOfFactors);
		}
		else {
			return getFactorMatrixUsingColt(new DenseDoubleMatrix2D(correlationMatrix), numberOfFactors).toArray();
		}
	}

	/**
	 * Returns a correlation matrix which has rank &lt; n and for which the first n factors agree with the factors of correlationMatrix.
	 * 
	 * @param correlationMatrix The given correlation matrix.
	 * @param numberOfFactors The requested number of factors (Eigenvectors).
	 * @return Factor reduced correlation matrix.
	 */
	public static double[][] factorReduction(double[][] correlationMatrix, int numberOfFactors) {
		boolean  isUseApacheCommonsMath = true;
		if(isUseApacheCommonsMath) {
			return factorReductionUsingCommonsMath(correlationMatrix, numberOfFactors);
		}
		else {
			return factorReductionUsingColt(new DenseDoubleMatrix2D(correlationMatrix), numberOfFactors).toArray();
		}
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
		EigenDecomposition eigenDecomp = new EigenDecomposition(new Array2DRowRealMatrix(correlationMatrix, false));
		double[]	eigenValues			= eigenDecomp.getRealEigenvalues();
		double[][]	eigenVectorMatrix	= eigenDecomp.getV().getData();

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
	 * Returns the matrix of the n Eigenvectors corresponding to the first n largest Eigenvalues of a correlation matrix.
	 * These eigenvectors can also be interpreted as "principal components" (i.e., the method implements the PCA).
	 * 
	 * @param correlationMatrix The given correlation matrix.
	 * @param numberOfFactors The requested number of factors (eigenvectors).
	 * @return Matrix of n Eigenvectors (columns) (matrix is given as double[n][numberOfFactors], where n is the number of rows of the correlationMatrix.
	 */
	private static DoubleMatrix2D getFactorMatrixUsingColt(DoubleMatrix2D correlationMatrix, int numberOfFactors) {
		/*
		 * Factor reduction
		 */
		// Create an eigen vector decomposition of the correlation matrix
		EigenvalueDecomposition eigenDecomp = new EigenvalueDecomposition(correlationMatrix);
		DoubleMatrix2D eigenVectorMatrix = eigenDecomp.getV();
		DoubleMatrix1D eigenValues = eigenDecomp.getRealEigenvalues();

		// Sort eigen vectors (will be sorted ascending)
		DenseDoubleMatrix2D eigenValuesSortMatrix = new DenseDoubleMatrix2D(eigenValues.size(), 2);
		for (int row = 0; row < eigenValues.size(); row++) {
			eigenValuesSortMatrix.set(row, 0, eigenValues.get(row));
			eigenValuesSortMatrix.set(row, 1, row);
		}

		// Extract factors corresponding to the largest eigenvalues
		DoubleMatrix2D factorMatrix = new DenseDoubleMatrix2D(eigenVectorMatrix.rows(), numberOfFactors);
		for (int factor = 0; factor < numberOfFactors; factor++) {
			double	eigenValue			= eigenValuesSortMatrix.get(eigenValuesSortMatrix.rows() - 1 - factor, 0);
			int		eigenVectorIndex	= (int) eigenValuesSortMatrix.get(eigenValuesSortMatrix.rows() - 1 - factor, 1);
			double	signChange			= eigenVectorMatrix.get(0, eigenVectorIndex) > 0 ? 1.0 : -1.0;		// Convention: Have first entry of eigenvector positive. This is to make results more consistent.
            double  eigenVectorNormSquared     = 0.0;
            for (int row = 0; row < eigenValuesSortMatrix.rows(); row++) {
                eigenVectorNormSquared += eigenVectorMatrix.get(row, eigenVectorIndex) * eigenVectorMatrix.get(row, eigenVectorIndex);
            }
            eigenValue = Math.max(eigenValue,0.0);
			for (int row = 0; row < eigenValuesSortMatrix.rows(); row++) {
				factorMatrix.set(row, factor, signChange * Math.sqrt(eigenValue/eigenVectorNormSquared) * eigenVectorMatrix.get(row, eigenVectorIndex));
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

		// Renormalized rows
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
	 * Returns a correlation matrix which has rank &lt; n and for which the first n factors agree with the factors of correlationMatrix.
	 * 
	 * @param correlationMatrix The given correlation matrix.
	 * @param numberOfFactors The requested number of factors (Eigenvectors).
	 * @return Factor reduced correlation matrix.
	 */
	public static DoubleMatrix2D factorReductionUsingColt(DoubleMatrix2D correlationMatrix, int numberOfFactors) {

		// Extract factors corresponding to the largest eigenvalues
		DoubleMatrix2D factorMatrix = getFactorMatrixUsingColt(correlationMatrix, numberOfFactors);

		// Renormalized rows
		for (int row = 0; row < factorMatrix.rows(); row++) {
			double sumSquared = 0;
			for (int factor = 0; factor < factorMatrix.columns(); factor++)
				sumSquared += factorMatrix.get(row, factor) * factorMatrix.get(row, factor);
			if(sumSquared != 0) {
			    for (int factor = 0; factor < factorMatrix.columns(); factor++)
					factorMatrix.set(row, factor, factorMatrix.get(row, factor) / Math.sqrt(sumSquared));
			}
			else {
			    // This is a rare case: The factor reduction of a completely decorrelated system to 1 factor
			    for (int factor = 0; factor < factorMatrix.columns(); factor++)
					factorMatrix.set(row, factor, 1.0);			    
			}
		}

		// Orthogonalized again
		cern.colt.matrix.linalg.Algebra alg = new cern.colt.matrix.linalg.Algebra();
		DoubleMatrix2D reducedCorrelationMatrix = alg.mult(factorMatrix, alg.transpose(factorMatrix));
		
		return getFactorMatrixUsingColt(reducedCorrelationMatrix, numberOfFactors);
	}
}
