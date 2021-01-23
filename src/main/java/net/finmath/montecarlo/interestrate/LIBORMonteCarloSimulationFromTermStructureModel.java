/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2004
 */
package net.finmath.montecarlo.interestrate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.process.MonteCarloProcess;
import net.finmath.montecarlo.process.MonteCarloProcessFromProcessModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;

/**
 * Implements convenient methods for a LIBOR market model,
 * based on a given <code>LIBORMarketModelFromCovarianceModel</code> model
 * and <code>AbstractLogNormalProcess</code> process.
 *
 * @author Christian Fries
 * @version 0.9
 */
public class LIBORMonteCarloSimulationFromTermStructureModel implements LIBORModelMonteCarloSimulationModel {

	private final TermStructureModel model;
	private final MonteCarloProcess process;

	/**
	 * Create a LIBOR Monte-Carlo Simulation from a given LIBORMarketModelFromCovarianceModel and an MonteCarloProcessFromProcessModel.
	 *
	 * @param model The LIBORMarketModelFromCovarianceModel.
	 * @param process The process.
	 */
	public LIBORMonteCarloSimulationFromTermStructureModel(final TermStructureModel model, final MonteCarloProcess process) {
		super();
		this.model		= model;
		this.process	= process;
	}

	/**
	 * Create a LIBOR Monte-Carlo Simulation from a given LIBORMarketModelFromCovarianceModel and an MonteCarloProcessFromProcessModel.
	 *
	 * @param process The process creating the model.
	 */
	public LIBORMonteCarloSimulationFromTermStructureModel(final MonteCarloProcess process) {
		this((TermStructureModel)process.getModel(), process);
	}

	@Override
	public RandomVariable getMonteCarloWeights(final int timeIndex) throws CalculationException {
		return process.getMonteCarloWeights(timeIndex);
	}

	@Override
	public RandomVariable getMonteCarloWeights(final double time) throws CalculationException {
		int timeIndex = getTimeIndex(time);
		if(timeIndex < 0) {
			timeIndex = (-timeIndex-1)-1;
		}
		return process.getMonteCarloWeights(timeIndex);
	}

	@Override
	public int getNumberOfFactors() {
		return process.getNumberOfFactors();
	}

	@Override
	public int getNumberOfPaths() {
		return process.getNumberOfPaths();
	}

	@Override
	public LocalDateTime getReferenceDate() {
		throw new UnsupportedOperationException("This model does not provide a reference date. Reference dates will be mandatory in a future version.");
	}

	@Override
	public double getTime(final int timeIndex) {
		return process.getTime(timeIndex);
	}

	@Override
	public TimeDiscretization getTimeDiscretization() {
		return process.getTimeDiscretization();
	}

	@Override
	public int getTimeIndex(final double time) {
		return process.getTimeIndex(time);
	}

	@Override
	public RandomVariable getRandomVariableForConstant(final double value) {
		return process.getStochasticDriver().getRandomVariableForConstant(value);
	}

	@Override
	public RandomVariable getLIBOR(final int timeIndex, final int liborIndex) throws CalculationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public RandomVariable[] getLIBORs(final int timeIndex) {
		throw new UnsupportedOperationException();
	}

	@Override
	public RandomVariable getForwardRate(final double time, final double periodStart, final double periodEnd) throws CalculationException
	{
		return model.getLIBOR(process, time, periodStart, periodEnd);
	}

	@Override
	public double getLiborPeriod(final int timeIndex) {
		throw new UnsupportedOperationException();
	}

	@Override
	public TimeDiscretization getLiborPeriodDiscretization() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getLiborPeriodIndex(final double time) {
		throw new UnsupportedOperationException();
	}

	public int getNumberOfComponents() {
		return model.getNumberOfComponents();
	}

	@Override
	public int getNumberOfLibors() {
		throw new UnsupportedOperationException();
	}

	@Override
	public RandomVariable getNumeraire(final double time) throws CalculationException {
		return model.getNumeraire(process, time);
	}

	@Override
	public TermStructureModel getModel() {
		return model;
	}

	@Override
	public MonteCarloProcess getProcess() {
		return process;
	}

	@Override
	public Object getCloneWithModifiedSeed(final int seed) {
		final MonteCarloProcessFromProcessModel process = (MonteCarloProcessFromProcessModel) ((MonteCarloProcessFromProcessModel)getProcess()).getCloneWithModifiedSeed(seed);
		return new LIBORMonteCarloSimulationFromTermStructureModel(model, process);
	}

	@Override
	public LIBORModelMonteCarloSimulationModel getCloneWithModifiedData(final Map<String, Object> dataModified) throws CalculationException {
		final TermStructureModel modelClone = model.getCloneWithModifiedData(dataModified);
		if(dataModified.containsKey("discountCurve") && dataModified.size() == 1) {
			// In this case we may re-use the underlying process
			final LIBORMonteCarloSimulationFromTermStructureModel lmmSimClone = new LIBORMonteCarloSimulationFromTermStructureModel(modelClone, process);
			return lmmSimClone;
		}
		else {
			return new LIBORMonteCarloSimulationFromTermStructureModel(modelClone, getProcess().clone());
		}
	}

	/**
	 * Create a clone of this simulation modifying one of its properties (if any).
	 *
	 * @param entityKey The entity to modify.
	 * @param dataModified The data which should be changed in the new model
	 * @return Returns a clone of this model, where the specified part of the data is modified data (then it is no longer a clone :-)
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	public TermStructureMonteCarloSimulationModel getCloneWithModifiedData(final String entityKey, final Object dataModified) throws CalculationException
	{
		final Map<String, Object> dataModifiedMap = new HashMap<>();
		dataModifiedMap.put(entityKey, dataModified);
		return getCloneWithModifiedData(dataModifiedMap);
	}

	@Override
	public Map<String, RandomVariable> getModelParameters() {
		// TODO Add implementation
		throw new UnsupportedOperationException();
	}
}
