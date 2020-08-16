package net.finmath.singleswaprate.calibration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import net.finmath.functions.AnalyticFormulas;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurveFromDiscountCurve;
import net.finmath.marketdata.model.volatilities.SwaptionDataLattice;
import net.finmath.marketdata.products.Swap;
import net.finmath.optimizer.LevenbergMarquardt;
import net.finmath.optimizer.SolverException;
import net.finmath.singleswaprate.Utils;
import net.finmath.singleswaprate.annuitymapping.AnnuityMapping.AnnuityMappingType;
import net.finmath.singleswaprate.model.AnalyticModelWithVolatilityCubes;
import net.finmath.singleswaprate.model.VolatilityCubeModel;
import net.finmath.singleswaprate.model.volatilities.VolatilityCube;
import net.finmath.singleswaprate.products.CashSettledPayerSwaption;
import net.finmath.singleswaprate.products.CashSettledReceiverSwaption;
import net.finmath.time.Schedule;
import net.finmath.time.SchedulePrototype;
import net.finmath.time.businessdaycalendar.BusinessdayCalendar;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingTARGETHolidays;

public class SABRShiftedSmileCalibrationTest {

	private static double testAccuracy				= 0.1;
	private static int calibrationMaxIteration		= 5000;
	private static boolean useLinearInterpolation	= true;

	// selection for single smile calibration test
	private static int singleSmileMaturity	= 120;
	private static int singleSmileTenor		= 180;

	// files
	private static String curveFilePath					= "./src/test/resources/curves";
	private static String discountCurveFileName			= "EUR-EONIA.crv";
	private static String forwardCurveFileName			= "EUR-OIS6M.crv";
	private static String swaptionFilePath				= "./src/test/resources/swaptions";
	private static String payerFileName					= "CashPayerSwaptionPrice.sdl";
	private static String receiverFileName				= "CashReceiverSwaptionPrice.sdl";
	private static String physicalFileName				= "PhysicalSwaptionPriceATM.sdl";

	private final AnnuityMappingType type = AnnuityMappingType.MULTIPITERBARG;

	// cube parameters
	private static double sabrDisplacement 		= 0.25;
	private static double sabrBeta 				= 0.5;
	private static double correlationDecay 		= 0.45;
	private static double iborOisDecorrelation	= 1.0;

	private static LocalDate referenceDate 		= LocalDate.of(2017, 8, 30);

	//setup output to excel
	//	private static StringBuilder output = new StringBuilder();

	private static SchedulePrototype floatMetaSchedule;
	private static SchedulePrototype fixMetaSchedule;

	private static VolatilityCubeModel model;
	private static VolatilityCube cube;
	private static String forwardCurveName;
	private static String discountCurveName;
	private static SwaptionDataLattice payerSwaptions;
	private static SwaptionDataLattice receiverSwaptions;
	private static SwaptionDataLattice physicalSwaptions;

	@BeforeClass
	public static void setup() {

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
				new BusinessdayCalendarExcludingTARGETHolidays(), BusinessdayCalendar.DateRollConvention.FOLLOWING, 365/360.0, 0);

		model = new AnalyticModelWithVolatilityCubes();
		model = (AnalyticModelWithVolatilityCubes) model.addCurve(discountCurve.getName(), discountCurve);
		model = (AnalyticModelWithVolatilityCubes) model.addCurve(forwardCurve.getName(), forwardCurve);
		model = (AnalyticModelWithVolatilityCubes) model.addCurve(forwardDiscountCurve.getName(), forwardDiscountCurve);

		forwardCurveName	= forwardCurve.getName();
		discountCurveName	= discountCurve.getName();

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

