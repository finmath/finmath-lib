/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 */
package net.finmath.montecarlo.interestrate.products;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiable;
import net.finmath.montecarlo.automaticdifferentiation.backward.RandomVariableDifferentiableAADFactory;
import net.finmath.montecarlo.automaticdifferentiation.forwardsensitivities.ForwardSensitivities.ReductionMethod;
import net.finmath.montecarlo.interestrate.TermStructureModel;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.montecarlo.process.MonteCarloProcess;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
import net.finmath.time.FloatingpointDate;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * Tests pathwise rebalancing and analytic hedge-value conventions.
 */
public class ForwardSensitivityDeltaHedgedPortfolioTest {

	@Test
	public void testRawPathwiseUsesDefaultTradeValueFallbackAndSkipsTerminalRebalance() throws Exception {
		final TimeDiscretization simulationTimes = new TimeDiscretizationFromArray(0.0, 0.5, 1.0);
		final RandomVariableDifferentiable state = new RandomVariableDifferentiableAADFactory()
				.createRandomVariable(0.0, new double[] { 1.0, 2.0 });
		final MonteCarloProcess process = createProcess(simulationTimes, state);
		final TermStructureMonteCarloSimulationModel model = createPathwiseModel(simulationTimes, process);

		final TermStructureMonteCarloProduct product = productReturning(state);
		final TermStructureMonteCarloProduct hedgeInstrument = productReturning(state.mult(2.0));
		final ForwardSensitivityDeltaHedgedPortfolio portfolio =
				new ForwardSensitivityDeltaHedgedPortfolio(
						product,
						Collections.singletonList(hedgeInstrument),
						new TimeDiscretizationFromArray(0.5, 1.0),
						null,
						0.0,
						ReductionMethod.PATHWISE);

		final RandomVariable value = portfolio.getValue(1.0, model);

		Assert.assertArrayEquals(new double[] { 1.0, 2.0 }, value.getRealizations(), 1E-12);
		Assert.assertEquals(Collections.singletonList(0.5), portfolio.getLastRebalancingTimes());
		Assert.assertArrayEquals(
				new double[] { 0.5, 0.5 },
				portfolio.getLastHedgeInstrumentPositions()[0].getRealizations(),
				1E-12);
	}

	@Test
	public void testTradeRegressionUsesFreshProductValuesWithNonUnitNumeraireAndWeights() throws Exception {
		final TimeDiscretization simulationTimes = new TimeDiscretizationFromArray(0.0, 0.5, 1.0);
		final RandomVariableDifferentiable state = new RandomVariableDifferentiableAADFactory()
				.createRandomVariable(0.0, new double[] { 1.0, 2.0 });
		final MonteCarloProcess process = createProcess(simulationTimes, state);
		final TermStructureMonteCarloSimulationModel model = createScaledPathwiseModel(simulationTimes, process);

		final ForwardSensitivityDeltaHedgedPortfolio portfolio =
				new ForwardSensitivityDeltaHedgedPortfolio(
						relativeValueProduct(state),
						Collections.singletonList(relativeValueProduct(state.mult(2.0))),
						new TimeDiscretizationFromArray(0.5),
						null,
						0.0,
						ReductionMethod.PATHWISE);

		portfolio.getValue(1.0, model);

		Assert.assertEquals(0.5, portfolio.getLastHedgeInstrumentPositions()[0].getAverage(), 1E-12);
		Assert.assertEquals(-3.0, portfolio.getLastNumerairePosition().getAverage(), 1E-12);
	}

