/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 */

package net.finmath.montecarlo.automaticdifferentiation.forwardsensitivities;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.function.Executable;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.automaticdifferentiation.forwardsensitivities.ForwardSensitivities.ProjectedHedgeRatioResult;
import net.finmath.stochastic.RandomVariable;

public class ForwardSensitivitiesTest {

	@Test
	public void testPathwiseSolvePreservesSmallIndependentSensitivity() throws CalculationException {
		final Map<String, Long> parameterIDsByName = parameterIDs("factor-1", 1L, "factor-2", 2L);
		final Map<Long, RandomVariable> productGradient = new LinkedHashMap<>();
		productGradient.put(1L, randomVariable(1.0, 2.0, -3.0));
		productGradient.put(2L, randomVariable(4E-8, -5E-8, 6E-8));

		final List<Map<Long, RandomVariable>> hedgeGradients = new ArrayList<>();
		hedgeGradients.add(gradient(1L, deterministic(1.0)));
		hedgeGradients.add(gradient(2L, deterministic(1E-8)));

		final ProjectedHedgeRatioResult result = ForwardSensitivities.getHedgeRatiosPathwise(
				parameterIDsByName,
				0.0,
				productGradient,
				hedgeGradients,
				3);

		assertRealizations(new double[] { 1.0, 2.0, -3.0 }, result.getHedgeRatios()[0], 1E-12);
		assertRealizations(new double[] { 4.0, -5.0, 6.0 }, result.getHedgeRatios()[1], 1E-12);
	}

	@Test
	public void testPathwiseRankDeficientSystemUsesMinimumNormSolution() throws CalculationException {
		final Map<String, Long> parameterIDsByName = parameterIDs("factor-1", 1L, "factor-2", 2L);
		final Map<Long, RandomVariable> productGradient = new LinkedHashMap<>();
		productGradient.put(1L, deterministic(2.0));
		productGradient.put(2L, deterministic(4.0));

		final Map<Long, RandomVariable> repeatedHedgeGradient = new LinkedHashMap<>();
		repeatedHedgeGradient.put(1L, deterministic(1.0));
		repeatedHedgeGradient.put(2L, deterministic(2.0));

		final List<Map<Long, RandomVariable>> hedgeGradients = new ArrayList<>();
		hedgeGradients.add(repeatedHedgeGradient);
		hedgeGradients.add(new LinkedHashMap<>(repeatedHedgeGradient));

		final ProjectedHedgeRatioResult result = ForwardSensitivities.getHedgeRatiosPathwise(
				parameterIDsByName,
				0.0,
				productGradient,
				hedgeGradients,
				2);

		assertRealizations(new double[] { 1.0, 1.0 }, result.getHedgeRatios()[0], 1E-12);
		assertRealizations(new double[] { 1.0, 1.0 }, result.getHedgeRatios()[1], 1E-12);
	}

	@Test
	public void testPathwiseProjectionWithSmallDependentBasis() throws CalculationException {
		final Map<String, Long> parameterIDsByName = parameterIDs("factor", 1L);
		final Map<Long, RandomVariable> productGradient = gradient(
				1L,
				randomVariable(1.0, 3.0, 5.0, 7.0));

		final List<Map<Long, RandomVariable>> hedgeGradients = new ArrayList<>();
		hedgeGradients.add(gradient(1L, deterministic(1.0)));

		final ProjectedHedgeRatioResult rawResult = ForwardSensitivities.getHedgeRatiosPathwise(
				parameterIDsByName,
				0.0,
				productGradient,
				hedgeGradients,
				4);

		final RandomVariable state = randomVariable(-1E-8, -1E-8, 1E-8, 1E-8);
		final RandomVariable[] dependentProjectionBasis = new RandomVariable[] {
				deterministic(1E-8),
				state,
				state.mult(2.0)
		};
		final ProjectedHedgeRatioResult projectedResult = ForwardSensitivities.getHedgeRatiosPathwise(
				parameterIDsByName,
				0.0,
				productGradient,
				hedgeGradients,
				dependentProjectionBasis,
				4);

		assertRealizations(new double[] { 1.0, 3.0, 5.0, 7.0 }, rawResult.getHedgeRatios()[0], 1E-12);
		assertRealizations(new double[] { 2.0, 2.0, 6.0, 6.0 }, projectedResult.getHedgeRatios()[0], 1E-12);
	}

