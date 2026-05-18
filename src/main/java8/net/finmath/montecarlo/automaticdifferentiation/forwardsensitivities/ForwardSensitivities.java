package net.finmath.montecarlo.automaticdifferentiation.forwardsensitivities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.finmath.exception.CalculationException;
import net.finmath.functions.LinearAlgebra;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFromArrayFactory;
import net.finmath.montecarlo.automaticdifferentiation.IndependentModelParameterProvider;
import net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiable;
import net.finmath.stochastic.RandomVariable;

/**
 * Provides static methods to obtain reduced stochastic hedge ratios dV/dP_j.
 *
 * The hedge ratios are represented in a finite solution basis
 *
 *\[ phi_j^r(omega_l) = sum_q xi_j^q X_q(omega_l) \text{.} \]
 *
 * Two reduced coefficient criteria are supported:
 *
 * <ul>
 *   <li>{@link ReductionMethod#L2}: minimize the full empirical pathwise residual
 *       1/N sum_l ||A_l phi_l^r - b_l||^2.</li>
 *   <li>{@link ReductionMethod#PROJECTED_GALERKIN}: impose the projected moment equations
 *       &lt;A phi^r - b, Y_s&gt;_N = 0, where Y_s may differ from the solution basis X_q.</li>
 * </ul>
 *
 * Here b_l is the pathwise derivative of the derivative value with respect to model
 * primitives M_i, and A_l is the pathwise derivative of hedge instruments P_j
 * with respect to the same primitives.
 *
 * See https://ssrn.com/abstract=6758541 for documentation.
 *
 * @author Christian Fries
 */
public class ForwardSensitivities {

	/**
	 * The reduced coefficient criterion used to determine the basis coefficients.
	 */
	public enum ReductionMethod {

		/**
		 * Empirical L2 residual minimization:
		 *
		 *     min_xi 1/N sum_l ||A_l phi_l^r - b_l||_2^2 + lambda ||xi||_2^2.
		 *
		 * The returned reduced matrix is the normal matrix X^T A^T A X / N,
		 * and the returned right-hand side is X^T A^T b / N.
		 */
		L2,

		/**
		 * Projected/Galerkin, or Petrov-Galerkin, moment matching:
		 *
		 *     &lt;(A phi^r - b)_i, Y_s&gt;_N = 0.
		 *
		 * If Y_s = X_s, this is the usual Galerkin case. If the test basis differs
		 * from the solution basis, it is a Petrov-Galerkin projected moment system.
		 *
		 * If the system is not solved exactly, the code solves the reduced least-squares
		 * problem ||B xi - beta||^2 + lambda ||xi||^2.
		 */
		PROJECTED_GALERKIN
	}

	/**
	 * Result container for a reduced stochastic hedge-ratio calculation.
	 *
	 * hedgeRatios[j] is the reconstructed stochastic hedge ratio phi_j^r(t, omega).
	 * coefficients[j][q] is xi_j^q with respect to the solution basis X_q.
	 */
	public static final class ProjectedHedgeRatioResult {

		private final RandomVariable[] hedgeRatios;
		private final double[][] coefficients;      // [hedgeIndex][solutionBasisIndex] = xi_j^q
		private final double[][] reducedMatrix;     // method-dependent reduced system matrix
		private final double[] reducedRhs;          // method-dependent reduced system right-hand side
		private final List<String> riskFactorNames; // row risk factors M_i
		private final ReductionMethod reductionMethod;

		/**
		 * Backwards-compatible constructor. The method is assumed to be PROJECTED_GALERKIN.
		 */
		public ProjectedHedgeRatioResult(
				final RandomVariable[] hedgeRatios,
				final double[][] coefficients,
				final double[][] reducedMatrix,
				final double[] reducedRhs,
				final List<String> riskFactorNames) {
			this(
					hedgeRatios,
					coefficients,
					reducedMatrix,
					reducedRhs,
					riskFactorNames,
					ReductionMethod.PROJECTED_GALERKIN);
		}

		public ProjectedHedgeRatioResult(
				final RandomVariable[] hedgeRatios,
				final double[][] coefficients,
				final double[][] reducedMatrix,
				final double[] reducedRhs,
				final List<String> riskFactorNames,
				final ReductionMethod reductionMethod) {
			this.hedgeRatios = hedgeRatios;
			this.coefficients = coefficients;
			this.reducedMatrix = reducedMatrix;
			this.reducedRhs = reducedRhs;
			this.riskFactorNames = riskFactorNames;
			this.reductionMethod = reductionMethod;
		}

