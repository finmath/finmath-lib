/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christianfries.com.
 *
 * Created on 17.06.2017
 */
package net.finmath.montecarlo.automaticdifferentiation.forward;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import net.finmath.stochastic.ConditionalExpectationEstimatorInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * Implementation of <code>RandomVariableDifferentiableInterface</code> using
 * the forward algorithmic differentiation (AD).
 *
 * This class implements the optimized stochastic AD as it is described in
 * <a href="https://ssrn.com/abstract=2995695">ssrn.com/abstract=2995695</a>.
 *
 * @author Christian Fries
 * @author Stefan Sedlmair
 * @version 1.1
 */
public class RandomVariableDifferentiableAD implements RandomVariableDifferentiableInterface {

	private static final long serialVersionUID = 2459373647785530657L;
	
	private static final int typePriorityDefault = 3;
	
	private final int typePriority;
	
	private static AtomicLong indexOfNextRandomVariable = new AtomicLong(0);

	private enum OperatorType {
		ADD, MULT, DIV, SUB, SQUARED, SQRT, LOG, SIN, COS, EXP, INVERT, CAP, FLOOR, ABS,
		ADDPRODUCT, ADDRATIO, SUBRATIO, BARRIER, DISCOUNT, ACCRUE, POW, MIN, MAX, AVERAGE, VARIANCE,
		STDEV, STDERROR, SVARIANCE, AVERAGE2, VARIANCE2,
		STDEV2, STDERROR2, CONDITIONAL_EXPECTATION
	}

	/**
	 * A node in the <i>operator tree</i>. It
	 * stores an id (the index m), the operator (the function f_m), and the arguments.
	 * It also stores reference to the argument values, if required.
	 *
	 * @author Christian Fries
	 */
	private static class OperatorTreeNode {
		private final Long id;
		private final OperatorType operatorType;
		private final List<OperatorTreeNode> arguments;
		private final List<RandomVariableInterface> argumentValues;
		private final Object operator;

		OperatorTreeNode(OperatorType operatorType, List<RandomVariableInterface> arguments, Object operator) {
			this(operatorType,
					arguments != null ? arguments.stream().map((RandomVariableInterface x) -> {
						return (x != null && x instanceof RandomVariableDifferentiableAD) ? ((RandomVariableDifferentiableAD)x).getOperatorTreeNode(): null;
					}).collect(Collectors.toList()) : null,
							arguments != null ? arguments.stream().map((RandomVariableInterface x) -> {
								return (x != null && x instanceof RandomVariableDifferentiableAD) ? ((RandomVariableDifferentiableAD)x).getValues() : x;
							}).collect(Collectors.toList()) : null,
									operator
					);

		}
		OperatorTreeNode(OperatorType operatorType, List<OperatorTreeNode> arguments, List<RandomVariableInterface> argumentValues, Object operator) {
			super();
			this.id = indexOfNextRandomVariable.getAndIncrement();
			this.operatorType = operatorType;
			this.arguments = arguments;
			// This is the simple modification which reduces memory requirements.
			this.argumentValues = (operatorType != null && operatorType.equals(OperatorType.ADD)) ? null: argumentValues;
			this.operator = operator;
			if(operatorType != null && (operatorType.equals(OperatorType.ADD) || operatorType.equals(OperatorType.SUB))) {
				// Addition does not need to retain arguments
				argumentValues = null;
			}
			else if(operatorType != null && operatorType.equals(OperatorType.AVERAGE)) {
				// Average does not need to retain arguments
				argumentValues = null;
			}
			else if(operatorType != null && operatorType.equals(OperatorType.MULT)) {
				// Product only needs to retain factors on differentiables
				if(arguments.get(0) == null) {
					argumentValues.set(1, null);
				}
				if(arguments.get(1) == null) {
					argumentValues.set(0, null);
				}
			}
			else if(operatorType != null && operatorType.equals(OperatorType.DIV)) {
				// Division only needs to retain numerator if denominator is differentiable
				if(arguments.get(1) == null) {
					argumentValues.set(0, null);
				}
			}
			else if(operatorType != null && operatorType.equals(OperatorType.ADDPRODUCT)) {
				// Addition does not need to retain arguments
				argumentValues.set(0, null);
				// Addition of product only needs to retain factors on differentiables
				if(arguments.get(1) == null) {
					argumentValues.set(2, null);
				}
				if(arguments.get(2) == null) {
					argumentValues.set(1, null);
				}
			}
			else if(operatorType != null && operatorType.equals(OperatorType.ACCRUE)) {
				// Addition of product only needs to retain factors on differentiables
				if(arguments.get(1) == null && arguments.get(2) == null) {
					argumentValues.set(0, null);
				}
				if(arguments.get(0) == null && arguments.get(1) == null) {
					argumentValues.set(1, null);
				}
				if(arguments.get(0) == null && arguments.get(2) == null) {
					argumentValues.set(2, null);
				}
			}
			else if(operatorType != null && operatorType.equals(OperatorType.BARRIER)) {
				if(arguments.get(0) == null) {
					argumentValues.set(1, null);
					argumentValues.set(2, null);
				}
			}
		}

