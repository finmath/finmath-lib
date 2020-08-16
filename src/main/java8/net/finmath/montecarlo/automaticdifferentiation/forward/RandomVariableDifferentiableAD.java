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
import java.util.function.Function;
import java.util.function.IntToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

import net.finmath.functions.DoubleTernaryOperator;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiable;
import net.finmath.stochastic.ConditionalExpectationEstimator;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;

/**
 * Implementation of <code>RandomVariableDifferentiable</code> using
 * the forward algorithmic differentiation (AD).
 *
 * This class implements the optimized stochastic AD as it is described in
 * <a href="https://ssrn.com/abstract=2995695">ssrn.com/abstract=2995695</a>.
 * For details see <a href="http://christianfries.com/finmath/stochasticautodiff/">http://christianfries.com/finmath/stochasticautodiff/</a>.
 *
 * @author Christian Fries
 * @author Stefan Sedlmair
 * @version 1.1
 */
public class RandomVariableDifferentiableAD implements RandomVariableDifferentiable {

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
		private final List<RandomVariable> argumentValues;
		private final Object operator;

		private static final RandomVariable zero = new Scalar(0.0);
		private static final RandomVariable one = new Scalar(1.0);
		private static final RandomVariable minusOne = new Scalar(-1.0);

		OperatorTreeNode(final OperatorType operatorType, final List<RandomVariable> arguments, final Object operator) {
			this(operatorType,
					arguments != null ? arguments.stream().map(new Function<RandomVariable, OperatorTreeNode>() {
						@Override
						public OperatorTreeNode apply(final RandomVariable x) {
							return (x != null && x instanceof RandomVariableDifferentiableAD) ? ((RandomVariableDifferentiableAD)x).getOperatorTreeNode(): null;
						}
					}).collect(Collectors.toList()) : null,
							arguments != null ? arguments.stream().map(new Function<RandomVariable, RandomVariable>() {
								@Override
								public RandomVariable apply(final RandomVariable x) {
									return (x != null && x instanceof RandomVariableDifferentiableAD) ? ((RandomVariableDifferentiableAD)x).getValues() : x;
								}
							}).collect(Collectors.toList()) : null,
									operator
					);

		}
		OperatorTreeNode(final OperatorType operatorType, final List<OperatorTreeNode> arguments, List<RandomVariable> argumentValues, final Object operator) {
			super();
			id = indexOfNextRandomVariable.getAndIncrement();
			this.operatorType = operatorType;
			this.arguments = arguments;
			this.operator = operator;
			// This is the simple modification which reduces memory requirements.
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

			this.argumentValues = argumentValues;
		}

