/**
 * 
 */
package net.finmath.montecarlo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntToDoubleFunction;
import java.util.stream.DoubleStream;

import org.hamcrest.core.IsInstanceOf;

import net.finmath.functions.DoubleTernaryOperator;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * Implementation of <code>RandomVariableInterface</code> having the additional feature to calculate the backward algorithmic differentiation.
 * 
 * @author Christian Fries
 * @author Stefan Sedlmair
 * @version 1.0
 */
public class RandomVariableAAD2 implements RandomVariableInterface {

	private static final long serialVersionUID = 2459373647785530657L;

	/* static elements of the class are shared between all members */
	private static AtomicLong indexOfNextRandomVariable = new AtomicLong(0);
	private static enum OperatorType {
		ADD, MULT, DIV, SUB, SQUARED, SQRT, LOG, SIN, COS, EXP, INVERT, CAP, FLOOR, ABS, 
		ADDPRODUCT, ADDRATIO, SUBRATIO, BARRIER, DISCOUNT, ACCURUE, POW, AVERAGE, VARIANCE, 
		STDEV, MIN, MAX, STDERROR, SVARIANCE
	}

	private final OperatorType operator;
	private ArrayList<RandomVariableInterface> arguments = new ArrayList<>();
	private final RandomVariableInterface values;
	private final Long id;

	/**
	 * @param indexOfRandomVariable
	 * @param parentRandomVariables
	 * @param parentOperator
	 * @param isConstant
	 */
	private RandomVariableAAD2(Long id, RandomVariableInterface values, 
			ArrayList<RandomVariableInterface> arguments, OperatorType parentOperator) {
		super();
		this.id = id;
		this.values = values;
		this.arguments = arguments;
		this.operator = parentOperator;
	}



	/**
	 * @param randomVariable
	 * @param arguments
	 * @param operator
	 * @return A new RandomVariableAAD.
	 */
	public static RandomVariableAAD2 constructNewAADRandomVariable(RandomVariableInterface randomVariable, ArrayList<RandomVariableInterface> arguments,
			OperatorType operator){

		/* get index of this random variable */
		Long indexOfThisAADRandomVariable = indexOfNextRandomVariable.getAndIncrement();

		RandomVariableAAD2 newAADRandomVariable = new RandomVariableAAD2(indexOfThisAADRandomVariable, randomVariable, 
				arguments, operator);

		/* return a new random variable */
		return newAADRandomVariable;
	}

	public static RandomVariableAAD2 constructNewAADRandomVariable(double value){
		return constructNewAADRandomVariable(new RandomVariable(value), /*parentRandomVariables*/ null, /*parentOperator*/ null);
	}

	public static RandomVariableAAD2 constructNewAADRandomVariable(RandomVariableInterface randomVariable) {
		return constructNewAADRandomVariable(randomVariable, /* no parents*/ null,
				/*no parent operator*/ null);
	}

	public static RandomVariableAAD2 constructNewAADRandomVariable(double time, double[] realisations) {
		return constructNewAADRandomVariable(new RandomVariable(time, realisations), /* no parents*/ null,
				/*no parent operator*/ null);
	}

	public RandomVariableInterface getRandomVariable() {
		return values;
	}

