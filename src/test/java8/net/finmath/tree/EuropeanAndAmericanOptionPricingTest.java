package net.finmath.tree;


import org.junit.Assert;
import org.junit.Test;

import net.finmath.modelling.products.CallOrPut;
import net.finmath.tree.assetderivativevaluation.models.BoyleTrinomial;
import net.finmath.tree.assetderivativevaluation.models.CoxRossRubinsteinModel;
import net.finmath.tree.assetderivativevaluation.models.JarrowRuddModel;
import net.finmath.tree.assetderivativevaluation.products.AmericanNonPathDependent;
import net.finmath.tree.assetderivativevaluation.products.EuropeanOption;

public class EuropeanAndAmericanOptionPricingTest {

	@Test
	public void testNonPathDep_Call() {

		final double spot = 100.0;
		final double rate = 0.05;
		final double vol = 0.2;
		final double maturity = 1.0;
		final int steps = 100;
		final double strike = 110.0;
		final double tol = 1e-2;

		final CoxRossRubinsteinModel crr = new CoxRossRubinsteinModel(spot, rate, vol, maturity, steps);
		final JarrowRuddModel jr = new JarrowRuddModel(spot,rate,vol,maturity,steps);
		final BoyleTrinomial tri = new BoyleTrinomial(spot,rate,vol,maturity,steps);


		final EuropeanOption euCall = new EuropeanOption(maturity, strike, CallOrPut.CALL);
		final AmericanNonPathDependent usCall = new AmericanNonPathDependent(maturity, s -> Math.max(s - strike, 0.0));


		final double euCRR = euCall.getValue(crr);
		final double euJR = euCall.getValue(0.0,jr).getAverage();
		final double euTRI = euCall.getValue(0.0,tri).getAverage();

		final double usCRR = usCall.getValue(0.0, crr).getAverage();
		final double usJR = usCall.getValue(0.0,jr).getAverage();
		final double usTRI = usCall.getValue(0.0,tri).getAverage();
		System.out.println(euCRR + " " + euJR + " " + euTRI);

		//"US(Call) TRI must be  >= EU(Call) TRI"
		Assert.assertTrue(usTRI + 1e-12 >= euTRI);
		//"EU(Call) CRR vs TRI difference beyond tolerance"
		Assert.assertEquals(euCRR, euTRI, 2*tol);

		//"EU(Call) JR vs CRR difference beyond tol"
		Assert.assertEquals(euJR, euCRR, 2*tol);
		//"EU(Call) JR vs TRI difference beyond tol"
		Assert.assertEquals(euJR, euTRI, 2*tol);
		//"US(Call) JR vs CRR difference beyond tol"
		Assert.assertEquals(usJR, usCRR, 2*tol);
		//"US(Call) JR vs TRI difference beyond tol"
		Assert.assertEquals(usJR, usTRI, 2*tol);
		//"US(Call) CRR must be >= EU(Call) CRR"
		Assert.assertTrue(usCRR + 1e-12 >= euCRR);
	}

	@Test
	public void testNonPathDep_Put() {
		final double spot = 100.0;
		final double rate = 0.03;
		final double vol = 0.25;
		final double maturity = 2.0;
		final int steps = 300;
		final double strike = 110.0;
		final double tol = 2e-2;

		final CoxRossRubinsteinModel crr = new CoxRossRubinsteinModel(spot, rate, vol, maturity, steps);
		final JarrowRuddModel jr = new JarrowRuddModel(spot,rate,vol,maturity,steps);
		final BoyleTrinomial tri = new BoyleTrinomial(spot, rate, vol, maturity, steps);



		final EuropeanOption euPut = new EuropeanOption(maturity, strike, CallOrPut.PUT);
		final AmericanNonPathDependent usPut = new AmericanNonPathDependent(maturity, s -> Math.max(strike - s, 0.0));


		final double euCRR = euPut.getValue(0.0, crr).getAverage();
		final double euJR  = euPut.getValue(0.0, jr).getAverage();
		final double euTRI = euPut.getValue(0.0, tri).getAverage();


		final double usCRR = usPut.getValue(0.0, crr).getAverage();
		final double usJR  = usPut.getValue(0.0,jr).getAverage();
		final double usTRI = usPut.getValue(0.0, tri).getAverage();
		//"US(Put) CRR must be >= EU(Put) CRR"
		Assert.assertTrue(usCRR + 1e-12 >= euCRR);
		//"EU(Put) JR vs CRR difference beyond tol"
		Assert.assertEquals(euJR, euCRR, 2*tol);
		//"EU(Put) JR vs TRI difference beyond tol"
		Assert.assertEquals(euJR, euTRI, 2*tol);
		//"US(Put) JR vs CRR difference beyond tol"
		Assert.assertEquals(usJR, usCRR, 2*tol);
		//"US(Put) JR vs TRI difference beyond tol"
		Assert.assertEquals(usJR, usTRI, 2*tol);
		//"US(Put) TRI must be >= EU(Put) TRI"
		Assert.assertTrue(usTRI + 1e-12 >= euTRI);
		//"EU(Put) CRR vs TRI difference beyond tolerance"
		Assert.assertEquals(euCRR, euTRI, 2*tol);
	}
}
