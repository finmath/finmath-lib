/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christianfries.com.
 *
 * Created on 17.06.2017
 */
package net.finmath.montecarlo.automaticdifferentiation.backward.alternative;

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
import net.finmath.stochastic.RandomVariable;

/**
 * Implementation of <code>RandomVariableDifferentiable</code> using
 * the backward algorithmic differentiation (adjoint algorithmic differentiation, AAD).
 *
 * @author Christian Fries
 * @author Stefan Sedlmair
 * @version 1.0
 */
public class RandomVariableDifferentiableAADPathwise implements RandomVariableDifferentiable {

	private static final long serialVersionUID = 2459373647785530657L;

	private static AtomicLong indexOfNextRandomVariable = new AtomicLong(0);

	private enum OperatorType {
		ADD, MULT, DIV, SUB, SQUARED, SQRT, LOG, SIN, COS, EXP, INVERT, CAP, FLOOR, ABS,
		ADDPRODUCT, ADDRATIO, SUBRATIO, BARRIER, DISCOUNT, ACCRUE, POW, MIN, MAX, AVERAGE, VARIANCE,
		STDEV, STDERROR, SVARIANCE, AVERAGE2, VARIANCE2,
		STDEV2, STDERROR2
	}

	private static class OperatorTreeNode {
		private final Long id;
		private final OperatorType operator;
		private final List<OperatorTreeNode> arguments;
		private final List<RandomVariable> argumentValues;

		OperatorTreeNode(final OperatorType operator, final List<RandomVariable> arguments) {
			this(operator,
					arguments != null ? arguments.stream().map(new Function<RandomVariable, OperatorTreeNode>() {
						@Override
						public OperatorTreeNode apply(final RandomVariable x) {
							return (x != null && x instanceof RandomVariableDifferentiableAADPathwise) ? ((RandomVariableDifferentiableAADPathwise)x).getOperatorTreeNode(): null;
						}
					}).collect(Collectors.toList()) : null,
							arguments != null ? arguments.stream().map(new Function<RandomVariable, RandomVariable>() {
								@Override
								public RandomVariable apply(final RandomVariable x) {
									return (x != null && x instanceof RandomVariableDifferentiableAADPathwise) ? ((RandomVariableDifferentiableAADPathwise)x).getValues() : x;
								}
							}).collect(Collectors.toList()) : null
					);

		}
		OperatorTreeNode(final OperatorType operator, final List<OperatorTreeNode> arguments, final List<RandomVariable> argumentValues) {
			super();
			id = indexOfNextRandomVariable.getAndIncrement();
			this.operator = operator;
			this.arguments = arguments;
			// This is the simple modification which reduces memory requirements.
			this.argumentValues = argumentValues;
		}

		private void propagateDerivativesFromResultToArgument(final Map<Long, RandomVariable> derivatives) {

			for(final OperatorTreeNode argument : arguments) {
				if(argument != null) {
					final Long argumentID = argument.id;
					if(!derivatives.containsKey(argumentID)) {
						derivatives.put(argumentID, new RandomVariableFromDoubleArray(0.0));
					}

					final RandomVariable partialDerivative	= getPartialDerivative(argument);
					final RandomVariable derivative			= derivatives.get(id);
					RandomVariable argumentDerivative	= derivatives.get(argumentID);

					argumentDerivative = argumentDerivative.addProduct(partialDerivative, derivative);

					derivatives.put(argumentID, argumentDerivative);
				}
			}
		}

