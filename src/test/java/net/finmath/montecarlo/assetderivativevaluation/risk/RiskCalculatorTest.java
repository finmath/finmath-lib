package net.finmath.montecarlo.assetderivativevaluation.risk;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel;
import net.finmath.montecarlo.assetderivativevaluation.products.AbstractAssetMonteCarloProduct;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;


public class RiskCalculatorTest {

	/* =======================================================================
	   ARRAY OVERLOADS
	 * ======================================================================= */

	@Test
	@DisplayName("Array – VaR/CVaR on ascending losses (1..100), alpha=0.10")
	void array_var_cvar_simple() {
		double[] losses = new double[100];
		for (int i = 0; i < losses.length; i++) losses[i] = i + 1; // 1..100

		double var10  = RiskCalculator.computeVaR(losses, 0.10);   // 90th percentile ≈ 90
		double cvar10 = RiskCalculator.computeCVaR(losses, 0.10);  // avg of 90..100 = 95.0

		assertEquals(90.0, var10, 1e-9);
		assertEquals(95.0, cvar10, 1e-9);
		assertTrue(cvar10 >= var10);
	}

	@Test
	@DisplayName("Array – invalid alpha throws")
	void array_invalid_alpha() {
		double[] losses = {1,2,3};
		assertThrows(IllegalArgumentException.class, () -> RiskCalculator.computeVaR(losses, 0.0));
		assertThrows(IllegalArgumentException.class, () -> RiskCalculator.computeVaR(losses, 1.0));
		assertThrows(IllegalArgumentException.class, () -> RiskCalculator.computeCVaR(losses, 0.0));
		assertThrows(IllegalArgumentException.class, () -> RiskCalculator.computeCVaR(losses, 1.0));
	}

	@Test
	@DisplayName("Array – empty input throws")
	void array_empty_throws() {
		assertThrows(IllegalArgumentException.class, () -> RiskCalculator.computeVaR(new double[]{}, 0.05));
		assertThrows(IllegalArgumentException.class, () -> RiskCalculator.computeCVaR(new double[]{}, 0.05));
	}

	@Test
	@DisplayName("Array – null input throws NPE")
	void array_null_input() {
		assertThrows(NullPointerException.class, () -> RiskCalculator.computeVaR((double[]) null, 0.10));
		assertThrows(NullPointerException.class, () -> RiskCalculator.computeCVaR((double[]) null, 0.10));
	}

	/* =======================================================================
	   RANDOM VARIABLE OVERLOADS
	 * ======================================================================= */

	@Test
	@DisplayName("RandomVariable – VaR/CVaR on linear losses 0 to 999, alpha=0.05")
	void rv_var_cvar_linear() {
		double[] arr = new double[1000];
		for (int i = 0; i < arr.length; i++) arr[i] = i; // 0..999
		RandomVariable losses = new RandomVariableFromDoubleArray(0.0, arr);

		double var5  = RiskCalculator.computeVaR(losses, 0.05);   // ~950
		double cvar5 = RiskCalculator.computeCVaR(losses, 0.05);  // ~974.5

		assertEquals(950.0, var5, 1.0);
		assertEquals(974.5, cvar5, 1.0);
		assertTrue(cvar5 >= var5);
	}

	@Test
	@DisplayName("RandomVariable – invalid alpha throws")
	void rv_invalid_alpha() {
		RandomVariable rv = new RandomVariableFromDoubleArray(0.0, new double[]{1,2,3});
		assertThrows(IllegalArgumentException.class, () -> RiskCalculator.computeVaR(rv, 0.0));
		assertThrows(IllegalArgumentException.class, () -> RiskCalculator.computeCVaR(rv, 1.0));
	}

	@Test
	@DisplayName("RandomVariable – ties at VaR threshold produce sensible CVaR")
	void rv_ties_at_threshold() {
		double[] losses = new double[100];
		Arrays.fill(losses, 0.0);
		for (int i = 95; i < 100; i++) losses[i] = 10.0; // worst 5% exactly 10
		RandomVariable rv = new RandomVariableFromDoubleArray(0.0, losses);

		double alpha = 0.05;
		double var5  = RiskCalculator.computeVaR(rv, alpha);   // should be 10
		double cvar5 = RiskCalculator.computeCVaR(rv, alpha);  // should be 10

		assertEquals(10.0, var5, 1e-9);
		assertEquals(10.0, cvar5, 1e-9);
	}

