/**
 * 
 */
package net.finmath.montecarlo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.analysis.UnivariateFunction;

import net.finmath.stochastic.RandomVariableInterface;

/**
 * {@link RandomVariableInterface} having the feature to calculate the backward algorithmic differentiation.
 * 
 * For this class has a static {@link ArrayList} saving all instances of the class that are produced over time.
 * Hence the class does not have a {@link public} constructor! 
 * 
 * In order to initialize a instance of this class, please use the {@link static} method {@link RandomVariableAAD.constructNewAADRandomVariable}!
 * 
 * @author Stefan Sedlmair
 * @version 1.0
 */
public class RandomVariableAAD implements RandomVariableInterface {

	private static final long serialVersionUID = 2459373647785530657L;

	/* static elements of the class are shared between all members */
	private static ArrayList<RandomVariableAAD> arrayListOfAllAADRandomVariables = new ArrayList<>();
	private static int indexOfNextRandomVariable = 0;
	private static enum OperatorType {
		ADD, MULT, DIV, SUB, SQUARED, SQRT, LOG, SIN, COS, EXP, INVERT, CAP, FLOOR, ABS, 
		ADDPRODUCT, ADDRATIO, SUBRATIO, BARRIER, DISCOUNT, ACCURUE, POW, AVERAGE, VARIANCE, STDEV, MIN, MAX
	}
	
	/* index of corresponding random variable in the static array list*/
	private final RandomVariableInterface ownRandomVariable;
	private final int ownIndexInList;
	
	/* this could maybe be outsourced to own class ParentElement */
	private final int[] parentIndices;
	private final OperatorType parentOperator;
	private boolean isConstant;
		
	/**
	 * @param indexOfRandomVariable
	 * @param parentRandomVariables
	 * @param parentOperator
	 * @param isConstant
	 */
	private RandomVariableAAD(int ownIndexInList, RandomVariableInterface ownRandomVariable, 
			int[] parentIndices, OperatorType parentOperator, boolean isConstant) {
		super();
		this.ownIndexInList = ownIndexInList;
		this.ownRandomVariable = ownRandomVariable;
		this.parentIndices = parentIndices;
		this.parentOperator = parentOperator;
		this.isConstant = isConstant;
	}
	
	public static void resetArrayListOfAllAADRandomVariables(){
		arrayListOfAllAADRandomVariables = new ArrayList<>();
		indexOfNextRandomVariable = 0;
	}
	
	public void setIsConstantTo(boolean isConstant){
		this.isConstant = isConstant;
	}
	
	private RandomVariableInterface getRandomVariableInterface(){
		return ownRandomVariable;
	}
	
	private RandomVariableInterface getRandomVariableInterfaceOfIndex(int index){
		return getFunctionList().get(index).getRandomVariableInterface();
	}
	
	private int getFunctionIndex(){
		return ownIndexInList;
	}
	
	private int[] getParentIDs(){
		return parentIndices;
	}
	
	private int getNumberOfParentVariables(){
		if(getParentIDs() == null) return 0;
		return getParentIDs().length;
	}
	
	private RandomVariableAAD getAADRandomVariableFromList(int index){
		return getFunctionList().get(index);
	}
	
	private RandomVariableAAD[] getParentAADRandomVariables(){
		
		if(getParentIDs() == null) return null;
		
		int[] parentIndices = getParentIDs();
		RandomVariableAAD[] parentAADRandomVariables = new RandomVariableAAD[getNumberOfParentVariables()];
		
		for(int i=0; i < parentAADRandomVariables.length; i++){
			parentAADRandomVariables[i] = getAADRandomVariableFromList(parentIndices[i]);
		}
		
		return parentAADRandomVariables;
	}
	
