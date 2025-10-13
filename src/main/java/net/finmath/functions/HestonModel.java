package net.finmath.functions;

import java.util.function.Function;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import net.finmath.interpolation.RationalFunctionInterpolation;
import net.finmath.interpolation.RationalFunctionInterpolation.ExtrapolationMethod;
import net.finmath.interpolation.RationalFunctionInterpolation.InterpolationMethod;

/**
 * This class implements some functions as static class methods related to the Heston model.
 * The calculation is performed by means of the FFT algorithm of Carr Madan applied to the gradient of the characteristic funtion.
 * 
 * The model is
 * \[
 * 	dS(t) = r^{\text{c}}(t) S(t) dt + \sqrt{V(t)} S(t) dW_{1}(t), \quad S(0) = S_{0},
 * \]
 * \[
 * 	dV(t) = \kappa ( \theta - V(t) ) dt + \xi \sqrt{V(t)} dW_{2}(t), \quad V(0) = \sigma^2,
 * \]
 * \[
 * 	dW_{1} dW_{2} = \rho dt
 * \]
 * \[
 * 	dN(t) = r^{\text{d}}(t) N(t) dt, \quad N(0) = N_{0},
 * \]
 * where \( W \) is a Brownian motion.
 *
 * The free parameters of this model are:
 * <dl>
 * 	<dt>\( S_{0} \)</dt> <dd>spot - initial value of S</dd>
 * 	<dt>\( r^{\text{c}} \)</dt> <dd>the risk free rate</dd>
 * 	<dt>\( \sigma \)</dt> <dd>the initial volatility level</dd>
 * 	<dt>\( r^{\text{d}} \)</dt> <dd>the discount rate</dd>
 * 	<dt>\( \xi \)</dt> <dd>the volatility of volatility</dd>
 * 	<dt>\( \theta \)</dt> <dd>the mean reversion level of the stochastic volatility</dd>
 * 	<dt>\( \kappa \)</dt> <dd>the mean reversion speed of the stochastic volatility</dd>
 * 	<dt>\( \rho \)</dt> <dd>the correlation of the Brownian drivers</dd>
 * </dl>
 *
 * @author Alessandro Gnoatto
 */
public class HestonModel {

	/*
	 * Parameters for the FFT calculator
	 */
	final static InterpolationMethod intMethod =InterpolationMethod.LINEAR;
	final static ExtrapolationMethod extMethod = ExtrapolationMethod.CONSTANT;
	final static int numberOfPoints = 4096*2;
	final static double gridSpacing = 0.4;

	enum HestonGreek {DELTA, GAMMA, THETA, RHO, VEGA1, VANNA, VOLGA};

	/**
	 * Calculates the delta of a call option under a Heston model.
	 * 
	 * @param initialStockValue Initital value of the stock.
	 * @param riskFreeRate The risk free rate.
	 * @param dividendYield The dividend yield.
	 * @param sigma the square root of the initial instantaneous variance (\( V_0 = sigma^2 \))
	 * @param theta the long run mean of the volatility.
	 * @param kappa the speed of mean reversion.
	 * @param xi the volatility of variance.
	 * @param rho correlation between the two Brownian motions
	 * @param optionMaturity the maturity of the option
	 * @param optionStrike the strike of the option.
	 * @return The delta of the option.
	 */
	public static double hestonOptionDelta(
			final double initialStockValue,
			final double riskFreeRate,
			final double dividendYield,
			final double sigma, 
			final double theta, 
			final double kappa, 
			final double xi, 
			final double rho,
			final double optionMaturity,
			final double optionStrike)
	{

		HestonGreek whatToCompute = HestonGreek.DELTA;

		return hestonGreekCalculator(initialStockValue,
				riskFreeRate,
				dividendYield,
				sigma, 
				theta, 
				kappa, 
				xi, 
				rho,
				optionMaturity,
				optionStrike,
				whatToCompute,
				numberOfPoints,
				gridSpacing);
	}

