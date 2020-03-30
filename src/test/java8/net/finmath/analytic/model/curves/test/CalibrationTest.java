/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 28.11.2012
 */
package net.finmath.analytic.model.curves.test;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.finmath.marketdata2.calibration.ParameterObject;
import net.finmath.marketdata2.calibration.Solver;
import net.finmath.marketdata2.model.AnalyticModel;
import net.finmath.marketdata2.model.AnalyticModelFromCurvesAndVols;
import net.finmath.marketdata2.model.curves.Curve;
import net.finmath.marketdata2.model.curves.CurveInterpolation.ExtrapolationMethod;
import net.finmath.marketdata2.model.curves.CurveInterpolation.InterpolationEntity;
import net.finmath.marketdata2.model.curves.CurveInterpolation.InterpolationMethod;
import net.finmath.marketdata2.model.curves.DiscountCurveInterpolation;
import net.finmath.marketdata2.model.curves.ForwardCurveFromDiscountCurve;
import net.finmath.marketdata2.model.curves.ForwardCurveInterface;
import net.finmath.marketdata2.model.curves.ForwardCurveInterpolation;
import net.finmath.marketdata2.products.AnalyticProduct;
import net.finmath.marketdata2.products.Swap;
import net.finmath.optimizer.SolverException;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.RegularSchedule;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * This class makes some basic tests related to the setup, use and calibration of discount curves and forward curve.
 *
 * @author Christian Fries
 */
@RunWith(Parameterized.class)
public class CalibrationTest {

	static final double errorTolerance = 1E-14;

	final InterpolationMethod interpolationMethod;

	public CalibrationTest(final InterpolationMethod interpolationMethod)
	{
		this.interpolationMethod = interpolationMethod;
	}

	/**
	 * The parameters for this test, that is an error consisting of
	 * { numberOfPaths, setup }.
	 *
	 * @return Array of parameters.
	 */
	@Parameters(name="{0}")
	public static Collection<Object[]> generateData()
	{
		return Arrays.asList(new Object[][] {
			{ InterpolationMethod.LINEAR },
			//				{ InterpolationMethod.CUBIC_SPLINE },
			//				{ InterpolationMethod.AKIMA },
			//				{ InterpolationMethod.AKIMA_CONTINUOUS },
			{ InterpolationMethod.HARMONIC_SPLINE },
			{ InterpolationMethod.HARMONIC_SPLINE_WITH_MONOTONIC_FILTERING },
		});
	}

	/**
	 * Run some test using discount curves and forward curves and the solver to create a calibrated model.
	 *
	 * @param args Arguments - not used.
	 * @throws SolverException Thrown if the solver cannot find a solution to the calibration problem.
	 */
	public static void main(final String[] args) throws SolverException {

		final CalibrationTest calibrationTest = new CalibrationTest(InterpolationMethod.LINEAR);

		calibrationTest.testForwardCurveFromDiscountCurve();
		calibrationTest.testCurvesAndCalibration();
	}

	@Test
	public void testForwardCurveFromDiscountCurve() {
		/*
		 * CREATING AND USING A DISCOUNT CURVE
		 */

		// Create a discount curve
		final DiscountCurveInterpolation			discountCurve					= DiscountCurveInterpolation.createDiscountCurveFromDiscountFactors(
				"discountCurve"								/* name */,
				new double[] {0.0,  1.0,  2.0,  4.0,  5.0}	/* maturities */,
				new double[] {1.0, 0.95, 0.90, 0.85, 0.80}	/* discount factors */
				);

		// Create a forward curve from that discount curve for semi-annual forward rates
		final ForwardCurveInterface	forwardCurveFromDiscountCurve	= new ForwardCurveFromDiscountCurve(
				discountCurve.getName()						/* name of the discount curve to use */,
				null										/* reference date: not specified since single curve setup */,
				null										/* period length: not specified since single curve setup */
				);

		// A model is a collection of curves (curves and products find other curves by looking up their name in the model)
		final AnalyticModelFromCurvesAndVols model1 = new AnalyticModelFromCurvesAndVols(new Curve[] { discountCurve , forwardCurveFromDiscountCurve });

		System.out.println("Given a discount curve:");
		System.out.println(discountCurve.toString());

		// We may ask the forward curve for a forward.
		final double fixingTime	= 1.0;
		final double periodLength = 0.5;
		final RandomVariable forwardRate	= forwardCurveFromDiscountCurve.getForward(model1, fixingTime, periodLength);
		System.out.println("Semi-annual forward with fixing in " + fixingTime + " calculated from that discount curve is " + forwardRate);

		// Check if we have the right value
		final RandomVariable forwardRateFromDiscountFactor = discountCurve.getDiscountFactor(model1, fixingTime).div(discountCurve.getDiscountFactor(model1, fixingTime + periodLength)).sub(1.0).div(periodLength);
		Assert.assertTrue(Math.abs(forwardRate.sub(forwardRateFromDiscountFactor).getAverage()) < errorTolerance);

		System.out.println("__________________________________________________________________________________________\n");
	}

