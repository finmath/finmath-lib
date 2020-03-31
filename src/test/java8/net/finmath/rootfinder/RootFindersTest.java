/*
 * Created on 02.12.2004
 *
 * (c) Copyright Christian P. Fries, Germany.
 * Contact: email@christian-fries.de.
 */
package net.finmath.rootfinder;

import java.text.DecimalFormat;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Christian Fries
 * @version 1.1
 * @date 2008-04-06
 */
public class RootFindersTest {

	@Test
	public void testRootFinders() {
		System.out.println("Applying root finders to x^3 + 2*y^2 + x + 1 = 0\n");

		System.out.println("Root finders without derivative:");
		System.out.println("--------------------------------");

		RootFinder rootFinder;

		rootFinder = new BisectionSearch(-10.0,10.0);
		testRootFinder(rootFinder);

		rootFinder = new RiddersMethod(-10.0,10.0);
		testRootFinder(rootFinder);

		rootFinder = new SecantMethod(2.0,10.0);
		testRootFinder(rootFinder);

		System.out.println();

		System.out.println("Root finders with    derivative:");
		System.out.println("--------------------------------");

		RootFinderWithDerivative rootFinderWithDerivative;

		rootFinderWithDerivative = new NewtonsMethod(2.0);
		testRootFinderWithDerivative(rootFinderWithDerivative);

		rootFinderWithDerivative = new SecantMethod(2.0,10.0);
		testRootFinderWithDerivative(rootFinderWithDerivative);
	}

	public static void testRootFinder(final RootFinder rootFinder) {
		System.out.println("Testing " + rootFinder.getClass().getName() + ":");

		// Find a solution to x^3 + x^2 + x + 1 = 0
		while(rootFinder.getAccuracy() > 1E-11 && !rootFinder.isDone()) {
			final double x = rootFinder.getNextPoint();

			// Our test function. Analytic solution is -1.0.
			final double y = x*x*x + x*x + x + 1;

			rootFinder.setValue(y);
		}

		// Print result:
		final DecimalFormat formatter = new DecimalFormat("0.00E00");
		System.out.print("Root......: "+formatter.format(rootFinder.getBestPoint())+"\t");
		System.out.print("Accuracy..: "+formatter.format(rootFinder.getAccuracy() )+"\t");
		System.out.print("Iterations: "+rootFinder.getNumberOfIterations() +"\n");

		Assert.assertEquals("x such that x^3 + x^2 + x + 1 = 0", -1.0, rootFinder.getBestPoint(), 1E-12);
	}

	public static void testRootFinderWithDerivative(
			final RootFinderWithDerivative rootFinder) {
		System.out.println("Testing " + rootFinder.getClass().getName() + ":");

		// Find a solution to x^3 + x^2 + x + 1 = 0
		while(rootFinder.getAccuracy() > 1E-11 && !rootFinder.isDone()) {
			final double x = rootFinder.getNextPoint();

			final double y = x*x*x + x*x + x + 1;
			final double p = 3*x*x + 2*x + 1;

			rootFinder.setValueAndDerivative(y,p);
		}

		// Print result:
		final DecimalFormat formatter = new DecimalFormat("0.00E00");
		System.out.print("Root......: "+formatter.format(rootFinder.getBestPoint())+"\t");
		System.out.print("Accuracy..: "+formatter.format(rootFinder.getAccuracy() )+"\t");
		System.out.print("Iterations: "+rootFinder.getNumberOfIterations() +"\n");

		Assert.assertEquals("x such that x^3 + x^2 + x + 1 = 0", -1.0, rootFinder.getBestPoint(), 1E-12);
	}
}
