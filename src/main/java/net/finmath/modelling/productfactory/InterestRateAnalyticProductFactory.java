package net.finmath.modelling.productfactory;

import java.time.LocalDate;

import net.finmath.marketdata.products.AnalyticProductInterface;
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


	public InterestRateAnalyticProductFactory(LocalDate referenceDate) {
		super();
		this.referenceDate = referenceDate;
	}

	@Override
	public DescribedProduct<? extends InterestRateProductDescriptor> getProductFromDescriptor(ProductDescriptor descriptor) {
		if(descriptor instanceof InterestRateSwapLegProductDescriptor) {
			InterestRateSwapLegProductDescriptor swapLeg = (InterestRateSwapLegProductDescriptor) descriptor;
			DescribedProduct<InterestRateSwapLegProductDescriptor> product = new SwapLeg(swapLeg.getLegScheduleDescriptor().getSchedule(referenceDate),
					swapLeg.getForwardCurveName(), swapLeg.getNotionals(), swapLeg.getSpreads(), swapLeg.getDiscountCurveName(),
					swapLeg.isNotionalExchanged());
			return product;
		}
		else if(descriptor instanceof InterestRateSwapProductDescriptor){
			InterestRateSwapProductDescriptor swap = (InterestRateSwapProductDescriptor) descriptor;
			InterestRateProductDescriptor legDescriptor = (InterestRateProductDescriptor) swap.getLegReceiver();
			AnalyticProductInterface legReceiver =  (AnalyticProductInterface) getProductFromDescriptor(legDescriptor);
			legDescriptor = (InterestRateProductDescriptor) swap.getLegPayer();
			AnalyticProductInterface legPayer = (AnalyticProductInterface) getProductFromDescriptor(legDescriptor);
			DescribedProduct<InterestRateSwapProductDescriptor> product = new Swap(legReceiver, legPayer);
			return product;
		}
		else {
			String name = descriptor.name();
			throw new IllegalArgumentException("Unsupported product type " + name);
		}
	}

}
