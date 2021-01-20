package net.finmath.montecarlo.hybridassetinterestrate;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.curves.ForwardCurveInterpolation;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.BrownianMotionFromMersenneRandomNumbers;
import net.finmath.montecarlo.BrownianMotionView;
import net.finmath.montecarlo.IndependentIncrements;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFromArrayFactory;
import net.finmath.montecarlo.assetderivativevaluation.models.BlackScholesModel;
import net.finmath.montecarlo.hybridassetinterestrate.products.Bond;
import net.finmath.montecarlo.hybridassetinterestrate.products.BondWithForeignNumeraire;
import net.finmath.montecarlo.hybridassetinterestrate.products.ForwardRateAgreementGeneralized;
import net.finmath.montecarlo.interestrate.CalibrationProduct;
import net.finmath.montecarlo.interestrate.LIBORMarketModel;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.LIBORMonteCarloSimulationFromLIBORModel;
import net.finmath.montecarlo.interestrate.models.LIBORMarketModelFromCovarianceModel;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCorrelationModelExponentialDecay;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCovarianceModelCalibrateable;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCovarianceModelFromVolatilityAndCorrelation;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORVolatilityModel;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORVolatilityModelFourParameterExponentialForm;
import net.finmath.montecarlo.model.ProcessModel;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.montecarlo.process.MonteCarloProcess;
import net.finmath.montecarlo.process.MonteCarloProcessFromProcessModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

public class CrossCurrencyLIBORMarketModelFromModelsTest {

	private static final int numberOfPaths = 200000;
	private static final RandomVariableFactory randomVariableFactory = new RandomVariableFromArrayFactory();

	@Test
	public void testProperties() throws CalculationException {
		final CrossCurrencyLIBORMarketModelFromModels model = getModel(new String[] { "EUR", "USD" });

		final RandomVariable rate = model.getValue(new RiskFactorForwardRate("USD", 0.5, 1.0), 0.5);
		final RandomVariable fx = model.getValue(new RiskFactorFX("USD"), 1.0);
		final RandomVariable numeraire = model.getNumeraire(1.0);

		final double foreignRateDiscountedExpected = rate.mult(fx).div(numeraire).getAverage();
		final double foreignBond = fx.div(numeraire).getAverage();
		final double foreignForward = foreignRateDiscountedExpected/foreignBond;
		final double foreignForwardAnalytic = model.getInterestRateModel("USD").getModel().getForwardRateCurve().getForward(null, 0.5);

		System.out.println("(1) = E(ForeignRate * FX / N)....: " + foreignRateDiscountedExpected);
		System.out.println("(2) = E(    1       * FX / N)....: " + foreignBond);
		System.out.println("Foreign Forward Rate (1)/(2).....: " + foreignForward);
		System.out.println("Analytic Foreign Forward.........: " + foreignForwardAnalytic);

		System.out.println("_______________________________________________________________________________"+"\n");

		Assert.assertEquals("Foreign Forward", foreignForwardAnalytic, foreignForward, 1E-2);
	}

	@Test
	public void testForeignBond() throws CalculationException {
		final CrossCurrencyLIBORMarketModelFromModels model = getModel(new String[] { "EUR", "USD" });

		final BondWithForeignNumeraire bondFor = new BondWithForeignNumeraire("USD", 10.0);
		final Bond boncCcyFor = new Bond("USD", 10.0);
		final Bond bondCcyDom = new Bond("EUR", 10.0);

		final double valueCcyFor = boncCcyFor.getValue(model);
		final double valueCcyDom = bondCcyDom.getValue(model);

		/*
		 * Create foreign model
		 */
		final CrossCurrencyLIBORMarketModelFromModels foreignModel = getModel(new String[] { "USD" });
		final double valueForeign = bondFor.getValue(foreignModel);

		System.out.println("Forein Bond valued in CCY model......: " + valueCcyFor);
		System.out.println("Forein Bond valued in foreign model..: " + valueForeign);
		System.out.println("Domestic Bond in Domestic Currency...: " + valueCcyDom);

		System.out.println("_______________________________________________________________________________"+"\n");

		Assert.assertEquals("Foreign Bond", valueCcyFor, valueForeign, 1E-2);
	}

