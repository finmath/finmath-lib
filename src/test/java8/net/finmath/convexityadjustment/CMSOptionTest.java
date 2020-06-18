/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 10.03.2013
 */
package net.finmath.convexityadjustment;

import java.text.DecimalFormat;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.exception.CalculationException;
import net.finmath.functions.AnalyticFormulas;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurveInterpolation;
import net.finmath.marketdata.products.Swap;
import net.finmath.marketdata.products.SwapAnnuity;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.BrownianMotionFromMersenneRandomNumbers;
import net.finmath.montecarlo.interestrate.LIBORMonteCarloSimulationFromLIBORModel;
import net.finmath.montecarlo.interestrate.models.LIBORMarketModelFromCovarianceModel;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCorrelationModelExponentialDecay;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCovarianceModelFromVolatilityAndCorrelation;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORVolatilityModelTwoParameterExponentialForm;
import net.finmath.montecarlo.interestrate.products.CMSOption;
import net.finmath.montecarlo.interestrate.products.Caplet;
import net.finmath.montecarlo.interestrate.products.Swaption;
import net.finmath.montecarlo.interestrate.products.SwaptionAnalyticApproximation;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

public class CMSOptionTest {

	// Model properties
	private final double	initialValue		= 0.10;
	private final double	volatility			= 0.10;
	private final int		numberOfFactors		= 5;
	private final double	correlationDecay	= 0.1;

	// Process discretization properties
	private final int		numberOfPaths		= 10000;
	private final int		numberOfTimeSteps	= 15;
	private final double	deltaT				= 0.5;

	// LIBOR tenor discretization
	private final int		numberOfPeriods		= 30;
	private final double	periodLength		= 0.5;

	// Random number generator seed
	private final int		seed				= 3141;

	// Java DecimalFormat for our output format
	static final	DecimalFormat	formatterPercent	= new DecimalFormat("0.0000%");

