/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2009
 */
package net.finmath.stochastic;

import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntToDoubleFunction;
import java.util.stream.DoubleStream;

import net.finmath.functions.DoubleTernaryOperator;

/**
 * An implementation of <code>RandomVariableArray</code> implementing an array of
 * <code>RandomVariable</code> objects,
 * implementing the <code>RandomVariable</code> interface.
 *
 * @author Christian Fries
 */
public class RandomVariableArrayImplementation implements RandomVariableArray {

	private static final long serialVersionUID = -5718980901166760522L;

	private final RandomVariable[] elements;

	public static RandomVariableArray of(final RandomVariable[] elements) {
		return new RandomVariableArrayImplementation(elements.clone());
	}

	private RandomVariableArrayImplementation(final RandomVariable[] elements) {
		super();

		if(elements.length == 0) {
			throw new IllegalArgumentException("Empty array.");
		}

		// Ensure that all elements
		final int level = getLevel(elements[0]);
		for(int i=1; i<elements.length; i++) {
			if(getLevel(elements[i]) != level) {
				throw new IllegalArgumentException("Elements must be of same array type.");
			}
		}

		this.elements = elements;
	}

	private int getLevel(final RandomVariable randomVariable) {
		if(randomVariable instanceof RandomVariableArray) {
			return ((RandomVariableArray) randomVariable).getLevel();
		}
		else {
			return 0;
		}
	}

	@Override
	public int getNumberOfElements() {
		return elements.length;
	}

	@Override
	public RandomVariable getElement(final int index) {
		return elements[index];
	}

	@Override
	public RandomVariableArray map(final RandomOperator operator) {
		final RandomVariable[] newElments = new RandomVariable[getNumberOfElements()];
		for(int i=1; i<elements.length; i++) {
			newElments[i] = operator.apply(elements[i]);
		}
		return new RandomVariableArrayImplementation(newElments);
	}

	@Override
	public RandomVariable sumProduct(final RandomVariableArray array) {
		RandomVariable result = elements[0].mult(array.getElement(0));
		for(int i=1; i<elements.length; i++) {
			result = result.add(elements[i].mult(array.getElement(i)));
		}
		return result;
	}

	/*
	 * net.finmath.stochastic.RandomVariable
	 */

	@Override
	public boolean equals(final RandomVariable randomVariable) {
		boolean equal = (randomVariable instanceof RandomVariableArray);
		for(int i=0; i<getNumberOfElements() && equal; i++) {
			equal &= getElement(i).equals(((RandomVariableArray)randomVariable).getElement(i));
		}
		return equal;
	}

	@Override
	public double getFiltrationTime() {
		double time = Double.NEGATIVE_INFINITY;
		for(int i=0; i<getNumberOfElements(); i++) {
			time = Math.max(getElement(i).getFiltrationTime(), time);
		}
		return time;
	}

	@Override
	public int getTypePriority() {
		return 100;
	}


	@Override
	public double get(final int pathOrState) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int size() {
		int size = 1;
		for(int i=0; i<getNumberOfElements(); i++) {
			size = Math.max(size, getElement(i).size());
		}
		return size;
	}

	@Override
	public boolean isDeterministic() {
		boolean deterministic = true;
		for(int i=0; i<getNumberOfElements() && deterministic; i++) {
			deterministic &= getElement(i).isDeterministic();
		}
		return deterministic;
	}

	@Override
	public double[] getRealizations() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Double doubleValue() {
		throw new UnsupportedOperationException();
	}

	@Override
	public IntToDoubleFunction getOperator() {
		throw new UnsupportedOperationException();
	}

