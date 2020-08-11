package net.finmath.equities.models;

/**
 * Interface for a shifted volatility surface (used in Vega calculations).
 *
 * @author Andreas Grotz
 */

public interface IShiftedVolatilitySurface extends IVolatilitySurface {

	double getShift();
}
