/**
 * 
 */
package net.finmath.montecarlo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntToDoubleFunction;
import java.util.stream.DoubleStream;

import net.finmath.functions.DoubleTernaryOperator;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * @author Stefan Sedlmair
 * @date 29.05.2017
 */
public class RandomVariableUniqueVariable implements RandomVariableInterface {

	private static final long serialVersionUID = -2631868286977854016L;

	public enum OperatorType {
		ADD, MULT, DIV, SUB, SQUARED, SQRT, LOG, SIN, COS, EXP
	}
	
	private final RandomVariableUniqueVariableFactory factory = new RandomVariableUniqueVariableFactory();
	private ArrayList<RandomVariableUniqueVariable> parentsVariables;
	private OperatorType parentOperatorType; /* important for the partial derivatives */
	
	private int variableID;
	private boolean isConstant;
	
	/*---------------------------------------------------------------------------------------------------------------------------------*/
	
	/**
	 * <b>Do not use this constructor on its own.</b> It is thought only to be use by the {@link RandomVariableUniqueVariableFactory}!
	 * 
	 * @param variableID is the index of the corresponding {@link RandomVariableInterface} in the ArrayList of the {@link RandomVariableUniqueVariableFactory}
	 * @param isConstant If true, this is a constant.
	 * @param parentVariables Indices of parents
	 * @param parentOperatorType Operator
	 */
	public RandomVariableUniqueVariable(int variableID, boolean isConstant, ArrayList<RandomVariableUniqueVariable> parentVariables, OperatorType parentOperatorType) {
		this.variableID = variableID;		
		this.isConstant = isConstant;
		this.parentsVariables = parentVariables;
		this.parentOperatorType = parentOperatorType;
	}

	public RandomVariableUniqueVariable(double time, double[] values, boolean isConstant, ArrayList<RandomVariableUniqueVariable> parentVariables, OperatorType parentOperatorType){
		constructRandomVariableUniqueVariable(new RandomVariable(time, values), isConstant, parentVariables, parentOperatorType);
	}
	
	public RandomVariableUniqueVariable(RandomVariableInterface randomVariable, boolean isConstant, ArrayList<RandomVariableUniqueVariable> parentVariables, OperatorType parentOperatorType){
		constructRandomVariableUniqueVariable(randomVariable, isConstant, parentVariables, parentOperatorType);
	}
	
	public RandomVariableUniqueVariable(double time, double[] values, boolean isConstant){
		constructRandomVariableUniqueVariable(new RandomVariable(time, values), isConstant, /*parentVariables*/ null, /*parentOperatorType*/ null);
	}
	
	public RandomVariableUniqueVariable(RandomVariableInterface randomVariable, boolean isConstant){
		constructRandomVariableUniqueVariable(randomVariable, isConstant, /*parentVariables*/ null, /*parentOperatorType*/ null);
	}
	
	public RandomVariableUniqueVariable(double time, double[] values){
		constructRandomVariableUniqueVariable(new RandomVariable(time, values), /*isConstant*/ false, /*parentVariables*/ null, /*parentOperatorType*/ null);
	}
	
	public RandomVariableUniqueVariable(RandomVariableInterface randomVariable){
		constructRandomVariableUniqueVariable(randomVariable, /*isConstant*/ false, /*parentVariables*/ null, /*parentOperatorType*/ null);
	}
	
