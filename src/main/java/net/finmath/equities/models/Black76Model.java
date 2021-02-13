package net.finmath.equities.models;

import java.util.function.Function;

import net.finmath.functions.NormalDistribution;

/**
 * This class implements formulas for the Black76 model.
 *
 * @author Andreas Grotz
 */

public final class Black76Model {

	private Black76Model()
	{
		// This constructor will never be invoked
	}

	/**
	 * Calculates the Black76 option price and sensitivities of a call or put.
	 */
	public static double optionPrice(
			final double forward,
			final double optionStrike,
			final double optionMaturity,
			final double volatility,
			final boolean isCall,
			final double discountFactor)
	{
		final double callFactor = isCall ? 1.0 : -1.0;
		double valueAnalytic;
		if(optionMaturity < 0) {
			valueAnalytic = 0;
		}
		else if(volatility == 0.0 || optionMaturity == 0.0)
		{
			valueAnalytic = Math.max(callFactor * (forward - optionStrike),0);
		}
		else if(volatility == Double.POSITIVE_INFINITY)
		{
			valueAnalytic = isCall ? forward : optionStrike;
		}
		else
		{
			final double dPlus = (Math.log(forward / optionStrike) + 0.5 * volatility * volatility * optionMaturity)
					/ (volatility * Math.sqrt(optionMaturity));
			final double dMinus = dPlus - volatility * Math.sqrt(optionMaturity);
			valueAnalytic = callFactor * (forward * NormalDistribution.cumulativeDistribution(callFactor * dPlus)
					- optionStrike * NormalDistribution.cumulativeDistribution(callFactor * dMinus));
		}
		return valueAnalytic * discountFactor;
	}

	public static double optionDelta(
			final double forward,
			final double optionStrike,
			final double optionMaturity,
			final double volatility,
			final boolean isCall,
			final double discountFactor)
	{
		final double callFactor = isCall ? 1.0 : -1.0;
		double valueAnalytic;
		if(optionMaturity < 0) {
			valueAnalytic =  0;
		}
		else if(volatility == 0.0 || optionMaturity == 0.0)
		{
			valueAnalytic = (forward == optionStrike) ? 0.5 : (callFactor * (forward - optionStrike) > 0.0 ? callFactor : 0.0);
		}
		else if(volatility == Double.POSITIVE_INFINITY)
		{
			valueAnalytic = isCall ? 1.0 : 0.0;
		}
		else
		{
			final double dPlus = (Math.log(forward / optionStrike) + 0.5 * volatility * volatility * optionMaturity) / (volatility * Math.sqrt(optionMaturity));
			valueAnalytic = callFactor * NormalDistribution.cumulativeDistribution(callFactor * dPlus);
		}
		return valueAnalytic * discountFactor;
	}

	public static double optionVega(
			final double forward,
			final double optionStrike,
			final double optionMaturity,
			final double volatility,
			final boolean isCall,
			final double discountFactor)
	{
		double valueAnalytic;
		if(optionMaturity < 0) {
			valueAnalytic = 0;
		}
		else if(volatility == 0.0 || optionMaturity == 0.0)
		{
			valueAnalytic = 0;
		}
		else if(volatility == Double.POSITIVE_INFINITY)
		{
			valueAnalytic = 0;
		}
		else
		{
			final double sqrtT = Math.sqrt(optionMaturity);
			final double dPlus = (Math.log(forward / optionStrike) + 0.5 * volatility * volatility * optionMaturity) / (volatility * sqrtT);
			valueAnalytic =   forward * sqrtT * NormalDistribution.density(dPlus);
		}
		return valueAnalytic * discountFactor;
	}

	public static double optionGamma(
			final double forward,
			final double optionStrike,
			final double optionMaturity,
			final double volatility,
			final boolean isCall,
			final double discountFactor)
	{
		double valueAnalytic;
		if(optionMaturity < 0) {
			valueAnalytic = 0;
		}
		else if(volatility == 0.0 || optionMaturity == 0.0)
		{
			valueAnalytic = 0;
		}
		else if(volatility == Double.POSITIVE_INFINITY)
		{
			valueAnalytic = 0;
		}
		else
		{
			final double sDev = volatility * Math.sqrt(optionMaturity);
			final double dPlus = (Math.log(forward / optionStrike) + 0.5 * volatility * volatility * optionMaturity) / sDev;
			valueAnalytic =  NormalDistribution.density(dPlus) / forward / sDev;
		}
		return valueAnalytic * discountFactor;
	}

