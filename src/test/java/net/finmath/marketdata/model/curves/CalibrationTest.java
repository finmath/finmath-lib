/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 28.11.2012
 */
package net.finmath.marketdata.model.curves;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.function.BiFunction;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.finmath.marketdata.calibration.CalibratedCurves;
import net.finmath.marketdata.calibration.ParameterObjectInterface;
import net.finmath.marketdata.calibration.Solver;
import net.finmath.marketdata.calibration.CalibratedCurves.CalibrationSpec;
import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.marketdata.model.curves.Curve.ExtrapolationMethod;
import net.finmath.marketdata.model.curves.Curve.InterpolationEntity;
import net.finmath.marketdata.model.curves.Curve.InterpolationMethod;
import net.finmath.marketdata.products.AnalyticProductInterface;
import net.finmath.marketdata.products.Swap;
import net.finmath.optimizer.SolverException;
import net.finmath.time.RegularSchedule;
import net.finmath.time.ScheduleGenerator;
import net.finmath.time.ScheduleInterface;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingTARGETHolidays;

/**
 * This class makes some basic tests related to the setup, use and calibration of discount curves and forward curve.
 * 
 * @author Christian Fries
 */
@RunWith(Parameterized.class)
public class CalibrationTest {

	static final double errorTolerance = 1E-14;

	final InterpolationMethod interpolationMethod;
	
	public CalibrationTest(InterpolationMethod interpolationMethod)
	{
		this.interpolationMethod = interpolationMethod;
	}
	
	/**
	 * The parameters for this test, that is an error consisting of
	 * { numberOfPaths, setup }.
	 * 
	 * @return Array of parameters.
	 */
	@Parameters(name="{0}")
	public static Collection<Object[]> generateData()
	{
		return Arrays.asList(new Object[][] {
				{ InterpolationMethod.LINEAR },
				{ InterpolationMethod.CUBIC_SPLINE },
				{ InterpolationMethod.AKIMA },
				{ InterpolationMethod.AKIMA_CONTINUOUS },
				{ InterpolationMethod.HARMONIC_SPLINE },
				{ InterpolationMethod.HARMONIC_SPLINE_WITH_MONOTONIC_FILTERING },
		});
	};

	/**
	 * Run some test using discount curves and forward curves and the solver to create a calibrated model.
	 * 
	 * @param args Arguments - not used.
	 * @throws SolverException Thrown if the solver cannot find a solution to the calibration problem.
	 */
	public static void main(String[] args) throws SolverException {
		
		CalibrationTest calibrationTest = new CalibrationTest(InterpolationMethod.LINEAR);

		calibrationTest.testForwardCurveFromDiscountCurve();
		calibrationTest.testCurvesAndCalibration();
	}

	@Test
	public void testForwardCurveFromDiscountCurve() {
		/*
		 * CREATING AND USING A DISCOUNT CURVE
		 */

		// Create a discount curve
		DiscountCurve			discountCurve					= DiscountCurve.createDiscountCurveFromDiscountFactors(
				"discountCurve"								/* name */,
				new double[] {0.0,  1.0,  2.0,  4.0,  5.0}	/* maturities */,
				new double[] {1.0, 0.95, 0.90, 0.85, 0.80}	/* discount factors */
				);

		// Create a forward curve from that discount curve for semi-annual forward rates
		ForwardCurveInterface	forwardCurveFromDiscountCurve	= new ForwardCurveFromDiscountCurve(
				discountCurve.getName()						/* name of the discount curve to use */,
				null										/* reference date: not specified since single curve setup */,
				null										/* period length: not specified since single curve setup */
				);

		// A model is a collection of curves (curves and products find other curves by looking up their name in the model)
		AnalyticModel model1 = new AnalyticModel(new CurveInterface[] { discountCurve , forwardCurveFromDiscountCurve });

		System.out.println("Given a discount curve:");
		System.out.println(discountCurve.toString());

		// We may ask the forward curve for a forward.
		double fixingTime	= 1.0;
		double periodLength = 0.5;
		double forwardRate	= forwardCurveFromDiscountCurve.getForward(model1, fixingTime, periodLength);
		System.out.println("Semi-annual forward with fixing in " + fixingTime + " calculated from that discount curve is " + forwardRate);

		// Check if we have the right value
		double forwardRateFromDiscountFactor = (discountCurve.getDiscountFactor(model1, fixingTime) / discountCurve.getDiscountFactor(model1, fixingTime + periodLength) - 1) / periodLength;
		Assert.assertTrue(Math.abs(forwardRate - forwardRateFromDiscountFactor) < errorTolerance);

		System.out.println("__________________________________________________________________________________________\n");
	}	
	
