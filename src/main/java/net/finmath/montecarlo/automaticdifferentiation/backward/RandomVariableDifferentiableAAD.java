/**
 * 
 */
package net.finmath.montecarlo.automaticdifferentiation.backward;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntToDoubleFunction;
import java.util.stream.DoubleStream;

import net.finmath.functions.DoubleTernaryOperator;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiableInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * Implementation of <code>RandomVariableDifferentiableInterface</code> using
 * the backward algorithmic differentiation (adjoint algorithmic differentiation, AAD).
 * 
 * @author Christian Fries
 * @author Stefan Sedlmair
 * @version 1.0
 */
public class RandomVariableDifferentiableAAD implements RandomVariableDifferentiableInterface {

	private static final long serialVersionUID = 2459373647785530657L;

	private static AtomicLong indexOfNextRandomVariable = new AtomicLong(0);
	private static enum OperatorType {
		ADD, MULT, DIV, SUB, SQUARED, SQRT, LOG, SIN, COS, EXP, INVERT, CAP, FLOOR, ABS, 
		ADDPRODUCT, ADDRATIO, SUBRATIO, BARRIER, DISCOUNT, ACCURUE, POW, MIN, MAX, AVERAGE, VARIANCE, 
		STDEV, STDERROR, SVARIANCE, AVERAGE2, VARIANCE2, 
		STDEV2, STDERROR2
	}

	private final OperatorType operator;
	private final List<RandomVariableInterface> arguments;
	private final RandomVariableInterface values;
	private final Long id;

	public static RandomVariableDifferentiableAAD of(double value) {
		return new RandomVariableDifferentiableAAD(value);
	}

	public static RandomVariableDifferentiableAAD of(RandomVariableInterface randomVariable) {
		return new RandomVariableDifferentiableAAD(randomVariable);
	}

	public RandomVariableDifferentiableAAD(double value) {
		this(new RandomVariable(value), null, null);
	}

	public RandomVariableDifferentiableAAD(double time, double[] realisations) {
		this(new RandomVariable(time, realisations), null, null);
	}

	public RandomVariableDifferentiableAAD(RandomVariableInterface randomVariable) {
		this(randomVariable, null, null);
	}

	private RandomVariableDifferentiableAAD(RandomVariableInterface values, List<RandomVariableInterface> arguments, OperatorType parentOperator) {
		super();
		this.id = indexOfNextRandomVariable.getAndIncrement();
		this.values = values;
		this.arguments = arguments;
		this.operator = parentOperator;
	}

	public RandomVariableInterface getRandomVariable() {
		return values;
	}

	private static RandomVariableInterface valuesOf(RandomVariableInterface randomVariable) {
		if(randomVariable instanceof RandomVariableDifferentiableAAD) return ((RandomVariableDifferentiableAAD) randomVariable).getRandomVariable();
		else return randomVariable;
	}

