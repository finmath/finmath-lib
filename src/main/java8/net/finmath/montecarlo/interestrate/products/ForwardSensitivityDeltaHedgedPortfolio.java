/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 14.05.2026
 */
package net.finmath.montecarlo.interestrate.products;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiable;
import net.finmath.montecarlo.automaticdifferentiation.forwardsensitivities.ForwardSensitivities;
import net.finmath.montecarlo.automaticdifferentiation.forwardsensitivities.ForwardSensitivities.ProjectedHedgeRatioResult;
import net.finmath.montecarlo.automaticdifferentiation.forwardsensitivities.ForwardSensitivities.ReductionMethod;
import net.finmath.montecarlo.conditionalexpectation.MonteCarloConditionalExpectationRegression;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.stochastic.ConditionalExpectationEstimator;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
import net.finmath.time.TimeDiscretization;
import net.finmath.util.Java8BackportUtil;

/**
 * A self-financing hedge simulator for term-structure products using stochastic
 * hedge ratios obtained from {@link ForwardSensitivities}.
 *
 * <p>
 * This class is the term-structure analogue of a delta-hedged portfolio. At each
 * rebalancing time {@code t} it
 * </p>
 * <ol>
 *   <li>selects the differentiable process state at {@code t} as the set of
 *       model primitives,</li>
 *   <li>values the product to replicate and the hedge instruments as
 *       pathwise/proto-values,</li>
 *   <li>calls {@link ForwardSensitivities} to obtain stochastic hedge ratios,
 *       and</li>
 *   <li>changes hedge-instrument positions in a self-financing way using
 *       adapted trade values, i.e. conditional expectations of the proto-values
 *       given the information at {@code t}.</li>
 * </ol>
 *
 * <p>
 * The distinction between proto-values and trade values is important for
 * products such as {@link Bond}: {@code Bond.getValue(t, model)} returns the
 * discounted pathwise unit payoff converted to evaluation time {@code t}. For a
 * Monte-Carlo simulation this is generally not yet the tradable bond price
 * {@code P(t,T)}; the tradable price is its conditional expectation given
 * {@code F_t}. The proto-values are used for the sensitivity identities, while
 * the adapted trade values are used for the cash-account update.
 * </p>
 *
 * <p>
 * The default primitive ID provider uses the process values
 * {@code model.getProcess().getProcessValue(timeIndex(t))}. Thus the
 * sensitivities are with respect to the state at the current rebalancing time,
 * not with respect to initial model parameters. This is the appropriate local
 * hedge object for a dynamic delta hedge.
 * </p>
 *
 * <p>
 * The simulator does not independently book hedge-instrument cashflows paid
 * between rebalancing and final evaluation. Hedge instruments must therefore
 * have no payment strictly before {@code evaluationTime}, unless a custom hedge
 * product represents a total-return value that carries paid cashflows. Known
 * built-in cashflow products are checked at runtime. For custom products,
 * ensuring this convention is the caller's responsibility.
 * </p>
 *
 * @author Christian Fries
 */
public class ForwardSensitivityDeltaHedgedPortfolio extends AbstractTermStructureMonteCarloProduct {

	/**
	 * Provides basis functions used to represent stochastic hedge ratios.
	 */
	@FunctionalInterface
	public interface BasisFunctionProvider {

		/**
		 * Returns basis functions evaluated at the given time on the paths of the model.
		 *
		 * @param evaluationTime The time t at which the hedge ratio is calculated.
		 * @param model The term-structure Monte-Carlo model.
		 * @return The basis functions X_q(t, omega).
		 * @throws CalculationException Thrown if a model quantity cannot be obtained.
		 */
		RandomVariable[] getBasisFunctions(
				double evaluationTime,
				TermStructureMonteCarloSimulationModel model) throws CalculationException;
	}

	/**
	 * Provides the AAD IDs of the primitives with respect to which the hedge
	 * equations are formed.
	 */
	@FunctionalInterface
	public interface ParameterIDProvider {

		/**
		 * Returns the AAD IDs of the model primitives at the given rebalancing time.
		 *
		 * @param evaluationTime The rebalancing time t.
		 * @param model The term-structure Monte-Carlo model.
		 * @return Map from primitive names to AAD IDs.
		 * @throws CalculationException Thrown if the process state cannot be obtained.
		 */
		Map<String, Long> getParameterIDs(
				double evaluationTime,
				TermStructureMonteCarloSimulationModel model) throws CalculationException;
	}

	/**
	 * Provides the hedge-instrument values used in the forward-sensitivity
	 * equation. The default implementation calls product.getValue(t, model).
	 * For diagnostic purposes, bonds may instead be valued analytically from the
	 * current forward rates.
	 */
	@FunctionalInterface
	public interface HedgeInstrumentValueProvider {

		/**
		 * Returns the hedge-instrument values used by ForwardSensitivities.
		 *
		 * @param evaluationTime The rebalancing time t.
		 * @param model The term-structure Monte-Carlo model.
		 * @param hedgeInstruments The hedge instruments.
		 * @return The hedge-instrument values P_j(t).
		 * @throws CalculationException Thrown if a value cannot be obtained.
		 */
		RandomVariable[] getValues(
				double evaluationTime,
				TermStructureMonteCarloSimulationModel model,
				List<TermStructureMonteCarloProduct> hedgeInstruments) throws CalculationException;
	}

	/**
	 * Provides adapted trade values used in the self-financing cash-account update.
	 */
	@FunctionalInterface
	public interface HedgeInstrumentTradeValueProvider {

		/**
		 * Returns adapted trade values of the hedge instruments at evaluationTime.
		 *
		 * @param evaluationTime The rebalancing time t.
		 * @param model The term-structure Monte-Carlo model.
		 * @param hedgeInstruments The hedge instruments.
		 * @param hedgeInstrumentProtoValues The current-time raw product values returned by getValue(t, model).
		 * @param conditioningBasisFunctions Basis functions used for conditional-expectation projection.
		 *        May be null if the provider supports a fallback conditioning rule.
		 * @return The adapted trade values used to finance the hedge trades.
		 * @throws CalculationException Thrown if a value cannot be obtained.
		 */
		RandomVariable[] getTradeValues(
				double evaluationTime,
				TermStructureMonteCarloSimulationModel model,
				List<TermStructureMonteCarloProduct> hedgeInstruments,
				RandomVariable[] hedgeInstrumentProtoValues,
				RandomVariable[] conditioningBasisFunctions) throws CalculationException;
	}

	/**
	 * Provides the hedge-ratio estimator used at a rebalancing time.
	 *
	 * <p>
	 * The default provider delegates to {@link ForwardSensitivities#getHedgeRatios(Map, double, Map, List, RandomVariable[], RandomVariable[], double, ReductionMethod, int)}.
	 * A custom provider may persist the raw pathwise gradients or replace them by
	 * externally projected gradients before delegating to {@link ForwardSensitivities}.
	 * </p>
	 */
	@FunctionalInterface
	public interface HedgeRatioProvider {

