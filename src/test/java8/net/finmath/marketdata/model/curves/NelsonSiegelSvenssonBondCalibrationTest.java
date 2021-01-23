package net.finmath.marketdata.model.curves;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import net.finmath.marketdata.calibration.ParameterObject;
import net.finmath.marketdata.calibration.Solver;
import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.AnalyticModelFromCurvesAndVols;
import net.finmath.marketdata.model.bond.Bond;
import net.finmath.marketdata.products.AnalyticProduct;
import net.finmath.optimizer.SolverException;
import net.finmath.time.Schedule;
import net.finmath.time.ScheduleGenerator;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingTARGETHolidays;

public class NelsonSiegelSvenssonBondCalibrationTest {

	public static final String DISCOUNT_CURVE_NAME = "discountCurve-EUR";
	public static final String ACT_365 = "ACT/365";
	public static final String ANNUAL = "annual";
	public static final LocalDate REFERENCE_DAY = LocalDate.of(2020, 11, 1);

	/*
	 * Test the calibration of a Nelson-Siegel-Svensson curve to a given set of bond.
	 * Input values for the model are:
	 *  - set of bonds' maturity,
	 *  - coupon rates,
	 *  - current prices
	 * Then we calibrate the curve using current prices. The aim is to calibrate model in a way that interpolated yields are close enough to actual yields
	 * @throws SolverException Thrown if the solver cannot find a solution.
	 */
	@Test
	public void testBondNSSCurveCalibration() throws SolverException {
		// maturities in years.
		final double[] maturity = new double[]{ 0.1, 0.3, 1.0, 2.0, 3.0, 4.0, 5.0, 7.0, 10.0, 20.0, 30.0 };
		// coupon rates of bonds, 1.5% for all of them
		final double[] rates = new double[]{ 0.015, 0.015, 0.015, 0.015, 0.015, 0.015, 0.015, 0.015, 0.015, 0.015, 0.015 };
		// current prices of bonds, current price for face value of 1.0. (i.e. 0.99 means that the bond is sold with a discount)
		final double[] prices = new double[]{ 0.99, 0.998, 1.010, 1.015, 1.020, 1.028, 1.030, 1.038, 1.039, 1.040, 1.042 };
		// yield to maturity
		final double[] actualYields = new double[maturity.length];
		for(int i = 0; i < maturity.length; i++) {
			actualYields[i] = getYieldToMaturity(prices[i], 1.0, rates[i], maturity[i]);
		}

		final HashMap<String, Object> parameters = new HashMap<>();

		parameters.put("maturities", maturity);
		parameters.put("rates", rates);
		parameters.put("prices", prices);

		final double[] nssParameters = calibrateNSSCurve(parameters);

		final DiscountCurve discountCurve = new DiscountCurveNelsonSiegelSvensson("EUR CurveFromInterpolationPoints", LocalDate.now(), nssParameters, 1.0);
		System.out.println("Calibrated parameters: " + Arrays.toString(nssParameters));
		for(int i = 0; i < maturity.length; i++) {
			final double discountFactor = discountCurve.getDiscountFactor(maturity[i]);
			final double interpolatedYield = -1 * Math.log(discountFactor) / maturity[i];

			System.out.println(
					String.format("%-10s%f", "actual yield: ", actualYields[i]) +
					"\t" +
					String.format("%-10s%f", "interpolated yield: ", interpolatedYield));
		}
	}

	/**
	 * Calibrate a Nelson-Siegel-Svensson curve to a given set of bonds.
	 *
	 * @param parameters Key-Value-Map of parameters.
	 * @return The best fitting NSS parameters.
	 * @throws SolverException Thrown is the solver encountered a problem.
	 */
	private double[] calibrateNSSCurve(final Map<String, Object> parameters) throws SolverException {

		final double[] maturities = (double[]) parameters.get("maturities");
		final double[] rates = (double[]) parameters.get("rates");
		final double[] prices = (double[]) parameters.get("prices");

		final int spotOffsetDays = 2;
		final String forwardStartPeriod = "0D";

		final double[] initialParameters = new double[]{0.025, -0.015, -0.025, 0.03, 1.5, 10};

		final DiscountCurve discountCurve = new DiscountCurveNelsonSiegelSvensson(DISCOUNT_CURVE_NAME, REFERENCE_DAY, initialParameters, 1.0);

		final AnalyticModel model = new AnalyticModelFromCurvesAndVols(new Curve[]{discountCurve});

		// Create a collection of objective functions (calibration products)
		final Vector<AnalyticProduct> calibrationProducts = new Vector<>();

		for(int i = 0; i < rates.length; i++) {
			final BusinessdayCalendarExcludingTARGETHolidays businessdayCalendar = new BusinessdayCalendarExcludingTARGETHolidays();

			final LocalDate spotDate = businessdayCalendar.getRolledDate(REFERENCE_DAY, spotOffsetDays);
			final LocalDate startDate = businessdayCalendar.getDateFromDateAndOffsetCode(spotDate, forwardStartPeriod);
			final LocalDate maturityDate = LocalDate.now().plus(Math.round(365 * maturities[i]), ChronoUnit.DAYS);

			final Schedule schedulePay = ScheduleGenerator.createScheduleFromConventions(REFERENCE_DAY, startDate, maturityDate, ANNUAL, ACT_365, "first", "following", businessdayCalendar, -2, 0);

			final Bond bond = new Bond(schedulePay, DISCOUNT_CURVE_NAME, rates[i]);
			calibrationProducts.add(bond);

		}

		//Create a collection of curves to calibrate
		final Set<ParameterObject> curvesToCalibrate = new HashSet<>();
		curvesToCalibrate.add(discountCurve);

		// Calibrate the curve
		final Solver solver = new Solver(model, calibrationProducts, Arrays.stream(prices).boxed().collect(Collectors.toList()), null, 0, 0);
		final AnalyticModel calibratedModel = solver.getCalibratedModel(curvesToCalibrate);

		/*
		 * Check valuation errors
		 */
		System.out.println("Calibration errors: ");
		System.out.println(String.format("%8s\t%8s\t%8s\t", "model", "target", "error"));

		for(int i = 0; i < calibrationProducts.size(); i++) {
			final double valueModel = calibrationProducts.get(i).getValue(0.0, calibratedModel);
			final double valueTarget = prices[i];
			final double error = valueModel-valueTarget;
			System.out.println(String.format("%8.5f\t%8.5f\t%8.5f\t", valueModel, valueTarget, error));

			Assertions.assertEquals(valueTarget, valueModel, 0.01, "bond value");
		}
		// Get best parameters
		return calibratedModel.getDiscountCurve(discountCurve.getName()).getParameter();
	}

	/**
	 * Simple calculation of the yield to maturity. Coupon payments are not reinvested.
	 *
	 * @param pv            - present value of the bond
	 * @param fv            - face value of the bond
	 * @param couponRate    - annual coupon rate
	 * @param maturityYears - years to maturity
	 * @return yield to maturity
	 */
	public static double getYieldToMaturity(double pv, double fv, double couponRate, double maturityYears) {
		return ((fv - pv) / maturityYears + fv * couponRate) / pv;
	}
}
