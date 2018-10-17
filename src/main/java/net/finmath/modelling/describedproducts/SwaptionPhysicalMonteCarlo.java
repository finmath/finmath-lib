package net.finmath.modelling.describedproducts;

import java.time.LocalDate;

import net.finmath.exception.CalculationException;
import net.finmath.modelling.DescribedProduct;
import net.finmath.modelling.descriptor.InterestRateSwaptionProductDescriptor;
import net.finmath.modelling.productfactory.InterestRateMonteCarloProductFactory;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.montecarlo.interestrate.products.components.Option;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.FloatingpointDate;

/**
 * Monte-Carlo method based implementation of a physically settled interest rate swaption from a product descriptor.
 * 
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public class SwaptionPhysicalMonteCarlo extends AbstractLIBORMonteCarloProduct
implements DescribedProduct<InterestRateSwaptionProductDescriptor> {

	private final InterestRateSwaptionProductDescriptor descriptor;

	private final Option swaption;

	/**
	 * Create product from descriptor.
	 * 
	 * @param descriptor The descriptor of the product.
	 * @param referenceDate The reference date of the data for the valuation, used to convert absolute date to relative dates in double representation.
	 */
	public SwaptionPhysicalMonteCarlo(InterestRateSwaptionProductDescriptor descriptor, LocalDate referenceDate) {
		super();
		this.descriptor = descriptor;
		double excercise = FloatingpointDate.getFloatingPointDateFromDate(referenceDate, descriptor.getExcerciseDate());
		AbstractLIBORMonteCarloProduct swap =
				(AbstractLIBORMonteCarloProduct) new InterestRateMonteCarloProductFactory(referenceDate).getProductFromDescriptor(descriptor.getUnderlyingSwap());
		swaption = new Option(excercise, descriptor.getStrikeRate(), swap);
	}

	@Override
	public InterestRateSwaptionProductDescriptor getDescriptor() {

		return descriptor;
	}

	@Override
	public RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model)
			throws CalculationException {
		return swaption.getValue(evaluationTime, model);
	}

}
