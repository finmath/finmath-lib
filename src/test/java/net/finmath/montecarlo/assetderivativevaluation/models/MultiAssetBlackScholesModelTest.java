package net.finmath.montecarlo.assetderivativevaluation.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.finmath.exception.CalculationException;
import net.finmath.functions.AnalyticFormulas;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.BrownianMotionFromMersenneRandomNumbers;
import net.finmath.montecarlo.assetderivativevaluation.MonteCarloAssetModel;
import net.finmath.montecarlo.assetderivativevaluation.products.EuropeanOption;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.montecarlo.process.MonteCarloProcess;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 *
 * TODO Add UnitTest for correlation - using ExchangeOption.
 *
 * @author Christian Fries
 */
public class MultiAssetBlackScholesModelTest {

	@Test
	public void testModelWithFactorLoadings() throws CalculationException {

		final double[] initialValue = new double[] { 100.0, 110 };
		final double riskFreeRate = 0.05;
		final double[][] factorLoadings = new double[][] {
			{ 0.25, 0.10 },
			{ 0.00, 0.20 }

		};

		final MultiAssetBlackScholesModel model = new MultiAssetBlackScholesModel(initialValue, riskFreeRate, factorLoadings);

		final int numberOfPaths = 1000000;
		final int seed = 3216;
		final MonteCarloAssetModel assetModel = getMonteCarloAssetModel(model, numberOfPaths, seed);

		/*
		 * Test the two individual European options
		 */
		final double maturity = 5.0;
		final double strike1 = 120;
		final double strike2 = 130;
		final EuropeanOption europeanOption1 = new EuropeanOption(maturity, strike1, 0);
		final EuropeanOption europeanOption2 = new EuropeanOption(maturity, strike2, 1);

		final double volatility1 = Math.sqrt(factorLoadings[0][0]*factorLoadings[0][0] + factorLoadings[0][1]*factorLoadings[0][1]);

		assertEqualsValuationEuropeanProduct(assetModel, europeanOption1, initialValue[0], riskFreeRate, volatility1);

		final double volatility2 = Math.sqrt(factorLoadings[1][0]*factorLoadings[1][0] + factorLoadings[1][1]*factorLoadings[1][1]);
		assertEqualsValuationEuropeanProduct(assetModel, europeanOption2, initialValue[1], riskFreeRate, volatility2);
	}

	@Test
	public void testModelWithVolatilityAndCorrelation() throws CalculationException {

		final double[] initialValue = new double[] { 100.0, 110 };
		final double riskFreeRate = 0.05;
		final double[] volatilities = new double[] { 0.30, 0.20 };
		final double[][] correlations = new double[][] {
			{ 1.0, 0.0 },
			{ 0.0, 1.0 }

		};

		final MultiAssetBlackScholesModel model = new MultiAssetBlackScholesModel(initialValue, riskFreeRate, volatilities, correlations);

		final int numberOfPaths = 1000000;
		final int seed = 3216;
		final MonteCarloAssetModel assetModel = getMonteCarloAssetModel(model, numberOfPaths, seed);

		/*
		 * Test the two individual European options
		 */
		final double maturity = 5.0;
		final double strike1 = 120;
		final double strike2 = 130;
		final EuropeanOption europeanOption1 = new EuropeanOption(maturity, strike1, 0);
		final EuropeanOption europeanOption2 = new EuropeanOption(maturity, strike2, 1);

		assertEqualsValuationEuropeanProduct(assetModel, europeanOption1, initialValue[0], riskFreeRate, volatilities[0]);

		assertEqualsValuationEuropeanProduct(assetModel, europeanOption2, initialValue[1], riskFreeRate, volatilities[1]);
	}

	@Test
	public void testModelCloneWithModifiedSeed() throws CalculationException {

		final double[] initialValue = new double[] { 100.0, 110 };
		final double riskFreeRate = 0.05;
		final double[][] factorLoadings = new double[][] {
			{ 0.25, 0.10 },
			{ 0.00, 0.20 }

		};

		final MultiAssetBlackScholesModel model = new MultiAssetBlackScholesModel(initialValue, riskFreeRate, factorLoadings);

		final int numberOfPaths = 10000;
		final int seed = 3216;
		final MonteCarloAssetModel assetModel = getMonteCarloAssetModel(model, numberOfPaths, seed);

		/*
		 * Test the European option with different seeds.
		 */
		final double maturity = 5.0;
		final double strike = 120;
		final EuropeanOption europeanOption = new EuropeanOption(maturity, strike, 0);

		final double valueSeed1 = europeanOption.getValue(assetModel);
		final double valueSeed2 = europeanOption.getValue(assetModel.getCloneWithModifiedData(Map.of("seed", 3141)));
		final double valueSeed3 = europeanOption.getValue(getMonteCarloAssetModel(model, numberOfPaths, 3141));

		Assertions.assertNotEquals(valueSeed1, valueSeed2, "Models with different seed.");
		Assertions.assertEquals(valueSeed2, valueSeed3, "Models with same seed, different construction.");
	}