	@Override
	public DoubleStream getRealizationsStream() {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getMin() {
		double min = Double.POSITIVE_INFINITY;
		for(int i=0; i<getNumberOfElements(); i++) {
			min = Math.min(min, getElement(i).getMin());
		}
		return min;
	}

	@Override
	public double getMax() {
		double max = Double.NEGATIVE_INFINITY;
		for(int i=0; i<getNumberOfElements(); i++) {
			max = Math.max(max, getElement(i).getMax());
		}
		return max;
	}

	@Override
	public double getAverage() {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getAverage(final RandomVariable probabilities) {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getVariance() {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getVariance(final RandomVariable probabilities) {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getSampleVariance() {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getStandardDeviation() {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getStandardDeviation(final RandomVariable probabilities) {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getStandardError() {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getStandardError(final RandomVariable probabilities) {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getQuantile(final double quantile) {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getQuantile(final double quantile, final RandomVariable probabilities) {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getQuantileExpectation(final double quantileStart, final double quantileEnd) {
		throw new UnsupportedOperationException();
	}

	@Override
	public double[] getHistogram(final double[] intervalPoints) {
		throw new UnsupportedOperationException();
	}

	@Override
	public double[][] getHistogram(final int numberOfPoints, final double standardDeviations) {
		throw new UnsupportedOperationException();
	}

	@Override
	public RandomVariable cache() {
		return this;
	}

	@Override
	public RandomVariable apply(final DoubleUnaryOperator operator) {
		return map(new RandomOperator() {
			@Override
			public RandomVariable apply(final RandomVariable x) {
				return x.apply(operator);
			}
		});
	}

	@Override
	public RandomVariable apply(final DoubleBinaryOperator operator, final RandomVariable argument) {
		return map(new RandomOperator() {
			@Override
			public RandomVariable apply(final RandomVariable x) {
				return x.apply(operator, argument);
			}
		});
	}

	@Override
	public RandomVariable apply(final DoubleTernaryOperator operator, final RandomVariable argument1, final RandomVariable argument2) {
		return map(new RandomOperator() {
			@Override
			public RandomVariable apply(final RandomVariable x) {
				return x.apply(operator, argument1, argument2);
			}
		});
	}

	@Override
	public RandomVariable cap(final double cap) {
		return map(new RandomOperator() {
			@Override
			public RandomVariable apply(final RandomVariable x) {
				return x.cap(cap);
			}
		});
	}

	@Override
	public RandomVariable floor(final double floor) {
		return map(new RandomOperator() {
			@Override
			public RandomVariable apply(final RandomVariable x) {
				return x.floor(floor);
			}
		});
	}

	@Override
	public RandomVariable add(final double value) {
		return map(new RandomOperator() {
			@Override
			public RandomVariable apply(final RandomVariable x) {
				return x.add(value);
			}
		});
	}

	@Override
	public RandomVariable sub(final double value) {
		return map(new RandomOperator() {
			@Override
			public RandomVariable apply(final RandomVariable x) {
				return x.sub(value);
			}
		});
	}

	@Override
	public RandomVariable mult(final double value) {
		return map(new RandomOperator() {
			@Override
			public RandomVariable apply(final RandomVariable x) {
				return x.mult(value);
			}
		});
	}

	@Override
	public RandomVariable div(final double value) {
		return map(new RandomOperator() {
			@Override
			public RandomVariable apply(final RandomVariable x) {
				return x.div(value);
			}
		});
	}

	@Override
	public RandomVariable pow(final double exponent) {
		return map(new RandomOperator() {
			@Override
			public RandomVariable apply(final RandomVariable x) {
				return x.pow(exponent);
			}
		});
	}

	@Override
	public RandomVariable average() {
		return map(new RandomOperator() {
			@Override
			public RandomVariable apply(final RandomVariable x) {
				return x.average();
			}
		});
	}

	@Override
	public RandomVariable squared() {
		return map(new RandomOperator() {
			@Override
			public RandomVariable apply(final RandomVariable x) {
				return x.squared();
			}
		});
	}

	@Override
	public RandomVariable sqrt() {
		return map(new RandomOperator() {
			@Override
			public RandomVariable apply(final RandomVariable x) {
				return x.sqrt();
			}
		});
	}

	@Override
	public RandomVariable exp() {
		return map(new RandomOperator() {
			@Override
			public RandomVariable apply(final RandomVariable x) {
				return x.exp();
			}
		});
	}

	@Override
	public RandomVariable log() {
		return map(new RandomOperator() {
			@Override
			public RandomVariable apply(final RandomVariable x) {
				return x.log();
			}
		});
	}

	@Override
	public RandomVariable sin() {
		return map(new RandomOperator() {
			@Override
			public RandomVariable apply(final RandomVariable x) {
				return x.sin();
			}
		});
	}

	@Override
	public RandomVariable cos() {
		return map(new RandomOperator() {
			@Override
			public RandomVariable apply(final RandomVariable x) {
				return x.cos();
			}
		});
	}

	@Override
	public RandomVariable add(final RandomVariable randomVariable) {
		return map(new RandomOperator() {
			@Override
			public RandomVariable apply(final RandomVariable x) {
				return x.add(randomVariable);
			}
		});
	}

	@Override
	public RandomVariable sub(final RandomVariable randomVariable) {
		return map(new RandomOperator() {
			@Override
			public RandomVariable apply(final RandomVariable x) {
				return x.sub(randomVariable);
			}
		});
	}

	@Override
	public RandomVariable bus(final RandomVariable randomVariable) {
		return map(new RandomOperator() {
			@Override
			public RandomVariable apply(final RandomVariable x) {
				return x.bus(randomVariable);
			}
		});
	}

	@Override
	public RandomVariable mult(final RandomVariable randomVariable) {
		return map(new RandomOperator() {
			@Override
			public RandomVariable apply(final RandomVariable x) {
				return x.mult(randomVariable);
			}
		});
	}

	@Override
	public RandomVariable div(final RandomVariable randomVariable) {
		return map(new RandomOperator() {
			@Override
			public RandomVariable apply(final RandomVariable x) {
				return x.div(randomVariable);
			}
		});
	}

	@Override
	public RandomVariable vid(final RandomVariable randomVariable) {
		return map(new RandomOperator() {
			@Override
			public RandomVariable apply(final RandomVariable x) {
				return x.vid(randomVariable);
			}
		});
	}

	@Override
	public RandomVariable cap(final RandomVariable cap) {
		return map(new RandomOperator() {
			@Override
			public RandomVariable apply(final RandomVariable x) {
				return x.cap(cap);
			}
		});
	}

	@Override
	public RandomVariable floor(final RandomVariable floor) {
		return map(new RandomOperator() {
			@Override
			public RandomVariable apply(final RandomVariable x) {
				return x.cap(floor);
			}
		});
	}

	@Override
	public RandomVariable accrue(final RandomVariable rate, final double periodLength) {
		return map(new RandomOperator() {
			@Override
			public RandomVariable apply(final RandomVariable x) {
				return x.accrue(rate, periodLength);
			}
		});
	}

	@Override
	public RandomVariable discount(final RandomVariable rate, final double periodLength) {
		return map(new RandomOperator() {
			@Override
			public RandomVariable apply(final RandomVariable x) {
				return x.discount(rate, periodLength);
			}
		});
	}

	@Override
	public RandomVariable choose(final RandomVariable valueIfTriggerNonNegative, final RandomVariable valueIfTriggerNegative) {
		return map(new RandomOperator() {
			@Override
			public RandomVariable apply(final RandomVariable x) {
				return x.choose(valueIfTriggerNonNegative, valueIfTriggerNegative);
			}
		});
	}

	@Override
	public RandomVariable invert() {
		return map(new RandomOperator() {
			@Override
			public RandomVariable apply(final RandomVariable x) {
				return x.invert();
			}
		});
	}

	@Override
	public RandomVariable abs() {
		return map(new RandomOperator() {
			@Override
			public RandomVariable apply(final RandomVariable x) {
				return x.abs();
			}
		});
	}

	@Override
	public RandomVariable addProduct(final RandomVariable factor1, final double factor2) {
		return map(new RandomOperator() {
			@Override
			public RandomVariable apply(final RandomVariable x) {
				return x.addProduct(factor1, factor2);
			}
		});
	}

	@Override
	public RandomVariable addProduct(final RandomVariable factor1, final RandomVariable factor2) {
		return map(new RandomOperator() {
			@Override
			public RandomVariable apply(final RandomVariable x) {
				return x.addProduct(factor1, factor2);
			}
		});
	}

	@Override
	public RandomVariable addRatio(final RandomVariable numerator, final RandomVariable denominator) {
		return map(new RandomOperator() {
			@Override
			public RandomVariable apply(final RandomVariable x) {
				return x.addRatio(numerator, denominator);
			}
		});
	}

	@Override
	public RandomVariable subRatio(final RandomVariable numerator, final RandomVariable denominator) {
		return map(new RandomOperator() {
			@Override
			public RandomVariable apply(final RandomVariable x) {
				return x.subRatio(numerator, denominator);
			}
		});
	}

	@Override
	public RandomVariable isNaN() {
		return map(new RandomOperator() {
			@Override
			public RandomVariable apply(final RandomVariable x) {
				return x.isNaN();
			}
		});
	}
}
