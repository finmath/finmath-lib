package net.finmath.marketdata.model.curves;

import java.time.LocalDate;
import java.time.Month;

import org.junit.Assert;
import org.junit.Test;

public class DiscountCurveNelsonSiegelSvenssonTest {

	@Test
	public void test() {
		
		LocalDate referenceDate = LocalDate.of(2014, Month.SEPTEMBER, 16); 
		double[] nssParameters = new double[] { 0.02 , -0.01, 0.16, -0.17, 4.5, 3.5 };

		DiscountCurveInterface discountCurve = new DiscountCurveNelsonSiegelSvensson("EUR Curve", referenceDate, nssParameters, 1.0);

		double df = discountCurve.getDiscountFactor(10.0);

		Assert.assertEquals("Discount factor", 0.847664288, df, 1E-8);
	}
}
