/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 17.02.2018
 */

package net.finmath.modelling.descriptor;

import java.time.LocalDate;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.functions.AnalyticFormulas;
import net.finmath.marketdata.model.curves.CurveInterpolation.ExtrapolationMethod;
import net.finmath.marketdata.model.curves.CurveInterpolation.InterpolationEntity;
import net.finmath.marketdata.model.curves.CurveInterpolation.InterpolationMethod;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.DiscountCurveInterpolation;
import net.finmath.modelling.DescribedModel;
import net.finmath.modelling.Product;
import net.finmath.modelling.ProductDescriptor;
import net.finmath.modelling.modelfactory.AssetModelFourierMethodFactory;
import net.finmath.modelling.modelfactory.AssetModelMonteCarloFactory;
import net.finmath.modelling.modelfactory.BlackScholesModelMonteCarloFiniteDifference1D;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.BrownianMotionFromMersenneRandomNumbers;
import net.finmath.montecarlo.RandomVariableFromArrayFactory;
import net.finmath.time.FloatingpointDate;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * Unit test creating a Black-Scholes model and a European option from corresponding model descriptors and product descriptors
 * using two different factories: Fourier versus Monte-Carlo.
 *
 * @author Christian Fries
 */
public class AssetBlackScholesModelDescriptorTest {

	// Model properties
	private static final LocalDate referenceDate = LocalDate.of(2017,8,15);

	private static final double initialValue   = 1.0;
	private static final double riskFreeRate   = 0.05;
	private static final double volatility     = 0.30;

	// Product properties
	private static final double maturity			= 1.0;
	private static final LocalDate maturityDate		= FloatingpointDate.getDateFromFloatingPointDate(referenceDate, maturity);
	private static final double strike				= 0.95;

	// Monte Carlo simulation  properties
	private final int		numberOfPaths		= 1000000;
	private final int		numberOfTimeSteps	= 10;
	private final double 	deltaT				= 0.5;
	private final int		seed				= 31415;

