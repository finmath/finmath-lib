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
 *
 */
public class RandomVariableAAD implements RandomVariableInterface {

	/**
	 * 
	 */
	
	/* static elements of the class are shared between all members */
	private static ArrayList<RandomVariableInterface> arrayListOfRandomVariables = new ArrayList<>();
	private static int indexOfNextRandomVariable = 0;
	private static enum OperatorType {
		ADD, MULT, DIV, SUB, SQUARED, SQRT, LOG, SIN, COS, EXP, INVERT, CAP, FLOOR, ABS, 
		ADDPRODUCT, ADDRATIO, SUBRATIO, BARRIER, DISCOUNT, ACCURUE, POW, AVERAGE, VARIANCE, STDEV
	}
	
	/* index of corresponding random variable in the static array list*/
	private final int indexOfRandomVariable;
	
	/* this could maybe be outsourced to own class ParentElement */
	private final RandomVariableAAD[] parentRandomVariables;
	private final OperatorType parentOperator;
	private boolean isConstant;
	
	/**
	 * @param indexOfRandomVariable
	 * @param parentRandomVariables
	 * @param parentOperator
	 * @param isConstant
	 */
	private RandomVariableAAD(int indexOfRandomVariable, RandomVariableAAD[] parentRandomVariables,
			OperatorType parentOperator, boolean isConstant) {
		super();
		this.indexOfRandomVariable = indexOfRandomVariable;
		this.parentRandomVariables = parentRandomVariables;
		this.parentOperator = parentOperator;
		this.isConstant = isConstant;
	}
	
	public void setIsConstantTo(boolean isConstant){
		this.isConstant = isConstant;
	}
	
