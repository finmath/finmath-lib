/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christianfries.com.
 *
 * Created on 17.06.2017
 */
package net.finmath.montecarlo.automaticdifferentiation.backward;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
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
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiable;
import net.finmath.montecarlo.automaticdifferentiation.backward.RandomVariableDifferentiableAADFactory.DiracDeltaApproximationMethod;
import net.finmath.montecarlo.conditionalexpectation.LinearRegression;
import net.finmath.stochastic.ConditionalExpectationEstimator;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;

/**
 * Implementation of <code>RandomVariableDifferentiable</code> using
 * the backward algorithmic differentiation (adjoint algorithmic differentiation, AAD).
 *
 * This class implements the optimized stochastic ADD as it is described in
 * <a href="https://ssrn.com/abstract=2995695">ssrn.com/abstract=2995695</a>.
 *
 * The class implements the special treatment of the conditional expectation operator as it is described in
 * <a href="https://ssrn.com/abstract=3000822">ssrn.com/abstract=3000822</a>.
 *
 * The class implements the special treatment of indicator functions as it is described in
 * <a href="https://ssrn.com/abstract=3282667">ssrn.com/abstract=3282667</a>.
 *
 * For details see <a href="http://christianfries.com/finmath/stochasticautodiff/">http://christianfries.com/finmath/stochasticautodiff/</a>.
 *
 * The class is serializable. Upon de-serialization the value of {@link #getID()} may be changed to ensure unique IDs in de-serialization context.
 *
 * @author Christian Fries
 * @author Stefan Sedlmair
 * @version 1.1
 */
public class RandomVariableDifferentiableAAD implements RandomVariableDifferentiable {

	private static final long serialVersionUID = 2459373647785530657L;

	private static final int typePriorityDefault = 3;

	private static final RandomVariable one = new Scalar(1.0);

	private final int typePriority;

	private static AtomicLong indexOfNextRandomVariable = new AtomicLong(0);

	private enum OperatorType {
		ADD, MULT, DIV, SUB, SQUARED, SQRT, LOG, SIN, COS, EXP, INVERT, CAP, FLOOR, ABS,
		ADDPRODUCT, ADDRATIO, SUBRATIO, CHOOSE, DISCOUNT, ACCRUE, POW, MIN, MAX, AVERAGE, VARIANCE,
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
	private static class OperatorTreeNode implements Serializable {

		private static final long serialVersionUID = -8428352552169568990L;

		private final Long id;
		private final OperatorType operatorType;
		private final List<OperatorTreeNode> arguments;
		private final List<RandomVariable> argumentValues;
		private final Object operator;
		private final RandomVariableDifferentiableAADFactory factory;

		private static final RandomVariable zero = new Scalar(0.0);
		private static final RandomVariable one = new Scalar(1.0);
		private static final RandomVariable minusOne = new Scalar(-1.0);

		OperatorTreeNode(final OperatorType operatorType, final List<OperatorTreeNode> arguments, List<RandomVariable> argumentValues, final Object operator, final RandomVariableDifferentiableAADFactory factory) {
			super();
			id = indexOfNextRandomVariable.getAndIncrement();
			this.operatorType = operatorType;
			this.arguments = arguments;
			this.operator = operator;
			this.factory = factory;

			/*
			 * This is the simple modification which reduces memory requirements.
			 */
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
			else if(operatorType != null && operatorType.equals(OperatorType.CHOOSE)) {
				if(arguments.get(0) == null) {
					argumentValues.set(1, null);
					argumentValues.set(2, null);
				}
			}

			this.argumentValues = argumentValues;
		}

