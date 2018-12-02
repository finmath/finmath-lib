/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
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
import java.util.Optional;

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
import net.finmath.montecarlo.interestrate.covariancemodels.LIBORCovarianceModelFromVolatilityAndCorrelation;
import net.finmath.montecarlo.interestrate.covariancemodels.LIBORVolatilityModel;
import net.finmath.montecarlo.interestrate.covariancemodels.LIBORVolatilityModelFromGivenMatrix;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORCorrelationModelExponentialDecay;
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
 * The unit test has currently only an assert for a single (selected) bucket vega,
 * because a finite difference benchmark of all vegas would simply take far too long (hours!).
 * (But I did that benchmark once ;-).
 *
 * The unit test uses a smaller volatility time discretization to reduce memory requirements
 * and allow the unit test to run on the continuous integration server.
 *
 * @author Christian Fries
 */
@RunWith(Parameterized.class)
public class LIBORMarketModelNormalAADSensitivitiesTest {

	private static final int numberOfPaths		= 5000; // 15000; more possible if memory of unit test is increased.
	private static final int numberOfFactors	= 1;
	private static final boolean isUsePartialSetOfDifferentiables = false;
	private static final boolean isUseReducedVolatilityMatrix = true;

	private static DecimalFormat formatReal1		= new DecimalFormat("####0.0", new DecimalFormatSymbols(Locale.ENGLISH));
	private static DecimalFormat formatSci		= new DecimalFormat(" 0.0000E0", new DecimalFormatSymbols(Locale.ENGLISH));
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
		 * Test cases
		 */

		testParameters.add(new Object[] {
				"Caplet maturity " + 5.0,
				new Caplet(5.0, swapPeriodLength, swaprate),
				Optional.of(10),
				true
		});

		testParameters.add(new Object[] {
				"Caplet maturity " + exerciseDate,
				new Caplet(exerciseDate, swapPeriodLength, swaprate),
				Optional.of((int)Math.round(exerciseDate*2)),
				true
		});

		testParameters.add(new Object[] {
				"Swaption " + exerciseDate + " in " + paymentDates[paymentDates.length-1],
				new Swaption(exerciseDate, fixingDates, paymentDates, periodLengths, swaprates),
				Optional.of((int)Math.round(exerciseDate*2)),
				true
		});

		testParameters.add(new Object[] {
				"Bermudan Swaption " + exerciseDate + " in " + paymentDates[paymentDates.length-1],
				new BermudanSwaption(isPeriodStartDateExerciseDate, fixingDates, periodLengths, paymentDates, periodNotionals, swaprates),
				Optional.of((int)Math.round(exerciseDate*2)),
				true
		});

		/*
		 * If you like to measure the performance, add the following test cases because
		 * the result will become more accurate after JVM warm up / hot spot optimization.
		 */
		boolean isRunPerformanceMesurement = false;

