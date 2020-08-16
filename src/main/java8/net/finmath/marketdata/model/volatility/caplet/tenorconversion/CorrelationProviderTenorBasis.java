package net.finmath.marketdata.model.volatility.caplet.tenorconversion;

import java.time.LocalDate;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.volatility.caplet.CapVolMarketData;
import net.finmath.marketdata.model.volatility.caplet.CapletVolBootstrapping;
import net.finmath.marketdata.model.volatility.caplet.smile.LinearSmileInterpolater;
import net.finmath.marketdata.model.volatility.caplet.smile.SmileInterpolationExtrapolationMethod;
import net.finmath.marketdata.products.Swap;
import net.finmath.rootfinder.NewtonsMethod;
import net.finmath.time.Schedule;
import net.finmath.time.ScheduleGenerator;
import net.finmath.time.ScheduleGenerator.DaycountConvention;
import net.finmath.time.ScheduleGenerator.Frequency;
import net.finmath.time.ScheduleGenerator.ShortPeriodConvention;
import net.finmath.time.businessdaycalendar.BusinessdayCalendar.DateRollConvention;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingTARGETHolidays;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingWeekends;

/**
 * This class implements a correlation provider based on iCap market data.
 * The weekly iCap delivery consists of 3M and 6M caplet volatilities.
 * Using those and the formulas from the Schlenkrich paper we can calculate the correlation
 * between forward rates of the form F(0,0.5*i,0.5*i+0.3) and F(0,0.5*i+0.25,0.5*(i+1)).
 * Those aren't enough though to convert tenors from anything else than 3M to 6M.
 *
 * @TODO Calibrate a parameterization as suggested by Schlenkrich given the calculated 3M correlations and use this parameterization to extract all missing correlations.
 *
 * @author Daniel Willhalm
 */
public class CorrelationProviderTenorBasis implements CorrelationProvider {

	private final CapVolMarketData iCap3MCapVolMarketData;
	private final CapVolMarketData iCap6MCapVolMarketData;

	private CapletVolBootstrapping iCap3MCapletVolBootrapper;
	private CapletVolBootstrapping iCap6MCapletVolBootrapper;

	private double[][] iCap3MCapletVolMatrix;
	private double[][] iCap6MCapletVolMatrix;
	private double[][] correlationMatrix1M;
	private double[][] correlationMatrix3M;
	private double[][] correlationMatrix6M;

	public CorrelationProviderTenorBasis(final CapVolMarketData iCap3MCapVolMarketData, final CapVolMarketData iCap6MCapVolMarketData) {
		this.iCap3MCapVolMarketData = iCap3MCapVolMarketData;
		this.iCap6MCapVolMarketData = iCap6MCapVolMarketData;
	}

	public double get6MCorrelation(final double firstForwardFixingTimeVectorInYears, final double secondForwardFixingTimeVectorInYears, final AnalyticModel analyticModel) throws CalculationException {
		//todo: Kalibriere auf Basis der 3M Correlationsdaten die zur Verf�gung stehen eine Parametrisierung der Korrelationen wie sie im Schlenkrich zu finden ist.
		return 0.0;
	}


