/**
 * 
 */
package net.finmath.montecarlo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntToDoubleFunction;
import java.util.stream.DoubleStream;

import net.finmath.functions.DoubleTernaryOperator;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * Implementation of <code>RandomVariableInterface</code> having the additional feature to calculate the backward algorithmic differentiation.
 * 
 * For construction use the factory method <code>constructNewAADRandomVariable</code>.
 *
 * @author Stefan Sedlmair
 * @version 1.0
 */
public class RandomVariableAADv2 implements RandomVariableInterface {

	private static final long serialVersionUID = 2459373647785530657L;
	
	private static AtomicInteger randomVariableUID = new AtomicInteger(0);

	/* static elements of the class are shared between all members */
	private static enum OperatorType {
		ADD, MULT, DIV, SUB, SQUARED, SQRT, LOG, SIN, COS, EXP, INVERT, CAP, FLOOR, ABS, 
		ADDPRODUCT, ADDRATIO, SUBRATIO, BARRIER, DISCOUNT, ACCURUE, POW, AVERAGE, VARIANCE, 
		STDEV, MIN, MAX, STDERROR, SVARIANCE
	}

	/* index of corresponding random variable in the static array list*/
	private final RandomVariableInterface ownRandomVariable;
	private final int ownRandomVariableUID;

	/* this could maybe be outsourced to own class ParentElement */
	private final RandomVariableAADv2[] parentRandomVariables;
	private final OperatorType parentOperator;
	private ArrayList<RandomVariableAADv2> childRandomVariables;
	private boolean isConstant;

	public RandomVariableAADv2(RandomVariableInterface ownRandomVariable, RandomVariableAADv2[] parentRandomVariables, OperatorType parentOperator, 
			ArrayList<RandomVariableAADv2> childRandomVariables ,boolean isConstant) {
		super();
		this.ownRandomVariable 		= ownRandomVariable;
		this.parentRandomVariables 	= parentRandomVariables;
		this.parentOperator 		= parentOperator;
		this.childRandomVariables 	= childRandomVariables;
		this.isConstant 			= isConstant;
		
		this.ownRandomVariableUID 	= randomVariableUID.getAndIncrement();
	}

	public RandomVariableAADv2(RandomVariableInterface ownRandomVariable) {
		super();
		this.ownRandomVariable 		= ownRandomVariable;
		this.parentRandomVariables 	= null;
		this.parentOperator 		= null;
		this.childRandomVariables 	= new ArrayList<RandomVariableAADv2>();
		this.isConstant 			= false;
		
		this.ownRandomVariableUID 	= randomVariableUID.getAndIncrement();
	}
	
	public RandomVariableAADv2(double time, double[] values) {
		super();
		this.ownRandomVariable 		= new RandomVariable(time, values);
		this.parentRandomVariables 	= null;
		this.parentOperator 		= null;
		this.childRandomVariables 	= new ArrayList<RandomVariableAADv2>();
		this.isConstant 			= false;
		
		this.ownRandomVariableUID 	= randomVariableUID.getAndIncrement();
	}
	
	public RandomVariableAADv2(double value) {
		super();
		this.ownRandomVariable 		= new RandomVariable(value);
		this.parentRandomVariables 	= null;
		this.parentOperator 		= null;
		this.childRandomVariables 	= new ArrayList<RandomVariableAADv2>();
		this.isConstant 			= false;
		
		this.ownRandomVariableUID 	= randomVariableUID.getAndIncrement();
	}

