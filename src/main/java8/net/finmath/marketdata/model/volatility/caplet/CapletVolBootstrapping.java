package net.finmath.marketdata.model.volatility.caplet;

import java.time.LocalDate;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.model.volatilities.VolatilitySurface;
import net.finmath.marketdata.model.volatilities.VolatilitySurface.QuotingConvention;
import net.finmath.marketdata.model.volatility.caplet.tenorconversion.CorrelationProvider;
import net.finmath.marketdata.model.volatility.caplet.tenorconversion.TenorConverter;
import net.finmath.rootfinder.BisectionSearch;
import net.finmath.time.Schedule;
import net.finmath.time.ScheduleGenerator;
import net.finmath.time.ScheduleGenerator.DaycountConvention;
import net.finmath.time.ScheduleGenerator.Frequency;
import net.finmath.time.ScheduleGenerator.ShortPeriodConvention;
import net.finmath.time.businessdaycalendar.BusinessdayCalendar;
import net.finmath.time.businessdaycalendar.BusinessdayCalendar.DateRollConvention;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingTARGETHolidays;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingWeekends;

/**
 * This class implements a caplet volatility bootstrapper. Given an object of type CapVolMarketData
 * which contains market volatilities for caps it calculates the caplet volatilities.
 *
 * @author Daniel Willhalm (initial version)
 * @author Christian Fries (review and fixes)
 */
public class CapletVolBootstrapping {

	private final CapVolMarketData capVolMarketData;
	private final DiscountCurve discountCurve;
	private final ForwardCurve forwardCurve;

	private double[][] capletVolMatrix;
	private double[] capletFixingTimeVectorInYears;
	private final CorrelationProvider correlationProvider;

	private transient AnalyticModel analyticModel;

	/**
	 * The constructor of the caplet bootstrapping class.
	 *
	 * @param correlationProvider The correlationProvider which is necessary only if the underlying cap data changes its tenor (common for EUR cap data).
	 * @param capVolMarketData The market data for the caps.
	 * @param parsedModel The analytic model for forward and discount curves.
	 */
	public CapletVolBootstrapping(final CorrelationProvider correlationProvider, final CapVolMarketData capVolMarketData, final AnalyticModel parsedModel) {
		super();
		this.capVolMarketData = capVolMarketData;
		this.correlationProvider = correlationProvider;
		this.analyticModel = parsedModel;
		String currency = null;
		switch (capVolMarketData.getCapTenorStructure()) {
		case EUR:
			currency = "EUR";
			break;

		case USD:
			currency = "USD";
			break;
		default:
			throw new IllegalArgumentException("Unknown currency " + capVolMarketData.getCapTenorStructure() + ".");
		}
		forwardCurve = parsedModel.getForwardCurve("Forward_" + currency + "_" + capVolMarketData.getIndex());
		discountCurve = parsedModel.getDiscountCurve(currency + "_" + capVolMarketData.getDiscountIndex());
	}

	/**
	 * Overloaded constructor of the caplet bootstrapping class if a correlation provider isn't necessary.
	 *
	 * @param capVolMarketData The market data for the caps.
	 * @param parsedModel The analytic model for forward and discount curves.
	 */
	public CapletVolBootstrapping(final CapVolMarketData capVolMarketData, final AnalyticModel parsedModel) {
		this(null, capVolMarketData, parsedModel);
	}

