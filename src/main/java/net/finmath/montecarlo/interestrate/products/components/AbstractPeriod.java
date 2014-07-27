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

	private static final long serialVersionUID = 8035860121112226049L;
	private final double periodStart;
	private final double periodEnd;
	private final double fixingDate;
	private final double paymentDate;

	private final AbstractNotional				notional;
	private final AbstractProductComponent		index;

    public abstract RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException;
    
    public abstract RandomVariableInterface getCoupon(LIBORModelMonteCarloSimulationInterface model) throws CalculationException;

    
    /**
     * Initialize basic properties of the period.
     * 
     * @param periodStart The period start.
     * @param periodEnd The period end.
     * @param fixingDate The fixing date (as double).
     * @param paymentDate The payment date (as double).
     * @param notional The notional object relevant for this period.
     * @param index The index (coupon) associated with this period.
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
     * @return the period start
     */
    public double getPeriodStart() {
    	return periodStart;
    }

    /**
     * @return the period end
     */
    public double getPeriodEnd() {
    	return periodEnd;
    }

    /**
     * @return the fixing date
     */
    public double getFixingDate() {
    	return fixingDate;
    }

    /**
     * @return the payment date
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