		if(isRunPerformanceMesurement) {
			testParameters.add(new Object[] {
					"Bermudan Swaption " + exerciseDate + " in " + paymentDates[paymentDates.length-1],
					new BermudanSwaption(isPeriodStartDateExerciseDate, fixingDates, periodLengths, paymentDates, periodNotionals, swaprates),
					Optional.of((int)Math.round(exerciseDate*2)),
					true
			});
			testParameters.add(new Object[] {
					"Bermudan Swaption " + exerciseDate + " in " + paymentDates[paymentDates.length-1],
					new BermudanSwaption(isPeriodStartDateExerciseDate, fixingDates, periodLengths, paymentDates, periodNotionals, swaprates),
					Optional.of((int)Math.round(exerciseDate*2)),
					true
			});
			testParameters.add(new Object[] {
					"Bermudan Swaption " + exerciseDate + " in " + paymentDates[paymentDates.length-1],
					new BermudanSwaption(isPeriodStartDateExerciseDate, fixingDates, periodLengths, paymentDates, periodNotionals, swaprates),
					Optional.of((int)Math.round(exerciseDate*2)),
					true
			});
			testParameters.add(new Object[] {
					"Bermudan Swaption " + exerciseDate + " in " + paymentDates[paymentDates.length-1],
					new BermudanSwaption(isPeriodStartDateExerciseDate, fixingDates, periodLengths, paymentDates, periodNotionals, swaprates),
					Optional.of((int)Math.round(exerciseDate*2)),
					true
			});
			testParameters.add(new Object[] {
					"Bermudan Swaption " + exerciseDate + " in " + paymentDates[paymentDates.length-1],
					new BermudanSwaption(isPeriodStartDateExerciseDate, fixingDates, periodLengths, paymentDates, periodNotionals, swaprates),
					Optional.of((int)Math.round(exerciseDate*2)),
					true
			});
			testParameters.add(new Object[] {
					"Bermudan Swaption " + exerciseDate + " in " + paymentDates[paymentDates.length-1],
					new BermudanSwaption(isPeriodStartDateExerciseDate, fixingDates, periodLengths, paymentDates, periodNotionals, swaprates),
					Optional.of((int)Math.round(exerciseDate*2)),
					true
			});
			testParameters.add(new Object[] {
					"Bermudan Swaption " + exerciseDate + " in " + paymentDates[paymentDates.length-1],
					new BermudanSwaption(isPeriodStartDateExerciseDate, fixingDates, periodLengths, paymentDates, periodNotionals, swaprates),
					Optional.of((int)Math.round(exerciseDate*2)),
					true
			});
			testParameters.add(new Object[] {
					"Caplet maturity " + exerciseDate,
					new Caplet(exerciseDate, swapPeriodLength, swaprate),
					Optional.of((int)Math.round(exerciseDate*2)),
					true
			});

			testParameters.add(new Object[] {
					"Swaption " + exerciseDate + " in " + paymentDates[paymentDates.length-1],
					new Swaption(exerciseDate, fixingDates, paymentDates, periodLengths, swaprates),
					Optional.of((int)Math.round(exerciseDate*2)),
					true
			});

			testParameters.add(new Object[] {
					"Bermudan Swaption " + exerciseDate + " in " + paymentDates[paymentDates.length-1],
					new BermudanSwaption(isPeriodStartDateExerciseDate, fixingDates, periodLengths, paymentDates, periodNotionals, swaprates),
					Optional.of((int)Math.round(exerciseDate*2)),
					true
			});
		}

