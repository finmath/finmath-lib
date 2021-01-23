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
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.finmath.exception.CalculationException;
import net.finmath.functions.AnalyticFormulas;
import net.finmath.marketdata.model.curves.DiscountCurveFromForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurveInterpolation;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFromArrayFactory;
import net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiable;
import net.finmath.montecarlo.automaticdifferentiation.backward.RandomVariableDifferentiableAADFactory;
import net.finmath.montecarlo.interestrate.models.LIBORMarketModelFromCovarianceModel;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCorrelationModelExponentialDecay;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCovarianceModelFromVolatilityAndCorrelation;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORVolatilityModel;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORVolatilityModelFromGivenMatrix;
import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.montecarlo.interestrate.products.BermudanSwaption;
import net.finmath.montecarlo.interestrate.products.Caplet;
import net.finmath.montecarlo.interestrate.products.Swaption;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * This class tests the sensitivities calculated from a LIBOR market model and products.
 * We have test for delta and vega (using AAD and finite differences).
 *
 * The unit test has currently only an assert for a single (selected) bucket,
 * because a finite difference benchmark of all buckets would simply take far too long (hours!).
 * (But I did that benchmark once ;-).
 *
 * For vega: The unit test uses a smaller volatility time discretization to reduce memory requirements
 * and allow the unit test to run on the continuous integration server.
 *
 * @author Christian Fries
 */
@RunWith(Parameterized.class)
public class LIBORMarketModelNormalAADSensitivitiesTest {

	private static final int numberOfPaths		= 5000; // 15000; more possible if memory of unit test is increased.
	private static final int numberOfFactors	= 1;
	private static final int seed	= 1352;//3141;
	private static final boolean isUsePartialSetOfDifferentiables = false;
	private static final boolean isUseReducedVolatilityMatrix = true;

	private static DecimalFormat formatReal1		= new DecimalFormat("####0.0", new DecimalFormatSymbols(Locale.ENGLISH));
	private static DecimalFormat formatSci		= new DecimalFormat(" 0.0000E0", new DecimalFormatSymbols(Locale.ENGLISH));
	private static DecimalFormat formatterValue		= new DecimalFormat(" ##0.000%;-##0.000%", new DecimalFormatSymbols(Locale.ENGLISH));