	@Test
	public void testSingeCurveCalibration() {

		/*
		 * Calibration of a single curve - OIS curve - self disocunted curve, from a set of calibration products.
		 */

		LocalDate referenceDate = LocalDate.of(2012,01,10);

		/*
		 * Define the calibration spec generators for our calibration products
		 */
		BiFunction<String, Double, CalibrationSpec> depositForMaturityAndRate = (maturity, rate) -> {
			ScheduleInterface scheduleInterfaceRec = ScheduleGenerator.createScheduleFromConventions(referenceDate, 2, "0D", maturity, "tenor", "act/360", "first", "following", new BusinessdayCalendarExcludingTARGETHolidays(), 0, 0);
			ScheduleInterface scheduleInterfacePay = null;
			double calibrationTime = scheduleInterfaceRec.getPayment(scheduleInterfaceRec.getNumberOfPeriods()-1);
			CalibrationSpec calibrationSpec = new CalibrationSpec("EUR-OIS-"+maturity, "Deposit", scheduleInterfaceRec, "", rate, "discount-EUR-OIS", scheduleInterfacePay, null, 0, null, "discount-EUR-OIS", calibrationTime);
			return calibrationSpec;
		};

		BiFunction<String, Double, CalibrationSpec> swapForMaturityAndRate = (maturity, rate) -> {
			ScheduleInterface scheduleInterfaceRec = ScheduleGenerator.createScheduleFromConventions(referenceDate, 2, "0D", maturity, "annual", "act/360", "first", "modified_following", new BusinessdayCalendarExcludingTARGETHolidays(), 0, 1);
			ScheduleInterface scheduleInterfacePay = ScheduleGenerator.createScheduleFromConventions(referenceDate, 2, "0D", maturity, "annual", "act/360", "first", "modified_following", new BusinessdayCalendarExcludingTARGETHolidays(), 0, 1);
			double calibrationTime = scheduleInterfaceRec.getPayment(scheduleInterfaceRec.getNumberOfPeriods()-1);
			CalibrationSpec calibrationSpec = new CalibrationSpec("EUR-OIS-"+maturity, "Swap", scheduleInterfaceRec, "forward-EUR-OIS", 0, "discount-EUR-OIS", scheduleInterfacePay, "", rate, "discount-EUR-OIS", "discount-EUR-OIS", calibrationTime);
			return calibrationSpec;
		};

		/*
		 * Generate empty curve template (for cloning during calibration)
		 */
		double[] times = { 0.0 };
		double[]	 discountFactors = { 1.0 };
		boolean[] isParameter = { false };

		DiscountCurve discountCurve = DiscountCurve.createDiscountCurveFromDiscountFactors("discount-EUR-OIS", referenceDate, times, discountFactors, isParameter, InterpolationMethod.LINEAR, ExtrapolationMethod.CONSTANT, InterpolationEntity.LOG_OF_VALUE);
		ForwardCurveFromDiscountCurve forwardCurveFromDiscountCurve = new ForwardCurveFromDiscountCurve("forward-EUR-OIS", "discount-EUR-OIS", referenceDate, "3M");


		CurveInterface[] curveInterfacesSD = {discountCurve, forwardCurveFromDiscountCurve};
		AnalyticModel forwardCurveModel = new AnalyticModel(curveInterfacesSD);

		List<CalibrationSpec> calibrationSpecs = new LinkedList<>();

		calibrationSpecs.add(depositForMaturityAndRate.apply("1D", 0.00202));
		calibrationSpecs.add(depositForMaturityAndRate.apply("1M", 0.00191));
		calibrationSpecs.add(depositForMaturityAndRate.apply("12M", 0.00129));

		calibrationSpecs.add(swapForMaturityAndRate.apply("18M", 0.00108));
		calibrationSpecs.add(swapForMaturityAndRate.apply("21M", 0.00101));
		calibrationSpecs.add(swapForMaturityAndRate.apply("2Y", 0.00101));
		calibrationSpecs.add(swapForMaturityAndRate.apply("3Y", 0.00194));
		calibrationSpecs.add(swapForMaturityAndRate.apply("4Y", 0.00346));
		calibrationSpecs.add(swapForMaturityAndRate.apply("5Y", 0.00534));

		CalibrationSpec[] calibratedSpecsArray = calibrationSpecs.toArray(new CalibrationSpec[calibrationSpecs.size()]);
		CalibratedCurves calibratedCurves = null;
		try {
			calibratedCurves = new CalibratedCurves(calibratedSpecsArray, forwardCurveModel, 0.00000001);
		} catch (SolverException | CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		double sumOfSquaredErrors = 0.0;
		for(CalibrationSpec calibratedSpec : calibrationSpecs) {
			AnalyticProductInterface product = calibratedCurves.getCalibrationProductForSpec(calibratedSpec);
			double value = product.getValue(0.0, calibratedCurves.getModel());
			sumOfSquaredErrors += value*value;
		}

		Assert.assertEquals("Calibration error", 0.0, Math.sqrt(sumOfSquaredErrors)/calibrationSpecs.size(), 1E-10);

		DiscountCurveInterface discountCurveCalibrated = (DiscountCurveInterface) calibratedCurves.getCurve("discount-EUR-OIS");
		System.out.println(discountCurveCalibrated);
	}

	@Test
	public void testCurvesAndCalibration() throws SolverException {

		/*
		 * CALIBRATING A CURVE - SINGLE CURVE SETUP
		 * 
		 * Note: Only maturity > 0 (DiscountCurve) and fixing > 0 (ForwardCurve) are calibration parameters (!)
		 */
		
		System.out.println("Calibrating a discount curve from swaps (single-curve/self-discounting).");

		// Create a discount curve
		DiscountCurve			discountCurve					= DiscountCurve.createDiscountCurveFromDiscountFactors(
				"discountCurve"								/* name */,
				new double[] {0.0,  1.0,  2.0,  4.0,  5.0}	/* maturities */,
				new double[] {1.0, 0.95, 0.90, 0.85, 0.80}	/* discount factors */,
				interpolationMethod,
				ExtrapolationMethod.CONSTANT,
				InterpolationEntity.LOG_OF_VALUE
				);

		// Create a forward curve from that discount curve for forward rates
		ForwardCurveInterface	forwardCurveFromDiscountCurve	= new ForwardCurveFromDiscountCurve(
				discountCurve.getName()						/* name of the discount curve to use */,
				null										/* reference date: not specified since single curve setup */,
				null										/* period length: not specified since single curve setup */
				);

		// Create a collection of objective functions (calibration products)
		Vector<AnalyticProductInterface> calibrationProducts1 = new Vector<AnalyticProductInterface>();
		
		calibrationProducts1.add(new Swap(new RegularSchedule(new TimeDiscretization(0.0, 1, 1.0)), null, 0.05, "discountCurve", new RegularSchedule(new TimeDiscretization(0.0, 1, 1.0)), forwardCurveFromDiscountCurve.getName(), 0.0, "discountCurve"));
		calibrationProducts1.add(new Swap(new RegularSchedule(new TimeDiscretization(0.0, 2, 1.0)), null, 0.04, "discountCurve", new RegularSchedule(new TimeDiscretization(0.0, 2, 1.0)), forwardCurveFromDiscountCurve.getName(), 0.0, "discountCurve"));
		calibrationProducts1.add(new Swap(new RegularSchedule(new TimeDiscretization(0.0, 8, 0.5)), null, 0.03, "discountCurve", new RegularSchedule(new TimeDiscretization(0.0, 8, 0.5)), forwardCurveFromDiscountCurve.getName(), 0.0, "discountCurve"));
		calibrationProducts1.add(new Swap(new RegularSchedule(new TimeDiscretization(0.0, 10, 0.5)), null, 0.04, "discountCurve", new RegularSchedule(new TimeDiscretization(0.0, 10, 0.5)), forwardCurveFromDiscountCurve.getName(), 0.0, "discountCurve"));

		// A model is a collection of curves (curves and products find other curves by looking up their name in the model)
		AnalyticModel model1 = new AnalyticModel(new CurveInterface[] { discountCurve , forwardCurveFromDiscountCurve });

		// Create a collection of curves to calibrate
		Set<ParameterObjectInterface> curvesToCalibrate1 = new HashSet<ParameterObjectInterface>();
		curvesToCalibrate1.add(discountCurve);

		// Calibrate the curve
		Solver solver1 = new Solver(model1, calibrationProducts1);
		AnalyticModelInterface calibratedModel1 = solver1.getCalibratedModel(curvesToCalibrate1);
		System.out.println("The solver required " + solver1.getIterations() + " iterations.");
		System.out.println("The best fit curve is:");
		System.out.println(calibratedModel1.getCurve(discountCurve.getName()).toString());

		/*
		 * The model calibratedModel1 now contains a set af calibrated curves.
		 * The curves are clones. The model model1 still contains the original curve.
		 */

		// Calibration check
		System.out.println("Calibration check:");
		double evaluationTime = 0.0;
		double error = 0;
		for(int calibrationProductIndex = 0; calibrationProductIndex < calibrationProducts1.size(); calibrationProductIndex++) {
			AnalyticProductInterface	calibrationProduct		= calibrationProducts1.get(calibrationProductIndex);
			double						calibrationProductValue	= calibrationProduct.getValue(evaluationTime, calibratedModel1);
			System.out.println("Calibration product " + calibrationProductIndex + ":\t" + calibrationProductValue);

			error += calibrationProductValue*calibrationProductValue;
		}
		error = Math.sqrt(error);
		Assert.assertTrue(error < errorTolerance);

		System.out.println("__________________________________________________________________________________________\n");

		
		/*
		 * CALIBRATE A FORWARD CURVE, USING THE GIVEN DISCOUNT CURVE (MULTI-CURVE SETUP)
		 * 
		 * Note: Only maturity > 0 (DiscountCurve) and fixing > 0 (ForwardCurve) are calibration parameters (!)
		 */
		
		// Create initial guess for the curve
		ForwardCurve forwardCurve = ForwardCurve.createForwardCurveFromForwards("forwardCurve", new double[] {2.0/365.0, 1.0, 2.0, 3.0, 4.0}, new double[] {0.05, 0.05, 0.05, 0.05, 0.05}, model1, discountCurve.getName(), 0.5);

		// Make collection of all curves used in valuation
		AnalyticModel model2 = new AnalyticModel( new CurveInterface[] { discountCurve, forwardCurve } );

		System.out.println("Calibrating a forward curve from swaps using the given discount curve.");

		// Create a collection of objective functions (calibration products)
		Vector<AnalyticProductInterface> calibrationProducts2 = new Vector<AnalyticProductInterface>();
			
		// It is possible to mix tenors (although it may not be meaningful in a forward curve calibration)
		calibrationProducts2.add(new Swap(new RegularSchedule(new TimeDiscretization(0.0, 1, 1.0)), null, 0.06, "discountCurve", new RegularSchedule(new TimeDiscretization(0.0, 1, 0.5)), "forwardCurve", 0.0, "discountCurve"));
		calibrationProducts2.add(new Swap(new RegularSchedule(new TimeDiscretization(0.0, 2, 1.0)), null, 0.05, "discountCurve", new RegularSchedule(new TimeDiscretization(0.0, 2, 0.5)), "forwardCurve", 0.0, "discountCurve"));
		calibrationProducts2.add(new Swap(new RegularSchedule(new TimeDiscretization(0.0, 6, 0.5)), null, 0.04, "discountCurve", new RegularSchedule(new TimeDiscretization(0.0, 6, 0.5)), "forwardCurve", 0.0, "discountCurve"));
		calibrationProducts2.add(new Swap(new RegularSchedule(new TimeDiscretization(0.0, 8, 0.5)), null, 0.04, "discountCurve", new RegularSchedule(new TimeDiscretization(0.0, 8, 0.5)), "forwardCurve", 0.0, "discountCurve"));
		calibrationProducts2.add(new Swap(new RegularSchedule(new TimeDiscretization(0.0, 10, 0.5)), null, 0.04, "discountCurve", new RegularSchedule(new TimeDiscretization(0.0, 10, 0.5)), "forwardCurve", 0.0, "discountCurve"));

		// Create a collection of curves to calibrate
		Set<ParameterObjectInterface> curvesToCalibrate2 = new HashSet<ParameterObjectInterface>();
		curvesToCalibrate2.add(forwardCurve);

		// Calibrate the curve
		Solver solver2 = new Solver(model2, calibrationProducts2);
		AnalyticModelInterface calibratedModel2 = solver2.getCalibratedModel(curvesToCalibrate2);
		System.out.println("The solver required " + solver2.getIterations() + " iterations.");
		System.out.println("The best fit curve is:");
		System.out.println(calibratedModel2.getCurve(forwardCurve.getName()).toString());

		/*
		 * The model calibratedModel2 now contains a set of calibrated curves.
		 * The curves are clones. The model model2 still contains the original curve.
		 */
	
		// Calibration check
		System.out.println("Calibration check:");
		double error2 = 0;
		for(int calibrationProductIndex = 0; calibrationProductIndex < calibrationProducts2.size(); calibrationProductIndex++) {
			AnalyticProductInterface	calibrationProduct		= calibrationProducts2.get(calibrationProductIndex);
			double						calibrationProductValue	= calibrationProduct.getValue(evaluationTime, calibratedModel2);
			System.out.println("Calibration product " + calibrationProductIndex + ":\t" + calibrationProductValue);

			error2 += calibrationProductValue*calibrationProductValue;
		}
		error2 = Math.sqrt(error2);
		Assert.assertTrue(error2 < errorTolerance);

		System.out.println("__________________________________________________________________________________________\n");
	}		
}
