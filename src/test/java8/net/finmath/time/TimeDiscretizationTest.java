package net.finmath.time;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

public class TimeDiscretizationTest {

	private static final double ONE_TENTH_OF_DEFAULT_TICK_SIZE = 0.1 / (365.0 * 24.0);

	private static final double DEFAULT_TICK_SIZE = 1.0 / (365.0 * 24.0);

	private static double getHalfTickMore(final double a) {
		return a + 0.5 / (365.0 * 24.0);
	}

	@Before
	public void setUp() {
		System.setProperty("net.finmath.functions.TimeDiscretization.timeTickSize", Double.toString(DEFAULT_TICK_SIZE));
	}

	@Test
	public void constructWithUnboxedArrayAtDefaultTickSize() {

		final double a = 4.2;
		final double closeToA = getHalfTickMore(a);

		final TimeDiscretizationFromArray discretization = new TimeDiscretizationFromArray(a, closeToA);

		assertThat(discretization.getAsDoubleArray(), is(equalTo(new double[]{a})));
	}

	@Test
	public void constructWithBoxedArrayAtDefaultTickSize() {

		final double a = 4.2;
		final double closeToA = getHalfTickMore(a);

		final TimeDiscretizationFromArray discretization = new TimeDiscretizationFromArray(new Double[]{a, closeToA});

		assertThat(discretization.getAsDoubleArray(), is(equalTo(new double[]{a})));
	}

	@Test
	public void constructWithBoxedArrayAtBigTickSize() {

		final double a = 4.2;
		final double closeToA = getHalfTickMore(a);

		final TimeDiscretizationFromArray discretization = new TimeDiscretizationFromArray(new Double[]{a, closeToA}, 1.0);

		assertThat(discretization.getAsDoubleArray(), is(equalTo(new double[]{4.0})));
	}

	@Test
	public void constructWithBoxedArrayAtSmallTickSize() {

		final double a = 4.2;
		final double closeToA = getHalfTickMore(a);

		final TimeDiscretizationFromArray discretization = new TimeDiscretizationFromArray(new Double[]{a, closeToA}, ONE_TENTH_OF_DEFAULT_TICK_SIZE);

		assertThat(discretization.getAsDoubleArray(), is(equalTo(new double[]{a, closeToA})));
	}

	@Test
	public void constructWithSetAtDefaultTickSize() {

		final double a = 4.2;
		final double closeToA = getHalfTickMore(a);

		final TimeDiscretizationFromArray discretization = new TimeDiscretizationFromArray(Arrays.stream(new double[]{a, closeToA}).boxed().collect(Collectors.toSet()));

		assertThat(discretization.getAsDoubleArray(), is(equalTo(new double[]{a})));
	}

	@Test
	public void constructWithSetAtBigTickSize() {

		final double a = 4.2;
		final double closeToA = getHalfTickMore(a);

		final TimeDiscretizationFromArray discretization = new TimeDiscretizationFromArray(Arrays.stream(new double[]{a, closeToA}).boxed().collect(Collectors.toSet()), 1.0);

		assertThat(discretization.getAsDoubleArray(), is(equalTo(new double[]{4.0})));
	}

	@Test
	public void constructWithSetAtSmallTickSize() {

		final double a = 4.2;
		final double closeToA = getHalfTickMore(a);

		final TimeDiscretizationFromArray discretization = new TimeDiscretizationFromArray(Arrays.stream(new double[]{a, closeToA}).boxed().collect(Collectors.toSet()), ONE_TENTH_OF_DEFAULT_TICK_SIZE);

		assertThat(discretization.getAsDoubleArray(), is(equalTo(new double[]{a, closeToA})));
	}

	@Test
	public void constructWithArrayListAtDefaultTickSize() {

		final double a = 4.2;
		final double closeToA = getHalfTickMore(a);

		final TimeDiscretizationFromArray discretization = new TimeDiscretizationFromArray(new ArrayList<>(Arrays.stream(new double[]{a, closeToA}).boxed().collect(Collectors.toList())));

		assertThat(discretization.getAsDoubleArray(), is(equalTo(new double[]{a})));
	}

	@Test
	public void constructWithArrayListAtBigTickSize() {

		final double a = 4.2;
		final double closeToA = getHalfTickMore(a);

		final TimeDiscretizationFromArray discretization = new TimeDiscretizationFromArray(new ArrayList<>(Arrays.stream(new double[]{a, closeToA}).boxed().collect(Collectors.toList())), 1.0);

		assertThat(discretization.getAsDoubleArray(), is(equalTo(new double[]{4.0})));
	}

	@Test
	public void constructWithArrayListAtSmallTickSize() {

		final double a = 4.2;
		final double closeToA = getHalfTickMore(a);

		final TimeDiscretizationFromArray discretization = new TimeDiscretizationFromArray(new ArrayList<>(Arrays.stream(new double[]{a, closeToA}).boxed().collect(Collectors.toList())), ONE_TENTH_OF_DEFAULT_TICK_SIZE);

		assertThat(discretization.getAsDoubleArray(), is(equalTo(new double[]{a, closeToA})));
	}