	@Test
	public void testModelWithCloneModifiedVolatility() throws CalculationException {

		final double[] initialValue = new double[] { 100.0, 110 };
		final double riskFreeRate = 0.05;
		final double[] volatilities = new double[] { 0.30, 0.20 };
		final double[][] correlations = new double[][] {
			{ 1.0, 0.2 },
			{ 0.2, 1.0 }

		};

		final MultiAssetBlackScholesModel model = new MultiAssetBlackScholesModel(initialValue, riskFreeRate, volatilities, correlations);

		final int numberOfPaths = 10000;
		final int seed = 3216;
		final MonteCarloAssetModel assetModel = getMonteCarloAssetModel(model, numberOfPaths, seed);

		/*
		 * Test the two individual European options
		 */
		final double maturity = 5.0;
		final double strike1 = 120;
		final double strike2 = 130;
		final EuropeanOption europeanOption1 = new EuropeanOption(maturity, strike1, 0);
		final EuropeanOption europeanOption2 = new EuropeanOption(maturity, strike2, 1);

		/*
		 * Calling getCloneWithModifiedData, but using same volatilities as before.
		 */
		final double tolerance = 1E-5;
		assertEquals(europeanOption1.getValue(assetModel), europeanOption1.getValue(assetModel.getCloneWithModifiedData(Map.of("volatilities", volatilities))), tolerance);
		assertEquals(europeanOption2.getValue(assetModel), europeanOption2.getValue(assetModel.getCloneWithModifiedData(Map.of("volatilities", volatilities))), tolerance);
		assertEqualsValuationEuropeanProduct(
				assetModel.getCloneWithModifiedData(Map.of("volatilities", volatilities)),
				europeanOption1, initialValue[0], riskFreeRate, volatilities[0]);
		assertEqualsValuationEuropeanProduct(
				assetModel.getCloneWithModifiedData(Map.of("volatilities", volatilities)),
				europeanOption2, initialValue[1], riskFreeRate, volatilities[1]);

		/*
		 * Calling getCloneWithModifiedData, but using different volatilities.
		 */
		final double[] newVolatilities = new double[] { 0.10, 0.30 };
		assertEqualsValuationEuropeanProduct(
				assetModel.getCloneWithModifiedData(Map.of("volatilities", newVolatilities)),
				europeanOption1, initialValue[0], riskFreeRate, newVolatilities[0]);
		assertEqualsValuationEuropeanProduct(
				assetModel.getCloneWithModifiedData(Map.of("volatilities", newVolatilities)),
				europeanOption2, initialValue[1], riskFreeRate, newVolatilities[1]);

		/*
		 * Checking that the two models are indeed different
		 */
		assertNotEquals(europeanOption1.getValue(assetModel), europeanOption1.getValue(assetModel.getCloneWithModifiedData(Map.of("volatilities", newVolatilities))), tolerance);
		assertNotEquals(europeanOption2.getValue(assetModel), europeanOption2.getValue(assetModel.getCloneWithModifiedData(Map.of("volatilities", newVolatilities))), tolerance);
	}

	/*
	 * Private helpers
	 */

	private MonteCarloAssetModel getMonteCarloAssetModel(MultiAssetBlackScholesModel model, int numberOfPaths, int seed) {
		final int numberOfTimeSteps = 30;
		final double deltaT = 0.2;

		final int numberOfFactors = model.getNumberOfFactors();

		// Create a time discretization
		final TimeDiscretization timeDiscretization = new TimeDiscretizationFromArray(0.0 /* initial */, numberOfTimeSteps, deltaT);

		// Create Brownian motion (and random number generator)
		final BrownianMotion brownianMotion = new BrownianMotionFromMersenneRandomNumbers(timeDiscretization, numberOfFactors , numberOfPaths, seed);

		final MonteCarloProcess process = new EulerSchemeFromProcessModel(model, brownianMotion);

		return new MonteCarloAssetModel(process);
	}

	private static void assertEqualsValuationEuropeanProduct(MonteCarloAssetModel assetModel, EuropeanOption europeanOption, double initialValue, double riskFreeRate, double volatility) throws CalculationException {

		final double tolerance = 100 * 1.0/Math.sqrt(assetModel.getNumberOfPaths());
		final double valueMonteCarlo = europeanOption.getValue(assetModel);
		final double valueAnalytic = AnalyticFormulas.blackScholesOptionValue(initialValue, riskFreeRate, volatility, europeanOption.getMaturity(), europeanOption.getStrike());
		Assertions.assertEquals(valueAnalytic, valueMonteCarlo, tolerance, "valuation");

	}
}
