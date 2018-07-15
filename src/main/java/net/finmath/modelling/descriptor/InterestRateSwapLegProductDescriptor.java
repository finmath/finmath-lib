package net.finmath.modelling.descriptor;

import net.finmath.modelling.InterestRateProductDescriptor;
import net.finmath.montecarlo.interestrate.products.components.AbstractNotional;
import net.finmath.montecarlo.interestrate.products.indices.AbstractIndex;
import net.finmath.time.ScheduleInterface;

/**
 * Describes an interest rate swap.
 *
 * @author Christian Fries
 * @author Roland Bachl
 */
public class InterestRateSwapLegProductDescriptor implements InterestRateProductDescriptor {

	private static final String productName = "Interest Rate Swap Leg";

	private final ScheduleInterface				legSchedule;
	private final AbstractNotional				notional;
	private final AbstractIndex					index;
	private final double						spread;
	private final boolean						couponFlow;
	private final boolean						isNotionalExchanged;
	private final boolean						isNotionalAccruing;


	public InterestRateSwapLegProductDescriptor(ScheduleInterface legSchedule, AbstractNotional notional,
			AbstractIndex index, double spread, boolean couponFlow, boolean isNotionalExchanged,
			boolean isNotionalAccruing) {
		super();
		this.legSchedule = legSchedule;
		this.notional = notional;
		this.index = index;
		this.spread = spread;
		this.couponFlow = couponFlow;
		this.isNotionalExchanged = isNotionalExchanged;
		this.isNotionalAccruing = isNotionalAccruing;
	}

	public static String getProductname() {
		return productName;
	}



	public ScheduleInterface getLegSchedule() {
		return legSchedule;
	}



	public AbstractNotional getNotional() {
		return notional;
	}



	public AbstractIndex getIndex() {
		return index;
	}



	public double getSpread() {
		return spread;
	}



	public boolean isCouponFlow() {
		return couponFlow;
	}



	public boolean isNotionalExchanged() {
		return isNotionalExchanged;
	}



	public boolean isNotionalAccruing() {
		return isNotionalAccruing;
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
