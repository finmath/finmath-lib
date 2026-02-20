package net.finmath.tree.assetderivativevaluation.products;

import java.util.function.DoubleUnaryOperator;

import net.finmath.modelling.products.CallOrPut;
import net.finmath.stochastic.RandomVariable;
import net.finmath.tree.TreeModel;

/**
 * Implements the valuation of a European option on a single asset.
 *
 * Given a model for an asset <i>S</i>, the European option with strike <i>K</i>, maturity <i>T</i>
 * pays
 * <br>
 * 	<i>V(T) = max((S(T) - K) * CallOrPut , 0)</i> in <i>T</i>.
 * <br>
 *
 * @author Alessandro Gnoatto
 */
public class EuropeanOption extends AbstractNonPathDependentProduct {
	private final double maturity;
	private final double strike;
	private final CallOrPut callOrPutSign;
	private final String underlyingName;

	/**
	 * Construct a product representing an European option on an asset S (where S the asset with index <code>underlyingIndex</code> from the model - single asset case).
	 * @param underlyingName Name of the underlying
	 * @param maturity The maturity T in the option payoff max(sign * (S(T)-K),0).
	 * @param strike The strike K in the option payoff max(sign * (S(T)-K),0).
	 * @param callOrPutSign The sign in the payoff.
	 */
	public EuropeanOption(final String underlyingName, final double maturity, final double strike, final double callOrPutSign) {	
		super(maturity);
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
		if(this.callOrPutSign == CallOrPut.CALL) {
			this.payOffFunction = assetValue ->  Math.max(assetValue - strike, 0);
		}else {
			this.payOffFunction = assetValue ->  Math.max(strike - assetValue, 0);
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
		super(maturity);
		this.underlyingName	= underlyingName;
		this.maturity		= maturity;
		this.strike			= strike;
		this.callOrPutSign	= callOrPutSign;
		if(this.callOrPutSign == CallOrPut.CALL) {
			this.payOffFunction = assetValue ->  Math.max(assetValue - strike, 0);
		}else {
			this.payOffFunction = assetValue ->  Math.max(strike - assetValue, 0);
		}

	}

	/**
	 * Construct a product representing an European option on an asset S (where S the asset with index 0 from the model - single asset case).
	 * @param maturity The maturity T in the option payoff max(S(T)-K,0)
	 * @param strike The strike K in the option payoff max(S(T)-K,0).
	 * @param callOrPutSign The sign in the payoff.
	 * @param underlyingIndex The index of the underlying to be fetched from the model.
	 */
	public EuropeanOption(final double maturity, final double strike, final double callOrPutSign) {
		super(maturity);
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
		if(this.callOrPutSign == CallOrPut.CALL) {
			this.payOffFunction = assetValue ->  Math.max(assetValue - strike, 0);
		}else {
			this.payOffFunction = assetValue ->  Math.max(strike - assetValue, 0);
		}

	}

	/**
	 * Construct a product representing an European option on an asset S (where S the asset with index 0 from the model - single asset case).
	 * @param maturity The maturity T in the option payoff max(S(T)-K,0)
	 * @param strike The strike K in the option payoff max(S(T)-K,0).
	 * @param callOrPutSign The sign in the payoff.
	 * @param underlyingIndex The index of the underlying to be fetched from the model.
	 */
	public EuropeanOption(final double maturity, final double strike, final CallOrPut callOrPutSign) {
		super(maturity);
		this.maturity			= maturity;
		this.strike				= strike;
		this.callOrPutSign		= callOrPutSign;
		this.underlyingName	= null;		// Use underlyingIndex
		if(this.callOrPutSign == CallOrPut.CALL) {
			this.payOffFunction = assetValue ->  Math.max(assetValue - strike, 0);
		}else {
			this.payOffFunction = assetValue ->  Math.max(strike - assetValue, 0);
		}

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
	protected DoubleUnaryOperator getPayOffFunction(){
		if(callOrPutSign == CallOrPut.CALL) {
			return assetValue ->  Math.max(assetValue - strike, 0);
		}else {
			return assetValue ->  Math.max(strike - assetValue, 0);
		}
	}

	@Override
	public RandomVariable[] getValues(double evaluationTime, TreeModel model) {
		final int k0  = timeToIndex(evaluationTime,model);
		final int n = model.getNumberOfTimes()-1;
		final RandomVariable[] values = new RandomVariable[n -k0 +1];
		values[n-k0] = model.getTransformedValuesAtGivenTimeRV(model.getLastTime(),this.getPayOffFunction());

		for (int timeIndex = n - 1; timeIndex >= k0; timeIndex--) {
			values[timeIndex - k0] = model.getConditionalExpectationRV(values[(timeIndex+1)], timeIndex);
		}
		return values;

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

	public String getUnderlyingName() {
		return underlyingName;
	}

}
