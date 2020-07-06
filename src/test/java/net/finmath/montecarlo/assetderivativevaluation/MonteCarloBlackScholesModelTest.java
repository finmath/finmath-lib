/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 15.06.2016
 */

package net.finmath.montecarlo.assetderivativevaluation;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.exception.CalculationException;
import net.finmath.functions.AnalyticFormulas;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.BrownianMotionFromMersenneRandomNumbers;
import net.finmath.montecarlo.assetderivativevaluation.models.BlackScholesModel;
import net.finmath.montecarlo.assetderivativevaluation.products.EuropeanOption;
import net.finmath.montecarlo.model.ProcessModel;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel.Scheme;
import net.finmath.montecarlo.process.MonteCarloProcess;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * @author Christian Fries
 *
 */
public class MonteCarloBlackScholesModelTest {

	// Model properties
	private final double	initialValue   = 1.0;
	private final double	riskFreeRate   = 0.05;
	private final double	volatility     = 0.30;

	// Process discretization properties
	private final int		numberOfPaths		= 20000;
	private final int		numberOfTimeSteps	= 10;
	private final double	deltaT				= 0.5;

	private final int		seed				= 31415;

	// Product properties
	private final int		assetIndex = 0;
	private final double	optionMaturity = 2.0;
	private final double	optionStrike = 1.05;

	@Test
	public void testDirectValuation() throws CalculationException {
		// Create a model
		final ProcessModel model = new BlackScholesModel(initialValue, riskFreeRate, volatility);

		// Create a time discretization
		final TimeDiscretization timeDiscretization = new TimeDiscretizationFromArray(0.0 /* initial */, numberOfTimeSteps, deltaT);

		// Create a corresponding MC process
		final MonteCarloProcess process = new EulerSchemeFromProcessModel(model, new BrownianMotionFromMersenneRandomNumbers(timeDiscretization, 1 /* numberOfFactors */, numberOfPaths, seed), Scheme.EULER);

		/*
		 * Value a call option - directly using random variables not using product class.
		 */

		final RandomVariable asset = process.getProcessValue(timeDiscretization.getTimeIndex(optionMaturity), assetIndex);
		final RandomVariable numeraireAtPayment = model.getNumeraire(process, optionMaturity);
		final RandomVariable numeraireAtEval = model.getNumeraire(process, 0.0);

		final RandomVariable payoff = asset.sub(optionStrike).floor(0.0);
		final double value = payoff.div(numeraireAtPayment).mult(numeraireAtEval).getAverage();

		final double valueAnalytic = AnalyticFormulas.blackScholesOptionValue(initialValue, riskFreeRate, volatility, optionMaturity, optionStrike);
		System.out.println("value using Monte-Carlo.......: " + value);
		System.out.println("value using analytic formula..: " + valueAnalytic);

		Assert.assertEquals(valueAnalytic, value, 0.005);
	}

	@Test
	public void testProductImplementation() throws CalculationException {
		/*
		 * Model and Numerical Method
		 */

		// Create a process model (for the Euler-scheme)
		final ProcessModel model = new BlackScholesModel(initialValue, riskFreeRate, volatility);

		// Create a time discretization
		final TimeDiscretization timeDiscretization = new TimeDiscretizationFromArray(0.0 /* initial */, numberOfTimeSteps, deltaT);

		// Create Brownian motion (and random number generator)
		final BrownianMotion brownianMotion = new BrownianMotionFromMersenneRandomNumbers(timeDiscretization, 1 /* numberOfFactors */, numberOfPaths, seed);

		// Create a corresponding MC process
		final MonteCarloProcess process = new EulerSchemeFromProcessModel(model, brownianMotion);

		// Using the process (Euler scheme), create an MC simulation of a Black-Scholes model
		final AssetModelMonteCarloSimulationModel monteCarloBlackScholesModel = new MonteCarloAssetModel(process);

		/*
		 * Product: Value a call option (using the product implementation)
		 */

		// Create product
		final EuropeanOption europeanOption = new EuropeanOption(optionMaturity, optionStrike);

		// Value product using model
		final double value = europeanOption.getValue(monteCarloBlackScholesModel);

		/*
		 * Analytic value using Black-Scholes formula
		 */

		final double valueAnalytic = AnalyticFormulas.blackScholesOptionValue(initialValue, riskFreeRate, volatility, optionMaturity, optionStrike);


		System.out.println("value using Monte-Carlo.......: " + value);
		System.out.println("value using analytic formula..: " + valueAnalytic);

		Assert.assertEquals(valueAnalytic, value, 0.005);
	}
}
