package net.finmath.finitedifference;

import net.finmath.finitedifference.experimental.LocalVolatilityTheta;
import net.finmath.functions.NonCentralChiSquaredDistribution;
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

    @Test
    public void testNonCentralChiSquaredDistribution() throws AssertionError{
        double degreesOfFreedom = 0.01;
        double nonCentrality = 5;
        NonCentralChiSquaredDistribution ncx2k = new NonCentralChiSquaredDistribution(degreesOfFreedom, nonCentrality);
        Assert.assertEquals(.590869982, ncx2k.getCDF(5.), 1e-6);

    }
}
