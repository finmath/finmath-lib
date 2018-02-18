/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 23.03.2014
 */

package net.finmath.integration;

import net.finmath.compatibility.java.util.function.DoubleUnaryOperator;

/**
 * A real integral with lower and upper integration bounds.
 * 
 * @author Christian Fries
 */
public abstract class AbstractRealIntegral implements RealIntegralInterface {

	private double lowerBound;
	private double upperBound;
	

	/**
	 * Create a real integral with lower and upper integration bounds.
	 * 
	 * @param lowerBound Lower integration bound.
	 * @param upperBound Upper integration bound.
	 */
	public AbstractRealIntegral(double lowerBound, double upperBound) {
		super();
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
	}


	/**
	 * Get the lower integration bound.
	 * 
	 * @return the lower integration bound.
	 */
	public double getLowerBound() {
		return lowerBound;
	}

	/**
	 * Get the upper integration bound.
	 * 
	 * @return the upper integration bound.
	 */
	public double getUpperBound() {
		return upperBound;
	}

	/* (non-Javadoc)
	 * @see net.finmath.integration.RealIntegralInterface#integrate(java.util.function.DoubleUnaryOperator)
	 */
	@Override
	public abstract double integrate(DoubleUnaryOperator integrand);

}
