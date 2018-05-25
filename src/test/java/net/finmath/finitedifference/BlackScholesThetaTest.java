package net.finmath.finitedifference;

import net.finmath.finitedifference.experimental.BlackScholesTheta;
import net.finmath.functions.AnalyticFormulas;
import org.junit.Assert;
import org.junit.Test;

public class BlackScholesThetaTest {

    @Test
    public void test() throws AssertionError {
        BlackScholesTheta blackScholesFD = new BlackScholesTheta();
        double[][] stockAndOptionPrice = blackScholesFD.solve();
        double[] initialStockPrice = stockAndOptionPrice[0];
        double[] optionValue = stockAndOptionPrice[1];

        double riskFreeRate = 0.06;
        double volatility = 0.4;
        double optionMaturity = 1;
        double optionStrike = 50;
        double[] analyticalOptionValue = new double[stockAndOptionPrice[0].length];
        for (int i =0; i < analyticalOptionValue.length; i++) {
            analyticalOptionValue[i] = AnalyticFormulas.blackScholesOptionValue(initialStockPrice[i], riskFreeRate,
                    volatility, optionMaturity, optionStrike, true);
        }

        Assert.assertArrayEquals(optionValue, analyticalOptionValue, 2e-2);

    }
}
