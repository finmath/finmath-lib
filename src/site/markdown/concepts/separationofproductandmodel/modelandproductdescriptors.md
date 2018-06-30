# Separation of Product and Model and Numerical Method

02.02.2017, updated 22.06.2018.

[Work in progress. Will appear in a future version (4.x)]

## Introduction

Models and Products possibly come with different implementations for the different interfaces.
For example, a Heston model may be implemented in a Monte-Carlo simulation, a Fourier transform or a Finite Difference method
and a European option may be implemented against the three different interfaces.
To allow for a unified way of model and product construction the model and products are constructed by implementation specific factories
consuming implementation independent descriptors: the model descriptor and the product descriptor.
Since it is reasonable to have one descriptor per object these are provided via a parametrized interface.
That is a model may (should) implement

``DescribedDescribedModel<T extends ModelDescriptor>``

and a product may (should) implement

``DescribedDescribedProduct<T extends ProductDescriptor>``

Note: In case of a Model allowing construction from several different descriptors a common base type of the descriptors has to be defined
(this is required due to type erasure in the parametrized interface).

### Descriptors

The core concept here is to provide data, products, models and implementations (mostly) independent of each other. For this the realm of model and products is decomposed into the following components:

*product descriptors*: A product descriptor is a class representing a financial product. It is a model independent representation comparable to a POJO. There should be factories creating product descriptor out of FpML, XML or Key-Value representations and a product descriptor should be able to serialize itself into these formats.

*model descriptors*: A model descriptor is a class representing a financial model. It is independent of a specific numerical method. An example is a model descriptor for the equity Heston model, without specifying if the model is implemented as a PDE, Monte-Carlo simulation or Fourier transform.

*model implementations*: Implement a specific model (or model family) using a specific numerical method. An instance of a model implementation can be created by passing a model descriptor. Models can perform a calibration utilising the specific implementation. The calibrated model can be serialized back to a model descriptor.

*product implementations*: A product implementation implements the valuation of a financial product assuming a specific numerical method implemented by a model. An instance of a product implementation can be created by passing a product descriptor.

### Factories

To allow the construction of products for a given model implementation without knowing the actual model and product implementation, models provide a product factory. That is all model implementations provide a method

```
	DescribedProduct<? extends ProductDescriptor> getProductFromDesciptor(ProductDescriptor productDescriptor);
```

Product factories may be registered with a model (or the library in general) including some priority. This allows to add product factories for new descriptors and to *inject* alternative product implementations to be used for specific descriptors.

## Core Interfaces
The concept is mirrored via a few interfaces:

```
interface ProductDescriptor {
	// Marker Interface
}
```
```
interface ModelDescriptor {
	// Marker Interface
}
```
```
public interface DescribedProduct<T extends ProductDescriptor> {

	/**
	 * Return a product descriptor representing this product.
	 * 
	 * @return The product descriptor of this product.
	 */
	T getDescriptor();
}
```
```
public interface DescribedModel<T extends ModelDescriptor> {

	/**
	 * Return a model descriptor representing this model.
	 * 
	 * @return The model descriptor of this model.
	 */
	T getDescriptor();
	

	/**
	 * Construct a product from a product descriptor, which may be valued by this mmodel.
	 * 
	 * @param productDescriptor Given product descriptor.
	 * @return An instance of a product implementation.
	 */
	DescribedProduct<? extends ProductDescriptor> getProductFromDesciptor(ProductDescriptor productDescriptor);
}
```

## Product and Model Factories
Product and models are constructed by implementing factories. Factories carry the implementation specific part of the model or product. For example: A Fourier transform implementation of a Heston model requires the specification of the integration method used (line of integration, number of integration points, etc.). A Monte-Carlo implementation of a Heston model requires the specification of the Brownian driver (random number generator) and numerical scheme (Euler scheme with truncation, etc.).

For this reason we have two additional interfaces

```
public interface ProductFactory<T extends ProductDescriptor> {

	/**
	 * Constructs the product from a given product descriptor.
	 * 
	 * @param descriptor A product descriptor.
	 * @return An instance of the product describable by this descriptor.
	 */
	DescribedProduct<? extends T> getProductFromDescription(T descriptor);
}
```
```
public interface ModelFactory<T extends ModelDescriptor> {

	DescribedModel<? extends T> getModelFromDescriptor(T description);
}
```

## Calibration Round Trip

The setup allows to perform calibrations using specific calibration products and numerical methods to be re-used in more general models. Consider for example an Equity Heston model implemented as Monte-Carlo simulation. While brute-force Monte-Carlo calibration is expensive, fast an accurate valuations (and hence calibrations) exists for a restricted set of products where fast Fourier transforms can be utilized. Hence, calibration of the Monte-Carlo Heston model can be performed in the following steps:

* Create a Fourier transform implementation of the Heston model.
* Create ProductDescriptors for the calibration products.
* The model will create Fourier transform representations for the calibration products.
* Generically calibrate the Heston model using Fourier transforms.
* Extract the model descriptor from the calibrated model.
* Create the Monte-Carlo Heston model from the model descriptor of the calibrated model.
* The Monte-Carlo model will create Monte-Carlo implementations of the calibration products using the Monte-Carlo Heston model’s product factory.
* Check that the Monte-Carlo Heston model is calibrated by checking the calibration products.

## Specifications specific to the Numerical Method

The implementation of a model may require additional specifications.
These additional specifications are *not* part of the model descriptor. They belong to the model factory.

### Example

A standard Black Scholes model with constant coefficients is fully determined by the initial value, the drift and the volatility.
A Monte-Carlo simulation of this model requires a time-discretization of the Euler-Scheme, the number of paths, specifications related to the random number generators, etc., which are part of the BlackScholesModelMonteCarloFactory.
A Fourier transform implementation of this model requires specification of the integration method applied to the characteristic functions, which are part of the BlackScholesModelFourierFactory.
