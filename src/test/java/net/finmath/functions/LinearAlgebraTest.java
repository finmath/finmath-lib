/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 08.11.2018
 */

package net.finmath.functions;

import org.junit.Assert;
import org.junit.Test;

public class LinearAlgebraTest {

	@Test
	public void testSolveLinearEquationLeastSquarePseudoInverse() {
		double[][] A = new double[][] {
			{ -1.0, 2.0, 2.0 },
		};

		double[][] B = new double[][] {
			{ 1.0 },
		};

		double[][] X = LinearAlgebra.solveLinearEquationLeastSquare(A, B);

		Assert.assertEquals("Pseudo inverse", -1.0/9.0, X[0][0], 1E-12);
		Assert.assertEquals("Pseudo inverse",  2.0/9.0, X[1][0], 1E-12);
		Assert.assertEquals("Pseudo inverse",  2.0/9.0, X[2][0], 1E-12);
	}

	@Test
	public void testSolveLinearEquationLeastSquarePseudoInverse2() {
		double[][] A = new double[][] {
			{ 1, 0, 0, 0, 2 },
			{ 0, 0, 3, 0, 0 },
			{ 0, 0, 0, 0, 0 },
			{ 0, 4, 0, 0, 0 }
		};

		double[][] B = new double[][] {
			{ 1.0, 0.0, 0.0, 0.0 },
			{ 0.0, 1.0, 0.0, 0.0 },
			{ 0.0, 0.0, 1.0, 0.0 },
			{ 0.0, 0.0, 0.0, 1.0 }
		};

		double[][] X = LinearAlgebra.solveLinearEquationLeastSquare(A, B);

		Assert.assertEquals("Pseudo inverse", 1.0/5.0, X[0][0], 1E-12);
		Assert.assertEquals("Pseudo inverse", 1.0/4.0, X[1][3], 1E-12);
		Assert.assertEquals("Pseudo inverse", 1.0/3.0, X[2][1], 1E-12);
		Assert.assertEquals("Pseudo inverse", 2.0/5.0, X[4][0], 1E-12);
	}
}