	public static double optionTheta(
			final double forward,
			final double optionStrike,
			final double optionMaturity,
			final double volatility,
			final boolean isCall,
			final double discountFactor,
			final double discountRate)
	{
		double valueAnalytic = discountRate * optionPrice(
				forward,
				optionStrike,
				optionMaturity,
				volatility,
				isCall,
				discountFactor);
		valueAnalytic -= 0.5 * forward * forward * volatility * volatility * optionGamma(
				forward,
				optionStrike,
				optionMaturity,
				volatility,
				isCall,
				discountFactor);
		return valueAnalytic;
	}

	/**
	 * Determine the implied volatility of a call or put, given its (undiscounted) market price.
	 * Implementation according to Jaeckel's 2016 paper.
	 * NOTE: The special cases of the Black-Scholes function from Section 6 in Jaeckel's paper
	 * are not implemented. Thus, the double precision convergence after
	 * two Householder steps cannot be guaranteed for all possible inputs.
	 */
	public static double optionImpliedVolatility(
			double forward,
			double optionStrike,
			double optionMaturity,
			double undiscountedPrice,
			boolean isCall)
	{
		final double x, beta, bMax;
		final double xTemp = Math.log(forward / optionStrike);
		final double betaTemp = undiscountedPrice / Math.sqrt(forward * optionStrike);
		final double bMaxTemp = Math.exp(0.5 * xTemp);
		// Convert to case of OTM Call
		if (isCall)
		{
			if (xTemp > 0.0)
			{
				// ITM call
				x = -xTemp;
				bMax = Math.exp(0.5 * x);
				beta = betaTemp + 2.0 * Math.sinh(0.5 * x);
			}
			else
			{
				x = xTemp;
				beta = betaTemp;
				bMax = bMaxTemp;
			}
		}
		else
		{
			if (xTemp >= 0.0)
			{
				// OTM put
				x = -xTemp;
				beta = betaTemp;
				bMax = Math.exp(0.5 * x);
			}
			else
			{
				// ITM put
				x = xTemp;
				beta = betaTemp + 2.0 * Math.sinh(0.5 * x);
				bMax = bMaxTemp;
			}
		}
		assert beta >= 0.0 && beta <= bMax : "The price " + undiscountedPrice
				+ "is not attainable in Black-Scholes given the other parameters provided.";
		if (x == 0.0) {
			return 2.0 * NormalDistribution.inverseCumulativeDistribution(0.5 * (beta + 1.0));
		}
		// Initial guess using rational interpolation
		final double sqrtPi = Math.sqrt(2.0 * Math.PI);
		final double sigmaCentral = Math.sqrt(-2.0 * x);
		final double d1Central = x / sigmaCentral;
		final double d2Central = 0.5 * sigmaCentral;
		final double bCentral = NormalDistribution.cumulativeDistribution(d1Central + d2Central) * bMax -
				NormalDistribution.cumulativeDistribution(d1Central - d2Central) / bMax;
		final double bPrimeCentral = Math.exp(-0.5 * (d1Central * d1Central + d2Central * d2Central)) / sqrtPi;
		final double sigmaLower = sigmaCentral - bCentral / bPrimeCentral;
		final double d1Lower = x / sigmaLower;
		final double d2Lower = 0.5 * sigmaLower;
		final double bLower = NormalDistribution.cumulativeDistribution(d1Lower + d2Lower) * bMax -
				NormalDistribution.cumulativeDistribution(d1Lower - d2Lower) / bMax;
		final double sigmaUpper = sigmaCentral + (bMax - bCentral) / bPrimeCentral;
		final double d1Upper = x / sigmaUpper;
		final double d2Upper = 0.5 * sigmaUpper;
		final double bUpper = NormalDistribution.cumulativeDistribution(d1Upper + d2Upper) * bMax -
				NormalDistribution.cumulativeDistribution(d1Upper - d2Upper) / bMax;

		double impliedSdev;
		if (beta < bLower)
		{
			final double sqrtThree = Math.sqrt(3.0);
			final double twoPi = 2.0 * Math.PI;
			final double z = x / sigmaLower / sqrtThree;
			final double normDistOfZ = NormalDistribution.cumulativeDistribution(z);
			final double fOfZ = -twoPi * x * normDistOfZ * normDistOfZ * normDistOfZ / 3.0 / sqrtThree;
			final double sigmaLowerSquare = sigmaLower * sigmaLower;
			final double zSquare = z * z;
			final double fPrime = twoPi * zSquare * normDistOfZ * normDistOfZ * Math.exp(zSquare + sigmaLowerSquare / 8.0);
			final double fPrime2 = Math.PI * zSquare * normDistOfZ * Math.exp(2.0 * zSquare + sigmaLowerSquare / 4.0)
					/ 6.0 / sigmaLowerSquare / sigmaLower
					* (-8 * sqrtThree * sigmaLower * x
							+ (3.0 * sigmaLowerSquare * (sigmaLowerSquare - 8.0) - 8.0 * x * x) * normDistOfZ / NormalDistribution.density(z));
			final double r = (0.5 * fPrime2 * bLower + fPrime - 1.0) / (fPrime - fOfZ / bLower);
			final double fRationalCubic = rationalCubicInterpol(beta, 0.0, bLower, 0.0, fOfZ, 1.0, fPrime, r);
			impliedSdev = NormalDistribution.inverseCumulativeDistribution(sqrtThree * Math.pow(Math.abs(fRationalCubic / twoPi / x), 1.0 / 3.0));
			impliedSdev = Math.abs(x / sqrtThree / impliedSdev);
		}
		else if (beta <= bCentral)
		{
			final double bPrimeLower1 = Math.exp(0.5 * (d1Lower * d1Lower + d2Lower * d2Lower)) * sqrtPi;
			final double bPrimeCentral1 = 1.0 / bPrimeCentral;
			final double r = (bPrimeCentral1 - bPrimeLower1) / (bPrimeCentral1 - (sigmaCentral - sigmaLower) / (bCentral - bLower) );
			impliedSdev = rationalCubicInterpol(beta, bLower, bCentral, sigmaLower, sigmaCentral, bPrimeLower1, bPrimeCentral1, r);
		}
		else if (beta <= bUpper)
		{
			final double bPrimeUpper1 = Math.exp(0.5 * (d1Upper * d1Upper + d2Upper * d2Upper)) * sqrtPi;
			final double bPrimeCentral1 = 1.0 / bPrimeCentral;
			final double r = (bPrimeUpper1 - bPrimeCentral1) / ((sigmaUpper - sigmaCentral) / (bUpper - bCentral) - bPrimeCentral1);
			impliedSdev = rationalCubicInterpol(beta, bCentral, bUpper, sigmaCentral, sigmaUpper, bPrimeCentral1, bPrimeUpper1, r);
		}
		else
		{
			final double f = NormalDistribution.cumulativeDistribution(-0.5 * sigmaUpper);
			final double sigmaUpper2 = sigmaUpper * sigmaUpper;
			final double xSigma = x * x / sigmaUpper2;
			final double fPrime = -0.5 * Math.exp(0.5 * xSigma);
			final double fPrime2 = Math.sqrt(0.5 * Math.PI) * xSigma / sigmaUpper * Math.exp(xSigma + sigmaUpper2 / 8);
			final double h = bMax - bUpper;
			final double r = (0.5 * fPrime2 * h - 0.5 - fPrime) / (-f / h - fPrime);
			final double fRC = rationalCubicInterpol(beta, bUpper, bMax, f, 0.0, fPrime, -0.5, r);
			impliedSdev = -2.0 * NormalDistribution.inverseCumulativeDistribution(fRC);
		}

		// Third-order Householder steps using three branch rational objective function
		final double bMaxHalf = 0.5 * bMax;
		final double bTildeUpper = (bUpper >= bMaxHalf) ? bUpper : bMaxHalf;

		// Efficient implementation of Black derivatives
		// We have b(x) = b0, db(x)/dx = b1, d^2b(x)/dx^2 = b2 * b1, d^3b(x)/dx^3 = b3 * b1
		final Function<Double, Double[]> BlackFunctionDerivatives = sigma -> {
			final double d1 = x / sigma;
			final double d2 = 0.5 * sigma;
			final double d1Square = d1 * d1;
			final double d2Square = d2 * d2;
			final double b0 = NormalDistribution.cumulativeDistribution(d1 + d2) * bMax - NormalDistribution.cumulativeDistribution(d1 - d2) / bMax;
			final double b1 = Math.exp(-0.5 * (d1Square + d2Square)) / sqrtPi;
			final double b2 = d1Square / sigma - 0.25 * sigma;
			final double b3 = b2 * b2 - 0.75 * d1Square / d2Square - 0.25;
			return new Double[] {b0, b1, b2, b3};
		};

		if (beta <= bLower)
		{
			final Function<Double, Double> HouseholderStep = sigma ->
			{
				final Double[] derivatives = BlackFunctionDerivatives.apply(sigma);
				final double b0 = derivatives[0];
				final double b1 = derivatives[1];
				final double b2 = derivatives[2];
				final double b3 = derivatives[3];
				final double lnOfB = Math.log(b0);
				final double bLnOfB = b0 * lnOfB;
				final double bLnOfBSquare = bLnOfB * bLnOfB;
				final double nu = bLnOfB * (1.0 - lnOfB / Math.log(beta)) / b1;
				final double gamma = (b0 * b2 * lnOfB - b1 * (lnOfB + 2.0)) / bLnOfB;
				final double delta = (bLnOfBSquare * b3 + 2.0 * b1 * b1 * (lnOfB * lnOfB + 3.0 * lnOfB + 3.0)
						- 3.0 * bLnOfB * b1 * b2 * (lnOfB + 2.0)) / bLnOfBSquare;
				return sigma + nu * (1.0 + 0.5 * nu * gamma) / (1.0 + nu * (gamma + delta * nu / 6.0));
			};
			impliedSdev = HouseholderStep.apply(impliedSdev);
			impliedSdev = HouseholderStep.apply(impliedSdev);
		}
		else if (beta <= bTildeUpper)
		{
			final Function<Double, Double> HouseholderStep = sigma ->
			{
				final Double[] deriv = BlackFunctionDerivatives.apply(sigma);
				final double b0 = deriv[0] - beta;
				final double b1 = deriv[1];
				final double b2 = deriv[2];
				final double b3 = deriv[3];
				final double nu = -b0 / b1;
				final double gamma = b2;
				final double delta = b3;
				return sigma + nu * (1.0 + 0.5 * nu * gamma) / (1.0 + nu * (gamma + delta * nu / 6.0));
			};
			impliedSdev = HouseholderStep.apply(impliedSdev);
			impliedSdev = HouseholderStep.apply(impliedSdev);
		}
		else
		{
			final Function<Double, Double> HouseholderStep = sigma ->
			{
				final Double[] deriv = BlackFunctionDerivatives.apply(sigma);
				final double b0 = deriv[0];
				final double b1 = deriv[1];
				final double b2 = deriv[2];
				final double b3 = deriv[3];
				final double bmaxb0 = bMax - b0;
				final double nu = bmaxb0 * Math.log(bmaxb0 / (bMax - beta)) / b1;
				final double gamma = b2 + b1 / bmaxb0;
				final double delta = b3 + 3.0 * b1 * b2 / bmaxb0 + 2.0 * b1 * b1 / bmaxb0 / bmaxb0;
				return sigma + nu * (1.0 + 0.5 * nu * gamma) / (1.0 + nu * (gamma + delta * nu / 6.0));
			};
			impliedSdev = HouseholderStep.apply(impliedSdev);
			impliedSdev = HouseholderStep.apply(impliedSdev);
		}

		// Return implied volatility
		return impliedSdev / Math.sqrt(optionMaturity);
	}

	/**
	 * Helper function for rational cubic interpolation in implied volatility calculations
	 */
	private static double rationalCubicInterpol(double xValue, double xLeft, double xRight, double fLeft, double fRight, double fPrimeLeft, double fPrimeRight, double blend)
	{
		final double h = xRight - xLeft;
		final double s = (xValue - xLeft) / h;
		final double sMinusOne = 1.0 - s;
		return (fRight * s * s * s + (blend * fRight - h * fPrimeRight) * s * s * sMinusOne
				+ (blend * fLeft + h * fPrimeLeft) * s * sMinusOne * sMinusOne + fLeft * sMinusOne * sMinusOne * sMinusOne)
				/ (1 + (blend - 3) * s * sMinusOne);
	}
}
