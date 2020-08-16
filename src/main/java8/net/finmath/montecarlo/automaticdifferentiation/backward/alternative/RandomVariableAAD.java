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
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.stochastic.RandomVariable;

/**
 * Implementation of <code>RandomVariable</code> having the additional feature to calculate the backward algorithmic differentiation.
 *
 * For construction use the factory method <code>constructNewAADRandomVariable</code>.
 *
 * @author Stefan Sedlmair
 * @version 1.0
 */
public class RandomVariableAAD implements RandomVariable {

	private static final long serialVersionUID = 2459373647785530657L;

	/* static elements of the class are shared between all members */
	private static ArrayList<RandomVariableAAD> arrayListOfAllAADRandomVariables = new ArrayList<>();
	private static AtomicInteger indexOfNextRandomVariable = new AtomicInteger(0);
	private enum OperatorType {
		ADD, MULT, DIV, SUB, SQUARED, SQRT, LOG, SIN, COS, EXP, INVERT, CAP, FLOOR, ABS,
		ADDPRODUCT, ADDRATIO, SUBRATIO, BARRIER, DISCOUNT, ACCRUE, POW, AVERAGE, VARIANCE,
		STDEV, MIN, MAX, STDERROR, SVARIANCE
	}

	/* index of corresponding random variable in the static array list*/
	private final RandomVariable ownRandomVariable;
	private final int ownIndexInList;

	/* this could maybe be outsourced to own class ParentElement */
	private final int[] parentIndices;
	private final OperatorType parentOperator;
	private ArrayList<Integer> childrenIndices;
	private boolean isConstant;

	private RandomVariableAAD(final int ownIndexInList, final RandomVariable ownRandomVariable,
			final int[] parentIndices, final OperatorType parentOperator, final ArrayList<Integer> childrenIndices ,final boolean isConstant) {
		super();
		this.ownIndexInList = ownIndexInList;
		this.ownRandomVariable = ownRandomVariable;
		this.parentIndices = parentIndices;
		this.parentOperator = parentOperator;
		this.childrenIndices = childrenIndices;
		this.isConstant = isConstant;
	}

	public static RandomVariableAAD constructNewAADRandomVariable(final RandomVariable randomVariable, final int[] parentIndices,
			final OperatorType parentOperator, final ArrayList<Integer> childrenIndices, final boolean isConstant){

		/* TODO how to handle cases with different realization lengths? */
		if(!arrayListOfAllAADRandomVariables.isEmpty()){
			if(arrayListOfAllAADRandomVariables.get(0).size() != randomVariable.size() && !randomVariable.isDeterministic()) {
				throw new IllegalArgumentException("RandomVariables with different sizes are not supported at the moment!");
			}
		}

		/* get index of this random variable */
		final int indexOfThisAADRandomVariable = indexOfNextRandomVariable.getAndIncrement();

		final RandomVariableAAD newAADRandomVariable = new RandomVariableAAD(indexOfThisAADRandomVariable, randomVariable,
				parentIndices, parentOperator, childrenIndices, isConstant);

		/* add random variable to static list for book keeping */
		arrayListOfAllAADRandomVariables.add(indexOfThisAADRandomVariable, newAADRandomVariable);

		/* return a new random variable */
		return newAADRandomVariable;
	}

	public static RandomVariableAAD constructNewAADRandomVariable(final double value){
		return constructNewAADRandomVariable(new RandomVariableFromDoubleArray(value), /*parentRandomVariables*/ null, /*parentOperator*/ null, /*childrenIndices*/ null ,/*isConstant*/ true);
	}


	public static RandomVariableAAD constructNewAADRandomVariable(final RandomVariable randomVariable) {
		return constructNewAADRandomVariable(randomVariable, /* no parents*/ null,
				/*no parent operator*/ null, /*no childrenIndices*/ null, /*not constant*/ false);
	}

	public static RandomVariableAAD constructNewAADRandomVariable(final double time, final double[] realisations) {
		return constructNewAADRandomVariable(new RandomVariableFromDoubleArray(time, realisations), /* no parents*/ null,
				/*no parent operator*/ null, /*no childrenIndices*/ null, /*not constant*/ false);
	}

