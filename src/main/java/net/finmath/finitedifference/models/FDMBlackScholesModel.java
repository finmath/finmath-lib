package net.finmath.finitedifference.models;

import net.finmath.finitedifference.products.FDMEuropeanCallOption;
import net.finmath.finitedifference.solvers.FDMThetaMethod;

/**
 * Black Scholes model using finite difference method.
 * 
 * @author Ralph Rudd
 * @author Christian Fries
 * @author Jörg Kienitz
 */
public class FDMBlackScholesModel implements FiniteDifference1DModel {
    public int numTimesteps;
    public int numSpacesteps;
    public int numStandardDeviations;
    public double initialValue;
    public double riskFreeRate;
    public double volatility;

    public FDMBlackScholesModel(
            int numTimesteps,
            int numSpacesteps,
            int numStandardDeviations,
            double initialValue,
            double riskFreeRate,
            double volatility) {
        this.numTimesteps = numTimesteps;
        this.numSpacesteps = numSpacesteps;
        this.numStandardDeviations = numStandardDeviations;
        this.initialValue = initialValue;
        this.riskFreeRate = riskFreeRate;
        this.volatility = volatility;
    }

    /* (non-Javadoc)
	 * @see net.finmath.finitedifference.models.FiniteDifference1DModel#varianceOfStockPrice(double)
	 */
    @Override
	public double varianceOfStockPrice(double time) {
        return Math.pow(initialValue, 2) * Math.exp(2 * riskFreeRate * time)
                * (Math.exp(Math.pow(volatility, 2) * time) - 1);
    }

    /* (non-Javadoc)
	 * @see net.finmath.finitedifference.models.FiniteDifference1DModel#getForwardValue(double)
	 */
    @Override
	public double getForwardValue(double time) {
        return initialValue * Math.exp(riskFreeRate * time);
    }

	/* (non-Javadoc)
	 * @see net.finmath.finitedifference.models.FiniteDifference1DModel#getRiskFreeRate()
	 */
	@Override
	public double getRiskFreeRate() {
		return riskFreeRate;
	}

	/* (non-Javadoc)
	 * @see net.finmath.finitedifference.models.FiniteDifference1DModel#valueOptionWithThetaMethod(net.finmath.finitedifference.products.FDMEuropeanCallOption, double)
	 */
	@Override
	public double[][] valueOptionWithThetaMethod(FDMEuropeanCallOption option, double theta) {
        FDMThetaMethod solver = new FDMThetaMethod(this, option, option.maturity, option.strike, theta);
        return solver.getValue(stockPrice -> option.valueAtMaturity(stockPrice));
    }

}
