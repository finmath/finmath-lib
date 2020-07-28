package net.finmath.montecarlo.interestrate.products.components;

import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.finmath.montecarlo.interestrate.models.FundingCapacity;
import net.finmath.montecarlo.interestrate.models.FundingCapacity.DefaultFactors;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;

/**
 * @author Christian Fries
 *
 */
public class FundingCapacityTest {

	@Test
	public void test() {

		final SortedMap<Double, Double> instSurvProb = new TreeMap<Double, Double>();
		instSurvProb.put(1.5, 0.9);
		instSurvProb.put(2.0, 0.8);
		instSurvProb.put(5.0, 0.7);
		instSurvProb.put(10.0, 0.5);

		final FundingCapacity fc = new FundingCapacity("EUR", new Scalar(0.0), instSurvProb);

		final DefaultFactors df = fc.getDefaultFactors(1.0, new Scalar(1.0));

		final RandomVariable sp = df.getSurvivalProbability();
		Assertions.assertEquals(0.9, sp.doubleValue());

		final RandomVariable dc = df.getDefaultCompensation();
		Assertions.assertEquals(1.0/0.9, dc.doubleValue());


		final DefaultFactors df2 = fc.getDefaultFactors(1.0, new Scalar(1.0));

		final RandomVariable sp2 = df2.getSurvivalProbability();
		Assertions.assertEquals(0.85, sp2.doubleValue(), 1E-12);

		final RandomVariable dc2 = df2.getDefaultCompensation();
		Assertions.assertEquals((1.5-1.0/0.9)+0.5+(1-(1.5-1.0/0.9)*0.9-0.5*0.8)/0.7, dc2.doubleValue(), 1E-12);


		final RandomVariable sp3 = fc.getDefaultFactors(1.0, new Scalar(3.0)).getSurvivalProbability();
		Assertions.assertEquals(0.7, sp3.doubleValue(), 1E-12);

		final RandomVariable sp4 = fc.getDefaultFactors(1.0, new Scalar(-4.0)).getSurvivalProbability();
		Assertions.assertEquals((3*0.7+0.5*0.8+0.5*0.9)/4, sp4.doubleValue(), 1E-12);

		final RandomVariable level = fc.getCurrentFundingLevel();
		Assertions.assertEquals(1.0, level.doubleValue(), 1E-12);
	}
}