	private RandomVariableAAD[] getParentAADRandomVariables(){

		if(getParentIDs() == null) {
			return null;
		}

		final int[] parentIndices = getParentIDs();
		final RandomVariableAAD[] parentAADRandomVariables = new RandomVariableAAD[getNumberOfParentVariables()];

		for(int i=0; i < parentAADRandomVariables.length; i++){
			parentAADRandomVariables[i] = getAADRandomVariableFromList(parentIndices[i]);
		}

		return parentAADRandomVariables;
	}

	/**
	 * @return
	 */
	private RandomVariable[] getParentRandomVariableInderfaces(){

		final RandomVariableAAD[] parentAADRandomVariables = getParentAADRandomVariables();
		final RandomVariable[] parentRandomVariableInderfaces = new RandomVariable[parentAADRandomVariables.length];

		for(int i=0;i<parentAADRandomVariables.length;i++){
			parentRandomVariableInderfaces[i] = parentAADRandomVariables[i].getRandomVariableInterface();
		}

		return parentRandomVariableInderfaces;
	}

	private RandomVariable apply(final OperatorType operator, final RandomVariable[] randomVariableInterfaces){

		final RandomVariableAAD[] aadRandomVariables = new RandomVariableAAD[randomVariableInterfaces.length];
		final int[] futureParentIndices = new int[aadRandomVariables.length];

		for(int randomVariableIndex = 0; randomVariableIndex < randomVariableInterfaces.length; randomVariableIndex++){

			aadRandomVariables[randomVariableIndex] = (randomVariableInterfaces[randomVariableIndex] instanceof RandomVariableAAD) ?
					(RandomVariableAAD)randomVariableInterfaces[randomVariableIndex] : constructNewAADRandomVariable(randomVariableInterfaces[randomVariableIndex]);

					futureParentIndices[randomVariableIndex] = aadRandomVariables[randomVariableIndex].getFunctionIndex();
		}

		RandomVariable resultrandomvariable;
		RandomVariable X,Y,Z;

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
				resultrandomvariable = new RandomVariableFromDoubleArray(X.getAverage());
				break;
			case STDERROR:
				resultrandomvariable = new RandomVariableFromDoubleArray(X.getStandardError());
				break;
			case STDEV:
				resultrandomvariable = new RandomVariableFromDoubleArray(X.getStandardDeviation());
				break;
			case VARIANCE:
				resultrandomvariable = new RandomVariableFromDoubleArray(X.getVariance());
				break;
			case SVARIANCE:
				resultrandomvariable = new RandomVariableFromDoubleArray(X.getSampleVariance());
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
				resultrandomvariable = new RandomVariableFromDoubleArray(X.getAverage(Y));
				break;
			case STDERROR:
				resultrandomvariable = new RandomVariableFromDoubleArray(X.getStandardError(Y));
				break;
			case STDEV:
				resultrandomvariable = new RandomVariableFromDoubleArray(X.getStandardDeviation(Y));
				break;
			case VARIANCE:
				resultrandomvariable = new RandomVariableFromDoubleArray(X.getVariance(Y));
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
		final RandomVariableAAD newRandomVariableAAD =  constructNewAADRandomVariable(resultrandomvariable, futureParentIndices, operator, /*no children*/ null ,/*not constant*/ false);

		/* add new variable (or at least its index) as child to its parents */
		for(final RandomVariableAAD parentRandomVariable:aadRandomVariables) {
			parentRandomVariable.addToChildrenIndices(newRandomVariableAAD.getFunctionIndex());
		}

		/* return new RandomVariableFromDoubleArray */
		return newRandomVariableAAD;
	}

	@Override
	public String toString(){
		return  super.toString() + "\n" +
				"time: " + getFiltrationTime() + "\n" +
				"realizations: " + Arrays.toString(getRealizations()) + "\n" +
				"variableID: " + getFunctionIndex() + "\n" +
				"parentIDs: " + Arrays.toString(getParentIDs()) + ((getParentIDs() == null) ? "" : (" type: " + parentOperator.name())) + "\n" +
				"isTrueVariable: " + isVariable() + "\n";
	}