	/**
	 * Calculates the gamma of a call option under a Heston model
	 * 
	 * @param initialStockValue Initital value of the stock.
	 * @param riskFreeRate The risk free rate.
	 * @param dividendYield The dividend yield.
	 * @param sigma the square root of the initial instantaneous variance (\( V_0 = sigma^2 \))
	 * @param theta the long run mean of the volatility.
	 * @param kappa the speed of mean reversion.
	 * @param xi the volatility of variance.
	 * @param rho correlation between the two Brownian motions
	 * @param optionMaturity the maturity of the option
	 * @param optionStrike the strike of the option.
	 * @return The gamma of the option
	 */
	public static double hestonOptionGamma(
			final double initialStockValue,
			final double riskFreeRate,
			final double dividendYield,
			final double sigma, 
			final double theta, 
			final double kappa, 
			final double xi, 
			final double rho,
			final double optionMaturity,
			final double optionStrike)
	{
		HestonGreek whatToCompute = HestonGreek.GAMMA;

		return hestonGreekCalculator(initialStockValue,
				riskFreeRate,
				dividendYield,
				sigma, 
				theta, 
				kappa, 
				xi, 
				rho,
				optionMaturity,
				optionStrike,
				whatToCompute,
				numberOfPoints,
				gridSpacing);
	}

	/**
	 * Calculates the theta of a call option under a Heston model
	 * 
	 * @param initialStockValue Initital value of the stock.
	 * @param riskFreeRate The risk free rate.
	 * @param dividendYield The dividend yield.
	 * @param sigma the square root of the initial instantaneous variance (\( V_0 = sigma^2 \))
	 * @param theta the long run mean of the volatility.
	 * @param kappa the speed of mean reversion.
	 * @param xi the volatility of variance.
	 * @param rho correlation between the two Brownian motions
	 * @param optionMaturity the maturity of the option
	 * @param optionStrike the strike of the option.
	 * @return The theta of the option
	 */
	public static double hestonOptionTheta(
			final double initialStockValue,
			final double riskFreeRate,
			final double dividendYield,
			final double sigma, 
			final double theta, 
			final double kappa, 
			final double xi, 
			final double rho,
			final double optionMaturity,
			final double optionStrike)
	{
		HestonGreek whatToCompute = HestonGreek.THETA;

		return hestonGreekCalculator(initialStockValue,
				riskFreeRate,
				dividendYield,
				sigma, 
				theta, 
				kappa, 
				xi, 
				rho,
				optionMaturity,
				optionStrike,
				whatToCompute,
				numberOfPoints,
				gridSpacing);
	}

	/**
	 * Calculates the rho of a call option under a Heston model
	 * 
	 * @param initialStockValue Initital value of the stock.
	 * @param riskFreeRate The risk free rate.
	 * @param dividendYield The dividend yield.
	 * @param sigma the square root of the initial instantaneous variance (\( V_0 = sigma^2 \))
	 * @param theta the long run mean of the volatility.
	 * @param kappa the speed of mean reversion.
	 * @param xi the volatility of variance.
	 * @param rho correlation between the two Brownian motions
	 * @param optionMaturity the maturity of the option
	 * @param optionStrike the strike of the option.
	 * @return The rho of the option
	 */
	public static double hestonOptionRho(
			final double initialStockValue,
			final double riskFreeRate,
			final double dividendYield,
			final double sigma, 
			final double theta, 
			final double kappa, 
			final double xi, 
			final double rho,
			final double optionMaturity,
			final double optionStrike)
	{
		HestonGreek whatToCompute = HestonGreek.RHO;

		return hestonGreekCalculator(initialStockValue,
				riskFreeRate,
				dividendYield,
				sigma, 
				theta, 
				kappa, 
				xi, 
				rho,
				optionMaturity,
				optionStrike,
				whatToCompute,
				numberOfPoints,
				gridSpacing);
	}

	/**
	 * Calculates the vega1 of a call option under a Heston model
	 * 
	 * @param initialStockValue Initital value of the stock.
	 * @param riskFreeRate The risk free rate.
	 * @param dividendYield The dividend yield.
	 * @param sigma the square root of the initial instantaneous variance (\( V_0 = sigma^2 \))
	 * @param theta the long run mean of the volatility.
	 * @param kappa the speed of mean reversion.
	 * @param xi the volatility of variance.
	 * @param rho correlation between the two Brownian motions
	 * @param optionMaturity the maturity of the option
	 * @param optionStrike the strike of the option.
	 * @return The vega1 of the option
	 */
	public static double hestonOptionVega1(
			final double initialStockValue,
			final double riskFreeRate,
			final double dividendYield,
			final double sigma, 
			final double theta, 
			final double kappa, 
			final double xi, 
			final double rho,
			final double optionMaturity,
			final double optionStrike)
	{
		HestonGreek whatToCompute = HestonGreek.VEGA1;

		return hestonGreekCalculator(initialStockValue,
				riskFreeRate,
				dividendYield,
				sigma, 
				theta, 
				kappa, 
				xi, 
				rho,
				optionMaturity,
				optionStrike,
				whatToCompute,
				numberOfPoints,
				gridSpacing);
	}

