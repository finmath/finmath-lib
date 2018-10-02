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
import net.finmath.montecarlo.interestrate.products.indices.AbstractIndex;
import net.finmath.montecarlo.interestrate.products.indices.LIBORIndex;
import net.finmath.time.ScheduleInterface;

/**
 * @author Christian Fries
 *
 */
public class InterestRateMonteCarloProductFactory implements ProductFactory<InterestRateProductDescriptor> {

	private final AbstractNotional						notional;
	private static final boolean						couponFlow = true;
	private static final boolean						isNotionalAccruing = false;

	public InterestRateMonteCarloProductFactory(AbstractNotional notional) {
		this.notional = notional;
	}

	@Override
	public DescribedProduct<? extends InterestRateProductDescriptor> getProductFromDescriptor(ProductDescriptor descriptor) {

		if(descriptor instanceof InterestRateSwapLegProductDescriptor) {
			InterestRateSwapLegProductDescriptor swapLeg = (InterestRateSwapLegProductDescriptor) descriptor;
			AbstractIndex index;
			if(((InterestRateSwapLegProductDescriptor) descriptor).getForwardCurveName() != null) {
				double[] liborIndexParameters = liborIndexParameters(((InterestRateSwapLegProductDescriptor) descriptor).getLegSchedule());
				index = new LIBORIndex(((InterestRateSwapLegProductDescriptor) descriptor).getForwardCurveName(), liborIndexParameters[0], liborIndexParameters[1]);
			} else {
				index = null;
			}
			DescribedProduct<InterestRateSwapLegProductDescriptor> product = new SwapLeg(swapLeg.getLegSchedule(), notional, index, swapLeg.getSpread(), couponFlow,
					swapLeg.isNotionalExchanged(), isNotionalAccruing);
			return product;
		}else if(descriptor instanceof InterestRateSwapProductDescriptor){
			InterestRateSwapProductDescriptor swap = (InterestRateSwapProductDescriptor) descriptor;
			InterestRateProductDescriptor legDescriptor = (InterestRateProductDescriptor) swap.getLegReceiver();
			AbstractLIBORMonteCarloProduct legReceiver = (AbstractLIBORMonteCarloProduct) getProductFromDescriptor(legDescriptor);  
			legDescriptor = (InterestRateProductDescriptor) swap.getLegPayer();
			AbstractLIBORMonteCarloProduct legPayer = (AbstractLIBORMonteCarloProduct) getProductFromDescriptor(legDescriptor); 
			DescribedProduct<InterestRateSwapProductDescriptor> product = new Swap(legReceiver, legPayer);
			return product;
		} else {
			String name = descriptor.name();
			throw new IllegalArgumentException("Unsupported product type " + name);
		}
	}

	private static double[] liborIndexParameters(ScheduleInterface schedule) {
		
		//determine average fixing offset and period length
		double fixingOffset = 0;
		double periodLength = 0;
		
		for(int i = 0; i < schedule.getNumberOfPeriods(); i++) {
			fixingOffset *= ((double) i) / (i+1);
			fixingOffset += (schedule.getPeriodStart(i) - schedule.getFixing(i)) / (i+1);
			
			periodLength *= ((double) i) / (i+1);
			periodLength += schedule.getPeriodLength(i) / (i+1);
		}
		
		return new double[] {fixingOffset, periodLength};
	}
}