	public double get3MCorrelation(final double firstForwardFixingTimeVectorInYears, final double secondForwardFixingTimeVectorInYears, final AnalyticModel analyticModel) throws CalculationException {
		//Auf Basis der 3M und 6M Icap daten wird mit der Formel aus Schlenkrich die Korrelation bestimmt. Achtung f�r Umrechung auf 12M br�uchte man mehr 3M Korrelationen die man sich �ber die Parametrisierung holen m�sste.
		final double[] capletFixingTimeVectorInYears = new double[iCap3MCapVolMarketData.getMaxExpiryInMonths()/iCap3MCapVolMarketData.getUnderlyingTenorInMonths()-1];
		for (int i = 0; i < capletFixingTimeVectorInYears.length; i++) {
			capletFixingTimeVectorInYears[i] = (i+1)*iCap3MCapVolMarketData.getUnderlyingTenorInMonths()/12.0;
		}
		int rowIndex = 0;
		int columnIndex = 0;
		double distanceToFixingTimeRow = Double.MAX_VALUE;
		double distanceToFixingTimeColumn = Double.MAX_VALUE;
		for (int i = 0; i < capletFixingTimeVectorInYears.length; i++) {
			if (Math.abs(firstForwardFixingTimeVectorInYears - capletFixingTimeVectorInYears[i]) < distanceToFixingTimeRow) {
				rowIndex = i;
				distanceToFixingTimeRow = Math.abs(firstForwardFixingTimeVectorInYears - capletFixingTimeVectorInYears[i]);
			}
			if (Math.abs(secondForwardFixingTimeVectorInYears - capletFixingTimeVectorInYears[i]) < distanceToFixingTimeColumn) {
				columnIndex = i;
				distanceToFixingTimeColumn = Math.abs(secondForwardFixingTimeVectorInYears - capletFixingTimeVectorInYears[i]);
			}
		}

		//lazy initialization
		if (correlationMatrix3M != null) {
			return correlationMatrix3M[rowIndex][columnIndex];
		}

		iCap3MCapletVolBootrapper = new CapletVolBootstrapping(iCap3MCapVolMarketData, analyticModel);
		iCap6MCapletVolBootrapper = new CapletVolBootstrapping(iCap6MCapVolMarketData, analyticModel);
		if(iCap3MCapletVolMatrix == null || iCap6MCapletVolMatrix == null) {
			iCap3MCapletVolMatrix = iCap3MCapletVolBootrapper.getCapletVolMatrix();
			iCap6MCapletVolMatrix = iCap6MCapletVolBootrapper.getCapletVolMatrix();
		}

		correlationMatrix3M = new double[iCap3MCapVolMarketData.getMaxExpiryInMonths()/iCap3MCapVolMarketData.getUnderlyingTenorInMonths()-1][iCap3MCapVolMarketData.getMaxExpiryInMonths()/iCap3MCapVolMarketData.getUnderlyingTenorInMonths()-1];

		for (int i = 0; i < correlationMatrix3M.length; i++) {
			correlationMatrix3M[i][i] = 1;
		}

		//todo: give interpolation method as an argument
		final SmileInterpolationExtrapolationMethod smileInterExtrapolater3M = new LinearSmileInterpolater(iCap3MCapletVolMatrix, iCap3MCapVolMarketData.getStrikeVector());
		final SmileInterpolationExtrapolationMethod smileInterExtrapolater6M = new LinearSmileInterpolater(iCap6MCapletVolMatrix, iCap6MCapVolMarketData.getStrikeVector());
		final double[][] K = new double[correlationMatrix3M.length/2-(1-correlationMatrix3M.length%2)][2];
		final double[][] nu = new double[correlationMatrix3M.length/2-(1-correlationMatrix3M.length%2)][2];
		final double[] sumNu = new double[correlationMatrix3M.length/2-(1-correlationMatrix3M.length%2)];

		for (int i = 0; i < correlationMatrix3M.length/2-(1-correlationMatrix3M.length%2); i++) {
			final LocalDate localDate = iCap3MCapletVolBootrapper.getDiscountCurve().getReferenceDate();
			final Schedule schedule3M = ScheduleGenerator.createScheduleFromConventions(
					localDate,
					localDate.plusMonths(2*(i+1)*iCap3MCapVolMarketData.getUnderlyingTenorInMonths()),
					localDate.plusMonths(2*(i+2)*iCap3MCapVolMarketData.getUnderlyingTenorInMonths()),
					Frequency.QUARTERLY, DaycountConvention.ACT_365, ShortPeriodConvention.FIRST, DateRollConvention.MODIFIED_FOLLOWING,
					new BusinessdayCalendarExcludingTARGETHolidays(new BusinessdayCalendarExcludingWeekends()), -2, 0);
			final Schedule schedule6M = ScheduleGenerator.createScheduleFromConventions(
					localDate,
					localDate.plusMonths(2*(i+1)*iCap3MCapVolMarketData.getUnderlyingTenorInMonths()),
					localDate.plusMonths(2*(i+2)*iCap3MCapVolMarketData.getUnderlyingTenorInMonths()),
					Frequency.SEMIANNUAL, DaycountConvention.ACT_365, ShortPeriodConvention.FIRST, DateRollConvention.MODIFIED_FOLLOWING,
					new BusinessdayCalendarExcludingTARGETHolidays(new BusinessdayCalendarExcludingWeekends()), -2, 0);
			//tests have shown that the correlations that we get close to the strike are the most meaningful
			//The way the strike is approximated isn't perfect. That's why the first correlation in the test class is > 1
			final double strikeATM = Swap.getForwardSwapRate(schedule3M, schedule3M, iCap3MCapletVolBootrapper.getForwardCurve(), iCap3MCapletVolBootrapper.getParsedModel());
			final double[] shortTenorTau = new double[nu[0].length];
			for (int k = 0; k < shortTenorTau.length; k++) {
				shortTenorTau[k] = schedule3M.getPeriodLength(k);
			}
			final double longTenorTau = schedule6M.getPeriodLength(0);

			for (int k = 0; k < nu[0].length; k++) {
				nu[i][k] = shortTenorTau[k]
						* (1.0 + longTenorTau * iCap6MCapletVolBootrapper.getForwardCurve().getForward(
								iCap6MCapletVolBootrapper.getParsedModel(), capletFixingTimeVectorInYears[i * 2 + 1]))
						/ (longTenorTau * (1.0 + shortTenorTau[k] * iCap3MCapletVolBootrapper.getForwardCurve()
								.getForward(iCap3MCapletVolBootrapper.getParsedModel(),
										capletFixingTimeVectorInYears[i * 2 + 1] + schedule3M.getPeriodStart(k))));
				sumNu[i] += nu[i][k];
			}

			for (int k = 0; k < K[0].length; k++) {
				K[i][k] = (strikeATM - (iCap6MCapletVolBootrapper.getForwardCurve().getForward(
						iCap6MCapletVolBootrapper.getParsedModel(), capletFixingTimeVectorInYears[i * 2 + 1])
						- sumNu[i] * iCap3MCapletVolBootrapper.getForwardCurve()
						.getForward(iCap3MCapletVolBootrapper.getParsedModel(),
								capletFixingTimeVectorInYears[i * 2 + 1]
										+ k * iCap3MCapVolMarketData.getUnderlyingTenorInMonths() / 12.0)))
						/ sumNu[i];
			}

			//newtons method is not really necessary since we search for the root of a linear function but due to convenience we use one iteration of newtons method to find it
			final double guess = 1.0;
			final NewtonsMethod newtonsMethod = new NewtonsMethod(guess);
			double derivativeSumToMinimize = 0.0;
			double secondDerivativeSumToMinimize = 0.0;
			double rightHandSideSum = 0.0;
			double derivativeRightHandSideSum = 0.0;
			for (int k1 = 0; k1 < 2; k1++) {
				for (int k2 = 0; k2 < 2; k2++) {
					if (k1 == k2) {
						rightHandSideSum += nu[i][k1]*nu[i][k2]*smileInterExtrapolater3M.calculateInterpolatedExtrapolatedSmileVolatility(K[i][k1], i*2+1+k1)*smileInterExtrapolater3M.calculateInterpolatedExtrapolatedSmileVolatility(K[i][k2], i*2+1+k2);
					}
					else {
						rightHandSideSum += nu[i][k1]*nu[i][k2]*guess*smileInterExtrapolater3M.calculateInterpolatedExtrapolatedSmileVolatility(K[i][k1], i*2+1+k1)*smileInterExtrapolater3M.calculateInterpolatedExtrapolatedSmileVolatility(K[i][k2], i*2+1+k2);
						derivativeRightHandSideSum += nu[i][k1]*nu[i][k2]*smileInterExtrapolater3M.calculateInterpolatedExtrapolatedSmileVolatility(K[i][k1], i*2+1+k1)*smileInterExtrapolater3M.calculateInterpolatedExtrapolatedSmileVolatility(K[i][k2], i*2+1+k2);
					}
				}
			}
			derivativeSumToMinimize += 2*(smileInterExtrapolater6M.calculateInterpolatedExtrapolatedSmileVolatility(strikeATM, i)*smileInterExtrapolater6M.calculateInterpolatedExtrapolatedSmileVolatility(strikeATM, i)-rightHandSideSum)*(-derivativeRightHandSideSum);
			secondDerivativeSumToMinimize += 2*derivativeRightHandSideSum*derivativeRightHandSideSum;

			newtonsMethod.setValueAndDerivative(derivativeSumToMinimize, secondDerivativeSumToMinimize);
			correlationMatrix3M[i*2+1][i*2+2] = newtonsMethod.getNextPoint();
			correlationMatrix3M[i*2+2][i*2+1] = newtonsMethod.getNextPoint();
		}

		return correlationMatrix3M[rowIndex][columnIndex];
	}