	/**
	 * Method that bootstraps the caplet volatilities from the cap volatility data.
	 * It is assumed that the caplet volatilities between available cap volatility data is constant.
	 * The bisection method is used as a root finder to align the cap price and the sum of caplets price.
	 *
	 * @return The bootstrapped caplet volatility matrix.
	 * @throws CalculationException Thrown if calculation fails arithmetically.
	 */
	public double[][] getCapletVolMatrix() throws CalculationException {
		//initialize the caplet volatility matrix. It has 2 * highest cap expiry - 1 entries since the first caplet has value 0
		capletVolMatrix = new double[capVolMarketData.getMaxExpiryInMonths()/capVolMarketData.getUnderlyingTenorInMonths()-1][capVolMarketData.getNumberOfStrikes()];
		capletFixingTimeVectorInYears = new double[capVolMarketData.getMaxExpiryInMonths()/capVolMarketData.getUnderlyingTenorInMonths()-1];
		for (int i = 0; i < capletFixingTimeVectorInYears.length; i++) {
			capletFixingTimeVectorInYears[i] = (i+1)*capVolMarketData.getUnderlyingTenorInMonths()/12.0;
		}

		Frequency frequency = null;
		switch (capVolMarketData.getUnderlyingTenorInMonths()) {
		case 1:
			frequency = Frequency.MONTHLY;
			break;
		case 3:
			frequency = Frequency.QUARTERLY;
			break;
		case 6:
			frequency = Frequency.SEMIANNUAL;
			break;
		case 12:
			frequency = Frequency.ANNUAL;
			break;
		default:
			throw new IllegalArgumentException("Unknown tenor " + capVolMarketData.getUnderlyingTenorInMonths() + ".");
		}

		//Conversion if underlying tenors are different; not tested yet
		//analytic model has to contain two forward curves
		if (capVolMarketData.getUnderlyingTenorInMonths() != capVolMarketData.getUnderlyingTenorInMonthsBeforeChange()) {

			int numberOfExpiriesBeforeChange = 0;
			while(capVolMarketData.getExpiryInMonths(numberOfExpiriesBeforeChange) <= capVolMarketData.getTenorChangeTimeInMonths()) {
				numberOfExpiriesBeforeChange++;
			}

			final int[] expiryVectorInMonthsBeforeChange = new int[numberOfExpiriesBeforeChange];
			for (int i = 0; i < numberOfExpiriesBeforeChange; i++) {
				expiryVectorInMonthsBeforeChange[i] = capVolMarketData.getExpiryInMonths(i);
			}
			final double[][] capVolMatrixBeforeChange = new double[numberOfExpiriesBeforeChange][capVolMarketData.getNumberOfStrikes()];
			for (int j = 0; j < capVolMarketData.getNumberOfStrikes(); j++) {
				for (int i = 0; i < numberOfExpiriesBeforeChange; i++) {
					capVolMatrixBeforeChange[i][j] = capVolMarketData.getVolMatrix()[i][j];
				}
			}
			final double[] capletFixingTimeVectorInYearsBeforeChange = new double[capVolMarketData.getMaxExpiryInMonths()/capVolMarketData.getUnderlyingTenorInMonths()-1];
			for (int i = 0; i < capletFixingTimeVectorInYearsBeforeChange.length; i++) {
				capletFixingTimeVectorInYearsBeforeChange[i] = (i+1)*capVolMarketData.getUnderlyingTenorInMonthsBeforeChange()/12.0;
			}

			final CapVolMarketData capVolMarketDataBeforeChange = new CapVolMarketData(
					capVolMarketData.getIndexBeforeChange(),
					capVolMarketData.getDiscountIndex(),
					capVolMarketData.getCapTenorStructure(),
					expiryVectorInMonthsBeforeChange, capVolMarketData.getStrikeVector(),
					capVolMatrixBeforeChange, capVolMarketData.getShift(),
					capVolMarketData.getUnderlyingTenorInMonthsBeforeChange());

			final CapletVolBootstrapping capletVolBootstrapperBeforeChange = new CapletVolBootstrapping(capVolMarketDataBeforeChange, analyticModel);

			final TenorConverter tenorConverterBeforeChange = new TenorConverter(
					correlationProvider,
					capVolMarketDataBeforeChange.getUnderlyingTenorInMonthsBeforeChange(),
					capVolMarketData.getUnderlyingTenorInMonths(),
					capletFixingTimeVectorInYearsBeforeChange,
					capVolMarketDataBeforeChange.getStrikeVector(),
					capletVolBootstrapperBeforeChange.getCapletVolMatrix(),
					capVolMarketDataBeforeChange.getCapTenorStructure(),
					analyticModel, capVolMarketData.getDiscountIndex(),
					capVolMarketDataBeforeChange.getIndex(),
					capVolMarketData.getIndex());

			final double[][] capVolMatrixBeforeChangeNewTenor = capletVolBootstrapperBeforeChange.calculateCapVolsFromCapletVols(tenorConverterBeforeChange.convertTenor());

			for (int j = 0; j < capVolMarketData.getNumberOfStrikes(); j++) {
				for (int i = 0; i < numberOfExpiriesBeforeChange; i++) {
					capVolMarketData.setCapVolMatrixEntry(i, j, capVolMatrixBeforeChangeNewTenor[i][j]);
				}
			}
		}

		//loop over the number of strikes of the market cap volatilities
		for (int j = 0; j < capVolMarketData.getNumberOfStrikes(); j++) {
			int lastExpiryInMonths = 0;
			int currentExpiryInMonths = capVolMarketData.getExpiryInMonths(0);
			int lastCaplet = 0;
			boolean isFirstCap = true;

			//loop over the number of caplets
			for (int i = 0; i < capVolMarketData.getMaxExpiryInMonths()/capVolMarketData.getUnderlyingTenorInMonths(); i++) {
				if (currentExpiryInMonths != (i+1)*capVolMarketData.getUnderlyingTenorInMonths()) {
					continue;
				}
				if (isFirstCap) {
					for (int k = 0; k < i; k++) {
						capletVolMatrix[k][j] = capVolMarketData.getCapVolData(0, j);
						//next line due to numerical errors from strike interpolation
						if (j+1 < capVolMarketData.getNumberOfStrikes()) {
							capletVolMatrix[k][j+1] = capletVolMatrix[k][j];
						}
					}
					isFirstCap = false;
				}
				else {
					//rootfinder
					//bisection method
					final int currentExpiryRow = capVolMarketData.getRowIndex(currentExpiryInMonths);
					final BusinessdayCalendar businessdayCalendar = new BusinessdayCalendarExcludingTARGETHolidays(new BusinessdayCalendarExcludingWeekends());
					final LocalDate localDate = discountCurve.getReferenceDate();
					final LocalDate startDate = businessdayCalendar.getRolledDate(localDate, 2);
					//Quoting convention k�nnte auch �bergeben werden
					VolatilitySurface capletVolatilities = new CapletVolatilitySurface("Cap volatility surface", localDate, capVolMarketData.getCapVolData(currentExpiryRow, j), capletFixingTimeVectorInYears, capVolMarketData.getStrikeVector(), forwardCurve, QuotingConvention.VOLATILITYLOGNORMAL, discountCurve);
					//Schedule k�nnte auch �bergeben werden
					final Schedule schedule = ScheduleGenerator.createScheduleFromConventions(localDate, startDate, localDate.plusMonths(currentExpiryInMonths), frequency, DaycountConvention.ACT_365, ShortPeriodConvention.FIRST, DateRollConvention.MODIFIED_FOLLOWING, businessdayCalendar, -2, 0);
					final CapShiftedVol cap = new CapShiftedVol(schedule, forwardCurve.getName(), capVolMarketData.getStrike(j), false, discountCurve.getName(), capletVolatilities.getName(), capVolMarketData.getShift());
					final double capPrice = cap.getValueAsPrice(0, analyticModel.addVolatilitySurfaces(capletVolatilities));
					double sumCapletPrices = Double.MAX_VALUE;
					double leftPoint = capVolMarketData.getCapVolData(currentExpiryRow, j)*2;
					while (capPrice - sumCapletPrices <= 0) {
						leftPoint /= 1.5;
						for (int l = i; l > lastCaplet; l--) {
							capletVolMatrix[l-1][j] = leftPoint;
							if (j+1 < capVolMarketData.getNumberOfStrikes()) {
								capletVolMatrix[l-1][j+1] = capletVolMatrix[l-1][j];
							}
						}
						capletVolatilities = new CapletVolatilitySurface("Cap volatility surface", localDate, capletVolMatrix, capletFixingTimeVectorInYears, capVolMarketData.getStrikeVector(), forwardCurve, QuotingConvention.VOLATILITYLOGNORMAL, discountCurve);
						sumCapletPrices = cap.getValueAsPrice(0, analyticModel.addVolatilitySurfaces(capletVolatilities));
						if(capPrice - sumCapletPrices == 0) {
							break;
						}
					}
					final double leftValue = capPrice - sumCapletPrices;
					double rightPoint = leftPoint;
					while (capPrice - sumCapletPrices >= 0) {
						rightPoint += 0.1;
						for (int l = i; l > lastCaplet; l--) {
							capletVolMatrix[l-1][j] = rightPoint;
							if (j+1 < capVolMarketData.getNumberOfStrikes()) {
								capletVolMatrix[l-1][j+1] = capletVolMatrix[l-1][j];
							}
						}
						capletVolatilities = new CapletVolatilitySurface("Cap volatility surface", localDate, capletVolMatrix, capletFixingTimeVectorInYears, capVolMarketData.getStrikeVector(), forwardCurve, QuotingConvention.VOLATILITYLOGNORMAL, discountCurve);
						sumCapletPrices = cap.getValueAsPrice(0, analyticModel.addVolatilitySurfaces(capletVolatilities));
						if(capPrice - sumCapletPrices == 0) {
							break;
						}
					}
					final double rightValue = capPrice - sumCapletPrices;
					final BisectionSearch bisectionSearch = new BisectionSearch(leftPoint, rightPoint);
					bisectionSearch.setValue(leftValue);
					bisectionSearch.setValue(rightValue);
					while (bisectionSearch.isDone() != true) {
						for (int l = i; l > lastCaplet; l--) {
							capletVolMatrix[l-1][j] = bisectionSearch.getNextPoint();
							if (j+1 < capVolMarketData.getNumberOfStrikes()) {
								capletVolMatrix[l-1][j+1] = capletVolMatrix[l-1][j];
							}
						}
						capletVolatilities = new CapletVolatilitySurface("Cap volatility surface", localDate, capletVolMatrix, capletFixingTimeVectorInYears, capVolMarketData.getStrikeVector(), forwardCurve, QuotingConvention.VOLATILITYLOGNORMAL, discountCurve);
						sumCapletPrices = cap.getValueAsPrice(0, analyticModel.addVolatilitySurfaces(capletVolatilities));
						bisectionSearch.setValue(capPrice - sumCapletPrices);
					}
					final double volaBestPoint = bisectionSearch.getBestPoint();

					for (int m = i; i - m < (currentExpiryInMonths-lastExpiryInMonths)/capVolMarketData.getUnderlyingTenorInMonths(); m--) {
						capletVolMatrix[m-1][j] = volaBestPoint;
					}
				}
				lastExpiryInMonths = currentExpiryInMonths;
				lastCaplet = i;
				if (capVolMarketData.getRowIndex(currentExpiryInMonths)+1 < capVolMarketData.getNumberOfExpiryDates()) {
					currentExpiryInMonths = capVolMarketData.getExpiryInMonths(capVolMarketData.getRowIndex(currentExpiryInMonths)+1);
				}
			}
		}
		return capletVolMatrix;
	}

