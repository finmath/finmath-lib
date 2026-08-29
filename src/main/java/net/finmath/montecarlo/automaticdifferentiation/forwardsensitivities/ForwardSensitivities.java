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
		 * benchmark for the reduced projection methods.
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
	 * @param regularizationLambda Absolute lambda in the selected regularized criterion. Use 0.0 for unregularized.
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
	 * @param regularizationLambda Absolute lambda in the selected regularized criterion. Use 0.0 for unregularized.
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
	 * @param regularizationLambda Absolute lambda in the selected regularized criterion. Use 0.0 for unregularized.
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
			int numberOfPaths) throws CalculationException {

		/*
		validateInputs(
				parameterIDsByName,
				derivativeValue,
				hedgePortfolioValues,
				solutionBasisFunctions,
				testBasisFunctions,
				regularizationLambda,
				reductionMethod);
		 */

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
	 *     X (X^T X)^{-1} X^T Y,
	 *
	 * where X is the matrix of projection basis realizations. The pathwise solve
	 * is then performed on the projected derivatives. If projectionBasisFunctions
	 * is null, this method is identical to {@link #getHedgeRatiosPathwise(Map, double, RandomVariable, RandomVariable[])}.
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

		if(projectionBasisFunctions != null && projectionBasisFunctions.length == 0) {
			throw new IllegalArgumentException("projectionBasisFunctions must be null or contain at least one basis function.");
		}

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
	 * The implementation forms the small pathwise normal equation
	 *
	 *     (A_l^T A_l) phi_l = A_l^T b_l
	 *
	 * and solves it path by path. If the pathwise normal matrix is singular,
	 * a least-squares fallback is used.
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
	 * of projectionBasisFunctions before the pathwise normal equations are formed.
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

		if(parameterIDsByName == null || parameterIDsByName.isEmpty()) {
			throw new IllegalArgumentException("parameterIDsByName must contain at least one parameter.");
		}
		if(derivativeGradient == null) {
			throw new IllegalArgumentException("derivativeGradient must not be null.");
		}
		if(hedgePortfolioGradients == null || hedgePortfolioGradients.isEmpty()) {
			throw new IllegalArgumentException("hedgePortfolioGradients must contain at least one hedge instrument.");
		}
		if(numberOfPaths <= 0) {
			throw new IllegalArgumentException("numberOfPaths must be positive.");
		}
		if(projectionBasisFunctions != null && projectionBasisFunctions.length == 0) {
			throw new IllegalArgumentException("projectionBasisFunctions must be null or contain at least one basis function.");
		}

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

	public static double[] solveReducedSystem(
			final double[][] matrix,
			final double[] rhs,
			final double regularizationLambda,
			final boolean matrixIsNormalEquationSystem) throws CalculationException {
	
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
			final double[][] matrixToSolve = matrixPruned;
			for(int index = 0; index < matrixToSolve.length; index++) {
				matrixToSolve[index][index] += regularizationLambda;
			}
	
			if(regularizationLambda > 0.0) {
				try {
					solutionPruned = LinearAlgebra.solveLinearEquationCholesky(matrixToSolve, rhsPrunded);
				}
				catch(final RuntimeException choleskyFailed) {
					/*
					 * Fallback if the matrix is not numerically SPD due to roundoff
					 * or because lambda is too small relative to the matrix scale.
					 */
					solutionPruned = LinearAlgebra.solveLinearEquationSVD(matrixToSolve, rhsPrunded);
				}
			}
			else {
				solutionPruned = LinearAlgebra.solveLinearEquationLeastSquare(matrixToSolve, rhsPrunded);
			}
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
	 * Empirical least-squares projection onto span(X): Y -> X (X^T X)^{-1} X^T Y.
	 *
	 * Inner products are empirical Monte-Carlo averages. A Cholesky factor of X^T X
	 * is reused if the Gram matrix is numerically positive definite. If not, each
	 * projection falls back to the generic least-squares solver used elsewhere in
	 * this class, which also handles linearly dependent basis functions.
	 */
	private static final class EmpiricalProjection {

		private static final double CHOLESKY_TOLERANCE = 1E-14;

		private final RandomVariable[] basisFunctions;
		private final double[][] gramMatrix;
		private final double[][] gramMatrixCholeskyFactor;

		private EmpiricalProjection(
				final RandomVariable[] basisFunctions,
				final int numberOfPaths) {

			if(basisFunctions == null || basisFunctions.length == 0) {
				throw new IllegalArgumentException("basisFunctions must contain at least one basis function.");
			}

			this.basisFunctions = new RandomVariable[basisFunctions.length];
			for(int basisIndex = 0; basisIndex < basisFunctions.length; basisIndex++) {
				if(basisFunctions[basisIndex] == null) {
					throw new IllegalArgumentException("basisFunctions[" + basisIndex + "] is null.");
				}

				/*
				 * Check the number of paths for non-deterministic basis functions and strip
				 * possible AAD information from the basis before repeatedly using it.
				 */
				getPathValues(basisFunctions[basisIndex], numberOfPaths);
				this.basisFunctions[basisIndex] = basisFunctions[basisIndex].getValues();
			}

			gramMatrix = new double[basisFunctions.length][basisFunctions.length];
			for(int row = 0; row < basisFunctions.length; row++) {
				for(int column = 0; column <= row; column++) {
					gramMatrix[row][column] = this.basisFunctions[row].getAverageFast(this.basisFunctions[column]);
					gramMatrix[column][row] = gramMatrix[row][column];
				}
			}

			gramMatrixCholeskyFactor = getCholeskyFactor(gramMatrix, CHOLESKY_TOLERANCE);
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
					: solveReducedSystem(copyMatrix(gramMatrix), rhs, 0.0, true);

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
		final double tolerance = relativeTolerance * Math.max(1.0, maxDiagonal);

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

		try {
			IntStream.range(0, numberOfPaths).parallel().forEach(pathIndex -> {

				final double[][] normalMatrix = new double[numberOfHedges][numberOfHedges];
				final double[] normalRhs = new double[numberOfHedges];
				final double[] hedgeDerivativeAtPath = new double[numberOfHedges];

				for(final String riskFactorName : riskFactorNames) {

					final RandomVariable productSensitivity = productSensitivities.get(riskFactorName);
					final double productDerivativeAtPath = getPathValueOrZero(productSensitivity, pathIndex);

					boolean hasAnyHedgeDerivative = false;
					for(int hedgeIndex = 0; hedgeIndex < numberOfHedges; hedgeIndex++) {
						final RandomVariable hedgeSensitivity = hedgeSensitivities.get(hedgeIndex).get(riskFactorName);
						final double hedgeDerivative = getPathValueOrZero(hedgeSensitivity, pathIndex);
						hedgeDerivativeAtPath[hedgeIndex] = hedgeDerivative;
						hasAnyHedgeDerivative |= hedgeDerivative != 0.0;
					}

					if(!hasAnyHedgeDerivative) {
						continue;
					}

					for(int hedgeIndex1 = 0; hedgeIndex1 < numberOfHedges; hedgeIndex1++) {
						final double hedgeDerivative1 = hedgeDerivativeAtPath[hedgeIndex1];
						if(hedgeDerivative1 == 0.0) {
							continue;
						}

						normalRhs[hedgeIndex1] += hedgeDerivative1 * productDerivativeAtPath;

						for(int hedgeIndex2 = 0; hedgeIndex2 <= hedgeIndex1; hedgeIndex2++) {
							final double hedgeDerivative2 = hedgeDerivativeAtPath[hedgeIndex2];
							if(hedgeDerivative2 == 0.0) {
								continue;
							}

							normalMatrix[hedgeIndex1][hedgeIndex2] += hedgeDerivative1 * hedgeDerivative2;
						}
					}
				}

				for(int hedgeIndex1 = 0; hedgeIndex1 < numberOfHedges; hedgeIndex1++) {
					for(int hedgeIndex2 = 0; hedgeIndex2 < hedgeIndex1; hedgeIndex2++) {
						normalMatrix[hedgeIndex2][hedgeIndex1] = normalMatrix[hedgeIndex1][hedgeIndex2];
					}
				}

				final double[] pathwiseHedgeRatios;
				try {
					pathwiseHedgeRatios = solvePathwiseNormalSystem(normalMatrix, normalRhs);
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

		final RandomVariableFactory factory = new RandomVariableFromArrayFactory();
		final RandomVariable[] hedgeRatios = new RandomVariable[numberOfHedges];
		for(int hedgeIndex = 0; hedgeIndex < numberOfHedges; hedgeIndex++) {
			hedgeRatios[hedgeIndex] = factory.createRandomVariable(evaluationTime, hedgeRatioRealizations[hedgeIndex]);
		}

		return hedgeRatios;
	}

	private static double[] solvePathwiseNormalSystem(
			final double[][] normalMatrix,
			final double[] normalRhs) throws CalculationException {

		final int dimension = normalRhs.length;
		final int[] active = new int[dimension];
		int numberOfActive = 0;

		for(int index = 0; index < dimension; index++) {
			boolean isActive = false;
			for(int column = 0; column < dimension; column++) {
				if(normalMatrix[index][column] != 0.0 || normalMatrix[column][index] != 0.0) {
					isActive = true;
					break;
				}
			}

			if(isActive) {
				active[numberOfActive++] = index;
			}
			else if(normalRhs[index] != 0.0) {
				throw new IllegalArgumentException(
						"Inconsistent pathwise normal equation system: zero row/column with non-zero RHS at hedge index " + index);
			}
		}

		if(numberOfActive == 0) {
			return new double[dimension];
		}

		if(numberOfActive == 1) {
			final double[] solution = new double[dimension];
			final int activeIndex = active[0];
			solution[activeIndex] = normalRhs[activeIndex] / normalMatrix[activeIndex][activeIndex];
			return solution;
		}

		if(numberOfActive == 2) {
			final int activeIndex0 = active[0];
			final int activeIndex1 = active[1];

			final double a = normalMatrix[activeIndex0][activeIndex0];
			final double b = normalMatrix[activeIndex0][activeIndex1];
			final double c = normalMatrix[activeIndex1][activeIndex1];
			final double d = normalRhs[activeIndex0];
			final double e = normalRhs[activeIndex1];

			final double determinant = a * c - b * b;
			final double determinantScale = Math.max(1.0, Math.max(Math.abs(a * c), Math.abs(b * b)));

			if(Math.abs(determinant) > 1E-14 * determinantScale) {
				final double[] solution = new double[dimension];
				solution[activeIndex0] = (d * c - b * e) / determinant;
				solution[activeIndex1] = (a * e - b * d) / determinant;
				return solution;
			}
		}

		final double[][] matrixPruned = new double[numberOfActive][numberOfActive];
		final double[] rhsPruned = new double[numberOfActive];

		for(int row = 0; row < numberOfActive; row++) {
			final int originalRow = active[row];
			for(int column = 0; column < numberOfActive; column++) {
				matrixPruned[row][column] = normalMatrix[originalRow][active[column]];
			}
			rhsPruned[row] = normalRhs[originalRow];
		}

		double[] solutionPruned;
		try {
			solutionPruned = LinearAlgebra.solveLinearEquationCholesky(copyMatrix(matrixPruned), rhsPruned);
		}
		catch(final RuntimeException choleskyFailed) {
			solutionPruned = LinearAlgebra.solveLinearEquationLeastSquare(matrixPruned, rhsPruned);
		}

		final double[] solution = new double[dimension];
		for(int index = 0; index < numberOfActive; index++) {
			solution[active[index]] = solutionPruned[index];
		}

		return solution;
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

		if(parameterIDsByName == null || parameterIDsByName.isEmpty()) {
			throw new IllegalArgumentException("parameterIDsByName must contain at least one parameter.");
		}
		if(derivativeValue == null) {
			throw new IllegalArgumentException("derivativeValue must not be null.");
		}
		if(hedgePortfolioValues == null || hedgePortfolioValues.length == 0) {
			throw new IllegalArgumentException("hedgePortfolioValues must contain at least one hedge instrument.");
		}
		if(reductionMethod == null) {
			throw new IllegalArgumentException("reductionMethod must not be null.");
		}
		if(reductionMethod != ReductionMethod.PATHWISE
				&& (solutionBasisFunctions == null || solutionBasisFunctions.length == 0)) {
			throw new IllegalArgumentException("solutionBasisFunctions must contain at least one basis function.");
		}
		if(reductionMethod == ReductionMethod.PATHWISE
				&& solutionBasisFunctions != null
				&& solutionBasisFunctions.length == 0) {
			throw new IllegalArgumentException("solutionBasisFunctions must be null or contain at least one basis function for PATHWISE.");
		}
		if(reductionMethod == ReductionMethod.PROJECTED_GALERKIN
				&& testBasisFunctions != null
				&& testBasisFunctions.length == 0) {
			throw new IllegalArgumentException("testBasisFunctions must be null or contain at least one basis function.");
		}
		if(regularizationLambda < 0.0) {
			throw new IllegalArgumentException("regularizationLambda must be non-negative.");
		}
	}

	private static void validatePathwiseInputs(
			final Map<String, Long> parameterIDsByName,
			final RandomVariable derivativeValue,
			final RandomVariable[] hedgePortfolioValues) {

		if(parameterIDsByName == null || parameterIDsByName.isEmpty()) {
			throw new IllegalArgumentException("parameterIDsByName must contain at least one parameter.");
		}
		if(derivativeValue == null) {
			throw new IllegalArgumentException("derivativeValue must not be null.");
		}
		if(hedgePortfolioValues == null || hedgePortfolioValues.length == 0) {
			throw new IllegalArgumentException("hedgePortfolioValues must contain at least one hedge instrument.");
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
