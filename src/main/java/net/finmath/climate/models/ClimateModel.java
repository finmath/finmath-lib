package net.finmath.climate.models;

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
	 * The value (scenario vector) (discounted)
	 *
	 * @return The value (scenario vector).
	 */
	RandomVariable getValue();

	RandomVariable[] getValues();

	RandomVariable[] getAbatement();

	RandomVariable[] getEmission();

	CarbonConcentration[] getCarbonConcentration();

	Temperature[] getTemperature();

	RandomVariable[] getDamage();

	RandomVariable[] getGDP();
}