	/**
	 * @return
	 */
	private RandomVariableInterface[] getParentRandomVariableInderfaces(){
		
		RandomVariableAAD[] parentAADRandomVariables = getParentAADRandomVariables();
		RandomVariableInterface[] parentRandomVariableInderfaces = new RandomVariableInterface[parentAADRandomVariables.length];
		
		for(int i=0;i<parentAADRandomVariables.length;i++){
			parentRandomVariableInderfaces[i] = parentAADRandomVariables[i].getRandomVariableInterface();
		}
		
		return parentRandomVariableInderfaces;
	}
	
	private OperatorType getParentOperator(){
		return parentOperator;
	}
	
	private boolean isConstant(){
		return isConstant;
	}
	
	private boolean isVariable() {
		return (isConstant() == false && getParentIDs() == null);
	}	
	
	private ArrayList<RandomVariableAAD> getFunctionList(){
		return arrayListOfAllAADRandomVariables;
	}
	
	/**
	 * @param randomVariable
	 * @param parentIndices
	 * @param parentOperator
	 * @param isConstant
	 * @return
	 */
	public static RandomVariableAAD constructNewAADRandomVariable(RandomVariableInterface randomVariable, int[] parentIndices,
			OperatorType parentOperator, boolean isConstant){
		
		/* TODO: how to handle cases with different realization lengths? */
		if(!arrayListOfAllAADRandomVariables.isEmpty()){
			if(arrayListOfAllAADRandomVariables.get(0).size() != randomVariable.size() && !randomVariable.isDeterministic()){
				throw new IllegalArgumentException("RandomVariables with different sizes are not supported at the moment!");
			}
		}
		
		/* get index of this random variable */
		int indexOfThisAADRandomVariable = indexOfNextRandomVariable++;
		
		RandomVariableAAD newAADRandomVariable = new RandomVariableAAD(indexOfThisAADRandomVariable, randomVariable, 
				parentIndices, parentOperator, isConstant);
		
		/* add random variable to static list for book keeping */
		arrayListOfAllAADRandomVariables.add(indexOfThisAADRandomVariable, newAADRandomVariable);
		
		/* return a new random variable */
		return newAADRandomVariable;
	}
	
	public static RandomVariableAAD constructNewAADRandomVariable(double value){
		return constructNewAADRandomVariable(new RandomVariable(value), /*parentRandomVariables*/ null, /*parentOperator*/ null, /*isConstant*/ true);
	}
	
	
	public static RandomVariableAAD constructNewAADRandomVariable(RandomVariableInterface randomVariable) {
		return constructNewAADRandomVariable(randomVariable, /* no parents*/ null,
				/*no parent operator*/ null, /*not constant*/ false);
	}

	public static RandomVariableAAD constructNewAADRandomVariable(double time, double[] realisations) {
		return constructNewAADRandomVariable(new RandomVariable(time, realisations), /* no parents*/ null,
				/*no parent operator*/ null, /*not constant*/ false);
	}
	
