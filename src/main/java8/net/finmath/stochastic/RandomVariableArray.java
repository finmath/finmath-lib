/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2009
 */
package net.finmath.stochastic;

/**
 * An array of <code>RandomVariable</code> objects, implementing the <code>RandomVariable</code> interface.
 *
 * The array features a method <code>getLevel()</code> which indicates if the object is an array where elements are themselves arrays. See {@link #getLevel()}.
 *
 * All methods inherited from <code>RandomVariable</code> act element wise on the vector elements getElement(int) and return
 * corresponding RandomVariableArray having the same level.
 *
 * In addition methods are provided that reduce the level by one, like the scalar product, see {@link #sumProduct(RandomVariableArray)}.
 *
 * @author Christian Fries
 */
public interface RandomVariableArray extends RandomVariable {

	int getNumberOfElements();

	RandomVariable getElement(int index);

	/**
	 * Returns the level of the array
	 *
	 * The level of the array is given by 1 if the elements are of type <code>RandomVariable</code> but not of type <code>RandomVariableArray</code>.
	 * If the elements are of type <code>RandomVariableArray</code> the level of this array is 1 plus the level of its elements.
	 * Note: the elements are required to be of the the same level.
	 *
	 * @return The level of the array.
	 */
	default int getLevel() {

		final RandomVariable element = getElement(0);
		if(element instanceof RandomVariableArray) {
			return ((RandomVariableArray)element).getLevel() + 1;
		}
		else {
			return 1;
		}
	}

	default Object toDoubleArray() {
		if(getLevel() == 1) {
			final double[] doubleArray = new double[getNumberOfElements()];
			for(int i=0; i<getNumberOfElements(); i++) {
				doubleArray[i] = getElement(i).doubleValue();
			}
			return doubleArray;
		}
		else {
			// TODO The following code requires a consistent level on all elements
			final Object[] doubleArray = new Object[getNumberOfElements()];
			for(int i=0; i<getNumberOfElements(); i++) {
				doubleArray[i] = ((RandomVariableArray)getElement(i)).toDoubleArray();
			}
			return doubleArray;
		}
	}

	/**
	 * Component wise operation
	 *
	 * @param operator A function operator mapping random variables x &rarr; operator(x).
	 * @return An array where operator has been applied to eeach element.
	 */
	RandomVariableArray map(RandomOperator operator);

	/**
	 * Components wise product followed by sum of all elements.
	 * Reduction of getLevel by 1.
	 * Note: The return value is for sure instanceof <code>RandomVariable</code> but may be instanceof <code>RandomVariableArray</code>.
	 *
	 * @param array Given <code>RandomVariableArray</code> of the same size()
	 * @return The scalar product of this array and the given array.
	 */
	RandomVariable sumProduct(RandomVariableArray array);

	@Override
	default RandomVariableArray getConditionalExpectation(final ConditionalExpectationEstimator conditionalExpectationOperator)
	{
		return map(new RandomOperator() {
			@Override
			public RandomVariable apply(final RandomVariable x) {
				return conditionalExpectationOperator.getConditionalExpectation(x);
			}
		});
	}
}