	@Test
	public void testConstructorRejectsInvalidRegularization() {
		final TermStructureMonteCarloProduct product = productReturning(Scalar.ONE);
		final List<TermStructureMonteCarloProduct> hedgeInstruments = Collections.singletonList(product);
		final TimeDiscretization rebalancingTimes = new TimeDiscretizationFromArray(0.5);

		try {
			new ForwardSensitivityDeltaHedgedPortfolio(
					product,
					hedgeInstruments,
					rebalancingTimes,
					null,
					0.1,
					ReductionMethod.PATHWISE);
			Assert.fail("PATHWISE regularization should have been rejected.");
		}
		catch(final IllegalArgumentException exception) {
			Assert.assertTrue(exception.getMessage().contains("PATHWISE"));
		}

		try {
			new ForwardSensitivityDeltaHedgedPortfolio(
					product,
					hedgeInstruments,
					rebalancingTimes,
					null,
					Double.NaN,
					ReductionMethod.L2);
			Assert.fail("Non-finite regularization should have been rejected.");
		}
		catch(final IllegalArgumentException exception) {
			Assert.assertTrue(exception.getMessage().contains("finite"));
		}
	}

	@Test
	public void testAnalyticSwapAnnuityValueUsesOffsetAndExcludesPaidCashflows() throws Exception {
		final LocalDateTime modelReferenceDate = LocalDateTime.of(2020, 1, 1, 0, 0);
		final LocalDateTime productReferenceDate = LocalDateTime.of(2021, 1, 1, 0, 0);
		final double offset = FloatingpointDate.getFloatingPointDateFromDate(
				modelReferenceDate,
				productReferenceDate);
		final double evaluationTime = offset + 0.5;
		final List<Double> requestedBondMaturities = new ArrayList<>();
		final TermStructureMonteCarloSimulationModel model = createAnalyticBondModel(
				modelReferenceDate,
				requestedBondMaturities);
		final SwapAnnuity annuity = new SwapAnnuity(
				productReferenceDate,
				new double[] { 0.25, 0.75 },
				new double[] { 0.4, 0.7 });

		final RandomVariable[] values = ForwardSensitivityDeltaHedgedPortfolio
				.getAnalyticBondValueProvider(0.5)
				.getValues(evaluationTime, model, Collections.singletonList(annuity));

		Assert.assertEquals(1, requestedBondMaturities.size());
		Assert.assertEquals(offset + 0.75, requestedBondMaturities.get(0), 1E-12);
		Assert.assertEquals(0.7 * (offset + 0.75), values[0].getAverage(), 1E-12);
	}

	@Test
	public void testAnalyticBondValueIsExDividendAfterMaturity() throws Exception {
		final List<Double> requestedBondMaturities = new ArrayList<>();
		final TermStructureMonteCarloSimulationModel model = createAnalyticBondModel(
				LocalDateTime.of(2020, 1, 1, 0, 0),
				requestedBondMaturities);

		final RandomVariable valueAfterMaturity = ForwardSensitivityDeltaHedgedPortfolio
				.getAnalyticBondValue(1.0, 0.5, 0.5, model);
		final RandomVariable valueAtMaturity = ForwardSensitivityDeltaHedgedPortfolio
				.getAnalyticBondValue(1.0, 1.0, 0.5, model);

		Assert.assertEquals(0.0, valueAfterMaturity.getAverage(), 0.0);
		Assert.assertEquals(1.0, valueAtMaturity.getAverage(), 0.0);
		Assert.assertTrue("No forward bond is needed at or after maturity.", requestedBondMaturities.isEmpty());
	}

	@Test
	public void testDateBasedBondUsesModelTimeForProductAndAnalyticValue() throws Exception {
		final LocalDateTime modelReferenceDate = LocalDateTime.of(2020, 1, 1, 0, 0);
		final LocalDateTime productReferenceDate = LocalDateTime.of(2021, 1, 1, 0, 0);
		final double offset = FloatingpointDate.getFloatingPointDateFromDate(
				modelReferenceDate,
				productReferenceDate);
		final List<Double> requestedBondMaturities = new ArrayList<>();
		final TermStructureMonteCarloSimulationModel model = createAnalyticBondModel(
				modelReferenceDate,
				requestedBondMaturities);
		final Bond bond = new Bond(productReferenceDate, 0.75);

		final RandomVariable productValue = bond.getValue(offset + 0.5, model);
		final RandomVariable analyticValue = ForwardSensitivityDeltaHedgedPortfolio
				.getAnalyticBondValueProvider(0.5)
				.getValues(offset + 0.5, model, Collections.singletonList(bond))[0];

		Assert.assertEquals(productReferenceDate, bond.getReferenceDate());
		Assert.assertEquals(1.0, productValue.getAverage(), 0.0);
		Assert.assertEquals(Collections.singletonList(offset + 0.75), requestedBondMaturities);
		Assert.assertEquals(offset + 0.75, analyticValue.getAverage(), 1E-12);
	}

