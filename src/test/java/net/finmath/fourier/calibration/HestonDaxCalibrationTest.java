package net.finmath.fourier.calibration;

import java.time.LocalDate;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.exception.CalculationException;
import net.finmath.fouriermethod.calibration.CalibrationProblem;
import net.finmath.fouriermethod.calibration.CalibrationProblem.OptimizationResult;
import net.finmath.fouriermethod.calibration.models.CalibrableHestonModel;
import net.finmath.fouriermethod.products.EuropeanOptionSmileByCarrMadan;
import net.finmath.marketdata.model.curves.Curve.ExtrapolationMethod;
import net.finmath.marketdata.model.curves.Curve.InterpolationEntity;
import net.finmath.marketdata.model.curves.Curve.InterpolationMethod;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.DiscountCurveInterface;
import net.finmath.marketdata.model.volatilities.OptionSmileData;
import net.finmath.marketdata.model.volatilities.OptionSurfaceData;
import net.finmath.marketdata.model.volatilities.VolatilitySurfaceInterface.QuotingConvention;
import net.finmath.modelling.descriptor.HestonModelDescriptor;
import net.finmath.optimizer.OptimizerFactoryInterface;
import net.finmath.optimizer.OptimizerFactoryLevenbergMarquardt;
import net.finmath.optimizer.SolverException;

/**
 * Test program showing how to calibrate a Heston model.
 *
 * @author Alessandro Gnoatto
 *
 */
public class HestonDaxCalibrationTest {

	/**
	 * Get the discount curve using the riskFreeRate.
	 *
	 * @param name Name of the curve
	 * @param referenceDate Date corresponding to t=0.
	 * @param riskFreeRate Constant continuously compounded rate
	 *
	 * @return the discount curve using the riskFreeRate.
	 */
	private static DiscountCurveInterface getDiscountCurve(String name, LocalDate referenceDate, double riskFreeRate) {
		double[] times = new double[] { 1.0 };
		double[] givenAnnualizedZeroRates = new double[] { riskFreeRate };
		InterpolationMethod interpolationMethod = InterpolationMethod.LINEAR;
		InterpolationEntity interpolationEntity = InterpolationEntity.LOG_OF_VALUE_PER_TIME;
		ExtrapolationMethod extrapolationMethod = ExtrapolationMethod.CONSTANT;
		DiscountCurveInterface discountCurve = DiscountCurve.createDiscountCurveFromAnnualizedZeroRates(name, referenceDate, times, givenAnnualizedZeroRates, interpolationMethod, extrapolationMethod, interpolationEntity);
		return discountCurve;
	}

	@Test
	public void test() throws CalculationException, SolverException {

		LocalDate referenceDate = LocalDate.of(2010, 8, 1);

		double[] strike1 = {5500,
				5600,
				5700,
				5800,
				5900,
				6000,
				6100,
				6200,
				6300,
				6400};


		double[] firstSmile = {0.277475758170766,
				0.269515340374296,
				0.261571882863362,
				0.253276716037966,
				0.246026095740043,
				0.238654656319249,
				0.23116183944298,
				0.223598439942633,
				0.216448858389314,
				0.209586497365038};

		double[] secondSmile = {0.278915422959662,
				0.271901489924065,
				0.265156150952273,
				0.258805052234575,
				0.252251857517556,
				0.246778487031158,
				0.240218126189617,
				0.23407389543181,
				0.228093463234625,
				0.222039563314172};

		QuotingConvention convention = QuotingConvention.VOLATILITYLOGNORMAL;

		double riskFreeRate = 0.05;

		ExtrapolationMethod exMethod = ExtrapolationMethod.CONSTANT;
		InterpolationMethod intMethod = InterpolationMethod.LINEAR;
		InterpolationEntity intEntity = InterpolationEntity.LOG_OF_VALUE;

		DiscountCurveInterface discountCurve = getDiscountCurve("discountCurve", referenceDate, riskFreeRate);

		double initialValue = 6149.62;

		DiscountCurveInterface equityForwardCurve = DiscountCurve.createDiscountCurveFromDiscountFactors(
				"daxForwardCurve"								/* name */,
				new double[] {0.0,  1.0,  2.0,  4.0,  5.0}	/* maturities */,
				new double[] {initialValue, initialValue*Math.exp(riskFreeRate*1.0), initialValue*Math.exp(riskFreeRate*2.0), initialValue*Math.exp(riskFreeRate*3.0), initialValue*Math.exp(riskFreeRate*4.0)}	/* discount factors */,
				intMethod, exMethod,intEntity);


		double maturity = 105.0/252;
		double secondMaturity = 170.0/252;

		OptionSmileData smileContainer = new OptionSmileData("DAX", referenceDate, strike1, maturity, firstSmile, convention);
		OptionSmileData secondSmileContainer = new OptionSmileData("DAX", referenceDate, strike1, secondMaturity, secondSmile, convention);

		OptionSmileData[] mySmiles = {smileContainer,secondSmileContainer};

		OptionSurfaceData surface = new OptionSurfaceData(mySmiles, discountCurve, equityForwardCurve);

		/*
		 * The parameters we specify here do not have an impact on the starting point of the calibration.
		 * The true initial condition is fixed by optimizer factory.
		 *
		 */
		double volatility = 0.0423;
		double theta = 0.0818;
		double kappa = 0.8455;
		double xi = 0.4639;
		double rho = -0.4;

		HestonModelDescriptor hestonModelDescriptor = new HestonModelDescriptor(referenceDate, initialValue, getDiscountCurve("forward curve", referenceDate, riskFreeRate), getDiscountCurve("discount curve", referenceDate, riskFreeRate), volatility, theta, kappa, xi, rho);

		CalibrableHestonModel model = new CalibrableHestonModel(hestonModelDescriptor);

		OptimizerFactoryInterface optimizerFactory = new OptimizerFactoryLevenbergMarquardt(300 /* maxIterations */, 2 /* maxThreads */);

		double[] initialParameters = new double[] { 0.0423,0.0818,0.8455,0.4639,-0.7} /* initialParameters */;
		double[] lowerBound = new double[] { 0.01,0.01,0.01,0.01,-1.0} /* lowerBound */;
		double[] upperBound = new double[] { 1.0,1.0,1.0,1.0,1.0} /* upperBound */;
		double[] parameterStep = new double[] { 0.01,0.01,0.01,0.01,0.01} /* parameterStep */;

		/*
		 * Maturity and strikes are here immaterial and only meant to generate the first instance of the class.
		 */
		EuropeanOptionSmileByCarrMadan pricer = new EuropeanOptionSmileByCarrMadan(maturity, strike1);

		CalibrationProblem problem = new CalibrationProblem(surface, model, optimizerFactory, pricer,initialParameters,
				lowerBound, upperBound, parameterStep);

		System.out.println("Calibration started");

		long startMillis	= System.currentTimeMillis();
		OptimizationResult result = problem.runCalibration();
		long endMillis		= System.currentTimeMillis();

		double calculationTime = ((endMillis-startMillis)/1000.0);

		System.out.println("Calibration completed in: " +calculationTime + " seconds");

		System.out.println("The solver required " + result.getIterations() + " iterations.");
		System.out.println("RMSQE " +result.getRootMeanSquaredError());

		double[] parameters = result.getBestFitParameters();
		for(int i =0; i<parameters.length; i++) {
			System.out.println(parameters[i]);
		}

		ArrayList<String> errorsOverview = result.getCalibrationOutput();

		for(String myString : errorsOverview)
			System.out.println(myString);

		Assert.assertEquals(result.getRootMeanSquaredError() < 1.0, true);

	}

}
