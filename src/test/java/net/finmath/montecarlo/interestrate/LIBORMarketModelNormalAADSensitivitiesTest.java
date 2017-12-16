/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 10.02.2004
 */
package net.finmath.montecarlo.interestrate;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.curves.DiscountCurveFromForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.montecarlo.AbstractRandomVariableFactory;
import net.finmath.montecarlo.BrownianMotionInterface;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiableInterface;
import net.finmath.montecarlo.automaticdifferentiation.backward.RandomVariableDifferentiableAADFactory;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORCorrelationModelExponentialDecay;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORCovarianceModelFromVolatilityAndCorrelation;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORVolatilityModel;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORVolatilityModelFourParameterExponentialForm;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORVolatilityModelFromGivenMatrix;
import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.montecarlo.interestrate.products.BermudanSwaption;
import net.finmath.montecarlo.interestrate.products.Caplet;
import net.finmath.montecarlo.interestrate.products.Swaption;
import net.finmath.montecarlo.process.ProcessEulerScheme;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretization;

/**
 * This class tests the LIBOR market model and products.
 * 
 * @author Christian Fries
 */
@RunWith(Parameterized.class)
public class LIBORMarketModelNormalAADSensitivitiesTest {

	private final static int numberOfPaths		= 5000;
	private final static int numberOfFactors	= 1;

	private static DecimalFormat formatReal1		= new DecimalFormat("####0.0", new DecimalFormatSymbols(Locale.ENGLISH));
	private static DecimalFormat formatterValue		= new DecimalFormat(" ##0.000%;-##0.000%", new DecimalFormatSymbols(Locale.ENGLISH));

	@Parameters(name="{0}")
	public static Collection<Object[]> data() {
		Collection<Object[]> testParameters = new ArrayList<>();

		double liborPeriodLength	= 0.5;
		double liborRateTimeHorzion	= 40.0;
		TimeDiscretization liborPeriodDiscretization = new TimeDiscretization(0.0, (int) (liborRateTimeHorzion / liborPeriodLength), liborPeriodLength);

		// Create the forward curve (initial value of the LIBOR market model)
		ForwardCurve forwardCurve = ForwardCurve.createForwardCurveFromForwards(
				"forwardCurve"								/* name of the curve */,
				new double[] {0.5 , 1.0 , 2.0 , 5.0 , 40.0}	/* fixings of the forward */,
				new double[] {0.05, 0.05, 0.05, 0.05, 0.05}	/* forwards */,
				liborPeriodLength							/* tenor / period length */
				);

		int maturityIndex = 60;
		double exerciseDate = liborPeriodDiscretization.getTime(maturityIndex);

		int numberOfPeriods = 20;

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
		double swaprate = net.finmath.marketdata.products.Swap.getForwardSwapRate(new TimeDiscretization(swapTenor), new TimeDiscretization(swapTenor), forwardCurve, new DiscountCurveFromForwardCurve(forwardCurve));

		// Set swap rates for each period
		double[] swaprates = new double[numberOfPeriods];
		Arrays.fill(swaprates, swaprate);

		double[] periodLengths = new double[numberOfPeriods];
		Arrays.fill(periodLengths, swapPeriodLength);

		double[] periodNotionals = new double[numberOfPeriods];
		Arrays.fill(periodNotionals, 1.0);

		boolean[] isPeriodStartDateExerciseDate = new boolean[numberOfPeriods];
		Arrays.fill(isPeriodStartDateExerciseDate, true);

		/*
		 * The first three are just for the JVM warm up
		 * (following timings will be a little more accurate).
		 */
		testParameters.add(new Object[] {
				"Caplet maturity " + exerciseDate,
				new Caplet(exerciseDate, swapPeriodLength, swaprate)
		});

		testParameters.add(new Object[] {
				"Caplet maturity " + 5.0,
				new Caplet(5.0, swapPeriodLength, swaprate)
		});

		testParameters.add(new Object[] {
				"Bermudan Swaption " + exerciseDate + " in " + paymentDates[paymentDates.length-1],
				new BermudanSwaption(isPeriodStartDateExerciseDate, fixingDates, periodLengths, paymentDates, periodNotionals, swaprates)
		});

		/*
		 * Test cases
		 */

		testParameters.add(new Object[] {
				"Caplet maturity " + 5.0,
				new Caplet(5.0, swapPeriodLength, swaprate)
		});

		testParameters.add(new Object[] {
				"Caplet maturity " + exerciseDate,
				new Caplet(exerciseDate, swapPeriodLength, swaprate)
		});

		testParameters.add(new Object[] {
				"Swaption " + exerciseDate + " in " + paymentDates[paymentDates.length-1],
				new Swaption(exerciseDate, fixingDates, paymentDates, periodLengths, swaprates)
		});

		testParameters.add(new Object[] {
				"Bermudan Swaption " + exerciseDate + " in " + paymentDates[paymentDates.length-1],
				new BermudanSwaption(isPeriodStartDateExerciseDate, fixingDates, periodLengths, paymentDates, periodNotionals, swaprates)
		});

		return testParameters;
	}