	private RandomVariableInterface apply(OperatorType operator, RandomVariableInterface[] randomVariableInterfaces){
		
		RandomVariableAAD[] aadRandomVariables = new RandomVariableAAD[randomVariableInterfaces.length];
		int[] futureParentIndices = new int[aadRandomVariables.length];
		
		/* TODO: implement with Array and .foreach(...) */
		for(int randomVariableIndex = 0; randomVariableIndex < randomVariableInterfaces.length; randomVariableIndex++){
			
			aadRandomVariables[randomVariableIndex] = (randomVariableInterfaces[randomVariableIndex] instanceof RandomVariableAAD) ?
					(RandomVariableAAD)randomVariableInterfaces[randomVariableIndex] : constructNewAADRandomVariable(randomVariableInterfaces[randomVariableIndex]);
					
			futureParentIndices[randomVariableIndex] = aadRandomVariables[randomVariableIndex].getFunctionIndex();
		}
		
		RandomVariableInterface resultrandomvariable;
		
		switch(operator){
			/* functions with one argument  */
			case SQUARED:
				if(randomVariableInterfaces.length != 1) throw new IllegalArgumentException();
				resultrandomvariable = aadRandomVariables[0].getRandomVariableInterface().squared();
				break;
			case SQRT:
				if(randomVariableInterfaces.length != 1) throw new IllegalArgumentException();
				resultrandomvariable = aadRandomVariables[0].getRandomVariableInterface().sqrt();
				break;
			case EXP:
				if(randomVariableInterfaces.length != 1) throw new IllegalArgumentException();
				resultrandomvariable = aadRandomVariables[0].getRandomVariableInterface().exp();
				break;
			case LOG:
				if(randomVariableInterfaces.length != 1) throw new IllegalArgumentException();
				resultrandomvariable = aadRandomVariables[0].getRandomVariableInterface().log();
				break;
			case SIN:
				if(randomVariableInterfaces.length != 1) throw new IllegalArgumentException();
				resultrandomvariable = aadRandomVariables[0].getRandomVariableInterface().sin();
				break;
			case COS:
				if(randomVariableInterfaces.length != 1) throw new IllegalArgumentException();
				resultrandomvariable = aadRandomVariables[0].getRandomVariableInterface().cos();
				break;
			case ABS:
				if(randomVariableInterfaces.length != 1) throw new IllegalArgumentException();
				resultrandomvariable = aadRandomVariables[0].getRandomVariableInterface().abs();
				break;
			case INVERT:
				if(randomVariableInterfaces.length != 1) throw new IllegalArgumentException();
				resultrandomvariable = aadRandomVariables[0].getRandomVariableInterface().invert();
				break;
			case AVERAGE:
				if(randomVariableInterfaces.length != 1) throw new IllegalArgumentException();
				/* return a new deterministic random variable  */
				resultrandomvariable = new RandomVariable(aadRandomVariables[0].getRandomVariableInterface().getAverage());
				break;
			case VARIANCE:
				if(randomVariableInterfaces.length != 1) throw new IllegalArgumentException();
				/* return a new deterministic random variable  */
				resultrandomvariable = new RandomVariable(aadRandomVariables[0].getRandomVariableInterface().getVariance());
				break;
			case STDEV:
				if(randomVariableInterfaces.length != 1) throw new IllegalArgumentException();
				/* return a new deterministic random variable  */
				resultrandomvariable = new RandomVariable(aadRandomVariables[0].getRandomVariableInterface().getStandardDeviation());
				break;
				
			/* functions with two arguments */
			case ADD:
				if(randomVariableInterfaces.length != 2) throw new IllegalArgumentException();
				resultrandomvariable = aadRandomVariables[0].getRandomVariableInterface().add(aadRandomVariables[1].getRandomVariableInterface());
				break;
			case SUB:
				if(randomVariableInterfaces.length != 2) throw new IllegalArgumentException();
				resultrandomvariable = aadRandomVariables[0].getRandomVariableInterface().sub(aadRandomVariables[1].getRandomVariableInterface());
				break;
			case MULT:
				if(randomVariableInterfaces.length != 2) throw new IllegalArgumentException();
				resultrandomvariable = aadRandomVariables[0].getRandomVariableInterface().mult(aadRandomVariables[1].getRandomVariableInterface());
				break;
			case DIV:
				if(randomVariableInterfaces.length != 2) throw new IllegalArgumentException();
				resultrandomvariable = aadRandomVariables[0].getRandomVariableInterface().div(aadRandomVariables[1].getRandomVariableInterface());
				break;
			case CAP:
				if(randomVariableInterfaces.length != 2) throw new IllegalArgumentException();
				resultrandomvariable = aadRandomVariables[0].getRandomVariableInterface().cap(aadRandomVariables[1].getRandomVariableInterface());
				break;
			case FLOOR:
				if(randomVariableInterfaces.length != 2) throw new IllegalArgumentException();
				resultrandomvariable = aadRandomVariables[0].getRandomVariableInterface().floor(aadRandomVariables[1].getRandomVariableInterface());
				break;			
			case POW:
				if(randomVariableInterfaces.length != 2) throw new IllegalArgumentException();
				resultrandomvariable = aadRandomVariables[0].getRandomVariableInterface().pow(
						/* argument is deterministic anyway */ aadRandomVariables[1].getRandomVariableInterface().getAverage());
				break;
				
			/* functions with three arguments */	
			case ADDPRODUCT:
				if(randomVariableInterfaces.length != 3) throw new IllegalArgumentException();
				resultrandomvariable = aadRandomVariables[0].getRandomVariableInterface().addProduct(aadRandomVariables[1].getRandomVariableInterface(), 
						aadRandomVariables[2].getRandomVariableInterface());
				break;
			case ADDRATIO:
				if(randomVariableInterfaces.length != 3) throw new IllegalArgumentException();
				resultrandomvariable = aadRandomVariables[0].getRandomVariableInterface().addRatio(aadRandomVariables[1].getRandomVariableInterface(), 
						aadRandomVariables[2].getRandomVariableInterface());
				break;
			case SUBRATIO:
				if(randomVariableInterfaces.length != 3) throw new IllegalArgumentException();
				resultrandomvariable = aadRandomVariables[0].getRandomVariableInterface().subRatio(aadRandomVariables[1].getRandomVariableInterface(), 
						aadRandomVariables[2].getRandomVariableInterface());
				break;
			case ACCURUE:
				if(randomVariableInterfaces.length != 3) throw new IllegalArgumentException();
				resultrandomvariable = aadRandomVariables[0].getRandomVariableInterface().accrue(aadRandomVariables[1].getRandomVariableInterface(), 
						/* second argument is deterministic anyway */ aadRandomVariables[2].getRandomVariableInterface().getAverage());
				break;
			case DISCOUNT:
				if(randomVariableInterfaces.length != 3) throw new IllegalArgumentException();
				resultrandomvariable = aadRandomVariables[0].getRandomVariableInterface().discount(aadRandomVariables[1].getRandomVariableInterface(), 
						/* second argument is deterministic anyway */ aadRandomVariables[2].getRandomVariableInterface().getAverage());
				break;
				
			/* if non of the above throw exception */
			default:
				throw new IllegalArgumentException("Operation not supported!\n");
		}
		
		/* create new RandomVariableUniqueVariable which is definitely NOT Constant */
		return constructNewAADRandomVariable(resultrandomvariable, futureParentIndices, operator, /*not constant*/ false);
	}
	