	@Test
	public void testSingleHedgePathwiseSolveHandlesExtremeScales() throws CalculationException {
		final Map<String, Long> parameterIDsByName = parameterIDs("factor-1", 1L, "factor-2", 2L);
		final Map<Long, RandomVariable> productGradient = new LinkedHashMap<>();
		productGradient.put(1L, randomVariable(3E-200, 3E200));
		productGradient.put(2L, randomVariable(6E-200, 6E200));

		final Map<Long, RandomVariable> hedgeGradient = new LinkedHashMap<>();
		hedgeGradient.put(1L, randomVariable(1E-200, 1E200));
		hedgeGradient.put(2L, randomVariable(2E-200, 2E200));

		final ProjectedHedgeRatioResult result = ForwardSensitivities.getHedgeRatiosPathwise(
				parameterIDsByName,
				0.0,
				productGradient,
				hedgeGradients(hedgeGradient),
				2);

		assertRealizations(new double[] { 3.0, 3.0 }, result.getHedgeRatios()[0], 1E-12);
	}

	@Test
	public void testSingleHedgePathwiseSolveHandlesZeroColumn() throws CalculationException {
		final Map<String, Long> parameterIDsByName = parameterIDs("factor", 1L);
		final Map<Long, RandomVariable> productGradient = gradient(1L, randomVariable(1.0, 2.0));
		final Map<Long, RandomVariable> hedgeGradient = gradient(1L, deterministic(0.0));

		final ProjectedHedgeRatioResult result = ForwardSensitivities.getHedgeRatiosPathwise(
				parameterIDsByName,
				0.0,
				productGradient,
				hedgeGradients(hedgeGradient),
				2);

		assertRealizations(new double[] { 0.0, 0.0 }, result.getHedgeRatios()[0], 0.0);
	}

	@Test
	public void testScaledNormalSystemPreservesSmallIndependentDirection() throws CalculationException {
		final double[] solution = ForwardSensitivities.solveReducedSystem(
				new double[][] {
					{ 1.0, 0.0 },
					{ 0.0, 1E-16 }
				},
				new double[] { 1.0, 1E-16 },
				0.0,
				true);

		Assert.assertArrayEquals(new double[] { 1.0, 1.0 }, solution, 1E-12);
	}

	@Test
	public void testScaledNormalSystemAppliesRegularizationInOriginalCoordinates() throws CalculationException {
		final double[] solution = ForwardSensitivities.solveReducedSystem(
				new double[][] {
					{ 1.0, 0.0 },
					{ 0.0, 1E-16 }
				},
				new double[] { 1.0, 1E-16 },
				1E-16,
				true);

		Assert.assertArrayEquals(new double[] { 1.0, 0.5 }, solution, 1E-12);
	}

	@Test
	public void testRankDeficientNormalSystemKeepsUnscaledMinimumNorm() throws CalculationException {
		final double[] solution = ForwardSensitivities.solveReducedSystem(
				new double[][] {
					{ 1.0, 1.0 },
					{ 1.0, 1.0 }
				},
				new double[] { 2.0, 2.0 },
				0.0,
				true);

		Assert.assertArrayEquals(new double[] { 1.0, 1.0 }, solution, 1E-12);
	}

	@Test
	public void testL2ScaledHedgeColumnsPreserveSmallIndependentDirection() throws CalculationException {
		final Map<String, Long> parameterIDsByName = parameterIDs("factor-1", 1L, "factor-2", 2L);
		final Map<Long, RandomVariable> productGradient = new LinkedHashMap<>();
		productGradient.put(1L, deterministic(1.0));
		productGradient.put(2L, deterministic(1E-8));

		final List<Map<Long, RandomVariable>> hedgeGradients = new ArrayList<>();
		hedgeGradients.add(gradient(1L, deterministic(1.0)));
		hedgeGradients.add(gradient(2L, deterministic(1E-8)));

		final ProjectedHedgeRatioResult result = ForwardSensitivities.getHedgeRatios(
				parameterIDsByName,
				0.0,
				productGradient,
				hedgeGradients,
				new RandomVariable[] { deterministic(1.0) },
				null,
				0.0,
				ForwardSensitivities.ReductionMethod.L2,
				2);

		Assert.assertArrayEquals(new double[] { 1.0 }, result.getCoefficients()[0], 1E-12);
		Assert.assertArrayEquals(new double[] { 1.0 }, result.getCoefficients()[1], 1E-12);
	}