		private RandomVariable getPartialDerivative(final OperatorTreeNode differential){

			if(!arguments.contains(differential)) {
				return new RandomVariableFromDoubleArray(0.0);
			}

			final int differentialIndex = arguments.indexOf(differential);
			final RandomVariable X = arguments.size() > 0 && argumentValues != null ? argumentValues.get(0) : null;
			final RandomVariable Y = arguments.size() > 1 && argumentValues != null ? argumentValues.get(1) : null;
			final RandomVariable Z = arguments.size() > 2 && argumentValues != null ? argumentValues.get(2) : null;

			RandomVariable resultrandomvariable = null;

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
				resultrandomvariable = new RandomVariableFromDoubleArray(X.size()).invert();
				break;
			case VARIANCE:
				resultrandomvariable = X.sub(X.getAverage()*(2.0*X.size()-1.0)/X.size()).mult(2.0/X.size());
				break;
			case STDEV:
				resultrandomvariable = X.sub(X.getAverage()*(2.0*X.size()-1.0)/X.size()).mult(2.0/X.size()).mult(0.5).div(Math.sqrt(X.getVariance()));
				break;
			case MIN:
				final double min = X.getMin();
				resultrandomvariable = X.apply(new DoubleUnaryOperator() {
					@Override
					public double applyAsDouble(final double x) {
						return (x == min) ? 1.0 : 0.0;
					}
				});
				break;
			case MAX:
				final double max = X.getMax();
				resultrandomvariable = X.apply(new DoubleUnaryOperator() {
					@Override
					public double applyAsDouble(final double x) {
						return (x == max) ? 1.0 : 0.0;
					}
				});
				break;
			case ABS:
				resultrandomvariable = X.choose(new RandomVariableFromDoubleArray(1.0), new RandomVariableFromDoubleArray(-1.0));
				break;
			case STDERROR:
				resultrandomvariable = X.sub(X.getAverage()*(2.0*X.size()-1.0)/X.size()).mult(2.0/X.size()).mult(0.5).div(Math.sqrt(X.getVariance() * X.size()));
				break;
			case SVARIANCE:
				resultrandomvariable = X.sub(X.getAverage()*(2.0*X.size()-1.0)/X.size()).mult(2.0/(X.size()-1));
				break;
			case ADD:
				resultrandomvariable = X.size() > 1 ? new RandomVariableFromDoubleArray(0.0, X.size(), 1.0) : new RandomVariableFromDoubleArray(1.0);
				break;
			case SUB:
				resultrandomvariable = new RandomVariableFromDoubleArray(differentialIndex == 0 ? 1.0 : -1.0);
				break;
			case MULT:
				resultrandomvariable = differentialIndex == 0 ? Y : X;
				break;
			case DIV:
				resultrandomvariable = differentialIndex == 0 ? Y.invert() : X.div(Y.squared()).mult(-1);
				break;
			case CAP:
				if(differentialIndex == 0) {
					resultrandomvariable = X.sub(Y).choose(new RandomVariableFromDoubleArray(0.0), new RandomVariableFromDoubleArray(1.0));
				}
				else {
					resultrandomvariable = X.sub(Y).choose(new RandomVariableFromDoubleArray(1.0), new RandomVariableFromDoubleArray(0.0));
				}
				break;
			case FLOOR:
				if(differentialIndex == 0) {
					resultrandomvariable = X.sub(Y).choose(new RandomVariableFromDoubleArray(1.0), new RandomVariableFromDoubleArray(0.0));
				}
				else {
					resultrandomvariable = X.sub(Y).choose(new RandomVariableFromDoubleArray(0.0), new RandomVariableFromDoubleArray(1.0));
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
				resultrandomvariable = (differentialIndex == 0) ? Y.mult(X.pow(Y.getAverage() - 1.0)) : new RandomVariableFromDoubleArray(0.0);
				break;
			case ADDPRODUCT:
				if(differentialIndex == 0) {
					resultrandomvariable = new RandomVariableFromDoubleArray(1.0);
				} else if(differentialIndex == 1) {
					resultrandomvariable = Z;
				} else {
					resultrandomvariable = Y;
				}
				break;
			case ADDRATIO:
				if(differentialIndex == 0) {
					resultrandomvariable = new RandomVariableFromDoubleArray(1.0);
				} else if(differentialIndex == 1) {
					resultrandomvariable = Z.invert();
				} else {
					resultrandomvariable = Y.div(Z.squared());
				}
				break;
			case SUBRATIO:
				if(differentialIndex == 0) {
					resultrandomvariable = new RandomVariableFromDoubleArray(1.0);
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
					resultrandomvariable = X.apply(new DoubleUnaryOperator() {
						@Override
						public double applyAsDouble(final double x) {
							return (x == 0.0) ? Double.POSITIVE_INFINITY : 0.0;
						}
					});
				} else if(differentialIndex == 1) {
					resultrandomvariable = X.choose(new RandomVariableFromDoubleArray(1.0), new RandomVariableFromDoubleArray(0.0));
				} else {
					resultrandomvariable = X.choose(new RandomVariableFromDoubleArray(0.0), new RandomVariableFromDoubleArray(1.0));
				}
			default:
				break;
			}

			return resultrandomvariable;
		}
	}

