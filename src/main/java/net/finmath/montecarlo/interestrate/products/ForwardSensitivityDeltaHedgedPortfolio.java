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
import net.finmath.time.TimeDiscretization;

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
		 * @param hedgeInstrumentProtoValues The raw product values returned by getValue(t, model).
		 * @param conditioningBasisFunctions Basis functions used for conditional-expectation projection.
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

	private final TermStructureMonteCarloProduct productToReplicate;
	private final List<TermStructureMonteCarloProduct> hedgeInstruments;
	private final double[] rebalancingTimes;
	private final BasisFunctionProvider solutionBasisFunctionProvider;
	private final BasisFunctionProvider testBasisFunctionProvider;
	private final ParameterIDProvider parameterIDProvider;
	private final HedgeInstrumentValueProvider hedgeInstrumentValueProvider;
	private final HedgeInstrumentTradeValueProvider hedgeInstrumentTradeValueProvider;
	private final double regularizationLambda;
	private final ReductionMethod reductionMethod;

	private double lastOperationTimingTotal = Double.NaN;
	private double lastOperationTimingValuation = Double.NaN;
	private double lastOperationTimingHedgeRatios = Double.NaN;
	private double lastOperationTimingTradeValues = Double.NaN;

	private List<Double> lastRebalancingTimes = Collections.emptyList();
	private List<Map<String, Long>> lastParameterIDsByName = Collections.emptyList();
	private List<ProjectedHedgeRatioResult> lastHedgeRatioResults = Collections.emptyList();
	private RandomVariable[] lastHedgeInstrumentPositions = new RandomVariable[0];
	private RandomVariable lastNumerairePosition;

	/**
	 * Creates a self-financing hedge using the same basis for solution and test
	 * functions, the process-state primitive provider, and regression trade values.
	 *
	 * @param productToReplicate The product to replicate.
	 * @param hedgeInstruments The hedge instruments P_j.
	 * @param rebalancingTimes The times at which the hedge is rebalanced.
	 * @param basisFunctionProvider The basis functions X_q used for the hedge ratios.
	 * @param regularizationLambda Tikhonov regularization parameter. Use 0.0 for none.
	 * @param reductionMethod The reduction method, e.g. PROJECTED_GALERKIN or L2.
	 */
	public ForwardSensitivityDeltaHedgedPortfolio(
			final TermStructureMonteCarloProduct productToReplicate,
			final List<TermStructureMonteCarloProduct> hedgeInstruments,
			final double[] rebalancingTimes,
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
	 *
	 * @param productToReplicate The product to replicate.
	 * @param hedgeInstruments The hedge instruments P_j.
	 * @param rebalancingTimes The times at which the hedge is rebalanced.
	 * @param solutionBasisFunctionProvider The basis functions X_q used for hedge ratios.
	 * @param testBasisFunctionProvider The basis functions Y_s used for PROJECTED_GALERKIN moments. May be null.
	 * @param regularizationLambda Tikhonov regularization parameter. Use 0.0 for none.
	 * @param reductionMethod The reduction method, e.g. PROJECTED_GALERKIN or L2.
	 */
	public ForwardSensitivityDeltaHedgedPortfolio(
			final TermStructureMonteCarloProduct productToReplicate,
			final List<TermStructureMonteCarloProduct> hedgeInstruments,
			final double[] rebalancingTimes,
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
	 *
	 * @param productToReplicate The product to replicate.
	 * @param hedgeInstruments The hedge instruments P_j.
	 * @param rebalancingTimes The times at which the hedge is rebalanced.
	 * @param solutionBasisFunctionProvider The basis functions X_q used for hedge ratios.
	 * @param testBasisFunctionProvider The basis functions Y_s used for PROJECTED_GALERKIN moments. May be null.
	 * @param parameterIDProvider Provides the primitive AAD IDs used by ForwardSensitivities.
	 * @param hedgeInstrumentTradeValueProvider Provides adapted trade values for the self-financing update.
	 * @param regularizationLambda Tikhonov regularization parameter. Use 0.0 for none.
	 * @param reductionMethod The reduction method, e.g. PROJECTED_GALERKIN or L2.
	 */
	public ForwardSensitivityDeltaHedgedPortfolio(
			final TermStructureMonteCarloProduct productToReplicate,
			final List<TermStructureMonteCarloProduct> hedgeInstruments,
			final double[] rebalancingTimes,
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
	 * trade-value providers.
	 *
	 * @param productToReplicate The product to replicate.
	 * @param hedgeInstruments The hedge instruments P_j.
	 * @param rebalancingTimes The times at which the hedge is rebalanced.
	 * @param solutionBasisFunctionProvider The basis functions X_q used for hedge ratios.
	 * @param testBasisFunctionProvider The basis functions Y_s used for PROJECTED_GALERKIN moments. May be null.
	 * @param parameterIDProvider Provides the primitive AAD IDs used by ForwardSensitivities.
	 * @param hedgeInstrumentValueProvider Provides the hedge-instrument values used in ForwardSensitivities.
	 * @param hedgeInstrumentTradeValueProvider Provides adapted trade values for the self-financing update.
	 * @param regularizationLambda Tikhonov regularization parameter. Use 0.0 for none.
	 * @param reductionMethod The reduction method, e.g. PROJECTED_GALERKIN or L2.
	 */
	public ForwardSensitivityDeltaHedgedPortfolio(
			final TermStructureMonteCarloProduct productToReplicate,
			final List<TermStructureMonteCarloProduct> hedgeInstruments,
			final double[] rebalancingTimes,
			final BasisFunctionProvider solutionBasisFunctionProvider,
			final BasisFunctionProvider testBasisFunctionProvider,
			final ParameterIDProvider parameterIDProvider,
			final HedgeInstrumentValueProvider hedgeInstrumentValueProvider,
			final HedgeInstrumentTradeValueProvider hedgeInstrumentTradeValueProvider,
			final double regularizationLambda,
			final ReductionMethod reductionMethod) {

		super();

		this.productToReplicate = Objects.requireNonNull(productToReplicate, "productToReplicate must not be null.");
		Objects.requireNonNull(hedgeInstruments, "hedgeInstruments must not be null.");
		if(hedgeInstruments.isEmpty()) {
			throw new IllegalArgumentException("hedgeInstruments must contain at least one hedge instrument.");
		}
		this.hedgeInstruments = Collections.unmodifiableList(new ArrayList<>(hedgeInstruments));

		Objects.requireNonNull(rebalancingTimes, "rebalancingTimes must not be null.");
		if(rebalancingTimes.length == 0) {
			throw new IllegalArgumentException("rebalancingTimes must contain at least one time.");
		}
		this.rebalancingTimes = rebalancingTimes.clone();
		Arrays.sort(this.rebalancingTimes);

		this.solutionBasisFunctionProvider = Objects.requireNonNull(
				solutionBasisFunctionProvider,
				"solutionBasisFunctionProvider must not be null.");
		this.testBasisFunctionProvider = testBasisFunctionProvider;
		this.parameterIDProvider = Objects.requireNonNull(parameterIDProvider, "parameterIDProvider must not be null.");
		this.hedgeInstrumentValueProvider = Objects.requireNonNull(
				hedgeInstrumentValueProvider,
				"hedgeInstrumentValueProvider must not be null.");
		this.hedgeInstrumentTradeValueProvider = Objects.requireNonNull(
				hedgeInstrumentTradeValueProvider,
				"hedgeInstrumentTradeValueProvider must not be null.");

		if(regularizationLambda < 0.0) {
			throw new IllegalArgumentException("regularizationLambda must be non-negative.");
		}
		this.regularizationLambda = regularizationLambda;
		this.reductionMethod = Objects.requireNonNull(reductionMethod, "reductionMethod must not be null.");
	}

	/**
	 * Convenience constructor accepting an array of hedge instruments.
	 *
	 * @param productToReplicate The product to replicate.
	 * @param hedgeInstruments The hedge instruments P_j.
	 * @param rebalancingTimes The times at which the hedge is rebalanced.
	 * @param basisFunctionProvider The basis functions X_q used for hedge ratios.
	 * @param regularizationLambda Tikhonov regularization parameter. Use 0.0 for none.
	 * @param reductionMethod The reduction method, e.g. PROJECTED_GALERKIN or L2.
	 */
	public ForwardSensitivityDeltaHedgedPortfolio(
			final TermStructureMonteCarloProduct productToReplicate,
			final TermStructureMonteCarloProduct[] hedgeInstruments,
			final double[] rebalancingTimes,
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

	@Override
	public RandomVariable getValue(
			final double evaluationTime,
			final TermStructureMonteCarloSimulationModel model) throws CalculationException {

		final long timingStart = System.currentTimeMillis();
		long timingValuationMillis = 0L;
		long timingHedgeRatioMillis = 0L;
		long timingTradeValueMillis = 0L;

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
			final RandomVariable derivativeProtoValue = productToReplicate.getValue(rebalancingTime, model);
			final RandomVariable[] hedgeInstrumentProtoValues = hedgeInstrumentValueProvider.getValues(
					rebalancingTime,
					model,
					hedgeInstruments);
			final RandomVariable[] solutionBasisFunctions = solutionBasisFunctionProvider.getBasisFunctions(rebalancingTime, model);
			final RandomVariable[] testBasisFunctions = testBasisFunctionProvider != null
					? testBasisFunctionProvider.getBasisFunctions(rebalancingTime, model)
					: null;
			final RandomVariable numeraireAtRebalancingTime = model.getNumeraire(rebalancingTime);
			timingValuationMillis += System.currentTimeMillis() - timingValuationStart;

			if(hedgeInstrumentProtoValues.length != hedgeInstrumentPositions.length) {
				throw new IllegalStateException(
						"Hedge-instrument value provider returned " + hedgeInstrumentProtoValues.length
						+ " values for " + hedgeInstrumentPositions.length + " hedge instruments.");
			}

			final long timingTradeValueStart = System.currentTimeMillis();
			final RandomVariable[] hedgeInstrumentTradeValues = hedgeInstrumentTradeValueProvider.getTradeValues(
					rebalancingTime,
					model,
					hedgeInstruments,
					hedgeInstrumentProtoValues,
					solutionBasisFunctions);
			timingTradeValueMillis += System.currentTimeMillis() - timingTradeValueStart;

			final long timingHedgeRatioStart = System.currentTimeMillis();
			final ProjectedHedgeRatioResult hedgeRatioResult = ForwardSensitivities.getHedgeRatios(
					parameterIDsByName,
					rebalancingTime,
					derivativeProtoValue,
					hedgeInstrumentProtoValues,
					solutionBasisFunctions,
					testBasisFunctions,
					regularizationLambda,
					reductionMethod);
			timingHedgeRatioMillis += System.currentTimeMillis() - timingHedgeRatioStart;

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
		 * Mark the final hedge portfolio to the same proto-value convention as the
		 * target product at evaluationTime. For a bond maturing before evaluationTime,
		 * this represents its cashflow carried forward through the model numeraire.
		 */
		final long timingFinalValuationStart = System.currentTimeMillis();
		RandomVariable portfolioValue = amountOfNumeraireAsset.mult(model.getNumeraire(evaluationTime));
		final RandomVariable[] hedgeInstrumentValuesAtEvaluationTime = getHedgeInstrumentProductValues(evaluationTime, model);
		for(int hedgeIndex = 0; hedgeIndex < hedgeInstrumentPositions.length; hedgeIndex++) {
			portfolioValue = portfolioValue.add(
					hedgeInstrumentPositions[hedgeIndex].mult(hedgeInstrumentValuesAtEvaluationTime[hedgeIndex]));
		}
		timingValuationMillis += System.currentTimeMillis() - timingFinalValuationStart;

		lastOperationTimingTotal = (System.currentTimeMillis() - timingStart) / 1000.0;
		lastOperationTimingValuation = timingValuationMillis / 1000.0;
		lastOperationTimingHedgeRatios = timingHedgeRatioMillis / 1000.0;
		lastOperationTimingTradeValues = timingTradeValueMillis / 1000.0;
		lastRebalancingTimes = Collections.unmodifiableList(new ArrayList<>(rebalancedTimes));
		lastParameterIDsByName = Collections.unmodifiableList(new ArrayList<>(parameterIDsByNameHistory));
		lastHedgeRatioResults = Collections.unmodifiableList(new ArrayList<>(hedgeRatioResults));
		lastHedgeInstrumentPositions = hedgeInstrumentPositions.clone();
		lastNumerairePosition = amountOfNumeraireAsset;

		return portfolioValue;
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
	 * Hedge-instrument value provider valuing {@link Bond}s by the model's
	 * forward-discount-bond implementation and all non-bond instruments by their
	 * product value.
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
					values[hedgeIndex] = getAnalyticBondValue(
							evaluationTime,
							((Bond)hedgeInstrument).getMaturity(),
							tenorPeriodLength,
							model);
				}
				else {
					values[hedgeIndex] = hedgeInstrument.getValue(evaluationTime, model);
				}
			}
			return values;
		};
	}

	/**
	 * Trade-value provider using analytic bond values for bonds and product values
	 * for non-bond instruments.
	 *
	 * @param tenorPeriodLength The tenor period length used to form analytic bond prices.
	 * @return A trade-value provider with analytic bond values.
	 */
	public static HedgeInstrumentTradeValueProvider getAnalyticBondTradeValueProvider(final double tenorPeriodLength) {
		final HedgeInstrumentValueProvider valueProvider = getAnalyticBondValueProvider(tenorPeriodLength);
		return (evaluationTime, model, hedgeInstruments, hedgeInstrumentProtoValues, conditioningBasisFunctions) ->
				valueProvider.getValues(evaluationTime, model, hedgeInstruments);
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
	 * @return The model-implied adapted bond value P(t,T).
	 * @throws CalculationException Thrown if the model cannot calculate the bond value.
	 */
	public static RandomVariable getAnalyticBondValue(
			final double evaluationTime,
			final double maturity,
			final double tenorPeriodLength,
			final TermStructureMonteCarloSimulationModel model) throws CalculationException {

		final double tolerance = 1E-12;
		if(maturity <= evaluationTime + tolerance) {
			if(maturity >= evaluationTime - tolerance) {
				return model.getRandomVariableForConstant(1.0);
			}

			/*
			 * The bond has already paid. Return the same carried-cash convention as
			 * Bond.getValue(t, model), used only as a safe fallback. Dynamic trading
			 * in already matured bonds should normally be avoided.
			 */
			return model.getRandomVariableForConstant(1.0)
					.div(model.getNumeraire(maturity))
					.mult(model.getMonteCarloWeights(maturity))
					.mult(model.getNumeraire(evaluationTime))
					.div(model.getMonteCarloWeights(evaluationTime));
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

			List<RandomVariable> modelPrimitivesList = new ArrayList<>();
			for(int processTimeIndexLagged = Math.max(processTimeIndex-lag, 0); processTimeIndexLagged<=processTimeIndex; processTimeIndexLagged++) {
				final RandomVariable[] modelPrimitivesLagged = model.getProcess().getProcessValue(processTimeIndexLagged);
				modelPrimitivesList.addAll(Arrays.asList(modelPrimitivesLagged));
			}
			final RandomVariable[] modelPrimitives = modelPrimitivesList.toArray(RandomVariable[]::new);

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

			List<RandomVariable> forwardRates = new ArrayList<>();
			int indexRateStart = tenorDiscretization.getTimeIndexNearestGreaterOrEqual(evaluationTime);
			int indexRateEnd = tenorDiscretization.getNumberOfTimes()-1;
			for(int indexPeriodStart = indexRateStart; indexPeriodStart<indexRateEnd; indexPeriodStart++) {
				RandomVariable forwardRate = model.getForwardRate(evaluationTime, tenorDiscretization.getTime(indexPeriodStart), tenorDiscretization.getTime(indexPeriodStart+1));
				forwardRates.add(forwardRate);
			}

			final RandomVariable[] modelPrimitives = forwardRates.toArray(RandomVariable[]::new);
			
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
	 * averages, which is the correct static trade price.
	 *
	 * @return A trade-value provider based on conditional-expectation regression.
	 */
	public static HedgeInstrumentTradeValueProvider getRegressionTradeValueProvider() {
		return (evaluationTime, model, hedgeInstruments, hedgeInstrumentProtoValues, conditioningBasisFunctions) -> {

			Objects.requireNonNull(hedgeInstrumentProtoValues, "hedgeInstrumentProtoValues must not be null.");
			if(conditioningBasisFunctions == null || conditioningBasisFunctions.length == 0) {
				throw new IllegalArgumentException("conditioningBasisFunctions must contain at least one basis function.");
			}

			final RandomVariable[] tradeValues = new RandomVariable[hedgeInstrumentProtoValues.length];

			if(evaluationTime == 0.0) {
				for(int hedgeIndex = 0; hedgeIndex < tradeValues.length; hedgeIndex++) {
					tradeValues[hedgeIndex] = model.getRandomVariableForConstant(hedgeInstrumentProtoValues[hedgeIndex].getAverage());
				}
				return tradeValues;
			}

			final ConditionalExpectationEstimator conditionalExpectationOperator =
					new MonteCarloConditionalExpectationRegression(conditioningBasisFunctions);

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

	public double[] getRebalancingTimes() {
		return rebalancingTimes.clone();
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
}