	@Test
	public void testNearDependentPivotedPathwiseSystemMatchesOriginalMatrixSVD() throws CalculationException {
		final int numberOfRiskFactors = 8;
		final int numberOfHedges = 5;
		final double[][] matrix = new double[numberOfRiskFactors][numberOfHedges];
		final Random random = new Random(314159L);
		for(int row = 0; row < numberOfRiskFactors; row++) {
			for(int column = 0; column < numberOfHedges - 1; column++) {
				matrix[row][column] = random.nextGaussian();
			}
			matrix[row][numberOfHedges - 1] = matrix[row][0]
					+ 0.5 * matrix[row][1]
					+ 1E-8 * random.nextGaussian();
		}

		final double[] rhs = new double[numberOfRiskFactors];
		for(int row = 0; row < numberOfRiskFactors; row++) {
			for(int column = 0; column < numberOfHedges; column++) {
				rhs[row] += matrix[row][column] * (column + 1.0);
			}
		}

		final Map<String, Long> parameterIDsByName = new LinkedHashMap<>();
		final Map<Long, RandomVariable> productGradient = new LinkedHashMap<>();
		final List<Map<Long, RandomVariable>> hedgeGradients = new ArrayList<>();
		for(int column = 0; column < numberOfHedges; column++) {
			hedgeGradients.add(new LinkedHashMap<Long, RandomVariable>());
		}
		for(int row = 0; row < numberOfRiskFactors; row++) {
			final Long parameterID = Long.valueOf(row + 1L);
			parameterIDsByName.put("factor-" + row, parameterID);
			productGradient.put(parameterID, deterministic(rhs[row]));
			for(int column = 0; column < numberOfHedges; column++) {
				hedgeGradients.get(column).put(parameterID, deterministic(matrix[row][column]));
			}
		}

		final ProjectedHedgeRatioResult result = ForwardSensitivities.getHedgeRatiosPathwise(
				parameterIDsByName,
				0.0,
				productGradient,
				hedgeGradients,
				1);
		final double[] actual = new double[numberOfHedges];
		for(int hedgeIndex = 0; hedgeIndex < numberOfHedges; hedgeIndex++) {
			actual[hedgeIndex] = result.getHedgeRatios()[hedgeIndex].doubleValue();
		}

		final double[] expected = net.finmath.functions.LinearAlgebra.solveLinearEquationLeastSquare(matrix, rhs);
		/*
		 * At condition number O(1E8), stable QR and SVD may differ by a few
		 * multiples of eps*kappa. The defective single-sweep implementation this
		 * guards against differs by O(1E-1).
		 */
		Assert.assertArrayEquals(expected, actual, 1E-6);
	}

	@Test
	public void testGradientOverloadValidatesCommonInputs() {
		final Map<String, Long> parameterIDsByName = parameterIDs("factor", 1L);
		final Map<Long, RandomVariable> productGradient = gradient(1L, deterministic(2.0));
		final List<Map<Long, RandomVariable>> hedgeGradients = hedgeGradients(gradient(1L, deterministic(1.0)));
		final RandomVariable[] basisFunctions = new RandomVariable[] { deterministic(1.0) };

		assertIllegalArgument(
				"parameterIDsByName must contain at least one parameter.",
				() -> ForwardSensitivities.getHedgeRatios(
						null, 0.0, productGradient, hedgeGradients, basisFunctions, null, 0.0,
						ForwardSensitivities.ReductionMethod.L2, 2));
		assertIllegalArgument(
				"derivativeGradient must not be null.",
				() -> ForwardSensitivities.getHedgeRatios(
						parameterIDsByName, 0.0, null, hedgeGradients, basisFunctions, null, 0.0,
						ForwardSensitivities.ReductionMethod.L2, 2));
		assertIllegalArgument(
				"hedgePortfolioGradients must contain at least one hedge instrument.",
				() -> ForwardSensitivities.getHedgeRatios(
						parameterIDsByName, 0.0, productGradient, null, basisFunctions, null, 0.0,
						ForwardSensitivities.ReductionMethod.L2, 2));
		assertIllegalArgument(
				"hedgePortfolioGradients[0] must not be null.",
				() -> ForwardSensitivities.getHedgeRatios(
						parameterIDsByName, 0.0, productGradient, hedgeGradients(null), basisFunctions, null, 0.0,
						ForwardSensitivities.ReductionMethod.L2, 2));
		assertIllegalArgument(
				"numberOfPaths must be positive.",
				() -> ForwardSensitivities.getHedgeRatios(
						parameterIDsByName, 0.0, productGradient, hedgeGradients, basisFunctions, null, 0.0,
						ForwardSensitivities.ReductionMethod.L2, 0));
		assertIllegalArgument(
				"reductionMethod must not be null.",
				() -> ForwardSensitivities.getHedgeRatios(
						parameterIDsByName, 0.0, productGradient, hedgeGradients, basisFunctions, null, 0.0,
						null, 2));
	}

