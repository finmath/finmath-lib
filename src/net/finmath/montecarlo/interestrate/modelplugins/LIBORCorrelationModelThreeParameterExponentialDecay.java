/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2006
 */
package net.finmath.montecarlo.interestrate.modelplugins;

import net.finmath.functions.LinearAlgebra;
import net.finmath.time.TimeDiscretizationInterface;


/**
 * @author Christian Fries
 *
 */
public class LIBORCorrelationModelThreeParameterExponentialDecay extends LIBORCorrelationModel {
	
	private int		numberOfFactors;
	private double	a;
	private double	b;
	private double	c;
	private final boolean isCalibrateable;

	private double[][]	correlationMatrix;
	private double[][]	factorMatrix;
	
	public LIBORCorrelationModelThreeParameterExponentialDecay(TimeDiscretizationInterface timeDiscretization, TimeDiscretizationInterface liborPeriodDiscretization, int numberOfFactors, double a, double b, double c, boolean isCalibrateable) {
		super(timeDiscretization, liborPeriodDiscretization);
		
		this.numberOfFactors = numberOfFactors;
		this.a = a;
		this.b = b;
		this.c = c;
		this.isCalibrateable = isCalibrateable;

		initialize(numberOfFactors, a, b, c);
	}

	@Override
	public double[] getParameter() {
		if(!isCalibrateable) return null;

		double[] parameter = new double[3];

		parameter[0] = a;
		parameter[1] = b;
		parameter[2] = c;

		return parameter;
	}

	@Override
	public void setParameter(double[] parameter) {
		if(!isCalibrateable) return;

		a = parameter[0];
		b = parameter[1];
		c = parameter[2];

		initialize(numberOfFactors, a, b, c);
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
		return numberOfFactors;
	}

	public void setParameters(int numberOfFactors, double a, double b, double c) {
		this.numberOfFactors = numberOfFactors;
		this.a = a;
		this.b = b;
		this.c = c;
		initialize(numberOfFactors, a, b, c);
	}

	private void initialize(int numberOfFactors, double a, double b, double c) {
		/*
		 * Create instantaneous correlation matrix
		 */

		a = Math.max(a, 0.0);
		b = Math.min(Math.max(b, 0.0), 1.0);
		c = Math.max(c, 0.0);

		correlationMatrix = new double[liborPeriodDiscretization.getNumberOfTimeSteps()][liborPeriodDiscretization.getNumberOfTimeSteps()];
		for(int row=0; row<correlationMatrix.length; row++) {
			for(int col=0; col<correlationMatrix[row].length; col++) {
				// Exponentially decreasing instantaneous correlation
				double T1 = liborPeriodDiscretization.getTime(row);
				double T2 = liborPeriodDiscretization.getTime(col);
				double correlation = b + (1-b) * Math.exp(-a * Math.abs(T1 - T2) - c * Math.max(T1, T2));
				correlationMatrix[row][col] = correlation;
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
	public Object clone() {
		return new LIBORCorrelationModelThreeParameterExponentialDecay(
				super.getTimeDiscretization(),
				super.getLiborPeriodDiscretization(),
				numberOfFactors,
				a, b, c, isCalibrateable);
	}
}
