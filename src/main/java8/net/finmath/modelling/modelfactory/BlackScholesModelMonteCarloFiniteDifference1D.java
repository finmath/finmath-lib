/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2018
 */
package net.finmath.modelling.modelfactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.finmath.finitedifference.models.FDMBlackScholesModel;
import net.finmath.finitedifference.models.FiniteDifference1DModel;
import net.finmath.finitedifference.products.FDMEuropeanCallOption;
import net.finmath.modelling.DescribedModel;
import net.finmath.modelling.DescribedProduct;
import net.finmath.modelling.Model;
import net.finmath.modelling.ModelFactory;
import net.finmath.modelling.ProductDescriptor;
import net.finmath.modelling.SingleAssetProductDescriptor;
import net.finmath.modelling.descriptor.BlackScholesModelDescriptor;
import net.finmath.modelling.descriptor.SingleAssetEuropeanOptionProductDescriptor;
import net.finmath.time.FloatingpointDate;

/**
 * @author Christian Fries
 * @version 1.0
 */
public class BlackScholesModelMonteCarloFiniteDifference1D implements ModelFactory<BlackScholesModelDescriptor> {

	private final double theta;

	public BlackScholesModelMonteCarloFiniteDifference1D(final double theta) {
		super();
		this.theta = theta;
	}

	@Override
	public DescribedModel<BlackScholesModelDescriptor> getModelFromDescriptor(final BlackScholesModelDescriptor modelDescriptor) {

		final double initialValue = modelDescriptor.getInitialValue();
		// @TODO The model does not support a curve for the forward
		final double riskFreeRate = -Math.log(modelDescriptor.getDiscountCurveForForwardRate().getDiscountFactor(1.0));
		final double volatility = modelDescriptor.getVolatility();

		final int numTimesteps = 35;
		final int numSpacesteps = 120;
		final int numStandardDeviations = 5;
		final double center = initialValue;

		class BlackScholesFDModel extends FDMBlackScholesModel implements DescribedModel<BlackScholesModelDescriptor> {

			BlackScholesFDModel() {
				super(numTimesteps, numSpacesteps, numStandardDeviations, center, theta, initialValue, riskFreeRate, volatility);
				// TODO Auto-generated constructor stub
			}


			@Override
			public BlackScholesModelDescriptor getDescriptor() {
				return modelDescriptor;
			}

			@Override
			public DescribedProduct<? extends ProductDescriptor> getProductFromDescriptor(final ProductDescriptor productDescriptor) {
				if(productDescriptor instanceof SingleAssetEuropeanOptionProductDescriptor) {

					class FDCallOptionProduct extends FDMEuropeanCallOption implements DescribedProduct<SingleAssetProductDescriptor> {

						FDCallOptionProduct() {
							super(FloatingpointDate.getFloatingPointDateFromDate(modelDescriptor.getReferenceDate(),
									((SingleAssetEuropeanOptionProductDescriptor)productDescriptor).getMaturity()),
									((SingleAssetEuropeanOptionProductDescriptor)productDescriptor).getStrike());
							// TODO Auto-generated constructor stub
						}

						@Override
						public Object getValue(final double evaluationTime, final Model model) {
							// TODO Check return type - should be RandomVariable
							return getValues(evaluationTime, model);
						}

						@Override
						public Map<String, Object> getValues(final double evaluationTime, final Model model) {
							// TODO This implementation should go into a class
							final double[][] valueFDM = this.getValue(0.0, (FiniteDifference1DModel)model);
							final double[] initialStockPrice = valueFDM[0];
							final double[] optionValue = valueFDM[1];

							final int indexOfSpot = Arrays.binarySearch(initialStockPrice, initialValue);

							double value;
							if(indexOfSpot >= 0) {
								value = optionValue[indexOfSpot];
							}
							else {
								final int indexOfSpotLow	= -indexOfSpot-2;
								final double alpha = (initialValue-initialStockPrice[indexOfSpotLow])/(initialStockPrice[indexOfSpotLow+1]-initialStockPrice[indexOfSpotLow]);
								value = (1-alpha) * optionValue[indexOfSpotLow] + alpha * optionValue[indexOfSpotLow+1];
							}

							final Map<String, Object> results = new HashMap<>();
							results.put("value", value);
							return results;
						}

						@Override
						public SingleAssetProductDescriptor getDescriptor() {
							return (SingleAssetProductDescriptor) productDescriptor;
						}
					}
					return new FDCallOptionProduct();
				}
				else {
					final String name = modelDescriptor.name();
					throw new IllegalArgumentException("Unsupported product type " + name);
				}
			}
		}

		return new BlackScholesFDModel();
	}
}