		public RandomVariable[] getHedgeRatios() {
			return hedgeRatios;
		}

		public double[][] getCoefficients() {
			return coefficients;
		}

		/**
		 * Method-dependent reduced system matrix.
		 *
		 * <ul>
		 *   <li>PROJECTED_GALERKIN: B with rows (i,s) for test basis Y_s
		 *       and columns (j,q) for solution basis X_q.</li>
		 *   <li>L2: normal matrix G = D^T D / N with columns (j,q).</li>
		 * </ul>
		 */
		public double[][] getReducedMatrix() {
			return reducedMatrix;
		}

		/**
		 * Method-dependent reduced right-hand side.
		 *
		 * <ul>
		 *   <li>PROJECTED_GALERKIN: beta with rows (i,s) for test basis Y_s.</li>
		 *   <li>L2: h = D^T b / N with columns (j,q).</li>
		 * </ul>
		 */
		public double[] getReducedRhs() {
			return reducedRhs;
		}

		public List<String> getRiskFactorNames() {
			return riskFactorNames;
		}

		public ReductionMethod getReductionMethod() {
			return reductionMethod;
		}
	}

	/**
	 * Backwards-compatible projected stochastic hedge-ratio calculation.
	 *
	 * This solves the projected/Galerkin moment equations using the same basis
	 * for the solution and test spaces,
	 *
	 *     &lt;(A phi^r - b)_i, X_s&gt;_N = 0.
	 */
	public static ProjectedHedgeRatioResult getHedgeRatiosProjected(
			final Map<String, Long> parameterIDsByName,
			final double evaluationTime,
			final RandomVariable derivativeValue,
			final RandomVariable[] hedgePortfolioValues,
			final RandomVariable[] basisFunctions,
			final double regularizationLambda) throws CalculationException {

		return getHedgeRatios(
				parameterIDsByName,
				evaluationTime,
				derivativeValue,
				hedgePortfolioValues,
				basisFunctions,
				basisFunctions,
				regularizationLambda,
				ReductionMethod.PROJECTED_GALERKIN);
	}

	/**
	 * Projected stochastic hedge-ratio calculation with separate solution and test bases.
	 *
	 * The hedge ratios use the solution basis X_q,
	 *
	 *     phi_j^r = sum_q xi_j^q X_q,
	 *
	 * while the residual is tested against Y_s,
	 *
	 *     &lt;(A phi^r - b)_i, Y_s&gt;_N = 0.
	 *
	 * Taking Y_s = X_s gives the Galerkin case. Different Y_s give a
	 * Petrov-Galerkin projected moment system.
	 */
	public static ProjectedHedgeRatioResult getHedgeRatiosProjected(
			final Map<String, Long> parameterIDsByName,
			final double evaluationTime,
			final RandomVariable derivativeValue,
			final RandomVariable[] hedgePortfolioValues,
			final RandomVariable[] solutionBasisFunctions,
			final RandomVariable[] testBasisFunctions,
			final double regularizationLambda) throws CalculationException {

		return getHedgeRatios(
				parameterIDsByName,
				evaluationTime,
				derivativeValue,
				hedgePortfolioValues,
				solutionBasisFunctions,
				testBasisFunctions,
				regularizationLambda,
				ReductionMethod.PROJECTED_GALERKIN);
	}

	/**
	 * Reduced empirical L2 stochastic hedge-ratio calculation.
	 *
	 * This solves
	 *
	 *     min_xi 1/N sum_l ||A_l phi_l^r - b_l||_2^2 + lambda ||xi||_2^2,
	 *
	 * without projecting the output residual onto the hedge-ratio basis.
	 */
	public static ProjectedHedgeRatioResult getHedgeRatiosEmpiricalL2(
			final Map<String, Long> parameterIDsByName,
			final double evaluationTime,
			final RandomVariable derivativeValue,
			final RandomVariable[] hedgePortfolioValues,
			final RandomVariable[] basisFunctions,
			final double regularizationLambda) throws CalculationException {

		return getHedgeRatios(
				parameterIDsByName,
				evaluationTime,
				derivativeValue,
				hedgePortfolioValues,
				basisFunctions,
				null,
				regularizationLambda,
				ReductionMethod.L2);
	}

