package net.finmath.modelling.descriptor;

import java.time.LocalDate;

import net.finmath.modelling.InterestRateProductDescriptor;

/**
 * Product descriptor for an interest rate swaption.
 *
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public class InterestRateSwaptionProductDescriptor implements InterestRateProductDescriptor {

	private static final String productName = "Interest Rate Swap";

	private final InterestRateSwapProductDescriptor swap;
	private final LocalDate excerciseDate;
	private final double strikeRate;

	/**
	 * Construct the descriptor of a swaption from the descriptor of a swap plus option parameters.
	 *
	 * @param swap Descriptor of the underlying swap.
	 * @param excerciseDate Exercise date of the option as abolute LocalDate.
	 * @param strikeRate Strike rate of the option.
	 */
	public InterestRateSwaptionProductDescriptor(final InterestRateSwapProductDescriptor swap, final LocalDate excerciseDate,
			final double strikeRate) {
		super();
		this.swap = swap;
		this.excerciseDate = excerciseDate;
		this.strikeRate = strikeRate;
	}

	@Override
	public Integer version() {
		return 1;
	}

	@Override
	public String name() {
		return productName;
	}

	/**
	 * Return the descriptor of the underlying swap.
	 *
	 * @return THe swap descriptor.
	 */
	public InterestRateSwapProductDescriptor getUnderlyingSwap() {
		return swap;
	}

	/**
	 * Return the exercise date of the option.
	 *
	 * @return The exercise date as absolute LocalDate.
	 */
	public LocalDate getExcerciseDate() {
		return excerciseDate;
	}

	/**
	 * Return the strike rate of the option.
	 *
	 * @return The strike rate.
	 */
	public double getStrikeRate() {
		return strikeRate;
	}

}
