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
 * The calculation is performed by means of the FFT algorithm of Carr Madan applied to the gradient of the
 * characteristic funtion.
 *
 * @author Alessandro Gnoatto
 */
public class HestonModel {

	/*
	 * Parameters for the FFT calculator.
	 */
	private static final InterpolationMethod INT_METHOD = InterpolationMethod.LINEAR;
	private static final ExtrapolationMethod EXT_METHOD = ExtrapolationMethod.CONSTANT;
	private static final int NUMBER_OF_POINTS = 4096 * 2;
	private static final double GRID_SPACING = 0.4;

	enum HestonGreek {DELTA, GAMMA, THETA, RHO, VEGA1, VANNA, VOLGA}

	/**
	 * Calculates the delta of a call option under a Heston model.
	 *
	 * @param initialStockValue Initital value of the stock.
	 * @param riskFreeRate The risk free rate.
	 * @param dividendYield The dividend yield.
	 * @param kappa The speed of mean reversion.
	 * @param theta The long run mean of the volatility.
	 * @param sigma The volatility of variance.
	 * @param v0 The initial instantaneous variance.
	 * @param rho Correlation between the two Brownian motions.
	 * @param optionMaturity The maturity of the option.
	 * @param optionStrike The strike of the option.
	 * @return The delta of the option.
	 */
	public static double hestonOptionDelta(
			final double initialStockValue,
			final double riskFreeRate,
			final double dividendYield,
			final double kappa,
			final double theta,
			final double sigma,
			final double v0,
			final double rho,
			final double optionMaturity,
			final double optionStrike) {

		final HestonGreek whatToCompute = HestonGreek.DELTA;

		return hestonGreekCalculator(
				initialStockValue,
				riskFreeRate,
				dividendYield,
				kappa,
				theta,
				sigma,
				v0,
				rho,
				optionMaturity,
				optionStrike,
				whatToCompute,
				NUMBER_OF_POINTS,
				GRID_SPACING);
	}

	/**
	 * Calculates the gamma of a call option under a Heston model.
	 *
	 * @param initialStockValue Initital value of the stock.
	 * @param riskFreeRate The risk free rate.
	 * @param dividendYield The dividend yield.
	 * @param kappa The speed of mean reversion.
	 * @param theta The long run mean of the volatility.
	 * @param sigma The volatility of variance.
	 * @param v0 The initial instantaneous variance.
	 * @param rho Correlation between the two Brownian motions.
	 * @param optionMaturity The maturity of the option.
	 * @param optionStrike The strike of the option.
	 * @return The gamma of the option.
	 */
	public static double hestonOptionGamma(
			final double initialStockValue,
			final double riskFreeRate,
			final double dividendYield,
			final double kappa,
			final double theta,
			final double sigma,
			final double v0,
			final double rho,
			final double optionMaturity,
			final double optionStrike) {

		final HestonGreek whatToCompute = HestonGreek.GAMMA;

		return hestonGreekCalculator(
				initialStockValue,
				riskFreeRate,
				dividendYield,
				kappa,
				theta,
				sigma,
				v0,
				rho,
				optionMaturity,
				optionStrike,
				whatToCompute,
				NUMBER_OF_POINTS,
				GRID_SPACING);
	}

	/**
	 * Calculates the theta of a call option under a Heston model.
	 *
	 * @param initialStockValue Initital value of the stock.
	 * @param riskFreeRate The risk free rate.
	 * @param dividendYield The dividend yield.
	 * @param kappa The speed of mean reversion.
	 * @param theta The long run mean of the volatility.
	 * @param sigma The volatility of variance.
	 * @param v0 The initial instantaneous variance.
	 * @param rho Correlation between the two Brownian motions.
	 * @param optionMaturity The maturity of the option.
	 * @param optionStrike The strike of the option.
	 * @return The theta of the option.
	 */
	public static double hestonOptionTheta(
			final double initialStockValue,
			final double riskFreeRate,
			final double dividendYield,
			final double kappa,
			final double theta,
			final double sigma,
			final double v0,
			final double rho,
			final double optionMaturity,
			final double optionStrike) {

		final HestonGreek whatToCompute = HestonGreek.THETA;

		return hestonGreekCalculator(
				initialStockValue,
				riskFreeRate,
				dividendYield,
				kappa,
				theta,
				sigma,
				v0,
				rho,
				optionMaturity,
				optionStrike,
				whatToCompute,
				NUMBER_OF_POINTS,
				GRID_SPACING);
	}

