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
import net.finmath.marketdata.model.AnalyticModelFromCurvesAndVols;
import net.finmath.marketdata.model.curves.Curve;
import net.finmath.marketdata.model.curves.CurveInterpolation;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurveInterpolation;
import net.finmath.montecarlo.interestrate.CalibrationProduct;
import net.finmath.montecarlo.interestrate.LIBORMarketModel;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.LIBORMonteCarloSimulationFromLIBORModel;
import net.finmath.montecarlo.interestrate.models.LIBORMarketModelFromCovarianceModel;
import net.finmath.montecarlo.interestrate.models.LIBORMarketModelFromCovarianceModel.Measure;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCorrelationModelExponentialDecay;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCovarianceModelFromVolatilityAndCorrelation;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORVolatilityModelFromGivenMatrix;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.time.TimeDiscretizationFromArray;
import net.finmath.time.businessdaycalendar.BusinessdayCalendar;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingTARGETHolidays;

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

	public SimpleCappedFlooredFloatingRateBondTest(final Measure measure) {
		// Store measure
		this.measure = measure;
	}

	@Test
	public void test() throws CalculationException {

		/*
		 * Create Monte-Carlo model
		 */
		final LIBORModelMonteCarloSimulationModel model = createLIBORMarketModel(numberOfPaths, measure);

		/*
		 * Create Product
		 */
		final double[] fixingDates  = (new TimeDiscretizationFromArray(0.0, 9, 0.5)).getAsDoubleArray();
		final double[] paymentDates = (new TimeDiscretizationFromArray(0.5, 9, 0.5)).getAsDoubleArray();
		final double maturity = 0.5 + 9 * 0.5;

		final double[] floors = null;
		final double[] caps = null;
		final double[] spreads = null;

		final AbstractLIBORMonteCarloProduct product = new SimpleCappedFlooredFloatingRateBond("", fixingDates, paymentDates, spreads, floors, caps, maturity);

		final double value = product.getValue(model);

		System.out.println("Value of floating rate bond (measure = " + measure + "): " + value);

		if(measure == Measure.SPOT) {
			Assert.assertEquals("Value of floating rate bond.", 1.0, value, 1E-10);
		}
		if(measure == Measure.TERMINAL) {
			Assert.assertEquals("Value of floating rate bond.", 1.0, value, 2E-2);
		}
	}

	public static LIBORModelMonteCarloSimulationModel createLIBORMarketModel(final int numberOfPaths, final Measure measure) throws CalculationException {

		final LocalDate	referenceDate = LocalDate.of(2014, Month.AUGUST, 12);

		// Create the forward curve (initial value of the LIBOR market model)
		final ForwardCurve forwardCurve = ForwardCurveInterpolation.createForwardCurveFromForwards(
				"forwardCurve"								/* name of the curve */,
				referenceDate,
				"6M",
				new BusinessdayCalendarExcludingTARGETHolidays(),
				BusinessdayCalendar.DateRollConvention.FOLLOWING,
				CurveInterpolation.InterpolationMethod.LINEAR,
				CurveInterpolation.ExtrapolationMethod.CONSTANT,
				CurveInterpolation.InterpolationEntity.VALUE,
				ForwardCurveInterpolation.InterpolationEntityForward.FORWARD,
				null,
				null,
				new double[] {0.5 , 1.0 , 2.0 , 5.0 , 40.0}	/* fixings of the forward */,
				new double[] {0.05, 0.05, 0.05, 0.05, 0.05}	/* forwards */
				);

		// No discount curve - single curve model
		final DiscountCurve discountCurve = null;

		//		AnalyticModel model = new AnalyticModelFromCuvesAndVols(new CurveInterface[] { forwardCurve , discountCurve });
		final AnalyticModel model = new AnalyticModelFromCurvesAndVols(new Curve[] { forwardCurve });

		/*
		 * Create the libor tenor structure and the initial values
		 */
		final double liborPeriodLength	= 0.5;
		final double liborRateTimeHorzion	= 40.0;
		final TimeDiscretizationFromArray liborPeriodDiscretization = new TimeDiscretizationFromArray(0.0, (int) (liborRateTimeHorzion / liborPeriodLength), liborPeriodLength);

		/*
		 * Create a simulation time discretization
		 */
		final double lastTime	= 40.0;
		final double dt		= 0.5;

		final TimeDiscretizationFromArray timeDiscretizationFromArray = new TimeDiscretizationFromArray(0.0, (int) (lastTime / dt), dt);

		/*
		 * Create a volatility structure v[i][j] = sigma_j(t_i)
		 */
		final double[][] volatility = new double[timeDiscretizationFromArray.getNumberOfTimeSteps()][liborPeriodDiscretization.getNumberOfTimeSteps()];
		for (int timeIndex = 0; timeIndex < volatility.length; timeIndex++) {
			for (int liborIndex = 0; liborIndex < volatility[timeIndex].length; liborIndex++) {
				// Create a very simple volatility model here
				final double time = timeDiscretizationFromArray.getTime(timeIndex);
				final double maturity = liborPeriodDiscretization.getTime(liborIndex);
				final double timeToMaturity = maturity - time;

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
		final LIBORVolatilityModelFromGivenMatrix volatilityModel = new LIBORVolatilityModelFromGivenMatrix(timeDiscretizationFromArray, liborPeriodDiscretization, volatility);

		/*
		 * Create a correlation model rho_{i,j} = exp(-a * abs(T_i-T_j))
		 */
		final int numberOfFactors = 5;
		final double correlationDecayParam = 0.2;
		final LIBORCorrelationModelExponentialDecay correlationModel = new LIBORCorrelationModelExponentialDecay(
				timeDiscretizationFromArray, liborPeriodDiscretization, numberOfFactors,
				correlationDecayParam);


		/*
		 * Combine volatility model and correlation model to a covariance model
		 */
		final LIBORCovarianceModelFromVolatilityAndCorrelation covarianceModel =
				new LIBORCovarianceModelFromVolatilityAndCorrelation(timeDiscretizationFromArray,
						liborPeriodDiscretization, volatilityModel, correlationModel);

		// BlendedLocalVolatlityModel (future extension)
		//		AbstractLIBORCovarianceModel covarianceModel2 = new BlendedLocalVolatlityModel(covarianceModel, 0.00, false);

		// Set model properties
		final Map<String, String> properties = new HashMap<>();

		// Choose the simulation measure
		properties.put("measure", measure.name());

		// Choose log normal model
		properties.put("stateSpace", LIBORMarketModelFromCovarianceModel.StateSpace.LOGNORMAL.name());

		// Empty array of calibration items - hence, model will use given covariance
		final CalibrationProduct[] calibrationItems = new CalibrationProduct[0];

		/*
		 * Create corresponding LIBOR Market Model
		 */
		final LIBORMarketModel liborMarketModel = new LIBORMarketModelFromCovarianceModel(
				liborPeriodDiscretization, model, forwardCurve, discountCurve, covarianceModel, calibrationItems, properties);

		final EulerSchemeFromProcessModel process = new EulerSchemeFromProcessModel(liborMarketModel,
				new net.finmath.montecarlo.BrownianMotionLazyInit(timeDiscretizationFromArray,
						numberOfFactors, numberOfPaths, 3141 /* seed */), EulerSchemeFromProcessModel.Scheme.PREDICTOR_CORRECTOR);

		return new LIBORMonteCarloSimulationFromLIBORModel(liborMarketModel, process);
	}
}
