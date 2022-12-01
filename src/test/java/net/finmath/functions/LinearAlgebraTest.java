/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 08.11.2018
 */

package net.finmath.functions;

import org.apache.commons.math3.linear.MatrixUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

public class LinearAlgebraTest {

	@Test
	public void testSolveLinearEquationLeastSquarePseudoInverse0() {
		final double[][] A = new double[][] {
			{ -1.0, 2.0, 2.0 },
		};

		final double[] b = new double[] { 1.0 };

		final double[] x = LinearAlgebra.solveLinearEquationLeastSquare(A, b);

		Assert.assertEquals("Pseudo inverse", -1.0/9.0, x[0], 1E-12);
		Assert.assertEquals("Pseudo inverse",  2.0/9.0, x[1], 1E-12);
		Assert.assertEquals("Pseudo inverse",  2.0/9.0, x[2], 1E-12);
	}

	@Test
	public void testSolveLinearEquationLeastSquarePseudoInverse1() {
		final double[][] A = new double[][] {
			{ -1.0, 2.0, 2.0 },
		};

		final double[][] B = new double[][] {
			{ 1.0 },
		};

		final double[][] X = LinearAlgebra.solveLinearEquationLeastSquare(A, B);

		Assert.assertEquals("Pseudo inverse", -1.0/9.0, X[0][0], 1E-12);
		Assert.assertEquals("Pseudo inverse",  2.0/9.0, X[1][0], 1E-12);
		Assert.assertEquals("Pseudo inverse",  2.0/9.0, X[2][0], 1E-12);
	}

	@Test
	public void testSolveLinearEquationLeastSquarePseudoInverse2() {
		final double[][] A = new double[][] {
			{ 1, 0, 0, 0, 2 },
			{ 0, 0, 3, 0, 0 },
			{ 0, 0, 0, 0, 0 },
			{ 0, 4, 0, 0, 0 }
		};

		final double[][] B = new double[][] {
			{ 1.0, 0.0, 0.0, 0.0 },
			{ 0.0, 1.0, 0.0, 0.0 },
			{ 0.0, 0.0, 1.0, 0.0 },
			{ 0.0, 0.0, 0.0, 1.0 }
		};

		final double[][] X = LinearAlgebra.solveLinearEquationLeastSquare(A, B);

		Assert.assertEquals("Pseudo inverse", 1.0/5.0, X[0][0], 1E-12);
		Assert.assertEquals("Pseudo inverse", 1.0/4.0, X[1][3], 1E-12);
		Assert.assertEquals("Pseudo inverse", 1.0/3.0, X[2][1], 1E-12);
		Assert.assertEquals("Pseudo inverse", 2.0/5.0, X[4][0], 1E-12);

		Assert.assertEquals("Pseudo inverse", 0.0, X[0][1], 1E-12);
		Assert.assertEquals("Pseudo inverse", 0.0, X[0][2], 1E-12);
		Assert.assertEquals("Pseudo inverse", 0.0, X[0][3], 1E-12);
		Assert.assertEquals("Pseudo inverse", 0.0, X[1][0], 1E-12);
		Assert.assertEquals("Pseudo inverse", 0.0, X[1][1], 1E-12);
		Assert.assertEquals("Pseudo inverse", 0.0, X[1][2], 1E-12);
		Assert.assertEquals("Pseudo inverse", 0.0, X[2][0], 1E-12);
		Assert.assertEquals("Pseudo inverse", 0.0, X[2][2], 1E-12);
		Assert.assertEquals("Pseudo inverse", 0.0, X[2][3], 1E-12);
		Assert.assertEquals("Pseudo inverse", 0.0, X[3][0], 1E-12);
		Assert.assertEquals("Pseudo inverse", 0.0, X[3][1], 1E-12);
		Assert.assertEquals("Pseudo inverse", 0.0, X[3][2], 1E-12);
		Assert.assertEquals("Pseudo inverse", 0.0, X[3][3], 1E-12);
		Assert.assertEquals("Pseudo inverse", 0.0, X[4][1], 1E-12);
		Assert.assertEquals("Pseudo inverse", 0.0, X[4][2], 1E-12);
		Assert.assertEquals("Pseudo inverse", 0.0, X[4][3], 1E-12);
	}

	@Test
	public void testSolveLinearEquationLeastSquarePseudoInverse3() {
		final double[][] matrix = new double[][] {
			{ 1.0, 0.0 }, { 0.0, 1.0 }, { 0.0, 1.0 }
		};

		final double[][] pseudoInverse = LinearAlgebra.pseudoInverse(matrix);

		final double[][] product = LinearAlgebra.multMatrices(pseudoInverse, matrix);

		Assert.assertEquals("Pseudo inverse", 1.0, product[0][0], 1E-12);
		Assert.assertEquals("Pseudo inverse", 1.0, product[1][1], 1E-12);
		Assert.assertEquals("Pseudo inverse", 0.0, product[0][1], 1E-12);
		Assert.assertEquals("Pseudo inverse", 0.0, product[1][0], 1E-12);
	}

	@Test
	public void testMatrixPowerSymmetric() {
		final double[][] M = new double[][] {
			{ 1.0, 0.2, 0.0, 0.0 },
			{ 0.2, 1.2, 0.0, 0.0 },
			{ 0.0, 0.0, 0.9, 0.1 },
			{ 0.0, 0.0, 0.1, 1.0 }
		};
		
		int numberOfSteps = 5;
		final double[][] A = LinearAlgebra.matrixPow(M, 1.0/numberOfSteps);
		
		double[][] testMatrix = MatrixUtils.createRealIdentityMatrix(M.length).getData();
		for(int i=0; i<numberOfSteps; i++) {
			testMatrix = LinearAlgebra.multMatrices(testMatrix, A);
		}
		
		for(int i=0; i<M.length; i++) {
			Assertions.assertArrayEquals(M[i], testMatrix[i], 1E-9);
		}
	}

	@Test
	public void testMatrixPowerNonSymmetric() {
		final double[][] M = new double[][] {
			{ 1.0, 0.2, 0.0, 0.0 },
			{ 0.0, 1.2, 0.0, 0.0 },
			{ 0.0, 0.0, 0.9, 0.1 },
			{ 0.0, 0.0, 0.1, 1.0 }
		};
		
		int numberOfSteps = 5;
		final double[][] A = LinearAlgebra.matrixPow(M, 1.0/numberOfSteps);
		
		double[][] testMatrix = MatrixUtils.createRealIdentityMatrix(M.length).getData();
		for(int i=0; i<numberOfSteps; i++) {
			testMatrix = LinearAlgebra.multMatrices(testMatrix, A);
		}
		
		for(int i=0; i<M.length; i++) {
			Assertions.assertArrayEquals(M[i], testMatrix[i], 1E-9);
		}
	}
}
