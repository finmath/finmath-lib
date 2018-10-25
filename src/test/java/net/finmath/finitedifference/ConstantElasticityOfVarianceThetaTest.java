package net.finmath.finitedifference;

import net.finmath.finitedifference.models.FDMConstantElasticityOfVarianceModel;
import net.finmath.finitedifference.models.FiniteDifference1DModel;
import net.finmath.finitedifference.products.FDMEuropeanCallOption;
import net.finmath.finitedifference.products.FiniteDifference1DProduct;
import net.finmath.functions.AnalyticFormulas;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class ConstantElasticityOfVarianceThetaTest {

    @Test
    public void test() throws AssertionError {
        double riskFreeRate = 0.06;
        double volatility = 0.4;
        double exponent = 0.9;
        double optionMaturity = 1;
        double optionStrike = 100;

        int numTimesteps = 80;
        int numSpacesteps = 160;
        int numStandardDeviations = 5;
        double initialValue = 100;
        double theta = 0.5;

        FiniteDifference1DModel model = new FDMConstantElasticityOfVarianceModel(
                numTimesteps,
                numSpacesteps,
                numStandardDeviations,
                optionStrike, // center of the grid.
                theta,
                initialValue,
                riskFreeRate,
                volatility,
                exponent);

        FiniteDifference1DProduct callOption = new FDMEuropeanCallOption(optionMaturity, optionStrike);
        double[][] valueFDM = callOption.getValue(0.0, model);
        double[] initialStockPrice = valueFDM[0];
        double[] optionValue = valueFDM[1];
        double[] analyticalOptionValue = new double[optionValue.length];
        for (int i =0; i < analyticalOptionValue.length; i++) {
            analyticalOptionValue[i] = AnalyticFormulas.constantElasticityOfVarianceOptionValue(initialStockPrice[i], riskFreeRate,
                    volatility, exponent, optionMaturity, optionStrike, true);
        }
        System.out.println(Arrays.toString(optionValue));
        Assert.assertArrayEquals(analyticalOptionValue, optionValue, 5e-3);
    }
}