	@Test
	public void testBondAndAnalyticBoundaryToleranceAgree() throws Exception {
		final List<Double> requestedBondMaturities = new ArrayList<>();
		final TermStructureMonteCarloSimulationModel model = createAnalyticBondModel(
				LocalDateTime.of(2020, 1, 1, 0, 0),
				requestedBondMaturities);
		final double evaluationTime = 1.0 + 0.5 * Bond.PAYMENT_TIME_TOLERANCE;

		final RandomVariable productValue = new Bond(1.0).getValue(evaluationTime, model);
		final RandomVariable analyticValue = ForwardSensitivityDeltaHedgedPortfolio
				.getAnalyticBondValue(evaluationTime, 1.0, 0.5, model);

		Assert.assertEquals(1.0, productValue.getAverage(), 0.0);
		Assert.assertEquals(1.0, analyticValue.getAverage(), 0.0);
		Assert.assertTrue(requestedBondMaturities.isEmpty());
	}

	@Test
	public void testAnalyticRollOverUsesProductPiecewiseSemantics() throws Exception {
		final TermStructureMonteCarloSimulationModel model = createRollOverModel();
		final TermStructureMonteCarloProduct rollOver = new DiscreteTenorRollOver(1.0, 2.0, 1.0);
		final ForwardSensitivityDeltaHedgedPortfolio.HedgeInstrumentValueProvider provider =
				ForwardSensitivityDeltaHedgedPortfolio.getAnalyticBondValueProvider(0.5);

		Assert.assertEquals(
				6.0,
				provider.getValues(0.5, model, Collections.singletonList(rollOver))[0].getAverage(),
				1E-12);
		Assert.assertEquals(
				18.7,
				provider.getValues(1.5, model, Collections.singletonList(rollOver))[0].getAverage(),
				1E-12);
		Assert.assertEquals(
				1.1,
				provider.getValues(2.0, model, Collections.singletonList(rollOver))[0].getAverage(),
				1E-12);
		Assert.assertEquals(
				1.44375,
				provider.getValues(2.5, model, Collections.singletonList(rollOver))[0].getAverage(),
				1E-12);
	}

	@Test
	public void testAnalyticBondSensitivityValuePreservesAadGradient() throws Exception {
		final RandomVariableDifferentiable state = new RandomVariableDifferentiableAADFactory()
				.createRandomVariable(0.0, new double[] { 1.0, 2.0 });
		final TermStructureMonteCarloSimulationModel model = createDifferentiableAnalyticBondModel(state);
		final List<TermStructureMonteCarloProduct> bonds = Collections.singletonList(new Bond(2.0));

		final RandomVariable value = ForwardSensitivityDeltaHedgedPortfolio
				.getAnalyticBondValueProvider(0.5)
				.getValues(0.5, model, bonds)[0];
		final RandomVariable tradeValue = ForwardSensitivityDeltaHedgedPortfolio
				.getAnalyticBondTradeValueProvider(0.5)
				.getTradeValues(0.5, model, bonds, new RandomVariable[] { value }, null)[0];

		Assert.assertTrue(value instanceof RandomVariableDifferentiable);
		Assert.assertTrue(((RandomVariableDifferentiable)value).getGradient().containsKey(state.getID()));
		Assert.assertFalse(tradeValue instanceof RandomVariableDifferentiable);
	}

