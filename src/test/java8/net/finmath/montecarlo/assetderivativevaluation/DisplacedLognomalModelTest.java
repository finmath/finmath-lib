/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 25.06.2016
 */

package net.finmath.montecarlo.assetderivativevaluation;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.finmath.exception.CalculationException;
import net.finmath.functions.AnalyticFormulas;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.BrownianMotionFromMersenneRandomNumbers;
import net.finmath.montecarlo.assetderivativevaluation.models.BlackScholesModel;
import net.finmath.montecarlo.assetderivativevaluation.models.DisplacedLognomalModel;
import net.finmath.montecarlo.assetderivativevaluation.models.InhomogeneousDisplacedLognomalModel;
import net.finmath.montecarlo.assetderivativevaluation.models.InhomogenousBachelierModel;
import net.finmath.montecarlo.assetderivativevaluation.products.EuropeanOption;
import net.finmath.montecarlo.model.ProcessModel;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.montecarlo.process.MonteCarloProcessFromProcessModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * @author Christian Fries
 *
 */
@RunWith(Parameterized.class)
public class DisplacedLognomalModelTest {

	private static DecimalFormat formatterValue		= new DecimalFormat(" 00.000%;-00.000%", new DecimalFormatSymbols(Locale.ENGLISH));

	// Model properties
	private final double	initialValue	= 1.0;
	private final double	riskFreeRate	= 0.05;
	private final double	displacement;

	private final double	volatility		= 0.30;

	// Process discretization properties
	private final int		numberOfPaths		= 10000;
	private final int		numberOfTimeSteps	= 100;
	private final double	deltaT				= 0.05;

	private final int		seed				= 31415;

	// Product properties
	private final int		assetIndex = 0;
	private final double	optionMaturity = 5.0;
	private final double	optionStrike = 1.284;

	/*
	 * Run the test with different values for the displacement
	 */
	@Parameters(name="displacement={0}")
	public static Collection<Object[]> generateData()
	{
		return Arrays.asList(new Object[][] {
			{ 0.00 },
			{ 0.01 },
			{ 0.05 },
		});
	}

	/*
	 * Set the displacement using the parameterization.
	 */
	public DisplacedLognomalModelTest(double displacement) {
		super();
		this.displacement = displacement;
	}

	@Test
	public void testProductImplementation() throws CalculationException {
		// Create a time discretization
		final TimeDiscretization timeDiscretization = new TimeDiscretizationFromArray(0.0 /* initial */, numberOfTimeSteps, deltaT);

		// Create a brownianMotion
		final BrownianMotion brownianMotion = new BrownianMotionFromMersenneRandomNumbers(timeDiscretization, 1 /* numberOfFactors */, numberOfPaths, seed);

		final AssetModelMonteCarloSimulationModel monteCarloInhomogeneouseDisplacedModel = getMonteCarloSimulationFromModel(
				new InhomogeneousDisplacedLognomalModel(initialValue, riskFreeRate, displacement, volatility),
				brownianMotion);

		final AssetModelMonteCarloSimulationModel monteCarloDisplacedModel = getMonteCarloSimulationFromModel(
				new DisplacedLognomalModel(initialValue, riskFreeRate, displacement, volatility),
				brownianMotion);

		final AssetModelMonteCarloSimulationModel monteCarloBlackScholesModel = getMonteCarloSimulationFromModel(
				new BlackScholesModel(initialValue, riskFreeRate, volatility),
				brownianMotion);

		final AssetModelMonteCarloSimulationModel monteCarloBachelierModel = getMonteCarloSimulationFromModel(
				new InhomogenousBachelierModel(initialValue, riskFreeRate, volatility*displacement),
				brownianMotion);

		/*
		 * Value a call option (using the product implementation)
		 */
		final double evaluationTime = 0.0;

		final EuropeanOption europeanOption = new EuropeanOption(optionMaturity, optionStrike);
		final RandomVariable valueInhomogeneousDisplaced	= europeanOption.getValue(evaluationTime, monteCarloInhomogeneouseDisplacedModel);
		final RandomVariable valueDisplaced	= europeanOption.getValue(evaluationTime, monteCarloDisplacedModel);
		final RandomVariable valueBlack		= europeanOption.getValue(evaluationTime, monteCarloBlackScholesModel);
		final RandomVariable valueBachelier	= europeanOption.getValue(evaluationTime, monteCarloBachelierModel);
		final double valueAnalytic = AnalyticFormulas.blackScholesOptionValue(initialValue, riskFreeRate, volatility, optionMaturity, optionStrike);

		/*
		 * Note: The inhomogenous displaced model is not the same as the corresponding interpolation
		 * of Black and Bachelier. For a homogenous displaced model there is a relation between dispalcement
		 * and interpolation:
		 * 	sigma (S+d) = sigma* (a S + (1-a))
		 * implies
		 * 	sigma = sigma* a
		 * 	sigma d = sigma* (1-a)
		 * that is
		 * 	d = (1-a)/a
		 * 	a = 1/(1+d)
		 */
		final double alpha = 1/(1+displacement);
		final double valueSumBlackBachelier = (valueBlack.getAverage() + valueBachelier.getAverage());

		System.out.println("value using Monte-Carlo Inhom. Displaced Model: " + formatterValue.format(valueInhomogeneousDisplaced.getAverage()) + " ± " + formatterValue.format(valueInhomogeneousDisplaced.getStandardError()));
		System.out.println("value using Monte-Carlo Homog. Displaced Model: " + formatterValue.format(valueDisplaced.getAverage()) + " ± " + formatterValue.format(valueInhomogeneousDisplaced.getStandardError()));
		System.out.println("value using Monte-Carlo Black Scholes Model...: " + formatterValue.format(valueBlack.getAverage()) + " ± " + formatterValue.format(valueBlack.getStandardError()));
		System.out.println("value using Monte-Carlo Bachelier Model.......: " + formatterValue.format(valueBachelier.getAverage()) + " ± " + formatterValue.format(valueBachelier.getStandardError()));
		System.out.println("value using sum of Black and Bachelier........: " + formatterValue.format(valueSumBlackBachelier));
		System.out.println("value using analytic formula for Black Model..: " + formatterValue.format(valueAnalytic));

		System.out.println("_______________________________________________________________________________");

		Assert.assertEquals("Value Black-Scholes", valueBlack.getAverage(), valueAnalytic, 1E-2);

		// Appoximately the sum of the two models
		Assert.assertEquals("Value Displaced-Model", valueInhomogeneousDisplaced.getAverage(), valueSumBlackBachelier, 1E-2);
	}

	private AssetModelMonteCarloSimulationModel getMonteCarloSimulationFromModel(ProcessModel model, BrownianMotion brownianMotion) {
		// Create a corresponding MC process
		final MonteCarloProcessFromProcessModel process = new EulerSchemeFromProcessModel(model, brownianMotion);

		// Using the process (Euler scheme), create an MC simulation model
		return new MonteCarloAssetModel(process);
	}
}
