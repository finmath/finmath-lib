package net.finmath.modelling.descriptor;

import net.finmath.modelling.InterestRateProductDescriptor;

/**
 * Product descriptor for an interest rate swap.
 *
 * @author Christian Fries
 * @author Roland Bachl
 * @version 1.0
 */
public class InterestRateSwapProductDescriptor implements InterestRateProductDescriptor {

	private static final String productName = "Interest Rate Swap";

	private final InterestRateProductDescriptor legReceiver;
	private final InterestRateProductDescriptor legPayer;

	/**
	 * Construct a swap product descriptor from the descriptors of its legs.
	 *
	 * @param legReceiver The descriptor of the receiver leg.
	 * @param legPayer The descriptor of the payer leg.
	 */
	public InterestRateSwapProductDescriptor(final InterestRateProductDescriptor legReceiver,
			final InterestRateProductDescriptor legPayer) {
		super();
		this.legReceiver = legReceiver;
		this.legPayer = legPayer;
	}


	/**
	 * Return the descriptor of the receiver leg of this swap.
	 *
	 * @return The leg descriptor.
	 */
	public InterestRateProductDescriptor getLegReceiver() {
		return legReceiver;
	}

	/**
	 * Return the descriptor of the payer leg of this swap.
	 *
	 * @return The leg descriptor.
	 */
	public InterestRateProductDescriptor getLegPayer() {
		return legPayer;
	}

	@Override
	public Integer version() {
		return 1;
	}

	@Override
	public String name() {
		return productName;
	}

}
