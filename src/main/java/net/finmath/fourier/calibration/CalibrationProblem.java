package net.finmath.fourier.calibration;

import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;

import net.finmath.exception.CalculationException;
import net.finmath.fourier.calibration.models.CalibrableProcessInterface;
import net.finmath.fouriermethod.models.ProcessCharacteristicFunctionInterface;
import net.finmath.fouriermethod.products.EuropeanOptionSmile;
import net.finmath.marketdata.model.volatilities.OptionSmileData;
import net.finmath.marketdata.model.volatilities.OptionSurfaceData;
import net.finmath.marketdata.model.volatilities.VolatilitySurfaceInterface.QuotingConvention;
import net.finmath.optimizer.OptimizerFactoryInterface;
import net.finmath.optimizer.OptimizerInterface;
import net.finmath.optimizer.SolverException;

/**
 * This class solves a calibration problem. The problem is parametrized in terms of:
 * 
 * 1) a generic container of market data OptionSurfaceData.
 * 2) a generic pricing model.
 * 3) a generic calibration algorithm.
 * 4) a generic pricer for claims.
 * 
 * The class supports both calibration in terms of:
 * 
 * - Prices
 * - Lognormal implied volatilities.
 * - Normal implied volatilities.
 * 
 * To change the calibration entity please change the convention in the option surface.
 * The calibration entity (i.e. price/vol/normal vol) is directly detected from market data.
 * 
 * @author Alessandro Gnoatto
 *
 */
public class CalibrationProblem {
	
	private final OptionSurfaceData surface; //target calibration instruments. They dictate the calibration entity: vol/price.
	private final CalibrableProcessInterface model; //Pricing model
	private final OptimizerFactoryInterface optimizerFactory; //construct the instance of the optimization algorithm inside the class.
	private final EuropeanOptionSmile pricer; //How do we compute prices: Carr Madan, Cos, Conv, Lewis...
	
	//Optimizer parameters
	private final double[] initialParameters;
	private final double[] lowerBound;
	private final double[] upperBound;
	private final double[] parameterStep;
	
	public CalibrationProblem(OptionSurfaceData surface, CalibrableProcessInterface model,
			OptimizerFactoryInterface optimizerFactory, EuropeanOptionSmile pricer, double[] initialParameters,
			double[] lowerBound, double[] upperBound, double[] parameterStep) {
		super();
		this.surface = surface;
		this.model = model;
		this.optimizerFactory = optimizerFactory;
		this.pricer = pricer;
		this.initialParameters = initialParameters;
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
		this.parameterStep = parameterStep;
	}

	public OptimizationResult runCalibration() throws SolverException {
		
		OptimizerInterface.ObjectiveFunction objectiveFunction = new OptimizerInterface.ObjectiveFunction() {
			@Override
			public void setValues(double[] parameters, double[] values) {
			
				//We change the parameters of the model
				ProcessCharacteristicFunctionInterface newModel = model.getCloneForModifiedParameters(parameters);
								
				int numberOfMaturities = surface.getMaturities().length;
				double mats[] = surface.getMaturities();
				
				QuotingConvention targetConvention = surface.getQuotingConvention();
				
				for(int t = 0; t<numberOfMaturities; t++) {
					
					double[] currentStrikes = surface.getSmile(mats[t]).getStrikes();
					
					EuropeanOptionSmile newPricer = pricer.getCloneWithModifiedParameters(mats[t],currentStrikes);
									
					try {
						Map<Double, Double> currentModelPrices = newPricer.getValue(newModel);
					
						for(int i = 0; i<currentStrikes.length;i++) {
															
							if(targetConvention.equals(QuotingConvention.VOLATILITYLOGNORMAL)) {
								//we convert prices into lognormal volatilities
								double forward = surface.getEquityForwardCurve().getDiscountFactor(mats[t]);
								double optionMaturity =mats[t];
								double optionStrike = currentStrikes[i];
								double payoffUnit = surface.getDiscountCurve().getDiscountFactor(mats[t]);
								double optionValue = currentModelPrices.get(currentStrikes[i]);
								values[i] = net.finmath.functions.AnalyticFormulas.blackScholesOptionImpliedVolatility(forward, optionMaturity, optionStrike, payoffUnit, optionValue);
								
							
							}else if(targetConvention.equals(QuotingConvention.VOLATILITYNORMAL)) {
								//we convert prices into normal volatilities
								double forward = surface.getEquityForwardCurve().getDiscountFactor(mats[t]);
								double optionMaturity =mats[t];
								double optionStrike = currentStrikes[i];
								double payoffUnit = surface.getDiscountCurve().getDiscountFactor(mats[t]);
								double optionValue = currentModelPrices.get(currentStrikes[i]);
								values[i] = net.finmath.functions.AnalyticFormulas.bachelierOptionImpliedVolatility(forward, optionMaturity, optionStrike, payoffUnit, optionValue);
							}else {
								//just output the prices
								values[i] = currentModelPrices.get(currentStrikes[i]);
							}						
							
						}
					} catch (CalculationException e) {
						e.printStackTrace();
					}		
					
				}
			}
		};
		
		OptimizerInterface optimizer = optimizerFactory.getOptimizer(
				objectiveFunction,
				initialParameters,
				lowerBound,
				upperBound,
				parameterStep,
				formatTargetValuesForOptimizer() /* targetValues */);
		
		
		optimizer.run();
		
		ArrayList<String> calibrationOutput = outputCalibrationResult(optimizer.getBestFitParameters()); 
		
		ProcessCharacteristicFunctionInterface calibratedModel = model.getCloneForModifiedParameters(optimizer.getBestFitParameters());
		
		return new OptimizationResult(calibratedModel,optimizer.getBestFitParameters(),optimizer.getIterations(),optimizer.getRootMeanSquaredError(),calibrationOutput);
	}
	
