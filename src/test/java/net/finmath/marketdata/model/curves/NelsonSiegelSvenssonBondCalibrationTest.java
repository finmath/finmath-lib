package net.finmath.marketdata.model.curves;

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
import org.junit.Test;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

//TODO
//it is not clear why interpolated yield is so different from the actual yield. For exmaple for the first bond in the list (the one with maturity 0.1 years)
//actual yield: 11.616161616161616, interpolated yield : -15.188921707979922, exampleInterpolatedYield: 7.187521869740281

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
		//maturities in years.
		double[] maturity = new double[]{0.1, 0.3, 1.0, 2.0, 3.0, 4.0, 5.0, 7.0, 10.0, 20.0, 30.0};
		//coupon rates of bonds, 1.5% for all of them
		double[] rates = new double[]{0.015, 0.015, 0.015, 0.015, 0.015, 0.015, 0.015, 0.015, 0.015, 0.015, 0.015};
		//current prices of bonds, current price for face value of 100. (i.e. 99 means that the bond is sold with a discount)
		double[] prices = new double[]{99.0, 99.8, 101.0, 101.5, 102.0, 102.8, 103.0, 103.8, 103.9, 104.0, 104.2};
		//yield to maturity
		double[] actualYields = new double[maturity.length];
		for(int i = 0; i < maturity.length; i++) {
			actualYields[i] = getYieldToMaturity(prices[i], 100, rates[i], maturity[i]);
		}
		//example parameters of NSS curve (pre-calculated using some other calibration method)
		double[] nssExample = new double[]{-2.309007062964981, 9.806543347881817, 2.785897664184778, -1.1102357677666546, 1.1336182819018041, 9.490989944468486};

		final HashMap<String, Object> parameters = new HashMap<>();

		parameters.put("maturities", maturity);
		parameters.put("rates", rates);
		parameters.put("prices", prices);

		double[] nssParameters = calibrateNSSCurve(parameters);

		final DiscountCurve discountCurve = new DiscountCurveNelsonSiegelSvensson("EUR CurveFromInterpolationPoints", LocalDate.now(), nssParameters, 1.0);
		final DiscountCurve exampleDiscountCurve = new DiscountCurveNelsonSiegelSvensson("EUR CurveFromInterpolationPoints", LocalDate.now(), nssExample, 1.0);
		System.out.println("Calibrated parameters: " + Arrays.toString(nssParameters));
		System.out.println("Example parameters: " + Arrays.toString(nssExample));
		for(int i = 0; i < maturity.length; i++) {
			final double discountFactor = discountCurve.getDiscountFactor(maturity[i]);
			final double exampleDiscountFactor = exampleDiscountCurve.getDiscountFactor(maturity[i]);
			double interpolatedYield = -1 * Math.log(discountFactor) / maturity[i];
			double exampleInterpolatedYield = -1 * Math.log(exampleDiscountFactor) / maturity[i];
			System.out.println(
					"  actual yield: " + actualYields[i]
							+ ", interpolated yield : " + interpolatedYield
							+ ", exampleInterpolatedYield: " + exampleInterpolatedYield);
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

		DiscountCurve discountCurve = new DiscountCurveNelsonSiegelSvensson(DISCOUNT_CURVE_NAME, REFERENCE_DAY, initialParameters, 1.0);


		AnalyticModel model = new AnalyticModelFromCurvesAndVols(new Curve[]{discountCurve});

		// Create a collection of objective functions (calibration products)
		final Vector<AnalyticProduct> calibrationProducts = new Vector<>();

		for(int i = 0; i < rates.length; i++) {
			final BusinessdayCalendarExcludingTARGETHolidays businessdayCalendar = new BusinessdayCalendarExcludingTARGETHolidays();

			LocalDate spotDate = businessdayCalendar.getRolledDate(REFERENCE_DAY, spotOffsetDays);
			LocalDate startDate = businessdayCalendar.getDateFromDateAndOffsetCode(spotDate, forwardStartPeriod);
			LocalDate maturityDate = LocalDate.now().plus(Math.round(365 * maturities[i]), ChronoUnit.DAYS);

			final Schedule schedulePay = ScheduleGenerator.createScheduleFromConventions(REFERENCE_DAY, startDate, maturityDate, ANNUAL, ACT_365, "first", "following", businessdayCalendar, -2, 0);

			final Bond bond = new Bond(schedulePay, DISCOUNT_CURVE_NAME, null, null, rates[i]);
			calibrationProducts.add(bond);

		}

		//Create a collection of curves to calibrate
		final Set<ParameterObject> curvesToCalibrate = new HashSet<>();
		curvesToCalibrate.add(discountCurve);

		// Calibrate the curve
		final Solver solver = new Solver(model, calibrationProducts, Arrays.stream(prices).map(x -> x/100).boxed().collect(Collectors.toList()), null, 0, 0);
		final AnalyticModel calibratedModel = solver.getCalibratedModel(curvesToCalibrate);

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
		return 1.0 * ((fv - pv) / maturityYears + fv * couponRate) / pv;
	}
}
