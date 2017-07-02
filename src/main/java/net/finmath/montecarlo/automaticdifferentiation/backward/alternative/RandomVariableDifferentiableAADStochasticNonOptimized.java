/**
 * 
 */
package net.finmath.montecarlo.automaticdifferentiation.backward.alternative;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntToDoubleFunction;
import java.util.stream.Collectors;
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
public class RandomVariableDifferentiableAADStochasticNonOptimized implements RandomVariableDifferentiableInterface {

	private static final long serialVersionUID = 2459373647785530657L;

	private static AtomicLong indexOfNextRandomVariable = new AtomicLong(0);

	private static enum OperatorType {
		ADD, MULT, DIV, SUB, SQUARED, SQRT, LOG, SIN, COS, EXP, INVERT, CAP, FLOOR, ABS, 
		ADDPRODUCT, ADDRATIO, SUBRATIO, BARRIER, DISCOUNT, ACCRUE, POW, MIN, MAX, AVERAGE, VARIANCE, 
		STDEV, STDERROR, SVARIANCE, AVERAGE2, VARIANCE2, 
		STDEV2, STDERROR2
	}

	private static class OperatorTreeNode {
		private final Long id;
		private final OperatorType operator;
		private final List<OperatorTreeNode> arguments;
		private final List<RandomVariableInterface> argumentValues;

		public OperatorTreeNode(OperatorType operator, List<RandomVariableInterface> arguments) {
			this(operator,
					arguments != null ? arguments.stream().map((RandomVariableInterface x) -> {
						return (x != null && x instanceof RandomVariableDifferentiableAADStochasticNonOptimized) ? ((RandomVariableDifferentiableAADStochasticNonOptimized)x).getOperatorTreeNode(): null;
					}).collect(Collectors.toList()) : null,
							arguments != null ? arguments.stream().map((RandomVariableInterface x) -> {
						return (x != null && x instanceof RandomVariableDifferentiableAADStochasticNonOptimized) ? ((RandomVariableDifferentiableAADStochasticNonOptimized)x).getValues() : x;
					}).collect(Collectors.toList()) : null
					);

		}
		public OperatorTreeNode(OperatorType operator, List<OperatorTreeNode> arguments, List<RandomVariableInterface> argumentValues) {
			super();
			this.id = indexOfNextRandomVariable.getAndIncrement();
			this.operator = operator;
			this.arguments = arguments;
			this.argumentValues = argumentValues;
		}
		
		private void propagateDerivativesFromResultToArgument(Map<Long, RandomVariableInterface> derivatives) {

			for(OperatorTreeNode argument : arguments) {
				if(argument != null) {
					Long argumentID = argument.id;
					if(!derivatives.containsKey(argumentID)) derivatives.put(argumentID, new RandomVariable(0.0));

					RandomVariableInterface partialDerivative	= getPartialDerivative(argument);
					RandomVariableInterface derivative			= derivatives.get(id);
					RandomVariableInterface argumentDerivative	= derivatives.get(argumentID);

					argumentDerivative = argumentDerivative.addProduct(partialDerivative, derivative);

					derivatives.put(argumentID, argumentDerivative);
				}
			}
		}

		private RandomVariableInterface getPartialDerivative(OperatorTreeNode differential){

			if(!arguments.contains(differential)) return new RandomVariable(0.0);

			int differentialIndex = arguments.indexOf(differential);
			RandomVariableInterface X = arguments.size() > 0 && argumentValues != null ? argumentValues.get(0) : null;
			RandomVariableInterface Y = arguments.size() > 1 && argumentValues != null ? argumentValues.get(1) : null;
			RandomVariableInterface Z = arguments.size() > 2 && argumentValues != null ? argumentValues.get(2) : null;

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
				break;
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
			case ACCRUE:
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
	}

	private final RandomVariableInterface values;
	private final OperatorTreeNode operatorTreeNode;

	public static RandomVariableDifferentiableAADStochasticNonOptimized of(double value) {
		return new RandomVariableDifferentiableAADStochasticNonOptimized(value);
	}

	public static RandomVariableDifferentiableAADStochasticNonOptimized of(RandomVariableInterface randomVariable) {
		return new RandomVariableDifferentiableAADStochasticNonOptimized(randomVariable);
	}

	public RandomVariableDifferentiableAADStochasticNonOptimized(double value) {
		this(new RandomVariable(value), null, null);
	}

