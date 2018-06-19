package net.finmath.finitedifference;

import net.finmath.finitedifference.experimental.BlackScholesTheta;
import net.finmath.finitedifference.models.FDMBlackScholesModel;
import net.finmath.finitedifference.models.FiniteDifference1DModel;
import net.finmath.finitedifference.products.FDMEuropeanCallOption;
import net.finmath.functions.AnalyticFormulas;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class BlackScholesThetaTest {

    @Test
    public void test() throws AssertionError {
        double riskFreeRate = 0.06;
        double volatility = 0.4;
        double optionMaturity = 1;
        double optionStrike = 50;

        /*BlackScholesTheta blackScholesFD = new BlackScholesTheta();
        double[][] stockAndOptionPrice = blackScholesFD.solve();
        double[] initialStockPrice = stockAndOptionPrice[0];
        double[] optionValue = stockAndOptionPrice[1];

        double[] analyticalOptionValue = new double[stockAndOptionPrice[0].length];
        for (int i =0; i < analyticalOptionValue.length; i++) {
            analyticalOptionValue[i] = AnalyticFormulas.blackScholesOptionValue(initialStockPrice[i], riskFreeRate,
                    volatility, optionMaturity, optionStrike, true);
        }

        Assert.assertArrayEquals(optionValue, analyticalOptionValue, 2e-2);*/

        // First refactoring attempt
        int numTimesteps = 35;
        int numSpacesteps = 120;
        int numStandardDeviations = 5;
        double initialValue = 50;
        double theta = 0.5;

        FiniteDifference1DModel model = new FDMBlackScholesModel(
                numTimesteps,
                numSpacesteps,
                numStandardDeviations,
                theta,
                initialValue,
                riskFreeRate,
                volatility);

        FDMEuropeanCallOption callOption = new FDMEuropeanCallOption(optionMaturity, optionStrike);
        double[][] valueFDM = callOption.getValue(model);
        double[] initialStockPrice = valueFDM[0];
        double[] optionValue = valueFDM[1];
        double[] analyticalOptionValue = new double[optionValue.length];
        for (int i =0; i < analyticalOptionValue.length; i++) {
            analyticalOptionValue[i] = AnalyticFormulas.blackScholesOptionValue(initialStockPrice[i], riskFreeRate,
                    volatility, optionMaturity, optionStrike, true);
        }

        Assert.assertArrayEquals(optionValue, analyticalOptionValue, 1.2e-2);
        System.out.println(Arrays.toString(optionValue));
    }
}
