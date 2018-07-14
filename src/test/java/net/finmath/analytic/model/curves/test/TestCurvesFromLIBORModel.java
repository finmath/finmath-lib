package net.finmath.analytic.model.curves.test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.analytic.model.AnalyticModel;
import net.finmath.analytic.model.curves.CurveInterface;
import net.finmath.analytic.products.AbstractAnalyticProduct;
import net.finmath.analytic.products.Swap;
import net.finmath.analytic.products.SwapLeg;
import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.AbstractRandomVariableFactory;
import net.finmath.montecarlo.BrownianMotionInterface;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.conditionalexpectation.MonteCarloConditionalExpectationRegression;
import net.finmath.montecarlo.interestrate.LIBORMarketModel;
import net.finmath.montecarlo.interestrate.LIBORMarketModelInterface;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulation;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORCorrelationModelExponentialDecay;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORCovarianceModelFromVolatilityAndCorrelation;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORVolatilityModel;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORVolatilityModelFourParameterExponentialForm;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORVolatilityModelFromGivenMatrix;
import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.montecarlo.interestrate.products.components.AbstractNotional;
import net.finmath.montecarlo.interestrate.products.components.Notional;
import net.finmath.montecarlo.interestrate.products.indices.AbstractIndex;
import net.finmath.montecarlo.interestrate.products.indices.LIBORIndex;
import net.finmath.montecarlo.process.ProcessEulerScheme;
import net.finmath.stochastic.ConditionalExpectationEstimatorInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.ScheduleGenerator;
import net.finmath.time.ScheduleInterface;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingTARGETHolidays;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarInterface;

public class TestCurvesFromLIBORModel {
	private static final int numberOfPaths		= 1000;
	private static final int numberOfFactors	= 1;

	static LIBORVolatilityModel volatilityModel;


	@Test
	/*
	 * We value a forward starting swap with LMM and using curves built from the LMM. The curves' time 0 is the forward starting date of the swap.
	 */
	public void testStochasticCurves() throws CalculationException{
		// Create Random Variable Factory
		AbstractRandomVariableFactory randomVariableFactory = new RandomVariableFactory();

		int maturityInYears = 5;
		int forwardStartTimeInYears = 0;
		// Create Analytic Swap
		AbstractAnalyticProduct swapAnalytic = createSwapAnalytic(maturityInYears,forwardStartTimeInYears);
		// Create Monte Carlo Swap
		AbstractLIBORMonteCarloProduct swapMonteCarlo = createSwap(maturityInYears,forwardStartTimeInYears,randomVariableFactory);

		// Create a Libor market model
		LIBORModelMonteCarloSimulationInterface liborMarketModel = createLIBORMarketModel(randomVariableFactory,
				numberOfPaths,
				numberOfFactors,
				//(ForwardCurve)curves.getModel().getForwardCurve("forwardCurve"),
				0.0 /* Correlation */);
		double evaluationTime = forwardStartTimeInYears;

		int timeIndex = liborMarketModel.getTimeIndex(evaluationTime);
		// Get all Libors at timeIndex which are not yet fixed (others null) and times for the timeDiscretization of the curves
		ArrayList<RandomVariableInterface> liborsAtTimeIndex = new ArrayList<>();
		int firstLiborIndex = liborMarketModel.getLiborPeriodDiscretization().getTimeIndexNearestGreaterOrEqual(evaluationTime);
		double firstLiborTime = liborMarketModel.getLiborPeriodDiscretization().getTime(firstLiborIndex);
		if(firstLiborTime>evaluationTime) {
			liborsAtTimeIndex.add(liborMarketModel.getLIBOR(evaluationTime, evaluationTime, firstLiborTime));
		}
		for(int i=firstLiborIndex;i<liborMarketModel.getNumberOfLibors();i++) {
			liborsAtTimeIndex.add(liborMarketModel.getLIBOR(timeIndex,i));
		}
		//times[times.length-1]= model.getLiborPeriodDiscretization().getTime(model.getNumberOfLibors())-evaluationTime;
		RandomVariableInterface[] libors = liborsAtTimeIndex.toArray(new RandomVariableInterface[liborsAtTimeIndex.size()]);
		// Create conditional expectation operator
		ArrayList<RandomVariableInterface> basisFunctions = getRegressionBasisFunctions(libors);
		ConditionalExpectationEstimatorInterface conditionalExpectationOperator = new MonteCarloConditionalExpectationRegression(basisFunctions.toArray(new RandomVariableInterface[0]));
		// Get value with Monte Carlo
		double valueMonteCarlo = swapMonteCarlo.getValue(evaluationTime, liborMarketModel).getConditionalExpectation(conditionalExpectationOperator).getAverage();

		// Create Forward Curve from the LIBORMonteCarloModel
		net.finmath.analytic.model.curves.ForwardCurve forwardCurve = net.finmath.analytic.model.curves.ForwardCurve.createForwardCurveFromMonteCarloLiborModel("forwardCurve",liborMarketModel, evaluationTime);
		// Get value of analytic swap
		net.finmath.analytic.model.curves.DiscountCurveInterface discountCurve = new net.finmath.analytic.model.curves.DiscountCurveFromForwardCurve(net.finmath.analytic.model.curves.ForwardCurve.createForwardCurveFromMonteCarloLiborModel(forwardCurve.getName(), liborMarketModel, 0));
		//net.finmath.analytic.model.curves.DiscountCurve.createDiscountCurveFromMonteCarloLiborModel("forwardCurve",liborMarketModel, evaluationTime);

		double valueWithCurves = swapAnalytic.getValue(0.0, new AnalyticModel(randomVariableFactory, new CurveInterface[]{forwardCurve,discountCurve})).getAverage();

		System.out.println("" + valueMonteCarlo + "\t" + valueWithCurves);

		Assert.assertEquals(valueMonteCarlo,valueWithCurves,1E-4); //True if forwardStartTimeInYears = 0;
	}

