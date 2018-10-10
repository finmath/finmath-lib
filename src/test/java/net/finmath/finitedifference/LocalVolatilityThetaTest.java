package net.finmath.finitedifference;

import net.finmath.finitedifference.experimental.LocalVolatilityTheta;
import net.finmath.functions.AnalyticFormulas;
import net.finmath.functions.NonCentralChiSquaredDistribution;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class LocalVolatilityThetaTest {

    @Test
    public void test() throws AssertionError {
        double riskFreeRate = 0.06;
        double exponent = 0.9;
        double volatility = 0.4;
        double optionMaturity = 1;
        double optionStrike = 40;
        boolean isCall = false;

        LocalVolatilityTheta localVolatilityFD = new LocalVolatilityTheta();
        double[][] stockAndOptionPrice = localVolatilityFD.solve();
        double[] initialStockPrice = stockAndOptionPrice[0];
        double[] optionValue = stockAndOptionPrice[1];
        double[] analyticalOptionValue = new double[initialStockPrice.length];

        for (int i = 0; i < initialStockPrice.length; i++) {
            analyticalOptionValue[i] = AnalyticFormulas.constantElasticityOfVarianceOptionValue(
                    initialStockPrice[i], riskFreeRate, exponent, volatility, optionMaturity, optionStrike, isCall);
        }
        Assert.assertArrayEquals(analyticalOptionValue, optionValue, 5e-3);
    }

}