	@Test
	public void testPortfolioRejectsIntermediateKnownCashflowButAllowsEvaluationTimePayment() throws Exception {
		final TimeDiscretization simulationTimes = new TimeDiscretizationFromArray(0.0, 1.0);
		final RandomVariableDifferentiable state = new RandomVariableDifferentiableAADFactory()
				.createRandomVariable(0.0, new double[] { 1.0, 2.0 });
		final TermStructureMonteCarloSimulationModel model = createPathwiseModel(
				simulationTimes,
				createProcess(simulationTimes, state));

		assertIntermediateCashflowRejected(state, model, new Bond(0.5));
		assertIntermediateCashflowRejected(
				state,
				model,
				new SwapAnnuity(new double[] { 0.5 }, new double[] { 1.0 }));
		assertIntermediateCashflowRejected(state, model, new DiscreteTenorRollOver(0.0, 0.5, 0.5));

		final ForwardSensitivityDeltaHedgedPortfolio boundaryPortfolio =
				new ForwardSensitivityDeltaHedgedPortfolio(
						productReturning(state),
						Collections.singletonList(new Bond(1.0)),
						new TimeDiscretizationFromArray(1.0),
						null,
						0.0,
						ReductionMethod.PATHWISE);
		Assert.assertTrue(Double.isFinite(boundaryPortfolio.getValue(1.0, model).getAverage()));
	}

	private static void assertIntermediateCashflowRejected(
			final RandomVariable state,
			final TermStructureMonteCarloSimulationModel model,
			final TermStructureMonteCarloProduct hedgeInstrument) throws CalculationException {
		final ForwardSensitivityDeltaHedgedPortfolio portfolio =
				new ForwardSensitivityDeltaHedgedPortfolio(
						productReturning(state),
						Collections.singletonList(hedgeInstrument),
						new TimeDiscretizationFromArray(1.0),
						null,
						0.0,
						ReductionMethod.PATHWISE);
		try {
			portfolio.getValue(1.0, model);
			Assert.fail("An intermediate hedge cashflow should have been rejected.");
		}
		catch(final IllegalArgumentException exception) {
			Assert.assertTrue(exception.getMessage().contains("does not book intermediate hedge cashflows"));
		}
	}

	private static TermStructureMonteCarloProduct productReturning(final RandomVariable value) {
		return new AbstractTermStructureMonteCarloProduct() {
			@Override
			public RandomVariable getValue(
					final double evaluationTime,
					final TermStructureMonteCarloSimulationModel model) throws CalculationException {
				return value;
			}
		};
	}

	private static TermStructureMonteCarloProduct relativeValueProduct(final RandomVariable relativeValue) {
		return new AbstractTermStructureMonteCarloProduct() {
			@Override
			public RandomVariable getValue(
					final double evaluationTime,
					final TermStructureMonteCarloSimulationModel model) throws CalculationException {
				return relativeValue.mult(model.getNumeraire(evaluationTime))
						.div(model.getMonteCarloWeights(evaluationTime));
			}
		};
	}

	private static MonteCarloProcess createProcess(
			final TimeDiscretization simulationTimes,
			final RandomVariable state) {

		return (MonteCarloProcess)Proxy.newProxyInstance(
				ForwardSensitivityDeltaHedgedPortfolioTest.class.getClassLoader(),
				new Class<?>[] { MonteCarloProcess.class },
				(proxy, method, arguments) -> {
					switch(method.getName()) {
					case "getProcessValue":
						return arguments.length == 1 ? new RandomVariable[] { state } : state;
					case "getTimeDiscretization":
						return simulationTimes;
					case "getTimeIndex":
						return simulationTimes.getTimeIndex(((Number)arguments[0]).doubleValue());
					case "getTime":
						return simulationTimes.getTime(((Number)arguments[0]).intValue());
					case "getNumberOfComponents":
						return 1;
					case "getNumberOfPaths":
						return state.size();
					case "getNumberOfFactors":
						return 0;
					case "getMonteCarloWeights":
						return Scalar.ONE;
					case "clone":
						return proxy;
					case "toString":
						return "TestProcess";
					default:
						throw new UnsupportedOperationException("Unexpected process method: " + method);
					}
				});
	}