	@Test
	@DisplayName("RandomVariable – null inputs throw NPE")
	void rv_null_checks() {
		assertThrows(NullPointerException.class, () -> RiskCalculator.computeVaR((RandomVariable) null, 0.05));
		assertThrows(NullPointerException.class, () -> RiskCalculator.computeCVaR((RandomVariable) null, 0.05));
	}

	@Test
	@DisplayName("RandomVariable – single-path: VaR == CVaR == value")
	void rv_single_path_equalities() {
		RandomVariable one = new RandomVariableFromDoubleArray(0.0, new double[]{42.0});
		double var = RiskCalculator.computeVaR(one, 0.10);
		double es  = RiskCalculator.computeCVaR(one, 0.10);
		assertEquals(42.0, var, 1e-12);
		assertEquals(42.0, es, 1e-12);
	}

	/* =======================================================================
	   PORTFOLIO HELPERS
	 * ======================================================================= */

	@Test
	@DisplayName("Portfolio – aggregateLosses sums path-by-path; portfolio VaR/CVaR computed")
	void portfolio_helpers() {
		RandomVariable l1 = new RandomVariableFromDoubleArray(0.0, new double[]{10, 20, 30});
		RandomVariable l2 = new RandomVariableFromDoubleArray(0.0, new double[]{ 5, 15, 25});
		RandomVariable l3 = new RandomVariableFromDoubleArray(0.0, new double[]{ 2,  4,  6});

		RandomVariable portfolioLoss = RiskCalculator.aggregateLosses(l1, l2, l3);
		// expected pathwise sum: [17, 39, 61]
		assertArrayEquals(new double[]{17.0, 39.0, 61.0}, portfolioLoss.getRealizations(), 1e-12);

		double var33  = RiskCalculator.computeVaR(portfolioLoss, 1.0/3.0);   // (1 - 1/3) = 2/3 quantile
		double cvar33 = RiskCalculator.computeCVaR(portfolioLoss, 1.0/3.0);  // average of tail >= VaR

		// sorted [17, 39, 61]; with tiny samples, VaR at 2/3 may be 39 or 61 depending on quantile rule
		assertTrue(Math.abs(var33 - 39.0) < 1e-12 || Math.abs(var33 - 61.0) < 1e-12,
				"VaR(α=1/3) should be either 39 or 61 for 3-point sample");

		// If VaR=61 → tail={61} → CVaR=61; If VaR=39 → tail={39,61} → CVaR=50
		double expectedCvar = (Math.abs(var33 - 61.0) < 1e-12) ? 61.0 : 50.0;
		assertEquals(expectedCvar, cvar33, 1e-12);
	}

	@Test
	@DisplayName("Portfolio – list-based convenience methods")
	void portfolio_list_helpers() {
		RandomVariable l1 = new RandomVariableFromDoubleArray(0.0, new double[]{0,  0, 100});
		RandomVariable l2 = new RandomVariableFromDoubleArray(0.0, new double[]{0, 50,   0});

		double var50  = RiskCalculator.computePortfolioVaR(List.of(l1, l2), 0.50);   // 50% tail
		double cvar50 = RiskCalculator.computePortfolioCVaR(List.of(l1, l2), 0.50);  // average of worst half

		// portfolio paths = [0, 50, 100] → VaR50% could be 50 (lower) or 100 (upper) depending on convention
		assertTrue(var50 == 50.0 || var50 == 100.0);
		assertTrue(cvar50 >= var50);
	}

	@Test
	@DisplayName("Portfolio – null and empty list validations")
	void portfolio_null_empty_validations() {
		assertThrows(NullPointerException.class, () -> RiskCalculator.computePortfolioVaR(null, 0.1));
		assertThrows(NullPointerException.class, () -> RiskCalculator.computePortfolioCVaR(null, 0.1));
		assertThrows(IllegalArgumentException.class, () -> RiskCalculator.computePortfolioVaR(List.of(), 0.1));
		assertThrows(IllegalArgumentException.class, () -> RiskCalculator.computePortfolioCVaR(List.of(), 0.1));
	}

	@Test
	@DisplayName("Portfolio – list contains null element")
	void portfolio_list_contains_null() {
		RandomVariable ok = new RandomVariableFromDoubleArray(0.0, new double[]{1,2,3});
		assertThrows(NullPointerException.class, () -> RiskCalculator.computePortfolioVaR(List.of(ok, null), 0.25));
		assertThrows(NullPointerException.class, () -> RiskCalculator.computePortfolioCVaR(List.of(ok, null), 0.25));
	}

