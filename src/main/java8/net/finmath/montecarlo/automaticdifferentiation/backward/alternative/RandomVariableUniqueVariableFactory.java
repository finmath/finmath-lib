package net.finmath.montecarlo.automaticdifferentiation.backward.alternative;

import java.util.ArrayList;

import net.finmath.montecarlo.AbstractRandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFromArrayFactory;
import net.finmath.stochastic.RandomVariable;

/**
 * @author Stefan Sedlmair
 * @version 1.0
 */
public class RandomVariableUniqueVariableFactory extends AbstractRandomVariableFactory {

	/**
	 *
	 */
	private static final long serialVersionUID = -45129698827709536L;

	/**
	 *
	 */
	private static ArrayList<RandomVariable> listOfAllVariables = new ArrayList<>();

	/* starting index for adding variables to array list. */
	private static int currentFactoryID = 0;

	/**
	 *  empty constructor: does nothing since all elements of this class are static!
	 * */
	public RandomVariableUniqueVariableFactory() {
		/* do nothing */
	}

	/*---------------------------------------------------------------------------------------------------------------------------------*/

	/**
	 * Add an object of {@link RandomVariable} to variable list at the index of the current ID
	 * and rises the current ID to the next one.
	 * @param randomvariable object of {@link RandomVariable} to add to ArrayList and return as {@link RandomVariableUniqueVariable}
	 * @param isConstant boolean such that if true the derivative will be set to zero
	 * @param parentVariables List of {@link RandomVariableUniqueVariable} that are the parents of the new instance
	 * @param parentOperatorType Operator type
	 * @return new instance of {@link RandomVariableUniqueVariable}
	 **/
	public RandomVariable createRandomVariable(final RandomVariable randomvariable, final boolean isConstant, final ArrayList<RandomVariableUniqueVariable> parentVariables, final RandomVariableUniqueVariable.OperatorType parentOperatorType) {

		final int factoryID = currentFactoryID++;

		listOfAllVariables.add(
				factoryID,
				randomvariable
				);

		return new RandomVariableUniqueVariable(factoryID, isConstant, parentVariables, parentOperatorType);
	}

	public RandomVariable createRandomVariable(final double time, final double value, final boolean isConstant) {

		final RandomVariableFromArrayFactory randomvariablefactory = new RandomVariableFromArrayFactory();
		final RandomVariable newrandomvariable = randomvariablefactory.createRandomVariable(time, value);

		return createRandomVariable(newrandomvariable, isConstant, /*parentVariables*/ null, /*parentOperatorType*/ null);
	}

	public RandomVariable createRandomVariable(final double time, final double[] values, final boolean isConstant) {

		final RandomVariableFromArrayFactory randomvariablefactory = new RandomVariableFromArrayFactory();
		final RandomVariable newrandomvariable = randomvariablefactory.createRandomVariable(time, values);

		return createRandomVariable(newrandomvariable, isConstant, /*parentVariables*/ null, /*parentOperatorType*/ null);
	}

	public RandomVariable createRandomVariable(final RandomVariable randomvariable) {
		return createRandomVariable(randomvariable, /*isConstant*/ false, /*parentVariables*/ null, /*parentOperatorType*/ null);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.AbstractRandomVariableFactory#createRandomVariable(double, double)
	 */
	@Override
	public RandomVariable createRandomVariable(final double time, final double value) {
		return createRandomVariable(time, value, /*isConstant*/ false);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.AbstractRandomVariableFactory#createRandomVariable(double, double[])
	 */
	@Override
	public RandomVariable createRandomVariable(final double time, final double[] values) {
		return createRandomVariable(time, values, /*isConstant*/ false);

	}

	/*---------------------------------------------------------------------------------------------------------------------------------*/


	/**
	 * @return ArrayList containing all Variables and Constants of the session in the JVM
	 * */
	public ArrayList<RandomVariable> getListOfAllVariables(){
		return listOfAllVariables;
	}

	/**
	 * @return Number of Entries in the List
	 * */
	public int getNumberOfEntriesInList(){
		/* starts at zero and is increased by one every time a new variable is added */
		return currentFactoryID;
	}
}