	private static TermStructureMonteCarloSimulationModel createPathwiseModel(
			final TimeDiscretization simulationTimes,
			final MonteCarloProcess process) {

		return (TermStructureMonteCarloSimulationModel)Proxy.newProxyInstance(
				ForwardSensitivityDeltaHedgedPortfolioTest.class.getClassLoader(),
				new Class<?>[] { TermStructureMonteCarloSimulationModel.class },
				(proxy, method, arguments) -> {
					switch(method.getName()) {
					case "getProcess":
						return process;
					case "getTimeDiscretization":
						return simulationTimes;
					case "getTimeIndex":
						return simulationTimes.getTimeIndex(((Number)arguments[0]).doubleValue());
					case "getTime":
						return simulationTimes.getTime(((Number)arguments[0]).intValue());
					case "getNumeraire":
					case "getMonteCarloWeights":
						return Scalar.ONE;
					case "getRandomVariableForConstant":
						return Scalar.of(((Number)arguments[0]).doubleValue());
					case "getNumberOfPaths":
						return 2;
					case "toString":
						return "PathwiseTestModel";
					default:
						throw new UnsupportedOperationException("Unexpected model method: " + method);
					}
				});
	}

	private static TermStructureMonteCarloSimulationModel createScaledPathwiseModel(
			final TimeDiscretization simulationTimes,
			final MonteCarloProcess process) {

		return (TermStructureMonteCarloSimulationModel)Proxy.newProxyInstance(
				ForwardSensitivityDeltaHedgedPortfolioTest.class.getClassLoader(),
				new Class<?>[] { TermStructureMonteCarloSimulationModel.class },
				(proxy, method, arguments) -> {
					switch(method.getName()) {
					case "getProcess":
						return process;
					case "getTimeDiscretization":
						return simulationTimes;
					case "getTimeIndex":
						return simulationTimes.getTimeIndex(((Number)arguments[0]).doubleValue());
					case "getTime":
						return simulationTimes.getTime(((Number)arguments[0]).intValue());
					case "getNumeraire":
						return Scalar.of(valueAtTime(((Number)arguments[0]).doubleValue(), 2.0, 6.0, 1.0));
					case "getMonteCarloWeights":
						return Scalar.of(valueAtTime(((Number)arguments[0]).doubleValue(), 0.5, 0.25, 1.0));
					case "getRandomVariableForConstant":
						return Scalar.of(((Number)arguments[0]).doubleValue());
					case "getNumberOfPaths":
						return 2;
					case "toString":
						return "ScaledPathwiseTestModel";
					default:
						throw new UnsupportedOperationException("Unexpected model method: " + method);
					}
				});
	}

	private static double valueAtTime(
			final double time,
			final double valueAtZero,
			final double valueAtHalf,
			final double valueAtOne) {
		if(Math.abs(time) < 1E-12) {
			return valueAtZero;
		}
		if(Math.abs(time - 0.5) < 1E-12) {
			return valueAtHalf;
		}
		return valueAtOne;
	}

	private static TermStructureMonteCarloSimulationModel createAnalyticBondModel(
			final LocalDateTime referenceDate,
			final List<Double> requestedBondMaturities) {

		final TermStructureModel termStructureModel = (TermStructureModel)Proxy.newProxyInstance(
				ForwardSensitivityDeltaHedgedPortfolioTest.class.getClassLoader(),
				new Class<?>[] { TermStructureModel.class },
				(proxy, method, arguments) -> {
					if("getForwardDiscountBond".equals(method.getName())) {
						final double maturity = ((Number)arguments[2]).doubleValue();
						requestedBondMaturities.add(maturity);
						return Scalar.of(maturity);
					}
					if("toString".equals(method.getName())) {
						return "AnalyticBondTestModel";
					}
					throw new UnsupportedOperationException("Unexpected term-structure model method: " + method);
				});

		return (TermStructureMonteCarloSimulationModel)Proxy.newProxyInstance(
				ForwardSensitivityDeltaHedgedPortfolioTest.class.getClassLoader(),
				new Class<?>[] { TermStructureMonteCarloSimulationModel.class },
				(proxy, method, arguments) -> {
					switch(method.getName()) {
					case "getReferenceDate":
						return referenceDate;
					case "getModel":
						return termStructureModel;
					case "getProcess":
						return null;
					case "getNumeraire":
					case "getMonteCarloWeights":
						return Scalar.ONE;
					case "getRandomVariableForConstant":
						return Scalar.of(((Number)arguments[0]).doubleValue());
					case "toString":
						return "AnalyticTestSimulation";
					default:
						throw new UnsupportedOperationException("Unexpected simulation method: " + method);
					}
				});
	}

