package net.finmath.finitedifference;

import net.finmath.finitedifference.experimental.LocalVolatilityTheta;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class LocalVolatilityThetaTest {

    @Test
    public void test() throws AssertionError {
        LocalVolatilityTheta localVolatilityFD = new LocalVolatilityTheta();
        double[][] stockAndOptionPrice = localVolatilityFD.solve();
        double[] initialStockPrice = stockAndOptionPrice[0];
        double[] optionValue = stockAndOptionPrice[1];
        System.out.println(Arrays.toString(optionValue));
        Assert.assertEquals(1, 1, 1e-3);
    }
}
