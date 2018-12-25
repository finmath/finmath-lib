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
import net.finmath.montecarlo.BrownianMotionInterface;
import net.finmath.montecarlo.process.AbstractProcess;
import net.finmath.montecarlo.process.AbstractProcessInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * Implements convenient methods for a LIBOR market model,
 * based on a given <code>LIBORMarketModel</code> model
 * and <code>AbstractLogNormalProcess</code> process.
 *
 * @author Christian Fries
 * @version 0.7
 */
public class LIBORModelMonteCarloSimulation implements LIBORModelMonteCarloSimulationInterface {

	private final LIBORModelInterface model;

	/**
	 * Create a LIBOR Monte-Carlo Simulation from a given LIBORMarketModel and an AbstractProcess.
	 *
	 * @param model The LIBORMarketModel.
	 * @param process The process.
	 */
	public LIBORModelMonteCarloSimulation(LIBORModelInterface model, AbstractProcessInterface process) {
		super();
		this.model		= model;

		this.model.setProcess(process);
		process.setModel(model);
	}

	/**
	 * Create a LIBOR Monte-Carlo Simulation from a given LIBORModelInterface.
	 *
	 * @param model The LIBORModelInterface.
	 */
	public LIBORModelMonteCarloSimulation(LIBORModelInterface model) {
		super();
		this.model		= model;
	}

	@Override
	public RandomVariableInterface getMonteCarloWeights(int timeIndex) throws CalculationException {
		return model.getProcess().getMonteCarloWeights(timeIndex);
	}

	@Override
	public RandomVariableInterface getMonteCarloWeights(double time) throws CalculationException {
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
	public double getTime(int timeIndex) {
		return model.getProcess().getTime(timeIndex);
	}

	@Override
	public TimeDiscretizationInterface getTimeDiscretization() {
		return model.getProcess().getTimeDiscretization();
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
	public BrownianMotionInterface getBrownianMotion() {
		return (BrownianMotionInterface)model.getProcess().getStochasticDriver();
	}

	@Override
	public RandomVariableInterface getLIBOR(int timeIndex, int liborIndex) throws CalculationException {
		return model.getLIBOR(timeIndex, liborIndex);
	}

	@Override
	public RandomVariableInterface[] getLIBORs(int timeIndex) throws CalculationException
	{
		RandomVariableInterface[] randomVariableVector = new RandomVariableInterface[getNumberOfComponents()];
		for(int componentIndex=0; componentIndex<getNumberOfComponents(); componentIndex++) {
			randomVariableVector[componentIndex] = getLIBOR(timeIndex, componentIndex);
		}

		return randomVariableVector;
	}

	@Override
	public RandomVariableInterface getLIBOR(double time, double periodStart, double periodEnd) throws CalculationException
	{
		return model.getLIBOR(time, periodStart, periodEnd);
	}

	@Override
	public double getLiborPeriod(int timeIndex) {
		return model.getLiborPeriod(timeIndex);
	}

	@Override
	public TimeDiscretizationInterface getLiborPeriodDiscretization() {
		return model.getLiborPeriodDiscretization();
	}

	@Override
	public int getLiborPeriodIndex(double time) {
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
	public RandomVariableInterface getNumeraire(double time) throws CalculationException {
		return model.getNumeraire(time);
	}

	@Override
	public LIBORModelInterface getModel() {
		return model;
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface#getProcess()
	 */
	@Override
	public AbstractProcessInterface getProcess() {
		return model.getProcess();
	}

	@Override
	public Object getCloneWithModifiedSeed(int seed) {
		AbstractProcess process = (AbstractProcess) ((AbstractProcess)getProcess()).getCloneWithModifiedSeed(seed);
		return new LIBORModelMonteCarloSimulation(model, process);
	}

	@Override
	public LIBORModelMonteCarloSimulationInterface getCloneWithModifiedData(Map<String, Object> dataModified) throws CalculationException {
		LIBORModelInterface modelClone = model.getCloneWithModifiedData(dataModified);
		if(dataModified.containsKey("discountCurve") && dataModified.size() == 1) {
			// In this case we may re-use the underlying process
			LIBORModelMonteCarloSimulation lmmSimClone = new LIBORModelMonteCarloSimulation(modelClone);
			modelClone.setProcess(getProcess());		// Reuse process associated with other model
			return lmmSimClone;
		}
		else {
			return new LIBORModelMonteCarloSimulation(modelClone, (AbstractProcess)getProcess().clone());
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
	public LIBORModelMonteCarloSimulationInterface getCloneWithModifiedData(String entityKey, Object dataModified) throws CalculationException
	{
		Map<String, Object> dataModifiedMap = new HashMap<>();
		dataModifiedMap.put(entityKey, dataModified);
		return getCloneWithModifiedData(dataModifiedMap);
	}

	@Override
	public Map<String, RandomVariableInterface> getModelParameters() {
		return model.getModelParameters();
	}
}
