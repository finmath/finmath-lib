/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.01.2004
 */
package net.finmath.montecarlo.assetderivativevaluation;

import java.util.HashMap;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntFunction;

import net.finmath.exception.CalculationException;
import net.finmath.functions.NormalDistribution;
import net.finmath.functions.PoissonDistribution;
import net.finmath.montecarlo.IndependentIncrements;
import net.finmath.montecarlo.IndependentIncrementsInterface;
import net.finmath.montecarlo.process.AbstractProcess;
import net.finmath.montecarlo.process.ProcessEulerScheme;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * This class glues together a <code>MertonModel</code> and a Monte-Carlo implementation of a <code>AbstractProcess</code>, namely <code>ProcessEulerScheme</code>,
 * and forms a Monte-Carlo implementation of the Merton model by implementing <code>AssetModelMonteCarloSimulationInterface</code>.
 *
 * The model is
 * \[
 * 	dS = \mu S dt + \sigma S dW + S dJ, \quad S(0) = S_{0},
 * \]
 * \[
 * 	dN = r N dt, \quad N(0) = N_{0},
 * \]
 * where \( W \) is Brownian motion and \( J \)  is a jump process (compound Poisson process).
 *
 * The process \( J \) is given by \( J(t) = \sum_{i=1}^{N(t)} (Y_{i}-1) \), where
 * \( \log(Y_{i}) \) are i.i.d. normals with mean \( a - \frac{1}{2} b^{2} \) and standard deviation \( b \).
 * Here \( a \) is the jump size mean and \( b \) is the jump size std. dev.
 *
 * For details on the construction of the model see {@link net.finmath.montecarlo.assetderivativevaluation.MertonModel}.
 *
 * @author Christian Fries
 * @see net.finmath.montecarlo.assetderivativevaluation.MertonModel
 * @see net.finmath.montecarlo.process.AbstractProcessInterface The interface for numerical schemes.
 * @see net.finmath.montecarlo.model.AbstractModelInterface The interface for models provinding parameters to numerical schemes.
 */
public class MonteCarloMertonModel implements AssetModelMonteCarloSimulationInterface {

	private final MertonModel model;
	private final double initialValue;
	private final int seed;

	/**
	 * Create a Monte-Carlo simulation using given time discretization and given parameters.
	 *
	 * @param timeDiscretization The time discretization.
	 * @param numberOfPaths The number of Monte-Carlo path to be used.
	 * @param seed The seed used for the random number generator.
	 * @param initialValue Spot value.
	 * @param riskFreeRate The risk free rate.
	 * @param volatility The log volatility.
	 * @param jumpIntensity The intensity parameter lambda of the compound Poisson process.
	 * @param jumpSizeMean The mean jump size of the normal distributes jump sizes of the compound Poisson process.
	 * @param jumpSizeStDev The standard deviation of the normal distributes jump sizes of the compound Poisson process.
	 */
	public MonteCarloMertonModel(
			final TimeDiscretizationInterface timeDiscretization,
			final int numberOfPaths,
			final int seed,
			final double initialValue,
			final double riskFreeRate,
			final double volatility,
			final double jumpIntensity,
			final double jumpSizeMean,
			final double jumpSizeStDev
			) {
		super();

		this.initialValue = initialValue;
		this.seed = seed;

		// Create the model
		model = new MertonModel(initialValue, riskFreeRate, volatility, jumpIntensity, jumpSizeMean, jumpSizeStDev);

		IntFunction<IntFunction<DoubleUnaryOperator>> inverseCumulativeDistributionFunctions = i -> j -> {
			if(j==0) {
				// The Brownian increment
				double sqrtOfTimeStep = Math.sqrt(timeDiscretization.getTimeStep(i));
				return x -> NormalDistribution.inverseCumulativeDistribution(x)*sqrtOfTimeStep;
			}
			else if(j==1) {
				// The random jump size
				return x -> NormalDistribution.inverseCumulativeDistribution(x);
			}
			else if(j==2) {
				// The jump increment
				double timeStep = timeDiscretization.getTimeStep(i);
				PoissonDistribution poissonDistribution = new PoissonDistribution(jumpIntensity*timeStep);
				return x -> poissonDistribution.inverseCumulativeDistribution(x);
			}
			else {
				return null;
			}
		};

		IndependentIncrementsInterface icrements = new IndependentIncrements(timeDiscretization, 3, numberOfPaths, seed, inverseCumulativeDistributionFunctions ) {
			private static final long serialVersionUID = -7858107751226404629L;

			@Override
			public RandomVariableInterface getIncrement(int timeIndex, int factor) {
				if(factor == 1) {
					RandomVariableInterface Z = super.getIncrement(timeIndex, 1);
					RandomVariableInterface N = super.getIncrement(timeIndex, 2);
					return Z.mult(N.sqrt());
				}
				else {
					return super.getIncrement(timeIndex, factor);
				}
			}
		};

		// Create a corresponding MC process
		AbstractProcess process = new ProcessEulerScheme(icrements);

		// Link model and process for delegation
		process.setModel(model);
		model.setProcess(process);
	}