	/**
	 * This is a service method that takes care of putting al the target values in a single array.
	 * @return
	 */
	private double[] formatTargetValuesForOptimizer() {
		//Put all values in an array for the optimizer.
		int numberOfMaturities = surface.getMaturities().length;
		double mats[] = surface.getMaturities();
		
		ArrayList<Double> vals = new ArrayList<Double>();
		
		for(int t = 0; t<numberOfMaturities; t++) {
			double mat = mats[t];
			double[] myStrikes = surface.getSurface().get(mat).getStrikes();
			
			OptionSmileData smileOfInterest = surface.getSurface().get(mat);
			
			for(int k = 0; k < myStrikes.length; k++) {
				vals.add(smileOfInterest.getSmile().get(myStrikes[k]).getValue());
			}
					
		}
		Double[] targetVals = new Double[vals.size()];
		return ArrayUtils.toPrimitive(vals.toArray(targetVals));
	}
	
	/**
	 * When the calibration is over this method is called to produce a table
	 * @param parameters
	 */
	private ArrayList<String> outputCalibrationResult(double[] parameters) {
		
		ArrayList<String> calibrationOutput = new ArrayList<String>();
		
		//We change the parameters of the model
		ProcessCharacteristicFunctionInterface newModel = model.getCloneForModifiedParameters(parameters);
						
		int numberOfMaturities = surface.getMaturities().length;
		double mats[] = surface.getMaturities();
		
		QuotingConvention targetConvention = surface.getQuotingConvention();
		
		double value;
		double targetValue;
		double T;
		double K;
		
		calibrationOutput.add("Strike"+ "\t" + "Maturity"+ "\t" + "Market Value" + "\t" + "Model Value" + "\t" + "Squared Error");
		
		for(int t = 0; t<numberOfMaturities; t++) {
			T = mats[t];
			OptionSmileData currentSmile = surface.getSmile(mats[t]);
			double[] currentStrikes = currentSmile.getStrikes();
			
			EuropeanOptionSmile newPricer = pricer.getCloneWithModifiedParameters(mats[t],currentStrikes);
						
			try {
				Map<Double, Double> currentModelPrices = newPricer.getValue(newModel);
			
				for(int i = 0; i<currentStrikes.length;i++) {
					K = currentStrikes[i];
					targetValue = currentSmile.getOption(currentStrikes[i]).getValue();
													
					if(targetConvention.equals(QuotingConvention.VOLATILITYLOGNORMAL)) {
						//we convert prices into lognormal volatilities
						double forward = surface.getEquityForwardCurve().getDiscountFactor(mats[t]);
						double optionMaturity =mats[t];
						double optionStrike = currentStrikes[i];
						double payoffUnit = surface.getDiscountCurve().getDiscountFactor(mats[t]);
						double optionValue = currentModelPrices.get(currentStrikes[i]);
						value = net.finmath.functions.AnalyticFormulas.blackScholesOptionImpliedVolatility(forward, optionMaturity, optionStrike, payoffUnit, optionValue);
						
					
					}else if(targetConvention.equals(QuotingConvention.VOLATILITYNORMAL)) {
						//we convert prices into normal volatilities
						double forward = surface.getEquityForwardCurve().getDiscountFactor(mats[t]);
						double optionMaturity =mats[t];
						double optionStrike = currentStrikes[i];
						double payoffUnit = surface.getDiscountCurve().getDiscountFactor(mats[t]);
						double optionValue = currentModelPrices.get(currentStrikes[i]);
						value = net.finmath.functions.AnalyticFormulas.bachelierOptionImpliedVolatility(forward, optionMaturity, optionStrike, payoffUnit, optionValue);
					}else {
						//just output the prices
						value = currentModelPrices.get(currentStrikes[i]);
					}						
					calibrationOutput.add(K+ "\t" + T + "\t" + targetValue + "\t" + value+ "\t" + Math.pow(targetValue-value,2));
				}
			} catch (CalculationException e) {
				e.printStackTrace();
			}
			
			
		}
		return calibrationOutput;
	}
	
		
	/**
	 * Helper class for calibration results.
	 * @author Alessandro Gnoatto
	 *
	 */
	public class OptimizationResult{
		private final ProcessCharacteristicFunctionInterface model; //the calibrated model
		private final double[] bestFitParameters;
		private final int iterations;
		private final double rootMeanSquaredError;
		private final ArrayList<String> calibrationOutput;
		
		public OptimizationResult(ProcessCharacteristicFunctionInterface model, double[] bestFitParameters,
				int iterations, double rootMeanSquaredError, ArrayList<String> calibrationOutput) {
			super();
			this.model = model;
			this.bestFitParameters = bestFitParameters;
			this.iterations = iterations;
			this.rootMeanSquaredError = rootMeanSquaredError;
			this.calibrationOutput = calibrationOutput;
		}

		public ProcessCharacteristicFunctionInterface getModel() {
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
			return this.calibrationOutput;
		}
		
	}

}
