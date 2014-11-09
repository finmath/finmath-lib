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
		if(calibrationParameters == null) calibrationParameters = new HashMap<String,Object>();
		Integer maxIterationsParameter	= (Integer)calibrationParameters.get("maxIterations");
		Double	accuracyParameter		= (Double)calibrationParameters.get("accuracy");
		Double	evaluationTimeParameter		= (Double)calibrationParameters.get("evaluationTime");

		// @TODO: These constants should become parameters. The numberOfPaths and seed is only relevant if Monte-Carlo products are used for calibration.
		int maxIterations	= maxIterationsParameter != null ? maxIterationsParameter.intValue() : 400;
		double accuracy		= accuracyParameter != null ? accuracyParameter.doubleValue() : 1E-8;
		double evaluationTime		= evaluationTimeParameter != null ? evaluationTimeParameter.doubleValue() : 0.0;

		AnalyticModelInterface model = calibrationModel.addVolatilitySurfaces(this);
		
		
		// @TODO We constrain the parameters to positive values - this is experimental and will bekome a parameter
		ParameterTransformation parameterTransformation = new ParameterTransformation() {
			@Override
			public double[] getSolverParameter(final double[] parameter) {
				double[] newParameter = parameter.clone(); 
				for(int i=0; i<parameter.length; i++) newParameter[i] = Math.log(parameter[i]);
				return newParameter;
			}
			
			@Override
			public double[] getParameter(final double[] solverParameter) {
				double[] newSolverParameter = solverParameter.clone(); 
				for(int i=0; i<solverParameter.length; i++) newSolverParameter[i] = Math.exp(solverParameter[i]);
				return newSolverParameter;
			}
		};
		Solver solver = new Solver(model, calibrationProducts, calibrationTargetValues, parameterTransformation, evaluationTime, accuracy);

		Set<ParameterObjectInterface> objectsToCalibrate = new HashSet<ParameterObjectInterface>();
		objectsToCalibrate.add(this);
		AnalyticModelInterface modelCalibrated = solver.getCalibratedModel(objectsToCalibrate);

		double lastAccuracy		= solver.getAccuracy();
		double lastIterations	= solver.getIterations();

		return (AbstractVolatilitySurfaceParametric)modelCalibrated.getVolatilitySurface(this.getName());
	}
}
