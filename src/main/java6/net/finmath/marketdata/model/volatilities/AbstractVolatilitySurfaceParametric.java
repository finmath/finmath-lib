/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 07.09.2014
 */

package net.finmath.marketdata.model.volatilities;

import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.calibration.ParameterObjectInterface;
import net.finmath.marketdata.calibration.ParameterTransformation;
import net.finmath.marketdata.calibration.Solver;
import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.marketdata.products.AnalyticProductInterface;
import net.finmath.optimizer.SolverException;

/**
 * @author Christian Fries
 */
public abstract class AbstractVolatilitySurfaceParametric extends AbstractVolatilitySurface implements ParameterObjectInterface {

	public AbstractVolatilitySurfaceParametric(String name, Calendar referenceDate) {
		super(name, referenceDate);
	}

	/**
	 * Returns a clone of this volatility surface with modified parameters.
	 * 
	 * @param value Parameter array.
	 * @return Clone with new parameters.
	 * @throws CloneNotSupportedException Thrown if this object cannot be cloned.
	 */
	public abstract AbstractVolatilitySurfaceParametric getCloneForParameter(double[] value) throws CloneNotSupportedException;

	public AbstractVolatilitySurfaceParametric getCloneCalibrated(final AnalyticModelInterface calibrationModel, final Vector<AnalyticProductInterface> calibrationProducts, final List<Double> calibrationTargetValues, Map<String,Object> calibrationParameters) throws CalculationException, SolverException {
		return getCloneCalibrated(calibrationModel, calibrationProducts, calibrationTargetValues, calibrationParameters, null);
	}

	public AbstractVolatilitySurfaceParametric getCloneCalibrated(final AnalyticModelInterface calibrationModel, final Vector<AnalyticProductInterface> calibrationProducts, final List<Double> calibrationTargetValues, Map<String,Object> calibrationParameters, final ParameterTransformation parameterTransformation) throws CalculationException, SolverException {
		if(calibrationParameters == null) calibrationParameters = new HashMap<String,Object>();
		Integer maxIterationsParameter	= (Integer)calibrationParameters.get("maxIterations");
		Double	accuracyParameter		= (Double)calibrationParameters.get("accuracy");
		Double	evaluationTimeParameter		= (Double)calibrationParameters.get("evaluationTime");

		// @TODO: These constants should become parameters. The numberOfPaths and seed is only relevant if Monte-Carlo products are used for calibration.
		int maxIterations	= maxIterationsParameter != null ? maxIterationsParameter.intValue() : 400;
		double accuracy		= accuracyParameter != null ? accuracyParameter.doubleValue() : 1E-8;
		double evaluationTime		= evaluationTimeParameter != null ? evaluationTimeParameter.doubleValue() : 0.0;

		AnalyticModelInterface model = calibrationModel.addVolatilitySurfaces(this);
		Solver solver = new Solver(model, calibrationProducts, calibrationTargetValues, parameterTransformation, evaluationTime, accuracy);

		Set<ParameterObjectInterface> objectsToCalibrate = new HashSet<ParameterObjectInterface>();
		objectsToCalibrate.add(this);
		AnalyticModelInterface modelCalibrated = solver.getCalibratedModel(objectsToCalibrate);

		double lastAccuracy		= solver.getAccuracy();
		double lastIterations	= solver.getIterations();

		return (AbstractVolatilitySurfaceParametric)modelCalibrated.getVolatilitySurface(this.getName());
	}
}
