/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2009
 */
package net.finmath.stochastic;

import java.util.function.Function;

/**
 * An array of <code>RandomVariableInterface</code> objects, implementing the <code>RandomVariableInterface</code> interface.
 *
 * The array features a method <code>getLevel()</code> which indicates if the object is an array where elements are themselves arrays. See {@link #getLevel()}.
 *
 * All methods inherited from <code>RandomVariableInterface</code> act element wise on the vector elements getElement(int) and return
 * corresponding RandomVariableArray having the same level.
 *
 * In addition methods are provided that reduce the level by one, like the scalar product, see {@link #sumProduct(RandomVariableArray)}.
 *
 * @author Christian Fries
 */
public interface RandomVariableArray extends RandomVariableInterface {

	RandomVariableArray of(RandomVariableInterface[] elements);

	int getNumberOfElements();

	RandomVariableInterface getElement(int index);

	/**
	 * Returns the level of the array
	 *
	 * The level of the array is given by 1 if the elements are of type <code>RandomVariableInterface</code> but not of type <code>RandomVariableArray</code>.
	 * If the elements are of type <code>RandomVariableArray</code> the level of this array is 1 plus the level of its elements.
	 * Note: the elements are required to be of the the same level.
	 *
	 * @return The level of the array.
	 */
	default int getLevel() {

		RandomVariableInterface element = getElement(0);
		if(element instanceof RandomVariableArray) {
			return ((RandomVariableArray)element).getLevel() + 1;
		}
		else {
			return 1;
		}
	}

	/**
	 * Component wise operation
	 */
	RandomVariableArray map(Function<RandomVariableInterface, RandomVariableInterface> operator);

	/**
	 * Components wise product followed by sum of all elements.
	 * Reduction of getLevel by 1.
	 * Note: The return value is for sure instanceof <code>RandomVariableInterface</code> but may be instanceof <code>RandomVariableArray</code>.
	 *
	 * @param array Given <code>RandomVariableArray</code> of the same size()
	 * @return The scalar product of this array and the given array.
	 */
	RandomVariableInterface sumProduct(RandomVariableArray array);
}