	private RandomVariable getPartialDerivative(final int functionIndex, final int variableIndex){
		return getFunctionList().get(functionIndex).partialDerivativeWithRespectTo(variableIndex);
	}

	private RandomVariable partialDerivativeWithRespectTo(final int variableIndex){

		/* parentIDsSorted needs to be sorted for binarySearch! */
		final int[] parentIDsSorted = (getParentIDs() == null) ? new int[]{} : getParentIDs().clone();
		Arrays.sort(parentIDsSorted);

		/* if random variable not dependent on variable or it is constant anyway return 0.0 */
		if((Arrays.binarySearch(parentIDsSorted, variableIndex) < 0) || isConstant) {
			return new RandomVariableFromDoubleArray(0.0);
		}

		RandomVariable resultrandomvariable = null;
		RandomVariable X,Y,Z;
		final double[] resultRandomVariableRealizations;

		if(getParentIDs().length == 1){

			X = getRandomVariableInterfaceOfIndex(getParentIDs()[0]);

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
				resultrandomvariable = new RandomVariableFromDoubleArray(X.size()).invert();
				break;
			case VARIANCE:
				resultrandomvariable = X.sub(X.getAverage()*(2.0*X.size()-1.0)/X.size()).mult(2.0/X.size());
				break;
			case STDEV:
				resultrandomvariable = X.sub(X.getAverage()*(2.0*X.size()-1.0)/X.size()).mult(2.0/X.size()).mult(0.5).div(Math.sqrt(X.getVariance()));
				break;
			case MIN:
				resultrandomvariable = X.apply(new DoubleUnaryOperator() {
					@Override
					public double applyAsDouble(final double x) {
						return (x == X.getMin()) ? 1.0 : 0.0;
					}
				});
				//				resultRandomVariableRealizations = new double[X.size()];
				//				for(int i = 0; i < X.size(); i++) resultRandomVariableRealizations[i] = (X.getRealizations()[i] == X.getMin()) ? 1.0 : 0.0;
				//				resultrandomvariable = new RandomVariableFromDoubleArray(X.getFiltrationTime(), resultRandomVariableRealizations);
				break;
			case MAX:
				resultrandomvariable = X.apply(new DoubleUnaryOperator() {
					@Override
					public double applyAsDouble(final double x) {
						return (x == X.getMax()) ? 1.0 : 0.0;
					}
				});
				//				resultRandomVariableRealizations = new double[X.size()];
				//				for(int i = 0; i < X.size(); i++) resultRandomVariableRealizations[i] = (X.getRealizations()[i] == X.getMax()) ? 1.0 : 0.0;
				//				resultrandomvariable = new RandomVariableFromDoubleArray(X.getFiltrationTime(), resultRandomVariableRealizations);
				break;
			case ABS:
				resultrandomvariable = X.apply(new DoubleUnaryOperator() {
					@Override
					public double applyAsDouble(final double x) {
						return (x > 0.0) ? 1.0 : (x < 0) ? -1.0 : 0.0;
					}
				});
				//				resultRandomVariableRealizations = new double[X.size()];
				//				for(int i = 0; i < X.size(); i++) resultRandomVariableRealizations[i] = (X.getRealizations()[i] > 0) ? 1.0 : (X.getRealizations()[i] < 0) ? -1.0 : 0.0;
				//				resultrandomvariable = new RandomVariableFromDoubleArray(X.getFiltrationTime(), resultRandomVariableRealizations);
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

			X = getRandomVariableInterfaceOfIndex(getParentIDs()[0]);
			Y = getRandomVariableInterfaceOfIndex(getParentIDs()[1]);

			switch(parentOperator){
			case ADD:
				resultrandomvariable = new RandomVariableFromDoubleArray(1.0);
				break;
			case SUB:
				resultrandomvariable = new RandomVariableFromDoubleArray((variableIndex == getParentIDs()[0]) ? 1.0 : -1.0);
				break;
			case MULT:
				resultrandomvariable = (variableIndex == getParentIDs()[0]) ? Y : X;
				break;
			case DIV:
				resultrandomvariable = (variableIndex == getParentIDs()[0]) ? Y.invert() : X.div(Y.squared()).mult(-1);
				break;
			case CAP:
				resultrandomvariable = X.apply(new DoubleUnaryOperator() {
					@Override
					public double applyAsDouble(final double x) {
						return (x > Y.getAverage()) ? 0.0 : 1.0;
					}
				});
				//				resultRandomVariableRealizations = new double[X.size()];
				//				for(int i = 0; i < X.size(); i++) resultRandomVariableRealizations[i] = (X.getRealizations()[i] > Y.getAverage()) ? 0.0 : 1.0;
				//				resultrandomvariable = new RandomVariableFromDoubleArray(X.getFiltrationTime(), resultRandomVariableRealizations);
				break;
			case FLOOR:
				resultrandomvariable = X.apply(new DoubleUnaryOperator() {
					@Override
					public double applyAsDouble(final double x) {
						return (x > Y.getAverage()) ? 1.0 : 0.0;
					}
				});
				//				resultRandomVariableRealizations = new double[X.size()];
				//				for(int i = 0; i < X.size(); i++) resultRandomVariableRealizations[i] = (X.getRealizations()[i] > Y.getAverage()) ? 1.0 : 0.0;
				//				resultrandomvariable = new RandomVariableFromDoubleArray(X.getFiltrationTime(), resultRandomVariableRealizations);
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
				resultrandomvariable = (variableIndex == getParentIDs()[0]) ? Y.mult(X.pow(Y.getAverage() - 1.0)) : new RandomVariableFromDoubleArray(0.0);
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
					resultrandomvariable = new RandomVariableFromDoubleArray(1.0);
				} else if(variableIndex == getParentIDs()[1]){
					resultrandomvariable = Z;
				} else {
					resultrandomvariable = Y;
				}
				break;
			case ADDRATIO:
				if(variableIndex == getParentIDs()[0]){
					resultrandomvariable = new RandomVariableFromDoubleArray(1.0);
				} else if(variableIndex == getParentIDs()[0]){
					resultrandomvariable = Z.invert();
				} else {
					resultrandomvariable = Y.div(Z.squared());
				}
				break;
			case SUBRATIO:
				if(variableIndex == getParentIDs()[0]){
					resultrandomvariable = new RandomVariableFromDoubleArray(1.0);
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
					resultrandomvariable = X.apply(new DoubleUnaryOperator() {
						@Override
						public double applyAsDouble(final double x) {
							return (x == 0.0) ? Double.POSITIVE_INFINITY : 0.0;
						}
					});
				} else if(variableIndex == getParentIDs()[1]){
					resultrandomvariable = X.choose(new RandomVariableFromDoubleArray(1.0), new RandomVariableFromDoubleArray(0.0));
				} else {
					resultrandomvariable = X.choose(new RandomVariableFromDoubleArray(0.0), new RandomVariableFromDoubleArray(1.0));
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
	public Map<Integer, RandomVariable> getGradient(){

		final int numberOfCalculationSteps = getFunctionList().size();

		final RandomVariable[] omegaHat = new RandomVariable[numberOfCalculationSteps];

		omegaHat[numberOfCalculationSteps-1] = new RandomVariableFromDoubleArray(1.0);

		for(int variableIndex = numberOfCalculationSteps-2; variableIndex >= 0; variableIndex--){

			omegaHat[variableIndex] = new RandomVariableFromDoubleArray(0.0);

			final ArrayList<Integer> childrenList = getAADRandomVariableFromList(variableIndex).getChildrenIndices();

			for(final int functionIndex:childrenList){
				final RandomVariable D_i_j = getPartialDerivative(functionIndex, variableIndex);
				omegaHat[variableIndex] = omegaHat[variableIndex].addProduct(D_i_j, omegaHat[functionIndex]);
			}
		}

		final ArrayList<Integer> arrayListOfAllIndicesOfDependentRandomVariables = getArrayListOfAllIndicesOfDependentRandomVariables();

		final Map<Integer, RandomVariable> gradient = new HashMap<>();

		for(final Integer indexOfDependentRandomVariable: arrayListOfAllIndicesOfDependentRandomVariables){
			gradient.put(indexOfDependentRandomVariable, omegaHat[arrayListOfAllIndicesOfDependentRandomVariables.get(indexOfDependentRandomVariable)]);
		}

		return gradient;
	}

	private ArrayList<Integer> getArrayListOfAllIndicesOfDependentRandomVariables(){

		final ArrayList<Integer> arrayListOfAllIndicesOfDependentRandomVariables = new ArrayList<>();

		for(int index = 0; index < getNumberOfParentVariables(); index++){

			final int currentParentIndex = getParentIDs()[index];

			/* if current index belongs to a true variable and is not yet in the list: add it*/
			if(getAADRandomVariableFromList(currentParentIndex).isVariable() &&
					!arrayListOfAllIndicesOfDependentRandomVariables.contains(currentParentIndex)){
				arrayListOfAllIndicesOfDependentRandomVariables.add(currentParentIndex);
			} else {
				arrayListOfAllIndicesOfDependentRandomVariables.addAll(
						getAADRandomVariableFromList(currentParentIndex).getArrayListOfAllIndicesOfDependentRandomVariables());
			}
		}

		return arrayListOfAllIndicesOfDependentRandomVariables;
	}

	/* for all functions that need to be differentiated and are returned as double in the Interface, write a method to return it as RandomVariableAAD
	 * that is deterministic by its nature. For their double-returning pendant just return the average of the deterministic RandomVariableAAD  */

	public RandomVariable getAverageAsRandomVariableAAD(final RandomVariable probabilities){
		/*returns deterministic AAD random variable */
		return apply(OperatorType.AVERAGE, new RandomVariable[]{this, probabilities});
	}

	public RandomVariable getVarianceAsRandomVariableAAD(final RandomVariable probabilities){
		/*returns deterministic AAD random variable */
		return apply(OperatorType.VARIANCE, new RandomVariable[]{this, probabilities});
	}

	public RandomVariable 	getStandardDeviationAsRandomVariableAAD(final RandomVariable probabilities){
		/*returns deterministic AAD random variable */
		return apply(OperatorType.STDEV, new RandomVariable[]{this, probabilities});
	}

	public RandomVariable 	getStandardErrorAsRandomVariableAAD(final RandomVariable probabilities){
		/*returns deterministic AAD random variable */
		return apply(OperatorType.STDERROR, new RandomVariable[]{this, probabilities});
	}

	public RandomVariable getAverageAsRandomVariableAAD(){
		/*returns deterministic AAD random variable */
		return apply(OperatorType.AVERAGE, new RandomVariable[]{this});
	}

	public RandomVariable getVarianceAsRandomVariableAAD(){
		/*returns deterministic AAD random variable */
		return apply(OperatorType.VARIANCE, new RandomVariable[]{this});
	}

	public RandomVariable getSampleVarianceAsRandomVariableAAD() {
		/*returns deterministic AAD random variable */
		return apply(OperatorType.SVARIANCE, new RandomVariable[]{this});
	}

	public RandomVariable 	getStandardDeviationAsRandomVariableAAD(){
		/*returns deterministic AAD random variable */
		return apply(OperatorType.STDEV, new RandomVariable[]{this});
	}

	public RandomVariable getStandardErrorAsRandomVariableAAD(){
		/*returns deterministic AAD random variable */
		return apply(OperatorType.STDERROR, new RandomVariable[]{this});
	}

	public RandomVariable 	getMinAsRandomVariableAAD(){
		/*returns deterministic AAD random variable */
		return apply(OperatorType.MIN, new RandomVariable[]{this});
	}

	public RandomVariable 	getMaxAsRandomVariableAAD(){
		/*returns deterministic AAD random variable */
		return apply(OperatorType.MAX, new RandomVariable[]{this});
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

	public void setIsConstantTo(final boolean isConstant){
		this.isConstant = isConstant;
	}

	private RandomVariable getRandomVariableInterface(){
		return ownRandomVariable;
	}

	private RandomVariable getRandomVariableInterfaceOfIndex(final int index){
		return getFunctionList().get(index).getRandomVariableInterface();
	}

	private int getFunctionIndex(){
		return ownIndexInList;
	}

	private int[] getParentIDs(){
		return parentIndices;
	}

	private ArrayList<Integer> getChildrenIndices(){
		if(childrenIndices == null) {
			childrenIndices = new ArrayList<>();
		}
		return childrenIndices;
	}

	private int getNumberOfParentVariables(){
		if(getParentIDs() == null) {
			return 0;
		}
		return getParentIDs().length;
	}

	private RandomVariableAAD getAADRandomVariableFromList(final int index){
		return getFunctionList().get(index);
	}

	private void addToChildrenIndices(final int index){
		getChildrenIndices().add(index);
	}

	/*--------------------------------------------------------------------------------------------------------------------------------------------------*/



	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#equals(net.finmath.stochastic.RandomVariable)
	 */
	@Override
	public boolean equals(final RandomVariable randomVariable) {
		return getRandomVariableInterface().equals(randomVariable);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#getFiltrationTime()
	 */
	@Override
	public double getFiltrationTime() {
		return getRandomVariableInterface().getFiltrationTime();
	}

	@Override
	public int getTypePriority() {
		return 3;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#get(int)
	 */
	@Override
	public double get(final int pathOrState) {
		return getRandomVariableInterface().get(pathOrState);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#size()
	 */
	@Override
	public int size() {
		return getRandomVariableInterface().size();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#isDeterministic()
	 */
	@Override
	public boolean isDeterministic() {
		return getRandomVariableInterface().isDeterministic();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#getRealizations()
	 */
	@Override
	public double[] getRealizations() {
		return getRandomVariableInterface().getRealizations();
	}

	@Override
	public Double doubleValue() {
		return getRandomVariableInterface().doubleValue();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#getMin()
	 */
	@Override
	public double getMin() {
		return ((RandomVariableAAD) getMinAsRandomVariableAAD()).getRandomVariableInterface().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#getMax()
	 */
	@Override
	public double getMax() {
		return ((RandomVariableAAD) getMaxAsRandomVariableAAD()).getRandomVariableInterface().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#getAverage()
	 */
	@Override
	public double getAverage() {
		return ((RandomVariableAAD) getAverageAsRandomVariableAAD()).getRandomVariableInterface().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#getAverage(net.finmath.stochastic.RandomVariable)
	 */
	@Override
	public double getAverage(final RandomVariable probabilities) {
		return ((RandomVariableAAD) getAverageAsRandomVariableAAD(probabilities)).getRandomVariableInterface().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#getVariance()
	 */
	@Override
	public double getVariance() {
		return ((RandomVariableAAD) getVarianceAsRandomVariableAAD()).getRandomVariableInterface().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#getVariance(net.finmath.stochastic.RandomVariable)
	 */
	@Override
	public double getVariance(final RandomVariable probabilities) {
		return ((RandomVariableAAD) getAverageAsRandomVariableAAD(probabilities)).getRandomVariableInterface().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#getSampleVariance()
	 */
	@Override
	public double getSampleVariance() {
		return ((RandomVariableAAD) getSampleVarianceAsRandomVariableAAD()).getRandomVariableInterface().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#getStandardDeviation()
	 */
	@Override
	public double getStandardDeviation() {
		return ((RandomVariableAAD) getStandardDeviationAsRandomVariableAAD()).getRandomVariableInterface().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#getStandardDeviation(net.finmath.stochastic.RandomVariable)
	 */
	@Override
	public double getStandardDeviation(final RandomVariable probabilities) {
		return ((RandomVariableAAD) getStandardDeviationAsRandomVariableAAD(probabilities)).getRandomVariableInterface().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#getStandardError()
	 */
	@Override
	public double getStandardError() {
		return ((RandomVariableAAD) getStandardErrorAsRandomVariableAAD()).getRandomVariableInterface().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#getStandardError(net.finmath.stochastic.RandomVariable)
	 */
	@Override
	public double getStandardError(final RandomVariable probabilities) {
		return ((RandomVariableAAD) getStandardErrorAsRandomVariableAAD(probabilities)).getRandomVariableInterface().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#getQuantile(double)
	 */
	@Override
	public double getQuantile(final double quantile) {
		return ((RandomVariableAAD) getRandomVariableInterface()).getRandomVariableInterface().getQuantile(quantile);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#getQuantile(double, net.finmath.stochastic.RandomVariable)
	 */
	@Override
	public double getQuantile(final double quantile, final RandomVariable probabilities) {
		return ((RandomVariableAAD) getRandomVariableInterface()).getRandomVariableInterface().getQuantile(quantile, probabilities);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#getQuantileExpectation(double, double)
	 */
	@Override
	public double getQuantileExpectation(final double quantileStart, final double quantileEnd) {
		return ((RandomVariableAAD) getRandomVariableInterface()).getRandomVariableInterface().getQuantileExpectation(quantileStart, quantileEnd);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#getHistogram(double[])
	 */
	@Override
	public double[] getHistogram(final double[] intervalPoints) {
		return getRandomVariableInterface().getHistogram(intervalPoints);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#getHistogram(int, double)
	 */
	@Override
	public double[][] getHistogram(final int numberOfPoints, final double standardDeviations) {
		return getRandomVariableInterface().getHistogram(numberOfPoints, standardDeviations);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#cache()
	 */
	@Override
	public RandomVariable cache() {
		return this;
	}

	@Override
	public RandomVariable cap(final double cap) {
		return apply(OperatorType.CAP, new RandomVariable[]{this, constructNewAADRandomVariable(cap)});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#floor(double)
	 */
	@Override
	public RandomVariable floor(final double floor) {
		return apply(OperatorType.FLOOR, new RandomVariable[]{this, constructNewAADRandomVariable(floor)});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#add(double)
	 */
	@Override
	public RandomVariable add(final double value) {
		return apply(OperatorType.ADD, new RandomVariable[]{this, constructNewAADRandomVariable(value)});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#sub(double)
	 */
	@Override
	public RandomVariable sub(final double value) {
		return apply(OperatorType.SUB, new RandomVariable[]{this, constructNewAADRandomVariable(value)});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#mult(double)
	 */
	@Override
	public RandomVariable mult(final double value) {
		return apply(OperatorType.MULT, new RandomVariable[]{this, constructNewAADRandomVariable(value)});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#div(double)
	 */
	@Override
	public RandomVariable div(final double value) {
		return apply(OperatorType.DIV, new RandomVariable[]{this, constructNewAADRandomVariable(value)});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#pow(double)
	 */
	@Override
	public RandomVariable pow(final double exponent) {
		return apply(OperatorType.POW, new RandomVariable[]{this, constructNewAADRandomVariable(exponent)});
	}

	@Override
	public RandomVariable average() {
		return apply(OperatorType.AVERAGE, new RandomVariable[]{this});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#squared()
	 */
	@Override
	public RandomVariable squared() {
		return apply(OperatorType.SQUARED, new RandomVariable[]{this});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#sqrt()
	 */
	@Override
	public RandomVariable sqrt() {
		return apply(OperatorType.SQRT, new RandomVariable[]{this});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#exp()
	 */
	@Override
	public RandomVariable exp() {
		return apply(OperatorType.EXP, new RandomVariable[]{this});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#log()
	 */
	@Override
	public RandomVariable log() {
		return apply(OperatorType.LOG, new RandomVariable[]{this});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#sin()
	 */
	@Override
	public RandomVariable sin() {
		return apply(OperatorType.SIN, new RandomVariable[]{this});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#cos()
	 */
	@Override
	public RandomVariable cos() {
		return apply(OperatorType.COS, new RandomVariable[]{this});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#add(net.finmath.stochastic.RandomVariable)
	 */
	@Override
	public RandomVariable add(final RandomVariable randomVariable) {
		return apply(OperatorType.ADD, new RandomVariable[]{this, randomVariable});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#sub(net.finmath.stochastic.RandomVariable)
	 */
	@Override
	public RandomVariable sub(final RandomVariable randomVariable) {
		return apply(OperatorType.SUB, new RandomVariable[]{this, randomVariable});
	}

	@Override
	public RandomVariable bus(final RandomVariable randomVariable) {
		return apply(OperatorType.SUB, new RandomVariable[]{randomVariable,this});	// SUB with swapped arguments
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#mult(net.finmath.stochastic.RandomVariable)
	 */
	@Override
	public RandomVariable mult(final RandomVariable randomVariable) {
		return apply(OperatorType.MULT, new RandomVariable[]{this, randomVariable});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#div(net.finmath.stochastic.RandomVariable)
	 */
	@Override
	public RandomVariable div(final RandomVariable randomVariable) {
		return apply(OperatorType.DIV, new RandomVariable[]{this, randomVariable});
	}

	@Override
	public RandomVariable vid(final RandomVariable randomVariable) {
		return apply(OperatorType.DIV, new RandomVariable[]{randomVariable, this}); // DIV with swapped arguments
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#cap(net.finmath.stochastic.RandomVariable)
	 */
	@Override
	public RandomVariable cap(final RandomVariable cap) {
		return apply(OperatorType.CAP, new RandomVariable[]{this, cap});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#floor(net.finmath.stochastic.RandomVariable)
	 */
	@Override
	public RandomVariable floor(final RandomVariable floor) {
		return apply(OperatorType.FLOOR, new RandomVariable[]{this, floor});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#accrue(net.finmath.stochastic.RandomVariable, double)
	 */
	@Override
	public RandomVariable accrue(final RandomVariable rate, final double periodLength) {
		return apply(OperatorType.ACCRUE, new RandomVariable[]{this, rate, constructNewAADRandomVariable(periodLength)});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#discount(net.finmath.stochastic.RandomVariable, double)
	 */
	@Override
	public RandomVariable discount(final RandomVariable rate, final double periodLength) {
		return apply(OperatorType.DISCOUNT, new RandomVariable[]{this, rate, constructNewAADRandomVariable(periodLength)});
	}

	@Override
	public RandomVariable choose(final RandomVariable valueIfTriggerNonNegative, final RandomVariable valueIfTriggerNegative) {
		return apply(OperatorType.BARRIER, new RandomVariable[]{this, valueIfTriggerNonNegative, valueIfTriggerNegative});
	}

	@Override
	public RandomVariable invert() {
		return apply(OperatorType.INVERT, new RandomVariable[]{this});
	}

	@Override
	public RandomVariable abs() {
		return apply(OperatorType.ABS, new RandomVariable[]{this});
	}

	@Override
	public RandomVariable addProduct(final RandomVariable factor1, final double factor2) {
		return apply(OperatorType.ADDPRODUCT, new RandomVariable[]{this, factor1, constructNewAADRandomVariable(factor2)});
	}

	@Override
	public RandomVariable addProduct(final RandomVariable factor1, final RandomVariable factor2) {
		return apply(OperatorType.ADDPRODUCT, new RandomVariable[]{this, factor1, factor2});
	}

	@Override
	public RandomVariable addRatio(final RandomVariable numerator, final RandomVariable denominator) {
		return apply(OperatorType.ADDRATIO, new RandomVariable[]{this, numerator, denominator});
	}

	@Override
	public RandomVariable subRatio(final RandomVariable numerator, final RandomVariable denominator) {
		return apply(OperatorType.SUBRATIO, new RandomVariable[]{this, numerator, denominator});
	}

	@Override
	public RandomVariable isNaN() {
		return getRandomVariableInterface().isNaN();
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
	public RandomVariable apply(final DoubleUnaryOperator operator) {
		throw new UnsupportedOperationException("Applying functions is not supported.");
	}

	@Override
	public RandomVariable apply(final DoubleBinaryOperator operator, final RandomVariable argument) {
		throw new UnsupportedOperationException("Applying functions is not supported.");
	}

	@Override
	public RandomVariable apply(final DoubleTernaryOperator operator, final RandomVariable argument1, final RandomVariable argument2) {
		throw new UnsupportedOperationException("Applying functions is not supported.");
	}
}