	private RandomVariableInterface apply(OperatorType operator, RandomVariableInterface[] randomVariableInterfaces){

		ArrayList<RandomVariableInterface> arguments = new ArrayList<RandomVariableInterface>();
		for(RandomVariableInterface randomVariable : randomVariableInterfaces) {
			arguments.add(randomVariable);
		}

		RandomVariableInterface resultrandomvariable;
		RandomVariableInterface X,Y,Z;

		if(randomVariableInterfaces.length == 1){

			X = arguments.get(0);
			if(X instanceof RandomVariableAAD2) X = ((RandomVariableAAD2)X).getRandomVariable();

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

			X = arguments.get(0);
			if(X instanceof RandomVariableAAD2) X = ((RandomVariableAAD2)X).getRandomVariable();
			Y = arguments.get(1);
			if(Y instanceof RandomVariableAAD2) Y = ((RandomVariableAAD2)Y).getRandomVariable();

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

			X = arguments.get(0);
			if(X instanceof RandomVariableAAD2) X = ((RandomVariableAAD2)X).getRandomVariable();
			Y = arguments.get(1);
			if(Y instanceof RandomVariableAAD2) Y = ((RandomVariableAAD2)Y).getRandomVariable();
			Z = arguments.get(2);
			if(Z instanceof RandomVariableAAD2) Z = ((RandomVariableAAD2)Z).getRandomVariable();

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
			case ACCURUE:
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
		RandomVariableAAD2 newRandomVariableAAD =  constructNewAADRandomVariable(resultrandomvariable, arguments, operator);

		/* return new RandomVariable */
		return newRandomVariableAAD;
	}

	private RandomVariableInterface getPartialDerivative(RandomVariableAAD2 argumentAAD){

		if(!arguments.contains(argumentAAD)) return new RandomVariable(0.0);

		RandomVariableInterface resultrandomvariable = null;
		RandomVariableInterface X,Y,Z;

		if(getArguments().size() == 1){

			X = getArguments().get(0);
			if(X instanceof RandomVariableAAD2) X = ((RandomVariableAAD2)X).getRandomVariable();

			switch(operator){
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
				double min = X.getMin();
				resultrandomvariable = X.apply(x -> (x == min) ? 1.0 : 0.0);
				//				resultRandomVariableRealizations = new double[X.size()];
				//				for(int i = 0; i < X.size(); i++) resultRandomVariableRealizations[i] = (X.getRealizations()[i] == X.getMin()) ? 1.0 : 0.0;
				//				resultrandomvariable = new RandomVariable(X.getFiltrationTime(), resultRandomVariableRealizations);
				break;
			case MAX:
				double max = X.getMax();
				resultrandomvariable = X.apply(x -> (x == max) ? 1.0 : 0.0);
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
		} else if(getArguments().size() == 2){

			X = getArguments().get(0);
			if(X instanceof RandomVariableAAD2) X = ((RandomVariableAAD2)X).getRandomVariable();
			Y = getArguments().get(1);
			if(Y instanceof RandomVariableAAD2) Y = ((RandomVariableAAD2)Y).getRandomVariable();

			switch(operator){
			case ADD:
				resultrandomvariable = new RandomVariable(1.0);
				break;
			case SUB:
				resultrandomvariable = new RandomVariable((argumentAAD.equals(X)) ? 1.0 : -1.0);
				break;
			case MULT:
				resultrandomvariable = (argumentAAD.equals(X)) ? Y : X;
				break;
			case DIV:
				resultrandomvariable = (argumentAAD.equals(X)) ? Y.invert() : X.div(Y.squared());
				break;
			case CAP:
				// @TODO: Dummy implementation - wrong for stochastic arguments? Fix me!
				double cap = Y.getAverage();
				resultrandomvariable = X.apply(x -> (x > cap) ? 0.0 : 1.0);
				//				resultRandomVariableRealizations = new double[X.size()];
				//				for(int i = 0; i < X.size(); i++) resultRandomVariableRealizations[i] = (X.getRealizations()[i] > Y.getAverage()) ? 0.0 : 1.0;
				//				resultrandomvariable = new RandomVariable(X.getFiltrationTime(), resultRandomVariableRealizations);
				break;
			case FLOOR:
				// @TODO: Dummy implementation - wrong for stochastic arguments? Fix me!
				double floor = Y.getAverage();
				resultrandomvariable = X.apply(x -> (x > floor) ? 1.0 : 0.0);
				//				resultRandomVariableRealizations = new double[X.size()];
				//				for(int i = 0; i < X.size(); i++) resultRandomVariableRealizations[i] = (X.getRealizations()[i] > Y.getAverage()) ? 1.0 : 0.0;
				//				resultrandomvariable = new RandomVariable(X.getFiltrationTime(), resultRandomVariableRealizations);
				break;
			case AVERAGE:
				resultrandomvariable = (argumentAAD.equals(X)) ? Y : X;
				break;
			case VARIANCE:
				resultrandomvariable = (argumentAAD.equals(X)) ? Y.mult(2.0).mult(X.mult(Y.add(X.getAverage(Y)*(X.size()-1)).sub(X.getAverage(Y)))) :
					X.mult(2.0).mult(Y.mult(X.add(Y.getAverage(X)*(X.size()-1)).sub(Y.getAverage(X))));
				break;
			case STDEV:				
				resultrandomvariable = (argumentAAD.equals(X)) ? Y.mult(2.0).mult(X.mult(Y.add(X.getAverage(Y)*(X.size()-1)).sub(X.getAverage(Y)))).div(Math.sqrt(X.getVariance(Y))) :
					X.mult(2.0).mult(Y.mult(X.add(Y.getAverage(X)*(X.size()-1)).sub(Y.getAverage(X)))).div(Math.sqrt(Y.getVariance(X)));
				break;
			case STDERROR:				
				resultrandomvariable = (argumentAAD.equals(X)) ? Y.mult(2.0).mult(X.mult(Y.add(X.getAverage(Y)*(X.size()-1)).sub(X.getAverage(Y)))).div(Math.sqrt(X.getVariance(Y) * X.size())) :
					X.mult(2.0).mult(Y.mult(X.add(Y.getAverage(X)*(X.size()-1)).sub(Y.getAverage(X)))).div(Math.sqrt(Y.getVariance(X) * Y.size()));
				break;
			case POW:
				/* second argument will always be deterministic and constant! */
				resultrandomvariable = (argumentAAD.equals(X)) ? Y.mult(X.pow(Y.getAverage() - 1.0)) : new RandomVariable(0.0);
			default:
				break;
			}
		} else if(getArguments().size() == 3){ 
			X = getArguments().get(0);
			if(X instanceof RandomVariableAAD2) X = ((RandomVariableAAD2)X).getRandomVariable();
			Y = getArguments().get(1);
			if(Y instanceof RandomVariableAAD2) Y = ((RandomVariableAAD2)Y).getRandomVariable();
			Z = getArguments().get(2);
			if(Z instanceof RandomVariableAAD2) Z = ((RandomVariableAAD2)Z).getRandomVariable();

			switch(operator){
			case ADDPRODUCT:
				if(argumentAAD.equals(X)){
					resultrandomvariable = new RandomVariable(1.0);
				} else if(argumentAAD.equals(Y)){
					resultrandomvariable = Z;
				} else {
					resultrandomvariable = Y;
				}
				break;
			case ADDRATIO:
				if(argumentAAD.equals(X)){
					resultrandomvariable = new RandomVariable(1.0);
				} else if(argumentAAD.equals(Y)){
					resultrandomvariable = Z.invert();
				} else {
					resultrandomvariable = Y.div(Z.squared());
				}
				break;
			case SUBRATIO:
				if(argumentAAD.equals(X)){
					resultrandomvariable = new RandomVariable(1.0);
				} else if(argumentAAD.equals(Y)){
					resultrandomvariable = Z.invert().mult(-1.0);
				} else {
					resultrandomvariable = Y.div(Z.squared()).mult(-1.0);
				}
				break;
			case ACCURUE:
				if(argumentAAD.equals(X)){
					resultrandomvariable = Y.mult(Z).add(1.0);
				} else if(argumentAAD.equals(Y)){
					resultrandomvariable = X.mult(Z);
				} else {
					resultrandomvariable = X.mult(Y);
				}
				break;
			case DISCOUNT:
				if(argumentAAD.equals(X)){
					resultrandomvariable = Y.mult(Z).add(1.0).invert();
				} else if(argumentAAD.equals(Y)){
					resultrandomvariable = X.mult(Z).div(Y.mult(Z).add(1.0).squared());
				} else {
					resultrandomvariable = X.mult(Y).div(Y.mult(Z).add(1.0).squared());
				}
				break;
			case BARRIER:
				if(argumentAAD.equals(X)){
					resultrandomvariable = X.apply(x -> (x == 0.0) ? Double.POSITIVE_INFINITY : 0.0);
				} else if(argumentAAD.equals(Y)){
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
	public Map<Long, RandomVariableInterface> getGradient(){

		// The map maintaining the derivatives id -> derivative
		Map<Long, RandomVariableInterface> derivatives = new HashMap<Long, RandomVariableInterface>();

		// Put derivative of this node w.r.t. itself
		derivatives.put(getID(), new RandomVariable(1.0));

		// The set maintaining the independets. This needs to be sorted
		TreeMap<Long, RandomVariableAAD2> independents = new TreeMap<Long, RandomVariableAAD2>();
		independents.put(getID(), this);

		while(independents.size() > 0) {
			Map.Entry<Long, RandomVariableAAD2> independentEntry = independents.lastEntry();
			Long id = independentEntry.getKey();
			RandomVariableAAD2 independent = independentEntry.getValue();
			ArrayList<RandomVariableInterface> arguments = independent.getArguments();

			if(arguments != null && arguments.size() > 0) {
				independent.propagateDerivativesFromResultToArgument(derivatives);

				for(RandomVariableInterface argument : arguments) {
					if(argument instanceof RandomVariableAAD2) {
						Long argumentId = (long) ((RandomVariableAAD2) argument).getID();
						independents.put(argumentId, (RandomVariableAAD2) argument);
					}
				}
			}
			independents.remove(id);

		}

		return derivatives;
	}

	/**
	 * @return
	 */
	private ArrayList<RandomVariableInterface> getArguments() {
		return arguments;
	}



	private void propagateDerivativesFromResultToArgument(Map<Long, RandomVariableInterface> derivatives) {

		for(RandomVariableInterface argument : getArguments()) {
			if(argument instanceof RandomVariableAAD2) {
				RandomVariableAAD2 argumentAAD = (RandomVariableAAD2)argument;
				Long argumentID = argumentAAD.getID();
				if(!derivatives.containsKey(argumentID)) derivatives.put(argumentID, new RandomVariable(0.0));

				RandomVariableInterface partialDerivative = getPartialDerivative(argumentAAD);
				RandomVariableInterface derivative			= derivatives.get(this.getID());
				RandomVariableInterface argumentDerivative	= derivatives.get(argumentID);
				
				argumentDerivative = argumentDerivative.addProduct(partialDerivative, derivative);
				
				derivatives.put(argumentID, argumentDerivative);
			}
		}
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

	private RandomVariableInterface getRandomVariableInterface(){
		return values;
	}

	private Long getID(){
		return id;
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
		return ((RandomVariableAAD2) getMinAsRandomVariableAAD()).getRandomVariableInterface().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getMax()
	 */
	@Override
	public double getMax() {
		return ((RandomVariableAAD2) getMaxAsRandomVariableAAD()).getRandomVariableInterface().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getAverage()
	 */
	@Override
	public double getAverage() {		
		return ((RandomVariableAAD2) getAverageAsRandomVariableAAD()).getRandomVariableInterface().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getAverage(net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public double getAverage(RandomVariableInterface probabilities) {
		return ((RandomVariableAAD2) getAverageAsRandomVariableAAD(probabilities)).getRandomVariableInterface().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getVariance()
	 */
	@Override
	public double getVariance() {
		return ((RandomVariableAAD2) getVarianceAsRandomVariableAAD()).getRandomVariableInterface().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getVariance(net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public double getVariance(RandomVariableInterface probabilities) {
		return ((RandomVariableAAD2) getAverageAsRandomVariableAAD(probabilities)).getRandomVariableInterface().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getSampleVariance()
	 */
	@Override
	public double getSampleVariance() {
		return ((RandomVariableAAD2) getSampleVarianceAsRandomVariableAAD()).getRandomVariableInterface().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getStandardDeviation()
	 */
	@Override
	public double getStandardDeviation() {
		return ((RandomVariableAAD2) getStandardDeviationAsRandomVariableAAD()).getRandomVariableInterface().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getStandardDeviation(net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public double getStandardDeviation(RandomVariableInterface probabilities) {
		return ((RandomVariableAAD2) getStandardDeviationAsRandomVariableAAD(probabilities)).getRandomVariableInterface().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getStandardError()
	 */
	@Override
	public double getStandardError() {
		return ((RandomVariableAAD2) getStandardErrorAsRandomVariableAAD()).getRandomVariableInterface().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getStandardError(net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public double getStandardError(RandomVariableInterface probabilities) {
		return ((RandomVariableAAD2) getStandardErrorAsRandomVariableAAD(probabilities)).getRandomVariableInterface().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getQuantile(double)
	 */
	@Override
	public double getQuantile(double quantile) {
		return ((RandomVariableAAD2) getRandomVariableInterface()).getRandomVariableInterface().getQuantile(quantile);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getQuantile(double, net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public double getQuantile(double quantile, RandomVariableInterface probabilities) {
		return ((RandomVariableAAD2) getRandomVariableInterface()).getRandomVariableInterface().getQuantile(quantile, probabilities);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getQuantileExpectation(double, double)
	 */
	@Override
	public double getQuantileExpectation(double quantileStart, double quantileEnd) {
		return ((RandomVariableAAD2) getRandomVariableInterface()).getRandomVariableInterface().getQuantileExpectation(quantileStart, quantileEnd);
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
