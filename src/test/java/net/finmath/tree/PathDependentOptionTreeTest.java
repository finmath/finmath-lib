package net.finmath.tree;

import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.BrownianMotionFromMersenneRandomNumbers;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel;
import net.finmath.montecarlo.assetderivativevaluation.MonteCarloAssetModel;
import net.finmath.montecarlo.assetderivativevaluation.models.BlackScholesModel;
import net.finmath.montecarlo.assetderivativevaluation.products.AsianOption; // Monte Carlo Asian
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.montecarlo.process.MonteCarloProcessFromProcessModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

// Tree side:
import net.finmath.tree.assetderivativevaluation.models.CoxRossRubinsteinModel;


/**
 * Compares a fixed-strike Asian option price between
 *  - Monte Carlo (Black-Scholes via Euler scheme)
 *  - Binomial tree (CRR) with a FULL non-recombining Asian product (exponential node growth).
 *
 * The tree AsianOption is assumed to be your class:
 *   net.finmath.tree.assetderivativevaluation.products.AsianOption
 *
 * and it is assumed to support:
 *  - fixed strike call/put
 *  - a subset of fixing time indices (or times -> indices).
 */
public class PathDependentOptionTreeTest {

    @Test
    public void testAsianOptionTreeVsMonteCarlo() throws CalculationException {

        // -------------------------
        // Common contract parameters
        // -------------------------
        final double S0 = 100.0;
        final double r  = 0.04;
        final double sigma = 0.25;

        final double maturity = 1.0;
        final double strike   = 90.0;

        // -------------------------
        // Discretization
        // -------------------------
        // Keep time steps modest because your tree Asian is exponential: 2^N nodes at maturity.
        final int numberOfTimeSteps = 12;      // => 2^12 = 4096 terminal nodes
        final double dt = maturity / numberOfTimeSteps;

        // Averaging schedule (subset of the grid):
        // Example: fixings from time index 1..N inclusive (exclude t=0).
        final int[] fixingTimeIndices = IntStream.rangeClosed(0, numberOfTimeSteps).toArray();

        // Convert fixing indices to fixing times for the MC product
        final double[] fixingTimes = IntStream.of(fixingTimeIndices)
                .mapToDouble(k -> k * dt)
                .toArray();

        // -------------------------
        // Monte Carlo pricing
        // -------------------------
        final int numberOfPaths = 200_000;
        final int seed = 31415;

        final TimeDiscretization timeDiscretization =
                new TimeDiscretizationFromArray(0.0, numberOfTimeSteps, dt);

        final BlackScholesModel bsModel = new BlackScholesModel(S0, r, sigma);

        final MonteCarloProcessFromProcessModel process =
                new EulerSchemeFromProcessModel(
                        bsModel,
                        new BrownianMotionFromMersenneRandomNumbers(
                                timeDiscretization,
                                1,              // numberOfFactors
                                numberOfPaths,
                                seed
                        )
                );

        final AssetModelMonteCarloSimulationModel mcSimulation =
                new MonteCarloAssetModel(bsModel, process);

        final AsianOption mcAsian =
                new AsianOption(
                        maturity,
                        strike,
                        new TimeDiscretizationFromArray(fixingTimes)
                );

        final RandomVariable mcValueRV = mcAsian.getValue(0.0, mcSimulation);
        final double mcValue = mcValueRV.getAverage();

        // -------------------------
        // Tree pricing (CRR + AsianOption)
        // -------------------------
        final TreeModel treeModel =
                new CoxRossRubinsteinModel(S0, r, sigma, maturity, dt);

        final net.finmath.tree.assetderivativevaluation.products.AsianOption treeAsian =
                new net.finmath.tree.assetderivativevaluation.products.AsianOption(
                        maturity,
                        strike,
                        fixingTimeIndices
                );

        final double treeValue = treeAsian.getValue(treeModel);

        // -------------------------
        // Comparison
        // -------------------------
        System.out.println("Asian option (MC BS)   : " + mcValue);
        System.out.println("Asian option (Tree CRR): " + treeValue);
        System.out.println("Abs difference         : " + Math.abs(mcValue - treeValue));

        // Tolerance: MC error ~ O(1/sqrt(paths)). With 200k paths, this is typically a few 1e-3..1e-2.
        // Tree has discretization error too (CRR + dt).
        final double tolerance = 1.0e-1;

        Assert.assertEquals("Tree vs Monte Carlo price mismatch", mcValue, treeValue, tolerance);
    }
}