	@Test
	public void testForeignFRA() throws CalculationException {
		final CrossCurrencyLIBORMarketModelFromModels model = getModel(new String[] { "EUR", "USD" });

		final ForwardRateAgreementGeneralized fra = new ForwardRateAgreementGeneralized("USD", 10.0, 10.0, 10.5);

		final double fraCcy = fra.getValue(model);

		/*
		 * Create foreign model
		 */
		final CrossCurrencyLIBORMarketModelFromModels foreignModel = getModel(new String[] { "USD" });

		final double fraFor = fra.getValue(foreignModel);

		System.out.println("Foreign FRA in ccy model......: " + fraCcy);
		System.out.println("Foreign FRA in foreign model..: " + fraFor);

		System.out.println("_______________________________________________________________________________"+"\n");

		Assert.assertEquals("Foreign FRA", fraFor, fraCcy, 1E-2);
	}

	@Test
	public void testForeignCaplet() throws CalculationException {
		final CrossCurrencyLIBORMarketModelFromModels model = getModel(new String[] { "EUR", "USD" });

		/*
		 * Calculate ATM numerically
		 */
		final ForwardRateAgreementGeneralized fra = new ForwardRateAgreementGeneralized("USD", 10.0, 10.0, 10.5);
		final double fraCcy = fra.getValue(model);

		final Bond bond = new Bond("USD", 10.5);
		final double boncCcy = bond.getValue(model);

		final double forwardATM = fraCcy / boncCcy;

		System.out.println("ATM forward...: " + forwardATM);

		final ForwardRateAgreementGeneralized caplet = new ForwardRateAgreementGeneralized(
				null,
				"USD", 10.0, 10.0, 10.5,
				new Scalar(-forwardATM), new Scalar(0.0), null);

		final double capletCcy = caplet.getValue(model);

		/*
		 * Create foreign model
		 */
		final CrossCurrencyLIBORMarketModelFromModels foreignModel = getModel(new String[] { "USD" });

		final double capletFor = caplet.getValue(foreignModel);

		System.out.println("Foreign Caplet in ccy model......: " + capletCcy);
		System.out.println("Foreign Caplet in foreign model..: " + capletFor);

		System.out.println("_______________________________________________________________________________"+"\n");

		Assert.assertEquals("Foreign FRA", capletFor, capletCcy, 1E-2);
	}