	@Parameters(name="{0}")
	public static Collection<Object[]> data() {
		final Collection<Object[]> testParameters = new ArrayList<>();

		final double liborPeriodLength	= 0.5;
		final double liborRateTimeHorzion	= 40.0;
		final TimeDiscretizationFromArray liborPeriodDiscretization = new TimeDiscretizationFromArray(0.0, (int) (liborRateTimeHorzion / liborPeriodLength), liborPeriodLength);

		// Create the forward curve (initial value of the LIBOR market model)
		final ForwardCurveInterpolation forwardCurveInterpolation = ForwardCurveInterpolation.createForwardCurveFromForwards(
				"forwardCurve"								/* name of the curve */,
				new double[] {0.5 , 1.0 , 2.0 , 5.0 , 40.0}	/* fixings of the forward */,
				new double[] {0.05, 0.05, 0.05, 0.05, 0.05}	/* forwards */,
				liborPeriodLength							/* tenor / period length */
				);

		final int maturityIndex = 60;
		final double exerciseDate = liborPeriodDiscretization.getTime(maturityIndex);

		final int numberOfPeriods = 20;

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
		final double swaprate = net.finmath.marketdata.products.Swap.getForwardSwapRate(new TimeDiscretizationFromArray(swapTenor), new TimeDiscretizationFromArray(swapTenor), forwardCurveInterpolation, new DiscountCurveFromForwardCurve(forwardCurveInterpolation));

		// Set swap rates for each period
		final double[] swaprates = new double[numberOfPeriods];
		Arrays.fill(swaprates, swaprate);

		final double[] periodLengths = new double[numberOfPeriods];
		Arrays.fill(periodLengths, swapPeriodLength);

		final double[] periodNotionals = new double[numberOfPeriods];
		Arrays.fill(periodNotionals, 1.0);

		final boolean[] isPeriodStartDateExerciseDate = new boolean[numberOfPeriods];
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
				"Caplet maturity " + 10.0,
				new Caplet(10.0, swapPeriodLength, swaprate),
				Optional.of(20),
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
		final boolean isRunPerformanceMesurement = false;

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

	public static void main(final String[] args) throws CalculationException {
		final Object[] data = (Object[])data().toArray(new Object[4])[3];

		final boolean isParalellSensi = false;

		if(isParalellSensi) {
			new LIBORMarketModelNormalAADSensitivitiesTest(
					(String)data[0],
					(AbstractLIBORMonteCarloProduct)data[1],
					Optional.empty(),
					false
					).testDelta();
		}
		else {
			new LIBORMarketModelNormalAADSensitivitiesTest(
					(String)data[0],
					(AbstractLIBORMonteCarloProduct)data[1],
					Optional.empty(),
					false
					).testDelta();

			for(int i=0; i<80; i++) {
				new LIBORMarketModelNormalAADSensitivitiesTest(
						(String)data[0],
						(AbstractLIBORMonteCarloProduct)data[1],
						Optional.of(i), //(int)data[2]
						false
						).testDelta();
			}
		}
	}

	public LIBORMarketModelNormalAADSensitivitiesTest(final String productName, final AbstractLIBORMonteCarloProduct product, final Optional<Integer> bucketDeltaLIBORIndex, final boolean isVerbose) {
		this.productName = productName;
		this.product = product;
		this.bucketDeltaLIBORIndex = bucketDeltaLIBORIndex;
		this.isVerbose = isVerbose;
	}

	public LIBORModelMonteCarloSimulationModel createLIBORMarketModel(
			final RandomVariableFactory randomVariableFactoryInitialValue,
			final RandomVariableFactory randomVariableFactoryVolatility,
			final int numberOfPaths, final int numberOfFactors, final double correlationDecayParam,
			final Optional<Integer> deltaBucketLiborIndex, final double deltaShift,
			final int volatilityBucketTimeIndex, final int volatilityBucketLiborIndex, final double shift
			) throws CalculationException {

		/*
		 * Create the libor tenor structure and the initial values
		 */
		final double liborPeriodLength	= 0.5;
		final double liborRateTimeHorzion	= 40.0;
		final TimeDiscretizationFromArray liborPeriodDiscretization = new TimeDiscretizationFromArray(0.0, (int) (liborRateTimeHorzion / liborPeriodLength), liborPeriodLength);

		final double[] times = liborPeriodDiscretization.getAsDoubleArray();
		final double[] rates = new double[times.length];
		if(deltaBucketLiborIndex.isPresent()) {
			Arrays.fill(rates, 0.05);
			rates[deltaBucketLiborIndex.get()] += deltaShift;
		}
		else {
			Arrays.fill(rates, 0.05 + deltaShift);
		}

		// Create the forward curve (initial value of the LIBOR market model)
		final ForwardCurveInterpolation forwardCurveInterpolation = ForwardCurveInterpolation.createForwardCurveFromForwards(
				"forwardCurve"								/* name of the curve */,
				times										/* fixings of the forward */,
				rates										/* forwards */,
				liborPeriodLength							/* tenor / period length */
				);

		/*
		 * Create a simulation time discretization
		 */
		final double lastTime	= 40.0;
		final double dt		= 0.125;

		final TimeDiscretizationFromArray timeDiscretizationFromArray = new TimeDiscretizationFromArray(0.0, (int) (lastTime / dt), dt);

		// Use smaller volatility discretization.
		TimeDiscretizationFromArray timeDiscretizationVolatility;
		if(isUseReducedVolatilityMatrix) {
			timeDiscretizationVolatility = new TimeDiscretizationFromArray(0.0, 4, 10.0);
		}
		else {
			timeDiscretizationVolatility = liborPeriodDiscretization;//timeDiscretizationFromArray;
		}

		/*
		 * Create a volatility structure v[i][j] = sigma_j(t_i)
		 */
		final double d = 0.3 / 20.0 / 2.0;
		//		double d = 0.03 / 20.0 / 2.0;
		final RandomVariable[][] volatilityMatrix = new RandomVariable[timeDiscretizationVolatility.getNumberOfTimeSteps()][timeDiscretizationVolatility.getNumberOfTimeSteps()];
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
		final LIBORCorrelationModelExponentialDecay correlationModel = new LIBORCorrelationModelExponentialDecay(
				timeDiscretizationVolatility, timeDiscretizationVolatility, numberOfFactors,
				correlationDecayParam);


		/*
		 * Combine volatility model and correlation model to a covariance model
		 */
		final LIBORCovarianceModelFromVolatilityAndCorrelation covarianceModel =
				new LIBORCovarianceModelFromVolatilityAndCorrelation(timeDiscretizationVolatility,
						timeDiscretizationVolatility, volatilityModel, correlationModel);

		// BlendedLocalVolatlityModel (future extension)
		//		AbstractLIBORCovarianceModel covarianceModel2 = new BlendedLocalVolatlityModel(covarianceModel, 0.00, false);

		// Set model properties
		final Map<String, Object> properties = new HashMap<>();

		// Choose the simulation measure
		properties.put("measure", LIBORMarketModelFromCovarianceModel.Measure.SPOT.name());

		// Choose log normal model
		properties.put("stateSpace", LIBORMarketModelFromCovarianceModel.StateSpace.NORMAL.name());

		// Current the calculation of delta with AAD requires that we disable liborCap (on enable retaining of all AAD nodes)
		properties.put("liborCap", Double.POSITIVE_INFINITY);

		// Empty array of calibration items - hence, model will use given covariance
		final CalibrationProduct[] calibrationItems = new CalibrationProduct[0];

		/*
		 * Create corresponding LIBOR Market Model
		 */
		//		DiscountCurveFromForwardCurve discountCurve = new DiscountCurveFromForwardCurve(forwardCurve);
		final DiscountCurveFromForwardCurve discountCurve = null;
		final LIBORMarketModel liborMarketModel = new LIBORMarketModelFromCovarianceModel(liborPeriodDiscretization, null, forwardCurveInterpolation, discountCurve, randomVariableFactoryInitialValue, covarianceModel, calibrationItems, properties);

		final BrownianMotion brownianMotion = new net.finmath.montecarlo.BrownianMotionLazyInit(timeDiscretizationFromArray, numberOfFactors, numberOfPaths, seed);

		final EulerSchemeFromProcessModel process = new EulerSchemeFromProcessModel(liborMarketModel, brownianMotion, EulerSchemeFromProcessModel.Scheme.EULER);

		return new LIBORMonteCarloSimulationFromLIBORModel(liborMarketModel, process);
	}

	/**
	 * A test for the LIBOR Market Model vega calculated by AAD.
	 * The test calculates all model vegas using AAD, but only one model vega using finite difference to benchmark that one.
	 *
	 * @throws CalculationException Thrown if valuation inside calibration fails.
	 */
	@Test
	public void testVega() throws CalculationException {

		int bucketVegaTimeIndex;
		int bucketVegaLIBORIndex;
		double bucketShift;
		if(isUseReducedVolatilityMatrix) {
			bucketVegaTimeIndex = 2;
			bucketVegaLIBORIndex = 2;
			bucketShift = 1E-8;	// Finite difference vega has problems for larger shifts
		}
		else {
			bucketVegaTimeIndex = 40;
			bucketVegaLIBORIndex = 50;
			bucketShift = 1E-4;
		}

		System.out.println(product.getClass().getSimpleName() + ": " + productName);
		System.out.println("_______________________________________________________________________");

		// Create a libor market model
		final RandomVariableFactory randomVariableFactoryInitialValue = new RandomVariableFromArrayFactory();
		final RandomVariableFactory randomVariableFactoryVolatility = new RandomVariableDifferentiableAADFactory();
		LIBORModelMonteCarloSimulationModel liborMarketModel = createLIBORMarketModel(randomVariableFactoryVolatility, randomVariableFactoryVolatility,  numberOfPaths, numberOfFactors, 0.0 /* Correlation */,
				Optional.empty(), 0.0, bucketVegaTimeIndex, 0, 0.0);

		/*
		 * Test valuation
		 */
		final long memoryStart = getAllocatedMemory();
		final long timingCalculationStart = System.currentTimeMillis();

		final RandomVariable value = product.getValue(0.0, liborMarketModel);
		final double valueSimulation = value.getAverage();

		final long timingCalculationEnd = System.currentTimeMillis();


		/*
		 * Test gradient
		 */

		final long timingGradientStart = System.currentTimeMillis();

		Map<Long, RandomVariable> gradient = null;
		try {
			gradient = ((RandomVariableDifferentiable)value).getGradient();
		}
		catch(final java.lang.ClassCastException e) {}

		final long timingGradientEnd = System.currentTimeMillis();
		final long memoryEnd = getAllocatedMemory();

		int numberOfVegasTheoretical	= 0;
		int numberOfVegasEffective	= 0;
		final double[][] modelVegas = new double[volatilityModel.getTimeDiscretization().getNumberOfTimeSteps()][volatilityModel.getLiborPeriodDiscretization().getNumberOfTimeSteps()];
		if(gradient != null) {
			for(int timeIndex=0; timeIndex<volatilityModel.getTimeDiscretization().getNumberOfTimeSteps(); timeIndex++) {
				for(int componentIndex=0; componentIndex<volatilityModel.getLiborPeriodDiscretization().getNumberOfTimeSteps(); componentIndex++) {
					double modelVega = 0.0;
					final RandomVariable volatility = volatilityModel.getVolatility(timeIndex, componentIndex);
					if(volatility != null && volatility instanceof RandomVariableDifferentiable) {
						numberOfVegasTheoretical++;
						final RandomVariable modelVegaRandomVariable = gradient.get(((RandomVariableDifferentiable)volatility).getID());
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
		//		RandomVariable modelDelta = gradient.get(liborMarketModel.getLIBOR(0, 0));

		// Free memory
		liborMarketModel = null;
		gradient = null;

		/*
		 * Test valuation
		 */

		LIBORModelMonteCarloSimulationModel liborMarketModelPlain = createLIBORMarketModel(new RandomVariableFromArrayFactory(), new RandomVariableFromArrayFactory(),  numberOfPaths, numberOfFactors, 0.0 /* Correlation */,
				Optional.empty(), 0.0, 0, 0, 0.0);

		final long timingCalculation2Start = System.currentTimeMillis();

		final double valueSimulation2 = product.getValue(liborMarketModelPlain);

		final long timingCalculation2End = System.currentTimeMillis();

		// Free memory
		liborMarketModelPlain = null;

		/*
		 * Test results against finite difference implementation
		 * For performance reasons we test one bucket only
		 */

		final long timingCalculation3Start = System.currentTimeMillis();

		LIBORModelMonteCarloSimulationModel liborMarketModelDnShift = createLIBORMarketModel(new RandomVariableFromArrayFactory(), new RandomVariableFromArrayFactory(),  numberOfPaths, numberOfFactors, 0.0 /* Correlation */,
				Optional.empty(), 0.0, bucketVegaTimeIndex, bucketVegaLIBORIndex, -bucketShift);
		final double valueSimulationDown = product.getValue(liborMarketModelDnShift);

		LIBORModelMonteCarloSimulationModel liborMarketModelUpShift = createLIBORMarketModel(new RandomVariableFromArrayFactory(), new RandomVariableFromArrayFactory(),  numberOfPaths, numberOfFactors, 0.0 /* Correlation */,
				Optional.empty(), 0.0, bucketVegaTimeIndex, bucketVegaLIBORIndex, bucketShift);
		final double valueSimulationUp = product.getValue(liborMarketModelUpShift);

		final double bucketVega = (valueSimulationUp-valueSimulationDown) / bucketShift / 2;

		final long timingCalculation3End = System.currentTimeMillis();

		// Free memory
		liborMarketModelDnShift = null;
		liborMarketModelUpShift = null;

		final long memoryEnd2 = getAllocatedMemory();

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
		System.out.println("memory (AAD)....................: " + formatReal1.format((memoryEnd-memoryStart)/1024.0/1024.0) + " M");
		System.out.println("memory (check)-.................: " + formatReal1.format((memoryEnd2-memoryStart)/1024.0/1024.0) + " M");
		System.out.println("\n");

		Assert.assertEquals("Valuation", valueSimulation2, valueSimulation, 0.0 /* delta */);
		Assert.assertEquals("Comparing FD and AD vega", bucketVega, modelVegas[bucketVegaTimeIndex][bucketVegaLIBORIndex], 1.3E-2);
	}

	/**
	 * A test for the LIBOR Market Model delta calculated by AAD.
	 * The test calculates all model deltas using AAD, but only one model delta using finite difference to benchmark that one.
	 *
	 * @throws CalculationException Thrown if valuation inside calibration fails.
	 */
	@Test
	public void testDelta() throws CalculationException {

		final double bucketShift = 1E-2;

		/*
		 * Create a libor market model with appropriate AAD random variable factory
		 */

		// The following properties control the approximation of the derivatives of conditional expectation operators and indicator functions
		final Map<String, Object> randomVariableProps = new HashMap<>();
		randomVariableProps.put("diracDeltaApproximationWidthPerStdDev", 0.05);
		//		randomVariableProps.put("diracDeltaApproximationMethod", "ZERO");		// Switches of differentiation of exercise boundary
		final RandomVariableDifferentiableAADFactory randomVariableFactoryAAD = new RandomVariableDifferentiableAADFactory(new RandomVariableFromArrayFactory(), randomVariableProps);

		final RandomVariableFactory randomVariableFactoryInitialValue = randomVariableFactoryAAD;
		final RandomVariableFactory randomVariableFactoryVolatility = new RandomVariableFromArrayFactory();
		LIBORModelMonteCarloSimulationModel liborMarketModel = createLIBORMarketModel(randomVariableFactoryInitialValue, randomVariableFactoryVolatility,  numberOfPaths, numberOfFactors, 0.0 /* Correlation */, Optional.empty(), 0.0, 0, 0, 0.0);

		/*
		 * Test valuation
		 */
		final long memoryStart = getAllocatedMemory();
		final long timingCalculationStart = System.currentTimeMillis();

		final RandomVariable value = product.getValue(0.0, liborMarketModel);
		final double valueSimulation = value.getAverage();

		final long timingCalculationEnd = System.currentTimeMillis();


		/*
		 * Test gradient
		 */

		final long timingGradientStart = System.currentTimeMillis();

		Map<Long, RandomVariable> gradient = null;
		try {
			gradient = ((RandomVariableDifferentiable)value).getGradient();
		}
		catch(final java.lang.ClassCastException e) {
			System.out.println(e.getMessage());
		}

		final long timingGradientEnd = System.currentTimeMillis();
		final long memoryEnd = getAllocatedMemory();

		int numberOfDeltasTheoretical	= 0;
		int numberOfDeltasEffective	= 0;
		final double[] modelDeltas = new double[liborMarketModel.getNumberOfLibors()];
		if(gradient != null) {
			for(int componentIndex=0; componentIndex<liborMarketModel.getLiborPeriodDiscretization().getNumberOfTimeSteps(); componentIndex++) {
				double modelDelta = 0.0;
				final RandomVariable forwardRate = liborMarketModel.getLIBOR(0, componentIndex);
				if(forwardRate != null && forwardRate instanceof RandomVariableDifferentiable) {
					numberOfDeltasTheoretical++;
					final RandomVariable modelDeltaRandomVariable = gradient.get(((RandomVariableDifferentiable)forwardRate).getID());
					if(modelDeltaRandomVariable != null) {
						modelDelta = modelDeltaRandomVariable.getAverage();
						numberOfDeltasEffective++;
					}
				}
				modelDeltas[componentIndex] = modelDelta;
			}
			//System.out.println();
		}

		// Free memory
		liborMarketModel = null;
		gradient = null;

		/*
		 * Test valuation
		 */

		LIBORModelMonteCarloSimulationModel liborMarketModelPlain = createLIBORMarketModel(new RandomVariableFromArrayFactory(), new RandomVariableFromArrayFactory(),  numberOfPaths, numberOfFactors, 0.0 /* Correlation */,
				Optional.empty(), 0.0, 0, 0, 0);

		final long timingCalculation2Start = System.currentTimeMillis();

		final double valueSimulation2 = product.getValue(liborMarketModelPlain);

		final long timingCalculation2End = System.currentTimeMillis();

		// Free memory
		liborMarketModelPlain = null;

		/*
		 * Test results against finite difference implementation
		 * For performance reasons we test one bucket only
		 */

		final long timingCalculation3Start = System.currentTimeMillis();

		LIBORModelMonteCarloSimulationModel liborMarketModelDnShift = createLIBORMarketModel(new RandomVariableFromArrayFactory(), new RandomVariableFromArrayFactory(),  numberOfPaths, numberOfFactors, 0.0 /* Correlation */,
				bucketDeltaLIBORIndex, -bucketShift, 0, 0, 0.0);
		final double valueSimulationDown = product.getValue(liborMarketModelDnShift);

		LIBORModelMonteCarloSimulationModel liborMarketModelUpShift = createLIBORMarketModel(new RandomVariableFromArrayFactory(), new RandomVariableFromArrayFactory(),  numberOfPaths, numberOfFactors, 0.0 /* Correlation */,
				bucketDeltaLIBORIndex, bucketShift, 0, 0, 0.0);
		final double valueSimulationUp = product.getValue(liborMarketModelUpShift);

		final double bucketDelta = (valueSimulationUp-valueSimulationDown) / bucketShift / 2;

		final long timingCalculation3End = System.currentTimeMillis();

		// Free memory
		liborMarketModelDnShift = null;
		liborMarketModelUpShift = null;

		final long memoryEnd2 = getAllocatedMemory();

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
			System.out.println("memory (AAD)....................: " + formatReal1.format((memoryEnd-memoryStart)/1024.0/1024.0) + " M");
			System.out.println("memory (check)-.................: " + formatReal1.format((memoryEnd2-memoryStart)/1024.0/1024.0) + " M");
			System.out.println("\n");

			Assert.assertEquals("Valuation", valueSimulation2, valueSimulation, 0.0 /* delta */);
			Assert.assertEquals("Comparing FD and AD Delta", bucketDelta, deltaAAD, 1.3E-2);
		}
		else {
			System.out.println(bucketDeltaLIBORIndex.orElse(-1) + "\t" + seed + "\t" + value.getAverage() + "\t" + bucketDelta + "\t" + deltaAAD);
		}
	}

	/**
	 * A test for the LIBOR Market Model delta calculated by AAD.
	 * The test calculates all model deltas using AAD, but only one model delta using finite difference to benchmark that one.
	 *
	 * @throws CalculationException Thrown if valuation inside calibration fails.
	 */
	@Test
	public void testGenericDelta() throws CalculationException {

		/*
		 * Create a libor market model with appropriate AAD random variable factory
		 */

		// The following properties control the approximation of the derivatives of conditional expectation operators and indicator functions
		final Map<String, Object> randomVariableProps = new HashMap<>();
		randomVariableProps.put("diracDeltaApproximationWidthPerStdDev", 0.05);
		//		randomVariableProps.put("diracDeltaApproximationMethod", "ZERO");		// Switches of differentiation of exercise boundary
		final RandomVariableDifferentiableAADFactory randomVariableFactoryAAD = new RandomVariableDifferentiableAADFactory(new RandomVariableFromArrayFactory(), randomVariableProps);

		final RandomVariableFactory randomVariableFactoryInitialValue = randomVariableFactoryAAD;
		final RandomVariableFactory randomVariableFactoryVolatility = new RandomVariableFromArrayFactory();
		final LIBORModelMonteCarloSimulationModel liborMarketModel = createLIBORMarketModel(randomVariableFactoryInitialValue, randomVariableFactoryVolatility,  numberOfPaths, numberOfFactors, 0.0 /* Correlation */, Optional.empty(), 0.0, 0, 0, 0.0);

		/*
		 * Test valuation
		 */
		final long memoryStart = getAllocatedMemory();
		final long timingCalculationStart = System.currentTimeMillis();

		final RandomVariable value = product.getValue(0.0, liborMarketModel);
		final double valueSimulation = value.getAverage();

		final long timingCalculationEnd = System.currentTimeMillis();


		/*
		 * Test gradient
		 */

		final long timingGradientStart = System.currentTimeMillis();

		Map<Long, RandomVariable> gradientMap;
		try {
			gradientMap = ((RandomVariableDifferentiable)value).getGradient();
		}
		catch(final java.lang.ClassCastException e) {
			gradientMap = null;
		}
		final Map<Long, RandomVariable> gradient = gradientMap;

		final long timingGradientEnd = System.currentTimeMillis();
		final long memoryEnd = getAllocatedMemory();

		final Map<String, RandomVariable> modelParameters = liborMarketModel.getModelParameters();

		final int numberOfDeltasTheoretical	= 0;
		final int numberOfDeltasEffective	= 0;
		Map<String, Double> modelDeltas = new HashMap<>();
		if(gradient != null) {
			modelDeltas = modelParameters.entrySet().parallelStream().collect(Collectors.toMap(
					new Function<Entry<String, RandomVariable>, String>() {
						@Override
						public String apply(final Entry<String, RandomVariable> entry) {
							return entry.getKey();
						}
					},
					new Function<Entry<String, RandomVariable>, Double>() {
						@Override
						public Double apply(final Entry<String, RandomVariable> entry) {
							Double derivativeValue = 0.0;
							final RandomVariable parameter = entry.getValue();
							if(parameter instanceof RandomVariableDifferentiable) {
								final RandomVariable derivativeRV = gradient.get(((RandomVariableDifferentiable)parameter).getID());
								if(derivativeRV != null) {
									derivativeValue = derivativeRV.getAverage();
								}
							}
							return derivativeValue;
						}
					}
					));
		}

		// Free memory
		//		liborMarketModel = null;
		gradientMap = null;

		/*
		 * Test valuation
		 */

		LIBORModelMonteCarloSimulationModel liborMarketModelPlain = createLIBORMarketModel(new RandomVariableFromArrayFactory(), new RandomVariableFromArrayFactory(),  numberOfPaths, numberOfFactors, 0.0 /* Correlation */,
				Optional.empty(), 0.0, 0, 0, 0);

		final long timingCalculation2Start = System.currentTimeMillis();

		final double valueSimulation2 = product.getValue(liborMarketModelPlain);

		final long timingCalculation2End = System.currentTimeMillis();

		// Free memory
		liborMarketModelPlain = null;

		final long memoryEnd2 = getAllocatedMemory();

		/*
		 * Calculate delta - in case we calculate parallel delta.
		 */

		/*
		 * Print status
		 */
		if(isVerbose) {
			System.out.println(product.getClass().getSimpleName() + ": " + productName);
			System.out.println("_______________________________________________________________________");

			modelDeltas.forEach(
					new BiConsumer<String, Double>() {
						@Override
						public void accept(final String key, final Double delta) { System.out.println(key + "\t" + delta); }
					});

			System.out.println("value...........................: " + formatterValue.format(valueSimulation));
			System.out.println("value (plain)...................: " + formatterValue.format(valueSimulation2));
			System.out.println("evaluation (plain)..............: " + formatReal1.format((timingCalculation2End-timingCalculation2Start)/1000.0) + " s");
			System.out.println("evaluation (AAD)................: " + formatReal1.format((timingCalculationEnd-timingCalculationStart)/1000.0) + " s");
			System.out.println("derivative (AAD).(all buckets)..: " + formatReal1.format((timingGradientEnd-timingGradientStart)/1000.0) + " s");
			System.out.println("number of Deltas (theoretical)...: " + numberOfDeltasTheoretical);
			System.out.println("number of Deltas (effective).....: " + numberOfDeltasEffective);
			System.out.println("memory (AAD)....................: " + formatReal1.format((memoryEnd-memoryStart)/1024.0/1024.0) + " M");
			System.out.println("memory (check)-.................: " + formatReal1.format((memoryEnd2-memoryStart)/1024.0/1024.0) + " M");
			System.out.println("\n");

			/*
			 * Analytic assert only for some products
			 */
			if(productName.equals("Caplet maturity 5.0")) {
				final String riskFactorName = "FORWARD(5.0,5.5)";
				final double sensitivityAAD = modelDeltas.get(riskFactorName);

				final double forward = liborMarketModel.getForwardRate(0.0, 5.0, 5.5).getAverage();

				final double optionMaturity = 5.0;
				final double optionStrike = forward;

				final LIBORMarketModelFromCovarianceModel modelLMMFromCov = ((LIBORMarketModelFromCovarianceModel)liborMarketModel.getModel());

				final double[][][] integratedLIBORCovariance = modelLMMFromCov.getIntegratedLIBORCovariance(liborMarketModel.getTimeDiscretization());
				final TimeDiscretization timeDiscretizationCovariance = modelLMMFromCov.getCovarianceModel().getTimeDiscretization();
				final double integratedVariance = integratedLIBORCovariance[modelLMMFromCov.getCovarianceModel().getTimeDiscretization().getTimeIndexNearestLessOrEqual(5.0)][liborMarketModel.getLiborPeriodDiscretization().getTimeIndex(5.0)][liborMarketModel.getLiborPeriodDiscretization().getTimeIndex(5.0)];

				final double volatility = Math.sqrt(integratedVariance/optionMaturity);
				final double periodLength = 0.5;
				final double discountFactor = liborMarketModel.getNumeraire(optionMaturity+periodLength).invert().mult(liborMarketModel.getNumeraire(0.0)).getAverage();

				final double payoffUnit = discountFactor * periodLength;

				final double sensitivityAnl = AnalyticFormulas.bachelierOptionDelta(forward, volatility, optionMaturity, optionStrike, payoffUnit) * payoffUnit;

				System.out.println("derivative (AAD)      " + riskFactorName + "...: " + formatterValue.format(sensitivityAAD));
				System.out.println("derivative (analytic) " + riskFactorName + "...: " + formatterValue.format(sensitivityAnl));

				Assert.assertEquals("Sensitivity " + riskFactorName, sensitivityAnl, sensitivityAAD, 5E-3 /* delta */);
			}
			else if(productName.equals("Caplet maturity 10.0")) {
				final String riskFactorName = "FORWARD(10.0,10.5)";
				final double sensitivityAAD = modelDeltas.get(riskFactorName);

				final double forward = liborMarketModel.getForwardRate(0.0, 10.0, 10.5).getAverage();

				final double optionMaturity = 10.0;
				final double optionStrike = forward;

				final double integratedVariance = ((LIBORMarketModelFromCovarianceModel)liborMarketModel.getModel()).getIntegratedLIBORCovariance(liborMarketModel.getTimeDiscretization())[liborMarketModel.getTimeDiscretization().getTimeIndex(5.0)][liborMarketModel.getLiborPeriodDiscretization().getTimeIndex(5.0)][liborMarketModel.getLiborPeriodDiscretization().getTimeIndex(5.0)];
				final double volatility = Math.sqrt(integratedVariance/optionMaturity);
				final double periodLength = 0.5;
				final double discountFactor = liborMarketModel.getNumeraire(optionMaturity+periodLength).invert().mult(liborMarketModel.getNumeraire(0.0)).getAverage();

				final double payoffUnit = discountFactor * periodLength;

				final double sensitivityAnl = AnalyticFormulas.bachelierOptionDelta(forward, volatility, optionMaturity, optionStrike, payoffUnit) * payoffUnit;

				System.out.println("derivative (AAD)      " + riskFactorName + "...: " + formatterValue.format(sensitivityAAD));
				System.out.println("derivative (analytic) " + riskFactorName + "...: " + formatterValue.format(sensitivityAnl));

				Assert.assertEquals("Sensitivity " + riskFactorName, sensitivityAnl, sensitivityAAD, 5E-3 /* delta */);
			}
			Assert.assertEquals("Valuation", valueSimulation2, valueSimulation, 0.0 /* delta */);
		}
		else {
			//			System.out.println(bucketDeltaLIBORIndex.orElse(-1) + "\t" + seed + "\t" + value.getAverage() + "\t" + bucketDelta + "\t" + deltaAAD);
		}
	}

	static long getAllocatedMemory() {
		System.gc();
		System.gc();
		System.gc();
		final long allocatedMemory = (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());
		return allocatedMemory;
	}
}
