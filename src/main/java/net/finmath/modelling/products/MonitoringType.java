package net.finmath.modelling.products;

/**
 * Monitoring convention for barrier-style products.
 *
 * <p>
 * CONTINUOUS means that the barrier is enforced continuously in time
 * through the PDE formulation.
 * DISCRETE means that barrier activation / deactivation is checked only
 * on a prescribed set of monitoring dates.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public enum MonitoringType {
    /**
     * The continuous.
     */
    CONTINUOUS,
    /**
     * The discrete.
     */
    DISCRETE
}
