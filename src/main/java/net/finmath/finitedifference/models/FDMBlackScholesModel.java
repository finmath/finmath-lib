package net.finmath.finitedifference.models;

import net.finmath.finitedifference.products.FDMEuropeanCallOption;

public class FDMBlackScholesModel {
    int numTimesteps;
    int numSpacesteps;
    int numStandardDeviations;
    double initialValue;
    double riskFreeRate;
    double volatility;

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


    public double valueOptionWithThetaMethod(FDMEuropeanCallOption option) {
        return 0.0;
    }
}
