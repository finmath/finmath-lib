/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.01.2004
 */
package net.finmath.montecarlo.assetderivativevaluation;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.IndependentIncrements;
import net.finmath.montecarlo.MertonJumpProcess;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.assetderivativevaluation.models.MertonModel;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel.Scheme;
import net.finmath.montecarlo.process.MonteCarloProcess;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;

/**
 * This class glues together a <code>MertonModel</code> and a Monte-Carlo implementation of a <code>MonteCarloProcessFromProcessModel</code>, namely <code>EulerSchemeFromProcessModel</code>,
 * and forms a Monte-Carlo implementation of the Merton model by implementing <code>AssetModelMonteCarloSimulationModel</code>.
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
 * For details on the construction of the model see {@link net.finmath.montecarlo.assetderivativevaluation.models.MertonModel}.
 *
 * @author Christian Fries
 * @see net.finmath.montecarlo.assetderivativevaluation.models.MertonModel
 * @see net.finmath.montecarlo.process.MonteCarloProcess The interface for numerical schemes.
 * @see net.finmath.montecarlo.model.ProcessModel The interface for models provinding parameters to numerical schemes.
 * @version 1.0
 */
public class MonteCarloMertonModel implements AssetModelMonteCarloSimulationModel {

	private final MertonModel model;
	private final MonteCarloProcess process;

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
	 * @param randomVariableFactory The factory to be used to construct random variables.
	 */
	public MonteCarloMertonModel(
			final TimeDiscretization timeDiscretization,
			final int numberOfPaths,
			final int seed,
			final double initialValue,
			final double riskFreeRate,
			final double volatility,
			final double jumpIntensity,
			final double jumpSizeMean,
			final double jumpSizeStDev,
			final RandomVariableFactory randomVariableFactory
			) {
		super();

		this.initialValue = initialValue;
		this.seed = seed;

		final double discountRate = riskFreeRate;

		// Create the model
		model = new MertonModel(initialValue, riskFreeRate, volatility, discountRate, jumpIntensity, jumpSizeMean, jumpSizeStDev, randomVariableFactory);

		// Create the Compound Poisson process
		final IndependentIncrements icrements = new MertonJumpProcess(jumpIntensity, jumpSizeMean, jumpSizeStDev,timeDiscretization,numberOfPaths, seed);

		// Create a corresponding MC process
		process = new EulerSchemeFromProcessModel(model, icrements, Scheme.EULER_FUNCTIONAL);
	}

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
			final TimeDiscretization timeDiscretization,
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

		// Create the Compound Poisson process
		final IndependentIncrements icrements = new MertonJumpProcess(jumpIntensity, jumpSizeMean, jumpSizeStDev,timeDiscretization,numberOfPaths, seed);

		// Create a corresponding MC process
		process = new EulerSchemeFromProcessModel(model, icrements, Scheme.EULER_FUNCTIONAL);
	}

	@Override
	public LocalDateTime getReferenceDate() {
		throw new UnsupportedOperationException("This model does not provide a reference date. Reference dates will be mandatory in a future version.");
	}

	@Override
	public RandomVariable getAssetValue(final double time, final int assetIndex) throws CalculationException {
		final int timeIndex = getTimeIndex(time);
		if(timeIndex < 0) {
			throw new IllegalArgumentException("The model does not provide an interpolation of simulation time (time given was " + time + ").");
		}

		return getAssetValue(timeIndex, assetIndex);
	}

	@Override
	public RandomVariable getAssetValue(final int timeIndex, final int assetIndex) throws CalculationException {
		return process.getProcessValue(timeIndex, assetIndex);
	}

	@Override
	public RandomVariable getNumeraire(final int timeIndex) throws CalculationException {
		final double time = getTime(timeIndex);

		return model.getNumeraire(process, time);
	}

	@Override
	public RandomVariable getNumeraire(final double time) throws CalculationException {
		return model.getNumeraire(process, time);
	}

	@Override
	public RandomVariable getMonteCarloWeights(final double time) throws CalculationException {
		return getMonteCarloWeights(getTimeIndex(time));
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel#getNumberOfAssets()
	 */
	@Override
	public int getNumberOfAssets() {
		return 1;
	}

	@Override
	public AssetModelMonteCarloSimulationModel getCloneWithModifiedData(final Map<String, Object> dataModified) {
		/*
		 * Determine the new model parameters from the provided parameter map.
		 */
		final double	newInitialTime	= dataModified.get("initialTime") != null	? ((Number)dataModified.get("initialTime")).doubleValue() : getTime(0);
		final double	newInitialValue	= dataModified.get("initialValue") != null	? ((Number)dataModified.get("initialValue")).doubleValue() : initialValue;
		final double	newRiskFreeRate	= dataModified.get("riskFreeRate") != null	? ((Number)dataModified.get("riskFreeRate")).doubleValue() : model.getRiskFreeRate().doubleValue();
		final double	newVolatility	= dataModified.get("volatility") != null	? ((Number)dataModified.get("volatility")).doubleValue()	: model.getVolatility().doubleValue();
		final double	newJumpIntensity	= dataModified.get("jumpIntensity") != null	? ((Number)dataModified.get("jumpIntensity")).doubleValue()	: model.getJumpIntensity().doubleValue();
		final double	newJumpSizeMean		= dataModified.get("jumpSizeMean") != null	? ((Number)dataModified.get("jumpSizeMean")).doubleValue()	: model.getVolatility().doubleValue();
		final double	newJumpSizeStdDev	= dataModified.get("jumpSizeStdDev") != null	? ((Number)dataModified.get("jumpSizeStdDev")).doubleValue()	: model.getVolatility().doubleValue();
		final int		newSeed				= dataModified.get("seed") != null			? ((Number)dataModified.get("seed")).intValue()				: seed;

		return new MonteCarloMertonModel(process.getTimeDiscretization().getTimeShiftedTimeDiscretization(newInitialTime-getTime(0)), process.getNumberOfPaths(), newSeed, newInitialValue, newRiskFreeRate, newVolatility, newJumpIntensity, newJumpSizeMean, newJumpSizeStdDev);

	}

	@Override
	public AssetModelMonteCarloSimulationModel getCloneWithModifiedSeed(final int seed) {
		final Map<String, Object> dataModified = new HashMap<>();
		dataModified.put("seed", new Integer(seed));
		return getCloneWithModifiedData(dataModified);
	}

	@Override
	public int getNumberOfPaths() {
		return process.getNumberOfPaths();
	}

	@Override
	public TimeDiscretization getTimeDiscretization() {
		return process.getTimeDiscretization();
	}

	@Override
	public double getTime(final int timeIndex) {
		return process.getTime(timeIndex);
	}

	@Override
	public int getTimeIndex(final double time) {
		return process.getTimeIndex(time);
	}

	@Override
	public RandomVariable getRandomVariableForConstant(final double value) {
		return model.getRandomVariableForConstant(value);
	}

	@Override
	public RandomVariable getMonteCarloWeights(final int timeIndex) throws CalculationException {
		return process.getMonteCarloWeights(timeIndex);
	}
}
