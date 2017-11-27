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
import org.junit.Assert
import org.junit.Test
import java.time.LocalDate
import java.util.*

class CurveCalibrationKotlinTest {
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

        val swap = { maturity: String, rate: Double ->
            val scheduleInterfaceRec = ScheduleGenerator.createScheduleFromConventions(referenceDate, 2, "0D", maturity, "annual", "act/360", "first", "modified_following", BusinessdayCalendarExcludingTARGETHolidays(), 0, 1)
            val scheduleInterfacePay = ScheduleGenerator.createScheduleFromConventions(referenceDate, 2, "0D", maturity, "annual", "act/360", "first", "modified_following", BusinessdayCalendarExcludingTARGETHolidays(), 0, 1)
            val calibrationTime = scheduleInterfaceRec.getPayment(scheduleInterfaceRec.numberOfPeriods - 1)
            val calibrationSpec = CalibratedCurves.CalibrationSpec("EUR-OIS-" + maturity, "Swap", scheduleInterfaceRec, "forward-EUR-OIS", 0.0, "discount-EUR-OIS", scheduleInterfacePay, "", rate!!, "discount-EUR-OIS", "discount-EUR-OIS", calibrationTime)
            calibrationSpec
        }

        /*
		 * Generate empty curve template (for cloning during calibration)
		 */
        val times = doubleArrayOf(0.0)
        val discountFactors = doubleArrayOf(1.0)
        val isParameter = booleanArrayOf(false)

        val discountCurve = DiscountCurve.createDiscountCurveFromDiscountFactors("discount-EUR-OIS", referenceDate, times, discountFactors, isParameter, Curve.InterpolationMethod.LINEAR, Curve.ExtrapolationMethod.CONSTANT, Curve.InterpolationEntity.LOG_OF_VALUE)
        val forwardCurveFromDiscountCurve = ForwardCurveFromDiscountCurve("forward-EUR-OIS", "discount-EUR-OIS", referenceDate, "3M")


        val curveInterfacesSD = arrayOf<CurveInterface>(discountCurve, forwardCurveFromDiscountCurve)
        val forwardCurveModel = AnalyticModel(curveInterfacesSD)

        val calibrationSpecs = LinkedList<CalibratedCurves.CalibrationSpec>()

        calibrationSpecs.add(deposit("1D", 0.00202))
        calibrationSpecs.add(deposit("1M", 0.00191))
        calibrationSpecs.add(deposit("12M", 0.00129))

        calibrationSpecs.add(swap("18M", 0.00108))
        calibrationSpecs.add(swap("21M", 0.00101))
        calibrationSpecs.add(swap("2Y", 0.00101))
        calibrationSpecs.add(swap("3Y", 0.00194))
        calibrationSpecs.add(swap("4Y", 0.00346))
        calibrationSpecs.add(swap("5Y", 0.00534))

        val calibratedSpecsArray = calibrationSpecs.toTypedArray()
        var calibratedCurves: CalibratedCurves? = null
        try {
            calibratedCurves = CalibratedCurves(calibratedSpecsArray, forwardCurveModel, 0.00000001)
        } catch (e: SolverException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        } catch (e: CloneNotSupportedException) {
            e.printStackTrace()
        }

        var sumOfSquaredErrors = 0.0
        for (calibratedSpec in calibrationSpecs) {
            val product = calibratedCurves!!.getCalibrationProductForSpec(calibratedSpec)
            val value = product.getValue(0.0, calibratedCurves.model)
            sumOfSquaredErrors += value * value
        }

        Assert.assertEquals("Calibration error", 0.0, Math.sqrt(sumOfSquaredErrors) / calibrationSpecs.size, 1E-10)

        val discountCurveCalibrated = calibratedCurves!!.getCurve("discount-EUR-OIS") as DiscountCurveInterface
        println(discountCurveCalibrated)
    }
}