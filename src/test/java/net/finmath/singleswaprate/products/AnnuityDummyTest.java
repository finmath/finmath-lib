package net.finmath.singleswaprate.products;

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
import net.finmath.singleswaprate.annuitymapping.AnnuityMapping.AnnuityMappingType;
import net.finmath.singleswaprate.model.AnalyticModelWithVolatilityCubes;
import net.finmath.singleswaprate.model.VolatilityCubeModel;
import net.finmath.singleswaprate.model.volatilities.VolatilityCube;
import net.finmath.singleswaprate.model.volatilities.VolatilityCubeFactory;
import net.finmath.time.Schedule;
import net.finmath.time.ScheduleGenerator.DaycountConvention;
import net.finmath.time.ScheduleGenerator.Frequency;
import net.finmath.time.ScheduleGenerator.ShortPeriodConvention;
import net.finmath.time.SchedulePrototype;
import net.finmath.time.businessdaycalendar.BusinessdayCalendar;
import net.finmath.time.businessdaycalendar.BusinessdayCalendar.DateRollConvention;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingTARGETHolidays;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingWeekends;

public class AnnuityDummyTest {

	private static double testAccuracy = 0.0001;

	// files
	private static String curveFilePath					= "./src/test/resources/curves";
	private static String discountCurveFileName			= "EUR-EONIA.crv";
	private static String forwardCurveFileName			= "EUR-OIS6M.crv";
	private static String swaptionFilePath				= "./src/test/resources/swaptions";
	private static String payerFileName					= "CashPayerSwaptionPrice.sdl";
	private static String receiverFileName				= "CashReceiverSwaptionPrice.sdl";
	private static String physicalFileName				= "PhysicalSwaptionPriceATM.sdl";

	private static LocalDate referenceDate	= LocalDate.of(2017, 8, 30);
	private static LocalDate startDate		= referenceDate.plusYears(10);
	private static LocalDate endDate		= startDate.plusYears(20);

	private static Schedule fixSchedule;
	private static Schedule floatSchedule;

	private static VolatilityCubeModel model;
	private static String discountCurveName;
	private static String forwardCurveSingleName;
	private static String forwardCurveMarketName;
	private static String volatilityCubeName;
	private static SwaptionDataLattice payerSwaptions;
	private static SwaptionDataLattice receiverSwaptions;
	private static SwaptionDataLattice physicalSwaptions;

