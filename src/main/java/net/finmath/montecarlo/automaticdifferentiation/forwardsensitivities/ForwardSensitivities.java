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
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.products.TermStructureMonteCarloProduct;
import net.finmath.stochastic.RandomVariable;

/**
 * Povides a static method to obtain (projected) hedge ratios \( dV/dP_{i} \) projected on span(U_{1}, \ldots, U_r },
 * where U_{k} denotes a set of basis functions.
 * 
 * @author Christian Fries
 */
public class ForwardSensitivities {

	/**
	 * Result container for the reduced stochastic hedge-ratio system.
	 *
	 * hedgeRatios[j] is the reconstructed stochastic hedge ratio phi_j^r(t, omega).
	 * coefficients[j][q] is xi_j^q.
	 */
	public static final class ProjectedHedgeRatioResult {

		private final RandomVariable[] hedgeRatios;
		private final double[][] coefficients;      // [hedgeIndex][basisIndex] = xi_j^q
		private final double[][] reducedMatrix;     // flattened B
		private final double[] reducedRhs;          // flattened beta
		private final List<String> riskFactorNames; // row risk factors M_i

		public ProjectedHedgeRatioResult(
				final RandomVariable[] hedgeRatios,
				final double[][] coefficients,
				final double[][] reducedMatrix,
				final double[] reducedRhs,
				final List<String> riskFactorNames) {
			this.hedgeRatios = hedgeRatios;
			this.coefficients = coefficients;
			this.reducedMatrix = reducedMatrix;
			this.reducedRhs = reducedRhs;
			this.riskFactorNames = riskFactorNames;
		}

		public RandomVariable[] getHedgeRatios() {
			return hedgeRatios;
		}

		public double[][] getCoefficients() {
			return coefficients;
		}

		public double[][] getReducedMatrix() {
			return reducedMatrix;
		}

		public double[] getReducedRhs() {
			return reducedRhs;
		}

		public List<String> getRiskFactorNames() {
			return riskFactorNames;
		}
	}

