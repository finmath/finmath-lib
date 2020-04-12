package net.finmath.marketdata.model.curves;

import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.marketdata.calibration.ParameterObject;
import net.finmath.marketdata.calibration.Solver;
import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.AnalyticModelFromCurvesAndVols;
import net.finmath.marketdata.products.AnalyticProduct;
import net.finmath.marketdata.products.Swap;
import net.finmath.optimizer.SolverException;
import net.finmath.time.Schedule;
import net.finmath.time.ScheduleGenerator;
import net.finmath.time.businessdaycalendar.BusinessdayCalendar.DateRollConvention;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingTARGETHolidays;

public class NelsonSiegelSvenssonCalibrationTest {

	/**
	 * Calibrate a Nelson-Siegel-Svensson curve to a given set of swap rates of
	 * (self-discounted) swaps.
	 *
	 * @param parameters Key-Value-Map of parameters.
	 * @return The best fitting NSS parameters.
	 * @throws SolverException Thrown is the solver encountered a problem.
	 */
	public double[] calibrateNSSCurve(final Map<String, Object> parameters) throws SolverException {

		final LocalDate	referenceDate		= (LocalDate) parameters.get("referenceDate");
		final String	currency			= (String) parameters.get("currency");
		final String	forwardCurveTenor	= (String) parameters.get("forwardCurveTenor");
		final String[]	maturities			= (String[]) parameters.get("maturities");
		final String[]	frequency			= (String[]) parameters.get("fixLegFrequencies");
		final String[]	frequencyFloat		= (String[]) parameters.get("floatLegFrequencies");
		final String[]	daycountConventions	= (String[]) parameters.get("fixLegDaycountConventions");
		final String[]	daycountConventionsFloat	= (String[]) parameters.get("floatLegDaycountConventions");
		final double[]	rates						= (double[]) parameters.get("rates");

		Assert.assertEquals(maturities.length, frequency.length);
		Assert.assertEquals(maturities.length, daycountConventions.length);
		Assert.assertEquals(maturities.length, rates.length);

		Assert.assertEquals(frequency.length, frequencyFloat.length);
		Assert.assertEquals(daycountConventions.length, daycountConventionsFloat.length);

		final int		spotOffsetDays = 2;
		final String	forwardStartPeriod = "0D";

		final double[] initialParameters	= new double[] { 0.025, -0.015, -0.025, 0.03, 1.5, 10 };

		DiscountCurve discountCurve	= new DiscountCurveNelsonSiegelSvensson("discountCurve-" + currency, referenceDate, initialParameters, 1.0);

		/*
		 * We create a forward curve by referencing the same discount curve, since
		 * this is a single curve setup.
		 *
		 * Note that using an independent NSS forward curve with its own NSS parameters
		 * would result in a problem where both, the forward curve and the discount curve
		 * have free parameters.
		 */
		ForwardCurve forwardCurve		= new ForwardCurveFromDiscountCurve(discountCurve.getName(), referenceDate, forwardCurveTenor);

		/*
		 * Model consists of the two curves, but only one of them provides free parameters.
		 */
		AnalyticModel model = new AnalyticModelFromCurvesAndVols(new Curve[] { discountCurve, forwardCurve });

		// Create a collection of objective functions (calibration products)
		final Vector<AnalyticProduct> calibrationProducts = new Vector<>();
		for(int i=0; i<rates.length; i++) {

			final Schedule schedulePay = ScheduleGenerator.createScheduleFromConventions(referenceDate, spotOffsetDays, forwardStartPeriod, maturities[i], frequency[i], daycountConventions[i], "first", "following", new BusinessdayCalendarExcludingTARGETHolidays(), -2, 0);
			final Schedule scheduleRec = ScheduleGenerator.createScheduleFromConventions(referenceDate, spotOffsetDays, forwardStartPeriod, maturities[i], frequencyFloat[i], daycountConventionsFloat[i], "first", "following", new BusinessdayCalendarExcludingTARGETHolidays(), -2, 0);

			calibrationProducts.add(new Swap(schedulePay, null, rates[i], discountCurve.getName(), scheduleRec, forwardCurve.getName(), 0.0, discountCurve.getName()));
		}

		/*
		 * Create a collection of curves to calibrate
		 */
		final Set<ParameterObject> curvesToCalibrate = new HashSet<>();
		curvesToCalibrate.add(discountCurve);

		/*
		 * Calibrate the curve
		 */
		final Solver solver = new Solver(model, calibrationProducts);
		final AnalyticModel calibratedModel = solver.getCalibratedModel(curvesToCalibrate);
		System.out.println("Solver reported acccurary....: " + solver.getAccuracy());

		Assert.assertEquals("Calibration accurarcy", 0.0, solver.getAccuracy(), 1E-3);

		// Get best parameters
		final double[] parametersBest = calibratedModel.getDiscountCurve(discountCurve.getName()).getParameter();

		// Test calibration
		discountCurve	= new DiscountCurveNelsonSiegelSvensson(discountCurve.getName(), referenceDate, parametersBest, 1.0);
		forwardCurve	= new ForwardCurveFromDiscountCurve(forwardCurve.getName(), discountCurve.getName(), referenceDate, "3M", new BusinessdayCalendarExcludingTARGETHolidays(), DateRollConvention.MODIFIED_FOLLOWING, 365.0/365.0, 0.0);
		model			= new AnalyticModelFromCurvesAndVols(new Curve[] { discountCurve, forwardCurve });

		double squaredErrorSum = 0.0;
		for(final AnalyticProduct c : calibrationProducts) {
			final double value = c.getValue(0.0, model);
			final double valueTaget = 0.0;
			final double error = value - valueTaget;
			squaredErrorSum += error*error;
		}
		final double rms = Math.sqrt(squaredErrorSum/calibrationProducts.size());

		System.out.println("Independent checked acccurary: " + rms);

		return parametersBest;
	}

