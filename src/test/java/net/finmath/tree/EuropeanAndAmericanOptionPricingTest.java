package net.finmath.tree;


import org.junit.jupiter.api.Test;

import net.finmath.tree.assetderivativevaluation.models.BoyleTrinomial;
import net.finmath.tree.assetderivativevaluation.models.CoxRossRubinsteinModel;
import net.finmath.tree.assetderivativevaluation.models.JarrowRuddModel;
import net.finmath.tree.assetderivativevaluation.products.EuNonPathDependent;
import net.finmath.tree.assetderivativevaluation.products.UsNonPathDependent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EuropeanAndAmericanOptionPricingTest {

	@Test
	void testNonPathDep_Call() {

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



		EuNonPathDependent euCall = new EuNonPathDependent(maturity, s -> Math.max(s - strike, 0.0));
		UsNonPathDependent usCall = new UsNonPathDependent(maturity, s -> Math.max(s - strike, 0.0));


		double euCRR = euCall.getValue(crr);
		double euJR = euCall.getValue(0.0,jr).getAverage();
		double euTRI = euCall.getValue(0.0,tri).getAverage();

		double usCRR = usCall.getValue(0.0, crr).getAverage();
		double usJR = usCall.getValue(0.0,jr).getAverage();
		double usTRI = usCall.getValue(0.0,tri).getAverage();
		System.out.println(euCRR + " " + euJR + " " + euTRI);


		assertTrue(usTRI + 1e-12 >= euTRI, "US(Call) TRI must be  >= EU(Call) TRI");

		assertEquals(euCRR, euTRI, 2*tol, "EU(Call) CRR vs TRI difference beyond tolerance");


		assertEquals(euJR, euCRR, 2*tol, "EU(Call) JR vs CRR differenza oltre tolleranza");
		assertEquals(euJR, euTRI, 2*tol, "EU(Call) JR vs TRI differenza oltre tolleranza");

		assertEquals(usJR, usCRR, 2*tol, "US(Call) JR vs CRR differenza oltre tolleranza");
		assertEquals(usJR, usTRI, 2*tol, "US(Call) JR vs TRI differenza oltre tolleranza");

		assertTrue(usCRR + 1e-12 >= euCRR, "US(Call) CRR must be >= EU(Call) CRR");
	}

	@Test
	void testNonPathDep_Put() {
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



		EuNonPathDependent euPut = new EuNonPathDependent(maturity, s -> Math.max(strike - s, 0.0));
		UsNonPathDependent usPut = new UsNonPathDependent(maturity, s -> Math.max(strike - s, 0.0));


		double euCRR = euPut.getValue(0.0, crr).getAverage();
		double euJR  = euPut.getValue(0.0, jr).getAverage();
		double euTRI = euPut.getValue(0.0, tri).getAverage();


		double usCRR = usPut.getValue(0.0, crr).getAverage();
		double usJR  = usPut.getValue(0.0,jr).getAverage();
		double usTRI = usPut.getValue(0.0, tri).getAverage();

		assertTrue(usCRR + 1e-12 >= euCRR, "US(Put) CRR must be >= EU(Put) CRR");

		assertEquals(euJR, euCRR, 2*tol, "EU(Put) JR vs CRR differenza oltre tolleranza");
		assertEquals(euJR, euTRI, 2*tol, "EU(Put) JR vs TRI differenza oltre tolleranza");

		assertEquals(usJR, usCRR, 2*tol, "US(Put) JR vs CRR differenza oltre tolleranza");
		assertEquals(usJR, usTRI, 2*tol, "US(Put) JR vs TRI differenza oltre tolleranza");

		assertTrue(usTRI + 1e-12 >= euTRI, "US(Put) TRI must be >= EU(Put) TRI");
		assertEquals(euCRR, euTRI, 2*tol, "EU(Put) CRR vs TRI difference beyond tolerance");
	}
}