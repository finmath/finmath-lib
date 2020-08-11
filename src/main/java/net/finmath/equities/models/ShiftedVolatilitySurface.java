package net.finmath.equities.models;

/**
 * Interface for a shifted volatility surface (used in Vega calculations).
 *
 * @author Andreas Grotz
 */

public interface ShiftedVolatilitySurface extends VolatilitySurface {

	double getShift();
}