	public RandomVariableDifferentiableAADStochasticNonOptimized(double time, double[] realisations) {
		this(new RandomVariable(time, realisations), null, null);
	}

	public RandomVariableDifferentiableAADStochasticNonOptimized(RandomVariableInterface randomVariable) {
		this(randomVariable, null, null);
	}

	private RandomVariableDifferentiableAADStochasticNonOptimized(RandomVariableInterface values, List<RandomVariableInterface> arguments, OperatorType operator) {
		super();
		this.values = values;
		this.operatorTreeNode = new OperatorTreeNode(operator, arguments);
	}

	public RandomVariableInterface getRandomVariable() {
		return values;
	}

	public OperatorTreeNode getOperatorTreeNode() {
		return operatorTreeNode;
	}

	public Long getID(){
		return getOperatorTreeNode().id;
	}

	public Map<Long, RandomVariableInterface> getGradient() {

		// The map maintaining the derivatives id -> derivative
		Map<Long, RandomVariableInterface> derivatives = new HashMap<Long, RandomVariableInterface>();

		// Put derivative of this node w.r.t. itself
		derivatives.put(getID(), new RandomVariable(1.0));

		// The set maintaining the independents. Note: TreeMap is maintaining a sort on the keys.
		TreeMap<Long, OperatorTreeNode> independents = new TreeMap<Long, OperatorTreeNode>();
		independents.put(getID(), getOperatorTreeNode());

		while(independents.size() > 0) {
			// Process node with the highest id in independents
			Map.Entry<Long, OperatorTreeNode> independentEntry = independents.lastEntry();
			Long id = independentEntry.getKey();
			OperatorTreeNode independent = independentEntry.getValue();

			// Get arguments of this node and propagate derivative to arguments
			List<OperatorTreeNode> arguments = independent.arguments;
			if(arguments != null && arguments.size() > 0) {
				independent.propagateDerivativesFromResultToArgument(derivatives);

				// Add all non constant arguments to the list of independents
				for(OperatorTreeNode argument : arguments) {
					if(argument != null) {
						Long argumentId = argument.id;
						independents.put(argumentId, argument);
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

	/* for all functions that need to be differentiated and are returned as double in the Interface, write a method to return it as RandomVariableAAD 
	 * that is deterministic by its nature. For their double-returning pendant just return the average of the deterministic RandomVariableAAD  */

	public RandomVariableInterface getAverageAsRandomVariableAAD(RandomVariableInterface probabilities) {
		/*returns deterministic AAD random variable */
		return new RandomVariableDifferentiableAADStochasticNonOptimized(
				new RandomVariable(getAverage(probabilities)),
				Arrays.asList(new RandomVariableInterface[]{ this, new RandomVariable(probabilities) }),
				OperatorType.AVERAGE2);
	}

	public RandomVariableInterface getVarianceAsRandomVariableAAD(RandomVariableInterface probabilities){
		/*returns deterministic AAD random variable */
		return new RandomVariableDifferentiableAADStochasticNonOptimized(
				new RandomVariable(getVariance(probabilities)),
				Arrays.asList(new RandomVariableInterface[]{ this, new RandomVariable(probabilities) }),
				OperatorType.VARIANCE2);
	}

	public RandomVariableInterface 	getStandardDeviationAsRandomVariableAAD(RandomVariableInterface probabilities){
		/*returns deterministic AAD random variable */
		return new RandomVariableDifferentiableAADStochasticNonOptimized(
				new RandomVariable(getStandardDeviation(probabilities)),
				Arrays.asList(new RandomVariableInterface[]{ this, new RandomVariable(probabilities) }),
				OperatorType.STDEV2);
	}

	public RandomVariableInterface 	getStandardErrorAsRandomVariableAAD(RandomVariableInterface probabilities){
		/*returns deterministic AAD random variable */
		return new RandomVariableDifferentiableAADStochasticNonOptimized(
				new RandomVariable(getStandardError(probabilities)),
				Arrays.asList(new RandomVariableInterface[]{ this, new RandomVariable(probabilities) }),
				OperatorType.STDERROR2);
	}

	public RandomVariableInterface getAverageAsRandomVariableAAD(){
		/*returns deterministic AAD random variable */
		return new RandomVariableDifferentiableAADStochasticNonOptimized(
				new RandomVariable(getAverage()),
				Arrays.asList(new RandomVariableInterface[]{ this }),
				OperatorType.AVERAGE);
	}

	public RandomVariableInterface getVarianceAsRandomVariableAAD(){
		/*returns deterministic AAD random variable */
		return new RandomVariableDifferentiableAADStochasticNonOptimized(
				new RandomVariable(getVariance()),
				Arrays.asList(new RandomVariableInterface[]{ this }),
				OperatorType.VARIANCE);
	}

	public RandomVariableInterface getSampleVarianceAsRandomVariableAAD() {
		/*returns deterministic AAD random variable */
		return new RandomVariableDifferentiableAADStochasticNonOptimized(
				new RandomVariable(getSampleVariance()),
				Arrays.asList(new RandomVariableInterface[]{ this }),
				OperatorType.SVARIANCE);
	}

	public RandomVariableInterface 	getStandardDeviationAsRandomVariableAAD(){
		/*returns deterministic AAD random variable */
		return new RandomVariableDifferentiableAADStochasticNonOptimized(
				new RandomVariable(getStandardDeviation()),
				Arrays.asList(new RandomVariableInterface[]{ this }),
				OperatorType.STDEV);
	}

	public RandomVariableInterface getStandardErrorAsRandomVariableAAD(){
		/*returns deterministic AAD random variable */
		return new RandomVariableDifferentiableAADStochasticNonOptimized(
				new RandomVariable(getStandardError()),
				Arrays.asList(new RandomVariableInterface[]{ this }),
				OperatorType.STDERROR);
	}

	public RandomVariableInterface 	getMinAsRandomVariableAAD(){
		/*returns deterministic AAD random variable */
		return new RandomVariableDifferentiableAADStochasticNonOptimized(
				new RandomVariable(getMin()),
				Arrays.asList(new RandomVariableInterface[]{ this }),
				OperatorType.MIN);
	}

	public RandomVariableInterface 	getMaxAsRandomVariableAAD(){
		/*returns deterministic AAD random variable */
		return new RandomVariableDifferentiableAADStochasticNonOptimized(
				new RandomVariable(getMax()),
				Arrays.asList(new RandomVariableInterface[]{ this }),
				OperatorType.MAX);
	}

	private RandomVariableInterface getValues(){
		return values;
	}

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
		return ((RandomVariableDifferentiableAADStochasticNonOptimized) getAverageAsRandomVariableAAD(probabilities)).getValues().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getVariance()
	 */
	@Override
	public double getVariance() {
		return getValues().getVariance();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getVariance(net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public double getVariance(RandomVariableInterface probabilities) {
		return getValues().getVariance(probabilities);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getSampleVariance()
	 */
	@Override
	public double getSampleVariance() {
		return getValues().getSampleVariance();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getStandardDeviation()
	 */
	@Override
	public double getStandardDeviation() {
		return getValues().getStandardDeviation();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getStandardDeviation(net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public double getStandardDeviation(RandomVariableInterface probabilities) {
		return getValues().getStandardDeviation(probabilities);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getStandardError()
	 */
	@Override
	public double getStandardError() {
		return getValues().getStandardError();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getStandardError(net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public double getStandardError(RandomVariableInterface probabilities) {
		return getValues().getStandardError(probabilities);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getQuantile(double)
	 */
	@Override
	public double getQuantile(double quantile) {
		return getValues().getQuantile(quantile);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getQuantile(double, net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public double getQuantile(double quantile, RandomVariableInterface probabilities) {
		return ((RandomVariableDifferentiableAADStochasticNonOptimized) getValues()).getValues().getQuantile(quantile, probabilities);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getQuantileExpectation(double, double)
	 */
	@Override
	public double getQuantileExpectation(double quantileStart, double quantileEnd) {
		return ((RandomVariableDifferentiableAADStochasticNonOptimized) getValues()).getValues().getQuantileExpectation(quantileStart, quantileEnd);
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
		return new RandomVariableDifferentiableAADStochasticNonOptimized(
				getValues().cap(cap),
				Arrays.asList(new RandomVariableInterface[]{ this, new RandomVariable(cap) }),
				OperatorType.CAP);
	}

	@Override
	public RandomVariableInterface floor(double floor) {
		return new RandomVariableDifferentiableAADStochasticNonOptimized(
				getValues().floor(floor),
				Arrays.asList(new RandomVariableInterface[]{ this, new RandomVariable(floor) }),
				OperatorType.FLOOR);
	}

	@Override
	public RandomVariableInterface add(double value) {
		return new RandomVariableDifferentiableAADStochasticNonOptimized(
				getValues().add(value),
				Arrays.asList(new RandomVariableInterface[]{ this, new RandomVariable(value) }),
				OperatorType.ADD);
	}

	@Override
	public RandomVariableInterface sub(double value) {
		return new RandomVariableDifferentiableAADStochasticNonOptimized(
				getValues().sub(value),
				Arrays.asList(new RandomVariableInterface[]{ this, new RandomVariable(value) }),
				OperatorType.SUB);
	}

	@Override
	public RandomVariableInterface mult(double value) {
		return new RandomVariableDifferentiableAADStochasticNonOptimized(
				getValues().mult(value),
				Arrays.asList(new RandomVariableInterface[]{ this, new RandomVariable(value) }),
				OperatorType.MULT);
	}

	@Override
	public RandomVariableInterface div(double value) {
		return new RandomVariableDifferentiableAADStochasticNonOptimized(
				getValues().div(value),
				Arrays.asList(new RandomVariableInterface[]{ this, new RandomVariable(value) }),
				OperatorType.DIV);
	}

	@Override
	public RandomVariableInterface pow(double exponent) {
		return new RandomVariableDifferentiableAADStochasticNonOptimized(
				getValues().pow(exponent),
				Arrays.asList(new RandomVariableInterface[]{ this, new RandomVariable(exponent) }),
				OperatorType.POW);
	}

	@Override
	public RandomVariableInterface squared() {
		return new RandomVariableDifferentiableAADStochasticNonOptimized(
				getValues().squared(),
				Arrays.asList(new RandomVariableInterface[]{ this }),
				OperatorType.SQUARED);
	}

	@Override
	public RandomVariableInterface sqrt() {
		return new RandomVariableDifferentiableAADStochasticNonOptimized(
				getValues().sqrt(),
				Arrays.asList(new RandomVariableInterface[]{ this }),
				OperatorType.SQRT);
	}

	@Override
	public RandomVariableInterface exp() {
		return new RandomVariableDifferentiableAADStochasticNonOptimized(
				getValues().exp(),
				Arrays.asList(new RandomVariableInterface[]{ this }),
				OperatorType.EXP);
	}

	@Override
	public RandomVariableInterface log() {
		return new RandomVariableDifferentiableAADStochasticNonOptimized(
				getValues().log(),
				Arrays.asList(new RandomVariableInterface[]{ this }),
				OperatorType.LOG);
	}

	@Override
	public RandomVariableInterface sin() {
		return new RandomVariableDifferentiableAADStochasticNonOptimized(
				getValues().sin(),
				Arrays.asList(new RandomVariableInterface[]{ this }),
				OperatorType.SIN);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#cos()
	 */
	@Override
	public RandomVariableInterface cos() {
		return new RandomVariableDifferentiableAADStochasticNonOptimized(
				getValues().cos(),
				Arrays.asList(new RandomVariableInterface[]{ this }),
				OperatorType.COS);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#add(net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public RandomVariableInterface add(RandomVariableInterface randomVariable) {	
		return new RandomVariableDifferentiableAADStochasticNonOptimized(
				getValues().add(randomVariable),
				Arrays.asList(new RandomVariableInterface[]{ this, randomVariable }),
				OperatorType.ADD);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#sub(net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public RandomVariableInterface sub(RandomVariableInterface randomVariable) {
		return new RandomVariableDifferentiableAADStochasticNonOptimized(
				getValues().sub(randomVariable),
				Arrays.asList(new RandomVariableInterface[]{ this, randomVariable }),
				OperatorType.SUB);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#mult(net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public RandomVariableDifferentiableInterface mult(RandomVariableInterface randomVariable) {
		return new RandomVariableDifferentiableAADStochasticNonOptimized(
				getValues().mult(randomVariable),
				Arrays.asList(new RandomVariableInterface[]{ this, randomVariable }),
				OperatorType.MULT);
	}

	@Override
	public RandomVariableInterface div(RandomVariableInterface randomVariable) {
		return new RandomVariableDifferentiableAADStochasticNonOptimized(
				getValues().div(randomVariable),
				Arrays.asList(new RandomVariableInterface[]{ this, randomVariable }),
				OperatorType.DIV);
	}

	@Override
	public RandomVariableInterface cap(RandomVariableInterface cap) {
		return new RandomVariableDifferentiableAADStochasticNonOptimized(
				getValues().cap(cap),
				Arrays.asList(new RandomVariableInterface[]{ this, cap }),
				OperatorType.CAP);
	}

	@Override
	public RandomVariableInterface floor(RandomVariableInterface floor) {
		return new RandomVariableDifferentiableAADStochasticNonOptimized(
				getValues().cap(floor),
				Arrays.asList(new RandomVariableInterface[]{ this, floor }),
				OperatorType.FLOOR);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#accrue(net.finmath.stochastic.RandomVariableInterface, double)
	 */
	@Override
	public RandomVariableInterface accrue(RandomVariableInterface rate, double periodLength) {
		return new RandomVariableDifferentiableAADStochasticNonOptimized(
				getValues().accrue(rate, periodLength),
				Arrays.asList(new RandomVariableInterface[]{ this, rate, new RandomVariable(periodLength) }),
				OperatorType.ACCRUE);
	}

	@Override
	public RandomVariableInterface discount(RandomVariableInterface rate, double periodLength) {
		return new RandomVariableDifferentiableAADStochasticNonOptimized(
				getValues().discount(rate, periodLength),
				Arrays.asList(new RandomVariableInterface[]{ this, rate, new RandomVariable(periodLength) }),
				OperatorType.DISCOUNT);
	}

	@Override
	public RandomVariableInterface barrier(RandomVariableInterface trigger, RandomVariableInterface valueIfTriggerNonNegative, RandomVariableInterface valueIfTriggerNegative) {
		return new RandomVariableDifferentiableAADStochasticNonOptimized(
				getValues().barrier(trigger, valueIfTriggerNonNegative, valueIfTriggerNegative),
				Arrays.asList(new RandomVariableInterface[]{ trigger, valueIfTriggerNonNegative, valueIfTriggerNegative }),
				OperatorType.BARRIER);
	}

	@Override
	public RandomVariableInterface barrier(RandomVariableInterface trigger, RandomVariableInterface valueIfTriggerNonNegative, double valueIfTriggerNegative) {
		return new RandomVariableDifferentiableAADStochasticNonOptimized(
				getValues().barrier(trigger, valueIfTriggerNonNegative, valueIfTriggerNegative),
				Arrays.asList(new RandomVariableInterface[]{ trigger, valueIfTriggerNonNegative, new RandomVariable(valueIfTriggerNegative) }),
				OperatorType.BARRIER);
	}

	@Override
	public RandomVariableInterface invert() {
		return new RandomVariableDifferentiableAADStochasticNonOptimized(
				getValues().invert(),
				Arrays.asList(new RandomVariableInterface[]{ this }),
				OperatorType.INVERT);
	}

	@Override
	public RandomVariableInterface abs() {
		return new RandomVariableDifferentiableAADStochasticNonOptimized(
				getValues().abs(),
				Arrays.asList(new RandomVariableInterface[]{ this }),
				OperatorType.ABS);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#addProduct(net.finmath.stochastic.RandomVariableInterface, double)
	 */
	@Override
	public RandomVariableInterface addProduct(RandomVariableInterface factor1, double factor2) {
		return new RandomVariableDifferentiableAADStochasticNonOptimized(
				getValues().addProduct(factor1, factor2),
				Arrays.asList(new RandomVariableInterface[]{ this, factor1, new RandomVariable(factor2) }),
				OperatorType.ADDPRODUCT);
	}

	@Override
	public RandomVariableInterface addProduct(RandomVariableInterface factor1, RandomVariableInterface factor2) {
		return new RandomVariableDifferentiableAADStochasticNonOptimized(
				getValues().addProduct(factor1, factor2),
				Arrays.asList(new RandomVariableInterface[]{ this, factor1, factor2 }),
				OperatorType.ADDPRODUCT);
	}

	@Override
	public RandomVariableInterface addRatio(RandomVariableInterface numerator, RandomVariableInterface denominator) {
		return new RandomVariableDifferentiableAADStochasticNonOptimized(
				getValues().addRatio(numerator, denominator),
				Arrays.asList(new RandomVariableInterface[]{ this, numerator, denominator }),
				OperatorType.ADDRATIO);
	}

	@Override
	public RandomVariableInterface subRatio(RandomVariableInterface numerator, RandomVariableInterface denominator) {
		return new RandomVariableDifferentiableAADStochasticNonOptimized(
				getValues().subRatio(numerator, denominator),
				Arrays.asList(new RandomVariableInterface[]{ this, numerator, denominator }),
				OperatorType.SUBRATIO);
	}

	@Override
	public RandomVariableInterface isNaN() {
		return getValues().isNaN();
	}

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
