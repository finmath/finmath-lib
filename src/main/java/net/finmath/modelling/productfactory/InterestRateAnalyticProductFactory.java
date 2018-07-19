package net.finmath.modelling.productfactory;

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

	private final String				forwardCurveName;
	private final String				discountCurveName;
	private final String				discountCurveForNotionalResetName;


	public InterestRateAnalyticProductFactory(String forwardCurveName, String discountCurveName,
			String discountCurveForNotionalResetName) {
		super();
		this.forwardCurveName = forwardCurveName;
		this.discountCurveName = discountCurveName;
		this.discountCurveForNotionalResetName = discountCurveForNotionalResetName;
	}

	@Override
	public DescribedProduct<? extends InterestRateProductDescriptor> getProductFromDescriptor(ProductDescriptor descriptor) {
		if(descriptor instanceof InterestRateSwapLegProductDescriptor) {
			InterestRateSwapLegProductDescriptor swapLeg = (InterestRateSwapLegProductDescriptor) descriptor;
			DescribedProduct<InterestRateSwapLegProductDescriptor> product = new SwapLeg(swapLeg.getLegSchedule(), forwardCurveName, swapLeg.getSpread(), discountCurveName,
					discountCurveForNotionalResetName, swapLeg.isNotionalExchanged());
			return product;
		}else if(descriptor instanceof InterestRateSwapProductDescriptor){
			InterestRateSwapProductDescriptor swap = (InterestRateSwapProductDescriptor) descriptor;
			// TODO what if these are not SwapLegs? Is that a realistic case? Do recursion?
			InterestRateSwapLegProductDescriptor legDescriptor = (InterestRateSwapLegProductDescriptor) swap.getLegReceiver();
			//			AnalyticProductInterface legReceiver =  new SwapLeg(legDescriptor.getLegSchedule(), forwardCurveName, legDescriptor.getSpread(), discountCurveName,
			//					discountCurveForNotionalResetName, legDescriptor.isNotionalExchanged());
			AnalyticProductInterface legReceiver =  (AnalyticProductInterface) getProductFromDescriptor(legDescriptor);
			legDescriptor = (InterestRateSwapLegProductDescriptor) swap.getLegPayer();
			AnalyticProductInterface legPayer =  new SwapLeg(legDescriptor.getLegSchedule(), forwardCurveName, legDescriptor.getSpread(), discountCurveName,
					discountCurveForNotionalResetName, legDescriptor.isNotionalExchanged());
			DescribedProduct<InterestRateSwapProductDescriptor> product = new Swap(legReceiver, legPayer);
			return product;
		} else {
			String name = descriptor.name();
			throw new IllegalArgumentException("Unsupported product type " + name);
		}
	}

}
