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
import net.finmath.functions.AnalyticFormulas;
import net.finmath.marketdata.model.curves.DiscountCurveFromForwardCurve;
import net.finmath.marketdata.model.curves.DiscountCurveInterface;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurveFromDiscountCurve;
import net.finmath.marketdata.model.curves.ForwardCurveInterface;
import net.finmath.montecarlo.BrownianMotionInterface;
import net.finmath.montecarlo.interestrate.modelplugins.AbstractLIBORCovarianceModel;
import net.finmath.montecarlo.interestrate.modelplugins.HullWhiteLocalVolatilityModel;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORCorrelationModelExponentialDecay;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORCovarianceModelFromVolatilityAndCorrelation;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORVolatilityModelFromGivenMatrix;
import net.finmath.montecarlo.interestrate.modelplugins.ShortRateVolailityModelInterface;
import net.finmath.montecarlo.interestrate.modelplugins.ShortRateVolatilityModel;
import net.finmath.montecarlo.interestrate.products.BermudanSwaption;
import net.finmath.montecarlo.interestrate.products.Bond;
import net.finmath.montecarlo.interestrate.products.Caplet;
import net.finmath.montecarlo.interestrate.products.SimpleSwap;
import net.finmath.montecarlo.interestrate.products.SimpleZeroSwap;
import net.finmath.montecarlo.interestrate.products.SwapLeg;
import net.finmath.montecarlo.interestrate.products.Swaption;
import net.finmath.montecarlo.interestrate.products.SwaptionAnalyticApproximation;
import net.finmath.montecarlo.interestrate.products.components.AbstractNotional;
import net.finmath.montecarlo.interestrate.products.components.Notional;
import net.finmath.montecarlo.interestrate.products.components.Numeraire;
import net.finmath.montecarlo.interestrate.products.components.Option;
import net.finmath.montecarlo.interestrate.products.indices.AbstractIndex;
import net.finmath.montecarlo.interestrate.products.indices.CappedFlooredIndex;
import net.finmath.montecarlo.interestrate.products.indices.ConstantMaturitySwaprate;
import net.finmath.montecarlo.interestrate.products.indices.FixedCoupon;
import net.finmath.montecarlo.interestrate.products.indices.LIBORIndex;
import net.finmath.montecarlo.interestrate.products.indices.LaggedIndex;
import net.finmath.montecarlo.interestrate.products.indices.LinearCombinationIndex;
import net.finmath.montecarlo.process.ProcessEulerScheme;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.ScheduleGenerator;
import net.finmath.time.ScheduleInterface;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingTARGETHolidays;

/**
 * This class tests the Hull White model and products.
 *
 * It also compares a Hull White model to a special parameterization of the
 * LIBOR Market model, illustrating that a special parameterization of the
 * LIBOR Market model is equivalent to the Hull White model.
 *
 * @author Christian Fries
 */
public class HullWhiteModelTest {

	private final int numberOfPaths		= 20000;

	// LMM parameters
	private final int numberOfFactors	= 1;		// For LMM Model.
	private final double correlationDecay = 0.0;	// For LMM Model. If 1 factor, parameter has no effect.

	// Hull White parameters (example: sigma = 0.02, a = 0.1 or sigma = 0.05, a = 0.5)
	private final double shortRateVolatility = 0.02;	// Investigating LIBOR in Arrears, use a high volatility here (e.g. 0.1)
	private final double shortRateMeanreversion = 0.1;

	private LIBORModelMonteCarloSimulationInterface hullWhiteModelSimulation;
	private LIBORModelMonteCarloSimulationInterface liborMarketModelSimulation;

	private static DecimalFormat formatterMaturity	= new DecimalFormat("00.00", new DecimalFormatSymbols(Locale.ENGLISH));
	private static DecimalFormat formatterValue		= new DecimalFormat(" ##0.000%;-##0.000%", new DecimalFormatSymbols(Locale.ENGLISH));
	private static DecimalFormat formatterDeviation	= new DecimalFormat(" 0.00000E00;-0.00000E00", new DecimalFormatSymbols(Locale.ENGLISH));

	public HullWhiteModelTest() throws CalculationException {
		initModels();
		System.out.println("Initialized models:");
		System.out.println("\t" + hullWhiteModelSimulation.getModel().getClass().getName());
		System.out.println("\t" + liborMarketModelSimulation.getModel().getClass().getName());
	}

