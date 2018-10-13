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

	public InterestRateSwapProductDescriptor(InterestRateProductDescriptor legReceiver,
			InterestRateProductDescriptor legPayer) {
		super();
		this.legReceiver = legReceiver;
		this.legPayer = legPayer;
	}


	public InterestRateProductDescriptor getLegReceiver() {
		return legReceiver;
	}


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
