package net.finmath.singleswaprate.products;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.time.LocalDate;

import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurveFromDiscountCurve;
import net.finmath.marketdata.model.volatilities.SwaptionDataLattice;
import net.finmath.marketdata.model.volatilities.VolatilitySurface.QuotingConvention;
import net.finmath.marketdata.products.Swap;
import net.finmath.marketdata.products.SwapAnnuity;
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

public class ConstantMaturitySwapTest {

	private static double testAccuracy = 0.005;

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

	private static double baseSwapRate;
	private static double[] lowerBounds;
	private static double[] upperBounds;
	private static int[] numbersOfEvaluationPoints;

	private static double valueCMS;
	private static double analyticValue;
	private static double[] values;

	private static AnnuityMappingType type;

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

		final int numberOfSimulations = 50;
		lowerBounds = new double[numberOfSimulations];
		upperBounds = new double[numberOfSimulations];
		numbersOfEvaluationPoints = new int[numberOfSimulations];
		values = new double[numberOfSimulations];

		for(int i = 0; i < numberOfSimulations; i++) {
			lowerBounds[i] = -0.1 -i*0.1;//baseSwapRate * (1 - i*10000000.0);
			upperBounds[i] =  0.1 +i*0.1;//baseSwapRate * (1 + i*10000000.0);
			numbersOfEvaluationPoints[i] = 5000; //500 +500*i;
		}
	}

	@Test
	public void testSimplifiedLinear() throws SolverException, IOException {

		type = AnnuityMappingType.SIMPLIFIEDLINEAR;
		final VolatilityCubeModel model = buildCube(type);

		final ConstantMaturitySwap cms = new ConstantMaturitySwap(fixSchedule, floatSchedule, discountCurveName, forwardCurveSingleName, volatilityCubeName, type);

		valueCMS = cms.getValue(fixSchedule.getFixing(0), model);

		//analytic
		baseSwapRate	= Swap.getForwardSwapRate(fixSchedule, floatSchedule, model.getForwardCurve(forwardCurveSingleName), model);
		final double swapAnnuity 	= SwapAnnuity.getSwapAnnuity(fixSchedule.getFixing(0), fixSchedule, model.getDiscountCurve(discountCurveName), model);
		final double swapFixing	= fixSchedule.getFixing(0);
		final double swapMaturity = fixSchedule.getPayment(fixSchedule.getNumberOfPeriods()-1);
		final double volatility	= model.getVolatilityCube(volatilityCubeName).getValue(model, swapMaturity, swapFixing, baseSwapRate, QuotingConvention.VOLATILITYNORMAL);
		final double payoffUnit	= model.getDiscountCurve(discountCurveName).getDiscountFactor(swapFixing);

		analyticValue = ConstantMaturitySwap.analyticApproximation(baseSwapRate, volatility, swapAnnuity, swapFixing, swapMaturity, payoffUnit);

		for(int i = 0; i< values.length;i++) {
			cms.setIntegrationParameters(lowerBounds[i], upperBounds[i], numbersOfEvaluationPoints[i]);
			values[i] = cms.getValue(fixSchedule.getFixing(0), model);
		}

		Assert.assertEquals(analyticValue, valueCMS, testAccuracy);
	}

	@Test
	public void testBasicPiterbarg() throws SolverException, IOException {

		type = AnnuityMappingType.BASICPITERBARG;
		final VolatilityCubeModel model = buildCube(type);

		final ConstantMaturitySwap cms = new ConstantMaturitySwap(fixSchedule, floatSchedule, discountCurveName, forwardCurveSingleName, volatilityCubeName, type);

		valueCMS = cms.getValue(fixSchedule.getFixing(0), model);

		//analytic
		baseSwapRate	= Swap.getForwardSwapRate(fixSchedule, floatSchedule, model.getForwardCurve(forwardCurveSingleName), model);
		final double swapAnnuity 	= SwapAnnuity.getSwapAnnuity(fixSchedule.getFixing(0), fixSchedule, model.getDiscountCurve(discountCurveName), model);
		final double swapFixing	= fixSchedule.getFixing(0);
		final double swapMaturity = fixSchedule.getPayment(fixSchedule.getNumberOfPeriods()-1);
		final double volatility	= model.getVolatilityCube(volatilityCubeName).getValue(model, swapMaturity, swapFixing, baseSwapRate, QuotingConvention.VOLATILITYNORMAL);
		final double payoffUnit	= model.getDiscountCurve(discountCurveName).getDiscountFactor(swapFixing);

		analyticValue = ConstantMaturitySwap.analyticApproximation(baseSwapRate, volatility, swapAnnuity, swapFixing, swapMaturity, payoffUnit);

		for(int i = 0; i< values.length;i++) {
			cms.setIntegrationParameters(lowerBounds[i], upperBounds[i], numbersOfEvaluationPoints[i]);
			values[i] = cms.getValue(fixSchedule.getFixing(0), model);
		}

		Assert.assertEquals(analyticValue, valueCMS, testAccuracy);
	}

	@Test
	public void testMultiPiterbarg() throws SolverException, IOException {

		type = AnnuityMappingType.MULTIPITERBARG;
		final VolatilityCubeModel model = buildCube(type);

		final ConstantMaturitySwap cms = new ConstantMaturitySwap(fixSchedule, floatSchedule, discountCurveName, forwardCurveMarketName, volatilityCubeName, type);

		valueCMS = cms.getValue(fixSchedule.getFixing(0), model);

		//analytic
		baseSwapRate	= Swap.getForwardSwapRate(fixSchedule, floatSchedule, model.getForwardCurve(forwardCurveMarketName), model);
		final double swapAnnuity 	= SwapAnnuity.getSwapAnnuity(fixSchedule.getFixing(0), fixSchedule, model.getDiscountCurve(discountCurveName), model);
		final double swapFixing	= fixSchedule.getFixing(0);
		final double swapMaturity = fixSchedule.getPayment(fixSchedule.getNumberOfPeriods()-1);
		final double volatility	= model.getVolatilityCube(volatilityCubeName).getValue(model, swapMaturity, swapFixing, baseSwapRate, QuotingConvention.VOLATILITYNORMAL);
		final double payoffUnit	= model.getDiscountCurve(discountCurveName).getDiscountFactor(swapFixing);

		analyticValue = ConstantMaturitySwap.analyticApproximation(baseSwapRate, volatility, swapAnnuity, swapFixing, swapMaturity, payoffUnit);

		for(int i = 0; i< values.length;i++) {
			cms.setIntegrationParameters(lowerBounds[i], upperBounds[i], numbersOfEvaluationPoints[i]);
			values[i] = cms.getValue(fixSchedule.getFixing(0), model);
		}

		Assert.assertEquals(analyticValue, valueCMS, testAccuracy);
	}

	@After
	public void gatherOutput(){

		//output from tests
		System.out.println("Results of test using annuity mapping: " + type + "\n");
		System.out.println("Value of the constant maturity swap using default integration: \t" + valueCMS);
		System.out.println("Value of the constant maturity swap using analytic apporximation: \t" +analyticValue);

		System.out.println("\nThe base swap rate is: "+baseSwapRate);
		System.out.println("LowerBound\tUpperBound\tNumberOfEvaluationPoints\tValue");
		for(int i=0; i< values.length; i++) {
			System.out.println(lowerBounds[i]+"\t"+upperBounds[i]+"\t"+numbersOfEvaluationPoints[i]+"\t"+values[i]);
		}
		System.out.println("\n\n\n\n");
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
