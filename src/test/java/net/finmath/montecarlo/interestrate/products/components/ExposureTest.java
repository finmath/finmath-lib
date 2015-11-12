/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 01.10.2015
 */

package net.finmath.montecarlo.interestrate.products.components;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.Month;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.montecarlo.interestrate.LIBORMarketModel;
import net.finmath.montecarlo.interestrate.LIBORMarketModel.Measure;
import net.finmath.montecarlo.interestrate.LIBORMarketModelInterface;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulation;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORCorrelationModelExponentialDecay;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORCovarianceModelFromVolatilityAndCorrelation;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORVolatilityModelFromGivenMatrix;
import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.montecarlo.interestrate.products.Swap;
import net.finmath.montecarlo.interestrate.products.SwapLeg;
import net.finmath.montecarlo.interestrate.products.Swaption;
import net.finmath.montecarlo.interestrate.products.indices.AbstractIndex;
import net.finmath.montecarlo.interestrate.products.indices.LIBORIndex;
import net.finmath.montecarlo.process.ProcessEulerScheme;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.RegularSchedule;
import net.finmath.time.ScheduleGenerator;
import net.finmath.time.ScheduleInterface;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingTARGETHolidays;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarInterface;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Christian Fries
 *
 */
public class ExposureTest {

	private final NumberFormat formatter6 = new DecimalFormat("0.000000", new DecimalFormatSymbols(new Locale("en")));

	@Test
	public void testExpectedPositiveExposure() throws CalculationException {
		/*
		 * Create a receiver swap (receive fix, pay float)
		 */
		ScheduleInterface legScheduleRec = ScheduleGenerator.createScheduleFromConventions(
				LocalDate.of(2015, Month.JANUARY, 03) /* referenceDate */, 
				LocalDate.of(2015, Month.JANUARY, 06) /* startDate */,
				LocalDate.of(2015, Month.JANUARY, 06) /* maturityDate */,
				ScheduleGenerator.Frequency.ANNUAL /* frequency */,
				ScheduleGenerator.DaycountConvention.ACT_365 /* daycountConvention */,
				ScheduleGenerator.ShortPeriodConvention.FIRST /* shortPeriodConvention */,
				BusinessdayCalendarInterface.DateRollConvention.FOLLOWING /* dateRollConvention */,
				new BusinessdayCalendarExcludingTARGETHolidays() /* businessdayCalendar */,
				0 /* fixingOffsetDays */,
				0 /* paymentOffsetDays */);

		ScheduleInterface legSchedulePay = ScheduleGenerator.createScheduleFromConventions(
				LocalDate.of(2015, Month.JANUARY, 03) /* referenceDate */, 
				LocalDate.of(2015, Month.JANUARY, 06) /* startDate */,
				LocalDate.of(2015, Month.JANUARY, 06) /* maturityDate */,
				ScheduleGenerator.Frequency.QUARTERLY /* frequency */,
				ScheduleGenerator.DaycountConvention.ACT_365 /* daycountConvention */,
				ScheduleGenerator.ShortPeriodConvention.FIRST /* shortPeriodConvention */,
				BusinessdayCalendarInterface.DateRollConvention.FOLLOWING /* dateRollConvention */,
				new BusinessdayCalendarExcludingTARGETHolidays() /* businessdayCalendar */,
				0 /* fixingOffsetDays */,
				0 /* paymentOffsetDays */);
		AbstractNotional notional = new Notional(1.0);
		AbstractIndex index = new LIBORIndex("forwardCurve", 0.0, 0.25);
		double fixedCoupon = 0.025;

		SwapLeg swapLegRec = new SwapLeg(legScheduleRec, notional, null, fixedCoupon /* spread */, false /* isNotionalExchanged */);
		SwapLeg swapLegPay = new SwapLeg(legSchedulePay, notional, index, 0.0 /* spread */, false /* isNotionalExchanged */);
		AbstractLIBORMonteCarloProduct swap = new Swap(swapLegRec, swapLegPay);
		AbstractLIBORMonteCarloProduct swapExposureEstimator = new ExposureEstimator(swap);


		LIBORModelMonteCarloSimulationInterface lmm = createLIBORMarketModel(Measure.SPOT, 10000, 5, 0.1);

		// Print a single exposure path and the expected positive exposure
		for(double observationDate : lmm.getTimeDiscretization()) {

			if(observationDate == 0) continue;
			
			/*
			 * Calculate expected positive exposure of a swap
			 */
			RandomVariableInterface valuesSwap = swap.getValue(observationDate, lmm);
			RandomVariableInterface valuesEstimatedExposure = swapExposureEstimator.getValue(observationDate, lmm);
			RandomVariableInterface valuesPositiveExposure = valuesSwap.mult(valuesEstimatedExposure.barrier(valuesEstimatedExposure, new RandomVariable(1.0), 0.0));

			double exposureOnPath				= valuesEstimatedExposure.get(0);
			double expectedPositiveExposure		= valuesPositiveExposure.getAverage();

			System.out.println(observationDate + "\t" + formatter6.format(exposureOnPath) + " \t " + formatter6.format(expectedPositiveExposure));

			double basisPoint = 1E-4;
			Assert.assertTrue("Expected positive exposure", expectedPositiveExposure >= 0-1*basisPoint);
		}
	}


