/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 31.03.2014
 */

package net.finmath.tests.marketdata.curves;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import net.finmath.marketdata.model.curves.Curve;
import net.finmath.marketdata.model.curves.CurveInterface;
import net.finmath.optimizer.LevenbergMarquardt;
import net.finmath.optimizer.SolverException;

import org.junit.Assert;
import org.junit.Test;

/**
 * A short demo on how to use {@link net.finmath.marketdata.model.curves.Curve}.
 * 
 * @author Christian Fries
 */
public class CurveTest {

	private static NumberFormat numberFormat = new DecimalFormat("0.0000");

	/**
	 * Run a short demo on how to use {@link net.finmath.marketdata.model.curves.Curve}.
	 * 
	 * @param args Not used.
	 * @throws SolverException Thrown if optimizer fails.
	 * @throws CloneNotSupportedException Thrown if curve cannot be cloned for optimization.
	 */
	public static void main(String[] args) throws SolverException, CloneNotSupportedException {
		(new CurveTest()).testCurveFitting();
	}
	
	/**
	 * Tests fitting of curve to given data.
	 * 
	 * @throws SolverException Thrown if optimizer fails.
	 * @throws CloneNotSupportedException Thrown if curve cannot be cloned for optimization.
	 */
	@Test
	public void testCurveFitting() throws SolverException, CloneNotSupportedException {
		
		/*
		 * Build a curve (initial guess for our fitting problem, defines the times).
		 */
		Curve.CurveBuilder curveBuilder = new Curve.CurveBuilder();
		
		curveBuilder.setInterpolationMethod(Curve.InterpolationMethod.LINEAR);
		curveBuilder.setExtrapolationMethod(Curve.ExtrapolationMethod.LINEAR);
		curveBuilder.setInterpolationEntity(Curve.InterpolationEntity.VALUE);
		
		// Add some points - which will not be fitted
		curveBuilder.addPoint(-1.0 /* time */, 1.0 /* value */, false /* isParameter */);
		curveBuilder.addPoint( 0.0 /* time */, 1.0 /* value */, false /* isParameter */);

		// Add some points - which will be fitted
		curveBuilder.addPoint( 0.5  /* time */, 2.0 /* value */, true /* isParameter */);
		curveBuilder.addPoint( 0.75 /* time */, 2.0 /* value */, true /* isParameter */);
		curveBuilder.addPoint( 1.0 /* time */, 2.0 /* value */, true /* isParameter */);
		curveBuilder.addPoint( 2.2 /* time */, 2.0 /* value */, true /* isParameter */);
		curveBuilder.addPoint( 3.0 /* time */, 2.0 /* value */, true /* isParameter */);

		final CurveInterface curve = curveBuilder.build();
		
		/*
		 * Create data to which the curve should be fitted to
		 */
		final double[] givenTimes	= { 0.0,  0.5, 0.75, 1.0, 1.5, 1.75, 2.5 };
		final double[] givenValues	= { 3.5, 12.3, 13.2, 7.5, 5.5, 2.9,  4.4 };

		/*
		 * Find a best fitting curve.
		 */

		// Define the objective function
		LevenbergMarquardt optimizer = new LevenbergMarquardt(
				curve.getParameter()	/* initial parameters */,
				givenValues				/* target values */,
				100,					/* max iterations */
				Runtime.getRuntime().availableProcessors() /* max number of threads */	
				) {
			
			private static final long serialVersionUID = -5128114286941153154L;

			@Override
			public void setValues(double[] parameters, double[] values) throws SolverException {
				
				CurveInterface curveGuess = null;
				try {
					curveGuess = curve.getCloneForParameter(parameters);
				} catch (CloneNotSupportedException e) {
					throw new SolverException(e);
				}

				for(int valueIndex=0; valueIndex<values.length; valueIndex++) {
					values[valueIndex] = curveGuess.getValue(givenTimes[valueIndex]);
				}
			}
		};

		// Fit the curve (find best parameters)
		optimizer.run();
		
		final CurveInterface fittedCurve = curve.getCloneForParameter(optimizer.getBestFitParameters());
		
		// Print out fitted curve
		for(double time = -2.0; time < 5.0; time += 0.1) {
			System.out.println(numberFormat.format(time) + "\t" + numberFormat.format(fittedCurve.getValue(time)));
		}

		// Check fitted curve
		double errorSum = 0.0;
		for(int pointIndex = 0; pointIndex<givenTimes.length; pointIndex++) {
			errorSum += fittedCurve.getValue(givenTimes[pointIndex]) - givenValues[pointIndex];
		}
		System.out.println("Mean deviation: " + errorSum);
		
		/*
		 * jUnit assertion: condition under which we consider this test successful.
		 * With the given data, the fit cannot over come that at 0.0 we have an error of -2.5.
		 * Hence we test if the mean deviation is -2.5 (the optimizer reduces the variance)
		 */
		Assert.assertEquals("Deviation", errorSum, -2.5, 1E-5);
	}
}
