package net.finmath.singleswaprate.model.volatilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurveFromDiscountCurve;
import net.finmath.marketdata.model.volatilities.SwaptionDataLattice;
import net.finmath.marketdata.model.volatilities.VolatilitySurface.QuotingConvention;
import net.finmath.marketdata.products.Swap;
import net.finmath.singleswaprate.Utils;
import net.finmath.singleswaprate.data.DataTable;
import net.finmath.singleswaprate.data.DataTable.TableConvention;
import net.finmath.singleswaprate.data.DataTableLight;
import net.finmath.singleswaprate.model.AnalyticModelWithVolatilityCubes;
import net.finmath.singleswaprate.model.VolatilityCubeModel;
import net.finmath.time.Schedule;
import net.finmath.time.SchedulePrototype;
import net.finmath.time.businessdaycalendar.BusinessdayCalendar;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingWeekends;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)

public class SABRVolatilityCubeSharedParametersTest {

	private static double testAccuracy = 0.003;

	// files
	private static String curveFilePath					= "./src/test/resources/curves";
	private static String discountCurveFileName			= "EUR-EONIA.crv";
	private static String forwardCurveFileName			= "EUR-OIS6M.crv";
	private static String swaptionFilePath				= "./src/test/resources/swaptions";
	private static String payerFileName					= "CashPayerSwaptionPrice.sdl";
	private static String receiverFileName				= "CashReceiverSwaptionPrice.sdl";
	private static String physicalFileName				= "PhysicalSwaptionPriceATM.sdl";

	// cube parameters
	private static double sabrDisplacement 		= 0.25;
	private static double sabrBeta 				= 0.5;
	private static double sabrRho				= 0.45;
	private static double sabrVolvol			= 0.40;
	private static double correlationDecay 		= 0.045;
	private static double iborOisDecorrelation	= 1.0;

	private static String cubeName = "TestCube";
	private static LocalDate referenceDate = LocalDate.of(2017, 8, 30);

	private static SchedulePrototype floatMetaSchedule;
	private static SchedulePrototype fixMetaSchedule;

	private static VolatilityCubeModel model;
	private static String forwardCurveName;
	private static VolatilityCube cube;
	private static SwaptionDataLattice payerSwaptions;
	private static SwaptionDataLattice receiverSwaptions;
	private static SwaptionDataLattice physicalSwaptions;
	private static SwaptionDataLattice physicalVolatilities;

	private static List<Double> differenceList;

