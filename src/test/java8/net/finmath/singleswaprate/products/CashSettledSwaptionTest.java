package net.finmath.singleswaprate.products;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.Locale;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import net.finmath.functions.AnalyticFormulas;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurveFromDiscountCurve;
import net.finmath.marketdata.model.volatilities.SwaptionDataLattice;
import net.finmath.marketdata.model.volatilities.VolatilitySurface.QuotingConvention;
import net.finmath.marketdata.products.Swap;
import net.finmath.marketdata.products.SwapAnnuity;
import net.finmath.optimizer.SolverException;
import net.finmath.singleswaprate.annuitymapping.AnnuityMapping;
import net.finmath.singleswaprate.annuitymapping.AnnuityMapping.AnnuityMappingType;
import net.finmath.singleswaprate.annuitymapping.AnnuityMappingFactory;
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

@FixMethodOrder(MethodSorters.NAME_ASCENDING)

public class CashSettledSwaptionTest {

	// files
	private static String curveFilePath					= "./src/test/resources/curves";
	private static String discountCurveFileName			= "EUR-EONIA.crv";
	private static String forwardCurveFileName			= "EUR-OIS6M.crv";
	private static String swaptionFilePath				= "./src/test/resources/swaptions";
	private static String payerFileName					= "CashPayerSwaptionPrice.sdl";
	private static String receiverFileName				= "CashReceiverSwaptionPrice.sdl";
	private static String physicalFileName				= "PhysicalSwaptionPriceATM.sdl";

	// specify evaluated moneyness
	private static int numberOfEvaluationPoints 	= 100;
	private static double[] evaluationRanges		= new double[]{ -150 , 150 };
	private static double step						= (evaluationRanges[1]-evaluationRanges[0])/numberOfEvaluationPoints;

	private static LocalDate referenceDate	= LocalDate.of(2017, 8, 30);
	private static LocalDate startDate		= referenceDate.plusYears(10);
	private static LocalDate endDate		= startDate.plusYears(20);

	//replicaiton parameters
	private static double replicationLowerBound 			= -0.25;
	private static double replicationUpperBound				= 0.25;
	private static int replicationNumberOfEvaluationPoints	= 500;

	private static StringBuilder[] outputs 	= new StringBuilder[2*(numberOfEvaluationPoints +2)];
	private static DecimalFormat formatter	= new DecimalFormat(" 0.00000000;-0.00000000", new DecimalFormatSymbols(Locale.ENGLISH));

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
	private static double swapAnnuity;
	private static double cashAnnuity;

	private static double[] moneynesss;
	private static double[] valuesCashPay;
	private static double[] valuesCashRec;
	private static double[] valuesPhysPay;
	private static double[] valuesPhysRec;

	private static AnnuityMappingType type;
	private static AnnuityMapping mapping;

	@BeforeClass
	public static void setup() {

		for(int index = 0; index < outputs.length; index++) {
			outputs[index] = new StringBuilder();
		}

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

		moneynesss = new double[numberOfEvaluationPoints];
		for(int index = 0; index < numberOfEvaluationPoints; index++) {
			moneynesss[index] = evaluationRanges[0] + index *step;
		}

	}

	@Before
	public void initiate(){

		//cleaning house
		valuesCashPay = new double[numberOfEvaluationPoints];
		valuesCashRec = new double[numberOfEvaluationPoints];
		valuesPhysPay = new double[numberOfEvaluationPoints];
		valuesPhysRec = new double[numberOfEvaluationPoints];

	}

