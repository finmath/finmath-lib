package net.finmath.singleswaprate.annuitymapping;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.time.LocalDate;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurveFromDiscountCurve;
import net.finmath.marketdata.model.volatilities.SwaptionDataLattice;
import net.finmath.optimizer.SolverException;
import net.finmath.singleswaprate.model.AnalyticModelWithVolatilityCubes;
import net.finmath.singleswaprate.model.VolatilityCubeModel;
import net.finmath.singleswaprate.model.volatilities.SABRVolatilityCubeParallelFactory;
import net.finmath.singleswaprate.model.volatilities.VolatilityCube;
import net.finmath.singleswaprate.products.NormalizingDummyProduct;
import net.finmath.time.Schedule;
import net.finmath.time.ScheduleGenerator.DaycountConvention;
import net.finmath.time.ScheduleGenerator.Frequency;
import net.finmath.time.ScheduleGenerator.ShortPeriodConvention;
import net.finmath.time.SchedulePrototype;
import net.finmath.time.businessdaycalendar.BusinessdayCalendar;
import net.finmath.time.businessdaycalendar.BusinessdayCalendar.DateRollConvention;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingTARGETHolidays;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingWeekends;

/**
 * Test to verify that the expectation of the normalizing function equals 1.
 *
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public class NormalizingFunctionTest {

	private static String curveFilePath				= "./src/test/resources/curves";
	private static String discountCurveFileName		= "EUR-EONIA.crv";
	private static String forwardCurveFileName		= "EUR-OIS6M.crv";
	private static String swaptionFilePath			= "./src/test/resources/swaptions";
	private static String swaptionFileName			= "PhysicalSwaptionPriceATM.sdl";

	private static double sabrBeta 					= 0.5;
	private static double sabrDisplacement			= 0.5;
	private static double sabrRho					= 0.25;
	private static double sabrVolvol				= 0.25;
	private static double correlationDecay			= 1;
	private static double iborOisDecorrelation		= 1;

	private static String discountCurveName;
	private static String forwardCurveName;
	private static String volatilityCubeName;

	private static VolatilityCubeModel model;
	private static Schedule fixSchedule;
	private static Schedule floatSchedule;

	@BeforeClass
	public static void setup() throws IOException, SolverException {

		final LocalDate referenceDate = LocalDate.of(2017, 8, 30);
		final LocalDate startDate = referenceDate.plusYears(10);
		final LocalDate endDate	= startDate.plusYears(20);

		final SchedulePrototype floatMetaSchedule = new SchedulePrototype(
				Frequency.SEMIANNUAL,
				DaycountConvention.ACT_360,
				ShortPeriodConvention.LAST,
				DateRollConvention.FOLLOWING,
				new BusinessdayCalendarExcludingTARGETHolidays(),
				0, 0, false);

		final SchedulePrototype fixMetaSchedule = new SchedulePrototype(
				Frequency.ANNUAL,
				DaycountConvention.ACT_360,
				ShortPeriodConvention.LAST,
				DateRollConvention.FOLLOWING,
				new BusinessdayCalendarExcludingTARGETHolidays(),
				0, 0, false);

		fixSchedule		= fixMetaSchedule.generateSchedule(referenceDate, startDate, endDate);
		floatSchedule	= floatMetaSchedule.generateSchedule(referenceDate, startDate, endDate);

		//Get curves
		DiscountCurve discountCurve = null;
		DiscountCurve forwardDiscountCurve = null;
		ForwardCurve forwardCurve = null;
		try (ObjectInputStream discountIn = new ObjectInputStream(new FileInputStream(new File(curveFilePath, discountCurveFileName)));
				ObjectInputStream forwardIn = new ObjectInputStream(new FileInputStream(new File(curveFilePath, forwardCurveFileName)))) {
			discountCurve = (DiscountCurve) discountIn.readObject();
			forwardDiscountCurve = (DiscountCurve) forwardIn.readObject();
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
		forwardCurve = new ForwardCurveFromDiscountCurve("Forward-" + forwardDiscountCurve.getName(), forwardDiscountCurve.getName(), discountCurve.getName(), referenceDate, "6M",
				new BusinessdayCalendarExcludingWeekends(), BusinessdayCalendar.DateRollConvention.FOLLOWING, 1, 0);

		model = new AnalyticModelWithVolatilityCubes();
		model = (AnalyticModelWithVolatilityCubes) model.addCurve(discountCurve.getName(), discountCurve);
		model = (AnalyticModelWithVolatilityCubes) model.addCurve(forwardCurve.getName(), forwardCurve);
		model = (AnalyticModelWithVolatilityCubes) model.addCurve(forwardDiscountCurve.getName(), forwardDiscountCurve);

		//Get swaption data
		SwaptionDataLattice swaptions = null;
		try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(new File(swaptionFilePath, swaptionFileName)))) {
			swaptions = (SwaptionDataLattice) in.readObject();
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}

		discountCurveName	= discountCurve.getName();
		forwardCurveName	= forwardCurve.getName();
		volatilityCubeName	= "VOLA";

		final VolatilityCube cube = SABRVolatilityCubeParallelFactory.createSABRVolatilityCubeParallel(
				volatilityCubeName,
				referenceDate,
				fixMetaSchedule,
				floatMetaSchedule,
				sabrDisplacement,
				sabrBeta, sabrRho,
				sabrVolvol,
				correlationDecay,
				iborOisDecorrelation,
				swaptions,
				model,
				forwardCurve.getName());

		model = model.addVolatilityCube(cube);

	}

	@Test
	public void testExpectation() {

		final NormalizingFunction normalizer = new ExponentialNormalizer(fixSchedule, floatSchedule, discountCurveName, forwardCurveName, volatilityCubeName, model);
		final NormalizingDummyProduct normalizerDummy = new NormalizingDummyProduct(fixSchedule, floatSchedule, discountCurveName, forwardCurveName, volatilityCubeName, normalizer);

		final double value = normalizerDummy.getValue(fixSchedule.getFixing(0), model);
		System.out.println("Expectation of the normalizing function is: " + value);

		Assert.assertEquals(1, value, 0.01);
	}

}