	/**
	 * Calculates the vanna of a call option under a Heston model
	 * 
	 * @param initialStockValue Initital value of the stock.
	 * @param riskFreeRate The risk free rate.
	 * @param dividendYield The dividend yield.
	 * @param sigma the square root of the initial instantaneous variance (\( V_0 = sigma^2 \))
	 * @param theta the long run mean of the volatility.
	 * @param kappa the speed of mean reversion.
	 * @param xi the volatility of variance.
	 * @param rho correlation between the two Brownian motions
	 * @param optionMaturity the maturity of the option
	 * @param optionStrike the strike of the option.
	 * @return The vanna of the option
	 */
	public static double hestonOptionVanna(
			final double initialStockValue,
			final double riskFreeRate,
			final double dividendYield,
			final double sigma, 
			final double theta, 
			final double kappa, 
			final double xi, 
			final double rho,
			final double optionMaturity,
			final double optionStrike)
	{
		HestonGreek whatToCompute = HestonGreek.VANNA;

		return hestonGreekCalculator(initialStockValue,
				riskFreeRate,
				dividendYield,
				sigma, 
				theta, 
				kappa, 
				xi, 
				rho,
				optionMaturity,
				optionStrike,
				whatToCompute,
				numberOfPoints,
				gridSpacing);
	}

	/**
	 * Calculates the volga of a call option under a Heston model
	 * 
	 * @param initialStockValue Initital value of the stock.
	 * @param riskFreeRate The risk free rate.
	 * @param dividendYield The dividend yield.
	 * @param sigma the square root of the initial instantaneous variance (\( V_0 = sigma^2 \))
	 * @param theta the long run mean of the volatility.
	 * @param kappa the speed of mean reversion.
	 * @param xi the volatility of variance.
	 * @param rho correlation between the two Brownian motions
	 * @param optionMaturity the maturity of the option
	 * @param optionStrike the strike of the option.
	 * @return The volga of the option
	 */
	public static double hestonOptionVolga(
			final double initialStockValue,
			final double riskFreeRate,
			final double dividendYield,
			final double sigma, 
			final double theta, 
			final double kappa, 
			final double xi, 
			final double rho,
			final double optionMaturity,
			final double optionStrike)
	{
		HestonGreek whatToCompute = HestonGreek.VOLGA;

		return hestonGreekCalculator(initialStockValue,
				riskFreeRate,
				dividendYield,
				sigma, 
				theta, 
				kappa, 
				xi, 
				rho,
				optionMaturity,
				optionStrike,
				whatToCompute,
				numberOfPoints,
				gridSpacing);
	}


