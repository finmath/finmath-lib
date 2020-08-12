package net.finmath.singleswaprate.model;

import java.util.Map;
import java.util.Set;

import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.singleswaprate.model.volatilities.VolatilityCube;

/**
 * A collection of objects representing analytic valuations. In addition to the curves and volatility surfaces the base interface handles, this also includes volatility cubes.
 *
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public interface VolatilityCubeModel extends AnalyticModel, Cloneable {

	/**
	 * Get a volatility cube by a given name.
	 *
	 * @param name The name of the volatility cube.
	 * @return The cube with the corresponding name, given that it is part of this model, otherwise null is return.
	 */
	VolatilityCube getVolatilityCube(String name);

	/**
	 * Add a reference to the given volatility cube to this model.
	 *
	 * @param volatilityCube The cube.
	 * @return A clone of this model, with the given cube added or overwritten.
	 */
	VolatilityCubeModel addVolatilityCube(VolatilityCube volatilityCube);

	/**
	 * Add a reference to the given volatility cube to this model under the name provided.
	 *
	 * @param volatilityCubeName The name under which this cube is to known in the model.
	 * @param volatilityCube The cube.
	 * @return A clone of this model, with the given cube added or overwritten under the name provided.
	 */
	VolatilityCubeModel addVolatilityCube(String volatilityCubeName, VolatilityCube volatilityCube);

	/**
	 * Return a Set view of all volatility cubes of this model.
	 *
	 * @return The set containing all names of volatility cubes referenced in this model.
	 */
	Set<String> getVolatilityCubeNames();

	/**
	 * Returns an unmodifiable map of all volatility cubes in the model.
	 *
	 * @return Map of all volatility cubes.
	 */
	Map<String, VolatilityCube> getVolatilityCubes();

}
