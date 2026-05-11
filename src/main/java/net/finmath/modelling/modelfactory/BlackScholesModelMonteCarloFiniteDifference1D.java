/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2018
 */
package net.finmath.modelling.modelfactory;

import java.util.HashMap;
import java.util.Map;

import net.finmath.finitedifference.assetderivativevaluation.models.FDMBlackScholesModel;
import net.finmath.finitedifference.assetderivativevaluation.models.FiniteDifferenceEquityModel;
import net.finmath.finitedifference.assetderivativevaluation.products.EuropeanOption;
import net.finmath.finitedifference.grids.SpaceTimeDiscretization;
import net.finmath.finitedifference.utilities.FiniteDifferenceValueInterpolator;
import net.finmath.modelling.DescribedModel;
import net.finmath.modelling.DescribedProduct;
import net.finmath.modelling.Model;
import net.finmath.modelling.ModelFactory;
import net.finmath.modelling.ProductDescriptor;
import net.finmath.modelling.SingleAssetProductDescriptor;
import net.finmath.modelling.descriptor.BlackScholesModelDescriptor;
import net.finmath.modelling.descriptor.SingleAssetEuropeanOptionProductDescriptor;
import net.finmath.modelling.products.CallOrPut;
import net.finmath.time.FloatingpointDate;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;
import net.finmath.finitedifference.grids.Grid;
import net.finmath.finitedifference.grids.UniformGrid;

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
	public DescribedModel<BlackScholesModelDescriptor> getModelFromDescriptor(
			final BlackScholesModelDescriptor modelDescriptor) {

		final double initialValue = modelDescriptor.getInitialValue();

		// @TODO The model does not support a curve for the forward.
		final double riskFreeRate =
				-Math.log(modelDescriptor.getDiscountCurveForForwardRate().getDiscountFactor(1.0));

		final double volatility = modelDescriptor.getVolatility();

		final int numTimesteps = 100;
		final int numSpacesteps = 200;
		final int numStandardDeviations = 5;

		/*
		 * The old implementation used theta in the old FDMBlackScholesModel
		 * constructor. Keep the same theta here.
		 *
		 * If theta already exists as a field of this class, remove this local
		 * declaration and use the field instead.
		 */
		final double theta = 0.5;

		class BlackScholesFDModel extends FDMBlackScholesModel
				implements DescribedModel<BlackScholesModelDescriptor> {

			BlackScholesFDModel() {
				super(
						initialValue,
						riskFreeRate,
						volatility,
						createSpaceTimeDiscretization(
								initialValue,
								riskFreeRate,
								volatility,
								1.0,
								numTimesteps,
								numSpacesteps,
								numStandardDeviations,
								theta
						)
				);
			}

			@Override
			public BlackScholesModelDescriptor getDescriptor() {
				return modelDescriptor;
			}

			@Override
			public DescribedProduct<? extends ProductDescriptor> getProductFromDescriptor(
					final ProductDescriptor productDescriptor) {

				if(productDescriptor instanceof SingleAssetEuropeanOptionProductDescriptor) {

					final SingleAssetEuropeanOptionProductDescriptor europeanOptionDescriptor =
							(SingleAssetEuropeanOptionProductDescriptor) productDescriptor;

					final double maturity = FloatingpointDate.getFloatingPointDateFromDate(
							modelDescriptor.getReferenceDate(),
							europeanOptionDescriptor.getMaturity()
					);

					final double strike = europeanOptionDescriptor.getStrike();

					class FDCallOptionProduct extends EuropeanOption
							implements DescribedProduct<SingleAssetProductDescriptor> {

						FDCallOptionProduct() {
							super(maturity, strike, CallOrPut.CALL);
						}

						@Override
						public Object getValue(final double evaluationTime, final Model model) {
							return getValues(evaluationTime, model).get("value");
						}

						@Override
						public Map<String, Object> getValues(
								final double evaluationTime,
								final Model model) {

							if(!(model instanceof FiniteDifferenceEquityModel)) {
								throw new IllegalArgumentException(
										"Model must be a FiniteDifferenceEquityModel.");
							}

							final SpaceTimeDiscretization valuationDiscretization =
									createSpaceTimeDiscretization(
											initialValue,
											riskFreeRate,
											volatility,
											maturity,
											numTimesteps,
											numSpacesteps,
											numStandardDeviations,
											theta
									);

							final FDMBlackScholesModel valuationModel =
									new FDMBlackScholesModel(
											initialValue,
											riskFreeRate,
											volatility,
											valuationDiscretization
									);

							final double[] optionValues =
									super.getValue(evaluationTime, valuationModel);

							final double value =
									FiniteDifferenceValueInterpolator.interpolateValue(
											optionValues,
											valuationDiscretization,
											initialValue
									);

							final Map<String, Object> results = new HashMap<>();
							results.put("value", value);
							return results;
						}

						@Override
						public SingleAssetProductDescriptor getDescriptor() {
							return europeanOptionDescriptor;
						}
					}

					return new FDCallOptionProduct();
				}

				final String name = modelDescriptor.name();
				throw new IllegalArgumentException("Unsupported product type " + name);
			}
		}

		return new BlackScholesFDModel();
	}

	private static SpaceTimeDiscretization createSpaceTimeDiscretization(
			final double initialValue,
			final double riskFreeRate,
			final double volatility,
			final double maturity,
			final int numTimesteps,
			final int numSpacesteps,
			final int numStandardDeviations,
			final double theta) {

		final double timeHorizon = Math.max(maturity, 1.0E-8);

		final double forwardValue = initialValue * Math.exp(riskFreeRate * timeHorizon);

		final double varianceStock =
				initialValue
				* initialValue
				* Math.exp(2.0 * riskFreeRate * timeHorizon)
				* (Math.exp(volatility * volatility * timeHorizon) - 1.0);

		final double standardDeviationStock =
				Math.sqrt(Math.max(varianceStock, 0.0));

		final double minimumStockPrice =
				Math.max(
						forwardValue - numStandardDeviations * standardDeviationStock,
						0.0
				);

		final double maximumStockPrice =
				forwardValue + numStandardDeviations * standardDeviationStock;

		final Grid spaceGrid =
				new UniformGrid(numSpacesteps, minimumStockPrice, maximumStockPrice);

		final TimeDiscretization timeDiscretization =
				new TimeDiscretizationFromArray(
						0.0,
						numTimesteps,
						timeHorizon / numTimesteps
				);

		return new SpaceTimeDiscretization(
				spaceGrid,
				timeDiscretization,
				theta,
				new double[] {initialValue}
		);
	}
}
