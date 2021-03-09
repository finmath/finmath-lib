package net.finmath.montecarlo.interestrate;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
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
import net.finmath.marketdata.model.curves.ForwardCurveInterpolation;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFromArrayFactory;
import net.finmath.montecarlo.interestrate.models.LIBORMarketModelFromCovarianceModel;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCorrelationModelExponentialDecay;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCovarianceModelFromVolatilityAndCorrelation;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORVolatilityModel;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORVolatilityModelFourParameterExponentialForm;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * This class tests interpolated LIBORs.
 */
@RunWith(Parameterized.class)
public class LIBORMarketModelInterpolationTest {

	@Parameters(name="{0}")
	public static Collection<Object[]> generateData()
	{
		return Arrays.asList(new Object[][] {
			{ LIBORMarketModelFromCovarianceModel.InterpolationMethod.LINEAR                 },
			{ LIBORMarketModelFromCovarianceModel.InterpolationMethod.LOG_LINEAR_UNCORRECTED },
			{ LIBORMarketModelFromCovarianceModel.InterpolationMethod.LOG_LINEAR_CORRECTED   },
		});
	}

	private final int numberOfPaths		= 500000;
	private final int numberOfFactors	= 1;

	private final LIBORModelMonteCarloSimulationModel liborMarketModel;

	private static DecimalFormat formatterMaturity	= new DecimalFormat("00.00", new DecimalFormatSymbols(Locale.ENGLISH));
	private static DecimalFormat formatterValue		= new DecimalFormat(" ##0.000%;-##0.000%", new DecimalFormatSymbols(Locale.ENGLISH));
	private static DecimalFormat formatterMoneyness	= new DecimalFormat(" 000.0%;-000.0%", new DecimalFormatSymbols(Locale.ENGLISH));
	private static DecimalFormat formatterDeviation	= new DecimalFormat(" 0.00000E00;-0.00000E00", new DecimalFormatSymbols(Locale.ENGLISH));



	public LIBORMarketModelInterpolationTest(final LIBORMarketModelFromCovarianceModel.InterpolationMethod interpolationMethod) throws CalculationException {

		// Create a libor market model
		System.out.println("Test for interpolationMethod " + interpolationMethod.name() + ":");
		final RandomVariableFromArrayFactory randomVariableFromArrayFactory =  new RandomVariableFromArrayFactory();
		liborMarketModel = createLIBORMarketModel(interpolationMethod, randomVariableFromArrayFactory, numberOfPaths, numberOfFactors, 0.1 /* Correlation */);
	}

	public static LIBORModelMonteCarloSimulationModel createLIBORMarketModel(
			final LIBORMarketModelFromCovarianceModel.InterpolationMethod interpolationMethod,
			final RandomVariableFactory randomVariableFactory, final int numberOfPaths, final int numberOfFactors, final double correlationDecayParam) throws CalculationException {

		/*
		 * Create the libor tenor structure and the initial values
		 */
		final double liborPeriodLength	= 2.0;
		final double liborRateTimeHorzion	= 20.0;
		final TimeDiscretizationFromArray liborPeriodDiscretization = new TimeDiscretizationFromArray(0.0, (int) (liborRateTimeHorzion / liborPeriodLength), liborPeriodLength);

		// Create the forward curve (initial value of the LIBOR market model)
		final ForwardCurveInterpolation forwardCurveInterpolation = ForwardCurveInterpolation.createForwardCurveFromForwards(
				"forwardCurve"								/* name of the curve */,
				new double[] {0.5 , 1.0 , 2.0 , 5.0 , 40.0}	/* fixings of the forward */,
				new double[] {0.05, 0.05, 0.05, 0.05, 0.05}	/* forwards */,
				liborPeriodLength							/* tenor / period length */
				);

		/*
		 * Create a simulation time discretization
		 */
		final double lastTime	= 20.0;
		final double dt		= 0.5;

		final TimeDiscretizationFromArray timeDiscretizationFromArray = new TimeDiscretizationFromArray(0.0, (int) (lastTime / dt), dt);

		/*
		 * Create a volatility structure v[i][j] = sigma_j(t_i)
		 */
		final double a = 0.2, b = 0.0, c = 0.25, d = 0.3;
		final LIBORVolatilityModel volatilityModel = new LIBORVolatilityModelFourParameterExponentialForm(timeDiscretizationFromArray, liborPeriodDiscretization, a, b, c, d, false);

		/*
		 * Create a correlation model rho_{i,j} = exp(-a * abs(T_i-T_j))
		 */
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
		properties.put("measure", LIBORMarketModelFromCovarianceModel.Measure.TERMINAL.name());

		// Choose log normal model
		properties.put("stateSpace", LIBORMarketModelFromCovarianceModel.StateSpace.LOGNORMAL.name());

		properties.put("interpolationMethod" , interpolationMethod.name());

		// Empty array of calibration items - hence, model will use given covariance
		final CalibrationProduct[] calibrationItems = new CalibrationProduct[0];

		/*
		 * Create corresponding LIBOR Market Model
		 */
		final LIBORMarketModel liborMarketModel = new LIBORMarketModelFromCovarianceModel(liborPeriodDiscretization, null /* analyticModel */, forwardCurveInterpolation, new DiscountCurveFromForwardCurve(forwardCurveInterpolation), randomVariableFactory, covarianceModel, calibrationItems, properties);

		final BrownianMotion brownianMotion = new net.finmath.montecarlo.BrownianMotionLazyInit(timeDiscretizationFromArray, numberOfFactors, numberOfPaths, 3141 /* seed */);

		final EulerSchemeFromProcessModel process = new EulerSchemeFromProcessModel(liborMarketModel, brownianMotion, EulerSchemeFromProcessModel.Scheme.PREDICTOR_CORRECTOR);

		return new LIBORMonteCarloSimulationFromLIBORModel(liborMarketModel, process);
	}

	@Test
	public void testInterpolatedLastLIBOR() throws CalculationException
	{
		final RandomVariable longLastLIBOR       = liborMarketModel.getForwardRate(18.0, 18.0, 20.0);
		//RandomVariable longLastLIBORAtZero = liborMarketModel.getLIBOR(0.0, 18.0, 20.0);
		final double longLastForwardAtZero  =liborMarketModel.getModel().getForwardRateCurve().getForward(
				liborMarketModel.getModel().getAnalyticModel(), 18.0, 2.0);

		final RandomVariable interpolatedLastLIBOR = liborMarketModel.getForwardRate(18.0, 19.0, 20.0);
		//RandomVariable interpolatedLastLIBORAtZero = liborMarketModel.getLIBOR(0.0, 19.0, 20.0);
		final double interpolatedLastForwardAtZero  =liborMarketModel.getModel().getForwardRateCurve().getForward(
				liborMarketModel.getModel().getAnalyticModel(), 19.0, 1.0);

		System.out.println("1+L(18,20;0)Dt    \t" + longLastForwardAtZero);
		System.out.println("E[1+L(18,20;18)Dt]\t" + longLastLIBOR.getAverage());
		System.out.println();
		System.out.println("1+L(19,20;0)Dt    \t" + interpolatedLastForwardAtZero);
		System.out.println("E[1+L(19,20;18)Dt]\t" + interpolatedLastLIBOR.getAverage());
		System.out.println("---------------------------------------");
		System.out.println();

		Assert.assertEquals(null, longLastForwardAtZero, longLastLIBOR.getAverage(), 0.005);
		Assert.assertEquals(null, interpolatedLastForwardAtZero, interpolatedLastLIBOR.getAverage(), 0.005);
	}
}