	/**
	 * Projected stochastic hedge-ratio calculation.
	 *
	 * It solves
	 *
	 *     sum_{j,q} B_{ij}^{sq} xi_j^q = beta_i^s,
	 *
	 * where
	 *
	 *     B_{ij}^{sq} = 1/N sum_l A_{l i j} X_{l q} X_{l s},
	 *     beta_i^s    = 1/N sum_l b_{l i} X_{l s}.
	 *
	 * The hedge ratios are reconstructed pathwise as
	 *
	 *     phi_j^r(omega_l) = sum_q xi_j^q X_q(omega_l).
	 *
	 * @param parameterIDsByName Map of parameter IDs.
	 * @param evaluationTime The time t at which the hedge ratios are calculated.
	 * @param derivative The product V (financial derivative).
	 * @param hedgePortfolio The products P_j (hedge instruments).
	 * @param basisFunctions Basis random variables X_q evaluated on the same paths.
	 *                       To match the derivation literally, pass empirically
	 *                       orthonormal basis functions. See orthonormalizeBasis below.
	 * @param regularizationLambda The lambda in ||Bz-g||^2 + lambda ||z||^2.
	 *                             Use 0.0 for unregularized least squares.
	 * @return stochastic hedge ratios and reduced-system diagnostics.
	 */
	public static ProjectedHedgeRatioResult getHedgeRatiosProjected(
			final Map<String, Long> parameterIDsByName,
			final double evaluationTime,
			final RandomVariable derivativeValue,
			final RandomVariable[] hedgePortfolioValues,
			final RandomVariable[] basisFunctions,
			final double regularizationLambda) throws CalculationException {

		if(parameterIDsByName == null || parameterIDsByName.size() == 0) {
			throw new IllegalArgumentException("parameterIDsByName must contain at least one parameter.");
		}
		if(derivativeValue == null) {
			throw new IllegalArgumentException("derivativeValue must not be null.");
		}
		if(hedgePortfolioValues == null || hedgePortfolioValues.length == 0) {
			throw new IllegalArgumentException("hedgePortfolioValues must contain at least one hedge instrument.");
		}
		if(basisFunctions == null || basisFunctions.length == 0) {
			throw new IllegalArgumentException("basisFunctions must contain at least one basis function.");
		}
		if(regularizationLambda < 0.0) {
			throw new IllegalArgumentException("regularizationLambda must be non-negative.");
		}

		final int numberOfPaths = derivativeValue.size();
		final int numberOfHedges = hedgePortfolioValues.length;
		final int numberOfBasisFunctions = basisFunctions.length;

		if(parameterIDsByName.isEmpty()) {
			throw new IllegalArgumentException(
					"The model exposes no differentiable model parameters. "
					+ "Check that the simulation was created with RandomVariableDifferentiableAADFactory.");
		}

		final List<String> riskFactorNames = new ArrayList<>(parameterIDsByName.keySet());
		final Set<Long> independentIDs = new HashSet<>(parameterIDsByName.values());
		final int numberOfRiskFactors = riskFactorNames.size();

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
		 * X[q][path] = X_q(omega_path).
		 */
		final double[][] basisValues = new double[numberOfBasisFunctions][numberOfPaths];
		for(int basisIndex = 0; basisIndex < numberOfBasisFunctions; basisIndex++) {
			if(basisFunctions[basisIndex] == null) {
				throw new IllegalArgumentException("basisFunctions[" + basisIndex + "] is null.");
			}
			basisValues[basisIndex] = getPathValues(basisFunctions[basisIndex], numberOfPaths);
		}

		/*
		 * Flattened system:
		 *
		 * row(i,s) = s * n + i,
		 * col(j,q) = q * m + j.
		 */
		final int numberOfRows = numberOfRiskFactors * numberOfBasisFunctions;
		final int numberOfColumns = numberOfHedges * numberOfBasisFunctions;

		final double[][] reducedMatrix = new double[numberOfRows][numberOfColumns];
		final double[]   reducedRhs = new double[numberOfRows];

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

			for(int testBasisIndex = 0; testBasisIndex < numberOfBasisFunctions; testBasisIndex++) {

				final int row = rowIndex(riskFactorIndex, testBasisIndex, numberOfRiskFactors);

				/*
				 * beta_i^s = 1/N sum_l b_{l i} X_{l s}.
				 */
				double beta = 0.0;
				for(int path = 0; path < numberOfPaths; path++) {
					beta += productGradient[path] * basisValues[testBasisIndex][path];
				}
				reducedRhs[row] = beta / numberOfPaths;

				/*
				 * B_{ij}^{sq} = 1/N sum_l A_{l i j} X_{l q} X_{l s}.
				 */
				for(int hedgeIndex = 0; hedgeIndex < numberOfHedges; hedgeIndex++) {
					for(int coefficientBasisIndex = 0;
							coefficientBasisIndex < numberOfBasisFunctions;
							coefficientBasisIndex++) {

						double entry = 0.0;
						for(int path = 0; path < numberOfPaths; path++) {
							entry += hedgeGradient[hedgeIndex][path]
									* basisValues[coefficientBasisIndex][path]
									* basisValues[testBasisIndex][path];
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

		/*
		 * Solve reduced system.
		 *
		 * finmath's solveLinearEquationTikonov(A,b,lambdaFinmath) solves the
		 * augmented least-squares problem with lambdaFinmath * I. Hence it corresponds
		 * to ||Az-b||^2 + lambdaFinmath^2 ||z||^2.
		 *
		 * Our input regularizationLambda is the lambda in
		 * ||Az-b||^2 + lambda ||z||^2, so we pass sqrt(lambda).
		 */
		final double[] solution;
		if(regularizationLambda > 0.0) {
			solution = LinearAlgebra.solveLinearEquationTikonov(
					reducedMatrix,
					reducedRhs,
					Math.sqrt(regularizationLambda));
		}
		else {
			solution = LinearAlgebra.solveLinearEquationLeastSquare(reducedMatrix, reducedRhs);
		}

		/*
		 * Unflatten xi_j^q.
		 */
		final double[][] coefficients = new double[numberOfHedges][numberOfBasisFunctions];
		for(int hedgeIndex = 0; hedgeIndex < numberOfHedges; hedgeIndex++) {
			for(int basisIndex = 0; basisIndex < numberOfBasisFunctions; basisIndex++) {
				coefficients[hedgeIndex][basisIndex] =
						solution[columnIndex(hedgeIndex, basisIndex, numberOfHedges)];
			}
		}

		/*
		 * Reconstruct stochastic hedge ratios:
		 *
		 * phi_j^r(omega_l) = sum_q xi_j^q X_q(omega_l).
		 */
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

		return new ProjectedHedgeRatioResult(
				hedgeRatios,
				coefficients,
				reducedMatrix,
				reducedRhs,
				Collections.unmodifiableList(riskFactorNames));
	}

	/**
	 * Empirical Gram-Schmidt orthonormalization.
	 *
	 * Use this if your raw basis is not already orthonormal under
	 *
	 *     <X,Y>_N = 1/N sum_l X_l Y_l.
	 *
	 * The returned basis satisfies <X_k, X_q>_N approximately delta_{kq}.
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
	
}