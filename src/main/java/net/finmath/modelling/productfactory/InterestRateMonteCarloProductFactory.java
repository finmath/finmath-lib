package net.finmath.modelling.productfactory;

import net.finmath.modelling.DescribedProduct;
import net.finmath.modelling.InterestRateProductDescriptor;
import net.finmath.modelling.ProductDescriptor;
import net.finmath.modelling.ProductFactory;
import net.finmath.modelling.descriptor.InterestRateSwapLegProductDescriptor;
import net.finmath.modelling.descriptor.InterestRateSwapProductDescriptor;
import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.montecarlo.interestrate.products.Swap;
import net.finmath.montecarlo.interestrate.products.SwapLeg;
import net.finmath.montecarlo.interestrate.products.components.AbstractNotional;
import net.finmath.montecarlo.interestrate.products.components.Notional;
import net.finmath.montecarlo.interestrate.products.indices.AbstractIndex;

/**
 * @author Christian Fries
 *
 */
public class InterestRateMonteCarloProductFactory implements ProductFactory<InterestRateProductDescriptor> {

	private final AbstractNotional				notional;
	private final AbstractIndex					index;
	private final boolean						couponFlow = true;
	private final boolean						isNotionalAccruing = false;

	public InterestRateMonteCarloProductFactory(AbstractIndex index) {
		this(new Notional(1.0), index);
	}

	public InterestRateMonteCarloProductFactory(AbstractNotional notional, AbstractIndex index) {
		this.notional = notional;
		this.index = index;
	}

	@Override
	public DescribedProduct<? extends InterestRateProductDescriptor> getProductFromDescriptor(ProductDescriptor descriptor) {

		if(descriptor instanceof InterestRateSwapLegProductDescriptor) {
			InterestRateSwapLegProductDescriptor swapLeg = (InterestRateSwapLegProductDescriptor) descriptor;
			DescribedProduct<InterestRateSwapLegProductDescriptor> product = new SwapLeg(swapLeg.getLegSchedule(), notional, index, swapLeg.getSpread(), couponFlow,
					swapLeg.isNotionalExchanged(), isNotionalAccruing);
			return product;
		}else if(descriptor instanceof InterestRateSwapProductDescriptor){
			InterestRateSwapProductDescriptor swap = (InterestRateSwapProductDescriptor) descriptor;
			// TODO what if these are not SwapLegs? Is that a realistic case?
			InterestRateSwapLegProductDescriptor legDescriptor = (InterestRateSwapLegProductDescriptor) swap.getLegReceiver();
			AbstractLIBORMonteCarloProduct legReceiver =  new SwapLeg(legDescriptor.getLegSchedule(), notional, index, legDescriptor.getSpread(), couponFlow,
					legDescriptor.isNotionalExchanged(), isNotionalAccruing);
			legDescriptor = (InterestRateSwapLegProductDescriptor) swap.getLegPayer();
			AbstractLIBORMonteCarloProduct legPayer =  new SwapLeg(legDescriptor.getLegSchedule(), notional, index, legDescriptor.getSpread(), couponFlow,
					legDescriptor.isNotionalExchanged(), isNotionalAccruing);
			DescribedProduct<InterestRateSwapProductDescriptor> product = new Swap(legReceiver, legPayer);
			return product;
		} else {
			String name = descriptor.name();
			throw new IllegalArgumentException("Unsupported product type " + name);
		}
	}

}