	public String toString(){
		return  super.toString() + "\n" + 
				"time: " + getFiltrationTime() + "\n" + 
				"realizations: " + Arrays.toString(getRealizations()) + "\n" + 
				"variableID: " + getFunctionIndex() + "\n" +
				"parentIDs: " + Arrays.toString(getParentIDs()) + ((getParentIDs() == null) ? "" : (" type: " + parentOperator.name())) + "\n" +
				"isTrueVariable: " + isVariable() + "\n";
	}

	private RandomVariableInterface getPartialDerivative(int functionIndex, int variableIndex){
		return getFunctionList().get(functionIndex).partialDerivativeWithRespectTo(variableIndex);		
	}
	
	private RandomVariableInterface partialDerivativeWithRespectTo(int variableIndex){
		
		/* parentIDsSorted needs to be sorted for binarySearch! */
		int[] parentIDsSorted = (getParentIDs() == null) ? new int[]{} : getParentIDs().clone();
		Arrays.sort(parentIDsSorted);
		
		/* if random variable not dependent on variable or it is constant anyway return 0.0 */
		if((Arrays.binarySearch(parentIDsSorted, variableIndex) < 0) || isConstant) return new RandomVariable(0.0);
		
		RandomVariableInterface resultrandomvariable;
		
		switch(parentOperator){
		/* functions with one argument  */
		case SQUARED:
			resultrandomvariable = getRandomVariableInterfaceOfIndex(getParentIDs()[0]).mult(2.0);
			break;
		case SQRT:
			resultrandomvariable = getRandomVariableInterfaceOfIndex(getParentIDs()[0]).sqrt().invert().mult(0.5);
			break;
		case EXP:
			resultrandomvariable = getRandomVariableInterfaceOfIndex(getParentIDs()[0]).exp();
			break;
		case LOG:
			resultrandomvariable = getRandomVariableInterfaceOfIndex(getParentIDs()[0]).invert();
			break;
		case SIN:
			resultrandomvariable = getRandomVariableInterfaceOfIndex(getParentIDs()[0]).cos();
			break;
		case COS:
			resultrandomvariable = getRandomVariableInterfaceOfIndex(getParentIDs()[0]).sin().mult(-1.0);
			break;
		case AVERAGE:
			resultrandomvariable = new RandomVariable(getRandomVariableInterfaceOfIndex(getParentIDs()[0]).size()).invert();
			break;
		case VARIANCE:
			resultrandomvariable = getRandomVariableInterfaceOfIndex(getParentIDs()[0]);
			resultrandomvariable = resultrandomvariable.sub(resultrandomvariable.getAverage()*(2.0*resultrandomvariable.size()-1.0)/resultrandomvariable.size()).mult(2.0/resultrandomvariable.size());
			break;
		case STDEV:
			resultrandomvariable = getRandomVariableInterfaceOfIndex(getParentIDs()[0]);
			resultrandomvariable = resultrandomvariable.sub(resultrandomvariable.getAverage()*(2.0*resultrandomvariable.size()-1.0)/resultrandomvariable.size()).mult(2.0/resultrandomvariable.size()).mult(0.5).div(Math.sqrt(resultrandomvariable.getVariance()));
			break;
			
			
		/* functions with two arguments */
		case ADD:
			resultrandomvariable = new RandomVariable(1.0);
			break;
		case SUB:
			resultrandomvariable = new RandomVariable(1.0);
			if(variableIndex == getParentIDs()[1]){
				resultrandomvariable = resultrandomvariable.mult(-1.0);
			}
			break;
		case MULT:
			if(variableIndex == getParentIDs()[0]){
				resultrandomvariable = getRandomVariableInterfaceOfIndex(getParentIDs()[1]);
			} else {
				resultrandomvariable = getRandomVariableInterfaceOfIndex(getParentIDs()[0]);
			}
			break;
		case DIV:
			if(variableIndex == getParentIDs()[0]){
				resultrandomvariable = getRandomVariableInterfaceOfIndex(getParentIDs()[1]).invert();
			} else {
				resultrandomvariable = getRandomVariableInterfaceOfIndex(getParentIDs()[0]).div(getRandomVariableInterfaceOfIndex(getParentIDs()[1]).squared());
			}
			break;
			
		/* if non of the above throw exception */
		default:
			throw new IllegalArgumentException("Operation not supported!\n");
		}
		
		return resultrandomvariable;
	}
	
