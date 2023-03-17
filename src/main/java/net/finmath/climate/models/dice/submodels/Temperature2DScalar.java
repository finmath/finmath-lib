package net.finmath.climate.models.dice.submodels;

import net.finmath.climate.models.Temperature;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;

/**
 * State vector representing temperature above pre-industrial level in Kelvin (K).
 *
 * @author Christian Fries
 */
public class Temperature2DScalar implements Temperature {

	private final double temperatureOfAtmosphere;
	private final double temperatureOfLandAndOcean;

	/**
	 * Create a temperature vector.
	 * 
	 * @param temperatureOfAtmosphere Temperature over pre-industrial of the atmosphere.
	 * @param temperatureOfLandAndOcean Temperature over pre-industrial of land and ocean.
	 */
	public Temperature2DScalar(double temperatureOfAtmosphere, double temperatureOfLandAndOcean) {
		super();
		this.temperatureOfAtmosphere = temperatureOfAtmosphere;
		this.temperatureOfLandAndOcean = temperatureOfLandAndOcean;
	}

	public Temperature2DScalar(double[] temperature) {
		super();
		this.temperatureOfAtmosphere = temperature[0];
		this.temperatureOfLandAndOcean = temperature[1];
	}

	public Temperature2DScalar() {
		this(0.85, 0.0068);
	}

	@Override
	public Double getExpectedTemperatureOfAtmosphere() {
		return temperatureOfAtmosphere;
	}

	@Override
	public RandomVariable getTemperatureOfAtmosphere() {
		return Scalar.of(temperatureOfAtmosphere);
	}

	public double getTemperatureOfLandAndOcean() {
		return temperatureOfLandAndOcean;
	}

	double[] getAsDoubleArray() {
		return new double[] { temperatureOfAtmosphere, temperatureOfLandAndOcean };
	}
}
