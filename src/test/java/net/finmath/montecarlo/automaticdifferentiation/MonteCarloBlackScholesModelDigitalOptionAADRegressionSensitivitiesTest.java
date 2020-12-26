/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christianfries.com.
 *
 * Created on 15.06.2016
 */

package net.finmath.montecarlo.automaticdifferentiation;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Assert;
import org.junit.Test;

import net.finmath.exception.CalculationException;
import net.finmath.functions.AnalyticFormulas;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.BrownianMotionFromMersenneRandomNumbers;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFromArrayFactory;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.assetderivativevaluation.MonteCarloAssetModel;
import net.finmath.montecarlo.assetderivativevaluation.models.BlackScholesModel;
import net.finmath.montecarlo.assetderivativevaluation.products.DigitalOption;
import net.finmath.montecarlo.assetderivativevaluation.products.DigitalOptionDeltaLikelihood;
import net.finmath.montecarlo.automaticdifferentiation.backward.RandomVariableDifferentiableAADFactory;
import net.finmath.montecarlo.automaticdifferentiation.backward.RandomVariableDifferentiableAADFactory.DiracDeltaApproximationMethod;
import net.finmath.montecarlo.conditionalexpectation.LinearRegression;
import net.finmath.montecarlo.model.AbstractProcessModel;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.montecarlo.process.MonteCarloProcessFromProcessModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * This test checks several methods for calculating the delta of a digital option. Among the methods
 * is the calculation via AAD with regression,
 * see Stochastic Algorithmic Differentiation of (Expectations of) Discontinuous Functions (Indicator Functions).
 * https://ssrn.com/abstract=3282667
 *
 * The test shows that the method provides a significant improvement in the quality (in terms of variance reduction):
 * AAD is 8 time better than finite differences,
 * AAD with regression is 3 times better than AAD
 * Likelihood Ratio (the benchmark for a digital option) is only 6% better than AAD with regression.
 *
 * @author Christian Fries
 */
public class MonteCarloBlackScholesModelDigitalOptionAADRegressionSensitivitiesTest {

	private static DecimalFormat formatterReal4 = new DecimalFormat("0.0000");

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

		final int seed = 3141;
		final double width = 0.05;

		final Map<String, Object> results = getSensitivityApproximations(width, seed, true);

		final Double deltaAnalytic = (Double)results.get("delta.analytic");

		final RandomVariable deltaFD = (RandomVariable)results.get("delta.fd");
		final RandomVariable deltaAAD = (RandomVariable)results.get("delta.aad");
		final RandomVariable deltaAADRegression = (RandomVariable)results.get("delta.aad.regression");
		final RandomVariable deltaLikelihoodRatio = (RandomVariable)results.get("delta.likelihood");

		final RandomVariable deltaAADRegressionDirect = (RandomVariable)results.get("delta.aad.directregression");

		System.out.println("Digital Option Delta\n");
		System.out.println("finite difference..........: " + formatterReal4.format(deltaFD.getAverage()) + "\t" + formatterReal4.format(deltaFD.getAverage()-deltaAnalytic));
		System.out.println("algorithmic diff...........: " + formatterReal4.format(deltaAAD.getAverage()) + "\t" + formatterReal4.format(deltaAAD.getAverage()-deltaAnalytic));
		System.out.println("algo diff with regression.1: " + formatterReal4.format(deltaAADRegressionDirect.getAverage()) + "\t" + formatterReal4.format(deltaAADRegressionDirect.getAverage()-deltaAnalytic));
		System.out.println("algo diff with regression.2: " + formatterReal4.format(deltaAADRegression.getAverage()) + "\t" + formatterReal4.format(deltaAADRegression.getAverage()-deltaAnalytic));
		System.out.println("likelihood ratio...........: " + formatterReal4.format(deltaLikelihoodRatio.getAverage()) + "\t" + formatterReal4.format(deltaLikelihoodRatio.getAverage()-deltaAnalytic));
		System.out.println("analytic...................: " + formatterReal4.format(deltaAnalytic) + "\t" + formatterReal4.format(0));
		System.out.println();

