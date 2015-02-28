/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2004
 */
package net.finmath.montecarlo.interestrate;

import java.util.HashMap;
import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.BrownianMotionInterface;
import net.finmath.montecarlo.interestrate.modelplugins.AbstractLIBORCovarianceModel;
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

	private final LIBORMarketModelInterface model;

	/**
	 * Create a LIBOR Monte-Carlo Simulation from a given LIBORMarketModel and an AbstractProcess.
	 * 
	 * @param model The LIBORMarketModel.
	 * @param process The process.
	 */
	public LIBORModelMonteCarloSimulation(LIBORMarketModelInterface model, AbstractProcess process) {
		super();
		this.model		= model;
		
		this.model.setProcess(process);
		process.setModel(model);
	}

	/**
	 * Create a LIBOR Monte-Carlo Simulation from a given LIBORMarketModel.
	 * 
	 * @param model The LIBORMarketModel.
	 */
	public LIBORModelMonteCarloSimulation(LIBORMarketModelInterface model) {
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
		if(timeIndex < 0) timeIndex = (-timeIndex-1)-1;
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
		return model.getProcess().getBrownianMotion().getRandomVariableForConstant(value);
	}

	@Override
	public BrownianMotionInterface getBrownianMotion() {
		return model.getProcess().getBrownianMotion();
	}

	@Override
	public RandomVariableInterface getLIBOR(int timeIndex, int liborIndex) throws CalculationException {
		return model.getLIBOR(timeIndex, liborIndex);
	}

	@Override
	public RandomVariableInterface[] getLIBORs(int timeIndex) throws CalculationException
	{
		RandomVariableInterface[] randomVariableVector = new RandomVariableInterface[getNumberOfComponents()];
		for(int componentIndex=0; componentIndex<getNumberOfComponents(); componentIndex++)	randomVariableVector[componentIndex] = getLIBOR(timeIndex, componentIndex);

		return randomVariableVector;
	}

	@Override
	public RandomVariableInterface getLIBOR(double time, double periodStart, double periodEnd) throws CalculationException
	{
		int periodStartIndex    = getLiborPeriodIndex(periodStart);
		int periodEndIndex      = getLiborPeriodIndex(periodEnd);

		// The forward rates are provided on fractional tenor discretization points using linear interpolation. See ISBN 0470047224.
		
		// Interpolation on tenor, consistent with interpolation on numeraire (log-linear): interpolate end date
		if(periodEndIndex < 0) {
			int		previousEndIndex	= (-periodEndIndex-1)-1;
			double	previousEndTime		= getLiborPeriod(previousEndIndex);
			double	nextEndTime			= getLiborPeriod(previousEndIndex+1);
			RandomVariableInterface liborLongPeriod		= getLIBOR(time, periodStart, nextEndTime);
			RandomVariableInterface	liborShortPeriod	= getLIBOR(time, previousEndTime, nextEndTime);

			// Interpolate libor from periodStart to periodEnd on periodEnd
			RandomVariableInterface libor = liborLongPeriod.mult(nextEndTime-periodStart).add(1.0)
					.div(
							liborShortPeriod.mult(nextEndTime-previousEndTime).add(1.0).log().mult((nextEndTime-periodEnd)/(nextEndTime-previousEndTime)).exp()
							).sub(1.0).div(periodEnd-periodStart);

			// Analytic adjustment for the interpolation
			// @TODO reference to AnalyticModel must not be null
			// @TODO Code needs review
			/*
			double analyticLibor				= model.getForwardRateCurve().getForward(model.getAnalyticModel(), periodStart, periodEnd-periodStart);
			double analyticLiborLongPeriod		= model.getForwardRateCurve().getForward(model.getAnalyticModel(), periodStart, nextEndTime-periodStart);
			double analyticLiborShortPeriod		= model.getForwardRateCurve().getForward(model.getAnalyticModel(), previousEndTime, nextEndTime-previousEndTime);
			double analyticInterpolatedOnePlusLiborDt		= (1 + analyticLiborLongPeriod * (nextEndTime-periodStart)) / Math.exp(Math.log(1 + analyticLiborShortPeriod * (nextEndTime-previousEndTime)) * (nextEndTime-periodEnd)/(nextEndTime-previousEndTime));
			double analyticOnePlusLiborDt					= (1 + analyticLibor * (periodEnd-periodStart));
			double adjustment = analyticOnePlusLiborDt / analyticInterpolatedOnePlusLiborDt;
			libor = libor.mult(periodEnd-periodStart).add(1.0).mult(adjustment).sub(1.0).div(periodEnd-periodStart);
			*/
			return libor;
		}

		// Interpolation on tenor, consistent with interpolation on numeraire (log-linear): interpolate start date
		if(periodStartIndex < 0) {
			int		previousStartIndex	= (-periodStartIndex-1)-1;
			double	previousStartTime	= getLiborPeriod(previousStartIndex);
			double	nextStartTime		= getLiborPeriod(previousStartIndex+1);
			RandomVariableInterface liborLongPeriod		= getLIBOR(time, previousStartTime, periodEnd);
			RandomVariableInterface	liborShortPeriod	= getLIBOR(time, previousStartTime, nextStartTime);

			RandomVariableInterface libor = liborLongPeriod.mult(periodEnd-previousStartTime).add(1.0)
					.div(
							liborShortPeriod.mult(nextStartTime-previousStartTime).add(1.0).log().mult((periodStart-previousStartTime)/(nextStartTime-previousStartTime)).exp()
							).sub(1.0).div(periodEnd-periodStart);

			// Analytic adjustment for the interpolation
			// @TODO reference to AnalyticModel must not be null
			// @TODO Code needs review
			/*
			double analyticLibor				= model.getForwardRateCurve().getForward(model.getAnalyticModel(), periodStart, periodEnd-periodStart);
			double analyticLiborLongPeriod		= model.getForwardRateCurve().getForward(model.getAnalyticModel(), previousStartTime, periodEnd-previousStartTime);
			double analyticLiborShortPeriod		= model.getForwardRateCurve().getForward(model.getAnalyticModel(), previousStartTime, nextStartTime-previousStartTime);
			double analyticInterpolatedOnePlusLiborDt		= (1 + analyticLiborLongPeriod * (periodEnd-previousStartTime)) / Math.exp(Math.log(1 + analyticLiborShortPeriod * (nextStartTime-previousStartTime)) * (periodStart-previousStartTime)/(nextStartTime-previousStartTime));
			double analyticOnePlusLiborDt					= (1 + analyticLibor * (periodEnd-periodStart));
			double adjustment = analyticOnePlusLiborDt / analyticInterpolatedOnePlusLiborDt;
			libor = libor.mult(periodEnd-periodStart).add(1.0).mult(adjustment).sub(1.0).div(periodEnd-periodStart);
			*/
			return libor;
		}

		if(periodStartIndex < 0 || periodEndIndex < 0) throw new AssertionError("LIBOR requested outside libor discretization points and interpolation was not performed.");

		// If time is beyond fixing, use the fixing time.
		time = Math.min(time, periodStart);
		int timeIndex           = getTimeIndex(time);

		// If time is not part of the discretization, use the latest available point.
		if(timeIndex < 0) {
			timeIndex = -timeIndex-2;
//			double timeStep = getTimeDiscretization().getTimeStep(timeIndex);
//			return getLIBOR(getTime(timeIndex), periodStart, periodEnd).mult((getTime(timeIndex+1)-time)/timeStep).add(getLIBOR(getTime(timeIndex+1), periodStart, periodEnd).mult((time-getTime(timeIndex))/timeStep));
		}
		
		// If this is a model primitive then return it
		if(periodStartIndex+1==periodEndIndex) return getLIBOR(timeIndex, periodStartIndex);

		// The requested LIBOR is not a model primitive. We need to calculate it (slow!)
		RandomVariableInterface accrualAccount = getRandomVariableForConstant(1.0);

		// Calculate the value of the forward bond
		for(int periodIndex = periodStartIndex; periodIndex<periodEndIndex; periodIndex++)
		{
			double subPeriodLength = getLiborPeriod(periodIndex+1) - getLiborPeriod(periodIndex);
			RandomVariableInterface liborOverSubPeriod = getLIBOR(timeIndex, periodIndex);
			
			accrualAccount = accrualAccount.accrue(liborOverSubPeriod, subPeriodLength);
		}

		RandomVariableInterface libor = accrualAccount.sub(1.0).div(periodEnd - periodStart);

		return libor;
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

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface#getCovarianceModel()
	 */
	public AbstractLIBORCovarianceModel getCovarianceModel() {
		return model.getCovarianceModel();
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface#getModel()
	 */
	public LIBORMarketModelInterface getModel() {
		return model;
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface#getProcess()
	 */
	public AbstractProcessInterface getProcess() {
		return model.getProcess();
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface#getCloneWithModifiedSeed(int)
	 */
	public Object getCloneWithModifiedSeed(int seed) {
		AbstractProcess process = (AbstractProcess) ((AbstractProcess)getProcess()).getCloneWithModifiedSeed(seed);
		return new LIBORModelMonteCarloSimulation(model, process);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.MonteCarloSimulationInterface#getCloneWithModifiedData(java.util.Map)
	 */
	public LIBORModelMonteCarloSimulationInterface getCloneWithModifiedData(Map<String, Object> dataModified) throws CalculationException {
		LIBORMarketModelInterface modelClone = model.getCloneWithModifiedData(dataModified);
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
		Map<String, Object> dataModifiedMap = new HashMap<String, Object>();
		dataModifiedMap.put(entityKey, dataModified);
		return getCloneWithModifiedData(dataModifiedMap);
	}
}
