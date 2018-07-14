/*
 * Created on 12.10.2007
 *
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 */
package net.finmath.rootfinder;

/**
 * @author Christian Fries
 */
public abstract class AbstractRootFinder implements RootFinder, RootFinderWithDerivative {

	@Override
	public void setValueAndDerivative(double value, double derivative) {
		setValue(value);
	}
}

