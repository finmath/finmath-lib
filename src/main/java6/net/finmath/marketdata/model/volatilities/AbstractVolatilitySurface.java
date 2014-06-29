/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 17.02.2013
 */
package net.finmath.marketdata.model.volatilities;

import java.util.Calendar;

/**
 * Abstract base class for a volatility surface. It stores the name of the surface and
 * provides some convenient way of getting values.
 *  
 * @author Christian Fries
 */
public abstract class AbstractVolatilitySurface implements VolatilitySurfaceInterface, Cloneable {

	private	final	Calendar	referenceDate;
	private final	String		name;

	public AbstractVolatilitySurface(String name, Calendar referenceDate) {
		super();
		this.name = name;
		this.referenceDate = referenceDate;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Calendar getReferenceDate() {
		return referenceDate;
	}

	@Override
	public String toString() {
		return super.toString() + "\n\"" + this.getName() + "\"";
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	@Override
	public VolatilitySurfaceInterface getCloneForParameter(double[] value) throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}
}