/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christianfries.com.
 *
 * Created on 07.05.2013
 */

package net.finmath.montecarlo.hybridassetinterestrate;

import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.MonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.montecarlo.process.MonteCarloProcessFromProcessModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
import net.finmath.time.TimeDiscretization;

/**
 * Cross Currency LIBOR Market Model with Black-Scholes FX Model.
 *
 * @author Christian Fries
 */
public class CrossCurrencyLIBORMarketModelFromModels implements HybridAssetMonteCarloSimulation {

	private final String										baseModel;

	private final Map<String, LIBORModelMonteCarloSimulationModel>	interestRatesModels;
	private final Map<String, MonteCarloProcessFromProcessModel> 	fxModels;

	/**
	 * Create a Cross Currency LIBOR Market Model with Black-Scholes FX Model.
	 *
	 * @param baseModel The name of the interest rate model used for the numeraire.
	 * @param interestRatesModels A collection of single currency interest rate models.
	 * @param fxModels A collection of (corresponding) fx models.
	 */
	public CrossCurrencyLIBORMarketModelFromModels(String baseModel,
			Map<String, LIBORModelMonteCarloSimulationModel> interestRatesModels,
			Map<String, MonteCarloProcessFromProcessModel> fxModels) {
		super();
		this.baseModel				= baseModel;
		this.interestRatesModels	= interestRatesModels;
		this.fxModels				= fxModels;
	}


	public LIBORModelMonteCarloSimulationModel getBaseModel() {
		return interestRatesModels.get(baseModel);
	}

	@Override
	public RandomVariable getRandomVariableForConstant(double value) {
		return getBaseModel().getRandomVariableForConstant(value);
	}

	@Override
	public int getNumberOfPaths() {
		return getBaseModel().getNumberOfPaths();
	}

	@Override
	public TimeDiscretization getTimeDiscretization() {
		return getBaseModel().getTimeDiscretization();
	}

	@Override
	public double getTime(int timeIndex) {
		return getBaseModel().getTime(timeIndex);
	}

	@Override
	public int getTimeIndex(double time) {
		return getBaseModel().getTimeIndex(time);
	}

	@Override
	public RandomVariable getMonteCarloWeights(int timeIndex) throws CalculationException {
		// TODO Check for equal weights on all providers!
		return getBaseModel().getMonteCarloWeights(timeIndex);
	}

	@Override
	public RandomVariable getMonteCarloWeights(double time) throws CalculationException {
		// TODO Check for equal weights on all providers!
		return getBaseModel().getMonteCarloWeights(time);
	}

	@Override
	public MonteCarloSimulationModel getCloneWithModifiedData(Map<String, Object> dataModified) throws CalculationException {
		throw new UnsupportedOperationException("Method not implemented.");
	}

	@Override
	public RandomVariable getNumeraire(double time) throws CalculationException {
		return getBaseModel().getNumeraire(time);
	}

	@Override
	public RandomVariable getNumeraire(String account, double time) throws CalculationException {
		return interestRatesModels.get(account).getNumeraire(time);
	}

	@Override
	public RandomVariable getValue(RiskFactorID riskFactorIdentifyer, double time) throws CalculationException {
		if(riskFactorIdentifyer instanceof RiskFactorForwardRate) {
			final RiskFactorForwardRate riskFactorForwardRate = (RiskFactorForwardRate)riskFactorIdentifyer;
			final LIBORModelMonteCarloSimulationModel riskFactorModel = interestRatesModels.get(riskFactorIdentifyer.getName());

			final RandomVariable forwardRate = riskFactorModel.getForwardRate(time,riskFactorForwardRate.getPeriodStart(), riskFactorForwardRate.getPeriodEnd());

			if(!riskFactorIdentifyer.getName().equals(baseModel)) {
				final MonteCarloProcessFromProcessModel riskFactorModelFX = fxModels.get(riskFactorIdentifyer.getName());
				final RandomVariable fxRate = riskFactorModelFX.getProcessValue(riskFactorModelFX.getTimeIndex(time), 0);
				//				forwardRate = forwardRate.div(fxRate);
			}

			return forwardRate;
		}
		else if(riskFactorIdentifyer instanceof RiskFactorFX) {
			if(riskFactorIdentifyer.getName().equals(baseModel)) {
				return new Scalar(1.0);
			}

			final MonteCarloProcessFromProcessModel riskFactorModelFX = fxModels.get(riskFactorIdentifyer.getName());
			RandomVariable fxRate = riskFactorModelFX.getProcessValue(riskFactorModelFX.getTimeIndex(time), 0);

			final LIBORModelMonteCarloSimulationModel riskFactorModel = interestRatesModels.get(riskFactorIdentifyer.getName());
			fxRate = fxRate.mult(getNumeraire(time)).div(riskFactorModel.getNumeraire(time));
			return fxRate;
		}
		else {
			throw new IllegalArgumentException("Risk factor unsupported: " + riskFactorIdentifyer);
		}
	}

	LIBORModelMonteCarloSimulationModel getInterestRateModel(String model) {
		return interestRatesModels.get(model);
	}
}