		ProjectedHedgeRatioResult getHedgeRatios(
				Map<String, Long> parameterIDsByName,
				double evaluationTime,
				Map<Long, RandomVariable> derivativeGradient,
				List<Map<Long, RandomVariable>> hedgePortfolioGradients,
				RandomVariable[] solutionBasisFunctions,
				RandomVariable[] testBasisFunctions,
				double regularizationLambda,
				ReductionMethod reductionMethod,
				int numberOfPaths) throws CalculationException;
	}

	private final TermStructureMonteCarloProduct productToReplicate;
	private final List<TermStructureMonteCarloProduct> hedgeInstruments;
	private final TimeDiscretization rebalancingTimes;
	private final BasisFunctionProvider solutionBasisFunctionProvider;
	private final BasisFunctionProvider testBasisFunctionProvider;
	private final ParameterIDProvider parameterIDProvider;
	private final HedgeInstrumentValueProvider hedgeInstrumentValueProvider;
	private final HedgeInstrumentTradeValueProvider hedgeInstrumentTradeValueProvider;
	private final HedgeInstrumentValueProvider finalHedgeInstrumentValueProvider;
	private final HedgeRatioProvider hedgeRatioProvider;
	private final double regularizationLambda;
	private final ReductionMethod reductionMethod;

	private double lastOperationTimingTotal = Double.NaN;
	private double lastOperationTimingValuation = Double.NaN;
	private double lastOperationTimingHedgeRatios = Double.NaN;
	private double lastOperationTimingHedgeRatioProject = Double.NaN;
	private double lastOperationTimingHedgeRatioSolve = Double.NaN;
	private double lastOperationTimingTradeValues = Double.NaN;

	private List<Double> lastRebalancingTimes = Collections.emptyList();
	private List<Map<String, Long>> lastParameterIDsByName = Collections.emptyList();
	private List<ProjectedHedgeRatioResult> lastHedgeRatioResults = Collections.emptyList();
	private RandomVariable[] lastHedgeInstrumentPositions = new RandomVariable[0];
	private RandomVariable lastNumerairePosition;

	/**
	 * Creates a self-financing hedge using the same basis for solution and test
	 * functions, the process-state primitive provider, and regression trade values.
	 * Hedge instruments are subject to the intermediate-cashflow restriction in
	 * the class documentation.
	 *
	 * @param productToReplicate The product to replicate.
	 * @param hedgeInstruments The hedge instruments P_j.
	 * @param rebalancingTimes The times at which the hedge is rebalanced.
	 * @param basisFunctionProvider The basis functions X_q used for the hedge ratios.
	 *        May be null for an unreduced PATHWISE hedge.
	 * @param regularizationLambda Finite, non-negative Tikhonov parameter.
	 *        Must be 0.0 for PATHWISE.
	 * @param reductionMethod The reduction method, e.g. PROJECTED_GALERKIN or L2.
	 */
	public ForwardSensitivityDeltaHedgedPortfolio(
			final TermStructureMonteCarloProduct productToReplicate,
			final List<TermStructureMonteCarloProduct> hedgeInstruments,
			final TimeDiscretization rebalancingTimes,
			final BasisFunctionProvider basisFunctionProvider,
			final double regularizationLambda,
			final ReductionMethod reductionMethod) {
		this(
				productToReplicate,
				hedgeInstruments,
				rebalancingTimes,
				basisFunctionProvider,
				null,
				getProcessStateParameterIDProvider(),
				getRegressionTradeValueProvider(),
				regularizationLambda,
				reductionMethod);
	}

	/**
	 * Creates a self-financing hedge using possibly different solution and test bases,
	 * the process-state primitive provider, and regression trade values.
	 * Hedge instruments are subject to the intermediate-cashflow restriction in
	 * the class documentation.
	 *
	 * @param productToReplicate The product to replicate.
	 * @param hedgeInstruments The hedge instruments P_j.
	 * @param rebalancingTimes The times at which the hedge is rebalanced.
	 * @param solutionBasisFunctionProvider The basis functions X_q used for hedge ratios.
	 * @param testBasisFunctionProvider The basis functions Y_s used for PROJECTED_GALERKIN moments. May be null.
	 * @param regularizationLambda Finite, non-negative Tikhonov parameter.
	 *        Must be 0.0 for PATHWISE.
	 * @param reductionMethod The reduction method, e.g. PROJECTED_GALERKIN or L2.
	 */
	public ForwardSensitivityDeltaHedgedPortfolio(
			final TermStructureMonteCarloProduct productToReplicate,
			final List<TermStructureMonteCarloProduct> hedgeInstruments,
			final TimeDiscretization rebalancingTimes,
			final BasisFunctionProvider solutionBasisFunctionProvider,
			final BasisFunctionProvider testBasisFunctionProvider,
			final double regularizationLambda,
			final ReductionMethod reductionMethod) {
		this(
				productToReplicate,
				hedgeInstruments,
				rebalancingTimes,
				solutionBasisFunctionProvider,
				testBasisFunctionProvider,
				getProcessStateParameterIDProvider(),
				getRegressionTradeValueProvider(),
				regularizationLambda,
				reductionMethod);
	}

	/**
	 * Full constructor allowing custom primitive and trade-value providers. The
	 * hedge-instrument values used in ForwardSensitivities are the product values
	 * returned by {@code product.getValue(t, model)}.
	 * Hedge instruments are subject to the intermediate-cashflow restriction in
	 * the class documentation.
	 *
	 * @param productToReplicate The product to replicate.
	 * @param hedgeInstruments The hedge instruments P_j.
	 * @param rebalancingTimes The times at which the hedge is rebalanced.
	 * @param solutionBasisFunctionProvider The basis functions X_q used for hedge ratios.
	 * @param testBasisFunctionProvider The basis functions Y_s used for PROJECTED_GALERKIN moments. May be null.
	 * @param parameterIDProvider Provides the primitive AAD IDs used by ForwardSensitivities.
	 * @param hedgeInstrumentTradeValueProvider Provides adapted trade values for the self-financing update.
	 * @param regularizationLambda Finite, non-negative Tikhonov parameter.
	 *        Must be 0.0 for PATHWISE.
	 * @param reductionMethod The reduction method, e.g. PROJECTED_GALERKIN or L2.
	 */
	public ForwardSensitivityDeltaHedgedPortfolio(
			final TermStructureMonteCarloProduct productToReplicate,
			final List<TermStructureMonteCarloProduct> hedgeInstruments,
			final TimeDiscretization rebalancingTimes,
			final BasisFunctionProvider solutionBasisFunctionProvider,
			final BasisFunctionProvider testBasisFunctionProvider,
			final ParameterIDProvider parameterIDProvider,
			final HedgeInstrumentTradeValueProvider hedgeInstrumentTradeValueProvider,
			final double regularizationLambda,
			final ReductionMethod reductionMethod) {
		this(
				productToReplicate,
				hedgeInstruments,
				rebalancingTimes,
				solutionBasisFunctionProvider,
				testBasisFunctionProvider,
				parameterIDProvider,
				getProductValueProvider(),
				hedgeInstrumentTradeValueProvider,
				regularizationLambda,
				reductionMethod);
	}

