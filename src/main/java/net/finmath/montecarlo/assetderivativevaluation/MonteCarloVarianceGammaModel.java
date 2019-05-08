package net.finmath.montecarlo.assetderivativevaluation;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.VarianceGammaProcess;
import net.finmath.montecarlo.assetderivativevaluation.models.VarianceGammaModel;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.montecarlo.process.MonteCarloProcessFromProcessModel;
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
			TimeDiscretization timeDiscretization,
			int numberOfPaths,
			int seed,
			double initialValue,
			double riskFreeRate,
			double sigma,
			double theta,
			double nu) {
		super();

		this.initialValue = initialValue;
		this.seed = seed;

		//Create the model
		model = new VarianceGammaModel(initialValue,riskFreeRate,sigma,theta,nu);

		//Create a corresponding MC process
		MonteCarloProcessFromProcessModel process = new EulerSchemeFromProcessModel(new VarianceGammaProcess(sigma, nu, theta,
				timeDiscretization, 1, numberOfPaths,seed));

		//Link model and process for delegation
		process.setModel(model);
		model.setProcess(process);
	}

	@Override
	public LocalDateTime getReferenceDate() {
		throw new UnsupportedOperationException("This model does not provide a reference date. Reference dates will be mandatory in a future version.");
	}

	@Override
	public RandomVariable getAssetValue(double time, int assetIndex) throws CalculationException {
		return getAssetValue(getTimeIndex(time), assetIndex);
	}

	@Override
	public RandomVariable getAssetValue(int timeIndex, int assetIndex) throws CalculationException {
		return model.getProcess().getProcessValue(timeIndex, assetIndex);
	}

	@Override
	public RandomVariable getNumeraire(int timeIndex) throws CalculationException {
		double time = getTime(timeIndex);

		return model.getNumeraire(time);
	}

	@Override
	public RandomVariable getNumeraire(double time) throws CalculationException {
		return model.getNumeraire(time);
	}

	@Override
	public RandomVariable getMonteCarloWeights(double time) throws CalculationException {
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
	public AssetModelMonteCarloSimulationModel getCloneWithModifiedData(Map<String, Object> dataModified) {
		/*
		 * Determine the new model parameters from the provided parameter map.
		 */
		double	newInitialTime	= dataModified.get("initialTime") != null	? ((Number)dataModified.get("initialTime")).doubleValue() : getTime(0);
		double	newInitialValue	= dataModified.get("initialValue") != null	? ((Number)dataModified.get("initialValue")).doubleValue() : initialValue;
		double	newRiskFreeRate	= dataModified.get("riskFreeRate") != null	? ((Number)dataModified.get("riskFreeRate")).doubleValue() : model.getRiskFreeRate();
		double	newSigma	= dataModified.get("sigma") != null	? ((Number)dataModified.get("sigma")).doubleValue()	: model.getSigma();
		double	newTheta	= dataModified.get("theta") != null	? ((Number)dataModified.get("theta")).doubleValue()	: model.getTheta();
		double	newNu	= dataModified.get("nu") != null	? ((Number)dataModified.get("nu")).doubleValue()	: model.getNu();
		int		newSeed				= dataModified.get("seed") != null			? ((Number)dataModified.get("seed")).intValue()				: seed;

		return new MonteCarloVarianceGammaModel(model.getProcess().getTimeDiscretization().getTimeShiftedTimeDiscretization(newInitialTime-getTime(0)), model.getProcess().getNumberOfPaths(), newSeed, newInitialValue, newRiskFreeRate, newSigma, newTheta, newNu);
	}

	@Override
	public AssetModelMonteCarloSimulationModel getCloneWithModifiedSeed(int seed) {
		Map<String, Object> dataModified = new HashMap<>();
		dataModified.put("seed", new Integer(seed));
		return getCloneWithModifiedData(dataModified);
	}

	@Override
	public int getNumberOfPaths() {
		return model.getProcess().getNumberOfPaths();
	}

	@Override
	public TimeDiscretization getTimeDiscretization() {
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
	public RandomVariable getRandomVariableForConstant(double value) {
		return model.getRandomVariableForConstant(value);
	}

	@Override
	public RandomVariable getMonteCarloWeights(int timeIndex) throws CalculationException {
		return model.getProcess().getMonteCarloWeights(timeIndex);
	}
}
