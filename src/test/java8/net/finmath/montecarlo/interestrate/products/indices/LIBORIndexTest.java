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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.AnalyticModelFromCurvesAndVols;
import net.finmath.marketdata.model.curves.Curve;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.DiscountCurveFromForwardCurve;
import net.finmath.marketdata.model.curves.DiscountCurveNelsonSiegelSvensson;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurveInterpolation;
import net.finmath.marketdata.model.curves.ForwardCurveNelsonSiegelSvensson;
import net.finmath.montecarlo.interestrate.CalibrationProduct;
import net.finmath.montecarlo.interestrate.LIBORMarketModel;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.LIBORMonteCarloSimulationFromLIBORModel;
import net.finmath.montecarlo.interestrate.models.LIBORMarketModelFromCovarianceModel;
import net.finmath.montecarlo.interestrate.models.LIBORMarketModelFromCovarianceModel.Measure;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCorrelationModelExponentialDecay;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCovarianceModelFromVolatilityAndCorrelation;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORVolatilityModelFromGivenMatrix;
import net.finmath.montecarlo.interestrate.products.components.AbstractProductComponent;
import net.finmath.montecarlo.interestrate.products.components.NotionalFromConstant;
import net.finmath.montecarlo.interestrate.products.components.Period;
import net.finmath.montecarlo.interestrate.products.components.ProductCollection;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;
import net.finmath.time.businessdaycalendar.BusinessdayCalendar;
import net.finmath.time.businessdaycalendar.BusinessdayCalendar.DateRollConvention;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingTARGETHolidays;
import net.finmath.time.daycount.DayCountConvention;

/**
 * @author Christian Fries
 */
@RunWith(Parameterized.class)
public class LIBORIndexTest {

	public enum CurveSetup {
		NSS,				// Uses NSS curve
		DISCRETE			// Uses forward and discount curve with interpolation points.
	}

	/**
	 * The parameters for this test, that is an error consisting of
	 * { numberOfPaths, setup }.
	 *
	 * @return Array of parameters.
	 */
	@Parameters(name="{1}-{0}")
	public static Collection<Object[]> generateData()
	{
		return Arrays.asList(new Object[][] {
			{ new Integer(1000) , CurveSetup.NSS },
			{ new Integer(2000) , CurveSetup.NSS },
			{ new Integer(4000) , CurveSetup.NSS },
			{ new Integer(8000) , CurveSetup.NSS },
			{ new Integer(10000) , CurveSetup.NSS },
			{ new Integer(20000) , CurveSetup.NSS },
			{ new Integer(40000) , CurveSetup.NSS },
			{ new Integer(80000) , CurveSetup.NSS },
			{ new Integer(100000) , CurveSetup.NSS },
			{ new Integer(200000) , CurveSetup.NSS },
			{ new Integer(1000) , CurveSetup.DISCRETE },
			{ new Integer(2000) , CurveSetup.DISCRETE },
			{ new Integer(4000) , CurveSetup.DISCRETE },
			{ new Integer(8000) , CurveSetup.DISCRETE },
			{ new Integer(10000) , CurveSetup.DISCRETE },
			{ new Integer(20000) , CurveSetup.DISCRETE },
			{ new Integer(40000) , CurveSetup.DISCRETE },
			{ new Integer(80000) , CurveSetup.DISCRETE },
			{ new Integer(100000) , CurveSetup.DISCRETE },
			{ new Integer(200000) , CurveSetup.DISCRETE },
		});
	}

	private final int numberOfFactors = 5;
	private final double correlationDecayParam = 0.05;

	private final double[] periodStarts	= { 2.00, 2.00, 2.00, 2.50, 2.50, 2.50, 2.00, 2.00, 2.25 , 4.00 };
	private final double[] periodEnds		= { 2.50, 2.25, 3.00, 3.00, 3.25, 3.50, 4.00, 5.00, 2.50 , 5.00 };
	private final double[] tolerance		= { 3E-4, 3E-4, 3E-4, 3E-4, 3E-4, 3E-4, 3E-4, 3E-4, 3E-4 , 3E-4 };		// Tolerance at 100.000 path