	/**
	 * Full constructor allowing custom primitive, hedge-instrument value and
	 * trade-value providers. The final portfolio is marked using the product
	 * value convention. This preserves the previous behavior and is appropriate
	 * when the evaluation time is the payment time and the hedge instruments are
	 * interpreted as their cashflow products.
	 * Hedge instruments are subject to the intermediate-cashflow restriction in
	 * the class documentation.
	 *
	 * @param productToReplicate The product to replicate.
	 * @param hedgeInstruments The hedge instruments P_j.
	 * @param rebalancingTimes The times at which the hedge is rebalanced.
	 * @param solutionBasisFunctionProvider The basis functions X_q used for hedge ratios.
	 * @param testBasisFunctionProvider The basis functions Y_s used for PROJECTED_GALERKIN moments. May be null.
	 * @param parameterIDProvider Provides the primitive AAD IDs used by ForwardSensitivities.
	 * @param hedgeInstrumentValueProvider Provides the hedge-instrument values used in ForwardSensitivities.
	 * @param hedgeInstrumentTradeValueProvider Provides adapted trade values for the self-financing update.
	 * @param regularizationLambda Finite, non-negative Tikhonov parameter.
	 *        Must be 0.0 for PATHWISE.
	 * @param reductionMethod The reduction method, e.g. PROJECTED_GALERKIN or L2.
	 */
	public ForwardSensitivityDeltaHedgedPortfolio(
			final TermStructureMonteCarloProduct productToReplicate,
			final List<TermStructureMonteCarloProduct> hedgeInstruments,
			final TimeDiscretization rebalancingTimes,
			final BasisFunctionProvider solutionBasisFunctionProvider,
			final BasisFunctionProvider testBasisFunctionProvider,
			final ParameterIDProvider parameterIDProvider,
			final HedgeInstrumentValueProvider hedgeInstrumentValueProvider,
			final HedgeInstrumentTradeValueProvider hedgeInstrumentTradeValueProvider,
			final double regularizationLambda,
			final ReductionMethod reductionMethod) {
		this(
				productToReplicate,
				hedgeInstruments,
				rebalancingTimes,
				solutionBasisFunctionProvider,
				testBasisFunctionProvider,
				parameterIDProvider,
				hedgeInstrumentValueProvider,
				hedgeInstrumentTradeValueProvider,
				getProductValueProvider(),
				regularizationLambda,
				reductionMethod);
	}

	/**
	 * Full constructor allowing custom primitive, hedge-instrument value, trade-value
	 * and final marking providers. The final marking provider is useful for
	 * diagnostics where the hedge is stopped at the fixing time and should be
	 * marked with adapted tradable prices instead of product proto-values.
	 * Hedge instruments are subject to the intermediate-cashflow restriction in
	 * the class documentation.
	 *
	 * @param productToReplicate The product to replicate.
	 * @param hedgeInstruments The hedge instruments \( P_{j} \).
	 * @param rebalancingTimes The times at which the hedge is rebalanced.
	 * @param solutionBasisFunctionProvider The basis functions X_q used for hedge ratios.
	 * @param testBasisFunctionProvider The basis functions Y_s used for PROJECTED_GALERKIN moments. May be null.
	 * @param parameterIDProvider Provides the primitive AAD IDs used by ForwardSensitivities.
	 * @param hedgeInstrumentValueProvider Provides the hedge-instrument values used in ForwardSensitivities.
	 * @param hedgeInstrumentTradeValueProvider Provides adapted trade values for the self-financing update.
	 * @param finalHedgeInstrumentValueProvider Provides hedge-instrument values used for final portfolio marking.
	 * @param regularizationLambda Finite, non-negative Tikhonov parameter.
	 *        Must be 0.0 for PATHWISE.
	 * @param reductionMethod The reduction method, e.g. PROJECTED_GALERKIN or L2.
	 */
	public ForwardSensitivityDeltaHedgedPortfolio(
			final TermStructureMonteCarloProduct productToReplicate,
			final List<TermStructureMonteCarloProduct> hedgeInstruments,
			final TimeDiscretization rebalancingTimes,
			final BasisFunctionProvider solutionBasisFunctionProvider,
			final BasisFunctionProvider testBasisFunctionProvider,
			final ParameterIDProvider parameterIDProvider,
			final HedgeInstrumentValueProvider hedgeInstrumentValueProvider,
			final HedgeInstrumentTradeValueProvider hedgeInstrumentTradeValueProvider,
			final HedgeInstrumentValueProvider finalHedgeInstrumentValueProvider,
			final double regularizationLambda,
			final ReductionMethod reductionMethod) {
		this(
				productToReplicate,
				hedgeInstruments,
				rebalancingTimes,
				solutionBasisFunctionProvider,
				testBasisFunctionProvider,
				parameterIDProvider,
				hedgeInstrumentValueProvider,
				hedgeInstrumentTradeValueProvider,
				finalHedgeInstrumentValueProvider,
				regularizationLambda,
				reductionMethod,
				getDefaultHedgeRatioProvider());
	}

	/**
	 * Full constructor with a custom hedge-ratio estimator.
	 * Hedge instruments are subject to the intermediate-cashflow restriction in
	 * the class documentation.
	 *
	 * @param hedgeRatioProvider Provider that estimates hedge ratios from the raw
	 *        pathwise product and hedge-instrument gradients.
	 */
	public ForwardSensitivityDeltaHedgedPortfolio(
			final TermStructureMonteCarloProduct productToReplicate,
			final List<TermStructureMonteCarloProduct> hedgeInstruments,
			final TimeDiscretization rebalancingTimes,
			final BasisFunctionProvider solutionBasisFunctionProvider,
			final BasisFunctionProvider testBasisFunctionProvider,
			final ParameterIDProvider parameterIDProvider,
			final HedgeInstrumentValueProvider hedgeInstrumentValueProvider,
			final HedgeInstrumentTradeValueProvider hedgeInstrumentTradeValueProvider,
			final HedgeInstrumentValueProvider finalHedgeInstrumentValueProvider,
			final double regularizationLambda,
			final ReductionMethod reductionMethod,
			final HedgeRatioProvider hedgeRatioProvider) {

		super();

		this.productToReplicate = Objects.requireNonNull(productToReplicate, "productToReplicate must not be null.");
		Objects.requireNonNull(hedgeInstruments, "hedgeInstruments must not be null.");
		if(hedgeInstruments.isEmpty()) {
			throw new IllegalArgumentException("hedgeInstruments must contain at least one hedge instrument.");
		}
		this.hedgeInstruments = Collections.unmodifiableList(new ArrayList<>(hedgeInstruments));

		Objects.requireNonNull(rebalancingTimes, "rebalancingTimes must not be null.");
		if(rebalancingTimes.size() == 0) {
			throw new IllegalArgumentException("rebalancingTimes must contain at least one time.");
		}
		this.rebalancingTimes = rebalancingTimes;

		this.solutionBasisFunctionProvider = solutionBasisFunctionProvider;
		this.testBasisFunctionProvider = testBasisFunctionProvider;
		this.parameterIDProvider = Objects.requireNonNull(parameterIDProvider, "parameterIDProvider must not be null.");
		this.hedgeInstrumentValueProvider = Objects.requireNonNull(
				hedgeInstrumentValueProvider,
				"hedgeInstrumentValueProvider must not be null.");
		this.hedgeInstrumentTradeValueProvider = Objects.requireNonNull(
				hedgeInstrumentTradeValueProvider,
				"hedgeInstrumentTradeValueProvider must not be null.");
		this.finalHedgeInstrumentValueProvider = Objects.requireNonNull(
				finalHedgeInstrumentValueProvider,
				"finalHedgeInstrumentValueProvider must not be null.");
		this.hedgeRatioProvider = Objects.requireNonNull(
				hedgeRatioProvider,
				"hedgeRatioProvider must not be null.");

		if(!Double.isFinite(regularizationLambda) || regularizationLambda < 0.0) {
			throw new IllegalArgumentException("regularizationLambda must be finite and non-negative.");
		}
		this.regularizationLambda = regularizationLambda;
		this.reductionMethod = Objects.requireNonNull(reductionMethod, "reductionMethod must not be null.");
		if(reductionMethod == ReductionMethod.PATHWISE && regularizationLambda != 0.0) {
			throw new IllegalArgumentException("PATHWISE requires regularizationLambda to be 0.0.");
		}
	}

