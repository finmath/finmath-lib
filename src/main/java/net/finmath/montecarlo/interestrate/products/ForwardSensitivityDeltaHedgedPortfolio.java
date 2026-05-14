/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 14.05.2026
 */
package net.finmath.montecarlo.interestrate.products;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.automaticdifferentiation.IndependentModelParameterProvider;
import net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiable;
import net.finmath.montecarlo.automaticdifferentiation.forwardsensitivities.ForwardSensitivities;
import net.finmath.montecarlo.automaticdifferentiation.forwardsensitivities.ForwardSensitivities.ProjectedHedgeRatioResult;
import net.finmath.montecarlo.automaticdifferentiation.forwardsensitivities.ForwardSensitivities.ReductionMethod;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;

/**
 * A self-financing hedge simulator for term-structure products using stochastic
 * hedge ratios obtained from {@link ForwardSensitivities}.
 *
 * <p>
 * The class is the term-structure analogue of a delta-hedged portfolio. At each
 * rebalancing time t it values the product to replicate and the hedge
 * instruments, calls {@link ForwardSensitivities#getHedgeRatios(Map, double, RandomVariable, RandomVariable[], RandomVariable[], RandomVariable[], double, ReductionMethod)},
 * and then changes the positions in the hedge instruments. The required cash is
 * taken from, or added to, the numeraire account. Thus the strategy is
 * self-financing after the initial funding by the time-0 value of the product.
 * </p>
 *
 * <p>
 * The hedge ratios are represented in the solution basis supplied by the
 * {@link BasisFunctionProvider}. For {@link ReductionMethod#PROJECTED_GALERKIN}
 * an optional separate test basis may be supplied. If no test basis is supplied,
 * the same basis is used for solution and test functions. For
 * {@link ReductionMethod#L2}, the test basis is ignored by
 * {@link ForwardSensitivities}.
 * </p>
 *
 * <p>
 * Important: this class treats each hedge instrument as a mark-to-market product
 * whose {@code getValue(t, model)} is the tradable value at time t. If a hedge
 * instrument has intermediate cashflows, either rebalance before those cashflows
 * or use instruments whose product implementation accounts for the cashflow
 * convention required by the experiment.
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

	private final TermStructureMonteCarloProduct productToReplicate;
	private final List<TermStructureMonteCarloProduct> hedgeInstruments;
	private final double[] rebalancingTimes;
	private final BasisFunctionProvider solutionBasisFunctionProvider;
	private final BasisFunctionProvider testBasisFunctionProvider;
	private final double regularizationLambda;
	private final ReductionMethod reductionMethod;

	private double lastOperationTimingTotal = Double.NaN;
	private double lastOperationTimingValuation = Double.NaN;
	private double lastOperationTimingHedgeRatios = Double.NaN;

	private List<Double> lastRebalancingTimes = Collections.emptyList();
	private List<ProjectedHedgeRatioResult> lastHedgeRatioResults = Collections.emptyList();
	private RandomVariable[] lastHedgeInstrumentPositions = new RandomVariable[0];
	private RandomVariable lastNumerairePosition;

	/**
	 * Creates a self-financing hedge using the same basis for solution and test
	 * functions.
	 *
	 * @param productToReplicate The product to replicate.
	 * @param hedgeInstruments The tradable hedge instruments P_j.
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
				regularizationLambda,
				reductionMethod);
	}

	/**
	 * Creates a self-financing hedge using possibly different solution and test bases.
	 *
	 * @param productToReplicate The product to replicate.
	 * @param hedgeInstruments The tradable hedge instruments P_j.
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
	 * @param hedgeInstruments The tradable hedge instruments P_j.
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

		if(!(model instanceof IndependentModelParameterProvider)) {
			throw new IllegalArgumentException(
					"The model must implement IndependentModelParameterProvider, "
					+ "so that ForwardSensitivities can identify differentiable model parameters.");
		}

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
		final List<ProjectedHedgeRatioResult> hedgeRatioResults = new ArrayList<>();

		for(final double rebalancingTime : rebalancingTimes) {

			if(rebalancingTime < 0.0 || rebalancingTime >= evaluationTime) {
				continue;
			}

			final long timingValuationStart = System.currentTimeMillis();
			final RandomVariable derivativeValue = productToReplicate.getValue(rebalancingTime, model);
			final RandomVariable[] hedgeInstrumentValues = getHedgeInstrumentValues(rebalancingTime, model);
			final RandomVariable[] solutionBasisFunctions = solutionBasisFunctionProvider.getBasisFunctions(rebalancingTime, model);
			final RandomVariable[] testBasisFunctions = testBasisFunctionProvider != null
					? testBasisFunctionProvider.getBasisFunctions(rebalancingTime, model)
					: null;
			final RandomVariable numeraireAtRebalancingTime = model.getNumeraire(rebalancingTime);
			timingValuationMillis += System.currentTimeMillis() - timingValuationStart;

			/*
			 *  Build model parameters
			 */
			final Map<String, Long> parameterIDsByName = new HashMap<>();
			RandomVariable[] modelPrimitives = model.getProcess().getProcessValue(model.getTimeIndex(rebalancingTime));
			IntStream.range(0, modelPrimitives.length).forEach(i -> parameterIDsByName.put("("+rebalancingTime+","+i+")", ((RandomVariableDifferentiable)modelPrimitives[i]).getID()));

			final long timingHedgeRatioStart = System.currentTimeMillis();
			final ProjectedHedgeRatioResult hedgeRatioResult = ForwardSensitivities.getHedgeRatios(
					parameterIDsByName,
					rebalancingTime,
					derivativeValue,
					hedgeInstrumentValues,
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

			/*
			 * Self-financing rebalancing:
			 * buy/sell hedge instruments and finance the trade via the numeraire account.
			 */
			RandomVariable valueOfHedgeInstrumentsToBuy = model.getRandomVariableForConstant(0.0);
			for(int hedgeIndex = 0; hedgeIndex < hedgeInstrumentPositions.length; hedgeIndex++) {
				final RandomVariable hedgeInstrumentPositionChange = newHedgeInstrumentPositions[hedgeIndex]
						.sub(hedgeInstrumentPositions[hedgeIndex]);
				valueOfHedgeInstrumentsToBuy = valueOfHedgeInstrumentsToBuy
						.add(hedgeInstrumentPositionChange.mult(hedgeInstrumentValues[hedgeIndex]));
			}

			amountOfNumeraireAsset = amountOfNumeraireAsset
					.sub(valueOfHedgeInstrumentsToBuy.div(numeraireAtRebalancingTime));
			hedgeInstrumentPositions = newHedgeInstrumentPositions;

			rebalancedTimes.add(rebalancingTime);
			hedgeRatioResults.add(hedgeRatioResult);
		}

		/*
		 * Mark the final hedge portfolio to market at evaluationTime.
		 */
		final long timingFinalValuationStart = System.currentTimeMillis();
		RandomVariable portfolioValue = amountOfNumeraireAsset.mult(model.getNumeraire(evaluationTime));
		final RandomVariable[] hedgeInstrumentValuesAtEvaluationTime = getHedgeInstrumentValues(evaluationTime, model);
		for(int hedgeIndex = 0; hedgeIndex < hedgeInstrumentPositions.length; hedgeIndex++) {
			portfolioValue = portfolioValue.add(
					hedgeInstrumentPositions[hedgeIndex].mult(hedgeInstrumentValuesAtEvaluationTime[hedgeIndex]));
		}
		timingValuationMillis += System.currentTimeMillis() - timingFinalValuationStart;

		lastOperationTimingTotal = (System.currentTimeMillis() - timingStart) / 1000.0;
		lastOperationTimingValuation = timingValuationMillis / 1000.0;
		lastOperationTimingHedgeRatios = timingHedgeRatioMillis / 1000.0;
		lastRebalancingTimes = Collections.unmodifiableList(new ArrayList<>(rebalancedTimes));
		lastHedgeRatioResults = Collections.unmodifiableList(new ArrayList<>(hedgeRatioResults));
		lastHedgeInstrumentPositions = hedgeInstrumentPositions.clone();
		lastNumerairePosition = amountOfNumeraireAsset;

		return portfolioValue;
	}

	private RandomVariable[] getHedgeInstrumentValues(
			final double evaluationTime,
			final TermStructureMonteCarloSimulationModel model) throws CalculationException {

		final RandomVariable[] values = new RandomVariable[hedgeInstruments.size()];
		for(int hedgeIndex = 0; hedgeIndex < hedgeInstruments.size(); hedgeIndex++) {
			values[hedgeIndex] = hedgeInstruments.get(hedgeIndex).getValue(evaluationTime, model);
		}
		return values;
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

	public List<Double> getLastRebalancingTimes() {
		return lastRebalancingTimes;
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
