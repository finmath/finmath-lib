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

import net.finmath.marketdata.model.curves.Curve.ExtrapolationMethod;
import net.finmath.marketdata.model.curves.Curve.InterpolationEntity;
import net.finmath.marketdata.model.curves.Curve.InterpolationMethod;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.DiscountCurveInterface;
import net.finmath.modelling.Model;
import net.finmath.modelling.Product;
import net.finmath.modelling.ProductDescriptor;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.BrownianMotionInterface;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * Unit test creating a Heston model and a European option from corresponding model descriptors and product descriptors
 * using two different factories: Fourier versus Monte-Carlo.
 * 
 * @author Christian Fries
 */
public class HestonModelDescriptorTest {

	// Model properties
	private final LocalDate referenceDate = LocalDate.of(2017,8,15);

	private final double initialValue   = 1.0;
	private final double riskFreeRate   = 0.05;
	private final double volatility     = 0.30;

	private final double theta = volatility*volatility;
	private final double kappa = 0.1;
	private final double xi = 0.50;
	private final double rho = 0.1;

	// Product properties
	private static final double maturity = 1.0;
	private static final double strike		= 0.95;

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
		ProductDescriptor europeanOptionDescriptor = (new SingleAssetEuropeanOptionProductDescriptor(underlyingName, maturity, strike));

		/*
		 * Create Fourier implementation of model and product
		 */

		// Create Fourier implementation of Heston model
		Model<?> hestonModelFourier = (new HestonModelFourierFactory()).getModelFromDescriptor(hestonModelDescriptor);

		// Create product implementation compatible with Heston model
		Product<?> europeanOptionFourier = hestonModelFourier.getProductFromDesciptor(europeanOptionDescriptor);
		
		// Evaluate product
		double evaluationTime = 0.0;
		Map<String, Object> valueFourier = europeanOptionFourier.getValues(evaluationTime, hestonModelFourier);

		System.out.println(valueFourier);

		/*
		 * Create Monte Carlo implementation of model and product
		 */

		// Create a time discretization
		BrownianMotionInterface brownianMotion = getBronianMotion(numberOfTimeSteps, deltaT, 2 /* numberOfFactors */, numberOfPaths, seed);
		RandomVariableFactory randomVariableFactory = new RandomVariableFactory();
		
		// Create Fourier implementation of Heston model
		Model<?> hestonModelMonteCarlo = (new HestonModelMonteCarloFactory(net.finmath.montecarlo.assetderivativevaluation.HestonModel.Scheme.FULL_TRUNCATION, randomVariableFactory, brownianMotion)).getModelFromDescriptor(hestonModelDescriptor);

		// Create product implementation compatible with Heston model
		Product<?> europeanOptionMonteCarlo = hestonModelMonteCarlo.getProductFromDesciptor(europeanOptionDescriptor);

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
	private static DiscountCurveInterface getDiscountCurve(String name, LocalDate referenceDate, double riskFreeRate) {
		double[] times = new double[] { 1.0 };
		double[] givenAnnualizedZeroRates = new double[] { riskFreeRate };
		InterpolationMethod interpolationMethod = InterpolationMethod.LINEAR;
		InterpolationEntity interpolationEntity = InterpolationEntity.LOG_OF_VALUE_PER_TIME;
		ExtrapolationMethod extrapolationMethod = ExtrapolationMethod.CONSTANT;
		DiscountCurveInterface discountCurve = DiscountCurve.createDiscountCurveFromAnnualizedZeroRates(name, referenceDate, times, givenAnnualizedZeroRates, interpolationMethod, extrapolationMethod, interpolationEntity);
		return discountCurve;
	}

	/**
	 * Create a Brownian motion implementing BrownianMotionInterface from given specs.
	 * 
	 * @param numberOfTimeSteps The number of time steps.
	 * @param deltaT The time step size.
	 * @param numberOfFactors The number of factors.
	 * @param numberOfPaths The number of paths.
	 * @param seed The seed for the random number generator.
	 * @return A Brownian motion implementing BrownianMotionInterface with the given specs.
	 */
	private static BrownianMotionInterface getBronianMotion(int numberOfTimeSteps, double deltaT, int numberOfFactors, int numberOfPaths, int seed) {
		TimeDiscretizationInterface timeDiscretization = new TimeDiscretization(0.0 /* initial */, numberOfTimeSteps, deltaT);
		BrownianMotionInterface brownianMotion = new BrownianMotion(timeDiscretization, numberOfFactors, numberOfPaths, seed);
		return brownianMotion;
	}

}