	private RandomVariableInterface apply(OperatorType operator, RandomVariableInterface[] randomVariableInterfaces){

		// Construct argument list
		ArrayList<RandomVariableInterface> arguments = new ArrayList<RandomVariableInterface>();
		for(RandomVariableInterface randomVariable : randomVariableInterfaces) {
			arguments.add(randomVariable);
		}

		// Calculate values
		RandomVariableInterface result;
		RandomVariableInterface X = null,Y = null,Z = null;

		if(arguments.size() > 0) X = valuesOf(arguments.get(0));
		if(arguments.size() > 1) Y = valuesOf(arguments.get(1));
		if(arguments.size() > 2) Z = valuesOf(arguments.get(2));

		switch(operator){
		case SQUARED:
			result = X.squared();
			break;
		case SQRT:
			result = X.sqrt();
			break;
		case EXP:
			result = X.exp();
			break;
		case LOG:
			result = X.log();
			break;
		case SIN:
			result = X.sin();
			break;
		case COS:
			result = X.cos();
			break;
		case ABS:
			result = X.abs();
			break;
		case INVERT:
			result = X.invert();
			break;
		case AVERAGE:
			result = new RandomVariable(X.getAverage());
			break;
		case STDERROR:
			result = new RandomVariable(X.getStandardError());
			break;
		case STDEV:
			result = new RandomVariable(X.getStandardDeviation());
			break;
		case VARIANCE:
			result = new RandomVariable(X.getVariance());
			break;
		case SVARIANCE:
			result = new RandomVariable(X.getSampleVariance());
			break;
		case ADD:
			result = X.add(Y);
			break;
		case SUB:
			result = X.sub(Y);
			break;
		case MULT:
			result = X.mult(Y);
			break;
		case DIV:
			result = X.div(Y);
			break;
		case CAP:
			result = X.cap(Y);
			break;
		case FLOOR:
			result = X.floor(Y);
			break;			
		case POW:
			result = X.pow( /* argument is deterministic random variable */ Y.getAverage());
			break;
		case AVERAGE2:
			result = new RandomVariable(X.getAverage(Y));
			break;
		case STDERROR2:
			result = new RandomVariable(X.getStandardError(Y));
			break;
		case STDEV2:
			result = new RandomVariable(X.getStandardDeviation(Y));
			break;
		case VARIANCE2:
			result = new RandomVariable(X.getVariance(Y));
			break;
		case ADDPRODUCT:
			result = X.addProduct(Y,Z);
			break;
		case ADDRATIO:
			result = X.addRatio(Y, Z);
			break;
		case SUBRATIO:
			result = X.subRatio(Y, Z);
			break;
		case ACCURUE:
			result = X.accrue(Y, /* second argument is deterministic anyway */ Z.getAverage());
			break;
		case DISCOUNT:
			result = X.discount(Y, /* second argument is deterministic anyway */ Z.getAverage());
			break;
		default:
			throw new IllegalArgumentException("Operation not supported!\n");
		}

		/* create new RandomVariableUniqueVariable which is definitely NOT Constant */
		RandomVariableDifferentiableAAD newRandomVariableAAD = new RandomVariableDifferentiableAAD(result, arguments, operator);

		/* return new RandomVariable */
		return newRandomVariableAAD;
	}

