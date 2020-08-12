package net.finmath.singleswaprate.data;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import net.finmath.singleswaprate.data.DataTable.TableConvention;
import net.finmath.time.Schedule;
import net.finmath.time.ScheduleGenerator.DaycountConvention;
import net.finmath.time.ScheduleGenerator.Frequency;
import net.finmath.time.ScheduleGenerator.ShortPeriodConvention;
import net.finmath.time.SchedulePrototype;
import net.finmath.time.businessdaycalendar.BusinessdayCalendar.DateRollConvention;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingTARGETHolidays;

public class DataTablesTest {

	private static LocalDate referenceDate =  LocalDate.of(2017, 8, 30);
	private static SchedulePrototype scheduleMetaData = new SchedulePrototype(
			Frequency.TENOR,
			DaycountConvention.ACT_360,
			ShortPeriodConvention.LAST,
			DateRollConvention.FOLLOWING,
			new BusinessdayCalendarExcludingTARGETHolidays(),
			0, 0, false);

	private static List<Integer> maturities;
	private static List<Integer> terminations;
	private static List<Double> values;

	@BeforeClass
	public static void setupTableValues() {

		maturities	= new ArrayList<>();
		terminations	= new ArrayList<>();
		values			= new ArrayList<>();

		for(int mat = 10; mat < 110; mat += 10) {
			for(int ter = 50; ter < 210; ter += 10) {
				maturities.add(mat);
				terminations.add(ter);
				values.add((double) mat * ter);
			}
		}
	}

