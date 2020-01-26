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

import net.finmath.exception.CalculationException;
import net.finmath.marketdata2.model.AnalyticModelFromCurvesAndVols;
import net.finmath.marketdata2.model.curves.Curve;
import net.finmath.marketdata2.products.AbstractAnalyticProduct;
import net.finmath.marketdata2.products.Swap;
import net.finmath.marketdata2.products.SwapLeg;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFromArrayFactory;
import net.finmath.montecarlo.conditionalexpectation.MonteCarloConditionalExpectationRegression;
import net.finmath.montecarlo.interestrate.CalibrationProduct;
import net.finmath.montecarlo.interestrate.LIBORMarketModel;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.LIBORMonteCarloSimulationFromLIBORModel;
import net.finmath.montecarlo.interestrate.models.LIBORMarketModelFromCovarianceModel;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCorrelationModelExponentialDecay;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCovarianceModelFromVolatilityAndCorrelation;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORVolatilityModel;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORVolatilityModelFourParameterExponentialForm;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORVolatilityModelFromGivenMatrix;
import net.finmath.montecarlo.interestrate.products.TermStructureMonteCarloProduct;
import net.finmath.montecarlo.interestrate.products.components.AbstractNotional;
import net.finmath.montecarlo.interestrate.products.components.Notional;
import net.finmath.montecarlo.interestrate.products.indices.AbstractIndex;
import net.finmath.montecarlo.interestrate.products.indices.LIBORIndex;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.stochastic.ConditionalExpectationEstimator;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.Schedule;
import net.finmath.time.ScheduleGenerator;
import net.finmath.time.TimeDiscretizationFromArray;
import net.finmath.time.businessdaycalendar.BusinessdayCalendar;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingTARGETHolidays;

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
		RandomVariableFactory abstractRandomVariableFactory = new RandomVariableFromArrayFactory();

		int maturityInYears = 5;
		int forwardStartTimeInYears = 0;
		// Create Analytic Swap
		AbstractAnalyticProduct swapAnalytic = createSwapAnalytic(maturityInYears,forwardStartTimeInYears);
		// Create Monte Carlo Swap
		TermStructureMonteCarloProduct swapMonteCarlo = createSwap(maturityInYears,forwardStartTimeInYears,abstractRandomVariableFactory);

		// Create a Libor market model
		LIBORModelMonteCarloSimulationModel liborMarketModel = createLIBORMarketModel(abstractRandomVariableFactory,
				numberOfPaths,
				numberOfFactors,
				//(ForwardCurve)curves.getModel().getForwardCurve("forwardCurve"),
				0.0 /* Correlation */);
		double evaluationTime = forwardStartTimeInYears;

		int timeIndex = liborMarketModel.getTimeIndex(evaluationTime);
		// Get all Libors at timeIndex which are not yet fixed (others null) and times for the timeDiscretizationFromArray of the curves
		ArrayList<RandomVariable> liborsAtTimeIndex = new ArrayList<>();
		int firstLiborIndex = liborMarketModel.getLiborPeriodDiscretization().getTimeIndexNearestGreaterOrEqual(evaluationTime);
		double firstLiborTime = liborMarketModel.getLiborPeriodDiscretization().getTime(firstLiborIndex);
		if(firstLiborTime>evaluationTime) {
			liborsAtTimeIndex.add(liborMarketModel.getLIBOR(evaluationTime, evaluationTime, firstLiborTime));
		}
		for(int i=firstLiborIndex;i<liborMarketModel.getNumberOfLibors();i++) {
			liborsAtTimeIndex.add(liborMarketModel.getLIBOR(timeIndex,i));
		}
		//times[times.length-1]= model.getLiborPeriodDiscretization().getTime(model.getNumberOfLibors())-evaluationTime;
		RandomVariable[] libors = liborsAtTimeIndex.toArray(new RandomVariable[liborsAtTimeIndex.size()]);
		// Create conditional expectation operator
		ArrayList<RandomVariable> basisFunctions = getRegressionBasisFunctions(libors);
		ConditionalExpectationEstimator conditionalExpectationOperator = new MonteCarloConditionalExpectationRegression(basisFunctions.toArray(new RandomVariable[0]));
		// Get value with Monte Carlo
		double valueMonteCarlo = swapMonteCarlo.getValue(evaluationTime, liborMarketModel).getConditionalExpectation(conditionalExpectationOperator).getAverage();

		// Create Forward CurveFromInterpolationPoints from the LIBORMonteCarloModel
		net.finmath.marketdata2.model.curves.ForwardCurveInterpolation forwardCurveInterpolation = net.finmath.marketdata2.model.curves.ForwardCurveInterpolation.createForwardCurveFromMonteCarloLiborModel("forwardCurve",liborMarketModel, evaluationTime);
		// Get value of analytic swap
		net.finmath.marketdata2.model.curves.DiscountCurveInterface discountCurve = new net.finmath.marketdata2.model.curves.DiscountCurveFromForwardCurve(net.finmath.marketdata2.model.curves.ForwardCurveInterpolation.createForwardCurveFromMonteCarloLiborModel(forwardCurveInterpolation.getName(), liborMarketModel, 0));
		//net.finmath.analytic.model.curves.DiscountCurveInterpolation.createDiscountCurveFromMonteCarloLiborModel("forwardCurve",liborMarketModel, evaluationTime);

		double valueWithCurves = swapAnalytic.getValue(0.0, new AnalyticModelFromCurvesAndVols(abstractRandomVariableFactory, new Curve[]{forwardCurveInterpolation,discountCurve})).getAverage();

		System.out.println("" + valueMonteCarlo + "\t" + valueWithCurves);

		Assert.assertEquals(valueMonteCarlo,valueWithCurves,1E-4); //True if forwardStartTimeInYears = 0;
	}

	private static ArrayList<RandomVariable> getRegressionBasisFunctions(RandomVariable[] libors) {
		ArrayList<RandomVariable> basisFunctions = new ArrayList<>();

		// Create basis functions - here: 1, L
		for(int liborIndex=0; liborIndex<libors.length;liborIndex++){
			for(int powerOfRegressionMonomial=0; powerOfRegressionMonomial<=1; powerOfRegressionMonomial++) {
				basisFunctions.add(libors[liborIndex].pow(powerOfRegressionMonomial));
			}
		}
		return basisFunctions;
	}


	public static  LIBORModelMonteCarloSimulationModel createLIBORMarketModel(
			RandomVariableFactory abstractRandomVariableFactory,
			int numberOfPaths, int numberOfFactors, /*ForwardCurve forwardCurve,*/ double correlationDecayParam) throws CalculationException {

		/*
		 * Create the libor tenor structure and the initial values
		 */
		double liborPeriodLength	= 0.5;
		double liborRateTimeHorzion	= 30.0;
		TimeDiscretizationFromArray liborPeriodDiscretization = new TimeDiscretizationFromArray(0.0, (int) (liborRateTimeHorzion / liborPeriodLength), liborPeriodLength);

		LocalDate referenceDate = LocalDate.of(2017, 8, 20);

		// Create the forward curve (initial value of the LIBOR market model). This curve is still double!
		net.finmath.marketdata.model.curves.DiscountCurveInterpolation discountCurveInterpolation = net.finmath.marketdata.model.curves.DiscountCurveInterpolation.createDiscountCurveFromZeroRates(
				"discountCurve"								/* name of the curve */,
				new double[] {0.5 , 1.0, 2.0, 5.0, 30.0}	/* fixings of the forward */,
				new double[] {0.01, 0.02, 0.02, 0.022, 0.025}	/* zero rates */
				);

		net.finmath.marketdata.model.curves.ForwardCurve forwardCurve = new net.finmath.marketdata.model.curves.ForwardCurveFromDiscountCurve(discountCurveInterpolation.getName(), referenceDate, "6M");
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

		TimeDiscretizationFromArray timeDiscretizationFromArray = new TimeDiscretizationFromArray(0.0, (int) (lastTime / dt), dt);

		/*
		 * Create a volatility structure v[i][j] = sigma_j(t_i)
		 */
		double a = 0.0 / 20.0, b = 0.0, c = 0.25, d = 0.3 / 20.0 / 2.0;
		//LIBORVolatilityModel volatilityModel = new LIBORVolatilityModelFourParameterExponentialFormIntegrated(timeDiscretizationFromArray, liborPeriodDiscretization, a, b, c, d, false);
		volatilityModel = new LIBORVolatilityModelFourParameterExponentialForm(abstractRandomVariableFactory, timeDiscretizationFromArray, liborPeriodDiscretization, a, b, c, d, false);
		double[][] volatilityMatrix = new double[timeDiscretizationFromArray.getNumberOfTimeSteps()][liborPeriodDiscretization.getNumberOfTimeSteps()];
		for(int timeIndex=0; timeIndex<timeDiscretizationFromArray.getNumberOfTimeSteps(); timeIndex++) {
			Arrays.fill(volatilityMatrix[timeIndex], d);
		}
		volatilityModel = new LIBORVolatilityModelFromGivenMatrix(abstractRandomVariableFactory, timeDiscretizationFromArray, liborPeriodDiscretization, volatilityMatrix);

		/*
		 * Create a correlation model rho_{i,j} = exp(-a * abs(T_i-T_j))
		 */
		LIBORCorrelationModelExponentialDecay correlationModel = new LIBORCorrelationModelExponentialDecay(
				timeDiscretizationFromArray, liborPeriodDiscretization, numberOfFactors,
				correlationDecayParam);


		/*
		 * Combine volatility model and correlation model to a covariance model
		 */
		LIBORCovarianceModelFromVolatilityAndCorrelation covarianceModel =
				new LIBORCovarianceModelFromVolatilityAndCorrelation(timeDiscretizationFromArray,
						liborPeriodDiscretization, volatilityModel, correlationModel);

		// Set model properties
		Map<String, String> properties = new HashMap<>();

		// Choose the simulation measure
		properties.put("measure", LIBORMarketModelFromCovarianceModel.Measure.SPOT.name());

		// Choose log normal model
		properties.put("stateSpace", LIBORMarketModelFromCovarianceModel.StateSpace.LOGNORMAL.name());

		// Empty array of calibration items - hence, model will use given covariance
		CalibrationProduct[] calibrationItems = new CalibrationProduct[0];

		/*
		 * Create corresponding LIBOR Market Model
		 */
		LIBORMarketModel liborMarketModel = new LIBORMarketModelFromCovarianceModel(liborPeriodDiscretization, new net.finmath.marketdata.model.AnalyticModelFromCurvesAndVols(new net.finmath.marketdata.model.curves.Curve[]{forwardCurve, discountCurveInterpolation}), forwardCurve, discountCurveInterpolation, abstractRandomVariableFactory, covarianceModel, calibrationItems, properties);

		BrownianMotion brownianMotion = new net.finmath.montecarlo.BrownianMotionLazyInit(timeDiscretizationFromArray, numberOfFactors, numberOfPaths, 3141 /* seed */);

		EulerSchemeFromProcessModel process = new EulerSchemeFromProcessModel(brownianMotion, EulerSchemeFromProcessModel.Scheme.EULER_FUNCTIONAL);

		return new LIBORMonteCarloSimulationFromLIBORModel(liborMarketModel, process);
	}


	public static TermStructureMonteCarloProduct createSwap(int maturityInYears, int forwardStartTimeInYears, RandomVariableFactory factory){


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
		BusinessdayCalendar businessdayCalendar = new BusinessdayCalendarExcludingTARGETHolidays();
		String		frequency = "semiannual";
		String		daycountConvention = "act/365";

		calMat.add(Calendar.YEAR, maturityInYears);
		Date maturityDate = calMat.getTime();

		Schedule schedulePayer = ScheduleGenerator.createScheduleFromConventions(
				referenceDate,
				startDate,
				maturityDate,
				frequency,
				daycountConvention,
				shortPeriodConvention,
				"modified_following", businessdayCalendar, fixingOffsetDays, paymentOffsetDays);

		Schedule scheduleReceiver = schedulePayer;

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
		BusinessdayCalendar businessdayCalendar = new BusinessdayCalendarExcludingTARGETHolidays();
		String		frequency = "semiannual";
		String		daycountConvention = "act/365";



		calMat.add(Calendar.YEAR, maturityInYears);
		Date maturityDate = calMat.getTime();

		Schedule schedulePayer = ScheduleGenerator.createScheduleFromConventions(
				referenceDate,
				startDate,
				maturityDate,
				frequency,
				daycountConvention,
				shortPeriodConvention,
				"modified_following", businessdayCalendar, fixingOffsetDays, paymentOffsetDays);



		Schedule scheduleReceiver = schedulePayer;

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
