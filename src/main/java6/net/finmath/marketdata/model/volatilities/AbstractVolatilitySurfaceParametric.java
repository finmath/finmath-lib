/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 07.09.2014
 */

package net.finmath.marketdata.model.volatilities;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.threeten.bp.LocalDate;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.calibration.ParameterObjectInterface;
import net.finmath.marketdata.calibration.ParameterTransformation;
import net.finmath.marketdata.calibration.Solver;
import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.marketdata.products.AnalyticProductInterface;
import net.finmath.optimizer.OptimizerFactoryInterface;
import net.finmath.optimizer.SolverException;

/**
 * Base class for parametric volatility surfaces, implementing a generic calibration algorithm.
 *
 * @author Christian Fries
 */
public abstract class AbstractVolatilitySurfaceParametric extends AbstractVolatilitySurface implements ParameterObjectInterface {

	private static final Logger logger = Logger.getLogger("net.finmath");

	public AbstractVolatilitySurfaceParametric(String name, LocalDate referenceDate) {
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
		return getCloneCalibrated(calibrationModel, calibrationProducts, calibrationTargetValues, calibrationParameters, parameterTransformation, null);
	}

	/**
	 * Create a clone of this volatility surface using a generic calibration
	 * of its parameters to given market data.
	 *
	 * @param calibrationModel The model used during calibration (contains additional objects required during valuation, e.g. curves).
	 * @param calibrationProducts The calibration products.
	 * @param calibrationTargetValues The target values of the calibration products.
	 * @param calibrationParameters A map containing additional settings like "evaluationTime" (Double).
	 * @param parameterTransformation An optional parameter transformation.
	 * @param optimizerFactory The factory providing the optimizer to be used during calibration.
	 * @return An object having the same type as this one, using (hopefully) calibrated parameters.
	 * @throws SolverException Exception thrown when solver fails.
	 */
	public AbstractVolatilitySurfaceParametric getCloneCalibrated(final AnalyticModelInterface calibrationModel, final Vector<AnalyticProductInterface> calibrationProducts, final List<Double> calibrationTargetValues, Map<String,Object> calibrationParameters, final ParameterTransformation parameterTransformation, OptimizerFactoryInterface optimizerFactory) throws SolverException {
		if(calibrationParameters == null) {
			calibrationParameters = new HashMap<String,Object>();
		}
		Integer maxIterationsParameter	= (Integer)calibrationParameters.get("maxIterations");
		Double	accuracyParameter		= (Double)calibrationParameters.get("accuracy");
		Double	evaluationTimeParameter		= (Double)calibrationParameters.get("evaluationTime");

		// @TODO currently ignored, we use the setting form the OptimizerFactoryInterface
		int maxIterations		= maxIterationsParameter != null ? maxIterationsParameter.intValue() : 600;
		double accuracy			= accuracyParameter != null ? accuracyParameter.doubleValue() : 1E-8;
		double evaluationTime	= evaluationTimeParameter != null ? evaluationTimeParameter.doubleValue() : 0.0;

		AnalyticModelInterface model = calibrationModel.addVolatilitySurfaces(this);
		Solver solver = new Solver(model, calibrationProducts, calibrationTargetValues, parameterTransformation, evaluationTime, optimizerFactory);

		Set<ParameterObjectInterface> objectsToCalibrate = new HashSet<ParameterObjectInterface>();
		objectsToCalibrate.add(this);
		AnalyticModelInterface modelCalibrated = solver.getCalibratedModel(objectsToCalibrate);

		// Diagnostic output
		if (logger.isLoggable(Level.FINE)) {
			double lastAccuracy		= solver.getAccuracy();
			int 	lastIterations	= solver.getIterations();

			logger.fine("The solver achived an accuracy of " + lastAccuracy + " in " + lastIterations + ".");
		}

		return (AbstractVolatilitySurfaceParametric)modelCalibrated.getVolatilitySurface(this.getName());
	}
}

