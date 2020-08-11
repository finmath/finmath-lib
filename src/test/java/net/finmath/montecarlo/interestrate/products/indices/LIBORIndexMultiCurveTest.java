/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 07.12.2014
 */

package net.finmath.montecarlo.interestrate.products.indices;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.calibration.CalibratedCurves;
import net.finmath.marketdata.calibration.CalibratedCurves.CalibrationSpec;
import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.AnalyticModelFromCurvesAndVols;
import net.finmath.marketdata.model.curves.Curve;
import net.finmath.marketdata.model.curves.CurveInterpolation;
import net.finmath.marketdata.model.curves.CurveInterpolation.ExtrapolationMethod;
import net.finmath.marketdata.model.curves.CurveInterpolation.InterpolationEntity;
import net.finmath.marketdata.model.curves.CurveInterpolation.InterpolationMethod;
import net.finmath.marketdata.model.curves.DiscountCurveFromForwardCurve;
import net.finmath.marketdata.model.curves.DiscountCurveInterpolation;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurveFromDiscountCurve;
import net.finmath.marketdata.model.curves.ForwardCurveInterpolation;
import net.finmath.montecarlo.interestrate.CalibrationProduct;
import net.finmath.montecarlo.interestrate.LIBORMarketModel;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.LIBORMonteCarloSimulationFromLIBORModel;
import net.finmath.montecarlo.interestrate.models.LIBORMarketModelFromCovarianceModel;
import net.finmath.montecarlo.interestrate.models.LIBORMarketModelFromCovarianceModel.Measure;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCorrelationModelExponentialDecay;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCovarianceModelFromVolatilityAndCorrelation;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORVolatilityModelFromGivenMatrix;
import net.finmath.montecarlo.interestrate.products.components.NotionalFromConstant;
import net.finmath.montecarlo.interestrate.products.components.Period;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.optimizer.SolverException;
import net.finmath.time.Schedule;
import net.finmath.time.ScheduleGenerator;
import net.finmath.time.TimeDiscretizationFromArray;
import net.finmath.time.businessdaycalendar.BusinessdayCalendar.DateRollConvention;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingTARGETHolidays;

/**
 * Implements a test valuing a FRA on a LIBOR Market Model referencing different forward rates curves
 * and comparing the Monte-Carlo valuation with the analytic valuation.
 *
 * The model created features three curves: OIS, 3M, 6M.
 *
 * @author Christian Fries
 */
@RunWith(Parameterized.class)
public class LIBORIndexMultiCurveTest {

	private static final String DISCOUNT_CURVE_NAME = "discount-EUR-OIS";
	private static final String FORWARD_CURVE_NAME = "forward-EUR-3M";
	private static final String FORWARD_CURVE_NAME2 = "forward-EUR-6M";

	/**
	 * The parameters for this test, that is an error consisting of
	 * { numberOfPaths, setup }.
	 *
	 * @return Array of parameters.
	 */
	@Parameters(name="{1}-{0}")
	public static Collection<Object[]> generateData()
	{
		/*
		 * Test parameters are:
		 * 	numberOfPaths, nameOfForwardCurve
		 */
		return Arrays.asList(new Object[][] {
			{ 40000, FORWARD_CURVE_NAME },
			{ 100000, FORWARD_CURVE_NAME },
			{ 200000, FORWARD_CURVE_NAME },
			{ 40000, FORWARD_CURVE_NAME2 },
			{ 100000, FORWARD_CURVE_NAME2 },
			{ 200000, FORWARD_CURVE_NAME2 },

		});
	}

	private final int numberOfFactors = 5;
	private final double correlationDecayParam = 0.05;

	private final double[] periodStarts	= { 2.00, 2.00, 2.00, 2.50, 2.50, 2.50, 2.00, 2.00, 2.25 , 4.00 };
	private final double[] periodEnds		= { 2.50, 2.25, 3.00, 3.00, 3.25, 3.50, 4.00, 5.00, 2.50 , 5.00 };
	private final double[] tolerance		= { 3E-5, 3E-5, 3E-5, 3E-5, 3E-5, 3E-5, 5E-5, 6E-5, 3E-5 , 3E-5 };		// Tolerance at 100.000 path