	@Test
	public void testGradientOverloadValidatesRegularization() {
		final Map<String, Long> parameterIDsByName = parameterIDs("factor", 1L);
		final Map<Long, RandomVariable> productGradient = gradient(1L, deterministic(2.0));
		final List<Map<Long, RandomVariable>> hedgeGradients = hedgeGradients(gradient(1L, deterministic(1.0)));
		final RandomVariable[] basisFunctions = new RandomVariable[] { deterministic(1.0) };

		for(final double invalidLambda : new double[] { -1.0, Double.NaN, Double.POSITIVE_INFINITY }) {
			assertIllegalArgument(
					"regularizationLambda must be finite and non-negative.",
					() -> ForwardSensitivities.getHedgeRatios(
							parameterIDsByName, 0.0, productGradient, hedgeGradients, basisFunctions, null,
							invalidLambda, ForwardSensitivities.ReductionMethod.L2, 2));
		}

		assertIllegalArgument(
				"regularizationLambda must be 0.0 for PATHWISE.",
				() -> ForwardSensitivities.getHedgeRatios(
						parameterIDsByName, 0.0, productGradient, hedgeGradients, null, null, 1E-8,
						ForwardSensitivities.ReductionMethod.PATHWISE, 2));
	}

	@Test
	public void testGradientOverloadValidatesBasisContracts() throws CalculationException {
		final Map<String, Long> parameterIDsByName = parameterIDs("factor", 1L);
		final Map<Long, RandomVariable> productGradient = gradient(1L, deterministic(2.0));
		final List<Map<Long, RandomVariable>> hedgeGradients = hedgeGradients(gradient(1L, deterministic(1.0)));
		final RandomVariable[] basisFunctions = new RandomVariable[] { deterministic(1.0) };

		assertIllegalArgument(
				"solutionBasisFunctions must contain at least one basis function.",
				() -> ForwardSensitivities.getHedgeRatios(
						parameterIDsByName, 0.0, productGradient, hedgeGradients, null, null, 0.0,
						ForwardSensitivities.ReductionMethod.L2, 2));
		assertIllegalArgument(
				"solutionBasisFunctions must contain at least one basis function.",
				() -> ForwardSensitivities.getHedgeRatios(
						parameterIDsByName, 0.0, productGradient, hedgeGradients, new RandomVariable[0], null, 0.0,
						ForwardSensitivities.ReductionMethod.L2, 2));
		assertIllegalArgument(
				"solutionBasisFunctions[0] must not be null.",
				() -> ForwardSensitivities.getHedgeRatios(
						parameterIDsByName, 0.0, productGradient, hedgeGradients,
						new RandomVariable[] { null }, null, 0.0, ForwardSensitivities.ReductionMethod.L2, 2));
		assertIllegalArgument(
				"testBasisFunctions must be null or contain at least one basis function.",
				() -> ForwardSensitivities.getHedgeRatios(
						parameterIDsByName, 0.0, productGradient, hedgeGradients, basisFunctions,
						new RandomVariable[0], 0.0, ForwardSensitivities.ReductionMethod.PROJECTED_GALERKIN, 2));
		assertIllegalArgument(
				"testBasisFunctions[0] must not be null.",
				() -> ForwardSensitivities.getHedgeRatios(
						parameterIDsByName, 0.0, productGradient, hedgeGradients, basisFunctions,
						new RandomVariable[] { null }, 0.0,
						ForwardSensitivities.ReductionMethod.PROJECTED_GALERKIN, 2));
		assertIllegalArgument(
				"solutionBasisFunctions must be null or contain at least one basis function.",
				() -> ForwardSensitivities.getHedgeRatios(
						parameterIDsByName, 0.0, productGradient, hedgeGradients, new RandomVariable[0], null, 0.0,
						ForwardSensitivities.ReductionMethod.PATHWISE, 2));

		final ProjectedHedgeRatioResult rawPathwiseResult = ForwardSensitivities.getHedgeRatios(
				parameterIDsByName,
				0.0,
				productGradient,
				hedgeGradients,
				null,
				null,
				0.0,
				ForwardSensitivities.ReductionMethod.PATHWISE,
				2);
		assertRealizations(new double[] { 2.0, 2.0 }, rawPathwiseResult.getHedgeRatios()[0], 1E-12);

		final ProjectedHedgeRatioResult projectedGalerkinResult = ForwardSensitivities.getHedgeRatios(
				parameterIDsByName,
				0.0,
				productGradient,
				hedgeGradients,
				basisFunctions,
				null,
				0.0,
				ForwardSensitivities.ReductionMethod.PROJECTED_GALERKIN,
				2);
		assertRealizations(new double[] { 2.0 }, projectedGalerkinResult.getHedgeRatios()[0], 1E-12);
	}

