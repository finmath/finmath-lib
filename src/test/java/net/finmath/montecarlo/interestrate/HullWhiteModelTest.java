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
import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.AnalyticModelFromCurvesAndVols;
import net.finmath.marketdata.model.curves.Curve;
import net.finmath.marketdata.model.curves.CurveInterpolation.ExtrapolationMethod;
import net.finmath.marketdata.model.curves.CurveInterpolation.InterpolationEntity;
import net.finmath.marketdata.model.curves.CurveInterpolation.InterpolationMethod;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.DiscountCurveFromForwardCurve;
import net.finmath.marketdata.model.curves.DiscountCurveInterpolation;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurveFromDiscountCurve;
import net.finmath.marketdata.model.curves.ForwardCurveInterpolation;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.interestrate.models.HullWhiteModel;
import net.finmath.montecarlo.interestrate.models.HullWhiteModelWithDirectSimulation;
import net.finmath.montecarlo.interestrate.models.HullWhiteModelWithShiftExtension;
import net.finmath.montecarlo.interestrate.models.LIBORMarketModelFromCovarianceModel;
import net.finmath.montecarlo.interestrate.models.covariance.HullWhiteLocalVolatilityModel;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCorrelationModelExponentialDecay;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCovarianceModel;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCovarianceModelFromVolatilityAndCorrelation;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORVolatilityModelFromGivenMatrix;
import net.finmath.montecarlo.interestrate.models.covariance.ShortRateVolatilityModel;
import net.finmath.montecarlo.interestrate.models.covariance.ShortRateVolatilityModelAsGiven;
import net.finmath.montecarlo.interestrate.products.AbstractTermStructureMonteCarloProduct;
import net.finmath.montecarlo.interestrate.products.BermudanSwaption;
import net.finmath.montecarlo.interestrate.products.Bond;
import net.finmath.montecarlo.interestrate.products.Caplet;
import net.finmath.montecarlo.interestrate.products.SimpleZeroSwap;
import net.finmath.montecarlo.interestrate.products.Swap;
import net.finmath.montecarlo.interestrate.products.SwapLeg;
import net.finmath.montecarlo.interestrate.products.Swaption;
import net.finmath.montecarlo.interestrate.products.SwaptionAnalyticApproximation;
import net.finmath.montecarlo.interestrate.products.TermStructureMonteCarloProduct;
import net.finmath.montecarlo.interestrate.products.components.Notional;
import net.finmath.montecarlo.interestrate.products.components.NotionalFromConstant;
import net.finmath.montecarlo.interestrate.products.components.Numeraire;
import net.finmath.montecarlo.interestrate.products.components.Option;
import net.finmath.montecarlo.interestrate.products.indices.AbstractIndex;
import net.finmath.montecarlo.interestrate.products.indices.CappedFlooredIndex;
import net.finmath.montecarlo.interestrate.products.indices.ConstantMaturitySwaprate;
import net.finmath.montecarlo.interestrate.products.indices.FixedCoupon;
import net.finmath.montecarlo.interestrate.products.indices.LIBORIndex;
import net.finmath.montecarlo.interestrate.products.indices.LaggedIndex;
import net.finmath.montecarlo.interestrate.products.indices.LinearCombinationIndex;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.Schedule;
import net.finmath.time.ScheduleGenerator;
import net.finmath.time.ScheduleGenerator.DaycountConvention;
import net.finmath.time.ScheduleGenerator.Frequency;
import net.finmath.time.ScheduleGenerator.ShortPeriodConvention;
import net.finmath.time.SchedulePrototype;
import net.finmath.time.TimeDiscretizationFromArray;
import net.finmath.time.businessdaycalendar.BusinessdayCalendar;
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
	private final double shortRateVolatility = 0.005;	// Investigating LIBOR in Arrears, use a high volatility here (e.g. 0.1)
	private final double shortRateMeanreversion = 0.1;
	private final LocalDate referenceDate = LocalDate.of(2017, 6, 15);
	//	private final LocalDate referenceDate = LocalDate.of(2014, Month.AUGUST, 12);

	private LIBORModelMonteCarloSimulationModel hullWhiteModelSimulation;
	private LIBORModelMonteCarloSimulationModel liborMarketModelSimulation;

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
		final double liborPeriodLength	= 0.5;
		final double liborRateTimeHorzion	= 20.0;
		final TimeDiscretizationFromArray liborPeriodDiscretization = new TimeDiscretizationFromArray(0.0, (int) (liborRateTimeHorzion / liborPeriodLength), liborPeriodLength);

		// Create the forward curve (initial value of the LIBOR market model)
		final DiscountCurve discountCurve = DiscountCurveInterpolation.createDiscountCurveFromZeroRates(
				"discount curve",
				referenceDate,
				new double[] {0.5, 40.00}	/* zero rate end points */,
				new double[] {0.03,  0.04}	/* zeros */,
				new boolean[] {false,  false},
				InterpolationMethod.LINEAR,
				ExtrapolationMethod.CONSTANT,
				InterpolationEntity.LOG_OF_VALUE_PER_TIME
				);

		AnalyticModel curveModel = new AnalyticModelFromCurvesAndVols(new Curve[] { discountCurve });

		// Create the forward curve (initial value of the LIBOR market model)
		final ForwardCurve forwardCurve = ForwardCurveInterpolation.createForwardCurveFromForwards(
				"forwardCurve"								/* name of the curve */,
				referenceDate,
				"6M",
				ForwardCurveInterpolation.InterpolationEntityForward.FORWARD,
				"discount curve",
				curveModel,
				new double[] {0.5 , 1.0 , 2.0 , 5.0 , 40.0}	/* fixings of the forward */,
				new double[] {0.05, 0.06, 0.07, 0.07, 0.08}	/* forwards */
				);

		// Create the discount curve
		final ForwardCurve forwardCurve2 = new ForwardCurveFromDiscountCurve(discountCurve.getName(),referenceDate,"6M");

		curveModel = new AnalyticModelFromCurvesAndVols(new Curve[] { discountCurve, forwardCurve2 });

		/*
		 * Create a simulation time discretization
		 */
		final double lastTime	= 20.0;
		final double dt		= 0.5;

		final TimeDiscretizationFromArray timeDiscretizationFromArray = new TimeDiscretizationFromArray(0.0, (int) (lastTime / dt), dt);

		/*
		 * Create corresponding Hull White model
		 */
		{
			/*
			 * Create a volatility model: Hull white with constant coefficients (non time dep.).
			 */
			final ShortRateVolatilityModel volatilityModel = new ShortRateVolatilityModelAsGiven(
					new TimeDiscretizationFromArray(0.0),
					new double[] { shortRateVolatility } /* volatility */,
					new double[] { shortRateMeanreversion } /* meanReversion */);

			final Map<String, Object> properties = new HashMap<>();
			properties.put("isInterpolateDiscountFactorsOnLiborPeriodDiscretization", false);

			// TODO Left hand side type should be TermStructureModel once interface are refactored
			final LIBORModel hullWhiteModel = new HullWhiteModel(
					liborPeriodDiscretization, curveModel, forwardCurve2, discountCurve, volatilityModel, properties);

			final BrownianMotion brownianMotion = new net.finmath.montecarlo.BrownianMotionLazyInit(timeDiscretizationFromArray, 2 /* numberOfFactors */, numberOfPaths, 3141 /* seed */);

			final EulerSchemeFromProcessModel process = new EulerSchemeFromProcessModel(hullWhiteModel, brownianMotion, EulerSchemeFromProcessModel.Scheme.EULER);

			hullWhiteModelSimulation = new LIBORMonteCarloSimulationFromLIBORModel(hullWhiteModel, process);
		}

		/*
		 * Create corresponding LIBOR Market model
		 */
		{
			/*
			 * Create a volatility structure v[i][j] = sigma_j(t_i)
			 */
			final double[][] volatility = new double[timeDiscretizationFromArray.getNumberOfTimeSteps()][liborPeriodDiscretization.getNumberOfTimeSteps()];
			for (int timeIndex = 0; timeIndex < volatility.length; timeIndex++) {
				for (int liborIndex = 0; liborIndex < volatility[timeIndex].length; liborIndex++) {
					// Create a very simple volatility model here
					final double time = timeDiscretizationFromArray.getTime(timeIndex);
					final double time2 = timeDiscretizationFromArray.getTime(timeIndex+1);
					final double maturity = liborPeriodDiscretization.getTime(liborIndex);
					final double maturity2 = liborPeriodDiscretization.getTime(liborIndex+1);

					final double timeToMaturity	= maturity - time;
					final double deltaTime		= time2-time;
					final double deltaMaturity	= maturity2-maturity;

					final double meanReversion = shortRateMeanreversion;

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
			final LIBORVolatilityModelFromGivenMatrix volatilityModel = new LIBORVolatilityModelFromGivenMatrix(timeDiscretizationFromArray, liborPeriodDiscretization, volatility);

			/*
			 * Create a correlation model rho_{i,j} = exp(-a * abs(T_i-T_j))
			 */
			final LIBORCorrelationModelExponentialDecay correlationModel = new LIBORCorrelationModelExponentialDecay(
					timeDiscretizationFromArray, liborPeriodDiscretization, numberOfFactors,
					correlationDecay);

			/*
			 * Combine volatility model and correlation model to a covariance model
			 */
			final LIBORCovarianceModelFromVolatilityAndCorrelation covarianceModel =
					new LIBORCovarianceModelFromVolatilityAndCorrelation(timeDiscretizationFromArray,
							liborPeriodDiscretization, volatilityModel, correlationModel);

			// BlendedLocalVolatlityModel
			final LIBORCovarianceModel covarianceModel2 = new HullWhiteLocalVolatilityModel(covarianceModel, liborPeriodLength);

			// Set model properties
			final Map<String, String> properties = new HashMap<>();

			// Choose the simulation measure
			properties.put("measure", LIBORMarketModelFromCovarianceModel.Measure.SPOT.name());

			// Choose log normal model
			properties.put("stateSpace", LIBORMarketModelFromCovarianceModel.StateSpace.NORMAL.name());

			// Empty array of calibration items - hence, model will use given covariance
			final CalibrationProduct[] calibrationItems = new CalibrationProduct[0];

			/*
			 * Create corresponding LIBOR Market Model
			 */
			final LIBORMarketModel liborMarketModel = new LIBORMarketModelFromCovarianceModel(
					liborPeriodDiscretization, curveModel, forwardCurve2, discountCurve, covarianceModel2, calibrationItems, properties);

			final BrownianMotion brownianMotion = new net.finmath.montecarlo.BrownianMotionLazyInit(timeDiscretizationFromArray, numberOfFactors, numberOfPaths, 3141 /* seed */);

			final EulerSchemeFromProcessModel process = new EulerSchemeFromProcessModel(liborMarketModel, brownianMotion, EulerSchemeFromProcessModel.Scheme.EULER);

			liborMarketModelSimulation = new LIBORMonteCarloSimulationFromLIBORModel(liborMarketModel, process);
		}
	}

	@Test
	public void testBond() throws CalculationException {
		/*
		 * Value a bond
		 */

		System.out.println("Bond prices:\n");
		System.out.println("Maturity       Simulation (HW)   Simulation (LMM) Analytic         Deviation (HW-LMM)    Deviation (HW-Analytic)");

		final long startMillis	= System.currentTimeMillis();

		double maxAbsDeviation = 0.0;
		for (int maturityIndex = 0; maturityIndex <= hullWhiteModelSimulation.getNumberOfLibors(); maturityIndex++) {
			final double maturity = hullWhiteModelSimulation.getLiborPeriod(maturityIndex);
			System.out.print(formatterMaturity.format(maturity) + "          ");

			// Create a bond
			final Bond bond = new Bond(maturity);

			// Value with Hull-White Model Monte Carlo
			final double valueSimulationHW = bond.getValue(hullWhiteModelSimulation);
			System.out.print(formatterValue.format(valueSimulationHW) + "          ");

			// Value with LIBOR Market Model Monte Carlo
			final double valueSimulationLMM = bond.getValue(liborMarketModelSimulation);
			System.out.print(formatterValue.format(valueSimulationLMM) + "          ");

			// Bond price analytic
			DiscountCurve discountCurve = hullWhiteModelSimulation.getModel().getDiscountCurve();
			if(discountCurve == null) {
				discountCurve = new DiscountCurveFromForwardCurve(hullWhiteModelSimulation.getModel().getForwardRateCurve());
			}
			final double valueAnalytic = discountCurve.getDiscountFactor(maturity);
			System.out.print(formatterValue.format(valueAnalytic) + "          ");

			// Absolute deviation
			final double deviationHWLMM = (valueSimulationHW - valueSimulationLMM);
			System.out.print(formatterDeviation.format(deviationHWLMM) + "          ");

			// Absolute deviation
			final double deviationHWAnalytic = (valueSimulationHW - valueAnalytic);
			System.out.print(formatterDeviation.format(deviationHWAnalytic) + "          ");

			System.out.println();

			maxAbsDeviation = Math.max(maxAbsDeviation, Math.abs(deviationHWAnalytic));
		}

		final long endMillis		= System.currentTimeMillis();

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
		System.out.println("Swap                             \tValue (HW)       Value (LMM)     Deviation");

		final long startMillis	= System.currentTimeMillis();


		/*
		 * Set up the derivative
		 */
		//Set properties of input swaptions
		final SchedulePrototype fixMetaSchedule = new SchedulePrototype(
				Frequency.ANNUAL,
				DaycountConvention.E30_360,
				ShortPeriodConvention.FIRST,
				BusinessdayCalendar.DateRollConvention.MODIFIED_FOLLOWING,
				new BusinessdayCalendarExcludingTARGETHolidays(),
				0,
				0,
				false);

		final SchedulePrototype floatMetaSchedule = new SchedulePrototype(
				Frequency.SEMIANNUAL,
				DaycountConvention.ACT_360,
				ShortPeriodConvention.FIRST,
				BusinessdayCalendar.DateRollConvention.MODIFIED_FOLLOWING,
				new BusinessdayCalendarExcludingTARGETHolidays(),
				0,
				0,
				false);

		double maxAbsDeviation = 0.0;
		final double maxMaturity = hullWhiteModelSimulation.getLiborPeriod(hullWhiteModelSimulation.getNumberOfLibors()-1) - 10.0;
		for (int maturityIndex = 1; maturityIndex <= maxMaturity*12; maturityIndex++) {

			final LocalDate startDate = referenceDate.plusMonths(maturityIndex);
			final LocalDate endDate = startDate.plusYears(5);

			final Schedule fixLegSchedule = fixMetaSchedule.generateSchedule(referenceDate, startDate, endDate);
			final Schedule floatLegSchedule = floatMetaSchedule.generateSchedule(referenceDate, startDate, endDate);

			final ForwardCurve forwardCurve = liborMarketModelSimulation.getModel().getForwardRateCurve();
			//hullWhiteModelSimulation.getModel().getForwardRateCurve();
			final AnalyticModel model = hullWhiteModelSimulation.getModel().getAnalyticModel();

			// Par swap rate
			final double swaprate = net.finmath.marketdata.products.Swap.getForwardSwapRate(fixLegSchedule, floatLegSchedule, forwardCurve, model);

			//Create libor index for Monte-Carlo swap
			final AbstractIndex libor = new LIBORIndex(forwardCurve.getName(), "EUR", "6M", floatMetaSchedule.getBusinessdayCalendar(), floatMetaSchedule.getDateRollConvention());

			//Create legs
			final TermStructureMonteCarloProduct floatLeg = new SwapLeg(floatLegSchedule, new NotionalFromConstant(1.0), libor, 0, false);
			final TermStructureMonteCarloProduct fixLeg = new SwapLeg(fixLegSchedule, new NotionalFromConstant(1.0), null, swaprate, false);

			//Create swap
			final AbstractTermStructureMonteCarloProduct swap = new Swap(floatLeg, fixLeg);

			System.out.print("(" + startDate + "," + endDate + "," + floatMetaSchedule.getFrequency().name() + ")" + "\t");

			// Value with Hull-White Model Monte Carlo
			final double valueSimulationHW = swap.getValue(hullWhiteModelSimulation);
			System.out.print(formatterValue.format(valueSimulationHW) + "          ");

			// Value with LIBOR Market Model Monte Carlo
			final double valueSimulationLMM = swap.getValue(liborMarketModelSimulation);
			System.out.print(formatterValue.format(valueSimulationLMM) + "          ");

			// Absolute deviation
			final double deviationHWLMM = (valueSimulationHW - valueSimulationLMM);
			System.out.print(formatterDeviation.format(deviationHWLMM) + "          ");

			System.out.println();

			maxAbsDeviation = Math.max(maxAbsDeviation, Math.abs(valueSimulationHW));
		}

		final long endMillis		= System.currentTimeMillis();

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

		final long startMillis	= System.currentTimeMillis();

		double maxAbsDeviation = 0.0;
		for (int maturityIndex = 1; maturityIndex <= hullWhiteModelSimulation.getNumberOfLibors() - 10; maturityIndex++) {

			final double optionMaturity = hullWhiteModelSimulation.getLiborPeriod(maturityIndex);
			System.out.print(formatterMaturity.format(optionMaturity) + "          ");

			final double periodStart	= hullWhiteModelSimulation.getLiborPeriod(maturityIndex);
			final double periodEnd	= hullWhiteModelSimulation.getLiborPeriod(maturityIndex+1);
			final double periodLength	= periodEnd-periodStart;
			final double daycountFraction = periodEnd-periodStart;

			String		tenorCode;
			if(periodLength == 0.5) {
				tenorCode = "6M";
			} else if(periodLength == 1.0) {
				tenorCode = "1Y";
			} else {
				throw new IllegalArgumentException("Unsupported period length.");
			}

			final double forward			= getParSwaprate(hullWhiteModelSimulation, new double[] { periodStart , periodEnd}, tenorCode);
			final double discountFactor	= getSwapAnnuity(hullWhiteModelSimulation, new double[] { periodStart , periodEnd}) / periodLength;

			final double strike = forward + 0.01;

			// Create a caplet
			final Caplet caplet = new Caplet(optionMaturity, periodLength, strike, daycountFraction, false /* isFloorlet */, Caplet.ValueUnit.VALUE);

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
				forwardBondVolatility = Math.sqrt(((HullWhiteModel)(hullWhiteModelSimulation.getModel())).getIntegratedBondSquaredVolatility(optionMaturity, optionMaturity+periodLength).doubleValue()/optionMaturity);
			}
			else if(hullWhiteModelSimulation.getModel() instanceof HullWhiteModelWithDirectSimulation) {
				forwardBondVolatility = Math.sqrt(((HullWhiteModelWithDirectSimulation)(hullWhiteModelSimulation.getModel())).getIntegratedBondSquaredVolatility(optionMaturity, optionMaturity+periodLength)/optionMaturity);
			}
			else if(hullWhiteModelSimulation.getModel() instanceof HullWhiteModelWithShiftExtension) {
				forwardBondVolatility = Math.sqrt(((HullWhiteModelWithShiftExtension)(hullWhiteModelSimulation.getModel())).getIntegratedBondSquaredVolatility(optionMaturity, optionMaturity+periodLength)/optionMaturity);
			}

			final double bondForward = (1.0+forward*periodLength);
			final double bondStrike = (1.0+strike*periodLength);

			final double zeroBondPut = net.finmath.functions.AnalyticFormulas.blackModelCapletValue(bondForward, forwardBondVolatility, optionMaturity, bondStrike, periodLength, discountFactor);

			double valueAnalytic = zeroBondPut / bondStrike / periodLength;
			valueAnalytic = AnalyticFormulas.bachelierOptionImpliedVolatility(forward, optionMaturity, strike, discountFactor * periodLength /* payoffUnit */, valueAnalytic);
			System.out.print(formatterValue.format(valueAnalytic) + "          ");

			// Absolute deviation
			final double deviationHWLMM = (valueSimulationHW - valueSimulationLMM);
			System.out.print(formatterDeviation.format(deviationHWLMM) + "          ");

			// Absolute deviation
			final double deviationHWAnalytic = (valueSimulationHW - valueAnalytic);
			System.out.print(formatterDeviation.format(deviationHWAnalytic) + "          ");

			System.out.println();

			maxAbsDeviation = Math.max(maxAbsDeviation, Math.abs(deviationHWLMM));
			if(!Double.isNaN(valueAnalytic)) {
				maxAbsDeviation = Math.max(maxAbsDeviation, Math.abs(deviationHWAnalytic));
			}
		}

		final long endMillis		= System.currentTimeMillis();

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

		final long startMillis	= System.currentTimeMillis();

		double maxAbsDeviation = 0.0;
		for (int maturityIndex = 1; maturityIndex <= hullWhiteModelSimulation.getNumberOfLibors() - 10; maturityIndex++) {

			final double exerciseDate = hullWhiteModelSimulation.getLiborPeriod(maturityIndex);
			System.out.print(formatterMaturity.format(exerciseDate) + "          ");

			final int numberOfPeriods = 5;

			// Create a swaption

			final double[] fixingDates = new double[numberOfPeriods];
			final double[] paymentDates = new double[numberOfPeriods];
			final double[] swapTenor = new double[numberOfPeriods + 1];
			final double swapPeriodLength = 0.5;
			final String tenorCode = "6M";

			for (int periodStartIndex = 0; periodStartIndex < numberOfPeriods; periodStartIndex++) {
				fixingDates[periodStartIndex] = exerciseDate + periodStartIndex * swapPeriodLength;
				paymentDates[periodStartIndex] = exerciseDate + (periodStartIndex + 1) * swapPeriodLength;
				swapTenor[periodStartIndex] = exerciseDate + periodStartIndex * swapPeriodLength;
			}
			swapTenor[numberOfPeriods] = exerciseDate + numberOfPeriods * swapPeriodLength;

			// Swaptions swap rate
			final double swaprate = getParSwaprate(hullWhiteModelSimulation, swapTenor, tenorCode);
			final double swapAnnuity = getSwapAnnuity(hullWhiteModelSimulation, swapTenor);

			// Set swap rates for each period
			final double[] swaprates = new double[numberOfPeriods];
			for (int periodStartIndex = 0; periodStartIndex < numberOfPeriods; periodStartIndex++) {
				swaprates[periodStartIndex] = swaprate;
			}

			// Create a swaption
			final Swaption swaptionMonteCarlo	= new Swaption(exerciseDate, fixingDates, paymentDates, swaprates);

			// Value with Hull-White Model Monte Carlo
			final double valueSimulationHW = swaptionMonteCarlo.getValue(hullWhiteModelSimulation);
			System.out.print(formatterValue.format(valueSimulationHW) + "          ");

			// Value with LIBOR Market Model Monte Carlo
			final double valueSimulationLMM = swaptionMonteCarlo.getValue(liborMarketModelSimulation);
			System.out.print(formatterValue.format(valueSimulationLMM) + "          ");

			// Value with analytic formula (approximate, using Bachelier formula)
			final SwaptionAnalyticApproximation swaptionAnalytic = new SwaptionAnalyticApproximation(swaprate, swapTenor, SwaptionAnalyticApproximation.ValueUnit.VOLATILITYLOGNORMAL);
			final double volatilityAnalytic = swaptionAnalytic.getValue(liborMarketModelSimulation);
			final double valueAnalytic = AnalyticFormulas.bachelierOptionValue(swaprate, volatilityAnalytic, exerciseDate, swaprate, swapAnnuity);
			System.out.print(formatterValue.format(valueAnalytic) + "          ");

			// Absolute deviation
			final double deviationHWLMM = (valueSimulationHW - valueSimulationLMM);
			System.out.print(formatterDeviation.format(deviationHWLMM) + "          ");

			System.out.println();

			maxAbsDeviation = Math.max(maxAbsDeviation, Math.abs(deviationHWLMM));
		}

		final long endMillis		= System.currentTimeMillis();

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

		final long startMillis	= System.currentTimeMillis();

		double maxAbsDeviation = 0.0;
		for (int maturityIndex = 1; maturityIndex <= hullWhiteModelSimulation.getNumberOfLibors() - 10; maturityIndex++) {

			final double exerciseDate = hullWhiteModelSimulation.getLiborPeriod(maturityIndex);
			System.out.print(formatterMaturity.format(exerciseDate) + "          ");

			final int numberOfPeriods = 5;

			// Create a swaption

			final double[] fixingDates = new double[numberOfPeriods];
			final double[] paymentDates = new double[numberOfPeriods];
			final double[] swapTenor = new double[numberOfPeriods + 1];
			final double swapPeriodLength = 0.5;
			final String tenorCode = "6M";

			for (int periodStartIndex = 0; periodStartIndex < numberOfPeriods; periodStartIndex++) {
				fixingDates[periodStartIndex] = exerciseDate + periodStartIndex * swapPeriodLength;
				paymentDates[periodStartIndex] = exerciseDate + (periodStartIndex + 1) * swapPeriodLength;
				swapTenor[periodStartIndex] = exerciseDate + periodStartIndex * swapPeriodLength;
			}
			swapTenor[numberOfPeriods] = exerciseDate + numberOfPeriods * swapPeriodLength;

			// Swaptions swap rate
			final double swaprate = getParSwaprate(hullWhiteModelSimulation, swapTenor, tenorCode);
			final double swapAnnuity = getSwapAnnuity(hullWhiteModelSimulation, swapTenor);

			// Set swap rates for each period
			final double[] swaprates = new double[numberOfPeriods];
			Arrays.fill(swaprates, swaprate);

			final double[] periodLength = new double[numberOfPeriods];
			Arrays.fill(periodLength, swapPeriodLength);

			final double[] periodNotionals = new double[numberOfPeriods];
			Arrays.fill(periodNotionals, 1.0);

			final boolean[] isPeriodStartDateExerciseDate = new boolean[numberOfPeriods];
			Arrays.fill(isPeriodStartDateExerciseDate, true);

			// Create a bermudan swaption
			final BermudanSwaption bermudanSwaption = new BermudanSwaption(isPeriodStartDateExerciseDate, fixingDates, periodLength, paymentDates, periodNotionals, swaprates);

			// Value with Hull-White Model Monte Carlo
			final double valueSimulationHW = bermudanSwaption.getValue(hullWhiteModelSimulation);
			System.out.print(formatterValue.format(valueSimulationHW) + "          ");

			// Value with LIBOR Market Model Monte Carlo
			final double valueSimulationLMM = bermudanSwaption.getValue(liborMarketModelSimulation);
			System.out.print(formatterValue.format(valueSimulationLMM) + "          ");

			// Value the underlying swaption with analytic formula (approximate, using Bachelier formula)
			final SwaptionAnalyticApproximation swaptionAnalytic = new SwaptionAnalyticApproximation(swaprate, swapTenor, SwaptionAnalyticApproximation.ValueUnit.VOLATILITYLOGNORMAL);
			final double volatilityAnalytic = swaptionAnalytic.getValue(liborMarketModelSimulation);
			final double valueAnalytic = AnalyticFormulas.bachelierOptionValue(swaprate, volatilityAnalytic, exerciseDate, swaprate, swapAnnuity);
			System.out.print(formatterValue.format(valueAnalytic) + "          ");

			// Absolute deviation
			final double deviationHWLMM = (valueSimulationHW - valueSimulationLMM);
			System.out.print(formatterDeviation.format(deviationHWLMM) + "          ");

			System.out.println();

			maxAbsDeviation = Math.max(maxAbsDeviation, Math.abs(deviationHWLMM));
		}

		final long endMillis		= System.currentTimeMillis();

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
		System.out.println("Checking that the two models generate the same smiles.");

		/*
		 * Value a caplet
		 */
		System.out.println("Caplet implied volatilities:\n");
		System.out.println("Strike       Simulation (HW)   Simulation (LMM) Analytic         Deviation (HW-LMM)    Deviation (HW-Analytic)");

		final long startMillis	= System.currentTimeMillis();

		double maxAbsDeviation = 0.0;
		final double optionMaturity = 5.0;
		final double periodLength = 0.5;
		for (double strike = 0.03; strike <= 0.10; strike+=0.01) {

			System.out.print(formatterMaturity.format(strike) + "          ");

			final double periodStart		= optionMaturity;
			final double periodEnd		= optionMaturity+periodLength;
			final double daycountFraction = periodEnd-periodStart;

			String		tenorCode;
			if(periodLength == 0.5) {
				tenorCode = "6M";
			} else if(periodLength == 1.0) {
				tenorCode = "1Y";
			} else {
				throw new IllegalArgumentException("Unsupported period length.");
			}

			final double forward			= getParSwaprate(hullWhiteModelSimulation, new double[] { periodStart , periodEnd}, tenorCode);
			final double discountFactor	= getSwapAnnuity(hullWhiteModelSimulation, new double[] { periodStart , periodEnd}) / periodLength;

			// Create a caplet
			final Caplet caplet = new Caplet(optionMaturity, periodLength, strike, daycountFraction, false /* isFloorlet */, Caplet.ValueUnit.VALUE);

			// Value with Hull-White Model Monte Carlo
			double valueSimulationHW = caplet.getValue(hullWhiteModelSimulation);
			valueSimulationHW = AnalyticFormulas.bachelierOptionImpliedVolatility(forward, optionMaturity, strike, discountFactor * periodLength /* payoffUnit */, valueSimulationHW);
			System.out.print(formatterValue.format(valueSimulationHW) + "          ");

			// Value with LIBOR Market Model Monte Carlo
			double valueSimulationLMM = caplet.getValue(liborMarketModelSimulation);
			valueSimulationLMM = AnalyticFormulas.bachelierOptionImpliedVolatility(forward, optionMaturity, strike, discountFactor * periodLength /* payoffUnit */, valueSimulationLMM);
			System.out.print(formatterValue.format(valueSimulationLMM) + "          ");

			// Value analytic
			final double forwardBondVolatility = shortRateVolatility*(1-Math.exp(-shortRateMeanreversion*periodLength))/(shortRateMeanreversion)*Math.sqrt((1-Math.exp(-2*shortRateMeanreversion*optionMaturity))/(2*shortRateMeanreversion*optionMaturity));
			final double bondForward = (1.0+forward*periodLength);
			final double bondStrike = (1.0+strike*periodLength);

			final double zeroBondPut = net.finmath.functions.AnalyticFormulas.blackModelCapletValue(bondForward, forwardBondVolatility, optionMaturity, bondStrike, periodLength, discountFactor);
			double valueAnalytic = zeroBondPut / bondStrike / periodLength;
			valueAnalytic = AnalyticFormulas.bachelierOptionImpliedVolatility(forward, optionMaturity, strike, discountFactor * periodLength /* payoffUnit */, valueAnalytic);
			System.out.print(formatterValue.format(valueAnalytic) + "          ");

			// Absolute deviation
			final double deviationHWLMM = (valueSimulationHW - valueSimulationLMM);
			System.out.print(formatterDeviation.format(deviationHWLMM) + "          ");

			// Absolute deviation
			final double deviationHWAnalytic = (valueSimulationHW - valueAnalytic);
			System.out.print(formatterDeviation.format(deviationHWAnalytic) + "          ");

			System.out.println();

			maxAbsDeviation = Math.max(maxAbsDeviation, Math.abs(deviationHWLMM));
		}

		final long endMillis		= System.currentTimeMillis();

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

		final long startMillis	= System.currentTimeMillis();

		double maxAbsDeviation = 0.0;
		for (int maturityIndex = 1; maturityIndex <= hullWhiteModelSimulation.getNumberOfLibors()-10-20; maturityIndex++) {

			final double startDate = hullWhiteModelSimulation.getLiborPeriod(maturityIndex);

			final int numberOfPeriods = 10;

			// Create a swap
			final double[]	fixingDates			= new double[numberOfPeriods];
			final double[]	paymentDates		= new double[numberOfPeriods];
			final double[]	swapTenor			= new double[numberOfPeriods + 1];
			final double		swapPeriodLength	= 0.5;
			final String		tenorCode			= "6M";

			for (int periodStartIndex = 0; periodStartIndex < numberOfPeriods; periodStartIndex++) {
				fixingDates[periodStartIndex]	= startDate + periodStartIndex * swapPeriodLength;
				paymentDates[periodStartIndex]	= startDate + (periodStartIndex + 1) * swapPeriodLength;
				swapTenor[periodStartIndex]		= startDate + periodStartIndex * swapPeriodLength;
			}
			swapTenor[numberOfPeriods] = startDate + numberOfPeriods * swapPeriodLength;

			System.out.print("(" + formatterMaturity.format(swapTenor[0]) + "," + formatterMaturity.format(swapTenor[numberOfPeriods-1]) + "," + swapPeriodLength + ")" + "\t");

			// Par swap rate
			final double swaprate = getParSwaprate(hullWhiteModelSimulation, swapTenor, tenorCode);

			// Set swap rates for each period
			final double[] swaprates = new double[numberOfPeriods];
			for (int periodStartIndex = 0; periodStartIndex < numberOfPeriods; periodStartIndex++) {
				swaprates[periodStartIndex] = swaprate;
			}

			// Create a swap
			AbstractIndex index = new ConstantMaturitySwaprate(10.0, 0.5);
			index = new CappedFlooredIndex(index, new FixedCoupon(0.1) /* cap */, new FixedCoupon(0.04) /* Floor */);
			final SimpleZeroSwap swap = new SimpleZeroSwap(fixingDates, paymentDates, swaprates, index, true);

			// Value the swap
			final double valueSimulationHW = swap.getValue(hullWhiteModelSimulation);
			System.out.print(formatterValue.format(valueSimulationHW) + "          ");

			final double valueSimulationLMM = swap.getValue(liborMarketModelSimulation);
			System.out.print(formatterValue.format(valueSimulationLMM) + "          ");

			// Absolute deviation
			final double deviationHWLMM = (valueSimulationHW - valueSimulationLMM);
			System.out.print(formatterDeviation.format(deviationHWLMM) + "          ");

			System.out.println();

			maxAbsDeviation = Math.max(maxAbsDeviation, Math.abs(deviationHWLMM));
		}

		final long endMillis		= System.currentTimeMillis();

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
		final LocalDate	referenceDate = LocalDate.of(2014, Month.AUGUST, 12);
		final int			spotOffsetDays = 2;
		final String		forwardStartPeriod = "6M";
		final String		maturity = "6M";
		final String		frequency = "semiannual";
		final String		daycountConvention = "30/360";

		final Schedule schedule = ScheduleGenerator.createScheduleFromConventions(referenceDate, spotOffsetDays, forwardStartPeriod, maturity, frequency, daycountConvention, "first", "following", new BusinessdayCalendarExcludingTARGETHolidays(), -2, 0);

		/*
		 * Create the leg with a notional and index
		 */
		final Notional notional = new NotionalFromConstant(1.0);
		final AbstractIndex liborIndex = new LIBORIndex(0.0, 0.5);
		final AbstractIndex index = new LinearCombinationIndex(-1.0, liborIndex, +1, new LaggedIndex(liborIndex, +0.5 /* fixingOffset */));
		final double spread = 0.0;

		final SwapLeg leg = new SwapLeg(schedule, notional, index, spread, false /* isNotionalExchanged */);

		System.out.println("LIBOR in Arrears Swap prices:\n");
		System.out.println("Value (HW)               Value (LMM)              Deviation");

		// Value the swap
		final RandomVariable valueSimulationHW = leg.getValue(0,hullWhiteModelSimulation);
		System.out.print(formatterValue.format(valueSimulationHW.getAverage()) + " " + formatterValue.format(valueSimulationHW.getStandardError()) + "          ");

		final RandomVariable valueSimulationLMM = leg.getValue(0,liborMarketModelSimulation);
		System.out.print(formatterValue.format(valueSimulationLMM.getAverage()) + " " + formatterValue.format(valueSimulationLMM.getStandardError()) + "          ");

		// Absolute deviation
		final double deviationHWLMM = (valueSimulationHW.getAverage() - valueSimulationLMM.getAverage());
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
		final Option product = new Option(0.5, 1.025, new Numeraire());

		System.out.println("Put-on-Money-Market-Account prices:\n");
		System.out.println("Value (HW)               Value (LMM)              Deviation");

		// Value the product
		final RandomVariable valueSimulationHW = product.getValue(0,hullWhiteModelSimulation);
		System.out.print(formatterValue.format(valueSimulationHW.getAverage()) + " " + formatterValue.format(valueSimulationHW.getStandardError()) + "          ");

		final RandomVariable valueSimulationLMM = product.getValue(0,liborMarketModelSimulation);
		System.out.print(formatterValue.format(valueSimulationLMM.getAverage()) + " " + formatterValue.format(valueSimulationLMM.getStandardError()) + "          ");

		// Absolute deviation
		final double deviationHWLMM = (valueSimulationHW.getAverage() - valueSimulationLMM.getAverage());
		System.out.print(formatterDeviation.format(deviationHWLMM) + "          ");

		System.out.println();

		System.out.println("__________________________________________________________________________________________\n");

		/*
		 * jUnit assertion: condition under which we consider this test successful
		 */
		Assert.assertEquals("Valuation of put on MMA", valueSimulationLMM.getAverage(), valueSimulationHW.getAverage(), 1E-5);
	}

	private static double getParSwaprate(final LIBORModelMonteCarloSimulationModel liborMarketModel, final Schedule fixLeg, final Schedule floatLeg, final String tenorCode) {
		final ForwardCurve forwardCurve = liborMarketModel.getModel().getForwardRateCurve();
		final AnalyticModel analyticModel = liborMarketModel.getModel().getAnalyticModel();
		return net.finmath.marketdata.products.Swap.getForwardSwapRate(fixLeg, floatLeg, forwardCurve, analyticModel);
	}

	private static double getParSwaprate(final LIBORModelMonteCarloSimulationModel liborMarketModel, final double[] swapTenor, final String tenorCode) {
		final DiscountCurve discountCurve = liborMarketModel.getModel().getDiscountCurve();
		final ForwardCurve forwardCurve = liborMarketModel.getModel().getForwardRateCurve();
		final AnalyticModel analyticModel = liborMarketModel.getModel().getAnalyticModel();
		return net.finmath.marketdata.products.Swap.getForwardSwapRate(new TimeDiscretizationFromArray(swapTenor), new TimeDiscretizationFromArray(swapTenor),
				forwardCurve,
				discountCurve);
	}

	private static double getSwapAnnuity(final LIBORModelMonteCarloSimulationModel liborMarketModel, final double[] swapTenor) {
		DiscountCurve discountCurve = liborMarketModel.getModel().getDiscountCurve();
		if(discountCurve == null) {
			discountCurve = new DiscountCurveFromForwardCurve(liborMarketModel.getModel().getForwardRateCurve());
		}
		return net.finmath.marketdata.products.SwapAnnuity.getSwapAnnuity(new TimeDiscretizationFromArray(swapTenor), discountCurve);
	}
}