	/* =======================================================================
	   FROM-PRODUCT HELPERS (model + product)
	 * ======================================================================= */

	//Minimal product that returns a fixed payoff RandomVariable (P&L).
	static final class DummyProduct extends AbstractAssetMonteCarloProduct {
		private final RandomVariable payoff;
		DummyProduct(RandomVariable payoff) { this.payoff = payoff; }
		@Override
		public RandomVariable getValue(double evaluationTime, AssetModelMonteCarloSimulationModel model) {
			// Ignore model; return the fixed P&L distribution
			return payoff;
		}
	}

	//Model stub-none of its methods are used by the helpers.
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
		@Override public int getNumberOfAssets() { return 1; }
		@Override public TimeDiscretization getTimeDiscretization() { throw new UnsupportedOperationException(); }
		@Override public AssetModelMonteCarloSimulationModel getCloneWithModifiedSeed(int seed) { return this; }
		@Override public AssetModelMonteCarloSimulationModel getCloneWithModifiedData(Map<String, Object> dataModified) { return this; }
		@Override public java.time.LocalDateTime getReferenceDate() { throw new UnsupportedOperationException(); }
		@Override public RandomVariable getRandomVariableForConstant(double value) { throw new UnsupportedOperationException(); }
	}

	@Test
	@DisplayName("FromProduct – VaR/CVaR equals RV-based manual conversion of P&L to losses (-P&L)")
	void fromProduct_matches_manual_conversion() throws Exception {
		// P&L (profit>0, loss<0). Helpers convert to losses by multiplying -1.
		double[] pnl = {-10, 0, 20, -5, 100};
		RandomVariable payoff = new RandomVariableFromDoubleArray(0.0, pnl);

		DummyProduct product = new DummyProduct(payoff);
		DummyModel model = new DummyModel();

		double alpha = 0.20;

		// Values produced by the helpers (losses = -P&L inside the helper)
		double varFromHelper  = RiskCalculator.computeVaRFromProduct(model, product, 0.0, alpha);
		double cvarFromHelper = RiskCalculator.computeCVaRFromProduct(model, product, 0.0, alpha);

		// Build the baseline using the SAME quantile convention (RV-based)
		double[] loss = Arrays.stream(pnl).map(x -> -x).toArray();              // convert P&L -> loss
		RandomVariable lossRV = new RandomVariableFromDoubleArray(0.0, loss);   // use RandomVariable
		double varBaseline  = RiskCalculator.computeVaR(lossRV, alpha);         // not the array overload
		double cvarBaseline = RiskCalculator.computeCVaR(lossRV, alpha);

		assertEquals(varBaseline,  varFromHelper,  1e-12);
		assertEquals(cvarBaseline, cvarFromHelper, 1e-12);
		assertTrue(cvarFromHelper >= varFromHelper);
	}


	@Test
	@DisplayName("FromProduct – null model/product validations")
	void fromProduct_null_validations() {
		RandomVariable payoff = new RandomVariableFromDoubleArray(0.0, new double[]{1,2,3});
		DummyProduct product = new DummyProduct(payoff);
		DummyModel model = new DummyModel();

		assertThrows(NullPointerException.class, () ->
				RiskCalculator.computeVaRFromProduct(null, product, 0.0, 0.10));
		assertThrows(NullPointerException.class, () ->
				RiskCalculator.computeVaRFromProduct(model, null, 0.0, 0.10));
		assertThrows(NullPointerException.class, () ->
				RiskCalculator.computeCVaRFromProduct(null, product, 0.0, 0.10));
		assertThrows(NullPointerException.class, () ->
				RiskCalculator.computeCVaRFromProduct(model, null, 0.0, 0.10));
	}

	@Test
	@DisplayName("FromProduct – invalid alpha")
	void fromProduct_invalid_alpha() {
		RandomVariable payoff = new RandomVariableFromDoubleArray(0.0, new double[]{1,2,3});
		DummyProduct product = new DummyProduct(payoff);
		DummyModel model = new DummyModel();

		assertThrows(IllegalArgumentException.class, () ->
				RiskCalculator.computeVaRFromProduct(model, product, 0.0, 0.0));
		assertThrows(IllegalArgumentException.class, () ->
				RiskCalculator.computeCVaRFromProduct(model, product, 0.0, 1.0));
	}
}