	private final LIBORModelMonteCarloSimulationModel liborMarketModel;

	public LIBORIndexTest(final Integer numberOfPaths, final CurveSetup curveSetup) throws CalculationException {

		// Create a LIBOR market model
		liborMarketModel = createLIBORMarketModel(curveSetup, Measure.SPOT, numberOfPaths, numberOfFactors, correlationDecayParam);
	}

	@Test
	public void testSinglePeriods() throws CalculationException {

		final NumberFormat formatDec2 = new DecimalFormat("0.00");
		final NumberFormat formatDec6 = new DecimalFormat("0.000000");

		for(int iTestCase = 0; iTestCase<periodStarts.length; iTestCase++) {
			final double periodStart	= periodStarts[iTestCase];
			final double periodEnd	= periodEnds[iTestCase];
			final double periodLength	= periodEnd-periodStart;

			final AbstractIndex index = new LIBORIndex(0.0, periodLength);
			final Period period = new Period(periodStart, periodEnd, periodStart, periodEnd, new NotionalFromConstant(1.0), index, periodLength, true, true, false);
			final double value = period.getValue(liborMarketModel);

			final double toleranceThisTest = tolerance[iTestCase]/Math.sqrt((liborMarketModel.getNumberOfPaths())/100000.0);

			System.out.println(
					formatDec2.format(periodStart) + "\t" + formatDec2.format(periodEnd) + "\t" +
							formatDec6.format(value) + "\t" +
							formatDec6.format(toleranceThisTest));
			Assert.assertTrue(Math.abs(value) < toleranceThisTest);
		}
		System.out.println();
	}

	@Test
	public void testMultiPeriodFloater() throws CalculationException {

		final double tolerance = 2E-3;
		final ArrayList<AbstractProductComponent> periods = new ArrayList<>();
		for(int iPeriod = 0; iPeriod<10; iPeriod++) {
			final double periodStart	= 2.0 + 0.5 * iPeriod;
			final double periodEnd	= 2.0 + 0.5 * (iPeriod+1);
			final double periodLength	= periodEnd-periodStart;

			final AbstractIndex index = new LIBORIndex(0.0, periodLength);
			final Period period = new Period(periodStart, periodEnd, periodStart, periodEnd, new NotionalFromConstant(1.0), index, periodLength, true, true, false);
			periods.add(period);
		}
		final AbstractProductComponent floater = new ProductCollection(periods);
		final double value = floater.getValue(liborMarketModel);

		final double toleranceThisTest = tolerance/Math.sqrt((liborMarketModel.getNumberOfPaths())/100000.0);

		final NumberFormat formatDec6 = new DecimalFormat("0.000000");
		System.out.println(
				formatDec6.format(value) + "\t" +
						formatDec6.format(toleranceThisTest));

		Assert.assertEquals("Deviation", 0.0, value, toleranceThisTest);
	}

