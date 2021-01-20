package net.finmath.montecarlo.assetderivativevaluation.products;

import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.BrownianMotionFromMersenneRandomNumbers;
import net.finmath.montecarlo.assetderivativevaluation.MonteCarloBlackScholesModel;
import net.finmath.montecarlo.interestrate.models.FundingCapacity;
import net.finmath.stochastic.Scalar;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

public class ForwardAgreementWithFundingRequirementTest {

	@Test
	public void test() throws CalculationException {
		final double maturity = 5.0;
		final double forwardValue = 1.25;

		final double initialValue = 1.0;
		final double riskFreeRate = 0.05;
		final double volatility = 0.20;
		final int numberOfFactors = 1;
		final int numberOfPaths = 500000;
		final int seed = 3216;

		final TimeDiscretization timeDiscretization = new TimeDiscretizationFromArray(0.0, maturity);
		final BrownianMotion brownianMotion = new BrownianMotionFromMersenneRandomNumbers(timeDiscretization, numberOfFactors, numberOfPaths, seed);

		final MonteCarloBlackScholesModel model = new MonteCarloBlackScholesModel(initialValue, riskFreeRate, volatility, brownianMotion);

		final double survivalProbLevelBoundary = 1.3;

		final String currency = "EUR";
		final SortedMap probs = new TreeMap<Double, Double>();
		probs.put(-Double.MAX_VALUE, 1.0);
		probs.put(survivalProbLevelBoundary, 1.0);
		//		probs.put(survivalProbLevelBoundary+1E-5, 0.5);
		probs.put(Double.MAX_VALUE, 0.5);

		final SortedMap probsHalf = new TreeMap<Double, Double>();
		probsHalf.put(0.0, 0.5);
		probsHalf.put(Double.MAX_VALUE, 0.5);

		final AbstractAssetMonteCarloProduct product = new ForwardAgreement(maturity, forwardValue, 0);
		final AbstractAssetMonteCarloProduct product2 = new ForwardAgreementWithFundingRequirement(maturity, forwardValue, 0, new FundingCapacity(currency, new Scalar(0.0), probsHalf));
		final AbstractAssetMonteCarloProduct product3 = new ForwardAgreementWithFundingRequirement(maturity, forwardValue, 0, new FundingCapacity(currency, new Scalar(0.0), probs));
		final AbstractAssetMonteCarloProduct product4 = new EuropeanOption(maturity, forwardValue+survivalProbLevelBoundary, 0);
		final AbstractAssetMonteCarloProduct product6 = new EuropeanOption(maturity, forwardValue-survivalProbLevelBoundary, 0);
		final AbstractAssetMonteCarloProduct product5 = new DigitalOption(maturity, forwardValue+survivalProbLevelBoundary, 0);

		final double valueForwardMixed = product3.getValue(model);
		final double valueForwardPlain = product.getValue(model);
		final double valueForwardHalf = product2.getValue(model);
		final double valueOption = product4.getValue(model);
		final double valueOption2 = product6.getValue(model);
		final double valueDigital = product5.getValue(model);

		System.out.println(valueForwardPlain);
		System.out.println(valueForwardHalf);
		System.out.println(valueForwardMixed);
		System.out.println(valueOption);
		System.out.println(valueOption2);
		System.out.println(valueDigital);
		System.out.println(valueForwardPlain-0.5*valueOption);//-(1.0-(forwardValue-survivalProbLevelBoundary)-valueOption2));

	}

}