	@Test
	public void a_testSimplifiedLinear() throws SolverException, IOException {

		type										= AnnuityMappingType.SIMPLIFIEDLINEAR;
		model										= buildCube(type);
		final String forwardCurveName						= forwardCurveSingleName;

		swapAnnuity		= SwapAnnuity.getSwapAnnuity(fixSchedule.getFixing(0), fixSchedule, model.getDiscountCurve(discountCurveName), model);
		baseSwapRate	= Swap.getForwardSwapRate(fixSchedule, floatSchedule, model.getForwardCurve(forwardCurveName), model);
		cashAnnuity		= cashFunction(baseSwapRate);

		CashSettledPayerSwaption cashPayer;
		CashSettledReceiverSwaption cashReceiver;
		for(int index = 0; index < numberOfEvaluationPoints; index++){
			final double strike 					= baseSwapRate + moneynesss[index] / 10000.0;
			final AnnuityMappingFactory factory	= new AnnuityMappingFactory(fixSchedule, floatSchedule, discountCurveName, forwardCurveName, volatilityCubeName, strike,
					replicationLowerBound, replicationUpperBound, replicationNumberOfEvaluationPoints);
			mapping							= factory.build(type, model);
			cashPayer	 					= new CashSettledPayerSwaption(fixSchedule, floatSchedule, strike, discountCurveName,
					forwardCurveName, volatilityCubeName, type);
			cashReceiver 					= new CashSettledReceiverSwaption(fixSchedule, floatSchedule, strike, discountCurveName,
					forwardCurveName, volatilityCubeName, type);

			valuesCashPay[index] = cashPayer.getValue(fixSchedule.getFixing(0), mapping, model);
			valuesCashRec[index] = cashReceiver.getValue(fixSchedule.getFixing(0), mapping, model);
			valuesPhysPay[index] = valuePayerPSS(strike, model, swapAnnuity, baseSwapRate, fixSchedule, volatilityCubeName);
			valuesPhysRec[index] = valueReceiverPSS(strike, model, swapAnnuity, baseSwapRate, fixSchedule, volatilityCubeName);
		}

	}

	@Test
	public void b_testBasicPiterbarg() throws SolverException, IOException {

		type 										= AnnuityMappingType.BASICPITERBARG;
		model										= buildCube(type);
		final String forwardCurveName						= forwardCurveSingleName;

		swapAnnuity		= SwapAnnuity.getSwapAnnuity(fixSchedule.getFixing(0), fixSchedule, model.getDiscountCurve(discountCurveName), model);
		baseSwapRate	= Swap.getForwardSwapRate(fixSchedule, floatSchedule, model.getForwardCurve(forwardCurveName), model);
		cashAnnuity		= cashFunction(baseSwapRate);

		CashSettledPayerSwaption cashPayer;
		CashSettledReceiverSwaption cashReceiver;
		for(int index = 0; index < numberOfEvaluationPoints; index++){
			final double strike 					= baseSwapRate + moneynesss[index] / 10000.0;
			final AnnuityMappingFactory factory	= new AnnuityMappingFactory(fixSchedule, floatSchedule, discountCurveName, forwardCurveName, volatilityCubeName, strike,
					replicationLowerBound, replicationUpperBound, replicationNumberOfEvaluationPoints);
			mapping							= factory.build(type, model);
			cashPayer	 					= new CashSettledPayerSwaption(fixSchedule, floatSchedule, strike, discountCurveName,
					forwardCurveName, volatilityCubeName, type);
			cashReceiver 					= new CashSettledReceiverSwaption(fixSchedule, floatSchedule, strike, discountCurveName,
					forwardCurveName, volatilityCubeName, type);

			valuesCashPay[index] = cashPayer.getValue(fixSchedule.getFixing(0), mapping, model);
			valuesCashRec[index] = cashReceiver.getValue(fixSchedule.getFixing(0), mapping, model);
			valuesPhysPay[index] = valuePayerPSS(strike, model, swapAnnuity, baseSwapRate, fixSchedule, volatilityCubeName);
			valuesPhysRec[index] = valueReceiverPSS(strike, model, swapAnnuity, baseSwapRate, fixSchedule, volatilityCubeName);
		}

	}