	private static ArrayList<RandomVariableInterface> getRegressionBasisFunctions(RandomVariableInterface[] libors) {
		ArrayList<RandomVariableInterface> basisFunctions = new ArrayList<>();

		// Create basis functions - here: 1, L
		for(int liborIndex=0; liborIndex<libors.length;liborIndex++){
			for(int powerOfRegressionMonomial=0; powerOfRegressionMonomial<=1; powerOfRegressionMonomial++) {
				basisFunctions.add(libors[liborIndex].pow(powerOfRegressionMonomial));
			}
		}
		return basisFunctions;
	}


	public static  LIBORModelMonteCarloSimulationInterface createLIBORMarketModel(
			AbstractRandomVariableFactory randomVariableFactory,
			int numberOfPaths, int numberOfFactors, /*ForwardCurve forwardCurve,*/ double correlationDecayParam) throws CalculationException {

		/*
		 * Create the libor tenor structure and the initial values
		 */
		double liborPeriodLength	= 0.5;
		double liborRateTimeHorzion	= 30.0;
		TimeDiscretization liborPeriodDiscretization = new TimeDiscretization(0.0, (int) (liborRateTimeHorzion / liborPeriodLength), liborPeriodLength);

		LocalDate referenceDate = LocalDate.of(2017, 8, 20);

		// Create the forward curve (initial value of the LIBOR market model). This curve is still double!
		net.finmath.marketdata.model.curves.DiscountCurve discountCurve = net.finmath.marketdata.model.curves.DiscountCurve.createDiscountCurveFromZeroRates(
				"discountCurve"								/* name of the curve */,
				new double[] {0.5 , 1.0, 2.0, 5.0, 30.0}	/* fixings of the forward */,
				new double[] {0.01, 0.02, 0.02, 0.022, 0.025}	/* zero rates */
				);

		net.finmath.marketdata.model.curves.ForwardCurveInterface forwardCurve = new net.finmath.marketdata.model.curves.ForwardCurveFromDiscountCurve(discountCurve.getName(), referenceDate, "6M");
		//		ForwardCurve forwardCurve = ForwardCurve.createForwardCurveFromForwards(
		//				"forwardCurve"								/* name of the curve */,
		//				new double[] {0.5 , 1.0, 2.0, 5.0, 30.0}	/* fixings of the forward */,
		//				new double[] {0.01, 0.02, 0.02, 0.03, 0.04}	/* forwards */,
		//				liborPeriodLength							/* tenor / period length */
		//				);


		/*
		 * Create a simulation time discretization
		 */
		double lastTime	= 30.0;
		double dt		= 0.125;

		TimeDiscretization timeDiscretization = new TimeDiscretization(0.0, (int) (lastTime / dt), dt);

		/*
		 * Create a volatility structure v[i][j] = sigma_j(t_i)
		 */
		double a = 0.0 / 20.0, b = 0.0, c = 0.25, d = 0.3 / 20.0 / 2.0;
		//LIBORVolatilityModel volatilityModel = new LIBORVolatilityModelFourParameterExponentialFormIntegrated(timeDiscretization, liborPeriodDiscretization, a, b, c, d, false);
		volatilityModel = new LIBORVolatilityModelFourParameterExponentialForm(randomVariableFactory, timeDiscretization, liborPeriodDiscretization, a, b, c, d, false);
		double[][] volatilityMatrix = new double[timeDiscretization.getNumberOfTimeSteps()][liborPeriodDiscretization.getNumberOfTimeSteps()];
		for(int timeIndex=0; timeIndex<timeDiscretization.getNumberOfTimeSteps(); timeIndex++) {
			Arrays.fill(volatilityMatrix[timeIndex], d);
		}
		volatilityModel = new LIBORVolatilityModelFromGivenMatrix(randomVariableFactory, timeDiscretization, liborPeriodDiscretization, volatilityMatrix);

		/*
		 * Create a correlation model rho_{i,j} = exp(-a * abs(T_i-T_j))
		 */
		LIBORCorrelationModelExponentialDecay correlationModel = new LIBORCorrelationModelExponentialDecay(
				timeDiscretization, liborPeriodDiscretization, numberOfFactors,
				correlationDecayParam);


		/*
		 * Combine volatility model and correlation model to a covariance model
		 */
		LIBORCovarianceModelFromVolatilityAndCorrelation covarianceModel =
				new LIBORCovarianceModelFromVolatilityAndCorrelation(timeDiscretization,
						liborPeriodDiscretization, volatilityModel, correlationModel);

		// Set model properties
		Map<String, String> properties = new HashMap<>();

		// Choose the simulation measure
		properties.put("measure", LIBORMarketModel.Measure.SPOT.name());

		// Choose log normal model
		properties.put("stateSpace", LIBORMarketModel.StateSpace.LOGNORMAL.name());

		// Empty array of calibration items - hence, model will use given covariance
		LIBORMarketModel.CalibrationItem[] calibrationItems = new LIBORMarketModel.CalibrationItem[0];

		/*
		 * Create corresponding LIBOR Market Model
		 */
		LIBORMarketModelInterface liborMarketModel = new LIBORMarketModel(liborPeriodDiscretization, new net.finmath.marketdata.model.AnalyticModel(new net.finmath.marketdata.model.curves.CurveInterface[]{forwardCurve, discountCurve}), forwardCurve, discountCurve, randomVariableFactory, covarianceModel, calibrationItems, properties);

		BrownianMotionInterface brownianMotion = new net.finmath.montecarlo.BrownianMotion(timeDiscretization, numberOfFactors, numberOfPaths, 3141 /* seed */);

		ProcessEulerScheme process = new ProcessEulerScheme(brownianMotion, ProcessEulerScheme.Scheme.EULER_FUNCTIONAL);

		return new LIBORModelMonteCarloSimulation(liborMarketModel, process);
	}


