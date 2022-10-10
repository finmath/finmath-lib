package net.finmath.climate.models;

import net.finmath.stochastic.RandomVariable;

public interface Temperature {

	RandomVariable getTemperatureOfAtmosphere();

	Double getExpectedTemperatureOfAtmosphere();

}