/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 17.02.2013
 */
package net.finmath.marketdata2.model.curves;

import java.time.LocalDate;

import net.finmath.stochastic.RandomVariable;

/**
 * Abstract base class for a curve. It stores the name of the curve and
 * provides some convenient way of getting values.
 *
 * @author Christian Fries
 * @version 1.0
 */
public abstract class AbstractCurve implements Curve, Cloneable {

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
	public RandomVariable getValue(final double time) {
		return getValue(null, time);
	}

	/**
	 * Return a vector of values corresponding to a given vector of times.
	 * @param times A given vector of times.
	 * @return A vector of values corresponding to the given vector of times.
	 */
	public RandomVariable[] getValues(final double[] times) {
		final RandomVariable[] values = new RandomVariable[times.length];

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
	public Curve getCloneForParameter(final RandomVariable[] value) throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	@Override
	public String toString() {
		return "AbstractCurve [name=" + name + ", referenceDate=" + referenceDate + "]";
	}
}