	private final String productName;
	private final AbstractLIBORMonteCarloProduct product;

	private LIBORVolatilityModel volatilityModel;

	public LIBORMarketModelNormalAADSensitivitiesTest(String productName, AbstractLIBORMonteCarloProduct product) throws CalculationException {
		this.productName = productName;
		this.product = product;
	}

	public LIBORModelMonteCarloSimulationInterface createLIBORMarketModel(
			AbstractRandomVariableFactory randomVariableFactory,
			int numberOfPaths, int numberOfFactors, double correlationDecayParam) throws CalculationException {

		/*
		 * Create the libor tenor structure and the initial values
		 */
		double liborPeriodLength	= 0.5;
		double liborRateTimeHorzion	= 40.0;
		TimeDiscretization liborPeriodDiscretization = new TimeDiscretization(0.0, (int) (liborRateTimeHorzion / liborPeriodLength), liborPeriodLength);

		// Create the forward curve (initial value of the LIBOR market model)
		ForwardCurve forwardCurve = ForwardCurve.createForwardCurveFromForwards(
				"forwardCurve"								/* name of the curve */,
				new double[] {0.5 , 1.0 , 2.0 , 5.0 , 40.0}	/* fixings of the forward */,
				new double[] {0.05, 0.05, 0.05, 0.05, 0.05}	/* forwards */,
				liborPeriodLength							/* tenor / period length */
				);

		/*
		 * Create a simulation time discretization
		 */
		double lastTime	= 40.0;
		double dt		= 0.125;

		TimeDiscretization timeDiscretization = new TimeDiscretization(0.0, (int) (lastTime / dt), dt);

		/*
		 * Create a volatility structure v[i][j] = sigma_j(t_i)
		 */
		double a = 0.0 / 20.0, b = 0.0, c = 0.25, d = 0.3 / 20.0 / 2.0;
		//		LIBORVolatilityModel volatilityModel = new LIBORVolatilityModelFourParameterExponentialFormIntegrated(timeDiscretization, liborPeriodDiscretization, a, b, c, d, false);		
		volatilityModel = new LIBORVolatilityModelFourParameterExponentialForm(randomVariableFactory, timeDiscretization, liborPeriodDiscretization, a, b, c, d, false);
		double[][] volatilityMatrix = new double[timeDiscretization.getNumberOfTimeSteps()][liborPeriodDiscretization.getNumberOfTimeSteps()];
		for(int timeIndex=0; timeIndex<timeDiscretization.getNumberOfTimeSteps(); timeIndex++) Arrays.fill(volatilityMatrix[timeIndex], d);
		volatilityModel = new LIBORVolatilityModelFromGivenMatrix(randomVariableFactory, timeDiscretization, liborPeriodDiscretization, volatilityMatrix);

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
		properties.put("measure", LIBORMarketModel.Measure.SPOT.name());

		// Choose log normal model
		properties.put("stateSpace", LIBORMarketModel.StateSpace.NORMAL.name());

		// Empty array of calibration items - hence, model will use given covariance
		LIBORMarketModel.CalibrationItem[] calibrationItems = new LIBORMarketModel.CalibrationItem[0];

		/*
		 * Create corresponding LIBOR Market Model
		 */
		LIBORMarketModelInterface liborMarketModel = new LIBORMarketModel(liborPeriodDiscretization, null, forwardCurve, new DiscountCurveFromForwardCurve(forwardCurve), randomVariableFactory, covarianceModel, calibrationItems, properties);

		BrownianMotionInterface brownianMotion = new net.finmath.montecarlo.BrownianMotion(timeDiscretization, numberOfFactors, numberOfPaths, 3141 /* seed */);

		ProcessEulerScheme process = new ProcessEulerScheme(brownianMotion, ProcessEulerScheme.Scheme.EULER);

		return new LIBORModelMonteCarloSimulation(liborMarketModel, process);
	}