	@Test
	public void c_testMultiPiterbarg() throws SolverException, IOException {

		type										= AnnuityMappingType.MULTIPITERBARG;
		model										= buildCube(type);
		final String forwardCurveName						= forwardCurveMarketName;

		swapAnnuity		= SwapAnnuity.getSwapAnnuity(fixSchedule.getFixing(0), fixSchedule, model.getDiscountCurve(discountCurveName), model);
		baseSwapRate	= Swap.getForwardSwapRate(fixSchedule, floatSchedule, model.getForwardCurve(forwardCurveName), model);
		cashAnnuity		= cashFunction(baseSwapRate);

		CashSettledPayerSwaption cashPayer;
		CashSettledReceiverSwaption cashReceiver;
		for(int index = 0; index < numberOfEvaluationPoints; index++){
			final double strike 					= baseSwapRate + moneynesss[index] / 10000.0;
			final AnnuityMappingFactory factory	= new AnnuityMappingFactory(fixSchedule, floatSchedule, discountCurveName, forwardCurveName, volatilityCubeName, strike,
					replicationLowerBound, replicationUpperBound, replicationNumberOfEvaluationPoints);
			mapping							= factory.build(type, model);
			cashPayer	 					= new CashSettledPayerSwaption(fixSchedule, floatSchedule, strike, discountCurveName,
					forwardCurveName, volatilityCubeName, type);
			cashReceiver 					= new CashSettledReceiverSwaption(fixSchedule, floatSchedule, strike, discountCurveName,
					forwardCurveName, volatilityCubeName, type);

			valuesCashPay[index] = cashPayer.getValue(fixSchedule.getFixing(0), mapping, model);
			valuesCashRec[index] = cashReceiver.getValue(fixSchedule.getFixing(0), mapping, model);
			valuesPhysPay[index] = valuePayerPSS(strike, model, swapAnnuity, baseSwapRate, fixSchedule, volatilityCubeName);
			valuesPhysRec[index] = valueReceiverPSS(strike, model, swapAnnuity, baseSwapRate, fixSchedule, volatilityCubeName);
		}

	}

	@After
	public void gatherOutput(){

		//output from tests
		outputs[0].append("Premium" +"\t"+ type +"\t\t\t");
		outputs[1].append(" Moneyness \t Payer \t Receiver \t PSS Pay \t PSS Rec");
		for(int index = 0; index < numberOfEvaluationPoints; index++) {
			outputs[index+2].append(moneynesss[index] +"\t"+ formatter.format(valuesCashPay[index]) +"\t"+ formatter.format(valuesCashRec[index]) +"\t"+ formatter.format(valuesPhysPay[index]) +"\t"+ formatter.format(valuesPhysRec[index]));
		}

		//convert to implied bachelier volatilities
		final double[] valuesCube = new double[numberOfEvaluationPoints];
		final double optionMaturity 	= fixSchedule.getFixing(0);
		final double termination		= fixSchedule.getPayment(fixSchedule.getNumberOfPeriods()-1);


		outputs[numberOfEvaluationPoints +2].append("Implied Volatility" +"\t"+ type +"\t\t\t");
		outputs[numberOfEvaluationPoints +3].append(" Moneyness \t Payer \t Receiver \t Physical \t Cube ");
		for(int index = 0; index < numberOfEvaluationPoints; index++){
			final double strike = baseSwapRate + moneynesss[index] / 10000.0;
			//			double cashAnnuity =  this.cashAnnuity * model.getVolatilityCube(volatilityCubeName).getValue(model, termination, optionMaturity, strikes[index], QuotingConvention.VOLATILITYNORMAL);
			valuesCashPay[index] = AnalyticFormulas.bachelierOptionImpliedVolatility(baseSwapRate, fixSchedule.getFixing(0), strike, cashAnnuity, valuesCashPay[index]);
			valuesCashRec[index] += (baseSwapRate -strike) *cashAnnuity;
			valuesCashRec[index] = AnalyticFormulas.bachelierOptionImpliedVolatility(baseSwapRate, fixSchedule.getFixing(0), strike, cashAnnuity, valuesCashRec[index]);

			valuesPhysPay[index] = AnalyticFormulas.bachelierOptionImpliedVolatility(baseSwapRate, fixSchedule.getFixing(0), strike, swapAnnuity, valuesPhysPay[index]);
			//			valuesPhysRec[index] = (baseSwapRate -strikes[index]) *swapAnnuity + valuesPhysRec[index];
			//			valuesPhysRec[index] = AnalyticFormulas.bachelierOptionImpliedVolatility(baseSwapRate, schedule.getFixing(0), strikes[index], swapAnnuity, valuesPhysRec[index]);
			valuesCube[index] = model.getVolatilityCube(volatilityCubeName).getValue(termination, optionMaturity, strike, QuotingConvention.VOLATILITYNORMAL);

			outputs[index +numberOfEvaluationPoints+4].append(moneynesss[index] +"\t"+ formatter.format(valuesCashPay[index]) +"\t"+ formatter.format(valuesCashRec[index]) +"\t"+ formatter.format(valuesPhysPay[index]) +"\t"+ formatter.format(valuesCube[index]));
		}

		//make some space between annuity mappings
		for(int index = 0; index < outputs.length; index++) {
			outputs[index].append("\t\t\t");
		}

		for(final double val : valuesCashPay) {
			if(Double.isNaN(val)) {
				Assert.fail();
			}
		}
		for(final double val : valuesCashRec) {
			if(Double.isNaN(val)) {
				Assert.fail();
			}
		}
		for(final double val : valuesPhysPay) {
			if(Double.isNaN(val)) {
				Assert.fail();
			}
		}
		for(final double val : valuesPhysRec) {
			if(Double.isNaN(val)) {
				Assert.fail();
			}
		}
	}

