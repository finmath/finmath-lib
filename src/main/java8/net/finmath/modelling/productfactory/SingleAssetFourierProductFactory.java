/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2018
 */

package net.finmath.modelling.productfactory;

import java.time.LocalDate;

import net.finmath.fouriermethod.products.DigitalOption;
import net.finmath.fouriermethod.products.EuropeanOption;
import net.finmath.modelling.DescribedProduct;
import net.finmath.modelling.ProductDescriptor;
import net.finmath.modelling.ProductFactory;
import net.finmath.modelling.SingleAssetProductDescriptor;
import net.finmath.modelling.descriptor.SingleAssetDigitalOptionProductDescriptor;
import net.finmath.modelling.descriptor.SingleAssetEuropeanOptionProductDescriptor;
import net.finmath.time.FloatingpointDate;

/**
 * Product factory of single asset derivatives for use with a Fourier method based model.
 *
 * @author Christian Fries
 * @author Roland Bachl
 * @version 1.0
 */
public class SingleAssetFourierProductFactory implements ProductFactory<SingleAssetProductDescriptor> {

	private final LocalDate referenceDate;

	/**
	 * Create the product factory.
	 *
	 * @param referenceDate To be used when converting absolute dates to relative dates in double.
	 */
	public SingleAssetFourierProductFactory(final LocalDate referenceDate) {
		this.referenceDate = referenceDate;
	}

	@Override
	public DescribedProduct<? extends SingleAssetProductDescriptor> getProductFromDescriptor(final ProductDescriptor descriptor) {

		if(descriptor instanceof SingleAssetEuropeanOptionProductDescriptor) {
			final DescribedProduct<SingleAssetEuropeanOptionProductDescriptor> product = new EuropeanOptionFourierMethod((SingleAssetEuropeanOptionProductDescriptor) descriptor, referenceDate);
			return product;
		}
		else if(descriptor instanceof SingleAssetDigitalOptionProductDescriptor) {
			final DescribedProduct<SingleAssetDigitalOptionProductDescriptor> product = new DigitalOptionFourierMethod((SingleAssetDigitalOptionProductDescriptor) descriptor, referenceDate);
			return product;
		}
		else {
			final String name = descriptor.name();
			throw new IllegalArgumentException("Unsupported product type " + name);
		}

	}


	/**
	 * Fourier method based implementation of a European option from a product descriptor.
	 *
	 * @author Christian Fries
	 * @author Roland Bachl
	 */
	public static class EuropeanOptionFourierMethod extends EuropeanOption  implements DescribedProduct<SingleAssetEuropeanOptionProductDescriptor> {

		private final SingleAssetEuropeanOptionProductDescriptor descriptor;

		/**
		 * Create the product from a descriptor.
		 *
		 * @param descriptor A descriptor of the product.
		 * @param referenceDate the reference date to be used when converting dates to doubles.
		 */
		public EuropeanOptionFourierMethod(final SingleAssetEuropeanOptionProductDescriptor descriptor, final LocalDate referenceDate) {
			super(descriptor.getUnderlyingName(), FloatingpointDate.getFloatingPointDateFromDate(referenceDate, descriptor.getMaturity()), descriptor.getStrike());
			this.descriptor = descriptor;
		}

		@Override
		public SingleAssetEuropeanOptionProductDescriptor getDescriptor() {
			return descriptor;
		}

	}

	/**
	 * Fourier method based implementation of a digital option from a product descriptor.
	 *
	 * @author Christian Fries
	 * @author Roland Bachl
	 */
	public static class DigitalOptionFourierMethod extends DigitalOption  implements DescribedProduct<SingleAssetDigitalOptionProductDescriptor>{

		private final SingleAssetDigitalOptionProductDescriptor descriptor;

		/**
		 * Create product from descriptor.
		 *
		 * @param descriptor The descriptor of the product.
		 * @param referenceDate The reference date of the data for the valuation, used to convert absolute date to relative dates in double representation.
		 */
		public DigitalOptionFourierMethod(final SingleAssetDigitalOptionProductDescriptor descriptor, final LocalDate referenceDate) {
			super(FloatingpointDate.getFloatingPointDateFromDate(referenceDate, descriptor.getMaturity()), descriptor.getStrike());
			this.descriptor = descriptor;
		}

		@Override
		public SingleAssetDigitalOptionProductDescriptor getDescriptor() {
			return descriptor;
		}

	}
}