	/**
	 * General reduced stochastic hedge-ratio calculation supporting both coefficient criteria.
	 *
	 * @param parameterIDsByName Map of model-parameter names to AAD IDs.
	 * @param evaluationTime The time t at which the hedge ratios are calculated.
	 * @param derivativeValue The product value V.
	 * @param hedgePortfolioValues The hedge-instrument values P_j.
	 * @param basisFunctions Basis random variables X_q evaluated on the same paths.
	 *                       For PROJECTED_GALERKIN this basis is used both as solution
	 *                       and test basis.
	 * @param regularizationLambda Lambda in the selected regularized criterion. Use 0.0 for unregularized.
	 * @param reductionMethod The reduced coefficient criterion.
	 * @return stochastic hedge ratios and reduced-system diagnostics.
	 */
	public static ProjectedHedgeRatioResult getHedgeRatiosReduced(
			final Map<String, Long> parameterIDsByName,
			final double evaluationTime,
			final RandomVariable derivativeValue,
			final RandomVariable[] hedgePortfolioValues,
			final RandomVariable[] basisFunctions,
			final double regularizationLambda,
			final ReductionMethod reductionMethod) throws CalculationException {

		return getHedgeRatios(
				parameterIDsByName,
				evaluationTime,
				derivativeValue,
				hedgePortfolioValues,
				basisFunctions,
				basisFunctions,
				regularizationLambda,
				reductionMethod);
	}

