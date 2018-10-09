package net.finmath.modelling.descriptor;

import java.time.LocalDate;

import net.finmath.modelling.InterestRateProductDescriptor;

/**
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public class InterestRateSwaptionProductDescriptor implements InterestRateProductDescriptor {
	
	private static final String productName = "Interest Rate Swap";
	
	private final InterestRateSwapProductDescriptor swap;
	private final LocalDate excerciseDate;
	private final double strikeRate;

	public InterestRateSwaptionProductDescriptor(InterestRateSwapProductDescriptor swap, LocalDate excerciseDate,
			double strikeRate) {
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
	
	public InterestRateSwapProductDescriptor getUnderlyingSwap() {
		return swap;
	}

	public LocalDate getExcerciseDate() {
		return excerciseDate;
	}

	public double getStrikeRate() {
		return strikeRate;
	}

}
