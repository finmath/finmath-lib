/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2018
 */

package net.finmath.modelling;

/**
 * A factory to instantiate a model from a given descriptor.
 *
 * Factories are specific to a numerical method.
 *
 * @author Christian Fries
 * @author Luca Del Re
 *
 * @param <T> An interface extending model descriptor given the descriptor describing the class impelmenting this interface.
 * @version 1.0
 */
public interface ModelFactory<T extends ModelDescriptor> {

	/**
	 * Get the model for the given descriptor.
	 *
	 * @param descriptor An object being able to describe the given model.
	 * @return The model.
	 */
	DescribedModel<? extends T> getModelFromDescriptor(T descriptor);
}
