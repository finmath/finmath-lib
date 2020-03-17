/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 10.02.2004
 */
package net.finmath.montecarlo.interestrate;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.AnalyticModelFromCurvesAndVols;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.DiscountCurveInterpolation;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurveInterpolation;
import net.finmath.marketdata.model.volatilities.AbstractVolatilitySurface;
import net.finmath.marketdata.model.volatilities.CapletVolatilitiesParametric;
import net.finmath.marketdata.products.Cap;
import net.finmath.montecarlo.RandomVariableFromArrayFactory;
import net.finmath.montecarlo.interestrate.models.LIBORMarketModelFromCovarianceModel;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCorrelationModelExponentialDecay;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCovarianceModelFromVolatilityAndCorrelation;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORVolatilityModel;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORVolatilityModelFourParameterExponentialFormIntegrated;
import net.finmath.montecarlo.interestrate.products.FlexiCap;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.time.RegularSchedule;
import net.finmath.time.Schedule;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * This class tests the valuation of a Cap using LMM and an AnalyticModelFromCuvesAndVols.
 *
 * @author Christian Fries
 */
public class CapValuationTest {

	LocalDate referenceDate = LocalDate.of(2014, Month.JUNE, 15);

	private ForwardCurve					forwardCurve;
	private DiscountCurve					discountCurve;
	private LIBORModelMonteCarloSimulationModel	liborMarketModel;
	private AbstractVolatilitySurface				capletVol;

	private static DecimalFormat formatterMaturity	= new DecimalFormat("00.00", new DecimalFormatSymbols(Locale.ENGLISH));
	private static DecimalFormat formatterValue		= new DecimalFormat(" ##0.000%;-##0.000%", new DecimalFormatSymbols(Locale.ENGLISH));
	private static DecimalFormat formatterDeviation	= new DecimalFormat(" 0.00000E00;-0.00000E00", new DecimalFormatSymbols(Locale.ENGLISH));

	public CapValuationTest() throws CalculationException {

		final int numberOfPaths		= 10000;

		// Create a libor market model
		init(numberOfPaths);
	}


	/**
	 * Initialize market data objects and the libor market model object.
	 *
	 * @param numberOfPaths Numer of paths of the LIBOR market model.
	 * @throws CalculationException Thrown if a numerical algorithm fails.
	 */
	private void init(final int numberOfPaths) throws CalculationException {

		/*
		 * Create the libor tenor structure and the initial values
		 */
		final double liborPeriodLength	= 0.25;
		final double liborRateTimeHorzion	= 20.0;
		final TimeDiscretizationFromArray liborPeriodDiscretization = new TimeDiscretizationFromArray(0.0, (int) (liborRateTimeHorzion / liborPeriodLength), liborPeriodLength);

		// Create the forward curve (initial value of the LIBOR market model)
		forwardCurve = ForwardCurveInterpolation.createForwardCurveFromForwards(
				"forwardCurve"								/* name of the curve */,
				new double[] {0.5 , 1.0 , 2.0 , 5.0 , 40.0}	/* fixings of the forward */,
				new double[] {0.05, 0.05, 0.05, 0.05, 0.05}	/* forwards */,
				liborPeriodLength							/* tenor / period length */
				);

		// Create the discount curve
		discountCurve = DiscountCurveInterpolation.createDiscountCurveFromZeroRates(
				"discountCurve"								/* name of the curve */,
				new double[] {0.5 , 1.0 , 2.0 , 5.0 , 40.0}	/* maturities */,
				new double[] {0.04, 0.04, 0.04, 0.04, 0.05}	/* zero rates */
				);

		// Create the capletVolatilitySurface
		final double a = 0.25;
		final double b = 3.00;
		final double c = 1.50;
		final double d = 0.10;
		capletVol = new CapletVolatilitiesParametric("EUR", referenceDate, a, b, c, d);
		//		capletVol = new CapletVolatilitiesParametricFourParameterPicewiseConstant("EUR", referenceDate, a, b, c, d, liborPeriodDiscretization);


		/*
		 * Create a simulation time discretization
		 */
		final double lastTime	= 20.0;
		final double dt		= 0.25;

		final TimeDiscretizationFromArray timeDiscretizationFromArray = new TimeDiscretizationFromArray(0.0, (int) (lastTime / dt), dt);

		// LIBOR volatility model
		final LIBORVolatilityModel volatilityModel = new LIBORVolatilityModelFourParameterExponentialFormIntegrated(timeDiscretizationFromArray, liborPeriodDiscretization, a, b, c, d, false /* isCalibrateable */);
		//		LIBORVolatilityModel volatilityModel = new LIBORVolatilityModelFourParameterExponentialForm(timeDiscretizationFromArray, liborPeriodDiscretization, a, b, c, d, false /* isCalibrateable */);

		/*
		 * Create a correlation model rho_{i,j} = exp(-a * abs(T_i-T_j))
		 */
		final int		numberOfFactors = 1;
		final double	correlationDecayParam = 0.0;
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
		properties.put("measure", LIBORMarketModelFromCovarianceModel.Measure.SPOT.name());

		// Choose log normal model
		properties.put("stateSpace", LIBORMarketModelFromCovarianceModel.StateSpace.LOGNORMAL.name());

		// Empty array of calibration items - hence, model will use given covariance
		final CalibrationProduct[] calibrationItems = new CalibrationProduct[0];

		/*
		 * Create corresponding LIBOR Market Model
		 */
		final LIBORMarketModel liborMarketModel = LIBORMarketModelFromCovarianceModel.of(
				liborPeriodDiscretization, null, forwardCurve, discountCurve, new RandomVariableFromArrayFactory(), covarianceModel, calibrationItems, properties);

		final EulerSchemeFromProcessModel process = new EulerSchemeFromProcessModel(liborMarketModel,
				new net.finmath.montecarlo.BrownianMotionLazyInit(timeDiscretizationFromArray,
						numberOfFactors, numberOfPaths, 3141 /* seed */), EulerSchemeFromProcessModel.Scheme.PREDICTOR_CORRECTOR);

		this.liborMarketModel = new LIBORMonteCarloSimulationFromLIBORModel(liborMarketModel, process);
	}