	/**
	 * Convenience constructor accepting an array of hedge instruments.
	 * Hedge instruments are subject to the intermediate-cashflow restriction in
	 * the class documentation.
	 *
	 * @param productToReplicate The product to replicate.
	 * @param hedgeInstruments The hedge instruments P_j.
	 * @param rebalancingTimes The times at which the hedge is rebalanced.
	 * @param basisFunctionProvider The basis functions X_q used for hedge ratios.
	 * @param regularizationLambda Finite, non-negative Tikhonov parameter.
	 *        Must be 0.0 for PATHWISE.
	 * @param reductionMethod The reduction method, e.g. PROJECTED_GALERKIN or L2.
	 */
	public ForwardSensitivityDeltaHedgedPortfolio(
			final TermStructureMonteCarloProduct productToReplicate,
			final TermStructureMonteCarloProduct[] hedgeInstruments,
			final TimeDiscretization rebalancingTimes,
			final BasisFunctionProvider basisFunctionProvider,
			final double regularizationLambda,
			final ReductionMethod reductionMethod) {
		this(
				productToReplicate,
				Arrays.asList(hedgeInstruments),
				rebalancingTimes,
				basisFunctionProvider,
				regularizationLambda,
				reductionMethod);
	}

	/**
	 * Values the self-financing hedge at {@code evaluationTime}.
	 *
	 * <p>
	 * Intermediate hedge cashflows are not booked separately. Known built-in
	 * hedge products with a payment strictly before {@code evaluationTime} are
	 * rejected; callers using custom products must ensure that their values carry
	 * any earlier cashflows as a total-return value.
	 * </p>
	 *
	 * @param evaluationTime The final portfolio evaluation time.
	 * @param model The term-structure Monte-Carlo model.
	 * @return The value of the hedge portfolio.
	 * @throws CalculationException Thrown if model quantities cannot be calculated.
	 */
	@Override
	public RandomVariable getValue(
			final double evaluationTime,
			final TermStructureMonteCarloSimulationModel model) throws CalculationException {

		final long timingStart = System.currentTimeMillis();
		validateNoIntermediateHedgeCashflows(evaluationTime, model);
		long timingValuationMillis = 0L;
		long timingHedgeRatioMillis = 0L;
		long timingTradeValueMillis = 0L;
		long timingHedgeRatioProjectMillis = 0L;
		long timingHedgeRatioSolveMillis = 0L;

		/*
		 * Initial funding: start with the time-0 price of the product in the
		 * numeraire account and zero positions in all hedge instruments.
		 */
		final long timingInitialValuationStart = System.currentTimeMillis();
		final RandomVariable initialProductValue = productToReplicate.getValue(0.0, model);
		RandomVariable amountOfNumeraireAsset = model.getRandomVariableForConstant(initialProductValue.getAverage()).div(model.getNumeraire(0.0));
		timingValuationMillis += System.currentTimeMillis() - timingInitialValuationStart;

		RandomVariable[] hedgeInstrumentPositions = new RandomVariable[hedgeInstruments.size()];
		for(int hedgeIndex = 0; hedgeIndex < hedgeInstrumentPositions.length; hedgeIndex++) {
			hedgeInstrumentPositions[hedgeIndex] = model.getRandomVariableForConstant(0.0);
		}

		final List<Double> rebalancedTimes = new ArrayList<>();
		final List<Map<String, Long>> parameterIDsByNameHistory = new ArrayList<>();
		final List<ProjectedHedgeRatioResult> hedgeRatioResults = new ArrayList<>();

		/*
		 * Note: There are two possible ways of doing this here:
		 * 1) we can use the proto values V(0) and P(0) to calculate dV(0)/dM(t) and dP(0)/dM(t) and infer dV(t)/dP(t), or,
		 * 2) we can use the proto values V(t) and P(t) to calculate dV(t)/dM(t) and dP(t)/dM(t) and infer dV(t)/dP(t).
		 * The second option is much slower. The first works for sensitivities because
		 * V(t) w(t) / N(t) - V(0) w(0) / N(0) does not depend on M(t).
		 * Trade values are nevertheless recomputed from current-time proto values below,
		 * preserving their units and ex-dividend cash-flow semantics.
		 */
		final RandomVariable derivativeProtoValue = productToReplicate.getValue(0.0, model);
		final RandomVariable[] hedgeInstrumentSensitivityProtoValues = hedgeInstrumentValueProvider.getValues(0.0, model, hedgeInstruments);
		if(hedgeInstrumentSensitivityProtoValues.length != hedgeInstrumentPositions.length) {
			throw new IllegalStateException(
					"Hedge-instrument sensitivity-value provider returned "
					+ hedgeInstrumentSensitivityProtoValues.length + " values for "
					+ hedgeInstrumentPositions.length + " hedge instruments.");
		}
		final Map<Long, RandomVariable> derivativeGradient = ((RandomVariableDifferentiable)derivativeProtoValue).getGradient();
		final List<Map<Long, RandomVariable>> hedgePortfolioGradients = new ArrayList<Map<Long, RandomVariable>>(hedgeInstrumentSensitivityProtoValues.length);

		for(final RandomVariable hedgeInstrumentProtoValue : hedgeInstrumentSensitivityProtoValues) {
			if(hedgeInstrumentProtoValue instanceof RandomVariableDifferentiable) {
				hedgePortfolioGradients.add(((RandomVariableDifferentiable)hedgeInstrumentProtoValue).getGradient());
			}
			else {
				hedgePortfolioGradients.add(Java8BackportUtil.Map.<Long, RandomVariable>of());
			}
		}

		for(final double rebalancingTime : rebalancingTimes) {

			if(rebalancingTime < 0.0 || rebalancingTime >= evaluationTime) {
				continue;
			}

			/*
			 * Materialize the process state at the rebalancing time first. The
			 * product values calculated below should then be linked to the same
			 * cached AAD nodes of the process state.
			 */
			final long timingParameterStart = System.currentTimeMillis();
			final Map<String, Long> parameterIDsByName = parameterIDProvider.getParameterIDs(rebalancingTime, model);
			timingValuationMillis += System.currentTimeMillis() - timingParameterStart;

			final long timingValuationStart = System.currentTimeMillis();

			final RandomVariable[] solutionBasisFunctions = solutionBasisFunctionProvider != null ? solutionBasisFunctionProvider.getBasisFunctions(rebalancingTime, model) : null;
			final RandomVariable[] testBasisFunctions = testBasisFunctionProvider != null ? testBasisFunctionProvider.getBasisFunctions(rebalancingTime, model) : null;
			final RandomVariable[] hedgeInstrumentTradeProtoValues = getHedgeInstrumentProductValues(
					rebalancingTime,
					model);

			final RandomVariable numeraireAtRebalancingTime = model.getNumeraire(rebalancingTime).getValues();

			timingValuationMillis += System.currentTimeMillis() - timingValuationStart;
			if(hedgeInstrumentTradeProtoValues.length != hedgeInstrumentPositions.length) {
				throw new IllegalStateException(
						"Current-time product valuation returned " + hedgeInstrumentTradeProtoValues.length
						+ " values for " + hedgeInstrumentPositions.length + " hedge instruments.");
			}

			final long timingTradeValueStart = System.currentTimeMillis();
			final RandomVariable[] hedgeInstrumentTradeValues = hedgeInstrumentTradeValueProvider.getTradeValues(
					rebalancingTime,
					model,
					hedgeInstruments,
					hedgeInstrumentTradeProtoValues,
					solutionBasisFunctions);
			timingTradeValueMillis += System.currentTimeMillis() - timingTradeValueStart;

			final long timingHedgeRatioStart = System.currentTimeMillis();
			final ProjectedHedgeRatioResult hedgeRatioResult = hedgeRatioProvider.getHedgeRatios(
					parameterIDsByName,
					rebalancingTime,
					derivativeGradient,
					hedgePortfolioGradients,
					solutionBasisFunctions,
					testBasisFunctions,
					regularizationLambda,
					reductionMethod, derivativeProtoValue.size());
			timingHedgeRatioMillis += System.currentTimeMillis() - timingHedgeRatioStart;

			timingHedgeRatioProjectMillis += hedgeRatioResult.getTimings().getTimingProjectSystem();
			timingHedgeRatioSolveMillis += hedgeRatioResult.getTimings().getTimingSolveSystem();

			final RandomVariable[] newHedgeInstrumentPositions = hedgeRatioResult.getHedgeRatios();
			if(newHedgeInstrumentPositions.length != hedgeInstrumentPositions.length) {
				throw new IllegalStateException(
						"ForwardSensitivities returned " + newHedgeInstrumentPositions.length
						+ " hedge ratios for " + hedgeInstrumentPositions.length + " hedge instruments.");
			}
			if(hedgeInstrumentTradeValues.length != hedgeInstrumentPositions.length) {
				throw new IllegalStateException(
						"Trade-value provider returned " + hedgeInstrumentTradeValues.length
						+ " trade values for " + hedgeInstrumentPositions.length + " hedge instruments.");
			}

			/*
			 * Self-financing rebalancing: buy/sell hedge instruments at adapted
			 * trade values and finance the trade via the numeraire account.
			 */
			RandomVariable valueOfHedgeInstrumentsToBuy = model.getRandomVariableForConstant(0.0);
			for(int hedgeIndex = 0; hedgeIndex < hedgeInstrumentPositions.length; hedgeIndex++) {
				final RandomVariable hedgeInstrumentPositionChange = newHedgeInstrumentPositions[hedgeIndex]
						.sub(hedgeInstrumentPositions[hedgeIndex]);
				valueOfHedgeInstrumentsToBuy = valueOfHedgeInstrumentsToBuy
						.add(hedgeInstrumentPositionChange.mult(hedgeInstrumentTradeValues[hedgeIndex]));
			}

			amountOfNumeraireAsset = amountOfNumeraireAsset
					.sub(valueOfHedgeInstrumentsToBuy.div(numeraireAtRebalancingTime));
			hedgeInstrumentPositions = newHedgeInstrumentPositions;

			rebalancedTimes.add(rebalancingTime);
			parameterIDsByNameHistory.add(Collections.unmodifiableMap(new LinkedHashMap<>(parameterIDsByName)));
			hedgeRatioResults.add(hedgeRatioResult);
		}

		/*
		 * No rebalance occurs exactly at evaluationTime. Mark the remaining positions
		 * with the configured final provider, whose product/analytic convention
		 * determines the final ex-dividend or total-return semantics.
		 */
		final long timingFinalValuationStart = System.currentTimeMillis();
		RandomVariable portfolioValue = amountOfNumeraireAsset.mult(model.getNumeraire(evaluationTime));
		final RandomVariable[] hedgeInstrumentValuesAtEvaluationTime = finalHedgeInstrumentValueProvider.getValues(
				evaluationTime,
				model,
				hedgeInstruments);
		for(int hedgeIndex = 0; hedgeIndex < hedgeInstrumentPositions.length; hedgeIndex++) {
			portfolioValue = portfolioValue.add(
					hedgeInstrumentPositions[hedgeIndex].mult(hedgeInstrumentValuesAtEvaluationTime[hedgeIndex]));
		}
		timingValuationMillis += System.currentTimeMillis() - timingFinalValuationStart;

		lastOperationTimingTotal = (System.currentTimeMillis() - timingStart) / 1000.0;
		lastOperationTimingValuation = timingValuationMillis / 1000.0;
		lastOperationTimingHedgeRatios = timingHedgeRatioMillis / 1000.0;
		lastOperationTimingHedgeRatioProject = timingHedgeRatioProjectMillis / 1000.0;
		lastOperationTimingHedgeRatioSolve = timingHedgeRatioSolveMillis / 1000.0;
		lastOperationTimingTradeValues = timingTradeValueMillis / 1000.0;
		lastRebalancingTimes = Collections.unmodifiableList(new ArrayList<>(rebalancedTimes));
		lastParameterIDsByName = Collections.unmodifiableList(new ArrayList<>(parameterIDsByNameHistory));
		lastHedgeRatioResults = Collections.unmodifiableList(new ArrayList<>(hedgeRatioResults));
		lastHedgeInstrumentPositions = hedgeInstrumentPositions.clone();
		lastNumerairePosition = amountOfNumeraireAsset;

		return portfolioValue;
	}