	public void initModels() throws CalculationException {
		/*
		 * Create the libor tenor structure and the initial values
		 */
		double liborPeriodLength	= 0.5;
		double liborRateTimeHorzion	= 20.0;
		TimeDiscretization liborPeriodDiscretization = new TimeDiscretization(0.0, (int) (liborRateTimeHorzion / liborPeriodLength), liborPeriodLength);

		// Create the forward curve (initial value of the LIBOR market model)
		ForwardCurveInterface forwardCurve = ForwardCurve.createForwardCurveFromForwards(
				"forwardCurve"								/* name of the curve */,
				LocalDate.of(2014, Month.AUGUST, 12),
				"6M",
				ForwardCurve.InterpolationEntityForward.FORWARD,
				null,
				null,
				new double[] {0.5 , 1.0 , 2.0 , 5.0 , 40.0}	/* fixings of the forward */,
				new double[] {0.05, 0.05, 0.05, 0.05, 0.05}	/* forwards */
				);

		// Create the discount curve
		DiscountCurveInterface discountCurve = new DiscountCurveFromForwardCurve(forwardCurve);

		/*
		 * Create a simulation time discretization
		 */
		double lastTime	= 20.0;
		double dt		= 0.5;

		TimeDiscretization timeDiscretization = new TimeDiscretization(0.0, (int) (lastTime / dt), dt);

		/*
		 * Create corresponding Hull White model
		 */
		{
			/*
			 * Create a volatility model: Hull white with constant coefficients (non time dep.).
			 */
			ShortRateVolailityModelInterface volatilityModel = new ShortRateVolatilityModel(
					new TimeDiscretization(0.0),
					new double[] { shortRateVolatility } /* volatility */,
					new double[] { shortRateMeanreversion } /* meanReversion */);

			LIBORModelInterface hullWhiteModel = new HullWhiteModel(
					liborPeriodDiscretization, null, forwardCurve, null /*discountCurve*/, volatilityModel, null);

			BrownianMotionInterface brownianMotion = new net.finmath.montecarlo.BrownianMotion(timeDiscretization, 2 /* numberOfFactors */, numberOfPaths, 3141 /* seed */);

			ProcessEulerScheme process = new ProcessEulerScheme(brownianMotion, ProcessEulerScheme.Scheme.EULER);

			hullWhiteModelSimulation = new LIBORModelMonteCarloSimulation(hullWhiteModel, process);
		}

		/*
		 * Create corresponding LIBOR Market model
		 */
		{
			/*
			 * Create a volatility structure v[i][j] = sigma_j(t_i)
			 */
			double[][] volatility = new double[timeDiscretization.getNumberOfTimeSteps()][liborPeriodDiscretization.getNumberOfTimeSteps()];
			for (int timeIndex = 0; timeIndex < volatility.length; timeIndex++) {
				for (int liborIndex = 0; liborIndex < volatility[timeIndex].length; liborIndex++) {
					// Create a very simple volatility model here
					double time = timeDiscretization.getTime(timeIndex);
					double time2 = timeDiscretization.getTime(timeIndex+1);
					double maturity = liborPeriodDiscretization.getTime(liborIndex);
					double maturity2 = liborPeriodDiscretization.getTime(liborIndex+1);

					double timeToMaturity	= maturity - time;
					double deltaTime		= time2-time;
					double deltaMaturity	= maturity2-maturity;

					double meanReversion = shortRateMeanreversion;

					double instVolatility;
					if(timeToMaturity <= 0) {
						instVolatility = 0;				// This forward rate is already fixed, no volatility
					}
					else {
						instVolatility = shortRateVolatility * Math.exp(-meanReversion * timeToMaturity)
								*
								Math.sqrt((Math.exp(2 * meanReversion * deltaTime) - 1)/ (2 * meanReversion * deltaTime))
								*
								(1-Math.exp(-meanReversion * deltaMaturity))/(meanReversion * deltaMaturity);
					}

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
					correlationDecay);

			/*
			 * Combine volatility model and correlation model to a covariance model
			 */
			LIBORCovarianceModelFromVolatilityAndCorrelation covarianceModel =
					new LIBORCovarianceModelFromVolatilityAndCorrelation(timeDiscretization,
							liborPeriodDiscretization, volatilityModel, correlationModel);

			// BlendedLocalVolatlityModel
			AbstractLIBORCovarianceModel covarianceModel2 = new HullWhiteLocalVolatilityModel(covarianceModel, liborPeriodLength);

			// Set model properties
			Map<String, String> properties = new HashMap<>();

			// Choose the simulation measure
			properties.put("measure", LIBORMarketModel.Measure.SPOT.name());

			// Choose log normal model
			properties.put("stateSpace", LIBORMarketModel.StateSpace.NORMAL.name());

			// Empty array of calibration items - hence, model will use given covariance
			LIBORMarketModel.CalibrationItem[] calibrationItems = new LIBORMarketModel.CalibrationItem[0];

			/*
			 * Create corresponding LIBOR Market Model
			 */
			LIBORMarketModelInterface liborMarketModel = new LIBORMarketModel(
					liborPeriodDiscretization, forwardCurve, null /*discountCurve*/, covarianceModel2, calibrationItems, properties);

			BrownianMotionInterface brownianMotion = new net.finmath.montecarlo.BrownianMotion(timeDiscretization, numberOfFactors, numberOfPaths, 3141 /* seed */);

			ProcessEulerScheme process = new ProcessEulerScheme(brownianMotion, ProcessEulerScheme.Scheme.EULER);

			liborMarketModelSimulation = new LIBORModelMonteCarloSimulation(liborMarketModel, process);
		}
	}

	@Test
	public void testBond() throws CalculationException {
		/*
		 * Value a bond
		 */

		System.out.println("Bond prices:\n");
		System.out.println("Maturity       Simulation (HW)   Simulation (LMM) Analytic         Deviation (HW-LMM)    Deviation (HW-Analytic)");

		long startMillis	= System.currentTimeMillis();

		double maxAbsDeviation = 0.0;
		for (int maturityIndex = 0; maturityIndex <= hullWhiteModelSimulation.getNumberOfLibors(); maturityIndex++) {
			double maturity = hullWhiteModelSimulation.getLiborPeriod(maturityIndex);
			System.out.print(formatterMaturity.format(maturity) + "          ");

			// Create a bond
			Bond bond = new Bond(maturity);

			// Value with Hull-White Model Monte Carlo
			double valueSimulationHW = bond.getValue(hullWhiteModelSimulation);
			System.out.print(formatterValue.format(valueSimulationHW) + "          ");

			// Value with LIBOR Market Model Monte Carlo
			double valueSimulationLMM = bond.getValue(liborMarketModelSimulation);
			System.out.print(formatterValue.format(valueSimulationLMM) + "          ");

			// Bond price analytic
			DiscountCurveInterface discountCurve = hullWhiteModelSimulation.getModel().getDiscountCurve();
			if(discountCurve == null) {
				discountCurve = new DiscountCurveFromForwardCurve(hullWhiteModelSimulation.getModel().getForwardRateCurve());
			}
			double valueAnalytic = discountCurve.getDiscountFactor(maturity);
			System.out.print(formatterValue.format(valueAnalytic) + "          ");

			// Absolute deviation
			double deviationHWLMM = (valueSimulationHW - valueSimulationLMM);
			System.out.print(formatterDeviation.format(deviationHWLMM) + "          ");

			// Absolute deviation
			double deviationHWAnalytic = (valueSimulationHW - valueAnalytic);
			System.out.print(formatterDeviation.format(deviationHWAnalytic) + "          ");

			System.out.println();

			maxAbsDeviation = Math.max(maxAbsDeviation, Math.abs(deviationHWAnalytic));
		}

		long endMillis		= System.currentTimeMillis();

		System.out.println("Maximum abs deviation  : " + formatterDeviation.format(maxAbsDeviation));
		System.out.println("Calculation time (sec) : " + ((endMillis-startMillis) / 1000.0));

		System.out.println("__________________________________________________________________________________________\n");


		// jUnit assertion: condition under which we consider this test successful
		Assert.assertTrue(maxAbsDeviation < 5E-03);
	}

	@Test
	public void testSwap() throws CalculationException {
		/*
		 * Value a swap
		 */

		System.out.println("Par-Swap prices:\n");
		System.out.println("Swap             \tValue (HW)       Value (LMM)     Deviation");

		long startMillis	= System.currentTimeMillis();

		double maxAbsDeviation = 0.0;
		for (int maturityIndex = 1; maturityIndex <= hullWhiteModelSimulation.getNumberOfLibors() - 10; maturityIndex++) {

			double startDate = hullWhiteModelSimulation.getLiborPeriod(maturityIndex);

			int numberOfPeriods = 10;

			// Create a swap
			double[]	fixingDates			= new double[numberOfPeriods];
			double[]	paymentDates		= new double[numberOfPeriods];
			double[]	swapTenor			= new double[numberOfPeriods + 1];
			double		swapPeriodLength	= 0.5;
			String		tenorCode			= "6M";

			for (int periodStartIndex = 0; periodStartIndex < numberOfPeriods; periodStartIndex++) {
				fixingDates[periodStartIndex]	= startDate + periodStartIndex * swapPeriodLength;
				paymentDates[periodStartIndex]	= startDate + (periodStartIndex + 1) * swapPeriodLength;
				swapTenor[periodStartIndex]		= startDate + periodStartIndex * swapPeriodLength;
			}
			swapTenor[numberOfPeriods] = startDate + numberOfPeriods * swapPeriodLength;

			System.out.print("(" + formatterMaturity.format(swapTenor[0]) + "," + formatterMaturity.format(swapTenor[numberOfPeriods-1]) + "," + swapPeriodLength + ")" + "\t");

			// Par swap rate
			double swaprate = getParSwaprate(hullWhiteModelSimulation, swapTenor, tenorCode);

			// Set swap rates for each period
			double[] swaprates = new double[numberOfPeriods];
			for (int periodStartIndex = 0; periodStartIndex < numberOfPeriods; periodStartIndex++) {
				swaprates[periodStartIndex] = swaprate;
			}

			// Create a swap
			SimpleSwap swap = new SimpleSwap(fixingDates, paymentDates, swaprates);

			// Value with Hull-White Model Monte Carlo
			double valueSimulationHW = swap.getValue(hullWhiteModelSimulation);
			System.out.print(formatterValue.format(valueSimulationHW) + "          ");

			// Value with LIBOR Market Model Monte Carlo
			double valueSimulationLMM = swap.getValue(liborMarketModelSimulation);
			System.out.print(formatterValue.format(valueSimulationLMM) + "          ");

			// Absolute deviation
			double deviationHWLMM = (valueSimulationHW - valueSimulationLMM);
			System.out.print(formatterDeviation.format(deviationHWLMM) + "          ");

			System.out.println();

			maxAbsDeviation = Math.max(maxAbsDeviation, Math.abs(valueSimulationHW));
		}

		long endMillis		= System.currentTimeMillis();

		System.out.println("Maximum abs deviation  : " + formatterDeviation.format(maxAbsDeviation));
		System.out.println("Calculation time (sec) : " + ((endMillis-startMillis) / 1000.0));
		System.out.println("__________________________________________________________________________________________\n");

		/*
		 * jUnit assertion: condition under which we consider this test successful
		 * The swap should be at par (close to zero)
		 */
		Assert.assertEquals(0.0, maxAbsDeviation, 1.5E-3);
	}

	@Test
	public void testCaplet() throws CalculationException {
		/*
		 * Value a caplet
		 */
		System.out.println("Caplet prices:\n");
		System.out.println("Maturity       Simulation (HW)   Simulation (LMM) Analytic         Deviation (HW-LMM)    Deviation (HW-Analytic)");

		long startMillis	= System.currentTimeMillis();

		double maxAbsDeviation = 0.0;
		for (int maturityIndex = 1; maturityIndex <= hullWhiteModelSimulation.getNumberOfLibors() - 10; maturityIndex++) {

			double optionMaturity = hullWhiteModelSimulation.getLiborPeriod(maturityIndex);
			System.out.print(formatterMaturity.format(optionMaturity) + "          ");

			double periodStart	= hullWhiteModelSimulation.getLiborPeriod(maturityIndex);
			double periodEnd	= hullWhiteModelSimulation.getLiborPeriod(maturityIndex+1);
			double periodLength	= periodEnd-periodStart;
			double daycountFraction = periodEnd-periodStart;

			String		tenorCode;
			if(periodLength == 0.5) {
				tenorCode = "6M";
			} else if(periodLength == 1.0) {
				tenorCode = "1Y";
			} else {
				throw new IllegalArgumentException("Unsupported period length.");
			}

			double strike = 0.05;

			double forward			= getParSwaprate(hullWhiteModelSimulation, new double[] { periodStart , periodEnd}, tenorCode);
			double discountFactor	= getSwapAnnuity(hullWhiteModelSimulation, new double[] { periodStart , periodEnd}) / periodLength;

			// Create a caplet
			Caplet caplet = new Caplet(optionMaturity, periodLength, strike, daycountFraction, false /* isFloorlet */, Caplet.ValueUnit.VALUE);

			// Value with Hull-White Model Monte Carlo
			double valueSimulationHW = caplet.getValue(hullWhiteModelSimulation);
			valueSimulationHW = AnalyticFormulas.bachelierOptionImpliedVolatility(forward, optionMaturity, strike, discountFactor * periodLength /* payoffUnit */, valueSimulationHW);
			System.out.print(formatterValue.format(valueSimulationHW) + "          ");

			// Value with LIBOR Market Model Monte Carlo
			double valueSimulationLMM = caplet.getValue(liborMarketModelSimulation);
			valueSimulationLMM = AnalyticFormulas.bachelierOptionImpliedVolatility(forward, optionMaturity, strike, discountFactor * periodLength /* payoffUnit */, valueSimulationLMM);
			System.out.print(formatterValue.format(valueSimulationLMM) + "          ");

			// Value with analytic formula
			double forwardBondVolatility = Double.NaN;
			if(hullWhiteModelSimulation.getModel() instanceof HullWhiteModel) {
				forwardBondVolatility = Math.sqrt(((HullWhiteModel)(hullWhiteModelSimulation.getModel())).getIntegratedBondSquaredVolatility(optionMaturity, optionMaturity+periodLength)/optionMaturity);
			}
			else if(hullWhiteModelSimulation.getModel() instanceof HullWhiteModelWithDirectSimulation) {
				forwardBondVolatility = Math.sqrt(((HullWhiteModelWithDirectSimulation)(hullWhiteModelSimulation.getModel())).getIntegratedBondSquaredVolatility(optionMaturity, optionMaturity+periodLength)/optionMaturity);
			}
			else if(hullWhiteModelSimulation.getModel() instanceof HullWhiteModelWithShiftExtension) {
				forwardBondVolatility = Math.sqrt(((HullWhiteModelWithShiftExtension)(hullWhiteModelSimulation.getModel())).getIntegratedBondSquaredVolatility(optionMaturity, optionMaturity+periodLength)/optionMaturity);
			}

			double bondForward = (1.0+forward*periodLength);
			double bondStrike = (1.0+strike*periodLength);

			double zeroBondPut = net.finmath.functions.AnalyticFormulas.blackModelCapletValue(bondForward, forwardBondVolatility, optionMaturity, bondStrike, periodLength, discountFactor);

			double valueAnalytic = zeroBondPut / bondStrike / periodLength;
			valueAnalytic = AnalyticFormulas.bachelierOptionImpliedVolatility(forward, optionMaturity, strike, discountFactor * periodLength /* payoffUnit */, valueAnalytic);
			System.out.print(formatterValue.format(valueAnalytic) + "          ");

			// Absolute deviation
			double deviationHWLMM = (valueSimulationHW - valueSimulationLMM);
			System.out.print(formatterDeviation.format(deviationHWLMM) + "          ");

			// Absolute deviation
			double deviationHWAnalytic = (valueSimulationHW - valueAnalytic);
			System.out.print(formatterDeviation.format(deviationHWAnalytic) + "          ");

			System.out.println();

			maxAbsDeviation = Math.max(maxAbsDeviation, Math.abs(deviationHWLMM));
			if(!Double.isNaN(valueAnalytic)) {
				maxAbsDeviation = Math.max(maxAbsDeviation, Math.abs(deviationHWAnalytic));
			}
		}

		long endMillis		= System.currentTimeMillis();

		System.out.println("Maximum abs deviation: " + formatterDeviation.format(maxAbsDeviation));
		System.out.println("Calculation time (sec) : " + ((endMillis-startMillis) / 1000.0));
		System.out.println("__________________________________________________________________________________________\n");

		/*
		 * jUnit assertion: condition under which we consider this test successful
		 */
		Assert.assertTrue(Math.abs(maxAbsDeviation) < 1E-3);
	}

	@Test
	public void testSwaption() throws CalculationException {
		/*
		 * Value a swaption
		 */
		System.out.println("Swaption prices:\n");
		System.out.println("Maturity      Simulation (HW)  Simulation (LMM) Analytic         Deviation          ");

		long startMillis	= System.currentTimeMillis();

		double maxAbsDeviation = 0.0;
		for (int maturityIndex = 1; maturityIndex <= hullWhiteModelSimulation.getNumberOfLibors() - 10; maturityIndex++) {

			double exerciseDate = hullWhiteModelSimulation.getLiborPeriod(maturityIndex);
			System.out.print(formatterMaturity.format(exerciseDate) + "          ");

			int numberOfPeriods = 5;

			// Create a swaption

			double[] fixingDates = new double[numberOfPeriods];
			double[] paymentDates = new double[numberOfPeriods];
			double[] swapTenor = new double[numberOfPeriods + 1];
			double swapPeriodLength = 0.5;
			String tenorCode = "6M";

			for (int periodStartIndex = 0; periodStartIndex < numberOfPeriods; periodStartIndex++) {
				fixingDates[periodStartIndex] = exerciseDate + periodStartIndex * swapPeriodLength;
				paymentDates[periodStartIndex] = exerciseDate + (periodStartIndex + 1) * swapPeriodLength;
				swapTenor[periodStartIndex] = exerciseDate + periodStartIndex * swapPeriodLength;
			}
			swapTenor[numberOfPeriods] = exerciseDate + numberOfPeriods * swapPeriodLength;

			// Swaptions swap rate
			double swaprate = getParSwaprate(hullWhiteModelSimulation, swapTenor, tenorCode);
			double swapAnnuity = getSwapAnnuity(hullWhiteModelSimulation, swapTenor);

			// Set swap rates for each period
			double[] swaprates = new double[numberOfPeriods];
			for (int periodStartIndex = 0; periodStartIndex < numberOfPeriods; periodStartIndex++) {
				swaprates[periodStartIndex] = swaprate;
			}

			// Create a swaption
			Swaption swaptionMonteCarlo	= new Swaption(exerciseDate, fixingDates, paymentDates, swaprates);

			// Value with Hull-White Model Monte Carlo
			double valueSimulationHW = swaptionMonteCarlo.getValue(hullWhiteModelSimulation);
			System.out.print(formatterValue.format(valueSimulationHW) + "          ");

			// Value with LIBOR Market Model Monte Carlo
			double valueSimulationLMM = swaptionMonteCarlo.getValue(liborMarketModelSimulation);
			System.out.print(formatterValue.format(valueSimulationLMM) + "          ");

			// Value with analytic formula (approximate, using Bachelier formula)
			SwaptionAnalyticApproximation swaptionAnalytic = new SwaptionAnalyticApproximation(swaprate, swapTenor, SwaptionAnalyticApproximation.ValueUnit.VOLATILITY);
			double volatilityAnalytic = swaptionAnalytic.getValue(liborMarketModelSimulation);
			double valueAnalytic = AnalyticFormulas.bachelierOptionValue(swaprate, volatilityAnalytic, exerciseDate, swaprate, swapAnnuity);
			System.out.print(formatterValue.format(valueAnalytic) + "          ");

			// Absolute deviation
			double deviationHWLMM = (valueSimulationHW - valueSimulationLMM);
			System.out.print(formatterDeviation.format(deviationHWLMM) + "          ");

			System.out.println();

			maxAbsDeviation = Math.max(maxAbsDeviation, Math.abs(deviationHWLMM));
		}

		long endMillis		= System.currentTimeMillis();

		System.out.println("Maximum abs deviation  : " + formatterDeviation.format(maxAbsDeviation));
		System.out.println("Calculation time (sec) : " + ((endMillis-startMillis) / 1000.0));
		System.out.println("__________________________________________________________________________________________\n");

		/*
		 * jUnit assertion: condition under which we consider this test successful
		 */
		Assert.assertTrue(Math.abs(maxAbsDeviation) < 8E-3);
	}

	@Test
	public void testBermudanSwaption() throws CalculationException {
		/*
		 * Value a swaption
		 */
		System.out.println("Bermudan Swaption prices:\n");
		System.out.println("Maturity      Simulation(HW)  Simulation(LMM)  AnalyticSwaption  Deviation          ");

		long startMillis	= System.currentTimeMillis();

		double maxAbsDeviation = 0.0;
		for (int maturityIndex = 1; maturityIndex <= hullWhiteModelSimulation.getNumberOfLibors() - 10; maturityIndex++) {

			double exerciseDate = hullWhiteModelSimulation.getLiborPeriod(maturityIndex);
			System.out.print(formatterMaturity.format(exerciseDate) + "          ");

			int numberOfPeriods = 5;

			// Create a swaption

			double[] fixingDates = new double[numberOfPeriods];
			double[] paymentDates = new double[numberOfPeriods];
			double[] swapTenor = new double[numberOfPeriods + 1];
			double swapPeriodLength = 0.5;
			String tenorCode = "6M";

			for (int periodStartIndex = 0; periodStartIndex < numberOfPeriods; periodStartIndex++) {
				fixingDates[periodStartIndex] = exerciseDate + periodStartIndex * swapPeriodLength;
				paymentDates[periodStartIndex] = exerciseDate + (periodStartIndex + 1) * swapPeriodLength;
				swapTenor[periodStartIndex] = exerciseDate + periodStartIndex * swapPeriodLength;
			}
			swapTenor[numberOfPeriods] = exerciseDate + numberOfPeriods * swapPeriodLength;

			// Swaptions swap rate
			double swaprate = getParSwaprate(hullWhiteModelSimulation, swapTenor, tenorCode);
			double swapAnnuity = getSwapAnnuity(hullWhiteModelSimulation, swapTenor);

			// Set swap rates for each period
			double[] swaprates = new double[numberOfPeriods];
			Arrays.fill(swaprates, swaprate);

			double[] periodLength = new double[numberOfPeriods];
			Arrays.fill(periodLength, swapPeriodLength);

			double[] periodNotionals = new double[numberOfPeriods];
			Arrays.fill(periodNotionals, 1.0);

			boolean[] isPeriodStartDateExerciseDate = new boolean[numberOfPeriods];
			Arrays.fill(isPeriodStartDateExerciseDate, true);

			// Create a bermudan swaption
			BermudanSwaption bermudanSwaption = new BermudanSwaption(isPeriodStartDateExerciseDate, fixingDates, periodLength, paymentDates, periodNotionals, swaprates);

			// Value with Hull-White Model Monte Carlo
			double valueSimulationHW = bermudanSwaption.getValue(hullWhiteModelSimulation);
			System.out.print(formatterValue.format(valueSimulationHW) + "          ");

			// Value with LIBOR Market Model Monte Carlo
			double valueSimulationLMM = bermudanSwaption.getValue(liborMarketModelSimulation);
			System.out.print(formatterValue.format(valueSimulationLMM) + "          ");

			// Value the underlying swaption with analytic formula (approximate, using Bachelier formula)
			SwaptionAnalyticApproximation swaptionAnalytic = new SwaptionAnalyticApproximation(swaprate, swapTenor, SwaptionAnalyticApproximation.ValueUnit.VOLATILITY);
			double volatilityAnalytic = swaptionAnalytic.getValue(liborMarketModelSimulation);
			double valueAnalytic = AnalyticFormulas.bachelierOptionValue(swaprate, volatilityAnalytic, exerciseDate, swaprate, swapAnnuity);
			System.out.print(formatterValue.format(valueAnalytic) + "          ");

			// Absolute deviation
			double deviationHWLMM = (valueSimulationHW - valueSimulationLMM);
			System.out.print(formatterDeviation.format(deviationHWLMM) + "          ");

			System.out.println();

			maxAbsDeviation = Math.max(maxAbsDeviation, Math.abs(deviationHWLMM));
		}

		long endMillis		= System.currentTimeMillis();

		System.out.println("Maximum abs deviation  : " + formatterDeviation.format(maxAbsDeviation));
		System.out.println("Calculation time (sec) : " + ((endMillis-startMillis) / 1000.0));
		System.out.println("__________________________________________________________________________________________\n");

		/*
		 * jUnit assertion: condition under which we consider this test successful
		 */
		Assert.assertTrue(Math.abs(maxAbsDeviation) < 8E-3);
	}

	@Test
	public void testCapletSmile() throws CalculationException {
		/*
		 * Value a caplet
		 */
		System.out.println("Caplet implied volatilities:\n");
		System.out.println("Strike       Simulation (HW)   Simulation (LMM) Analytic         Deviation (HW-LMM)    Deviation (HW-Analytic)");

		long startMillis	= System.currentTimeMillis();

		double maxAbsDeviation = 0.0;
		double optionMaturity = 5.0;
		double periodLength = 0.5;
		for (double strike = 0.03; strike <= 0.10; strike+=0.01) {

			System.out.print(formatterMaturity.format(strike) + "          ");

			double periodStart		= optionMaturity;
			double periodEnd		= optionMaturity+periodLength;
			double daycountFraction = periodEnd-periodStart;

			String		tenorCode;
			if(periodLength == 0.5) {
				tenorCode = "6M";
			} else if(periodLength == 1.0) {
				tenorCode = "1Y";
			} else {
				throw new IllegalArgumentException("Unsupported period length.");
			}

			double forward			= getParSwaprate(hullWhiteModelSimulation, new double[] { periodStart , periodEnd}, tenorCode);
			double discountFactor	= getSwapAnnuity(hullWhiteModelSimulation, new double[] { periodStart , periodEnd}) / periodLength;

			// Create a caplet
			Caplet caplet = new Caplet(optionMaturity, periodLength, strike, daycountFraction, false /* isFloorlet */, Caplet.ValueUnit.VALUE);

			// Value with Hull-White Model Monte Carlo
			double valueSimulationHW = caplet.getValue(hullWhiteModelSimulation);
			valueSimulationHW = AnalyticFormulas.bachelierOptionImpliedVolatility(forward, optionMaturity, strike, discountFactor * periodLength /* payoffUnit */, valueSimulationHW);
			System.out.print(formatterValue.format(valueSimulationHW) + "          ");

			// Value with LIBOR Market Model Monte Carlo
			double valueSimulationLMM = caplet.getValue(liborMarketModelSimulation);
			valueSimulationLMM = AnalyticFormulas.bachelierOptionImpliedVolatility(forward, optionMaturity, strike, discountFactor * periodLength /* payoffUnit */, valueSimulationLMM);
			System.out.print(formatterValue.format(valueSimulationLMM) + "          ");

			// Value analytic
			double forwardBondVolatility = shortRateVolatility*(1-Math.exp(-shortRateMeanreversion*periodLength))/(shortRateMeanreversion)*Math.sqrt((1-Math.exp(-2*shortRateMeanreversion*optionMaturity))/(2*shortRateMeanreversion*optionMaturity));
			double bondForward = (1.0+forward*periodLength);
			double bondStrike = (1.0+strike*periodLength);

			double zeroBondPut = net.finmath.functions.AnalyticFormulas.blackModelCapletValue(bondForward, forwardBondVolatility, optionMaturity, bondStrike, periodLength, discountFactor);
			double valueAnalytic = zeroBondPut / bondStrike / periodLength;
			valueAnalytic = AnalyticFormulas.bachelierOptionImpliedVolatility(forward, optionMaturity, strike, discountFactor * periodLength /* payoffUnit */, valueAnalytic);
			System.out.print(formatterValue.format(valueAnalytic) + "          ");

			// Absolute deviation
			double deviationHWLMM = (valueSimulationHW - valueSimulationLMM);
			System.out.print(formatterDeviation.format(deviationHWLMM) + "          ");

			// Absolute deviation
			double deviationHWAnalytic = (valueSimulationHW - valueAnalytic);
			System.out.print(formatterDeviation.format(deviationHWAnalytic) + "          ");

			System.out.println();

			maxAbsDeviation = Math.max(maxAbsDeviation, Math.abs(deviationHWAnalytic));
		}

		long endMillis		= System.currentTimeMillis();

		System.out.println("Maximum abs deviation  : " + formatterDeviation.format(maxAbsDeviation));
		System.out.println("Calculation time (sec) : " + ((endMillis-startMillis) / 1000.0));
		System.out.println("__________________________________________________________________________________________\n");

		/*
		 * jUnit assertion: condition under which we consider this test successful
		 */
		Assert.assertTrue(Math.abs(maxAbsDeviation) < 1E-3);
	}


	@Test
	public void testZeroCMSSwap() throws CalculationException {
		/*
		 * Value a swap
		 */

		System.out.println("Zero-CMS-Swap prices:\n");
		System.out.println("Swap             \tValue (HW)       Value (LMM)     Deviation");

		long startMillis	= System.currentTimeMillis();

		double maxAbsDeviation = 0.0;
		for (int maturityIndex = 1; maturityIndex <= hullWhiteModelSimulation.getNumberOfLibors()-10-20; maturityIndex++) {

			double startDate = hullWhiteModelSimulation.getLiborPeriod(maturityIndex);

			int numberOfPeriods = 10;

			// Create a swap
			double[]	fixingDates			= new double[numberOfPeriods];
			double[]	paymentDates		= new double[numberOfPeriods];
			double[]	swapTenor			= new double[numberOfPeriods + 1];
			double		swapPeriodLength	= 0.5;
			String		tenorCode			= "6M";

			for (int periodStartIndex = 0; periodStartIndex < numberOfPeriods; periodStartIndex++) {
				fixingDates[periodStartIndex]	= startDate + periodStartIndex * swapPeriodLength;
				paymentDates[periodStartIndex]	= startDate + (periodStartIndex + 1) * swapPeriodLength;
				swapTenor[periodStartIndex]		= startDate + periodStartIndex * swapPeriodLength;
			}
			swapTenor[numberOfPeriods] = startDate + numberOfPeriods * swapPeriodLength;

			System.out.print("(" + formatterMaturity.format(swapTenor[0]) + "," + formatterMaturity.format(swapTenor[numberOfPeriods-1]) + "," + swapPeriodLength + ")" + "\t");

			// Par swap rate
			double swaprate = getParSwaprate(hullWhiteModelSimulation, swapTenor, tenorCode);

			// Set swap rates for each period
			double[] swaprates = new double[numberOfPeriods];
			for (int periodStartIndex = 0; periodStartIndex < numberOfPeriods; periodStartIndex++) {
				swaprates[periodStartIndex] = swaprate;
			}

			// Create a swap
			AbstractIndex index = new ConstantMaturitySwaprate(10.0, 0.5);
			index = new CappedFlooredIndex(index, new FixedCoupon(0.1) /* cap */, new FixedCoupon(0.04) /* Floor */);
			SimpleZeroSwap swap = new SimpleZeroSwap(fixingDates, paymentDates, swaprates, index, true);

			// Value the swap
			double valueSimulationHW = swap.getValue(hullWhiteModelSimulation);
			System.out.print(formatterValue.format(valueSimulationHW) + "          ");

			double valueSimulationLMM = swap.getValue(liborMarketModelSimulation);
			System.out.print(formatterValue.format(valueSimulationLMM) + "          ");

			// Absolute deviation
			double deviationHWLMM = (valueSimulationHW - valueSimulationLMM);
			System.out.print(formatterDeviation.format(deviationHWLMM) + "          ");

			System.out.println();

			maxAbsDeviation = Math.max(maxAbsDeviation, Math.abs(deviationHWLMM));
		}

		long endMillis		= System.currentTimeMillis();

		System.out.println("Maximum abs deviation  : " + formatterDeviation.format(maxAbsDeviation));
		System.out.println("Calculation time (sec) : " + ((endMillis-startMillis) / 1000.0));
		System.out.println("__________________________________________________________________________________________\n");

		/*
		 * jUnit assertion: condition under which we consider this test successful
		 */
		Assert.assertTrue(maxAbsDeviation < 1E-3);
	}

	@Test
	public void testLIBORInArrearsFloatLeg() throws CalculationException {

		/*
		 * Create a payment schedule from conventions
		 */
		LocalDate	referenceDate = LocalDate.of(2014, Month.AUGUST, 12);
		int			spotOffsetDays = 2;
		String		forwardStartPeriod = "6M";
		String		maturity = "6M";
		String		frequency = "semiannual";
		String		daycountConvention = "30/360";

		ScheduleInterface schedule = ScheduleGenerator.createScheduleFromConventions(referenceDate, spotOffsetDays, forwardStartPeriod, maturity, frequency, daycountConvention, "first", "following", new BusinessdayCalendarExcludingTARGETHolidays(), -2, 0);

		/*
		 * Create the leg with a notional and index
		 */
		AbstractNotional notional = new Notional(1.0);
		AbstractIndex liborIndex = new LIBORIndex(0.0, 0.5);
		AbstractIndex index = new LinearCombinationIndex(-1.0, liborIndex, +1, new LaggedIndex(liborIndex, +0.5 /* fixingOffset */));
		double spread = 0.0;

		SwapLeg leg = new SwapLeg(schedule, notional, index, spread, false /* isNotionalExchanged */);

		System.out.println("LIBOR in Arrears Swap prices:\n");
		System.out.println("Value (HW)               Value (LMM)              Deviation");

		// Value the swap
		RandomVariableInterface valueSimulationHW = leg.getValue(0,hullWhiteModelSimulation);
		System.out.print(formatterValue.format(valueSimulationHW.getAverage()) + " " + formatterValue.format(valueSimulationHW.getStandardError()) + "          ");

		RandomVariableInterface valueSimulationLMM = leg.getValue(0,liborMarketModelSimulation);
		System.out.print(formatterValue.format(valueSimulationLMM.getAverage()) + " " + formatterValue.format(valueSimulationLMM.getStandardError()) + "          ");

		// Absolute deviation
		double deviationHWLMM = (valueSimulationHW.getAverage() - valueSimulationLMM.getAverage());
		System.out.print(formatterDeviation.format(deviationHWLMM) + "          ");

		System.out.println();

		System.out.println("__________________________________________________________________________________________\n");

		/*
		 * jUnit assertion: condition under which we consider this test successful
		 */
		Assert.assertTrue(deviationHWLMM < 1E-3);
	}

	@Test
	public void testPutOnMoneyMarketAccount() throws CalculationException {

		/*
		 * Create the product
		 */
		Option product = new Option(0.5, 1.025, new Numeraire());

		System.out.println("Put-on-Money-Market-Account prices:\n");
		System.out.println("Value (HW)               Value (LMM)              Deviation");

		// Value the product
		RandomVariableInterface valueSimulationHW = product.getValue(0,hullWhiteModelSimulation);
		System.out.print(formatterValue.format(valueSimulationHW.getAverage()) + " " + formatterValue.format(valueSimulationHW.getStandardError()) + "          ");

		RandomVariableInterface valueSimulationLMM = product.getValue(0,liborMarketModelSimulation);
		System.out.print(formatterValue.format(valueSimulationLMM.getAverage()) + " " + formatterValue.format(valueSimulationLMM.getStandardError()) + "          ");

		// Absolute deviation
		double deviationHWLMM = (valueSimulationHW.getAverage() - valueSimulationLMM.getAverage());
		System.out.print(formatterDeviation.format(deviationHWLMM) + "          ");

		System.out.println();

		System.out.println("__________________________________________________________________________________________\n");

		/*
		 * jUnit assertion: condition under which we consider this test successful
		 */
		Assert.assertTrue(deviationHWLMM >= 0);
	}

	private static double getParSwaprate(LIBORModelMonteCarloSimulationInterface liborMarketModel, double[] swapTenor, String tenorCode) {
		DiscountCurveInterface modelCurve = new DiscountCurveFromForwardCurve(liborMarketModel.getModel().getForwardRateCurve());
		ForwardCurveInterface forwardCurve = new ForwardCurveFromDiscountCurve(modelCurve.getName(), liborMarketModel.getModel().getForwardRateCurve().getReferenceDate(), tenorCode);
		return net.finmath.marketdata.products.Swap.getForwardSwapRate(new TimeDiscretization(swapTenor), new TimeDiscretization(swapTenor),
				forwardCurve,
				modelCurve);
	}

	private static double getSwapAnnuity(LIBORModelMonteCarloSimulationInterface liborMarketModel, double[] swapTenor) {
		DiscountCurveInterface discountCurve = liborMarketModel.getModel().getDiscountCurve();
		if(discountCurve == null) {
			discountCurve = new DiscountCurveFromForwardCurve(liborMarketModel.getModel().getForwardRateCurve());
		}
		return net.finmath.marketdata.products.SwapAnnuity.getSwapAnnuity(new TimeDiscretization(swapTenor), discountCurve);
	}
}
