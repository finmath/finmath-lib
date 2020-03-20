/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 26.11.2012
 */
package net.finmath.marketdata2.calibration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import net.finmath.marketdata2.model.curves.Curve;
import net.finmath.stochastic.RandomVariable;

/**
 * Combine a set of parameter vectors to a single parameter vector.
 *
 * The class is a little helper class which can combine a set of double[] parameter-vectors to a
 * single large double[] parameter-vector. More precisely: this class combines a set of objects
 * implementing ParameterObjectInterface to a single object by implementing ParameterObjectInterface
 * with the combined parameter.
 *
 * An application is an optimization problem which depends on different parametrized objects, using a solver/optimizer
 * which operates an a single parameter vector.
 *
 * @author Christian Fries
 *
 * @param <E> A class implementing the ParameterObjectInterface
 * @version 1.0
 */
public class ParameterAggregation<E extends ParameterObject> implements ParameterObject {

	private final Set<ParameterObject> parameters;

	/**
	 * Create a collection of parametrized objects.
	 */
	public ParameterAggregation() {
		this.parameters = new LinkedHashSet<>();
	}

	/**
	 * Create a collection of parametrized objects. The constructor will internally create a
	 * (shallow) copy of the set passed to it.
	 *
	 * @param parameters A set of objects implementing ParameterObjectInterface to be combined to a single object.
	 */
	public ParameterAggregation(final Set<E> parameters) {
		this.parameters = new LinkedHashSet<>(parameters);
	}

	/**
	 * Create a collection of parametrized objects. The constructor will internally create a
	 * (shallow) copy of the set passed to it.
	 *
	 * @param parameters A set of objects implementing ParameterObjectInterface to be combined to a single object.
	 */
	public ParameterAggregation(final E[] parameters) {
		this.parameters = new LinkedHashSet<>(Arrays.asList(parameters));
	}

	/**
	 * Add an object this parameterization.
	 *
	 * @param parameterObject The parameter object to add to this parameterization
	 */
	public void add(final E parameterObject) {
		parameters.add(parameterObject);
	}

	/**
	 * Remove an object from this parameterization.
	 *
	 * @param parameterObject The parameter object to remove.
	 */
	public void remove(final E parameterObject) {
		parameters.remove(parameterObject);
	}

	@Override
	public RandomVariable[] getParameter() {
		// Calculate the size of the total parameter vector
		int parameterArraySize = 0;
		for(final ParameterObject parameterVector : parameters) {
			if(parameterVector.getParameter() != null) {
				parameterArraySize += parameterVector.getParameter().length;
			}
		}

		final RandomVariable[] parameterArray = new RandomVariable[parameterArraySize];

		// Copy parameter object parameters to aggregated parameter vector
		int parameterIndex = 0;
		for(final ParameterObject parameterVector : parameters) {
			final RandomVariable[] parameterVectorOfDouble = parameterVector.getParameter();
			if(parameterVectorOfDouble != null) {
				System.arraycopy(parameterVectorOfDouble, 0, parameterArray, parameterIndex, parameterVectorOfDouble.length);
				parameterIndex += parameterVectorOfDouble.length;
			}
		}

		return parameterArray;
	}

	@Override
	public void setParameter(final RandomVariable[] parameter) {
		int parameterIndex = 0;
		for(final ParameterObject parametrizedObject : parameters) {
			final RandomVariable[] parameterVectorOfDouble = parametrizedObject.getParameter();
			if(parameterVectorOfDouble != null) {
				// Copy parameter starting from parameterIndex to parameterVectorOfDouble
				System.arraycopy(parameter, parameterIndex, parameterVectorOfDouble, 0, parameterVectorOfDouble.length);
				parameterIndex += parameterVectorOfDouble.length;
				parametrizedObject.setParameter(parameterVectorOfDouble);
			}
		}
	}

	public Map<E, RandomVariable[]> getObjectsToModifyForParameter(final RandomVariable[] parameter) {
		final Map<E, RandomVariable[]> result = new HashMap<>();
		int parameterIndex = 0;
		for(final ParameterObject parametrizedObject : parameters) {
			final RandomVariable[] parameterVectorOfDouble = parametrizedObject.getParameter().clone();
			if(parameterVectorOfDouble != null) {
				// Copy parameter starting from parameterIndex to parameterVectorOfDouble
				System.arraycopy(parameter, parameterIndex, parameterVectorOfDouble, 0, parameterVectorOfDouble.length);
				parameterIndex += parameterVectorOfDouble.length;
				result.put((E)parametrizedObject, parameterVectorOfDouble);
			}
		}
		return result;
	}

	@Override
	public Curve getCloneForParameter(final RandomVariable[] value) throws CloneNotSupportedException {
		throw new UnsupportedOperationException("Method getCloneForParameter not supported on an aggregate.");
	}
}
