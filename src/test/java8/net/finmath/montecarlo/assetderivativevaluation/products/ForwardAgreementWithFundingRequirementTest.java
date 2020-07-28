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
		double maturity = 5.0;
		double forwardValue = 1.25;

		double initialValue = 1.0;
		double riskFreeRate = 0.05;
		double volatility = 0.20;
		int numberOfFactors = 1;
		int numberOfPaths = 500000;
		int seed = 3216;

		TimeDiscretization timeDiscretization = new TimeDiscretizationFromArray(0.0, maturity);
		BrownianMotion brownianMotion = new BrownianMotionFromMersenneRandomNumbers(timeDiscretization, numberOfFactors, numberOfPaths, seed);

		MonteCarloBlackScholesModel model = new MonteCarloBlackScholesModel(initialValue, riskFreeRate, volatility, brownianMotion);

		double survivalProbLevelBoundary = 1.3;

		String currency = "EUR";
		SortedMap probs = new TreeMap<Double, Double>();
		probs.put(-Double.MAX_VALUE, 1.0);
		probs.put(survivalProbLevelBoundary, 1.0);
		//		probs.put(survivalProbLevelBoundary+1E-5, 0.5);
		probs.put(Double.MAX_VALUE, 0.5);

		SortedMap probsHalf = new TreeMap<Double, Double>();
		probsHalf.put(0.0, 0.5);
		probsHalf.put(Double.MAX_VALUE, 0.5);

		AbstractAssetMonteCarloProduct product = new ForwardAgreement(maturity, forwardValue, 0);
		AbstractAssetMonteCarloProduct product2 = new ForwardAgreementWithFundingRequirement(maturity, forwardValue, 0, new FundingCapacity(currency, new Scalar(0.0), probsHalf));
		AbstractAssetMonteCarloProduct product3 = new ForwardAgreementWithFundingRequirement(maturity, forwardValue, 0, new FundingCapacity(currency, new Scalar(0.0), probs));
		AbstractAssetMonteCarloProduct product4 = new EuropeanOption(maturity, forwardValue+survivalProbLevelBoundary, 0);
		AbstractAssetMonteCarloProduct product6 = new EuropeanOption(maturity, forwardValue-survivalProbLevelBoundary, 0);
		AbstractAssetMonteCarloProduct product5 = new DigitalOption(maturity, forwardValue+survivalProbLevelBoundary, 0);

		double valueForwardMixed = product3.getValue(model);
		double valueForwardPlain = product.getValue(model);
		double valueForwardHalf = product2.getValue(model);
		double valueOption = product4.getValue(model);
		double valueOption2 = product6.getValue(model);
		double valueDigital = product5.getValue(model);

		System.out.println(valueForwardPlain);
		System.out.println(valueForwardHalf);
		System.out.println(valueForwardMixed);
		System.out.println(valueOption);
		System.out.println(valueOption2);
		System.out.println(valueDigital);
		System.out.println(valueForwardPlain-0.5*valueOption);//-(1.0-(forwardValue-survivalProbLevelBoundary)-valueOption2));

	}

}