	double get1MCorrelation(final double firstForwardFixingTimeVectorInYears, final double secondForwardFixingTimeVectorInYears, final AnalyticModel analyticModel) {
		return 0;
	}

	public double[][] getiCap3MCapletVolMatrix() {
		return iCap3MCapletVolMatrix;
	}

	public double[][] getiCap6MCapletVolMatrix() {
		return iCap6MCapletVolMatrix;
	}

	public double[][] getCorrelationMatrix3M() {
		return correlationMatrix3M;
	}

	public double[][] getCorrelationMatrix6M() {
		return correlationMatrix6M;
	}

	public CapletVolBootstrapping getICap3MCapletVolBootrapper() {
		return iCap3MCapletVolBootrapper;
	}

	public CapletVolBootstrapping getICap6MCapletVolBootrapper() {
		return iCap6MCapletVolBootrapper;
	}

	@Override
	public double getCorrelation(final int oldTenor, final double firstForwardFixingTimeVectorInYears, final double secondForwardFixingTimeVectorInYears, final AnalyticModel analyticModel, final String indexForDiscount) throws CalculationException {
		if(oldTenor == 6) {
			return get6MCorrelation(firstForwardFixingTimeVectorInYears, secondForwardFixingTimeVectorInYears, analyticModel);
		}
		if(oldTenor == 3) {
			return get3MCorrelation(firstForwardFixingTimeVectorInYears, secondForwardFixingTimeVectorInYears, analyticModel);
		}
		if(oldTenor == 1) {
			return get1MCorrelation(firstForwardFixingTimeVectorInYears, secondForwardFixingTimeVectorInYears, analyticModel);
		}
		throw new IllegalArgumentException("Wrong Tenor for the iCap correlation provider");
	}
}
