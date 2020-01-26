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
	private final RandomVariableFactory abstractRandomVariableFactory;
	private final IndependentIncrements brownianMotion;


	public HestonModelMonteCarloFactory(Scheme scheme, RandomVariableFactory abstractRandomVariableFactory,
			IndependentIncrements brownianMotion) {
		super();
		this.scheme = scheme;
		this.abstractRandomVariableFactory = abstractRandomVariableFactory;
		this.brownianMotion = brownianMotion;
	}


	@Override
	public DescribedModel<HestonModelDescriptor> getModelFromDescriptor(HestonModelDescriptor modelDescriptor) {
		class HestonMonteCarloModel extends MonteCarloAssetModel implements DescribedModel<HestonModelDescriptor> {

			private final SingleAssetMonteCarloProductFactory productFactory = new SingleAssetMonteCarloProductFactory(modelDescriptor.getReferenceDate());

			/**
			 * @param model
			 * @param process
			 */
			HestonMonteCarloModel(ProcessModel model, MonteCarloProcess process) {
				super(model, process);
			}

			@Override
			public HestonModelDescriptor getDescriptor() {
				return modelDescriptor;
			}

			@Override
			public DescribedProduct<? extends ProductDescriptor> getProductFromDescriptor(ProductDescriptor productDescriptor) {
				if(productDescriptor instanceof SingleAssetProductDescriptor) {
					return productFactory.getProductFromDescriptor(productDescriptor);
				}
				else {
					String name = modelDescriptor.name();
					throw new IllegalArgumentException("Unsupported product type " + name);
				}
			}
		}

		return new HestonMonteCarloModel(
				new net.finmath.montecarlo.assetderivativevaluation.models.HestonModel(modelDescriptor, scheme, abstractRandomVariableFactory),
				new EulerSchemeFromProcessModel(brownianMotion)
				);
	}

}
