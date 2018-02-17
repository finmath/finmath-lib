/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 09.02.2018
 */

package net.finmath.experimental.model;

/**
 * 
 * @author Christian Fries
 * @author Luca Del Re
 */
public interface ModelFactory<T extends ModelDescriptor> {
	
		Model<?> getModelFromDescription(T description);
}
