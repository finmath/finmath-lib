package net.finmath.singleswaprate.annuitymapping;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.time.LocalDate;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurveFromDiscountCurve;
import net.finmath.marketdata.products.Swap;
import net.finmath.marketdata.products.SwapAnnuity;
import net.finmath.singleswaprate.model.AnalyticModelWithVolatilityCubes;
import net.finmath.singleswaprate.model.VolatilityCubeModel;
import net.finmath.singleswaprate.model.volatilities.SABRVolatilityCubeSingleSmile;
import net.finmath.singleswaprate.model.volatilities.VolatilityCube;
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
 * Testing annuity mappings at atm and their smiles plus derivatives.
 *
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)

public class AnnuityMappingTest {

	private static String curveFilePath				= "./src/test/resources/curves";
	private static String discountCurveFileName		= "EUR-EONIA.crv";
	private static String forwardCurveFileName		= "EUR-OIS6M.crv";

	private static double correlationDecay = 1.0;
	private static double iborOisDecorrelation = 0.5;
	private static double[] testRanges = new double[]{-0.1, 1.0};
	private static int numberOfEvaluationPoints = 50;
	private static double step = (testRanges[1]-testRanges[0])/numberOfEvaluationPoints;

	private static MultiPiterbargAnnuityMapping multiPiterbarg;
	private static BasicPiterbargAnnuityMapping basicPiterbarg;
	private static SimplifiedLinearAnnuityMapping simpleMapping;

	private static double forwardAnnuity;
	private static double discountCurveSwapRate;
	private static double forwardCurveSwapRate;

	private static Schedule fixSchedule;
	private static Schedule floatSchedule;
	private static VolatilityCubeModel model;

	private static StringBuilder output = new StringBuilder();

	@BeforeClass
	public static void create(){

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

		ForwardCurve forwardFromDiscountCurve = null;
		forwardFromDiscountCurve = new ForwardCurveFromDiscountCurve(discountCurve.getName(), referenceDate, "6M");

		forwardAnnuity	= SwapAnnuity.getSwapAnnuity(fixSchedule.getFixing(0), fixSchedule, discountCurve, null);

		discountCurveSwapRate	= Swap.getForwardSwapRate(fixSchedule, floatSchedule, forwardFromDiscountCurve, model);
		forwardCurveSwapRate	= Swap.getForwardSwapRate(fixSchedule, floatSchedule, forwardCurve, model);

		//Cube
		final String cubeName = "VOLA";
		final VolatilityCube cube = createVolatilityCube(cubeName, referenceDate, forwardCurveSwapRate);

		model = model.addVolatilityCube(cubeName, cube);

		System.out.println("Projected forward annuity is \t" + forwardAnnuity +
				"\t(Evaluated by adjusting the annuity given the current information with the discount at time of fixing.)" + "\n");

		System.out.println("Swap rate from discount curve is  \t" + discountCurveSwapRate);
		System.out.println("Swap rate from forward curve is \t" +forwardCurveSwapRate +'\n');

		basicPiterbarg		= new BasicPiterbargAnnuityMapping(fixSchedule, floatSchedule, model, discountCurve.getName(), cubeName);
		multiPiterbarg		= new MultiPiterbargAnnuityMapping(fixSchedule, floatSchedule, model, discountCurve.getName(), forwardCurve.getName(), cubeName);
		simpleMapping		= new SimplifiedLinearAnnuityMapping(fixSchedule, floatSchedule, model, discountCurve.getName());

	}

	@Test
	public void a_testMappings(){

		final double basicResult = basicPiterbarg.getValue(discountCurveSwapRate);
		final double multiResult = multiPiterbarg.getValue(forwardCurveSwapRate);
		final double simpleResult = simpleMapping.getValue(discountCurveSwapRate);

		System.out.println("Annuity at par swap rate via BasicPiterbarg is \t" + forwardAnnuity /basicResult);
		System.out.println("Annuity at par swap rate via MultiPiterbarg is \t" + forwardAnnuity /multiResult);
		System.out.println("Annuity at par swap rate via SimplifiedAM is \t" + forwardAnnuity /simpleResult +"\n\n\n");

		Assert.assertEquals(1, basicResult, 0.2);
		Assert.assertEquals(1, multiResult, 0.2);
		Assert.assertEquals(1, simpleResult, 0.2);

	}

	@Test
	public void b_testSeq() {
		output.append("Value of annuity mappings: \t\t (note that the return is the fraction of the forward annuity evaluated now and at time of fixing)\n");
		output.append("swapRate\tsimpleAM\tbasicPiterbarg\tmultiPiterbarg\n");
		for(double rate = testRanges[0]; rate < testRanges[1]; rate += step){
			final double basicResult = basicPiterbarg.getValue(rate);
			final double multiResult = multiPiterbarg.getValue(rate);
			final double simpleResult = simpleMapping.getValue(rate);

			output.append(rate +"\t"+ simpleResult +"\t" + basicResult +"\t"+ multiResult +'\n');
		}
		output.append("\n\n");
	}

	@Test
	public void c_testFirstDerivative() {
		output.append("First derivative of annuity mappings: \n stike\tsimpleAM\tbasicPiterbarg\tmultiPiterbarg\n");
		for(double rate = testRanges[0]; rate < testRanges[1]; rate += step){
			output.append(rate +"\t"+
					simpleMapping.getFirstDerivative(rate) +"\t" +
					basicPiterbarg.getFirstDerivative(rate) +"\t"+
					multiPiterbarg.getFirstDerivative(rate) +'\n');
		}
		output.append("\n\n");
	}

	@Test
	public void d_testSecondDerivative() {
		output.append("Second derivative of annuity mappings: \n stike\tsimpleAM\tbasicPiterbarg\tmultiPiterbarg\n");
		for(double rate = testRanges[0]; rate < testRanges[1]; rate += step){
			output.append(rate +"\t"+ simpleMapping.getSecondDerivative(rate) +"\t" + basicPiterbarg.getSecondDerivative(rate) +"\t"+
					multiPiterbarg.getSecondDerivative(rate) +'\n');
		}
		output.append("\n\n");
	}

	@AfterClass
	public static void manageOutput() throws FileNotFoundException {
		System.out.println(output.toString());
	}

	//creating a dumb volatility cube for tests
	private static VolatilityCube createVolatilityCube(final String name, final LocalDate referenceDate, final double underlying){
		final double sabrAlpha		= 0.006459764701438869;
		final double sabrBeta			= 0.0;
		final double sabrRho			= 0.5025290759225008;
		final double sabrNu			= 0.14614712329776228;
		final double sabrDisplacement	= -0.03284122992284627;

		final SABRVolatilityCubeSingleSmile cube = new SABRVolatilityCubeSingleSmile(name, referenceDate, underlying, sabrAlpha, sabrBeta, sabrRho, sabrNu, sabrDisplacement,
				correlationDecay, iborOisDecorrelation);
		return cube;
	}

}
