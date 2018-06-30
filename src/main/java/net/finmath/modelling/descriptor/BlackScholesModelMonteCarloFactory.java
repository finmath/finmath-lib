/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2018
 */

package net.finmath.modelling.descriptor;

import net.finmath.modelling.DescribedModel;
import net.finmath.modelling.DescribedProduct;
import net.finmath.modelling.ModelFactory;
import net.finmath.modelling.ProductDescriptor;
import net.finmath.modelling.SingleAssetProductDescriptor;
import net.finmath.montecarlo.AbstractRandomVariableFactory;
import net.finmath.montecarlo.IndependentIncrementsInterface;
import net.finmath.montecarlo.assetderivativevaluation.HestonModel;
import net.finmath.montecarlo.assetderivativevaluation.HestonModel.Scheme;
import net.finmath.montecarlo.assetderivativevaluation.MonteCarloAssetModel;
import net.finmath.montecarlo.model.AbstractModelInterface;
import net.finmath.montecarlo.process.AbstractProcessInterface;
import net.finmath.montecarlo.process.ProcessEulerScheme;

/**
 * @author Christian Fries
 */
public class BlackScholesModelMonteCarloFactory implements ModelFactory<BlackScholesModelDescriptor> {

	private final AbstractRandomVariableFactory randomVariableFactory;
	private final IndependentIncrementsInterface brownianMotion;


	public BlackScholesModelMonteCarloFactory(AbstractRandomVariableFactory randomVariableFactory,
			IndependentIncrementsInterface brownianMotion) {
		super();
		this.randomVariableFactory = randomVariableFactory;
		this.brownianMotion = brownianMotion;
	}

	@Override
	public DescribedModel<BlackScholesModelDescriptor> getModelFromDescriptor(BlackScholesModelDescriptor modelDescriptor) {
		class BlackScholesMonteCarloModel extends MonteCarloAssetModel implements DescribedModel<BlackScholesModelDescriptor> {

			final SingleAssetMonteCarloProductFactory productFactory = new SingleAssetMonteCarloProductFactory();
			
			/**
			 * @param model
			 * @param process
			 */
			public BlackScholesMonteCarloModel(AbstractModelInterface model, AbstractProcessInterface process) {
				super(model, process);
			}
			
			@Override
			public BlackScholesModelDescriptor getDescriptor() {
				return modelDescriptor;
			}

			@Override
			public DescribedProduct<? extends ProductDescriptor> getProductFromDesciptor(ProductDescriptor productDescriptor) {
				if(productDescriptor instanceof SingleAssetProductDescriptor) {
					return productFactory.getProductFromDescription((SingleAssetProductDescriptor) productDescriptor);
				}
				else {
					String name = modelDescriptor.name();
					throw new IllegalArgumentException("Unsupported product type " + name);
				}
			}	
		}

        return new BlackScholesMonteCarloModel(
				new net.finmath.montecarlo.assetderivativevaluation.HestonModel(
						brownianMotion.getRandomVariableForConstant(modelDescriptor.getInitialValue()),
						modelDescriptor.getDiscountCurveForForwardRate(),
						brownianMotion.getRandomVariableForConstant(modelDescriptor.getVolatility()),
						modelDescriptor.getDiscountCurveForDiscountRate(),
						brownianMotion.getRandomVariableForConstant(0.0),
						brownianMotion.getRandomVariableForConstant(0.0),
						brownianMotion.getRandomVariableForConstant(0.0),
						brownianMotion.getRandomVariableForConstant(0.0),
						HestonModel.Scheme.FULL_TRUNCATION, randomVariableFactory
						), 
				new ProcessEulerScheme(brownianMotion)
				);
	}

}
