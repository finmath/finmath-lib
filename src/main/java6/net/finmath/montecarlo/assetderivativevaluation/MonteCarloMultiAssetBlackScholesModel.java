/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.06.2014
 */
package net.finmath.montecarlo.assetderivativevaluation;

import java.util.Arrays;
import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.functions.LinearAlgebra;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.BrownianMotionInterface;
import net.finmath.montecarlo.model.AbstractModel;
import net.finmath.montecarlo.process.AbstractProcess;
import net.finmath.montecarlo.process.ProcessEulerScheme;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * This class glues together a <code>BlackScholeModel</code> and a Monte-Carlo implementation of a <code>AbstractProcess</code>
 * and forms a Monte-Carlo implementation of the Black-Scholes Model by implementing <code>AssetModelMonteCarloSimulationInterface</code>.
 *
 * The model is
 * \[
 * 	dS_{i} = r S_{i} dt + \sigma_{i} S_{i} dW_{i}, \quad S_{i}(0) = S_{i,0},
 * \]
 * \[
 * 	dN = r N dt, \quad N(0) = N_{0},
 * \]
 * \[
 * 	dW_{i} dW_{j} = \rho_{i,j} dt,
 * \]
 *
 * The class provides the model of \( S_{i} \) to an <code>{@link net.finmath.montecarlo.process.AbstractProcessInterface}</code> via the specification of
 * \( f = exp \), \( \mu_{i} = r - \frac{1}{2} \sigma_{i}^2 \), \( \lambda_{i,j} = \sigma_{i} g_{i,j} \), i.e.,
 * of the SDE
 * \[
 * 	dX_{i} = \mu_{i} dt + \lambda_{i,j} dW, \quad X_{i}(0) = \log(S_{i,0}),
 * \]
 * with \( S = f(X) \). See {@link net.finmath.montecarlo.process.AbstractProcessInterface} for the notation.
 *
 * @author Christian Fries
 * @see net.finmath.montecarlo.process.AbstractProcessInterface The interface for numerical schemes.
 * @see net.finmath.montecarlo.model.AbstractModelInterface The interface for models provinding parameters to numerical schemes.
 */
public class MonteCarloMultiAssetBlackScholesModel extends AbstractModel implements AssetModelMonteCarloSimulationInterface {

	private final double[]		initialValues;
	private final double		riskFreeRate;		// Actually the same as the drift (which is not stochastic)
	private final double[]		volatilities;
	private final double[][]	factorLoadings;

	private static final int seed = 3141;

	private final RandomVariableInterface[]		initialStates;
	private final RandomVariableInterface[]		drift;
	private final RandomVariableInterface[][]	factorLoadingOnPaths;

	/**
	 * Create a Monte-Carlo simulation using given time discretization.
	 *
	 * @param brownianMotion The Brownian motion to be used for the numerical scheme.
	 * @param initialValues Spot values.
	 * @param riskFreeRate The risk free rate.
	 * @param volatilities The log volatilities.
	 * @param correlations A correlation matrix.
	 */
	public MonteCarloMultiAssetBlackScholesModel(
			BrownianMotionInterface brownianMotion,
			double[]	initialValues,
			double		riskFreeRate,
			double[]	volatilities,
			double[][]	correlations
			) {
		super();

		// Create a corresponding MC process
		AbstractProcess process = new ProcessEulerScheme(brownianMotion);

		this.initialValues	= initialValues;
		this.riskFreeRate	= riskFreeRate;
		this.volatilities	= volatilities;
		this.factorLoadings = LinearAlgebra.getFactorMatrix(correlations, correlations.length);


		/*
		 * The interface definition requires that we provide the initial value, the drift and the volatility in terms of random variables.
		 * We construct the corresponding random variables here and will return (immutable) references to them.
		 *
		 * Since the underlying process is configured to simulate log(S),
		 * the initial value and the drift are transformed accordingly.
		 *
		 */
		initialStates = new RandomVariableInterface[getNumberOfComponents()];
		drift = new RandomVariableInterface[getNumberOfComponents()];
		factorLoadingOnPaths = new RandomVariableInterface[getNumberOfComponents()][];
		for(int underlyingIndex = 0; underlyingIndex<initialValues.length; underlyingIndex++) {
			this.initialStates[underlyingIndex]				= process.getStochasticDriver().getRandomVariableForConstant(Math.log(initialValues[underlyingIndex]));
			this.drift[underlyingIndex]						= process.getStochasticDriver().getRandomVariableForConstant(riskFreeRate - volatilities[underlyingIndex] * volatilities[underlyingIndex] / 2.0);
			this.factorLoadingOnPaths[underlyingIndex]		= new RandomVariableInterface[process.getNumberOfFactors()];
			for(int factorIndex = 0; factorIndex<process.getNumberOfFactors(); factorIndex++) {
				this.factorLoadingOnPaths[underlyingIndex][factorIndex]	= process.getStochasticDriver().getRandomVariableForConstant(volatilities[underlyingIndex] * factorLoadings[underlyingIndex][factorIndex]);
			}
		}

		// Link model and process for delegation
		process.setModel(this);
		this.setProcess(process);
	}

