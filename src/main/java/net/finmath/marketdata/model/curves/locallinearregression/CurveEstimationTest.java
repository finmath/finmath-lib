/*
 * (c) Copyright finmath.net, Germany. Contact: info@finmath.net.
 *
 * Created on 01.06.2018
 */
package net.finmath.marketdata.model.curves.locallinearregression;

//import net.finmath.marketdata.model.curves.*;
import java.time.LocalDate;

import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.jblas.DoubleMatrix;

import net.finmath.marketdata.model.curves.CurveInterface;

public class CurveEstimationTest {

	public static void main(String[] args) {
		LocalDate date=LocalDate.now();
		double[] X={0,1};
		double[] Y={1,0.8};
		CurveEstimation estimatedcurve = new CurveEstimation(date,5060,X,Y,X,0.5);
		CurveInterface regressionCurve = estimatedcurve.getRegressionCurve();
		System.out.println(regressionCurve.getValue(-0.5));
		System.out.println(regressionCurve.getValue(0));
		System.out.println(regressionCurve.getValue(0.5));
		System.out.println(regressionCurve.getValue(1));
		System.out.println(regressionCurve.getValue(1.5));


		AbstractRealDistribution kernel=new NormalDistribution();
		double K=kernel.density(0.0);
		DoubleMatrix R=new DoubleMatrix(new double[] {K*(Y[0]+Y[1]),Y[1]*(X[1]-X[0])*K} );
		DoubleMatrix M=new DoubleMatrix(new double[][] {{2*K,K*(X[1]-X[0])},{K*(X[1]-X[0]),K*(X[1]-X[0])*(X[1]-X[0])}} );
		double detM= M.get(0,0)*M.get(1, 1)-M.get(1,0)*M.get(1,0);
		DoubleMatrix MInv=new DoubleMatrix(new double[][] {{M.get(1, 1),-M.get(1,0)},{-M.get(1,0),M.get(0,0)}} );
		MInv=MInv.mul(1/detM);
		DoubleMatrix a=MInv.mmul(R);
		System.out.println(R.toString());
		System.out.println(M.toString());
		System.out.println(a.toString());

	}

}
