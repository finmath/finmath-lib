package net.finmath.modelling.productfactory;

import java.time.LocalDate;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.functions.AnalyticFormulas;
import net.finmath.marketdata.model.curves.Curve.ExtrapolationMethod;
import net.finmath.marketdata.model.curves.Curve.InterpolationEntity;
import net.finmath.marketdata.model.curves.Curve.InterpolationMethod;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.DiscountCurveInterface;
import net.finmath.modelling.DescribedModel;
import net.finmath.modelling.Product;
import net.finmath.modelling.ProductDescriptor;
import net.finmath.modelling.descriptor.AssetModelDescriptor;
import net.finmath.modelling.descriptor.BlackScholesModelDescriptor;
import net.finmath.modelling.descriptor.HestonModelDescriptor;
import net.finmath.modelling.descriptor.SingleAssetDigitalOptionProductDescriptor;
import net.finmath.modelling.modelfactory.AssetModelFourierMethodFactory;
import net.finmath.modelling.modelfactory.BlackScholesModelMonteCarloFactory;
import net.finmath.modelling.modelfactory.HestonModelMonteCarloFactory;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.BrownianMotionLazyInit;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.time.FloatingpointDate;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

public class ModelWithProductFactoryTest {

	// Model properties
	private static final LocalDate referenceDate = LocalDate.of(2017,8,15);

	private static final double initialValue   = 1.0;
	private static final double riskFreeRate   = 0.05;
	private static final double volatility     = 0.30;

	private static final double theta = volatility*volatility;
	private static final double kappa = 0.1;
	private static final double xi = 0.50;
	private static final double rho = 0.1;

	// Product properties
	private static final double maturity 			= 1.0;
	private static final LocalDate maturityDate		= FloatingpointDate.getDateFromFloatingPointDate(referenceDate, maturity);
	private static final double strike				= 0.95;

	// Monte Carlo simulation  properties
	private final int		numberOfPaths		= 1000000;
	private final int		numberOfTimeSteps	= 10;
	private final double 	deltaT				= 0.5;
	private final int		seed				= 31415;


