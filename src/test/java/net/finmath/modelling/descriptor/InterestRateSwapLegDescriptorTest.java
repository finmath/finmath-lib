package net.finmath.modelling.descriptor;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.marketdata.model.curves.Curve;
import net.finmath.marketdata.model.curves.CurveInterface;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.DiscountCurveInterface;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurveInterface;
import net.finmath.marketdata.products.AnalyticProductInterface;
import net.finmath.modelling.DescribedModel;
import net.finmath.modelling.DescribedProduct;
import net.finmath.modelling.modelfactory.AnalyticModelFactory;
import net.finmath.modelling.productfactory.InterestRateMonteCarloProductFactory;
import net.finmath.montecarlo.interestrate.CalibrationProduct;
import net.finmath.montecarlo.interestrate.LIBORMarketModel;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.LIBORMonteCarloSimulationFromLIBORModel;
import net.finmath.montecarlo.interestrate.models.LIBORMarketModelFromCovarianceModel;
import net.finmath.montecarlo.interestrate.models.modelplugins.LIBORCorrelationModelExponentialDecay;
import net.finmath.montecarlo.interestrate.models.modelplugins.LIBORCovarianceModelFromVolatilityAndCorrelation;
import net.finmath.montecarlo.interestrate.models.modelplugins.LIBORVolatilityModelFromGivenMatrix;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.Schedule;
import net.finmath.time.ScheduleGenerator;
import net.finmath.time.TimeDiscretizationFromArray;
import net.finmath.time.businessdaycalendar.BusinessdayCalendar;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingTARGETHolidays;

public class InterestRateSwapLegDescriptorTest {

	static LocalDate referenceDate = LocalDate.of(2018, Month.JULY, 18);

	/**
	 * Test a floating rate leg.
	 *
	 * @throws CalculationException Thrown if calculation fails.
	 */
	@Test
	public void testFloatLeg() throws CalculationException {

		int			spotOffsetDays = 2;
		String		forwardStartPeriod = "0D";
		String		maturity = "35Y";
		String		frequency = "semiannual";
		String		daycountConvention = "30/360";

		/*
		 * Create leg descriptor
		 */
		Schedule schedule = ScheduleGenerator.createScheduleFromConventions(referenceDate, spotOffsetDays, forwardStartPeriod, maturity, frequency, daycountConvention,
				"first", "following", new BusinessdayCalendarExcludingTARGETHolidays(), -2, 0);
		double notional = 2;
		double spread = 0.0;
		boolean isNotionalExchanged = false;
		InterestRateSwapLegProductDescriptor legDescriptor = new InterestRateSwapLegProductDescriptor("forwardCurve", "discountCurve", new ScheduleDescriptor(schedule), notional, spread, isNotionalExchanged);

		/*
		 * Create Monte-Carlo model
		 */
		// TODO change this to go through a descriptor
		int numberOfPaths = 10000;
		int numberOfFactors = 5;
		double correlationDecayParam = 0.2;
		LIBORModelMonteCarloSimulationModel modelMC = createLIBORMarketModel(numberOfPaths, numberOfFactors, correlationDecayParam, createDiscountCurve(), createForwardCurve());

		/*
		 * Create Monte-Carlo leg
		 */
		DescribedProduct<InterestRateSwapLegProductDescriptor> legMC =
				(DescribedProduct<InterestRateSwapLegProductDescriptor>) (new InterestRateMonteCarloProductFactory(referenceDate)).getProductFromDescriptor(legDescriptor);

		/*
		 * Monte-Carlo value
		 */
		RandomVariable value = (RandomVariable) legMC.getValue(0.0, modelMC);
		double valueSimulation = value.getAverage();
		System.out.println("Float leg (simulation): " + value.getAverage() + "\t +/-" + value.getStandardError());


		/*
		 * Create Analytic model
		 */
		AnalyticModelDescriptor modelAnalyticDescriptor = new AnalyticModelDescriptor(referenceDate, Arrays.asList(new CurveInterface[] {createDiscountCurve(), createForwardCurve()}) , null);
		DescribedModel<AnalyticModelDescriptor> modelAnalytic = (new AnalyticModelFactory()).getModelFromDescriptor(modelAnalyticDescriptor);


		/*
		 * Create analytic leg
		 */
		DescribedProduct<InterestRateSwapLegProductDescriptor> legAnalytic = (DescribedProduct<InterestRateSwapLegProductDescriptor>) modelAnalytic.getProductFromDescriptor(legDescriptor);

		/*
		 * Analytic value
		 */
		double valueAnalytic = ((AnalyticProductInterface) legAnalytic).getValue(0.0, (AnalyticModelInterface) modelAnalytic);
		System.out.println("Float leg (analytic)..: " + valueAnalytic);

		System.out.println();

		assertEquals("Monte-Carlo value", valueAnalytic, valueSimulation, 1E-2);
	}