	/**
	 * Calculates the rho of a call option under a Heston model.
	 *
	 * @param initialStockValue Initital value of the stock.
	 * @param riskFreeRate The risk free rate.
	 * @param dividendYield The dividend yield.
	 * @param kappa The speed of mean reversion.
	 * @param theta The long run mean of the volatility.
	 * @param sigma The volatility of variance.
	 * @param v0 The initial instantaneous variance.
	 * @param rho Correlation between the two Brownian motions.
	 * @param optionMaturity The maturity of the option.
	 * @param optionStrike The strike of the option.
	 * @return The rho of the option.
	 */
	public static double hestonOptionRho(
			final double initialStockValue,
			final double riskFreeRate,
			final double dividendYield,
			final double kappa,
			final double theta,
			final double sigma,
			final double v0,
			final double rho,
			final double optionMaturity,
			final double optionStrike) {

		final HestonGreek whatToCompute = HestonGreek.RHO;

		return hestonGreekCalculator(
				initialStockValue,
				riskFreeRate,
				dividendYield,
				kappa,
				theta,
				sigma,
				v0,
				rho,
				optionMaturity,
				optionStrike,
				whatToCompute,
				NUMBER_OF_POINTS,
				GRID_SPACING);
	}

	/**
	 * Calculates the vega1 of a call option under a Heston model.
	 *
	 * @param initialStockValue Initital value of the stock.
	 * @param riskFreeRate The risk free rate.
	 * @param dividendYield The dividend yield.
	 * @param kappa The speed of mean reversion.
	 * @param theta The long run mean of the volatility.
	 * @param sigma The volatility of variance.
	 * @param v0 The initial instantaneous variance.
	 * @param rho Correlation between the two Brownian motions.
	 * @param optionMaturity The maturity of the option.
	 * @param optionStrike The strike of the option.
	 * @return The vega1 of the option.
	 */
	public static double hestonOptionVega1(
			final double initialStockValue,
			final double riskFreeRate,
			final double dividendYield,
			final double kappa,
			final double theta,
			final double sigma,
			final double v0,
			final double rho,
			final double optionMaturity,
			final double optionStrike) {

		final HestonGreek whatToCompute = HestonGreek.VEGA1;

		return hestonGreekCalculator(
				initialStockValue,
				riskFreeRate,
				dividendYield,
				kappa,
				theta,
				sigma,
				v0,
				rho,
				optionMaturity,
				optionStrike,
				whatToCompute,
				NUMBER_OF_POINTS,
				GRID_SPACING);
	}

	/**
	 * Calculates the vanna of a call option under a Heston model.
	 *
	 * @param initialStockValue Initital value of the stock.
	 * @param riskFreeRate The risk free rate.
	 * @param dividendYield The dividend yield.
	 * @param kappa The speed of mean reversion.
	 * @param theta The long run mean of the volatility.
	 * @param sigma The volatility of variance.
	 * @param v0 The initial instantaneous variance.
	 * @param rho Correlation between the two Brownian motions.
	 * @param optionMaturity The maturity of the option.
	 * @param optionStrike The strike of the option.
	 * @return The vanna of the option.
	 */
	public static double hestonOptionVanna(
			final double initialStockValue,
			final double riskFreeRate,
			final double dividendYield,
			final double kappa,
			final double theta,
			final double sigma,
			final double v0,
			final double rho,
			final double optionMaturity,
			final double optionStrike) {

		final HestonGreek whatToCompute = HestonGreek.VANNA;

		return hestonGreekCalculator(
				initialStockValue,
				riskFreeRate,
				dividendYield,
				kappa,
				theta,
				sigma,
				v0,
				rho,
				optionMaturity,
				optionStrike,
				whatToCompute,
				NUMBER_OF_POINTS,
				GRID_SPACING);
	}

