/**
 *
 */
package net.finmath.montecarlo.automaticdifferentiation.backward.alternative;

import java.util.ArrayList;

import net.finmath.montecarlo.AbstractRandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * @author Stefan Sedlmair
 */
public class RandomVariableUniqueVariableFactory extends AbstractRandomVariableFactory {

	/**
	 *
	 */
	private static ArrayList<RandomVariableInterface> listOfAllVariables = new ArrayList<>();

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
	 * Add an object of {@link RandomVariableInterface} to variable list at the index of the current ID
	 * and rises the current ID to the next one.
	 * @param randomvariable object of {@link RandomVariableInterface} to add to ArrayList and return as {@link RandomVariableUniqueVariable}
	 * @param isConstant boolean such that if true the derivative will be set to zero
	 * @param parentVariables List of {@link RandomVariableUniqueVariable} that are the parents of the new instance
	 * @param parentOperatorType Operator type
	 * @return new instance of {@link RandomVariableUniqueVariable}
	 **/
	public RandomVariableInterface createRandomVariable(RandomVariableInterface randomvariable, boolean isConstant, ArrayList<RandomVariableUniqueVariable> parentVariables, RandomVariableUniqueVariable.OperatorType parentOperatorType) {

		int factoryID = currentFactoryID++;

		listOfAllVariables.add(
				factoryID,
				randomvariable
				);

		return new RandomVariableUniqueVariable(factoryID, isConstant, parentVariables, parentOperatorType);
	}

	public RandomVariableInterface createRandomVariable(double time, double value, boolean isConstant) {

		RandomVariableFactory randomvariablefactory = new RandomVariableFactory();
		RandomVariableInterface newrandomvariable = randomvariablefactory.createRandomVariable(time, value);

		return createRandomVariable(newrandomvariable, isConstant, /*parentVariables*/ null, /*parentOperatorType*/ null);
	}

	public RandomVariableInterface createRandomVariable(double time, double[] values, boolean isConstant) {

		RandomVariableFactory randomvariablefactory = new RandomVariableFactory();
		RandomVariableInterface newrandomvariable = randomvariablefactory.createRandomVariable(time, values);

		return createRandomVariable(newrandomvariable, isConstant, /*parentVariables*/ null, /*parentOperatorType*/ null);
	}

	public RandomVariableInterface createRandomVariable(RandomVariableInterface randomvariable) {
		return createRandomVariable(randomvariable, /*isConstant*/ false, /*parentVariables*/ null, /*parentOperatorType*/ null);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.AbstractRandomVariableFactory#createRandomVariable(double, double)
	 */
	@Override
	public RandomVariableInterface createRandomVariable(double time, double value) {
		return createRandomVariable(time, value, /*isConstant*/ false);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.AbstractRandomVariableFactory#createRandomVariable(double, double[])
	 */
	@Override
	public RandomVariableInterface createRandomVariable(double time, double[] values) {
		return createRandomVariable(time, values, /*isConstant*/ false);

	}

	/*---------------------------------------------------------------------------------------------------------------------------------*/


	/**
	 * @return ArrayList containing all Variables and Constants of the session in the JVM
	 * */
	public ArrayList<RandomVariableInterface> getListOfAllVariables(){
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
