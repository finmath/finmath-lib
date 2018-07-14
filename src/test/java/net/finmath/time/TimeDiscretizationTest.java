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

	private static double getHalfTickMore(double a) {
		return a + 0.5 / (365.0 * 24.0);
	}

	@Before
	public void setUp() {
		System.setProperty("net.finmath.functions.TimeDiscretization.timeTickSize", Double.toString(DEFAULT_TICK_SIZE));
	}

	@Test
	public void constructWithUnboxedArrayAtDefaultTickSize() {

		double a = 4.2;
		double closeToA = getHalfTickMore(a);

		TimeDiscretization discretization = new TimeDiscretization(a, closeToA);

		assertThat(discretization.getAsDoubleArray(), is(equalTo(new double[]{a})));
	}

	@Test
	public void constructWithBoxedArrayAtDefaultTickSize() {

		double a = 4.2;
		double closeToA = getHalfTickMore(a);

		TimeDiscretization discretization = new TimeDiscretization(new Double[]{a, closeToA});

		assertThat(discretization.getAsDoubleArray(), is(equalTo(new double[]{a})));
	}

	@Test
	public void constructWithBoxedArrayAtBigTickSize() {

		double a = 4.2;
		double closeToA = getHalfTickMore(a);

		TimeDiscretization discretization = new TimeDiscretization(new Double[]{a, closeToA}, 1.0);

		assertThat(discretization.getAsDoubleArray(), is(equalTo(new double[]{4.0})));
	}

	@Test
	public void constructWithBoxedArrayAtSmallTickSize() {

		double a = 4.2;
		double closeToA = getHalfTickMore(a);

		TimeDiscretization discretization = new TimeDiscretization(new Double[]{a, closeToA}, ONE_TENTH_OF_DEFAULT_TICK_SIZE);

		assertThat(discretization.getAsDoubleArray(), is(equalTo(new double[]{a, closeToA})));
	}

	@Test
	public void constructWithSetAtDefaultTickSize() {

		double a = 4.2;
		double closeToA = getHalfTickMore(a);

		TimeDiscretization discretization = new TimeDiscretization(Arrays.stream(new double[]{a, closeToA}).boxed().collect(Collectors.toSet()));

		assertThat(discretization.getAsDoubleArray(), is(equalTo(new double[]{a})));
	}

	@Test
	public void constructWithSetAtBigTickSize() {

		double a = 4.2;
		double closeToA = getHalfTickMore(a);

		TimeDiscretization discretization = new TimeDiscretization(Arrays.stream(new double[]{a, closeToA}).boxed().collect(Collectors.toSet()), 1.0);

		assertThat(discretization.getAsDoubleArray(), is(equalTo(new double[]{4.0})));
	}

	@Test
	public void constructWithSetAtSmallTickSize() {

		double a = 4.2;
		double closeToA = getHalfTickMore(a);

		TimeDiscretization discretization = new TimeDiscretization(Arrays.stream(new double[]{a, closeToA}).boxed().collect(Collectors.toSet()), ONE_TENTH_OF_DEFAULT_TICK_SIZE);

		assertThat(discretization.getAsDoubleArray(), is(equalTo(new double[]{a, closeToA})));
	}

	@Test
	public void constructWithArrayListAtDefaultTickSize() {

		double a = 4.2;
		double closeToA = getHalfTickMore(a);

		TimeDiscretization discretization = new TimeDiscretization(new ArrayList<>(Arrays.stream(new double[]{a, closeToA}).boxed().collect(Collectors.toList())));

		assertThat(discretization.getAsDoubleArray(), is(equalTo(new double[]{a})));
	}

	@Test
	public void constructWithArrayListAtBigTickSize() {

		double a = 4.2;
		double closeToA = getHalfTickMore(a);

		TimeDiscretization discretization = new TimeDiscretization(new ArrayList<>(Arrays.stream(new double[]{a, closeToA}).boxed().collect(Collectors.toList())), 1.0);

		assertThat(discretization.getAsDoubleArray(), is(equalTo(new double[]{4.0})));
	}

	@Test
	public void constructWithArrayListAtSmallTickSize() {

		double a = 4.2;
		double closeToA = getHalfTickMore(a);

		TimeDiscretization discretization = new TimeDiscretization(new ArrayList<>(Arrays.stream(new double[]{a, closeToA}).boxed().collect(Collectors.toList())), ONE_TENTH_OF_DEFAULT_TICK_SIZE);

		assertThat(discretization.getAsDoubleArray(), is(equalTo(new double[]{a, closeToA})));
	}

	@Test
	public void constructWithNumberOfSteps() {
		TimeDiscretization discretization = new TimeDiscretization(0.0, 5, 0.5);

		assertThat(discretization.getAsDoubleArray(), is(equalTo(new double[]{0.0, 0.5, 1.0, 1.5, 2.0, 2.5})));
	}

	@Test
	public void constructWithNumberOfStepsSmallerThanTickSize() {
		TimeDiscretization discretization = new TimeDiscretization(0.0, 10, ONE_TENTH_OF_DEFAULT_TICK_SIZE);

		assertThat(discretization.getAsDoubleArray(), is(equalTo(new double[]{0.0, DEFAULT_TICK_SIZE})));
	}

	@Test
	public void constructWithIntervalShortStubAtEnd() {
		TimeDiscretization discretization = new TimeDiscretization(0.0, 2.75, 0.5, TimeDiscretization.ShortPeriodLocation.SHORT_PERIOD_AT_END);

		assertThat(discretization.getAsDoubleArray(), is(equalTo(new double[]{0.0, 0.5, 1.0, 1.5, 2.0, 2.5, 2.75})));
	}

	@Test
	public void constructWithIntervalShortStubAtFront() {
		TimeDiscretization discretization = new TimeDiscretization(0.0, 2.75, 0.5, TimeDiscretization.ShortPeriodLocation.SHORT_PERIOD_AT_START);

		assertThat(discretization.getAsDoubleArray(), is(equalTo(new double[]{0.0, 0.25, 0.75, 1.25, 1.75, 2.25, 2.75})));
	}

	@Test
	public void testUnionWithNoDuplicates() {

		double[] leftTimes = {1.0, 2.0, 3.0};
		double[] rightTimes = {0.5, 1.5};

		TimeDiscretizationInterface union = new TimeDiscretization(leftTimes).union(new TimeDiscretization(rightTimes));

		assertThat(union.getAsDoubleArray(), is(equalTo(new double[]{0.5, 1.0, 1.5, 2.0, 3.0})));
	}

	@Test
	public void testUnionWithSubTickDuplicates() {

		Double[] leftTimes = {1.0, 2.0, 3.0};
		Double[] rightTimes = {0.5, 1.5, 1.53};

		TimeDiscretizationInterface union = new TimeDiscretization(leftTimes, 0.1).union(new TimeDiscretization(rightTimes, 0.1));

		assertThat(union.getAsDoubleArray(), is(equalTo(new double[]{0.5, 1.0, 1.5, 2.0, 3.0})));
	}

	@Test
	public void testUnionWithDifferentTickSizes() {

		Double[] leftTimes = {7.0, 3.0, 5.0, 3.0};
		Double[] rightTimes = {5.0, 3.0, 1.0, 8.0, 0.0, 0.0, 8.0};

		TimeDiscretizationInterface union = new TimeDiscretization(leftTimes, 0.1).union(new TimeDiscretization(rightTimes, 1.0));

		assertThat(union.getTickSize(), is(equalTo(0.1)));
	}

	@Test
	public void testIntersectionWithNoDuplicates() {

		double[] leftTimes = {1.0, 2.0, 3.0};
		double[] rightTimes = {0.5, 1.5};

		TimeDiscretizationInterface intersection = new TimeDiscretization(leftTimes).intersect(new TimeDiscretization(rightTimes));

		assertThat(intersection.getAsDoubleArray().length, is(equalTo(0)));
	}

	@Test
	public void testIntersectionWithSubTickDuplicates() {

		Double[] leftTimes = {1.0, 1.53, 3.0};
		Double[] rightTimes = {0.5, 1.5};

		TimeDiscretizationInterface intersection = new TimeDiscretization(leftTimes, 0.1).intersect(new TimeDiscretization(rightTimes, 0.1));

		assertThat(intersection.getAsDoubleArray(), is(equalTo(new double[]{1.5})));
	}

	@Test
	public void testIntersectionWithDifferentTickSizes() {

		Double[] leftTimes = {7.0, 3.0, 5.0, 3.0};
		Double[] rightTimes = {5.0, 3.0, 1.0, 8.0, 0.0, 0.0, 8.0};

		TimeDiscretizationInterface intersection = new TimeDiscretization(leftTimes, 0.1).intersect(new TimeDiscretization(rightTimes, 1.0));

		assertThat(intersection.getTickSize(), is(equalTo(1.0)));
	}
}

