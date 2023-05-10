package net.finmath.montecarlo;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import net.finmath.stochastic.RandomVariable;

public class RandomVariableFromDoubleArrayTest {

	@Test
	public void testSize() {
		double[] values = { 1.0, 2.0, 3.0 };
		RandomVariable variable = new RandomVariableFromDoubleArray(0.0, values);

		assertEquals(3, variable.size());
	}

	@Test
	public void testGet() {
		double[] values = { 1.0, 2.0, 3.0 };
		RandomVariable variable = new RandomVariableFromDoubleArray(0.0, values);

		assertEquals(1.0, variable.get(0), 0.0);
		assertEquals(2.0, variable.get(1), 0.0);
		assertEquals(3.0, variable.get(2), 0.0);
	}

	@Test
	public void testGetRealizations() {
		double[] values = { 1.0, 2.0, 3.0 };
		RandomVariable variable = new RandomVariableFromDoubleArray(0.0, values);

		double[] realizations = variable.getRealizations();

		assertEquals(3, realizations.length);
		assertEquals(1.0, realizations[0], 0.0);
		assertEquals(2.0, realizations[1], 0.0);
		assertEquals(3.0, realizations[2], 0.0);
	}

	@Test
	public void testApply() {
		double[] values = { 1.0, 2.0, 3.0 };
		RandomVariable variable = new RandomVariableFromDoubleArray(0.0, values);

		RandomVariable result = variable.apply(x -> x * 2);

		assertEquals(3, result.size());
		assertEquals(2.0, result.get(0), 0.0);
		assertEquals(4.0, result.get(1), 0.0);
		assertEquals(6.0, result.get(2), 0.0);
	}

	@Test
	public void testAdd() {
	    double[] values1 = { 1.0, 2.0, 3.0 };
	    RandomVariable variable1 = new RandomVariableFromDoubleArray(0.0, values1);

	    double[] values2 = { 2.0, 3.0, 4.0 };
	    RandomVariable variable2 = new RandomVariableFromDoubleArray(0.0, values2);

	    RandomVariable result = variable1.add(variable2);

	    assertEquals(3, result.size());
	    assertEquals(3.0, result.get(0), 0.0);
	    assertEquals(5.0, result.get(1), 0.0);
	    assertEquals(7.0, result.get(2), 0.0);
	}
	
	@Test
	public void testMult() {
	    double[] values1 = { 1.0, 2.0, 3.0 };
	    RandomVariable variable1 = new RandomVariableFromDoubleArray(0.0, values1);

	    double[] values2 = { 2.0, 3.0, 4.0 };
	    RandomVariable variable2 = new RandomVariableFromDoubleArray(0.0, values2);

	    RandomVariable result = variable1.mult(variable2);

	    assertEquals(3, result.size());
	    assertEquals(2.0, result.get(0), 0.0);
	    assertEquals(6.0, result.get(1), 0.0);
	    assertEquals(12.0, result.get(2), 0.0);

	    RandomVariable result2 = variable1.mult(2.0);

	    assertEquals(3, result2.size());
	    assertEquals(2.0, result2.get(0), 0.0);
	    assertEquals(4.0, result2.get(1), 0.0);
	    assertEquals(6.0, result2.get(2), 0.0);
	}

	@Test
	public void testDiv() {
	    double[] values1 = { 2.0, 4.0, 6.0 };
	    RandomVariable variable1 = new RandomVariableFromDoubleArray(0.0, values1);

	    double[] values2 = { 1.0, 2.0, 3.0 };
	    RandomVariable variable2 = new RandomVariableFromDoubleArray(0.0, values2);

	    RandomVariable result = variable1.div(variable2);

	    assertEquals(3, result.size());
	    assertEquals(2.0, result.get(0), 0.0);
	    assertEquals(2.0, result.get(1), 0.0);
	    assertEquals(2.0, result.get(2), 0.0);

	    RandomVariable result2 = variable1.div(2.0);

	    assertEquals(3, result2.size());
	    assertEquals(1.0, result2.get(0), 0.0);
	    assertEquals(2.0, result2.get(1), 0.0);
	    assertEquals(3.0, result2.get(2), 0.0);
	}
	
	@Test
	public void testAverage() {
	    double[] values = { 1.0, 2.0, 3.0 };
	    RandomVariable variable = new RandomVariableFromDoubleArray(0.0, values);

	    double expectedAverage = (values[0] + values[1] + values[2]) / values.length;
	    double actualAverage = variable.average().doubleValue();

	    assertEquals(expectedAverage, actualAverage, 0.0);
	}

	@Test
	public void testVariance() {
	    double[] values = { 1.0, 2.0, 3.0 };
	    RandomVariable variable = new RandomVariableFromDoubleArray(0.0, values);

	    double expectedVariance = ((values[0] - variable.average().doubleValue()) * (values[0] - variable.average().doubleValue())
	        + (values[1] - variable.average().doubleValue()) * (values[1] - variable.average().doubleValue())
	        + (values[2] - variable.average().doubleValue()) * (values[2] - variable.average().doubleValue())) / (values.length);
	    double actualVariance = variable.variance().doubleValue();

	    assertEquals(expectedVariance, actualVariance, 0.0);
	}

}