	@Override
	public RandomVariableInterface getAssetValue(double time, int assetIndex) throws CalculationException {
		return getAssetValue(getTimeIndex(time), assetIndex);
	}

	@Override
	public RandomVariableInterface getAssetValue(int timeIndex, int assetIndex) throws CalculationException {
		return model.getProcess().getProcessValue(timeIndex, assetIndex);
	}

	@Override
	public RandomVariableInterface getNumeraire(int timeIndex) throws CalculationException {
		double time = getTime(timeIndex);

		return model.getNumeraire(time);
	}

	@Override
	public RandomVariableInterface getNumeraire(double time) throws CalculationException {
		return model.getNumeraire(time);
	}

	@Override
	public RandomVariableInterface getMonteCarloWeights(double time) throws CalculationException {
		return getMonteCarloWeights(getTimeIndex(time));
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationInterface#getNumberOfAssets()
	 */
	@Override
	public int getNumberOfAssets() {
		return 1;
	}

	@Override
	public AssetModelMonteCarloSimulationInterface getCloneWithModifiedData(Map<String, Object> dataModified) {
		/*
		 * Determine the new model parameters from the provided parameter map.
		 */
		double	newInitialTime	= dataModified.get("initialTime") != null	? ((Number)dataModified.get("initialTime")).doubleValue() : getTime(0);
		double	newInitialValue	= dataModified.get("initialValue") != null	? ((Number)dataModified.get("initialValue")).doubleValue() : initialValue;
		double	newRiskFreeRate	= dataModified.get("riskFreeRate") != null	? ((Number)dataModified.get("riskFreeRate")).doubleValue() : model.getRiskFreeRate();
		double	newVolatility	= dataModified.get("volatility") != null	? ((Number)dataModified.get("volatility")).doubleValue()	: model.getVolatility();
		double	newJumpIntensity	= dataModified.get("jumpIntensity") != null	? ((Number)dataModified.get("jumpIntensity")).doubleValue()	: model.getJumpIntensity();
		double	newJumpSizeMean		= dataModified.get("jumpSizeMean") != null	? ((Number)dataModified.get("jumpSizeMean")).doubleValue()	: model.getVolatility();
		double	newJumpSizeStdDev	= dataModified.get("jumpSizeStdDev") != null	? ((Number)dataModified.get("jumpSizeStdDev")).doubleValue()	: model.getVolatility();
		int		newSeed				= dataModified.get("seed") != null			? ((Number)dataModified.get("seed")).intValue()				: seed;

		return new MonteCarloMertonModel(model.getProcess().getTimeDiscretization().getTimeShiftedTimeDiscretization(newInitialTime-getTime(0)), model.getProcess().getNumberOfPaths(), newSeed, newInitialValue, newRiskFreeRate, newVolatility, newJumpIntensity, newJumpSizeMean, newJumpSizeStdDev);

	}

	@Override
	public AssetModelMonteCarloSimulationInterface getCloneWithModifiedSeed(int seed) {
		Map<String, Object> dataModified = new HashMap<>();
		dataModified.put("seed", new Integer(seed));
		return getCloneWithModifiedData(dataModified);
	}

	@Override
	public int getNumberOfPaths() {
		return model.getProcess().getNumberOfPaths();
	}

	@Override
	public TimeDiscretizationInterface getTimeDiscretization() {
		return model.getProcess().getTimeDiscretization();
	}

	@Override
	public double getTime(int timeIndex) {
		return model.getProcess().getTime(timeIndex);
	}

	@Override
	public int getTimeIndex(double time) {
		return model.getProcess().getTimeIndex(time);
	}

	@Override
	public RandomVariableInterface getRandomVariableForConstant(double value) {
		return model.getRandomVariableForConstant(value);
	}

	@Override
	public RandomVariableInterface getMonteCarloWeights(int timeIndex) throws CalculationException {
		return model.getProcess().getMonteCarloWeights(timeIndex);
	}
}

