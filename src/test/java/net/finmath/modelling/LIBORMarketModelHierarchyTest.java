/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 28 Feb 2015
 */

package net.finmath.modelling;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.time.Month;
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
import net.finmath.montecarlo.MonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.CalibrationProduct;
import net.finmath.montecarlo.interestrate.LIBORMarketModel;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.LIBORMonteCarloSimulationFromLIBORModel;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.models.LIBORMarketModelFromCovarianceModel;
import net.finmath.montecarlo.interestrate.models.modelplugins.LIBORCorrelationModelExponentialDecay;
import net.finmath.montecarlo.interestrate.models.modelplugins.LIBORCovarianceModelFromVolatilityAndCorrelation;
import net.finmath.montecarlo.interestrate.models.modelplugins.LIBORVolatilityModelFromGivenMatrix;
import net.finmath.montecarlo.interestrate.products.SwapLeg;
import net.finmath.montecarlo.interestrate.products.components.AbstractNotional;
import net.finmath.montecarlo.interestrate.products.components.Notional;
import net.finmath.montecarlo.interestrate.products.indices.AbstractIndex;
import net.finmath.montecarlo.interestrate.products.indices.LIBORIndex;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.time.Schedule;
import net.finmath.time.ScheduleGenerator;
import net.finmath.time.TimeDiscretizationFromArray;
import net.finmath.time.businessdaycalendar.BusinessdayCalendar;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingTARGETHolidays;

/**
 * Test the call to getValue on a product using different casts of the model.
 *
 * @author Christian Fries
 */
public class LIBORMarketModelHierarchyTest {

	/**
	 * Test the call to getValue on a product using different casts of the model.
	 *
	 * @throws CalculationException Thrown if calculation fails.
	 */
	@Test
	public void testModelHierarchy() throws CalculationException {

		LocalDate	referenceDate = LocalDate.of(2014,  Month.AUGUST,  12);
		int			spotOffsetDays = 2;
		String		forwardStartPeriod = "0D";
		String		maturity = "35Y";
		String		frequency = "semiannual";
		String		daycountConvention = "30/360";

		/*
		 * Create Monte-Carlo leg
		 */
		AbstractNotional notional = new Notional(1.0);
		AbstractIndex index = new LIBORIndex(0.0, 0.5);
		double spread = 0.0;
		Schedule schedule = ScheduleGenerator.createScheduleFromConventions(referenceDate, spotOffsetDays, forwardStartPeriod, maturity, frequency, daycountConvention, "first", "following", new BusinessdayCalendarExcludingTARGETHolidays(), -2, 0);
		SwapLeg leg = new SwapLeg(schedule, notional, index, spread, false /* isNotionalExchanged */);

		/*
		 * Create Monte-Carlo model
		 */
		int numberOfPaths = 10000;
		int numberOfFactors = 5;
		double correlationDecayParam = 0.2;
		LIBORModelMonteCarloSimulationModel model = createMultiCurveLIBORMarketModel(numberOfPaths, numberOfFactors, correlationDecayParam);

		/*
		 * Monte-Carlo value
		 */
		Map<String, Object> valueLIBORModelMonteCarloSimulation = leg.getValues(0.0, model);
		Map<String, Object> valueLIBORModelMonteCarloSimulationInterface = leg.getValues(0.0, model);
		Map<String, Object> valueTermStructureModelMonteCarloSimulationInterface = leg.getValues(0.0, (TermStructureMonteCarloSimulationModel)model);
		Map<String, Object> valueMonteCarloSimulationInterface = leg.getValues(0.0, (MonteCarloSimulationModel)model);
		Map<String, Object> valueModelInterface = leg.getValues(0.0, (Model)model);

		System.out.println(valueLIBORModelMonteCarloSimulation);
		System.out.println(valueLIBORModelMonteCarloSimulationInterface);
		System.out.println(valueTermStructureModelMonteCarloSimulationInterface);
		System.out.println(valueMonteCarloSimulationInterface);
		System.out.println(valueModelInterface);

		Double benchmarkValue = ((Double)valueLIBORModelMonteCarloSimulation.get("value")).doubleValue();
		assertEquals("Monte-Carlo value", benchmarkValue, ((Double)valueLIBORModelMonteCarloSimulation.get("value")).doubleValue(), 0);
		assertEquals("Monte-Carlo value", benchmarkValue, ((Double)valueLIBORModelMonteCarloSimulationInterface.get("value")).doubleValue(), 0);
		assertEquals("Monte-Carlo value", benchmarkValue, ((Double)valueTermStructureModelMonteCarloSimulationInterface.get("value")).doubleValue(), 0);
		assertEquals("Monte-Carlo value", benchmarkValue, ((Double)valueMonteCarloSimulationInterface.get("value")).doubleValue(), 0);
		assertEquals("Monte-Carlo value", benchmarkValue, ((Double)valueModelInterface.get("value")).doubleValue(), 0);
	}

	public static LIBORModelMonteCarloSimulationModel createMultiCurveLIBORMarketModel(int numberOfPaths, int numberOfFactors, double correlationDecayParam) throws CalculationException {

		LocalDate	referenceDate = LocalDate.of(2014, Month.AUGUST, 12);


		// Create the forward curve (initial value of the LIBOR market model)
		ForwardCurve forwardCurve = ForwardCurve.createForwardCurveFromForwards(
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

		// Create the discount curve
		DiscountCurve discountCurve = DiscountCurve.createDiscountCurveFromZeroRates(
				"discountCurve"								/* name of the curve */,
				new double[] {0.5 , 1.0 , 2.0 , 5.0 , 40.0}	/* maturities */,
				new double[] {0.04, 0.04, 0.04, 0.04, 0.05}	/* zero rates */
				);

		return createLIBORMarketModel(numberOfPaths, numberOfFactors, correlationDecayParam, discountCurve, forwardCurve);
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