		/*
		 * This implements the update rule D_i = D_i + Dm * d fm/dxi where i are the arguments of this node and m is this node.
		 */
		private void propagateDerivativesFromResultToArgument(final Map<Long, RandomVariable> derivatives) {
			if(arguments == null) {
				// The node has no arguments (it is a leaf node the tree). Do nothing.
				return;
			}

			for(int argumentIndex = 0; argumentIndex < arguments.size(); argumentIndex++) {
				final OperatorTreeNode argument = arguments.get(argumentIndex);
				if(argument != null) {
					final Long argumentID = argument.id;

					final RandomVariable partialDerivative	= getPartialDerivative(argument, argumentIndex);
					RandomVariable derivative			= derivatives.get(id);
					RandomVariable argumentDerivative	= derivatives.get(argumentID);

					/*
					 * Special treatment of some stochastic operators
					 */
					switch(operatorType) {
					case AVERAGE:
						// Implementation of AVERAGE (see https://ssrn.com/abstract=2995695 for details).
						derivative = derivative.average();
						break;
					case CONDITIONAL_EXPECTATION:
						// Implementation of CONDITIONAL_EXPECTATION (see https://ssrn.com/abstract=2995695 for details).
						final ConditionalExpectationEstimator estimator = (ConditionalExpectationEstimator)operator;
						derivative = estimator.getConditionalExpectation(derivative);
						break;
					case CHOOSE:
						// Implementation of CHOOSE (INDICATOR_FUNCTION)
						if(argumentIndex == 0 && (factory.getDiracDeltaApproximationMethod() == DiracDeltaApproximationMethod.REGRESSION_ON_DENSITY || factory.getDiracDeltaApproximationMethod() == DiracDeltaApproximationMethod.REGRESSION_ON_DISTRIBUITON)) {
							derivative = getDiracDeltaRegression(derivative, argumentValues.get(0));
						}
						break;
					default:
						// Ordinary operator - nothing to do
						break;
					}

					/*
					 * Add the product of current nodes derivative and the vertex partialDerivative to the argument derivative
					 */
					if(argumentDerivative == null) {
						// argumentDerivative is zero. Initialize value
						argumentDerivative = derivative.mult(partialDerivative);
					}
					else {
						// Add product to given value
						argumentDerivative = argumentDerivative.addProduct(partialDerivative, derivative);
					}

					derivatives.put(argumentID, argumentDerivative);
				}
			}
		}