	public static AbstractLIBORMonteCarloProduct createSwap(int maturityInYears, int forwardStartTimeInYears, AbstractRandomVariableFactory factory){


		//1)   Construct payer and receiver leg
		//1.1) Generate a schedule
		// 1.1.1) Set reference Date
		Calendar calRef = Calendar.getInstance();
		calRef.set(Calendar.YEAR, 2017);
		calRef.set(Calendar.MONTH, Calendar.AUGUST);
		calRef.set(Calendar.DAY_OF_MONTH, 20);
		Date referenceDate = calRef.getTime();

		// 1.1.2) Set Start Date
		Calendar calStart = Calendar.getInstance();
		calStart.set(Calendar.YEAR, 2017+forwardStartTimeInYears);
		calStart.set(Calendar.MONTH, Calendar.AUGUST);
		calStart.set(Calendar.DAY_OF_MONTH, 20);
		Date startDate = calStart.getTime();
		//Maturity Date prepare calendar
		Calendar calMat = calStart;

		//1.1.3) Set further schedule parameters
		int fixingOffsetDays = 0;
		int paymentOffsetDays = 1; //error if = 0;
		String shortPeriodConvention = "first";
		BusinessdayCalendarInterface businessdayCalendar = new BusinessdayCalendarExcludingTARGETHolidays();
		String		frequency = "semiannual";
		String		daycountConvention = "act/365";

		calMat.add(Calendar.YEAR, maturityInYears);
		Date maturityDate = calMat.getTime();

		ScheduleInterface schedulePayer = ScheduleGenerator.createScheduleFromConventions(
				referenceDate,
				startDate,
				maturityDate,
				frequency,
				daycountConvention,
				shortPeriodConvention,
				"modified_following", businessdayCalendar, fixingOffsetDays, paymentOffsetDays);

		ScheduleInterface scheduleReceiver = schedulePayer;

		//Create Monte-Carlo payer leg (float)
		AbstractNotional notional = new Notional(1.0); // equal Notional as for analytic Swap.
		AbstractIndex index = new LIBORIndex(0.0, 0.5);
		double spread = 0.0;
		net.finmath.montecarlo.interestrate.products.SwapLeg leg =
				new net.finmath.montecarlo.interestrate.products.SwapLeg(schedulePayer, notional, index, spread, false /* isNotionalExchanged */);

		//Create Monte-Carlo receiver leg (fixed)
		AbstractNotional notionalF = notional;
		AbstractIndex indexF = null;
		double spreadF = 0.01;
		net.finmath.montecarlo.interestrate.products.SwapLeg legF =
				new net.finmath.montecarlo.interestrate.products.SwapLeg(scheduleReceiver, notionalF, indexF, spreadF, false /* isNotionalExchanged */);

		return new net.finmath.montecarlo.interestrate.products.Swap(legF,leg);

	}