	/**
	 * General reduced stochastic hedge-ratio calculation supporting both coefficient criteria.
	 *
	 * @param parameterIDsByName Map of model-parameter names to AAD IDs.
	 * @param evaluationTime The time t at which the hedge ratios are calculated.
	 * @param derivativeValue The product value V.
	 * @param hedgePortfolioValues The hedge-instrument values P_j.
	 * @param solutionBasisFunctions Basis random variables X_q used for the hedge ratios.
	 * @param testBasisFunctions Basis random variables Y_s used for PROJECTED_GALERKIN moments.
	 *                           May be null for L2. If null for PROJECTED_GALERKIN,
	 *                           the solution basis is used as the test basis.
	 * @param regularizationLambda Lambda in the selected regularized criterion. Use 0.0 for unregularized.
	 * @param reductionMethod The reduced coefficient criterion.
	 * @return stochastic hedge ratios and reduced-system diagnostics.
	 */
	public static ProjectedHedgeRatioResult getHedgeRatios(
			final Map<String, Long> parameterIDsByName,
			final double evaluationTime,
			final RandomVariable derivativeValue,
			final RandomVariable[] hedgePortfolioValues,
			final RandomVariable[] solutionBasisFunctions,
			final RandomVariable[] testBasisFunctions,
			final double regularizationLambda,
			final ReductionMethod reductionMethod) throws CalculationException {

		validateInputs(
				parameterIDsByName,
				derivativeValue,
				hedgePortfolioValues,
				solutionBasisFunctions,
				testBasisFunctions,
				regularizationLambda,
				reductionMethod);

		final int numberOfPaths = derivativeValue.size();
		final int numberOfHedges = hedgePortfolioValues.length;
		final int numberOfSolutionBasisFunctions = solutionBasisFunctions.length;

		final List<String> riskFactorNames = new ArrayList<>(parameterIDsByName.keySet());
		final Set<Long> independentIDs = new HashSet<>(parameterIDsByName.values());

		/*
		 * b_{l i} = dV / dM_i, pathwise.
		 */
		final Map<String, RandomVariable> productSensitivities =
				getGradientByModelParameterName(
						derivativeValue,
						parameterIDsByName,
						independentIDs,
						true);

		/*
		 * A_{l i j} = dP_j / dM_i, pathwise.
		 */
		final List<Map<String, RandomVariable>> hedgeSensitivities = new ArrayList<>();
		for(int hedgeIndex = 0; hedgeIndex < numberOfHedges; hedgeIndex++) {

			/*
			 * A hedge may be deterministic at evaluationTime, e.g. a matured bond.
			 * In that case its gradient is zero.
			 */
			final Map<String, RandomVariable> sensitivities =
					getGradientByModelParameterName(
							hedgePortfolioValues[hedgeIndex],
							parameterIDsByName,
							independentIDs,
							false);

			hedgeSensitivities.add(sensitivities);
		}

		/*
		 * X[q][path] = X_q(omega_path), the solution basis.
		 */
		final double[][] solutionBasisValues = new double[numberOfSolutionBasisFunctions][numberOfPaths];
		for(int basisIndex = 0; basisIndex < numberOfSolutionBasisFunctions; basisIndex++) {
			if(solutionBasisFunctions[basisIndex] == null) {
				throw new IllegalArgumentException("solutionBasisFunctions[" + basisIndex + "] is null.");
			}
			solutionBasisValues[basisIndex] = getPathValues(solutionBasisFunctions[basisIndex], numberOfPaths);
		}

		/*
		 * Y[s][path] = Y_s(omega_path), the test basis. It is used only by
		 * PROJECTED_GALERKIN. If no test basis is supplied, use X as Y.
		 */
		final double[][] testBasisValues;
		if(reductionMethod == ReductionMethod.PROJECTED_GALERKIN) {
			final RandomVariable[] effectiveTestBasisFunctions =
					testBasisFunctions != null ? testBasisFunctions : solutionBasisFunctions;
			testBasisValues = new double[effectiveTestBasisFunctions.length][numberOfPaths];
			for(int basisIndex = 0; basisIndex < effectiveTestBasisFunctions.length; basisIndex++) {
				if(effectiveTestBasisFunctions[basisIndex] == null) {
					throw new IllegalArgumentException("testBasisFunctions[" + basisIndex + "] is null.");
				}
				testBasisValues[basisIndex] = getPathValues(effectiveTestBasisFunctions[basisIndex], numberOfPaths);
			}
		}
		else {
			testBasisValues = null;
		}

		final ReducedSystem reducedSystem;
		switch(reductionMethod) {
		case PROJECTED_GALERKIN:
			reducedSystem = assembleProjectedGalerkinSystem(
					riskFactorNames,
					productSensitivities,
					hedgeSensitivities,
					solutionBasisValues,
					testBasisValues,
					numberOfPaths,
					numberOfHedges);
			break;

		case L2:
			reducedSystem = assembleEmpiricalL2NormalSystem(
					riskFactorNames,
					productSensitivities,
					hedgeSensitivities,
					solutionBasisValues,
					numberOfPaths,
					numberOfHedges);
			break;

		default:
			throw new IllegalArgumentException("Unsupported reductionMethod: " + reductionMethod);
		}

		final double[] solution = solveReducedSystem(
				reducedSystem.matrix,
				reducedSystem.rhs,
				regularizationLambda,
				reducedSystem.isNormalEquationSystem);

		/*
		 * Unflatten xi_j^q.
		 */
		final double[][] coefficients = new double[numberOfHedges][numberOfSolutionBasisFunctions];
		for(int hedgeIndex = 0; hedgeIndex < numberOfHedges; hedgeIndex++) {
			for(int basisIndex = 0; basisIndex < numberOfSolutionBasisFunctions; basisIndex++) {
				coefficients[hedgeIndex][basisIndex] =
						solution[columnIndex(hedgeIndex, basisIndex, numberOfHedges)];
			}
		}

		final RandomVariable[] hedgeRatios = reconstructHedgeRatios(
				evaluationTime,
				coefficients,
				solutionBasisValues,
				numberOfPaths);

		return new ProjectedHedgeRatioResult(
				hedgeRatios,
				coefficients,
				reducedSystem.matrix,
				reducedSystem.rhs,
				Collections.unmodifiableList(riskFactorNames),
				reductionMethod);
	}

