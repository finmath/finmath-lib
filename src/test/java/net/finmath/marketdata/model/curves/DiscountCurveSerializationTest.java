/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 31.03.2014
 */
package net.finmath.marketdata.model.curves;

import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * Test serialization and de-serialization of a DiscountCurveInterpolation.
 *
 * @author Christian Fries
 */
public class DiscountCurveSerializationTest {

	@Test
	public void test() {
		final int		numberOfPeriods		= 16;
		final double	periodLength		= 0.5;

		// Create the tenor discretization
		final TimeDiscretization timeDiscretization = new TimeDiscretizationFromArray(0.0, numberOfPeriods, periodLength);

		/*
		 * Create a discount curve.
		 */
		final double[] liborInitialValues = new double[numberOfPeriods];
		java.util.Arrays.fill(liborInitialValues, 0.10);

		final DiscountCurve discountFactors = DiscountCurveInterpolation.createDiscountFactorsFromForwardRates("DiscountCurve", timeDiscretization, liborInitialValues);

		/*
		 * Serialize to a byte stream (replace the ByteArrayOutputStream by an FileOutputStream to serialize to a file)
		 */
		byte[] serializedObject = null;
		try {
			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			final ObjectOutputStream oos = new ObjectOutputStream( baos );
			oos.writeObject(discountFactors);
			serializedObject = baos.toByteArray();
		} catch (final IOException e) {
			fail("Serialization failed with exception " + e.getMessage());
		}

		/*
		 * De-serialize from a byte stream
		 */
		DiscountCurve discountFactorsClone = null;
		try {
			final ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(serializedObject) );
			discountFactorsClone = (DiscountCurve)ois.readObject();
		} catch (IOException | ClassNotFoundException e) {
			fail("Deserialization failed with exception " + e.getMessage());
		}


		/*
		 * The loaded curve will be a deep copy of the original curve.
		 */

		// Check equality
		for(double time=0; time < 10; time += 0.1) {
			Assert.assertEquals("Discount factor for maturity " + time, discountFactors.getDiscountFactor(time), discountFactorsClone.getDiscountFactor(time), 0.0);
		}
	}

}