	/**
	 * Function calls {@link RandomVariableUniqueVariableFactory} to use the given {@link RandomVariable}
	 * and save it to its internal ArrayList. The index of the object will be give to the new {@link RandomVariableUniqueVariable} 
	 * object.
	 * 
	 *  @param randomVariable
	 *  @param isConstant
	 * */
	private void constructRandomVariableUniqueVariable(RandomVariableInterface randomVariable, boolean isConstant, ArrayList<RandomVariableUniqueVariable> parentVariables, OperatorType parentOperatorType){
		/* 	by calling the method in the factory it will produce a new object of RandomVariableInterface and 
	 	the new item will be stored in its factory internal array list */
		RandomVariableInterface normalrandomvariable = factory.createRandomVariable(randomVariable, isConstant, parentVariables, parentOperatorType);
		
		/* by construction this object can be up-casted to RandomVariableUniqueVariable */
		RandomVariableUniqueVariable newrandomvariableuniquevariable = (RandomVariableUniqueVariable)normalrandomvariable;
		
		/* now we have access to the internal variables of the new RandomVarialeUniqueVariable */
		this.variableID = newrandomvariableuniquevariable.getVariableID();
		this.isConstant = newrandomvariableuniquevariable.isConstant();
		this.parentsVariables = newrandomvariableuniquevariable.getParentVariables();
		this.parentOperatorType = newrandomvariableuniquevariable.getParentOperatorType();
	}

	/*---------------------------------------------------------------------------------------------------------------------------------*/

	private int[] getParentIDs(){
		
		if(parentsVariables == null) return null;
		
		int[] parentIDs = new int[parentsVariables.size()];
		
		for(int i = 0; i < parentsVariables.size(); i++){
			parentIDs[i] = parentsVariables.get(i).getVariableID();
		}
		
		/*DO NOT sort this array! This deletes the information for divisions (de-/nominator)*/
		
		return parentIDs;
	}
	
	public int getVariableID(){
		return variableID;
	}
	
	private boolean isConstant(){
		return isConstant;
	}
	
	private ArrayList<RandomVariableUniqueVariable> getParentVariables(){
		return parentsVariables;
	}
	
	private OperatorType getParentOperatorType(){
		return parentOperatorType;
	}
	
	private ArrayList<RandomVariableInterface> getListOfAllVariables(){
		return factory.getListOfAllVariables();
	}

	private ArrayList<RandomVariableInterface> getParentRandomVariables(){
		
		ArrayList<RandomVariableInterface> parentrandomvariables = new ArrayList<>();
		
		for(RandomVariableUniqueVariable parent:parentsVariables){
			parentrandomvariables.add(parent.getRandomVariable());
		}
		
		return parentrandomvariables;	
	}
	
	private RandomVariableInterface getRandomVariable(){
		return getListOfAllVariables().get(variableID);
	}
	
	public boolean isVariable(){
		return (parentsVariables == null && isConstant() == false) ? true : false;

	}
	