	/**
	 * Implements the AAD Algorithm
	 * @return HashMap where the key is the internal index of the random variable with respect to which the partial derivative was computed. This key then gives access to the actual derivative.
	 * */
	public Map<Integer, RandomVariableInterface> getGradient(){
		
		int numberOfCalculationSteps = getFunctionList().size();
		
		RandomVariableInterface[] omegaHat = new RandomVariableInterface[numberOfCalculationSteps];
		
		omegaHat[numberOfCalculationSteps-1] = new RandomVariable(1.0);
		
		for(int variableIndex = numberOfCalculationSteps-2; variableIndex >= 0; variableIndex--){
			
			omegaHat[variableIndex] = new RandomVariable(0.0);
			
			/* would be useful to know which entries are zero before hand and let the loop only run over the non-zero entries*/
			for(int functionIndex = variableIndex+1; functionIndex < numberOfCalculationSteps; functionIndex++){
				RandomVariableInterface D_i_j = getPartialDerivative(functionIndex, variableIndex);
				omegaHat[variableIndex] = omegaHat[variableIndex].addProduct(D_i_j, omegaHat[functionIndex]);
			}
		}
		
		ArrayList<Integer> arrayListOfAllIndicesOfDependentRandomVariables = getArrayListOfAllIndicesOfDependentRandomVariables();
		
		Map<Integer, RandomVariableInterface> gradient = new HashMap<Integer, RandomVariableInterface>();
		
		for(Integer indexOfDependentRandomVariable: arrayListOfAllIndicesOfDependentRandomVariables){
			gradient.put(indexOfDependentRandomVariable, omegaHat[arrayListOfAllIndicesOfDependentRandomVariables.get(indexOfDependentRandomVariable)]);
		};
		
		return gradient;
	}
	
