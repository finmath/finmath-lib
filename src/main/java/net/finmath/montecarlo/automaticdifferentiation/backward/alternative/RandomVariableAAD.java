/**
 * 
 */
package net.finmath.montecarlo.automaticdifferentiation.backward.alternative;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntToDoubleFunction;
import java.util.stream.DoubleStream;

import net.finmath.functions.DoubleTernaryOperator;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * Implementation of <code>RandomVariableInterface</code> having the additional feature to calculate the backward algorithmic differentiation.
 * 
 * For construction use the factory method <code>constructNewAADRandomVariable</code>.
 *
 * @author Stefan Sedlmair
 * @version 1.0
 */
public class RandomVariableAAD implements RandomVariableInterface {

	private static final long serialVersionUID = 2459373647785530657L;

	/* static elements of the class are shared between all members */
	private static ArrayList<RandomVariableAAD> arrayListOfAllAADRandomVariables = new ArrayList<>();
	private static AtomicInteger indexOfNextRandomVariable = new AtomicInteger(0);
	private static enum OperatorType {
		ADD, MULT, DIV, SUB, SQUARED, SQRT, LOG, SIN, COS, EXP, INVERT, CAP, FLOOR, ABS, 
		ADDPRODUCT, ADDRATIO, SUBRATIO, BARRIER, DISCOUNT, ACCRUE, POW, AVERAGE, VARIANCE, 
		STDEV, MIN, MAX, STDERROR, SVARIANCE
	}

	/* index of corresponding random variable in the static array list*/
	private final RandomVariableInterface ownRandomVariable;
	private final int ownIndexInList;

	/* this could maybe be outsourced to own class ParentElement */
	private final int[] parentIndices;
	private final OperatorType parentOperator;
	private ArrayList<Integer> childrenIndices;
	private boolean isConstant;

	/**
	 * @param indexOfRandomVariable
	 * @param parentRandomVariables
	 * @param parentOperator
	 * @param isConstant
	 */
	private RandomVariableAAD(int ownIndexInList, RandomVariableInterface ownRandomVariable, 
			int[] parentIndices, OperatorType parentOperator, ArrayList<Integer> childrenIndices ,boolean isConstant) {
		super();
		this.ownIndexInList = ownIndexInList;
		this.ownRandomVariable = ownRandomVariable;
		this.parentIndices = parentIndices;
		this.parentOperator = parentOperator;
		this.childrenIndices = childrenIndices;
		this.isConstant = isConstant;
	}

	

	/**
	 * @param randomVariable
	 * @param parentIndices
	 * @param parentOperator
	 * @param isConstant
	 * @return A new RandomVariableAAD.
	 */
	public static RandomVariableAAD constructNewAADRandomVariable(RandomVariableInterface randomVariable, int[] parentIndices,
			OperatorType parentOperator, ArrayList<Integer> childrenIndices, boolean isConstant){

		/* TODO: how to handle cases with different realization lengths? */
		if(!arrayListOfAllAADRandomVariables.isEmpty()){
			if(arrayListOfAllAADRandomVariables.get(0).size() != randomVariable.size() && !randomVariable.isDeterministic()){
				throw new IllegalArgumentException("RandomVariables with different sizes are not supported at the moment!");
			}
		}

		/* get index of this random variable */
		int indexOfThisAADRandomVariable = indexOfNextRandomVariable.getAndIncrement();

		RandomVariableAAD newAADRandomVariable = new RandomVariableAAD(indexOfThisAADRandomVariable, randomVariable, 
				parentIndices, parentOperator, childrenIndices, isConstant);

		/* add random variable to static list for book keeping */
		arrayListOfAllAADRandomVariables.add(indexOfThisAADRandomVariable, newAADRandomVariable);

		/* return a new random variable */
		return newAADRandomVariable;
	}

	public static RandomVariableAAD constructNewAADRandomVariable(double value){
		return constructNewAADRandomVariable(new RandomVariable(value), /*parentRandomVariables*/ null, /*parentOperator*/ null, /*childrenIndices*/ null ,/*isConstant*/ true);
	}


	public static RandomVariableAAD constructNewAADRandomVariable(RandomVariableInterface randomVariable) {
		return constructNewAADRandomVariable(randomVariable, /* no parents*/ null,
				/*no parent operator*/ null, /*no childrenIndices*/ null, /*not constant*/ false);
	}