	/**
	 * Test the calibration of a Nelson-Siegel-Svensson curve to a given set of swap rates of
	 * (self-discounted) swaps.
	 *
	 * @throws SolverException Thrown if the solver cannot find a solution.
	 */
	@Test
	public void testCalibration() throws SolverException {

		final String[] maturity					= { "3M", "6M", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "11Y", "12Y", "15Y", "20Y", "25Y", "30Y", "35Y", "40Y", "50Y" };
		final String[] frequency				= { "annual", "annual", "annual", "annual", "annual", "annual", "annual", "annual", "annual", "annual", "annual", "annual", "annual", "annual", "annual", "annual", "annual", "annual", "annual", "annual", "annual" };
		final String[] frequencyFloat			= { "quarterly", "quarterly", "quarterly", "quarterly", "quarterly", "quarterly", "quarterly", "quarterly", "quarterly", "quarterly", "quarterly", "quarterly", "quarterly", "quarterly", "quarterly", "quarterly", "quarterly", "quarterly", "quarterly", "quarterly", "quarterly" };
		final String[] daycountConventions		= { "ACT/360", "E30/360", "E30/360", "E30/360", "E30/360", "E30/360", "E30/360", "E30/360", "E30/360", "E30/360", "E30/360", "E30/360", "E30/360", "E30/360", "E30/360", "E30/360", "E30/360", "E30/360", "E30/360", "E30/360", "E30/360" };
		final String[] daycountConventionsFloat	= { "ACT/360", "ACT/360", "ACT/360", "ACT/360", "ACT/360", "ACT/360", "ACT/360", "ACT/360", "ACT/360", "ACT/360", "ACT/360", "ACT/360", "ACT/360", "ACT/360", "ACT/360", "ACT/360", "ACT/360", "ACT/360", "ACT/360", "ACT/360", "ACT/360" };
		final double[] rates					= {0.0042, 0.0032, 0.0038, 0.0052, 0.0069, 0.00855, 0.0102, 0.0119, 0.0134, 0.0150, 0.0165, 0.0178, 0.0189, 0.0200, 0.0224, 0.0250, 0.0264, 0.0271, 0.0275, 0.0276, 0.0276 };
		final HashMap<String, Object> parameters = new HashMap<>();

		parameters.put("referenceDate", LocalDate.of(2014, Month.AUGUST, 12));
		parameters.put("currency", "EUR");
		parameters.put("forwardCurveTenor", "3M");
		parameters.put("maturities", maturity);
		parameters.put("fixLegFrequencies", frequency);
		parameters.put("floatLegFrequencies", frequencyFloat);
		parameters.put("fixLegDaycountConventions", daycountConventions);
		parameters.put("floatLegDaycountConventions", daycountConventionsFloat);
		parameters.put("rates", rates);

		final double[] nssParameters = calibrateNSSCurve(parameters);

		System.out.println(Arrays.toString(nssParameters));
		System.out.println();
	}
}