	private final String indexCurve;
	private final LIBORModelMonteCarloSimulationModel liborMarketModel;

	public LIBORIndexMultiCurveTest(final Integer numberOfPaths, final String forwardCurveName) throws CalculationException, SolverException, CloneNotSupportedException {
		indexCurve = forwardCurveName;

		// Create a LIBOR market model
		liborMarketModel = createLIBORMarketModel(FORWARD_CURVE_NAME, Measure.SPOT, numberOfPaths, numberOfFactors, correlationDecayParam);
	}

	@Test
	public void testFRAMonteCarloVersusAnalytic() throws CalculationException {

		final NumberFormat formatDec2 = new DecimalFormat("0.00");
		final NumberFormat formatDec6 = new DecimalFormat("0.000000");

		for(int iTestCase = 0; iTestCase<periodStarts.length; iTestCase++) {
			final double periodStart	= periodStarts[iTestCase];
			final double periodEnd	= periodEnds[iTestCase];

			final double fixingTime	= periodStart;
			final double paymentTime	= periodEnd;

			final double periodLength	= periodEnd-periodStart;

			final DiscountCurveFromForwardCurve d2 = new DiscountCurveFromForwardCurve(indexCurve);
			final ForwardCurveFromDiscountCurve f2 = new ForwardCurveFromDiscountCurve(d2.getName(), liborMarketModel.getModel().getAnalyticModel().getForwardCurve(indexCurve).getReferenceDate(), null);
			final String forwardCurveName = f2.getName();

			final AbstractIndex index = new LIBORIndex(forwardCurveName, 0.0, periodLength);
			final Period period = new Period(periodStart, periodEnd, fixingTime, paymentTime, new NotionalFromConstant(1.0), index, periodLength, true, false, false);
			final double value = period.getValue(liborMarketModel);

			final AnalyticModel analyticModel = liborMarketModel.getModel().getAnalyticModel();
			final double forward = analyticModel.getForwardCurve(forwardCurveName).getForward(analyticModel, fixingTime, paymentTime-fixingTime);
			final double discountFactor = liborMarketModel.getModel().getDiscountCurve().getDiscountFactor(paymentTime);

			final double valueAnalytic = forward * periodLength * discountFactor;

			final double deviation = value - valueAnalytic;

			final double toleranceThisTest = tolerance[iTestCase]/Math.sqrt((liborMarketModel.getNumberOfPaths())/100000.0);

			System.out.println(
					formatDec2.format(periodStart) + "\t" + formatDec2.format(periodEnd) + "\t" +
							formatDec6.format(value) + "\t" +
							formatDec6.format(valueAnalytic) + "\t" +
							formatDec6.format(deviation) + "\t" +
							formatDec6.format(toleranceThisTest));
			Assert.assertTrue(Math.abs(deviation) < toleranceThisTest);
		}
		System.out.println();
	}


