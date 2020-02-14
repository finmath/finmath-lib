/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 26.11.2012
 */
package net.finmath.marketdata2.calibration;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import net.finmath.marketdata2.model.AnalyticModel;
import net.finmath.marketdata2.products.AnalyticProduct;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.optimizer.SolverException;
import net.finmath.optimizer.StochasticOptimizer;
import net.finmath.optimizer.StochasticOptimizerFactory;
import net.finmath.optimizer.StochasticPathwiseOptimizerFactoryLevenbergMarquardt;
import net.finmath.stochastic.RandomVariable;

/**
 * Generates a calibrated model for a given set
 * of <code>calibrationProducts</code> with respect to given <code>CurveFromInterpolationPoints</code>s.
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

	private final AnalyticModel			model;
	private final List<AnalyticProduct>	calibrationProducts;
	private final List<Double>						calibrationTargetValues;
	private final double							calibrationAccuracy;
	private final ParameterTransformation			parameterTransformation;

	private StochasticOptimizerFactory		optimizerFactory;

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
	public Solver(final AnalyticModel model, final Vector<AnalyticProduct> calibrationProducts, final List<Double> calibrationTargetValues, final ParameterTransformation parameterTransformation, final double evaluationTime, final StochasticOptimizerFactory optimizerFactory) {
		super();
		this.model = model;
		this.calibrationProducts = calibrationProducts;
		this.calibrationTargetValues = calibrationTargetValues;
		this.parameterTransformation = parameterTransformation;
		this.evaluationTime = evaluationTime;
		this.optimizerFactory = optimizerFactory;
		calibrationAccuracy = 0.0;
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
	public Solver(final AnalyticModel model, final Vector<AnalyticProduct> calibrationProducts, final List<Double> calibrationTargetValues, final ParameterTransformation parameterTransformation, final double evaluationTime, final double calibrationAccuracy) {
		super();
		this.model = model;
		this.calibrationProducts = calibrationProducts;
		this.calibrationTargetValues = calibrationTargetValues;
		this.parameterTransformation = parameterTransformation;
		this.evaluationTime = evaluationTime;
		this.calibrationAccuracy = calibrationAccuracy;
		optimizerFactory = null;
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
	public Solver(final AnalyticModel model, final Vector<AnalyticProduct> calibrationProducts, final List<Double> calibrationTargetValues, final double evaluationTime, final double calibrationAccuracy) {
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
	public Solver(final AnalyticModel model, final Vector<AnalyticProduct> calibrationProducts, final double evaluationTime, final double calibrationAccuracy) {
		this(model, calibrationProducts, null, null, evaluationTime, calibrationAccuracy);
	}

	/**
	 * Generate a solver for the given parameter objects (independents) and
	 * objective functions (dependents).
	 *
	 * @param model The model from which a calibrated clone should be created.
	 * @param calibrationProducts The objective functions.
	 */
	public Solver(final AnalyticModel model, final Vector<AnalyticProduct> calibrationProducts) {
		this(model, calibrationProducts, 0.0, 0.0);
	}

	/**
	 * Find the model such that the equation
	 * <code>
	 * objectiveFunctions.getValue(model) = 0
	 * </code>
	 * holds.
	 *
	 * @param objectsToCalibrate The set of parameterized objects to calibrate.
	 * @return A reference to a calibrated clone of the given model.
	 * @throws net.finmath.optimizer.SolverException Thrown if the underlying optimizer does not find a solution.
	 */
	public AnalyticModel getCalibratedModel(final Set<ParameterObject> objectsToCalibrate) throws SolverException {
		final ParameterAggregation<ParameterObject> parameterAggregate = new ParameterAggregation<>(objectsToCalibrate);

		// Set solver parameters
		final RandomVariable[] initialParameters;

		// Apply parameter transformation to solver parameter space
		if(parameterTransformation != null) {
			initialParameters = parameterTransformation.getSolverParameter(parameterAggregate.getParameter());
		} else {
			initialParameters = parameterAggregate.getParameter();
		}

		final RandomVariable[] zeros				= new RandomVariable[calibrationProducts.size()];
		final RandomVariable[] ones				= new RandomVariable[calibrationProducts.size()];
		final RandomVariable[] lowerBound			= new RandomVariable[initialParameters.length];
		final RandomVariable[] upperBound			= new RandomVariable[initialParameters.length];
		java.util.Arrays.fill(zeros, new RandomVariableFromDoubleArray(0.0));
		java.util.Arrays.fill(ones, new RandomVariableFromDoubleArray(1.0));
		java.util.Arrays.fill(lowerBound, new RandomVariableFromDoubleArray(Double.NEGATIVE_INFINITY));
		java.util.Arrays.fill(upperBound, new RandomVariableFromDoubleArray(Double.POSITIVE_INFINITY));

		final StochasticOptimizer.ObjectiveFunction objectiveFunction = new StochasticOptimizer.ObjectiveFunction() {
			@Override
			public void setValues(final RandomVariable[] parameters, final RandomVariable[] values) throws SolverException {
				RandomVariable[] modelParameters = parameters;
				try {
					if(parameterTransformation != null) {
						modelParameters = parameterTransformation.getParameter(parameters);
						// Copy back the parameter constrain to inform the optimizer
						System.arraycopy(parameterTransformation.getSolverParameter(modelParameters), 0, parameters, 0, parameters.length);
					}

					final Map<ParameterObject, RandomVariable[]> curvesParameterPairs = parameterAggregate.getObjectsToModifyForParameter(modelParameters);
					final AnalyticModel modelClone = model.getCloneForParameter(curvesParameterPairs);
					for(int i=0; i<calibrationProducts.size(); i++) {
						values[i] = calibrationProducts.get(i).getValue(evaluationTime, modelClone);
					}
					if(calibrationTargetValues != null) {
						for(int i=0; i<calibrationTargetValues.size(); i++) {
							values[i].sub(calibrationTargetValues.get(i));
						}
					}
				} catch (final CloneNotSupportedException e) {
					throw new SolverException(e);
				}
			}
		};

		if(optimizerFactory == null) {
			final int maxThreads		= Math.min(2 * Math.max(Runtime.getRuntime().availableProcessors(), 1), initialParameters.length);
			optimizerFactory = new StochasticPathwiseOptimizerFactoryLevenbergMarquardt(maxIterations, calibrationAccuracy, maxThreads);
		}

		final StochasticOptimizer optimizer = optimizerFactory.getOptimizer(objectiveFunction, initialParameters, lowerBound, upperBound, zeros);
		optimizer.run();

		iterations = optimizer.getIterations();

		RandomVariable[] bestParameters = optimizer.getBestFitParameters();
		if(parameterTransformation != null) {
			bestParameters = parameterTransformation.getParameter(bestParameters);
		}

		AnalyticModel calibratedModel = null;
		try {

			final Map<ParameterObject, RandomVariable[]> curvesParameterPairs = parameterAggregate.getObjectsToModifyForParameter(bestParameters);
			calibratedModel = model.getCloneForParameter(curvesParameterPairs);
		} catch (final CloneNotSupportedException e) {
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