	private void validateNoIntermediateHedgeCashflows(
			final double evaluationTime,
			final TermStructureMonteCarloSimulationModel model) {

		for(final TermStructureMonteCarloProduct hedgeInstrument : hedgeInstruments) {
			if(hedgeInstrument instanceof Bond) {
				final Bond bond = (Bond)hedgeInstrument;
				validatePaymentTime(
						"Bond",
						bond.getProductToModelTimeOffset(model) + bond.getMaturity(),
						evaluationTime);
			}
			else if(hedgeInstrument instanceof SwapAnnuity) {
				final SwapAnnuity swapAnnuity = (SwapAnnuity)hedgeInstrument;
				final double offset = swapAnnuity.getProductToModelTimeOffset(model);
				final double[] maturities = swapAnnuity.getMaturities();
				final double[] periodLengths = swapAnnuity.getPeriodLengths();
				for(int periodIndex = 0; periodIndex < maturities.length; periodIndex++) {
					if(periodLengths[periodIndex] != 0.0) {
						validatePaymentTime(
								"SwapAnnuity payment " + periodIndex,
								offset + maturities[periodIndex],
								evaluationTime);
					}
				}
			}
			else if(hedgeInstrument instanceof DiscreteTenorRollOver) {
				validatePaymentTime(
						"DiscreteTenorRollOver",
						((DiscreteTenorRollOver)hedgeInstrument).getPaymentTime(),
						evaluationTime);
			}
		}
	}

