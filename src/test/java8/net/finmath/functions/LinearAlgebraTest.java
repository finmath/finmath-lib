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
	public void testSolveTikhonovViaNormalEquationsSingularFallback() {
		final double[][] matrix = new double[][] {
			{ 1.0, 1.0 }
		};
		final double[] rhs = new double[] { 2.0 };

		for(final LinearAlgebra.SolverBackend solverBackend : getAvailableSolverBackends()) {
			assertSingularTikhonovFallback(solverBackend, matrix, rhs);
		}
	}

	@Test
	public void testSolveTikhonovViaNormalEquationsIllConditionedFallback() {
		final double[][] matrix = new double[][] {
			{ 1.0, 0.0 },
			{ 0.0, 1E-8 }
		};
		final double[] rhs = new double[] { 1.0, 1E-8 };

		for(final LinearAlgebra.SolverBackend solverBackend : getAvailableSolverBackends()) {
			final double[] solution = LinearAlgebra.solveTikhonovViaNormalEquations(
					solverBackend,
					matrix,
					rhs,
					0.0);
			Assert.assertArrayEquals(
					"Ill-conditioned normal equation fallback using " + solverBackend,
					new double[] { 1.0, 1.0 },
					solution,
					1E-10);
		}
	}

	@Test
	public void testSolveTikhonovViaNormalEquationsValidatesInputs() {
		final double[][] matrix = new double[][] {
			{ 1.0 },
			{ 2.0 }
		};
		final double[] rhs = new double[] { 1.0, 2.0 };

		for(final LinearAlgebra.SolverBackend solverBackend : getAvailableSolverBackends()) {
			for(final double invalidLambda : new double[] { -1.0, Double.NaN, Double.POSITIVE_INFINITY }) {
				final IllegalArgumentException exception = Assertions.assertThrows(
						IllegalArgumentException.class,
						() -> LinearAlgebra.solveTikhonovViaNormalEquations(
								solverBackend,
								matrix,
								rhs,
								invalidLambda));
				Assert.assertEquals(
						"regularizationLambda must be finite and non-negative.",
						exception.getMessage());
			}

			final IllegalArgumentException exception = Assertions.assertThrows(
					IllegalArgumentException.class,
					() -> LinearAlgebra.solveTikhonovViaNormalEquations(
							solverBackend,
							matrix,
							new double[] { 1.0 },
							0.0));
			Assert.assertTrue(exception.getMessage().contains("Incompatible dimensions"));
		}
	}

	@Test
	public void testSolveLinearEquationBackendParity() {
		final double[][] matrix = new double[][] {
			{ 3.0, 1.0 },
			{ 1.0, 2.0 }
		};
		final double[] rhs = new double[] { 9.0, 8.0 };

		for(final LinearAlgebra.SolverBackend solverBackend : getAvailableSolverBackends()) {
			final double[] solution = LinearAlgebra.solveLinearEquation(solverBackend, matrix, rhs);
			Assert.assertArrayEquals(
					"Square solve using " + solverBackend,
					new double[] { 2.0, 3.0 },
					solution,
					1E-12);
		}
	}

	@Test
	public void testSolveRankDeficientLeastSquaresBackendParity() {
		final double[][] matrix = new double[][] {
			{ 1.0, 1.0 },
			{ 2.0, 2.0 },
			{ 3.0, 3.0 }
		};
		final double[] rhs = new double[] { 2.0, 4.0, 6.0 };

		for(final LinearAlgebra.SolverBackend solverBackend : getAvailableSolverBackends()) {
			final double[] solution = LinearAlgebra.solveLinearEquationLeastSquare(solverBackend, matrix, rhs);
			Assert.assertArrayEquals(
					"Rank-deficient least-square solve using " + solverBackend,
					new double[] { 1.0, 1.0 },
					solution,
					1E-10);
		}
	}

	@Test
	public void testSolveMatrixRightHandSideBackendParity() {
		final double[][] matrix = new double[][] {
			{ 1.0, 0.0 },
			{ 0.0, 2.0 },
			{ 1.0, 2.0 }
		};
		final double[][] rhs = new double[][] {
			{ 1.0, 2.0 },
			{ 2.0, 4.0 },
			{ 3.0, 6.0 }
		};

		for(final LinearAlgebra.SolverBackend solverBackend : getAvailableSolverBackends()) {
			final double[][] solution = LinearAlgebra.solveLinearEquationLeastSquare(solverBackend, matrix, rhs);
			Assert.assertArrayEquals(
					"First matrix-RHS solution row using " + solverBackend,
					new double[] { 1.0, 2.0 },
					solution[0],
					1E-12);
			Assert.assertArrayEquals(
					"Second matrix-RHS solution row using " + solverBackend,
					new double[] { 1.0, 2.0 },
					solution[1],
					1E-12);
		}
	}

	@Test
	public void testInvertBackendParity() {
		final double[][] matrix = new double[][] {
			{ 4.0, 1.0 },
			{ 2.0, 3.0 }
		};

		for(final LinearAlgebra.SolverBackend solverBackend : getAvailableSolverBackends()) {
			final double[][] inverse = LinearAlgebra.invert(solverBackend, matrix);
			Assert.assertArrayEquals(
					"First inverse row using " + solverBackend,
					new double[] { 0.3, -0.1 },
					inverse[0],
					1E-12);
			Assert.assertArrayEquals(
					"Second inverse row using " + solverBackend,
					new double[] { -0.2, 0.4 },
					inverse[1],
					1E-12);
		}
	}

	@Test
	public void testPseudoInverseBackendParity() {
		final double[][] matrix = new double[][] {
			{ 2.0, 0.0, 0.0 },
			{ 0.0, 4.0, 0.0 }
		};

		for(final LinearAlgebra.SolverBackend solverBackend : getAvailableSolverBackends()) {
			final double[][] pseudoInverse = LinearAlgebra.pseudoInverse(solverBackend, matrix);
			Assert.assertArrayEquals(
					"First pseudo-inverse row using " + solverBackend,
					new double[] { 0.5, 0.0 },
					pseudoInverse[0],
					1E-12);
			Assert.assertArrayEquals(
					"Second pseudo-inverse row using " + solverBackend,
					new double[] { 0.0, 0.25 },
					pseudoInverse[1],
					1E-12);
			Assert.assertArrayEquals(
					"Third pseudo-inverse row using " + solverBackend,
					new double[] { 0.0, 0.0 },
					pseudoInverse[2],
					1E-12);
		}
	}

	@Test
	public void testSolveLinearEquationSymmetricBackendParity() {
		final double[][] matrix = new double[][] {
			{ 4.0, 1.0 },
			{ 1.0, 3.0 }
		};
		final double[] rhs = new double[] { 6.0, 7.0 };

		for(final LinearAlgebra.SolverBackend solverBackend : getAvailableSolverBackends()) {
			final double[] solution = LinearAlgebra.solveLinearEquationSymmetric(solverBackend, matrix, rhs);
			Assert.assertArrayEquals(
					"Symmetric solve using " + solverBackend,
					new double[] { 1.0, 2.0 },
					solution,
					1E-12);
		}
	}

	@Test
	public void testSolveLinearEquationCholeskySolverFacadeBackendParity() {
		final double[][] matrix = new double[][] {
			{ 4.0, 1.0 },
			{ 1.0, 3.0 }
		};
		final double[] rhs = new double[] { 6.0, 7.0 };

		for(final LinearAlgebra.SolverBackend solverBackend : getAvailableSolverBackends()) {
			final LinearAlgebra.Solver solver = LinearAlgebra.getSolver(solverBackend);
			final double[] solution = solver.solveLinearEquationCholesky(matrix, rhs);
			Assert.assertArrayEquals(
					"Cholesky facade solve using " + solverBackend,
					new double[] { 1.0, 2.0 },
					solution,
					1E-12);
		}
	}

	@Test
	public void testSolveWideLeastSquaresBackendParity() {
		final double[][] matrix = new double[][] {
			{ 1.0, 0.0, 1.0 },
			{ 0.0, 1.0, 1.0 }
		};
		final double[] rhs = new double[] { 1.0, 1.0 };

		for(final LinearAlgebra.SolverBackend solverBackend : getAvailableSolverBackends()) {
			final double[] solution = LinearAlgebra.solveLinearEquationLeastSquare(solverBackend, matrix, rhs);
			Assert.assertArrayEquals(
					"Wide minimum-norm least-square solve using " + solverBackend,
					new double[] { 1.0 / 3.0, 1.0 / 3.0, 2.0 / 3.0 },
					solution,
					1E-12);
		}
	}

	@Test
	public void testJBlasAvailabilityMatchesExplicitBackend() {
		final boolean isJBlasAvailable = LinearAlgebra.isJBlasAvailable();

		if(isJBlasAvailable) {
			Assert.assertEquals(
					LinearAlgebra.SolverBackend.JBLAS,
					LinearAlgebra.getSolver(LinearAlgebra.SolverBackend.JBLAS).getSolverBackend());
			Assert.assertArrayEquals(
					new double[] { 2.0 },
					LinearAlgebra.solveLinearEquation(
							LinearAlgebra.SolverBackend.JBLAS,
							new double[][] { { 2.0 } },
							new double[] { 4.0 }),
					1E-12);
		}
		else {
			final IllegalArgumentException exception = Assertions.assertThrows(
					IllegalArgumentException.class,
					() -> LinearAlgebra.solveLinearEquation(
							LinearAlgebra.SolverBackend.JBLAS,
							new double[][] { { 2.0 } },
							new double[] { 4.0 }));
			Assert.assertEquals(
					"JBLAS backend requested, but jblas is not available.",
					exception.getMessage());
		}
	}

	private static LinearAlgebra.SolverBackend[] getAvailableSolverBackends() {
		/*
		 * Keep optional-backend detection separate from the test body. An
		 * IllegalArgumentException raised by a solve or assertion must fail the test.
		 */
		try {
			LinearAlgebra.getSolver(LinearAlgebra.SolverBackend.JBLAS);
			return new LinearAlgebra.SolverBackend[] {
				LinearAlgebra.SolverBackend.COMMONS_MATH,
				LinearAlgebra.SolverBackend.EJML,
				LinearAlgebra.SolverBackend.JBLAS
			};
		}
		catch(final IllegalArgumentException jblasUnavailable) {
			return new LinearAlgebra.SolverBackend[] {
				LinearAlgebra.SolverBackend.COMMONS_MATH,
				LinearAlgebra.SolverBackend.EJML
			};
		}
	}

	private static void assertSingularTikhonovFallback(
			final LinearAlgebra.SolverBackend solverBackend,
			final double[][] matrix,
			final double[] rhs) {

		final double[] solution = LinearAlgebra.solveTikhonovViaNormalEquations(solverBackend, matrix, rhs, 0.0);

		Assert.assertArrayEquals(
				"Singular normal equation fallback using " + solverBackend,
				new double[] { 1.0, 1.0 },
				solution,
				1E-12);
	}

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

		final int numberOfSteps = 5;
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

		final int numberOfSteps = 5;
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