	private CrossCurrencyLIBORMarketModelFromModels getModel(String[] currency) throws CalculationException {
		/*
		 * Create domestic rate model
		 */

		/*
		 * Create a simulation time discretization
		 */
		final double lastTime	= 20.0;
		final double dt		= 0.5;

		final TimeDiscretization timeDiscretization = new TimeDiscretizationFromArray(0.0, (int) (lastTime / dt), dt);

		/*
		 * The configuration of Brownian drivers and convexity adjustments could be a bit more convenient.
		 */
		final BrownianMotion brownianMotion = new BrownianMotionFromMersenneRandomNumbers(timeDiscretization, 3 /* numberOfFactors */, numberOfPaths, 323, randomVariableFactory);
		final BrownianMotion brownianMotionDom = new BrownianMotionView(brownianMotion, new Integer[] { 0 });
		final BrownianMotion brownianMotionFor = new BrownianMotionView(brownianMotion, new Integer[] { 1 });
		final BrownianMotion brownianMotionFXM = new BrownianMotionView(brownianMotion, new Integer[] { 1 });

		final double initialValue = 1.0;
		final MonteCarloProcessFromProcessModel fxMartingaleModel = getFxMartingaleModel(initialValue, brownianMotionFXM);

		/*
		 * This map specifies that there is no convexity adjustment in the EUR model
		 */
		final Map<Integer,Integer> factorLoadingMapEUR = new HashMap();

		/*
		 * This map specifies that USD model and FX model have a common Brownian driver dW0 <-> dW0.
		 * Hence, it will trigger a convexity adjustment.
		 */
		final Map<Integer,Integer> factorLoadingMapUSD = new HashMap<Integer, Integer>();
		factorLoadingMapUSD.put(0, 0);

		if(currency.length == 2 && currency[0] == "EUR" && currency[1] == "USD") {
			final LIBORMonteCarloSimulationFromLIBORModel modelDomestic = getInterestRateModel(0.05, brownianMotionDom, fxMartingaleModel, factorLoadingMapEUR);
			final LIBORMonteCarloSimulationFromLIBORModel modelForeign = getInterestRateModel(0.10, brownianMotionFor, fxMartingaleModel, factorLoadingMapUSD);

			final Map<String, LIBORModelMonteCarloSimulationModel> interestRateModels = new HashMap<String, LIBORModelMonteCarloSimulationModel>();
			interestRateModels.put("EUR", modelDomestic	/* EUR model */);
			interestRateModels.put("USD", modelForeign	/* EUR model */);

			final Map<String, MonteCarloProcessFromProcessModel> fxModels = new HashMap<String, MonteCarloProcessFromProcessModel>();
			fxModels.put("USD", fxMartingaleModel /* Conversion from USD to base (EUR) */);

			return new CrossCurrencyLIBORMarketModelFromModels(
					"EUR"	/* base model */,
					interestRateModels, fxModels);
		}
		else if(currency.length == 1 && currency[0] == "EUR") {
			final LIBORMonteCarloSimulationFromLIBORModel modelDomestic = getInterestRateModel(0.05, brownianMotionDom, fxMartingaleModel, factorLoadingMapEUR);

			final Map<String, LIBORModelMonteCarloSimulationModel> interestRateModels = new HashMap<String, LIBORModelMonteCarloSimulationModel>();
			interestRateModels.put("EUR", modelDomestic	/* EUR model */);

			final Map<String, MonteCarloProcessFromProcessModel> fxModels = new HashMap<String, MonteCarloProcessFromProcessModel>();

			return new CrossCurrencyLIBORMarketModelFromModels(
					"EUR"	/* base model */,
					interestRateModels, fxModels);
		}
		else if(currency.length == 1 && currency[0] == "USD") {
			final LIBORMonteCarloSimulationFromLIBORModel modelForeign = getInterestRateModel(0.10, brownianMotionFor, fxMartingaleModel, factorLoadingMapEUR);

			final Map<String, LIBORModelMonteCarloSimulationModel> interestRateModels = new HashMap<String, LIBORModelMonteCarloSimulationModel>();
			interestRateModels.put("USD", modelForeign	/* EUR model */);

			final Map<String, MonteCarloProcessFromProcessModel> fxModels = new HashMap<String, MonteCarloProcessFromProcessModel>();

			return new CrossCurrencyLIBORMarketModelFromModels(
					"USD"	/* base model */,
					interestRateModels, fxModels);

		}
		else {
			throw new IllegalArgumentException("Unsupported currency configuration.");
		}
	}

	private MonteCarloProcessFromProcessModel getFxMartingaleModel(double initialValue, IndependentIncrements brownianMotion) {
		final double riskFreeRate = 0.00;
		final double volatility = 0.30;
		// Create the model
		final ProcessModel blackScholesModel = new BlackScholesModel(initialValue, riskFreeRate, volatility);

		// Create a corresponding MC process
		final MonteCarloProcessFromProcessModel process = new EulerSchemeFromProcessModel(blackScholesModel, brownianMotion);

		return process;
	}