		return testParameters;
	}

	private final String productName;
	private final AbstractLIBORMonteCarloProduct product;
	private final Optional<Integer> bucketDeltaLIBORIndex;
	private final boolean isVerbose;

	private LIBORVolatilityModel volatilityModel;

	public static void main(String args[]) throws CalculationException {
		Object[] data = (Object[])data().toArray(new Object[4])[3];

		for(int i=40; i<80; i++) {
			LIBORMarketModelNormalAADSensitivitiesTest test = new LIBORMarketModelNormalAADSensitivitiesTest(
					(String)data[0],
					(AbstractLIBORMonteCarloProduct)data[1],
					Optional.empty(),
					//					i, //(int)data[2]
					false
					);

			test.testDelta();
		}
	}

	public LIBORMarketModelNormalAADSensitivitiesTest(String productName, AbstractLIBORMonteCarloProduct product, Optional<Integer> bucketDeltaLIBORIndex, boolean isVerbose) {
		this.productName = productName;
		this.product = product;
		this.bucketDeltaLIBORIndex = bucketDeltaLIBORIndex;
		this.isVerbose = isVerbose;
	}

	public LIBORModelMonteCarloSimulationInterface createLIBORMarketModel(
			AbstractRandomVariableFactory randomVariableFactoryInitialValue,
			AbstractRandomVariableFactory randomVariableFactoryVolatility,
			int numberOfPaths, int numberOfFactors, double correlationDecayParam,
			Optional<Integer> deltaBucketLiborIndex, double deltaShift,
			int volatilityBucketTimeIndex, int volatilityBucketLiborIndex, double shift
			) throws CalculationException {

		/*
		 * Create the libor tenor structure and the initial values
		 */
		double liborPeriodLength	= 0.5;
		double liborRateTimeHorzion	= 40.0;
		TimeDiscretization liborPeriodDiscretization = new TimeDiscretization(0.0, (int) (liborRateTimeHorzion / liborPeriodLength), liborPeriodLength);

		double[] times = liborPeriodDiscretization.getAsDoubleArray();
		double[] rates = new double[times.length];
		if(deltaBucketLiborIndex.isPresent()) {
			Arrays.fill(rates, 0.05);
			rates[deltaBucketLiborIndex.get()] += deltaShift;
		}
		else {
			Arrays.fill(rates, 0.05 + deltaShift);
		}

		// Create the forward curve (initial value of the LIBOR market model)
		ForwardCurve forwardCurve = ForwardCurve.createForwardCurveFromForwards(
				"forwardCurve"								/* name of the curve */,
				times										/* fixings of the forward */,
				rates										/* forwards */,
				liborPeriodLength							/* tenor / period length */
				);

		/*
		 * Create a simulation time discretization
		 */
		double lastTime	= 40.0;
		double dt		= 0.125;

		TimeDiscretization timeDiscretization = new TimeDiscretization(0.0, (int) (lastTime / dt), dt);

		// Use smaller volatility discretization.
		TimeDiscretization timeDiscretizationVolatility;
		if(isUseReducedVolatilityMatrix) {
			timeDiscretizationVolatility = new TimeDiscretization(0.0, 8, 8.0);
		}
		else {
			timeDiscretizationVolatility = liborPeriodDiscretization;//timeDiscretization;
		}

		/*
		 * Create a volatility structure v[i][j] = sigma_j(t_i)
		 */
		double d = 0.3 / 20.0 / 2.0;
		//		double d = 0.03 / 20.0 / 2.0;
		RandomVariableInterface[][] volatilityMatrix = new RandomVariableInterface[timeDiscretizationVolatility.getNumberOfTimeSteps()][timeDiscretizationVolatility.getNumberOfTimeSteps()];
		for(int timeIndex=0; timeIndex<volatilityMatrix.length; timeIndex++) {
			for(int componentIndex=0; componentIndex<volatilityMatrix[timeIndex].length; componentIndex++) {
				if(isUsePartialSetOfDifferentiables && timeIndex < volatilityBucketTimeIndex) {
					volatilityMatrix[timeIndex][componentIndex] = randomVariableFactoryVolatility.createRandomVariable(d);
				}
				else {
					volatilityMatrix[timeIndex][componentIndex] = randomVariableFactoryVolatility.createRandomVariable(d);
				}
			}
		}
		volatilityMatrix[volatilityBucketTimeIndex][volatilityBucketLiborIndex] = volatilityMatrix[volatilityBucketTimeIndex][volatilityBucketLiborIndex].add(shift);
		volatilityModel = new LIBORVolatilityModelFromGivenMatrix(timeDiscretizationVolatility, timeDiscretizationVolatility, volatilityMatrix);

		/*
		 * Create a correlation model rho_{i,j} = exp(-a * abs(T_i-T_j))
		 */
		LIBORCorrelationModelExponentialDecay correlationModel = new LIBORCorrelationModelExponentialDecay(
				timeDiscretizationVolatility, timeDiscretizationVolatility, numberOfFactors,
				correlationDecayParam);


		/*
		 * Combine volatility model and correlation model to a covariance model
		 */
		LIBORCovarianceModelFromVolatilityAndCorrelation covarianceModel =
				new LIBORCovarianceModelFromVolatilityAndCorrelation(timeDiscretizationVolatility,
						timeDiscretizationVolatility, volatilityModel, correlationModel);

		// BlendedLocalVolatlityModel (future extension)
		//		AbstractLIBORCovarianceModel covarianceModel2 = new BlendedLocalVolatlityModel(covarianceModel, 0.00, false);

		// Set model properties
		Map<String, Object> properties = new HashMap<String, Object>();

		// Choose the simulation measure
		properties.put("measure", LIBORMarketModel.Measure.SPOT.name());

		// Choose log normal model
		properties.put("stateSpace", LIBORMarketModel.StateSpace.NORMAL.name());

		// Current the calculation of delta with AAD requires that we disable liborCap (on enable retaining of all AAD nodes)
		properties.put("liborCap", Double.POSITIVE_INFINITY);

		// Empty array of calibration items - hence, model will use given covariance
		LIBORMarketModel.CalibrationItem[] calibrationItems = new LIBORMarketModel.CalibrationItem[0];

		/*
		 * Create corresponding LIBOR Market Model
		 */
		DiscountCurveFromForwardCurve discountCurve = new DiscountCurveFromForwardCurve(forwardCurve);//null:
		LIBORMarketModelInterface liborMarketModel = new LIBORMarketModel(liborPeriodDiscretization, null, forwardCurve, discountCurve, randomVariableFactoryInitialValue, covarianceModel, calibrationItems, properties);

		BrownianMotionInterface brownianMotion = new net.finmath.montecarlo.BrownianMotion(timeDiscretization, numberOfFactors, numberOfPaths, 3141 /* seed */);

		ProcessEulerScheme process = new ProcessEulerScheme(brownianMotion, ProcessEulerScheme.Scheme.EULER);

		return new LIBORModelMonteCarloSimulation(liborMarketModel, process);
	}

	/**
	 * A test for the LIBOR Market Model vega calculated by AAD.
	 * The test calculates all model vegas using AAD, but only one model vega using finite difference to benchmark that one.
	 * @throws CalculationException
	 */
	@Test
	public void testVega() throws CalculationException {

		int bucketVegaTimeIndex;
		int bucketVegaLIBORIndex;
		double bucketShift;
		if(isUseReducedVolatilityMatrix) {
			bucketVegaTimeIndex = 2;
			bucketVegaLIBORIndex = 3;
			bucketShift = 1E-8;
		}
		else {
			bucketVegaTimeIndex = 40;
			bucketVegaLIBORIndex = 50;
			bucketShift = 1E-4;
		}

		System.out.println(product.getClass().getSimpleName() + ": " + productName);
		System.out.println("_______________________________________________________________________");

		// Create a libor market model
		AbstractRandomVariableFactory randomVariableFactoryInitialValue = new RandomVariableFactory();
		AbstractRandomVariableFactory randomVariableFactoryVolatility = new RandomVariableDifferentiableAADFactory();
		LIBORModelMonteCarloSimulationInterface liborMarketModel = createLIBORMarketModel(randomVariableFactoryVolatility, randomVariableFactoryVolatility,  numberOfPaths, numberOfFactors, 0.0 /* Correlation */,
				Optional.empty(), 0.0, bucketVegaTimeIndex, 0, 0.0);

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
		catch(java.lang.ClassCastException e) {}

		long timingGradientEnd = System.currentTimeMillis();
		long memoryEnd = getAllocatedMemory();

		int numberOfVegasTheoretical	= 0;
		int numberOfVegasEffective	= 0;
		double[][] modelVegas = new double[volatilityModel.getTimeDiscretization().getNumberOfTimeSteps()][volatilityModel.getLiborPeriodDiscretization().getNumberOfTimeSteps()];
		if(gradient != null) {
			for(int timeIndex=0; timeIndex<volatilityModel.getTimeDiscretization().getNumberOfTimeSteps(); timeIndex++) {
				for(int componentIndex=0; componentIndex<volatilityModel.getLiborPeriodDiscretization().getNumberOfTimeSteps(); componentIndex++) {
					double modelVega = 0.0;
					RandomVariableInterface volatility = volatilityModel.getVolatility(timeIndex, componentIndex);
					if(volatility != null && volatility instanceof RandomVariableDifferentiableInterface) {
						numberOfVegasTheoretical++;
						RandomVariableInterface modelVegaRandomVariable = gradient.get(((RandomVariableDifferentiableInterface)volatility).getID());
						if(modelVegaRandomVariable != null) {
							modelVega = modelVegaRandomVariable.getAverage();
							numberOfVegasEffective++;
						}
					}
					//					System.out.println(volatilityModel.getTimeDiscretization().getTime(timeIndex) + "\t" + volatilityModel.getLiborPeriodDiscretization().getTime(componentIndex) + "\t" + modelVega);
					modelVegas[timeIndex][componentIndex] = modelVega;
					//					System.out.print(formatSci.format(modelVega) + "\t");
				}
				//System.out.println();
			}
		}
		//		RandomVariableInterface modelDelta = gradient.get(liborMarketModel.getLIBOR(0, 0));

		// Free memory
		liborMarketModel = null;
		gradient = null;

		/*
		 * Test valuation
		 */

		LIBORModelMonteCarloSimulationInterface liborMarketModelPlain = createLIBORMarketModel(new RandomVariableFactory(), new RandomVariableFactory(),  numberOfPaths, numberOfFactors, 0.0 /* Correlation */,
				Optional.empty(), 0.0, 0, 0, 0.0);

		long timingCalculation2Start = System.currentTimeMillis();

		double valueSimulation2 = product.getValue(liborMarketModelPlain);

		long timingCalculation2End = System.currentTimeMillis();

		// Free memory
		liborMarketModelPlain = null;

		/*
		 * Test results against finite difference implementation
		 * For performance reasons we test one bucket only
		 */

		long timingCalculation3Start = System.currentTimeMillis();

		LIBORModelMonteCarloSimulationInterface liborMarketModelDnShift = createLIBORMarketModel(new RandomVariableFactory(), new RandomVariableFactory(),  numberOfPaths, numberOfFactors, 0.0 /* Correlation */,
				Optional.empty(), 0.0, bucketVegaTimeIndex, bucketVegaLIBORIndex, -bucketShift);
		double valueSimulationDown = product.getValue(liborMarketModelDnShift);

		LIBORModelMonteCarloSimulationInterface liborMarketModelUpShift = createLIBORMarketModel(new RandomVariableFactory(), new RandomVariableFactory(),  numberOfPaths, numberOfFactors, 0.0 /* Correlation */,
				Optional.empty(), 0.0, bucketVegaTimeIndex, bucketVegaLIBORIndex, bucketShift);
		double valueSimulationUp = product.getValue(liborMarketModelUpShift);

		double bucketVega = (valueSimulationUp-valueSimulationDown) / bucketShift / 2;

		long timingCalculation3End = System.currentTimeMillis();

		// Free memory
		liborMarketModelDnShift = null;
		liborMarketModelUpShift = null;

		long memoryEnd2 = getAllocatedMemory();

		/*
		 * Print status
		 */

		System.out.println("FD vega..." + bucketVega);
		System.out.println("AD vega..." + modelVegas[bucketVegaTimeIndex][bucketVegaLIBORIndex]);

		System.out.println("value...........................: " + formatterValue.format(valueSimulation));
		System.out.println("value (plain)...................: " + formatterValue.format(valueSimulation2));
		System.out.println("evaluation (plain)..............: " + formatReal1.format((timingCalculation2End-timingCalculation2Start)/1000.0) + " s");
		System.out.println("evaluation (AAD)................: " + formatReal1.format((timingCalculationEnd-timingCalculationStart)/1000.0) + " s");
		System.out.println("derivative (plain) (1 bucket)...: " + formatReal1.format((timingCalculation3End-timingCalculation3Start)/1000.0) + " s");
		System.out.println("derivative (AAD).(all buckets)..: " + formatReal1.format((timingGradientEnd-timingGradientStart)/1000.0) + " s");
		System.out.println("number of vegas (theoretical)...: " + numberOfVegasTheoretical);
		System.out.println("number of vegas (effective).....: " + numberOfVegasEffective);
		System.out.println("memory (AAD)....................: " + formatReal1.format(((double)(memoryEnd-memoryStart))/1024.0/1024.0) + " M");
		System.out.println("memory (check)-.................: " + formatReal1.format(((double)(memoryEnd2-memoryStart))/1024.0/1024.0) + " M");
		System.out.println("\n");

		Assert.assertEquals("Valuation", valueSimulation2, valueSimulation, 0.0 /* delta */);
		Assert.assertEquals("Comparing FD and AD vega", bucketVega, modelVegas[bucketVegaTimeIndex][bucketVegaLIBORIndex], 1.3E-2);
	}

	/**
	 * A test for the LIBOR Market Model delta calculated by AAD.
	 * The test calculates all model deltas using AAD, but only one model delta using finite difference to benchmark that one.
	 * @throws CalculationException
	 */
	@Test
	public void testDelta() throws CalculationException {

		double bucketShift = 1E-2;

		/*
		 * Create a libor market model with appropriate AAD random variable factory
		 */

		// The following properties control the approximation of the derivatives of conditional expectation operators and indicator functions
		Map<String, Object> randomVariableProps = new HashMap<String, Object>();
		randomVariableProps.put("diracDeltaApproximationWidthPerStdDev", 0.05);
		//		randomVariableProps.put("diracDeltaApproximationMethod", "ZERO");		// Switches of differentiation of exercise boundary
		RandomVariableDifferentiableAADFactory randomVariableFactoryAAD = new RandomVariableDifferentiableAADFactory(new RandomVariableFactory(), randomVariableProps);

		AbstractRandomVariableFactory randomVariableFactoryInitialValue = randomVariableFactoryAAD;
		AbstractRandomVariableFactory randomVariableFactoryVolatility = new RandomVariableFactory();
		LIBORModelMonteCarloSimulationInterface liborMarketModel = createLIBORMarketModel(randomVariableFactoryInitialValue, randomVariableFactoryVolatility,  numberOfPaths, numberOfFactors, 0.0 /* Correlation */, Optional.empty(), 0.0, 0, 0, 0.0);

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
		catch(java.lang.ClassCastException e) {
			System.out.println(e.getMessage());
		}

		long timingGradientEnd = System.currentTimeMillis();
		long memoryEnd = getAllocatedMemory();

		int numberOfDeltasTheoretical	= 0;
		int numberOfDeltasEffective	= 0;
		double[] modelDeltas = new double[liborMarketModel.getNumberOfLibors()];
		if(gradient != null) {
			for(int componentIndex=0; componentIndex<liborMarketModel.getLiborPeriodDiscretization().getNumberOfTimeSteps(); componentIndex++) {
				double modelDelta = 0.0;
				RandomVariableInterface forwardRate = liborMarketModel.getLIBOR(0, componentIndex);
				if(forwardRate != null && forwardRate instanceof RandomVariableDifferentiableInterface) {
					numberOfDeltasTheoretical++;
					RandomVariableInterface modelDeltaRandomVariable = gradient.get(((RandomVariableDifferentiableInterface)forwardRate).getID());
					if(modelDeltaRandomVariable != null) {
						modelDelta = modelDeltaRandomVariable.getAverage();
						numberOfDeltasEffective++;
					}
				}
				//					System.out.println(volatilityModel.getTimeDiscretization().getTime(timeIndex) + "\t" + volatilityModel.getLiborPeriodDiscretization().getTime(componentIndex) + "\t" + modelDelta);
				modelDeltas[componentIndex] = modelDelta;
				//					System.out.print(formatSci.format(modelDelta) + "\t");
			}
			//System.out.println();
		}
		//		RandomVariableInterface modelDelta = gradient.get(liborMarketModel.getLIBOR(0, 0));

		// Free memory
		liborMarketModel = null;
		gradient = null;

		/*
		 * Test valuation
		 */

		LIBORModelMonteCarloSimulationInterface liborMarketModelPlain = createLIBORMarketModel(new RandomVariableFactory(), new RandomVariableFactory(),  numberOfPaths, numberOfFactors, 0.0 /* Correlation */,
				Optional.empty(), 0.0, 0, 0, 0);

		long timingCalculation2Start = System.currentTimeMillis();

		double valueSimulation2 = product.getValue(liborMarketModelPlain);

		long timingCalculation2End = System.currentTimeMillis();

		// Free memory
		liborMarketModelPlain = null;

		/*
		 * Test results against finite difference implementation
		 * For performance reasons we test one bucket only
		 */

		long timingCalculation3Start = System.currentTimeMillis();

		LIBORModelMonteCarloSimulationInterface liborMarketModelDnShift = createLIBORMarketModel(new RandomVariableFactory(), new RandomVariableFactory(),  numberOfPaths, numberOfFactors, 0.0 /* Correlation */,
				bucketDeltaLIBORIndex, -bucketShift, 0, 0, 0.0);
		double valueSimulationDown = product.getValue(liborMarketModelDnShift);

		LIBORModelMonteCarloSimulationInterface liborMarketModelUpShift = createLIBORMarketModel(new RandomVariableFactory(), new RandomVariableFactory(),  numberOfPaths, numberOfFactors, 0.0 /* Correlation */,
				bucketDeltaLIBORIndex, bucketShift, 0, 0, 0.0);
		double valueSimulationUp = product.getValue(liborMarketModelUpShift);

		double bucketDelta = (valueSimulationUp-valueSimulationDown) / bucketShift / 2;

		long timingCalculation3End = System.currentTimeMillis();

		// Free memory
		liborMarketModelDnShift = null;
		liborMarketModelUpShift = null;

		long memoryEnd2 = getAllocatedMemory();

		/*
		 * Calculate delta - in case we calculate parallel delta.
		 */

		double deltaAAD;
		if(bucketDeltaLIBORIndex.isPresent()) {
			deltaAAD = modelDeltas[bucketDeltaLIBORIndex.get()];
		}
		else {
			deltaAAD = Arrays.stream(modelDeltas).sum();
		}

		/*
		 * Print status
		 */
		if(isVerbose) {
			System.out.println(product.getClass().getSimpleName() + ": " + productName);
			System.out.println("_______________________________________________________________________");

			System.out.println("value......: " + value.getAverage());
			System.out.println("FD Delta...: " + bucketDelta);
			System.out.println("AD Delta...: " + deltaAAD);

			System.out.println("value...........................: " + formatterValue.format(valueSimulation));
			System.out.println("value (plain)...................: " + formatterValue.format(valueSimulation2));
			System.out.println("evaluation (plain)..............: " + formatReal1.format((timingCalculation2End-timingCalculation2Start)/1000.0) + " s");
			System.out.println("evaluation (AAD)................: " + formatReal1.format((timingCalculationEnd-timingCalculationStart)/1000.0) + " s");
			System.out.println("derivative (plain) (1 bucket)...: " + formatReal1.format((timingCalculation3End-timingCalculation3Start)/1000.0) + " s");
			System.out.println("derivative (AAD).(all buckets)..: " + formatReal1.format((timingGradientEnd-timingGradientStart)/1000.0) + " s");
			System.out.println("number of Deltas (theoretical)...: " + numberOfDeltasTheoretical);
			System.out.println("number of Deltas (effective).....: " + numberOfDeltasEffective);
			System.out.println("memory (AAD)....................: " + formatReal1.format(((double)(memoryEnd-memoryStart))/1024.0/1024.0) + " M");
			System.out.println("memory (check)-.................: " + formatReal1.format(((double)(memoryEnd2-memoryStart))/1024.0/1024.0) + " M");
			System.out.println("\n");

			Assert.assertEquals("Valuation", valueSimulation2, valueSimulation, 0.0 /* delta */);
			Assert.assertEquals("Comparing FD and AD Delta", bucketDelta, deltaAAD, 1.3E-2);
		}
		else {
			System.out.println(bucketDeltaLIBORIndex + "\t" + value.getAverage() + "\t" + bucketDelta + "\t" + deltaAAD);
		}
	}

	static long getAllocatedMemory() {
		System.gc();
		System.gc();
		System.gc();
		long allocatedMemory = (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());
		return allocatedMemory;
	}
}