	private RandomVariableInterface apply(OperatorType operator, RandomVariableInterface[] randomVariableInterfaces){

		RandomVariableAADv2[] aadRandomVariables = new RandomVariableAADv2[randomVariableInterfaces.length];
		
		/* convert all non-AAD arguments to instances of this class (non-AAD arguments will be considered constant!)*/
		for(int randomVariableIndex = 0; randomVariableIndex < randomVariableInterfaces.length; randomVariableIndex++){
			aadRandomVariables[randomVariableIndex] = (randomVariableInterfaces[randomVariableIndex] instanceof RandomVariableAADv2) ?
					(RandomVariableAADv2)randomVariableInterfaces[randomVariableIndex] : 
						new RandomVariableAADv2(randomVariableInterfaces[randomVariableIndex]){{setIsConstantTo(true);}};
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
		
		/* create new RandomVariableAADv2 which is definitely NOT Constant */
		RandomVariableAADv2 newRandomVariableAAD = new RandomVariableAADv2(resultrandomvariable, aadRandomVariables, operator,
				/*no children*/ new ArrayList<RandomVariableAADv2>() ,/*not constant*/ false);
	
		/* add new variable as child to its parents */
		for(RandomVariableAADv2 parentRandomVariable:aadRandomVariables) 
			parentRandomVariable.addChildToRandomVariableAADv2s(newRandomVariableAAD);
		
		/* return new RandomVariable */
		return newRandomVariableAAD;
	}

	public String toString(){
		return  super.toString() + "\n" + 
				"time:              " + getFiltrationTime() + "\n" + 
				"realizations:      " + Arrays.toString(getRealizations()) + "\n" + 
				"randomVariableUID: " + getRandomVariableUID() + "\n" +
				"parentIDs:         " + Arrays.toString(getParentRandomVariableUIDs()) + ((getParentRandomVariableAADv2s() == null) ? "" : (" type: " + parentOperator.name())) + "\n" +
				"isTrueVariable:    " + isVariable() + "\n";
	}

	private RandomVariableInterface partialDerivativeWithRespectTo(int variableIndex){

		boolean parentsContainVariable = false;
		for(RandomVariableAADv2 parentRandomVariableAADv2:getParentRandomVariableAADv2s()){
			if(parentRandomVariableAADv2.getRandomVariableUID() == variableIndex){
				parentsContainVariable = true;
				break;
			}
		}
		
		/* if random variable not dependent on variable or it is constant anyway return 0.0 */
		if(!parentsContainVariable || isConstant) return new RandomVariable(0.0);

		RandomVariableInterface resultrandomvariable = null;
		RandomVariableInterface X,Y,Z;
		double[] resultRandomVariableRealizations;
		
		if(getParentRandomVariableAADv2s().length == 1){
			
			X = getParentRandomVariableAADv2s()[0].getRandomVariableInterface().getMutableCopy();
		
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
		} else if(getParentRandomVariableAADv2s().length == 2){
			
			X = getParentRandomVariableAADv2s()[0].getRandomVariableInterface().getMutableCopy();
			Y = getParentRandomVariableAADv2s()[1].getRandomVariableInterface().getMutableCopy();
			boolean isFirstArgument = (getParentRandomVariableUIDs()[0] == variableIndex);
			
			switch(parentOperator){
			case ADD:
				resultrandomvariable = new RandomVariable(1.0);
				break;
			case SUB:
				resultrandomvariable = new RandomVariable(isFirstArgument ? 1.0 : -1.0);
				break;
			case MULT:
				resultrandomvariable = isFirstArgument ? Y : X;
				break;
			case DIV:
				resultrandomvariable = isFirstArgument ? Y.invert() : X.div(Y.squared());
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
				resultrandomvariable = isFirstArgument ? Y : X;
				break;
			case VARIANCE:
				resultrandomvariable = isFirstArgument ? Y.mult(2.0).mult(X.mult(Y.add(X.getAverage(Y)*(X.size()-1)).sub(X.getAverage(Y)))) :
					X.mult(2.0).mult(Y.mult(X.add(Y.getAverage(X)*(X.size()-1)).sub(Y.getAverage(X))));
				break;
			case STDEV:				
				resultrandomvariable = isFirstArgument ? Y.mult(2.0).mult(X.mult(Y.add(X.getAverage(Y)*(X.size()-1)).sub(X.getAverage(Y)))).div(Math.sqrt(X.getVariance(Y))) :
				X.mult(2.0).mult(Y.mult(X.add(Y.getAverage(X)*(X.size()-1)).sub(Y.getAverage(X)))).div(Math.sqrt(Y.getVariance(X)));
				break;
			case STDERROR:				
				resultrandomvariable = isFirstArgument ? Y.mult(2.0).mult(X.mult(Y.add(X.getAverage(Y)*(X.size()-1)).sub(X.getAverage(Y)))).div(Math.sqrt(X.getVariance(Y) * X.size())) :
				X.mult(2.0).mult(Y.mult(X.add(Y.getAverage(X)*(X.size()-1)).sub(Y.getAverage(X)))).div(Math.sqrt(Y.getVariance(X) * Y.size()));
				break;
			case POW:
				/* second argument will always be deterministic and constant! */
				resultrandomvariable = isFirstArgument ? Y.mult(X.pow(Y.getAverage() - 1.0)) : new RandomVariable(0.0);
			default:
				break;
			}
		} else if(getParentRandomVariableAADv2s().length == 3){ 
			X = getParentRandomVariableAADv2s()[0].getRandomVariableInterface().getMutableCopy();
			Y = getParentRandomVariableAADv2s()[1].getRandomVariableInterface().getMutableCopy();
			Z = getParentRandomVariableAADv2s()[2].getRandomVariableInterface().getMutableCopy();

			boolean isFirstArgument = (getParentRandomVariableUIDs()[0] == variableIndex);
			boolean isSecondArgument = (getParentRandomVariableUIDs()[1] == variableIndex);

			
			switch(parentOperator){
			case ADDPRODUCT:
				if(isFirstArgument){
					resultrandomvariable = new RandomVariable(1.0);
				} else if(isSecondArgument){
					resultrandomvariable = Z;
				} else {
					resultrandomvariable = Y;
				}
				break;
			case ADDRATIO:
				if(isFirstArgument){
					resultrandomvariable = new RandomVariable(1.0);
				} else if(isSecondArgument){
					resultrandomvariable = Z.invert();
				} else {
					resultrandomvariable = Y.div(Z.squared());
				}
				break;
			case SUBRATIO:
				if(isFirstArgument){
					resultrandomvariable = new RandomVariable(1.0);
				} else if(isSecondArgument){
					resultrandomvariable = Z.invert().mult(-1.0);
				} else {
					resultrandomvariable = Y.div(Z.squared()).mult(-1.0);
				}
				break;
			case ACCURUE:
				if(isFirstArgument){
					resultrandomvariable = Y.mult(Z).add(1.0);
				} else if(isSecondArgument){
					resultrandomvariable = X.mult(Z);
				} else {
					resultrandomvariable = X.mult(Y);
				}
				break;
			case DISCOUNT:
				if(isFirstArgument){
					resultrandomvariable = Y.mult(Z).add(1.0).invert();
				} else if(isSecondArgument){
					resultrandomvariable = X.mult(Z).div(Y.mult(Z).add(1.0).squared());
				} else {
					resultrandomvariable = X.mult(Y).div(Y.mult(Z).add(1.0).squared());
				}
				break;
			case BARRIER:
				if(isFirstArgument){
					resultrandomvariable = X.apply(x -> (x == 0.0) ? Double.POSITIVE_INFINITY : 0.0);
				} else if(isSecondArgument){
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

		/* get dependence tree */
		Map<Integer, RandomVariableAADv2> getMapOfDependentRandomVariables = mapAllDependentRandomVariableAADv2s();
		
		/* key set is indicating in which order random variables were generated */
		Set<Integer> keySet = getMapOfDependentRandomVariables.keySet();
		
		int numberOfDependentRandomVariables = keySet.size();

		/* catch trivial case here */
		if(numberOfDependentRandomVariables == 1) 
			return new HashMap<Integer, RandomVariableInterface>()
				{{put(getRandomVariableUID(), new RandomVariable(getFiltrationTime(), isConstant() ? 0.0 : 1.0));}};
		
		
		Integer[] idsOfDependentRandomVariables = new Integer[numberOfDependentRandomVariables];
		idsOfDependentRandomVariables = keySet.toArray(idsOfDependentRandomVariables);
		
		/*sorts array in ascending ordering */
		Arrays.sort(idsOfDependentRandomVariables);
		
		/*_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_*/
		
		Map<Integer, RandomVariableInterface> gradient = new HashMap<Integer, RandomVariableInterface>();
		Map<Integer, RandomVariableInterface> omegaHat = new HashMap<Integer, RandomVariableInterface>();

		/* first entry (with highest variable UID) of omegaHat is set to 1.0 */
		int variableIndex = idsOfDependentRandomVariables[numberOfDependentRandomVariables-1];
		
		omegaHat.put(variableIndex, new RandomVariable(1.0));
		
		if(getMapOfDependentRandomVariables.get(variableIndex).isVariable())
			gradient.put(variableIndex, omegaHat.get(variableIndex));
		
		/* now  */
		for(int i = numberOfDependentRandomVariables-2; i >= 0; i--){
			variableIndex = idsOfDependentRandomVariables[i];
			RandomVariableInterface newOmegaHatEntry = new RandomVariable(0.0);
			
			for(int functionIndex : getMapOfDependentRandomVariables.get(variableIndex).getChildrenUIDs()){
				RandomVariableInterface D_i_j = getMapOfDependentRandomVariables.get(functionIndex).partialDerivativeWithRespectTo(variableIndex);
				newOmegaHatEntry = newOmegaHatEntry.addProduct(D_i_j, omegaHat.get(functionIndex));
			}
			
			if(getMapOfDependentRandomVariables.get(variableIndex).isVariable())
				gradient.put(variableIndex, newOmegaHatEntry);
			
			omegaHat.put(variableIndex, newOmegaHatEntry);
		}

		return gradient;
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
		return (isConstant() == false && getParentRandomVariableAADv2s() == null);
	}	

	public void setIsConstantTo(boolean isConstant){
		this.isConstant = isConstant;
	}

	private RandomVariableInterface getRandomVariableInterface(){
		return ownRandomVariable;
	}

	private RandomVariableAADv2[] getParentRandomVariableAADv2s(){
		return parentRandomVariables;
	}
	
	private ArrayList<RandomVariableAADv2> getChildrenRandomVariableAADv2s(){
		return childRandomVariables;
	}
	
	private void addChildToRandomVariableAADv2s(RandomVariableAADv2 child){
		getChildrenRandomVariableAADv2s().add(child);
	}
	
	private int getRandomVariableUID() {
		return ownRandomVariableUID;
	}
	
	private int[] getParentRandomVariableUIDs(){
		int[] parentUIDs = new int[getParentRandomVariableAADv2s().length];
		
		for(int i = 0; i < parentUIDs.length; i++)
			parentUIDs[i] = getParentRandomVariableAADv2s()[i].getRandomVariableUID();
		
		return parentUIDs;
	}
	
	private int[] getChildrenUIDs(){
		int[] childrenUIDs = new int[getChildrenRandomVariableAADv2s().size()];
		
		for(int i = 0; i < childrenUIDs.length; i++)
			childrenUIDs[i] = getChildrenRandomVariableAADv2s().get(i).getRandomVariableUID();
		
		return childrenUIDs;
	}
	
	
	/* get the dependence tree for a instance of RandomVariableAADv2 */
	private Map<Integer, RandomVariableAADv2> mapAllDependentRandomVariableAADv2s(){
		Map<Integer, RandomVariableAADv2> mapOfDependenRandomVariableAADv2s = new HashMap<Integer, RandomVariableAADv2>();
		
		/* add the variable it self */
		if(!mapOfDependenRandomVariableAADv2s.containsKey(getRandomVariableUID())) mapOfDependenRandomVariableAADv2s.put(getRandomVariableUID(), this);
		
		if(getParentRandomVariableAADv2s() != null){
			for(RandomVariableAADv2 parentRandomVariableAADv2:getParentRandomVariableAADv2s())
				mapOfDependenRandomVariableAADv2s.putAll(parentRandomVariableAADv2.mapAllDependentRandomVariableAADv2s());
		}
		
		return mapOfDependenRandomVariableAADv2s;
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
		return ((RandomVariableAADv2) getMinAsRandomVariableAAD()).getRandomVariableInterface().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getMax()
	 */
	@Override
	public double getMax() {
		return ((RandomVariableAADv2) getMaxAsRandomVariableAAD()).getRandomVariableInterface().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getAverage()
	 */
	@Override
	public double getAverage() {		
		return ((RandomVariableAADv2) getAverageAsRandomVariableAAD()).getRandomVariableInterface().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getAverage(net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public double getAverage(RandomVariableInterface probabilities) {
		return ((RandomVariableAADv2) getAverageAsRandomVariableAAD(probabilities)).getRandomVariableInterface().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getVariance()
	 */
	@Override
	public double getVariance() {
		return ((RandomVariableAADv2) getVarianceAsRandomVariableAAD()).getRandomVariableInterface().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getVariance(net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public double getVariance(RandomVariableInterface probabilities) {
		return ((RandomVariableAADv2) getAverageAsRandomVariableAAD(probabilities)).getRandomVariableInterface().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getSampleVariance()
	 */
	@Override
	public double getSampleVariance() {
		return ((RandomVariableAADv2) getSampleVarianceAsRandomVariableAAD()).getRandomVariableInterface().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getStandardDeviation()
	 */
	@Override
	public double getStandardDeviation() {
		return ((RandomVariableAADv2) getStandardDeviationAsRandomVariableAAD()).getRandomVariableInterface().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getStandardDeviation(net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public double getStandardDeviation(RandomVariableInterface probabilities) {
		return ((RandomVariableAADv2) getStandardDeviationAsRandomVariableAAD(probabilities)).getRandomVariableInterface().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getStandardError()
	 */
	@Override
	public double getStandardError() {
		return ((RandomVariableAADv2) getStandardErrorAsRandomVariableAAD()).getRandomVariableInterface().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getStandardError(net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public double getStandardError(RandomVariableInterface probabilities) {
		return ((RandomVariableAADv2) getStandardErrorAsRandomVariableAAD(probabilities)).getRandomVariableInterface().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getQuantile(double)
	 */
	@Override
	public double getQuantile(double quantile) {
		return ((RandomVariableAADv2) getRandomVariableInterface()).getRandomVariableInterface().getQuantile(quantile);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getQuantile(double, net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public double getQuantile(double quantile, RandomVariableInterface probabilities) {
		return ((RandomVariableAADv2) getRandomVariableInterface()).getRandomVariableInterface().getQuantile(quantile, probabilities);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getQuantileExpectation(double, double)
	 */
	@Override
	public double getQuantileExpectation(double quantileStart, double quantileEnd) {
		return ((RandomVariableAADv2) getRandomVariableInterface()).getRandomVariableInterface().getQuantileExpectation(quantileStart, quantileEnd);
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
		return apply(OperatorType.CAP, new RandomVariableInterface[]{this, new RandomVariableAADv2(cap)});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#floor(double)
	 */
	@Override
	public RandomVariableInterface floor(double floor) {
		return apply(OperatorType.FLOOR, new RandomVariableInterface[]{this, new RandomVariableAADv2(floor)});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#add(double)
	 */
	@Override
	public RandomVariableInterface add(double value) {
		return apply(OperatorType.ADD, new RandomVariableInterface[]{this, new RandomVariableAADv2(value)});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#sub(double)
	 */
	@Override
	public RandomVariableInterface sub(double value) {
		return apply(OperatorType.SUB, new RandomVariableInterface[]{this, new RandomVariableAADv2(value)});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#mult(double)
	 */
	@Override
	public RandomVariableInterface mult(double value) {
		return apply(OperatorType.MULT, new RandomVariableInterface[]{this, new RandomVariableAADv2(value)});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#div(double)
	 */
	@Override
	public RandomVariableInterface div(double value) {
		return apply(OperatorType.DIV, new RandomVariableInterface[]{this, new RandomVariableAADv2(value)});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#pow(double)
	 */
	@Override
	public RandomVariableInterface pow(double exponent) {
		return apply(OperatorType.POW, new RandomVariableInterface[]{this, new RandomVariableAADv2(exponent)});
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
		return apply(OperatorType.ACCURUE, new RandomVariableInterface[]{this, rate, new RandomVariableAADv2(periodLength)});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#discount(net.finmath.stochastic.RandomVariableInterface, double)
	 */
	@Override
	public RandomVariableInterface discount(RandomVariableInterface rate, double periodLength) {
		return apply(OperatorType.DISCOUNT, new RandomVariableInterface[]{this, rate, new RandomVariableAADv2(periodLength)});
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
		return apply(OperatorType.BARRIER, new RandomVariableInterface[]{this, valueIfTriggerNonNegative, new RandomVariableAADv2(valueIfTriggerNegative)});
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
		return apply(OperatorType.ADDPRODUCT, new RandomVariableInterface[]{this, factor1, new RandomVariableAADv2(factor2)});
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
