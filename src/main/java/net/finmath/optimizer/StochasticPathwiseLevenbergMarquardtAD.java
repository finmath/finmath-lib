/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 16.06.2006
 */
package net.finmath.optimizer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiableInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * This class implements a stochastic Levenberg Marquardt non-linear least-squares fit
 * algorithm.
 * <p>
 * The design avoids the need to define the objective function as a
 * separate class. The objective function is defined by overriding a class
 * method, see the sample code below.
 * </p>
 *
 * <p>
 * The Levenberg-Marquardt solver is implemented in using multi-threading.
 * The calculation of the derivatives (in case a specific implementation of
 * {@code setDerivatives(RandomVariableInterface[] parameters, RandomVariableInterface[][] derivatives)} is not
 * provided) may be performed in parallel by setting the parameter <code>numberOfThreads</code>.
 * </p>
 *
 * <p>
 * To use the solver inherit from it and implement the objective function as
 * {@code setValues(RandomVariableInterface[] parameters, RandomVariableInterface[] values)} where values has
 * to be set to the value of the objective functions for the given parameters.
 * <br>
 * You may also provide an a derivative for your objective function by
 * additionally overriding the function {@code setDerivatives(RandomVariableInterface[] parameters, RandomVariableInterface[][] derivatives)},
 * otherwise the solver will calculate the derivative via finite differences.
 * </p>
 * <p>
 * To reject a point, it is allowed to set an element of <code>values</code> to {@link java.lang.Double#NaN}
 * in the implementation of {@code setValues(RandomVariableInterface[] parameters, RandomVariableInterface[] values)}.
 * Put differently: The solver handles NaN values in <code>values</code> as an error larger than
 * the current one (regardless of the current error) and rejects the point.
 * <br>
 * Note, however, that is is an error if the initial parameter guess results in an NaN value.
 * That is, the solver should be initialized with an initial parameter in an admissible region.
 * </p>
 *
 * The following simple example finds a solution for the equation <br>
 * <center>
 * <table>
 * <caption>Sample linear system of equations.</caption>
 * <tr><td>
 * 0.0 * x<sub>1</sub> + 1.0 * x<sub>2</sub> = 5.0
 * </td></tr>
 * <tr><td>
 * 2.0 * x<sub>1</sub> + 1.0 * x<sub>2</sub> = 10.0
 * </td></tr>
 * </table>
 * </center>
 *
 * <pre>
 * <code>
 * 	LevenbergMarquardt optimizer = new LevenbergMarquardt() {
 * 		// Override your objective function here
 * 		public void setValues(RandomVariableInterface[] parameters, RandomVariableInterface[] values) {
 * 			values[0] = parameters[0] * 0.0 + parameters[1];
 * 			values[1] = parameters[0] * 2.0 + parameters[1];
 * 		}
 * 	};
 *
 * 	// Set solver parameters
 * 	optimizer.setInitialParameters(new RandomVariableInterface[] { 0, 0 });
 * 	optimizer.setWeights(new RandomVariableInterface[] { 1, 1 });
 * 	optimizer.setMaxIteration(100);
 * 	optimizer.setTargetValues(new RandomVariableInterface[] { 5, 10 });
 *
 * 	optimizer.run();
 *
 * 	RandomVariableInterface[] bestParameters = optimizer.getBestFitParameters();
 * </code>
 * </pre>
 *
 * See the example in the main method below.
 *
 * <p>
 * The class can be initialized to use a multi-threaded valuation. If initialized
 * this way the implementation of <code>setValues</code> must be thread-safe.
 * The solver will evaluate the gradient of the value vector in parallel, i.e.,
 * use as many threads as the number of parameters.
 * </p>
 *
 * Note: Iteration steps will be logged (java.util.logging) with LogLevel.FINE
 *
 * @author Christian Fries
 * @version 1.6
 */
public abstract class StochasticPathwiseLevenbergMarquardtAD extends net.finmath.optimizer.StochasticPathwiseLevenbergMarquardt {

	/**
	 *
	 */
	private static final long serialVersionUID = -8852002990042152135L;

	public StochasticPathwiseLevenbergMarquardtAD(List<RandomVariableInterface> initialParameters, List<RandomVariableInterface> targetValues, int maxIteration, ExecutorService executorService) {
		super(initialParameters, targetValues, maxIteration, executorService);
	}

	public StochasticPathwiseLevenbergMarquardtAD(List<RandomVariableInterface> initialParameters, List<RandomVariableInterface> targetValues, int maxIteration, int numberOfThreads) {
		super(initialParameters, targetValues, maxIteration, numberOfThreads);
	}


	public StochasticPathwiseLevenbergMarquardtAD(RandomVariableInterface[] initialParameters, RandomVariableInterface[] targetValues, int maxIteration, int numberOfThreads) {
		super(initialParameters, targetValues, maxIteration, numberOfThreads);
	}


	public StochasticPathwiseLevenbergMarquardtAD(RandomVariableInterface[] initialParameters,
			RandomVariableInterface[] targetValues, RandomVariableInterface[] weights,
			RandomVariableInterface[] parameterSteps, int maxIteration, RandomVariableInterface errorTolerance,
			ExecutorService executorService) {
		super(initialParameters, targetValues, weights, parameterSteps, maxIteration, errorTolerance, executorService);
	}


	@Override
	protected void prepareAndSetValues(RandomVariableInterface[] parameters, RandomVariableInterface[] values) throws SolverException {
		/**
		 * Small modification to avoid growing operator trees.
		 */
		for(int i=0; i<parameters.length; i++) {
			if(parameters[i] instanceof RandomVariableDifferentiableInterface) {
				parameters[i] = ((RandomVariableDifferentiableInterface) parameters[i]).getCloneIndependent();
			}
		}

		setValues(parameters, values);
	}

	@Override
	protected void prepareAndSetDerivatives(RandomVariableInterface[] parameters, RandomVariableInterface[] values, RandomVariableInterface[][] derivatives) throws SolverException {
		/*
		 * Check if random variable is differentiable
		 */
		boolean isRandomVariableDifferentiable = true;
		for(int parameterIndex=0; parameterIndex<parameters.length && isRandomVariableDifferentiable; parameterIndex++) {
			isRandomVariableDifferentiable = (parameters[parameterIndex] instanceof RandomVariableDifferentiableInterface) && isRandomVariableDifferentiable;
		}
		for(int valueIndex=0; valueIndex<values.length && isRandomVariableDifferentiable; valueIndex++) {
			isRandomVariableDifferentiable = (values[valueIndex] instanceof RandomVariableDifferentiableInterface) && isRandomVariableDifferentiable;
		}


		if(isRandomVariableDifferentiable) {
			for (int valueIndex = 0; valueIndex < values.length; valueIndex++) {
				Map<Long, RandomVariableInterface> gradient = ((RandomVariableDifferentiableInterface)values[valueIndex]).getGradient();
				for (int parameterIndex = 0; parameterIndex < parameters.length; parameterIndex++) {
					derivatives[parameterIndex][valueIndex] = gradient.get(((RandomVariableDifferentiableInterface)parameters[parameterIndex]).getID());
				}
			}
		}
		else {
			setDerivatives(parameters, derivatives);
		}
	}
}
