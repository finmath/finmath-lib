package net.finmath.climate.models;

import net.finmath.stochastic.RandomVariable;

public interface CarbonConcentration {

	RandomVariable getCarbonConcentrationInAtmosphere();

	Double getExpectedCarbonConcentrationInAtmosphere();

}