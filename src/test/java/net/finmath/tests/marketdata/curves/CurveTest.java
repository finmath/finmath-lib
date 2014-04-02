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
		
		/*
		 * Build a curve (initial guess for our fitting problem, defines the times).
		 */
		Curve.CurveBuilder curveBuilder = new Curve.CurveBuilder();
		
		curveBuilder.setInterpolationMethod(Curve.InterpolationMethod.CUBIC_SPLINE);
		curveBuilder.setExtrapolationMethod(Curve.ExtrapolationMethod.LINEAR);
		curveBuilder.setInterpolationEntity(Curve.InterpolationEntity.VALUE);
		
		// Add some points - which will not be fitted
		curveBuilder.addPoint(-1.0 /* time */, 1.0 /* value */, false /* is Parameter */);
		curveBuilder.addPoint( 0.0 /* time */, 1.0 /* value */, false /* is Parameter */);

		// Add some points - which will be fitted
		curveBuilder.addPoint( 1.0 /* time */, 2.0 /* value */, true /* is Parameter */);
		curveBuilder.addPoint( 2.0 /* time */, 2.0 /* value */, true /* is Parameter */);
		curveBuilder.addPoint( 3.0 /* time */, 2.0 /* value */, true /* is Parameter */);

		final Curve curve = curveBuilder.build();
		
		/*
		 * Create data to which the curve should be fitted
		 */
		final double[] times		= { 0.0,  0.5, 0.75, 1.0, 1.5, 1.75, 2.3 };
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
			
			@Override
			public void setValues(double[] parameters, double[] values) throws SolverException {
				
				CurveInterface cuveGuess = null;
				try {
					cuveGuess = curve.getCloneForParameter(parameters);
				} catch (CloneNotSupportedException e) {
					throw new SolverException(e);
				}

				for(int valueIndex=0; valueIndex<values.length; valueIndex++) {
					values[valueIndex] = cuveGuess.getValue(times[valueIndex]) - givenValues[valueIndex];
				}
			}
		};

		// Fit the curve (find best parameters)
		optimizer.run();
		
		CurveInterface fittedCurve = curve.getCloneForParameter(optimizer.getBestFitParameters());
		
		// Print out fitted curve
		for(double time = -2.0; time < 5.0; time += 0.1) {
			System.out.println(numberFormat.format(time) + "\t" + numberFormat.format(fittedCurve.getValue(time)));
		}
	}
}
