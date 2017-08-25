package net.finmath.marketdata.model.curves;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.junit.Assert;
import org.junit.Test;
import org.threeten.bp.LocalDate;
import org.threeten.bp.Month;

import net.finmath.marketdata.calibration.ParameterObjectInterface;
import net.finmath.marketdata.calibration.Solver;
import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.marketdata.products.AnalyticProductInterface;
import net.finmath.marketdata.products.Swap;
import net.finmath.optimizer.SolverException;
import net.finmath.time.ScheduleGenerator;
import net.finmath.time.ScheduleInterface;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingTARGETHolidays;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarInterface.DateRollConvention;

public class NelsonSiegelSvenssonCalibrationTest {

	/**
	 * Calibrate a Nelson-Siegel-Svensson curve to a given set of swap rates of
	 * (self-discounted) swaps.
	 * 
	 * @param parameters Key-Value-Map of parameters.
	 * @return The best fitting NSS parameters.
	 * @throws SolverException Thrown is the solver encountered a problem.
	 */
	public double[] calibrateNSSCurve(Map<String, Object> parameters) throws SolverException {

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

		int		spotOffsetDays = 2;
		String	forwardStartPeriod = "0D";

		double[] initialParameters	= new double[] { 0.025, -0.015, -0.025, 0.03, 1.5, 10 };
		
		DiscountCurveInterface discountCurve	= new DiscountCurveNelsonSiegelSvensson("discountCurve-" + currency, referenceDate, initialParameters, 1.0);
		
		/*
		 * We create a forward curve by referencing the same discount curve, since
		 * this is a single curve setup.
		 * 
		 * Note that using an independent NSS forward curve with its own NSS parameters
		 * would result in a problem where both, the forward curve and the discount curve
		 * have free parameters.
		 */
		ForwardCurveInterface forwardCurve		= new ForwardCurveFromDiscountCurve(discountCurve.getName(), referenceDate, forwardCurveTenor);
		
		/*
		 * Model consists of the two curves, but only one of them provides free parameters.
		 */
		AnalyticModelInterface model = new AnalyticModel(new CurveInterface[] { discountCurve, forwardCurve });

		// Create a collection of objective functions (calibration products)
		Vector<AnalyticProductInterface> calibrationProducts = new Vector<AnalyticProductInterface>();
		for(int i=0; i<rates.length; i++) {
			
			ScheduleInterface schedulePay = ScheduleGenerator.createScheduleFromConventions(referenceDate, spotOffsetDays, forwardStartPeriod, maturities[i], frequency[i], daycountConventions[i], "first", "following", new BusinessdayCalendarExcludingTARGETHolidays(), -2, 0);
			ScheduleInterface scheduleRec = ScheduleGenerator.createScheduleFromConventions(referenceDate, spotOffsetDays, forwardStartPeriod, maturities[i], frequencyFloat[i], daycountConventionsFloat[i], "first", "following", new BusinessdayCalendarExcludingTARGETHolidays(), -2, 0);
			
			calibrationProducts.add(new Swap(schedulePay, null, rates[i], discountCurve.getName(), scheduleRec, forwardCurve.getName(), 0.0, discountCurve.getName()));
		}

		/*
		 * Create a collection of curves to calibrate
		 */
		Set<ParameterObjectInterface> curvesToCalibrate = new HashSet<ParameterObjectInterface>();
		curvesToCalibrate.add(discountCurve);

		/*
		 * Calibrate the curve
		 */
		Solver solver = new Solver(model, calibrationProducts);
		AnalyticModelInterface calibratedModel = solver.getCalibratedModel(curvesToCalibrate);
		System.out.println("Solver reported acccurary....: " + solver.getAccuracy());

		Assert.assertEquals("Calibration accurarcy", 0.0, solver.getAccuracy(), 1E-3);

		// Get best parameters
		double[] parametersBest = calibratedModel.getDiscountCurve(discountCurve.getName()).getParameter();

		// Test calibration
		discountCurve	= new DiscountCurveNelsonSiegelSvensson(discountCurve.getName(), referenceDate, parametersBest, 1.0);
		forwardCurve	= new ForwardCurveFromDiscountCurve(forwardCurve.getName(), discountCurve.getName(), referenceDate, "3M", new BusinessdayCalendarExcludingTARGETHolidays(), DateRollConvention.MODIFIED_FOLLOWING, 365.0/365.0, 0.0);
		model			= new AnalyticModel(new CurveInterface[] { discountCurve, forwardCurve });
		
		double squaredErrorSum = 0.0;
		for(AnalyticProductInterface c : calibrationProducts) {
			double value = c.getValue(0.0, model);
			double valueTaget = 0.0;
			double error = value - valueTaget;
			squaredErrorSum += error*error;
		}
		double rms = Math.sqrt(squaredErrorSum/calibrationProducts.size());
		
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
		HashMap<String, Object> parameters = new HashMap<String, Object>();

		parameters.put("referenceDate", LocalDate.of(2014, Month.AUGUST, 12)); 
		parameters.put("currency", "EUR");
		parameters.put("forwardCurveTenor", "3M");
		parameters.put("maturities", maturity);
		parameters.put("fixLegFrequencies", frequency);
		parameters.put("floatLegFrequencies", frequencyFloat);
		parameters.put("fixLegDaycountConventions", daycountConventions);
		parameters.put("floatLegDaycountConventions", daycountConventionsFloat);
		parameters.put("rates", rates);

		double[] nssParameters = calibrateNSSCurve(parameters);

		System.out.println(Arrays.toString(nssParameters));
		System.out.println();
	}
}
