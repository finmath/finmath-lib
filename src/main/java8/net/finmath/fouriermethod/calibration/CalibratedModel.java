package net.finmath.fouriermethod.calibration;

import java.util.ArrayList;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.lang3.ArrayUtils;

import net.finmath.exception.CalculationException;
import net.finmath.fouriermethod.calibration.models.CalibratableProcess;
import net.finmath.fouriermethod.models.CharacteristicFunctionModel;
import net.finmath.fouriermethod.products.smile.EuropeanOptionSmile;
import net.finmath.marketdata.model.volatilities.OptionSmileData;
import net.finmath.marketdata.model.volatilities.OptionSurfaceData;
import net.finmath.marketdata.model.volatilities.VolatilitySurface.QuotingConvention;
import net.finmath.optimizer.Optimizer;
import net.finmath.optimizer.OptimizerFactory;
import net.finmath.optimizer.SolverException;

/**
 * This class solves a calibration problem. The problem is defined in terms of:
 * <ul>
 * <li> a generic container of market data OptionSurfaceData.
 * <li> a generic pricing model.
 * <li> a generic calibration algorithm.
 * <li> a generic pricer for claims.
 * </ul>
 *
 * The class supports both calibration in terms of:
 * <ul>
 * <li> Prices
 * <li> Log-normal implied volatilities.
 * <li> Normal implied volatilities.
 * </ul>
 *
 * To change the calibration entity please change the convention in the option surface.
 * The calibration entity (i.e. price/vol/normal vol) is directly detected from market data.
 *
 * @author Alessandro Gnoatto
 */
public class CalibratedModel {

	private final OptionSurfaceData surface; //target calibration instruments. They dictate the calibration entity: vol/price.
	private final CalibratableProcess model; //Pricing model
	private final OptimizerFactory optimizerFactory; //construct the instance of the optimization algorithm inside the class.
	private final EuropeanOptionSmile pricer; //How do we compute prices: Carr Madan, Cos, Conv, Lewis...

	//Optimizer parameters
	private final double[] initialParameters;
	private final double[] lowerBound;
	private final double[] upperBound;
	private final double[] parameterStep;

	/**
	 * Create the calibration from data.
	 *
	 * @param surface The target calibration instruments. They dictate the calibration entity: vol/price.
	 * @param model The model to calibrate.
	 * @param optimizerFactory Factory providing the optimizer to use.
	 * @param pricer How do we compute prices: Carr Madan, Cos, Conv, Lewis...
	 * @param initialParameters Initial parameters
	 * @param parameterStep Parameter steps.
	 */
	public CalibratedModel(final OptionSurfaceData surface, final CalibratableProcess model,
			final OptimizerFactory optimizerFactory, final EuropeanOptionSmile pricer, final double[] initialParameters,
			final double[] parameterStep) {
		super();
		this.surface = surface;
		this.model = model;
		this.optimizerFactory = optimizerFactory;
		this.pricer = pricer;
		this.initialParameters = initialParameters;
		lowerBound = model.getParameterLowerBounds();
		upperBound = model.getParameterUpperBounds();
		this.parameterStep = parameterStep;
	}

