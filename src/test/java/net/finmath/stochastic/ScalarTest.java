package net.finmath.stochastic;

import org.junit.Assert;
import org.junit.Test;

public class ScalarTest {

	@Test
	public void testAbs(){
		Assert.assertEquals(0.0, (new Scalar(0.0)).abs().doubleValue(), 0.0);
		Assert.assertEquals(5.6, (new Scalar(5.6)).abs().doubleValue(), 0.0);
		Assert.assertEquals(21.8, (new Scalar(-21.8)).abs().doubleValue(), 0.0);
	}

	@Test
	public void testAccrue(){
		Scalar rate = new Scalar(0);
		Assert.assertEquals(100.0, (new Scalar(100.0)).accrue(rate, 10).doubleValue(), 0.0);

		rate = new Scalar(10);
		Assert.assertEquals(5100, (new Scalar(100.0)).accrue(rate, 5).doubleValue(), 0.0);

		rate = new Scalar(-0.5);
		Assert.assertEquals(600, (new Scalar(100.0)).accrue(rate, -10).doubleValue(), 0.0);
	}

	@Test
	public void testAdd(){
		Assert.assertEquals(11.0, (new Scalar(5)).add(6).doubleValue(), 0.0);
		Assert.assertEquals(Double.NaN, (new Scalar(5)).add(Double.NaN).doubleValue(), 0.0);
		Assert.assertEquals(-3.0, (new Scalar(5)).add(-8).doubleValue(), 0.0);
	}

	@Test
	public void testAddProduct(){
		final Scalar five = new Scalar(5);
		final Scalar six = new Scalar(6);
		final Scalar seven = new Scalar(7);
		final Scalar minusFour = new Scalar(-4);

		Assert.assertEquals(41, six.addProduct(five, seven).doubleValue(), 0.0);
		Assert.assertEquals(-15, five.addProduct(five, minusFour).doubleValue(), 0.0);
		Assert.assertEquals(26, minusFour.addProduct(five, six).doubleValue(), 0.0);
	}

	@Test
	public void testAddRatio(){
		final Scalar five = new Scalar(5);
		final Scalar six = new Scalar(6);
		final Scalar seven = new Scalar(7);
		final Scalar minusFour = new Scalar(-4);

		Assert.assertEquals(6.0 + (5.0 / 7.0), six.addRatio(five, seven).doubleValue(), 1e-10);
		Assert.assertEquals(5.0 + (5.0 / -4.0), five.addRatio(five, minusFour).doubleValue(), 1e-10);
		Assert.assertEquals(-4.0 + (5.0 / 6.0), minusFour.addRatio(five, six).doubleValue(), 1e-10);
	}

	@Test
	public void testArrayOf() {
		final Scalar[] testArray = Scalar.arrayOf(new double[] {10, -5, 14.1});
		Assert.assertTrue(new Scalar(10).equals(testArray[0]));
		Assert.assertTrue(new Scalar(-5).equals(testArray[1]));
		Assert.assertTrue(new Scalar(14.1).equals(testArray[2]));
	}

	@Test
	public void testCap(){
		Assert.assertEquals(0, (new Scalar(10)).cap(new Scalar(0)).doubleValue(), 0);
		Assert.assertEquals(5, (new Scalar(10)).cap(new Scalar(5)).doubleValue(), 0);
		Assert.assertEquals(-5, (new Scalar(10)).cap(new Scalar(-5)).doubleValue(), 0);
		Assert.assertEquals(0, (new Scalar(0)).cap(new Scalar(0)).doubleValue(), 0);
		Assert.assertEquals(Double.NEGATIVE_INFINITY, (new Scalar(Double.POSITIVE_INFINITY)).cap(new Scalar(Double.NEGATIVE_INFINITY)).doubleValue(), 0);
	}

	@Test
	public void testChoose() {
		final RandomVariable five = new Scalar(5);
		final RandomVariable six = new Scalar(6);

		Assert.assertEquals(five, (new Scalar(10)).choose(five, six));
		Assert.assertEquals(six, (new Scalar(-3)).choose(five, six));
		Assert.assertEquals(five, (new Scalar(0)).choose(five, six));
	}

	@Test
	public void testDiscount(){
		final Scalar toDiscount = new Scalar(1000);

		Scalar rate = new Scalar(10);
		Assert.assertEquals(9.900990099009901, toDiscount.discount(rate, 10.0).doubleValue(), 1e-10);

		rate = new Scalar(-5);
		Assert.assertEquals(-10.1010101010101, toDiscount.discount(rate, 20.0).doubleValue(), 1e-10);
	}

