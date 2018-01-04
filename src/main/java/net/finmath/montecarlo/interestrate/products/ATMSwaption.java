/**
 * 
 */
package net.finmath.montecarlo.interestrate.products;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.curves.DiscountCurveInterface;
import net.finmath.marketdata.model.curves.ForwardCurveInterface;
import net.finmath.marketdata.products.Swap;
import net.finmath.marketdata.products.SwapAnnuity;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.interestrate.products.SwaptionSimple.ValueUnit;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.RegularSchedule;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * @author Stefan Sedlmair
 * @version 0.1
 */
public class ATMSwaption extends AbstractLIBORMonteCarloProduct {

	private final TimeDiscretizationInterface	tenor;
	private final ValueUnit						valueUnit;

	public ATMSwaption(double[] swapTenor, ValueUnit valueUnit) {
		super();
		this.tenor = new TimeDiscretization(swapTenor);
		this.valueUnit = valueUnit;
	}
	
	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct#getValue(double, net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface)
	 */
	@Override
	public RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {
		
		ForwardCurveInterface forwardCurve	 = model.getModel().getForwardRateCurve();
		DiscountCurveInterface discountCurve = model.getModel().getAnalyticModel() != null ? model.getModel().getAnalyticModel().getDiscountCurve(forwardCurve.getDiscountCurveName()) : null;
		
		double optionMaturity = tenor.getTime(0);
		double swapAnnuity = discountCurve != null ? SwapAnnuity.getSwapAnnuity(tenor, discountCurve) : SwapAnnuity.getSwapAnnuity(tenor, forwardCurve);
		
		// Swaption is per definition at the money in this class
		double parSwapRate = Swap.getForwardSwapRate(new RegularSchedule(tenor), new RegularSchedule(tenor), forwardCurve, model.getModel().getAnalyticModel());
		
		// define an atm swaption
		AbstractLIBORMonteCarloProduct swaption = new Swaption(tenor.getTime(0), tenor, parSwapRate);
		
		// get swaption value
		RandomVariableInterface optionValue = swaption.getValue(evaluationTime, model);
		
		switch (valueUnit) {
		case VALUE:
			return optionValue;
		case VOLATILITYNORMAL:
			return getImpliedBachelierOptionVolatility(optionValue, optionMaturity, swapAnnuity);
		case INTEGRATEDNORMALVARIANCE:
			return getImpliedBachelierOptionVolatility(optionValue, optionMaturity, swapAnnuity).squared().mult(optionMaturity);
		default:
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * 	calculate ATM Bachelier Implied Volatilities
	 * 	@see net.finmath.functions.AnalyticFormulas.bachelierOptionImpliedVolatility(double, double, double, double, double)
	 * */
	public RandomVariableInterface getImpliedBachelierOptionVolatility(RandomVariableInterface optionValue, double optionMaturity, double swapAnnuity){
		return optionValue.average().div(Math.sqrt(optionMaturity / Math.PI / 2.0)).div(swapAnnuity);
	}
	
}