		/**
		 * Calculate the partial derivative of this node with respect to an argument node.
		 * Since a function f may use an argument node X in multiple arguments, say f(X,X), we need to provide index
		 * of the argument with respect to which the differentiation is performed (thanks to Vincent E. for pointing to this).
		 *
		 * @param differential The node of the argument.
		 * @param differentialIndex The index of the argument in the functions argument list.
		 * @return The value of the partial derivative.
		 */
		private RandomVariable getPartialDerivative(final OperatorTreeNode differential, final int differentialIndex) {

			if(!arguments.contains(differential)) {
				return zero;
			}

			final RandomVariable X = arguments.size() > 0 && argumentValues != null ? argumentValues.get(0) : null;
			final RandomVariable Y = arguments.size() > 1 && argumentValues != null ? argumentValues.get(1) : null;
			final RandomVariable Z = arguments.size() > 2 && argumentValues != null ? argumentValues.get(2) : null;

			RandomVariable derivative;

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
				// second argument will always be deterministic and constant (currently pow does not exist with a random variable exponent)
				derivative = (differentialIndex == 0) ? X.pow(Y.doubleValue() - 1.0).mult(Y) : zero;
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
			case CHOOSE:
				if(differentialIndex == 0) {
					switch(factory.getDiracDeltaApproximationMethod()) {
					case ONE:
					{
						derivative = Y.sub(Z);
						break;
					}
					case ZERO:
					{
						derivative = zero;
						break;
					}
					case DISCRETE_DELTA:
					{
						/*
						 * Approximation via local finite difference
						 * (see https://ssrn.com/abstract=2995695 for details).
						 */
						final double epsilon = factory.getDiracDeltaApproximationWidthPerStdDev()*X.getStandardDeviation();
						if(Double.isInfinite(epsilon)) {
							derivative = Y.sub(Z);
						}
						else if(epsilon > 0) {
							derivative = Y.sub(Z);
							derivative = derivative.mult(X.add(epsilon/2).choose(one, zero));
							derivative = derivative.mult(X.sub(epsilon/2).choose(zero, one));
							derivative = derivative.div(epsilon);
						}
						else {
							derivative = zero;
						}
						break;
					}
					case REGRESSION_ON_DENSITY:
					case REGRESSION_ON_DISTRIBUITON:
					{
						derivative = Y.sub(Z);
						break;
					}
					default:
					{
						throw new UnsupportedOperationException("Diract Delta Approximation Method " + factory.getDiracDeltaApproximationMethod().name() + " not supported.");
					}
					}
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

		private RandomVariable getDiracDeltaRegression(RandomVariable derivative, final RandomVariable indicator) {
			final double diracDeltaApproximationWidthPerStdDev = factory.getDiracDeltaApproximationWidthPerStdDev();
			final double epsilon = diracDeltaApproximationWidthPerStdDev*indicator.getStandardDeviation();

			final RandomVariable localizedOne = (indicator.add(epsilon/2).choose(one, zero)).mult(indicator.sub(epsilon/2).choose(zero, one));

			final boolean isDirectDeltaRegressionUseRegressionOnAdjointDerivative = false;	// currently disabled, was used in experiments
			if(isDirectDeltaRegressionUseRegressionOnAdjointDerivative) {
				final RandomVariable localizedValue = indicator.mult(localizedOne);
				final RandomVariable[] regressionBasisFunctions = new RandomVariable[] {
						localizedOne,
						localizedValue,
						localizedValue.squared()
				};
				derivative = localizedOne.mult((new LinearRegression(regressionBasisFunctions)).getRegressionCoefficients(derivative)[0]).div(localizedOne.getAverage());
			}
			else {
				derivative = derivative.mult(localizedOne).div(localizedOne.getAverage());
			}

			return derivative.mult(getDensityRegression(indicator));

		}

		private double getDensityRegression(final RandomVariable indicator) {
			final double diracDeltaApproximationDensityRegressionWidthPerStdDev = factory.getDiracDeltaApproximationDensityRegressionWidthPerStdDev();

			/*
			 * Density regression
			 */
			final double underlyingStdDev = indicator.getStandardDeviation();
			final int numberOfSamplePointsHalf = 50;			// @TODO numberOfSamplePoints should become a parameter.
			final double sampleIntervalWidthHalf = diracDeltaApproximationDensityRegressionWidthPerStdDev/2 * underlyingStdDev / numberOfSamplePointsHalf;
			final double[] samplePointX = new double[numberOfSamplePointsHalf*2];
			final double[] samplePointY = new double[numberOfSamplePointsHalf*2];
			double sampleInterval = sampleIntervalWidthHalf;
			final RandomVariable indicatorPositiveValues = indicator.choose(new Scalar(1.0), new Scalar(0.0));
			final RandomVariable indicatorNegativeValues = indicator.choose(new Scalar(0.0), new Scalar(1.0));

			switch(factory.getDiracDeltaApproximationMethod()) {
			case REGRESSION_ON_DENSITY:
			{
				for(int i=0; i<numberOfSamplePointsHalf*2; i+=2) {
					sampleInterval += sampleIntervalWidthHalf;

					final RandomVariable indicatorOnNegValues = indicator.add(sampleInterval).choose(new Scalar(1.0), new Scalar(0.0)).mult(indicatorNegativeValues);
					final RandomVariable indicatorOnPosValues = indicator.sub(sampleInterval).choose(new Scalar(0.0), new Scalar(1.0)).mult(indicatorPositiveValues);

					samplePointX[i] = -sampleInterval;
					samplePointY[i] = indicatorOnNegValues.getAverage() / sampleInterval;

					samplePointX[i+1] = sampleInterval;
					samplePointY[i+1] = indicatorOnPosValues.getAverage() / sampleInterval;
				}

				final RandomVariable densityX = new RandomVariableFromDoubleArray(0.0, samplePointX);
				final RandomVariable densityValues = new RandomVariableFromDoubleArray(0.0, samplePointY);

				//				final double[] densityRegressionCoeff = new LinearRegression(new RandomVariable[] { densityX.mult(0.0).add(1.0), densityX }).getRegressionCoefficients(densityValues);
				final double[] densityRegressionCoeff = new LinearRegression(new RandomVariable[] { densityX.mult(0.0).add(1.0), densityX, densityX.squared() }).getRegressionCoefficients(densityValues);
				final double density = densityRegressionCoeff[0];

				return density;
			}
			case REGRESSION_ON_DISTRIBUITON:
			{
				for(int i=0; i<numberOfSamplePointsHalf*2; i+=2) {
					sampleInterval += sampleIntervalWidthHalf;

					final RandomVariable indicatorOnNegValues = indicator.add(sampleInterval).choose(new Scalar(1.0), new Scalar(0.0)).mult(indicatorNegativeValues);
					final RandomVariable indicatorOnPosValues = indicator.sub(sampleInterval).choose(new Scalar(0.0), new Scalar(1.0)).mult(indicatorPositiveValues);

					samplePointX[i] = -sampleInterval;
					samplePointY[i] = -indicatorOnNegValues.getAverage();

					samplePointX[i+1] = sampleInterval;
					samplePointY[i+1] = indicatorOnPosValues.getAverage();
				}

				final RandomVariable densityX = new RandomVariableFromDoubleArray(0.0, samplePointX);
				final RandomVariable densityValues = new RandomVariableFromDoubleArray(0.0, samplePointY);
				//				final double[] densityRegressionCoeff = new LinearRegression(new RandomVariable[] { densityX, densityX.squared() }).getRegressionCoefficients(densityValues);
				final double[] densityRegressionCoeff = new LinearRegression(new RandomVariable[] { densityX, densityX.squared(), densityX.pow(3) }).getRegressionCoefficients(densityValues);
				//				final double[] densityRegressionCoeff = new LinearRegression(new RandomVariable[] { densityX, new Scalar(1.0) }).getRegressionCoefficients(densityValues);
				//				final double[] densityRegressionCoeff = new LinearRegression(new RandomVariable[] { densityX }).getRegressionCoefficients(densityValues);
				//				final double[] densityRegressionCoeff = new LinearRegression(new RandomVariable[] { densityX, new Scalar(1.0), densityX.squared(), densityX.pow(3) }).getRegressionCoefficients(densityValues);
				final double density = densityRegressionCoeff[0];

				return density;
			}
			default:
				throw new UnsupportedOperationException("Density regression method " + factory.getDiracDeltaApproximationMethod().name() + " not supported.");
			}
		}

		private static OperatorTreeNode of(final RandomVariable randomVariable) {
			return (randomVariable != null && randomVariable instanceof RandomVariableDifferentiableAAD) ? ((RandomVariableDifferentiableAAD)randomVariable).getOperatorTreeNode() : null;
		}

		private static RandomVariable getValue(final RandomVariable randomVariable) {
			return randomVariable != null ? randomVariable.getValues() : randomVariable;
		}

		private static List<OperatorTreeNode> extractOperatorTreeNodes(final List<RandomVariable> arguments) {
			return arguments != null ? arguments.stream().map( OperatorTreeNode::of ).collect(Collectors.toList()) : null;
		}

		private static List<RandomVariable> extractOperatorValues(final List<RandomVariable> arguments) {
			return arguments != null ? arguments.stream().map( OperatorTreeNode::getValue ).collect(Collectors.toList()) : null;
		}

		private void readObject(final java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
			stream.defaultReadObject();
			// Reassign id
			try {
				final Field idField = this.getClass().getDeclaredField("id");
				idField.setAccessible(true);
				idField.set(this, indexOfNextRandomVariable.getAndIncrement());
				idField.setAccessible(false);
			} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
				throw new RuntimeException("Unable to re-assing id of " + this.getClass().getSimpleName() + ".", e);
			}
		}
	}