	private final RandomVariable values;
	private final OperatorTreeNode operatorTreeNode;

	public static RandomVariableDifferentiableAADPathwise of(final double value) {
		return new RandomVariableDifferentiableAADPathwise(value);
	}

	public static RandomVariableDifferentiableAADPathwise of(final RandomVariable randomVariable) {
		return new RandomVariableDifferentiableAADPathwise(randomVariable);
	}

	public RandomVariableDifferentiableAADPathwise(final double value) {
		this(new RandomVariableFromDoubleArray(value), null, null);
	}

	public RandomVariableDifferentiableAADPathwise(final double time, final double[] realisations) {
		this(new RandomVariableFromDoubleArray(time, realisations), null, null);
	}

	public RandomVariableDifferentiableAADPathwise(final RandomVariable randomVariable) {
		this(randomVariable, null, null);
	}

	private RandomVariableDifferentiableAADPathwise(final RandomVariable values, final List<RandomVariable> arguments, final OperatorType operator) {
		super();
		this.values = values;
		operatorTreeNode = new OperatorTreeNode(operator, arguments);
	}

	public RandomVariable getRandomVariable() {
		return values;
	}

	public OperatorTreeNode getOperatorTreeNode() {
		return operatorTreeNode;
	}

	@Override
	public Long getID(){
		return getOperatorTreeNode().id;
	}

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

	/* for all functions that need to be differentiated and are returned as double in the Interface, write a method to return it as RandomVariableAAD
	 * that is deterministic by its nature. For their double-returning pendant just return the average of the deterministic RandomVariableAAD  */

	public RandomVariable getAverageAsRandomVariableAAD(final RandomVariable probabilities) {
		/*returns deterministic AAD random variable */
		return new RandomVariableDifferentiableAADPathwise(
				new RandomVariableFromDoubleArray(getAverage(probabilities)),
				Arrays.asList(this, new RandomVariableFromDoubleArray(probabilities)),
				OperatorType.AVERAGE2);
	}

	public RandomVariable getVarianceAsRandomVariableAAD(final RandomVariable probabilities){
		/*returns deterministic AAD random variable */
		return new RandomVariableDifferentiableAADPathwise(
				new RandomVariableFromDoubleArray(getVariance(probabilities)),
				Arrays.asList(this, new RandomVariableFromDoubleArray(probabilities)),
				OperatorType.VARIANCE2);
	}

	public RandomVariable 	getStandardDeviationAsRandomVariableAAD(final RandomVariable probabilities){
		/*returns deterministic AAD random variable */
		return new RandomVariableDifferentiableAADPathwise(
				new RandomVariableFromDoubleArray(getStandardDeviation(probabilities)),
				Arrays.asList(this, new RandomVariableFromDoubleArray(probabilities)),
				OperatorType.STDEV2);
	}

	public RandomVariable 	getStandardErrorAsRandomVariableAAD(final RandomVariable probabilities){
		/*returns deterministic AAD random variable */
		return new RandomVariableDifferentiableAADPathwise(
				new RandomVariableFromDoubleArray(getStandardError(probabilities)),
				Arrays.asList(this, new RandomVariableFromDoubleArray(probabilities)),
				OperatorType.STDERROR2);
	}

	public RandomVariable getAverageAsRandomVariableAAD(){
		/*returns deterministic AAD random variable */
		return new RandomVariableDifferentiableAADPathwise(
				new RandomVariableFromDoubleArray(getAverage()),
				Arrays.asList(new RandomVariable[]{ this }),
				OperatorType.AVERAGE);
	}

	public RandomVariable getVarianceAsRandomVariableAAD(){
		/*returns deterministic AAD random variable */
		return new RandomVariableDifferentiableAADPathwise(
				new RandomVariableFromDoubleArray(getVariance()),
				Arrays.asList(new RandomVariable[]{ this }),
				OperatorType.VARIANCE);
	}

	public RandomVariable getSampleVarianceAsRandomVariableAAD() {
		/*returns deterministic AAD random variable */
		return new RandomVariableDifferentiableAADPathwise(
				new RandomVariableFromDoubleArray(getSampleVariance()),
				Arrays.asList(new RandomVariable[]{ this }),
				OperatorType.SVARIANCE);
	}

