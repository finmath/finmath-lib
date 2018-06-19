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
    public double initialValue;
    public double riskFreeRate;
    public double volatility;

    /*
     * Solver properties - will be moved to solver.
     */
    public int numTimesteps;
    public int numSpacesteps;
    public int numStandardDeviations;
    private double theta;

    public FDMBlackScholesModel(
            int numTimesteps,
            int numSpacesteps,
            int numStandardDeviations,
            double theta,
            double initialValue,
            double riskFreeRate,
            double volatility) {
        this.initialValue = initialValue;
        this.riskFreeRate = riskFreeRate;
        this.volatility = volatility;
        
        this.numTimesteps = numTimesteps;
        this.numSpacesteps = numSpacesteps;
        this.numStandardDeviations = numStandardDeviations;
        this.theta = theta;
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

	
	public double getInitialValue() {
		return initialValue;
	}

	public double getVolatility() {
		return volatility;
	}

	public int getNumTimesteps() {
		return numTimesteps;
	}

	public int getNumSpacesteps() {
		return numSpacesteps;
	}

	public int getNumStandardDeviations() {
		return numStandardDeviations;
	}

	/* (non-Javadoc)
	 * @see net.finmath.finitedifference.models.FiniteDifference1DModel#valueOptionWithThetaMethod(net.finmath.finitedifference.products.FDMEuropeanCallOption, double)
	 */
	@Override
	public double[][] valueOptionWithThetaMethod(FDMEuropeanCallOption option) {
        FDMThetaMethod solver = new FDMThetaMethod(this, option, option.maturity, option.strike, theta);
        return solver.getValue(stockPrice -> option.valueAtMaturity(stockPrice));
    }

}