	//setup output to excel
	private static StringBuilder output = new StringBuilder();

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
				new BusinessdayCalendarExcludingWeekends(), BusinessdayCalendar.DateRollConvention.FOLLOWING, 1, 0);

		model = new AnalyticModelWithVolatilityCubes();
		model = (AnalyticModelWithVolatilityCubes) model.addCurve(discountCurve.getName(), discountCurve);
		model = (AnalyticModelWithVolatilityCubes) model.addCurve(forwardCurve.getName(), forwardCurve);
		model = (AnalyticModelWithVolatilityCubes) model.addCurve(forwardDiscountCurve.getName(), forwardDiscountCurve);

		forwardCurveName	= forwardCurve.getName();

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

		physicalVolatilities = Utils.shiftCashToPhysicalSmile(model, physicalSwaptions, payerSwaptions, receiverSwaptions);

		cube = makeVolatilityCube();
	}

	@Before
	public void clearList() {
		differenceList = new ArrayList<>();
	}

	@Test
	public void a_cubeATM() {

		final ArrayList<Integer> maturities			= new ArrayList<>();
		final ArrayList<Integer> terminations			= new ArrayList<>();
		final ArrayList<Double> volatilitiesModel		= new ArrayList<>();
		final ArrayList<Double> volatilitiesMarket	= new ArrayList<>();

		for(final int maturity : physicalVolatilities.getMaturities(0)) {
			for(final int termination : physicalVolatilities.getTenors(0, maturity)) {

				final LocalDate maturityDate = referenceDate.plusMonths(maturity);
				final LocalDate terminationDate = maturityDate.plusMonths(termination);

				final Schedule floatSchedule = floatMetaSchedule.generateSchedule(referenceDate, maturityDate, terminationDate);
				final Schedule fixSchedule = fixMetaSchedule.generateSchedule(referenceDate, maturityDate, terminationDate);
				final double swapRate = Swap.getForwardSwapRate(fixSchedule, floatSchedule, model.getForwardCurve(forwardCurveName), model);

				try {
					final double volatility = cube.getValue(model, fixSchedule.getPayment(fixSchedule.getNumberOfPeriods()-1), fixSchedule.getFixing(0), swapRate,
							QuotingConvention.VOLATILITYNORMAL);
					maturities.add(maturity);
					terminations.add(termination);
					volatilitiesModel.add(volatility);
					volatilitiesMarket.add(physicalVolatilities.getValue(maturity, termination, 0));
				} catch (final Exception e) {
					maturities.add(maturity);
					terminations.add(termination);
					volatilitiesModel.add(0.0);
					volatilitiesMarket.add(physicalVolatilities.getValue(maturity, termination, 0));
				}
			}
		}

		final DataTableLight modelTable	= new DataTableLight("Volatilites-Model", TableConvention.MONTHS, maturities, terminations, volatilitiesModel);
		final DataTableLight marketTable	= new DataTableLight("Volatilites-Market", TableConvention.MONTHS, maturities, terminations, volatilitiesMarket);
		output.append(marketTable.toString()+"\n");
		output.append("\n"+modelTable.toString()+"\n\n\n\n");

	}

	@Test
	public void b_strikeSlices() {

		for(final int moneyness : physicalVolatilities.getMoneyness()) {

			final ArrayList<Integer> marketMaturities = new ArrayList<>();
			final ArrayList<Integer> marketTerminations = new ArrayList<>();
			final ArrayList<Double> marketVolatilities = new ArrayList<>();

			final ArrayList<Integer> modelMaturities = new ArrayList<>();
			final ArrayList<Integer> modelTerminations = new ArrayList<>();
			final ArrayList<Double> modelVolatilities = new ArrayList<>();

			final ArrayList<Double> differenceList = new ArrayList<>();

			for(final int maturity : physicalVolatilities.getMaturities(moneyness)) {
				for(final int termination : physicalVolatilities.getTenors(moneyness, maturity)) {

					final LocalDate maturityDate = referenceDate.plusMonths(maturity);
					final LocalDate terminationDate = maturityDate.plusMonths(termination);

					final Schedule floatSchedule = floatMetaSchedule.generateSchedule(referenceDate, maturityDate, terminationDate);
					final Schedule fixSchedule = fixMetaSchedule.generateSchedule(referenceDate, maturityDate, terminationDate);
					final double swapRate = Swap.getForwardSwapRate(fixSchedule, floatSchedule, model.getForwardCurve(forwardCurveName), model);

					double volatilityModel	= 0;
					double volatilityMarket	= 0;
					try {
						volatilityModel = cube.getValue(model, fixSchedule.getPayment(fixSchedule.getNumberOfPeriods()-1), fixSchedule.getFixing(0),
								swapRate+moneyness/10000.0, QuotingConvention.VOLATILITYNORMAL);
						modelMaturities.add(maturity);
						modelTerminations.add(termination);
						modelVolatilities.add(volatilityModel);
					} catch (final Exception e) {
						modelMaturities.add(maturity);
						modelTerminations.add(termination);
						modelVolatilities.add(0.0);
					}

					try{
						volatilityMarket = physicalVolatilities.getValue(maturity, termination, moneyness);
						marketMaturities.add(maturity);
						marketTerminations.add(termination);
						marketVolatilities.add(volatilityMarket);
					} catch (final Exception e) {}

					if(volatilityModel != 0 && volatilityMarket != 0) {
						differenceList.add(volatilityModel - volatilityMarket);
					}
				}
			}

			final DataTable marketTable = new DataTableLight("Volatilities-Market-atMoneyness"+moneyness, TableConvention.MONTHS, marketMaturities, marketTerminations, marketVolatilities);
			final DataTable modelTable = new DataTableLight("Volatilites-Model-atMoneyness"+moneyness, TableConvention.MONTHS, modelMaturities, modelTerminations, modelVolatilities);
			output.append(marketTable.toString()+"\n\n");
			output.append(modelTable.toString()+"\n\n");

			final double maxDiff	= differenceList.stream().mapToDouble(a -> Math.abs(a)).max().getAsDouble();
			final double avrgDiff	= differenceList.stream().mapToDouble(a -> Math.abs(a)).average().getAsDouble();
			output.append("Maximal difference: " + maxDiff + ", Average difference: " + avrgDiff +"\n\n\n\n");
			SABRVolatilityCubeSharedParametersTest.differenceList.addAll(differenceList);
		}

	}

	@After
	public void checkDifferences() {
		for(final double difference : differenceList) {
			Assert.assertEquals(0, difference, testAccuracy);
		}
	}

	@AfterClass
	public static void print() throws FileNotFoundException {

		System.out.println(output.toString());

	}

	private static VolatilityCube makeVolatilityCube() {

		return SABRVolatilityCubeParallelFactory.createSABRVolatilityCubeParallel(cubeName, referenceDate, fixMetaSchedule, floatMetaSchedule,
				sabrDisplacement, sabrBeta, sabrRho, sabrVolvol, correlationDecay, iborOisDecorrelation, physicalSwaptions, model, forwardCurveName);
	}
}
