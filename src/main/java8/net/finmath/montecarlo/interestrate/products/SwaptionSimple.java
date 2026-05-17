/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 15.02.2004
 */
package net.finmath.montecarlo.interestrate.products;

import net.finmath.exception.CalculationException;
import net.finmath.functions.AnalyticFormulas;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.products.Swap;
import net.finmath.marketdata.products.SwapAnnuity;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.RegularSchedule;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * Implements the valuation of a simplified (idealized) swaption under a
 * LIBORModelMonteCarloSimulationModel
 *
 * @author Christian Fries
 * @version 1.2
 */
public class SwaptionSimple extends AbstractTermStructureMonteCarloProduct implements net.finmath.modelling.products.Swaption {

	private final TimeDiscretization	tenor;
	private final double						swaprate;
	private final Swaption						swaption;
	private final ValueUnit						valueUnit;

	/**
	 * Note: It is implicitly assumed that swapTenor[0] is the exercise date (no forward starting).
	 * @param swaprate The strike swaprate of the swaption.
	 * @param swapTenor The swap tenor in doubles.
	 */
	public SwaptionSimple(final double swaprate, final TimeDiscretization swapTenor) {
		this(swaprate, swapTenor.getAsDoubleArray(), ValueUnit.VALUE);
	}

	/**
	 * Note: It is implicitly assumed that swapTenor[0] is the exercise date (no forward starting).
	 * @param swaprate The strike swaprate of the swaption.
	 * @param swapTenor The swap tenor in doubles.
	 * @param valueUnit See <code>getValue(AbstractLIBORMarketModel model)</code>
	 */
	public SwaptionSimple(final double swaprate, final double[] swapTenor, final ValueUnit valueUnit) {
		super();
		tenor = new TimeDiscretizationFromArray(swapTenor);
		this.swaprate = swaprate;
		swaption	= new Swaption(swapTenor[0], tenor, swaprate);
		this.valueUnit	= valueUnit;
	}

	/**
	 * This method returns the value random variable of the product within the specified model, evaluated at a given evalutationTime.
	 * Note: For a lattice this is often the value conditional to evalutationTime, for a Monte-Carlo simulation this is the (sum of) value discounted to evaluation time.
	 * Cashflows prior evaluationTime are not considered.
	 *
	 * @param evaluationTime The time on which this products value should be observed.
	 * @param model The model used to price the product.
	 * @return The random variable representing the value of the product discounted to evaluation time
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	@Override
	public RandomVariable getValue(final double evaluationTime, final TermStructureMonteCarloSimulationModel model) throws CalculationException {
		final RandomVariable value = swaption.getValue(evaluationTime, model);

		if(valueUnit == ValueUnit.VALUE) {
			return value;
		}

		final ForwardCurve forwardCurve	 = model.getModel().getForwardRateCurve();
		final DiscountCurve discountCurve = model.getModel().getAnalyticModel() != null ? model.getModel().getAnalyticModel().getDiscountCurve(forwardCurve.getDiscountCurveName()) : null;

		final double parSwaprate = Swap.getForwardSwapRate(new RegularSchedule(tenor), new RegularSchedule(tenor), forwardCurve, model.getModel().getAnalyticModel());
		final double optionMaturity = tenor.getTime(0);
		final double strikeSwaprate = swaprate;
		final double swapAnnuity = discountCurve != null ? SwapAnnuity.getSwapAnnuity(tenor, discountCurve) : SwapAnnuity.getSwapAnnuity(tenor, forwardCurve);

		if(valueUnit == ValueUnit.VOLATILITYLOGNORMAL || valueUnit == ValueUnit.VOLATILITY) {
			final double volatility = AnalyticFormulas.blackScholesOptionImpliedVolatility(parSwaprate, optionMaturity, strikeSwaprate, swapAnnuity, value.getAverage());
			return model.getRandomVariableForConstant(volatility);
		}
		else if(valueUnit == ValueUnit.VOLATILITYNORMAL) {
			final double volatility = AnalyticFormulas.bachelierOptionImpliedVolatility(parSwaprate, optionMaturity, strikeSwaprate, swapAnnuity, value.getAverage());
			return model.getRandomVariableForConstant(volatility);
		}
		else if(valueUnit == ValueUnit.INTEGRATEDVARIANCELOGNORMAL || valueUnit == ValueUnit.INTEGRATEDVARIANCE  || valueUnit == ValueUnit.INTEGRATEDLOGNORMALVARIANCE) {
			final double volatility = AnalyticFormulas.blackScholesOptionImpliedVolatility(parSwaprate, optionMaturity, strikeSwaprate, swapAnnuity, value.getAverage());
			return model.getRandomVariableForConstant(volatility * volatility * optionMaturity);
		}
		else if(valueUnit == ValueUnit.INTEGRATEDVARIANCENORMAL || valueUnit == ValueUnit.INTEGRATEDNORMALVARIANCE) {
			final double volatility = AnalyticFormulas.bachelierOptionImpliedVolatility(parSwaprate, optionMaturity, strikeSwaprate, swapAnnuity, value.getAverage());
			return model.getRandomVariableForConstant(volatility * volatility * optionMaturity);
		}
		else {
			throw new UnsupportedOperationException("Provided valueUnit not implemented.");
		}
	}

	@Override
	public String toString() {
		return "SwaptionSimple [tenor=" + tenor + ", swaprate=" + swaprate
				+ ", valueUnit=" + valueUnit + "]";
	}
}
