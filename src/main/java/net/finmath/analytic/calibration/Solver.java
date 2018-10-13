/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 26.11.2012
 */
package net.finmath.analytic.calibration;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import net.finmath.analytic.model.AnalyticModelInterface;
import net.finmath.analytic.products.AnalyticProductInterface;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.optimizer.SolverException;
import net.finmath.optimizer.StochasticOptimizerFactoryInterface;
import net.finmath.optimizer.StochasticOptimizerInterface;
import net.finmath.optimizer.StochasticPathwiseOptimizerFactoryLevenbergMarquardt;
import net.finmath.stochastic.RandomVariableInterface;

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
 * @version 1.0
 */
public class Solver {

	private final AnalyticModelInterface			model;
	private final List<AnalyticProductInterface>	calibrationProducts;
	private final List<Double>						calibrationTargetValues;
	private final double							calibrationAccuracy;
	private final ParameterTransformation			parameterTransformation;

	private StochasticOptimizerFactoryInterface		optimizerFactory;

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
	 * @param optimizerFactory A factory providing the optimizer (for the given objective function)
	 */
	public Solver(AnalyticModelInterface model, Vector<AnalyticProductInterface> calibrationProducts, List<Double> calibrationTargetValues, ParameterTransformation parameterTransformation, double evaluationTime, StochasticOptimizerFactoryInterface optimizerFactory) {
		super();
		this.model = model;
		this.calibrationProducts = calibrationProducts;
		this.calibrationTargetValues = calibrationTargetValues;
		this.parameterTransformation = parameterTransformation;
		this.evaluationTime = evaluationTime;
		this.optimizerFactory = optimizerFactory;
		this.calibrationAccuracy = 0.0;
	}

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
		this.optimizerFactory = null;
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
		final ParameterAggregation<ParameterObjectInterface> parameterAggregate = new ParameterAggregation<>(objectsToCalibrate);

		// Set solver parameters
		final RandomVariableInterface[] initialParameters;

		// Apply parameter transformation to solver parameter space
		if(parameterTransformation != null) {
			initialParameters = parameterTransformation.getSolverParameter(parameterAggregate.getParameter());
		} else {
			initialParameters = parameterAggregate.getParameter();
		}

		final RandomVariableInterface[] zeros				= new RandomVariableInterface[calibrationProducts.size()];
		final RandomVariableInterface[] ones				= new RandomVariableInterface[calibrationProducts.size()];
		final RandomVariableInterface[] lowerBound			= new RandomVariableInterface[initialParameters.length];
		final RandomVariableInterface[] upperBound			= new RandomVariableInterface[initialParameters.length];
		java.util.Arrays.fill(zeros, new RandomVariable(0.0));
		java.util.Arrays.fill(ones, new RandomVariable(1.0));
		java.util.Arrays.fill(lowerBound, new RandomVariable(Double.NEGATIVE_INFINITY));
		java.util.Arrays.fill(upperBound, new RandomVariable(Double.POSITIVE_INFINITY));

		StochasticOptimizerInterface.ObjectiveFunction objectiveFunction = new StochasticOptimizerInterface.ObjectiveFunction() {
			@Override
			public void setValues(RandomVariableInterface[] parameters, RandomVariableInterface[] values) throws SolverException {
				RandomVariableInterface[] modelParameters = parameters;
				try {
					if(parameterTransformation != null) {
						modelParameters = parameterTransformation.getParameter(parameters);
						// Copy back the parameter constrain to inform the optimizer
						System.arraycopy(parameterTransformation.getSolverParameter(modelParameters), 0, parameters, 0, parameters.length);
					}

					Map<ParameterObjectInterface, RandomVariableInterface[]> curvesParameterPairs = parameterAggregate.getObjectsToModifyForParameter(modelParameters);
					AnalyticModelInterface modelClone = model.getCloneForParameter(curvesParameterPairs);
					for(int i=0; i<calibrationProducts.size(); i++) {
						values[i] = calibrationProducts.get(i).getValue(evaluationTime, modelClone);
					}
					if(calibrationTargetValues != null) {
						for(int i=0; i<calibrationTargetValues.size(); i++) {
							values[i].sub(calibrationTargetValues.get(i));
						}
					}
				} catch (CloneNotSupportedException e) {
					throw new SolverException(e);
				}
			}
		};

		if(optimizerFactory == null) {
			int maxThreads		= Math.min(2 * Math.max(Runtime.getRuntime().availableProcessors(), 1), initialParameters.length);
			optimizerFactory = new StochasticPathwiseOptimizerFactoryLevenbergMarquardt(maxIterations, calibrationAccuracy, maxThreads);
		}

		StochasticOptimizerInterface optimizer = optimizerFactory.getOptimizer(objectiveFunction, initialParameters, lowerBound, upperBound, zeros);
		optimizer.run();

		iterations = optimizer.getIterations();

		RandomVariableInterface[] bestParameters = optimizer.getBestFitParameters();
		if(parameterTransformation != null) {
			bestParameters = parameterTransformation.getParameter(bestParameters);
		}

		AnalyticModelInterface calibratedModel = null;
		try {

			Map<ParameterObjectInterface, RandomVariableInterface[]> curvesParameterPairs = parameterAggregate.getObjectsToModifyForParameter(bestParameters);
			calibratedModel = model.getCloneForParameter(curvesParameterPairs);
		} catch (CloneNotSupportedException e) {
			throw new SolverException(e);
		}

		accuracy = 0.0;
		for(int i=0; i<calibrationProducts.size(); i++) {
			double error = calibrationProducts.get(i).getValue(evaluationTime, calibratedModel).getStandardDeviation();
			if(calibrationTargetValues != null) {
				error -= calibrationTargetValues.get(i);
			}
			accuracy += error * error;
		}
		accuracy = Math.sqrt(accuracy/calibrationProducts.size());

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

	/**
	 * Returns the accuracy achieved in the last solver run.
	 *
	 * @return The accuracy achieved in the last solver run.
	 */
	public double getAccuracy() {
		return accuracy;
	}
}
