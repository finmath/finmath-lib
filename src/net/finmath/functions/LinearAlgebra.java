/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 * 
 * Created on 23.02.2004
 */

package net.finmath.functions;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.EigenvalueDecomposition;

/**
 * This class implements some methods from linear algebra (e.g. solution of a linear equation, PCA).
 * The class uses standard libraries.
 * 
 * @author Christian Fries
 * @version 1.2
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
	 * Returns the matrix of the n Eigenvectors corresponding to the first n largest Eigenvalues of a correlation matrix.
	 * These eigenvectors can also be interpreted as "principal components" (i.e., the method implements the PCA).
	 * 
	 * @param correlationMatrix The given correlation matrix.
	 * @param numberOfFactors The requested number of factors (eigenvectors).
	 * @return Matrix of n Eigenvectors (columns) (matrix is given as double[n][numberOfFactors], where n is the number of rows of the correlationMatrix.
	 */
	public static double[][] getFactorMatrix(double[][] correlationMatrix, int numberOfFactors) {
		return getFactorMatrix(new DenseDoubleMatrix2D(correlationMatrix), numberOfFactors).toArray();
	}

	/**
	 * Returns a correlation matrix which has rank &lt; n and for which the first n factors agree with the factors of correlationMatrix.
	 * 
	 * @param correlationMatrix The given correlation matrix.
	 * @param numberOfFactors The requested number of factors (Eigenvectors).
	 * @return Factor reduced correlation matrix.
	 */
	public static double[][] factorReduction(double[][] correlationMatrix, int numberOfFactors) {
		return factorReduction(new DenseDoubleMatrix2D(correlationMatrix), numberOfFactors).toArray();
	}


	private static DoubleMatrix2D getFactorMatrix(DoubleMatrix2D correlationMatrix, int numberOfFactors) {
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

	public static DoubleMatrix2D factorReduction(DoubleMatrix2D correlationMatrix, int numberOfFactors) {

		// Extract factors corresponding to the largest eigenvalues
		DoubleMatrix2D factorMatrix = getFactorMatrix(correlationMatrix, numberOfFactors);

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
		
		return getFactorMatrix(reducedCorrelationMatrix, numberOfFactors);
	}
}