	/**
	 * Empirical Gram-Schmidt orthonormalization.
	 *
	 * Use this if your raw basis is not already orthonormal under
	 *
	 *     <X,Y>_N = 1/N sum_l X_l Y_l.
	 *
	 * The returned basis satisfies <X_k, X_q>_N approximately delta_{kq}.
	 *
	 * Orthonormality is convenient for interpreting projected coefficients. It is
	 * not required for the empirical L2 residual formulation.
	 */
	static RandomVariable[] orthonormalizeBasis(
			final RandomVariable[] rawBasis,
			final int numberOfPaths,
			final double filtrationTime,
			final double tolerance) {

		if(rawBasis == null || rawBasis.length == 0) {
			throw new IllegalArgumentException("rawBasis must contain at least one basis function.");
		}

		final List<double[]> orthonormalValues = new ArrayList<>();

		for(int basisIndex = 0; basisIndex < rawBasis.length; basisIndex++) {

			if(rawBasis[basisIndex] == null) {
				throw new IllegalArgumentException("rawBasis[" + basisIndex + "] is null.");
			}

			final double[] vector = getPathValues(rawBasis[basisIndex], numberOfPaths);

			for(final double[] previous : orthonormalValues) {
				final double projection = empiricalInnerProduct(vector, previous);
				for(int path = 0; path < numberOfPaths; path++) {
					vector[path] -= projection * previous[path];
				}
			}

			final double norm = Math.sqrt(empiricalInnerProduct(vector, vector));
			if(norm <= tolerance) {
				throw new IllegalArgumentException(
						"rawBasis[" + basisIndex + "] is linearly dependent "
								+ "under the empirical inner product. Norm = " + norm);
			}

			for(int path = 0; path < numberOfPaths; path++) {
				vector[path] /= norm;
			}

			orthonormalValues.add(vector);
		}

		final RandomVariableFactory factory = new RandomVariableFromArrayFactory();
		final RandomVariable[] result = new RandomVariable[orthonormalValues.size()];

		for(int basisIndex = 0; basisIndex < result.length; basisIndex++) {
			result[basisIndex] =
					factory.createRandomVariable(filtrationTime, orthonormalValues.get(basisIndex));
		}

		return result;
	}

	/**
	 * Extract differentiable model parameters M_i and their AAD IDs.
	 */
	public static Map<String, Long> getDifferentiableModelParameterIDs(
			final IndependentModelParameterProvider parameterProvider) {

		final Map<String, RandomVariable> modelParameters = parameterProvider.getModelParameters();
		final Map<String, Long> parameterIDsByName = new LinkedHashMap<>();

		if(modelParameters == null) {
			return parameterIDsByName;
		}

		for(final Entry<String, RandomVariable> entry : modelParameters.entrySet()) {
			final RandomVariable parameter = entry.getValue();

			if(parameter instanceof RandomVariableDifferentiable) {
				parameterIDsByName.put(
						entry.getKey(),
						((RandomVariableDifferentiable) parameter).getID());
			}
		}

		return parameterIDsByName;
	}

	/**
	 * Gradient d(value) / d(model parameter), returned by model-parameter name.
	 */
	private static Map<String, RandomVariable> getGradientByModelParameterName(
			final RandomVariable value,
			final Map<String, Long> parameterIDsByName,
			final Set<Long> independentIDs,
			final boolean requireDifferentiable) {

		if(!(value instanceof RandomVariableDifferentiable)) {
			if(requireDifferentiable) {
				throw new IllegalArgumentException(
						"The product value is not a RandomVariableDifferentiable. "
								+ "Check that the model was created with RandomVariableDifferentiableAADFactory.");
			}
			return Collections.emptyMap();
		}

		final RandomVariableDifferentiable differentiableValue =
				(RandomVariableDifferentiable) value;

		final Map<Long, RandomVariable> gradientByID =
				differentiableValue.getGradient(independentIDs);

		if(gradientByID == null) {
			if(requireDifferentiable) {
				throw new IllegalArgumentException("AAD gradient is null.");
			}
			return Collections.emptyMap();
		}

		final Map<String, RandomVariable> gradientByName = new LinkedHashMap<>();

		for(final Entry<String, Long> parameterEntry : parameterIDsByName.entrySet()) {
			final RandomVariable derivative = gradientByID.get(parameterEntry.getValue());

			if(derivative != null) {
				gradientByName.put(parameterEntry.getKey(), derivative);
			}
		}

		return gradientByName;
	}

