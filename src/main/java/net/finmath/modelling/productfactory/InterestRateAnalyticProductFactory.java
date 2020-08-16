package net.finmath.modelling.productfactory;

import java.time.LocalDate;

import net.finmath.marketdata.products.AnalyticProduct;
import net.finmath.marketdata.products.Swap;
import net.finmath.marketdata.products.SwapLeg;
import net.finmath.modelling.DescribedProduct;
import net.finmath.modelling.InterestRateProductDescriptor;
import net.finmath.modelling.ProductDescriptor;
import net.finmath.modelling.ProductFactory;
import net.finmath.modelling.descriptor.InterestRateSwapLegProductDescriptor;
import net.finmath.modelling.descriptor.InterestRateSwapProductDescriptor;

/**
 * Product factory of interest rate derivatives for use with an analytic model.
 *
 * @author Christian Fries
 * @author Roland Bachl
 */
public class InterestRateAnalyticProductFactory implements ProductFactory<InterestRateProductDescriptor> {

	private final LocalDate 						referenceDate;


	/**
	 * Initialize the factory with the given referenceDate.
	 *
	 * @param referenceDate To be used when converting absolute dates to relative dates in double.
	 */
	public InterestRateAnalyticProductFactory(final LocalDate referenceDate) {
		super();
		this.referenceDate = referenceDate;
	}

	@Override
	public DescribedProduct<? extends InterestRateProductDescriptor> getProductFromDescriptor(final ProductDescriptor descriptor) {
		if(descriptor instanceof InterestRateSwapLegProductDescriptor) {
			final InterestRateSwapLegProductDescriptor swapLeg = (InterestRateSwapLegProductDescriptor) descriptor;
			final DescribedProduct<InterestRateSwapLegProductDescriptor> product = new SwapLeg(swapLeg.getLegScheduleDescriptor().getSchedule(referenceDate),
					swapLeg.getForwardCurveName(), swapLeg.getNotionals(), swapLeg.getSpreads(), swapLeg.getDiscountCurveName(),
					swapLeg.isNotionalExchanged());
			return product;
		}
		else if(descriptor instanceof InterestRateSwapProductDescriptor){
			final InterestRateSwapProductDescriptor swap = (InterestRateSwapProductDescriptor) descriptor;
			InterestRateProductDescriptor legDescriptor = swap.getLegReceiver();
			final AnalyticProduct legReceiver =  (AnalyticProduct) getProductFromDescriptor(legDescriptor);
			legDescriptor = swap.getLegPayer();
			final AnalyticProduct legPayer = (AnalyticProduct) getProductFromDescriptor(legDescriptor);
			final DescribedProduct<InterestRateSwapProductDescriptor> product = new Swap(legReceiver, legPayer);
			return product;
		}
		else {
			final String name = descriptor.name();
			throw new IllegalArgumentException("Unsupported product type " + name);
		}
	}

}