	/**
	 * @param randomVariable
	 * @param parentRandomVariables
	 * @param parentOperator
	 * @param isConstant
	 */
	public static RandomVariableAAD constructNewAADRandomVariable(RandomVariableInterface randomVariable, RandomVariableAAD[] parentRandomVariables,
			OperatorType parentOperator, boolean isConstant){
		
		/* get index of this random variable */
		int indexOfThisRandomVariable = indexOfNextRandomVariable++;
		
		/* add random variable to static list for book keeping */
		arrayListOfRandomVariables.add(indexOfThisRandomVariable, randomVariable);
		
		/* return a new random variable */
		return new RandomVariableAAD(indexOfThisRandomVariable, parentRandomVariables, parentOperator, isConstant);
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
	
	private RandomVariableInterface getRandomVariable() {
		return arrayListOfRandomVariables.get(indexOfRandomVariable);
	}
	
	private RandomVariableInterface apply(OperatorType operator, RandomVariableInterface[] randomVariableInterfaces){
		
		RandomVariableAAD[] aadRandomVariables = new RandomVariableAAD[randomVariableInterfaces.length];
		
		/* TODO: implement with Array and .foreach(...) */
		for(int randomVariableIndex = 0; randomVariableIndex < randomVariableInterfaces.length; randomVariableIndex++){
			aadRandomVariables[randomVariableIndex] = (randomVariableInterfaces[randomVariableIndex] instanceof RandomVariableAAD) ?
					(RandomVariableAAD)randomVariableInterfaces[randomVariableIndex] : constructNewAADRandomVariable(randomVariableInterfaces[randomVariableIndex]);
			}
		
		RandomVariableInterface resultrandomvariable;
		
		switch(operator){
			/* functions with one argument  */
			case SQUARED:
				if(randomVariableInterfaces.length != 1) throw new IllegalArgumentException();
				resultrandomvariable = aadRandomVariables[0].getRandomVariable().squared();
				break;
			case SQRT:
				if(randomVariableInterfaces.length != 1) throw new IllegalArgumentException();
				resultrandomvariable = aadRandomVariables[0].getRandomVariable().sqrt();
				break;
			case EXP:
				if(randomVariableInterfaces.length != 1) throw new IllegalArgumentException();
				resultrandomvariable = aadRandomVariables[0].getRandomVariable().exp();
				break;
			case LOG:
				if(randomVariableInterfaces.length != 1) throw new IllegalArgumentException();
				resultrandomvariable = aadRandomVariables[0].getRandomVariable().log();
				break;
			case SIN:
				if(randomVariableInterfaces.length != 1) throw new IllegalArgumentException();
				resultrandomvariable = aadRandomVariables[0].getRandomVariable().sin();
				break;
			case COS:
				if(randomVariableInterfaces.length != 1) throw new IllegalArgumentException();
				resultrandomvariable = aadRandomVariables[0].getRandomVariable().cos();
				break;
			case ABS:
				if(randomVariableInterfaces.length != 1) throw new IllegalArgumentException();
				resultrandomvariable = aadRandomVariables[0].getRandomVariable().abs();
				break;
			case INVERT:
				if(randomVariableInterfaces.length != 1) throw new IllegalArgumentException();
				resultrandomvariable = aadRandomVariables[0].getRandomVariable().invert();
				break;
			case AVERAGE:
				if(randomVariableInterfaces.length != 1) throw new IllegalArgumentException();
				/* return a new deterministic random variable  */
				resultrandomvariable = new RandomVariable(aadRandomVariables[0].getRandomVariable().getAverage());
				break;
			case VARIANCE:
				if(randomVariableInterfaces.length != 1) throw new IllegalArgumentException();
				/* return a new deterministic random variable  */
				resultrandomvariable = new RandomVariable(aadRandomVariables[0].getRandomVariable().getVariance());
				break;
			case STDEV:
				if(randomVariableInterfaces.length != 1) throw new IllegalArgumentException();
				/* return a new deterministic random variable  */
				resultrandomvariable = new RandomVariable(aadRandomVariables[0].getRandomVariable().getStandardDeviation());
				break;
				
			/* functions with two arguments */
			case ADD:
				if(randomVariableInterfaces.length != 2) throw new IllegalArgumentException();
				resultrandomvariable = aadRandomVariables[0].getRandomVariable().add(aadRandomVariables[1].getRandomVariable());
				break;
			case SUB:
				if(randomVariableInterfaces.length != 2) throw new IllegalArgumentException();
				resultrandomvariable = aadRandomVariables[0].getRandomVariable().sub(aadRandomVariables[1].getRandomVariable());
				break;
			case MULT:
				if(randomVariableInterfaces.length != 2) throw new IllegalArgumentException();
				resultrandomvariable = aadRandomVariables[0].getRandomVariable().mult(aadRandomVariables[1].getRandomVariable());
				break;
			case DIV:
				if(randomVariableInterfaces.length != 2) throw new IllegalArgumentException();
				resultrandomvariable = aadRandomVariables[0].getRandomVariable().div(aadRandomVariables[1].getRandomVariable());
				break;
			case CAP:
				if(randomVariableInterfaces.length != 2) throw new IllegalArgumentException();
				resultrandomvariable = aadRandomVariables[0].getRandomVariable().cap(aadRandomVariables[1].getRandomVariable());
				break;
			case FLOOR:
				if(randomVariableInterfaces.length != 2) throw new IllegalArgumentException();
				resultrandomvariable = aadRandomVariables[0].getRandomVariable().floor(aadRandomVariables[1].getRandomVariable());
				break;			
			case POW:
				if(randomVariableInterfaces.length != 2) throw new IllegalArgumentException();
				resultrandomvariable = aadRandomVariables[0].getRandomVariable().pow(
						/* argument is deterministic anyway */ aadRandomVariables[1].getRandomVariable().getAverage());
				break;
				
			/* functions with three arguments */	
			case ADDPRODUCT:
				if(randomVariableInterfaces.length != 3) throw new IllegalArgumentException();
				resultrandomvariable = aadRandomVariables[0].getRandomVariable().addProduct(aadRandomVariables[1].getRandomVariable(), 
						aadRandomVariables[2].getRandomVariable());
				break;
			case ADDRATIO:
				if(randomVariableInterfaces.length != 3) throw new IllegalArgumentException();
				resultrandomvariable = aadRandomVariables[0].getRandomVariable().addRatio(aadRandomVariables[1].getRandomVariable(), 
						aadRandomVariables[2].getRandomVariable());
				break;
			case SUBRATIO:
				if(randomVariableInterfaces.length != 3) throw new IllegalArgumentException();
				resultrandomvariable = aadRandomVariables[0].getRandomVariable().subRatio(aadRandomVariables[1].getRandomVariable(), 
						aadRandomVariables[2].getRandomVariable());
				break;
			case ACCURUE:
				if(randomVariableInterfaces.length != 3) throw new IllegalArgumentException();
				resultrandomvariable = aadRandomVariables[0].getRandomVariable().accrue(aadRandomVariables[1].getRandomVariable(), 
						/* second argument is deterministic anyway */ aadRandomVariables[2].getRandomVariable().getAverage());
				break;
			case DISCOUNT:
				if(randomVariableInterfaces.length != 3) throw new IllegalArgumentException();
				resultrandomvariable = aadRandomVariables[0].getRandomVariable().discount(aadRandomVariables[1].getRandomVariable(), 
						/* second argument is deterministic anyway */ aadRandomVariables[2].getRandomVariable().getAverage());
				break;
				
			/* if non of the above throw exception */
			default:
				throw new IllegalArgumentException("Operation not supported!\n");
		}
		
		/* create new RandomVariableUniqueVariable which is definitely NOT Constant */
		return constructNewAADRandomVariable(resultrandomvariable, aadRandomVariables, operator, /*not constant*/ false);
	}
	
	private int[] getParentIDs() {
		
		if(parentRandomVariables == null) return null;
		
		int[] parentIDs = new int[parentRandomVariables.length];
		for(int parentVariableIndex = 0; parentVariableIndex < parentRandomVariables.length; parentVariableIndex++){
			parentIDs[parentVariableIndex] = parentRandomVariables[parentVariableIndex].indexOfRandomVariable;
		}
		return parentIDs;
	}
		
	private boolean isVariable() {
		return (isConstant == false && parentRandomVariables == null);
	}	
	
	public String toString(){
		return  super.toString() + "\n" + 
				"time: " + getFiltrationTime() + "\n" + 
				"realizations: " + Arrays.toString(getRealizations()) + "\n" + 
				"variableID: " + indexOfRandomVariable + "\n" +
				"parentIDs: " + Arrays.toString(getParentIDs()) + ((getParentIDs() == null) ? "" : (" type: " + parentOperator.name())) + "\n" +
				"isTrueVariable: " + isVariable() + "\n";
	}

	public static RandomVariableInterface getPartialDerivative(int functionIndex, int variableIndex){
		return ((RandomVariableAAD) arrayListOfRandomVariables.get(functionIndex)).partialDerivativeWithRespectTo(variableIndex);		
	}
	
	private RandomVariableInterface partialDerivativeWithRespectTo(int variableIndex){
		
		/* if random variable not dependent on variable or it is constant anyway return 0.0 */
		if(!Arrays.asList(getParentIDs()).contains(variableIndex) || isConstant) return new RandomVariable(0.0);
		
		RandomVariableInterface resultrandomvariable;
		
		switch(parentOperator){
		/* functions with one argument  */
		case SQUARED:
			resultrandomvariable = parentRandomVariables[0].getRandomVariable().mult(2.0);
			break;
		case SQRT:
			resultrandomvariable = parentRandomVariables[0].getRandomVariable().sqrt().invert().mult(0.5);
			break;
		case EXP:
			resultrandomvariable = parentRandomVariables[0].getRandomVariable().exp();
			break;
		case LOG:
			resultrandomvariable = parentRandomVariables[0].getRandomVariable().invert();
			break;
		case SIN:
			resultrandomvariable = parentRandomVariables[0].getRandomVariable().cos();
			break;
		case COS:
			resultrandomvariable = parentRandomVariables[0].getRandomVariable().sin().mult(-1.0);
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
				resultrandomvariable = parentRandomVariables[1].getRandomVariable();
			} else {
				resultrandomvariable = parentRandomVariables[0].getRandomVariable();
			}
			break;
		case DIV:
			if(variableIndex == getParentIDs()[0]){
				resultrandomvariable = parentRandomVariables[1].getRandomVariable().invert();
			} else {
				resultrandomvariable = parentRandomVariables[0].getRandomVariable().div(parentRandomVariables[1].getRandomVariable().squared());
			}
			break;
			
		/* if non of the above throw exception */
		default:
			throw new IllegalArgumentException("Operation not supported!\n");
		}
		
		return resultrandomvariable;
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
	 * @see net.finmath.stochastic.RandomVariableInterface#getOperator()
	 */
	@Override
	public IntToDoubleFunction getOperator() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getRealizationsStream()
	 */
	@Override
	public DoubleStream getRealizationsStream() {
		// TODO Auto-generated method stub
		return null;
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
		return ((RandomVariableAAD)apply(OperatorType.AVERAGE, new RandomVariableInterface[]{this})).getRandomVariable().getAverage();
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
		/* for AVERAGE apply returns a deterministic value anyway! Hence take .getAverage to access this value */
		return apply(OperatorType.VARIANCE, new RandomVariableInterface[]{this}).getAverage();
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
		/* for AVERAGE apply returns a deterministic value anyway! Hence take .getAverage to access this value */
		return apply(OperatorType.STDEV, new RandomVariableInterface[]{this}).getAverage();
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
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#apply(java.util.function.DoubleUnaryOperator)
	 */
	@Override
	public RandomVariableInterface apply(DoubleUnaryOperator operator) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#apply(java.util.function.DoubleBinaryOperator, net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public RandomVariableInterface apply(DoubleBinaryOperator operator, RandomVariableInterface argument) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#apply(net.finmath.functions.DoubleTernaryOperator, net.finmath.stochastic.RandomVariableInterface, net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public RandomVariableInterface apply(DoubleTernaryOperator operator, RandomVariableInterface argument1,
			RandomVariableInterface argument2) {
		// TODO Auto-generated method stub
		return null;
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
		return getRandomVariable().isNaN();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getMutableCopy()
	 */
	@Override
	public RandomVariableInterface getMutableCopy() {
		// TODO Auto-generated method stub
		return null;
	}

}
