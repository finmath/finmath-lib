/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 10.02.2004
 */
package net.finmath.montecarlo.interestrate;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.threeten.bp.LocalDate;
import org.threeten.bp.Month;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.DiscountCurveInterface;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurveInterface;
import net.finmath.marketdata.model.volatilities.AbstractVolatilitySurface;
import net.finmath.marketdata.model.volatilities.CapletVolatilitiesParametric;
import net.finmath.marketdata.products.Cap;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORCorrelationModelExponentialDecay;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORCovarianceModelFromVolatilityAndCorrelation;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORVolatilityModelFourParameterExponentialFormIntegrated;
import net.finmath.montecarlo.interestrate.products.FlexiCap;
import net.finmath.montecarlo.process.ProcessEulerScheme;
import net.finmath.time.RegularSchedule;
import net.finmath.time.ScheduleInterface;
import net.finmath.time.TimeDiscretization;

/**
 * This class tests the valuation of a Cap using LMM and an AnalyticModel.
 *
 * @author Christian Fries
 */
public class CapValuationTest {

	LocalDate referenceDate = LocalDate.of(2014, Month.JUNE, 15);

	private ForwardCurveInterface					forwardCurve;
	private DiscountCurveInterface					discountCurve;
	private LIBORModelMonteCarloSimulationInterface	liborMarketModel;
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
	private void init(int numberOfPaths) throws CalculationException {

		/*
		 * Create the libor tenor structure and the initial values
		 */
		double liborPeriodLength	= 0.25;
		double liborRateTimeHorzion	= 20.0;
		TimeDiscretization liborPeriodDiscretization = new TimeDiscretization(0.0, (int) (liborRateTimeHorzion / liborPeriodLength), liborPeriodLength);

		// Create the forward curve (initial value of the LIBOR market model)
		forwardCurve = ForwardCurve.createForwardCurveFromForwards(
				"forwardCurve"								/* name of the curve */,
				new double[] {0.5 , 1.0 , 2.0 , 5.0 , 40.0}	/* fixings of the forward */,
				new double[] {0.05, 0.05, 0.05, 0.05, 0.05}	/* forwards */,
				liborPeriodLength							/* tenor / period length */
				);

		// Create the discount curve
		discountCurve = DiscountCurve.createDiscountCurveFromZeroRates(
				"discountCurve"								/* name of the curve */,
				new double[] {0.5 , 1.0 , 2.0 , 5.0 , 40.0}	/* maturities */,
				new double[] {0.04, 0.04, 0.04, 0.04, 0.05}	/* zero rates */
				);

		// Create the capletVolatilitySurface
		double a = 0.25;
		double b = 3.00;
		double c = 1.50;
		double d = 0.10;
		capletVol = new CapletVolatilitiesParametric("EUR", referenceDate, a, b, c, d);
		//		capletVol = new CapletVolatilitiesParametricFourParameterPicewiseConstant("EUR", referenceDate, a, b, c, d, liborPeriodDiscretization);


		/*
		 * Create a simulation time discretization
		 */
		double lastTime	= 20.0;
		double dt		= 0.25;

		TimeDiscretization timeDiscretization = new TimeDiscretization(0.0, (int) (lastTime / dt), dt);

		// LIBOR volatility model
		LIBORVolatilityModelFourParameterExponentialFormIntegrated volatilityModel = new LIBORVolatilityModelFourParameterExponentialFormIntegrated(timeDiscretization, liborPeriodDiscretization, a, b, c, d, false /* isCalibrateable */);
		//		LIBORVolatilityModelFourParameterExponentialForm volatilityModel = new LIBORVolatilityModelFourParameterExponentialForm(timeDiscretization, liborPeriodDiscretization, a, b, c, d, false /* isCalibrateable */);

		/*
		 * Create a correlation model rho_{i,j} = exp(-a * abs(T_i-T_j))
		 */
		int		numberOfFactors = 1;
		double	correlationDecayParam = 0.0;
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
		properties.put("measure", LIBORMarketModel.Measure.SPOT.name());

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
						numberOfFactors, numberOfPaths, 3141 /* seed */));
		//		process.setScheme(ProcessEulerScheme.Scheme.PREDICTOR_CORRECTOR);

		this.liborMarketModel = new LIBORModelMonteCarloSimulation(liborMarketModel, process);
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

			double maturity = liborMarketModel.getLiborPeriod(maturityIndex);
			System.out.print(formatterMaturity.format(maturity) + "          ");

			double strike = 0.05;
			double[] fixingDates	= (new TimeDiscretization(0.25, maturityIndex-2, 0.25)).getAsDoubleArray();
			double[] paymentDates	= (new TimeDiscretization(0.50, maturityIndex-2, 0.25)).getAsDoubleArray();
			double[] strikes		= new double[maturityIndex-1];
			Arrays.fill(strikes, strike);

			// Create a digital caplet
			FlexiCap cap = new FlexiCap(fixingDates, paymentDates, strikes, Integer.MAX_VALUE);

			// Value with Monte Carlo
			double valueSimulation = cap.getValue(liborMarketModel);
			System.out.print(formatterValue.format(valueSimulation) + "          ");

			// Value analytic
			AnalyticModelInterface model = new AnalyticModel();
			model = model.addCurves(forwardCurve);
			model = model.addCurves(discountCurve);
			model = model.addVolatilitySurfaces(capletVol);

			LocalDate startDate = referenceDate.plusMonths(3);

			ScheduleInterface schedule = new RegularSchedule(new TimeDiscretization(0.25, maturityIndex-1, 0.25));
			//			ScheduleInterface schedule = ScheduleGenerator.createScheduleFromConventions(referenceDate.getTime(), startDate.getTime(), "quarterly", (maturityIndex-1)*0.25, "act/365", "first");

			Cap capAnalytic = new Cap(schedule, forwardCurve.getName() /* forwardCurveName */, strike, false /* isStrikeMoneyness */, discountCurve.getName() /* discountCurveName */, "EUR" /* volatiltiySufaceName */);
			double valueAnalytic = capAnalytic.getValue(model);
			System.out.print(formatterValue.format(valueAnalytic) + "          ");

			// Absolute deviation
			double deviation = (valueSimulation - valueAnalytic);
			System.out.println(formatterDeviation.format(deviation) + "          ");

			maxAbsDeviation = Math.max(maxAbsDeviation, Math.abs(deviation));
		}

		System.out.println("Maximum abs deviation: " + formatterDeviation.format(maxAbsDeviation));
		System.out.println("__________________________________________________________________________________________\n");

		/*
		 * jUnit assertion: condition under which we consider this test successful
		 */
		Assert.assertEquals("Deviation", 0.0, maxAbsDeviation, 3E-3);
	}
}