	private static void validatePaymentTime(
			final String hedgeInstrumentDescription,
			final double paymentTime,
			final double evaluationTime) {

		if(paymentTime < evaluationTime - Bond.PAYMENT_TIME_TOLERANCE) {
			throw new IllegalArgumentException(
					hedgeInstrumentDescription + " pays at " + paymentTime
					+ ", strictly before portfolio evaluationTime " + evaluationTime
					+ ". ForwardSensitivityDeltaHedgedPortfolio does not book intermediate hedge cashflows; "
					+ "use a custom total-return hedge product instead.");
		}
	}

	/**
	 * Returns the standard in-process hedge-ratio estimator.
	 *
	 * @return A provider delegating to {@link ForwardSensitivities}.
	 */
	public static HedgeRatioProvider getDefaultHedgeRatioProvider() {
		return ForwardSensitivities::getHedgeRatios;
	}

	private RandomVariable[] getHedgeInstrumentProductValues(
			final double evaluationTime,
			final TermStructureMonteCarloSimulationModel model) throws CalculationException {

		final RandomVariable[] values = new RandomVariable[hedgeInstruments.size()];
		for(int hedgeIndex = 0; hedgeIndex < hedgeInstruments.size(); hedgeIndex++) {
			values[hedgeIndex] = hedgeInstruments.get(hedgeIndex).getValue(evaluationTime, model);
		}
		return values;
	}

	/**
	 * Hedge-instrument value provider using each product's getValue method.
	 *
	 * @return A provider returning product.getValue(t, model).
	 */
	public static HedgeInstrumentValueProvider getProductValueProvider() {
		return (evaluationTime, model, hedgeInstruments) -> {
			final RandomVariable[] values = new RandomVariable[hedgeInstruments.size()];
			for(int hedgeIndex = 0; hedgeIndex < hedgeInstruments.size(); hedgeIndex++) {
				values[hedgeIndex] = hedgeInstruments.get(hedgeIndex).getValue(evaluationTime, model);
			}
			return values;
		};
	}

	/**
	 * Hedge-instrument value provider valuing {@link Bond}s, the bond components
	 * of {@link SwapAnnuity}s, and {@link DiscreteTenorRollOver}s by the model's
	 * forward-discount-bond implementation.
	 *
	 * <p>
	 * For a bond with maturity T greater than t, the provider returns the
	 * model-implied adapted bond price
	 *
	 * \[
	 *     P(t,T) = E\left(\frac{N(t)}{N(T)} \mid \mathcal F_t\right),
	 * \]
	 *
	 * using {@code model.getModel().getForwardDiscountBond(model.getProcess(), t, T)}.
	 * This keeps the hedge-instrument value consistent with the model's numeraire,
	 * measure, interpolation and any deterministic discount-curve adjustment.
	 * Differentiable values returned by the model are preserved so this provider
	 * can safely be used as the AAD sensitivity-value provider.
	 * </p>
	 *
	 * @param tenorPeriodLength Kept for backward compatibility. It is not used by
	 *        the model-implied bond valuation.
	 * @return A hedge-instrument value provider with model-implied analytic bond values.
	 */
	public static HedgeInstrumentValueProvider getAnalyticBondValueProvider(final double tenorPeriodLength) {
		return (evaluationTime, model, hedgeInstruments) -> {
			final RandomVariable[] values = new RandomVariable[hedgeInstruments.size()];
			for(int hedgeIndex = 0; hedgeIndex < hedgeInstruments.size(); hedgeIndex++) {
				final TermStructureMonteCarloProduct hedgeInstrument = hedgeInstruments.get(hedgeIndex);
				if(hedgeInstrument instanceof Bond) {
					final Bond bond = (Bond)hedgeInstrument;
					final double maturity = bond.getProductToModelTimeOffset(model) + bond.getMaturity();
					values[hedgeIndex] = getAnalyticBondValue(
							evaluationTime,
							maturity,
							tenorPeriodLength,
							model);
				}
				else if(hedgeInstrument instanceof SwapAnnuity) {
					values[hedgeIndex] = Scalar.of(0.0);
					final SwapAnnuity swapAnnuity = (SwapAnnuity)hedgeInstrument;
					final double[] maturities = swapAnnuity.getMaturities();
					final double[] periodLengths = swapAnnuity.getPeriodLengths();
					final double productToModelTimeOffset = swapAnnuity.getProductToModelTimeOffset(model);
					for(int timeIndex = 0; timeIndex<maturities.length; timeIndex++) {
						final double paymentTime = productToModelTimeOffset + maturities[timeIndex];
						if(paymentTime < evaluationTime - Bond.PAYMENT_TIME_TOLERANCE) {
							continue;
						}
						values[hedgeIndex] = values[hedgeIndex].add(getAnalyticBondValue(
								evaluationTime,
								paymentTime,
								tenorPeriodLength,
								model).mult(periodLengths[timeIndex]));
					}
				}
				else if(hedgeInstrument instanceof DiscreteTenorRollOver) {
					/*
					 * The product owns the piecewise fixing/payment semantics and already
					 * uses adapted forward-discount bonds in each pre-payment phase.
					 */
					values[hedgeIndex] = hedgeInstrument.getValue(evaluationTime, model);
				}
				else {
					// Using the proto value will generate biases, due to correlation (need cond. exp. first).
					// Option: Add numerical cond. expectation.
					throw new UnsupportedOperationException("The hedge instrument does not have an analytic proxy. We need an analytic proxy here for benchmarking.");
				}
			}
			return values;
		};
	}

