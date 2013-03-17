/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 17.05.2007
 */
package net.finmath.montecarlo.interestrate.products;

import net.finmath.functions.AnalyticFormulas;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.montecarlo.interestrate.LIBORMarketModel;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.ImmutableRandomVariableInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * This class implements an analytic swaption pricing formula under a LIBOR market model.
 * 
 * @author Christian Fries
 */
public class SwaptionAnalyticApproximation extends AbstractLIBORMonteCarloProduct {

    public enum ValueUnit {
        VALUE,
        INTEGRATEDVARIANCE,
        VOLATILITY
    };

    private double      swaprate;
    private double[]    swapTenor;       // Vector of swap tenor (period start and end dates). Start of first period is the option maturity.
    private ValueUnit   valueUnit;

    /**
     * Note: It is implicitly assumed that swapTenor[0] is the exercise date (no forward starting).
     * @param swaprate The strike swaprate of the swaption.
     * @param swapTenor The swap tenor in doubles.
     */
    public SwaptionAnalyticApproximation(double swaprate, TimeDiscretizationInterface swapTenor) {
        this(swaprate, swapTenor.getAsDoubleArray(), SwaptionAnalyticApproximation.ValueUnit.VALUE);
    }

    /**
     * Note: It is implicitly assumed that swapTenor[0] is the exercise date (no forward starting).
     * @param swaprate The strike swaprate of the swaption.
     * @param swapTenor The swap tenor in doubles.
     * @param valueUnit See <code>getValue(AbstractLIBORMarketModel model)</code>
     */
    public SwaptionAnalyticApproximation(double swaprate, double[] swapTenor, ValueUnit valueUnit) {
        super();
        this.swaprate = swaprate;
        this.swapTenor = swapTenor;
        this.valueUnit = valueUnit;
    }