	/*
	 * Data model. We maintain the underlying values and a link to the node in the operator tree.
	 */
	private RandomVariable values;
	private final OperatorTreeNode operatorTreeNode;
	private final RandomVariableDifferentiableAADFactory factory;

	public RandomVariableDifferentiableAAD(final RandomVariable values, final List<OperatorTreeNode> argumentOperatorTreeNodes, final List<RandomVariable> argumentValues, final ConditionalExpectationEstimator estimator, final OperatorType operator, final RandomVariableDifferentiableAADFactory factory, final int methodArgumentTypePriority) {
		super();
		this.values = values;
		operatorTreeNode = new OperatorTreeNode(operator, argumentOperatorTreeNodes, argumentValues, estimator, factory);
		this.factory = factory != null ? factory : new RandomVariableDifferentiableAADFactory();
		typePriority = methodArgumentTypePriority;
	}

	public static RandomVariableDifferentiableAAD of(final double value) {
		return new RandomVariableDifferentiableAAD(value);
	}

	public static RandomVariableDifferentiableAAD of(final RandomVariable randomVariable) {
		return new RandomVariableDifferentiableAAD(randomVariable);
	}

	public RandomVariableDifferentiableAAD(final double value) {
		this(new Scalar(value), null, null, null);
	}

	public RandomVariableDifferentiableAAD(final RandomVariable randomVariable) {
		this(randomVariable, null, null, randomVariable instanceof RandomVariableDifferentiableAAD ? ((RandomVariableDifferentiableAAD)randomVariable).getFactory() : null);
	}

	public RandomVariableDifferentiableAAD(final RandomVariable values, final RandomVariableDifferentiableAADFactory factory) {
		this(values, null, null, factory);
	}

	private RandomVariableDifferentiableAAD(final RandomVariable values, final List<RandomVariable> arguments, final OperatorType operator, final RandomVariableDifferentiableAADFactory factory) {
		this(values, arguments, null, operator, factory);
	}