	public static AnalyticModel createCurves() throws SolverException, CloneNotSupportedException {

		/*
		 * Calibration of a single curve - OIS curve - self disocunted curve, from a set of calibration products.
		 */

		final LocalDate referenceDate = LocalDate.of(2012, 1,10);

		/*
		 * Define the calibration spec generators for our calibration products
		 */
		final Function<String,String> frequencyForTenor = new Function<String, String>() {
			@Override
			public String apply(final String tenor) {
				switch(tenor) {
				case "3M":
					return "quarterly";
				case "6M":
					return "semiannual";
				default:
					throw new IllegalArgumentException("Unkown tenor " + tenor);
				}
			}
		};

		final BiFunction<String, Double, CalibrationSpec> deposit = new BiFunction<String, Double, CalibrationSpec>() {
			@Override
			public CalibrationSpec apply(final String maturity, final Double rate) {
				final Schedule scheduleInterfaceRec = ScheduleGenerator.createScheduleFromConventions(referenceDate, 2, "0D", maturity, "tenor", "act/360", "first", "following", new BusinessdayCalendarExcludingTARGETHolidays(), 0, 0);
				final Schedule scheduleInterfacePay = null;
				final double calibrationTime = scheduleInterfaceRec.getPayment(scheduleInterfaceRec.getNumberOfPeriods()-1);
				final CalibrationSpec calibrationSpec = new CalibratedCurves.CalibrationSpec("EUR-OIS-" + maturity, "Deposit", scheduleInterfaceRec, "", rate, "discount-EUR-OIS", scheduleInterfacePay, null, 0.0, null, "discount-EUR-OIS", calibrationTime);
				return calibrationSpec;
			}
		};

		final BiFunction<String, Double, CalibrationSpec> swapSingleCurve = new BiFunction<String, Double, CalibrationSpec>() {
			@Override
			public CalibrationSpec apply(final String maturity, final Double rate) {
				final Schedule scheduleInterfaceRec = ScheduleGenerator.createScheduleFromConventions(referenceDate, 2, "0D", maturity, "annual", "act/360", "first", "modified_following", new BusinessdayCalendarExcludingTARGETHolidays(), 0, 1);
				final Schedule scheduleInterfacePay = ScheduleGenerator.createScheduleFromConventions(referenceDate, 2, "0D", maturity, "annual", "act/360", "first", "modified_following", new BusinessdayCalendarExcludingTARGETHolidays(), 0, 1);
				final double calibrationTime = scheduleInterfaceRec.getPayment(scheduleInterfaceRec.getNumberOfPeriods() - 1);
				final CalibrationSpec calibrationSpec = new CalibratedCurves.CalibrationSpec("EUR-OIS-" + maturity, "Swap", scheduleInterfaceRec, "forward-EUR-OIS", 0.0, "discount-EUR-OIS", scheduleInterfacePay, "", rate, "discount-EUR-OIS", "discount-EUR-OIS", calibrationTime);
				return calibrationSpec;
			}
		};

		final Function<String,BiFunction<String, Double, CalibrationSpec>> fra = new Function<String, BiFunction<String, Double, CalibrationSpec>>() {
			@Override
			public BiFunction<String, Double, CalibrationSpec> apply(final String tenor) {
				return new BiFunction<String, Double, CalibrationSpec>() {
					@Override
					public CalibrationSpec apply(final String fixing, final Double rate) {
						final Schedule scheduleInterfaceRec = ScheduleGenerator.createScheduleFromConventions(referenceDate, 2, fixing, tenor, "tenor", "act/360", "first", "modified_following", new BusinessdayCalendarExcludingTARGETHolidays(), 0, 0);
						final double calibrationTime = scheduleInterfaceRec.getFixing(scheduleInterfaceRec.getNumberOfPeriods() - 1);
						final String curveName = "forward-EUR-" + tenor;
						final CalibrationSpec calibrationSpec = new CalibratedCurves.CalibrationSpec("EUR-" + tenor + "-" + fixing, "FRA", scheduleInterfaceRec, curveName, rate, "discount-EUR-OIS", null, null, 0.0, null, curveName, calibrationTime);
						return calibrationSpec;
					}
				};
			}
		};

		final Function<String,BiFunction<String, Double, CalibrationSpec>> swap = new Function<String, BiFunction<String, Double, CalibrationSpec>>() {
			@Override
			public BiFunction<String, Double, CalibrationSpec> apply(final String tenor) {
				return new BiFunction<String, Double, CalibrationSpec>() {
					@Override
					public CalibrationSpec apply(final String maturity, final Double rate) {
						final String frequencyRec = frequencyForTenor.apply(tenor);

						final Schedule scheduleInterfaceRec = ScheduleGenerator.createScheduleFromConventions(referenceDate, 2, "0D", maturity, frequencyRec, "act/360", "first", "following", new BusinessdayCalendarExcludingTARGETHolidays(), 0, 0);
						final Schedule scheduleInterfacePay = ScheduleGenerator.createScheduleFromConventions(referenceDate, 2, "0D", maturity, "annual", "E30/360", "first", "following", new BusinessdayCalendarExcludingTARGETHolidays(), 0, 0);
						final double calibrationTime = scheduleInterfaceRec.getFixing(scheduleInterfaceRec.getNumberOfPeriods() - 1);
						final String curveName = "forward-EUR-" + tenor;
						final CalibrationSpec calibrationSpec = new CalibratedCurves.CalibrationSpec("EUR-" + tenor + maturity, "Swap", scheduleInterfaceRec, curveName, 0.0, "discount-EUR-OIS", scheduleInterfacePay, "", rate, "discount-EUR-OIS", curveName, calibrationTime);
						return calibrationSpec;
					}
				};
			}
		};

		final BiFunction<String,String,BiFunction<String, Double, CalibrationSpec>> swapBasis = new BiFunction<String, String, BiFunction<String, Double, CalibrationSpec>>() {
			@Override
			public BiFunction<String, Double, CalibrationSpec> apply(final String tenorRec, final String tenorPay) {
				return new BiFunction<String, Double, CalibrationSpec>() {
					@Override
					public CalibrationSpec apply(final String maturity, final Double rate) {
						final String curveNameRec = "forward-EUR-" + tenorRec;
						final String curveNamePay = "forward-EUR-" + tenorPay;

						final String frequencyRec = frequencyForTenor.apply(tenorRec);
						final String frequencyPay = frequencyForTenor.apply(tenorPay);

						final Schedule scheduleInterfaceRec = ScheduleGenerator.createScheduleFromConventions(referenceDate, 2, "0D", maturity, frequencyRec, "act/360", "first", "following", new BusinessdayCalendarExcludingTARGETHolidays(), 0, 0);
						final Schedule scheduleInterfacePay = ScheduleGenerator.createScheduleFromConventions(referenceDate, 2, "0D", maturity, frequencyPay, "act/360", "first", "following", new BusinessdayCalendarExcludingTARGETHolidays(), 0, 0);
						final double calibrationTime = scheduleInterfaceRec.getFixing(scheduleInterfaceRec.getNumberOfPeriods() - 1);

						final CalibrationSpec calibrationSpec = new CalibratedCurves.CalibrationSpec("EUR-" + tenorRec + "-" + tenorPay + maturity, "Swap", scheduleInterfaceRec, curveNameRec, 0.0, "discount-EUR-OIS", scheduleInterfacePay, curveNamePay, rate, "discount-EUR-OIS", curveNameRec, calibrationTime);
						return calibrationSpec;
					}
				};
			}
		};

		/*
		 * Generate empty curve template (for cloning during calibration)
		 */
		final double[] times = { 0.0 };
		final double[] discountFactors = { 1.0 };
		final boolean[] isParameter = { false };

		final DiscountCurveInterpolation discountCurveOIS = DiscountCurveInterpolation.createDiscountCurveFromDiscountFactors("discount-EUR-OIS", referenceDate, times, discountFactors, isParameter, InterpolationMethod.LINEAR, ExtrapolationMethod.CONSTANT, InterpolationEntity.LOG_OF_VALUE);
		final ForwardCurve forwardCurveOIS = new ForwardCurveFromDiscountCurve("forward-EUR-OIS", "discount-EUR-OIS", referenceDate, "3M");
		final ForwardCurve forwardCurve3M = new ForwardCurveInterpolation("forward-EUR-3M", referenceDate, "3M", new BusinessdayCalendarExcludingTARGETHolidays(), DateRollConvention.FOLLOWING, CurveInterpolation.InterpolationMethod.LINEAR, CurveInterpolation.ExtrapolationMethod.CONSTANT, CurveInterpolation.InterpolationEntity.VALUE,ForwardCurveInterpolation.InterpolationEntityForward.FORWARD, "discount-EUR-OIS");
		final ForwardCurve forwardCurve6M = new ForwardCurveInterpolation("forward-EUR-6M", referenceDate, "6M", new BusinessdayCalendarExcludingTARGETHolidays(), DateRollConvention.FOLLOWING, CurveInterpolation.InterpolationMethod.LINEAR, CurveInterpolation.ExtrapolationMethod.CONSTANT, CurveInterpolation.InterpolationEntity.VALUE,ForwardCurveInterpolation.InterpolationEntityForward.FORWARD, "discount-EUR-OIS");

		final AnalyticModelFromCurvesAndVols forwardCurveModel = new AnalyticModelFromCurvesAndVols(new Curve[] { discountCurveOIS, forwardCurveOIS, forwardCurve3M, forwardCurve6M });

		final List<CalibrationSpec> calibrationSpecs = new LinkedList<>();

		/*
		 * Calibration products for OIS curve: Deposits
		 */
		calibrationSpecs.add(deposit.apply("1D", 0.202 / 100.0));
		calibrationSpecs.add(deposit.apply("1W", 0.195 / 100.0));
		calibrationSpecs.add(deposit.apply("2W", 0.193 / 100.0));
		calibrationSpecs.add(deposit.apply("3W", 0.193 / 100.0));
		calibrationSpecs.add(deposit.apply("1M", 0.191 / 100.0));
		calibrationSpecs.add(deposit.apply("2M", 0.185 / 100.0));
		calibrationSpecs.add(deposit.apply("3M", 0.180 / 100.0));
		calibrationSpecs.add(deposit.apply("4M", 0.170 / 100.0));
		calibrationSpecs.add(deposit.apply("5M", 0.162 / 100.0));
		calibrationSpecs.add(deposit.apply("6M", 0.156 / 100.0));
		calibrationSpecs.add(deposit.apply("7M", 0.150 / 100.0));
		calibrationSpecs.add(deposit.apply("8M", 0.145 / 100.0));
		calibrationSpecs.add(deposit.apply("9M", 0.141 / 100.0));
		calibrationSpecs.add(deposit.apply("10M", 0.136 / 100.0));
		calibrationSpecs.add(deposit.apply("11M", 0.133 / 100.0));
		calibrationSpecs.add(deposit.apply("12M", 0.129 / 100.0));

		/*
		 * Calibration products for OIS curve: Swaps
		 */
		calibrationSpecs.add(swapSingleCurve.apply("15M", 0.118 / 100.0));
		calibrationSpecs.add(swapSingleCurve.apply("18M", 0.108 / 100.0));
		calibrationSpecs.add(swapSingleCurve.apply("21M", 0.101 / 100.0));
		calibrationSpecs.add(swapSingleCurve.apply("2Y", 0.101 / 100.0));
		calibrationSpecs.add(swapSingleCurve.apply("3Y", 0.194 / 100.0));
		calibrationSpecs.add(swapSingleCurve.apply("4Y", 0.346 / 100.0));
		calibrationSpecs.add(swapSingleCurve.apply("5Y", 0.534 / 100.0));
		calibrationSpecs.add(swapSingleCurve.apply("6Y", 0.723 / 100.0));
		calibrationSpecs.add(swapSingleCurve.apply("7Y", 0.895 / 100.0));
		calibrationSpecs.add(swapSingleCurve.apply("8Y", 1.054 / 100.0));
		calibrationSpecs.add(swapSingleCurve.apply("9Y", 1.189 / 100.0));
		calibrationSpecs.add(swapSingleCurve.apply("10Y", 1.310 / 100.0));
		calibrationSpecs.add(swapSingleCurve.apply("11Y", 1.423 / 100.0));
		calibrationSpecs.add(swapSingleCurve.apply("12Y", 1.520 / 100.0));
		calibrationSpecs.add(swapSingleCurve.apply("15Y", 1.723 / 100.0));
		calibrationSpecs.add(swapSingleCurve.apply("20Y", 1.826 / 100.0));
		calibrationSpecs.add(swapSingleCurve.apply("25Y", 1.877 / 100.0));
		calibrationSpecs.add(swapSingleCurve.apply("30Y", 1.910 / 100.0));
		calibrationSpecs.add(swapSingleCurve.apply("40Y", 2.025 / 100.0));
		calibrationSpecs.add(swapSingleCurve.apply("50Y", 2.101 / 100.0));

		/*
		 * Calibration products for 3M curve: FRAs
		 */
		calibrationSpecs.add(fra.apply("3M").apply("0D", 0.322 / 100.0));
		calibrationSpecs.add(fra.apply("3M").apply("1M", 0.329 / 100.0));
		calibrationSpecs.add(fra.apply("3M").apply("2M", 0.328 / 100.0));
		calibrationSpecs.add(fra.apply("3M").apply("3M", 0.326 / 100.0));
		calibrationSpecs.add(fra.apply("3M").apply("6M", 0.323 / 100.0));
		calibrationSpecs.add(fra.apply("3M").apply("9M", 0.316 / 100.0));
		calibrationSpecs.add(fra.apply("3M").apply("12M", 0.360 / 100.0));
		calibrationSpecs.add(fra.apply("3M").apply("15M", 0.390 / 100.0));

		/*
		 * Calibration products for 3M curve: swaps
		 */
		calibrationSpecs.add(swap.apply("3M").apply("2Y", 0.380 / 100.0));
		calibrationSpecs.add(swap.apply("3M").apply("3Y", 0.485 / 100.0));
		calibrationSpecs.add(swap.apply("3M").apply("4Y", 0.628 / 100.0));
		calibrationSpecs.add(swap.apply("3M").apply("5Y", 0.812 / 100.0));
		calibrationSpecs.add(swap.apply("3M").apply("6Y", 0.998 / 100.0));
		calibrationSpecs.add(swap.apply("3M").apply("7Y", 1.168 / 100.0));
		calibrationSpecs.add(swap.apply("3M").apply("8Y", 1.316 / 100.0));
		calibrationSpecs.add(swap.apply("3M").apply("9Y", 1.442 / 100.0));
		calibrationSpecs.add(swap.apply("3M").apply("10Y", 1.557 / 100.0));
		calibrationSpecs.add(swap.apply("3M").apply("12Y", 1.752 / 100.0));
		calibrationSpecs.add(swap.apply("3M").apply("15Y", 1.942 / 100.0));
		calibrationSpecs.add(swap.apply("3M").apply("20Y", 2.029 / 100.0));
		calibrationSpecs.add(swap.apply("3M").apply("25Y", 2.045 / 100.0));
		calibrationSpecs.add(swap.apply("3M").apply("30Y", 2.097 / 100.0));
		calibrationSpecs.add(swap.apply("3M").apply("40Y", 2.208 / 100.0));
		calibrationSpecs.add(swap.apply("3M").apply("50Y", 2.286 / 100.0));

		/*
		 * Calibration products for 6M curve: FRAs
		 */

		calibrationSpecs.add(fra.apply("6M").apply("0D", 0.590 / 100.0));
		calibrationSpecs.add(fra.apply("6M").apply("1M", 0.597 / 100.0));
		calibrationSpecs.add(fra.apply("6M").apply("2M", 0.596 / 100.0));
		calibrationSpecs.add(fra.apply("6M").apply("3M", 0.594 / 100.0));
		calibrationSpecs.add(fra.apply("6M").apply("6M", 0.591 / 100.0));
		calibrationSpecs.add(fra.apply("6M").apply("9M", 0.584 / 100.0));
		calibrationSpecs.add(fra.apply("6M").apply("12M", 0.584 / 100.0));

		/*
		 * Calibration products for 6M curve: tenor basis swaps
		 * Note: the fixed bases is added to the second argument tenor (here 3M).
		 */
		calibrationSpecs.add(swapBasis.apply("6M","3M").apply("2Y", 0.255 / 100.0));
		calibrationSpecs.add(swapBasis.apply("6M","3M").apply("3Y", 0.245 / 100.0));
		calibrationSpecs.add(swapBasis.apply("6M","3M").apply("4Y", 0.227 / 100.0));
		calibrationSpecs.add(swapBasis.apply("6M","3M").apply("5Y", 0.210 / 100.0));
		calibrationSpecs.add(swapBasis.apply("6M","3M").apply("6Y", 0.199 / 100.0));
		calibrationSpecs.add(swapBasis.apply("6M","3M").apply("7Y", 0.189 / 100.0));
		calibrationSpecs.add(swapBasis.apply("6M","3M").apply("8Y", 0.177 / 100.0));
		calibrationSpecs.add(swapBasis.apply("6M","3M").apply("9Y", 0.170 / 100.0));
		calibrationSpecs.add(swapBasis.apply("6M","3M").apply("10Y", 0.164 / 100.0));
		calibrationSpecs.add(swapBasis.apply("6M","3M").apply("12Y", 0.156 / 100.0));
		calibrationSpecs.add(swapBasis.apply("6M","3M").apply("15Y", 0.135 / 100.0));
		calibrationSpecs.add(swapBasis.apply("6M","3M").apply("20Y", 0.125 / 100.0));
		calibrationSpecs.add(swapBasis.apply("6M","3M").apply("25Y", 0.117 / 100.0));
		calibrationSpecs.add(swapBasis.apply("6M","3M").apply("30Y", 0.107 / 100.0));
		calibrationSpecs.add(swapBasis.apply("6M","3M").apply("40Y", 0.095 / 100.0));
		calibrationSpecs.add(swapBasis.apply("6M","3M").apply("50Y", 0.088 / 100.0));

		/*
		 * Calibrate
		 */
		final CalibratedCurves calibratedCurves = new CalibratedCurves(calibrationSpecs.toArray(new CalibrationSpec[calibrationSpecs.size()]), forwardCurveModel, 1E-15);

		/*
		 * Get the calibrated model
		 */
		final AnalyticModel calibratedModel = calibratedCurves.getModel();

		return calibratedModel;
	}