	private LIBORMonteCarloSimulationFromLIBORModel getInterestRateModel(double flatForwardRate, IndependentIncrements brownianMotion, MonteCarloProcess measureTransformModel, Map<Integer, Integer> factorLoadingMap) throws CalculationException {
		final RandomVariableFactory randomVariableFactory = new RandomVariableFromArrayFactory();

		final TimeDiscretization timeDiscretization =  brownianMotion.getTimeDiscretization();
		final int numberOfFactors = brownianMotion.getNumberOfFactors();
		final double correlationDecayParam = 0.01;
		/*
		 * Create the libor tenor structure and the initial values
		 */
		final double liborPeriodLength	= 0.5;
		final double liborRateTimeHorzion	= 20.0;
		final TimeDiscretization liborPeriodDiscretization = new TimeDiscretizationFromArray(0.0, (int) (liborRateTimeHorzion / liborPeriodLength), liborPeriodLength);

		// Create the forward curve (initial value of the LIBOR market model)
		final ForwardCurveInterpolation forwardCurveInterpolation = ForwardCurveInterpolation.createForwardCurveFromForwards(
				"forwardCurve"								/* name of the curve */,
				new double[] {0.5 , 1.0 , 2.0 , 5.0 , 40.0}	/* fixings of the forward */,
				new double[] {flatForwardRate, flatForwardRate, flatForwardRate, flatForwardRate, flatForwardRate}	/* forwards */,
				liborPeriodLength							/* tenor / period length */
				);

		/*
		 * Create a volatility structure v[i][j] = sigma_j(t_i)
		 */
		final double a = 0.2, b = 0.0, c = 0.25, d = 0.3;
		final LIBORVolatilityModel volatilityModel = new LIBORVolatilityModelFourParameterExponentialForm(timeDiscretization, liborPeriodDiscretization, a, b, c, d, false);

		/*
		 * Create a correlation model rho_{i,j} = exp(-a * abs(T_i-T_j))
		 */
		final LIBORCorrelationModelExponentialDecay correlationModel = new LIBORCorrelationModelExponentialDecay(
				timeDiscretization, liborPeriodDiscretization, numberOfFactors,
				correlationDecayParam);


		/*
		 * Combine volatility model and correlation model to a covariance model
		 */
		final LIBORCovarianceModelCalibrateable covarianceModel = new LIBORCovarianceModelFromVolatilityAndCorrelation(
				timeDiscretization, liborPeriodDiscretization, volatilityModel, correlationModel);

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
		 * Create corresponding LIBOR Market Model with Convexity Adustment
		 */
		final LIBORMarketModel liborMarketModel = new LIBORMarketModelFromCovarianceModel(liborPeriodDiscretization, null /* analyticModel */, forwardCurveInterpolation,
				null,//new DiscountCurveFromForwardCurve(forwardCurveInterpolation),
				randomVariableFactory, covarianceModel, calibrationItems, properties) {

			private static final long serialVersionUID = -8114905083663193000L;

			final ConvexityAdjustedModel convexityAdjustment = new ConvexityAdjustedModel(this, measureTransformModel, factorLoadingMap);

			@Override
			public RandomVariable[] getDrift(MonteCarloProcess process, int timeIndex, RandomVariable[] realizationAtTimeIndex, RandomVariable[] realizationPredictor) {
				final RandomVariable[] driftUnadjusted = super.getDrift(process, timeIndex, realizationAtTimeIndex, realizationPredictor);
				return convexityAdjustment.getDrift(driftUnadjusted, process, timeIndex, realizationAtTimeIndex, realizationPredictor);
			}
		};

		final EulerSchemeFromProcessModel process = new EulerSchemeFromProcessModel(liborMarketModel, brownianMotion, EulerSchemeFromProcessModel.Scheme.EULER);

		final LIBORMonteCarloSimulationFromLIBORModel model =  new LIBORMonteCarloSimulationFromLIBORModel(process);

		return model;
	}
}
