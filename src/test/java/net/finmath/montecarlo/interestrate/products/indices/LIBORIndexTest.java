/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
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

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.marketdata.model.curves.CurveInterface;
import net.finmath.marketdata.model.curves.DiscountCurveFromForwardCurve;
import net.finmath.marketdata.model.curves.DiscountCurveInterface;
import net.finmath.marketdata.model.curves.DiscountCurveNelsonSiegelSvensson;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurveInterface;
import net.finmath.marketdata.model.curves.ForwardCurveNelsonSiegelSvensson;
import net.finmath.montecarlo.interestrate.LIBORMarketModel;
import net.finmath.montecarlo.interestrate.LIBORMarketModel.Measure;
import net.finmath.montecarlo.interestrate.LIBORMarketModelInterface;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulation;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORCorrelationModelExponentialDecay;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORCovarianceModelFromVolatilityAndCorrelation;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORVolatilityModelFromGivenMatrix;
import net.finmath.montecarlo.interestrate.products.components.AbstractProductComponent;
import net.finmath.montecarlo.interestrate.products.components.Notional;
import net.finmath.montecarlo.interestrate.products.components.Period;
import net.finmath.montecarlo.interestrate.products.components.ProductCollection;
import net.finmath.montecarlo.process.ProcessEulerScheme;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingTARGETHolidays;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarInterface;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarInterface.DateRollConvention;
import net.finmath.time.daycount.DayCountConventionInterface;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

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
	@Parameters
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
	};

	private final int numberOfFactors = 5;
	private final double correlationDecayParam = 0.05;
	
	private double[] periodStarts	= { 2.00, 2.00, 2.00, 2.50, 2.50, 2.50, 2.00, 2.00, 2.25 , 4.00 };
	private double[] periodEnds		= { 2.50, 2.25, 3.00, 3.00, 3.25, 3.50, 4.00, 5.00, 2.50 , 5.00 };
	private double[] tolerance		= { 3E-4, 3E-4, 3E-4, 3E-4, 3E-4, 3E-4, 3E-4, 3E-4, 3E-4 , 3E-4 };		// Tolerance at 100.000 path
	
	private LIBORModelMonteCarloSimulationInterface liborMarketModel; 

	public LIBORIndexTest(Integer numberOfPaths, CurveSetup curveSetup) throws CalculationException {

		// Create a LIBOR market model
		liborMarketModel = createLIBORMarketModel(curveSetup, Measure.SPOT, numberOfPaths, numberOfFactors, correlationDecayParam);
	}

	@Test
	public void testSinglePeriods() throws CalculationException {
		
		NumberFormat formatDec2 = new DecimalFormat("0.00");
		NumberFormat formatDec6 = new DecimalFormat("0.000000");
		
		for(int iTestCase = 0; iTestCase<periodStarts.length; iTestCase++) {
			double periodStart	= periodStarts[iTestCase];
			double periodEnd	= periodEnds[iTestCase];
			double periodLength	= periodEnd-periodStart;

			AbstractIndex index = new LIBORIndex(0.0, periodLength);
			Period period = new Period(periodStart, periodEnd, periodStart, periodEnd, new Notional(1.0), index, periodLength, true, true, false);
			double value = period.getValue(liborMarketModel);

			double toleranceThisTest = tolerance[iTestCase]/Math.sqrt(((double)liborMarketModel.getNumberOfPaths())/100000.0);
			
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
		
		double tolerance = 2E-3;
		ArrayList<AbstractProductComponent> periods = new ArrayList<AbstractProductComponent>();
		for(int iPeriod = 0; iPeriod<10; iPeriod++) {
			double periodStart	= 2.0 + 0.5 * iPeriod;
			double periodEnd	= 2.0 + 0.5 * (iPeriod+1);
			double periodLength	= periodEnd-periodStart;

			AbstractIndex index = new LIBORIndex(0.0, periodLength);
			Period period = new Period(periodStart, periodEnd, periodStart, periodEnd, new Notional(1.0), index, periodLength, true, true, false);
			periods.add(period);
		}
		AbstractProductComponent floater = new ProductCollection(periods);
		double value = floater.getValue(liborMarketModel);
		
		double toleranceThisTest = tolerance/Math.sqrt(((double)liborMarketModel.getNumberOfPaths())/100000.0);

		NumberFormat formatDec6 = new DecimalFormat("0.000000");
		System.out.println(
				formatDec6.format(value) + "\t" +
						formatDec6.format(toleranceThisTest));

		Assert.assertEquals("Deviation", 0.0, value, toleranceThisTest);
	}

	public static LIBORModelMonteCarloSimulationInterface createLIBORMarketModel(
			CurveSetup curveSetup,
			Measure measure,
			int numberOfPaths, int numberOfFactors, double correlationDecayParam) throws CalculationException {

		/*
		 * Create the libor tenor structure and the initial values
		 */
		double liborPeriodLength	= 0.5;
		double liborRateTimeHorzion	= 20.0;
		TimeDiscretization liborPeriodDiscretization = new TimeDiscretization(0.0, (int) (liborRateTimeHorzion / liborPeriodLength), liborPeriodLength);

		LocalDate referenceDate = LocalDate.of(2014, Month.SEPTEMBER, 16);

		double[] nssParameters = new double[] { 0.02 , -0.01, 0.16, -0.17, 4.5, 3.5 };

		/*
		 * Create forwardCurve and disocuntCurve. The two need to fit to each other for this test.
		 */
		DiscountCurveInterface discountCurve;
		ForwardCurveInterface forwardCurve;
		switch(curveSetup) {
		case NSS:
		{
			discountCurve = new DiscountCurveNelsonSiegelSvensson("EUR Curve", referenceDate, nssParameters, 1.0);

			String paymentOffsetCode = "6M";
			BusinessdayCalendarInterface paymentBusinessdayCalendar = new BusinessdayCalendarExcludingTARGETHolidays();
			BusinessdayCalendarInterface.DateRollConvention paymentDateRollConvention = DateRollConvention.MODIFIED_FOLLOWING;
			DayCountConventionInterface daycountConvention = null;//new DayCountConvention_ACT_360();

			forwardCurve = new ForwardCurveNelsonSiegelSvensson("EUR Curve", referenceDate, paymentOffsetCode, paymentBusinessdayCalendar, paymentDateRollConvention, daycountConvention, nssParameters, 1.0, 0.0);
			break;
		}
		case DISCRETE:
		{
			// Create the forward curve (initial value of the LIBOR market model)
			forwardCurve = ForwardCurve.createForwardCurveFromForwards(
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

		AnalyticModelInterface analyticModel = new AnalyticModel(new CurveInterface[] { discountCurve, forwardCurve });
		/*
		 * Create a simulation time discretization
		 */
		double lastTime	= 20.0;
		double dt		= 0.50;

		TimeDiscretization timeDiscretization = new TimeDiscretization(0.0, (int) (lastTime / dt), dt);

		/*
		 * Create a volatility structure v[i][j] = sigma_j(t_i)
		 */
		double[][] volatility = new double[timeDiscretization.getNumberOfTimeSteps()][liborPeriodDiscretization.getNumberOfTimeSteps()];
		for (int timeIndex = 0; timeIndex < volatility.length; timeIndex++) {
			for (int liborIndex = 0; liborIndex < volatility[timeIndex].length; liborIndex++) {
				// Create a very simple volatility model here
				double time = timeDiscretization.getTime(timeIndex);
				double maturity = liborPeriodDiscretization.getTime(liborIndex);
				double timeToMaturity = maturity - time;

				double instVolatility;
				if(timeToMaturity <= 0)
					instVolatility = 0;				// This forward rate is already fixed, no volatility
				else
					instVolatility = 0.3 + 0.2 * Math.exp(-0.25 * timeToMaturity);

				// Store
				volatility[timeIndex][liborIndex] = instVolatility;
			}
		}
		LIBORVolatilityModelFromGivenMatrix volatilityModel = new LIBORVolatilityModelFromGivenMatrix(timeDiscretization, liborPeriodDiscretization, volatility);

		/*
		 * Create a correlation model rho_{i,j} = exp(-a * abs(T_i-T_j))
		 */
		LIBORCorrelationModelExponentialDecay correlationModel = new LIBORCorrelationModelExponentialDecay(
				timeDiscretization, liborPeriodDiscretization, numberOfFactors,
				correlationDecayParam);


		/*
		 * Combine volatility model and correlation model to a covariance model
		 */
		LIBORCovarianceModelFromVolatilityAndCorrelation covarianceModel =
				new LIBORCovarianceModelFromVolatilityAndCorrelation(timeDiscretization,
						liborPeriodDiscretization, volatilityModel, correlationModel);

		// BlendedLocalVolatlityModel (future extension)
		//		AbstractLIBORCovarianceModel covarianceModel2 = new BlendedLocalVolatlityModel(covarianceModel, 0.00, false);

		// Set model properties
		Map<String, String> properties = new HashMap<String, String>();

		// Choose the simulation measure
		properties.put("measure", measure.name());
		
		// Choose log normal model
		properties.put("stateSpace", LIBORMarketModel.StateSpace.LOGNORMAL.name());

		// Empty array of calibration items - hence, model will use given covariance
		LIBORMarketModel.CalibrationItem[] calibrationItems = new LIBORMarketModel.CalibrationItem[0];

		/*
		 * Create corresponding LIBOR Market Model
		 */
		LIBORMarketModelInterface liborMarketModel = new LIBORMarketModel(liborPeriodDiscretization, analyticModel, forwardCurve, discountCurve, covarianceModel, calibrationItems, properties);
		//		LIBORMarketModel(liborPeriodDiscretization, forwardRateCurve, null, covarianceModel, calibrationItems, properties);

		ProcessEulerScheme process = new ProcessEulerScheme(
				new net.finmath.montecarlo.BrownianMotion(timeDiscretization,
						numberOfFactors, numberOfPaths, 8787 /* seed */));
		//		process.setScheme(ProcessEulerScheme.Scheme.PREDICTOR_CORRECTOR);

		return new LIBORModelMonteCarloSimulation(liborMarketModel, process);
	}
}
