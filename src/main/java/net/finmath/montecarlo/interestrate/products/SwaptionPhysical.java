package net.finmath.montecarlo.interestrate.products;

import java.time.LocalDate;

import net.finmath.exception.CalculationException;
import net.finmath.modelling.DescribedProduct;
import net.finmath.modelling.descriptor.InterestRateSwaptionProductDescriptor;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.interestrate.products.components.Option;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.FloatingpointDate;

/**
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public class SwaptionPhysical extends AbstractLIBORMonteCarloProduct
		implements DescribedProduct<InterestRateSwaptionProductDescriptor> {

	private final InterestRateSwaptionProductDescriptor descriptor;
	
	private final Option swaption;
	
	public SwaptionPhysical(InterestRateSwaptionProductDescriptor descriptor, LocalDate referenceDate, AbstractLIBORMonteCarloProduct swap) {
		super();
		this.descriptor = descriptor;
		//TODO not necessarily right convention
		double excercise = FloatingpointDate.getFloatingPointDateFromDate(referenceDate, descriptor.getExcerciseDate());
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
