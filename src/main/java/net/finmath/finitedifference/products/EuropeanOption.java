package net.finmath.finitedifference.products;

import net.finmath.finitedifference.models.FiniteDifference1DBoundary;
import net.finmath.finitedifference.models.FiniteDifference1DModel;
import net.finmath.modelling.products.CallOrPut;


/**
 * Implements valuation of a European option on a single asset.
 *
 * Given a model for an asset <i>S</i>, the European option with strike <i>K</i>, maturity <i>T</i>
 * pays
 * <br>
 * 	<i>max((S(T) - K) * CallOrPut , 0)</i> in <i>T</i>
 * <br>
 *
 * The class implements the characteristic function of the call option
 * payoff, i.e., its Fourier transform.
 *
 * @author Christian Fries
 * @author Ralph Rudd
 * @author Alessandro Gnoatto
 * @version 1.0
 */
public class EuropeanOption implements FiniteDifference1DProduct, FiniteDifference1DBoundary{

	private final String underlyingName;
	private final double maturity;
	private final double strike;
	private final CallOrPut callOrPutSign;

	/**
	 * Construct a product representing an European option on an asset S (where S the asset with index <code>underlyingIndex</code> from the model - single asset case).
	 * @param underlyingName Name of the underlying
	 * @param maturity The maturity T in the option payoff max(sign * (S(T)-K),0).
	 * @param strike The strike K in the option payoff max(sign * (S(T)-K),0).
	 * @param callOrPutSign The sign in the payoff.
	 */
	public EuropeanOption(final String underlyingName, final double maturity, final double strike, final double callOrPutSign) {
		super();
		this.underlyingName	= underlyingName;
		this.maturity		= maturity;
		this.strike			= strike;
		if(callOrPutSign == 1.0) {
			this.callOrPutSign = CallOrPut.CALL;
		}else if(callOrPutSign == - 1.0) {
			this.callOrPutSign = CallOrPut.PUT;
		}else {
			throw new IllegalArgumentException("Unknown option type");
		}
	}

	/**
	 * Construct a product representing an European option on an asset S (where S the asset with index <code>underlyingIndex</code> from the model - single asset case).
	 * @param underlyingName Name of the underlying
	 * @param maturity The maturity T in the option payoff max(sign * (S(T)-K),0).
	 * @param strike The strike K in the option payoff max(sign * (S(T)-K),0).
	 * @param callOrPutSign The sign in the payoff.
	 */
	public EuropeanOption(final String underlyingName, final double maturity, final double strike, final CallOrPut callOrPutSign) {
		super();
		this.underlyingName	= underlyingName;
		this.maturity		= maturity;
		this.strike			= strike;
		this.callOrPutSign	= callOrPutSign;
	}

	/**
	 * Construct a product representing an European option on an asset S (where S the asset with index 0 from the model - single asset case).
	 * @param maturity The maturity T in the option payoff max(S(T)-K,0)
	 * @param strike The strike K in the option payoff max(S(T)-K,0).
	 * @param callOrPutSign The sign in the payoff.
	 */
	public EuropeanOption(final double maturity, final double strike, final double callOrPutSign) {
		super();
		this.maturity			= maturity;
		this.strike				= strike;
		if(callOrPutSign == 1.0) {
			this.callOrPutSign = CallOrPut.CALL;
		}else if(callOrPutSign == - 1.0) {
			this.callOrPutSign = CallOrPut.PUT;
		}else {
			throw new IllegalArgumentException("Unknown option type");
		}
		this.underlyingName	= null;		// Use underlyingIndex
	}

	/**
	 * Construct a product representing an European option on an asset S (where S the asset with index 0 from the model - single asset case).
	 * @param maturity The maturity T in the option payoff max(S(T)-K,0)
	 * @param strike The strike K in the option payoff max(S(T)-K,0).
	 * @param callOrPutSign The sign in the payoff.
	 */
	public EuropeanOption(final double maturity, final double strike, final CallOrPut callOrPutSign) {
		super();
		this.maturity			= maturity;
		this.strike				= strike;
		this.callOrPutSign		= callOrPutSign;
		this.underlyingName	= null;		// Use underlyingIndex
	}
	
	/**
	 * Construct a product representing an European option on an asset S (where S the asset with index <code>underlyingIndex</code> from the model - single asset case).
	 * @param underlyingName Name of the underlying
	 * @param maturity The maturity T in the option payoff max(S(T)-K,0)
	 * @param strike The strike K in the option payoff max(S(T)-K,0).
	 */
	public EuropeanOption(final String underlyingName, final double maturity, final double strike) {
		this(underlyingName, maturity, strike, 1.0);
	}


	/**
	 * Construct a product representing an European option on an asset S (where S the asset with index 0 from the model - single asset case).
	 * @param maturity The maturity T in the option payoff max(S(T)-K,0)
	 * @param strike The strike K in the option payoff max(S(T)-K,0).
	 */
	public EuropeanOption(final double maturity, final double strike) {
		this(maturity, strike, 1.0);
	}

	@Override
	public double[][] getValue(final double evaluationTime, final FiniteDifference1DModel model) {

		/*
		 * The FDM algorithm requires the boundary conditions of the product.
		 * This product implements the boundary interface
		 */
		final FiniteDifference1DBoundary boundary = this;
		
		if(callOrPutSign == CallOrPut.CALL) {
			return model.getValue(evaluationTime, maturity, assetValue ->  Math.max(assetValue - strike, 0), boundary);
		}else {
			return model.getValue(evaluationTime, maturity, assetValue ->  Math.max(strike - assetValue, 0), boundary);
		}
	}

	/*
	 * Implementation of the interface:
	 * @see net.finmath.finitedifference.products.FiniteDifference1DBoundary#getValueAtLowerBoundary(net.finmath.finitedifference.models.FDMBlackScholesModel, double, double)
	 */
	@Override
	public double getValueAtLowerBoundary(final FiniteDifference1DModel model, final double currentTime, final double stockPrice) {
		if(callOrPutSign == CallOrPut.CALL) {
			return 0;
		}else {
			return strike * Math.exp(-model.getRiskFreeRate()*(maturity - currentTime));
		}
	}

	@Override
	public double getValueAtUpperBoundary(final FiniteDifference1DModel model, final double currentTime, final double stockPrice) {
		if(callOrPutSign == CallOrPut.CALL) {
			return stockPrice - strike * Math.exp(-model.getRiskFreeRate()*(maturity - currentTime));
		}else {
			return 0.0;
		}
		
	}

	public String getUnderlyingName() {
		return underlyingName;
	}

	public double getMaturity() {
		return maturity;
	}

	public double getStrike() {
		return strike;
	}

	public CallOrPut getCallOrPutSign() {
		return callOrPutSign;
	}

}
