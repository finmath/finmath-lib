package net.finmath.climate.models.dice.submodels;

/**
 * State vector representing carbon concentration.
 *
 * @author Christian Fries
 */
public class CarbonConcentration {

	private final double carbonConcentrationInAtmosphere;
	private final double carbonConcentrationInShallowOcean;
	private final double carbonConcentrationInLowerOcean;

	public CarbonConcentration(double carbonConcentrationInAtmosphere, double carbonConcentrationInShallowOcean, double carbonConcentrationInLowerOcean) {
		super();
		this.carbonConcentrationInAtmosphere = carbonConcentrationInAtmosphere;
		this.carbonConcentrationInShallowOcean = carbonConcentrationInShallowOcean;
		this.carbonConcentrationInLowerOcean = carbonConcentrationInLowerOcean;
	}

	public CarbonConcentration(double[] carbonConcentration) {
		this(carbonConcentration[0], carbonConcentration[1], carbonConcentration[2]);
	}

	public CarbonConcentration() {
		this(851, 460, 1740);		// GtC
	}


	public double getCarbonConcentrationInAtmosphere() {
		return carbonConcentrationInAtmosphere;
	}

	public double getCarbonConcentrationInShallowOcean() {
		return carbonConcentrationInShallowOcean;
	}

	public double getCarbonConcentrationInLowerOcean() {
		return carbonConcentrationInLowerOcean;
	}

	public double[] getAsDoubleArray() {
		return new double[] { carbonConcentrationInAtmosphere, carbonConcentrationInShallowOcean, carbonConcentrationInLowerOcean };
	}
}
