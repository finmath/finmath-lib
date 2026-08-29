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
import java.util.stream.IntStream;

import net.finmath.exception.CalculationException;
import net.finmath.functions.LinearAlgebra;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFromArrayFactory;
import net.finmath.montecarlo.automaticdifferentiation.IndependentModelParameterProvider;
import net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiable;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;

/**
 * Provides static methods to obtain reduced stochastic hedge ratios dV/dP_j.
 *
 * The hedge ratios are represented in a finite solution basis
 *
 *\[ phi_j^r(omega_l) = sum_q xi_j^q X_q(omega_l) \text{.} \]
 *
 * Two reduced coefficient criteria and one unreduced pathwise benchmark are supported:
 *
 * <ul>
 *   <li>{@link ReductionMethod#L2}: minimize the full empirical pathwise residual
 *       1/N sum_l ||A_l phi_l^r - b_l||^2.</li>
 *   <li>{@link ReductionMethod#PROJECTED_GALERKIN}: impose the projected moment equations
 *       &lt;A phi^r - b, Y_s&gt;_N = 0, where Y_s may differ from the solution basis X_q.</li>
 *   <li>{@link ReductionMethod#PATHWISE}: optionally project dV/dM and dP_j/dM onto
 *       the solution basis first, then solve dV/dM = phi^T dP/dM path by path,
 *       without reducing phi to a finite basis.</li>
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

	/* Relative numerical-rank threshold for the column-equilibrated QR factorization. */
	private static final double QR_RANK_TOLERANCE_FACTOR = 32.0 * Math.ulp(1.0);

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
		PROJECTED_GALERKIN,

		/**
		 * No hedge-ratio reduction: optionally project the input derivatives
		 * b = dV/dM and A_j = dP_j/dM onto the solution basis, then solve the
		 * pathwise least-squares problem
		 *
		 *     min_phi_l ||A_l phi_l - b_l||_2^2
		 *
		 * independently for every Monte-Carlo path l. This directly returns the
		 * pathwise hedge ratios phi_j(l) = dV/dP_j(l) and is intended as a
		 * benchmark for the reduced projection methods. Regularization is not
		 * supported for this method.
		 */
		PATHWISE
	}

	/**
	 * Result container for a reduced stochastic hedge-ratio calculation.
	 *
	 * hedgeRatios[j] is the reconstructed stochastic hedge ratio phi_j^r(t, omega),
	 * or the unreduced pathwise hedge ratio phi_j(t, omega) for PATHWISE.
	 * coefficients[j][q] is xi_j^q with respect to the solution basis X_q.
	 * For PATHWISE, coefficients is empty since no basis expansion of phi is used.
	 */
	public static final class ProjectedHedgeRatioResult {

		private final RandomVariable[] hedgeRatios;
		private final double[][] coefficients;      // [hedgeIndex][solutionBasisIndex] = xi_j^q
		private final double[][] reducedMatrix;     // method-dependent reduced system matrix
		private final double[] reducedRhs;          // method-dependent reduced system right-hand side
		private final List<String> riskFactorNames; // row risk factors M_i
		private final ReductionMethod reductionMethod;
		private final Timings timings;

		public ProjectedHedgeRatioResult(
				final RandomVariable[] hedgeRatios,
				final double[][] coefficients,
				final double[][] reducedMatrix,
				final double[] reducedRhs,
				final List<String> riskFactorNames,
				final ReductionMethod reductionMethod,
				final Timings timings) {
			this.hedgeRatios = hedgeRatios;
			this.coefficients = coefficients;
			this.reducedMatrix = reducedMatrix;
			this.reducedRhs = reducedRhs;
			this.riskFactorNames = riskFactorNames;
			this.reductionMethod = reductionMethod;
			this.timings = timings;
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
		 *   <li>PATHWISE: empty matrix, since no reduced system is assembled.</li>
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
		 *   <li>PATHWISE: empty vector, since no reduced right-hand side is assembled.</li>
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

		public Timings getTimings() {
			return timings;
		}
	}

	public static final class Timings {
		private final long timingProjectSystem;
		private final long timingSolveSystem;

		public Timings(long timingProjectSystem, long timingSolveSystem) {
			super();
			this.timingProjectSystem = timingProjectSystem;
			this.timingSolveSystem = timingSolveSystem;
		}
		public long getTimingProjectSystem() {
			return timingProjectSystem;
		}

		public long getTimingSolveSystem() {
			return timingSolveSystem;
		}
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
	 *                       and test basis. For PATHWISE this basis is used only for
	 *                       the optional derivative projection.
	 * @param regularizationLambda Absolute lambda in the selected regularized criterion.
	 *                             Must be 0.0 for PATHWISE.
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
	 *                               For PATHWISE, this is optional; if supplied, b=dV/dM
	 *                               and A_j=dP_j/dM are first projected onto this basis.
	 * @param testBasisFunctions Basis random variables Y_s used for PROJECTED_GALERKIN moments.
	 *                           May be null for L2. If null for PROJECTED_GALERKIN,
	 *                           the solution basis is used as the test basis. Ignored for PATHWISE.
	 * @param regularizationLambda Absolute lambda in the selected regularized criterion.
	 *                             Must be 0.0 for PATHWISE.
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

		if(!(derivativeValue instanceof RandomVariableDifferentiable)) {
			throw new IllegalArgumentException(
					"The product value is not a RandomVariableDifferentiable. "
							+ "Check that the model was created with RandomVariableDifferentiableAADFactory.");
		}

		final Map<Long, RandomVariable> derivativeGradient = ((RandomVariableDifferentiable)derivativeValue).getGradient();
		final List<Map<Long, RandomVariable>> hedgePortfolioGradients = new ArrayList<Map<Long, RandomVariable>>(hedgePortfolioValues.length);

		for(final RandomVariable hedgeInstrumentProtoValue : hedgePortfolioValues) {
			if(hedgeInstrumentProtoValue instanceof RandomVariableDifferentiable) {
				hedgePortfolioGradients.add(((RandomVariableDifferentiable)hedgeInstrumentProtoValue).getGradient());
			}
			else {
				hedgePortfolioGradients.add(Collections.<Long, RandomVariable>emptyMap());
			}
		}

		return getHedgeRatios(parameterIDsByName, evaluationTime, derivativeGradient, hedgePortfolioGradients,solutionBasisFunctions,
				testBasisFunctions,
				regularizationLambda,
				reductionMethod, derivativeValue.size());
	}


	/**
	 * General reduced stochastic hedge-ratio calculation supporting both coefficient criteria.
	 *
	 * @param parameterIDsByName Map of model-parameter names to AAD IDs.
	 * @param evaluationTime The time t at which the hedge ratios are calculated.
	 * @param derivativeGradient The product gradient d V / dM  (getGradient of a {@code RandomVariableDifferentiable} of the derivative V.
	 * @param hedgePortfolioGradients The gradients (getGradient of a {@code RandomVariableDifferentiable} of the hedge-instrument values P_j.
	 * @param solutionBasisFunctions Basis random variables X_q used for the hedge ratios.
	 *                               For PATHWISE, this is optional; if supplied, b=dV/dM
	 *                               and A_j=dP_j/dM are first projected onto this basis.
	 * @param testBasisFunctions Basis random variables Y_s used for PROJECTED_GALERKIN moments.
	 *                           May be null for L2. If null for PROJECTED_GALERKIN,
	 *                           the solution basis is used as the test basis. Ignored for PATHWISE.
	 * @param regularizationLambda Absolute lambda in the selected regularized criterion.
	 *                             Must be 0.0 for PATHWISE.
	 * @param reductionMethod The reduced coefficient criterion.
	 * @return stochastic hedge ratios and reduced-system diagnostics.
	 */
	public static ProjectedHedgeRatioResult getHedgeRatios(
			final Map<String, Long> parameterIDsByName,
			final double evaluationTime,
			final Map<Long, RandomVariable> derivativeGradient,
			final List<Map<Long, RandomVariable>> hedgePortfolioGradients,
			final RandomVariable[] solutionBasisFunctions,
			final RandomVariable[] testBasisFunctions,
			final double regularizationLambda,
			final ReductionMethod reductionMethod,
			final int numberOfPaths) throws CalculationException {

		validateGradientInputs(
				parameterIDsByName,
				derivativeGradient,
				hedgePortfolioGradients,
				solutionBasisFunctions,
				testBasisFunctions,
				regularizationLambda,
				reductionMethod,
				numberOfPaths);

		if(reductionMethod == ReductionMethod.PATHWISE) {
			return getHedgeRatiosPathwise(
					parameterIDsByName,
					evaluationTime,
					derivativeGradient,
					hedgePortfolioGradients,
					solutionBasisFunctions,
					numberOfPaths);
		}

		final int numberOfHedges = hedgePortfolioGradients.size();
		final int numberOfSolutionBasisFunctions = solutionBasisFunctions.length;

		final List<String> riskFactorNames = new ArrayList<>(parameterIDsByName.keySet());
		final Set<Long> independentIDs = new HashSet<>(parameterIDsByName.values());

		final long timingProjectSystemStart = System.currentTimeMillis();

		/*
		 * b_{l i} = dV / dM_i, pathwise.
		 */
		final Map<String, RandomVariable> productSensitivities = getGradientByModelParameterName(
				derivativeGradient,
				parameterIDsByName,
				independentIDs);

		/*
		 * A_{l i j} = dP_j / dM_i, pathwise.
		 */
		final List<Map<String, RandomVariable>> hedgeSensitivities = new ArrayList<>();
		for(int hedgeIndex = 0; hedgeIndex < numberOfHedges; hedgeIndex++) {

			/*
			 * A hedge may be deterministic at evaluationTime, e.g. a matured bond.
			 * In that case its gradient is zero.
			 */
			final Map<String, RandomVariable> sensitivities = getGradientByModelParameterName(
					hedgePortfolioGradients.get(hedgeIndex),
					parameterIDsByName,
					independentIDs);

			hedgeSensitivities.add(sensitivities);
		}

		/*
		 * X[q][path] = X_q(omega_path), the solution basis.: solutionBasisFunctions[basisIndex]
		 */
		/*
		final double[][] solutionBasisValues = new double[numberOfSolutionBasisFunctions][numberOfPaths];
		for(int basisIndex = 0; basisIndex < numberOfSolutionBasisFunctions; basisIndex++) {
			if(solutionBasisFunctions[basisIndex] == null) {
				throw new IllegalArgumentException("solutionBasisFunctions[" + basisIndex + "] is null.");
			}
			solutionBasisValues[basisIndex] = getPathValues(solutionBasisFunctions[basisIndex], numberOfPaths);
		}
		*/

		/*
		 * Y[s][path] = Y_s(omega_path), the test basis. It is used only by
		 * PROJECTED_GALERKIN. If no test basis is supplied, use X as Y.
		 */
		/*
		double[][] testBasisValues = null;
		if(testBasisFunctions != null) {
			testBasisValues = new double[testBasisFunctions.length][numberOfPaths];
			for(int basisIndex = 0; basisIndex < testBasisFunctions.length; basisIndex++) {
				if(testBasisFunctions[basisIndex] == null) {
					throw new IllegalArgumentException("testBasisFunctions[" + basisIndex + "] is null.");
				}
				testBasisValues[basisIndex] = getPathValues(testBasisFunctions[basisIndex], numberOfPaths);
			}
		}
		*/

		final ReducedSystem reducedSystem;
		switch(reductionMethod) {
		case PROJECTED_GALERKIN:
			reducedSystem = assembleProjectedGalerkinSystem(
					riskFactorNames,
					productSensitivities,
					hedgeSensitivities,
					solutionBasisFunctions,
					testBasisFunctions != null ? testBasisFunctions : solutionBasisFunctions, numberOfPaths, numberOfHedges);
//					solutionBasisValues,
//					testBasisFunctions != null ? testBasisValues : solutionBasisValues, numberOfPaths, numberOfHedges);
			break;

		case L2:
			reducedSystem = assembleEmpiricalL2NormalSystem(
					riskFactorNames,
					productSensitivities,
					hedgeSensitivities,
					solutionBasisFunctions,
//					solutionBasisValues,
					numberOfPaths,
					numberOfHedges);
			break;

		default:
			throw new IllegalArgumentException("Unsupported reductionMethod: " + reductionMethod);
		}

		final long timingProjectSystem = System.currentTimeMillis() - timingProjectSystemStart;
		final long timingSolveSystemStart = System.currentTimeMillis();

		final double[] solution = solveReducedSystem(
				reducedSystem.matrix,
				reducedSystem.rhs,
				regularizationLambda,
				reducedSystem.isNormalEquationSystem);

		final long timingSolveSystem = System.currentTimeMillis() - timingSolveSystemStart;

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
				solutionBasisFunctions);

		return new ProjectedHedgeRatioResult(
				hedgeRatios,
				coefficients,
				reducedSystem.matrix,
				reducedSystem.rhs,
				Collections.unmodifiableList(riskFactorNames),
				reductionMethod,
				new Timings(timingProjectSystem, timingSolveSystem));
	}


	/**
	 * Unreduced pathwise hedge-ratio calculation.
	 *
	 * This solves, independently for every path l,
	 *
	 *     min_phi_l ||A_l phi_l - b_l||_2^2,
	 *
	 * where b_l is dV/dM on path l and A_l contains dP_j/dM on path l.
	 * The returned hedgeRatios[j] are the full pathwise hedge ratios phi_j(l).
	 * No finite basis and no reduced system are used.
	 *
	 * @param parameterIDsByName Map of model-parameter names to AAD IDs.
	 * @param evaluationTime The time t at which the hedge ratios are calculated.
	 * @param derivativeValue The product value V.
	 * @param hedgePortfolioValues The hedge-instrument values P_j.
	 * @return pathwise stochastic hedge ratios and timing diagnostics.
	 */
	public static ProjectedHedgeRatioResult getHedgeRatiosPathwise(
			final Map<String, Long> parameterIDsByName,
			final double evaluationTime,
			final RandomVariable derivativeValue,
			final RandomVariable[] hedgePortfolioValues) throws CalculationException {

		return getHedgeRatiosPathwise(
				parameterIDsByName,
				evaluationTime,
				derivativeValue,
				hedgePortfolioValues,
				null);
	}

	/**
	 * Pathwise hedge-ratio calculation with optional derivative projection.
	 *
	 * If projectionBasisFunctions is non-null, every component of b=dV/dM and
	 * A_j=dP_j/dM is first replaced by its empirical least-squares projection
	 *
	 *     X X^+ Y,
	 *
	 * where X is the matrix of projection basis realizations and X^+ is its
	 * Moore-Penrose pseudo-inverse. This also supports dependent basis functions.
	 * The pathwise solve is then performed on the projected derivatives. If
	 * projectionBasisFunctions is null, this method is identical to
	 * {@link #getHedgeRatiosPathwise(Map, double, RandomVariable, RandomVariable[])}.
	 *
	 * @param parameterIDsByName Map of model-parameter names to AAD IDs.
	 * @param evaluationTime The time t at which the hedge ratios are calculated.
	 * @param derivativeValue The product value V.
	 * @param hedgePortfolioValues The hedge-instrument values P_j.
	 * @param projectionBasisFunctions Optional basis used for empirical conditional-expectation projection.
	 * @return pathwise stochastic hedge ratios and timing diagnostics.
	 */
	public static ProjectedHedgeRatioResult getHedgeRatiosPathwise(
			final Map<String, Long> parameterIDsByName,
			final double evaluationTime,
			final RandomVariable derivativeValue,
			final RandomVariable[] hedgePortfolioValues,
			final RandomVariable[] projectionBasisFunctions) throws CalculationException {

		validatePathwiseInputs(parameterIDsByName, derivativeValue, hedgePortfolioValues);

		validateOptionalBasisFunctions(projectionBasisFunctions, "projectionBasisFunctions");

		if(!(derivativeValue instanceof RandomVariableDifferentiable)) {
			throw new IllegalArgumentException(
					"The product value is not a RandomVariableDifferentiable. "
							+ "Check that the model was created with RandomVariableDifferentiableAADFactory.");
		}

		final Map<Long, RandomVariable> derivativeGradient = ((RandomVariableDifferentiable)derivativeValue).getGradient();
		final List<Map<Long, RandomVariable>> hedgePortfolioGradients = new ArrayList<Map<Long, RandomVariable>>(hedgePortfolioValues.length);

		for(final RandomVariable hedgeInstrumentProtoValue : hedgePortfolioValues) {
			if(hedgeInstrumentProtoValue instanceof RandomVariableDifferentiable) {
				hedgePortfolioGradients.add(((RandomVariableDifferentiable)hedgeInstrumentProtoValue).getGradient());
			}
			else {
				hedgePortfolioGradients.add(Collections.<Long, RandomVariable>emptyMap());
			}
		}

		return getHedgeRatiosPathwise(
				parameterIDsByName,
				evaluationTime,
				derivativeGradient,
				hedgePortfolioGradients,
				projectionBasisFunctions,
				derivativeValue.size());
	}

	/**
	 * Unreduced pathwise hedge-ratio calculation using pre-computed AAD gradients.
	 *
	 * This solves, independently for every path l,
	 *
	 *     min_phi_l ||A_l phi_l - b_l||_2^2,
	 *
	 * where b_l is dV/dM on path l and A_l contains dP_j/dM on path l.
	 * The original rectangular sensitivity system is solved path by path. A
	 * column-equilibrated QR solve provides the full-rank fast path without
	 * squaring the condition number. Rank-deficient systems fall back to an
	 * SVD-based least-squares solve and hence return a minimum-norm solution.
	 *
	 * @param parameterIDsByName Map of model-parameter names to AAD IDs.
	 * @param evaluationTime The time t at which the hedge ratios are calculated.
	 * @param derivativeGradient The product gradient dV/dM.
	 * @param hedgePortfolioGradients The hedge-instrument gradients dP_j/dM.
	 * @param numberOfPaths Number of Monte-Carlo paths.
	 * @return pathwise stochastic hedge ratios and timing diagnostics.
	 */
	public static ProjectedHedgeRatioResult getHedgeRatiosPathwise(
			final Map<String, Long> parameterIDsByName,
			final double evaluationTime,
			final Map<Long, RandomVariable> derivativeGradient,
			final List<Map<Long, RandomVariable>> hedgePortfolioGradients,
			final int numberOfPaths) throws CalculationException {

		return getHedgeRatiosPathwise(
				parameterIDsByName,
				evaluationTime,
				derivativeGradient,
				hedgePortfolioGradients,
				null,
				numberOfPaths);
	}

	/**
	 * Pathwise hedge-ratio calculation using pre-computed AAD gradients and optional
	 * empirical projection of the input derivatives.
	 *
	 * If projectionBasisFunctions is non-null, every component of b=dV/dM and
	 * A_j=dP_j/dM is first replaced by its least-squares projection onto the span
	 * of projectionBasisFunctions before the pathwise systems are solved.
	 *
	 * @param parameterIDsByName Map of model-parameter names to AAD IDs.
	 * @param evaluationTime The time t at which the hedge ratios are calculated.
	 * @param derivativeGradient The product gradient dV/dM.
	 * @param hedgePortfolioGradients The hedge-instrument gradients dP_j/dM.
	 * @param projectionBasisFunctions Optional basis used for empirical conditional-expectation projection.
	 * @param numberOfPaths Number of Monte-Carlo paths.
	 * @return pathwise stochastic hedge ratios and timing diagnostics.
	 */
	public static ProjectedHedgeRatioResult getHedgeRatiosPathwise(
			final Map<String, Long> parameterIDsByName,
			final double evaluationTime,
			final Map<Long, RandomVariable> derivativeGradient,
			final List<Map<Long, RandomVariable>> hedgePortfolioGradients,
			final RandomVariable[] projectionBasisFunctions,
			final int numberOfPaths) throws CalculationException {

		validatePathwiseGradientInputs(
				parameterIDsByName,
				derivativeGradient,
				hedgePortfolioGradients,
				projectionBasisFunctions,
				numberOfPaths);

		final int numberOfHedges = hedgePortfolioGradients.size();

		final List<String> riskFactorNames = new ArrayList<>(parameterIDsByName.keySet());
		final Set<Long> independentIDs = new HashSet<>(parameterIDsByName.values());

		final long timingProjectSystemStart = System.currentTimeMillis();

		/*
		 * b_{l i} = dV / dM_i, pathwise.
		 */
		Map<String, RandomVariable> productSensitivities = getGradientByModelParameterName(
				derivativeGradient,
				parameterIDsByName,
				independentIDs);

		/*
		 * A_{l i j} = dP_j / dM_i, pathwise.
		 */
		List<Map<String, RandomVariable>> hedgeSensitivities = new ArrayList<>();
		for(int hedgeIndex = 0; hedgeIndex < numberOfHedges; hedgeIndex++) {
			final Map<String, RandomVariable> sensitivities = getGradientByModelParameterName(
					hedgePortfolioGradients.get(hedgeIndex),
					parameterIDsByName,
					independentIDs);

			hedgeSensitivities.add(sensitivities);
		}

		if(projectionBasisFunctions != null) {
			final EmpiricalProjection projection = new EmpiricalProjection(projectionBasisFunctions, numberOfPaths);
			productSensitivities = projectSensitivityMap(productSensitivities, projection);
			hedgeSensitivities = projectSensitivityMaps(hedgeSensitivities, projection);
		}

		final long timingProjectSystem = System.currentTimeMillis() - timingProjectSystemStart;
		final long timingSolveSystemStart = System.currentTimeMillis();

		final RandomVariable[] hedgeRatios = solvePathwiseHedgeRatios(
				evaluationTime,
				riskFactorNames,
				productSensitivities,
				hedgeSensitivities,
				numberOfPaths,
				numberOfHedges);

		final long timingSolveSystem = System.currentTimeMillis() - timingSolveSystemStart;

		return new ProjectedHedgeRatioResult(
				hedgeRatios,
				new double[numberOfHedges][0],
				new double[0][0],
				new double[0],
				Collections.unmodifiableList(riskFactorNames),
				ReductionMethod.PATHWISE,
				new Timings(timingProjectSystem, timingSolveSystem));
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
			final Map<Long, RandomVariable> gradientByID,
			final Map<String, Long> parameterIDsByName,
			final Set<Long> independentIDs) {

		final Map<String, RandomVariable> gradientByName = new LinkedHashMap<>();

		for(final Entry<String, Long> parameterEntry : parameterIDsByName.entrySet()) {
			// TODO Check if this is a performance impact
			if(!independentIDs.contains(parameterEntry.getValue())) {
				continue;
			}
			final RandomVariable derivative = gradientByID.get(parameterEntry.getValue());

			if(derivative != null) {
				// The additional getValues prevents that the derivative itself is still a differentiable
				// which would be a performance impact and is not necessary here (since we project to non-differentiables)
				gradientByName.put(parameterEntry.getKey(), derivative.getValues());
			}
		}

		return gradientByName;
	}

	private static ReducedSystem assembleProjectedGalerkinSystem(
			final List<String> riskFactorNames,
			final Map<String, RandomVariable> productSensitivities,
			final List<Map<String, RandomVariable>> hedgeSensitivities,
			final RandomVariable[] solutionBasisFunctions,
			final RandomVariable[] testBasisFunctions,
			final int numberOfPaths,
			final int numberOfHedges) {

		final int numberOfRiskFactors = riskFactorNames.size();
		final int numberOfSolutionBasisFunctions = solutionBasisFunctions.length;
		final int numberOfTestBasisFunctions = testBasisFunctions.length;

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

		IntStream.range(0, numberOfRiskFactors).parallel().forEach(riskFactorIndex ->
		{
			final String riskFactorName = riskFactorNames.get(riskFactorIndex);

			final RandomVariable productGradient = productSensitivities.get(riskFactorName);

			final RandomVariable[] hedgeGradient = new RandomVariable[numberOfHedges];
			for(int hedgeIndex = 0; hedgeIndex < numberOfHedges; hedgeIndex++) {
				hedgeGradient[hedgeIndex] = hedgeSensitivities.get(hedgeIndex).get(riskFactorName);
			}

			for(int testBasisIndex = 0; testBasisIndex < numberOfTestBasisFunctions; testBasisIndex++) {

				final int row = rowIndex(riskFactorIndex, testBasisIndex, numberOfRiskFactors);

				final RandomVariable testBasisFunction = testBasisFunctions[testBasisIndex].getValues();

				/*
				 * beta_i^s = 1/N sum_l b_{l i} Y_{l s}.
				 */
				final double beta = productGradient != null ? productGradient.getAverageFast(testBasisFunction) : 0.0;
				reducedRhs[row] = beta;

				/*
				 * B_{ij}^{sq} = 1/N sum_l A_{l i j} X_{l q} Y_{l s}.
				 */
				for(int hedgeIndex = 0; hedgeIndex < numberOfHedges; hedgeIndex++) {
					final RandomVariable hedgeGradientTimesTest = hedgeGradient[hedgeIndex] != null ? hedgeGradient[hedgeIndex].mult(testBasisFunction) : null;
					for(int coefficientBasisIndex = 0; coefficientBasisIndex < numberOfSolutionBasisFunctions; coefficientBasisIndex++) {
						final double entry = hedgeGradientTimesTest != null ? hedgeGradientTimesTest.getAverageFast(solutionBasisFunctions[coefficientBasisIndex]) : 0.0;

						final int column = columnIndex(hedgeIndex, coefficientBasisIndex, numberOfHedges);

						reducedMatrix[row][column] = entry;
					}
				}
			}
		});

		return new ReducedSystem(reducedMatrix, reducedRhs, false);
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
			final RandomVariable[] basisFunctions,
			final int numberOfPaths,
			final int numberOfHedges) {

		final int numberOfRiskFactors = riskFactorNames.size();
		final int numberOfBasisFunctions = basisFunctions.length;
		final int numberOfColumns = numberOfHedges * numberOfBasisFunctions;

		double[][] normalMatrix = new double[numberOfColumns][numberOfColumns];
		double[] normalRhs = new double[numberOfColumns];

		for(int riskFactorIndex=0; riskFactorIndex<numberOfRiskFactors; riskFactorIndex++)
		{
			final String riskFactorName = riskFactorNames.get(riskFactorIndex);

			final RandomVariable productDerivative = productSensitivities.get(riskFactorName);

			final RandomVariable[] hedgesDerivative = new RandomVariable[numberOfHedges];
			boolean hasAnyHedgeDerivative = false;

			for(int hedgeIndex = 0; hedgeIndex < numberOfHedges; hedgeIndex++) {
				final RandomVariable hedgeDerivative = hedgeSensitivities.get(hedgeIndex).get(riskFactorName);
				hedgesDerivative[hedgeIndex] = hedgeDerivative;
				hasAnyHedgeDerivative |= hedgeDerivative != null;
			}

			// If A_i = 0, this risk factor cannot affect the coefficients.
			// A nonzero productDerivative would only add a constant residual term.
			if(!hasAnyHedgeDerivative) continue;

			/*
			 * D_{(l,i),(j,q)} = A_{l i j} X_{l q}.
			 */
			final RandomVariable[] designRow = new RandomVariable[numberOfColumns];

			/*
			for(int coefficientBasisIndex = 0; coefficientBasisIndex < numberOfBasisFunctions; coefficientBasisIndex++) {
				final RandomVariable basisFunction = basisFunctions[coefficientBasisIndex];
				for(int hedgeIndex = 0; hedgeIndex < numberOfHedges; hedgeIndex++) {
					final int column = columnIndex(hedgeIndex, coefficientBasisIndex, numberOfHedges);
					designRow[column] = hedgeGradient[hedgeIndex] != null ? hedgeGradient[hedgeIndex].mult(basisFunction) : null;
				}
			}
			 */

			/*
			 * h_{(j,q)} = 1/N sum_l sum_i A_{l i j} b_{l i} X_{l q}.
			 * A = hedgeSensitivities, j = hedgeIndex,i = riskFactorIndex, l = pathIndex
			 * b = productSensitivities, j = hedgeIndex,i = riskFactorIndex, l = pathIndex
			 * X = basisFunctions, q = coefficientBasisIndex, l = pathIndex
			 */
			IntStream.range(0, numberOfHedges).parallel().forEach(hedgeIndex -> {
//			for(int hedgeIndex = 0; hedgeIndex < numberOfHedges; hedgeIndex++) {
				final RandomVariable hedgeDerivative = hedgesDerivative[hedgeIndex];
				if(hedgeDerivative == null) return;

				for(int coefficientBasisIndex = 0; coefficientBasisIndex < numberOfBasisFunctions; coefficientBasisIndex++) {
					final RandomVariable basisFunction = basisFunctions[coefficientBasisIndex].getValues();

					final int column = columnIndex(hedgeIndex, coefficientBasisIndex, numberOfHedges);
					designRow[column] = hedgeDerivative.mult(basisFunction);

					if(productDerivative != null) {
						normalRhs[column] += productDerivative.getAverageFast(designRow[column]);
					}
				}
			});

			/*
			 * h_{(j,q)} = 1/N sum_l sum_i A_{l i j} b_{l i} X_{l q}.
			 */
			/*
			if(productGradient != null ) {
				for(int column1 = 0; column1 < numberOfColumns; column1++) {
					final RandomVariable value1 = designRow[column1];
					if(value1 == null) continue;
					normalRhs[column1] += designRow[column1] != null ? productGradient.getAverage(value1) : 0.0;
				}
			}
			 */

			/*
			 * G_{(j,q),(k,p)} = 1/N sum_l sum_i A_{l i j} A_{l i k} X_{l q} X_{l p}.
			 * Accumulate the lower triangle and mirror after all paths.
			 */
			// Parallelize over columns
			IntStream.range(0, numberOfColumns).parallel().forEach(column1 -> {
//			for(int column1 = 0; column1 < numberOfColumns; column1++) {
				final RandomVariable value1 = designRow[column1];
				if(value1 == null) return;
				for(int column2 = 0; column2 <= column1; column2++) {
					final RandomVariable value2 = designRow[column2];
					if(value2 == null) continue;
					normalMatrix[column1][column2] += value1.getAverageFast(value2);
				}
			});
		}

		for(int column1 = 0; column1 < numberOfColumns; column1++) {
			for(int column2 = 0; column2 < column1; column2++) {
				normalMatrix[column2][column1] = normalMatrix[column1][column2];
			}
		}

		return new ReducedSystem(normalMatrix, normalRhs, true);
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

	/**
	 * Solves a reduced hedge-ratio system, optionally recognizing that the supplied
	 * matrix is already a Gram/normal matrix. Active normal systems are diagonally
	 * equilibrated before the Cholesky fast path. If equilibration or Cholesky fails,
	 * the unscaled system is solved by SVD so an exactly rank-deficient system retains
	 * its minimum-Euclidean-norm solution in the original coordinates.
	 *
	 * @param matrix The reduced system matrix.
	 * @param rhs The reduced right-hand side.
	 * @param regularizationLambda The finite, non-negative regularization parameter.
	 * @param matrixIsNormalEquationSystem Whether matrix is already a Gram/normal matrix.
	 * @return The reduced-system solution.
	 * @throws CalculationException Thrown if the numerical solve fails.
	 */
	public static double[] solveReducedSystem(
			final double[][] matrix,
			final double[] rhs,
			final double regularizationLambda,
			final boolean matrixIsNormalEquationSystem) throws CalculationException {

		if(!Double.isFinite(regularizationLambda) || regularizationLambda < 0.0) {
			throw new IllegalArgumentException("regularizationLambda must be finite and non-negative.");
		}

		/*
		 * Perform a pruning
		 */
		int[] rows;
		int[] cols;
		if(matrixIsNormalEquationSystem) {
			if(matrix.length != rhs.length || matrix[0].length != matrix.length) {
				throw new IllegalArgumentException("Normal equation system must be square and match the RHS dimension.");
			}

			// Pruning rows and cols simultaneously only to ensure square matrix.
			final int[] active = IntStream.range(0, matrix.length).parallel()
					.filter(i -> !LinearAlgebra.matrixIsRowZero(matrix, i) || !LinearAlgebra.matrixIsColZero(matrix, i)).toArray();

			// Build a mask of active columns/rows
			final boolean[] isActive = new boolean[matrix.length];
			for(final int index : active) {
				isActive[index] = true;
			}

			/*
			 * Sanity check. In a consistent normal equation system G = D^T D, h = D^T b,
			 * a structurally zero row/column of G must have zero RHS.
			 */
			for(int i = 0; i < rhs.length; i++) {
				if(!isActive[i] && rhs[i] != 0.0) {
					throw new IllegalArgumentException(
							"Inconsistent normal equation system: zero row/column with non-zero RHS at index " + i);
				}
			}

			rows = active;
			cols = active;
		}
		else {
			rows = IntStream.range(0, matrix.length).parallel().
					filter(i -> !LinearAlgebra.matrixIsRowZero(matrix, i)).toArray();
			cols = IntStream.range(0, matrix[0].length).parallel()
					.filter(i -> !LinearAlgebra.matrixIsColZero(matrix, i)).toArray();
		}

		if(cols.length == 0 || rows.length == 0) {
			// Nothing to do
			return new double[matrix[0].length];
		}

		double[][] matrixPruned = new double[rows.length][cols.length];
		double[] rhsPrunded = new double[rows.length];
		double[] solutionPruned;

		for(int row = 0; row<rows.length; row++) {
			for(int col = 0; col<cols.length; col++) {
				matrixPruned[row][col] = matrix[rows[row]][cols[col]];
			}
			rhsPrunded[row] = rhs[rows[row]];
		}

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
			solutionPruned = solveEquilibratedNormalSystem(matrixPruned, rhsPrunded, regularizationLambda);
		}
		else if(regularizationLambda > 0.0) {
			/*
			 * Projected/Galerkin system. finmath's solveLinearEquationTikonov(A,b,lambdaFinmath)
			 * solves the augmented least-squares problem with lambdaFinmath * I. Hence it
			 * corresponds to ||Az-b||^2 + lambdaFinmath^2 ||z||^2.
			 *
			 * Our input regularizationLambda is the absolute lambda in
			 * ||Az-b||^2 + lambda ||z||^2, so we pass sqrt(lambda).
			 */

			/*
			solutionPruned = LinearAlgebra.solveLinearEquationTikonov(
					matrixPruned,
					rhsPrunded,
					Math.sqrt(regularizationLambda));
			 */

			/*
			 * Projected/Galerkin system.
			 *
			 * Current objective:
			 *
			 *     ||B z - beta||^2 + lambda ||z||^2.
			 *
			 * Instead of solving the augmented system
			 *
			 *     [B; sqrt(lambda) I] z ≈ [beta; 0]
			 *
			 * by SVD, solve the equivalent normal equation
			 *
			 *     (B^T B + lambda I) z = B^T beta.
			 */
			solutionPruned = LinearAlgebra.solveTikhonovViaNormalEquations(
					matrixPruned,
					rhsPrunded,
					regularizationLambda);
		}
		else {
			solutionPruned = LinearAlgebra.solveLinearEquationLeastSquare(matrixPruned, rhsPrunded);
		}

		double[] solution = new double[matrix[0].length];
		for(int col = 0; col < cols.length; col++) {
			solution[cols[col]] = solutionPruned[col];
		}
		return solution;
	}

	private static double[] solveEquilibratedNormalSystem(
			final double[][] gramMatrix,
			final double[] rhs,
			final double regularizationLambda) {

		final int dimension = gramMatrix.length;
		final double[][] unscaledMatrix = new double[dimension][dimension];
		final double[] sqrtDiagonal = new double[dimension];
		boolean canEquilibrate = true;

		for(int row = 0; row < dimension; row++) {
			System.arraycopy(gramMatrix[row], 0, unscaledMatrix[row], 0, dimension);
			unscaledMatrix[row][row] += regularizationLambda;

			final double diagonal = gramMatrix[row][row];
			if(!(diagonal > 0.0) || !Double.isFinite(diagonal)) {
				canEquilibrate = false;
			}
			else {
				sqrtDiagonal[row] = Math.sqrt(diagonal);
			}
		}

		if(canEquilibrate) {
			final double[][] equilibratedMatrix = new double[dimension][dimension];
			final double[] equilibratedRhs = new double[dimension];

			for(int row = 0; row < dimension; row++) {
				equilibratedRhs[row] = rhs[row] / sqrtDiagonal[row];
				for(int column = 0; column < dimension; column++) {
					final double largerScale = Math.max(sqrtDiagonal[row], sqrtDiagonal[column]);
					final double smallerScale = Math.min(sqrtDiagonal[row], sqrtDiagonal[column]);
					equilibratedMatrix[row][column] = gramMatrix[row][column] / largerScale / smallerScale;
				}
				equilibratedMatrix[row][row] += regularizationLambda / gramMatrix[row][row];
			}

			if(allFinite(equilibratedMatrix) && allFinite(equilibratedRhs)) {
				try {
					final double[] equilibratedSolution = LinearAlgebra.solveLinearEquationCholesky(
							equilibratedMatrix,
							equilibratedRhs);
					final double[] solution = new double[dimension];
					for(int index = 0; index < dimension; index++) {
						solution[index] = equilibratedSolution[index] / sqrtDiagonal[index];
					}
					if(allFinite(solution)) {
						return solution;
					}
				}
				catch(final RuntimeException choleskyFailed) {
					// Fall through to the unscaled SVD below.
				}
			}
		}

		return LinearAlgebra.solveLinearEquationSVD(unscaledMatrix, rhs);
	}

	private static boolean allFinite(final double[] vector) {
		for(final double value : vector) {
			if(!Double.isFinite(value)) {
				return false;
			}
		}
		return true;
	}

	private static boolean allFinite(final double[][] matrix) {
		for(final double[] row : matrix) {
			if(!allFinite(row)) {
				return false;
			}
		}
		return true;
	}


	private static Map<String, RandomVariable> projectSensitivityMap(
			final Map<String, RandomVariable> sensitivities,
			final EmpiricalProjection projection) throws CalculationException {

		final Map<String, RandomVariable> projectedSensitivities = new LinkedHashMap<String, RandomVariable>();

		for(final Entry<String, RandomVariable> sensitivity : sensitivities.entrySet()) {
			final RandomVariable projectedSensitivity = projection.project(sensitivity.getValue());
			if(projectedSensitivity != null) {
				projectedSensitivities.put(sensitivity.getKey(), projectedSensitivity);
			}
		}

		return projectedSensitivities;
	}

	private static List<Map<String, RandomVariable>> projectSensitivityMaps(
			final List<Map<String, RandomVariable>> sensitivities,
			final EmpiricalProjection projection) throws CalculationException {

		final List<Map<String, RandomVariable>> projectedSensitivities = new ArrayList<Map<String, RandomVariable>>(sensitivities.size());

		for(final Map<String, RandomVariable> sensitivity : sensitivities) {
			projectedSensitivities.add(projectSensitivityMap(sensitivity, projection));
		}

		return projectedSensitivities;
	}

	/**
	 * Empirical least-squares projection onto span(X): Y -> X X^+ Y.
	 *
	 * The basis columns are normalized before forming empirical inner products, so
	 * the positive-definiteness check is independent of their absolute scale. A
	 * Cholesky factor of the normalized Gram matrix is reused when possible. For a
	 * linearly dependent or ill-conditioned basis, the original rectangular design
	 * matrix is solved by the robust least-squares solver instead of solving normal
	 * equations.
	 */
	private static final class EmpiricalProjection {

		private static final double CHOLESKY_TOLERANCE = 1E-14;

		private final RandomVariable[] basisFunctions;
		private final double[][] gramMatrix;
		private final double[][] gramMatrixCholeskyFactor;
		private final double[][] leastSquaresDesignMatrix;
		private final int numberOfPaths;

		private EmpiricalProjection(
				final RandomVariable[] basisFunctions,
				final int numberOfPaths) {

			if(basisFunctions == null || basisFunctions.length == 0) {
				throw new IllegalArgumentException("basisFunctions must contain at least one basis function.");
			}

			this.numberOfPaths = numberOfPaths;
			this.basisFunctions = new RandomVariable[basisFunctions.length];
			for(int basisIndex = 0; basisIndex < basisFunctions.length; basisIndex++) {
				if(basisFunctions[basisIndex] == null) {
					throw new IllegalArgumentException("basisFunctions[" + basisIndex + "] is null.");
				}

				/*
				 * Check the number of paths for non-deterministic basis functions and strip
				 * possible AAD information from the basis before repeatedly using it.
				 */
				final RandomVariable basisValue = basisFunctions[basisIndex].getValues();
				final double basisNorm = getRobustEuclideanNorm(getPathValues(basisValue, numberOfPaths));
				this.basisFunctions[basisIndex] = basisNorm > 0.0 && Double.isFinite(basisNorm)
						? basisValue.div(basisNorm)
						: basisValue;
			}

			gramMatrix = new double[basisFunctions.length][basisFunctions.length];
			for(int row = 0; row < basisFunctions.length; row++) {
				for(int column = 0; column <= row; column++) {
					gramMatrix[row][column] = this.basisFunctions[row].getAverageFast(this.basisFunctions[column]);
					gramMatrix[column][row] = gramMatrix[row][column];
				}
			}

			gramMatrixCholeskyFactor = getCholeskyFactor(gramMatrix, CHOLESKY_TOLERANCE);

			if(gramMatrixCholeskyFactor == null) {
				leastSquaresDesignMatrix = new double[numberOfPaths][basisFunctions.length];
				for(int basisIndex = 0; basisIndex < basisFunctions.length; basisIndex++) {
					final double[] basisRealizations = getPathValues(this.basisFunctions[basisIndex], numberOfPaths);
					for(int pathIndex = 0; pathIndex < numberOfPaths; pathIndex++) {
						leastSquaresDesignMatrix[pathIndex][basisIndex] = basisRealizations[pathIndex];
					}
				}
			}
			else {
				leastSquaresDesignMatrix = null;
			}
		}

		private RandomVariable project(final RandomVariable randomVariable) throws CalculationException {

			if(randomVariable == null) {
				return null;
			}

			final RandomVariable value = randomVariable.getValues();
			final double[] rhs = new double[basisFunctions.length];
			for(int basisIndex = 0; basisIndex < basisFunctions.length; basisIndex++) {
				rhs[basisIndex] = value.getAverageFast(basisFunctions[basisIndex]);
			}

			final double[] coefficients = gramMatrixCholeskyFactor != null
					? solveWithCholeskyFactor(gramMatrixCholeskyFactor, rhs)
					: solveLeastSquaresRobust(
							leastSquaresDesignMatrix,
							getPathValues(value, numberOfPaths));

			RandomVariable projection = Scalar.of(0.0);
			for(int basisIndex = 0; basisIndex < basisFunctions.length; basisIndex++) {
				projection = projection.addProduct(basisFunctions[basisIndex], coefficients[basisIndex]);
			}

			return projection.getValues();
		}
	}

	private static double[][] getCholeskyFactor(
			final double[][] matrix,
			final double relativeTolerance) {

		final int dimension = matrix.length;
		if(dimension == 0) {
			return new double[0][0];
		}

		double maxDiagonal = 0.0;
		for(int row = 0; row < dimension; row++) {
			maxDiagonal = Math.max(maxDiagonal, Math.abs(matrix[row][row]));
		}
		if(!(maxDiagonal > 0.0) || !Double.isFinite(maxDiagonal)) {
			return null;
		}
		final double tolerance = relativeTolerance * maxDiagonal;

		final double[][] factor = new double[dimension][dimension];
		for(int row = 0; row < dimension; row++) {
			for(int column = 0; column <= row; column++) {
				double sum = matrix[row][column];
				for(int k = 0; k < column; k++) {
					sum -= factor[row][k] * factor[column][k];
				}

				if(row == column) {
					if(sum <= tolerance) {
						return null;
					}
					factor[row][column] = Math.sqrt(sum);
				}
				else {
					factor[row][column] = sum / factor[column][column];
				}
			}
		}

		return factor;
	}

	private static double[] solveWithCholeskyFactor(
			final double[][] lowerTriangularFactor,
			final double[] rhs) {

		final int dimension = rhs.length;
		final double[] forwardSolution = new double[dimension];

		for(int row = 0; row < dimension; row++) {
			double value = rhs[row];
			for(int column = 0; column < row; column++) {
				value -= lowerTriangularFactor[row][column] * forwardSolution[column];
			}
			forwardSolution[row] = value / lowerTriangularFactor[row][row];
		}

		final double[] solution = new double[dimension];
		for(int row = dimension-1; row >= 0; row--) {
			double value = forwardSolution[row];
			for(int column = row+1; column < dimension; column++) {
				value -= lowerTriangularFactor[column][row] * solution[column];
			}
			solution[row] = value / lowerTriangularFactor[row][row];
		}

		return solution;
	}

	private static RandomVariable[] solvePathwiseHedgeRatios(
			final double evaluationTime,
			final List<String> riskFactorNames,
			final Map<String, RandomVariable> productSensitivities,
			final List<Map<String, RandomVariable>> hedgeSensitivities,
			final int numberOfPaths,
			final int numberOfHedges) throws CalculationException {

		final double[][] hedgeRatioRealizations = new double[numberOfHedges][numberOfPaths];
		if(numberOfHedges == 1) {
			IntStream.range(0, numberOfPaths).parallel().forEach(pathIndex ->
				hedgeRatioRealizations[0][pathIndex] = solveSingleHedgeRatioPathwise(
						pathIndex,
						riskFactorNames,
						productSensitivities,
						hedgeSensitivities.get(0)));
			return createPathwiseHedgeRatios(evaluationTime, hedgeRatioRealizations);
		}

		try {
			IntStream.range(0, numberOfPaths).parallel().forEach(pathIndex -> {

				/*
				 * Store A column-major. The QR fast path operates on these columns in
				 * place, avoiding one row-array allocation per risk factor and a second
				 * copy of the full pathwise system.
				 */
				final double[][] sensitivityMatrixColumns = new double[numberOfHedges][riskFactorNames.size()];
				final double[] productDerivative = new double[riskFactorNames.size()];
				int numberOfActiveRows = 0;

				for(final String riskFactorName : riskFactorNames) {

					final RandomVariable productSensitivity = productSensitivities.get(riskFactorName);
					boolean hasAnyHedgeDerivative = false;
					for(int hedgeIndex = 0; hedgeIndex < numberOfHedges; hedgeIndex++) {
						final RandomVariable hedgeSensitivity = hedgeSensitivities.get(hedgeIndex).get(riskFactorName);
						final double hedgeDerivative = getPathValueOrZero(hedgeSensitivity, pathIndex);
						sensitivityMatrixColumns[hedgeIndex][numberOfActiveRows] = hedgeDerivative;
						hasAnyHedgeDerivative |= hedgeDerivative != 0.0;
					}

					if(hasAnyHedgeDerivative) {
						productDerivative[numberOfActiveRows] = getPathValueOrZero(productSensitivity, pathIndex);
						numberOfActiveRows++;
					}
				}

				final double[] pathwiseHedgeRatios;
				try {
					pathwiseHedgeRatios = numberOfActiveRows == 0
							? new double[numberOfHedges]
							: solveLeastSquaresRobust(
									sensitivityMatrixColumns,
									productDerivative,
									numberOfActiveRows);
				}
				catch(final CalculationException exception) {
					throw new RuntimeException(exception);
				}

				for(int hedgeIndex = 0; hedgeIndex < numberOfHedges; hedgeIndex++) {
					hedgeRatioRealizations[hedgeIndex][pathIndex] = pathwiseHedgeRatios[hedgeIndex];
				}
			});
		}
		catch(final RuntimeException exception) {
			if(exception.getCause() instanceof CalculationException) {
				throw (CalculationException)exception.getCause();
			}
			throw exception;
		}

		return createPathwiseHedgeRatios(evaluationTime, hedgeRatioRealizations);
	}

	private static RandomVariable[] createPathwiseHedgeRatios(
			final double evaluationTime,
			final double[][] hedgeRatioRealizations) {

		final RandomVariableFactory factory = new RandomVariableFromArrayFactory();
		final int numberOfHedges = hedgeRatioRealizations.length;
		final RandomVariable[] hedgeRatios = new RandomVariable[numberOfHedges];
		for(int hedgeIndex = 0; hedgeIndex < numberOfHedges; hedgeIndex++) {
			hedgeRatios[hedgeIndex] = factory.createRandomVariable(evaluationTime, hedgeRatioRealizations[hedgeIndex]);
		}

		return hedgeRatios;
	}

	private static double solveSingleHedgeRatioPathwise(
			final int pathIndex,
			final List<String> riskFactorNames,
			final Map<String, RandomVariable> productSensitivities,
			final Map<String, RandomVariable> hedgeSensitivities) {

		double hedgeScale = 0.0;
		double productScale = 0.0;
		for(final String riskFactorName : riskFactorNames) {
			final double hedgeDerivative = getPathValueOrZero(hedgeSensitivities.get(riskFactorName), pathIndex);
			if(hedgeDerivative == 0.0) {
				continue;
			}
			final double productDerivative = getPathValueOrZero(productSensitivities.get(riskFactorName), pathIndex);
			if(!Double.isFinite(hedgeDerivative) || !Double.isFinite(productDerivative)) {
				throw new IllegalArgumentException("Pathwise sensitivities must be finite.");
			}
			hedgeScale = Math.max(hedgeScale, Math.abs(hedgeDerivative));
			productScale = Math.max(productScale, Math.abs(productDerivative));
		}

		if(hedgeScale == 0.0 || productScale == 0.0) {
			return 0.0;
		}

		double numerator = 0.0;
		double numeratorCorrection = 0.0;
		double denominator = 0.0;
		double denominatorCorrection = 0.0;
		for(final String riskFactorName : riskFactorNames) {
			final double hedgeDerivative = getPathValueOrZero(hedgeSensitivities.get(riskFactorName), pathIndex);
			if(hedgeDerivative == 0.0) {
				continue;
			}
			final double normalizedHedgeDerivative = hedgeDerivative / hedgeScale;
			final double normalizedProductDerivative = getPathValueOrZero(
					productSensitivities.get(riskFactorName),
					pathIndex) / productScale;

			final double numeratorTerm = normalizedHedgeDerivative * normalizedProductDerivative;
			final double adjustedNumeratorTerm = numeratorTerm - numeratorCorrection;
			final double nextNumerator = numerator + adjustedNumeratorTerm;
			numeratorCorrection = (nextNumerator - numerator) - adjustedNumeratorTerm;
			numerator = nextNumerator;

			final double denominatorTerm = normalizedHedgeDerivative * normalizedHedgeDerivative;
			final double adjustedDenominatorTerm = denominatorTerm - denominatorCorrection;
			final double nextDenominator = denominator + adjustedDenominatorTerm;
			denominatorCorrection = (nextDenominator - denominator) - adjustedDenominatorTerm;
			denominator = nextDenominator;
		}

		return scaleByRatio(numerator / denominator, productScale, hedgeScale);
	}

	private static double scaleByRatio(
			final double value,
			final double numeratorScale,
			final double denominatorScale) {

		final int numeratorExponent = Math.getExponent(numeratorScale);
		final int denominatorExponent = Math.getExponent(denominatorScale);
		final double numeratorMantissa = Math.scalb(numeratorScale, -numeratorExponent);
		final double denominatorMantissa = Math.scalb(denominatorScale, -denominatorExponent);
		final double scaledValue = value * numeratorMantissa / denominatorMantissa;
		return Math.scalb(scaledValue, numeratorExponent - denominatorExponent);
	}

	private static double[] solveLeastSquaresRobust(
			final double[][] matrix,
			final double[] rhs) throws CalculationException {

		if(matrix == null || matrix.length == 0 || matrix[0] == null || matrix[0].length == 0) {
			throw new IllegalArgumentException("matrix must contain at least one row and one column.");
		}
		if(rhs == null || rhs.length != matrix.length) {
			throw new IllegalArgumentException("rhs length must match the number of matrix rows.");
		}

		final int numberOfRows = matrix.length;
		final int numberOfColumns = matrix[0].length;
		final double[][] matrixColumns = new double[numberOfColumns][numberOfRows];
		for(int row = 0; row < numberOfRows; row++) {
			if(matrix[row] == null || matrix[row].length != numberOfColumns) {
				throw new IllegalArgumentException("matrix must be rectangular.");
			}
			for(int column = 0; column < numberOfColumns; column++) {
				matrixColumns[column][row] = matrix[row][column];
			}
		}

		return solveLeastSquaresRobust(matrixColumns, rhs, numberOfRows);
	}

	/**
	 * Solves a rectangular least-squares system supplied as matrix columns.
	 *
	 * Full-column-rank systems use a column-equilibrated, pivoted modified
	 * Gram-Schmidt QR decomposition with re-orthogonalization. The decomposition
	 * operates on working copies of the columns and avoids a heavyweight solver
	 * object on every Monte-Carlo path. The original columns are retained so that,
	 * if numerical rank is lost (or the system is underdetermined), an SVD solve uses
	 * the unmodified matrix and provides the minimum-norm least-squares solution.
	 */
	private static double[] solveLeastSquaresRobust(
			final double[][] matrixColumns,
			final double[] rhs,
			final int numberOfRows) throws CalculationException {

		if(matrixColumns == null || matrixColumns.length == 0 || numberOfRows <= 0) {
			throw new IllegalArgumentException("matrix must contain at least one row and one column.");
		}
		if(rhs == null || rhs.length < numberOfRows) {
			throw new IllegalArgumentException("rhs must contain all matrix rows.");
		}

		final int numberOfColumns = matrixColumns.length;
		final double[] columnNorms = new double[numberOfColumns];
		final double[][] qrColumns = new double[numberOfColumns][];
		final int[] originalColumns = new int[numberOfColumns];
		int numberOfActiveColumns = 0;

		for(int column = 0; column < numberOfColumns; column++) {
			if(matrixColumns[column] == null || matrixColumns[column].length < numberOfRows) {
				throw new IllegalArgumentException("Each matrix column must contain all matrix rows.");
			}

			final double columnNorm = getRobustEuclideanNorm(matrixColumns[column], numberOfRows);
			if(!Double.isFinite(columnNorm)) {
				return solveLeastSquaresUsingSVD(matrixColumns, rhs, numberOfRows);
			}
			columnNorms[column] = columnNorm;
			if(columnNorm > 0.0) {
				qrColumns[numberOfActiveColumns] = Arrays.copyOf(matrixColumns[column], numberOfRows);
				originalColumns[numberOfActiveColumns] = column;
				numberOfActiveColumns++;
			}
		}

		if(numberOfActiveColumns == 0) {
			return new double[numberOfColumns];
		}

		if(numberOfRows < numberOfActiveColumns) {
			return solveLeastSquaresUsingSVD(matrixColumns, rhs, numberOfRows);
		}

		for(int qrColumn = 0; qrColumn < numberOfActiveColumns; qrColumn++) {
			final double columnNorm = columnNorms[originalColumns[qrColumn]];
			for(int row = 0; row < numberOfRows; row++) {
				qrColumns[qrColumn][row] /= columnNorm;
			}
		}

		final double[][] upperTriangularFactor = new double[numberOfActiveColumns][numberOfActiveColumns];
		final double[] transformedRhs = new double[numberOfActiveColumns];
		final double rankTolerance = QR_RANK_TOLERANCE_FACTOR
				* Math.max(numberOfRows, numberOfActiveColumns);

		for(int step = 0; step < numberOfActiveColumns; step++) {
			int pivot = step;
			double pivotNorm = getRobustEuclideanNorm(qrColumns[step], numberOfRows);
			for(int candidate = step+1; candidate < numberOfActiveColumns; candidate++) {
				final double candidateNorm = getRobustEuclideanNorm(qrColumns[candidate], numberOfRows);
				if(candidateNorm > pivotNorm) {
					pivot = candidate;
					pivotNorm = candidateNorm;
				}
			}

			if(pivot != step) {
				final double[] pivotColumn = qrColumns[pivot];
				qrColumns[pivot] = qrColumns[step];
				qrColumns[step] = pivotColumn;

				final int originalColumn = originalColumns[pivot];
				originalColumns[pivot] = originalColumns[step];
				originalColumns[step] = originalColumn;

				for(int previousStep = 0; previousStep < step; previousStep++) {
					final double entry = upperTriangularFactor[previousStep][pivot];
					upperTriangularFactor[previousStep][pivot] = upperTriangularFactor[previousStep][step];
					upperTriangularFactor[previousStep][step] = entry;
				}
			}

			/*
			 * Complete the second modified Gram-Schmidt sweep for the selected
			 * column. Corrections against later q vectors may reintroduce components
			 * along earlier q vectors, so immediate per-q double subtraction is not
			 * equivalent to this full corrective sweep.
			 */
			for(int previousStep = 0; previousStep < step; previousStep++) {
				final double correction = innerProduct(qrColumns[previousStep], qrColumns[step], numberOfRows);
				for(int row = 0; row < numberOfRows; row++) {
					qrColumns[step][row] -= correction * qrColumns[previousStep][row];
				}
				upperTriangularFactor[previousStep][step] += correction;
			}

			pivotNorm = getRobustEuclideanNorm(qrColumns[step], numberOfRows);
			if(!(pivotNorm > rankTolerance) || !Double.isFinite(pivotNorm)) {
				return solveLeastSquaresUsingSVD(matrixColumns, rhs, numberOfRows);
			}

			upperTriangularFactor[step][step] = pivotNorm;
			for(int row = 0; row < numberOfRows; row++) {
				qrColumns[step][row] /= pivotNorm;
			}
			transformedRhs[step] = innerProduct(qrColumns[step], rhs, numberOfRows);

			for(int column = step+1; column < numberOfActiveColumns; column++) {
				final double projection = innerProduct(qrColumns[step], qrColumns[column], numberOfRows);
				for(int row = 0; row < numberOfRows; row++) {
					qrColumns[column][row] -= projection * qrColumns[step][row];
				}
				upperTriangularFactor[step][column] = projection;
			}
		}

		final double[] equilibratedSolution = new double[numberOfActiveColumns];
		for(int row = numberOfActiveColumns-1; row >= 0; row--) {
			double value = transformedRhs[row];
			for(int column = row+1; column < numberOfActiveColumns; column++) {
				value -= upperTriangularFactor[row][column] * equilibratedSolution[column];
			}
			equilibratedSolution[row] = value / upperTriangularFactor[row][row];
			if(!Double.isFinite(equilibratedSolution[row])) {
				return solveLeastSquaresUsingSVD(matrixColumns, rhs, numberOfRows);
			}
		}

		final double[] solution = new double[numberOfColumns];
		for(int qrColumn = 0; qrColumn < numberOfActiveColumns; qrColumn++) {
			final int originalColumn = originalColumns[qrColumn];
			solution[originalColumn] = equilibratedSolution[qrColumn] / columnNorms[originalColumn];
		}
		return solution;
	}

	private static double[] solveLeastSquaresUsingSVD(
			final double[][] originalMatrixColumns,
			final double[] rhs,
			final int numberOfRows) {

		final double[][] matrix = new double[numberOfRows][originalMatrixColumns.length];
		for(int column = 0; column < originalMatrixColumns.length; column++) {
			for(int row = 0; row < numberOfRows; row++) {
				matrix[row][column] = originalMatrixColumns[column][row];
			}
		}
		return LinearAlgebra.solveLinearEquationLeastSquare(matrix, Arrays.copyOf(rhs, numberOfRows));
	}

	private static RandomVariable[] reconstructHedgeRatios(
			final double evaluationTime,
			final double[][] coefficients,
			final RandomVariable[] basisValues) {

		final int numberOfHedges = coefficients.length;
		final int numberOfBasisFunctions = basisValues.length;

		final RandomVariable[] hedgeRatios = new RandomVariable[numberOfHedges];

		for(int hedgeIndex = 0; hedgeIndex < numberOfHedges; hedgeIndex++) {

			RandomVariable hedgeRatio = Scalar.of(0);
			for(int basisIndex = 0; basisIndex < numberOfBasisFunctions; basisIndex++) {
				hedgeRatio = hedgeRatio.addProduct(basisValues[basisIndex], coefficients[hedgeIndex][basisIndex]);
			}

			hedgeRatios[hedgeIndex] = hedgeRatio;
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

		validateParameterIDs(parameterIDsByName);
		if(derivativeValue == null) {
			throw new IllegalArgumentException("derivativeValue must not be null.");
		}
		if(hedgePortfolioValues == null || hedgePortfolioValues.length == 0) {
			throw new IllegalArgumentException("hedgePortfolioValues must contain at least one hedge instrument.");
		}
		for(int hedgeIndex = 0; hedgeIndex < hedgePortfolioValues.length; hedgeIndex++) {
			if(hedgePortfolioValues[hedgeIndex] == null) {
				throw new IllegalArgumentException("hedgePortfolioValues[" + hedgeIndex + "] must not be null.");
			}
		}

		validateReductionConfiguration(
				solutionBasisFunctions,
				testBasisFunctions,
				regularizationLambda,
				reductionMethod);
	}

	private static void validateGradientInputs(
			final Map<String, Long> parameterIDsByName,
			final Map<Long, RandomVariable> derivativeGradient,
			final List<Map<Long, RandomVariable>> hedgePortfolioGradients,
			final RandomVariable[] solutionBasisFunctions,
			final RandomVariable[] testBasisFunctions,
			final double regularizationLambda,
			final ReductionMethod reductionMethod,
			final int numberOfPaths) {

		validateParameterIDs(parameterIDsByName);
		validateGradients(derivativeGradient, hedgePortfolioGradients);
		if(numberOfPaths <= 0) {
			throw new IllegalArgumentException("numberOfPaths must be positive.");
		}

		validateReductionConfiguration(
				solutionBasisFunctions,
				testBasisFunctions,
				regularizationLambda,
				reductionMethod);
	}

	private static void validateReductionConfiguration(
			final RandomVariable[] solutionBasisFunctions,
			final RandomVariable[] testBasisFunctions,
			final double regularizationLambda,
			final ReductionMethod reductionMethod) {

		if(reductionMethod == null) {
			throw new IllegalArgumentException("reductionMethod must not be null.");
		}
		if(!Double.isFinite(regularizationLambda) || regularizationLambda < 0.0) {
			throw new IllegalArgumentException("regularizationLambda must be finite and non-negative.");
		}
		if(reductionMethod == ReductionMethod.PATHWISE) {
			if(regularizationLambda != 0.0) {
				throw new IllegalArgumentException("regularizationLambda must be 0.0 for PATHWISE.");
			}
			validateOptionalBasisFunctions(solutionBasisFunctions, "solutionBasisFunctions");
			return;
		}

		validateRequiredBasisFunctions(solutionBasisFunctions, "solutionBasisFunctions");
		if(reductionMethod == ReductionMethod.PROJECTED_GALERKIN) {
			validateOptionalBasisFunctions(testBasisFunctions, "testBasisFunctions");
		}
	}

	private static void validatePathwiseInputs(
			final Map<String, Long> parameterIDsByName,
			final RandomVariable derivativeValue,
			final RandomVariable[] hedgePortfolioValues) {

		validateParameterIDs(parameterIDsByName);
		if(derivativeValue == null) {
			throw new IllegalArgumentException("derivativeValue must not be null.");
		}
		if(hedgePortfolioValues == null || hedgePortfolioValues.length == 0) {
			throw new IllegalArgumentException("hedgePortfolioValues must contain at least one hedge instrument.");
		}
		for(int hedgeIndex = 0; hedgeIndex < hedgePortfolioValues.length; hedgeIndex++) {
			if(hedgePortfolioValues[hedgeIndex] == null) {
				throw new IllegalArgumentException("hedgePortfolioValues[" + hedgeIndex + "] must not be null.");
			}
		}
	}

	private static void validatePathwiseGradientInputs(
			final Map<String, Long> parameterIDsByName,
			final Map<Long, RandomVariable> derivativeGradient,
			final List<Map<Long, RandomVariable>> hedgePortfolioGradients,
			final RandomVariable[] projectionBasisFunctions,
			final int numberOfPaths) {

		validateParameterIDs(parameterIDsByName);
		validateGradients(derivativeGradient, hedgePortfolioGradients);
		if(numberOfPaths <= 0) {
			throw new IllegalArgumentException("numberOfPaths must be positive.");
		}
		validateOptionalBasisFunctions(projectionBasisFunctions, "projectionBasisFunctions");
	}

	private static void validateParameterIDs(final Map<String, Long> parameterIDsByName) {

		if(parameterIDsByName == null || parameterIDsByName.isEmpty()) {
			throw new IllegalArgumentException("parameterIDsByName must contain at least one parameter.");
		}
		for(final Entry<String, Long> parameter : parameterIDsByName.entrySet()) {
			if(parameter.getKey() == null || parameter.getValue() == null) {
				throw new IllegalArgumentException("parameterIDsByName must not contain null names or IDs.");
			}
		}
	}

	private static void validateGradients(
			final Map<Long, RandomVariable> derivativeGradient,
			final List<Map<Long, RandomVariable>> hedgePortfolioGradients) {

		if(derivativeGradient == null) {
			throw new IllegalArgumentException("derivativeGradient must not be null.");
		}
		if(hedgePortfolioGradients == null || hedgePortfolioGradients.isEmpty()) {
			throw new IllegalArgumentException("hedgePortfolioGradients must contain at least one hedge instrument.");
		}
		for(int hedgeIndex = 0; hedgeIndex < hedgePortfolioGradients.size(); hedgeIndex++) {
			if(hedgePortfolioGradients.get(hedgeIndex) == null) {
				throw new IllegalArgumentException("hedgePortfolioGradients[" + hedgeIndex + "] must not be null.");
			}
		}
	}

	private static void validateRequiredBasisFunctions(
			final RandomVariable[] basisFunctions,
			final String argumentName) {

		if(basisFunctions == null || basisFunctions.length == 0) {
			throw new IllegalArgumentException(argumentName + " must contain at least one basis function.");
		}
		validateBasisFunctionElements(basisFunctions, argumentName);
	}

	private static void validateOptionalBasisFunctions(
			final RandomVariable[] basisFunctions,
			final String argumentName) {

		if(basisFunctions == null) {
			return;
		}
		if(basisFunctions.length == 0) {
			throw new IllegalArgumentException(argumentName + " must be null or contain at least one basis function.");
		}
		validateBasisFunctionElements(basisFunctions, argumentName);
	}

	private static void validateBasisFunctionElements(
			final RandomVariable[] basisFunctions,
			final String argumentName) {

		for(int basisIndex = 0; basisIndex < basisFunctions.length; basisIndex++) {
			if(basisFunctions[basisIndex] == null) {
				throw new IllegalArgumentException(argumentName + "[" + basisIndex + "] must not be null.");
			}
		}
	}

	private static double[] getPathValuesOrZero(final RandomVariable randomVariable, final int numberOfPaths) {

		if(randomVariable == null) {
			return new double[numberOfPaths];
		}

		return getPathValues(randomVariable, numberOfPaths);
	}

	private static double getPathValueOrZero(final RandomVariable randomVariable, final int pathIndex) {

		if(randomVariable == null) {
			return 0.0;
		}

		return randomVariable.get(pathIndex);
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

		return randomVariable.getRealizations();
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

	private static double getRobustEuclideanNorm(final double[] vector) {
		return getRobustEuclideanNorm(vector, vector.length);
	}

	private static double getRobustEuclideanNorm(final double[] vector, final int length) {

		double maximumAbsoluteValue = 0.0;
		for(int index = 0; index < length; index++) {
			final double value = vector[index];
			final double absoluteValue = Math.abs(value);
			if(Double.isNaN(absoluteValue)) {
				return Double.NaN;
			}
			maximumAbsoluteValue = Math.max(maximumAbsoluteValue, absoluteValue);
		}

		if(maximumAbsoluteValue == 0.0 || Double.isInfinite(maximumAbsoluteValue)) {
			return maximumAbsoluteValue;
		}

		double scaledSumOfSquares = 0.0;
		for(int index = 0; index < length; index++) {
			final double value = vector[index];
			final double scaledValue = value / maximumAbsoluteValue;
			scaledSumOfSquares += scaledValue * scaledValue;
		}
		return maximumAbsoluteValue * Math.sqrt(scaledSumOfSquares);
	}

	private static double innerProduct(final double[] x, final double[] y, final int length) {

		if(x.length < length || y.length < length) {
			throw new IllegalArgumentException("Vectors do not contain the requested number of entries.");
		}

		double sum = 0.0;
		double compensation = 0.0;
		for(int index = 0; index < length; index++) {
			final double product = x[index] * y[index];
			final double compensatedProduct = product - compensation;
			final double newSum = sum + compensatedProduct;
			compensation = (newSum - sum) - compensatedProduct;
			sum = newSum;
		}
		return sum;
	}

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
