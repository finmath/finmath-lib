package net.finmath.finitedifference.assetderivativevaluation.products;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import net.finmath.exception.CalculationException;
import net.finmath.finitedifference.assetderivativevaluation.models.FDMBlackScholesModel;
import net.finmath.finitedifference.assetderivativevaluation.models.FDMHestonModel;
import net.finmath.finitedifference.assetderivativevaluation.models.FDMVarianceGammaModel;
import net.finmath.finitedifference.assetderivativevaluation.models.FiniteDifferenceEquityModel;
import net.finmath.finitedifference.grids.Grid;
import net.finmath.finitedifference.grids.SpaceTimeDiscretization;
import net.finmath.finitedifference.grids.UniformGrid;
import net.finmath.finitedifference.utilities.FiniteDifferenceValueInterpolator;
import net.finmath.modelling.products.CallOrPut;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.BrownianMotionFromMersenneRandomNumbers;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel;
import net.finmath.montecarlo.assetderivativevaluation.MonteCarloAssetModel;
import net.finmath.montecarlo.assetderivativevaluation.MonteCarloBlackScholesModel;
import net.finmath.montecarlo.assetderivativevaluation.MonteCarloVarianceGammaModel;
import net.finmath.montecarlo.assetderivativevaluation.models.HestonModel;
import net.finmath.montecarlo.assetderivativevaluation.models.HestonModel.Scheme;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * Compares finite-difference prices of European options against Monte Carlo
 * prices under Black-Scholes, Heston, and Variance Gamma models.
 *
 * @author Alessandro Gnoatto
 */
public class FiniteDifferenceVersusMonteCarloEuropeanOptionTest {

	private static final double INITIAL_VALUE = 100.0;
	private static final double RISK_FREE_RATE = 0.05;
	private static final double DIVIDEND_YIELD = 0.0;

	private static final double[] MATURITIES = new double[] {
			0.5,
			1.0
	};

	private static final double[] STRIKES = new double[] {
			80.0,
			100.0,
			120.0
	};

	private static final CallOrPut[] CALL_OR_PUTS = new CallOrPut[] {
			CallOrPut.CALL,
			CallOrPut.PUT
	};

	private static final double BLACK_SCHOLES_VOLATILITY = 0.20;

	private static final double HESTON_INITIAL_VARIANCE = 0.04;
	private static final double HESTON_KAPPA = 1.50;
	private static final double HESTON_THETA = 0.04;
	private static final double HESTON_XI = 0.30;
	private static final double HESTON_RHO = -0.70;

	private static final double VARIANCE_GAMMA_SIGMA = 0.30;
	private static final double VARIANCE_GAMMA_THETA = -0.10;
	private static final double VARIANCE_GAMMA_NU = 0.20;
	private static final double VARIANCE_GAMMA_LOWER_INTEGRATION_BOUND = -2.0;
	private static final double VARIANCE_GAMMA_UPPER_INTEGRATION_BOUND = 2.0;

	private static final double FINITE_DIFFERENCE_THETA = 0.5;

	private static final int NUMBER_OF_TIME_STEPS_FD_1D = 160;
	private static final int NUMBER_OF_SPACE_STEPS_FD_1D = 220;

	private static final int NUMBER_OF_TIME_STEPS_FD_HESTON = 60;
	private static final int NUMBER_OF_SPACE_STEPS_FD_HESTON_SPOT = 140;
	private static final int NUMBER_OF_SPACE_STEPS_FD_HESTON_VARIANCE = 90;

	private static final int NUMBER_OF_TIME_STEPS_MC = 250;
	private static final int NUMBER_OF_PATHS_MC = 50000;
	private static final int RANDOM_SEED = 31415;

	private static final double MONTE_CARLO_STANDARD_ERROR_MULTIPLIER = 4.0;

	private static final double BLACK_SCHOLES_FD_ALLOWANCE = 0.35;
	private static final double HESTON_FD_ALLOWANCE = 1.50;
	private static final double VARIANCE_GAMMA_FD_ALLOWANCE = 1.75;

