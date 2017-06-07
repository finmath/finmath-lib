/*
 * Created on 12.10.2007
 *
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 */
package net.finmath.math.rootfinder;

/**
 * @author Christian Fries
 */
public abstract class AbstractRootFinder implements RootFinder, RootFinderWithDerivative {

    @Override
    public void setValueAndDerivative(double value, double derivative) {
        setValue(value);
    }
}