	private static ReducedSystem assembleProjectedGalerkinSystem(
			final List<String> riskFactorNames,
			final Map<String, RandomVariable> productSensitivities,
			final List<Map<String, RandomVariable>> hedgeSensitivities,
			final double[][] solutionBasisValues,
			final double[][] testBasisValues,
			final int numberOfPaths,
			final int numberOfHedges) {

		final int numberOfRiskFactors = riskFactorNames.size();
		final int numberOfSolutionBasisFunctions = solutionBasisValues.length;
		final int numberOfTestBasisFunctions = testBasisValues.length;

		/*
		 * Flattened system:
		 *
		 * row(i,s) = s * n + i,
		 * col(j,q) = q * m + j.
		 */
		final int numberOfRows = numberOfRiskFactors * numberOfTestBasisFunctions;
		final int numberOfColumns = numberOfHedges * numberOfSolutionBasisFunctions;

		final double[][] reducedMatrix = new double[numberOfRows][numberOfColumns];
		final double[] reducedRhs = new double[numberOfRows];

		for(int riskFactorIndex = 0; riskFactorIndex < numberOfRiskFactors; riskFactorIndex++) {

			final String riskFactorName = riskFactorNames.get(riskFactorIndex);

			final double[] productGradient =
					getPathValuesOrZero(productSensitivities.get(riskFactorName), numberOfPaths);

			final double[][] hedgeGradient = new double[numberOfHedges][];
			for(int hedgeIndex = 0; hedgeIndex < numberOfHedges; hedgeIndex++) {
				hedgeGradient[hedgeIndex] =
						getPathValuesOrZero(
								hedgeSensitivities.get(hedgeIndex).get(riskFactorName),
								numberOfPaths);
			}

			for(int testBasisIndex = 0; testBasisIndex < numberOfTestBasisFunctions; testBasisIndex++) {

				final int row = rowIndex(riskFactorIndex, testBasisIndex, numberOfRiskFactors);

				/*
				 * beta_i^s = 1/N sum_l b_{l i} Y_{l s}.
				 */
				double beta = 0.0;
				for(int path = 0; path < numberOfPaths; path++) {
					beta += productGradient[path] * testBasisValues[testBasisIndex][path];
				}
				reducedRhs[row] = beta / numberOfPaths;

				/*
				 * B_{ij}^{sq} = 1/N sum_l A_{l i j} X_{l q} Y_{l s}.
				 */
				for(int hedgeIndex = 0; hedgeIndex < numberOfHedges; hedgeIndex++) {
					for(int coefficientBasisIndex = 0;
							coefficientBasisIndex < numberOfSolutionBasisFunctions;
							coefficientBasisIndex++) {

						double entry = 0.0;
						for(int path = 0; path < numberOfPaths; path++) {
							entry += hedgeGradient[hedgeIndex][path]
									* solutionBasisValues[coefficientBasisIndex][path]
											* testBasisValues[testBasisIndex][path];
						}

						final int column = columnIndex(
								hedgeIndex,
								coefficientBasisIndex,
								numberOfHedges);

						reducedMatrix[row][column] = entry / numberOfPaths;
					}
				}
			}
		}

		return new ReducedSystem(reducedMatrix, reducedRhs, false);
	}

	private static ReducedSystem assembleEmpiricalL2NormalSystem(
			final List<String> riskFactorNames,
			final Map<String, RandomVariable> productSensitivities,
			final List<Map<String, RandomVariable>> hedgeSensitivities,
			final double[][] basisValues,
			final int numberOfPaths,
			final int numberOfHedges) {

		final int numberOfRiskFactors = riskFactorNames.size();
		final int numberOfBasisFunctions = basisValues.length;
		final int numberOfColumns = numberOfHedges * numberOfBasisFunctions;

		final double[][] normalMatrix = new double[numberOfColumns][numberOfColumns];
		final double[] normalRhs = new double[numberOfColumns];
		final double[] designRow = new double[numberOfColumns];
		final double scale = 1.0 / numberOfPaths;

		for(int riskFactorIndex = 0; riskFactorIndex < numberOfRiskFactors; riskFactorIndex++) {

			final String riskFactorName = riskFactorNames.get(riskFactorIndex);

			final double[] productGradient =
					getPathValuesOrZero(productSensitivities.get(riskFactorName), numberOfPaths);

			final double[][] hedgeGradient = new double[numberOfHedges][];
			for(int hedgeIndex = 0; hedgeIndex < numberOfHedges; hedgeIndex++) {
				hedgeGradient[hedgeIndex] =
						getPathValuesOrZero(
								hedgeSensitivities.get(hedgeIndex).get(riskFactorName),
								numberOfPaths);
			}

			for(int path = 0; path < numberOfPaths; path++) {

				/*
				 * D_{(l,i),(j,q)} = A_{l i j} X_{l q}.
				 */
				for(int coefficientBasisIndex = 0;
						coefficientBasisIndex < numberOfBasisFunctions;
						coefficientBasisIndex++) {
					final double basisValue = basisValues[coefficientBasisIndex][path];
					for(int hedgeIndex = 0; hedgeIndex < numberOfHedges; hedgeIndex++) {
						final int column = columnIndex(
								hedgeIndex,
								coefficientBasisIndex,
								numberOfHedges);
						designRow[column] = hedgeGradient[hedgeIndex][path] * basisValue;
					}
				}

				/*
				 * h_{(j,q)} = 1/N sum_l sum_i A_{l i j} b_{l i} X_{l q}.
				 */
				final double rhsValue = productGradient[path];
				for(int column1 = 0; column1 < numberOfColumns; column1++) {
					normalRhs[column1] += scale * designRow[column1] * rhsValue;
				}

				/*
				 * G_{(j,q),(k,p)} = 1/N sum_l sum_i A_{l i j} A_{l i k} X_{l q} X_{l p}.
				 * Accumulate the lower triangle and mirror after all paths.
				 */
				for(int column1 = 0; column1 < numberOfColumns; column1++) {
					final double value1 = designRow[column1];
					for(int column2 = 0; column2 <= column1; column2++) {
						normalMatrix[column1][column2] += scale * value1 * designRow[column2];
					}
				}
			}
		}

		for(int column1 = 0; column1 < numberOfColumns; column1++) {
			for(int column2 = 0; column2 < column1; column2++) {
				normalMatrix[column2][column1] = normalMatrix[column1][column2];
			}
		}

		return new ReducedSystem(normalMatrix, normalRhs, true);
	}

