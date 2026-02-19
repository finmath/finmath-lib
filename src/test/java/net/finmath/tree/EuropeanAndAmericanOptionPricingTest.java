package net.finmath.tree;


import org.junit.Assert;
import org.junit.Test;

import net.finmath.tree.assetderivativevaluation.models.BoyleTrinomial;
import net.finmath.tree.assetderivativevaluation.models.CoxRossRubinsteinModel;
import net.finmath.tree.assetderivativevaluation.models.JarrowRuddModel;
import net.finmath.tree.assetderivativevaluation.products.EuropeanNonPathDependent;
import net.finmath.tree.assetderivativevaluation.products.AmericanNonPathDependent;

public class EuropeanAndAmericanOptionPricingTest {

	@Test
	public void testNonPathDep_Call() {

		double spot = 100.0;
		double rate = 0.05;
		double vol = 0.2;
		double maturity = 1.0;
		int steps = 100;
		double strike = 110.0;
		double tol = 1e-2;

		CoxRossRubinsteinModel crr = new CoxRossRubinsteinModel(spot, rate, vol, maturity, steps);
		JarrowRuddModel jr = new JarrowRuddModel(spot,rate,vol,maturity,steps);
		BoyleTrinomial tri = new BoyleTrinomial(spot,rate,vol,maturity,steps);



		EuropeanNonPathDependent euCall = new EuropeanNonPathDependent(maturity, s -> Math.max(s - strike, 0.0));
		AmericanNonPathDependent usCall = new AmericanNonPathDependent(maturity, s -> Math.max(s - strike, 0.0));


		double euCRR = euCall.getValue(crr);
		double euJR = euCall.getValue(0.0,jr).getAverage();
		double euTRI = euCall.getValue(0.0,tri).getAverage();

		double usCRR = usCall.getValue(0.0, crr).getAverage();
		double usJR = usCall.getValue(0.0,jr).getAverage();
		double usTRI = usCall.getValue(0.0,tri).getAverage();
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
		double spot = 100.0;
		double rate = 0.03;
		double vol = 0.25;
		double maturity = 2.0;
		int steps = 300;
		double strike = 110.0;
		double tol = 2e-2;

		CoxRossRubinsteinModel crr = new CoxRossRubinsteinModel(spot, rate, vol, maturity, steps);
		JarrowRuddModel jr = new JarrowRuddModel(spot,rate,vol,maturity,steps);
		BoyleTrinomial tri = new BoyleTrinomial(spot, rate, vol, maturity, steps);



		EuropeanNonPathDependent euPut = new EuropeanNonPathDependent(maturity, s -> Math.max(strike - s, 0.0));
		AmericanNonPathDependent usPut = new AmericanNonPathDependent(maturity, s -> Math.max(strike - s, 0.0));


		double euCRR = euPut.getValue(0.0, crr).getAverage();
		double euJR  = euPut.getValue(0.0, jr).getAverage();
		double euTRI = euPut.getValue(0.0, tri).getAverage();


		double usCRR = usPut.getValue(0.0, crr).getAverage();
		double usJR  = usPut.getValue(0.0,jr).getAverage();
		double usTRI = usPut.getValue(0.0, tri).getAverage();
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