	@Test
	public void testVega() throws CalculationException {

		// Create a libor market model
		AbstractRandomVariableFactory randomVariableFactory = new RandomVariableDifferentiableAADFactory();
		LIBORModelMonteCarloSimulationInterface liborMarketModel = createLIBORMarketModel(randomVariableFactory,  numberOfPaths, numberOfFactors, 0.0 /* Correlation */);

		/*
		 * Test valuation
		 */
		long memoryStart = getAllocatedMemory();
		long timingCalculationStart = System.currentTimeMillis();

		RandomVariableInterface value = product.getValue(0.0, liborMarketModel);
		double valueSimulation = value.getAverage();

		long timingCalculationEnd = System.currentTimeMillis();


		/*
		 * Test gradient
		 */

		long timingGradientStart = System.currentTimeMillis();

		Map<Long, RandomVariableInterface> gradient = null;
		try {
			gradient = ((RandomVariableDifferentiableInterface)value).getGradient();
		}
		catch(java.lang.ClassCastException e) {};

		long timingGradientEnd = System.currentTimeMillis();
		long memoryEnd = getAllocatedMemory();

		int numberOfVegasTheoretical	= 0;
		int numberOfVegasEffective	= 0;
		double[][] modelVegas = new double[volatilityModel.getTimeDiscretization().getNumberOfTimeSteps()][volatilityModel.getLiborPeriodDiscretization().getNumberOfTimeSteps()];
		if(gradient != null) {
			for(int timeIndex=0; timeIndex<volatilityModel.getTimeDiscretization().getNumberOfTimeSteps(); timeIndex++) {
				for(int componentIndex=0; componentIndex<volatilityModel.getLiborPeriodDiscretization().getNumberOfTimeSteps(); componentIndex++) {
					double modelVega = 0.0;
					RandomVariableDifferentiableInterface volatility = ((RandomVariableDifferentiableInterface)volatilityModel.getVolatility(timeIndex, componentIndex));
					if(volatility != null) {
						numberOfVegasTheoretical++;
						RandomVariableInterface modelVegaRandomVariable = gradient.get(volatility.getID());
						if(modelVegaRandomVariable != null) {
							modelVega = modelVegaRandomVariable.getAverage();
							numberOfVegasEffective++;
						}
					}
//					System.out.println(volatilityModel.getTimeDiscretization().getTime(timeIndex) + "\t" + volatilityModel.getLiborPeriodDiscretization().getTime(componentIndex) + "\t" + modelVega);
					modelVegas[timeIndex][componentIndex] = modelVega;
					System.out.print(modelVega + "\t");
				}
				System.out.println("");
			}
		}
		//		RandomVariableInterface modelDelta = gradient.get(liborMarketModel.getLIBOR(0, 0));


		/*
		 * Test results against alternative implementation
		 */
		LIBORModelMonteCarloSimulationInterface liborMarketModelPlain = createLIBORMarketModel(new RandomVariableFactory(),  numberOfPaths, numberOfFactors, 0.0 /* Correlation */);
	
		/*
		 * Test valuation
		 */
		long timingCalculation2Start = System.currentTimeMillis();

		double valueSimulation2 = product.getValue(liborMarketModelPlain);

		long timingCalculation2End = System.currentTimeMillis();


		System.out.println(product.getClass().getSimpleName() + ": " + productName);
		System.out.println("_______________________________________________________________________");
		System.out.println("value...........................: " + formatterValue.format(valueSimulation));
		System.out.println("value (plain)...................: " + formatterValue.format(valueSimulation2));
		System.out.println("evaluation (plain)..............: " + formatReal1.format((timingCalculation2End-timingCalculation2Start)/1000.0) + " s");
		System.out.println("evaluation......................: " + formatReal1.format((timingCalculationEnd-timingCalculationStart)/1000.0) + " s");
		System.out.println("derivative......................: " + formatReal1.format((timingGradientEnd-timingGradientStart)/1000.0) + " s");
		System.out.println("number of vegas (theoretical)...: " + numberOfVegasTheoretical);
		System.out.println("number of vegas (effective).....: " + numberOfVegasEffective);
		System.out.println("memory..........................: " + ((double)(memoryEnd-memoryStart))/1024.0/1024.0 + " M");
		System.out.println("\n");
		
		Assert.assertEquals("Valuation", valueSimulation2, valueSimulation, 0.0 /* delta */);
	}

	private static double getParSwaprate(LIBORModelMonteCarloSimulationInterface liborMarketModel, double[] swapTenor) throws CalculationException {
		return net.finmath.marketdata.products.Swap.getForwardSwapRate(new TimeDiscretization(swapTenor), new TimeDiscretization(swapTenor), liborMarketModel.getModel().getForwardRateCurve(), liborMarketModel.getModel().getDiscountCurve());
	}

	private static double getSwapAnnuity(LIBORModelMonteCarloSimulationInterface liborMarketModel, double[] swapTenor) throws CalculationException {
		return net.finmath.marketdata.products.SwapAnnuity.getSwapAnnuity(new TimeDiscretization(swapTenor), liborMarketModel.getModel().getDiscountCurve());
	}	

	
	static long getAllocatedMemory() {
		System.gc();
		System.gc();
		System.gc();
		long allocatedMemory = (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());
		return allocatedMemory;
	}
}