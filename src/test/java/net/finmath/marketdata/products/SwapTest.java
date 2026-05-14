package net.finmath.marketdata.products;

import java.time.LocalDate;
import java.util.Random;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.AnalyticModelFromCurvesAndVols;
import net.finmath.marketdata.model.curves.Curve;
import net.finmath.marketdata.model.curves.CurveInterpolation.ExtrapolationMethod;
import net.finmath.marketdata.model.curves.CurveInterpolation.InterpolationEntity;
import net.finmath.marketdata.model.curves.CurveInterpolation.InterpolationMethod;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.DiscountCurveInterpolation;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurveInterpolation;
import net.finmath.time.RegularSchedule;
import net.finmath.time.Schedule;
import net.finmath.time.ScheduleGenerator;
import net.finmath.time.TimeDiscretizationFromArray;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingTARGETHolidays;

public class SwapTest {
	private final LocalDate referenceDate = LocalDate.of(2017, 6, 15);
	private static double calibrateMilli;
	
	/**
	 * We calibrate a duration that corresponds to a millisecond on an Apple M1 with Java 17.
	 * 
	 * This is just to have a rough (large) independend bound for the timings that is roughly
	 * independend of a system.
	 */
	@BeforeAll
	public static void calibrateDuration() {

		long timeStart = System.nanoTime();

		Random random = new Random(3141);
		double numberOfRuns = 100000;
		double sum = 0.0;
		for(int i = 0; i<numberOfRuns; i++) {
			sum += 2*random.nextDouble();
		}
		double average = sum/numberOfRuns;
		
		long timeEnd = System.nanoTime();
		double durationMillis = (double)(timeEnd-timeStart)/1000000;
		
		// Approximate 1 ms
		calibrateMilli = durationMillis / 10;
		System.out.println("Calibrated millisecond.......: " + calibrateMilli);
	}

	@Test
	public void testRegularSchedule() {

		double sum = 0.0;
		long timeStart = System.nanoTime();
		int numberOfRuns = 100000;

		for(int i=0; i< numberOfRuns; i++) {
			// Create the discount curve
			final DiscountCurve discountCurve = DiscountCurveInterpolation.createDiscountCurveFromZeroRates(
					"discountCurve",
					referenceDate,
					new double[] {0.5, 40.00}	/* zero rate end points */,
					new double[] {0.03,  0.04}	/* zeros */,
					new boolean[] {false,  false},
					InterpolationMethod.LINEAR,
					ExtrapolationMethod.CONSTANT,
					InterpolationEntity.LOG_OF_VALUE_PER_TIME
					);

			AnalyticModel curveModel = new AnalyticModelFromCurvesAndVols(new Curve[] { discountCurve });

			// Create the forward curve
			final ForwardCurve forwardCurve = ForwardCurveInterpolation.createForwardCurveFromForwards(
					"forwardCurve"								/* name of the curve */,
					referenceDate,
					"6M",
					ForwardCurveInterpolation.InterpolationEntityForward.FORWARD,
					"discountCurve",
					curveModel,
					new double[] {0.5 , 1.0 , 2.0 , 5.0 , 40.0}	/* fixings of the forward */,
					new double[] {Math.random()/100.0, Math.random()/100.0, Math.random()/100.0, Math.random()/100.0, Math.random()/100.0}	/* forwards */
					);

			curveModel = curveModel.addCurves(forwardCurve);

			AnalyticProduct swap = new Swap(new RegularSchedule(new TimeDiscretizationFromArray(0.0, 10, 0.5)), null, 0.04, "discountCurve", new RegularSchedule(new TimeDiscretizationFromArray(0.0, 10, 0.5)), "forwardCurve", 0.0, "discountCurve");

			double value = swap.getValue(0, curveModel);
			
			// Use the value to avoid optimization
			sum += value;
		}
		long timeEnd = System.nanoTime();

		double durationMillis = (double)(timeEnd-timeStart)/1000000/numberOfRuns;
		System.out.println("Swap valuation required......: " + durationMillis + " ms");
		
		Assertions.assertTrue(durationMillis < calibrateMilli);
	}

	@Test
	public void testMetadataSchedule() {

		double sum = 0.0;
		long timeStart = System.nanoTime();
		int numberOfRuns = 100000;
		for(int i=0; i< numberOfRuns; i++) {

			// Create the forward curve (initial value of the LIBOR market model)
			final DiscountCurve discountCurve = DiscountCurveInterpolation.createDiscountCurveFromZeroRates(
					"discountCurve",
					referenceDate,
					new double[] {0.5, 40.00}	/* zero rate end points */,
					new double[] {0.03,  0.04}	/* zeros */,
					new boolean[] {false,  false},
					InterpolationMethod.LINEAR,
					ExtrapolationMethod.CONSTANT,
					InterpolationEntity.LOG_OF_VALUE_PER_TIME
					);

			AnalyticModel curveModel = new AnalyticModelFromCurvesAndVols(new Curve[] { discountCurve });

			// Create the forward curve (initial value of the LIBOR market model)
			final ForwardCurve forwardCurve = ForwardCurveInterpolation.createForwardCurveFromForwards(
					"forwardCurve"								/* name of the curve */,
					referenceDate,
					"6M",
					ForwardCurveInterpolation.InterpolationEntityForward.FORWARD,
					"discountCurve",
					curveModel,
					new double[] {0.5 , 1.0 , 2.0 , 5.0 , 40.0}	/* fixings of the forward */,
					new double[] {Math.random()/100.0, Math.random()/100.0, Math.random()/100.0, Math.random()/100.0, Math.random()/100.0}	/* forwards */
					);

			curveModel = curveModel.addCurves(forwardCurve);

			final Schedule schedule2 = ScheduleGenerator.createScheduleFromConventions(
					LocalDate.of(2012, 1, 10) /* referenceDate */,
					"2D" /* startOffset */,
					"10Y" /* maturity */,
					"quarterly" /* frequency */,
					"act/360" /* daycountConvention */,
					"first" /* shortPeriodConvention */,
					"following",
					new BusinessdayCalendarExcludingTARGETHolidays(),
					0,
					0);

			AnalyticProduct swap = new Swap(schedule2, null, 0.04, "discountCurve", schedule2, "forwardCurve", 0.0, "discountCurve");

			double value = swap.getValue(0, curveModel);
			
			// Use the value to avoid optimization
			sum += value;
		}
		long timeEnd = System.nanoTime();

		double durationMillis = (double)(timeEnd-timeStart)/1000000/numberOfRuns;
		System.out.println("Swap valuation required......: " + durationMillis + " ms");

		Assertions.assertTrue(durationMillis < calibrateMilli);
	}
}
