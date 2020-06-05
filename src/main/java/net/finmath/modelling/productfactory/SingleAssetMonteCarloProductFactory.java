/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2018
 */

package net.finmath.modelling.productfactory;

import java.time.LocalDate;

import net.finmath.modelling.DescribedProduct;
import net.finmath.modelling.ProductDescriptor;
import net.finmath.modelling.ProductFactory;
import net.finmath.modelling.SingleAssetProductDescriptor;
import net.finmath.modelling.descriptor.SingleAssetDigitalOptionProductDescriptor;
import net.finmath.modelling.descriptor.SingleAssetEuropeanOptionProductDescriptor;
import net.finmath.montecarlo.assetderivativevaluation.products.DigitalOption;
import net.finmath.montecarlo.assetderivativevaluation.products.EuropeanOption;
import net.finmath.time.FloatingpointDate;

/**
 * Product factory of single asset derivatives for use with a Monte-Carlo method based model.
 *
 * @author Christian Fries
 * @author Roland Bachl
 * @version 1.0
 */
public class SingleAssetMonteCarloProductFactory implements ProductFactory<SingleAssetProductDescriptor> {

	private final LocalDate referenceDate;

	/**
	 * Create the product factory.
	 *
	 * @param referenceDate To be used when converting absolute dates to relative dates in double.
	 */
	public SingleAssetMonteCarloProductFactory(final LocalDate referenceDate) {
		this.referenceDate = referenceDate;
	}

	@Override
	public DescribedProduct<? extends SingleAssetProductDescriptor> getProductFromDescriptor(final ProductDescriptor descriptor) {

		if(descriptor instanceof SingleAssetEuropeanOptionProductDescriptor) {
			final DescribedProduct<SingleAssetEuropeanOptionProductDescriptor> product = new EuropeanOptionMonteCarlo((SingleAssetEuropeanOptionProductDescriptor) descriptor, referenceDate);
			return product;
		}
		else if(descriptor instanceof SingleAssetDigitalOptionProductDescriptor) {
			final DescribedProduct<SingleAssetDigitalOptionProductDescriptor> product = new DigitalOptionMonteCarlo((SingleAssetDigitalOptionProductDescriptor) descriptor, referenceDate);
			return product;
		}
		else {
			final String name = descriptor.name();
			throw new IllegalArgumentException("Unsupported product type " + name);
		}
	}


	/**
	 * Monte-Carlo method based implementation of a European option from a product descriptor.
	 *
	 * @author Christian Fries
	 * @author Roland Bachl
	 */
	public static class EuropeanOptionMonteCarlo extends EuropeanOption implements DescribedProduct<SingleAssetEuropeanOptionProductDescriptor> {

		private final SingleAssetEuropeanOptionProductDescriptor descriptor;

		/**
		 * Construct a product representing an European option on an asset S (where S the asset with index 0 from the model - single asset case).
		 *
		 * @param descriptor Implementation of SingleAssetEuropeanOptionProductDescriptor
		 * @param referenceDate The reference date to be used to convert absolute maturities to relative maturities.
		 */
		public EuropeanOptionMonteCarlo(final SingleAssetEuropeanOptionProductDescriptor descriptor, final LocalDate referenceDate) {
			super(descriptor.getUnderlyingName(), FloatingpointDate.getFloatingPointDateFromDate(referenceDate, descriptor.getMaturity()), descriptor.getStrike());
			this.descriptor = descriptor;
		}

		@Override
		public SingleAssetEuropeanOptionProductDescriptor getDescriptor() {
			return descriptor;
		}
	}

	/**
	 * Monte-Carlo method based implementation of a digital option from a product descriptor.
	 *
	 * @author Christian Fries
	 * @author Roland Bachl
	 */
	public static class DigitalOptionMonteCarlo extends DigitalOption implements DescribedProduct<SingleAssetDigitalOptionProductDescriptor>  {

		private final SingleAssetDigitalOptionProductDescriptor descriptor;

		/**
		 * Create product from descriptor.
		 *
		 * @param descriptor The descriptor of the product.
		 * @param referenceDate The reference date of the data for the valuation, used to convert absolute date to relative dates in double representation.
		 */
		public DigitalOptionMonteCarlo(final SingleAssetDigitalOptionProductDescriptor descriptor, final LocalDate referenceDate) {
			super(descriptor.getNameOfUnderlying(), FloatingpointDate.getFloatingPointDateFromDate(referenceDate, descriptor.getMaturity()), descriptor.getStrike());
			this.descriptor = descriptor;
		}

		@Override
		public SingleAssetDigitalOptionProductDescriptor getDescriptor() {
			return descriptor;
		}
	}
}