	private static TermStructureMonteCarloSimulationModel createRollOverModel() {
		final TermStructureModel termStructureModel = (TermStructureModel)Proxy.newProxyInstance(
				ForwardSensitivityDeltaHedgedPortfolioTest.class.getClassLoader(),
				new Class<?>[] { TermStructureModel.class },
				(proxy, method, arguments) -> {
					if("getForwardDiscountBond".equals(method.getName())) {
						final double evaluationTime = ((Number)arguments[1]).doubleValue();
						final double maturity = ((Number)arguments[2]).doubleValue();
						return Scalar.of(10.0 * evaluationTime + maturity);
					}
					if("toString".equals(method.getName())) {
						return "RollOverTermStructureModel";
					}
					throw new UnsupportedOperationException("Unexpected term-structure model method: " + method);
				});

		return (TermStructureMonteCarloSimulationModel)Proxy.newProxyInstance(
				ForwardSensitivityDeltaHedgedPortfolioTest.class.getClassLoader(),
				new Class<?>[] { TermStructureMonteCarloSimulationModel.class },
				(proxy, method, arguments) -> {
					switch(method.getName()) {
					case "getModel":
						return termStructureModel;
					case "getProcess":
						return null;
					case "getForwardRate":
						return Scalar.of(0.1);
					case "getNumeraire":
						return Scalar.of(2.0 + ((Number)arguments[0]).doubleValue());
					case "getMonteCarloWeights":
						return Scalar.of(1.0 / (1.0 + ((Number)arguments[0]).doubleValue()));
					case "getRandomVariableForConstant":
						return Scalar.of(((Number)arguments[0]).doubleValue());
					case "toString":
						return "RollOverSimulationModel";
					default:
						throw new UnsupportedOperationException("Unexpected simulation method: " + method);
					}
				});
	}

	private static TermStructureMonteCarloSimulationModel createDifferentiableAnalyticBondModel(
			final RandomVariableDifferentiable state) {
		final TermStructureModel termStructureModel = (TermStructureModel)Proxy.newProxyInstance(
				ForwardSensitivityDeltaHedgedPortfolioTest.class.getClassLoader(),
				new Class<?>[] { TermStructureModel.class },
				(proxy, method, arguments) -> {
					if("getForwardDiscountBond".equals(method.getName())) {
						return state.mult(2.0);
					}
					if("toString".equals(method.getName())) {
						return "DifferentiableAnalyticBondModel";
					}
					throw new UnsupportedOperationException("Unexpected term-structure model method: " + method);
				});

		return (TermStructureMonteCarloSimulationModel)Proxy.newProxyInstance(
				ForwardSensitivityDeltaHedgedPortfolioTest.class.getClassLoader(),
				new Class<?>[] { TermStructureMonteCarloSimulationModel.class },
				(proxy, method, arguments) -> {
					switch(method.getName()) {
					case "getModel":
						return termStructureModel;
					case "getProcess":
						return null;
					case "getRandomVariableForConstant":
						return Scalar.of(((Number)arguments[0]).doubleValue());
					case "toString":
						return "DifferentiableAnalyticBondSimulation";
					default:
						throw new UnsupportedOperationException("Unexpected simulation method: " + method);
					}
				});
	}
}
