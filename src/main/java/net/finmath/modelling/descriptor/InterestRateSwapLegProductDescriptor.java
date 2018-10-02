package net.finmath.modelling.descriptor;

import net.finmath.modelling.InterestRateProductDescriptor;
import net.finmath.time.ScheduleInterface;

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
	
	private final ScheduleInterface				legSchedule;
	// analytic implementation defaults notional to 1
	//	private final AbstractNotional				notional;
	private final double						spread;
	//	private final boolean						couponFlow;
	private final boolean						isNotionalExchanged;
	//	private final boolean						isNotionalAccruing;


	public InterestRateSwapLegProductDescriptor(String forwardCurveName, String discountCurveName, ScheduleInterface legSchedule, double spread, boolean isNotionalExchanged) {
		super();
		this.forwardCurveName = forwardCurveName;
		this.discountCurveName = discountCurveName;
		
		this.legSchedule = legSchedule;
		//		this.notional = notional;
		this.spread = spread;
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

	public ScheduleInterface getLegSchedule() {
		return legSchedule;
	}



	//	public AbstractNotional getNotional() {
	//		return notional;
	//	}



	public double getSpread() {
		return spread;
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
