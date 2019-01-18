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
public class LIBORMonteCarloSimulationFromTermStructureModel implements LIBORModelMonteCarloSimulationModel {

	private final TermStructureModel model;

	/**
	 * Create a LIBOR Monte-Carlo Simulation from a given LIBORMarketModelFromCovarianceModel and an MonteCarloProcessFromProcessModel.
	 *
	 * @param model The LIBORMarketModelFromCovarianceModel.
	 * @param process The process.
	 */
	public LIBORMonteCarloSimulationFromTermStructureModel(TermStructureModel model, MonteCarloProcessFromProcessModel process) {
		super();
		this.model		= model;

		this.model.setProcess(process);
		process.setModel(model);
	}

	/**
	 * Create a LIBOR Monte-Carlo Simulation from a given LIBORMarketModelFromCovarianceModel.
	 *
	 * @param model The LIBORMarketModelFromCovarianceModel.
	 */
	public LIBORMonteCarloSimulationFromTermStructureModel(TermStructureModel model) {
		super();
		this.model		= model;
	}

	@Override
	public RandomVariable getMonteCarloWeights(int timeIndex) throws CalculationException {
		return model.getProcess().getMonteCarloWeights(timeIndex);
	}

	@Override
	public RandomVariable getMonteCarloWeights(double time) throws CalculationException {
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
		throw new UnsupportedOperationException("This model does not provide a reference date. Reference dates will be mandatory in a future version.");
	}

	@Override
	public double getTime(int timeIndex) {
		return model.getProcess().getTime(timeIndex);
	}

	@Override
	public TimeDiscretization getTimeDiscretization() {
		return model.getProcess().getTimeDiscretization();
	}

	@Override
	public int getTimeIndex(double time) {
		return model.getProcess().getTimeIndex(time);
	}

	@Override
	public RandomVariable getRandomVariableForConstant(double value) {
		return model.getProcess().getStochasticDriver().getRandomVariableForConstant(value);
	}

	@Override
	public BrownianMotion getBrownianMotion() {
		return (BrownianMotion)model.getProcess().getStochasticDriver();
	}

	@Override
	public RandomVariable getLIBOR(int timeIndex, int liborIndex) throws CalculationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public RandomVariable[] getLIBORs(int timeIndex) {
		throw new UnsupportedOperationException();
	}

	@Override
	public RandomVariable getLIBOR(double time, double periodStart, double periodEnd) throws CalculationException
	{
		return model.getLIBOR(time, periodStart, periodEnd);
	}

	@Override
	public double getLiborPeriod(int timeIndex) {
		throw new UnsupportedOperationException();
	}

	@Override
	public TimeDiscretization getLiborPeriodDiscretization() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getLiborPeriodIndex(double time) {
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
	public RandomVariable getNumeraire(double time) throws CalculationException {
		return model.getNumeraire(time);
	}

	@Override
	public TermStructureModel getModel() {
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
	public Object getCloneWithModifiedSeed(int seed) {
		MonteCarloProcessFromProcessModel process = (MonteCarloProcessFromProcessModel) ((MonteCarloProcessFromProcessModel)getProcess()).getCloneWithModifiedSeed(seed);
		return new LIBORMonteCarloSimulationFromTermStructureModel(model, process);
	}

	@Override
	public LIBORModelMonteCarloSimulationModel getCloneWithModifiedData(Map<String, Object> dataModified) throws CalculationException {
		TermStructureModel modelClone = model.getCloneWithModifiedData(dataModified);
		if(dataModified.containsKey("discountCurve") && dataModified.size() == 1) {
			// In this case we may re-use the underlying process
			LIBORMonteCarloSimulationFromTermStructureModel lmmSimClone = new LIBORMonteCarloSimulationFromTermStructureModel(modelClone);
			modelClone.setProcess(getProcess());		// Reuse process associated with other model
			return lmmSimClone;
		}
		else {
			return new LIBORMonteCarloSimulationFromTermStructureModel(modelClone, (MonteCarloProcessFromProcessModel)getProcess().clone());
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
	public TermStructureMonteCarloSimulationModel getCloneWithModifiedData(String entityKey, Object dataModified) throws CalculationException
	{
		Map<String, Object> dataModifiedMap = new HashMap<>();
		dataModifiedMap.put(entityKey, dataModified);
		return getCloneWithModifiedData(dataModifiedMap);
	}

	@Override
	public Map<String, RandomVariable> getModelParameters() {
		// TODO Add implementation
		throw new UnsupportedOperationException();
	}
}