	@Test
	public void testBlackScholesEuropeanOptionsAgainstMonteCarlo() throws CalculationException {

		for(final double maturity : MATURITIES) {

			final FDMBlackScholesModel finiteDifferenceModel =
					createBlackScholesFiniteDifferenceModel(maturity);

			final AssetModelMonteCarloSimulationModel monteCarloModel =
					createBlackScholesMonteCarloModel(maturity);

			for(final double strike : STRIKES) {
				for(final CallOrPut callOrPut : CALL_OR_PUTS) {

					final double finiteDifferencePrice =
							getFiniteDifferencePrice(
									finiteDifferenceModel,
									maturity,
									strike,
									callOrPut
									);

					final MonteCarloEstimate monteCarloPrice =
							getMonteCarloEuropeanOptionValue(
									monteCarloModel,
									maturity,
									strike,
									callOrPut
									);

					assertFiniteDifferenceCloseToMonteCarlo(
							"Black-Scholes",
							maturity,
							strike,
							callOrPut,
							finiteDifferencePrice,
							monteCarloPrice,
							BLACK_SCHOLES_FD_ALLOWANCE
							);
				}
			}
		}
	}

	@Test
	public void testHestonEuropeanOptionsAgainstMonteCarlo() throws CalculationException {

		for(final double maturity : MATURITIES) {

			final FDMHestonModel finiteDifferenceModel =
					createHestonFiniteDifferenceModel(maturity);

			final AssetModelMonteCarloSimulationModel monteCarloModel =
					createHestonMonteCarloModel(maturity);

			for(final double strike : STRIKES) {
				for(final CallOrPut callOrPut : CALL_OR_PUTS) {

					final double finiteDifferencePrice =
							getFiniteDifferencePrice(
									finiteDifferenceModel,
									maturity,
									strike,
									callOrPut
									);

					final MonteCarloEstimate monteCarloPrice =
							getMonteCarloEuropeanOptionValue(
									monteCarloModel,
									maturity,
									strike,
									callOrPut
									);

					assertFiniteDifferenceCloseToMonteCarlo(
							"Heston",
							maturity,
							strike,
							callOrPut,
							finiteDifferencePrice,
							monteCarloPrice,
							HESTON_FD_ALLOWANCE
							);
				}
			}
		}
	}

	@Test
	public void testVarianceGammaEuropeanOptionsAgainstMonteCarlo() throws CalculationException {

		for(final double maturity : MATURITIES) {

			final FDMVarianceGammaModel finiteDifferenceModel =
					createVarianceGammaFiniteDifferenceModel(maturity);

			final AssetModelMonteCarloSimulationModel monteCarloModel =
					createVarianceGammaMonteCarloModel(maturity);

			for(final double strike : STRIKES) {
				for(final CallOrPut callOrPut : CALL_OR_PUTS) {

					final double finiteDifferencePrice =
							getFiniteDifferencePrice(
									finiteDifferenceModel,
									maturity,
									strike,
									callOrPut
									);

					final MonteCarloEstimate monteCarloPrice =
							getMonteCarloEuropeanOptionValue(
									monteCarloModel,
									maturity,
									strike,
									callOrPut
									);

					assertFiniteDifferenceCloseToMonteCarlo(
							"Variance Gamma",
							maturity,
							strike,
							callOrPut,
							finiteDifferencePrice,
							monteCarloPrice,
							VARIANCE_GAMMA_FD_ALLOWANCE
							);
				}
			}
		}
	}

	private static FDMBlackScholesModel createBlackScholesFiniteDifferenceModel(
			final double maturity) {

		return new FDMBlackScholesModel(
				INITIAL_VALUE,
				RISK_FREE_RATE,
				DIVIDEND_YIELD,
				BLACK_SCHOLES_VOLATILITY,
				createOneDimensionalSpaceTimeDiscretization(maturity)
				);
	}

	private static FDMHestonModel createHestonFiniteDifferenceModel(
			final double maturity) {

		final TimeDiscretization timeDiscretization =
				new TimeDiscretizationFromArray(
						0.0,
						NUMBER_OF_TIME_STEPS_FD_HESTON,
						maturity / NUMBER_OF_TIME_STEPS_FD_HESTON
						);

		final Grid spotGrid =
				new UniformGrid(
						NUMBER_OF_SPACE_STEPS_FD_HESTON_SPOT,
						0.0,
						250.0
						);

		final Grid varianceGrid =
				new UniformGrid(
						NUMBER_OF_SPACE_STEPS_FD_HESTON_VARIANCE,
						0.0,
						0.50
						);

		final SpaceTimeDiscretization spaceTimeDiscretization =
				new SpaceTimeDiscretization(
						new Grid[] {
								spotGrid,
								varianceGrid
						},
						timeDiscretization,
						FINITE_DIFFERENCE_THETA,
						new double[] {
								INITIAL_VALUE,
								HESTON_INITIAL_VARIANCE
						}
						);

		return new FDMHestonModel(
				INITIAL_VALUE,
				HESTON_INITIAL_VARIANCE,
				RISK_FREE_RATE,
				DIVIDEND_YIELD,
				HESTON_KAPPA,
				HESTON_THETA,
				HESTON_XI,
				HESTON_RHO,
				spaceTimeDiscretization
				);
	}