	/**
	 * Calculates the volga of a call option under a Heston model.
	 *
	 * @param initialStockValue Initital value of the stock.
	 * @param riskFreeRate The risk free rate.
	 * @param dividendYield The dividend yield.
	 * @param kappa The speed of mean reversion.
	 * @param theta The long run mean of the volatility.
	 * @param sigma The volatility of variance.
	 * @param v0 The initial instantaneous variance.
	 * @param rho Correlation between the two Brownian motions.
	 * @param optionMaturity The maturity of the option.
	 * @param optionStrike The strike of the option.
	 * @return The volga of the option.
	 */
	public static double hestonOptionVolga(
			final double initialStockValue,
			final double riskFreeRate,
			final double dividendYield,
			final double kappa,
			final double theta,
			final double sigma,
			final double v0,
			final double rho,
			final double optionMaturity,
			final double optionStrike) {

		final HestonGreek whatToCompute = HestonGreek.VOLGA;

		return hestonGreekCalculator(
				initialStockValue,
				riskFreeRate,
				dividendYield,
				kappa,
				theta,
				sigma,
				v0,
				rho,
				optionMaturity,
				optionStrike,
				whatToCompute,
				NUMBER_OF_POINTS,
				GRID_SPACING);
	}

	private static Complex hestonCharacteristicFunctionGradient(
			final Complex zeta,
			final double initialStockValue,
			final double riskFreeRate,
			final double dividendYield,
			final double kappa,
			final double theta,
			final double sigma,
			final double v0,
			final double rho,
			final double optionMaturity,
			final double optionStrike,
			final HestonGreek whichGreek,
			final int numberOfPoints,
			final double gridSpacing) {

		final double lambda = 2 * Math.PI / (numberOfPoints * gridSpacing);

		final double x = Math.log(initialStockValue);
		final double a = kappa * theta;

		Complex b;
		Complex u;
		Complex d;
		Complex g;
		Complex c;
		Complex D;
		Complex G;
		Complex C;

		u = new Complex(-0.5, 0.0);
		b = new Complex(kappa + lambda, 0.0);

		final Complex term1 = (Complex.I.multiply(rho * sigma).multiply(zeta)).subtract(b);
		final Complex powPart = term1.pow(2.0);
		final Complex term2 = new Complex(sigma * sigma, 0.0)
				.multiply((u.multiply(2.0).multiply(Complex.I).multiply(zeta))
						.subtract(zeta.multiply(zeta)));
		d = powPart.subtract(term2).sqrt();

		final Complex numerator = (b.subtract(Complex.I.multiply(rho * sigma).multiply(zeta))).add(d);
		final Complex denominator = (b.subtract(Complex.I.multiply(rho * sigma).multiply(zeta))).subtract(d);
		g = numerator.divide(denominator);

		c = Complex.ONE.divide(g);

		final Complex exp_dT = d.multiply(-optionMaturity).exp();
		final Complex bMinusRhoSigmaIphiMinusD = b.subtract(Complex.I.multiply(rho * sigma).multiply(zeta)).subtract(d);

		D = bMinusRhoSigmaIphiMinusD
				.divide(sigma * sigma)
				.multiply(
						(Complex.ONE.subtract(exp_dT))
								.divide(Complex.ONE.subtract(c.multiply(exp_dT))));

		G = (Complex.ONE.subtract(c.multiply(exp_dT)))
				.divide(Complex.ONE.subtract(c));

		final Complex firstTerm = Complex.I.multiply(zeta).multiply((riskFreeRate - dividendYield) * optionMaturity);
		final Complex secondTerm = new Complex(a / (sigma * sigma), 0.0)
				.multiply(
						(bMinusRhoSigmaIphiMinusD.multiply(optionMaturity))
								.subtract(Complex.valueOf(2.0).multiply(G.log())));

		C = firstTerm.add(secondTerm);

		final Complex f = (C.add(D.multiply(v0)).add(Complex.I.multiply(zeta).multiply(x))).exp();

		switch(whichGreek) {
		case DELTA:
			return f.multiply(Complex.I).multiply(zeta).divide(initialStockValue);
		case GAMMA:
			return Complex.I.multiply(zeta)
					.multiply(f)
					.multiply(Complex.I.multiply(zeta).subtract(Complex.ONE))
					.divide(initialStockValue * initialStockValue);
		case THETA:
			final Complex numerator_dDdT = d.multiply(exp_dT)
					.multiply(b.subtract(Complex.I.multiply(rho * sigma).multiply(zeta)).add(d))
					.multiply(g.subtract(Complex.ONE));
			final Complex denominator_dDdT = Complex.valueOf(sigma * sigma)
					.multiply(Complex.ONE.subtract(g.multiply(exp_dT)).pow(2.0));
			final Complex dDdT = numerator_dDdT.divide(denominator_dDdT);

			final Complex innerTerm = (b.subtract(Complex.I.multiply(rho * sigma).multiply(zeta)).add(d))
					.add(
							Complex.valueOf(2.0)
									.multiply(g)
									.multiply(d)
									.multiply(exp_dT)
									.divide(Complex.ONE.subtract(g.multiply(exp_dT))));

			final Complex dCdT = Complex.I.multiply(zeta).multiply(riskFreeRate)
					.add(
							Complex.valueOf(kappa * theta / (sigma * sigma))
									.multiply(innerTerm));

			return Complex.valueOf(riskFreeRate)
					.multiply(f)
					.subtract(f.multiply(dCdT.add(dDdT.multiply(v0))));
		case RHO:
			final Complex dCdr = Complex.I.multiply(zeta).multiply(optionMaturity);
			return f.multiply(dCdr).subtract(f.multiply(optionMaturity));
		case VEGA1:
			return f.multiply(D);
		case VANNA:
			return f.multiply(Complex.I).multiply(zeta).multiply(D).divide(initialStockValue);
		case VOLGA:
			return Complex.valueOf(2.0)
					.multiply(D)
					.multiply(f)
					.multiply(D.multiply(2.0 * v0).add(Complex.ONE));
		default:
			throw new IllegalArgumentException("Unknown Greek: " + whichGreek);
		}
	}

