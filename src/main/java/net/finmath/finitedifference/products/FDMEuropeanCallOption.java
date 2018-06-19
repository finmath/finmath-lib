package net.finmath.finitedifference.products;

import net.finmath.finitedifference.models.FiniteDifference1DBoundary;
import net.finmath.finitedifference.models.FiniteDifference1DModel;

public class FDMEuropeanCallOption implements FiniteDifference1DBoundary {
    public double maturity;
    public double strike;

    public FDMEuropeanCallOption(double optionMaturity, double optionStrike) {
        this.maturity = optionMaturity;
        this.strike = optionStrike;
    }

    public double valueAtMaturity(double stockPrice) {
        return Math.max(stockPrice - strike, 0);
    }

    public double[][] getValue(FiniteDifference1DModel model, double theta){
        // The FDM algorithm requires the boundary conditions of the product
        return model.valueOptionWithThetaMethod(this, theta);
    }

    /*
     * Implementation of the interface:
     * @see net.finmath.finitedifference.products.FiniteDifference1DBoundary#getValueAtLowerBoundary(net.finmath.finitedifference.models.FDMBlackScholesModel, double, double)
     */
    
    @Override
	public double getValueAtLowerBoundary(FiniteDifference1DModel model, double currentTime, double stockPrice) {
        return 0;
    }

    @Override
	public double getValueAtUpperBoundary(FiniteDifference1DModel model, double currentTime, double stockPrice) {
        return stockPrice - strike * Math.exp(-model.getRiskFreeRate()*(maturity - currentTime));
    }

}
