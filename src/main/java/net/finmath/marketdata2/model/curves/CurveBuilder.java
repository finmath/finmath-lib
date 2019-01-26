/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 21.05.2014
 */

package net.finmath.marketdata2.model.curves;

import net.finmath.stochastic.RandomVariable;


/**
 * Interface of builders which allow to build curve objects by successively adding
 * points.
 *
 * Points cannot be added directly to a curve since curve objects are immutable
 * (cannot be changed at a later time, i.e., after construction).
 * Instead, a clone with an additional point is created. To efficiently create
 * a new curve by successively adding points use this builder.
 *
 * @author Christian Fries
 * @version 1.0
 */
public interface CurveBuilder {

	/**
	 * Build the curve. The method returns the curve object.
	 * The builder cannot be used to build another curve. Use clone instead.
	 *
	 * @return The curve according to the specification.
	 * @throws CloneNotSupportedException Thrown if the curve could not be build (likely due to a template throwing {@link CloneNotSupportedException}.
	 */
	Curve build() throws CloneNotSupportedException;

	/**
	 * Add a point to the curve.
	 *
	 * @param time The time of the corresponding point.
	 * @param value The value of the corresponding point.
	 * @param isParameter A boolean, specifying weather the point should be considered a free parameter (true) or not (false). Fee parameters can be used to create a clone with modified values, see {@link CurveInterpolation#getCloneForParameter(RandomVariable[])}
	 * @return A self reference to this curve build object.
	 */
	CurveBuilder addPoint(double time, RandomVariable value, boolean isParameter);
}