	/**
	 * Trade-value provider using the analytic hedge-instrument values supplied by
	 * {@link #getAnalyticBondValueProvider(double)}. Differentiation wrappers are
	 * removed from the returned trade values since they are not sensitivity inputs.
	 *
	 * @param tenorPeriodLength The tenor period length used to form analytic bond prices.
	 * @return A trade-value provider with analytic bond values.
	 */
	public static HedgeInstrumentTradeValueProvider getAnalyticBondTradeValueProvider(final double tenorPeriodLength) {
		final HedgeInstrumentValueProvider valueProvider = getAnalyticBondValueProvider(tenorPeriodLength);
		return (evaluationTime, model, hedgeInstruments, hedgeInstrumentProtoValues, conditioningBasisFunctions) -> {
			final RandomVariable[] values = valueProvider.getValues(evaluationTime, model, hedgeInstruments);
			for(int hedgeIndex = 0; hedgeIndex < values.length; hedgeIndex++) {
				values[hedgeIndex] = values[hedgeIndex].getValues();
			}
			return values;
		};
	}

	/**
	 * Model-implied analytic bond value.
	 *
	 * <p>
	 * This method intentionally does not rebuild the discount bond as a raw product
	 * over the model's current forward rates. Instead, it delegates to the
	 * term-structure model's forward-discount-bond implementation, which is the
	 * adapted conditional expectation of the numeraire ratio and is therefore
	 * consistent with the model's numeraire, interpolation and optional
	 * discount-curve adjustment.
	 * </p>
	 *
	 * @param evaluationTime The valuation time t.
	 * @param maturity The bond maturity T.
	 * @param tenorPeriodLength Kept for backward compatibility. It is not used by
	 *        the model-implied bond valuation.
	 * @param model The term-structure Monte-Carlo model.
	 * @return The model-implied adapted bond value P(t,T), or zero after the
	 *         bond has paid.
	 * @throws CalculationException Thrown if the model cannot calculate the bond value.
	 */
	public static RandomVariable getAnalyticBondValue(
			final double evaluationTime,
			final double maturity,
			final double tenorPeriodLength,
			final TermStructureMonteCarloSimulationModel model) throws CalculationException {

		final double tolerance = Bond.PAYMENT_TIME_TOLERANCE;
		if(maturity <= evaluationTime + tolerance) {
			if(maturity >= evaluationTime - tolerance) {
				return model.getRandomVariableForConstant(1.0);
			}
			return model.getRandomVariableForConstant(0.0);
		}

		return model.getModel().getForwardDiscountBond(
				model.getProcess(),
				evaluationTime,
				maturity);
	}

	/**
	 * Single-curve product-of-forwards diagnostic value.
	 *
	 * <p>
	 * This is useful for diagnostics, but it is not used by the analytic bond
	 * hedge-value provider. It can differ from the model-implied bond value if
	 * the model uses a deterministic discounting adjustment, a different
	 * interpolation convention, or if {@code getForwardRate(t,S,T)} for a long
	 * period is not equivalent to chaining all short-tenor forward rates.
	 * </p>
	 *
	 * @param evaluationTime The valuation time t.
	 * @param maturity The bond maturity T.
	 * @param tenorPeriodLength The tenor period length used to form the product.
	 * @param model The term-structure Monte-Carlo model.
	 * @return The product-of-forwards value.
	 * @throws CalculationException Thrown if a forward rate cannot be obtained.
	 */
	public static RandomVariable getForwardProductBondValue(
			final double evaluationTime,
			final double maturity,
			final double tenorPeriodLength,
			final TermStructureMonteCarloSimulationModel model) throws CalculationException {

		if(tenorPeriodLength <= 0.0) {
			throw new IllegalArgumentException("tenorPeriodLength must be positive.");
		}

		final double tolerance = 1E-12;
		if(maturity <= evaluationTime + tolerance) {
			if(maturity >= evaluationTime - tolerance) {
				return model.getRandomVariableForConstant(1.0);
			}
			return model.getRandomVariableForConstant(1.0)
					.div(model.getNumeraire(maturity))
					.mult(model.getMonteCarloWeights(maturity))
					.mult(model.getNumeraire(evaluationTime))
					.div(model.getMonteCarloWeights(evaluationTime));
		}

		RandomVariable bondValue = model.getRandomVariableForConstant(1.0);
		double periodStart = evaluationTime;

		while(periodStart < maturity - tolerance) {
			final double nextTenorTime = getNextTenorTimeStrictlyAfter(periodStart, tenorPeriodLength, tolerance);
			final double periodEnd = Math.min(maturity, nextTenorTime);
			final double periodLength = periodEnd - periodStart;

			if(periodLength <= tolerance) {
				break;
			}

			final RandomVariable forwardRate = model.getForwardRate(evaluationTime, periodStart, periodEnd);
			final RandomVariable onePlusForwardRateTimesPeriodLength = model.getRandomVariableForConstant(1.0)
					.add(forwardRate.mult(periodLength));
			bondValue = bondValue.div(onePlusForwardRateTimesPeriodLength);

			periodStart = periodEnd;
		}

		return bondValue;
	}

	private static double getNextTenorTimeStrictlyAfter(
			final double time,
			final double tenorPeriodLength,
			final double tolerance) {

		final double scaledTime = time / tenorPeriodLength;
		double nextTenorTime = Math.floor(scaledTime + tolerance) * tenorPeriodLength + tenorPeriodLength;

		if(nextTenorTime <= time + tolerance) {
			nextTenorTime += tenorPeriodLength;
		}

		return nextTenorTime;
	}

	/**
	 * Default primitive provider: use the differentiable process state at the
	 * rebalancing time.
	 *
	 * @return A primitive provider based on model.getProcess().getProcessValue(timeIndex).
	 */
	public static ParameterIDProvider getProcessStateParameterIDProvider() {
		return getProcessStateParameterIDProvider(0);
	}

	public static ParameterIDProvider getProcessStateParameterIDProvider(int lag) {
		return (evaluationTime, model) -> {

			int processTimeIndex = model.getTimeIndex(evaluationTime);
			if(processTimeIndex < 0) {
				processTimeIndex = model.getProcess().getTimeDiscretization().getTimeIndexNearestLessOrEqual(evaluationTime);
			}
			if(processTimeIndex < 0) {
				throw new IllegalArgumentException("Could not find process time index for evaluationTime " + evaluationTime + ".");
			}

			final List<RandomVariable> modelPrimitivesList = new ArrayList<>();
			for(int processTimeIndexLagged = Math.max(processTimeIndex-lag, 0); processTimeIndexLagged<=processTimeIndex; processTimeIndexLagged++) {
				final RandomVariable[] modelPrimitivesLagged = model.getProcess().getProcessValue(processTimeIndexLagged);
				modelPrimitivesList.addAll(Arrays.asList(modelPrimitivesLagged));
			}
			final RandomVariable[] modelPrimitives = modelPrimitivesList.toArray(new RandomVariable[modelPrimitivesList.size()]);

			final Map<String, Long> parameterIDsByName = new LinkedHashMap<>();

			for(int componentIndex = 0; componentIndex < modelPrimitives.length; componentIndex++) {
				final RandomVariable primitive = modelPrimitives[componentIndex];
				if(!(primitive instanceof RandomVariableDifferentiable)) {
					throw new IllegalArgumentException(
							"Process primitive (" + evaluationTime + "," + componentIndex + ") is not differentiable. "
									+ "Check that the model was created with RandomVariableDifferentiableAADFactory.");
				}

				parameterIDsByName.put(
						"(" + evaluationTime + "," + componentIndex + ")",
						((RandomVariableDifferentiable)primitive).getID());
			}

			if(parameterIDsByName.isEmpty()) {
				throw new IllegalArgumentException("No differentiable process primitives found at time " + evaluationTime + ".");
			}

			return parameterIDsByName;
		};
	}