	@Test
	public void bsTest() {
		/*
		 * Create Black-Scholes Model descriptor
		 */
		BlackScholesModelDescriptor blackScholesModelDescriptor = new BlackScholesModelDescriptor(referenceDate, initialValue, getDiscountCurve("forward curve", referenceDate, riskFreeRate), getDiscountCurve("discount curve", referenceDate, riskFreeRate), volatility);

		/*
		 * Create European option descriptor
		 */
		String underlyingName = "eurostoxx";
		ProductDescriptor digitalOptionDescriptor = (new SingleAssetDigitalOptionProductDescriptor(underlyingName, maturityDate, strike));

		/*
		 * Create Fourier implementation of model and product
		 */

		// Create base Fourier implementation of Black-Scholes model
		DescribedModel<? extends AssetModelDescriptor> blackScholesModelFourier = (new AssetModelFourierMethodFactory()).getModelFromDescriptor(blackScholesModelDescriptor);

		//		// Add custom product factory
		//		ProductFactory<SingleAssetDigitalOptionProductDescriptor> fourierProductFactory = new ProductFactory<SingleAssetDigitalOptionProductDescriptor>() {
		//
		//			@Override
		//			public DescribedProduct<? extends SingleAssetDigitalOptionProductDescriptor> getProductFromDescriptor(ProductDescriptor descriptor) {
		//				if(descriptor instanceof SingleAssetDigitalOptionProductDescriptor) {
		//					final DescribedProduct<SingleAssetDigitalOptionProductDescriptor> product = new Digi((SingleAssetDigitalOptionProductDescriptor) descriptor);
		//					return product;
		//				}
		//				else {
		//					String name = descriptor.name();
		//					throw new IllegalArgumentException("Unsupported product type " + name);
		//				}
		//			}
		//		};

		// Create product implementation compatible with Black-Scholes model
		Product digitalOptionFourier = blackScholesModelFourier.getProductFromDescriptor(digitalOptionDescriptor);

		// Evaluate product
		double evaluationTime = 0.0;
		Map<String, Object> valueFourier = digitalOptionFourier.getValues(evaluationTime, blackScholesModelFourier);

		System.out.println("Fourier transform implementation..:" + valueFourier);

		/*
		 * Create Monte Carlo implementation of model and product
		 */

		// Create a time discretization
		BrownianMotion brownianMotion = getBronianMotion(numberOfTimeSteps, deltaT, 2 /* numberOfFactors */, numberOfPaths, seed);
		RandomVariableFactory randomVariableFactory = new RandomVariableFactory();

		// Create Fourier implementation of Black-Scholes model
		DescribedModel<?> blackScholesModelMonteCarlo = (new BlackScholesModelMonteCarloFactory(randomVariableFactory, brownianMotion)).getModelFromDescriptor(blackScholesModelDescriptor);

		// Create product implementation compatible with Black-Scholes model
		Product digitalOptionMonteCarlo = blackScholesModelMonteCarlo.getProductFromDescriptor(digitalOptionDescriptor);

		Map<String, Object> valueMonteCarlo = digitalOptionMonteCarlo.getValues(evaluationTime, blackScholesModelMonteCarlo);

		System.out.println("Monte-Carlo implementation........:" + valueMonteCarlo);

		/*
		 * Create Finite Difference implementation of model and product
		 */

		//		// Create finite difference implementation of Black-Scholes model
		//		DescribedModel<?> blackScholesModelFiniteDifference = (new BlackScholesModelMonteCarloFiniteDifference1D(0.5 /* theta */)).getModelFromDescriptor(blackScholesModelDescriptor);
		//
		//		// Create product implementation compatible with Black-Scholes model
		//		Product europeanOptionFiniteDifference = blackScholesModelFiniteDifference.getProductFromDescriptor(digitalOptionDescriptor);
		//
		//		Map<String, Object> valueFiniteDifference = europeanOptionFiniteDifference.getValues(evaluationTime, blackScholesModelFiniteDifference);
		//
		//		System.out.println("Finite difference implementation..:" + valueFiniteDifference);

		/*
		 * Calculate analytic benchmark.
		 */

		double optionMaturity = FloatingpointDate.getFloatingPointDateFromDate(referenceDate, ((SingleAssetDigitalOptionProductDescriptor) digitalOptionDescriptor).getMaturity());
		//		double forward = blackScholesModelDescriptor.getInitialValue() / blackScholesModelDescriptor.getDiscountCurveForForwardRate().getDiscountFactor(optionMaturity);
		//		double payOffUnit = blackScholesModelDescriptor.getDiscountCurveForDiscountRate().getDiscountFactor(optionMaturity);
		double volatility = blackScholesModelDescriptor.getVolatility();
		double optionStrike = ((SingleAssetDigitalOptionProductDescriptor) digitalOptionDescriptor).getStrike();

		double valueAnalytic = AnalyticFormulas.blackScholesDigitalOptionValue(initialValue, riskFreeRate, volatility, optionMaturity, optionStrike);

		System.out.println("Analytic implementation...........:" + valueAnalytic);

		/*
		 * Assertions
		 */

		Assert.assertEquals("Deviation of Fourier method from analytic", valueAnalytic, ((Double)valueFourier.get("value")).doubleValue(), 1E-3);
		Assert.assertEquals("Deviation of Monte-Carlo method from analytic", valueAnalytic, ((Double)valueMonteCarlo.get("value")).doubleValue(), 1E-3);
		//		Assert.assertEquals("Deviation of finite difference method from analytic", valueAnalytic, ((Double)valueFiniteDifference.get("value")).doubleValue(), 1E-3);
	}


