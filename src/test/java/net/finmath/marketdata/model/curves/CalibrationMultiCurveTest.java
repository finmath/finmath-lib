/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 26.11.2017
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
import java.util.function.Function;

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
import net.finmath.time.businessdaycalendar.BusinessdayCalendarInterface.DateRollConvention;

/**
 * This class makes some basic tests related to the setup, use and calibration of discount curves and forward curve.
 * 
 * @author Christian Fries
 */
public class CalibrationMultiCurveTest {

	@Test
	public void testMultiCurveCalibration() throws SolverException, CloneNotSupportedException {

		/*
		 * Calibration of a single curve - OIS curve - self disocunted curve, from a set of calibration products.
		 */

		LocalDate referenceDate = LocalDate.of(2012,01,10);

		/*
		 * Define the calibration spec generators for our calibration products
		 */
		BiFunction<String, Double, CalibrationSpec> deposit = (maturity, rate) -> {
			ScheduleInterface scheduleInterfaceRec = ScheduleGenerator.createScheduleFromConventions(referenceDate, 2, "0D", maturity, "tenor", "act/360", "first", "following", new BusinessdayCalendarExcludingTARGETHolidays(), 0, 0);
			ScheduleInterface scheduleInterfacePay = null;
			double calibrationTime = scheduleInterfaceRec.getPayment(scheduleInterfaceRec.getNumberOfPeriods()-1);
			CalibrationSpec calibrationSpec = new CalibratedCurves.CalibrationSpec("EUR-OIS-" + maturity, "Deposit", scheduleInterfaceRec, "", rate, "discount-EUR-OIS", scheduleInterfacePay, null, 0.0, null, "discount-EUR-OIS", calibrationTime);
			return calibrationSpec;
		};

		BiFunction<String, Double, CalibrationSpec> swapSingleCurve = (maturity, rate) -> {
			ScheduleInterface scheduleInterfaceRec = ScheduleGenerator.createScheduleFromConventions(referenceDate, 2, "0D", maturity, "annual", "act/360", "first", "modified_following", new BusinessdayCalendarExcludingTARGETHolidays(), 0, 1);
			ScheduleInterface scheduleInterfacePay = ScheduleGenerator.createScheduleFromConventions(referenceDate, 2, "0D", maturity, "annual", "act/360", "first", "modified_following", new BusinessdayCalendarExcludingTARGETHolidays(), 0, 1);
			double calibrationTime = scheduleInterfaceRec.getPayment(scheduleInterfaceRec.getNumberOfPeriods() - 1);
			CalibrationSpec calibrationSpec = new CalibratedCurves.CalibrationSpec("EUR-OIS-" + maturity, "Swap", scheduleInterfaceRec, "forward-EUR-OIS", 0.0, "discount-EUR-OIS", scheduleInterfacePay, "", rate, "discount-EUR-OIS", "discount-EUR-OIS", calibrationTime);
			return calibrationSpec;
		};

		Function<String,BiFunction<String, Double, CalibrationSpec>> fra = (tenor) -> {
			return (fixing, rate) -> {
				ScheduleInterface scheduleInterfaceRec = ScheduleGenerator.createScheduleFromConventions(referenceDate, 2, fixing, tenor, "tenor", "act/360", "first", "modified_following", new BusinessdayCalendarExcludingTARGETHolidays(), 0, 0);
				double calibrationTime = scheduleInterfaceRec.getFixing(scheduleInterfaceRec.getNumberOfPeriods() - 1);
				String curveName = "forward-EUR-" + tenor;
				CalibrationSpec calibrationSpec = new CalibratedCurves.CalibrationSpec("EUR-" + tenor + "-" + fixing, "FRA", scheduleInterfaceRec, curveName, rate, "discount-EUR-OIS", null, null, 0.0, null, curveName, calibrationTime);
				return calibrationSpec;
			};
		};

		Function<String,BiFunction<String, Double, CalibrationSpec>> swap = (tenor) -> {
			return (maturity, rate) -> {
				ScheduleInterface scheduleInterfaceRec = ScheduleGenerator.createScheduleFromConventions(referenceDate, 2, "0D", maturity, "quarterly", "act/360", "first", "following", new BusinessdayCalendarExcludingTARGETHolidays(), 0, 0);
				ScheduleInterface scheduleInterfacePay = ScheduleGenerator.createScheduleFromConventions(referenceDate, 2, "0D", maturity, "annual", "E30/360", "first", "following", new BusinessdayCalendarExcludingTARGETHolidays(), 0, 0);
				double calibrationTime = scheduleInterfaceRec.getFixing(scheduleInterfaceRec.getNumberOfPeriods() - 1);
				String curveName = "forward-EUR-" + tenor;
				CalibrationSpec calibrationSpec = new CalibratedCurves.CalibrationSpec("EUR-" + tenor + maturity, "Swap", scheduleInterfaceRec, curveName, 0.0, "discount-EUR-OIS", scheduleInterfacePay, "", rate, "discount-EUR-OIS", curveName, calibrationTime);
				return calibrationSpec;
			};
		};

		/*
		 * Generate empty curve template (for cloning during calibration)
		 */
		double[] times = { 0.0 };
		double[]	 discountFactors = { 1.0 };
		boolean[] isParameter = { false };

		DiscountCurve discountCurveOIS = DiscountCurve.createDiscountCurveFromDiscountFactors("discount-EUR-OIS", referenceDate, times, discountFactors, isParameter, InterpolationMethod.LINEAR, ExtrapolationMethod.CONSTANT, InterpolationEntity.LOG_OF_VALUE);
		ForwardCurveInterface forwardCurveOIS = new ForwardCurveFromDiscountCurve("forward-EUR-OIS", "discount-EUR-OIS", referenceDate, "3M");
		ForwardCurveInterface forwardCurve3M = new ForwardCurve("forward-EUR-3M", referenceDate, "3M", new BusinessdayCalendarExcludingTARGETHolidays(), DateRollConvention.FOLLOWING, Curve.InterpolationMethod.LINEAR, Curve.ExtrapolationMethod.CONSTANT, Curve.InterpolationEntity.VALUE,ForwardCurve.InterpolationEntityForward.FORWARD, "discount-EUR-OIS");

		AnalyticModel forwardCurveModel = new AnalyticModel(new CurveInterface[] { discountCurveOIS, forwardCurveOIS, forwardCurve3M });

		List<CalibrationSpec> calibrationSpecs = new LinkedList<>();

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
		 * Calibrate
		 */
		CalibratedCurves calibratedCurves = new CalibratedCurves(calibrationSpecs.toArray(new CalibrationSpec[calibrationSpecs.size()]), forwardCurveModel, 1E-12);

		/*
		 * Get the calibrated model
		 */
		AnalyticModelInterface calibratedModel = calibratedCurves.getModel();

		/*
		 * Print calibration errors
		 */
		System.out.println("\nCalibrated errors:");

		double sumOfSquaredErrors = 0.0;
		for(CalibrationSpec calibratedSpec : calibrationSpecs) {
			AnalyticProductInterface product = calibratedCurves.getCalibrationProductForSpec(calibratedSpec);
			double value = product.getValue(0.0, calibratedModel);
			sumOfSquaredErrors += value*value;
		}

		/*
		 * Print calibrated curves
		 */
		DiscountCurveInterface discountCurveCalibrated = calibratedModel.getDiscountCurve("discount-EUR-OIS");
		ForwardCurveInterface forwardCurveCalibrated = calibratedModel.getForwardCurve("forward-EUR-3M");

		System.out.println("\nCalibrated discount curve:");
		System.out.println(discountCurveCalibrated);

		System.out.println("\nCalibrated forward curve:");
		System.out.println(forwardCurveCalibrated);

		Assert.assertEquals("Calibration error", 0.0, Math.sqrt(sumOfSquaredErrors)/calibrationSpecs.size(), 1E-10);
	}	
}
