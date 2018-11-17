/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 15.06.2016
 */

package net.finmath.montecarlo.automaticdifferentiation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Assert;
import org.junit.Test;

import net.finmath.exception.CalculationException;
import net.finmath.functions.AnalyticFormulas;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.assetderivativevaluation.BlackScholesModel;
import net.finmath.montecarlo.assetderivativevaluation.MonteCarloAssetModel;
import net.finmath.montecarlo.assetderivativevaluation.products.DigitalOption;
import net.finmath.montecarlo.assetderivativevaluation.products.DigitalOptionDeltaLikelihood;
import net.finmath.montecarlo.automaticdifferentiation.backward.RandomVariableDifferentiableAADFactory;
import net.finmath.montecarlo.conditionalexpectation.LinearRegression;
import net.finmath.montecarlo.model.AbstractModel;
import net.finmath.montecarlo.process.AbstractProcess;
import net.finmath.montecarlo.process.ProcessEulerScheme;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.stochastic.Scalar;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * This test checks several methods for calculating the delta of a digital option. Among the methods
 * is the calculation via AAD with regression,
 * see Stochastic Algorithmic Differentiation of (Expectations of) Discontinuous Functions (Indicator Functions).
 * https://ssrn.com/abstract=3282667
 *
 * The test shows that the method provides a significant improvement in the quality (in terms of variance redcution):
 * AAD is 8 time better than finite differences,
 * AAD with regression is 3 times better than AAD
 * Likelihood Ratio (the benchmark for a digital option) is only 6% better than AAD with regression.
 *
 * @author Christian Fries
 */
public class MonteCarloBlackScholesModelDigitalOptionAADRegressionSensitivitiesTest {

	// Model properties
	private final double	modelInitialValue   = 1.0;
	private final double	modelRiskFreeRate   = 0.05;
	private final double	modelVolatility     = 0.50;

	// Process discretization properties
	private final int		numberOfPaths		= 200000;
	private final int		numberOfTimeSteps	= 1;
	private final double	deltaT				= 1.0;

	// Product properties
	private final int		assetIndex = 0;
	private final double	optionMaturity = 1.0;
	private final double	optionStrike = 1.05;

	@Test
	public void test() throws CalculationException {


		int seed = 3141;
		double width = 0.1;

		Map<String, Object> results = getSensitivityApproximations(width, seed);

		RandomVariableInterface deltaAAD = (RandomVariableInterface)results.get("delta.aad");
		RandomVariableInterface deltaAADReg = (RandomVariableInterface)results.get("delta.aadreg");
		Double deltaAnalytic = (Double)results.get("delta.analytic");
		RandomVariableInterface deltaFD = (RandomVariableInterface)results.get("delta.fd");

		RandomVariableInterface deltaLikelihoodRatio = (RandomVariableInterface)results.get("delta.likelihood");

		System.out.println(deltaFD.getAverage()-deltaAnalytic);
		System.out.println(deltaAAD.getAverage()-deltaAnalytic);
		System.out.println(deltaAADReg.getAverage()-deltaAnalytic);
		System.out.println(deltaLikelihoodRatio.getAverage()-deltaAnalytic);

		Assert.assertEquals("digital option delta finite difference", deltaAnalytic, deltaFD.getAverage(), 1E-1);
		Assert.assertEquals("digital option delta aad", deltaAnalytic, deltaAAD.getAverage(), 1E-2);
		Assert.assertEquals("digital option delta aad regression", deltaAnalytic, deltaAADReg.getAverage(), 4E-3);
		Assert.assertEquals("digital option delta likelihood ratio", deltaAnalytic, deltaLikelihoodRatio.getAverage(), 4E-3);
	}

