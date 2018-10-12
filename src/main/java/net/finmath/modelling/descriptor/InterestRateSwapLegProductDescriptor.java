package net.finmath.modelling.descriptor;

import java.util.Arrays;

import net.finmath.modelling.InterestRateProductDescriptor;

/**
 * Describes an interest rate swap.
 *
 * @author Christian Fries
 * @author Roland Bachl
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

	public InterestRateSwapLegProductDescriptor(String forwardCurveName, String discountCurveName, ScheduleDescriptor legSchedule, double notional, double spread, boolean isNotionalExchanged) {
		super();
		this.forwardCurveName = forwardCurveName;
		this.discountCurveName = discountCurveName;
		
		this.legSchedule = legSchedule;
		double[] notionals = new double[legSchedule.getNumberOfPeriods()];
		Arrays.fill(notionals, notional);
		this.notionals = notionals;
		double[] spreads = new double[legSchedule.getNumberOfPeriods()];
		Arrays.fill(spreads, spread);
		this.spreads = spreads;
		//		this.couponFlow = couponFlow;
		this.isNotionalExchanged = isNotionalExchanged;
		//		this.isNotionalAccruing = isNotionalAccruing;
	}

	public InterestRateSwapLegProductDescriptor(String forwardCurveName, String discountCurveName, ScheduleDescriptor legSchedule, double[] notionals, double[] spreads, boolean isNotionalExchanged) {
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

	public static String getProductname() {
		return productName;
	}



	public String getForwardCurveName() {
		return forwardCurveName;
	}

	public String getDiscountCurveName() {
		return discountCurveName;
	}

	public ScheduleDescriptor getLegScheduleDescriptor() {
		return legSchedule;
	}
	
	public double[] getNotionals() {
		return notionals.clone();
	}



	//	public AbstractNotional getNotional() {
	//		return notional;
	//	}



	public double[] getSpreads() {
		return spreads;
	}



	//	public boolean isCouponFlow() {
	//		return couponFlow;
	//	}



	public boolean isNotionalExchanged() {
		return isNotionalExchanged;
	}



	//	public boolean isNotionalAccruing() {
	//		return isNotionalAccruing;
	//	}



	@Override
	public Integer version() {
		return 1;
	}

	@Override
	public String name() {
		return productName;
	}

}
