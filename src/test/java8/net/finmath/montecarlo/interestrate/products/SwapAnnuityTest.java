/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 */
package net.finmath.montecarlo.interestrate.products;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.stochastic.Scalar;
import net.finmath.time.FloatingpointDate;

/**
 * Tests the schedule and reference-date contracts of {@link SwapAnnuity}.
 */
public class SwapAnnuityTest {

	@Test
	public void testReferenceDateOffsetIsUsedForPaymentFiltering() throws Exception {
		final LocalDateTime modelReferenceDate = LocalDateTime.of(2020, 1, 1, 0, 0);
		final LocalDateTime productReferenceDate = LocalDateTime.of(2021, 1, 1, 0, 0);
		final double offset = FloatingpointDate.getFloatingPointDateFromDate(
				modelReferenceDate,
				productReferenceDate);
		final List<Double> requestedNumeraireTimes = new ArrayList<>();
		final TermStructureMonteCarloSimulationModel model = createUnitModel(
				modelReferenceDate,
				requestedNumeraireTimes);

		final SwapAnnuity annuity = new SwapAnnuity(
				productReferenceDate,
				new double[] { 0.25, 0.75 },
				new double[] { 0.4, 0.7 });
		final double evaluationTime = offset + 0.5;

		final double value = annuity.getValue(evaluationTime, model).getAverage();

		Assert.assertEquals("Only the unpaid period contributes.", 0.7, value, 0.0);
		Assert.assertEquals(2, requestedNumeraireTimes.size());
		Assert.assertEquals(offset + 0.75, requestedNumeraireTimes.get(0), 1E-12);
		Assert.assertEquals(evaluationTime, requestedNumeraireTimes.get(1), 1E-12);
	}

	@Test
	public void testPaymentInsideBoundaryToleranceContributesPeriodLength() throws Exception {
		final List<Double> requestedNumeraireTimes = new ArrayList<Double>();
		final TermStructureMonteCarloSimulationModel model = createUnitModel(
				null,
				requestedNumeraireTimes);
		final SwapAnnuity annuity = new SwapAnnuity(
				new double[] { 1.0 },
				new double[] { 0.4 });
		final double evaluationTime = 1.0 + 0.5 * Bond.PAYMENT_TIME_TOLERANCE;

		final double value = annuity.getValue(evaluationTime, model).getAverage();

		Assert.assertEquals(0.4, value, 0.0);
		Assert.assertEquals(2, requestedNumeraireTimes.size());
		Assert.assertEquals(1.0, requestedNumeraireTimes.get(0), 0.0);
		Assert.assertEquals(evaluationTime, requestedNumeraireTimes.get(1), 0.0);
	}

	@Test
	public void testScheduleIsDefensivelyCopied() {
		final double[] maturities = new double[] { 1.0, 2.0 };
		final double[] periodLengths = new double[] { 0.5, 0.5 };
		final SwapAnnuity annuity = new SwapAnnuity(maturities, periodLengths);

		maturities[0] = 10.0;
		periodLengths[0] = 10.0;
		final double[] returnedMaturities = annuity.getMaturities();
		final double[] returnedPeriodLengths = annuity.getPeriodLengths();
		returnedMaturities[1] = 20.0;
		returnedPeriodLengths[1] = 20.0;

		Assert.assertArrayEquals(new double[] { 1.0, 2.0 }, annuity.getMaturities(), 0.0);
		Assert.assertArrayEquals(new double[] { 0.5, 0.5 }, annuity.getPeriodLengths(), 0.0);
	}

	@Test
	public void testScheduleLengthIsValidated() {
		try {
			new SwapAnnuity(new double[] { 1.0 }, new double[] { 0.5, 0.5 });
			Assert.fail("Expected mismatched schedule arrays to be rejected.");
		}
		catch(final IllegalArgumentException expected) {
			Assert.assertTrue(expected.getMessage().contains("same length"));
		}
	}

	@Test
	public void testDateBasedScheduleRequiresModelReferenceDate() throws Exception {
		final SwapAnnuity annuity = new SwapAnnuity(
				LocalDateTime.of(2021, 1, 1, 0, 0),
				new double[] { 1.0 },
				new double[] { 0.5 });
		final TermStructureMonteCarloSimulationModel model = createUnitModel(null, new ArrayList<Double>());

		try {
			annuity.getValue(0.0, model);
			Assert.fail("Expected a missing model reference date to be rejected.");
		}
		catch(final IllegalArgumentException expected) {
			Assert.assertTrue(expected.getMessage().contains("reference date"));
		}
	}

	private static TermStructureMonteCarloSimulationModel createUnitModel(
			final LocalDateTime referenceDate,
			final List<Double> requestedNumeraireTimes) {

		return (TermStructureMonteCarloSimulationModel)Proxy.newProxyInstance(
				SwapAnnuityTest.class.getClassLoader(),
				new Class<?>[] { TermStructureMonteCarloSimulationModel.class },
				(proxy, method, arguments) -> {
					switch(method.getName()) {
					case "getReferenceDate":
						return referenceDate;
					case "getNumeraire":
						requestedNumeraireTimes.add(((Number)arguments[0]).doubleValue());
						return Scalar.ONE;
					case "getMonteCarloWeights":
						return Scalar.ONE;
					case "getRandomVariableForConstant":
						return Scalar.of(((Number)arguments[0]).doubleValue());
					case "getNumberOfPaths":
						return 1;
					case "toString":
						return "UnitTermStructureModel";
					default:
						throw new UnsupportedOperationException("Unexpected model method: " + method);
					}
				});
	}
}
