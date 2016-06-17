/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 15.06.2016
 */

package net.finmath.montecarlo.assetderivativevaluation;

import static org.junit.Assert.*;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.exception.CalculationException;
import net.finmath.functions.AnalyticFormulas;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.assetderivativevaluation.products.EuropeanOption;
import net.finmath.montecarlo.model.AbstractModel;
import net.finmath.montecarlo.process.AbstractProcess;
import net.finmath.montecarlo.process.ProcessEulerScheme;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationInterface;

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
		AbstractModel model = new BlackScholesModel(initialValue, riskFreeRate, volatility);

		// Create a time discretizeion
		TimeDiscretizationInterface timeDiscretization = new TimeDiscretization(0.0 /* initial */, numberOfTimeSteps, deltaT);

		// Create a corresponding MC process
		AbstractProcess process = new ProcessEulerScheme(new BrownianMotion(timeDiscretization, 1 /* numberOfFactors */, numberOfPaths, seed));

		// Link model and process for delegation
		process.setModel(model);
		model.setProcess(process);

		/*
		 * Value a call option - directly
		 */
		
		RandomVariableInterface asset = process.getProcessValue(timeDiscretization.getTimeIndex(optionMaturity), assetIndex);
		RandomVariableInterface numeraireAtPayment = model.getNumeraire(optionMaturity);
		RandomVariableInterface numeraireAtEval = model.getNumeraire(0.0);
		
		RandomVariableInterface payoff = asset.sub(optionStrike).floor(0.0);
		double value = payoff.div(numeraireAtPayment).mult(numeraireAtEval).getAverage();

		double valueAnalytic = AnalyticFormulas.blackScholesOptionValue(initialValue, riskFreeRate, volatility, optionMaturity, optionStrike);
		System.out.println("value using Monte-Carlo.......: " + value);
		System.out.println("value using analytic formula..: " + valueAnalytic);
		
		Assert.assertEquals(valueAnalytic, value, 0.005);
	}

	@Test
	public void testProductImpelmentation() throws CalculationException {
		// Create a time discretizeion
		TimeDiscretizationInterface timeDiscretization = new TimeDiscretization(0.0 /* initial */, numberOfTimeSteps, deltaT);

		// Create a corresponding MC process
		AbstractProcess process = new ProcessEulerScheme(new BrownianMotion(timeDiscretization, 1 /* numberOfFactors */, numberOfPaths, seed));

		// Using the process (Euler scheme), create an MC simulation of a Black-Scholes model
		AssetModelMonteCarloSimulationInterface monteCarloBlackScholesModel = new MonteCarloBlackScholesModel(initialValue, riskFreeRate, volatility, process);

		/*
		 * Value a call option (using the product implementation)
		 */
		EuropeanOption europeanOption = new EuropeanOption(optionMaturity, optionStrike);
		double value = europeanOption.getValue(monteCarloBlackScholesModel);
		double valueAnalytic = AnalyticFormulas.blackScholesOptionValue(initialValue, riskFreeRate, volatility, optionMaturity, optionStrike);

		System.out.println("value using Monte-Carlo.......: " + value);
		System.out.println("value using analytic formula..: " + valueAnalytic);
		
		Assert.assertEquals(valueAnalytic, value, 0.005);
	}
}
