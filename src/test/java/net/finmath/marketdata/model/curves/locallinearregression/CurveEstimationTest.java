/*
 * (c) Copyright finmath.net, Germany. Contact: info@finmath.net.
 *
 * Created on 01.06.2018
 */
package net.finmath.marketdata.model.curves.locallinearregression;

import java.time.LocalDate;

import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.jblas.DoubleMatrix;
import org.junit.Assert;
import org.junit.Test;

import net.finmath.marketdata.model.curves.Curve;

public class CurveEstimationTest {

	private static boolean isJBLASPresent;
	static {
		boolean isJBLASPresent = false;
		/*
		 * Check if jblas is available
		 */
		try {
			final double[] x = org.jblas.Solve.solve(new org.jblas.DoubleMatrix(2, 2, 1.0, 1.0, 0.0, 1.0), new org.jblas.DoubleMatrix(2, 1, 1.0, 1.0)).data;
			if(x[0] != 1.0 || x[1] != 0.0) {
				isJBLASPresent = true;
			}
		}
		catch(final java.lang.UnsatisfiedLinkError e) {
			isJBLASPresent = false;
		}
		CurveEstimationTest.isJBLASPresent = isJBLASPresent;
	}

	/**
	 * Simple test of curve consisting of two points..
	 */
	@Test
	public void testLinearInterpolation() {
		final LocalDate date=LocalDate.now();
		final double[] X = { 0.0 , 1.0 };
		final double[] Y = { 1.0 , 0.8 };
		final double bandwidth = 1500;

		if(isJBLASPresent) {
			// The following code only works if JBlas is present
			final CurveEstimation estimatedcurve = new CurveEstimation(date, bandwidth, X, Y, X, 0.5);
			final Curve regressionCurve = estimatedcurve.getRegressionCurve();

			Assert.assertEquals("left extrapolatoin", 1.0, regressionCurve.getValue(-0.5), 1E-12);
			Assert.assertEquals("left interpolation", 0.95, regressionCurve.getValue(0.25), 1E-12);
			Assert.assertEquals("center interpolation", 0.9, regressionCurve.getValue(0.50), 1E-12);
			Assert.assertEquals("right interpolation", 0.85, regressionCurve.getValue(0.75), 1E-12);
			Assert.assertEquals("right extrapolatoin", 0.8, regressionCurve.getValue(1.5), 1E-12);
		}
	}


	/**
	 * Regression matrix (currently no test, just for inspection)
	 */
	@Test
	public void testRegressionMatrix() {
		final double[] X = { 0.0 , 1.0 };
		final double[] Y = { 1.0 , 0.8 };

		if(isJBLASPresent) {
			// The following code only works if JBlas is present

			final AbstractRealDistribution kernel=new NormalDistribution();
			final double K=kernel.density(0.0);
			final DoubleMatrix R=new DoubleMatrix(new double[] {K*(Y[0]+Y[1]),Y[1]*(X[1]-X[0])*K} );
			final DoubleMatrix M=new DoubleMatrix(new double[][] {{2*K,K*(X[1]-X[0])},{K*(X[1]-X[0]),K*(X[1]-X[0])*(X[1]-X[0])}} );
			final double detM= M.get(0,0)*M.get(1, 1)-M.get(1,0)*M.get(1,0);
			DoubleMatrix MInv=new DoubleMatrix(new double[][] {{M.get(1, 1),-M.get(1,0)},{-M.get(1,0),M.get(0,0)}} );
			MInv=MInv.mul(1/detM);
			final DoubleMatrix a=MInv.mmul(R);
			System.out.println(R.toString());
			System.out.println(M.toString());
			System.out.println(a.toString());
		}
	}
}
