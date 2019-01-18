/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 06.11.2015
 */

package net.finmath.montecarlo.interestrate.products;

import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.marketdata.model.curves.Curve;
import net.finmath.marketdata.model.curves.CurveInterface;
import net.finmath.marketdata.model.curves.DiscountCurveInterface;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurveInterface;
import net.finmath.montecarlo.interestrate.CalibrationProduct;
import net.finmath.montecarlo.interestrate.LIBORMarketModel;
import net.finmath.montecarlo.interestrate.LIBORMarketModel.Measure;
import net.finmath.montecarlo.interestrate.LIBORMarketModelInterface;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulation;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORCorrelationModelExponentialDecay;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORCovarianceModelFromVolatilityAndCorrelation;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORVolatilityModelFromGivenMatrix;
import net.finmath.montecarlo.process.ProcessEulerScheme;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingTARGETHolidays;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarInterface;

/**
 * @author Christian Fries
 */
@RunWith(Parameterized.class)
public class SimpleCappedFlooredFloatingRateBondTest {

	@Parameters(name="{0}")
	public static Collection<Object[]> generateData()
	{
		return Arrays.asList(new Object[][] {
			{ Measure.SPOT }, { Measure.TERMINAL }
		});
	}

	private final int numberOfPaths = 10000;

	private final Measure measure;

	public SimpleCappedFlooredFloatingRateBondTest(Measure measure) {
		// Store measure
		this.measure = measure;
	}

	@Test
	public void test() throws CalculationException {

		/*
		 * Create Monte-Carlo model
		 */
		LIBORModelMonteCarloSimulationInterface model = createLIBORMarketModel(numberOfPaths, measure);

		/*
		 * Create Product
		 */
		double[] fixingDates  = (new TimeDiscretization(0.0, 9, 0.5)).getAsDoubleArray();
		double[] paymentDates = (new TimeDiscretization(0.5, 9, 0.5)).getAsDoubleArray();
		double maturity = 0.5 + 9 * 0.5;

		double[] floors = null;
		double[] caps = null;
		double[] spreads = null;

		AbstractLIBORMonteCarloProduct product = new SimpleCappedFlooredFloatingRateBond("", fixingDates, paymentDates, spreads, floors, caps, maturity);

		double value = product.getValue(model);

		System.out.println("Value of floating rate bond (measure = " + measure + "): " + value);

		if(measure == Measure.SPOT) {
			Assert.assertEquals("Value of floating rate bond.", 1.0, value, 1E-10);
		}
		if(measure == Measure.TERMINAL) {
			Assert.assertEquals("Value of floating rate bond.", 1.0, value, 2E-2);
		}
	}

	public static LIBORModelMonteCarloSimulationInterface createLIBORMarketModel(int numberOfPaths, Measure measure) throws CalculationException {

		LocalDate	referenceDate = LocalDate.of(2014, Month.AUGUST, 12);

		// Create the forward curve (initial value of the LIBOR market model)
		ForwardCurveInterface forwardCurve = ForwardCurve.createForwardCurveFromForwards(
				"forwardCurve"								/* name of the curve */,
				referenceDate,
				"6M",
				new BusinessdayCalendarExcludingTARGETHolidays(),
				BusinessdayCalendarInterface.DateRollConvention.FOLLOWING,
				Curve.InterpolationMethod.LINEAR,
				Curve.ExtrapolationMethod.CONSTANT,
				Curve.InterpolationEntity.VALUE,
				ForwardCurve.InterpolationEntityForward.FORWARD,
				null,
				null,
				new double[] {0.5 , 1.0 , 2.0 , 5.0 , 40.0}	/* fixings of the forward */,
				new double[] {0.05, 0.05, 0.05, 0.05, 0.05}	/* forwards */
				);

		// No discount curve - single curve model
		DiscountCurveInterface discountCurve = null;

		//		AnalyticModelInterface model = new AnalyticModel(new CurveInterface[] { forwardCurve , discountCurve });
		AnalyticModelInterface model = new AnalyticModel(new CurveInterface[] { forwardCurve });

		/*
		 * Create the libor tenor structure and the initial values
		 */
		double liborPeriodLength	= 0.5;
		double liborRateTimeHorzion	= 40.0;
		TimeDiscretization liborPeriodDiscretization = new TimeDiscretization(0.0, (int) (liborRateTimeHorzion / liborPeriodLength), liborPeriodLength);

		/*
		 * Create a simulation time discretization
		 */
		double lastTime	= 40.0;
		double dt		= 0.5;

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
				if(timeToMaturity <= 0) {
					instVolatility = 0;				// This forward rate is already fixed, no volatility
				} else {
					instVolatility = 0.3 + 0.2 * Math.exp(-0.25 * timeToMaturity);
				}

				// Store
				volatility[timeIndex][liborIndex] = instVolatility;
			}
		}
		LIBORVolatilityModelFromGivenMatrix volatilityModel = new LIBORVolatilityModelFromGivenMatrix(timeDiscretization, liborPeriodDiscretization, volatility);

		/*
		 * Create a correlation model rho_{i,j} = exp(-a * abs(T_i-T_j))
		 */
		int numberOfFactors = 5;
		double correlationDecayParam = 0.2;
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
		Map<String, String> properties = new HashMap<>();

		// Choose the simulation measure
		properties.put("measure", measure.name());

		// Choose log normal model
		properties.put("stateSpace", LIBORMarketModel.StateSpace.LOGNORMAL.name());

		// Empty array of calibration items - hence, model will use given covariance
		CalibrationProduct[] calibrationItems = new CalibrationProduct[0];

		/*
		 * Create corresponding LIBOR Market Model
		 */
		LIBORMarketModelInterface liborMarketModel = new LIBORMarketModel(
				liborPeriodDiscretization, model, forwardCurve, discountCurve, covarianceModel, calibrationItems, properties);

		ProcessEulerScheme process = new ProcessEulerScheme(
				new net.finmath.montecarlo.BrownianMotionLazyInit(timeDiscretization,
						numberOfFactors, numberOfPaths, 3141 /* seed */), ProcessEulerScheme.Scheme.PREDICTOR_CORRECTOR);

		return new LIBORModelMonteCarloSimulation(liborMarketModel, process);
	}
}