	@Test
	public void testUnalignedPeriods() throws CalculationException {

		final NumberFormat formatDec2 = new DecimalFormat("0.00");
		final NumberFormat formatDec6 = new DecimalFormat("0.000000");

		final TimeDiscretization liborPeriodDiscretization = liborMarketModel.getLiborPeriodDiscretization();

		for(int iPeriodStart=liborPeriodDiscretization.getNumberOfTimeSteps()-2; iPeriodStart < liborPeriodDiscretization.getNumberOfTimeSteps()-1; iPeriodStart++) {
			double periodStart	= liborPeriodDiscretization.getTime(3);
			double periodEnd	= liborPeriodDiscretization.getTime(iPeriodStart+1);
			final double periodLength	= periodEnd-periodStart;

			// Shift period by half libor period
			periodStart	+= liborPeriodDiscretization.getTime(4)-liborPeriodDiscretization.getTime(3);
			periodEnd	+= liborPeriodDiscretization.getTime(4)-liborPeriodDiscretization.getTime(3);

			final AbstractIndex index = new LIBORIndex(0.0, periodLength);
			final Period period = new Period(periodStart, periodEnd, periodStart, periodEnd, new NotionalFromConstant(1.0), index, periodLength, true, true, false);
			final double value = period.getValue(liborMarketModel);

			final double oneBasisPoint = 1.0 / 100.0 / 100.0;
			final double toleranceThisTest = oneBasisPoint/Math.sqrt((liborMarketModel.getNumberOfPaths())/200000.0);

			Assert.assertEquals(0.0, value / periodLength, toleranceThisTest);

			System.out.println(
					formatDec2.format(periodStart) + "\t" + formatDec2.format(periodEnd) + "\t" +
							formatDec6.format(value) + "\t");
		}
		System.out.println();
	}

	public static LIBORModelMonteCarloSimulationModel createLIBORMarketModel(
			final CurveSetup curveSetup,
			final Measure measure,
			final int numberOfPaths, final int numberOfFactors, final double correlationDecayParam) throws CalculationException {

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
		DiscountCurve discountCurve;
		ForwardCurve forwardCurve;
		switch(curveSetup) {
		case NSS:
		{
			discountCurve = new DiscountCurveNelsonSiegelSvensson("EUR CurveFromInterpolationPoints", referenceDate, nssParameters, 1.0);

			final String paymentOffsetCode = "6M";
			final BusinessdayCalendar paymentBusinessdayCalendar = new BusinessdayCalendarExcludingTARGETHolidays();
			final BusinessdayCalendar.DateRollConvention paymentDateRollConvention = DateRollConvention.MODIFIED_FOLLOWING;
			final DayCountConvention daycountConvention = null;//new DayCountConvention_ACT_360();

			forwardCurve = new ForwardCurveNelsonSiegelSvensson("EUR CurveFromInterpolationPoints", referenceDate, paymentOffsetCode, paymentBusinessdayCalendar, paymentDateRollConvention, daycountConvention, nssParameters, 1.0, 0.0);
			break;
		}
		case DISCRETE:
		{
			// Create the forward curve (initial value of the LIBOR market model)
			forwardCurve = ForwardCurveInterpolation.createForwardCurveFromForwards(
					"forwardCurve"								/* name of the curve */,
					new double[] {0.5 , 1.0 , 2.0 , 5.0 , 40.0}	/* fixings of the forward */,
					new double[] {0.02, 0.025, 0.03, 0.035, 0.04}	/* forwards */,
					liborPeriodLength							/* tenor / period length */
					);

			// Create the discount curve
			discountCurve = new DiscountCurveFromForwardCurve(forwardCurve);
			break;
		}
		default:
			throw new IllegalArgumentException("Unknown curve setup: " + curveSetup.toString());
		}

		final AnalyticModel analyticModel = new AnalyticModelFromCurvesAndVols(new Curve[] { discountCurve, forwardCurve });
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
		final LIBORMarketModel liborMarketModel = new LIBORMarketModelFromCovarianceModel(liborPeriodDiscretization, analyticModel, forwardCurve, discountCurve, covarianceModel, calibrationItems, properties);
		//		LIBORMarketModelFromCovarianceModel(liborPeriodDiscretization, forwardRateCurve, null, covarianceModel, calibrationItems, properties);

		final EulerSchemeFromProcessModel process = new EulerSchemeFromProcessModel(
				liborMarketModel,
				new net.finmath.montecarlo.BrownianMotionLazyInit(timeDiscretizationFromArray,
						numberOfFactors, numberOfPaths, 8787 /* seed */));
		//		process.setScheme(EulerSchemeFromProcessModel.Scheme.PREDICTOR_CORRECTOR);

		return new LIBORMonteCarloSimulationFromLIBORModel(liborMarketModel, process);
	}
}
