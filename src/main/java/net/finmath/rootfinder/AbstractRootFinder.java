/*
 * Created on 12.10.2007
 *
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 */
package net.finmath.rootfinder;

/**
 * @author Christian Fries
 * @version 1.0
 */
public abstract class AbstractRootFinder implements RootFinder, RootFinderWithDerivative {

	@Override
	public void setValueAndDerivative(final double value, final double derivative) {
		setValue(value);
	}
}
