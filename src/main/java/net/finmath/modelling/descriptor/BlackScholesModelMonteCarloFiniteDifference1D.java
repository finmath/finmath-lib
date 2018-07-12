/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2018
 */
package net.finmath.modelling.descriptor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.finmath.finitedifference.models.FDMBlackScholesModel;
import net.finmath.finitedifference.models.FiniteDifference1DModel;
import net.finmath.finitedifference.products.FDMEuropeanCallOption;
import net.finmath.modelling.DescribedModel;
import net.finmath.modelling.DescribedProduct;
import net.finmath.modelling.ModelFactory;
import net.finmath.modelling.ModelInterface;
import net.finmath.modelling.ProductDescriptor;
import net.finmath.modelling.SingleAssetProductDescriptor;

/**
 * @author Christian Fries
 */
public class BlackScholesModelMonteCarloFiniteDifference1D implements ModelFactory<BlackScholesModelDescriptor> {

	private final double theta;

	public BlackScholesModelMonteCarloFiniteDifference1D(double theta) {
		super();
		this.theta = theta;
	}

	@Override
	public DescribedModel<BlackScholesModelDescriptor> getModelFromDescriptor(BlackScholesModelDescriptor modelDescriptor) {

		double initialValue = modelDescriptor.getInitialValue();
		// @TODO The model does not support a curve for the forward
		double riskFreeRate = -Math.log(modelDescriptor.getDiscountCurveForForwardRate().getDiscountFactor(1.0));
		double volatility = modelDescriptor.getVolatility();

		int numTimesteps = 35;
		int numSpacesteps = 120;
		int numStandardDeviations = 5;
		double center = initialValue;

		class BlackScholesFDModel extends FDMBlackScholesModel implements DescribedModel<BlackScholesModelDescriptor> {

			public BlackScholesFDModel() {
				super(numTimesteps, numSpacesteps, numStandardDeviations, center, theta, initialValue, riskFreeRate, volatility);
				// TODO Auto-generated constructor stub
			}


			@Override
			public BlackScholesModelDescriptor getDescriptor() {
				return modelDescriptor;
			}

			@Override
			public DescribedProduct<? extends ProductDescriptor> getProductFromDescriptor(ProductDescriptor productDescriptor) {
				if(productDescriptor instanceof SingleAssetEuropeanOptionProductDescriptor) {

					class FDCallOptionProduct extends FDMEuropeanCallOption implements DescribedProduct<SingleAssetProductDescriptor> {

						public FDCallOptionProduct() {
							super(((SingleAssetEuropeanOptionProductDescriptor)productDescriptor).getMaturity(), ((SingleAssetEuropeanOptionProductDescriptor)productDescriptor).getStrike());
							// TODO Auto-generated constructor stub
						}

						@Override
						public Object getValue(double evaluationTime, ModelInterface model) {
							return getValues(evaluationTime, model);
						}

						@Override
						public Map<String, Object> getValues(double evaluationTime, ModelInterface model) {
							double[][] valueFDM = this.getValue(0.0, (FiniteDifference1DModel)model);
							double[] initialStockPrice = valueFDM[0];
							double[] optionValue = valueFDM[1];

							int indexOfSpot = Arrays.binarySearch(initialStockPrice, initialValue);
							if(indexOfSpot < 0) {
								indexOfSpot = -indexOfSpot-1;
							}

							Map<String, Object> results = new HashMap<>();
							results.put("value", optionValue[indexOfSpot]);
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
					String name = modelDescriptor.name();
					throw new IllegalArgumentException("Unsupported product type " + name);
				}
			}
		}

		return new BlackScholesFDModel();
	}
}