	/**
	 * Computes the gradient of the (discounted) characteristic function of the Heston model.
	 * More sensitivies can be added by introducing more cases in the switch statement.
	 * 
	 * @param zeta The argument of the characteristic function gradient
	 * @param initialStockValue Initital value of the stock.
	 * @param riskFreeRate The risk free rate.
	 * @param dividendYield The dividend yield.
	 * @param sigma the square root of the initial instantaneous variance (\( V_0 = sigma^2 \))
	 * @param theta the long run mean of the volatility.
	 * @param kappa the speed of mean reversion.
	 * @param xi the volatility of variance.
	 * @param rho correlation between the two Brownian motions
	 * @param optionMaturity the maturity of the option
	 * @param optionStrike the strike of the option.
	 * @param whichGreek
	 * @param numberOfPoints
	 * @param gridSpacing
	 * @return
	 */
	private static Complex hestonCharacteristicFunctionGradient(
			final Complex zeta,
			final double initialStockValue,
			final double riskFreeRate,
			final double dividendYield,
			final double sigma, 
			final double theta, 
			final double kappa, 
			final double xi, 
			final double rho,
			final double optionMaturity,
			final double optionStrike,
			final HestonGreek whichGreek,
			final int numberOfPoints,
			final double gridSpacing) {

		final double lambda = 2*Math.PI/(numberOfPoints*gridSpacing);
		final double v0 = sigma*sigma;

		double x = Math.log(initialStockValue);
		double a = kappa * theta;

		Complex b, u, d, g, c, D, G, C;

		// Initialize
		u = new Complex(-0.5, 0.0);
		b = new Complex(kappa + lambda, 0.0);

		// Compute d
		Complex term1 = (Complex.I.multiply(rho * xi).multiply(zeta)).subtract(b); // (ρσiφ - b)
		Complex powPart = term1.pow(2.0); // (...)
		Complex term2 = new Complex(xi * xi, 0.0)
				.multiply((u.multiply(2.0).multiply(Complex.I).multiply(zeta))
						.subtract(zeta.multiply(zeta))); // σ²(2u i φ - φ²)
		d = powPart.subtract(term2).sqrt();

		// Compute g
		Complex numerator = (b.subtract(Complex.I.multiply(rho * xi).multiply(zeta))).add(d);
		Complex denominator = (b.subtract(Complex.I.multiply(rho * xi).multiply(zeta))).subtract(d);
		g = numerator.divide(denominator);

		// Initialize remaining
		c = Complex.ZERO;
		D = Complex.ZERO;
		G = Complex.ZERO;
		C = Complex.ZERO;

		// "Little Heston Trap" formulation
		// c = 1.0 / g
		c = Complex.ONE.divide(g);

		// Precompute some recurring parts
		Complex exp_dT = d.multiply(-optionMaturity).exp(); // exp(-d*T)
		Complex bMinusRhoSigmaIphiMinusD = b.subtract(Complex.I.multiply(rho * xi).multiply(zeta)).subtract(d);

		// D = (b - rho*sigma*i*phi - d) / (sigma^2) * ((1 - exp(-d*T)) / (1 - c * exp(-d*T)))
		D = bMinusRhoSigmaIphiMinusD
				.divide(xi * xi)
				.multiply(
						(Complex.ONE.subtract(exp_dT))
						.divide(Complex.ONE.subtract(c.multiply(exp_dT)))
						);

		// G = (1 - c * exp(-d*T)) / (1 - c)
		G = (Complex.ONE.subtract(c.multiply(exp_dT)))
				.divide(Complex.ONE.subtract(c));

		// C = (r - q) * i * phi * T + a / sigma^2 * ((b - rho*sigma*i*phi - d) * T - 2.0 * Complex.Log(G))
		Complex firstTerm = Complex.I.multiply(zeta).multiply((riskFreeRate - dividendYield) * optionMaturity);
		Complex secondTerm = new Complex(a / (xi * xi), 0.0)
				.multiply(
						(bMinusRhoSigmaIphiMinusD.multiply(optionMaturity))
						.subtract(Complex.valueOf(2.0).multiply(G.log()))
						);

		C = firstTerm.add(secondTerm);

		// The characteristic function
		Complex f = (C.add(D.multiply(v0)).add(Complex.I.multiply(zeta).multiply(x))).exp();


		// Return depending on the requested Greek
		switch (whichGreek) {
		case DELTA:
			return  f.multiply(Complex.I).multiply(zeta).divide(initialStockValue);
		case GAMMA:
			return Complex.I.multiply(zeta)
					.multiply(f)
					.multiply(Complex.I.multiply(zeta).subtract(Complex.ONE))
					.divide(initialStockValue * initialStockValue); 
		case THETA:
			Complex numerator_dDdT = d.multiply(exp_dT)
			.multiply(b.subtract(Complex.I.multiply(rho * xi).multiply(zeta)).add(d))
			.multiply(g.subtract(Complex.ONE));
			Complex denominator_dDdT = Complex.valueOf(xi * xi)
					.multiply(Complex.ONE.subtract(g.multiply(exp_dT)).pow(2.0));
			Complex dDdT = numerator_dDdT.divide(denominator_dDdT);

			Complex innerTerm = (b.subtract(Complex.I.multiply(rho * xi).multiply(zeta)).add(d))
					.add(
							Complex.valueOf(2.0)
							.multiply(g)
							.multiply(d)
							.multiply(exp_dT)
							.divide(Complex.ONE.subtract(g.multiply(exp_dT)))
							);

			Complex dCdT = Complex.I.multiply(zeta).multiply(riskFreeRate)
					.add(
							Complex.valueOf(kappa * theta / (xi * xi))
							.multiply(innerTerm)
							);

			return Complex.valueOf(riskFreeRate)
					.multiply(f)
					.subtract(
							f.multiply(dCdT.add(dDdT.multiply(v0)))
							);
		case RHO:
			Complex dCdr = Complex.I.multiply(zeta).multiply(optionMaturity);
			return f.multiply(dCdr).subtract(f.multiply(optionMaturity)); 
		case VEGA1:
			return f.multiply(D); 
		case VANNA:
			return f.multiply(Complex.I).multiply(zeta).multiply(D).divide(initialStockValue); 
		case VOLGA:
			return Complex.valueOf(2.0)
					.multiply(D)
					.multiply(f)
					.multiply(
							D.multiply(2.0 * v0).add(Complex.ONE)
							);
		default:
			throw new IllegalArgumentException("Unknown Greek: " + whichGreek);
		}
	}

