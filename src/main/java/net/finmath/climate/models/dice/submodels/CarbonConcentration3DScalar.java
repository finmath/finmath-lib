package net.finmath.climate.models.dice.submodels;

import org.apache.commons.lang3.Validate;

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

	
	/**
	 * Create a state vector representing carbon concentration in units of GtC.
	 */
	public CarbonConcentration3DScalar(double carbonConcentrationInAtmosphere, double carbonConcentrationInShallowOcean, double carbonConcentrationInLowerOcean) {
		super();
		Validate.isTrue(carbonConcentrationInAtmosphere >= 0, "carbonConcentrationInAtmosphere must not be negative.", carbonConcentrationInAtmosphere);
		Validate.isTrue(carbonConcentrationInShallowOcean >= 0, "carbonConcentrationInShallowOcean must not be negative.", carbonConcentrationInShallowOcean);
		Validate.isTrue(carbonConcentrationInLowerOcean >= 0, "carbonConcentrationInLowerOcean must not be negative.", carbonConcentrationInLowerOcean);
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
