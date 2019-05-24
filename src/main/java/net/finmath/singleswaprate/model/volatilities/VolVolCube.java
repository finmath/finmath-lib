package net.finmath.singleswaprate.model.volatilities;

import java.time.LocalDate;
import java.util.Map;

import net.finmath.interpolation.RationalFunctionInterpolation;
import net.finmath.interpolation.RationalFunctionInterpolation.ExtrapolationMethod;
import net.finmath.interpolation.RationalFunctionInterpolation.InterpolationMethod;
import net.finmath.marketdata.model.volatilities.VolatilitySurface.QuotingConvention;
import net.finmath.singleswaprate.model.VolatilityCubeModel;
import net.finmath.singleswaprate.model.curves.ExponentialCorrelationCurve;
import net.finmath.time.Schedule;

/**
 * This cube provides the volatility of the stochastic driver for each sub-tenor of the swap rate's schedule in the Piterbarg model of the annuity mapping. They are linked to normal volatilities via
 * \[ \frac{\tau_j}{1+\tau_j S_j(0)} \rho_{i,j} \sigma_j ]\,
 * where \(\tau\) is the accrual fraction, \(S_j(0)\) is the swap rate of the j-th subtenor evaluated at time 0, \(\sigma_j\) the volatility of the j-th subtenor at the strike and \(\rho_{i,j}\) is the
 * correlation between the swap rates of the two tenors. We assume a correlation according to
 * \[ \rho_{i,j} = e^{d(T_j - T_i)} \],
 * where d is some decay parameter, given by the underlying cube.
 *
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public class VolVolCube implements VolatilityCube {

	private final String		name;
	private final LocalDate	referenceDate;
	private final String referenceCubeName;
	private final double baseTermination;

	private final double periodLength;

	private final RationalFunctionInterpolation rateInterpolator;

	//	private final double iborOisDecorrelation;


	QuotingConvention quotingConvention = QuotingConvention.VOLATILITYNORMAL;

	/**
	 * Create the volvol cube.
	 *
	 * @param name The name of the cube.
	 * @param referenceDate The referenceDate of the cube.
	 * @param referenceCubeName The name of the underlying cube.
	 * @param schedule The schedule of the swap rate.
	 * @param initialSwapRates Initial swap rates of all sub-tenors.
	 */
	public VolVolCube(String name, LocalDate referenceDate, String referenceCubeName, Schedule schedule, double[] initialSwapRates) {
		super();
		this.name = name;
		this.referenceDate = referenceDate;
		this.referenceCubeName = referenceCubeName;
		this.baseTermination = schedule.getPayment(schedule.getNumberOfPeriods()-1);

		double periodLength = 0;
		for(int index = 0; index < schedule.getNumberOfPeriods(); index++) {
			periodLength += schedule.getPeriodLength(index);
		}
		this.periodLength = periodLength /schedule.getNumberOfPeriods();

		double[] tenors = new double[initialSwapRates.length];
		for(int index = 0; index < tenors.length; index++) {
			tenors[index] = schedule.getPeriodStart(index);
		}
		tenors[tenors.length -1] = schedule.getPeriodEnd(schedule.getNumberOfPeriods()-1);

		rateInterpolator = new RationalFunctionInterpolation(tenors, initialSwapRates, InterpolationMethod.LINEAR, ExtrapolationMethod.DEFAULT);
	}

	@Override
	public double getValue(VolatilityCubeModel model, double termination, double maturity, double strike, QuotingConvention quotingConvention) {

		VolatilityCube cube = model.getVolatilityCube(referenceCubeName);
		ExponentialCorrelationCurve correlation = new ExponentialCorrelationCurve(name, referenceDate, baseTermination, cube.getCorrelationDecay());

		double value = cube.getValue(model, termination, maturity, strike, quotingConvention);
		value *= correlation.getValue(termination);
		value *= periodLength /(1.0 +periodLength *rateInterpolator.getValue(termination));

		return value;
	}

	@Override
	public String getName(){
		return name;
	}

	@Override
	public LocalDate getReferenceDate() {
		return referenceDate;
	}

	public String getReferenceCubeName() {
		return referenceCubeName;
	}

	@Override
	public String toString() {
		return super.toString() + "\n\"" + this.getName() + "\"";
	}

	@Override
	public double getValue(double tenorLength, double maturity, double strike, QuotingConvention quotingConvention) {
		return getValue(null, tenorLength, maturity, strike, quotingConvention);
	}

	@Override
	public double getCorrelationDecay() {
		throw new UnsupportedOperationException("This VolVolCube does not support a further native correlated cube.");
	}

	@Override
	public Map<String, Object> getParameters() {
		//TODO
		throw new UnsupportedOperationException("This VolVolCube's field cannot be converted to Map<String, Double>");
	}

	@Override
	public double getLowestStrike(VolatilityCubeModel model) {
		return model.getVolatilityCube(referenceCubeName).getLowestStrike(model);
	}

	@Override
	public double getIborOisDecorrelation() {
		throw new UnsupportedOperationException("This VolVolCube does not support use of ibor ois decorrelation.");
	}

}