	public RandomVariable 	getStandardDeviationAsRandomVariableAAD(){
		/*returns deterministic AAD random variable */
		return new RandomVariableDifferentiableAADPathwise(
				new RandomVariableFromDoubleArray(getStandardDeviation()),
				Arrays.asList(new RandomVariable[]{ this }),
				OperatorType.STDEV);
	}

	public RandomVariable getStandardErrorAsRandomVariableAAD(){
		/*returns deterministic AAD random variable */
		return new RandomVariableDifferentiableAADPathwise(
				new RandomVariableFromDoubleArray(getStandardError()),
				Arrays.asList(new RandomVariable[]{ this }),
				OperatorType.STDERROR);
	}

	public RandomVariable 	getMinAsRandomVariableAAD(){
		/*returns deterministic AAD random variable */
		return new RandomVariableDifferentiableAADPathwise(
				new RandomVariableFromDoubleArray(getMin()),
				Arrays.asList(new RandomVariable[]{ this }),
				OperatorType.MIN);
	}

	public RandomVariable 	getMaxAsRandomVariableAAD(){
		/*returns deterministic AAD random variable */
		return new RandomVariableDifferentiableAADPathwise(
				new RandomVariableFromDoubleArray(getMax()),
				Arrays.asList(new RandomVariable[]{ this }),
				OperatorType.MAX);
	}