	public Map<String, Object> getSensitivityApproximations(double width, int seed) throws CalculationException {
		Map<String, Object> results = new HashMap<String, Object>();


		Map<String, Object> randomVariableProps = new HashMap<String, Object>();
		randomVariableProps.put("barrierDiracWidth", width);	// 0.05 is the default
		RandomVariableDifferentiableAADFactory randomVariableFactory = new RandomVariableDifferentiableAADFactory(new RandomVariableFactory(), randomVariableProps);

		// Generate independent variables (quantities w.r.t. to which we like to differentiate)
		RandomVariableDifferentiableInterface initialValue	= randomVariableFactory.createRandomVariable(modelInitialValue);
		RandomVariableDifferentiableInterface riskFreeRate	= randomVariableFactory.createRandomVariable(modelRiskFreeRate);
		RandomVariableDifferentiableInterface volatility	= randomVariableFactory.createRandomVariable(modelVolatility);

		// Create a model
		AbstractModel model = new BlackScholesModel(initialValue, riskFreeRate, volatility, randomVariableFactory);

		// Create a time discretization
		TimeDiscretizationInterface timeDiscretization = new TimeDiscretization(0.0 /* initial */, numberOfTimeSteps, deltaT);

		// Create a corresponding MC process
		AbstractProcess process = new ProcessEulerScheme(new BrownianMotion(timeDiscretization, 1 /* numberOfFactors */, numberOfPaths, seed));

		// Using the process (Euler scheme), create an MC simulation of a Black-Scholes model
		AssetModelMonteCarloSimulationInterface monteCarloBlackScholesModel = new MonteCarloAssetModel(model, process);

		DigitalOption option = new DigitalOption(optionMaturity, optionStrike);
		RandomVariableInterface value = option.getValue(0.0, monteCarloBlackScholesModel);

		/*
		 * Calculate sensitivities using AAD
		 */
		Map<Long, RandomVariableInterface> derivative = ((RandomVariableDifferentiableInterface)value).getGradient();
		RandomVariableInterface deltaAAD = derivative.get(initialValue.getID());

		results.put("delta.aad", deltaAAD);


		/*
		 * Calculate sensitivities using AAD with Regression
		 */

		/*
		 * Calculate A from
		 * Fries, Christian P.: Stochastic Algorithmic Differentiation of (Expectations of) Discontinuous Functions (Indicator Functions).
		 * https://ssrn.com/abstract=3282667
		 */

		Map<String, Object> randomVariablePropsZeroWidth = new HashMap<String, Object>();
		randomVariablePropsZeroWidth.put("barrierDiracWidth", 0.0);
		RandomVariableDifferentiableAADFactory randomVariableFactoryZeroWidth = new RandomVariableDifferentiableAADFactory(new RandomVariableFactory(), randomVariablePropsZeroWidth);

		Map<String, Object> randomVariablePropsInftyWidth = new HashMap<String, Object>();
		randomVariablePropsInftyWidth.put("barrierDiracWidth", Double.POSITIVE_INFINITY);
		RandomVariableDifferentiableAADFactory randomVariableFactoryInftyWidth = new RandomVariableDifferentiableAADFactory(new RandomVariableFactory(), randomVariablePropsInftyWidth);

		RandomVariableDifferentiableInterface initialValueZeroWidth = randomVariableFactoryZeroWidth.createRandomVariable(modelInitialValue);
		RandomVariableDifferentiableInterface initialValueInftyWidth = randomVariableFactoryInftyWidth.createRandomVariable(modelInitialValue);

		AssetModelMonteCarloSimulationInterface monteCarloBlackScholesModelZeroWidth = new MonteCarloAssetModel(
				new BlackScholesModel(initialValueZeroWidth, randomVariableFactoryZeroWidth.createRandomVariableNonDifferentiable(0.0, modelRiskFreeRate), randomVariableFactoryZeroWidth.createRandomVariableNonDifferentiable(0.0, modelVolatility), randomVariableFactoryZeroWidth),
				new ProcessEulerScheme(new BrownianMotion(timeDiscretization, 1 /* numberOfFactors */, numberOfPaths, seed)));

		AssetModelMonteCarloSimulationInterface monteCarloBlackScholesModelInftyWidth = new MonteCarloAssetModel(
				new BlackScholesModel(initialValueInftyWidth, riskFreeRate, volatility, randomVariableFactoryInftyWidth),
				new ProcessEulerScheme(new BrownianMotion(timeDiscretization, 1 /* numberOfFactors */, numberOfPaths, seed)));

		RandomVariableInterface A0 = ((RandomVariableDifferentiableInterface)option.getValue(0.0, monteCarloBlackScholesModelZeroWidth)).getGradient().get(initialValueZeroWidth.getID());
		RandomVariableInterface A1 = ((RandomVariableDifferentiableInterface)option.getValue(0.0, monteCarloBlackScholesModelInftyWidth)).getGradient().get(initialValueInftyWidth.getID());

		RandomVariableInterface A = A1.sub(A0);

		/*
		 * Density regression
		 */
		RandomVariableInterface X = monteCarloBlackScholesModel.getAssetValue(optionMaturity, assetIndex).sub(optionStrike);

		double underlyingStdDev = X.getStandardDeviation();
		ArrayList<Double> maskX = new ArrayList<Double>();
		ArrayList<Double> maskY = new ArrayList<Double>();
		for(double maskSizeFactor = -0.5; maskSizeFactor<0.5; maskSizeFactor+=0.01) {
			double maskSize2 = maskSizeFactor * underlyingStdDev;
			if(Math.abs(maskSizeFactor) < 1E-10) continue;
			RandomVariableInterface maskPos = X.add(Math.max(maskSize2,0)).choose(new Scalar(1.0), new Scalar(0.0));
			RandomVariableInterface maskNeg = X.add(Math.min(maskSize2,0)).choose(new Scalar(0.0), new Scalar(1.0));
			RandomVariableInterface mask2 = maskPos.mult(maskNeg);
			double density = mask2.getAverage() / Math.abs(maskSize2);
			maskX.add(maskSize2);
			maskY.add(density);
		}
		RandomVariableInterface densityX = new RandomVariable(0.0, ArrayUtils.toPrimitive(maskX.toArray(new Double[0])));
		RandomVariableInterface densityValues = new RandomVariable(0.0, ArrayUtils.toPrimitive(maskY.toArray(new Double[0])));

		double[] densityRegressionCoeff = new LinearRegression(new RandomVariableInterface[] { densityX.mult(0.0).add(1.0), densityX }).getRegressionCoefficients(densityValues);
		double density = densityRegressionCoeff[0];

		RandomVariableInterface densityRegression = densityValues.mult(0.0).add(densityRegressionCoeff[0]);
		for(int i=1; i<densityRegressionCoeff.length; i++) {
			densityRegression = densityRegression.add(densityX.pow(i).mult(densityRegressionCoeff[i]));
		}
		results.put("density", density);
		results.put("density.x", densityX);
		results.put("density.values", densityValues);
		results.put("density.regression", densityRegression);

		/*
		 * Linear regression of A
		 */
		double alphaLinear = (X.squared().getAverage() * A.getAverage() - X.getAverage() * X.mult(A).getAverage()) / (X.squared().getAverage() - X.average().squared().doubleValue());

		/*
		 * Non-linear regression of A
		 */
		double maskSize = randomVariableFactory.getBarrierDiracWidth()*X.getStandardDeviation();
		RandomVariableInterface mask = X.add(maskSize/2.0).choose(new Scalar(1.0), new Scalar(0.0));
		mask = mask.mult(X.sub(maskSize/2.0).choose(new Scalar(0.0), new Scalar(1.0)));

		A = A.mult(mask);
		X = X.mult(mask);

		double[] adjointDerivativeRegressionCoeff = new LinearRegression(new RandomVariableInterface[] { mask, X }).getRegressionCoefficients(A);

		double alphaNonLinear = adjointDerivativeRegressionCoeff[0] * density;

		RandomVariableInterface deltaAADReg = A0.add(alphaNonLinear);

		RandomVariableInterface sensitivityRegression = new Scalar(adjointDerivativeRegressionCoeff[0]);
		RandomVariableInterface densityRegressionOnX = new Scalar(densityRegressionCoeff[0]);
		for(int i=1; i<adjointDerivativeRegressionCoeff.length; i++) {
			sensitivityRegression = sensitivityRegression.add(X.pow(i).mult(adjointDerivativeRegressionCoeff[i]));
			densityRegressionOnX = densityRegressionOnX.add(X.pow(i).mult(densityRegressionCoeff[i]));
		}

		results.put("delta.aadreg", deltaAADReg);
		results.put("delta.aadreg.regression.x", X);
		results.put("delta.aadreg.regression.sensitivity", sensitivityRegression);
		results.put("delta.aadreg.regression.density", densityRegressionOnX);

		/*
		 * Analytic
		 */
		double deltaAnalytic = AnalyticFormulas.blackScholesDigitalOptionDelta(modelInitialValue, modelRiskFreeRate, modelVolatility, optionMaturity, optionStrike);
		results.put("delta.analytic", deltaAnalytic);

		/*
		 * Finite Difference
		 */

		double epsilon = randomVariableFactory.getBarrierDiracWidth()*X.getStandardDeviation();
		Map<String, Object> shiftedValues = new HashMap<String, Object>();
		shiftedValues.put("initialValue", modelInitialValue+epsilon/2.0);
		RandomVariableInterface valueUp = option.getValue(0.0, monteCarloBlackScholesModel.getCloneWithModifiedData(shiftedValues));
		shiftedValues.put("initialValue", modelInitialValue-epsilon/2.0);
		RandomVariableInterface valueDn = option.getValue(0.0, monteCarloBlackScholesModel.getCloneWithModifiedData(shiftedValues));
		RandomVariableInterface deltaFD = valueUp.sub(valueDn).div(epsilon);

		results.put("delta.fd", deltaFD);

		/*
		 * Likelihood ratio
		 */

		DigitalOptionDeltaLikelihood digitalOption = new DigitalOptionDeltaLikelihood(optionMaturity, optionStrike);
		RandomVariableInterface deltaLikelihoodRatio = new Scalar(digitalOption.getValue(monteCarloBlackScholesModel));

		results.put("delta.likelihood", deltaLikelihoodRatio);


		return results;
	}
}