	@Test
	public void testCMSOption() throws CalculationException {

		// Create a flat forward rate curve
		final ForwardCurve forwardCurve = ForwardCurveInterpolation.createForwardCurveFromForwards("forwardCurve",
				new double[] { 0.0, numberOfPeriods*periodLength },
				new double[] { initialValue, initialValue },
				periodLength);

		// Create a LIBOR market model Monte-Carlo simulation
		final LIBORMonteCarloSimulationFromLIBORModel liborMarketModelMonteCarloSimulation = this.getLIBORModelMonteCarloSimulation(forwardCurve);

		final double		exerciseDate	= 5.0;
		final double[]	fixingDates		= {5.0, 5.5, 6.0, 6.5, 7.0, 7.5};
		final double[]	paymentDates	= {5.5, 6.0, 6.5, 7.0, 7.5, 8.0};
		final double[]	swapTenor		= {5.0, 5.5, 6.0, 6.5, 7.0, 7.5, 8.0};
		final double[]	periodLengths	= {0.5, 0.5, 0.5, 0.5, 0.5, 0.5};
		final double		strike			= 0.100;
		final double[]	swaprates		= new double[periodLengths.length];
		java.util.Arrays.fill(swaprates, strike);

		/*
		 *  Value the CMS Option
		 */

		// Calculate approximate swaprate volatility from LIBOR market model (analytic).
		final SwaptionAnalyticApproximation swaptionAnalytic	= new SwaptionAnalyticApproximation(strike, swapTenor, SwaptionAnalyticApproximation.ValueUnit.INTEGRATEDVARIANCELOGNORMAL);
		final double swaprateIntegratedVariance				= swaptionAnalytic.getValue(liborMarketModelMonteCarloSimulation);
		final double swaprateVolatility						= Math.sqrt(swaprateIntegratedVariance/exerciseDate);

		// Create CMS Option
		final CMSOption	cmsOption	= new CMSOption(exerciseDate, fixingDates, paymentDates, periodLengths, strike);

		// Value using LMM
		final double valueCMSOptionLMM = cmsOption.getValue(liborMarketModelMonteCarloSimulation);
		System.out.println("CMS Option with LIBOR Market Model..........................:\t" + formatterPercent.format(valueCMSOptionLMM));

		// Value using analytics model
		final double valueCMSOptionHK	= cmsOption.getValue(forwardCurve, swaprateVolatility);
		System.out.println("CMS Option with Hunt-Kennedy/Black-Scholes..................:\t" + formatterPercent.format(valueCMSOptionHK));

		// Value using convexity adjusted forward rate in a Black-Scholes formula
		final TimeDiscretization fixTenor	= new TimeDiscretizationFromArray(swapTenor);
		final TimeDiscretization floatTenor	= new TimeDiscretizationFromArray(swapTenor);
		final double rate = Swap.getForwardSwapRate(fixTenor, floatTenor, forwardCurve);
		final double swapAnnuity			= SwapAnnuity.getSwapAnnuity(fixTenor, forwardCurve);
		final double payoffUnit			= SwapAnnuity.getSwapAnnuity(new TimeDiscretizationFromArray(swapTenor[0], swapTenor[1]), forwardCurve) / (swapTenor[1]-swapTenor[0]);
		final double adjustedCMSRate = AnalyticFormulas.huntKennedyCMSAdjustedRate(rate, swaprateVolatility, swapAnnuity, exerciseDate, swapTenor[swapTenor.length-1]-swapTenor[0], payoffUnit);
		final double valueCMSOptionHKAdjRate	= AnalyticFormulas.blackModelSwaptionValue(adjustedCMSRate, swaprateVolatility, exerciseDate, strike, payoffUnit) * (swapTenor[1]-swapTenor[0]);
		System.out.println("CMS Option with Black-Scholes using Adjusted Forward Swapate:\t" + formatterPercent.format(valueCMSOptionHKAdjRate));

		System.out.println("\nInfo:");
		System.out.println("Forward Swaprate............................................:\t" + formatterPercent.format(rate));
		System.out.println("Convexity Adjusted Forward Swaprate (Hunt-Kennedy)..........:\t" + formatterPercent.format(adjustedCMSRate));

		/*
		 * jUnit assertion: condition under which we consider this test successful
		 */
		Assert.assertEquals("Value", valueCMSOptionLMM, valueCMSOptionHK, 1E-3);
		/*
		 * Value a caplet with same fixing date
		 */
		final Caplet		caplet		= new Caplet(fixingDates[0], periodLengths[0], strike);
		final double valueCaplet = caplet.getValue(liborMarketModelMonteCarloSimulation);
		System.out.println("Caplet with LIBOR Market Model..............................:\t" + formatterPercent.format(valueCaplet));

		/*
		 * Value a swaption with same swap tenor and exercise date
		 */
		final Swaption	swaption			= new Swaption(exerciseDate, fixingDates, paymentDates, periodLengths, swaprates);
		final double		swaptionNotional	= payoffUnit / swapAnnuity * (swapTenor[1]-swapTenor[0]);

		final double valueSwp = swaption.getValue(liborMarketModelMonteCarloSimulation);
		System.out.println("Swaption with LIBOR Market Model............................:\t" + formatterPercent.format(valueSwp * swaptionNotional));

		final double valueSwaptionAnalytic = swaption.getValue(forwardCurve, swaprateVolatility);
		System.out.println("Swaption with Black-Scholes.................................:\t" + formatterPercent.format(valueSwaptionAnalytic * swaptionNotional));
	}

	public LIBORMonteCarloSimulationFromLIBORModel getLIBORModelMonteCarloSimulation(final ForwardCurve forwardCurve) throws CalculationException {
		// Create the time discretization
		final TimeDiscretization timeDiscretization = new TimeDiscretizationFromArray(0.0, numberOfTimeSteps, deltaT);

		// Create the tenor discretization
		final TimeDiscretization tenorDiscretization = new TimeDiscretizationFromArray(0.0, numberOfPeriods, periodLength);

		/*
		 * Create LIBOR Market Model
		 */
		final LIBORMarketModelFromCovarianceModel liborMarketModel = new LIBORMarketModelFromCovarianceModel(
				tenorDiscretization,
				forwardCurve,
				new LIBORCovarianceModelFromVolatilityAndCorrelation(
						timeDiscretization, tenorDiscretization,
						new LIBORVolatilityModelTwoParameterExponentialForm(timeDiscretization, tenorDiscretization, volatility, 0.0),
						new LIBORCorrelationModelExponentialDecay(timeDiscretization, tenorDiscretization, numberOfFactors, correlationDecay, false))
				);

		final BrownianMotion brownianMotion = new BrownianMotionFromMersenneRandomNumbers(timeDiscretization, numberOfFactors, numberOfPaths, seed);

		final EulerSchemeFromProcessModel process = new EulerSchemeFromProcessModel(liborMarketModel, brownianMotion);
		//		process.setScheme(EulerSchemeFromProcessModel.Scheme.PREDICTOR_CORRECTOR);

		final LIBORMonteCarloSimulationFromLIBORModel liborMarketModelMonteCarloSimulation =  new LIBORMonteCarloSimulationFromLIBORModel(process);

		return liborMarketModelMonteCarloSimulation;
	}
}
