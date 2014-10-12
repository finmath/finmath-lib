package net.finmath.marketdata.model.curves;

import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.junit.Test;

public class DiscountCurveNelsonSiegelSvenssonTest {

	@Test
	public void test() {
		Calendar referenceDate = new GregorianCalendar(2014, Calendar.SEPTEMBER, 16);
		double[] nssParameters = new double[] { 0.02 , -0.01, 0.16, -0.17, 4.5, 3.5 };

		DiscountCurveInterface discountCurve = new DiscountCurveNelsonSiegelSvensson("EUR Curve", referenceDate, nssParameters, 1.0);

		double df = discountCurve.getDiscountFactor(10.0);
		
		assertTrue(Math.abs(df-0.847664288) < 1E-8);
	}
}
