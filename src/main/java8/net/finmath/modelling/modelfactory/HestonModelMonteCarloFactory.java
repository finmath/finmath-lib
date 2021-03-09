/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2018
 */

package net.finmath.modelling.modelfactory;

import net.finmath.modelling.DescribedModel;
import net.finmath.modelling.DescribedProduct;
import net.finmath.modelling.ModelFactory;
import net.finmath.modelling.ProductDescriptor;
import net.finmath.modelling.SingleAssetProductDescriptor;
import net.finmath.modelling.descriptor.HestonModelDescriptor;
import net.finmath.modelling.productfactory.SingleAssetMonteCarloProductFactory;
import net.finmath.montecarlo.IndependentIncrements;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.assetderivativevaluation.MonteCarloAssetModel;
import net.finmath.montecarlo.assetderivativevaluation.models.HestonModel;
import net.finmath.montecarlo.assetderivativevaluation.models.HestonModel.Scheme;
import net.finmath.montecarlo.model.ProcessModel;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.montecarlo.process.MonteCarloProcess;

/**
 * @author Christian Fries
 * @version 1.0
 */
public class HestonModelMonteCarloFactory implements ModelFactory<HestonModelDescriptor> {

	private final net.finmath.montecarlo.assetderivativevaluation.models.HestonModel.Scheme scheme;
	private final RandomVariableFactory randomVariableFactory;
	private final IndependentIncrements brownianMotion;


	public HestonModelMonteCarloFactory(final Scheme scheme, final RandomVariableFactory randomVariableFactory,
			final IndependentIncrements brownianMotion) {
		super();
		this.scheme = scheme;
		this.randomVariableFactory = randomVariableFactory;
		this.brownianMotion = brownianMotion;
	}


	@Override
	public DescribedModel<HestonModelDescriptor> getModelFromDescriptor(final HestonModelDescriptor modelDescriptor) {
		class HestonMonteCarloModel extends MonteCarloAssetModel implements DescribedModel<HestonModelDescriptor> {

			private final SingleAssetMonteCarloProductFactory productFactory = new SingleAssetMonteCarloProductFactory(modelDescriptor.getReferenceDate());

			/**
			 * @param model
			 * @param process
			 */
			HestonMonteCarloModel(final ProcessModel model, final MonteCarloProcess process) {
				super(model, process);
			}

			@Override
			public HestonModelDescriptor getDescriptor() {
				return modelDescriptor;
			}

			@Override
			public DescribedProduct<? extends ProductDescriptor> getProductFromDescriptor(final ProductDescriptor productDescriptor) {
				if(productDescriptor instanceof SingleAssetProductDescriptor) {
					return productFactory.getProductFromDescriptor(productDescriptor);
				}
				else {
					final String name = modelDescriptor.name();
					throw new IllegalArgumentException("Unsupported product type " + name);
				}
			}
		}

		final HestonModel model = new HestonModel(modelDescriptor, scheme, randomVariableFactory);

		return new HestonMonteCarloModel(
				model,
				new EulerSchemeFromProcessModel(model, brownianMotion)
				);
	}

}
