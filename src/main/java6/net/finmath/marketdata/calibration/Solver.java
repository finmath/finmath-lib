/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 26.11.2012
 */
package net.finmath.marketdata.calibration;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.marketdata.products.AnalyticProductInterface;
import net.finmath.optimizer.LevenbergMarquardt;
import net.finmath.optimizer.SolverException;

/**
 * Generates a calibrated model for a given set
 * of <code>calibrationProducts</code> with respect to given <code>Curve</code>s.
 * 
 * The model and the curve are assumed to be immutable, i.e., the solver
 * will return a calibrate clone, containing clones for every curve
 * which is part of the set of curves to be calibrated.
 * 
 * The calibration is performed as a multi-threaded global optimization.
 * I will greatly profit from a multi-core architecture.
 * 
 * @author Christian Fries
 */
public class Solver {

	private final AnalyticModelInterface			model;
	private final List<AnalyticProductInterface>	calibrationProducts;
	private final List<Double>						calibrationTargetValues;
	private final double							calibrationAccuracy;
	private final ParameterTransformation			parameterTransformation;

	private	final	double	evaluationTime;
	private final	int		maxIterations	= 1000;

	private 		int		iterations		= 0;
	private 		double	accuracy		= Double.POSITIVE_INFINITY;

	/**
	 * Generate a solver for the given parameter objects (independents) and
	 * objective functions (dependents).
	 * 
	 * @param model The model from which a calibrated clone should be created.
	 * @param calibrationProducts The objective functions.
	 * @param calibrationTargetValues Array of target values for the objective functions.
	 * @param parameterTransformation A parameter transformation, if any, otherwise null.
	 * @param evaluationTime Evaluation time applied to the calibration products.
	 * @param calibrationAccuracy The error tolerance of the solver.
	 */
	public Solver(AnalyticModelInterface model, Vector<AnalyticProductInterface> calibrationProducts, List<Double> calibrationTargetValues, ParameterTransformation parameterTransformation, double evaluationTime, double calibrationAccuracy) {
		super();
		this.model = model;
		this.calibrationProducts = calibrationProducts;
		this.calibrationTargetValues = calibrationTargetValues;
		this.parameterTransformation = parameterTransformation;
		this.evaluationTime = evaluationTime;
		this.calibrationAccuracy = calibrationAccuracy;
	}

	/**
	 * Generate a solver for the given parameter objects (independents) and
	 * objective functions (dependents).
	 * 
	 * @param model The model from which a calibrated clone should be created.
	 * @param calibrationProducts The objective functions.
	 * @param calibrationTargetValues Array of target values for the objective functions.
	 * @param evaluationTime Evaluation time applied to the calibration products.
	 * @param calibrationAccuracy The error tolerance of the solver.
	 */
	public Solver(AnalyticModelInterface model, Vector<AnalyticProductInterface> calibrationProducts, List<Double> calibrationTargetValues, double evaluationTime, double calibrationAccuracy) {
		this(model, calibrationProducts, calibrationTargetValues, null, evaluationTime, calibrationAccuracy);
	}

	/**
	 * Generate a solver for the given parameter objects (independents) and
	 * objective functions (dependents).
	 * 
	 * @param model The model from which a calibrated clone should be created.
	 * @param calibrationProducts The objective functions.
	 * @param evaluationTime Evaluation time applied to the calibration products.
	 * @param calibrationAccuracy The error tolerance of the solver.
	 */
	public Solver(AnalyticModelInterface model, Vector<AnalyticProductInterface> calibrationProducts, double evaluationTime, double calibrationAccuracy) {
		this(model, calibrationProducts, null, null, evaluationTime, calibrationAccuracy);
	}

	/**
	 * Generate a solver for the given parameter objects (independents) and
	 * objective functions (dependents).
	 * 
	 * @param model The model from which a calibrated clone should be created.
	 * @param calibrationProducts The objective functions.
	 */
	public Solver(AnalyticModelInterface model, Vector<AnalyticProductInterface> calibrationProducts) {
		this(model, calibrationProducts, 0.0, 0.0);
	}

	/**
	 * Find the model such that the equation
	 * <center>
	 * <code>
	 * objectiveFunctions.getValue(model) = 0
	 * </code>
	 * </center>
	 * holds.
	 * 
	 * @param objectsToCalibrate The set of parameterized objects to calibrate.
	 * @return A reference to a calibrated clone of the given model.
	 * @throws net.finmath.optimizer.SolverException Thrown if the underlying optimizer does not find a solution.
	 */
	public AnalyticModelInterface getCalibratedModel(Set<ParameterObjectInterface> objectsToCalibrate) throws SolverException {
		final ParameterAggregation<ParameterObjectInterface> parameterAggregate = new ParameterAggregation<ParameterObjectInterface>(objectsToCalibrate);

		// Set solver parameters
		double[] initialParameters	= parameterAggregate.getParameter();
		double[] zeros				= new double[calibrationProducts.size()];
		java.util.Arrays.fill(zeros, 0.0);

		int maxThreads		= Math.min(2 * Math.max(Runtime.getRuntime().availableProcessors(), 1), initialParameters.length);

		// Apply parameter transformation to solver parameter space
		if(parameterTransformation != null) initialParameters = parameterTransformation.getSolverParameter(initialParameters);

		LevenbergMarquardt optimizer = new LevenbergMarquardt(
				initialParameters,
				zeros, /* targetValues */
				maxIterations,
				maxThreads)
		{	
			@Override
			public void setValues(double[] parameters, double[] values) throws SolverException {
				try {
					if(parameterTransformation != null) parameters = parameterTransformation.getParameter(parameters);

					Map<ParameterObjectInterface, double[]> curvesParameterPairs = parameterAggregate.getObjectsToModifyForParameter(parameters);
					AnalyticModelInterface modelClone = model.getCloneForParameter(curvesParameterPairs);
					for(int i=0; i<calibrationProducts.size(); i++) {
						values[i] = calibrationProducts.get(i).getValue(evaluationTime, modelClone);
					}
					if(calibrationTargetValues != null) {
						for(int i=0; i<calibrationTargetValues.size(); i++) {
							values[i] -= calibrationTargetValues.get(i);
						}
					}
				} catch (CloneNotSupportedException e) {
					throw new SolverException(e);
				}
			}
		};
		optimizer.setErrorTolerance(calibrationAccuracy);
		optimizer.run();

		iterations = optimizer.getIterations();

		AnalyticModelInterface calibratedModel = null;
		try {
			double[] bestParameters = optimizer.getBestFitParameters();
			if(parameterTransformation != null) bestParameters = parameterTransformation.getParameter(bestParameters);

			Map<ParameterObjectInterface, double[]> curvesParameterPairs = parameterAggregate.getObjectsToModifyForParameter(bestParameters);
			calibratedModel = model.getCloneForParameter(curvesParameterPairs);
		} catch (CloneNotSupportedException e) {
			throw new SolverException(e);
		}

		accuracy = 0.0;
		for(int i=0; i<calibrationProducts.size(); i++) {
			double error = calibrationProducts.get(i).getValue(evaluationTime, calibratedModel);
			accuracy += error * error;
		}
		accuracy = Math.sqrt(accuracy);

		return calibratedModel;
	}

	/**
	 * Returns the number of iterations required in the last solver step.
	 * 
	 * @return The number of iterations required.
	 */
	public int getIterations() {
		return iterations;
	}

	public double getAccuracy() {
		return accuracy;
	}
}