	@Test
	public void testDirectPathwiseGradientOverloadValidatesInputs() {
		final Map<String, Long> parameterIDsByName = parameterIDs("factor", 1L);
		final Map<Long, RandomVariable> productGradient = gradient(1L, deterministic(2.0));
		final List<Map<Long, RandomVariable>> hedgeGradients = hedgeGradients(gradient(1L, deterministic(1.0)));

		assertIllegalArgument(
				"derivativeGradient must not be null.",
				() -> ForwardSensitivities.getHedgeRatiosPathwise(
						parameterIDsByName, 0.0, null, hedgeGradients, 2));
		assertIllegalArgument(
				"projectionBasisFunctions must be null or contain at least one basis function.",
				() -> ForwardSensitivities.getHedgeRatiosPathwise(
						parameterIDsByName, 0.0, productGradient, hedgeGradients, new RandomVariable[0], 2));
	}

	private static Map<String, Long> parameterIDs(
			final String name1,
			final Long id1) {
		final Map<String, Long> parameterIDs = new LinkedHashMap<>();
		parameterIDs.put(name1, id1);
		return parameterIDs;
	}

	private static Map<String, Long> parameterIDs(
			final String name1,
			final Long id1,
			final String name2,
			final Long id2) {
		final Map<String, Long> parameterIDs = parameterIDs(name1, id1);
		parameterIDs.put(name2, id2);
		return parameterIDs;
	}

	private static Map<Long, RandomVariable> gradient(final Long id, final RandomVariable value) {
		final Map<Long, RandomVariable> gradient = new LinkedHashMap<>();
		gradient.put(id, value);
		return gradient;
	}

	private static List<Map<Long, RandomVariable>> hedgeGradients(final Map<Long, RandomVariable> gradient) {
		final List<Map<Long, RandomVariable>> hedgeGradients = new ArrayList<>();
		hedgeGradients.add(gradient);
		return hedgeGradients;
	}

	private static RandomVariable deterministic(final double value) {
		return new RandomVariableFromDoubleArray(0.0, value);
	}

	private static RandomVariable randomVariable(final double... values) {
		return new RandomVariableFromDoubleArray(0.0, values);
	}

	private static void assertRealizations(
			final double[] expected,
			final RandomVariable actual,
			final double tolerance) {
		Assert.assertArrayEquals(expected, actual.getRealizations(), tolerance);
	}

	private static void assertIllegalArgument(final String expectedMessage, final Executable executable) {
		final IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, executable);
		Assert.assertEquals(expectedMessage, exception.getMessage());
	}
}
