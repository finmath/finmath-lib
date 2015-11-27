/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 30.11.2013
 */
package net.finmath.marketdata.model.volatilities;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Vector;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.marketdata.model.curves.CurveInterface;
import net.finmath.marketdata.model.curves.DiscountCurveInterface;
import net.finmath.marketdata.model.curves.DiscountCurveNelsonSiegelSvensson;
import net.finmath.marketdata.model.curves.ForwardCurveInterface;
import net.finmath.marketdata.model.curves.ForwardCurveNelsonSiegelSvensson;
import net.finmath.marketdata.model.volatilities.VolatilitySurfaceInterface.QuotingConvention;
import net.finmath.marketdata.products.AnalyticProductInterface;
import net.finmath.marketdata.products.Cap;
import net.finmath.optimizer.SolverException;
import net.finmath.time.ScheduleGenerator;
import net.finmath.time.ScheduleGenerator.DaycountConvention;
import net.finmath.time.ScheduleGenerator.Frequency;
import net.finmath.time.ScheduleGenerator.ShortPeriodConvention;
import net.finmath.time.businessdaycalendar.BusinessdayCalendar;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingTARGETHolidays;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingWeekends;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarInterface;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarInterface.DateRollConvention;
import net.finmath.time.daycount.DayCountConventionInterface;
import net.finmath.time.daycount.DayCountConvention_ACT_360;
import net.finmath.time.daycount.DayCountConvention_ACT_365;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * This class makes some basic tests related to the setup, use and calibration of discount curves and forward curve.
 * 
 * @author Christian Fries
 */
@RunWith(Parameterized.class)
public class CapletVolatilitiesParametricCalibrationTest {

	static final double errorTolerance = 1E-5;