	public static RandomVariableAAD constructNewAADRandomVariable(double time, double[] realisations) {
		return constructNewAADRandomVariable(new RandomVariable(time, realisations), /* no parents*/ null,
				/*no parent operator*/ null, /*no childrenIndices*/ null, /*not constant*/ false);
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
	
	private RandomVariableInterface apply(OperatorType operator, RandomVariableInterface[] randomVariableInterfaces){

		RandomVariableAAD[] aadRandomVariables = new RandomVariableAAD[randomVariableInterfaces.length];
		int[] futureParentIndices = new int[aadRandomVariables.length];

		for(int randomVariableIndex = 0; randomVariableIndex < randomVariableInterfaces.length; randomVariableIndex++){

			aadRandomVariables[randomVariableIndex] = (randomVariableInterfaces[randomVariableIndex] instanceof RandomVariableAAD) ?
					(RandomVariableAAD)randomVariableInterfaces[randomVariableIndex] : constructNewAADRandomVariable(randomVariableInterfaces[randomVariableIndex]);

			futureParentIndices[randomVariableIndex] = aadRandomVariables[randomVariableIndex].getFunctionIndex();
		}

		RandomVariableInterface resultrandomvariable;
		RandomVariableInterface X,Y,Z;

		if(randomVariableInterfaces.length == 1){

			X = aadRandomVariables[0].getRandomVariableInterface();

			switch(operator){
			case SQUARED:
				resultrandomvariable = X.squared();
				break;
			case SQRT:
				resultrandomvariable = X.sqrt();
				break;
			case EXP:
				resultrandomvariable = X.exp();
				break;
			case LOG:
				resultrandomvariable = X.log();
				break;
			case SIN:
				resultrandomvariable = X.sin();
				break;
			case COS:
				resultrandomvariable = X.cos();
				break;
			case ABS:
				resultrandomvariable = X.abs();
				break;
			case INVERT:
				resultrandomvariable = X.invert();
				break;
			case AVERAGE:
				resultrandomvariable = new RandomVariable(X.getAverage());
				break;
			case STDERROR:
				resultrandomvariable = new RandomVariable(X.getStandardError());
				break;
			case STDEV:
				resultrandomvariable = new RandomVariable(X.getStandardDeviation());
				break;
			case VARIANCE:
				resultrandomvariable = new RandomVariable(X.getVariance());
				break;
			case SVARIANCE:
				resultrandomvariable = new RandomVariable(X.getSampleVariance());
				break;
			default:
				throw new IllegalArgumentException();	
			}
		} else if (randomVariableInterfaces.length == 2){

			X = aadRandomVariables[0].getRandomVariableInterface();
			Y = aadRandomVariables[1].getRandomVariableInterface();

			switch(operator){
			case ADD:
				resultrandomvariable = X.add(Y);
				break;
			case SUB:
				resultrandomvariable = X.sub(Y);
				break;
			case MULT:
				resultrandomvariable = X.mult(Y);
				break;
			case DIV:
				resultrandomvariable = X.div(Y);
				break;
			case CAP:
				resultrandomvariable = X.cap( /* argument is deterministic random variable */ Y.getAverage());
				break;
			case FLOOR:
				resultrandomvariable = X.floor( /* argument is deterministic random variable */ Y.getAverage());
				break;			
			case POW:
				resultrandomvariable = X.pow( /* argument is deterministic random variable */ Y.getAverage());
				break;
			case AVERAGE:
				resultrandomvariable = new RandomVariable(X.getAverage(Y));
				break;
			case STDERROR:
				resultrandomvariable = new RandomVariable(X.getStandardError(Y));
				break;
			case STDEV:
				resultrandomvariable = new RandomVariable(X.getStandardDeviation(Y));
				break;
			case VARIANCE:
				resultrandomvariable = new RandomVariable(X.getVariance(Y));
				break;
			default:
				throw new IllegalArgumentException();	
			}
		} else if(randomVariableInterfaces.length == 3){

			X = aadRandomVariables[0].getRandomVariableInterface();
			Y = aadRandomVariables[1].getRandomVariableInterface();
			Z = aadRandomVariables[2].getRandomVariableInterface();

			switch(operator){
			case ADDPRODUCT:
				resultrandomvariable = X.addProduct(Y,Z);
				break;
			case ADDRATIO:
				resultrandomvariable = X.addRatio(Y, Z);
				break;
			case SUBRATIO:
				resultrandomvariable = X.subRatio(Y, Z);
				break;
			case ACCRUE:
				resultrandomvariable = X.accrue(Y, /* second argument is deterministic anyway */ Z.getAverage());
				break;
			case DISCOUNT:
				resultrandomvariable = X.discount(Y, /* second argument is deterministic anyway */ Z.getAverage());
				break;
			default:
				throw new IllegalArgumentException();
			}
		} else {
			/* if non of the above throw exception */
			throw new IllegalArgumentException("Operation not supported!\n");
		}
		
		/* create new RandomVariableUniqueVariable which is definitely NOT Constant */
		RandomVariableAAD newRandomVariableAAD =  constructNewAADRandomVariable(resultrandomvariable, futureParentIndices, operator, /*no children*/ null ,/*not constant*/ false);
	
		/* add new variable (or at least its index) as child to its parents */
		for(RandomVariableAAD parentRandomVariable:aadRandomVariables) 
			parentRandomVariable.addToChildrenIndices(newRandomVariableAAD.getFunctionIndex());
		
		/* return new RandomVariable */
		return newRandomVariableAAD;
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

		RandomVariableInterface resultrandomvariable = null;
		RandomVariableInterface X,Y,Z;
		double[] resultRandomVariableRealizations;
		
		if(getParentIDs().length == 1){
			
			X = getRandomVariableInterfaceOfIndex(getParentIDs()[0]).getMutableCopy();
		
			switch(parentOperator){
			/* functions with one argument  */
			case SQUARED:
				resultrandomvariable = X.mult(2.0);
				break;
			case SQRT:
				resultrandomvariable = X.sqrt().invert().mult(0.5);
				break;
			case EXP:
				resultrandomvariable = X.exp();
				break;
			case LOG:
				resultrandomvariable = X.invert();
				break;
			case SIN:
				resultrandomvariable = X.cos();
				break;
			case COS:
				resultrandomvariable = X.sin().mult(-1.0);
				break;
			case AVERAGE:
				resultrandomvariable = new RandomVariable(X.size()).invert();
				break;
			case VARIANCE:
				resultrandomvariable = X.sub(X.getAverage()*(2.0*X.size()-1.0)/X.size()).mult(2.0/X.size());
				break;
			case STDEV:
				resultrandomvariable = X.sub(X.getAverage()*(2.0*X.size()-1.0)/X.size()).mult(2.0/X.size()).mult(0.5).div(Math.sqrt(X.getVariance()));
				break;
			case MIN:
				resultrandomvariable = X.apply(x -> (x == X.getMin()) ? 1.0 : 0.0);
//				resultRandomVariableRealizations = new double[X.size()];
//				for(int i = 0; i < X.size(); i++) resultRandomVariableRealizations[i] = (X.getRealizations()[i] == X.getMin()) ? 1.0 : 0.0;
//				resultrandomvariable = new RandomVariable(X.getFiltrationTime(), resultRandomVariableRealizations);
				break;
			case MAX:
				resultrandomvariable = X.apply(x -> (x == X.getMax()) ? 1.0 : 0.0);
//				resultRandomVariableRealizations = new double[X.size()];
//				for(int i = 0; i < X.size(); i++) resultRandomVariableRealizations[i] = (X.getRealizations()[i] == X.getMax()) ? 1.0 : 0.0;
//				resultrandomvariable = new RandomVariable(X.getFiltrationTime(), resultRandomVariableRealizations);
				break;
			case ABS:
				resultrandomvariable = X.apply(x -> (x > 0.0) ? 1.0 : (x < 0) ? -1.0 : 0.0);
//				resultRandomVariableRealizations = new double[X.size()];
//				for(int i = 0; i < X.size(); i++) resultRandomVariableRealizations[i] = (X.getRealizations()[i] > 0) ? 1.0 : (X.getRealizations()[i] < 0) ? -1.0 : 0.0;
//				resultrandomvariable = new RandomVariable(X.getFiltrationTime(), resultRandomVariableRealizations);
				break;
			case STDERROR:
				resultrandomvariable = X.sub(X.getAverage()*(2.0*X.size()-1.0)/X.size()).mult(2.0/X.size()).mult(0.5).div(Math.sqrt(X.getVariance() * X.size()));
				break;
			case SVARIANCE:
				resultrandomvariable = X.sub(X.getAverage()*(2.0*X.size()-1.0)/X.size()).mult(2.0/(X.size()-1));
				break;
			default:
				break;
			}
		} else if(getParentIDs().length == 2){
			
			X = getRandomVariableInterfaceOfIndex(getParentIDs()[0]).getMutableCopy();
			Y = getRandomVariableInterfaceOfIndex(getParentIDs()[1]).getMutableCopy();

			switch(parentOperator){
			case ADD:
				resultrandomvariable = new RandomVariable(1.0);
				break;
			case SUB:
				resultrandomvariable = new RandomVariable((variableIndex == getParentIDs()[0]) ? 1.0 : -1.0);
				break;
			case MULT:
				resultrandomvariable = (variableIndex == getParentIDs()[0]) ? Y : X;
				break;
			case DIV:
				resultrandomvariable = (variableIndex == getParentIDs()[0]) ? Y.invert() : X.div(Y.squared());
				break;
			case CAP:
				resultrandomvariable = X.apply(x -> (x > Y.getAverage()) ? 0.0 : 1.0);
//				resultRandomVariableRealizations = new double[X.size()];
//				for(int i = 0; i < X.size(); i++) resultRandomVariableRealizations[i] = (X.getRealizations()[i] > Y.getAverage()) ? 0.0 : 1.0;
//				resultrandomvariable = new RandomVariable(X.getFiltrationTime(), resultRandomVariableRealizations);
				break;
			case FLOOR:
				resultrandomvariable = X.apply(x -> (x > Y.getAverage()) ? 1.0 : 0.0);
//				resultRandomVariableRealizations = new double[X.size()];
//				for(int i = 0; i < X.size(); i++) resultRandomVariableRealizations[i] = (X.getRealizations()[i] > Y.getAverage()) ? 1.0 : 0.0;
//				resultrandomvariable = new RandomVariable(X.getFiltrationTime(), resultRandomVariableRealizations);
				break;
			case AVERAGE:
				resultrandomvariable = (variableIndex == getParentIDs()[0]) ? Y : X;
				break;
			case VARIANCE:
				resultrandomvariable = (variableIndex == getParentIDs()[0]) ? Y.mult(2.0).mult(X.mult(Y.add(X.getAverage(Y)*(X.size()-1)).sub(X.getAverage(Y)))) :
					X.mult(2.0).mult(Y.mult(X.add(Y.getAverage(X)*(X.size()-1)).sub(Y.getAverage(X))));
				break;
			case STDEV:				
				resultrandomvariable = (variableIndex == getParentIDs()[0]) ? Y.mult(2.0).mult(X.mult(Y.add(X.getAverage(Y)*(X.size()-1)).sub(X.getAverage(Y)))).div(Math.sqrt(X.getVariance(Y))) :
				X.mult(2.0).mult(Y.mult(X.add(Y.getAverage(X)*(X.size()-1)).sub(Y.getAverage(X)))).div(Math.sqrt(Y.getVariance(X)));
				break;
			case STDERROR:				
				resultrandomvariable = (variableIndex == getParentIDs()[0]) ? Y.mult(2.0).mult(X.mult(Y.add(X.getAverage(Y)*(X.size()-1)).sub(X.getAverage(Y)))).div(Math.sqrt(X.getVariance(Y) * X.size())) :
				X.mult(2.0).mult(Y.mult(X.add(Y.getAverage(X)*(X.size()-1)).sub(Y.getAverage(X)))).div(Math.sqrt(Y.getVariance(X) * Y.size()));
				break;
			case POW:
				/* second argument will always be deterministic and constant! */
				resultrandomvariable = (variableIndex == getParentIDs()[0]) ? Y.mult(X.pow(Y.getAverage() - 1.0)) : new RandomVariable(0.0);
				break;
			default:
				break;
			}
		} else if(getParentIDs().length == 3){ 
			X = getRandomVariableInterfaceOfIndex(getParentIDs()[0]);
			Y = getRandomVariableInterfaceOfIndex(getParentIDs()[1]);
			Z = getRandomVariableInterfaceOfIndex(getParentIDs()[2]);

			switch(parentOperator){
			case ADDPRODUCT:
				if(variableIndex == getParentIDs()[0]){
					resultrandomvariable = new RandomVariable(1.0);
				} else if(variableIndex == getParentIDs()[1]){
					resultrandomvariable = Z;
				} else {
					resultrandomvariable = Y;
				}
				break;
			case ADDRATIO:
				if(variableIndex == getParentIDs()[0]){
					resultrandomvariable = new RandomVariable(1.0);
				} else if(variableIndex == getParentIDs()[0]){
					resultrandomvariable = Z.invert();
				} else {
					resultrandomvariable = Y.div(Z.squared());
				}
				break;
			case SUBRATIO:
				if(variableIndex == getParentIDs()[0]){
					resultrandomvariable = new RandomVariable(1.0);
				} else if(variableIndex == getParentIDs()[1]){
					resultrandomvariable = Z.invert().mult(-1.0);
				} else {
					resultrandomvariable = Y.div(Z.squared()).mult(-1.0);
				}
				break;
			case ACCRUE:
				if(variableIndex == getParentIDs()[0]){
					resultrandomvariable = Y.mult(Z).add(1.0);
				} else if(variableIndex == getParentIDs()[1]){
					resultrandomvariable = X.mult(Z);
				} else {
					resultrandomvariable = X.mult(Y);
				}
				break;
			case DISCOUNT:
				if(variableIndex == getParentIDs()[0]){
					resultrandomvariable = Y.mult(Z).add(1.0).invert();
				} else if(variableIndex == getParentIDs()[1]){
					resultrandomvariable = X.mult(Z).div(Y.mult(Z).add(1.0).squared());
				} else {
					resultrandomvariable = X.mult(Y).div(Y.mult(Z).add(1.0).squared());
				}
				break;
			case BARRIER:
				if(variableIndex == getParentIDs()[0]){
					resultrandomvariable = X.apply(x -> (x == 0.0) ? Double.POSITIVE_INFINITY : 0.0);
				} else if(variableIndex == getParentIDs()[1]){
					resultrandomvariable = X.barrier(X, new RandomVariable(1.0), new RandomVariable(0.0));
				} else {
					resultrandomvariable = X.barrier(X, new RandomVariable(0.0), new RandomVariable(1.0));
				}
			default:
				break;
			}
		} else {
			/* if non of the above throw exception */
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
			
			ArrayList<Integer> childrenList = getAADRandomVariableFromList(variableIndex).getChildrenIndices();
			
			for(int functionIndex:childrenList){
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

	/* for all functions that need to be differentiated and are returned as double in the Interface, write a method to return it as RandomVariableAAD 
	 * that is deterministic by its nature. For their double-returning pendant just return the average of the deterministic RandomVariableAAD  */

	public RandomVariableInterface getAverageAsRandomVariableAAD(RandomVariableInterface probabilities){
		/*returns deterministic AAD random variable */
		return apply(OperatorType.AVERAGE, new RandomVariableInterface[]{this, probabilities});
	}

	public RandomVariableInterface getVarianceAsRandomVariableAAD(RandomVariableInterface probabilities){
		/*returns deterministic AAD random variable */
		return apply(OperatorType.VARIANCE, new RandomVariableInterface[]{this, probabilities});
	}

	public RandomVariableInterface 	getStandardDeviationAsRandomVariableAAD(RandomVariableInterface probabilities){
		/*returns deterministic AAD random variable */
		return apply(OperatorType.STDEV, new RandomVariableInterface[]{this, probabilities});
	}

	public RandomVariableInterface 	getStandardErrorAsRandomVariableAAD(RandomVariableInterface probabilities){
		/*returns deterministic AAD random variable */
		return apply(OperatorType.STDERROR, new RandomVariableInterface[]{this, probabilities});
	}

	public RandomVariableInterface getAverageAsRandomVariableAAD(){
		/*returns deterministic AAD random variable */
		return apply(OperatorType.AVERAGE, new RandomVariableInterface[]{this});
	}

	public RandomVariableInterface getVarianceAsRandomVariableAAD(){
		/*returns deterministic AAD random variable */
		return apply(OperatorType.VARIANCE, new RandomVariableInterface[]{this});
	}

	public RandomVariableInterface getSampleVarianceAsRandomVariableAAD() {
		/*returns deterministic AAD random variable */
		return apply(OperatorType.SVARIANCE, new RandomVariableInterface[]{this});
	}

	public RandomVariableInterface 	getStandardDeviationAsRandomVariableAAD(){
		/*returns deterministic AAD random variable */
		return apply(OperatorType.STDEV, new RandomVariableInterface[]{this});
	}

	public RandomVariableInterface getStandardErrorAsRandomVariableAAD(){
		/*returns deterministic AAD random variable */
		return apply(OperatorType.STDERROR, new RandomVariableInterface[]{this});
	}

	public RandomVariableInterface 	getMinAsRandomVariableAAD(){
		/*returns deterministic AAD random variable */
		return apply(OperatorType.MIN, new RandomVariableInterface[]{this});
	}

	public RandomVariableInterface 	getMaxAsRandomVariableAAD(){
		/*returns deterministic AAD random variable */
		return apply(OperatorType.MAX, new RandomVariableInterface[]{this});
	}

	/* setter and getter */

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
	
	public static void resetArrayListOfAllAADRandomVariables(){
		synchronized (arrayListOfAllAADRandomVariables) {
			arrayListOfAllAADRandomVariables = new ArrayList<>();
			indexOfNextRandomVariable = new AtomicInteger(0);
		}
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
	
	private ArrayList<Integer> getChildrenIndices(){
		if(childrenIndices == null) childrenIndices = new ArrayList<>();
		return childrenIndices;
	}

	private int getNumberOfParentVariables(){
		if(getParentIDs() == null) return 0;
		return getParentIDs().length;
	}

	private RandomVariableAAD getAADRandomVariableFromList(int index){
		return getFunctionList().get(index);
	}
	
	private void addToChildrenIndices(int index){
		getChildrenIndices().add(index);
	}
	
	/*--------------------------------------------------------------------------------------------------------------------------------------------------*/



	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#equals(net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public boolean equals(RandomVariableInterface randomVariable) {
		return getRandomVariableInterface().equals(randomVariable);
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
		return ((RandomVariableAAD) getMinAsRandomVariableAAD()).getRandomVariableInterface().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getMax()
	 */
	@Override
	public double getMax() {
		return ((RandomVariableAAD) getMaxAsRandomVariableAAD()).getRandomVariableInterface().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getAverage()
	 */
	@Override
	public double getAverage() {		
		return ((RandomVariableAAD) getAverageAsRandomVariableAAD()).getRandomVariableInterface().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getAverage(net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public double getAverage(RandomVariableInterface probabilities) {
		return ((RandomVariableAAD) getAverageAsRandomVariableAAD(probabilities)).getRandomVariableInterface().getAverage();
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
		return ((RandomVariableAAD) getAverageAsRandomVariableAAD(probabilities)).getRandomVariableInterface().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getSampleVariance()
	 */
	@Override
	public double getSampleVariance() {
		return ((RandomVariableAAD) getSampleVarianceAsRandomVariableAAD()).getRandomVariableInterface().getAverage();
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
		return ((RandomVariableAAD) getStandardDeviationAsRandomVariableAAD(probabilities)).getRandomVariableInterface().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getStandardError()
	 */
	@Override
	public double getStandardError() {
		return ((RandomVariableAAD) getStandardErrorAsRandomVariableAAD()).getRandomVariableInterface().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getStandardError(net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public double getStandardError(RandomVariableInterface probabilities) {
		return ((RandomVariableAAD) getStandardErrorAsRandomVariableAAD(probabilities)).getRandomVariableInterface().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getQuantile(double)
	 */
	@Override
	public double getQuantile(double quantile) {
		return ((RandomVariableAAD) getRandomVariableInterface()).getRandomVariableInterface().getQuantile(quantile);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getQuantile(double, net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public double getQuantile(double quantile, RandomVariableInterface probabilities) {
		return ((RandomVariableAAD) getRandomVariableInterface()).getRandomVariableInterface().getQuantile(quantile, probabilities);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getQuantileExpectation(double, double)
	 */
	@Override
	public double getQuantileExpectation(double quantileStart, double quantileEnd) {
		return ((RandomVariableAAD) getRandomVariableInterface()).getRandomVariableInterface().getQuantileExpectation(quantileStart, quantileEnd);
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
		return this;
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
		return apply(OperatorType.ACCRUE, new RandomVariableInterface[]{this, rate, constructNewAADRandomVariable(periodLength)});
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
		return this;
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