	@Test
	public void testAgainstSwaption() throws CalculationException {
		/*
		 * Create a receiver swap (receive fix, pay float)
		 */
		TimeDiscretization tenor = new TimeDiscretization(0.0, 40, 0.25);
		ScheduleInterface schedule = new RegularSchedule(tenor);
		
		AbstractNotional notional = new Notional(1.0);
		AbstractIndex index = new LIBORIndex("forwardCurve", 0.0, 0.25);
		double fixedCoupon = 0.025;

		SwapLeg swapLegRec = new SwapLeg(schedule, notional, index, 0.0 /* spread */, false /* isNotionalExchanged */);
		SwapLeg swapLegPay = new SwapLeg(schedule, notional, null, fixedCoupon /* spread */, false /* isNotionalExchanged */);
		AbstractLIBORMonteCarloProduct swap = new Swap(swapLegRec, swapLegPay);
		AbstractLIBORMonteCarloProduct swapExposureEstimator = new ExposureEstimator(swap);

		LIBORModelMonteCarloSimulationInterface lmm = createLIBORMarketModel(Measure.SPOT, 10000, 5, 0.1);

		// Print a single exposure path and the expected positive exposure
		for(double observationDate : lmm.getTimeDiscretization()) {

			/*
			 * Calculate expected positive exposure of a swap
			 */
			RandomVariableInterface valuesSwap = swap.getValue(observationDate, lmm);
			RandomVariableInterface valuesEstimatedExposure = swapExposureEstimator.getValue(observationDate, lmm);
			RandomVariableInterface valuesPositiveExposure = valuesSwap.mult(valuesEstimatedExposure.barrier(valuesEstimatedExposure, new RandomVariable(1.0), 0.0));

			double exposureOnPath = valuesEstimatedExposure.get(0);
			double expectedPositiveExposure				= valuesPositiveExposure.getAverage();
			double expectedPositiveExposureFromEstimate	= valuesEstimatedExposure.floor(0.0).getAverage();

			/*
			 * Benchmark value against a swaption
			 */
			double exerciseDate = observationDate;
			AbstractLIBORMonteCarloProduct swaption = new Swaption(exerciseDate, tenor, fixedCoupon);
			double swaptionValue = (Double)swaption.getValues(observationDate, lmm).get("value");

			System.out.println(observationDate + "\t" + formatter6.format(exposureOnPath) + " \t " + formatter6.format(expectedPositiveExposureFromEstimate) + " \t " + formatter6.format(expectedPositiveExposure) + " \t " + formatter6.format(swaptionValue) + " \t " + formatter6.format((expectedPositiveExposure-swaptionValue)*10000));

			double basisPoint = 1E-4;
			Assert.assertEquals("Expected positive exposure", swaptionValue, expectedPositiveExposure, 25*basisPoint);
		}
	}