	public RandomVariableDifferentiableAAD(final RandomVariable values, final List<RandomVariable> arguments, final ConditionalExpectationEstimator estimator, final OperatorType operator, final RandomVariableDifferentiableAADFactory factory) {
		this(values, arguments, estimator, operator, factory, typePriorityDefault);
	}

	public RandomVariableDifferentiableAAD(final RandomVariable values, final List<RandomVariable> arguments, final ConditionalExpectationEstimator estimator, final OperatorType operator, final RandomVariableDifferentiableAADFactory factory, final int methodArgumentTypePriority) {
		this(values, OperatorTreeNode.extractOperatorTreeNodes(arguments), OperatorTreeNode.extractOperatorValues(arguments), estimator, operator, factory, methodArgumentTypePriority);
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

	public RandomVariableDifferentiableAADFactory getFactory() {
		return factory;
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
		derivatives.put(getID(), one);

		// The set maintaining the independents. Note: TreeMap is maintaining a sorting on the keys.
		final TreeMap<Long, OperatorTreeNode> independents = new TreeMap<>();
		// Initialize with root node
		independents.put(getID(), getOperatorTreeNode());

		while(independents.size() > 0) {
			// Get and remove node with the highest id in independents
			final Map.Entry<Long, OperatorTreeNode> independentEntry = independents.pollLastEntry();
			final Long id = independentEntry.getKey();
			final OperatorTreeNode independent = independentEntry.getValue();

			// Process this node (node with highest id in independents)
			final List<OperatorTreeNode> arguments = independent.arguments;
			if(arguments != null && arguments.size() > 0) {
				// Node has arguments: Propagate derivative to arguments.
				independent.propagateDerivativesFromResultToArgument(derivatives);

				// Remove id of this node from derivatives - keep only leaf nodes.
				if(isGradientRetainsLeafNodesOnly()) {
					derivatives.remove(id);
				}

				// Add all non leaf node arguments to the list of independents
				for(final OperatorTreeNode argument : arguments) {
					// If an argument is null, it is a (non-differentiable) constant.
					if(argument != null) {
						independents.put(argument.id, argument);
					}
				}
			}

			if(independentIDs != null && independentIDs.contains(id)) {
				derivatives.remove(id);
			}
		}

		return derivatives;
	}

	private boolean isGradientRetainsLeafNodesOnly() {
		return getFactory() != null && getFactory().isGradientRetainsLeafNodesOnly();
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
	 * The following methods are differentiable operations.
	 */

	@Override
	public RandomVariable cache() {
		values = values.cache();
		return this;
	}

	@Override
	public RandomVariable cap(final double cap) {
		return new RandomVariableDifferentiableAAD(
				getValues().cap(cap),
				Arrays.asList(this.getOperatorTreeNode(), null),
				Arrays.asList(this.getValues(), new Scalar(cap)),
				null,
				OperatorType.CAP,
				getFactory(),
				typePriorityDefault);
	}

	@Override
	public RandomVariable floor(final double floor) {
		return new RandomVariableDifferentiableAAD(
				getValues().floor(floor),
				Arrays.asList(this.getOperatorTreeNode(), null),
				Arrays.asList(this.getValues(), new Scalar(floor)),
				null,
				OperatorType.FLOOR,
				getFactory(),
				typePriorityDefault);
	}

	@Override
	public RandomVariable add(final double value) {
		return new RandomVariableDifferentiableAAD(
				getValues().add(value),
				Arrays.asList(this.getOperatorTreeNode(), null),
				Arrays.asList(null /* this */, null /* new RandomVariableFromDoubleArray(value) */),		// For ADD we do not need all arguments to evaluate the differential
				null,
				OperatorType.ADD,
				getFactory(),
				typePriorityDefault);
	}

	@Override
	public RandomVariable sub(final double value) {
		return new RandomVariableDifferentiableAAD(
				getValues().sub(value),
				Arrays.asList(this.getOperatorTreeNode(), null),
				Arrays.asList(null /* this */, null /* new RandomVariableFromDoubleArray(value) */),		// For SUB we do not need all arguments to evaluate the differential
				null,
				OperatorType.SUB,
				getFactory(),
				typePriorityDefault);
	}

	@Override
	public RandomVariable mult(final double value) {
		return new RandomVariableDifferentiableAAD(
				getValues().mult(value),
				Arrays.asList(this.getOperatorTreeNode(), null),
				Arrays.asList(null, new Scalar(value)),		// For MULT with constant we do not need all arguments to evaluate the differential
				null,
				OperatorType.MULT,
				getFactory(),
				typePriorityDefault);
	}

	@Override
	public RandomVariable div(final double value) {
		return new RandomVariableDifferentiableAAD(
				getValues().div(value),
				Arrays.asList(this.getOperatorTreeNode(), null),
				Arrays.asList(null, new Scalar(value)),		// For DIV with constant we do not need all arguments to evaluate the differential
				null,
				OperatorType.DIV,
				getFactory(),
				typePriorityDefault);
	}

	@Override
	public RandomVariable pow(final double exponent) {
		return new RandomVariableDifferentiableAAD(
				getValues().pow(exponent),
				Arrays.asList(this, new Scalar(exponent)),
				OperatorType.POW,
				getFactory());
	}

	@Override
	public RandomVariable average() {
		return new RandomVariableDifferentiableAAD(
				getValues().average(),
				Arrays.asList(new RandomVariable[]{ this }),
				OperatorType.AVERAGE,
				getFactory());
	}

	@Override
	public RandomVariable getConditionalExpectation(final ConditionalExpectationEstimator estimator) {
		return new RandomVariableDifferentiableAAD(
				getValues().getConditionalExpectation(estimator),
				Arrays.asList(new RandomVariable[]{ this }),
				estimator,
				OperatorType.CONDITIONAL_EXPECTATION,
				getFactory());

	}

	@Override
	public RandomVariable squared() {
		return new RandomVariableDifferentiableAAD(
				getValues().squared(),
				Arrays.asList(new RandomVariable[]{ this }),
				OperatorType.SQUARED,
				getFactory());
	}

	@Override
	public RandomVariable sqrt() {
		return new RandomVariableDifferentiableAAD(
				getValues().sqrt(),
				Arrays.asList(new RandomVariable[]{ this }),
				OperatorType.SQRT,
				getFactory());
	}

	@Override
	public RandomVariable exp() {
		return new RandomVariableDifferentiableAAD(
				getValues().exp(),
				Arrays.asList(new RandomVariable[]{ this }),
				OperatorType.EXP,
				getFactory());
	}

	@Override
	public RandomVariable log() {
		return new RandomVariableDifferentiableAAD(
				getValues().log(),
				Arrays.asList(new RandomVariable[]{ this }),
				OperatorType.LOG,
				getFactory());
	}

	@Override
	public RandomVariable sin() {
		return new RandomVariableDifferentiableAAD(
				getValues().sin(),
				Arrays.asList(new RandomVariable[]{ this }),
				OperatorType.SIN,
				getFactory());
	}

	@Override
	public RandomVariable cos() {
		return new RandomVariableDifferentiableAAD(
				getValues().cos(),
				Arrays.asList(new RandomVariable[]{ this }),
				OperatorType.COS,
				getFactory());
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

		return new RandomVariableDifferentiableAAD(
				getValues().add(randomVariable.getValues()),
				Arrays.asList(this.getOperatorTreeNode(), OperatorTreeNode.of(randomVariable)),
				Arrays.asList(null /* this.getValues() */, null /* randomVariable.getValues() */),		// For ADD we do not need all arguments to evaluate the differential
				null,
				OperatorType.ADD,
				getFactory(),
				typePriorityDefault);
	}

	@Override
	public RandomVariable sub(final RandomVariable randomVariable) {
		if(randomVariable.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return randomVariable.bus(this);
		}

		return new RandomVariableDifferentiableAAD(
				getValues().sub(randomVariable.getValues()),
				Arrays.asList(this.getOperatorTreeNode(), OperatorTreeNode.of(randomVariable)),
				Arrays.asList(null /* this.getValues() */, null /* randomVariable.getValues() */),		// For SUB we do not need all arguments to evaluate the differential
				null,
				OperatorType.SUB,
				getFactory(),
				typePriorityDefault);
	}

	@Override
	public RandomVariable bus(final RandomVariable randomVariable) {
		if(randomVariable.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return randomVariable.sub(this);
		}

		return new RandomVariableDifferentiableAAD(
				getValues().bus(randomVariable.getValues()),
				Arrays.asList(OperatorTreeNode.of(randomVariable), this.getOperatorTreeNode()),			// SUB with swapped arguments
				Arrays.asList(null, null),																// For SUB we do not need all arguments to evaluate the differential
				null,
				OperatorType.SUB,
				getFactory(),
				typePriorityDefault);
	}

	@Override
	public RandomVariable mult(final RandomVariable randomVariable) {
		if(randomVariable.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return randomVariable.mult(this);
		}

		return new RandomVariableDifferentiableAAD(
				getValues().mult(randomVariable.getValues()),
				Arrays.asList(this.getOperatorTreeNode(), OperatorTreeNode.of(randomVariable)),
				Arrays.asList(this.getValues(), randomVariable.getValues()),
				null,
				OperatorType.MULT,
				getFactory(),
				typePriorityDefault);
	}

	@Override
	public RandomVariable div(final RandomVariable randomVariable) {
		if(randomVariable.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return randomVariable.vid(this);
		}

		return new RandomVariableDifferentiableAAD(
				getValues().div(randomVariable.getValues()),
				Arrays.asList(this.getOperatorTreeNode(), OperatorTreeNode.of(randomVariable)),
				Arrays.asList(this.getValues(), randomVariable.getValues()),
				null,
				OperatorType.DIV,
				getFactory(),
				typePriorityDefault);
	}

	@Override
	public RandomVariable vid(final RandomVariable randomVariable) {
		if(randomVariable.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return randomVariable.div(this);
		}

		return new RandomVariableDifferentiableAAD(
				getValues().vid(randomVariable.getValues()),
				Arrays.asList(OperatorTreeNode.of(randomVariable), this.getOperatorTreeNode()),	// DIV with swapped arguments
				Arrays.asList(randomVariable.getValues(), this.getValues()),					// DIV with swapped arguments
				null,
				OperatorType.DIV,
				getFactory(),
				typePriorityDefault);
	}

	@Override
	public RandomVariable cap(final RandomVariable randomVariable) {
		if(randomVariable.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return randomVariable.cap(this);
		}

		return new RandomVariableDifferentiableAAD(
				getValues().cap(randomVariable.getValues()),
				Arrays.asList(this, randomVariable),
				OperatorType.CAP,
				getFactory());
	}

	@Override
	public RandomVariable floor(final RandomVariable floor) {
		if(floor.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return floor.floor(this);
		}

		return new RandomVariableDifferentiableAAD(
				getValues().floor(floor.getValues()),
				Arrays.asList(this, floor),
				OperatorType.FLOOR,
				getFactory());
	}

	@Override
	public RandomVariable accrue(final RandomVariable rate, final double periodLength) {
		if(rate.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return rate.mult(periodLength).add(1.0).mult(this);
		}

		return new RandomVariableDifferentiableAAD(
				getValues().accrue(rate.getValues(), periodLength),
				Arrays.asList(this, rate, new RandomVariableFromDoubleArray(periodLength)),
				OperatorType.ACCRUE,
				getFactory());
	}

	@Override
	public RandomVariable discount(final RandomVariable rate, final double periodLength) {
		if(rate.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return rate.mult(periodLength).add(1.0).invert().mult(this);
		}

		return new RandomVariableDifferentiableAAD(
				getValues().discount(rate.getValues(), periodLength),
				Arrays.asList(this, rate, new RandomVariableFromDoubleArray(periodLength)),
				OperatorType.DISCOUNT,
				getFactory());
	}

	@Override
	public RandomVariable choose(final RandomVariable valueIfTriggerNonNegative, final RandomVariable valueIfTriggerNegative) {
		return new RandomVariableDifferentiableAAD(
				getValues().choose(valueIfTriggerNonNegative.getValues(), valueIfTriggerNegative.getValues()),
				Arrays.asList(this.getOperatorTreeNode(), OperatorTreeNode.of(valueIfTriggerNonNegative), OperatorTreeNode.of(valueIfTriggerNegative)),
				Arrays.asList(this.getValues(), valueIfTriggerNonNegative.getValues(), valueIfTriggerNegative.getValues()),
				null,
				OperatorType.CHOOSE,
				getFactory(),
				typePriorityDefault);
	}

	@Override
	public RandomVariable invert() {
		return new RandomVariableDifferentiableAAD(
				getValues().invert(),
				Arrays.asList(new RandomVariable[]{ this }),
				OperatorType.INVERT,
				getFactory());
	}

	@Override
	public RandomVariable abs() {
		return new RandomVariableDifferentiableAAD(
				getValues().abs(),
				Arrays.asList(new RandomVariable[]{ this }),
				OperatorType.ABS,
				getFactory());
	}

	@Override
	public RandomVariable addProduct(final RandomVariable factor1, final double factor2) {
		if(factor1.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return factor1.mult(factor2).add(this);
		}

		return new RandomVariableDifferentiableAAD(
				getValues().addProduct(factor1.getValues(), factor2),
				Arrays.asList(this.getOperatorTreeNode(), OperatorTreeNode.of(factor1), null),
				Arrays.asList(this.getValues(), factor1.getValues(), new Scalar(factor2)),
				null,
				OperatorType.ADDPRODUCT,
				getFactory(),
				typePriorityDefault);
	}

	@Override
	public RandomVariable addProduct(final RandomVariable factor1, final RandomVariable factor2) {
		if(factor1.getTypePriority() > this.getTypePriority() || factor2.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return factor1.mult(factor2).add(this);
		}

		return new RandomVariableDifferentiableAAD(
				getValues().addProduct(factor1.getValues(), factor2.getValues()),
				Arrays.asList(this.getOperatorTreeNode(), OperatorTreeNode.of(factor1), OperatorTreeNode.of(factor2)),
				Arrays.asList(this.getValues(), factor1.getValues(), factor2.getValues()),
				null,
				OperatorType.ADDPRODUCT,
				getFactory(),
				typePriorityDefault);
	}

	@Override
	public RandomVariable addRatio(final RandomVariable numerator, final RandomVariable denominator) {
		if(numerator.getTypePriority() > this.getTypePriority() || denominator.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return numerator.div(denominator).add(this);
		}

		return new RandomVariableDifferentiableAAD(
				getValues().addRatio(numerator.getValues(), denominator.getValues()),
				Arrays.asList(this, numerator, denominator),
				OperatorType.ADDRATIO,
				getFactory());
	}

	@Override
	public RandomVariable subRatio(final RandomVariable numerator, final RandomVariable denominator) {
		if(numerator.getTypePriority() > this.getTypePriority() || denominator.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return numerator.div(denominator).mult(-1).add(this);
		}

		return new RandomVariableDifferentiableAAD(
				getValues().subRatio(numerator.getValues(), denominator.getValues()),
				Arrays.asList(this, numerator, denominator),
				OperatorType.SUBRATIO,
				getFactory());
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
		return new RandomVariableDifferentiableAAD(
				new RandomVariableFromDoubleArray(getVariance()),
				Arrays.asList(new RandomVariable[]{ this }),
				OperatorType.VARIANCE,
				getFactory());
	}

	public RandomVariable getSampleVarianceAsRandomVariableAAD() {
		/*returns deterministic AAD random variable */
		return new RandomVariableDifferentiableAAD(
				new RandomVariableFromDoubleArray(getSampleVariance()),
				Arrays.asList(new RandomVariable[]{ this }),
				OperatorType.SVARIANCE,
				getFactory());
	}

	public RandomVariable 	getStandardDeviationAsRandomVariableAAD(){
		/*returns deterministic AAD random variable */
		return new RandomVariableDifferentiableAAD(
				new RandomVariableFromDoubleArray(getStandardDeviation()),
				Arrays.asList(new RandomVariable[]{ this }),
				OperatorType.STDEV,
				getFactory());
	}

	public RandomVariable getStandardErrorAsRandomVariableAAD(){
		/*returns deterministic AAD random variable */
		return new RandomVariableDifferentiableAAD(
				new RandomVariableFromDoubleArray(getStandardError()),
				Arrays.asList(new RandomVariable[]{ this }),
				OperatorType.STDERROR,
				getFactory());
	}

	public RandomVariable 	getMinAsRandomVariableAAD(){
		/*returns deterministic AAD random variable */
		return new RandomVariableDifferentiableAAD(
				new RandomVariableFromDoubleArray(getMin()),
				Arrays.asList(new RandomVariable[]{ this }),
				OperatorType.MIN,
				getFactory());
	}

	public RandomVariable 	getMaxAsRandomVariableAAD(){
		/*returns deterministic AAD random variable */
		return new RandomVariableDifferentiableAAD(
				new RandomVariableFromDoubleArray(getMax()),
				Arrays.asList(new RandomVariable[]{ this }),
				OperatorType.MAX,
				getFactory());
	}

	@Override
	public String toString() {
		return "RandomVariableDifferentiableAAD [values=" + values + ",\n ID=" + getID() + "]";
	}

	@Override
	public RandomVariableDifferentiable getCloneIndependent() {
		return new RandomVariableDifferentiableAAD(this.getValues());
	}
}
