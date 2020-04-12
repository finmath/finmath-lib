package net.finmath.montecarlo.assetderivativevaluation;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.VarianceGammaProcess;
import net.finmath.montecarlo.assetderivativevaluation.models.VarianceGammaModel;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.montecarlo.process.MonteCarloProcess;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;

/**
 * This class glues together a {@link VarianceGammaModel} and a Monte-Carlo implementation of a <code>MonteCarloProcessFromProcessModel</code>
 * and forms a Monte-Carlo implementation of the Variance Gamma Model by implementing <code>AssetModelMonteCarloSimulationModel</code>.
 *
 * @author Alessandro Gnoatto
 * @version 1.0
 */
public class MonteCarloVarianceGammaModel implements AssetModelMonteCarloSimulationModel {

	private final VarianceGammaModel model;
	private final MonteCarloProcess process;

	private final double initialValue;
	private final int seed;

	/**
	 * Create a Monte Carlo simulation using a given time discretization.
	 *
	 * @param timeDiscretization The time discretization.
	 * @param numberOfPaths The number of Monte-Carlo paths to be used.
	 * @param seed The seed used for the random number generator.
	 * @param initialValue \( S_0 \) The spot value.
	 * @param riskFreeRate The risk free rate.
	 * @param sigma The parameter \( \sigma \)
	 * @param theta The parameter \( \theta \)
	 * @param nu The parameter \( \nu \)
	 */
	public MonteCarloVarianceGammaModel(
			final TimeDiscretization timeDiscretization,
			final int numberOfPaths,
			final int seed,
			final double initialValue,
			final double riskFreeRate,
			final double sigma,
			final double theta,
			final double nu) {
		super();

		this.initialValue = initialValue;
		this.seed = seed;

		// Create the model
		model = new VarianceGammaModel(initialValue,riskFreeRate,sigma,theta,nu);

		// Create a corresponding MC process
		process = new EulerSchemeFromProcessModel(model, new VarianceGammaProcess(sigma, nu, theta,
				timeDiscretization, 1, numberOfPaths,seed));
	}

	@Override
	public LocalDateTime getReferenceDate() {
		throw new UnsupportedOperationException("This model does not provide a reference date. Reference dates will be mandatory in a future version.");
	}

	@Override
	public RandomVariable getAssetValue(final double time, final int assetIndex) throws CalculationException {
		return getAssetValue(getTimeIndex(time), assetIndex);
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
		final double	newSigma	= dataModified.get("sigma") != null	? ((Number)dataModified.get("sigma")).doubleValue()	: model.getSigma().doubleValue();
		final double	newTheta	= dataModified.get("theta") != null	? ((Number)dataModified.get("theta")).doubleValue()	: model.getTheta().doubleValue();
		final double	newNu	= dataModified.get("nu") != null	? ((Number)dataModified.get("nu")).doubleValue()	: model.getNu().doubleValue();
		final int		newSeed				= dataModified.get("seed") != null			? ((Number)dataModified.get("seed")).intValue()				: seed;

		return new MonteCarloVarianceGammaModel(process.getTimeDiscretization().getTimeShiftedTimeDiscretization(newInitialTime-getTime(0)), process.getNumberOfPaths(), newSeed, newInitialValue, newRiskFreeRate, newSigma, newTheta, newNu);
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