	private static double hestonGreekCalculator(
			final double initialStockValue,
			final double riskFreeRate,
			final double dividendYield,
			final double kappa,
			final double theta,
			final double sigma,
			final double v0,
			final double rho,
			final double optionMaturity,
			final double optionStrike,
			final HestonGreek whichGreek,
			final int numberOfPoints,
			final double gridSpacing) {

		final double lineOfIntegration = 1.2;

		final double lambda = 2 * Math.PI / (numberOfPoints * gridSpacing);
		final double upperBound = (numberOfPoints * lambda) / 2.0;

		final Complex[] integrandEvaluations = new Complex[numberOfPoints];

		for(int i = 0; i < numberOfPoints; i++) {

			final double u = gridSpacing * i;
			final Complex z = new Complex(u, -lineOfIntegration);

			final Complex numerator = hestonCharacteristicFunctionGradient(
					z.subtract(Complex.I),
					initialStockValue,
					riskFreeRate,
					dividendYield,
					kappa,
					theta,
					sigma,
					v0,
					rho,
					optionMaturity,
					optionStrike,
					whichGreek,
					numberOfPoints,
					gridSpacing);

			final Complex denominator = ((z.subtract(Complex.I)).multiply(z)).negate();
			Complex ratio = numerator.divide(denominator);
			ratio = (ratio.multiply(((Complex.I).multiply(upperBound * u)).exp())).multiply(gridSpacing);

			final double delta = (i == 0) ? 1.0 : 0.0;
			final double simpsonWeight = (3 + Math.pow(-1, i + 1) - delta) / 3;

			integrandEvaluations[i] = ratio.multiply(simpsonWeight);
		}

		final FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);
		final Complex[] transformedVector = fft.transform(integrandEvaluations, TransformType.FORWARD);

		final double[] logStrikeVector = new double[numberOfPoints];
		final double[] strikeVector = new double[numberOfPoints];
		final double[] valuesVector = new double[numberOfPoints];

		for(int j = 0; j < numberOfPoints; j++) {
			logStrikeVector[j] = -upperBound + lambda * j;
			strikeVector[j] = Math.exp(logStrikeVector[j]);
			valuesVector[j] = (transformedVector[j].multiply(Math.exp(-lineOfIntegration * logStrikeVector[j]))).getReal()
					/ Math.PI;
		}

		final RationalFunctionInterpolation interpolation = new RationalFunctionInterpolation(
				strikeVector,
				valuesVector,
				INT_METHOD,
				EXT_METHOD);

		final Function<Double, Double> strikeToValue = new Function<Double, Double>() {
			@Override
			public Double apply(final Double t) {
				return interpolation.getValue(t);
			}
		};

		return strikeToValue.apply(optionStrike);
	}
}
