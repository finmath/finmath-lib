package net.finmath.montecarlo.assetderivativevaluation.risk;

import java.util.Arrays;
import java.util.List;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel;
import net.finmath.montecarlo.assetderivativevaluation.products.AbstractAssetMonteCarloProduct;
import net.finmath.stochastic.RandomVariable;

/**
 Risk metrics based on Monte Carlo samples.

 Conventions used here
 Loss variable:Values represent losses (higher = worse).
 Tail probability alpha:alpha is the size of the worst tail (e.g., 0.05 for the worst 5%).
 VaR(alpha): Quantile at (1 - alpha). Example: VaR(5%) = 95th percentile of loss.
 CVaR(alpha): Average loss in the worst alpha tail (values >= VaR(alpha)).

 Typical usage:

 RandomVariable losses = ...; // e.g. simulated portfolio loss
 double var5  = RiskCalculator.computeVaR(losses, 0.05);
 double cvar5 = RiskCalculator.computeCVaR(losses, 0.05);

 You can also aggregate multiple instruments via {@link #aggregateLosses(RandomVariable...)}.
 */


public final class RiskCalculator {
	private RiskCalculator() {}

	/* ********************************************************************************************
	 * Public API — RandomVariable inputs
	 * ********************************************************************************************/

	/**
	 * Compute Value-at-Risk at tail probability alpha (e.g., alpha=0.05 ⇒ 5% VaR).
	 * Assumes the input is a loss random variable (larger = worse).
	 */
	public static double computeVaR(final RandomVariable losses, final double alpha) {
		if (losses == null) {
			throw new NullPointerException("losses must not be null");
		}
		validateAlpha(alpha);
		// Worst alpha tail ⇒ upper-tail quantile = (1 - alpha)
		return losses.getQuantile(1.0 - alpha);
		// If your finmath build defines quantiles differently, adjust this line accordingly.
	}

	/**
	 * Compute Conditional VaR (Expected Shortfall) at tail probability alpha:
	 * the average of losses that are >= VaR(alpha).
	 */
	public static double computeCVaR(final RandomVariable losses, final double alpha) {
		if (losses == null) {
			throw new NullPointerException("losses must not be null");
		}
		validateAlpha(alpha);

		final double var = computeVaR(losses, alpha);

		// Use values to form the tail and average it.
		final double[] x = losses.getRealizations();
		if (x == null || x.length == 0) {
			throw new IllegalArgumentException("losses has no sample values.");
		}

		double sumTail = 0.0;
		int countTail = 0;
		for (double v : x) {
			if (v >= var) {
				sumTail += v;
				countTail++;
			}
		}

		// Fallback: if numerical ties produce 0 tail count, approximate by expected tail size
		if (countTail == 0) {
			// expected tail size ≈ alpha * N; protect against division by zero
			int expected = Math.max(1, (int)Math.round(alpha * x.length));
			return var; // conservative fallback (or sumTail / expected if you prefer)
		}
		return sumTail / countTail;
	}

	/**
	 * Sum multiple loss random variables into a single portfolio loss.
	 */
	public static RandomVariable aggregateLosses(final RandomVariable... losses) {
		if (losses == null || losses.length == 0) {
			throw new IllegalArgumentException("At least one RandomVariable is required.");
		}
		if (losses[0] == null) {
			throw new NullPointerException("losses[0] must not be null");
		}
		RandomVariable total = losses[0];
		for (int i = 1; i < losses.length; i++) {
			if (losses[i] == null) {
				throw new NullPointerException("losses[" + i + "] must not be null");
			}
			total = total.add(losses[i]);
		}
		return total;
	}

	/* ********************************************************************************************
	 * Convenience overloads — arrays, portfolio lists, and model/product helpers
	 * ********************************************************************************************/