		floatMetaSchedule 	= physicalSwaptions.getFloatMetaSchedule();
		fixMetaSchedule		= physicalSwaptions.getFixMetaSchedule();

	}

	@Test
	public void testCalibration() {

		final List<Double> modelPayerValues		= new ArrayList<>();
		final List<Double> marketPayerValues		= new ArrayList<>();
		final List<Double> modelReceiverValues	= new ArrayList<>();
		final List<Double> marketReceiverValues	= new ArrayList<>();

		System.out.println("Running calibration...");
		final long startTime = System.currentTimeMillis();

		final SABRShiftedSmileCalibration calibrator = new SABRShiftedSmileCalibration(referenceDate, payerSwaptions, receiverSwaptions, physicalSwaptions, model,
				sabrDisplacement, sabrBeta, correlationDecay, iborOisDecorrelation);
		calibrator.setCalibrationParameters(calibrationMaxIteration, Runtime.getRuntime().availableProcessors());
		calibrator.setUseLinearInterpolation(useLinearInterpolation);

		try {
			cube = calibrator.build("ShiftedSmileCube");
		} catch (final SolverException e) {
			e.printStackTrace();
		}

		final long endTime = System.currentTimeMillis();

		System.out.println("\nCalibration finished after "+(endTime-startTime)/1000 +"s.");
		System.out.println("Cube calibrated to parameters:");
		System.out.println(cube.getParameters().toString());

		System.out.println("\nValue of CSPayerSwaption\nmoneyness maturity termination | model-value market-value");
		for(final int moneyness : payerSwaptions.getMoneyness()) {
			for(final int maturity : payerSwaptions.getMaturities(moneyness)) {
				for(final int termination : payerSwaptions.getTenors(moneyness, maturity)) {
					final double valueModel	= payerValue(model.addVolatilityCube(cube), maturity, termination, moneyness);
					final double valueMarket	= payerSwaptions.getValue(maturity, termination, moneyness);

					System.out.println(moneyness + "\t" + maturity + "\t" + termination + "\t|\t" + valueModel + "\t" + valueMarket);
					modelPayerValues.add(valueModel);
					marketPayerValues.add(valueMarket);
				}
			}
		}

		System.out.println("\nValue of CSReceiverSwaption\nmoneyness maturity termination | model-value market-value");
		for(final int moneyness : receiverSwaptions.getMoneyness()) {
			for(final int maturity : receiverSwaptions.getMaturities(moneyness)) {
				for(final int termination : receiverSwaptions.getTenors(moneyness, maturity)) {
					final double valueModel	= receiverValue(model.addVolatilityCube(cube), maturity, termination, moneyness);
					final double valueMarket	= receiverSwaptions.getValue(maturity, termination, moneyness);

					System.out.println(moneyness + "\t" + maturity + "\t" + termination + "\t|\t" + valueModel + "\t" + valueMarket);
					modelReceiverValues.add(valueModel);
					marketReceiverValues.add(valueMarket);
				}
			}
		}
		System.out.println("\n\n\n\n");

		for(int index = 0; index < modelPayerValues.size(); index++) {
			Assert.assertEquals(marketPayerValues.get(index), modelPayerValues.get(index), testAccuracy);
		}
		for(int index = 0; index < modelReceiverValues.size(); index++) {
			Assert.assertEquals(marketReceiverValues.get(index), modelReceiverValues.get(index), testAccuracy);
		}
	}

	@Test
	public void testSingleSmile() throws SolverException {

		final double initialBaseVol = 0.01;
		final double initialVolVol = 0.15;
		final double initialRho = 0.3;
		final double[] initialParameters = new double[]{initialBaseVol, initialVolVol, initialRho};

		final Schedule fixSchedule	= fixMetaSchedule.generateSchedule(referenceDate, singleSmileMaturity, singleSmileTenor);
		final Schedule floatSchedule	= floatMetaSchedule.generateSchedule(referenceDate, singleSmileMaturity, singleSmileTenor);

		final double parSwapRate		= Swap.getForwardSwapRate(fixSchedule, floatSchedule, model.getForwardCurve(forwardCurveName), model);
		final double optionMaturity	= fixSchedule.getFixing(0);

		final List<Double> marketVolatilitiesList	= new ArrayList<>();
		final List<Double> marketStrikesList		= new ArrayList<>();

		final SwaptionDataLattice shiftedLattice = Utils.shiftCashToPhysicalSmile(model, physicalSwaptions, payerSwaptions, receiverSwaptions);
		for(final int moneyness : shiftedLattice.getMoneyness()) {
			if(shiftedLattice.containsEntryFor(singleSmileMaturity, singleSmileTenor, moneyness)) {
				marketVolatilitiesList.add(shiftedLattice.getValue(singleSmileMaturity, singleSmileTenor, moneyness));
				marketStrikesList.add(parSwapRate + moneyness * 0.0001);
			}
		}

		final double[] marketVolatilities	= marketVolatilitiesList.stream().mapToDouble(Double::doubleValue).toArray();
		final double[] marketStrikes		= marketStrikesList.stream().mapToDouble(Double::doubleValue).toArray();

		final LevenbergMarquardt optimizer = new LevenbergMarquardt(
				initialParameters,
				marketVolatilities,
				calibrationMaxIteration,
				Runtime.getRuntime().availableProcessors()
				) {

			private static final long serialVersionUID = -109907086732741286L;

			@Override
			public void setValues(final double[] parameters, final double[] values) {

				// making sure that volatility stays above 0
				parameters[0] = Math.max(parameters[0], 0);
				parameters[1] = Math.max(parameters[1], 0);
				parameters[2] = Math.max(Math.min(parameters[2], 1), -1);

				for(int i = 0; i < marketStrikes.length; i++) {
					values[i] = AnalyticFormulas.sabrBerestyckiNormalVolatilityApproximation(parameters[0] /* alpha */, sabrBeta /* beta */,
							parameters[2] /* rho */, parameters[1] /* nu */, sabrDisplacement /* displacement */, parSwapRate, marketStrikes[i], optionMaturity);

				}
			}
		};

		optimizer.run();
		System.out.println("Optimizer finished after " +optimizer.getIterations() +" iterations with mean error " + optimizer.getRootMeanSquaredError());

		final double[] calibratedParameters = optimizer.getBestFitParameters();
		System.out.println("Given parameters:");
		System.out.println("beta= "+sabrBeta);
		System.out.println("displacement= " +sabrDisplacement);
		System.out.println("par swap rate= " +parSwapRate);
		System.out.println("\nCalibrated following parameters:");
		System.out.println("alpha= "+calibratedParameters[0]);
		System.out.println("nu= "+ calibratedParameters[1]);
		System.out.println("rho= "+ calibratedParameters[2]);

		final List<Double> modelValues	= new ArrayList<>();
		final List<Double> marketValues	= new ArrayList<>();
		System.out.println("\n\nSmile at "+singleSmileMaturity+"M"+singleSmileTenor+"M");
		System.out.println("Moneyness\tSABR\tMarket");
		for(final int moneyness : shiftedLattice.getMoneyness()) {
			if(shiftedLattice.containsEntryFor(singleSmileMaturity, singleSmileTenor, moneyness)) {
				final double marketValue	= shiftedLattice.getValue(singleSmileMaturity, singleSmileTenor, moneyness);
				final double sabrValue	= AnalyticFormulas.sabrBerestyckiNormalVolatilityApproximation(
						calibratedParameters[0],
						sabrBeta,
						calibratedParameters[2],
						calibratedParameters[1],
						sabrDisplacement,
						parSwapRate,
						parSwapRate + moneyness * 0.0001, //optionStrike
						optionMaturity);
				modelValues.add(sabrValue);
				marketValues.add(marketValue);
				System.out.println(moneyness + "\t" + sabrValue + "\t" + marketValue);
			}
		}
		System.out.println("\n\n\n\n");

		for(int i = 0; i < marketValues.size(); i++) {
			Assert.assertEquals(marketValues.get(i), modelValues.get(i), testAccuracy);
		}
	}

	private double payerValue(final VolatilityCubeModel model, final int maturity, final int termination, final int moneyness) {

		final Schedule fixSchedule = fixMetaSchedule.generateSchedule(referenceDate, maturity, termination);
		final Schedule floatSchedule = floatMetaSchedule.generateSchedule(referenceDate, maturity, termination);
		final double forwardSwapRate	= Swap.getForwardSwapRate(fixSchedule, floatSchedule, model.getForwardCurve(forwardCurveName), model);
		final double strike = forwardSwapRate + moneyness/10000.0;

		final CashSettledPayerSwaption css = new CashSettledPayerSwaption(fixSchedule, floatSchedule, strike, discountCurveName, forwardCurveName,
				cube.getName(), type);
		return css.getValue(floatSchedule.getFixing(0), model);
	}

	private double receiverValue(final VolatilityCubeModel model, final int maturity, final int termination, final int moneyness) {

		final Schedule fixSchedule = fixMetaSchedule.generateSchedule(referenceDate, maturity, termination);
		final Schedule floatSchedule = floatMetaSchedule.generateSchedule(referenceDate, maturity, termination);
		final double forwardSwapRate	= Swap.getForwardSwapRate(fixSchedule, floatSchedule, model.getForwardCurve(forwardCurveName), model);
		final double strike = forwardSwapRate - moneyness/10000.0;

		final CashSettledReceiverSwaption css = new CashSettledReceiverSwaption(fixSchedule, floatSchedule, strike, discountCurveName, forwardCurveName,
				cube.getName(), type);
		return css.getValue(floatSchedule.getFixing(0), model);
	}
}
