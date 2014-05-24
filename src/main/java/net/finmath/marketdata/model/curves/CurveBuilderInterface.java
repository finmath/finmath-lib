/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 21.05.2014
 */

package net.finmath.marketdata.model.curves;


/**
 * @author Christian Fries
 *
 */
public interface CurveBuilderInterface {

	/**
	 * Build the curve. The method returns the curve object.
	 * The builder cannot be used to build another curve. Use clone instead.
	 * 
	 * @return The curve according to the specification.
	 * @throws CloneNotSupportedException 
	 */
	CurveInterface build() throws CloneNotSupportedException;

	/**
	 * Add a point to the curve.
	 * 
	 * @param time The time of the corresponding point.
	 * @param value The value of the corresponding point.
	 * @param isParameter A boolean, specifying weather the point should be considered a free parameter (true) or not (false). Fee parameters can be used to create a clone with modified values, see {@link #getCloneForParameter(double[])}
	 * @return A self reference to this curve build object.
	 */
	CurveBuilderInterface addPoint(double time, double value, boolean isParameter);

}