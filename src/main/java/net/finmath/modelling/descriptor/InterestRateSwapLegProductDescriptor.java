package net.finmath.modelling.descriptor;

import java.util.Arrays;

import net.finmath.modelling.InterestRateProductDescriptor;
import net.finmath.montecarlo.interestrate.products.components.AbstractNotional;
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
	private final double[]			notionals;
	private final double						spread;
	//	private final boolean						couponFlow;
	private final boolean						isNotionalExchanged;
	//	private final boolean						isNotionalAccruing;

	//TODO
	public InterestRateSwapLegProductDescriptor(String forwardCurveName, String discountCurveName, ScheduleInterface legSchedule, double notional, double spread, boolean isNotionalExchanged) {
		super();
		this.forwardCurveName = forwardCurveName;
		this.discountCurveName = discountCurveName;
		
		this.legSchedule = legSchedule;
		double[] notionals = new double[legSchedule.getNumberOfPeriods()];
		Arrays.fill(notionals, notional);
		this.notionals = notionals;
		this.spread = spread;
		//		this.couponFlow = couponFlow;
		this.isNotionalExchanged = isNotionalExchanged;
		//		this.isNotionalAccruing = isNotionalAccruing;
	}

	public InterestRateSwapLegProductDescriptor(String forwardCurveName, String discountCurveName, ScheduleInterface legSchedule, double[] notionals, double spread, boolean isNotionalExchanged) {
		super();
		this.forwardCurveName = forwardCurveName;
		this.discountCurveName = discountCurveName;
		
		this.legSchedule = legSchedule;
		this.notionals = notionals;
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
	
	public double[] getNotionals() {
		return notionals.clone();
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