	/**
	 * Test a fixed rate leg.
	 *
	 * @throws CalculationException Thrown if calculation fails.
	 */
	@Test
	public void testFixLeg() throws CalculationException {

		int			spotOffsetDays = 2;
		String		forwardStartPeriod = "0D";
		String		maturity = "35Y";
		String		frequency = "semiannual";
		String		daycountConvention = "30/360";

		/*
		 * Create leg descriptor
		 */
		Schedule schedule = ScheduleGenerator.createScheduleFromConventions(referenceDate, spotOffsetDays, forwardStartPeriod, maturity, frequency, daycountConvention,
				"first", "following", new BusinessdayCalendarExcludingTARGETHolidays(), -2, 0);
		double notional = 2;
		double spread = 0.05;
		boolean isNotionalExchanged = false;
		InterestRateSwapLegProductDescriptor legDescriptor = new InterestRateSwapLegProductDescriptor(null, "discountCurve", new ScheduleDescriptor(schedule), notional, spread, isNotionalExchanged);

		/*
		 * Create Monte-Carlo model
		 */
		int numberOfPaths = 10000;
		int numberOfFactors = 5;
		double correlationDecayParam = 0.2;
		LIBORModelMonteCarloSimulationModel model = createLIBORMarketModel(numberOfPaths, numberOfFactors, correlationDecayParam, createDiscountCurve(), createForwardCurve());

		/*
		 * Create Monte-Carlo leg
		 */
		DescribedProduct<InterestRateSwapLegProductDescriptor> legMC =
				(DescribedProduct<InterestRateSwapLegProductDescriptor>) (new InterestRateMonteCarloProductFactory(referenceDate)).getProductFromDescriptor(legDescriptor);

		/*
		 * Monte-Carlo value
		 */
		RandomVariable value = (RandomVariable) legMC.getValue(0.0, model);
		double valueSimulation = value.getAverage();
		System.out.println("Fixed leg (simulation): " + value.getAverage() + "\t +/-" + value.getStandardError());

		/*
		 * Create Analytic model
		 */
		AnalyticModelDescriptor modelAnalyticDescriptor = new AnalyticModelDescriptor(referenceDate, Arrays.asList(new CurveInterface[] {createDiscountCurve(), createForwardCurve()}) , null);
		DescribedModel<AnalyticModelDescriptor> modelAnalytic = (new AnalyticModelFactory()).getModelFromDescriptor(modelAnalyticDescriptor);

		/*
		 * Create analytic leg
		 */
		DescribedProduct<InterestRateSwapLegProductDescriptor> legAnalytic = (DescribedProduct<InterestRateSwapLegProductDescriptor>) modelAnalytic.getProductFromDescriptor(legDescriptor);

		/*
		 * Analytic value
		 */
		double valueAnalytic = ((AnalyticProductInterface) legAnalytic).getValue(0.0, (AnalyticModelInterface) modelAnalytic);
		System.out.println("Fixed leg (analytic)..: " + valueAnalytic);

		System.out.println();

		assertEquals("Monte-Carlo value", valueAnalytic, valueSimulation, 1E-2);
	}

	public static DiscountCurve createDiscountCurve() {
		return DiscountCurve.createDiscountCurveFromZeroRates(
				"discountCurve"								/* name of the curve */,
				new double[] {0.5 , 1.0 , 2.0 , 5.0 , 40.0}	/* maturities */,
				new double[] {0.04, 0.04, 0.04, 0.04, 0.05}	/* zero rates */
				);
	}

