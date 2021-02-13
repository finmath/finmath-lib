/*
 * Created on 03.09.2006
 *
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 */
package net.finmath.montecarlo.interestrate.products.indices;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.products.components.AbstractProductComponent;
import net.finmath.stochastic.RandomVariable;

/**
 * Base class for indices.
 *
 * Indices are small functions mapping time and a vector of
 * random variables to a random variable, where the time
 * is the fixing time of the index.
 *
 * @author Christian Fries
 * @version 1.0
 */
public abstract class AbstractIndex extends AbstractProductComponent {

	private static final long serialVersionUID = 7992943924779922710L;

	private final String name;

	/**
	 * Initialize name and currency of an index.
	 *
	 * @param name The name of an index. Used to map an index on a curve.
	 * @param currency The natural currency of an index. This more for compatibility purposes, since the information sould be contained in the name.
	 */
	public AbstractIndex(final String name, final String currency) {
		super(currency);
		this.name = name;
	}

	/**
	 * Initialize the name of an index.
	 *
	 * @param name The name of an index. Used to map an index on a curve.
	 */
	public AbstractIndex(final String name) {
		this(name, null);
	}

	/**
	 * Initialize an abstract index which does not have a dedicated name or currency,
	 * e.g. a function of other indicies.
	 */
	public AbstractIndex() {
		this(null, null);
	}

	@Override
	public abstract RandomVariable getValue(double fixingTime, TermStructureMonteCarloSimulationModel model) throws CalculationException;

	/**
	 * Returns the name of the index.
	 *
	 * @return The name of the index.
	 */
	public String getName() {
		return name;
	}
}