	public static AbstractAnalyticProduct createSwapAnalytic(int maturityInYears, int forwardStartTimeInYears){

		//1)   Construct payer and receiver leg
		//1.1) Generate a schedule
		// 1.1.1) Set reference Date
		Calendar calRef = Calendar.getInstance();
		calRef.set(Calendar.YEAR, 2017);
		calRef.set(Calendar.MONTH, Calendar.AUGUST);
		calRef.set(Calendar.DAY_OF_MONTH, 20);
		Date referenceDate = calRef.getTime();

		// 1.1.2) Set Start Date
		Calendar calStart = Calendar.getInstance();
		calStart.set(Calendar.YEAR, 2017+forwardStartTimeInYears);
		calStart.set(Calendar.MONTH, Calendar.AUGUST);
		calStart.set(Calendar.DAY_OF_MONTH, 20);
		Date startDate = calStart.getTime();
		//Maturity Date prepare calendar
		Calendar calMat = calStart;

		//1.1.3) Set further schedule parameters
		int fixingOffsetDays = 0;
		int paymentOffsetDays = 1; //error if = 0;
		String shortPeriodConvention = "first";
		BusinessdayCalendarInterface businessdayCalendar = new BusinessdayCalendarExcludingTARGETHolidays();
		String		frequency = "semiannual";
		String		daycountConvention = "act/365";



		calMat.add(Calendar.YEAR, maturityInYears);
		Date maturityDate = calMat.getTime();

		ScheduleInterface schedulePayer = ScheduleGenerator.createScheduleFromConventions(
				referenceDate,
				startDate,
				maturityDate,
				frequency,
				daycountConvention,
				shortPeriodConvention,
				"modified_following", businessdayCalendar, fixingOffsetDays, paymentOffsetDays);



		ScheduleInterface scheduleReceiver = schedulePayer;

		//1.2) Set spreads
		double spreadPayer     = 0.0;
		double spreadReceiver  = 0.01;
		//1.3) Create legs: receive fixed, pay float
		SwapLeg legPayer    = new SwapLeg(schedulePayer, "forwardCurve", spreadPayer, "DiscountCurveFromForwardCurveforwardCurve)");
		SwapLeg legReceiver = new SwapLeg(scheduleReceiver, null, spreadReceiver, "DiscountCurveFromForwardCurveforwardCurve)");
		//2)  Create Swap
		Swap swap = new Swap(legReceiver, legPayer);

		return swap;
	}
}
