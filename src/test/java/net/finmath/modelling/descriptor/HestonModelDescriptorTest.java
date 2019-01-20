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

import net.finmath.marketdata.model.curves.CurveFromInterpolationPoints.ExtrapolationMethod;
import net.finmath.marketdata.model.curves.CurveFromInterpolationPoints.InterpolationEntity;
import net.finmath.marketdata.model.curves.CurveFromInterpolationPoints.InterpolationMethod;
import net.finmath.marketdata.model.curves.DiscountCurveInterpolation;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.modelling.DescribedModel;
import net.finmath.modelling.Product;
import net.finmath.modelling.ProductDescriptor;
import net.finmath.modelling.modelfactory.AssetModelFourierMethodFactory;
import net.finmath.modelling.modelfactory.AssetModelMonteCarloFactory;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.BrownianMotionLazyInit;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.assetderivativevaluation.models.HestonModel.Scheme;
import net.finmath.time.FloatingpointDate;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * Unit test creating a Heston model and a European option from corresponding model descriptors and product descriptors
 * using two different factories: Fourier versus Monte-Carlo.
 *
 * @author Christian Fries
 */
public class HestonModelDescriptorTest {

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
	private static final LocalDate maturityDate	= FloatingpointDate.getDateFromFloatingPointDate(referenceDate, maturity);
	private static final double strike				= 0.95;

	// Monte Carlo simulation  properties
	private final int		numberOfPaths		= 100000;
	private final int		numberOfTimeSteps	= 100;
	private final double 	deltaT				= 0.05;
	private final int		seed				= 31415;

	@Test
	public void test() {
		/*
		 * Create Heston Model descriptor
		 */
		HestonModelDescriptor hestonModelDescriptor = new HestonModelDescriptor(referenceDate, initialValue, getDiscountCurve("forward curve", referenceDate, riskFreeRate), getDiscountCurve("discount curve", referenceDate, riskFreeRate), volatility, theta, kappa, xi, rho);

		/*
		 * Create European option descriptor
		 */
		String underlyingName = "eurostoxx";
		ProductDescriptor europeanOptionDescriptor = (new SingleAssetEuropeanOptionProductDescriptor(underlyingName, maturityDate, strike));

		/*
		 * Create Fourier implementation of model and product
		 */

		// Create Fourier implementation of Heston model
		DescribedModel<?> hestonModelFourier = (new AssetModelFourierMethodFactory()).getModelFromDescriptor(hestonModelDescriptor);

		// Create product implementation compatible with Heston model
		Product europeanOptionFourier = hestonModelFourier.getProductFromDescriptor(europeanOptionDescriptor);

		// Evaluate product
		double evaluationTime = 0.0;
		Map<String, Object> valueFourier = europeanOptionFourier.getValues(evaluationTime, hestonModelFourier);

		System.out.println(valueFourier);

		/*
		 * Create Monte Carlo implementation of model and product
		 */

		// Create a time discretization
		BrownianMotion brownianMotion = getBronianMotion(numberOfTimeSteps, deltaT, 2 /* numberOfFactors */, numberOfPaths, seed);
		RandomVariableFactory randomVariableFactory = new RandomVariableFactory();

		// Create Fourier implementation of Heston model
		DescribedModel<?> hestonModelMonteCarlo = (new AssetModelMonteCarloFactory(randomVariableFactory, brownianMotion, Scheme.FULL_TRUNCATION)).getModelFromDescriptor(hestonModelDescriptor);

		// Create product implementation compatible with Heston model
		Product europeanOptionMonteCarlo = hestonModelMonteCarlo.getProductFromDescriptor(europeanOptionDescriptor);

		Map<String, Object> valueMonteCarlo = europeanOptionMonteCarlo.getValues(evaluationTime, hestonModelMonteCarlo);

		System.out.println(valueMonteCarlo);

		double deviation = (Double)valueMonteCarlo.get("value") - (Double)valueFourier.get("value");
		Assert.assertEquals("Difference of Fourier and Monte-Carlo valuation", 0.0, deviation, 1E-3);
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
	private static DiscountCurve getDiscountCurve(String name, LocalDate referenceDate, double riskFreeRate) {
		double[] times = new double[] { 1.0 };
		double[] givenAnnualizedZeroRates = new double[] { riskFreeRate };
		InterpolationMethod interpolationMethod = InterpolationMethod.LINEAR;
		InterpolationEntity interpolationEntity = InterpolationEntity.LOG_OF_VALUE_PER_TIME;
		ExtrapolationMethod extrapolationMethod = ExtrapolationMethod.CONSTANT;
		DiscountCurve discountCurve = DiscountCurveInterpolation.createDiscountCurveFromAnnualizedZeroRates(name, referenceDate, times, givenAnnualizedZeroRates, interpolationMethod, extrapolationMethod, interpolationEntity);
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