	/**
	 * Service method that performs the Fourier inversion according to the FFT algorithm of Carr and Madan
	 * 
	 * @param initialStockValue Initital value of the stock.
	 * @param riskFreeRate The risk free rate.
	 * @param dividendYield The dividend yield.
	 * @param sigma the square root of the initial instantaneous variance (\( V_0 = sigma^2 \))
	 * @param theta the long run mean of the volatility.
	 * @param kappa the speed of mean reversion.
	 * @param xi the volatility of variance.
	 * @param rho correlation between the two Brownian motions
	 * @param optionMaturity the maturity of the option
	 * @param optionStrike the strike of the option.
	 * @param whichGreek
	 * @param numberOfPoints
	 * @param gridSpacing
	 * @return the requested calculation
	 */
	private static double hestonGreekCalculator(final double initialStockValue,
			final double riskFreeRate,
			final double dividendYield,
			final double sigma, 
			final double theta, 
			final double kappa, 
			final double xi, 
			final double rho,
			final double optionMaturity,
			final double optionStrike,
			final HestonGreek whichGreek,
			final int numberOfPoints,
			final double gridSpacing) {

		final double lineOfIntegration = 1.2;

		final double lambda = 2*Math.PI/(numberOfPoints*gridSpacing); //Equation 23 Carr and Madan
		final double upperBound = (numberOfPoints * lambda)/2.0; //Equation 20 Carr and Madan

		final double v0 = sigma*sigma;

		final Complex[] integrandEvaluations = new Complex[numberOfPoints];

		for(int i = 0; i<numberOfPoints; i++) {

			final double u = gridSpacing * i;

			//Integration over a line parallel to the real axis
			final Complex z = new Complex(u,-lineOfIntegration);

			//The characteristic function is already discounted
			final Complex numerator = hestonCharacteristicFunctionGradient(
					z.subtract(Complex.I), initialStockValue,riskFreeRate, dividendYield, v0,theta, kappa,xi, 
					rho,
					optionMaturity,
					optionStrike,
					whichGreek,
					numberOfPoints,
					gridSpacing);

			final Complex denominator = ((z.subtract(Complex.I)).multiply(z)).negate();
			Complex ratio = numerator.divide(denominator);
			ratio = (ratio.multiply(((Complex.I).multiply(upperBound*u)).exp())).multiply(gridSpacing);

			double delta;
			if (i==0){
				delta=1.0;
			}else{
				delta = 0.0;
			}
			final double simpsonWeight = (3+Math.pow(-1,i+1)-delta)/3;

			integrandEvaluations[i] = ratio.multiply(simpsonWeight);
		}

		//Compute the FFT
		Complex[] transformedVector = new Complex[numberOfPoints];
		final FastFourierTransformer fft=new FastFourierTransformer(DftNormalization.STANDARD);
		transformedVector=	fft.transform(integrandEvaluations,TransformType.FORWARD);

		//Find relevant prices via interpolation
		final double[] logStrikeVector = new double[numberOfPoints];
		final double[] strikeVector = new double[numberOfPoints];
		final double[] valuesVector = new double[numberOfPoints];

		for(int j = 0; j<numberOfPoints; j++) {
			logStrikeVector[j] = -upperBound+lambda*j;
			strikeVector[j] = Math.exp(logStrikeVector[j]);
			valuesVector[j] = (transformedVector[j].multiply(Math.exp(-lineOfIntegration * logStrikeVector[j]))).getReal()/Math.PI;
		}

		final RationalFunctionInterpolation interpolation = new RationalFunctionInterpolation(strikeVector, valuesVector,intMethod, extMethod);


		final Function<Double, Double> strikeToValue = new Function<Double, Double>(){

			@Override
			public Double apply(final Double t) {
				return  interpolation.getValue(t);
			}

		};

		return strikeToValue.apply(optionStrike);
	}
}
