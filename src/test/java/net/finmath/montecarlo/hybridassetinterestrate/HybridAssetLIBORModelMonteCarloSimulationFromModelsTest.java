package net.finmath.montecarlo.hybridassetinterestrate;

import org.junit.Test;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.BrownianMotionFromMersenneRandomNumbers;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFromArrayFactory;
import net.finmath.montecarlo.assetderivativevaluation.MonteCarloAssetModel;
import net.finmath.montecarlo.assetderivativevaluation.models.BlackScholesModel;
import net.finmath.montecarlo.assetderivativevaluation.products.EuropeanOption;
import net.finmath.montecarlo.interestrate.LIBORMarketModelValuationTest;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.montecarlo.model.ProcessModel;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.montecarlo.process.MonteCarloProcessFromProcessModel;

public class HybridAssetLIBORModelMonteCarloSimulationFromModelsTest {

	@Test
	public void test() throws CalculationException {
		final RandomVariableFactory randomVariableFactory = new RandomVariableFromArrayFactory();
		final int numberOfPaths = 10000;
		final int numberOfFactors = 5;
		final double correlationDecayParam = 0.01;
		final LIBORModelMonteCarloSimulationModel model = LIBORMarketModelValuationTest.createLIBORMarketModel(randomVariableFactory, numberOfPaths, numberOfFactors, correlationDecayParam);


		final double initialValue = 100.0;
		final double riskFreeRate = 0.05;
		final double volatility = 0.30;
		// Create the model
		final ProcessModel blackScholesModel = new BlackScholesModel(initialValue, riskFreeRate, volatility);

		// Create a corresponding MC process
		final MonteCarloProcessFromProcessModel process = new EulerSchemeFromProcessModel(blackScholesModel, new BrownianMotionFromMersenneRandomNumbers(model.getTimeDiscretization(), 1 /* numberOfFactors */, numberOfPaths, 323, randomVariableFactory));

		final var model2 = new MonteCarloAssetModel(blackScholesModel, process);

		final HybridAssetLIBORModelMonteCarloSimulationFromModels model3 = new HybridAssetLIBORModelMonteCarloSimulationFromModels(model, model2);

		final double maturity = 3.0;

		for(double strike = 60.0; strike < 150.0; strike += 0.1) {
			final EuropeanOption product = new EuropeanOption(maturity, strike);

			final double value1 = product.getValue(model2);
			final double value2 = product.getValue(model3);

			System.out.println(strike + "\t" + value1 + "\t" +value2);
		}
	}
}