	/**
	 * Solves the calibration problem thus providing a calibrated model.
	 *
	 * @return the calibrated model wrapped in an {@link OptimizationResult}.
	 * @throws SolverException Thrown if the calibration problem cannot be solved.
	 */
	public OptimizationResult getCalibration() throws SolverException {

		final Optimizer.ObjectiveFunction objectiveFunction = new Optimizer.ObjectiveFunction() {
			@Override
			public void setValues(final double[] parameters, final double[] values) {

				//We change the parameters of the model
				final CalibratableProcess newModel = model.getCloneForModifiedParameters(parameters);
				final CharacteristicFunctionModel newModelFourier = newModel.getCharacteristicFunctionModel();

				final int numberOfMaturities = surface.getMaturities().length;
				final double[] mats = surface.getMaturities();

				final QuotingConvention targetConvention = surface.getQuotingConvention();

				final ArrayList<Double> vals = new ArrayList<>();

				for(int t = 0; t<numberOfMaturities; t++) {

					final double[] currentStrikes = surface.getSmile(mats[t]).getStrikes();

					final EuropeanOptionSmile newPricer = pricer.getCloneWithModifiedParameters(mats[t],currentStrikes);

					try {
						final Map<String, Function<Double, Double>> currentModelPrices = newPricer.getValue(0.0, newModelFourier);

						for(int i = 0; i<currentStrikes.length;i++) {

							if(targetConvention.equals(QuotingConvention.VOLATILITYLOGNORMAL)) {
								//we convert prices into lognormal volatilities
								final double forward = surface.getEquityForwardCurve().getDiscountFactor(mats[t]);
								final double optionMaturity =mats[t];
								final double optionStrike = currentStrikes[i];
								final double payoffUnit = surface.getDiscountCurve().getDiscountFactor(mats[t]);
								final double optionValue = currentModelPrices.get("valuePerStrike").apply(optionStrike);
								vals.add(net.finmath.functions.AnalyticFormulas.blackScholesOptionImpliedVolatility(forward, optionMaturity, optionStrike, payoffUnit, optionValue));


							}else if(targetConvention.equals(QuotingConvention.VOLATILITYNORMAL)) {
								//we convert prices into normal volatilities
								final double forward = surface.getEquityForwardCurve().getDiscountFactor(mats[t]);
								final double optionMaturity =mats[t];
								final double optionStrike = currentStrikes[i];
								final double payoffUnit = surface.getDiscountCurve().getDiscountFactor(mats[t]);
								final double optionValue = currentModelPrices.get("valuePerStrike").apply(optionStrike);
								vals.add(net.finmath.functions.AnalyticFormulas.bachelierOptionImpliedVolatility(forward, optionMaturity, optionStrike, payoffUnit, optionValue));
							}else {
								//just output the prices
								vals.add(currentModelPrices.get("valuePerStrike").apply(currentStrikes[i]));
							}

						}
					} catch (final CalculationException e) {
						e.printStackTrace();
					}

				}
				for(int i = 0; i<values.length; i++) {values[i] = vals.get(i);}
			}
		};

		final Optimizer optimizer = optimizerFactory.getOptimizer(
				objectiveFunction,
				initialParameters,
				lowerBound,
				upperBound,
				parameterStep,
				formatTargetValuesForOptimizer() /* targetValues */);


		optimizer.run();

		final ArrayList<String> calibrationOutput = outputCalibrationResult(optimizer.getBestFitParameters());

		final CalibratableProcess calibratedModel = model.getCloneForModifiedParameters(optimizer.getBestFitParameters());

		return new OptimizationResult(calibratedModel, optimizer.getBestFitParameters(), optimizer.getIterations(), optimizer.getRootMeanSquaredError(), calibrationOutput);
	}

	/**
	 * This is a service method that takes care of putting al the target values in a single array.
	 * @return
	 */
	private double[] formatTargetValuesForOptimizer() {
		//Put all values in an array for the optimizer.
		final int numberOfMaturities = surface.getMaturities().length;
		final double[] mats = surface.getMaturities();

		final ArrayList<Double> vals = new ArrayList<>();

		for(int t = 0; t<numberOfMaturities; t++) {
			final double mat = mats[t];
			final double[] myStrikes = surface.getSurface().get(mat).getStrikes();

			final OptionSmileData smileOfInterest = surface.getSurface().get(mat);

			for(int k = 0; k < myStrikes.length; k++) {
				vals.add(smileOfInterest.getSmile().get(myStrikes[k]).getValue());
			}

		}
		final Double[] targetVals = new Double[vals.size()];
		return ArrayUtils.toPrimitive(vals.toArray(targetVals));
	}