	private static FDMVarianceGammaModel createVarianceGammaFiniteDifferenceModel(
			final double maturity) {

		return new FDMVarianceGammaModel(
				INITIAL_VALUE,
				RISK_FREE_RATE,
				DIVIDEND_YIELD,
				VARIANCE_GAMMA_SIGMA,
				VARIANCE_GAMMA_NU,
				VARIANCE_GAMMA_THETA,
				VARIANCE_GAMMA_LOWER_INTEGRATION_BOUND,
				VARIANCE_GAMMA_UPPER_INTEGRATION_BOUND,
				createOneDimensionalSpaceTimeDiscretization(maturity)
				);
	}

	private static SpaceTimeDiscretization createOneDimensionalSpaceTimeDiscretization(
			final double maturity) {

		final TimeDiscretization timeDiscretization =
				new TimeDiscretizationFromArray(
						0.0,
						NUMBER_OF_TIME_STEPS_FD_1D,
						maturity / NUMBER_OF_TIME_STEPS_FD_1D
						);

		final Grid spotGrid =
				new UniformGrid(
						NUMBER_OF_SPACE_STEPS_FD_1D,
						20.0,
						220.0
						);

		return new SpaceTimeDiscretization(
				spotGrid,
				timeDiscretization,
				FINITE_DIFFERENCE_THETA,
				new double[] {
						INITIAL_VALUE
				}
				);
	}

	private static AssetModelMonteCarloSimulationModel createBlackScholesMonteCarloModel(
			final double maturity) {

		final TimeDiscretization timeDiscretization =
				new TimeDiscretizationFromArray(
						0.0,
						NUMBER_OF_TIME_STEPS_MC,
						maturity / NUMBER_OF_TIME_STEPS_MC
						);

		final BrownianMotion brownianMotion =
				new BrownianMotionFromMersenneRandomNumbers(
						timeDiscretization,
						1,
						NUMBER_OF_PATHS_MC,
						RANDOM_SEED
						);

		return new MonteCarloBlackScholesModel(
				INITIAL_VALUE,
				RISK_FREE_RATE,
				BLACK_SCHOLES_VOLATILITY,
				brownianMotion
				);
	}

	private static AssetModelMonteCarloSimulationModel createHestonMonteCarloModel(
			final double maturity) {

		final TimeDiscretization timeDiscretization =
				new TimeDiscretizationFromArray(
						0.0,
						NUMBER_OF_TIME_STEPS_MC,
						maturity / NUMBER_OF_TIME_STEPS_MC
						);

		final BrownianMotion brownianMotion =
				new BrownianMotionFromMersenneRandomNumbers(
						timeDiscretization,
						2,
						NUMBER_OF_PATHS_MC,
						RANDOM_SEED
						);

		final HestonModel hestonModel =
				new HestonModel(
						INITIAL_VALUE,
						RISK_FREE_RATE - DIVIDEND_YIELD,
						Math.sqrt(HESTON_INITIAL_VARIANCE),
						RISK_FREE_RATE,
						HESTON_THETA,
						HESTON_KAPPA,
						HESTON_XI,
						HESTON_RHO,
						Scheme.FULL_TRUNCATION
						);

		final EulerSchemeFromProcessModel process =
				new EulerSchemeFromProcessModel(hestonModel, brownianMotion);

		return new MonteCarloAssetModel(process);
	}

