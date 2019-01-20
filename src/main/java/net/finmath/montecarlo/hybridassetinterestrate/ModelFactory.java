/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 05.04.2015
 */

package net.finmath.montecarlo.hybridassetinterestrate;

import net.finmath.exception.CalculationException;
import net.finmath.functions.AnalyticFormulas;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel;
import net.finmath.montecarlo.assetderivativevaluation.MonteCarloMultiAssetBlackScholesModel;
import net.finmath.montecarlo.assetderivativevaluation.products.EuropeanOption;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.optimizer.LevenbergMarquardt;
import net.finmath.optimizer.Optimizer;
import net.finmath.optimizer.SolverException;

/**
 * Helper factory to create a simple equity hybrid LIBOR market model.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class ModelFactory {

	private static ModelFactory modelFactory;

	/**
	 * Private constructor.
	 */
	private ModelFactory() {
		// TODO Auto-generated constructor stub
	}

	public static synchronized ModelFactory getInstance() {
		if(modelFactory == null) {
			modelFactory = new ModelFactory();
		}

		return modelFactory;
	}

	/**
	 * Create a simple equity hybrid LIBOR market model with a calibration of the equity processes
	 * to a given Black-Scholes implied volatility.
	 *
	 * @param baseModel LIBOR model providing the stochastic numeraire.
	 * @param brownianMotion {@link BrownianMotion} for the asset process.
	 * @param initialValues Initial value of the asset process.
	 * @param riskFreeRate Not used (internally used to generate paths, will be later adjusted)
	 * @param correlations Correlation of the asset processes.
	 * @param maturities Maturities of the options (one for each asset process).
	 * @param strikes Strikes of the options (one for each asset process).
	 * @param volatilities Implied volatilities of the options (one for each asset process).
	 * @param discountCurve Discount curve used for the final hybrid model (not used in calibration).
	 * @return An object implementing {@link HybridAssetLIBORModelMonteCarloSimulation}, where each asset process is calibrated to a given option.
	 * @throws CalculationException Thrown if calibration fails.
	 */
	public HybridAssetLIBORModelMonteCarloSimulation getHybridAssetLIBORModel(
			final LIBORModelMonteCarloSimulationModel baseModel,
			final BrownianMotion brownianMotion,
			final double[] initialValues,
			final double riskFreeRate,
			final double[][] correlations,
			final double[] maturities,
			final double[] strikes,
			final double[] volatilities,
			final DiscountCurve discountCurve) throws CalculationException {

		Optimizer optimizer = new LevenbergMarquardt(volatilities /*initialParameters*/, volatilities /*targetValues*/, 100 /*maxIteration*/, 1 /*numberOfThreads*/) {
			private static final long serialVersionUID = -9199565564991442848L;

			@Override
			public void setValues(double[] parameters, double[] values) throws SolverException {
				AssetModelMonteCarloSimulationModel model = new MonteCarloMultiAssetBlackScholesModel(brownianMotion, initialValues, riskFreeRate, parameters, correlations);
				HybridAssetLIBORModelMonteCarloSimulationFromModels hybridModel = new HybridAssetLIBORModelMonteCarloSimulationFromModels(baseModel, model);

				try {
					for(int assetIndex=0; assetIndex<values.length; assetIndex++) {
						double df = hybridModel.getNumeraire(maturities[assetIndex]).invert().getAverage();
						double spot = hybridModel.getAssetValue(0.0, assetIndex).getAverage();
						EuropeanOption option = new EuropeanOption(maturities[assetIndex], strikes[assetIndex], assetIndex);
						double valueOptoin = option.getValue(hybridModel);
						double impliedVol = AnalyticFormulas.blackScholesOptionImpliedVolatility(spot/df, maturities[assetIndex]/*optionMaturity*/, strikes[assetIndex]/*optionStrike*/, df /*payoffUnit*/, valueOptoin);
						values[assetIndex] = impliedVol;
					}
				} catch (CalculationException e) {
					throw new SolverException(e);
				}
			}
		};

		try {
			optimizer.run();
		} catch (SolverException e) {
			if(e.getCause() instanceof CalculationException) {
				throw (CalculationException)e.getCause();
			} else {
				throw new CalculationException(e);
			}
		}

		AssetModelMonteCarloSimulationModel model = new MonteCarloMultiAssetBlackScholesModel(brownianMotion, initialValues, riskFreeRate, optimizer.getBestFitParameters(), correlations);

		/*
		 * Test calibration
		 */
		HybridAssetLIBORModelMonteCarloSimulationFromModels hybridModelWithoutDiscountAdjustment = new HybridAssetLIBORModelMonteCarloSimulationFromModels(baseModel, model, null);
		for(int assetIndex=0; assetIndex<volatilities.length; assetIndex++) {
			double df = hybridModelWithoutDiscountAdjustment.getNumeraire(maturities[assetIndex]).invert().getAverage();
			double spot = hybridModelWithoutDiscountAdjustment.getAssetValue(0.0, assetIndex).getAverage();
			EuropeanOption option = new EuropeanOption(maturities[assetIndex], strikes[assetIndex], assetIndex);
			double valueOptoin = option.getValue(hybridModelWithoutDiscountAdjustment);
			double impliedVol = AnalyticFormulas.blackScholesOptionImpliedVolatility(spot/df, maturities[assetIndex]/*optionMaturity*/, strikes[assetIndex]/*optionStrike*/, df /*payoffUnit*/, valueOptoin);
			if(Math.abs(impliedVol - volatilities[assetIndex]) > 0.01) {
				throw new CalculationException("Calibration failed");
			}
		}

		/*
		 * Construct model with discounting (options will then use the discounting spread adjustment).
		 */
		HybridAssetLIBORModelMonteCarloSimulationFromModels hybridModel = new HybridAssetLIBORModelMonteCarloSimulationFromModels(baseModel, model, discountCurve);
		return hybridModel;
	}
}
