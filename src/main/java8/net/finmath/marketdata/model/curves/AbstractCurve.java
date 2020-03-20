/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 17.02.2013
 */
package net.finmath.marketdata.model.curves;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Abstract base class for a curve. It stores the name of the curve and
 * provides some convenient way of getting values.
 *
 * @author Christian Fries
 * @version 1.0
 */
public abstract class AbstractCurve implements Curve, Serializable, Cloneable {

	private static final long serialVersionUID = 584342257323693502L;

	private	final	LocalDate	referenceDate;
	private final	String		name;

	/**
	 * @param name The name of this curve.
	 * @param referenceDate The reference date of this curve.
	 */
	public AbstractCurve(final String name, final LocalDate referenceDate) {
		super();
		this.name = name;
		this.referenceDate = referenceDate;
	}

	/* (non-Javadoc)
	 * @see net.finmath.marketdata.model.curves.CurveInterface#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	@Override
	public LocalDate getReferenceDate() {
		return referenceDate;
	}

	/* (non-Javadoc)
	 * @see net.finmath.marketdata.model.curves.CurveInterface#getValue(double)
	 */
	@Override
	public double getValue(final double time) {
		return getValue(null, time);
	}

	/**
	 * Return a vector of values corresponding to a given vector of times.
	 * @param times A given vector of times.
	 * @return A vector of values corresponding to the given vector of times.
	 */
	public double[] getValues(final double[] times) {
		final double[] values = new double[times.length];

		for(int i=0; i<times.length; i++) {
			values[i] = getValue(null, times[i]);
		}

		return values;
	}

	@Override
	public AbstractCurve clone() throws CloneNotSupportedException {
		return (AbstractCurve)super.clone();
	}

	@Override
	public Curve getCloneForParameter(final double[] value) throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	@Override
	public String toString() {
		return "AbstractCurve [name=" + name + ", referenceDate=" + referenceDate + "]";
	}
}