	/**
	 * Method that implements the opposite direction. That means using a caplet volatility matrix
	 * the cap volatility matrix is calculated
	 *
	 * @param inputCapletVolMatrix The caplet volatility matrix.
	 * @return The cap volatility matrix.
	 */
	public double[][] calculateCapVolsFromCapletVols(final double[][] inputCapletVolMatrix) {
		final double[][] capVolMatrix = new double[capVolMarketData.getNumberOfExpiryDates()][capVolMarketData.getNumberOfStrikes()];
		final double[] capletFixingTimeVectorInYears = new double[capVolMarketData.getMaxExpiryInMonths()/capVolMarketData.getUnderlyingTenorInMonths()-1];
		for (int i = 0; i < capletFixingTimeVectorInYears.length; i++) {
			capletFixingTimeVectorInYears[i] = (i+1)*capVolMarketData.getUnderlyingTenorInMonths()/12.0;
		}

		Frequency frequency = null;
		switch (capVolMarketData.getUnderlyingTenorInMonths()) {
		case 1:
			frequency = Frequency.MONTHLY;
			break;
		case 3:
			frequency = Frequency.QUARTERLY;
			break;
		case 6:
			frequency = Frequency.SEMIANNUAL;
			break;
		case 12:
			frequency = Frequency.ANNUAL;
			break;
		default:
			throw new IllegalArgumentException("Unknown tenor " + capVolMarketData.getUnderlyingTenorInMonths() + ".");
		}
		//loop over the number of strikes of the market cap volatilities
		for (int j = 0; j < capVolMarketData.getNumberOfStrikes(); j++) {
			int lastExpiryInMonths = 0;
			int currentExpiryInMonths = capVolMarketData.getExpiryInMonths(0);
			boolean isFirstCaplet = true;
			//loop over the number of caps
			for (int i = 0; i < capVolMarketData.getNumberOfExpiryDates(); i++) {
				if (isFirstCaplet) {
					capVolMatrix[0][j] = inputCapletVolMatrix[0][j];
					//next line due to numerical errors from strike interpolation
					if (j+1 < capVolMarketData.getNumberOfStrikes()) {
						capVolMatrix[0][j+1] = capVolMatrix[0][j];
					}
					isFirstCaplet = false;
				}
				else {
					//rootfinder
					//bisection method
					final LocalDate localDate = discountCurve.getReferenceDate();
					VolatilitySurface capletVolatilities = new CapletVolatilitySurface("Cap volatility surface", localDate, inputCapletVolMatrix, capletFixingTimeVectorInYears, capVolMarketData.getStrikeVector(), forwardCurve, QuotingConvention.VOLATILITYLOGNORMAL, discountCurve);
					final Schedule schedule = ScheduleGenerator.createScheduleFromConventions(localDate, localDate, localDate.plusMonths(currentExpiryInMonths), frequency, DaycountConvention.ACT_365, ShortPeriodConvention.FIRST, DateRollConvention.MODIFIED_FOLLOWING, new BusinessdayCalendarExcludingTARGETHolidays(new BusinessdayCalendarExcludingWeekends()), -2, 0);
					final CapShiftedVol cap = new CapShiftedVol(schedule, forwardCurve.getName(), capVolMarketData.getStrike(j), false, discountCurve.getName(), capletVolatilities.getName(), capVolMarketData.getShift());
					final double sumCapletPrices = cap.getValueAsPrice(0, analyticModel.addVolatilitySurfaces(capletVolatilities));
					double capPrice = Double.MAX_VALUE;
					double leftPoint = inputCapletVolMatrix[lastExpiryInMonths/capVolMarketData.getUnderlyingTenorInMonths()-1][j];
					while (capPrice - sumCapletPrices >= 0) {
						leftPoint /= 1.5;
						capletVolatilities = new CapletVolatilitySurface("Cap volatility surface", localDate, leftPoint, capletFixingTimeVectorInYears, capVolMarketData.getStrikeVector(), forwardCurve, QuotingConvention.VOLATILITYLOGNORMAL, discountCurve);
						capPrice = cap.getValueAsPrice(0, analyticModel.addVolatilitySurfaces(capletVolatilities));
						if(capPrice - sumCapletPrices == 0) {
							break;
						}
					}
					final double leftValue = capPrice - sumCapletPrices;
					double rightPoint = leftPoint;
					while (capPrice - sumCapletPrices <= 0) {
						rightPoint += 0.1;
						capletVolatilities = new CapletVolatilitySurface("Cap volatility surface", localDate, rightPoint, capletFixingTimeVectorInYears, capVolMarketData.getStrikeVector(), forwardCurve, QuotingConvention.VOLATILITYLOGNORMAL, discountCurve);
						capPrice = cap.getValueAsPrice(0, analyticModel.addVolatilitySurfaces(capletVolatilities));
						if(capPrice - sumCapletPrices == 0) {
							break;
						}
					}
					final double rightValue = capPrice - sumCapletPrices;
					final BisectionSearch bisectionSearch = new BisectionSearch(leftPoint, rightPoint);
					bisectionSearch.setValue(leftValue);
					bisectionSearch.setValue(rightValue);
					while (bisectionSearch.isDone() != true) {
						capletVolatilities = new CapletVolatilitySurface("Cap volatility surface", localDate, bisectionSearch.getNextPoint(), capletFixingTimeVectorInYears, capVolMarketData.getStrikeVector(), forwardCurve, QuotingConvention.VOLATILITYLOGNORMAL, discountCurve);
						capPrice = cap.getValueAsPrice(0, analyticModel.addVolatilitySurfaces(capletVolatilities));
						bisectionSearch.setValue(capPrice - sumCapletPrices);
					}
					final double volaBestPoint = bisectionSearch.getBestPoint();
					capVolMatrix[i][j] = volaBestPoint;
				}
				lastExpiryInMonths = currentExpiryInMonths;
				if (capVolMarketData.getRowIndex(currentExpiryInMonths)+1 < capVolMarketData.getNumberOfExpiryDates()) {
					currentExpiryInMonths = capVolMarketData.getExpiryInMonths(capVolMarketData.getRowIndex(currentExpiryInMonths)+1);
				}
			}
		}
		return capVolMatrix;
	}

	public AnalyticModel getParsedModel() {
		return analyticModel;
	}

	public DiscountCurve getDiscountCurve() {
		return discountCurve;
	}

	public ForwardCurve getForwardCurve() {
		return forwardCurve;
	}

	public double[] getCapletFixingTimeVectorInYears() {
		return capletFixingTimeVectorInYears;
	}
}
