/**
<p>
	This package provides classes with numerical algorithm for optimization of
	an objective function and a factory to easy construction of the optimizers.
</p>

<b>Why a package for optimization algorithms?</b>
<p>
	Given that there are a variety of numerical libraries featuring optimization algorithms
	(e.g., Apache Commons Math), why do we provide a package inside finmath lib?
	This packages provides a unified interface for passing optimizers to other classes
	via an <code>OptimizationFactoryInterface</code> and an <code>Optimizer</code>
	and an <code>Optimizer.ObjectiveFunction</code>.
	This allows use of different optimization frameworks without bothering with the
	framework specific constructors and framework specific definitions of objective functions.
</p>

<p>
	A class implementing the <code>OptimizationFactoryInterface</code> allows the
	specification of parameters specific to the optimizer, but leave the specification
	of the initial values and the objective function still open. It provides a factory
	method which takes the objective function and initial values as parameters and
	constructs the specific optimizer by returning an object implementing
	<code>Optimizer</code>.
</p>

<b>Example</b>

<p>

The following code is an example of an optimization problem using an <code>OptimizerFactory</code>
as argument.
<pre><code>
	public void testOptimizerWithRosenbrockFunction(OptimizerFactory optimizerFactory) throws SolverException {
		Optimizer.ObjectiveFunction objectiveFunction = new Optimizer.ObjectiveFunction() {
				public void setValues(double[] parameters, double[] values) {
					values[0] = 10.0 * (parameters[1] - parameters[0]*parameters[0]);
					values[1] = 1.0 - parameters[0];
				}
		};

		Optimizer optimizer = optimizerFactory.getOptimizer(
				objectiveFunction,
				new double[] { 0.5, 0.5 }, // initialParameters
				new double[] { Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY }, // lowerBound
				new double[] { Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY }, // upperBound
				new double[] { 0.5, 0.5 }, // parameterStep
				new double[] { 0.0, 0.0 }); // targetValues

		optimizer.run();

		double[] bestParameters = optimizer.getBestFitParameters();
		System.out.println("The solver " + optimizer.getClass() + " for problem 'Rosebrock' required " + optimizer.getIterations() + " iterations. Accuracy is " + optimizer.getRootMeanSquaredError() + ". The best fit parameters are:");
		for (int i = 0; i &lt; bestParameters.length; i++) System.out.println("\tparameter[" + i + "]: " + bestParameters[i]);

		System.out.println();

		Assert.assertTrue(Math.abs(bestParameters[0] - 1.0) &lt; 1E-10);
		Assert.assertTrue(Math.abs(bestParameters[1] - 1.0) &lt; 1E-10);
	}
</code></pre>

Now, we may pass different optimizers to the optimization problem. For example the CMA-ES solver from commons math:
<pre>
	public void testRosenbrockFunctionWithCMAES() throws SolverException {

		OptimizerFactory optimizerFactory = new OptimizerFactoryCMAES(0.0, 200);
		this.testOptimizerWithRosenbrockFunction(optimizerFactory);
	}
</pre>

Or the multi-threadded Levenberg Marquardt solver (using two threads) from finmath-lib:
<pre>
	public void testRosenbrockFunctionWithLevenbergMarquard() throws SolverException {

		OptimizerFactory optimizerFactory = new OptimizerFactoryLevenbergMarquardt(200, 2);
		this.testOptimizerWithRosenbrockFunction(optimizerFactory);
	}
</pre>

<b>Optimization algorithms</b>
<p>
	The package also contains an implementation of the Levenberg Marquardt optimizer,
	a multi-dimensional non-linear least-square.
	In addition we provide wrappers (via specific <code>OptimizationFactoryInterface</code>
	implementations) to some optimizers from Apache commons-math.
</p>
 */
package net.finmath.optimizer;
