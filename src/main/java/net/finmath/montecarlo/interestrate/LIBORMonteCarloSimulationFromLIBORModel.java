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
import net.finmath.montecarlo.BrownianMotion;
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
 * @version 0.7
 */
public class LIBORMonteCarloSimulationFromLIBORModel implements LIBORModelMonteCarloSimulationModel {

	private final LIBORModel model;

	/**
	 * Create a LIBOR Monte-Carlo Simulation from a given LIBORMarketModelFromCovarianceModel and an MonteCarloProcess.
	 *
	 * @param model The LIBORMarketModelFromCovarianceModel.
	 * @param process The process.
	 */
	public LIBORMonteCarloSimulationFromLIBORModel(final LIBORModel model, final MonteCarloProcess process) {
		super();
		this.model		= model;

		this.model.setProcess(process);
		process.setModel(model);
	}

	/**
	 * Create a LIBOR Monte-Carlo Simulation from a given LIBORModel.
	 *
	 * @param model The LIBORModel.
	 */
	public LIBORMonteCarloSimulationFromLIBORModel(final LIBORModel model) {
		super();
		this.model		= model;
	}

	@Override
	public RandomVariable getMonteCarloWeights(final int timeIndex) throws CalculationException {
		return model.getProcess().getMonteCarloWeights(timeIndex);
	}

	@Override
	public RandomVariable getMonteCarloWeights(final double time) throws CalculationException {
		int timeIndex = getTimeIndex(time);
		if(timeIndex < 0) {
			timeIndex = (-timeIndex-1)-1;
		}
		return model.getProcess().getMonteCarloWeights(timeIndex);
	}

	@Override
	public int getNumberOfFactors() {
		return model.getProcess().getNumberOfFactors();
	}

	@Override
	public int getNumberOfPaths() {
		return model.getProcess().getNumberOfPaths();
	}

	@Override
	public LocalDateTime getReferenceDate() {
		return model.getReferenceDate();
	}

	@Override
	public double getTime(final int timeIndex) {
		return model.getProcess().getTime(timeIndex);
	}

	@Override
	public TimeDiscretization getTimeDiscretization() {
		return model.getProcess().getTimeDiscretization();
	}

	@Override
	public int getTimeIndex(final double time) {
		return model.getProcess().getTimeIndex(time);
	}

	@Override
	public RandomVariable getRandomVariableForConstant(final double value) {
		return model.getRandomVariableForConstant(value);
	}

	@Override
	public BrownianMotion getBrownianMotion() {
		return (BrownianMotion)model.getProcess().getStochasticDriver();
	}

	@Override
	public RandomVariable getLIBOR(final int timeIndex, final int liborIndex) throws CalculationException {
		return model.getLIBOR(timeIndex, liborIndex);
	}

	@Override
	public RandomVariable[] getLIBORs(final int timeIndex) throws CalculationException
	{
		final RandomVariable[] randomVariableVector = new RandomVariable[getNumberOfComponents()];
		for(int componentIndex=0; componentIndex<getNumberOfComponents(); componentIndex++) {
			randomVariableVector[componentIndex] = getLIBOR(timeIndex, componentIndex);
		}

		return randomVariableVector;
	}

	@Override
	public RandomVariable getLIBOR(final double time, final double periodStart, final double periodEnd) throws CalculationException
	{
		return model.getLIBOR(time, periodStart, periodEnd);
	}

	@Override
	public double getLiborPeriod(final int timeIndex) {
		return model.getLiborPeriod(timeIndex);
	}

	@Override
	public TimeDiscretization getLiborPeriodDiscretization() {
		return model.getLiborPeriodDiscretization();
	}

	@Override
	public int getLiborPeriodIndex(final double time) {
		return model.getLiborPeriodIndex(time);
	}

	public int getNumberOfComponents() {
		return model.getNumberOfComponents();
	}

	@Override
	public int getNumberOfLibors() {
		return model.getNumberOfLibors();
	}

	@Override
	public RandomVariable getNumeraire(final double time) throws CalculationException {
		return model.getNumeraire(time);
	}

	@Override
	public LIBORModel getModel() {
		return model;
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel#getProcess()
	 */
	@Override
	public MonteCarloProcess getProcess() {
		return model.getProcess();
	}

	@Override
	public Object getCloneWithModifiedSeed(final int seed) {
		final MonteCarloProcessFromProcessModel process = (MonteCarloProcessFromProcessModel) ((MonteCarloProcessFromProcessModel)getProcess()).getCloneWithModifiedSeed(seed);
		return new LIBORMonteCarloSimulationFromLIBORModel(model, process);
	}

	@Override
	public LIBORModelMonteCarloSimulationModel getCloneWithModifiedData(final Map<String, Object> dataModified) throws CalculationException {
		final LIBORModel modelClone = model.getCloneWithModifiedData(dataModified);
		if(dataModified.containsKey("discountCurve") && dataModified.size() == 1) {
			// In this case we may re-use the underlying process
			final LIBORMonteCarloSimulationFromLIBORModel lmmSimClone = new LIBORMonteCarloSimulationFromLIBORModel(modelClone);
			modelClone.setProcess(getProcess());		// Reuse process associated with other model
			return lmmSimClone;
		}
		else {
			return new LIBORMonteCarloSimulationFromLIBORModel(modelClone, getProcess().clone());
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
	public LIBORModelMonteCarloSimulationModel getCloneWithModifiedData(final String entityKey, final Object dataModified) throws CalculationException
	{
		final Map<String, Object> dataModifiedMap = new HashMap<>();
		dataModifiedMap.put(entityKey, dataModified);
		return getCloneWithModifiedData(dataModifiedMap);
	}

	@Override
	public Map<String, RandomVariable> getModelParameters() {
		return model.getModelParameters();
	}
}
