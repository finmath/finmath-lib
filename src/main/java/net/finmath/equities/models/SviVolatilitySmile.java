package net.finmath.equities.models;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * Class that implements the smile-specific parts of the SVI volatility parametrization
 * from Gatheral's 2013 paper.
 *
 * @author Andreas Grotz
 */

public class SviVolatilitySmile {

	private final double a;
	private final double b;
	private final double rho;
	private final double m;
	private final double sigma;
	private final LocalDate smileDate;

	public SviVolatilitySmile(LocalDate date, double a, double b, double rho, double m, double sigma) {
		this.a = a;
		this.b = b;
		this.rho = rho;
		this.m = m;
		this.sigma = sigma;
		smileDate = date;
		validate();
	}

	public static double sviTotalVariance(
			double logStrike, double a, double b, double rho, double m, double sigma)
	{
		final var kShifted = logStrike - m;
		return a + b * (rho * kShifted + Math.sqrt(kShifted * kShifted + sigma * sigma));
	}

	public static double sviVolatility(
			double logStrike, double a, double b, double rho, double m, double sigma, double ttm)
	{
		return Math.sqrt(sviTotalVariance(logStrike, a, b, rho, m, sigma) / ttm);
	}

	public static double[] sviInitialGuess(ArrayList<Double> logStrikes, ArrayList<Double> totalVariances)
	{
		// Use the Jump Wing parametrization from Gatheral's 2013 paper to derive an initial guess
		final var nPoints = logStrikes.size();
		assert nPoints >= 5 : "An initial guess for SVI is not sensible with less than 5 points.";
		final var minIndex = totalVariances.indexOf(Collections.min(totalVariances));
		final var k0 = logStrikes.get(minIndex);
		if(k0 == 0.0)
		{
			final var atmIndex = logStrikes.indexOf(0.0);
			final var w = totalVariances.get(atmIndex);
			final var d2wdk2 = 2 * (totalVariances.get(atmIndex + 1) * totalVariances.get(atmIndex - 1) - 2 * w)
					/ (Math.pow(logStrikes.get(atmIndex + 1), 2) + Math.pow(logStrikes.get(atmIndex - 1), 2));
			final var c = (totalVariances.get(nPoints - 1) - totalVariances.get(nPoints - 2))
					/ (logStrikes.get(nPoints - 1) - logStrikes.get(nPoints - 2));
			final var p = (totalVariances.get(0) - totalVariances.get(1))
					/ (logStrikes.get(1) - logStrikes.get(0));
			final var b = 0.5 * (c + p);
			final var rho = 1 - 2 * p / (c + p);
			final var m = b * (1 - rho * rho) * Math.abs(rho) / d2wdk2;
			final var sigma = m * Math.sqrt(1 - rho * rho) / rho;
			final var a = w - b * sigma * Math.sqrt(1 - rho * rho);
			return new double[] {a, b, rho, m, sigma};
		}
		else
		{
			final var wMin = totalVariances.get(minIndex);
			double w, dwdk;
			if (logStrikes.contains(0.0))
			{
				final var atmIndex = logStrikes.indexOf(0.0);
				w = totalVariances.get(atmIndex);
				dwdk = 0.5 * ((totalVariances.get(atmIndex + 1) - w) / (logStrikes.get(atmIndex + 1))
						+ (totalVariances.get(atmIndex - 1) - w) / (logStrikes.get(atmIndex - 1)));
			}
			else
			{
				final var maxNegIndex = logStrikes.indexOf(
						Collections.max(logStrikes.stream().filter(s -> s < 0.0).collect(Collectors.toList())));
				dwdk = (totalVariances.get(maxNegIndex + 1) - totalVariances.get(maxNegIndex))
						/ (logStrikes.get(maxNegIndex + 1) - logStrikes.get(maxNegIndex));
				w = totalVariances.get(maxNegIndex) - dwdk * logStrikes.get(maxNegIndex);
			}
			final var c = (totalVariances.get(nPoints - 1) - totalVariances.get(nPoints - 2))
					/ (logStrikes.get(nPoints - 1) - logStrikes.get(nPoints - 2));
			final var p = (totalVariances.get(0) - totalVariances.get(1))
					/ (logStrikes.get(1) - logStrikes.get(0));

			final var b = 0.5 * (c + p);
			final var rho = 1 - 2 * p / (c + p);
			final var beta = rho - dwdk / b;
			final var alpha = Math.signum(beta) * Math.sqrt(1 / beta / beta - 1);
			final var m = (w - wMin) / b / (Math.signum(alpha) * Math.sqrt(1 + alpha * alpha)
					- alpha * Math.sqrt(1 - rho * rho) - rho);
			final var sigma = alpha * m;
			final var a = wMin - b * sigma * Math.sqrt(1 - rho * rho);
			return new double[] {a, b, rho, m, sigma};
		}
	}

	public void validate()
	{
		assert getB() >= 0.0;
		assert Math.abs(getRho()) < 1.0;
		assert getSigma() > 0.0;
		assert getA() + getB() * getSigma() * Math.sqrt(1.0 - getRho() * getRho()) >= 0.0;

	}

	public double getTotalVariance(double logStrike)
	{
		return sviTotalVariance(logStrike, getA(), getB(), getRho(), getM(), getSigma());
	}

	public double getTotalVariance(double strike, double forward)
	{
		return sviTotalVariance(Math.log(strike/forward), getA(), getB(), getRho(), getM(), getSigma());
	}

	public double getVolatility(double strike, double forward, double timeToExpiry)
	{
		return sviVolatility(Math.log(strike/forward), getA(), getB(), getRho(), getM(), getSigma(), timeToExpiry);
	}

	public LocalDate getSmileDate() {
		return smileDate;
	}

	public double getSigma() {
		return sigma;
	}

	public double getM() {
		return m;
	}

	public double getRho() {
		return rho;
	}

	public double getB() {
		return b;
	}

	public double getA() {
		return a;
	}
}
