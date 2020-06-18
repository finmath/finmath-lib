package net.finmath.integration;

import java.util.function.DoubleUnaryOperator;

import org.junit.Assert;
import org.junit.Test;

public class PiecewiseContantDoubleUnaryOperatorTest {

	@Test
	public void testValuation() {
		final double[] integralRightPoints = new double[] { 1, 2, 4, 8, 10 };

		final double[] values = new double[] {13, 7, 5, 21, 3, 2};

		final PiecewiseContantDoubleUnaryOperator function = new PiecewiseContantDoubleUnaryOperator(integralRightPoints, values);

		Assert.assertEquals("Valuation", 13, function.applyAsDouble(0.5), 0.0);
		Assert.assertEquals("Valuation", 13, function.applyAsDouble(1.0), 0.0);
		Assert.assertEquals("Valuation",  7, function.applyAsDouble(1.1), 0.0);
		Assert.assertEquals("Valuation",  3, function.applyAsDouble(9.0), 0.0);
		Assert.assertEquals("Valuation",  3, function.applyAsDouble(10.0), 0.0);
		Assert.assertEquals("Valuation",  2, function.applyAsDouble(11.0), 0.0);
	}

	@Test
	public void testIntegral() {
		final double[] integralRightPoints = new double[] { 1, 2, 4, 8, 10 };

		final double[] values = new double[] {13, 7, 5, 21, 3, 2};

		final PiecewiseContantDoubleUnaryOperator function = new PiecewiseContantDoubleUnaryOperator(integralRightPoints, values);

		Assert.assertEquals("Integral", 13*0.5, function.getIntegral(0, 0.5), 0.0);
		Assert.assertEquals("Integral", 7*0.5, function.getIntegral(1, 1.5), 0.0);
		Assert.assertEquals("Integral", 7*0.5+5*1.5, function.getIntegral(1.5, 3.5), 0.0);
		Assert.assertEquals("Integral", 7*0.5+5*2+21*1.0, function.getIntegral(1.5, 5), 0.0);
		Assert.assertEquals("Integral", 21*1+3*1, function.getIntegral(7, 9), 0.0);
		Assert.assertEquals("Integral", 3*1+2*1, function.getIntegral(9, 11), 0.0);
		Assert.assertEquals("Integral", 2*10, function.getIntegral(20, 30), 0.0);
	}

	@Test
	public void testIntegralOfSquares() {
		final double[] integralRightPoints = new double[] { 1, 2, 4, 8, 10 };

		final double[] values = new double[] {13, 7, 5, 21, 3, 2};

		final DoubleUnaryOperator squared = x -> x*x;
		final PiecewiseContantDoubleUnaryOperator function = new PiecewiseContantDoubleUnaryOperator(integralRightPoints, values);

		Assert.assertEquals("Integral", 13*13*0.5, function.getIntegral(0, 0.5, squared), 0.0);
		Assert.assertEquals("Integral", 7*7*0.5, function.getIntegral(1, 1.5, squared), 0.0);
		Assert.assertEquals("Integral", 7*7*0.5+5*5*1.5, function.getIntegral(1.5, 3.5, squared), 0.0);
		Assert.assertEquals("Integral", 7*7*0.5+5*5*2+21*21*1.0, function.getIntegral(1.5, 5, squared), 0.0);
		Assert.assertEquals("Integral", 21*21*1+3*3*1, function.getIntegral(7, 9, squared), 0.0);
		Assert.assertEquals("Integral", 3*3*1+2*2*1, function.getIntegral(9, 11, squared), 0.0);
		Assert.assertEquals("Integral", 2*2*10, function.getIntegral(20, 30, squared), 0.0);
	}

	@Test
	public void testIntegralErrorCorrection() {
		final double[] integralRightPoints = new double[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };

		final double[] values = new double[] { 100000, 0.01, 0.01, 0.01, 0.01, 0.01, 0.01, 0.01, 0.01, 0.01, 0.01 };

		final PiecewiseContantDoubleUnaryOperator function = new PiecewiseContantDoubleUnaryOperator(integralRightPoints, values);

		Assert.assertEquals("Integral", 100000.1, function.getIntegral(0, 11), 0.0);
	}

	@Test
	public void testExceptions() {
		final double[] integralRightPoints = new double[] { 1, 2, 4, 8, 10 };

		final double[] values = new double[] {13, 7, 5, 21, 3};

		try {
			@SuppressWarnings("unused")
			final
			PiecewiseContantDoubleUnaryOperator function = new PiecewiseContantDoubleUnaryOperator(integralRightPoints, values);
			Assert.fail("Exception not thrown.");
		}
		catch(final Exception e) {
			Assert.assertTrue(e instanceof IllegalArgumentException);
		}

		try {
			@SuppressWarnings("unused")
			final
			PiecewiseContantDoubleUnaryOperator function = new PiecewiseContantDoubleUnaryOperator(null, values);
			Assert.fail("Exception not thrown.");
		}
		catch(final Exception e) {
			Assert.assertTrue(e instanceof NullPointerException);
		}

		try {
			@SuppressWarnings("unused")
			final
			PiecewiseContantDoubleUnaryOperator function = new PiecewiseContantDoubleUnaryOperator(integralRightPoints, null);
			Assert.fail("Exception not thrown.");
		}
		catch(final Exception e) {
			Assert.assertTrue(e instanceof NullPointerException);
		}
	}
}