	/**
	 * VaR for raw loss samples (higher = worse).
	 */
	public static double computeVaR(final double[] losses, final double alpha) {
		if (losses == null) {
			throw new NullPointerException("losses must not be null");
		}
		validateAlpha(alpha);
		if (losses.length == 0) {
			throw new IllegalArgumentException("losses must have length > 0");
		}
		// Upper-tail VaR at (1 - alpha)
		double[] copy = Arrays.copyOf(losses, losses.length);
		Arrays.sort(copy); // ascending
		int n = copy.length;
		// index for p-th percentile with p = (1 - alpha)
		double p = 1.0 - alpha;
		int idx = (int)Math.ceil(p * n) - 1;
		idx = Math.max(0, Math.min(n - 1, idx));
		return copy[idx];
	}

	/**
	 * CVaR for raw loss samples (higher = worse).
	 */
	public static double computeCVaR(final double[] losses, final double alpha) {
		if (losses == null) {
			throw new NullPointerException("losses must not be null");
		}
		validateAlpha(alpha);
		if (losses.length == 0) {
			throw new IllegalArgumentException("losses must have length > 0");
		}
		final double var = computeVaR(losses, alpha);
		double sum = 0.0;
		int cnt = 0;
		for (double v : losses) {
			if (v >= var) {
				sum += v;
				cnt++;
			}
		}
		if (cnt == 0) {
			return var; // conservative fallback
		}
		return sum / cnt;
	}

	/**
	 * Convenience: compute VaR from a simulated product by interpreting payoff as P&L and converting to loss.
	 * If your payoff is already a loss, pass it directly to {@link #computeVaR(RandomVariable, double)}.
	 *
	 * @param evaluationTime    usually the product's maturity or reporting time (e.g. 0.0 for present value)
	 */
	public static double computeVaRFromProduct(
			final AssetModelMonteCarloSimulationModel model,
			final AbstractAssetMonteCarloProduct product,
			final double evaluationTime,
			final double alpha
	) throws Exception {
		if (model == null) {
			throw new NullPointerException("model must not be null");
		}
		if (product == null) {
			throw new NullPointerException("product must not be null");
		}
		validateAlpha(alpha);

		// Interpret product value as P&L and convert to loss = -P&L
		final RandomVariable pnl = product.getValue(evaluationTime, model);
		final RandomVariable loss = pnl.mult(-1.0);
		return computeVaR(loss, alpha);
	}

	/**
	 * Convenience: compute CVaR from a simulated product by interpreting payoff as P&L and converting to loss.
	 */
	public static double computeCVaRFromProduct(
			final AssetModelMonteCarloSimulationModel model,
			final AbstractAssetMonteCarloProduct product,
			final double evaluationTime,
			final double alpha
	) throws Exception {
		if (model == null) {
			throw new NullPointerException("model must not be null");
		}
		if (product == null) {
			throw new NullPointerException("product must not be null");
		}
		validateAlpha(alpha);

		final RandomVariable pnl = product.getValue(evaluationTime, model);
		final RandomVariable loss = pnl.mult(-1.0);
		return computeCVaR(loss, alpha);
	}

	/**
	 * Convenience: aggregate a list of loss RandomVariables into one and return VaR.
	 */
	public static double computePortfolioVaR(final List<RandomVariable> lossList, final double alpha) {
		if (lossList == null) {
			throw new NullPointerException("lossList must not be null");
		}
		if (lossList.isEmpty()) {
			throw new IllegalArgumentException("lossList must not be empty");
		}
		return computeVaR(aggregateLosses(lossList.toArray(RandomVariable[]::new)), alpha);
	}

	/**
	 * Convenience: aggregate a list of loss RandomVariables into one and return CVaR.
	 */
	public static double computePortfolioCVaR(final List<RandomVariable> lossList, final double alpha) {
		if (lossList == null) {
			throw new NullPointerException("lossList must not be null");
		}
		if (lossList.isEmpty()) {
			throw new IllegalArgumentException("lossList must not be empty");
		}
		return computeCVaR(aggregateLosses(lossList.toArray(RandomVariable[]::new)), alpha);
	}

	/* ********************************************************************************************
	 * Helpers
	 * ********************************************************************************************/

	private static void validateAlpha(final double alpha) {
		if (!(alpha > 0.0 && alpha < 1.0)) {
			throw new IllegalArgumentException("alpha must be in (0,1). Given: " + alpha);
		}
	}
}
