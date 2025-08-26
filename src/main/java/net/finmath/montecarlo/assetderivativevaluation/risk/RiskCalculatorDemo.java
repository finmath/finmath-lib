package net.finmath.montecarlo.assetderivativevaluation.risk;

import java.util.Arrays;
import java.util.List;

import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel;
import net.finmath.montecarlo.assetderivativevaluation.products.AbstractAssetMonteCarloProduct;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;

public class RiskCalculatorDemo {

	public static void main(String[] args) throws Exception {
		demoWithArrays();
		demoWithRandomVariable();
		demoPortfolioHelpers();
		demoFromProductHelpers();
	}

	/* ------------------------- Array-based ------------------------- */
	private static void demoWithArrays() {
		System.out.println("\n=== Array-based ===");
		double[] losses = new double[100];
		for (int i = 0; i < losses.length; i++) losses[i] = i + 1;  // 1 to 100 (higher = worse)

		double var10  = RiskCalculator.computeVaR(losses, 0.10);   // ~90
		double cvar10 = RiskCalculator.computeCVaR(losses, 0.10);  // avg >= VaR  = 95.0

		System.out.println("VaR(10%)  = " + var10);
		System.out.println("CVaR(10%) = " + cvar10);

		// Another small example with ties
		double[] tied = {1, 2, 2, 2, 10};
		double var20t  = RiskCalculator.computeVaR(tied, 0.20);
		double cvar20t = RiskCalculator.computeCVaR(tied, 0.20);
		System.out.println("With ties {1,2,2,2,10}, VaR(20%)=" + var20t + ", CVaR(20%)=" + cvar20t);
	}

	/* ---------------------- RandomVariable-based ---------------------- */
	private static void demoWithRandomVariable() {
		System.out.println("\n=== RandomVariable-based ===");
		double[] arr = new double[1000];
		for (int i = 0; i < arr.length; i++) arr[i] = i; // 0..999
		RandomVariable losses = new RandomVariableFromDoubleArray(0.0, arr);

		double var5  = RiskCalculator.computeVaR(losses, 0.05);   // ~950
		double cvar5 = RiskCalculator.computeCVaR(losses, 0.05);  // ~974.5

		System.out.println("VaR(5%)   = " + var5);
		System.out.println("CVaR(5%)  = " + cvar5);
	}

	/* ----------------------- Portfolio helpers ----------------------- */
	private static void demoPortfolioHelpers() {
		System.out.println("\n=== Portfolio helpers ===");

		// Three instruments’ loss random variables (3 paths each for illustration)
		RandomVariable l1 = new RandomVariableFromDoubleArray(0.0, new double[]{10, 20, 30});
		RandomVariable l2 = new RandomVariableFromDoubleArray(0.0, new double[]{ 5, 15, 25});
		RandomVariable l3 = new RandomVariableFromDoubleArray(0.0, new double[]{ 2,  4,  6});

		// 1) Aggregate manually and compute metrics
		RandomVariable portfolioLoss = RiskCalculator.aggregateLosses(l1, l2, l3); // pathwise sum
		System.out.println("Portfolio loss paths: " + Arrays.toString(portfolioLoss.getRealizations()));

		double var33  = RiskCalculator.computeVaR(portfolioLoss, 1.0/3.0);
		double cvar33 = RiskCalculator.computeCVaR(portfolioLoss, 1.0/3.0);
		System.out.println("Aggregate→ VaR(33%)=" + var33 + ", CVaR(33%)=" + cvar33);

		// 2) Use list-based convenience methods
		List<RandomVariable> list = List.of(l1, l2, l3);
		double varList  = RiskCalculator.computePortfolioVaR(list, 1.0/3.0);
		double cvarList = RiskCalculator.computePortfolioCVaR(list, 1.0/3.0);
		System.out.println("List API → VaR(33%)=" + varList + ", CVaR(33%)=" + cvarList);
	}

	/* -------------------- From-product helpers demo -------------------- */
	private static void demoFromProductHelpers() throws Exception {
		System.out.println("\n=== From-product helpers ===");

		// Imagine a product payoff (P&L): positive = profit, negative = loss
		double[] pnlPaths = {-10, 0, 20, -5, 100};
		RandomVariable payoff = new RandomVariableFromDoubleArray(0.0, pnlPaths);

		// Dummy product: ignores the model and returns the payoff as given.
		DummyProduct product = new DummyProduct(payoff);
		// Dummy model: Won’t actually call any methods on it in these helpers.
		DummyModel model = new DummyModel();

		double alpha = 0.20;
		double var = RiskCalculator.computeVaRFromProduct(model, product, 0.0, alpha);
		double es  = RiskCalculator.computeCVaRFromProduct(model, product, 0.0, alpha);

		System.out.println("FromProduct VaR(20%) = " + var);
		System.out.println("FromProduct CVaR(20%)= " + es);

		// Sanity check: do the same manually by converting payoff→loss = -pnl
		double[] loss = Arrays.stream(pnlPaths).map(x -> -x).toArray();
		double varCheck = RiskCalculator.computeVaR(loss, alpha);
		double esCheck  = RiskCalculator.computeCVaR(loss, alpha);
		System.out.println("Manual check: VaR=" + varCheck + ", CVaR=" + esCheck);
	}

	/* ==================================================================
	 * Stubs for the “from product” demo.
	 * ================================================================== */

	//Returns a fixed RandomVariable payoff, ignoring the model.
	static final class DummyProduct extends AbstractAssetMonteCarloProduct {
		private final RandomVariable rv;
		DummyProduct(RandomVariable rv) { this.rv = rv; }
		@Override
		public RandomVariable getValue(double evaluationTime, AssetModelMonteCarloSimulationModel model) {
			return rv;
		}
	}

	//Model stub-methods are not used by the demo helpers.
	static final class DummyModel implements AssetModelMonteCarloSimulationModel {
		@Override public RandomVariable getAssetValue(double time, int assetIndex) { throw new UnsupportedOperationException(); }
		@Override public RandomVariable getAssetValue(int timeIndex, int assetIndex) { throw new UnsupportedOperationException(); }
		@Override public RandomVariable getNumeraire(double time) { throw new UnsupportedOperationException(); }
		@Override public RandomVariable getNumeraire(int timeIndex) { throw new UnsupportedOperationException(); }
		@Override public RandomVariable getMonteCarloWeights(double time) { throw new UnsupportedOperationException(); }
		@Override public RandomVariable getMonteCarloWeights(int timeIndex) { throw new UnsupportedOperationException(); }
		@Override public double getTime(int timeIndex) { throw new UnsupportedOperationException(); }
		@Override public int getTimeIndex(double time) { throw new UnsupportedOperationException(); }
		@Override public int getNumberOfPaths() { throw new UnsupportedOperationException(); }
		@Override public int getNumberOfAssets() { return 1; } // dummy safe default
		@Override public TimeDiscretization getTimeDiscretization() { throw new UnsupportedOperationException(); }
		@Override public AssetModelMonteCarloSimulationModel getCloneWithModifiedSeed(int seed) { return this; }
		@Override public AssetModelMonteCarloSimulationModel getCloneWithModifiedData(java.util.Map<String,Object> dataModified) { return this; }
		@Override public java.time.LocalDateTime getReferenceDate() { throw new UnsupportedOperationException(); }
		@Override public RandomVariable getRandomVariableForConstant(double value) { throw new UnsupportedOperationException(); }
	}
}
