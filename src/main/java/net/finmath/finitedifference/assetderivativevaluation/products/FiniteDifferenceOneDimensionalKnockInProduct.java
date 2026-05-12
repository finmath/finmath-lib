package net.finmath.finitedifference.assetderivativevaluation.products;

import net.finmath.modelling.products.BarrierType;

/**
 * Interface for one-dimensional finite-difference knock-in products.
 *
 * <p>
 * A knock-in product is characterized by a maturity <i>T</i>, a barrier level
 * <i>B</i>,
 * and a barrier type defining the activation region. Let <i>S(t)</i> denote the
 * underlying.
 * The contract is inactive before the barrier event and becomes active once the
 * barrier is
 * hit. For a down barrier, activation occurs when
 * </p>
 *
 * <p>
 * <i>S(t) &le; B</i>,
 * </p>
 *
 * <p>
 * while for an up barrier, activation occurs when
 * </p>
 *
 * <p>
 * <i>S(t) &ge; B</i>.
 * </p>
 *
 * <p>
 * In a one-dimensional finite-difference implementation, such products are
 * typically handled
 * by splitting the valuation into inactive and active regimes, or by augmenting
 * the state
 * space with an activation flag. The inactive value at maturity prescribes the
 * terminal
 * condition in the no-hit region. For standard barrier options, this quantity
 * is often the
 * rebate; for digital knock-in products it is typically zero.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public interface FiniteDifferenceOneDimensionalKnockInProduct {

	/**
	 * Returns the maturity.
	 *
	 * @return The maturity.
	 */
	double getMaturity();

	/**
	 * Returns the barrier level.
	 *
	 * @return The barrier level.
	 */
	double getBarrierValue();

	/**
	 * Returns the barrier type.
	 *
	 * @return The barrier type.
	 */
	BarrierType getBarrierType();

	/**
	 * Returns the value prescribed in the inactive regime at maturity.
	 *
	 * <p>
	 * This is the terminal value in the no-hit region. For vanilla barrier
	 * options this is
	 * typically the rebate. For digital barrier options it is typically {@code
	 * 0.0}.
	 * </p>
	 *
	 * @return The inactive value at maturity.
	 */
	double getInactiveValueAtMaturity();
}
