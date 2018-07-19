package net.finmath.modelling.descriptor;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.finmath.marketdata.model.curves.CurveInterface;
import net.finmath.marketdata.model.volatilities.VolatilitySurfaceInterface;

/**
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public class AnalyticModelDescriptor implements InterestRateModelDescriptor {
	
	private final Map<String, CurveInterface>				curvesMap;
	private final Map<String, VolatilitySurfaceInterface>	volatilitySurfaceMap;
	
	
	/**
	 * Construct an AnalyticModelDescriptor mapping the collections of curves and volatility surfaces provided.
	 * 
	 * @param curves
	 * @param surfaces
	 */
	public AnalyticModelDescriptor(Collection<CurveInterface> curves, Collection<VolatilitySurfaceInterface> surfaces) {
		super();
		curvesMap = new HashMap<String, CurveInterface>();
		volatilitySurfaceMap = new HashMap<String, VolatilitySurfaceInterface>();
		
		if(curves != null) {
			for(CurveInterface curve : curves) {
				curvesMap.put(curve.getName(), curve);
			}
		}
		if (surfaces != null) {
			for (VolatilitySurfaceInterface surface : surfaces) {
				volatilitySurfaceMap.put(surface.getName(), surface);
			} 
		}
	}

	/**
	 * Construct an AnalyticModelDescriptor holding copies of the maps provided.
	 * 
	 * @param curvesMap
	 * @param volatilitySurfaceMap
	 */
	public AnalyticModelDescriptor(Map<String, CurveInterface> curvesMap, Map<String, VolatilitySurfaceInterface> volatilitySurfaceMap) {
		this.curvesMap = new HashMap<String, CurveInterface>();
		this.volatilitySurfaceMap = new HashMap<String, VolatilitySurfaceInterface>();
		
		this.curvesMap.putAll(curvesMap);
		this.volatilitySurfaceMap.putAll(volatilitySurfaceMap);
	}

	@Override
	public Integer version() {
		return 1;
	}

	@Override
	public String name() {
		return "Analytic model";
	}

	public Map<String, CurveInterface> getCurvesMap() {
		return Collections.unmodifiableMap(curvesMap);
	}

	public Map<String, VolatilitySurfaceInterface> getVolatilitySurfaceMap() {
		return Collections.unmodifiableMap(volatilitySurfaceMap);
	}


}
