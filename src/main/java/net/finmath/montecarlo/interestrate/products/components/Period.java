/*
 * Created on 22.11.2009
 */
package net.finmath.montecarlo.interestrate.products.components;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * A period. A period has references to the index (coupon) and the notional.
 * It provides the fixing date for the index, the period length, and the payment date.
 * 
 * @author Christian Fries
 * @version 1.1
 */
public class Period extends AbstractPeriod {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7107623461781510475L;
	private boolean couponFlow;
	private boolean notionalFlow;
	private boolean payer;

	/**
     * @param periodStart
     * @param periodEnd
     * @param fixingDate
     * @param paymentDate
     * @param notional
     * @param index
     * @param couponFlow
     * @param notionalFlow
     * @param payer
     */
    public Period(double periodStart, double periodEnd, double fixingDate,
            double paymentDate, AbstractNotional notional, AbstractProductComponent index,
            boolean couponFlow, boolean notionalFlow, boolean payer) {
	    super(periodStart, periodEnd, fixingDate, paymentDate, notional, index);
	    this.couponFlow = couponFlow;
	    this.notionalFlow = notionalFlow;
	    this.payer = payer;
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
    public RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {        
		
    	if(evaluationTime >= this.getPaymentDate()) return new RandomVariable(0.0);

    	// Get random variables
        RandomVariableInterface	nationalAtPeriodStart	= getNotional().getNotionalAtPeriodStart(this, model);
        RandomVariableInterface	numeraireAtEval			= model.getNumeraire(evaluationTime);
        RandomVariableInterface	numeraire				= model.getNumeraire(getPaymentDate());
//        RandomVariableInterface	monteCarloProbabilities	= model.getMonteCarloWeights(getPaymentDate());

        RandomVariableInterface values;

        // Calculate numeraire relative value of coupon flows
        if(couponFlow) {
        	values = getCoupon(model);
        	values = values.mult(nationalAtPeriodStart);
        	values = values.div(numeraire);
        }
        else {
        	values = new RandomVariable(0.0,0.0);
        }

        // Apply notional exchange
        if(notionalFlow) {
            RandomVariableInterface	nationalAtPeriodEnd		= getNotional().getNotionalAtPeriodEnd(this, model);

            if(getPeriodStart() > evaluationTime) {
                RandomVariableInterface	numeraireAtPeriodStart	= model.getNumeraire(getPeriodStart());
            	values = values.subRatio(nationalAtPeriodStart, numeraireAtPeriodStart);
            }

        	if(getPeriodEnd() > evaluationTime) {
            	RandomVariableInterface	numeraireAtPeriodEnd	= model.getNumeraire(getPeriodEnd());
        		values = values.addRatio(nationalAtPeriodEnd, numeraireAtPeriodEnd);
        	}
        }
        
        if(payer) values = values.mult(-1.0);
        
        values = values.mult(numeraireAtEval);

        // Return values
		return values;	
	}

    @Override
    public RandomVariableInterface getCoupon(LIBORModelMonteCarloSimulationInterface model) throws CalculationException {
        // Calculate percentage value of coupon (not multiplied with notional, not discounted)
        RandomVariableInterface values = getIndex().getValue(getFixingDate(), model);

        // Apply daycount fraction
        double periodLength = getPeriodEnd()-getPeriodStart();
        values = values.mult(periodLength);
        
        return values;
    }
}
