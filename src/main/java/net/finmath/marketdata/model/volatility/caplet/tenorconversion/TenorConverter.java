package net.finmath.marketdata.model.volatility.caplet.tenorconversion;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.model.volatilities.VolatilitySurface.QuotingConvention;
import net.finmath.marketdata.model.volatility.caplet.CapTenorStructure;
import net.finmath.marketdata.model.volatility.caplet.CapletVolatilitySurface;

/**
 * This class implements a caplet volatility tenor converter. Given a correlationprovider,
 * the current caplet vol data and the new tenor it converts the volatilities to the new tenor using
 * the method suggested in the paper by Schlenkrich.
 *
 * @author Daniel Willhalm
 */
public class TenorConverter {
	private final int currentTenorInMonths;
	private final int newTenorInMonths;
	private final double[] capletFixingTimeVectorInYears;
	private final double[] strikeVector;
	private final double[][] capletVolatilities;
	private final CapletVolatilitySurface capletVolatilitySurface;
	private final CorrelationProvider correlationProvider;

	private final DiscountCurve discountCurve, discountForForwardCurveOldTenor, discountForForwardCurveNewTenor;
	private final ForwardCurve forwardCurveOldTenor;
	private final ForwardCurve forwardCurveNewTenor;

	private final String indexForDiscount, indexOldTenor, indexNewTenor;

	private transient AnalyticModel analyticModel;

	/**
	 * The constructor of the tenor conversion class
	 *
	 * @param correlationProvider The correlation provider.
	 * @param currentTenorInMonths The tenor of the current caplet volatilities.
	 * @param newTenorInMonths The target tenor.
	 * @param capletFixingTimeVectorInYears The fixing times of the caplets.
	 * @param strikeVector The strikes of the caplets.
	 * @param capletVolatilities The caplet volatilities.
	 * @param capTenorStructure Enum determining the currency.
	 * @param analyticModel2 The analytic model containing the curves.
	 * @param indexForDiscount Index of the discount curve.
	 * @param indexOldTenor Index of the old forward curve.
	 * @param indexNewTenor Index of the new forward curve.
	 */
	public TenorConverter(final CorrelationProvider correlationProvider, final int currentTenorInMonths,
			final int newTenorInMonths, final double[] capletFixingTimeVectorInYears, final double[] strikeVector,
			final double[][] capletVolatilities, final CapTenorStructure capTenorStructure,
			final AnalyticModel analyticModel2, final String indexForDiscount,
			final String indexOldTenor, final String indexNewTenor) {
		this.currentTenorInMonths = currentTenorInMonths;
		this.newTenorInMonths = newTenorInMonths;
		this.capletFixingTimeVectorInYears = capletFixingTimeVectorInYears;
		this.strikeVector = strikeVector;
		this.capletVolatilities = capletVolatilities;
		this.correlationProvider = correlationProvider;

		this.indexForDiscount = indexForDiscount;
		this.indexOldTenor = indexOldTenor;
		this.indexNewTenor = indexNewTenor;

		String currency = null;
		switch (capTenorStructure) {
		case EUR:
			currency = "EUR";
			break;
		case USD:
			currency = "USD";
			break;
		default:
			throw new IllegalArgumentException("Unknown currency " + capTenorStructure + ".");
		}

		this.analyticModel = analyticModel2;
		discountCurve = analyticModel2.getDiscountCurve(currency + "_" + indexForDiscount);
		discountForForwardCurveOldTenor = null;
		discountForForwardCurveNewTenor = null;
		forwardCurveOldTenor = this.analyticModel.getForwardCurve("Forward_" + currency + "_" + indexNewTenor);
		forwardCurveNewTenor = this.analyticModel.getForwardCurve("Forward_" + currency + "_" + indexNewTenor);
		capletVolatilitySurface = new CapletVolatilitySurface("Tenor " + this.currentTenorInMonths + " Months", discountCurve.getReferenceDate(), this.capletVolatilities, this.capletFixingTimeVectorInYears, this.strikeVector, forwardCurveOldTenor, QuotingConvention.VOLATILITYLOGNORMAL, discountCurve);
	}