	private static double[] solveReducedSystem(
			final double[][] matrix,
			final double[] rhs,
			final double regularizationLambda,
			final boolean matrixIsNormalEquationSystem) throws CalculationException {

		if(matrixIsNormalEquationSystem) {
			/*
			 * The matrix is already G = D^T D / N and rhs is h = D^T b / N.
			 * Tikhonov regularization for
			 *
			 *     ||D z - b||_N^2 + lambda ||z||^2
			 *
			 * is therefore implemented by solving (G + lambda I) z = h.
			 * Do not call solveLinearEquationTikonov here, because that would regularize
			 * the normal equations themselves.
			 */
			final double[][] matrixToSolve = copyMatrix(matrix);
			if(regularizationLambda > 0.0) {
				for(int index = 0; index < matrixToSolve.length; index++) {
					matrixToSolve[index][index] += regularizationLambda;
				}
			}
			return LinearAlgebra.solveLinearEquationLeastSquare(matrixToSolve, rhs);
		}

		/*
		 * Projected/Galerkin system. finmath's solveLinearEquationTikonov(A,b,lambdaFinmath)
		 * solves the augmented least-squares problem with lambdaFinmath * I. Hence it
		 * corresponds to ||Az-b||^2 + lambdaFinmath^2 ||z||^2.
		 *
		 * Our input regularizationLambda is the lambda in ||Az-b||^2 + lambda ||z||^2,
		 * so we pass sqrt(lambda).
		 */
		if(regularizationLambda > 0.0) {
			return LinearAlgebra.solveLinearEquationTikonov(
					matrix,
					rhs,
					Math.sqrt(regularizationLambda));
		}

		return LinearAlgebra.solveLinearEquationLeastSquare(matrix, rhs);
	}

	private static RandomVariable[] reconstructHedgeRatios(
			final double evaluationTime,
			final double[][] coefficients,
			final double[][] basisValues,
			final int numberOfPaths) {

		final int numberOfHedges = coefficients.length;
		final int numberOfBasisFunctions = basisValues.length;

		final RandomVariableFactory outputRandomVariableFactory = new RandomVariableFromArrayFactory();
		final RandomVariable[] hedgeRatios = new RandomVariable[numberOfHedges];

		for(int hedgeIndex = 0; hedgeIndex < numberOfHedges; hedgeIndex++) {

			final double[] hedgeRatioPathValues = new double[numberOfPaths];

			for(int path = 0; path < numberOfPaths; path++) {
				double value = 0.0;

				for(int basisIndex = 0; basisIndex < numberOfBasisFunctions; basisIndex++) {
					value += coefficients[hedgeIndex][basisIndex] * basisValues[basisIndex][path];
				}

				hedgeRatioPathValues[path] = value;
			}

			hedgeRatios[hedgeIndex] =
					outputRandomVariableFactory.createRandomVariable(evaluationTime, hedgeRatioPathValues);
		}

		return hedgeRatios;
	}