	private RandomVariableInterface getPartialDerivative(RandomVariableDifferentiableAAD argumentAAD){

		if(!arguments.contains(argumentAAD)) return new RandomVariable(0.0);

		int differentialIndex = getArguments().indexOf(argumentAAD);
		RandomVariableInterface X = getArguments().size() > 0 ? valuesOf(arguments.get(0)) : null;
		RandomVariableInterface Y = getArguments().size() > 1 ? valuesOf(arguments.get(1)) : null;
		RandomVariableInterface Z = getArguments().size() > 2 ? valuesOf(arguments.get(2)) : null;

		RandomVariableInterface resultrandomvariable = null;

		switch(operator) {
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
			break;
		case MAX:
			double max = X.getMax();
			resultrandomvariable = X.apply(x -> (x == max) ? 1.0 : 0.0);
			break;
		case ABS:
			resultrandomvariable = X.barrier(X, new RandomVariable(1.0), new RandomVariable(-1.0));
			break;
		case STDERROR:
			resultrandomvariable = X.sub(X.getAverage()*(2.0*X.size()-1.0)/X.size()).mult(2.0/X.size()).mult(0.5).div(Math.sqrt(X.getVariance() * X.size()));
			break;
		case SVARIANCE:
			resultrandomvariable = X.sub(X.getAverage()*(2.0*X.size()-1.0)/X.size()).mult(2.0/(X.size()-1));
			break;
		case ADD:
			resultrandomvariable = new RandomVariable(1.0);
			break;
		case SUB:
			resultrandomvariable = new RandomVariable(differentialIndex == 0 ? 1.0 : -1.0);
			break;
		case MULT:
			resultrandomvariable = differentialIndex == 0 ? Y : X;
			break;
		case DIV:
			resultrandomvariable = differentialIndex == 0 ? Y.invert() : X.div(Y.squared());
			break;
		case CAP:
			if(differentialIndex == 0) {
				resultrandomvariable = X.barrier(X.sub(Y), new RandomVariable(0.0), new RandomVariable(1.0));
			}
			else {
				resultrandomvariable = X.barrier(X.sub(Y), new RandomVariable(1.0), new RandomVariable(0.0));
			}
			break;
		case FLOOR:
			if(differentialIndex == 0) {
				resultrandomvariable = X.barrier(X.sub(Y), new RandomVariable(1.0), new RandomVariable(0.0));
			}
			else {
				resultrandomvariable = X.barrier(X.sub(Y), new RandomVariable(0.0), new RandomVariable(1.0));
			}
			break;
		case AVERAGE2:
			resultrandomvariable = differentialIndex == 0 ? Y : X;
			break;
		case VARIANCE2:
			resultrandomvariable = differentialIndex == 0 ? Y.mult(2.0).mult(X.mult(Y.add(X.getAverage(Y)*(X.size()-1)).sub(X.getAverage(Y)))) :
				X.mult(2.0).mult(Y.mult(X.add(Y.getAverage(X)*(X.size()-1)).sub(Y.getAverage(X))));
			break;
		case STDEV2:		
			resultrandomvariable = differentialIndex == 0 ? Y.mult(2.0).mult(X.mult(Y.add(X.getAverage(Y)*(X.size()-1)).sub(X.getAverage(Y)))).div(Math.sqrt(X.getVariance(Y))) :
				X.mult(2.0).mult(Y.mult(X.add(Y.getAverage(X)*(X.size()-1)).sub(Y.getAverage(X)))).div(Math.sqrt(Y.getVariance(X)));
			break;
		case STDERROR2:				
			resultrandomvariable = differentialIndex == 0 ? Y.mult(2.0).mult(X.mult(Y.add(X.getAverage(Y)*(X.size()-1)).sub(X.getAverage(Y)))).div(Math.sqrt(X.getVariance(Y) * X.size())) :
				X.mult(2.0).mult(Y.mult(X.add(Y.getAverage(X)*(X.size()-1)).sub(Y.getAverage(X)))).div(Math.sqrt(Y.getVariance(X) * Y.size()));
			break;
		case POW:
			/* second argument will always be deterministic and constant! */
			resultrandomvariable = (differentialIndex == 0) ? Y.mult(X.pow(Y.getAverage() - 1.0)) : new RandomVariable(0.0);
		case ADDPRODUCT:
			if(differentialIndex == 0) {
				resultrandomvariable = new RandomVariable(1.0);
			} else if(differentialIndex == 1) {
				resultrandomvariable = Z;
			} else {
				resultrandomvariable = Y;
			}
			break;
		case ADDRATIO:
			if(differentialIndex == 0) {
				resultrandomvariable = new RandomVariable(1.0);
			} else if(differentialIndex == 1) {
				resultrandomvariable = Z.invert();
			} else {
				resultrandomvariable = Y.div(Z.squared());
			}
			break;
		case SUBRATIO:
			if(differentialIndex == 0) {
				resultrandomvariable = new RandomVariable(1.0);
			} else if(differentialIndex == 1) {
				resultrandomvariable = Z.invert().mult(-1.0);
			} else {
				resultrandomvariable = Y.div(Z.squared()).mult(-1.0);
			}
			break;
		case ACCURUE:
			if(differentialIndex == 0) {
				resultrandomvariable = Y.mult(Z).add(1.0);
			} else if(differentialIndex == 1) {
				resultrandomvariable = X.mult(Z);
			} else {
				resultrandomvariable = X.mult(Y);
			}
			break;
		case DISCOUNT:
			if(differentialIndex == 0) {
				resultrandomvariable = Y.mult(Z).add(1.0).invert();
			} else if(differentialIndex == 1) {
				resultrandomvariable = X.mult(Z).div(Y.mult(Z).add(1.0).squared());
			} else {
				resultrandomvariable = X.mult(Y).div(Y.mult(Z).add(1.0).squared());
			}
			break;
		case BARRIER:
			if(differentialIndex == 0) {
				resultrandomvariable = X.apply(x -> (x == 0.0) ? Double.POSITIVE_INFINITY : 0.0);
			} else if(differentialIndex == 1) {
				resultrandomvariable = X.barrier(X, new RandomVariable(1.0), new RandomVariable(0.0));
			} else {
				resultrandomvariable = X.barrier(X, new RandomVariable(0.0), new RandomVariable(1.0));
			}
		default:
			break;
		}

		return resultrandomvariable;
	}

