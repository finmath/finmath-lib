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
	private final 	double		a;
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
	public LIBORCorrelationModelExponentialDecay(final TimeDiscretization timeDiscretization, final TimeDiscretization liborPeriodDiscretization, final int numberOfFactors, final double a, final boolean isCalibrateable) {
		super(timeDiscretization, liborPeriodDiscretization);

		this.numberOfFactors	= numberOfFactors;
		this.a					= a;
		this.isCalibrateable	= isCalibrateable;

		initialize(numberOfFactors, a);
	}

	public LIBORCorrelationModelExponentialDecay(final TimeDiscretization timeDiscretization, final TimeDiscretization liborPeriodDiscretization, final int numberOfFactors, final double a) {
		super(timeDiscretization, liborPeriodDiscretization);

		this.numberOfFactors	= numberOfFactors;
		this.a					= a;
		isCalibrateable	= false;

		initialize(numberOfFactors, a);
	}

	@Override
	public LIBORCorrelationModelExponentialDecay getCloneWithModifiedParameter(final RandomVariable[] parameter) {
		if(!isCalibrateable) {
			return this;
		}

		return new LIBORCorrelationModelExponentialDecay(getTimeDiscretization(), getLiborPeriodDiscretization(), numberOfFactors, parameter[0].doubleValue());
	}

	@Override
	public Object clone() {
		return new LIBORCorrelationModelExponentialDecay(getTimeDiscretization(), getLiborPeriodDiscretization(), numberOfFactors, a, isCalibrateable);
	}

	@Override
	public double	getFactorLoading(final int timeIndex, final int factor, final int component) {
		return factorMatrix[component][factor];
	}

	@Override
	public double	getCorrelation(final int timeIndex, final int component1, final int component2) {
		return correlationMatrix[component1][component2];
	}

	@Override
	public int		getNumberOfFactors() {
		return factorMatrix[0].length;
	}

	private void initialize(final int numberOfFactors, double a) {
		/*
		 * Create instantaneous correlation matrix
		 */

		// Negative values of a do not make sense.
		a = Math.max(a, 0);

		correlationMatrix = new double[getLiborPeriodDiscretization().getNumberOfTimeSteps()][getLiborPeriodDiscretization().getNumberOfTimeSteps()];
		for(int row=0; row<correlationMatrix.length; row++) {
			for(int col=0; col<correlationMatrix[row].length; col++) {
				// Exponentially decreasing instantaneous correlation
				correlationMatrix[row][col] = Math.exp(-a * Math.abs(getLiborPeriodDiscretization().getTime(row)-getLiborPeriodDiscretization().getTime(col)));
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

		final RandomVariable[] parameter = new RandomVariable[1];

		parameter[0] = new Scalar(a);

		return parameter;
	}

	@Override
	public LIBORCorrelationModel getCloneWithModifiedData(final Map<String, Object> dataModified) {
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

		final LIBORCorrelationModel newModel = new LIBORCorrelationModelExponentialDecay(timeDiscretization, liborPeriodDiscretization, numberOfFactors, a, isCalibrateable);
		return newModel;
	}
}