    @Override
    public RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) {
    	return getValues(evaluationTime, (LIBORMarketModel)model.getModel());
    }
    
    /**
     * Calculates the approximated integrated instantaneous variance of the swap rate,
     * using the approximation d log(S(t))/d log(L(t)) = d log(S(0))/d log(L(0)).
     * 
     * @param model A model implementing the LIBORModelMonteCarloSimulationInterface
     * @return Depending on the value of value unit, the method returns either
     * the approximated integrated instantaneous variance of the swap rate (ValueUnit.INTEGRATEDVARIANCE)
     * or the value using the Black formula (ValueUnit.VALUE).
     * @TODO make initial values an arg and use evaluation time.
     */
    public RandomVariableInterface getValues(double evaluationTime, LIBORMarketModel model) {
    	if(evaluationTime > 0) throw new RuntimeException("Forward start evaluation currently not supported.");

        double swapStart    = swapTenor[0];
        double swapEnd      = swapTenor[swapTenor.length-1];
        
        int swapStartIndex  = model.getLiborPeriodIndex(swapStart);
        int swapEndIndex    = model.getLiborPeriodIndex(swapEnd);
        int optionMaturityIndex = model.getTimeIndex(swapStart)-1;
        
        double[][]  logSwaprateDerivative  = getLogSwaprateDerivative(model.getLiborPeriodDiscretization(), model.getInitialValue(), swapTenor);
        double[]    discountFactors        = logSwaprateDerivative[0];
        double[]    swapAnnuities          = logSwaprateDerivative[1];
        double[]    swapCovarianceWeights  = logSwaprateDerivative[2];

//        double[][][]	integratedLIBORCovariance = SwaptionAnalyticApproximation.getIntegratedLIBORCovariance(model);
        double[][][]	integratedLIBORCovariance = model.getIntegratedLIBORCovariance();

        // Caclculate integrated model covariance
        double integratedSwapRateVariance = 0.0;

        for(int componentIndex1 = swapStartIndex; componentIndex1 < swapEndIndex; componentIndex1++) {
            // Sum the libor cross terms (use symmetry)
            for(int componentIndex2 = componentIndex1+1; componentIndex2 < swapEndIndex; componentIndex2++) {
                integratedSwapRateVariance += 2.0 * swapCovarianceWeights[componentIndex1-swapStartIndex] * swapCovarianceWeights[componentIndex2-swapStartIndex] * integratedLIBORCovariance[componentIndex1][componentIndex2][optionMaturityIndex];
            }

            // Add diagonal term (libor variance term)
            integratedSwapRateVariance += swapCovarianceWeights[componentIndex1-swapStartIndex] * swapCovarianceWeights[componentIndex1-swapStartIndex] * integratedLIBORCovariance[componentIndex1][componentIndex1][optionMaturityIndex];
        }

        // Return integratedSwapRateVariance if requested
        if(valueUnit == SwaptionAnalyticApproximation.ValueUnit.INTEGRATEDVARIANCE) return new RandomVariable(evaluationTime, integratedSwapRateVariance);

        double volatility		= Math.sqrt(integratedSwapRateVariance / swapStart);

        // Return integratedSwapRateVariance if requested
        if(valueUnit == SwaptionAnalyticApproximation.ValueUnit.VOLATILITY) return new RandomVariable(evaluationTime, volatility);

        // Use black formula for swaption to calculate the price
        double swapAnnuity      =   swapAnnuities[0];
        double parSwaprate      =   (discountFactors[0] - discountFactors[swapEndIndex-swapStartIndex]) / swapAnnuity;

        double optionMaturity	= swapStart;

        double valueSwaption = AnalyticFormulas.blackModelSwaptionValue(parSwaprate, volatility, optionMaturity, this.swaprate, swapAnnuity);
        return new RandomVariable(evaluationTime, valueSwaption);
    }
    
    static public double[][] getLogSwaprateDerivative(TimeDiscretizationInterface liborPeriodDiscretization, ImmutableRandomVariableInterface[] immutableRandomVariableInterfaces, double[] swapTenor) {
        double swapStart    = swapTenor[0];
        double swapEnd      = swapTenor[swapTenor.length-1];
        
        int swapStartIndex  = liborPeriodDiscretization.getTimeIndex(swapStart);
        int swapEndIndex    = liborPeriodDiscretization.getTimeIndex(swapEnd);

        // Precalculate discount factors
        double[] forwardRates       = new double[swapEndIndex-swapStartIndex+1];
        double[] discountFactors    = new double[swapEndIndex-swapStartIndex+1];

        discountFactors[0] = 1.0;
        for(int liborPeriodIndex = 0; liborPeriodIndex < swapStartIndex; liborPeriodIndex++) {
            double libor = immutableRandomVariableInterfaces[liborPeriodIndex].get(0);
            double liborPeriodLength = liborPeriodDiscretization.getTimeStep(liborPeriodIndex);
            discountFactors[0] /= (1 + libor * liborPeriodLength);
        }

        // Calculate discount factors for swap period ends (used for swap annuity)
        for(int liborPeriodIndex = swapStartIndex; liborPeriodIndex < swapEndIndex; liborPeriodIndex++) {
            double libor = immutableRandomVariableInterfaces[liborPeriodIndex].get(0);
            double liborPeriodLength = liborPeriodDiscretization.getTimeStep(liborPeriodIndex);

            forwardRates[liborPeriodIndex-swapStartIndex]       = libor;
            discountFactors[liborPeriodIndex-swapStartIndex+1]  = discountFactors[liborPeriodIndex-swapStartIndex] / (1 + libor * liborPeriodLength);
        }

        // Precalculate swap annuities
        double[]    swapAnnuities   = new double[swapTenor.length-1];
        double      swapAnnuity     = 0.0;
        for(int swapPeriodIndex = swapTenor.length-2; swapPeriodIndex >= 0; swapPeriodIndex--) {
            int periodEndIndex = liborPeriodDiscretization.getTimeIndex(swapTenor[swapPeriodIndex+1]);
            swapAnnuity += discountFactors[periodEndIndex-swapStartIndex] * (swapTenor[swapPeriodIndex+1]-swapTenor[swapPeriodIndex]);
            swapAnnuities[swapPeriodIndex] = swapAnnuity;
        }
        

        // Precalculate weights
        double longForwardRate = discountFactors[swapEndIndex-swapStartIndex] / ( discountFactors[0] - discountFactors[swapEndIndex-swapStartIndex]);
        
        double[] swapCovarianceWeights = new double[swapEndIndex-swapStartIndex];

        int swapPeriodIndex = 0;
        for(int liborPeriodIndex = swapStartIndex; liborPeriodIndex < swapEndIndex; liborPeriodIndex++) {
            if(liborPeriodDiscretization.getTime(liborPeriodIndex) >= swapTenor[swapPeriodIndex+1]) swapPeriodIndex++;
            
            swapCovarianceWeights[liborPeriodIndex-swapStartIndex] = (longForwardRate + swapAnnuities[swapPeriodIndex] / swapAnnuity) * (1.0 - discountFactors[liborPeriodIndex-swapStartIndex+1] / discountFactors[liborPeriodIndex-swapStartIndex]);
        }

        return new double[][] { discountFactors, swapAnnuities, swapCovarianceWeights };
    }
    
    static public double[][][] getIntegratedLIBORCovariance(LIBORMarketModel model) {
    	return model.getIntegratedLIBORCovariance();
    }
 }
