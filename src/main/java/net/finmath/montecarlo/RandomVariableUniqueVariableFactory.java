/**
 * 
 */
package net.finmath.montecarlo;

import java.util.ArrayList;

import net.finmath.stochastic.RandomVariableInterface;

/**
 * @author Stefan Sedlmair
 * @date 29.05.2017
 *
 */
public class RandomVariableUniqueVariableFactory extends AbstractRandomVariableFactory {

	/**
	 * 
	 */
	static private ArrayList<RandomVariableInterface> listOfAllVariables = new ArrayList<>();
	
	/* starting index for adding variables to array list. */
	static private int currentFactoryID = 0;
	
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
	 *  @param randomvariable object of {@link RandomVariableInterface} to add to ArrayList and return as {@link RandomVariableUniqueVariable}
	 *  @param isConstant boolean such that if true the derivative will be set to zero
	 *  @param parentVariables List of {@link RandomVariableUniqueVariable} that are the parents of the new instance
	 *  @return new instance of {@link RandomVariableUniqueVariable}
	 * */
	public RandomVariableInterface createRandomVariable(RandomVariableInterface randomvariable, boolean isConstant, ArrayList<RandomVariableUniqueVariable> parentVariables) {
		
		int factoryID = currentFactoryID++;
		
		listOfAllVariables.add(
				factoryID,
				randomvariable
				);
		
		return new RandomVariableUniqueVariable(factoryID, isConstant, parentVariables);
	}
	
	public RandomVariableInterface createRandomVariable(double time, double value, boolean isConstant) {
		
		RandomVariableFactory randomvariablefactory = new RandomVariableFactory();
		RandomVariableInterface newrandomvariable = randomvariablefactory.createRandomVariable(time, value);

		return createRandomVariable(newrandomvariable, isConstant, /*parentVariables*/ null);
	}

	public RandomVariableInterface createRandomVariable(double time, double[] values, boolean isConstant) {

		RandomVariableFactory randomvariablefactory = new RandomVariableFactory();
		RandomVariableInterface newrandomvariable = randomvariablefactory.createRandomVariable(time, values);
	
		return createRandomVariable(newrandomvariable, isConstant, /*parentVariables*/ null);
	}

	public RandomVariableInterface createRandomVariable(RandomVariableInterface randomvariable) {
		return createRandomVariable(randomvariable, /*isConstant*/ false, /*parentVariables*/ null);
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
	 * Number of Variables not constant and without parents is the number of Variables in the Session
	 * @return Number of Variables in the internal ArrayList
	 * */
	public int getNumberOfVariablesInList(){
		
		int numberOfVariablesInList = 0;
		
		for(RandomVariableInterface entry:listOfAllVariables){
			if(((RandomVariableUniqueVariable) entry).isVariable()) numberOfVariablesInList++;
		}
		
		return numberOfVariablesInList;
	}
	
	public int[] getIDsOfVariablesInList(){
		/*TODO: nicer implementation needed! */
		int[] variableIDs = new int[getNumberOfVariablesInList()];
		
		int i = 0;
		
		for(RandomVariableInterface entry:listOfAllVariables){
			if(((RandomVariableUniqueVariable) entry).isVariable())	variableIDs[i++] = ((RandomVariableUniqueVariable) entry).getVariableID();
		}
		
		return variableIDs;
		
	}
	
	/**
	 * @return Number of Entries in the List
	 * */
	public int getNumberOfEntriesInList(){
		/* starts at zero and is increased by one every time a new variable is added */
		return currentFactoryID;
	}
}
