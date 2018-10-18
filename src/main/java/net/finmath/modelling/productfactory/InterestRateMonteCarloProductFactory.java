package net.finmath.modelling.productfactory;

import java.time.LocalDate;

import net.finmath.modelling.DescribedProduct;
import net.finmath.modelling.InterestRateProductDescriptor;
import net.finmath.modelling.ProductDescriptor;
import net.finmath.modelling.ProductFactory;
import net.finmath.modelling.describedproducts.SwapLegMonteCarlo;
import net.finmath.modelling.describedproducts.SwaptionPhysicalMonteCarlo;
import net.finmath.modelling.descriptor.InterestRateSwapLegProductDescriptor;
import net.finmath.modelling.descriptor.InterestRateSwapProductDescriptor;
import net.finmath.modelling.descriptor.InterestRateSwaptionProductDescriptor;
import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.montecarlo.interestrate.products.Swap;

/**
 * Product factory of interest rate derivatives for use with a Monte-Carlo method based model.
 *
 * @author Christian Fries
 * @author Roland Bachl
 */
public class InterestRateMonteCarloProductFactory implements ProductFactory<InterestRateProductDescriptor> {

	private final LocalDate 						referenceDate;

	//	private static final boolean						couponFlow = true;
	//	private static final boolean						isNotionalAccruing = false;

	/**
	 * Initialize the factory with the given referenceDate.
	 *
	 * @param referenceDate To be used when converting absolute dates to relative dates in double.
	 */
	public InterestRateMonteCarloProductFactory(LocalDate referenceDate) {
		super();
		this.referenceDate = referenceDate;
	}

	@Override
	public DescribedProduct<? extends InterestRateProductDescriptor> getProductFromDescriptor(ProductDescriptor descriptor) {

		if(descriptor instanceof InterestRateSwapLegProductDescriptor) {
			InterestRateSwapLegProductDescriptor swapLeg 					= (InterestRateSwapLegProductDescriptor) descriptor;
			DescribedProduct<InterestRateSwapLegProductDescriptor> product 	= new SwapLegMonteCarlo(swapLeg, referenceDate);
			return product;

		}
		else if(descriptor instanceof InterestRateSwapProductDescriptor){
			InterestRateSwapProductDescriptor swap 							= (InterestRateSwapProductDescriptor) descriptor;
			InterestRateProductDescriptor legDescriptor 					= (InterestRateProductDescriptor) swap.getLegReceiver();
			AbstractLIBORMonteCarloProduct legReceiver 						= (AbstractLIBORMonteCarloProduct) getProductFromDescriptor(legDescriptor);
			legDescriptor 													= (InterestRateProductDescriptor) swap.getLegPayer();
			AbstractLIBORMonteCarloProduct legPayer 						= (AbstractLIBORMonteCarloProduct) getProductFromDescriptor(legDescriptor);
			DescribedProduct<InterestRateSwapProductDescriptor> product 	= new Swap(legReceiver, legPayer);
			return product;

		}
		else if(descriptor instanceof InterestRateSwaptionProductDescriptor) {
			InterestRateSwaptionProductDescriptor swaption						= (InterestRateSwaptionProductDescriptor) descriptor;
			DescribedProduct<InterestRateSwaptionProductDescriptor> product		= new SwaptionPhysicalMonteCarlo(swaption, referenceDate);
			return product;

		}
		else {
			String name = descriptor.name();
			throw new IllegalArgumentException("Unsupported product type " + name);
		}
	}

}