	public static LIBORModelMonteCarloSimulationInterface createLIBORMarketModel(
			Measure measure, int numberOfPaths, int numberOfFactors, double correlationDecayParam) throws CalculationException {

		/*
		 * Create the libor tenor structure and the initial values
		 */
		double liborPeriodLength	= 0.25;
		double liborRateTimeHorzion	= 20.0;
		TimeDiscretization liborPeriodDiscretization = new TimeDiscretization(0.0, (int) (liborRateTimeHorzion / liborPeriodLength), liborPeriodLength);

		// Create the forward curve (initial value of the LIBOR market model)
		ForwardCurve forwardCurve = ForwardCurve.createForwardCurveFromForwards(
				"forwardCurve"								/* name of the curve */,
				new double[] {0.0 , 1.0 , 2.0 , 5.0 , 40.0}	/* fixings of the forward */,
				new double[] {0.025, 0.025, 0.025, 0.025, 0.025}	/* forwards */,
				liborPeriodLength							/* tenor / period length */
				);

		// Create the discount curve
		DiscountCurve discountCurve = DiscountCurve.createDiscountCurveFromZeroRates(
				"discountCurve"								/* name of the curve */,
				new double[] {0.5 , 1.0 , 2.0 , 5.0 , 40.0}	/* maturities */,
				new double[] {0.02, 0.02, 0.02, 0.02, 0.03}	/* zero rates */
				);
		
		/*
		 * Create a simulation time discretization
		 */
		double lastTime	= 15.0;
		double dt		= 0.125;

		TimeDiscretization timeDiscretization = new TimeDiscretization(0.0, (int) (lastTime / dt), dt);

		/*
		 * Create a volatility structure v[i][j] = sigma_j(t_i)
		 */
		double[][] volatility = new double[timeDiscretization.getNumberOfTimeSteps()][liborPeriodDiscretization.getNumberOfTimeSteps()];
		for (int timeIndex = 0; timeIndex < volatility.length; timeIndex++) {
			for (int liborIndex = 0; liborIndex < volatility[timeIndex].length; liborIndex++) {
				// Create a very simple volatility model here
				double time = timeDiscretization.getTime(timeIndex);
				double maturity = liborPeriodDiscretization.getTime(liborIndex);
				double timeToMaturity = maturity - time;

				double instVolatility;
				if(timeToMaturity <= 0)
					instVolatility = 0;				// This forward rate is already fixed, no volatility
				else
					instVolatility = 0.3 + 0.2 * Math.exp(-0.25 * timeToMaturity);

				// Store
				volatility[timeIndex][liborIndex] = instVolatility;
			}
		}
		LIBORVolatilityModelFromGivenMatrix volatilityModel = new LIBORVolatilityModelFromGivenMatrix(timeDiscretization, liborPeriodDiscretization, volatility);

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

		// BlendedLocalVolatlityModel (future extension)
		//		AbstractLIBORCovarianceModel covarianceModel2 = new BlendedLocalVolatlityModel(covarianceModel, 0.00, false);

		// Set model properties
		Map<String, String> properties = new HashMap<String, String>();

		// Choose the simulation measure
		properties.put("measure", measure.name());
		
		// Choose log normal model
		properties.put("stateSpace", LIBORMarketModel.StateSpace.LOGNORMAL.name());

		// Empty array of calibration items - hence, model will use given covariance
		LIBORMarketModel.CalibrationItem[] calibrationItems = new LIBORMarketModel.CalibrationItem[0];

		/*
		 * Create corresponding LIBOR Market Model
		 */
		LIBORMarketModelInterface liborMarketModel = new LIBORMarketModel(
				liborPeriodDiscretization, forwardCurve, discountCurve, covarianceModel, calibrationItems, properties);

		ProcessEulerScheme process = new ProcessEulerScheme(
				new net.finmath.montecarlo.BrownianMotion(timeDiscretization,
						numberOfFactors, numberOfPaths, 3141 /* seed */), ProcessEulerScheme.Scheme.PREDICTOR_CORRECTOR);

		return new LIBORModelMonteCarloSimulation(liborMarketModel, process);
	}
}
