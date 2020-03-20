package net.finmath.marketdata.model.volatilities;

import java.time.LocalDate;
import java.util.HashMap;

import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.volatilities.VolatilitySurface.QuotingConvention;

/**
 * An option quote surface with the ability to query option quotes for different strikes and maturities.
 *
 * The surface is constructed as a collection of smiles. The choice of this dimension is convenient in view of calibration via FFT methods.
 *
 * This class does not perform any interpolation of market quotes. It merely represents a container of information.
 *
 * The class provides also the ability to perform the conversion among different quoting conventions and hence can be used both for a calibration on prices or implied volatilities.
 *
 * The class currently does not cover normal volatilities. Lognormal volatilities are more common in the equity space. The extension is not problematic.
 *
 * @author Alessandro Gnoatto
 */
public class OptionSurfaceData {

	private final String underlying;
	private final LocalDate referenceDate;
	private final DiscountCurve discountCurve; //\exp(-r*T) needed e.g. for application of the B&S formula
	private final DiscountCurve equityForwardCurve; //S0*\exp((r-d)*T)
	private final QuotingConvention convention; //either price or volatility (lognormal/normal)
	private final HashMap<Double, OptionSmileData> surface;
	private final double[] maturities;

	/**
	 * This is a very restrictive constructor that assumes that for each maturity we have the same number of option quotes.
	 *
	 * @param underlying The name of the underlying of this surface.
	 * @param referenceDate The reference date for this market data (t=0).
	 * @param strikes The vector of strikes.
	 * @param maturities The vector of maturities.
	 * @param values The matrix of values per (strike, maturity)
	 * @param convention The quoting convention (@see net.finmath.marketdata.model.volatilities.VolatilitySurfaceInterface.QuotingConvention).
	 * @param discountCurve A discount curve for discounting (funding/collateral rate).
	 * @param equityForwardCurve A the discount curve for forwarding (repo rate (e.g. funding minus dividents).
	 */
	public OptionSurfaceData(final String underlying, final LocalDate referenceDate, final double[] strikes,
			final double[] maturities, final double[][] values, final QuotingConvention convention, final DiscountCurve discountCurve,
			final DiscountCurve equityForwardCurve) {
		if(strikes.length != values.length || maturities.length != values[0].length ) {
			throw new IllegalArgumentException("Inconsistent number of strikes, maturities or values");
		}else {
			surface = new HashMap<>();

			for(int j = 0; j< maturities.length; j++) {

				final double[] valuesOfInterest = new double[strikes.length];

				for(int i= 0; i< strikes.length; i++) {
					valuesOfInterest[i] = values[i][j];
				}

				final OptionSmileData jthSmile = new OptionSmileData(underlying, referenceDate, strikes, maturities[j], valuesOfInterest, convention);
				surface.put(maturities[j],jthSmile);
			}

			this.underlying = underlying;
			this.referenceDate = referenceDate;
			this.discountCurve = discountCurve;
			this.equityForwardCurve = equityForwardCurve;
			this.convention = convention;
			this.maturities = maturities;

		}
	}


	/**
	 * Creates an equity option surface from an array of smiles.
	 *
	 * @param smiles The option smile data.
	 * @param discountCurve A discount curve for discounting (funding/collateral rate).
	 * @param equityForwardCurve A the discount curve for forwarding (repo rate (e.g. funding minus dividents).
	 */
	public OptionSurfaceData(final OptionSmileData[] smiles, final DiscountCurve discountCurve,
			final DiscountCurve equityForwardCurve) {

		final OptionSmileData firstSmile = smiles[0];
		final String myUnderlying = firstSmile.getUnderlying();
		final LocalDate myReferenceDate = firstSmile.getReferenceDate();
		final QuotingConvention myConvention = firstSmile.getSmile().get(firstSmile.getStrikes()[0]).getConvention();

		final HashMap<Double, OptionSmileData> mySurface = new HashMap<>();
		final double[] mats = new double[smiles.length];

		for(int t = 0; t<smiles.length;t++) {
			final double maturity = smiles[t].getMaturity();
			mats[t] = maturity;

			if(!(smiles[t].getReferenceDate().equals(myReferenceDate))) {
				throw new IllegalArgumentException("All reference dates must be equal");
			}

			if(!(smiles[t].getUnderlying().equals(myUnderlying))) {
				throw new IllegalArgumentException("Option must be written on the same underlying");
			}

			final QuotingConvention testConvention = smiles[t].getSmile().get(smiles[t].getStrikes()[0]).getConvention();
			if(!(testConvention.equals(myConvention))) {
				throw new IllegalArgumentException("Convention must be the same for all points in the surface");
			}

			mySurface.put(maturity, smiles[t]);
		}
		underlying = myUnderlying;
		referenceDate = myReferenceDate;
		this.discountCurve = discountCurve;
		this.equityForwardCurve = equityForwardCurve;
		surface = mySurface;
		convention = myConvention;
		maturities = mats;

	}

	public DiscountCurve getDiscountCurve() {
		return discountCurve;
	}

	public DiscountCurve getEquityForwardCurve() {
		return equityForwardCurve;
	}


	public String getName() {
		return underlying;
	}

	public LocalDate getReferenceDate() {
		return referenceDate;
	}

	public QuotingConvention getQuotingConvention() {
		return convention;
	}

	public HashMap<Double, OptionSmileData> getSurface(){
		return surface;
	}

	public double[] getMaturities() {
		return maturities;
	}

	public double getValue(final double maturity, final double strike){
		return getValue(maturity, strike, convention);
	}


	public double getValue(final double maturity, final double strike, final QuotingConvention quotingConvention)  {

		return getValue(null,maturity,strike,quotingConvention);
	}


	public double getValue(final AnalyticModel model, final double maturity, final double strike,
			final QuotingConvention quotingConvention) {
		if(quotingConvention.equals(convention)) {
			final OptionSmileData relevantSmile = surface.get(maturity);

			return relevantSmile.getSmile().get(strike).getValue();
		} else {
			if(quotingConvention == QuotingConvention.PRICE && convention == QuotingConvention.VOLATILITYLOGNORMAL) {

				final double forwardPrice = equityForwardCurve.getValue(maturity);
				final double discountBond = discountCurve.getValue(maturity);
				final OptionSmileData relevantSmile = surface.get(maturity);
				final double volatility = relevantSmile.getSmile().get(strike).getValue();
				return net.finmath.functions.AnalyticFormulas.blackScholesGeneralizedOptionValue(forwardPrice, volatility, maturity, strike, discountBond);

			} else if(quotingConvention == QuotingConvention.VOLATILITYLOGNORMAL && convention == QuotingConvention.PRICE) {
				final double forwardPrice = equityForwardCurve.getValue(maturity);
				final double discountBond = discountCurve.getValue(maturity);
				final OptionSmileData relevantSmile = surface.get(maturity);
				final double price = relevantSmile.getSmile().get(strike).getValue();
				return net.finmath.functions.AnalyticFormulas.blackScholesOptionImpliedVolatility(forwardPrice,maturity,strike,discountBond,price);

			}
			return 0.0;
		}
	}

	public OptionSmileData getSmile(final double maturity) {
		return surface.get(maturity);
	}
}
