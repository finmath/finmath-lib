package net.finmath.fouriermethod.calibration;

import java.time.LocalDate;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.exception.CalculationException;
import net.finmath.fouriermethod.calibration.CalibratedModel.OptimizationResult;
import net.finmath.fouriermethod.calibration.models.CalibratableHestonModel;
import net.finmath.fouriermethod.products.smile.EuropeanOptionSmileByCarrMadan;
import net.finmath.marketdata.model.curves.CurveInterpolation.ExtrapolationMethod;
import net.finmath.marketdata.model.curves.CurveInterpolation.InterpolationEntity;
import net.finmath.marketdata.model.curves.CurveInterpolation.InterpolationMethod;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.DiscountCurveInterpolation;
import net.finmath.marketdata.model.volatilities.OptionSmileData;
import net.finmath.marketdata.model.volatilities.OptionSurfaceData;
import net.finmath.marketdata.model.volatilities.VolatilitySurface.QuotingConvention;
import net.finmath.modelling.descriptor.HestonModelDescriptor;
import net.finmath.optimizer.OptimizerFactory;
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
	private static DiscountCurve getDiscountCurve(final String name, final LocalDate referenceDate, final double riskFreeRate) {
		final double[] times = new double[] { 1.0 };
		final double[] givenAnnualizedZeroRates = new double[] { riskFreeRate };
		final InterpolationMethod interpolationMethod = InterpolationMethod.LINEAR;
		final InterpolationEntity interpolationEntity = InterpolationEntity.LOG_OF_VALUE_PER_TIME;
		final ExtrapolationMethod extrapolationMethod = ExtrapolationMethod.CONSTANT;
		final DiscountCurve discountCurve = DiscountCurveInterpolation.createDiscountCurveFromAnnualizedZeroRates(name, referenceDate, times, givenAnnualizedZeroRates, interpolationMethod, extrapolationMethod, interpolationEntity);
		return discountCurve;
	}

	@Test
	public void test() throws CalculationException, SolverException {

		final LocalDate referenceDate = LocalDate.of(2010, 8, 1);

		final double[] strike1 = {5500,
				5600,
				5700,
				5800,
				5900,
				6000,
				6100,
				6200,
				6300,
				6400};


		final double[] firstSmile = {0.277475758170766,
				0.269515340374296,
				0.261571882863362,
				0.253276716037966,
				0.246026095740043,
				0.238654656319249,
				0.23116183944298,
				0.223598439942633,
				0.216448858389314,
				0.209586497365038};

		final double[] secondSmile = {0.278915422959662,
				0.271901489924065,
				0.265156150952273,
				0.258805052234575,
				0.252251857517556,
				0.246778487031158,
				0.240218126189617,
				0.23407389543181,
				0.228093463234625,
				0.222039563314172};

		final QuotingConvention convention = QuotingConvention.VOLATILITYLOGNORMAL;

		final double riskFreeRate = 0.05;

		final ExtrapolationMethod exMethod = ExtrapolationMethod.CONSTANT;
		final InterpolationMethod intMethod = InterpolationMethod.LINEAR;
		final InterpolationEntity intEntity = InterpolationEntity.LOG_OF_VALUE;

		final DiscountCurve discountCurve = getDiscountCurve("discountCurve", referenceDate, riskFreeRate);

		final double initialValue = 6149.62;

		final DiscountCurve equityForwardCurve = DiscountCurveInterpolation.createDiscountCurveFromDiscountFactors(
				"daxForwardCurve"								/* name */,
				new double[] {0.0,  1.0,  2.0,  4.0,  5.0}	/* maturities */,
				new double[] {initialValue, initialValue*Math.exp(riskFreeRate*1.0), initialValue*Math.exp(riskFreeRate*2.0), initialValue*Math.exp(riskFreeRate*3.0), initialValue*Math.exp(riskFreeRate*4.0)}	/* discount factors */,
				intMethod, exMethod,intEntity);


		final double maturity = 105.0/252;
		final double secondMaturity = 170.0/252;

		final OptionSmileData smileContainer = new OptionSmileData("DAX", referenceDate, strike1, maturity, firstSmile, convention);
		final OptionSmileData secondSmileContainer = new OptionSmileData("DAX", referenceDate, strike1, secondMaturity, secondSmile, convention);

		final OptionSmileData[] mySmiles = {smileContainer,secondSmileContainer};

		final OptionSurfaceData surface = new OptionSurfaceData(mySmiles, discountCurve, equityForwardCurve);

		/*
		 * The parameters we specify here do not have an impact on the starting point of the calibration.
		 * The true initial condition is fixed by optimizer factory.
		 *
		 */
		final double volatility = 0.0423;
		final double theta = 0.0818;
		final double kappa = 0.8455;
		final double xi = 0.4639;
		final double rho = -0.4;

		final HestonModelDescriptor hestonModelDescriptor = new HestonModelDescriptor(referenceDate, initialValue, getDiscountCurve("forward curve", referenceDate, riskFreeRate), getDiscountCurve("discount curve", referenceDate, riskFreeRate), volatility, theta, kappa, xi, rho);

		final ScalarParameterInformationImplementation volatilityInformation = new ScalarParameterInformationImplementation(true, new BoundConstraint(0.01,1.0));
		final ScalarParameterInformationImplementation thetaInformation = new ScalarParameterInformationImplementation(true, new BoundConstraint(0.01,0.2));
		final ScalarParameterInformationImplementation kappaInformation = new ScalarParameterInformationImplementation(true, new BoundConstraint(0.01,1.0));
		final ScalarParameterInformationImplementation xiInformation = new ScalarParameterInformationImplementation(true, new BoundConstraint(0.01,1.0));
		final ScalarParameterInformationImplementation rhoInformation = new ScalarParameterInformationImplementation(true, new BoundConstraint(-1.0,1.0));

		final CalibratableHestonModel model = new CalibratableHestonModel(hestonModelDescriptor,volatilityInformation,thetaInformation,kappaInformation,xiInformation,rhoInformation,false);

		final OptimizerFactory optimizerFactory = new OptimizerFactoryLevenbergMarquardt(300 /* maxIterations */, 2 /* maxThreads */);

		final double[] initialParameters = new double[] { 0.0423,0.0818,0.8455,0.4639,-0.7} /* initialParameters */;
		final double[] parameterStep = new double[] { 0.01,0.01,0.01,0.01,0.01} /* parameterStep */;

		/*
		 * Maturity and strikes are here immaterial and only meant to generate the first instance of the class.
		 */
		final EuropeanOptionSmileByCarrMadan pricer = new EuropeanOptionSmileByCarrMadan(maturity, strike1);

		final CalibratedModel problem = new CalibratedModel(surface, model, optimizerFactory, pricer,initialParameters,parameterStep);

		System.out.println("Calibration started");

		final long startMillis	= System.currentTimeMillis();
		final OptimizationResult result = problem.getCalibration();
		final long endMillis		= System.currentTimeMillis();

		final double calculationTime = ((endMillis-startMillis)/1000.0);

		System.out.println("Calibration completed in: " +calculationTime + " seconds");

		System.out.println("The solver required " + result.getIterations() + " iterations.");
		System.out.println("RMSQE " +result.getRootMeanSquaredError());

		final HestonModelDescriptor hestonDescriptor = (HestonModelDescriptor) result.getModel().getModelDescriptor();

		System.out.println(hestonDescriptor.getVolatility());
		System.out.println(hestonDescriptor.getTheta());
		System.out.println(hestonDescriptor.getKappa());
		System.out.println(hestonDescriptor.getXi());
		System.out.println(hestonDescriptor.getRho());

		final ArrayList<String> errorsOverview = result.getCalibrationOutput();

		for(final String myString : errorsOverview) {
			System.out.println(myString);
		}

		Assert.assertTrue(result.getRootMeanSquaredError() < 1.0);

	}

}
