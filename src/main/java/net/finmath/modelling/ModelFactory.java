/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2018
 */

package net.finmath.modelling;

/**
 * 
 * @author Christian Fries
 * @author Luca Del Re
 */
public interface ModelFactory<T extends ModelDescriptor> {
	
		Model<?> getModelFromDescription(T description);
}