	/**
	 * Create a Monte-Carlo simulation using given time discretization.
	 *
	 * @param timeDiscretization The time discretization.
	 * @param numberOfPaths The number of Monte-Carlo path to be used.
	 * @param initialValues Spot values.
	 * @param riskFreeRate The risk free rate.
	 * @param volatilities The log volatilities.
	 * @param correlations A correlation matrix.
	 */
	public MonteCarloMultiAssetBlackScholesModel(
			TimeDiscretizationInterface timeDiscretization,
			int numberOfPaths,
			double[]	initialValues,
			double		riskFreeRate,
			double[]	volatilities,
			double[][]	correlations
			) {
		this(new BrownianMotion(timeDiscretization, initialValues.length /* numberOfFactors */, numberOfPaths, seed), initialValues, riskFreeRate, volatilities, correlations);
	}

	@Override
	public RandomVariableInterface[] getInitialState() {
		return initialStates;
	}

	@Override
	public RandomVariableInterface[] getDrift(int timeIndex, RandomVariableInterface[] realizationAtTimeIndex, RandomVariableInterface[] realizationPredictor) {
		return drift;
	}

	@Override
	public RandomVariableInterface[] getFactorLoading(int timeIndex, int component, RandomVariableInterface[] realizationAtTimeIndex) {
		return factorLoadingOnPaths[component];
	}

	@Override
	public RandomVariableInterface applyStateSpaceTransform(int componentIndex, RandomVariableInterface randomVariable) {
		return randomVariable.exp();
	}

	@Override
	public RandomVariableInterface applyStateSpaceTransformInverse(int componentIndex, RandomVariableInterface randomVariable) {
		return randomVariable.log();
	}

	@Override
	public RandomVariableInterface getAssetValue(double time, int assetIndex) throws CalculationException {
		int timeIndex = getTimeIndex(time);
		if(timeIndex < 0) {
			timeIndex = -timeIndex-1;
		}
		return getAssetValue(timeIndex, assetIndex);
	}

	@Override
	public RandomVariableInterface getAssetValue(int timeIndex, int assetIndex) throws CalculationException {
		return getProcessValue(timeIndex, assetIndex);
	}

	@Override
	public RandomVariableInterface getMonteCarloWeights(double time) throws CalculationException {
		return getMonteCarloWeights(getTimeIndex(time));
	}

	@Override
	public RandomVariableInterface getNumeraire(int timeIndex) {
		double time = getTime(timeIndex);

		return getNumeraire(time);
	}

	@Override
	public RandomVariableInterface getNumeraire(double time) {
		double numeraireValue = Math.exp(riskFreeRate * time);

		return getRandomVariableForConstant(numeraireValue);
	}

	@Override
	public RandomVariableInterface getRandomVariableForConstant(double value) {
		return getProcess().getStochasticDriver().getRandomVariableForConstant(value);
	}

	@Override
	public int getNumberOfComponents() {
		return initialValues.length;
	}

	@Override
	public int getNumberOfAssets() {
		return getNumberOfComponents();
	}

	@Override
	public String toString() {
		return "MonteCarloMultiAssetBlackScholesModel [initialValues="
				+ Arrays.toString(initialValues) + ", riskFreeRate="
				+ riskFreeRate + ", volatilities="
				+ Arrays.toString(volatilities) + ", factorLoadings="
				+ Arrays.toString(factorLoadings) + "]";
	}

	/**
	 * Returns the risk free rate parameter of this model.
	 *
	 * @return Returns the riskFreeRate.
	 */
	public double getRiskFreeRate() {
		return riskFreeRate;
	}

	/**
	 * Returns the volatility parameters of this model.
	 *
	 * @return Returns the volatilities.
	 */
	public double[] getVolatilities() {
		return volatilities;
	}

	/**
	 * @return The number of paths.
	 * @see net.finmath.montecarlo.process.AbstractProcess#getNumberOfPaths()
	 */
	@Override
	public int getNumberOfPaths() {
		return getProcess().getNumberOfPaths();
	}

	@Override
	public MonteCarloMultiAssetBlackScholesModel getCloneWithModifiedData(Map<String, Object> dataModified) throws CalculationException {

		double[]	newInitialValues = initialValues;
		double		newRiskFreeRate = riskFreeRate;
		double[]	newVolatilities = volatilities;
		double[][]	newCorrelations = null;// = correlations;

		if(dataModified.containsKey("initialValues")) {
			newInitialValues = (double[]) dataModified.get("initialValues");
		}
		if(dataModified.containsKey("riskFreeRate")) {
			newRiskFreeRate = ((Double)dataModified.get("riskFreeRate")).doubleValue();
		}
		if(dataModified.containsKey("volatilities")) {
			newVolatilities = (double[]) dataModified.get("volatilities");
		}
		if(dataModified.containsKey("correlations")) {
			newCorrelations = (double[][]) dataModified.get("correlations");
		}

		return new MonteCarloMultiAssetBlackScholesModel(getTimeDiscretization(), getNumberOfPaths(), newInitialValues, newRiskFreeRate, newVolatilities, newCorrelations);
	}

	@Override
	public AssetModelMonteCarloSimulationInterface getCloneWithModifiedSeed(int seed) {
		// TODO Auto-generated method stub
		return null;
	}
}
