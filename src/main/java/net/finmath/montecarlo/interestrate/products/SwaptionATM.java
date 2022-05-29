package net.finmath.montecarlo.interestrate.products;

import net.finmath.exception.CalculationException;
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
 * A lightweight ATM swaption product used for calibration.
 *
 * @author Stefan Sedlmair
 * @author Christian Fries
 * @version 1.0
 */
public class SwaptionATM extends AbstractTermStructureMonteCarloProduct implements net.finmath.modelling.products.Swaption {

	private final TimeDiscretization	tenor;
	private final ValueUnit						valueUnit;

	public SwaptionATM(final double[] swapTenor, final ValueUnit valueUnit) {
		super();
		tenor = new TimeDiscretizationFromArray(swapTenor);
		this.valueUnit = valueUnit;
	}

	@Override
	public RandomVariable getValue(final double evaluationTime, final TermStructureMonteCarloSimulationModel model) throws CalculationException {

		final ForwardCurve forwardCurve	 = model.getModel().getForwardRateCurve();
		final DiscountCurve discountCurve = model.getModel().getAnalyticModel() != null ? model.getModel().getAnalyticModel().getDiscountCurve(forwardCurve.getDiscountCurveName()) : null;

		final double optionMaturity = tenor.getTime(0);
		final double swapAnnuity = discountCurve != null ? SwapAnnuity.getSwapAnnuity(tenor, discountCurve) : SwapAnnuity.getSwapAnnuity(tenor, forwardCurve);

		// Swaption is per definition at the money in this class
		final double parSwapRate = Swap.getForwardSwapRate(new RegularSchedule(tenor), new RegularSchedule(tenor), forwardCurve, model.getModel().getAnalyticModel());

		// define an atm swaption
		final TermStructureMonteCarloProduct swaption = new Swaption(tenor.getTime(0), tenor, parSwapRate);

		// get swaption value
		final RandomVariable optionValue = swaption.getValue(evaluationTime, model);

		switch (valueUnit) {
		case VALUE:
			return optionValue;
		case VOLATILITYNORMAL:
			return getImpliedBachelierATMOptionVolatility(optionValue, optionMaturity, swapAnnuity);
		case INTEGRATEDVARIANCENORMAL:
		case INTEGRATEDNORMALVARIANCE:
			return getImpliedBachelierATMOptionVolatility(optionValue, optionMaturity, swapAnnuity).squared().mult(optionMaturity);
		default:
			throw new IllegalArgumentException("Unknown valueUnit: " + valueUnit.name());
		}
	}

	/**
	 * 	Calculates ATM Bachelier implied volatilities.
	 *
	 * @see net.finmath.functions.AnalyticFormulas#bachelierOptionImpliedVolatility(double, double, double, double, double)
	 *
	 * @param optionValue RandomVarable representing the value of the option
	 * @param optionMaturity Time to maturity.
	 * @param swapAnnuity The swap annuity as seen on valuation time.
	 * @return The Bachelier implied volatility.
	 */
	public RandomVariable getImpliedBachelierATMOptionVolatility(final RandomVariable optionValue, final double optionMaturity, final double swapAnnuity){
		return optionValue.average().mult(Math.sqrt(2.0 * Math.PI / optionMaturity) / swapAnnuity);
	}
}