	private ArrayList<Integer> getArrayListOfAllIndicesOfDependentRandomVariables(){
		
		ArrayList<Integer> arrayListOfAllIndicesOfDependentRandomVariables = new ArrayList<>();
		
		for(int index = 0; index < getNumberOfParentVariables(); index++){
			
			int currentParentIndex = getParentIDs()[index];
			
			/* if current index belongs to a true variable and is not yet in the list: add it*/
			if(getAADRandomVariableFromList(currentParentIndex).isVariable() && 
					!arrayListOfAllIndicesOfDependentRandomVariables.contains((Integer)currentParentIndex)){
				arrayListOfAllIndicesOfDependentRandomVariables.add((Integer)currentParentIndex);
			} else {
				arrayListOfAllIndicesOfDependentRandomVariables.addAll(
						getAADRandomVariableFromList(currentParentIndex).getArrayListOfAllIndicesOfDependentRandomVariables());
			}
		}
		
		return arrayListOfAllIndicesOfDependentRandomVariables;
	}
	
	/* TODO: for all functions that need to be differentiated and are returned as double in the Interface write a method to return it as RandomVariableAAD 
	 * that is deterministic by its nature. For their double-returning pendant just return the average of the deterministic RandomVariableAAD  */
	
	public RandomVariableInterface getAverageAsRandomVariableAAD(){
		/*returns deterministic AAD random variable */
		return apply(OperatorType.AVERAGE, new RandomVariableInterface[]{this});
	}
	
	public RandomVariableInterface getVarianceAsRandomVariableAAD() {
		/*returns deterministic AAD random variable */
		return apply(OperatorType.VARIANCE, new RandomVariableInterface[]{this});
	}

	public RandomVariableInterface 	getStandardDeviationAsRandomVariableAAD() {
		/*returns deterministic AAD random variable */
		return apply(OperatorType.STDEV, new RandomVariableInterface[]{this});
	}
	
	public RandomVariableInterface 	getMinAsRandomVariableAAD() {
		/*returns deterministic AAD random variable */
		return apply(OperatorType.MIN, new RandomVariableInterface[]{this});
	}
	