	@BeforeClass
	public static void setup() {

		final SchedulePrototype floatMetaSchedule = new SchedulePrototype(
				Frequency.SEMIANNUAL,
				DaycountConvention.ACT_360,
				ShortPeriodConvention.LAST,
				DateRollConvention.FOLLOWING,
				new BusinessdayCalendarExcludingTARGETHolidays(),
				0, //fixingOffsetDays,
				0, //paymentOffsetDays,
				false); //isUseEndOfMonth);

		final SchedulePrototype fixMetaSchedule = new SchedulePrototype(
				Frequency.ANNUAL,
				DaycountConvention.ACT_360,
				ShortPeriodConvention.LAST,
				DateRollConvention.FOLLOWING,
				new BusinessdayCalendarExcludingTARGETHolidays(),
				0, //fixingOffsetDays,
				0, //paymentOffsetDays,
				false); //isUseEndOfMonth);

		floatSchedule	= floatMetaSchedule.generateSchedule(referenceDate, startDate, endDate);
		fixSchedule		= fixMetaSchedule.generateSchedule(referenceDate, startDate, endDate);

		//Get curves
		DiscountCurve discountCurve = null;
		DiscountCurve forwardDiscountCurve = null;
		ForwardCurve forwardCurveMarket = null;
		ForwardCurve forwardCurveSingle = null;
		try (ObjectInputStream discountIn = new ObjectInputStream(new FileInputStream(new File(curveFilePath, discountCurveFileName)));
				ObjectInputStream forwardIn = new ObjectInputStream(new FileInputStream(new File(curveFilePath, forwardCurveFileName)))) {
			discountCurve = (DiscountCurve) discountIn.readObject();
			forwardDiscountCurve = (DiscountCurve) forwardIn.readObject();
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
		forwardCurveSingle = new ForwardCurveFromDiscountCurve("Forward-" + discountCurve.getName(), discountCurve.getName(), referenceDate, "6M");
		forwardCurveMarket = new ForwardCurveFromDiscountCurve("Forward-" + forwardDiscountCurve.getName(), forwardDiscountCurve.getName(), discountCurve.getName(), referenceDate, "6M",
				new BusinessdayCalendarExcludingWeekends(), BusinessdayCalendar.DateRollConvention.FOLLOWING, 1, 0);

		model = new AnalyticModelWithVolatilityCubes();
		model = (AnalyticModelWithVolatilityCubes) model.addCurve(discountCurve.getName(), discountCurve);
		model = (AnalyticModelWithVolatilityCubes) model.addCurve(forwardDiscountCurve.getName(), forwardDiscountCurve);
		model = (AnalyticModelWithVolatilityCubes) model.addCurve(forwardCurveSingle.getName(), forwardCurveSingle);
		model = (AnalyticModelWithVolatilityCubes) model.addCurve(forwardCurveMarket.getName(), forwardCurveMarket);

		discountCurveName		= discountCurve.getName();
		forwardCurveSingleName	= forwardCurveSingle.getName();
		forwardCurveMarketName	= forwardCurveMarket.getName();

		//Get swaption data
		try (ObjectInputStream inPayer = new ObjectInputStream(new FileInputStream(new File(swaptionFilePath, payerFileName)));
				ObjectInputStream inReceiver = new ObjectInputStream(new FileInputStream(new File(swaptionFilePath, receiverFileName)));
				ObjectInputStream inPhysical = new ObjectInputStream(new FileInputStream(new File(swaptionFilePath, physicalFileName)))) {
			payerSwaptions 		= (SwaptionDataLattice) inPayer.readObject();
			receiverSwaptions	= (SwaptionDataLattice) inReceiver.readObject();
			physicalSwaptions	= (SwaptionDataLattice) inPhysical.readObject();
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}

	}

	@Test
	public void testSimplified() throws SolverException, IOException {

		final AnnuityMappingType type = AnnuityMappingType.SIMPLIFIEDLINEAR;
		final VolatilityCubeModel model = buildCube(type);

		final AnnuityDummyProduct mapping = new AnnuityDummyProduct(fixSchedule, floatSchedule, discountCurveName, forwardCurveSingleName, volatilityCubeName, type);

		final double value = mapping.getValue(fixSchedule.getFixing(0), model);

		System.out.println("Expectation of SimplifiedAnnuityMapping: " +value);
		Assert.assertEquals(1, value, testAccuracy);
	}

	@Test
	public void testBasic() throws SolverException, IOException {

		final AnnuityMappingType type = AnnuityMappingType.BASICPITERBARG;
		final VolatilityCubeModel model = buildCube(type);

		final AnnuityDummyProduct mapping = new AnnuityDummyProduct(fixSchedule, floatSchedule, discountCurveName, forwardCurveSingleName, volatilityCubeName, type);

		final double value = mapping.getValue(fixSchedule.getFixing(0), model);

		System.out.println("Expectation of BasicPiterbargAnnuityMapping: " +value);
		Assert.assertEquals(1, value, testAccuracy);
	}

	@Test
	public void testMulti() throws SolverException, IOException {

		final AnnuityMappingType type = AnnuityMappingType.MULTIPITERBARG;
		final VolatilityCubeModel model = buildCube(type);

		final AnnuityDummyProduct mapping = new AnnuityDummyProduct(fixSchedule, floatSchedule, discountCurveName, forwardCurveMarketName, volatilityCubeName, type);

		final double value = mapping.getValue(fixSchedule.getFixing(0), model);

		System.out.println("Expectation of MultiPiterbargAnnuityMapping: " +value);
		Assert.assertEquals(1, value, testAccuracy);
	}

	//creating a volatility cube for tests
	private static VolatilityCubeModel buildCube(final AnnuityMappingType type) throws SolverException, IOException {
		final VolatilityCubeFactory factory = new VolatilityCubeFactory(
				referenceDate,
				payerSwaptions,
				receiverSwaptions,
				physicalSwaptions,
				0.3,	//displacement,
				0.5,	//beta,
				0.5,	//correlationDecay,
				0.5,	//iborOisDecorrelation,
				type);

		volatilityCubeName				= type.toString();
		final VolatilityCube cube	= factory.buildShiftedSmileSABRCube(volatilityCubeName, model);

		return model.addVolatilityCube(cube);
	}
}