	@Test
	public void testCap() throws CalculationException {
		/*
		 * Value a set of caps
		 */
		System.out.println("Cap prices:\n");
		System.out.println("Maturity      Simulation       Analytic        Deviation");

		double maxAbsDeviation = 0.0;
		for (int maturityIndex = 2; maturityIndex <= liborMarketModel.getNumberOfLibors() - 1; maturityIndex++) {

			final double maturity = liborMarketModel.getLiborPeriod(maturityIndex);
			System.out.print(formatterMaturity.format(maturity) + "          ");

			final double strike = 0.05;
			final double[] fixingDates	= (new TimeDiscretizationFromArray(0.25, maturityIndex-2, 0.25)).getAsDoubleArray();
			final double[] paymentDates	= (new TimeDiscretizationFromArray(0.50, maturityIndex-2, 0.25)).getAsDoubleArray();
			final double[] strikes		= new double[maturityIndex-1];
			Arrays.fill(strikes, strike);

			// Create a digital caplet
			final FlexiCap cap = new FlexiCap(fixingDates, paymentDates, strikes, Integer.MAX_VALUE);

			// Value with Monte Carlo
			final double valueSimulation = cap.getValue(liborMarketModel);
			System.out.print(formatterValue.format(valueSimulation) + "          ");

			// Value analytic
			AnalyticModel model = new AnalyticModelFromCurvesAndVols();
			model = model.addCurves(forwardCurve);
			model = model.addCurves(discountCurve);
			model = model.addVolatilitySurfaces(capletVol);

			final LocalDate startDate = referenceDate.plusMonths(3);

			final Schedule schedule = new RegularSchedule(new TimeDiscretizationFromArray(0.25, maturityIndex-1, 0.25));
			//			Schedule schedule = ScheduleGenerator.createScheduleFromConventions(referenceDate.getTime(), startDate.getTime(), "quarterly", (maturityIndex-1)*0.25, "act/365", "first");

			final Cap capAnalytic = new Cap(schedule, forwardCurve.getName() /* forwardCurveName */, strike, false /* isStrikeMoneyness */, discountCurve.getName() /* discountCurveName */, "EUR" /* volatiltiySufaceName */);
			final double valueAnalytic = capAnalytic.getValue(model);
			System.out.print(formatterValue.format(valueAnalytic) + "          ");

			// Absolute deviation
			final double deviation = (valueSimulation - valueAnalytic);
			System.out.println(formatterDeviation.format(deviation) + "          ");

			maxAbsDeviation = Math.max(maxAbsDeviation, Math.abs(deviation));
		}

		System.out.println("Maximum abs deviation: " + formatterDeviation.format(maxAbsDeviation));
		System.out.println("__________________________________________________________________________________________\n");

		/*
		 * jUnit assertion: condition under which we consider this test successful
		 */
		Assert.assertEquals("Deviation", 0.0, maxAbsDeviation, 2E-3);
	}
}