	public Long getID(){
		return id;
	}

	public Map<Long, RandomVariableInterface> getGradient() {

		// The map maintaining the derivatives id -> derivative
		Map<Long, RandomVariableInterface> derivatives = new HashMap<Long, RandomVariableInterface>();

		// Put derivative of this node w.r.t. itself
		derivatives.put(getID(), new RandomVariable(1.0));

		// The set maintaining the independents. Note: TreeMap is maintaining a sort on the keys.
		TreeMap<Long, RandomVariableDifferentiableAAD> independents = new TreeMap<Long, RandomVariableDifferentiableAAD>();
		independents.put(getID(), this);

		while(independents.size() > 0) {
			// Process node with the highest id in independents
			Map.Entry<Long, RandomVariableDifferentiableAAD> independentEntry = independents.lastEntry();
			Long id = independentEntry.getKey();
			RandomVariableDifferentiableAAD independent = independentEntry.getValue();

			// Get arguments of this node and propagate derivative to arguments
			List<RandomVariableInterface> arguments = independent.getArguments();
			if(arguments != null && arguments.size() > 0) {
				independent.propagateDerivativesFromResultToArgument(derivatives);

				// Add all non constant arguments to the list of independents
				for(RandomVariableInterface argument : arguments) {
					if(argument instanceof RandomVariableDifferentiableAAD) {
						Long argumentId = (long) ((RandomVariableDifferentiableAAD) argument).getID();
						independents.put(argumentId, (RandomVariableDifferentiableAAD) argument);
					}
				}

				// Remove id from derivatives - keep only leaf nodes.
				derivatives.remove(id);
			}

			// Done with processing. Remove from map.
			independents.remove(id);
		}

		return derivatives;
	}

	private List<RandomVariableInterface> getArguments() {
		return arguments;
	}

