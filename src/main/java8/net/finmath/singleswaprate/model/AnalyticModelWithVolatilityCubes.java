package net.finmath.singleswaprate.model;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.finmath.marketdata.model.AnalyticModelFromCurvesAndVols;
import net.finmath.marketdata.model.curves.Curve;
import net.finmath.marketdata.model.volatilities.VolatilitySurface;
import net.finmath.singleswaprate.model.volatilities.VolatilityCube;

/**
 * Implementation of {@link net.finmath.singleswaprate.model.VolatilityCubeModel} based on {@link net.finmath.marketdata.model.AnalyticModelFromCurvesAndVols}.
 *
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public class AnalyticModelWithVolatilityCubes extends AnalyticModelFromCurvesAndVols implements VolatilityCubeModel, Cloneable {

	/**
	 *
	 */
	private static final long serialVersionUID = 5548526355763266102L;
	private final Map<String, VolatilityCube> volatilityCubeMap = new HashMap<>();

	/**
	 * Create an empty analytic model.
	 */
	public AnalyticModelWithVolatilityCubes() {
		super();
	}

	/**
	 * Create an empty analytic model for a specified date.
	 *
	 * @param referenceDate The reference date the curves of this model should match.
	 */
	public AnalyticModelWithVolatilityCubes(final LocalDate referenceDate) {
		super(referenceDate);
	}

	/**
	 * Create an analytic model for the specified reference date, together with curves as well as volatility surfaces and cubes, each with their specific name.
	 *
	 * @param referenceDate The reference date that should be used for all curves and surfaces of this model.
	 * @param curvesMap A map containing all curves, together with their names they should have in the model.
	 * @param volatilitySurfaceMap A map containing all volatility surfaces, together with their names they should have in the model.
	 * @param volatilityCubeMap A map containing all volatility cubes, together with their names they should have in the model.
	 */
	public AnalyticModelWithVolatilityCubes(final LocalDate referenceDate, final Map<String, Curve> curvesMap, final Map<String, VolatilitySurface> volatilitySurfaceMap,
			final Map<String, VolatilityCube> volatilityCubeMap) {
		super(referenceDate, curvesMap, volatilitySurfaceMap);
		this.volatilityCubeMap.putAll(volatilityCubeMap);
	}

	@Override
	public VolatilityCube getVolatilityCube(final String name){
		return volatilityCubeMap.get(name);
	}

	@Override
	public VolatilityCubeModel  addVolatilityCube(final VolatilityCube volatilityCube){

		final LocalDate cubeDate = volatilityCube.getReferenceDate();

		if(getReferenceDate() != null && cubeDate != null && ! getReferenceDate().equals(cubeDate)) {
			throw new IllegalArgumentException("Reference date of curve does not match reference date of model.");
		}

		final AnalyticModelWithVolatilityCubes newModel = clone();
		newModel.volatilityCubeMap.put(volatilityCube.getName(), volatilityCube);

		return newModel;
	}

	@Override
	public VolatilityCubeModel  addVolatilityCube(final String volatilityCubeName, final VolatilityCube volatilityCube){

		final LocalDate cubeDate = volatilityCube.getReferenceDate();

		if(getReferenceDate() != null && cubeDate != null && ! getReferenceDate().equals(cubeDate)) {
			throw new IllegalArgumentException("Reference date of curve does not match reference date of model.");
		}

		final AnalyticModelWithVolatilityCubes newModel = clone();
		newModel.volatilityCubeMap.put(volatilityCubeName, volatilityCube);

		return newModel;
	}

	@Override
	public AnalyticModelWithVolatilityCubes clone() {
		return new AnalyticModelWithVolatilityCubes(getReferenceDate(), getCurves(), getVolatilitySurfaces(), getVolatilityCubes());
	}

	@Override
	public String toString() {
		return "EnhancedAnalyticModel: curves=" + getCurves().keySet() + ", volatilitySurfaces=" + getVolatilitySurfaces().keySet() +
				", volatilityCubes=" + volatilityCubeMap.keySet();
	}

	@Override
	public Set<String> getVolatilityCubeNames() {
		return new HashSet<>(volatilityCubeMap.keySet());
	}

	@Override
	public Map<String, VolatilityCube> getVolatilityCubes() {
		return Collections.unmodifiableMap(volatilityCubeMap);
	}
}