	@Test
	public void test() {
		/*
		 * Create Black-Scholes Model descriptor
		 */
		final BlackScholesModelDescriptor blackScholesModelDescriptor = new BlackScholesModelDescriptor(referenceDate, initialValue, getDiscountCurve("forward curve", referenceDate, riskFreeRate), getDiscountCurve("discount curve", referenceDate, riskFreeRate), volatility);

		/*
		 * Create European option descriptor
		 */
		final String underlyingName = "eurostoxx";
		final ProductDescriptor europeanOptionDescriptor = (new SingleAssetEuropeanOptionProductDescriptor(underlyingName, maturityDate, strike));

		/*
		 * Create Fourier implementation of model and product
		 */

		// Create Fourier implementation of Black-Scholes model
		final DescribedModel<?> blackScholesModelFourier = (new AssetModelFourierMethodFactory()).getModelFromDescriptor(blackScholesModelDescriptor);

		// Create product implementation compatible with Black-Scholes model
		final Product europeanOptionFourier = blackScholesModelFourier.getProductFromDescriptor(europeanOptionDescriptor);

		// Evaluate product
		final double evaluationTime = 0.0;
		final Map<String, Object> valueFourier = europeanOptionFourier.getValues(evaluationTime, blackScholesModelFourier);

		System.out.println("Fourier transform implementation..:" + valueFourier);

		/*
		 * Create Monte Carlo implementation of model and product
		 */

		// Create a time discretization
		final BrownianMotion brownianMotion = getBronianMotion(numberOfTimeSteps, deltaT, 2 /* numberOfFactors */, numberOfPaths, seed);
		final RandomVariableFromArrayFactory randomVariableFromArrayFactory = new RandomVariableFromArrayFactory();

		// Create Monte Carlo implementation of Black-Scholes model
		final DescribedModel<?> blackScholesModelMonteCarlo = (new AssetModelMonteCarloFactory(randomVariableFromArrayFactory, brownianMotion, null)).getModelFromDescriptor(blackScholesModelDescriptor);

		// Create product implementation compatible with Black-Scholes model
		final Product europeanOptionMonteCarlo = blackScholesModelMonteCarlo.getProductFromDescriptor(europeanOptionDescriptor);

		final Map<String, Object> valueMonteCarlo = europeanOptionMonteCarlo.getValues(evaluationTime, blackScholesModelMonteCarlo);

		System.out.println("Monte-Carlo implementation........:" + valueMonteCarlo);

		/*
		 * Create Finite Difference implementation of model and product
		 */

		// Create finite difference implementation of Black-Scholes model
		final DescribedModel<?> blackScholesModelFiniteDifference = (new BlackScholesModelMonteCarloFiniteDifference1D(0.5 /* theta */)).getModelFromDescriptor(blackScholesModelDescriptor);

		// Create product implementation compatible with Black-Scholes model
		final Product europeanOptionFiniteDifference = blackScholesModelFiniteDifference.getProductFromDescriptor(europeanOptionDescriptor);

		final Map<String, Object> valueFiniteDifference = europeanOptionFiniteDifference.getValues(evaluationTime, blackScholesModelFiniteDifference);

		System.out.println("Finite difference implementation..:" + valueFiniteDifference);

		/*
		 * Calculate analytic benchmark.
		 */

		final double optionMaturity = FloatingpointDate.getFloatingPointDateFromDate(referenceDate, ((SingleAssetEuropeanOptionProductDescriptor) europeanOptionDescriptor).getMaturity());
		final double forward = blackScholesModelDescriptor.getInitialValue() / blackScholesModelDescriptor.getDiscountCurveForForwardRate().getDiscountFactor(optionMaturity);
		final double payOffUnit = blackScholesModelDescriptor.getDiscountCurveForDiscountRate().getDiscountFactor(optionMaturity);
		final double volatility = blackScholesModelDescriptor.getVolatility();
		final double optionStrike = ((SingleAssetEuropeanOptionProductDescriptor) europeanOptionDescriptor).getStrike();

		final double valueAnalytic = AnalyticFormulas.blackScholesGeneralizedOptionValue(forward, volatility, optionMaturity, optionStrike, payOffUnit);

		System.out.println("Analytic implementation...........:" + valueAnalytic);

		/*
		 * Assertions
		 */

		Assert.assertEquals("Deviation of Fourier method from analytic", valueAnalytic, ((Double)valueFourier.get("value")).doubleValue(), 3E-4);
		Assert.assertEquals("Deviation of Monte-Carlo method from analytic", valueAnalytic, ((Double)valueMonteCarlo.get("value")).doubleValue(), 3E-4);
		Assert.assertEquals("Deviation of finite difference method from analytic", valueAnalytic, ((Double)valueFiniteDifference.get("value")).doubleValue(), 3E-4);
	}

	/**
	 * Get the discount curve using the riskFreeRate.
	 *
	 * @param name Name of the curve
	 * @param referenceDate Date corresponding to t=0.
	 * @param riskFreeRate Constant continuously compounded rate
	 *
	 * @return the discount curve using the riskFreeRate.
	 */
	private static DiscountCurve getDiscountCurve(final String name, final LocalDate referenceDate, final double riskFreeRate) {
		final double[] times = new double[] { 1.0 };
		final double[] givenAnnualizedZeroRates = new double[] { riskFreeRate };
		final InterpolationMethod interpolationMethod = InterpolationMethod.LINEAR;
		final InterpolationEntity interpolationEntity = InterpolationEntity.LOG_OF_VALUE_PER_TIME;
		final ExtrapolationMethod extrapolationMethod = ExtrapolationMethod.CONSTANT;
		final DiscountCurve discountCurve = DiscountCurveInterpolation.createDiscountCurveFromAnnualizedZeroRates(name, referenceDate, times, givenAnnualizedZeroRates, interpolationMethod, extrapolationMethod, interpolationEntity);
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
	private static BrownianMotion getBronianMotion(final int numberOfTimeSteps, final double deltaT, final int numberOfFactors, final int numberOfPaths, final int seed) {
		final TimeDiscretization timeDiscretization = new TimeDiscretizationFromArray(0.0 /* initial */, numberOfTimeSteps, deltaT);
		final BrownianMotion brownianMotion = new BrownianMotionFromMersenneRandomNumbers(timeDiscretization, numberOfFactors, numberOfPaths, seed);
		return brownianMotion;
	}

}