		private void propagateDerivativesFromResultToArgument(Map<Long, RandomVariableInterface> derivatives) {
			if(arguments == null) return;
			for(OperatorTreeNode argument : arguments) {
				if(argument != null) {
					Long argumentID = argument.id;

					RandomVariableInterface partialDerivative	= getPartialDerivative(argument);
					RandomVariableInterface derivative			= derivatives.get(id);
					RandomVariableInterface argumentDerivative	= derivatives.get(argumentID);

					// Implementation of AVERAGE (see https://ssrn.com/abstract=2995695 for details).
					if(operatorType == OperatorType.AVERAGE) {
						derivative = derivative.average();
					}
					// Implementation of CONDITIONAL_EXPECTATION (see https://ssrn.com/abstract=2995695 for details).
					if(operatorType == OperatorType.CONDITIONAL_EXPECTATION) {
						ConditionalExpectationEstimatorInterface estimator = (ConditionalExpectationEstimatorInterface)operator;
						derivative = estimator.getConditionalExpectation(derivative);
					}

					if(argumentDerivative == null) {
						argumentDerivative = derivative.mult(partialDerivative);
					}
					else {
						argumentDerivative = argumentDerivative.addProduct(partialDerivative, derivative);
					}

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

			switch(operatorType) {
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
				resultrandomvariable = new RandomVariable(1.0);
				break;
			case CONDITIONAL_EXPECTATION:
				resultrandomvariable = new RandomVariable(1.0);
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
				resultrandomvariable = differentialIndex == 0 ? Y.invert() : X.div(Y.squared()).mult(-1);
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
					resultrandomvariable = Y.div(Z.squared()).mult(-1.0);
				}
				break;
			case SUBRATIO:
				if(differentialIndex == 0) {
					resultrandomvariable = new RandomVariable(1.0);
				} else if(differentialIndex == 1) {
					resultrandomvariable = Z.invert().mult(-1.0);
				} else {
					resultrandomvariable = Y.div(Z.squared());
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
					resultrandomvariable = X.mult(Z).div(Y.mult(Z).add(1.0).squared()).mult(-1.0);
				} else {
					resultrandomvariable = X.mult(Y).div(Y.mult(Z).add(1.0).squared()).mult(-1.0);
				}
				break;
			case BARRIER:
				if(differentialIndex == 0) {
					/*
					 * Experimental version - This should be specified as a parameter.
					 */
					resultrandomvariable = Y.sub(Z);
					double epsilon = 0.2*X.getStandardDeviation();
					resultrandomvariable = resultrandomvariable.mult(X.barrier(X.add(epsilon/2), new RandomVariable(1.0), new RandomVariable(0.0)));
					resultrandomvariable = resultrandomvariable.mult(X.barrier(X.sub(epsilon/2), new RandomVariable(0.0), new RandomVariable(1.0)));
					resultrandomvariable = resultrandomvariable.div(epsilon);
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

	/*
	 * Data model. We maintain the underlying values and a link to the node in the operator tree.
	 */
	private RandomVariableInterface values;
	private final OperatorTreeNode operatorTreeNode;

	public static RandomVariableDifferentiableAD of(double value) {
		return new RandomVariableDifferentiableAD(value);
	}

	public static RandomVariableDifferentiableAD of(RandomVariableInterface randomVariable) {
		return new RandomVariableDifferentiableAD(randomVariable);
	}

	public RandomVariableDifferentiableAD(double value) {
		this(new RandomVariable(value), null, null);
	}

	public RandomVariableDifferentiableAD(double time, double[] realisations) {
		this(new RandomVariable(time, realisations), null, null);
	}

	public RandomVariableDifferentiableAD(RandomVariableInterface randomVariable) {
		this(randomVariable, null, null);
	}

	private RandomVariableDifferentiableAD(RandomVariableInterface values, List<RandomVariableInterface> arguments, OperatorType operator) {
		this(values, arguments, null, operator);
	}

	public RandomVariableDifferentiableAD(RandomVariableInterface values, List<RandomVariableInterface> arguments, ConditionalExpectationEstimatorInterface estimator, OperatorType operator) {
		this(values, arguments, estimator, operator, typePriorityDefault);
	}

	public RandomVariableDifferentiableAD(RandomVariableInterface values, List<RandomVariableInterface> arguments, ConditionalExpectationEstimatorInterface estimator, OperatorType operator, int methodArgumentTypePriority) {
		super();
		this.values = values;
		this.operatorTreeNode = new OperatorTreeNode(operator, arguments, estimator);
		
		this.typePriority = methodArgumentTypePriority;
	}

	public OperatorTreeNode getOperatorTreeNode() {
		return operatorTreeNode;
	}

	/**
	 * Returns the underlying values.
	 *
	 * @return The underling values.
	 */
	@Override
	public RandomVariableInterface getValues(){
		return values;
	}

	@Override
	public Long getID(){
		return getOperatorTreeNode().id;
	}

	/**
	 * Returns the gradient of this random variable with respect to all its leaf nodes.
	 * The method calculated the map \( v \mapsto \frac{d u}{d v} \) where \( u \) denotes <code>this</code>.
	 *
	 * Performs a backward automatic differentiation.
	 *
	 * @return The gradient map.
	 */
	@Override
	public Map<Long, RandomVariableInterface> getGradient(Set<Long> independentIDs) {

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

	@Override
	public Map<Long, RandomVariableInterface> getTangents(Set<Long> dependentIDs) {
		throw new UnsupportedOperationException();
	}

	/*
	 * The following methods are end points since they return <code>double</double> values.
	 * You cannot differentiate these results.
	 */

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

	@Override
	public int getTypePriority() {
		return typePriority;
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

	@Override
	public Double doubleValue() {
		return getValues().doubleValue();
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
		return getValues().getAverage(probabilities);
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
		return getValues().getQuantile(quantile, probabilities);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getQuantileExpectation(double, double)
	 */
	@Override
	public double getQuantileExpectation(double quantileStart, double quantileEnd) {
		return getValues().getQuantileExpectation(quantileStart, quantileEnd);
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

	/*
	 * The following methods are operations with are differentiable.
	 */

	@Override
	public RandomVariableInterface cache() {
		values = values.cache();
		return this;
	}

	@Override
	public RandomVariableInterface cap(double cap) {
		return new RandomVariableDifferentiableAD(
				getValues().cap(cap),
				Arrays.asList(this, new RandomVariable(cap)),
				OperatorType.CAP);
	}

	@Override
	public RandomVariableInterface floor(double floor) {
		return new RandomVariableDifferentiableAD(
				getValues().floor(floor),
				Arrays.asList(this, new RandomVariable(floor)),
				OperatorType.FLOOR);
	}

	@Override
	public RandomVariableInterface add(double value) {
		return new RandomVariableDifferentiableAD(
				getValues().add(value),
				Arrays.asList(this, new RandomVariable(value)),
				OperatorType.ADD);
	}

	@Override
	public RandomVariableInterface sub(double value) {
		return new RandomVariableDifferentiableAD(
				getValues().sub(value),
				Arrays.asList(this, new RandomVariable(value)),
				OperatorType.SUB);
	}

	@Override
	public RandomVariableInterface mult(double value) {
		return new RandomVariableDifferentiableAD(
				getValues().mult(value),
				Arrays.asList(this, new RandomVariable(value)),
				OperatorType.MULT);
	}

	@Override
	public RandomVariableInterface div(double value) {
		return new RandomVariableDifferentiableAD(
				getValues().div(value),
				Arrays.asList(this, new RandomVariable(value)),
				OperatorType.DIV);
	}

	@Override
	public RandomVariableInterface pow(double exponent) {
		return new RandomVariableDifferentiableAD(
				getValues().pow(exponent),
				Arrays.asList(this, new RandomVariable(exponent)),
				OperatorType.POW);
	}

	@Override
	public RandomVariableInterface average() {
		return new RandomVariableDifferentiableAD(
				getValues().average(),
				Arrays.asList(new RandomVariableInterface[]{ this }),
				OperatorType.AVERAGE);
	}

	@Override
	public RandomVariableInterface getConditionalExpectation(ConditionalExpectationEstimatorInterface estimator) {
		return new RandomVariableDifferentiableAD(
				getValues().average(),
				Arrays.asList(new RandomVariableInterface[]{ this }),
				estimator,
				OperatorType.CONDITIONAL_EXPECTATION);

	}

	@Override
	public RandomVariableInterface squared() {
		return new RandomVariableDifferentiableAD(
				getValues().squared(),
				Arrays.asList(new RandomVariableInterface[]{ this }),
				OperatorType.SQUARED);
	}

	@Override
	public RandomVariableInterface sqrt() {
		return new RandomVariableDifferentiableAD(
				getValues().sqrt(),
				Arrays.asList(new RandomVariableInterface[]{ this }),
				OperatorType.SQRT);
	}

	@Override
	public RandomVariableInterface exp() {
		return new RandomVariableDifferentiableAD(
				getValues().exp(),
				Arrays.asList(new RandomVariableInterface[]{ this }),
				OperatorType.EXP);
	}

	@Override
	public RandomVariableInterface log() {
		return new RandomVariableDifferentiableAD(
				getValues().log(),
				Arrays.asList(new RandomVariableInterface[]{ this }),
				OperatorType.LOG);
	}

	@Override
	public RandomVariableInterface sin() {
		return new RandomVariableDifferentiableAD(
				getValues().sin(),
				Arrays.asList(new RandomVariableInterface[]{ this }),
				OperatorType.SIN);
	}

	@Override
	public RandomVariableInterface cos() {
		return new RandomVariableDifferentiableAD(
				getValues().cos(),
				Arrays.asList(new RandomVariableInterface[]{ this }),
				OperatorType.COS);
	}

	/*
	 * Binary operators: checking for return type priority.
	 */

	@Override
	public RandomVariableInterface add(RandomVariableInterface randomVariable) {
		if(randomVariable.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return randomVariable.add(this);
		}

		return new RandomVariableDifferentiableAD(
				getValues().add(randomVariable.getValues()),
				Arrays.asList(this, randomVariable),
				OperatorType.ADD);
	}

	@Override
	public RandomVariableInterface sub(RandomVariableInterface randomVariable) {
		if(randomVariable.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return randomVariable.bus(this);
		}

		return new RandomVariableDifferentiableAD(
				getValues().sub(randomVariable.getValues()),
				Arrays.asList(this, randomVariable),
				OperatorType.SUB);
	}

	@Override
	public RandomVariableInterface bus(RandomVariableInterface randomVariable) {
		if(randomVariable.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return randomVariable.sub(this);
		}

		return new RandomVariableDifferentiableAD(
				getValues().bus(randomVariable.getValues()),
				Arrays.asList(randomVariable, this),	// SUB with swapped arguments
				OperatorType.SUB);
	}

	@Override
	public RandomVariableInterface mult(RandomVariableInterface randomVariable) {
		if(randomVariable.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return randomVariable.mult(this);
		}

		return new RandomVariableDifferentiableAD(
				getValues().mult(randomVariable.getValues()),
				Arrays.asList(this, randomVariable),
				OperatorType.MULT);
	}

	@Override
	public RandomVariableInterface div(RandomVariableInterface randomVariable) {
		if(randomVariable.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return randomVariable.vid(this);
		}

		return new RandomVariableDifferentiableAD(
				getValues().div(randomVariable.getValues()),
				Arrays.asList(this, randomVariable),
				OperatorType.DIV);
	}

	@Override
	public RandomVariableInterface vid(RandomVariableInterface randomVariable) {
		if(randomVariable.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return randomVariable.div(this);
		}

		return new RandomVariableDifferentiableAD(
				getValues().vid(randomVariable.getValues()),
				Arrays.asList(randomVariable, this),	// DIV with swapped arguments
				OperatorType.DIV);
	}

	@Override
	public RandomVariableInterface cap(RandomVariableInterface randomVariable) {
		if(randomVariable.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return randomVariable.cap(this);
		}

		return new RandomVariableDifferentiableAD(
				getValues().cap(randomVariable.getValues()),
				Arrays.asList(this, randomVariable),
				OperatorType.CAP);
	}

	@Override
	public RandomVariableInterface floor(RandomVariableInterface floor) {
		if(floor.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return floor.floor(this);
		}

		return new RandomVariableDifferentiableAD(
				getValues().floor(floor.getValues()),
				Arrays.asList(this, floor),
				OperatorType.FLOOR);
	}

	@Override
	public RandomVariableInterface accrue(RandomVariableInterface rate, double periodLength) {
		if(rate.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return rate.mult(periodLength).add(1.0).mult(this);
		}

		return new RandomVariableDifferentiableAD(
				getValues().accrue(rate.getValues(), periodLength),
				Arrays.asList(this, rate, new RandomVariable(periodLength)),
				OperatorType.ACCRUE);
	}

	@Override
	public RandomVariableInterface discount(RandomVariableInterface rate, double periodLength) {
		if(rate.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return rate.mult(periodLength).add(1.0).invert().mult(this);
		}

		return new RandomVariableDifferentiableAD(
				getValues().discount(rate.getValues(), periodLength),
				Arrays.asList(this, rate, new RandomVariable(periodLength)),
				OperatorType.DISCOUNT);
	}

	/*
	 * Ternary operators: checking for return type priority.
	 * @TODO add checking for return type priority.
	 */

	@Override
	public RandomVariableInterface barrier(RandomVariableInterface trigger, RandomVariableInterface valueIfTriggerNonNegative, RandomVariableInterface valueIfTriggerNegative) {
		RandomVariableInterface triggerValues = trigger instanceof RandomVariableDifferentiableAD ? ((RandomVariableDifferentiableAD)trigger).getValues() : trigger;
		return new RandomVariableDifferentiableAD(
				getValues().barrier(triggerValues.getValues(), valueIfTriggerNonNegative.getValues(), valueIfTriggerNegative),
				Arrays.asList(trigger, valueIfTriggerNonNegative, valueIfTriggerNegative),
				OperatorType.BARRIER);
	}

	@Override
	public RandomVariableInterface barrier(RandomVariableInterface trigger, RandomVariableInterface valueIfTriggerNonNegative, double valueIfTriggerNegative) {
		RandomVariableInterface triggerValues = trigger instanceof RandomVariableDifferentiableAD ? ((RandomVariableDifferentiableAD)trigger).getValues() : trigger;
		return new RandomVariableDifferentiableAD(
				getValues().barrier(triggerValues.getValues(), valueIfTriggerNonNegative, valueIfTriggerNegative),
				Arrays.asList(trigger, valueIfTriggerNonNegative, new RandomVariable(valueIfTriggerNegative)),
				OperatorType.BARRIER);
	}

	@Override
	public RandomVariableInterface invert() {
		return new RandomVariableDifferentiableAD(
				getValues().invert(),
				Arrays.asList(new RandomVariableInterface[]{ this }),
				OperatorType.INVERT);
	}

	@Override
	public RandomVariableInterface abs() {
		return new RandomVariableDifferentiableAD(
				getValues().abs(),
				Arrays.asList(new RandomVariableInterface[]{ this }),
				OperatorType.ABS);
	}

	@Override
	public RandomVariableInterface addProduct(RandomVariableInterface factor1, double factor2) {
		if(factor1.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return factor1.mult(factor2).add(this);
		}

		return new RandomVariableDifferentiableAD(
				getValues().addProduct(factor1.getValues(), factor2),
				Arrays.asList(this, factor1, new RandomVariable(factor2)),
				OperatorType.ADDPRODUCT);
	}

	@Override
	public RandomVariableInterface addProduct(RandomVariableInterface factor1, RandomVariableInterface factor2) {
		if(factor1.getTypePriority() > this.getTypePriority() || factor2.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return factor1.mult(factor2).add(this);
		}

		return new RandomVariableDifferentiableAD(
				getValues().addProduct(factor1.getValues(), factor2.getValues()),
				Arrays.asList(this, factor1, factor2),
				OperatorType.ADDPRODUCT);
	}

	@Override
	public RandomVariableInterface addRatio(RandomVariableInterface numerator, RandomVariableInterface denominator) {
		if(numerator.getTypePriority() > this.getTypePriority() || denominator.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return numerator.div(denominator).add(this);
		}

		return new RandomVariableDifferentiableAD(
				getValues().addRatio(numerator.getValues(), denominator.getValues()),
				Arrays.asList(this, numerator, denominator),
				OperatorType.ADDRATIO);
	}

	@Override
	public RandomVariableInterface subRatio(RandomVariableInterface numerator, RandomVariableInterface denominator) {
		if(numerator.getTypePriority() > this.getTypePriority() || denominator.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return numerator.div(denominator).mult(-1).add(this);
		}

		return new RandomVariableDifferentiableAD(
				getValues().subRatio(numerator.getValues(), denominator.getValues()),
				Arrays.asList(this, numerator, denominator),
				OperatorType.SUBRATIO);
	}

	/*
	 * The following methods are end points, the result is not differentiable.
	 */

	@Override
	public RandomVariableInterface isNaN() {
		return getValues().isNaN();
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

	public RandomVariableInterface getVarianceAsRandomVariableAAD(){
		/*returns deterministic AAD random variable */
		return new RandomVariableDifferentiableAD(
				new RandomVariable(getVariance()),
				Arrays.asList(new RandomVariableInterface[]{ this }),
				OperatorType.VARIANCE);
	}

	public RandomVariableInterface getSampleVarianceAsRandomVariableAAD() {
		/*returns deterministic AAD random variable */
		return new RandomVariableDifferentiableAD(
				new RandomVariable(getSampleVariance()),
				Arrays.asList(new RandomVariableInterface[]{ this }),
				OperatorType.SVARIANCE);
	}

	public RandomVariableInterface 	getStandardDeviationAsRandomVariableAAD(){
		/*returns deterministic AAD random variable */
		return new RandomVariableDifferentiableAD(
				new RandomVariable(getStandardDeviation()),
				Arrays.asList(new RandomVariableInterface[]{ this }),
				OperatorType.STDEV);
	}

	public RandomVariableInterface getStandardErrorAsRandomVariableAAD(){
		/*returns deterministic AAD random variable */
		return new RandomVariableDifferentiableAD(
				new RandomVariable(getStandardError()),
				Arrays.asList(new RandomVariableInterface[]{ this }),
				OperatorType.STDERROR);
	}

	public RandomVariableInterface 	getMinAsRandomVariableAAD(){
		/*returns deterministic AAD random variable */
		return new RandomVariableDifferentiableAD(
				new RandomVariable(getMin()),
				Arrays.asList(new RandomVariableInterface[]{ this }),
				OperatorType.MIN);
	}

	public RandomVariableInterface 	getMaxAsRandomVariableAAD(){
		/*returns deterministic AAD random variable */
		return new RandomVariableDifferentiableAD(
				new RandomVariable(getMax()),
				Arrays.asList(new RandomVariableInterface[]{ this }),
				OperatorType.MAX);
	}

	@Override
	public Map<Long, RandomVariableInterface> getTangents() {
		// TODO Auto-generated method stub
		return null;
	}
}