		Assert.assertEquals("digital option delta finite difference", deltaAnalytic, deltaFD.getAverage(), 1E-1);
		Assert.assertEquals("digital option delta aad", deltaAnalytic, deltaAAD.getAverage(), 1E-2);
		Assert.assertEquals("digital option delta aad regression", deltaAnalytic, deltaAADRegressionDirect.getAverage(), 4E-3);
		Assert.assertEquals("digital option delta likelihood ratio", deltaAnalytic, deltaLikelihoodRatio.getAverage(), 4E-3);
	}

	/**
	 * Create a Monte-Carlo simulation of a Black-Scholes model using a specified Brownian motion
	 * and random variable factory. The random variable factory will control the use of AAD (by means of dependency injection).
	 *
	 * @param randomVariableFactory The random variable factory to be used.
	 * @param brownianMotion The Brownian motion used to drive the model.
	 * @return A Monte-Carlo simulation of a Black-Scholes model.
	 */
	public MonteCarloAssetModel getMonteCarloBlackScholesModel(final RandomVariableFactory randomVariableFactory, final BrownianMotion brownianMotion) {
		// Create a model
		final AbstractProcessModel model = new BlackScholesModel(modelInitialValue, modelRiskFreeRate, modelVolatility, randomVariableFactory);

		// Create a corresponding MC process
		final MonteCarloProcessFromProcessModel process = new EulerSchemeFromProcessModel(model, brownianMotion);

		// Using the process (Euler scheme), create an MC simulation of a Black-Scholes model
		return new MonteCarloAssetModel(process);
	}

	public Map<String, Object> getSensitivityApproximations(final double width, final int seed, final boolean isDirectRegression) throws CalculationException {
		final Map<String, Object> results = new HashMap<>();

		// Create Brownian motion with specified seed
		final TimeDiscretization timeDiscretization = new TimeDiscretizationFromArray(0.0 /* initial */, numberOfTimeSteps, deltaT);
		final BrownianMotion brownianMotion = new BrownianMotionFromMersenneRandomNumbers(timeDiscretization, 1 /* numberOfFactors */, numberOfPaths, seed);

		/*
		 * Create product
		 */
		final DigitalOption option = new DigitalOption(optionMaturity, optionStrike);

		/*
		 * Calculate sensitivities using AAD
		 */
		{
			final Map<String, Object> randomVariableProps = new HashMap<>();
			randomVariableProps.put("diracDeltaApproximationWidthPerStdDev", width);	// 0.05 is the default
			final RandomVariableDifferentiableAADFactory randomVariableFactory = new RandomVariableDifferentiableAADFactory(new RandomVariableFromArrayFactory(), randomVariableProps);

			/*
			 * Create Model
			 */
			final MonteCarloAssetModel monteCarloBlackScholesModel = getMonteCarloBlackScholesModel(randomVariableFactory, brownianMotion);


			final RandomVariable value = option.getValue(0.0, monteCarloBlackScholesModel);
			final Map<Long, RandomVariable> derivative = ((RandomVariableDifferentiable)value).getGradient();
			final RandomVariableDifferentiable initialValue = (RandomVariableDifferentiable)((BlackScholesModel)monteCarloBlackScholesModel.getModel()).getInitialValue(monteCarloBlackScholesModel.getProcess())[0];
			final RandomVariable deltaAAD = derivative.get(initialValue.getID());

			results.put("delta.aad", deltaAAD);
		}


		/*
		 * AAD with regression (using lib code)
		 */
		{
			final Map<String, Object> randomVariableRegressionProps = new HashMap<>();
			randomVariableRegressionProps.put("diracDeltaApproximationWidthPerStdDev", width);	// 0.05 is the default
			randomVariableRegressionProps.put("diracDeltaApproximationMethod", DiracDeltaApproximationMethod.REGRESSION_ON_DISTRIBUITON.name());
			randomVariableRegressionProps.put("diracDeltaApproximationDensityRegressionWidthPerStdDev", 0.75);	// 0.5 is the default
			final RandomVariableDifferentiableAADFactory randomVariableFactoryRegression = new RandomVariableDifferentiableAADFactory(new RandomVariableFromArrayFactory(), randomVariableRegressionProps);

			/*
			 * Create Model
			 */
			final MonteCarloAssetModel monteCarloBlackScholesModel = getMonteCarloBlackScholesModel(randomVariableFactoryRegression, brownianMotion);

			final RandomVariable value = option.getValue(0.0, monteCarloBlackScholesModel);
			final Map<Long, RandomVariable> derivative = ((RandomVariableDifferentiable)value).getGradient();
			final RandomVariableDifferentiable initialValue = (RandomVariableDifferentiable)((BlackScholesModel)monteCarloBlackScholesModel.getModel()).getInitialValue(monteCarloBlackScholesModel.getProcess())[0];

			final RandomVariable deltaRegression = derivative.get(initialValue.getID());

			results.put("delta.aad.regression", deltaRegression);
		}

		/*
		 * Analytic
		 */
		final double deltaAnalytic = AnalyticFormulas.blackScholesDigitalOptionDelta(modelInitialValue, modelRiskFreeRate, modelVolatility, optionMaturity, optionStrike);
		results.put("delta.analytic", deltaAnalytic);


		/*
		 * Other Methods
		 */
		final MonteCarloAssetModel monteCarloBlackScholesModel = getMonteCarloBlackScholesModel(new RandomVariableFromArrayFactory(), brownianMotion);
		final RandomVariable X = monteCarloBlackScholesModel.getAssetValue(optionMaturity, assetIndex).sub(optionStrike);

		/*
		 * Finite Difference
		 */
		{

			final double epsilon = width*X.getStandardDeviation();
			final Map<String, Object> shiftedValues = new HashMap<>();
			shiftedValues.put("initialValue", modelInitialValue+epsilon/2.0);
			final RandomVariable valueUp = option.getValue(0.0, monteCarloBlackScholesModel.getCloneWithModifiedData(shiftedValues));

			shiftedValues.put("initialValue", modelInitialValue-epsilon/2.0);
			final RandomVariable valueDn = option.getValue(0.0, monteCarloBlackScholesModel.getCloneWithModifiedData(shiftedValues));

			final RandomVariable deltaFD = valueUp.sub(valueDn).div(epsilon);

			results.put("delta.fd", deltaFD);
		}

		/*
		 * Likelihood ratio
		 */
		{
			final DigitalOptionDeltaLikelihood digitalOption = new DigitalOptionDeltaLikelihood(optionMaturity, optionStrike);
			final RandomVariable deltaLikelihoodRatio = new Scalar(digitalOption.getValue(monteCarloBlackScholesModel));

			results.put("delta.likelihood", deltaLikelihoodRatio);
		}

		/*
		 * Calculate sensitivities using AAD with regression by extracting the derivative A
		 *
		 * The following code is only for research purposes. We can explicitly extract the random variables A and X
		 * in the valuation of the derivative being E(A d/dX 1_X ) and analyse the random variables.
		 */
		if(isDirectRegression) {
			/*
			 * Calculate A from
			 */
			final Map<String, Object> randomVariablePropsZeroWidth = new HashMap<>();
			randomVariablePropsZeroWidth.put("diracDeltaApproximationWidthPerStdDev", 0.0);
			final RandomVariableDifferentiableAADFactory randomVariableFactoryZeroWidth = new RandomVariableDifferentiableAADFactory(new RandomVariableFromArrayFactory(), randomVariablePropsZeroWidth);

			final Map<String, Object> randomVariablePropsInftyWidth = new HashMap<>();
			randomVariablePropsInftyWidth.put("diracDeltaApproximationWidthPerStdDev", Double.POSITIVE_INFINITY);
			final RandomVariableDifferentiableAADFactory randomVariableFactoryInftyWidth = new RandomVariableDifferentiableAADFactory(new RandomVariableFromArrayFactory(), randomVariablePropsInftyWidth);

			final MonteCarloAssetModel monteCarloBlackScholesModelZeroWidth = getMonteCarloBlackScholesModel(randomVariableFactoryZeroWidth, brownianMotion);
			final MonteCarloAssetModel monteCarloBlackScholesModelInftyWidth = getMonteCarloBlackScholesModel(randomVariableFactoryInftyWidth, brownianMotion);

			final RandomVariableDifferentiable initialValueZeroWidth = (RandomVariableDifferentiable)((BlackScholesModel)monteCarloBlackScholesModelZeroWidth.getModel()).getInitialValue(monteCarloBlackScholesModelZeroWidth.getProcess())[0];
			final RandomVariableDifferentiable initialValueInftyWidth = (RandomVariableDifferentiable)((BlackScholesModel)monteCarloBlackScholesModelInftyWidth.getModel()).getInitialValue(monteCarloBlackScholesModelInftyWidth.getProcess())[0];

			final RandomVariable A0 = ((RandomVariableDifferentiable)option.getValue(0.0, monteCarloBlackScholesModelZeroWidth)).getGradient().get(initialValueZeroWidth.getID());
			final RandomVariable A1 = ((RandomVariableDifferentiable)option.getValue(0.0, monteCarloBlackScholesModelInftyWidth)).getGradient().get(initialValueInftyWidth.getID());

			final RandomVariable A = A1.sub(A0);

			/*
			 * Density regression
			 */
			final double underlyingStdDev = X.getStandardDeviation();
			final ArrayList<Double> maskX = new ArrayList<>();
			final ArrayList<Double> maskY = new ArrayList<>();
			for(double maskSizeFactor = -0.5; maskSizeFactor<0.505; maskSizeFactor+=0.01) {
				final double maskSize2 = maskSizeFactor * underlyingStdDev;
				if(Math.abs(maskSizeFactor) < 1E-10) {
					continue;
				}
				final RandomVariable maskPos = X.add(Math.max(maskSize2,0)).choose(new Scalar(1.0), new Scalar(0.0));
				final RandomVariable maskNeg = X.add(Math.min(maskSize2,0)).choose(new Scalar(0.0), new Scalar(1.0));
				final RandomVariable mask2 = maskPos.mult(maskNeg);
				final double density = mask2.getAverage() / Math.abs(maskSize2);
				maskX.add(maskSize2);
				maskY.add(density);
			}
			final RandomVariable densityX = new RandomVariableFromDoubleArray(0.0, ArrayUtils.toPrimitive(maskX.toArray(new Double[0])));
			final RandomVariable densityValues = new RandomVariableFromDoubleArray(0.0, ArrayUtils.toPrimitive(maskY.toArray(new Double[0])));

			final double[] densityRegressionCoeff = new LinearRegression(new RandomVariable[] { densityX.mult(0.0).add(1.0), densityX }).getRegressionCoefficients(densityValues);
			final double density = densityRegressionCoeff[0];

			RandomVariable densityRegression = densityValues.mult(0.0).add(densityRegressionCoeff[0]);
			for(int i=1; i<densityRegressionCoeff.length; i++) {
				densityRegression = densityRegression.add(densityX.pow(i).mult(densityRegressionCoeff[i]));
			}
			results.put("density", density);
			results.put("density.x", densityX);
			results.put("density.values", densityValues);
			results.put("density.regression", densityRegression);

			/*
			 * Localization
			 */
			final double derivativeLocalizationSize = width*X.getStandardDeviation();
			RandomVariable derivativeLocalizer = X.add(derivativeLocalizationSize/2.0).choose(new Scalar(1.0), new Scalar(0.0));
			derivativeLocalizer = derivativeLocalizer.mult(X.sub(derivativeLocalizationSize/2.0).choose(new Scalar(0.0), new Scalar(1.0)));

			final RandomVariable Atilde = A.mult(derivativeLocalizer);
			final RandomVariable Xtilde = X.mult(derivativeLocalizer);

			/*
			 * Linear regression of A
			 */
			final double alphaLinear = (Xtilde.squared().getAverage() * Atilde.getAverage() - Xtilde.getAverage() * Xtilde.mult(Atilde).getAverage()) / (Xtilde.squared().getAverage() - Xtilde.average().squared().doubleValue());

			/*
			 * Non-linear regression of A
			 */

			//		double[] adjointDerivativeRegressionCoeff = new LinearRegression(new RandomVariable[] { derivativeLocalizer, Xtilde }).getRegressionCoefficients(Atilde);
			final double[] adjointDerivativeRegressionCoeff = new LinearRegression(new RandomVariable[] { derivativeLocalizer }).getRegressionCoefficients(Atilde);

			final double alphaNonLinear = adjointDerivativeRegressionCoeff[0] * density;

			final RandomVariable deltaAADReg = A0.add(alphaNonLinear);

			RandomVariable sensitivityRegression = new Scalar(adjointDerivativeRegressionCoeff[0]);
			RandomVariable densityRegressionOnX = new Scalar(densityRegressionCoeff[0]);
			for(int i=1; i<adjointDerivativeRegressionCoeff.length; i++) {
				sensitivityRegression = sensitivityRegression.add(X.pow(i).mult(adjointDerivativeRegressionCoeff[i]));
				densityRegressionOnX = densityRegressionOnX.add(X.pow(i).mult(densityRegressionCoeff[i]));
			}

			results.put("delta.aad.directregression", deltaAADReg);
			results.put("delta.aad.directregression.regression.x", X);
			results.put("delta.aad.directregression.regression.sensitivity", sensitivityRegression);
			results.put("delta.aad.directregression.regression.density", densityRegressionOnX);
		}

		return results;
	}
}
