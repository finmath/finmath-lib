package net.finmath.fouriermethod.models;

import java.util.ArrayList;
import java.util.Collection;

import net.finmath.modelling.DescribedModel;
import net.finmath.modelling.DescribedProduct;
import net.finmath.modelling.ProductDescriptor;
import net.finmath.modelling.ProductFactory;
import net.finmath.modelling.descriptor.AssetModelDescriptor;
import net.finmath.modelling.productfactory.SingleAssetFourierProductFactory;

public abstract class AbstractFourierModel<M extends AssetModelDescriptor> implements ProcessCharacteristicFunctionInterface, DescribedModel<M>, Cloneable {
	
	private ArrayList<ProductFactory<? extends ProductDescriptor, M>> factoryList;
	
	public AbstractFourierModel() {
		super();
		factoryList = new ArrayList<ProductFactory< ? extends ProductDescriptor, M >>(1);
		factoryList.add( new SingleAssetFourierProductFactory<M>() );
	}
	
	public AbstractFourierModel( Collection<ProductFactory< ? extends ProductDescriptor, M >> factories) {
		super();
		factoryList = new ArrayList<ProductFactory< ? extends ProductDescriptor, M >>(factories);
	}

	/* (non-Javadoc)
	 * @see net.finmath.modelling.DescribedModel#getProductFromDesciptor(net.finmath.modelling.ProductDescriptor)
	 */
	public DescribedProduct<? extends ProductDescriptor> getProductFromDescriptor(ProductDescriptor productDescriptor) {
		for(ProductFactory<? extends ProductDescriptor, M> factory : factoryList) 
			if(factory.supportsProduct(productDescriptor))
				return factory.getProductFromDescription(productDescriptor);
		String name = getDescriptor().name();
		throw new IllegalArgumentException("Unsupported product type " + name);
	}
	
	/* (non-Javadoc)
	 * @see net.finmath.modelling.DescribedModel#getModelWithProductFactory(net.finmath.modelling.ProductFactory, boolean)
	 */
	public DescribedModel<? extends M> getModelWithProductFactory(ProductFactory< ? extends ProductDescriptor, M > productFactory, boolean append) {
		AbstractFourierModel<M> newModel;
		try {
			newModel = (AbstractFourierModel<M>) clone();
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			throw new RuntimeException("Failed to amend model", e);
		}
		if(append)
			newModel.factoryList.add(productFactory);
		else
			newModel.factoryList.add(0, productFactory);
		return newModel;
	}
	
	public ArrayList<ProductFactory<? extends ProductDescriptor, M>> getFactoryList() {
		return factoryList;
	}
	
}