	@Test
	public void hTest() {
		/*
		 * Create Heston Model descriptor
		 */
		HestonModelDescriptor hestonModelDescriptor = new HestonModelDescriptor(referenceDate, initialValue, getDiscountCurve("forward curve", referenceDate, riskFreeRate), getDiscountCurve("discount curve", referenceDate, riskFreeRate), volatility, theta, kappa, xi, rho);

		/*
		 * Create Digital option descriptor
		 */
		String underlyingName = "eurostoxx";
		ProductDescriptor digitalOptionDescriptor = (new SingleAssetDigitalOptionProductDescriptor(underlyingName, maturityDate, strike));

		/*
		 * Create Fourier implementation of model and product
		 */

		// Create base Fourier implementation of Heston model
		DescribedModel<? extends AssetModelDescriptor> hestonModelFourier = (new AssetModelFourierMethodFactory()).getModelFromDescriptor(hestonModelDescriptor);

		//		// Create custom product factory
		//		ProductFactory<SingleAssetDigitalOptionProductDescriptor> fourierProductFactory = new ProductFactory<SingleAssetDigitalOptionProductDescriptor>() {
		//			@Override
		//			public DescribedProduct<? extends SingleAssetDigitalOptionProductDescriptor> getProductFromDescriptor(ProductDescriptor descriptor) {
		//				if(descriptor instanceof SingleAssetDigitalOptionProductDescriptor) {
		//					final DescribedProduct<SingleAssetDigitalOptionProductDescriptor> product = new net.finmath.fouriermethod.products.DigitalOption((SingleAssetDigitalOptionProductDescriptor) descriptor);
		//					return product;
		//				}
		//				else {
		//					String name = descriptor.name();
		//					throw new IllegalArgumentException("Unsupported product type " + name);
		//				}
		//			}
		//		};

		// Create product implementation compatible with Heston model
		Product digitalOptionFourier = hestonModelFourier.getProductFromDescriptor(digitalOptionDescriptor);

		// Evaluate product
		double evaluationTime = 0.0;
		Map<String, Object> valueFourier = digitalOptionFourier.getValues(evaluationTime, hestonModelFourier);

		System.out.println(valueFourier);

		/*
		 * Create Monte Carlo implementation of model and product
		 */

		// Create a time discretization
		BrownianMotion brownianMotion = getBronianMotion(numberOfTimeSteps, deltaT, 2 /* numberOfFactors */, numberOfPaths, seed);
		RandomVariableFactory randomVariableFactory = new RandomVariableFactory();

		// Create Fourier implementation of Heston model
		DescribedModel<?> hestonModelMonteCarlo = (new HestonModelMonteCarloFactory(net.finmath.montecarlo.assetderivativevaluation.models.HestonModel.Scheme.FULL_TRUNCATION, randomVariableFactory, brownianMotion)).getModelFromDescriptor(hestonModelDescriptor);

		// Create product implementation compatible with Heston model
		Product digitalOptionMonteCarlo = hestonModelMonteCarlo.getProductFromDescriptor(digitalOptionDescriptor);

		Map<String, Object> valueMonteCarlo = digitalOptionMonteCarlo.getValues(evaluationTime, hestonModelMonteCarlo);

		System.out.println(valueMonteCarlo);

		double deviation = (Double)valueMonteCarlo.get("value") - (Double)valueFourier.get("value");
		Assert.assertEquals("Difference of Fourier and Monte-Carlo valuation", 0.0, deviation, 5E-2);
	}

	/**
	 * Get the discount curve using the riskFreeRate.
	 *
	 * @param name Name of the curve
	 * @param referenceDate Date corresponding to t=0.
	 * @param zeroRate Constant continuously compounded rate (using library internal daycount convention).
	 *
	 * @return the discount curve using the riskFreeRate.
	 */
	public static DiscountCurveInterface getDiscountCurve(String name, LocalDate referenceDate, double zeroRate) {
		double[] times = new double[] { 1.0 };
		double[] givenAnnualizedZeroRates = new double[] { zeroRate };
		InterpolationMethod interpolationMethod = InterpolationMethod.LINEAR;
		InterpolationEntity interpolationEntity = InterpolationEntity.LOG_OF_VALUE_PER_TIME;
		ExtrapolationMethod extrapolationMethod = ExtrapolationMethod.CONSTANT;
		DiscountCurveInterface discountCurve = DiscountCurve.createDiscountCurveFromAnnualizedZeroRates(name, referenceDate, times, givenAnnualizedZeroRates, interpolationMethod, extrapolationMethod, interpolationEntity);
		return discountCurve;
	}

	/**
	 * Create a Brownian motion implementing BrownianMotion from given specs.
	 *
	 * @param numberOfTimeSteps The number of time steps.
	 * @param deltaT The time step size.
	 * @param numberOfFactors The number of factors.
	 * @param numberOfPaths The number of paths.
	 * @param seed The seed for the random number generator.
	 * @return A Brownian motion implementing BrownianMotion with the given specs.
	 */
	private static BrownianMotion getBronianMotion(int numberOfTimeSteps, double deltaT, int numberOfFactors, int numberOfPaths, int seed) {
		TimeDiscretization timeDiscretization = new TimeDiscretizationFromArray(0.0 /* initial */, numberOfTimeSteps, deltaT);
		BrownianMotion brownianMotion = new BrownianMotionLazyInit(timeDiscretization, numberOfFactors, numberOfPaths, seed);
		return brownianMotion;
	}
}