	/**
	 * Default primitive provider: use the differentiable process state at the
	 * rebalancing time.
	 *
	 * @return A primitive provider based on model.getProcess().getProcessValue(timeIndex).
	 */
	public static ParameterIDProvider getForwardRateIDProvider(TimeDiscretization tenorDiscretization) {
		return (evaluationTime, model) -> {

			int processTimeIndex = model.getTimeIndex(evaluationTime);
			if(processTimeIndex < 0) {
				processTimeIndex = model.getProcess().getTimeDiscretization().getTimeIndexNearestLessOrEqual(evaluationTime);
			}
			if(processTimeIndex < 0) {
				throw new IllegalArgumentException("Could not find process time index for evaluationTime " + evaluationTime + ".");
			}

			final List<RandomVariable> forwardRates = new ArrayList<>();
			final int indexRateStart = tenorDiscretization.getTimeIndexNearestGreaterOrEqual(evaluationTime);
			final int indexRateEnd = tenorDiscretization.getNumberOfTimes()-1;
			for(int indexPeriodStart = indexRateStart; indexPeriodStart<indexRateEnd; indexPeriodStart++) {
				final RandomVariable forwardRate = model.getForwardRate(evaluationTime, tenorDiscretization.getTime(indexPeriodStart), tenorDiscretization.getTime(indexPeriodStart+1));
				forwardRates.add(forwardRate);
			}

			final RandomVariable[] modelPrimitives = forwardRates.toArray(new RandomVariable[forwardRates.size()]);

			final Map<String, Long> parameterIDsByName = new LinkedHashMap<>();
			for(int componentIndex = 0; componentIndex < modelPrimitives.length; componentIndex++) {
				final RandomVariable primitive = modelPrimitives[componentIndex];
				if(!(primitive instanceof RandomVariableDifferentiable)) {
					throw new IllegalArgumentException(
							"Process primitive (" + evaluationTime + "," + componentIndex + ") is not differentiable. "
									+ "Check that the model was created with RandomVariableDifferentiableAADFactory.");
				}
				else {
					parameterIDsByName.put(
							"(" + evaluationTime + "," + componentIndex + ")",
							((RandomVariableDifferentiable)primitive).getID());
				}
			}

			if(parameterIDsByName.isEmpty()) {
				throw new IllegalArgumentException("No differentiable process primitives found at time " + evaluationTime + ".");
			}

			return parameterIDsByName;
		};
	}

	/**
	 * Default trade-value provider: regress proto-values on the supplied
	 * conditioning basis. At time zero it returns constants equal to the Monte-Carlo
	 * averages, which is the correct static trade price. If no conditioning basis
	 * is supplied, it uses a constant basis. This keeps a raw PATHWISE hedge usable
	 * without conflating its absent solution basis with the basis required to adapt
	 * trade values; the resulting trade values are unconditional adapted prices.
	 *
	 * @return A trade-value provider based on conditional-expectation regression.
	 */
	public static HedgeInstrumentTradeValueProvider getRegressionTradeValueProvider() {
		return (evaluationTime, model, hedgeInstruments, hedgeInstrumentProtoValues, conditioningBasisFunctions) -> {

			Objects.requireNonNull(hedgeInstrumentProtoValues, "hedgeInstrumentProtoValues must not be null.");

			final RandomVariable[] tradeValues = new RandomVariable[hedgeInstrumentProtoValues.length];

			if(evaluationTime == 0.0) {
				for(int hedgeIndex = 0; hedgeIndex < tradeValues.length; hedgeIndex++) {
					tradeValues[hedgeIndex] = model.getRandomVariableForConstant(hedgeInstrumentProtoValues[hedgeIndex].getAverage());
				}
				return tradeValues;
			}

			final RandomVariable[] effectiveConditioningBasisFunctions =
					conditioningBasisFunctions == null || conditioningBasisFunctions.length == 0
					? new RandomVariable[] { model.getRandomVariableForConstant(1.0) }
					: conditioningBasisFunctions;

			final ConditionalExpectationEstimator conditionalExpectationOperator =
					new MonteCarloConditionalExpectationRegression(effectiveConditioningBasisFunctions);

			for(int hedgeIndex = 0; hedgeIndex < tradeValues.length; hedgeIndex++) {
				tradeValues[hedgeIndex] = hedgeInstrumentProtoValues[hedgeIndex]
						.getConditionalExpectation(conditionalExpectationOperator);
			}

			return tradeValues;
		};
	}

	public TermStructureMonteCarloProduct getProductToReplicate() {
		return productToReplicate;
	}

	public List<TermStructureMonteCarloProduct> getHedgeInstruments() {
		return hedgeInstruments;
	}

	public TimeDiscretization getRebalancingTimes() {
		return rebalancingTimes;
	}

	public double getRegularizationLambda() {
		return regularizationLambda;
	}

	public ReductionMethod getReductionMethod() {
		return reductionMethod;
	}

	public double getLastOperationTimingTotal() {
		return lastOperationTimingTotal;
	}

	public double getLastOperationTimingValuation() {
		return lastOperationTimingValuation;
	}

	public double getLastOperationTimingHedgeRatios() {
		return lastOperationTimingHedgeRatios;
	}

	public double getLastOperationTimingHedgeRatioProject() {
		return lastOperationTimingHedgeRatioProject;
	}

	public double getLastOperationTimingHedgeRatioSolve() {
		return lastOperationTimingHedgeRatioSolve;
	}

	public double getLastOperationTimingTradeValues() {
		return lastOperationTimingTradeValues;
	}

	public List<Double> getLastRebalancingTimes() {
		return lastRebalancingTimes;
	}

	public List<Map<String, Long>> getLastParameterIDsByName() {
		return lastParameterIDsByName;
	}

	public List<ProjectedHedgeRatioResult> getLastHedgeRatioResults() {
		return lastHedgeRatioResults;
	}

	public RandomVariable[] getLastHedgeInstrumentPositions() {
		return lastHedgeInstrumentPositions.clone();
	}

	public RandomVariable getLastNumerairePosition() {
		return lastNumerairePosition;
	}

	public HedgeInstrumentValueProvider getFinalHedgeInstrumentValueProvider() {
		return finalHedgeInstrumentValueProvider;
	}
}
