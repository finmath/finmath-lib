package net.finmath.climate.models.dice.submodels;

import net.finmath.climate.models.CarbonConcentration;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;

/**
 * State vector representing carbon concentration in units of GtC.
 *
 * @author Christian Fries
 */
public class CarbonConcentration3DScalar implements CarbonConcentration {

	private final double carbonConcentrationInAtmosphere;
	private final double carbonConcentrationInShallowOcean;
	private final double carbonConcentrationInLowerOcean;

	public CarbonConcentration3DScalar(double carbonConcentrationInAtmosphere, double carbonConcentrationInShallowOcean, double carbonConcentrationInLowerOcean) {
		super();
		this.carbonConcentrationInAtmosphere = carbonConcentrationInAtmosphere;
		this.carbonConcentrationInShallowOcean = carbonConcentrationInShallowOcean;
		this.carbonConcentrationInLowerOcean = carbonConcentrationInLowerOcean;
	}

	public CarbonConcentration3DScalar(double[] carbonConcentration) {
		this(carbonConcentration[0], carbonConcentration[1], carbonConcentration[2]);
	}

	/**
	 * Create a state vector with 851 GtC in atmosphere, 460 GtC in shallow ocean, 1740 in lower ocean.
	 */
	public CarbonConcentration3DScalar() {
		this(851, 460, 1740);		// GtC
	}

	@Override
	public Double getExpectedCarbonConcentrationInAtmosphere() {
		return carbonConcentrationInAtmosphere;
	}

	@Override
	public RandomVariable getCarbonConcentrationInAtmosphere() {
		return Scalar.of(carbonConcentrationInAtmosphere);
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
