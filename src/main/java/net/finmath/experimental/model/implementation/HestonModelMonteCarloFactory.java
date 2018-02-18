/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2018
 */

package net.finmath.experimental.model.implementation;

import net.finmath.experimental.model.Model;
import net.finmath.experimental.model.ModelFactory;
import net.finmath.experimental.model.Product;
import net.finmath.experimental.model.ProductDescriptor;
import net.finmath.experimental.model.SingleAssetProductDescriptor;
import net.finmath.montecarlo.AbstractRandomVariableFactory;
import net.finmath.montecarlo.IndependentIncrementsInterface;
import net.finmath.montecarlo.assetderivativevaluation.HestonModel.Scheme;
import net.finmath.montecarlo.assetderivativevaluation.MonteCarloAssetModel;
import net.finmath.montecarlo.model.AbstractModelInterface;
import net.finmath.montecarlo.process.AbstractProcessInterface;
import net.finmath.montecarlo.process.ProcessEulerScheme;

/**
 * @author Christian Fries
 */
public class HestonModelMonteCarloFactory implements ModelFactory<HestonModelDescriptor> {

	private final net.finmath.montecarlo.assetderivativevaluation.HestonModel.Scheme scheme;
	private final AbstractRandomVariableFactory randomVariableFactory;
	private final IndependentIncrementsInterface brownianMotion;


	public HestonModelMonteCarloFactory(Scheme scheme, AbstractRandomVariableFactory randomVariableFactory,
			IndependentIncrementsInterface brownianMotion) {
		super();
		this.scheme = scheme;
		this.randomVariableFactory = randomVariableFactory;
		this.brownianMotion = brownianMotion;
	}


	@Override
	public Model<HestonModelDescriptor> getModelFromDescription(HestonModelDescriptor descriptor) {
		class HestonMonteCarloModel extends MonteCarloAssetModel implements Model<HestonModelDescriptor> {

			/**
			 * @param model
			 * @param process
			 */
			public HestonMonteCarloModel(AbstractModelInterface model, AbstractProcessInterface process) {
				super(model, process);
			}
			
			@Override
			public HestonModelDescriptor getDescriptor() {
				return descriptor;
			}

			@Override
			public Product<?> getProductFromDesciptor(ProductDescriptor productDescriptor) {
				return (new SingleAssetMonteCarloProductFactory()).getProductFromDescription((SingleAssetProductDescriptor) productDescriptor);
			}	
		};
		
		return new HestonMonteCarloModel(
				new net.finmath.montecarlo.assetderivativevaluation.HestonModel(descriptor, scheme, randomVariableFactory), 
				new ProcessEulerScheme(brownianMotion)
				);
	}

}
