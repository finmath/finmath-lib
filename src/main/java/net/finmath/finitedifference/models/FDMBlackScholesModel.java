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
    private double initialValue;
    private double riskFreeRate;
    private double volatility;

    /*
     * Solver properties - will be moved to solver.
     */
    private int numTimesteps;
    private int numSpacesteps;
    private int numStandardDeviations;
    private double center;
    private double theta;

    public FDMBlackScholesModel(
            int numTimesteps,
            int numSpacesteps,
            int numStandardDeviations,
            double center,
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
        this.center = center;
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

	public double getNumStandardDeviations() {
		return numStandardDeviations;
	}

	/* (non-Javadoc)
	 * @see net.finmath.finitedifference.models.FiniteDifference1DModel#valueOptionWithThetaMethod(net.finmath.finitedifference.products.FDMEuropeanCallOption, double)
	 */
	@Override
	public double[][] valueOptionWithThetaMethod(FDMEuropeanCallOption option) {
        FDMThetaMethod solver = new FDMThetaMethod(this, option, option.getMaturity(), center, theta);
        return solver.getValue(stockPrice -> option.valueAtMaturity(stockPrice));
    }

}