	public static LIBORModelMonteCarloSimulationModel createLIBORMarketModel(
			String forwardCurveName,
			final Measure measure,
			final int numberOfPaths, final int numberOfFactors, final double correlationDecayParam) throws CalculationException, SolverException, CloneNotSupportedException {

		/*
		 * Create the libor tenor structure and the initial values
		 */
		final double liborPeriodLength	= 0.5;
		final double liborRateTimeHorzion	= 20.0;
		final TimeDiscretizationFromArray liborPeriodDiscretization = new TimeDiscretizationFromArray(0.0, (int) (liborRateTimeHorzion / liborPeriodLength), liborPeriodLength);

		final LocalDate referenceDate = LocalDate.of(2014, Month.SEPTEMBER, 16);

		final double[] nssParameters = new double[] { 0.02 , -0.01, 0.16, -0.17, 4.5, 3.5 };

		/*
		 * Create forwardCurve and discountCurve. The two need to fit to each other for this test.
		 */
		AnalyticModel analyticModel = createCurves();

		final DiscountCurveFromForwardCurve d = new DiscountCurveFromForwardCurve(FORWARD_CURVE_NAME);
		final ForwardCurveFromDiscountCurve f = new ForwardCurveFromDiscountCurve(d.getName(), d.getReferenceDate(), null);

		final DiscountCurveFromForwardCurve d2 = new DiscountCurveFromForwardCurve(FORWARD_CURVE_NAME2);
		final ForwardCurveFromDiscountCurve f2 = new ForwardCurveFromDiscountCurve(d2.getName(), d.getReferenceDate(), null);

		analyticModel = analyticModel.addCurves(d, d2, f, f2);
		forwardCurveName = f.getName();//forwardCurveName.equals(FORWARD_CURVE_NAME) ? f.getName() : f2.getName();

		/*
		 * Create a simulation time discretization
		 */
		final double lastTime	= 20.0;
		final double dt		= 0.50;

		final TimeDiscretizationFromArray timeDiscretizationFromArray = new TimeDiscretizationFromArray(0.0, (int) (lastTime / dt), dt);

		/*
		 * Create a volatility structure v[i][j] = sigma_j(t_i)
		 */
		final double[][] volatility = new double[timeDiscretizationFromArray.getNumberOfTimeSteps()][liborPeriodDiscretization.getNumberOfTimeSteps()];
		for (int timeIndex = 0; timeIndex < volatility.length; timeIndex++) {
			for (int liborIndex = 0; liborIndex < volatility[timeIndex].length; liborIndex++) {
				// Create a very simple volatility model here
				final double time = timeDiscretizationFromArray.getTime(timeIndex);
				final double maturity = liborPeriodDiscretization.getTime(liborIndex);
				final double timeToMaturity = maturity - time;

				double instVolatility;
				if(timeToMaturity <= 0) {
					instVolatility = 0;				// This forward rate is already fixed, no volatility
				} else {
					instVolatility = 0.3 + 0.2 * Math.exp(-0.25 * timeToMaturity);
				}

				// Store
				volatility[timeIndex][liborIndex] = instVolatility;
			}
		}
		final LIBORVolatilityModelFromGivenMatrix volatilityModel = new LIBORVolatilityModelFromGivenMatrix(timeDiscretizationFromArray, liborPeriodDiscretization, volatility);

		/*
		 * Create a correlation model rho_{i,j} = exp(-a * abs(T_i-T_j))
		 */
		final LIBORCorrelationModelExponentialDecay correlationModel = new LIBORCorrelationModelExponentialDecay(
				timeDiscretizationFromArray, liborPeriodDiscretization, numberOfFactors,
				correlationDecayParam);


		/*
		 * Combine volatility model and correlation model to a covariance model
		 */
		final LIBORCovarianceModelFromVolatilityAndCorrelation covarianceModel =
				new LIBORCovarianceModelFromVolatilityAndCorrelation(timeDiscretizationFromArray,
						liborPeriodDiscretization, volatilityModel, correlationModel);

		// BlendedLocalVolatlityModel (future extension)
		//		AbstractLIBORCovarianceModel covarianceModel2 = new BlendedLocalVolatlityModel(covarianceModel, 0.00, false);

		// Set model properties
		final Map<String, String> properties = new HashMap<>();

		// Choose the simulation measure
		properties.put("measure", measure.name());

		// Choose log normal model
		properties.put("stateSpace", LIBORMarketModelFromCovarianceModel.StateSpace.LOGNORMAL.name());

		// Empty array of calibration items - hence, model will use given covariance
		final CalibrationProduct[] calibrationItems = new CalibrationProduct[0];

		/*
		 * Create corresponding LIBOR Market Model
		 */
		final LIBORMarketModel liborMarketModel = new LIBORMarketModelFromCovarianceModel(liborPeriodDiscretization, analyticModel, analyticModel.getForwardCurve(forwardCurveName),  analyticModel.getDiscountCurve(DISCOUNT_CURVE_NAME), covarianceModel, calibrationItems, properties);

		final EulerSchemeFromProcessModel process = new EulerSchemeFromProcessModel(
				liborMarketModel,
				new net.finmath.montecarlo.BrownianMotionLazyInit(timeDiscretizationFromArray,
						numberOfFactors, numberOfPaths, 8787 /* seed */));
		//		process.setScheme(EulerSchemeFromProcessModel.Scheme.PREDICTOR_CORRECTOR);

		return new LIBORMonteCarloSimulationFromLIBORModel(liborMarketModel, process);
	}
}