	/**
	 * When the calibration is done this method is called to produce a table
	 *
	 * @param parameters Calibration parameters.
	 */
	private ArrayList<String> outputCalibrationResult(final double[] parameters) {

		final ArrayList<String> calibrationOutput = new ArrayList<>();

		//We change the parameters of the model
		final CalibratableProcess newModel = model.getCloneForModifiedParameters(parameters);
		final CharacteristicFunctionModel newModelFourier = newModel.getCharacteristicFunctionModel();

		final int numberOfMaturities = surface.getMaturities().length;
		final double[] mats = surface.getMaturities();

		final QuotingConvention targetConvention = surface.getQuotingConvention();

		double value;
		double targetValue;
		double T;
		double K;

		calibrationOutput.add("Strike"+ "\t" + "Maturity"+ "\t" + "Market Value" + "\t" + "Model Value" + "\t" + "Squared Error");

		for(int t = 0; t<numberOfMaturities; t++) {
			T = mats[t];
			final OptionSmileData currentSmile = surface.getSmile(mats[t]);
			final double[] currentStrikes = currentSmile.getStrikes();

			final EuropeanOptionSmile newPricer = pricer.getCloneWithModifiedParameters(mats[t],currentStrikes);

			try {
				final Map<String, Function<Double, Double>> currentModelPrices = newPricer.getValue(0.0, newModelFourier);

				for(int i = 0; i<currentStrikes.length;i++) {
					K = currentStrikes[i];
					targetValue = currentSmile.getOption(currentStrikes[i]).getValue();

					if(targetConvention.equals(QuotingConvention.VOLATILITYLOGNORMAL)) {
						//we convert prices into lognormal volatilities
						final double forward = surface.getEquityForwardCurve().getDiscountFactor(mats[t]);
						final double optionMaturity =mats[t];
						final double optionStrike = currentStrikes[i];
						final double payoffUnit = surface.getDiscountCurve().getDiscountFactor(mats[t]);
						final double optionValue = currentModelPrices.get("valuePerStrike").apply(optionStrike);//currentModelPrices.get(currentStrikes[i]);
						value = net.finmath.functions.AnalyticFormulas.blackScholesOptionImpliedVolatility(forward, optionMaturity, optionStrike, payoffUnit, optionValue);


					}else if(targetConvention.equals(QuotingConvention.VOLATILITYNORMAL)) {
						//we convert prices into normal volatilities
						final double forward = surface.getEquityForwardCurve().getDiscountFactor(mats[t]);
						final double optionMaturity =mats[t];
						final double optionStrike = currentStrikes[i];
						final double payoffUnit = surface.getDiscountCurve().getDiscountFactor(mats[t]);
						final double optionValue = currentModelPrices.get("valuePerStrike").apply(optionStrike);//currentModelPrices.get(currentStrikes[i]);
						value = net.finmath.functions.AnalyticFormulas.bachelierOptionImpliedVolatility(forward, optionMaturity, optionStrike, payoffUnit, optionValue);
					}else {
						//just output the prices
						value = currentModelPrices.get("valuePerStrike").apply(currentStrikes[i]);//currentModelPrices.get(currentStrikes[i]);
					}
					calibrationOutput.add(K+ "\t" + T + "\t" + targetValue + "\t" + value+ "\t" + Math.pow(targetValue-value,2));
				}
			} catch (final CalculationException e) {
				e.printStackTrace();
			}


		}
		return calibrationOutput;
	}


	/**
	 * Helper class for calibration results.
	 *
	 * @author Alessandro Gnoatto
	 */
	public static class OptimizationResult{
		private final CalibratableProcess model; //the calibrated model
		private final double[] bestFitParameters;
		private final int iterations;
		private final double rootMeanSquaredError;
		private final ArrayList<String> calibrationOutput;

		public OptimizationResult(final CalibratableProcess model, final double[] bestFitParameters,
				final int iterations, final double rootMeanSquaredError, final ArrayList<String> calibrationOutput) {
			super();
			this.model = model;
			this.bestFitParameters = bestFitParameters;
			this.iterations = iterations;
			this.rootMeanSquaredError = rootMeanSquaredError;
			this.calibrationOutput = calibrationOutput;
		}

		public CalibratableProcess getModel() {
			return model;
		}

		public double[] getBestFitParameters() {
			return bestFitParameters;
		}

		public int getIterations() {
			return iterations;
		}

		public double getRootMeanSquaredError() {
			return rootMeanSquaredError;
		}

		public ArrayList<String> getCalibrationOutput(){
			return calibrationOutput;
		}

	}

}