		private void propagateDerivativesFromResultToArgument(final Map<Long, RandomVariable> derivatives) {
			if(arguments == null) {
				return;
			}
			for(int argumentIndex = 0; argumentIndex < arguments.size(); argumentIndex++) {
				final OperatorTreeNode argument = arguments.get(argumentIndex);
				if(argument != null) {
					final Long argumentID = argument.id;

					final RandomVariable partialDerivative	= getPartialDerivative(argument, argumentIndex);
					RandomVariable derivative			= derivatives.get(id);
					RandomVariable argumentDerivative	= derivatives.get(argumentID);

					// Implementation of AVERAGE (see https://ssrn.com/abstract=2995695 for details).
					if(operatorType == OperatorType.AVERAGE) {
						derivative = derivative.average();
					}
					// Implementation of CONDITIONAL_EXPECTATION (see https://ssrn.com/abstract=2995695 for details).
					if(operatorType == OperatorType.CONDITIONAL_EXPECTATION) {
						final ConditionalExpectationEstimator estimator = (ConditionalExpectationEstimator)operator;
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

		private RandomVariable getPartialDerivative(final OperatorTreeNode differential, final int differentialIndex) {

			if(!arguments.contains(differential)) {
				return zero;
			}

			final RandomVariable X = arguments.size() > 0 && argumentValues != null ? argumentValues.get(0) : null;
			final RandomVariable Y = arguments.size() > 1 && argumentValues != null ? argumentValues.get(1) : null;
			final RandomVariable Z = arguments.size() > 2 && argumentValues != null ? argumentValues.get(2) : null;

			RandomVariable derivative = null;

			switch(operatorType) {
			/* functions with one argument  */
			case SQUARED:
				derivative = X.mult(2.0);
				break;
			case SQRT:
				derivative = X.sqrt().invert().mult(0.5);
				break;
			case EXP:
				derivative = X.exp();
				break;
			case LOG:
				derivative = X.invert();
				break;
			case SIN:
				derivative = X.cos();
				break;
			case COS:
				derivative = X.sin().mult(-1.0);
				break;
			case INVERT:
				derivative = X.invert().squared().mult(-1);
				break;
			case AVERAGE:
				derivative = one;
				break;
			case CONDITIONAL_EXPECTATION:
				derivative = one;
				break;
			case VARIANCE:
				derivative = X.sub(X.getAverage()*(2.0*X.size()-1.0)/X.size()).mult(2.0/X.size());
				break;
			case STDEV:
				derivative = X.sub(X.getAverage()*(2.0*X.size()-1.0)/X.size()).mult(2.0/X.size()).mult(0.5).div(Math.sqrt(X.getVariance()));
				break;
			case MIN:
				final double min = X.getMin();
				derivative = X.apply(new DoubleUnaryOperator() {
					@Override
					public double applyAsDouble(final double x) {
						return (x == min) ? 1.0 : 0.0;
					}
				});
				break;
			case MAX:
				final double max = X.getMax();
				derivative = X.apply(new DoubleUnaryOperator() {
					@Override
					public double applyAsDouble(final double x) {
						return (x == max) ? 1.0 : 0.0;
					}
				});
				break;
			case ABS:
				derivative = X.choose(one, minusOne);
				break;
			case STDERROR:
				derivative = X.sub(X.getAverage()*(2.0*X.size()-1.0)/X.size()).mult(2.0/X.size()).mult(0.5).div(Math.sqrt(X.getVariance() * X.size()));
				break;
			case SVARIANCE:
				derivative = X.sub(X.getAverage()*(2.0*X.size()-1.0)/X.size()).mult(2.0/(X.size()-1));
				break;
			case ADD:
				derivative = one;
				break;
			case SUB:
				derivative = differentialIndex == 0 ? one : minusOne;
				break;
			case MULT:
				derivative = differentialIndex == 0 ? Y : X;
				break;
			case DIV:
				derivative = differentialIndex == 0 ? Y.invert() : X.div(Y.squared()).mult(-1);
				break;
			case CAP:
				if(differentialIndex == 0) {
					derivative = X.sub(Y).choose(zero, one);
				}
				else {
					derivative = X.sub(Y).choose(one, zero);
				}
				break;
			case FLOOR:
				if(differentialIndex == 0) {
					derivative = X.sub(Y).choose(one, zero);
				}
				else {
					derivative = X.sub(Y).choose(zero, one);
				}
				break;
			case AVERAGE2:
				derivative = differentialIndex == 0 ? Y : X;
				break;
			case VARIANCE2:
				derivative = differentialIndex == 0 ? Y.mult(2.0).mult(X.mult(Y.add(X.getAverage(Y)*(X.size()-1)).sub(X.getAverage(Y)))) :
					X.mult(2.0).mult(Y.mult(X.add(Y.getAverage(X)*(X.size()-1)).sub(Y.getAverage(X))));
				break;
			case STDEV2:
				derivative = differentialIndex == 0 ? Y.mult(2.0).mult(X.mult(Y.add(X.getAverage(Y)*(X.size()-1)).sub(X.getAverage(Y)))).div(Math.sqrt(X.getVariance(Y))) :
					X.mult(2.0).mult(Y.mult(X.add(Y.getAverage(X)*(X.size()-1)).sub(Y.getAverage(X)))).div(Math.sqrt(Y.getVariance(X)));
				break;
			case STDERROR2:
				derivative = differentialIndex == 0 ? Y.mult(2.0).mult(X.mult(Y.add(X.getAverage(Y)*(X.size()-1)).sub(X.getAverage(Y)))).div(Math.sqrt(X.getVariance(Y) * X.size())) :
					X.mult(2.0).mult(Y.mult(X.add(Y.getAverage(X)*(X.size()-1)).sub(Y.getAverage(X)))).div(Math.sqrt(Y.getVariance(X) * Y.size()));
				break;
			case POW:
				// second argument will always be deterministic and constant.
				// @TODO Optimize this part by making use of Y being scalar.
				derivative = (differentialIndex == 0) ? X.pow(Y.getAverage() - 1.0).mult(Y) : zero;
				break;
			case ADDPRODUCT:
				if(differentialIndex == 0) {
					derivative = one;
				} else if(differentialIndex == 1) {
					derivative = Z;
				} else {
					derivative = Y;
				}
				break;
			case ADDRATIO:
				if(differentialIndex == 0) {
					derivative = one;
				} else if(differentialIndex == 1) {
					derivative = Z.invert();
				} else {
					derivative = Y.div(Z.squared()).mult(-1.0);
				}
				break;
			case SUBRATIO:
				if(differentialIndex == 0) {
					derivative = one;
				} else if(differentialIndex == 1) {
					derivative = Z.invert().mult(-1.0);
				} else {
					derivative = Y.div(Z.squared());
				}
				break;
			case ACCRUE:
				if(differentialIndex == 0) {
					derivative = Y.mult(Z).add(1.0);
				} else if(differentialIndex == 1) {
					derivative = X.mult(Z);
				} else {
					derivative = X.mult(Y);
				}
				break;
			case DISCOUNT:
				if(differentialIndex == 0) {
					derivative = Y.mult(Z).add(1.0).invert();
				} else if(differentialIndex == 1) {
					derivative = X.mult(Z).div(Y.mult(Z).add(1.0).squared()).mult(-1.0);
				} else {
					derivative = X.mult(Y).div(Y.mult(Z).add(1.0).squared()).mult(-1.0);
				}
				break;
			case BARRIER:
				if(differentialIndex == 0) {
					/*
					 * Approximation via local finite difference
					 * (see https://ssrn.com/abstract=2995695 for details).
					 */
					derivative = Y.sub(Z);
					final double epsilon = 0.2*X.getStandardDeviation();
					derivative = derivative.mult(X.add(epsilon/2).choose(new RandomVariableFromDoubleArray(1.0), new RandomVariableFromDoubleArray(0.0)));
					derivative = derivative.mult(X.sub(epsilon/2).choose(new RandomVariableFromDoubleArray(0.0), new RandomVariableFromDoubleArray(1.0)));
					derivative = derivative.div(epsilon);
				} else if(differentialIndex == 1) {
					derivative = X.choose(one, zero);
				} else {
					derivative = X.choose(zero, one);
				}
				break;
			default:
				throw new IllegalArgumentException("Operation " + operatorType.name() + " not supported in differentiation.");
			}

			return derivative;
		}
	}

	/*
	 * Data model. We maintain the underlying values and a link to the node in the operator tree.
	 */
	private RandomVariable values;
	private final OperatorTreeNode operatorTreeNode;

	public static RandomVariableDifferentiableAD of(final double value) {
		return new RandomVariableDifferentiableAD(value);
	}

	public static RandomVariableDifferentiableAD of(final RandomVariable randomVariable) {
		return new RandomVariableDifferentiableAD(randomVariable);
	}

	public RandomVariableDifferentiableAD(final double value) {
		this(new RandomVariableFromDoubleArray(value), null, null);
	}

	public RandomVariableDifferentiableAD(final double time, final double[] realisations) {
		this(new RandomVariableFromDoubleArray(time, realisations), null, null);
	}

	public RandomVariableDifferentiableAD(final RandomVariable randomVariable) {
		this(randomVariable, null, null);
	}

	private RandomVariableDifferentiableAD(final RandomVariable values, final List<RandomVariable> arguments, final OperatorType operator) {
		this(values, arguments, null, operator);
	}

	public RandomVariableDifferentiableAD(final RandomVariable values, final List<RandomVariable> arguments, final ConditionalExpectationEstimator estimator, final OperatorType operator) {
		this(values, arguments, estimator, operator, typePriorityDefault);
	}

	public RandomVariableDifferentiableAD(final RandomVariable values, final List<RandomVariable> arguments, final ConditionalExpectationEstimator estimator, final OperatorType operator, final int methodArgumentTypePriority) {
		super();
		this.values = values;
		operatorTreeNode = new OperatorTreeNode(operator, arguments, estimator);

		typePriority = methodArgumentTypePriority;
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
	public RandomVariable getValues(){
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
	public Map<Long, RandomVariable> getGradient(final Set<Long> independentIDs) {

		// The map maintaining the derivatives id -> derivative
		final Map<Long, RandomVariable> derivatives = new HashMap<>();

		// Put derivative of this node w.r.t. itself
		derivatives.put(getID(), new RandomVariableFromDoubleArray(1.0));

		// The set maintaining the independents. Note: TreeMap is maintaining a sort on the keys.
		final TreeMap<Long, OperatorTreeNode> independents = new TreeMap<>();
		independents.put(getID(), getOperatorTreeNode());

		while(independents.size() > 0) {
			// Process node with the highest id in independents
			final Map.Entry<Long, OperatorTreeNode> independentEntry = independents.lastEntry();
			final Long id = independentEntry.getKey();
			final OperatorTreeNode independent = independentEntry.getValue();

			// Get arguments of this node and propagate derivative to arguments
			final List<OperatorTreeNode> arguments = independent.arguments;
			if(arguments != null && arguments.size() > 0) {
				independent.propagateDerivativesFromResultToArgument(derivatives);

				// Add all non constant arguments to the list of independents
				for(final OperatorTreeNode argument : arguments) {
					if(argument != null) {
						final Long argumentId = argument.id;
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
	public Map<Long, RandomVariable> getTangents(final Set<Long> dependentIDs) {
		throw new UnsupportedOperationException();
	}

	/*
	 * The following methods are end points since they return <code>double</double> values.
	 * You cannot differentiate these results.
	 */

	@Override
	public boolean equals(final RandomVariable randomVariable) {
		return getValues().equals(randomVariable);
	}

	@Override
	public double getFiltrationTime() {
		return getValues().getFiltrationTime();
	}

	@Override
	public int getTypePriority() {
		return typePriority;
	}

	@Override
	public double get(final int pathOrState) {
		return getValues().get(pathOrState);
	}

	@Override
	public int size() {
		return getValues().size();
	}

	@Override
	public boolean isDeterministic() {
		return getValues().isDeterministic();
	}

	@Override
	public double[] getRealizations() {
		return getValues().getRealizations();
	}

	@Override
	public Double doubleValue() {
		return getValues().doubleValue();
	}

	@Override
	public double getMin() {
		return getValues().getMin();
	}

	@Override
	public double getMax() {
		return getValues().getMax();
	}

	@Override
	public double getAverage() {
		return getValues().getAverage();
	}

	@Override
	public double getAverage(final RandomVariable probabilities) {
		return getValues().getAverage(probabilities);
	}

	@Override
	public double getVariance() {
		return getValues().getVariance();
	}

	@Override
	public double getVariance(final RandomVariable probabilities) {
		return getValues().getVariance(probabilities);
	}

	@Override
	public double getSampleVariance() {
		return getValues().getSampleVariance();
	}

	@Override
	public double getStandardDeviation() {
		return getValues().getStandardDeviation();
	}

	@Override
	public double getStandardDeviation(final RandomVariable probabilities) {
		return getValues().getStandardDeviation(probabilities);
	}

	@Override
	public double getStandardError() {
		return getValues().getStandardError();
	}

	@Override
	public double getStandardError(final RandomVariable probabilities) {
		return getValues().getStandardError(probabilities);
	}

	@Override
	public double getQuantile(final double quantile) {
		return getValues().getQuantile(quantile);
	}

	@Override
	public double getQuantile(final double quantile, final RandomVariable probabilities) {
		return getValues().getQuantile(quantile, probabilities);
	}

	@Override
	public double getQuantileExpectation(final double quantileStart, final double quantileEnd) {
		return getValues().getQuantileExpectation(quantileStart, quantileEnd);
	}

	@Override
	public double[] getHistogram(final double[] intervalPoints) {
		return getValues().getHistogram(intervalPoints);
	}

	@Override
	public double[][] getHistogram(final int numberOfPoints, final double standardDeviations) {
		return getValues().getHistogram(numberOfPoints, standardDeviations);
	}

	/*
	 * The following methods are operations with are differentiable.
	 */

	@Override
	public RandomVariable cache() {
		values = values.cache();
		return this;
	}

	@Override
	public RandomVariable cap(final double cap) {
		return new RandomVariableDifferentiableAD(
				getValues().cap(cap),
				Arrays.asList(this, new RandomVariableFromDoubleArray(cap)),
				OperatorType.CAP);
	}

	@Override
	public RandomVariable floor(final double floor) {
		return new RandomVariableDifferentiableAD(
				getValues().floor(floor),
				Arrays.asList(this, new RandomVariableFromDoubleArray(floor)),
				OperatorType.FLOOR);
	}

	@Override
	public RandomVariable add(final double value) {
		return new RandomVariableDifferentiableAD(
				getValues().add(value),
				Arrays.asList(this, new RandomVariableFromDoubleArray(value)),
				OperatorType.ADD);
	}

	@Override
	public RandomVariable sub(final double value) {
		return new RandomVariableDifferentiableAD(
				getValues().sub(value),
				Arrays.asList(this, new RandomVariableFromDoubleArray(value)),
				OperatorType.SUB);
	}

	@Override
	public RandomVariable mult(final double value) {
		return new RandomVariableDifferentiableAD(
				getValues().mult(value),
				Arrays.asList(this, new RandomVariableFromDoubleArray(value)),
				OperatorType.MULT);
	}

	@Override
	public RandomVariable div(final double value) {
		return new RandomVariableDifferentiableAD(
				getValues().div(value),
				Arrays.asList(this, new RandomVariableFromDoubleArray(value)),
				OperatorType.DIV);
	}

	@Override
	public RandomVariable pow(final double exponent) {
		return new RandomVariableDifferentiableAD(
				getValues().pow(exponent),
				Arrays.asList(this, new RandomVariableFromDoubleArray(exponent)),
				OperatorType.POW);
	}

	@Override
	public RandomVariable average() {
		return new RandomVariableDifferentiableAD(
				getValues().average(),
				Arrays.asList(new RandomVariable[]{ this }),
				OperatorType.AVERAGE);
	}

	@Override
	public RandomVariable getConditionalExpectation(final ConditionalExpectationEstimator estimator) {
		return new RandomVariableDifferentiableAD(
				getValues().average(),
				Arrays.asList(new RandomVariable[]{ this }),
				estimator,
				OperatorType.CONDITIONAL_EXPECTATION);

	}

	@Override
	public RandomVariable squared() {
		return new RandomVariableDifferentiableAD(
				getValues().squared(),
				Arrays.asList(new RandomVariable[]{ this }),
				OperatorType.SQUARED);
	}

	@Override
	public RandomVariable sqrt() {
		return new RandomVariableDifferentiableAD(
				getValues().sqrt(),
				Arrays.asList(new RandomVariable[]{ this }),
				OperatorType.SQRT);
	}

	@Override
	public RandomVariable exp() {
		return new RandomVariableDifferentiableAD(
				getValues().exp(),
				Arrays.asList(new RandomVariable[]{ this }),
				OperatorType.EXP);
	}

	@Override
	public RandomVariable log() {
		return new RandomVariableDifferentiableAD(
				getValues().log(),
				Arrays.asList(new RandomVariable[]{ this }),
				OperatorType.LOG);
	}

	@Override
	public RandomVariable sin() {
		return new RandomVariableDifferentiableAD(
				getValues().sin(),
				Arrays.asList(new RandomVariable[]{ this }),
				OperatorType.SIN);
	}

	@Override
	public RandomVariable cos() {
		return new RandomVariableDifferentiableAD(
				getValues().cos(),
				Arrays.asList(new RandomVariable[]{ this }),
				OperatorType.COS);
	}

	/*
	 * Binary operators: checking for return type priority.
	 */

	@Override
	public RandomVariable add(final RandomVariable randomVariable) {
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
	public RandomVariable sub(final RandomVariable randomVariable) {
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
	public RandomVariable bus(final RandomVariable randomVariable) {
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
	public RandomVariable mult(final RandomVariable randomVariable) {
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
	public RandomVariable div(final RandomVariable randomVariable) {
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
	public RandomVariable vid(final RandomVariable randomVariable) {
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
	public RandomVariable cap(final RandomVariable randomVariable) {
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
	public RandomVariable floor(final RandomVariable floor) {
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
	public RandomVariable accrue(final RandomVariable rate, final double periodLength) {
		if(rate.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return rate.mult(periodLength).add(1.0).mult(this);
		}

		return new RandomVariableDifferentiableAD(
				getValues().accrue(rate.getValues(), periodLength),
				Arrays.asList(this, rate, new RandomVariableFromDoubleArray(periodLength)),
				OperatorType.ACCRUE);
	}

	@Override
	public RandomVariable discount(final RandomVariable rate, final double periodLength) {
		if(rate.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return rate.mult(periodLength).add(1.0).invert().mult(this);
		}

		return new RandomVariableDifferentiableAD(
				getValues().discount(rate.getValues(), periodLength),
				Arrays.asList(this, rate, new RandomVariableFromDoubleArray(periodLength)),
				OperatorType.DISCOUNT);
	}

	/*
	 * Ternary operators: checking for return type priority.
	 * @TODO add checking for return type priority.
	 */

	@Override
	public RandomVariable choose(final RandomVariable valueIfTriggerNonNegative, final RandomVariable valueIfTriggerNegative) {
		return new RandomVariableDifferentiableAD(
				getValues().choose(valueIfTriggerNonNegative.getValues(), valueIfTriggerNegative.getValues()),
				Arrays.asList(this, valueIfTriggerNonNegative, valueIfTriggerNegative),
				OperatorType.BARRIER);
	}

	@Override
	public RandomVariable invert() {
		return new RandomVariableDifferentiableAD(
				getValues().invert(),
				Arrays.asList(new RandomVariable[]{ this }),
				OperatorType.INVERT);
	}

	@Override
	public RandomVariable abs() {
		return new RandomVariableDifferentiableAD(
				getValues().abs(),
				Arrays.asList(new RandomVariable[]{ this }),
				OperatorType.ABS);
	}

	@Override
	public RandomVariable addProduct(final RandomVariable factor1, final double factor2) {
		if(factor1.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return factor1.mult(factor2).add(this);
		}

		return new RandomVariableDifferentiableAD(
				getValues().addProduct(factor1.getValues(), factor2),
				Arrays.asList(this, factor1, new RandomVariableFromDoubleArray(factor2)),
				OperatorType.ADDPRODUCT);
	}

	@Override
	public RandomVariable addProduct(final RandomVariable factor1, final RandomVariable factor2) {
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
	public RandomVariable addRatio(final RandomVariable numerator, final RandomVariable denominator) {
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
	public RandomVariable subRatio(final RandomVariable numerator, final RandomVariable denominator) {
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
	public RandomVariable isNaN() {
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

	public RandomVariable getVarianceAsRandomVariableAAD(){
		/*returns deterministic AAD random variable */
		return new RandomVariableDifferentiableAD(
				new RandomVariableFromDoubleArray(getVariance()),
				Arrays.asList(new RandomVariable[]{ this }),
				OperatorType.VARIANCE);
	}

	public RandomVariable getSampleVarianceAsRandomVariableAAD() {
		/*returns deterministic AAD random variable */
		return new RandomVariableDifferentiableAD(
				new RandomVariableFromDoubleArray(getSampleVariance()),
				Arrays.asList(new RandomVariable[]{ this }),
				OperatorType.SVARIANCE);
	}

	public RandomVariable 	getStandardDeviationAsRandomVariableAAD(){
		/*returns deterministic AAD random variable */
		return new RandomVariableDifferentiableAD(
				new RandomVariableFromDoubleArray(getStandardDeviation()),
				Arrays.asList(new RandomVariable[]{ this }),
				OperatorType.STDEV);
	}

	public RandomVariable getStandardErrorAsRandomVariableAAD(){
		/*returns deterministic AAD random variable */
		return new RandomVariableDifferentiableAD(
				new RandomVariableFromDoubleArray(getStandardError()),
				Arrays.asList(new RandomVariable[]{ this }),
				OperatorType.STDERROR);
	}

	public RandomVariable 	getMinAsRandomVariableAAD(){
		/*returns deterministic AAD random variable */
		return new RandomVariableDifferentiableAD(
				new RandomVariableFromDoubleArray(getMin()),
				Arrays.asList(new RandomVariable[]{ this }),
				OperatorType.MIN);
	}

	public RandomVariable 	getMaxAsRandomVariableAAD(){
		/*returns deterministic AAD random variable */
		return new RandomVariableDifferentiableAD(
				new RandomVariableFromDoubleArray(getMax()),
				Arrays.asList(new RandomVariable[]{ this }),
				OperatorType.MAX);
	}

	@Override
	public Map<Long, RandomVariable> getTangents() {
		// TODO Auto-generated method stub
		return null;
	}
}