	/**
	 * Method that converts the current tenor caplet volatilities to the new tenor.
	 *
	 * @return Caplet volatility matrix adapted to the new tenor.
	 * @throws CalculationException Thrown if conversion fails arithmetically.
	 */
	public double[][] convertTenor() throws CalculationException {
		//based on paper by Schlenkrich
		if (currentTenorInMonths == newTenorInMonths) {
			throw new CalculationException("old and new tenor collide.");
		}
		double[][] newCapletVolatilities = null;
		//long tenor conversion
		if (currentTenorInMonths < newTenorInMonths) {
			if (newTenorInMonths % currentTenorInMonths != 0) {
				throw new CalculationException("The new tenor has to be divisible by the old tenor or the other way round.");
			}
			final int n = newTenorInMonths/currentTenorInMonths;
			final double[] newCapletFixingTimeVectorInYears = new double[(capletFixingTimeVectorInYears.length+1)/n-1];
			for (int i = 0; i < newCapletFixingTimeVectorInYears.length; i++) {
				newCapletFixingTimeVectorInYears[i] = capletFixingTimeVectorInYears[n-1+n*i];
			}
			newCapletVolatilities = new double[(capletFixingTimeVectorInYears.length+1)/n-1][strikeVector.length];

			for (int j = 0; j < strikeVector.length; j++) {
				for (int i = 0; i < newCapletFixingTimeVectorInYears.length; i++) {
					final double[] nu = new double[n];
					double sumNu = 0.0;
					final double[] K = new double[n];
					for (int k = 0; k < n; k++) {
						nu[k] = currentTenorInMonths*(1.0 + newTenorInMonths/12.0*forwardCurveNewTenor.getForward(analyticModel, newCapletFixingTimeVectorInYears[i]))/(newTenorInMonths*(1.0 + currentTenorInMonths/12.0*forwardCurveOldTenor.getForward(analyticModel, newCapletFixingTimeVectorInYears[i]+k*currentTenorInMonths/12.0)));
						sumNu += nu[k];
					}
					for (int k = 0; k < n; k++) {
						K[k] = (strikeVector[j]-(forwardCurveNewTenor.getForward(analyticModel, newCapletFixingTimeVectorInYears[i])-sumNu*forwardCurveOldTenor.getForward(analyticModel, newCapletFixingTimeVectorInYears[i]+k*currentTenorInMonths/12.0)))/sumNu;
					}
					for (int k1 = 0; k1 < n; k1++) {
						for (int k2 = 0; k2 < n; k2++) {
							newCapletVolatilities[i][j] += nu[k1] * nu[k2]
									* correlationProvider.getCorrelation(currentTenorInMonths,
											newCapletFixingTimeVectorInYears[i] + k1 * currentTenorInMonths / 12.0,
											newCapletFixingTimeVectorInYears[i] + k2 * currentTenorInMonths / 12.0,
											analyticModel, indexForDiscount)
									* capletVolatilitySurface.getValue(analyticModel,
											newCapletFixingTimeVectorInYears[i] + k1 * currentTenorInMonths / 12.0,
											K[k1], QuotingConvention.VOLATILITYLOGNORMAL)
									* capletVolatilitySurface.getValue(analyticModel,
											newCapletFixingTimeVectorInYears[i] + k2 * currentTenorInMonths / 12.0,
											K[k2], QuotingConvention.VOLATILITYLOGNORMAL);
						}
					}
					newCapletVolatilities[i][j] = Math.sqrt(newCapletVolatilities[i][j]);
				}
			}
		}
		//short tenor conversion
		if (currentTenorInMonths > newTenorInMonths) {
			if (currentTenorInMonths % newTenorInMonths != 0) {
				throw new CalculationException("The new tenor has to be divisible by the old tenor or the other way round.");
			}
			final int n = currentTenorInMonths/newTenorInMonths;
			final double[] newCapletFixingTimeVectorInYears = new double[(capletFixingTimeVectorInYears.length+1)*n-1];
			final double[] firstLongCapletFixingTimeVectorInYears = new double[(capletFixingTimeVectorInYears.length+1)*n-1];
			for (int i = 0; i < newCapletFixingTimeVectorInYears.length; i+=n) {
				newCapletFixingTimeVectorInYears[i] = capletFixingTimeVectorInYears[0]/n*(i+1);
				if (i < n-1) {
					firstLongCapletFixingTimeVectorInYears[i] = capletFixingTimeVectorInYears[0];
				}
				else {
					firstLongCapletFixingTimeVectorInYears[i] = capletFixingTimeVectorInYears[(i+1)/n-1];
				}
			}
			newCapletVolatilities = new double[(capletFixingTimeVectorInYears.length+1)*n-1][strikeVector.length];
			for (int j = 0; j < strikeVector.length; j++) {
				for (int i = 0; i < newCapletFixingTimeVectorInYears.length; i++) {
					final double[] nu = new double[n];
					double sumNuNuRho = 0.0;
					for (int k = 0; k < n; k++) {
						nu[k] = newTenorInMonths*(1.0 + currentTenorInMonths/12.0*forwardCurveNewTenor.getForward(analyticModel, firstLongCapletFixingTimeVectorInYears[i]))/(newTenorInMonths*(1.0 + currentTenorInMonths/12.0*forwardCurveOldTenor.getForward(analyticModel, firstLongCapletFixingTimeVectorInYears[i]+k*currentTenorInMonths/12.0)));
					}
					for (int k1 = 0; k1 < n; k1++) {
						for (int k2 = 0; k2 < n; k2++) {
							sumNuNuRho += nu[k1]*nu[k2]*correlationProvider.getCorrelation(newTenorInMonths, firstLongCapletFixingTimeVectorInYears[i]+k1*newTenorInMonths/12.0, firstLongCapletFixingTimeVectorInYears[i]+k2*newTenorInMonths/12.0, analyticModel, indexForDiscount);
						}
					}
					if (i < n-1) {
						newCapletVolatilities[i][j] = capletVolatilitySurface.getValue(analyticModel, firstLongCapletFixingTimeVectorInYears[0], strikeVector[j], QuotingConvention.VOLATILITYLOGNORMAL)/Math.sqrt(sumNuNuRho);
					}
					else {
						newCapletVolatilities[i][j] = capletVolatilitySurface.getValue(analyticModel, firstLongCapletFixingTimeVectorInYears[i], strikeVector[j], QuotingConvention.VOLATILITYLOGNORMAL)/Math.sqrt(sumNuNuRho);
					}
				}
			}
		}
		return newCapletVolatilities;
	}
}
