/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 26.01.2018
 */
package net.finmath.interpolation;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Christian Fries
 */
public class BiLinearInterpolationTest {

	@Test
	public void test() {

		final double[] xArray = { 5.0, 7.0, 8.0 };
		final double[] yArray = { 2.5, 5.0, 7.5 };
		final double[][] zArray = { { 3.0, 4.0, 2.0 }, { 2.0, 1.0, 0.0 }, { 1.0, 5.0, 1.0 } };

		final BiLinearInterpolation interpolation = new BiLinearInterpolation(xArray, yArray, zArray);

		for(int i=0; i<xArray.length; i++) {
			for(int j=0; j<yArray.length; j++ ) {
				final double x = xArray[i];
				final double y = yArray[j];
				final double z = zArray[i][j];
				Assert.assertEquals("Interpolation points", z, interpolation.getValue(x, y), 1E-15);
			}
		}

		/*
		 * Print interpolates surface (check in Excel for example)
		 */
		for(double x = 4.0; x < 9.0; x += 0.1) {
			for(double y = 2.0; y < 10.0; y += 0.1) {
				final double z = interpolation.getValue(x, y);
				System.out.println(x + "\t" + y + "\t" +z);
			}
		}
	}

}
