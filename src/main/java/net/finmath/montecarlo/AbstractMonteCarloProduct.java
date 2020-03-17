/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2006
 */
package net.finmath.montecarlo;

import java.util.HashMap;
import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.modelling.Model;
import net.finmath.stochastic.RandomVariable;

/**
 * Base class for products requiring an MonteCarloSimulationModel for valuation.
 *
 * @author Christian Fries
 * @version 1.0
 */
public abstract class AbstractMonteCarloProduct implements MonteCarloProduct {

	private final String currency;

	public AbstractMonteCarloProduct(final String currency) {
		super();
		this.currency = currency;
	}

	public AbstractMonteCarloProduct() {
		this(null);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.MonteCarloProduct#getValue(double, net.finmath.modelling.Model)
	 */
	@Override
	public Object getValue(final double evaluationTime, final Model model) {
		if(model instanceof MonteCarloSimulationModel) {
			try {
				return getValue(evaluationTime, (MonteCarloSimulationModel)model);
			} catch (final CalculationException e) {
				return null;
			}
		}
		else {
			throw new IllegalArgumentException("The product " + this.getClass()
			+ " cannot be valued against a model " + model.getClass() + "."
			+ "It requires a model of type " + MonteCarloSimulationModel.class + ".");
		}
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.MonteCarloProduct#getValues(double, net.finmath.modelling.Model)
	 */
	@Override
	public Map<String, Object> getValues(final double evaluationTime, final Model model) {
		Map<String, Object> results;
		if(model instanceof MonteCarloSimulationModel) {
			try {
				results = getValues(evaluationTime, (MonteCarloSimulationModel)model);
			} catch (final CalculationException e) {
				results = new HashMap<>();
				results.put("exception", e);
			}
		}
		else {
			final Exception e = new IllegalArgumentException("The product " + this.getClass()
			+ " cannot be valued against a model " + model.getClass() + "."
			+ "It requires a model of type " + MonteCarloSimulationModel.class + ".");
			results = new HashMap<>();
			results.put("exception", e);
		}

		return results;
	}

	@Override
	public abstract RandomVariable getValue(double evaluationTime, MonteCarloSimulationModel model) throws CalculationException;

	@Override
	public double getValue(final MonteCarloSimulationModel model) throws CalculationException {
		return getValue(0.0, model).getAverage();
	}

	@Override
	public Map<String, Object> getValues(final double evaluationTime, final MonteCarloSimulationModel model) throws CalculationException
	{
		final RandomVariable values = getValue(evaluationTime, model);

		if(values == null) {
			return null;
		}

		// Sum up values on path
		final double value = values.getAverage();
		final double error = values.getStandardError();

		final Map<String, Object> results = new HashMap<>();
		results.put("value", value);
		results.put("error", error);

		return results;
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.MonteCarloProduct#getValuesForModifiedData(double, net.finmath.montecarlo.MonteCarloSimulationModel, java.util.Map)
	 */
	@Override
	public Map<String, Object> getValuesForModifiedData(final double evaluationTime, final MonteCarloSimulationModel model, final Map<String,Object> dataModified) throws CalculationException
	{
		final MonteCarloSimulationModel modelModified = model.getCloneWithModifiedData(dataModified);

		return getValues(evaluationTime, modelModified);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.MonteCarloProduct#getValuesForModifiedData(double, net.finmath.montecarlo.MonteCarloSimulationModel, java.lang.String, java.lang.Object)
	 */
	@Override
	public Map<String, Object> getValuesForModifiedData(final double evaluationTime, final MonteCarloSimulationModel model, final String entityKey, final Object dataModified) throws CalculationException
	{
		final Map<String, Object> dataModifiedMap = new HashMap<>();
		dataModifiedMap.put(entityKey, dataModified);
		return getValuesForModifiedData(evaluationTime, model, dataModifiedMap);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.MonteCarloProduct#getValues(net.finmath.montecarlo.MonteCarloSimulationModel)
	 */
	@Override
	public Map<String, Object> getValues(final MonteCarloSimulationModel model) throws CalculationException
	{
		return getValues(0.0, model);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.MonteCarloProduct#getValuesForModifiedData(net.finmath.montecarlo.MonteCarloSimulationModel, java.util.Map)
	 */
	@Override
	public Map<String, Object> getValuesForModifiedData(final MonteCarloSimulationModel model, final Map<String,Object> dataModified) throws CalculationException
	{
		return getValuesForModifiedData(0.0, model, dataModified);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.MonteCarloProduct#getValuesForModifiedData(net.finmath.montecarlo.MonteCarloSimulationModel, java.lang.String, java.lang.Object)
	 */
	@Override
	public Map<String, Object> getValuesForModifiedData(final MonteCarloSimulationModel model, final String entityKey, final Object dataModified) throws CalculationException
	{
		return getValuesForModifiedData(0.0, model, entityKey, dataModified);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.MonteCarloProduct#getCurrency()
	 */
	@Override
	public String getCurrency() {
		return currency;
	}

	@Override
	public String toString() {
		return "AbstractMonteCarloProduct [currency=" + currency + "]";
	}
}