	private void propagateDerivativesFromResultToArgument(Map<Long, RandomVariableInterface> derivatives) {

		for(RandomVariableInterface argument : getArguments()) {
			if(argument instanceof RandomVariableDifferentiableAAD) {
				RandomVariableDifferentiableAAD argumentAAD = (RandomVariableDifferentiableAAD)argument;
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

	public RandomVariableInterface getAverageAsRandomVariableAAD(RandomVariableInterface probabilities) {
		/*returns deterministic AAD random variable */
		return apply(OperatorType.AVERAGE2, new RandomVariableInterface[]{this, probabilities});
	}

	public RandomVariableInterface getVarianceAsRandomVariableAAD(RandomVariableInterface probabilities){
		/*returns deterministic AAD random variable */
		return apply(OperatorType.VARIANCE2, new RandomVariableInterface[]{this, probabilities});
	}

	public RandomVariableInterface 	getStandardDeviationAsRandomVariableAAD(RandomVariableInterface probabilities){
		/*returns deterministic AAD random variable */
		return apply(OperatorType.STDEV2, new RandomVariableInterface[]{this, probabilities});
	}

	public RandomVariableInterface 	getStandardErrorAsRandomVariableAAD(RandomVariableInterface probabilities){
		/*returns deterministic AAD random variable */
		return apply(OperatorType.STDERROR2, new RandomVariableInterface[]{this, probabilities});
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

	private RandomVariableInterface getValues(){
		return values;
	}

	/*--------------------------------------------------------------------------------------------------------------------------------------------------*/



	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#equals(net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public boolean equals(RandomVariableInterface randomVariable) {
		return getValues().equals(randomVariable);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getFiltrationTime()
	 */
	@Override
	public double getFiltrationTime() {
		return getValues().getFiltrationTime();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#get(int)
	 */
	@Override
	public double get(int pathOrState) {
		return getValues().get(pathOrState);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#size()
	 */
	@Override
	public int size() {
		return getValues().size();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#isDeterministic()
	 */
	@Override
	public boolean isDeterministic() {
		return getValues().isDeterministic();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getRealizations()
	 */
	@Override
	public double[] getRealizations() {
		return getValues().getRealizations();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getRealizations(int)
	 */
	@Override
	public double[] getRealizations(int numberOfPaths) {
		return getValues().getRealizations(numberOfPaths);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getMin()
	 */
	@Override
	public double getMin() {
		return getValues().getMin();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getMax()
	 */
	@Override
	public double getMax() {
		return getValues().getMax();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getAverage()
	 */
	@Override
	public double getAverage() {		
		return getValues().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getAverage(net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public double getAverage(RandomVariableInterface probabilities) {
		return ((RandomVariableDifferentiableAAD) getAverageAsRandomVariableAAD(probabilities)).getValues().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getVariance()
	 */
	@Override
	public double getVariance() {
		return ((RandomVariableDifferentiableAAD) getVarianceAsRandomVariableAAD()).getValues().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getVariance(net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public double getVariance(RandomVariableInterface probabilities) {
		return ((RandomVariableDifferentiableAAD) getAverageAsRandomVariableAAD(probabilities)).getValues().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getSampleVariance()
	 */
	@Override
	public double getSampleVariance() {
		return ((RandomVariableDifferentiableAAD) getSampleVarianceAsRandomVariableAAD()).getValues().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getStandardDeviation()
	 */
	@Override
	public double getStandardDeviation() {
		return ((RandomVariableDifferentiableAAD) getStandardDeviationAsRandomVariableAAD()).getValues().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getStandardDeviation(net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public double getStandardDeviation(RandomVariableInterface probabilities) {
		return ((RandomVariableDifferentiableAAD) getStandardDeviationAsRandomVariableAAD(probabilities)).getValues().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getStandardError()
	 */
	@Override
	public double getStandardError() {
		return ((RandomVariableDifferentiableAAD) getStandardErrorAsRandomVariableAAD()).getValues().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getStandardError(net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public double getStandardError(RandomVariableInterface probabilities) {
		return ((RandomVariableDifferentiableAAD) getStandardErrorAsRandomVariableAAD(probabilities)).getValues().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getQuantile(double)
	 */
	@Override
	public double getQuantile(double quantile) {
		return ((RandomVariableDifferentiableAAD) getValues()).getValues().getQuantile(quantile);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getQuantile(double, net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public double getQuantile(double quantile, RandomVariableInterface probabilities) {
		return ((RandomVariableDifferentiableAAD) getValues()).getValues().getQuantile(quantile, probabilities);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getQuantileExpectation(double, double)
	 */
	@Override
	public double getQuantileExpectation(double quantileStart, double quantileEnd) {
		return ((RandomVariableDifferentiableAAD) getValues()).getValues().getQuantileExpectation(quantileStart, quantileEnd);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getHistogram(double[])
	 */
	@Override
	public double[] getHistogram(double[] intervalPoints) {
		return getValues().getHistogram(intervalPoints);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getHistogram(int, double)
	 */
	@Override
	public double[][] getHistogram(int numberOfPoints, double standardDeviations) {
		return getValues().getHistogram(numberOfPoints, standardDeviations);
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
		return new RandomVariableDifferentiableAAD(
				getValues().cap(cap),
				Arrays.asList(new RandomVariableInterface[]{ this, new RandomVariable(cap) }),
				OperatorType.CAP);
	}

	@Override
	public RandomVariableInterface floor(double floor) {
		return new RandomVariableDifferentiableAAD(
				getValues().floor(floor),
				Arrays.asList(new RandomVariableInterface[]{ this, new RandomVariable(floor) }),
				OperatorType.FLOOR);
	}

	@Override
	public RandomVariableInterface add(double value) {
		return new RandomVariableDifferentiableAAD(
				getValues().add(value),
				Arrays.asList(new RandomVariableInterface[]{ this, new RandomVariable(value) }),
				OperatorType.ADD);
	}

	@Override
	public RandomVariableInterface sub(double value) {
		return new RandomVariableDifferentiableAAD(
				getValues().sub(value),
				Arrays.asList(new RandomVariableInterface[]{ this, new RandomVariable(value) }),
				OperatorType.SUB);
	}

	@Override
	public RandomVariableInterface mult(double value) {
		return apply(OperatorType.MULT, new RandomVariableInterface[]{this, new RandomVariable(value)});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#div(double)
	 */
	@Override
	public RandomVariableInterface div(double value) {
		return apply(OperatorType.DIV, new RandomVariableInterface[]{this, new RandomVariable(value)});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#pow(double)
	 */
	@Override
	public RandomVariableInterface pow(double exponent) {
		return apply(OperatorType.POW, new RandomVariableInterface[]{this, new RandomVariable(exponent)});
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
		return new RandomVariableDifferentiableAAD(
				getValues().add(randomVariable),
				Arrays.asList(new RandomVariableInterface[]{ this, randomVariable }),
				OperatorType.ADD);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#sub(net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public RandomVariableInterface sub(RandomVariableInterface randomVariable) {
		return new RandomVariableDifferentiableAAD(
				getValues().sub(randomVariable),
				Arrays.asList(new RandomVariableInterface[]{ this, randomVariable }),
				OperatorType.SUB);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#mult(net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public RandomVariableDifferentiableInterface mult(RandomVariableInterface randomVariable) {
		return new RandomVariableDifferentiableAAD(
				getValues().mult(randomVariable),
				Arrays.asList(new RandomVariableInterface[]{ this, randomVariable }),
				OperatorType.MULT);
	}

	@Override
	public RandomVariableInterface div(RandomVariableInterface randomVariable) {
		return apply(OperatorType.DIV, new RandomVariableInterface[]{this, randomVariable});
	}

	@Override
	public RandomVariableInterface cap(RandomVariableInterface cap) {
		return new RandomVariableDifferentiableAAD(
				getValues().cap(cap),
				Arrays.asList(new RandomVariableInterface[]{ this, cap }),
				OperatorType.CAP);
	}

	@Override
	public RandomVariableInterface floor(RandomVariableInterface floor) {
		return new RandomVariableDifferentiableAAD(
				getValues().cap(floor),
				Arrays.asList(new RandomVariableInterface[]{ this, floor }),
				OperatorType.FLOOR);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#accrue(net.finmath.stochastic.RandomVariableInterface, double)
	 */
	@Override
	public RandomVariableInterface accrue(RandomVariableInterface rate, double periodLength) {
		return apply(OperatorType.ACCURUE, new RandomVariableInterface[]{this, rate, new RandomVariable(periodLength)});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#discount(net.finmath.stochastic.RandomVariableInterface, double)
	 */
	@Override
	public RandomVariableInterface discount(RandomVariableInterface rate, double periodLength) {
		return apply(OperatorType.DISCOUNT, new RandomVariableInterface[]{this, rate, new RandomVariable(periodLength)});
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
		return apply(OperatorType.BARRIER, new RandomVariableInterface[]{this, valueIfTriggerNonNegative, new RandomVariable(valueIfTriggerNegative)});
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
		return apply(OperatorType.ADDPRODUCT, new RandomVariableInterface[]{this, factor1, new RandomVariable(factor2)});
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
		return getValues().isNaN();
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
		return getValues().getOperator();
	}

	@Override
	public DoubleStream getRealizationsStream() {
		return getValues().getRealizationsStream();
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
