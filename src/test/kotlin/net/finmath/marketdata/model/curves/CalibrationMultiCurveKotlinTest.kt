/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 27.11.2017
 */

package net.finmath.marketdata.model.curves

import net.finmath.marketdata.calibration.CalibratedCurves
import net.finmath.marketdata.model.AnalyticModel
import net.finmath.optimizer.SolverException
import net.finmath.time.ScheduleGenerator
import net.finmath.time.ScheduleInterface
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingTARGETHolidays
import net.finmath.time.businessdaycalendar.BusinessdayCalendarInterface
import org.junit.Assert
import org.junit.Test
import java.time.LocalDate
import java.util.*

class CalibrationMultiCurveKotlinTest {
	@Test
	fun testSingeCurveCalibration() {

		/*
		 * Calibration of a single curve - OIS curve - self disocunted curve, from a set of calibration products.
		 */

		val referenceDate = LocalDate.of(2012, 1, 10)

		/*
		 * Define the calibration spec generators for our calibration products
		 */
		val deposit = { maturity: String, rate: Double ->
			val scheduleInterfaceRec = ScheduleGenerator.createScheduleFromConventions(referenceDate, 2, "0D", maturity, "tenor", "act/360", "first", "following", BusinessdayCalendarExcludingTARGETHolidays(), 0, 0)
			val scheduleInterfacePay: ScheduleInterface? = null
			val calibrationTime = scheduleInterfaceRec.getPayment(scheduleInterfaceRec.numberOfPeriods - 1)
			val calibrationSpec = CalibratedCurves.CalibrationSpec("EUR-OIS-" + maturity, "Deposit", scheduleInterfaceRec, "", rate, "discount-EUR-OIS", scheduleInterfacePay, null, 0.0, null, "discount-EUR-OIS", calibrationTime)
			calibrationSpec
		}

		val swapSingleCurve = { maturity: String, rate: Double ->
			val scheduleInterfaceRec = ScheduleGenerator.createScheduleFromConventions(referenceDate, 2, "0D", maturity, "annual", "act/360", "first", "modified_following", BusinessdayCalendarExcludingTARGETHolidays(), 0, 1)
			val scheduleInterfacePay = ScheduleGenerator.createScheduleFromConventions(referenceDate, 2, "0D", maturity, "annual", "act/360", "first", "modified_following", BusinessdayCalendarExcludingTARGETHolidays(), 0, 1)
			val calibrationTime = scheduleInterfaceRec.getPayment(scheduleInterfaceRec.numberOfPeriods - 1)
			val calibrationSpec = CalibratedCurves.CalibrationSpec("EUR-OIS-" + maturity, "Swap", scheduleInterfaceRec, "forward-EUR-OIS", 0.0, "discount-EUR-OIS", scheduleInterfacePay, "", rate, "discount-EUR-OIS", "discount-EUR-OIS", calibrationTime)
			calibrationSpec
		}

		val fra = { tenor: String, fixing: String, rate: Double ->
			val scheduleInterfaceRec = ScheduleGenerator.createScheduleFromConventions(referenceDate, 2, fixing, tenor, "tenor", "act/360", "first", "modified_following", BusinessdayCalendarExcludingTARGETHolidays(), 0, 0)
			val calibrationTime = scheduleInterfaceRec.getFixing(scheduleInterfaceRec.numberOfPeriods - 1)
			val curveName = "forward-EUR-" + tenor;
			val calibrationSpec = CalibratedCurves.CalibrationSpec("EUR-" + tenor + "-" + fixing, "FRA", scheduleInterfaceRec, curveName, rate, "discount-EUR-OIS", null, null, 0.0, null, curveName, calibrationTime)
			calibrationSpec
		}

		val swap = { tenor: String, maturity: String, rate: Double ->
			val scheduleInterfaceRec = ScheduleGenerator.createScheduleFromConventions(referenceDate, 2, "0D", maturity, "quarterly", "act/360", "first", "following", BusinessdayCalendarExcludingTARGETHolidays(), 0, 0)
			val scheduleInterfacePay = ScheduleGenerator.createScheduleFromConventions(referenceDate, 2, "0D", maturity, "annual", "E30/360", "first", "following", BusinessdayCalendarExcludingTARGETHolidays(), 0, 0)
			val calibrationTime = scheduleInterfaceRec.getFixing(scheduleInterfaceRec.numberOfPeriods - 1)
			val curveName = "forward-EUR-" + tenor;
			val calibrationSpec = CalibratedCurves.CalibrationSpec("EUR-3M-" + maturity, "Swap", scheduleInterfaceRec, curveName, 0.0, "discount-EUR-OIS", scheduleInterfacePay, "", rate, "discount-EUR-OIS", curveName, calibrationTime)
			calibrationSpec
		}

		/*
		 * Generate empty curve templates (for cloning during calibration)
		 */
		val discountCurveOIS = DiscountCurve.createDiscountCurveFromDiscountFactors("discount-EUR-OIS", referenceDate, doubleArrayOf(0.0), doubleArrayOf(1.0), booleanArrayOf(false), Curve.InterpolationMethod.LINEAR, Curve.ExtrapolationMethod.CONSTANT, Curve.InterpolationEntity.LOG_OF_VALUE)
		val forwardCurveOIS = ForwardCurveFromDiscountCurve("forward-EUR-OIS", "discount-EUR-OIS", referenceDate, "3M")
        val forwardCurve3M = ForwardCurve("forward-EUR-3M", referenceDate, "3M", BusinessdayCalendarExcludingTARGETHolidays(),BusinessdayCalendarInterface.DateRollConvention.FOLLOWING, Curve.InterpolationMethod.LINEAR, Curve.ExtrapolationMethod.CONSTANT, Curve.InterpolationEntity.VALUE,ForwardCurve.InterpolationEntityForward.FORWARD, "discount-EUR-OIS")

		val forwardCurveModel = AnalyticModel(arrayOf(discountCurveOIS, forwardCurveOIS, forwardCurve3M))

		val calibrationSpecs = LinkedList<CalibratedCurves.CalibrationSpec>()

		/*
		 * Calibration products for OIS curve: Deposits
		 */
		calibrationSpecs.add(deposit("1D", 0.202 / 100.0))
		calibrationSpecs.add(deposit("1W", 0.195 / 100.0))
		calibrationSpecs.add(deposit("2W", 0.193 / 100.0))
		calibrationSpecs.add(deposit("3W", 0.193 / 100.0))
		calibrationSpecs.add(deposit("1M", 0.191 / 100.0))
		calibrationSpecs.add(deposit("2M", 0.185 / 100.0))
		calibrationSpecs.add(deposit("3M", 0.180 / 100.0))
		calibrationSpecs.add(deposit("4M", 0.170 / 100.0))
		calibrationSpecs.add(deposit("5M", 0.162 / 100.0))
		calibrationSpecs.add(deposit("6M", 0.156 / 100.0))
		calibrationSpecs.add(deposit("7M", 0.150 / 100.0))
		calibrationSpecs.add(deposit("8M", 0.145 / 100.0))
		calibrationSpecs.add(deposit("9M", 0.141 / 100.0))
		calibrationSpecs.add(deposit("10M", 0.136 / 100.0))
		calibrationSpecs.add(deposit("11M", 0.133 / 100.0))
		calibrationSpecs.add(deposit("12M", 0.129 / 100.0))

		/*
		 * Calibration products for OIS curve: Swaps
		 */
		calibrationSpecs.add(swapSingleCurve("15M", 0.118 / 100.0))
		calibrationSpecs.add(swapSingleCurve("18M", 0.108 / 100.0))
		calibrationSpecs.add(swapSingleCurve("21M", 0.101 / 100.0))
		calibrationSpecs.add(swapSingleCurve("2Y", 0.101 / 100.0))
		calibrationSpecs.add(swapSingleCurve("3Y", 0.194 / 100.0))
		calibrationSpecs.add(swapSingleCurve("4Y", 0.346 / 100.0))
		calibrationSpecs.add(swapSingleCurve("5Y", 0.534 / 100.0))
		calibrationSpecs.add(swapSingleCurve("6Y", 0.723 / 100.0))
		calibrationSpecs.add(swapSingleCurve("7Y", 0.895 / 100.0))
		calibrationSpecs.add(swapSingleCurve("8Y", 1.054 / 100.0))
		calibrationSpecs.add(swapSingleCurve("9Y", 1.189 / 100.0))
		calibrationSpecs.add(swapSingleCurve("10Y", 1.310 / 100.0))
		calibrationSpecs.add(swapSingleCurve("11Y", 1.423 / 100.0))
		calibrationSpecs.add(swapSingleCurve("12Y", 1.520 / 100.0))
		calibrationSpecs.add(swapSingleCurve("15Y", 1.723 / 100.0))
		calibrationSpecs.add(swapSingleCurve("20Y", 1.826 / 100.0))
		calibrationSpecs.add(swapSingleCurve("25Y", 1.877 / 100.0))
		calibrationSpecs.add(swapSingleCurve("30Y", 1.910 / 100.0))
		calibrationSpecs.add(swapSingleCurve("40Y", 2.025 / 100.0))
		calibrationSpecs.add(swapSingleCurve("50Y", 2.101 / 100.0))

		/*
		 * Calibration products for 3M curve: FRAs
		 */
		calibrationSpecs.add(fra("3M", "0D", 0.322 / 100.0))
		calibrationSpecs.add(fra("3M", "1M", 0.329 / 100.0))
		calibrationSpecs.add(fra("3M", "2M", 0.328 / 100.0))
		calibrationSpecs.add(fra("3M", "3M", 0.326 / 100.0))
		calibrationSpecs.add(fra("3M", "6M", 0.323 / 100.0))
		calibrationSpecs.add(fra("3M", "9M", 0.316 / 100.0))
		calibrationSpecs.add(fra("3M", "12M", 0.360 / 100.0))
		calibrationSpecs.add(fra("3M", "15M", 0.390 / 100.0))

		/*
		 * Calibration products for 3M curve: swaps
		 */
		calibrationSpecs.add(swap("3M", "2Y", 0.380 / 100.0))
		calibrationSpecs.add(swap("3M", "3Y", 0.485 / 100.0))
		calibrationSpecs.add(swap("3M", "4Y", 0.628 / 100.0))
		calibrationSpecs.add(swap("3M", "5Y", 0.812 / 100.0))
		calibrationSpecs.add(swap("3M", "6Y", 0.998 / 100.0))
		calibrationSpecs.add(swap("3M", "7Y", 1.168 / 100.0))
		calibrationSpecs.add(swap("3M", "8Y", 1.316 / 100.0))
		calibrationSpecs.add(swap("3M", "9Y", 1.442 / 100.0))
		calibrationSpecs.add(swap("3M", "10Y", 1.557 / 100.0))
		calibrationSpecs.add(swap("3M", "12Y", 1.752 / 100.0))
		calibrationSpecs.add(swap("3M", "15Y", 1.942 / 100.0))
		calibrationSpecs.add(swap("3M", "20Y", 2.029 / 100.0))
		calibrationSpecs.add(swap("3M", "25Y", 2.045 / 100.0))
		calibrationSpecs.add(swap("3M", "30Y", 2.097 / 100.0))
		calibrationSpecs.add(swap("3M", "40Y", 2.208 / 100.0))
		calibrationSpecs.add(swap("3M", "50Y", 2.286 / 100.0))

		/*
		 * Calibrate
		 */
		var calibratedCurves = CalibratedCurves(calibrationSpecs.toTypedArray(), forwardCurveModel, 1E-12)

        /*
         * Get the calibrated model
         */
        var calibratedModel = calibratedCurves.model;

        /*
         * Print calibration errors
         */
        println("\nCalibrated errors:")

        var sumOfSquaredErrors = 0.0
        for (calibratedSpec in calibrationSpecs) {
            val product = calibratedCurves.getCalibrationProductForSpec(calibratedSpec)
            val value = product.getValue(0.0, calibratedModel)
            System.out.println(calibratedSpec.getSymbol() + "\t" + value);
            sumOfSquaredErrors += value * value
        }

        /*
         * Print calibrated curves
         */
        val discountCurveCalibrated = calibratedModel.getDiscountCurve("discount-EUR-OIS")
        val forwardCurveCalibrated = calibratedModel.getForwardCurve("forward-EUR-3M")

        println("\nCalibrated discount curve:")
        println(discountCurveCalibrated)

        println("\nCalibrated forward curve:")
        println(forwardCurveCalibrated)

        Assert.assertEquals("Calibration error", 0.0, Math.sqrt(sumOfSquaredErrors) / calibrationSpecs.size, 1E-10)
    }
}