	@Override
	public RandomVariable getValues(){
		return values;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#equals(net.finmath.stochastic.RandomVariable)
	 */
	@Override
	public boolean equals(final RandomVariable randomVariable) {
		return getValues().equals(randomVariable);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#getFiltrationTime()
	 */
	@Override
	public double getFiltrationTime() {
		return getValues().getFiltrationTime();
	}

	@Override
	public int getTypePriority() {
		return 3;
	}

	@Override
	public double get(final int pathOrState) {
		return getValues().get(pathOrState);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#size()
	 */
	@Override
	public int size() {
		return getValues().size();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#isDeterministic()
	 */
	@Override
	public boolean isDeterministic() {
		return getValues().isDeterministic();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#getRealizations()
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
	 * @see net.finmath.stochastic.RandomVariable#getMin()
	 */
	@Override
	public double getMin() {
		return getValues().getMin();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#getMax()
	 */
	@Override
	public double getMax() {
		return getValues().getMax();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#getAverage()
	 */
	@Override
	public double getAverage() {
		return getValues().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#getAverage(net.finmath.stochastic.RandomVariable)
	 */
	@Override
	public double getAverage(final RandomVariable probabilities) {
		return getValues().getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#getVariance()
	 */
	@Override
	public double getVariance() {
		return getValues().getVariance();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#getVariance(net.finmath.stochastic.RandomVariable)
	 */
	@Override
	public double getVariance(final RandomVariable probabilities) {
		return getValues().getVariance(probabilities);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#getSampleVariance()
	 */
	@Override
	public double getSampleVariance() {
		return getValues().getSampleVariance();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#getStandardDeviation()
	 */
	@Override
	public double getStandardDeviation() {
		return getValues().getStandardDeviation();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#getStandardDeviation(net.finmath.stochastic.RandomVariable)
	 */
	@Override
	public double getStandardDeviation(final RandomVariable probabilities) {
		return getValues().getStandardDeviation(probabilities);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#getStandardError()
	 */
	@Override
	public double getStandardError() {
		return getValues().getStandardError();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#getStandardError(net.finmath.stochastic.RandomVariable)
	 */
	@Override
	public double getStandardError(final RandomVariable probabilities) {
		return getValues().getStandardError(probabilities);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#getQuantile(double)
	 */
	@Override
	public double getQuantile(final double quantile) {
		return getValues().getQuantile(quantile);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#getQuantile(double, net.finmath.stochastic.RandomVariable)
	 */
	@Override
	public double getQuantile(final double quantile, final RandomVariable probabilities) {
		return ((RandomVariableDifferentiableAADPathwise) getValues()).getValues().getQuantile(quantile, probabilities);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#getQuantileExpectation(double, double)
	 */
	@Override
	public double getQuantileExpectation(final double quantileStart, final double quantileEnd) {
		return ((RandomVariableDifferentiableAADPathwise) getValues()).getValues().getQuantileExpectation(quantileStart, quantileEnd);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#getHistogram(double[])
	 */
	@Override
	public double[] getHistogram(final double[] intervalPoints) {
		return getValues().getHistogram(intervalPoints);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#getHistogram(int, double)
	 */
	@Override
	public double[][] getHistogram(final int numberOfPoints, final double standardDeviations) {
		return getValues().getHistogram(numberOfPoints, standardDeviations);
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
		return new RandomVariableDifferentiableAADPathwise(
				getValues().cap(cap),
				Arrays.asList(this, new RandomVariableFromDoubleArray(cap)),
				OperatorType.CAP);
	}

	@Override
	public RandomVariable floor(final double floor) {
		return new RandomVariableDifferentiableAADPathwise(
				getValues().floor(floor),
				Arrays.asList(this, new RandomVariableFromDoubleArray(floor)),
				OperatorType.FLOOR);
	}

	@Override
	public RandomVariable add(final double value) {
		return new RandomVariableDifferentiableAADPathwise(
				getValues().add(value),
				Arrays.asList(this, new RandomVariableFromDoubleArray(value)),
				OperatorType.ADD);
	}

	@Override
	public RandomVariable sub(final double value) {
		return new RandomVariableDifferentiableAADPathwise(
				getValues().sub(value),
				Arrays.asList(this, new RandomVariableFromDoubleArray(value)),
				OperatorType.SUB);
	}

	@Override
	public RandomVariable mult(final double value) {
		return new RandomVariableDifferentiableAADPathwise(
				getValues().mult(value),
				Arrays.asList(this, new RandomVariableFromDoubleArray(value)),
				OperatorType.MULT);
	}

	@Override
	public RandomVariable div(final double value) {
		return new RandomVariableDifferentiableAADPathwise(
				getValues().div(value),
				Arrays.asList(this, new RandomVariableFromDoubleArray(value)),
				OperatorType.DIV);
	}

	@Override
	public RandomVariable pow(final double exponent) {
		return new RandomVariableDifferentiableAADPathwise(
				getValues().pow(exponent),
				Arrays.asList(this, new RandomVariableFromDoubleArray(exponent)),
				OperatorType.POW);
	}

	@Override
	public RandomVariable average() {
		return new RandomVariableDifferentiableAADPathwise(
				getValues().average(),
				Arrays.asList(new RandomVariable[]{ this }),
				OperatorType.AVERAGE);
	}

	@Override
	public RandomVariable squared() {
		return new RandomVariableDifferentiableAADPathwise(
				getValues().squared(),
				Arrays.asList(new RandomVariable[]{ this }),
				OperatorType.SQUARED);
	}

	@Override
	public RandomVariable sqrt() {
		return new RandomVariableDifferentiableAADPathwise(
				getValues().sqrt(),
				Arrays.asList(new RandomVariable[]{ this }),
				OperatorType.SQRT);
	}

	@Override
	public RandomVariable exp() {
		return new RandomVariableDifferentiableAADPathwise(
				getValues().exp(),
				Arrays.asList(new RandomVariable[]{ this }),
				OperatorType.EXP);
	}

	@Override
	public RandomVariable log() {
		return new RandomVariableDifferentiableAADPathwise(
				getValues().log(),
				Arrays.asList(new RandomVariable[]{ this }),
				OperatorType.LOG);
	}

	@Override
	public RandomVariable sin() {
		return new RandomVariableDifferentiableAADPathwise(
				getValues().sin(),
				Arrays.asList(new RandomVariable[]{ this }),
				OperatorType.SIN);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#cos()
	 */
	@Override
	public RandomVariable cos() {
		return new RandomVariableDifferentiableAADPathwise(
				getValues().cos(),
				Arrays.asList(new RandomVariable[]{ this }),
				OperatorType.COS);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#add(net.finmath.stochastic.RandomVariable)
	 */
	@Override
	public RandomVariable add(final RandomVariable randomVariable) {
		return new RandomVariableDifferentiableAADPathwise(
				getValues().add(randomVariable),
				Arrays.asList(this, randomVariable),
				OperatorType.ADD);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#sub(net.finmath.stochastic.RandomVariable)
	 */
	@Override
	public RandomVariable sub(final RandomVariable randomVariable) {
		return new RandomVariableDifferentiableAADPathwise(
				getValues().sub(randomVariable),
				Arrays.asList(this, randomVariable),
				OperatorType.SUB);
	}

	@Override
	public RandomVariable bus(final RandomVariable randomVariable) {
		return new RandomVariableDifferentiableAADPathwise(
				getValues().bus(randomVariable),
				Arrays.asList(randomVariable, this),
				OperatorType.SUB);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#mult(net.finmath.stochastic.RandomVariable)
	 */
	@Override
	public RandomVariableDifferentiable mult(final RandomVariable randomVariable) {
		return new RandomVariableDifferentiableAADPathwise(
				getValues().mult(randomVariable),
				Arrays.asList(this, randomVariable),
				OperatorType.MULT);
	}

	@Override
	public RandomVariable div(final RandomVariable randomVariable) {
		return new RandomVariableDifferentiableAADPathwise(
				getValues().div(randomVariable),
				Arrays.asList(this, randomVariable),
				OperatorType.DIV);
	}

	@Override
	public RandomVariable vid(final RandomVariable randomVariable) {
		return new RandomVariableDifferentiableAADPathwise(
				getValues().vid(randomVariable),
				Arrays.asList(randomVariable, this),
				OperatorType.DIV);
	}

	@Override
	public RandomVariable cap(final RandomVariable cap) {
		return new RandomVariableDifferentiableAADPathwise(
				getValues().cap(cap),
				Arrays.asList(this, cap),
				OperatorType.CAP);
	}

	@Override
	public RandomVariable floor(final RandomVariable floor) {
		return new RandomVariableDifferentiableAADPathwise(
				getValues().cap(floor),
				Arrays.asList(this, floor),
				OperatorType.FLOOR);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#accrue(net.finmath.stochastic.RandomVariable, double)
	 */
	@Override
	public RandomVariable accrue(final RandomVariable rate, final double periodLength) {
		return new RandomVariableDifferentiableAADPathwise(
				getValues().accrue(rate, periodLength),
				Arrays.asList(this, rate, new RandomVariableFromDoubleArray(periodLength)),
				OperatorType.ACCRUE);
	}

	@Override
	public RandomVariable discount(final RandomVariable rate, final double periodLength) {
		return new RandomVariableDifferentiableAADPathwise(
				getValues().discount(rate, periodLength),
				Arrays.asList(this, rate, new RandomVariableFromDoubleArray(periodLength)),
				OperatorType.DISCOUNT);
	}

	@Override
	public RandomVariable choose(final RandomVariable valueIfTriggerNonNegative, final RandomVariable valueIfTriggerNegative) {
		return new RandomVariableDifferentiableAADPathwise(
				getValues().choose(valueIfTriggerNonNegative.getValues(), valueIfTriggerNegative.getValues()),
				Arrays.asList(this, valueIfTriggerNonNegative, valueIfTriggerNegative),
				OperatorType.BARRIER);
	}

	@Override
	public RandomVariable invert() {
		return new RandomVariableDifferentiableAADPathwise(
				getValues().invert(),
				Arrays.asList(new RandomVariable[]{ this }),
				OperatorType.INVERT);
	}

	@Override
	public RandomVariable abs() {
		return new RandomVariableDifferentiableAADPathwise(
				getValues().abs(),
				Arrays.asList(new RandomVariable[]{ this }),
				OperatorType.ABS);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#addProduct(net.finmath.stochastic.RandomVariable, double)
	 */
	@Override
	public RandomVariable addProduct(final RandomVariable factor1, final double factor2) {
		return new RandomVariableDifferentiableAADPathwise(
				getValues().addProduct(factor1, factor2),
				Arrays.asList(this, factor1, new RandomVariableFromDoubleArray(factor2)),
				OperatorType.ADDPRODUCT);
	}

	@Override
	public RandomVariable addProduct(final RandomVariable factor1, final RandomVariable factor2) {
		return new RandomVariableDifferentiableAADPathwise(
				getValues().addProduct(factor1, factor2),
				Arrays.asList(this, factor1, factor2),
				OperatorType.ADDPRODUCT);
	}

	@Override
	public RandomVariable addRatio(final RandomVariable numerator, final RandomVariable denominator) {
		return new RandomVariableDifferentiableAADPathwise(
				getValues().addRatio(numerator, denominator),
				Arrays.asList(this, numerator, denominator),
				OperatorType.ADDRATIO);
	}

	@Override
	public RandomVariable subRatio(final RandomVariable numerator, final RandomVariable denominator) {
		return new RandomVariableDifferentiableAADPathwise(
				getValues().subRatio(numerator, denominator),
				Arrays.asList(this, numerator, denominator),
				OperatorType.SUBRATIO);
	}

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
}