	public static ForwardCurve createForwardCurve() {
		return ForwardCurve.createForwardCurveFromForwards(
				"forwardCurve"								/* name of the curve */,
				referenceDate,
				"6M",
				new BusinessdayCalendarExcludingTARGETHolidays(),
				BusinessdayCalendar.DateRollConvention.FOLLOWING,
				Curve.InterpolationMethod.LINEAR,
				Curve.ExtrapolationMethod.CONSTANT,
				Curve.InterpolationEntity.VALUE,
				ForwardCurve.InterpolationEntityForward.FORWARD,
				null,
				null,
				new double[] {0.5 , 1.0 , 2.0 , 5.0 , 40.0}	/* fixings of the forward */,
				new double[] {0.05, 0.05, 0.05, 0.05, 0.05}	/* forwards */
				);
	}


	public static LIBORModelMonteCarloSimulationModel createLIBORMarketModel(
			int numberOfPaths, int numberOfFactors, double correlationDecayParam, DiscountCurveInterface discountCurve, ForwardCurveInterface forwardCurve) throws CalculationException {

		AnalyticModelInterface model = new AnalyticModel(new CurveInterface[] { forwardCurve , discountCurve });

		/*
		 * Create the libor tenor structure and the initial values
		 */
		double liborPeriodLength	= 0.5;
		double liborRateTimeHorzion	= 40.0;
		TimeDiscretizationFromArray liborPeriodDiscretization = new TimeDiscretizationFromArray(0.0, (int) (liborRateTimeHorzion / liborPeriodLength), liborPeriodLength);

		/*
		 * Create a simulation time discretization
		 */
		double lastTime	= 40.0;
		double dt		= 0.5;

		TimeDiscretizationFromArray timeDiscretizationFromArray = new TimeDiscretizationFromArray(0.0, (int) (lastTime / dt), dt);

		/*
		 * Create a volatility structure v[i][j] = sigma_j(t_i)
		 */
		double[][] volatility = new double[timeDiscretizationFromArray.getNumberOfTimeSteps()][liborPeriodDiscretization.getNumberOfTimeSteps()];
		for (int timeIndex = 0; timeIndex < volatility.length; timeIndex++) {
			for (int liborIndex = 0; liborIndex < volatility[timeIndex].length; liborIndex++) {
				// Create a very simple volatility model here
				double time = timeDiscretizationFromArray.getTime(timeIndex);
				double maturity = liborPeriodDiscretization.getTime(liborIndex);
				double timeToMaturity = maturity - time;

				double instVolatility;
				if(timeToMaturity <= 0) {
					instVolatility = 0;				// This forward rate is already fixed, no volatility
				} else {
					instVolatility = 0.3 + 0.2 * Math.exp(-0.25 * timeToMaturity);
				}

				// Store
				volatility[timeIndex][liborIndex] = instVolatility;
			}
		}
		LIBORVolatilityModelFromGivenMatrix volatilityModel = new LIBORVolatilityModelFromGivenMatrix(timeDiscretizationFromArray, liborPeriodDiscretization, volatility);

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

		// BlendedLocalVolatlityModel (future extension)
		//		AbstractLIBORCovarianceModel covarianceModel2 = new BlendedLocalVolatlityModel(covarianceModel, 0.00, false);

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
		LIBORMarketModel liborMarketModel = new LIBORMarketModelFromCovarianceModel(
				liborPeriodDiscretization, model, forwardCurve, discountCurve, covarianceModel, calibrationItems, properties);

		EulerSchemeFromProcessModel process = new EulerSchemeFromProcessModel(
				new net.finmath.montecarlo.BrownianMotionLazyInit(timeDiscretizationFromArray,
						numberOfFactors, numberOfPaths, 3141 /* seed */));
		//		process.setScheme(EulerSchemeFromProcessModel.Scheme.PREDICTOR_CORRECTOR);

		return new LIBORMonteCarloSimulationFromLIBORModel(liborMarketModel, process);
	}

}