	@Test
	public void constructWithNumberOfSteps() {
		final TimeDiscretizationFromArray discretization = new TimeDiscretizationFromArray(0.0, 5, 0.5);

		assertThat(discretization.getAsDoubleArray(), is(equalTo(new double[]{0.0, 0.5, 1.0, 1.5, 2.0, 2.5})));
	}

	@Test
	public void constructWithNumberOfStepsSmallerThanTickSize() {
		final TimeDiscretizationFromArray discretization = new TimeDiscretizationFromArray(0.0, 10, ONE_TENTH_OF_DEFAULT_TICK_SIZE);

		assertThat(discretization.getAsDoubleArray(), is(equalTo(new double[]{0.0, DEFAULT_TICK_SIZE})));
	}

	@Test
	public void constructWithIntervalShortStubAtEnd() {
		final TimeDiscretizationFromArray discretization = new TimeDiscretizationFromArray(0.0, 2.75, 0.5, TimeDiscretizationFromArray.ShortPeriodLocation.SHORT_PERIOD_AT_END);

		assertThat(discretization.getAsDoubleArray(), is(equalTo(new double[]{0.0, 0.5, 1.0, 1.5, 2.0, 2.5, 2.75})));
	}

	@Test
	public void constructWithIntervalShortStubAtFront() {
		final TimeDiscretizationFromArray discretization = new TimeDiscretizationFromArray(0.0, 2.75, 0.5, TimeDiscretizationFromArray.ShortPeriodLocation.SHORT_PERIOD_AT_START);

		assertThat(discretization.getAsDoubleArray(), is(equalTo(new double[]{0.0, 0.25, 0.75, 1.25, 1.75, 2.25, 2.75})));
	}

	@Test
	public void testUnionWithNoDuplicates() {

		final double[] leftTimes = {1.0, 2.0, 3.0};
		final double[] rightTimes = {0.5, 1.5};

		final TimeDiscretization union = new TimeDiscretizationFromArray(leftTimes).union(new TimeDiscretizationFromArray(rightTimes));

		assertThat(union.getAsDoubleArray(), is(equalTo(new double[]{0.5, 1.0, 1.5, 2.0, 3.0})));
	}

	@Test
	public void testUnionWithSubTickDuplicates() {

		final Double[] leftTimes = {1.0, 2.0, 3.0};
		final Double[] rightTimes = {0.5, 1.5, 1.53};

		final TimeDiscretization union = new TimeDiscretizationFromArray(leftTimes, 0.1).union(new TimeDiscretizationFromArray(rightTimes, 0.1));

		assertThat(union.getAsDoubleArray(), is(equalTo(new double[]{0.5, 1.0, 1.5, 2.0, 3.0})));
	}

	@Test
	public void testUnionWithDifferentTickSizes() {

		final Double[] leftTimes = {7.0, 3.0, 5.0, 3.0};
		final Double[] rightTimes = {5.0, 3.0, 1.0, 8.0, 0.0, 0.0, 8.0};

		final TimeDiscretization union = new TimeDiscretizationFromArray(leftTimes, 0.1).union(new TimeDiscretizationFromArray(rightTimes, 1.0));

		assertThat(union.getTickSize(), is(equalTo(0.1)));
	}

	@Test
	public void testIntersectionWithNoDuplicates() {

		final double[] leftTimes = {1.0, 2.0, 3.0};
		final double[] rightTimes = {0.5, 1.5};

		final TimeDiscretization intersection = new TimeDiscretizationFromArray(leftTimes).intersect(new TimeDiscretizationFromArray(rightTimes));

		assertThat(intersection.getAsDoubleArray().length, is(equalTo(0)));
	}

	@Test
	public void testIntersectionWithSubTickDuplicates() {

		final Double[] leftTimes = {1.0, 1.53, 3.0};
		final Double[] rightTimes = {0.5, 1.5};

		final TimeDiscretization intersection = new TimeDiscretizationFromArray(leftTimes, 0.1).intersect(new TimeDiscretizationFromArray(rightTimes, 0.1));

		assertThat(intersection.getAsDoubleArray(), is(equalTo(new double[]{1.5})));
	}

	@Test
	public void testIntersectionWithDifferentTickSizes() {

		final Double[] leftTimes = {7.0, 3.0, 5.0, 3.0};
		final Double[] rightTimes = {5.0, 3.0, 1.0, 8.0, 0.0, 0.0, 8.0};

		final TimeDiscretization intersection = new TimeDiscretizationFromArray(leftTimes, 0.1).intersect(new TimeDiscretizationFromArray(rightTimes, 1.0));

		assertThat(intersection.getTickSize(), is(equalTo(1.0)));
	}
}
