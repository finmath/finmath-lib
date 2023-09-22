package net.finmath.climate.models;

import java.util.function.Function;

import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;

/**
 * Interface implemented by Climate Models
 *
 * @author Christian Fries
 */
public interface ClimateModel {

	/**
	 * 
	 * @return the time discretization associated with this model.
	 */
	TimeDiscretization getTimeDiscretization();
	
	/**
	 * The temperature (scenario vector) at a given time.
	 *
	 * @param time The observation time.
	 * @return The temperature (scenario vector) at a given time.
	 */
	RandomVariable getTemperature(double time);

	/**
	 * The aggregated (discounted) value.
	 *
	 * @return The value (scenario wise).
	 */
	RandomVariable getValue();

	/**
	 * The random vector of un-discounted values (utilities).
	 * 
	 * @return random vector of un-discounted values (utilities).
	 */
	RandomVariable[] getValues();
	
	RandomVariable[] getAbatement();

	RandomVariable[] getEmission();

	CarbonConcentration[] getCarbonConcentration();

	Temperature[] getTemperature();

	RandomVariable[] getDamage();

	RandomVariable[] getGDP();

	RandomVariable[] getConsumptions();

	RandomVariable[] getAbatementCosts();

	RandomVariable getAbatementCost();

	RandomVariable[] getDamageCosts();

	RandomVariable getDamageCost();

	RandomVariable getNumeraire(double time);

	Function<Double, RandomVariable> getAbatementModel();
	
	Function<Double, RandomVariable> getSavingsRateModel();
	
}
