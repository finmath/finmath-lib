package net.finmath.finitedifference.products;

import net.finmath.finitedifference.models.FDMBlackScholesModel;

public class FDMEuropeanCallOption {
    private double optionMaturity;
    private double optionStrike;
    private double riskFreeRate;

    public FDMEuropeanCallOption(double optionMaturity, double optionStrike, double riskFreeRate) {
        this.optionMaturity = optionMaturity;
        this.optionStrike = optionStrike;
        this.riskFreeRate = riskFreeRate;
    }

    public double valueAtMaturity(double stockPrice) {
        return Math.max(stockPrice - optionStrike, 0);
    }

    public double valueAtLowerStockPriceBoundary(double stockPrice, double currentTime) {
        return 0;
    }

    public double valueAtUpperStockPriceBoundary(double stockPrice, double currentTime) {
        return stockPrice - optionStrike * Math.exp(-riskFreeRate*(optionMaturity - currentTime));
    }

    public double getValue(FDMBlackScholesModel model){
        // The FDM algorithm requires the boundary conditions of the product
        return model.valueOptionWithThetaMethod(this);
    }
}