	@AfterClass
	public static void printResults() throws FileNotFoundException{
		final StringBuilder output = new StringBuilder();
		for(int index = 0; index < outputs.length; index++){
			if(index == outputs.length/2) {
				output.append('\n');
			}
			output.append(outputs[index].toString()+'\n');
		}
		System.out.println(output.toString());
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
				0.045,	//correlationDecay,
				0.5,	//iborOisDecorrelation,
				type);

		volatilityCubeName				= type.toString();
		final VolatilityCube cube	= factory.buildShiftedSmileSABRCube(volatilityCubeName, model);

		return model.addVolatilityCube(cube);
	}

	private static double valueReceiverPSS(final double optionStrike, final VolatilityCubeModel model, final double swapAnnuity, final double forwardSwapRate, final Schedule schedule, final String volatilityCubeName){

		return valuePayerPSS(optionStrike, model, swapAnnuity, forwardSwapRate, schedule, volatilityCubeName) - (forwardSwapRate -optionStrike) *swapAnnuity;
	}

	private static double valuePayerPSS(final double optionStrike, final VolatilityCubeModel model, final double swapAnnuity, final double forwardSwapRate, final Schedule schedule, final String volatilityCubeName){
		final double optionMaturity 	= schedule.getFixing(0);
		final double termination		= schedule.getPayment(schedule.getNumberOfPeriods()-1);
		final double volatility		 = model.getVolatilityCube(volatilityCubeName).getValue(termination, optionMaturity, optionStrike, QuotingConvention.VOLATILITYNORMAL);
		return AnalyticFormulas.bachelierOptionValue(forwardSwapRate, volatility, optionMaturity, optionStrike, swapAnnuity );
	}

	//cash function for equidistant tenors
	private static double cashFunction(final double swapRate) {

		final int numberOfPeriods = fixSchedule.getNumberOfPeriods();
		double periodLength = 0.0;
		for(int index = 0; index < numberOfPeriods; index++) {
			periodLength += fixSchedule.getPeriodLength(index);
		}
		periodLength /= fixSchedule.getNumberOfPeriods();

		if(swapRate == 0.0) {
			return numberOfPeriods * periodLength;
		} else {
			return (1 - Math.pow(1 + periodLength * swapRate, - numberOfPeriods)) / swapRate;
		}
	}
}
