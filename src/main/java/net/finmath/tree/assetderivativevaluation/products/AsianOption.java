package net.finmath.tree.assetderivativevaluation.products;

import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;
import net.finmath.tree.TreeModel;

import java.util.HashSet;
import java.util.Set;
import java.util.function.DoubleUnaryOperator;

/**
 * Fixed-strike Asian option priced on a recombining tree.
 * The averaging times are a subset of the tree time grid.
 *
 * The running average includes S0 if 0.0 is contained in the averaging times.
 */
public class AsianOption extends AbstractPathDependentProduct {

    private final TimeDiscretization averagingTimes;
    private final Set<Integer> averagingIndices;
    private final int numberOfAveragingTimes;

    /**
     * @param maturity        Option maturity T
     * @param averagingTimes  Discrete averaging times (subset of model grid)
     * @param payOffFunction  Payoff applied to the arithmetic average
     */
    public AsianOption(
            double maturity,
            TimeDiscretization averagingTimes,
            DoubleUnaryOperator payOffFunction) {
        super(maturity, payOffFunction);
        this.averagingTimes = averagingTimes;
        this.averagingIndices = new HashSet<>();
        this.numberOfAveragingTimes = averagingTimes.getNumberOfTimes();
    }

    @Override
    public RandomVariable[] getValues(double evaluationTime, TreeModel model) {

        validate(evaluationTime, model);

        int maturityIndex   = timeToIndex(getMaturity(), model);
        int evaluationIndex = timeToIndex(evaluationTime, model);

        // Map averaging times → tree indices
        averagingIndices.clear();
        for (int i = 0; i < averagingTimes.getNumberOfTimes(); i++) {
            int idx = timeToIndex(averagingTimes.getTime(i), model);
            averagingIndices.add(idx);
        }

        // ---------- Forward construction of running sum ----------
        RandomVariable[] runningSum = new RandomVariable[maturityIndex + 1];

        for (int k = 0; k <= maturityIndex; k++) {
            RandomVariable Sk = model.getSpotAtGivenTimeIndexRV(k);

            if (k == 0) {
                runningSum[k] = averagingIndices.contains(0)
                        ? Sk
                        : Sk.mult(0.0);
            } else {
                runningSum[k] = runningSum[k - 1];
                if (averagingIndices.contains(k)) {
                    runningSum[k] = runningSum[k].add(Sk);
                }
            }
        }

        // ---------- Payoff at maturity ----------
        RandomVariable average =
                runningSum[maturityIndex].div(numberOfAveragingTimes);

        RandomVariable value =
                model.getTransformedValuesAtGivenTimeRV(
                        getMaturity(),
                        x -> getPayOffFunction().applyAsDouble(x)
                );

        // Replace payoff argument with average
        value = average.apply(getPayOffFunction());

        // ---------- Backward induction ----------
        RandomVariable[] values = new RandomVariable[maturityIndex - evaluationIndex + 1];
        values[values.length - 1] = value;

        for (int k = maturityIndex - 1; k >= evaluationIndex; k--) {
            value = model.getConditionalExpectationRV(value, k);
            values[k - evaluationIndex] = value;
        }

        return values;
    }
}
