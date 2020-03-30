/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.01.2004
 */
package net.finmath.montecarlo.assetderivativevaluation;

import java.time.LocalDateTime;
import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.IndependentIncrements;
import net.finmath.montecarlo.model.AbstractProcessModel;
import net.finmath.montecarlo.model.ProcessModel;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.montecarlo.process.MonteCarloProcess;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;

/**
 * This class glues together an <code>AbstractProcessModel</code> and a Monte-Carlo implementation of a <code>MonteCarloProcessFromProcessModel</code>
 * and implements <code>AssetModelMonteCarloSimulationModel</code>.
 *
 * The model is specified via the object implementing <code>ProcessModel</code>.
 *
 * @author Christian Fries
 * @see net.finmath.montecarlo.process.MonteCarloProcess The interface for numerical schemes.
 * @see net.finmath.montecarlo.model.ProcessModel The interface for models provinding parameters to numerical schemes.
 * @version 1.0
 */
public class MonteCarloAssetModel implements AssetModelMonteCarloSimulationModel {

	private final ProcessModel model;
	private final MonteCarloProcess process;

	/**
	 * Create a Monte-Carlo simulation using given process discretization scheme.
	 *
	 * @param process The numerical scheme to be used.
	 */
	public MonteCarloAssetModel(
			final MonteCarloProcess process) {
		super();

		this.model = process.getModel();
		this.process = process;

		// Link model and process for delegation (this code will no longer be necessary in version 5.0)
		process.setModel(model);
		model.setProcess(process);
	}

	/**
	 * Create a Monte-Carlo simulation using given process discretization scheme.
	 *
	 * @param model The model to be used.
	 * @param process The numerical scheme to be used.
	 */
	public MonteCarloAssetModel(
			final ProcessModel model,
			final MonteCarloProcess process) {
		super();

		this.model = model;
		this.process = process;

		// Link model and process for delegation (this code will no longer be necessary in version 5.0)
		process.setModel(model);
		model.setProcess(process);
	}

	public MonteCarloAssetModel(ProcessModel model, IndependentIncrements stochasticDriver) {
		super();

		this.model = model;
		this.process = new EulerSchemeFromProcessModel(model, stochasticDriver);

		// Link model and process for delegation (this code will no longer be necessary in version 5.0)
		process.setModel(model);
		model.setProcess(process);
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

		// TODO Add caching of the numerare here!
		return model.getNumeraire(process, time);
	}

	@Override
	public RandomVariable getNumeraire(final double time) throws CalculationException {

		// TODO Add caching of the numerare here!
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
	public AssetModelMonteCarloSimulationModel getCloneWithModifiedData(final Map<String, Object> dataModified) throws CalculationException {
		final ProcessModel		newModel	= model.getCloneWithModifiedData(dataModified);

		MonteCarloProcess	newProcess;
		try {
			newProcess = process.getCloneWithModifiedData(dataModified);
		}
		catch(final UnsupportedOperationException e) {
			newProcess = process;
		}

		// In the case where the model has changed we need a new process anyway
		if(newModel != model && newProcess == process) {
			newProcess = process.clone();
		}

		return new MonteCarloAssetModel(newModel, newProcess);
	}

	@Override
	public AssetModelMonteCarloSimulationModel getCloneWithModifiedSeed(final int seed) {
		throw new UnsupportedOperationException("Method not implemented");
	}

	@Override
	public int getNumberOfPaths() {
		return process.getNumberOfPaths();
	}

	@Override
	public LocalDateTime getReferenceDate() {
		return model.getReferenceDate();
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

	/**
	 * Returns the {@link AbstractProcessModel} used for this Monte-Carlo simulation.
	 *
	 * @return the model
	 */
	public ProcessModel getModel() {
		return model;
	}

	/**
	 * Returns the {@link MonteCarloProcess} used for this Monte-Carlo simulation.
	 *
	 * @return the process
	 */
	public MonteCarloProcess getProcess() {
		return process;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + " [model=" + model + "]";
	}
}
