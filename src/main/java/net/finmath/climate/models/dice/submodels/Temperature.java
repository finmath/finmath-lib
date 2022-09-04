package net.finmath.climate.models.dice.submodels;

/**
 * State vector representing temperature above pre-industrial level.
 *
 * @author Christian Fries
 */
public class Temperature {

	private final double temperatureOfAtmosphere;
	private final double temperatureOfLandAndOcean;

	public Temperature(double temperatureOfAtmosphere, double temperatureOfLandAndOcean) {
		super();
		this.temperatureOfAtmosphere = temperatureOfAtmosphere;
		this.temperatureOfLandAndOcean = temperatureOfLandAndOcean;
	}

	public Temperature(double[] temperature) {
		super();
		this.temperatureOfAtmosphere = temperature[0];
		this.temperatureOfLandAndOcean = temperature[1];
	}

	public Temperature() {
		this(0.85, 0.0068);
	}

	public double getTemperatureOfAtmosphere() {
		return temperatureOfAtmosphere;
	}

	public double getTemperatureOfLandAndOcean() {
		return temperatureOfLandAndOcean;
	}

	double[] getAsDoubleArray() {
		return new double[] { temperatureOfAtmosphere, temperatureOfLandAndOcean };
	}
}
