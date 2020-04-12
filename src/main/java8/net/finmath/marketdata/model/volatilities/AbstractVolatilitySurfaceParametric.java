/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 07.09.2014
 */

package net.finmath.marketdata.model.volatilities;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.calibration.ParameterObject;
import net.finmath.marketdata.calibration.ParameterTransformation;
import net.finmath.marketdata.calibration.Solver;
import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.products.AnalyticProduct;
import net.finmath.optimizer.OptimizerFactory;
import net.finmath.optimizer.SolverException;
import net.finmath.time.daycount.DayCountConvention;

/**
 * Base class for parametric volatility surfaces, implementing a generic calibration algorithm.
 *
 * @author Christian Fries
 * @version 1.0
 */
public abstract class AbstractVolatilitySurfaceParametric extends AbstractVolatilitySurface implements ParameterObject {

	private static final Logger logger = Logger.getLogger("net.finmath");


	public AbstractVolatilitySurfaceParametric(final String name, final LocalDate referenceDate, final ForwardCurve forwardCurve, final DiscountCurve discountCurve, final QuotingConvention quotingConvention, final DayCountConvention daycountConvention) {
		super(name, referenceDate, forwardCurve, discountCurve, quotingConvention, daycountConvention);
	}

	public AbstractVolatilitySurfaceParametric(final String name, final LocalDate referenceDate) {
		super(name, referenceDate);
	}

	/**
	 * Returns a clone of this volatility surface with modified parameters.
	 *
	 * @param value Parameter array.
	 * @return Clone with new parameters.
	 * @throws CloneNotSupportedException Thrown if this object cannot be cloned.
	 */
	@Override
	public abstract AbstractVolatilitySurfaceParametric getCloneForParameter(double[] value) throws CloneNotSupportedException;

	public AbstractVolatilitySurfaceParametric getCloneCalibrated(final AnalyticModel calibrationModel, final Vector<AnalyticProduct> calibrationProducts, final List<Double> calibrationTargetValues, final Map<String,Object> calibrationParameters) throws CalculationException, SolverException {
		return getCloneCalibrated(calibrationModel, calibrationProducts, calibrationTargetValues, calibrationParameters, null);
	}

	public AbstractVolatilitySurfaceParametric getCloneCalibrated(final AnalyticModel calibrationModel, final Vector<AnalyticProduct> calibrationProducts, final List<Double> calibrationTargetValues, final Map<String,Object> calibrationParameters, final ParameterTransformation parameterTransformation) throws CalculationException, SolverException {
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
	public AbstractVolatilitySurfaceParametric getCloneCalibrated(final AnalyticModel calibrationModel, final Vector<AnalyticProduct> calibrationProducts, final List<Double> calibrationTargetValues, Map<String,Object> calibrationParameters, final ParameterTransformation parameterTransformation, final OptimizerFactory optimizerFactory) throws SolverException {
		if(calibrationParameters == null) {
			calibrationParameters = new HashMap<>();
		}
		final Integer maxIterationsParameter	= (Integer)calibrationParameters.get("maxIterations");
		final Double	accuracyParameter		= (Double)calibrationParameters.get("accuracy");
		final Double	evaluationTimeParameter		= (Double)calibrationParameters.get("evaluationTime");

		// @TODO currently ignored, we use the setting form the OptimizerFactory
		final int maxIterations		= maxIterationsParameter != null ? maxIterationsParameter.intValue() : 600;
		final double accuracy			= accuracyParameter != null ? accuracyParameter.doubleValue() : 1E-8;
		final double evaluationTime	= evaluationTimeParameter != null ? evaluationTimeParameter.doubleValue() : 0.0;

		final AnalyticModel model = calibrationModel.addVolatilitySurfaces(this);
		final Solver solver = new Solver(model, calibrationProducts, calibrationTargetValues, parameterTransformation, evaluationTime, optimizerFactory);

		final Set<ParameterObject> objectsToCalibrate = new HashSet<>();
		objectsToCalibrate.add(this);
		final AnalyticModel modelCalibrated = solver.getCalibratedModel(objectsToCalibrate);

		// Diagnostic output
		if (logger.isLoggable(Level.FINE)) {
			final double lastAccuracy		= solver.getAccuracy();
			final int 	lastIterations	= solver.getIterations();

			logger.fine("The solver achieved an accuracy of " + lastAccuracy + " in " + lastIterations + ".");
		}

		return (AbstractVolatilitySurfaceParametric)modelCalibrated.getVolatilitySurface(this.getName());
	}
}
