/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.01.2004
 */
package net.finmath.montecarlo.assetderivativevaluation;

import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.model.AbstractModel;
import net.finmath.montecarlo.model.AbstractModelInterface;
import net.finmath.montecarlo.process.AbstractProcessInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * This class glues together an <code>AbstractModel</code> and a Monte-Carlo implementation of a <code>AbstractProcess</code>
 * and implements <code>AssetModelMonteCarloSimulationInterface</code>.
 *
 * The model is specified via the object implementing <code>AbstractModelInterface</code>.
 *
 * @author Christian Fries
 * @see net.finmath.montecarlo.process.AbstractProcessInterface The interface for numerical schemes.
 * @see net.finmath.montecarlo.model.AbstractModelInterface The interface for models provinding parameters to numerical schemes.
 * @version 1.0
 */
public class MonteCarloAssetModel implements AssetModelMonteCarloSimulationInterface {

	private final AbstractModelInterface model;

	/**
	 * Create a Monte-Carlo simulation using given process discretization scheme.
	 *
	 * @param model The model to be used.
	 * @param process The numerical scheme to be used.
	 */
	public MonteCarloAssetModel(
			AbstractModelInterface model,
			AbstractProcessInterface process) {
		super();

		this.model = model;

		// Link model and process for delegation
		process.setModel(model);
		model.setProcess(process);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationInterface#getAssetValue(double, int)
	 */
	@Override
	public RandomVariableInterface getAssetValue(double time, int assetIndex) throws CalculationException {
		return getAssetValue(getTimeIndex(time), assetIndex);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationInterface#getAssetValue(int, int)
	 */
	@Override
	public RandomVariableInterface getAssetValue(int timeIndex, int assetIndex) throws CalculationException {
		return model.getProcess().getProcessValue(timeIndex, assetIndex);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationInterface#getNumeraire(int)
	 */
	@Override
	public RandomVariableInterface getNumeraire(int timeIndex) throws CalculationException {
		double time = getTime(timeIndex);

		return model.getNumeraire(time);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationInterface#getNumeraire(double)
	 */
	@Override
	public RandomVariableInterface getNumeraire(double time) throws CalculationException {
		return model.getNumeraire(time);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.MonteCarloSimulationInterface#getMonteCarloWeights(double)
	 */
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

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationInterface#getCloneWithModifiedData(java.util.Map)
	 */
	@Override
	public AssetModelMonteCarloSimulationInterface getCloneWithModifiedData(Map<String, Object> dataModified) throws CalculationException {
		AbstractProcessInterface process = model.getProcess();

		AbstractModelInterface		newModel	= model.getCloneWithModifiedData(dataModified);

		AbstractProcessInterface	newProcess;
		try {
			newProcess = process.getCloneWithModifiedData(dataModified);
		}
		catch(UnsupportedOperationException e) {
			newProcess = process;
		}

		// In the case where the model has changed we need a new process anyway
		if(newModel != model && newProcess == process) {
			newProcess = process.clone();
		}

		return new MonteCarloAssetModel(newModel, newProcess);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationInterface#getCloneWithModifiedSeed(int)
	 */
	@Override
	public AssetModelMonteCarloSimulationInterface getCloneWithModifiedSeed(int seed) {
		throw new UnsupportedOperationException("Method not implemented");
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.MonteCarloSimulationInterface#getNumberOfPaths()
	 */
	@Override
	public int getNumberOfPaths() {
		return model.getProcess().getNumberOfPaths();
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.MonteCarloSimulationInterface#getTimeDiscretization()
	 */
	@Override
	public TimeDiscretizationInterface getTimeDiscretization() {
		return model.getProcess().getTimeDiscretization();
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.MonteCarloSimulationInterface#getTime(int)
	 */
	@Override
	public double getTime(int timeIndex) {
		return model.getProcess().getTime(timeIndex);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.MonteCarloSimulationInterface#getTimeIndex(double)
	 */
	@Override
	public int getTimeIndex(double time) {
		return model.getProcess().getTimeIndex(time);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.MonteCarloSimulationInterface#getRandomVariableForConstant(double)
	 */
	@Override
	public RandomVariableInterface getRandomVariableForConstant(double value) {
		return model.getRandomVariableForConstant(value);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.MonteCarloSimulationInterface#getMonteCarloWeights(int)
	 */
	@Override
	public RandomVariableInterface getMonteCarloWeights(int timeIndex) throws CalculationException {
		return model.getProcess().getMonteCarloWeights(timeIndex);
	}

	/**
	 * Returns the {@link AbstractModel} used for this Monte-Carlo simulation.
	 *
	 * @return the model
	 */
	public AbstractModelInterface getModel() {
		return model;
	}
}