	private static void validateInputs(
			final Map<String, Long> parameterIDsByName,
			final RandomVariable derivativeValue,
			final RandomVariable[] hedgePortfolioValues,
			final RandomVariable[] solutionBasisFunctions,
			final RandomVariable[] testBasisFunctions,
			final double regularizationLambda,
			final ReductionMethod reductionMethod) {

		if(parameterIDsByName == null || parameterIDsByName.isEmpty()) {
			throw new IllegalArgumentException("parameterIDsByName must contain at least one parameter.");
		}
		if(derivativeValue == null) {
			throw new IllegalArgumentException("derivativeValue must not be null.");
		}
		if(hedgePortfolioValues == null || hedgePortfolioValues.length == 0) {
			throw new IllegalArgumentException("hedgePortfolioValues must contain at least one hedge instrument.");
		}
		if(solutionBasisFunctions == null || solutionBasisFunctions.length == 0) {
			throw new IllegalArgumentException("solutionBasisFunctions must contain at least one basis function.");
		}
		if(reductionMethod == ReductionMethod.PROJECTED_GALERKIN
				&& testBasisFunctions != null
				&& testBasisFunctions.length == 0) {
			throw new IllegalArgumentException("testBasisFunctions must be null or contain at least one basis function.");
		}
		if(regularizationLambda < 0.0) {
			throw new IllegalArgumentException("regularizationLambda must be non-negative.");
		}
		if(reductionMethod == null) {
			throw new IllegalArgumentException("reductionMethod must not be null.");
		}
	}

	private static double[] getPathValuesOrZero(
			final RandomVariable randomVariable,
			final int numberOfPaths) {

		if(randomVariable == null) {
			return new double[numberOfPaths];
		}

		return getPathValues(randomVariable, numberOfPaths);
	}

	private static double[] getPathValues(
			final RandomVariable randomVariable,
			final int numberOfPaths) {

		final double[] values = new double[numberOfPaths];

		if(randomVariable.isDeterministic()) {
			Arrays.fill(values, randomVariable.doubleValue());
			return values;
		}

		if(randomVariable.size() != numberOfPaths) {
			throw new IllegalArgumentException(
					"RandomVariable has size " + randomVariable.size()
					+ " but model has " + numberOfPaths + " paths.");
		}

		for(int path = 0; path < numberOfPaths; path++) {
			values[path] = randomVariable.get(path);
		}

		return values;
	}

	private static double[][] copyMatrix(final double[][] matrix) {
		final double[][] copy = new double[matrix.length][];
		for(int row = 0; row < matrix.length; row++) {
			copy[row] = Arrays.copyOf(matrix[row], matrix[row].length);
		}
		return copy;
	}

	/*
	 * Helpers
	 */

	private static double empiricalInnerProduct(final double[] x, final double[] y) {

		if(x.length != y.length) {
			throw new IllegalArgumentException("Vector lengths do not match.");
		}

		double sum = 0.0;
		for(int i = 0; i < x.length; i++) {
			sum += x[i] * y[i];
		}

		return sum / x.length;
	}

	/*
	 * Zero-based versions of:
	 *
	 * row(i,s) = (s-1)n + i,
	 * col(j,q) = (q-1)m + j.
	 */
	private static int rowIndex(
			final int riskFactorIndex,
			final int testBasisIndex,
			final int numberOfRiskFactors) {

		return testBasisIndex * numberOfRiskFactors + riskFactorIndex;
	}

	private static int columnIndex(
			final int hedgeIndex,
			final int coefficientBasisIndex,
			final int numberOfHedges) {

		return coefficientBasisIndex * numberOfHedges + hedgeIndex;
	}

	private static final class ReducedSystem {

		private final double[][] matrix;
		private final double[] rhs;
		private final boolean isNormalEquationSystem;

		private ReducedSystem(
				final double[][] matrix,
				final double[] rhs,
				final boolean isNormalEquationSystem) {
			this.matrix = matrix;
			this.rhs = rhs;
			this.isNormalEquationSystem = isNormalEquationSystem;
		}
	}
}