	@Test
	public void testCurvesAndCalibration() throws SolverException {

		/*
		 * CALIBRATING A CURVE - SINGLE CURVE SETUP
		 *
		 * Note: Only maturity > 0 (DiscountCurveInterpolation) and fixing > 0 (ForwardCurve) are calibration parameters (!)
		 */

		System.out.println("Calibrating a discount curve from swaps (single-curve/self-discounting).");

		// Create a discount curve
		final DiscountCurveInterpolation			discountCurve					= DiscountCurveInterpolation.createDiscountCurveFromDiscountFactors(
				"discountCurve"								/* name */,
				new double[] {0.0,  1.0,  2.0,  4.0,  5.0}	/* maturities */,
				new double[] {1.0, 0.95, 0.90, 0.85, 0.80}	/* discount factors */,
				interpolationMethod,
				ExtrapolationMethod.CONSTANT,
				InterpolationEntity.LOG_OF_VALUE
				);

		// Create a forward curve from that discount curve for forward rates
		final ForwardCurveInterface	forwardCurveFromDiscountCurve	= new ForwardCurveFromDiscountCurve(
				discountCurve.getName()						/* name of the discount curve to use */,
				null										/* reference date: not specified since single curve setup */,
				null										/* period length: not specified since single curve setup */
				);

		// Create a collection of objective functions (calibration products)
		final Vector<AnalyticProduct> calibrationProducts1 = new Vector<>();

		calibrationProducts1.add(new Swap(new RegularSchedule(new TimeDiscretizationFromArray(0.0, 1, 1.0)), null, 0.05, "discountCurve", new RegularSchedule(new TimeDiscretizationFromArray(0.0, 1, 1.0)), forwardCurveFromDiscountCurve.getName(), 0.0, "discountCurve"));
		calibrationProducts1.add(new Swap(new RegularSchedule(new TimeDiscretizationFromArray(0.0, 2, 1.0)), null, 0.04, "discountCurve", new RegularSchedule(new TimeDiscretizationFromArray(0.0, 2, 1.0)), forwardCurveFromDiscountCurve.getName(), 0.0, "discountCurve"));
		calibrationProducts1.add(new Swap(new RegularSchedule(new TimeDiscretizationFromArray(0.0, 8, 0.5)), null, 0.03, "discountCurve", new RegularSchedule(new TimeDiscretizationFromArray(0.0, 8, 0.5)), forwardCurveFromDiscountCurve.getName(), 0.0, "discountCurve"));
		calibrationProducts1.add(new Swap(new RegularSchedule(new TimeDiscretizationFromArray(0.0, 10, 0.5)), null, 0.04, "discountCurve", new RegularSchedule(new TimeDiscretizationFromArray(0.0, 10, 0.5)), forwardCurveFromDiscountCurve.getName(), 0.0, "discountCurve"));

		// A model is a collection of curves (curves and products find other curves by looking up their name in the model)
		final AnalyticModelFromCurvesAndVols model1 = new AnalyticModelFromCurvesAndVols(new Curve[] { discountCurve , forwardCurveFromDiscountCurve });

		// Create a collection of curves to calibrate
		final Set<ParameterObject> curvesToCalibrate1 = new HashSet<>();
		curvesToCalibrate1.add(discountCurve);

		// Calibrate the curve
		final Solver solver1 = new Solver(model1, calibrationProducts1);
		final AnalyticModel calibratedModel1 = solver1.getCalibratedModel(curvesToCalibrate1);
		System.out.println("The solver required " + solver1.getIterations() + " iterations.");
		System.out.println("The best fit curve is:");
		System.out.println(calibratedModel1.getCurve(discountCurve.getName()).toString());

		/*
		 * The model calibratedModel1 now contains a set af calibrated curves.
		 * The curves are clones. The model model1 still contains the original curve.
		 */

		// Calibration check
		System.out.println("Calibration check:");
		final double evaluationTime = 0.0;
		double error = 0;
		for(int calibrationProductIndex = 0; calibrationProductIndex < calibrationProducts1.size(); calibrationProductIndex++) {
			final AnalyticProduct	calibrationProduct		= calibrationProducts1.get(calibrationProductIndex);
			final RandomVariable		calibrationProductValue	= calibrationProduct.getValue(evaluationTime, calibratedModel1);
			System.out.println("Calibration product " + calibrationProductIndex + ":\t" + calibrationProductValue);

			error += calibrationProductValue.getVariance();
		}
		error = Math.sqrt(error);
		Assert.assertTrue(error < errorTolerance);

		System.out.println("__________________________________________________________________________________________\n");


		/*
		 * CALIBRATE A FORWARD CURVE, USING THE GIVEN DISCOUNT CURVE (MULTI-CURVE SETUP)
		 *
		 * Note: Only maturity > 0 (DiscountCurveInterpolation) and fixing > 0 (ForwardCurve) are calibration parameters (!)
		 */

		// Create initial guess for the curve
		final ForwardCurveInterpolation forwardCurveInterpolation = ForwardCurveInterpolation.createForwardCurveFromForwards("forwardCurve", new double[] {2.0/365.0, 1.0, 2.0, 3.0, 4.0}, new double[] {0.05, 0.05, 0.05, 0.05, 0.05}, model1, discountCurve.getName(), 0.5);

		// Make collection of all curves used in valuation
		final AnalyticModelFromCurvesAndVols model2 = new AnalyticModelFromCurvesAndVols( new Curve[] { discountCurve, forwardCurveInterpolation } );

		System.out.println("Calibrating a forward curve from swaps using the given discount curve.");

		// Create a collection of objective functions (calibration products)
		final Vector<AnalyticProduct> calibrationProducts2 = new Vector<>();

		// It is possible to mix tenors (although it may not be meaningful in a forward curve calibration)
		calibrationProducts2.add(new Swap(new RegularSchedule(new TimeDiscretizationFromArray(0.0, 1, 1.0)), null, 0.06, "discountCurve", new RegularSchedule(new TimeDiscretizationFromArray(0.0, 1, 0.5)), "forwardCurve", 0.0, "discountCurve"));
		calibrationProducts2.add(new Swap(new RegularSchedule(new TimeDiscretizationFromArray(0.0, 2, 1.0)), null, 0.05, "discountCurve", new RegularSchedule(new TimeDiscretizationFromArray(0.0, 2, 0.5)), "forwardCurve", 0.0, "discountCurve"));
		calibrationProducts2.add(new Swap(new RegularSchedule(new TimeDiscretizationFromArray(0.0, 6, 0.5)), null, 0.04, "discountCurve", new RegularSchedule(new TimeDiscretizationFromArray(0.0, 6, 0.5)), "forwardCurve", 0.0, "discountCurve"));
		calibrationProducts2.add(new Swap(new RegularSchedule(new TimeDiscretizationFromArray(0.0, 8, 0.5)), null, 0.04, "discountCurve", new RegularSchedule(new TimeDiscretizationFromArray(0.0, 8, 0.5)), "forwardCurve", 0.0, "discountCurve"));
		calibrationProducts2.add(new Swap(new RegularSchedule(new TimeDiscretizationFromArray(0.0, 10, 0.5)), null, 0.04, "discountCurve", new RegularSchedule(new TimeDiscretizationFromArray(0.0, 10, 0.5)), "forwardCurve", 0.0, "discountCurve"));

		// Create a collection of curves to calibrate
		final Set<ParameterObject> curvesToCalibrate2 = new HashSet<>();
		curvesToCalibrate2.add(forwardCurveInterpolation);

		// Calibrate the curve
		final Solver solver2 = new Solver(model2, calibrationProducts2);
		final AnalyticModel calibratedModel2 = solver2.getCalibratedModel(curvesToCalibrate2);
		System.out.println("The solver required " + solver2.getIterations() + " iterations.");
		System.out.println("The best fit curve is:");
		System.out.println(calibratedModel2.getCurve(forwardCurveInterpolation.getName()).toString());

		/*
		 * The model calibratedModel2 now contains a set of calibrated curves.
		 * The curves are clones. The model model2 still contains the original curve.
		 */

		// Calibration check
		System.out.println("Calibration check:");
		double error2 = 0;
		for(int calibrationProductIndex = 0; calibrationProductIndex < calibrationProducts2.size(); calibrationProductIndex++) {
			final AnalyticProduct	calibrationProduct		= calibrationProducts2.get(calibrationProductIndex);
			final RandomVariable		calibrationProductValue	= calibrationProduct.getValue(evaluationTime, calibratedModel2);
			System.out.println("Calibration product " + calibrationProductIndex + ":\t" + calibrationProductValue);

			error2 += calibrationProductValue.getVariance();
		}
		error2 = Math.sqrt(error2);
		Assert.assertTrue(error2 < errorTolerance);

		System.out.println("__________________________________________________________________________________________\n");
	}
}
