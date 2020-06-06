package net.finmath.montecarlo.process;

import java.time.LocalDateTime;

import net.finmath.time.TimeDiscretization;

/**
 * An object implementing this interfaces provides a suggestion for an optimal time-discretization
 * associated with this object.
 *
 * Examples are:
 * <ul>
 * 	<li>
 * 		a European option can implement this interface and provide a time discretization with a single point, its exercise date.
 * 	</li>
 * </ul>
 *
 * @author Christian Fries
 */
public interface ProcessTimeDiscretizationProvider {

	/**
	 * Returns a suggestion for a time discretization which is suited (or required) for the processing (e.g valuation) of this object.
	 *
	 * @param referenceDate A reference date relative to which the discretization is generated.
	 * @return A time discretization.
	 */
	TimeDiscretization getProcessTimeDiscretization(LocalDateTime referenceDate);
}
