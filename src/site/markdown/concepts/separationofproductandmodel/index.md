# Separation of Product and Model and Numerical Method
02.02.2017. Work in progress. Will appear in a future version (4.x).

## Introduction
The valuation library proposes the separation of financial products and models and numerical methods (as suggested in ISBN).

The core concept here is to provide data, products, models and implementations (mostly) independent of each other. For this the realm of model and products is decomposed into the following components:

*product descriptors*: A product descriptor is a class representing a financial product. It is a model independent representation comparable to a POJO. There should be factories creating product descriptor out of FpML, XML or Key-Value representations and a product descriptor should be able to serialize itself into these formats.

*model descriptors*: A model descriptor is a class representing a financial model. It is independent of a specific numerical method. An example is a model descriptor for the equity Heston model, without specifying if the model is implemented as a PDE, Monte-Carlo simulation or Fourier transform.

*model implementations*: Implement a specific model (or model family) using a specific numerical method. An instance of a model implementation can be created by passing a model descriptor. Models can perform a calibration utilising the specific implementation. The calibrated model can be serialized back to a model descriptor.

*product implementations*: A product implementation implements the valuation of a financial product assuming a specific numerical method implemented by a model. An instance of a product implementation can be created by passing a product descriptor.

To allow the construction of products for a given model implementation without knowing the actual model and product implementation, models provide a product factory. That is all model implementations provide a method

```
	Product<? extends ProductDescriptor> getProductFromDesciptor(ProductDescriptor productDescriptor);
```

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
public interface Product<T extends ProductDescriptor> {

	/**
	 * Return a product descriptor representing this product.
	 * 
	 * @return The product descriptor of this product.
	 */
	T getDescriptor();

	/**
	 * Return the valuation of the product using the given model.
	 * 
	 * Implement this method using a checked cast of the model to a derived model for which the product
	 * provides a valuation algorithm. Example: an interest rate product requires that the passed model
	 * object implements the interface of an interest rate model. Since there is no polymorphism on
	 * arguments (see Double Dynamic Dispatch), we reply on a checked cast.
	 * 
	 * @param evaluationTime The evaluation time as double. Cash flows prior and including this time are not considered.
	 * @param model The model under which the product is valued.
	 * @return The value of the product using the given model.
	 */
	Object getValue(double evaluationTime, Model<?> model);

	/**
	 * Return the valuation of the product using the given model.
	 * 
	 * Implement this method using a checked cast of the model to a derived model for which the product
	 * provides a valuation algorithm. Example: an interest rate product requires that the passed model
	 * object implements the interface of an interest rate model. Since there is no polymorphism on
	 * arguments (see Double Dynamic Dispatch), we reply on a checked cast.
	 * 
	 * @param evaluationTime The evaluation time as double. Cash flows prior and including this time are not considered.
	 * @param model The model under which the product is valued.
	 * @return Map containing the value of the product using the given model.
	 */
	Map<String, Object> getValues(double evaluationTime, Model<?> model);
}
```
```
public interface Model<T extends ModelDescriptor> {

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
	Product<? extends ProductDescriptor> getProductFromDesciptor(ProductDescriptor productDescriptor);
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
	Product<? extends T> getProductFromDescription(T descriptor);
}
```
```
public interface ModelFactory<T extends ModelDescriptor> {

	Model<? extends T> getModelFromDescriptor(T description);
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
These additional specificaitons are *not* part of the model descriptor. They belong to the model factory.

### Example

A standard Black Scholes model with constant coefficients is fully determined by the inital value, the drift and the volatility.
A Monte-Carlo simulation of this model requires a time-diescretization of the Euler-Scheme, the number of paths, specifcations related to the random number generators, etc., which are part of the BlackScholesModelMonteCarloFactory.
A Fourier transform implementation of this model requires specification of the ingration method applied to the characteristic functions, which are part of the BlackScholesModelFourierFactory.
