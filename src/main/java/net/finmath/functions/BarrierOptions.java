package net.finmath.functions;

/**
 * This class implements the valuation of barrier options.
 *
 * Currently only supports a a lognormal model.
 *
 * We use the notation from the book by Espeen Gaarder Haugh.
 * "The complete Guide to Option Pricing Formulas".
 *
 * @author Alessandro Gnoatto
 * @version 1.0
 * @date 09.03.2020
 */
public class BarrierOptions {

	public enum BarrierType{
		DOWN_IN,
		UP_IN,
		DOWN_OUT,
		UP_OUT
	}

	/**
	 * Preventing instantiation of this class.
	 */
	private BarrierOptions() {}

	/**
	 * Value a barrier option.
	 *
	 * @param initialStockValue The model's initial value of the stock.
	 * @param riskFreeRate The model's risk free rate of the stock.
	 * @param dividendYield The model's dividend yield of the stock.
	 * @param volatility  The model's volatility yield of the stock.
	 * @param optionMaturity The product's option maturity.
	 * @param optionStrike The product's option strike.
	 * @param isCall If true, the a call option will be valued, otherwise a put option.
	 * @param rebate The product's rebate.
	 * @param barrierValue The location of the barrier.
	 * @param barrierType The type of the barrier.
	 *
	 * @return The value of the option.
	 */
	public static double blackScholesBarrierOptionValue(double initialStockValue,
			double riskFreeRate,
			double dividendYield,
			double volatility,
			double optionMaturity,
			double optionStrike,
			boolean isCall,
			double rebate,
			double barrierValue,
			BarrierType barrierType) {

		final int phi = isCall ? +1 : -1;
		int eta = 0;

		switch(barrierType) {
		case UP_IN:
			eta = -1;
			break;
		case DOWN_IN:
			eta = 1;
			break;
		case UP_OUT:
			eta = -1;
			break;
		case DOWN_OUT:
			eta = 1;
			break;
		default:
			throw new IllegalArgumentException("Invalid barrier type.");
		}

		final double volSq = volatility * volatility;
		final double volTime = volatility * Math.sqrt(optionMaturity);
		final double mu = (dividendYield - 0.5 * volSq)/(volSq);
		final double lambda = Math.sqrt(mu * mu + (2*(riskFreeRate))/(volSq));
		final double z = Math.log(barrierValue / initialStockValue) / volTime + lambda * volTime;

		final double muVolTime = (1 + mu) * volTime;

		final double x1 = Math.log(initialStockValue / optionStrike)/ volTime
				+ muVolTime;
		final double x2 = Math.log(initialStockValue / barrierValue)/ volTime
				+ muVolTime;

		final double y1 = Math.log(barrierValue * barrierValue / (initialStockValue * optionStrike))
				/ volTime + muVolTime;
		final double y2 = Math.log(barrierValue / initialStockValue) / volTime + muVolTime;

		final double A = phi  * initialStockValue * Math.exp((dividendYield-riskFreeRate) * optionMaturity)
				* NormalDistribution.cumulativeDistribution(phi * x1)
				- phi * optionStrike *Math.exp(-riskFreeRate * optionMaturity)
				* NormalDistribution.cumulativeDistribution(phi* (x1 - volTime));
		final double B = phi * initialStockValue * Math.exp((dividendYield-riskFreeRate)  * optionMaturity)
				* NormalDistribution.cumulativeDistribution(phi * x2)
				- phi * optionStrike * Math.exp(-riskFreeRate *optionMaturity)
				* NormalDistribution.cumulativeDistribution(phi * (x2 - volTime));
		final double C = phi * initialStockValue * Math.exp((dividendYield-riskFreeRate)  * optionMaturity)
				* Math.pow(barrierValue / initialStockValue, 2 * (mu+1))
				* NormalDistribution.cumulativeDistribution(eta * y1)
				- phi * optionStrike * Math.exp(-riskFreeRate * optionMaturity)
				* Math.pow(barrierValue / initialStockValue, 2 * mu)
				* NormalDistribution.cumulativeDistribution(eta * (y1-volTime));
		final double D =  phi * initialStockValue * Math.exp((dividendYield-riskFreeRate)  * optionMaturity)
				* Math.pow(barrierValue / initialStockValue, 2 * (mu+1))
				* NormalDistribution.cumulativeDistribution(eta * y2)
				- phi * optionStrike * Math.exp(-riskFreeRate * optionMaturity)
				* Math.pow(barrierValue / initialStockValue, 2 * mu)
				* NormalDistribution.cumulativeDistribution(eta * (y2-volTime));
		final double E = rebate * Math.exp(-riskFreeRate * optionMaturity)
				* (NormalDistribution.cumulativeDistribution(eta * (x2-volTime))
						- Math.pow(barrierValue / initialStockValue, 2*mu)
						* NormalDistribution.cumulativeDistribution(eta * (y2-volTime)));
		final double F = rebate *(Math.pow(barrierValue / initialStockValue, mu + lambda)
				* NormalDistribution.cumulativeDistribution(eta * z)
				+ Math.pow(barrierValue / initialStockValue, mu-lambda)
				* NormalDistribution.cumulativeDistribution(eta * (z - 2 * lambda * volTime)));

		double optionValue = 0.0;

		switch(barrierType) {
		/*
		 * In options are paid for today but first come into existence
		 * if the asset price S hits the barrier H before expiration.
		 * It is possible to include a pres-pecified cash rebate K,
		 * which is paid out at option expiration if the option
		 * has not been knocked in during its lifetime.
		 */
		case DOWN_IN: //Down in call e put ok
			if(isCall) {
				//down and in call
				//Payoff: max(S — X; 0) if S < H before T else K at expiration.
				if(optionStrike >= barrierValue) {
					//(eta=1, phi=1)
					optionValue = C + E;
				}else {
					//(eta=1, phi=1)
					optionValue = A - B + D + E;
				}
			}else {
				//down and in put
				//Payoff: max(X - S;0) if S < H before T else K at expiration.
				if(optionStrike >= barrierValue) {
					//(eta=1, phi=-1)
					optionValue = B - C + D + E;
				}else {
					//(eta=1, phi=-1)
					optionValue = A + E;
				}
			}
			break;
		case UP_IN:
			if(isCall) {
				//up and in call
				//Payoff: max (S —X; 0) if S > H before T else K at expiration.
				if(optionStrike >= barrierValue) {
					//(eta=-1, phi=1)
					optionValue = A + E;
				}else {
					//(eta=-1, phi=1)
					optionValue = B - C + D + E;
				}
			}else {
				//up and in put
				//Payoff: max(X — S; 0) if S > H before T else K at expiration.
				if(optionStrike >= barrierValue) {
					//(eta=-1, phi=-1)
					optionValue = A - B + D + E;
				}else {
					//(eta=-1, phi=-1)
					optionValue = C + E;
				}
			}
			break;
			/*
			 * Out options are similar to standard options except that
			 * the option becomes worthless if the asset price S hits
			 * the barrier before expiration.
			 * It is possible to include a prespecified cash rebate K,
			 * which is paid out if the option is knocked out before expiration.
			 */
		case DOWN_OUT:
			if(isCall) {
				//down and out call
				//Payoff: max(S — X; 0) if S > H before T else K at hit.
				if(optionStrike >= barrierValue) {
					//(eta=1, phi=1)
					optionValue = A - C + F;
				}else {
					//(eta=1, phi=1)
					optionValue = B - D + F;
				}
			}else {
				//down and out put
				//Payoff: max(X — S; 0) if S > H before T else K at hit.
				if(optionStrike >= barrierValue) {
					//(eta=1, phi=-1)
					optionValue = A - B + C - D + F;
				}else {
					//(eta=1, phi=-1)
					optionValue = F;
				}
			}
			break;
		case UP_OUT:
			if(isCall) {
				//up and out call
				//Payoff: max(S — X; 0) if S < H before T else K at hit.
				if(optionStrike >= barrierValue) {
					//(eta=-1, phi=1)
					optionValue = F;
				}else {
					//(eta=-1, phi=1)
					optionValue = A - B + C - D + F;
				}
			}else {
				//up and out put
				//Payoff: max(X — S; 0) if S < H before T else K at hit.
				if(optionStrike >= barrierValue) {
					//(eta=-1, phi=-1)
					optionValue = B - D + F;
				}else {
					//(eta=-1, phi=-1)
					optionValue = A - C + F;
				}
			}
			break;
		default:
			throw new IllegalArgumentException("Invalid barrier type.");
		}

		return optionValue;
	}
}
