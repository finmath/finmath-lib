/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2006
 */
package net.finmath.montecarlo.interestrate.models.covariance;

import java.util.Map;

import net.finmath.functions.LinearAlgebra;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
import net.finmath.time.TimeDiscretization;


/**
 * Simple correlation model given by R, where R is a factor reduced matrix
 * (see {@link net.finmath.functions.LinearAlgebra#factorReduction(double[][], int)}) created from the
 * \( n \) Eigenvectors of \( \tilde{R} \) belonging to the \( n \) largest non-negative Eigenvalues,
 * where \( \tilde{R} = \tilde{\rho}_{i,j} \) and \[ \tilde{\rho}_{i,j} = \exp( -\max(a,0) | T_{i}-T_{j} | ) \]
 *
 * For a more general model featuring three parameters see {@link LIBORCorrelationModelThreeParameterExponentialDecay}.
 *
 * @see net.finmath.functions.LinearAlgebra#factorReduction(double[][], int)
 * @see LIBORCorrelationModelThreeParameterExponentialDecay
 *
 * @author Christian Fries
 * @version 1.0
 */
public class LIBORCorrelationModelExponentialDecay extends LIBORCorrelationModel {

	private static final long serialVersionUID = -8218022418731667531L;

	private final	int			numberOfFactors;
	private 		double		a;
	private final	boolean		isCalibrateable;

	private double[][]	correlationMatrix;
	private double[][]	factorMatrix;

	/**
	 * Create a correlation model with an exponentially decaying correlation structure and the given number of factors.
	 *
	 * @param timeDiscretization Simulation time dicretization. Not used.
	 * @param liborPeriodDiscretization TenorFromArray time discretization, i.e., the \( T_{i} \)'s.
	 * @param numberOfFactors Number \( n \) of factors to be used.
	 * @param a Decay parameter. Should be positive. Negative values will be floored to 0.
	 * @param isCalibrateable If true, the parameter will become a free parameter in a calibration.
	 */
	public LIBORCorrelationModelExponentialDecay(TimeDiscretization timeDiscretization, TimeDiscretization liborPeriodDiscretization, int numberOfFactors, double a, boolean isCalibrateable) {
		super(timeDiscretization, liborPeriodDiscretization);

		this.numberOfFactors	= numberOfFactors;
		this.a					= a;
		this.isCalibrateable	= isCalibrateable;

		initialize(numberOfFactors, a);
	}

	public LIBORCorrelationModelExponentialDecay(TimeDiscretization timeDiscretization, TimeDiscretization liborPeriodDiscretization, int numberOfFactors, double a) {
		super(timeDiscretization, liborPeriodDiscretization);

		this.numberOfFactors	= numberOfFactors;
		this.a					= a;
		isCalibrateable	= false;

		initialize(numberOfFactors, a);
	}

	@Override
	public LIBORCorrelationModelExponentialDecay getCloneWithModifiedParameter(RandomVariable[] parameter) {
		if(!isCalibrateable) {
			return this;
		}

		return new LIBORCorrelationModelExponentialDecay(timeDiscretization, liborPeriodDiscretization, numberOfFactors, parameter[0].doubleValue());
	}

	@Override
	public Object clone() {
		return new LIBORCorrelationModelExponentialDecay(timeDiscretization, liborPeriodDiscretization, numberOfFactors, a, isCalibrateable);
	}

	@Override
	public double	getFactorLoading(int timeIndex, int factor, int component) {
		return factorMatrix[component][factor];
	}

	@Override
	public double	getCorrelation(int timeIndex, int component1, int component2) {
		return correlationMatrix[component1][component2];
	}

	@Override
	public int		getNumberOfFactors() {
		return factorMatrix[0].length;
	}

	private void initialize(int numberOfFactors, double a) {
		/*
		 * Create instantaneous correlation matrix
		 */

		// Negative values of a do not make sense.
		a = Math.max(a, 0);

		correlationMatrix = new double[liborPeriodDiscretization.getNumberOfTimeSteps()][liborPeriodDiscretization.getNumberOfTimeSteps()];
		for(int row=0; row<correlationMatrix.length; row++) {
			for(int col=0; col<correlationMatrix[row].length; col++) {
				// Exponentially decreasing instantaneous correlation
				correlationMatrix[row][col] = Math.exp(-a * Math.abs(liborPeriodDiscretization.getTime(row)-liborPeriodDiscretization.getTime(col)));
			}
		}

		/*
		 * Perform a factor decomposition (and reduction if numberOfFactors < correlationMatrix.columns())
		 */
		factorMatrix = LinearAlgebra.factorReduction(correlationMatrix, numberOfFactors);

		for(int component1=0; component1<factorMatrix.length; component1++) {
			for(int component2=0; component2<component1; component2++) {
				double correlation = 0.0;
				for(int factor=0; factor<factorMatrix[component1].length; factor++) {
					correlation += factorMatrix[component1][factor] * factorMatrix[component2][factor];
				}
				correlationMatrix[component1][component2] = correlation;
				correlationMatrix[component2][component1] = correlation;
			}
			correlationMatrix[component1][component1] = 1.0;
		}
	}

	@Override
	public RandomVariable[] getParameter() {
		if(!isCalibrateable) {
			return null;
		}

		RandomVariable[] parameter = new RandomVariable[1];

		parameter[0] = new Scalar(a);

		return parameter;
	}

	@Override
	public LIBORCorrelationModel getCloneWithModifiedData(Map<String, Object> dataModified) {
		TimeDiscretization timeDiscretization = this.getTimeDiscretization();
		TimeDiscretization liborPeriodDiscretization = this.getLiborPeriodDiscretization();
		int numberOfFactors = this.getNumberOfFactors();
		double a = this.a;
		boolean isCalibrateable = this.isCalibrateable;

		if(dataModified != null) {
			timeDiscretization = (TimeDiscretization)dataModified.getOrDefault("timeDiscretization", timeDiscretization);
			liborPeriodDiscretization = (TimeDiscretization)dataModified.getOrDefault("liborPeriodDiscretization", liborPeriodDiscretization);
			numberOfFactors = (int)dataModified.getOrDefault("numberOfFactors", numberOfFactors);
			a = (double)dataModified.getOrDefault("a", a);
			isCalibrateable = (boolean)dataModified.getOrDefault("isCalibrateable", isCalibrateable);
		}

		LIBORCorrelationModel newModel = new LIBORCorrelationModelExponentialDecay(timeDiscretization, liborPeriodDiscretization, numberOfFactors, a, isCalibrateable);
		return newModel;
	}
}