	/*---------------------------------------------------------------------------------------------------------------------------------*/

	
	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#equals(net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public boolean equals(RandomVariableInterface randomVariable) {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getFiltrationTime()
	 */
	@Override
	public double getFiltrationTime() {
		return getRandomVariable().getFiltrationTime();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#get(int)
	 */
	@Override
	public double get(int pathOrState) {
		return getRandomVariable().get(pathOrState);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#size()
	 */
	@Override
	public int size() {
		return getRandomVariable().size();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#isDeterministic()
	 */
	@Override
	public boolean isDeterministic() {
		return getRandomVariable().isDeterministic();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getRealizations()
	 */
	@Override
	public double[] getRealizations() {
		return getRandomVariable().getRealizations();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getRealizations(int)
	 */
	@Override
	public double[] getRealizations(int numberOfPaths) {
		return getRandomVariable().getRealizations(numberOfPaths);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getMin()
	 */
	@Override
	public double getMin() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getMax()
	 */
	@Override
	public double getMax() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getAverage()
	 */
	@Override
	public double getAverage() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getAverage(net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public double getAverage(RandomVariableInterface probabilities) {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getVariance()
	 */
	@Override
	public double getVariance() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getVariance(net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public double getVariance(RandomVariableInterface probabilities) {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getSampleVariance()
	 */
	@Override
	public double getSampleVariance() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getStandardDeviation()
	 */
	@Override
	public double getStandardDeviation() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getStandardDeviation(net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public double getStandardDeviation(RandomVariableInterface probabilities) {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getStandardError()
	 */
	@Override
	public double getStandardError() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getStandardError(net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public double getStandardError(RandomVariableInterface probabilities) {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getQuantile(double)
	 */
	@Override
	public double getQuantile(double quantile) {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getQuantile(double, net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public double getQuantile(double quantile, RandomVariableInterface probabilities) {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getQuantileExpectation(double, double)
	 */
	@Override
	public double getQuantileExpectation(double quantileStart, double quantileEnd) {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getHistogram(double[])
	 */
	@Override
	public double[] getHistogram(double[] intervalPoints) {
		return getRandomVariable().getHistogram(intervalPoints);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getHistogram(int, double)
	 */
	@Override
	public double[][] getHistogram(int numberOfPoints, double standardDeviations) {
		return getRandomVariable().getHistogram(numberOfPoints, standardDeviations);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#cache()
	 */
	@Override
	public RandomVariableInterface cache() {
		return getRandomVariable().cache();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#apply(java.util.function.DoubleUnaryOperator)
	 */

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#floor(double)
	 */
	@Override
	public RandomVariableInterface floor(double floor) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#add(double)
	 */
	@Override
	public RandomVariableInterface add(double value) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#sub(double)
	 */
	@Override
	public RandomVariableInterface sub(double value) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#mult(double)
	 */
	@Override
	public RandomVariableInterface mult(double value) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#div(double)
	 */
	@Override
	public RandomVariableInterface div(double value) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#pow(double)
	 */
	@Override
	public RandomVariableInterface pow(double exponent) {
		return null;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#squared()
	 */
	@Override
	public RandomVariableInterface squared() {
		return apply(OperatorType.SQUARED, new RandomVariableInterface[] {this});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#sqrt()
	 */
	@Override
	public RandomVariableInterface sqrt() {		
		return apply(OperatorType.SQRT, new RandomVariableInterface[] {this});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#exp()
	 */
	@Override
	public RandomVariableInterface exp() {
		return apply(OperatorType.EXP, new RandomVariableInterface[] {this});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#log()
	 */
	@Override
	public RandomVariableInterface log() {
		return apply(OperatorType.LOG, new RandomVariableInterface[] {this});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#sin()
	 */
	@Override
	public RandomVariableInterface sin() {
		return apply(OperatorType.SIN, new RandomVariableInterface[] {this});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#cos()
	 */
	@Override
	public RandomVariableInterface cos() {
		return apply(OperatorType.COS, new RandomVariableInterface[] {this});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#add(net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public RandomVariableInterface add(RandomVariableInterface randomVariable) {
		return apply(OperatorType.ADD, new RandomVariableInterface[] {this, randomVariable});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#sub(net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public RandomVariableInterface sub(RandomVariableInterface randomVariable) {
		return apply(OperatorType.SUB, new RandomVariableInterface[] {this, randomVariable});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#mult(net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public RandomVariableInterface mult(RandomVariableInterface randomVariable) {
		return apply(OperatorType.MULT, new RandomVariableInterface[] {this, randomVariable});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#div(net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public RandomVariableInterface div(RandomVariableInterface randomVariable) {
		return apply(OperatorType.DIV, new RandomVariableInterface[] {this, randomVariable});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#cap(net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public RandomVariableInterface cap(RandomVariableInterface cap) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#floor(net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public RandomVariableInterface floor(RandomVariableInterface floor) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#accrue(net.finmath.stochastic.RandomVariableInterface, double)
	 */
	@Override
	public RandomVariableInterface accrue(RandomVariableInterface rate, double periodLength) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#discount(net.finmath.stochastic.RandomVariableInterface, double)
	 */
	@Override
	public RandomVariableInterface discount(RandomVariableInterface rate, double periodLength) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#barrier(net.finmath.stochastic.RandomVariableInterface, net.finmath.stochastic.RandomVariableInterface, net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public RandomVariableInterface barrier(RandomVariableInterface trigger,
			RandomVariableInterface valueIfTriggerNonNegative, RandomVariableInterface valueIfTriggerNegative) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#barrier(net.finmath.stochastic.RandomVariableInterface, net.finmath.stochastic.RandomVariableInterface, double)
	 */
	@Override
	public RandomVariableInterface barrier(RandomVariableInterface trigger,
			RandomVariableInterface valueIfTriggerNonNegative, double valueIfTriggerNegative) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#invert()
	 */
	@Override
	public RandomVariableInterface invert() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#abs()
	 */
	@Override
	public RandomVariableInterface abs() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#addProduct(net.finmath.stochastic.RandomVariableInterface, double)
	 */
	@Override
	public RandomVariableInterface addProduct(RandomVariableInterface factor1, double factor2) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#addProduct(net.finmath.stochastic.RandomVariableInterface, net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public RandomVariableInterface addProduct(RandomVariableInterface factor1, RandomVariableInterface factor2) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#addRatio(net.finmath.stochastic.RandomVariableInterface, net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public RandomVariableInterface addRatio(RandomVariableInterface numerator, RandomVariableInterface denominator) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#subRatio(net.finmath.stochastic.RandomVariableInterface, net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public RandomVariableInterface subRatio(RandomVariableInterface numerator, RandomVariableInterface denominator) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#isNaN()
	 */
	@Override
	public RandomVariableInterface isNaN() {
		return getRandomVariable().isNaN();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getMutableCopy()
	 */
	@Override
	public RandomVariableInterface getMutableCopy() {
		return null;
	}
	
	/**
	 * Check if an object can be up-casted to {@link RandomVariableUniqueVariable}.
	 * <b>If not treat the object as an constant</b> with respect to the AAD components.
	 * 
	 * @param obj any object that should be tested.
	 * @return <i>true</i> if object can be casted to {@link RandomVariableUniqueVariable}, else <i>false</i>  
	 * */
	private boolean isUpcastableToRandomVariableUniqueVariable(Object obj){
		return (obj instanceof RandomVariableUniqueVariable);
	}
	
	/** Apply one of the possible {@link OperatorType} to an array of {@link RandomVariableInterface}s. 
	 *  If the entries in the array are not an instance of {@link RandomVariableUniqueVariable} 
	 *  generate a new {@link RandomVariableUniqueVariable} and consider them as constants.
	 * */
	private RandomVariableUniqueVariable apply(OperatorType operatortype, RandomVariableInterface[] operatorVariables){
		
		ArrayList<RandomVariableUniqueVariable> parentVariables = new ArrayList<>();
				
		for(int i = 0; i < operatorVariables.length; i++){
			/* 
			 * is variable upcastable to {@link RandomVariableUniqueVariable} ? 
			 */
			if(!isUpcastableToRandomVariableUniqueVariable(operatorVariables[i])){
				/* 
				 * if no then construct a new one and consider it constant 
				 */
				operatorVariables[i] = new RandomVariableUniqueVariable(operatorVariables[i], /*isConstant*/ true);
			}
			/* add current function variable to parentVariables of new RandomVariableUniqueVariable*/
			parentVariables.add(i, (RandomVariableUniqueVariable) operatorVariables[i]);
			
			/* get the underlying RandomVariable from the factory */
			operatorVariables[i] = parentVariables.get(i).getRandomVariable();
		}
		
		RandomVariableInterface resultrandomvariable;
		
		switch(operatortype){
			/* functions with one argument  */
			case SQUARED:
				resultrandomvariable = operatorVariables[0].squared();
				break;
			case SQRT:
				resultrandomvariable = operatorVariables[0].sqrt();
				break;
			case EXP:
				resultrandomvariable = operatorVariables[0].exp();
				break;
			case LOG:
				resultrandomvariable = operatorVariables[0].log();
				break;
			case SIN:
				resultrandomvariable = operatorVariables[0].sin();
				break;
			case COS:
				resultrandomvariable = operatorVariables[0].cos();
				break;
				
			/* functions with two arguments */
			case ADD:
				resultrandomvariable = operatorVariables[0].add(operatorVariables[1]);
				break;
			case SUB:
				resultrandomvariable = operatorVariables[0].sub(operatorVariables[1]);
				break;
			case MULT:
				resultrandomvariable = operatorVariables[0].mult(operatorVariables[1]);
				break;
			case DIV:
				resultrandomvariable = operatorVariables[0].div(operatorVariables[1]);
				break;
				
			/* if non of the above throw exception */
			default:
				throw new IllegalArgumentException("Operation not supported!\n");
		}
		
		/* create new RandomVariableUniqueVariable which is definitely NOT Constant */
		return new RandomVariableUniqueVariable(resultrandomvariable, /*isConstant*/ false, parentVariables, operatortype);
	}
	
	/**
	 * Apply the AAD algorithm to this very variable
	 * 
	 * NOTE: in this case it is indeed correct to assume that the output dimension is "one"
	 * meaning that there is only one {@link RandomVariableUniqueVariable} as an output.
	 * 
	 * @return gradient for the built up function 
	 * */
	public RandomVariableInterface[] getGradient(){
		
		// for now let us take the case for output-dimension equal to one!
		int numberOfVariables = getNumberOfVariablesInList();
		int numberOfCalculationSteps = factory.getNumberOfEntriesInList();		
		
		RandomVariableInterface[] omega_hat = new RandomVariableInterface[numberOfCalculationSteps];
		
		// first entry gets initialized 
		omega_hat[numberOfCalculationSteps-1] = new RandomVariable(1.0);
		
		/*
		 * TODO: Find way that calculations form here on are not 'recorded' by the factory
		 * IDEA: Let the calculation below run on {@link RandomVariable}, ie cast everything down!
		 * */
				
		for(int functionIndex = numberOfCalculationSteps - 2; functionIndex > 0; functionIndex--){
			// apply chain rule
			omega_hat[functionIndex] = new RandomVariable(0.0);
			
			/*TODO: save all D_{i,j}*\omega_j in vector and sum up later */
			for(RandomVariableUniqueVariable parent:parentsVariables){
				
				int variableIndex = parent.getVariableID();
				
				omega_hat[functionIndex] = omega_hat[functionIndex].add(getPartialDerivative(functionIndex, variableIndex).mult(omega_hat[variableIndex]));
			}
		}
		
		/* Due to the fact that we can still introduce 'new' true variables on the fly they are NOT the last couple of indices!
		 * Thus save the indices of the true variables and recover them after finalizing all the calculations
		 * IDEA: quit calculation after minimal true variable index is reached */
		RandomVariableInterface[] gradient = new RandomVariableInterface[numberOfVariables];
		
		/* TODO: sort array in correct manner! */
		int[] indicesOfVariables = getIDsOfVariablesInList();
		
		for(int i = 0; i < numberOfVariables; i++){
			gradient[i] = omega_hat[numberOfCalculationSteps - numberOfVariables + indicesOfVariables[i]];
		}

		return gradient;
	}
	
	private ArrayList<RandomVariableUniqueVariable> getListOfDependingTrueVariables(){
				
		ArrayList<RandomVariableUniqueVariable> listOfDependingTrueVariables = new ArrayList<>();
		
		for(RandomVariableUniqueVariable parent:parentsVariables){
			if(parent.isVariable() && !listOfDependingTrueVariables.contains(parent)){
				listOfDependingTrueVariables.add(parent);
			} else if (parent.getParentIDs() != null){
				listOfDependingTrueVariables.addAll(parent.getListOfDependingTrueVariables());
			}
		}
		
		return listOfDependingTrueVariables;
	}
	
	private int[] getIDsOfVariablesInList() {
		int[] IDsOfVariablesInList = new int[getNumberOfVariablesInList()];
		
		ArrayList<RandomVariableUniqueVariable> listOfDependingTrueVariables = getListOfDependingTrueVariables();
		
		for(RandomVariableUniqueVariable variable:listOfDependingTrueVariables){
			IDsOfVariablesInList[listOfDependingTrueVariables.indexOf(variable)] = variable.getVariableID();
		}
		
		return IDsOfVariablesInList;
	}

	private int getNumberOfVariablesInList() {
		return getListOfDependingTrueVariables().size();
	}

	/**
	 * @param functionIndex
	 * @param variableIndex
	 * @return
	 */
	private RandomVariableInterface getPartialDerivative(int functionIndex, int variableIndex) {
		
		if(!Arrays.asList(getParentIDs()).contains(variableIndex)) return new RandomVariable(0.0);
		
		RandomVariableUniqueVariable currentRandomVariable = (RandomVariableUniqueVariable) getListOfAllVariables().get(functionIndex);
		ArrayList<RandomVariableInterface> currentParentRandomVaribles = currentRandomVariable.getParentRandomVariables();
		
		RandomVariableInterface resultrandomvariable;
		
		switch(currentRandomVariable.getParentOperatorType()){
		/* functions with one argument  */
		case SQUARED:
			resultrandomvariable = currentParentRandomVaribles.get(0).mult(2.0);
			break;
		case SQRT:
			resultrandomvariable = currentParentRandomVaribles.get(0).sqrt().invert().mult(0.5);
			break;
		case EXP:
			resultrandomvariable = currentParentRandomVaribles.get(0).exp();
			break;
		case LOG:
			resultrandomvariable = currentParentRandomVaribles.get(0).invert();
			break;
		case SIN:
			resultrandomvariable = currentParentRandomVaribles.get(0).cos();
			break;
		case COS:
			resultrandomvariable = currentParentRandomVaribles.get(0).sin().mult(-1.0);
			break;
			
		/* functions with two arguments */
		case ADD:
			resultrandomvariable = new RandomVariable(1.0);
			break;
		case SUB:
			resultrandomvariable = new RandomVariable(1.0);
			if(variableIndex == currentRandomVariable.getParentIDs()[1]){
				resultrandomvariable = resultrandomvariable.mult(-1.0);
			}
			break;
		case MULT:
			if(variableIndex == currentRandomVariable.getParentIDs()[0]){
				resultrandomvariable = currentParentRandomVaribles.get(1);
			} else {
				resultrandomvariable = currentParentRandomVaribles.get(0);
			}
			break;
		case DIV:
			if(variableIndex == currentRandomVariable.getParentIDs()[0]){
				resultrandomvariable = currentParentRandomVaribles.get(1).invert();
			} else {
				resultrandomvariable = currentParentRandomVaribles.get(0).div(currentParentRandomVaribles.get(1).squared());
			}
			break;
			
		/* if non of the above throw exception */
		default:
			throw new IllegalArgumentException("Operation not supported!\n");
		}
		
		return resultrandomvariable;
	}

	public String toString(){
		return super.toString() + "\n" + 
				"time: " + getFiltrationTime() + "\n" + 
				"realizations: " + Arrays.toString(getRealizations()) + "\n" + 
				"variableID: " + variableID + "\n" +
				"parentIDs: " + Arrays.toString(getParentIDs()) + ((getParentIDs() == null) ? "" : (" type: " + parentOperatorType.name())) + "\n" +
				"isTrueVariable: " + isVariable() + "";
	}

	@Override
	public RandomVariableInterface cap(double cap) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IntToDoubleFunction getOperator() {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public DoubleStream getRealizationsStream() {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public RandomVariableInterface apply(DoubleUnaryOperator operator) {
		throw new UnsupportedOperationException("Applying functions is not supported.");
	}

	@Override
	public RandomVariableInterface apply(DoubleBinaryOperator operator, RandomVariableInterface argument) {
		throw new UnsupportedOperationException("Applying functions is not supported.");
	}

	@Override
	public RandomVariableInterface apply(DoubleTernaryOperator operator, RandomVariableInterface argument1, RandomVariableInterface argument2) {
		throw new UnsupportedOperationException("Applying functions is not supported.");
	}
}
