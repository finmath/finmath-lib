package net.finmath.modelling.descriptor;

import java.util.Arrays;

import net.finmath.modelling.InterestRateProductDescriptor;

/**
 * Product descriptor for an interest rate swap leg.
 *
 * @author Christian Fries
 * @author Roland Bachl
 * @version 1.0
 */
public class InterestRateSwapLegProductDescriptor implements InterestRateProductDescriptor {

	private static final String productName = "Interest Rate Swap Leg";

	private final String forwardCurveName;
	private final String discountCurveName;

	private final ScheduleDescriptor			legSchedule;
	private final double[]						notionals;
	private final double[]						spreads;
	//	private final boolean						couponFlow;
	private final boolean						isNotionalExchanged;
	//	private final boolean						isNotionalAccruing;

	/**
	 * Create the descriptor with period uniform notional and spread.
	 *
	 * @param forwardCurveName The name of the forward curve this leg is quoted on. (Or null/empty)
	 * @param discountCurveName The name of the curve this leg is to be discounted with.
	 * @param legSchedule ScheduleFromPeriods of the leg.
	 * @param notional The notional.
	 * @param spread Fixed spread on the forward or fix rate.
	 * @param isNotionalExchanged If true, the leg will pay notional at the beginning of the swap and receive notional at the end of the swap.
	 */
	public InterestRateSwapLegProductDescriptor(final String forwardCurveName, final String discountCurveName, final ScheduleDescriptor legSchedule, final double notional, final double spread, final boolean isNotionalExchanged) {
		super();
		this.forwardCurveName = forwardCurveName;
		this.discountCurveName = discountCurveName;

		this.legSchedule = legSchedule;
		final double[] notionals = new double[legSchedule.getNumberOfPeriods()];
		Arrays.fill(notionals, notional);
		this.notionals = notionals;
		final double[] spreads = new double[legSchedule.getNumberOfPeriods()];
		Arrays.fill(spreads, spread);
		this.spreads = spreads;
		//		this.couponFlow = couponFlow;
		this.isNotionalExchanged = isNotionalExchanged;
		//		this.isNotionalAccruing = isNotionalAccruing;
	}

	/**
	 * Create the descriptor with notional and spread variable between periods.
	 *
	 * @param forwardCurveName The name of the forward curve this leg is quoted on. (Or null/empty)
	 * @param discountCurveName The name of the curve this leg is to be discounted with.
	 * @param legSchedule ScheduleFromPeriods of the leg.
	 * @param notionals Array of notionals for each period.
	 * @param spreads Array of fixed spreads on the forward or fix rates for each period.
	 * @param isNotionalExchanged If true, the leg will pay notional at the beginning of the swap and receive notional at the end of the swap.
	 */
	public InterestRateSwapLegProductDescriptor(final String forwardCurveName, final String discountCurveName, final ScheduleDescriptor legSchedule, final double[] notionals, final double[] spreads, final boolean isNotionalExchanged) {
		super();
		this.forwardCurveName = forwardCurveName;
		this.discountCurveName = discountCurveName;

		this.legSchedule = legSchedule;
		this.notionals = notionals;
		this.spreads = spreads;
		//		this.couponFlow = couponFlow;
		this.isNotionalExchanged = isNotionalExchanged;
		//		this.isNotionalAccruing = isNotionalAccruing;
	}

	/**
	 * Return the name of the forward curve in this descriptor.
	 *
	 * @return Name of the forward curve.
	 */
	public String getForwardCurveName() {
		return forwardCurveName;
	}

	/**
	 * Return the name of the discount curve in this descriptor.
	 *
	 * @return Name of the discount curve.
	 */
	public String getDiscountCurveName() {
		return discountCurveName;
	}

	/**
	 * Return the descriptor of the schedule of this product descriptor.
	 *
	 * @return The schedule descriptor.
	 */
	public ScheduleDescriptor getLegScheduleDescriptor() {
		return legSchedule;
	}

	/**
	 * Return the notionals per period of this descriptor.
	 *
	 * @return Array of notionals.
	 */
	public double[] getNotionals() {
		return notionals.clone();
	}

	/**
	 * Return the spreads per period of this descriptor.
	 *
	 * @return Array of spreads.
	 */
	public double[] getSpreads() {
		return spreads;
	}

	/**
	 * Indicates whether the leg exchanges notional.
	 *
	 * @return true, if the leg pays notional at the beginning of the swap and reveives notional at the end.
	 */
	public boolean isNotionalExchanged() {
		return isNotionalExchanged;
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