	@Test
	public void testTables() throws IOException {

		final DataTableLight light			= new DataTableLight("TestTable", TableConvention.MONTHS, maturities, terminations, values);
		final DataTableBasic regular			= DataTableBasic.upgradeDataTableLight(light, referenceDate, scheduleMetaData);
		final DataTableInterpolated spline	= DataTableInterpolated.interpolateDataTable(regular);
		final DataTableLinear linear			= DataTableLinear.interpolateDataTable(regular);


		System.out.println(light.getClass());
		System.out.println(light.toString() +"\n\n");
		System.out.println(regular.getClass());
		System.out.println(regular.toString() +"\n\n");
		System.out.println(spline.getClass());
		System.out.println(spline.toString() +"\n\n");
		System.out.println(linear.getClass());
		System.out.println(linear.toString() +"\n\n");

		int intMat = 60; int intTerm = 60;
		System.out.println(intMat+"M"+intTerm+"M via int: ");
		try {
			System.out.println("light: "+light.getValue(intMat, intTerm));
		} catch (final Exception e) {
			System.out.println("light failed.");
			Assert.fail(e.toString());
		}
		try {
			System.out.println("regular: "+regular.getValue(intMat, intTerm));
		} catch (final Exception e) {
			System.out.println("regular failed.");
			Assert.fail(e.toString());
		}
		try {
			System.out.println("spline: "+spline.getValue(intMat, intTerm));
		} catch (final Exception e) {
			System.out.println("spline failed.");
			Assert.fail(e.toString());
		}
		try {
			System.out.println("linear: "+linear.getValue(intMat, intTerm));
		} catch (final Exception e) {
			System.out.println("linear failed.");
			Assert.fail(e.toString());
		}


		System.out.println("\n\n"+intMat+"M"+intTerm+"M via double: ");
		Schedule schedule = scheduleMetaData.generateSchedule(referenceDate, referenceDate.plusMonths(intMat), referenceDate.plusMonths(intMat+intTerm));
		double doubleMat = schedule.getFixing(0);
		double doubleTerm = schedule.getPayment(schedule.getNumberOfPeriods()-1);
		try {
			System.out.println("light: "+light.getValue(doubleMat, doubleTerm));
		} catch (final Exception e) {
			System.out.println("light failed.");
		}
		try {
			System.out.println("regular: "+regular.getValue(doubleMat, doubleTerm));
		} catch (final Exception e) {
			System.out.println("regular failed.");
			Assert.fail(e.toString());
		}
		try {
			System.out.println("spline: "+spline.getValue(doubleMat, doubleTerm));
		} catch (final Exception e) {
			System.out.println("spline failed.");
			Assert.fail(e.toString());
		}
		try {
			System.out.println("linear: "+linear.getValue(doubleMat, doubleTerm));
		} catch (final Exception e) {
			System.out.println("linear failed.");
			Assert.fail(e.toString());
		}

		intMat = 60; intTerm = 186;
		System.out.println("\n\n"+intMat+"M"+intTerm+"M via int: ");
		try {
			System.out.println("light: "+light.getValue(intMat, intTerm));
		} catch (final Exception e) {
			System.out.println("light failed.");
		}
		try {
			System.out.println("regular: "+regular.getValue(intMat, intTerm));
		} catch (final Exception e) {
			System.out.println("regular failed.");
		}
		try {
			System.out.println("spline: "+spline.getValue(intMat, intTerm));
		} catch (final Exception e) {
			System.out.println("spline failed.");
			Assert.fail(e.toString());
		}
		try {
			System.out.println("linear: "+linear.getValue(intMat, intTerm));
		} catch (final Exception e) {
			System.out.println("linear failed.");
			Assert.fail(e.toString());
		}


		System.out.println("\n\n"+intMat+"M"+intTerm+"M via double: ");
		schedule = scheduleMetaData.generateSchedule(referenceDate, referenceDate.plusMonths(intMat), referenceDate.plusMonths(intMat+intTerm));
		doubleMat = schedule.getFixing(0);
		doubleTerm = schedule.getPayment(schedule.getNumberOfPeriods()-1);
		try {
			System.out.println("light: "+light.getValue(doubleMat, doubleTerm));
		} catch (final Exception e) {
			System.out.println("light failed.");
		}
		try {
			System.out.println("regular: "+regular.getValue(doubleMat, doubleTerm));
		} catch (final Exception e) {
			System.out.println("regular failed.");
		}
		try {
			System.out.println("spline: "+spline.getValue(doubleMat, doubleTerm));
		} catch (final Exception e) {
			System.out.println("spline failed.");
			Assert.fail(e.toString());
		}
		try {
			System.out.println("linear: "+linear.getValue(doubleMat, doubleTerm));
		} catch (final Exception e) {
			System.out.println("linear failed.");
			Assert.fail(e.toString());
		}


		intMat = 37; intTerm = 180;
		System.out.println("\n\n"+intMat+"M"+intTerm+"M via int: ");
		try {
			System.out.println("light: "+light.getValue(intMat, intTerm));
		} catch (final Exception e) {
			System.out.println("light failed.");
		}
		try {
			System.out.println("regular: "+regular.getValue(intMat, intTerm));
		} catch (final Exception e) {
			System.out.println("regular failed.");
		}
		try {
			System.out.println("spline: "+spline.getValue(intMat, intTerm));
		} catch (final Exception e) {
			System.out.println("spline failed.");
			Assert.fail(e.toString());
		}
		try {
			System.out.println("linear: "+linear.getValue(intMat, intTerm));
		} catch (final Exception e) {
			System.out.println("linear failed.");
			Assert.fail(e.toString());
		}


		System.out.println("\n\n"+intMat+"M"+intTerm+"M via double: ");
		schedule = scheduleMetaData.generateSchedule(referenceDate, referenceDate.plusMonths(intMat), referenceDate.plusMonths(intMat+intTerm));
		doubleMat = schedule.getFixing(0);
		doubleTerm = schedule.getPayment(schedule.getNumberOfPeriods()-1);
		try {
			System.out.println("light: "+light.getValue(doubleMat, doubleTerm));
		} catch (final Exception e) {
			System.out.println("light failed.");
		}
		try {
			System.out.println("regular: "+regular.getValue(doubleMat, doubleTerm));
		} catch (final Exception e) {
			System.out.println("regular failed.");
		}
		try {
			System.out.println("spline: "+spline.getValue(doubleMat, doubleTerm));
		} catch (final Exception e) {
			System.out.println("spline failed.");
			Assert.fail(e.toString());
		}
		try {
			System.out.println("linear: "+linear.getValue(doubleMat, doubleTerm));
		} catch (final Exception e) {
			System.out.println("linear failed.");
			Assert.fail(e.toString());
		}


		intMat = 54; intTerm = 164;
		System.out.println("\n\n"+intMat+"M"+intTerm+"M via int: ");
		try {
			System.out.println("light: "+light.getValue(intMat, intTerm));
		} catch (final Exception e) {
			System.out.println("light failed.");
		}
		try {
			System.out.println("regular: "+regular.getValue(intMat, intTerm));
		} catch (final Exception e) {
			System.out.println("regular failed.");
		}
		try {
			System.out.println("spline: "+spline.getValue(intMat, intTerm));
		} catch (final Exception e) {
			System.out.println("spline failed.");
			Assert.fail(e.toString());
		}
		try {
			System.out.println("linear: "+linear.getValue(intMat, intTerm));
		} catch (final Exception e) {
			System.out.println("linear failed.");
			Assert.fail(e.toString());
		}


		System.out.println("\n\n"+intMat+"M"+intTerm+"M via double: ");
		schedule = scheduleMetaData.generateSchedule(referenceDate, referenceDate.plusMonths(intMat), referenceDate.plusMonths(intMat+intTerm));
		doubleMat = schedule.getFixing(0);
		doubleTerm = schedule.getPayment(schedule.getNumberOfPeriods()-1);
		try {
			System.out.println("light: "+light.getValue(doubleMat, doubleTerm));
		} catch (final Exception e) {
			System.out.println("light failed.");
		}
		try {
			System.out.println("regular: "+regular.getValue(doubleMat, doubleTerm));
		} catch (final Exception e) {
			System.out.println("regular failed.");
		}
		try {
			System.out.println("spline: "+spline.getValue(doubleMat, doubleTerm));
		} catch (final Exception e) {
			System.out.println("spline failed.");
			Assert.fail(e.toString());
		}
		try {
			System.out.println("linear: "+linear.getValue(doubleMat, doubleTerm));
		} catch (final Exception e) {
			System.out.println("linear failed.");
			Assert.fail(e.toString());
		}


		intMat = 60; intTerm = 199;
		System.out.println("\n\n"+intMat+"M"+intTerm+"M via int: ");
		try {
			System.out.println("light: "+light.getValue(intMat, intTerm));
		} catch (final Exception e) {
			System.out.println("light failed.");
		}
		try {
			System.out.println("regular: "+regular.getValue(intMat, intTerm));
		} catch (final Exception e) {
			System.out.println("regular failed.");
		}
		try {
			System.out.println("spline: "+spline.getValue(intMat, intTerm));
		} catch (final Exception e) {
			System.out.println("spline failed.");
			Assert.fail(e.toString());
		}
		try {
			System.out.println("linear: "+linear.getValue(intMat, intTerm));
		} catch (final Exception e) {
			System.out.println("linear failed.");
			Assert.fail(e.toString());
		}


		System.out.println("\n\n"+intMat+"M"+intTerm+"M via double: ");
		schedule = scheduleMetaData.generateSchedule(referenceDate, referenceDate.plusMonths(intMat), referenceDate.plusMonths(intMat+intTerm));
		doubleMat = schedule.getFixing(0);
		doubleTerm = schedule.getPayment(schedule.getNumberOfPeriods()-1);
		try {
			System.out.println("light: "+light.getValue(doubleMat, doubleTerm));
		} catch (final Exception e) {
			System.out.println("light failed.");
		}
		try {
			System.out.println("regular: "+regular.getValue(doubleMat, doubleTerm));
		} catch (final Exception e) {
			System.out.println("regular failed.");
		}
		try {
			System.out.println("spline: "+spline.getValue(doubleMat, doubleTerm));
		} catch (final Exception e) {
			System.out.println("spline failed.");
			Assert.fail(e.toString());
		}
		try {
			System.out.println("linear: "+linear.getValue(doubleMat, doubleTerm));
		} catch (final Exception e) {
			System.out.println("linear failed.");
			Assert.fail(e.toString());
		}

		intMat = 60; intTerm = 220;
		System.out.println("\n\n"+intMat+"M"+intTerm+"M via int: ");
		try {
			System.out.println("light: "+light.getValue(intMat, intTerm));
		} catch (final Exception e) {
			System.out.println("light failed.");
		}
		try {
			System.out.println("regular: "+regular.getValue(intMat, intTerm));
		} catch (final Exception e) {
			System.out.println("regular failed.");
		}
		try {
			System.out.println("spline: "+spline.getValue(intMat, intTerm));
		} catch (final Exception e) {
			System.out.println("spline failed.");
			//			Assert.fail(e.toString());
		}
		try {
			System.out.println("linear: "+linear.getValue(intMat, intTerm));
		} catch (final Exception e) {
			System.out.println("linear failed.");
			Assert.fail(e.toString());
		}


		System.out.println("\n\n"+intMat+"M"+intTerm+"M via double: ");
		schedule = scheduleMetaData.generateSchedule(referenceDate, referenceDate.plusMonths(intMat), referenceDate.plusMonths(intMat+intTerm));
		doubleMat = schedule.getFixing(0);
		doubleTerm = schedule.getPayment(schedule.getNumberOfPeriods()-1);
		try {
			System.out.println("light: "+light.getValue(doubleMat, doubleTerm));
		} catch (final Exception e) {
			System.out.println("light failed.");
		}
		try {
			System.out.println("regular: "+regular.getValue(doubleMat, doubleTerm));
		} catch (final Exception e) {
			System.out.println("regular failed.");
		}
		try {
			System.out.println("spline: "+spline.getValue(doubleMat, doubleTerm));
		} catch (final Exception e) {
			System.out.println("spline failed.");
			//			Assert.fail(e.toString());
		}
		try {
			System.out.println("linear: "+linear.getValue(doubleMat, doubleTerm));
		} catch (final Exception e) {
			System.out.println("linear failed.");
			Assert.fail(e.toString());
		}

	}


}
