/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 28.02.2015
 */

package net.finmath.montecarlo.interestrate.products;


import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.products.components.Notional;
import net.finmath.montecarlo.interestrate.products.indices.AbstractIndex;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.Schedule;

/**
 * Create a swap from schedules, notional, indices and spreads (fixed coupons).
 *
 * The getValue method of this class simple returns
 * <code>
 * 	legReceiver.getValue(evaluationTime, model).sub(legPayer.getValue(evaluationTime, model))
 * </code>
 * where <code>legReceiver</code> and <code>legPayer</code> are {@link net.finmath.montecarlo.interestrate.products.SwapLeg}s.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class Swap extends AbstractTermStructureMonteCarloProduct {

	private final TermStructureMonteCarloProduct legReceiver;
	private final TermStructureMonteCarloProduct legPayer;

	/**
	 * Create a swap which values as <code>legReceiver - legPayer</code>.
	 *
	 * @param legReceiver The receiver leg.
	 * @param legPayer The payer leg.
	 */
	public Swap(final TermStructureMonteCarloProduct legReceiver, final TermStructureMonteCarloProduct legPayer) {
		super();
		this.legReceiver = legReceiver;
		this.legPayer = legPayer;
	}

	/**
	 * Create a swap from schedules, notional, indices and spreads (fixed coupons).
	 *
	 * @param notional The notional.
	 * @param scheduleReceiveLeg The period schedule for the receiver leg.
	 * @param indexReceiveLeg The index of the receiver leg, may be null if no index is received.
	 * @param spreadReceiveLeg The constant spread or fixed coupon rate of the receiver leg.
	 * @param schedulePayLeg The period schedule for the payer leg.
	 * @param indexPayLeg The index of the payer leg, may be null if no index is paid.
	 * @param spreadPayLeg The constant spread or fixed coupon rate of the payer leg.
	 */
	public Swap(final Notional notional,
			final Schedule scheduleReceiveLeg,
			final AbstractIndex indexReceiveLeg, final double spreadReceiveLeg,
			final Schedule schedulePayLeg, final AbstractIndex indexPayLeg,
			final double spreadPayLeg) {
		super();

		legReceiver = new SwapLeg(scheduleReceiveLeg, notional, indexReceiveLeg, spreadReceiveLeg, false);
		legPayer = new SwapLeg(schedulePayLeg, notional, indexPayLeg, spreadPayLeg, false);
	}

	/**
	 * Create a payer swap from idealized data.
	 *
	 * @param fixingDates Vector of fixing dates
	 * @param paymentDates Vector of payment dates (must have same length as fixing dates)
	 * @param swaprates Vector of strikes (must have same length as fixing dates)
	 * @deprecated This constructor is deprecated. If you like to create a payer swap from fixingDates, paymentDates and swaprates use {@link net.finmath.montecarlo.interestrate.products.SimpleSwap}.
	 */
	@Deprecated
	public Swap(
			final double[] fixingDates,
			final double[] paymentDates,
			final double[] swaprates) {
		super();
		legReceiver = new SimpleSwap(fixingDates, paymentDates, swaprates);
		legPayer	= null;
	}

	@Override
	public RandomVariable getValue(final double evaluationTime, final TermStructureMonteCarloSimulationModel model) throws CalculationException {
		RandomVariable value = legReceiver.getValue(evaluationTime, model);
		if(legPayer != null) {
			value = value.sub(legPayer.getValue(evaluationTime, model));
		}

		return value;
	}

	@Override
	public String toString() {
		return "Swap [legReceiver=" + legReceiver + ", legPayer=" + legPayer + "]";
	}
}