	@Test
	public void testDiv() {
		Assert.assertEquals(0.0, (new Scalar(0)).div(10).doubleValue(), 0.0);
		Assert.assertEquals(2.0, (new Scalar(10)).div(5).doubleValue(), 0.0);
		Assert.assertEquals(Double.POSITIVE_INFINITY, (new Scalar(10)).div(0).doubleValue(), 0.0);
		Assert.assertEquals(Double.NEGATIVE_INFINITY, (new Scalar(-5)).div(0).doubleValue(), 0.0);
	}

	@Test
	public void testEquals() {
		Assert.assertTrue((new Scalar(10)).equals(new Scalar(10)));
		Assert.assertTrue((new Scalar(0)).equals(new Scalar(0)));
		Assert.assertFalse((new Scalar(3)).equals(new Scalar(4)));
	}

	@Test
	public void testFloor(){
		Assert.assertEquals(1.0, new Scalar(0.8).floor(1.0).doubleValue(), 0);
		Assert.assertEquals(0.0, new Scalar(-0.8).floor(0.0).doubleValue(), 0);
		Assert.assertEquals(0.0, new Scalar(0.0).floor(0.0).doubleValue(), 0);
		Assert.assertEquals(0.8, new Scalar(0.8).floor(0.0).doubleValue(), 0);
		Assert.assertEquals(Double.POSITIVE_INFINITY, new Scalar(Double.NEGATIVE_INFINITY).floor(Double.POSITIVE_INFINITY).doubleValue(), 0);
	}


	@Test
	public void testGetAverage() {
		Assert.assertEquals(0.125, (new Scalar(0.5)).getAverage(new Scalar(0.25)), 0.0);
		Assert.assertEquals(-1, (new Scalar(-0.5)).getAverage(new Scalar(2)), 0.0);
	}

	@Test
	public void testInvert() {
		Assert.assertEquals(0.2, (new Scalar(5)).invert().doubleValue(), 0.0);
		Assert.assertEquals(-5, (new Scalar(-0.2)).invert().doubleValue(), 0.0);
		Assert.assertEquals(Double.POSITIVE_INFINITY, (new Scalar(0)).invert().doubleValue(), 0.0);
	}

	@Test
	public void testIsNaN() {
		Assert.assertEquals(0.0, (new Scalar(0)).isNaN().doubleValue(), 0.0);
		Assert.assertEquals(1.0, (new Scalar(Double.NaN)).isNaN().doubleValue(), 0.0);
		Assert.assertEquals(0.0, (new Scalar(Double.POSITIVE_INFINITY)).isNaN().doubleValue(), 0.0);
		Assert.assertEquals(0.0, (new Scalar(-10)).isNaN().doubleValue(), 0.0);
	}

	@Test
	public void testMult(){
		Assert.assertEquals(4, (new Scalar(2)).mult(2).doubleValue(), 0);
		Assert.assertEquals(-4, (new Scalar(-2)).mult(2).doubleValue(), 0);
		Assert.assertEquals(0, (new Scalar(0)).mult(0).doubleValue(), 0);
	}

	@Test
	public void testSqrt(){
		Assert.assertEquals(8, (new Scalar(64)).sqrt().doubleValue(), 0);
		Assert.assertEquals(1, (new Scalar(1)).sqrt().doubleValue(), 0);
		Assert.assertEquals(0, (new Scalar(0)).sqrt().doubleValue(), 0);
		Assert.assertEquals( 1.5, (new Scalar(2.25)).sqrt().doubleValue(), 0);
	}

	@Test
	public void testSquared() {
		Assert.assertEquals(0, (new Scalar(0)).squared().doubleValue(), 0.0);
		Assert.assertEquals(64, (new Scalar(8)).squared().doubleValue(), 0.0);
		Assert.assertEquals(16, (new Scalar(-4)).squared().doubleValue(), 0.0);
	}

	@Test
	public void testSub() {
		final Scalar five = new Scalar(5);
		final Scalar six = new Scalar(6);

		Assert.assertEquals(1, six.sub(five).doubleValue(), 0);
		Assert.assertEquals(-1, five.sub(six).doubleValue(), 0);
	}

	@Test
	public void testSubRatio() {
		final Scalar three = new Scalar(3);
		final Scalar four = new Scalar(4);
		final Scalar five = new Scalar(5);

		Assert.assertEquals(4.25, five.subRatio(three, four).doubleValue(), 0);
	}
}
