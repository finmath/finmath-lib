/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 17.02.2013
 */
package net.finmath.marketdata.model.curves;

import java.util.Calendar;

import net.finmath.time.daycount.DayCountConventionInterface;
import net.finmath.time.daycount.DayCountConvention_ACT_365;


/**
 * Abstract base class for a curve. It stores the name of the curve and provides some convenient way of getting
 * values.
 *  
 * @author Christian Fries
 */
public abstract class AbstractCurve implements CurveInterface {

	private static	DayCountConventionInterface	internalDayCounting = new DayCountConvention_ACT_365();//ACT_ISDA();
	private			Calendar					referenceDate;

	private final String					name;

	public AbstractCurve(String name, Calendar referenceDate) {
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

	public Calendar getReferenceDate() {
		return referenceDate;
	}

	public void setReferenceDate(Calendar referenceDate) {
		this.referenceDate = referenceDate;
	}

	/* (non-Javadoc)
	 * @see net.finmath.marketdata.model.curves.CurveInterface#getValue(double)
	 */
	@Override
    public double getValue(double time) {
		return getValue(null, time);
	}

	/**
	 * Return a vector of values corresponding to a given vector of times.
	 * @param times A given vector of times.
	 * @return A vector of values corresponding to the given vector of times.
	 */
	public double[] getValues(double[] times) {
		double[] values = new double[times.length];

		for(int i=0; i<times.length; i++) values[i] = getValue(null, times[i]);
		
		return values;
	}

	public String toString() {
		return super.toString() + "\n\"" + this.getName() + "\"";
	}

	/* (non-Javadoc)
	 * @see net.finmath.marketdata.model.curves.CurveInterface#getCloneForParameter(net.finmath.marketdata.model.AnalyticModelInterface, double[])
	 */
	@Override
	public CurveInterface getCloneForParameter(double[] value) throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}
}