	private final QuotingConvention calibrationTargetValueQuotingConvention;

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
				{ QuotingConvention.VOLATILITYLOGNORMAL },
				{ QuotingConvention.VOLATILITYNORMAL },
		});
	};

	
	public CapletVolatilitiesParametricCalibrationTest(QuotingConvention calibrationTargetValueQuotingConvention) {
		super();
		this.calibrationTargetValueQuotingConvention = calibrationTargetValueQuotingConvention;
		
		System.out.println("Running calibration test using quoting convention " + calibrationTargetValueQuotingConvention.name() + " for calibration objective function.");
	}


	/**
	 * Run some test using discount curves and forward curves and the solver to create a calibrated model.
	 * 
	 * @param args Arguments - not used.
	 * @throws SolverException Thrown if the solver cannot find a solution to the calibration problem.
	 * @throws CalculationException Thrown if the calibration product failed.
	 */
	public static void main(String[] args) throws SolverException, CalculationException {

		CapletVolatilitiesParametricCalibrationTest calibrationTest = new CapletVolatilitiesParametricCalibrationTest(QuotingConvention.VOLATILITYLOGNORMAL);

		calibrationTest.testVolatilityCalibration();
	}

	@Test
	public void testVolatilityCalibration() throws CalculationException, SolverException {

		/*
		 * CREATING AND USING A DISCOUNT CURVE
		 */

		// Create a discount curve
		DiscountCurveInterface		discountCurve					= new DiscountCurveNelsonSiegelSvensson(
				"EUR",
				LocalDate.of(2014, Month.JULY, 15),
				new double[]
						{
								0.02,
								-0.01,
								0.14,
								-0.1,
								4.0,
								3.0
						}
				, 365.0/365.0);

		ForwardCurveInterface	forwardCurve = new ForwardCurveNelsonSiegelSvensson("EUR FWD", LocalDate.of(2014, Month.JULY, 17), 
				"3M", new BusinessdayCalendarExcludingTARGETHolidays(), BusinessdayCalendarInterface.DateRollConvention.MODIFIED_FOLLOWING, new DayCountConvention_ACT_360(),
				discountCurve.getParameter(), 365.0/365.0, 0.0);

		String[] maturities		= { "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "12Y", "15Y", "20Y" };
		Double[] volatilities	= { 0.72, 0.86, 0.75, 0.69, 0.67, 0.64, 0.58, 0.52, 0.47, 0.44, 0.40, 0.35, 0.32 };

		// A model is a collection of curves (curves and products find other curves by looking up their name in the model)
		AnalyticModel model = new AnalyticModel(new CurveInterface[] { discountCurve , forwardCurve });

		System.out.println("Given a discount curve:");
		System.out.println(discountCurve.toString());

		// We may ask the forward curve for a forward.
		double fixingTime	= 1.0;
		double forwardRate	= forwardCurve.getForward(model, fixingTime);
		System.out.println("Quaterly forward with fixing in " + fixingTime + " requested from forward curve is " + forwardRate);

		// Check if we have the right value
		double periodLength		= (92.0+2.0)/365.0;					// 3M + 2D rolling
		double daycountFraction	= periodLength * 365.0/360.0;		// ACT/360 instead of ACT/365
		double forwardRateFromDiscountFactor = (discountCurve.getDiscountFactor(model, fixingTime) / discountCurve.getDiscountFactor(model, fixingTime + periodLength) - 1) / daycountFraction;
		System.out.println("Quaterly forward with fixing in " + fixingTime + " calculated from that discount curve is " + forwardRateFromDiscountFactor);

		Assert.assertTrue(Math.abs(forwardRate - forwardRateFromDiscountFactor) < errorTolerance);

		/*
		 * Create volatility surface
		 */
		double a = 0.5, b = 1.00, c = 0.5, d = 0.20;

		AbstractVolatilitySurfaceParametric capletVolatility = new CapletVolatilitiesParametric("Caplet", null, forwardCurve, discountCurve, a, b, c, d, 1.0);
		//		AbstractVolatilitySurfaceParametric capletVolatility = new CapletVolatilitiesParametricFourParameterPicewiseConstant("Caplet", null, a, b, c, d, new TimeDiscretization(0.0, 100, 0.5));


		/*
		 * Convert given market products (which are in VOLATILITYLOGNORMAL) to the calibration (objective function) quoting conventions
		 */
		Vector<AnalyticProductInterface>	marketProducts = new Vector<AnalyticProductInterface>();
		ArrayList<Double>					marketTargetValues = new ArrayList<Double>();
		LocalDate referenceDate = LocalDate.of(2014,  Month.JULY,  15);
		DayCountConventionInterface modelDayCountConvention = new DayCountConvention_ACT_365();
		DaycountConvention capDayCountConvention = DaycountConvention.ACT_360;
		for(int i=0; i<maturities.length; i++) {
			LocalDate	tradeDate		= referenceDate;
			LocalDate	maturityDate	= BusinessdayCalendar.createDateFromDateAndOffsetCode(referenceDate, maturities[i]);
			double		volatility		= volatilities[i];
			Cap cap = new Cap(
					ScheduleGenerator.createScheduleFromConventions(referenceDate, tradeDate, maturityDate, Frequency.SEMIANNUAL, capDayCountConvention, ShortPeriodConvention.FIRST, DateRollConvention.FOLLOWING, new BusinessdayCalendarExcludingWeekends(), 0, 0),
					forwardCurve.getName(),
					0.0,
					true,
					discountCurve.getName(),
					capletVolatility.getName(),
					calibrationTargetValueQuotingConvention);

			// Create flat lognormal surface under the same name, using market quoted lognormal volatility.
			AbstractVolatilitySurfaceParametric flatSurface = new CapletVolatilitiesParametric(capletVolatility.getName(), null, forwardCurve, discountCurve, 0, 0, 0, volatility, 1.0);
			AnalyticModelInterface flatModel = model.clone().addVolatilitySurfaces(flatSurface);

			// Convert to calibrationTargetValueQuotingConvention
			double valueMarket = cap.getValue(0.0, flatModel);

			marketProducts.add(cap);
			marketTargetValues.add(new Double(valueMarket));
		}

		/*
		 * Calibrate a surface to given marketTargetValues using the calibrationTargetValueQuotingConvention
		 */
		Vector<AnalyticProductInterface>	calibrationProducts = new Vector<AnalyticProductInterface>();
		ArrayList<Double>					calibrationTargetValues = new ArrayList<Double>();
		for(int i=0; i<maturities.length; i++) {
			LocalDate	tradeDate		= referenceDate;
			LocalDate	maturityDate	= BusinessdayCalendar.createDateFromDateAndOffsetCode(referenceDate, maturities[i]);
			double		volatility		= volatilities[i];
			Cap cap = new Cap(
					ScheduleGenerator.createScheduleFromConventions(referenceDate, tradeDate, maturityDate, Frequency.SEMIANNUAL, capDayCountConvention, ShortPeriodConvention.FIRST, DateRollConvention.FOLLOWING, new BusinessdayCalendarExcludingWeekends(), 0, 0),
					forwardCurve.getName(),
					0.0,
					true,
					discountCurve.getName(),
					capletVolatility.getName(),
					calibrationTargetValueQuotingConvention);

			AbstractVolatilitySurfaceParametric flatSurface = new CapletVolatilitiesParametric("Caplet", null, forwardCurve, discountCurve, 0, 0, 0, volatility, 1.0);
			AnalyticModelInterface flatModel = model.clone().addVolatilitySurfaces(flatSurface);
			double valueTarget = cap.getValue(0.0, flatModel);

			calibrationProducts.add(cap);
			calibrationTargetValues.add(new Double(valueTarget));
		}


		AbstractVolatilitySurfaceParametric calibratedSurface = capletVolatility.getCloneCalibrated(model, calibrationProducts, calibrationTargetValues, null);
		double[] calibratedParameters = calibratedSurface.getParameter();

		AnalyticModelInterface modelCalibrated = model.clone();
		modelCalibrated = modelCalibrated.addVolatilitySurfaces(calibratedSurface);

		double rms = 0.0;
		for(int i=0; i<marketProducts.size(); i++) {
			double volatilityTarget = marketTargetValues.get(i);
			System.out.print(volatilityTarget);

			double value = marketProducts.get(i).getValue(0.0, modelCalibrated);
			System.out.print("\t" + value);

			AbstractVolatilitySurfaceParametric flatSurface = new CapletVolatilitiesParametric("Caplet", null, forwardCurve, discountCurve, 0, 0, 0, volatilityTarget, 1.0);
			AnalyticModelInterface flatModel = model.clone().addVolatilitySurfaces(flatSurface);
			double valueMarket = marketProducts.get(i).getValue(0.0, flatModel);
			System.out.print("\t" + valueMarket);
			System.out.println("\t" + (value-valueMarket));
			rms += (value-valueMarket)*(value-valueMarket);
		}
		rms = Math.sqrt(rms/marketProducts.size());
		System.out.println("RMS (" + calibrationTargetValueQuotingConvention.name() + "):" + rms);

		System.out.println(Arrays.toString(calibratedParameters));
		System.out.println("__________________________________________________________________________________________\n");

		/*
		 * Check results - results may vary depending on the quoting convention used for calibration
		 */
		if(calibrationTargetValueQuotingConvention ==  QuotingConvention.VOLATILITYLOGNORMAL) {
			// Check parameters
			Assert.assertArrayEquals(new double[] { -1.04, 8.35 , 2.10, -0.01 }, calibratedParameters, 0.01 /* tolerance */ );
			// Check rms (not a very strict test)
			Assert.assertTrue(rms < 0.08);
		}
		else {
			// Check parameters
			Assert.assertArrayEquals(new double[] { -1.08, 8.73 , 2.19, -0.01 }, calibratedParameters, 0.01 /* tolerance */ );
			// Check rms (not a very strict test)
			Assert.assertTrue(rms < 0.02);
		}
	}
}