	public RandomVariableInterface 	getMaxAsRandomVariableAAD() {
		/*returns deterministic AAD random variable */
		return apply(OperatorType.MAX, new RandomVariableInterface[]{this});
	}
	/*--------------------------------------------------------------------------------------------------------------------------------------------------*/
	
	

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
		return getRandomVariableInterface().getFiltrationTime();
	}



	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#get(int)
	 */
	@Override
	public double get(int pathOrState) {
		return getRandomVariableInterface().get(pathOrState);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#size()
	 */
	@Override
	public int size() {
		return getRandomVariableInterface().size();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#isDeterministic()
	 */
	@Override
	public boolean isDeterministic() {
		return getRandomVariableInterface().isDeterministic();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getRealizations()
	 */
	@Override
	public double[] getRealizations() {
		return getRandomVariableInterface().getRealizations();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getRealizations(int)
	 */
	@Override
	public double[] getRealizations(int numberOfPaths) {
		return getRandomVariableInterface().getRealizations(numberOfPaths);
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
		/* for AVERAGE apply returns a deterministic value anyway! Hence take .getAverage to access this value */
		return ((RandomVariableAAD) getAverageAsRandomVariableAAD()).getRandomVariableInterface().getAverage();
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
		return ((RandomVariableAAD) getVarianceAsRandomVariableAAD()).getRandomVariableInterface().getAverage();
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
		return ((RandomVariableAAD) getStandardDeviationAsRandomVariableAAD()).getRandomVariableInterface().getAverage();
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
		return getRandomVariableInterface().getHistogram(intervalPoints);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getHistogram(int, double)
	 */
	@Override
	public double[][] getHistogram(int numberOfPoints, double standardDeviations) {
		return getRandomVariableInterface().getHistogram(numberOfPoints, standardDeviations);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#cache()
	 */
	@Override
	public RandomVariableInterface cache() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RandomVariableInterface cap(double cap) {
		return apply(OperatorType.CAP, new RandomVariableInterface[]{this, constructNewAADRandomVariable(cap)});
	}
	
	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#floor(double)
	 */
	@Override
	public RandomVariableInterface floor(double floor) {
		return apply(OperatorType.FLOOR, new RandomVariableInterface[]{this, constructNewAADRandomVariable(floor)});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#add(double)
	 */
	@Override
	public RandomVariableInterface add(double value) {
		return apply(OperatorType.ADD, new RandomVariableInterface[]{this, constructNewAADRandomVariable(value)});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#sub(double)
	 */
	@Override
	public RandomVariableInterface sub(double value) {
		return apply(OperatorType.SUB, new RandomVariableInterface[]{this, constructNewAADRandomVariable(value)});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#mult(double)
	 */
	@Override
	public RandomVariableInterface mult(double value) {
		return apply(OperatorType.MULT, new RandomVariableInterface[]{this, constructNewAADRandomVariable(value)});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#div(double)
	 */
	@Override
	public RandomVariableInterface div(double value) {
		return apply(OperatorType.DIV, new RandomVariableInterface[]{this, constructNewAADRandomVariable(value)});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#pow(double)
	 */
	@Override
	public RandomVariableInterface pow(double exponent) {
		return apply(OperatorType.POW, new RandomVariableInterface[]{this, constructNewAADRandomVariable(exponent)});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#squared()
	 */
	@Override
	public RandomVariableInterface squared() {
		return apply(OperatorType.SQUARED, new RandomVariableInterface[]{this});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#sqrt()
	 */
	@Override
	public RandomVariableInterface sqrt() {
		return apply(OperatorType.SQRT, new RandomVariableInterface[]{this});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#exp()
	 */
	@Override
	public RandomVariableInterface exp() {
		return apply(OperatorType.EXP, new RandomVariableInterface[]{this});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#log()
	 */
	@Override
	public RandomVariableInterface log() {
		return apply(OperatorType.LOG, new RandomVariableInterface[]{this});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#sin()
	 */
	@Override
	public RandomVariableInterface sin() {
		return apply(OperatorType.SIN, new RandomVariableInterface[]{this});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#cos()
	 */
	@Override
	public RandomVariableInterface cos() {
		return apply(OperatorType.COS, new RandomVariableInterface[]{this});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#add(net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public RandomVariableInterface add(RandomVariableInterface randomVariable) {	
		return apply(OperatorType.ADD, new RandomVariableInterface[]{this, randomVariable});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#sub(net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public RandomVariableInterface sub(RandomVariableInterface randomVariable) {
		return apply(OperatorType.SUB, new RandomVariableInterface[]{this, randomVariable});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#mult(net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public RandomVariableInterface mult(RandomVariableInterface randomVariable) {
		return apply(OperatorType.MULT, new RandomVariableInterface[]{this, randomVariable});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#div(net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public RandomVariableInterface div(RandomVariableInterface randomVariable) {
		return apply(OperatorType.DIV, new RandomVariableInterface[]{this, randomVariable});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#cap(net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public RandomVariableInterface cap(RandomVariableInterface cap) {
		return apply(OperatorType.CAP, new RandomVariableInterface[]{this, cap});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#floor(net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public RandomVariableInterface floor(RandomVariableInterface floor) {
		return apply(OperatorType.FLOOR, new RandomVariableInterface[]{this, floor});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#accrue(net.finmath.stochastic.RandomVariableInterface, double)
	 */
	@Override
	public RandomVariableInterface accrue(RandomVariableInterface rate, double periodLength) {
		return apply(OperatorType.ACCURUE, new RandomVariableInterface[]{this, rate, constructNewAADRandomVariable(periodLength)});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#discount(net.finmath.stochastic.RandomVariableInterface, double)
	 */
	@Override
	public RandomVariableInterface discount(RandomVariableInterface rate, double periodLength) {
		return apply(OperatorType.DISCOUNT, new RandomVariableInterface[]{this, rate, constructNewAADRandomVariable(periodLength)});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#barrier(net.finmath.stochastic.RandomVariableInterface, net.finmath.stochastic.RandomVariableInterface, net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public RandomVariableInterface barrier(RandomVariableInterface trigger,
			RandomVariableInterface valueIfTriggerNonNegative, RandomVariableInterface valueIfTriggerNegative) {
		return apply(OperatorType.BARRIER, new RandomVariableInterface[]{this, valueIfTriggerNonNegative, valueIfTriggerNegative});
		}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#barrier(net.finmath.stochastic.RandomVariableInterface, net.finmath.stochastic.RandomVariableInterface, double)
	 */
	@Override
	public RandomVariableInterface barrier(RandomVariableInterface trigger,
			RandomVariableInterface valueIfTriggerNonNegative, double valueIfTriggerNegative) {
		return apply(OperatorType.BARRIER, new RandomVariableInterface[]{this, valueIfTriggerNonNegative, constructNewAADRandomVariable(valueIfTriggerNegative)});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#invert()
	 */
	@Override
	public RandomVariableInterface invert() {
		return apply(OperatorType.INVERT, new RandomVariableInterface[]{this});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#abs()
	 */
	@Override
	public RandomVariableInterface abs() {
		return apply(OperatorType.ABS, new RandomVariableInterface[]{this});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#addProduct(net.finmath.stochastic.RandomVariableInterface, double)
	 */
	@Override
	public RandomVariableInterface addProduct(RandomVariableInterface factor1, double factor2) {
		return apply(OperatorType.ADDPRODUCT, new RandomVariableInterface[]{this, factor1, constructNewAADRandomVariable(factor2)});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#addProduct(net.finmath.stochastic.RandomVariableInterface, net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public RandomVariableInterface addProduct(RandomVariableInterface factor1, RandomVariableInterface factor2) {
		return apply(OperatorType.ADDPRODUCT, new RandomVariableInterface[]{this, factor1, factor2});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#addRatio(net.finmath.stochastic.RandomVariableInterface, net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public RandomVariableInterface addRatio(RandomVariableInterface numerator, RandomVariableInterface denominator) {
		return apply(OperatorType.ADDRATIO, new RandomVariableInterface[]{this, numerator, denominator});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#subRatio(net.finmath.stochastic.RandomVariableInterface, net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public RandomVariableInterface subRatio(RandomVariableInterface numerator, RandomVariableInterface denominator) {
		return apply(OperatorType.SUBRATIO, new RandomVariableInterface[]{this, numerator, denominator});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#isNaN()
	 */
	@Override
	public RandomVariableInterface isNaN() {
		return getRandomVariableInterface().isNaN();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getMutableCopy()
	 */
	@Override
	public RandomVariableInterface getMutableCopy() {
		// TODO Auto-generated method stub
		return null;
	}
	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getOperator()
	 */
	

	@Override
	public RandomVariableInterface apply(UnivariateFunction function) {
		// TODO Auto-generated method stub
		return null;
	}


}