	private static AssetModelMonteCarloSimulationModel createVarianceGammaMonteCarloModel(
			final double maturity) {

		final TimeDiscretization timeDiscretization =
				new TimeDiscretizationFromArray(
						0.0,
						NUMBER_OF_TIME_STEPS_MC,
						maturity / NUMBER_OF_TIME_STEPS_MC
						);

		return new MonteCarloVarianceGammaModel(
				timeDiscretization,
				NUMBER_OF_PATHS_MC,
				RANDOM_SEED,
				INITIAL_VALUE,
				RISK_FREE_RATE,
				VARIANCE_GAMMA_SIGMA,
				VARIANCE_GAMMA_THETA,
				VARIANCE_GAMMA_NU
				);
	}

	private static double getFiniteDifferencePrice(
			final FiniteDifferenceEquityModel model,
			final double maturity,
			final double strike,
			final CallOrPut callOrPut) {

		final EuropeanOption option =
				new EuropeanOption(maturity, strike, callOrPut);

		final double[] valuesOnGrid = option.getValue(0.0, model);

		return FiniteDifferenceValueInterpolator.interpolateValue(
				valuesOnGrid,
				model.getSpaceTimeDiscretization(),
				model.getInitialValue()
				);
	}

	private static MonteCarloEstimate getMonteCarloEuropeanOptionValue(
			final AssetModelMonteCarloSimulationModel model,
			final double maturity,
			final double strike,
			final CallOrPut callOrPut) throws CalculationException {

		final RandomVariable underlyingAtMaturity =
				model.getAssetValue(maturity, 0);

		final double sign = callOrPut == CallOrPut.CALL ? 1.0 : -1.0;

		final RandomVariable payoff =
				underlyingAtMaturity
				.sub(strike)
				.mult(sign)
				.floor(0.0);

		final RandomVariable discountedPayoff =
				payoff
				.div(model.getNumeraire(maturity))
				.mult(model.getMonteCarloWeights(maturity))
				.mult(model.getNumeraire(0.0))
				.div(model.getMonteCarloWeights(0.0));

		return new MonteCarloEstimate(
				discountedPayoff.getAverage(),
				discountedPayoff.getStandardError()
				);
	}

	private static void assertFiniteDifferenceCloseToMonteCarlo(
			final String modelName,
			final double maturity,
			final double strike,
			final CallOrPut callOrPut,
			final double finiteDifferencePrice,
			final MonteCarloEstimate monteCarloPrice,
			final double finiteDifferenceAllowance) {

		assertTrue(
				"Finite difference price must be finite for " + modelName,
				Double.isFinite(finiteDifferencePrice)
				);

		assertTrue(
				"Monte Carlo price must be finite for " + modelName,
				Double.isFinite(monteCarloPrice.getValue())
				);

		assertTrue(
				"Finite difference price must be non-negative for " + modelName,
				finiteDifferencePrice >= -1E-10
				);

		assertTrue(
				"Monte Carlo price must be non-negative for " + modelName,
				monteCarloPrice.getValue() >= -1E-10
				);

		final double tolerance =
				finiteDifferenceAllowance
				+ MONTE_CARLO_STANDARD_ERROR_MULTIPLIER
				* monteCarloPrice.getStandardError();

		final double absoluteDifference =
				Math.abs(finiteDifferencePrice - monteCarloPrice.getValue());

		System.out.println(
				modelName
				+ " "
				+ callOrPut
				+ " T="
				+ maturity
				+ " K="
				+ strike
				+ " FD="
				+ finiteDifferencePrice
				+ " MC="
				+ monteCarloPrice.getValue()
				+ " MC stderr="
				+ monteCarloPrice.getStandardError()
				+ " tolerance="
				+ tolerance
				+ " abs diff="
				+ absoluteDifference
				);

		assertTrue(
				modelName
				+ " finite-difference price differs from Monte Carlo price."
				+ " maturity=" + maturity
				+ ", strike=" + strike
				+ ", optionType=" + callOrPut
				+ ", finiteDifferencePrice=" + finiteDifferencePrice
				+ ", monteCarloPrice=" + monteCarloPrice.getValue()
				+ ", standardError=" + monteCarloPrice.getStandardError()
				+ ", tolerance=" + tolerance,
				absoluteDifference <= tolerance
				);
	}

	private static final class MonteCarloEstimate {

		private final double value;
		private final double standardError;

		MonteCarloEstimate(
				final double value,
				final double standardError) {
			this.value = value;
			this.standardError = standardError;
		}

		double getValue() {
			return value;
		}

		double getStandardError() {
			return standardError;
		}
	}
}