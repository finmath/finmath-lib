package net.finmath.finitedifference.products;

import net.finmath.finitedifference.models.FDMBlackScholesModel;

public class FDMEuropeanCallOption implements FiniteDifference1DBoundary {
    public double maturity;
    public double strike;
    private double riskFreeRate;

    public FDMEuropeanCallOption(double optionMaturity, double optionStrike, double riskFreeRate) {
        this.maturity = optionMaturity;
        this.strike = optionStrike;
        this.riskFreeRate = riskFreeRate;
    }

    public double valueAtMaturity(double stockPrice) {
        return Math.max(stockPrice - strike, 0);
    }

    @Override
	public double getValueAtLowerBoundary(double currentTime, double stockPrice) {
        return 0;
    }

    @Override
	public double getValueAtUpperBoundary(double currentTime, double stockPrice) {
        return stockPrice - strike * Math.exp(-riskFreeRate*(maturity - currentTime));
    }

    public double[][] getValue(FDMBlackScholesModel model, double theta){
        // The FDM algorithm requires the boundary conditions of the product
        return model.valueOptionWithThetaMethod(this, theta);
    }
}
