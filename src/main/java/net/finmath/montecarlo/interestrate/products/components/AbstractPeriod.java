package net.finmath.montecarlo.interestrate.products.components;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * Base class for a period. A period has references to the index (coupon) and the notional.
 * It provides the fixing date for the index, the period length, and the payment date.
 * 
 * @author Christian Fries
 * @version 1.1
 */
public abstract class AbstractPeriod extends AbstractProductComponent {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8035860121112226049L;
	private double periodStart;
	private double periodEnd;
	private double fixingDate;
	private double paymentDate;

	private AbstractNotional				notional;
	private AbstractProductComponent		index;

    public abstract RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException;
    
    public abstract RandomVariableInterface getCoupon(LIBORModelMonteCarloSimulationInterface model) throws CalculationException;

    
    /**
     * @param periodStart
     * @param periodEnd
     * @param fixingDate
     * @param paymentDate
     * @param notional
     * @param index
     */
    public AbstractPeriod(double periodStart, double periodEnd,
            double fixingDate, double paymentDate, AbstractNotional notional,
            AbstractProductComponent index) {
	    super();
	    this.periodStart = periodStart;
	    this.periodEnd = periodEnd;
	    this.fixingDate = fixingDate;
	    this.paymentDate = paymentDate;
	    this.notional = notional;
	    this.index = index;
    }

	/**
     * @return the periodStart
     */
    public double getPeriodStart() {
    	return periodStart;
    }

    /**
     * @return the periodEnd
     */
    public double getPeriodEnd() {
    	return periodEnd;
    }

    /**
     * @return the fixingDate
     */
    public double getFixingDate() {
    	return fixingDate;
    }

    /**
     * @return the paymentDate
     */
    public double getPaymentDate() {
    	return paymentDate;
    }

	/**
     * @return the notional
     */
    public AbstractNotional getNotional() {
    	return notional;
    }

	/**
     * @return the index
     */
    public AbstractProductComponent getIndex() {
    	return index;
    }
}
