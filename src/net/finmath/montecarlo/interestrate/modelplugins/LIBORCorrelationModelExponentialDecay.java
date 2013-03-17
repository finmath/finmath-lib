/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2006
 */
package net.finmath.montecarlo.interestrate.modelplugins;

import net.finmath.functions.LinearAlgebra;
import net.finmath.time.TimeDiscretizationInterface;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;


/**
 * @author Christian Fries
 *
 */
public class LIBORCorrelationModelExponentialDecay extends LIBORCorrelationModel {
	
	private double		numberOfFactors;
	private double		a;
	private boolean		isCalibrateable;
	

	private DoubleMatrix2D	correlationMatrix;
	private DoubleMatrix2D	factorMatrix;
	
	
	public LIBORCorrelationModelExponentialDecay(TimeDiscretizationInterface timeDiscretization, TimeDiscretizationInterface liborPeriodDiscretization, int numberOfFactors, double a, boolean isCalibrateable) {
		super(timeDiscretization, liborPeriodDiscretization);

		this.numberOfFactors	= numberOfFactors;
		this.a					= a;
		this.isCalibrateable	= isCalibrateable;

		initialize(numberOfFactors, a);
	}
	
	public LIBORCorrelationModelExponentialDecay(TimeDiscretizationInterface timeDiscretization, TimeDiscretizationInterface liborPeriodDiscretization, int numberOfFactors, double a) {
		super(timeDiscretization, liborPeriodDiscretization);

		this.numberOfFactors	= numberOfFactors;
		this.a					= a;
		this.isCalibrateable	= false;

		initialize(numberOfFactors, a);
	}

	@Override
    public double	getFactorLoading(int timeIndex, int factor, int component) {
		return factorMatrix.get(component, factor);
	}
	@Override
    public double	getCorrelation(int timeIndex, int component1, int component2) {
		return correlationMatrix.get(component1, component2);
	}
	@Override
    public int		getNumberOfFactors() {
		return factorMatrix.columns();
	}

	public void setParameters(int numberOfFactors, double a) {
		initialize(numberOfFactors, a);
	}

	private void initialize(int numberOfFactors, double a) {
		/*
		 * Create instantaneous correlation matrix
		 */
		correlationMatrix = new DenseDoubleMatrix2D(liborPeriodDiscretization.getNumberOfTimeSteps(), liborPeriodDiscretization.getNumberOfTimeSteps());		
		for(int row=0; row<correlationMatrix.rows(); row++) {
			for(int col=0; col<correlationMatrix.columns(); col++) {
				// Exponentially decreasing instantaneous correlation
				correlationMatrix.set(row, col, Math.exp(-a * Math.abs(liborPeriodDiscretization.getTime(row)-liborPeriodDiscretization.getTime(col))));
			}
		}

		/*
		 * Perform a factor decomposition (and reduction if numberOfFactors < correlationMatrix.columns())
		 */
        factorMatrix = LinearAlgebra.factorReduction(correlationMatrix, numberOfFactors);

        cern.colt.matrix.linalg.Algebra linAlg = new cern.colt.matrix.linalg.Algebra();
        correlationMatrix = linAlg.mult(factorMatrix, linAlg.transpose(factorMatrix));
	}

	@Override
	public double[] getParameter() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setParameter(double[] parameter) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Object clone() {
		// TODO Auto-generated method stub
		return null;
	}
}
